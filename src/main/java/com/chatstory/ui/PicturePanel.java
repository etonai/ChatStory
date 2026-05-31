package com.chatstory.ui;

import com.chatstory.picture.PictureFileStore;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class PicturePanel extends JPanel {

    private final PictureFileStore pictureFileStore;
    private final JTextField fileNameField = new JTextField();
    private final ImageDisplayPanel imageDisplay = new ImageDisplayPanel();

    public PicturePanel(PictureFileStore pictureFileStore) {
        super(new BorderLayout());
        this.pictureFileStore = pictureFileStore;
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        add(buildFileSection(), BorderLayout.NORTH);
        add(buildImageSection(), BorderLayout.CENTER);

        Path saved = pictureFileStore.getImageFile();
        if (saved != null && Files.exists(saved)) {
            fileNameField.setText(saved.getFileName().toString());
            loadImage(saved);
        }
    }

    private JPanel buildFileSection() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "File",
                TitledBorder.LEFT, TitledBorder.TOP));

        fileNameField.setEditable(false);
        fileNameField.setFont(fileNameField.getFont().deriveFont(11f));

        JButton browseButton = new JButton("Browse...");
        browseButton.addActionListener(e -> browseForImage());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 1.0;
        panel.add(fileNameField, gbc);

        gbc.gridx = 1; gbc.weightx = 0;
        panel.add(browseButton, gbc);

        return panel;
    }

    private JPanel buildImageSection() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Image",
                TitledBorder.LEFT, TitledBorder.TOP));
        panel.add(imageDisplay, BorderLayout.CENTER);
        return panel;
    }

    private void browseForImage() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Image File");
        chooser.setFileFilter(new FileNameExtensionFilter("PNG and JPG images", "png", "jpg", "jpeg"));

        Path current = pictureFileStore.getImageFile();
        if (current != null) {
            chooser.setCurrentDirectory(current.getParent().toFile());
        }

        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File selected = chooser.getSelectedFile();
        Path path = selected.toPath();
        pictureFileStore.setImageFile(path);
        fileNameField.setText(path.getFileName().toString());
        loadImage(path);
    }

    private void loadImage(Path path) {
        try {
            BufferedImage img = ImageIO.read(path.toFile());
            imageDisplay.setImage(img);
        } catch (IOException e) {
            imageDisplay.setImage(null);
            JOptionPane.showMessageDialog(this,
                    "Could not load image:\n" + e.getMessage(),
                    "Image Load Failed",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private static class ImageDisplayPanel extends JPanel {

        private BufferedImage image;

        ImageDisplayPanel() {
            setBackground(Color.DARK_GRAY);
        }

        void setImage(BufferedImage image) {
            this.image = image;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (image == null) return;

            int panelW = getWidth();
            int panelH = getHeight();
            int imgW = image.getWidth();
            int imgH = image.getHeight();

            double scale = Math.min((double) panelW / imgW, (double) panelH / imgH);
            int drawW = (int) (imgW * scale);
            int drawH = (int) (imgH * scale);
            int x = (panelW - drawW) / 2;
            int y = (panelH - drawH) / 2;

            g.drawImage(image, x, y, drawW, drawH, this);
        }
    }
}
