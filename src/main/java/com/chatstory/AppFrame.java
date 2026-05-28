package com.chatstory;

import com.chatstory.bridge.ChatGptBridge;
import com.chatstory.bridge.CorrectionType;
import com.chatstory.bridge.ResponseListener;
import com.chatstory.browser.BrowserPanel;
import com.chatstory.canon.CanonStore;
import com.chatstory.context.ContextFileStore;
import com.chatstory.mode.AppMode;
import com.chatstory.mode.AppModeModel;
import com.chatstory.theme.NativeThemeApplier;
import com.chatstory.theme.NativeThemeModel;
import com.chatstory.ui.ConfigurationPanel;
import com.chatstory.ui.ContextPanel;
import com.chatstory.ui.InputPanel;
import com.chatstory.ui.LeftPanePanel;
import com.chatstory.ui.ParsePreviewPanel;
import org.cef.browser.CefBrowser;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class AppFrame extends JFrame {

    private final JLabel statusLabel;
    private final LeftPanePanel leftPane;
    private final AppModeModel modeModel = new AppModeModel();
    private final NativeThemeModel themeModel = new NativeThemeModel();
    private final NativeThemeApplier themeApplier = new NativeThemeApplier();

    public AppFrame(AppState appState, BrowserPanel browserPanel, CefBrowser browser,
                    ChatGptBridge chatBridge, ContextFileStore contextFileStore) {
        super("Story Workstation");

        setSize(1400, 900);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        statusLabel = new JLabel(" Starting...");
        statusLabel.setForeground(Color.DARK_GRAY);
        CanonStore canonStore = new CanonStore();
        leftPane = new LeftPanePanel(canonStore,
                prompt -> chatBridge.sendPrompt(prompt, statusResponseListener("Correction sent")));
        ParsePreviewPanel parsePreviewPanel = new ParsePreviewPanel();
        JTabbedPane rightTabs = new JTabbedPane();
        rightTabs.addTab("MAIN", new JPanel(new BorderLayout()));
        rightTabs.addTab("Configuration", new ConfigurationPanel(modeModel, themeModel, contextFileStore));
        rightTabs.addTab("Parsed Input", parsePreviewPanel);
        rightTabs.addTab("Context", new ContextPanel(contextFileStore));

        JButton devToolsBtn = new JButton("DevTools");
        devToolsBtn.setToolTipText("Open Chromium DevTools for this page");
        devToolsBtn.addActionListener(e -> browser.openDevTools());

        JButton testInjectBtn = new JButton("Test Inject");
        testInjectBtn.setToolTipText("Inject a hardcoded prompt without sending");
        testInjectBtn.addActionListener(e -> chatBridge.testInjectPrompt(
                "DC3 test injection",
                statusResponseListener("Test prompt injected")));

        JButton redoBtn = new JButton("Redo");
        redoBtn.setToolTipText("Redo the last story beat");
        redoBtn.addActionListener(e -> chatBridge.sendPrompt(
                CorrectionType.REDO_PROMPT,
                statusResponseListener("Redo sent")));

        JButton resetBtn = new JButton("Reset");
        resetBtn.setToolTipText("Force app state back to Ready");
        resetBtn.addActionListener(e -> chatBridge.reset());

        JPanel leftTools = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        leftTools.add(devToolsBtn);
        leftTools.add(testInjectBtn);
        leftTools.add(redoBtn);
        leftTools.add(resetBtn);

        JPanel toolbar = new JPanel(new BorderLayout(6, 0));
        toolbar.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        toolbar.add(leftTools, BorderLayout.WEST);
        toolbar.add(statusLabel, BorderLayout.CENTER);

        JSplitPane browserAndRight = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                browserPanel.getUIComponent(),
                rightTabs);
        browserAndRight.setResizeWeight(0.82);

        JSplitPane mainSplit = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                leftPane,
                browserAndRight);
        mainSplit.setResizeWeight(0.22);

        add(toolbar, BorderLayout.NORTH);
        add(mainSplit, BorderLayout.CENTER);
        add(new InputPanel(appState, modeModel, chatBridge, statusResponseListener("Prompt submitted"),
                        () -> browser.setFocus(false),
                        parsePreviewPanel::setSegments),
                BorderLayout.SOUTH);

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dispose();
                System.exit(0);
            }
        });

        appState.addListener((prev, current) ->
                UiThread.run(() -> statusLabel.setText(" " + labelFor(current))));
modeModel.addListener((prev, current) ->
                UiThread.run(() -> statusLabel.setText(" " + labelFor(appState.current()))));
        themeModel.addListener((prev, current) ->
                UiThread.run(() -> themeApplier.apply(this, current)));

        setVisible(true);
        UiThread.run(() -> themeApplier.apply(this, themeModel.current()));
    }

    private String labelFor(AppState.State state) {
        switch (state) {
            case Starting:        return "Starting...";
            case LoadingChatGPT:  return "Loading ChatGPT...";
            case NeedsLogin:      return "Please log in to ChatGPT";
            case Ready:           return "Ready";
            case InjectingPrompt: return "Injecting prompt...";
            case Sending:         return "Sending prompt...";
            case WaitingForResponse: return "Waiting for response...";
            case Complete:        return "Response complete";
            case Error:           return "Error - check console";
            default:              return state.name();
        }
    }

    private ResponseListener statusResponseListener(String successText) {
        return new ResponseListener() {
            @Override
            public void onPromptSubmitted(long requestId) {
                UiThread.run(() -> {
                    if (modeModel.current() == AppMode.STORY) {
                        leftPane.clearResponse();
                    }
                    statusLabel.setText(" " + successText);
                });
            }

            @Override
            public void onResponsePartial(long requestId, String responseText) {
                leftPane.setResponse(responseText);
            }

            @Override
            public void onResponseComplete(long requestId, String responseText) {
                leftPane.setResponse(responseText);
                UiThread.run(() -> statusLabel.setText(" Response complete"));
            }

            @Override
            public void onError(long requestId, String errorCode, String message) {
                UiThread.run(() -> statusLabel.setText(" Error: " + errorCode + " - " + message));
            }
        };
    }
}
