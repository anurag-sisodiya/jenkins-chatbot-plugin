package com.anurag.jenkins.chatbot;

import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;

import javax.annotation.Nonnull;

@Extension
public class ChatbotRunListener extends RunListener<Run<?, ?>> {

    /**
     * Called when a build is started.
     * We use this hook to add our action.
     * @param run The build that is starting.
     * @param listener The task listener.
     */
    @Override
    public void onStarted(@Nonnull Run<?, ?> run, @Nonnull TaskListener listener) {
        // Add our custom action to the build
        run.addAction(new ChatbotAction(run));
    }
}