package com.chatstory.ui;

import com.chatstory.UiThread;
import com.chatstory.beat.BeatParser;
import com.chatstory.beat.CurrentBeatModel;
import com.chatstory.canon.CanonFolderStore;
import com.chatstory.canon.CanonStore;
import com.chatstory.mode.AppMode;
import com.chatstory.mode.AppModeModel;
import com.chatstory.transcript.TranscriptStore;

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
        TranscriptStore transcriptStore = new TranscriptStore();
        outputPanel = new OutputPanel(canonStore,
                text -> {
                    canonPanel.appendEntry(text);
                    currentBeatModel.markAppended();
                },
                onSendPrompt, beforeFocusRequest,
                transcriptStore::add);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Response", outputPanel);
        tabs.addTab("Canon", canonPanel);
        tabs.addTab("Transcript", new TranscriptPanel(transcriptStore));

        add(tabs, BorderLayout.CENTER);
    }

    public void setResponse(String text) {
        outputPanel.setResponse(text);
    }

    public void onResponseComplete(String text, String html) {
        System.out.println("[LeftPanePanel] onResponseComplete enter thread="
                + Thread.currentThread().getName()
                + " onEdt=" + SwingUtilities.isEventDispatchThread()
                + " textLen=" + (text == null ? 0 : text.length())
                + " mode=" + modeModel.current());
        if (!SwingUtilities.isEventDispatchThread()) {
            System.out.println("[LeftPanePanel] onResponseComplete queueing on EDT");
            UiThread.run(() -> onResponseComplete(text, html));
            System.out.println("[LeftPanePanel] onResponseComplete queued on EDT");
            return;
        }
        if (modeModel.current() != AppMode.STORY) {
            System.out.println("[LeftPanePanel] onResponseComplete exit non-story mode");
            return;
        }
        OptionalInt parsed = BeatParser.parse(text);
        if (!parsed.isPresent()) {
            System.out.println("[LeftPanePanel] onResponseComplete no beat parsed");
            System.out.println("[LeftPanePanel] onResponseComplete exit");
            return;
        }
        System.out.println("[LeftPanePanel] onResponseComplete parsed beat=" + parsed.getAsInt());
        CurrentBeatModel.UpdateResult result = currentBeatModel.update(parsed.getAsInt(), text);
        System.out.println("[LeftPanePanel] onResponseComplete currentBeatModel result=" + result.kind);
        if (result.kind == CurrentBeatModel.ResultKind.ROLLED_OVER) {
            System.out.println("[LeftPanePanel] onResponseComplete appending rolled-over beat to canon");
            canonPanel.appendEntry(result.textToAppend);
            System.out.println("[LeftPanePanel] onResponseComplete canon append done");
        }
        outputPanel.setResponse(text, html, parsed.getAsInt());
        System.out.println("[LeftPanePanel] onResponseComplete exit");
    }

    public void onResponseComplete(String text) {
        onResponseComplete(text, null);
    }

    public void focusResponse() {
        outputPanel.focusResponse();
    }

    public void clearResponse() {
        outputPanel.clear();
    }

    public void endSession() {
        if (currentBeatModel.hasUnappendedBeat()) {
            canonPanel.appendEntry(currentBeatModel.getText());
            currentBeatModel.markAppended();
        }
        canonPanel.saveCanon();
    }
}
