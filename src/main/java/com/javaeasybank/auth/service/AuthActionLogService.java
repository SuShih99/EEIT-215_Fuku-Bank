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

        Document document = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(document, out);
        document.open();

        Font font = new Font(Font.HELVETICA, 12, Font.BOLD);
        document.add(new Paragraph("System Audit Logs", font));
        document.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(8);
        table.setWidthPercentage(100);
        table.addCell("ID");
        table.addCell("EmpID");
        table.addCell("Name");
        table.addCell("Action");
        table.addCell("Target");
        table.addCell("Details");
        table.addCell("Time");
        table.addCell("IP");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (AuthActionLog log : logs) {
            table.addCell(log.getId().toString());
            table.addCell(log.getEmpId() != null ? log.getEmpId() : "-");
            table.addCell(log.getEmpName() != null ? log.getEmpName() : "-");
            table.addCell(log.getAction());
            table.addCell(log.getTarget() != null ? log.getTarget() : "-");
            table.addCell(log.getDetails() != null ? log.getDetails() : "-");
            table.addCell(log.getActionTime().format(formatter));
            table.addCell(log.getIpAddress() != null ? log.getIpAddress() : "-");
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
