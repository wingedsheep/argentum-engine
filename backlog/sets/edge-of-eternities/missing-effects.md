# Edge of Eternities (EOE) - Missing Effects & Implementation Plan

This document lists engine features required to implement EOE cards that do not yet exist in the
Argentum Engine, ordered by priority (number of cards unblocked).

---

## 1. Warp (Alternative Casting Cost + Exile Loop)

**Cards affected:** ~30+ (Anticausal Vestige, All-Fates Stalker, Bygone Colossus, Codecracker Hound,
Drix Fatemaker, Starbreach Whale, Starfield Shepherd, Weftstalker Ardent, Timeline Culler, etc.)

**Rules:** "You may cast this card from your hand for its warp cost. Exile this creature at the
beginning of the next end step, then you may cast it from exile on a later turn."

**Implementation plan:**

1. **mtg-sdk:** Add `WarpAbility(cost: ManaCost)` to `KeywordAbility` sealed hierarchy. Add a
   `warp` DSL block in `CardBuilder` similar to `morph {}` / `kicker {}`.
2. **rules-engine:** Add alternative casting cost handling — when a player casts a card for its warp
   cost, mark it with a `WarpedComponent` tag component.
3. **rules-engine:** Create a delayed trigger: "At the beginning of the next end step, exile this
   permanent." When exiled this way, mark the card with a `WarpExiledComponent` so it can be cast
   from exile on future turns.
4. **rules-engine:** Grant cast-from-exile permission for cards with `WarpExiledComponent`. When
   cast from exile, the warp loop continues (exile again at end step).
5. **rules-engine:** Track `WarpedThisTurnComponent` on game state for Void condition checks.
6. **rules-engine:** Handle Timeline Culler variant: "You may cast this card from your graveyard
   using its warp ability" — extend warp to allow graveyard as a cast zone.
7. **rules-engine:** Handle Tannuk, Steadfast Second: "Artifact cards and red creature cards in your
   hand have warp {2}{R}" — grant warp to other cards as a static ability.

**Dependencies:** None. Can be implemented independently.

---

## 2. Station (Spacecraft/Planet Charge Counter Mechanic)

**Cards affected:** ~20+ (Sledge-Class Seedship, Atmospheric Greenhouse, Debris Field Crusher,
Dawnsire, Galvanizing Sawship, Wurmwall Sweeper, The Eternity Elevator, Synthesizer Labship, etc.)

**Rules:** "Tap another creature you control: Put charge counters equal to its power on this
permanent. Station only as a sorcery." At threshold levels (e.g., 7+), the permanent gains
abilities and may become a creature.

**Implementation plan:**

1. **mtg-sdk:** Add `StationAbility` to `KeywordAbility`. Define a `StationThreshold` data class:
   `(minCounters: Int, abilities: List<...>, becomesCreature: Boolean)`.
2. **mtg-sdk:** Add DSL support: `station { threshold(7) { flying; becomesCreature() } }`.
3. **rules-engine:** Implement the station activated ability: tap another creature, add charge
   counters equal to its power. Restrict to sorcery speed.
4. **rules-engine:** Implement threshold-based continuous effects via `StateProjector`:
   - At N+ counters: grant keywords, activated abilities, triggered abilities.
   - Type-changing: "It's an artifact creature at 7+" — add Creature type when threshold met.
5. **rules-engine:** Handle multi-tier thresholds (Dawnsire has 10+ and 20+, Synthesizer Labship
   has 2+ and 9+).
6. **rules-engine:** Handle Tapestry Warden interaction: "stations permanents using its toughness
   rather than its power."

**Dependencies:** None. Can be implemented independently.

---

## 3. Void (Ability Word / Turn-State Condition)

**Cards affected:** ~10+ (Alpharael Stonechosen, Chorale of the Void, Decode Transmissions, Elegy
Acolyte, Temporal Intervention, Tragic Trajectory, Voidforged Titan, etc.)

**Rules:** "If a nonland permanent left the battlefield this turn or a spell was warped this turn..."
Used as trigger conditions, spell cost reductions, and effect upgrades.

