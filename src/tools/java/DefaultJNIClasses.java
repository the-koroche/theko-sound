/*
 * Copyright 2025-present Alex Soloviov (aka Theko)
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Set;

public class DefaultJNIClasses {

    private record ClassInfo(Class<?> cls, String outName) {}

    public static void main(String[] args) {
        Set<ClassInfo> classes = Set.of(
            new ClassInfo(java.lang.IllegalArgumentException.class, "Java_IllegalArgumentException"),
            new ClassInfo(java.lang.RuntimeException.class, "Java_RuntimeException"),
            new ClassInfo(java.util.concurrent.atomic.AtomicReference.class, "Java_Concurrent_AtomicReference"),
            new ClassInfo(org.slf4j.Logger.class, "SLF4J_Logger"),
            new ClassInfo(org.slf4j.LoggerFactory.class, "SLF4J_LoggerFactory"),
            new ClassInfo(org.theko.sound.AudioFlow.class, "ThekoSound_AudioFlow"),
            new ClassInfo(org.theko.sound.AudioFormat.class, "ThekoSound_AudioFormat"),
            new ClassInfo(org.theko.sound.AudioFormat.Encoding.class, "ThekoSound_AudioFormat_Encoding"),
            new ClassInfo(org.theko.sound.AudioPort.class, "ThekoSound_AudioPort"),
            new ClassInfo(org.theko.sound.AudioPortsNotFoundException.class, "ThekoSound_AudioPortsNotFoundException"),
            new ClassInfo(org.theko.sound.UnsupportedAudioEncodingException.class, "ThekoSound_UnsupportedAudioEncodingException"),
            new ClassInfo(org.theko.sound.UnsupportedAudioFormatException.class, "ThekoSound_UnsupportedAudioFormatException"),
            new ClassInfo(org.theko.sound.backends.AudioBackendException.class, "ThekoSound_AudioBackendException"),
            new ClassInfo(org.theko.sound.backends.DeviceInactiveException.class, "ThekoSound_DeviceInactiveException"),
            new ClassInfo(org.theko.sound.backends.DeviceInvalidatedException.class, "ThekoSound_DeviceInvalidatedException"),
            new ClassInfo(org.theko.sound.backends.wasapi.WASAPIPortHandle.class, "ThekoSound_WASAPIPortHandle")
        );

        classes.stream()
            .sorted((a, b) -> a.cls.getCanonicalName().compareTo(b.cls.getCanonicalName()))
            .forEach(info -> {
                String path = "src/native/cache/" + info.outName + ".hpp";
                System.out.println("Generating: " + info.cls.getCanonicalName() + " -> " + path);
                writeString(path, JNIClassCacheGenerator.generate(info.cls, info.outName));
            });
    }

    private static void writeString(String filePath, String content) {
        try {
            Files.writeString(Path.of(filePath), content,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}