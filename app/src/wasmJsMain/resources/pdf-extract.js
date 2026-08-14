// Bridges browser PDF text extraction for the wasmJs build. Android uses
// PDFBox, iOS uses PDFKit; the web has no bundled PDF engine, so this loads
// Mozilla's pdf.js lazily, on the first PDF actually picked, as an ES module
// (dynamic `import()`) rather than shipping it in the app's own JS bundle.
//
// The library is served from this site, out of `vendor/pdfjs/` (see
// `vendor/README.md`), not from a CDN: the app is a PWA that has to work with
// the network off, and a cross-origin dependency is exactly what a service
// worker cannot cache. It is still loaded lazily, so a user who never imports
// a PDF never downloads it.
//
// pdf.js's `getTextContent()` returns the page as positioned glyph runs,
// unlike PDFBox (`sortByPosition = true`) and PDFKit's `.string`, which
// return text already padded into columns. This file only hands those runs
// over as JSON; turning them back into padded text is PdfTextLayout's job in
// commonMain, so the reconstruction is covered by the JVM test run instead of
// living untested in browser-only code.
const PDFJS_VERSION = "5.7.284";
// Resolved against this module's own URL rather than the page's, so the paths
// hold wherever the site is deployed (a repository subdirectory on GitHub
// Pages) and whatever route the app is showing.
const PDFJS_BASE = new URL("./vendor/pdfjs/", import.meta.url);

/** Absolute URL of a vendored pdf.js file. */
function pdfjsUrl(path) {
    return new URL(path, PDFJS_BASE).href;
}

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
        pdfjsLibPromise = import(pdfjsUrl("pdf.min.mjs"))
            .then((module) => {
                // Depending on how the CDN serves the build, the API is either
                // the module's named exports or hidden behind `default`.
                // Reading `getDocument` off the wrong one is what turns a
                // perfectly loaded library into "undefined is not a function"
                // at the call site, far away from the real cause.
                const lib = typeof module.getDocument === "function" ? module : module.default;
                if (!lib || typeof lib.getDocument !== "function") {
                    throw new Error(`pdf.js loaded from ${PDFJS_BASE.href} without a usable getDocument export`);
                }
                if (lib.GlobalWorkerOptions) {
                    lib.GlobalWorkerOptions.workerSrc = pdfjsUrl("pdf.worker.min.mjs");
                }
                return lib;
            })
            .catch((error) => {
                // Without this the first failure (a file not yet in the
                // service worker's cache while offline) would be cached in the
                // promise and every later import in the session would fail
                // with it, even once the network is back. Clearing it lets the
                // next PDF retry from scratch.
                pdfjsLibPromise = null;
                throw new Error(`could not load pdf.js from ${PDFJS_BASE.href}: ${(error && error.message) || error}`);
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
 * The page's text runs in the shape `PdfTextLayout` reads: x is the run's left
 * edge and y its baseline, both straight out of the text matrix. Items that
 * carry no `str` at all are pdf.js's marked-content markers, which have no
 * position; blank ones do have a position and are kept, because it is
 * PdfTextLayout that decides what to do with them.
 */
function pageRuns(items) {
    const runs = [];
    for (const item of items) {
        if (typeof item.str !== "string" || !item.transform) continue;
        runs.push({
            text: item.str,
            x: item.transform[4],
            y: item.transform[5],
            width: item.width || 0,
        });
    }
    return runs;
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

async function extractPdfRuns(base64) {
    const pdfjsLib = await loadPdfjs();
    const bytes = base64ToBytes(base64);

    // Without these two, a PDF that leans on the 14 standard fonts (or on a
    // CJK encoding) loses characters or fails outright during extraction —
    // pdf.js only bundles the data next to the build, it does not fetch it
    // from anywhere by default. Both directories are vendored alongside the
    // library; the fonts are pre-cached for offline use, the cmaps are not
    // (see `vendor/README.md`).
    const doc = await pdfjsLib.getDocument({
        data: bytes,
        standardFontDataUrl: pdfjsUrl("standard_fonts/"),
        cMapUrl: pdfjsUrl("cmaps/"),
        cMapPacked: true,
    }).promise;

    try {
        const pages = [];
        for (let pageNumber = 1; pageNumber <= doc.numPages; pageNumber++) {
            const page = await doc.getPage(pageNumber);
            const content = await page.getTextContent();
            pages.push({ runs: pageRuns(content.items) });
        }
        return JSON.stringify({ pages });
    } finally {
        // Releases the worker's copy of the file; without it every imported
        // PDF stays in memory for the rest of the session.
        doc.destroy();
    }
}

window.praOndeExtractPdfRuns = async function (base64) {
    try {
        return await extractPdfRuns(base64);
    } catch (error) {
        throw new Error(describeFailure(error));
    }
};
