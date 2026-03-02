package org.example.projet_pi.Service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import org.example.projet_pi.entity.Account;
import org.example.projet_pi.entity.Transaction;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PdfService {

    public byte[] generateStatement(Account account, List<Transaction> transactions)
            throws DocumentException {

        Document document = new Document();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        document.open();

        // 🎨 Fonts
        Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
        Font headerFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
        Font normalFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);

        // 📌 Titre
        Paragraph title = new Paragraph("EXTRAIT DE COMPTE", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
        document.add(Chunk.NEWLINE);

        // 📋 Infos du compte
        document.add(new Paragraph("Informations du Compte", headerFont));
        document.add(new Paragraph("Account ID : " + account.getAccountId(), normalFont));
        document.add(new Paragraph("Solde actuel : " + account.getBalance() + " TND", normalFont));
        document.add(Chunk.NEWLINE);

        // 📊 Tableau des transactions
        document.add(new Paragraph("Historique des Transactions", headerFont));
        document.add(Chunk.NEWLINE);

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1, 2, 2, 2});

        // En-têtes
        addTableHeader(table, "ID");
        addTableHeader(table, "Type");
        addTableHeader(table, "Montant");
        addTableHeader(table, "Date");

        // Lignes
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        for (Transaction t : transactions) {
            table.addCell(new Phrase(String.valueOf(t.getTransactionId()), normalFont));
            table.addCell(new Phrase(t.getType(), normalFont));
            table.addCell(new Phrase(t.getAmount() + " TND", normalFont));
            table.addCell(new Phrase(t.getDate().format(formatter), normalFont));
        }

        document.add(table);
        document.add(Chunk.NEWLINE);

        // 📅 Date de génération
        document.add(new Paragraph(
                "Généré le : " + java.time.LocalDate.now().format(formatter), normalFont));

        document.close();
        return out.toByteArray();
    }

    private void addTableHeader(PdfPTable table, String text) {
        Font font = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, BaseColor.WHITE);
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(new BaseColor(63, 81, 181));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(8);
        table.addCell(cell);
    }
}