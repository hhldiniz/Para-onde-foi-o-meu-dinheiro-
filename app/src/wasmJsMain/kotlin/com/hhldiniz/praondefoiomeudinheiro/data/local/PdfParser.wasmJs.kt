package com.hhldiniz.praondefoiomeudinheiro.data.local

// A real PDF text extractor needs either a large JS library (pdf.js) or a
// native PDF engine, neither of which fits this function's synchronous,
// dependency-free contract. SpreadsheetFileValidator.validate already
// catches parsing exceptions and reports error_cannot_read_file, so this
// surfaces as a normal-looking rejection instead of a crash — PDF import is
// out of scope for the web build.
actual fun extractPdfText(bytes: ByteArray): String =
    throw UnsupportedOperationException("PDF import is not supported in the web build")
