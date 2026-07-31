# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

"Pra onde foi o meu dinheiro" (Portuguese for "Where did my money go") is a Kotlin Multiplatform personal-finance app for Android and iOS. Users import a CSV/ODS/PDF file of income and expenses, and the app stores the parsed entries in a local Room database and visualizes them with pie/line charts. There is no backend — everything is on-device.

- Language: Kotlin Multiplatform, UI: Compose Multiplatform (Material 3) — the same composables render on both platforms
- Package/namespace: `com.hhldiniz.praondefoiomeudinheiro`
- Single Gradle module: `app` (applies both `com.android.application` and `org.jetbrains.kotlin.multiplatform`), plus the `iosApp/` Xcode project
- DI: Koin
- Persistence: Room KMP + `androidx.sqlite` bundled driver
- Android: minSdk 31 / targetSdk & compileSdk 37, Java/Kotlin target 11, AGP 9.3.1, Kotlin 2.4.0, Compose Multiplatform 1.11.1
- iOS: `iosX64`, `iosArm64`, `iosSimulatorArm64`, static framework `ComposeApp`, deployment target 15.0

## Source sets

```
app/src/commonMain/kotlin      all business logic AND the whole Compose UI
app/src/commonMain/composeResources/values*/strings.xml   localized strings (pt default, en-rUS, es-rES)
app/src/androidMain/kotlin     actuals + MainActivity + Application; AndroidManifest.xml and res/ live here too
app/src/iosMain/kotlin         actuals + MainViewController
app/src/androidUnitTest/kotlin JVM unit tests (JUnit4 + mockito-kotlin)
app/src/androidInstrumentedTest/kotlin  device tests
iosApp/                        SwiftUI shell + Xcode project
```

New code belongs in `commonMain` by default. Only drop to `androidMain`/`iosMain` when an API genuinely has no multiplatform equivalent, and then declare it as `expect` in `commonMain` first.

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

# Instrumented tests (require an emulator/device; live in app/src/androidInstrumentedTest)
./gradlew connectedDebugAndroidTest

