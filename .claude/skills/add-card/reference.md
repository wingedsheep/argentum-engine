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

- `Effects.GainLife(amount, target = Controller)`
- `Effects.LoseLife(amount, target = TargetOpponent)` — also accepts `DynamicAmount`

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
- `Effects.ShuffleIntoLibrary(target)`
- `Effects.PutOntoBattlefield(target, tapped = false)`
- `Effects.ShuffleGraveyardIntoLibrary(target)` — shuffle graveyard into library

### Stats & Keywords

- `Effects.ModifyStats(power, toughness, target = ContextTarget(0))` — until end of turn
- `Effects.ModifyStats(power: DynamicAmount, toughness: DynamicAmount, target)` — dynamic P/T
- `Effects.GrantKeyword(keyword, target = ContextTarget(0))` — until end of turn
- `Effects.AddCounters(counterType, count, target)`
- `Effects.AnimateLand(target, power, toughness, duration)` — turn land into creature

### Mass Effects (group)

- `Effects.DestroyAll(filter: GroupFilter, noRegenerate = false)` — board wipe
- `Effects.GrantKeywordToAll(keyword, filter: GroupFilter, duration)` — keyword to group
- `Effects.RemoveKeywordFromAll(keyword, filter: GroupFilter, duration)` — remove keyword from group
- `Effects.ModifyStatsForAll(power, toughness, filter: GroupFilter, duration)` — P/T for group
- `Effects.DealDamageToAll(amount, filter: GroupFilter)` — also accepts `DynamicAmount`

### Control

- `Effects.GainControl(target, duration = Permanent)` — gain control of target
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

### Tokens

- `Effects.CreateToken(power, toughness, colors, creatureTypes, keywords, count = 1)`
- `Effects.CreateTreasure(count = 1)`

### Library

- `Effects.SearchLibrary(filter, count = 1, destination = HAND, entersTapped, shuffle, reveal)`
- `Effects.Scry(count)` — returns CompositeEffect (Gather → Select → Move pipeline)
- `Effects.Surveil(count)` — returns CompositeEffect (Gather → Select → Move pipeline)
- `Effects.Mill(count, target = Controller)`
- `Effects.HeadGames(target)` — look at target's hand, rearrange library
-
`Effects.EachPlayerRevealCreaturesCreateTokens(tokenPower, tokenToughness, tokenColors, tokenCreatureTypes, tokenImageUri?)` —
each player reveals and creates tokens
- `Effects.EachPlayerSearchesLibrary(filter, count: DynamicAmount)` — each player searches

### Stack

- `Effects.CounterSpell()`
- `Effects.CounterUnlessPays(cost: String)` — counter unless mana paid
- `Effects.CounterUnlessDynamicPays(amount: DynamicAmount)` — counter unless dynamic amount paid
- `Effects.ChangeSpellTarget(targetMustBeSource = false)` — redirect a spell's target

### Sacrifice

- `Effects.Sacrifice(filter, count = 1, target = Controller)`

### Tap/Untap

- `Effects.Tap(target)` / `Effects.Untap(target)`
- `Effects.UntapGroup(filter: GroupFilter)` — untap all matching
- `Effects.TapAll(filter: GroupFilter)` — tap all matching

### Permanent Manipulation

- `Effects.SeparatePermanentsIntoPiles(target)` — separate into piles
- `Effects.RemoveFromCombat(target)` — remove creature from combat

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
| `DealDamageEffect`          | `amount: DynamicAmount, target, cantBePrevented, damageSource?` | Damage to single target   |
| `DealDamageToPlayersEffect` | `amount: DynamicAmount, target`                                 | Damage to players         |
| `DividedDamageEffect`       | `totalDamage, minTargets, maxTargets`                           | Divided damage allocation |
| `FightEffect`               | `target1, target2`                                              | Two creatures fight       |
| `ChainCopyEffect`           | `action, target, targetFilter?, copyRecipient, copyCost, copyTargetRequirement, spellName` | Unified chain copy (all Chain of X) |

### Life

| Effect                                   | Parameters                       | Purpose                   |
|------------------------------------------|----------------------------------|---------------------------|
| `GainLifeEffect`                         | `amount: DynamicAmount, target`  | Gain life                 |
| `LoseLifeEffect`                         | `amount: DynamicAmount, target`  | Lose life                 |
| `PayLifeEffect`                          | `amount: Int`                    | Pay life cost             |
| `LoseHalfLifeEffect`                     | `roundUp, target`                | Lose half life total      |
| `OwnerGainsLifeEffect`                   | `amount: DynamicAmount`          | Card owner gains life     |
| `GainLifeForEachLandOnBattlefieldEffect` | `landType, lifePerLand`          | Life per lands            |
| `SetLifeTotalForEachPlayerEffect`        | `perPlayerAmount: DynamicAmount` | Set life total per player |

