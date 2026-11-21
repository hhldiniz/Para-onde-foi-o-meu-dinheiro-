// Service worker for the web (wasmJs) build — the half of the PWA that makes
// the app start offline. It is registered by pwa.js and, like every other file
// in this folder, is copied verbatim into the distribution, so its scope is the
// deployed site's own directory (`/Para-onde-foi-o-meu-dinheiro-/` on GitHub
// Pages, `/` behind a custom domain). Everything below is therefore addressed
// relatively; nothing may assume the site sits at the origin's root.
//
// There is nothing to sync and no backend: entries live in localStorage (see
// data/local/web/), so once the app shell is cached the whole app works with
// the network off. The assistant chat's model, downloaded once from Hugging
// Face, is the one thing that comes from elsewhere — see the cross-origin note
// in the fetch handler.

const CACHE = "praonde-shell-v1";

// The compiled bundle is deliberately absent from this list: webpack names the
// wasm file (and any chunk it splits out) with a content hash that changes on
// every build, and Compose resources are fetched lazily by name at runtime. A
// precache list would have to be generated at build time to stay honest; the
// fetch handler below caches all of it on first use instead, which needs no
// build step and cannot go stale.
const SHELL = [
    "./",
    "./manifest.webmanifest",
    "./debug.js",
    "./icon.svg",
    "./icon-192.png",
    "./icon-512.png",
    "./icon-maskable-192.png",
    "./icon-maskable-512.png",
    "./apple-touch-icon.png",
    "./favicon-32.png",
];

// The base-14 font data pdf.js needs for any PDF that does not embed its
// fonts. Written out in full rather than built from a list of names, so every
// file this worker pre-caches can be found in it by searching for its path —
// which is how PwaAssetsTest checks the list against what is actually shipped.
const PDFJS_STANDARD_FONTS = [
    "./vendor/pdfjs/standard_fonts/FoxitDingbats.pfb",
    "./vendor/pdfjs/standard_fonts/FoxitFixed.pfb",
    "./vendor/pdfjs/standard_fonts/FoxitFixedBold.pfb",
    "./vendor/pdfjs/standard_fonts/FoxitFixedBoldItalic.pfb",
    "./vendor/pdfjs/standard_fonts/FoxitFixedItalic.pfb",
    "./vendor/pdfjs/standard_fonts/FoxitSerif.pfb",
    "./vendor/pdfjs/standard_fonts/FoxitSerifBold.pfb",
    "./vendor/pdfjs/standard_fonts/FoxitSerifBoldItalic.pfb",
    "./vendor/pdfjs/standard_fonts/FoxitSerifItalic.pfb",
    "./vendor/pdfjs/standard_fonts/FoxitSymbol.pfb",
    "./vendor/pdfjs/standard_fonts/LiberationSans-Bold.ttf",
    "./vendor/pdfjs/standard_fonts/LiberationSans-BoldItalic.ttf",
    "./vendor/pdfjs/standard_fonts/LiberationSans-Italic.ttf",
    "./vendor/pdfjs/standard_fonts/LiberationSans-Regular.ttf",
];

/**
 * The import engines, vendored under `vendor/` so they are same-origin and can
 * be cached at all (see `vendor/README.md`). Around 13MB in total, which is
 * why they are not in [SHELL]: they are fetched in the background, well after
 * the app is up, and only when the page asks for it — `pwa.js` skips the whole
 * thing on a metered connection. Anything missed here still lands in the cache
 * the first time it is genuinely used, online.
 *
 * The cmaps (`vendor/pdfjs/cmaps/`) are deliberately absent: 169 files that
 * only a PDF with a CJK encoding ever touches. They stay a normal, online
 * fetch.
 *
 * So is the assistant chat's engine (`vendor/wllama/`, `vendor/wllama-compat/`,
 * ~25MB): it is useless without a model the user has to download first — a
 * gigabyte, online, from Hugging Face — so pre-fetching it for everyone would
 * spend a phone's data on a feature most users may never open. It lands in
 * the cache through [handleAsset] the first time the chat runs, and the chat
 * works offline from then on.
 */
const OFFLINE_LIBRARIES = [
    "./vendor/pdfjs/pdf.min.mjs",
    "./vendor/pdfjs/pdf.worker.min.mjs",
    ...PDFJS_STANDARD_FONTS,
    "./vendor/tesseract/tesseract.esm.min.js",
    "./vendor/tesseract/worker.min.js",
    "./vendor/tesseract-core/tesseract-core-simd-lstm.wasm.js",
    "./vendor/tessdata/por.traineddata.gz",
    "./vendor/tessdata/eng.traineddata.gz",
    "./vendor/tessdata/spa.traineddata.gz",
];

self.addEventListener("install", (event) => {
    event.waitUntil(
        caches
            .open(CACHE)
            // Individually, so one 404 (an icon renamed and this list not
            // updated) cannot leave the app with no cached shell at all.
            .then((cache) => Promise.allSettled(SHELL.map((url) => cache.add(url))))
            .then(() => self.skipWaiting())
    );
});

