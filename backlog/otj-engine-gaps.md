# Outlaws of Thunder Junction — Engine Gap Analysis

Cross-reference of the **254 remaining (unimplemented) OTJ cards** against the engine's actual
capabilities (SDK reference + source verification, June 2026). Generated to scope what must be
built before the set can be completed.

**Status:** 17 / 271 implemented (6%). Card list from `scripts/card-status --list --set OTJ`.
Oracle text pulled from Scryfall (`set:otj unique:cards`, 276 printings → 271 unique cards + 5
basics).

## Bottom line

OTJ is built around a small set of named mechanics, **most of which the engine already supports**:
Plot, Spree, "commit a crime", the Outlaw creature-type group, Treasure, Crew, **and now Saddle /
Mount** are all done. With Saddle landed, the overwhelming majority of the set is buildable today
(standard creatures, removal, Deserts, Plot/Spree spells, crime payoffs, token-makers). What's left
is a handful of small recurring primitives plus ~12 genuinely one-off cards.

### Already supported — no new engine work

- **Plot** (CR 718) — full special-action support: `KeywordAbility.Plot`, `PlotEnumerator`,
  `PlottedComponent`, `CardPlottedEvent`, sorcery-speed gate, cast-plotted-from-exile. **38 cards.**
  (Aloe Alchemist, Aven Interrupter, Demonic Ruckus, Fblthp, Jace Reawakened, Outlaw Stitcher,
  Slickshot Show-Off, Stingerback Terror, …)
- **Spree** — modal "choose one or more additional costs" casting via the existing modal
  per-mode-additional-cost path (`CastSpellEnumerator`, `ModalPerModeAdditionalCostTest`).
  **21 cards.** (Three Steps Ahead, Smuggler's Surprise, Final Showdown, Rush of Dread,
  Great Train Heist, Trash the Town, …)
- **Commit a crime** trigger — `CommitCrimeEvent` + `CrimeDetector` (targeting opponents / their
  permanents / cards / spells / planeswalkers), emitted once per cast or activation in
  `StackResolver`. `Triggers.youCommitCrime` / `Conditions.YouCommittedACrimeThisTurn` work.
  **26 cards.** (Forsaken Miner — already implemented — Marchesa, Magda, Oko the Ringleader,
  Vadmir, Nimble Brigand, …)
- **Outlaw** creature-type group (Assassin, Mercenary, Pirate, Rogue, Warlock) —
  `Filters.OutlawCreature` / `NonOutlawCreature`, `Subtype.OUTLAW_TYPES`. (Double Down,
  Hellspur Posse Boss, Laughing Jasper Flint, affinity-for-outlaws, …)
- **Treasure** tokens, **Crew N** keyword, **Desert** subtype (`Subtype.DESERT`),
  landcycling / typecycling — all present.
- Building blocks several gaps below compose: keyword counters
  (flying/first-strike/deathtouch/trample/lifelink/indestructible), tapped-and-attacking tokens,
  "next end step" / event-based delayed triggers, impulse draw (`MayPlayFromExile`),
  copy-spell-on-stack, copy-card-in-zone-then-cast, becomes-copy/clone, `NthSpellCast` trigger
  (first/second spell each turn), modal spells, exile-until-leaves (O-Ring), dynamic/mass P/T,
  `ExchangeControlEffect`, `DoubleCountersEffect`, divided damage, sacrifice-a-token cost,
  exile-from-graveyard cost, `EntersWithChoice(COLOR/MODE)`, Saga + planeswalker frameworks.

What follows are the **genuine gaps** — elements no current SDK primitive expresses.

---

## Tier 1 — Headline keyword (17 cards, highest leverage)

### 1. Saddle N + Mount subtype — ✅ DONE

**Implemented:** `Keyword.SADDLE` + `KeywordAbility.saddle(n)` DSL, the `SaddleMount` special action
+ `SaddleMountHandler` (taps other untapped creatures totalling power ≥ N, sorcery-speed gated),
`SaddledComponent` (cleared in `CleanupPhaseManager`), `StatePredicate.IsSaddled` /
`Conditions.SourceIsSaddled` for "while saddled" gating, and `SaddleEnumerator` for the tap-for-power
selection UI. Tests: `SaddleScenarioTest`. The `Mount` subtype string already existed in
`Subtype.kt`.

<details><summary>Original gap description</summary>

