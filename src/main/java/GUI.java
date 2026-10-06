import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

public class GUI {

    // ── State ────────────────────────────────────────────────────────────────
    private File   selectedLibFile  = null;
    private String loadedConfigPath = null;

    // ── Log textarea ─────────────────────────────────────────────────────────
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
        frame.setSize(960, 640);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}

        JPanel main = new JPanel(new GridBagLayout());
        main.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        int row = 0;

        // ── Row 0: Project Folder ────────────────────────────────────────
        addLabel(main, gbc, row, "Project Folder");
        JTextField txtProject = new JTextField(cfg.projectFolder);
        addField(main, gbc, row, 1, 2, txtProject);
        JButton btnBrowseProject = new JButton("Browse");
        addWidget(main, gbc, row, 3, 1, btnBrowseProject);
        row++;

        // ── Row 1: SourceSet | PackageFolder | MainClass ─────────────────
        addLabel(main, gbc, row, "Source Set");
        JComboBox<String> comboSourceSet = new JComboBox<>(new String[]{"main", "test"});
        comboSourceSet.setSelectedItem(cfg.sourceSet);
        addWidget(main, gbc, row, 1, 1, comboSourceSet);

        gbc.gridx = 2; gbc.gridy = row; gbc.weightx = 0; gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        main.add(new JLabel("Package Folder"), gbc);

        JTextField txtPackageFolder = new JTextField(cfg.packageFolder);
        gbc.gridx = 3; gbc.gridy = row; gbc.weightx = 0.6; gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        main.add(txtPackageFolder, gbc);

        gbc.gridx = 4; gbc.gridy = row; gbc.weightx = 0; gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        main.add(new JLabel("Main Class"), gbc);

        JTextField txtMainClass = new JTextField(cfg.mainClass);
        gbc.gridx = 5; gbc.gridy = row; gbc.weightx = 0.4; gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        main.add(txtMainClass, gbc);
        row++;

        // ── Row 2: Output Folder ─────────────────────────────────────────
        addLabel(main, gbc, row, "Output Folder");
        JTextField txtOutput = new JTextField(cfg.output);
        addField(main, gbc, row, 1, 3, txtOutput);
        row++;

        // ── Row 3: Library ──────────────────────────────────────────────
        addLabel(main, gbc, row, "Library");
        DefaultComboBoxModel<String> libModel = new DefaultComboBoxModel<>();
        for (String lib : cfg.library) libModel.addElement(lib);
        JComboBox<String> comboLibrary = new JComboBox<>(libModel);
        addField(main, gbc, row, 1, 1, comboLibrary);
        JButton btnDeleteLib = new JButton("Delete");
        addWidget(main, gbc, row, 2, 1, btnDeleteLib);
        JButton btnAddLib = new JButton("ADD");
        addWidget(main, gbc, row, 3, 1, btnAddLib);
        JButton btnBrowseLib = new JButton("Browse library");
        addWidget(main, gbc, row, 4, 1, btnBrowseLib);
        row++;

        // ── Row 4: Build Mode | CloseWhenBuildFinish | DebugBuild ───────
        addLabel(main, gbc, row, "Build Mode");
        JComboBox<String> comboBuildMode = new JComboBox<>(new String[]{"Normal", "Onefile"});
        comboBuildMode.setSelectedItem(cfg.buildMode);
        addWidget(main, gbc, row, 1, 1, comboBuildMode);
        JCheckBox chkClose = new JCheckBox("Close When Finish", cfg.closeWhenBuildFinish);
        addWidget(main, gbc, row, 2, 1, chkClose);
        JCheckBox chkDebug = new JCheckBox("Debug Build", cfg.debugBuild);
        addWidget(main, gbc, row, 3, 1, chkDebug);
        row++;

        // ── Row 5: Resources ─────────────────────────────────────────────
        addLabel(main, gbc, row, "Resources");
        JCheckBox chkAddResources = new JCheckBox("Add Resources", cfg.addResources);
        addWidget(main, gbc, row, 1, 1, chkAddResources);

        JTextField txtResourcesPath = new JTextField(
            cfg.resourcesPath.isEmpty() ? "" : cfg.resourcesPath);
        txtResourcesPath.setEnabled(cfg.addResources);
        txtResourcesPath.setToolTipText("Leave empty to use default: src/{SourceSet}/resources/{PackageFolder}");
        addField(main, gbc, row, 2, 1, txtResourcesPath);

        JButton btnBrowseResources = new JButton("Browse");
        btnBrowseResources.setEnabled(cfg.addResources);
        addWidget(main, gbc, row, 3, 1, btnBrowseResources);
        row++;

        // ── Row 6: Icon ──────────────────────────────────────────────────
        addLabel(main, gbc, row, "Icon");
        JCheckBox chkAddIcon = new JCheckBox("Add Icon", cfg.addIcon);
        addWidget(main, gbc, row, 1, 1, chkAddIcon);

        JTextField txtIconPath = new JTextField(
            cfg.iconPath.isEmpty() ? "" : cfg.iconPath);
        txtIconPath.setEnabled(cfg.addIcon);
        txtIconPath.setToolTipText("Leave empty to use default: src/{SourceSet}/resources/{PackageFolder}/icon.ico");
        addField(main, gbc, row, 2, 1, txtIconPath);

        JButton btnBrowseIcon = new JButton("Browse");
        btnBrowseIcon.setEnabled(cfg.addIcon);
        addWidget(main, gbc, row, 3, 1, btnBrowseIcon);
        row++;

        // ── Row 7: Load / Save / Build ───────────────────────────────────
        JButton btnLoadConfig = new JButton("Load Config (.jbt)");
        addWidget(main, gbc, row, 0, 1, btnLoadConfig);

        JLabel lblConfigStatus = new JLabel(
            loadedConfigPath != null ? new File(loadedConfigPath).getName() : "No config loaded");
        lblConfigStatus.setForeground(loadedConfigPath != null ? new Color(0, 128, 0) : Color.GRAY);
        addField(main, gbc, row, 1, 2, lblConfigStatus);

        JButton btnSaveConfig = new JButton("Save Config");
        addWidget(main, gbc, row, 3, 1, btnSaveConfig);
        JButton btnBuild = new JButton("Start Build");
        btnBuild.setFont(new Font("SansSerif", Font.BOLD, 12));
        addWidget(main, gbc, row, 4, 1, btnBuild);
        row++;

        // ── Row 8: Log area ───────────────────────────────────────────────
        JPanel logSide = new JPanel(new GridBagLayout());
        GridBagConstraints sg = new GridBagConstraints();
        sg.gridx = 0; sg.gridy = 0; sg.anchor = GridBagConstraints.NORTHWEST; sg.insets = new Insets(0,0,8,0);
        logSide.add(new JLabel("Output Log"), sg);
        JButton btnClearLog = new JButton("Clear");
        sg.gridy = 1; sg.insets = new Insets(4,0,0,0);
        logSide.add(btnClearLog, sg);

        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE;
        gbc.gridwidth = 1; gbc.anchor = GridBagConstraints.NORTHWEST;
        main.add(logSide, gbc);

        txtLog = new JTextArea();
        txtLog.setEditable(false);
        txtLog.setFont(new Font("Monospaced", Font.PLAIN, 11));
        JScrollPane scroll = new JScrollPane(txtLog);
        gbc.gridx = 1; gbc.gridy = row; gbc.weightx = 1.0; gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH; gbc.gridwidth = 4;
        main.add(scroll, gbc);

        // ── Events ────────────────────────────────────────────────────────

        // Load Config
        btnLoadConfig.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new FileNameExtensionFilter("JBT Config (*.jbt)", "jbt"));
            if (fc.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                JBTConfig loaded = JBTConfig.loadFromFile(fc.getSelectedFile().getAbsolutePath());
                if (loaded == null) {
                    JOptionPane.showMessageDialog(frame,
                        "Failed to parse: " + fc.getSelectedFile().getAbsolutePath(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                loadedConfigPath = loaded.configFilePath;
                applyToForm(loaded, txtProject, comboSourceSet, txtPackageFolder,
                    txtMainClass, txtOutput, comboBuildMode, libModel,
                    chkAddResources, txtResourcesPath, btnBrowseResources,
                    chkClose, chkDebug,
                    chkAddIcon, txtIconPath, btnBrowseIcon);
                lblConfigStatus.setText(fc.getSelectedFile().getName());
                lblConfigStatus.setForeground(new Color(0, 128, 0));
                Log("Loaded config: " + loaded.configFilePath + "\n");
            }
        });

        btnBrowseProject.addActionListener(e -> {
            File dir = new FileManager().openFileChooser(JFileChooser.DIRECTORIES_ONLY);
            if (dir != null) txtProject.setText(dir.getAbsolutePath());
        });

        btnBrowseLib.addActionListener(e -> {
            if (selectedLibFile != null) { selectedLibFile = null; btnAddLib.setText("ADD"); }
            JFileChooser fc = new JFileChooser();
            fc.setMultiSelectionEnabled(true);
            fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
            if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                File[] sel = fc.getSelectedFiles();
                if (sel.length == 1) { selectedLibFile = sel[0]; btnAddLib.setText(sel[0].getName()); }
                else { for (File f : sel) libModel.addElement(f.getAbsolutePath()); }
            }
        });

        btnAddLib.addActionListener(e -> {
            if (selectedLibFile != null) {
                libModel.addElement(selectedLibFile.getAbsolutePath());
                selectedLibFile = null; btnAddLib.setText("ADD");
            } else {
                JOptionPane.showMessageDialog(frame, "Browse a library file first!", "Warning", JOptionPane.WARNING_MESSAGE);
            }
        });

        btnDeleteLib.addActionListener(e -> {
            String sel = (String) comboLibrary.getSelectedItem();
            if (sel != null) libModel.removeElement(sel);
        });

        // Resources toggle
        chkAddResources.addActionListener(e -> {
            txtResourcesPath.setEnabled(chkAddResources.isSelected());
            btnBrowseResources.setEnabled(chkAddResources.isSelected());
        });
        btnBrowseResources.addActionListener(e -> {
            File dir = new FileManager().openFileChooser(JFileChooser.DIRECTORIES_ONLY);
            if (dir != null) txtResourcesPath.setText(dir.getAbsolutePath());
        });

        // Icon toggle
        chkAddIcon.addActionListener(e -> {
            txtIconPath.setEnabled(chkAddIcon.isSelected());
            btnBrowseIcon.setEnabled(chkAddIcon.isSelected());
        });
        btnBrowseIcon.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new FileNameExtensionFilter("Icon files", "ico", "png", "jpg", "gif"));
            if (fc.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION)
                txtIconPath.setText(fc.getSelectedFile().getAbsolutePath());
        });

        // Save Config
        btnSaveConfig.addActionListener(e -> {
            if (loadedConfigPath == null) {
                JFileChooser fc = new JFileChooser();
                fc.setFileFilter(new FileNameExtensionFilter("JBT Config (*.jbt)", "jbt"));
                fc.setSelectedFile(new File("build.jbt"));
                if (fc.showSaveDialog(frame) != JFileChooser.APPROVE_OPTION) return;
                String p = fc.getSelectedFile().getAbsolutePath();
                if (!p.toLowerCase().endsWith(".jbt")) p += ".jbt";
                loadedConfigPath = p;
                lblConfigStatus.setText(new File(p).getName());
                lblConfigStatus.setForeground(new Color(0, 128, 0));
            }
            saveConfig(frame, loadedConfigPath,
                txtProject, comboSourceSet, txtPackageFolder, txtMainClass, txtOutput,
                comboBuildMode, libModel,
                chkAddResources, txtResourcesPath,
                chkClose, chkDebug,
                chkAddIcon, txtIconPath);
        });

        btnClearLog.addActionListener(e -> txtLog.setText(""));

        // Start Build
        btnBuild.addActionListener(e -> {
            btnBuild.setEnabled(false);
            btnBuild.setText("Building...");

            JBTConfig runCfg = collectConfig(
                txtProject, comboSourceSet, txtPackageFolder, txtMainClass, txtOutput,
                comboBuildMode, libModel,
                chkAddResources, txtResourcesPath,
                chkClose, chkDebug,
                chkAddIcon, txtIconPath);
            runCfg.configFilePath = loadedConfigPath;

            Logger logger = new Logger(runCfg.debugBuild, msg -> Log(msg));
            Log("Starting build...\n");

            new Thread(() -> {
                try {
                    new Build().StartBuild(runCfg, logger);
                    Log("Build Finished!\n\n");
                } catch (Exception ex) {
                    Log("Build Failed: " + ex.getMessage() + "\n\n");
                } finally {
                    SwingUtilities.invokeLater(() -> {
                        btnBuild.setEnabled(true);
                        btnBuild.setText("Start Build");
                        if (chkClose.isSelected()) System.exit(0);
                    });
                }
            }).start();
        });

        frame.add(main);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    // ── Form helpers ─────────────────────────────────────────────────────────

    private void applyToForm(JBTConfig cfg,
            JTextField txtProject, JComboBox<String> comboSourceSet,
            JTextField txtPackageFolder, JTextField txtMainClass,
            JTextField txtOutput,
            JComboBox<String> comboBuildMode, DefaultComboBoxModel<String> libModel,
            JCheckBox chkAddResources, JTextField txtResourcesPath, JButton btnBrowseRes,
            JCheckBox chkClose, JCheckBox chkDebug,
            JCheckBox chkAddIcon, JTextField txtIconPath, JButton btnBrowseIcon) {

        txtProject.setText(cfg.projectFolder);
        comboSourceSet.setSelectedItem(cfg.sourceSet);
        txtPackageFolder.setText(cfg.packageFolder);
        txtMainClass.setText(cfg.mainClass);
        txtOutput.setText(cfg.output);
        comboBuildMode.setSelectedItem(cfg.buildMode);

        libModel.removeAllElements();
        for (String lib : cfg.library) libModel.addElement(lib);

        chkAddResources.setSelected(cfg.addResources);
        txtResourcesPath.setText(cfg.resourcesPath);
        txtResourcesPath.setEnabled(cfg.addResources);
        btnBrowseRes.setEnabled(cfg.addResources);

        chkClose.setSelected(cfg.closeWhenBuildFinish);
        chkDebug.setSelected(cfg.debugBuild);

        chkAddIcon.setSelected(cfg.addIcon);
        txtIconPath.setText(cfg.iconPath);
        txtIconPath.setEnabled(cfg.addIcon);
        btnBrowseIcon.setEnabled(cfg.addIcon);
    }

    private JBTConfig collectConfig(
            JTextField txtProject, JComboBox<String> comboSourceSet,
            JTextField txtPackageFolder, JTextField txtMainClass,
            JTextField txtOutput,
            JComboBox<String> comboBuildMode, DefaultComboBoxModel<String> libModel,
            JCheckBox chkAddResources, JTextField txtResourcesPath,
            JCheckBox chkClose, JCheckBox chkDebug,
            JCheckBox chkAddIcon, JTextField txtIconPath) {

        JBTConfig cfg = new JBTConfig();
        cfg.projectFolder        = txtProject.getText().trim();
        cfg.sourceSet            = comboSourceSet.getSelectedItem().toString();
        cfg.packageFolder        = txtPackageFolder.getText().trim();
        cfg.mainClass            = txtMainClass.getText().trim().isEmpty() ? "Main" : txtMainClass.getText().trim();
        cfg.output               = txtOutput.getText().trim();
        cfg.buildMode            = comboBuildMode.getSelectedItem().toString();
        cfg.addResources         = chkAddResources.isSelected();
        cfg.resourcesPath        = txtResourcesPath.getText().trim();
        cfg.closeWhenBuildFinish = chkClose.isSelected();
        cfg.debugBuild           = chkDebug.isSelected();
        cfg.addIcon              = chkAddIcon.isSelected();
        cfg.iconPath             = txtIconPath.getText().trim();

        cfg.library = new ArrayList<>();
        for (int i = 0; i < libModel.getSize(); i++) cfg.library.add(libModel.getElementAt(i));
        return cfg;
    }

    private void saveConfig(JFrame frame, String path,
            JTextField txtProject, JComboBox<String> comboSourceSet,
            JTextField txtPackageFolder, JTextField txtMainClass,
            JTextField txtOutput,
            JComboBox<String> comboBuildMode, DefaultComboBoxModel<String> libModel,
            JCheckBox chkAddResources, JTextField txtResourcesPath,
            JCheckBox chkClose, JCheckBox chkDebug,
            JCheckBox chkAddIcon, JTextField txtIconPath) {
        try {
            JBTConfig cfg = collectConfig(txtProject, comboSourceSet, txtPackageFolder,
                txtMainClass, txtOutput, comboBuildMode, libModel,
                chkAddResources, txtResourcesPath,
                chkClose, chkDebug, chkAddIcon, txtIconPath);
            cfg.configFilePath = path;
            Files.write(Paths.get(path), cfg.toJson().getBytes());
            JOptionPane.showMessageDialog(frame,
                "Config saved to " + path, "Saved", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(frame,
                "Error saving: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── GridBagLayout shortcuts ───────────────────────────────────────────────

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
