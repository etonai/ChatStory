package com.chatstory.ui;

import com.chatstory.UiThread;
import com.chatstory.input.SceneInputSegment;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class ParsePreviewPanel extends JPanel {

    private final JTextArea textArea = new JTextArea();

    public ParsePreviewPanel() {
        super(new BorderLayout(6, 6));
        setBorder(BorderFactory.createTitledBorder("Parsed Input"));

        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);

        add(new JScrollPane(textArea), BorderLayout.CENTER);
    }

    public void setSegments(List<SceneInputSegment> segments) {
        UiThread.run(() -> textArea.setText(format(segments)));
    }

    private String format(List<SceneInputSegment> segments) {
        if (segments == null || segments.isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < segments.size(); i++) {
            SceneInputSegment segment = segments.get(i);
            if (i > 0) {
                result.append("\n\n");
            }
            result.append(i + 1)
                    .append(". ")
                    .append(segment.type())
                    .append(": \"")
                    .append(segment.text())
                    .append("\"");
        }
        return result.toString();
    }
}
