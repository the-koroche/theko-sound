/*
 * Copyright 2025 Alex Soloviov (aka Theko)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;
import java.io.FileNotFoundException;

import org.theko.sound.SoundPlayer;
import org.theko.sound.codec.AudioCodecNotFoundException;

/*
 * Simple example: plays a WAV file using SoundPlayer.
 * Use Ctrl+C to stop playback.
 */
public class MinimalWavePlayback {

    public static void main(String[] args) {
        try (SoundPlayer player = new SoundPlayer()) {
            File audioFile = SharedFunctions.chooseAudioFile();
            if (audioFile == null) {
                System.out.println("No audio file selected.");
                return;
            }
            player.open(audioFile);
            player.startAndWait(); // Start playback and wait for it to finish
        } catch (AudioCodecNotFoundException e) {
            // AudioCodecNotFoundException used to handle an unsupported audio extensions,
            // such as mp3 or flac
            System.err.println("Provided audio file is unsupported.");
        } catch (FileNotFoundException e) {
            System.err.println("File not found.");
        } catch (InterruptedException e) {
            // When the playback is interrupted
            Thread.currentThread().interrupt();
            System.err.println("Playback interrupted.");
        }
    }
}