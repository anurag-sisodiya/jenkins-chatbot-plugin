# Jenkins Console Chatbot Plugin

A smart assistant for your Jenkins builds — ask questions about build logs, errors, and execution directly from the Jenkins console.

## Features

- Real-time interaction with your Jenkins build console.
- Ask questions about build logs, warnings, or errors without manually scrolling.
- Provides contextual responses based on the current build.
- Lightweight and easy to install, no extra infrastructure needed.
- Designed to improve developer productivity and reduce troubleshooting time.

## Installation

1. **Download the Plugin**
   - Build the plugin from source:
     ```
     mvn clean install
     ```
   - This generates `target/jenkins-chatbot.hpi`.

2. **Install in Jenkins**
   - Go to `Manage Jenkins → Manage Plugins → Advanced → Upload Plugin`.
   - Upload the `jenkins-chatbot.hpi` file.
   - Restart Jenkins if required.

3. **Verify Installation**
   - After installation, navigate to any build console.
   - You should see the Chatbot panel below the console output.

## Usage

1. Open any Jenkins job build console.
2. Scroll down to the Chatbot panel.
3. Ask questions like:
   - "Why did this build fail?"
   - "Show me all warnings in this build."
   - "Summarize the build steps."
4. The chatbot will provide insights directly based on the build logs.
