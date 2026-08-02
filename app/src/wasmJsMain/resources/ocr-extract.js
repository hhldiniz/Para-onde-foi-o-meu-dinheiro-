// Bridges browser text recognition for the wasmJs build, used by
// TextRecognizer.wasmJs.kt. Android has ML Kit and iOS has the Vision
// framework; the web has no built-in OCR engine, so this loads Tesseract.js
// lazily — on the first image actually picked — straight from a CDN as an ES
// module, exactly like pdf-extract.js does for pdf.js. Neither the library
// (~1MB) nor the language data (a few MB) touches the app's own bundle, and a
// user who never uses the automatic import never downloads any of it.
//
// The result is returned as a JSON string of words in *normalized* page
// coordinates (0..1 from the top-left), which is the contract every platform
// recognizer honours so DocumentLayoutAnalyzer (commonMain) can stay
// platform-agnostic.
const TESSERACT_VERSION = "5.1.1";
const TESSERACT_CORE_VERSION = "5.1.1";
const TESSERACT_BASE = `https://cdn.jsdelivr.net/npm/tesseract.js@${TESSERACT_VERSION}`;
const TESSERACT_CORE_BASE = `https://cdn.jsdelivr.net/npm/tesseract.js-core@${TESSERACT_CORE_VERSION}`;
const TESSDATA_BASE = "https://tessdata.projectnaptha.com/4.0.0";

// The three languages the app itself is translated into. Statement layouts are
// mostly digits, but the month names and the header words are not.
const LANGUAGES = "por+eng+spa";

let workerPromise = null;

// One worker is created on first use and then reused: loading the language
// data is by far the slowest part of a recognition, and it only happens once.
function loadWorker() {
    if (!workerPromise) {
        workerPromise = import(`${TESSERACT_BASE}/dist/tesseract.esm.min.js`).then((module) => {
            const tesseract = module.default || module;
            return tesseract.createWorker(LANGUAGES, 1, {
                workerPath: `${TESSERACT_BASE}/dist/worker.min.js`,
                corePath: TESSERACT_CORE_BASE,
                langPath: TESSDATA_BASE,
            });
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

window.praOndeRecognizeText = async function (base64) {
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
};
