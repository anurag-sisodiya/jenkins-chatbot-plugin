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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ChatbotApi {

    private static final Logger LOGGER = Logger.getLogger(ChatbotApi.class.getName());

    // This ID is used to look up the credential in Jenkins.
    // It's recommended to make this configurable in the future for advanced use cases.
    private static final String CREDENTIALS_ID = "gemini-api-key";

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

        if (credential == null) {
            LOGGER.warning("Could not find credential with ID: " + CREDENTIALS_ID);
            return null;
        }
        return credential.getSecret().getPlainText();
    }

    /**
     * Main method that communicates with the generative AI service.
     * @param query The user's question.
     * @param logs The list of log lines from the build.
     * @return The AI's response as a string, or an error message.
     */
    public static String askChatbot(String query, List<String> logs) {
        String apiKey = getApiKey();
        if (StringUtils.isEmpty(apiKey)) {
            return "ERROR: API Key not found. Please configure a 'Secret text' credential with the ID '" + CREDENTIALS_ID + "' in Jenkins Global Credentials.";
        }

        String apiUrl = Jenkins.get().getDescriptorByType(ChatbotGlobalConfiguration.class).getApiUrl();
        if (StringUtils.isEmpty(apiUrl)) {
            return "ERROR: API URL is not configured in Manage Jenkins -> Configure System.";
        }

        String logsAsString = String.join("\n", logs);
        String prompt = "You are a helpful Jenkins build assistant. Analyze the following build logs and answer the user's question.\n\n" +
                "--- BUILD LOGS ---\n" + logsAsString + "\n\n" +
                "--- QUESTION ---\n" + query;

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            // 2. Use the dynamically configured API URL.
            HttpPost request = new HttpPost(apiUrl + "?key=" + apiKey);
            request.setHeader("Content-Type", "application/json");

            // Construct the JSON payload required by the Google Gemini API.
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

            LOGGER.info("Sending request to AI API at: " + apiUrl);

            return httpClient.execute(request, response -> {
                String responseBody;
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), StandardCharsets.UTF_8))) {
                    responseBody = reader.lines().collect(Collectors.joining("\n"));
                }

                // Log the raw response for easier debugging if there's an issue.
                LOGGER.fine("Received raw API response: " + responseBody);

                JSONObject jsonResponse = new JSONObject(responseBody);
                if (jsonResponse.has("error")) {
                    String errorMessage = jsonResponse.getJSONObject("error").getString("message");
                    LOGGER.severe("API returned an error: " + errorMessage);
                    return "API Error: " + errorMessage;
                }

                if (jsonResponse.getJSONArray("candidates").getJSONObject(0).has("finishReason") &&
                        "SAFETY".equals(jsonResponse.getJSONArray("candidates").getJSONObject(0).getString("finishReason"))) {
                    LOGGER.warning("Response was blocked due to safety concerns.");
                    return "Response was blocked due to safety concerns.";
                }

                if (!jsonResponse.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").has("parts")) {
                    LOGGER.warning("Model returned an empty response content.");
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
            // Log the full exception for administrators.
            LOGGER.log(Level.SEVERE, "Error communicating with the Chatbot API", e);
            // Return a user-friendly error message.
            return "Error communicating with the API: " + e.getMessage();
        }
    }
}