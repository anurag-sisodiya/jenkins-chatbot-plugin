package com.anurag.jenkins.chatbot;

import hudson.Extension;
import jenkins.model.GlobalConfiguration;
import org.kohsuke.stapler.DataBoundSetter;

@Extension
public class ChatbotGlobalConfiguration extends GlobalConfiguration {

    private String apiUrl;
    private String modelName;

    public ChatbotGlobalConfiguration() {
        // Load the saved configuration
        load();
    }

    public String getApiUrl() {
        // Provide a sensible default if not configured
        return apiUrl != null ? apiUrl : "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";
    }

    @DataBoundSetter
    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
        save();
    }

    public String getModelName() {
        // This is not used by Gemini but is good practice for other APIs
        return modelName;
    }

    @DataBoundSetter
    public void setModelName(String modelName) {
        this.modelName = modelName;
        save();
    }
}