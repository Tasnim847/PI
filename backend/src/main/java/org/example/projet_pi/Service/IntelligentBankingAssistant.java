package org.example.projet_pi.Service;

import org.example.projet_pi.Dto.ChatMessageDTO;
import org.example.projet_pi.Dto.ChatResponseDTO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

@Service
public class IntelligentBankingAssistant {

    private final OllamaService ollamaService;
    private final BankingToolService bankingTool;
    private final MarketDataService marketDataService;  // AJOUTÉ
    private final Map<String, Map<String, Object>> conversationContext = new HashMap<>();
    private final Map<String, Boolean> pendingComplaint = new HashMap<>();

    // CONSTRUCTEUR MIS À JOUR
    public IntelligentBankingAssistant(OllamaService ollamaService,
                                       BankingToolService bankingTool,
                                       MarketDataService marketDataService) {
        this.ollamaService = ollamaService;
        this.bankingTool = bankingTool;
        this.marketDataService = marketDataService;
    }

    public ChatResponseDTO processMessage(ChatMessageDTO message) {
        String userMessage = message.getMessage().trim();
        Long userId = message.getUserId();
        String sessionId = message.getSessionId();
        String firstName = message.getUserFirstName() != null ? message.getUserFirstName() : "Client";
        String lowerMsg = userMessage.toLowerCase();

        Map<String, Object> context = conversationContext.getOrDefault(sessionId, new HashMap<>());

        System.out.println("📩 MESSAGE: " + userMessage);

        // ============ 1. RÉCLAMATIONS ============
        if (pendingComplaint.getOrDefault(sessionId, false)) {
            if (lowerMsg.equals("annuler") || lowerMsg.equals("non")) {
                pendingComplaint.put(sessionId, false);
                return new ChatResponseDTO("❌ Réclamation annulée.", getDefaultSuggestions());
            }
            String result = bankingTool.createComplaint(userId, userMessage);
            pendingComplaint.put(sessionId, false);
            return new ChatResponseDTO(result, getDefaultSuggestions());
        }

        if (lowerMsg.matches(".*(réclamation|plainte|problème|incident|réclamer).*")) {
            pendingComplaint.put(sessionId, true);
            return new ChatResponseDTO(
                    "📝 **Décrivez votre réclamation**\n\nVeuillez écrire votre message ci-dessous :",
                    Arrays.asList("Annuler")
            );
        }

        // ============ 2. CONFIRMATION VIREMENT ============
        if (context.containsKey("pendingTransfer")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> pending = (Map<String, Object>) context.get("pendingTransfer");
            String targetRip = (String) pending.get("targetRip");
            Double amount = (Double) pending.get("amount");

            if (lowerMsg.matches(".*\\b(oui|yes|ok|confirme|valide|d'accord)\\b.*")) {
                String result = bankingTool.transfer(userId, targetRip, amount, "Virement via assistant");
                context.remove("pendingTransfer");
                conversationContext.put(sessionId, context);
                return new ChatResponseDTO(result, getDefaultSuggestions());
            }

            if (lowerMsg.matches(".*\\b(non|annuler|stop|cancel)\\b.*")) {
                context.remove("pendingTransfer");
                conversationContext.put(sessionId, context);
                return new ChatResponseDTO("❌ Virement annulé.", getDefaultSuggestions());
            }

            return new ChatResponseDTO(
                    String.format("📝 **Confirmation virement**\n💰 %.2f TND vers `%s`\n✅ Répondez OUI ou NON", amount, targetRip),
                    Arrays.asList("✅ OUI", "❌ NON")
            );
        }

        // ============ 3. TAUX DE CHANGE (NOUVEAU - PRIORITÉ ÉLEVÉE) ============
        boolean wantsExchangeRate = lowerMsg.matches(".*(euro|usd|dollar|taux change|taux de change|prix euro|prix dollar|exchange rate|conversion|taux du jour).*");

        if (wantsExchangeRate && !lowerMsg.matches(".*(virement|transfert|envoyer).*")) {
            System.out.println("💱 DEMANDE DE TAUX DE CHANGE");

            // Cas spécifique : EUR -> TND
            if (lowerMsg.contains("euro") && (lowerMsg.contains("tnd") || lowerMsg.contains("dinar"))) {
                return new ChatResponseDTO(marketDataService.getEurTndRate(), getDefaultSuggestions());
            }

            // Cas spécifique : USD -> TND
            if ((lowerMsg.contains("dollar") || lowerMsg.contains("usd")) && (lowerMsg.contains("tnd") || lowerMsg.contains("dinar"))) {
                return new ChatResponseDTO(marketDataService.getUsdTndRate(), getDefaultSuggestions());
            }

            // Extraction de devises (ex: "euro vers dollar")
            Pattern currencyPattern = Pattern.compile("(euro|usd|dollar|gbp|chf|cad|livre|franc)\\s*(?:vers|en|to|/|->)\\s*(euro|usd|dollar|gbp|chf|cad|tnd|dinar|livre|franc)", Pattern.CASE_INSENSITIVE);
            Matcher currencyMatcher = currencyPattern.matcher(lowerMsg);
            if (currencyMatcher.find()) {
                String from = currencyMatcher.group(1);
                String to = currencyMatcher.group(2);
                String fromCode = marketDataService.normalizeCurrency(from);
                String toCode = marketDataService.normalizeCurrency(to);
                return new ChatResponseDTO(marketDataService.getRate(fromCode, toCode), getDefaultSuggestions());
            }

            // Réponse générale
            return new ChatResponseDTO(
                    "💱 **Taux de change indicatifs**\n\n" +
                            "• 1 EUR ≈ 3.37 TND\n" +
                            "• 1 USD ≈ 3.10 TND\n" +
                            "• 1 GBP ≈ 3.90 TND\n\n" +
                            "Pour le taux exact du moment, consultez l'onglet 'Taux' de l'application.\n" +
                            "Ou demandez-moi : '1 EUR en TND'",
                    getDefaultSuggestions()
            );
        }

        // ============ 4. BOURSE ET ACTUALITÉS FINANCIÈRES (NOUVEAU) ============
        boolean wantsMarketNews = lowerMsg.matches(".*(bourse|cac|action|indice|marché|marchés|wall street|nasdaq|sp500|actualité bourse|actualités bourse).*");

        if (wantsMarketNews && !lowerMsg.matches(".*(virement|transfert).*")) {
            System.out.println("📈 DEMANDE BOURSE");
            return new ChatResponseDTO(marketDataService.getStockMarketNews(), getDefaultSuggestions());
        }

        // ============ 5. TRANSACTIONS ============
        boolean wantsTransactions = userMessage.contains("📊") ||
                lowerMsg.equals("transactions") ||
                lowerMsg.matches(".*\\b(transactions?|historique|opération|mouvement|dernieres?|activité|mes transactions|voir transactions|afficher transactions)\\b.*");

        if (wantsTransactions) {
            System.out.println("📊 AFFICHAGE DES TRANSACTIONS");
            var transactions = bankingTool.getRecentTransactions(userId, 5);

            if (transactions == null || transactions.isEmpty()) {
                return new ChatResponseDTO(
                        "📭 " + firstName + ", vous n'avez aucune transaction récente.",
                        Arrays.asList("💰 Voir solde", "💸 Faire virement")
                );
            }

            StringBuilder sb = new StringBuilder();
            sb.append("📊 **Vos 5 dernières transactions**\n\n");

            int count = 1;
            for (var tx : transactions) {
                String type = (String) tx.get("type");
                String date = (String) tx.get("date");
                Object amountObj = tx.get("amount");
                double amount = 0;
                if (amountObj instanceof Number) {
                    amount = ((Number) amountObj).doubleValue();
                } else if (amountObj instanceof String) {
                    try { amount = Double.parseDouble((String) amountObj); } catch (Exception e) {}
                }

                String sign = "DEPOSIT".equalsIgnoreCase(type) ? "+" : "-";
                String emoji = "DEPOSIT".equalsIgnoreCase(type) ? "🟢" : "🔴";
                String description = tx.get("description") != null ? " - " + tx.get("description") : "";

                sb.append(String.format("%s %d. %s : %s%.2f TND%s\n",
                        emoji, count, date, sign, amount, description));
                count++;
            }

            return new ChatResponseDTO(sb.toString(), Arrays.asList("💰 Voir solde", "💸 Faire virement"));
        }

        // ============ 6. VIREMENT (RIP + MONTANT) ============
        Pattern ripPattern = Pattern.compile("\\b(\\d{21})\\b");
        Matcher ripMatcher = ripPattern.matcher(userMessage);
        String foundRip = ripMatcher.find() ? ripMatcher.group(1) : null;

        Pattern amountPattern = Pattern.compile("\\b(\\d{1,7}(?:[.,]\\d{1,2})?)\\b");
        Matcher amountMatcher = amountPattern.matcher(userMessage);
        Double foundAmount = null;
        while (amountMatcher.find()) {
            try {
                String amountStr = amountMatcher.group(1).replace(",", ".");
                double testAmount = Double.parseDouble(amountStr);
                if (testAmount > 0 && testAmount < 1000000 && String.valueOf((long)testAmount).length() != 21) {
                    foundAmount = testAmount;
                    break;
                }
            } catch (NumberFormatException ignored) {}
        }

        if (foundRip != null && foundAmount != null) {
            Map<String, Object> pendingTransfer = new HashMap<>();
            pendingTransfer.put("targetRip", foundRip);
            pendingTransfer.put("amount", foundAmount);
            context.put("pendingTransfer", pendingTransfer);
            conversationContext.put(sessionId, context);

            return new ChatResponseDTO(
                    String.format("📝 **Confirmation de virement**\n\n💰 Montant : **%.2f TND**\n📤 Vers le RIP : `%s`\n\n✅ Répondez **OUI** pour confirmer\n❌ Répondez **NON** pour annuler",
                            foundAmount, foundRip),
                    Arrays.asList("✅ OUI", "❌ NON")
            );
        }

        // ============ 7. INTENTION DE VIREMENT ============
        if (lowerMsg.matches(".*\\b(virement|envoyer|transfert|virer|paye|payer)\\b.*")) {
            if (foundRip != null) {
                return new ChatResponseDTO(
                        "💸 **Montant manquant**\n\nRIP reçu : `" + foundRip + "`\nQuel montant voulez-vous transférer ?",
                        Arrays.asList("💰 Voir solde", "❌ Annuler")
                );
            }
            return new ChatResponseDTO(
                    "💸 **Faire un virement**\n\nDonnez-moi :\n1️⃣ Le RIP (21 chiffres)\n2️⃣ Le montant\n\n📝 Exemple : `100001646213812059721 200`",
                    Arrays.asList("💰 Voir solde", "📊 Transactions")
            );
        }

        // ============ 8. SOLDE ============
        if (lowerMsg.matches(".*\\b(solde|balance|argent|combien j'ai|mon solde)\\b.*")) {
            String balance = bankingTool.getBalance(userId);
            return new ChatResponseDTO(
                    String.format("💰 **%s**, votre solde total est de **%s TND**", firstName, balance),
                    Arrays.asList("📊 Voir transactions", "💸 Faire virement")
            );
        }

        // ============ 9. AUTRES QUESTIONS (IA) ============
        String balance = bankingTool.getBalance(userId);
        String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

        String prompt = String.format("""
            Tu es SurFin, assistant bancaire de SurFinity Bank.
            Date: %s
            Client: %s
            Solde: %s TND
            
            RÈGLES IMPORTANTES:
            1. Ne JAMAIS inventer des taux de change
            2. Ne JAMAIS demander la carte bancaire ou code PIN
            3. Pour les taux EUR/TND, dire "environ 3,37 TND"
            4. Réponse courte (2-3 phrases)
            
            Question: "%s"
            
            Réponse:""", today, firstName, balance, userMessage);

        String aiResponse = ollamaService.generateResponse(prompt);

        if (aiResponse == null || aiResponse.isEmpty() ||
                aiResponse.toLowerCase().contains("carte") && aiResponse.toLowerCase().contains("pin") ||
                aiResponse.toLowerCase().contains("1 eur =") && aiResponse.toLowerCase().contains("tnd") && !aiResponse.contains("3.37")) {
            aiResponse = "🤔 Comment puis-je vous aider ?\n\n💰 Consulter mon solde\n📊 Voir mes transactions\n💸 Faire un virement\n📝 Créer une réclamation\n💱 Voir les taux de change";
        }

        conversationContext.put(sessionId, context);
        return new ChatResponseDTO(aiResponse, getDefaultSuggestions());
    }

    private List<String> getDefaultSuggestions() {
        return Arrays.asList("💰 Solde", "📊 Transactions", "💸 Virement", "📝 Réclamation", "💱 Taux EUR/TND");
    }
}