# PWA / iOS feasibility spike (RESEARCH-1)

**Question:** MacTrack is Android-native (Kotlin + Jetpack Compose + Room, with a bundled SQLite
`cnf.db` asset). What's realistic for reaching **iOS** and the **web (PWA)**, and what does each path
cost / force on the current architecture? This is a recommendation, not an implementation.

**Short answer:**
- **iOS → Compose Multiplatform (CMP/KMP).** Share the domain, data, and most of the UI; ship a real
  native iOS app. This is the realistic path and reuses the most code.
- **Web/PWA → a thin web client over the eventual Supabase backend (ACCT-1), later.** CMP-for-web (wasm)
  technically works but the PWA/offline story is weaker than a purpose-built web app. Don't block on it.
- **A fresh Flutter/React-Native rewrite is not worth it** — it discards the Kotlin/Compose investment
  for no architectural gain.

---

## Why the current architecture is already half-way there

The app's non-UI layers are pure Kotlin with no Android coupling, so they're **free to share** as-is:

- `domain/calc/` (Mifflin–St Jeor, TDEE, macro targets) — pure Kotlin, already unit-tested.
- `data/food/FoodModels.kt` (Nutrients, PortionUnit, FoodDetail, all the mappers) — pure Kotlin.
- ViewModels + `StateFlow` + repository pattern + manual DI — all Kotlin, KMP-friendly.
- Compose UI (including the Canvas charts and `MarkdownText`) — Compose Multiplatform renders it on
  iOS and desktop; most screens port with little change.

## The Android-coupling points (the actual work)

Each of these needs an `expect`/`actual` split or a multiplatform replacement:

| Coupling | Today | KMP path |
|---|---|---|
| Database | Room (Android) | Room **KMP** (2.7+) with the bundled SQLite driver — supported |
| `cnf.db` asset (opened outside Room) | Android asset + SQLite | KMP SQLite driver (BundledSQLite / SQLDelight) reading a bundled resource |
| Prefs (Theme, AI settings, LogDate) | `SharedPreferences` | `multiplatform-settings` or DataStore-KMP |
| Notifications | `AlarmManager` + receiver | platform `expect`/`actual` (UNUserNotificationCenter on iOS) |
| File pickers (backup export/import) | SAF (`ActivityResultContracts`) | platform document pickers |
| Secret storage (AI key) | Android Keystore | platform crypto (iOS Keychain) via `expect`/`actual` |
| Barcode/OFF + AI HTTP | `HttpURLConnection` | swap to Ktor client (multiplatform) — small, and arguably worth doing on Android too |

None of these is exotic; each is a known KMP pattern. The migration is a **project restructure**
(`shared` / `androidApp` / `iosApp` modules) plus these platform shims — meaningful effort, but an order
of magnitude cheaper than a rewrite, and the domain/data/most-UI carry straight over.

## Web / PWA

- **CMP for web (Kotlin/Wasm)** can render the same Compose UI in a browser, but it's the least mature
  CMP target and produces a canvas-painted app, not an idiomatic, installable, offline-first PWA. Fine
  for a preview; not great as *the* web experience.
- **Better:** once ACCT-1 (Supabase) exists, a small purpose-built web client (Svelte/React + the
  Supabase JS SDK) gives a proper PWA — installable, offline-capable via service worker, reading the
  same Postgres. It shares no Kotlin code but shares the *data model* and backend. Only build this if
  web genuinely matters; it's independent of the iOS path.

## Recommendation & sequencing

1. **Don't restructure now.** Keep shipping the Android MVP; the shared layers are already
   KMP-shaped, so nothing today makes the future move harder. (Keeping `domain`/`data`/`calc` free of
   Android imports is the one discipline to maintain — it already is.)
2. **When iOS becomes a goal:** migrate to Compose Multiplatform — extract a `shared` module, move the
   Android-coupling points to `expect`/`actual`, get `cnf.db` reading via a KMP SQLite driver, and add
   an `iosApp`. Budget this as a focused multi-day restructure, not a rewrite.
3. **When web becomes a goal:** build a thin PWA over the Supabase backend (post-ACCT-1). Treat it as a
   separate client, not a CMP-wasm port.
4. **Never:** a Flutter/RN rewrite — it throws away working, tested Kotlin for no benefit here.

**One forcing function on today's code:** keep the domain/data/calc layers Android-free (no `Context`,
no `android.*` imports leaking in). They're clean now; a stray Android import there is the only thing
that would quietly raise the future porting cost.
