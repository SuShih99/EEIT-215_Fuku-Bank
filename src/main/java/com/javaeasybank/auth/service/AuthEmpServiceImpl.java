package com.javaeasybank.auth.service;

import com.javaeasybank.auth.repository.AuthRespository;
import com.javaeasybank.auth.entity.AuthActionLog;
import com.javaeasybank.auth.entity.AuthEmp;
import com.javaeasybank.auth.entity.AuthRole;
import com.javaeasybank.auth.repository.AuthEmpRepository;
import com.javaeasybank.auth.repository.AuthRoleRepository;
import com.javaeasybank.common.exception.BusinessException;
import org.springframework.beans.BeanUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AuthEmpServiceImpl implements AuthEmpService {

    private final AuthEmpRepository authEmpRepository;
    private final AuthRoleRepository authRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthActionLogService actionLogService;

    public AuthEmpServiceImpl(AuthEmpRepository authEmpRepository,
                              AuthRoleRepository authRoleRepository,
                              PasswordEncoder passwordEncoder,
                              AuthActionLogService actionLogService) {
        this.authEmpRepository = authEmpRepository;
        this.authRoleRepository = authRoleRepository;
        this.passwordEncoder = passwordEncoder;
        this.actionLogService = actionLogService;
    }

    private void recordLog(String empId, String empName,
                           String action, String target,
                           String details, String ipAddress) {
        AuthActionLog log = new AuthActionLog();
        log.setEmpId(empId != null ? empId : "SYSTEM");
        log.setEmpName(empName != null ? empName : "系統");
        log.setAction(action);
        log.setTarget(target != null ? target : "-");
        log.setDetails(details != null ? details : "-");
        log.setIpAddress(ipAddress != null ? ipAddress : "127.0.0.1");
        actionLogService.saveLog(log);
    }

    /** 從 SecurityContext 取得目前登入員工，失敗時回傳系統身份。 */
    private String[] resolveCurrentEmp() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                String email = auth.getName();
                Optional<AuthEmp> optEmp = authEmpRepository.findByEmail(email);
                if (optEmp.isPresent()) {
                    return new String[]{ optEmp.get().getEmpId(), optEmp.get().getEmpName() };
                }
            }
        } catch (Exception ignored) {
            // Best-effort lookup; use system identity below.
        }
        return new String[]{ "SYSTEM", "系統" };
    }

    @Override
    public AuthRespository.AuthEmpResponse login(AuthRespository.LoginRequest request, String ipAddress) {
        AuthEmp emp = authEmpRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException("帳號或密碼錯誤"));

        emp.setLastLoginDate(LocalDateTime.now());
        authEmpRepository.save(emp);

        recordLog(emp.getEmpId(), emp.getEmpName(),
                  "LOGIN", emp.getEmpId(),
                  "員工登入系統", ipAddress);

        return convertToResponse(emp);
    }

    @Override
    public void logout(String email, String ipAddress) {
        String empId = "SYSTEM";
        String empName = "系統";
        if (email != null) {
            Optional<AuthEmp> optEmp = authEmpRepository.findByEmail(email);
            if (optEmp.isPresent()) {
                empId = optEmp.get().getEmpId();
                empName = optEmp.get().getEmpName();
            }
        }
        recordLog(empId, empName, "LOGOUT", empId, "員工登出系統", ipAddress);
    }
    // 員工 CRUD
    @Override
    public Long getEmpCount() {
        return authEmpRepository.count();
    }

    @Override
    public List<AuthRespository.AuthEmpResponse> getAllEmps() {
        return authEmpRepository.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<AuthRespository.AuthEmpResponse> getAllEmpsExcludingCurrent() {
        String currentEmpId = resolveCurrentEmp()[0];
        return authEmpRepository.findAll().stream()
                .filter(emp -> !emp.getEmpId().equals(currentEmpId))
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<AuthRespository.AuthEmpResponse> searchEmpsByName(String keyword) {
        return authEmpRepository.findByEmpNameContaining(keyword).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<AuthRespository.AuthEmpResponse> searchEmpsByNameExcludingCurrent(String keyword) {
        String currentEmpId = resolveCurrentEmp()[0];
        return authEmpRepository.findByEmpNameContaining(keyword).stream()
                .filter(emp -> !emp.getEmpId().equals(currentEmpId))
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public AuthRespository.AuthEmpResponse createEmp(AuthRespository.AuthEmpRequest request) {
        if (authEmpRepository.existsById(request.getEmpId())) {
            throw new BusinessException("員工編號已存在");
        }
        if (authEmpRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new BusinessException("信箱已被使用");
        }

        AuthEmp emp = new AuthEmp();
        BeanUtils.copyProperties(request, emp);
        String rawPassword = request.getPassword() != null ? request.getPassword() : "123456";
        emp.setPasswordHash(passwordEncoder.encode(rawPassword));
        emp.setStatus("ACTIVE");
        if (emp.getPermissionExpire() == null) {
            emp.setPermissionExpire(LocalDateTime.now().plusYears(1));
        }

        AuthEmp saved = authEmpRepository.save(emp);
        String[] cur = resolveCurrentEmp();
        recordLog(cur[0], cur[1],
                  "CREATE_EMP", saved.getEmpId(),
                  "新增員工：" + saved.getEmpName() + "（" + saved.getEmpId() + "）",
                  null);
        return convertToResponse(saved);
    }

    @Override
    public AuthRespository.AuthEmpResponse updateEmp(String empId, AuthRespository.AuthEmpRequest request) {
        AuthEmp emp = authEmpRepository.findById(empId)
                .orElseThrow(() -> new BusinessException("查無此員工"));

        emp.setEmpName(request.getEmpName());
        emp.setEmail(request.getEmail());
        emp.setDeptId(request.getDeptId());
        emp.setRoleId(request.getRoleId());

        if (request.getContractEndDate() != null) {
            emp.setContractEndDate(request.getContractEndDate());
        }

        AuthEmp saved = authEmpRepository.save(emp);
        String[] cur = resolveCurrentEmp();
        recordLog(cur[0], cur[1],
                  "UPDATE_EMP", saved.getEmpId(),
                  "修改員工資料：" + saved.getEmpName() + "（" + saved.getEmpId() + "）",
                  null);
        return convertToResponse(saved);
    }

    @Override
    public void suspendEmp(String empId) {
        String[] cur = resolveCurrentEmp();
        if (empId.equals(cur[0])) {
            throw new BusinessException("不可停用自己的帳號");
        }
        AuthEmp emp = authEmpRepository.findById(empId)
                .orElseThrow(() -> new BusinessException("查無此員工"));
        emp.setStatus("SUSPENDED");
        authEmpRepository.save(emp);
        recordLog(cur[0], cur[1],
                  "SUSPEND_EMP", empId,
                  "停用員工：" + emp.getEmpName() + "（" + empId + "）",
                  null);
    }

    @Override
    public void resumeEmp(String empId) {
        AuthEmp emp = authEmpRepository.findById(empId)
                .orElseThrow(() -> new BusinessException("查無此員工"));
        emp.setStatus("ACTIVE");
        authEmpRepository.save(emp);
        String[] cur = resolveCurrentEmp();
        recordLog(cur[0], cur[1],
                  "RESUME_EMP", empId,
                  "重新啟用員工：" + emp.getEmpName() + "（" + empId + "）",
                  null);
    }
    // 一鍵帶入測試資料
    @Override
    public void seedTestData() {
        if (authEmpRepository.count() > 0) {
            throw new BusinessException("資料庫已有員工資料，請先清空再帶入");
        }

        String[][] data = {
            {"E26001", "林家豪", "DPT001", "R001", "chiahao.lin@javabank.com"},
            {"E26002", "蔡欣妤", "DPT001", "R002", "xinyu.tsai@javabank.com"},
            {"E26003", "陳建志", "DPT002", "R003", "chienchi.chen@javabank.com"},
            {"E26004", "黃雅芳", "DPT002", "R004", "yafang.huang@javabank.com"},
            {"E26005", "吳承翰", "DPT003", "R005", "chenghan.wu@javabank.com"},
            {"E26006", "張佩琪", "DPT003", "R006", "peichi.chang@javabank.com"},
            {"E26007", "王俊傑", "DPT003", "R007", "chunchie.wang@javabank.com"},
            {"E26008", "劉美華", "DPT004", "R008", "meihua.liu@javabank.com"},
            {"E26009", "許志豪", "DPT004", "R009", "chihhao.hsu@javabank.com"},
            {"E26010", "郭建國", "DPT005", "R010", "chienkuo.kuo@javabank.com"},
            {"E26011", "鄭文華", "DPT005", "R011", "wenhua.cheng@javabank.com"},
            {"E26012", "管理員甲", "DPT005", "R012", "admin.a@javabank.com"},
            {"E26013", "管理員乙", "DPT005", "R013", "admin.b@javabank.com"},
        };

        for (String[] d : data) {
            AuthEmp emp = new AuthEmp();
            emp.setEmpId(d[0]);
            emp.setEmpName(d[1]);
            emp.setDeptId(d[2]);
            emp.setRoleId(d[3]);
            emp.setEmail(d[4]);
            emp.setPasswordHash(passwordEncoder.encode("123456"));
            emp.setStatus("ACTIVE");
            emp.setPermissionExpire(LocalDateTime.now().plusYears(1));
            authEmpRepository.save(emp);
        }
        String[] cur = resolveCurrentEmp();
        recordLog(cur[0], cur[1], "SEED_DATA", null, "一鍵帶入員工測試資料（13筆）", null);
    }
    // 給其他模組對接用
    @Override
    public AuthRespository.AuthEmpResponse getEmpByEmpId(String empId) {
        AuthEmp emp = authEmpRepository.findById(empId)
                .orElseThrow(() -> new BusinessException("查無此員工"));
        return convertToResponse(emp);
    }

    @Override
    public AuthRespository.AuthEmpResponse getEmpByEmail(String email) {
        AuthEmp emp = authEmpRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("查無此員工"));
        return convertToResponse(emp);
    }

    @Override
    public String getRoleCodeByEmpId(String empId) {
        AuthEmp emp = authEmpRepository.findById(empId)
                .orElseThrow(() -> new BusinessException("查無此員工"));
        AuthRole role = authRoleRepository.findById(emp.getRoleId())
                .orElseThrow(() -> new BusinessException("角色設定異常"));
        return role.getRoleCode();
    }

    private AuthRespository.AuthEmpResponse convertToResponse(AuthEmp emp) {
        AuthRespository.AuthEmpResponse res = new AuthRespository.AuthEmpResponse();
        BeanUtils.copyProperties(emp, res);

        authRoleRepository.findById(emp.getRoleId()).ifPresent(role -> {
            res.setRoleCode(role.getRoleCode());
            res.setPermScope(role.getPermScope());
            res.setPermLevel(role.getPermLevel());
        });

        return res;
    }
}
