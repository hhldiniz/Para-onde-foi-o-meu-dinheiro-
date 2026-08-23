# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

"Pra onde foi o meu dinheiro" (Portuguese for "Where did my money go") is a Kotlin Multiplatform personal-finance app for Android, iOS, and the web (Kotlin/Wasm, deployed to GitHub Pages). Users import a CSV/ODS/PDF file of income and expenses — or a photo/screenshot of a statement, or a photo of a receipt ("nota fiscal"), via the automatic computer-vision importer (see "Automatic import" below) — and the app stores the parsed entries locally and visualizes them with pie/line charts. A separate tab tracks the user's investment positions, typed in by hand (see "Investments tab" below). There is no backend — everything is on-device (Room on Android/iOS, `localStorage` on web; text recognition uses each platform's on-device engine).

- Language: Kotlin Multiplatform, UI: Compose Multiplatform (Material 3) — the same composables render on all three platforms
- Package/namespace: `com.hhldiniz.praondefoiomeudinheiro`
- Single Gradle module: `app` (applies both `com.android.application` and `org.jetbrains.kotlin.multiplatform`), plus the `iosApp/` Xcode project
- DI: Koin
- Persistence: Room KMP + `androidx.sqlite` bundled driver on Android/iOS; hand-written `localStorage`-backed DAOs on wasmJs (Room has no wasmJs target — see "Room schema" below)
- Android: minSdk 31 / targetSdk & compileSdk 37, Java/Kotlin target 11, AGP 9.3.1, Kotlin 2.4.0, Compose Multiplatform 1.11.1
- iOS: `iosArm64` + `iosSimulatorArm64`, static framework `ComposeApp`, deployment target 15.0 (no `iosX64` — Compose Multiplatform no longer publishes for the Intel simulator)
- Web: `wasmJs` (Kotlin/Wasm), deployed as a static site to GitHub Pages by `.github/workflows/deploy-pages.yml` on every push to the default branch. It ships as an installable PWA (manifest + service worker) that works fully offline, and PDF import and text recognition go through JS libraries vendored into the site rather than a platform engine (see "Web target" below).

## Source sets

```
app/src/commonMain/kotlin      all business logic AND the whole Compose UI, Room-free
app/src/commonMain/composeResources/values*/strings.xml   localized strings (pt default, en-rUS, es-rES)
app/src/roomMain/kotlin        Room @Entity/@Dao classes + AppDatabase; androidMain/iosMain depend on this, wasmJs does not
app/src/androidMain/kotlin     actuals + MainActivity + Application; AndroidManifest.xml and res/ live here too
app/src/iosMain/kotlin         actuals + MainViewController
app/src/wasmJsMain/kotlin      actuals + browser entry point (main.kt) + localStorage-backed DAOs (data/local/web/)
app/src/wasmJsMain/resources/index.html  minimal HTML shell loading the compiled JS bundle
app/src/wasmJsMain/resources/pdf-extract.js / ocr-extract.js  lazy bridges to the vendored pdf.js / Tesseract.js
app/src/wasmJsMain/resources/vendor/       pdf.js, Tesseract.js, its OCR engine and language data, served from the site itself
app/src/wasmJsMain/resources/manifest.webmanifest / sw.js / pwa.js / icon*.png  PWA manifest, service worker and icons
app/src/androidUnitTest/kotlin JVM unit tests (JUnit4 + mockito-kotlin)
app/src/androidInstrumentedTest/kotlin  device tests
iosApp/                        SwiftUI shell + Xcode project
```

New code belongs in `commonMain` by default. Only drop to `androidMain`/`iosMain`/`wasmJsMain` when an API genuinely has no multiplatform equivalent, and then declare it as `expect` in `commonMain` first. Code that touches Room (entities, DAOs, `AppDatabase`) belongs in `roomMain` instead of `commonMain` — see "Room schema".

## Commands

Build tooling is the Gradle wrapper (`./gradlew`); there is no separate lint/format script configured.

```bash
# Build debug APK
./gradlew assembleDebug

# Signed release APK — app/build/outputs/apk/release/ (see "Release signing")
./gradlew assembleRelease

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

# Web (wasmJs) production build — static site under app/build/dist/wasmJs/productionExecutable/
./gradlew :app:wasmJsBrowserDistribution

# Web dev server with hot reload, for local iteration
./gradlew :app:wasmJsBrowserDevelopmentRun
```

The iOS targets can only be *compiled* on a macOS host; everything else (including `testDebugUnitTest` and the wasmJs build) builds on any platform, which is why CI stays green on Linux runners.

`iosApp/iosApp.xcodeproj` runs `:app:embedAndSignAppleFrameworkForXcode` as its first build phase, so opening it in Xcode and pressing Run rebuilds the Kotlin framework automatically.

Unit test reports land in `app/build/reports/tests/testDebugUnitTest/` and JUnit XML in `app/build/test-results/testDebugUnitTest/`. CI (`.github/workflows/ci.yml`) runs `testDebugUnitTest` on every push/PR to `master`/`main` and publishes a pass/fail summary; there is no separate lint job. A `wasm-build` job runs `wasmJsBrowserDistribution` as a compile check so a broken web build fails CI instead of surfacing only on deploy. A third `instrumented-tests` job boots a cached AVD (API 34, `google_apis`, x86_64) via `reactivecircus/android-emulator-runner` and runs `connectedDebugAndroidTest`, publishing its own pass/fail summary from `app/build/outputs/androidTest-results/connected/`.

