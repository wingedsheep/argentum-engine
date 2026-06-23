# Antiquities (ATQ) — Implementation Plan & Triage

> **Every card must be implemented perfectly — exactly as stated in the rules.** No
> approximations, no "close enough", no silently dropped clauses. Each card's behavior must
> match its oracle text (from `atq_set.json`) and the Comprehensive Rules
> (`MagicCompRules_20260417.pdf` / `.txt`, repo root) in full, including edge cases, timing,
> and interactions. A card is not done until its scenario test proves rules-correct behavior.

**Set:** Antiquities (ATQ), 85 cards, released 1994-03-04. **Implemented: 48 / 85** — all 45
no-engine-work (List A) cards plus the 3 pre-existing reprints (Jalum Tome, Sage of Lat-Nam,
Wall of Spears). The remaining 37 are List B (need engine work). Verify with
`scripts/card-status --set ATQ`.

The set's mechanics are catalogued in [`MECHANICS.md`](MECHANICS.md); the checklist is
[`cards.md`](cards.md). This file is the **card-for-card triage**: which cards compose from
existing SDK primitives (no engine work) and which need engine work (with the missing
mechanic named).

## Scaffolding status

**Scaffolded; all List A cards implemented.** `mtg-sets/.../definitions/atq/AntiquitiesSet.kt`
exists (auto-discovered via `CardDiscovery`, with `basicLandsFallback = PortalSet`), and every
List A card has a full `CardDefinition` under `definitions/atq/cards/` with one commit each.
The 37 List B cards remain — each needs the engine feature named in its group below
(`add-feature` territory), to be built in separate PRs.

## Data sources — do NOT hit the network

- **Card data:** read from [`atq_set.json`](atq_set.json) — a full Scryfall dump of all 100
  ATQ prints (85 unique cards), keyed under `.data[]`. Feed `add-card` the matching entry
  instead of a Scryfall lookup.
- **Rules:** cite and verify against `MagicCompRules_20260417.pdf` / `.txt` (repo root), not a
  web source. Quote rule numbers from that document.

## Workflow & git strategy

Each card via the **`add-card` skill** (oracle errata, set registration, scenario test),
sourcing data from `atq_set.json`. One card per Claude invocation. The skill is the source of
truth on whether a card needs an engine change — the buckets below are a triage to sequence
the work; **confirm during implementation.**

1. **One big PR — "Antiquities: cards (no engine change)"**, branch `atq-cards`, one commit
   per card. Only cards that compose from existing SDK primitives.
2. **One PR per engine feature**, off `main`. Group cards that share a missing mechanic so a
   single feature PR can land several (see "Needs engine work" grouping). Update
   `docs/card-sdk-language-reference.md` in the same PR (required for any SDK change).

---

## ✅ List A — Implementable WITHOUT engine work (45 cards)

Compose from existing primitives. Items tagged **(verify)** are composable but non-trivial —
confirm the noted clause during `add-card`; if it surprises you, move it to List B.

### Clean — straightforward compositions
- **Argivian Archaeologist** — `{W}{W},{T}`: return target artifact card from GY to hand.
- **Argivian Blacksmith** — `{T}`: prevent next 2 damage to target artifact creature (`Effects.PreventNextDamage`).
- **Reconstruction** — return target artifact card from your GY to hand.
- **Artifact Blast** — counter target artifact spell (`Effects.CounterSpell` + artifact-spell target).
- **Atog** — `Sacrifice an artifact`: +2/+2 EOT (`Costs.Sacrifice(Artifact)`).
- **Orcish Mechanics** — `{T}, Sacrifice an artifact`: 2 damage to any target.
- **Shatterstorm** — destroy all artifacts, can't be regenerated (mass destroy + no-regen).
- **Citanul Druid** — opponent casts an artifact spell → +1/+1 counter (`opponentCasts(Artifact)`).
- **Gaea's Avenger** — P/T = 1 + artifacts opponents control (dynamic P/T, battlefield count).
- **Amulet of Kroog** — `{2},{T}`: prevent next 1 damage to any target.
- **Ashnod's Altar** — `Sacrifice a creature`: add `{C}{C}`.
- **Candelabra of Tawnos** — `{X},{T}`: untap X target lands (`Effects.Untap`, X targets).
- **Clay Statue** — `{2}`: regenerate this creature.
- **Coral Helm** — `{3}, Discard a card at random`: target creature +2/+2 EOT (`Costs.DiscardAtRandom`).
- **Dragon Engine** — `{2}`: +1/+0 EOT.
- **Grapeshot Catapult** — `{T}`: 1 damage to target creature with flying.
- **Mightstone** — attacking creatures get +1/+0 (static anthem on attackers).
- **Weakstone** — attacking creatures get -1/-0.
- **Millstone** — `{2},{T}`: target player mills two (`Patterns.Library.mill`).
- **Obelisk of Undoing** — `{6},{T}`: return a permanent you own and control to hand.
- **Onulet** — dies → gain 2 life (death trigger).
- **Ornithopter** — 0/2 flying.
- **Staff of Zegon** — `{3},{T}`: target creature -2/-0 EOT.
- **Tablet of Epityr** — artifact you control dies → may pay `{1}`, gain 1 (artifact-dies trigger).
- **Tawnos's Wand** — `{2},{T}`: target creature with power ≤2 can't be blocked this turn.
- **Triskelion** — enters with three +1/+1 counters; `Remove a +1/+1 counter`: 1 damage to any target.
- **Yotian Soldier** — vigilance, 1/4.
- **Strip Mine** — `{T}`: add `{C}`; `{T}, Sacrifice`: destroy target land.

