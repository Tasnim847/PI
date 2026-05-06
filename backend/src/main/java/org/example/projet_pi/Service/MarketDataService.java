package org.example.projet_pi.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class MarketDataService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Cache pour les taux (évite trop d'appels API)
    private Map<String, Object> cache = new HashMap<>();
    private LocalDateTime cacheTime = null;
    private static final long CACHE_DURATION_MINUTES = 5;

    // Taux de fallback (mis à jour manuellement dans application.yml)
    @Value("${taux.eur-tnd:3.37}")
    private double eurTndFallback;

    @Value("${taux.usd-tnd:3.10}")
    private double usdTndFallback;

    public MarketDataService() {
        this.webClient = WebClient.builder()
                .baseUrl("https://api.exchangerate-api.com/v4")
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                .build();
    }

    /**
     * Récupère le taux EUR/TND en temps réel
     */
    public String getEurTndRate() {
        // Vérifier le cache
        if (isCacheValid() && cache.containsKey("eur_tnd")) {
            double rate = (double) cache.get("eur_tnd");
            String date = (String) cache.get("last_update");
            return String.format("💱 **Taux EUR/TND** : 1 EUR = %.2f TND\n📅 Mise à jour : %s", rate, date);
        }

        try {
            String response = webClient.get()
                    .uri("/latest/EUR")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode json = objectMapper.readTree(response);
            double tndRate = json.path("rates").path("TND").asDouble();
            String lastUpdate = json.path("date").asText();

            // Mettre en cache
            cache.put("eur_tnd", tndRate);
            cache.put("last_update", lastUpdate);
            cacheTime = LocalDateTime.now();

            return String.format("💱 **Taux EUR/TND en temps réel** : 1 EUR = %.2f TND\n📅 Mise à jour : %s",
                    tndRate, lastUpdate);

        } catch (Exception e) {
            // Fallback si API indisponible
            return String.format("💱 **Taux EUR/TND** : 1 EUR = %.2f TND (taux indicatif)\n💡 Consultez l'application pour le taux exact",
                    eurTndFallback);
        }
    }

    /**
     * Récupère le taux USD/TND en temps réel
     */
    public String getUsdTndRate() {
        if (isCacheValid() && cache.containsKey("usd_tnd")) {
            double rate = (double) cache.get("usd_tnd");
            return String.format("💱 **Taux USD/TND** : 1 USD = %.2f TND", rate);
        }

        try {
            String response = webClient.get()
                    .uri("/latest/USD")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode json = objectMapper.readTree(response);
            double tndRate = json.path("rates").path("TND").asDouble();

            cache.put("usd_tnd", tndRate);
            cacheTime = LocalDateTime.now();

            return String.format("💱 **Taux USD/TND en temps réel** : 1 USD = %.2f TND", tndRate);

        } catch (Exception e) {
            return String.format("💱 **Taux USD/TND** : 1 USD = %.2f TND (taux indicatif)", usdTndFallback);
        }
    }

    /**
     * Récupère un taux de change entre deux devises
     */
    public String getRate(String from, String to) {
        try {
            String response = webClient.get()
                    .uri("/latest/" + from.toUpperCase())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode json = objectMapper.readTree(response);
            double rate = json.path("rates").path(to.toUpperCase()).asDouble();

            return String.format("💱 **Taux de change** : 1 %s = %.2f %s",
                    from.toUpperCase(), rate, to.toUpperCase());

        } catch (Exception e) {
            return String.format("💱 Désolé, je n'ai pas pu récupérer le taux %s/%s. Consultez l'application.",
                    from.toUpperCase(), to.toUpperCase());
        }
    }

    /**
     * Récupère l'actualité boursière
     */
    public String getStockMarketNews() {
        String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

        // Pour une vraie API, utilisez Alpha Vantage ou Yahoo Finance
        // Voici un template avec données simulées mais réalistes
        return String.format("""
                📈 **Marchés boursiers - %s
                
                🇪🇺 **CAC 40** : 8,120.45 📉 (-0.32%%)
                🇫🇷 **SBF 120** : 5,890.23 📈 (+0.15%%)
                🇺🇸 **S&P 500** : 5,234.18 📈 (+0.15%%)
                🇺🇸 **NASDAQ** : 18,342.32 📈 (+0.67%%)
                🇩🇪 **DAX** : 18,456.78 📉 (-0.21%%)
                🇬🇧 **FTSE 100** : 8,234.56 📈 (+0.08%%)
                
                ⚡ **Actualités** :
                • La BCE maintient ses taux directeurs
                • Wall Street termine en hausse
                • Le pétrole brut stable autour de 85$
                
                💡 *Pour des données en temps réel, consultez l'onglet "Investissements"*
                """, today);
    }

    /**
     * Vérifie si le cache est encore valide
     */
    private boolean isCacheValid() {
        if (cacheTime == null) return false;
        return LocalDateTime.now().isBefore(cacheTime.plusMinutes(CACHE_DURATION_MINUTES));
    }

    /**
     * Normalise le nom de la devise vers son code ISO
     */
    public String normalizeCurrency(String currency) {
        if (currency == null) return "EUR";
        switch(currency.toLowerCase()) {
            case "euro": return "EUR";
            case "dollar":
            case "usd":
            case "dollar us":
            case "us dollar":
                return "USD";
            case "gbp":
            case "livre":
            case "livre sterling":
                return "GBP";
            case "chf":
            case "franc":
            case "franc suisse":
                return "CHF";
            case "cad":
            case "dollar canadien":
                return "CAD";
            case "dinar":
            case "tnd":
            case "dinar tunisien":
                return "TND";
            case "jpy":
            case "yen":
                return "JPY";
            default: return currency.toUpperCase();
        }
    }
}