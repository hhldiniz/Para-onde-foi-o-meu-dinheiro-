// Bridges browser PDF text extraction for the wasmJs build. Android uses
// PDFBox, iOS uses PDFKit; the web has no bundled PDF engine, so this loads
// Mozilla's pdf.js lazily, on the first PDF actually picked, straight from a
// CDN as an ES module (dynamic `import()`) rather than shipping it in the
// app's own JS bundle.
//
// pdf.js's `getTextContent()` returns the page as positioned glyph runs.
// PdfParser.splitIntoRows (commonMain) expects columns separated by 2+
// whitespace characters, which is what PDFBox's `sortByPosition = true` and
// PDFKit's `.string` naturally produce. `itemsToText` below reconstructs that
// padding from the runs' x positions so the same `\s{2,}` regex keeps working
// regardless of which platform parsed the file.
const PDFJS_VERSION = "5.7.284";
const PDFJS_PACKAGE = `https://cdn.jsdelivr.net/npm/pdfjs-dist@${PDFJS_VERSION}`;
const PDFJS_BASE = `${PDFJS_PACKAGE}/build`;

let pdfjsLibPromise = null;

// The app's own floor is Kotlin/Wasm (garbage-collected WebAssembly): Chrome
// 119, Firefox 120, Safari 18.2. pdf.js builds `Promise.withResolvers` into
// its worker plumbing, and that arrived a release later in Firefox (121), so a
// browser can run this app and still be unable to run pdf.js. Left unchecked
// the user gets a bare engine message from somewhere deep inside the library
// ("Promise.withResolvers is not a function", or, in Safari's wording,
// "undefined is not a function") that says nothing about the PDF they picked.
function unsupportedFeature() {
    return typeof Promise.withResolvers !== "function" ? "Promise.withResolvers" : null;
}

function loadPdfjs() {
    if (!pdfjsLibPromise) {
        const missing = unsupportedFeature();
        if (missing) {
            return Promise.reject(
                new Error(`this browser is too old to read PDFs: it has no ${missing}`)
            );
        }
        pdfjsLibPromise = import(`${PDFJS_BASE}/pdf.min.mjs`)
            .then((module) => {
                // Depending on how the CDN serves the build, the API is either
                // the module's named exports or hidden behind `default`.
                // Reading `getDocument` off the wrong one is what turns a
                // perfectly loaded library into "undefined is not a function"
                // at the call site, far away from the real cause.
                const lib = typeof module.getDocument === "function" ? module : module.default;
                if (!lib || typeof lib.getDocument !== "function") {
                    throw new Error(`pdf.js loaded from ${PDFJS_BASE} without a usable getDocument export`);
                }
                if (lib.GlobalWorkerOptions) {
                    lib.GlobalWorkerOptions.workerSrc = `${PDFJS_BASE}/pdf.worker.min.mjs`;
                }
                return lib;
            })
            .catch((error) => {
                // Without this the first failure (an offline moment, a blocked
                // CDN) would be cached in the promise and every later import
                // in the session would fail with it, even once the network is
                // back. Clearing it lets the next PDF retry from scratch.
                pdfjsLibPromise = null;
                throw new Error(`could not load pdf.js from ${PDFJS_BASE}: ${(error && error.message) || error}`);
            });
    }
    return pdfjsLibPromise;
}

function base64ToBytes(base64) {
    const binary = atob(base64);
    const bytes = new Uint8Array(binary.length);
    for (let i = 0; i < binary.length; i++) {
        bytes[i] = binary.charCodeAt(i);
    }
    return bytes;
}

/**
 * Rough width of one character in this run, used as the unit for every gap
 * threshold below. Absolute PDF points cannot be used: the same 8pt gap is a
 * column boundary in an 11pt statement and mid-word in a 24pt one.
 */
function averageCharWidth(part) {
    return part.width > 0 && part.str.length > 0 ? part.width / part.str.length : 0;
}

function median(values) {
    if (values.length === 0) return 0;
    const sorted = [...values].sort((a, b) => a - b);
    return sorted[Math.floor(sorted.length / 2)];
}

