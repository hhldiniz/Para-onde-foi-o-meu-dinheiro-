// Startup diagnostics for the web build — the only way to see why the app
// fails on a device with no developer tools at hand (an iPhone, above all an
// installed home-screen app, which has no address bar and no Web Inspector
// without a Mac).
//
// Loaded first, as a classic script in <head>, so it is listening before
// anything else runs. It records, quietly:
//   - uncaught errors and unhandled promise rejections (the wasm bundle's
//     failures surface as one or the other),
//   - scripts, wasm and resources that failed to load,
//   - console.error / console.warn,
//   - WebGL contexts the browser lost or refused (Compose draws through
//     WebGL; a white page with no error is usually this),
//   - what the browser supports: WebGL2, WasmGC-era features, memory, whether
//     it runs installed and under which service worker.
//
// The panel opens by itself when something actually broke (an uncaught error,
// a lost WebGL context, a resource the app needs failing to load, or the app
// never putting its canvas on screen), and always with `?debug=1`. It has a
// button to copy the report and one to drop the service worker and its caches
// and reload — the escape hatch an installed app otherwise lacks, since
// opening `?sw=off` in Safari does not reach a home-screen app's storage.
(function () {
    "use strict";

    var started = Date.now();
    var entries = [];
    var panel = null;
    var list = null;
    var forced = /[?&]debug=1\b/.test(location.search);

    function stamp() {
        return ((Date.now() - started) / 1000).toFixed(1) + "s";
    }

    function text(value) {
        if (value === undefined) return "undefined";
        if (value === null) return "null";
        if (value instanceof Error) return value.name + ": " + value.message + (value.stack ? "\n" + value.stack : "");
        if (typeof value === "object") {
            try {
                return JSON.stringify(value);
            } catch (_) {
                return String(value);
            }
        }
        return String(value);
    }

    function record(kind, message, serious) {
        entries.push("[" + stamp() + "] " + kind + ": " + message);
        if (entries.length > 200) entries.shift();
        if (serious) show();
        render();
    }

    // --- what goes wrong -------------------------------------------------

    window.addEventListener("error", function (event) {
        var target = event.target;
        if (target && target !== window && (target.src || target.href)) {
            // A <script>/<link>/<img> that failed to load: the event does not
            // bubble, hence the capture-phase listener.
            record("load failed", target.src || target.href, true);
            return;
        }
        var where = event.filename ? " (" + event.filename + ":" + event.lineno + ":" + event.colno + ")" : "";
        record("uncaught error", text(event.error || event.message) + where, true);
    }, true);

    window.addEventListener("unhandledrejection", function (event) {
        record("unhandled rejection", text(event.reason), true);
    });

    ["error", "warn"].forEach(function (level) {
        var original = console[level];
        console[level] = function () {
            try {
                var parts = [];
                for (var i = 0; i < arguments.length; i++) parts.push(text(arguments[i]));
                record("console." + level, parts.join(" "), false);
            } catch (_) {
                // never let the diagnostics break the logging they wrap
            }
            return original.apply(console, arguments);
        };
    });

    document.addEventListener("webglcontextlost", function () {
        record("webgl", "WebGL context lost", true);
    }, true);
    document.addEventListener("webglcontextcreationerror", function (event) {
        record("webgl", "WebGL context creation failed: " + (event.statusMessage || "no reason given"), true);
    }, true);

    // Failed fetches (wasm, Compose resources) do not raise an error event.
    if (window.fetch) {
        var originalFetch = window.fetch;
        window.fetch = function (input, init) {
            var url = typeof input === "string" ? input : (input && input.url) || String(input);
            return originalFetch.apply(this, arguments).then(function (response) {
                if (!response.ok) record("fetch", response.status + " " + url, response.status === 404);
                return response;
            }, function (error) {
                record("fetch failed", url + " — " + text(error), false);
                throw error;
            });
        };
    }

    // --- what the browser is -----------------------------------------------

    function environment() {
        var lines = [];
        lines.push("url: " + location.href);
        lines.push("userAgent: " + navigator.userAgent);
        lines.push("installed (standalone): " + (navigator.standalone === true || (window.matchMedia && matchMedia("(display-mode: standalone)").matches)));
        lines.push("viewport: " + innerWidth + "x" + innerHeight + " @" + (window.devicePixelRatio || 1) + "x");
        lines.push("deviceMemory: " + (navigator.deviceMemory || "n/a") + ", hardwareConcurrency: " + (navigator.hardwareConcurrency || "n/a"));
        lines.push("service worker: " + ("serviceWorker" in navigator ? (navigator.serviceWorker.controller ? "controlling " + navigator.serviceWorker.controller.scriptURL : "none controlling") : "unsupported"));
        lines.push("crossOriginIsolated: " + !!window.crossOriginIsolated);
        var wasm = typeof WebAssembly === "object";
        lines.push("WebAssembly: " + wasm + (wasm ? ", JSTag: " + !!WebAssembly.JSTag + ", Suspending: " + (typeof WebAssembly.Suspending) : ""));
        try {
            var gl = document.createElement("canvas").getContext("webgl2");
            if (!gl) {
                lines.push("WebGL2: unavailable");
            } else {
                lines.push("WebGL2: " + gl.getParameter(gl.VERSION) + " / " + gl.getParameter(gl.RENDERER) +
                    ", max texture " + gl.getParameter(gl.MAX_TEXTURE_SIZE));
                var lose = gl.getExtension("WEBGL_lose_context");
                if (lose) lose.loseContext();
            }
        } catch (error) {
            lines.push("WebGL2: threw " + text(error));
        }
        var canvases = findCanvases();
        lines.push("canvases: " + canvases.length + Array.prototype.map.call(canvases, function (c) {
            return " [" + c.width + "x" + c.height + "]";
        }).join(""));
        lines.push("loading placeholder still shown: " + !!document.getElementById("app-loading"));
        return lines.join("\n");
    }

    /** Canvases in the page, including inside the shadow root Compose renders into. */
    function findCanvases() {
        var found = Array.prototype.slice.call(document.getElementsByTagName("canvas"));
        var hosts = document.querySelectorAll("*");
        for (var i = 0; i < hosts.length; i++) {
            if (hosts[i].shadowRoot) {
                found = found.concat(Array.prototype.slice.call(hosts[i].shadowRoot.querySelectorAll("canvas")));
            }
        }
        return found;
    }

    function report() {
        return "Pra onde foi o meu dinheiro — diagnostics\n\n" + environment() + "\n\nevents:\n" +
            (entries.length ? entries.join("\n") : "(none)");
    }

    // --- the panel -----------------------------------------------------------

    function button(label, onClick) {
        var b = document.createElement("button");
        b.textContent = label;
        b.style.cssText = "font:600 13px -apple-system,system-ui,sans-serif;padding:8px 10px;margin:0 6px 6px 0;" +
            "border:2px solid #000;background:#EAB308;color:#000;border-radius:0;";
        b.addEventListener("click", onClick);
        return b;
    }

    function show() {
        if (panel) return;
        panel = document.createElement("div");
        panel.id = "praonde-debug";
        panel.style.cssText = "position:fixed;left:0;right:0;bottom:0;max-height:70%;z-index:2147483647;" +
            "background:#FFF5E6;color:#000;border-top:4px solid #000;padding:10px 10px calc(10px + env(safe-area-inset-bottom));" +
            "box-sizing:border-box;display:flex;flex-direction:column;";
        var actions = document.createElement("div");
        actions.appendChild(button("Copiar relatório", function () {
            var done = function () { record("debug", "report copied", false); };
            if (navigator.clipboard && navigator.clipboard.writeText) {
                navigator.clipboard.writeText(report()).then(done, function () { selectReport(); });
            } else {
                selectReport();
            }
        }));
        actions.appendChild(button("Limpar cache e recarregar", resetAndReload));
        actions.appendChild(button("Fechar", function () {
            panel.remove();
            panel = null;
        }));
        list = document.createElement("pre");
        list.style.cssText = "margin:0;overflow:auto;white-space:pre-wrap;word-break:break-word;" +
            "font:11px/1.35 ui-monospace,Menlo,monospace;-webkit-user-select:text;user-select:text;";
        panel.appendChild(actions);
        panel.appendChild(list);
        // On <html>, not <body>: ComposeViewport takes <body> over and drops
        // whatever else was in it.
        document.documentElement.appendChild(panel);
        render();
    }

    function selectReport() {
        if (!list) return;
        var range = document.createRange();
        range.selectNodeContents(list);
        var selection = window.getSelection();
        selection.removeAllRanges();
        selection.addRange(range);
    }

    function render() {
        if (list) list.textContent = report();
    }

    function resetAndReload() {
        var jobs = [];
        if ("serviceWorker" in navigator) {
            jobs.push(navigator.serviceWorker.getRegistrations().then(function (registrations) {
                return Promise.all(registrations.map(function (r) { return r.unregister(); }));
            }));
        }
        if (window.caches) {
            jobs.push(caches.keys().then(function (names) {
                return Promise.all(names.map(function (name) { return caches.delete(name); }));
            }));
        }
        Promise.all(jobs).then(function () {
            location.replace(location.pathname + (forced ? "?debug=1" : ""));
        }, function () {
            location.reload();
        });
    }

    // The app takes over the page by putting its canvas on it; if that never
    // happens, or the placeholder is still up, the start failed without
    // telling anyone.
    setTimeout(function () {
        var placeholder = document.getElementById("app-loading");
        var canvas = findCanvases()[0];
        if (placeholder) record("startup", "the app did not start within 20s (loading placeholder still shown)", true);
        else if (!canvas) record("startup", "the app removed its placeholder but never created its canvas", true);
        else render();
    }, 20000);

    // Five quick taps anywhere open the panel: an installed app has no address
    // bar to add `?debug=1` to, and a white page that threw nothing would
    // never open it on its own. Capture phase, because the app's canvas
    // consumes the touches before they would bubble here.
    var taps = [];
    document.addEventListener("pointerdown", function () {
        var now = Date.now();
        taps = taps.filter(function (t) { return now - t < 3000; });
        taps.push(now);
        if (taps.length >= 5) {
            taps = [];
            show();
        }
    }, true);

    if (forced) show();

    window.praOndeDebugReport = report;
})();
