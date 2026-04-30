# Reference: DSL Facades, Effects, Keywords, and Test Helpers

## DSL Facades (Primary API)

Always use these facades for card definitions. They provide type-safe factory methods that abstract away raw
constructors.

| Facade           | Import                                   | Purpose                                  |
|------------------|------------------------------------------|------------------------------------------|
| `Effects`        | `com.wingedsheep.sdk.dsl.Effects`        | Build effect objects                     |
| `Targets`        | `com.wingedsheep.sdk.dsl.Targets`        | Build target requirements                |
| `Triggers`       | `com.wingedsheep.sdk.dsl.Triggers`       | Build trigger objects                    |
| `Filters`        | `com.wingedsheep.sdk.dsl.Filters`        | Build filters for search/group/target    |
| `Costs`          | `com.wingedsheep.sdk.dsl.Costs`          | Build ability activation costs           |
| `Conditions`     | `com.wingedsheep.sdk.dsl.Conditions`     | Build conditions for conditional effects |
| `DynamicAmounts` | `com.wingedsheep.sdk.dsl.DynamicAmounts` | Build dynamic value expressions          |
| `EffectPatterns` | `com.wingedsheep.sdk.dsl.EffectPatterns` | Common multi-effect patterns             |

---

## Effects Facade

### Damage

- `Effects.DealDamage(amount: Int, target)` / `Effects.DealDamage(amount: DynamicAmount, target)`
- `Effects.DealXDamage(target)` — shorthand for X-value damage
- `Effects.Drain(amount, target)` — deal damage + gain life
- `Effects.Fight(target1, target2)` — two creatures fight

### Life

