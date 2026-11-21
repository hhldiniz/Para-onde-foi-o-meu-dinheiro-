package com.hhldiniz.praondefoiomeudinheiro.resources

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Checks the Compose Multiplatform `strings.xml` files for Android-only
 * escaping. Android's resource compiler turns `\'` and `\"` into plain quotes,
 * but Compose Multiplatform resources take the text as written, so the
 * backslash reaches the screen ("Let\'s Get Started!") on every platform.
 * A plain `'` or `"` inside the element is what both expect.
 */
class StringResourcesTest {

    private val resources: File = findComposeResources()

    @Test
    fun `no string carries an Android-style escaped quote`() {
        val files = resources.listFiles { file -> file.isDirectory && file.name.startsWith("values") }!!
            .map { it.resolve("strings.xml") }
            .filter { it.isFile }
        assertTrue("no strings.xml found under $resources", files.isNotEmpty())

        val offending = files.flatMap { file ->
            file.readLines().withIndex()
                .filter { (_, line) -> line.contains("\\'") || line.contains("\\\"") }
                .map { (index, line) -> "${file.parentFile.name}/strings.xml:${index + 1}: ${line.trim()}" }
        }
        assertTrue("escaped quotes show up literally in Compose resources:\n" + offending.joinToString("\n"), offending.isEmpty())
    }

    private companion object {
        /** Walks up from the working directory, like PwaAssetsTest, so the test does not depend on where Gradle runs it. */
        fun findComposeResources(): File {
            var directory: File? = File(System.getProperty("user.dir")).absoluteFile
            while (directory != null) {
                for (relative in listOf("src/commonMain/composeResources", "app/src/commonMain/composeResources")) {
                    val candidate = directory.resolve(relative)
                    if (candidate.isDirectory) return candidate
                }
                directory = directory.parentFile
            }
            throw AssertionError("could not find app/src/commonMain/composeResources from ${System.getProperty("user.dir")}")
        }
    }
}
