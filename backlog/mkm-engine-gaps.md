# Murders at Karlov Manor — Engine Gap Analysis

Cross-reference of the **260 remaining (unimplemented, non-basic) MKM cards** against the engine's
actual capabilities (SDK reference + source verification, June 2026). Generated to scope what must be
built before the set can be completed.

**Status:** 14 / 276 implemented (5%). The 14 done are almost entirely the ten surveil dual lands
(Raucous Theater, Hedge Maze, Undercity Sewers, Elegant Parlor, Underground Mortuary, Lush Portico,
Meticulous Archive, …) — essentially no MKM *mechanic* is built yet. Card list comes from
`scripts/card-status --list --set MKM`; oracle text pulled from Scryfall (`set:mkm`, 279 printings).

## Bottom line

MKM is a murder-mystery set built around **five new mechanics** — *Investigate / Clue*, *Disguise*,
*Cloak*, *Cases*, and *Collect evidence* — plus two returning ones the engine **already has**
(*Suspect* and *Surveil*). Of the 260 missing cards:

| | cards |
|---|---|
| **Blocked** by at least one unbuilt mechanic | **109** |
| **Buildable today** with existing primitives (suspect, surveil, split, adventure, …) | **151** |

So the bulk of the set is reachable once the five headline mechanics land. The mechanic spread of the
260 missing cards (a card can touch more than one):

| Mechanic | Cards | Engine status |
|---|---|---|
| Investigate (create a Clue) | 38 | ❌ Clue token unregistered; no `Effects.Investigate` |
| Disguise | 37 | ❌ no keyword (morph machinery exists, reusable) |
| Collect evidence N | 22 | ✅ **done** (`CostAtom.CollectEvidence`, `Effects.CollectEvidence`, CR 701.59) |
| Cases (Enchantment — Case) | 12 | ❌ no Case subtype / solve framework |
| Suspect | 19 | ✅ **done** (`Effects.Suspect`, CR 701.60) |
| Surveil | 11 | ✅ **done** (`EffectPatterns.surveil`) |
| Cloak | 5 | ❌ no manifest/cloak effect |

### Already supported — no new engine work

- **Suspect** (CR 701.60) — `Effects.Suspect(target)` is a complete atomic: named "suspected" status +
  granted menace + "can't block", queryable for "becomes suspected" triggers. Built earlier; covers
  the suspect half of all 19 suspect cards.
- **Surveil N** — `EffectPatterns.surveil(n)` (the ten MKM surveil duals already use it).
- **Split cards** — `CardLayout.SPLIT` is fully wired (cast routing in `CastSpellEnumerator` /
  `StackResolver`, proven by the Invasion split cards: Assault // Battery, Pain // Suffering, …). The
  five MKM splits (**Cease // Desist, Flotsam // Jetsam, Fuss // Bother, Hustle // Bustle, Push //
  Pull**) need only their halves' effects, not framework.
- **Adventure DFC** (CR 715) — done (TDM). **Kellan, Inquisitive Prodigy // Tail the Suspect** rides
  it (its Adventure also wants Investigate + `PlayAdditionalLandsEffect`, both available).
- **Leyline "begin the game on the battlefield"** — `LeylineContinuations` + `MulliganHandler`
  support it. **Leyline of the Guildpact** needs only its two continuous statics (see Tier 3 §18).
- **Impulse / play-from-exile with any mana type** — `Effects.GrantMayPlayFromExile(withAnyManaType
  = true)` covers **Outrageous Robbery** and the impulse halves of others.
- **`PlayAdditionalLandsEffect`** (Summer Bloom), **`BecomeAllColors`**, the **WITHER** keyword,
  **keyword counters** (deathtouch/trample/etc. — TDM), the **predefined-token framework** (Treasure,
  Food, Map, …), the **morph turn-face-up machinery** (`TurnFaceUpEnumerator`, `MorphDataComponent`),
  **"cards leave your graveyard" trigger** (TDM) — all building blocks the gaps below compose.

What follows are the **genuine gaps** — elements no current SDK primitive expresses.

---

## Tier 1 — Headline mechanics (109 cards, highest leverage)

### 1. Investigate / Clue token (38 cards) — *lowest risk; framework already exists*

"Investigate. *(Create a Clue token. It's an artifact with "{2}, Sacrifice this token: Draw a
card.")*" Clue is **not** in `PredefinedTokens.kt` and there is no `Effects.CreateClue` /
`Effects.Investigate` facade (the framework doc explicitly names "`Effects.CreateClue()`" as the
worked example of *how to add one* — it was never added).

**Work:** register the **Clue** CardDefinition in `PredefinedTokens.kt` (colorless artifact, the
sac-to-draw ability) + add an `Effects.Investigate(n = 1)` facade over
`CreatePredefinedTokenEffect("Clue", count = n)`. No new engine subsystem.

**Clue-matters payoffs** (compose once Clue exists):
- "Whenever you sacrifice a Clue, …" — a sacrifice trigger filtered to the Clue token. Needs a
  Clue-scoped sacrifice trigger (Curious Cadaver, Lazav, Cold Case Cracker). Minor.
- "Sacrifice a Clue" / "Sacrifice an artifact" cost, count Clues / artifacts — already generic
  (`Costs.Sacrifice(filter)`, artifact counts).

→ Novice Inspector, Deduce, Magnifying Glass, Forensic Gadgeteer, Ezrim Agency Chief, Teysa Opulent
  Oligarch, Wojek Investigator, Officious Interrogation, Detective's Satchel, … (38)

### 2. Disguise (37 cards) — *reuses morph; needs ward-while-face-down + X turn-up*

"Disguise {cost} *(You may cast this card face down as a 2/2 creature for {3}. Turn it face up any
time for its disguise cost.)*" Disguise is **morph + ward {2} on the face-down permanent**. The
morph machinery is built (`KeywordAbility.Morph`, `MorphDataComponent`, `TurnFaceUpEnumerator`,
cast-face-down) but there is **no `Disguise` keyword** and morph does **not** grant the face-down
creature ward {2}.

