package com.javaeasybank.creditcard.service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

import org.springframework.stereotype.Service;

import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;

@Service
public class CardBillPDFService {

        public byte[] generateBillPdf(
                        String customerName,
                        String billingMonth,
                        BigDecimal totalAmount,
                        BigDecimal minimumPayment,
                        LocalDate dueDate,
                        String password) {

                try {

                        ByteArrayOutputStream baos = new ByteArrayOutputStream();

                        Document document = new Document();

                        PdfWriter writer = PdfWriter.getInstance(document, baos);

                        writer.setEncryption(
                                        password.getBytes(),
                                        password.getBytes(),
                                        PdfWriter.ALLOW_PRINTING,
                                        PdfWriter.ENCRYPTION_AES_128);

                        String fontPath = getClass()
                                        .getClassLoader()
                                        .getResource("fonts/SourceHanSansTC-Normal.otf")
                                        .getPath();

                        BaseFont baseFont = BaseFont.createFont(
                                        fontPath,
                                        BaseFont.IDENTITY_H,
                                        BaseFont.EMBEDDED);

                        Font chineseFont = new Font(baseFont, 12);

                        Font titleFont = new Font(baseFont, 22, Font.BOLD);

                        document.open();
                        

                        Paragraph title = new Paragraph(
                                        "Java Easy Bank 信用卡月結帳單",
                                        titleFont);

                        title.setSpacingAfter(30f);

                        document.add(title);

                        PdfPTable table = new PdfPTable(2);

                        table.setWidthPercentage(100);
                        table.setSpacingBefore(20f);
                        table.setSpacingAfter(20f);

                        addRow(
                                        table,
                                        "客戶姓名",
                                        customerName,
                                        chineseFont);

                        addRow(
                                        table,
                                        "帳單月份",
                                        billingMonth,
                                        chineseFont);

                        addRow(
                                        table,
                                        "本期應繳金額",
                                        "NT$ " + formatMoney(totalAmount),
                                        chineseFont);

                        addRow(
                                        table,
                                        "最低應繳金額",
                                        "NT$ " + formatMoney(minimumPayment),
                                        chineseFont);

                        addRow(
                                        table,
                                        "繳款截止日",
                                        dueDate.toString(),
                                        chineseFont);

                        document.add(table);

                        document.add(new Paragraph(
                                        "本 PDF 已加密，請使用身分證字號末 4 碼開啟。",
                                        chineseFont));

                        document.close();

                        return baos.toByteArray();

                } catch (Exception e) {

                        throw new RuntimeException(
                                        "產生信用卡帳單 PDF 失敗",
                                        e);
                }
        }

        private void addRow(
                        PdfPTable table,
                        String label,
                        String value,
                        Font font) {

                PdfPCell left = new PdfPCell(
                                new Phrase(label, font));

                left.setPadding(10f);

                left.setBackgroundColor(
                                new Color(240, 240, 240));

                PdfPCell right = new PdfPCell(
                                new Phrase(value, font));

                right.setPadding(10f);

                table.addCell(left);
                table.addCell(right);
        }

        private String formatMoney(BigDecimal amount) {

                return String.format(
                                "%,d",
                                amount.setScale(0, RoundingMode.HALF_UP)
                                                .longValue());
        }
}
