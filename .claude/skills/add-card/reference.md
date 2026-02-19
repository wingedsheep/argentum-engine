# Reference: DSL Facades, Effects, Keywords, and Test Helpers

## DSL Facades (Primary API)

Always use these facades for card definitions. They provide type-safe factory methods that abstract away raw constructors.

| Facade | Import | Purpose |
|--------|--------|---------|
| `Effects` | `com.wingedsheep.sdk.dsl.Effects` | Build effect objects |
| `Targets` | `com.wingedsheep.sdk.dsl.Targets` | Build target requirements |
| `Triggers` | `com.wingedsheep.sdk.dsl.Triggers` | Build trigger objects |
| `Filters` | `com.wingedsheep.sdk.dsl.Filters` | Build filters for search/group/target |
| `Costs` | `com.wingedsheep.sdk.dsl.Costs` | Build ability activation costs |
| `Conditions` | `com.wingedsheep.sdk.dsl.Conditions` | Build conditions for conditional effects |
| `DynamicAmounts` | `com.wingedsheep.sdk.dsl.DynamicAmounts` | Build dynamic value expressions |
| `EffectPatterns` | `com.wingedsheep.sdk.dsl.EffectPatterns` | Common multi-effect patterns |

---

## Effects Facade

### Damage
- `Effects.DealDamage(amount: Int, target)` / `Effects.DealDamage(amount: DynamicAmount, target)`
- `Effects.DealXDamage(target)` — shorthand for X-value damage
- `Effects.Drain(amount, target)` — deal damage + gain life

### Life
- `Effects.GainLife(amount, target = Controller)`
- `Effects.LoseLife(amount, target = TargetOpponent)` — also accepts `DynamicAmount`

### Drawing
- `Effects.DrawCards(count, target = Controller)`
- `Effects.Discard(count, target = TargetOpponent)`
- `Effects.Loot(draw = 1, discard = 1)` — draw then discard

### Zone Movement (via `MoveToZoneEffect`)
- `Effects.Destroy(target)`
- `Effects.Exile(target)`
- `Effects.ReturnToHand(target)`
- `Effects.PutOnTopOfLibrary(target)`
- `Effects.ShuffleIntoLibrary(target)`
- `Effects.PutOntoBattlefield(target, tapped = false)`

### Stats & Keywords
- `Effects.ModifyStats(power, toughness, target = ContextTarget(0))` — until end of turn
- `Effects.GrantKeyword(keyword, target = ContextTarget(0))` — until end of turn
- `Effects.AddCounters(counterType, count, target)`

### Mana
- `Effects.AddMana(color, amount = 1)`
- `Effects.AddColorlessMana(amount)`
- `Effects.AddAnyColorMana(amount = 1)`

### Tokens
- `Effects.CreateToken(power, toughness, colors, creatureTypes, keywords, count = 1)`
- `Effects.CreateTreasure(count = 1)`

### Library
- `Effects.SearchLibrary(filter, count = 1, destination = HAND, entersTapped, shuffle, reveal)`
- `Effects.Scry(count)` — returns CompositeEffect (Gather → Select → Move pipeline)
- `Effects.Surveil(count)` — returns CompositeEffect (Gather → Select → Move pipeline)
- `Effects.Mill(count, target = Controller)`

### Stack
- `Effects.CounterSpell()`

### Sacrifice
- `Effects.Sacrifice(filter, count = 1, target = TargetOpponent)`

### Tap/Untap
- `Effects.Tap(target)` / `Effects.Untap(target)`

### Composite
- `Effects.Composite(vararg effects)` — or use `effect1 then effect2` infix operator

---

## All Effect Types (in scripting/effect/)

### Damage
| Effect | Parameters | Purpose |
|--------|------------|---------|
| `DealDamageEffect` | `amount: DynamicAmount, target, cantBePrevented` | Damage to single target |
| `DealDamageToGroupEffect` | `amount: DynamicAmount, filter: GroupFilter` | Damage to creature group |
| `DealDamageToPlayersEffect` | `amount: DynamicAmount, target` | Damage to players |
| `DealDamageToAttackingCreaturesEffect` | `amount: Int` | Damage to attacking creatures |
| `DividedDamageEffect` | `totalDamage, minTargets, maxTargets` | Divided damage allocation |
| `DealDamageExileOnDeathEffect` | `amount, target` | Damage with exile on death |

