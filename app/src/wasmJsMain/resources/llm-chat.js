// Bridges the assistant chat's on-device model for the wasmJs build, used by
// WllamaRuntime.kt. Android and iOS run llama.cpp natively through Llamatik;
// the web has no native code to call, so this loads wllama — llama.cpp
// compiled to WebAssembly — lazily, on the first visit to the chat, exactly
// like pdf-extract.js does for pdf.js. A user who never opens the chat never
// loads any of it.
//
// wllama is served from this site, out of `vendor/` (see `vendor/README.md`),
// not from a CDN. The one thing that does come from another origin is the
// model itself: a GGUF file downloaded once from Hugging Face, after the user
// asks for it, and then kept in the browser's origin-private file system
// (OPFS) by wllama's own cache — the chat runs offline from then on.
//
// Every failure leaves here wrapped with the library's name, for the same
// reason as the other bridges: the Kotlin side surfaces an error's message,
// and a bare engine message names neither the file nor what was being done.
const WLLAMA_VERSION = "3.8.1";
// Resolved against this module's own URL, not the page's, so the paths hold
// wherever the site is deployed; they are handed to a Web Worker, which would
// resolve relative URLs against itself.
const WLLAMA_MODULE = new URL("./vendor/wllama/index.js", import.meta.url);
const WLLAMA_WASM = new URL("./vendor/wllama/wllama.wasm", import.meta.url);
// The build for browsers without JSPI or 64-bit wasm memory — Safari, today.
// wllama would otherwise fetch it from jsDelivr.
const WLLAMA_COMPAT_WORKER = new URL("./vendor/wllama-compat/wllama.js", import.meta.url);
const WLLAMA_COMPAT_WASM = new URL("./vendor/wllama-compat/wllama.wasm", import.meta.url);

// Prefix WllamaRuntime.kt recognizes in an error message.
const NOT_ENOUGH_STORAGE = "praonde:not-enough-storage";

let modulePromise = null;
let wllama = null;
let loadedUrl = null;
let downloadAbort = null;
let completionAbort = null;
// Bumped by every load and unload, so a load that was abandoned (the app gave
// up waiting and asked again, or left the chat) cannot mark itself loaded
// after a newer call has already replaced its instance.
let loadGeneration = 0;
// Set while a load is in flight. wllama hands a crash of its Web Worker (the
// browser killing it for using too much memory, typically) to nothing but its
// logger — `worker.onerror = logger.error` — and leaves the load's promise
// pending forever, which the app would show as "loading" for good.
let onWorkerCrash = null;
// Set once the default build has failed to start in this browser. wllama
// only falls back to its compat build when `WebAssembly.Suspending` or 64-bit
// memory is missing, and Safari 27 has both — yet cannot start that build,
// so the bridge makes the fallback itself.
let defaultBuildFailure = null;

function loadModule() {
    if (!modulePromise) {
        modulePromise = import(WLLAMA_MODULE.href).catch((error) => {
            // Don't cache a failure: one bad moment would otherwise disable
            // the chat for the rest of the session.
            modulePromise = null;
            throw new Error(`could not load wllama from ${WLLAMA_MODULE.href}: ${(error && error.message) || error}`);
        });
    }
    return modulePromise;
}

async function instance() {
    if (!wllama) {
        const { Wllama } = await loadModule();
        wllama = new Wllama(
            { default: WLLAMA_WASM.href },
            {
                // llama.cpp is chatty; the app has nowhere to show its log.
                suppressNativeLog: true,
                logger: {
                    debug() {},
                    log() {},
                    warn: console.warn,
                    error: (...args) => {
                        console.error(...args);
                        // The worker's error event, as opposed to a message.
                        if (args[0] instanceof Event && onWorkerCrash) onWorkerCrash(args[0]);
                    },
                },
                // A downloaded model must load with the network off.
                allowOffline: true,
            },
        );
        wllama.setCompat({ worker: WLLAMA_COMPAT_WORKER.href, wasm: WLLAMA_COMPAT_WASM.href });
    }
    return wllama;
}

