package org.example.projet_pi.Service;

import org.example.projet_pi.Dto.ChatMessageDTO;
import org.example.projet_pi.Dto.ChatResponseDTO;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class IntelligentBankingAssistant {

    private final OllamaService ollamaService;
    private final BankingToolService bankingTool;
    private final Map<String, Map<String, Object>> conversationContext = new HashMap<>();

    // ✅ CONSTRUCTEUR CORRIGÉ
    public IntelligentBankingAssistant(OllamaService ollamaService,
                                       BankingToolService bankingTool) {
        this.ollamaService = ollamaService;
        this.bankingTool = bankingTool;
    }

    public ChatResponseDTO processMessage(ChatMessageDTO message) {
        String userMessage = message.getMessage().toLowerCase().trim();
        Long userId = message.getUserId();
        String sessionId = message.getSessionId();
        String firstName = message.getUserFirstName() != null ? message.getUserFirstName() : "Client";

        Map<String, Object> context = conversationContext.getOrDefault(sessionId, new HashMap<>());

        // 1. Vérifier les actions spécifiques avec le prénom
        String actionResponse = checkForActions(userMessage, userId, context, firstName);
        if (actionResponse != null) {
            return new ChatResponseDTO(actionResponse, getSuggestions(userMessage));
        }

        // 2. Construction du prompt pour Ollama avec le prénom
        String balance = bankingTool.getBalance(userId);
        String accountInfo = bankingTool.getAccountInfo(userId);

        String prompt = buildPrompt(userMessage, balance, accountInfo, context, firstName);

        // 3. Appel à Ollama
        String aiResponse = ollamaService.generateResponse(prompt);

        conversationContext.put(sessionId, context);

        return new ChatResponseDTO(aiResponse, getSuggestions(userMessage));
    }

    private String checkForActions(String message, Long userId, Map<String, Object> context, String firstName) {
        String lowerMsg = message.toLowerCase();

        // SOLDE
        if (lowerMsg.matches(".*(solde|balance|argent|combien j'ai|mon solde).*") &&
                !lowerMsg.contains("historique")) {
            String balance = bankingTool.getBalance(userId);
            return "💰 **" + firstName + "**, votre solde total est de **" + balance + " TND**.\n\nSouhaitez-vous voir vos dernières transactions ?";
        }

        // TRANSACTIONS
        if (lowerMsg.matches(".*(transaction|historique|opération|mouvement|dernier).*")) {
            var transactions = bankingTool.getRecentTransactions(userId, 5);
            if (transactions.isEmpty()) {
                return "📭 " + firstName + ", vous n'avez aucune transaction récente.";
            }
            StringBuilder sb = new StringBuilder("📊 **Vos 5 dernières transactions**\n\n");
            for (var tx : transactions) {
                String sign = tx.get("type").equals("DEPOSIT") ? "+" : "-";
                String emoji = tx.get("type").equals("DEPOSIT") ? "🟢" : "🔴";
                sb.append(String.format("%s %s : %s%.2f TND\n",
                        emoji, tx.get("date"), sign, tx.get("amount")));
            }
            return sb.toString();
        }

        // VIREMENT - AMÉLIORÉ
        if (lowerMsg.matches(".*(virement|envoyer|transfert|virer).*")) {
            java.util.regex.Pattern ripPattern = java.util.regex.Pattern.compile("\\b(\\d{21})\\b");
            var ripMatcher = ripPattern.matcher(message);
            String rip = ripMatcher.find() ? ripMatcher.group(1) : null;

            java.util.regex.Pattern amountPattern = java.util.regex.Pattern.compile("(\\d+[.,]?\\d*)\\s*(tnd|dt|dinard|€)?");
            var amountMatcher = amountPattern.matcher(message);
            Double amount = amountMatcher.find() ? Double.parseDouble(amountMatcher.group(1).replace(",", ".")) : null;

            // Vérifier si c'est une confirmation
            if (lowerMsg.contains("oui") && context.containsKey("pendingTransfer")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> pending = (Map<String, Object>) context.get("pendingTransfer");
                String targetRip = (String) pending.get("targetRip");
                Double pendingAmount = (Double) pending.get("amount");

                String result = bankingTool.transfer(userId, targetRip, pendingAmount, "Virement via IA");
                context.remove("pendingTransfer");
                return result;
            }

            if (lowerMsg.contains("non") && context.containsKey("pendingTransfer")) {
                context.remove("pendingTransfer");
                return "❌ Virement annulé. Puis-je vous aider avec autre chose ?";
            }

            if (rip != null && amount != null) {
                // Demander confirmation
                context.put("pendingTransfer", Map.of("targetRip", rip, "amount", amount));
                return "📝 **Confirmation de virement**\n\n" +
                        "💰 Montant : " + amount + " TND\n" +
                        "📤 Vers le compte : " + rip + "\n\n" +
                        "✅ Répondez **OUI** pour confirmer\n" +
                        "❌ Répondez **NON** pour annuler";
            } else if (rip == null) {
                return "💸 " + firstName + ", pour faire un virement, j'ai besoin du **RIP** (21 chiffres) du destinataire.\nExemple: 100003053814053105923";
            } else {
                return "💸 Quel montant souhaitez-vous transférer ? (ex: 100 TND)";
            }
        }

        // RÉCLAMATION
        if (lowerMsg.matches(".*(r[eé]clamation|plainte|probl[eè]me|incident|réclamer).*")) {
            String result = bankingTool.createComplaint(userId, message);
            return result;
        }

        // ACTUALITÉS / QUESTIONS GÉNÉRALES - Laisser l'IA répondre
        return null; // Pas d'action spécifique → l'IA répondra
    }
    private String buildPrompt(String userMessage, String balance, String accountInfo,
                               Map<String, Object> context, String firstName) {
        return """
        Tu es "SurFin", l'assistant bancaire intelligent de SurFinity Bank.
        
        Le client s'appelle %s.
        
        INFORMATIONS CLIENT:
        - Solde total: %s TND
        - Informations compte: %s
        
        CAPACITÉS:
        - Donner des informations sur la bourse, actualités financières
        - Répondre aux questions générales sur la banque
        - Donner des conseils financiers
        - NE PAS donner de conseils d'investissement spécifiques sans avertissement
        
        RÈGLES:
        - Sois professionnel mais chaleureux
        - Réponds en français
        - Sois concis (2-3 phrases maximum)
        - Utilise des émojis quand c'est approprié
        - APPELLE LE CLIENT PAR SON PRÉNOM: %s
        
        CLIENT: %s
        
        SURFIN:""".formatted(firstName, balance, accountInfo, firstName, userMessage);
    }

    private List<String> getSuggestions(String message) {
        if (message.contains("solde")) {
            return Arrays.asList("📊 Voir transactions", "💸 Faire virement");
        }
        if (message.contains("transaction")) {
            return Arrays.asList("💰 Voir solde", "🔄 Faire virement");
        }
        if (message.contains("virement")) {
            return Arrays.asList("💰 Solde", "📊 Transactions");
        }
        if (message.contains("réclamation")) {
            return Arrays.asList("💰 Solde", "📞 Contacter agence");
        }
        return Arrays.asList("💰 Solde", "📊 Transactions", "💸 Virement", "📝 Réclamation");
    }
}