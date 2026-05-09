# AudioSystemProperties - Specification

## 1. Structures & Classes

### TimeMeasure

**Format:**

```
<time><unit>
```

**Fields:**

* **time** - `long` or `double`
* **unit** - `TimeUnit` with aliases

**Examples:**

```
-Dkey=1s
-Dkey=1.5s
-Dkey=100ms
-Dkey=1000nanos
-Dkey=0.6h
```

---

### ThreadConfig

**Format:**

```
<type;optional>:<priority;optional>
```

**Fields:**

* **type** - thread type (`ThreadType`), optional if priority specified
* **priority** - int `0–10`, optional if type specified

**Examples:**

```
-Dkey=platform:7        // Platform thread with priority 7
-Dkey=p:10              // Alias for "platform"
-Dkey=platform          // Default priority
-Dkey=virtual           // Default priority
-Dkey=v                 // Alias
-Dkey=4                 // Default thread type + priority 4
```

---

### AudioMeasure.Unit

**Values:**
`frames`, `samples`, `bytes`, `seconds`

**Aliases:**

| Unit    | Aliases              |
| ------- | -------------------- |
| frames  | `f`, `frms`, `frame` |
| samples | `smp`, `sample`      |
| bytes   | `b`, `byte`          |
| seconds | `s`, `sec`, `second` |

---

### AudioMeasure

**Format:**

```
<value><unit;optional>
```

**Fields:**

* **value** - `long` or `double`
* **unit** - default is `frames`

**Examples:**

```
-Dkey=512frms      // 512 frames
-Dkey=2048bytes    // 2048 bytes
-Dkey=0.52sec      // 0.52 seconds
-Dkey=1024smp      // 1024 samples
-Dkey=4096         // default unit = frames
-Dkey=2048b        // alias for bytes
```

---

### ResampleMethod

**Format:**

```
<method>
```

**Fields:**

* **method** - class name of resample method

**Examples:**

```
-Dkey=LinearResampleMethod
-Dkey=LanczosResampleMethod
-Dkey=my.package.MyResampleMethod
```

---

## 2. Properties

---

## Audio Backends

| Property                                            | Type                 | Default | Description                                   |
| --------------------------------------------------- | -------------------- | ------- | --------------------------------------------- |
| `org.theko.sound.backends.requireDuplexSelect`      | boolean              | false   | Autoselect backend with both IO support       |

---

## Audio Output Layer

| Property                                               | Type                 | Default     | Description                                   |
| ------------------------------------------------------ | -------------------- | ----------- | --------------------------------------------- |
| `org.theko.sound.outputLayer.thread`                   | ThreadConfig         | Platform:7  | Playback thread configuration                 |
| `org.theko.sound.outputLayer.timeout`                  | TimeMeasure          | 1000 ms     | Timeout for stopping the playback thread      |
| `org.theko.sound.outputLayer.defaultBuffer`            | AudioMeasure         | 2048 frames | Default buffer size                           |
| `org.theko.sound.outputLayer.resampler`                | ResampleMethod       | linear      | Resampler method used for output              |
| `org.theko.sound.outputLayer.maxLengthMismatches`      | int ≥ 0              | 10          | Max ignored render-length mismatches          |
| `org.theko.sound.outputLayer.resetLengthMismatches`    | boolean              | true        | Reset mismatch counter after success          |
| `org.theko.sound.outputLayer.maxWriteErrors`           | int ≥ 0              | 10          | Max ignored write errors                      |
| `org.theko.sound.outputLayer.resetWriteErrors`         | boolean              | true        | Reset write error counter after success       |
| `org.theko.sound.outputLayer.ignorePlaybackExceptions` | boolean              | false       | Ignore exceptions occured in playback thread  |
| `org.theko.sound.outputLayer.enableShutdownHook`       | boolean              | true        | Enables/disables JVM shutdown hook            |

---

## Shared Resampler

| Property                           | Type                 | Default | Description                    |
| ---------------------------------- | -------------------- | ------- | ------------------------------ |
| `org.theko.sound.resampler.shared` | ResampleMethod       | linear  | Shared resampler method        |

---

## Audio Mixer

| Property                                        | Type    | Default | Description              |
| ----------------------------------------------- | ------- | ------- | ------------------------ |
| `org.theko.sound.mixer.default.enableEffects`   | boolean | true    | Enables effects globally |
| `org.theko.sound.mixer.default.swapChannels`    | boolean | false   | Swaps stereo channels    |
| `org.theko.sound.mixer.default.reversePolarity` | boolean | false   | Reverses polarity        |

---

## Codecs

### WAVE

| Property                                 | Type    | Default | Description                    |
| ---------------------------------------- | ------- | ------- | ------------------------------ |
| `org.theko.sound.waveCodec.cleanTagText` | boolean | true    | Clean tag text from LIST chunk |

---

## Miscellaneous

| Property                                               | Type                     | Default   | Description                                 |
| ------------------------------------------------------ | ------------------------ | --------- | ------------------------------------------- |
| `org.theko.sound.automation.threads`                   | int (≥1 & < CPU_CORES×4) | CPU_CORES | Number of automation/LFO threads            |
| `org.theko.sound.automation.updateTime`                | int > 0                  | 15 ms     | Automation update interval (ms)             |
| `org.theko.sound.automation.threadPoolShutdownTimeout` | TimeMeasure              | 5 sec     | Shutdown timeout for automation thread pool |
| `org.theko.sound.cleaner.thread`                       | ThreadConfig             | Virtual:1 | Thread config for cleaners                  |
| `org.theko.sound.effects.resampler`                    | ResampleMethod           | linear    | Default ResamplerEffect resampler           |