### Drawing & Hand

| Effect                                   | Parameters                         | Purpose                         |
|------------------------------------------|------------------------------------|---------------------------------|
| `DrawCardsEffect`                        | `count: DynamicAmount, target`     | Draw cards                      |
| `DrawUpToEffect`                         | `maxCards: Int, target, lifePerCardNotDrawn` | Draw up to N (with optional life gain) |
| `EachOpponentDiscardsEffect`             | `count, controllerDrawsPerDiscard` | Each opponent discards          |
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
| `MoveToZoneEffect`                          | `target, destination: Zone, placement, byDestruction` | Unified zone movement    |
| `SacrificeEffect`                           | `filter, count, any`                                  | Sacrifice permanents     |
| `SacrificeSelfEffect`                       | (object)                                              | Sacrifice this permanent |
| `ForceSacrificeEffect`                      | `filter, count, target`                               | Force opponent sacrifice |
| `RegenerateEffect`                          | `target`                                              | Regenerate               |
| `CantBeRegeneratedEffect`                   | `target`                                              | Prevents regeneration    |
| `MarkExileOnDeathEffect`                    | `target`                                              | Mark for exile on death  |
| `ExileUntilLeavesEffect`                    | `target`                                              | O-Ring style exile       |
| `ExileAndReplaceWithTokenEffect`            | `target, tokenPower/Toughness/Colors/Types/Keywords`  | Exile + token            |
| `SeparatePermanentsIntoPilesEffect`         | `target`                                              | Separate into piles      |
| `DestroyAtEndOfCombatEffect`                | `target`                                              | Destroy at end of combat |
| `DestroyAllSharingTypeWithSacrificedEffect` | `noRegenerate`                                        | Destroy all sharing type |
| `HarshMercyEffect`                          | (object)                                              | Harsh Mercy              |
| `PatriarchsBiddingEffect`                   | (object)                                              | Patriarch's Bidding      |

### Permanent Modification

| Effect                                      | Parameters                                                                  | Purpose                                  |
|---------------------------------------------|-----------------------------------------------------------------------------|------------------------------------------|
| `TapUntapEffect`                            | `target, tap: Boolean`                                                      | Tap or untap                             |
| `TapTargetCreaturesEffect`                  | `maxTargets`                                                                | Tap up to X targets                      |
| `ModifyStatsEffect`                         | `power: DynamicAmount, toughness: DynamicAmount, target, duration`          | P/T for single target                    |
| `GrantKeywordUntilEndOfTurnEffect`          | `keyword, target, duration`                                                 | Keyword for single target                |
| `RemoveKeywordUntilEndOfTurnEffect`         | `keyword, target, duration`                                                 | Remove keyword from single target        |
| `GrantTriggeredAbilityUntilEndOfTurnEffect` | `ability, target, duration`                                                 | Grant triggered ability                  |
| `GrantActivatedAbilityUntilEndOfTurnEffect` | `ability, target, duration`                                                 | Grant activated ability                  |
| `GrantActivatedAbilityToGroupEffect`        | `ability, filter, duration`                                                 | Grant activated ability to group         |
| `AddCountersEffect`                         | `counterType, count, target`                                                | Add counters                             |
| `RemoveCountersEffect`                      | `counterType, count, target`                                                | Remove counters                          |
| `AddMinusCountersEffect`                    | `count, target`                                                             | Add -1/-1 counters                       |
| `LoseAllCreatureTypesEffect`                | `target, duration`                                                          | Remove all creature types                |
| `TransformAllCreaturesEffect`               | `target, loseAllAbilities, addCreatureType, setBasePower, setBaseToughness` | Transform creatures                      |
| `ChangeCreatureTypeTextEffect`              | `target, excludedTypes`                                                     | Change creature type text                |
| `BecomeCreatureTypeEffect`                  | `target, duration, excludedTypes`                                           | Become a creature type                   |
| `BecomeChosenTypeAllCreaturesEffect`        | `excludedTypes, duration`                                                   | All creatures become chosen type         |
| `SetGroupCreatureSubtypesEffect`            | `subtypes, filter, duration`                                                | Set group subtypes                       |
| `ChangeGroupColorEffect`                    | `colors, filter, duration`                                                  | Change group color                       |
| `ChooseCreatureTypeModifyStatsEffect`       | `power: DynamicAmount, toughness: DynamicAmount, duration`                  | Choose type, modify stats                |
| `ChooseCreatureTypeUntapEffect`             | (object)                                                                    | Choose type, untap all of that type      |
| `GainControlEffect`                         | `target, duration`                                                          | Gain control                             |
| `GainControlByActivePlayerEffect`           | `target`                                                                    | Active player gains control              |
| `GainControlByMostOfSubtypeEffect`          | `subtype, target`                                                           | Control by most of subtype               |
| `GiveControlToTargetPlayerEffect`           | `permanent, newController, duration`                                        | Give control to target                   |
| `ChooseCreatureTypeGainControlEffect`       | `duration`                                                                  | Choose type, gain control                |
| `GrantToEnchantedCreatureTypeGroupEffect`   | `powerModifier, toughnessModifier, keyword?, protectionColors, duration`    | Grant to enchanted creature's type group |
| `AnimateLandEffect`                         | `target, power, toughness, duration`                                        | Animate land                             |
| `TurnFaceDownEffect`                        | `target`                                                                    | Turn face down                           |
| `TurnFaceUpEffect`                          | `target`                                                                    | Turn face up                             |
| `TransformEffect`                           | `target`                                                                    | Transform DFC                            |
| `RemoveFromCombatEffect`                    | `target`                                                                    | Remove from combat                       |

