import java.awt.FileDialog;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import org.theko.sound.codec.AudioCodecException;
import org.theko.sound.codec.AudioCodecs;
import org.theko.sound.codec.AudioDecodeResult;

import java.awt.Frame;

public class SharedFunctions {
    public static AudioDecodeResult decodeAudioFile(String filePath) {
        try {
            return AudioCodecs.getCodec(AudioCodecs.fromExtension(getFileExtension(filePath))).decode(new FileInputStream(filePath));
        } catch (FileNotFoundException | AudioCodecException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static String chooseAudioFile() {
        FileDialog fileDialog = new FileDialog((Frame) null, "Select an audio file (*.wav, *.ogg, *.flac)", FileDialog.LOAD);
        fileDialog.setFilenameFilter((dir, name) ->
            name.endsWith(".wav") || name.endsWith(".ogg") || name.endsWith(".flac")
        );

        fileDialog.setVisible(true);

        String directory = fileDialog.getDirectory();
        String filename = fileDialog.getFile();

        if (directory != null && filename != null) {
            return new File(directory, filename).getAbsolutePath();
        } else {
            System.out.println("No file selected.");
            return null;
        }
    }

    public static String getFileExtension(String filepath) {
        int index = filepath.lastIndexOf(".");
        if (index > 0) {
            return filepath.substring(index + 1);
        }
        return "";
    }
}