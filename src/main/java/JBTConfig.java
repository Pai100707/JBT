import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * Holds all configuration values parsed from a .jbt file.
 * 
 * Supported fields:
 *   sys          - config type tag (e.g. "run")
 *   ProjectFolder
 *   SrcFolder    - relative to ProjectFolder, under src/main/ (not src/main/java/)
 *   Output
 *   BuildMode    - "Normal" | "Onefile"
 *   Library      - array of paths
 *   AutoBuild    - (legacy) same as TerminalOnly=false + auto-trigger build
 *   CloseWhenBuildFinish
 *   TerminalOnly
 *   AddResources
 *   Icon.HasIcon
 *   Icon.IconPath
 *   Debug
 */
public class JBTConfig {

    // ── fields ──────────────────────────────────────────────────────────────
    public String  sys                   = "";
    public String  projectFolder         = System.getProperty("user.dir");
    public String  srcFolder             = "src/main/java";   // kept for back-compat display
    public String  output                = "build";
    public String  buildMode             = "Normal";
    public ArrayList<String> library     = new ArrayList<>();
    public boolean autoBuild             = false;
    public boolean closeWhenBuildFinish  = false;
    public boolean terminalOnly          = false;
    public boolean addResources          = false;
    public boolean hasIcon               = false;
    public String  iconPath              = "";
    public boolean debug                 = false;

    /** Raw config file path (may be null when config was not loaded from file) */
    public String  configFilePath        = null;

    // ── factory ─────────────────────────────────────────────────────────────

    /**
     * Load and parse a .jbt file.  Returns null and prints a message if the
     * file cannot be read or parsed.
     */
    public static JBTConfig loadFromFile(String path) {
        try {
            String json = new String(Files.readAllBytes(Paths.get(path)));
            JBTConfig cfg = new JBTConfig();
            cfg.configFilePath = new File(path).getAbsolutePath();
            cfg.parse(json);
            return cfg;
        } catch (Exception e) {
            System.err.println("[JBTConfig] Failed to load config: " + e.getMessage());
            return null;
        }
    }

    // ── parser ───────────────────────────────────────────────────────────────

    void parse(String json) {
        String rawSys = parseString(json, "sys");
        if (!rawSys.isEmpty()) sys = rawSys;

        String rawProject = parseString(json, "ProjectFolder");
        if (!rawProject.isEmpty()) projectFolder = rawProject;

        /* SrcFolder — the new spec uses {ProjectFolder}/src/main/<SrcFolder>
         * so a value like "java" resolves to src/main/java.
         * We also still accept the old full-relative form "src/main/java". */
        String rawSrc = parseString(json, "SrcFolder");
        if (!rawSrc.isEmpty()) srcFolder = rawSrc;

        String rawOutput = parseString(json, "Output");
        if (!rawOutput.isEmpty()) output = rawOutput;

        String rawBuildMode = parseString(json, "BuildMode");
        if (!rawBuildMode.isEmpty()) buildMode = rawBuildMode;

        ArrayList<String> libs = parseArray(json, "Library");
        if (libs.isEmpty()) libs = parseArray(json, "Libary"); // typo compat
        library = libs;

        autoBuild            = parseBoolean(json, "AutoBuild");
        closeWhenBuildFinish = parseBoolean(json, "CloseWhenBuildFinish");
        terminalOnly         = parseBoolean(json, "TerminalOnly");
        addResources         = parseBoolean(json, "AddResources");
        debug                = parseBoolean(json, "Debug");

        // Icon is a nested object: "Icon": { "HasIcon": true, "IconPath": "..." }
        hasIcon  = parseNestedBoolean(json, "Icon", "HasIcon");
        iconPath = parseNestedString(json, "Icon", "IconPath");
    }

    // ── serialiser ───────────────────────────────────────────────────────────

