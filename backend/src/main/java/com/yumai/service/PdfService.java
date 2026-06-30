package com.yumai.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.yumai.entity.Bill;
import com.yumai.entity.OrderItem;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Server-side PDF generation with OpenPDF (SRS 3.3.3, FR-02.4, FR-06.1). */
@Service
public class PdfService {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Font TITLE = new Font(Font.HELVETICA, 18, Font.BOLD);
    private static final Font HEADING = new Font(Font.HELVETICA, 11, Font.BOLD, Color.WHITE);
    private static final Font BODY = new Font(Font.HELVETICA, 10);
    private static final Font BOLD = new Font(Font.HELVETICA, 10, Font.BOLD);
    private static final Color BRAND = new Color(220, 88, 42);

    /** FR-02.4 - printable bill/invoice. */
    public byte[] billPdf(Bill bill) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 40, 40, 40, 40);
        PdfWriter.getInstance(doc, out);
        doc.open();

        doc.add(new Paragraph("YumAI Restaurant", TITLE));
        doc.add(new Paragraph("Invoice #" + bill.getBillId(), BOLD));
        doc.add(new Paragraph("Order #" + bill.getOrder().getOrderId()
                + (bill.getOrder().getTableNumber() != null
                        ? "  |  Table " + bill.getOrder().getTableNumber() : ""), BODY));
        doc.add(new Paragraph("Generated: " + bill.getGeneratedAt().format(DATE_TIME), BODY));
        doc.add(new Paragraph("Payment status: " + bill.getPaymentStatus(), BODY));
        doc.add(Chunk.NEWLINE);

        PdfPTable table = new PdfPTable(new float[]{5, 2, 2, 2});
        table.setWidthPercentage(100);
        addHeader(table, "Item", "Qty", "Unit Price", "Subtotal");
        for (OrderItem item : bill.getOrder().getItems()) {
            addRow(table, item.getMenuItem().getName(), String.valueOf(item.getQuantity()),
                    money(item.getUnitPrice()), money(item.getSubtotal()));
        }
        doc.add(table);
        doc.add(Chunk.NEWLINE);

        double subtotal = bill.getTotalAmount() + bill.getDiscountAmount() - bill.getTaxAmount();
        Paragraph totals = new Paragraph();
        totals.setAlignment(Element.ALIGN_RIGHT);
        totals.add(new Chunk("Subtotal: " + money(subtotal) + "\n", BODY));
        totals.add(new Chunk("Discount: -" + money(bill.getDiscountAmount()) + "\n", BODY));
        totals.add(new Chunk("Tax: +" + money(bill.getTaxAmount()) + "\n", BODY));
        totals.add(new Chunk("TOTAL: " + money(bill.getTotalAmount()), BOLD));
        doc.add(totals);

        doc.add(Chunk.NEWLINE);
        doc.add(new Paragraph("Thank you for dining with us!", BODY));
        doc.close();
        return out.toByteArray();
    }

    /** FR-06.1 - generic tabular report. */
    public byte[] tablePdf(String title, String subtitle, List<String> headers, List<List<String>> rows) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate(), 36, 36, 36, 36);
        PdfWriter.getInstance(doc, out);
        doc.open();
        doc.add(new Paragraph(title, TITLE));
        doc.add(new Paragraph(subtitle, BODY));
        doc.add(Chunk.NEWLINE);

        PdfPTable table = new PdfPTable(headers.size());
        table.setWidthPercentage(100);
        addHeader(table, headers.toArray(new String[0]));
        for (List<String> row : rows) {
            addRow(table, row.toArray(new String[0]));
        }
        doc.add(table);
        doc.close();
        return out.toByteArray();
    }

    private void addHeader(PdfPTable table, String... cells) {
        for (String cell : cells) {
            PdfPCell c = new PdfPCell(new Phrase(cell, HEADING));
            c.setBackgroundColor(BRAND);
            c.setPadding(6);
            table.addCell(c);
        }
    }

    private void addRow(PdfPTable table, String... cells) {
        for (String cell : cells) {
            PdfPCell c = new PdfPCell(new Phrase(cell, BODY));
            c.setPadding(5);
            table.addCell(c);
        }
    }

    private static String money(double value) {
        return String.format("%.2f", value);
    }
}
