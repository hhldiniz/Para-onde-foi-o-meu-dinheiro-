package com.hhldiniz.praondefoiomeudinheiro.data.local

import com.hhldiniz.praondefoiomeudinheiro.domain.file.PlatformFile

/**
 * Holds the files selected by the user during the landing flow, so they can
 * be consumed later by `HomeViewModel`.
 */
object SelectedFilesHolder {
    var files: List<PlatformFile> = emptyList()
}
