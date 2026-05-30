package com.chatstory.ui;

import com.chatstory.transcript.TranscriptStore;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class TranscriptPanel extends JPanel {

    private final JTextArea textArea = new JTextArea();
    private final JButton saveButton = new JButton("Save");
    private final TranscriptStore transcriptStore;
    private boolean firstEntry = true;

    public TranscriptPanel(TranscriptStore transcriptStore) {
        super(new BorderLayout(6, 6));
        this.transcriptStore = transcriptStore;
        setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setFont(textArea.getFont().deriveFont(13f));

        saveButton.setEnabled(false);
        saveButton.addActionListener(e -> saveToFile());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.add(saveButton);

        JPanel header = new JPanel(new BorderLayout());
        header.add(new JLabel("Transcript"), BorderLayout.CENTER);
        header.add(buttonPanel, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);
        add(new JScrollPane(textArea), BorderLayout.CENTER);

        transcriptStore.addListener(this::onEntryAdded);
    }

    private void onEntryAdded() {
        SwingUtilities.invokeLater(() -> {
            if (firstEntry) {
                textArea.setText(transcriptStore.export());
                firstEntry = false;
            } else {
                textArea.append("\n\n---\n\n" + lastEntry());
            }
            textArea.setCaretPosition(0);
            saveButton.setEnabled(true);
        });
    }

    private String lastEntry() {
        String exported = transcriptStore.export();
        int sep = exported.lastIndexOf("\n\n---\n\n");
        return sep >= 0 ? exported.substring(sep + 7) : exported;
    }

    private void saveToFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Transcript");
        chooser.setFileFilter(new FileNameExtensionFilter("Text files (*.txt)", "txt"));
        chooser.setSelectedFile(new File("transcript.txt"));

        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();
        if (!file.getName().endsWith(".txt")) {
            file = new File(file.getAbsolutePath() + ".txt");
        }

        try {
            Files.writeString(file.toPath(), transcriptStore.export(), StandardCharsets.UTF_8);
            saveButton.setText("Saved!");
            Timer revert = new Timer(1200, e -> saveButton.setText("Save"));
            revert.setRepeats(false);
            revert.start();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Failed to save transcript:\n" + ex.getMessage(),
                    "Save Failed",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
