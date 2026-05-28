package com.chatstory.ui;

import com.chatstory.UiThread;
import com.chatstory.canon.CanonStore;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;

public class OutputPanel extends JPanel {

    private final JTextArea textArea = new JTextArea();
    private final JButton addToCanonButton = new JButton("Add to Canon");

    public OutputPanel(CanonStore canonStore, Runnable onCanonAdded) {
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

        addToCanonButton.setEnabled(false);
        addToCanonButton.addActionListener(e -> {
            canonStore.add(textArea.getText());
            onCanonAdded.run();
            addToCanonButton.setText("Added!");
            Timer revert = new Timer(1000, ev -> addToCanonButton.setText("Add to Canon"));
            revert.setRepeats(false);
            revert.start();
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.add(addToCanonButton);
        buttonPanel.add(copyButton);

        JPanel header = new JPanel(new BorderLayout());
        header.add(new JLabel("Assistant Response"), BorderLayout.CENTER);
        header.add(buttonPanel, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);
        add(new JScrollPane(textArea), BorderLayout.CENTER);
    }

    public void setResponse(String text) {
        UiThread.run(() -> {
            textArea.setText(text == null ? "" : text);
            textArea.setCaretPosition(0);
            addToCanonButton.setEnabled(text != null && !text.isBlank());
        });
    }

    public void clear() {
        setResponse("");
    }
}
