package com.javaeasybank.customer.service;

import com.javaeasybank.common.exception.BusinessException;
import com.javaeasybank.customer.repository.CustomerRespository;
import com.javaeasybank.customer.entity.CustomerProfile;
import com.javaeasybank.customer.repository.CustomerProfileRepository;
import com.javaeasybank.risk.enums.BlacklistType;
import com.javaeasybank.risk.service.BlackListService;
import org.springframework.beans.BeanUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CustomerServiceImpl implements CustomerService {

    private final CustomerProfileRepository customerProfileRepository;

    private final BlackListService blackListService;
    // 加入 JdbcTemplate 依賴，用於執行原生 SQL
    private final JdbcTemplate jdbcTemplate;

    // 用於產生隨機英數 (供 createCustomer 使用)
    // 移除了容易混淆的字元: O, 0, I, 1, L
    private static final String ALPHANUMERIC_CHARS = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private final SecureRandom secureRandom = new SecureRandom();

    public CustomerServiceImpl(CustomerProfileRepository customerProfileRepository, JdbcTemplate jdbcTemplate, BlackListService blackListService) {
        this.customerProfileRepository = customerProfileRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.blackListService = blackListService;
    }

    // ===========================
    // CRUD
    // ===========================
    @Override
    public List<CustomerRespository.CustomerResponse> getAllCustomers() {
        return customerProfileRepository.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<CustomerRespository.CustomerResponse> searchByName(String keyword) {
        return customerProfileRepository.findByNameContaining(keyword).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public CustomerRespository.CustomerResponse createCustomer(CustomerRespository.CustomerRequest request) {
        if (customerProfileRepository.findByIdNumber(request.getIdNumber()).isPresent()) {
            throw new BusinessException("身分證字號已存在");
        }
        if (customerProfileRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new BusinessException("Email 已被使用");
        }
        if (customerProfileRepository.findByPhone(request.getPhone()).isPresent()) {
            throw new BusinessException("電話號碼已被使用");
        }

        CustomerProfile profile = new CustomerProfile();
        BeanUtils.copyProperties(request, profile);
        normalizeApplicationDerivedFields(profile);

        // 1. customer_id: 8碼大寫英數 (例如：X7K9P2M4)
        if (profile.getCustomerId() == null || profile.getCustomerId().isEmpty()) {
            profile.setCustomerId(generateAlphanumeric(8));
        }

        // 2. cif: YYMM-8碼大寫英數 (例如：2605-A3R5W8J1)
        String yymm = LocalDate.now().format(DateTimeFormatter.ofPattern("yyMM"));
        profile.setCif(yymm + "-" + generateAlphanumeric(8));

        if(blackListService.isBlacklisted(BlacklistType.PHONE, request.getPhone())){
            throw new BusinessException("此電話號碼已在黑名單中，無法新增");
        }

        profile.setStatus("ACTIVE");
        profile.setIsPep(Boolean.TRUE.equals(profile.getIsPep()));

        CustomerProfile saved = customerProfileRepository.save(profile);
        return convertToResponse(saved);
    }

    @Override
    public CustomerRespository.CustomerResponse updateCustomer(String customerId, CustomerRespository.CustomerRequest request) {
        CustomerProfile profile = customerProfileRepository.findById(customerId)
                .orElseThrow(() -> new BusinessException("查無此客戶"));

        ensureIdNumberAvailable(customerId, request.getIdNumber());
        ensureEmailAvailable(customerId, request.getEmail());
        ensurePhoneAvailable(customerId, request.getPhone());

        if (request.getPhone() != null && blackListService.isBlacklisted(BlacklistType.PHONE, request.getPhone())) {
            throw new BusinessException("此電話號碼已在黑名單中，無法修改");
        }

        if (request.getIdNumber() != null) {
            profile.setIdNumber(request.getIdNumber());
        }
        if (request.getName() != null) {
            profile.setName(request.getName());
        }
        if (request.getBirthday() != null) {
            profile.setBirthday(request.getBirthday());
        }
        if (request.getEmail() != null) {
            profile.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            profile.setPhone(request.getPhone());
        }
        if (request.getAddress() != null) {
            profile.setAddress(request.getAddress());
        }
        if (request.getStatus() != null) {
            profile.setStatus(request.getStatus());
        }
        if (request.getAvatarUrl() != null) {
            profile.setAvatarUrl(request.getAvatarUrl());
        }
        applyOptionalApplicationFields(profile, request);
        if (request.getJob() != null) {
            profile.setJob(request.getJob());
        }
        if (request.getAnnualIncome() != null) {
            profile.setAnnualIncome(request.getAnnualIncome());
        }
        if (request.getRiskLevel() != null) {
            profile.setRiskLevel(request.getRiskLevel());
        }
        normalizeApplicationDerivedFields(profile);

        CustomerProfile saved = customerProfileRepository.save(profile);
        return convertToResponse(saved);
    }

    @Override
    public void deactivateCustomer(String customerId) {
        CustomerProfile profile = customerProfileRepository.findById(customerId)
                .orElseThrow(() -> new BusinessException("查無此客戶"));
        profile.setStatus("INACTIVE");
        customerProfileRepository.save(profile);
    }

    @Override
    public void activateCustomer(String customerId) {
        CustomerProfile profile = customerProfileRepository.findById(customerId)
                .orElseThrow(() -> new BusinessException("查無此客戶"));
        profile.setStatus("ACTIVE");
        customerProfileRepository.save(profile);
    }

    // ===========================
    // 一鍵帶入測試資料（直接執行 SQL）
    // ===========================
    @Override
    @Transactional // 確保整段 SQL 執行過程若有錯誤會自動 Rollback
    public void seedTestData() {

        // 1. 先反向刪除舊資料 (從子表刪回主表) 避免外鍵衝突
        jdbcTemplate.execute("DELETE FROM customer_application");
        jdbcTemplate.execute("DELETE FROM customer_kyc");
        jdbcTemplate.execute("DELETE FROM customer_risk_tag");
        jdbcTemplate.execute("DELETE FROM customer_auth");
        jdbcTemplate.execute("DELETE FROM customer_profile");

        // 2. 準備 Insert 的 SQL 字串 (將您提供的 SQL 貼入)
        String insertProfile = """
                INSERT INTO customer_profile (customer_id, cif, id_number, name, birthday, gender, email, phone, address, status) VALUES
                ('X7K9P2M4', '2605-A3R5W8J1', 'A123456789', N'王大明', '1985-05-15', 'M', 'ming.wang@email.com', '0912000001', N'台北市信義區信義路五段1號', 'ACTIVE'),
                ('V4L6T1Y8', '2605-N9Q2E7C4', 'B223456789', N'林小華', '1990-08-22', 'F', 'hua.lin@email.com', '0912000002', N'台中市西屯區台灣大道三段99號', 'ACTIVE'),
                ('D3H8F5G2', '2605-M1V6P4K9', 'C123456789', N'陳建國', '1978-11-03', 'M', 'chien.chen@email.com', '0912000003', N'高雄市三民區建國一路1號', 'ACTIVE'),
                ('B9W1C7R5', '2605-H5T2J8X3', 'D223456789', N'張雅婷', '1995-02-14', 'F', 'ya.chang@email.com', '0912000004', N'台南市東區大學路1號', 'ACTIVE'),
                ('P6M4N2Q8', '2605-L7F3Y1W6', 'E123456789', N'李志明', '1982-07-30', 'M', 'chih.lee@email.com', '0912000005', N'新北市板橋區中山路一段161號', 'ACTIVE'),
                ('K1T9V5L3', '2605-C4G8R2M5', 'F223456789', N'吳美玲', '1988-12-10', 'F', 'mei.wu@email.com', '0912000006', N'桃園市中壢區中大路300號', 'ACTIVE'),
                ('E8C2X7J4', '2605-T6N1P9V8', 'G123456789', N'黃文輝', '1975-04-05', 'M', 'wen.huang@email.com', '0912000007', N'新竹市東區光復路二段101號', 'INACTIVE'),
                ('Y5R4W1H6', '2605-F2K7M3L9', 'H223456789', N'蔡佳蓉', '1992-09-18', 'F', 'chia.tsai@email.com', '0912000008', N'苗栗縣苗栗市縣府路100號', 'ACTIVE'),
                ('G7N3M8P2', '2605-X1W5C4T6', 'I123456789', N'劉冠宇', '1980-01-25', 'M', 'kuan.liu@email.com', '0912000009', N'彰化縣彰化市中山路二段416號', 'ACTIVE'),
                ('J2F6K9V1', '2605-R8Y4H7N2', 'J223456789', N'許家瑩', '1998-06-08', 'F', 'chia.hsu@email.com', '0912000010', N'南投縣南投市中興路660號', 'ACTIVE'),
                ('Q4W8C1T7', '2605-V3L9M2P5', 'K123456789', N'鄭宗翰', '1986-10-12', 'M', 'tsung.cheng@email.com', '0912000011', N'雲林縣斗六市雲林路二段515號', 'ACTIVE'),
                ('M9P2R5N4', '2605-J7X1W6C8', 'L223456789', N'洪玉婷', '1991-03-20', 'F', 'yu.hung@email.com', '0912000012', N'嘉義市東區中山路199號', 'ACTIVE'),
                ('H3T7J1V9', '2605-K4F8N5R2', 'M123456789', N'邱信宏', '1979-08-05', 'M', 'hsin.chiu@email.com', '0912000013', N'屏東縣屏東市自由路527號', 'ACTIVE'),
                ('L5V1M6P3', '2605-W9T2C7H4', 'N223456789', N'曾婉茹', '1994-11-28', 'F', 'wan.tseng@email.com', '0912000014', N'宜蘭縣宜蘭市縣政北路1號', 'ACTIVE'),
                ('R8K4N9M2', '2605-P1V5J3X6', 'O123456789', N'廖偉翔', '1983-05-10', 'M', 'wei.liao@email.com', '0912000015', N'花蓮縣花蓮市府前路17號', 'ACTIVE'),
                ('T1C7W4R8', '2605-N6M2L9F5', 'P223456789', N'賴怡君', '1987-12-01', 'F', 'yi.lai@email.com', '0912000016', N'台東縣台東市中山路276號', 'ACTIVE'),
                ('N6M9P2V5', '2605-C8H1T4K7', 'Q123456789', N'徐俊傑', '1976-02-18', 'M', 'chun.hsu@email.com', '0912000017', N'澎湖縣馬公市治平路32號', 'ACTIVE'),
                ('W2R5T8C1', '2605-M7N3P6V9', 'R223456789', N'卓佩樺', '1996-07-07', 'F', 'pei.cho@email.com', '0912000018', N'基隆市中正區義一路1號', 'ACTIVE'),
                ('C4H1K7N9', '2605-V5L2W8R3', 'S123456789', N'江宇軒', '1989-09-30', 'M', 'yu.chiang@email.com', '0912000019', N'台北市中正區重慶南路一段122號', 'ACTIVE'),
                ('V7P3M1R6', '2605-T9C4F2J8', 'T223456789', N'郭欣儀', '1993-04-25', 'F', 'hsin.kuo@email.com', '0912000020', N'新北市三重區重新路一段1號', 'ACTIVE')
                """;

        String insertApplication = """
                INSERT INTO customer_application (case_id, customer_id, cif, name, birthday, gender, id_number, email, phone, address, username, status) VALUES
                ('APP260428001', 'X7K9P2M4', '2605-A3R5W8J1', N'王大明', '1985-05-15', 'M', 'A123456789', 'ming.wang@email.com', '0912000001', N'台北市信義區信義路五段1號', 'mingwang85', 'APPROVED'),
                ('APP260428002', 'V4L6T1Y8', '2605-N9Q2E7C4', N'林小華', '1990-08-22', 'F', 'B223456789', 'hua.lin@email.com', '0912000002', N'台中市西屯區台灣大道三段99號', 'hualin90', 'APPROVED'),
                ('APP260428003', 'D3H8F5G2', '2605-M1V6P4K9', N'陳建國', '1978-11-03', 'M', 'C123456789', 'chien.chen@email.com', '0912000003', N'高雄市三民區建國一路1號', 'chienchen78', 'APPROVED'),
                ('APP260428004', 'B9W1C7R5', '2605-H5T2J8X3', N'張雅婷', '1995-02-14', 'F', 'D223456789', 'ya.chang@email.com', '0912000004', N'台南市東區大學路1號', 'yachang95', 'APPROVED'),
                ('APP260428005', 'P6M4N2Q8', '2605-L7F3Y1W6', N'李志明', '1982-07-30', 'M', 'E123456789', 'chih.lee@email.com', '0912000005', N'新北市板橋區中山路一段161號', 'chihlee82', 'APPROVED'),
                ('APP260428006', 'K1T9V5L3', '2605-C4G8R2M5', N'吳美玲', '1988-12-10', 'F', 'F223456789', 'mei.wu@email.com', '0912000006', N'桃園市中壢區中大路300號', 'meiwu88', 'APPROVED'),
                ('APP260428007', 'E8C2X7J4', '2605-T6N1P9V8', N'黃文輝', '1975-04-05', 'M', 'G123456789', 'wen.huang@email.com', '0912000007', N'新竹市東區光復路二段101號', 'wenhuang75', 'APPROVED'),
                ('APP260428008', 'Y5R4W1H6', '2605-F2K7M3L9', N'蔡佳蓉', '1992-09-18', 'F', 'H223456789', 'chia.tsai@email.com', '0912000008', N'苗栗縣苗栗市縣府路100號', 'chiatsai92', 'APPROVED'),
                ('APP260428009', 'G7N3M8P2', '2605-X1W5C4T6', N'劉冠宇', '1980-01-25', 'M', 'I123456789', 'kuan.liu@email.com', '0912000009', N'彰化縣彰化市中山路二段416號', 'kuanliu80', 'APPROVED'),
                ('APP260428010', 'J2F6K9V1', '2605-R8Y4H7N2', N'許家瑩', '1998-06-08', 'F', 'J223456789', 'chia.hsu@email.com', '0912000010', N'南投縣南投市中興路660號', 'chiahsu98', 'APPROVED'),
                ('APP260428011', 'Q4W8C1T7', '2605-V3L9M2P5', N'鄭宗翰', '1986-10-12', 'M', 'K123456789', 'tsung.cheng@email.com', '0912000011', N'雲林縣斗六市雲林路二段515號', 'tsungcheng86', 'APPROVED'),
                ('APP260428012', 'M9P2R5N4', '2605-J7X1W6C8', N'洪玉婷', '1991-03-20', 'F', 'L223456789', 'yu.hung@email.com', '0912000012', N'嘉義市東區中山路199號', 'yuhung91', 'APPROVED'),
                ('APP260428013', 'H3T7J1V9', '2605-K4F8N5R2', N'邱信宏', '1979-08-05', 'M', 'M123456789', 'hsin.chiu@email.com', '0912000013', N'屏東縣屏東市自由路527號', 'hsinchiu79', 'APPROVED'),
                ('APP260428014', 'L5V1M6P3', '2605-W9T2C7H4', N'曾婉茹', '1994-11-28', 'F', 'N223456789', 'wan.tseng@email.com', '0912000014', N'宜蘭縣宜蘭市縣政北路1號', 'wantseng94', 'APPROVED'),
                ('APP260428015', 'R8K4N9M2', '2605-P1V5J3X6', N'廖偉翔', '1983-05-10', 'M', 'O123456789', 'wei.liao@email.com', '0912000015', N'花蓮縣花蓮市府前路17號', 'weiliao83', 'APPROVED'),
                ('APP260428016', 'T1C7W4R8', '2605-N6M2L9F5', N'賴怡君', '1987-12-01', 'F', 'P223456789', 'yi.lai@email.com', '0912000016', N'台東縣台東市中山路276號', 'yilai87', 'APPROVED'),
                ('APP260428017', 'N6M9P2V5', '2605-C8H1T4K7', N'徐俊傑', '1976-02-18', 'M', 'Q123456789', 'chun.hsu@email.com', '0912000017', N'澎湖縣馬公市治平路32號', 'chunhsu76', 'APPROVED'),
                ('APP260428018', 'W2R5T8C1', '2605-M7N3P6V9', N'卓佩樺', '1996-07-07', 'F', 'R223456789', 'pei.cho@email.com', '0912000018', N'基隆市中正區義一路1號', 'peicho96', 'APPROVED'),
                ('APP260428019', 'C4H1K7N9', '2605-V5L2W8R3', N'江宇軒', '1989-09-30', 'M', 'S123456789', 'yu.chiang@email.com', '0912000019', N'台北市中正區重慶南路一段122號', 'yuchiang89', 'APPROVED'),
                ('APP260428020', 'V7P3M1R6', '2605-T9C4F2J8', N'郭欣儀', '1993-04-25', 'F', 'T223456789', 'hsin.kuo@email.com', '0912000020', N'新北市三重區重新路一段1號', 'hsinkuo93', 'APPROVED')
                """;

        String insertKyc = """
                INSERT INTO customer_kyc (customer_id, id_issue_date, id_issue_location, id_issue_type, marital_status, education_level, occupation_category, company_name, annual_income, source_of_wealth) VALUES
                ('X7K9P2M4', '2015-10-01', N'台北市', N'初發', 'M', N'大學', N'資訊科技業', N'台積電', 150, N'薪資收入'),
                ('V4L6T1Y8', '2018-05-12', N'台中市', N'換發', 'S', N'碩士', N'金融保險業', N'國泰金控', 120, N'薪資收入'),
                ('D3H8F5G2', '2010-12-20', N'高雄市', N'換發', 'M', N'高中', N'製造業', N'中鋼', 90, N'薪資與投資'),
                ('B9W1C7R5', '2020-03-15', N'台南市', N'初發', 'S', N'大學', N'服務業', N'統一超商', 50, N'薪資收入'),
                ('P6M4N2Q8', '2016-08-08', N'新北市', N'補發', 'D', N'專科', N'營造建築業', N'遠雄建設', 200, N'營利所得'),
                ('K1T9V5L3', '2019-11-11', N'桃園市', N'換發', 'M', N'大學', N'運輸物流業', N'長榮航空', 110, N'薪資收入'),
                ('E8C2X7J4', '2014-02-28', N'新竹市', N'換發', 'M', N'博士', N'教育研究', N'清華大學', 130, N'薪資收入'),
                ('Y5R4W1H6', '2021-07-05', N'苗栗縣', N'初發', 'S', N'大學', N'醫療保健業', N'長庚醫院', 80, N'薪資收入'),
                ('G7N3M8P2', '2012-09-15', N'彰化縣', N'補發', 'M', N'高中', N'農林漁牧業', N'自營農場', 60, N'營業收入'),
                ('J2F6K9V1', '2022-01-10', N'南投縣', N'初發', 'S', N'大學', N'藝術娛樂業', N'自由業', 40, N'執行業務所得'),
                ('Q4W8C1T7', '2017-06-22', N'雲林縣', N'換發', 'M', N'碩士', N'公用事業', N'台電', 100, N'薪資收入'),
                ('M9P2R5N4', '2019-04-18', N'嘉義市', N'補發', 'S', N'專科', N'批發零售業', N'全聯實業', 55, N'薪資收入'),
                ('H3T7J1V9', '2011-10-30', N'屏東縣', N'換發', 'M', N'高中', N'住宿餐飲業', N'自營餐廳', 85, N'營業收入'),
                ('L5V1M6P3', '2020-08-14', N'宜蘭縣', N'初發', 'S', N'大學', N'出版傳媒業', N'天下雜誌', 65, N'薪資收入'),
                ('R8K4N9M2', '2015-03-25', N'花蓮縣', N'換發', 'D', N'碩士', N'政府機關', N'花蓮縣政府', 95, N'薪資收入'),
                ('T1C7W4R8', '2018-12-05', N'台東縣', N'補發', 'M', N'大學', N'休閒旅遊業', N'雄獅旅遊', 50, N'薪資收入'),
                ('N6M9P2V5', '2013-07-19', N'澎湖縣', N'換發', 'M', N'高中', N'漁業', N'遠洋漁業公司', 140, N'薪資收入'),
                ('W2R5T8C1', '2021-11-02', N'基隆市', N'初發', 'S', N'大學', N'專業科學技術', N'理律法律事務所', 120, N'執行業務所得'),
                ('C4H1K7N9', '2016-01-08', N'台北市', N'換發', 'M', N'博士', N'資訊科技業', N'Google 台灣', 350, N'薪資與股票'),
                ('V7P3M1R6', '2019-09-12', N'新北市', N'補發', 'S', N'碩士', N'金融保險業', N'富邦金控', 115, N'薪資收入')
                """;

        String insertRiskTag = """
                INSERT INTO customer_risk_tag (customer_id, aml_risk_level, pep_status, is_fraud_suspect, block_reason, kyc_next_review_date) VALUES
                ('X7K9P2M4', 'LOW', 'N', 'N', NULL, '2028-04-28'),
                ('V4L6T1Y8', 'LOW', 'N', 'N', NULL, '2028-04-28'),
                ('D3H8F5G2', 'MEDIUM', 'N', 'N', NULL, '2027-04-28'),
                ('B9W1C7R5', 'LOW', 'N', 'N', NULL, '2028-04-28'),
                ('P6M4N2Q8', 'HIGH', 'N', 'N', NULL, '2026-10-28'),
                ('K1T9V5L3', 'LOW', 'N', 'N', NULL, '2028-04-28'),
                ('E8C2X7J4', 'LOW', 'N', 'Y', N'多次小額異常匯出被警示', '2026-05-28'),
                ('Y5R4W1H6', 'LOW', 'N', 'N', NULL, '2028-04-28'),
                ('G7N3M8P2', 'MEDIUM', 'N', 'N', NULL, '2027-04-28'),
                ('J2F6K9V1', 'LOW', 'N', 'N', NULL, '2028-04-28'),
                ('Q4W8C1T7', 'LOW', 'N', 'N', NULL, '2028-04-28'),
                ('M9P2R5N4', 'LOW', 'N', 'N', NULL, '2028-04-28'),
                ('H3T7J1V9', 'LOW', 'N', 'N', NULL, '2028-04-28'),
                ('L5V1M6P3', 'LOW', 'N', 'N', NULL, '2028-04-28'),
                ('R8K4N9M2', 'HIGH', 'Y', 'N', N'重要政治性職務人士', '2026-10-28'),
                ('T1C7W4R8', 'LOW', 'N', 'N', NULL, '2028-04-28'),
                ('N6M9P2V5', 'LOW', 'N', 'N', NULL, '2028-04-28'),
                ('W2R5T8C1', 'LOW', 'N', 'N', NULL, '2028-04-28'),
                ('C4H1K7N9', 'LOW', 'N', 'N', NULL, '2028-04-28'),
                ('V7P3M1R6', 'LOW', 'N', 'N', NULL, '2028-04-28')
                """;

        // 3. 依序執行正向新增 (先建主表，再建子表)
        jdbcTemplate.execute(insertProfile);
        jdbcTemplate.execute(insertApplication);
        jdbcTemplate.execute(insertKyc);
        jdbcTemplate.execute(insertRiskTag);
        jdbcTemplate.execute("""
                UPDATE p
                SET
                    p.nationality = COALESCE(p.nationality, 'TW'),
                    p.registered_address = COALESCE(p.registered_address, p.address),
                    p.current_address = COALESCE(p.current_address, p.address),
                    p.occupation = COALESCE(p.occupation, k.occupation_category),
                    p.job = COALESCE(p.job, k.occupation_category),
                    p.employer = COALESCE(p.employer, k.company_name),
                    p.annual_income = COALESCE(p.annual_income, k.annual_income),
                    p.tax_residency = COALESCE(p.tax_residency, k.tax_residency, 'TW'),
                    p.is_pep = CASE WHEN r.pep_status = 'Y' THEN 1 ELSE COALESCE(p.is_pep, 0) END,
                    p.risk_level = COALESCE(p.risk_level, r.aml_risk_level),
                    p.account_application_synced_at = COALESCE(p.account_application_synced_at, GETDATE())
                FROM customer_profile p
                LEFT JOIN customer_kyc k ON p.customer_id = k.customer_id
                LEFT JOIN customer_risk_tag r ON p.customer_id = r.customer_id
                """);
    }

    // ===========================
    // 給其他模組對接用
    // ===========================
    @Override
    public CustomerRespository.CustomerResponse findByCustomerId(String customerId) {
        CustomerProfile profile = customerProfileRepository.findById(customerId)
                .orElseThrow(() -> new BusinessException("查無此客戶"));
        return convertToResponse(profile);
    }

    @Override
    public CustomerRespository.CustomerResponse findByIdNumber(String idNumber) {
        CustomerProfile profile = customerProfileRepository.findByIdNumber(idNumber)
                .orElseThrow(() -> new BusinessException("查無此客戶"));
        return convertToResponse(profile);
    }

    @Override
    public CustomerRespository.CustomerResponse findByCif(String cif) {
        CustomerProfile profile = customerProfileRepository.findByCif(cif)
                .orElseThrow(() -> new BusinessException("查無此客戶編號：" + cif));
        return convertToResponse(profile);
    }

    @Override
    @Transactional
    public CustomerRespository.CustomerResponse syncAccountApplicationProfile(
            String customerId,
            CustomerRespository.AccountApplicationProfileSyncRequest request) {
        CustomerProfile profile = customerProfileRepository.findById(customerId)
                .orElseThrow(() -> new BusinessException("查無此客戶"));

        ensureIdNumberAvailable(customerId, request.getIdNumber());
        ensureEmailAvailable(customerId, request.getEmail());
        ensurePhoneAvailable(customerId, request.getPhone());

        if (request.getName() != null) {
            profile.setName(request.getName());
        }
        if (request.getIdNumber() != null) {
            profile.setIdNumber(request.getIdNumber());
        }
        if (request.getBirthday() != null) {
            profile.setBirthday(request.getBirthday());
        }
        if (request.getGender() != null) {
            profile.setGender(request.getGender());
        }
        if (request.getEmail() != null) {
            profile.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            profile.setPhone(request.getPhone());
        }

        profile.setNationality(request.getNationality());
        profile.setRegisteredAddress(request.getRegisteredAddress());
        profile.setCurrentAddress(request.getCurrentAddress());
        profile.setAddress(resolveAddress(request.getAddress(), request.getCurrentAddress(), request.getRegisteredAddress(), profile.getAddress()));
        profile.setOccupation(request.getOccupation());
        profile.setJob(request.getOccupation());
        profile.setEmployer(request.getEmployer());
        profile.setEstimatedMonthlyTx(request.getEstimatedMonthlyTx());
        profile.setAccountPurpose(request.getAccountPurpose());
        profile.setFundSource(request.getFundSource());
        profile.setTaxResidency(request.getTaxResidency());
        profile.setIsPep(Boolean.TRUE.equals(request.getIsPep()));
        profile.setIdFrontUrl(request.getIdFrontUrl());
        profile.setIdBackUrl(request.getIdBackUrl());
        profile.setSecondIdUrl(request.getSecondIdUrl());
        profile.setLatestAccountApplicationId(request.getLatestAccountApplicationId());
        profile.setLatestAccountApplicationNo(request.getLatestAccountApplicationNo());
        profile.setLatestAppliedAccountType(request.getLatestAppliedAccountType());
        profile.setLatestAppliedCurrency(request.getLatestAppliedCurrency());
        profile.setLatestAccountApplicationStatus(request.getLatestAccountApplicationStatus());
        profile.setLatestAccountApplicationRiskFlag(request.getLatestAccountApplicationRiskFlag());
        profile.setLatestAccountApplicationReviewedAt(request.getLatestAccountApplicationReviewedAt());
        profile.setLatestAccountApplicationReviewedBy(request.getLatestAccountApplicationReviewedBy());
        profile.setLatestAccountApplicationRejectReason(request.getLatestAccountApplicationRejectReason());
        profile.setCreatedAccountNumber(request.getCreatedAccountNumber());
        profile.setAccountApplicationSyncedAt(LocalDateTime.now());

        CustomerProfile saved = customerProfileRepository.save(profile);
        return convertToResponse(saved);
    }

    // ===========================
    // 私有方法
    // ===========================
    private CustomerRespository.CustomerResponse convertToResponse(CustomerProfile profile) {
        CustomerRespository.CustomerResponse res = new CustomerRespository.CustomerResponse();
        BeanUtils.copyProperties(profile, res);
        return res;
    }

    private void applyOptionalApplicationFields(CustomerProfile profile, CustomerRespository.CustomerRequest request) {
        if (request.getNationality() != null) profile.setNationality(request.getNationality());
        if (request.getRegisteredAddress() != null) profile.setRegisteredAddress(request.getRegisteredAddress());
        if (request.getCurrentAddress() != null) profile.setCurrentAddress(request.getCurrentAddress());
        if (request.getOccupation() != null) {
            profile.setOccupation(request.getOccupation());
            profile.setJob(request.getOccupation());
        }
        if (request.getEmployer() != null) profile.setEmployer(request.getEmployer());
        if (request.getEstimatedMonthlyTx() != null) profile.setEstimatedMonthlyTx(request.getEstimatedMonthlyTx());
        if (request.getAccountPurpose() != null) profile.setAccountPurpose(request.getAccountPurpose());
        if (request.getFundSource() != null) profile.setFundSource(request.getFundSource());
        if (request.getTaxResidency() != null) profile.setTaxResidency(request.getTaxResidency());
        if (request.getIsPep() != null) profile.setIsPep(request.getIsPep());
        if (request.getIdFrontUrl() != null) profile.setIdFrontUrl(request.getIdFrontUrl());
        if (request.getIdBackUrl() != null) profile.setIdBackUrl(request.getIdBackUrl());
        if (request.getSecondIdUrl() != null) profile.setSecondIdUrl(request.getSecondIdUrl());
        if (request.getLatestAccountApplicationId() != null) profile.setLatestAccountApplicationId(request.getLatestAccountApplicationId());
        if (request.getLatestAccountApplicationNo() != null) profile.setLatestAccountApplicationNo(request.getLatestAccountApplicationNo());
        if (request.getLatestAppliedAccountType() != null) profile.setLatestAppliedAccountType(request.getLatestAppliedAccountType());
        if (request.getLatestAppliedCurrency() != null) profile.setLatestAppliedCurrency(request.getLatestAppliedCurrency());
        if (request.getLatestAccountApplicationStatus() != null) profile.setLatestAccountApplicationStatus(request.getLatestAccountApplicationStatus());
        if (request.getLatestAccountApplicationRiskFlag() != null) profile.setLatestAccountApplicationRiskFlag(request.getLatestAccountApplicationRiskFlag());
        if (request.getLatestAccountApplicationReviewedAt() != null) profile.setLatestAccountApplicationReviewedAt(request.getLatestAccountApplicationReviewedAt());
        if (request.getLatestAccountApplicationReviewedBy() != null) profile.setLatestAccountApplicationReviewedBy(request.getLatestAccountApplicationReviewedBy());
        if (request.getLatestAccountApplicationRejectReason() != null) profile.setLatestAccountApplicationRejectReason(request.getLatestAccountApplicationRejectReason());
        if (request.getCreatedAccountNumber() != null) profile.setCreatedAccountNumber(request.getCreatedAccountNumber());
        if (request.getAccountApplicationSyncedAt() != null) profile.setAccountApplicationSyncedAt(request.getAccountApplicationSyncedAt());
    }

    private void normalizeApplicationDerivedFields(CustomerProfile profile) {
        if (profile.getRegisteredAddress() == null) {
            profile.setRegisteredAddress(profile.getAddress());
        }
        if (profile.getCurrentAddress() == null) {
            profile.setCurrentAddress(profile.getAddress());
        }
        if (profile.getOccupation() != null && profile.getJob() == null) {
            profile.setJob(profile.getOccupation());
        }
        if (profile.getIsPep() == null) {
            profile.setIsPep(false);
        }
    }

    private void ensureIdNumberAvailable(String customerId, String idNumber) {
        if (idNumber == null) {
            return;
        }
        customerProfileRepository.findByIdNumber(idNumber).ifPresent(profile -> {
            if (!profile.getCustomerId().equals(customerId)) {
                throw new BusinessException("身分證字號已被他人使用");
            }
        });
    }

    private void ensurePhoneAvailable(String customerId, String phone) {
        if (phone == null) {
            return;
        }
        customerProfileRepository.findByPhone(phone).ifPresent(profile -> {
            if (!profile.getCustomerId().equals(customerId)) {
                throw new BusinessException("電話號碼已被他人使用");
            }
        });
    }

    private void ensureEmailAvailable(String customerId, String email) {
        if (email == null) {
            return;
        }
        customerProfileRepository.findByEmail(email).ifPresent(profile -> {
            if (!profile.getCustomerId().equals(customerId)) {
                throw new BusinessException("Email 已被他人使用");
            }
        });
    }

    private String resolveAddress(String address, String currentAddress, String registeredAddress, String fallback) {
        if (address != null && !address.isBlank()) {
            return address;
        }
        if (currentAddress != null && !currentAddress.isBlank()) {
            return currentAddress;
        }
        if (registeredAddress != null && !registeredAddress.isBlank()) {
            return registeredAddress;
        }
        return fallback;
    }

    /**
     * 產生指定長度的隨機大寫英數混合字串
     */
    private String generateAlphanumeric(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int randomIndex = secureRandom.nextInt(ALPHANUMERIC_CHARS.length());
            sb.append(ALPHANUMERIC_CHARS.charAt(randomIndex));
        }
        return sb.toString();
    }
}
