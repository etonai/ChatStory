package com.chatstory.ui;

import com.chatstory.canon.CanonStore;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public class LeftPanePanel extends JPanel {

    private final OutputPanel outputPanel;

    public LeftPanePanel(CanonStore canonStore, Consumer<String> onSendPrompt,
                         Runnable beforeFocusRequest) {
        super(new BorderLayout());
        CanonPanel canonPanel = new CanonPanel(canonStore, beforeFocusRequest);
        outputPanel = new OutputPanel(canonStore, canonPanel::appendEntry, onSendPrompt);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Response", outputPanel);
        tabs.addTab("Canon", canonPanel);

        add(tabs, BorderLayout.CENTER);
    }

    public void setResponse(String text) {
        outputPanel.setResponse(text);
    }

    public void clearResponse() {
        outputPanel.clear();
    }
}
