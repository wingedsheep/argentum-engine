# Assay ⇄ Engine vocabulary gap analysis

**Snapshot:** 2026-09-30, `main` @ `5346242787`. Point-in-time report — the numbers go stale as Assay
bands land; regenerate rather than edit (see [Method](#method)).

**Question:** which capabilities does the engine/SDK support — i.e. which `mtg-sdk` model values
hand-written cards actually use — that Argentum Assay's grammar never produces from Oracle text?

## Headline

| | total in SDK | used by ≥1 hand-written card | produced by Assay | **gap** (cards use it, Assay never produces it) | used by neither |
|---|--:|--:|--:|--:|--:|
| Sealed subtypes (effects, statics, conditions, triggers, amounts, costs, filters, …) | 1,552 | 1,455 | 378 | **1,077** | 97 |
| `Keyword` values | 121 | 114 | 90 | **27** | 4 |

- Assay produces **~26%** of the SDK subtypes the card corpus uses. Everything it produces is also used
  by some card (no Assay-only vocabulary).
- Of the 13,985 hand-written cards, the baked ledger has **6,911** read whole by Assay and
  **6,340** stopped by at least one declined line (+71 lines-don't-fold, 33 multi-face, 630 not in
  the ledger by name).
- The gap is concentrated: `Effect` (267 subtypes), `StaticAbility` (156), `Condition` (93),
  `CardPredicate` (78), `StatePredicate` (66), `EventPattern` (55), `DynamicAmount` (40),
  `ReplacementEffect` (39). Beyond subtypes, **264 fields** on types Assay *does* produce are set by
  cards but never by Assay (see [Axis gaps](#axis-gaps-on-types-assay-already-produces)).

## Where the gap is — themes

Grouping the largest gaps by what they would take to close. Card counts are distinct hand-written
cards using the value.

1. **Reflexive / pipeline machinery.** `ReflexiveTrigger` (113), `EffectTarget.PipelineTarget` (160),
   `SelectTarget` (32), `StoreNumber` (17), `StoreCardName`, `GatherUntilMatch` (37),
   `CastFromCollectionWithoutPayingCost` (42), `DynamicAmount.StoredCardManaValue` (20),
   `CollectionContainsMatch` (58), plus `MoveCollection.storeMovedAs` (78) and
   `Gated.otherwise` (197) as axes. "When you do, …", "exile … then you may cast it", "if it's a
   creature card, …" — the captured-collection vocabulary the batch-trigger band already named as the
   real next band.
2. **Kicker / cast-history conditions.** `Condition.WasKicked` (96), `WasCast` (19),
   `WasCastFromZone` (16), `ManaSpentToCastIncludes` (14), `SourceChosenModeIs` (17), and the
   `ContextPropertyKey` enum (23 of 24 values unproduced — e.g. `MANA_SPENT_ON_TRIGGERING_SPELL`).
   Assay reads the `Kicker` keyword (152 cards) but not the "if this spell was kicked" payoff.
3. **Turn-history ("this turn") vocabulary.** `TurnTracker` enum 30 of 35 used values unproduced
   (`PLAYER_ATTACKED` 24, `DESCENDED` 12, `NONLAND_PERMANENTS_ENTERED` 11, …),
   `PlayerCastSpellsThisTurn` (31), `CreatureDiedThisTurn` (14),
   `PermanentLeftBattlefieldThisTurn` (10), `SourceAbilityResolvedNTimesThisTurn` (14),
   `StatePredicate.EnteredThisTurn` (20) / `WasDealtDamageThisTurn` (13) / `AttackedThisTurn` (10).
4. **Tokens beyond the vanilla body.** `CreateTokenCopyOfTarget` (75), `CreateTokenCopyOfSource`
   (32), `CreateRoleToken` (35), `Amass` (41), and on `CreateToken` itself: `name` (54),
   `artifactToken` (67), token `staticAbilities`/`triggeredAbilities`/`activatedAbilities`
   (49/31/15), `attacking` (29), `sacrificeAtStep` (18), `legendary` (12), dynamic P/T (28).
   (`imageUri` 597 is presentation, not a gap.)
5. **Control, copy and type-changing.** `GainControl` (61), `GiveControlToTargetPlayer` (16),
   `CopyTargetSpell` (35), `EachPermanentBecomesCopyOfTarget` (20), `SetBaseStats` (37),
   `AnimateLand` (29), `AddCreatureType` (19), `AddCardType` (15), `RemoveAllAbilities` (14),
   statics `GrantCardType` (35), `LoseAllAbilities` (20), `SetBasePowerToughnessDynamicStatic` (15,
   *deliberate* — see below), `ControlEnchantedPermanent` (13), replacement `EntersAsCopy` (17).
6. **Counters as objects.** `AddCountersToCollection` (71), `RemoveCounters` (46),
   `DistributeCountersAmongTargets` (18), `MoveAllLastKnownCounters` (16), `DoubleCounters` (10),
   `Proliferate` (4), replacement `ModifyCounterPlacement` (8), `DoubleCounterPlacement` (4),
   `EntersWithCounters.condition` (51) as an axis.
7. **Aura/Equipment subjects.** `EffectTarget.EnchantedCreature` (62), `EquippedCreature` (16),
   `AttachTargetEquipmentToCreature` (23), `StatePredicate.IsEquipped` (16) /
   `IsAttachedToBySource` (15), `GrantTriggeredAbility` (69; its `target`/`duration` axes too),
   `GrantStaticAbility` (22), `GrantActivatedAbility` (8), `GrantWard` (20), `GrantProtection` (11).
8. **Set mechanics with engine support but no grammar row.** The Ring tempts you (45), bending
   (`EmitBendEvent` 61 + `BendType`), manifest dread (27), prepare (`BecomePrepared` 25 +
   `PREPARED` 40), discover (23), clash (20), connive (20), suspect (16 + `IsSuspected`), explore
   (17), saddle (`IsSaddled` 28), station (`StationCharge` 27), speed (`Speed` 25, `MAX_SPEED` 34),
   cases (`BecomeSolved` 12), rooms/doors (`DoorUnlockedEvent` 20, `RoomFullyUnlockedEvent` 15),
   crimes (`CommitCrimeEvent` 19), gift (`GiftGiven` 23, `ChooseOpponentForSource` 43), craft
   (`CostCraft` 19), `ExileUntilLeaves` (50, O-Ring shape), fight (`Fight` 47).
9. **Trigger events.** `YouAttackEvent` (83), `DamageEvent` (56), `NthCardDrawnEvent` (34),
   `DrawEvent` (21), `AbilityActivatedEvent` (18), `ScriedEvent` (11), `BlocksOrBecomesBlockedByEvent`
   (10), `BecomesUnblockedEvent` (9), `CounterPlacementEvent` (8), plus axes on events Assay does read:
   `AttackEvent.requires/filter` (40/34), `DealsDamageEvent.sourceFilter` (43),
   `CreateDelayedTrigger.trigger/watchedTarget` (44/24).
10. **Costs.** Planeswalker loyalty costs (`CostLoyalty` 68, `CostLoyaltyX` 3 — no loyalty-ability
    line reads), `CostDiscardSelf` (33, channel-style), `AdditionalCost` 14 of 15
    subtypes, `ManaColorSet` (none of its 9 used values produced; `AddManaOfChoice.colorSet`, set by
    43 cards, is never set by Assay), `ManaExpiry.END_OF_COMBAT` (27).
11. **Filter vocabulary.** `CardPredicate.IsToken` (117), `HasAnyOfSubtypes` (49), `IsBattle` (32),
    `IsNonlegendary` (15), `SharesColorWith` (15), `NameEqualsChosen` (16), toughness bounds;
    `StatePredicate.StateNot` (37 — negated state), `IsSource` (20), `HasAnyCounter` (11);
    `Aggregation` (`DISTINCT_TYPES` 26, `DISTINCT_COLORS` 17, `SUM` 11); `DynamicAmount.Add` (50),
    `AggregateZone` (48), `Conditional` (37), `CastX` (19).
12. **Keywords** — see below; mostly ability words the engine tags for display plus a handful of
    real keyword abilities (dredge, soulshift, champion, bargain, escape, bestow, replicate, split
    second, evolve, ravenous, umbra armor).

### Not gaps — deliberate Assay choices and structural limits

- **Two SDK spellings, one canonical.** Assay emits one model per meaning and declines the minority
  spelling on purpose, so these show as "gaps" but are findings about the corpus, not missing grammar:
  `SetBasePowerToughnessDynamicStatic` (CDA lives in `creatureStats`), `ManaColorSet.Specific`
  (dual-land lines), `ProtectionScope.Colors` / `Simple(PROTECTION_FROM_EACH_OPPONENT)`,
  `CostReductionSource.FixedIf…` vs `CostGating.OnlyIf`, `DynamicAmount.Count` on the battlefield vs
  `AggregateBattlefield`, `EventPattern.AnyOfEvents` (9) vs two abilities, `ModifyLifeGain(0,0)`.
- **Multi-face cards are out of scope for Assay** (adventure, split, MDFC, transform, flip, rooms,
  prepare/omen layouts). Values that only exist on those — `TransformEvent` (23), `Flip` (10),
  `ReturnSelfFromExileTransformed` (19), `ExileAndReturnTransformed` (18), `HasAdventure`,
  `UnlockDoor`, `CardLayout.*` — close only if that scope changes.
- **Ability words are normalized away** (CR 207.2c: no rules meaning), so `Keyword` markers like
  `EERIE`, `VIVID`, `RENEW`, `FLURRY`, `MAX_SPEED`, `PREPARED`, `JOB_SELECT`, `PARADIGM` show as
  unproduced even when Assay reads the ability behind them.
- **Presentation fields** (`descriptionOverride`, `imageUri`, `prompt`, `consequenceDescription`)
  appear in the axis table but carry no rules meaning; the differential folds them out.
- **Engine-internal event emitters** (`EmitLibrarySearchedEvent`, `EmitBendEvent`,
  `EmitChampionedEvent`, `EmitExploitedEvent`, `EmitTrainedEvent`, …) are lowered by SDK facades;
  Assay closes them by calling the same facade, not by spelling them.


## Gap by SDK family

| SDK family | subtypes | used by cards | read by Assay | **gap** (cards use, Assay never emits) | cards blocked by gap* | unused by any card |
|---|--:|--:|--:|--:|--:|--:|
| `Effect` | 357 | 344 | 77 | **267** | 2264 | 13 |
| `StaticAbility` | 181 | 181 | 25 | **156** | 611 | 0 |
| `Condition` | 115 | 106 | 13 | **93** | 517 | 9 |
| `CardPredicate` | 112 | 111 | 33 | **78** | 452 | 1 |
| `StatePredicate` | 76 | 76 | 10 | **66** | 333 | 0 |
| `EventPattern` | 95 | 86 | 31 | **55** | 428 | 9 |
| `DynamicAmount` | 55 | 55 | 15 | **40** | 368 | 0 |
| `ReplacementEffect` | 47 | 46 | 7 | **39** | 173 | 1 |
| `AbilityCost` | 27 | 27 | 7 | **20** | 155 | 0 |
| `EffectTarget` | 31 | 26 | 8 | **18** | 334 | 5 |
| `CostReductionSource` | 26 | 25 | 8 | **17** | 37 | 1 |
| `Player` | 26 | 26 | 10 | **16** | 111 | 0 |
| `Duration` | 21 | 17 | 2 | **15** | 162 | 4 |
| `SpellCastPredicate` | 16 | 16 | 1 | **15** | 49 | 0 |
| `AdditionalCost` | 15 | 15 | 1 | **14** | 64 | 0 |
| `CardSource` | 18 | 18 | 5 | **13** | 157 | 0 |
| `CostAtom` | 22 | 22 | 10 | **12** | 26 | 0 |
| `EffectTarget.SingleEntity` | 22 | 15 | 5 | **10** | 163 | 7 |
| `EntityNumericProperty` | 16 | 15 | 5 | **10** | 39 | 1 |
| `ManaColorSet` | 10 | 9 | 0 | **9** | 53 | 1 |
| `SelectionRestriction` | 9 | 9 | 0 | **9** | 28 | 0 |
| `ControllerPredicate` | 15 | 14 | 6 | **8** | 62 | 1 |
| `SuccessCriterion` | 8 | 7 | 0 | **7** | 61 | 1 |
| `AttackPredicate` | 6 | 6 | 0 | **6** | 40 | 0 |
| `CostModification` | 12 | 12 | 6 | **6** | 7 | 0 |
| `SpellCostTarget` | 9 | 9 | 3 | **6** | 8 | 0 |
| `ActivationRestriction` | 10 | 10 | 5 | **5** | 76 | 0 |
| `CollectionFilter` | 6 | 5 | 0 | **5** | 22 | 1 |
| `KeywordAbility` | 39 | 39 | 34 | **5** | 20 | 0 |
| `ManaRestriction` | 23 | 21 | 16 | **5** | 7 | 2 |
| `ManaSpellRider` | 4 | 4 | 0 | **4** | 8 | 0 |
| `CounterDestination` | 4 | 3 | 0 | **3** | 11 | 1 |
| `DelayedTriggerExpiry` | 4 | 3 | 0 | **3** | 6 | 1 |
| `MayPlayExpiry` | 6 | 5 | 2 | **3** | 3 | 1 |
| `NumberProperty` | 4 | 3 | 0 | **3** | 3 | 1 |
| `PayCost` | 4 | 4 | 1 | **3** | 6 | 0 |
| `TargetRequirement` | 11 | 10 | 7 | **3** | 55 | 1 |
| `CounterCondition` | 3 | 2 | 0 | **2** | 41 | 1 |
| `CounterTarget` | 3 | 2 | 0 | **2** | 10 | 1 |
| `FeasibilityCheck` | 2 | 2 | 0 | **2** | 42 | 0 |
| `IterationSpace` | 5 | 5 | 3 | **2** | 43 | 0 |
| `PlayerRankMetric` | 2 | 2 | 0 | **2** | 3 | 0 |
| `Recipient` | 4 | 4 | 2 | **2** | 18 | 0 |
| `RepeatCondition` | 2 | 2 | 0 | **2** | 8 | 0 |
| `WardCost` | 9 | 2 | 0 | **2** | 20 | 7 |
| `AmountFilter` | 4 | 1 | 0 | **1** | 1 | 3 |
| `CardDestination` | 2 | 2 | 1 | **1** | 1 | 0 |
| `CardMeasure` | 2 | 1 | 0 | **1** | 1 | 1 |
| `CastRestriction` | 3 | 3 | 2 | **1** | 2 | 0 |
| `CostGating` | 3 | 2 | 1 | **1** | 7 | 1 |
| `CounterTargetSource` | 2 | 1 | 0 | **1** | 9 | 1 |
| `DamagePredicate` | 1 | 1 | 0 | **1** | 1 | 0 |
| `DamageType` | 3 | 2 | 1 | **1** | 14 | 1 |
| `Gate` | 6 | 5 | 4 | **1** | 115 | 1 |
| `PlotCostTarget` | 1 | 1 | 0 | **1** | 1 | 0 |
| `RetargetChooser` | 2 | 1 | 0 | **1** | 1 | 1 |
| `Scope` | 5 | 3 | 2 | **1** | 4 | 2 |
| `SelectionMode` | 6 | 6 | 5 | **1** | 1 | 0 |
| `UnlockCostTarget` | 1 | 1 | 0 | **1** | 1 | 0 |
| `PreventionSourceFilter` | 3 | 2 | 2 | **0** | 0 | 1 |
| `ProtectionScope` | 13 | 0 | 0 | **0** | 0 | 13 |
| `TimingRule` | 3 | 2 | 2 | **0** | 0 | 1 |

\* sum over gap subtypes of distinct cards using each — a card using two gap subtypes counts twice.



## Keyword gaps

`Keyword` values used by hand-written cards that no Assay-read line produces. Ability-word markers (see above) are included for completeness.

| keyword | cards | examples |
|---|--:|---|
| `PREPARED` | 40 | Blossom-Blessed Angel; Carnivorous Cultivator; Diviner of Victory; Emergency Phytomedic |
| `MAX_SPEED` | 34 | Aether Syphon; Amonkhet Raceway; Avishkar Raceway; Burnout Bashtronaut |
| `BARGAIN` | 20 | Agatha's Champion; Archon's Glory; Back for Seconds; Beseech the Mirror |
| `CRAFT` | 19 | Braided Net; Clay-Fired Bricks; Dire Flail; Idol of the Deep King |
| `JOB_SELECT` | 16 | Astrologian's Planisphere; Bard's Bow; Black Mage's Rod; Dark Knight's Greatsword |
| `EERIE` | 15 | Balemurk Leech; Cult Healer; Dashing Bloodsucker; Entity Tracker |
| `VIVID` | 13 | Aurora Awakener; Bloom Tender; Explosive Prodigy; Glister Bairn |
| `DREDGE` | 12 | Darkblast; Golgari Brownscale; Golgari Grave-Troll; Golgari Thug |
| `RENEW` | 12 | Adorned Crocodile; Agent of Kotis; Alchemist's Assistant; Champion of Dusan |
| `SOULSHIFT` | 12 | Burr Grafter; Gibbering Kami; He Who Hungers; Hundred-Talon Kami |
| `TEAMWORK` | 12 | Atlantis Attacks; Cruel Alliance; Earth's Mightiest Heroes; Go Nuts! |
| `CHAMPION` | 9 | Boggart Mob; Changeling Berserker; Changeling Hero; Changeling Titan |
| `FLURRY` | 9 | Cori Mountain Stalwart; Cori-Steel Cutter; Devoted Duelist; Equilibrium Adept |
| `STORIED` | 9 | Balin, Loremaster; Bifur, Melodic Rider; Bombur, Gentle Dreamer; Dáin, Lord of the Iron Hills |
| `ENDURING` | 5 | Enduring Courage; Enduring Curiosity; Enduring Innocence; Enduring Tenacity |
| `PARADIGM` | 5 | Decorum Dissertation; Echocasting Symposium; Germination Practicum; Improvisation Capstone |
| `FOR_MIRRODIN` | 4 | Barbed Batterfist; Blade of Shared Souls; Bladehold War-Whip; Vulshok Splitter |
| `ESCAPE` | 2 | Phlage, Titan of Fire's Fury; Ox of Agonas |
| `REPLICATE` | 2 | Consign to Memory; Reiterating Bolt |
| `BESTOW` | 1 | Triton Wavebreaker |
| `COMPLEATED` | 1 | Nissa, Ascended Animist |
| `EVOLVE` | 1 | Pollywog Prodigy |
| `NONBASIC_LANDWALK` | 1 | Concerted Effort |
| `PROTECTION_FROM_EACH_OPPONENT` | 1 | Figure of Fable |
| `RAVENOUS` | 1 | Jacked Rabbit |
| `SPLIT_SECOND` | 1 | Samut, Tyrant of Naktamun |
| `UMBRA_ARMOR` | 1 | Lion Umbra |


## Enum gaps

Enum values cards use that Assay never produces (`Keyword`, `Rarity`, `CardLayout`, `Color`, `DeckFormat` excluded).

| enum | values | used by cards | read by Assay | **gap** | gap values (cards) |
|---|--:|--:|--:|--:|---|
| `TurnTracker` | 39 | 35 | 5 | **30** | `PLAYER_ATTACKED` (24), `DESCENDED` (12), `NONLAND_PERMANENTS_ENTERED` (11), `CARDS_DRAWN` (5), `CARDS_LEFT_GRAVEYARD` (3), `DAMAGE_RECEIVED` (3), `LANDS_ENTERED_UNDER_CONTROL` (3), `OPPONENTS_WHO_LOST_LIFE` (3), `PERMANENTS_SACRIFICED` (3), `ARTIFACTS_DIED` (2), `ARTIFACT_SACRIFICED` (2), `CARDS_PUT_INTO_EXILE` (2), `CREATURE_CARDS_PUT_INTO_GRAVEYARD` (2), `SCRIED_OR_SURVEILED` (2), `CARDS_IN_HAND_AT_TURN_START` (1), `CARDS_PUT_INTO_GRAVEYARD_FROM_LIBRARY` (1), `COUNTERS_PUT_ON_CREATURE` (1), `CREATURES_ENTERED_UNDER_CONTROL` (1), `CREATURES_LEFT_BATTLEFIELD` (1), `DAMAGE_RECEIVED_FROM_ARTIFACTS` (1), `DAMAGE_SOURCES` (1), `DEALT_COMBAT_DAMAGE_SINCE_YOUR_LAST_TURN` (1), `DEALT_NONCOMBAT_DAMAGE` (1), `DEALT_NONCOMBAT_DAMAGE_LAST_TURN` (1), `DISTINCT_BENDS` (1), `FOOD_SACRIFICED` (1), `LIFE_LOST_AMOUNT` (1), `LOYALTY_ABILITIES_ACTIVATED` (1), `OPPONENT_CREATURES_EXILED` (1), `PERMANENTS_PUT_INTO_GRAVEYARD_FROM_BATTLEFIELD` (1) |
| `ContextPropertyKey` | 24 | 24 | 1 | **23** | `MANA_SPENT_ON_TRIGGERING_SPELL` (17), `LAST_KNOWN_PLUS_ONE_COUNTER_COUNT` (9), `PREVENTED_DAMAGE_AMOUNT` (8), `TRIGGER_LIFE_GAINED` (6), `TRIGGER_DISCARD_COUNT` (5), `TRIGGER_LIFE_LOST` (5), `TRIGGER_COUNTERS_PLACED_AMOUNT` (4), `LAST_KNOWN_TOTAL_COUNTER_COUNT` (3), `LINKED_EXILE_CARD_COUNT` (3), `TRIGGERING_SPELL_MANA_VALUE` (3), `TRIGGER_SCRY_COUNT` (3), `X_VALUE_OF_TRIGGERING_SPELL` (2), `ADDITIONAL_COST_EXILED_COUNT` (1), `COLORS_SPENT_ON_TRIGGERING_SPELL` (1), `DIED_BATCH_TOTAL_POWER` (1), `LINKED_EXILE_DISTINCT_CARD_TYPE_COUNT` (1), `MODES_CHOSEN_ON_TRIGGERING_SPELL` (1), `TARGETS_TOTAL_MANA_VALUE` (1), `TARGET_COUNT` (1), `TRIGGER_COUNTERS_REMOVED_AMOUNT` (1), `TRIGGER_DISCOVER_VALUE` (1), `TRIGGER_EXCESS_DAMAGE_AMOUNT` (1), `TRIGGER_RECIPIENT_TOUGHNESS` (1) |
| `ChoiceSlot` | 24 | 11 | 1 | **10** | `TEAMWORK` (10), `EVIDENCE_COLLECTED` (7), `GIFT_PROMISED` (3), `CHOSEN_NUMBER` (2), `CONVOKED_CREATURES` (2), `ADDITIONAL_COST_BRANCH` (1), `BLIGHT_AMOUNT` (1), `COLOR` (1), `CREATURE_TYPE` (1), `WEB_SLUNG_RETURNED_MV` (1) |
| `Aggregation` | 13 | 10 | 2 | **8** | `DISTINCT_TYPES` (26), `DISTINCT_COLORS` (17), `SUM` (11), `DISTINCT_NAMES` (6), `DISTINCT_VALUES` (5), `DISTINCT_COLOR_PAIRS` (1), `DISTINCT_COUNTER_TYPES` (1), `DISTINCT_PLANESWALKER_SUBTYPES` (1) |
| `Chooser` | 9 | 7 | 2 | **5** | `Opponent` (17), `SourceController` (8), `ControllerOfSelection` (6), `ControllerOfTarget` (5), `DefendingPlayer` (3) |
| `DonorCards` | 4 | 4 | 0 | **4** | `LinkedExile` (3), `AllGraveyards` (1), `CraftMaterials` (1), `YourGraveyard` (1) |
| `FaceDownMode` | 5 | 4 | 0 | **4** | `MANIFEST` (27), `HIDDEN` (25), `CLOAK` (5), `MORPH` (1) |
| `BendType` | 4 | 3 | 0 | **3** | `EARTH` (27), `FIRE` (24), `AIR` (10) |
| `CopyRecipient` | 3 | 3 | 0 | **3** | `TARGET_CONTROLLER` (3), `AFFECTED_PLAYER` (1), `TARGET_PLAYER` (1) |
| `DelayedTriggerTiming` | 4 | 3 | 0 | **3** | `NextEndStep` (2), `NextTurn` (2), `ThisTurnOnly` (2) |
| `LibraryChoicePosition` | 3 | 3 | 0 | **3** | `Bottom` (4), `SecondFromTop` (3), `Top` (1) |
| `Phase` | 3 | 3 | 0 | **3** | `POSTCOMBAT_MAIN` (5), `PRECOMBAT_MAIN` (5), `COMBAT` (2) |
| `TargetChooser` | 4 | 3 | 0 | **3** | `Opponent` (4), `ControllerOfTriggeringEntity` (1), `TriggeringPlayer` (1) |
| `TurnPart` | 3 | 3 | 0 | **3** | `COMBAT_PHASE` (1), `DRAW_STEP` (1), `MAIN_PHASE` (1) |
| `AbilityFlag` | 4 | 4 | 2 | **2** | `MAY_NOT_UNTAP` (24), `ASSIGNS_COMBAT_DAMAGE_AS_ABSOLUTE_POWER` (1) |
| `AfterResolveDestination` | 2 | 2 | 0 | **2** | `EXILE` (10), `BOTTOM_OF_LIBRARY` (2) |
| `AttachmentKind` | 3 | 2 | 0 | **2** | `EQUIPMENT` (6), `AURA` (1) |
| `BattlefieldDirection` | 2 | 2 | 0 | **2** | `Entering` (1), `Leaving` (1) |
| `CardType` | 10 | 7 | 5 | **2** | `INSTANT` (1), `SORCERY` (1) |
| `ComparisonOperator` | 6 | 5 | 3 | **2** | `EQ` (62), `LT` (5) |
| `ExploreReveal` | 3 | 2 | 0 | **2** | `LAND` (1), `NONLAND` (1) |
| `FaceDownLookScope` | 2 | 2 | 0 | **2** | `SINGLE_TARGET` (2), `ALL_CONTROLLED_BY_TARGET_PLAYER` (1) |
| `OnceOnlyAbilityKind` | 2 | 2 | 0 | **2** | `EXHAUST` (1), `POWER_UP` (1) |
| `OptionType` | 4 | 3 | 1 | **2** | `CARD_NAME` (10), `BASIC_LAND_TYPE` (5) |
| `RedirectScope` | 3 | 2 | 0 | **2** | `CONTINUOUS` (2), `NEXT_BATCH` (1) |
| `Zone` | 8 | 8 | 6 | **2** | `Sideboard` (27), `Command` (2) |
| `CardNumericProperty` | 3 | 3 | 2 | **1** | `TOUGHNESS` (7) |
| `ChoiceType` | 8 | 8 | 7 | **1** | `MODE` (17) |
| `ControlChangeDirection` | 2 | 1 | 0 | **1** | `LOST` (2) |
| `CounterRemovalAmount` | 2 | 1 | 0 | **1** | `EqualToDamage` (1) |
| `CreatureStat` | 2 | 1 | 0 | **1** | `TOUGHNESS` (1) |
| `CrewSaddleCharacteristic` | 2 | 1 | 0 | **1** | `TOUGHNESS` (1) |
| `DamageCounterRecipient` | 2 | 1 | 0 | **1** | `DamagedPermanent` (1) |
| `DayNight` | 2 | 1 | 0 | **1** | `NIGHT` (1) |
| `HijackScope` | 2 | 1 | 0 | **1** | `NextCombatPhase` (1) |
| `LandControllerScope` | 3 | 1 | 0 | **1** | `OPPONENTS` (2) |
| `LookAudience` | 3 | 1 | 0 | **1** | `None` (5) |
| `ManaColorSource` | 2 | 1 | 0 | **1** | `CraftedMaterials` (1) |
| `ManaExpiry` | 2 | 1 | 0 | **1** | `END_OF_COMBAT` (27) |
| `MoveType` | 4 | 3 | 2 | **1** | `Sacrifice` (33) |
| `PlayerRankDirection` | 2 | 1 | 0 | **1** | `LEAST` (1) |
| `RankTieBreak` | 2 | 1 | 0 | **1** | `CONTROLLER_CHOOSES` (1) |
| `ReturnFace` | 3 | 1 | 0 | **1** | `FRONT` (10) |
| `Supertype` | 1 | 1 | 0 | **1** | `LEGENDARY` (10) |
| `TapReason` | 2 | 1 | 0 | **1** | `TEAMWORK` (1) |
| `TappedForManaType` | 3 | 1 | 0 | **1** | `COLORLESS` (1) |
| `ZoneChangeCause` | 2 | 1 | 0 | **1** | `DiscardedByOpponentEffect` (1) |


## Axis gaps on types Assay already produces

Fields that hand-written cards set on a subtype Assay does emit, but that Assay never sets — the "missing axis on an existing type" shape, usually the cheapest grammar work. Sorted by total usage.

| subtype Assay emits | fields cards set that Assay never sets (cards) |
|---|---|
| `CreateToken` | `imageUri` (597), `artifactToken` (67), `name` (54), `staticAbilities` (49), `triggeredAbilities` (31), `controller` (30), `attacking` (29), `dynamicPower` (28), `dynamicToughness` (28), `sacrificeAtStep` (18), `activatedAbilities` (15), `legendary` (12), `enchantmentToken` (6), `exileAtStep` (4), `initialCounters` (2), `stampCreator` (2), `colorsFromChoice` (1), `creatureTypesFromChoice` (1), `numericKeywords` (1) |
| `Gated` | `descriptionOverride` (240), `otherwise` (197), `decisionMaker` (18) |
| `MoveCollection` | `storeMovedAs` (78), `faceDown` (56), `filter` (13), `markEnteredViaSourceAbility` (6), `revealToSelf` (5), `addCounterType` (4), `attachTo` (2), `lookableInExile` (2), `unlinkFromSource` (2) |
| `DealDamage` | `damageSource` (136), `cantBePrevented` (3), `excessDamageVariable` (2), `excessToController` (1) |
| `Composite` | `descriptionOverride` (111), `steps` (6), `descriptionAmounts` (1) |
| `CreateDelayedTrigger` | `trigger` (44), `watchedTarget` (24), `fireOnce` (15), `fireOnPlayer` (11), `expiry` (6), `timing` (6), `targetRequirement` (4), `additionalTargetRequirements` (1), `carryCollections` (1), `repeatAtEachMatchingStep` (1), `watchedRecipient` (1) |
| `GrantTriggeredAbility` | `target` (69), `duration` (36) |
| `Exists` | `negate` (65), `excludeSelf` (26) |
| `BattlefieldMatching` | `player` (79), `excludeChosenTargets` (3), `excludeTriggering` (3), `includeAttachments` (2) |
| `SelectFromCollection` | `alwaysPrompt` (59), `restrictions` (28) |
| `AttackEvent` | `requires` (40), `filter` (34) |
| `ConditionalOnCollection` | `filter` (39), `ifEmpty` (32), `minSize` (2), `countDistinctCardTypes` (1) |
| `Counter` | `condition` (41), `counterDestination` (11), `target` (10), `targetSource` (8) |
| `GrantMayPlayFromExile` | `withAnyManaType` (15), `ownerControls` (12), `fixedAlternativeManaCost` (10), `condition` (8), `nonLandOnly` (4), `insteadOfGraveyard` (3), `onPlayRider` (2), `recipient` (2), `asThoughFlash` (1), `castColorRestriction` (1), `castFaceIndex` (1), `fixedAlternativeCostIsManaValue` (1), `landEntersTapped` (1), `singleUse` (1), `waterbend` (1) |
| `EntersWithCounters` | `condition` (51), `appliesTo` (3), `otherOnly` (2) |
| `Modal` | `dynamicChooseCount` (16), `additionalManaCostPerExtraMode` (14), `excludePreviouslyChosenModes` (8), `dynamicMinChooseCount` (7), `additionalCostPerExtraMode` (3), `allowRepeat` (3), `excludeModesChosenThisTurn` (3), `chooseAllIfBlightPaid` (1) |
| `DealsDamageEvent` | `sourceFilter` (43), `batch` (4), `requireExcess` (2), `requires` (1) |
| `AddManaOfChoice` | `colorSet` (43), `recipient` (3), `riders` (2), `colorChosenByRecipient` (1) |
| `MoveToZone` | `linkToSource` (26), `addCounterType` (11), `positionFromTop` (8), `faceDown` (2) |
| `BecomeCreature` | `duration` (33), `dynamicPower` (4), `dynamicToughness` (4), `imageUri` (2), `removeTypes` (1) |
| `CreatePredefinedToken` | `controller` (19), `tapped` (18) |
| `TargetObject` | `sameOwner` (11), `differentControllers` (6), `chooser` (5), `sameController` (4), `totalManaValueAtMost` (4), `sameCreatureType` (3), `differentNames` (1), `onePerCardType` (1), `sameCardType` (1) |
| `AddMana` | `expiry` (27), `riders` (3) |
| `AnyTarget` | `count` (10), `minCount` (10), `chooser` (1), `descriptionOverride` (1), `filter` (1), `optional` (1) |
| `EntersWithChoice` | `modeOptions` (17), `excludedColors` (5), `allowedCreatureTypes` (2) |
| `TargetPlayer` | `unlimited` (8), `optional` (6), `count` (4), `descriptionOverride` (4), `restriction` (2) |
| `PayOrSuffer` | `player` (13), `consequenceDescription` (10) |
| `BecomesTargetEvent` | `byOpponent` (9), `targetFilter` (8), `abilitiesOnly` (1), `backupAbilitiesOnly` (1), `includePlayerTargets` (1), `includeSpellTargets` (1), `sourceFilter` (1) |
| `FilterCollection` | `collectionFilter` (22) |
| `CantAttack` | `filter` (17), `duration` (2) |
| `PreventDamageShield` | `nextInstanceOnly` (5), `onPrevented` (5), `duration` (3), `gainLifeFromPrevented` (2), `gainLifeFromColors` (1), `halvePreventedDamage` (1), `preventDamage` (1), `toPlayersOnly` (1) |
| `RevealCollection` | `revealToSelf` (9), `fromZone` (6), `toZone` (4) |
| `ShuffleLibrary` | `target` (19) |
| `ModifyStats` | `duration` (18) |
| `Sacrifice` | `excludeSource` (12), `any` (5), `count` (1) |
| `Divide` | `roundUp` (13) |
| `EntersWithDynamicCounters` | `otherOnly` (7), `appliesTo` (4), `activeZones` (1) |
| `ChooseOption` | `prompt` (7), `cardNamePool` (2), `excludedOptions` (2) |
| `GrantFlashToSpellType` | `controllerOnly` (9), `nthOfTypePerTurn` (1) |
| `LifeGainEvent` | `player` (7), `firstTimeEachTurn` (3) |
| `GrantActivatedAbility` | `target` (8), `duration` (1) |
| `MustBeBlocked` | `allCreatures` (9) |
| `TapEvent` | `tapper` (4), `batch` (3), `firstTimeEachTurn` (1), `reason` (1) |
| `AtomRemoveCounters` | `filter` (8) |
| `BlockEvent` | `attackerFilter` (3), `filter` (3), `batch` (1), `minBlockedAttackers` (1) |
| `DiscardEvent` | `player` (6), `cardFilter` (2) |
| `FlipCoin` | `wonEffect` (8) |
| `RedirectNextDamage` | `amount` (3), `scope` (3), `creaturesOnly` (1), `optional` (1) |
| `ForEach` | `collectCollections` (7) |
| `AtomReturnToHand` | `count` (5), `youControl` (1) |
| `DividedDamage` | `maxTargets` (5), `dynamicTotal` (1) |
| `RedirectZoneChange` | `reveal` (2), `shuffleIntoLibrary` (2), `linkToSource` (1), `requiredCause` (1) |
| `CountersPlacedEvent` | `firstTimeEachTurn` (3), `batch` (1), `includePlayers` (1) |
| `GatherCards` | `lookAudience` (5) |
| `ReturnSelfFromZoneTransformed` | `tapped` (5) |
| `SetCreatureSubtypes` | `subtypes` (5) |
| `TakeExtraTurn` | `target` (4), `powerUpAbilitiesCantBeActivated` (1) |
| `AttackTax` | `condition` (2), `coversPlaneswalkers` (2) |
| `CantBlockTargetCreatures` | `duration` (3), `attacker` (1) |
| `FromMultipleZones` | `player` (4) |
| `LifeLossEvent` | `player` (4) |
| `LoseGame` | `message` (4) |
| `PreventFromChosen` | `eligible` (4) |
| `AggregateBattlefield` | `counterType` (2), `excludeTriggeringEntity` (1) |
| `AtomExileFrom` | `anyPlayersZone` (1), `excludeSelf` (1), `singleZone` (1) |
| `BecomeCreatureType` | `excludedTypes` (2), `duration` (1) |
| `GreatestAmongPlayers` | `players` (3) |
| `PermanentsSacrificedEvent` | `sacrificedBy` (3) |
| `ZoneChangeEvent` | `excludeSacrifice` (1), `excludeTo` (1), `requireCraftMaterial` (1) |
| `AddColorlessMana` | `riders` (2) |
| `ChangeTarget` | `newTargetMustBePlayer` (1), `onlyIfCurrentTargetIsController` (1) |
| `CreaturesAttackYouEvent` | `includePlaneswalkersYouControl` (1), `minAttackers` (1) |
| `FromZone` | `excludeSacrificedThisWay` (2) |
| `Numeric` | `keyword` (1), `n` (1) |
| `Power` | `base` (1), `exponent` (1) |
| `RemoveKeyword` | `duration` (2) |
| `SacrificeTarget` | `sacrificedByItsController` (2) |
| `SkipNextTurn` | `count` (1), `target` (1) |
| `SkipUntap` | `affectsLands` (2) |
| `UntapEvent` | `batch` (1), `filter` (1) |
| `AtomSacrifice` | `distinctNames` (1) |
| `ChooseNumberThen` | `minValue` (1) |
| `DamageReceivedEvent` | `source` (1) |
| `FromLinkedExile` | `count` (1) |
| `GrantCantBeCountered` | `includesAbilities` (1) |
| `GrantKeyword` | `condition` (1) |
| `LandPlayedEvent` | `fromZoneOtherThan` (1) |
| `LinkedExiledCard` | `index` (1) |
| `ModifyLifeGain` | `restrictions` (1) |
| `OneOrMoreDealCombatDamageToPlayerEvent` | `orBattle` (1) |
| `PayManaCost` | `waterbend` (1) |
| `TargetOpponent` | `unlimited` (1) |
| `TargetPlayerOrPlaneswalker` | `optional` (1) |


## Vocabulary no card uses

Subtypes neither the corpus nor Assay produces (inflated by default values — see caveats).

- **`ProtectionScope`** (13): `ProtectionScope.ActivatedAbilities`, `ProtectionScope.CardType`, `ProtectionScope.Color`, `ProtectionScope.Colors`, `ProtectionScope.EachOpponent`, `ProtectionScope.Everything`, `ProtectionScope.Multicolored`, `ProtectionScope.NonColor`, `ProtectionScope.PermanentsCastThisTurn`, `ProtectionScope.Spells`, `ProtectionScope.Subtype`, `ProtectionScope.Supertype`, `ProtectionScope.TriggeredAbilities`
- **`Effect`** (13): `BecomeRenowned`, `CreateRandomCreatureTokenWithManaValue`, `EmitClashedEvent`, `EmitConnivedEvent`, `EmitDiscoveredEvent`, `EmitExploredEvent`, `GainCitysBlessing`, `LevelUpClass`, `MarkEnduringReturn`, `MoveTrackedBattlefieldObject`, `StormCopy`, `WardCounter`, `WarpExile`
- **`EventPattern`** (9): `BecameRenownedEvent`, `CardPlayedFromPermissionEvent`, `CounterSpellEvent`, `DamagePreventedEvent`, `DrawCardsEvent`, `ExtraTurnEvent`, `LifePaymentEvent`, `StateConditionMetEvent`, `SurveiledEvent`
- **`Condition`** (9): `BeforeAttackersDeclaredThisTurn`, `CounterRemovedFromPermanentYouControlledThisTurn`, `IsDay`, `OpponentSpellOnStack`, `PermanentWithCounterPutIntoGraveyardThisTurn`, `SourceForetoldOnPriorTurn`, `SourcePlottedOnPriorTurn`, `YouDiscardedThisCardThisTurn`, `YouWereAttackedThisStep`
- **`WardCost`** (7): `WardCost.Choice`, `WardCost.CollectEvidence`, `WardCost.Composite`, `WardCost.Discard`, `WardCost.DynamicLife`, `WardCost.PlayerCounters`, `WardCost.Sacrifice`
- **`SingleEntity`** (7): `AttachedToTriggeringPermanent`, `ChosenCreature`, `EnchantedPermanent`, `GrantingSource`, `SpecificEntity`, `TappedAsCost`, `TargetingSource`
- **`EffectTarget`** (5): `AffectedEntity`, `FilteredTarget`, `RingBearer`, `SacrificedAsCost`, `SpecificEntity`
- **`Duration`** (4): `NextUse`, `UntilCondition`, `UntilPhase`, `WhileControlledByController`
- **`AmountFilter`** (3): `AmountAny`, `AmountAtLeast`, `AmountExactly`
- **`ManaRestriction`** (2): `AllOf`, `AnySpend`
- **`Scope`** (2): `Battlefield`, `Specific`
- **`CostGating`** (1): `None`
- **`CostReductionSource`** (1): `CreaturesYouControl`
- **`ReplacementEffect`** (1): `ModifyTokenCount`
- **`TimingRule`** (1): `InstantSpeed`
- **`NumberProperty`** (1): `MultipleOf`
- **`CardMeasure`** (1): `MeasureManaValue`
- **`CollectionFilter`** (1): `ExcludeEntity`
- **`CounterCondition`** (1): `CounterCondition.Always`
- **`CounterDestination`** (1): `CounterDestination.Graveyard`
- **`CounterTargetSource`** (1): `CounterTargetSource.Chosen`
- **`CounterTarget`** (1): `CounterTarget.Spell`
- **`DelayedTriggerExpiry`** (1): `DelayedTriggerExpiry.EndOfTurn`
- **`Gate`** (1): `Gate.OnceEachTurn`
- **`MayPlayExpiry`** (1): `EndOfTurn`
- **`PreventionSourceFilter`** (1): `AnySource`
- **`RetargetChooser`** (1): `RetargetChooser.Controller`
- **`SuccessCriterion`** (1): `SuccessCriterion.Auto`
- **`DamageType`** (1): `DamageAny`
- **`CardPredicate`** (1): `ToughnessEquals`
- **`ControllerPredicate`** (1): `ControllerOr`
- **`TargetRequirement`** (1): `TargetCreatureOrPlayer`
- **`EntityNumericProperty`** (1): `BaseToughness`
- **`ManaColorSet`** (1): `ManaColorSet.AnyColor`


## Method

A throwaway Kotest spec in `:oracle-assay` (not committed) did two walks, both guided by the SDK's own
kotlinx-serialization descriptors from `CardDefinition.serializer()`:

- **Vocabulary** — every sealed subtype (`"type"` discriminator), every enum value, every class
  field reachable from `CardDefinition`.
- **Cards side** — every golden in `mtg-sets/src/test/resources/snapshots/cards/` decoded with
  `ImplementedCorpus` (`CardLoader`), re-encoded with `CardSerialization.json`, walked; counted as
  distinct card names per value.
- **Assay side** — `Touchstone().assay(card)` over the whole Scryfall Oracle bulk; for every line
  whose verdict is `ROUND_TRIP` or `VARIANT`, the `CardFragment`'s `script`, `keywordAbilities`
  (plus their `Keyword`s), `flags`, `equipCost` and dynamic P/T were encoded and walked. Per-line,
  not per-card: a value counts as "produced" if *any* line of *any* card reads into it, even when the
  rest of that card declines. (Line verdicts: 28,502 round-trip, 2,001 variant, 34,301 declined.)

Caveats:

- `encodeDefaults = false`, so a value equal to its field's default is invisible on both sides — e.g.
  `Duration.EndOfTurn`, `CounterCondition.Always`, `TimingRule.InstantSpeed`. The "used by neither"
  column is therefore an over-count of dead vocabulary, and a gap in a *default* value is not
  measurable this way.
- "Cards blocked" is how many cards *use* the value, not how many a new grammar row would finish;
  per the Assay README, run the tail ranking and a `PrefixProbe` before sizing a band.
- Field-level "axis gaps" only look at types Assay already produces; a field is counted when present
  in the encoded JSON (i.e. non-default).



## Appendix — full gap list per family

Every subtype hand-written cards use that Assay never produces. Examples are up to four card names.


### `Effect` — 267 gap subtypes

| subtype | cards | examples |
|---|--:|---|
| `ReflexiveTrigger` | 113 | Generous Plunderer; Alania, Divergent Storm; Curious Forager; Wick's Patrol |
| `CreateTokenCopyOfTarget` | 75 | Esoteric Duplicator; Molten Duplication; Nexus of Becoming; Oltec Matterweaver |
| `AddCountersToCollection` | 71 | Kastral, the Windcrested; Oakhollow Village; Scavenger's Talent; Wick, the Whorled Mind |
| `GrantTriggeredAbility` | 69 | Cruel Deceiver; Sygg, Wanderwine Wisdom; Perigee Beckoner; Requiem Monolith |
| `EmitBendEvent` | 61 | Aang, Swift Savior; Aang, at the Crossroads; Aang, the Last Airbender; Airbender Ascension |
| `GainControl` | 61 | Aladdin; Old Man of the Sea; Zealous Conscripts; Reptilian Recruiter |
| `ExileUntilLeaves` | 50 | Stasis Snare; Driftgloom Coyote; Detention Chariot; Perilous Snare |
| `Fight` | 47 | Hivespine Wolverine; Longstalk Brawl; Grothama, All-Devouring; Bushwhack |
| `RemoveCounters` | 46 | Armageddon Clock; Clockwork Avian; Darigaaz Reincarnated; Cursed Recording |
| `TheRingTemptsYou` | 45 | Bilbo, Retired Burglar; Birthday Escape; Bombadil's Song; Boromir, Warden of the Tower |
| `ChooseOpponentForSource` | 43 | Blooming Blast; Coiling Rebirth; Consumed by Greed; Cruelclaw's Heist |
| `CastFromCollectionWithoutPayingCost` | 42 | Wishing Well; Shell of the Last Kappa; Bre of Clan Stoutarm; Goliath Daydreamer |
| `Amass` | 41 | Along the Crooked Way; Azog, Moria's Ruin; Bolg of the North; Bothersome Noisemaker |
| `GatherUntilMatch` | 37 | Clifftop Lookout; The Infamous Cruelclaw; Reweave; Skyserpent Seeker |
| `SetBaseStats` | 37 | Island of Wak-Wak; Singing Tree; Sorceress Queen; Azure Beastbinder |
| `CopyTargetSpell` | 35 | Alania, Divergent Storm; Kitsa, Otterball Elite; Uyo, Silent Prophet; Mendicant Core, Guidelight |
| `CreateRoleToken` | 35 | Asinine Antics; Become Brutes; Besotted Knight; Charmed Clothier |
| `CreateTokenCopyOfSource` | 32 | Vaultborn Tyrant; Bushy Bodyguard; Coruscation Mage; Darkstar Augur |
| `SelectTarget` | 32 | Season of Gathering; Season of the Burrow; Gastal Blockbuster; Settle the Score |
| `AnimateLand` | 29 | Lifespark Spellbomb; Kamahl, Fist of Krosa; Aang, at the Crossroads; Ba Sing Se |
| `ChooseAction` | 28 | Bushy Bodyguard; Corpseberry Cultivator; Curious Forager; Rottenmouth Viper |
| `EmitManifestedDreadEvent` | 27 | Abhorrent Oculus; Bashful Beastie; Break Down the Door; Conductive Machete |
| `ChooseColorThen` | 26 | Blessed Breath; Kami of the Painted Road; Addle; Armored Guardian |
| `MarkExileOnDeath` | 26 | Agate Assault; Obliterating Bolt; Nine-Ringed Bo; Yamabushi's Flame |
| `BecomePrepared` | 25 | Bloodline Recollector; Codie, Ravenous Codex; Hexhaven Dueling Arena; Paradox Shaper |
| `AttachTargetEquipmentToCreature` | 23 | Blacksmith's Talent; Beatrix, Loyal General; Gilgamesh, Master-at-Arms; Raubahn, Bull of Ala Mhigo |
| `Discover` | 23 | Brass's Tunnel-Grinder; Buried Treasure; Caparocti Sunborn; Chimil, the Inner Sun |
| `GiftGiven` | 23 | Blooming Blast; Coiling Rebirth; Consumed by Greed; Cruelclaw's Heist |
| `GrantStaticAbility` | 22 | Deep Water; Runesword; Tower of Coireall; Walk-In Closet // Forgotten Cellar |
| `PutOnLibraryPositionOfChoice` | 21 | Dire Downdraft; Trip Up; Vanish from Sight; Swat Away |
| `Clash` | 20 | Adder-Staff Boggart; Bog Hoodlums; Broken Ambitions; Captivating Glance |
| `Connive` | 20 | Copycrook; Spymaster's Vault; A.I.M. Scientists; Baron Helmut Zemo |
| `EachPermanentBecomesCopyOfTarget` | 20 | Mimeoplasm, Revered One; Niko, Light of Hope; Silent Hallcreeper; Mirrorform |
| `AddCreatureType` | 19 | Sensei Golden-Tail; Ghost Vacuum; Jump Scare; Possessed Goat |
| `ReturnSelfFromExileTransformed` | 19 | Braided Net; Clay-Fired Bricks; Dire Flail; Idol of the Deep King |
| `DistributeCountersAmongTargets` | 18 | Jugan, the Rising Star; Cloudspire Skycycle; Omnivorous Flytrap; The Earth Crystal |
| `ExileAndReturnTransformed` | 18 | Clive, Ifrit's Dominant; Crystal Fragments; Dion, Bahamut's Dominant; Jecht, Reluctant Guardian |
| `GrantPlayWithoutPayingCost` | 18 | Collector's Cage; The Infamous Cruelclaw; Evercoat Ursine; Dream Harvest |
| `TapUntapCollection` | 18 | Fabled Passage; Teferi, Hero of Dominaria; Unwind; Orphans of the Wheat |
| `AddCombatPhase` | 17 | Godo, Bandit Warlord; Full Throttle; Fear of Missing Out; Balthier and Fran |
| `CreateGlobalTriggeredAbility` | 17 | Season of the Bold; Chandra, Spark Hunter; Teferi, Hero of Dominaria; Tezzeret, Cruel Captain |
| `Explore` | 17 | Amalia Benavides Aguirre; Cenote Scout; Deepfathom Echo; Defossilize |
| `StoreNumber` | 17 | Tyvar, the Pummeler; Spry and Mighty; Taster of Wares; Ouroboroid |
| `GiveControlToTargetPlayer` | 16 | Wishclaw Talisman; Harmless Offering; Rainbow Vale; Kain, Traitorous Dragoon |
| `MoveAllLastKnownCounters` | 16 | Essence Channeler; Reluctant Role Model; Servant of the Scale; Broodguard Elite |
| `Suspect` | 16 | Absolving Lammasu; Agrus Kos, Spirit of Justice; Barbed Servitor; Case of the Stashed Skeleton |
| `AddCardType` | 15 | Ashnod's Transmogrant; Alacrian Armory; Chandra, Spark Hunter; Earthrumbler |
| `CopyNextSpellCast` | 14 | Sword of Wealth and Power; Cursed Recording; Rimefire Torque; Ether |
| `IncrementAbilityResolutionCount` | 14 | Harvestrite Host; Victor, Valgavoth's Seneschal; Soulbright Seeker; Tannuk, Memorial Ensign |
| `RemoveAllAbilities` | 14 | Azure Beastbinder; Merfolk Trickster; Abigale, Eloquent First-Year; Curious Colossus |
| `GrantProtectionFromChosenColor` | 13 | Blessed Breath; Kami of the Painted Road; Armored Guardian; Stormscape Master |
| `RepeatDynamicTimes` | 13 | Rottenmouth Viper; Prosperous Bandit; Tempt with Bunnies; Tempt with Discovery |
| `BecomeSolved` | 12 | Case of the Burning Masks; Case of the Crimson Pulse; Case of the Filched Falcon; Case of the Gateway Express |
| `AddSubtype` | 11 | Navigator's Compass; Beorn the Fierce; Olivia Voldaren; Abuelo's Awakening |
| `CastAnyNumberFromCollectionWithoutPayingCost` | 11 | Etali, Primal Storm; The Tale of Tamiyo; Uldaros Theorix; Villainous Wealth |
| `ChoosePile` | 11 | Curator of Destinies; Riddles in the Dark; Bend or Break; Death or Glory |
| `CopyTargetSpellOrAbility` | 10 | Pit Automaton; Gogo, Master of Mimicry; The Enigma Jewel; Rings of Brighthearth |
| `DoubleCounters` | 10 | Byrke, Long Ear of the Law; Omnivorous Flytrap; Zimone, Paradox Sculptor; Sazh Katzroy |
| `Flip` | 10 | Akki Lavarunner; Budoka Gardener; Bushi Tenderfoot; Initiate of Blood |
| `GrantEvasionKeyword` | 10 | Dawn's Truce; Aquitect's Defenses; Airtight Alibi; Fae Flight |
| `WinGame` | 10 | Simic Ascendancy; Twenty-Toed Toad; Maze's End; Central Elevator // Promising Stairs |
| `Cascade` | 9 | Bloodbraid Elf; Wildsear, Scouring Maw; Annoyed Altisaur; Meteoric Mace |
| `EmitChampionedEvent` | 9 | Boggart Mob; Changeling Berserker; Changeling Hero; Changeling Titan |
| `EmitExploitedEvent` | 9 | Diver Skaab; Fell Stinger; Graf Reaver; Mindleech Ghoul |
| `EmitTrainedEvent` | 9 | Apprentice Sharpshooter; Cloaked Cadet; Gryff Rider; Gryffwing Cavalry |
| `PayLife` | 9 | Zoraline, Cosmos Caller; Meathook Massacre II; Voracious Tome-Skimmer; Seymour Flux |
| `Provoke` | 9 | Brontotherium; Crested Craghorn; Deftblade Elite; Feral Throwback |
| `RemoveFromCombat` | 9 | Mijae Djinn; Ydwen Efreet; Bill Ferny, Bree Swindler; Gollum, Scheming Guide |
| `BecomeArtifact` | 8 | Ultima, Origin of Oblivion; Supper for Spiders; Tom, Bert, and William; Kitesail Larcenist |
| `CreatePermanentEmblem` | 8 | Kaito, Bane of Nightmares; Oko, Lorwyn Liege; Tamiyo, Field Researcher; Ajani Resolute |
| `DrainLife` | 8 | Kokusho, the Evening Star; Bloodhunter Bat; Vein Ripper; Dreg Recycler |
| `GrantActivatedAbility` | 8 | Emrakul, the Exigent Doom; Defiling Tears; Scorn-Blade Berserker; Voldaren Thrillseeker |
| `RepeatWhile` | 8 | Struggle for Sanity; Mana Clash; The Tale of Tamiyo; Sin, Spira's Punishment |
| `ChangeColor` | 7 | Eight-and-a-Half-Tails; Tam, Mindful First-Year; Ancient Kavu; Defiling Tears |
| `ChangeColorToChosen` | 7 | Blind Seer; Kavu Chameleon; Rainbow Crow; Sway of Illusion |
| `GrantFreeCastTargetFromExile` | 7 | Daring Waverider; Portent of Calamity; Malcolm, Alluring Scoundrel; Horde of Notions |
| `RemoveAnyNumberOfCounters` | 7 | Rhys, the Evermore; Mabel, Bitter Recluse; Render Inert; Mister Hyde, Monster Within |
| `ReplaceNextDrawWith` | 7 | Aladdin's Lamp; Ring of Ma'rûf; Words of War; Words of Waste |
| `ReturnSelfToBattlefieldAttached` | 7 | Eagle's Rescue; Dragon Breath; Dragon Fangs; Dragon Scales |
| `SetLandType` | 7 | Thelonite Monk; Dream Thrush; Slimy Kavu; Streambed Aquitects |
| `CanAttackDespiteDefenderThisTurn` | 6 | Stalked Researcher; Vodalian War Machine; The Pride of Hull Clade; Krotiq Nestguard |
| `CaptureControllers` | 6 | Martyr's Cry; Broken Ambitions; Builder's Bane; Deadly Cover-Up |
| `CopyCollectionIntoCollection` | 6 | The Tale of Tamiyo; Uldaros Theorix; Isochron Scepter; Spellweaver Helix |
| `ForceBlock` | 6 | Matsu-Tribe Decoy; Rampant Elephant; Avalanche Tusker; Hunt Down |
| `GrantFlashback` | 6 | Sphinx of Forgotten Lore; Stingcaster Mage; Snapcaster Mage; Archmage's Newt |
| `MarkMustAttackThisTurn` | 6 | Howlsquad Heavy; Grizzled Angler; Nettling Imp; Hustle // Bustle |
| `MayRevealCardFromHand` | 6 | Ancient Amphitheater; Auntie's Hovel; Gilt-Leaf Palace; Secluded Glen |
| `RemoveAllCountersOfType` | 6 | Homarid; Tidal Influence; Ashling the Pilgrim; Lightning Coils |
| `StoreCardName` | 6 | Maelstrom Pulse; Lobotomy; Sever the Bloodline; Deadly Cover-Up |
| `AnyPlayerMayPay` | 5 | Cleansing; Aether Rift; Prowling Pangolin; Desecration Demon |
| `BudgetModal` | 5 | Season of Gathering; Season of Loss; Season of Weaving; Season of the Bold |
| `ChainCopy` | 5 | Chain of Acid; Chain of Plasma; Chain of Silence; Chain of Smog |
| `CollectEvidence` | 5 | Evidence Examiner; Izoni, Center of the Web; Lamplight Phoenix; Sample Collector |
| `CopyCardIntoCollection` | 5 | Roving Actuator; Saruman of Many Colors; Reenact the Crime; Kaervek, the Punisher |
| `ExileTargetSpell` | 5 | Shell of the Last Kappa; Spell Queller; Aven Interrupter; Eye of the Storm |
| `ForEachCapturedController` | 5 | Martyr's Cry; Broken Ambitions; Builder's Bane; Deadly Cover-Up |
| `GrantActivatedAbilityToGroup` | 5 | Song of Freyalise; Vorinclex; Psychic Trance; Shade's Breath |
| `GrantToEnchantedCreatureTypeGroup` | 5 | Crown of Ascension; Crown of Awe; Crown of Fury; Crown of Suspicion |
| `HijackNextTurn` | 5 | Emrakul, the Promised End; The Dominion Bracelet; Mindslaver; Construct a Cosmic Cube |
| `PayDynamicManaCost` | 5 | Cyclone; Magnetic Mountain; Transmute Artifact; Thelon's Curse |
| `RemoveAllCounters` | 5 | Perfect Intimidation; Enchanted River's Grasp; Invasion of Fiora; Purging Stormbrood |
| `RemoveSuspected` | 5 | Absolving Lammasu; Airtight Alibi; Deadly Complication; Eliminate the Impossible |
| `SetLifeTotal` | 5 | Torgaar, Famine Incarnate; The Endstone; Arbiter of Knollridge; Biorhythm |
| `CantCastSpells` | 4 | Bilbo's Gambit; Orim's Chant; Xantid Swarm; Flamescroll Celebrant |
| `ChangeTriggeringObjectTargets` | 4 | Sideswipe; Psychic Battle; Wild Ricochet; Speedball, New Warrior |
| `DamageCantBePreventedThisTurn` | 4 | Pyrewood Gearhulk; Fear, Fire, Foes!; Impractical Joke; Alchemist's Gambit |
| `Foraged` | 4 | Bushy Bodyguard; Corpseberry Cultivator; Curious Forager; Treetop Sentries |
| `GrantReplacementEffect` | 4 | Whippoorwill; Walk-In Closet // Forgotten Cellar; Malicious Eclipse; Kaya, Geist Hunter |
| `PairWithSource` | 4 | Deadeye Navigator; Lightning Mauler; Spectral Gateguards; Tandem Lookout |
| `Proliferate` | 4 | High Perfect Morcant; Tam, the Possibility; Powerful Broker; Ichormoon Gauntlet |
| `ReduceSpellCosts` | 4 | Goblin Maskmaker; Armor Wars; Rowan, Scion of War; Will, Scion of Peace |
| `RemoveMaximumHandSize` | 4 | Wrenn and Seven; Wisdom of Ages; Spirit Water Revival; Finale of Revelation |
| `ReturnSpellOrPermanentToOwnersHand` | 4 | Fatehold Charm; Press the Enemy; Divide by Zero; Jeskai Revelation |
| `ReturnSpellToOwnersHand` | 4 | Bilbo's Gambit; Reprieve; Hullbreaker Horror; Spellscorn Coven |
| `UnlockDoor` | 4 | Ghostly Dancers; Ghostly Keybearer; Keys to the House; Marina Vendrell |
| `AddAnyColorManaSpendOnChosenType` | 3 | Cavern of Souls; Eclipsed Realms; Secluded Courtyard |
| `AddMainPhase` | 3 | Aggravated Assault; All-Out Assault; Relentless Assault |
| `AddOneManaOfEachColorAmong` | 3 | Tarnation Vista; Bloom Tender; Sunbird Standard |
| `BecomeSaddled` | 3 | Alacrian Armory; Guidelight Matrix; Kolodin, Triumph Caster |
| `Behold` | 3 | Theorist's Sanctum; Elven Passage; Sarkhan, Dragon Ascendant |
| `CantAttackGroup` | 3 | Festival; Unstable Glyphbridge; Orim's Chant |
| `CantBlockGroup` | 3 | Barrage of Boulders; Temur Charm; Fire of Orthanc |
| `CopyTargetTriggeredAbility` | 3 | Kirol, Attentive First-Year; Mirror-Shield Hoplite; Firebender Ascension |
| `EmitSurveiledEvent` | 3 | Chandra, Chill of Compliance; Enlightened Confidant; Spider-Man Noir |
| `ExileOpponentsGraveyards` | 3 | Phyrexian Scriptures; Dauntless Scrapbot; Soul-Guide Lantern |
| `GainControlByActivePlayer` | 3 | Contested Game Ball; Risky Move; Karona, False God |
| `GainControlByRank` | 3 | Ghazbán Ogre; Loxodon Peacekeeper; Thoughtbound Primoc |
| `GrantKeywordToSpell` | 3 | Spinerock Tyrant; Ojer Pakpatiq, Deepest Epoch; Judith, Carnage Connoisseur |
| `LookAtFaceDown` | 3 | Smoke Teller; Aven Soulgazer; Spy Network |
| `LoseAllCreatureTypes` | 3 | Amoeboid Changeling; Ego Erasure; Nameless Inversion |
| `MakePlotted` | 3 | Jace Reawakened; Kellan Joins Up; Make Your Own Luck |
| `MarkMustBlockThisTurn` | 3 | Culvert Ambusher; Hustle // Bustle; Academic Dispute |
| `MoveCounters` | 3 | Explorer's Cache; Tester of the Tangential; Costume Closet |
| `PayExactCounters` | 3 | Guide of Souls; Jolted Awake; Volatile Stormdrake |
| `StorePlayer` | 3 | Tempt with Bunnies; Tempt with Discovery; Plaguecrafter |
| `UnattachEquipment` | 3 | Stolen Uniform; Unexpected Request; Disarm |

Also (1–2 cards each): `AddAdditionalEndSteps`, `AddAdditionalUpkeepSteps`, `AddColor`, `AddCountersOfChosenKind`, `AddCountersUpTo`, `AllowLoyaltyActivationsThisTurn`, `AmplifyDamageThisTurn`, `AttachToChosenHost`, `BecomeChosenManaColor`, `BecomeCopyOfLinkedExile`, `CantActivateLoyaltyAbilities`, `CantCastSpellsFromNonHandZones`, `CantPlayCardsFromHand`, `CantSearchLibraries`, `ChangeCreatureTypeText`, `ChangeGroupColor`, `ChangeSpeed`, `ChangeSpellTarget`, `ChangeWordInText`, `ChooseCardTypeForSource`, `ChooseColorForTarget`, `ChooseNumberForSource`, `ChooseOnePerCategory`, `CollectEvidenceChosenAmount`, `ControlCombatDeclarationsThisTurn`, `ConvertCountersToTokens`, `CopyEachSpellCast`, `CopyEachTargetSpell`, `CopyForEachOtherPossibleTarget`, `CounterAllOnStack`, `CreateTokenCopyOfChosenPermanent`, `CreateTokenCopyOfEquippedCreature`, `DamageToTargetCantBePreventedThisTurn`, `DealDamagePerEntityInZone`, `DestroyAllEquipmentOnTarget`, `DestroySourceOfTargetedAbility`, `DistributeCountersAmongFiltered`, `DistributeCountersFromSelf`, `DoubleDamageToPlayer`, `EachPlayerChoosesCreatureType`, `EachPlayerDiscardsOrLoseLife`, `EachPlayerDrawsForDamageDealtToSource`, `EachPlayerReturnsPermanentToHand`, `EmitScriedEvent`, `EndTheTurn`, `ExchangeLifeAndStat`, `ExchangeLifeTotals`, `ExileAndGrantOwnerPlayPermission`, `ExileFromTopRepeating`, `ExileLibraryUntilManaValue`, `ExileSpellsOnStack`, `ExileTopCardContest`, `ExileWithAurasNotingCounters`, `FlipCoins`, `FlipCoinsUntilLoss`, `FlipTwoCoins`, `ForceExileMultiZone`, `GainAllActivatedAbilitiesOf`, `GatherSubtypes`, `Goad`, `GrantCantBeBlockedByChosenColor`, `GrantCastCreaturesFromGraveyardWithForage`, `GrantCounterPlacementModifier`, `GrantDamageBonus`, `GrantEmbalm`, `GrantExileOnLeave`, `GrantFlashToSpells`, `GrantHarmonize`, `GrantHexproofFromChosenColor`, `GrantInstantSpeedLoyaltyAbilities`, `GrantKeywordToAttackersBlockedBy`, `GrantNextSpellAffinity`, `GrantNextSpellFreeCast`, `GrantPlayWithAdditionalCost`, `GrantPlayWithCostIncrease`, `GrantPlayerProtection`, `GrantProtectionFromChosenCardType`, `GrantProtectionFromColorlessOrChosenColor`, `GrantProtectionsSharedByGroup`, `GrantSpellKeyword`, `GrantSpellsCantBeCountered`, `GrantStateTriggeredAbility`, `GrantSuspend`, `LockDoor`, `LockLifeGain`, `LoseUnspentMana`, `MakeNextSpellUncounterable`, `MarkExileControllerGraveyardOnDeath`, `MarkSpellExileWithCounters`, `MarkSpellPlotOnResolve`, `MassAnimate`, `MoveChosenCountersToTarget`, `MoveCountersEachKindMissing`, `MoveUntilSourceLeaves`, `NoteCreatureType`, `OpenLifeBid`, `OpponentGuessesTopCardKindEffect`, `PayAnyAmountOfLifeAsEnters`, `PayCounters`, `PayDynamicLife`, `PayManaCostRepeatedly`, `PhaseInLinkedToSource`, `PhaseOut`, `PhaseOutUntilLeaves`, `PlayFromCollectionWithoutPayingCost`, `PlayerGuessesConditionEffect`, `PreventLandPlaysThisTurn`, `PutOntoBattlefieldAttachedToChosen`, `RecordChosenLinkedExile`, `RedirectCombatDamageToController`, `RedistributeLifeTotals`, `ReduceMaximumHandSize`, `RemoveAbilitiesFromSourceOfTargetedAbility`, `RemoveDamageShield`, `ReselectTargetRandomly`, `RetainUnspentMana`, `ReturnCreaturesPutInGraveyardThisTurn`, `ReturnNotedExileTappedWithAuras`, `ReturnOneFromLinkedExile`, `ReturnSameNamedFromGraveyard`, `RevealFaceDownPermanent`, `SecretBid`, `SetDayNight`, `SetGroupCreatureSubtypes`, `SkipNextDrawStep`, `SkipNextUntapStep`, `SkipStepOrPhaseThisTurn`, `SwapBlockingAssignments`, `SwitchPowerToughness`, `TapForManaPermanentsYouDontControl`, `Unprepare`

### `StaticAbility` — 156 gap subtypes

| subtype | cards | examples |
|---|--:|---|
| `GrantCardType` | 35 | Titania's Song; Bello, Bard of the Brambles; Midnight Mangler; Kaito, Bane of Nightmares |
| `LookAtTopOfLibrary` | 23 | Vizier of the Menagerie; Glarb, Calamity's Augur; Precognition Field; Mm'menon, the Right Hand |
| `MustAttack` | 22 | Battle-Mad Ronin; Uncontrollable Anger; Crazed Goblin; Flamewake Phoenix |
| `GrantWard` | 20 | Innkeeper's Talent; Long River Lurker; Winter, Cursed Rider; Combat Research |
| `LoseAllAbilities` | 20 | Titania's Song; Sugar Coat; Flood the Engine; Deep Freeze |
| `CantAttack` | 18 | Cage of Hands; Spiral into Solitude; Bonds of Faith; Petrify |
| `TransformPermanent` | 17 | Sugar Coat; Oni Possession; Enduring Courage; Enduring Curiosity |
| `SetBasePowerToughnessDynamicStatic` | 15 | Shapeshifter; Titania's Song; Voice of Resurgence; Dollmaker's Shop // Porcelain Gallery |
| `CrewSaddleContribution` | 14 | Back on Track; Cloudspire Captain; Cloudspire Coordinator; Country Roads |
| `ControlEnchantedPermanent` | 13 | Yavimaya's Embrace; Kitnap; In Bolas's Clutches; Duskmourn's Domination |
| `MayCastFromGraveyard` | 13 | Festival of Embers; Walk-In Closet // Forgotten Cellar; Gisa and Geralf; Emet-Selch, Unsundered |
| `PreventActivatedAbilities` | 13 | Stuck in Summoner's Sanctum; Braided Net; Petrify; Sharkey, Tyrant of the Shire |
| `GrantAdditionalLandDrop` | 12 | Hugs, Grisly Guardian; Azusa, Lost but Seeking; Prismatic Undercurrents; Icetill Explorer |
| `GrantKeywordToOwnSpells` | 12 | Dazzling Theater // Prop Room; Eirdu, Carrier of Dawn; Raiding Schemes; Samut, Tyrant of Naktamun |
| `PlayersCantCastSpells` | 12 | City in a Bottle; Dosan the Falling Leaf; Brisela, Voice of Nightmares; Yuriko, Blade of the Mighty |
| `CanAttackDespiteDefender` | 11 | Mechan Shieldmate; Demon Wall; Ghalta the Immovable; Surveillance Phantasm |
| `GrantProtection` | 11 | Shield of Duty and Reason; Black Ward; Blue Ward; Green Ward |
| `AdditionalManaOnSourceTap` | 10 | Heartbeat of Spring; Lavaleaper; High Tide; Ultima, Origin of Oblivion |
| `AdditionalSourceTriggers` | 10 | Mirror Room // Fractured Realm; Twinflame Travelers; Cloud, Midgar Mercenary; Bifur, Melodic Rider |
| `CastSpellTypesFromTopOfLibrary` | 10 | Vizier of the Menagerie; Precognition Field; Mm'menon, the Right Hand; Elven Chorus |
| `NoMaximumHandSize` | 10 | Thought Vessel; Graceful Adept; Reliquary Tower; Vnwxt, Verbose Host |
| `GrantMayCastFromLinkedExile` | 8 | Rona, Disciple of Gix; Valgavoth, Terror Eater; Dawnhand Dissident; Maralen, Fae Ascendant |
| `MayCastSelfFromZones` | 8 | Lightwheel Enhancements; Wickerfolk Indomitable; Gravecrawler; Squee, the Immortal |
| `MayCastWithoutPayingManaCost` | 8 | Charred Foyer // Warped Space; Tamiyo, Field Researcher; Weftwalking; Omnipresence |
| `GrantKeywordByCounter` | 7 | Abzan Battle Priest; Abzan Falconer; Ainok Bond-Kin; Longshot Squad |
| `MayPlayLandsFromGraveyard` | 7 | Walk-In Closet // Forgotten Cellar; Icetill Explorer; Emet-Selch, Unsundered; Yawgmoth's Agenda |
| `MustBeBlockedStatic` | 7 | Fear of Being Hunted; The Masamune; Lure; Nath's Elite |
| `PlayLandsAndCastFilteredFromTopOfLibrary` | 7 | Glarb, Calamity's Augur; The Lunar Whale; Traveling Chocobo; The Belligerent |
| `ReduceActivatedAbilityCost` | 7 | Power Artifact; Boom Scholar; Forensic Gadgeteer; Hulk, Gamma Goliath |
| `AdditionalETBOrLTBTriggers` | 6 | Naban, Dean of Iteration; Starfield Vocalist; Traveling Chocobo; Gandalf the White |
| `AssignCombatDamageAsUnblocked` | 6 | Thorn Elemental; Invasion of Ikoria; Deathcoil Wurm; Lone Wolf |
| `HasAllActivatedAbilitiesOfCards` | 6 | Territory Forge; Thranduil, the Elvenking; The Enigma Jewel; Mirran Safehouse |
| `ReduceEquipCost` | 6 | Cloud, Planet's Champion; Firion, Wild Rose Warrior; Dwarven Mauler; Éowyn, Lady of Rohan |
| `RemoveCardType` | 6 | Kaito, Bane of Nightmares; Overlord of the Balemurk; Overlord of the Boilerbilges; Overlord of the Floodpits |
| `AdditionalManaOnTap` | 5 | Shimmerwilds Growth; Fertile Ground; Buried in the Garden; Blighted Burgeoning |
| `GrantAdditionalTypesToGroup` | 5 | Ygra, Eater of All; Ragost, Deft Gastronaut; Laughing Jasper Flint; Avatar Destiny |
| `GrantChosenSubtype` | 5 | Metallic Mimic; Lifecraft Engine; Leyline of Transformation; Roaming Throne |
| `RestrictSpellsCastPerTurn` | 5 | Yawgmoth's Agenda; Colfenor's Plans; Phyrexian Censor; Rule of Law |
| `SetMaximumHandSize` | 5 | Cursed Rack; Twenty-Toed Toad; Winter, Misanthropic Guide; The Ten Rings |
| `CanBlockAnyNumber` | 4 | Thoughtweft Trio; Ironfist Crusher; Entangler; Wall of Glare |
| `CantBeAttackedBy` | 4 | Teferi's Moat; Storm, Windrider; Blazing Archon; Form of the Dragon |
| `SpendAnyManaTypeForActivatedAbilities` | 4 | Sharkey, Tyrant of the Shire; Drana and Linvala; Quicksilver Elemental; Agatha's Soul Cauldron |
| `SuppressEntersTriggers` | 4 | Karn, Argent Defender; Doorkeeper Thrull; Torpor Orb; Elesh Norn, Mother of Machines |
| `CanBlockAdditionalForCreatureGroup` | 3 | Brave the Sands; Lairwatch Giant; Selesnya Sagittars |
| `CantAttackUnlessCoAttacker` | 3 | Toby, Beastie Befriender; Scarred Puma; Militia Rallier |
| `CantBeBlockedByCreaturesWithLessPower` | 3 | Shrill Howler; Formation Breaker; Elusive Otter |
| `GainActivatedAbilitiesOfPermanents` | 3 | Marvin, Murderous Mimic; Sharkey, Tyrant of the Shire; Drana and Linvala |
| `GrantAlternativeCastingCost` | 3 | Jodah, Archmage Eternal; Leyline of Mutation; Conspiracy Unraveler |
| `GrantCantLoseGame` | 3 | Lich's Mastery; Herald of Eternal Dawn; Platinum Angel |
| `GrantChosenColor` | 3 | Puca's Eye; Shimmerwilds Growth; Alloy Golem |
| `GrantHexproofToController` | 3 | Shalai, Voice of Plenty; Crystal Barricade; Captain America, Super-Soldier |
| `IsAllCreatureTypes` | 3 | Stalactite Dagger; Runed Stalactite; Undercover Skrull |
| `LegendRuleDoesNotApplyTo` | 3 | Brothers Yamazaki; Hall of Echoes; Spider-Verse |
| `LookAtFaceDownCreatures` | 3 | Found Footage; Lens of Clarity; Lumbering Laundry |
| `SetEnchantedLandType` | 3 | Tainted Well; Evil Presence; Sea's Claim |

Also (1–2 cards each): `AddCreatureTypeByCounter`, `AddLandTypeByCounter`, `AdditionalAttackTriggers`, `AdditionalDeathTriggers`, `AdditionalManaForEntryCounters`, `AnimateLandGroup`, `AssignUnblockedCombatDamageToDefendingCreature`, `AttackerCountLimit`, `BlockTax`, `BlockerCountLimit`, `CanBlockAsThoughUntapped`, `CantAttackOrBlockUnlessPay`, `CantAttackUnlessSacrifice`, `CantBeAttackedWhileAttached`, `CantBeBlockedIfCastSpellType`, `CantBeBlockedIfDefenderControls`, `CantBeBlockedUnlessDefenderSharesCreatureType`, `CantBeBlockedWhilePropertyAtMost`, `CantBeSacrificed`, `CantBeTargetedByOpponentAbilities`, `CantBeTargetedBySourceTypeAbilities`, `CantBeTurnedFaceUp`, `CantBlockCreaturesWithGreaterPower`, `CantBlockUnlessCoBlocker`, `CantCastSpellsSharingColorWithLastCast`, `CantReceiveCounters`, `ChangeAllColorWordsToChosenColor`, `ConvertEmptyingMana`, `CreaturesDamagedBySourceAreDoomed`, `DamagePersistsThroughCleanup`, `DampLandManaProduction`, `DivideCombatDamageFreely`, `EquipAbilitiesAtInstantSpeed`, `EquipmentAttachRestriction`, `ExtraLoyaltyActivation`, `ExtraOnceOnlyActivations`, `FlipAdditionalCoins`, `FreeFirstEquipEachTurn`, `GainKeywordsOfGraveyardCreatureCards`, `GrantCantLoseGameFromLife`, `GrantColor`, `GrantHexproofFromMonocoloredToGroup`, `GrantHexproofFromMulticoloredToGroup`, `GrantHexproofFromOwnColorsToGroup`, `GrantLandwalkOfChosenType`, `GrantMadnessToOwnedCards`, `GrantMiracleToCardsInHand`, `GrantOpponentsCantWinGame`, `GrantProtectionFromCardType`, `GrantProtectionFromControlledColors`, `GrantProtectionFromLinkedExiledCardTypes`, `GrantProtectionToController`, `GrantShroudToController`, `GrantSupertype`, `GrantWarpToCardsInHand`, `GrantWebSlingingToSpells`, `GraveyardCardsHaveDredge`, `GraveyardCardsHaveFlashback`, `GraveyardCardsHaveMayhem`, `GraveyardCreaturesHaveSneak`, `HasAbilitiesOfChosenLinkedExiledCard`, `HasCreatureTypesOf`, `IncreaseActivatedAbilityCost`, `LandsCantEnterTheBattlefield`, `MayPlayCardsFromExile`, `MayPlayPermanentsFromGraveyard`, `ModifyPlotCost`, `ModifyUnlockCost`, `MultiplyManaOnSourceTap`, `MustBlock`, `NoncombatDamageBonus`, `OpponentsCantMakeYouSacrifice`, `OpponentsPlayWithHandsRevealed`, `OverrideEnchantedLandManaColor`, `PayLifeForColoredMana`, `PlayFromTopOfLibrary`, `PlayFromTopWithAlternativeCost`, `PlayersCantActivateAbilities`, `PlayersCantPlayLands`, `PlayersRevealTopOfLibrary`, `PlotFromTopOfLibrary`, `PreventCycling`, `PreventManaPoolEmptying`, `RemoveKeywordStatic`, `ReplaceLandManaColor`, `RetainUnspentColoredMana`, `RevealFirstDrawEachTurn`, `RevealTopOfLibrary`, `SetBaseToughnessForCreatureGroup`, `SetEnchantedLandTypeFromChosen`, `SetLandTypesForGroup`, `SetName`, `SkipDrawStep`, `SpendAnyManaTypeForSpells`, `StationUsingToughness`, `SuppressHexproofForGroup`, `SuppressWardForGroup`, `UntapFilteredDuringOtherUntapSteps`, `UntapLimitPerStep`, `UntapSelfDuringOtherUntapSteps`, `WinCoinFlips`

### `Condition` — 93 gap subtypes

| subtype | cards | examples |
|---|--:|---|
| `WasKicked` | 96 | Bushy Bodyguard; Coruscation Mage; Darkstar Augur; Finch Formation |
| `CollectionContainsMatch` | 58 | Transmute Artifact; Cache Grab; Brutal Deceiver; Callous Deceiver |
| `PlayerCastSpellsThisTurn` | 31 | Alania, Divergent Storm; Sift Through Sands; Brightspear Zealot; Gigastorm Titan |
| `TargetIsSource` | 21 | Archpriest of Shadows; Bola Slinger; Boon-Bringer Valkyrie; Chomping Kavu |
| `WasCast` | 19 | Territory Forge; Doomsday Excruciator; Marina Vendrell's Grimoire; Sunderflock |
| `SourceChosenModeIs` | 17 | Primal Clay; Outpost Siege; Gollum, Riddle Master; Ashling's Prerogative |
| `WasCastFromZone` | 16 | Ruthless Negotiation; Increasing Devotion; Undead Sprinter; From Father to Son |
| `CreatureDiedThisTurn` | 14 | Tragic Slip; Cackling Slasher; Cackling Prowler; Needletooth Pack |
| `ManaSpentToCastIncludes` | 14 | Catharsis; Deceit; Emptiness; Vibrance |
| `SourceAbilityResolvedNTimesThisTurn` | 14 | Harvestrite Host; Victor, Valgavoth's Seneschal; Soulbright Seeker; Tannuk, Memorial Ensign |
| `Void` | 13 | Alpharael, Stonechosen; Chorale of the Void; Decode Transmissions; Elegy Acolyte |
| `PermanentLeftBattlefieldThisTurn` | 10 | Shortcut to Mushrooms; Foot Mystic; Insectoid Exterminator; Krang & Shredder |
| `PlayerDrewCardsThisTurn` | 9 | Lyra, Tolarian Archangel; Lake-town Toymaker; Gwaihir the Windlord; Kami of Jealous Thirst |
| `PlayerHasEnduringStory` | 9 | Balin, Loremaster; Bifur, Melodic Rider; Bombur, Gentle Dreamer; Dáin, Lord of the Iron Hills |
| `IsInPhase` | 7 | Canyon Vaulter; Full Throttle; Reckless Velocitaur; Dose of Dawnglow |
| `PlayerCommittedCrimeThisTurn` | 7 | Nimble Brigand; Oko, the Ringleader; Omenport Vigilante; Seize the Secrets |
| `TriggeringEntityHadCounters` | 7 | Reluctant Role Model; Ambitious Augmenter; Scolding Administrator; Host of the Hereafter |
| `PlayerAttackedWithCreaturesThisTurn` | 6 | Fire and Brimstone; Deepway Navigator; Thaumaton Torpedo; Case of the Gateway Express |
| `SacrificedPermanentHadSubtype` | 6 | Serendib Djinn; Hellish Sideswipe; Falkenrath Torturer; Thallid Omnivore |
| `ColorIsMostCommon` | 5 | Goham Djinn; Halam Djinn; Ruham Djinn; Sulam Djinn |
| `ControlledCreatureDiedThisTurn` | 5 | Denethor, Ruling Steward; Faramir, Field Commander; Sméagol, Helpful Guide; Essenceknit Scholar |
| `EnchantedCreatureHasSubtype` | 5 | Sorcerer's Wand; Bonds of Faith; Lavamancer's Skill; Clutch of Undeath |
| `PermanentTypeEnteredBattlefieldThisTurn` | 5 | Mechan Shieldmate; Akal Pakal, First Among Equals; Shipwreck Sentry; Iron Man, Master of Machines |
| `SourceCastForImpending` | 5 | Overlord of the Balemurk; Overlord of the Boilerbilges; Overlord of the Floodpits; Overlord of the Hauntwoods |
| `SourceInZone` | 5 | Edgar Markov; Uba Mask; Deep-Cavern Bat; Auratouched Mage |
| `SourceReturnedAsEnchantment` | 5 | Enduring Courage; Enduring Curiosity; Enduring Innocence; Enduring Tenacity |
| `TargetIsCreatureCard` | 5 | Scavenging Ooze; Hauntwoods Shrieker; Invasion of Innistrad; Raven Eagle |
| `SneakCostWasPaid` | 4 | Karai, Future of the Foot; Leonardo, Leader in Blue; The Last Ronin's Technique; Turncoat Kunoichi |
| `TriggeringSpellManaSpentAtLeast` | 4 | Prompto Argentum; Sahagin; The Prima Vista; Ultros, Obnoxious Octopus |
| `YouChoseOtherCreatureAsRingBearer` | 4 | Aragorn, Company Leader; Faramir, Field Commander; Galadriel of Lothlórien; Gandalf, Friend of the Shire |
| `BlightWasPaid` | 3 | Burning Curiosity; Cinder Strike; Requiting Hex |
| `EnchantedCreatureIsLegendary` | 3 | Combat Research; Andúril, Flame of the West; Gimli's Axe |
| `IsPlayersTurn` | 3 | Adrenaline Jockey; March of the World Ooze; Scytheclaw Raptor |
| `TargetMarkedDamageExceedsToughness` | 3 | Orbital Plunge; Bolg of the North; Torch the Witness |
| `WaterbendWasPaid` | 3 | Ruinous Waterbending; Secret of Bloodbending; Spirit Water Revival |
| `YouControlSource` | 3 | Menacing Ogre; Gwen Stacy; Hama, the Bloodbender |
| `YouSacrificedPermanentThisWay` | 3 | Garruk, Veiled Butcher; Rise of the Witch-king; Deadly Brew |

Also (1–2 cards each): `APlayerControlsMostOfSubtype`, `AnOpponentLifeAtMost`, `AnotherPermanentWithSameNameAsTarget`, `AnyEnteredOrWasCastFromExile`, `AnyPlayerDealtCombatDamageThisTurnAtLeast`, `CastChoiceIs`, `CastTimeFlagSet`, `CollectionSharesCardType`, `ControllerTurnsTakenAtMost`, `CounterPutOnPermanentYouControlledThisTurn`, `CreatureWithSubtypeDiedThisTurn`, `EachPlayerLifeAtMost`, `Escaped`, `ExiledAsCostHadSubtype`, `IsFirstCombatPhaseOfTurn`, `IsFirstEndStepOfTurn`, `IsFirstSpellPaidWithTreasureManaCastThisTurn`, `IsInStep`, `IsNight`, `MayhemCostWasPaid`, `NoManaSpentToCast`, `NoManaSpentToCastEntered`, `NumberMatches`, `PermanentEnteredFaceDownThisTurn`, `PlayerActivatedExhaustAbilitiesThisTurn`, `PlayerAttackedPlayerThisTurn`, `PlayerControlsMostPermanents`, `PlayerHasCitysBlessing`, `PlayerHasMostLife`, `PlayerPlayedLandThisTurn`, `PlayerTurnedPermanentFaceUpThisTurn`, `RingHasTemptedPlayerAtLeast`, `SacrificedPermanentWasLegendary`, `SacrificedPermanentWasSuspected`, `SourceDealtDamageToPlayerThisTurn`, `SourceIsBlockingOrBlockedBySubtype`, `SourceIsModified`, `SourceIsRingBearer`, `TargetIsPlayer`, `TargetIsSpellOnStack`, `TargetIsTapped`, `TargetSharesMostCommonColor`, `ThisAbilityActivatedThisTurnAtLeast`, `TriggeringEntityEnteredOrWasCastFromGraveyard`, `TriggeringEntityHadCardType`, `TriggeringEntityHadMinusOneMinusOneCounter`, `TriggeringEntityHadSubtype`, `TriggeringEntityWasCast`, `TriggeringEntityWasHistoric`, `TriggeringEntityWasNotPutByThisSource`, `TriggeringPlayerIs`, `TriggeringSpellCastWithoutPayingMana`, `TriggeringSpellHasSingleTarget`, `WebSlungCostWasPaid`, `YouControlMostOfChosenType`, `YouWonTheClash`

### `CardPredicate` — 78 gap subtypes

| subtype | cards | examples |
|---|--:|---|
| `IsToken` | 117 | Oltec Matterweaver; Sandstorm Salvager; Transmutation Font; Worldwalker Helm |
| `HasAnyOfSubtypes` | 49 | Honored Dreyleader; Lupinflower Village; Mudflat Village; Oakhollow Village |
| `IsBattle` | 32 | Aetherblade Agent; Archpriest of Shadows; Assimilate Essence; Atraxa's Fall |
| `NameEqualsChosen` | 16 | Maelstrom Pulse; Cranial Extraction; Mindblaze; Ancient Vendetta |
| `IsNonlegendary` | 15 | Coiling Rebirth; Blind with Anger; Kiki-Jiki, Mirror Breaker; Cast Down |
| `SharesColorWith` | 15 | Konda's Banner; Spreading Plague; Mourner's Shield; Thought Prison |
| `TargetsMatching` | 14 | Teferi's Response; Conciliator's Duelist; Forum Necroscribe; Graduation Day |
| `IsNonartifact` | 11 | Ashnod's Transmogrant; Winter, Cursed Rider; Ashes to Ashes; Terror |
| `HasChosenSubtype` | 10 | Metallic Mimic; Three Tree City; Chronicle of Victory; Collective Inferno |
| `HasChosenColor` | 9 | Heraldic Banner; Addle; Crosis, the Purger; Darigaaz, the Igniter |
| `IsActivatedOrTriggeredAbility` | 9 | Gogo, Master of Mimicry; Louisoix's Sacrifice; Tishana's Tidebinder; Echo, Perceptive Prodigy |
| `ToughnessAtLeast` | 9 | Gallant Strike; Surgical Precision; Valorous Stance; Kheru Bloodsucker |
| `HasSubtypeFromVariable` | 8 | Celestial Reunion; Harmonized Crescendo; Kindred Judgment; Orcrist, Goblin-cleaver |
| `IsColorless` | 7 | Grizzled Angler; Ghostfire Blade; Tomb of the Spirit Dragon; Consign to Memory |
| `HasAdventure` | 6 | Beluna Grandsquall; Edgewall Inn; Frantic Firebolt; Hearth Elemental |
| `ManaValueEqualsDynamic` | 5 | Wishing Well; Repurposing Bay; Necroplasm; Sidisi, Regent of the Mire |
| `SharesChosenColorWithSource` | 5 | Jihad; Psychic Allergy; Harsh Judgment; Teferi's Moat |
| `ToughnessAtMost` | 5 | Unidentified Hovership; Goblin Kites; Stern Scolding; Massacre Girl, Known Killer |
| `IsActivatedAbility` | 4 | Squelch; Brown Ouphe; Bind; Reroute |
| `IsColored` | 4 | Ancient Cornucopia; Protective Sphere; Peter Parker; Ugin, Eye of the Storms |
| `ManaValueAtMostEntity` | 4 | Kodama of the East Tree; Saruman of Many Colors; Eriette, the Beguiler; Sunbird's Invocation |
| `ManaValueIsEven` | 4 | Mutinous Massacre; Gollum, Riddle Master; Ashling's Prerogative; Thanos, the Mad Titan |
| `ManaValueIsOdd` | 4 | Mutinous Massacre; Gollum, Riddle Master; Ashling's Prerogative; Thanos, the Mad Titan |
| `SharesCreatureTypeWith` | 4 | Konda's Banner; Invasion of New Capenna; Mana Echoes; Alpha Status |
| `SharesNameWith` | 4 | Extraplanar Lens; Spellweaver Helix; Bloodbond March; Circle of Confinement |
| `AbilitySourceMatches` | 3 | Brown Ouphe; Echo, Perceptive Prodigy; Scientist Supreme of A.I.M. |
| `HasXInManaCost` | 3 | Gaddock Teeg; Rosheen, Roaring Prophet; Paradox Surveyor |
| `IsDoubleFaced` | 3 | Invasion of Pyrulea; Overgrown Pest; Nick Fury, Agent of S.H.I.E.L.D. |
| `IsTriggeredAbility` | 3 | Kirol, Attentive First-Year; Consign to Memory; Spider-Sense |
| `NameEqualsChosenComponent` | 3 | Skyseer's Chariot; Petrified Hamlet; Sorcerous Spyglass |
| `PowerAtMostEntity` | 3 | Old Man of the Sea; Grishnákh, Brash Instigator; Spawnbroker |
| `PowerGreaterThanEntity` | 3 | Éowyn, Fearless Knight; Prehistoric Pet; Ingenious Prodigy |
| `PowerOrToughnessAtMost` | 3 | Tetsuko Umezawa, Pursuer; Arnyn, Deathbloom Botanist; Leonardo, Sewer Samurai |
| `ToughnessGreaterThanPower` | 3 | Fecund Greenshell; Doran, Besieged by Time; Catapult Fodder |

Also (1–2 cards each): `BasePowerEquals`, `BaseToughnessEquals`, `CardTypeEqualsChosenComponent`, `ColoredManaSymbolsAtLeast`, `ConvokedSource`, `CouldEnchant`, `DoesNotShareCreatureTypeWithPermanentYouControl`, `DoesNotShareLandTypeWithPermanentYouControl`, `HasActivatedAbility`, `HasBasicLandType`, `HasExactlyColors`, `HasNoAbilities`, `HasNonManaActivatedAbility`, `HasSubtypeInEachStoredGroup`, `HasSubtypeInStoredList`, `IsMonocolored`, `IsNonenchantment`, `ManaValueAtMostColorsSpent`, `ManaValueAtMostEntityManaSpent`, `NameNotSharedWithAnotherControlledPermanent`, `NameNotSharedWithControlledRoom`, `NameNotSharedWithControlledToken`, `NotOfSourceChosenType`, `OriginallyPrintedInSet`, `PowerAtLeastX`, `PowerAtMostDynamic`, `PowerEquals`, `PowerEqualsDynamic`, `PowerEqualsX`, `PowerGreaterThanBase`, `PowerLessThanEntity`, `PowerOrToughnessAtLeast`, `SharesCardTypeWithLinkedExile`, `SharesColorWithPermanentYouControl`, `SharesColorWithRecipient`, `SharesCreatureTypeWithSource`, `SharesCreatureTypeWithTriggeringEntity`, `SharesManaValueWith`, `SharesNameWithLinkedExile`, `SharesNameWithPermanentYouControl`, `TargetsPlayer`, `TotalPowerAndToughnessAtMost`, `ToughnessAtMostX`, `ToughnessEqualsDynamic`

### `StatePredicate` — 66 gap subtypes

| subtype | cards | examples |
|---|--:|---|
| `StateNot` | 37 | Deadeye Navigator; Lightning Mauler; Spectral Gateguards; Tandem Lookout |
| `IsSaddled` | 28 | Alacrian Jaguar; Autarch Mammoth; Brightfield Glider; Brightfield Mustang |
| `EnteredThisTurn` | 20 | Erg Raiders; Oakhollow Village; Waterspout Warden; Shardmage's Rescue |
| `IsSource` | 20 | Desert Nomads; Argothian Pixies; Argothian Treefolk; Uncle Istvan |
| `IsEquipped` | 16 | Blacksmith's Talent; Corrosive Ooze; Barret Wallace; Cloud, Midgar Mercenary |
| `IsAttachedToBySource` | 15 | Artifact Ward; General's Kabuto; Shield of the Realm; Curse Artifact |
| `InZone` | 13 | Sindbad; Silvan Reveler; Hostility; Deadly Cover-Up |
| `WasDealtDamageThisTurn` | 13 | Downwind Ambusher; Crushing Pain; Initiate of Blood; Giant Shark |
| `IsSolved` | 12 | Case of the Burning Masks; Case of the Crimson Pulse; Case of the Filched Falcon; Case of the Gateway Express |
| `HasAnyCounter` | 11 | Innkeeper's Talent; Demon Wall; Prison Barricade; Hunter of Eyeblights |
| `AttackedThisTurn` | 10 | Erg Raiders; Full Throttle; Lurker; The Lunar Whale |
| `IsSuspected` | 10 | Absolving Lammasu; Agrus Kos, Spirit of Justice; Case of the Stashed Skeleton; Clandestine Meddler |
| `AttackedThisCombat` | 8 | Clockwork Avian; Fearless Swashbuckler; Tolsimir, Midnight's Light; Clockwork Beetle |
| `IsModified` | 7 | Lethal Throwdown; Lion Umbra; Araña, Heart of the Spider; Biorganic Carapace |
| `IsAttachedTo` | 6 | Kitsune Mystic; Miracle Worker; Stolen Uniform; Glowcap Lantern |
| `BlockedThisCombat` | 5 | Clockwork Avian; Clockwork Beetle; Clockwork Condor; Clockwork Dragon |
| `IsEnchanted` | 5 | Gate Hound; A Tale for the Ages; Graceful Takedown; Lord Skitter's Blessing |
| `PutIntoGraveyardFromBattlefieldThisTurn` | 5 | Supper for Spiders; Lobelia Sackville-Baggins; Samwise the Stouthearted; Second Sunrise |
| `CrewedOrSaddledSourceThisTurn` | 4 | Subterranean Schooner; Giant Beaver; Rambling Possum; Turtle Van |
| `HasDealtDamage` | 4 | Ruric Thar, Magecrusher; Treacherous Greed; Red Guardian, Super-Soldier; Karakyk Guardian |
| `HasGreatestPower` | 4 | Consumed by Greed; Triumph of Gerrard; Extract a Confession; Kraven the Hunter |
| `IsAttachedToSource` | 4 | Cloud, Midgar Mercenary; Sunfire Torch; Ronin, Shadow Stalker; Faunsbane Troll |
| `IsAttackingAnOpponent` | 4 | Oviya, Automech Artisan; Garruk, Curse Breaker; Jiang Yanggu, Alone; Yuriko, Blade of the Mighty |
| `IsProtectedBy` | 4 | Etched Host Doombringer; Joyful Stormsculptor; Portent Tracker; Rampaging Raptor |
| `IsTransformed` | 4 | Corruption of Towashi; Invasion of Pyrulea; Mutagen Connoisseur; Oculus Whelp |
| `CreatedBySource` | 3 | Tetravus; Dance of Many; Mysterio, Master of Illusion |
| `ExiledWithSource` | 3 | Mimeoplasm, Revered One; The Darkness Crystal; Quintorius, Loremaster |
| `IsOnBattlefield` | 3 | Ceaseless Searblades; Bruce Banner; Warp World |
| `IsPrepared` | 3 | Paradox Shaper; Stingerquill Voxmancer; Woodwork Prodigy |
| `PutIntoGraveyardThisTurn` | 3 | Abyssal Harvester; Reenact the Crime; Night Nurse, Healer of Heroes |
| `ReceivedCounterThisTurn` | 3 | Beast, Erudite Aerialist; Kid Loki; Fractal Tender |
| `WasDealtDamageBySourceThisTurn` | 3 | Frostwielder; Kumano's Pupils; Kumano, Master Yamabushi |

Also (1–2 cards each): `ActivatedThisTurn`, `AttackedABattleThisTurn`, `AttackedLastTurn`, `BecameTappedOnlyOnceThisTurn`, `BlockedOrWasBlockedByEntityThisTurn`, `BlockedOrWasBlockedByLegendaryThisTurn`, `BlockedThisTurn`, `ControlledSinceTurnBegan`, `ControllerControls`, `ControllerDealtCombatDamageBySourceThisTurn`, `CrewedOrSaddledBySourceThisTurn`, `DealtCombatDamageToSourceControllerThisTurn`, `DealtDamageToSourceControllerThisTurn`, `HasDisguiseAbility`, `HasGreatestManaValueAmongAllCreatures`, `HasLeastManaValueAmong`, `HasLeastPower`, `HasLeastPowerAmongAllCreatures`, `HasLockedDoor`, `InSameBandAsSource`, `IsAttachedToCardType`, `IsAttackingABattle`, `IsAttackingEnchantedPlayer`, `IsAttackingYouOrYourPlaneswalkers`, `IsBlocked`, `IsBlockingIterationEntity`, `IsBlockingSource`, `IsCombatPairedWithSource`, `IsEnchantedByAura`, `IsRingBearer`, `IsWarpExiled`, `NotTargetedByAbilityFromSameNamedSource`, `SharesNameWithSpellCastThisTurn`, `WasCastForWarp`

### `EventPattern` — 55 gap subtypes

| subtype | cards | examples |
|---|--:|---|
| `YouAttackEvent` | 83 | Gossip's Talent; Hazardroot Herbalist; Hired Claw; Hunter's Talent |
| `DamageEvent` | 56 | Soul-Scar Mage; Camel; Desert Nomads; Argothian Pixies |
| `NthCardDrawnEvent` | 34 | Thopter Fabricator; Erudite Wizard; Homunculus Horde; Mischievous Mystic |
| `TransformEvent` | 23 | Huntmaster of the Fells; Ashling, Rekindled; Brigid, Clachan's Heart; Grub, Storied Matriarch |
| `DrawEvent` | 21 | Teferi, Temporal Pilgrim; Moonring Mirror; Uba Mask; Aether Syphon |
| `DoorUnlockedEvent` | 20 | Bottomless Pool // Locker Room; Central Elevator // Promising Stairs; Defiled Crypt // Cadaver Lab; Derelict Attic // Widow's Walk |
| `CommitCrimeEvent` | 19 | At Knifepoint; Bandit's Haul; Blood Hustler; Deepmuck Desperado |
| `AbilityActivatedEvent` | 18 | Artifact Possession; Haunting Wind; Powerleech; Adrenaline Jockey |
| `RoomFullyUnlockedEvent` | 15 | Balemurk Leech; Cult Healer; Dashing Bloodsucker; Entity Tracker |
| `ScriedEvent` | 11 | Arwen Undómiel; Celeborn the Wise; Chance-Met Elves; Council's Deliberation |
| `BlocksOrBecomesBlockedByEvent` | 10 | Corrosive Ooze; Giant Shark; Spitting Slug; Venom |
| `CreatureTurnedFaceUpEvent` | 10 | Cryptid Inspector; Growing Dread; Pine Walker; Secret Plans |
| `AnyOfEvents` | 9 | Moonstone Harbinger; Wax-Wane Witness; Canyon Vaulter; Reckless Velocitaur |
| `BecomesUnblockedEvent` | 9 | Merchant Ship; Murk Dwellers; Delif's Cone; Delif's Cube |
| `RingTemptedEvent` | 9 | Aragorn, Company Leader; Call of the Ring; Faramir, Field Commander; Galadriel of Lothlórien |
| `CounterPlacementEvent` | 8 | Innkeeper's Talent; Caradora, Heart of Alacria; Loading Zone; Mauhúr, Uruk-hai Captain |
| `CreatureDealtDamageBySourceDiesEvent` | 8 | Bushi Tenderfoot; Predator Ooze; Abattoir Ghoul; Zurgo Helmsmasher |
| `ScriedOrSurveiledEvent` | 7 | Matoya, Archon Elder; Denzilore Fatehold; Diviner of Victory; Proft, Consulting Detective |
| `BecomesAttachedEvent` | 4 | Blade of Shared Souls; Assimilation Aegis; Eriette, the Beguiler; Bramble Elemental |
| `TokenCreationEvent` | 4 | Worldwalker Helm; Draconic Visitor; Mirkwood Bats; Rosie Cotton of South Lane |
| `ClashedEvent` | 3 | Entangling Trap; Rebellion of the Flamekin; Sylvan Echoes |
| `ControlChangeEvent` | 3 | Stolen Uniform; Zidane, Tantalus Thief; Risky Move |
| `ExploredEvent` | 3 | Merfolk Cave-Diver; Nicanzil, Current Conductor; Twists and Turns |
| `ProliferatedEvent` | 3 | Scheming Aspirant; Tekuthal, Inquiry Dominus; Voidwing Hybrid |

Also (1–2 cards each): `AbilityTriggeredEvent`, `BecameSaddledEvent`, `BecomesPlottedEvent`, `BecomesUnattachedEvent`, `BendPerformedEvent`, `CardRevealedFromDrawEvent`, `CardsPutIntoExileEvent`, `CaseSolvedEvent`, `ChampionedEvent`, `ConnivedEvent`, `CountersRemovedEvent`, `CreaturesAttackYourOpponentEvent`, `CrewsEvent`, `DiscoveredEvent`, `EvidenceCollectedEvent`, `ExploitedEvent`, `ForagedEvent`, `LandTappedForMana`, `ManifestedDreadEvent`, `MillEvent`, `OneOrMoreDealCombatDamageToYouEvent`, `OpponentsDealtCombatDamageEvent`, `PhasesInEvent`, `PlayerLostGameEvent`, `SaddlesEvent`, `SagaChapterResolvedEvent`, `SearchLibraryEvent`, `ShuffleLibraryEvent`, `SpellOrAbilityOnStackEvent`, `TargetsChosenEvent`, `TrainedEvent`

### `DynamicAmount` — 40 gap subtypes

| subtype | cards | examples |
|---|--:|---|
| `Add` | 50 | Khabál Ghoul; Metamorphosis; Devouring Greed; Devouring Rage |
| `AggregateZone` | 48 | Ivory Tower; The Rack; Wick's Patrol; Mindwrack Demon |
| `Conditional` | 37 | Sonar Strike; Increasing Devotion; Sorcerer's Wand; Angry Mob |
| `StationCharge` | 27 | Adagia, Windswept Bastion; Atmospheric Greenhouse; Dawnsire, Sunstar Dreadnought; Debris Field Crusher |
| `DistinctEntitiesInCollections` | 25 | Tempt with Bunnies; Tempt with Discovery; Mana Seism; Shimatsu the Bloodcloaked |
| `Speed` | 25 | Aether Syphon; Burnout Bashtronaut; Embalmed Ascendant; Endrider Spikespitter |
| `StoredCardManaValue` | 20 | Transmute Artifact; Darkstar Augur; Bre of Clan Stoutarm; Ninja's Blades |
| `CastX` | 19 | Jacked Rabbit; Orochi Hatchery; Dune Drifter; Meathook Massacre II |
| `SpellsCastLastTurn` | 10 | Huntmaster of the Fells; Scorned Villager; Hanweir Watchkeep; Kruin Outlaw |
| `CountPlayersWith` | 9 | Bandit's Talent; Spikeshell Harrier; Lupine Prototype; Master's Councillors |
| `PermanentsSacrificedThisWay` | 9 | Devouring Greed; Devouring Rage; Reweave; Sephiroth, Fabled SOLDIER |
| `SpellsCastThisTurn` | 9 | Thousand-Year Storm; Sandstalker Moloch; Captain Mar-Vell, Space-Born; Magebane Lizard |
| `DistinctColorsManaSpent` | 8 | Arcane Omens; Archaic's Agony; Magmablood Archaic; Rancorous Archaic |
| `CastChoice` | 6 | Shapeshifter; Soul Immolation; Ancient Imperiosaur; Knight-Errant of Eos |
| `PlayerCount` | 6 | Hapatra, the Desert Fang; Hapatra, the Desert Frost; Lich's Relic; The Theorist, Jace Beleren |
| `StartingLifeTotal` | 6 | Chalice of Life; Torgaar, Famine Incarnate; Leyline of Hope; The Endstone |
| `CreaturesWithSubtypeDiedThisTurn` | 5 | Ashen-Skin Zubera; Dripping-Tongue Zubera; Ember-Fist Zubera; Floating-Dream Zubera |
| `LastKnownSourceCounters` | 5 | Twitching Doll; Nine-Lives Familiar; Ravenous Amulet; Icatian Moneychanger |
| `ManaValueSumOfCollection` | 5 | Necropolis; Wand of Ith; Lammastide Weave; Palantír of Orthanc |
| `Max` | 5 | Nezumi Shortfang; Doran, Besieged by Time; Spry and Mighty; Triumphant Chomp |
| `SubtypeEnteredUnderControlThisTurn` | 4 | Cloudspire Coordinator; Swordsworn Cavalier; Avengers Assemble!; Geralf, the Fleshwright |
| `Min` | 3 | Clockwork Avian; Ugin's Labyrinth; Diligent Zookeeper |
| `TotalManaSpent` | 3 | Dyadrine, Synthesis Amalgam; Memory Deluge; Molten Note |
| `TotalPowerSacrificedThisWay` | 3 | Soulblast; Kylox, Visionary Inventor; Twisted Justice |
| `UnlockedDoors` | 3 | Central Elevator // Promising Stairs; Rampaging Soulrager; Smoky Lounge // Misty Salon |

Also (1–2 cards each): `CardTypeEnteredUnderControlThisTurn`, `CountersRemovedAsCost`, `CraftedMaterialsColorCount`, `CraftedMaterialsTotalManaValue`, `CraftedMaterialsTotalPower`, `CreaturesThatCrewedOrSaddledThisTurn`, `DevotionTo`, `DistinctCardTypesInCollections`, `LargestSharedCreatureTypeCount`, `LastKnownDamageDealtToSource`, `ManaSpentFromSubtype`, `ManaSpentOnX`, `PlayerCounterCount`, `Power`, `UnspentMana`

### `ReplacementEffect` — 39 gap subtypes

| subtype | cards | examples |
|---|--:|---|
| `PreventDamage` | 27 | Camel; Desert Nomads; Argothian Pixies; Argothian Treefolk |
| `EntersAsCopy` | 17 | Mockingbird; Estrid's Invocation; Waxen Shapethief; Omni-Changeling |
| `OnEnterRunEffect` | 13 | Shimatsu the Bloodcloaked; Frankenstein's Monster; Nameless Race; Theorist's Sanctum |
| `PermanentsEnterTapped` | 10 | Thalia, Heretic Cathar; Authority of the Consuls; Dauntless Dismantler; Ashling's Prerogative |
| `ReplaceDrawWith` | 9 | Uba Mask; Vnwxt, Verbose Host; Bard, King of Dale; Laboratory Maniac |
| `DoubleDamage` | 8 | The Rollercrusher Ride; Collective Inferno; Twinflame Tyrant; Kuja, Genome Sorcerer |
| `ModifyCounterPlacement` | 8 | Caradora, Heart of Alacria; Yoshimaru, Beloved Companion; Hardened Scales; Mauhúr, Uruk-hai Captain |
| `EntersWithDevour` | 7 | Caldera Hellion; Predator Dragon; Thorn-Thrash Viashino; Thunder-Thrash Elder |
| `ModifyDamageAmount` | 6 | Valley Flamecaller; Akki Lavarunner; Far Fortune, End Boss; Invasion of Regatha |
| `MultiplyTokenCreation` | 6 | Exalted Sunborn; Bard, King of Dale; Ojer Taq, Deepest Foundation; Doubling Season |
| `PreventLifeGain` | 6 | Sunspine Lynx; Giant Cindermaw; Grievous Wound; Mornsong Aria |
| `EntersWithKeywords` | 5 | Benalish Lancer; Duskwalker; Faerie Squadron; Kavu Titan |
| `RedirectDamage` | 5 | Martyrs of Korlis; Ancient Adamantoise; Harsh Judgment; Pariah's Shield |
| `ReplaceDamageWithCounters` | 5 | Soul-Scar Mage; Phytohydra; Szadek, Lord of Secrets; Force Bubble |
| `CreateAdditionalToken` | 4 | Worldwalker Helm; Quina, Qu Gourmet; Peregrin Took; Case of the Pilfered Proof |
| `DamageCantBePrevented` | 4 | Sunspine Lynx; Frenzied Baloth; Excruciator; Spider-Punk |
| `DoubleCounterPlacement` | 4 | Innkeeper's Talent; Loading Zone; The Earth Crystal; Doubling Season |
| `RedirectZoneChangeWithEffect` | 4 | Darigaaz Reincarnated; The Darkness Crystal; Head of the Hunt; Ugin's Nexus |

Also (1–2 cards each): `CapCounterPlacementThisTurn`, `CapDamage`, `EntersUntapped`, `EntersWithExileCounters`, `ExileCounteredSpellInstead`, `HalveDamage`, `HealOtherDamage`, `LifeLossFloor`, `ModifyDrawAmount`, `ModifyKeywordAction`, `ModifyLifeLoss`, `ModifyMillAmount`, `PreventDamageByRemovingCounter`, `PreventDraw`, `PreventExtraTurns`, `RepeatKeywordAction`, `ReplaceDamageWithMill`, `ReplaceLifePaymentWithLibraryExile`, `ReplaceTokenCreationWithAttachedCopy`, `ReplaceTokenCreationWithToken`, `SetMinimumDamage`

### `AbilityCost` — 20 gap subtypes

| subtype | cards | examples |
|---|--:|---|
| `CostLoyalty` | 68 | Ral, Crackling Wit; Teferi, Temporal Pilgrim; Chandra, Spark Hunter; The Aetherspark |
| `CostDiscardSelf` | 33 | Harvester of Misery; Altanak, the Thrice-Called; Gideon's Memorial; Proft, Sinister Mastermind |
| `CostCraft` | 19 | Braided Net; Clay-Fired Bricks; Dire Flail; Idol of the Deep King |
| `CostBlight` | 6 | Champion of the Weird; Dawnhand Dissident; Evershrike's Gift; Gristle Glutton |
| `CostDiscardHand` | 4 | Tarrian's Journal; Connecting the Dots; Slate of Ancestry; Reverberating Summons |
| `CostFree` | 4 | Urza's Avenger; Eater of the Dead; Gaea's Touch; Lethal Vapors |
| `CostLoyaltyX` | 3 | Chandra, Chill of Compliance; Chandra Nalaar; Chandra, Hope's Beacon |

Also (1–2 cards each): `AttachedPermanentManaCost`, `CostDiscardLastDrawnThisTurn`, `CostExileGrantingPermanent`, `CostExileXFromGraveyard`, `CostForage`, `CostPayXLife`, `CostRemoveAllCounters`, `CostReturnSelfToHand`, `CostSacrificeChosenCreatureType`, `CostSacrificeGrantingPermanent`, `CostTapAttachedCreature`, `CostTapGrantingPermanent`, `CostTapXPermanents`

### `EffectTarget` — 18 gap subtypes

| subtype | cards | examples |
|---|--:|---|
| `PipelineTarget` | 160 | Nexus of Becoming; Portent of Calamity; Season of Gathering; Season of the Burrow |
| `EnchantedCreature` | 62 | Unstable Mutation; Charmed Sleep; Colossification; Kitnap |
| `TargetController` | 55 | Crumble; Detonate; Blooming Blast; Beast Within |
| `ControllerOfTriggeringEntity` | 18 | Eye for an Eye; Artifact Possession; Haunting Wind; Kusari-Gama |
| `EquippedCreature` | 16 | Lost Jitte; Oathkeeper, Takeno's Daisho; Tenza, Godo's Maul; Stitcher's Graft |
| `GrantingSource` | 5 | Hankyu; Fishing Pole; Nettlevine Blight; Trusty Boomerang |
| `AttachedToTriggeringPermanent` | 3 | Stitcher's Graft; Blade of Shared Souls; Eriette, the Beguiler |
| `ControllerOfPipelineTarget` | 3 | Season of the Burrow; Severance Priest; Zuko's Exile |

Also (1–2 cards each): `AmassedArmy`, `ChosenCreature`, `ControllerOfDamageSource`, `DiscardedAsCost`, `EachDamagedBySourceThisGame`, `GroupRef`, `LibraryTop`, `LinkedExiledCard`, `TappedAsCost`, `TargetingSource`

### `CostReductionSource` — 17 gap subtypes

| subtype | cards | examples |
|---|--:|---|
| `FixedIfControlFilter` | 16 | Pearl of Wisdom; Academy Journeymage; Wizard's Lightning; Wizard's Retort |

Also (1–2 cards each): `ArtifactsYouControl`, `AttachedPermanentProperty`, `CardTypesInYourGraveyard`, `CardsInGraveyardAndExileMatchingFilter`, `ChosenTargetsBeyondTheFirst`, `ColorsAmongPermanentsYouControl`, `CreaturesThatAttackedThisTurn`, `Dynamic`, `Fixed`, `FixedIfCreatureAttackingYou`, `FixedIfCreatureDiedThisTurn`, `FixedIfVoid`, `PermanentsSacrificedThisTurn`, `PermanentsWithCounterYouControl`, `TotalPowerYouControl`, `YourSpeed`

### `Player` — 16 gap subtypes

| subtype | cards | examples |
|---|--:|---|
| `ChosenOpponent` | 30 | Jihad; Cursed Rack; The Rack; Blooming Blast |
| `ControllerOf` | 25 | Demolition Field; Feast of Worms; Reweave; Spikeshell Harrier |
| `TargetPlayer` | 15 | Drafna's Restoration; Angel of Finality; Mindblaze; Orcish Spy |
| `ContextPlayer` | 8 | Riverchurn Monument; Singularity Rupture; Thraben Charm; Hollow Marauder |
| `OwnerOf` | 7 | Chaos Warp; Nezumi Graverobber; Compelling Deterrence; Clash of Elements |
| `ControllerOfTriggeringEntity` | 4 | Elesh Norn; Rona, Herald of Invasion; Mesmeric Orb; Belltower Sphinx |
| `EnchantedPlayer` | 4 | Grievous Wound; Curse of Hospitality; Faithbound Judge; Radiant Grace |
| `ControllerOfSource` | 3 | Garruk, Veiled Butcher; Wojek Investigator; Consuming Tide |
| `InCollection` | 3 | Tempt with Bunnies; Tempt with Discovery; Plaguecrafter |
| `OwnerOfSource` | 3 | Hanabi Blast; Petals of Insight; Gandalf, Wandering Wizard |

Also (1–2 cards each): `Candidate`, `ControllerOfAffectedEntity`, `ControllerOfIterationEntity`, `ControllerOfTargetingSource`, `EachTargetedPlayer`, `OwnersOfLinkedExile`

### `Duration` — 15 gap subtypes

| subtype | cards | examples |
|---|--:|---|
| `Permanent` | 89 | Sensei Golden-Tail; Through the Breach; Coalstoke Gearhulk; Marshals' Pathcruiser |
| `UntilYourNextTurn` | 22 | Azure Beastbinder; For the Common Good; Season of the Bold; Song of Freyalise |
| `WhileSourceTapped` | 16 | Ashnod's Battle Gear; Phyrexian Gremlins; Tawnos's Weaponry; Hisoka's Guard |
| `WhileSourceOnBattlefield` | 10 | Pyreswipe Hawk; Time of Ice; Scarwood Bandits; Old Fat Spider Can't See Me |
| `WhileYouControlSource` | 9 | Aladdin; Possession Engine; Thrull Champion; Olivia Voldaren |
| `EndOfCombat` | 3 | Battering Ram; Murk Dwellers; Ria Ivor, Bane of Bladehold |
| `WhileAffectedHasCounter` | 3 | Ultima, Origin of Oblivion; Aquitect's Will; Makeshift Mannequin |

Also (1–2 cards each): `EndOfYourNextTurn`, `UntilNextEndStep`, `UntilSourceCastFromExile`, `UntilYourNextUpkeep`, `WhileAffectedTapped`, `WhileSourceAttachedToAffected`, `WhileSourceTappedAndAffectedPowerAtMostSource`, `WhileYouControlSourceAndSourceTapped`

### `SpellCastPredicate` — 15 gap subtypes

| subtype | cards | examples |
|---|--:|---|
| `SpellCastFromZone` | 13 | Wildsear, Scouring Maw; Goliath Daydreamer; Burning Vengeance; Ojer Pakpatiq, Deepest Epoch |
| `SpellTargetsMatching` | 9 | Leyline of Resonance; Legolas, Master Archer; Tiller of Flesh; Colleen Wing, Street Samurai |
| `SpellCastFromZoneOtherThan` | 3 | Kellan, the Kid; Shadow of the Goblin; Spider-Verse |
| `SpellNotOwnedByController` | 3 | Gonti, Night Minister; Vaan, Street Thief; Nita, Forum Conciliator |
| `SpellPaidWithManaFromSource` | 3 | Brass's Tunnel-Grinder; The Everflowing Well; Thousand Moons Smithy |
| `SpellPaidWithManaFromSubtype` | 3 | Alchemist's Talent; Smaug, Wicked Worm; Rain of Riches |
| `SpellTargetsSource` | 3 | Legolas, Master Archer; Gnarlback Rhino; Speedball, New Warrior |
| `SpellWasKicked` | 3 | Bloodstone Goblin; Hallar, the Firefletcher; Saproling Infestation |

Also (1–2 cards each): `SpellAnyOf`, `SpellCastAsPrepareSpell`, `SpellHasXInCost`, `SpellIsCard`, `SpellIsModal`, `SpellPaidWithManaFromCardType`, `SpellTargetsOnlySource`

### `AdditionalCost` — 14 gap subtypes

| subtype | cards | examples |
|---|--:|---|
| `OrPay` | 19 | Kinsbaile Aspirant; Lys Alana Dignitary; Mudbutton Cursetosser; Silvergill Mentor |
| `Behold` | 12 | Champion of the Clachan; Champion of the Path; Champion of the Weird; Champions of the Perfect |
| `BlightOrPay` | 6 | Bogslither's Embrace; Burning Curiosity; Cinder Strike; Pyrrhic Strike |
| `Composite` | 6 | Wickerfolk Indomitable; Champion of the Clachan; Champion of the Path; Champion of the Weird |
| `ExileFromStorage` | 5 | Champion of the Clachan; Champion of the Path; Champion of the Weird; Champions of the Perfect |
| `ChoiceCost` | 3 | Souls of the Lost; Lethal Throwdown; Demand Answers |
| `PayLifeEqualToManaValueOfSpell` | 3 | Valgavoth, Terror Eater; Inside Information; Gwenom, Remorseless |

Also (1–2 cards each): `BlightVariable`, `ChooseEntity`, `ExileVariableCards`, `Forage`, `PayLifePerTarget`, `PayXLife`, `SacrificeCreaturesForCostReduction`

### `CardSource` — 13 gap subtypes

| subtype | cards | examples |
|---|--:|---|
| `ChosenTargets` | 72 | Maelstrom Pulse; Drafna's Restoration; Wishing Well; Iname, Life Aspect |
| `ControlledPermanents` | 51 | Magnetic Mountain; Teferi, Temporal Pilgrim; Teferi, Hero of Dominaria; Unwind |
| `AttachedToTarget` | 6 | Light of Judgment; Disarm; Soul Nova; Rhuk, Hexgold Nabber |
| `Self` | 6 | Estrid's Invocation; Hanabi Blast; Petals of Insight; Sphinx's Approach |
| `TriggeringEntity` | 6 | Deadeye Navigator; Lightning Mauler; Spectral Gateguards; Tandem Lookout |
| `FromVariable` | 4 | Curator of Destinies; Extrapolate the Impossible; Riddles in the Dark; Sauron's Ransom |
| `CreaturesThatSaddledSource` | 3 | Calamity, Galloping Inferno; Fortune, Loyal Steed; The Gitrog, Ravenous Ride |
| `ExiledAsCost` | 3 | Necropolis; Baron Helmut Zemo; Dollhouse of Horrors |

Also (1–2 cards each): `CraftedMaterials`, `EnteredViaThisResolution`, `LastKnownCombatPairedWithSource`, `LastKnownEquipmentAttachedToSource`, `TappedAsCost`

### `CostAtom` — 12 gap subtypes

| subtype | cards | examples |
|---|--:|---|
| `AtomCollectEvidence` | 8 | Conspiracy Unraveler; Cryptex; Forensic Researcher; Hedge Whisperer |
| `AtomRevealFromHand` | 5 | Flamekin Bladewhirl; Goldmeadow Stalwart; Silvergill Adept; Squeaking Pie Sneak |
| `AtomPutCountersOnSelf` | 3 | Mazemind Tome; Jessica Jones, Private Eye; Bloodletter Quill |

Also (1–2 cards each): `AtomDiscardHand`, `AtomExileFromGraveyardForTotal`, `AtomExileTopOfLibrary`, `AtomPayPlayerCounters`, `AtomPutCountersOnPermanent`, `AtomPutFromHandOnTopOfLibrary`, `AtomRevealNotedCreatureType`, `AtomSacrificeAll`, `AtomUnattach`

### `EffectTarget.SingleEntity` — 10 gap subtypes

| subtype | cards | examples |
|---|--:|---|
| `BoundVariable` | 130 | Crumble; Sheltering Word; Driftgloom Coyote; Hunter's Talent |
| `IterationEntity` | 11 | Coordinated Clobbering; Bartz and Boko; Thorin, Mountain-king; Sovereign Okinec Ahau |
| `AffectedEntity` | 7 | Titania's Song; Xenic Poltergeist; Takeno, Samurai General; March of the Machines |
| `EnchantedCreature` | 4 | Pain for All; Farrel's Mantle; Golem-Skin Gauntlets; With Great Power . . . |
| `PipelineTarget` | 4 | Monstrous Emergence; Close Encounter; Bolg of the North; Crush Underfoot |
| `AmassedArmy` | 3 | Foray of Orcs; Grishnákh, Brash Instigator; Surrounded by Orcs |

Also (1–2 cards each): `DiscardedAsCost`, `EquippedCreature`, `LibraryTop`, `RingBearer`

### `EntityNumericProperty` — 10 gap subtypes

| subtype | cards | examples |
|---|--:|---|
| `ManaSpent` | 13 | Unravel; Ambitious Augmenter; Berta, Wise Extrapolator; Cuboid Colony |
| `AttachmentCount` | 11 | Kitsune Mystic; Champion of the Flame; Valduk, Keeper of the Flame; Shagrat, Loot Bearer |
| `ExcessMarkedDamage` | 6 | Goblin Negotiation; Bolg of the North; Hell to Pay; Archaic's Agony |
| `ColorCount` | 3 | Ancient Cornucopia; Ramos, Dragon Engine; Dragonfire Blade |

Also (1–2 cards each): `BasePower`, `ColoredManaSymbolCount`, `DamageDealtThisTurn`, `KeywordValue`, `SubtypeCount`, `ValueChosenAsEntered`

### `ManaColorSet` — 9 gap subtypes

| subtype | cards | examples |
|---|--:|---|
| `ManaColorSet.Specific` | 21 | The Emperor of Palamecia; Vivi Ornitier; Thriving Bluff; Thriving Grove |
| `ManaColorSet.SourceChosenColor` | 17 | Tarnation Vista; Night Market; Valgavoth's Lair; Ashling, Rekindled |
| `ManaColorSet.Union` | 5 | Thriving Bluff; Thriving Grove; Thriving Heath; Thriving Isle |
| `ManaColorSet.CommanderIdentity` | 3 | Arcane Signet; Command Tower; Path of Ancestry |

Also (1–2 cards each): `ManaColorSet.AmongCardsInGraveyard`, `ManaColorSet.AmongLinkedExiledCards`, `ManaColorSet.AmongPermanents`, `ManaColorSet.ColorsOf`, `ManaColorSet.LandsCouldProduce`

### `SelectionRestriction` — 9 gap subtypes

| subtype | cards | examples |
|---|--:|---|
| `ReducedMinimumIfMatches` | 10 | Bandit's Talent; Thirst for Identity; Alpharael, Dreaming Acolyte; Steamcore Scholar |
| `OnePerCardName` | 8 | Omenpath Journey; Gifts Ungiven; Reach the Horizon; Extrapolate the Impossible |
| `TotalManaValueAtMost` | 3 | Lively Dirge; Eddie Brock; Michelangelo's Technique |

Also (1–2 cards each): `MaxAffordablePayment`, `OnePerBasicLandType`, `OnePerCardType`, `OnePerColor`, `OnePerPower`, `TotalPowerAtMost`

### `ControllerPredicate` — 8 gap subtypes

| subtype | cards | examples |
|---|--:|---|
| `ControlledByReferencedPlayer` | 33 | Kusari-Gama; Yosei, the Morning Star; Huntmaster of the Fells; Fear of Burning Alive |
| `ControlledByTriggeringPlayer` | 10 | Nature's Will; Tooth Collector; Topplegeist; Dreadmaw's Ire |
| `ControlledByTargetPlayer` | 5 | Radiating Lightning; Face Yourself; Tsabo's Decree; Incite War |
| `ControlledByTargetOpponent` | 4 | Dwarven Catapult; Rain of Daggers; Overwhelming Forces; Simoon |
| `ControllerAnd` | 4 | Obelisk of Undoing; Valgavoth, Terror Eater; Coveted Falcon; Laughing Jasper Flint |
| `ControlledByActivePlayer` | 3 | Temporal Distortion; Unstable Glyphbridge; Nettling Imp |

Also (1–2 cards each): `OwnedByTargetPlayer`, `OwnedByTriggeringPlayer`

### `SuccessCriterion` — 7 gap subtypes

| subtype | cards | examples |
|---|--:|---|
| `SuccessCriterion.CollectionNonEmpty` | 39 | Extrapolate the Impossible; Sphinx's Approach; Chandra, Torch of Defiance; Adder-Staff Boggart |
| `SuccessCriterion.PermanentsSacrificed` | 9 | Blood Speaker; Safe Haven; Tenured Tethermage; The Misty Mountains Cold |
| `SuccessCriterion.ControlChanged` | 5 | Kain, Traitorous Dragoon; Stiltzkin, Moogle Merchant; Volatile Stormdrake; Coveted Falcon |
| `SuccessCriterion.Always` | 4 | Oblivious Bookworm; Vaultguard Trooper; Sauron, the Dark Lord; Narset, Jeskai Waymaster |

Also (1–2 cards each): `SuccessCriterion.CountersRemoved`, `SuccessCriterion.DamageDealt`, `SuccessCriterion.TurnedFaceUp`

### `AttackPredicate` — 6 gap subtypes

| subtype | cards | examples |
|---|--:|---|
| `AttacksAlone` | 22 | Grunn, the Lonely King; Derelict Attic // Widow's Walk; Thoughtweft Imbuer; Seifer Almasy |
| `AttacksAlongsideGreaterPower` | 9 | Apprentice Sharpshooter; Cloaked Cadet; Gryff Rider; Gryffwing Cavalry |
| `AttacksFirstTimeEachTurn` | 3 | Godo, Bandit Warlord; Fear of Missing Out; Aurelia, the Warleader |

Also (1–2 cards each): `AttackerCountAtLeast`, `AttacksDefenderIsBattle`, `AttacksDefenderIsPlayer`

### `CostModification` — 6 gap subtypes

Also (1–2 cards each): `IncreaseColoredPerUnit`, `IncreaseGenericBy`, `IncreaseGenericPerOtherSpellThisTurn`, `IncreaseLife`, `ReduceColoredIfAnyTargetMatches`, `ReduceColoredPerUnit`

### `SpellCostTarget` — 6 gap subtypes

| subtype | cards | examples |
|---|--:|---|
| `YouCastFromZones` | 3 | Bilbo, Thief in the Night; Doc Aurlock, Grizzled Genius; Norman Osborn |

Also (1–2 cards each): `FaceDownYouCast`, `MorphActivation`, `OpponentsCast`, `OpponentsCastFromZones`, `OpponentsCastTargeting`

### `ActivationRestriction` — 5 gap subtypes

| subtype | cards | examples |
|---|--:|---|
| `Once` | 62 | Afterburner Expert; Boom Scholar; Boommobile; Camera Launcher |
| `MaxPerTurn` | 5 | Phyrexian Battleflies; Vampire Bats; Fire-Belly Changeling; Sewer Rats |
| `AnyPlayerMay` | 4 | Ifh-Bíff Efreet; Armageddon Clock; Oona's Prowler; Lethal Vapors |
| `DuringStep` | 4 | Desert; Armageddon Clock; Clockwork Avian; Kongming's Contraptions |

Also (1–2 cards each): `ControlledSinceYourMostRecentTurn`

### `CollectionFilter` — 5 gap subtypes

| subtype | cards | examples |
|---|--:|---|
| `ExcludeOtherCollection` | 15 | Clifftop Lookout; Demonic Junker; Skyserpent Seeker; House Cartographer |
| `GreatestManaValue` | 4 | Break Under Pressure; Psychic Battle; Ill-Timed Explosion; End of the Hunt |

Also (1–2 cards each): `GreatestPower`, `LeastToughness`, `SharesSubtypeWithSacrificed`

### `KeywordAbility` — 5 gap subtypes

| subtype | cards | examples |
|---|--:|---|
| `Dredge` | 12 | Darkblast; Golgari Brownscale; Golgari Grave-Troll; Golgari Thug |
| `Gift` | 3 | Kitnap; Scrapshooter; Starforged Sword |

Also (1–2 cards each): `Bestow`, `Escape`, `Variable`

### `ManaRestriction` — 5 gap subtypes

| subtype | cards | examples |
|---|--:|---|
| `SpellsWithManaValueAtLeast` | 3 | Helga, Skittish Seer; Ashling, Rekindled; Troyan, Gutsy Explorer |

Also (1–2 cards each): `CannotCastSpellsFromHand`, `CostsContainingXOnly`, `FaceDownSpellsOnly`, `SpellsOnly`

### `ManaSpellRider` — 4 gap subtypes

| subtype | cards | examples |
|---|--:|---|
| `GrantsKeywordWhenSpent` | 3 | Hall of the Bandit Lord; Carnelian Orb of Dragonkind; Arena of Glory |
| `MakesSpellUncounterable` | 3 | Cavern of Souls; Boseiju, Who Shelters All; Delighted Halfling |

Also (1–2 cards each): `CopySpellWhenSpent`, `ScryOnSharedTypeWithCommander`

### `CounterDestination` — 3 gap subtypes

| subtype | cards | examples |
|---|--:|---|
| `CounterDestination.Exile` | 9 | Syncopate; Thranduil's Decree; Kheru Spellsnatcher; Faerie Trickery |

Also (1–2 cards each): `CounterDestination.Hand`, `CounterDestination.Library`

### `DelayedTriggerExpiry` — 3 gap subtypes

| subtype | cards | examples |
|---|--:|---|
| `DelayedTriggerExpiry.UntilControllersNextTurn` | 3 | Tamiyo, Field Researcher; Garruk, Curse Breaker; Jace, Reality Sculptor |

Also (1–2 cards each): `DelayedTriggerExpiry.EndOfCombat`, `DelayedTriggerExpiry.Never`

### `MayPlayExpiry` — 3 gap subtypes

Also (1–2 cards each): `UntilSourceExilesAnother`, `WhileSourceOnBattlefield`, `WhileYouControlSource`

### `NumberProperty` — 3 gap subtypes

Also (1–2 cards each): `Even`, `Odd`, `Prime`

### `PayCost` — 3 gap subtypes

| subtype | cards | examples |
|---|--:|---|
| `Choice` | 4 | Starseer Mentor; Erosion; Perforating Artist; Thrull Wizard |

Also (1–2 cards each): `OwnManaCost`, `PayDynamicLife`

### `TargetRequirement` — 3 gap subtypes

| subtype | cards | examples |
|---|--:|---|
| `TargetOther` | 40 | Mabel's Mettle; Nesting Grounds; Clammy Prowler; Screaming Nemesis |
| `TargetSpellOrPermanent` | 10 | Eight-and-a-Half-Tails; Swat Away; Fatehold Charm; Blind Seer |
| `TargetPermanentOrPlayer` | 5 | Ayara, Widow of the Realm; Invasion of Regatha; Onakke Javelineer; Powerful Broker |

### `CounterCondition` — 2 gap subtypes

| subtype | cards | examples |
|---|--:|---|
| `CounterCondition.UnlessPaysMana` | 28 | Dazzling Denial; Soratami Savant; Diversion Unit; Spectral Interference |
| `CounterCondition.UnlessPaysDynamic` | 13 | Syncopate; Mausoleum Wanderer; Swallowed by Leviathan; Mindswipe |

### `CounterTarget` — 2 gap subtypes

| subtype | cards | examples |
|---|--:|---|
| `CounterTarget.Ability` | 5 | Squelch; Brown Ouphe; Bind; Tishana's Tidebinder |
| `CounterTarget.SpellOrAbility` | 5 | Louisoix's Sacrifice; Teferi's Response; Consign to Memory; Spider-Sense |

### `FeasibilityCheck` — 2 gap subtypes

| subtype | cards | examples |
|---|--:|---|
| `ControlsPermanentMatching` | 23 | Bushy Bodyguard; Corpseberry Cultivator; Curious Forager; Rottenmouth Viper |
| `HasCardsInZone` | 19 | Bushy Bodyguard; Corpseberry Cultivator; Curious Forager; Rottenmouth Viper |

### `IterationSpace` — 2 gap subtypes

| subtype | cards | examples |
|---|--:|---|
| `IterationSpace.Collection` | 42 | Magnetic Mountain; Dread Summons; Pore Over the Pages; Push the Limit |

Also (1–2 cards each): `IterationSpace.ColorsOf`

### `PlayerRankMetric` — 2 gap subtypes

Also (1–2 cards each): `CreaturesOfSubtype`, `LifeTotal`

### `Recipient` — 2 gap subtypes

| subtype | cards | examples |
|---|--:|---|
| `RecipientAnyOf` | 17 | Far Fortune, End Boss; Sygg, Wanderwine Wisdom; Twinflame Tyrant; Aetherblade Agent |

Also (1–2 cards each): `RecipientAnotherPlayer`

### `RepeatCondition` — 2 gap subtypes

| subtype | cards | examples |
|---|--:|---|
| `WhileCondition` | 7 | Struggle for Sanity; Mana Clash; The Tale of Tamiyo; Sin, Spira's Punishment |

Also (1–2 cards each): `PlayerChooses`

### `WardCost` — 2 gap subtypes

| subtype | cards | examples |
|---|--:|---|
| `WardCost.Mana` | 18 | Innkeeper's Talent; Long River Lurker; Combat Research; Sheltered by Ghosts |

Also (1–2 cards each): `WardCost.Life`

### `AmountFilter` — 1 gap subtypes

Also (1–2 cards each): `AmountAtMost`

### `CardDestination` — 1 gap subtypes

Also (1–2 cards each): `ToZoneExiledFrom`

### `CardMeasure` — 1 gap subtypes

Also (1–2 cards each): `MeasureColoredManaSymbols`

### `CastRestriction` — 1 gap subtypes

Also (1–2 cards each): `OnlyDuringPhase`

### `CostGating` — 1 gap subtypes

| subtype | cards | examples |
|---|--:|---|
| `NthOfTypePerTurn` | 7 | Eluge, the Shoreless Sea; Uthros Psionicist; Serah Farron; Radagast of Rhosgobel |

### `CounterTargetSource` — 1 gap subtypes

| subtype | cards | examples |
|---|--:|---|
| `CounterTargetSource.TriggeringEntity` | 9 | Mana Vortex; Boromir, Warden of the Tower; Chalice of the Void; Blood Funnel |

### `DamagePredicate` — 1 gap subtypes

Also (1–2 cards each): `DamageSourceSoleTargetIsRecipient`

### `DamageType` — 1 gap subtypes

| subtype | cards | examples |
|---|--:|---|
| `DamageNonCombat` | 14 | Soul-Scar Mage; Fear of Burning Alive; The Rollercrusher Ride; Crystal Barricade |

### `Gate` — 1 gap subtypes

| subtype | cards | examples |
|---|--:|---|
| `Gate.DoAction` | 115 | Mishra's War Machine; Whiskerquill Scribe; Blood Speaker; Iname, Life Aspect |

### `PlotCostTarget` — 1 gap subtypes

Also (1–2 cards each): `YouPlotFromHand`

### `RetargetChooser` — 1 gap subtypes

Also (1–2 cards each): `RetargetChooser.OwnerOfStored`

### `Scope` — 1 gap subtypes

| subtype | cards | examples |
|---|--:|---|
| `SoulbondPair` | 4 | Deadeye Navigator; Lightning Mauler; Spectral Gateguards; Tandem Lookout |

### `SelectionMode` — 1 gap subtypes

Also (1–2 cards each): `ChooseSpell`

### `UnlockCostTarget` — 1 gap subtypes

Also (1–2 cards each): `YouUnlock`
