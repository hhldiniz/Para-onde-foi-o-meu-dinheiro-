// Webpack loader for Skiko's skiko.mjs: works around a Safari 27.2 bug that
// turns the whole web app into a blank white page, with no error anywhere.
//
// skiko.mjs exposes each Skia function to the Kotlin/Wasm code as a small JS
// wrapper whose body is a call into Skiko's own Wasm module in *tail position*:
//
//     (...a) => (X = loadedWasm._["X"])(...a)
//
// Safari 27.2 (WebKit bug 329421, fixed upstream but not in that release)
// mishandles exactly that shape once the calling Wasm has been optimized: an
// f32/f64 argument of 0 or 1 arrives as NaN. Compose then measures and draws
// with garbage sizes and nothing visible reaches the screen — on iPhone and
// Mac alike, and only after the JIT warms up, which is why the loading
// placeholder disappears and the page then stays white.
//
// A call inside a `try` block is never a tail call, so each wrapper is
// rewritten to
//
//     (...a) => { try { return (X = loadedWasm._["X"])(...a) } catch (e) { throw e } }
//
// which behaves identically everywhere else. Drop this loader (and
// webpack.config.d/skiko-no-tail-call.js) once Safari users carry the WebKit
// fix. Upstream tracking: JetBrains CMP-10986.
'use strict';

// The wrapper as Skiko ships it, `(...a) => (X = loadedWasm._["X"])(...a)`,
// and in minified form, `(...e)=>(b=d._.X)(...e)`.
const TAIL_CALL_WRAPPER =
    /\(\.\.\.([\w$]+)\)\s*=>\s*\(\s*([\w$]+)\s*=\s*([\w$]+)\._(\.[\w$]+|\[(["'])[\w$]+\5\])\s*\)\s*\(\s*\.\.\.\1\s*\)/g;

function rewriteTailCallWrappers(source) {
    let count = 0;
    const code = source.replace(TAIL_CALL_WRAPPER, (match, rest, binding, holder, member) => {
        count++;
        const call = `(${binding} = ${holder}._${member})(...${rest})`;
        return `(...${rest}) => { try { return ${call} } catch (e) { throw e } }`;
    });
    return { code, count };
}

function findTailCallWrappers(code) {
    return Array.from(code.matchAll(TAIL_CALL_WRAPPER), (match) => match[0]);
}

function skikoNoTailCallLoader(source) {
    const { code, count } = rewriteTailCallWrappers(source);
    if (count === 0) {
        // A Skiko update that changed the wrapper shape must not silently
        // ship the white page again.
        const file = (this && this.resourcePath) || 'skiko.mjs';
        throw new Error(`${file}: no Skiko export wrapper matched, so the Safari 27.2 workaround would not apply`);
    }
    return code;
}

module.exports = skikoNoTailCallLoader;
module.exports.rewriteTailCallWrappers = rewriteTailCallWrappers;
module.exports.findTailCallWrappers = findTailCallWrappers;