### Library

| Effect                                                                              | Parameters                                          | Purpose                                           |
|-------------------------------------------------------------------------------------|-----------------------------------------------------|---------------------------------------------------|
| `ShuffleLibraryEffect`                                                              | `target`                                            | Shuffle library                                   |
| `PutCreatureFromHandSharingTypeWithTappedEffect`                                    | (object)                                            | Put creature from hand sharing type               |
| `EffectPatterns.lookAtTopAndKeep(count, keepCount)`                                 | `count, keepCount, keepDest?, restDest?, revealed?` | Look at top N keep some (pipeline)                |
| `EffectPatterns.lookAtTopAndReorder(count)`                                         | `count: Int` or `count: DynamicAmount`              | Look at top and reorder (pipeline)                |
| `EffectPatterns.lookAtTopXAndPutOntoBattlefield(countSource, filter, shuffleAfter)` | CoCo-style (pipeline)                               |
| `EffectPatterns.lookAtTargetLibraryAndDiscard(count, toGraveyard)`                  | `count, toGraveyard`                                | Look at target's library, discard some (pipeline) |
| `EffectPatterns.searchTargetLibraryExile(count, filter)`                            | Search target's library and exile (pipeline)        |

### Mana

| Effect                   | Parameters                                   | Purpose            |
|--------------------------|----------------------------------------------|--------------------|
| `AddManaEffect`          | `color, amount: DynamicAmount`               | Add colored mana   |
| `AddColorlessManaEffect` | `amount: DynamicAmount`                      | Add colorless mana |
| `AddAnyColorManaEffect`  | `amount: DynamicAmount`                      | Add any color mana |
| `AddDynamicManaEffect`   | `amountSource: DynamicAmount, allowedColors` | Dynamic mana       |

### Tokens

| Effect                           | Parameters                                                                                  | Purpose                        |
|----------------------------------|---------------------------------------------------------------------------------------------|--------------------------------|
| `CreateTokenEffect`              | `count: DynamicAmount, power, toughness, colors, creatureTypes, keywords, name?, imageUri?` | Create token                   |
| `CreateChosenTokenEffect`        | `dynamicPower: DynamicAmount, dynamicToughness: DynamicAmount`                              | Create token with chosen stats |
| `CreateTreasureTokensEffect`     | `count: DynamicAmount`                                                                      | Create Treasure                |
| `CreateTokenFromGraveyardEffect` | `power, toughness, colors, creatureTypes`                                                   | Token from graveyard           |

### Composite & Control Flow

