import helpers.FileChooser;

import java.io.File;
import javax.swing.filechooser.FileNameExtensionFilter;

public final class SharedFunctions {

    public static File chooseAudioFile() {
        FileChooser fc = new FileChooser();
        return fc.chooseFile(new FileNameExtensionFilter("Audio Files", "wav", "wave"));
    }
}