### (verify) — composable but confirm the noted clause
- **Gate to Phyrexia** — `Sacrifice a creature`: destroy target artifact; only your upkeep,
  once each turn (**verify** `ActivationRestriction.OncePerTurn` + upkeep timing).
- **Yawgmoth Demon** — flying, first strike; upkeep "may sacrifice an artifact; if you don't,
  tap it and it deals 2 to you" (**verify** optional-sac-else Tap+Damage on self).
- **Detonate** — `{X}{R}`: destroy target artifact with mana value X (no regen) + X damage to
  controller (**verify** target filtered by the chosen X).
- **Dwarven Weaponsmith** — `{T}, Sacrifice an artifact`: +1/+1 counter on target creature;
  only your upkeep (**verify** upkeep timing).
- **Argothian Pixies** — can't be blocked by artifact creatures + prevent all damage from
  artifact creatures (`CantBeBlockedBy(filter)` + `PreventDamage` static; **verify** source-filtered static prevention).
- **Argothian Treefolk** — prevent all damage from artifact sources (`PreventDamage` static; **verify**).
- **Crumble** — destroy target artifact (no regen); its controller gains life = its mana value
  (**verify** life = target's mana value).
- **Ashnod's Transmogrant** — `{T}, Sacrifice this`: +1/+1 counter on a nonartifact creature;
  it becomes an artifact in addition to its types (**verify** add-type effect).
- **Feldon's Cane** — `{T}, Exile this`: shuffle your graveyard into your library (**verify** exile-self cost).
- **Ivory Tower** — upkeep: gain life = cards in hand − 4 (dynamic, floor 0; **verify**).
- **Rakalite** — `{2}`: prevent next 1 to any target; return to hand next end step (**verify** self-bounce at end step).
- **Su-Chi** — dies → add `{C}{C}{C}{C}` (**verify** mana from a triggered ability).
- **Urza's Chalice** — any player casts an artifact spell → may pay `{1}`, gain 1 (**verify** any-player cast trigger + optional pay).
- **Mishra's Workshop** — `{T}`: add `{C}{C}{C}`, spend only on artifact spells
  (`ManaRestriction`; **verify**).
- **Urza's Mine / Urza's Power Plant / Urza's Tower** — `{T}`: add `{C}`, or more if you
  control the other tron pieces (**verify** conditional mana amount on controlling named lands).

---

## 🔧 List B — NEEDS ENGINE WORK (37 cards), grouped by missing mechanic

Each group is a candidate single feature PR. Confirm with `add-card` before building.

### Artifact-tapped / artifact-ability-activated trigger ✅ DONE
*Trigger: "whenever an artifact becomes tapped, or a player activates an artifact's ability
without `{T}` in its cost."* Implemented: the tap half reuses `Triggers.becomesTapped(filter)`;
the ability half is `Triggers.activatesAbilityWithoutTap(player, sourceFilter, binding)`, backed by
`EventPattern.AbilityActivatedEvent(sourceFilter, requireNoTapInCost)`. The engine now emits
`AbilityActivatedEvent` (carrying `costsTap`/`isManaAbility`) for every activated ability whose cost
lacks `{T}` — mana abilities included — so the literal "{T}-in-cost" wording is honored, distinct
from the existing "isn't a mana ability" wording.
- [x] **Haunting Wind** — global; 1 damage to the artifact's controller.
- [x] **Powerleech** — opponents' artifacts only; you gain 1 life.
- [x] **Artifact Possession** — single enchanted artifact (ATTACHED); 2 damage to its controller.

### "Doesn't untap" control family (untap restrictions / pay-to-untap / tap-locked buffs)
No "you may choose not to untap", "doesn't untap during your untap step", per-permanent untap
suppression, or untap-count restriction primitive exists.
- **Phyrexian Gremlins** — may choose not to untap; `{T}`: tap target artifact, it stays
  tapped while this remains tapped (imposed untap suppression).
- **Colossus of Sardia** — doesn't untap during your untap step; `{9}` in upkeep to untap.
- **Ashnod's Battle Gear** — may choose not to untap; `{2},{T}`: +2/-2 *for as long as this
  stays tapped* (tap-locked buff).
- **Tawnos's Weaponry** — may choose not to untap; `{2},{T}`: +1/+1 *for as long as this stays
  tapped* (tap-locked buff).
- **Damping Field** — players can't untap more than one artifact per untap step (untap-count restriction).

### Characteristic-defining / counter↔token state ✅ DONE
- [x] **Primal Clay** — choose 3/3, 2/2 flying, or 1/6 defender Wall as it enters. Composed from
  existing primitives: `EntersWithChoice(ChoiceType.MODE)` + mode-gated (`SourceChosenModeIs`)
  `SetBasePowerToughnessStatic` / `GrantKeyword` / `GrantSubtype` static abilities. No engine work.
- [x] **Shapeshifter** — choose a number 0–7 as it enters and each upkeep; power = chosen, toughness
  = 7 − chosen. Added the **choose-a-number-stored-on-permanent** primitive: `ChoiceSlot.CHOSEN_NUMBER`
  + `Effects.ChooseNumberForSource` (writes a durable `NumberChoice`) read by a
  `SetBasePowerToughnessDynamicStatic` CDA via `DynamicAmount.CastChoice` (now generic over numeric
  slots). The "you may" re-choice is an optional `YourUpkeep` trigger.
- [x] **Tetravus** — convert +1/+1 counters ↔ Tetravite tokens both ways. Added **token provenance**
  (`CreateTokenEffect.stampCreator` → `CreatedByComponent`; `StatePredicate.CreatedBySource` /
  `.createdBySource()`) so "tokens created with this creature" is recognized, and
  `ConvertCountersToTokensEffect` (counters→tokens). The tokens→counters half composes from a
  gather(`createdBySource`)→chooseAnyNumber→exile→`AddDynamicCounters` pipeline.

### Other one-off engine features (one PR each, or batch small ones)
- **Power Artifact** — enchanted artifact's activated abilities cost `{2}` less (floor 1 mana):
  activated-ability cost-reduction static. No general "abilities cost {N} less".
- **Transmute Artifact** — sacrifice an artifact → search library → put onto battlefield with
  mana-value comparison + optionally pay the difference X. No search-with-MV-comparison +
  pay-difference primitive.
- **Cursed Rack** — chosen player's maximum hand size is 4. Only `NoMaximumHandSize` exists;
  no `SetMaximumHandSize`.
- **Armageddon Clock** — doom counters accrue each upkeep, deal damage to *each* player in the
  draw step, and `{4}`: remove one — **any player may activate**, during any upkeep. Needs
  accruing-counter damage + any-player-activated ability.
- **Golgothian Sylex** — sacrifice every nontoken permanent *originally printed in the
  Antiquities expansion*. No set-membership ("printed in set X") filter (cf. ARN City in a Bottle).
- **Tawnos's Coffin** — exile a creature, *note its counters*, re-attach its Auras on return,
  and return it when this untaps/leaves. State-preserving blink tied to untap.
- **Urza's Avenger** — `{0}`: -1/-1 and gains your *choice* of banding, flying, first strike,
  or trample EOT. No "grant a chosen keyword from a set" primitive.
- **Energy Flux** — all artifacts gain "at the beginning of your upkeep, sacrifice this unless
  you pay `{2}`". Static *grant of a triggered ability* to all artifacts (incl. opponents').
- **Titania's Song** — continuous: every noncreature artifact *loses all abilities* and
  becomes an artifact creature P/T = mana value; lingers until EOT if it leaves. Continuous
  mass-animate with ability removal (`BecomeCreature` is one-shot, not a group static).
- **Reverse Polarity** — gain life = twice the damage dealt to you *by artifacts* this turn.
  Needs a per-turn source-typed damage tracker.
- **Priest of Yawgmoth** — `{T}, Sacrifice an artifact`: add `{B}` equal to the *sacrificed
  artifact's mana value*. `AddMana(DynamicAmount)` exists, but no dynamic amount referencing
  the permanent sacrificed to pay this ability's cost. (verify — may compose if a
  "cost-sacrificed permanent" reference exists.)