### Life
| Effect | Parameters | Purpose |
|--------|------------|---------|
| `GainLifeEffect` | `amount: DynamicAmount, target` | Gain life |
| `LoseLifeEffect` | `amount: DynamicAmount, target` | Lose life |
| `PayLifeEffect` | `amount: Int` | Pay life cost |
| `LoseHalfLifeEffect` | `roundUp, target` | Lose half life total |
| `OwnerGainsLifeEffect` | `amount: Int` | Card owner gains life |
| `GainLifeForEachLandOnBattlefieldEffect` | `landType, lifePerLand` | Life per lands |

### Drawing & Hand
| Effect | Parameters | Purpose |
|--------|------------|---------|
| `DrawCardsEffect` | `count: DynamicAmount, target` | Draw cards |
| `DiscardCardsEffect` | `count, target` | Discard (player chooses) |
| `DiscardRandomEffect` | `count, target` | Discard at random |
| `EachOpponentDiscardsEffect` | `count` | Each opponent discards |
| `WheelEffect` | `target` | Discard hand, draw that many |
| `EachPlayerDrawsXEffect` | `includeController, includeOpponents` | Each player draws X |
| `EachPlayerMayDrawEffect` | `maxCards, lifePerCardNotDrawn` | Each player may draw |
| `EachPlayerDiscardsDrawsEffect` | `minDiscard, maxDiscard, discardEntireHand, controllerBonusDraw` | Windfall-style |
| `LookAtTargetHandEffect` | `target` | Look at target's hand |
| `RevealHandEffect` | `target` | Reveal hand |

### Removal & Zone Movement
| Effect | Parameters | Purpose |
|--------|------------|---------|
| `MoveToZoneEffect` | `target, destination: Zone, placement, byDestruction` | Unified zone movement |
| `DestroyAllEffect` | `filter: GroupFilter, noRegenerate` | Board wipe |
| `SacrificeEffect` | `filter, count, any` | Sacrifice permanents |
| `SacrificeSelfEffect` | (object) | Sacrifice this permanent |
| `ForceSacrificeEffect` | `filter, count, target` | Force opponent sacrifice |
| `CantBeRegeneratedEffect` | `target` | Prevents regeneration |
| `ExileUntilLeavesEffect` | `target` | O-Ring style exile |
| `ExileAndReplaceWithTokenEffect` | `target, tokenPower/Toughness/Colors/Types/Keywords` | Exile + token |
| `SeparatePermanentsIntoPilesEffect` | `target` | Separate into piles |

### Permanent Modification
| Effect | Parameters | Purpose |
|--------|------------|---------|
| `TapUntapEffect` | `target, tap: Boolean` | Tap or untap |
| `TapAllCreaturesEffect` | `filter: GroupFilter` | Tap all matching |
| `TapTargetCreaturesEffect` | `maxTargets` | Tap up to X targets |
| `UntapAllCreaturesYouControlEffect` | (object) | Untap all your creatures |
| `ModifyStatsEffect` | `power, toughness, target, duration` | P/T for single target |
| `ModifyStatsForGroupEffect` | `power, toughness, filter: GroupFilter, duration` | P/T for group |
| `GrantKeywordUntilEndOfTurnEffect` | `keyword, target` | Keyword for single target |
| `GrantKeywordToGroupEffect` | `keyword, filter: GroupFilter, duration` | Keyword for group |
| `AddCountersEffect` | `counterType, count, target` | Add counters |
| `RemoveCountersEffect` | `counterType, count, target` | Remove counters |
| `AddMinusCountersEffect` | `count, target` | Add -1/-1 counters |
| `LoseAllCreatureTypesEffect` | `target, duration` | Remove all creature types |
| `TransformAllCreaturesEffect` | `target, loseAllAbilities, addCreatureType, setBasePower, setBaseToughness` | Transform creatures |
| `TransformEffect` | `target` | Transform DFC |

