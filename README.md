# Flare 🔥

Flare is a native Android speed test app (Kotlin, Jetpack Compose, Material 3 Expressive) that measures ping, jitter, download and upload against Cloudflare's speed test endpoints.

## Features

- **Ping & jitter** – 10 latency probes, ping is the median, jitter the standard deviation
- **Download & upload** – parallel connections, growing payloads (100 kB → 25 MB), robust 90th-percentile result
- **Live gauge** – speed dial with the current speed, phase track (Ping → Download → Upload) and a live throughput chart, abort at any time
- **Connection info** – provider (ISP), Cloudflare server location and network type, stored with every result
- **Quality at a glance** – 0–100 score, bufferbloat grade from loaded latency, and use-case verdicts (video calls, streaming, gaming)
- **History** – local Room storage with network and period filters, trend chart, swipe to delete with undo, and a detail sheet per test
- **Settings** – unit (Mbit/s or MB/s), default mode, test size, theme (system/light/dark), dynamic color, haptics, screen wake, history on/off
- **Design** – navy and flame-orange look with a floating pill navigation bar; Material 3 Expressive motion and shapes; optional Dynamic Color (Android 12+)
- **Launcher shortcuts** – long-press the icon to start a test or open History
- **Haptics** – subtle feedback on phase changes and results (can be turned off)
- Edge-to-edge, predictive back, adaptive layouts for tablets and foldables (navigation rail, two-pane screens)
- UI available in English and German

## Tech stack

Kotlin · Jetpack Compose · Material 3 Expressive (`androidx.compose.material3` 1.5.0-beta01, see note) · MVVM with `ViewModel` + `StateFlow` · Navigation Compose · OkHttp · Room · DataStore · Coroutines/Flow · Gradle version catalog · minSdk 26, target/compile SDK 37

> **Note on the Material 3 version:** the latest *stable* release (1.4.0) ships the expressive theme and motion scheme but **not** the expressive components (`LoadingIndicator`, wavy progress indicators, `ButtonGroup`, `MaterialShapes`, `FloatingToolbar`, ...). They are part of the 1.5.0 line, so Flare uses the newest 1.5.0 release (currently a beta) and opts in with `ExperimentalMaterial3ExpressiveApi`. Move to the stable 1.5.0 as soon as it is released.

## How the measurement works

Everything runs against `https://speed.cloudflare.com`:

| Phase | Request | Result |
|---|---|---|
| Latency | 10 × `GET /__down?bytes=0` (plus one warm-up), time to response headers | median = ping, standard deviation = jitter |
| Download | rounds of 3–4 parallel `GET /__down?bytes=N` over HTTP/1.1, N = 100 kB, 1 MB, 10 MB, 25 MB, streamed | `bytes × 8 / seconds / 1e6`, 90th percentile over all rounds |
| Upload | rounds of parallel `POST /__up` with random data, same sizes | same |

Bigger steps are skipped on slow links and every phase has a hard time budget, so a test never runs away. The base URL is a single constant ([`CloudflareEndpoints.BASE_URL`](app/src/main/java/dev/flare/speedtest/data/network/CloudflareEndpoints.kt)); if Cloudflare changes the endpoints, Flare shows a clear error message instead of crashing.

> ⚠️ Flare is **not affiliated with Cloudflare**. The measurement endpoints are not officially documented as a stable API and may change or disappear at any time.

## Quick and Full modes

Choose Quick for a short connection check, with a small, normal or large data budget. The default mode, screen wake behavior and local result storage are configurable in Settings.

Full follows the supplied Cloudflare export sequence using single-connection transfers in this order:

| Direction | Payload | Transfers |
|---|---:|---:|
| Download | 100 kB | 10 |
| Download | 1 MB | 8 |
| Upload | 100 kB | 8 |
| Upload | 1 MB | 6 |
| Download | 10 MB | 6 |
| Upload | 10 MB | 4 |
| Download | 25 MB | 4 |
| Download | 100 MB | 3 |

This is 49 transfers and 515.8 MB of planned payload, plus protocol overhead. Latency probes run alongside transfers. Full has a 180-second transfer budget and a 20-second limit per transfer; timed-out, failed and skipped rounds are explicitly labeled. Transfer speeds include HTTP request time, so Flare results are not guaranteed to match the Cloudflare website's calculations.

The step list shows live progress, a throughput chart and expandable per-transfer timing, response size, server timing when provided, and median loaded latency. Details remain available in History and can be exported with the reference CSV columns. History also includes network filters, aggregate statistics and a separate summary CSV export. Upgrading preserves existing saved tests.

Brand assets are in [`docs/branding`](docs/branding); the adaptive launcher, themed icon and app UI use the supplied Flare logo.

## Build

**Requirements:** JDK 17+ (21 recommended) and the Android SDK with platform 37 (`sdkmanager "platforms;android-37.0" "build-tools;37.0.0"`). Point Gradle at the SDK via `ANDROID_HOME` or a `local.properties` file with `sdk.dir=/path/to/sdk` (not committed).

```bash
git clone https://github.com/david-x3d/flare.git
cd flare
./gradlew assembleDebug     # build the debug APK
./gradlew test              # run the unit tests
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`. Install it on a connected device or emulator:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The tests include pure unit tests (median, jitter, Mbit/s, percentile), the engine against a local `MockWebServer`, ViewModel tests and headless Compose renderings (Robolectric) that write screenshots to `app/build/screenshots`.

## Releases / Signing

Release builds are signed only if signing values are available; without them `assembleRelease` still works and produces an unsigned APK. Never commit the keystore or its passwords.

Create `keystore.properties` in the project root (it is git-ignored):

```properties
storeFile=/path/to/your-release.jks
storePassword=...
keyAlias=...
keyPassword=...
```

Alternatively set the environment variables `FLARE_STORE_FILE`, `FLARE_STORE_PASSWORD`, `FLARE_KEY_ALIAS` and `FLARE_KEY_PASSWORD`. Then run `./gradlew assembleRelease` and verify with `apksigner verify --verbose --print-certs app/build/outputs/apk/release/app-release.apk`.

Keep your keystore backed up: updates of an installed app must be signed with the same key.

## Project structure

```
app/src/main/java/dev/flare/speedtest/
├── domain/   models, statistics (median, jitter, Mbit/s), test plan, repository/engine interfaces
├── data/
│   ├── network/   Cloudflare engine (OkHttp), endpoint constants, ConnectivityManager monitor
│   ├── db/        Room database for the history
│   ├── settings/  DataStore settings
│   └── repo/      repository implementations
└── ui/
    ├── theme/       Material 3 Expressive theme, dynamic color + fallback palette
    ├── components/  shape morphing, formatting, network chip
    ├── test/        gauge, phase stepper, result cards, controls, test screen + ViewModel
    ├── history/     history screen, chart + ViewModel
    ├── settings/    settings screen + ViewModel
    └── navigation/  floating toolbar / navigation rail, NavHost
```

## License

[MIT](LICENSE)
