package com.chatstory.ui;

import com.chatstory.AppState;
import com.chatstory.UiThread;
import com.chatstory.bridge.ChatBridge;
import com.chatstory.bridge.ResponseListener;
import com.chatstory.input.SceneInputParser;
import com.chatstory.input.SceneInputSegment;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Consumer;

public class InputPanel extends JPanel {

    private final AppState appState;
    private final ChatBridge chatBridge;
    private final JTextArea textArea = new JTextArea(4, 60);
    private final JButton sendButton = new JButton("Send");
    private final Runnable beforeFocusRequest;
    private final SceneInputParser parser = new SceneInputParser();
    private final Consumer<List<SceneInputSegment>> previewConsumer;

    public InputPanel(AppState appState, ChatBridge chatBridge, ResponseListener listener) {
        this(appState, chatBridge, listener, () -> {}, segments -> {});
    }

    public InputPanel(AppState appState, ChatBridge chatBridge, ResponseListener listener,
                      Runnable beforeFocusRequest) {
        this(appState, chatBridge, listener, beforeFocusRequest, segments -> {});
    }

    public InputPanel(AppState appState, ChatBridge chatBridge, ResponseListener listener,
                      Runnable beforeFocusRequest,
                      Consumer<List<SceneInputSegment>> previewConsumer) {
        super(new BorderLayout(6, 6));
        this.appState = appState;
        this.chatBridge = chatBridge;
        this.beforeFocusRequest = beforeFocusRequest == null ? () -> {} : beforeFocusRequest;
        this.previewConsumer = previewConsumer == null ? segments -> {} : previewConsumer;

        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(textArea);

        JPanel actions = new JPanel(new BorderLayout());
        actions.add(sendButton, BorderLayout.NORTH);

        setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        add(scrollPane, BorderLayout.CENTER);
        add(actions, BorderLayout.EAST);
        installFocusRecovery(scrollPane);

        sendButton.addActionListener(e -> send(listener));
        textArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { refreshInputState(); }
            @Override public void removeUpdate(DocumentEvent e) { refreshInputState(); }
            @Override public void changedUpdate(DocumentEvent e) { refreshInputState(); }
        });

        textArea.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "send");
        textArea.getInputMap().put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK),
                "insert-newline");
        textArea.getInputMap().put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,
                        InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK),
                "send-as-direction");
        textArea.getActionMap().put("send", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (sendButton.isEnabled()) send(listener);
            }
        });
        textArea.getActionMap().put("insert-newline", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                textArea.replaceSelection("\n");
            }
        });
        textArea.getActionMap().put("send-as-direction", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (sendButton.isEnabled()) sendAsDirection(listener);
            }
        });

        appState.addListener((prev, current) -> refreshInputState());
        refreshInputState();
    }

    private void installFocusRecovery(JScrollPane scrollPane) {
        MouseAdapter focusRecovery = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestInputFocus();
            }
        };

        addMouseListener(focusRecovery);
        textArea.addMouseListener(focusRecovery);
        scrollPane.addMouseListener(focusRecovery);
        scrollPane.getViewport().addMouseListener(focusRecovery);
    }

    private void requestInputFocus() {
        beforeFocusRequest.run();
        textArea.requestFocusInWindow();
        SwingUtilities.invokeLater(() -> {
            KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();
            textArea.requestFocusInWindow();
        });
    }

    private void send(ResponseListener listener) {
        send(listener, false);
    }

    private void sendAsDirection(ResponseListener listener) {
        send(listener, true);
    }

    private void send(ResponseListener listener, boolean directionOverride) {
        String prompt = textArea.getText();
        if (prompt == null || prompt.trim().isEmpty()) {
            refreshInputState();
            return;
        }
        updatePreview(directionOverride);
        chatBridge.sendPrompt(prompt, new ResponseListener() {
            @Override
            public void onPromptSubmitted(long requestId) {
                UiThread.run(() -> textArea.setText(""));
                listener.onPromptSubmitted(requestId);
            }

            @Override
            public void onResponsePartial(long requestId, String responseText) {
                listener.onResponsePartial(requestId, responseText);
            }

            @Override
            public void onResponseComplete(long requestId, String responseText) {
                listener.onResponseComplete(requestId, responseText);
            }

            @Override
            public void onError(long requestId, String errorCode, String message) {
                listener.onError(requestId, errorCode, message);
            }
        });
    }

    private void refreshInputState() {
        updatePreview(false);
        UiThread.run(() -> sendButton.setEnabled(
                appState.isSendEnabled() && !textArea.getText().trim().isEmpty()));
    }

    private void updatePreview(boolean directionOverride) {
        String input = textArea.getText();
        List<SceneInputSegment> segments = directionOverride
                ? parser.parseAsDirection(input)
                : parser.parse(input);
        previewConsumer.accept(segments);
    }
}
