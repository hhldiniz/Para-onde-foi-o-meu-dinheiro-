package com.hhldiniz.praondefoiomeudinheiro.domain.file

/**
 * A file the user picked, abstracted away from `android.net.Uri` /
 * `NSURL` so the parsing and repository layers can stay in common code.
 *
 * Implementations are provided per platform (a content URI on Android, a
 * security-scoped `NSURL` on iOS) plus [InMemoryPlatformFile] for tests and
 * Compose previews.
 */
interface PlatformFile {
    /** Display name including extension, e.g. `gastos.csv`. */
    val name: String

    /** Stable platform identifier (URI string / file path), used for logging and equality. */
    val identifier: String

    /** MIME type when the platform knows it, used as a fallback when the name has no extension. */
    val mimeType: String? get() = null

    /** Reads the whole file. Callers always run this off the main thread. */
    suspend fun readBytes(): ByteArray
}

/** A directory the user picked, from which spreadsheet files can be enumerated. */
interface PlatformFolder {
    val name: String
    val identifier: String

    /** Lists the `.csv` / `.ods` / `.pdf` files directly inside this folder. */
    suspend fun listSpreadsheetFiles(): List<PlatformFile>
}

/** File extensions the importer understands, used by both folder pickers. */
val SPREADSHEET_EXTENSIONS = listOf(".csv", ".ods", ".pdf")

fun String.hasSpreadsheetExtension(): Boolean =
    SPREADSHEET_EXTENSIONS.any { endsWith(it, ignoreCase = true) }

/** A [PlatformFile] backed by a byte array; used by tests and Compose previews. */
class InMemoryPlatformFile(
    override val name: String,
    private val bytes: ByteArray = ByteArray(0),
    override val identifier: String = name,
) : PlatformFile {
    override suspend fun readBytes(): ByteArray = bytes
}
