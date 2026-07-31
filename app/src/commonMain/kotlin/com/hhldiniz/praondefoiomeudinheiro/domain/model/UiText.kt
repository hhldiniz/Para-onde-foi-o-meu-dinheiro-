package com.hhldiniz.praondefoiomeudinheiro.domain.model

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

/**
 * A piece of user-facing text that is not resolved yet. Layers below the UI
 * describe *which* message to show; only the UI turns it into a `String`, in
 * the viewer's language. It also keeps those layers unit-testable, since
 * asserting on a [UiText] needs no resource loader.
 */
sealed interface UiText {

    /** A localized string, optionally with format arguments. */
    data class Localized(
        val resource: StringResource,
        val args: List<String> = emptyList(),
    ) : UiText

    /** Text that is already final (e.g. a file name or an exception message). */
    data class Raw(val value: String) : UiText
}

/** Resolves this text inside a composable. */
@Composable
fun UiText.resolve(): String = when (this) {
    is UiText.Raw -> value
    is UiText.Localized -> if (args.isEmpty()) {
        stringResource(resource)
    } else {
        stringResource(resource, *args.toTypedArray())
    }
}

/** Resolves this text outside composition (e.g. from a ViewModel coroutine). */
suspend fun UiText.resolveAsync(): String = when (this) {
    is UiText.Raw -> value
    is UiText.Localized -> if (args.isEmpty()) {
        getString(resource)
    } else {
        getString(resource, *args.toTypedArray())
    }
}
