package com.anurag.jenkins.chatbot;

import hudson.model.Action;
import hudson.model.Run;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.WebMethod;
import org.kohsuke.stapler.interceptor.RequirePOST;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.List;
import org.kohsuke.stapler.Stapler;
import jenkins.model.Jenkins;
import hudson.security.csrf.CrumbIssuer;


public class ChatbotAction implements Action {

    private final transient Run<?, ?> run;
    private String chatbotResponse;

    public ChatbotAction(Run<?, ?> run) {
        this.run = run;
    }
    public String getCrumb() {
        StaplerRequest req = Stapler.getCurrentRequest();
        if (req == null) return null;
        CrumbIssuer issuer = Jenkins.get().getCrumbIssuer();
        if (issuer == null) return null;
        return issuer.getCrumb(req);
    }

    // 👇 AND ADD THIS METHOD
    public String getCrumbField() {
        StaplerRequest req = Stapler.getCurrentRequest();
        if (req == null) return null;
        CrumbIssuer issuer = Jenkins.get().getCrumbIssuer();
        if (issuer == null) return null;
        return issuer.getCrumbRequestField();
    }
    @Override public String getDisplayName() { return "Chatbot"; }
    @Override public String getUrlName() { return "chatbot"; }
    @Override public String getIconFileName() { return "/plugin/jenkins-chatbot-plugin/images/chatbot-icon.png"; }

    public String getChatbotResponse() { return chatbotResponse; }
    public Run<?, ?> getRun() { return run; }

    @RequirePOST
    public void doSubmit(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
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


}