function itemsToText(items) {
    const yTolerance = 2;
    const lines = [];

    for (const item of items) {
        // Runs with no printable text are dropped rather than appended: pdf.js
        // already reconstructs inter-run gaps itself, but it does so as a
        // *single-space* run whose width spans the whole gap. Keeping those
        // would make every gap measured below come out as zero — a table's
        // columns would end up one space apart and PdfParser.splitIntoRows,
        // which splits on 2+ spaces, would read each row as a single column.
        // Dropping them leaves the real distance between the printable runs
        // visible, which is what the padding below is derived from.
        if (!item.str || !item.str.trim() || !item.transform) continue;
        const x = item.transform[4];
        const y = item.transform[5];
        let line = lines.find((candidate) => Math.abs(candidate.y - y) <= yTolerance);
        if (!line) {
            line = { y, parts: [] };
            lines.push(line);
        }
        line.parts.push({ x, str: item.str, width: item.width || 0 });
    }

    // PDF space grows upward, so reading order is descending Y, then
    // ascending X within a line.
    lines.sort((a, b) => b.y - a.y);

    const pageCharWidth = median(
        lines.flatMap((line) => line.parts.map(averageCharWidth).filter((width) => width > 0))
    );

    return lines
        .map((line) => {
            line.parts.sort((a, b) => a.x - b.x);
            // Prefer this line's own text size — a 7pt footer and a 14pt
            // heading do not share a column threshold — and fall back to the
            // page's when the line is too short to measure.
            const charWidth =
                median(line.parts.map(averageCharWidth).filter((width) => width > 0)) ||
                pageCharWidth ||
                4;

            let text = "";
            let previousEnd = null;
            for (const part of line.parts) {
                if (previousEnd !== null) {
                    const gap = (part.x - previousEnd) / charWidth;
                    // A gap around one character wide is ordinary word spacing
                    // (one space); a wide one is very likely a column boundary
                    // in a tabular report, so it is padded well past the
                    // `\s{2,}` threshold PdfParser.splitIntoRows splits on.
                    const spaces = gap >= 1.4 ? Math.max(2, Math.round(gap)) : gap >= 0.25 ? 1 : 0;
                    text += " ".repeat(spaces);
                }
                text += part.str;
                previousEnd = part.x + part.width;
            }
            return text;
        })
        .join("\n");
}

/**
 * Anything raised in here reaches the user: the Kotlin side surfaces a JS
 * error's `message` verbatim through `smart_import_error_failed` /
 * `error_cannot_read_file`. An engine-level message on its own ("undefined is
 * not a function") names neither the file nor the library it came from, so
 * every failure below leaves with pdf.js's name attached to it.
 */
function describeFailure(error) {
    const message = (error && error.message) || `${error}`;
    return message.includes("pdf.js") || message.startsWith("this browser")
        ? message
        : `pdf.js ${PDFJS_VERSION} failed to read this PDF: ${message}`;
}

async function extractPdfText(base64) {
    const pdfjsLib = await loadPdfjs();
    const bytes = base64ToBytes(base64);

    // Without these two, a PDF that leans on the 14 standard fonts (or on a
    // CJK encoding) loses characters or fails outright during extraction —
    // pdf.js only bundles the data next to the build, it does not fetch it
    // from anywhere by default.
    const doc = await pdfjsLib.getDocument({
        data: bytes,
        standardFontDataUrl: `${PDFJS_PACKAGE}/standard_fonts/`,
        cMapUrl: `${PDFJS_PACKAGE}/cmaps/`,
        cMapPacked: true,
    }).promise;

    try {
        const pageTexts = [];
        for (let pageNumber = 1; pageNumber <= doc.numPages; pageNumber++) {
            const page = await doc.getPage(pageNumber);
            const content = await page.getTextContent();
            pageTexts.push(itemsToText(content.items));
        }
        return pageTexts.join("\n");
    } finally {
        // Releases the worker's copy of the file; without it every imported
        // PDF stays in memory for the rest of the session.
        doc.destroy();
    }
}

window.praOndeExtractPdfText = async function (base64) {
    try {
        return await extractPdfText(base64);
    } catch (error) {
        throw new Error(describeFailure(error));
    }
};
