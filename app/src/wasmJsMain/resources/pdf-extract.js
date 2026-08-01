// Bridges browser PDF text extraction for the wasmJs build. Android uses
// PDFBox, iOS uses PDFKit; the web has no bundled PDF engine, so this loads
// Mozilla's pdf.js lazily, on the first PDF actually picked, straight from a
// CDN as an ES module (dynamic `import()`) rather than shipping it in the
// app's own JS bundle.
//
// pdf.js's `getTextContent()` returns individual glyph runs with x/y
// positions and no inter-run padding. PdfParser.splitIntoRows (commonMain)
// expects columns separated by 2+ whitespace characters, which is what
// PDFBox's `sortByPosition = true` and PDFKit's `.string` naturally produce.
// `itemsToText` below reconstructs that: it groups items into lines by Y
// position, then pads gaps in X with spaces proportional to their size so
// the same `\s{2,}` regex keeps working regardless of which platform parsed
// the file.
const PDFJS_VERSION = "5.7.284";
const PDFJS_BASE = `https://cdn.jsdelivr.net/npm/pdfjs-dist@${PDFJS_VERSION}/build`;

let pdfjsLibPromise = null;

function loadPdfjs() {
    if (!pdfjsLibPromise) {
        pdfjsLibPromise = import(`${PDFJS_BASE}/pdf.min.mjs`).then((pdfjsLib) => {
            pdfjsLib.GlobalWorkerOptions.workerSrc = `${PDFJS_BASE}/pdf.worker.min.mjs`;
            return pdfjsLib;
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

function itemsToText(items) {
    const yTolerance = 2;
    const lines = [];

    for (const item of items) {
        if (!item.str) continue;
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

    return lines
        .map((line) => {
            line.parts.sort((a, b) => a.x - b.x);
            let text = "";
            let previousEnd = null;
            for (const part of line.parts) {
                if (previousEnd !== null) {
                    const gap = part.x - previousEnd;
                    // A small gap is normal inter-word spacing (one space);
                    // a wide gap is very likely a column boundary in a
                    // tabular report, so it is padded well past the `\s{2,}`
                    // threshold that PdfParser.splitIntoRows splits on.
                    const spaces = gap > 8 ? Math.max(2, Math.round(gap / 4)) : gap > 2 ? 1 : 0;
                    text += " ".repeat(spaces);
                }
                text += part.str;
                previousEnd = part.x + part.width;
            }
            return text;
        })
        .join("\n");
}

window.praOndeExtractPdfText = async function (base64) {
    const pdfjsLib = await loadPdfjs();
    const bytes = base64ToBytes(base64);
    const doc = await pdfjsLib.getDocument({ data: bytes }).promise;

    const pageTexts = [];
    for (let pageNumber = 1; pageNumber <= doc.numPages; pageNumber++) {
        const page = await doc.getPage(pageNumber);
        const content = await page.getTextContent();
        pageTexts.push(itemsToText(content.items));
    }
    return pageTexts.join("\n");
};
