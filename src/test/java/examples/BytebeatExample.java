package examples;

import org.theko.sound.AudioFormat;
import org.theko.sound.AudioOutputLayer;

public class BytebeatExample {
    static final int bytebeatRate = 8000;

    static int bytebeat(int t) {
        return t * (42 & t >> 10);
    }

    public static void main(String[] args) throws Exception {
        try (AudioOutputLayer aol = new AudioOutputLayer()) {
            final long[] index = {0};
            aol.open(AudioFormat.builder().rate(44100).mono().build());
            aol.setRootNode((samples, rate) -> {
                for (int i = 0; i < samples[0].length; i++) {
                    int t = (int) (index[0]++ * bytebeatRate / rate);
                    int o = bytebeat(t) & 0xFF;
                    samples[0][i] = (o / 127.5f) - 1f;
                }
            });
            aol.start();
            System.out.println("Press enter to stop.");
            System.in.read();
        }
    }
}