    /** Produce a pretty-printed .jbt JSON string from current field values. */
    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        if (!sys.isEmpty())
            sb.append("    \"sys\": \"").append(escape(sys)).append("\",\n");
        sb.append("    \"ProjectFolder\": \"").append(escape(projectFolder)).append("\",\n");
        sb.append("    \"SrcFolder\": \"").append(escape(srcFolder)).append("\",\n");
        sb.append("    \"Output\": \"").append(escape(output)).append("\",\n");
        sb.append("    \"BuildMode\": \"").append(escape(buildMode)).append("\",\n");
        sb.append("    \"Library\": [\n");
        for (int i = 0; i < library.size(); i++) {
            sb.append("        \"").append(escape(library.get(i))).append("\"");
            if (i < library.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("    ],\n");
        sb.append("    \"AutoBuild\": ").append(autoBuild).append(",\n");
        sb.append("    \"CloseWhenBuildFinish\": ").append(closeWhenBuildFinish).append(",\n");
        sb.append("    \"TerminalOnly\": ").append(terminalOnly).append(",\n");
        sb.append("    \"AddResources\": ").append(addResources).append(",\n");
        sb.append("    \"Icon\": {\n");
        sb.append("        \"HasIcon\": ").append(hasIcon).append(",\n");
        sb.append("        \"IconPath\": \"").append(escape(iconPath)).append("\"\n");
        sb.append("    },\n");
        sb.append("    \"Debug\": ").append(debug).append("\n");
        sb.append("}");
        return sb.toString();
    }

    // ── helper: resolve effective src path ──────────────────────────────────

    /**
     * Returns the effective source folder path for compilation.
     *
     * New spec: SrcFolder is relative to {ProjectFolder}/src/main/
     *   e.g. SrcFolder = "java"  → {project}/src/main/java
     *        SrcFolder = "kotlin" → {project}/src/main/kotlin
     *
     * Legacy / explicit: if SrcFolder already starts with "src" or is absolute
     *   it is used as-is (relative to project or absolute).
     */
    public String resolvedSrcFolder() {
        if (srcFolder.startsWith("src") || new File(srcFolder).isAbsolute()) {
            return srcFolder;
        }
        // New compact form: treat as sub-folder of src/main/
        return "src/main/" + srcFolder;
    }

    // ── low-level JSON parsing helpers ───────────────────────────────────────

    private String parseString(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int ki = json.indexOf(searchKey);
        if (ki == -1) return "";
        int ci = json.indexOf(":", ki);
        int vs = json.indexOf("\"", ci);
        if (vs == -1) return "";
        int ve = json.indexOf("\"", vs + 1);
        if (ve == -1) return "";
        return json.substring(vs + 1, ve).replace("\\\\", "\\");
    }

    private boolean parseBoolean(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int ki = json.indexOf(searchKey);
        if (ki == -1) return false;
        int ci = json.indexOf(":", ki);
        if (ci == -1) return false;
        return json.substring(ci + 1).trim().startsWith("true");
    }

    private ArrayList<String> parseArray(String json, String key) {
        ArrayList<String> list = new ArrayList<>();
        String searchKey = "\"" + key + "\"";
        int ki = json.indexOf(searchKey);
        if (ki == -1) return list;
        int ci = json.indexOf(":", ki);
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

    /** Parse a boolean inside a nested object: "outerKey": { "innerKey": true } */
    private boolean parseNestedBoolean(String json, String outerKey, String innerKey) {
        String outerSearch = "\"" + outerKey + "\"";
        int oi = json.indexOf(outerSearch);
        if (oi == -1) return false;
        int ob = json.indexOf("{", oi);
        int oe = json.indexOf("}", ob);
        if (ob == -1 || oe == -1) return false;
        String nested = json.substring(ob + 1, oe);
        return parseBoolean("{" + nested + "}", innerKey);
    }

    /** Parse a string inside a nested object: "outerKey": { "innerKey": "value" } */
    private String parseNestedString(String json, String outerKey, String innerKey) {
        String outerSearch = "\"" + outerKey + "\"";
        int oi = json.indexOf(outerSearch);
        if (oi == -1) return "";
        int ob = json.indexOf("{", oi);
        int oe = json.indexOf("}", ob);
        if (ob == -1 || oe == -1) return "";
        String nested = json.substring(ob + 1, oe);
        return parseString("{" + nested + "}", innerKey);
    }

    private String escape(String s) {
        return s.replace("\\", "\\\\");
    }
}