- **Artifact Ward** — Aura: also "enchanted creature can't be the target of abilities from
  artifact sources" — hexproof keyed to a *source category* (its can't-be-blocked-by-artifacts
  and prevent-artifact-damage clauses compose; this clause does not). Could be approximated as
  protection from artifacts, but that wrongly also blocks equipping — implement the real clause.
- **Hurkyl's Recall** — "Return all artifacts target player **owns** to their hand." The engine has
  `ControllerPredicate.OwnedByYou`/`OwnedByOpponent` and `ControlledByTargetPlayer`, but **no
  `OwnedByTargetPlayer`** predicate. "Owns" ≠ "controls" once control of an artifact has changed,
  and the spell lets the caster target either player, so a fixed owned-by-you/opponent predicate
  can't capture it either. Needs a small filter addition (`OwnedByTargetPlayer` /
  `OwnedByReferencedPlayer`). (Originally triaged into List A; reclassified during implementation —
  the only "(clean)" card that turned out to need engine work.)

### Reclassified from List A during implementation (confirmed engine gaps)
- **The Rack** — needs a step/phase trigger keyed to *the chosen opponent*. `TriggerMatcher.
  matchesPlayerForStep` resolves only `You`/`Each`/`Opponent`/`EachOpponent`; `Player.ChosenOpponent`
  hits the `else -> true` branch, so a chosen-player upkeep trigger would fire on every upkeep.
