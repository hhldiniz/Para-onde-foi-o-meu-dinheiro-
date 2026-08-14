// Bridges browser text recognition for the wasmJs build, used by
// TextRecognizer.wasmJs.kt. Android has ML Kit and iOS has the Vision
// framework; the web has no built-in OCR engine, so this loads Tesseract.js
// lazily — on the first image actually picked — as an ES module, exactly like
// pdf-extract.js does for pdf.js. Neither the library nor the OCR engine nor
// the language data touches the app's own bundle, and a user who never uses
// the automatic import never loads any of it.
//
// All of it is served from this site, out of `vendor/` (see
// `vendor/README.md`), not from a CDN: the app is a PWA that has to work with
// the network off, and a cross-origin dependency is exactly what a service
// worker cannot cache.
//
// The result is returned as a JSON string of words in *normalized* page
// coordinates (0..1 from the top-left), which is the contract every platform
// recognizer honours so DocumentLayoutAnalyzer (commonMain) can stay
// platform-agnostic.
const TESSERACT_VERSION = "5.1.1";
// Resolved against this module's own URL, not the page's: the paths then hold
// wherever the site is deployed, and — since two of them are handed to a Web
// Worker, which resolves relative URLs against itself — they must be absolute
// by the time they leave here anyway.
const TESSERACT_BASE = new URL("./vendor/tesseract/", import.meta.url);
const TESSERACT_CORE_BASE = new URL("./vendor/tesseract-core", import.meta.url);
const TESSDATA_BASE = new URL("./vendor/tessdata", import.meta.url);

// The three languages the app itself is translated into. Statement layouts are
// mostly digits, but the month names and the header words are not.
const LANGUAGES = "por+eng+spa";

let workerPromise = null;

// One worker is created on first use and then reused: loading the language
// data is by far the slowest part of a recognition, and it only happens once.
function loadWorker() {
    if (!workerPromise) {
        workerPromise = import(new URL("tesseract.esm.min.js", TESSERACT_BASE).href)
            .then((module) => {
                const tesseract = typeof module.createWorker === "function" ? module : module.default;
                if (!tesseract || typeof tesseract.createWorker !== "function") {
                    throw new Error(`Tesseract.js loaded from ${TESSERACT_BASE.href} without a usable createWorker export`);
                }
                // `corePath` is a directory on purpose: the worker appends the
                // engine build it needs to it (`tesseract-core-simd-lstm.wasm.js`
                // for the LSTM-only mode asked for by the `1` above), and only
                // that one is vendored — every browser that can run this app's
                // WasmGC bundle also has wasm SIMD.
                return tesseract.createWorker(LANGUAGES, 1, {
                    workerPath: new URL("worker.min.js", TESSERACT_BASE).href,
                    corePath: TESSERACT_CORE_BASE.href,
                    langPath: TESSDATA_BASE.href,
                });
            })
            .catch((error) => {
                // Same reasoning as pdf-extract.js: don't cache a failure, or
                // one bad moment disables the feature for the rest of the
                // session.
                workerPromise = null;
                throw new Error(`could not load Tesseract.js from ${TESSERACT_BASE.href}: ${(error && error.message) || error}`);
            });
    }
    return workerPromise;
}

function base64ToBytes(base64) {
    const binary = atob(base64);
    const bytes = new Uint8Array(binary.length);
    for (let i = 0; i < binary.length; i++) {
        bytes[i] = binary.charCodeAt(i);
    }
    return bytes;
}

// Tesseract.js 4 exposes a flat `data.words`; version 5 only fills the nested
// block tree (and only when asked for it). Both shapes are walked so a CDN
// bumping the minor version cannot silently break the import.
function collectWords(data) {
    if (Array.isArray(data.words) && data.words.length > 0) return data.words;

    const words = [];
    for (const block of data.blocks || []) {
        for (const paragraph of block.paragraphs || []) {
            for (const line of paragraph.lines || []) {
                for (const word of line.words || []) {
                    words.push(word);
                }
            }
        }
    }
    return words;
}

/** See `pdf-extract.js`: a bare engine message reaches the user unexplained. */
function describeFailure(error) {
    const message = (error && error.message) || `${error}`;
    return message.includes("Tesseract.js")
        ? message
        : `Tesseract.js ${TESSERACT_VERSION} failed to read this image: ${message}`;
}

async function recognizeText(base64) {
    const worker = await loadWorker();
    const blob = new Blob([base64ToBytes(base64)]);
    // Tesseract reports pixel boxes, so the page size has to come from the
    // image itself to normalize them.
    const bitmap = await createImageBitmap(blob);
    const width = bitmap.width || 1;
    const height = bitmap.height || 1;

    try {
        const { data } = await worker.recognize(blob, {}, { blocks: true });
        const words = collectWords(data)
            .filter((word) => word.text && word.text.trim().length > 0 && word.bbox)
            .map((word) => ({
                text: word.text.trim(),
                left: word.bbox.x0 / width,
                top: word.bbox.y0 / height,
                right: word.bbox.x1 / width,
                bottom: word.bbox.y1 / height,
                confidence: (typeof word.confidence === "number" ? word.confidence : 100) / 100,
            }));
        return JSON.stringify({ words });
    } finally {
        bitmap.close();
    }
}

window.praOndeRecognizeText = async function (base64) {
    try {
        return await recognizeText(base64);
    } catch (error) {
        throw new Error(describeFailure(error));
    }
};
