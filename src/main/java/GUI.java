import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

public class GUI {

    // ── State ────────────────────────────────────────────────────────────────
    private File   selectedLibFile = null;
    private String loadedConfigPath = null;   // path of the currently-loaded .jbt

    // ── Log textarea (used by Logger sink) ───────────────────────────────────
    private JTextArea txtLog;

    /** Append text to the log area (thread-safe). */
    public void Log(String str) {
        if (txtLog == null) return;
        SwingUtilities.invokeLater(() -> {
            txtLog.append(str);
            txtLog.setCaretPosition(txtLog.getDocument().getLength());
        });
    }

    // ── Show ─────────────────────────────────────────────────────────────────

    public void ShowGui(JBTConfig cfg) {
        loadedConfigPath = cfg.configFilePath;

        JFrame frame = new JFrame("Java Build Tool (JBT)");
        frame.setSize(960, 620);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}

        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets  = new Insets(5, 5, 5, 5);
        gbc.anchor  = GridBagConstraints.WEST;

        int row = 0;

        // ── Row 0: Project Folder ────────────────────────────────────────
        addLabel(mainPanel, gbc, row, "Project Folder");
        JTextField txtProject = new JTextField(cfg.projectFolder);
        addField(mainPanel, gbc, row, 1, 2, txtProject);
        JButton btnBrowseProject = new JButton("Browse");
        addWidget(mainPanel, gbc, row, 3, 1, btnBrowseProject);
        row++;

        // ── Row 1: Source Folder ─────────────────────────────────────────
        addLabel(mainPanel, gbc, row, "Source Folder");
        JTextField txtSrc = new JTextField(cfg.srcFolder);
        addField(mainPanel, gbc, row, 1, 3, txtSrc);
        row++;

        // ── Row 2: Output Folder ─────────────────────────────────────────
        addLabel(mainPanel, gbc, row, "Output Folder");
        JTextField txtOutput = new JTextField(cfg.output);
        addField(mainPanel, gbc, row, 1, 3, txtOutput);
        row++;

        // ── Row 3: Library ──────────────────────────────────────────────
        addLabel(mainPanel, gbc, row, "Library");
        DefaultComboBoxModel<String> libModel = new DefaultComboBoxModel<>();
        for (String lib : cfg.library) libModel.addElement(lib);
        JComboBox<String> comboLibrary = new JComboBox<>(libModel);
        addField(mainPanel, gbc, row, 1, 1, comboLibrary);
        JButton btnDeleteSelected = new JButton("Delete selected");
        addWidget(mainPanel, gbc, row, 2, 1, btnDeleteSelected);
        JButton btnAddLibrary = new JButton("ADD");
        addWidget(mainPanel, gbc, row, 3, 1, btnAddLibrary);
        JButton btnBrowseLib = new JButton("Browse library");
        addWidget(mainPanel, gbc, row, 4, 1, btnBrowseLib);
        row++;

        // ── Row 4: Build Mode + options ──────────────────────────────────
        addLabel(mainPanel, gbc, row, "Build Mode");
        String[] buildModes = { "Normal", "Onefile" };
        JComboBox<String> comboBuildMode = new JComboBox<>(buildModes);
        comboBuildMode.setSelectedItem(cfg.buildMode);
        addField(mainPanel, gbc, row, 1, 1, comboBuildMode);

        JCheckBox chkAddResources = new JCheckBox("Add Resources", cfg.addResources);
        addWidget(mainPanel, gbc, row, 2, 1, chkAddResources);

        JCheckBox chkClose = new JCheckBox("Close When Finish", cfg.closeWhenBuildFinish);
        addWidget(mainPanel, gbc, row, 3, 1, chkClose);

        JCheckBox chkDebug = new JCheckBox("Debug", cfg.debug);
        addWidget(mainPanel, gbc, row, 4, 1, chkDebug);
        row++;

        // ── Row 5: Icon ──────────────────────────────────────────────────
        addLabel(mainPanel, gbc, row, "Icon");
        JCheckBox chkHasIcon = new JCheckBox("Pack Icon", cfg.hasIcon);
        addWidget(mainPanel, gbc, row, 1, 1, chkHasIcon);
        JTextField txtIconPath = new JTextField(cfg.iconPath.isEmpty() ? "(no icon)" : cfg.iconPath);
        txtIconPath.setEnabled(cfg.hasIcon);
        addField(mainPanel, gbc, row, 2, 1, txtIconPath);
        JButton btnBrowseIcon = new JButton("Browse Icon");
        btnBrowseIcon.setEnabled(cfg.hasIcon);
        addWidget(mainPanel, gbc, row, 3, 1, btnBrowseIcon);
        row++;

