package com.chatstory.ui;

import com.chatstory.UiThread;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;

public class OutputPanel extends JPanel {

    private final JTextArea textArea = new JTextArea();

    public OutputPanel() {
        super(new BorderLayout(6, 6));
        setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);

        JButton copyButton = new JButton("Copy");
        copyButton.addActionListener(e -> {
            StringSelection selection = new StringSelection(textArea.getText());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
        });

        JPanel header = new JPanel(new BorderLayout());
        header.add(new JLabel("Assistant Response"), BorderLayout.CENTER);
        header.add(copyButton, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);
        add(new JScrollPane(textArea), BorderLayout.CENTER);
    }

    public void setResponse(String text) {
        UiThread.run(() -> {
            textArea.setText(text == null ? "" : text);
            textArea.setCaretPosition(0);
        });
    }

    public void clear() {
        setResponse("");
    }
}