Saddle is an activated special action: **"Tap any number of other untapped creatures you control
with total power N or greater: This Mount becomes saddled until end of turn. Saddle only as a
sorcery."** Mounts then gate abilities on the saddled state — "Whenever this Mount attacks **while
saddled**, …" or "As long as it's saddled, …".

**Needs:**
- A new `Keyword.SADDLE(n)` + a `card { saddle(n) }` DSL builder (mirrors `crew(n)`).
- A **special-action / activated-ability** that taps a chosen set of other untapped creatures whose
  **total power ≥ N** (reuses the same "choose creatures totalling power X" selection that Convoke /
  Harmonize's tap-for-power already model on the cost side), sorcery-speed gated.
- A **`SaddledComponent`** (saddled-until-end-of-turn state, cleared in cleanup), analogous to the
  crewed/animated transient states.
- A trigger condition **"attacks while saddled"** and a static condition **"as long as it's
  saddled"** — the attack-trigger plumbing already exists; this is one new `Condition.SourceIsSaddled`.

→ **All 17 Mounts:** Archmage's Newt, Bounding Felidar, Bridled Bighorn, Calamity Galloping Inferno,
  Caustic Bronco, Congregation Gryff, Drover Grizzly, Fortune Loyal Steed, Giant Beaver, Gila
  Courser, Ornery Tumblewagg, Quilled Charger, Rambling Possum, Seraphic Steed, Stubborn
  Burrowfiend, The Gitrog Ravenous Ride, Trained Arynx.

</details>

---

## Tier 2 — Small recurring primitives

### 2. "Contributed this turn" set-tracker (saddled-it / crewed-it) — ✅ DONE

Several Mounts and one Vehicle pay off the **set of creatures that saddled / crewed this permanent
this turn** — to put counters on them, bounce them, or count them.

**Implemented:** a per-permanent `CrewSaddleContributorsComponent(creatureIds)` recorded by both
`CrewVehicleHandler` and `SaddleMountHandler` (union across activations) and cleared at end of turn
in `CleanupPhaseManager`. Two source-relative read surfaces, both keyed off the ability's source via
`PredicateContext.sourceId`:
  - membership — `StatePredicate.CrewedOrSaddledSourceThisTurn` /
    `GameObjectFilter.crewedOrSaddledSourceThisTurn()`, for "target/choose/return a creature that
    crewed/saddled it this turn" (the target system restricts to live creatures).
  - count — `DynamicAmount.CreaturesThatCrewedOrSaddledThisTurn` /
    `DynamicAmounts.creaturesThatCrewedOrSaddledThisTurn()`. Retains contributors that have since
    left, so it counts every creature that crewed it even if some are gone as the ability resolves
    (Luxurious Locomotive ruling).

Tests: `CrewSaddleContributorsScenarioTest`.

→ Unblocks: Giant Beaver, Ornery Tumblewagg, Rambling Possum (saddlers get counters / bounce),
  The Gitrog Ravenous Ride & Calamity (consume the saddlers), **Luxurious Locomotive** (Treasure per
  creature that crewed it this turn — note: dynamic-count Treasure creation is still Int-only; that
  card additionally needs a dynamic `CreateTreasure` or a `CreateTokenEffect`-based Treasure).

### 3. `DynamicAmount` over spells cast this turn (filtered, per-player, exclude-self) — ✅ DONE

The data exists (`GameState.spellsCastThisTurnByPlayer: Map<EntityId, List<CastSpellRecord>>`) and
there is a `YouCastSpellsThisTurn` **condition**, but **no `DynamicAmount`** that reads a *count* of
(optionally filtered: noncreature; optionally "other than this spell") spells a player cast this
turn. Add a `DynamicAmount.SpellsCastThisTurn(player, filter, excludeSelf)`.

→ **Thunder Salvo** ("2 plus the number of other spells you've cast this turn"), **Magebane Lizard**
  ("noncreature spells they've cast this turn").

**Implemented:** `DynamicAmount.SpellsCastThisTurn(player, filter, excludeSelf)` +
`DynamicAmounts.spellsCastThisTurn(...)` facade. `excludeSelf` matches the resolving spell by its
stack entity id via a new `CastSpellRecord.sourceEntityId`. Both cards are now buildable (Thunder
Salvo via `Add(Fixed(2), …excludeSelf=true)`; Magebane via the `Noncreature` filter). Tests:
`SpellsCastThisTurnAmountTest`.

### 4. "Cast from your hand" cast-zone qualifier on the spell-cast tracker — ✅ DONE

**Implemented:** `CastSpellRecord` now carries `castFromZone: Zone?`, stamped in `CastSpellHandler`
from `StackResolver.findCastFromZone(...)` at cast time (the card is still in its origin zone there, so
it agrees with the `SpellOnStackComponent.castFromZone` that `castSpell` sets). Both read surfaces gained
a symmetric `fromZone` qualifier, matched **independently of the spell filter** so a face-down/morph
cast from hand still counts (CR 708.2):
  - condition — `PlayerCastSpellsThisTurn(..., fromZone)` / `Conditions.YouCastSpellsThisTurn(atLeast,
    filter, fromZone)`. The Prairie Dog cycle's "you haven't cast a spell from your hand this turn" is
    `Not(YouCastSpellsThisTurn(1, fromZone = Zone.HAND))`.
  - count — `DynamicAmount.SpellsCastThisTurn(..., fromZone)` /
    `DynamicAmounts.spellsCastThisTurn(..., fromZone)`.

Flashback/forage (GRAVEYARD), plot/foretell (EXILE), and commander (COMMAND) casts are all distinguished
from hand casts. Tests: `CastFromZoneThisTurnTest`.

<details><summary>Original gap description</summary>

`CastSpellRecord` (verified: `typeLine, manaValue, colors, isFaceDown, paidWithTreasureMana`) has
**no source-zone field**, so "you haven't cast a spell **from your hand** this turn" can't be
distinguished from plotted / flashback / impulse casts. Add a `fromHand` (or `sourceZone`) flag to
`CastSpellRecord` and a `fromHand` qualifier on the condition.

→ Inventive Wingsmith, Prairie Dog, Canyon Crab, Emergent Haunting, Jem Lightfoote, Wrangler of the
  Damned, Annie Flash the Veteran. (Stoic Sphinx says "haven't cast a spell this turn" with no zone
  qualifier — buildable today.)

</details>

### 5. "Cast for free / no mana was spent" cast-state condition — ✅ DONE

**Implemented:** `Conditions.NoManaSpentToCast` — the full oracle clause *"it wasn't cast or no mana
was spent to cast it."* No new flag was needed: the engine only stamps the per-permanent
`CastRecordComponent` (the mana-spent record) when total mana spent to cast was > 0, so its absence
(or a zero total) is *exactly* "no mana was spent." The resolution-time evaluator reads that record
off the source, returning true for both free / `{0}` casts **and** uncast permanents (reanimation,
tokens, "put onto the battlefield"), and false the moment any mana is spent — including mana for
additional costs or cost increases on an otherwise-free cast (the Freestrider Commando / Aven
Interrupter ruling). One condition covers the whole clause; `All(WasCast, NoManaSpentToCast)` gives
the narrower "cast, but for free" sense. Tests: `FreestriderCommandoScenarioTest` (normal cast →
3/3 no counters; free cast → 5/5 with two counters).

→ **Freestrider Commando** built (`{2}{G}` 3/3 Plot, `EntersWithCounters(condition =
  NoManaSpentToCast)`). Satoru, the Infiltrator now has its condition primitive, but still needs the
  batch "Satoru and/or one or more other nontoken creatures enter" once-per-turn trigger before it's
  buildable.

### 6b. "Whenever one or more creatures you control die" batch trigger (once per batch) — ✅ DONE

**Implemented:** `EventPattern.CreaturesYouControlDiedEvent(filter, excludeSelf)` +
`Triggers.OneOrMoreCreaturesYouControlDie(...)` facade, detected by
`TriggerDetector.detectCreaturesDiedBatchTriggers` (a new `TriggerCategory.CREATURES_DIED_BATCH`):
it groups battlefield→graveyard zone-change events by each dying creature's **last-known**
controller (so dead tokens survive the 704.5s cleanup) and fires **once per controller per batch**
— a board wipe that kills several of your creatures fires it once, not once per creature.
`excludeSelf = true` models the "*other* creatures" wording. Per-event matching declines the event
in `TriggerMatcher` (batch-handled separately). The detector also honours CR 603.10 "look back in
time": a source that itself dies in the **same** batch as another qualifying creature still sees
that death and fires (recovered from its last-known card definition), so a *non-self* payoff
(draw / token / gain life) survives a board wipe that also kills the source — a per-self payoff like
Vengeful Townsfolk's own +1/+1 is just a harmless no-op once it is in the graveyard. Tests:
`VengefulTownsfolkScenarioTest` (board-wipe = one counter; single death; opponent's deaths; +
source-dies-in-batch look-back, fires & does-not-fire-alone). Docs: card-sdk-language-reference §8.

→ **Vengeful Townsfolk** built. Also the right home for other "one or more … die" payoffs (cf.
  Scavenger's Talent / Spiteful Banditry, which lean on the `oncePerTurn` approximation because
  their printed text *does* say "only once each turn").

<details><summary>Original gap description</summary>

The engine has per-creature dies triggers (`Triggers.YourCreatureDies`, ANY/OTHER binding, fires once
**per dying creature**) and a once-per-*turn* gate (`oncePerTurn = true`), plus batch detectors for
*cards put into a graveyard from anywhere* (`CardsPutIntoYourGraveyardEvent`) and for
leave-battlefield-without-dying / sacrifice / ETB. But there is **no once-per-*batch* trigger
restricted to creatures you control that *died* (battlefield → graveyard)**. "Whenever one or more
**other** creatures you control die, put a +1/+1 counter on this creature" must fire exactly once when
a board wipe kills several of your creatures simultaneously — the per-creature `YourCreatureDies`
over-counts (one counter per dead creature), and `oncePerTurn` is wrong (it should fire again on a
*later* batch the same turn). `CardsPutIntoYourGraveyard(Creature.youControl())` is too broad (also
fires on mill/discard, which aren't "die").

**Needs:** a batch "one or more creatures you control died" trigger event + detector (group the
battlefield→graveyard zone-change events for creatures you control by controller, fire once per batch),
with an OTHER-binding variant for "another creature(s)." Mirrors the existing
`detectAnyToGraveyardBatchTriggers` but scoped to from-battlefield deaths.

→ **Vengeful Townsfolk** ("Whenever one or more other creatures you control die, put a +1/+1 counter
  on this creature"). Also the right home for other "one or more … die" payoffs (cf. Scavenger's
  Talent / Spiteful Banditry, which currently lean on the `oncePerTurn` approximation because their
  printed text *does* say "only once each turn").

</details>

### 6. `CARDS_DRAWN` turn-tracker + characteristic-defining P/T from it — ✅ DONE

~~`TurnTracker` has no `CARDS_DRAWN` accumulator, so "power equal to the number of cards you've drawn
this turn" can't be expressed.~~ RESOLVED — added `TurnTracker.CARDS_DRAWN`, backed by the existing
`CardsDrawnThisTurnComponent` (no new accumulator needed — the engine already tracks per-turn draws
and resets it at turn start). `DynamicAmountEvaluator` reads the component; the value feeds a CDA
power via `dynamicPower = CharacteristicValue.dynamic(TurnTracking(You, CARDS_DRAWN))`.

→ Duelist of the Mind (implemented).

---

## Tier 3 — One-off complex cards (each needs unique new functionality)

7. **Copy an activated/triggered ability already on the stack.** *(partly done)* The unified
   `CopyTargetSpellOrAbilityEffect` (facade `Effects.CopyTargetSpellOrAbility`) now copies whichever
   stack-object kind the single target resolved to — instant/sorcery spell, **activated ability**, or
   triggered ability — via `Targets.InstantSorcerySpellOrAbility`; STACK targeting enumeration was
   widened so an ability becomes a legal target when the filter names an ability predicate. This
   closed the **Return the Favor** copy mode. Still open for **Ertha Jo, Frontier Mentor**: a trigger
   predicate for "an ability that targets a creature or player."

8. **Aura "becomes attached" trigger + control-for-as-long-as-attached + MV-on-attach condition.**
   No "becomes attached" trigger event, no MV-comparison-on-attachment condition, and no control
   duration tied to "while this Aura remains attached." Three small new pieces.
   → **Eriette, the Beguiler**.

9. **Characteristic-defining base P/T on a created token.** Token `dynamicPower/Toughness` are
   evaluated once at creation; there's no Layer-7b CDA "set base P/T = a dynamic count" granted to a
   token (here "= the number of lands you control", recomputed continuously).
   → **Bonny Pall, Clearcutter** (the Beau token).

10. **Additional upkeep steps inserted into the current turn.** Extra *combat* phases and extra
    *turns* are supported, but not inserting N extra upkeep steps after the current phase.
    → **Obeka, Splitter of Seconds**.

11. **"Tap an artifact (token) for mana" trigger + reflexive same-type mana.** The tap-for-mana
    trigger is land-only (`LandTappedForMana`); needs generalization to artifact/permanent-tapped-for-mana
    plus a reflexive effect that mirrors the mana type the tapped object produced.
    → **Roxanne, Starfall Savant** (second ability; her Meteorite-token half is buildable).

12. **Become a copy of a card in this Equipment's linked exile, while attached.** `BecomesCopyOfTarget`
    copies a battlefield target; making the equipped creature a copy of the *card sitting in this
    Equipment's linked exile* (for as long as attached) is a new copy-source + attachment-linked-copy
    duration. The exile-on-ETB half is supported.
    → **Assimilation Aegis**.

13. ~~**Conditional attack-tax + a new block-tax static.**~~ RESOLVED — `AttackTax` gained an optional
    `condition` gate (Archangel uses `Conditions.SourceIsUntapped`; it already protected the controller's
    planeswalkers); added a new global **`BlockTax(amountPerBlocker, condition)`** static (Archangel uses
    `Conditions.SourceIsAttacking`) evaluated in `BlockPhaseManager` and reusing the existing
    declare-blockers tax-confirmation pause.
    → **Archangel of Tithes** (implemented; canonical in ORI, reprint row in OTJ).

14. ~~**Group flicker (mass blink) + repeat-the-whole-effect X+1 times.**~~ RESOLVED — no new SDK
    needed. Composed from existing primitives: an `Effects.Pipeline { gather → chooseAnyNumber →
    exile(linkToSource) → gather(FromLinkedExile) → move(battlefield, underOwnersControl) }` blink,
    run once then `RepeatDynamicTimesEffect(amount = DynamicAmount.XValue, body = …)` for "X more times".
    → **Another Round** (implemented).

15. **Targeted reanimate-attached for Auras/Equipment.** ✅ DONE —
    `Effects.PutOntoBattlefieldAttachedToChosen(target, hostFilter = Creature.youControl())` puts a
    targeted graveyard Aura/Equipment onto the battlefield attached to a host the controller chooses at
    resolution (host is NOT a target). Works for both Auras and Equipment, intersects an Aura's enchant
    legality, and (per ruling) lets an Equipment enter unattached / an Aura stay back when no legal host
    exists. Pausing executor + `PutOntoBattlefieldAttachedToChosenContinuation` reuse the
    MoveCollection attach helper.
    → **One Last Job** (all three Spree modes) — implemented.

16. **Inline "excess damage dealt this way" captured within one resolving spell.** ✅ DONE —
    `EntityNumericProperty.ExcessMarkedDamage` reads `max(0, marked − toughness)` of a context target
    post-damage (the amount-valued twin of the existing `TargetMarkedDamageExceedsToughness` condition).
    Composite resolves with no interleaved SBA, so a `DealDamage → CreateTreasure(count = EntityProperty(
    Target(0), ExcessMarkedDamage))` pipeline reads the just-dealt excess while the creature is still present.
    → **Hell to Pay** (tapped Treasures = excess damage dealt) — implemented.

17. **Condition on the type of a permanent sacrificed as an activated-ability cost.** No primitive
    exposes the identity/type of the cost-sacrificed permanent to a follow-up effect. Needs to record
    the cost-sacrificed entity + a `WasSacrificedThisWayMatching(filter)` condition.
    → **Boneyard Desecrator** ("if an outlaw was sacrificed this way, create a Treasure").

18. **Move a face-up exiled card owned by a player to their graveyard (minor).** No primitive targets
    a face-up card in the exile zone owned by a given player and moves it to that player's graveyard.
    → **Binding Negotiation** (secondary clause only; its hand-discard half is buildable).

---

## Recommended build order

1. ✅ **Saddle N + Mount** (Tier 1) — done.
2. **Tier 2 shared primitives** — ✅ contributors-this-turn tracker (#2), ✅ spells-cast
   `DynamicAmount` (#3), ✅ `fromHand` cast-zone flag (#4), ✅ cast-for-free condition (#5),
   ✅ `CARDS_DRAWN` tracker (#6). All Tier-2 shared primitives done.
3. **Tier-3 one-offs** as the relevant legendaries / rares come up — none block large numbers of
   cards; pick them off individually.

With Plot, Spree, Crime, Outlaw, Treasure, and Crew already done, **roughly 220 of the 254 remaining
cards are buildable today**; Saddle plus the Tier-2/Tier-3 items close the rest.