**Work:** add `KeywordAbility.Disguise(cost)` that reuses the morph face-down/turn-up pipeline, grants
the face-down permanent **ward {2}**, and supports an **X in the turn-up cost** (Aurelia's Vindicator
is `Disguise {X}{3}{W}`, where X is chosen at turn-up and feeds a "turned face up" trigger). "When
this is turned face up" triggers already work through the morph path. The "face-down 2/2 with ward
{2}" piece is **shared with Cloak** below — build it once.

→ Aurelia's Vindicator, Nightdrinker Moroii, Fugitive Codebreaker, Cryptic Coat target, Riftburst
  Hellion, Unyielding Gatekeeper, Gadget Technician, … (37)

### 3. Collect evidence N (22 cards) — ✅ **DONE**

Built as one shared `CostAtom.CollectEvidence(n)` plus a resolution-time `Effects.CollectEvidence`,
all routed through the engine's `CollectEvidenceResolver` so the cost and effect forms cannot drift.
The optional *linked* cast cost (`card { collectEvidence(n) }`) rides the existing
optional-additional-cost rail under `ChoiceSlot.EVIDENCE_COLLECTED`, read back by
`Conditions.WasEvidenceCollected`; `Triggers.WheneverYouCollectEvidence` is the payoff. Selection is
sum-gated (`minTotalManaValue` / `exileMinTotalManaValue`), and CR 701.59b fails closed everywhere.
Six cards ship with it (Vitu-Ghazi Inspector, Crimestopper Sprite, Bite Down on Crime, Sample
Collector, Forensic Researcher, Surveillance Monitor); the rest are unblocked but not yet written,
and four need *separate* features first — see below.

**Still blocked on other features:** Conspiracy Unraveler (collect evidence as an *alternative* cost
"rather than pay the mana cost"), Axebane Ferox (`WardCost` has no collect-evidence variant), Urgent
Necropsy and Incinerator of the Guilty (a *dynamic* N — "collect evidence X" where X is the targets'
total mana value / a chosen X; the atom takes a fixed `Int`), Detective's Phoenix (a bestow cost with
a non-mana component), Kylox's Voltstrider (needs "cards exiled with it" linkage).

<details><summary>Original analysis</summary>

### 3. Collect evidence N (22 cards) — *new cost/payment type*

"Collect evidence N. *(Exile cards with total mana value N or greater from your graveyard.)*" No cost,
payment, or effect primitive exists. It appears in **three syntactic shapes**:

- **One-shot gate:** "Collect evidence N. If you do, <bonus>." (most cards — Extract a Confession,
  Bite Down on Crime, Toxin Analysis).
- **Additional cost:** "As an additional cost to cast this, collect evidence N."
- **Alternative cast cost:** Conspiracy Unraveler — "You may collect evidence 10 rather than pay the
  mana cost." (an alternative-cost path, like Harmonize/Convoke plumbing.)

**Work:** a new graveyard **subset-selection-by-MV-sum** primitive — player chooses a set of
graveyard cards whose total mana value ≥ N, which are then exiled (akin to Delve, but MV-sum-thresholded
and selection-driven rather than per-card). Wire it as (a) a resolution-time `MayEffect`-gated payment,
(b) an `AdditionalCost`, and (c) an alternative cast cost. "Evidence collected this way" payoffs read
the exiled set.

→ Extract a Confession, Deadly Cover-Up, Urgent Necropsy, Forensic Researcher, Conspiracy Unraveler,
  Izoni Center of the Web, Sample Collector, Incinerator of the Guilty, … (22)

</details>

### 4. Cases — Enchantment — Case (12 cards) — *new subtype + solve framework*

A Case is a three-line enchantment:
```
When this Case enters, <ETB effect>.
To solve — <condition>. (If unsolved, solve at the beginning of your end step.)
Solved — <ability active only while solved>.
```
There is **no `Case` enchantment subtype, no solved-state**, and no end-step solve check.

**Work:**
- Add the **Case** subtype + a `SolvedComponent` (sticky flag — once solved, stays solved).
- A **turn-based / triggered solve check at the controller's end step** that evaluates the Case's
  arbitrary **"to solve" `Condition`** and flips the flag. Most of the conditions already exist as
  trackers — `YouCastSpellsThisTurn` (Ransacked Lab: 4+ instants/sorceries), attacked-with-creatures
  count (Gateway Express: 3+ attackers), permanents/counters thresholds — so the framework, not the
  conditions, is the work.
- Gate the **"Solved —"** ability (static *or* triggered) on `SolvedComponent`. Reuses the existing
  condition-gated static/trigger machinery once the solved flag is queryable.

→ Case of the Gateway Express, Case of the Ransacked Lab, Case of the Crimson Pulse, Case of the
  Stashed Skeleton, Case of the Pilfered Proof, … (12)

### 5. Cloak (5 cards) — *manifest variant; needs the manifest framework first*

"Cloak the top card of your library. *(Put it onto the battlefield face down as a 2/2 creature with
ward {2}. Turn it face up any time for its mana cost if it's a creature card.)*" **Manifest is not
implemented as an effect** (only `FaceDownComponents` scaffolding + a `StatePredicate` reference
exist). Cloak = manifest + ward {2}, with face-up cost = the card's own mana cost (only if it's a
creature card).