### Library
| Effect | Parameters | Purpose |
|--------|------------|---------|
| `SearchLibraryEffect` | `filter, count, destination, entersTapped, shuffle, reveal` | Tutor |
| `Effects.Scry(count)` | `count` | Scry N (pipeline pattern) |
| `Effects.Surveil(count)` | `count` | Surveil N (pipeline pattern) |
| `MillEffect` | `count, target` | Mill cards |
| `ShuffleLibraryEffect` | `target` | Shuffle library |
| `EffectPatterns.lookAtTopAndKeep(count, keepCount)` | `count, keepCount, keepDest?, restDest?, revealed?` | Look at top N keep some (pipeline) |
| `EffectPatterns.lookAtTopAndReorder(count)` | `count: Int` or `count: DynamicAmount` | Look at top and reorder (pipeline) |
| `LookAtTopXPutOntoBattlefieldEffect` | `countSource: DynamicAmount, filter, shuffleAfter` | CoCo-style |
| `EffectPatterns.lookAtTargetLibraryAndDiscard(count, toGraveyard)` | `count, toGraveyard` | Look at target's library, discard some (pipeline) |

### Mana
| Effect | Parameters | Purpose |
|--------|------------|---------|
| `AddManaEffect` | `color, amount` | Add colored mana |
| `AddColorlessManaEffect` | `amount` | Add colorless mana |
| `AddAnyColorManaEffect` | `amount` | Add any color mana |
| `AddDynamicManaEffect` | `amountSource: DynamicAmount, allowedColors` | Dynamic mana |

### Tokens
| Effect | Parameters | Purpose |
|--------|------------|---------|
| `CreateTokenEffect` | `count, power, toughness, colors, creatureTypes, keywords, name, imageUri` | Create token |
| `CreateTreasureTokensEffect` | `count` | Create Treasure |
| `CreateTokenFromGraveyardEffect` | `power, toughness, colors, creatureTypes` | Token from graveyard |

### Composite & Control Flow
| Effect | Parameters | Purpose |
|--------|------------|---------|
| `CompositeEffect` | `effects: List<Effect>` | Chain multiple effects |
| `MayEffect` | `effect` | "You may..." |
| `ModalEffect` | `modes: List<Mode>, chooseCount` | "Choose one/two..." |
| `ConditionalEffect` | `condition, effect, elseEffect?` | "If... then... else..." |
| `OptionalCostEffect` | `cost, ifPaid, ifNotPaid?` | "You may [cost]. If you do..." |
| `ReflexiveTriggerEffect` | `action, optional, reflexiveEffect` | "When you do..." |
| `PayOrSufferEffect` | `cost: PayCost, suffer, player` | "Unless [cost], [suffer]" |
| `StoreResultEffect` | `effect, storeAs: EffectVariable` | Store result for later |
| `StoreCountEffect` | `effect, storeAs` | Store count for later |

### Combat
| Effect | Parameters | Purpose |
|--------|------------|---------|
| `MustBeBlockedEffect` | `target` | Must be blocked |
| `TauntEffect` | `target` | Lure |
| `ReflectCombatDamageEffect` | `target` | Reflect combat damage |
| `PreventCombatDamageFromEffect` | `source: GroupFilter, duration` | Fog |
| `PreventDamageFromAttackingCreaturesThisTurnEffect` | (object) | Prevent from attackers |
| `GrantCantBeBlockedExceptByColorEffect` | `filter, canOnlyBeBlockedByColor, duration` | Color evasion |

### Player
| Effect | Parameters | Purpose |
|--------|------------|---------|
| `SkipCombatPhasesEffect` | `target` | Skip combat |
| `SkipUntapEffect` | `target, affectsCreatures, affectsLands` | Skip untap |
| `PlayAdditionalLandsEffect` | `count` | Play extra lands |
| `TakeExtraTurnEffect` | `loseAtEndStep` | Extra turn |

