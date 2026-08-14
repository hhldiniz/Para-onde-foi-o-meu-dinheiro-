package com.hhldiniz.praondefoiomeudinheiro.web

import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Checks the static files that turn the web build into an installable PWA:
 * `manifest.webmanifest`, the icons it points at, the service worker's
 * precache list and the way `index.html` ties them together.
 *
 * None of it is Kotlin, so nothing else in the build would notice a renamed
 * icon or a manifest typo — the wasmJs compile check in CI copies these files
 * without looking inside them, and the failure only shows up as a browser
 * quietly refusing to offer the install. This test is the substitute for a
 * compiler, in the same spirit as the other coverage of browser-only
 * behaviour living on the JVM.
 */
class PwaAssetsTest {

    private val resources: File = findWebResources()

    private val manifest: JsonObject =
        Json.parseToJsonElement(resources.resolve("manifest.webmanifest").readText()).jsonObject

    private fun string(key: String): String? = manifest[key]?.jsonPrimitive?.content

    @Test
    fun `manifest declares what an installable app needs`() {
        assertEquals("Pra onde foi o meu dinheiro", string("name"))
        assertNotNull("short_name is what a launcher shows under the icon", string("short_name"))
        assertTrue("short_name has to fit a launcher label", string("short_name")!!.length <= 12)
        assertEquals("standalone", string("display"))
        assertNotNull(string("description"))

        // Relative, because GitHub Pages serves the app from a repository
        // subdirectory: an absolute "/" would scope the app to the origin's
        // root and break both the install and the service worker there.
        for (key in listOf("id", "start_url", "scope")) {
            val value = string(key)
            assertTrue("$key must be relative to the manifest, was $value", value?.startsWith("./") == true)
        }

        // The splash screen an installed launch shows is drawn from these two,
        // so they have to match what index.html and the theme paint.
        assertEquals("#FFF5E6", string("background_color"))
        assertEquals("#EAB308", string("theme_color"))
    }

    @Test
    fun `every icon the manifest points at exists at the size it claims`() {
        val icons = manifest["icons"]!!.jsonArray.map { it.jsonObject }
        assertTrue("a manifest with no icons is not installable", icons.isNotEmpty())

        for (icon in icons) {
            val src = icon["src"]!!.jsonPrimitive.content
            val file = resources.resolve(src)
            assertTrue("$src is declared in the manifest but missing from the web resources", file.isFile)

            val sizes = icon["sizes"]!!.jsonPrimitive.content
            if (sizes == "any") {
                assertTrue("$src is declared scalable but is not an SVG", file.readText().contains("<svg"))
                continue
            }
            val declared = sizes.split("x").map { it.toInt() }
            assertEquals("$src does not have the size the manifest declares", declared, pngSize(file))
        }

        // Chrome installs on the strength of a 192px icon and draws the splash
        // screen from a 512px one; Android masks whatever it is given, and an
        // icon that is not maskable-safe comes out cropped.
        assertTrue("no 192px icon", icons.any { it["sizes"]?.jsonPrimitive?.content == "192x192" })
        assertTrue("no 512px icon", icons.any { it["sizes"]?.jsonPrimitive?.content == "512x512" })
        assertTrue("no maskable icon", icons.any { it["purpose"]?.jsonPrimitive?.content == "maskable" })
    }

