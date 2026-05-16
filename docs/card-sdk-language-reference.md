# Card SDK Language Reference

A complete catalog of every building block available to card authors in the Argentum
Engine `mtg-sdk`, with a one-line description for each. Designed to be scanned and
searched. For step-by-step authoring workflow see [`api-guide.md`](api-guide.md) and
[`adding-new-cards-workflow.md`](adding-new-cards-workflow.md); for hard cases see
[`managing-complex-and-rare-abilities.md`](managing-complex-and-rare-abilities.md).

**Maintenance rule:** this document is the canonical SDK catalog. **Every change to the
SDK — new effect, trigger, condition, filter, cost, keyword, dynamic amount, modal
shape, replacement effect, etc. — must update the matching section here in the same
change.** If the entry doesn't fit cleanly in an existing section, add or rename a
section; do not let SDK additions land without a corresponding doc update.

---

## 1. Top-level card DSL

**Entry points**

- `card("Name") { ... }` — open the builder for a standard card.
- `basicLand("Plains" | "Island" | "Swamp" | "Mountain" | "Forest")` — shortcut for basic lands (sets type line,
  intrinsic mana ability, supertype).

**Card builder properties**

- `manaCost: String` — mana cost in `{X}{R}{U}` syntax.
- `typeLine: String` — full type line including supertypes and subtypes.
- `oracleText: String` — rules text; auto-generated from abilities if omitted.
- `power: Int?`, `toughness: Int?` — base P/T for creatures.
- `dynamicPower`, `dynamicToughness` — characteristic-defining P/T (e.g. `*/*` Tarmogoyf).
- `dynamicStats(source, powerOffset?, toughnessOffset?)` — sets both with optional `±` deltas.
- `startingLoyalty: Int?` — starting loyalty for planeswalkers.
- `colorIdentity: String?` — override (normally auto-detected). Treated as authoritative in this repo.
- `auraTarget: TargetRequirement?` — what this Aura enchants.
- `morph: String?` — morph mana cost (cast face-down).
- `morphCost: PayCost?` — non-mana morph cost.
- `morphFaceUpEffect: Effect?` — effect that fires when this morph turns face up.
- `warp: String?` — Warp alt-cost; exiles at end of turn.
- `evoke: String?` — Evoke alt-cost; sacrifices on ETB.
- `selfAlternativeCost: SelfAlternativeCost?` — generic alternative-cost slot.
- `castTimeCreatureTypeChoice: CastTimeCreatureTypeSource?` — forces a creature-type choice at cast time.
- `cantBeCountered: Boolean` — spell is uncounterable.
- `conditionalFlash: Condition?` — gains flash while condition holds.
- `layout: CardLayout` — physical layout shape (see §2).

**Ability blocks inside `card { ... }`**

- `triggeredAbility { ... }` — "when/whenever/at" abilities.
- `staticAbility { ... }` — continuous effects.
- `activatedAbility { ... }` — `cost: effect` abilities.
- `loyaltyAbility(±N) { ... }` — planeswalker loyalty abilities.
- `replacementEffect { ... }` — "instead/if … would" replacement.
- `keywords(...)` / `keywordAbility(...)` / `keywordAbilities(...)` — add keyword abilities.
- `spell { ... }` — define the spell payload for instants/sorceries and Adventure faces.

---

## 2. Card faces, layouts, printings, set metadata

**`CardLayout`**

- `NORMAL` — standard single face (default).
- `SPLIT` — two or more halves on one card; combined characteristics apply off-battlefield (CR 709.4c). Used for Rooms,
  Fuse, Aftermath.
- `ADVENTURE` — primary face is a creature, `cardFaces[0]` is an instant/sorcery Adventure (CR 715). Resolving the
  Adventure exiles the card and grants permission to cast the creature from exile.

**`CardFace` (SPLIT / ADVENTURE)**

- `name` — face name.
- `manaCost` — face mana cost.
- `typeLine` — face type line.
- `script { ... }` — that face's abilities.
- `keywords` — face-local keywords.

**`metadata { ... }`**

- `rarity: Rarity` — `COMMON | UNCOMMON | RARE | MYTHIC | SPECIAL | BONUS`.
- `collectorNumber: String` — Scryfall collector number.
- `artist: String` — illustrator credit.
- `flavorText: String` — italicized flavor.
- `imageUri: String?` — art URL; auto-fetched from Scryfall if omitted.
- `scryfallId: String?` — Scryfall UUID.
- `releaseDate: String?` — `YYYY-MM-DD`.
- `inBooster: Boolean` — appears in draft boosters (default `true`; `false` for Special Guests / starter exclusives).
- `oracleTextOverride: String?` — bypass auto-generated oracle text.

**Reprints** — add a `Printing` row in the new set's `Reprints.kt` and wire it into `MtgSet.printings`. Never duplicate
the `CardDefinition`.

---

## 3. Costs (`Costs.*`)

- `Costs.Free` — costs nothing (`{0}`).
- `Costs.Tap` — `{T}`; tap this permanent.
- `Costs.Untap` — `{Q}`; untap this permanent.
- `Costs.Mana("{2}{U}")` — pay the given mana cost (string or `ManaCost`).
- `Costs.PayLife(amount)` — pay N life.
- `Costs.Sacrifice(filter)` — sacrifice a permanent matching the filter (may include self).
- `Costs.SacrificeAnother(filter)` — sacrifice a *different* permanent matching the filter.
- `Costs.Composite(c1, c2, ...)` — multiple costs paid together.

**Spell-level alternatives**

- `selfAlternativeCost` — generic "cast instead for" alt-cost.
- `evoke` — pay evoke cost; creature is sacrificed at ETB.
- `morph` — cast face-down for `{3}`-ish.
- `warp` — cast from anywhere; exiled at end of turn.
- `conditionalFlash` — flash while condition holds.
- `cantBeCountered` — spell is uncounterable.

**`AdditionalCost`** — extra costs paid alongside the mana cost.

- `AdditionalCost.BlightVariable` — "as you cast, you may pay X life" (Blight X); X exposed via
  `DynamicAmount.AdditionalCostBlightAmount`.

---

## 4. Effects (`Effects.*`)

Atomic effect factories. For library/zone manipulation, prefer the pipelines in §5.

### Damage