# iOS framework — macOS host with Xcode only
./gradlew :app:linkDebugFrameworkIosSimulatorArm64
```

The iOS targets can only be *compiled* on a macOS host; everything else (including `testDebugUnitTest`) builds on any platform, which is why CI stays green on Linux runners.

`iosApp/iosApp.xcodeproj` runs `:app:embedAndSignAppleFrameworkForXcode` as its first build phase, so opening it in Xcode and pressing Run rebuilds the Kotlin framework automatically.

Unit test reports land in `app/build/reports/tests/testDebugUnitTest/` and JUnit XML in `app/build/test-results/testDebugUnitTest/`. CI (`.github/workflows/ci.yml`) runs `testDebugUnitTest` on every push/PR to `master`/`main` and publishes a pass/fail summary; there is no separate lint job. A second `instrumented-tests` job boots a cached AVD (API 34, `google_apis`, x86_64) via `reactivecircus/android-emulator-runner` and runs `connectedDebugAndroidTest`, publishing its own pass/fail summary from `app/build/outputs/androidTest-results/connected/`.

## Architecture

Standard layered structure under `app/src/commonMain/kotlin/com/hhldiniz/praondefoiomeudinheiro/`:

- **`platform/`** — the `expect` surface, i.e. everything the shared code cannot do by itself: `currentTimeMillis`, `timeZoneOffsetMillis`, `currentRegionCode`, `ioDispatcher` (there is no common `Dispatchers.IO`), `currencyFormatter`, and the `rememberSpreadsheetFilePicker` / `rememberSpreadsheetFolderPicker` composables. Add to this file rather than sprinkling `expect` declarations around.
- **`domain/file/`** — `PlatformFile` / `PlatformFolder`, the abstraction over a picked document that replaced `android.net.Uri` + `ContentResolver`. Android backs them with the Storage Access Framework, iOS with `UIDocumentPickerViewController` and security-scoped `NSURL`s. `InMemoryPlatformFile` exists for tests and previews.
- **`data/local/`** — Room `AppDatabase` (entities: `ImportedEntry`, `Category`), DAOs, and file parsing:
  - `CsvParser` / `OdsParser` / `PdfParser` turn raw file bytes into `List<List<String>>` rows; format is auto-detected by file extension in both `SpreadsheetFileValidator` and `FileSpreadsheetRepository` (`.ods` → OdsParser, `.pdf` → PdfParser, everything else treated as CSV).
  - `OdsParser` is fully common: `zip/ZipReader` walks the ZIP container and `xml/XmlPullReader` (a small namespace-aware pull parser) reads `content.xml`. Only raw DEFLATE is platform-specific (`expect fun inflateRaw` — `java.util.zip.Inflater` on Android, system zlib on iOS).
  - `PdfParser` splits extracted text into columns on runs of 2+ whitespace characters in common code; only `expect fun extractPdfText` differs (PDFBox-Android, which needs `PdfBoxInitializer.init(context)` once in `PraondefoiomeudinheiroApp.onCreate`; PDFKit on iOS, which needs no setup).
  - `SpreadsheetFileValidator` checks a `PlatformFile` before it's parsed. It reports failures as `UiText`, never as a resolved string, so it stays free of the resource loader and unit-testable.
  - `TransactionColumnMapper` locates the date/amount/description/category columns inside a header row by matching cell text against per-field synonym sets (Portuguese/English/Spanish + a few common variants, accent- and case-insensitive) — see "Spreadsheet import format" below. Both `SpreadsheetFileValidator` and `FileSpreadsheetRepository` share this instead of each hardcoding column positions/names.
  - `SelectedFilesHolder`, `CurrencyHolder`, `PatrimonyHolder`, `DataClearedHolder` are process-wide singleton `StateFlow` holders (not DI-managed) used to pass small bits of state across screens/lifecycle boundaries — e.g. `DataClearedHolder` tells screens to show zeroed data instead of stale/mocked values right after a "clear all data" action. `CurrencyHolder`/`PatrimonyHolder` persist through a `prefs.KeyValueStore` (SharedPreferences / NSUserDefaults) and take `init(store)` once from `AppInitializer`.
- **`data/repository/`** — `ImportRepository` (imported entries/categories CRUD + aggregation queries used by charts) and `FileSpreadsheetRepository` (implements `domain.repository.SpreadsheetRepository`, orchestrates validate → parse → map rows to spending/earnings entries).
- **`domain/`** — plain models/interfaces with no platform or Room dependency (`ValueRange`, `CurrencyOption`, `FileValidationReport`, `UiText`, `SpreadsheetRepository` interface).
- **`util/`** — `CivilDate.kt` holds the proleptic-Gregorian arithmetic that replaced `java.time`/`Calendar` (epoch millis ↔ local date, ISO week number, month subtraction, `dd/MM/yyyy` formatting), plus `TextNormalization.kt` for accent folding in place of `java.text.Normalizer`.
- **`di/AppModule.kt`** — shared Koin module wiring the database, DAOs, repositories, and ViewModels, plus `expect val platformModule` for the bindings that need platform APIs (currently `DatabaseBuilderFactory`). When adding a repository/ViewModel, register it in `appModule`.
- **`AppInitializer.kt` / `App.kt`** — the shared startup sequence and root composable. Android calls them from `PraondefoiomeudinheiroApp.onCreate` + `MainActivity`, iOS from `MainViewController()`.
- **`presentation/`** — Compose screens grouped by feature folder (`screen/landing`, `screen/intro`, `screen/home`, `screen/addentry`, `screen/settings`), each typically with a `XScreen.kt` (Composable) + `XViewModel.kt` (Koin-injected, exposes `StateFlow` UI state). `presentation/navigation/AppNavigation.kt` + `Screen.kt` define the NavHost.
- **`presentation/theme/`** — Material 3 theme plus a custom "neo-brutalist" component set (`NeoBrutalist.kt`: `HardShadowBox`, `NeoButton`, `NeoCard`, `NeoTag`) that gives the app its hard-shadow, high-contrast visual style. Prefer these components over raw Material widgets for buttons/cards/tags to keep the visual language consistent.

### Strings and resources

Strings live in `app/src/commonMain/composeResources/values*/strings.xml` and are reached through the generated `Res` class (`com.hhldiniz.praondefoiomeudinheiro.resources`), not Android's `R`. In composables use `stringResource(Res.string.x)`; outside composition use `getString(Res.string.x)` or, better, hand a `UiText` up to the UI and let it resolve. `app/src/androidMain/res/values/app_name.xml` keeps `app_name` only, because `AndroidManifest.xml` cannot read Compose resources.

Icons come from `org.jetbrains.compose.material:material-icons-core:1.7.3` — its final release, since that artifact is no longer republished alongside Compose Multiplatform. Every icon the app uses is in that core set; adding an icon outside it means vendoring the `ImageVector`.

### Navigation flow

`AppNavigation` picks the start destination at runtime: it queries `ImportRepository.count()` and routes to `Screen.IntroPatrimony` (first-run onboarding) if there are zero entries, otherwise `Screen.Home`. `AddEntryScreen` bumps a `refreshKey` int passed back into `HomeScreen` on `popBackStack` to force a data refresh, since there's no shared ViewModel between those screens.

### Entries paging

`androidx.paging` is Android-only, so the entries list pages itself: `HomeViewModel` exposes `entries` / `hasMoreEntries` `StateFlow`s and a `loadMoreEntries()` that reads the next `LIMIT`/`OFFSET` page straight from SQLite. `EntriesList` calls it when the last visible row comes within `ENTRIES_PREFETCH_DISTANCE` of the end. A generation counter discards a page that is still in flight when the filter changes.

### Spreadsheet import format

Column detection is name-based and position-independent, not tied to fixed indices or one language: `TransactionColumnMapper.findHeaderRowIndex` scans rows for the first one containing at least one recognizable date+amount pair, then `findColumnGroups` locates date/amount/description/category wherever they are in that row (description/category are optional and default to blank if absent). A header row can describe more than one side-by-side transaction table — the app's own export layout puts a spending table first and an earnings table after it — by having more than one recognizable date column; each date column starts a new group spanning up to the next one (or the row's end), and groups alternate spending/earnings by position (1st, 3rd, ... = expenses; 2nd, 4th, ... = earnings). Add new header-name variants to the synonym sets in `TransactionColumnMapper` rather than special-casing a language elsewhere.

### Room schema

`AppDatabase` is version 3 with `fallbackToDestructiveMigration(dropAllTables = true)` — schema changes do not need a migration path, the DB is just recreated (acceptable since this is unsynced local data). The `@Database` class lives in `commonMain` and is paired with `expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase>`, whose `actual` Room's KSP processor generates per target (hence the `kspAndroid` / `kspIos*` configurations in `app/build.gradle.kts`). `DatabaseBuilderFactory` supplies the platform's `RoomDatabase.Builder` (a `Context` on Android, the Documents directory on iOS) and `buildAppDatabase()` finishes the wiring in common code. On first creation the callback seeds a fixed set of Portuguese category names (Alimentacao, Transporte, Lazer, etc.). `ImportedEntry` has a unique composite index on `(date_millis, amount, description, category, is_expense)` so re-importing the same spreadsheet is a no-op — `ImportRepository.insertEntries` uses `OnConflictStrategy.IGNORE` and returns only the rows that were actually new.

## Testing conventions

- Unit tests (`app/src/androidUnitTest`) mirror the source package structure 1:1 and use JUnit4 + `mockito-kotlin` + `kotlinx-coroutines-test`; ViewModel tests typically pair with an `androidx-arch-core-testing` `InstantTaskExecutorRule`. They compile against the Android target, so they exercise the common code and its Android actuals together.
- Because the shared code no longer touches Android APIs, most tests need no framework doubles: `InMemoryPlatformFile` stands in for a picked document and `InMemoryKeyValueStore` for preferences.
- The common replacements for JVM libraries have their own tests — `util/CivilDateTest` (checked against `java.time`, which is still available in the JVM test run), `data/local/zip/ZipReaderTest` (archives produced by `java.util.zip`), and `data/local/xml/XmlPullReaderTest`.
- Instrumented tests (`app/src/androidInstrumentedTest`) are reserved for things that need a real Android environment: Room DB behavior (`AppDatabaseTest`) and PDFBox text extraction (`PdfParserTest`).
- No mocking framework is used for Room itself in unit tests — parser/repository logic is tested with plain Kotlin objects and in-memory data structures.
