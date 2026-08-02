package com.hhldiniz.praondefoiomeudinheiro.platform

import android.graphics.BitmapFactory
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.hhldiniz.praondefoiomeudinheiro.domain.vision.BoundingBox
import com.hhldiniz.praondefoiomeudinheiro.domain.vision.RecognizedDocument
import com.hhldiniz.praondefoiomeudinheiro.domain.vision.RecognizedWord
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * ML Kit's bundled Latin text recognizer. The model ships inside the APK, so
 * recognition works offline and no image ever leaves the device.
 *
 * ML Kit reports element (word) boxes in bitmap pixels; they are divided by
 * the bitmap's own size here, because the shared layout analyzer works in the
 * normalized `0f..1f` space every platform agrees on.
 */
actual suspend fun recognizeDocumentText(bytes: ByteArray): RecognizedDocument {
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        ?: throw IllegalArgumentException("Unsupported or corrupt image")

    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    try {
        val text = suspendCancellableCoroutine<Text> { continuation ->
            recognizer.process(InputImage.fromBitmap(bitmap, 0))
                .addOnSuccessListener { result -> continuation.resume(result) }
                .addOnFailureListener { error -> continuation.resumeWithException(error) }
        }

        val width = bitmap.width.toFloat().coerceAtLeast(1f)
        val height = bitmap.height.toFloat().coerceAtLeast(1f)
        val words = text.textBlocks
            .flatMap { block -> block.lines }
            .flatMap { line -> line.elements }
            .mapNotNull { element ->
                val box = element.boundingBox ?: return@mapNotNull null
                RecognizedWord(
                    text = element.text,
                    box = BoundingBox(
                        left = box.left / width,
                        top = box.top / height,
                        right = box.right / width,
                        bottom = box.bottom / height,
                    ),
                )
            }

        return RecognizedDocument(words)
    } finally {
        recognizer.close()
        bitmap.recycle()
    }
}
