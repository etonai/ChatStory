package com.chatstory;

import com.chatstory.bridge.ChatGptBridge;
import com.chatstory.bridge.ResponseListener;
import com.chatstory.browser.BrowserPanel;
import com.chatstory.ui.InputPanel;
import org.cef.browser.CefBrowser;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class AppFrame extends JFrame {

    private final JLabel statusLabel;

    public AppFrame(AppState appState, BrowserPanel browserPanel, CefBrowser browser,
                    ChatGptBridge chatBridge) {
        super("Story Workstation");

        setSize(1400, 900);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        statusLabel = new JLabel(" Starting...");
        statusLabel.setForeground(Color.DARK_GRAY);

        JButton devToolsBtn = new JButton("DevTools");
        devToolsBtn.setToolTipText("Open Chromium DevTools for this page");
        devToolsBtn.addActionListener(e -> browser.openDevTools());

        JButton testInjectBtn = new JButton("Test Inject");
        testInjectBtn.setToolTipText("Inject a hardcoded prompt without sending");
        testInjectBtn.addActionListener(e -> chatBridge.testInjectPrompt(
                "DC3 test injection",
                statusResponseListener("Test prompt injected")));

        JPanel leftTools = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        leftTools.add(devToolsBtn);
        leftTools.add(testInjectBtn);

        JPanel toolbar = new JPanel(new BorderLayout(6, 0));
        toolbar.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        toolbar.add(leftTools, BorderLayout.WEST);
        toolbar.add(statusLabel, BorderLayout.CENTER);

        add(toolbar, BorderLayout.NORTH);
        add(browserPanel.getUIComponent(), BorderLayout.CENTER);
        add(new InputPanel(appState, chatBridge, statusResponseListener("Prompt submitted")),
                BorderLayout.SOUTH);

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dispose();
                System.exit(0);
            }
        });

        appState.addListener((prev, current) ->
                UiThread.run(() -> statusLabel.setText(" " + labelFor(current))));

        setVisible(true);
    }

    private String labelFor(AppState.State state) {
        switch (state) {
            case Starting:        return "Starting...";
            case LoadingChatGPT:  return "Loading ChatGPT...";
            case NeedsLogin:      return "Please log in to ChatGPT";
            case Ready:           return "Ready";
            case InjectingPrompt: return "Injecting prompt...";
            case Sending:         return "Sending prompt...";
            case Complete:        return "Complete";
            case Error:           return "Error - check console";
            default:              return state.name();
        }
    }

    private ResponseListener statusResponseListener(String successText) {
        return new ResponseListener() {
            @Override
            public void onPromptSubmitted(long requestId) {
                UiThread.run(() -> statusLabel.setText(" " + successText));
            }

            @Override
            public void onResponsePartial(long requestId, String responseText) {
                // DC4 handles response streaming/extraction.
            }

            @Override
            public void onResponseComplete(long requestId, String responseText) {
                // DC4 handles response completion.
            }

            @Override
            public void onError(long requestId, String errorCode, String message) {
                UiThread.run(() -> statusLabel.setText(" Error: " + errorCode + " - " + message));
            }
        };
    }
}
