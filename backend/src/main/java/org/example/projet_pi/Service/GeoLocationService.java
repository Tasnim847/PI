// Service/GeoLocationService.java
package org.example.projet_pi.Service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class GeoLocationService {

    private final RestTemplate restTemplate = new RestTemplate();

    public Map<String, Object> getLocationFromIp(String ip) {
        try {
            // Ignorer localhost
            if (ip == null || ip.equals("0:0:0:0:0:0:0:1") || ip.equals("127.0.0.1")) {
                return Map.of(
                        "lat", 36.8065, "lon", 10.1815,
                        "city", "Tunis", "country", "Tunisia"
                );
            }

            String url = "http://ip-api.com/json/" + ip + "?fields=lat,lon,city,country,status";
            Map response = restTemplate.getForObject(url, Map.class);

            if (response != null && "success".equals(response.get("status"))) {
                return Map.of(
                        "lat", response.get("lat"),
                        "lon", response.get("lon"),
                        "city", response.getOrDefault("city", "Unknown"),
                        "country", response.getOrDefault("country", "Unknown")
                );
            }
        } catch (Exception e) {
            System.err.println("GeoLocation error: " + e.getMessage());
        }
        return Map.of("lat", 0.0, "lon", 0.0, "city", "Unknown", "country", "Unknown");
    }
}