    @Test
    fun `the service worker precaches files that are actually shipped`() {
        val serviceWorker = resources.resolve("sw.js").readText()
        val shell = Regex("""const SHELL = \[(.*?)]""", RegexOption.DOT_MATCHES_ALL)
            .find(serviceWorker)
            ?.groupValues
            ?.get(1)
            ?: throw AssertionError("sw.js no longer declares a SHELL list")

        val paths = Regex(""""(\./[^"]*)"""").findAll(shell).map { it.groupValues[1] }.toList()
        assertTrue("sw.js precaches nothing", paths.isNotEmpty())
        for (path in paths) {
            // "./" is the page itself, which the browser fetches from the
            // server rather than from this directory.
            if (path == "./") continue
            assertTrue("sw.js precaches $path, which is not in the web resources", resources.resolve(path).isFile)
        }
    }

    /**
     * The PDF and OCR engines are vendored under `vendor/` (see its README)
     * rather than loaded from a CDN, which is the only reason the service
     * worker can cache them at all — a cross-origin response it cannot read is
     * a response it cannot store. A single URL slipping back in would compile,
     * ship, and quietly break the import for an offline user.
     */
    @Test
    fun `the import engines are served from this site, not from a CDN`() {
        for (script in listOf("pdf-extract.js", "ocr-extract.js")) {
            val source = resources.resolve(script).readText()
            for (cdn in listOf("cdn.jsdelivr.net", "tessdata.projectnaptha.com", "unpkg.com")) {
                assertTrue("$script still loads something from $cdn", !source.contains(cdn))
            }
            assertTrue("$script does not load anything from vendor/", source.contains("./vendor/"))
        }

        // Every directory the bridges name has to be there, since a missing one
        // only shows up as a failed import in a browser.
        for (path in listOf(
            "vendor/pdfjs/pdf.min.mjs",
            "vendor/pdfjs/pdf.worker.min.mjs",
            "vendor/pdfjs/standard_fonts",
            "vendor/pdfjs/cmaps",
            "vendor/tesseract/tesseract.esm.min.js",
            "vendor/tesseract/worker.min.js",
            // The one core build ocr-extract.js can end up asking for: the
            // worker appends this exact name for LSTM-only on a SIMD browser.
            "vendor/tesseract-core/tesseract-core-simd-lstm.wasm.js",
            "vendor/tessdata/por.traineddata.gz",
            "vendor/tessdata/eng.traineddata.gz",
            "vendor/tessdata/spa.traineddata.gz",
        )) {
            assertTrue("$path is missing from the web resources", resources.resolve(path).exists())
        }
    }

    /**
     * What the service worker pulls down in the background for offline use.
     * The list is hand-written, so this checks it both ways: nothing in it is
     * missing from the build, and nothing shipped is missing from it — a
     * vendored file that nobody pre-caches works online and fails offline,
     * which is the failure this whole change exists to remove.
     */
    @Test
    fun `the offline library list matches what is vendored`() {
        // Every path the worker names, wherever in the file it is written —
        // the lists are plain literals precisely so this can read them.
        val listed = Regex(""""(\./vendor/[^"]*)"""")
            .findAll(resources.resolve("sw.js").readText())
            .map { it.groupValues[1] }
            .toList()

        assertTrue("sw.js pre-caches no libraries at all", listed.isEmpty().not())
        for (path in listed) {
            assertTrue("sw.js pre-caches $path, which is not in the web resources", resources.resolve(path).isFile)
        }

        val vendored = resources.resolve("vendor")
            .walkTopDown()
            .filter { it.isFile }
            .filterNot { it.name.startsWith("LICENSE") || it.name.endsWith(".LICENSE.txt") || it.name == "README.md" }
            // The cmaps are deliberately left online-only: 169 files that only
            // a PDF with a CJK encoding ever touches.
            .filterNot { it.parentFile.name == "cmaps" }
            .map { "./" + it.relativeTo(resources).path.replace('\\', '/') }
            .toList()

        val missing = vendored - listed.toSet()
        assertTrue("vendored but never pre-cached, so unusable offline: $missing", missing.isEmpty())
    }

    @Test
    fun `index html wires up the manifest, the icons and the service worker`() {
        val html = resources.resolve("index.html").readText()

        assertTrue("index.html does not link the manifest", html.contains("""href="manifest.webmanifest""""))
        assertTrue("index.html has no theme-color for the browser chrome", html.contains("""content="#EAB308""""))
        assertTrue("index.html does not load pwa.js, so nothing registers sw.js", html.contains("""src="pwa.js""""))

        // The icon links iOS and browser tabs read; neither goes through the
        // manifest, so they are easy to leave behind on a rename.
        for (icon in listOf("apple-touch-icon.png", "favicon-32.png", "icon.svg")) {
            assertTrue("index.html does not reference $icon", html.contains(icon))
            assertTrue("$icon is missing from the web resources", resources.resolve(icon).isFile)
        }

        assertTrue("pwa.js does not register sw.js", resources.resolve("pwa.js").readText().contains("""register("./sw.js")"""))
    }

    /**
     * `index.html` paints a placeholder while the wasm bundle downloads and
     * `main.kt` removes it once Compose is ready; they agree on nothing but
     * this id, and a mismatch would leave the placeholder covering the app.
     */
    @Test
    fun `the loading placeholder is removed by the wasmJs entry point`() {
        val html = resources.resolve("index.html").readText()
        val mainKt = wasmJsSource("main.kt")

        assertTrue("index.html no longer has the loading placeholder", html.contains("""id="app-loading""""))
        assertTrue("main.kt no longer removes the loading placeholder", mainKt.contains(""""app-loading""""))
    }

    /**
     * The in-app install button, whose two halves are joined by nothing but
     * these names: `pwa.js` catches `beforeinstallprompt` (it fires while the
     * wasm bundle is still downloading, so Kotlin cannot) and hangs the prompt
     * off `window`; `AppInstall.wasmJs.kt` reads it back from there. A rename
     * on either side compiles and ships, and the button just never appears.
     */
    @Test
    fun `the install bridge is defined on the JS side and read from Kotlin`() {
        val pwa = resources.resolve("pwa.js").readText()
        val installer = wasmJsSource("platform/AppInstall.wasmJs.kt")

        assertTrue("pwa.js does not hold back beforeinstallprompt", pwa.contains(""""beforeinstallprompt""""))
        for (name in listOf("praOndeCanInstall", "praOndeInstall")) {
            assertTrue("pwa.js does not define window.$name", pwa.contains("window.$name = function"))
            assertTrue("AppInstall.wasmJs.kt does not call window.$name", installer.contains("window.$name"))
        }

        // The event that moves the button between hidden and shown.
        val event = "praonde:installavailability"
        assertTrue("pwa.js does not dispatch $event", pwa.contains("""CustomEvent("$event")"""))
        assertTrue("AppInstall.wasmJs.kt does not listen for $event", installer.contains(""""$event""""))
    }

    private fun wasmJsSource(path: String): String =
        resources
            .resolveSibling("kotlin")
            .resolve("com/hhldiniz/praondefoiomeudinheiro")
            .resolve(path)
            .readText()

    /** Width and height read out of a PNG's IHDR chunk, which starts at byte 16. */
    private fun pngSize(file: File): List<Int> {
        val bytes = file.readBytes()
        assertTrue("${file.name} is not a PNG", bytes.take(4) == listOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte()))
        fun intAt(offset: Int): Int = (0 until 4).fold(0) { acc, i -> (acc shl 8) or (bytes[offset + i].toInt() and 0xFF) }
        return listOf(intAt(16), intAt(20))
    }

    private companion object {
        /**
         * The unit tests run with the module directory as their working
         * directory, but this walks up to whichever ancestor holds the web
         * resources so the test does not depend on that staying true.
         */
        fun findWebResources(): File {
            var directory: File? = File(System.getProperty("user.dir")).absoluteFile
            while (directory != null) {
                for (relative in listOf("src/wasmJsMain/resources", "app/src/wasmJsMain/resources")) {
                    val candidate = directory.resolve(relative)
                    if (candidate.isDirectory) return candidate
                }
                directory = directory.parentFile
            }
            throw AssertionError("could not find app/src/wasmJsMain/resources from ${System.getProperty("user.dir")}")
        }
    }
}