- `DealDamage(amount, target)` — deal fixed/dynamic damage.
- `DealXDamage(target)` — deal X damage (spell's X).
- `Fight(target1, target2)` — two creatures each deal damage equal to their power to each other (CR 701.12).

### Life

- `GainLife(amount, target?)` — target gains life (default: controller).
- `LoseLife(amount, target)` — target loses life.
- `SetLifeTotal(amount, target)` — set target's life total to N.
- `ExchangeLifeAndPower(target)` — swap target's power with controller's life total.
- `LoseHalfLife(roundUp, target, lifePlayer?)` — lose half of life total (round up/down).
- `LoseGame(target, message?)` — target loses the game.
- `WinGame(target, message?)` — target wins the game.
- `ForceExileMultiZone(count, target)` — exile from hand/battlefield/graveyard combined (Lich's Mastery shape).

### Cards (draw / discard)

- `DrawCards(count, target?)` — draw N (default: controller).
- `DrawUpTo(max, target)` — draw up to N (player picks 0–N).
- `Discard(count, target)` — controller-of-target chooses; mandatory.
- `EachOpponentDiscards(count)` — each opponent discards N.
- `EachPlayerReturnPermanentToHand()` — each player bounces a permanent.
- `EachPlayerDrawsForDamageDealtToSource()` — each player draws equal to damage source took this turn.
- `ReadTheRunes()` — draw N, then discard N (or sacrifice permanents).
- `ReplaceNextDraw(effect)` — replaces controller's next draw with the given effect.

### Destruction & exile

- `Destroy(target)` — destroy target (respects indestructible).
- `DestroyAll(filter, noRegenerate?, storeDestroyedAs?)` — destroy all matching; optionally save the ID list for
  follow-up.
- `DestroyAllAndAttached(filter, noRegenerate?)` — also destroys auras/equipment on the matching permanents.
- `DestroyAllEquipmentOnTarget(target)` — wreck the gear attached to a creature.
- `Exile(target)` — exile target.
- `ExileAndGrantOwnerPlayPermission(target, until?)` — exile + owner may play it (Garth-style).
- `ExileOpponentsGraveyards()` — exile every card in each opponent's graveyard.
- `ExileUntilLeaves(target)` — linked exile; returns when source leaves the battlefield.
- `ExileGroupAndLink(filter, storeAs?)` — exile all matching permanents into source's linked exile pile.
- `ExileFromTopRepeating(count, repeatCondition)` — keep exiling top cards while a condition holds.
- `ExileLibraryUntilManaValue(manaValue)` — exile from library until mana value ≤ N.

### Return / placement

- `ReturnToHand(target)` — bounce to hand.
- `PutOnTopOfLibrary(target)` — place target on top of its owner's library.
- `PutOnTopOrBottomOfLibrary(target)` — player chooses top or bottom.
- `PutSecondFromTopOrBottomOfLibrary(target)` — second-from-top or bottom.
- `ShuffleIntoLibrary(target)` — shuffle target into owner's library.
- `PutIntoLibraryNthFromTop(target, positionFromTop)` — place N from the top.
- `PutOntoBattlefield(target, tapped?)` — put target on the battlefield.
- `PutOntoBattlefieldUnderYourControl(target)` — under controller's control.
- `PutOntoBattlefieldFaceDown(count, target?)` — enter face-down (2/2 morph shape).
- `ReturnSelfToBattlefieldAttached(target)` — return source attached to target (Aura recursion).
- `ReturnCreaturesPutInGraveyardThisTurn(player)` — Patriarch's Bidding shape.

### Linked exile & play-from-exile permissions

- `ReturnLinkedExile()` — return all from source's linked exile, under controller.
- `ReturnLinkedExileUnderOwnersControl()` — return under each card's owner.
- `ReturnLinkedExileToHand()` — return all from linked exile to hand.
- `ReturnOneFromLinkedExile()` — return one chosen card.
- `GrantMayPlayFromExile(from, restriction?)` — owner may play matching cards from exile.
- `GrantPlayWithoutPayingCost(from)` — same, without paying mana costs.
- `GrantFreeCastTargetFromExile(target)` — cast specific exiled card for free.

### Stats & keywords

- `ModifyStats(power, toughness, target?)` — `±P/±T` until end of turn (default scope).
- `GrantKeyword(keyword, target, duration)` — grant a keyword for a duration.
- `RemoveKeyword(keyword, target, duration)` — strip a keyword.
- `RemoveAllAbilities(target, duration)` — wipe all abilities (including granted keywords).
- `LoseAllCreatureTypes(target, duration)` — remove all creature subtypes.
- `SetCreatureSubtypes(subtypes, target, duration)` — replace subtypes outright.
- `AddCreatureType(subtype, target, duration)` — additive subtype.
- `GrantHexproof(target, duration)` — temporary hexproof.
- `GrantExileOnLeave(target)` — "if it would leave, exile instead".
- `GrantKeywordToAttackersBlockedBy(keyword, target)` — grant keyword to creatures this blocks.

### Counters

- `AddCounters(type, count, target)` — add N counters of `type`.
- `AddDynamicCounters(type, amount, target)` — count is computed at resolution.
- `RemoveCounters(type, count, target)` — remove N counters.
- `RemoveAnyNumberOfCounters(target)` — player removes 0 or more.
- `RemoveAllCounters(target)` — wipe every counter.
- `RemoveAllCountersOfType(type, target)` — wipe one kind.
- `MoveAllLastKnownCounters(target)` — Hooded Hydra / Essence Channeler — move every counter kind from source's
  last-known state.
- `DistributeCountersFromSelf(type?, count?)` — split source's counters among creatures you control.
- `DistributeCountersAmongTargets(total, type?, minPerTarget?)` — divvy N counters among chosen targets.
- `Proliferate()` — add one counter of each kind already present on chosen permanents/players (CR 701.27).
- `AddCountersToCollection(name, type, count)` — add counters to cards held in a pipeline collection.

### Color & type

- `AddCardType(type, target, duration)` — add a card type (e.g. become an artifact).
- `AddSubtype(subtype, target, duration)` — add a subtype temporarily.
- `ChooseColorForTarget(target)` — target picks a color; stored in context.
- `BecomeChosenManaColor(target)` — adopt the previously chosen color.
- `ChangeColor(colors, target, duration)` — replace colors with the given set.
- `BecomeAllColors(target, duration)` — five-color until end of turn.

### Mana

- `AddMana(color, amount, restriction?)` — add N of one color.
- `AddColorlessMana(amount, restriction?)` — add colorless.
- `AddAnyColorMana(amount?, restriction?)` — add any-color mana; color chosen when spent.
- `AddAnyColorManaSpendOnChosenType(typeName)` — mana that can only pay for a specific card type.
- `AddDynamicMana(amount, allowedColors, restriction?)` — N mana within a color set.
- `AddManaInAnyCombination(colors, amount)` — split N across colors.
- `AddManaOfChosenColor(amount?)` — adds mana matching a chosen color.
- `AddManaOfColorAmong(filter)` — one mana of each color among matching permanents.
- `AddOneManaOfEachColorAmong(filter)` — same shape, explicit naming.
- `AddManaOfColorLandsCouldProduce(filter?)` — Chromatic Lantern shape.
- `AddManaOfColorInCommanderColorIdentity()` — mana of any colors in your commander's identity.

### Tokens & emblems

- `CreateToken(name, p, t, colors?, subtypes?, keywords?, count?, tapped?)` — make N tokens.
- `CreateDynamicToken(...)` — tokens whose P/T is computed.
- `CreateTokenCopyOfSelf(count?, tapped?)` — token copies of source.
- `CreateTokenCopyOfTarget(target, count?, tapped?)` — token copy of another permanent.
- `CreateTokenCopyOfEquippedCreature(count?, tapped?)` — equipment-specific copy.
- `CreateTreasure(count?, tapped?)` — Treasure tokens.
- `CreateFood(count?, controller?)` — Food tokens.
- `CreateLander(count?, controller?)` — Lander land tokens.
- `CreateMutavault(count?, tapped?, controller?)` — Mutavault tokens.
- `CreateRoleToken(roleName, target)` — attach a Role aura token.
- `CreateMapToken(count?)` — Map artifact tokens.
- `CreateDroneToken(count?)` — Drone tokens.
- `CreatePermanentEmblem(name, abilities)` — planeswalker emblem with static abilities.

### Ability granting

- `GrantTriggeredAbilityEffect(ability)` — permanently grant a triggered ability.
- `CreatePermanentGlobalTriggeredAbility(ability)` — engine-wide triggered ability with no source.
- `CreateGlobalTriggeredAbilityWithDuration(ability, duration)` — same, but bounded.
- `GrantSpellKeywordEffect` — grant a keyword to a spell on the stack.
- `GrantSpellsCantBeCountered(target, filter, duration)` — target's matching spells become uncounterable (Domri shape).

### Control & combat

- `GainControlEffect(target, duration)` — gain control until end of turn (default).
- `ExchangeControlEffect(target1, target2)` — swap control of two permanents.
- `GainControlByMostOfSubtypeEffect(subtype)` — whoever controls the most of a tribe takes it.
- `GiftGivenEffect(target)` — "gift" temporary control.
- `CantAttackEffect(target, unless?)` — target can't attack.
- `CantBlockEffect(target, unless?)` — target can't block.
- `CantAttackGroupEffect(filter, condition?)` — group-scoped can't-attack.
- `CantBlockGroupEffect(filter, condition?)` — group-scoped can't-block.
- `SuspectEffect(target)` — target becomes Suspected (MKM keyword).
- `RemoveFromCombatEffect(target)` — yank target out of combat.
- `SkipNextTurnEffect(target)` — target skips their next turn.
- `HijackNextTurnEffect(target)` — you control target's next turn.
- `GrantCantBeBlockedByChosenColorEffect(target, duration)` — unblockable except by chosen color.
- `CantCastSpellsEffect(target, until?)` — target can't cast spells.

### Forced sacrifice / discard

- `SacrificeTargetEffect(target)` — target sacrifices itself.
- `ForceSacrificeEffect(target, count)` — edict; target sacrifices N creatures.
- `ForceReturnOwnPermanentEffect(target)` — target bounces one of their own.

### Stack manipulation

- `CounterEffect(target, condition?, destination?)` — counter a spell/ability; optionally send elsewhere.
- `CounterAllOnStackEffect(filter?, destination?)` — counter everything matching.
- `CopyTargetSpellEffect(target)` — copy a spell on the stack.
- `CopyTargetTriggeredAbilityEffect(target)` — copy a triggered ability on the stack.
- `CopyNextSpellCastEffect` — copy the next spell its controller casts.
- `CopyEachSpellCastEffect` — copy every spell cast this turn.
- `ChangeTargetEffect(spell, newTarget)` — change a spell's target.
- `ChangeSpellTargetEffect(spell, filter)` — same, filtered.
- `ReselectTargetRandomlyEffect(spell)` — re-choose targets at random.
- `ReturnSpellToOwnersHandEffect(spell)` — return a spell from the stack to hand.

### Combat-shape & misc

- `PreventDamageEffect(amount, direction, scope, source?, recipient?)` — prevention shield.
- `BecomeCreatureEffect(target, p, t, subtypes, keywords, duration)` — animate non-creature (lands, artifacts).
- `EachPermanentBecomesCopyOfTargetEffect(filter, target)` — Cytoshape-style mass copy.
- `AnimateLandEffect(target, subtypes, keywords, duration)` — land becomes a creature.
- `ExploreEffect(target)` — Explore mechanic (reveal top; land → battlefield, else hand + counter).
- `AttachEquipmentEffect(equip, target)` — attach an Equipment.
- `TapUntapEffect(target, isTap)` — tap or untap.
- `MarkExileOnDeathEffect(target)` — replace next "to graveyard" with "to exile".
- `OptionalCostEffect(cost, effect)` — pay cost to trigger an effect.
- `StoreResultEffect(effect, as)` — stash an effect's result for later reference.
- `StoreCountEffect(effect, as)` — stash a count for later reference.
- `RepeatWhileEffect(condition, effect, maxIterations?)` — run effect repeatedly while condition holds.

### Sequencing & conditional

- `CompositeEffect(effects)` / `Composite(e1, e2, ...)` — run effects in order.
- `ConditionalEffect(condition, ifTrue, ifFalse?)` / `Branch(...)` — conditional branch.
- `IfYouDoEffect(action, reflexive, optional)` — if optional action is taken, run reflexive effect.
- `ReflexiveTriggerEffect(action, reflexive, optional)` — same shape but the reflexive effect goes on the stack.

### Modal & choice

- `ModalEffect.chooseOne { mode(...) }` / `ModalEffect.chooseN(n) { ... }` — modal effect block.
- `ChooseActionEffect(choices)` — player picks from a list of effects.
- `ChooseColorAndGrantProtectionToTargetEffect(target)` — pick a color, grant protection to target.
- `ChooseColorAndGrantProtectionToGroupEffect(filter)` — same, for a group.
- `ChooseColorThenEffect(whenChosen)` — pick a color, then run a function of that color.
- `GrantHexproofFromChosenColorEffect(target)` — hexproof from chosen color.
- `ChooseCreatureTypeEffect(...)` — pause for creature-type pick.
- `SelectTargetEffect(...)` — have a player pick from a valid set.
- `SeparatePermanentsIntoPilesEffect(filter, piles)` — divvy into piles (Fact-or-Fiction shape).

> **Authoring rule:** prefer composing primitives over adding parameters to an existing effect. Use `CompositeEffect`
> and the gather/select/move pipeline before writing a new executor.

---

## 5. Effect patterns (`EffectPatterns.*`)

Composed pipelines (`GatherCards → SelectFromCollection → MoveCollection` shapes and similar).

**Library search & reveal**

- `searchLibrary(filter, destination?, tapped?, shuffle?)` — search library, pick matching, move, shuffle.
- `searchLibraryNthFromTop(filter, n, destination)` — search only the top N cards.
- `searchMultipleZones(filters, ...)` — search multiple zones in one effect.
- `searchTargetLibraryExile(count?, filter?)` — exile from target's library.
- `lookAtTargetLibraryAndDiscard(count, toGraveyard?)` — peek at top N and discard.

**Top-deck manipulation**

- `scry(count)` — look at top N, bottom any, rest on top.
- `surveil(count)` — look at top N, any to graveyard, rest on top.
- `mill(count)` — top N cards into graveyard.
- `lookAtTopAndKeep(count, keepCount)` — Ancestral Memories — keep exactly K to hand.
- `lookAtTopAndReorder(count)` — reorder top N.
- `lookAtTopXAndPutOntoBattlefield(...)` — look at top N, put any onto battlefield.

**Reveal patterns**

- `revealUntilNonlandDealDamage(target)` — Bonecrusher Giant shape.
- `revealUntilNonlandModifyStats()` — Erratic Explosion shape.
- `revealUntilCreatureTypeToBattlefield()` — Riptide Shapeshifter shape.
- `revealAndOpponentChooses(count, filter)` — Animal Magnetism shape.
- `wheelEffect(players)` — each player shuffles hand into library, draws that many.
- `factOrFiction(...)` — reveal 5, opponent splits into two piles, you choose one.

**Hand manipulation**

- `discardCards(count, target)` — controller-of-target chooses (mandatory).
- `discardRandom(count, target)` — random discards.
- `discardHand(target)` — discard entire hand.
- `eachOpponentDiscards(count, controllerDrawsPerDiscard?)` — Mind Twist-style.
- `eachPlayerDiscardsDraws(controllerBonusDraw?)` — Windfall / Wheel of Fortune.
- `eachPlayerDrawsX(includeController?, includeOpponents?)` — Howling Mine shape.
- `eachPlayerMayDraw(maxCards, lifePerCardNotDrawn?)` — optional group draw with a tax.
- `headGames(target)` — Cranial Extraction — view hand, set up top of library.
- `exileFromHand(count?, target)` — exile N from hand.

**Sacrifice / destroy**

- `sacrifice(filter, count, then)` — sacrifice N, then run effect.
- `sacrificeFor(filter, countName, thenEffect)` — sacrifice variable count, store, then effect.
- `destroyAllPipeline(filter, noRegenerate?, storeDestroyedAs?)` — wrath pipeline with storage.
- `destroyAllAndAttachedPipeline(filter, noRegenerate?)` — wrath + attached.
- `destroyAllSharingTypeWithSacrificed(noRegenerate?)` — destroy all creatures sharing type with a sacrificed creature.

**Creature-type choice**

- `chooseCreatureTypeRevealTop()` — pick a type, reveal until matching.
- `chooseCreatureTypeReturnFromGraveyard(count)` — pick a type, return N from graveyard.
- `chooseCreatureTypeShuffleGraveyardIntoLibrary()` — pick a type, shuffle matching into library.
- `chooseCreatureTypeModifyStats(...)` — pick a type, buff matching.
- `chooseCreatureTypeUntap()` — pick a type, untap your matching.
- `chooseCreatureTypeGainControl(duration?)` — pick a type, control matching.
- `chooseCreatureTypeMustAttack()` — pick a type, matching must attack.
- `becomeChosenTypeAllCreatures(...)` — all creatures become the chosen type.
- `patriarchsBidding()` — return creatures, types named in graveyards.
- `destroyAllExceptStoredSubtypes(...)` — wrath sparing stored subtypes.

**Misc mechanic shapes**

- `mayPay(cost, effect)` — optionally pay cost to trigger an effect.
- `mayPayOrElse(cost, ifPaid, ifNotPaid)` — pay-or-else fork.
- `blight(amount, player?)` — Blight X additional cost glue.
- `forage(afterEffect?)` — Forage cost; choose card-from-hand to play.
- `loot(draw?, discard?)` — "draw N, discard M" loop.
- `rummage(count?)` — discard then draw.
- `drain(amount, target)` — deal N damage, gain N life.
- `eachOpponentMayPutFromHand(filter?)` — each opponent may dump a matching card.
- `putFromHand(filter?, count?, entersTapped?)` — you may put N from hand onto battlefield.
- `incubate(n)` — make an Incubator token with N counters.
- `returnLinkedExile(underOwnersControl?)` — bring back linked exile pile.
- `takeFromLinkedExile()` — pull one card from linked exile.
- `shuffleGraveyardIntoLibrary(target?)` — Elixir of Immortality shape.
- `reflexiveTrigger(action, whenYouDo, optional?)` — optional action; if taken, queue a reflexive trigger.

---

## 6. Targets

### Resolution-time (`EffectTarget`)

- `EffectTarget.ContextTarget(i)` — i-th cast-time target.
- `EffectTarget.Controller` — controller of the source ability.
- `EffectTarget.Self` — the source permanent.
- `EffectTarget.TriggeringEntity` — the entity that caused the trigger to fire.
- `EffectTarget.PlayerRef(...)` — a player slot: `You`, `Each`, `Opponent`, etc.
- `EffectTarget.ContextProperty(key)` — value plumbed into `EffectContext` (damage amount, life gained, blight
  amount, …).
- `EnchantedCreature` / `EquippedCreature` — resolve via `AttachedToComponent`; requires the state-aware
  `resolveTarget(state, target)` overload.

### Cast-time (`Targets.*` / `TargetRequirement`)

- `Targets.Any` — any creature, player, or planeswalker.
- `Targets.Creature` — any creature.
- `Targets.Player` — any player.
- `Targets.Planeswalker` — any planeswalker.
- `Targets.Permanent` — any permanent.
- `Targets.NonlandPermanent` — any nonland permanent.
- `Targets.Artifact` — any artifact.
- `Targets.Enchantment` — any enchantment.
- `Targets.Land` — any land.
- `Targets.BasicLand` — any basic land.
- `Targets.Spell` — any spell on the stack.
- `Targets.Card` — any card in any zone (e.g. graveyard).
- `Targets.CreatureOrPlaneswalker` — combined.
- `Targets.TappedCreature` / `UntappedCreature` — state-restricted.
- `Targets.InstantOrSorcery` — instant-or-sorcery card.

**Chained predicates** — `.youControl()`, `.controlledByOpponent()`, `.opponent()`, `.withSubtype(...)`,
`.withKeyword(...)`, `.ofColor(...)`, `.tapped()`, `.untapped()`, `.power(n)`, `.minPower(n)`, `.maxPower(n)`; plus
`TargetFilter.excludeSelf` to exclude the source.

### Named multi-target binding

```kotlin
spell {
    val creature = target("creature", Targets.Creature)
    val player = target("player", Targets.Player)
    effect = Effects.Composite(
        Effects.Destroy(creature),
        Effects.DealDamage(3, player),
    )
}
```

For modal spells, prefer the explicit `targetPlayerControls(target)` DSL form; per-mode targets route via
`modeTargetsOrdered`.

---

## 7. Filters & predicates

### `GameObjectFilter` — for searches, sacrifice, group effects

- `Filters.AnyCard` — any card.
- `Filters.Creature` — any creature card.
- `Filters.Land` — any land card.
- `Filters.BasicLand` — any basic land.
- `Filters.PlainsCard` / `IslandCard` / `SwampCard` / `MountainCard` / `ForestCard` — specific basics.
- `Filters.Instant` — instant card.
- `Filters.Sorcery` — sorcery card.
- `Filters.Permanent` — permanent card.
- `Filters.NonlandPermanent` — nonland permanent.
- `Filters.WithSubtype(subtype)` — card of a given subtype.

**Chained predicates**

- `.youControl()` / `.controlledByOpponent()` — control predicate.
- `.withSubtype(s)` / `.withKeyword(k)` — type/ability predicate.
- `.ofColor(c)` / `.ofColors(set)` — color predicate.
- `.power(n)` / `.minPower(n)` / `.maxPower(n)` — P/T comparator.
- `.tapped()` / `.untapped()` — tap state.
- `.nontoken()` / `.token()` — token vs printed.
- `.faceDown()` — face-down state.
- `.card(filter)` — defer to a card-shape filter for off-battlefield checks.

**Explicit constructor**:
`GameObjectFilter(cardPredicates, controllerPredicate, colorPredicate, keywordPredicate, powerToughnessPredicate, subtypePredicate)`.

### `GroupFilter` — static-ability scope

- `GroupFilter.CreaturesYouControl` — your creatures.
- `GroupFilter.CreaturesOpponentControls` — their creatures.
- `GroupFilter.AllCreatures` — every creature on the battlefield.
- `GroupFilter.All(filter)` — custom group.
- Chained: `.withColor`, `.withoutColor`, `.withKeyword`, `.withoutKeyword`, `.withSubtype`, `.withoutSubtype`,
  `.minPower`, `.maxPower`, `.power`.

### `StatePredicate` — battlefield state checks

- `IsTapped` — currently tapped.
- `IsUntapped` — currently untapped.
- `IsAttacking` — declared as attacker this combat.
- `IsBlocking` — declared as blocker this combat.
- `IsFaceDown` — currently face-down.
- `HasCounter(type)` — has at least one counter of `type`.

### `AffectsFilter` — static-ability target shapes

- `OtherCreaturesWithSubtype` — lord scope (other creatures of subtype).
- `CreaturesWithCounter` — creatures with at least one counter (Aurification).

> **Load-bearing rule:** filtering battlefield permanents by type/subtype/color/keyword/P-T MUST use
`predicateEvaluator.matchesWithProjection(state, projected, ...)`. Use `projected.isCreature(entityId)` rather than
`cardComponent.typeLine.isCreature`. Non-battlefield zones may read base state.

---

## 8. Triggered abilities (`Triggers.*`)

`triggeredAbility { trigger; effect; target?; triggerCondition?; optional?; checkOnNextState?; dealsDamageBeforeResolve?; controlledByTriggeringEntityController? }`.

### Zone change

- `EntersBattlefield` — when source ETB.
- `AnyEntersBattlefield` — when any permanent ETB.
- `LeavesBattlefield` — when source LTB.
- `Dies` — when source dies (battlefield → graveyard, creature only).
- `AnyCreatureDies` — when any creature dies.
- `YourCreatureDies` — when a creature you control dies.
- `YourCreatureLeavesBattlefield` — broader than dies (any zone).
- `PutIntoGraveyardFromBattlefield` — non-creature death equivalent.
- `LandYouControlEnters` — landfall.
- `OtherCreatureEnters` — when another creature you control ETB.
- `OtherPermanentYouControlEnters` — same but any permanent.
- `FaceDownCreatureEnters` — when a face-down creature ETB (any controller).
- `AnyEnchantmentYouControlEnters` — when an enchantment you control ETB.
- `OtherCreatureWithSubtypeDies(subtype)` — tribe-specific death trigger.

### Combat

- `Attacks` — when source attacks.
- `AttacksAlone` — when source is the only attacker.
- `AnyAttacks` — when any creature attacks.
- `CreatureYouControlAttacks` — your creature attacks.
- `NontokenCreatureYouControlAttacks` — same, nontoken.
- `YouAttack` — when you declare attackers (player-level).
- `YouAttackWithFilter(filter)` — when you attack with ≥1 matching attacker.
- `Blocks` — when source blocks.
- `CreatureYouControlBlocks` — your creature blocks.
- `BecomesBlocked` — when source becomes blocked.
- `CreatureYouControlBecomesBlocked` — your creature becomes blocked.
- `FilteredBecomesBlocked(filter)` — matching creature becomes blocked.
- `BlocksOrBecomesBlockedBy(filter)` — either direction, filtered.

### Damage

- `DealsDamage` — source deals any damage.
- `DealsCombatDamage` — source deals combat damage.
- `DealsCombatDamageToPlayer` — combat damage to a player.
- `DealsCombatDamageToPlayerOrPlaneswalker` — either.
- `DealsCombatDamageToCreature` — combat damage to a creature.
- `CreatureYouControlDealsCombatDamageToPlayer` — your creature CD's a player.
- `NontokenCreatureYouControlDealsCombatDamageToPlayer` — same, nontoken.
- `CreatureDealtDamageByThisDies` — Etali / Sengir shape.
- `TakesDamage` — source takes any damage.
- `DamagedByCreature` — source takes damage from a creature.
- `DamagedBySpell` — source takes damage from a spell.
- `EnchantedCreatureTakesDamage` — Aurification shape.

### Phase & turn

- `YourUpkeep` — start of your upkeep.
- `YourDrawStep` — start of your draw step.
- `EachUpkeep` — every upkeep.
- `EachOpponentUpkeep` — at each opponent's upkeep.
- `YourEndStep` — beginning of your end step.
- `EachEndStep` — beginning of each end step.
- `BeginCombat` — start of combat.
- `FirstMainPhase` — start of pre-combat main.
- `YourPostcombatMain` — start of post-combat main.

### Aura / equipment

- `EnchantedCreatureControllerUpkeep` — at upkeep of enchanted creature's controller.
- `EnchantedCreatureControllerEndStep` — at end step of same.
- `EnchantedCreatureDies` — when enchanted creature dies.
- `EnchantedCreatureAttacks` — when enchanted creature attacks.
- `EnchantedCreatureDealsCombatDamageToPlayer` — Aura damage trigger.
- `EnchantedCreatureTurnedFaceUp` — Aura sees a morph flip.
- `EnchantedCreatureDealsDamage` — any damage by enchanted creature.
- `EnchantedPermanentLeavesBattlefield` — enchanted permanent LTB.
- `EnchantedPermanentBecomesTapped` — Curse-on-tap shape.
- `EquippedCreatureAttacks` — equipped creature attacks.
- `EquippedCreatureDies` — equipped creature dies.

### Cards & draws

- `YouDraw` — when you draw a card.
- `AnyPlayerDraws` — when anyone draws.
- `RevealCreatureFromDraw` — Hatching Plans-style top-card reveal.
- `RevealCardFromDraw` — generic reveal-from-draw trigger.
- `CardsPutIntoYourGraveyard(filter?)` — when matching cards enter your yard.
- `PermanentCardsPutIntoYourGraveyard` — only permanent cards.
- `CreaturesPutIntoGraveyardFromLibrary` — mill-trigger shape.

### Spell casting

- `YouCastSpell` — any spell you cast.
- `YouCastCreature` — any creature spell you cast.
- `YouCastNoncreature` — non-creature spells you cast.
- `YouCastNoncreatureOrSubtype(subtype)` — noncreature OR subtype creature.
- `YouCastInstantOrSorcery` — instant/sorcery you cast.
- `YouCastInstantOrSorceryFromHand` — same, must be from hand.
- `YouCastEnchantment` — any enchantment you cast.
- `YouCastEnchantmentFromHand` — same, from hand.
- `YouCastHistoric` — artifact / legendary / Saga.
- `YouCastSubtype(subtype)` — creature with matching subtype.
- `YouCastKickedSpell` — kicked or supercast spell.
- `YouCastSpellPaidWithTreasureMana` — Treasure-fueled cast.
- `AnySpellOrAbilityOnStack` — any object hits the stack.

### State change & misc

- `TurnedFaceUp` — source turns face up.
- `CreatureTurnedFaceUp(player?)` — when a creature you control turns face up.
- `GainControlOfSelf` — you gain control of source.
- `BecomesTarget(filter?)` — source becomes target of spell/ability.
- `CreatureYouControlBecomesTargetByOpponent(filter?)` — your creature gets targeted by opponent.
- `Transforms` — source transforms (either direction).
- `TransformsToFront` — to front face.
- `TransformsToBack` — to back face.
- `YouCycle` — you cycle any card.
- `YouCycleThis` — you cycle source.
- `AnyPlayerCycles` — anyone cycles.
- `YouCommitCrime` — MKM crime mechanic.
- `YouGiveAGift` — Gift mechanic.
- `Valiant` — Bloomburrow Valiant trigger.
- `RoomFullyUnlocked` — Rooms — both doors unlocked.
- `OnDoorUnlocked` — single Room door unlocked.

### Life

- `YouGainLife` — you gain any life.
- `AnyPlayerGainsLife` — anyone gains life.
- `YouLoseLife` — you lose any life.
- `AnyPlayerLosesLife` — anyone loses life.
- `YouGainOrLoseLife` — combined life-change.

### Sacrifice & counters

- `YouSacrificeOneOrMore(filter?)` — you sac ≥1 matching.
- `Sacrificed` — source is sacrificed.
- `PlusOneCountersPlacedOnYourCreature` — Hardened Scales shape.
- `OneOrMorePermanentsEnter(filter?)` — batched ETB trigger.
- `OneOrMoreLeaveWithoutDying(...)` — batched LTB-without-dying.

### Conditional

- `NthSpellCast(n, player?)` — fires on the Nth spell cast.
- `Expend(threshold)` — Expend N (CLB mechanic).

### Delayed & granted triggers

- `DelayedTriggeredAbility` — registered now, fires at a specific future step (Astral Slide).
- `Effects.GrantTriggeredAbilityEffect` — grant a triggered ability for a duration; `GrantTriggeredAbilityExecutor` uses
  projected state and supports leaves-battlefield-to-zone triggers.

---

## 9. Static abilities

```kotlin
staticAbility {
    ability = Modification.GrantKeyword(Keyword.FLYING)
    filter = GroupFilter.CreaturesYouControl.withSubtype("Soldier")
    duration = Duration.Permanent
    layer = Layer.PT_POWER_TOUGHNESS    // optional; usually inferred
    condition = Conditions.YouControl(Filters.Swamp)
}
```

**`Modification` options**

- `AddSubtype(subtype)` — add a subtype to matching creatures.
- `RemoveSubtype(subtype)` — strip a subtype.
- `ReplaceSubtypes(subtypes)` — set the subtype list outright.
- `ModifyStats(p, t)` — `±P/±T`.
- `SetPower(p)` — overwrite power.
- `SetToughness(t)` — overwrite toughness.
- `SetStats(p, t)` — overwrite both.
- `GrantKeyword(keyword)` — grant a keyword.
- `RemoveKeyword(keyword)` — remove a keyword.
- `GrantProtection(color)` — grant protection from a color.
- `Custom(...)` — escape hatch for one-off modifications.

**Composite static abilities**

- `ModifyStatsForCreatureGroup` — lord-style P/T booster targeting a group.
- `GrantKeywordByCounter` — Aurification — keyword based on counters present.
- `AddCreatureTypeByCounter` — subtype based on counters present.
- `ConditionalStaticAbility` — static gated by a runtime `Condition`.
- `Effects.CreatePermanentEmblem(...)` — emblem with static abilities (planeswalker ultimates).

> Multiple lord effects on one card → multiple `staticAbility { }` blocks.

---

## 10. Activated abilities

```kotlin
activatedAbility {
    cost = Costs.Tap
    effect = Effects.DrawCards(1)
    target = Targets.Creature
    optional = false
    timing = TimingRule.Normal
    isManaAbility = false
    restriction = ActivationRestriction.MaxPerTurn(1)
}
```

**`TimingRule`**

- `Normal` — at instant speed (default for most abilities).
- `ManaAbility` — resolves immediately, doesn't use the stack (CR 605).
- `SorcerySpeed` — only during your main phase, empty stack.
- `OnlyIfCondition(c)` — guarded by a runtime condition.

**`ActivationRestriction`**

- `MaxPerTurn(n)` — at most N activations per turn.
- `OnlyOnce` — once per game.
- `OnlyIfCondition(c)` — condition gate.

**Loyalty abilities**

- `loyaltyAbility(+N) { ... }` — add loyalty + effect.
- `loyaltyAbility(-N) { ... }` — remove loyalty + effect.
- `loyaltyAbility(0) { ... }` — 0-loyalty ability.

---

## 11. Keywords

**`Keyword` enum (display-level)**

Flying, Menace, Intimidate, Fear, Shadow, Horsemanship, all landwalks (Plainswalk … Forestwalk), First Strike, Double
Strike, Trample, Deathtouch, Lifelink, Vigilance, Reach, Provoke, Defender, Indestructible, Hexproof, Shroud, Haste,
Flash, Prowess, Changeling, Convoke, Delve, Affinity, Storm, Flashback, Evoke, Conspire, Hideaway, Cascade, Offspring,
Persist, Ascend, Wither, Toxic, Eerie, Vivid, Fateful Bite, … (display-only — engine effect lives in handlers or
composite abilities).

**Parameterized `KeywordAbility.*`**

- `Ward(amount)` — opponent pays cost to target this.
- `Protection(color)` — protection from a single color.
- `ProtectionFrom(set)` — protection from a set of colors/types.
- `Affinity(filter)` — cost reduction per matching permanent.
- `Amplify(n)` — ETB reveal-creatures-for-counters.
- `Annihilator(n)` — attacker forces sacrifices.
- `Absorb(n)` — prevent N damage each time it would be dealt to this.
- `Bushido(n)` — +N/+N when blocking or blocked.
- `Rampage(n)` — +N/+N for each blocker past the first.
- `Afflict(n)` — defender loses N when this becomes blocked.
- `Crew(n)` — tap N power worth to animate a Vehicle.
- `Modular(n)` — ETB with +1/+1 counters, transfer on death.
- `Fading(n)` — ETB with N fade counters; removes one each upkeep, sacrifice if can't.
- `Vanishing(n)` — same idea with time counters.
- `Renown(n)` — first combat damage to a player grants renown counters.
- `Fabricate(n)` — ETB choose +1/+1 counters or Servo tokens.
- `Tribute(n)` — opponent chooses ETB bonus.
- `Toxic(n)` — adds poison counters on combat damage.
- `Cycling(cost)` — pay cost, discard, draw a card.
- `BasicLandcycling(cost)` — cycling that fetches a basic land type.
- `Typecycling(type, cost)` — cycling that fetches a card type.
- `Hideaway(cost)` — display tag; mechanic implemented via linked exile primitives.
- `Morph(cost)` — cast face-down for `{3}`, flip for cost.
- `Unmorph(cost, effect)` — turn-face-up cost + bonus effect.
- `Equip(cost)` — Equipment attach cost.
- `Fortify(cost)` — Aura-like attach cost on lands.

```kotlin
keywords(Keyword.FLYING, Keyword.VIGILANCE)
keywordAbility(KeywordAbility.Ward(2))
keywordAbilities(KeywordAbility.Protection(Color.BLUE), KeywordAbility.Annihilator(2))
```

---

## 12. Conditions (`Conditions.*`)

### Battlefield state

- `YouControl(filter)` — you control ≥1 matching permanent.
- `ControlCreature` — you control any creature.
- `ControlMoreCreatures` — you control more creatures than each opponent.
- `OpponentControlsCreature` — at least one opponent has a creature.
- `OpponentControlsMoreCreatures` — an opponent outpaces you.
- `OpponentControlsMoreLands` — an opponent has more lands.
- `OpponentControlsLandType(type)` — opponent controls land of a type.
- `TargetControlsCreature(target)` — target player has a creature.
- `TargetControlsLand(target)` — target player has a land.
- `YouHaveCitysBlessing` — you have City's Blessing (10+ permanents).

### Life & damage

- `LifeAtLeast(n, player?)` — player has ≥N life.
- `LifeAtMost(n, player?)` — player has ≤N life.
- `YouLostLife` — you lost life this turn.
- `OpponentLostLife` — an opponent lost life this turn.

### Cast / cost

- `WasCast` — source was cast (not put onto the stack).
- `WasCastFromHand` — cast specifically from hand.
- `WasCastFromZone(zone)` — cast from a specific zone.
- `WasKicked` — cast with kicker.
- `BlightWasPaid(amount)` — the Blight X additional cost was paid.

### Source state

- `SourceIsAttacking` — source is attacking.
- `SourceIsBlocking` — source is blocking.
- `SourceIsTapped` — source is tapped.
- `SourceIsUntapped` — source is untapped.

### Turn / phase

- `IsYourTurn` — it's your turn.
- `IsNotYourTurn` — it's an opponent's turn.
- `IsInPhase(phase)` — currently in `BEGINNING | MAIN | COMBAT | …`.

### Per-turn counts

- `YouAttackedWithCreaturesThisTurn(filter, atLeast)` — Raid/Battalion shape.
- `YouCastSpellsThisTurn(atLeast, filter)` — Prowess/Magecraft shape.

### Composition

- `All(c1, c2, ...)` — AND.
- `Any(c1, c2, ...)` — OR.
- `Not(c)` — negate.
- `Compare(v1, op, v2)` — numeric comparison between `DynamicAmount`s.
- `Exists(player, zone, filter)` — at least one matching object exists.
- `FixedIfCondition(...)` — bake a condition into a static-ability gate.

### Static-ability projection arm (`SourceProjectionCondition.*`)

Used inside `ConditionalStaticAbility` (projection-time evaluation):

- `ControllerAttackedWithCreaturesThisTurn` — Raid/Battalion as static.
- `ControllerCastSpellsThisTurn` — Magecraft as static.
- `ControllerHasCitysBlessing` — Ascend as static.
- `SourceChosenModeIs("id")` — gate on the chosen mode (Sieges / EntersWithChoice).

---

## 13. Dynamic amounts (`DynamicAmount.*`)

Numbers computed at resolution time.

### Math

- `Fixed(n)` — literal constant.
- `XValue` — the X chosen for the spell/ability.
- `Add(a, b)` — `a + b`.
- `Subtract(a, b)` — `a − b`.
- `Multiply(a, b)` — `a × b`.
- `Divide(a, b, roundUp?)` — division with rounding rule.
- `Min(a, b)` — minimum.
- `Max(a, b)` — maximum.
- `Absolute(a)` — `|a|`.

### Battlefield aggregation

- `AggregateBattlefield(player, filter)` — count matching permanents.
- `AggregateZone(player, zone, filter?, aggregation?)` — count cards in a zone.
- `CountPermanentsOfType(player, subtype)` — count by creature type.
- `CountCreaturesYouControl` — shorthand for "your creatures".

### Player & game

- `LifeTotal(player)` — current life total.
- `HandSize(player)` — cards in hand.
- `TurnCount(player)` — turn number for that player.
- `TurnTracking(player, TurnTracker)` — value of a per-turn counter (see below).

### Counters

- `CountersOnSource(type)` — counters of `type` on the source permanent.
- `LastKnownCountersOnSource(type)` — counters when source last existed (for dies-triggers).
- `CountersOnTarget(target, type)` — counters on a target permanent.
- `CountersOnContext(path, type)` — counters stored in an `EffectContext` path.

### Card properties

- `TargetPower(target)` — target's current power.
- `TargetToughness(target)` — target's current toughness.
- `TargetManaValue(target)` — target's mana value.
- `CardNumericProperty(card, property)` — generic numeric property accessor.

### Context-plumbed

- `ContextProperty(key)` — value plumbed via `EffectContext` (e.g. damage amount).
- `AdditionalCostBlightAmount` — X paid via the Blight additional cost.
- `ChosenNumber` — number a player chose via a Choose action.
- `VariableReference(name)` — named variable stored earlier by `StoreResult`/`StoreCount`.
- `ColorsAmongPermanents(player)` — count of distinct colors among player's permanents.

### `TurnTracker` keys (used with `TurnTracking`)

- `CREATURES_DIED` — creatures that died this turn.
- `NONTOKEN_CREATURES_DIED` — nontoken creatures that died this turn.
- `OPPONENT_CREATURES_EXILED` — opponent creatures you exiled.
- `OPPONENTS_WHO_LOST_LIFE` — count of opponents who lost life.
- `DAMAGE_RECEIVED` — damage received by player.
- `LIFE_GAINED` — life gained this turn (Bre of Clan Stoutarm).
- `LIFE_LOST` — life lost this turn.
- `PLAYER_ATTACKED` — whether/how many times you attacked.
- `DEALT_COMBAT_DAMAGE` — combat damage dealt.
- `COUNTERS_PUT_ON_CREATURE` — counters placed.
- `LANDS_PLAYED` — lands played this turn.
- `FOOD_SACRIFICED` — Food tokens sacrificed.
- `CARDS_LEFT_GRAVEYARD` — cards leaving your graveyard.

---

## 14. Modal & choice

### Modal spells

```kotlin
spell {
    modal(chooseCount = 1) {
        mode("Destroy a creature") {
            val c = target("creature", Targets.Creature)
            effect = Effects.Destroy(c)
        }
        mode("Draw a card") {
            effect = Effects.DrawCards(1)
        }
    }
}
```

- `modal(chooseCount = N) { ... }` — N modes picked at cast time (or resolution for Commands).
- `mode(description) { ... }` — one option with its own targets/effect.
- `.requiresTarget(filter)` — mode needs a target matching filter.
- `.optional()` — mode can be skipped.
- `Mode.noTarget(...)` — explicit target-less mode (outer targets are preserved).

`ModalEffect.chooseOne { mode(...) }` and `ModalEffect.chooseN(n) { ... }` for explicit modal effects.

### Permanent enters-with-choice (Sieges)

```kotlin
EntersWithChoice(
    ChoiceType.MODE,
    modeOptions = listOf(
        ModeOption(id = "khans", label = "Khans", description = "...", iconKey = "khans"),
        ModeOption(id = "dragons", label = "Dragons", description = "...", iconKey = "dragons"),
    ),
)
```

- Writes `ChosenModeComponent(modeId)` on the permanent.
- Downstream triggers/conditions gate via `SourceChosenModeIs("khans")`.
- Icons live in `web-client/src/assets/icons/options/`.

### Other choice effects

- `ChooseActionEffect(choices)` — pick one effect from a list.
- `ChooseColorThenEffect(whenChosen)` — pick a color, then apply a function of the color.
- `ChooseColorAndGrantProtectionTo{Target,Group}Effect` — color → protection from that color.
- `GrantHexproofFromChosenColorEffect(target)` — same shape, hexproof.
- `ChooseCreatureTypeEffect(...)` — pause for creature-type selection.
- `SelectTargetEffect(...)` — pick from a valid target set.
- `SeparatePermanentsIntoPilesEffect(filter, piles)` — divvy permanents into piles (Fact-or-Fiction shape).

---

## 15. Replacement effects

```kotlin
replacementEffect {
    condition = Conditions.YouControl(Filters.Swamp)
    effect = ReplacementEffect.PreventDamage(1)
}
```

- `ReplacementEffect.PreventDamage(amount?, direction?, scope?, source?, recipient?)` — prevent damage of a given shape.
- `ReplacementEffect.EntersBattlefieldTappedUnless(condition)` — ETB tapped unless condition met.
- `ReplacementEffect.IfYouDoBranchEffect(...)` — branch on "if you do" replacement.
- Custom — implement the `ReplacementEffect` interface directly.

Amount-modifying replacements expose **both** `multiplier` (×) and `modifier` (±) on the same type — do not split into
`DoubleX` + `ModifyXAmount`.

---

## 16. Counters

String-keyed counter types — resolve via the central `resolveCounterType` helper rather than per-executor character
substitution.

- `+1/+1`, `-1/-1` — power/toughness counters.
- `loyalty` — planeswalker loyalty.
- `charge`, `time`, `level`, `quest`, `shield`, `fade`, `vanishing`, `experience`, `age`, `velocity`, `awakening`,
  `blood`, `cage`, `doom`, `storage`, `divinity`, `charm`, `music`, `crumble`, `corpse`, `germ`, `ink`, `growth`,
  `hour`, `energy`, `scry`, `aura`, `chapter`, `citation`, `rune`, `scar`, `crux`, `omen`, `secret` — assorted printed
  counter kinds.

Counter effects live in §4 (`AddCounters`, `RemoveCounters`, `Proliferate`, `MoveAllLastKnownCounters`, etc.).

---

## 17. Zones & movement

**Zones** — `BATTLEFIELD`, `HAND`, `LIBRARY`, `GRAVEYARD`, `EXILE`, `STACK`.

**Primitives**

- `MoveToZoneEffect(target, zone, faceDown?, byDestruction?, linked?)` — single-target move.
- `MoveCollectionEffect(collectionName, zone, faceDown?, linkToSource?, asOwner?, likelyPosition?)` — pipeline move of a
  stored collection.
- `GatherCardsEffect(source, filter, into)` — pipeline gather from a zone into a named collection.
- `SelectFromCollectionEffect(from, into, selectCount?, allowZero?, alwaysPrompt?)` — let a player pick from a
  collection.

**Linked exile**

- `Effects.ExileGroupAndLink(filter, storeAs?)` — exile matching permanents linked to source.
- `Effects.ReturnLinkedExile` — return all to controller.
- `ReturnLinkedExileUnderOwnersControl` — return to owners.
- `ReturnLinkedExileToHand` — return to hand.
- `ReturnOneFromLinkedExile` — return one chosen card.
- `CardSource.FromLinkedExile()` — play permission targeting linked-exile pile.
- `CardSource.FromExile(name)` — play permission for a named exile zone.

**Face-down**

- `PutOntoBattlefieldFaceDown(count, target?)` — enter face-down (morph shape).
- `Triggers.TurnedFaceUp` — fires when source flips face-up.
- UI label: `"Turn Face-Up"` (used by E2E `selectAction("Turn Face-Up")`).

---

## 18. Components (set indirectly by effects)

### Permanent

- `ChosenModeComponent` — chosen entry mode (Sieges, modal permanents).
- `TypeLineOverrideComponent` — temporary type-line edits.
- `CountersComponent` — all counters on the permanent.
- `EnchantedCreatureComponent` — reference to attached creature (Auras).
- `EquippedCreatureComponent` — reference to equipped creature.
- `LinkedExileComponent` — linked exile pile attached to source.
- `ExileOnLeaveComponent` — replace next zone change with exile.
- `MayPlayFromExileComponent` — owner may play this from exile.
- `TappedStateComponent` — tap state.
- `FaceDownComponent` — face-down state.
- `ControllerComponent` — current controller.
- `ProtectionComponent` — protection from colors/types.
- `CantAttackComponent` / `CantBlockComponent` — combat restrictions.

### Player

- `PlayerCitysBlessingComponent` — you have City's Blessing.
- `SpellsCantBeCounteredComponent` — your matching spells can't be countered.
- `LifeGainedAmountThisTurnComponent` — accumulator for life gained.
- `LifeLostThisTurnComponent` — marker that you've lost life this turn.
- `PlayerAttackedThisTurnComponent` — marker that you've attacked this turn.
- `PlayerAttackersThisTurnComponent` — list of attackers declared this turn.
- `LandDropsComponent` — lands played this turn.
- `FoodSacrificeThisTurnComponent` — marker that you sacrificed a Food this turn.
- `SpellsCastThisTurnByPlayer` — count of spells you cast this turn.

Card authors rarely reference these directly; they are created/updated by the matching effect or trigger.

---

## 19. Named-mechanic composites

- **Cycling / Typecycling / Basic landcycling** — `KeywordAbility.Cycling(cost)`, `Typecycling(type, cost)`,
  `BasicLandcycling(cost)`; unified via `TypecyclingVariant(cost, searchFilter, description)` in `TypecycleCardHandler`.
- **Adventure (CR 715)** — `layout = ADVENTURE` + `cardFaces[0]` Adventure spell; DSL:
  `card { adventure("Name") { spell { … } } }`.
- **Hideaway** — `Keyword.HIDEAWAY` (display) + `MoveCollectionEffect(faceDown = true, linkToSource = true)` +
  `CardSource.FromLinkedExile()`; no special engine plumbing needed.
- **Ascend / City's Blessing** — `Keyword.ASCEND` + `Effects.GainCitysBlessing()` + `Conditions.YouHaveCitysBlessing` /
  `SourceProjectionCondition.ControllerHasCitysBlessing` + `PlayerCitysBlessingComponent`.
- **Siege (named-mode entry)** — `EntersWithChoice(ChoiceType.MODE, modeOptions = ...)` + `SourceChosenModeIs("id")`.
- **Morph** — `morph = "{2}{U}"` (top-level) + `morphFaceUpEffect` for "as it turns face up".
- **Warp** — `warp = "{1}{R}"`; alt-cost that exiles end of turn.
- **Evoke** — `evoke = "{U}"`; pay alt cost, sacrifice on ETB.
- **Earthbend** — `Effects.Earthbend` composes AnimateLand + GrantKeyword + AddCounters + granted self-triggers (no fake
  keyword).
- **Forage** — `EffectPatterns.forage`; cast-from-graveyard permissions need a branch in `CastSpellHandler.validate`.
- **Blight X** — `AdditionalCost.BlightVariable` + `DynamicAmount.AdditionalCostBlightAmount` +
  `Conditions.BlightWasPaid(n)`.
- **Divvy (Fact-or-Fiction)** — `EffectPatterns.factOrFiction(...)`; `SplitPilesDecision` stays dormant until N > 2.
- **Astral Slide / delayed return** — `ExileUntilEndStepEffect` + `DelayedTriggeredAbility`.
- **Lord effects** — multiple `staticAbility { }` blocks + `ModifyStatsForCreatureGroup` /
  `AffectsFilter.OtherCreaturesWithSubtype`.
- **Player-scoped uncounterable grant** — `Effects.GrantSpellsCantBeCountered(target, filter, duration)` +
  `SpellsCantBeCounteredComponent`.
- **Static emblems** — `Effects.CreatePermanentEmblem(...)` for planeswalker emblems with static abilities.

---

## 20. Miscellaneous author-facing knobs

- `triggeredAbility { controlledByTriggeringEntityController = true }` — the triggered ability is controlled by the
  triggering entity's controller (not source's). Useful for ETB-on-creature triggers and Death Match-style shapes.
- `metadata.oracleTextOverride` — bypass auto-generated oracle text when needed.
- `metadata.inBooster = false` — Special Guests, starter exclusives, bonus sheets.
- `colorIdentity` override is authoritative — never run `:mtg-sets:syncColorIdentityFromDump`.
- Layer dependencies (CR 613.8) — same-layer effects sort by dependency (trial application) before falling back to
  timestamp.
- Server is authoritative; never compute legal actions in the client. Every state change emits a `GameEvent` so triggers
  and animations can react.

---

## Authoritative source files

| Area               | Path                                                            |
|--------------------|-----------------------------------------------------------------|
| Card DSL           | `mtg-sdk/src/main/kotlin/.../dsl/CardBuilder.kt`                |
| Effects            | `mtg-sdk/src/main/kotlin/.../dsl/Effects.kt`                    |
| Effect patterns    | `mtg-sdk/src/main/kotlin/.../dsl/EffectPatterns.kt`             |
| Triggers           | `mtg-sdk/src/main/kotlin/.../dsl/Triggers.kt`                   |
| Costs              | `mtg-sdk/src/main/kotlin/.../dsl/Costs.kt`                      |
| Conditions         | `mtg-sdk/src/main/kotlin/.../dsl/Conditions.kt`                 |
| Filters            | `mtg-sdk/src/main/kotlin/.../dsl/Filters.kt`                    |
| Targets            | `mtg-sdk/src/main/kotlin/.../dsl/Targets.kt`                    |
| Keywords           | `mtg-sdk/src/main/kotlin/.../core/Keyword.kt`                   |
| Card model         | `mtg-sdk/src/main/kotlin/.../model/CardDefinition.kt`           |
| Dynamic amounts    | `mtg-sdk/src/main/kotlin/.../scripting/values/DynamicAmount.kt` |
| Real card examples | `mtg-sets/src/main/kotlin/.../definitions/blb/cards/`           |

For step-by-step authoring workflow see [`api-guide.md`](api-guide.md) and
[`adding-new-cards-workflow.md`](adding-new-cards-workflow.md);
for hard cases see [`managing-complex-and-rare-abilities.md`](managing-complex-and-rare-abilities.md).
