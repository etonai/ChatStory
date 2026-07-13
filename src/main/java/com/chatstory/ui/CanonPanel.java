package com.chatstory.ui;

import com.chatstory.UiThread;
import com.chatstory.beat.CurrentBeatModel;
import com.chatstory.canon.CanonFolderStore;
import com.chatstory.canon.CanonStore;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class CanonPanel extends JPanel {

    private final JTextArea textArea = new JTextArea();
    private final JButton saveButton = new JButton("Save");
    private final JButton clearButton = new JButton("Clear");
    private final CanonFolderStore canonFolderStore;
    private final CurrentBeatModel currentBeatModel;

    public CanonPanel(CanonStore canonStore, Runnable beforeFocusRequest,
                      CanonFolderStore canonFolderStore, CurrentBeatModel currentBeatModel) {
        super(new BorderLayout(6, 6));
        this.canonFolderStore = canonFolderStore;
        this.currentBeatModel = currentBeatModel;
        setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        textArea.setEditable(true);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);

        saveButton.setEnabled(false);
        saveButton.addActionListener(e -> saveToFile());

        clearButton.setEnabled(false);
        clearButton.addActionListener(e -> textArea.setText(""));

        textArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e)  { updateButtons(); }
            @Override public void removeUpdate(DocumentEvent e)  { updateButtons(); }
            @Override public void changedUpdate(DocumentEvent e) { updateButtons(); }
            private void updateButtons() {
                System.out.println("[CanonPanel] updateButtons enter thread="
                        + Thread.currentThread().getName()
                        + " onEdt=" + SwingUtilities.isEventDispatchThread());
                boolean hasContent = !textArea.getText().isBlank();
                saveButton.setEnabled(hasContent);
                clearButton.setEnabled(hasContent);
                System.out.println("[CanonPanel] updateButtons exit hasContent=" + hasContent);
            }
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.add(clearButton);
        buttonPanel.add(saveButton);

        JPanel header = new JPanel(new BorderLayout());
        header.add(new JLabel("Canon"), BorderLayout.CENTER);
        header.add(buttonPanel, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);
        JScrollPane scrollPane = new JScrollPane(textArea);
        add(scrollPane, BorderLayout.CENTER);

        MouseAdapter focusRecovery = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                beforeFocusRequest.run();
                textArea.requestFocusInWindow();
                SwingUtilities.invokeLater(() -> {
                    KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();
                    textArea.requestFocusInWindow();
                });
            }
        };
        textArea.addMouseListener(focusRecovery);
        scrollPane.addMouseListener(focusRecovery);
        scrollPane.getViewport().addMouseListener(focusRecovery);
    }

    public void appendEntry(String text) {
        System.out.println("[CanonPanel] appendEntry enter thread=" + Thread.currentThread().getName()
                + " onEdt=" + SwingUtilities.isEventDispatchThread()
                + " textLen=" + (text == null ? 0 : text.length()));
        if (!SwingUtilities.isEventDispatchThread()) {
            System.out.println("[CanonPanel] appendEntry queueing on EDT");
            UiThread.run(() -> appendEntry(text));
            System.out.println("[CanonPanel] appendEntry queued on EDT");
            return;
        }
        System.out.println("[CanonPanel] appendEntry checking blank input");
        if (text == null || text.isBlank()) {
            System.out.println("[CanonPanel] appendEntry exit blank input");
            return;
        }
        System.out.println("[CanonPanel] appendEntry before getText");
        String current = textArea.getText();
        System.out.println("[CanonPanel] appendEntry after getText currentLen=" + current.length());
        System.out.println("[CanonPanel] appendEntry before build updated");
        String updated = current.isBlank() ? text : current + "\n\n---\n\n" + text;
        System.out.println("[CanonPanel] appendEntry after build updatedLen=" + updated.length());
        System.out.println("[CanonPanel] appendEntry before setText");
        textArea.setText(updated);
        System.out.println("[CanonPanel] appendEntry after setText");
        System.out.println("[CanonPanel] appendEntry before setCaretPosition");
        textArea.setCaretPosition(0);
        System.out.println("[CanonPanel] appendEntry after setCaretPosition");
        System.out.println("[CanonPanel] appendEntry before autoSaveTemp");
        autoSaveTemp(updated);
        System.out.println("[CanonPanel] appendEntry after autoSaveTemp");
        System.out.println("[CanonPanel] appendEntry exit");
    }

    private void autoSaveTemp(String content) {
        Path tempFile = canonFolderStore.getTempFile();
        if (tempFile == null) return;
        Thread.ofVirtual().start(() -> {
            try {
                Files.createDirectories(tempFile.getParent());
                Files.writeString(tempFile, content, StandardCharsets.UTF_8);
            } catch (IOException ex) {
                System.err.println("[CanonPanel] Auto-save failed: " + ex.getMessage());
            }
        });
    }

    private void wipeTemp() {
        Path tempFile = canonFolderStore.getTempFile();
        if (tempFile == null) return;
        try {
            Files.deleteIfExists(tempFile);
        } catch (IOException ex) {
            System.err.println("[CanonPanel] Failed to wipe temp file: " + ex.getMessage());
        }
    }

    public void saveCanon() {
        saveToFile();
    }

    private void saveToFile() {
        if (currentBeatModel.hasUnappendedBeat()) {
            int choice = JOptionPane.showConfirmDialog(this,
                    "Current Beat " + currentBeatModel.getBeatNumber()
                            + " has not been added to Canon.\nAppend it before saving?",
                    "Unsaved Beat",
                    JOptionPane.YES_NO_CANCEL_OPTION);
            if (choice == JOptionPane.CANCEL_OPTION || choice == JOptionPane.CLOSED_OPTION) return;
            if (choice == JOptionPane.YES_OPTION) {
                appendEntry(currentBeatModel.getText());
                currentBeatModel.markAppended();
            }
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Canon");
        chooser.setFileFilter(new FileNameExtensionFilter("Markdown files (*.md)", "md"));
        chooser.setSelectedFile(new File("canon.md"));

        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();
        if (!file.getName().endsWith(".md")) {
            file = new File(file.getAbsolutePath() + ".md");
        }

        try {
            Files.writeString(file.toPath(), textArea.getText(), StandardCharsets.UTF_8);
            wipeTemp();
            saveButton.setText("Saved!");
            Timer revert = new Timer(1200, ev -> saveButton.setText("Save"));
            revert.setRepeats(false);
            revert.start();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Failed to save canon:\n" + ex.getMessage(),
                    "Save Failed",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
