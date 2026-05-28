package com.chatstory.ui;

import com.chatstory.UiThread;
import com.chatstory.canon.CanonStore;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class CanonPanel extends JPanel {

    private final CanonStore canonStore;
    private final JTextArea textArea = new JTextArea();
    private final JButton saveButton = new JButton("Save");

    public CanonPanel(CanonStore canonStore) {
        super(new BorderLayout(6, 6));
        this.canonStore = canonStore;
        setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);

        saveButton.setEnabled(false);
        saveButton.addActionListener(e -> saveToFile());

        JPanel header = new JPanel(new BorderLayout());
        header.add(new JLabel("Canon"), BorderLayout.CENTER);
        header.add(saveButton, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);
        add(new JScrollPane(textArea), BorderLayout.CENTER);
    }

    public void refresh() {
        UiThread.run(() -> {
            textArea.setText(canonStore.export());
            textArea.setCaretPosition(0);
            saveButton.setEnabled(!canonStore.isEmpty());
        });
    }

    private void saveToFile() {
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
            Files.writeString(file.toPath(), canonStore.export(), StandardCharsets.UTF_8);
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
