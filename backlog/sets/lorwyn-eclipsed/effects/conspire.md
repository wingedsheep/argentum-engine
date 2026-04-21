# Conspire (Optional Additional Cost → Copy the Spell)

**Status:** Specified, not yet implemented
**Cards affected in Lorwyn Eclipsed:** 1 — `Raiding Schemes` (grants Conspire to your noncreature
spells). Original Shadowmoor printed Conspire directly on several instants/sorceries; ECL only
reprints the granter. The SDK keyword is added so granted Conspire has somewhere to point.
**Priority:** Medium — blocks `Raiding Schemes`, but nothing else in the set.

## Rules text (CR 702.78)

> Conspire *(As you cast this spell, you may tap two untapped creatures you control that share a
> color with it. When you do, copy it and you may choose new targets for the copy.)*

Rules subtleties to respect:

- Conspire is an **optional additional cost** (CR 702.78a/b) — it stacks on top of the spell's
  mana cost. Unlike evoke, it does not replace the normal cost.
- The "when you do" clause is a reflexive triggered ability (CR 603.11). It triggers exactly when
  the optional cost is paid, and goes on the stack **above** the spell being cast. Since it is on
  top, it resolves first — the copy is created while the original spell is still on the stack, and
  the copy can be countered / responded to independently.
