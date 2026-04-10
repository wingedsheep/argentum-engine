# Replace SpellTypeFilter with GameObjectFilter

## Problem

`SpellCastEvent` reinvents its own filtering system instead of using `GameObjectFilter`. It has:
- `spellType: SpellTypeFilter` — a closed enum (`ANY`, `CREATURE`, `NONCREATURE`, `INSTANT_OR_SORCERY`, `ENCHANTMENT`, `HISTORIC`). Adding "artifact spell" or "Elf spell" requires a code change.
- `manaValueAtLeast`, `manaValueAtMost`, `manaValueEquals` — inline numeric filters that duplicate `CardPredicate.ManaValueAtLeast` etc.
- `subtype`, `orSubtype` — inline subtype matching that duplicates `CardPredicate.HasSubtype`.
- `kicked` — kicker-specific flag (keep as-is, since kick state isn't a card characteristic).

All of this except `kicked` is already expressible via `GameObjectFilter`.

`SpellTypeFilter` also leaks into `CantBeBlockedIfCastSpellType` and `SpellKeywordGrant` (player component), creating secondary coupling.

## Target State

```kotlin
// Before
SpellCastEvent(
    spellType = SpellTypeFilter.CREATURE,
    manaValueAtLeast = 6,
    player = Player.You
)

// After
SpellCastEvent(
    spellFilter = GameObjectFilter.Creature.manaValueAtLeast(6),
    player = Player.You
)
```

## Approach

1. **Add a `spellFilter: GameObjectFilter?` field to `SpellCastEvent`** alongside the existing fields. Default `null` means "any spell" (same as `SpellTypeFilter.ANY` with no constraints).

2. **Update `TriggerMatcher.matchesSpellTypeFilter()`** to check `spellFilter` via the standard `PredicateEvaluator` when present, falling back to the old enum path when `spellFilter` is null.

3. **Migrate all `SpellCastEvent` usages** in `Triggers.kt` and card definitions to use `spellFilter` instead of `spellType` + inline fields. Update `Triggers` facade methods like `CastCreatureSpell`, `CastNoncreatureSpell`, etc.

4. **Migrate `CantBeBlockedIfCastSpellType`** to take a `GameObjectFilter` instead of `SpellTypeFilter`. Update `BlockEvasionRules.kt` and `RelicRunner.kt`.

5. **Migrate `SpellKeywordGrant`** to use `GameObjectFilter` instead of `SpellTypeFilter`. Update `CastSpellHandler.kt` matching logic.

6. **Remove the old fields** from `SpellCastEvent` (`spellType`, `manaValueAtLeast/AtMost/Equals`, `subtype`, `orSubtype`) and delete `SpellTypeFilter` enum.

7. **Keep `kicked: Boolean?`** — kick state is runtime spell state, not a card characteristic, so it doesn't belong in `GameObjectFilter`.

## Files

- **SDK:** `EventFilters.kt` (delete `SpellTypeFilter`), `GameEvent.kt` (simplify `SpellCastEvent`), `BlockingStaticAbilities.kt`, `Triggers.kt`
- **Engine:** `TriggerMatcher.kt` (`matchesSpellTypeFilter`), `CastSpellHandler.kt` (`SpellKeywordGrant` matching), `BlockEvasionRules.kt`, `PlayerComponents.kt` (`SpellKeywordGrant`)
- **Sets:** `Kurgadon.kt`, `HelgaSkittishSeer.kt`, `RelicRunner.kt`, `RalCracklingWit.kt`
- **Server:** `ClientStateTransformer.kt`
- **Tests:** `RalCracklingWitScenarioTest.kt`

## Notes

- Step 1-2 can be done as a backwards-compatible addition (new field alongside old). Steps 3-6 are the migration and cleanup. This lets you validate incrementally.
- `GameObjectFilter` matching for spells on the stack should use base state (not projected), since spells on the stack don't get continuous effects. This is consistent with how `matchesSpellTypeFilter` works today.
