import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * Holds all configuration values parsed from a .jbt file.
 *
 * Schema:
 *   ProjectFolder         - absolute path to project root
 *   SourceSet             - "main" | "test"
 *   PackageFolder         - e.g. "com.example.a" (dot-separated); empty = scan all
 *   Output                - relative output path (e.g. "build")
 *   BuildMode             - "Normal" | "Onefile"
 *   Library               - array of jar paths (relative to ProjectFolder)
 *   CloseWhenBuildFinish  - exit after build completes
 *   TerminalOnly          - headless build (no GUI)
 *   Resources.AddResources
 *   Resources.ResourcesPath - empty → default: src/{SourceSet}/resources/{PackageFolder}
 *   Icon.AddIcon
 *   Icon.IconPath          - empty → default: src/{SourceSet}/resources/{PackageFolder}/icon.ico
 *   DebugBuild            - verbose logging
 */
public class JBTConfig {

    // ── Fields ───────────────────────────────────────────────────────────────
    public String  projectFolder        = System.getProperty("user.dir");
    public String  sourceSet            = "main";   // main | test
    public String  packageFolder        = "";        // e.g. "com.example.a"
    public String  mainClass            = "Main";   // e.g. "Main" → resolved as {packageFolder}.Main
    public String  output               = "build";
    public String  buildMode            = "Normal";  // Normal | Onefile
    public ArrayList<String> library    = new ArrayList<>();
    public boolean closeWhenBuildFinish = false;
    public boolean terminalOnly         = false;

    // Resources
    public boolean addResources         = false;
    public String  resourcesPath        = "";        // empty = use default

    // Icon
    public boolean addIcon              = false;
    public String  iconPath             = "";        // empty = use default

    public boolean debugBuild           = false;

    /** Path of the loaded .jbt file (null when not loaded from file). */
    public String  configFilePath       = null;

    // ── Factory ──────────────────────────────────────────────────────────────

    public static JBTConfig loadFromFile(String path) {
        try {
            String json = new String(Files.readAllBytes(Paths.get(path)));
            JBTConfig cfg = new JBTConfig();
            cfg.configFilePath = new File(path).getAbsolutePath();
            cfg.parse(json);
            // Resolve "." → directory of the .jbt file
            if (cfg.projectFolder.equals(".")) {
                cfg.projectFolder = new File(path).getParentFile().getAbsolutePath();
            }
            return cfg;
        } catch (Exception e) {
            System.err.println("[JBTConfig] Failed to load: " + e.getMessage());
            return null;
        }
    }

    // ── Parser ───────────────────────────────────────────────────────────────

    void parse(String json) {
        // Strip single-line comments (// ...) so the parser isn't confused
        String clean = stripComments(json);

        String v;
        v = parseString(clean, "ProjectFolder"); if (!v.isEmpty()) projectFolder = v;
        v = parseString(clean, "SourceSet");     if (!v.isEmpty()) sourceSet     = v;
        v = parseString(clean, "PackageFolder"); packageFolder = v;  // allow empty
        v = parseString(clean, "MainClass");     if (!v.isEmpty()) mainClass = v;
        v = parseString(clean, "Output");        if (!v.isEmpty()) output        = v;
        v = parseString(clean, "BuildMode");     if (!v.isEmpty()) buildMode     = v;

        ArrayList<String> libs = parseArray(clean, "Library");
        if (!libs.isEmpty()) library = libs;

        closeWhenBuildFinish = parseBoolean(clean, "CloseWhenBuildFinish");
        terminalOnly         = parseBoolean(clean, "TerminalOnly");
        debugBuild           = parseBoolean(clean, "DebugBuild");

        // Resources (nested object)
        addResources  = parseNestedBoolean(clean, "Resources", "AddResources");
        v = parseNestedString(clean, "Resources", "ResourcesPath"); resourcesPath = v;

        // Icon (nested object)
        addIcon  = parseNestedBoolean(clean, "Icon", "AddIcon");
        v = parseNestedString(clean, "Icon", "IconPath"); iconPath = v;
    }

