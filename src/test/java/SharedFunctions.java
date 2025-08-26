import org.theko.sound.codec.AudioCodecException;
import org.theko.sound.codec.AudioCodecs;
import org.theko.sound.codec.AudioDecodeResult;

import helpers.FileChooser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import javax.swing.filechooser.FileNameExtensionFilter;

public final class SharedFunctions {

    public static AudioDecodeResult decodeAudioFile(String filePath) {
        try {
            return 
                AudioCodecs.getCodec(
                    AudioCodecs.fromExtension(
                        getFileExtension(filePath)
                    )
                ).decode(new FileInputStream(filePath));
        } catch (FileNotFoundException | AudioCodecException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static File chooseAudioFile() {
        FileChooser fc = new FileChooser();
        return fc.chooseFile(new FileNameExtensionFilter("Audio Files", "wav", "wave"));
    }

    public static String getFileExtension(String filepath) {
        int index = filepath.lastIndexOf(".");
        if (index > 0) {
            return filepath.substring(index + 1);
        }
        return "";
    }
}