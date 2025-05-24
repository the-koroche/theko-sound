# Theko-Sound

🎧 *High-performance and extensible audio library for Java*

---

## 📌 What is Theko-Sound?

**Theko-Sound** is a high-performance, modular audio library for Java that enables developers to easily implement advanced audio features such as playback, processing, visualization, and real-time effects. Built with extensibility in mind, it offers fine-grained control over audio pipelines and supports both static and dynamic loading of backends and codecs.

Designed for use in games, DAWs, media players, and audio visualization tools.

---

## 🚀 Features

* 🖥 **Native audio output** via **JavaSound** *(more backends in development)*
* 🔌 **Dynamic loading of codecs and backends**
* 📁 **Audio decoding/encoding** for **WAV** (FLAC and OGG Vorbis planned)
* 🎚️ **Flexible mixing system** to combine and route audio streams
* 🎛️ **Real-time audio effects**: echo, equalizers, filters *(work in progress)*
* 🔁 **Format conversion**: sample rate, bit depth, channels
* 📊 **FFT with windowing support** for spectrum analysis and visualizers
* 🎨 **Real-time audio visualizers**
* 🧠 **Preset system** *(planned)*
* 🪵 **SLF4J-based logging**

---

## 🛠 Backend Roadmap

| Backend     | Status        |
| ----------- | ------------- |
| JavaSound   | ✅ Implemented |
| WASAPI      | 🔜 Planned    |
| DirectSound | 🔜 Planned    |
| PulseAudio  | 🔜 Planned    |
| ALSA        | 🔜 Planned    |

Backends are dynamically loaded and modularized for minimal footprint and easy platform-specific integration.

---

## 📦 How to Use

1. Go to the [Releases](#) section.
2. Download the latest `.jar` file.
3. Add it to your project's classpath or dependencies.

> *Maven/Gradle support is not available yet, but planned.*

---

## ⚠ Notes

* The library is in **active development**.
* Some effects may still produce artifacts (e.g., clicking) — these issues are being addressed.
* Contributions and feedback are welcome!

---

## ⚖ License

See [LICENSE](LICENSE) for licensing details.