    // ── Serialiser ───────────────────────────────────────────────────────────

    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("    \"ProjectFolder\": \"").append(escape(projectFolder)).append("\",\n");
        sb.append("    \"SourceSet\": \"").append(escape(sourceSet)).append("\",\n");
        sb.append("    \"PackageFolder\": \"").append(escape(packageFolder)).append("\",\n");
        sb.append("    \"MainClass\": \"").append(escape(mainClass)).append("\",\n");
        sb.append("    \"Output\": \"").append(escape(output)).append("\",\n");
        sb.append("    \"BuildMode\": \"").append(escape(buildMode)).append("\",\n");
        sb.append("    \"Library\": [");
        if (library.isEmpty()) {
            sb.append("],\n");
        } else {
            sb.append("\n");
            for (int i = 0; i < library.size(); i++) {
                sb.append("        \"").append(escape(library.get(i))).append("\"");
                if (i < library.size() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append("    ],\n");
        }
        sb.append("    \"CloseWhenBuildFinish\": ").append(closeWhenBuildFinish).append(",\n");
        sb.append("    \"TerminalOnly\": ").append(terminalOnly).append(",\n");
        sb.append("    \"Resources\": {\n");
        sb.append("        \"AddResources\": ").append(addResources).append(",\n");
        sb.append("        \"ResourcesPath\": \"").append(escape(resourcesPath)).append("\"\n");
        sb.append("    },\n");
        sb.append("    \"Icon\": {\n");
        sb.append("        \"AddIcon\": ").append(addIcon).append(",\n");
        sb.append("        \"IconPath\": \"").append(escape(iconPath)).append("\"\n");
        sb.append("    },\n");
        sb.append("    \"DebugBuild\": ").append(debugBuild).append("\n");
        sb.append("}");
        return sb.toString();
    }

    // ── Path resolvers ───────────────────────────────────────────────────────

    /**
     * Effective Java source scan root:
     *   PackageFolder = ""            → src/{sourceSet}/java
     *   PackageFolder = "com.example" → src/{sourceSet}/java/com/example
     */
    public String resolvedSrcFolder() {
        String base = "src/" + sourceSet + "/java";
        if (!packageFolder.isEmpty()) {
            base += "/" + packageFolder.replace(".", "/");
        }
        return base;
    }

    /**
     * Fully-qualified main class name for MANIFEST.MF:
     *   PackageFolder = ""            → "Main"
     *   PackageFolder = "com.example" → "com.example.Main"
     */
    public String resolvedMainClass() {
        if (packageFolder.isEmpty()) return mainClass;
        return packageFolder + "." + mainClass;
    }

    /**
     * Effective resources folder:
     *   ResourcesPath non-empty → use as-is
     *   Otherwise → src/{sourceSet}/resources[/{packageFolder as path}]
     */
    public String resolvedResourcesPath() {
        if (!resourcesPath.isEmpty()) return resourcesPath;
        String base = "src/" + sourceSet + "/resources";
        if (!packageFolder.isEmpty()) {
            base += "/" + packageFolder.replace(".", "/");
        }
        return base;
    }

    /**
     * Effective icon file path:
     *   IconPath non-empty → use as-is
     *   Otherwise → {resolvedResourcesPath()}/icon.ico
     */
    public String resolvedIconPath() {
        if (!iconPath.isEmpty()) return iconPath;
        return resolvedResourcesPath() + "/icon.ico";
    }

    // ── Low-level JSON helpers ────────────────────────────────────────────────

    /** Remove // single-line comments so values containing "//" aren't broken. */
    private String stripComments(String json) {
        StringBuilder sb = new StringBuilder();
        boolean inString = false;
        int i = 0;
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '"' && (i == 0 || json.charAt(i - 1) != '\\')) {
                inString = !inString;
                sb.append(c);
                i++;
            } else if (!inString && c == '/' && i + 1 < json.length() && json.charAt(i + 1) == '/') {
                // skip until end of line
                while (i < json.length() && json.charAt(i) != '\n') i++;
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }

    private String parseString(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int ki = json.indexOf(searchKey);
        if (ki == -1) return "";
        int ci = json.indexOf(":", ki + searchKey.length());
        if (ci == -1) return "";
        int vs = json.indexOf("\"", ci + 1);
        if (vs == -1) return "";
        int ve = json.indexOf("\"", vs + 1);
        if (ve == -1) return "";
        return json.substring(vs + 1, ve).replace("\\\\", "\\");
    }

    private boolean parseBoolean(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int ki = json.indexOf(searchKey);
        if (ki == -1) return false;
        int ci = json.indexOf(":", ki + searchKey.length());
        if (ci == -1) return false;
        return json.substring(ci + 1).trim().startsWith("true");
    }

    private ArrayList<String> parseArray(String json, String key) {
        ArrayList<String> list = new ArrayList<>();
        String searchKey = "\"" + key + "\"";
        int ki = json.indexOf(searchKey);
        if (ki == -1) return list;
        int ci = json.indexOf(":", ki + searchKey.length());
        int as = json.indexOf("[", ci);
        int ae = json.indexOf("]", as);
        if (as == -1 || ae == -1) return list;
        String content = json.substring(as + 1, ae).trim();
        if (content.isEmpty()) return list;
        for (String item : content.split(",")) {
            item = item.trim();
            if (item.startsWith("\"") && item.endsWith("\""))
                item = item.substring(1, item.length() - 1);
            if (!item.isEmpty()) list.add(item.replace("\\\\", "\\"));
        }
        return list;
    }

    private boolean parseNestedBoolean(String json, String outer, String inner) {
        int oi = json.indexOf("\"" + outer + "\"");
        if (oi == -1) return false;
        int ob = json.indexOf("{", oi);
        int oe = json.indexOf("}", ob);
        if (ob == -1 || oe == -1) return false;
        return parseBoolean("{" + json.substring(ob + 1, oe) + "}", inner);
    }

    private String parseNestedString(String json, String outer, String inner) {
        int oi = json.indexOf("\"" + outer + "\"");
        if (oi == -1) return "";
        int ob = json.indexOf("{", oi);
        int oe = json.indexOf("}", ob);
        if (ob == -1 || oe == -1) return "";
        return parseString("{" + json.substring(ob + 1, oe) + "}", inner);
    }

    private String escape(String s) {
        return s.replace("\\", "\\\\");
    }
}