| Effect                       | Parameters                                            | Purpose                        |
|------------------------------|-------------------------------------------------------|--------------------------------|
| `CompositeEffect`            | `effects: List<Effect>`                               | Chain multiple effects         |
| `MayEffect`                  | `effect, descriptionOverride?`                        | "You may..."                   |
| `ModalEffect`                | `modes: List<Mode>, chooseCount`                      | "Choose one/two..."            |
| `OptionalCostEffect`         | `cost, ifPaid, ifNotPaid?`                            | "You may [cost]. If you do..." |
| `ReflexiveTriggerEffect`     | `action, optional, reflexiveEffect`                   | "When you do..."               |
| `PayOrSufferEffect`          | `cost: PayCost, suffer, player`                       | "Unless [cost], [suffer]"      |
| `StoreResultEffect`          | `effect, storeAs: EffectVariable`                     | Store result for later         |
| `StoreCountEffect`           | `effect, storeAs`                                     | Store count for later          |
| `CreateDelayedTriggerEffect` | `step: Step, effect`                                  | Create delayed trigger at step |
| `BlightEffect`               | `blightAmount: DynamicAmount, innerEffect, targetId?` | Blight effect                  |
| `TapCreatureForEffectEffect` | `innerEffect, targetId?`                              | Tap creature for effect        |
| `MayPayManaEffect`           | `cost: ManaCost, effect`                              | May pay mana for effect        |
| `AnyPlayerMayPayEffect`      | `cost: PayCost, consequence`                          | Any player may pay             |
| `ForEachTargetEffect`        | `effects: List<Effect>`                               | Iterate per target             |
| `ForEachPlayerEffect`        | `players: Player, effects: List<Effect>`              | Iterate per player             |
| `FlipCoinEffect`             | `wonEffect?, lostEffect?`                             | Flip a coin                    |
| `RepeatWhileEffect`          | `body, repeatCondition`                               | Repeat while condition         |
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
| `CantBlockTargetCreaturesEffect`                    | `duration`                                  | Can't block target creatures    |
| `PreventNextDamageEffect`                           | `amount: DynamicAmount, target`             | Prevent next N damage           |
| `RemoveFromCombatEffect`                            | `target`                                    | Remove from combat              |
| `ChooseCreatureTypeMustAttackEffect`                | (object)                                    | Choose type, must attack        |
| `RedirectNextDamageEffect`                          | `protectedTargets, redirectTo`              | Redirect next damage            |
| `PreventNextDamageFromChosenCreatureTypeEffect`     | (object)                                    | Prevent damage from chosen type |
| `PreventAllDamageDealtByTargetEffect`               | `target`                                    | Prevent all damage by target    |

### Player

| Effect                                             | Parameters                               | Purpose                            |
|----------------------------------------------------|------------------------------------------|------------------------------------|
| `SkipCombatPhasesEffect`                           | `target`                                 | Skip combat                        |
| `SkipUntapEffect`                                  | `target, affectsCreatures, affectsLands` | Skip untap                         |
| `PlayAdditionalLandsEffect`                        | `count`                                  | Play extra lands                   |
| `AddCombatPhaseEffect`                             | (object)                                 | Additional combat phase            |
| `TakeExtraTurnEffect`                              | `loseAtEndStep`                          | Extra turn                         |
| `PreventLandPlaysThisTurnEffect`                   | (object)                                 | Prevent land plays                 |
| `CreateGlobalTriggeredAbilityUntilEndOfTurnEffect` | `ability: TriggeredAbility`              | Global triggered ability until EOT |

### Stack

| Effect                           | Parameters                    | Purpose                    |
|----------------------------------|-------------------------------|----------------------------|
| `CounterSpellEffect`             | (object)                      | Counter target spell       |
| `CounterSpellWithFilterEffect`   | `filter: GameObjectFilter`    | Counter matching spell     |
| `CounterUnlessPaysEffect`        | `cost: ManaCost`              | Counter unless pays        |
| `CounterUnlessDynamicPaysEffect` | `amount: DynamicAmount`       | Counter unless dynamic pay |
| `ChangeSpellTargetEffect`        | `targetMustBeSource: Boolean` | Redirect spell target      |

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
- `Targets.AttackingCreature` / `Targets.BlockingCreature` / `Targets.TappedCreature`
- `Targets.CreatureWithKeyword(keyword)` / `Targets.CreatureWithColor(color)`
- `Targets.CreatureWithPowerAtMost(maxPower)` / `Targets.UpToCreatures(count)`

### Permanent

- `Targets.Permanent` / `Targets.NonlandPermanent`
- `Targets.Artifact` / `Targets.Enchantment` / `Targets.Land`
- `Targets.PermanentOpponentControls`

### Combined

- `Targets.Any` — creature, player, or planeswalker
- `Targets.CreatureOrPlayer` / `Targets.CreatureOrPlaneswalker`

### Graveyard

