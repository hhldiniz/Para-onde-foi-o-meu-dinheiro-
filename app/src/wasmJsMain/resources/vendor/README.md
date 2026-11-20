# Vendored browser libraries

The web build's PDF import, automatic (photo) import and assistant chat need
engines that Kotlin/Wasm has no access to. They used to be loaded from a CDN (jsDelivr and
`tessdata.projectnaptha.com`); they are served from this site instead, because
the app is an offline-capable PWA and a service worker cannot cache a
cross-origin response it is not allowed to read. Vendoring them also means the
app has no third-party runtime dependency: nothing about a user's statements
is visible to anyone, not even as a request for a library. (The one request
to another origin is the chat's model, downloaded once from Hugging Face when
the user asks for it — a file, not code, and the same for everyone.)

Everything here is still loaded **lazily** — `pdf-extract.js` and
`ocr-extract.js` only `import()` on the first PDF or photo actually picked, and
`llm-chat.js` on the first visit to the chat — so none of it is in the app's own
bundle or on the critical path of a launch. The service worker pre-caches the
import engines in the background (`sw.js`, `OFFLINE_LIBRARIES`); the chat
engine is cached the first time it is used instead, since it is no use without
a model the user has to download online anyway.

| Directory | Package (npm) | Version | What it is |
| --- | --- | --- | --- |
| `pdfjs/` | `pdfjs-dist` | 5.7.284 | `build/pdf.min.mjs` + `build/pdf.worker.min.mjs`, plus `standard_fonts/` (the base-14 fonts a PDF may not embed) and `cmaps/` (CJK encodings) |
| `tesseract/` | `tesseract.js` | 5.1.1 | `dist/tesseract.esm.min.js` + `dist/worker.min.js` |
| `tesseract-core/` | `tesseract.js-core` | 5.1.1 | `tesseract-core-simd-lstm.wasm.js`, the OCR engine itself (wasm embedded in the JS) |
| `tessdata/` | `@tesseract.js-data/{por,eng,spa}` | 1.0.0 | The `4.0.0_best_int` models for the three languages the app is translated into |
| `wllama/` | `@wllama/wllama` | 3.8.1 | `esm/index.js` + `esm/wasm/wllama.wasm`: llama.cpp in WebAssembly, behind the assistant chat (default build: JSPI + 64-bit memory, i.e. Chromium) |
| `wllama-compat/` | `@wllama/wllama-compat` | 3.8.1 | `wasm/wllama.js` + `wasm/wllama.wasm`: the Asyncify build wllama falls back to without JSPI (Safari); `llm-chat.js` points `setCompat` at it, or wllama would fetch it from jsDelivr |

Licences ship next to each library (`LICENSE`); pdf.js and the Tesseract
packages are Apache-2.0, wllama is MIT.

## What was deliberately left out

- **The other three `tesseract.js-core` builds** (non-SIMD, and the
  Tesseract-legacy ones). `ocr-extract.js` asks for LSTM-only recognition and
  every browser that can run this app's WasmGC bundle also has wasm SIMD, so
  the worker only ever resolves `tesseract-core-simd-lstm.wasm.js`. That is
  ~12MB of engine builds not shipped.
- **The `4.0.0` (standard) language data**, which is what the CDN default
  served. Those files carry the legacy Tesseract engine's data as well, which
  LSTM-only recognition never reads: `4.0.0_best_int` is the same integerized
  LSTM model without it, and is a quarter of the size (6.2MB against 26MB for
  the three languages).
- **`pdfjs-dist/wasm/`** — the JPX/JBIG2/colour-management codecs. They are
  only used when *rendering* images, and this app only extracts text.
- **Sourcemaps, the pdf.js viewer, `legacy/` builds and the type definitions.**
- **wllama's minified bundle, docs, examples and C++ sources**: `index.js` is
  the plain ES module build (the minified one points at a sourcemap that is
  not shipped).

## Updating

Each directory holds exactly the files listed above, copied straight out of the
npm tarball — nothing is patched, so an update is a re-copy:

```bash
npm pack pdfjs-dist@<version> tesseract.js@<version> tesseract.js-core@<version> \
    @tesseract.js-data/por @tesseract.js-data/eng @tesseract.js-data/spa \
    @wllama/wllama@<version> @wllama/wllama-compat@<version>
```

Then unpack and copy the files named in the table, and bump `PDFJS_VERSION` in
`pdf-extract.js` / `TESSERACT_VERSION` in `ocr-extract.js` / `WLLAMA_VERSION`
in `llm-chat.js` — they only appear in
error messages, but a wrong version there is a wrong bug report. If a file is
added or renamed, `OFFLINE_LIBRARIES` in `sw.js` has to learn about it too;
`PwaAssetsTest` fails until it does.
