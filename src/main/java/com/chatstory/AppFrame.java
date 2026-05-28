package com.chatstory;

import com.chatstory.browser.BrowserPanel;
import org.cef.browser.CefBrowser;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class AppFrame extends JFrame {

    private final JLabel statusLabel;

    public AppFrame(AppState appState, BrowserPanel browserPanel, CefBrowser browser) {
        super("Story Workstation");

        setSize(1400, 900);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Status label
        statusLabel = new JLabel(" Starting...");
        statusLabel.setForeground(Color.DARK_GRAY);

        // DevTools button
        JButton devToolsBtn = new JButton("DevTools");
        devToolsBtn.setToolTipText("Open Chromium DevTools for this page");
        devToolsBtn.addActionListener(e -> browser.openDevTools());

        // Toolbar
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        toolbar.add(devToolsBtn, BorderLayout.WEST);
        toolbar.add(statusLabel, BorderLayout.CENTER);

        add(toolbar, BorderLayout.NORTH);
        add(browserPanel.getUIComponent(), BorderLayout.CENTER);

        // Window close
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dispose();
                System.exit(0);
            }
        });

        // Observe AppState — listener uses UiThread defensively so it stays safe
        // even if a future DC calls transition() from a non-EDT thread.
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
            case Error:           return "Error — check console";
            default:              return state.name(); // InjectingPrompt, Sending, etc. set in DC003+
        }
    }
}
