package com.chatstory.ui;

import com.chatstory.AppState;
import com.chatstory.UiThread;
import com.chatstory.bridge.ChatBridge;
import com.chatstory.bridge.ResponseListener;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

public class InputPanel extends JPanel {

    private final AppState appState;
    private final ChatBridge chatBridge;
    private final JTextArea textArea = new JTextArea(4, 60);
    private final JButton sendButton = new JButton("Send");

    public InputPanel(AppState appState, ChatBridge chatBridge, ResponseListener listener) {
        super(new BorderLayout(6, 6));
        this.appState = appState;
        this.chatBridge = chatBridge;

        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(textArea);

        JPanel actions = new JPanel(new BorderLayout());
        actions.add(sendButton, BorderLayout.NORTH);

        setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        add(scrollPane, BorderLayout.CENTER);
        add(actions, BorderLayout.EAST);

        sendButton.addActionListener(e -> send(listener));
        textArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { refreshSendEnabled(); }
            @Override public void removeUpdate(DocumentEvent e) { refreshSendEnabled(); }
            @Override public void changedUpdate(DocumentEvent e) { refreshSendEnabled(); }
        });

        textArea.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK), "send");
        textArea.getActionMap().put("send", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (sendButton.isEnabled()) send(listener);
            }
        });

        appState.addListener((prev, current) -> refreshSendEnabled());
        refreshSendEnabled();
    }

    private void send(ResponseListener listener) {
        String prompt = textArea.getText();
        if (prompt == null || prompt.trim().isEmpty()) {
            refreshSendEnabled();
            return;
        }
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

    private void refreshSendEnabled() {
        UiThread.run(() -> sendButton.setEnabled(
                appState.isSendEnabled() && !textArea.getText().trim().isEmpty()));
    }
}