// Drops the current instance, terminating its worker. Also the only way out of
// a load that failed or was abandoned halfway: wllama keeps the half-started
// worker of a failed load, and refuses every later `loadModel` on that
// instance with "Module is already initialized" — so without this, "try
// again" could never work.
async function discardInstance() {
    const current = wllama;
    wllama = null;
    loadedUrl = null;
    if (current) {
        try {
            await current.exit();
        } catch (_) {
            // Terminating a worker that already died can fail; it is gone either way.
        }
    }
}

async function findModel(url) {
    const models = await (await instance()).modelManager.getModels();
    return models.find((model) => model.url === url) || null;
}

// One attempt at loading the model, on wllama's default build or (`compat`)
// on the compat one.
async function loadOnce(url, contextLength, generation, compat) {
    // Whatever is there — another model, or a load that failed or is
    // still stuck — is thrown away; a fresh instance is cheaper to reason
    // about than one that has been torn down halfway.
    await discardInstance();

    const model = await findModel(url);
    if (!model) throw new Error(`the model ${url} is not downloaded`);
    const engine = await instance();
    if (compat) {
        // What `getWorkerResources` returns when wllama itself decides
        // the browser needs the compat build.
        engine.getWorkerResources = () => ({
            wasmPath: WLLAMA_COMPAT_WASM.href,
            jsPath: WLLAMA_COMPAT_WORKER.href,
            compat: true,
        });
    }
    if (generation !== loadGeneration) throw new Error("the load was superseded by a newer one");
    try {
        await new Promise((resolve, reject) => {
            onWorkerCrash = (event) => {
                const detail = event && event.message ? `: ${event.message}` : "";
                reject(new Error(`wllama ${WLLAMA_VERSION}'s worker stopped while loading the model${detail}`));
            };
            engine.loadModel(model, {
                n_ctx: contextLength,
                n_batch: 512,
                // CPU only, like the native runtimes. wllama's default is
                // to offload every layer to WebGPU wherever the browser
                // exposes it, which on a phone means a gigabyte of
                // weights pushed into GPU buffers on top of the copy in
                // the worker — the heaviest way there is to load it.
                n_gpu_layers: 0,
                // One conversation at a time; wllama prepares four slots
                // by default.
                n_parallel: 1,
            }).then(resolve, reject);
        });
    } catch (error) {
        if (generation === loadGeneration) await discardInstance();
        // Marks a failure of the engine itself — the only kind another
        // build might not have.
        throw Object.assign(error instanceof Error ? error : new Error(`${error}`), { fromEngine: true });
    } finally {
        if (generation === loadGeneration) onWorkerCrash = null;
    }
    if (generation !== loadGeneration) throw new Error("the load was superseded by a newer one");
    loadedUrl = url;
}

function describeFailure(error, action) {
    const message = (error && error.message) || `${error}`;
    if (message.startsWith(NOT_ENOUGH_STORAGE) || message.includes("wllama")) return message;
    return `wllama ${WLLAMA_VERSION} failed ${action}: ${message}`;
}

// Every entry point resolves — never rejects — with a string: the result
// (or "ok"), or FAILED followed by the message. How a rejected promise's
// message reaches Kotlin/Wasm has changed between Kotlin releases; a string
// result is read the same way by all of them.
const FAILED = "praonde:failed:";

function wrap(action, run) {
    return async (...args) => {
        try {
            const result = await run(...args);
            return typeof result === "string" ? result : "ok";
        } catch (error) {
            return FAILED + describeFailure(error, action);
        }
    };
}

function isAbort(error) {
    return error && (error.name === "AbortError" || /abort/i.test(error.message || ""));
}

