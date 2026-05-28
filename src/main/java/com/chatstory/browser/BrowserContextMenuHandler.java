package com.chatstory.browser;

import com.chatstory.UiThread;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefContextMenuParams;
import org.cef.callback.CefMenuModel;
import org.cef.handler.CefContextMenuHandlerAdapter;

import java.util.function.Consumer;

public class BrowserContextMenuHandler extends CefContextMenuHandlerAdapter {

    private static final int COPY_TO_RESPONSE_ID = CefMenuModel.MenuId.MENU_ID_USER_FIRST;

    private final Consumer<String> onCopyToResponse;

    public BrowserContextMenuHandler(Consumer<String> onCopyToResponse) {
        this.onCopyToResponse = onCopyToResponse;
    }

    @Override
    public void onBeforeContextMenu(CefBrowser browser, CefFrame frame,
                                    CefContextMenuParams params, CefMenuModel model) {
        String selection = params.getSelectionText();
        if (selection != null && !selection.isBlank()) {
            model.addSeparator();
            model.addItem(COPY_TO_RESPONSE_ID, "Copy to Response Window");
        }
    }

    @Override
    public boolean onContextMenuCommand(CefBrowser browser, CefFrame frame,
                                        CefContextMenuParams params, int commandId, int eventFlags) {
        if (commandId == COPY_TO_RESPONSE_ID) {
            String text = params.getSelectionText();
            if (text != null && !text.isBlank()) {
                UiThread.run(() -> onCopyToResponse.accept(text));
            }
            return true;
        }
        return false;
    }
}
