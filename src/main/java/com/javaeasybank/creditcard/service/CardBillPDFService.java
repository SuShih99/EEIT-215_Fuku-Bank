package com.javaeasybank.creditcard.service;

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
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;

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

            document.open();

            document.add(new Paragraph(
                    "Java Easy Bank 信用卡月結帳單",
                    chineseFont));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);

            table.addCell(new Phrase("客戶姓名", chineseFont));
            table.addCell(new Phrase(customerName, chineseFont));

            table.addCell(new Phrase("帳單月份", chineseFont));
            table.addCell(new Phrase(billingMonth, chineseFont));

            table.addCell(new Phrase("本期應繳金額", chineseFont));
            table.addCell(new Phrase(
                    "NT$ " + totalAmount.setScale(0, RoundingMode.HALF_UP),
                    chineseFont));

            table.addCell(new Phrase("最低應繳金額", chineseFont));
            table.addCell(new Phrase(
                    "NT$ " + minimumPayment.setScale(0, RoundingMode.HALF_UP),
                    chineseFont));

            table.addCell(new Phrase("繳款截止日", chineseFont));
            table.addCell(new Phrase(dueDate.toString(), chineseFont));

            document.add(table);

            document.add(new Paragraph(" "));
            document.add(new Paragraph(
                    "This PDF is protected by the last 4 digits of your ID number."));

            document.close();

            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("產生信用卡帳單 PDF 失敗", e);
        }
    }
}
