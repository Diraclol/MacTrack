# MacTrack

A local-first Android calorie and macro tracker. Log what you eat, track calories and macros, and
keep your data on your own device. Offline by default, single user, no account, no tracking.

Package: `com.dirac.mactrack` · Status: active development (pre-1.0).

## Screenshots

<p align="center">
  <img src="docs/screenshots/dashboard.png" width="220" alt="Dashboard">
  <img src="docs/screenshots/food-log.png" width="220" alt="Food log">
  <img src="docs/screenshots/food-detail.png" width="220" alt="Food detail">
  <img src="docs/screenshots/search.png" width="220" alt="Food search">
</p>

<p align="center"><em>Dashboard &middot; Food log &middot; Food detail &middot; Search</em></p>

<!-- Drop the PNGs in docs/screenshots/ with the filenames referenced above (and splash.png). -->


## What it does

- **Food logging** — a day view grouped by time, with per-day calorie and macro totals, macro rings
  and coloured pills, and a scrollable day strip.
- **Food search across three sources** — a bundled **Canadian Nutrient File** (CNF) of common whole
  foods (offline), your own **saved custom foods**, and **Open Food Facts** for branded/day-to-day
  products by name or barcode (online, with a graceful offline fallback).
- **Barcode scanning** — on-device camera scanning (offline, no API key), with a gallery-import option
  and a scan button on the food editor. A scanned code checks your saved foods first, then Open Food
  Facts.
- **Custom foods, recipes, and meals** — create and edit your own foods; build recipes and saved meals
  from ingredients and log them in one tap.
- **Serving sizes** — every food offers sensible portion units (grams, ounces, natural servings), with
  a "favourite serving units" setting to pin the ones you use most.
- **Goals and onboarding** — a stepped setup (sex, age, height, weight, activity) that estimates your
  daily energy needs and sets calorie and macro targets for your goal (lose / maintain / gain).
- **Trends** — calorie, macro, and micronutrient charts over selectable time ranges, a food-log
  calendar with a logging streak, and a weight-trend graph.
- **Micronutrients** — tracks sodium, potassium, fibre, and caffeine alongside the core macros.
- **AI assistant (optional)** — a chat tab, powered by your own Gemini API key, that estimates a
  food's macros from a description or photo and can turn an ingredient list into a saved recipe or
  meal. Fully optional; the app works without it. See "Gemini API key" below.
- **Backup** — export all your data to a JSON file you control, and import it back on any device.
- **Personalisation** — light / dark / system theme, a choice of start screen, an optional daily log
  reminder, and a profile with an emoji or photo avatar.

## Written in

Kotlin, with a Jetpack Compose UI.

## Formatted for

Android phones. `minSdk 26` (Android 8.0) and up; targets Android 16 (`targetSdk 36`).

## Tools used

Android Studio, Gradle (Kotlin DSL), Jetpack Compose, Room (via KSP), CameraX, and ML Kit.

## Android dependencies

- **Jetpack Compose** (BoM) — Material 3, Activity Compose, UI + tooling, Material Icons Extended
- **AndroidX Lifecycle** — runtime, `viewmodel-compose`, `runtime-compose`
- **Navigation Compose** — single-NavHost navigation
- **Room** — `room-runtime`, `room-ktx`, `room-compiler` (KSP)
- **Kotlin Coroutines** (`kotlinx-coroutines-android`)
- **AndroidX Core-KTX** and **Core-SplashScreen**
- **CameraX** — `camera-core`, `camera-camera2`, `camera-lifecycle`, `camera-view`, `camera-mlkit-vision`
- **ML Kit** — `barcode-scanning` (bundled, on-device, offline, no API key)

## Data sources

- **Canadian Nutrient File (CNF)** — shipped as a bundled read-only SQLite asset, so common-food
  lookups work fully offline.
- **Open Food Facts** — queried online for branded and barcoded products; when there is no network,
  search falls back to CNF and your saved foods.

## Gemini API key (optional)

The AI assistant is off by default and requires **your own** Google AI Studio (Gemini) API key, which
is free to create. You enter it in the app's AI settings; it is stored **encrypted on-device** (Android
Keystore) and is **never embedded in the app or committed to this repository**. Without a key, the app
is fully functional apart from the AI tab. Only the messages you send to the assistant are sent to
Google; nothing else leaves the device on your behalf.

## Privacy

MacTrack is local-first: your foods, logs, goals, weight, and profile live in an on-device database.
There is no account, no server, and no analytics or tracking. The only network calls are Open Food
Facts product lookups and — if you opt in with a key — the AI assistant. Backups are a plain JSON file
you export and keep yourself. See [SECURITY.md](docs/SECURITY.md) for the security model.

## Architecture (one paragraph)

Single-activity Jetpack Compose app. Room is the source of truth; screens observe repository `Flow`s
through `ViewModel` `StateFlow`s and are wired through one `NavHost`. Dependencies are provided by hand
(no DI framework). The app is offline-first, and the domain / data / calculation layers are kept free
of Android-specific types so they can be shared with other platforms later.

## Building

Open the project in Android Studio and run the `app` configuration, or from the command line:

```bash
./gradlew :app:assembleDebug     # build a debug APK
./gradlew :app:testDebugUnitTest # run the unit tests
./gradlew :app:installDebug      # install on a connected device/emulator
```

## Licence

Released under the [MIT License](LICENSE) — free to use, modify, and distribute with attribution.
