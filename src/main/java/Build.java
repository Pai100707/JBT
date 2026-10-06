import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class Build {

    /**
     * Main build entry point — uses JBTConfig + Logger.
     *
     * Steps:
     *   1. Compile Java sources from resolvedSrcFolder()
     *   2. Copy resources (if AddResources)
     *   3. Copy icon into JAR + copy to output folder for jpackage (if AddIcon)
     *   4. Handle libraries (Onefile = extract / Normal = copy to lib/)
     *   5. Package everything into a runnable JAR
     */
    public void StartBuild(JBTConfig cfg, Logger logger) {
        try {
            FileManager fm = new FileManager();

            Path projectFolder     = Paths.get(cfg.projectFolder);
            String resolvedSrc     = cfg.resolvedSrcFolder();
            Path srcFolder         = projectFolder.resolve(resolvedSrc);
            Path outputFolder      = projectFolder.resolve(cfg.output);
            Path tempClassesFolder = projectFolder.resolve("temp_classes");

            logger.log("Preparing directory structures...");
            fm.createFolder(outputFolder, false);
            fm.createFolder(tempClassesFolder, true);

            // ── 1. Compile ───────────────────────────────────────────────
            logger.log("Scanning for Java source files in: " + srcFolder);
            List<String> javaFiles = new ArrayList<>();
            findJavaFiles(srcFolder.toFile(), javaFiles);

            if (javaFiles.isEmpty()) {
                logger.warn("No .java files found in: " + resolvedSrc);
                fm.deleteFolder(tempClassesFolder);
                return;
            }
            logger.log("Found " + javaFiles.size() + " file(s) to compile.");
            if (cfg.debugBuild) javaFiles.forEach(f -> logger.log("  Source: " + f));

            // Build classpath
            StringBuilder classpath = new StringBuilder(".");
            String sep = System.getProperty("path.separator");
            for (String lib : cfg.library) {
                classpath.append(sep).append(projectFolder.resolve(lib).toAbsolutePath());
            }
            if (cfg.debugBuild) logger.log("Classpath: " + classpath);

            logger.log("Executing javac...");
            List<String> javacCmd = new ArrayList<>();
            javacCmd.add("javac");
            javacCmd.add("-cp"); javacCmd.add(classpath.toString());
            javacCmd.add("-d");  javacCmd.add(tempClassesFolder.toString());
            javacCmd.addAll(javaFiles);

            if (!runProcess(javacCmd, projectFolder.toFile(), logger)) {
                logger.error("Compilation failed! Please check your code.");
                fm.deleteFolder(tempClassesFolder);
                return;
            }
            logger.log("Compilation successful.");

            // ── 2. Resources ─────────────────────────────────────────────
            if (cfg.addResources) {
                Path resourcesFolder = projectFolder.resolve(cfg.resolvedResourcesPath());
                if (Files.exists(resourcesFolder)) {
                    logger.log("Adding resources from: " + resourcesFolder);
                    copyDirectory(resourcesFolder, tempClassesFolder, logger, cfg.debugBuild);
                } else {
                    logger.warn("AddResources=true but path not found: " + resourcesFolder);
                }
            }

            // ── 3. Icon ──────────────────────────────────────────────────
            Path resolvedIconFile = null;
            if (cfg.addIcon) {
                Path iconSrc = projectFolder.resolve(cfg.resolvedIconPath());
                if (!Paths.get(cfg.resolvedIconPath()).isAbsolute())
                    iconSrc = projectFolder.resolve(cfg.resolvedIconPath());
                else
                    iconSrc = Paths.get(cfg.resolvedIconPath());

                if (Files.exists(iconSrc)) {
                    // Pack icon into JAR root (accessible as classpath resource)
                    Path iconInJar = tempClassesFolder.resolve(iconSrc.getFileName());
                    Files.copy(iconSrc, iconInJar, StandardCopyOption.REPLACE_EXISTING);
                    logger.log("Icon packed into JAR: " + iconSrc.getFileName());

                    // Also copy icon next to the output JAR for jpackage --icon
                    Path iconForJpackage = outputFolder.resolve(iconSrc.getFileName());
                    Files.copy(iconSrc, iconForJpackage, StandardCopyOption.REPLACE_EXISTING);
                    resolvedIconFile = iconForJpackage;
                    logger.log("Icon copied to output for jpackage: " + iconForJpackage);
                } else {
                    logger.warn("Icon file not found: " + iconSrc);
                }
            }

            // ── 4. Libraries ─────────────────────────────────────────────
            if ("Onefile".equalsIgnoreCase(cfg.buildMode)) {
                logger.log("Extracting libraries for Onefile build...");
                for (String lib : cfg.library) {
                    Path libPath = projectFolder.resolve(lib);
                    if (Files.exists(libPath)) {
                        logger.log("  + Extracting: " + libPath.getFileName());
                        extractJar(libPath, tempClassesFolder, logger, cfg.debugBuild);
                    } else {
                        logger.warn("  ! Library not found: " + libPath);
                    }
                }
            } else {
                logger.log("Setting up Normal build lib folder...");
                Path libOut = outputFolder.resolve("lib");
                fm.createFolder(libOut, false);
                for (String lib : cfg.library) {
                    Path libPath = projectFolder.resolve(lib);
                    if (Files.exists(libPath)) {
                        logger.log("  + Copying: " + libPath.getFileName());
                        Files.copy(libPath, libOut.resolve(libPath.getFileName()), StandardCopyOption.REPLACE_EXISTING);
                    } else {
                        logger.warn("  ! Library not found: " + libPath);
                    }
                }
            }

            // ── 5. Package JAR ───────────────────────────────────────────
            String jarName    = projectFolder.getFileName().toString() + ".jar";
            Path   finalJar   = outputFolder.resolve(jarName);
            logger.log("Packaging JAR: " + jarName + "...");

            try (java.util.zip.ZipOutputStream zos =
                    new java.util.zip.ZipOutputStream(Files.newOutputStream(finalJar))) {

                // Manifest
                zos.putNextEntry(new java.util.zip.ZipEntry("META-INF/MANIFEST.MF"));
                String mainClassName = cfg.resolvedMainClass();
                zos.write(("Manifest-Version: 1.0\r\nMain-Class: " + mainClassName + "\r\n\r\n")
                        .getBytes(java.nio.charset.StandardCharsets.UTF_8));
                zos.closeEntry();
                if (cfg.debugBuild) logger.log("MANIFEST Main-Class: " + mainClassName);

                packDirectory(tempClassesFolder, tempClassesFolder, zos, logger, cfg.debugBuild);

                logger.log("\nSUCCESSFUL BUILD!");
                logger.log("Output: " + finalJar.toAbsolutePath());
                if (resolvedIconFile != null) {
                    logger.log("Icon (for jpackage --icon): " + resolvedIconFile.toAbsolutePath());
                }
            } catch (Exception jarEx) {
                logger.error("JAR packaging failed: " + jarEx.getMessage());
            }

            logger.log("Cleaning temp_classes...");
            fm.deleteFolder(tempClassesFolder);

        } catch (Exception e) {
            logger.error("Unexpected build error: " + e.getMessage());
        }
    }

    // ── Legacy overload (GUI backwards compat) ────────────────────────────────

    /** @deprecated Use {@link #StartBuild(JBTConfig, Logger)} instead. */
    @Deprecated
    public void StartBuild(String projectFolderStr, String srcFolderStr, String outputFolderStr,
                           List<String> libraries, String buildMode, GUI gui) {
        JBTConfig cfg = new JBTConfig();
        cfg.projectFolder = projectFolderStr;
        // srcFolderStr is a full relative path like "src/main/java" — map to sourceSet+packageFolder
        cfg.sourceSet     = "main";
        cfg.packageFolder = "";
        cfg.output        = outputFolderStr;
        cfg.library       = new ArrayList<>(libraries);
        cfg.buildMode     = buildMode;
        StartBuild(cfg, new Logger(false, msg -> gui.Log(msg)));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void findJavaFiles(File folder, List<String> out) {
        File[] files = folder.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) findJavaFiles(f, out);
            else if (f.getName().endsWith(".java")) out.add(f.getAbsolutePath());
        }
    }

    private boolean runProcess(List<String> cmd, File workingDir, Logger logger) {
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(workingDir);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) logger.log("[javac] " + line);
            }
            return p.waitFor() == 0;
        } catch (Exception e) {
            logger.error("Process failed: " + e.getMessage());
            return false;
        }
    }

    private void copyDirectory(Path src, Path destRoot, Logger logger, boolean debug) throws Exception {
        Files.walk(src).forEach(path -> {
            try {
                if (Files.isDirectory(path)) return;
                Path rel  = src.relativize(path);
                Path dest = destRoot.resolve(rel);
                Files.createDirectories(dest.getParent());
                Files.copy(path, dest, StandardCopyOption.REPLACE_EXISTING);
                if (debug) logger.log("  Resource: " + rel);
            } catch (IOException e) {
                logger.warn("Could not copy resource: " + path + " — " + e.getMessage());
            }
        });
    }

    private void extractJar(Path jar, Path dest, Logger logger, boolean debug) {
        try (java.util.zip.ZipInputStream zis =
                new java.util.zip.ZipInputStream(Files.newInputStream(jar))) {
            java.util.zip.ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                String n = entry.getName(), u = n.toUpperCase();
                if (u.equalsIgnoreCase("META-INF/MANIFEST.MF") || u.equalsIgnoreCase("module-info.class")) continue;
                if (u.startsWith("META-INF/") && (u.endsWith(".SF") || u.endsWith(".DSA") || u.endsWith(".RSA"))) continue;
                Path t = dest.resolve(n);
                Files.createDirectories(t.getParent());
                Files.copy(zis, t, StandardCopyOption.REPLACE_EXISTING);
                if (debug) logger.log("    Extracted: " + n);
            }
        } catch (IOException e) {
            logger.error("Extract failed for " + jar.getFileName() + ": " + e.getMessage());
        }
    }

    private void packDirectory(Path base, Path current,
                               java.util.zip.ZipOutputStream zos,
                               Logger logger, boolean debug) throws Exception {
        File[] files = current.toFile().listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) { packDirectory(base, f.toPath(), zos, logger, debug); continue; }
            String entry = base.relativize(f.toPath()).toString().replace("\\", "/");
            if (debug) logger.log("  Packing: " + entry);
            zos.putNextEntry(new java.util.zip.ZipEntry(entry));
            Files.copy(f.toPath(), zos);
            zos.closeEntry();
        }
    }
}