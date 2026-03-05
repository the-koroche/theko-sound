package helpers;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.theko.sound.codecs.AudioCodecInfo;
import org.theko.sound.codecs.AudioCodecs;

public final class FileChooserHelper {

    private static final String lastOpenedFilePath = System.getProperty("java.io.tmpdir") + "/TSoundEx_FCH_LastOpenedFile.txt";
    private static File lastOpened = getLastOpened();
    
    private static File getLastOpened() {
        Path path = Path.of(lastOpenedFilePath);
        if (!Files.exists(path)) return null;
        
        try {
            String savedPath = Files.readString(path, StandardCharsets.UTF_8).trim();
            if (savedPath.isEmpty()) return null;

            File f = new File(savedPath);
            return f.exists() ? f : null;
        } catch (IOException e) {
            return null;
        }
    }

    private static void setLastOpened(File file) {
        try {
            Files.writeString(
                Path.of(lastOpenedFilePath),
                file.getAbsolutePath(),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
            );
            lastOpened = file;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static File chooseAudioFile() {
        String[] extensions = getSupportedAudioExtensions();
        return chooseFile("Audio", extensions, getAdditionalAudioFilters());
    }

    public static String[] getSupportedAudioExtensions() {
        return AudioCodecs.getAllCodecs()
            .stream()
            .flatMap(codec -> Arrays.stream(codec.getExtensions()))
            .distinct()
            .toArray(String[]::new);
    }

    public static Map<String, String[]> getAdditionalAudioFilters() {
        return AudioCodecs.getAllCodecs()
        .stream()
        .collect(Collectors.toMap(
            AudioCodecInfo::getName,
            AudioCodecInfo::getExtensions,
            (existing, replacement) -> existing
        ));
    }

    public static File chooseFile(String name, String[] extensions, Map<String, String[]> additionalFilters) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select " + getArticle(name) + name + " file");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        if (lastOpened != null) {
            chooser.setSelectedFile(lastOpened);
        }

        FileNameExtensionFilter filter = createFilter(name + " files", extensions);
        List<FileNameExtensionFilter> additionalFileFilters = createChoosableFilters(additionalFilters);
        
        if (filter != null) {
            chooser.setFileFilter(filter);
            for (FileNameExtensionFilter f : additionalFileFilters) {
                chooser.addChoosableFileFilter(f);
            }
            chooser.setAcceptAllFileFilterUsed(false);
        } else {
            chooser.setAcceptAllFileFilterUsed(true);
        }

        int result = chooser.showOpenDialog(null);

        if (result == JFileChooser.APPROVE_OPTION) {
            setLastOpened(chooser.getSelectedFile());
            return chooser.getSelectedFile();
        }

        return null;
    }

    private static String getArticle(String word) {
        if (word == null || word.isEmpty()) {
            return "a ";
        }
        char firstChar = Character.toLowerCase(word.charAt(0));
        if (firstChar == 'a' || firstChar == 'e' || firstChar == 'i' ||
                firstChar == 'o' || firstChar == 'u' || firstChar == 'y') {
            return "an ";
        }
        return "a ";
    }

    private static FileNameExtensionFilter createFilter(String prefix, String[] extensions) {
        if (extensions == null || extensions.length == 0) {
            return null;
        }
        StringBuilder description = new StringBuilder()
                .append(prefix != null ? prefix : "").append("(");
        for (int i = 0; i < extensions.length; i++) {
            description.append("*.").append(extensions[i]);
            if (i < extensions.length - 1) {
                description.append(",");
            }
        }
        description.append(")");

        return new FileNameExtensionFilter(description.toString(), extensions);
    }

    private static List<FileNameExtensionFilter> createChoosableFilters(Map<String, String[]> filtersMap) {
        List<FileNameExtensionFilter> filters = new ArrayList<>();
        for (Map.Entry<String, String[]> entry : filtersMap.entrySet()) {
            FileNameExtensionFilter filter = createFilter(entry.getKey(), entry.getValue());
            if (filter != null) {
                filters.add(filter);
            }
        }
        return filters;
    }
}