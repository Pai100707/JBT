import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import javax.swing.JFileChooser;
import javax.swing.UIManager;

public class FileManager {
    public void deleteFolder(Path Folder) {
        try {
            if (Files.exists(Folder)) {
                Files.walk(Folder).sorted(Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException error) {
                        System.out.println("Error: " + error);
                    }
                });
            }
        } catch (IOException error) {
            System.out.println(error);
        }
    }
    public void createFolder(Path Folder, boolean deleteFolderWhenExists) {
        try {
            if (deleteFolderWhenExists && Files.exists(Folder)) {
                deleteFolder(Folder);
            }
            Files.createDirectories(Folder);
        } catch (IOException error) {
            System.out.println("Error: " + error);
        }
    }

    public void deleteFile(Path File) {
        try {
            Files.deleteIfExists(File);
        } catch (IOException error) {
            System.out.println("Error: " + error);
        }
    }

    public void createFile(Path File, boolean deleteFileWhenExists) {
        try {
            if (deleteFileWhenExists && Files.exists(File)) {
                deleteFile(File);
            }

            if (File.getParent() != null) {
                Files.createDirectories(File.getParent());
            }

            if (!Files.exists(File)) {
                Files.createFile(File);
            }
        } catch (IOException error) {
            System.out.println("Error: " + error);
        }
    }
    
    public File openFileChooser(int selectionMode) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception error) {
            System.out.println("Error: " + error);
        }

        JFileChooser fileChooser = new JFileChooser();
        
        fileChooser.setFileSelectionMode(selectionMode); 
        
        int result = fileChooser.showOpenDialog(null);

        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedPath = fileChooser.getSelectedFile();
            return selectedPath;
        } else {
            return null;
        }
    }
}