- `Targets.CardInGraveyard` / `Targets.CreatureCardInGraveyard`
- `Targets.CreatureCardInYourGraveyard` / `Targets.InstantOrSorceryInGraveyard`

### Spell (on stack)

- `Targets.Spell` / `Targets.CreatureSpell` / `Targets.NoncreatureSpell`
- `Targets.CreatureOrSorcerySpell`
- `Targets.SpellWithManaValueAtMost(manaValue)`

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
- `Triggers.OtherCreatureEnters` / `Triggers.OtherCreatureWithSubtypeDies(subtype)`
- `Triggers.LeavesBattlefield` / `Triggers.Dies` / `Triggers.AnyCreatureDies` / `Triggers.YourCreatureDies`
- `Triggers.PutIntoGraveyardFromBattlefield`

### Combat

- `Triggers.Attacks` / `Triggers.AnyAttacks` / `Triggers.YouAttack`
- `Triggers.Blocks` / `Triggers.BecomesBlocked` / `Triggers.CreatureYouControlBecomesBlocked`
- `Triggers.DealsDamage` / `Triggers.DealsCombatDamage`
- `Triggers.DealsCombatDamageToPlayer` / `Triggers.DealsCombatDamageToCreature`

### Phase/Step

- `Triggers.YourUpkeep` / `Triggers.EachUpkeep`
- `Triggers.YourEndStep` / `Triggers.EachEndStep`
- `Triggers.BeginCombat` / `Triggers.FirstMainPhase`
- `Triggers.EnchantedCreatureControllerUpkeep` — enchanted creature's controller's upkeep
- `Triggers.EnchantedCreatureControllerEndStep` — enchanted creature's controller's end step
- `Triggers.TurnedFaceUp` — self turns face up
- `Triggers.GainControlOfSelf` — you gain control of self

### Spell

- `Triggers.YouCastSpell` / `Triggers.YouCastCreature`
- `Triggers.YouCastNoncreature` / `Triggers.YouCastInstantOrSorcery`
- `Triggers.YouCastEnchantment`

### Card Drawing

- `Triggers.YouDraw` / `Triggers.AnyPlayerDraws`

### Damage

- `Triggers.TakesDamage` / `Triggers.DamagedByCreature` / `Triggers.DamagedBySpell`

### Tap/Untap

- `Triggers.BecomesTapped` / `Triggers.BecomesUntapped`

### Cycle

- `Triggers.YouCycle` / `Triggers.AnyPlayerCycles`

### Life

- `Triggers.YouGainLife` / `Triggers.AnyPlayerGainsLife`

### Transform

- `Triggers.Transforms` / `Triggers.TransformsToBack` / `Triggers.TransformsToFront`

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
- `Filters.Unified.withColor(color)` / `.withSubtype(subtype)` / `.withKeyword(keyword)`
- `Filters.Unified.manaValueAtMost(max)` / `.manaValueAtLeast(min)`

---

## Costs Facade

- `Costs.Tap` / `Costs.Untap`
- `Costs.Mana("2R")` / `Costs.Mana(manaCost)`
- `Costs.PayLife(amount)`
- `Costs.Sacrifice(filter)` / `Costs.SacrificeSelf` / `Costs.SacrificeChosenCreatureType`
- `Costs.DiscardCard` / `Costs.Discard(filter)` / `Costs.DiscardSelf` / `Costs.DiscardHand`
- `Costs.ExileFromGraveyard(count, filter)`
- `Costs.TapAttachedCreature` — tap the creature this is attached to
- `Costs.TapPermanents(count, filter)` — tap N permanents
- `Costs.Loyalty(change)` — planeswalker loyalty
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

### Life Total

- `Conditions.LifeAtMost(threshold)` / `.LifeAtLeast(threshold)`
- `Conditions.MoreLifeThanOpponent` / `.LessLifeThanOpponent`

### Hand & Graveyard

- `Conditions.EmptyHand` / `.CardsInHandAtLeast(count)` / `.CardsInHandAtMost(count)`
- `Conditions.CreatureCardsInGraveyardAtLeast(count)` / `.CardsInGraveyardAtLeast(count)`
- `Conditions.GraveyardContainsSubtype(subtype)`

### Source State

- `Conditions.SourceIsAttacking` / `.SourceIsBlocking`
- `Conditions.SourceIsTapped` / `.SourceIsUntapped`
- `Conditions.SourceHasSubtype(subtype)` — source has specific subtype

### Turn

- `Conditions.IsYourTurn` / `.IsNotYourTurn`

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

