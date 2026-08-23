package com.hhldiniz.praondefoiomeudinheiro.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.hhldiniz.praondefoiomeudinheiro.domain.file.PlatformFile
import com.hhldiniz.praondefoiomeudinheiro.domain.file.PlatformFolder
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIViewController
import platform.UniformTypeIdentifiers.UTTypeCommaSeparatedText
import platform.UniformTypeIdentifiers.UTTypeFolder
import platform.UniformTypeIdentifiers.UTTypeImage
import platform.UniformTypeIdentifiers.UTTypePDF
import platform.UniformTypeIdentifiers.UTTypeSpreadsheet
import platform.darwin.NSObject

/**
 * Bridges `UIDocumentPickerViewController` to the shared picker contract. The
 * delegate is kept alive by `remember` for as long as the calling composable
 * is on screen, which is what UIKit requires (delegates are weak references).
 */
private class DocumentPickerDelegate(
    private val onPicked: (NSURL) -> Unit,
) : NSObject(), UIDocumentPickerDelegateProtocol {

    override fun documentPicker(
        controller: UIDocumentPickerViewController,
        didPickDocumentsAtURLs: List<*>,
    ) {
        (didPickDocumentsAtURLs.firstOrNull() as? NSURL)?.let(onPicked)
    }
}

@Composable
actual fun rememberSpreadsheetFilePicker(onPicked: (PlatformFile) -> Unit): PickerLauncher {
    val currentOnPicked by rememberUpdatedState(onPicked)
    val delegate = remember {
        DocumentPickerDelegate { url -> currentOnPicked(IosPlatformFile(url)) }
    }
    return remember(delegate) {
        PickerLauncher {
            presentPicker(
                UIDocumentPickerViewController(
                    forOpeningContentTypes = listOf(
                        UTTypeCommaSeparatedText,
                        UTTypeSpreadsheet,
                        UTTypePDF,
                    ),
                ),
                delegate,
            )
        }
    }
}

@Composable
actual fun rememberImportSourcePicker(onPicked: (PlatformFile) -> Unit): PickerLauncher {
    val currentOnPicked by rememberUpdatedState(onPicked)
    val delegate = remember {
        DocumentPickerDelegate { url -> currentOnPicked(IosPlatformFile(url)) }
    }
    return remember(delegate) {
        PickerLauncher {
            presentPicker(
                UIDocumentPickerViewController(
                    forOpeningContentTypes = listOf(
                        UTTypeImage,
                        UTTypeCommaSeparatedText,
                        UTTypeSpreadsheet,
                    ),
                ),
                delegate,
            )
        }
    }
}

@Composable
actual fun rememberReceiptPicker(onPicked: (PlatformFile) -> Unit): PickerLauncher {
    val currentOnPicked by rememberUpdatedState(onPicked)
    val delegate = remember {
        DocumentPickerDelegate { url -> currentOnPicked(IosPlatformFile(url)) }
    }
    return remember(delegate) {
        PickerLauncher {
            presentPicker(
                UIDocumentPickerViewController(forOpeningContentTypes = listOf(UTTypeImage)),
                delegate,
            )
        }
    }
}

@Composable
actual fun rememberSpreadsheetFolderPicker(onPicked: (PlatformFolder) -> Unit): PickerLauncher {
    val currentOnPicked by rememberUpdatedState(onPicked)
    val delegate = remember {
        DocumentPickerDelegate { url -> currentOnPicked(IosPlatformFolder(url)) }
    }
    return remember(delegate) {
        PickerLauncher {
            presentPicker(
                UIDocumentPickerViewController(forOpeningContentTypes = listOf(UTTypeFolder)),
                delegate,
            )
        }
    }
}

private fun presentPicker(
    picker: UIDocumentPickerViewController,
    delegate: UIDocumentPickerDelegateProtocol,
) {
    picker.delegate = delegate
    topViewController()?.presentViewController(picker, animated = true, completion = null)
}

private fun topViewController(): UIViewController? {
    var controller = UIApplication.sharedApplication.keyWindow?.rootViewController
    while (controller?.presentedViewController != null) {
        controller = controller.presentedViewController
    }
    return controller
}
