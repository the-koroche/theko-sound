import org.theko.sound.AudioMixer;

/**
 * Tests for circular references in {@link AudioMixer}.
 * <p>
 * Circular references occur when two or more {@link AudioMixer} objects
 * reference each other in a loop. This test class attempts to create such
 * references and verifies that the {@link AudioMixer} class correctly
 * handles them.
 */
public class CircularReferenceMixerTest {
    public static void main(String[] args) {
        AudioMixer a, b, c, d;

        a = new AudioMixer();
        b = new AudioMixer();

        System.out.println("Testing A -> B -> A");
        try {
            a.addInput(b);
            b.addInput(a);
            System.out.println("Failed #1");
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            System.out.println("Passed #1");
        }

        a = new AudioMixer();
        b = new AudioMixer();
        c = new AudioMixer();
        
        System.out.println("Testing A -> B -> C -> A");
        try {
            a.addInput(b);
            b.addInput(c);
            c.addInput(a);
            System.out.println("Failed #2");
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            System.out.println("Passed #2");
        }

        a = new AudioMixer();
        b = new AudioMixer();
        c = new AudioMixer();
        d = new AudioMixer();
        
        System.out.println("Testing A -> B -> C -> D -> A");
        try {
            a.addInput(b);
            b.addInput(c);
            c.addInput(d);
            d.addInput(a);
            System.out.println("Failed #3");
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            System.out.println("Passed #3");
        }
    }
}
