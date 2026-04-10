# SDK Composability Refactors

Goal: every card should be describable by combining atoms in JSON, with zero code changes.

## Tier 1 — Every new card might hit these

These are the patterns most likely to block new card definitions. Fix first.

- ~~[Remove GlobalEffectType enum](remove-global-effect-type.md) — delete redundant enum, use existing group static abilities~~ **DONE**
- ~~[Replace SpellTypeFilter with GameObjectFilter](replace-spell-type-filter.md) — `SpellCastEvent` should use composable filters, not a closed enum + inline fields~~ **DONE**
- [Unify blocking evasion](unify-blocking-evasion.md) — collapse 12+ blocking restriction classes into 2 parameterized types
- [Generic DynamicAmount](generic-dynamic-amount.md) — replace ~10 card-specific counting variants with generic primitives

## Tier 2 — Blocks specific card patterns

These affect specific mechanics. Lower urgency but still require code changes for new cards using these patterns.

- [Generic ChainCopyEffect](generic-chain-copy.md) — replace `ChainAction` enum + `ChainCopyCost` with generic `Effect` + `PayCost`
- [Unify CombatCondition](unify-combat-condition.md) — replace 2-variant sealed interface with existing `Condition` system
- [Merge EntersWith*Choice](merge-enters-with-choice.md) — merge 3 replacement effects into single `EntersWithChoice(choiceType)`
- [Compose bespoke stat abilities](compose-bespoke-stat-abilities.md) — `ModifyStatsForChosenCreatureType`, `ModifyStatsByCounterOnSource`, `ModifyStatsPerSharedCreatureType` → generic composition

## Tier 3 — Niche or unused

Low impact — either unused by any current card, single-card mechanics, or set-specific.

- [Decompose card-specific triggers](decompose-card-specific-triggers.md) — `CreatureDealtDamageBySourceDiesEvent` and `AttackEvent.alone` (both currently unused)
- [Merge Undying/Persist](merge-undying-persist.md) — two identical patterns differing only by counter type (currently unused)
- [Decompose SecretBidEffect](decompose-secret-bid.md) — monolithic single-card effect (Menacing Ogre only)
- [Decompose AmplifyEffect](decompose-amplify.md) — set-specific replacement effect, should compose from pipeline primitives
