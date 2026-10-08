# SDK design principles

The bar every new SDK type is held to. `add-card` (when a card forces a new primitive), `add-feature`
(always), and `review-changes` (as the review lens) all point here — this is the single copy.

The SDK must stay small and reusable so new cards *compose* existing primitives instead of growing a
card-specific type per Magic card. Roughly 25k cards exist; a per-card type is not a strategy.

## Composition over monoliths

The strongest version of a feature is usually **not a new type at all**. Before adding one, try:

| You want | Try first |
|---|---|
| New behavior | `CompositeEffect` of existing `Effects.*`, or a recipe in a `*Patterns.kt` object |
| Zone / library manipulation | The Gather → Select → Move pipeline (`architecture-principles.md` §1.5) — covers almost all of it with zero new executors |
| A number that changes with game state | A `DynamicAmount` composition (`Add`, `Subtract`, `Multiply`, `Min`, `Max`, `EntityProperty`, counts over a filter) |
| "Which objects" | A `GameObjectFilter` composition of `CardPredicate` / `StatePredicate` / `ControllerPredicate` |
| A continuous stat/keyword/type change | An existing static ability (`GrantDynamicStats`, `ModifyStatsForCreatureGroup`) fed a `DynamicAmount` / `GameObjectFilter` |
| "Does X match a filter?" | `Conditions.EntityMatches(entity, filter)` and its `SourceMatches` / `EnchantedPermanentMatches` / `TargetMatchesFilter` / `TriggeringSpellMatches` facades — name the entity role via `EffectTarget` |
| "You did/gained/cast N this turn" | `Compare` over a tracked `DynamicAmount` (e.g. `TurnTracking`). Missing tracker → add the *enum value* (data), not a condition class |

Reach for a `Patterns.*` recipe only when a second caller appears or it's a named MTG mechanic —
inline the composition at the card otherwise.

## Extend before you add

When composition doesn't reach, work this order and stop at the first step that fits:

1. **Compose** existing primitives (the table above).
2. **Add an axis to the closest existing type.** "Skip your next draw step" and "skip your next untap
   step" are one type over a turn-part axis, not two types. If the old name no longer fits the widened
   type, rename it and migrate its cards in the same change — refactoring is cheap here (below). An
   axis is a value with a meaning — an enum, a filter, a `DynamicAmount`, a sealed option — not a
   card-shaped Boolean. A field that only makes sense for the card that motivated it, or only means
   something when another field is set, is a flag: name the real axis instead.
3. **Add a new type** — only when neither of the above can say it: no composition reads the state,
   produces the player interaction or expresses the timing, and no existing type takes the axis without
   becoming two concepts under one name.

**A new type names its two closest existing types and why neither can take the axis.** Say it in the
type's KDoc and in the line `SdkSurfaceBaselineTest` asks for in
`mtg-sdk/src/test/resources/sdk-surface-baseline.txt` — the test prints the new type's nearest
neighbours, so "I didn't find one" isn't an answer.

**A new general primitive deletes what it subsumes.** When the type or axis you add makes an older,
narrower type expressible — `CompareNumericProperty` over the X-specific power predicates, a
filter-taking predicate over three name-specific ones — migrate the old type's cards and delete it **in
the same PR**. A subsumed type left behind is a fossil: the next author finds it first, copies it, and
the long tail grows back. `just sdk-tail` lists fossil candidates.

## Four reusability rules

1. **Target generality** — an effect should work on any valid entity type; the executor branches.
   - ✗ `GrantPlayerShroudEffect` — players only
   - ✓ `GrantShroudEffect(target: EffectTarget, duration: Duration)` — players, creatures, planeswalkers

2. **Duration / removal generality** — never bake timing into a type name.
   - ✗ `GrantShroudUntilEndOfTurnEffect`, `PlayerShroudUntilEndOfTurnComponent`
   - ✓ `GrantShroudEffect(duration: Duration)`; `PlayerShroudComponent(removeOn: PlayerEffectRemoval)`
     with `{ EndOfTurn, Permanent }` — the cleanup system reads the field to decide when to remove

3. **Parameterized filters and amounts** — no baked-in constants, subtypes, or single entity types.
   - ✗ `ReduceCostOfGoblinsEffect`, `GainLifeWhenGoblinEntersEffect`, `bonusPerType = 1`, `count = 20` for "any number"
   - ✓ `ReduceSpellCostEffect(filter: GameObjectFilter, amount: DynamicAmount)`; a general
     `OnCreatureEntersBattlefield(filter)` trigger paired with `Effects.GainLife(amount)`;
     `unlimited` / `dynamicMaxCount` flags

4. **Name the mechanic, not the card** — `ReduceSpellCostEffect`, not `ReduceGoblinCostEffect`. A name
   that lies about what the type does (`CreatureTypeCount` that actually counts all subtypes) is a bug:
   rename it and document the gap.

## Design for the *next* card

Ask "what are the slight variations of this that will show up later?" A primitive built for "+1/+1 for
each Goblin" should be the same one that later serves "−1/−1 for each artifact" and "power equal to your
life total". **If a small change in requirements would force a second new type, the boundary is drawn
wrong** — generalize now.

## Effects are pure data

SDK types are serializable data bags with no behavior (`architecture-principles.md` §1.1). This is what
makes state projection safe, networking trivial, and replay deterministic. Never put a lambda or an
engine reference in an SDK type; logic lives in executors.

A genuinely set-specific condition goes in that mechanic's own file, quarantined next to the rest of its
SDK surface — never in the general `*Conditions.kt`.

## Refactoring is cheap here

Young, single-owner engine, no external API consumers, no compatibility contract. Reshaping a type,
renaming a primitive, collapsing redundant variants, or changing a signature to make the SDK simpler is
**usually the correct call**, not a risk. Don't bolt a new variant alongside a now-obsolete one to "stay
safe" — replace the old shape and update every call site. The condition-hierarchy unification and the
`EffectTarget` refactor are the model: sweeping, compat-breaking, and the SDK came out markedly simpler.

(Respect `AGENTS.md` collaboration rules while doing it: refactor *your own* surface freely, but never
revert or stash another agent's in-flight work. If a refactor collides with theirs, pause and report.)

## When the elegant version is more work

Do the work. A one-off type that "works for this case" is debt every future card pays interest on. If
you genuinely can't find a reusable shape, **say so explicitly and explain why** — don't silently ship
the monolith.
