# Wither

**Status:** Not implemented
**Cards affected in Lorwyn Eclipsed:** 1 (`Spinerock Tyrant`).
**Priority:** Low — single card, legacy keyword, but cheap to add alongside Blight since both are
-1/-1 counter mechanics.

## Rules text

> Wither *(This deals damage to creatures in the form of -1/-1 counters.)*

CR 702.79: when a source with wither would deal damage to a creature, that damage is replaced by
putting that many -1/-1 counters on the creature instead. Still counts as damage dealt for
triggered abilities like lifelink and "whenever this deals damage" hooks.

Wither is a static keyword ability. The replacement effect only applies when the wither source
deals damage **to a creature**; damage to players and planeswalkers is unchanged.

## Implementation plan

### 1. SDK — add `Keyword.WITHER`
`mtg-sdk/.../core/Keyword.kt`

```kotlin
WITHER("Wither"),
```

It's static text only — no parameters — so `Keyword` enum is the right place, not
`KeywordAbility`. Reminder-text parsing picks it up for free.

### 2. Engine — replacement effect during damage assignment
Search for the damage-dealing pipeline (`DamageEffects.kt` on the SDK side, the corresponding
executor in `rules-engine/.../handlers/effects/damage/`). Look at how `DEATHTOUCH` and `LIFELINK`
are hooked into the damage event — wither goes in at the same layer.

The replacement logic:
1. Source has wither (check projected keywords, not base).
2. Target is a creature (check `projectedState.isCreature(targetId)`).
3. Replace "deal N damage" with "put N -1/-1 counters" **plus** still emit a
   `DamageDealtEvent` so lifelink/damage triggers still fire.

This is a classic replacement effect — it should be modeled as a `ReplacementEffect` registered
by the `StateProjector` whenever a permanent has wither, not as a hardcoded branch inside the
damage executor. That way future cards that grant wither (via auras, lord effects) work for free.

### 3. Tests

- Scenario: `Spinerock Tyrant` deals combat damage to a 2/3 creature → creature gets two -1/-1
  counters, becomes 0/1, dies to SBA.
- Scenario: wither source + lifelink → life gained equals damage, even though damage is replaced.
- Scenario: wither source deals damage to player → normal damage, no replacement.
- Scenario: trample + wither → trample damage to defending player is unreplaced; damage assigned
  to blocker is replaced with counters.
- Regression: removing wither via `TextRewrite` / "loses all abilities" effect correctly stops
  the replacement from applying.

## Dependencies

- Consider implementing after Blight — both share the `Minus1Minus1` counter placement path.
- Not blocking for the rest of the set; defer until 1–2 other ECL replacement-effect cards stack
  up if schedule is tight.