`.github/workflows/deploy-pages.yml` runs `wasmJsBrowserDistribution` and publishes `app/build/dist/wasmJs/productionExecutable/` to GitHub Pages via `actions/upload-pages-artifact` + `actions/deploy-pages`, on every push to `master`/`main` (and manually via `workflow_dispatch`). The repo's Settings → Pages → Source must be switched to "GitHub Actions" once for this to actually publish; that's a manual step this workflow file cannot perform.

### Release signing

The app is not distributed through Play, so the release build is signed with a **self-signed certificate**. `app/build.gradle.kts` reads four settings — `storeFile`, `storePassword`, `keyAlias`, `keyPassword` — from the environment (`RELEASE_KEYSTORE_FILE`, `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`) or, failing that, from an untracked `keystore.properties` at the repository root, both through the `providers` API so the configuration cache treats them as build inputs. `scripts/generate-release-keystore.sh` is the one place that defines what "the certificate" is (PKCS12, RSA 4096, SHA256withRSA, 30 years), and both CI and a developer's machine call it.

`.github/workflows/release.yml` runs `assembleRelease` on pull requests, on `v*` tags and on demand, resolving a key in that order of preference:

1. **Repository secrets** — `RELEASE_KEYSTORE_BASE64` (a `base64 -w0` of the keystore) plus `RELEASE_KEYSTORE_PASSWORD` and `RELEASE_KEY_ALIAS`. The same certificate every time, which is what makes one build installable over another.
2. **A key generated for that job**, with a random password masked out of the log. Nothing to set up, but every run signs with a different certificate — Android treats that as a different app, so a tester has to uninstall before installing the next build. The job summary says which of the two signed the APK.

The job then proves the APK really is signed (`apksigner verify --print-certs`, whose output goes into the job summary), uploads it as a workflow artifact, and for a tag attaches it to that tag's GitHub release.

Nothing signing-related is committed: `*.jks`, `*.keystore` and `keystore.properties` are gitignored, and with no keystore configured the signing config is not declared at all — `assembleRelease` then produces `app-release-unsigned.apk` rather than failing, so checking that the release variant compiles needs no key.

## Architecture

Standard layered structure under `app/src/commonMain/kotlin/com/hhldiniz/praondefoiomeudinheiro/`:

