# Standalone browser checker

An opt-in Kotlin/Wasm build of Assay's existing whole-card compiler. No game engine, catalogue,
web server, inference, or corpus is loaded. The normal JVM build is unchanged.

From this directory, with JDK 21, `just`, Node and optionally Binaryen `wasm-opt` on PATH:

```sh
just build
node test.mjs
```

The output in `dist/` is self-contained ES modules plus WASM, without source maps or third-party
JavaScript packages. Load it in a disposable Web Worker, with a host timeout:

```js
import {checkOracleCard} from './assay-oracle.mjs';
const result = JSON.parse(checkOracleCard(JSON.stringify({
  name:'Probe', layout:'normal', type_line:'Sorcery', mana_cost:'{U}', oracle_text:'Draw two cards.'
})));
```

`parse_complete` means the full compiler gate passed: normalization, line round trips/variants,
fragment merging, header consistency and SDK validation. It does not prove official Oracle wording,
intent fidelity or runtime playability. Declines, accepted variants and warnings remain visible.
Multi-face compilation remains declined, exactly as in the JVM compiler.

The build shares selected upstream source files instead of maintaining another grammar. Its only
generated-source adaptations remove JVM inline annotations and substitute three pieces of platform
plumbing: per-worker local scopes, grammar rule IDs and UUID generation. Unexpected Java imports
fail the build. No parser, normalizer, merger or validator is replaced or stubbed. Java-specific
CLI, filesystem, HTTP, corpus and explorer modules never enter the browser build.

The target requires browser Wasm GC and exception handling. Test the emitted optimized artifact in
supported browsers; do not enable Binaryen's experimental stringref feature.
