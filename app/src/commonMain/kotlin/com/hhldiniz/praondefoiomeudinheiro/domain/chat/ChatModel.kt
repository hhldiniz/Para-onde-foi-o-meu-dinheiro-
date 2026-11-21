package com.hhldiniz.praondefoiomeudinheiro.domain.chat

/**
 * A language model the assistant chat can run on the device: one GGUF file on
 * Hugging Face, downloaded once and then read locally by llama.cpp (Llamatik
 * on Android/iOS, wllama on the web — see [LlmRuntime]).
 *
 * [revision] pins the exact commit of the repository, so the file that is
 * downloaded is always the one [sizeBytes] describes: a later re-upload under
 * the same name cannot change what the app runs, and a download whose size
 * does not match is known to be broken rather than "a newer version".
 */
data class ChatModel(
    /** Stable identifier, also used to name the file on disk. */
    val id: String,
    /** Name shown to the user. */
    val displayName: String,
    /** Hugging Face repository, `owner/name`. */
    val repository: String,
    /** Commit of [repository] the file is downloaded from. */
    val revision: String,
    /** GGUF file inside [repository]. */
    val fileName: String,
    /** Exact size of [fileName] at [revision], in bytes. */
    val sizeBytes: Long,
    /** Context window the app asks the runtime for, in tokens. */
    val contextLength: Int,
    /** How a conversation is rendered into the prompt this model was trained on. */
    val promptFormat: PromptFormat,
    /** SPDX identifier of the model's licence, shown next to its name. */
    val license: String,
) {
    /** Direct download URL of the pinned file. */
    val downloadUrl: String
        get() = "https://huggingface.co/$repository/resolve/$revision/$fileName"

    /** The model's page, for the user who wants to know what they are downloading. */
    val pageUrl: String
        get() = "https://huggingface.co/$repository"
}

/** The chat templates [com.hhldiniz.praondefoiomeudinheiro.data.chat.ChatPromptBuilder] knows how to write. */
enum class PromptFormat {
    /**
     * `<|im_start|>role\n...<|im_end|>` — Qwen's (and many others') format.
     * The prompt is rendered by the app rather than by the model's embedded
     * Jinja template so the two runtimes cannot disagree about it, and so it
     * is covered by the JVM unit tests.
     */
    CHATML,
}

object ChatModels {

    /**
     * Qwen2.5 1.5B Instruct, quantized to Q4_K_M (~1.1GB).
     *
     * The balance point for "runs on most modern devices": it needs roughly
     * 1.5GB of free memory and answers at a readable pace on a mid-range phone
     * from the last few years, while models below it (0.5B) lose the thread on
     * anything with numbers in it and models above it (3B+) are too big for a
     * large share of phones. It is also Apache-2.0 and ungated on Hugging
     * Face — so it downloads without an account — and handles Portuguese,
     * Spanish and English, the three languages this app is translated into.
     */
    val QWEN2_5_1_5B_INSTRUCT = ChatModel(
        id = "qwen2.5-1.5b-instruct-q4_k_m",
        displayName = "Qwen2.5 1.5B Instruct",
        repository = "Qwen/Qwen2.5-1.5B-Instruct-GGUF",
        revision = "91cad51170dc346986eccefdc2dd33a9da36ead9",
        fileName = "qwen2.5-1.5b-instruct-q4_k_m.gguf",
        sizeBytes = 1_117_320_736L,
        contextLength = 4096,
        promptFormat = PromptFormat.CHATML,
        license = "Apache-2.0",
    )

    /**
     * The same Qwen2.5 1.5B Instruct, quantized to Q2_K (~750MB) — the web
     * build's model.
     *
     * A browser cannot map the file the way llama.cpp does natively: wllama
     * reads every weight into the WebAssembly heap, and on an iPhone a 1.1GB
     * heap is enough for iOS to kill Safari's page. Q2_K is the same model in
     * a third less memory, at a visible cost in quality, which is why the
     * native builds keep [QWEN2_5_1_5B_INSTRUCT].
     */
    val QWEN2_5_1_5B_INSTRUCT_Q2_K = QWEN2_5_1_5B_INSTRUCT.copy(
        id = "qwen2.5-1.5b-instruct-q2_k",
        displayName = "Qwen2.5 1.5B Instruct (Q2_K)",
        fileName = "qwen2.5-1.5b-instruct-q2_k.gguf",
        sizeBytes = 752_880_160L,
    )

    /** The model the native builds (Android, iOS) use. */
    val DEFAULT: ChatModel = QWEN2_5_1_5B_INSTRUCT

    /** The model the web build uses; see [QWEN2_5_1_5B_INSTRUCT_Q2_K]. */
    val WEB: ChatModel = QWEN2_5_1_5B_INSTRUCT_Q2_K

    /**
     * Every model a build has ever downloaded. Whichever of them is not the
     * current one is deleted on sight ([com.hhldiniz.praondefoiomeudinheiro.data.chat.ChatModelManager.refresh]),
     * so switching models never leaves a gigabyte behind.
     */
    val ALL: List<ChatModel> = listOf(QWEN2_5_1_5B_INSTRUCT, QWEN2_5_1_5B_INSTRUCT_Q2_K)
}
