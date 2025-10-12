package com.anurag.jenkins.chatbot;

import hudson.model.Action;
import hudson.model.Run;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.WebMethod;
import org.kohsuke.stapler.verb.POST;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.List;

public class ChatbotAction implements Action {

    // This field is only used when the page is first loaded.
    private final transient Run<?, ?> run;
    private String chatbotResponse;

    public ChatbotAction(Run<?, ?> run) {
        this.run = run;
    }

    public String getChatbotResponse() {
        return chatbotResponse;
    }

    public Run<?, ?> getRun() {
        return run;
    }

    @POST
    @WebMethod(name = "query")
    public void doQuery(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
        // 1. Get the current build from the web request's context
        Run<?, ?> currentRun = req.findAncestorObject(Run.class);

        // 2. Add a null check for safety
        if (currentRun == null) {
            this.chatbotResponse = "Error: Could not find the context of the current build.";
            rsp.forwardToPreviousPage(req);
            return;
        }

        String userQuery = req.getParameter("userQuery");
        if (userQuery != null && !userQuery.trim().isEmpty()) {
            // 3. Use the 'currentRun' object, not 'this.run'
            List<String> logs = currentRun.getLog(Integer.MAX_VALUE);
            this.chatbotResponse = ChatbotApi.askChatbot(userQuery, logs);
        } else {
            this.chatbotResponse = "Please ask a question.";
        }

        rsp.forwardToPreviousPage(req);
    }

    // --- Standard Action methods ---
    @Override
    public String getIconFileName() {
        return "robot.png";
    }

    @Override
    public String getDisplayName() {
        return "Chatbot";
    }

    @Override
    public String getUrlName() {
        return "chatbot";
    }
}