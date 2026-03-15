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

package org.theko.sound.codecs.ffmpeg;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theko.sound.AudioFormat;
import org.theko.sound.codecs.AudioCodec;
import org.theko.sound.codecs.AudioCodecException;
import org.theko.sound.codecs.AudioCodecType;
import org.theko.sound.codecs.AudioDecodeResult;
import org.theko.sound.codecs.AudioEncodeConfig;
import org.theko.sound.codecs.AudioEncodeResult;
import org.theko.sound.codecs.AudioMetadata;
import org.theko.sound.codecs.AudioTag;
import org.theko.sound.codecs.wav.WavCodec;
import org.theko.sound.samples.SamplesConverter;
import org.theko.sound.samples.SamplesValidation;

/**
 * The FFmpegCodec class is a wrapper around the FFmpeg multimedia framework.
 * It provides methods to encode and decode audio data using FFmpeg.
 *
 * <p>The class uses the ProcessBuilder API to launch FFmpeg with the proper
 * arguments in order to encode or decode audio data.
 *
 * <p>FFmpeg is required to be installed and available on the system path in order
 * for this class to work properly.
 *
 * @author Theko
 * @since 0.3.0-beta
 */
@AudioCodecType(
    name = "FFmpeg",
    extensions = {
        "aac", "ac3", "aiff", "aif", "alac", "amr",
        "flac", "m4a", "mp2", "mp3", "oga", "ogg",
        "opus", "wma", "wv", "caf"
    }
)
public class FFmpegCodec extends AudioCodec {

    private static final Logger logger = LoggerFactory.getLogger(FFmpegCodec.class);
    private static final Logger ffmpegLogger = LoggerFactory.getLogger("FFmpeg");

    @Override
    public AudioDecodeResult decode(InputStream is) throws AudioCodecException {
        if (is == null) throw new AudioCodecException("InputStream is null");

        ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg",
                "-hide_banner",
                "-loglevel", "error",
                "-i", "pipe:0",
                "-f", "wav",
                "pipe:1"
        );
        if (logger.isTraceEnabled()) {
            String args = pb.command().stream().reduce((a, b) -> a + " " + b).get();
            logger.trace("FFmpeg launch command: {}", args);
        }