### Stack
| Effect | Parameters | Purpose |
|--------|------------|---------|
| `CounterSpellEffect` | (object) | Counter target spell |
| `CounterSpellWithFilterEffect` | `filter: TargetFilter` | Counter matching spell |

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

### Combined
- `Targets.Any` — creature, player, or planeswalker
- `Targets.CreatureOrPlayer` / `Targets.CreatureOrPlaneswalker`

### Graveyard
- `Targets.CardInGraveyard` / `Targets.CreatureCardInGraveyard`
- `Targets.CreatureCardInYourGraveyard` / `Targets.InstantOrSorceryInGraveyard`

### Spell (on stack)
- `Targets.Spell` / `Targets.CreatureSpell` / `Targets.NoncreatureSpell`
- `Targets.SpellWithManaValueAtMost(manaValue)`

### Composable (Targets.Unified)
- `Targets.Unified.creature` / `.creatureYouControl` / `.otherCreature` etc.
- `Targets.Unified.creature { withColor(Color.RED) }` — builder for custom filters

---

## Triggers Facade

### Zone Changes
- `Triggers.EntersBattlefield` / `Triggers.AnyEntersBattlefield`
- `Triggers.OtherCreatureEnters` / `Triggers.OtherCreatureWithSubtypeDies(subtype)`
- `Triggers.LeavesBattlefield` / `Triggers.Dies` / `Triggers.AnyCreatureDies`
- `Triggers.PutIntoGraveyardFromBattlefield`

### Combat
- `Triggers.Attacks` / `Triggers.AnyAttacks` / `Triggers.YouAttack`
- `Triggers.Blocks` / `Triggers.DealsDamage` / `Triggers.DealsCombatDamage`
- `Triggers.DealsCombatDamageToPlayer`

### Phase/Step
- `Triggers.YourUpkeep` / `Triggers.EachUpkeep`
- `Triggers.YourEndStep` / `Triggers.EachEndStep`
- `Triggers.BeginCombat` / `Triggers.FirstMainPhase`

### Spell
- `Triggers.YouCastSpell` / `Triggers.YouCastCreature`
- `Triggers.YouCastNoncreature` / `Triggers.YouCastInstantOrSorcery`

### Card Drawing
- `Triggers.YouDraw` / `Triggers.AnyPlayerDraws`

### State
- `Triggers.TakesDamage` / `Triggers.BecomesTapped` / `Triggers.BecomesUntapped`
- `Triggers.Transforms` / `Triggers.TransformsToBack` / `Triggers.TransformsToFront`

---

## Filters Facade

### Card Filters (`Filters.*`) — for search/library effects
- `Filters.AnyCard` / `Filters.Creature` / `Filters.Land` / `Filters.BasicLand`
- `Filters.Instant` / `Filters.Sorcery` / `Filters.Permanent` / `Filters.NonlandPermanent`
- `Filters.PlainsCard` / `.IslandCard` / `.SwampCard` / `.MountainCard` / `.ForestCard`
- `Filters.WithSubtype(subtype)` / `Filters.WithColor(color)` / `Filters.ManaValueAtMost(max)`

### Group Filters (`Filters.Group.*`) — for mass effects
- `Filters.Group.allCreatures` / `.creaturesYouControl` / `.creaturesOpponentsControl`
- `Filters.Group.otherCreatures` / `.otherCreaturesYouControl`
- `Filters.Group.attackingCreatures` / `.blockingCreatures` / `.tappedCreatures`
- `Filters.Group.allPermanents` / `.permanentsYouControl` / `.allArtifacts` / `.allEnchantments` / `.allLands`
- `Filters.Group.creatures { withColor(Color.RED) }` — builder for custom filters
- `Filters.Group.permanents { withSubtype("Goblin") }` — builder for custom filters

### Static Targets (`Filters.*`) — for equipment/auras/static abilities
- `Filters.AttachedCreature` / `Filters.EquippedCreature` / `Filters.EnchantedCreature`
- `Filters.Self` / `Filters.Controller` / `Filters.AllControlledCreatures`

