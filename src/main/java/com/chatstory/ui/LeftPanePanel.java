package com.chatstory.ui;

import com.chatstory.canon.CanonStore;

import javax.swing.*;
import java.awt.*;

public class LeftPanePanel extends JPanel {

    private final OutputPanel outputPanel;

    public LeftPanePanel(CanonStore canonStore) {
        super(new BorderLayout());
        CanonPanel canonPanel = new CanonPanel(canonStore);
        outputPanel = new OutputPanel(canonStore, canonPanel::refresh);

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
