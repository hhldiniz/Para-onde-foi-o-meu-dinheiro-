# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

"Pra onde foi o meu dinheiro" (Portuguese for "Where did my money go") is a native Android personal-finance app. Users import a CSV/ODS spreadsheet of income and expenses, and the app stores the parsed entries in a local Room database and visualizes them with pie/line charts. There is no backend — everything is on-device.

- Language: Kotlin, UI: Jetpack Compose (Material 3)
- Package/namespace: `com.hhldiniz.praondefoiomeudinheiro`
- Single Gradle module: `app`
- DI: Koin
- Persistence: Room
- minSdk 31 / targetSdk & compileSdk 37, Java/Kotlin target 11, AGP 9.2.1, Kotlin 2.4.0

## Commands

Build tooling is the Gradle wrapper (`./gradlew`); there is no separate lint/format script configured.

```bash
# Build debug APK
./gradlew assembleDebug

# Run all JVM unit tests (this is what CI runs)
./gradlew testDebugUnitTest --continue

# Run a single unit test class
./gradlew testDebugUnitTest --tests "com.hhldiniz.praondefoiomeudinheiro.data.local.CsvParserTest"

# Run a single test method
./gradlew testDebugUnitTest --tests "com.hhldiniz.praondefoiomeudinheiro.presentation.screen.home.HomeViewModelTest.methodName"

# Instrumented tests (require an emulator/device; live in app/src/androidTest)
./gradlew connectedDebugAndroidTest
```

Unit test reports land in `app/build/reports/tests/testDebugUnitTest/` and JUnit XML in `app/build/test-results/testDebugUnitTest/`. CI (`.github/workflows/ci.yml`) runs `testDebugUnitTest` on every push/PR to `master`/`main` and publishes a pass/fail summary; there is no separate lint or instrumented-test job.

## Architecture

Standard layered structure under `app/src/main/java/com/hhldiniz/praondefoiomeudinheiro/`:

- **`data/local/`** — Room `AppDatabase` (entities: `ImportedEntry`, `Category`), DAOs, and file parsing:
  - `CsvParser` / `OdsParser` turn a raw file `InputStream` into `List<List<String>>` rows; format is auto-detected in `FileSpreadsheetRepository` by file extension (`.ods` vs everything else treated as CSV).
  - `SpreadsheetFileValidator` checks a `Uri` before it's parsed.
  - `CsvUriHolder`, `CurrencyHolder`, `DataClearedHolder` are process-wide singleton `StateFlow` holders (not DI-managed) used to pass small bits of state across screens/lifecycle boundaries — e.g. `DataClearedHolder` tells screens to show zeroed data instead of stale/mocked values right after a "clear all data" action, `CurrencyHolder` persists the selected currency to `SharedPreferences` and must have `init(context)` called once (`PraondefoiomeudinheiroApp.onCreate`) before use.
- **`data/repository/`** — `ImportRepository` (imported entries/categories CRUD + aggregation queries used by charts) and `FileSpreadsheetRepository` (implements `domain.repository.SpreadsheetRepository`, orchestrates validate → parse → map rows to spending/earnings entries).
- **`domain/`** — plain models/interfaces with no Android or Room dependency (`ValueRange`, `CurrencyOption`, `FileValidationReport`, `SpreadsheetRepository` interface).
- **`di/AppModule.kt`** — single Koin module wiring the database, DAOs, repositories, and ViewModels. Started in `PraondefoiomeudinheiroApp.onCreate`. When adding a repository/ViewModel, register it here.
- **`presentation/`** — Compose screens grouped by feature folder (`screen/landing`, `screen/home`, `screen/addentry`, `screen/settings`), each typically with a `XScreen.kt` (Composable) + `XViewModel.kt` (Koin-injected, exposes `StateFlow` UI state). `presentation/navigation/AppNavigation.kt` + `Screen.kt` define the single-Activity NavHost.
- **`presentation/theme/`** — Material 3 theme plus a custom "neo-brutalist" component set (`NeoBrutalist.kt`: `HardShadowBox`, `NeoButton`, `NeoCard`, `NeoTag`) that gives the app its hard-shadow, high-contrast visual style. Prefer these components over raw Material widgets for buttons/cards/tags to keep the visual language consistent.

### Navigation flow

`AppNavigation` picks the start destination at runtime: it queries `ImportRepository.count()` and routes to `Screen.Landing` (first-run import flow) if there are zero entries, otherwise `Screen.Home`. `AddEntryScreen` bumps a `refreshKey` int passed back into `HomeScreen` on `popBackStack` to force a data refresh, since there's no shared ViewModel between those screens.

### Spreadsheet import format

Imported files are expected to have two side-by-side tables in one sheet: a spending table (columns index 1-4: Data, Valor, Descrição, Categoria) and an earnings table (columns index 6-9, same layout). `FileSpreadsheetRepository.findHeaderRowIndex` locates the header row by matching those exact (case-insensitive) Portuguese column names in both column groups before slicing out data rows.

### Room schema

`AppDatabase` is version 2 with `fallbackToDestructiveMigration(true)` — schema changes do not need a migration path, the DB is just recreated (acceptable since this is unsynced local data). On first creation, `categoryDao().insertAll(defaultCategories())` seeds a fixed set of Portuguese category names (Alimentacao, Transporte, Lazer, etc.) via `AppDatabase.Callback.onCreate`. `ImportedEntry` has a unique composite index on `(date_millis, amount, description, category, is_expense)` so re-importing the same spreadsheet is a no-op — `ImportRepository.insertEntries` uses `OnConflictStrategy.IGNORE` and returns only the rows that were actually new.

## Testing conventions

- Unit tests (`app/src/test`) mirror the main source package structure 1:1 and use JUnit4 + `mockito-kotlin` + `kotlinx-coroutines-test`; ViewModel tests typically pair with an `androidx-arch-core-testing` `InstantTaskExecutorRule`.
- Instrumented tests (`app/src/androidTest`) are reserved for things that need a real Android environment: Room DB behavior (`AppDatabaseTest`), content-resolver/`Uri` handling (`CsvUriHolderTest`), and Compose UI (`FileValidationReportTest`).
- No mocking framework is used for Room itself in unit tests — parser/repository logic is tested with plain Kotlin objects and in-memory data structures.