- **`platform/`** — the `expect` surface, i.e. everything the shared code cannot do by itself: `currentTimeMillis`, `timeZoneOffsetMillis`, `currentRegionCode`, `ioDispatcher` (there is no common `Dispatchers.IO`; wasmJs uses `Dispatchers.Default`, same reasoning as iOS), `currencyFormatter`, the `rememberSpreadsheetFilePicker` / `rememberSpreadsheetFolderPicker` / `rememberImportSourcePicker` / `rememberReceiptPicker` composables, `recognizeDocumentText` (`TextRecognizer.kt`, see "Automatic import"), and `rememberAppInstaller` (`AppInstall.kt`, the in-app install button — real only on wasmJs, a constant "no" on Android/iOS, see "Web target"). Add to this file rather than sprinkling `expect` declarations around.
- **`domain/file/`** — `PlatformFile` / `PlatformFolder`, the abstraction over a picked document that replaced `android.net.Uri` + `ContentResolver`. Android backs them with the Storage Access Framework, iOS with `UIDocumentPickerViewController` and security-scoped `NSURL`s, wasmJs with a hidden `<input type="file">` (`webkitdirectory` for the folder picker) reading bytes via the File API's `arrayBuffer()`. `InMemoryPlatformFile` exists for tests and previews.
- **`data/local/`** (commonMain, Room-free) — `ImportedEntry` / `Category` / `Investment` (plain `@Serializable` data classes) and the `ImportedEntryDao` / `CategoryDao` / `InvestmentDao` interfaces they're read through, plus file parsing:
  - `CsvParser` / `OdsParser` / `PdfParser` turn raw file bytes into `List<List<String>>` rows; format is auto-detected by file extension in both `SpreadsheetFileValidator` and `FileSpreadsheetRepository` (`.ods` → OdsParser, `.pdf` → PdfParser, everything else treated as CSV).
  - `OdsParser` is fully common: `zip/ZipReader` walks the ZIP container and `xml/XmlPullReader` (a small namespace-aware pull parser) reads `content.xml`. Only raw DEFLATE is platform-specific (`expect fun inflateRaw` — `java.util.zip.Inflater` on Android, system zlib on iOS, and a pure-Kotlin RFC 1951 decoder — `zip/RawInflate.kt`, also commonMain — on wasmJs, since browsers only expose `DecompressionStream`, which is Promise-based and doesn't fit this synchronous call site). `RawInflate` is cross-checked against `java.util.zip.Deflater` output in `RawInflateTest` (JVM, `androidUnitTest`) even though only wasmJs calls it in production.
  - `PdfParser` splits extracted text into columns on runs of 2+ whitespace characters in common code; only `expect fun extractPdfText` differs (PDFBox-Android, which needs `PdfBoxInitializer.init(context)` once in `PraondefoiomeudinheiroApp.onCreate`; PDFKit on iOS, which needs no setup; pdf.js on wasmJs — see "Web target" below). `PdfTextLayout` rebuilds that whitespace padding from a PDF engine's raw positioned runs; only wasmJs needs it (PDFBox and PDFKit pad their own output), but it lives in commonMain so the JVM tests reach it, the same arrangement `zip/RawInflate.kt` has.
  - `TransactionValueParser` parses dates and monetary amounts without assuming a format (day-first `dd/mm/yyyy` and ISO, named months in pt/en/es, `1.234,56` and `1,234.56`, currency symbols, parentheses/trailing-minus negatives) and answers the inverse question — `looksLikeDate` / `looksLikeMoney` / `looksLikeNumber` — which is how the automatic importer classifies columns. Both import paths go through it (`HomeViewModel` delegates to it), so they agree on what a date and an amount are.
  - `SpreadsheetFileValidator` checks a `PlatformFile` before it's parsed. It reports failures as `UiText`, never as a resolved string, so it stays free of the resource loader and unit-testable.
  - `TransactionColumnMapper` locates the date/amount/description/category columns inside a header row by matching cell text against per-field synonym sets (Portuguese/English/Spanish + a few common variants, accent- and case-insensitive) — see "Spreadsheet import format" below. Both `SpreadsheetFileValidator` and `FileSpreadsheetRepository` share this instead of each hardcoding column positions/names.
  - `SelectedFilesHolder`, `CurrencyHolder`, `PatrimonyHolder`, `DataClearedHolder` are process-wide singleton `StateFlow` holders (not DI-managed) used to pass small bits of state across screens/lifecycle boundaries — e.g. `DataClearedHolder` tells screens to show zeroed data instead of stale/mocked values right after a "clear all data" action (see "Navigation flow" for the rest of what that action resets). `CurrencyHolder`/`PatrimonyHolder` persist through a `prefs.KeyValueStore` (SharedPreferences / NSUserDefaults / `localStorage`) and take `init(store)` once from `AppInitializer`.
- **`data/repository/`** — `ImportRepository` (imported entries/categories CRUD + aggregation queries used by charts) and `FileSpreadsheetRepository` (implements `domain.repository.SpreadsheetRepository`, orchestrates validate → parse → map rows to spending/earnings entries). Both take the plain `ImportedEntryDao`/`CategoryDao` interfaces and are unaffected by which platform backs them.
- **`data/vision/`** — the automatic importer (see "Automatic import" below): `DocumentLayoutAnalyzer` (words + boxes → lines, and lines → table), `TransactionFieldClassifier` (table → column roles → transactions), `ReceiptAnalyzer` (lines → one purchase: merchant, date, items, total), `SmartImportAnalyzer` (both pipelines, injectable recognizer for tests).
- **`domain/vision/`** — its plain models: `BoundingBox` / `RecognizedWord` / `RecognizedDocument` (the recognizer contract), `TransactionField` / `FieldMapping` / `DetectedTransaction` / `SmartImportAnalysis` (the statement output) and `ReceiptItem` / `DetectedReceipt` / `ReceiptAnalysis` (the receipt output).
- **`domain/`** — plain models/interfaces with no platform or Room dependency (`ValueRange`, `CurrencyOption`, `FileValidationReport`, `UiText`, `SpreadsheetRepository` interface).
- **`util/`** — `CivilDate.kt` holds the proleptic-Gregorian arithmetic that replaced `java.time`/`Calendar` (epoch millis ↔ local date, ISO week number, month subtraction, `dd/MM/yyyy` formatting), plus `TextNormalization.kt` for accent folding in place of `java.text.Normalizer`.
- **`di/AppModule.kt`** — shared Koin module wiring repositories and ViewModels, plus `expect val platformModule` for the bindings that need platform APIs. `platformModule` also supplies the `ImportedEntryDao`/`CategoryDao` singletons on every platform (Room-backed on Android/iOS, `localStorage`-backed on wasmJs — see "Room schema"), since `appModule` itself must stay Room-free. When adding a repository/ViewModel, register it in `appModule`.
- **`AppInitializer.kt` / `App.kt`** — the shared startup sequence and root composable. Android calls them from `PraondefoiomeudinheiroApp.onCreate` + `MainActivity`, iOS from `MainViewController()`, wasmJs from `main.kt`'s `ComposeViewport`.
- **`presentation/`** — Compose screens grouped by feature folder (`screen/landing`, `screen/intro`, `screen/home`, `screen/addentry`, `screen/investments`, `screen/settings`, `screen/smartimport`), each typically with a `XScreen.kt` (Composable) + `XViewModel.kt` (Koin-injected, exposes `StateFlow` UI state). `presentation/navigation/AppNavigation.kt` + `Screen.kt` define the NavHost.
- **`presentation/theme/`** — Material 3 theme plus a custom "neo-brutalist" component set (`NeoBrutalist.kt`: `HardShadowBox`, `NeoButton`, `NeoCard`, `NeoTag`) that gives the app its hard-shadow, high-contrast visual style. Prefer these components over raw Material widgets for buttons/cards/tags to keep the visual language consistent.

### Strings and resources

Strings live in `app/src/commonMain/composeResources/values*/strings.xml` and are reached through the generated `Res` class (`com.hhldiniz.praondefoiomeudinheiro.resources`), not Android's `R`. In composables use `stringResource(Res.string.x)`; outside composition use `getString(Res.string.x)` or, better, hand a `UiText` up to the UI and let it resolve. `app/src/androidMain/res/values/app_name.xml` keeps `app_name` only, because `AndroidManifest.xml` cannot read Compose resources.

Icons come from `org.jetbrains.compose.material:material-icons-core:1.7.3` — its final release, since that artifact is no longer republished alongside Compose Multiplatform. Every icon the app uses is in that core set; adding an icon outside it means vendoring the `ImageVector`.

### Navigation flow

`AppNavigation` picks the start destination at runtime: it routes to `Screen.IntroPatrimony` (first-run onboarding) unless `ImportRepository.count()` is above zero or `OnboardingHolder.completed` is set, otherwise `Screen.Home`. The flag is what keeps a user who finished onboarding but hasn't added an entry yet from being sent back through it on every restart. `AddEntryScreen` bumps a `refreshKey` int passed back into `HomeScreen` on `popBackStack` to force a data refresh, since there's no shared ViewModel between those screens.

**Clearing all data restarts onboarding.** `data/repository/AppDataReset.kt`'s `clearAllAppData` is the whole action behind the settings screen's "apagar todos os dados": it drops entries, categories and investment positions, empties `SelectedFilesHolder`, zeroes `PatrimonyHolder`, clears `OnboardingHolder` and marks `DataClearedHolder`. Both halves of the start-destination check above therefore go back to their first-run values, so the next launch lands on onboarding on its own; the settings screen additionally navigates to `Screen.IntroPatrimony` popping the whole back stack, so the current session goes there immediately instead of returning to a Home screen with nothing in it.

### Entries paging

`androidx.paging` is Android-only, so the entries list pages itself: `HomeViewModel` exposes `entries` / `hasMoreEntries` `StateFlow`s and a `loadMoreEntries()` that reads the next `LIMIT`/`OFFSET` page straight from SQLite. `EntriesList` calls it when the last visible row comes within `ENTRIES_PREFETCH_DISTANCE` of the end. A generation counter discards a page that is still in flight when the filter changes.

### Investments tab

The Home screen's bottom bar has three tabs, all rendered by `HomeScreen` itself rather than by the NavHost: summary, entries and investments (`HomeTab` in `HomeUiState.kt` picks between them). The third one is `presentation/screen/investments/InvestmentsScreen.kt`, which is why that screen carries no `Scaffold` or top bar of its own and adds a position through an in-content button — Home's import FAB belongs to the summary tab.

The portfolio is **manual only**: nothing quotes a market price, so a position is worth whatever the user last typed into its "valor atual", and `Investment.profit` / `profitPercent` are derived from that against what was invested. Adding a live quote provider later means filling `currentValue` from it; nothing else in the model has to change.

`InvestmentType` (`domain/model/`) is the list of products offered — Brazilian retail plus the international ones (Tesouro Direto, CDB, LCI/LCA, CRI/CRA, debêntures, fundos, ações, FIIs, ETFs, BDRs, cripto, câmbio, previdência, imóveis, ...) — each carrying a stable `key` and an `InvestmentClass` that only groups the picker. **The `key` is what gets persisted** (the `type` column, the JSON value on wasmJs), so an entry may be renamed or reordered but its key may not change; `fromKey` falls back to `OTHER` so a row written by a newer version still loads. Display names live in `presentation/components/InvestmentTypeLocalization.kt`, the same split `localizedCategoryName` uses for the seeded spending categories.

`InvestmentsViewModel` collects the DAO's Flow, so an insert/update/delete re-emits and the totals and per-type allocation are recomputed from that same emission — there is no refresh handshake like the one Home needs after `AddEntry`. The add/edit form lives in the UI state (`InvestmentFormState`) rather than in `remember`, so a rotation does not drop what is being typed, and its amounts are read by `TransactionValueParser.parseAmount`, which is what lets `1.234,56` and `1234.56` both work. The allocation chart reuses `SpendingPieChart` by handing it already-localized type names as its labels.

### Spreadsheet import format

Column detection is name-based and position-independent, not tied to fixed indices or one language: `TransactionColumnMapper.findHeaderRowIndex` scans rows for the first one containing at least one recognizable date+amount pair, then `findColumnGroups` locates date/amount/description/category wherever they are in that row (description/category are optional and default to blank if absent). A header row can describe more than one side-by-side transaction table — the app's own export layout puts a spending table first and an earnings table after it — by having more than one recognizable date column; each date column starts a new group spanning up to the next one (or the row's end), and groups alternate spending/earnings by position (1st, 3rd, ... = expenses; 2nd, 4th, ... = earnings). Add new header-name variants to the synonym sets in `TransactionColumnMapper` rather than special-casing a language elsewhere.