- The copy inherits everything that isn't the targets (modes, additional costs, X, etc.), and the
  controller of the copy may choose new targets (but doesn't have to). If no legal new targets
  exist, the copy keeps the original's targets and fizzles on resolution if they're now illegal —
  same semantics as Storm. We already model this correctly in `StormCopyEffectExecutor`.
- "Share a color with it" is evaluated at the time the cost is paid, against the **spell's colors**
  (as it exists on the stack or is about to be cast). A colorless spell can never satisfy conspire
  — the tapped creatures must share at least one color with the spell, and a colorless spell has
  no colors to share.
- Hybrid/split colors: a creature tapped for conspire must share at least one of its colors with
  the spell. Rule 202.4: hybrid manifestations are both colors for color-matching.
- **Granted Conspire** (e.g., Raiding Schemes: "Each noncreature spell you cast has conspire")
  works by the standard `GrantKeywordToOwnSpells` path already used by Convoke. The engine treats
  the spell as if printed with Conspire while a permanent with the grant is in play, controlled by
  the caster, and the spell matches the grant's filter.

## Implementation plan

### 1. SDK — add `Keyword.CONSPIRE` and `KeywordAbility.Conspire`

`mtg-sdk/src/main/kotlin/com/wingedsheep/sdk/core/Keyword.kt`

```kotlin
CONSPIRE("Conspire"),
```

`mtg-sdk/src/main/kotlin/com/wingedsheep/sdk/scripting/KeywordAbility.kt`

```kotlin
@SerialName("Conspire")
@Serializable
data object Conspire : KeywordAbility {
    override val keyword: Keyword = Keyword.CONSPIRE
    override val description: String = "Conspire"
}
```

Simple `data object` — conspire has no parameters in the printed form. The "tap two untapped
creatures that share a color with this spell" cost is intrinsic to the keyword and handled by the
engine (not data-modeled as an `AdditionalCost`).

Add a DSL hook in `CardBuilder.kt` for completeness (no current card needs it for printed
Conspire, but future imports will):

```kotlin
fun conspire() = keywordAbility(KeywordAbility.Conspire)
```

### 2. SDK — no new `AdditionalCost` subtype

We deliberately do **not** add `AdditionalCost.Conspire` or model the tap-two-creatures payment as
a generic additional cost. The "share a color with the spell" predicate depends on the spell being
cast, which is information the cost payment pipeline doesn't currently thread. Modeling it as a
keyword-specific branch inside `CastSpellEnumerator` + `CastSpellHandler` (like Evoke) keeps the
cost system clean.

### 3. Engine — extend `CastSpell` action with conspire payment

`rules-engine/.../core/GameAction.kt`

Add a field:

```kotlin
val conspiredCreatures: List<EntityId> = emptyList()
```

Non-empty (two entries) iff the player chose to conspire. Two-entry constraint enforced in
`CastSpellHandler.validate()`.

### 4. Engine — enumerate a "cast with conspire" variant

`rules-engine/.../legalactions/enumerators/CastSpellEnumerator.kt`

Mirror the Evoke / Kicker enumeration (~line 859, `enumerateKicker`). For each castable spell:

1. Determine whether the spell has Conspire, either printed (`cardDef.hasKeyword(Keyword.CONSPIRE)`)
   or granted (`GrantedKeywordResolver.hasKeyword(state, playerId, cardDef, Keyword.CONSPIRE)`).
2. If so, check feasibility: does the caster control **at least two** untapped creatures such that
   some pair shares at least one color with the spell? (Cheap check: group untapped controlled
   creatures by color, cross-reference with `cardDef.colors`; if any color bucket has ≥2 entries,
   feasible.)
3. If feasible, add a `CastWithConspire` legal action variant alongside the normal cast. The
   `action` is `CastSpell(..., conspiredCreatures = <empty, filled in by client>)` — the client
   will resolve the two-creature selection via the cost-target UI.

The enumeration treats Conspire as an additive option layered on top of any other cast variant
(normal, kicked, modal, etc.). We'll enumerate only the basic-cost-with-conspire variant initially
and extend to kicked-with-conspire combinations later if any card ever needs it.

### 5. Engine — validate + pay conspire cost in `CastSpellHandler`

`rules-engine/.../handlers/actions/spell/CastSpellHandler.kt`

**Validation** (before mana payment):

- If `conspiredCreatures.isEmpty()`, conspire was not chosen — no validation needed.
- Otherwise, require:
  - Spell has Conspire (printed or granted — use `GrantedKeywordResolver`).
  - `conspiredCreatures.size == 2` and both IDs are distinct.
  - Both are on the battlefield, controlled by `action.playerId` (projected controller).
  - Both are creatures (projected type-line).
  - Both are untapped.
  - Spell has ≥1 color, and each conspired creature shares at least one color with the spell
    (projected colors for the creature, `cardDef.colors` for the spell).

**Payment** (as part of the existing additional-cost processing block):

- Tap both creatures. Emit `TappedEvent` for each (so "becomes tapped" triggers fire — mirror the
  attack-declare pattern per CLAUDE.md bug note).
- Record them in `AdditionalCostPayment.tappedPermanents` for transparency / undo paths.

**Copy trigger** (after the spell is placed on the stack, alongside the existing Storm-trigger
block near CastSpellHandler.kt:1625–1656):

- Build a single `PendingTrigger` whose ability mirrors the Storm pattern:
  - `trigger = SdkGameEvent.SpellCastEvent(player = Player.You)` (not actually detected — supplied
    synthetically)
  - `effect = StormCopyEffect(copyCount = 1, spellEffect, spellTargetRequirements, spellName)`
  - `descriptionOverride = "Conspire — copy ${cardComponent.name}"`
- Prepend it to the triggers list (same placement as Storm) so it lands on the stack immediately
  above the spell.

Reusing `StormCopyEffect` with `copyCount = 1` is intentional: the executor already handles the
"choose new targets for the copy (or inherit if illegal)" retargeting decision, modal copies, and
the `SpellOnStackComponent` clone plumbing.

### 6. Card — `Raiding Schemes`

`mtg-sets/.../lorwyneclipsed/cards/RaidingSchemes.kt`

```kotlin
val RaidingSchemes = card("Raiding Schemes") {
    manaCost = "{3}{R}{G}"
    typeLine = "Enchantment"
    oracleText = "Each noncreature spell you cast has conspire. (As you cast a noncreature spell, you may tap two untapped creatures you control that share a color with it. When you do, copy it and you may choose new targets for the copy. A copy of a permanent spell becomes a token.)"

    staticAbility {
        ability = GrantKeywordToOwnSpells(Keyword.CONSPIRE, GameObjectFilter.Noncreature)
    }

    metadata { /* rarity, collector number, artist, imageUri from Scryfall */ }
}
```

No new card-level plumbing — the granter is already fully supported by `GrantedKeywordResolver`.

### 7. Frontend

- Add `CONSPIRE` to `web-client/src/types/enums.ts` (Keyword enum + `KeywordDisplayNames`) so
  granted conspire renders in keyword chips.
- The "Cast with Conspire" action button is enumerated as a standard `LegalAction.CastSpell`
  variant with a distinct `description` — the existing cast-menu UI renders it without changes.
- Two-creature selection for the cost uses the existing additional-cost targeting flow (same flow
  as Convoke/Delve tap-as-cost). Filter presented to the player: "untapped creatures you control
  that share a color with {spell name}". If the existing cost-targeting UI only supports static
  filters, add a small extension that evaluates "shares color with the spell being cast" at prompt
  time — driven from the spell's `cardDef.colors`.

### 8. Tests

**SDK / DSL**
- `KeywordAbility.Conspire` round-trips through serialization.
- `card { conspire() }` produces the expected `keywordAbilities` entry.

**Engine — enumeration**
- Noncreature spell in hand + Raiding Schemes in play + two untapped creatures sharing the spell's
  color → enumerator produces both "Cast" and "Cast with Conspire" variants.
- Same setup but only one creature shares the spell's color → only "Cast" is enumerated.
- Creature spell + Raiding Schemes → only "Cast" (creature spells don't gain conspire via Raiding
  Schemes's filter).

**Engine — resolution (scenario)**
- Cast a targeted noncreature spell with conspire; tap two same-color creatures; copy is put on the
  stack above the original; retarget decision is offered; original and copy resolve independently.
- Decline conspire on the same spell → no copy is created, creatures stay untapped.
- Cast a colorless noncreature spell while Raiding Schemes is in play → conspire variant is not
  enumerated (no color to share).
- Original spell's target becomes illegal before resolution → only the original fizzles; the copy
  (with its own chosen target) still resolves. And vice versa.

**Regression**
- Storm-copy scenario tests (`StormCopyRetargetingTest`, `StormModalRetargetingTest`, etc.) still
  pass — Conspire's use of `StormCopyEffect(copyCount = 1)` must not change Storm behavior.

## Dependencies

- `StormCopyEffect` + `StormCopyEffectExecutor` + `StackResolver.putSpellCopy` — already present and
  well-tested. No new continuation types required.
- `GrantKeywordToOwnSpells` + `GrantedKeywordResolver` — already present. No extension needed.
- No new trigger type; the copy is emitted via a synthetic `PendingTrigger` with the existing
  `SpellCastEvent` trigger shape (same pattern Storm uses).
