import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class Build {

    /**
     * Main build entry point.  All configuration comes from JBTConfig.
     */
    public void StartBuild(JBTConfig cfg, Logger logger) {
        try {
            FileManager fileManager = new FileManager();

            Path projectFolder    = Paths.get(cfg.projectFolder);
            String resolvedSrc    = cfg.resolvedSrcFolder();
            Path srcFolder        = projectFolder.resolve(resolvedSrc);
            Path outputFolder     = projectFolder.resolve(cfg.output);
            Path tempClassesFolder = projectFolder.resolve("temp_classes");

            logger.log("Preparing directory structures...");
            fileManager.createFolder(outputFolder, false);
            fileManager.createFolder(tempClassesFolder, true);

            // ── Collect Java source files ────────────────────────────────
            logger.log("Scanning for Java source files in: " + srcFolder);
            List<String> javaFiles = new ArrayList<>();
            findJavaFiles(srcFolder.toFile(), javaFiles);

            if (javaFiles.isEmpty()) {
                logger.warn("No .java files found in " + resolvedSrc);
                fileManager.deleteFolder(tempClassesFolder);
                return;
            }
            logger.log("Found " + javaFiles.size() + " file(s) to compile.");
            if (cfg.debug) {
                for (String f : javaFiles) logger.log("  Source: " + f);
            }

            // ── Build classpath ──────────────────────────────────────────
            StringBuilder classpath = new StringBuilder(".");
            String pathSeparator = System.getProperty("path.separator");
            for (String lib : cfg.library) {
                Path libPath = projectFolder.resolve(lib);
                classpath.append(pathSeparator).append(libPath.toAbsolutePath());
            }
            if (cfg.debug) logger.log("Classpath: " + classpath);

            // ── Compile ──────────────────────────────────────────────────
            logger.log("Executing javac compiler engine...");
            List<String> javacCmd = new ArrayList<>();
            javacCmd.add("javac");
            javacCmd.add("-cp");
            javacCmd.add(classpath.toString());
            javacCmd.add("-d");
            javacCmd.add(tempClassesFolder.toString());
            javacCmd.addAll(javaFiles);

            boolean compileSuccess = runProcess(javacCmd, projectFolder.toFile(), logger);
            if (!compileSuccess) {
                logger.error("Compilation failed! Please check your code syntax.");
                fileManager.deleteFolder(tempClassesFolder);
                return;
            }
            logger.log("Java source code successfully compiled into byte code.");

            // ── Copy resources (AddResources) ────────────────────────────
            if (cfg.addResources) {
                // Resources live at {ProjectFolder}/src/main/resources
                Path resourcesFolder = projectFolder.resolve("src/main/resources");
                if (Files.exists(resourcesFolder)) {
                    logger.log("Adding resources from: " + resourcesFolder);
                    copyDirectory(resourcesFolder, tempClassesFolder, logger, cfg.debug);
                } else {
                    logger.warn("AddResources=true but folder not found: " + resourcesFolder);
                }
            }

            // ── Copy icon into jar resources (if HasIcon) ────────────────
            if (cfg.hasIcon) {
                if (cfg.iconPath.isEmpty()) {
                    logger.warn("Icon.HasIcon is true but Icon.IconPath is empty — skipping icon.");
                } else {
                    Path iconSrc = Paths.get(cfg.iconPath);
                    if (!iconSrc.isAbsolute()) iconSrc = projectFolder.resolve(cfg.iconPath);
                    if (Files.exists(iconSrc)) {
                        Path iconDest = tempClassesFolder.resolve(iconSrc.getFileName());
                        Files.copy(iconSrc, iconDest, StandardCopyOption.REPLACE_EXISTING);
                        logger.log("Icon packed: " + iconSrc.getFileName());
                    } else {
                        logger.warn("Icon file not found: " + iconSrc);
                    }
                }
            }

            // ── Library handling (Onefile vs Normal) ─────────────────────
            if ("Onefile".equalsIgnoreCase(cfg.buildMode)) {
                logger.log("Extracting libraries for Onefile build via stream...");
                for (String lib : cfg.library) {
                    Path libPath = projectFolder.resolve(lib);
                    if (Files.exists(libPath)) {
                        logger.log("  + Extracting: " + libPath.getFileName());
                        extractJar(libPath, tempClassesFolder, logger, cfg.debug);
                    } else {
                        logger.warn("  ! Library not found: " + libPath);
                    }
                }
            } else {
                logger.log("Setting up Normal build with lib folder...");
                Path libOutputFolder = outputFolder.resolve("lib");
                fileManager.createFolder(libOutputFolder, false);
                for (String lib : cfg.library) {
                    Path libPath = projectFolder.resolve(lib);
                    if (Files.exists(libPath)) {
                        logger.log("  + Copying: " + libPath.getFileName());
                        Files.copy(libPath, libOutputFolder.resolve(libPath.getFileName()), StandardCopyOption.REPLACE_EXISTING);
                    } else {
                        logger.warn("  ! Library not found: " + libPath);
                    }
                }
            }

            // ── Package JAR ──────────────────────────────────────────────
            String outputJarName = projectFolder.getFileName().toString() + ".jar";
            Path finalJarPath    = outputFolder.resolve(outputJarName);
            logger.log("Packaging compiled files into executable JAR: " + outputJarName + "...");

            try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(Files.newOutputStream(finalJarPath))) {
                // Manifest
                java.util.zip.ZipEntry manifestEntry = new java.util.zip.ZipEntry("META-INF/MANIFEST.MF");
                zos.putNextEntry(manifestEntry);
                String manifestContent = "Manifest-Version: 1.0\r\nMain-Class: Main\r\n\r\n";
                zos.write(manifestContent.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                zos.closeEntry();

                packDirectory(tempClassesFolder, tempClassesFolder, zos, logger, cfg.debug);

                logger.log("\nSUCCESSFUL BUILD!");
                logger.log("Output Location: " + finalJarPath.toAbsolutePath());
            } catch (Exception jarEx) {
                logger.error("Packaging failed while executing zip assembly engine: " + jarEx.getMessage());
            }

            logger.log("Cleaning temporary classes directory...");
            fileManager.deleteFolder(tempClassesFolder);

        } catch (Exception error) {
            logger.error("Unexpected internal compiler error: " + error.getMessage());
        }
    }

    // ── Legacy overload kept for backwards compatibility ──────────────────────

    /** @deprecated Use {@link #StartBuild(JBTConfig, Logger)} instead. */
    @Deprecated
    @SuppressWarnings("deprecation")
    public void StartBuild(String projectFolderStr, String srcFolderStr, String outputFolderStr,
                           List<String> libraries, String buildMode, GUI gui) {
        JBTConfig cfg = new JBTConfig();
        cfg.projectFolder = projectFolderStr;
        cfg.srcFolder     = srcFolderStr;
        cfg.output        = outputFolderStr;
        cfg.library       = new ArrayList<>(libraries);
        cfg.buildMode     = buildMode;

        Logger logger = new Logger(false, msg -> gui.Log(msg));
        StartBuild(cfg, logger);
    }

    // ── File helpers ─────────────────────────────────────────────────────────

    private void findJavaFiles(File folder, List<String> javaFiles) {
        File[] files = folder.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) {
                findJavaFiles(file, javaFiles);
            } else if (file.getName().endsWith(".java")) {
                javaFiles.add(file.getAbsolutePath());
            }
        }
    }

    private boolean runProcess(List<String> command, File workingDir, Logger logger) {
        try {
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.directory(workingDir);
            builder.redirectErrorStream(true);
            Process process = builder.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logger.log("[Compiler] " + line);
                }
            }

            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            logger.error("System process execution failure: " + e.getMessage());
            return false;
        }
    }

    /** Recursively copy a directory tree into destRoot (preserving sub-paths). */
    private void copyDirectory(Path src, Path destRoot, Logger logger, boolean debug) throws Exception {
        Files.walk(src).forEach(path -> {
            try {
                if (Files.isDirectory(path)) return;
                Path relative = src.relativize(path);
                Path dest     = destRoot.resolve(relative);
                Files.createDirectories(dest.getParent());
                Files.copy(path, dest, StandardCopyOption.REPLACE_EXISTING);
                if (debug) logger.log("  Resource: " + relative);
            } catch (IOException e) {
                logger.warn("Could not copy resource: " + path + " — " + e.getMessage());
            }
        });
    }

    /** Extract a JAR/ZIP into destFolder, skipping signing metadata. */
    private void extractJar(Path jarPath, Path destFolder, Logger logger, boolean debug) {
        try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(Files.newInputStream(jarPath))) {
            java.util.zip.ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                String name      = entry.getName();
                String upperName = name.toUpperCase();
                if (upperName.equalsIgnoreCase("META-INF/MANIFEST.MF") ||
                    upperName.equalsIgnoreCase("module-info.class")) continue;
                if (upperName.startsWith("META-INF/") &&
                    (upperName.endsWith(".SF") || upperName.endsWith(".DSA") || upperName.endsWith(".RSA"))) continue;

                Path target = destFolder.resolve(name);
                Files.createDirectories(target.getParent());
                Files.copy(zis, target, StandardCopyOption.REPLACE_EXISTING);
                if (debug) logger.log("    Extracted: " + name);
            }
        } catch (IOException e) {
            logger.error("Failed to extract " + jarPath.getFileName() + ": " + e.getMessage());
        }
    }

    private void packDirectory(Path baseFolder, Path currentFolder,
                               java.util.zip.ZipOutputStream zos,
                               Logger logger, boolean debug) throws Exception {
        File[] files = currentFolder.toFile().listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) {
                packDirectory(baseFolder, file.toPath(), zos, logger, debug);
            } else {
                Path relativePath = baseFolder.relativize(file.toPath());
                String entryName  = relativePath.toString().replace("\\", "/");
                if (debug) logger.log("  Packing: " + entryName);
                java.util.zip.ZipEntry zipEntry = new java.util.zip.ZipEntry(entryName);
                zos.putNextEntry(zipEntry);
                Files.copy(file.toPath(), zos);
                zos.closeEntry();
            }
        }
    }
}