# Theko-Sound

High-performance modular audio library for Java (17+). Provides tools for working with audio devices, playback, routing, effects, visualization, encoding/decoding, and DSP utilities.

---

## 📖 Table of Contents

* [Features](#-features)
* [Installation](#-installation)
* [Quick Start](#-quick-start)

  * [Minimal WAV Playback](#minimal-wav-playback)
  * [Routing and Effects](#routing-and-effects)
  * [Spectrum Visualization](#spectrum-visualization)
* [Architecture](#-architecture)
* [Roadmap](#-roadmap)
* [Known Limitations](#-known-limitations)
* [Dependencies](#-dependencies)
* [License](#-license)

---

## ✨ Features

* Multiple **audio backends** (JavaSound, WASAPI, ALSA, etc.)
* **Fallback** to JavaSound if no native backend is available
* **Routing through mixers** with effect support
* **Effects** (e.g., Bitcrusher) with dynamic parameter control
* **Visualization** (spectrum, waveform, etc.)
* **Codecs**: WAV (read/write), AIFF and AU planned
* **DSP Utilities**: FFT, window functions, filters
* **Audio metadata** (tags in supported formats)
* Extendable with custom backends/codecs/effects

---

## ⚡ Installation

The library is available through **Maven Central**:

```xml
<dependency>
    <groupId>org.theko.sound</groupId>
    <artifactId>theko-sound</artifactId>
    <version>2.4.0</version>
</dependency>
```

---

## 🚀 Quick Start

### Minimal WAV Playback

```java
try (SoundPlayer player = new SoundPlayer()) {
    player.open("song.wav");
    player.startAndWait();
} catch (Exception e) {
    e.printStackTrace();
}
```

### Routing and Effects

```java
AudioMixerOutput out = new AudioMixerOutput();
AudioMixer mixer = new AudioMixer();
out.setMixer(mixer);

SoundSource sound = new SoundSource();
sound.open("song.wav");

BitcrusherEffect bitcrusher = new BitcrusherEffect();
bitcrusher.getBitdepth().setValue(6);
bitcrusher.getSampleRateReduction().setValue(4000);

mixer.addEffect(bitcrusher);
out.open(sound.getAudioFormat());
mixer.addInput(sound);

sound.start();
```

### Spectrum Visualization

```java
SoundPlayer player = new SoundPlayer();
player.open("song.wav");

AudioMixer mixer = player.getInnerMixer();
SpectrumVisualizer spectrum = new SpectrumVisualizer(60.0f);
mixer.addEffect(spectrum);

JFrame frame = new JFrame("Spectrum");
frame.add(spectrum.getPanel());
frame.setSize(600, 400);
frame.setVisible(true);

player.startAndWait();
spectrum.close();
frame.dispose();
```

---

## 🏗 Architecture

Main modules of the library:

* `backend` — audio backends (JavaSound, WASAPI, etc.)
* `codec` — codecs (WAV, AIFF, AU)
* `controls` — controls for dynamic parameter changes
* `event` — event model
* `utils` — utilities (DSP, filters, windows, etc.)
* `visualizers` — spectrum and waveform visualization

---

## 🛣 Roadmap

**Backends:**

* [x] JavaSound — Backend, Input, Output
* [ ] WASAPI — In progress (Backend, Output)
* [ ] ALSA — Planned
* [ ] CoreAudio — Planned
* [ ] DirectSound — If possible
* [ ] PulseAudio — If possible

**Codecs:**

* [x] WAVE — Encoding, Decoding
* [ ] AIFF — Planned
* [ ] AU — Planned
* [ ] FLAC — Unlikely

---

## ⚠️ Known Limitations

* Many parts are in **active development** and may be unstable
* Not all backends are implemented
* Codec support is limited (WAV only)
* No CI/CD — builds are currently done manually

---

## 📦 Dependencies

```xml
<dependencies>
    <dependency>
        <groupId>org.slf4j</groupId>
        <artifactId>slf4j-api</artifactId>
        <version>2.0.13</version>
    </dependency>
    <dependency>
        <groupId>org.slf4j</groupId>
        <artifactId>slf4j-simple</artifactId>
        <version>2.0.13</version>
    </dependency>
</dependencies>
```

---

## 📜 License

This project is licensed under the **Apache License 2.0**.

* [LICENSE](LICENSE)
* [NOTICE](NOTICE)