### Automatic import (computer vision)

The direct import above requires the file to carry a recognizable header. The
*automatic* importer does not require anything: it takes a photo, a screenshot
or a CSV/ODS and works out for itself which column is the date, the amount, the
description and the category. Both paths stay available; the Home `+` menu
offers "Importar arquivo"/"Importar pasta" (direct, exact) and "Importação
inteligente" (automatic, reviewable).

The automatic screen offers two readings, chosen by the user rather than
guessed, because they are different problems:

- **A statement** — many transactions laid out as a table — goes through
  `SmartImportAnalyzer.analyze` and the four stages below.
- **A receipt** ("nota fiscal", "cupom fiscal", NFC-e) — one purchase, a
  merchant, a list of items and a total — goes through
  `SmartImportAnalyzer.analyzeReceipt` and `ReceiptAnalyzer`; see "Receipt
  reading" below.

**PDFs are not read by either.** A PDF already carries its text as text, so the
direct import reads it exactly instead of guessing at a reconstructed layout;
handing one to the automatic importer fails with
`UnsupportedImportSourceException`, which the UI turns into a message pointing
at "Importar arquivo". `SmartImportSource` therefore has no `PDF` entry and
`sourceOf` returns null for one.

The statement pipeline is `SmartImportAnalyzer` (`data/vision/`), and every
stage of it is common code except text recognition:

1. **Recognition** — `expect suspend fun recognizeDocumentText` (`platform/TextRecognizer.kt`)
   returns words with boxes. ML Kit's bundled Latin recognizer on Android
   (`com.google.mlkit:text-recognition`, model in the APK), the Vision framework
   on iOS (`VNRecognizeTextRequest`, plus `boundingBoxForRange` to get word-level
   boxes out of Vision's line-level observations), Tesseract.js on wasmJs
   (`ocr-extract.js`, lazily `import()`ed from `vendor/` on the first image picked,
   exactly like `pdf-extract.js` does for pdf.js). Every actual normalizes boxes
   into a top-left `0f..1f` space — Vision's are bottom-left, ML Kit's and
   Tesseract's are pixels — so the shared code never sees a platform quirk.
   Recognition is on-device on all three platforms; nothing is uploaded.
2. **Layout analysis** — `DocumentLayoutAnalyzer` turns those loose words into a
   `List<List<String>>` grid: words are swept into lines by vertical centre,
   glued into cells while the horizontal gap stays small, and the cells' extents
   are merged into bands whose whitespace corridors become the column
   boundaries (a projection-profile analysis, which is what makes left-aligned
   descriptions and right-aligned amounts land in the right columns). All
   thresholds are multiples of the page's median word height, so they hold at
   any resolution.
3. **Classification** — `TransactionFieldClassifier` scores each column for each
   role from a fixed feature vector (share of cells parsing as dates/money/text,
   wordiness, repetition, header-name match) through a linear model whose
   weights live in `score()`; roles are then assigned *jointly*, picking the
   combination with the highest total rather than letting each role grab its
   favourite column. Date and amount are required, description/category are left
   blank when nothing scores well enough. It also detects a debit/credit column
   pair and a direction column ("D"/"C", "entrada"/"saída"), falling back to the
   amount's sign and finally to "expense". **Tuning belongs in those weights**,
   not in per-bank special cases elsewhere.
4. **Review** — `SmartImportViewModel` / `SmartImportScreen` show the detected
   column mapping with confidences and let the user drop rows or flip a row
   between expense and income before anything is written. Nothing reaches the
   database until the user confirms; insertion then reuses `ImportRepository`,
   so the same duplicate-ignoring behaviour applies.

Because stage 2's output is the same `List<List<String>>` the CSV/ODS parsers
produce, a spreadsheet takes the identical route from stage 3 onward — which is
what lets the automatic path import a file whose layout the direct path would
reject.

### Receipt reading

`ReceiptAnalyzer` (`data/vision/`) reads a photographed receipt into a single
`DetectedReceipt`. It shares stage 1 (the same `recognizeDocumentText`) and the
*first* pass of stage 2 — `DocumentLayoutAnalyzer.lines`, which stops at lines
of cells — and then goes its own way, because a receipt is not a table: its item
lines have no date column and its amounts all belong to one purchase, so the
column classifier makes nonsense of them.

What it reads off those lines, all of it label-driven and store-agnostic:

- **The total** is the line carrying the strongest "total" label
  (`STRONG_TOTAL_LABELS` beats `WEAK_TOTAL_LABELS`, later lines beat earlier
  ones), with its value taken from the next line when the receipt right-aligns
  it onto its own. `NEVER_TOTAL` is checked first, which is what keeps
  "Subtotal", "Qtd. total de itens", taxes, payment and change out of it. With
  no label anywhere the largest remaining amount stands in and
  `totalWasLabelled` is false, which costs confidence and shows a warning.
- **The items** are the priced lines above the total, with the `2 UN x 12,50`
  tail matched as a shape (unit price, multiplier, unit, quantity — right to
  left, each optional) rather than by eating every trailing number, so
  "ARROZ TIPO 1" keeps its name and reports a quantity of 2.
- **The merchant** is the first header line that is neither an address, a
  document number nor boilerplate (`LINE_NOISE`); the **date** is the one next
  to an "emissão"/"data" label, or the first printed, falling back to today; the
  **CNPJ** is the digit run on the "CNPJ" line.
- **The category** is guessed from `CATEGORY_KEYWORDS` over the merchant (worth
  3) and the item descriptions (worth 1), and is blank when nothing matches.
  Add keywords there rather than special-casing a store.

`SmartImportViewModel` proposes the total as one expense; `onItemizedToggled`
swaps it for one candidate per item (the receipt's date, category and
confidence carry over). From there it is the same review and the same
`ImportRepository` insertion as the statement path.

### Room schema

Room 2.7.1 (the version this project is pinned to) has no wasmJs/js target — only the breaking-change Room 3.0 alpha line adds that, and it needs a hand-rolled Web Worker + OPFS setup that isn't worth the Android/iOS migration risk. So all Room-touching code lives in the `roomMain` intermediate source set (`dependsOn(commonMain)`, depended on by `androidMain` and `iosMain` only — see `app/build.gradle.kts`) instead of `commonMain`:

- `data/local/entity/ImportedEntryRecord.kt` / `CategoryRecord.kt` / `InvestmentRecord.kt` are the `@Entity`-annotated persistence records (same table names, columns, and indices the old commonMain entities had), with `toRecord()`/`toDomain()` mappers to/from the plain `ImportedEntry`/`Category` in `commonMain`.
- `data/local/dao/RoomImportedEntryDao.kt` / `RoomCategoryDao.kt` / `RoomInvestmentDao.kt` are `@Dao` interfaces that extend the commonMain `ImportedEntryDao`/`CategoryDao`/`InvestmentDao`, implementing them via default methods that map Record ↔ domain entity around Room-generated `@Query`/`@Insert` methods (e.g. `override suspend fun insertAll(entries: List<ImportedEntry>) = insertAllRecords(entries.map { it.toRecord() })`).
- `data/local/AppDatabase.kt` (the `@Database` abstract class, `AppDatabaseConstructor` expect/actual, `buildAppDatabase()`, `DatabaseBuilderFactory` expect) is otherwise unchanged from before the wasmJs work, just relocated. It's version 4 (bumped by the `investments` table) with `fallbackToDestructiveMigration(dropAllTables = true)` — schema changes do not need a migration path, the DB is just recreated (acceptable since this is unsynced local data, though it does mean an Android/iOS user's imported entries are dropped on the upgrade that adds a table; wasmJs, having no Room, keeps its `localStorage` data). Room's KSP processor generates `AppDatabaseConstructor`'s `actual` per target (hence `kspAndroid` / `kspIos*` in `app/build.gradle.kts`; KSP needs no separate wiring for `roomMain` itself, since it processes each compilation's full merged source set graph, and `androidMain`/`iosMain` already `dependsOn(roomMain)`). `DatabaseBuilderFactory` supplies the platform's `RoomDatabase.Builder` (a `Context` on Android, the Documents directory on iOS) and `buildAppDatabase()` finishes the wiring. On first creation the callback seeds a fixed set of Portuguese category names (Alimentacao, Transporte, Lazer, etc.) — see "Web target" below for how wasmJs, which has no such callback, ends up with the same seed data anyway.
- `di/AppModule.android.kt` / `AppModule.ios.kt` build the `AppDatabase` and bind `single<ImportedEntryDao> { db.importedEntryDao() }` / `single<CategoryDao> { db.categoryDao() }` / `single<InvestmentDao> { db.investmentDao() }` off it.

`ImportedEntryRecord` has a unique composite index on `(date_millis, amount, description, category, is_expense)` so re-importing the same spreadsheet is a no-op — Room's `RoomImportedEntryDao.insertAllRecords` uses `OnConflictStrategy.IGNORE`, `ImportRepository.insertEntries` filters out entries whose returned row ID is `-1L`, and `data/local/web/WebImportedEntryDao` on wasmJs reimplements the same dedupe-and-return-`-1L` semantics by hand (including within a single insert batch, matching how Room's sequential per-row insert would behave).

