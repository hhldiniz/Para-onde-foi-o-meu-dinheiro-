package com.hhldiniz.praondefoiomeudinheiro.presentation.screen.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hhldiniz.praondefoiomeudinheiro.data.chat.ModelFailure
import com.hhldiniz.praondefoiomeudinheiro.data.chat.ModelStatus
import com.hhldiniz.praondefoiomeudinheiro.domain.chat.ChatModel
import com.hhldiniz.praondefoiomeudinheiro.domain.chat.ChatRole
import com.hhldiniz.praondefoiomeudinheiro.domain.chat.DownloadProgress
import com.hhldiniz.praondefoiomeudinheiro.domain.model.resolve
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.BrutalBlack
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.BrutalCyan
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.BrutalPink
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.BrutalYellow
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.CyanContainer
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.HardShadowBox
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.LightBackground
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.LightSurface
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.NeoButton
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.NeoCard
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.NeoTag
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.RedContainer
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.YellowContainer
import com.hhldiniz.praondefoiomeudinheiro.resources.Res
import com.hhldiniz.praondefoiomeudinheiro.resources.action_back
import com.hhldiniz.praondefoiomeudinheiro.resources.action_cancel
import com.hhldiniz.praondefoiomeudinheiro.resources.chat_answer_interrupted
import com.hhldiniz.praondefoiomeudinheiro.resources.chat_btn_cancel_download
import com.hhldiniz.praondefoiomeudinheiro.resources.chat_btn_download
import com.hhldiniz.praondefoiomeudinheiro.resources.chat_btn_retry
import com.hhldiniz.praondefoiomeudinheiro.resources.chat_checking_model
import com.hhldiniz.praondefoiomeudinheiro.resources.chat_delete_model_body
import com.hhldiniz.praondefoiomeudinheiro.resources.chat_delete_model_confirm
import com.hhldiniz.praondefoiomeudinheiro.resources.chat_delete_model_title
import com.hhldiniz.praondefoiomeudinheiro.resources.chat_disclaimer
import com.hhldiniz.praondefoiomeudinheiro.resources.chat_download_hint
import com.hhldiniz.praondefoiomeudinheiro.resources.chat_download_info
import com.hhldiniz.praondefoiomeudinheiro.resources.chat_download_progress
import com.hhldiniz.praondefoiomeudinheiro.resources.chat_downloading_title
import com.hhldiniz.praondefoiomeudinheiro.resources.chat_empty_title
import com.hhldiniz.praondefoiomeudinheiro.resources.chat_error_detail
import com.hhldiniz.praondefoiomeudinheiro.resources.chat_error_download
import com.hhldiniz.praondefoiomeudinheiro.resources.chat_error_load
import com.hhldiniz.praondefoiomeudinheiro.resources.chat_error_storage
import com.hhldiniz.praondefoiomeudinheiro.resources.chat_input_hint
import com.hhldiniz.praondefoiomeudinheiro.resources.chat_intro_body
import com.hhldiniz.praondefoiomeudinheiro.resources.chat_intro_title
import com.hhldiniz.praondefoiomeudinheiro.resources.chat_loading_model
import com.hhldiniz.praondefoiomeudinheiro.resources.chat_menu_content_description
import com.hhldiniz.praondefoiomeudinheiro.resources.chat_menu_delete_model
import com.hhldiniz.praondefoiomeudinheiro.resources.chat_menu_new_conversation
import com.hhldiniz.praondefoiomeudinheiro.resources.chat_model_name
import com.hhldiniz.praondefoiomeudinheiro.resources.chat_role_assistant
import com.hhldiniz.praondefoiomeudinheiro.resources.chat_role_user
import com.hhldiniz.praondefoiomeudinheiro.resources.chat_send_content_description
import com.hhldiniz.praondefoiomeudinheiro.resources.chat_stop_content_description
import com.hhldiniz.praondefoiomeudinheiro.resources.chat_suggestion_investments
import com.hhldiniz.praondefoiomeudinheiro.resources.chat_suggestion_saving
import com.hhldiniz.praondefoiomeudinheiro.resources.chat_suggestion_spending
import com.hhldiniz.praondefoiomeudinheiro.resources.chat_thinking
import com.hhldiniz.praondefoiomeudinheiro.resources.chat_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * The assistant chat. Until the model is on the device the screen explains
 * what it is and offers the one-time download; once it is loaded, it is a
 * plain conversation, with a few suggested questions to start from.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showMenu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val languageTag = Locale.current.toLanguageTag()
    LaunchedEffect(languageTag) { viewModel.onLanguage(languageTag) }

    val modelOnDevice = state.status.let {
        it is ModelStatus.Downloaded || it is ModelStatus.Loading || it is ModelStatus.Ready ||
            (it is ModelStatus.Failed && it.failure == ModelFailure.LOAD)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.chat_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.action_back),
                        )
                    }
                },
                actions = {
                    if (modelOnDevice) {
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(
                                    imageVector = Icons.Filled.MoreVert,
                                    contentDescription = stringResource(Res.string.chat_menu_content_description),
                                )
                            }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(Res.string.chat_menu_new_conversation)) },
                                    enabled = state.messages.isNotEmpty(),
                                    onClick = {
                                        showMenu = false
                                        viewModel.onNewConversation()
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(Res.string.chat_menu_delete_model)) },
                                    onClick = {
                                        showMenu = false
                                        confirmDelete = true
                                    },
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BrutalCyan,
                    titleContentColor = BrutalBlack,
                    navigationIconContentColor = BrutalBlack,
                    actionIconContentColor = BrutalBlack,
                ),
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .imePadding(),
        ) {
            when (val status = state.status) {
                ModelStatus.Checking -> Waiting(stringResource(Res.string.chat_checking_model))
                ModelStatus.Downloaded, ModelStatus.Loading -> Waiting(stringResource(Res.string.chat_loading_model))
                ModelStatus.NotDownloaded -> SetupContent(state.model, failure = null, onDownload = viewModel::onDownload)
                is ModelStatus.Downloading -> DownloadingContent(status.progress, onCancel = viewModel::onCancelDownload)
                is ModelStatus.Failed -> if (status.failure == ModelFailure.LOAD) {
                    FailureContent(status, state.model, onRetry = viewModel::onRetry)
                } else {
                    SetupContent(state.model, failure = status, onDownload = viewModel::onRetry)
                }
                ModelStatus.Ready -> ConversationContent(
                    state = state,
                    onInputChanged = viewModel::onInputChanged,
                    onSend = viewModel::send,
                    onStop = viewModel::onStop,
                    onSuggestion = viewModel::onSuggestion,
                )
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = {
                Text(
                    text = stringResource(Res.string.chat_delete_model_title),
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            text = {
                Text(
                    text = stringResource(Res.string.chat_delete_model_body, formatByteSize(state.model.sizeBytes)),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.onDeleteModel()
                }) {
                    Text(
                        text = stringResource(Res.string.chat_delete_model_confirm),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(Res.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun Waiting(message: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(color = BrutalBlack)
        Spacer(Modifier.height(16.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
    }
}

/** First visit: what the assistant is, what will be downloaded, and the button to do it. */
@Composable
private fun SetupContent(model: ChatModel, failure: ModelStatus.Failed?, onDownload: () -> Unit) {
    val size = formatByteSize(model.sizeBytes)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        NeoCard(backgroundColor = CyanContainer) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(Res.string.chat_intro_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                )
                Text(stringResource(Res.string.chat_intro_body), style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(Res.string.chat_download_info, size), style = MaterialTheme.typography.bodyMedium)
                NeoTag(
                    text = stringResource(Res.string.chat_model_name, model.displayName, model.license),
                    backgroundColor = LightSurface,
                    textColor = BrutalBlack,
                )
            }
        }

        if (failure != null) ErrorCard(failure, model)

        NeoButton(
            onClick = onDownload,
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = BrutalPink,
            text = if (failure != null) {
                stringResource(Res.string.chat_btn_retry)
            } else {
                stringResource(Res.string.chat_btn_download, size)
            },
        )
    }
}

@Composable
private fun DownloadingContent(progress: DownloadProgress, onCancel: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        NeoCard(backgroundColor = YellowContainer) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(Res.string.chat_downloading_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                )
                LinearProgressIndicator(
                    progress = { progress.fraction },
                    modifier = Modifier.fillMaxWidth().height(12.dp).border(2.dp, BrutalBlack, RectangleShape),
                    color = BrutalPink,
                    trackColor = LightSurface,
                    drawStopIndicator = {},
                )
                Text(
                    text = stringResource(
                        Res.string.chat_download_progress,
                        formatByteSize(progress.downloadedBytes),
                        formatByteSize(progress.totalBytes),
                        (progress.fraction * 100).toInt(),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(stringResource(Res.string.chat_download_hint), style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.height(24.dp))
        NeoButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = LightSurface,
            textColor = BrutalBlack,
            text = stringResource(Res.string.chat_btn_cancel_download),
        )
    }
}

@Composable
private fun FailureContent(failure: ModelStatus.Failed, model: ChatModel, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        ErrorCard(failure, model)
        Spacer(Modifier.height(24.dp))
        NeoButton(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = BrutalPink,
            text = stringResource(Res.string.chat_btn_retry),
        )
    }
}

@Composable
private fun ErrorCard(failure: ModelStatus.Failed, model: ChatModel) {
    NeoCard(backgroundColor = RedContainer) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = when (failure.failure) {
                    ModelFailure.DOWNLOAD -> stringResource(Res.string.chat_error_download)
                    ModelFailure.STORAGE -> stringResource(Res.string.chat_error_storage, formatByteSize(model.sizeBytes))
                    ModelFailure.LOAD -> stringResource(Res.string.chat_error_load)
                },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
            )
            failure.detail?.takeIf { it.isNotBlank() }?.let {
                Text(stringResource(Res.string.chat_error_detail, it), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ConversationContent(
    state: ChatUiState,
    onInputChanged: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onSuggestion: (String) -> Unit,
) {
    val listState = rememberLazyListState()
    val last = state.messages.lastOrNull()
    // Follow the answer as it is written.
    LaunchedEffect(state.messages.size, last?.text?.length) {
        if (state.messages.isNotEmpty()) listState.scrollToItem(state.messages.lastIndex)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (state.messages.isEmpty()) {
            Suggestions(modifier = Modifier.weight(1f), onSuggestion = onSuggestion)
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(state.messages, key = { it.id }) { bubble ->
                    MessageBubble(bubble, thinking = state.isThinking && bubble.isStreaming)
                }
            }
        }

        state.error?.let { error ->
            Text(
                text = error.resolve(),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(RedContainer)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
            )
        }

        InputBar(
            input = state.input,
            isGenerating = state.isGenerating,
            canSend = state.canSend,
            onInputChanged = onInputChanged,
            onSend = onSend,
            onStop = onStop,
        )
        Text(
            text = stringResource(Res.string.chat_disclaimer),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
        )
    }
}

@Composable
private fun Suggestions(modifier: Modifier, onSuggestion: (String) -> Unit) {
    val suggestions = listOf(
        stringResource(Res.string.chat_suggestion_spending),
        stringResource(Res.string.chat_suggestion_saving),
        stringResource(Res.string.chat_suggestion_investments),
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
    ) {
        Text(
            text = stringResource(Res.string.chat_empty_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
        )
        for (suggestion in suggestions) {
            HardShadowBox(offsetX = 3.dp, offsetY = 3.dp, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = suggestion,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(2.dp, BrutalBlack, RectangleShape)
                        .background(LightSurface)
                        .clickable { onSuggestion(suggestion) }
                        .padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun MessageBubble(bubble: ChatBubble, thinking: Boolean) {
    val fromUser = bubble.role == ChatRole.USER
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = if (fromUser) Alignment.CenterEnd else Alignment.CenterStart) {
        HardShadowBox(offsetX = 3.dp, offsetY = 3.dp, modifier = Modifier.widthIn(max = 560.dp)) {
            Column(
                modifier = Modifier
                    .border(2.dp, BrutalBlack, RectangleShape)
                    .background(if (fromUser) BrutalYellow else LightSurface)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = stringResource(if (fromUser) Res.string.chat_role_user else Res.string.chat_role_assistant),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                )
                if (thinking && bubble.text.isEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = BrutalBlack)
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(Res.string.chat_thinking), style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    Text(
                        text = if (bubble.isStreaming) bubble.text + " ▍" else bubble.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = BrutalBlack,
                    )
                }
                if (bubble.failed) {
                    Text(
                        text = stringResource(Res.string.chat_answer_interrupted),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun InputBar(
    input: String,
    isGenerating: Boolean,
    canSend: Boolean,
    onInputChanged: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(start = 16.dp, end = 8.dp, top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextField(
            value = input,
            onValueChange = onInputChanged,
            modifier = Modifier.weight(1f).border(2.dp, BrutalBlack, RectangleShape),
            placeholder = { Text(stringResource(Res.string.chat_input_hint)) },
            maxLines = 4,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { if (canSend) onSend() }),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = BrutalBlack),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = LightBackground,
                unfocusedContainerColor = LightBackground,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
        )
        if (isGenerating) {
            IconButton(onClick = onStop) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(Res.string.chat_stop_content_description),
                )
            }
        } else {
            IconButton(onClick = onSend, enabled = canSend) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(Res.string.chat_send_content_description),
                )
            }
        }
    }
}

/** `1.1 GB`, `350 MB`: decimal units, one decimal for gigabytes, as storage settings show them. */
internal fun formatByteSize(bytes: Long): String {
    val gigabyte = 1_000_000_000L
    val megabyte = 1_000_000L
    return when {
        bytes >= gigabyte -> {
            val tenths = (bytes * 10 + gigabyte / 2) / gigabyte
            "${tenths / 10}.${tenths % 10} GB"
        }
        bytes >= megabyte -> "${(bytes + megabyte / 2) / megabyte} MB"
        else -> "${(bytes + 999) / 1000} KB"
    }
}