**Implementation plan:**

1. **rules-engine:** Track two turn-state flags:
   - `NonlandPermanentLeftBattlefieldThisTurn` — set when any nonland permanent changes zone from
     battlefield (via `ZoneChangeEvent`).
   - `SpellWarpedThisTurn` — set when any spell is cast for its warp cost (from Warp implementation).
2. **mtg-sdk:** Add `Conditions.Void` that checks either flag is true. This is a standard
   `Condition` usable in `ConditionalEffect`, trigger conditions, and cost reductions.
3. **mtg-sdk:** Add DSL support: `void { ... }` block or `condition = Conditions.Void`.

**Dependencies:** Warp (for the "spell was warped" half). The "nonland permanent left" half can be
implemented independently.

---

## 4. Lander Tokens

**Cards affected:** ~20+ (Beamsaw Prospector, Bioengineered Future, Biomechan Engineer, Biotech
Specialist, Dauntless Scrapbot, Edge Rover, Emergency Eject, Galactic Wayfarer, Glacier Godmaw,
Seedship Agrarian, Sunstar Expansionist, etc.)

**Rules:** "Create a Lander token. (It's an artifact with '{2}, {T}, Sacrifice this token: Search
your library for a basic land card, put it onto the battlefield tapped, then shuffle.')"

**Implementation plan:**

1. **mtg-sdk:** Add a `TokenDefinition.Lander` predefined token (like Food, Treasure, Clue). Define
   it as an artifact token with the sacrifice-to-search activated ability.
2. **mtg-sdk:** Add `Effects.CreateLanderToken()` convenience method.
3. **rules-engine:** The token's activated ability uses existing effects: sacrifice self (cost),
   then `SearchLibrary(filter = BasicLand, destination = Battlefield(tapped))` + shuffle. This
   should compose from existing atomic effects.

**Dependencies:** None. Mostly DSL/token definition work.

---

## 5. Poison Counters

**Cards affected:** 1 (Virulent Silencer), but also Thrumming Hivepool (Slivers) could enable
future poison cards.

**Rules:** "That player gets two poison counters. A player with ten or more poison counters loses
the game."

**Implementation plan:**

1. **rules-engine:** Add `PoisonCounterComponent` to player entities tracking poison count.
2. **rules-engine:** Add `AddPoisonCountersEffect` and corresponding executor.
3. **rules-engine:** Add state-based action: "A player with 10+ poison counters loses the game."
4. **game-server:** Expose poison counter count in `MaskedGameState` / client DTOs.
5. **web-client:** Display poison counter count in player info panel.

**Dependencies:** None.

---

## 6. Stun Counters

**Cards affected:** 1 (Cryogen Relic).

**Rules:** "Put a stun counter on target tapped creature. If a permanent with a stun counter would
become untapped, remove one from it instead."

**Implementation plan:**

1. **rules-engine:** Add `StunCounterComponent` (or use generic counter system).
2. **rules-engine:** Add replacement effect in untap logic: if permanent has stun counters, remove
   one instead of untapping.
3. **mtg-sdk:** Add `Effects.AddStunCounter()` convenience.

**Dependencies:** None.

---

## 7. Token Doubling (Replacement Effect)

**Cards affected:** 1 (Exalted Sunborn).

**Rules:** "If one or more tokens would be created under your control, twice that many of those
tokens are created instead."

**Implementation plan:**

1. **rules-engine:** Add a replacement effect that intercepts `CreateTokenEffect` execution.
   When the controller has a token-doubling continuous effect, double the token count.
2. **mtg-sdk:** Add a static ability type: `DoubleTokenCreation`.

**Dependencies:** None, but replacement effect infrastructure for token creation may need work.

---

## 8. Devour (Land Variant)

**Cards affected:** 1 (Famished Worldsire).

**Rules:** "Devour land 3 — As this enters, you may sacrifice any number of lands. It enters with
three times that many +1/+1 counters."

**Implementation plan:**