window.praOndeLlm = {
    /** "true" when the model at `url` is fully in the OPFS cache. */
    isDownloaded: wrap("checking the model", async (url) => {
        const model = await findModel(url);
        return model !== null && model.validate() === "valid" ? "true" : "false";
    }),

    /**
     * Downloads the model into the OPFS cache, reporting `(loaded, total)`
     * bytes. Resolves when the file is complete; `cancelDownload` makes it
     * resolve as a failure (an AbortError).
     */
    download: wrap("downloading the model", async (url, size, onProgress) => {
        // Ask for persistent storage so the browser does not evict a gigabyte
        // the user waited for the moment space gets tight. Best effort: a
        // browser may say no, and that is not a reason to stop.
        try {
            if (navigator.storage && navigator.storage.persist) await navigator.storage.persist();
        } catch (_) {
            // ignored
        }
        if (navigator.storage && navigator.storage.estimate) {
            const { quota, usage } = await navigator.storage.estimate();
            if (typeof quota === "number" && typeof usage === "number" && quota - usage < size) {
                throw new Error(`${NOT_ENOUGH_STORAGE}:${size}:${Math.max(0, quota - usage)}`);
            }
        }

        downloadAbort = new AbortController();
        try {
            await (await instance()).modelManager.downloadModel(url, {
                signal: downloadAbort.signal,
                progressCallback: ({ loaded, total }) => onProgress(loaded, total || size),
            });
        } finally {
            downloadAbort = null;
        }
    }),

    cancelDownload() {
        if (downloadAbort) downloadAbort.abort();
    },

    remove: wrap("removing the model", async (url) => {
        const model = await findModel(url);
        if (model) await model.remove();
    }),

    /**
     * Loads the cached model at `url` with a context window of `contextLength`
     * tokens. When the default build fails, tries once more on the compat
     * build, and sticks to it for the rest of the session.
     */
    load: wrap("loading the model", async (url, contextLength) => {
        if (loadedUrl === url && wllama && wllama.isModelLoaded()) return;
        const generation = ++loadGeneration;
        if (defaultBuildFailure === null) {
            try {
                await loadOnce(url, contextLength, generation, false);
                return;
            } catch (error) {
                if (generation !== loadGeneration || !(error && error.fromEngine)) throw error;
                defaultBuildFailure = (error && error.message) || `${error}`;
                console.warn(`wllama's default build failed, retrying on the compat build: ${defaultBuildFailure}`);
            }
        }
        try {
            await loadOnce(url, contextLength, generation, true);
        } catch (error) {
            const message = (error && error.message) || `${error}`;
            throw new Error(`${message} (compat build; the default build failed with: ${defaultBuildFailure})`);
        }
    }),


    unload: wrap("unloading the model", async () => {
        loadGeneration++;
        onWorkerCrash = null;
        await discardInstance();
    }),

    /**
     * Streams the continuation of `prompt` piece by piece into `onPiece`.
     * Resolves when the answer is finished — or was stopped with
     * `abortCompletion`, which is not an error.
     */
    complete: wrap("writing an answer", async (prompt, optionsJson, onPiece) => {
        if (!wllama || !wllama.isModelLoaded()) throw new Error("the model is not loaded");
        const options = JSON.parse(optionsJson);
        completionAbort = new AbortController();
        try {
            await wllama.createCompletion({
                prompt,
                stream: true,
                max_tokens: options.maxTokens,
                temperature: options.temperature,
                top_p: options.topP,
                top_k: options.topK,
                penalty_repeat: options.repeatPenalty,
                stop: options.stop,
                abortSignal: completionAbort.signal,
                onData: (chunk) => {
                    const text = chunk && chunk.choices && chunk.choices[0] && chunk.choices[0].text;
                    if (text) onPiece(text);
                },
            });
        } catch (error) {
            if (!isAbort(error)) throw error;
        } finally {
            completionAbort = null;
        }
    }),

    abortCompletion() {
        if (completionAbort) completionAbort.abort();
    },
};
