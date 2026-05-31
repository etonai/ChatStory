package com.chatstory;

import com.chatstory.bridge.ChatGptBridge;
import com.chatstory.bridge.CorrectionType;
import com.chatstory.bridge.ResponseListener;
import com.chatstory.canon.CanonFolderStore;
import com.chatstory.picture.PictureFileStore;
import com.chatstory.browser.BrowserContextMenuHandler;
import com.chatstory.browser.BrowserPanel;
import com.chatstory.canon.CanonStore;
import org.cef.CefClient;
import com.chatstory.context.ContextFileStore;
import com.chatstory.controller.FinalControllerStore;
import com.chatstory.controller.IntermediateControllerStore;
import com.chatstory.controller.SessionControllerStore;
import com.chatstory.mode.AppMode;
import com.chatstory.mode.AppModeModel;
import com.chatstory.rules.RulesFileStore;
import com.chatstory.theme.NativeThemeApplier;
import com.chatstory.theme.NativeThemeModel;
import com.chatstory.ui.ConfigurationPanel;
import com.chatstory.ui.ContextPanel;
import com.chatstory.ui.InputPanel;
import com.chatstory.ui.LeftPanePanel;
import com.chatstory.ui.MainPanel;
import com.chatstory.ui.ParsePreviewPanel;
import com.chatstory.ui.PicturePanel;
import com.chatstory.ui.RulesPanel;
import org.cef.browser.CefBrowser;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.util.Map;
import java.awt.event.WindowEvent;

public class AppFrame extends JFrame {

    private final JLabel statusLabel;
    private final LeftPanePanel leftPane;
    private final AppModeModel modeModel = new AppModeModel();
    private final NativeThemeModel themeModel = new NativeThemeModel();
    private final NativeThemeApplier themeApplier = new NativeThemeApplier();

