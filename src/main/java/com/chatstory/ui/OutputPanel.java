package com.chatstory.ui;

import com.chatstory.UiThread;
import com.chatstory.bridge.CorrectionType;
import com.chatstory.canon.CanonStore;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

public class OutputPanel extends JPanel {

    private final JTextArea textArea = new JTextArea();
    private final JButton addToCanonButton = new JButton("Add to Canon");
    private final JLabel responseLabel = new JLabel("Assistant Response");

    public OutputPanel(CanonStore canonStore, Consumer<String> onCanonAdded,
                       Consumer<String> onSendPrompt, Runnable beforeFocusRequest) {
        super(new BorderLayout(6, 6));
        setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        textArea.setFont(textArea.getFont().deriveFont(14f));
        textArea.setEditable(true);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);

        JButton copyButton = new JButton("Copy");
        copyButton.addActionListener(e -> {
            StringSelection selection = new StringSelection(textArea.getText());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
        });

        addToCanonButton.setEnabled(false);
        addToCanonButton.addActionListener(e -> {
            String text = textArea.getText();
            canonStore.add(text);
            onCanonAdded.accept(text);
            addToCanonButton.setText("Added!");
            addToCanonButton.setEnabled(false);
        });

        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(e -> {
            textArea.setText("");
            addToCanonButton.setText("Add to Canon");
            addToCanonButton.setEnabled(true);
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.add(addToCanonButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(copyButton);

        JPanel header = new JPanel(new BorderLayout());
        header.add(responseLabel, BorderLayout.CENTER);
        header.add(buttonPanel, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);
        JScrollPane scrollPane = new JScrollPane(textArea);
        add(scrollPane, BorderLayout.CENTER);

        installCorrectionMenu(onSendPrompt);
        installFocusRecovery(scrollPane, beforeFocusRequest);
    }

    private void installFocusRecovery(JScrollPane scrollPane, Runnable beforeFocusRequest) {
        MouseAdapter focusRecovery = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!e.isPopupTrigger()) {
                    beforeFocusRequest.run();
                    textArea.requestFocusInWindow();
                    SwingUtilities.invokeLater(() -> {
                        KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();
                        textArea.requestFocusInWindow();
                    });
                }
            }
        };
        textArea.addMouseListener(focusRecovery);
        scrollPane.addMouseListener(focusRecovery);
        scrollPane.getViewport().addMouseListener(focusRecovery);
    }

    private void installCorrectionMenu(Consumer<String> onSendPrompt) {
        JPopupMenu menu = new JPopupMenu();
        Map<CorrectionType, JMenuItem> items = new EnumMap<>(CorrectionType.class);
        for (CorrectionType type : CorrectionType.values()) {
            JMenuItem item = new JMenuItem(type.menuLabel());
            item.addActionListener(e -> {
                String selected = textArea.getSelectedText();
                if (selected != null && !selected.isBlank()) {
                    onSendPrompt.accept(type.buildPrompt(selected));
                }
            });
            menu.add(item);
            items.put(type, item);
        }

        menu.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                boolean hasSelection = textArea.getSelectedText() != null
                        && !textArea.getSelectedText().isBlank();
                items.values().forEach(item -> item.setEnabled(hasSelection));
            }
            @Override public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}
            @Override public void popupMenuCanceled(PopupMenuEvent e) {}
        });

        textArea.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e)  { maybeShow(e); }
            @Override public void mouseReleased(MouseEvent e) { maybeShow(e); }
            private void maybeShow(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    menu.show(textArea, e.getX(), e.getY());
                }
            }
        });
    }

    public void setResponse(String text) {
        UiThread.run(() -> {
            textArea.setText(text == null ? "" : text);
            textArea.setCaretPosition(0);
            boolean hasContent = text != null && !text.isBlank();
            addToCanonButton.setEnabled(hasContent);
            if (hasContent) addToCanonButton.setText("Add to Canon");
        });
    }

    public void setResponse(String text, int beatNumber) {
        UiThread.run(() -> {
            responseLabel.setText("Current Beat " + beatNumber);
            textArea.setText(text == null ? "" : text);
            textArea.setCaretPosition(0);
            boolean hasContent = text != null && !text.isBlank();
            addToCanonButton.setEnabled(hasContent);
            if (hasContent) addToCanonButton.setText("Add to Canon");
        });
    }

    public void clear() {
        UiThread.run(() -> {
            responseLabel.setText("Assistant Response");
            textArea.setText("");
            addToCanonButton.setText("Add to Canon");
            addToCanonButton.setEnabled(false);
        });
    }
}
