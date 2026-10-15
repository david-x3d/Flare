# Flare 🔥

Flare is a native Android speed test app (Kotlin, Jetpack Compose, Material 3 Expressive) that measures ping, jitter, download and upload against Cloudflare's speed test endpoints.

## Features

- **Ping & jitter** – 10 latency probes, ping is the median, jitter the standard deviation
- **Download & upload** – parallel connections, growing payloads (100 kB → 25 MB), robust 90th-percentile result
- **Live gauge** – animated wavy ring with the current speed, phase indicator (Ping → Download → Upload), abort at any time
- **History** – every test is stored locally (Room) with a chart of recent results; long-press to delete
- **Settings** – unit (Mbit/s or MB/s), test size (small/normal/large), theme (system/light/dark), dynamic color on/off
- **Material 3 Expressive** – expressive motion scheme, shape morphing, wavy progress indicators, button groups, split button, floating toolbar
- **Dynamic Color** (Material You, Android 12+) with a flame-orange fallback palette
- Connection type (Wi-Fi / mobile / ...) shown and stored with each result
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