### Composable (`Filters.Unified.*`)
- `Filters.Unified.creature` / `.land` / `.artifact` / `.enchantment` / `.instant` / `.sorcery`
- `Filters.Unified.withColor(color)` / `.withSubtype(subtype)` / `.withKeyword(keyword)`
- `Filters.Unified.manaValueAtMost(max)` / `.manaValueAtLeast(min)`

---

## Costs Facade

- `Costs.Tap` / `Costs.Untap`
- `Costs.Mana("2R")` / `Costs.Mana(manaCost)`
- `Costs.PayLife(amount)`
- `Costs.Sacrifice(filter)` / `Costs.DiscardCard` / `Costs.Discard(filter)` / `Costs.DiscardSelf`
- `Costs.ExileFromGraveyard(count, filter)`
- `Costs.Loyalty(change)` — planeswalker loyalty
- `Costs.Composite(cost1, cost2)` — multiple costs

---

## Conditions Facade

### Battlefield
- `Conditions.ControlCreature` / `.ControlEnchantment` / `.ControlArtifact`
- `Conditions.ControlCreaturesAtLeast(count)` / `.ControlCreatureWithKeyword(keyword)`
- `Conditions.ControlCreatureOfType(subtype)`
- `Conditions.OpponentControlsMoreLands` / `.OpponentControlsMoreCreatures` / `.OpponentControlsCreature`

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
- `DynamicAmounts.handSizeDifferenceFromTargetOpponent()`

### Raw DynamicAmount types
- `DynamicAmount.XValue` / `DynamicAmount.Fixed(n)` / `DynamicAmount.YourLifeTotal`
- `DynamicAmount.SacrificedPermanentPower` / `.SacrificedPermanentToughness`
- `DynamicAmount.Count(player, zone, filter)` / `DynamicAmount.AggregateBattlefield(player, filter, aggregation?, property?)`
- Fluent: `DynamicAmounts.battlefield(player, filter).count()` / `.maxManaValue()` / `.maxPower()` / `.sumPower()`
- Math: `DynamicAmount.Add(l, r)` / `.Subtract(l, r)` / `.Multiply(amt, n)` / `.Max(l, r)` / `.Min(l, r)` / `.IfPositive(amt)`

---

## EffectPatterns Facade

**IMPORTANT: Always prefer `EffectPatterns.*` and atomic pipelines over creating new monolithic effects.** This keeps the engine extendible — new cards can reuse existing atomic effects with different parameters instead of requiring new executor code.

### Atomic Library Pipelines

The engine uses a `GatherCards → SelectFromCollection → MoveCollection` pipeline for library manipulation. These atomic effects can be composed for any "look at top N" style ability:

| Pattern | Usage |
|---------|-------|
| `EffectPatterns.lookAtTopAndReorder(count)` | "Look at top N, put back in any order" (e.g., Sage Aven) |
| `EffectPatterns.lookAtTopAndReorder(dynamicAmount)` | Same but with dynamic count (e.g., Information Dealer) |
| `EffectPatterns.lookAtTopAndKeep(count, keepCount)` | "Look at top N, put one into hand, rest on bottom" (e.g., Impulse) |

