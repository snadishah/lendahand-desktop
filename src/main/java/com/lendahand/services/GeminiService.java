package com.lendahand.services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * AI Service using Google Gemini API.
 * Get your FREE key at: https://aistudio.google.com/app/apikey
 */
public class GeminiService {

    // Read from the GEMINI_API_KEY environment variable — never hardcode keys.
    public static String API_KEY = System.getenv("GEMINI_API_KEY");

    private static final String BASE_URL =
        "https://generativelanguage.googleapis.com/v1beta/models/" +
        "gemini-2.5-flash:generateContent?key=";

    private static final HttpClient CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(15))
        .build();

    public static String getPriceEstimate(String taskDescription) {
        String prompt =
            "You are a helpful assistant for LendAHand, a local task marketplace in Pakistan. " +
            "A user posted this task: \"" + taskDescription + "\". " +
            "Suggest a fair price range in Pakistani Rupees (Rs.) that a student helper " +
            "should charge. Also give a rough time estimate. " +
            "Keep your response brief (3-4 sentences), friendly and practical.";
        return callGemini(buildBody(prompt, null));
    }

    public static String chat(String userMessage, List<Map<String, String>> history) {
        JSONArray contents = new JSONArray();

        // System context as first turn
        JSONObject sys = new JSONObject();
        sys.put("role", "user");
        sys.put("parts", new JSONArray().put(new JSONObject().put("text",
            "You are a friendly assistant for LendAHand, a local task marketplace in Pakistan " +
            "where people post tasks and helpers earn money in Pakistani Rupees (Rs.). " +
            "Help users with posting tasks, bidding, using the wallet, and pricing.")));
        contents.put(sys);

        JSONObject ack = new JSONObject();
        ack.put("role", "model");
        ack.put("parts", new JSONArray().put(new JSONObject().put("text",
            "Got it! I am ready to help LendAHand users.")));
        contents.put(ack);

        for (Map<String, String> turn : history) {
            JSONObject obj = new JSONObject();
            String role = "model".equals(turn.get("role")) ? "model" : "user";
            obj.put("role", role);
            obj.put("parts", new JSONArray().put(new JSONObject().put("text", turn.get("text"))));
            contents.put(obj);
        }

        JSONObject cur = new JSONObject();
        cur.put("role", "user");
        cur.put("parts", new JSONArray().put(new JSONObject().put("text", userMessage)));
        contents.put(cur);

        JSONObject body = new JSONObject();
        body.put("contents", contents);
        body.put("generationConfig", new JSONObject().put("maxOutputTokens", 4096));
        return callGemini(body.toString());
    }

    private static String buildBody(String prompt, List<Map<String, String>> history) {
        JSONObject part    = new JSONObject().put("text", prompt);
        JSONArray  parts   = new JSONArray().put(part);
        JSONObject content = new JSONObject().put("parts", parts);
        JSONArray  contents = new JSONArray().put(content);
        JSONObject body    = new JSONObject();
        body.put("contents", contents);
        body.put("generationConfig", new JSONObject().put("maxOutputTokens", 2000));
        return body.toString();
    }

    private static String callGemini(String requestBody) {
        if (API_KEY == null || API_KEY.isBlank()) {
            return "Gemini API key not set.\n\n" +
                   "Get your free key at:\nhttps://aistudio.google.com/app/apikey\n\n" +
                   "Then set the GEMINI_API_KEY environment variable and restart.";
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + API_KEY))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .timeout(Duration.ofSeconds(30))
                .build();

            HttpResponse<String> response =
                CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JSONObject json = new JSONObject(response.body());
                return json.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")
                    .trim();
            } else if (response.statusCode() == 429) {
                return "Rate limit reached. Please wait a moment and try again.\n" +
                       "(Gemini free tier: 15 requests/minute)";
            } else {
                return "API Error " + response.statusCode() +
                       ". Check your API key at https://aistudio.google.com/app/apikey";
            }
        } catch (Exception e) {
            return "Connection error: " + e.getMessage();
        }
    }
}