**Work:** an `Effects.Cloak(source)` that moves a card **from a specified zone** (top of *your*
library, top of *target opponent's* library, or *from hand*) face down onto the battlefield as a 2/2
with **ward {2}** (shares the face-down-with-ward piece with Disguise §2). Generalize the
`TurnFaceUpEnumerator` face-up path so a cloaked/manifested card turns up for **its actual mana cost,
gated on being a creature card** (morph turns up for the printed morph cost — different rule). Each
caller varies only the source zone.

→ Cryptic Coat, Hide in Plain Sight, Vannifar Evolved Enigma, Etrata Deadly Fugitive, Expose the
  Culprit. (Etrata also grants face-down creatures a turn-up-or-exile-and-cast ability — composes on
  the same face-down infrastructure.)

---

## Tier 2 — Small recurring primitives

6. **Clue-scoped "whenever you sacrifice a Clue" trigger.** Compose a sacrifice trigger narrowed to
   the Clue token name/type. → Curious Cadaver, Lazav, Cold Case Cracker, Detective's Satchel.

7. **Detective-matters.** *Detective* is already a registered creature subtype; "another Detective",
   "Detectives you control" are plain filtered statics/counts. No engine work — flagged only because
   it recurs (Sharp-Eyed Rookie, Case File Auditor, Agency Outfitter, …).

8. **Generic named counters** ("impostor", "ticket") — verify `AddCounters` accepts an arbitrary
   counter name (the engine already routes counter-type strings through `resolveCounterType`). If so,
   Illicit Masquerade's impostor counters are free. → Illicit Masquerade, Dramatic Accusation.

9. **Wither granted to a group** ("Creatures you control have wither"). The `WITHER` keyword exists;
   confirm a static grant-keyword-to-filtered-group covers it. → Massacre Girl, Known Killer.

10. **"Toughness was less than 1" death trigger** reading the dying creature's last-known toughness
    (works off the LKI snapshot the engine already keeps for dies triggers). → Massacre Girl.

---

## Tier 3 — One-off complex cards (each needs unique new functionality)

11. **Reenact the Crime — "put into a graveyard from anywhere this turn" filter.** Needs a per-card
    *entered-graveyard-this-turn* tracker to constrain the exile target; the copy-and-cast tail
    already exists (`CopyCardIntoCollectionEffect` + cast-without-paying, TDM §11).

12. **Lazav, Wearer of Faces — exile linked to a specific Clue, then copy on its sacrifice.** The
    attack trigger exiles a card *and* investigates; sacrificing *that* Clue lets Lazav become a copy
    of the linked exiled card. Needs exile-card ↔ Clue linkage tracking (akin to linked-exile
    `CardSource.FromLinkedExile`, but keyed to a token).

13. **Doppelgang — "for each of X target permanents, create X tokens that are copies."** X targets
    *and* X copies each; a multi-target X with a per-target token-copy loop. New X-target-count ×
    dynamic-copy-count shape.

14. **Slime Against Humanity — deckbuilding exception + cross-zone owned count.** "A deck can have any
    number of cards named Slime Against Humanity" (deck-validation exception) and X = "cards you own in
    exile and your graveyard that are Oozes or named Slime Against Humanity" (a `DynamicAmount`
    counting owned cards across two zones by subtype/name).

15. **Niv-Mizzet, Guildpact — "different two-color pairs among your exactly-two-color permanents."** A
    bespoke `DynamicAmount` (enumerate two-color permanents, count distinct color-pair combinations) +
    **"hexproof from multicolored"** (a protection/hexproof variant keyed to a multicolored source —
    sibling of TDM's "hexproof from monocolored").

16. **Connecting the Dots — hidden impulse pile.** Exile cards face down "you can't look at it" (hidden
    even from the controller), later return *all* cards exiled with this enchantment to hands. Needs a
    source-linked exile pile + owner-hidden visibility.

17. **Outrageous Robbery / impulse-from-opponent's-library with any mana type.** Covered by
    `GrantMayPlayFromExile(withAnyManaType = true)` — listed only to confirm it's *not* a gap.

18. **Leyline of the Guildpact — two continuous statics.** "Each nonland permanent you control is all
    colors" (a group `BecomeAllColors` static — the single-target effect exists; needs a group/static
    form) and **"Lands you control are every basic land type in addition to their other types"** — a
    continuous *land-type-adding* static for a filtered group (no current primitive adds basic land
    types continuously to a group).

19. **Coerced to Kill — control + base-P/T set + add type via Aura.** Gain control of enchanted
    creature + set base P/T 1/1 + grant deathtouch + add Assassin type. Composable from existing
    control/CDA/type-adding pieces; flagged for the bundle.

---

## Recommended build order

1. **Investigate / Clue** (register token + facade) — unblocks 38 cards for almost no engine work; do
   first.
2. **Disguise** (morph + face-down ward {2} + X turn-up) and **Cloak** (manifest + ward {2}) together
   — they share the "face-down 2/2 with ward {2}" infrastructure; ~42 cards.
3. **Collect evidence** (graveyard MV-sum subset payment, three shapes) — ~22 cards.
4. **Cases** (Case subtype + end-step solve check + solved-gated abilities) — 12 cards; most "to
   solve" conditions already have trackers.
5. **Tier 2 + Tier 3** one-offs as the relevant rares/legendaries come up.

Suspect, Surveil, Split, and Adventure already being done means the 151 "buildable today" cards —
including most commons/uncommons and many of the legendaries — are reachable as soon as the shared
Tier-1 effects land.