- `Effects.GainLife(amount, target = Controller)` — also accepts `DynamicAmount`
- `Effects.LoseLife(amount, target = TargetOpponent)` — also accepts `DynamicAmount`
- `Effects.LoseGame(target = Controller, message = null)` — target player loses the game immediately
- `Effects.ForceExileMultiZone(count: DynamicAmount, target = Controller)` — exile from battlefield, hand, or graveyard (Lich's Mastery)
- `Effects.SetLifeTotal(amount, target = Controller)` — set a player's life total to a specific value
- `Effects.ExchangeLifeAndPower(target = Self)` — exchange controller's life total with target creature's power

### Drawing

- `Effects.DrawCards(count, target = Controller)` — also accepts `DynamicAmount`
- `Effects.DrawUpTo(maxCards, target = Controller)` — draw up to N
- `Effects.EachPlayerMayDraw(maxCards, lifePerCardNotDrawn = 0)` — each player may draw up to N
- `Effects.Discard(count, target)`
- `Effects.DiscardRandom(count, target)` — discard at random
- `Effects.DiscardHand(target)` — discard entire hand
- `Effects.EachOpponentDiscards(count)` — each opponent discards
- `Effects.EachPlayerDrawsX(includeController, includeOpponents)` — each player draws X
- `Effects.EachPlayerReturnPermanentToHand()` — each player returns a permanent
- `Effects.ReadTheRunes()` — Read the Runes effect
- `Effects.ReplaceNextDraw(effect)` — replace next draw with effect
- `Effects.Loot(draw = 1, discard = 1)` — draw then discard

### Zone Movement (via `MoveToZoneEffect`)

- `Effects.Destroy(target)`
- `Effects.Exile(target)`
- `Effects.ExileUntilEndStep(target)` — exile, return at end step
- `Effects.ReturnToHand(target)`
- `Effects.PutOnTopOfLibrary(target)`
- `Effects.PutIntoLibraryNthFromTop(target, positionFromTop)` — put into library at specific position (0=top, 2=third from top)
- `Effects.ShuffleIntoLibrary(target)`
- `Effects.PutOntoBattlefield(target, tapped = false)`
- `Effects.PutOntoBattlefieldUnderYourControl(target)` — put target onto battlefield under controller's control
- `Effects.PutOntoBattlefieldFaceDown(target = Self, controllerOverride = Controller, fromZone = GRAVEYARD)` — put target onto battlefield face down as a 2/2 morph creature (Ashcloud Phoenix)
- `Effects.ReturnSelfToBattlefieldAttached(target)` — return self from graveyard to battlefield attached to target (for Auras like Dragon Shadow)
- `Effects.ExileUntilLeaves(target: EffectTarget)` — exile target and link to source via LinkedExileComponent (Banishing Light / Suspension Field ETB)
- `Effects.ExileGroupAndLink(filter: GroupFilter, storeAs)` — exile all matching permanents and link to source (Day of the Dragons ETB)
- `Effects.ReturnLinkedExile()` — return all cards linked via LinkedExileComponent to battlefield under controller's control (Day of the Dragons LTB)
- `Effects.ReturnLinkedExileUnderOwnersControl()` — return all linked exiled cards to battlefield under their owners' control (Planar Guide)
- `Effects.ReturnOneFromLinkedExile()` — return one owned card from linked exile to battlefield (Dimensional Breach upkeep trigger)
- `Effects.ReturnCreaturesPutInGraveyardThisTurn(player)` — return to hand all creature cards in graveyard that were put there this turn (Garna, the Bloodflame)
- `Effects.CreatePermanentGlobalTriggeredAbility(ability)` — create a permanent global triggered ability (for sorcery-created recurring triggers)
- `Effects.ShuffleGraveyardIntoLibrary(target)` — shuffle graveyard into library

### Stats & Keywords

- `Effects.ModifyStats(power, toughness, target = ContextTarget(0))` — until end of turn
- `Effects.ModifyStats(power: DynamicAmount, toughness: DynamicAmount, target)` — dynamic P/T
- `Effects.GrantHexproof(target = Controller, duration = EndOfTurn)` — grant hexproof to player or permanent
- `Effects.GrantKeyword(keyword, target = ContextTarget(0), duration = EndOfTurn)` — grant a keyword for a duration
- `Effects.GrantKeywordToAttackersBlockedBy(keyword, target, duration)` — grant keyword to attackers blocked by target
- `Effects.GrantExileOnLeave(target)` — exile instead of leaving battlefield (Kheru Lich Lord, Whip of Erebos)
- `Effects.AddCounters(counterType, count, target)`
- `Effects.AddDynamicCounters(counterType, amount: DynamicAmount, target)` — add a dynamic number of counters
- `Effects.AddCountersToCollection(collectionName, counterType, count)` — add counters to all entities in a named collection
- `Effects.SetBasePower(target = Self, power: DynamicAmount, duration = Permanent)` — set creature's base power
- `Effects.AnimateLand(target, power, toughness, duration)` — turn land into creature
- `Effects.DistributeCountersFromSelf(counterType)` — move counters from self to other creatures (player chooses)
- `Effects.DistributeCountersAmongTargets(totalCounters, counterType, minPerTarget)` — distribute N counters among targets from context (deterministic distribution)
- `Effects.Proliferate()` — choose any number of permanents and/or players that have a counter, then give each another counter of each kind already there

- `Effects.AddSubtype(subtype, target, duration = EndOfTurn)` — add a subtype to any permanent (creature, land, etc.) in addition to its other types; supports `fromChosenValueKey` for pipeline composition with `ChooseOptionEffect(BASIC_LAND_TYPE)`
- `Effects.ChooseColorForTarget(target = Self, prompt = "Choose a color")` — choose a color during resolution and store it on a target permanent for chosen-color static abilities
- `Effects.BecomeChosenManaColor(target = Self, duration = EndOfTurn)` — pair inside a mana ability `Composite` with `Effects.AddAnyColorMana(1)`; the target becomes the same color the controller picked when activating, for `duration` (uses `EffectContext.manaColorChoice`)
- `Effects.ChangeColor(target = ContextTarget(0), colors: Set<Color>, duration = EndOfTurn)` — replace the colors of a single target with `colors` for `duration`; pass `emptySet()` for colorless
- `Effects.BecomeAllColors(target = ContextTarget(0), duration = EndOfTurn)` — convenience wrapper that sets the target to all five colors (Tam, Mindful First-Year)

### Mass Effects (group)

- `Effects.DestroyAll(filter: GameObjectFilter, noRegenerate = false, storeDestroyedAs?)` — board wipe via pipeline; compose with `DrawCardsEffect(VariableReference("<key>_count"))` for "draw per destroyed" patterns
- `Effects.DestroyAllAndAttached(filter: GameObjectFilter, noRegenerate = false)` — destroy all matching permanents AND all permanents attached to them (End Hostilities pattern)
- `EffectPatterns.destroyAllExceptStoredSubtypes(noRegenerate, exceptSubtypesFromStored)` — destroy creatures except those with stored subtypes (Harsh Mercy pattern)
- `Effects.GrantKeywordToAll(keyword, filter: GroupFilter, duration)` — keyword to group
- `Effects.RemoveKeyword(keyword, target, duration)` — remove keyword from single target
- `Effects.RemoveKeywordFromAll(keyword, filter: GroupFilter, duration)` — remove keyword from group
- `Effects.LoseAllCreatureTypes(target, duration = EndOfTurn)` — remove all creature types from target creature
- `Effects.SetCreatureSubtypes(subtypes: Set<String>, target, duration)` — set creature subtypes on single target
- `Effects.ModifyStatsForAll(power, toughness, filter: GroupFilter, duration)` — P/T for group (Int or DynamicAmount overloads)
- `Effects.DealDamageToAll(amount, filter: GroupFilter)` — also accepts `DynamicAmount`
- `Effects.CantBlockGroup(filter: GroupFilter, duration)` — group can't block this turn
- `Effects.CantAttackOrBlock(target, duration)` — target creature can't attack or block this turn
- `EffectPatterns.returnAllToHand(filter: GroupFilter)` — return all matching permanents to owners' hands

### Control

- `Effects.GainControl(target, duration = Permanent)` — gain control of target
- `Effects.ExchangeControl(target1, target2)` — exchange control of two target creatures
- `Effects.GainControlByMostOfSubtype(subtype, target)` — control if you have most of subtype
- `Effects.GainControlOfGroup(filter: GroupFilter, duration)` — gain control of group
- `Effects.ChooseCreatureTypeGainControl(duration)` — choose type, gain control

### Protection

- `Effects.ChooseColorAndGrantProtection(filter: GroupFilter, duration)` — protection to group
- `Effects.ChooseColorAndGrantProtectionToTarget(target, duration)` — protection to target

### Mana

- `Effects.AddMana(color, amount = 1)` — also accepts `DynamicAmount`
- `Effects.AddColorlessMana(amount)` — also accepts `DynamicAmount`
- `Effects.AddAnyColorMana(amount = 1)`
- `Effects.AddAnyColorManaSpendOnChosenType(amount = 1)` — produces one mana of any color restricted to casting/activating sources of the source's chosen creature type (Eclipsed Realms)
- `Effects.AddManaOfColorAmong(filter)` — add one mana of any color among matching permanents (Mox Amber)
- `Effects.AddOneManaOfEachColorAmong(filter)` — add one mana of EACH color among matching permanents (Bloom Tender / Vivid mana ability)

### Tokens

- `Effects.CreateToken(power, toughness, colors, creatureTypes, keywords, count = 1, legendary = false)`
- `Effects.CreateDynamicToken(dynamicPower, dynamicToughness, colors, creatureTypes, keywords, count = 1)` — token with P/T evaluated at resolution time
- `Effects.CreateTokenCopyOfSelf(count = 1, overridePower = null, overrideToughness = null)` — create a token that's a copy of the source permanent (copies CardComponent + abilities via cardDefinitionId). For Offspring, use `overridePower = 1, overrideToughness = 1` to create a 1/1 copy.
- `CreateTokenCopyOfChosenPermanentEffect(filter)` — choose a permanent you control matching filter during resolution, create a token copy. Used for "Choose an artifact or creature you control. Create a token that's a copy of it."
- `Effects.CreateTokenCopyOfTarget(target, count = 1, overridePower = null, overrideToughness = null, tapped = false, attacking = false, triggeredAbilities = emptyList())` — create N token copies of a targeted permanent or pipeline-selected permanent. Use `overridePower`/`overrideToughness` for "except it's 1/1" effects, `tapped`/`attacking` for combat token copies, and `triggeredAbilities` for extra copied-token text.
- `Effects.CreateTreasure(count = 1)`
- `Effects.CreateFood(count = 1, controller: EffectTarget? = null)` — create Food artifact tokens; `controller` overrides who gets the tokens (e.g., `PlayerRef(Player.EachOpponent)` for Gift a Food)

### Library

- `Effects.EachPlayerRevealCreaturesCreateTokens(tokenPower, tokenToughness, tokenColors, tokenCreatureTypes, tokenImageUri?)` — each player reveals and creates tokens
- `Effects.EachPlayerSearchesLibrary(filter, count: DynamicAmount)` — each player searches
- `Effects.HeadGames(target)` — look at target's hand, rearrange library
- `Effects.Mill(count, target = Controller)` — also accepts `DynamicAmount` for variable mill
- `Effects.SearchLibrary(filter, count = 1, destination = HAND, entersTapped, shuffle, reveal)` — `count` can be `Int` or `DynamicAmount` (e.g., `DynamicAmount.AggregateBattlefield(..., DISTINCT_COLORS)` for Prismatic Undercurrents-style variable search)
- `Effects.SearchMultipleZones(zones, filter, count = 1, destination = BATTLEFIELD, entersTapped)` — search graveyard/hand/library for a card and put it onto destination
- `Effects.SearchLibraryNthFromTop(filter = Any, positionFromTop = 2)` — search, shuffle, put Nth from top (Long-Term Plans: positionFromTop=2)
- `Effects.Scry(count)` — returns CompositeEffect (Gather → Select → Move pipeline)
- `Effects.Surveil(count)` — returns CompositeEffect (Gather → Select → Move pipeline)
- `Effects.TakeFromLinkedExile()` — put top card of linked exile pile into hand
- `Effects.ExileFromTopRepeating(matchFilter, repeatIfManaValueAtLeast, damagePerCard)` — exile from top until match, put in hand, repeat if MV >= threshold, deal damage per card to hand (Demonlord Belzenlok)
- `Effects.ExileLibraryUntilManaValue(players = EachOpponent, threshold, storeAs)` — for each matching player, exile top of library until cumulative mana value ≥ threshold; accumulates exiled IDs in `storeAs` under the spell's controller so downstream `GrantMayPlayFromExileEffect` / `GrantPlayWithoutPayingCostEffect` grant permission to the caster (Dream Harvest)

### Stack

- `Effects.CounterSpell()`
- `Effects.CounterSpellToExile(grantFreeCast: Boolean = false)` — counter target spell; exile instead of graveyard; optionally grant free cast from exile permanently
- `Effects.CounterTriggeringSpell()` — counter the spell that triggered this ability (non-targeted, uses triggering entity)
- `Effects.CounterAbility()` — counter target activated or triggered ability
- `Effects.CounterUnlessPays(cost: String)` — counter unless mana paid
- `Effects.CounterUnlessDynamicPays(amount: DynamicAmount, exileOnCounter: Boolean = false)` — counter unless dynamic amount paid; `exileOnCounter = true` exiles spell if countered (Syncopate)
- `Effects.ChangeSpellTarget(targetMustBeSource = false)` — redirect a spell's creature target to another creature
- `Effects.ChangeTarget()` — change the target of a spell or ability with a single target
- `Effects.ReselectTargetRandomly()` — reselect the target of the triggering spell/ability at random (Grip of Chaos)
- `Effects.CopyTargetSpell(target)` — copy target instant or sorcery spell on stack, may choose new targets
- `Effects.CopyNextSpellCast(copies)` — when you next cast an instant or sorcery spell this turn, copy it (Howl of the Horde)

### Sacrifice

- `Effects.Sacrifice(filter, count = 1, target = Controller)`
- `Effects.SacrificeTarget(target)` — sacrifice a specific permanent by target (for delayed triggers)

### Tap/Untap

- `Effects.Tap(target)` / `Effects.Untap(target)`
- `Effects.UntapGroup(filter: GroupFilter)` — untap all matching
- `Effects.TapAll(filter: GroupFilter)` — tap all matching

### Permanent Manipulation

- `Effects.SeparatePermanentsIntoPiles(target)` — separate into piles
- `Effects.Provoke(target)` — untap target and force it to block source (provoke keyword)
- `Effects.ForceBlock(target)` — force target creature to block source this combat if able (no untap, unlike Provoke)
- `Effects.PreventCombatDamageToAndBy(target)` — prevent all combat damage to and by creature this turn
- `Effects.RemoveFromCombat(target)` — remove creature from combat
- `Effects.GrantAttackBlockTaxPerCreatureType(target, creatureType, manaCostPer, duration)` — grant "can't attack or block unless pays {X} per creature type" (Whipgrass Entangler)
- `RedirectCombatDamageToControllerEffect(target)` — next time creature deals combat damage this turn, deals it to controller instead (Goblin Psychopath)
- `Effects.AttachEquipment(target)` — attach this equipment to target creature (for equip abilities)
- `Effects.AttachTargetEquipmentToCreature(equipmentTarget, creatureTarget)` — attach a targeted Equipment to a targeted creature (both explicit targets, not source)
- `Effects.DeflectNextDamageFromChosenSource()` — choose a source, prevent next damage from it, deal that much to source's controller (Deflecting Palm)
- `Effects.PreventNextDamageFromChosenSource(amount, target)` — choose a source, prevent next N damage from it to target (Healing Grace)

### Chain Copy (Chain of X)

- `Effects.DestroyAndChainCopy(target, targetFilter, spellName)` — destroy + chain copy (Chain of Acid)
- `Effects.BounceAndChainCopy(target, targetFilter, spellName)` — bounce + sacrifice land to copy (Chain of Vapor)
- `Effects.DamageAndChainCopy(amount, target, spellName)` — damage + discard to copy (Chain of Plasma)
- `Effects.DiscardAndChainCopy(count, target, spellName)` — discard + chain copy (Chain of Smog)
- `Effects.PreventDamageAndChainCopy(target, targetFilter, spellName)` — prevent damage + sacrifice land to copy (Chain of Silence)

### Composite & Control Flow

- `Effects.Composite(vararg effects)` — or use `effect1 then effect2` infix operator
- `Effects.RepeatWhile(body, repeatCondition)` — repeat while condition met
- `Effects.SelectTarget(requirement, storeAs)` — select and store a target

---

## All Effect Types (in scripting/effect/)

### Damage

| Effect                      | Parameters                                                      | Purpose                   |
|-----------------------------|-----------------------------------------------------------------|---------------------------|
| `DealDamageEffect`          | `amount: DynamicAmount, target, cantBePrevented, damageSource?` | Damage to target(s). Supports multi-player targets via `PlayerRef` (e.g., `Player.Each`, `Player.EachOpponent`) |
| `DividedDamageEffect`       | `totalDamage, minTargets, maxTargets`                           | Divided damage allocation |
| `FightEffect`               | `target1, target2`                                              | Two creatures fight       |
| `ChainCopyEffect`           | `action, target, targetFilter?, copyRecipient, copyCost, copyTargetRequirement, spellName` | Unified chain copy (all Chain of X) |

### Life

| Effect                                   | Parameters                       | Purpose                   |
|------------------------------------------|----------------------------------|---------------------------|
| `GainLifeEffect`                         | `amount: DynamicAmount, target`  | Gain life                 |
| `LoseLifeEffect`                         | `amount: DynamicAmount, target`  | Lose life                 |
| `LoseGameEffect`                         | `target, message?`               | Target player loses the game |
| `PayLifeEffect`                          | `amount: Int`                    | Pay life cost             |
| `LoseHalfLifeEffect`                     | `roundUp, target`                | Lose half life total      |
| `OwnerGainsLifeEffect`                   | `amount: DynamicAmount`          | Card owner gains life     |
| `SetLifeTotalEffect`                     | `amount: DynamicAmount, target: EffectTarget` | Set a player's life total |

### Drawing & Hand

| Effect                                   | Parameters                         | Purpose                         |
|------------------------------------------|------------------------------------|---------------------------------|
| `DrawCardsEffect`                        | `count: DynamicAmount, target`     | Draw cards                      |
| `DrawUpToEffect`                         | `maxCards: Int, target, lifePerCardNotDrawn` | Draw up to N (with optional life gain) |
| `EachPlayerDiscardsOrLoseLifeEffect`     | `lifeLoss: Int`                    | Discard or lose life            |
| `EachPlayerReturnsPermanentToHandEffect` | (object)                           | Each player returns a permanent |
| `LookAtTargetHandEffect`                 | `target`                           | Look at target's hand           |
| `LookAtFaceDownCreatureEffect`           | `target`                           | Look at a face-down creature    |
| `LookAtAllFaceDownCreaturesEffect`       | `target`                           | Look at all face-down creatures |
| `RevealHandEffect`                       | `target`                           | Reveal hand                     |
| `ReplaceNextDrawWithEffect`              | `replacementEffect: Effect`        | Replace next draw               |
| `ReadTheRunesEffect`                     | (object)                           | Read the Runes                  |

### Removal & Zone Movement

| Effect                                      | Parameters                                            | Purpose                  |
|---------------------------------------------|-------------------------------------------------------|--------------------------|
| `MoveToZoneEffect`                          | `target, destination: Zone, placement, byDestruction, controllerOverride, linkToSource` | Unified zone movement    |
| `SacrificeEffect`                           | `filter, count, any, excludeSource`                   | Sacrifice permanents     |
| `SacrificeSelfEffect`                       | (object)                                              | Sacrifice this permanent |
| `SacrificeTargetEffect`                     | `target`                                              | Sacrifice specific permanent by target |
| `ForceSacrificeEffect`                      | `filter, count, target`                               | Force opponent sacrifice |
| `ForceReturnOwnPermanentEffect`             | `filter, excludeSource`                               | Controller returns own permanent to hand |
| `RegenerateEffect`                          | `target`                                              | Regenerate               |
| `CantBeRegeneratedEffect`                   | `target`                                              | Prevents regeneration    |
| `MarkExileOnDeathEffect`                    | `target`                                              | Mark for exile on death  |
| `MarkExileControllerGraveyardOnDeathEffect` | `target`                                              | When dies, exile ctrl's GY |
| `ExileUntilLeavesEffect`                    | `target`                                              | O-Ring style exile       |
| `ExileAndReplaceWithTokenEffect`            | `target, tokenPower/Toughness/Colors/Types/Keywords`  | Exile + token            |
| `SeparatePermanentsIntoPilesEffect`         | `target`                                              | Separate into piles      |
| `DestroyAtEndOfCombatEffect`                | `target`                                              | Destroy at end of combat |
| `SacrificeAtEndOfCombatEffect`              | `target`                                              | Sacrifice at end of combat |
| ~~`DestroyAllSharingTypeWithSacrificedEffect`~~ | `noRegenerate` — **Deprecated**: use `Effects.DestroyAllSharingTypeWithSacrificed()` | Destroy all sharing type |
| `HarshMercyEffect`                          | (object)                                              | Harsh Mercy              |
| `Effects.PatriarchsBidding()`               | (pipeline pattern)                                    | Patriarch's Bidding      |
| `ExileUntilLeavesEffect`                    | `target: EffectTarget`                                | Exile target + link to source |
| `ExileGroupAndLinkEffect`                   | `filter: GroupFilter, storeAs`                        | Exile group + link to source |
| `ReturnLinkedExileEffect`                   | `underOwnersControl: Boolean = false`                 | Return linked exiled cards |

### Permanent Modification

| Effect                                      | Parameters                                                                  | Purpose                                  |
|---------------------------------------------|-----------------------------------------------------------------------------|------------------------------------------|
| `TapUntapEffect`                            | `target, tap: Boolean`                                                      | Tap or untap                             |
| `TapTargetCreaturesEffect`                  | `maxTargets`                                                                | Tap up to X targets                      |
| `ModifyStatsEffect`                         | `power: DynamicAmount, toughness: DynamicAmount, target, duration`          | P/T for single target                    |
| `GrantKeywordEffect`          | `keyword, target, duration`                                                 | Keyword for single target                |
| `GrantExileOnLeaveEffect`     | `target`                                                                    | Exile instead of leaving battlefield     |
| `RemoveKeywordEffect`         | `keyword, target, duration`                                                 | Remove keyword from single target        |
| `RemoveAllAbilitiesEffect`    | `target, duration`                                                          | Remove all abilities from target         |
| `GrantTriggeredAbilityEffect` | `ability, target, duration`                                                 | Grant triggered ability                  |
| `GrantActivatedAbilityEffect` | `ability, target, duration`                                                 | Grant activated ability                  |
| `GrantActivatedAbilityToGroupEffect`        | `ability, filter, duration`                                                 | Grant activated ability to group         |
| `AddCountersEffect`                         | `counterType, count, target`                                                | Add counters                             |
| `TapUntapCollectionEffect`                  | `collectionName, tap`                                                       | Tap/untap all entities in a named collection |
| `AddCountersToCollectionEffect`             | `collectionName, counterType, count`                                        | Add counters to all entities in collection |
| `RemoveCountersEffect`                      | `counterType, count, target`                                                | Remove counters                          |
| `AddMinusCountersEffect`                    | `count, target`                                                             | Add -1/-1 counters                       |
| `LoseAllCreatureTypesEffect`                | `target, duration`                                                          | Remove all creature types                |
| `TransformAllCreaturesEffect`               | `target, loseAllAbilities, addCreatureType, setBasePower, setBaseToughness` | Transform creatures                      |
| `ChangeCreatureTypeTextEffect`              | `target, excludedTypes`                                                     | Change creature type text                |
| `BecomeCreatureTypeEffect`                  | `target, duration, excludedTypes`                                           | Become a creature type                   |
| `BecomeChosenTypeAllCreaturesEffect`        | `excludedTypes, duration`                                                   | All creatures become chosen type         |
| `SetCreatureSubtypesEffect`                 | `subtypes, target, duration`                                                | Set single target subtypes               |
| `SetGroupCreatureSubtypesEffect`            | `subtypes, filter, duration`                                                | Set group subtypes                       |
| `ChangeGroupColorEffect`                    | `colors, filter, duration`                                                  | Change group color                       |
| `ChangeColorEffect`                         | `target, colors, duration`                                                  | Change a single target's colors          |
| `ChooseColorForTargetEffect`                | `target, prompt`                                                            | Choose and store color on a permanent    |
| `BecomeChosenManaColorEffect`               | `target, duration`                                                          | Target becomes the mana-ability color    |
| `ChooseCreatureTypeModifyStatsEffect`       | `power: DynamicAmount, toughness: DynamicAmount, duration, grantKeyword?`   | Choose type, modify stats (+ keyword)    |
| `Effects.ChooseCreatureTypeUntap()`         | (pipeline pattern)                                                          | Choose type, untap all of that type      |
| `GainControlEffect`                         | `target, duration`                                                          | Gain control                             |
| `GainControlByActivePlayerEffect`           | `target`                                                                    | Active player gains control              |
| `GainControlByMostOfSubtypeEffect`          | `subtype, target`                                                           | Control by most of subtype               |
| `GiveControlToTargetPlayerEffect`           | `permanent, newController, duration`                                        | Give control to target                   |
| `ChooseCreatureTypeGainControlEffect`       | `duration`                                                                  | Choose type, gain control                |
| `GrantToEnchantedCreatureTypeGroupEffect`   | `powerModifier, toughnessModifier, keyword?, protectionColors, duration`    | Grant to enchanted creature's type group |
| `SetBasePowerEffect`                        | `target, power: DynamicAmount, duration`                                    | Set base power only (leave toughness)    |
| `SetBasePowerToughnessEffect`               | `target, power: Int, toughness: Int, duration`                              | Set base P/T via floating effect         |
| `AnimateLandEffect`                         | `target, power, toughness, duration`                                        | Animate land                             |
| `DistributeCountersFromSelfEffect`          | `counterType`                                                               | Move counters from self to other creatures |
| `DistributeCountersAmongTargetsEffect`      | `totalCounters, counterType, minPerTarget`                                  | Distribute N counters among targets        |
| `ProliferateEffect`                         | (data object)                                                               | Add one of each existing counter kind to any number of chosen permanents/players |
| `TurnFaceDownEffect`                        | `target`                                                                    | Turn face down                           |
| `TurnFaceUpEffect`                          | `target`                                                                    | Turn face up                             |
| `TransformEffect`                           | `target`                                                                    | Transform DFC                            |
| `RemoveFromCombatEffect`                    | `target`                                                                    | Remove from combat                       |
| `IncrementAbilityResolutionCountEffect`     | (data object)                                                               | Track ability resolution count per turn  |

### Library

| Effect                                                                              | Parameters                                          | Purpose                                           |
|-------------------------------------------------------------------------------------|-----------------------------------------------------|---------------------------------------------------|
| `EffectPatterns.lookAtTargetLibraryAndDiscard(count, toGraveyard)`                  | `count, toGraveyard`                                | Look at target's library, discard some (pipeline) |
| `EffectPatterns.lookAtTopAndKeep(count, keepCount)`                                 | `count, keepCount, keepDest?, restDest?, revealed?` | Look at top N keep some (pipeline)                |
| `EffectPatterns.lookAtTopAndReorder(count)`                                         | `count: Int` or `count: DynamicAmount`              | Look at top and reorder (pipeline)                |
| `EffectPatterns.lookAtTopXAndPutOntoBattlefield(countSource, filter, shuffleAfter)` | CoCo-style (pipeline)                               |
| `EffectPatterns.searchAndExileLinked(count, filter)`                                | Search library, exile linked to source             |
| `EffectPatterns.searchTargetLibraryExile(count, filter)`                            | Search target's library and exile (pipeline)        |
| `Effects.ShuffleAndExileTopPlayFree()`                                              | (none)                                              | Shuffle + exile top + grant exile+free play (Mind's Desire) |
| `EffectPatterns.shuffleAndExileTopPlayFree()`                                       | (none)                                              | Pipeline: shuffle, exile top, grant exile+free play |
| `EffectPatterns.putCreatureFromHandSharingTypeWithTapped()`                          | (none)                                              | Pipeline: Gather tapped subtypes → filter hand → select → move to battlefield |
| `ShuffleLibraryEffect`                                                              | `target`                                            | Shuffle library                                   |
| `TakeFromLinkedExileEffect`                                                         | (object)                                            | Put top card of linked exile pile into hand       |
| `GrantMayPlayFromExileEffect`                                                       | `from`                                              | Grant play-from-exile permission to cards in collection |
| `GrantPlayWithoutPayingCostEffect`                                                  | `from`                                              | Grant play-without-paying-cost to cards in collection  |
| `GrantPlayWithAdditionalCostEffect`                                                 | `from, additionalCost`                              | Grant play-from-exile with additional cost (e.g., discard) |
| `GrantFreeCastTargetFromExileEffect`                                                | `target, exileAfterResolve`                         | Grant single target in exile free cast + optional exile-after-resolve |
| `ExileFromTopRepeatingEffect`                                                       | `matchFilter, repeatIfManaValueAtLeast, damagePerCard` | Exile from top until match, put in hand, repeat if MV >= threshold, deal damage (Demonlord Belzenlok) |
| `ExileLibraryUntilManaValueEffect`                                                  | `players, threshold, storeAs`                       | For each matching player, exile top of library until cumulative mana value ≥ threshold; accumulate IDs in collection (Dream Harvest) |

### Mana

| Effect                   | Parameters                                   | Purpose            |
|--------------------------|----------------------------------------------|--------------------|
| `AddManaEffect`          | `color, amount: DynamicAmount`               | Add colored mana   |
| `AddColorlessManaEffect` | `amount: DynamicAmount`                      | Add colorless mana |
| `AddAnyColorManaEffect`  | `amount: DynamicAmount`                      | Add any color mana |
| `AddAnyColorManaSpendOnChosenTypeEffect` | `amount: DynamicAmount`          | Add one mana of any color restricted to spells/abilities of the source's chosen creature subtype (Eclipsed Realms) |
| `AddDynamicManaEffect`   | `amountSource: DynamicAmount, allowedColors` | Dynamic mana       |
| `AddManaOfColorAmongEffect` | `filter: GameObjectFilter`                | Add mana of color among matching permanents (Mox Amber) |
| `AddOneManaOfEachColorAmongEffect` | `filter: GameObjectFilter`         | Add one mana of EACH color found among matching permanents (Bloom Tender) |

### Tokens

| Effect                           | Parameters                                                                                  | Purpose                        |
|----------------------------------|---------------------------------------------------------------------------------------------|--------------------------------|
| `CreateTokenEffect`              | `count: DynamicAmount, power, toughness, colors, creatureTypes, keywords, name?, imageUri?, tapped?, attacking?, legendary?, exileAtStep?` | Create token (tapped = enter tapped, attacking = enter attacking, legendary = legendary supertype, exileAtStep = create delayed trigger to exile tokens at that step) |
| `CreateChosenTokenEffect`        | `dynamicPower: DynamicAmount, dynamicToughness: DynamicAmount`                              | Create token with chosen stats |
| `CreatePredefinedTokenEffect`    | `tokenType: String, count: Int, controller: EffectTarget?`                                  | Create predefined tokens (Treasure, Food, Lander, Sword, Cragflame). Use `Effects.CreateTreasure()`, `Effects.CreateFood()`, `Effects.CreateLander()` facades, or `CreatePredefinedTokenEffect(name)` directly. |
| `CreateTokenCopyOfSourceEffect`  | `count: Int`                                                                                | Create token copy of source permanent |
| `CreateTokenFromGraveyardEffect` | `power, toughness, colors, creatureTypes`                                                   | Token from graveyard           |

### Composite & Control Flow

| Effect                       | Parameters                                            | Purpose                        |
|------------------------------|-------------------------------------------------------|--------------------------------|
| `CompositeEffect`            | `effects: List<Effect>`                               | Chain multiple effects         |
| `MayEffect`                  | `effect, descriptionOverride?`                        | "You may..."                   |
| `ModalEffect`                | `modes: List<Mode>, chooseCount`                      | "Choose one/two..."            |
| `BudgetModalEffect`          | `budget: Int, modes: List<BudgetMode>`                | "Choose up to N worth of modes" (Season cycle) |
| `OptionalCostEffect`         | `cost, ifPaid, ifNotPaid?`                            | "You may [cost]. If you do..." |
| `ReflexiveTriggerEffect`     | `action, optional, reflexiveEffect`                   | "When you do..."               |
| `PayOrSufferEffect`          | `cost: PayCost, suffer, player`                       | "Unless [cost], [suffer]"      |
| `StoreResultEffect`          | `effect, storeAs: EffectVariable`                     | Store result for later         |
| `StoreCountEffect`           | `effect, storeAs`                                     | Store count for later          |
| `CreateDelayedTriggerEffect` | `step: Step, effect, fireOnlyOnControllersTurn = false` | Create delayed trigger at step |
| `BlightEffect`               | `blightAmount: DynamicAmount, innerEffect, targetId?` | Blight effect                  |
| `TapCreatureForEffectEffect` | `innerEffect, targetId?`                              | Tap creature for effect        |
| `MayPayManaEffect`           | `cost: ManaCost, effect`                              | May pay mana for effect        |
| `MayPayXForEffect`           | `effect: Effect`                                      | May pay {X} for effect (auto-taps) |
| `AnyPlayerMayPayEffect`      | `cost: PayCost, consequence`                          | Any player may pay             |
| `ForEachTargetEffect`        | `effects: List<Effect>`                               | Iterate per target             |
| `ForEachPlayerEffect`        | `players: Player, effects: List<Effect>`              | Iterate per player             |
| `FlipCoinEffect`             | `wonEffect?, lostEffect?`                             | Flip a coin                    |
| `RepeatWhileEffect`          | `body, repeatCondition`                               | Repeat while condition         |
| `RepeatDynamicTimesEffect`   | `amount: DynamicAmount, body: Effect`                 | Repeat body N times            |
| `SelectTargetEffect`         | `requirement, storeAs`                                | Select and store a target      |

### Combat

| Effect                                              | Parameters                                  | Purpose                         |
|-----------------------------------------------------|---------------------------------------------|---------------------------------|
| `MustBeBlockedEffect`                               | `target`                                    | Must be blocked                 |
| `TauntEffect`                                       | `target`                                    | Lure                            |
| `ReflectCombatDamageEffect`                         | `target`                                    | Reflect combat damage           |
| `PreventCombatDamageFromEffect`                     | `source: GroupFilter, duration`             | Fog                             |
| `PreventDamageFromAttackingCreaturesThisTurnEffect` | (object)                                    | Prevent from attackers          |
| `PreventAllCombatDamageThisTurnEffect`              | (object)                                    | Prevent all combat damage       |
| `GrantCantBeBlockedExceptByColorEffect`             | `filter, canOnlyBeBlockedByColor, duration` | Color evasion                   |
| `GrantKeywordToAttackersBlockedByEffect`            | `target, keyword, duration`                 | Grant keyword to blocked attackers |
| `ProvokeEffect`                                     | `target`                                    | Untap + force block source      |
| `CantBlockGroupEffect`                              | `filter: GroupFilter, duration`             | Group can't block this turn     |
| `CantBlockEffect`                                   | `target, duration`                          | Target can't block this turn    |
| `CantAttackEffect`                                  | `target, duration`                          | Target can't attack this turn   |
| `PreventNextDamageEffect`                           | `amount: DynamicAmount, target`             | Prevent next N damage           |
| `RemoveFromCombatEffect`                            | `target`                                    | Remove from combat              |
| `MarkMustAttackThisTurnEffect`                      | `target`                                    | Mark creature must attack       |
| `RedirectNextDamageEffect`                          | `protectedTargets, redirectTo, amount?`     | Redirect next damage (amount=null for all) |
| `PreventNextDamageFromChosenCreatureTypeEffect`     | (object)                                    | Prevent damage from chosen type |
| `DeflectNextDamageFromChosenSourceEffect`           | (object)                                    | Choose source, prevent + reflect damage to source's controller |
| `PreventNextDamageFromChosenSourceEffect`           | `amount, target`                            | Choose source, prevent next N damage from it to target |
| `GrantAttackBlockTaxPerCreatureTypeEffect`          | `target, creatureType, manaCostPer, duration` | Can't attack/block unless pays per type |
| `PreventAllDamageDealtByTargetEffect`               | `target`                                    | Prevent all damage by target    |
| `PreventCombatDamageToAndByEffect`                  | `target`                                    | Prevent combat damage to and by creature |

### Player

| Effect                                             | Parameters                               | Purpose                            |
|----------------------------------------------------|------------------------------------------|------------------------------------|
| `SkipCombatPhasesEffect`                           | `target`                                 | Skip combat                        |
| `SkipNextTurnEffect`                               | `target: EffectTarget = Controller`      | Skip next turn                     |
| `SkipUntapEffect`                                  | `target, affectsCreatures, affectsLands` | Skip untap                         |
| `PlayAdditionalLandsEffect`                        | `count`                                  | Play extra lands                   |
| `AddCombatPhaseEffect`                             | (object)                                 | Additional combat phase            |
| `TakeExtraTurnEffect`                              | `loseAtEndStep, target`                  | Extra turn (target defaults to Controller) |
| `PreventLandPlaysThisTurnEffect`                   | (object)                                 | Prevent land plays                 |
| `CreateGlobalTriggeredAbilityUntilEndOfTurnEffect` | `ability: TriggeredAbility`              | Global triggered ability until EOT |
| `GrantHexproofEffect`                              | `target: EffectTarget = Controller, duration: Duration = EndOfTurn` | Grant hexproof (player or permanent) |
| `GrantShroudEffect`                                | `target: EffectTarget = Controller, duration: Duration = EndOfTurn` | Grant shroud (player or permanent) |
| `OptionalCostEffect`                               | `cost: Effect, ifPaid: Effect, ifNotPaid: Effect? = null` | "You may [cost]. If you do, [ifPaid]." with optional else |
| `CantCastSpellsEffect`                             | `target: EffectTarget = PlayerRef(Opponent), duration: Duration = EndOfTurn` | Prevent target player from casting spells |
| `GrantDamageBonusEffect`                           | `bonusAmount: Int, sourceFilter: SourceFilter = Any, target: EffectTarget = Controller, duration: Duration = EndOfTurn` | Grant flat damage bonus to player's sources |

### Stack

| Effect                           | Parameters                    | Purpose                    |
|----------------------------------|-------------------------------|----------------------------|
| `CounterEffect`                  | `target, targetSource, destination, condition, filter` | Unified counter — see below |

**CounterEffect parameter combinations (use `Effects.*` facades):**

| Facade Method | Equivalent CounterEffect | Purpose |
|---|---|---|
| `Effects.CounterSpell()` | `CounterEffect()` | Counter target spell |
| `Effects.CounterAbility()` | `CounterEffect(target = CounterTarget.Ability)` | Counter target ability |
| `Effects.CounterTriggeringSpell()` | `CounterEffect(targetSource = CounterTargetSource.TriggeringEntity)` | Counter triggering spell (non-targeted) |
| `Effects.CounterUnlessPays("{2}")` | `CounterEffect(condition = UnlessPaysMana(...))` | Counter unless pays fixed cost |
| `Effects.CounterUnlessDynamicPays(amt)` | `CounterEffect(condition = UnlessPaysDynamic(amt))` | Counter unless pays dynamic cost |
| `Effects.CounterSpellToExile()` | `CounterEffect(destination = Exile())` | Counter and exile |
| `ChangeSpellTargetEffect`        | `targetMustBeSource: Boolean` | Redirect spell target      |
| `ChangeTargetEffect`             | (object)                      | Change target of spell/ability with single target |
| `ReselectTargetRandomlyEffect`   | (object)                      | Randomly reselect triggering spell/ability's target |
| `StormCopyEffect`                | `copyCount, spellEffect, spellTargetRequirements, spellName` | Create Storm copies of a spell |
| `CopyTargetSpellEffect`          | `target: EffectTarget` | Copy target instant or sorcery spell on stack |
| `CopyNextSpellCastEffect`        | `copies: Int` | Copy next instant/sorcery cast this turn |

### Group

| Effect                 | Parameters                                                | Purpose                       |
|------------------------|-----------------------------------------------------------|-------------------------------|
| `ForEachInGroupEffect` | `filter: GroupFilter, effect, noRegenerate, simultaneous` | Apply effect to each in group |

### Protection

| Effect                                        | Parameters                      | Purpose              |
|-----------------------------------------------|---------------------------------|----------------------|
| `ChooseColorAndGrantProtectionToGroupEffect`  | `filter: GroupFilter, duration` | Protection to group  |
| `ChooseColorAndGrantProtectionToTargetEffect` | `target, duration`              | Protection to target |

### Secret Bid

| Effect            | Parameters                  | Purpose        |
|-------------------|-----------------------------|----------------|
| `SecretBidEffect` | `counterType, counterCount` | Secret bidding |

---

## Targets Facade

### Player

- `Targets.Player` / `Targets.Opponent` / `Targets.AllPlayers`

### Creature

- `Targets.Creature` / `Targets.CreatureYouControl` / `Targets.CreatureOpponentControls`
- `Targets.AttackingCreature` / `Targets.BlockingCreature` / `Targets.TappedCreature` / `Targets.FaceDownCreatureYouControl`
- `Targets.CreatureWithKeyword(keyword)` / `Targets.CreatureWithColor(color)`
- `Targets.CreatureWithPowerAtMost(maxPower)` / `Targets.UpToCreatures(count)`
- `TargetFilter.NonlegendaryCreature` — nonlegendary creature (use with `TargetCreature(filter = ...)`)
- Fluent builders: `.nonlegendary()`, `.legendary()`, `.nontoken()` on `TargetFilter` and `GameObjectFilter`
- Fluent state predicate: `.hasGreatestPower()` on `TargetFilter` and `GameObjectFilter` — restricts to creatures with the greatest power among creatures their controller controls
- `GameObjectFilter.Historic` — matches artifacts, legendaries, and Sagas (Dominaria "historic" batching)

### Permanent

- `Targets.Permanent` / `Targets.NonlandPermanent`
- `Targets.Artifact` / `Targets.Enchantment` / `Targets.Land`
- `Targets.PermanentOpponentControls`

### Combined

- `Targets.Any` — creature, player, or planeswalker
- `Targets.CreatureOrPlayer` / `Targets.CreatureOrPlaneswalker` / `Targets.PlayerOrPlaneswalker` / `Targets.OpponentOrPlaneswalker`

### Graveyard

- `Targets.CardInGraveyard` / `Targets.CreatureCardInGraveyard`
- `Targets.CreatureCardInYourGraveyard` / `Targets.InstantOrSorceryInGraveyard`

### Spell (on stack)

- `Targets.Spell` / `Targets.CreatureSpell` / `Targets.NoncreatureSpell`
- `Targets.InstantOrSorcerySpell` / `Targets.InstantOrSorcerySpellYouControl` / `Targets.CreatureOrSorcerySpell`
- `Targets.SpellWithManaValueAtMost(manaValue)`
- `Targets.SpellWithManaValueAtLeast(manaValue)`
- `Targets.ActivatedOrTriggeredAbility`
- `Targets.SpellOrAbilityWithSingleTarget` — target spell or ability (single-target check at resolution)

### Spell or Permanent (combined)

- `TargetSpellOrPermanent(permanentFilter = null)` — target spell on stack OR permanent on battlefield; pass a `GameObjectFilter` to restrict the permanent side (e.g., `permanentFilter = GameObjectFilter.Creature` for "target spell or creature" like Swat Away)

### Composable (Targets.Unified)

- `Targets.Unified.creature` / `.creatureYouControl` / `.creatureOpponentControls` / `.otherCreature` /
  `.otherCreatureYouControl`
- `Targets.Unified.tappedCreature` / `.untappedCreature` / `.attackingCreature` / `.blockingCreature` /
  `.attackingOrBlockingCreature`
- `Targets.Unified.permanent` / `.permanentYouControl` / `.nonlandPermanent` / `.nonlandPermanentOpponentControls`
- `Targets.Unified.artifact` / `.enchantment` / `.land` / `.planeswalker`
- `Targets.Unified.cardInGraveyard` / `.creatureInGraveyard` / `.instantOrSorceryInGraveyard`
- `Targets.Unified.spell` / `.creatureSpell` / `.noncreatureSpell` / `.instantOrSorcerySpell`
- `Targets.Unified.creature { withColor(Color.RED) }` — builder for custom filters
- `Targets.Unified.permanent { ... }` / `.inGraveyard { ... }` / `.onStack { ... }` / `.inExile { ... }`

---

## Triggers Facade

### Zone Changes

- `Triggers.EntersBattlefield` / `Triggers.AnyEntersBattlefield`
- `Triggers.OtherCreatureEnters` / `Triggers.AnyOtherCreatureEnters` / `Triggers.OtherPermanentYouControlEnters` / `Triggers.OtherCreatureWithSubtypeDies(subtype)`
- `Triggers.LeavesBattlefield` / `Triggers.Dies` / `Triggers.AnyCreatureDies` / `Triggers.AnyOtherCreatureDies` / `Triggers.YourCreatureDies` / `Triggers.YourCreatureLeavesBattlefieldWithoutDying`
- `Triggers.PutIntoGraveyardFromBattlefield`

### Combat

- `Triggers.Attacks` / `Triggers.AttacksAlone` / `Triggers.AnyAttacks` / `Triggers.YouAttack` / `Triggers.YouAttackWithFilter(filter: GameObjectFilter)` / `Triggers.NontokenCreatureYouControlAttacks`
- `AttackEvent(filter: GameObjectFilter?, alone: Boolean = false)` — filter restricts which attackers trigger; `alone = true` requires the creature to be the only declared attacker
- `Triggers.Blocks` / `Triggers.BecomesBlocked` / `Triggers.CreatureYouControlBecomesBlocked` / `Triggers.FilteredBecomesBlocked(filter: GameObjectFilter)` — any creature matching filter becomes blocked (any controller)
- `Triggers.BecomesTarget` / `Triggers.BecomesTarget(filter: GameObjectFilter)` — when a permanent becomes target of spell/ability
- `Triggers.Valiant` — Valiant: whenever this creature becomes the target of a spell or ability you control for the first time each turn
- `Triggers.DealsDamage` / `Triggers.DealsCombatDamage`
- `Triggers.DealsCombatDamageToPlayer` / `Triggers.DealsCombatDamageToCreature`
- `Triggers.CreatureYouControlDealsCombatDamageToPlayer` — ANY binding; fires for each creature you control that deals combat damage to a player. TriggeringEntity is the damage source creature.
- `Triggers.CreatureDealtDamageByThisDies` — whenever a creature dealt damage by this creature this turn dies
- `Triggers.EnchantedCreatureDealsCombatDamageToPlayer` — enchanted creature deals combat damage to a player (aura trigger)
- `Triggers.EnchantedCreatureAttacks` — attached creature attacks (aura trigger, e.g., Extra Arms)
- `Triggers.EquippedCreatureAttacks` — attached creature attacks (equipment trigger, e.g., Heart-Piercer Bow)
- `Triggers.EnchantedCreatureDealsDamage` — enchanted creature deals any damage (aura trigger, e.g., Guilty Conscience)
- `Triggers.EnchantedCreatureDies` — enchanted creature dies (aura trigger, e.g., Demonic Vigor)
- `Triggers.EnchantedPermanentLeavesBattlefield` — enchanted permanent leaves the battlefield (aura trigger, e.g., Curator's Ward)
- `Triggers.EquippedCreatureDies` — equipped creature dies (equipment trigger, e.g., Forebear's Blade)

### Phase/Step

- `Triggers.YourUpkeep` / `Triggers.EachUpkeep` / `Triggers.EachOpponentUpkeep`
- `Triggers.YourEndStep` / `Triggers.EachEndStep`
- `Triggers.BeginCombat` / `Triggers.FirstMainPhase` / `Triggers.YourPostcombatMain`
- `Triggers.EnchantedCreatureControllerUpkeep` — enchanted creature's controller's upkeep
- `Triggers.EnchantedCreatureControllerEndStep` — enchanted creature's controller's end step
- `Triggers.TurnedFaceUp` — self turns face up
- `Triggers.CreatureTurnedFaceUp(player)` — whenever a creature is turned face up; `player` defaults to `Player.You`
- `Triggers.FaceDownCreatureEnters` — whenever a face-down creature enters (any controller); compose with `.youControl()`
- `Triggers.EnchantedCreatureTurnedFaceUp` — enchanted creature turns face up (aura trigger)
- `Triggers.EnchantedPermanentBecomesTapped` — enchanted permanent becomes tapped (aura trigger)
- `Triggers.GainControlOfSelf` — you gain control of self

### Spell

- `Triggers.YouCastSpell` / `Triggers.YouCastCreature`
- `Triggers.YouCastNoncreature` / `Triggers.YouCastInstantOrSorcery`
- `Triggers.YouCastEnchantment`
- `Triggers.YouCastHistoric` — whenever you cast a historic spell (artifact, legendary, or Saga)
- `Triggers.YouCastSubtype(subtype)` — whenever you cast a spell with a specific subtype (e.g., `YouCastSubtype(Subtype.LIZARD)`)
- `Triggers.YouCastNoncreatureOrSubtype(subtype)` — whenever you cast a noncreature spell OR a spell with the given subtype (e.g., "noncreature or Otter spell")
- `Triggers.YouCastKickedSpell` — whenever you cast a kicked spell
- `Triggers.NthSpellCast(n, player)` — whenever a player casts their Nth spell each turn (e.g., `NthSpellCast(2)` for "second spell")
- `Triggers.AnySpellOrAbilityOnStack` — whenever any spell or ability is put onto the stack (any player)

### Card Drawing

- `Triggers.YouDraw` / `Triggers.AnyPlayerDraws`
- `Triggers.RevealCreatureFromDraw` — when you reveal a creature card from first draw (Primitive Etchings)
- `Triggers.RevealCardFromDraw` — when you reveal any card from first draw

### Damage

- `Triggers.TakesDamage` / `Triggers.DamagedByCreature` / `Triggers.DamagedBySpell`

### Tap/Untap

- `Triggers.BecomesTapped` / `Triggers.BecomesUntapped`

### Cycle

- `Triggers.YouCycleThis` (when you cycle this card) / `Triggers.YouCycle` (whenever you cycle any card) / `Triggers.AnyPlayerCycles`

### Gift

- `Triggers.YouGiveAGift` — whenever you give a gift (Bloomburrow gift mechanic)
- `Effects.GiftGiven()` — emit GiftGivenEvent; add to gift modes so triggers fire
- `GiftGivenEffect` — data object, no state change, just emits event

### Spell Keyword Grants

- `Effects.GrantSpellKeyword(keyword: Keyword, spellFilter: SpellTypeFilter)` — permanently grant a keyword to spells of a type the controller casts. Used for Ral's storm emblem. Adds `GrantedSpellKeywordsComponent` to the player.

### Life

- `Triggers.YouGainLife` / `Triggers.AnyPlayerGainsLife`

### Library to Graveyard (Batching)

- `Triggers.CreaturesPutIntoGraveyardFromLibrary` — whenever one or more creature cards are put into your graveyard from your library (batching trigger, fires at most once per event batch)

### Graveyard from Anywhere (Batching)

- `Triggers.CardsPutIntoYourGraveyard(filter)` — whenever one or more cards matching the filter are put into your graveyard from anywhere (batching; source zone is not constrained). Example: `Triggers.CardsPutIntoYourGraveyard(GameObjectFilter.Permanent)`
- `Triggers.PermanentCardsPutIntoYourGraveyard` — shorthand for the permanent-card variant (Moonshadow).

### Enter Battlefield (Batching)

- `Triggers.OneOrMorePermanentsEnter(filter)` — whenever one or more permanents matching filter you control enter the battlefield (batching trigger, fires at most once per event batch). Example: `Triggers.OneOrMorePermanentsEnter(GameObjectFilter.Noncreature and GameObjectFilter.Nonland)`

### Transform

- `Triggers.Transforms` / `Triggers.TransformsToBack` / `Triggers.TransformsToFront`

### Triggered Ability Builder Options

- `oncePerTurn = true` — "This ability triggers only once each turn." Tracked via `TriggeredAbilityFiredThisTurnComponent`, cleaned up at end of turn.
- `controlledByTriggeringEntityController = true` — triggered ability is controlled by the triggering entity's controller (Death Match)

---

## Filters Facade

### Card Filters (`Filters.*`) — for search/library effects

- `Filters.AnyCard` / `Filters.Creature` / `Filters.Land` / `Filters.BasicLand`
- `Filters.Instant` / `Filters.Sorcery` / `Filters.Permanent` / `Filters.NonlandPermanent`
- `Filters.PlainsCard` / `.IslandCard` / `.SwampCard` / `.MountainCard` / `.ForestCard`
- `Filters.WithSubtype(subtype)` / `Filters.WithColor(color)` / `Filters.ManaValueAtMost(max)`
- `Filters.GreenCreature`

### Group Filters (`Filters.Group.*`) — for mass effects

- `Filters.Group.allCreatures` / `.creaturesYouControl` / `.creaturesOpponentsControl`
- `Filters.Group.otherCreatures` / `.otherCreaturesYouControl`
- `Filters.Group.attackingCreatures` / `.blockingCreatures` / `.tappedCreatures` / `.untappedCreatures`
- `Filters.Group.allPermanents` / `.permanentsYouControl` / `.allArtifacts` / `.allEnchantments` / `.allLands`
- `Filters.Group.creatures { withColor(Color.RED) }` — builder for custom filters
- `Filters.Group.permanents { withSubtype("Goblin") }` — builder for custom filters

### Static Targets (`Filters.*`) — for equipment/auras/static abilities

- `Filters.AttachedCreature` / `Filters.EquippedCreature` / `Filters.EnchantedCreature`
- `Filters.Self` / `Filters.Controller` / `Filters.AllControlledCreatures`

### Target Filters (`Filters.Target.*`) — for targeting

- `Filters.Target.creature` / `.creatureYouControl` / `.creatureOpponentControls` / `.otherCreature`
- `Filters.Target.tappedCreature` / `.untappedCreature` / `.attackingCreature` / `.blockingCreature`
- `Filters.Target.permanent` / `.nonlandPermanent` / `.artifact` / `.enchantment` / `.land` / `.planeswalker`
- `Filters.Target.cardInGraveyard` / `.creatureInGraveyard` / `.instantOrSorceryInGraveyard`
- `Filters.Target.spellOnStack` / `.creatureSpellOnStack` / `.noncreatureSpellOnStack`
- `Filters.Target.creature { ... }` / `.permanent { ... }` / `.inZone(zone) { ... }` — builders

### Composable (`Filters.Unified.*`)

- `Filters.Unified.any` / `.creature` / `.land` / `.basicLand` / `.artifact` / `.enchantment` / `.planeswalker`
- `Filters.Unified.instant` / `.sorcery` / `.permanent` / `.nonlandPermanent` / `.instantOrSorcery`
- `Filters.Unified.withColor(color)` / `.withSubtype(subtype)` / `.withAnyOfSubtypes(listOf(Subtype("A"), Subtype("B")))` / `.withKeyword(keyword)`
- `Filters.Unified.manaValueAtMost(max)` / `.manaValueAtLeast(min)`
- `GameObjectFilter.Creature.totalPowerAndToughnessAtMost(max)` — creature cards/permanents with combined P/T at most `max`

---

## Costs Facade

- `Costs.Free` — no cost ({0})
- `Costs.Tap` / `Costs.Untap`
- `Costs.Mana("2R")` / `Costs.Mana(manaCost)`
- `Costs.PayLife(amount)`
- `Costs.Sacrifice(filter)` / `Costs.SacrificeAnother(filter)` / `Costs.SacrificeSelf` / `Costs.SacrificeMultiple(count, filter)` / `Costs.SacrificeChosenCreatureType`
- `Costs.DiscardCard` / `Costs.Discard(filter)` / `Costs.DiscardSelf` / `Costs.DiscardHand`
- `Costs.ExileFromGraveyard(count, filter)` / `Costs.ExileXFromGraveyard(filter)` / `Costs.ExileSelf`
- `Costs.RemoveXPlusOnePlusOneCounters` — remove X +1/+1 counters from among creatures you control (X chosen by player)
- `Costs.RemoveCounterFromSelf(counterType: String)` — remove a counter of the specified type from this permanent (e.g., "gem", "charge")
- `Costs.TapAttachedCreature` — tap the creature this is attached to
- `Costs.TapPermanents(count, filter)` — tap N permanents
- `Costs.TapXPermanents(filter)` — tap X permanents (where X is the ability's chosen X value)
- `Costs.Loyalty(change)` — planeswalker loyalty
- `Costs.ReturnToHand(filter, count)` — return N permanents matching filter to owner's hand (default count=1)
- `Costs.Composite(cost1, cost2)` — multiple costs

---

## Conditions Facade

### Battlefield

- `Conditions.ControlCreature` / `.ControlEnchantment` / `.ControlArtifact`
- `Conditions.ControlCreaturesAtLeast(count)` / `.ControlCreatureWithKeyword(keyword)`
- `Conditions.ControlCreatureOfType(subtype)`
- `Conditions.OpponentControlsMoreLands` / `.OpponentControlsMoreCreatures` / `.OpponentControlsCreature`
- `Conditions.APlayerControlsMostOfSubtype(subtype)` — check if a player controls most of a subtype
- `Conditions.TargetPowerAtMost(amount, targetIndex = 0)` — target's power at most N
- `Conditions.TargetSpellManaValueAtMost(amount, targetIndex = 0)` — target spell's MV at most N
- `Conditions.TargetHasCounter(counterType, targetIndex = 0)` — target has at least one counter of type
- `Conditions.TargetMatchesFilter(filter: GameObjectFilter, targetIndex = 0)` — target matches a GameObjectFilter (e.g., legendary, creature type)
- `Conditions.EnchantedCreatureIsLegendary()` — enchanted creature has the legendary supertype

### Life Total

- `Conditions.LifeAtMost(threshold)` / `.LifeAtLeast(threshold)`
- `Conditions.MoreLifeThanOpponent` / `.LessLifeThanOpponent`

### Hand & Graveyard

- `Conditions.EmptyHand` / `.CardsInHandAtLeast(count)` / `.CardsInHandAtMost(count)` / `.OpponentCardsInHandAtMost(count)`
- `Conditions.CreatureCardsInGraveyardAtLeast(count)` / `.CardsInGraveyardAtLeast(count)`
- `Conditions.GraveyardContainsSubtype(subtype)`

### Source State

- `Conditions.WasCastFromHand` — source permanent was cast from hand
- `Conditions.WasCastFromZone(zone)` — spell was cast from specified zone (e.g., `Zone.GRAVEYARD` for flashback)
- `Conditions.WasCastFromGraveyard` — shorthand for `WasCastFromZone(Zone.GRAVEYARD)`
- `Conditions.ManaSpentToCastIncludes(requiredWhite, requiredBlue, requiredBlack, requiredRed, requiredGreen)` — true if at least the specified amount of each color was spent to cast this spell (mana-spent gating, e.g., Lorwyn Incarnation cycle)
- `Conditions.SourceIsAttacking` / `.SourceIsBlocking`
- `Conditions.SourceIsTapped` / `.SourceIsUntapped`
- `Conditions.SourceHasSubtype(subtype)` — source has specific subtype
- `Conditions.SourceHasCounter(counterType)` — source has ≥1 counter of the given type (intervening-if, e.g., Moonshadow)
- `Conditions.SacrificedHadSubtype(subtype)` — a permanent sacrificed as cost had specific subtype
- `Conditions.TriggeringEntityWasHistoric` — the triggering entity was historic (legendary, artifact, or Saga)
- `Conditions.TriggeringEntityHadMinusOneMinusOneCounter` — the triggering entity had a -1/-1 counter on it when it left the battlefield (intervening-if for dies/leaves triggers, e.g., Retched Wretch)

### Turn

- `Conditions.IsYourTurn` / `.IsNotYourTurn`
- `Conditions.IsInPhase(vararg phases, yoursOnly = true)` — true if the current phase is one of the listed phases; with `yoursOnly` also requires the controller's turn (Dose of Dawnglow)
- `Conditions.IsYourMainPhase` — convenience for `IsInPhase(PRECOMBAT_MAIN, POSTCOMBAT_MAIN, yoursOnly = true)`
- `Conditions.YouGainedLifeThisTurn` — true if you gained life this turn
- `Conditions.YouGainedOrLostLifeThisTurn` — true if you gained or lost life this turn
- `Conditions.YouLostLifeThisTurn` — true if you lost life this turn (for conditional static abilities)
- `Conditions.YouGainedAndLostLifeThisTurn` — true if you both gained and lost life this turn
- `Conditions.OpponentLostLifeThisTurn` — true if any opponent lost life this turn (from any source)
- `Conditions.CardsLeftGraveyardThisTurn(count)` — true if N+ cards left your graveyard this turn
- `Conditions.SacrificedFoodThisTurn` — true if you've sacrificed a Food artifact this turn
- `Conditions.PutCounterOnCreatureThisTurn` — true if you put one or more counters on a creature this turn (Lasting Tarfire)
- `Conditions.SourceAbilityResolvedNTimes(count)` — true if this is the Nth time this ability has resolved this turn (use with `IncrementAbilityResolutionCountEffect`)
- `IsFirstSpellOfTypeCastThisTurn(spellCategory: String)` — raw condition. True if the count of spells matching the category cast by you this turn is exactly 1. Categories: `"INSTANT"`, `"SORCERY"`, `"CREATURE"`, `"NONCREATURE"`, `"INSTANT_OR_SORCERY"`, `"ENCHANTMENT"`, `"HISTORIC"`, `"SUBTYPE_<NAME>"` (e.g., `"SUBTYPE_OTTER"`).

### Zone Presence

- `Exists(player: Player, zone: Zone, filter: GameObjectFilter, negate = false, excludeSelf = false)` — raw condition (import directly from `com.wingedsheep.sdk.scripting.conditions.Exists`). Checks if any matching object exists in a player's zone. Use `excludeSelf = true` for "another" wording (excludes the source entity). Use with `GameObjectFilter.Creature.enteredThisTurn().youControl()` for "if a creature entered the battlefield under your control this turn".

### Pipeline Collection

- `Conditions.CollectionContainsMatch(collection: String, filter: GameObjectFilter)` — true if a named pipeline collection contains a card matching the filter. Used for "if you did X this way" patterns (e.g., "if you returned a Squirrel card to your hand this way").

### Composite

- `Conditions.All(cond1, cond2)` — AND
- `Conditions.Any(cond1, cond2)` — OR
- `Conditions.Not(cond)` — NOT

---

## DynamicAmounts Facade

- `DynamicAmounts.creaturesYouControl()` / `.otherCreaturesYouControl()`
- `DynamicAmounts.allCreatures()` / `.landsYouControl()`
- `DynamicAmounts.attackingCreaturesYouControl()`
- `DynamicAmounts.creaturesWithSubtype(subtype)` / `.landsWithSubtype(subtype)`
- `DynamicAmounts.otherCreaturesWithSubtypeYouControl(subtype)`
- `DynamicAmounts.cardsInYourGraveyard()` / `.creatureCardsInYourGraveyard()`
- `DynamicAmounts.creaturesAttackingYou(multiplier)` / `.tappedCreaturesTargetOpponentControls()`
- `DynamicAmounts.landsOfTypeTargetOpponentControls(landType, multiplier)` /
  `.creaturesOfColorTargetOpponentControls(color, multiplier)`
- `DynamicAmounts.handSizeDifferenceFromTargetOpponent()`
- `DynamicAmounts.colorsAmongPermanents(player, filter)` — distinct colors among permanents (Lorwyn Eclipsed Vivid)

### Raw DynamicAmount types

- `DynamicAmount.XValue` / `DynamicAmount.Fixed(n)` / `DynamicAmount.YourLifeTotal`
- `DynamicAmount.SacrificedPermanentPower` / `.SacrificedPermanentToughness`
- `DynamicAmount.SourcePower` / `.SourceToughness` / `.TriggerDamageAmount` / `.TriggerLifeGainAmount` / `.LastKnownCounterCount`
- `DynamicAmount.ColorsAmongPermanentsYouControl` / `.CardTypesInAllGraveyards` / `.CardTypesInLinkedExile`
- `DynamicAmount.CountersOnSelf(counterType)` / `.CountersOnTarget(counterType, targetIndex)` / `.CreaturesSharingTypeWithTriggeringEntity`
- `DynamicAmount.TargetCount` — number of targets in the current effect context (for "for each target" token creation)
- `DynamicAmount.VariableReference(variableName)` / `.StoredCardManaValue(collectionName)` / `.AdditionalCostExiledCount`
- `DynamicAmount.AttachmentsOnSelf` — count of Auras and Equipment attached to the source entity
- `DynamicAmount.NumberOfBlockers` / `DynamicAmounts.numberOfBlockers()` — number of creatures blocking the triggering entity
- `DynamicAmount.DamageDealtToTargetPlayerThisTurn(targetIndex)` — total damage dealt to a target player this turn
- `DynamicAmount.NonTokenCreaturesDiedThisTurn(player)` / `DynamicAmounts.nonTokenCreaturesDiedThisTurn(player)` — count of nontoken creatures put into a player's graveyard from battlefield this turn
- `DynamicAmount.OpponentsWhoLostLifeThisTurn` — count of opponents who lost life this turn
- `DynamicAmount.Count(player, zone, filter)` /
  `DynamicAmount.AggregateBattlefield(player, filter, aggregation?, property?, excludeSelf?)` /
  `DynamicAmount.AggregateZone(player, zone, filter, aggregation?, property?)` — zone-generic aggregate for non-battlefield zones (graveyard, hand, library, exile)
- `DynamicAmount.CountCreaturesOfSourceChosenType` — count creatures you control of the source's chosen creature type (Three Tree City)
- `DynamicAmount.Conditional(condition, ifTrue, ifFalse)` — conditional amount
- Fluent: `DynamicAmounts.battlefield(player, filter).count()` / `.maxManaValue()` / `.maxPower()` / `.maxToughness()` /
  `.minToughness()` / `.sumPower()`
- Fluent: `DynamicAmounts.zone(player, zone, filter).count()` / `.maxManaValue()` / `.maxPower()` / `.maxToughness()` /
  `.sumManaValue()`
- Math: `DynamicAmount.Add(l, r)` / `.Subtract(l, r)` / `.Multiply(amt, n)` / `.Max(l, r)` / `.Min(l, r)` /
  `.IfPositive(amt)`

---

## EffectPatterns Facade

**IMPORTANT: Always prefer `EffectPatterns.*` and atomic pipelines over creating new monolithic effects.** This keeps
the engine extendible — new cards can reuse existing atomic effects with different parameters instead of requiring new
executor code.

### Atomic Library Pipelines

The engine uses a `GatherCards → SelectFromCollection → MoveCollection` pipeline for library manipulation. These atomic
effects can be composed for any "look at top N" style ability:

| Pattern                                                                             | Usage                                                              |
|-------------------------------------------------------------------------------------|--------------------------------------------------------------------|
| `EffectPatterns.lookAtTopAndReorder(count)`                                         | "Look at top N, put back in any order" (e.g., Sage Aven)           |
| `EffectPatterns.lookAtTopAndReorder(dynamicAmount)`                                 | Same but with dynamic count (e.g., Information Dealer)             |
| `EffectPatterns.lookAtTopAndKeep(count, keepCount)`                                 | "Look at top N, put one into hand, rest on bottom" (e.g., Impulse) |
| `EffectPatterns.lookAtTopXAndPutOntoBattlefield(countSource, filter, shuffleAfter)` | CoCo-style put onto battlefield                                    |

For custom pipelines (e.g., looking at another player's library), compose directly:

```kotlin
CompositeEffect(
    listOf(
        GatherCardsEffect(
            source = CardSource.TopOfLibrary(DynamicAmount.Fixed(1), Player.ContextPlayer(0)),
            storeAs = "target_top"
        ),
        MoveCollectionEffect(
            from = "target_top",
            destination = CardDestination.ToZone(Zone.LIBRARY, Player.ContextPlayer(0), ZonePlacement.Top),
            order = CardOrder.ControllerChooses
        )
    )
)
```

**Reference implementation:** See `ZoralineCosmosCaller.kt` (Bloomburrow) for a complex card that composes
`MayPayManaEffect` → `PayLifeEffect` → `GatherCardsEffect` → `SelectFromCollectionEffect` → `MoveCollectionEffect`
into a reflexive "pay mana + life, reanimate with finality counter" pipeline — a good example of building sophisticated
abilities from atomic primitives.

### Atomic Building Blocks

| Effect                                                                                                                         | Purpose                                          |
|--------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------|
| `GatherCardsEffect(source, storeAs, revealed)`                                                                                 | Gather cards from a zone into a named collection |
| `SelectFromCollectionEffect(from, selection, chooser, filter, storeSelected, storeRemainder, matchChosenCreatureType, prompt)` | Player selects from a collection                 |
| `MoveCollectionEffect(from, destination, order, revealed, moveType, linkToSource)`                                            | Move a collection to a zone                      |
| `GatherUntilMatchEffect(player, filter, storeMatch, storeRevealed, count)`                                                     | Walk library until `count` matches (default 1), store both |
| `GatherSubtypesEffect(from, storeAs)`                                                                                          | Extract subtypes of entities into storedSubtypeGroups |
| `RevealCollectionEffect(from)`                                                                                                  | Emit CardsRevealedEvent for a stored collection  |
| `ChooseCreatureTypeEffect`                                                                                                     | Choose a creature type (data object)             |
| `FilterCollectionEffect(from, filter, storeMatching, storeNonMatching?)`                                                       | Filter collection into matching/non-matching     |
| `SelectTargetEffect(requirement, storeAs)`                                                                                     | Select and store a target                        |

Filters: `CollectionFilter.MatchesFilter(filter)`, `CollectionFilter.ExcludeSubtypesFromStored(key)`,
`CollectionFilter.SharesSubtypeWithSacrificed`, `CollectionFilter.GreatestPower` — keep only creatures with highest power,
`CollectionFilter.ManaValueAtMost(max: DynamicAmount)` — keep only cards with mana value ≤ dynamic amount (e.g., X value),
`CollectionFilter.ManaValueEquals(value: DynamicAmount)` — keep only cards with mana value exactly equal to dynamic amount (e.g., counters on source)

Sources: `CardSource.TopOfLibrary(count, player)`, `CardSource.FromZone(zone, player, filter)`,
`CardSource.FromVariable(name)`, `CardSource.TappedAsCost` (permanents tapped as cost),
`CardSource.ControlledPermanents(player, filter)`,
`CardSource.FromMultipleZones(zones, player, filter)` — gather cards from multiple zones (e.g., graveyard + hand + library)
Destinations: `CardDestination.ToZone(zone, player, placement)`
Placements: `ZonePlacement.Top`, `.Bottom`, `.Shuffled`, `.Default`, `.Tapped`
Selection: `SelectionMode.ChooseExactly(count)`, `.ChooseUpTo(count)`, `.All`, `.Random(count)`, `.ChooseAnyNumber`
Chooser: `Chooser.Controller`, `.Opponent`, `.TargetPlayer`, `.TriggeringPlayer`, `.SourceController` (resolves through `sourceId` -> projected controller, ignoring per-iteration controller swaps inside `ForEachPlayerEffect`)
Ordering: `CardOrder.ControllerChooses`, `.Random`, `.Preserve`
MoveType: `MoveType.Default`, `.Discard`, `.Sacrifice`

### General Patterns

- `EffectPatterns.mayPay(cost, effect)` — "You may [cost]. If you do, [effect]"
- `EffectPatterns.mayPayOrElse(cost, ifPaid, ifNotPaid)` — with fallback
- `EffectPatterns.blight(amount)` — "Blight N" (ECL): Gather → Select → AddCounters pipeline that puts N -1/-1 counters on a chosen creature you control (non-targeting; silent no-op if the player controls no creatures)
- `EffectPatterns.sacrifice(filter, count, then)` — sacrifice + effect
- `EffectPatterns.sacrificeFor(filter, countName, thenEffect)` — sacrifice, store count
- `EffectPatterns.reflexiveTrigger(action, whenYouDo, optional)` — "When you do, [effect]"
- `EffectPatterns.storeEntity(effect, as)` / `storeCount(effect, as)` — variable storage
- `EffectPatterns.sequence(effects...)` — chain effects
- `EffectPatterns.exileUntilLeaves(exileTarget, variableName)` — O-Ring pattern
- `EffectPatterns.exileUntilEndStep(target)` — exile until end of turn
- `EffectPatterns.revealUntilNonlandDealDamage(target)` — reveal until nonland, deal damage
- `EffectPatterns.revealUntilNonlandDealDamageEachTarget()` — same, per target
- `EffectPatterns.revealUntilNonlandModifyStats()` — reveal until nonland, modify stats
- `EffectPatterns.revealUntilCreatureTypeToBattlefield()` — reveal until creature type, put on battlefield
- `EffectPatterns.revealAndOpponentChooses(count, filter)` — reveal top, opponent chooses
- `EffectPatterns.chooseCreatureTypeRevealTop()` — choose type, reveal top
- `EffectPatterns.chooseCreatureTypeReturnFromGraveyard(count)` — choose type, return from graveyard
- `EffectPatterns.chooseCreatureTypeShuffleGraveyardIntoLibrary()` — choose type, shuffle matching creatures from graveyard into library
- `EffectPatterns.headGames(target)` — Head Games effect
- `EffectPatterns.wheelEffect(players)` — wheel effect
- `EffectPatterns.discardCards(count, target)` / `.discardRandom(count, target)` / `.discardHand(target)` — discard
  patterns
- `EffectPatterns.putFromHand(filter, count, entersTapped)` — put card from hand onto battlefield
- `EffectPatterns.eachOpponentMayPutFromHand(filter)` — each opponent may put from hand
- `EffectPatterns.eachOpponentDiscards(count, controllerDrawsPerDiscard = 0)` — each opponent discards (with optional controller draw per discard)
- `EffectPatterns.eachPlayerDiscardsDraws(controllerBonusDraw)` — each player discards and draws
- `EffectPatterns.eachPlayerDrawsX(includeController, includeOpponents)` — each player draws X
- `EffectPatterns.eachPlayerSearchesLibrary(filter, count)` — each player searches
-
`EffectPatterns.eachPlayerRevealCreaturesCreateTokens(tokenPower, tokenToughness, tokenColors, tokenCreatureTypes, tokenImageUri?)` —
reveal creatures, create tokens
- `EffectPatterns.eachPlayerMayDraw(maxCards, lifePerCardNotDrawn = 0)` — each player may draw up to N
- `EffectPatterns.eachPlayerReturnsPermanentToHand()` — each player returns a permanent
- `EffectPatterns.searchTargetLibraryExile(count, filter)` — search target's library, exile
- `EffectPatterns.mill(count, target)` — mill pipeline
- `EffectPatterns.shuffleGraveyardIntoLibrary(target)` — shuffle graveyard into library

---

## Keywords (Keyword enum)

### Evasion

`FLYING`, `MENACE`, `INTIMIDATE`, `FEAR`, `SHADOW`, `HORSEMANSHIP`, `CANT_BE_BLOCKED`,
`CANT_BE_BLOCKED_BY_MORE_THAN_ONE`

### Landwalk

`SWAMPWALK`, `FORESTWALK`, `ISLANDWALK`, `MOUNTAINWALK`, `PLAINSWALK`

### Combat

`FIRST_STRIKE`, `DOUBLE_STRIKE`, `TRAMPLE`, `DEATHTOUCH`, `LIFELINK`, `VIGILANCE`, `REACH`, `PROVOKE`

### Defense

`DEFENDER`, `INDESTRUCTIBLE`, `HEXPROOF`, `SHROUD`, `WARD`, `PROTECTION`

### Speed

`HASTE`, `FLASH`

### Triggered/Static

`PROWESS`, `CHANGELING`

### ETB Modification

`AMPLIFY`

### Cost Reduction

`CONVOKE`, `DELVE`, `AFFINITY`

### Spell Mechanics

`STORM`

### Damage Modification

`WITHER` — Damage dealt to creatures by this source is dealt in the form of -1/-1 counters (CR 702.79)

### Ability Words (display-only, no uniform mechanic)

`VIVID` (Lorwyn Eclipsed) — flavor prefix for effects scaled by the number of distinct colours among permanents you control. Attach via `vividEtb { colors -> ... }` (ETB scaling half) or `vividCostReduction()` (cost-reduction half) on `CardBuilder`.

### Restrictions

`DOESNT_UNTAP`, `MAY_NOT_UNTAP`, `CANT_RECEIVE_COUNTERS`

---

## Parameterized Keyword Abilities (KeywordAbility)

Used via `keywordAbility(...)` or `keywordAbilities(...)` in card DSL:

- `KeywordAbility.Simple(keyword)` — wraps a basic keyword
- **Ward**: `WardMana(cost)`, `WardLife(amount)`, `WardDiscard(count, random)`, `WardSacrifice(filter)`
- **Hexproof**: `HexproofFromColor(color)` — hexproof from a specific color
- **Protection**: `ProtectionFromColor(color)`, `ProtectionFromColors(colors)`, `ProtectionFromCardType(type)`,
  `ProtectionFromCreatureSubtype(subtype)`, `ProtectionFromEverything`
- **Combat**: `Annihilator(count)`, `Bushido(count)`, `Rampage(count)`, `Flanking`, `Afflict(count)`
- **Counters**: `Modular(count)`, `Fabricate(count)`, `Renown(count)`, `Tribute(count)`
- **Time**: `Fading(count)`, `Vanishing(count)`
- **Vehicles**: `Crew(power)`
- **Cost**: `Affinity(forType)`, `AffinityForSubtype(forSubtype)`, `Cycling(cost)`, `Typecycling(type, cost)`, `BasicLandcycling(cost)`, `Kicker(cost)`, `KickerWithAdditionalCost(cost: AdditionalCost)`, `Multikicker(cost)`
- **Transform**: `Morph(cost, faceUpEffect?)`, `Absorb(count)` — `faceUpEffect` is an `Effect` executed as a replacement effect when turned face up (e.g., `AddCountersEffect` for Hooded Hydra)
- **Alternative Cost**: `Evoke(cost: ManaCost)` — cast for evoke cost; sacrificed on ETB. DSL: `evoke = "{R/W}{R/W}"`. Engine detects ETB + `EvokedComponent` and creates sacrifice trigger.

Companion helpers: `KeywordAbility.of(keyword)`, `.ward(cost)`, `.wardLife(amount)`, `.wardDiscard(count, random)`,
`.hexproofFrom(color)`, `.protectionFrom(color)`, `.protectionFrom(vararg colors)`, `.protectionFromSubtype(subtype)`,
`.cycling(cost)`, `.basicLandcycling(cost)`, `.morph(cost)`, `.morphPayLife(amount)`, `.evoke(cost)`

---

## Static Abilities

Set via `staticAbility { ability = ... }`:

### Keywords & Stats

- `GrantKeyword(keyword, target: StaticTarget)` — permanent keyword grant
- `RemoveKeywordStatic(keyword, target: StaticTarget)` — permanent keyword removal (e.g., "equipped creature loses flying")
- `GrantKeywordToCreatureGroup(keyword, filter: AffectsFilter)` — keyword to group
- `GrantTriggeredAbilityToCreatureGroup(ability: TriggeredAbility, filter: GroupFilter)` — triggered ability to group (e.g., Hunter Sliver granting provoke to all Slivers)
- `GrantTriggeredAbilityToAttachedCreature(ability: TriggeredAbility)` — triggered ability to attached creature (e.g., Combat Research granting a combat-damage draw trigger)
- `GrantActivatedAbilityToCreatureGroup(ability: ActivatedAbility, filter: GroupFilter)` — activated ability to group (e.g., Spectral Sliver granting pump to all Slivers)
- `GrantActivatedAbilityToAttachedCreature(ability: ActivatedAbility)` — activated ability to attached creature (e.g., Singing Bell Strike granting "{6}: Untap this creature")
- `GrantCantBeBlockedExceptBySubtype(filter: GroupFilter, requiredSubtype: String)` — "can't be blocked except by [subtype]" to group (e.g., Shifting Sliver)
- `GrantKeywordByCounter(keyword, counterType)` — grant keyword when counter present
- `ModifyStats(powerBonus, toughnessBonus, target: StaticTarget)` — P/T bonus
- `ModifyStatsForCreatureGroup(powerBonus, toughnessBonus, filter: GroupFilter)` — P/T to group (use `GroupFilter.ChosenSubtypeCreatures()` for chosen-type lord effects)
- `SetBaseToughnessForCreatureGroup(toughness, filter: GroupFilter)` — set base toughness for a group (Layer 7b SET_VALUES)
- `GrantDynamicStatsEffect(target, powerBonus: DynamicAmount, toughnessBonus: DynamicAmount)` — dynamic P/T (use `EntityProperty(Source, CounterCount(...))` for counter-based, `CreaturesSharingTypeWithEntity(AffectedEntity)` for shared-type)
- `GrantProtection(color, target)` — grant protection from color
- `GrantProtectionFromChosenColorToGroup(filter: GroupFilter)` — grant protection from chosen color (via `EntersWithChoice(ChoiceType.COLOR)`) to a group
- `GrantHexproofFromOwnColorsToGroup(filter: GroupFilter)` — for each affected creature, grant `HEXPROOF_FROM_<color>` for every color the creature itself has after Layer 5; colorless creatures gain nothing (Tam, Mindful First-Year)

### Land Animation

- `AnimateLandGroup(filter: GroupFilter, power, toughness, creatureSubtypes, colors)` — lands matching filter become P/T creatures with subtypes/colors (still lands). Generates multi-layer continuous effects (TYPE, COLOR, P/T).

### Combat Restrictions

- `CantAttack(target)` / `CantBlock(target)` / `MustAttack(target)`
- `CantAttackForCreatureGroup(filter: GroupFilter)` — prevents creatures matching filter from attacking
- `CantBlockForCreatureGroup(filter: GroupFilter)` — prevents creatures matching filter from blocking (e.g., "Beasts can't block")
- `MustAttackForCreatureGroup(filter: GroupFilter)` — forces creatures matching filter to attack each combat if able
- `MustBlockForCreatureGroup(filter: GroupFilter)` — forces creatures matching filter to block each combat if able
- `CantAttackUnless(condition: Condition, target)` — conditional attack restriction (use `Conditions.ControlMoreCreatures`, `Conditions.OpponentControlsLandType(landType)`)
- `CantBlockUnless(condition: Condition, target)` — conditional block restriction (uses any `Condition`)
- `CantBlockCreaturesWithGreaterPower(target)`
- `CanOnlyBlockCreaturesWithKeyword(keyword, target)`
- `CanBlockAnyNumber(target)` — can block any number of creatures
- `CanBlockAdditionalForCreatureGroup(count, filter)` — creatures matching filter can block an additional N creatures (cumulative)

### Evasion

- `CantBeBlocked(target)` — this creature can't be blocked (use with ConditionalStaticAbility for conditional unblockability)
- `CantBeBlockedBy(blockerFilter, target)` — can't be blocked by creatures matching filter (replaces color/power/subtype variants)
- `CantBeBlockedExceptBy(blockerFilter, target)` — can only be blocked by creatures matching filter
- `CantBeBlockedByMoreThan(maxBlockers, target)`
- `CantBeBlockedUnlessDefenderSharesCreatureType(minSharedCount, target)`

### Damage

- `AssignDamageEqualToToughness(target, onlyWhenToughnessGreaterThanPower)` — Doran
- `DivideCombatDamageFreely(target)` — divide damage freely
- `AssignCombatDamageAsUnblocked(target)` — may assign combat damage as though unblocked (Thorn Elemental)

### Type & Subtype

- `AddCreatureTypeByCounter(creatureType, counterType)` — add type when counter present
- `AddLandTypeByCounter(landType, counterType)` — add basic land type to all lands with counter (e.g., flood counters → Island)
- `GrantSupertype(supertype, target)` — grant a supertype (e.g., "LEGENDARY") via Layer 4 continuous effect
- `SetEnchantedLandType(landType)` — set enchanted land's type
- `GrantChosenColor(target)` — add the chosen color (from source's `ChosenColorComponent`, set via `EntersWithChoice(ChoiceType.COLOR)`) to target, Layer 5 (Shimmerwilds Growth)

### Other

- `CantReceiveCounters(target)` — target can't have counters put on it (grants `AbilityFlag.CANT_RECEIVE_COUNTERS`; checked by `AddCountersExecutor`)
- `ControlEnchantedPermanent` — control the enchanted permanent
- `GrantShroudToController` — controller has shroud
- `GrantHexproofToController` — controller has hexproof (opponents can't target; self-targeting still allowed)
- `GrantCantLoseGame` — controller can't lose the game (Lich's Mastery, Platinum Angel)
- `ExtraLoyaltyActivation` — activate loyalty abilities of planeswalkers you control twice each turn (Oath of Teferi)
- `AdditionalETBTriggers(creatureFilter)` — when a creature matching the filter ETBs under your control, triggered abilities of your permanents that fired from that event trigger an additional time (Naban, Dean of Iteration)
- `AdditionalSourceTriggers(sourceFilter, excludeSelf = true)` — if a triggered ability of a permanent matching the filter you control triggers, it triggers an additional time (Twinflame Travelers — "another Elemental"). Works for *all* triggers (not just ETB). `excludeSelf` skips the doubler's own source to honour "another" wording.
- `NoncombatDamageBonus(bonusAmount)` — if a source you control would deal noncombat damage to an opponent or a permanent an opponent controls, it deals that much damage plus bonusAmount instead (Artist's Talent Level 3)
- `CantCastSpells(target, duration)` — prevent target player from casting spells
- `SkipNextTurn(target)` — target player skips their next turn
- `AdditionalManaOnTap(color: Color?, amount: DynamicAmount)` — whenever enchanted land is tapped for mana, add `amount` mana of `color`. If `color` is `null`, the color is read from the aura's own `ChosenColorComponent` (set via `EntersWithChoice(ChoiceType.COLOR)`) at tap time — used by Shimmerwilds Growth.
- `AdditionalManaOnLandTap(filter: GameObjectFilter, amount: DynamicAmount)` — global triggered mana ability: whenever any player taps a land matching the filter for mana, that player adds `amount` additional mana of the color the land produced (Lavaleaper, Heartbeat of Spring–style effects)
- `PlayFromTopOfLibrary` — play cards from top of library (revealed to all)
- `CastSpellTypesFromTopOfLibrary(filter: GameObjectFilter)` — cast matching spells from top of library (e.g., instants/sorceries only)
- `PlayLandsAndCastFilteredFromTopOfLibrary(spellFilter: GameObjectFilter)` — play lands + cast spells matching filter from top of library (e.g., MV 4+)
- `LookAtTopOfLibrary` — look at top card of your library any time (private, controller only)
- `MayCastSelfFromZones(zones: List<Zone>)` — intrinsic permission to cast this card from specified zones (e.g., graveyard, exile)
- `MayCastFromGraveyardWithLifeCost(filter, lifeCost, duringYourTurnOnly)` — controller may cast matching spells from graveyard by paying life (e.g., Festival of Embers)
- `MayPlayPermanentsFromGraveyard` — during each of your turns, play a land and cast a permanent spell of each type from your graveyard (Muldrotha). Tracks per-type usage via `GraveyardPlayPermissionUsedComponent` on the source permanent, cleared at end of turn.
- `GrantMayCastFromLinkedExile(filter, duringYourTurnOnly = false, additionalCost = null)` — you may cast cards exiled with this permanent that match the filter (Rona, Disciple of Gix). `duringYourTurnOnly` restricts the permission to the controller's turn; `additionalCost` (any `AdditionalCost`) must be paid alongside the spell's normal costs (e.g., Dawnhand Dissident's `RemoveCountersFromYourCreatures(3)`). Works with `LinkedExileComponent`.
- `LookAtFaceDownCreatures` — look at face-down creatures you don't control any time
- `PreventCycling` — players can't cycle cards
- `PreventManaPoolEmptying` — players don't lose unspent mana as steps and phases end
- `IncreaseMorphCost(amount: Int)` — all morph (turn face-up) costs cost more
- `IncreaseSpellCostByFilter(filter: GameObjectFilter, amount: Int)` — spells matching filter cost more (global tax effect)
- `IncreaseSpellCostByPlayerSpellsCast(amountPerSpell: Int = 1)` — each spell costs {N} more per other spell that player has cast this turn (Damping Sphere)
- `DampLandManaProduction` — if a land is tapped for 2+ mana, it produces {C} instead (Damping Sphere)
- `GrantFlashToSpellType(filter: GameObjectFilter, controllerOnly: Boolean = false)` — cast spells matching filter as though they had flash. `controllerOnly = false` (default) = any player benefits (Quick Sliver); `controllerOnly = true` = only the permanent's controller benefits (Raff Capashen)
- `GrantCantBeCountered(filter: GameObjectFilter)` — spells matching filter can't be countered (e.g., Root Sliver)
- `AttackTax(manaCostPerAttacker: String)` — creatures can't attack you unless their controller pays the cost per attacker (e.g., Ghostly Prison, Windborn Muse)
- `CantBeAttackedWithout(requiredKeyword: Keyword)` — creatures without the specified keyword can't attack the controller (e.g., Form of the Dragon — creatures without flying can't attack you)
- `RevealFirstDrawEachTurn` — reveal the first card drawn each turn (Primitive Etchings)
- `UntapDuringOtherUntapSteps` — untap all permanents you control during each other player's untap step (Seedborn Muse)
- `UntapFilteredDuringOtherUntapSteps(filter: GameObjectFilter)` — untap permanents matching filter you control during each other player's untap step (Ivorytusk Fortress)
- `SpellCostReduction(reductionSource)` — cost reduction
- `FaceDownSpellCostReduction(reductionSource)` — face-down spell cost reduction
- `ReduceSpellCostBySubtype(subtype, amount)` — reduce generic cost per subtype
- `ReduceSpellColoredCostBySubtype(subtype, manaReduction)` — reduce colored mana cost per subtype (e.g., Edgewalker: `"{W}{B}"`)
- `ReduceFirstSpellOfTypeColoredCost(spellFilter, spellCategory, manaReductionPerUnit, countSource)` — reduce first spell of type each turn by colored mana per dynamic count, overflow to generic (e.g., Eluge: first instant/sorcery costs {U} less per flood-counter land)
- `ReduceSpellCostByFilter(filter, amount)` — reduce spell cost for spells matching a GameObjectFilter
- `ReduceFaceDownCastingCost(amount)` — reduce face-down casting cost
- `GrantAlternativeCastingCost(cost: String)` — grants an alternative mana cost for all spells cast by this permanent's controller (e.g., Jodah: `"{W}{U}{B}{R}{G}"`)
- `ConditionalStaticAbility(ability, condition)` — conditional static. Supported conditions during projection: `SourceHasSubtype`, `SourceHasKeyword`, `SourceIsTapped`, `SourceIsUntapped` (Illusion Spinners), `EnchantedCreatureHasSubtype`, `EnchantedCreatureIsLegendary`, `IsYourTurn`, `YouLostLifeThisTurn`, `Compare`, `Exists` (subset: you/each/opponent control of creatures or filtered permanents), and `Not` of any supported condition.

### StaticTarget values

`StaticTarget.AttachedCreature`, `SourceCreature`, `Controller`, `AllControlledCreatures`, `SpecificCard(entityId)`

### CostReductionSource values

`ColorsAmongPermanentsYouControl`, `Fixed(amount)`, `CreaturesYouControl`, `TotalPowerYouControl`, `ArtifactsYouControl`, `FixedIfControlFilter(amount, filter)` — fixed reduction if you control a permanent matching the GameObjectFilter (e.g., "costs {1} less if you control a Wizard"), `CardsInGraveyardMatchingFilter(filter, amountPerCard = 1)` — reduces by amountPerCard for each card in your graveyard matching the filter (e.g., "costs {1} less for each instant and sorcery card in your graveyard"), `CardsInGraveyardAndExileMatchingFilter(filter, amountPerCard = 1)` — reduces by amountPerCard for each card you own in exile and in your graveyard matching the filter (e.g., "costs {1} less for each creature card you own in exile and in your graveyard"), `PermanentsWithCounterYouControl(filter, counterType)` — reduces by number of permanents you control matching filter that have the specified counter (e.g., "for each land you control with a flood counter"), `FixedIfAnyTargetMatches(amount, filter)` — fixed reduction if any of the spell's chosen targets match the filter (e.g., Dire Downdraft: "costs {1} less if it targets an attacking or tapped creature"), `FixedIfCreatureAttackingYou(amount)` — fixed reduction if any creature on the battlefield is currently attacking the caster (or a planeswalker they control), e.g., Swat Away: "costs {2} less if a creature is attacking you"

---

## Replacement Effects

Used in card definitions for effects that intercept events before they happen:

### Token

- `DoubleTokenCreation(appliesTo)` — Doubling Season
- `ModifyTokenCount(modifier, appliesTo)`

### Counter

- `DoubleCounterPlacement(appliesTo)` — Corpsejack Menace
- `ModifyCounterPlacement(modifier, appliesTo)` — Hardened Scales

### Zone Change

- `RedirectZoneChange(newDestination, appliesTo)` — Anafenza, Rest in Peace. Redirects zone changes matching `appliesTo` filter (e.g., creatures going to graveyard → exile). Engine hooks: SBA death, destroy, moveCardToZone, MoveCollectionExecutor.
- `RedirectZoneChangeWithEffect(newDestination, additionalEffect, selfOnly, appliesTo)` — Ugin's Nexus. Like RedirectZoneChange but also executes an additional effect (e.g., TakeExtraTurnEffect) when the redirect applies. `selfOnly=true` means only applies when the entity being moved IS the permanent with this effect.
- `EntersTapped(unlessCondition?, payLifeCost?, appliesTo)` — tap lands. Use `unlessCondition` for check lands (e.g., `Conditions.Any(Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Land.withSubtype("Island")), ...)`) — enters tapped unless condition is met. Use `payLifeCost` for shock lands (e.g., `EntersTapped(payLifeCost = 2)`) — player may pay life to enter untapped.
- `EntersWithCounters(counterType, count, appliesTo)` — Master Biomancer
- `EntersWithDynamicCounters(counterType, count: DynamicAmount, appliesTo)` — dynamic counter entry
- `UndyingEffect(appliesTo)` / `PersistEffect(appliesTo)`
- `EntersAsCopy(optional, copyFilter, filterByTotalManaSpent, additionalSubtypes, additionalKeywords, appliesTo)` — clone effects (copyFilter defaults to Creature; use `GameObjectFilter.NonlandPermanent` for Clever Impersonator; `filterByTotalManaSpent` for X-cost clones like Mockingbird; `additionalSubtypes`/`additionalKeywords` for "except it's a Bird and has flying")
- `EntersWithChoice(choiceType, chooser?, appliesTo)` — unified "as enters, choose X" effect. `choiceType`: `ChoiceType.COLOR`, `ChoiceType.CREATURE_TYPE`, or `ChoiceType.CREATURE_ON_BATTLEFIELD`. `chooser` defaults to `Player.You`; use `Player.Opponent` for Callous Oppressor. Stores chosen value as `ChosenColorComponent`, `ChosenCreatureTypeComponent`, or `ChosenCreatureComponent`. Reference chosen creature via `EffectTarget.ChosenCreature`
- `EntersWithRevealCounters(filter?, revealSource?, counterType?, countersPerReveal, appliesTo)` — as this creature enters, reveal cards matching filter from a zone, put N counters per card. Defaults reproduce Amplify: filter = creatures sharing type with source, revealSource = HAND, counterType = "+1/+1"

### Damage

- `PreventDamage(amount?, appliesTo)` — Fog, protection (null = prevent all)
- `RedirectDamage(redirectTo, appliesTo)` — Pariah
- `DoubleDamage(appliesTo)` — Furnace of Rath
- `ModifyDamageAmount(modifier, appliesTo)` — Valley Flamecaller (add fixed amount to damage from matching sources)
- `ReplaceDamageWithCounters(counterType, sacrificeThreshold?, appliesTo)` — Force Bubble (damage → counters on this permanent, sacrifice at threshold)
- `DamageCantBePrevented(appliesTo)` — Sunspine Lynx, Leyline of Punishment. All damage is treated as though it can't be prevented (protection, prevention shields, etc. are ignored).

### Draw

- `PreventDraw(appliesTo)` — Narset
- `ReplaceDrawWithEffect(replacementEffect, appliesTo, optional)` — Underrealm Lich

### Life

- `PreventLifeGain(appliesTo)` — Erebos
- `PreventExtraTurns(appliesTo)` — Ugin's Nexus. Prevents any player from taking extra turns. Checked by TakeExtraTurnExecutor and applyReplacementAdditionalEffect.
- `ReplaceLifeGain(replacementEffect, appliesTo)` — Tainted Remedy
- `ModifyLifeGain(multiplier, appliesTo)` — Alhammarret's Archive

### Generic

- `GenericReplacementEffect(replacement, appliesTo, description)` — complex scenarios

---

## Additional Costs

Used via `additionalCost(...)` in card DSL for spell additional costs:

- `AdditionalCost.SacrificePermanent(filter, count)` — Natural Order
- `AdditionalCost.DiscardCards(count, filter)` — Force of Will
- `AdditionalCost.PayLife(amount)` — Phyrexian mana
- `AdditionalCost.ExileCards(count, filter, fromZone)` — Delve-style
- `AdditionalCost.ExileVariableCards(minCount, filter, fromZone)` — Variable exile cost (Chill Haunting)
- `AdditionalCost.TapPermanents(count, filter)` — Convoke-style
- `AdditionalCost.BlightOrPay(blightAmount, alternativeManaCost)` — Blight N or pay extra mana (Wild Unraveling)
- `AdditionalCost.BeholdOrPay(filter, alternativeManaCost, storeAs)` — Behold a matching card or pay extra mana; behold reveals but does not exile (Lys Alana Dignitary)
- `AdditionalCost.RemoveCountersFromYourCreatures(totalCount)` — remove N counters distributed across creatures you control (any counter types qualify); payment supplied via `AdditionalCostPayment.distributedCounterRemovals` (Dawnhand Dissident's linked-exile cost)

CostZone enum: `HAND`, `GRAVEYARD`, `LIBRARY`, `BATTLEFIELD`

---

## Activation Restrictions

Used via `restrictions = listOf(...)` in activated abilities:

- `ActivationRestriction.AnyPlayerMay` — any player may activate (not just the controller)
- `ActivationRestriction.OnlyDuringYourTurn`
- `ActivationRestriction.OncePerTurn` — limit activation to once per turn
- `ActivationRestriction.Once` — limit activation to only once ever (permanent lifetime)
- `ActivationRestriction.BeforeStep(step)` / `DuringPhase(phase)` / `DuringStep(step)`
- `ActivationRestriction.OnlyIfCondition(condition)`
- `ActivationRestriction.All(restrictions...)` — combine multiple

---

## EffectTarget Types

All target references for effects (sealed interface):

- `EffectTarget.Controller` — controller of the source
- `EffectTarget.Self` — the source permanent (or iteration target in group effects)
- `EffectTarget.EnchantedCreature` — creature this aura enchants
- `EffectTarget.EquippedCreature` — creature this equipment is attached to
- `EffectTarget.TargetController` — controller of the target
- `EffectTarget.ContextTarget(index)` — cast-time target at position
- `EffectTarget.BoundVariable(name)` — named cast-time target (matches `TargetRequirement.id`)
- `EffectTarget.StoredEntityTarget(variableName)` — entity stored in execution context
- `EffectTarget.PlayerRef(player: Player)` — a player or set of players
- `EffectTarget.GroupRef(filter: GroupFilter)` — a group of permanents
- `EffectTarget.FilteredTarget(filter: TargetFilter)` — any game object matching filter
- `EffectTarget.SpecificEntity(entityId)` — a specific entity by ID
- `EffectTarget.PipelineTarget(collectionName, index)` — target from a pipeline collection
- `EffectTarget.ChosenCreature` — creature chosen when permanent entered (reads `ChosenCreatureComponent`)
- `EffectTarget.TriggeringEntity` — the entity that caused the trigger
- `EffectTarget.ControllerOfTriggeringEntity` — controller of the triggering entity
- `EffectTarget.ControllerOfPipelineTarget(collectionName, index)` — controller of a pipeline-stored entity

---

## Player References

Used in effects and targets (sealed interface):

- `Player.You` / `Player.Opponent` / `Player.EachOpponent` / `Player.Each`
- `Player.ActivePlayerFirst` / `Player.Any`
- `Player.TargetPlayer` / `Player.TargetOpponent`
- `Player.ContextPlayer(index)` / `Player.TriggeringPlayer`
- `Player.ControllerOf(targetDescription)` / `Player.OwnerOf(targetDescription)`

---

## PayCost Types

Used in `OptionalCostEffect`, `MayPayManaEffect`, `AnyPlayerMayPayEffect`, `PayOrSufferEffect`:

- `PayCost.Mana(cost: ManaCost)`
- `PayCost.Discard(filter, count, random)`
- `PayCost.Sacrifice(filter, count)`
- `PayCost.PayLife(amount)`
- `PayCost.ReturnToHand(filter, count)` — Return permanents you control to hand (e.g., morph cost)
- `PayCost.RevealCard(filter, count)` — Reveal cards from hand matching filter (e.g., "Morph—Reveal a white card")
- `PayCost.Choice(options: List<PayCost>)` — Choose one of several costs to pay (e.g., "sacrifice a nonland permanent or discard a card")

---

## Duration Types

- `Duration.EndOfTurn` / `.UntilYourNextTurn` / `.UntilYourNextUpkeep`
- `Duration.EndOfCombat` / `.Permanent`
- `Duration.WhileSourceOnBattlefield(sourceDescription)` / `.WhileSourceTapped(sourceDescription)`
- `Duration.UntilPhase(phase)` / `.UntilCondition(conditionDescription)`

---

## GameEvent Types (for Triggers and ReplacementEffect appliesTo)

- `DamageEvent(recipient, source, damageType)` / `DealsDamageEvent(damageType, recipient, sourceFilter?)`
- `DamageReceivedEvent(source)` — self receives damage
- `ZoneChangeEvent(filter, from?, to?)` — zone transitions
- `CounterPlacementEvent(counterType, recipient)` / `TokenCreationEvent(controller, tokenFilter?)`
- `DrawEvent(player)` / `LifeGainEvent(player)` / `LifeLossEvent(player)`
- `DiscardEvent(player, cardFilter?)` / `SearchLibraryEvent(player)`
- `ExtraTurnEvent(player)` — used by PreventExtraTurns replacement effect filter
- `AttackEvent` / `YouAttackEvent(minAttackers, attackerFilter: GameObjectFilter? = null)` / `BlockEvent` / `BecomesBlockedEvent(filter: GameObjectFilter? = null)`
- `BecomesTargetEvent(targetFilter, byYou: Boolean = false, firstTimeEachTurn: Boolean = false)` — when a permanent becomes the target of a spell or ability; `byYou` restricts to spells/abilities controlled by trigger's controller; `firstTimeEachTurn` restricts to first time each turn (Valiant)
- `StepEvent(step, player)`
- `SpellCastEvent(spellType, manaValueAtLeast?, manaValueAtMost?, manaValueEquals?, player, subtype?, orSubtype?)` — `orSubtype` enables OR logic: matches if spellType matches OR spell has the given subtype (e.g., "noncreature or Otter spell")
- `NthSpellCastEvent(nthSpell, player)` — fires when a player's per-turn spell count reaches exactly N
- `CycleEvent(player)` / `TapEvent` / `UntapEvent`
- `TurnFaceUpEvent` / `TransformEvent(intoBackFace?)` / `ControlChangeEvent`
- `OneOrMoreDealCombatDamageToPlayerEvent(sourceFilter)` — batching trigger: "whenever one or more [filter] you control deal combat damage to a player" (fires at most once per combat damage step)
- `LeaveBattlefieldWithoutDyingEvent(filter, excludeSelf)` — batching trigger: "whenever one or more [filter] you control leave the battlefield without dying" (fires at most once per event batch; excludeSelf for "other" clause)
- `PermanentsEnteredEvent(filter)` — batching trigger: "whenever one or more [filter] permanents you control enter the battlefield" (fires at most once per event batch)

### Event Filters

- **RecipientFilter**: `Any`, `You`, `Opponent`, `AnyPlayer`, `CreatureYouControl`, `CreatureOpponentControls`, `AnyCreature`, `PermanentYouControl`, `AnyPermanent`, `Self`, `EnchantedCreature`, `EquippedCreature`, `Matching(filter)`
- **SourceFilter**: `Any`, `Combat`, `NonCombat`, `Spell`, `Ability`, `HasColor(color)`, `HasType(type)`, `EnchantedCreature`, `Creature`, `Matching(filter)`
- **DamageType**: `Any`, `Combat`, `NonCombat`

---

## Key File Paths

| File                                                                                             | Purpose                              |
|--------------------------------------------------------------------------------------------------|--------------------------------------|
| `mtg-sdk/src/main/kotlin/com/wingedsheep/sdk/dsl/`                                               | DSL facades (Effects, Targets, etc.) |
| `mtg-sdk/src/main/kotlin/com/wingedsheep/sdk/scripting/effect/`                                  | Effect type definitions              |
| `mtg-sdk/src/main/kotlin/com/wingedsheep/sdk/scripting/KeywordAbility.kt`                        | Parameterized keyword abilities      |
| `mtg-sdk/src/main/kotlin/com/wingedsheep/sdk/scripting/ReplacementEffect.kt`                     | Replacement effect types             |
| `mtg-sdk/src/main/kotlin/com/wingedsheep/sdk/scripting/AdditionalCost.kt`                        | Additional cost types                |
| `mtg-sdk/src/main/kotlin/com/wingedsheep/sdk/scripting/ActivationRestriction.kt`                 | Activation restrictions              |
| `mtg-sdk/src/main/kotlin/com/wingedsheep/sdk/core/Keyword.kt`                                    | Keyword enum                         |
| `rules-engine/src/main/kotlin/com/wingedsheep/engine/handlers/effects/`                          | Effect executors (14 categories)     |
| `rules-engine/src/main/kotlin/com/wingedsheep/engine/handlers/effects/EffectExecutorRegistry.kt` | Executor registry                    |
| `mtg-sets/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/{set}/cards/`                     | Card definitions                     |
| `mtg-sets/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/{set}/{Set}Set.kt`                | Set card lists                       |
| `game-server/src/test/kotlin/com/wingedsheep/gameserver/scenarios/`                              | Scenario tests                       |

### Effect Executor Categories

| Directory      | Covers                                                                   |
|----------------|--------------------------------------------------------------------------|
| `combat/`      | Must-block, taunt, reflect, prevent, evasion, remove-from-combat         |
| `composite/`   | Composite, conditional, may, for-each, repeat, modal, delayed triggers   |
| `damage/`      | Single, group, player, divided, fight                                    |
| `drawing/`     | Draw, discard, wheel, each-player, look-at-hand, reveal                  |
| `information/` | Look at hand, reveal, face-down                                          |
| `library/`     | Scry, mill, search, reorder, shuffle, pipeline                           |
| `life/`        | Gain, lose, half, owner-gains, set-life                                  |
| `mana/`        | Colored, colorless, any-color, dynamic                                   |
| `permanent/`   | Tap, stats, keywords, counters, control, type-change, animate, transform |
| `player/`      | Skip phases, extra turns, extra lands, extra combat, global abilities    |
| `chain/`       | Chain copy (unified Chain of X executor)                                 |
| `removal/`     | Destroy-all, sacrifice, zone-move, can't-regen, exile-until             |
| `stack/`       | Counter spell, counter with filter, counter unless pays, change target   |
| `token/`       | Create token, create treasure, create chosen token                       |

---

## Test Helper Methods

### Scenario Builder

- `scenario().withPlayers(name1, name2)` — create 2-player game
- `.withCardInHand(playerNum, cardName)` — add card to hand
- `.withCardOnBattlefield(playerNum, cardName, tapped?, summoningSickness?)` — add permanent
- `.withLandsOnBattlefield(playerNum, landName, count)` — add lands
- `.withCardInGraveyard(playerNum, cardName)` — add to graveyard
- `.withCardInLibrary(playerNum, cardName)` — add to library
- `.withLifeTotal(playerNum, life)` — set life total
- `.inPhase(phase, step)` — set game phase
- `.withActivePlayer(playerNum)` — set active player
- `.build()` — create TestGame

### Game Actions

- `game.castSpell(playerNum, spellName, targetId?)` — cast spell
- `game.castSpellTargetingPlayer(playerNum, spellName, targetPlayerNum)` — cast targeting player
- `game.castSpellTargetingGraveyardCard(playerNum, spellName, graveyardCardName)` — cast targeting graveyard card
- `game.castXSpell(playerNum, spellName, xValue, targetId?)` — cast X spell
- `game.resolveStack()` — resolve stack (pass priority)
- `game.passPriority()` — pass priority once

### Game Queries

- `game.findPermanent(name)` — find permanent by name
- `game.getLifeTotal(playerNum)` — get life total
- `game.handSize(playerNum)` — get hand size
- `game.graveyardSize(playerNum)` — get graveyard size
- `game.isOnBattlefield(cardName)` — check if on battlefield
- `game.isInGraveyard(playerNum, cardName)` — check if in graveyard

### Decision Handling

- `game.hasPendingDecision()` — check for pending decision
- `game.selectTargets(entityIds)` — submit target selection
- `game.skipTargets()` — skip optional targets
- `game.answerYesNo(choice)` — submit yes/no response
- `game.selectCards(cardIds)` — submit card selection
- `game.submitDistribution(map)` — submit distribution (divided damage)

---

## E2E Test Helpers (Playwright)

E2E tests use the `GamePage` page object from `e2e-scenarios/helpers/gamePage.ts`.

### Setup

- **Fixture**: `createGame(config: ScenarioRequest)` — creates game via dev API, returns `{ player1, player2 }`
- **Import**: `import { test, expect } from '../../fixtures/scenarioFixture'`
- **Access**: `player1.gamePage` — the `GamePage` instance, `player1.playerId` — for life total assertions

### ScenarioRequest Config

- `player1` / `player2`:
  `{ hand?: string[], battlefield?: BattlefieldCardConfig[], graveyard?: string[], library?: string[], lifeTotal?: number }`
- `BattlefieldCardConfig`: `{ name: string, tapped?: boolean, summoningSickness?: boolean }`
- `phase`: `'BEGINNING'` | `'PRECOMBAT_MAIN'` | `'COMBAT'` | `'POSTCOMBAT_MAIN'` | `'ENDING'`
- `step`: Step name string (e.g., `'UPKEEP'`, `'DECLARE_ATTACKERS'`)
- `activePlayer` / `priorityPlayer`: `1` or `2`
- `player1StopAtSteps` / `player2StopAtSteps`: `string[]` — step names where auto-pass is disabled

### GamePage — Card Interaction

- `clickCard(name)` — click a card by img alt text (first match on page)
- `selectCardInHand(name)` — click a card scoped to the hand zone
- `selectAction(label)` — click an action menu button by partial text match
- `castFaceDown(name)` — click card + select "Cast Face-Down"
- `turnFaceUp(name)` — click face-down card + select "Turn Face-Up"

### GamePage — Targeting

- `selectTarget(name)` — click a target card on the battlefield
- `selectTargetInStep(name)` — click target inside targeting step modal
- `confirmTargets()` — click "Confirm Target" / "Confirm (N)" button
- `skipTargets()` — click "Decline" / "Select None" button
- `selectPlayer(playerId)` — click player's life display to target them

### GamePage — Priority & Stack

- `pass()` — click Pass / Resolve / End Turn button
- `resolveStack(stackItemText)` — wait for stack item text, then pass

### GamePage — Decisions

- `answerYes()` / `answerNo()` — may-effect yes/no buttons
- `selectNumber(n)` — select number + confirm
- `selectOption(text)` — select option + confirm
- `selectXValue(x)` — set X slider value + cast/activate
- `selectManaColor(color)` — select mana color from overlay
- `waitForDecision(timeout?)` — wait for any decision UI

### GamePage — Combat

- `attackAll()` — click "Attack All" + confirm
- `attackWith(name)` — declare single attacker + confirm
- `declareAttacker(name)` — click creature to toggle as attacker
- `skipAttacking()` — click "Skip Attacking"
- `declareBlocker(blockerName, attackerName)` — drag-and-drop blocker
- `confirmBlockers()` — click "Confirm Blocks"
- `noBlocks()` — click "No Blocks"
- `confirmBlockerOrder()` — confirm multiple blocker damage order

### GamePage — Overlays & Selections

- `selectCardInZoneOverlay(name)` — click card in graveyard/library overlay
- `selectCardInDecision(name)` — click card in discard/sacrifice overlay
- `confirmSelection()` — click "Confirm Selection" / "Confirm"
- `failToFind()` — click "Fail to Find" in library search
- `dismissRevealedCards()` — click "OK" to dismiss revealed cards

### GamePage — Damage Distribution

- `increaseDamageAllocation(name, times)` — click "+" in DamageDistributionModal
- `castSpellFromDistribution()` — click "Cast Spell" from distribution modal
- `allocateDamage(name, amount)` — click card N times in combat damage mode
- `allocateDamageToPlayer(playerId, amount)` — click player N times
- `increaseCombatDamage(name, times)` / `decreaseCombatDamage(name, times)` — combat damage +/-
- `confirmDamage()` — click "Confirm Damage"
- `confirmDistribution()` — click "Confirm" in distribute bar

### GamePage — Assertions

- `expectOnBattlefield(name)` / `expectNotOnBattlefield(name)`
- `expectInHand(name)` / `expectNotInHand(name)` / `expectHandSize(count)`
- `expectLifeTotal(playerId, value)`
- `expectGraveyardSize(playerId, size)`
- `expectStats(name, "3/3")`
- `expectTapped(name)` / `expectUntapped(name)`
- `expectGhostCardInHand(name)` / `expectNoGhostCardInHand(name)`
- `screenshot(stepName)` — capture screenshot for report
