# Compact serialization for scry / surveil (and siblings)

## Problem

`Patterns.Library.scry(n)` / `surveil(n)` are **factories that return a `CompositeEffect`** of atomic
steps (Gather → Select → Move → Move → `EmitScried/SurveiledEvent`). A card stores the *expanded*
composite, so:

- Every scry/surveil card serializes the full 5-step pipeline into its `CardDefinitionSnapshotTest`
  golden (≈81 scry + ≈105 surveil cards across most sets).
- Any change to the shared pipeline internals (e.g. adding the `EmitSurveiledEventEffect` completion
  tail in the FIN warm-ups PR #901) churns the golden of *every* card that uses the mechanic, even
  though no card's behavior was authored differently.

The atomic-composition design is correct and must stay (`CLAUDE.md`: "Prefer atomic pipeline
effects … over monolithic executors"). The issue is purely the **serialized representation**: a
card whose effect *is* "surveil 2" should serialize as `{"type":"Surveil","n":2}`, not as the
unrolled pipeline — while still executing via the shared atoms.

## Goal

In-memory and on-the-wire, a library keyword action is one compact node; at **execution** it expands
into the existing pipeline atoms. Net effects:

- Goldens shrink to one line per scry/surveil and stop churning when pipeline internals change.
- New mechanics keep reusing the same Gather/Select/Move atoms (no monolithic executor logic).
- Card SDK reads more like the oracle text (`Effects.Surveil(2)`).

## Design

A thin **macro effect** per keyword action — a serializable marker whose executor delegates to the
existing composite, reusing all pause/continuation handling:

- **SDK** (`mtg-sdk`):
  - Add `SurveilEffect(n: Int)` and `ScryEffect(n: Int)` (`@Serializable`, in `LibraryEffects.kt`),
    with `Effects.Surveil(n)` / `Effects.Scry(n)` facades.
  - Repoint `Patterns.Library.surveil(n)` / `scry(n)` to return the marker. Keep the current
    composite bodies as **public** builders `surveilPipeline(n)` / `scryPipeline(n)` (data only) so
    the engine can expand them. The `EmitScried/SurveiledEventEffect` tails live inside the pipeline
    builders, exactly as today.
- **Engine** (`rules-engine`):
  - `ScryExecutor` / `SurveilExecutor`: build `Patterns.Library.scryPipeline(n)` /
    `surveilPipeline(n)` and delegate to the registry's composite execution
    (`CompositeEffectExecutor` is already constructed with the registry `effectExecutor`, and it owns
    the choose-pause → `EffectContinuation` plumbing — so the macro executor adds **zero** new
    gather/select/move logic and the SelectCardsDecision pause still works).
  - Register both in `LibraryExecutors` (satisfies `EffectExecutorCoverageTest`).

### The real work: effect-tree introspection sites

Several places **walk the unrolled tree** instead of executing it. With scry/surveil collapsed to an
opaque node they no longer see the inner `GatherCardsEffect` / `SelectFromCollectionEffect`, so each
must either expand the macro first or grow a branch for it. Audit (trace per `add-feature` Step 4):

- `TriggerProcessor.findSelectionAmount` / `findStoreNumberAmount` (`TriggerProcessor.kt:829/843`) —
  recurse into `CompositeEffect` to read a Select's amount; add a `ScryEffect`/`SurveilEffect` branch
  (return the pipeline's select amount) or expand before walking.
- `ClientStateTransformer` — scry/surveil reveal/look-at UI and any `effectTree*` walkers; confirm the
  reveal path keys off resolution events (it should, via `ScriedEvent`/`SurveiledEvent` +
  `LookedAtCardsEvent`) and not off statically finding the gather node.
- `ManaSolver` / legal-action enumerators — they pattern-match composites for mana/selection; verify
  none assume scry/surveil internals.
- `FacadeBoundaryTest` / `CardLinter` — ensure the new facade is allowed and the markers are
  classified if they touch pipeline variables (they don't read/write named vars, so likely no
  dataflow classification needed — verify).

Decision point: **expand-at-walk vs teach-each-walker.** Prefer a single shared
`Effect.expandMacros()` helper (or have the macro executors be the *only* expansion point and make the
walkers call a shared `expandLibraryMacro(effect)`), so there's one place that knows scry/surveil →
pipeline, rather than N call sites each re-deriving it.

## Scope

- **Do scry *and* surveil together** — converting only surveil leaves the two inconsistent and only
  half-removes the golden bloat.
- Consider extending to `mill(n)` and `searchLibrary(...)` in the same pass (same factory-returns-
  composite shape) **only if** the introspection audit shows they're clean; otherwise leave them and
  note it. Don't widen scope past what the audit covers.

## Validation

1. `EffectExecutorCoverageTest` green (new executors registered).
2. Existing scry/surveil scenario tests (`ScryTriggerScenarioTest`, `SurveilTriggerScenarioTest`,
   plus the many cards that scry/surveil) pass unchanged — behavior must be identical.
3. Re-bless `CardDefinitionSnapshotTest` goldens; diff must show **only** the pipeline collapsing to a
   single `Scry`/`Surveil` node — no behavioral field lost.
4. `:mtg-sdk:test` JSON round-trip for the new effects; a card with `Effects.Surveil(2)` round-trips
   to the same object.
5. Client check: surveil/scry reveal UI still renders (drive a real game, per `verify`).

## Why this is its own PR

It's a cross-cutting **representation** change (SDK node + executor + every effect-tree walker +
~186 golden re-bless) that is independent of the FIN warm-ups it surfaced in (PR #901). Landing it
separately keeps the warm-up diff readable and gives this the layer-by-layer trace it needs.