- **Urza's Miter** — needs an intervening-if that distinguishes a death *caused by sacrifice* from
  other deaths on a leaves/dies trigger. `ZoneChangeEvent` carries no sacrifice-cause flag (the
  no-condition sibling, Tablet of Epityr, already exists).
- **Mishra's Factory** — `AnimateLandEffect`/`BecomeCreatureEffect` only add the CREATURE type;
  neither can grant the **artifact** type, so "becomes a 2/2 Assembly-Worker *artifact* creature"
  can't be modeled without altering the clause. (Shares the animate-also-grants-artifact gap.)
- **Battering Ram** — needs a *"this creature becomes blocked by a [filter] creature"* trigger
  (blocker-side filter). `BecomesBlockedEvent.filter` filters the attacker, not the blocker; the
  only blocker-filtered trigger (`BlocksOrBecomesBlockedBy`) is strictly broader than "becomes
  blocked by a Wall." (Grant-banding-until-EOC composes fine; the Wall trigger does not.)
- **Clockwork Avian** — needs a **+1/+0 stat counter**. The layer system (`EffectApplicator`) only
  maps +1/+1 and -1/-1 counters to P/T; a +1/+0 counter would be stored but never modify power, so
  the card's core mechanic can't function (the ≤4 cap and attacked/blocked condition are moot until
  that gap is closed).
- **Mishra's War Machine** — needs an "if it dealt damage to you this way, [then] tap it" gate: a
  conditional keyed on whether the sub-effect's damage was actually dealt (vs. prevented/replaced).
  `IfYouDo`/`SuccessCriterion` has no "damage was dealt" criterion.
- **Rocket Launcher** — needs an activation restriction "controlled continuously since the
  beginning of your most recent turn" (artifact summoning-sickness). No `ActivationRestriction` /
  `Condition` exposes it; `SourceEnteredThisTurn` is not equivalent (ignores control changes).
- **Circle of Protection: Artifacts** — needs a "choose an **artifact** source, prevent its next
  damage" prevention filter. `PreventionSourceFilter` has `ChosenSource` / `ChosenColoredSource` /
  `ChosenCreatureType` but no `ChosenArtifactSource`; the executor only supports a `coloredOnly`
  constraint. `ChosenSource` alone would silently drop the artifact restriction.
- **Martyrs of Korlis** — `RedirectDamage` (and `EventPattern.DamageEvent`) has no condition slot,
  so the redirect can't be gated on "as long as this creature is **untapped**." Needs a
  conditional/restricted replacement effect.
- **Xenic Poltergeist** — needs `BecomeCreature` with **dynamic** P/T equal to the target's own
  mana value. `BecomeCreatureEffect` takes only fixed `Int` P/T; `SetBasePowerEffect` is dynamic
  but power-only and doesn't add the creature type. No dynamic-P/T animate primitive.
- **Drafna's Restoration** — needs (a) an "**any number of target** cards in a **separately
  targeted** player's graveyard" predicate (same owned/controlled-by-target-player family as
  Hurkyl's Recall) and (b) player-chosen **ordering** of a target collection onto the library top.
- **Goblin Artisans** — coin-flip + targeted counter composes, but "…that isn't the target of an
  ability from another creature named Goblin Artisans" needs a self-referential targeting-
  restriction predicate that doesn't exist. Skipped rather than ship with the clause dropped.

### Postpone (no engine support, by project policy)
- **Bronze Tablet** — ante zone. The engine has no ante support; postpone, consistent with the
  ARN ante stance (see memory: "Postpone ante cards").

---

## Notes
- Battlefield filtering must use projected state (`matchesWithProjection`).
- Verify any MTG rule number against `MagicCompRules_20260417.pdf`/`.txt` (repo root) before
  citing it — never a web source.
- The "(verify)" tags in List A mark the cards most likely to surprise you into List B during
  `add-card`; everything else in List A should compose cleanly.
- Counts: 3 pre-existing + 45 List A + 37 List B = 85. (All 45 List A cards now implemented.)
