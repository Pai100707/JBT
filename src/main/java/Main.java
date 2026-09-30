import javax.swing.SwingUtilities;
import java.io.File;

/**
 * Entry point for JBT (Java Build Tool).
 *
 * Argument modes
 * ──────────────
 * (none)
 *     Open GUI with default settings (TerminalOnly = false).
 *
 * -jbtcfg <path.jbt>
 *     Load the config file and run immediately (ignores all other args).
 *     If TerminalOnly = true → build without showing GUI (terminal mode).
 *     If TerminalOnly = false → show GUI pre-filled and optionally auto-build.
 *
 * -<Key> <value>  (multiple pairs allowed)
 *     Override individual config values.  Supported keys:
 *       CloseWhenBuildFinish  true|false
 *       TerminalOnly          true|false
 *       AddResources          true|false
 *       Debug                 true|false
 *       ProjectFolder         <path>
 *       SrcFolder             <relative path>
 *       Output                <relative path>
 *       BuildMode             Normal|Onefile
 *       AutoBuild             true|false
 */
public class Main {

    public static void main(String[] args) {

        // ── No arguments: open GUI with defaults ──────────────────────────
        if (args.length == 0) {
            launchGui(new JBTConfig(), null);
            return;
        }

        // ── Check for -jbtcfg <path> ──────────────────────────────────────
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equalsIgnoreCase("-jbtcfg")) {
                String path = args[i + 1];
                File f = new File(path);
                if (!f.exists() || !f.isFile()) {
                    System.err.println("[JBT] Config file not found: " + path);
                    System.exit(1);
                }
                JBTConfig cfg = JBTConfig.loadFromFile(f.getAbsolutePath());
                if (cfg == null) {
                    System.err.println("[JBT] Failed to parse config file: " + path);
                    System.exit(1);
                }
                run(cfg);
                return;
            }
        }

        // ── Parse -Key Value pairs ────────────────────────────────────────
        JBTConfig cfg = new JBTConfig();
        for (int i = 0; i < args.length - 1; i += 2) {
            String key   = args[i].replaceFirst("^-+", "");  // strip leading dashes
            String value = args[i + 1];
            applyArg(cfg, key, value);
        }
        run(cfg);
    }

    // ── Route to terminal or GUI ──────────────────────────────────────────────

    private static void run(JBTConfig cfg) {
        if (cfg.terminalOnly) {
            runTerminal(cfg);
        } else {
            launchGui(cfg, cfg.configFilePath);
        }
    }

    /** Terminal (headless) build — no GUI opened. */
    private static void runTerminal(JBTConfig cfg) {
        Logger logger = new Logger(cfg.debug, msg -> System.out.print(msg));

        logger.log("JBT starting in terminal mode");
        if (cfg.debug) logger.log("Config: " + cfg.configFilePath);

        Build builder = new Build();
        try {
            builder.StartBuild(cfg, logger);
            logger.log("Build finished successfully.");
        } catch (Exception e) {
            logger.error("Build failed: " + e.getMessage());
            System.exit(1);
        }

        if (cfg.closeWhenBuildFinish) {
            System.exit(0);
        }
    }

    /** Launch Swing GUI. */
    private static void launchGui(JBTConfig cfg, String configFilePath) {
        final JBTConfig finalCfg = cfg;
        SwingUtilities.invokeLater(() -> {
            GUI window = new GUI();
            window.ShowGui(finalCfg);
        });
    }

    // ── Argument application ─────────────────────────────────────────────────

    private static void applyArg(JBTConfig cfg, String key, String value) {
        switch (key) {
            case "CloseWhenBuildFinish": cfg.closeWhenBuildFinish = toBool(value); break;
            case "TerminalOnly":         cfg.terminalOnly         = toBool(value); break;
            case "AddResources":         cfg.addResources         = toBool(value); break;
            case "Debug":                cfg.debug                = toBool(value); break;
            case "AutoBuild":            cfg.autoBuild            = toBool(value); break;
            case "ProjectFolder":        cfg.projectFolder        = value;         break;
            case "SrcFolder":            cfg.srcFolder            = value;         break;
            case "Output":               cfg.output               = value;         break;
            case "BuildMode":            cfg.buildMode            = value;         break;
            default:
                System.err.println("[JBT] Unknown argument: -" + key);
        }
    }

    private static boolean toBool(String s) {
        return "true".equalsIgnoreCase(s.trim());
    }
}
