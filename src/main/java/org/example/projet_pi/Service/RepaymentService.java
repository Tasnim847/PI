package org.example.projet_pi.Service;

import com.stripe.model.PaymentIntent;
import jakarta.transaction.Transactional;

import org.example.projet_pi.Dto.AnnualAmortissementDTO;
import org.example.projet_pi.Dto.RepaymentDTO;
import org.example.projet_pi.Repository.CreditRepository;
import org.example.projet_pi.Repository.RepaymentRepository;
import org.example.projet_pi.Service.EmailCredit.CreditEmailService;
import org.example.projet_pi.entity.*;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class RepaymentService implements IRepaymentService {

    private final RepaymentRepository repaymentRepository;
    private final CreditRepository creditRepository;
    private final TemplateEngine templateEngine;
    private final CreditEmailService creditEmailService;
    private final StripeRepaymentService stripePaymentService;

    // ✅ CONSTRUCTEUR
    public RepaymentService(RepaymentRepository repaymentRepository,
                            CreditRepository creditRepository,
                            TemplateEngine templateEngine,
                            CreditEmailService creditEmailService,
                            StripeRepaymentService stripeRepaymentService) {
        this.repaymentRepository = repaymentRepository;
        this.creditRepository = creditRepository;
        this.templateEngine = templateEngine;
        this.creditEmailService = creditEmailService;
        this.stripePaymentService = stripeRepaymentService;
    }

    // ===== CRUD =====
    @Override
    public Repayment addRepayment(Repayment repayment) {
        return repaymentRepository.save(repayment);
    }

    @Override
    public Repayment updateRepayment(Repayment repayment) {
        return repaymentRepository.save(repayment);
    }

    @Override
    public void deleteRepayment(Long id) {
        repaymentRepository.deleteById(id);
    }

    @Override
    public Repayment getRepaymentById(Long id) {
        return repaymentRepository.findById(id).orElse(null);
    }

    @Override
    public List<Repayment> getAllRepayments() {
        return repaymentRepository.findAll();
    }

    // ===== METIER : payer un crédit =====
    @Override
    @Transactional
    public Repayment payCredit(Long creditId, Repayment repayment) {
        // 1) Validation basique
        if (repayment == null) {
            throw new IllegalArgumentException("Repayment est null");
        }
        if (repayment.getAmount() == null || repayment.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Montant doit être > 0");
        }
        if (repayment.getPaymentMethod() == null) {
            throw new IllegalArgumentException("PaymentMethod obligatoire");
        }

        // ✅ Vérifier si c'est Stripe
        if (repayment.getPaymentMethod() == PaymentMethod.STRIPE) {
            throw new IllegalArgumentException(
                    "Pour les paiements Stripe, utilisez l'endpoint /Repayment/stripe-pay/{creditId}"
            );
        }

        // 2) Charger le crédit
        Credit credit = creditRepository.findById(creditId)
                .orElseThrow(() -> new IllegalArgumentException("Credit introuvable"));

        repayment.setCredit(credit);
        CreditStatus st = credit.getStatus();

        if (st == CreditStatus.CLOSED) {
            throw new IllegalStateException("Credit déjà fermé");
        }
        if (st == CreditStatus.REJECTED) {
            throw new IllegalStateException("Credit rejeté, paiement interdit");
        }
        if (st == CreditStatus.PENDING) {
            throw new IllegalStateException("Credit en attente, paiement interdit");
        }

        // 3) Date auto
        if (repayment.getPaymentDate() == null) {
            repayment.setPaymentDate(LocalDate.now());
        }

        if (repayment.getPaymentDate().isAfter(repayment.getCredit().getDueDate())) {
            repayment.setStatus(RepaymentStatus.LATE);
        } else {
            repayment.setStatus(RepaymentStatus.PAID);
        }

        // 4) Référence auto
        if (repayment.getReference() == null || repayment.getReference().isBlank()) {
            repayment.setReference("PAY-" + creditId + "-" + UUID.randomUUID().toString().substring(0, 8));
        }

        // 5) Total payé (PAID)
        BigDecimal totalPaid = repaymentRepository.sumPaidSuccess(creditId);
        if (totalPaid == null) totalPaid = BigDecimal.ZERO;
        totalPaid = totalPaid.setScale(2, RoundingMode.HALF_UP);

        // 6) Calcul du montant total à payer
        BigDecimal monthly = BigDecimal.valueOf(credit.getMonthlyPayment()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalPayable = monthly.multiply(BigDecimal.valueOf(credit.getDurationInMonths()))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal remaining = totalPayable.subtract(totalPaid).setScale(2, RoundingMode.HALF_UP);

        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            credit.setStatus(CreditStatus.CLOSED);
            creditRepository.save(credit);
            throw new IllegalStateException("Credit déjà payé");
        }

        // 7) Vérification stricte du paiement
        BigDecimal amountToPay = repayment.getAmount().setScale(2, RoundingMode.HALF_UP);

        if (amountToPay.compareTo(monthly) < 0 || amountToPay.compareTo(remaining) > 0) {
            repayment.setStatus(RepaymentStatus.FAILED);
            repayment.setCredit(credit);
            return repaymentRepository.save(repayment);
        }

        // 8) Paiement valide
        repayment.setCredit(credit);
        Repayment saved = repaymentRepository.save(repayment);

        // 9) Mise à jour du statut du crédit
        if (credit.getStatus() == CreditStatus.APPROVED) {
            credit.setStatus(CreditStatus.IN_REPAYMENT);
        }

        BigDecimal newTotalPaid = totalPaid.add(amountToPay).setScale(2, RoundingMode.HALF_UP);
        BigDecimal newRemaining = totalPayable.subtract(newTotalPaid).setScale(2, RoundingMode.HALF_UP);

        if (newRemaining.compareTo(BigDecimal.ZERO) <= 0) {
            credit.setStatus(CreditStatus.CLOSED);
        } else {
            if ((repayment.getStatus() == RepaymentStatus.PAID) || (repayment.getStatus() == RepaymentStatus.LATE)) {
                credit.setDueDate(credit.getDueDate().plusMonths(1));
            }
        }

        creditRepository.save(credit);
        return saved;
    }

    /**
     * Retourne le montant restant à payer pour un crédit
     */
    @Override
    public BigDecimal getRemainingAmount(Long creditId) {
        Credit credit = creditRepository.findById(creditId)
                .orElseThrow(() -> new IllegalArgumentException("Credit introuvable"));

        BigDecimal monthly = BigDecimal.valueOf(credit.getMonthlyPayment()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalPayable = monthly.multiply(BigDecimal.valueOf(credit.getDurationInMonths())).setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalPaid = repaymentRepository.sumPaidSuccess(creditId);
        if (totalPaid == null) totalPaid = BigDecimal.ZERO;
        totalPaid = totalPaid.setScale(2, RoundingMode.HALF_UP);

        return totalPayable.subtract(totalPaid).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public List<Repayment> getRepaymentsByCreditId(Long creditId) {
        return repaymentRepository.findByCredit_CreditId(creditId);
    }

    @Override
    public List<Repayment> getRepaymentsByClientEmail(String email) {
        return repaymentRepository.findByClient_Email(email);
    }

    public List<AnnualAmortissementDTO> generateAnnualAmortissement(Long creditId) {
        Credit credit = creditRepository.findById(creditId)
                .orElseThrow(() -> new IllegalArgumentException("Credit introuvable"));

        List<AnnualAmortissementDTO> result = new ArrayList<>();

        double capitalRestant = credit.getAmount();
        double tauxMensuel = (credit.getInterestRate() / 100.0) / 12.0;
        double mensualite = credit.getMonthlyPayment();
        int dureeMois = credit.getDurationInMonths();

        for (int annee = 1; annee <= dureeMois / 12; annee++) {
            double capitalDebutAnnee = capitalRestant;
            double totalInteretsAnnee = 0;
            double totalAmortissementAnnee = 0;

            for (int mois = 1; mois <= 12; mois++) {
                double interetMois = capitalRestant * tauxMensuel;
                double amortissementMois = mensualite - interetMois;

                capitalRestant -= amortissementMois;
                totalInteretsAnnee += interetMois;
                totalAmortissementAnnee += amortissementMois;
            }

            result.add(new AnnualAmortissementDTO(
                    annee,
                    round2(capitalDebutAnnee),
                    round2(totalInteretsAnnee),
                    round2(totalAmortissementAnnee),
                    round2(mensualite * 12),
                    round2(Math.max(0, capitalRestant))
            ));
        }
        return result;
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    // ===========================================
    // ✅ MÉTHODE PDF AVEC INFORMATIONS CLIENT
    // ===========================================
    @Override
    public byte[] generateAmortissementPdf(Long creditId) throws IOException {
        Credit credit = creditRepository.findById(creditId)
                .orElseThrow(() -> new IllegalArgumentException("Credit non trouvé"));

        Client client = credit.getClient();
        if (client == null) {
            throw new IllegalArgumentException("Ce crédit n'est pas associé à un client");
        }

        BigDecimal remainingAmount = getRemainingAmount(creditId);
        BigDecimal totalPaid = repaymentRepository.sumPaidSuccess(creditId);
        if (totalPaid == null) totalPaid = BigDecimal.ZERO;

        List<AnnualAmortissementDTO> data = generateAnnualAmortissement(creditId);

        Context context = new Context();

        context.setVariable("clientNom", client.getLastName() != null ? client.getLastName() : "");
        context.setVariable("clientPrenom", client.getFirstName() != null ? client.getFirstName() : "");
        context.setVariable("clientNomComplet",
                (client.getFirstName() != null ? client.getFirstName() : "") + " " +
                        (client.getLastName() != null ? client.getLastName() : ""));
        context.setVariable("clientTelephone", client.getTelephone() != null ? client.getTelephone() : "Non renseigné");
        context.setVariable("clientEmail", client.getEmail() != null ? client.getEmail() : "Non renseigné");

        context.setVariable("creditId", credit.getCreditId());
        context.setVariable("creditReference", "CRD-" + credit.getCreditId());
        context.setVariable("montantTotal", String.format("%.2f", credit.getAmount()) + " TND");
        context.setVariable("tauxInteret", String.format("%.2f", credit.getInterestRate()) + " %");
        context.setVariable("dureeMois", credit.getDurationInMonths() + " mois");
        context.setVariable("dureeAnnees", (credit.getDurationInMonths() / 12) + " ans");
        context.setVariable("mensualite", String.format("%.2f", credit.getMonthlyPayment()) + " TND");

        context.setVariable("montantRestant", String.format("%.2f", remainingAmount) + " TND");
        context.setVariable("montantPaye", String.format("%.2f", totalPaid) + " TND");

        if (credit.getStartDate() != null) {
            context.setVariable("dateDebut", credit.getStartDate().toString());
        } else {
            context.setVariable("dateDebut", "Non définie");
        }

        context.setVariable("rows", data);
        context.setVariable("dateGeneration", LocalDate.now().toString());

        String htmlContent = templateEngine.process("amortissement", context);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(htmlContent);
            renderer.layout();
            renderer.createPDF(outputStream);
            return outputStream.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException("Erreur génération PDF: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void sendAmortissementPdfByEmail(Long creditId) throws IOException {
        Credit credit = creditRepository.findById(creditId)
                .orElseThrow(() -> new IllegalArgumentException("Credit non trouvé"));

        Client client = credit.getClient();
        if (client == null || client.getEmail() == null) {
            throw new IllegalArgumentException("Client ou email non trouvé");
        }

        byte[] pdfContent = generateAmortissementPdf(creditId);

        String clientName = client.getFirstName() + " " + client.getLastName();
        creditEmailService.sendEmailWithPdf(client.getEmail(), clientName, creditId, pdfContent);

        System.out.println("✅ PDF envoyé par email à " + client.getEmail());
    }

    // ===========================================
    // ✅ MÉTHODES STRIPE
    // ===========================================

    @Override
    public RepaymentDTO createStripePaymentIntent(Long creditId, BigDecimal amount, String currency) {
        try {
            Credit credit = creditRepository.findById(creditId)
                    .orElseThrow(() -> new IllegalArgumentException("Crédit non trouvé"));

            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Montant invalide");
            }

            PaymentIntent paymentIntent = stripePaymentService.createPaymentIntent(creditId, amount, currency);

            RepaymentDTO dto = new RepaymentDTO();
            dto.setCreditId(creditId);
            dto.setAmount(amount);
            dto.setCurrency(currency);
            dto.setPaymentIntentId(paymentIntent.getId());
            dto.setClientSecret(paymentIntent.getClientSecret());

            return dto;

        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la création du PaymentIntent: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void processSuccessfulStripePayment(Long creditId, BigDecimal amount) {
        Credit credit = creditRepository.findById(creditId)
                .orElseThrow(() -> new IllegalArgumentException("Credit non trouvé"));

        // Vérifier si le paiement n'a pas déjà été traité
        String reference = "STRIPE-" + UUID.randomUUID().toString().substring(0, 8);

        if (repaymentRepository.existsByReference(reference)) {
            throw new RuntimeException("Ce paiement a déjà été traité");
        }

        Repayment repayment = new Repayment();
        repayment.setCredit(credit);
        repayment.setAmount(amount);
        repayment.setPaymentDate(LocalDate.now());
        repayment.setPaymentMethod(PaymentMethod.STRIPE);
        repayment.setReference(reference);
        repayment.setClient(credit.getClient());

        if (repayment.getPaymentDate().isAfter(credit.getDueDate())) {
            repayment.setStatus(RepaymentStatus.LATE);
        } else {
            repayment.setStatus(RepaymentStatus.PAID);
        }

        repaymentRepository.save(repayment);
        System.out.println("✅ Paiement Stripe enregistré: " + repayment.getReference());

        updateCreditStatusAfterPayment(credit);
    }

    /**
     * Met à jour le statut du crédit après un paiement
     */
    private void updateCreditStatusAfterPayment(Credit credit) {
        BigDecimal totalPaid = repaymentRepository.sumPaidSuccess(credit.getCreditId());
        if (totalPaid == null) totalPaid = BigDecimal.ZERO;

        BigDecimal monthly = BigDecimal.valueOf(credit.getMonthlyPayment()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalPayable = monthly.multiply(BigDecimal.valueOf(credit.getDurationInMonths()))
                .setScale(2, RoundingMode.HALF_UP);

        if (totalPaid.compareTo(totalPayable) >= 0) {
            credit.setStatus(CreditStatus.CLOSED);
            System.out.println("✅ Crédit N°" + credit.getCreditId() + " est maintenant CLOSED");
        } else if (credit.getStatus() == CreditStatus.APPROVED) {
            credit.setStatus(CreditStatus.IN_REPAYMENT);
            System.out.println("🔄 Crédit N°" + credit.getCreditId() + " est maintenant IN_REPAYMENT");
        }

        creditRepository.save(credit);
    }
}