        // ── Row 6: Config bar (Load / Save / Build) ───────────────────────
        JButton btnLoadConfig = new JButton("Load Config (.jbt)");
        addWidget(mainPanel, gbc, row, 0, 1, btnLoadConfig);

        JLabel lblConfigStatus = new JLabel(loadedConfigPath != null
            ? "Config: " + new File(loadedConfigPath).getName()
            : "No config loaded");
        lblConfigStatus.setForeground(loadedConfigPath != null ? new Color(0, 128, 0) : Color.GRAY);
        addField(mainPanel, gbc, row, 1, 2, lblConfigStatus);

        JButton btnSaveConfig = new JButton("Save Config");
        addWidget(mainPanel, gbc, row, 3, 1, btnSaveConfig);
        JButton btnBuild = new JButton("Start Build");
        btnBuild.setFont(new Font("SansSerif", Font.BOLD, 12));
        addWidget(mainPanel, gbc, row, 4, 1, btnBuild);
        row++;

        // ── Row 7: Log area ───────────────────────────────────────────────
        JPanel logSidePanel = new JPanel(new GridBagLayout());
        GridBagConstraints sGbc = new GridBagConstraints();
        sGbc.gridx = 0; sGbc.gridy = 0; sGbc.anchor = GridBagConstraints.NORTHWEST; sGbc.insets = new Insets(0,0,8,0);
        logSidePanel.add(new JLabel("Output Log"), sGbc);
        JButton btnClearLog = new JButton("Clear");
        sGbc.gridy = 1; sGbc.insets = new Insets(4,0,0,0);
        logSidePanel.add(btnClearLog, sGbc);

        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.0; gbc.fill = GridBagConstraints.NONE;
        gbc.gridwidth = 1; gbc.anchor = GridBagConstraints.NORTHWEST;
        mainPanel.add(logSidePanel, gbc);

        txtLog = new JTextArea();
        txtLog.setEditable(false);
        txtLog.setFont(new Font("Monospaced", Font.PLAIN, 11));
        JScrollPane scrollPane = new JScrollPane(txtLog);
        gbc.gridx = 1; gbc.gridy = row; gbc.weightx = 1.0; gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH; gbc.gridwidth = 4;
        mainPanel.add(scrollPane, gbc);

        // ── Wire events ───────────────────────────────────────────────────

