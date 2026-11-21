// Keeps Skiko's calls into its Wasm out of JS tail position, the Safari 27.2
// white-page workaround; see app/webpack/skiko-no-tail-call.cjs for why.
//
// The Kotlin Gradle plugin appends every file in this directory to the
// webpack config it generates under the build directory, so `config` is that
// config and `__dirname` is somewhere under <root>/build. The loader is found
// by walking up from there rather than by a fixed relative path, which would
// break the day the plugin moves its generated packages.
//
// After bundling, every emitted script is checked again: if minification (or
// a Skiko update) leaves any wrapper as a tail call, the build fails instead
// of shipping a page that is blank on Safari 27.2.
;(function () {
    const path = require('path');
    const fs = require('fs');

    let loaderPath = null;
    for (let dir = __dirname; dir !== path.dirname(dir); dir = path.dirname(dir)) {
        const candidate = path.join(dir, 'app', 'webpack', 'skiko-no-tail-call.cjs');
        if (fs.existsSync(candidate)) {
            loaderPath = candidate;
            break;
        }
    }
    if (!loaderPath) {
        throw new Error(`skiko-no-tail-call: app/webpack/skiko-no-tail-call.cjs not found above ${__dirname}`);
    }
    const { findTailCallWrappers } = require(loaderPath);

    config.module = config.module || {};
    config.module.rules = config.module.rules || [];
    config.module.rules.push({ test: /[\\/]skiko\.mjs$/, loader: loaderPath });

    config.plugins = config.plugins || [];
    config.plugins.push({
        apply(compiler) {
            const { Compilation, WebpackError } = compiler.webpack;
            compiler.hooks.thisCompilation.tap('SkikoNoTailCallCheck', (compilation) => {
                compilation.hooks.processAssets.tap(
                    { name: 'SkikoNoTailCallCheck', stage: Compilation.PROCESS_ASSETS_STAGE_ANALYSE },
                    (assets) => {
                        for (const [name, asset] of Object.entries(assets)) {
                            if (!/\.m?js$/.test(name)) continue;
                            const left = findTailCallWrappers(asset.source().toString());
                            if (left.length > 0) {
                                compilation.errors.push(new WebpackError(
                                    `${name}: ${left.length} Skiko wrapper(s) still tail-call Wasm (Safari 27.2 white page), e.g. ${left[0]}`
                                ));
                            }
                        }
                    },
                );
            });
        },
    });
})();
