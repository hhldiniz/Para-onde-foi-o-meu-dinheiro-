# Pra Onde Foi o Meu Dinheiro?

*("Where did my money go?")*

A Kotlin Multiplatform personal-finance app for **Android and iOS**. Import a CSV/ODS/PDF file of income and expenses — or a photo/screenshot of a statement, which the automatic importer reads on-device and maps to columns by itself — and the app stores the parsed entries in a local database and visualizes them with pie and line charts. An **AI assistant chat** answers questions about your own income, expenses and investments with a language model that runs on the device. There's no backend — everything runs on-device.

- **Language:** Kotlin Multiplatform · **UI:** Compose Multiplatform (Material 3), shared by both platforms
- **Persistence:** Room KMP with the bundled SQLite driver (local, unsynced)
- **DI:** Koin
- **Android:** minSdk 31 / targetSdk & compileSdk 37 · **iOS:** deployment target 16.6

## User guide

A screen-by-screen walkthrough of the app — onboarding, importing a spreadsheet, the Summary dashboard, the Entries list, adding movements by hand, and Settings — available in every language the app ships with:

- [🇧🇷 Português (padrão)](docs/guide/pt-BR.html)
- [🇺🇸 English](docs/guide/en-US.html)
- [🇪🇸 Español](docs/guide/es-ES.html)

Each is a standalone HTML file — download or clone the repo and open it directly in a browser, no server required.

## Building

Build tooling is the Gradle wrapper (`./gradlew`); there is no separate lint/format script configured.

```bash
# Build debug APK
./gradlew assembleDebug
```

```bash
# Run all JVM unit tests (this is what CI runs)
./gradlew testDebugUnitTest --continue
```

```bash
# Instrumented tests (require an emulator/device)
./gradlew connectedDebugAndroidTest
```

CI (`.github/workflows/ci.yml`) runs `testDebugUnitTest` on every push/PR to `master`/`main`.

### iOS

The iOS targets need a macOS host with Xcode; the Android target and the unit
tests build on any platform.

```bash
# Build the shared framework for the simulator
./gradlew :app:linkDebugFrameworkIosSimulatorArm64
```

Then open `iosApp/iosApp.xcodeproj` in Xcode and run. The project's first build
phase invokes `:app:embedAndSignAppleFrameworkForXcode`, so Xcode rebuilds the
Kotlin framework automatically.

## Architecture

Everything — business logic *and* the Compose UI — lives in
`app/src/commonMain/kotlin/com/hhldiniz/praondefoiomeudinheiro/`, with
`androidMain`/`iosMain` holding only the `actual` implementations of the few
platform capabilities the shared code declares as `expect`:

- **`data/local/`** — Room database, DAOs, and CSV/ODS/PDF parsing.
- **`data/repository/`** — imported-entries CRUD/aggregation and the spreadsheet import pipeline.
- **`data/vision/`** — the automatic importer: on-device text recognition → document layout analysis → column classification.
- **`data/chat/`** — the AI assistant: model lifecycle (download → load → generate), the prompt and the financial summary the model reads.
- **`domain/`** — plain models plus the `PlatformFile`/`PlatformFolder` abstraction over picked files.
- **`di/AppModule.kt`** — the shared Koin module (`platformModule` supplies the per-platform bindings).
- **`platform/`** — the `expect` declarations: clock/time zone, file pickers, currency formatting, preferences.
- **`presentation/`** — Compose screens grouped by feature (`landing`, `intro`, `home`, `addentry`, `settings`), each with a `XScreen.kt` + `XViewModel.kt` pair.

`iosApp/` holds the Xcode project; it is a thin SwiftUI shell around the shared
Compose UI.

## AI assistant

The assistant chat (the face icon on the Home screen's top bar) runs
**Qwen2.5 1.5B Instruct** (Q4_K_M GGUF, ~1.1 GB, Apache-2.0) entirely on the
device, through [llama.cpp](https://github.com/ggml-org/llama.cpp):
[Llamatik](https://github.com/ferranpons/Llamatik) on Android and iOS, and
[wllama](https://github.com/ngxson/wllama) (llama.cpp compiled to WebAssembly)
on the web. The model is downloaded once from
[Hugging Face](https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF), after
the user asks for it, and pinned to a fixed revision. Before each answer the
app summarizes the user's data (totals, months, categories, latest
transactions, investments) into the prompt; nothing the user writes or the
model answers leaves the device.

The model was chosen as the balance point for "most modern devices": ~1.5 GB
of free memory is enough, it answers at a readable pace on a mid-range phone,
it handles Portuguese, Spanish and English, and it downloads without a Hugging
Face account.

See [CLAUDE.md](CLAUDE.md) for the full architecture notes, spreadsheet import format, and Room schema details.
