package com.chatstory.ui;

import com.chatstory.UiThread;
import com.chatstory.context.ContextFileStore;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ContextPanel extends JPanel {

    private final ContextFileStore contextFileStore;
    private final JPanel checklistPanel = new JPanel();
    private final JLabel statusLabel = new JLabel(" ");
    private final JLabel stagingPathLabel;
    private final JButton stageButton = new JButton("Stage Checked");

    public ContextPanel(ContextFileStore contextFileStore) {
        super(new BorderLayout(6, 6));
        this.contextFileStore = contextFileStore;
        setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        stagingPathLabel = new JLabel(stagingText(contextFileStore.getStagingPath().toString()));
        stagingPathLabel.setFont(stagingPathLabel.getFont().deriveFont(Font.PLAIN, 10f));
        contextFileStore.addStagingPathListener(() ->
                UiThread.run(() -> stagingPathLabel.setText(
                        stagingText(contextFileStore.getStagingPath().toString()))));

        checklistPanel.setLayout(new BoxLayout(checklistPanel, BoxLayout.Y_AXIS));

        JScrollPane scrollPane = new JScrollPane(checklistPanel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        JButton addButton    = new JButton("Add Files");
        JButton removeButton = new JButton("Remove Checked");
        stageButton.setEnabled(false);

        addButton.addActionListener(e -> addFiles());
        removeButton.addActionListener(e -> removeChecked());
        stageButton.addActionListener(e -> stageChecked());

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        buttonRow.add(addButton);
        buttonRow.add(removeButton);
        buttonRow.add(stageButton);

        JPanel south = new JPanel(new BorderLayout(0, 3));
        south.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        south.add(buttonRow,       BorderLayout.NORTH);
        south.add(stagingPathLabel, BorderLayout.CENTER);
        south.add(statusLabel,     BorderLayout.SOUTH);

        add(scrollPane, BorderLayout.CENTER);
        add(south,      BorderLayout.SOUTH);

        for (Path entry : contextFileStore.getEntries()) {
            addCheckbox(entry);
        }
    }

    private void addFiles() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Add Context Files");
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileFilter(
                new FileNameExtensionFilter("Text and Markdown files (*.txt, *.md)", "txt", "md"));

        Path lastDir = contextFileStore.getLastDirectory();
        if (lastDir != null) chooser.setCurrentDirectory(lastDir.toFile());

        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        contextFileStore.setLastDirectory(chooser.getCurrentDirectory().toPath());

        List<Path> existing = contextFileStore.getEntries();
        for (File file : chooser.getSelectedFiles()) {
            Path path = file.toPath().toAbsolutePath();
            if (existing.contains(path)) continue;
            contextFileStore.add(path);
            addCheckbox(path);
        }
    }

    private void addCheckbox(Path path) {
        boolean exists = Files.exists(path);
        String label = exists
                ? path.getFileName().toString()
                : "[missing] " + path.getFileName().toString();

        JCheckBox cb = new JCheckBox(label);
        cb.putClientProperty("filePath", path);
        if (!exists) {
            cb.setFont(cb.getFont().deriveFont(Font.ITALIC));
        }
        cb.setAlignmentX(Component.LEFT_ALIGNMENT);
        cb.addItemListener(e -> updateStageButton());

        checklistPanel.add(cb);
        checklistPanel.revalidate();
        checklistPanel.repaint();
        updateStageButton();
    }

    private void removeChecked() {
        List<Component> toRemove = new ArrayList<>();
        for (Component c : checklistPanel.getComponents()) {
            if (c instanceof JCheckBox cb && cb.isSelected()) {
                Path path = (Path) cb.getClientProperty("filePath");
                contextFileStore.remove(path);
                toRemove.add(cb);
            }
        }
        toRemove.forEach(checklistPanel::remove);
        checklistPanel.revalidate();
        checklistPanel.repaint();
        updateStageButton();
        statusLabel.setText(" ");
    }

    private void stageChecked() {
        List<Path> selected = new ArrayList<>();
        for (Component c : checklistPanel.getComponents()) {
            if (c instanceof JCheckBox cb && cb.isSelected()) {
                selected.add((Path) cb.getClientProperty("filePath"));
            }
        }
        if (selected.isEmpty()) return;

        ContextFileStore.StagingResult result = contextFileStore.stageSelected(selected);
        if (result.failed() == 0) {
            statusLabel.setText(result.succeeded() + " file(s) staged to context-staging");
        } else {
            statusLabel.setText(result.succeeded() + " staged, " + result.failed()
                    + " failed — check console");
        }
    }

    private void updateStageButton() {
        for (Component c : checklistPanel.getComponents()) {
            if (c instanceof JCheckBox cb && cb.isSelected()) {
                stageButton.setEnabled(true);
                return;
            }
        }
        stageButton.setEnabled(false);
    }

    private static String stagingText(String path) {
        return "Staging: " + path;
    }
}
