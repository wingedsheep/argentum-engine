# Wiring a new SDK type

Read this only when a card genuinely needs vocabulary the SDK doesn't have. First satisfy the bar in
[`docs/sdk-design-principles.md`](../../../docs/sdk-design-principles.md) — composition first, and if a
new type survives that, it must be parameterized for the *next* card, not this one.

If the addition is more than a small primitive a single card needs — a mechanic, a decision flow, a
turn-structure change — stop and use the **`add-feature`** skill instead. It traces every layer.

## Where each kind of vocabulary lives

| New vocabulary | SDK home | Engine wiring |
|---|---|---|
| Effect | `mtg-sdk/.../scripting/effect/{Category}Effects.kt` + facade in `dsl/Effects.kt` | Executor in `rules-engine/.../handlers/effects/{category}/`, registered in `{Category}Executors.kt` |
| Trigger | `mtg-sdk/.../scripting/trigger/` + facade in `dsl/Triggers.kt` | `TriggerDetector` detection path + `TriggerIndex` registration |
| Condition | `mtg-sdk/.../scripting/condition/` + facade in `dsl/Conditions.kt` | `ConditionEvaluator` — must work in *both* resolution and projection via `ConditionEvaluationContext` |
| Static ability | `mtg-sdk/.../scripting/StaticAbility.kt` | `StateProjector`, in the correct Rule 613 layer |
| Replacement effect | `mtg-sdk/.../scripting/ReplacementEffect.kt` (declarative `appliesTo`) | engine interception point |
| DynamicAmount variant | `mtg-sdk/.../scripting/DynamicAmount.kt` | `DynamicAmountEvaluator` |
| Filter predicate | `mtg-sdk/.../model/` predicate types | `PredicateEvaluator` |
| Keyword | `mtg-sdk/.../core/Keyword.kt` | `web-client/src/types/enums.ts` (enum + `KeywordDisplayNames`) + `web-client/src/assets/icons/keywords/index.ts` if it needs an icon |
| Component / tracker | `mtg-sdk/.../component/` or an engine component | reader in projection/handlers; cleanup if it has a duration |

Effect category files: `DamageEffects.kt`, `LifeEffects.kt`, `DrawingEffects.kt`, `RemovalEffects.kt`,
`PermanentEffects.kt`, `LibraryEffects.kt`, `ManaEffects.kt`, `TokenEffects.kt`, `CompositeEffects.kt`,
`CombatEffects.kt`, `PlayerEffects.kt`, `StackEffects.kt`.

## Counter types span five layers

A new counter type is the most commonly under-wired addition — **all five** are required, and there is
no generic counter renderer on the frontend:

1. `mtg-sdk/.../core/CounterType.kt` — enum value + `Counters` string constant.
2. `rules-engine/.../mechanics/layers/StateProjector.kt` — add to `KEYWORD_COUNTER_MAP` if it's a keyword
   counter (flying, indestructible, trample, …) so projected state grants the keyword.
3. `web-client/src/types/enums.ts` — `CounterType` enum + `CounterTypeDisplayNames`.
4. `web-client/src/assets/icons/keywords/index.ts` — `counterManaClass` entry (`ability-<keyword>` for
   keyword counters, `counter-<style>` otherwise; mana-font has `counter-flood`, `counter-lore`,
   `counter-bolt`, `counter-charge`, …).
5. The badge itself, in three files: a `getXxxCounters(card)` helper in
   `web-client/src/components/game/board/shared.ts`, an `xxxCounterBadge` style in
   `.../board/styles.ts`, and a JSX block in `.../card/GameCard.tsx` following an existing badge such as
   `blightCounterBadge`.

## Document it in the same change

[`docs/card-sdk-language-reference.md`](../../../docs/card-sdk-language-reference.md) is the canonical
catalog and a standing `AGENTS.md` rule: add the new building block to the right section (§4 Effects,
§5 Effect patterns, §7 Filters, §8 Triggers, §9 Static abilities, §11 Keywords, §12 Conditions,
§13 Dynamic amounts, §14 Modal & choice, §15 Replacement effects).

A new SDK type that reads or writes a named pipeline variable must also be classified in
`CardLinter.dataflowFields`, or `CardLintTest`'s hygiene check fails. See §21.

## Teach the mtgish generator (best-effort, wide payoff)

One entry unlocks coverage and auto-draft for *every* card sharing the mechanic across the whole corpus,
not just this one. When the building block maps to an mtgish IR tag:

- **Capability bridge** (`mtgish-tooling/.../coverage/bridge/`) — a one-line `tag → capability` mapping
  in the closest themed bridge file, so the probe scores those cards as coverable rather than blocked.
- **Rendering emitter** (`mtgish-tooling/.../coverage/emitter/*Handlers.kt`) — a `simple("Tag",
  "MyEffect()")` entry, or `on("Tag") { node, args, tvar -> … }` when it needs amount/target/filter
  recovery. Extend `TargetRecovery.kt` rather than widening filters. Handlers don't track imports;
  `Shells.importsFor` derives them.

Confirm with `just coverage-verify --set <SET>`. If the mechanic is genuinely too card-specific to render
exactly, return `null` from the emitter (the SCAFFOLD tier) but **still add the bridge entry** so coverage
scoring stays correct. See [`mtgish-tooling/README.md`](../../../mtgish-tooling/README.md)
§"Adding a handler".

## Assay will want to spell it too — name it, don't build it here

[Argentum Assay](../../../oracle-assay/README.md) parses Oracle text into these same SDK types, so new
vocabulary widens what the grammar *could* read. Teaching it is deliberately **not** part of this change:
an Assay rule is written in both directions and lands as a measured family — a *band*, with its own probe,
ranking and PR (see [`docs/oracle-assay.md`](../../../docs/oracle-assay.md)). Bolting one onto a card PR
gets you a rule nobody measured.

What does belong here:

- **If you changed or renamed an existing SDK type** rather than adding one, `:oracle-assay` is a
  compile-time consumer of `mtg-sdk`. Update the grammar rules that construct it in the *same* change and
  re-run `just assay-gate --limit 2000`, so the touchstone still round-trips.
- **If you added one**, say so in the commit or PR body and leave it there.
  `just assay-report --rank tail` is the ranked backlog of what the grammar can't read yet; new SDK
  vocabulary is how a row on it becomes reachable, and naming it is what lets the next band pick it up.
