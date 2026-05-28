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

    public OutputPanel(CanonStore canonStore, Consumer<String> onCanonAdded, Consumer<String> onSendPrompt) {
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
            String text = textArea.getText();
            canonStore.add(text);
            onCanonAdded.accept(text);
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

        installCorrectionMenu(onSendPrompt);
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
            addToCanonButton.setEnabled(text != null && !text.isBlank());
        });
    }

    public void clear() {
        setResponse("");
    }
}
