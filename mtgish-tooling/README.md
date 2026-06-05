# mtgish Tooling

This module turns the external [mtgish](https://github.com/i5jb/mtgish) oracle IR into actionable Argentum data.
It is analysis and generation tooling, not a runtime dependency and not a card loader.

## Commands

Run through `just` from the repo root:

```bash
just coverage --set POR
just coverage-fidelity --set POR
just coverage-verify POR
just coverage-generate --set TMP
just coverage-refresh-set POR
```

The installed CLI has three subcommands:

```bash
mtgish-tooling probe     # coverage: can the SDK/engine express a card?
mtgish-tooling fidelity  # calibration: does generated output match implemented cards?
mtgish-tooling autogen   # write generated Kotlin drafts
```

## Data Flow

1. `Mtgish.kt` loads mtgish card IR from `spike/mtgish-coverage/data/mtgish.lines.json`.
2. `Cards.kt` joins mtgish names to Scryfall cached set data and existing Argentum card names.
3. `bridge/*` maps mtgish tags to Argentum capabilities for coverage scoring.
4. `emitter/*` renders Kotlin `card { ... }` DSL from mtgish rules.
5. `mtg-sets:verifyGeneratedCards` compiles generated drafts and serializes them with the same exporter used for golden snapshots.
6. `fidelity --gate SET` compares generated gameplay trees to golden snapshots, with a small allowlist of known-equivalent representations and Scryfall-oracle drift reporting.

## Mapping mtgish to Argentum

Coverage mappings live in `src/main/kotlin/com/wingedsheep/tooling/coverage/bridge/`.

Add mappings by editing the themed bridge files:

```kotlin
effect("DrawNumberCards", "DrawCards")
composed("DestroyPermanent", "MoveToZone -> graveyard", composes = listOf("MoveToZone"))
envelope("Targeted", "structural wrapper")
```

Mapping kinds:

- `keyword`: mtgish tag maps to an SDK `Keyword` enum value.
- `effect`: mtgish tag maps to one SDK `Effect` `@SerialName`.
- `composed`: mtgish action is expressible by existing Argentum primitives.
- `envelope`: structural IR node whose nested children carry the real capability.
- `supported`: accepted non-effect capability, such as trigger/cost vocabulary.

The bridge answers “can Argentum express this?” It does not prove the emitter can render exact card source.

## Rendering Kotlin Cards

Emitter code lives in `src/main/kotlin/com/wingedsheep/tooling/coverage/emitter/`.

Important files:

- `Emitter.kt`: whole-card assembly and shell rendering.
- `ActionHandlers.kt`: central registry joining all `_Action` handler maps.
- `TargetRecovery.kt`: target and filter reconstruction.
- `DamageDrawLifeHandlers.kt`, `ZoneHandlers.kt`, `TapLayerStateHandlers.kt`, `PlayerContinuousHandlers.kt`: themed action handlers.
- `CardStructure.kt`: spell, triggered ability, and activated ability envelopes.
- `Shells.kt`: mana cost, type line, metadata, imports.

To add support for a new mtgish action:

1. Find the mtgish `_Action` value in a failing `fidelity --gate` or scaffold reason.
2. Add a handler in the closest themed `*Handlers.kt` file.
3. If the handler needs target/filter support, add it to `TargetRecovery.kt` rather than widening filters.
4. Return `null` when exact rendering is not possible. This deliberately downgrades the card to `SCAFFOLD`.
5. Run `just coverage-verify POR` or another calibrated set before trusting the change.

## Fidelity Policy

The generator must be conservative:

- A confidently wrong generated card is worse than no generated card.
- If a target, filter, amount, cost, gate, or choice cannot be rendered exactly, return `null` and scaffold.
- `coverage-verify` is the regression gate: generated cards must compile and gameplay-tree match calibrated snapshots.
- When golden snapshots disagree with current Scryfall oracle text, the gate reports `GOLDEN DRIFT SUSPECTED` instead of treating the generated output as wrong.

## Source Refreshes

`autogen --write-all --set CODE` replaces real set card source files with mtgish-generated files:

```bash
just coverage-refresh-set POR
```

This writes complete generated cards and scaffold files for unsupported structures. Existing source files that use `basicLand(...)` are preserved, and mtgish basic-land entries are skipped, because sets can contain multiple basic-land printings that Scryfall/mtgish collapse by name. Use it only when intentionally converting a set to mtgish-authored source, then compile and refresh snapshots.
