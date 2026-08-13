// Registers sw.js, the service worker that makes the web build installable and
// able to start offline. Kept out of index.html so the registration, the
// escape hatch below and their rationale live in one readable place; kept out
// of Kotlin because it must run even if the wasm bundle fails to load, which is
// exactly when a cached shell matters.

// It also keeps the install prompt for the app's own "install" button, which
// is the other reason this file cannot live in Kotlin: Chromium fires
// `beforeinstallprompt` once, early, while the wasm bundle is still
// downloading, and an unclaimed prompt is gone. So it is caught here and held
// until AppInstall.wasmJs.kt asks for it.

let deferredInstallPrompt = null;

/**
 * Tells the app that the answer to `praOndeCanInstall()` changed. A DOM event
 * rather than a callback registry because the Kotlin side can subscribe to it
 * with the same typed `addEventListener` it already uses for file inputs.
 */
function announceInstallAvailability() {
    window.dispatchEvent(new CustomEvent("praonde:installavailability"));
}

window.addEventListener("beforeinstallprompt", (event) => {
    // Holds back the browser's own install banner: the app offers the install
    // in its settings screen instead, and the banner would cover its UI.
    event.preventDefault();
    deferredInstallPrompt = event;
    announceInstallAvailability();
});

window.addEventListener("appinstalled", () => {
    deferredInstallPrompt = null;
    announceInstallAvailability();
});

/** True while there is a prompt to show — what the settings button is shown by. */
window.praOndeCanInstall = function () {
    return deferredInstallPrompt !== null;
};

/**
 * Shows the held prompt, and reports whether there was one to show. The
 * outcome deliberately goes unread: a prompt can only be used once, accepted
 * or not, and a browser that still wants the app installed fires
 * `beforeinstallprompt` again on a later visit. An accepted install arrives
 * back here as `appinstalled` anyway.
 */
window.praOndeInstall = function () {
    if (!deferredInstallPrompt) return false;

    const prompt = deferredInstallPrompt;
    deferredInstallPrompt = null;
    announceInstallAvailability();
    prompt.prompt();
    return true;
};

// `?sw=off` tears the worker down and drops its caches, then reloads without
// the flag. It exists for two situations: iterating with
// `wasmJsBrowserDevelopmentRun`, where a worker left over from a previous run
// would keep serving the previous build's bundle, and rescuing a browser whose
// cached shell somehow went bad — otherwise the only cure is a trip through
// devtools.
async function unregisterServiceWorker() {
    const registrations = await navigator.serviceWorker.getRegistrations();
    await Promise.all(registrations.map((registration) => registration.unregister()));
    const names = await caches.keys();
    await Promise.all(names.map((name) => caches.delete(name)));

    const url = new URL(window.location.href);
    url.searchParams.delete("sw");
    window.location.replace(url.toString());
}

if ("serviceWorker" in navigator) {
    if (new URLSearchParams(window.location.search).get("sw") === "off") {
        unregisterServiceWorker().catch((error) => {
            console.warn("could not unregister the service worker:", error);
        });
    } else {
        // After `load`, so registering never competes with the wasm bundle for
        // the connection on a first, uncached visit.
        window.addEventListener("load", () => {
            navigator.serviceWorker.register("./sw.js").catch((error) => {
                // Not fatal: without a worker the app simply always needs the
                // network. Reasons range from a browser with workers disabled
                // to the site being served over plain HTTP.
                console.warn("could not register the service worker:", error);
            });
        });
    }
}
