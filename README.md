# Flare 🔥

**English** · [Deutsch](#deutsch)

Flare is a native Android speed test app (Kotlin, Jetpack Compose, Material 3 Expressive) that measures ping, jitter, download and upload against Cloudflare's speed test endpoints.

<!-- Add screenshots to docs/screenshots and uncomment:
<p>
  <img src="docs/screenshots/test.png" width="30%" alt="Test" />
  <img src="docs/screenshots/history.png" width="30%" alt="History" />
  <img src="docs/screenshots/settings.png" width="30%" alt="Settings" />
</p>
-->

> 📸 *Screenshots: placeholder, drop images into [`docs/screenshots`](docs/screenshots).*

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
- German and English

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

---

<a id="deutsch"></a>

# Flare 🔥 (Deutsch)

Flare ist eine native Android-Speedtest-App (Kotlin, Jetpack Compose, Material 3 Expressive), die Ping, Jitter, Download und Upload gegen die Speedtest-Endpunkte von Cloudflare misst.

> 📸 *Screenshots: Platzhalter, Bilder einfach in [`docs/screenshots`](docs/screenshots) ablegen.*

## Funktionen

- **Ping & Jitter** – 10 Latenzmessungen, Ping ist der Median, Jitter die Standardabweichung
- **Download & Upload** – parallele Verbindungen, steigende Größen (100 kB → 25 MB), robustes Ergebnis über das 90. Perzentil
- **Live-Gauge** – animierter Wavy-Ring mit der aktuellen Geschwindigkeit, Phasenanzeige (Ping → Download → Upload), jederzeit abbrechbar
- **Verlauf** – jeder Test wird lokal gespeichert (Room), mit Diagramm der letzten Ergebnisse; Lang-Drücken zum Löschen
- **Einstellungen** – Einheit (Mbit/s oder MB/s), Testgröße (klein/normal/groß), Theme (System/Hell/Dunkel), Dynamic Color an/aus
- **Material 3 Expressive** – expressives Motion-Schema, Shape Morphing, Wavy-Fortschrittsanzeigen, Button Groups, Split Button, Floating Toolbar
- **Dynamic Color** (Material You, ab Android 12) mit warmer Flammen-Palette als Fallback
- Verbindungstyp (WLAN / Mobilfunk / ...) wird angezeigt und mit jedem Ergebnis gespeichert
- Edge-to-Edge, Predictive Back, adaptive Layouts für Tablets und Foldables (Navigation Rail, Zwei-Spalten-Ansichten)
- Deutsch und Englisch

## Tech-Stack

Kotlin · Jetpack Compose · Material 3 Expressive (`androidx.compose.material3` 1.5.0-beta01, siehe Hinweis) · MVVM mit `ViewModel` + `StateFlow` · Navigation Compose · OkHttp · Room · DataStore · Coroutines/Flow · Gradle Version Catalog · minSdk 26, Target-/Compile-SDK 37

> **Hinweis zur Material-3-Version:** Die neueste *stabile* Version (1.4.0) enthält zwar Expressive-Theme und Motion-Schema, aber **nicht** die expressiven Komponenten (`LoadingIndicator`, Wavy-Fortschrittsanzeigen, `ButtonGroup`, `MaterialShapes`, `FloatingToolbar`, ...). Diese gehören zur 1.5.0-Linie, daher nutzt Flare die neueste 1.5.0 (derzeit Beta) mit `ExperimentalMaterial3ExpressiveApi`. Sobald 1.5.0 stable erscheint, sollte darauf gewechselt werden.

## So funktioniert die Messung

Alles läuft gegen `https://speed.cloudflare.com`:

| Phase | Anfrage | Ergebnis |
|---|---|---|
| Latenz | 10 × `GET /__down?bytes=0` (plus ein Warm-up), Zeit bis zu den Response-Headern | Median = Ping, Standardabweichung = Jitter |
| Download | Runden mit 3–4 parallelen `GET /__down?bytes=N` über HTTP/1.1, N = 100 kB, 1 MB, 10 MB, 25 MB, gestreamt | `Bytes × 8 / Sekunden / 1e6`, 90. Perzentil über alle Runden |
| Upload | Runden mit parallelen `POST /__up` mit Zufallsdaten, gleiche Größen | gleich |

Größere Schritte werden bei langsamen Anschlüssen übersprungen, und jede Phase hat ein hartes Zeitbudget, sodass ein Test nie ausufert. Die Basis-URL ist eine einzige Konstante ([`CloudflareEndpoints.BASE_URL`](app/src/main/java/dev/flare/speedtest/data/network/CloudflareEndpoints.kt)); ändert Cloudflare die Endpunkte, zeigt Flare eine verständliche Fehlermeldung statt abzustürzen.

> ⚠️ Flare steht **in keiner Verbindung zu Cloudflare**. Die Messendpunkte sind nicht offiziell als stabile API dokumentiert und können sich jederzeit ändern oder verschwinden.

## Build

**Voraussetzungen:** JDK 17+ (21 empfohlen) und das Android SDK mit Plattform 37 (`sdkmanager "platforms;android-37.0" "build-tools;37.0.0"`). Das SDK über `ANDROID_HOME` oder eine `local.properties` mit `sdk.dir=/pfad/zum/sdk` angeben (wird nicht committet).

```bash
git clone https://github.com/david-x3d/flare.git
cd flare
./gradlew assembleDebug     # Debug-APK bauen
./gradlew test              # Unit-Tests ausführen
```

Die APK liegt danach unter `app/build/outputs/apk/debug/app-debug.apk`. Installation auf einem angeschlossenen Gerät oder Emulator:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Die Tests umfassen reine Unit-Tests (Median, Jitter, Mbit/s, Perzentil), die Engine gegen einen lokalen `MockWebServer`, ViewModel-Tests und headless gerenderte Compose-Screens (Robolectric), die Screenshots nach `app/build/screenshots` schreiben.

## Projektstruktur

```
app/src/main/java/dev/flare/speedtest/
├── domain/   Modelle, Statistik (Median, Jitter, Mbit/s), Testplan, Repository-/Engine-Interfaces
├── data/     network (Cloudflare-Engine, Konstanten, Netzwerk-Monitor), db (Room), settings (DataStore), repo
└── ui/       theme, components, test, history, settings, navigation
```

## Lizenz

[MIT](LICENSE)