### Web target

`wasmJs { browser(); binaries.executable() }` in `app/build.gradle.kts` builds the web target; `app/src/wasmJsMain/kotlin/.../main.kt` is the entry point (`AppInitializer.init()` then `ComposeViewport(document.body!!) { App() }`), and `app/src/wasmJsMain/resources/index.html` is the hand-written HTML shell that loads the compiled JS bundle (`praondefoiomeudinheiro.js`, set via `moduleName`/`outputFileName`).

Persistence has no Room involved at all: `data/local/web/WebImportedEntryDao.kt` / `WebCategoryDao.kt` / `WebInvestmentDao.kt` implement the commonMain `ImportedEntryDao`/`CategoryDao`/`InvestmentDao` interfaces directly, keeping the full entry/category list as a `MutableStateFlow` in memory and re-serializing it to `localStorage` (via `kotlinx.serialization.json.Json`, one `localStorage` key per table) on every mutation — fine at the scale of an imported spreadsheet. Every method re-implements the equivalent Room query's exact filter/sort/paging semantics in plain Kotlin over the in-memory list (see `data/local/dao/ImportedEntryDao.kt`'s KDoc for what each one must do). Because there's no Room database-creation callback to seed default categories on wasmJs, that seeding instead happens implicitly: `IntroCategoriesViewModel.onContinue()` always inserts every still-selected default category name regardless of platform (a conflict-ignore no-op on Android/iOS, where it's already seeded; the actual seeding step on wasmJs).

Other wasmJs actuals worth knowing about: `platform/Platform.wasmJs.kt` and `CurrencyFormatter.wasmJs.kt` use small `@JsFun` snippets (`Date.now()`, `Intl.DateTimeFormat`/`navigator.language`, `Intl.NumberFormat`) rather than `kotlinx-browser` typed wrappers, since those APIs aren't DOM-shaped; `data/local/prefs/KeyValueStore.wasmJs.kt` is a thin `localStorage` wrapper namespacing keys as `"$name.$key"`, mirroring the separate SharedPreferences files / NSUserDefaults suites on the other platforms.