        // Load Config
        btnLoadConfig.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new FileNameExtensionFilter("JBT Config (*.jbt)", "jbt"));
            if (fc.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                File chosen = fc.getSelectedFile();
                JBTConfig loaded = JBTConfig.loadFromFile(chosen.getAbsolutePath());
                if (loaded == null) {
                    JOptionPane.showMessageDialog(frame,
                        "Failed to parse config: " + chosen.getAbsolutePath(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                loadedConfigPath = loaded.configFilePath;
                applyConfigToForm(loaded, txtProject, txtSrc, txtOutput,
                    comboBuildMode, libModel, chkAddResources, chkClose,
                    chkDebug, chkHasIcon, txtIconPath, btnBrowseIcon);
                lblConfigStatus.setText("Config: " + chosen.getName());
                lblConfigStatus.setForeground(new Color(0, 128, 0));
                Log("Loaded config: " + chosen.getAbsolutePath() + "\n");
            }
        });

        // Browse project
        btnBrowseProject.addActionListener(e -> {
            FileManager fm = new FileManager();
            File dir = fm.openFileChooser(JFileChooser.DIRECTORIES_ONLY);
            if (dir != null) txtProject.setText(dir.getAbsolutePath());
        });

        // Browse lib
        btnBrowseLib.addActionListener(e -> {
            if (selectedLibFile != null) { selectedLibFile = null; btnAddLibrary.setText("ADD"); }
            JFileChooser fc = new JFileChooser();
            fc.setMultiSelectionEnabled(true);
            fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
            if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                File[] sel = fc.getSelectedFiles();
                if (sel.length == 1) {
                    selectedLibFile = sel[0];
                    btnAddLibrary.setText(sel[0].getName());
                } else {
                    for (File f : sel) libModel.addElement(f.getAbsolutePath());
                    btnAddLibrary.setText("ADD");
                }
            }
        });

        btnAddLibrary.addActionListener(e -> {
            if (selectedLibFile != null) {
                libModel.addElement(selectedLibFile.getAbsolutePath());
                selectedLibFile = null;
                btnAddLibrary.setText("ADD");
            } else {
                JOptionPane.showMessageDialog(frame, "Please browse a library file first!", "Warning", JOptionPane.WARNING_MESSAGE);
            }
        });

        btnDeleteSelected.addActionListener(e -> {
            String sel = (String) comboLibrary.getSelectedItem();
            if (sel != null) libModel.removeElement(sel);
        });

        // Icon checkbox toggle
        chkHasIcon.addActionListener(e -> {
            boolean on = chkHasIcon.isSelected();
            txtIconPath.setEnabled(on);
            btnBrowseIcon.setEnabled(on);
            if (!on) txtIconPath.setText("(no icon)");
        });

        btnBrowseIcon.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new FileNameExtensionFilter("Image files", "png", "ico", "jpg", "gif"));
            if (fc.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                txtIconPath.setText(fc.getSelectedFile().getAbsolutePath());
            }
        });

        // Save Config
        btnSaveConfig.addActionListener(e -> {
            if (loadedConfigPath == null) {
                // Ask where to save
                JFileChooser fc = new JFileChooser();
                fc.setFileFilter(new FileNameExtensionFilter("JBT Config (*.jbt)", "jbt"));
                fc.setSelectedFile(new File("build.jbt"));
                if (fc.showSaveDialog(frame) != JFileChooser.APPROVE_OPTION) return;
                String path = fc.getSelectedFile().getAbsolutePath();
                if (!path.toLowerCase().endsWith(".jbt")) path += ".jbt";
                loadedConfigPath = path;
                lblConfigStatus.setText("Config: " + new File(path).getName());
                lblConfigStatus.setForeground(new Color(0, 128, 0));
            }
            saveConfig(frame, loadedConfigPath, txtProject, txtSrc, txtOutput,
                comboBuildMode, libModel, chkAddResources, chkClose,
                chkDebug, chkHasIcon, txtIconPath);
        });

        btnClearLog.addActionListener(e -> txtLog.setText(""));

        // Build
        btnBuild.addActionListener(e -> {
            btnBuild.setEnabled(false);
            btnBuild.setText("Building...");

            JBTConfig runCfg = collectConfig(txtProject, txtSrc, txtOutput,
                comboBuildMode, libModel, chkAddResources, chkClose,
                chkDebug, chkHasIcon, txtIconPath);
            runCfg.configFilePath = loadedConfigPath;

            boolean debugMode = chkDebug.isSelected();
            Logger logger = new Logger(debugMode, msg -> Log(msg));

            Log("Starting Build process...\n");
            Log("Attached Libraries: " + runCfg.library + "\n");

            new Thread(() -> {
                try {
                    Build builder = new Build();
                    builder.StartBuild(runCfg, logger);
                    Log("Build Finished Successfully!\n\n");
                } catch (Exception ex) {
                    Log("Build Failed: " + ex.getMessage() + "\n\n");
                } finally {
                    SwingUtilities.invokeLater(() -> {
                        btnBuild.setEnabled(true);
                        btnBuild.setText("Start Build");

                        if (chkClose.isSelected()) {
                            System.exit(0);
                        }
                    });
                }
            }).start();
        });

        frame.add(mainPanel);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // Auto-build if cfg flag is set
        if (cfg.autoBuild) {
            Log("[AutoBuild Activated] Triggering compiler engine...\n");
            btnBuild.doClick();
        }
    }

    // ── Helper: apply a loaded config to all form controls ───────────────────

    private void applyConfigToForm(JBTConfig cfg,
            JTextField txtProject, JTextField txtSrc, JTextField txtOutput,
            JComboBox<String> comboBuildMode, DefaultComboBoxModel<String> libModel,
            JCheckBox chkAddResources, JCheckBox chkClose, JCheckBox chkDebug,
            JCheckBox chkHasIcon, JTextField txtIconPath, JButton btnBrowseIcon) {

        txtProject.setText(cfg.projectFolder);
        txtSrc.setText(cfg.srcFolder);
        txtOutput.setText(cfg.output);
        comboBuildMode.setSelectedItem(cfg.buildMode);

        libModel.removeAllElements();
        for (String lib : cfg.library) libModel.addElement(lib);

        chkAddResources.setSelected(cfg.addResources);
        chkClose.setSelected(cfg.closeWhenBuildFinish);
        chkDebug.setSelected(cfg.debug);

        chkHasIcon.setSelected(cfg.hasIcon);
        txtIconPath.setEnabled(cfg.hasIcon);
        btnBrowseIcon.setEnabled(cfg.hasIcon);
        txtIconPath.setText(cfg.hasIcon && !cfg.iconPath.isEmpty() ? cfg.iconPath : "(no icon)");
    }

    // ── Helper: read form → JBTConfig ────────────────────────────────────────

    private JBTConfig collectConfig(JTextField txtProject, JTextField txtSrc,
            JTextField txtOutput, JComboBox<String> comboBuildMode,
            DefaultComboBoxModel<String> libModel,
            JCheckBox chkAddResources, JCheckBox chkClose, JCheckBox chkDebug,
            JCheckBox chkHasIcon, JTextField txtIconPath) {

        JBTConfig cfg = new JBTConfig();
        cfg.projectFolder        = txtProject.getText().trim();
        cfg.srcFolder            = txtSrc.getText().trim();
        cfg.output               = txtOutput.getText().trim();
        cfg.buildMode            = comboBuildMode.getSelectedItem().toString();
        cfg.addResources         = chkAddResources.isSelected();
        cfg.closeWhenBuildFinish = chkClose.isSelected();
        cfg.debug                = chkDebug.isSelected();
        cfg.hasIcon              = chkHasIcon.isSelected();
        String ip = txtIconPath.getText().trim();
        cfg.iconPath = (chkHasIcon.isSelected() && !ip.equals("(no icon)")) ? ip : "";

        cfg.library = new ArrayList<>();
        for (int i = 0; i < libModel.getSize(); i++) cfg.library.add(libModel.getElementAt(i));

        return cfg;
    }

    // ── Helper: save config to file ──────────────────────────────────────────

    private void saveConfig(JFrame frame, String path,
            JTextField txtProject, JTextField txtSrc, JTextField txtOutput,
            JComboBox<String> comboBuildMode, DefaultComboBoxModel<String> libModel,
            JCheckBox chkAddResources, JCheckBox chkClose, JCheckBox chkDebug,
            JCheckBox chkHasIcon, JTextField txtIconPath) {
        try {
            JBTConfig cfg = collectConfig(txtProject, txtSrc, txtOutput,
                comboBuildMode, libModel, chkAddResources, chkClose,
                chkDebug, chkHasIcon, txtIconPath);
            cfg.configFilePath = path;

            // Preserve AutoBuild / sys from existing file if present
            if (path != null && new File(path).exists()) {
                String oldJson = new String(Files.readAllBytes(Paths.get(path)));
                JBTConfig old = new JBTConfig();
                old.parse(oldJson);
                cfg.autoBuild = old.autoBuild;
                cfg.sys       = old.sys;
            }

            Files.write(Paths.get(path), cfg.toJson().getBytes());
            JOptionPane.showMessageDialog(frame,
                "Config saved to " + path, "Saved", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(frame,
                "Error saving config: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── GridBagLayout helpers ─────────────────────────────────────────────────

    private void addLabel(JPanel p, GridBagConstraints gbc, int row, String text) {
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0; gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        p.add(new JLabel(text), gbc);
    }

    private void addField(JPanel p, GridBagConstraints gbc, int row, int col, int span, JComponent c) {
        gbc.gridx = col; gbc.gridy = row; gbc.weightx = 1.0; gbc.gridwidth = span;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        p.add(c, gbc);
    }

    private void addWidget(JPanel p, GridBagConstraints gbc, int row, int col, int span, JComponent c) {
        gbc.gridx = col; gbc.gridy = row; gbc.weightx = 0; gbc.gridwidth = span;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        p.add(c, gbc);
    }
}
