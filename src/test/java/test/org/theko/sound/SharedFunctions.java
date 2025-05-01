package test.org.theko.sound;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.theko.sound.AudioFormat;
import org.theko.sound.AudioOutputLine;
import org.theko.sound.AudioPortsNotFoundException;
import org.theko.sound.UnsupportedAudioFormatException;
import org.theko.sound.codec.AudioCodecException;
import org.theko.sound.codec.AudioCodecs;
import org.theko.sound.codec.AudioDecodeResult;

public class SharedFunctions {
    public static AudioDecodeResult decodeAudioFile(String filePath) {
        try {
            return AudioCodecs.getCodec(AudioCodecs.fromExtension(getFileExtension(filePath))).callDecode(new FileInputStream(filePath));
        } catch (FileNotFoundException | AudioCodecException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String chooseAudioFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select an audio file");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Audio Files", "wav", "ogg", "flac"));
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        int result = fileChooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            return fileChooser.getSelectedFile().getAbsolutePath();
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

    public static void playAudioData(byte[] data, AudioFormat format) {
        try {
            AudioOutputLine aol = new AudioOutputLine();
            aol.open(format);
            aol.start();
            aol.write(data, 0, data.length);
            aol.stop();
            aol.close();
        } catch (UnsupportedAudioFormatException | AudioPortsNotFoundException e) {
            e.printStackTrace();
        }
    }
}