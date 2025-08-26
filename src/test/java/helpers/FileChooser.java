package helpers;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.*;

/**
 * Helper class for choosing files with a Swing {@link JFileChooser}.
 * <p>
 * Keeps track of the last path the user has chosen a file from.
 * </p>
 * 
 * @since 2.4.0
 * @author Theko
 */
public class FileChooser {

    private final String tempFileName;

    public FileChooser() {
        this.tempFileName = System.getProperty("java.io.tmpdir") + "\\fileChooser\\lastPath.txt";
    }

    public File chooseFile(FileNameExtensionFilter filter) {
        JFileChooser chooser = new JFileChooser();

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            SwingUtilities.updateComponentTreeUI(chooser);
        } catch (Exception ignored) {}

        File lastPathFile = new File(tempFileName);
        if (lastPathFile.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(lastPathFile))) {
                String path = br.readLine();
                if (path != null && !path.isEmpty()) {
                    chooser.setCurrentDirectory(new File(path));
                }
            } catch (IOException ignored) {}
        }

        if (filter != null) {
            chooser.setFileFilter(filter);
        }

        int result = chooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = chooser.getSelectedFile();

            try (BufferedWriter bw = new BufferedWriter(new FileWriter(lastPathFile))) {
                bw.write(selectedFile.getParent());
            } catch (IOException ignored) {}

            return selectedFile;
        }

        return null;
    }
}
