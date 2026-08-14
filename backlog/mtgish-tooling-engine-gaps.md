# mtgish Tooling — Engine Capability Gap Analysis

Cross-reference of the **engine/SDK's actual capability surface** against the two mtgish dictionaries
(`mtgish-tooling/src/main/kotlin/com/wingedsheep/tooling/coverage/`):

| Dictionary | Question it answers | Lives in |
|------------|---------------------|----------|
| **Capability** (`bridge/`) | *Can* Argentum express this tag? | `coverage/bridge/*` |
| **Rendering** (`emitter/`) | *What Kotlin DSL* does it emit? | `coverage/emitter/*Handlers.kt` |

This is the **inverse** of the usual coverage question. The dashboard's feature leaderboard answers
"what engine work unlocks the most cards?"; this document answers "what has the engine already
shipped that the tooling doesn't know about?" — capability we've paid for but aren't scoring.

Generated 2026-08-07 by scanning `mtg-sdk` source against a word-boundary match over both
dictionaries. See [Method & caveats](#8-method--caveats).

---

## Index

1. [Bottom line](#1-bottom-line)
1b. [**Ranked by cards unlocked** — the cross-set capability index](#1b-ranked-by-cards-unlocked)
2. [Effects in neither dictionary (188)](#2-effects-in-neither-dictionary-188)
   - [2.1 Counters & growth](#21-counters--growth)
   - [2.2 Explore, transform, phasing, animation](#22-explore-transform-phasing-animation)
   - [2.3 Life totals & game-ending](#23-life-totals--game-ending)
   - [2.4 Stack manipulation](#24-stack-manipulation)
   - [2.5 Combat](#25-combat)
   - [2.6 Types & colors](#26-types--colors)
   - [2.7 Protection & hexproof scoping](#27-protection--hexproof-scoping)
   - [2.8 Player-scoped statics & emblems](#28-player-scoped-statics--emblems)
   - [2.9 Tokens & copies](#29-tokens--copies)
   - [2.10 Granted alternative-cast keywords](#210-granted-alternative-cast-keywords)
   - [2.11 Zones, exile & removal riders](#211-zones-exile--removal-riders)
   - [2.12 Mana](#212-mana)
   - [2.13 Choice, pipeline & misc](#213-choice-pipeline--misc)
   - [2.14 Internal plumbing — do NOT map](#214-internal-plumbing--do-not-map)
3. [Bridge-only effects — always SCAFFOLD (27)](#3-bridge-only-effects--always-scaffold-27)
4. [Parameterized keyword abilities with no emitter branch (~35)](#4-parameterized-keyword-abilities-with-no-emitter-branch-35)
5. [Mechanic DSLs invisible to the tooling (16)](#5-mechanic-dsls-invisible-to-the-tooling-16)
6. [CostAtom variants (3)](#6-costatom-variants-3)
7. [Suggested order of attack](#7-suggested-order-of-attack)
8. [Method & caveats](#8-method--caveats)

---

## 1. Bottom line

| Axis | Engine has | Tooling knows | Gap |
|---|---|---|---|
| `Effect` SerialNames | 308 | 120 | **188 in neither dictionary** |
| Effects bridged but unrendered | — | — | **27** (forced `SCAFFOLD`) |
| `KeywordAbility` factories | ~70 | ~22 | **~35 unrendered** |
| `dsl/mechanics/*Dsl.kt` helpers | 29 | 13 | **16 unknown** |
| `CostAtom` variants | 12 | 9 | **3** |

**61% of the engine's `Effect` vocabulary is invisible to the coverage probe.** Roughly 25 of those
188 are internal plumbing that should never be mapped (§2.14), so the real actionable figure is
**~163 effects**.

Two distinct failure modes, with different consequences:

- **Unmapped in `bridge/`** — the probe reports the card `BLOCKED`/`UNMAPPED`. This *understates*
  our coverage and distorts the cross-set feature leaderboard: engine work already done shows up as
  work still to do. Fixing these is cheap (one line per tag) and changes the numbers immediately.
- **Bridged but unrendered in `emitter/`** — the card scores as covered but never auto-generates.
  This is why some sets show high `implemented/total` yet low `gN`. Fixing these is a handler, not
  a one-liner.

Keyword *capability* is less bad than the raw counts suggest: the probe's PascalCase→enum
auto-resolve (`Bridge.resolve`) covers bare keywords for free. The gaps in §4 are **rendering**
gaps, not coverage gaps — `keywordLines` correctly refuses to stamp a parameterized keyword bare
(it skips any rule carrying `args`), so those cards scaffold rather than mis-render. That guard is
working as designed; the missing piece is the explicit `rname ==` branch.

---

## 1b. Ranked by cards unlocked

Source: `just coverage-cross` (the non-interactive dump of the same `Analyzer.crossSet` roll-up the
TUI's `c` view shows) over **185 sets**, cross-referenced by hand against the SDK registry to
separate "the engine already has this" from "this is real engine work".

**Headline: ~6,000 of the 11,317 blocked card-instances — 53% of the entire "blocked on engine
work" backlog — are blocked on capability the engine already ships.** They're blocked by a missing
or wrong line in `bridge/`, not by missing engine code.

Counts are per-set instances (a reprint counts once per set), matching how the dashboard reports.
`SETS` is how many sets the capability appears in — a high number means the fix generalizes.

### Tier 0 — outright bugs in `bridge/` (self-caught, unfixed)

Both are `GAP` (kind = `MISSING`), meaning the bridge names a SerialName that isn't in the registry.
This is the anti-rot mechanism described in `bridge/Bridge.kt` working exactly as designed — it
caught the drift, nobody acted on it.

| Cards | Sets | Tag | Problem | Fix |
|------:|-----:|-----|---------|-----|
| 387 | 121 | `_LayerEffect = SetPT` | `ManaCountersAndState.kt:43` maps to `SetBasePowerToughness`; the actual SerialName is **`SetBaseStats`** | one word |
| 91 | 53 | `_StaticLayerEffect = SetPT` | same entry | — |
| 108 | 14 | `_Action = Investigate` | `DamageLifeAndCards.kt:99` maps to a SerialName `Investigate` that **does not exist** | re-file as `composed(...)` over the Clue-token primitives |

**586 card-instances on a one-word typo and one mis-filed entry.**

### Tier 1 — engine has it, bridge has no entry at all

Every row verified against the scanned registry (SerialName present) or a `KeywordAbility` factory /
`CardBuilder` helper.

| Cards | Sets | Tag | Engine capability |
|------:|-----:|-----|-------------------|
| **1674** | 145 | `_Action = PutACounterOfTypeOnPermanent` | `AddCounters` ✅ |
| **1336** | 147 | `_Trigger = WhenACreatureOrPlaneswalkerDies` | dies triggers ✅ (`WhenAPermanentDies` is already `supported`) |
| 372 | 118 | `_Action = PutNumberCountersOfTypeOnPermanent` | `AddCounters` ✅ |
| 282 | 104 | `_LayerEffect = AddCardtype` | `AddCardType` ✅ |
| 238 | 93 | `_Action = PutACounterOfTypeOnEachPermanent` | `AddCounters` ✅ |
| 169 | 48 | `_Action = ChooseADamageSource` | `ChosenSource` (`PreventionSourceFilter`) ✅ |
| 119 | 17 | `_Rule = Kicker` | `KeywordAbility.kicker` ✅ |
| 105 | 45 | `_LayerEffect = AddColor` | `AddColor` ✅ |
| 101 | 52 | `_LayerEffect = LosesAbility` | `RemoveAllAbilities` ✅ (partial — "loses *a* named ability" is narrower) |
| 98 | 19 | `_Rule = SpellActions_Kicker` | `KeywordAbility.kicker` ✅ |
| 94 | 46 | `_Action = GetAnEmblem` | `CreatePermanentEmblem` ✅ |
| 94 | 13 | `_Action = Proliferate` | `Proliferate` ✅ |
| 87 | 49 | `_Action = HaveCreaturesFight` | `Fight` ✅ |
| 80 | 11 | `_Rule = Morph` | `KeywordAbility.morph` ✅ (emitter already has the branch) |
| 79 | 44 | `_StaticLayerEffect = AddCardtype` | `AddCardType` ✅ |
| 72 | 10 | `_Rule = SagaChapters` | `CardBuilder.sagaChapter` ✅ |
| 52 | 35 | `_Action = WinTheGame` | `WinGame` ✅ |
| 48 | 5 | `_Rule = LevelUp` | `LevelUpClass` / `CardBuilder.classLevel` ✅ |
| 43 | 14 | `_Rule = Multikicker` | `KeywordAbility.multikicker` ✅ |
| 40 | 30 | `_Action = SetLifeTotal` | `SetLifeTotal` ✅ |
| 37 | 25 | `_StaticLayerEffect = SetLandType` | `SetLandType` ✅ |
| 33 | 15 | `_Action = ReturnSpellToOwnersHand` | `ReturnSpellToOwnersHand` ✅ |
| 31 | 22 | `_Action = PutNumberCountersOfTypeOnEachPermanent` | `AddCounters` ✅ |
| 29 | 24 | `_LayerEffect = SetLandType` | `SetLandType` ✅ |
| 29 | 4 | `_Action = Amass` | `Amass` ✅ |
| 28 | 20 | `_StaticLayerEffect = LosesAbility` | `RemoveAllAbilities` ✅ |
| 27 | 12 | `_Action = PhaseOutPermanent` | `PhaseOut` ✅ |
| 27 | 6 | `_Action = ExploreWithPermanent` | `Explore` ✅ |
| 26 | 7 | `_Rule = Dash` | `KeywordAbility.dash` ✅ |

**Subtotal ≈ 5,450 card-instances.** The top two rows alone are 3,010 — over a quarter of the entire
blocked backlog, on two tags whose capability is unambiguous.

> The two counter tags are worth understanding before fixing: `ManaCountersAndState.kt:52` already
> maps `ACounterOfTypeOnPermanent` / `NumberCountersOfTypeOnPermanent` — the *nested* variants under
> the `PutCounters` envelope. The IR also emits a **direct `_Action` form** with a `Put` prefix, and
> that key was never added. Same capability, different node shape.

### Tier 2 — structural envelopes

Not effects — mtgish wrappers whose children carry the real capability. Each is a candidate
`envelope(...)` row, but each needs a look at what the IR actually nests before pinning.

| Cards | Sets | Tag |
|------:|-----:|-----|
| 299 | 105 | `_Rule = EachPermanentRuleEffect` |
| 234 | 34 | `_Rule = DeckConstruction` (not a gameplay capability at all — deck rules; should probably be `envelope`/ignored) |
| 202 | 69 | `_Rule = FromStack` |
| 100 | 55 | `_Rule = ThisSpellEffect` |
| 79 | 27 | `_Rule = FromAnyZone` |
| 39 | 37 | `_Rule = ActivatedAbilityEffect` |
| 28 | 13 | `_Rule = FromExile` |

**Subtotal ≈ 981.** `FromStack` / `FromAnyZone` / `FromExile` already have emitter `rname` branches —
only the bridge pin is missing.

### Tier 3 — genuine engine work (the honest backlog)

These rank high but the engine does **not** have them. This is what the leaderboard is *supposed* to
be telling us, now that the noise above is separated out.

| Cards | Sets | Tag | Note |
|------:|-----:|-----|------|
| 279 | 87 | `_Cost = RemoveACounterOfTypeFromPermanent` | counter-removal as a cost |
| 246 | 102 | `_Cost = PayManaX` | the extra-costs / chosen-X area the creator's note flags |
| 145 | 9 | `_Rule = CDA_Color` | characteristic-defining color |
| 142 | 67 | `_Action = RevealTopCardOfLibrary` | |
| 134 | 56 | `_Cost = RemoveNumberCountersOfTypeFromPermanent` | |
| 133 | 74 | `_Action = RevealTheTopNumberCardsOfLibrary` | |
| 127 | 63 | `_Action = PermanentDoesntUntapDuringControllersNextUntap` | |
| 124 | 75 | `_Action = PlayerActions` | |
| 120 | 62 | `_Cost = TapAPermanent` | |
| 118 | 63 | `_Trigger = WhenAPermanentDealsDamageToAPlayer` | |
| 116 | 68 | `_Action = DiscardHand` | |
| 115 | 65 | `_Action = CopySpellAndMayChooseNewTargets` | |
| 113 | 18 | `_Rule = CDA_Types` | |
| 111 | 65 | `_Trigger = WhenACreatureBlocksACreature` | block triggers — a whole family (`WhenACreatureBecomesBlocked` 94, `…BecomesBlockedByACreature` 98) |
| 97 | 8 | `_Rule = CumulativeUpkeep` | |
| 53 | 22 | `_Rule = Intimidate` | **already pinned** `unsupported` in `bridge/Keywords.kt` — correctly blocking |

Mechanics with no engine support that show up further down: Bestow (54), Buyback (53), Monstrosity
(53), Echo (74), Unearth (68), Exalted (47), Megamorph (44), Infect (71), Soulbond (37), Escape (34),
Splice onto Arcane (34), Mutate (33), Extort (30), Retrace (30), Improvise (29), Split Second (29),
Bloodthirst (28), Mentor (28), Living Weapon (27), Soulshift (30), Disguise (38), Overload (42),
Bolster (26), Support (34), Adapt (32), Clash (36), Monarch (55), Venture/dungeon (34), Roll a d20
(49). Each is a real `add-feature` unit.

### What this changes about §7

The original suggested order was written from engine structure. The data says otherwise — **fix the
bridge before writing any engine code.** Revised order is in [§7](#7-suggested-order-of-attack).

---

## 2. Effects in neither dictionary (188)

Grouped by theme. Each name is an `Effect` `@SerialName` from
`mtg-sdk/src/main/kotlin/com/wingedsheep/sdk/scripting/effects/`.

### 2.1 Counters & growth

`AddCountersToCollection`, `Amass`, `ConvertCountersToTokens`, `DistributeCountersAmongFiltered`,
`DistributeCountersFromSelf`, `MoveChosenCountersToTarget`, `MoveCounters`,
`MoveCountersEachKindMissing`, `Proliferate`, `RemoveAllCounters`

> `Proliferate` alone is an evergreen-adjacent mechanic spanning Scars of Mirrodin through
> Phyrexia: All Will Be One. Its absence blocks every card that references it.

### 2.2 Explore, transform, phasing, animation

`Explore`, `Transform`, `ExileAndReturnTransformed`, `ReturnSelfFromExileTransformed`,
`ReturnSelfFromZoneTransformed`, `PhaseOut`, `PhaseOutUntilLeaves`, `PhaseInLinkedToSource`,
`TurnFaceDown`, `MassAnimate`, `BecomeArtifact`, `BecomeSaddled`, `TapUntapCollection`

### 2.3 Life totals & game-ending

`DrainLife`, `ExchangeLifeTotals`, `ExchangeLifeAndStat`, `SetLifeTotal`, `PayDynamicLife`,
`WinGame`, `EndTheTurn`, `SkipNextTurn`, `SkipNextDrawStep`, `HijackNextTurn`,
`AddAdditionalUpkeepSteps`, `AddAdditionalEndSteps`

### 2.4 Stack manipulation

The single largest untouched cluster — 19 effects, none mapped.

`ChangeSpellTarget`, `ChangeTriggeringObjectTargets`, `CopyEachSpellCast`, `CopyEachTargetSpell`,
`CopyTargetTriggeredAbility`, `CounterAllOnStack`, `DestroySourceOfTargetedAbility`,
`ExileSpellsOnStack`, `GrantKeywordToSpell`, `GrantNextSpellAffinity`, `MakeNextSpellUncounterable`,
`MarkSpellExileWithCounters`, `ReduceSpellCostsThisTurn`,
`RemoveAbilitiesFromSourceOfTargetedAbility`, `ReselectTargetRandomly`,
`ReturnSpellOrPermanentToOwnersHand`, `ReturnSpellToOwnersHand`, `StormCopy`, `WardCounter`

### 2.5 Combat

`CanAttackDespiteDefenderThisTurn`, `CantAttackGroup`, `CantBlockGroup`, `ForceBlock`,
`GrantAttackBlockTaxPerCreatureType`, `GrantCantBeBlockedByChosenColor`,
`GrantKeywordToAttackersBlockedBy`, `MarkMustAttackThisTurn`, `Provoke`,
`RedirectCombatDamageToController`, `SetSuspected`

Plus from removal/damage: `CantBeRegenerated`, `AmplifyNoncombatDamageThisTurn`,
`DealDamagePerEntityInZone`, `DoubleDamageToPlayer`, `RemoveDamageShield`

### 2.6 Types & colors

All 13 effects in `TypeAndColorEffects.kt` are unmapped:

`AddCardType`, `AddColor`, `AddSubtype`, `BecomeChosenManaColor`, `ChangeColor`,
`ChangeColorToChosen`, `ChangeCreatureTypeText`, `ChangeGroupColor`, `ChangeWordInText`,
`ChooseColorForTarget`, `LoseAllCreatureTypes`, `SetCreatureSubtypes`, `SetGroupCreatureSubtypes`

### 2.7 Protection & hexproof scoping

`ChooseColorThen`, `GrantHexproofFromChosenColor`, `GrantPlayerProtection`,
`GrantProtectionFromChosenCardType`, `GrantProtectionFromChosenColor`

### 2.8 Player-scoped statics & emblems

`CantActivateLoyaltyAbilities`, `CantCastSpellsFromNonHandZones`, `CreatePermanentEmblem`,
`GainCitysBlessing`, `GiftGiven`, `GrantCastCreaturesFromGraveyardWithForage`, `GrantDamageBonus`,
`GrantEvasionKeyword`, `GrantFlashToSpells`, `GrantSpellKeyword`, `GrantSpellsCantBeCountered`,
`LockLifeGain`, `PreventLandPlaysThisTurn`, `TheRingTemptsYou`

### 2.9 Tokens & copies

`CreateRandomCreatureTokenWithManaValue`, `CreateRoleToken`, `CreateTokenCopyOfChosenPermanent`,
`CreateTokenCopyOfEquippedCreature`, `CreateTokenCopyOfSource`, `BecomeCopyOfLinkedExile`,
`CopyCardIntoCollection`, `CopyCollectionIntoCollection`, `ChainCopy`,
`EachPermanentBecomesCopyOfTarget` *(bridged — see §3)*

### 2.10 Granted alternative-cast keywords

`GrantEmbalm`, `GrantFlashback`, `GrantHarmonize`, `GrantSuspend`, `GrantReplacementEffect`,
`GrantActivatedAbilityToGroup`, `RemoveAllAbilities`, `GrantFreeCastTargetFromExile`,
`GrantPlayWithAdditionalCost`, `GrantPlayWithCostIncrease`, `GrantExileOnLeave`,
`CastAnyNumberFromCollectionWithoutPayingCost`

### 2.11 Zones, exile & removal riders

`DestroyAllEquipmentOnTarget`, `ExileAndGrantOwnerPlayPermission`, `ExileOpponentsGraveyards`,
`ExileWithAurasNotingCounters`, `ExileFromTopRepeating`, `ForceExileMultiZone`,
`MoveTrackedBattlefieldObject`, `ReturnCreaturesPutInGraveyardThisTurn`,
`ReturnNotedExileTappedWithAuras`, `ReturnOneFromLinkedExile`, `ReturnSelfToBattlefieldAttached`,
`ReturnSameNamedFromGraveyard`, `WarpExile`, `AttachTargetEquipmentToCreature`,
`UnattachEquipment`, `GainControlByActivePlayer`, `GainControlByMost`

### 2.12 Mana

`AddAnyColorManaSpendOnChosenType`, `AddDynamicMana`, `AddOneManaOfEachColorAmong`,
`PayDynamicManaCost`, `RetainUnspentMana`

### 2.13 Choice, pipeline & misc

`AnyPlayerMayPay`, `Behold`, `BudgetModal`, `FlipCoins`, `FlipTwoCoins`, `SecretBid`, `OpenLifeBid`,
`LevelUpClass`, `LockDoor`, `UnlockDoor`, `CaptureControllers`, `ChooseCreatureType`,
`ChooseOption`, `ChoosePile`, `ConditionalOnCollection`, `EachPlayerChoosesCreatureType`,
`ForEachCapturedController`, `GatherSubtypes`, `ChooseCardTypeForSource`, `ChooseNumberForSource`,
`ChooseNumberThen`, `ChooseOpponentForSource`, `EachPlayerDiscardsOrLoseLife`,
`EachPlayerDrawsForDamageDealtToSource`, `LookAtFaceDown`, `MayRevealCardFromHand`,
`OpponentGuessesTopCardKindEffect`

> ⚠ Several of these sit squarely in the **extra-costs / chosen-value** area the creator's note in
> `mtgish-tooling/README.md` flags as engine-sloppy (`ChooseNumberThen`, `ChooseCardTypeForSource`,
> `AnyPlayerMayPay`, `PayDynamicManaCost`). Map the **capability** in `bridge/` so the leaderboard is
> honest, but the emitter should keep declining → `SCAFFOLD`.

### 2.14 Internal plumbing — do NOT map

These are engine-internal markers with no mtgish IR counterpart. Listed for completeness so a
future sweep doesn't mistake them for gaps.

`EmitBendEvent`, `EmitDiscoveredEvent`, `EmitExploitedEvent`, `EmitExploredEvent`,
`EmitLibrarySearchedEvent`, `EmitManifestedDreadEvent`, `EmitScriedEvent`, `EmitSurveiledEvent`,
`EmitTrainedEvent`, `IncrementAbilityResolutionCount`, `MarkEnduringReturn`,
`MarkExileControllerGraveyardOnDeath`, `NoteCreatureType`, `RecordChosenLinkedExile`,
`StoreCardName`, `StoreNumber`

---

## 3. Bridge-only effects — always SCAFFOLD (27)

Capability is mapped, so the probe scores these as covered — but no emitter handler exists, so the
card never auto-generates. These are the reason a set can show high coverage and low `gN`.

`AddCountersUpTo`, `AnimateLand`, `CantBlockTargetCreatures`, `CantPlayCardsFromHand`,
`ChangeSpeed`, `ChooseOnePerCategory`, `CreateGlobalTriggeredAbility`,
`DistributeCountersAmongTargets`, `DrawUpTo`, `EachPermanentBecomesCopyOfTarget`,
`EachPlayerReturnsPermanentToHand`, `ExileTargetSpell`, `FilterCollection`, `Gated`, `LoseGame`,
`MarkSpellPlotOnResolve`, `PayCounters`, `PayFixedCounters`, `PreventDamageShield`,
`PutOnLibraryPositionOfChoice`, `RedirectNextDamage`, `ReduceMaximumHandSize`,
`RemoveAnyNumberOfCounters`, `RepeatDynamicTimes`, `RepeatWhile`, `ReplaceNextDrawWith`, `TapUntap`

Note that some of these are *correctly* unrendered per the fidelity policy — `Gated`,
`RepeatWhile`, and `FilterCollection` are structural and their payload is a per-card read. The
straightforwardly renderable ones are `DrawUpTo`, `AddCountersUpTo`, `TapUntap`,
`RemoveAnyNumberOfCounters`, `ExileTargetSpell`, `LoseGame`, `ReduceMaximumHandSize`.

---

## 4. Parameterized keyword abilities with no emitter branch (~35)

The engine models all of these via `KeywordAbility` factories
(`mtg-sdk/src/main/kotlin/com/wingedsheep/sdk/scripting/KeywordAbility.kt`). `keywordLines` in
`emitter/Shells.kt` correctly skips any rule with `args`, so they scaffold instead of dropping their
parameter. Each needs an explicit `rname == "X"` branch alongside the existing Ward / Saddle /
Madness / Impending / Firebending ones.

### Numeric (`KeywordAbility.Numeric`)

`absorb`, `afflict`, `annihilator`, `bushido`, `casualty`, `devour` / `devourLand`, `fabricate`,
`fading`, `hideaway`, `mobilize`, `modular`, `rampage`, `renown`, `toxic`, `tribute`, `vanishing`

These are the cheapest wins in the whole document — the shape is identical to the already-rendered
`saddle(N)` / `firebending(N)`, just a different factory name.

### Cost-carrying

`basicLandcycling`, `cleave`, `dash`, `evoke`, `foretell`, `harmonize`, `mayhem`, `miracle`,
`morphPayLife`, `ninjutsu`, `offspring`, `sneak`, `suspend`, `warp`, `webSlinging`

### Kicker family

`kicker`, `multikicker`, `flashKicker` — `kicker` is mentioned in `bridge/` but has no emitter
render.

### Scoped protection / ward variants

`hexproofFrom`, `protectionFromSubtype`, `protectionFromSupertype`, `wardComposite`,
`wardWaterbend`

### No-arg but unbranched

`conspire`

---

## 5. Mechanic DSLs invisible to the tooling (16)

`mtg-sdk/src/main/kotlin/com/wingedsheep/sdk/dsl/mechanics/` ships 29 helpers. These 16 appear in
**neither** dictionary:

`BeginGameOnBattlefield`, `Converge`¹, `Craft`, `Decayed`, `Embalm`, `Enduring`, `Flurry`,
`JobSelect`, `Mayhem`, `Mobilize`, `Ninjutsu`, `Renew`, `Riot`, `Sneak`, `Vivid`, `WebSlinging`

¹ `Converge` is referenced in `emitter/` but has no `bridge/` pin, so it can't score as a
capability.

**Partially covered** — bridge pin exists, emitter declines:

| Mechanic | Status |
|---|---|
| `Disturb` | Deliberate — every disturb card is a transforming DFC and the emitter declines all multi-faced cards |
| `Gift` | Deliberate — which `GiftKind` is listed is a per-card read |
| `Exploit` | **Looks like a genuine miss** — worth a handler |

---

## 6. CostAtom variants (3)

`CostAtom` is nearly complete. Unknown to the tooling:

`ExileFrom`, `PutCountersOnSelf`, `RevealFromHand`

---

## 7. Suggested order of attack

Ordered by measured cards-unlocked from [§1b](#1b-ranked-by-cards-unlocked), not by intuition.

1. **Fix the two `GAP` bridge entries** — `SetPT` → `SetBaseStats` (one word) and re-file
   `Investigate` as `composed`. **586 card-instances**, minutes of work. Add a test that asserts
   every `effect(...)`/`keyword(...)` tag resolves against the registry so `GAP` can't sit unfixed
   again — the mechanism already detects this, it just has no alarm.
2. **Add the Tier 1 bridge rows** — ~29 lines. **≈5,450 card-instances.** Start with
   `PutACounterOfTypeOnPermanent` and `WhenACreatureOrPlaneswalkerDies`: 3,010 between them.
3. **Pin the Tier 2 structural envelopes** — ~981 more, and `FromStack`/`FromAnyZone`/`FromExile`
   already have emitter branches waiting.
4. **Re-run `just coverage-cross`** — steps 1–3 will reshuffle the ranking substantially. Do not
   plan engine work off the current numbers; more than half the list is noise until the bridge is
   honest.
5. **Numeric keyword branches (§4)** — a mechanical copy of the `saddle(N)` branch, ~16 keywords,
   converts a tail of old-set creatures from `SCAFFOLD` to `WHOLE`. Emitter work, so it raises `gN`
   rather than unblocking cards.
6. **Then, and only then, real engine work** — Tier 3. The block-trigger family
   (`WhenACreatureBlocksACreature` + `…BecomesBlocked` + `…BecomesBlockedByACreature` ≈ 303 across
   ~65 sets) is the largest single coherent unit and is plain `add-feature` territory.

Two caveats on the counts. They're **per-set instances** — a reprint counts once per set, so the
unique-card figure is lower (`autogen --all --gaps --unique` collapses it). And a bridge row moves a
card from `BLOCKED` to `SCAFFOLD`, not necessarily to `AUTOGEN`: it makes our *coverage reporting*
honest, which is the point, but only an emitter handler makes the card auto-generate.

---

## 8. Method & caveats

**How this was produced.** For each axis, the SDK source was scanned for the capability's canonical
identifier, then word-boundary matched against the concatenated text of `bridge/*.kt` and
`emitter/*.kt`:

- **Effects** — `@SerialName("X")` in `scripting/effects/`, filtered to declarations whose supertype
  is `Effect` (this correctly excludes the 90 co-located `CollectionFilter`, `ManaRestriction`,
  `CardSource`, `SelectionMode`, `PreventionSourceFilter`, `MayPlayExpiry`, `RepeatCondition`,
  `FeasibilityCheck`, `PlayerRankMetric`, `ManaSpellRider`, and `CardDestination` variants). Matched
  on both the SerialName and the Kotlin class name.
- **Keywords** — `Keyword` enum members, matched on both `UPPER_SNAKE` and the `PascalCase` form the
  bridge's auto-resolve would produce.
- **Keyword abilities** — `fun` factories in `KeywordAbility.kt`'s companion.
- **Mechanics** — filenames under `dsl/mechanics/`.
- **CostAtom** — `data class` / `object` declarations in `costs/CostAtom.kt`.

**Caveats.**

- Word-boundary matching is a heuristic. A handful of entries may already be reachable indirectly
  through a `composed` / `envelope` bridge entry filed under a different mtgish tag name — verify
  before adding a row (`BridgeBuilder.add` rejects duplicate keys loudly, so a collision fails fast).
- The counts assume every `Effect` has a working executor. `add-feature`'s executor-coverage
  guarantee makes a missing Static/Replacement executor a compile error, so this is safe for those
  two families; a leaf resolution effect that exists but is engine-inert would need the
  `unsupported(...)` treatment `Intimidate` gets in `bridge/Keywords.kt`.
- Adding a bridge row makes the probe score a card as coverable. If the emitter can't render it
  exactly, that's correct and expected — the card lands in `SCAFFOLD`, not `BLOCKED`. Per the
  fidelity policy, **a confidently wrong generated card is worse than no generated card**: return
  `null` from the handler rather than widen a filter to make something emit.

**Regenerating this.** §2–§6 come from a ~40-line script over the two source trees; nothing there is
committed tooling. If that becomes a recurring check, the natural home is a `probe --unmapped-sdk`
subcommand in `coverage/Main.kt` reusing `Registry.loadEffectSerialNames()` and `Bridge.entry(...)`,
which would also make it a test-able invariant instead of a point-in-time document.

§1b **is** reproducible: `just coverage-cross` (added for this analysis) dumps the cross-set index
non-interactively. The tier classification on top of it is hand-verified against the registry and
will need redoing as the bridge changes — but the ranking underneath regenerates in ~90s.

**On the §1b counts specifically.** Every ✅ in Tier 1 was checked against
`Registry.loadEffectSerialNames()` output or a concrete `KeywordAbility` / `CardBuilder` factory —
none are inferred from the tag name. What is *not* verified is whether each tag's **arguments** are
recoverable; a bridge row asserts the capability exists, which is the honest claim, and the emitter
independently decides whether it can render exactly. Two rows are marked partial (`LosesAbility` →
`RemoveAllAbilities`) because the engine primitive is broader than the tag; those need a closer read
before pinning.