For custom pipelines (e.g., looking at another player's library), compose directly:
```kotlin
CompositeEffect(listOf(
    GatherCardsEffect(
        source = CardSource.TopOfLibrary(DynamicAmount.Fixed(1), Player.ContextPlayer(0)),
        storeAs = "target_top"
    ),
    MoveCollectionEffect(
        from = "target_top",
        destination = CardDestination.ToZone(Zone.LIBRARY, Player.ContextPlayer(0), ZonePlacement.Top),
        order = CardOrder.ControllerChooses
    )
))
```

### Atomic Building Blocks

| Effect | Purpose |
|--------|---------|
| `GatherCardsEffect(source, storeAs)` | Gather cards from a zone into a named collection |
| `SelectFromCollectionEffect(from, filter, count, storeSelected, storeRest)` | Player selects from a collection |
| `MoveCollectionEffect(from, destination, order)` | Move a collection to a zone |

Sources: `CardSource.TopOfLibrary(count, player)`, `CardSource.FromZone(zone, player)`, `CardSource.FromVariable(name)`
Destinations: `CardDestination.ToZone(zone, player, placement)`
Placements: `ZonePlacement.Top`, `.Bottom`, `.Shuffled`, `.Default`
Ordering: `CardOrder.Preserve` (keep order), `CardOrder.ControllerChooses` (player reorders)

### General Patterns

- `EffectPatterns.mayPay(cost, effect)` — "You may [cost]. If you do, [effect]"
- `EffectPatterns.mayPayOrElse(cost, ifPaid, ifNotPaid)` — with fallback
- `EffectPatterns.sacrifice(filter, count, then)` — sacrifice + effect
- `EffectPatterns.reflexiveTrigger(action, whenYouDo, optional)` — "When you do, [effect]"
- `EffectPatterns.storeEntity(effect, as)` / `storeCount(effect, as)` — variable storage
- `EffectPatterns.sequence(effects...)` — chain effects
- `EffectPatterns.exileUntilLeaves(exileTarget, variableName)` — O-Ring pattern

---

## Keywords (Keyword enum)

### Evasion
`FLYING`, `MENACE`, `INTIMIDATE`, `FEAR`, `SHADOW`, `HORSEMANSHIP`, `CANT_BE_BLOCKED`, `CANT_BE_BLOCKED_BY_MORE_THAN_ONE`

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
`DOESNT_UNTAP`

---

## Parameterized Keyword Abilities (KeywordAbility)

Used via `keywordAbility(...)` or `keywordAbilities(...)` in card DSL:

- `KeywordAbility.Simple(keyword)` — wraps a basic keyword
- **Ward**: `WardMana(cost)`, `WardLife(amount)`, `WardDiscard(count, random)`, `WardSacrifice(filter)`
- **Protection**: `ProtectionFromColor(color)`, `ProtectionFromColors(colors)`, `ProtectionFromCardType(type)`, `ProtectionFromEverything`
- **Combat**: `Annihilator(count)`, `Bushido(count)`, `Rampage(count)`, `Flanking`, `Afflict(count)`
- **Counters**: `Modular(count)`, `Fabricate(count)`, `Renown(count)`, `Tribute(count)`
- **Time**: `Fading(count)`, `Vanishing(count)`
- **Vehicles**: `Crew(power)`
- **Cost**: `Affinity(forType)`, `Cycling(cost)`, `Typecycling(type, cost)`, `Kicker(cost)`, `Multikicker(cost)`
- **Transform**: `Morph(cost)`, `Absorb(count)`

---

## Static Abilities

Set via `staticAbility { ability = ... }`:

- `GrantKeyword(keyword, target: StaticTarget)` — permanent keyword grant
- `GrantKeywordToCreatureGroup(keyword, filter: GroupFilter)` — keyword to group
- `ModifyStats(powerBonus, toughnessBonus, target: StaticTarget)` — P/T bonus
- `GrantDynamicStatsEffect(target, powerBonus: DynamicAmount, toughnessBonus: DynamicAmount)`
- `GlobalEffect(effectType: GlobalEffectType, filter)` — global anthem/debuff
- `CantBlock(target)` / `CantAttackUnlessDefenderControlsLandType(landType, target)`
- `CantBeBlockedByColor(colors, target)` / `CantBeBlockedByPower(minPower, target)`
- `CantBeBlockedExceptByKeyword(requiredKeyword, target)` / `CantBeBlockedByMoreThan(maxBlockers, target)`
- `CanOnlyBlockCreaturesWithKeyword(keyword, target)`
- `CantReceiveCounters(target)` / `AssignDamageEqualToToughness(target, onlyWhenToughnessGreaterThanPower)`
- `SpellCostReduction(reductionSource)` — Affinity/convoke
- `ConditionalStaticAbility(ability, condition)` — conditional static

### GlobalEffectType values
`ALL_CREATURES_GET_PLUS_ONE_PLUS_ONE`, `YOUR_CREATURES_GET_PLUS_ONE_PLUS_ONE`, `OPPONENT_CREATURES_GET_MINUS_ONE_MINUS_ONE`, `ALL_CREATURES_HAVE_FLYING`, `YOUR_CREATURES_HAVE_VIGILANCE`, `YOUR_CREATURES_HAVE_LIFELINK`, `CREATURES_CANT_ATTACK`, `CREATURES_CANT_BLOCK`

### StaticTarget values
`StaticTarget.AttachedCreature`, `SourceCreature`, `Controller`, `AllControlledCreatures`, `SpecificCard(entityId)`

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
- `UndyingEffect(appliesTo)` / `PersistEffect(appliesTo)`

### Damage
- `PreventDamage(amount?, appliesTo)` — Fog, protection
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

---

## Activation Restrictions

Used via `restrictions = listOf(...)` in activated abilities:

- `ActivationRestriction.OnlyDuringYourTurn`
- `ActivationRestriction.BeforeStep(step)` / `DuringPhase(phase)` / `DuringStep(step)`
- `ActivationRestriction.OnlyIfCondition(condition)`
- `ActivationRestriction.All(restrictions...)` — combine multiple

---

## Key File Paths

| File | Purpose |
|------|---------|
| `mtg-sdk/src/main/kotlin/com/wingedsheep/sdk/dsl/` | DSL facades (Effects, Targets, etc.) |
| `mtg-sdk/src/main/kotlin/com/wingedsheep/sdk/scripting/effect/` | Effect type definitions |
| `mtg-sdk/src/main/kotlin/com/wingedsheep/sdk/scripting/KeywordAbility.kt` | Parameterized keyword abilities |
| `mtg-sdk/src/main/kotlin/com/wingedsheep/sdk/scripting/ReplacementEffect.kt` | Replacement effect types |
| `mtg-sdk/src/main/kotlin/com/wingedsheep/sdk/scripting/AdditionalCost.kt` | Additional cost types |
| `mtg-sdk/src/main/kotlin/com/wingedsheep/sdk/scripting/ActivationRestriction.kt` | Activation restrictions |
| `mtg-sdk/src/main/kotlin/com/wingedsheep/sdk/core/Keyword.kt` | Keyword enum |
| `rules-engine/src/main/kotlin/com/wingedsheep/engine/handlers/effects/` | Effect executors (13 categories) |
| `rules-engine/src/main/kotlin/com/wingedsheep/engine/handlers/effects/EffectExecutorRegistry.kt` | Executor registry |
| `mtg-sets/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/{set}/cards/` | Card definitions |
| `mtg-sets/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/{set}/{Set}Set.kt` | Set card lists |
| `game-server/src/test/kotlin/com/wingedsheep/gameserver/scenarios/` | Scenario tests |

### Effect Executor Categories

| Directory | Executors | Covers |
|-----------|-----------|--------|
| `combat/` | 5 | Must-block, taunt, reflect, prevent, evasion |
| `composite/` | 3 | Composite, conditional, may |
| `damage/` | 4 | Single, group, player, divided |
| `drawing/` | 7 | Draw, discard, wheel, each-player |
| `information/` | 2 | Look at hand, reveal |
| `library/` | 8 | Scry, mill, search, reorder, wheel |
| `life/` | 4 | Gain, lose, half, owner-gains |
| `mana/` | 2 | Colored, colorless |
| `permanent/` | 10 | Tap, stats, keywords, counters |
| `player/` | 4 | Skip phases, extra turns, extra lands |
| `removal/` | 5 | Destroy-all, sacrifice, zone-move, can't-regen |
| `stack/` | 2 | Counter spell, counter with filter |
| `token/` | 2 | Create token, create treasure |

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

- `player1` / `player2`: `{ hand?: string[], battlefield?: BattlefieldCardConfig[], graveyard?: string[], library?: string[], lifeTotal?: number }`
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