    public AppFrame(AppState appState, BrowserPanel browserPanel, CefBrowser browser,
                    ChatGptBridge chatBridge, CefClient client,
                    ContextFileStore contextFileStore, CanonFolderStore canonFolderStore,
                    PictureFileStore pictureFileStore,
                    SessionControllerStore sessionControllerStore,
                    IntermediateControllerStore intermediateControllerStore,
                    FinalControllerStore finalControllerStore,
                    RulesFileStore rulesFileStore,
                    Map<Integer, Runnable> browserShortcuts) {
        super("Story Workstation");

        setSize(1400, 900);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        statusLabel = new JLabel(" Starting...");
        statusLabel.setForeground(Color.DARK_GRAY);
        CanonStore canonStore = new CanonStore();
        leftPane = new LeftPanePanel(canonStore,
                prompt -> chatBridge.sendPrompt(prompt, statusResponseListener("Correction sent")),
                () -> browser.setFocus(false),
                canonFolderStore,
                modeModel);
        client.addContextMenuHandler(new BrowserContextMenuHandler(leftPane::onResponseComplete));

        Runnable onRedo = () -> chatBridge.sendPrompt(
                CorrectionType.REDO_PROMPT,
                statusResponseListener("Redo sent"));

        Runnable onContinue = () -> chatBridge.sendPrompt(
                CorrectionType.CONTINUE_PROMPT,
                statusResponseListener("Continue sent"));

        Runnable onEndScene = () -> chatBridge.sendPrompt(
                CorrectionType.END_SCENE_PROMPT,
                statusResponseListener("End scene sent"));

        Runnable onReset = chatBridge::reset;

        Runnable onFetch = () -> {
            chatBridge.reset();
            chatBridge.fetchLatestResponse(text -> {
                if (text != null && !text.isBlank()) {
                    leftPane.onResponseComplete(text);
                    UiThread.run(() -> statusLabel.setText(" Response fetched"));
                } else {
                    UiThread.run(() -> statusLabel.setText(" No response found in browser"));
                }
            });
        };

        ParsePreviewPanel parsePreviewPanel = new ParsePreviewPanel();
        MainPanel mainPanel = new MainPanel(
                sessionControllerStore,
                intermediateControllerStore,
                finalControllerStore,
                rulesFileStore,
                contextFileStore,
                text -> chatBridge.sendPrompt(text, statusResponseListener("Controller sent")),
                chatBridge::clickUploadFile,
                onRedo, onContinue, onEndScene, onReset, onFetch,
                leftPane::endSession);

        JTabbedPane rightTabs = new JTabbedPane();
        rightTabs.addTab("MAIN", mainPanel);
        rightTabs.addTab("Picture", new PicturePanel(pictureFileStore));
        rightTabs.addTab("Context", new ContextPanel(contextFileStore));
        rightTabs.addTab("Rules", new RulesPanel(rulesFileStore));
        rightTabs.addTab("Parsed Input", parsePreviewPanel);
        rightTabs.addTab("Configuration", new ConfigurationPanel(modeModel, themeModel, contextFileStore, canonFolderStore));

        JButton devToolsBtn = new JButton("DevTools");
        devToolsBtn.setToolTipText("Open Chromium DevTools for this page");
        devToolsBtn.addActionListener(e -> browser.openDevTools());

        JButton testInjectBtn = new JButton("Test Inject");
        testInjectBtn.setToolTipText("Inject a hardcoded prompt without sending");
        testInjectBtn.addActionListener(e -> chatBridge.testInjectPrompt(
                "DC3 test injection",
                statusResponseListener("Test prompt injected")));

        JPanel leftTools = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        leftTools.add(devToolsBtn);
        leftTools.add(testInjectBtn);

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

        InputPanel inputPanel = new InputPanel(appState, modeModel, chatBridge,
                statusResponseListener("Prompt submitted"),
                () -> browser.setFocus(false),
                parsePreviewPanel::setSegments);

        add(toolbar, BorderLayout.NORTH);
        add(mainSplit, BorderLayout.CENTER);
        add(inputPanel, BorderLayout.SOUTH);

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

        Runnable onFocusBrowser = () -> {
            browser.setFocus(true);
            browser.executeJavaScript("document.activeElement.blur();", browser.getURL(), 0);
        };

        browserShortcuts.put(KeyEvent.VK_M, () -> rightTabs.setSelectedIndex(0));
        browserShortcuts.put(KeyEvent.VK_P, () -> rightTabs.setSelectedIndex(1));
        browserShortcuts.put(KeyEvent.VK_X, onReset);
        browserShortcuts.put(KeyEvent.VK_R, onRedo);
        browserShortcuts.put(KeyEvent.VK_F, onFetch);
        browserShortcuts.put(KeyEvent.VK_S, inputPanel::triggerSend);
        browserShortcuts.put(KeyEvent.VK_C, mainPanel::triggerSendContext);
        browserShortcuts.put(KeyEvent.VK_B, onFocusBrowser);
        browserShortcuts.put(KeyEvent.VK_W, inputPanel::focusInput);

        installKeyboardShortcuts(browserShortcuts);

        setVisible(true);
        UiThread.run(() -> themeApplier.apply(this, themeModel.current()));
    }

    private void installKeyboardShortcuts(Map<Integer, Runnable> shortcuts) {
        int ctrlShift = InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK;
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
            if (e.getID() != KeyEvent.KEY_PRESSED) return false;
            if ((e.getModifiersEx() & ctrlShift) != ctrlShift) return false;
            Runnable action = shortcuts.get(e.getKeyCode());
            if (action == null) return false;
            action.run();
            return true;
        });
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
                UiThread.run(() -> statusLabel.setText(" " + successText));
            }

            @Override
            public void onResponsePartial(long requestId, String responseText) {
                // Response panel only updates when a complete beat is received
            }

            @Override
            public void onResponseComplete(long requestId, String responseText) {
                leftPane.onResponseComplete(responseText);
                UiThread.run(() -> statusLabel.setText(" Response complete"));
            }

            @Override
            public void onError(long requestId, String errorCode, String message) {
                UiThread.run(() -> statusLabel.setText(" Error: " + errorCode + " - " + message));
            }
        };
    }
}
