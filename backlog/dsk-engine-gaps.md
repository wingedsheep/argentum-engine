# Duskmourn: House of Horror — Engine Gap Analysis

Cross-reference of the **175 remaining (unimplemented) DSK cards** against the engine's actual
capabilities (SDK reference + source verification, June 2026). Generated to scope what must be
built before the set can be completed.

**Status:** 101 / 276 implemented (37%). Card list from `scripts/card-status --list --set DSK`;
oracle text pulled from the Scryfall API (`set:dsk`, 276 unique cards). The five basic lands among
the 175 are trivial; the 170 non-basic cards were each checked against the SDK.

Sources for the mechanics rundown:
- [Duskmourn: House of Horror Mechanics — WotC](https://magic.wizards.com/en/news/feature/duskmourn-house-of-horror-mechanics)
- [Duskmourn: House of Horror Release Notes — WotC](https://magic.wizards.com/en/news/feature/duskmourn-house-of-horror-release-notes)

## Bottom line

**All five headline mechanics are already built** — the hard structural work for this set is done.
DSK leans on Impending, Eerie, Survival, Manifest dread, Delirium, and Rooms, and every one of those
is in the engine and proven by implemented cards. As a result the large majority of the 170 remaining
cards are **buildable today** as compositions of existing primitives (standard creatures, the Nightmare
"Fear of …" enchantment-creatures, Eerie/Delirium payoffs, Rooms, manifest-dread creatures, life-13
dual lands, the impending Overlord cycle, the Glimmer "Enduring" cycle).

What remains are **~12 genuine gaps** — almost all one-off rares/uncommons — plus one small cluster
(Rooms door-state exposure) that unlocks several cards at once. None require a new mechanic subsystem
on the scale of the headline keywords.

### Marquee mythics (held out of autonomous scope — scoped June 2026)

The five headline mythics were deliberately kept out of autonomous card-authoring scope on the
expectation they need engine work. Verified against source, the picture is narrower than feared —
four are small, isolated SDK additions; only **Marina Vendrell** reshapes a subsystem. Each maps to
the numbered gaps below:

| Card | Engine gap(s) | Lift |
|------|---------------|------|
| **Zimone, All-Questioning** | prime/numeric-predicate condition (#12) | XS |
| **Kaito, Bane of Nightmares** | "source has ≥1 loyalty counter" condition (#15) | S |
| **Meathook Massacre II** | opponent-decides-or-you-steal death-trigger flow (#16) | M |
| **Valgavoth, Terror Eater** | linked-exile from a *replacement* effect (#17) + dynamic pay-life-=-MV alt-cost (#18) + `Ward—Sacrifice` count param (#19) | M |
| **Marina Vendrell** | Rooms door-state model: lock/unlock *effect* + re-lock transition + events (#1b, below) | **L** |

Notes that correct earlier optimism: Valgavoth and Marina-the-creature were previously filed under
"buildable-but-complex pure compositions" — that's wrong. Valgavoth's linked-exile pile is populated by
a *replacement* redirect, which does **not** link to the source today (#17); and Marina the creature
(distinct from *Marina Vendrell's Grimoire*, a separate card) needs door **re-locking**, which the
grow-only door model cannot represent (#1b). Kaito's Ninjutsu is already built — it ships as the
**"Sneak"** keyword (from TMNT), and the engine already permits sneak-attacking a planeswalker, so
planeswalker-as-ninja is covered.

### Already supported — no new engine work

- **Impending N** — `impending(n, cost)` DSL + `KeywordAbility.Impending`; time-counter alt-cost,
  not a creature while counters remain, end-step countdown. (Overlords of the Boilerbilges, Floodpits,
  and Hauntwoods implemented; the white **Overlord of the Mistmoors** remains. mtgish now renders the
  whole Impending + "enters or attacks" Overlord shape at AUTO tier — all five cycle members draft whole.)
- **Eerie** — ability word (no keyword); triggers on "an enchantment you control enters" or "you
  fully unlock a Room." Payoffs are plain triggered abilities.
- **Survival** — ability word modeled as an intervening-if trigger: "at the beginning of your second
  main phase, if this creature is tapped, …". Proven by Acrobatic Cheerleader / Cautious Survivor /
  House Cartographer.
- **Manifest dread** — look at top 2, manifest one face-down 2/2, other to graveyard; face-down
  permanents + turn-face-up special action (`FaceDownComponents`, `CreatureTurnedFaceUpEvent`).
- **Delirium** — `Conditions.Delirium(count=4)` (four+ card types in graveyard), plus a `DynamicAmount`
  for distinct card-type count.
- **Rooms** — split-layout enchantments with two doors; `RoomComponent` records per-face door state,
  unlock-the-other-door is a sorcery-speed special action, "when you unlock this door" /
  "fully unlock" triggers fire (`StackResolver` + `UnlockRoomDoorHandler`). Proven by Unholy Annex //
  Ritual Chamber.
- **Leylines** — `leyline()` DSL (cast from opening hand). Leyline of the Void = `RedirectZoneChange`.
- Building blocks several gaps below compose: enters-tapped-unless-a-player-has-≤13-life dual lands
  (`EntersTapped(unlessCondition=…)`), exile-until-leaves, impulse play-from-exile, distribute counters,
  `MoveAllLastKnownCounters`, copy-token-of-target, `EachPermanentBecomesCopyOfTarget`,
  `AdditionalSourceTriggers` (trigger doubling), `PayOrSuffer` / per-opponent edicts, stun counters,
  `AbilityResolutionCountThisTurn` ("the first/second/third time this turn").

What follows are the **genuine gaps** — elements no current SDK primitive expresses.

---

## Tier 1 — Rooms door-state exposure (small cluster, highest leverage among gaps)

The engine *tracks* door-unlock state in `RoomComponent` (`isFullyUnlocked`, per-door set) but does
**not expose it to the SDK** for counting, gating, or cost reduction. Several "Rooms-matter" cards are
blocked only by this. One cohesive feature closes all of them.

1. **Unlocked-door counting + gating.** Add (a) a `GameObjectFilter` / `StatePredicate` for "a Room
   you control with an unlocked door," (b) a `Condition` for "N+ unlocked doors among Rooms you
   control," and (c) a `DynamicAmount` that counts unlocked doors. These then compose into the
   existing `Count` / `Compare` machinery.
   → **Rampaging Soulrager** (static buff while ≥2 unlocked doors), **Smoky Lounge // Misty Salon**
     (token X = unlocked doors you control).

2. **Distinct names among unlocked doors (alt-win).** A `DynamicAmount` that walks Rooms you control
   and counts *distinct names of unlocked door faces* (not whole-entity names — the existing
   `Aggregation.DISTINCT_NAMES` counts entities, not per-face). Feeds the existing `WinGame`-gated-by
   -`Compare` shape (Simic Ascendancy precedent).
   → **Central Elevator // Promising Stairs** ("you win if eight or more different names among
     unlocked doors of Rooms you control").

3. **Unlock-action cost reduction.** Cost reduction is modeled only by `ModifySpellCost`, whose
   `SpellCostTarget` variants all describe *spell casting*; `UnlockRoomDoorHandler` pays
   `face.manaCost` directly with no reduction hook. Add an unlock-action cost-reduction scope and
   route the unlock handler through the cost calculator.
   → **Inquisitive Glimmer** ("unlock costs you pay cost {1} less"; its enchantment-spell discount is
     already buildable via `ModifySpellCost`).

### 1b — Door lock/unlock as an *effect*, and re-locking (heavier — a model change, not exposure)

Distinct from the exposure cluster above: this changes the door-state *model*. Today unlock is a
player **special action** only (`UnlockRoomDoorHandler`, CR 709.5e) — there is no `Effect` that
unlocks a door, and `RoomComponent.unlocked` is a **grow-only** `Set<RoomFaceId>` ("locked" = "not in
the set"), with only a `DoorUnlockedEvent`. There is no way to remove a face from the set, no
`DoorLockedEvent`, and no lock-vs-unlock choice. Closing this needs: (a) a door-state model that can
represent the unlocked→locked transition, (b) a `LockOrUnlockDoor` effect that targets a Room you
control and carries the lock/unlock choice (and the per-door choice), and (c) `DoorLocked` /
`DoorUnlocked` events so triggers stay consistent. This is `add-feature` work on the Rooms subsystem.
   → **Marina Vendrell** (`{T}: Lock or unlock a door of target Room you control. Activate only as a
     sorcery.` — the ETB reveal-7/enchantments-to-hand/rest-to-bottom-random half is a pure
     composition, and sorcery-speed timing already exists via `TimingRule.SorcerySpeed`).

---

## Tier 2 — Player-targeted & player-scoped effects

The engine attaches auras and floating replacements to **permanents**; DSK has a few effects scoped
to a **player** that have no home yet.

4. **Enchant player.** No aura can attach to a player: every aura uses `AttachedToComponent`
   against a permanent, there is no `EnchantedPlayer` target reference, and no player-recipient
   "is dealt damage" trigger bound to the enchanted player. Needs a player-attachment subsystem +
   an `EnchantedPlayer` reference. (Payoffs themselves exist: `PreventLifeGain`, lose-half-life-rounded-up.)
   → **Grievous Wound** (the only enchant-player card in DSK).

5. **Durable, source-independent life-gain lock on a player.** `PreventLifeGain` exists only as a
   static replacement on a permanent that ends when that permanent leaves. Needs an effect that
   tags a specific player so their life gain stays locked *for the rest of the game*, decoupled from
   the source. (The damage-reflection half is the supported Tephraderm pattern.)
   → **Screaming Nemesis**.

6. **Set maximum hand size to a dynamic value.** Only `NoMaximumHandSize` / `RemoveMaximumHandSize`
   exist (remove the cap). Needs a `SetMaximumHandSize(target, DynamicAmount)` static + a per-player
   max-hand-size override read by the cleanup-discard SBA.
   → **Winter, Misanthropic Guide** (opponents' max hand size = 7 − card types in your graveyard).

---

## Tier 3 — One-off complex cards (each needs unique new functionality)

7. **Gain all activated abilities of a battlefield-filtered group.** The only "gains all activated
   abilities" primitive reads the source's *linked exile pile*. Needs a static that grafts every
   activated ability from a live `GroupFilter` of permanents you control (excluding same-name),
   re-mapping each grafted ability's self/`{T}` references onto the source.
   → **Marvin, Murderous Mimic**.

8. **Cross-zone type granting.** `GrantSubtype` (Layer 4) resolves against battlefield projection
   only. This card also sets the type of creature *spells on the stack* and creature *cards you own
   in other zones* (hand/library/graveyard/exile) — a cross-zone characteristic-defining layer the
   projection system doesn't model (Conspiracy / Xenograft family).
   → **Leyline of Transformation**.

9. **Exile all-but-bottom-N of a library + library-size dynamic amount.** No effect exiles "all but
   the bottom N," and there is no `LibrarySize` / `CardsInLibrary` dynamic amount to derive the count.
   Needs either a dedicated `ExileAllButBottomN(faceDown, eachPlayer)` effect or a `LibrarySize`
   dynamic amount feeding a count-based exile.
   → **Doomsday Excruciator** (each player exiles all but the bottom six, face down).

10. **"One per distinct power" selection restriction.** The `OnePer*` selection family
    (`OnePerCardType` / `OnePerColor` / `OnePerCardName` / `OnePerBasicLandType`) has no
    distinct-numeric-property variant. Needs `OnePerNumericProperty(POWER)` (a.k.a. "different powers")
    wired into the pipeline selector.
    → **Rip, Spawn Hunter** ("any number of creature/Vehicle cards with different powers").

11. **"Can't block alone" (co-blocker restriction).** `CantAttackUnlessCoAttacker` exists (set-of-
    co-attackers check), but `CantBlockUnless` takes only a `Condition`, not a co-blocker-set check.
    Needs a sibling `CantBlockUnlessCoBlocker`.
    → **Toby, Beastie Befriender** (its Beast token "can't attack or block alone" — the attack half
      already maps to `CantAttackUnlessCoAttacker`).

12. **Prime-number count condition.** ✅ **Done.** Added the unary numeric-predicate condition
    `NumberMatches(amount, NumberProperty.{Prime,Even,Odd,MultipleOf})` (facades
    `Conditions.AmountIsPrime/Even/Odd/MultipleOf`) — the counterpart to the threshold-only `Compare`
    family. The arithmetic lives in `ConditionEvaluator` so `NumberProperty` stays pure data
    (mirroring `ComparisonOperator`); dual-mode (resolution + projection). The "make a Fractal with X
    +1/+1 counters where X = land count" half composes the existing `CREATED_TOKENS` +
    `AddDynamicCounters(PipelineTarget)` shape.
    → **Zimone, All-Questioning** (implemented).

13. **Opponent routes the caster's chosen targets.** The caster targets two creatures one opponent
    controls; *that opponent* then chooses which takes 5 damage and which can't block. This differs
    from `TargetChooser.Opponent` (opponent picks the target) — here the caster picks both targets and
    a different player selects which sub-effect hits which. Needs a resolution-time "opponent chooses
    one of these N targeted objects; route effect A to it, effect B to the rest" decision.
    → **Trial of Agony**.

14. **Repeat-an-effect-N-times (dynamic count) with cross-iteration entity accumulation.** No
    iteration space repeats a body a *counted* number of times: `IterationSpace` covers
    targets/players/collection/group/colors, and `RepeatWhileEffect` is a do-while gated by a
    player choice or a `Condition` — neither runs a body exactly *X* times where X is a chosen
    cast-time value. The card also needs each iteration's manifested creature accumulated into one
    collection so the follow-up "put X +1/+1 counters on **each of those creatures**" can target
    exactly the set just created (`AddCountersToCollection` with `DynamicAmount.XValue`). Needs an
    `IterationSpace.Count(DynamicAmount)` (or a `RepeatNTimes`) plus an accumulating
    `storeMovedAs`-style sink that survives across iterations.
    → **Valgavoth's Onslaught** ("Manifest dread X times, then put X +1/+1 counters on each of
      those creatures"). Skipped during the DSK spells batch (substituted **Come Back Wrong**)
      rather than approximated — `add-feature` territory.

15. **"Source has one or more loyalty counters" condition.** `ConditionalStaticAbility` +
    `BecomeCreatureEffect` already express "becomes a 3/4 Ninja with hexproof," and a "during your
    turn" gate exists — but `SourceConditions` has no loyalty-counter test. Add
    `SourceHasCounter(CounterType.LOYALTY)` (or `…GreaterThan(LOYALTY, 0)`) so the conditional
    planeswalker-animation can be gated. (Worth a combat sanity test that the conditionally-animated
    planeswalker deals/takes damage correctly; the parts are otherwise proven.)
    → **Kaito, Bane of Nightmares** (everything else exists: Ninjutsu = the **Sneak** keyword, emblem
      with a `Ninjas get +1/+1` static, `Surveil 2`, draw-per-`TurnTracker.OPPONENTS_WHO_LOST_LIFE`,
      tap + two `STUN` counters).

16. **Opponent-decides-or-you-steal death trigger.** `PayOrSuffer` routes its decision to the
    *source's* controller; here the decision belongs to the **dying creature's controller** and the
    consequence is *you* reanimating their card under your control with a finality counter. Needs the
    pay/suffer decision routed to a non-source player with a theft (controller-override return)
    consequence. (The `you`-side half — "your creature dies, you may pay 3 life, return it under your
    control with a finality counter" — composes from `TriggeringEntity` + `MoveToZoneEffect(controllerOverride)`
    + entering finality counter, but is untested end-to-end and should get a scenario test. `{X}{X}`
    → X-driven `each player sacrifices X`, and `CounterType.FINALITY`, all exist.)
    → **Meathook Massacre II**.

17. **Linked exile populated by a *replacement* effect.** `RedirectZoneChange` can send "a card you
    didn't control that would hit an opponent's graveyard" to exile (Anafenza precedent with an
    `OwnedByOpponent` filter) — but it does **not** record those cards in the source's
    `LinkedExileComponent`, which is what `GrantMayCastFromLinkedExile(duringYourTurnOnly=true)` reads.
    The explicit exile effects (`ExileLinkedToSource`, `ExileUntilLeaves`) link; the replacement path
    does not. Needs a redirect-and-link variant (or a linking hook on the redirect).
    → **Valgavoth, Terror Eater** ("play cards exiled with Valgavoth").

18. **Pay-life alt-cost equal to the spell's mana value.** `SelfAlternativeCost` + `AdditionalCost.PayLife`
    support "pay life instead of mana," but only a hard-coded amount; needs the life amount to read a
    `DynamicAmount` = the spell's mana value.
    → **Valgavoth, Terror Eater** ("pay life equal to its mana value rather than pay its mana cost").

19. **`Ward—Sacrifice N` count parameter.** `WardCost.Sacrifice(filter)` exists (Ygra) but takes no
    count; add a `count` param (or compose three via `WardCost.Composite`).
    → **Valgavoth, Terror Eater** ("Ward—Sacrifice three nonland permanents").

---

## Small / content-tier items (not subsystem gaps)

- **"Everywhere" / all-basic-land-type land token.** ✅ Done (content, not SDK): added the predefined
  `PredefinedTokens.Everywhere` `CardDefinition` — a colorless Land with all five basic land subtypes
  (Plains/Island/Swamp/Mountain/Forest) and a single tap-for-any-color mana ability (functionally the
  mana ability of each basic land type; no basic supertype, per the Scryfall ruling) — plus the
  `Effects.CreateEverywhere(count?, tapped?, controller?)` facade over `CreatePredefinedTokenEffect`.
  → **Overlord of the Hauntwoods** (the "Everywhere" token) implemented. Still open: **Overgrown Zealot**.
- **`ManaRestriction` "spend only to turn permanents face up."** Same shape as the existing
  `InstantOrSorceryOnly` / creature-spell-only restrictions — a one-member addition to the sealed
  interface + a solver branch, not a new subsystem.
  → **Overgrown Zealot**.

## Buildable-but-complex — flag for careful authoring (not gaps)

- **Manifest-then-attach equipment** — "manifest dread, then attach this Equipment to that creature."
  RESOLVED for the base shape: `Patterns.Library.manifestDread()` publishes the chosen face-down creature
  under the `"manifestDreadManifested"` pipeline collection, so a follow-up
  `Effects.AttachEquipment(EffectTarget.PipelineTarget("manifestDreadManifested"))` attaches to it within
  the same resolution. Implemented on **Conductive Machete** (and rendered AUTO by mtgish via the
  `AttachPermanentToPermanent` recipient = `ThePermanentPutOnTheBattlefieldThisWay`).
  → still to author: **Cursed Windbreaker, Dissection Tools, Chainsaw, Killer's Mask** (same pattern).
  (Chainsaw's "rev" counter is cosmetic — reuse an existing counter type feeding `ModifyStats`.)
- **Manifest dread directed at another player's library** — "its controller manifests dread" must
  target a *non-controller* player's library, with the face-down creature controlled by that player.
  RESOLVED: wrap the shared `Patterns.Library.manifestDread()` steps in
  `Effects.ForEachPlayer(Player.ControllerOf("target spell"), …)` — the per-player iteration rebinds
  the body's controller (and the manifested creature's control) to that player. Needed a fix so
  `ForEachExecutor.resolvePlayers` resolves single-player refs like `ControllerOf` instead of
  falling back to all active players.
  → **Fear of Impostors** (DONE), **Unwanted Remake** (same shape).
- **The Mindskinner / Marina Vendrell's Grimoire** — deep but pure compositions (prevent-damage→mill;
  no-max-hand + gain/lose-life draw/discard engine). NB: *Marina Vendrell's Grimoire* (artifact) is a
  pure composition; **Marina Vendrell** (the legendary creature) is **not** — its door lock/unlock
  ability is a Rooms-model gap (see §1b). **Valgavoth, Terror Eater** is **not** a pure composition
  either — its play-from-exile relies on a replacement-populated linked-exile pile (#16) plus a
  dynamic pay-life-=-MV alt-cost (#18) and a `Ward—Sacrifice N` count (#19).

---

## Recommended build order

1. **Rooms door-state exposure (Tier 1)** — one cohesive feature (unlocked-door filter + condition +
   dynamic amount, distinct-door-name count, unlock-cost reduction) unlocks Rampaging Soulrager,
   Smoky Lounge // Misty Salon, Central Elevator // Promising Stairs, and Inquisitive Glimmer.
2. **The buildable bulk** — work the ~155 buildable cards through the `add-card` skill: the impending
   Overlord cycle, the Glimmer "Enduring" cycle, the Nightmare "Fear of …" cycle, the remaining Rooms,
   manifest-dread creatures, Eerie/Delirium/Survival payoffs, the life-13 dual lands, and the five basics.
3. **Tier 2 player-scoped effects** — enchant-player (Grievous Wound), durable life-gain lock
   (Screaming Nemesis), dynamic max-hand-size (Winter).
4. **Tier 3 one-offs** as the relevant legendaries/rares come up (Marvin, Leyline of Transformation,
   Doomsday Excruciator, Rip, Toby, Zimone, Trial of Agony).
5. **Marquee mythics** — order them cheapest-first to unblock authoring: Zimone (#12) and Kaito (#15)
   are XS/S condition additions; Meathook Massacre II (#16) and Valgavoth (#17–19) are one real gap
   each plus small companions; **Marina Vendrell (§1b)** is the only `add-feature`-scale item (Rooms
   door-state model) and should be designed deliberately.

The five headline mechanics already being done means the structural risk for DSK is low: most of the
set is authoring work, and the gaps above are narrow, mostly-isolated additions. Among the marquee
mythics, only Marina Vendrell reshapes a subsystem; the other four are small, isolated SDK additions.