        try {
            Process ffmpeg = pb.start();

            Thread writerThread = new Thread(() -> {
                try (OutputStream stdin = ffmpeg.getOutputStream()) {
                    byte[] buf = new byte[8192];
                    int read;
                    while ((read = is.read(buf)) != -1) {
                        stdin.write(buf, 0, read);
                    }
                    stdin.flush();
                } catch (IOException e) {
                    logger.debug("IOException while writing to stdin.", e);
                }
            });
            writerThread.start();

            Thread stderrThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(ffmpeg.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        ffmpegLogger.error("[stderr] {}", line);
                    }
                } catch (IOException e) {
                    logger.debug("IOException while reading stderr.", e);
                }
            });
            stderrThread.start();

            ByteArrayOutputStream stdoutBuffer = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int read;
            try (InputStream stdout = ffmpeg.getInputStream()) {
                while ((read = stdout.read(buf)) != -1) {
                    stdoutBuffer.write(buf, 0, read);
                }
            }

            logger.trace("Waiting for writer and stderr threads to finish...");
            writerThread.join();
            stderrThread.join();
            logger.trace("Waiting for FFmpeg to finish...");
            int exitCode = ffmpeg.waitFor();

            logger.trace("FFmpeg exited with code {}", exitCode);
            byte[] wavData = stdoutBuffer.toByteArray();

            if (exitCode != 0) {
                logger.warn("FFmpeg exited with code {}", exitCode);
                throw new AudioCodecException("FFmpeg failed with exit code " + exitCode);
            }

            logger.trace("Starting WAVECodec decode with {} bytes", wavData.length);
            AudioDecodeResult adr = null;
            try (ByteArrayInputStream bais = new ByteArrayInputStream(wavData)) {
                adr = new WavCodec().decode(bais);
            }
            if (adr == null) {
                throw new AudioCodecException("FFmpeg decode failed");
            }

            return new AudioDecodeResult(getInfo(), adr.getSamples(), adr.getAudioFormat(), adr.getMetadata());
        } catch (IOException e) {
            logger.warn("IO error during FFmpeg decode", e);
            throw new AudioCodecException("IO error during FFmpeg decode", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AudioCodecException("FFmpeg process interrupted", e);
        }
    }

    @Override
    public AudioEncodeResult encode(float[][] samples, AudioFormat samplesFormat, AudioEncodeConfig config) throws AudioCodecException {
        if (samplesFormat == null) {
            throw new AudioCodecException("Audio format is null");
        }
        if (config == null) {
            throw new AudioCodecException("Audio encode config is null");
        }
        SamplesValidation.validateSamples(samples);

        byte[] pcmData = SamplesConverter.fromSamples(samples, samplesFormat);

        List<String> cmd = new ArrayList<>();
        cmd.add("ffmpeg");
        cmd.add("-hide_banner");
        cmd.add("-loglevel"); cmd.add("error");

        cmd.add("-f"); cmd.add(getFFmpegFormat(samplesFormat));
        cmd.add("-ar"); cmd.add(String.valueOf(samplesFormat.getSampleRate()));
        cmd.add("-ac"); cmd.add(String.valueOf(samplesFormat.getChannels()));
        cmd.add("-fflags");
        cmd.add("+bitexact");
        cmd.add("-i"); cmd.add("pipe:0");

        cmd.add("-f"); cmd.add((String) config.getOptions().get("ffmpeg.container"));
        addMetadata(cmd, config.getMetadata());
        cmd.add("pipe:1");

        ProcessBuilder pb = new ProcessBuilder(cmd);
        if (logger.isTraceEnabled()) {
            String args = pb.command().stream().reduce((a, b) -> a + " " + b).get();
            logger.trace("FFmpeg launch command: {}", args);
        }

        try {
            Process ffmpeg = pb.start();

            Thread writerThread = new Thread(() -> {
                try (OutputStream stdin = ffmpeg.getOutputStream()) {
                    stdin.write(pcmData);
                    stdin.flush();
                } catch (IOException e) {
                    logger.debug("IOException while writing to stdin.", e);
                }
            });
            writerThread.start();

            Thread stderrThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(ffmpeg.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        ffmpegLogger.error("[stderr] {}", line);
                    }
                } catch (IOException e) {
                    logger.debug("IOException while reading stderr.", e);
                }
            });
            stderrThread.start();

            ByteArrayOutputStream stdoutBuffer = new ByteArrayOutputStream();
            try (InputStream stdout = ffmpeg.getInputStream()) {
                byte[] buf = new byte[8192];
                int read;
                while ((read = stdout.read(buf)) != -1) {
                    stdoutBuffer.write(buf, 0, read);
                }
            }

            logger.trace("Waiting for writer and stderr threads to finish...");
            writerThread.join();
            stderrThread.join();
            logger.trace("Waiting for FFmpeg to finish...");
            int exitCode = ffmpeg.waitFor();

            logger.trace("FFmpeg exited with code {}", exitCode);
            if (exitCode != 0) {
                throw new AudioCodecException("FFmpeg encode failed with code " + exitCode);
            }

            byte[] encodedData = stdoutBuffer.toByteArray();
            logger.trace("Encoded {} bytes", encodedData.length);
            return new AudioEncodeResult(getInfo(), encodedData, samplesFormat, config.getMetadata());

        } catch (IOException e) {
            logger.warn("IO error during FFmpeg encode", e);
            throw new AudioCodecException("FFmpeg encode error", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AudioCodecException("FFmpeg process interrupted", e);
        }
    }

    private String getFFmpegFormat(AudioFormat fmt) {
        AudioFormat.Encoding enc = fmt.getEncoding();
        int bits = fmt.getBitsPerSample();
        boolean be = fmt.isBigEndian();

        return switch (enc) {
            case PCM_FLOAT ->
                "f" + bits + (be ? "be" : "le");

            case PCM_SIGNED ->
                "s" + bits + (bits == 8 ? "" : (be ? "be" : "le"));

            case PCM_UNSIGNED -> {
                if (bits == 8) {
                    yield "u8";
                }
                yield "u" + bits + (be ? "be" : "le");
            }
            case ULAW -> "mulaw";
            case ALAW -> "alaw";
            default ->
                throw new IllegalArgumentException("Unsupported encoding: " + enc);
        };
    }

    private void addMetadata(List<String> cmd, AudioMetadata metadata) {
        List<AudioTag> tags = metadata.asList();
        for (AudioTag tag : tags) {
            cmd.add("-metadata"); cmd.add(tag.getKey() + "=" + tag.getValue());
        }
    }
}