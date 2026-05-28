package com.chatstory.spike;

import me.friwi.jcefmaven.CefAppBuilder;
import me.friwi.jcefmaven.impl.progress.ConsoleProgressHandler;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.browser.CefMessageRouter;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefLoadHandlerAdapter;
import org.cef.handler.CefMessageRouterHandlerAdapter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

/**
 * DC-001 Spike: Minimum JCEF browser window loading ChatGPT.
 * This class is temporary and will be replaced in DC-002.
 *
 * Goals:
 *   - Prove JCEF launches and renders ChatGPT on this machine
 *   - Confirm persistent login session across restarts
 *   - Confirm DevTools are accessible
 *   - Stretch: confirm CefMessageRouter JS→Java channel works
 */
public class Main {

    private static final String TARGET_URL = "https://chatgpt.com";

    private static final String PING_JS =
        "if (typeof window.cefQuery === 'function') {" +
        "  window.cefQuery({" +
        "    request: JSON.stringify({ type: 'ping' })," +
        "    onSuccess: function(r) { console.log('[DC001-ping] response:', r); }," +
        "    onFailure: function(e, m) { console.error('[DC001-ping] error:', e, m); }" +
        "  });" +
        "} else { console.warn('[DC001-ping] cefQuery not available'); }";

    public static void main(String[] args) throws Exception {
        // Resolve profile directory for session persistence
        String localAppData = System.getenv("LOCALAPPDATA");
        String base = (localAppData != null && !localAppData.isBlank())
                ? localAppData
                : System.getProperty("user.home");
        String profilePath = base + File.separator + "ChatStory" + File.separator + "profile";

        File installDir = new File("jcef-bundle");

        System.out.println("=== Story Workstation DC-001 Spike ===");
        System.out.println("JCEF bundle dir : " + installDir.getAbsolutePath());
        System.out.println("Profile path    : " + profilePath);
        System.out.println("Target URL      : " + TARGET_URL);
        System.out.println("On first launch JCEF natives will be downloaded (~100 MB).");
        System.out.println("======================================");

        // Build JCEF — downloads and extracts Chromium natives on first run
        CefAppBuilder builder = new CefAppBuilder();
        builder.setInstallDir(installDir);
        builder.setProgressHandler(new ConsoleProgressHandler());
        builder.getCefSettings().windowless_rendering_enabled = false;
        builder.getCefSettings().cache_path = profilePath;

        CefApp cefApp;
        try {
            cefApp = builder.build();
        } catch (Exception e) {
            System.err.println("JCEF initialization failed: " + e.getMessage());
            System.err.println("Check BUILDING.md for setup requirements.");
            System.exit(1);
            return;
        }

        // Dispose JCEF cleanly on JVM shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Disposing CefApp...");
            CefApp.getInstance().dispose();
        }, "cef-shutdown"));

        // --- Bridge setup (stretch goal: CefMessageRouter ping) ---
        CefClient client = cefApp.createClient();

        CefMessageRouter.CefMessageRouterConfig routerConfig = new CefMessageRouter.CefMessageRouterConfig();
        routerConfig.jsQueryFunction = "cefQuery";
        routerConfig.jsCancelFunction = "cefQueryCancel";
        CefMessageRouter router = CefMessageRouter.create(routerConfig);
        router.addHandler(new CefMessageRouterHandlerAdapter() {
            @Override
            public boolean onQuery(CefBrowser browser, CefFrame frame, long queryId,
                                   String request, boolean persistent, CefQueryCallback callback) {
                System.out.println("[Bridge] Received from JS: " + request);
                callback.success("ok");
                return true;
            }

            @Override
            public void onQueryCanceled(CefBrowser browser, CefFrame frame, long queryId) {
                System.out.println("[Bridge] Query cancelled: " + queryId);
            }
        }, true);
        client.addMessageRouter(router);

        // Inject ping after each main-frame page load
        client.addLoadHandler(new CefLoadHandlerAdapter() {
            @Override
            public void onLoadEnd(CefBrowser browser, CefFrame frame, int httpStatusCode) {
                if (frame.isMain()) {
                    browser.executeJavaScript(PING_JS, frame.getURL(), 0);
                }
            }
        });

        // Create the browser pointed at ChatGPT
        CefBrowser browser = client.createBrowser(TARGET_URL, false, false);

        // Build the Swing UI on the EDT
        SwingUtilities.invokeLater(() -> buildUI(browser, client));
    }

    private static void buildUI(CefBrowser browser, CefClient client) {
        JFrame frame = new JFrame("Story Workstation – DC001 Spike");
        frame.setSize(1400, 900);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout());

        // Toolbar
        JButton devToolsBtn = new JButton("DevTools");
        devToolsBtn.setToolTipText("Open Chromium DevTools for this page");
        devToolsBtn.addActionListener(e -> browser.openDevTools());

        JLabel statusLabel = new JLabel(" DC-001 Spike  |  Log in at ChatGPT, then restart to confirm session persistence.");
        statusLabel.setForeground(Color.DARK_GRAY);

        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        toolbar.add(devToolsBtn, BorderLayout.WEST);
        toolbar.add(statusLabel, BorderLayout.CENTER);

        frame.add(toolbar, BorderLayout.NORTH);
        frame.add(browser.getUIComponent(), BorderLayout.CENTER);

        // On close: let the shutdown hook handle CefApp.dispose()
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                frame.dispose();
                System.exit(0);
            }
        });

        frame.setVisible(true);
    }
}
