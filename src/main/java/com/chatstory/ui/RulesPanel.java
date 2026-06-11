package com.chatstory.ui;

import com.chatstory.rules.RulesFileStore;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class RulesPanel extends JPanel {

    private final RulesFileStore rulesFileStore;
    private final JPanel checklistPanel = new JPanel();
    private final JButton selectAllButton = new JButton("Select All");

    private final JTextArea viewerArea = new JTextArea();
    private final JSplitPane splitPane;

    public RulesPanel(RulesFileStore rulesFileStore) {
        super(new BorderLayout(6, 6));
        this.rulesFileStore = rulesFileStore;
        setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        checklistPanel.setLayout(new BoxLayout(checklistPanel, BoxLayout.Y_AXIS));

        JScrollPane checklistScroll = new JScrollPane(checklistPanel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        checklistScroll.getVerticalScrollBar().setUnitIncrement(16);

        viewerArea.setEditable(false);
        viewerArea.setLineWrap(true);
        viewerArea.setWrapStyleWord(true);
        installViewerCopyMenu();
        JScrollPane viewerScroll = new JScrollPane(viewerArea,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, checklistScroll, viewerScroll);
        splitPane.setResizeWeight(1.0);

        JButton addButton    = new JButton("Add Files");
        JButton removeButton = new JButton("Remove Checked");

        addButton.addActionListener(e -> addFiles());
        selectAllButton.addActionListener(e -> selectAllOrNone());
        removeButton.addActionListener(e -> removeChecked());

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        buttonRow.add(addButton);
        buttonRow.add(selectAllButton);
        buttonRow.add(removeButton);

        add(splitPane, BorderLayout.CENTER);
        add(buttonRow, BorderLayout.SOUTH);

        SwingUtilities.invokeLater(() -> splitPane.setDividerLocation(1.0));

        rulesFileStore.addListener(() -> SwingUtilities.invokeLater(this::refreshChecklist));
        for (Path entry : rulesFileStore.getEntries()) {
            addCheckbox(entry);
        }
    }

    private void addFiles() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Add Rules Files");
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileFilter(
                new FileNameExtensionFilter("Text and Markdown files (*.txt, *.md)", "txt", "md"));

        Path lastDir = rulesFileStore.getLastDirectory();
        if (lastDir != null) chooser.setCurrentDirectory(lastDir.toFile());

        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        rulesFileStore.setLastDirectory(chooser.getCurrentDirectory().toPath());

        List<Path> existing = rulesFileStore.getEntries();
        for (File file : chooser.getSelectedFiles()) {
            Path path = file.toPath().toAbsolutePath();
            if (existing.contains(path)) continue;
            rulesFileStore.add(path);
        }
    }

    private void refreshChecklist() {
        checklistPanel.removeAll();
        for (Path entry : rulesFileStore.getEntries()) {
            addCheckbox(entry);
        }
        updateSelectAllButton();
        checklistPanel.revalidate();
        checklistPanel.repaint();
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
        cb.addItemListener(e -> updateSelectAllButton());

        cb.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e)  { maybeShowViewMenu(e, path); }
            @Override public void mouseReleased(MouseEvent e) { maybeShowViewMenu(e, path); }
        });

        checklistPanel.add(cb);
        sortChecklist();
        updateSelectAllButton();
    }

    private void installViewerCopyMenu() {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem copyItem = new JMenuItem("Copy");
        copyItem.addActionListener(e -> {
            String selected = viewerArea.getSelectedText();
            if (selected != null && !selected.isEmpty()) {
                StringSelection ss = new StringSelection(selected);
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, ss);
            }
        });
        menu.add(copyItem);

        menu.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                copyItem.setEnabled(viewerArea.getSelectedText() != null
                        && !viewerArea.getSelectedText().isEmpty());
            }
            @Override public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}
            @Override public void popupMenuCanceled(PopupMenuEvent e) {}
        });

        viewerArea.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e)  { maybeShowCopyMenu(e); }
            @Override public void mouseReleased(MouseEvent e) { maybeShowCopyMenu(e); }
            private void maybeShowCopyMenu(MouseEvent e) {
                if (e.isPopupTrigger()) menu.show(viewerArea, e.getX(), e.getY());
            }
        });
    }

    private void maybeShowViewMenu(MouseEvent e, Path path) {
        if (!e.isPopupTrigger()) return;
        JPopupMenu menu = new JPopupMenu();

        JMenuItem viewItem = new JMenuItem("View");
        viewItem.addActionListener(ev -> viewFile(path));
        menu.add(viewItem);

        JMenuItem copyItem = new JMenuItem("Copy Contents");
        copyItem.addActionListener(ev -> copyFileContents(path));
        menu.add(copyItem);

        menu.show(e.getComponent(), e.getX(), e.getY());
    }

    private void viewFile(Path path) {
        String content;
        try {
            content = Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            content = "Could not read file: " + ex.getMessage();
        }
        viewerArea.setText(content);
        viewerArea.setCaretPosition(0);

        if (splitPane.getDividerLocation() >= splitPane.getHeight() - splitPane.getDividerSize() - 10) {
            splitPane.setDividerLocation(0.55);
        }
    }

    private void copyFileContents(Path path) {
        try {
            String content = Files.readString(path, StandardCharsets.UTF_8);
            StringSelection ss = new StringSelection(content);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, ss);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Could not read file:\n" + ex.getMessage(),
                    "Copy Failed",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void sortChecklist() {
        Component[] components = checklistPanel.getComponents();
        java.util.Arrays.sort(components, (a, b) -> {
            if (!(a instanceof JCheckBox ca) || !(b instanceof JCheckBox cb)) return 0;
            Path pa = (Path) ca.getClientProperty("filePath");
            Path pb = (Path) cb.getClientProperty("filePath");
            return pa.getFileName().toString().compareToIgnoreCase(pb.getFileName().toString());
        });
        checklistPanel.removeAll();
        for (Component c : components) checklistPanel.add(c);
        checklistPanel.revalidate();
        checklistPanel.repaint();
    }

    private void selectAllOrNone() {
        Component[] components = checklistPanel.getComponents();
        boolean anyUnchecked = false;
        for (Component c : components) {
            if (c instanceof JCheckBox cb && !cb.isSelected()) {
                anyUnchecked = true;
                break;
            }
        }
        for (Component c : components) {
            if (c instanceof JCheckBox cb) cb.setSelected(anyUnchecked);
        }
        updateSelectAllButton();
    }

    private void updateSelectAllButton() {
        Component[] components = checklistPanel.getComponents();
        boolean allSelected = components.length > 0;
        for (Component c : components) {
            if (c instanceof JCheckBox cb && !cb.isSelected()) {
                allSelected = false;
                break;
            }
        }
        selectAllButton.setText(allSelected ? "Unselect All" : "Select All");
    }

    private void removeChecked() {
        for (Component c : checklistPanel.getComponents()) {
            if (c instanceof JCheckBox cb && cb.isSelected()) {
                Path path = (Path) cb.getClientProperty("filePath");
                rulesFileStore.remove(path);
            }
        }
        updateSelectAllButton();
    }
}
