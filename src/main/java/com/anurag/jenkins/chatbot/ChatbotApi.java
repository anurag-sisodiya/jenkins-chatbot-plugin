package com.anurag.jenkins.chatbot;

import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import hudson.security.ACL;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ChatbotApi {

    private static final String CREDENTIALS_ID = "gemini-api-key";

    // ✅ This URL uses the exact model name available to your API key.
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";

    private static String getApiKey() {
        StringCredentials credential = CredentialsProvider.lookupCredentials(
                        StringCredentials.class,
                        Jenkins.get(),
                        ACL.SYSTEM,
                        Collections.emptyList()
                ).stream()
                .filter(c -> c.getId().equals(CREDENTIALS_ID))
                .findFirst()
                .orElse(null);

        return (credential != null) ? credential.getSecret().getPlainText() : null;
    }

    public static String askChatbot(String query, List<String> logs) {
        String apiKey = getApiKey();
        if (StringUtils.isEmpty(apiKey)) {
            return "ERROR: API Key not found. Please configure a 'Secret text' credential with the ID '" + CREDENTIALS_ID + "' in Jenkins Global Credentials.";
        }

        String logsAsString = String.join("\n", logs);
        String prompt = "You are a helpful Jenkins build assistant. Analyze the following build logs and answer the user's question.\n\n" +
                "--- BUILD LOGS ---\n" + logsAsString + "\n\n" +
                "--- QUESTION ---\n" + query;

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(API_URL + "?key=" + apiKey);
            request.setHeader("Content-Type", "application/json");

            JSONObject payload = new JSONObject();
            JSONArray contents = new JSONArray();
            JSONObject content = new JSONObject();
            JSONArray parts = new JSONArray();
            JSONObject part = new JSONObject();
            part.put("text", prompt);
            parts.put(part);
            content.put("parts", parts);
            contents.put(content);
            payload.put("contents", contents);

            request.setEntity(new StringEntity(payload.toString(), StandardCharsets.UTF_8));

            return httpClient.execute(request, response -> {
                String responseBody;
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), StandardCharsets.UTF_8))) {
                    responseBody = reader.lines().collect(Collectors.joining("\n"));
                }

                JSONObject jsonResponse = new JSONObject(responseBody);
                if (jsonResponse.has("error")) {
                    return "API Error: " + jsonResponse.getJSONObject("error").getString("message");
                }

                if (jsonResponse.getJSONArray("candidates").getJSONObject(0).has("finishReason") &&
                        "SAFETY".equals(jsonResponse.getJSONArray("candidates").getJSONObject(0).getString("finishReason"))) {
                    return "Response was blocked due to safety concerns.";
                }

                if (!jsonResponse.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").has("parts")) {
                    return "The model returned an empty response.";
                }

                return jsonResponse.getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text");
            });

        } catch (Exception e) {
            e.printStackTrace();
            return "Error communicating with the API: " + e.getMessage();
        }
    }
}