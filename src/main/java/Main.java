import javax.swing.SwingUtilities;
import java.io.File;

/**
 * Entry point for JBT (Java Build Tool).
 *
 * Argument modes
 * ──────────────
 * (none)
 *     Open GUI with default settings.
 *
 * -jbtcfg <path.jbt>
 *     Load the config file.
 *     TerminalOnly = true  → build immediately, no GUI.
 *     TerminalOnly = false → open GUI pre-filled (user must click Start Build).
 *
 * -<Key> <value>  (multiple pairs)
 *     Override individual config values. Same routing as above.
 *
 *     Supported keys:
 *       ProjectFolder, SourceSet, PackageFolder, Output, BuildMode
 *       CloseWhenBuildFinish, TerminalOnly, DebugBuild
 */
public class Main {

    public static void main(String[] args) {

        // ── No arguments → open GUI ───────────────────────────────────────
        if (args.length == 0) {
            launchGui(new JBTConfig());
            return;
        }

        // ── -jbtcfg <path> ────────────────────────────────────────────────
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equalsIgnoreCase("-jbtcfg")) {
                File f = new File(args[i + 1]);
                if (!f.exists() || !f.isFile()) {
                    System.err.println("[JBT] Config file not found: " + args[i + 1]);
                    System.exit(1);
                }
                JBTConfig cfg = JBTConfig.loadFromFile(f.getAbsolutePath());
                if (cfg == null) {
                    System.err.println("[JBT] Failed to parse config: " + args[i + 1]);
                    System.exit(1);
                }
                route(cfg);
                return;
            }
        }

        // ── -Key Value pairs ──────────────────────────────────────────────
        JBTConfig cfg = new JBTConfig();
        for (int i = 0; i + 1 < args.length; i += 2) {
            applyArg(cfg, args[i].replaceFirst("^-+", ""), args[i + 1]);
        }
        route(cfg);
    }

    // ── Routing ──────────────────────────────────────────────────────────────

    private static void route(JBTConfig cfg) {
        if (cfg.terminalOnly) runTerminal(cfg);
        else                  launchGui(cfg);
    }

    /** Headless build — prints to stdout, no GUI. */
    private static void runTerminal(JBTConfig cfg) {
        Logger logger = new Logger(cfg.debugBuild, msg -> System.out.print(msg));
        logger.log("JBT starting in terminal mode");
        if (cfg.debugBuild) logger.log("Config: " + cfg.configFilePath);

        try {
            new Build().StartBuild(cfg, logger);
        } catch (Exception e) {
            logger.error("Build failed: " + e.getMessage());
            System.exit(1);
        }

        if (cfg.closeWhenBuildFinish) System.exit(0);
    }

    /** Launch Swing GUI — user must click Start Build. */
    private static void launchGui(JBTConfig cfg) {
        SwingUtilities.invokeLater(() -> new GUI().ShowGui(cfg));
    }

    // ── Argument application ──────────────────────────────────────────────────

    private static void applyArg(JBTConfig cfg, String key, String value) {
        switch (key) {
            case "ProjectFolder":        cfg.projectFolder        = value;         break;
            case "SourceSet":            cfg.sourceSet            = value;         break;
            case "PackageFolder":        cfg.packageFolder        = value;         break;
            case "Output":               cfg.output               = value;         break;
            case "BuildMode":            cfg.buildMode            = value;         break;
            case "CloseWhenBuildFinish": cfg.closeWhenBuildFinish = toBool(value); break;
            case "TerminalOnly":         cfg.terminalOnly         = toBool(value); break;
            case "DebugBuild":           cfg.debugBuild           = toBool(value); break;
            default:
                System.err.println("[JBT] Unknown argument: -" + key);
        }
    }

    private static boolean toBool(String s) {
        return "true".equalsIgnoreCase(s.trim());
    }
}
