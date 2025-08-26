import org.theko.sound.AudioMixer;

public class CircularReferenceMixerTest {
    public static void main(String[] args) {
        AudioMixer a = new AudioMixer();
        AudioMixer b = new AudioMixer();

        try {
            a.addInput(b);
            b.addInput(a);
            System.out.println("Failed");
        } catch (IllegalArgumentException e) {
            System.out.println("Passed");
        }
    }
}
