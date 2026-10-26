package com.lendahand.services;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Geocodes plain-text addresses into GPS coordinates using the free
 * OpenStreetMap Nominatim API — no API key required.
 *
 * Returns double[] { latitude, longitude }, or null on failure.
 */
public class LocationService {

    private static final String NOMINATIM_URL =
        "https://nominatim.openstreetmap.org/search?format=json&limit=1&q=";

    private static final HttpClient CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    /**
     * Converts a human-readable address to [lat, lon].
     *
     * @param address e.g. "Lahore, Pakistan" or "Main Market, Gulberg"
     * @return double[]{lat, lon} or null if geocoding fails
     */
    public static double[] getCoordinates(String address) {
        try {
            String encoded = URLEncoder.encode(address, StandardCharsets.UTF_8);
            String url     = NOMINATIM_URL + encoded;

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                // Nominatim requires a User-Agent header
                .header("User-Agent", "LendAHand-App/1.0 (student project)")
                .GET()
                .timeout(Duration.ofSeconds(10))
                .build();

            HttpResponse<String> response =
                CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JSONArray arr = new JSONArray(response.body());
                if (!arr.isEmpty()) {
                    JSONObject first = arr.getJSONObject(0);
                    double lat = Double.parseDouble(first.getString("lat"));
                    double lon = Double.parseDouble(first.getString("lon"));
                    return new double[]{lat, lon};
                }
            }
        } catch (Exception e) {
            System.err.println("[LocationService] Geocoding failed: " + e.getMessage());
        }
        return null;
    }
}