### Raw DynamicAmount types

- `DynamicAmount.XValue` / `DynamicAmount.Fixed(n)` / `DynamicAmount.YourLifeTotal`
- `DynamicAmount.SacrificedPermanentPower` / `.SacrificedPermanentToughness`
- `DynamicAmount.SourcePower` / `.TriggerDamageAmount` / `.TriggerLifeGainAmount`
- `DynamicAmount.ColorsAmongPermanentsYouControl` / `.CardTypesInAllGraveyards`
- `DynamicAmount.CountersOnSelf(counterType)` / `.CreaturesSharingTypeWithTriggeringEntity`
- `DynamicAmount.VariableReference(variableName)` / `.StoredCardManaValue(collectionName)`
- `DynamicAmount.Count(player, zone, filter)` /
  `DynamicAmount.AggregateBattlefield(player, filter, aggregation?, property?)`
- `DynamicAmount.Conditional(condition, ifTrue, ifFalse)` — conditional amount
- Fluent: `DynamicAmounts.battlefield(player, filter).count()` / `.maxManaValue()` / `.maxPower()` / `.maxToughness()` /
  `.minToughness()` / `.sumPower()`
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

### Atomic Building Blocks

| Effect                                                                                                                         | Purpose                                          |
|--------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------|
| `GatherCardsEffect(source, storeAs, revealed)`                                                                                 | Gather cards from a zone into a named collection |
| `SelectFromCollectionEffect(from, selection, chooser, filter, storeSelected, storeRemainder, matchChosenCreatureType, prompt)` | Player selects from a collection                 |
| `MoveCollectionEffect(from, destination, order, revealed, moveType)`                                                           | Move a collection to a zone                      |
| `RevealUntilEffect(source, matchFilter, storeMatch, storeRevealed, matchChosenCreatureType)`                                   | Reveal until filter matches                      |
| `ChooseCreatureTypeEffect`                                                                                                     | Choose a creature type (data object)             |
| `SelectTargetEffect(requirement, storeAs)`                                                                                     | Select and store a target                        |

Sources: `CardSource.TopOfLibrary(count, player)`, `CardSource.FromZone(zone, player, filter)`,
`CardSource.FromVariable(name)`, `CardSource.ControlledPermanents(player, filter)`
Destinations: `CardDestination.ToZone(zone, player, placement)`
Placements: `ZonePlacement.Top`, `.Bottom`, `.Shuffled`, `.Default`, `.Tapped`
Selection: `SelectionMode.ChooseExactly(count)`, `.ChooseUpTo(count)`, `.All`, `.Random(count)`, `.ChooseAnyNumber`
Chooser: `Chooser.Controller`, `.Opponent`, `.TargetPlayer`, `.TriggeringPlayer`
Ordering: `CardOrder.ControllerChooses`, `.Random`, `.Preserve`
MoveType: `MoveType.Default`, `.Discard`, `.Sacrifice`

### General Patterns

- `EffectPatterns.mayPay(cost, effect)` — "You may [cost]. If you do, [effect]"
- `EffectPatterns.mayPayOrElse(cost, ifPaid, ifNotPaid)` — with fallback
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
- `EffectPatterns.headGames(target)` — Head Games effect
- `EffectPatterns.wheelEffect(players)` — wheel effect
- `EffectPatterns.discardCards(count, target)` / `.discardRandom(count, target)` / `.discardHand(target)` — discard
  patterns
- `EffectPatterns.putFromHand(filter, count, entersTapped)` — put card from hand onto battlefield
- `EffectPatterns.eachOpponentMayPutFromHand(filter)` — each opponent may put from hand
- `EffectPatterns.eachOpponentDiscards(count)` — each opponent discards
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

`FIRST_STRIKE`, `DOUBLE_STRIKE`, `TRAMPLE`, `DEATHTOUCH`, `LIFELINK`, `VIGILANCE`, `REACH`

### Defense

`DEFENDER`, `INDESTRUCTIBLE`, `HEXPROOF`, `SHROUD`, `WARD`, `PROTECTION`

### Speed

`HASTE`, `FLASH`

### Triggered/Static

`PROWESS`, `CHANGELING`

### Cost Reduction

`CONVOKE`, `DELVE`, `AFFINITY`

### Restrictions

`DOESNT_UNTAP`, `MAY_NOT_UNTAP`

---

## Parameterized Keyword Abilities (KeywordAbility)

Used via `keywordAbility(...)` or `keywordAbilities(...)` in card DSL:

