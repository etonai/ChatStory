package com.chatstory.ui;

import com.chatstory.beat.BeatParser;
import com.chatstory.beat.CurrentBeatModel;
import com.chatstory.canon.CanonFolderStore;
import com.chatstory.canon.CanonStore;
import com.chatstory.mode.AppMode;
import com.chatstory.mode.AppModeModel;

import javax.swing.*;
import java.awt.*;
import java.util.OptionalInt;
import java.util.function.Consumer;

public class LeftPanePanel extends JPanel {

    private final OutputPanel outputPanel;
    private final CanonPanel canonPanel;
    private final CurrentBeatModel currentBeatModel = new CurrentBeatModel();
    private final AppModeModel modeModel;

    public LeftPanePanel(CanonStore canonStore, Consumer<String> onSendPrompt,
                         Runnable beforeFocusRequest, CanonFolderStore canonFolderStore,
                         AppModeModel modeModel) {
        super(new BorderLayout());
        this.modeModel = modeModel;
        canonPanel = new CanonPanel(canonStore, beforeFocusRequest, canonFolderStore, currentBeatModel);
        outputPanel = new OutputPanel(canonStore,
                text -> {
                    canonPanel.appendEntry(text);
                    currentBeatModel.markAppended();
                },
                onSendPrompt, beforeFocusRequest);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Response", outputPanel);
        tabs.addTab("Canon", canonPanel);

        add(tabs, BorderLayout.CENTER);
    }

    public void setResponse(String text) {
        outputPanel.setResponse(text);
    }

    public void onResponseComplete(String text) {
        if (modeModel.current() != AppMode.STORY) return;
        OptionalInt parsed = BeatParser.parse(text);
        if (!parsed.isPresent()) return;
        CurrentBeatModel.UpdateResult result = currentBeatModel.update(parsed.getAsInt(), text);
        if (result.kind == CurrentBeatModel.ResultKind.ROLLED_OVER) {
            canonPanel.appendEntry(result.textToAppend);
        }
        outputPanel.setResponse(text, parsed.getAsInt());
    }

    public void clearResponse() {
        outputPanel.clear();
    }
}