**PDF import on the web build** goes through Mozilla's pdf.js instead of a native engine (there is none available to Kotlin/Wasm). `data/local/PdfParser.kt`'s `expect fun extractPdfText` is `suspend` specifically for this actual: `data/local/PdfParser.wasmJs.kt` base64-encodes the bytes (no direct JS representation for `ByteArray` from a plain `js(...)` snippet — the same round trip `FilePicker.wasmJs.kt` uses in reverse), calls `window.praOndeExtractPdfRuns`, and awaits the returned `Promise` via `kotlinx.coroutines.await`. That `window` function is defined by `app/src/wasmJsMain/resources/pdf-extract.js`, loaded as a `<script type="module">` in `index.html`; it lazily `import()`s pdf.js out of `vendor/pdfjs/` only on the first PDF actually picked, so the ~1.7MB library never touches the app's own JS bundle or a user who never imports a PDF.

pdf.js's `getTextContent()` returns positioned glyph runs rather than the whitespace-padded text PDFBox (`sortByPosition = true`) and PDFKit's `.string` produce. The bridge hands those runs over as JSON (`{pages: [{runs: [{text, x, y, width}]}]}`) and stops there; `PdfTextLayout` in commonMain turns them back into padded text, so the reconstruction is covered by `testDebugUnitTest` rather than living untested in browser-only code. Two details in it are load-bearing and were each a bug once. First, pdf.js *does* signal inter-run gaps, as a run whose text is a single space and whose width spans the entire gap — blank runs are dropped before measuring, because keeping them makes every measured gap zero and collapses each table row into a single column (a PDF then imports as nothing at all, on the direct and automatic paths alike). Second, the gap thresholds are multiples of the line's own average character width, not absolute PDF points, so a 7pt statement and a 14pt one split at the same place. `SpreadsheetFileValidator.validate` still catches any parsing exception (a corrupt file, a pdf.js load failure) and reports `error_cannot_read_file`.

**The web build is a PWA**: it can be installed to a phone's home screen or a desktop launcher and starts with the network off. Three static files in `app/src/wasmJsMain/resources/` do it, all copied verbatim into the distribution and all addressing each other relatively, because GitHub Pages serves the site from a repository subdirectory rather than the origin's root:

- `manifest.webmanifest` — identity and install metadata (`display: standalone`, the theme's `#EAB308`/`#FFF5E6` for the splash screen, `start_url`/`scope`/`id` of `./`). Its icons are `icon.svg` plus PNGs rendered from the same artwork as the Android adaptive icon: `icon-192/512.png` zoomed 1.4x for contexts that never mask, and `icon-maskable-192/512.png` at the adaptive icon's own framing, whose 66/108 safe zone is exactly what a circle/squircle mask needs. `index.html` additionally links `apple-touch-icon.png` and `favicon-32.png`, which Safari's "Add to Home Screen" and browser tabs read instead of the manifest.
- `sw.js` — the service worker. It precaches only the shell (the page, the manifest, the icons) and runtime-caches everything else same-origin stale-while-revalidate, because webpack content-hashes the wasm binary's filename and Compose fetches its resources lazily by name: a hardcoded precache list would be a lie and a build-time generated one would be a build step. Navigations are network-first so a deploy lands on the next launch, falling back to the cached shell offline. Cross-origin requests are passed straight through, which now means nothing at runtime: the import engines are vendored (see below), so the app makes no third-party request at all. What those engines do cost is size, so they are not in the install precache — `pwa.js` asks the worker, once the app is up and idle, to pull `OFFLINE_LIBRARIES` (~13MB) into the same cache, skipping the whole thing when `navigator.connection` reports a metered or 2G link.
- `pwa.js` — registers the worker after `load`. Not a module and not dependent on the wasm bundle, since a cached shell has to survive the bundle failing to load. Loading any page with `?sw=off` unregisters the worker and drops its caches, which is how to iterate with `wasmJsBrowserDevelopmentRun` without a worker from a previous run serving the previous build. It also owns the **install button**: Chromium fires `beforeinstallprompt` once, early, while the wasm bundle is still downloading, and an unclaimed prompt is gone — so `pwa.js` catches it, holds it, and exposes `window.praOndeCanInstall()` / `window.praOndeInstall()` plus a `praonde:installavailability` DOM event when either answer changes. `platform/AppInstall.wasmJs.kt` wraps that as the `AppInstaller` the shared UI sees, as one long-lived object (a Kotlin lambda has no stable identity on the JS side, so a listener registered per composition could not be removed again). `SettingsScreen` renders its "install the app" section only while `canInstall` is true, which is never on Android/iOS (`UnavailableAppInstaller`) and only until the app is installed on the web — and never in Safari, which installs solely through its own share menu.

`index.html` also paints an `#app-loading` placeholder (the app's hard-shadow square) while the bundle downloads, and `main.kt` removes it as its first act — an installed launch goes from the system splash screen straight into the page, where a blank body reads as a broken app.

**The import engines are vendored**, under `app/src/wasmJsMain/resources/vendor/` (pdf.js, Tesseract.js, its wasm OCR core and the pt/en/es language models — see `vendor/README.md` for versions, provenance and what was deliberately left out). They used to come from jsDelivr and `tessdata.projectnaptha.com`, which the service worker could not cache — a cross-origin response it is not allowed to read is one it cannot store — so "offline" stopped at the app's own UI. Serving them from the site makes them ordinary same-origin assets, and removes the app's last third-party request. Three things follow from vendoring rather than bundling:

- They stay **lazily loaded**: `pdf-extract.js` / `ocr-extract.js` still only `import()` on the first PDF or photo picked, and resolve their paths from `import.meta.url` (not the page URL), so a deep link or a subdirectory deploy cannot break them. Tesseract's `corePath` and `langPath` are handed to a Web Worker, which resolves relative URLs against *itself* — hence absolute `new URL(...).href` values.
- Only what is reachable is shipped: one of the four OCR core builds (`tesseract-core-simd-lstm.wasm.js` — the app's WasmGC floor implies wasm SIMD, and recognition is LSTM-only) and the `4.0.0_best_int` models rather than the CDN default's standard ones, which carry legacy-engine data that LSTM-only recognition never reads. That is 15MB shipped where the naive copy would have been ~40MB.
- `sw.js`'s `OFFLINE_LIBRARIES` names each file that must survive going offline, and `PwaAssetsTest` checks that list against the directory in both directions, so a vendored file nobody pre-caches — one that works online and fails offline, the exact bug this removes — fails the build instead. The `cmaps/` are the one deliberate omission: 169 files only a CJK-encoded PDF touches.

Both browser bridges (`pdf-extract.js`, `ocr-extract.js`) wrap every failure with the library's name before it leaves: the Kotlin side surfaces a JS error's `message` verbatim to the user, and a bare engine message ("undefined is not a function") names neither the file nor what was being attempted. `pdf-extract.js` also checks up front for the JS features pdf.js needs (`Promise.withResolvers`, `structuredClone`) — the app's own baseline is WasmGC, which is not quite the same generation of browser — so an unsupported browser says so instead of failing somewhere inside the library.

## Testing conventions

- Unit tests (`app/src/androidUnitTest`) mirror the source package structure 1:1 and use JUnit4 + `mockito-kotlin` + `kotlinx-coroutines-test`; ViewModel tests typically pair with an `androidx-arch-core-testing` `InstantTaskExecutorRule`. They compile against the Android target (which now includes `roomMain`), so they exercise the common code and its Android/`roomMain` actuals together.
- Because the shared code no longer touches Android APIs, most tests need no framework doubles: `InMemoryPlatformFile` stands in for a picked document and `InMemoryKeyValueStore` for preferences.
- The automatic importer is tested end to end on the JVM by injecting a canned `RecognizedDocument` into `SmartImportAnalyzer` (`data/vision/SmartImportAnalyzerTest`), so the pipeline is covered without a text recognizer. `DocumentLayoutAnalyzerTest` places words by hand to check line/cell/column reconstruction; `TransactionFieldClassifierTest` pins the column mapping for the layouts the app has to survive (headerless, reordered, pt/en/es headers, debit/credit pairs, direction columns, signed amounts) and is the place to add a case when a new layout misclassifies.
- `ReceiptAnalyzerTest` does the same for the receipt reader, one fixture per printed layout it has to survive (an NFC-e coupon with item codes and a "Qtd. total de itens" line, a receipt with no "total" label, one printing the total's value below its label, one whose service fee has to land among the items for them to add up). A misread receipt belongs there as a new fixture.
- The common replacements for JVM libraries have their own tests — `util/CivilDateTest` (checked against `java.time`, which is still available in the JVM test run), `data/local/zip/ZipReaderTest` (archives produced by `java.util.zip`), `data/local/zip/RawInflateTest` (the wasmJs raw-DEFLATE decoder, cross-checked against `java.util.zip.Deflater` output across stored/fixed-Huffman/dynamic-Huffman blocks and every compression level even though it only runs in production on wasmJs), `data/local/xml/XmlPullReaderTest`, and `data/local/PdfTextLayoutTest` (the wasmJs PDF page-layout reconstruction, driven by hand-placed runs — including the blank gap-runs pdf.js emits, which is the case that regressed).
- `InvestmentsViewModelTest` drives the investments tab through a real `InvestmentRepository` over an in-memory `InvestmentDao` (a mock would not re-emit its Flow, which is where the tab's totals come from); `data/local/entity/InvestmentEntityTest` covers the derived profit numbers and both persisted representations, and pins `InvestmentType`'s stored keys — changing one silently reinterprets rows already on disk.
- Instrumented tests (`app/src/androidInstrumentedTest`) are reserved for things that need a real Android environment: Room DB behavior (`AppDatabaseTest`) and PDFBox text extraction (`PdfParserTest`).
- No mocking framework is used for Room itself in unit tests — parser/repository logic is tested with plain Kotlin objects and in-memory data structures.
- `web/PwaAssetsTest` is the same idea applied to files that aren't code at all: it parses `manifest.webmanifest`, checks each icon exists and really is the size it claims (read out of the PNG header), that `sw.js` precaches nothing that isn't shipped, that `index.html`/`main.kt` still agree on the loading placeholder's id, that `pwa.js` and `AppInstall.wasmJs.kt` still agree on the `window.praOnde*` names and event the install button rides on, and that the vendored engines are complete, pre-cached and never reached for over a CDN. Nothing else in the build looks inside those files — the wasmJs compile check copies them without reading them — so a renamed icon would otherwise surface as a browser quietly declining to install the app.
- There is no unit-test source set for wasmJs; its actuals are exercised indirectly through `wasmJsBrowserDistribution` compiling in CI (the `wasm-build` job) plus the shared `RawInflate`/`PdfTextLayout`/`ImportRepository`/etc. logic they call into being covered on the JVM. Keeping browser-only code thin — a bridge that fetches and marshals, with the interpretation in commonMain — is what makes that second half worth anything.
