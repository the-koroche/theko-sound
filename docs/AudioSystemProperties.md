# AudioSystemProperties - Specification

## 1. Structures & Classes

### ThreadType

**Values:**

* `virtual`
* `platform`

**Aliases:**

| Name     | Aliases |
| -------- | ------- |
| virtual  | `v`     |
| platform | `p`     |

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

### AudioResamplerConfig

**Format:**

```
<method>:<quality;optional>
```

**Fields:**

* **method** - class name of resample method
* **quality** - int > 0 (optional)

**Examples:**

```
-Dkey=LinearResampleMethod
-Dkey=LanczosResampleMethod:3
-Dkey=my.package.MyResampleMethod
```

---

## 2. Properties

---

## Audio Output Layer

| Property                                            | Type                 | Description                                   |
| --------------------------------------------------- | -------------------- | --------------------------------------------- |
| `org.theko.sound.outputLayer.thread`                | ThreadConfig         | Playback thread configuration                 |
| `org.theko.sound.outputLayer.timeout`               | int                  | Timeout (ms) for stopping the playback thread |
| `org.theko.sound.outputLayer.defaultBuffer`         | AudioMeasure         | Default buffer size                           |
| `org.theko.sound.outputLayer.resampler`             | AudioResamplerConfig | Resampler method and quality                  |
| `org.theko.sound.outputLayer.maxLengthMismatches`   | int ≥ 0              | Max ignored render-length mismatches          |
| `org.theko.sound.outputLayer.resetLengthMismatches` | boolean              | Reset mismatch counter after success          |
| `org.theko.sound.outputLayer.maxWriteErrors`        | int ≥ 0              | Max ignored write errors                      |
| `org.theko.sound.outputLayer.resetWriteErrors`      | boolean              | Reset write error counter after success       |
| `org.theko.sound.outputLayer.enableShutdownHook`    | boolean              | Enables/disables JVM shutdown hook            |

---

## Shared Resampler

| Property                           | Type                 | Description                    |
| ---------------------------------- | -------------------- | ------------------------------ |
| `org.theko.sound.resampler.shared` | AudioResamplerConfig | Shared resampler configuration |

---

## Audio Mixer

| Property                                        | Type    | Description              |
| ----------------------------------------------- | ------- | ------------------------ |
| `org.theko.sound.mixer.default.enableEffects`   | boolean | Enables effects globally |
| `org.theko.sound.mixer.default.swapChannels`    | boolean | Swaps stereo channels    |
| `org.theko.sound.mixer.default.reversePolarity` | boolean | Reverses polarity        |

---

## Codecs

### WAVE

| Property                                 | Type    | Description                    |
| ---------------------------------------- | ------- | ------------------------------ |
| `org.theko.sound.waveCodec.cleanTagText` | boolean | Clean tag text from LIST chunk |

---

## Miscellaneous

| Property                                | Type                     | Description                       |
| --------------------------------------- | ------------------------ | --------------------------------- |
| `org.theko.sound.automation.threads`    | int (≥1 & < CPU_CORES×4) | Number of automation/LFO threads  |
| `org.theko.sound.automation.updateTime` | int > 0                  | Automation update interval (ms)   |
| `org.theko.sound.cleaner.thread`        | ThreadConfig             | Thread config for cleaners        |
| `org.theko.sound.effects.resampler`     | AudioResamplerConfig     | Default ResamplerEffect resampler |