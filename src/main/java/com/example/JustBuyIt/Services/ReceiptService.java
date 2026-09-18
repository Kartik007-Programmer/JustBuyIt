package com.example.JustBuyIt.Services;

import com.example.JustBuyIt.DTOs.CartItemResponse;
import com.example.JustBuyIt.DTOs.PaymentResponse;
import com.example.JustBuyIt.DTOs.ShoppingCartResponse;
import com.example.JustBuyIt.Models.Users;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class ReceiptService {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    public byte[] generateReceiptPdf(PaymentResponse payment,
                                     ShoppingCartResponse order,
                                     Users user) {
        Document document = new Document(PageSize.A4, 50, 50, 50, 50);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Font h1     = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22);
            Font h2     = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            Font normal = FontFactory.getFont(FontFactory.HELVETICA, 11);
            Font muted  = FontFactory.getFont(FontFactory.HELVETICA, 10,
                    Font.NORMAL, new java.awt.Color(120,120,120));
            Font bold   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);

            // ---------- Header ----------
            Paragraph brand = new Paragraph("JustBuyIt", h1);
            brand.setAlignment(Element.ALIGN_CENTER);
            document.add(brand);

            Paragraph tagline = new Paragraph("Payment Receipt", muted);
            tagline.setAlignment(Element.ALIGN_CENTER);
            tagline.setSpacingAfter(20);
            document.add(tagline);

            // ---------- Transaction meta ----------
            PdfPTable metaTable = new PdfPTable(2);
            metaTable.setWidthPercentage(100);
            metaTable.setSpacingAfter(20);

            addMetaRow(metaTable, "Transaction ID", payment.getTransactionId(), bold, normal);
            addMetaRow(metaTable, "Date", LocalDateTime.now().format(DATE_FMT), bold, normal);
            addMetaRow(metaTable, "Customer", user.getName(), bold, normal);
            addMetaRow(metaTable, "Email", user.getEmail(), bold, normal);
            addMetaRow(metaTable, "Payment Method", payment.getPaymentMethod(), bold, normal);
            addMetaRow(metaTable, "Status", payment.getStatus(), bold, normal);

            document.add(metaTable);

            // ---------- Items table ----------
            PdfPTable items = new PdfPTable(new float[]{4, 1.5f, 2, 2});
            items.setWidthPercentage(100);
            items.setSpacingBefore(10);
            items.setSpacingAfter(15);

            addHeaderCell(items, "Product");
            addHeaderCell(items, "Qty");
            addHeaderCell(items, "Unit Price");
            addHeaderCell(items, "Subtotal");

            for (CartItemResponse item : order.getItems()) {
                items.addCell(new Phrase(item.getProductName(), normal));
                items.addCell(new Phrase(String.valueOf(item.getQuantity()), normal));
                items.addCell(new Phrase("₹" + item.getUnitPrice(), normal));
                items.addCell(new Phrase("₹" + item.getSubTotal(), normal));
            }

            document.add(items);

            // ---------- Totals ----------
            PdfPTable totals = new PdfPTable(new float[]{3, 1});
            totals.setWidthPercentage(45);
            totals.setHorizontalAlignment(Element.ALIGN_RIGHT);

            addTotalRow(totals, "Subtotal", "₹" + order.getTotalPrice(), normal, bold);
            
            PdfPCell labelCell = new PdfPCell(new Phrase("Total Paid", h2));
            labelCell.setBorder(Rectangle.TOP);
            labelCell.setPaddingTop(8);
            totals.addCell(labelCell);

            PdfPCell valueCell = new PdfPCell(new Phrase(
                    "₹" + payment.getAmount() + " " + payment.getCurrency(), h2));
            valueCell.setBorder(Rectangle.TOP);
            valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            valueCell.setPaddingTop(8);
            totals.addCell(valueCell);

            document.add(totals);

            // ---------- Footer ----------
            Paragraph footer = new Paragraph(
                    "\n\nThis is a computer-generated receipt. " +
                            "For queries, contact support@justbuyit.com",
                    muted);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();
            return out.toByteArray();

        } catch (DocumentException e) {
            throw new RuntimeException("Failed to generate receipt PDF", e);
        }
    }

    // ---------- helpers ----------
    private void addMetaRow(PdfPTable table, String label, String value,
                            Font bold, Font normal) {
        PdfPCell l = new PdfPCell(new Phrase(label, bold));
        l.setBorder(Rectangle.NO_BORDER);
        l.setPaddingBottom(6);
        PdfPCell v = new PdfPCell(new Phrase(value, normal));
        v.setBorder(Rectangle.NO_BORDER);
        v.setPaddingBottom(6);
        table.addCell(l);
        table.addCell(v);
    }

    private void addHeaderCell(PdfPTable table, String text) {
        Font header = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
        PdfPCell cell = new PdfPCell(new Phrase(text, header));
        cell.setBackgroundColor(new java.awt.Color(240, 239, 255));
        cell.setPadding(8);
        table.addCell(cell);
    }

    private void addTotalRow(PdfPTable table, String label, String value,
                             Font normal, Font bold) {
        table.addCell(new Phrase(label, normal));
        PdfPCell v = new PdfPCell(new Phrase(value, bold));
        v.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(v);
    }
}