- `KeywordAbility.Simple(keyword)` — wraps a basic keyword
- **Ward**: `WardMana(cost)`, `WardLife(amount)`, `WardDiscard(count, random)`, `WardSacrifice(filter)`
- **Protection**: `ProtectionFromColor(color)`, `ProtectionFromColors(colors)`, `ProtectionFromCardType(type)`,
  `ProtectionFromCreatureSubtype(subtype)`, `ProtectionFromEverything`
- **Combat**: `Annihilator(count)`, `Bushido(count)`, `Rampage(count)`, `Flanking`, `Afflict(count)`
- **Counters**: `Modular(count)`, `Fabricate(count)`, `Renown(count)`, `Tribute(count)`
- **Time**: `Fading(count)`, `Vanishing(count)`
- **Vehicles**: `Crew(power)`
- **Cost**: `Affinity(forType)`, `Cycling(cost)`, `Typecycling(type, cost)`, `Kicker(cost)`, `Multikicker(cost)`
- **Transform**: `Morph(cost)`, `Absorb(count)`

Companion helpers: `KeywordAbility.of(keyword)`, `.ward(cost)`, `.wardLife(amount)`, `.wardDiscard(count, random)`,
`.protectionFrom(color)`, `.protectionFrom(vararg colors)`, `.protectionFromSubtype(subtype)`, `.cycling(cost)`,
`.morph(cost)`, `.morphPayLife(amount)`

---

## Static Abilities

Set via `staticAbility { ability = ... }`:

### Keywords & Stats

- `GrantKeyword(keyword, target: StaticTarget)` — permanent keyword grant
- `GrantKeywordToCreatureGroup(keyword, filter: AffectsFilter)` — keyword to group
- `GrantKeywordForChosenCreatureType(keyword)` — keyword to chosen creature type
- `GrantKeywordByCounter(keyword, counterType)` — grant keyword when counter present
- `ModifyStats(powerBonus, toughnessBonus, target: StaticTarget)` — P/T bonus
- `ModifyStatsForCreatureGroup(powerBonus, toughnessBonus, filter: AffectsFilter)` — P/T to group
- `ModifyStatsForChosenCreatureType(powerBonus, toughnessBonus)` — P/T to chosen type
- `ModifyStatsByCounterOnSource(counterType, powerModPerCounter, toughnessModPerCounter, target)` — P/T per counter
- `GrantDynamicStatsEffect(target, powerBonus: DynamicAmount, toughnessBonus: DynamicAmount)`
- `GlobalEffect(effectType: GlobalEffectType, filter)` — global anthem/debuff
- `GrantProtection(color, target)` — grant protection from color

### Combat Restrictions

- `CantAttack(target)` / `CantBlock(target)` / `MustAttack(target)`
- `CantAttackUnlessDefenderControlsLandType(landType, target)`
- `CantBlockCreaturesWithGreaterPower(target)`
- `CanOnlyBlockCreaturesWithKeyword(keyword, target)`
- `CanBlockAnyNumber(target)` — can block any number of creatures

### Evasion

- `CantBeBlockedByColor(colors, target)` / `CantBeBlockedByPower(minPower, target)`
- `CantBeBlockedExceptByKeyword(requiredKeyword, target)` / `CantBeBlockedByMoreThan(maxBlockers, target)`
- `CantBeBlockedUnlessDefenderSharesCreatureType(minSharedCount, target)`

### Damage

- `AssignDamageEqualToToughness(target, onlyWhenToughnessGreaterThanPower)` — Doran
- `DivideCombatDamageFreely(target)` — divide damage freely

### Type & Subtype

- `AddCreatureTypeByCounter(creatureType, counterType)` — add type when counter present
- `SetEnchantedLandType(landType)` — set enchanted land's type

### Other

- `CantReceiveCounters(target)`
- `ControlEnchantedPermanent` — control the enchanted permanent
- `GrantShroudToController` — controller has shroud
- `AdditionalManaOnTap(color, amount: DynamicAmount)` — produce additional mana
- `PlayFromTopOfLibrary` — play cards from top of library
- `SpellCostReduction(reductionSource)` — cost reduction
- `FaceDownSpellCostReduction(reductionSource)` — face-down spell cost reduction
- `ReduceSpellCostBySubtype(subtype, amount)` — reduce cost per subtype
- `ReduceSpellCostByFilter(filter, amount)` — reduce spell cost for spells matching a GameObjectFilter
- `ReduceFaceDownCastingCost(amount)` — reduce face-down casting cost
- `ConditionalStaticAbility(ability, condition)` — conditional static

