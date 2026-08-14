# Marvel's Spider-Man (SPM) — Mechanics

**Set complete — 188 / 188 booster cards implemented.** This file is the archived record of the
engine/SDK gaps SPM originally surfaced: each section began as an `add-feature` blocker (a new SDK
primitive, keyword, or engine capability — not pure card authoring) and every one has since been
built. All sections below are marked ✅ IMPLEMENTED, and every card once listed as blocked is now
authored with a passing scenario test.

Supported today (confirmed against the `Keyword` enum + SDK): connive, saga, convoke, kicker,
vehicles/crew, surveil, fight, copy-spell, copy-ability, play-from-top-of-library, impulse
exile-and-play, max-hand-size modification, transform DFCs, Food/Treasure, **web-slinging**
(CR 702.188), **mayhem** (CR 702.187), **riot**.

---

## Web-slinging (new alternative cost + additional return-a-tapped-creature cost) — ✅ IMPLEMENTED

> Web-slinging {cost} *(You may cast this spell for {cost} if you also return a tapped
> creature you control to its owner's hand.)*

**Implemented** (CR 702.188). A new keyword modeled as a hand-timed **alternative cost** bundling a
non-mana portion (return a tapped creature you control to its owner's hand). SDK: `Keyword.WEB_SLINGING`,
`KeywordAbility.WebSlinging`, the `webSlinging("{cost}")` DSL helper, the durable
`ChoiceSlot.WEB_SLUNG` flag (read via `Conditions.WebSlungCostWasPaid`) and
`ChoiceSlot.WEB_SLUNG_RETURNED_MV` (the returned creature's mana value, read via
`DynamicAmount.CastChoice`). Engine: `AlternativeCostType.WEB_SLINGING`, `WebSlingingCastEnumerator`,
`WebSlinging` mechanics helper, and the `CastSpellHandler` / `StackResolver` / `ConditionEvaluator`
wiring. Unlike Sneak/Ninjutsu it grants no timing permission (normal timing).

Implemented cards (8): **Spider-Man, Web-Slinger** [16] · **Spider-UK** [17] · **Spider-Man,
Brooklyn Visionary** [115] · **Spiders-Man, Heroic Horde** [117] · **Scarlet Spider, Ben Reilly**
[142] · **Silk, Web Weaver** [145] · **Spider-Man India** [151] · **Spider-Sense** [46]. (Spider-UK's
end-step clause drove a small reusable addition — the `CREATURES_ENTERED_UNDER_CONTROL` turn tracker
+ `Conditions.CreaturesEnteredThisTurn`; Spider-Sense drove `Targets.InstantSorceryOrTriggeredAbility`.)

Related cards (all now implemented):
- **Arachne, Psionic Weaver** [2] — ✅ DONE (branch `spm-arachne`). `{2}{W}`, Web-slinging `{W}` plus the
  ETB "look at an opponent's hand, then choose a card type other than creature; spells of the chosen type
  cost {1} more to cast" — modeled by the **durable card-type enters-choice + a chosen-card-type spell-tax
  static** (see the next section).
- **Peter Parker // Amazing Spider-Man** [10] — ✅ DONE (branch `spm-peter-parker`). Transform DFC (front:
  ETB 2/1 green Spider token + sorcery-speed transform, both pre-existing). Back's "each legendary spell
  you cast that's one or more colors has web-slinging {G}{W}{U}" is the new `GrantWebSlingingToSpells(cost,
  spellFilter)` static (parallel to `GraveyardCardsHaveMayhem`): `WebSlinging.effectiveWebSlinging` now also
  scans the battlefield for it (printed → group grant), threaded through `WebSlingingCastEnumerator` +
  `CastSpellHandler` (validate/cost×2/rider). Filter = `IsLegendary` + `IsColored`. Scenario test pins the
  grant (legendary+colored → offered; nonlegendary → not; no granter → not).

## "Choose a card type" durable enters-choice + "spells of the chosen type cost {1} more" static — ✅ IMPLEMENTED

**Done** (branch `spm-arachne`). New durable card-type choice dimension: `ChoiceSlot.CARD_TYPE`,
`Effects.ChooseCardTypeForSource(allowedCardTypes, lookAtOpponentHand)` (on-resolution, writes the slot
durably — the analogue of `ChooseNumberForSource`, run from a "when ~ enters" trigger), and
`CardPredicate.CardTypeEqualsChosenComponent` / `GameObjectFilter.ofChosenCardTypeComponent()` read at
cost-calculation time. The tax is a plain `ModifySpellCost(AnyCaster(ofChosenCardTypeComponent()),
IncreaseGeneric(1))` (Thalia shape). Scenario test pins the durable write + the symmetric tax
(chosen-type spell costs {1} more, other types untaxed).

<details><summary>Original analysis (kept for reference)</summary>

> As Arachne enters, look at an opponent's hand, then **choose a card type** other than creature.
> **Spells of the chosen type cost {1} more to cast.**

No durable **card-type** enters-choice exists (`ChoiceSlot` has COLOR / CREATURE_TYPE / LAND_TYPE /
CARD_NAME / MODE, but not a general card type), and there is no static that reads such a choice to
**increase the cost of spells of the chosen card type** (the nearest neighbor,
`GrantProtectionFromChosenCardType`, chooses a card type transiently for protection, not a durable
stored choice feeding a cost-increase). "Look at an opponent's hand" is informational. Fix
(add-feature): a `ChoiceSlot.CARD_TYPE` written by an `EntersWithChoice(ChoiceType.CARD_TYPE)` resumer
+ a `SpellsOfChosenCardTypeCostMore(amount)` static reading it in the cost pipeline.

Blocked cards:
- **Arachne, Psionic Weaver** [2] — web-slinging implemented; the choose-a-card-type tax is the blocker.

</details>

## Harness / Infinity Stone ∞ ability — ✅ IMPLEMENTED

**Done** (branch `spm-soul-stone`). Modeled composition-first: "Harness" is a binary marker counter
(`Counters.HARNESS`, a new `CounterType` enum value — the only new vocabulary). The Harness activated
ability places one via `Effects.AddCounters`; the `∞` triggered ability is gated on the permanent
having a harness counter (`Conditions.SourceHasCounter(CounterTypeFilter.Named(Counters.HARNESS))`),
so it's dormant until harnessed and reactivates each qualifying trigger thereafter. A counter (not a
durable component) matches the flavor — it resets if the permanent leaves, and re-placing is
idempotent. No new engine executor/handler needed. Scenario test pins the gating (reanimates only
while harnessed).

<details><summary>Original analysis (kept for reference)</summary>

> `{cost}, {T}, Exile a creature you control: Harness <this>. (Once harnessed, its ∞ ability
> is active.)` — ∞ — <ongoing ability>

A new designation: an activated "harness" cost that permanently switches on a dormant `∞`
ability. No `Harness` / `InfinityStone` primitive exists (a permanent-state flag that gates a
static/triggered ability). `add-feature` territory.

Blocked cards:
- **The Soul Stone** [66] — `{1}{B}` Legendary Artifact — Infinity Stone; harness → `∞` upkeep reanimation

</details>

## Mayhem (new keyword — self graveyard-cast gated on "you discarded this card this turn") — ✅ IMPLEMENTED

> Mayhem {cost} *(You may cast this card from your graveyard for {cost} if you discarded it
> this turn. Timing rules still apply.)*

**Implemented** (CR 702.187) on branch `spm-mayhem`. Two-part `add-feature`:
(1) turn-scoped discarded-this-turn tracking — `CardsDiscardedThisTurnComponent(cardIds)` written at every
discard site via `ZoneTransitionService.trackDiscard`, reset per-turn in `TurnManager`, exposed as
`TurnTracker.CARDS_DISCARDED` / `DynamicAmounts.cardsDiscardedThisTurn()` (count) and
`Conditions.YouDiscardedThisCardThisTurn` (per-card membership gate).
(2) `Keyword.MAYHEM` / `KeywordAbility.Mayhem(cost)` / `mayhem("{cost}")` DSL / `AlternativeCostType.MAYHEM`,
resolved via `MayhemGrants.effectiveMayhem`, enumerated by `CastFromZoneEnumerator.enumerateMayhem` and gated in
`CastSpellHandler` by `CastZoneResolver.hasMayhemPermission`. Grants no timing permission; **not** exiled on
resolution (permanent just enters — the deliberate omission of any Mayhem branch in `StackResolver`'s exile
clause). "Mayhem cost was paid" is a durable `ChoiceSlot.MAYHEM_CAST` / resolution-context flag read via
`Conditions.MayhemCostWasPaid`.

Implemented cards (9): **Swarm, Being of Bees** [69] · **Spider-Islanders** [91] · **Raging Goblinoids** [85] ·
**Electro's Bolt** [77] · **Prison Break** [61] · **Sandman's Quicksand** [63] (MayhemCostWasPaid rider) ·
**Scarlet Spider, Kaine** [143] · **Chameleon, Master of Disguise** [27] (enter-as-copy) ·
**Rocket-Powered Goblin Glider** [172] (ETB attach gated on `WasCastFromGraveyard`).

Related cards (all now implemented):
- **Carnage, Crimson Chaos** [125] — ✅ **IMPLEMENTED** on branch `spm-grant-reanimate`. The persistent
  grant-abilities-to-a-reanimated-target was expressible after all: `GrantStaticAbilityEffect(MustAttack())`
  + `GrantTriggeredAbilityEffect(TriggeredAbility.create(DealsCombatDamageToPlayer → SacrificeSelfEffect))`,
  both `Duration.Permanent`, keyed to the reanimated `Effects.Move(...fromZone = GRAVEYARD)` target.
- **Oscorp Industries** [182] — ✅ **IMPLEMENTED** on branch `spm-land-plays`. The no-cost 702.187c form is a
  land-play from graveyard: `PlayLandEnumerator` now offers a discarded-this-turn Mayhem land as a `PlayLand`
  action and `PlayLandHandler` allows it (both gated on `MayhemGrants.effectiveMayhem`, via `mayhem("")`). Its
  "enters from a graveyard → lose 2 life" uses the `EnteredFromGraveyardComponent` the handler now stamps on
  graveyard land-plays (lands bypass `ZoneTransitionService`).
- **Ultimate Green Goblin** [157] — ✅ **IMPLEMENTED**. `{1}{B/R}{B/R}` Mayhem `{2}{B/R}`; the upkeep
  "discard a card, then create a Treasure" over the existing Mayhem primitive.
- **Norman Osborn // Green Goblin** [39] — ✅ DONE (branch `spm-norman-osborn`). Transform DFC (front: unblockable +
  connive-on-combat-damage + sorcery-speed transform; all pre-existing). Back's "Goblin Formula" is the new
  `GraveyardCardsHaveMayhem(filter, cost?)` static (mirrors `GraveyardCardsHaveFlashback`): `MayhemGrants.effectiveMayhem`
  now also scans the battlefield for it (group grant → per-entity → printed), threaded through the four cast read sites,
  still gated on discarded-this-turn. Back's gy-cast `{2}` reduction is a plain `ModifySpellCost(YouCastFromZones(GRAVEYARD))`.
  Scenario test pins the group-granted mayhem (grant on, grant off, discard-gate).

Also enabled by the **discarded-this-turn tracking** half:
- **Green Goblin, Revenant** [130] — ✅ **IMPLEMENTED**. `{3}{B}{R}` Flying/deathtouch; "Whenever Green
  Goblin attacks, discard a card. Then **draw a card for each card you've discarded this turn**" — via
  `DynamicAmounts.cardsDiscardedThisTurn()`.

## Riot (keyword — enters with your choice of a +1/+1 counter or haste) — ✅ IMPLEMENTED

**Done** (branch `spm-spider-punk`). New `Keyword.RIOT` + a `CardBuilder.riot()` DSL helper modeling
printed Riot via the Khans-Siege `EntersWithChoice(ChoiceType.MODE, [counter, haste])` pattern +
mode-gated `EntersWithCounters(condition = SourceChosenModeIs("counter"))` + mode-gated
`ConditionalStaticAbility(GrantKeyword(HASTE), SourceChosenModeIs("haste"))`. **Granted riot** ("Other
Spiders you control have riot") is handled by a new `RiotSynthesis` helper: for a Spider cast as a
spell it scans battlefield `GrantKeyword(RIOT)` lords (excludeSelf, deduped vs printed) and ORs a
synthesized `EntersWithChoice` into the entry; for token/land entries it detects granted RIOT via
projected `hasKeyword`; the resumers apply the chosen +1/+1-counter or Duration.Permanent-floating-haste
branch directly (a granted creature has no printed statics). Wired into StackResolver /
PermanentEntryReplacements / TokenFromDefinition / PlayLandHandler + both continuation resumers.
"Spells and abilities can't be countered" = `GrantCantBeCountered(Any, includesAbilities = true)` (the
new `includesAbilities` flag fizzles Stifle-type ability counters). "Damage can't be prevented" =
`DamageCantBePrevented`. Scenario tests cover printed Riot (counter/haste), granted riot (a cast Spider
gets the choice), and can't-be-countered. Full regression green. (Granted-riot synthesis for
blink/reanimation entry paths is not wired — a rare edge, flagged.)

<details><summary>Original analysis (kept for reference)</summary>

> Riot *(This creature enters with your choice of a +1/+1 counter or haste.)*

Not in the `Keyword` enum and no ETB "choose counter or haste" primitive that also grants a
**projectable** Riot keyword. Spider-Punk further needs to **grant riot to other Spiders**,
which requires Riot to exist as a grantable keyword. `add-feature` scope.

Blocked cards:
- **Spider-Punk** [92] — `{1}{R}` Riot; "Other Spiders you control have riot"; also "Spells and abilities can't be countered" + "Damage can't be prevented" (verify those two independently)

</details>

## "Modified" state on a leaves-the-battlefield (last-known-information) trigger — ✅ IMPLEMENTED

**Done** (branch `spm-modified-ltb`). `EntitySnapshot` now captures `wasEquipped` / `wasEnchanted`
(populated in `ZoneTransitionService` before the exit cleanup strips attachments), and
`TriggerMatcher.matchesStatePredicateForZoneChangeTrigger` evaluates `IsModified` / `IsEquipped` /
`IsEnchanted` against the snapshot (counters + those flags) on a leaves-battlefield trigger instead
of falling through fail-open. Costume Closet ships with a scenario test covering the counter,
equipped, and unmodified (regression-guard) legs.

<details><summary>Original analysis (kept for reference)</summary>

> Whenever a **modified** creature you control **leaves the battlefield**, …

The `IsModified` state-predicate (CR 700.4 — has an Equipment/controlled Aura attached, or a
counter) works fine as a *static/targeting* filter, but it is **not gated** on a zone-change
(leaves/dies) trigger. In `rules-engine/.../event/TriggerMatcher.kt`,
`matchesStatePredicateForZoneChangeTrigger` has last-known-information cases for `HasCounter`,
`HasAnyCounter`, `HasGreatestPower`, etc., but `IsModified` (and siblings `IsEquipped` /
`IsSaddled`) fall through to the "don't gate — return true" path, so the trigger fires for
**every** creature that leaves, not just modified ones. The counter half is recoverable from
`EntitySnapshot` (it captures `counters`), but the **Equipment/Aura-attached** half is not —
`EntitySnapshot` records a permanent's own `attachedTo`, not what was attached *to* the leaving
creature, so faithful "modified" on exit needs the snapshot extended to capture attachments at
battlefield-exit (or the predicate evaluated against pre-leave state). `add-feature` scope.

Blocked cards:
- **Costume Closet** [5] — `{1}{W}` Artifact; enters with two +1/+1 counters + sorcery-speed "{T}: move a counter to target creature you control" (both of those work today) + "Whenever a **modified** creature you control leaves the battlefield, put a +1/+1 counter on this artifact" (the blocked part)

</details>

## "Deals damage to a [filtered] creature" trigger (RecipientFilter.Matching on a deals-damage trigger) — ✅ IMPLEMENTED

**Implemented** on branch `spm-damage-triggers`. Added the missing `is RecipientFilter.Matching` case to
`TriggerMatcher.matchesDealsDamageTrigger` (evaluate the filter against the recipient in projected state,
mirroring `DamageCalculator`). The triggering entity is already the recipient (`TriggerContext.fromEvent`
sets `triggeringEntityId = event.targetId`), so `Effects.Destroy(EffectTarget.TriggeringEntity)` destroys
the damaged creature. Also repairs the two already-shipped cards with the identical shape (**East-Mark
Cavalier** LTR, **Mauhur, Uruk-hai Captain**). Card: **Spider-Slayer, Hatred Honed** [175].

<details><summary>Original analysis</summary>


> Whenever <this> deals damage to a **Spider**, destroy that creature.

A deals-damage trigger whose **recipient** is filtered to a creature matching a predicate
(`Triggers.dealsDamage(recipient = RecipientFilter.Matching(...))`) does not fire. In
`rules-engine/.../event/TriggerMatcher.kt`, `matchesDealsDamageTrigger` (the SELF-binding
deals-damage detection path via `DamageTriggerDetector`) has **no `is RecipientFilter.Matching`
case** — it falls through to `else -> false`, so the trigger never matches.
`RecipientFilter.Matching` is only wired for damage prevention/replacement/effect-targeting
(DamageCalculator, ReplacementEffectUtils, DamageUtils), never for trigger detection. This is a
pre-existing latent bug: the already-shipped **East-Mark Cavalier** (LTR) and **Mauhur,
Uruk-hai Captain** use the identical shape and are also silently broken. Fix (add-feature): add
`is RecipientFilter.Matching -> predicateEvaluator.matches(...)` (with LKI fallback like the
existing `CreatureYouControl` case) to `matchesDealsDamageTrigger`.

Blocked cards:
- **Spider-Slayer, Hatred Honed** [175] — `{2}` Legendary Artifact Creature; "Whenever Spider-Slayer deals damage to a Spider, destroy that creature" (blocked). Its other ability — `{6}`, exile-from-graveyard → two tapped 1/1 flying Robot tokens — works fine.
</details>

## Chosen card name surviving into a later-firing delayed trigger — ✅ IMPLEMENTED

**Done** (branch `spm-clone-saga`). `CreateDelayedTriggerExecutor.bakeChosenValuesIntoTrigger` now
also rewrites `NameEqualsChosen(v)` → literal `NameEquals(chosen[v])` and handles a
`DealsDamageEvent.sourceFilter` (not just `SpellCastEvent.spellFilter`), via a shared
`bakeChosenValuesIntoFilter` helper. So chapter III models as `Composite(ChooseCardName("clonedName"),
CreateDelayedTrigger(dealsDamage(Combat, AnyPlayer, Creature.namedFromVariable("clonedName")),
DrawCards(1)))`. Chapters I (Surveil 3) and II (`CreateDelayedTrigger(YouCastCreature,
CopyTargetSpell(TriggeringEntity, removeLegendary=true), fireOnce=true)`) needed no engine change.
Scenario test pins the chapter-III chosen-name combat-damage draw.

<details><summary>Original analysis (kept for reference)</summary>

> Choose a card name. Whenever a creature with the **chosen name** deals combat damage to a
> player this turn, draw a card.

`Effects.ChooseCardName` stores the name only in the resolving pipeline's `chosenValues`
(scoped to that one resolution). A `CardPredicate.NameEqualsChosen` in a **delayed** trigger's
event filter evaluates against `context.chosenValues`, which the later-firing delayed trigger
does not carry, so it fails closed and the trigger never fires. The only baker of chosen values
into delayed-trigger filters, `CreateDelayedTriggerExecutor.bakeChosenValuesIntoTrigger`,
handles only `HasSubtypeFromVariable` inside a `SpellCastEvent.spellFilter` — its own TODO
comment flags `NameEqualsChosen` as unhandled. Fix (add-feature): extend that baker to snapshot
`chosenValues` and rewrite `NameEqualsChosen` → literal `NameEquals(<name>)` inside delayed
event filters (e.g. `DealsDamageEvent.sourceFilter`), + verify the delayed matcher reads it.

Blocked cards:
- **The Clone Saga** [28] — `{3}{U}` Enchantment — Saga; chapters I (Surveil 3) and II (copy your next creature spell, non-legendary — both expressible today) are fine, but chapter III ("choose a card name … whenever a creature with the chosen name deals combat damage, draw") is blocked

</details>

## Exchange life totals with a player (CR 701.12c) + "life you lost this way" draw amount — ✅ IMPLEMENTED

**Implemented** on branch `spm-life-exchange`. Added `ExchangeLifeTotalsEffect(target, drawEqualToLifeLost)`
+ `ExchangeLifeTotalsExecutor` — reads both totals before any change (simultaneous swap, CR 701.12c),
emits gain/loss `LifeChangedEvent`s for both players (for lifelink/triggers), marks life gained/lost, and —
because the draw amount is the controller's life-loss delta that no `DynamicAmount` exposes — draws that
many cards itself (via `DrawCardPrimitive`, `cardRegistry` threaded through `LifeExecutors`). Card:
**Mister Negative** [135] (`MayEffect(Effects.ExchangeLifeTotals(drawEqualToLifeLost = true))`).

<details><summary>Original analysis</summary>

> You may **exchange life totals** with target opponent. If you lost life this way, draw that
> many cards.

No player-vs-player life-total exchange exists. `LifeEffects` has only
`ExchangeLifeAndPowerEffect` (a player's life ↔ a *creature's power*, CR 701.12g) and
`ExchangeControlEffect` (control of permanents). Two sequential `SetLifeTotal`s can't reproduce
a *simultaneous* swap (the second read sees the already-mutated value), and there is no
life-total snapshot primitive. Separately, "draw that many cards" = the controller's life-loss
**delta from the exchange**, which no effect exposes as a `DynamicAmount`. Fix (add-feature): an
`ExchangeLifeTotalsEffect(target player)` executor honoring 701.12c (emitting gain/loss events
for lifelink/triggers) + a way to feed the controller's life-lost delta into `DrawCards`.

Blocked cards:
- **Mister Negative** [135] — `{5}{W}{B}` Vigilance/lifelink; "you may exchange life totals with target opponent. If you lost life this way, draw that many cards." (Vigilance + lifelink are fine; the ETB exchange is blocked.)
</details>

## "Different names" multi-target distinctness constraint — ✅ IMPLEMENTED

**Implemented** on branch `spm-target-constraints`. Added a `differentNames: Boolean` field to
`TargetObject`, enforced cross-target by `TargetValidator` (authoritative) and `DecisionValidators`
(interactive, via a new `TargetRequirementInfo.differentNames` propagated at the target-decision build
sites) — grouping chosen targets by projected name (battlefield) / base card name (other zones). Card:
**Behold the Sinister Six!** [51] (`TargetObject(count = 6, optional = true, differentNames = true)` +
`ForEachTargetEffect(PutOntoBattlefield)`).

<details><summary>Original analysis</summary>

> Return up to six **target creature cards with different names** from your graveyard to the
> battlefield.

Cross-target selection constraints on `TargetObject` (enforced in `TargetValidator` /
`DecisionValidators`) currently cover only `sameController`, `sameOwner`, `sameCreatureType`,
and `totalManaValueAtMost` — plus object-identity distinctness (`TargetOther`, which prevents
picking the same entity twice, NOT the same *name*). There is no name-based distinctness gate,
so "with different names" can't be enforced. Fix (add-feature): a `differentNames` cross-target
requirement grouping chosen targets by projected card name, wired into both validators (+ SDK
reference + `CardLinter`).

Blocked cards:
- **Behold the Sinister Six!** [51] — `{6}{B}` Sorcery; "Return up to six target creature cards with different names from your graveyard to the battlefield." Dropping the constraint would wrongly allow six copies of the same-named creature, so it is not approximated.
</details>

## Color-filtered permanent "don't lose unspent [color] mana" static — ✅ IMPLEMENTED

**Implemented** on branch `spm-costs-mana`. Added `RetainUnspentColoredMana(color)` StaticAbility (scan-based,
controller-scoped, single-colour, durable) merged into the `retain` colour set in
`CleanupPhaseManager.emptyManaPools` (control-aware, via a `retainedColorsFromStatics` helper). `endCombat`
needs no change — it only touches firebending `END_OF_COMBAT` mana, not ordinary red. Card: **Electro,
Assaulting Battery** [76] (the cast-instant/sorcery→add-{R} and LTB `MayPayXForEffect` deal-X clauses use
existing primitives).

<details><summary>Original analysis</summary>

> You don't lose unspent **red** mana as steps and phases end.

Needs a controller-scoped, single-color, **permanent** mana-retention static. The three existing
neighbors don't fit: `PreventManaPoolEmptying` (Upwelling) keeps *all* colors for *all* players;
`ConvertEmptyingManaToRed` (Ozai) *converts* other colors to red instead of losing them (so a
floating `{G}` wrongly becomes `{R}`); `RetainUnspentManaEffect` (The Last Agni Kai) is red-only
controller-only but a **one-shot turn-scoped** effect (cleared each cleanup), not a static tied
to a permanent's presence. Fix (add-feature): a color-parameterized `RetainUnspentColoredMana`
`StaticAbility` wired into `CleanupPhaseManager.emptyManaPools` + `CombatManager.endCombat` +
`StaticAbilityHandler`.

Blocked cards:
- **Electro, Assaulting Battery** [76] — `{1}{R}{R}` Flying; "You don't lose unspent red mana as steps and phases end." Its other clauses (Flying; cast-instant/sorcery → add {R}; LTB pay-{X} deal X to a player) are all expressible today.
</details>

## "Discard a card OR pay {2}" additional cost (DiscardOrPay) — ✅ IMPLEMENTED

> As an additional cost to cast this spell, **discard a card or pay {2}**.

**Implemented** on branch `spm-goblins`. A choice between a non-mana cost (discard a card) and a **mana**
payment as an additional cost. Added `AdditionalCost.DiscardOrPay(alternativeManaCost, filter, count)` +
`Costs.additional.DiscardOrPay(...)`, mirroring the existing `*OrPay` family (`SacrificeOrPay` /
`ExileFromGraveyardOrPay` / `BlightOrPay` / `BeholdOrPay`). Wired into `CastSpellEnumerator` (two cast
paths — discard path with a `costType = "DiscardCard"` hand picker, and pay path folding in the alt mana),
`CostHandler` (always payable — pay path), and `CastSpellHandler` (validation mana adjustment, payment
validation, mana application, and the discard-payment application mirroring `CostAtom.Discard`, including
`ZoneTransitionService.trackDiscard` so it feeds the turn's discard tracking / Mayhem). Path recovered at
payment time from whether `AdditionalCostPayment.discardedCards` is non-empty.

Implemented cards (1): **Pumpkin Bombardment** [139] — `{B/R}` Sorcery; "discard a card or pay {2}. Deals 3
damage to target creature."

## "Play a land from anywhere other than your hand" trigger — ✅ IMPLEMENTED

**Implemented** on branch `spm-land-plays`. Added an additive `LandPlayedEvent` (engine) emitted by
`PlayLandHandler` alongside the entry `ZoneChangeEvent` (carrying `fromZone`) — distinct from an effect
*putting* a land onto the battlefield, so it doesn't over-trigger on fetch/reanimate/ramp. Wired through
`TriggerIndex` (new `LAND_PLAYED` category, both directions) + `TriggerMatcher` + a
`EventPattern.LandPlayedEvent(fromZoneOtherThan)` / `Triggers.youPlayLand(fromZoneOtherThan = Zone.HAND)`
primitive. The **turn-scoped** form: a `PlayedLandFromNonHandThisTurnComponent` flag set by the handler
(reset per-turn in `TurnManager`) backing `Conditions.YouPlayedLandFromNonHandThisTurn`, plus a
`fromZoneOtherThan` qualifier added to `Conditions.YouCastSpellsThisTurn` (the cast half). Cards: **Shadow
of the Goblin** [87] (two triggered abilities — land-play + cast-from-non-hand) and **Spider-Man 2099**
[150] (`any(YouPlayedLandFromNonHandThisTurn, YouCastSpellsThisTurn(1, fromZoneOtherThan = HAND))`).

<details><summary>Original analysis</summary>

> Whenever you **play a land** or cast a spell from anywhere other than your hand, …

The cast half ("cast a spell from a non-hand zone") is expressible (`SpellCastPredicate.CastFromZoneOtherThan`,
as Kellan the Kid uses). The **land-play** half is not: `PlayLandHandler` emits a plain
`ZoneChangeEvent(→ BATTLEFIELD)` with no "was played / special action" marker, indistinguishable
from a land an *effect* puts onto the battlefield (fetch, reanimate, ramp) — a `ZoneChangeEvent`
land trigger from a non-hand zone would over-trigger on every such put. (The Endstone dodges this
only by restricting to `from = HAND`, which Shadow can't use since it needs the non-hand zones;
`EventPattern.ZoneChangeEvent` also has no `excludeFrom` shape.) Fix (add-feature): a
`LandPlayedEvent` / `wasPlayed` marker from `PlayLandHandler` + a `Triggers.youPlayLand(fromZoneOtherThan=…)`
primitive mirroring the spell-cast one.

Blocked cards:
- **Shadow of the Goblin** [87] — `{1}{R}` Enchantment; first-main loot (fine) + "Whenever you play a land or cast a spell from anywhere other than your hand, deals 1 to each opponent" (the land-play-from-non-hand half is the blocker)

The **turn-scoped historical-condition** form of the same gap is also missing (distinct from the
trigger above): there is no `PlayerPlayedLandThisTurn(fromZone=…)` condition (land plays are
tracked only as a count via `LandDropsComponent` — no source-zone provenance, unlike
`CastSpellRecord.castFromZone` for spells), and no "cast a spell from **any zone other than
hand** this turn" condition (`YouCastSpellsThisTurn` is single-zone positive equality only; the
"other-than-hand" concept exists only at event/trigger level as `EventFilters.CastFromZoneOtherThan`).
Fix (add-feature): a land-play zone-of-origin turn record + an "other-than" zone qualifier on both
the land and spell turn-conditions.
- **Spider-Man 2099** [150] — `{U}{R}` double strike/vigilance; the "From the Future" turn-number cast restriction (`ControllerTurnsTakenAtMost`) and "deal power to any target" are fine, but the end-step intervening-if "if you've played a land or cast a spell this turn from anywhere other than your hand" is the blocker.
</details>

## Temporary "play from top of library, paying life = mana value instead of mana cost" — ✅ IMPLEMENTED

**Done** (branch `spm-gwenom`). New `PlayFromTopWithAlternativeCost(withoutPayingManaCost, additionalCost,
filter)` static (the top-of-library counterpart to `GrantMayCastFromLinkedExile`). `CastPermissionUtils`
gained `playFromTopAlternativeCost(...)` which scans BOTH printed `staticAbilities` and
`state.grantedStaticAbilities` (mirroring `MayCastFromGraveyard`), and every top-of-library read site
(`CastPermissionUtils`, `CastZoneResolver` incl. `hasPlayWithoutPayingCost`, the enumerator cost branch,
`CastSpellHandler` validate+execute) now honors it: mana is waived and `PayLifeEqualToManaValueOfSpell`
is charged. Gwenom's attack trigger grants it (+ `LookAtTopOfLibrary`) to Self until end of turn.
Scenario test pins it (not castable before the attack; after, casts for life = mana value, no mana).
(The durationally-granted look's client-side top-card reveal remains printed-scan only — a UI/visibility
gap with no rules impact.)

<details><summary>Original analysis (kept for reference)</summary>

> Whenever Gwenom attacks, until end of turn, you may look at the top card of your library any
> time and you may play cards from the top. If you cast a spell this way, **pay life equal to its
> mana value rather than pay its mana cost**.

Two independent gaps: (1) The "pay life = mana value instead of mana cost" hook
(`AdditionalCost.PayLifeEqualToManaValueOfSpell`) is wired **only** on the linked-exile play
permission (Valgavoth), not on the top-of-library statics (`PlayFromTopOfLibrary` etc. carry no
`additionalCost`/`withoutPayingManaCost`; `CastFromZoneEnumerator.enumerateTopOfLibrary` +
`CastSpellHandler` always compute the normal mana cost). (2) The ~5 play-from-top / look-at-top
read helpers scan only *printed* `staticAbilities`, never `state.grantedStaticAbilities`, so an
until-end-of-turn floating grant of the permission does nothing (unlike the graveyard-play path,
which already reads the floating channel). Fix (add-feature): add an alternative-cost hook to a
top-of-library permission + wire the enumerator/handler to it, and extend the top-of-library read
sites to consult granted statics (mirroring `MayCastFromGraveyard`).

Blocked cards:
- **Gwenom, Remorseless** [56] — `{3}{B}{B}` Deathtouch/lifelink; the attack-granted "play from top, pay life = mana value" is the blocker (deathtouch, lifelink, and the attack trigger itself are fine).

</details>

## "Prevent damage to this creature, put that many +1/+1 counters on it" self-replacement — ✅ IMPLEMENTED

**Implemented** on branch `spm-damage-triggers`. Added `is RecipientFilter.Self -> targetId == entityId`
to `DamageUtils.applyReplaceDamageWithCounters`'s recipient matcher, and invoked it on the creature-damage
paths — the non-player (`else`) branch of `DamageUtils.dealDamageToTarget` and
`CombatDamageManager.applyDamageToCreature` (before redirection/final marking). Card: **Anti-Venom,
Horrifying Healer** [1] (its "if he was cast" ETB reanimation uses the existing `Conditions.WasCast`).

<details><summary>Original analysis</summary>

> If damage would be dealt to Anti-Venom, prevent that damage and put that many +1/+1 counters
> on him.

The SDK replacement `ReplaceDamageWithCounters` + `RecipientFilter.Self` exists, but the engine
executor (`DamageUtils.applyReplaceDamageWithCounters`) is wired **only** on the player-damage
path (its two call sites are both `isPlayer`-guarded / inside `applyDamageToPlayer`), and its
recipient matcher handles only `RecipientFilter.You`/`.Any` (no `Self` case). The creature-damage
path never invokes it, so damage to a creature is never replaced. The only shipped user is Force
Bubble (damage to *you*). Note: Strength of Will's indestructible + `TakesDamage`-trigger counters
is NOT a faithful substitute — that *marks* the damage (survives only via indestructible) rather
than preventing/replacing it. Fix (add-feature): accept `RecipientFilter.Self` (targetId == host)
and invoke `applyReplaceDamageWithCounters` on the creature-damage paths
(`CombatDamageManager.applyDamageToCreature` + the non-player branch of `DamageUtils.applyDamage`).
(The "if he was cast" ETB reanimation half is fine — `Conditions`/`WasCast` exists.)

Blocked cards:
- **Anti-Venom, Horrifying Healer** [1] — `{W}{W}{W}{W}{W}` Symbiote Hero; ETB "if cast, reanimate a creature" is fine, but the damage-prevention-to-counters self-replacement is the blocker.
</details>

## Granted activated ability with `UntilYourNextTurn` duration never expires — ✅ IMPLEMENTED

**Implemented** on branch `spm-costs-mana`. `CleanupPhaseManager.expireUntilYourNextTurnEffects` now also
drops `grantedActivatedAbilities` whose duration is `UntilYourNextTurn`, keyed to the granted entity's
current controller (correct for the self-grant case). The "becomes a non-creature land" half was *not* a
missing primitive after all — `BecomeArtifactEffect` is a general "becomes [cardTypes]" effect;
`BecomeArtifactEffect(cardTypes = setOf("LAND"), colors = null, loseAllAbilities = false, grantedAbility =
"{T}: Add {U}", duration = UntilYourNextTurn)` expresses Hydro-Man's transform, and its granted mana
ability expires via the fix above. Card: **Hydro-Man, Fluid Felon** [33].

<details><summary>Original analysis</summary>

> …until your next turn, he becomes a land and **gains "{T}: Add {U}."**

Granting an *activated* ability routes only through `GameState.grantedActivatedAbilities`, which
has no floating-effect/projection path and is pruned in only three places — none handling
`Duration.UntilYourNextTurn` (`CleanupPhaseManager` end-of-turn only;
`EndedDurationExpiryCheck` counter/tapped only; `ZoneTransitionService` on-leave only). Notably
`CleanupPhaseManager.expireUntilYourNextTurnEffects` prunes floating effects and
`globalGrantedTriggeredAbilities` for `UntilYourNextTurn` but **not** `grantedActivatedAbilities`
(nor the sibling granted-triggered/static/replacement/keyword lists). Symptom: after Hydro-Man
reverts to a creature next turn, the "{T}: Add {U}" grant persists — a permanent 2/2 that taps
for blue forever. The type-change and untap halves work; only the granted activated ability
leaks. Fix (add-feature): extend `expireUntilYourNextTurnEffects` to drop
`grantedActivatedAbilities` (and siblings) with `UntilYourNextTurn` duration, keyed to the
grant-holder's controller (the `GrantedActivatedAbility` record needs a controller/expires-for
field, like the player-component grants).

Blocked cards:
- **Hydro-Man, Fluid Felon** [33] — `{U}{U}`; blue-cast pump (fine) + end-step "untap; until your next turn becomes a non-creature land with '{T}: Add {U}'". *(Resolved — see the section header above.* Both halves shipped: `expireUntilYourNextTurnEffects` now drops `grantedActivatedAbilities` with `UntilYourNextTurn` duration, and the "becomes a non-creature land" half needed **no** new primitive after all — the mid-review worry that only additive `AddCardTypeEffect` existed was wrong: `BecomeArtifactEffect` sets `SetCardTypes(setOf("LAND"))`, a full type-*replacement* that drops the creature type. *)*
</details>

## Static damage redirect to the enchanted/equipped creature (Pariah-style) — ✅ IMPLEMENTED

**Implemented** on branch `spm-damage-triggers`. Extended `DamageUtils.resolveRedirectTarget` with
`EffectTarget.EnchantedCreature` / `EquippedCreature` / `EnchantedPermanent` → the Aura/Equipment's
`AttachedToComponent.targetId`. The "+2/+2 for each attached Aura/Equipment" buff uses the new
`DynamicAmounts.attachmentsOnEnchantedCreature()` (`EntityProperty(EnchantedCreature, AttachmentCount())`)
over `GroupFilter.attachedCreature()`. Card: **With Great Power . . .** [24].

<details><summary>Original analysis</summary>

> All damage that would be dealt to you is dealt to **enchanted creature** instead.

The static-redirect resolver `DamageUtils.resolveRedirectTarget` is a bespoke resolver (it does
NOT delegate to the general `TargetResolutionUtils.resolveTarget`) and handles only
`ControllerOfDamageSource`, `Controller`, `TargetController`, `Self` — `EffectTarget.EnchantedCreature`
(and `EquippedCreature`/`EnchantedPermanent`) hits `else -> null`, so the redirect is silently
skipped and damage stays on the player. All three shipped static-`RedirectDamage` cards
(Ancient Adamantoise, Martyrs of Korlis, Harsh Judgment) redirect to `Self`; no Aura→enchanted
(Pariah-style) redirect exists. The "+2/+2 for each Aura/Equipment attached" buff clause IS
expressible (`GrantDynamicStatsEffect` over `attachedCreature()` with
`AttachmentCount`). Fix (add-feature): extend `resolveRedirectTarget` with
`EnchantedCreature`/`EquippedCreature`/`EnchantedPermanent` → the Aura's `AttachedToComponent.targetId`.

Blocked cards:
- **With Great Power . . .** [24] — `{3}{W}` Aura; "+2/+2 per attached Aura/Equipment" (fine) + "all damage that would be dealt to you is dealt to enchanted creature instead" (the redirect is the blocker).
</details>

## "The legend rule doesn't apply to [filter]" exemption — ✅ IMPLEMENTED

**Implemented** on branch `spm-keywords-statics`. Added `LegendRuleDoesNotApplyTo(filter)` StaticAbility
(scan-based) + a consult hook in `LegendRuleCheck.check` (`isExemptFromLegendRule` — excludes matching
permanents from the duplicate grouping; the check now takes a `CardRegistry`). Card: **Spider-Verse** [93]
(the copy-spell-from-non-hand clause uses `youCastSpell(CastFromZoneOtherThan(HAND))` + `oncePerTurn` +
`CopyTargetSpell(addedTokenKeywords = HASTE)`, wrapped in `MayEffect`).

<details><summary>Original analysis</summary>

> The "legend rule" doesn't apply to **Spiders you control**.

No SDK static or effect exempts a filtered group from the legend rule. `LegendRuleCheck`
(`rules-engine/.../mechanics/sba/permanent/LegendRuleCheck.kt`) is a hard-coded state-based
action with no exemption/filter hook. Fix (add-feature): a `LegendRuleDoesNotApplyTo(filter)`
static + a consult hook in `LegendRuleCheck.check`. (The card's other clause — "whenever you cast
a spell from a non-hand zone, you may copy it once per turn; permanent copy gains haste" — is
fully expressible via `youCastSpell(CastFromZoneOtherThan(HAND))` + `oncePerTurn` +
`CopyTargetSpell(addedTokenKeywords = HASTE)`.)

Blocked cards:
- **Spider-Verse** [93] — `{3}{R}{R}` Enchantment; the legend-rule exemption for Spiders is the blocker (the copy-spell-from-non-hand clause is fine).
</details>

## Play cards exiled **face down** from an opponent's library (controller may look + cast) — ✅ IMPLEMENTED

**Done** (branch `spm-black-cat`). Needed **no new code** — the enabling feature (`FaceDownMode.HIDDEN`
controller-visible-in-exile masking + `GrantMayPlayFromExileEffect(withAnyManaType = true)` + the
cast-from-face-down-exile legal-action path) has landed since this card was first drafted (used by
Laughing Jasper Flint / Cruelclaw's Heist). Black Cat's prior draft (from branch `spm-no-engine`,
removed pending the feature) was restored verbatim: ETB `Gather` top-9 of `Targets.Opponent`'s library
→ `SelectFromCollection(ChooseExactly(2), showAllCards, storeRemainder)` → exile the two
`FaceDownMode.HIDDEN` + bottom the rest `CardOrder.Random` → `GrantMayPlayFromExile(Permanent,
withAnyManaType = true)`. Scenario test pins the face-down exile + casting a stolen off-color spell
paying entirely with off-color mana. Full regression green.

<details><summary>Original analysis (kept for reference)</summary>

> Look at the top nine cards of target opponent's library, **exile two of them face down**, then
> put the rest on the bottom in a random order. **You may play the exiled cards** for as long as
> they remain exiled. Mana of any type can be spent to cast spells this way.

`FaceDownMode.HIDDEN` is defined as face down with **no turn-up procedure** — "simply hidden;
nothing lets it be turned face up in place" (used for Hideaway, where the card is later played via
a dedicated activated ability that re-gathers `FromLinkedExile()` and grants may-play +
without-paying-cost at activation time — e.g. Clive's Hideaway, Mosswort Bridge). Black Cat instead
grants a **persistent** `GrantMayPlayFromExileEffect(MayPlayExpiry.Permanent)` directly over cards
sitting in exile `HIDDEN`, expecting the controller to see and freely cast them from the exile zone
at any time. That path isn't wired: HIDDEN cards are masked from everyone (including the
controller), and the playable-action computation does not surface a face-down exiled card as a
castable option. There is no face-down mode meaning "hidden from opponents, but the controller may
look at and cast it from exile." Fix (add-feature): a controller-visible face-down-in-exile mode +
masking that reveals those cards to their controller only + the cast-from-exile / legal-action path
recognizing persistent-may-play over face-down exiled cards (cross-layer: masking → ClientDTO →
CastSpellHandler).

Blocked cards:
- **Black Cat, Cunning Thief** [52] — `{3}{B}{B}` Legendary Creature — Human Rogue Villain, 2/3; the ETB look/exile-two-face-down/bottom-rest pipeline resolves, but "you may play the exiled cards" is uncastable because the face-down HIDDEN exiled cards never surface as playable to the controller. (Previously authored on branch `spm-no-engine`, then removed pending this feature.)

</details>

---

## Known divergences (card IS implemented, but one clause needs a future engine feature)

- **Superior Foes of Spider-Man** [96] — *implemented and committed.* "you may play that card
  **until you exile another card with this creature**" is modeled with `MayPlayExpiry.Permanent`
  (play for as long as it stays exiled). Faithful in normal play; the only divergence is the
  rare case where a second mv≥4 spell is cast while a still-unplayed card from a prior trigger
  sits in exile — the engine keeps both playable instead of revoking the earlier one. Strict
  fidelity would need a new source-scoped "supersede prior grant" `MayPlayExpiry` variant
  (add-feature).

---

<!-- Additional mechanics appended below as the loop encounters them. -->
