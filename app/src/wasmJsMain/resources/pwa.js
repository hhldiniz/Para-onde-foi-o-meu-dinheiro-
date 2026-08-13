// Registers sw.js, the service worker that makes the web build installable and
// able to start offline. Kept out of index.html so the registration, the
// escape hatch below and their rationale live in one readable place; kept out
// of Kotlin because it must run even if the wasm bundle fails to load, which is
// exactly when a cached shell matters.

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
