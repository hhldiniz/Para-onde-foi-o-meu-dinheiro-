# Pra Onde Foi o Meu Dinheiro?

*("Where did my money go?")*

A native Android personal-finance app. Import a CSV/ODS/PDF file of income and expenses, and the app stores the parsed entries in a local database and visualizes them with pie and line charts. There's no backend — everything runs on-device.

- **Language:** Kotlin · **UI:** Jetpack Compose (Material 3)
- **Persistence:** Room (local, unsynced)
- **DI:** Koin
- **minSdk** 31 / **targetSdk & compileSdk** 37

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

## Architecture

Standard layered structure under `app/src/main/java/com/hhldiniz/praondefoiomeudinheiro/`:

- **`data/local/`** — Room database, DAOs, and CSV/ODS/PDF parsing.
- **`data/repository/`** — imported-entries CRUD/aggregation and the spreadsheet import pipeline.
- **`domain/`** — plain models with no Android/Room dependency.
- **`di/AppModule.kt`** — the single Koin module.
- **`presentation/`** — Compose screens grouped by feature (`landing`, `intro`, `home`, `addentry`, `settings`), each with a `XScreen.kt` + `XViewModel.kt` pair.

See [CLAUDE.md](CLAUDE.md) for the full architecture notes, spreadsheet import format, and Room schema details.
