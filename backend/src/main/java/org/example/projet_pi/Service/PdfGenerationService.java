package org.example.projet_pi.Service;

import lombok.RequiredArgsConstructor;
import org.example.projet_pi.entity.*;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PdfGenerationService implements IPdfGenerationService {

    private static final Logger logger = LoggerFactory.getLogger(PdfGenerationService.class);

    private final TemplateEngine templateEngine;

    @Override
    public byte[] generateContractPdf(InsuranceContract contract,
                                      Client client,
                                      AgentAssurance agent,
                                      List<Payment> payments) {

        logger.info("Génération du PDF pour le contrat ID: {}", contract.getContractId());

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            // 1. Préparer le contexte Thymeleaf
            Context context = new Context();
            context.setVariable("contract", contract);
            context.setVariable("client", client);
            context.setVariable("agent", agent);
            context.setVariable("payments", payments);
            context.setVariable("dateFormat", new SimpleDateFormat("dd/MM/yyyy"));
            context.setVariable("currency", "€");

            // 2. Ajouter des statistiques
            long paidCount = payments.stream()
                    .filter(p -> p.getStatus() == PaymentStatus.PAID)
                    .count();
            long pendingCount = payments.stream()
                    .filter(p -> p.getStatus() == PaymentStatus.PENDING)
                    .count();
            long lateCount = payments.stream()
                    .filter(p -> p.getStatus() == PaymentStatus.LATE)
                    .count();

            context.setVariable("paidCount", paidCount);
            context.setVariable("pendingCount", pendingCount);
            context.setVariable("lateCount", lateCount);

            // 3. Calculer le montant total payé
            double totalPaid = payments.stream()
                    .filter(p -> p.getStatus() == PaymentStatus.PAID)
                    .mapToDouble(Payment::getAmount)
                    .sum();
            context.setVariable("totalPaid", totalPaid);

            // 4. Générer le HTML
            String htmlContent = templateEngine.process("contract-template", context);

            // 5. Convertir HTML en PDF
            ITextRenderer renderer = new ITextRenderer();

            // Configuration des polices (optionnel)
            /*
            try {
                renderer.getFontResolver().addFont(
                        getClass().getResourceAsStream("/static/fonts/DejaVuSans.ttf"),
                        true
                );
            } catch (Exception e) {
                logger.warn("Police non trouvée, utilisation de la police par défaut");
            }
             */

            renderer.setDocumentFromString(htmlContent);
            renderer.layout();
            renderer.createPDF(outputStream);

            byte[] pdfContent = outputStream.toByteArray();
            logger.info("PDF généré avec succès - Taille: {} bytes", pdfContent.length);

            return pdfContent;

        } catch (Exception e) {
            logger.error("Erreur lors de la génération du PDF: {}", e.getMessage(), e);
            throw new RuntimeException("Erreur lors de la génération du PDF", e);
        }
    }

    @Override
    public void generateContractPdfStreaming(InsuranceContract contract,
                                             Client client,
                                             AgentAssurance agent,
                                             List<Payment> payments,
                                             OutputStream outputStream) {

        logger.info("Génération du PDF en streaming pour le contrat ID: {}", contract.getContractId());

        try {
            Context context = new Context();
            context.setVariable("contract", contract);
            context.setVariable("client", client);
            context.setVariable("agent", agent);
            context.setVariable("payments", payments);
            context.setVariable("dateFormat", new SimpleDateFormat("dd/MM/yyyy"));
            context.setVariable("currency", "€");

            String htmlContent = templateEngine.process("contract-template", context);

            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(htmlContent);
            renderer.layout();
            renderer.createPDF(outputStream);

            logger.info("PDF streaming terminé pour le contrat ID: {}", contract.getContractId());

        } catch (Exception e) {
            logger.error("Erreur lors de la génération du PDF en streaming: {}", e.getMessage(), e);
            throw new RuntimeException("Erreur lors de la génération du PDF", e);
        }
    }

    @Override
    public byte[] generateSimpleContractPdf(InsuranceContract contract) {

        logger.info("Génération du PDF simplifié pour le contrat ID: {}", contract.getContractId());

        StringBuilder content = new StringBuilder();
        content.append("CONTRAT D'ASSURANCE\n");
        content.append("===================\n\n");
        content.append("Numéro de contrat: ").append(contract.getContractId()).append("\n");
        content.append("Date de début: ").append(contract.getStartDate()).append("\n");
        content.append("Date de fin: ").append(contract.getEndDate()).append("\n");
        content.append("Prime: ").append(contract.getPremium()).append(" €\n");
        content.append("Franchise: ").append(contract.getDeductible()).append(" €\n");
        content.append("Plafond: ").append(contract.getCoverageLimit()).append(" €\n");
        content.append("Statut: ").append(contract.getStatus()).append("\n");
        content.append("Fréquence de paiement: ").append(contract.getPaymentFrequency()).append("\n");
        content.append("Total payé: ").append(contract.getTotalPaid()).append(" €\n");
        content.append("Reste à payer: ").append(contract.getRemainingAmount()).append(" €\n");

        if (contract.getClient() != null) {
            content.append("\nCLIENT:\n");
            content.append("  Nom: ").append(contract.getClient().getFirstName())
                    .append(" ").append(contract.getClient().getLastName()).append("\n");
            content.append("  Email: ").append(contract.getClient().getEmail()).append("\n");
        }

        return content.toString().getBytes();
    }
}