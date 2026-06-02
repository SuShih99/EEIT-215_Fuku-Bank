package com.javaeasybank.auth.service;

import com.javaeasybank.auth.entity.AuthActionLog;
import com.javaeasybank.auth.entity.AuthEmp;
import com.javaeasybank.auth.repository.AuthActionLogRepository;
import com.javaeasybank.auth.repository.AuthEmpRepository;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.opencsv.CSVWriter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class AuthActionLogService {

    private final AuthActionLogRepository actionLogRepository;
    private final AuthEmpRepository authEmpRepository;

    public AuthActionLogService(AuthActionLogRepository actionLogRepository, AuthEmpRepository authEmpRepository) {
        this.actionLogRepository = actionLogRepository;
        this.authEmpRepository = authEmpRepository;
    }

    public List<AuthActionLog> getAllLogs() {
        return actionLogRepository.findAllByOrderByActionTimeDesc();
    }

    public void saveLog(AuthActionLog log) {
        actionLogRepository.save(log);
    }

    public byte[] exportToCsv() throws Exception {
        List<AuthActionLog> logs = actionLogRepository.findAllByOrderByActionTimeDesc();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CSVWriter writer = new CSVWriter(new OutputStreamWriter(out));

        String[] header = {"ID", "員工編號", "姓名", "動作", "目標", "詳情", "時間", "IP"};
        writer.writeNext(header);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (AuthActionLog log : logs) {
            String[] data = {
                    log.getId().toString(),
                    log.getEmpId(),
                    log.getEmpName(),
                    log.getAction(),
                    log.getTarget(),
                    log.getDetails(),
                    log.getActionTime().format(formatter),
                    log.getIpAddress()
            };
            writer.writeNext(data);
        }
        writer.close();
        return out.toByteArray();
    }

    public byte[] exportToPdf() throws Exception {
        List<AuthActionLog> logs = actionLogRepository.findAllByOrderByActionTimeDesc();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        byte[] fontBytes;
        try (java.io.InputStream fontStream = getClass()
                        .getClassLoader()
                        .getResourceAsStream("fonts/SourceHanSansTC-Normal.otf")) {
            if (fontStream == null) {
                throw new RuntimeException("字型檔案 fonts/SourceHanSansTC-Normal.otf 未能在 classpath 中找到");
            }
            fontBytes = fontStream.readAllBytes();
        }

        com.lowagie.text.pdf.BaseFont baseFont = com.lowagie.text.pdf.BaseFont.createFont(
                        "SourceHanSansTC-Normal.otf",
                        com.lowagie.text.pdf.BaseFont.IDENTITY_H,
                        com.lowagie.text.pdf.BaseFont.EMBEDDED,
                        true,
                        fontBytes,
                        null);

        Font chineseFont = new Font(baseFont, 10);
        Font boldChineseFont = new Font(baseFont, 10, Font.BOLD);
        Font titleFont = new Font(baseFont, 14, Font.BOLD);

        Document document = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(document, out);
        document.open();

        document.add(new Paragraph("System Audit Logs", titleFont));
        document.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(8);
        table.setWidthPercentage(100);
        
        table.addCell(new Paragraph("ID", boldChineseFont));
        table.addCell(new Paragraph("EmpID", boldChineseFont));
        table.addCell(new Paragraph("Name", boldChineseFont));
        table.addCell(new Paragraph("Action", boldChineseFont));
        table.addCell(new Paragraph("Target", boldChineseFont));
        table.addCell(new Paragraph("Details", boldChineseFont));
        table.addCell(new Paragraph("Time", boldChineseFont));
        table.addCell(new Paragraph("IP", boldChineseFont));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (AuthActionLog log : logs) {
            table.addCell(new Paragraph(log.getId().toString(), chineseFont));
            table.addCell(new Paragraph(log.getEmpId() != null ? log.getEmpId() : "-", chineseFont));
            table.addCell(new Paragraph(log.getEmpName() != null ? log.getEmpName() : "-", chineseFont));
            table.addCell(new Paragraph(log.getAction(), chineseFont));
            table.addCell(new Paragraph(log.getTarget() != null ? log.getTarget() : "-", chineseFont));
            table.addCell(new Paragraph(log.getDetails() != null ? log.getDetails() : "-", chineseFont));
            table.addCell(new Paragraph(log.getActionTime().format(formatter), chineseFont));
            table.addCell(new Paragraph(log.getIpAddress() != null ? log.getIpAddress() : "-", chineseFont));
        }

        document.add(table);
        document.close();

        return out.toByteArray();
    }

    public void recordAction(String action, String target, String details, String ipAddress) {
        String[] cur = resolveCurrentEmp();
        AuthActionLog log = new AuthActionLog();
        log.setEmpId(cur[0]);
        log.setEmpName(cur[1]);
        log.setAction(action);
        log.setTarget(target != null ? target : "-");
        log.setDetails(details != null ? details : "-");
        log.setIpAddress(ipAddress != null ? ipAddress : "127.0.0.1");
        actionLogRepository.save(log);
    }

    private String[] resolveCurrentEmp() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                String email = auth.getName();
                Optional<AuthEmp> optEmp = authEmpRepository.findByEmail(email);
                if (optEmp.isPresent()) {
                    return new String[]{optEmp.get().getEmpId(), optEmp.get().getEmpName()};
                }
            }
        } catch (Exception ignored) {
        }
        return new String[]{"SYSTEM", "系統"};
    }

}