self.addEventListener("activate", (event) => {
    event.waitUntil(
        caches
            .keys()
            .then((names) => Promise.all(names.filter((name) => name !== CACHE).map((name) => caches.delete(name))))
            // Takes over the pages that were open when this worker installed,
            // so the app is offline-ready from the first visit rather than the
            // second.
            .then(() => self.clients.claim())
    );
});

/**
 * The page itself: network first, so a fresh deploy is picked up on the next
 * launch instead of a launch later, falling back to whatever was cached when
 * the network is gone. `./` is the last resort because start_url is `./` but a
 * user may well have bookmarked `./index.html`, and both serve the same shell.
 */
async function handleNavigation(request) {
    return networkFirst(request, "./");
}

/**
 * The scripts the page loads from the site's root — the bridges
 * (`llm-chat.js`, `pdf-extract.js`, `ocr-extract.js`), `pwa.js`, `debug.js`
 * and the compiled bundle's loader — are network first as well. Their names
 * carry no content hash, so served stale-while-revalidate a deploy would only
 * reach them a launch after the page itself: the first launch would run new
 * Kotlin against an old bridge, and a fix in a bridge would look like it had
 * not shipped. They are small, and GitHub Pages' ETags turn the check into a
 * 304 when nothing changed.
 *
 * `vendor/` is deliberately not included: a vendored engine's script and its
 * wasm change together, and fetching one fresh while the other comes from the
 * cache would pair two versions that were never meant to run together.
 */
function isRootScript(url) {
    if (!url.href.startsWith(self.registration.scope)) return false;
    const path = url.pathname.slice(new URL(self.registration.scope).pathname.length);
    return !path.includes("/") && /\.m?js$/.test(path);
}

/** Fetches [request] and caches it, falling back to the cache — then to [fallback] — offline. */
async function networkFirst(request, fallback) {
    const cache = await caches.open(CACHE);
    try {
        const response = await fetch(request);
        if (isCacheable(response)) cache.put(request, response.clone());
        return response;
    } catch (error) {
        const cached = (await cache.match(request)) || (fallback ? await cache.match(fallback) : undefined);
        if (cached) return cached;
        throw error;
    }
}

/**
 * Only plain, complete, same-origin successes go into the cache. A 206 is what
 * a ranged request for the wasm binary comes back as, and `cache.put` rejects
 * those outright rather than storing half a file.
 */
function isCacheable(response) {
    return response.status === 200 && response.type !== "opaque";
}

/**
 * Everything else the app loads from its own origin (the JS glue, the wasm
 * binary, Compose resources, icons): serve the cached copy immediately and
 * refresh it in the background, so startup never waits on the network and a
 * deploy still lands within one launch.
 */
async function handleAsset(event) {
    const cache = await caches.open(CACHE);
    const cached = await cache.match(event.request);
    const network = fetch(event.request).then((response) => {
        if (isCacheable(response)) cache.put(event.request, response.clone());
        return response;
    });

    if (cached) {
        // The refresh outlives the response, so the worker has to be kept
        // awake for it; without this it can be killed mid-write and the cache
        // never moves off the first version it saw. Offline it simply fails,
        // which is not an error worth surfacing — the cached copy already went
        // out.
        event.waitUntil(network.catch(() => {}));
        return cached;
    }
    return network;
}

/**
 * Fills the cache with [OFFLINE_LIBRARIES], one file at a time so a background
 * download of a dozen megabytes never competes with whatever the user is
 * actually doing. Already-cached files are skipped, which makes this safe to
 * ask for on every launch and lets an interrupted run pick up where it left
 * off: the worker can be killed at any point, and the next launch finishes the
 * rest.
 */
async function cacheOfflineLibraries() {
    const cache = await caches.open(CACHE);
    for (const path of OFFLINE_LIBRARIES) {
        if (await cache.match(path)) continue;
        try {
            await cache.add(path);
        } catch (error) {
            // Offline again, or a file that moved without this list being
            // updated. Neither is worth failing the rest of the run for: the
            // import features still work online, and PwaAssetsTest is what
            // catches the second case before it ships.
            console.warn("could not pre-cache", path, error);
        }
    }
}

self.addEventListener("message", (event) => {
    if (event.data && event.data.type === "praonde:cache-offline-libraries") {
        event.waitUntil(cacheOfflineLibraries());
    }
});

self.addEventListener("fetch", (event) => {
    const request = event.request;
    if (request.method !== "GET") return;

    // Chrome's devtools issue `only-if-cached` requests with mode "no-cors";
    // passing one to fetch() throws a TypeError, so leave those to the browser.
    if (request.cache === "only-if-cached" && request.mode !== "same-origin") return;

    const url = new URL(request.url);
    // The only cross-origin request the app makes is the assistant chat's
    // one-time model download from Hugging Face (llm-chat.js), a gigabyte that
    // wllama streams straight into the origin-private file system and keeps
    // there itself. It must not go through this cache — duplicating it in
    // Cache Storage would double its footprint — so cross-origin requests are
    // left to the browser.
    if (url.origin !== self.location.origin) return;

    if (request.mode === "navigate") {
        event.respondWith(handleNavigation(request));
        return;
    }
    if (isRootScript(url)) {
        event.respondWith(networkFirst(request));
        return;
    }
    event.respondWith(handleAsset(event));
});