### GlobalEffectType values

`ALL_CREATURES_GET_PLUS_ONE_PLUS_ONE`, `YOUR_CREATURES_GET_PLUS_ONE_PLUS_ONE`,
`OPPONENT_CREATURES_GET_MINUS_ONE_MINUS_ONE`, `ALL_CREATURES_HAVE_FLYING`, `YOUR_CREATURES_HAVE_VIGILANCE`,
`YOUR_CREATURES_HAVE_LIFELINK`, `CREATURES_CANT_ATTACK`, `CREATURES_CANT_BLOCK`

### StaticTarget values

`StaticTarget.AttachedCreature`, `SourceCreature`, `Controller`, `AllControlledCreatures`, `SpecificCard(entityId)`

### CostReductionSource values

`ColorsAmongPermanentsYouControl`, `Fixed(amount)`, `CreaturesYouControl`, `TotalPowerYouControl`, `ArtifactsYouControl`

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

- `RedirectZoneChange(newDestination, appliesTo)` — Rest in Peace
- `EntersTapped(appliesTo)` — tap lands
- `EntersWithCounters(counterType, count, appliesTo)` — Master Biomancer
- `EntersWithDynamicCounters(counterType, count: DynamicAmount, appliesTo)` — dynamic counter entry
- `UndyingEffect(appliesTo)` / `PersistEffect(appliesTo)`
- `EntersAsCopy(optional, appliesTo)` — clone effects
- `EntersWithColorChoice(appliesTo)` — choose color on entry
- `EntersWithCreatureTypeChoice(opponentChooses, appliesTo)` — choose creature type on entry

### Damage

- `PreventDamage(amount?, appliesTo)` — Fog, protection (null = prevent all)
- `RedirectDamage(redirectTo, appliesTo)` — Pariah
- `DoubleDamage(appliesTo)` — Furnace of Rath

### Draw

- `ReplaceDrawWithEffect(replacementEffect, appliesTo)` — Underrealm Lich
- `PreventDraw(appliesTo)` — Narset

### Life

- `PreventLifeGain(appliesTo)` — Erebos
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
- `AdditionalCost.TapPermanents(count, filter)` — Convoke-style

CostZone enum: `HAND`, `GRAVEYARD`, `LIBRARY`, `BATTLEFIELD`

---

## Activation Restrictions

Used via `restrictions = listOf(...)` in activated abilities:

- `ActivationRestriction.OnlyDuringYourTurn`
- `ActivationRestriction.BeforeStep(step)` / `DuringPhase(phase)` / `DuringStep(step)`
- `ActivationRestriction.OnlyIfCondition(condition)`
- `ActivationRestriction.All(restrictions...)` — combine multiple

---

## EffectTarget Types

All target references for effects (sealed interface):

- `EffectTarget.Controller` — controller of the source
- `EffectTarget.Self` — the source permanent (or iteration target in group effects)
- `EffectTarget.EnchantedCreature` — creature this aura enchants
- `EffectTarget.TargetController` — controller of the target
- `EffectTarget.ContextTarget(index)` — cast-time target at position
- `EffectTarget.BoundVariable(name)` — named cast-time target (matches `TargetRequirement.id`)
- `EffectTarget.StoredEntityTarget(variableName)` — entity stored in execution context
- `EffectTarget.PlayerRef(player: Player)` — a player or set of players
- `EffectTarget.GroupRef(filter: GroupFilter)` — a group of permanents
- `EffectTarget.FilteredTarget(filter: TargetFilter)` — any game object matching filter
- `EffectTarget.SpecificEntity(entityId)` — a specific entity by ID
- `EffectTarget.PipelineTarget(collectionName, index)` — target from a pipeline collection
- `EffectTarget.TriggeringEntity` — the entity that caused the trigger
- `EffectTarget.ControllerOfTriggeringEntity` — controller of the triggering entity

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
- `AttackEvent` / `YouAttackEvent(minAttackers)` / `BlockEvent` / `BecomesBlockedEvent`
- `StepEvent(step, player)` / `EnchantedCreatureControllerStepEvent(step)`
- `SpellCastEvent(spellType, manaValueAtLeast?, manaValueAtMost?, manaValueEquals?, player)`
- `CycleEvent(player)` / `TapEvent` / `UntapEvent`
- `TurnFaceUpEvent` / `TransformEvent(intoBackFace?)` / `ControlChangeEvent`

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