1. **mtg-sdk:** Extend existing `Devour` keyword (if creature-only) to support a filter parameter
   (lands vs creatures).
2. **rules-engine:** Modify devour handler to accept a `GameObjectFilter` for what can be
   sacrificed, rather than hardcoding creatures.

**Dependencies:** Check if standard Devour (creatures) is already implemented.

---

## 9. Mindslaver Effect (Control Opponent's Turn)

**Cards affected:** 1 (The Dominion Bracelet).

**Rules:** "You control target opponent during their next turn."

**Implementation plan:**

This is one of the most complex effects in Magic. It requires:

1. **rules-engine:** Add `ControlledByPlayerComponent` to player entities — during that player's
   turn, all decisions are made by the controlling player.
2. **rules-engine:** Modify decision routing so the controlling player receives all choices.
3. **game-server:** Route decisions to the controlling player's session.
4. **game-server:** State masking: the controlling player sees the controlled player's hand/hidden
   info.
5. **web-client:** UI for controlling another player's turn.

**Recommendation:** Defer this. It's a single card requiring massive infrastructure. Implement The
Dominion Bracelet last or skip it for the initial EOE implementation.

---

## 10. Spacecraft Subtype

**Cards affected:** ~25+ (all Spacecraft cards, plus cards referencing "creature or Spacecraft").

**Rules:** Spacecraft is an artifact subtype. Cards reference "creature or Spacecraft" as a
targeting/filtering category.

**Implementation plan:**

1. **mtg-sdk:** Add `Spacecraft` to artifact subtypes.
2. **mtg-sdk:** Add filter support: `GameObjectFilter.CreatureOrSpacecraft` or extend existing
   filter DSL to support "creature or Spacecraft" as a union filter.
3. **rules-engine:** Ensure type-line parsing handles `Artifact — Spacecraft`.

**Dependencies:** None. Likely minimal work if subtype system is flexible.

---

## 11. Differently Named Lands (DynamicAmount)

**Cards affected:** 3+ (All-Fates Scroll, Fungal Colossus, Survey Mechan).

**Rules:** "The number of differently named lands you control."

**Implementation plan:**

1. **mtg-sdk:** Add `DynamicAmount.DifferentlyNamedLands` (or parameterize an existing count with
   a "unique by name" modifier).
2. **rules-engine:** Implement evaluator that counts unique land names on the battlefield.

**Dependencies:** None.

---

## 12. "Mana Spent to Cast" Tracking

**Cards affected:** 2+ (Dyadrine enters with counters equal to mana spent; Astelli Reclaimer uses
X = mana spent).

**Rules:** Track the total mana spent to cast a spell (not just X value).

**Implementation plan:**

1. **rules-engine:** Store `manaSpentToCast` on spells/permanents when cast. This may already be
   partially tracked for X spells.
2. **mtg-sdk:** Add `DynamicAmount.ManaSpentToCast` for use in ETB effects.

**Dependencies:** None.

---

## Implementation Order (Recommended)

| Phase | Feature                    | Cards Unblocked | Effort |
|-------|----------------------------|-----------------|--------|
| 1     | Lander tokens              | ~20             | Small  |
| 2     | Spacecraft subtype         | ~25             | Small  |
| 3     | Warp                       | ~30             | Large  |
| 4     | Void                       | ~10             | Medium |
| 5     | Station                    | ~20             | Large  |
| 6     | Differently named lands    | ~3              | Small  |
| 7     | Mana spent tracking        | ~2              | Small  |
| 8     | Poison counters            | ~1              | Medium |
| 9     | Stun counters              | ~1              | Small  |
| 10    | Token doubling             | ~1              | Medium |
| 11    | Devour (land variant)      | ~1              | Small  |
| 12    | Mindslaver                 | ~1              | Huge   |

After phases 1-5, the majority of EOE cards become implementable using existing engine effects
(ETB triggers, +1/+1 counters, destroy, exile, draw, mill, surveil, kicker, convoke, equip,
landfall, combat damage triggers, etc.).
