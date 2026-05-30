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
- `Costs.DiscardCard` — discard a card you choose (any card).
- `Costs.Discard(filter, count = 1, atRandom = false)` — discard `count` cards matching the filter.
  When `atRandom` is true the engine picks the cards (no player selection); otherwise the player
  chooses which cards to discard.
- `Costs.DiscardAtRandom(count, filter)` — discard `count` cards chosen at random (Meteor Storm:
  "Discard two cards at random").
- `Costs.DiscardHand` — discard your entire hand.
- `Costs.DiscardSelf` — discard this card (cycling-style).
- `Costs.Composite(c1, c2, ...)` — multiple costs paid together.

**Spell-level alternatives**

- `selfAlternativeCost` — generic "cast instead for" alt-cost.
- `evoke` — pay evoke cost; creature is sacrificed at ETB.
- `morph` — cast face-down for `{3}`-ish.
- `warp` — cast from anywhere; exiled at end of turn.
- `conditionalFlash` — flash while condition holds.
- `cantBeCountered` — spell is uncounterable.
- `xManaRestriction = setOf(Color.BLACK, Color.RED)` — "spend only [colors] on X." Restricts which
  mana may pay the `{X}` portion of the cost (the fixed colored/generic portion is unaffected).
  Available in both `spell { }` and `activatedAbility { }` blocks; honored by the mana solver and the
  payment path. Per-color amount spent on X is then readable via `DynamicAmount.ManaSpentOnX(color)`.
  Soul Burn (`spell { xManaRestriction = setOf(Color.BLACK, Color.RED) }`) and Atalya, Samite Master
  (`activatedAbility { xManaRestriction = setOf(Color.WHITE) }`) are the first users.

**`AdditionalCost`** — extra costs paid alongside the mana cost.

- `AdditionalCost.BlightVariable` — "as you cast, you may pay X life" (Blight X); X exposed via
  `DynamicAmount.AdditionalCostBlightAmount`.
- `AdditionalCost.PayLifePerTarget(amountPerTarget)` — "this spell costs N life more to cast for
  each target." Pair with an unbounded `TargetCreature(unlimited = true)` etc.; the engine
  auto-pays `amountPerTarget × action.targets.size` at cast resolution (Phyrexian Purge).

**`PayCost`** — payable costs used by [`PayOrSufferEffect`](#15-replacement-effects) ("do X
unless you Y") and by `morphCost` (non-mana face-up cost). Distinct from `AbilityCost` / `Costs.*`
which model an ability's activation cost; `PayCost` models a single cost the engine prompts the
player to pay against an alternative consequence.

- `PayCost.Mana(ManaCost)` — pay mana (auto-taps lands via the solver). "...unless you pay {U}{U}"
  (Vaporous Djinn).
- `PayCost.PayLife(amount)` — pay N life; offered only when the player has more than N life.
  "...unless you pay 3 life."
- `PayCost.Discard(filter = Any, count = 1, random = false)` — discard cards matching `filter`.
  Random variant prompts a yes/no and the engine picks the discards (Pillaging Horde).
- `PayCost.Sacrifice(filter = Any, count = 1)` — sacrifice permanents you control matching
  `filter`. Source is auto-excluded. "...unless you sacrifice three Forests" (Primeval Force).
- `PayCost.Exile(filter = Any, zone = HAND, count = 1)` — exile cards from `zone` matching
  `filter`. "...unless you exile a blue card from your hand."
- `PayCost.Tap(filter = Any, count = 1)` — tap untapped permanents you control matching `filter`.
  Source is auto-excluded. Tapping each emits a `TappedEvent` so "becomes tapped" triggers fire.
  "...unless you tap an untapped permanent you control" (Command Bridge).
- `PayCost.Choice(options)` — present several `PayCost`s; player picks one (or the suffer effect).
  Unaffordable options are hidden. "...unless they sacrifice a nonland permanent or discard a card."
- `PayCost.ReturnToHand(filter, count = 1)` — return permanents you control to their owner's hand.
  Currently only consumed by `morphCost`; not yet wired into `PayOrSufferEffect`.
- `PayCost.RevealCard(filter, count = 1)` — reveal a card from hand matching `filter`. Currently
  only consumed by `morphCost`; not yet wired into `PayOrSufferEffect`.

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
- `DrawRevealDiscardUnless(filter, target?)` — draw a card, reveal it, and discard it unless it matches `filter` (e.g. Sindbad: "draw a card and reveal it; if it isn't a land card, discard it"). Matches the drawn card in hand against `filter`.
- `Discard(count, target)` — controller-of-target chooses; mandatory.
- `EachOpponentDiscards(count)` — each opponent discards N.
- `EachPlayerReturnPermanentToHand()` — each player bounces a permanent.
- `EachPlayerDrawsForDamageDealtToSource()` — each player draws equal to damage source took this turn.
- `ReadTheRunes()` — draw N, then discard N (or sacrifice permanents).
- `ReplaceNextDraw(effect)` — replaces controller's next draw with the given effect.

### Destruction & exile

- `Destroy(target)` — destroy target (respects indestructible).
- `DestroyAll(filter, noRegenerate?, storeDestroyedAs?, excludeTriggering?)` — destroy all matching; optionally
  save the ID list for follow-up. `excludeTriggering = true` spares the triggering entity, for "destroy all
  *other* … with it" triggers (Spreading Plague).
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

### Hand reveal

- `Effects.MayRevealCardFromHand(filter, otherwise?)` — atomic "you may reveal a `filter`
  card from your hand" choice. Computes eligible hand cards; if none, runs `otherwise`
  silently; otherwise prompts the controller with a `SelectCardsDecision` (min=0, max=1).
  Revealing emits a `CardsRevealedEvent` and stops; declining (or empty selection) runs
  `otherwise`. Compose with `Effects.Tap`/`Effects.Sacrifice`/etc. via `otherwise` to
  express "if you don't, X" riders — e.g. SOI shadow lands wrap this in
  `OnEnterRunEffect(...)` with `otherwise = Effects.Tap(EffectTarget.Self)` for the
  "this land enters tapped" branch.

### Library reveal & free cast

- `Effects.Cascade` — CR 702.85a (`CascadeEffect`). Exile from the top of the controller's library
  until a nonland card with mana value **strictly less than** the triggering spell's is exiled,
  offer to cast it for free, bottom-randomize every exiled card that isn't cast.
- `RevealAndMayCastFromLibraryEffect(count, maxManaValue, player?)` — Sunbird's Invocation
  shape. Reveal top `count` cards of `player`'s library, present a `SELECT_CARDS` prompt over
  the revealed nonland cards with mana value ≤ `maxManaValue` (player picks 0 or 1), free-cast
  the chosen card if any, bottom-randomize the rest. Pair with `DynamicAmounts.triggeringManaValue()`
  (= `EntityProperty(Triggering, ManaValue)`) when both bounds come from the triggering spell.

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
- `SetLandType(landType, target, duration, fromChosenValueKey)` — target land *becomes* the basic land type, **replacing** its existing land subtypes (Rule 305.7); pass `fromChosenValueKey` to read the type from a preceding `ChooseOption(OptionType.BASIC_LAND_TYPE)`. One-shot counterpart to the `SetEnchantedLandType` aura static ability. (Dream Thrush)
- `ChooseColorForTarget(target)` — target picks a color; stored in context.
- `BecomeChosenManaColor(target)` — adopt the previously chosen color.
- `ChangeColor(colors, target, duration)` — replace colors with the given set.
- `BecomeAllColors(target, duration)` — five-color until end of turn.
- `ChangeColorToChosen(target, duration)` — replace the target's colors with the single color picked
  by a preceding `ChooseColorThen` (read from `EffectContext.chosenColor`). The target may be a
  **spell on the stack** or a permanent — the color projection reads the recolored entry in both
  zones, so a recolored spell's new color drives color-matching checks (e.g. protection) during
  resolution. Compose as `ChooseColorThen(then = ChangeColorToChosen(target))` for "target ...
  becomes the color of your choice" (Blind Seer).
- `ChangeWordInText(target, duration)` — Layer-3 text change: the player picks one **color word**
  or **basic land type** on the target and a replacement of the same category, recorded as a
  `TextReplacement` on the target. A basic-land-type swap flows through the projected type line, so
  the land's mana (via `IntrinsicManaAbilities`), landwalk relevance, and type checks all follow
  automatically (Forest→Island taps for `{U}`); a color-word swap rewrites protection-from-color and
  `HasColor`/`NotColor` filters. `duration = EndOfTurn` is stripped at cleanup; `Permanent` is the
  Artificial-Evolution-style indefinite change. The player picks the FROM and TO words on **one
  screen** (a `ChooseReplacementDecision`), with words **present on the target** surfaced first
  (labeled "On <card>") so a no-op pick is discouraged, and a live `from → to` preview. (Crystal Spray)

### Mana

- `AddMana(color, amount, restriction?)` — add N of one color.
- `AddColorlessMana(amount, restriction?)` — add colorless.
- `AddManaOfChoice(colorSet, amount?, restriction?, riders?)` — **unified primitive.** Add N mana of one color the controller picks from a resolved [ManaColorSet](#manacolorset). All "any-color from a constrained pool" cards (any color, commander identity, among permanents, lands could produce, source-chosen color) are expressed as this effect plus a different `ManaColorSet`. `riders` is a `Set<ManaSpellRider>` consumed when the mana pays for a spell (e.g. Path of Ancestry tags its mana with `ScryOnSharedTypeWithCommander`); when riders are set without a `restriction`, the engine stores the entries under `ManaRestriction.AnySpend` to preserve the rider through the pool.
- `AddAnyColorMana(amount?, restriction?)` — sugar for `AddManaOfChoice(ManaColorSet.AnyColor, amount)`.
- `AddManaOfChosenColor(amount?)` — sugar for `AddManaOfChoice(ManaColorSet.SourceChosenColor, amount)`.
- `AddManaOfColorAmong(filter)` — sugar for `AddManaOfChoice(ManaColorSet.AmongPermanents(filter))`.
- `AddManaOfColorLandsCouldProduce(scope)` — sugar for `AddManaOfChoice(ManaColorSet.LandsCouldProduce(scope))`. Fellwar Stone / Exotic Orchard / Reflecting Pool shape.
- `AddManaOfColorInCommanderColorIdentity()` — sugar for `AddManaOfChoice(ManaColorSet.CommanderIdentity)`. Arcane Signet / Command Tower shape.
- `AddAnyColorManaSpendOnChosenType(typeName)` — mana that can only pay for a specific card type (kept separate because it derives a runtime [ManaRestriction] from the source's chosen subtype).
- `AddDynamicMana(amount, allowedColors, restriction?)` — split X across a fixed color set, distinct from `AddManaOfChoice` because it distributes the full X total across multiple colors rather than producing X copies of one chosen color.
- `AddManaInAnyCombination(colors, amount)` — split N across colors (alias for `AddDynamicMana`).
- `AddOneManaOfEachColorAmong(filter)` — one mana of *each* color found among matching permanents (Bloom Tender shape).

### Tokens & emblems

- `CreateToken(name, p, t, colors?, subtypes?, keywords?, count?, tapped?)` — make N tokens. `count` accepts an
  `Int` or a `DynamicAmount` (the latter for "create X tokens" wording — e.g. Verdeloth the Ancient passes
  `count = DynamicAmount.XValue` to make X Saprolings when kicked).
- `CreateDynamicToken(dynamicPower, dynamicToughness, colors?, creatureTypes, keywords?, count?, controller?, imageUri?)` —
  tokens whose P/T is computed at resolution (e.g. Pure Reflection's X/X Reflection where X = the cast spell's mana
  value, via `DynamicAmounts.triggeringManaValue()`). `controller` directs who gets the token (e.g.
  `EffectTarget.PlayerRef(Player.TriggeringPlayer)` for "that player creates …"); `imageUri` sets custom token art.
- `CreateTokenCopyOfSelf(count?, tapped?)` — token copies of source.
- `CreateTokenCopyOfTarget(target, count?, overridePower?, overrideToughness?, tapped?, attacking?, triggeredAbilities?, addedKeywords?, addedSupertypes?, removedSupertypes?, overrideColors?, overrideSubtypes?)` —
  token copy of another permanent (or a card in any zone — the executor copies the target's `CardComponent`,
  so a graveyard/exile card works). `overrideColors`/`overrideSubtypes` replace the copy's colors/subtypes
  outright for "a token that's a copy … except it's a 5/5 black Demon" wording (Ardyn, the Usurper).
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
- `GainControlByMostEffect(metric, target?)` — the player with strictly the most of a `PlayerRankMetric` takes it (tie = no change). Metrics: `PlayerRankMetric.LifeTotal` (Ghazbán Ogre), `PlayerRankMetric.CreaturesOfSubtype(subtype)` (Thoughtbound Primoc). Facades: `Effects.GainControlByMostLife()`, `Effects.GainControlByMostOfSubtype(subtype)`.
- `GiftGivenEffect(target)` — "gift" temporary control.
- `CantAttackEffect(target, unless?)` — target can't attack.
- `CantBlockEffect(target, unless?)` — target can't block.
- `CantAttackGroupEffect(filter, condition?)` — group-scoped can't-attack.
- `CantBlockGroupEffect(filter, condition?)` — group-scoped can't-block.
- `Effects.Suspect(target)` — target becomes Suspected (MKM keyword). Composite: `SetSuspectedEffect` (named status, CR 701.60d dedup) + `GrantKeywordEffect(MENACE)` + `CantBlockEffect`.
- `RemoveFromCombatEffect(target)` — yank target out of combat.
- `SkipNextTurnEffect(target)` — target skips their next turn.
- `HijackNextTurnEffect(target)` — you control target's next turn.
- `GrantCantBeBlockedByChosenColorEffect(target, duration)` — unblockable except by chosen color.
- `CantCastSpellsEffect(target, until?)` — target can't cast spells.

### Forced sacrifice / discard

- `SacrificeTargetEffect(target, sacrificedByItsController = false)` — sacrifice a specific permanent. By
  default only fires if the resolving player controls it; set `sacrificedByItsController = true` for
  "[that creature]'s controller sacrifices it" (e.g. The Ring's Ring-bearer ability).
- `ForceSacrificeEffect(target, count)` — edict; target sacrifices N creatures.
- `ForceReturnOwnPermanentEffect(target)` — target bounces one of their own.

### Stack manipulation

- `CounterEffect(target, condition?, destination?)` — counter a spell/ability; optionally send elsewhere.
  - `target = CounterTarget.Spell` / `Ability` / `SpellOrAbility` — `SpellOrAbility` dispatches at resolution by inspecting whether the stack entity has a `SpellOnStackComponent`. Used by Teferi's Response.
  - `condition = CounterCondition.UnlessPaysMana(cost, onPaid?)` / `UnlessPaysDynamic(amount, onPaid?)` — "unless its controller pays …" with an optional `onPaid: Effect` rider that fires **only** when the spell's controller pays (Divert Disaster's "If they do, you create a Lander token"). The rider executes with the counter's controller as `controllerId`, so "you" in the rider resolves to the caster of the counter. The rider does not fire when the spell is countered. Facade: `Effects.CounterUnlessPays(cost, onPaid)` / `Effects.CounterUnlessDynamicPays(amount, exileOnCounter, onPaid)`.
- `CounterAllOnStackEffect(filter?, destination?)` — counter everything matching.
- `LifeAuction(onWin)` — open life-bidding auction between you and the controller of a targeted spell (Mages' Contest). You open at a bid of 1; the two participants alternate topping the high bid (yes/no to top, then a number for the amount, capped at the bidder's life) until one passes. The high bidder loses that much life; `onWin` runs **only if you win**, with the original targets in context — e.g. `Effects.LifeAuction(Effects.CounterSpell())`. Pair with a `TargetSpell` requirement.
- `DestroySourceOfTargetedAbilityEffect` — when the targeted stack object is a permanent's activated/triggered ability, destroy that source permanent. Compose *before* the counter step so the ability component is still readable (Teferi's Response).
- `CopyTargetSpellEffect(target)` — copy a spell on the stack.
- `CopyTargetTriggeredAbilityEffect(target)` — copy a triggered ability on the stack.
- `CopyNextSpellCastEffect` — copy the next spell its controller casts.
- `CopyEachSpellCastEffect` — copy every spell cast this turn.
- `ChangeTargetEffect(spell, newTarget)` — change a spell's target.
- `ChangeSpellTargetEffect(spell, filter)` — same, filtered.
- `ReselectTargetRandomlyEffect(spell)` — re-choose targets at random.
- `Effects.ChangeTriggeringObjectTargets(chooser = RetargetChooser.Controller)` — the player named by `chooser` may change the target or targets of the triggering spell/ability (`context.triggeringEntityId`); the player-chosen, multi-target counterpart of `ReselectTargetRandomly`. `RetargetChooser.Controller` = the effect's controller; `RetargetChooser.OwnerOfStored(name)` = the owner of the single card in pipeline collection `name` (≠1 card → no chooser → no-op). Reselection is offered slot-by-slot among the original object's legal targets (legality judged from *its* controller, current target kept as a "keep" option, no target chosen twice). **Psychic Battle** composes from atoms: `Composite(GatherCards(TopOfLibrary(1, Player.Each), revealed=true, storeAs="revealed"), FilterCollection("revealed", GreatestManaValue, storeMatching="w"), ChangeTriggeringObjectTargets(RetargetChooser.OwnerOfStored("w")))` — a tie keeps several greatest cards so `OwnerOfStored` finds no unique owner and the targets stay put.
- `ReturnSpellToOwnersHandEffect(spell)` — return a spell from the stack to hand.

### Combat-shape & misc

- `PreventDamageEffect(amount, direction, scope, sourceFilter, reflect, gainLifeFromColors, duration)` — prevention shield. `amount = null` prevents all. `sourceFilter` can be `ChosenSource` (player picks any source on resolution) or `ChosenColoredSource` (player picks a source on resolution, but only colored sources are offered — "a source of your choice that shares a color with the mana spent"; a colorless source qualifies for nothing, so it's never offered — Protective Sphere). `reflect` deals prevented damage back to the source's controller (Deflecting Palm). `gainLifeFromColors: Set<Color>` makes the shield's controller gain that much life whenever it prevents damage from a source of one of those colors (Samite Ministration). Facades: `Effects.PreventNextDamage`, `Effects.PreventNextDamageFromChosenSource(amount, target)`, `Effects.PreventAllDamageFromChosenSource(target, gainLifeFromColors)`, `Effects.PreventAllDamageFromChosenColoredSource(target)`, `Effects.DeflectNextDamageFromChosenSource()`.
- `BecomeCreatureEffect(target, p, t, subtypes, keywords, duration)` — animate non-creature (lands, artifacts).
- `EachPermanentBecomesCopyOfTargetEffect(filter, target)` — Cytoshape-style mass copy.
- `AnimateLandEffect(target, subtypes, keywords, duration)` — land becomes a creature.
- `ExploreEffect(target)` — Explore mechanic (reveal top; land → battlefield, else hand + counter).
- `AttachEquipmentEffect(equip, target)` — attach an Equipment.
- `TapUntapEffect(target, isTap)` — tap or untap. Facade: `Effects.Tap` / `Effects.Untap`.
- `PhaseOutEffect(target = Self)` — phase the target permanent out (Rule 702.26); facade `Effects.PhaseOut(target)`. While phased out it's treated as though it doesn't exist (excluded from `getBattlefield`, so from projection, triggers, combat, targeting, and SBAs) and phases back in before its controller's next untap step. Indirect phasing (attached Auras/Equipment) is handled automatically. Used as the `suffer` branch of a pay-or-phase trigger (Vaporous Djinn: "phases out unless you pay {U}{U}" = `PayOrSufferEffect(PayCost.Mana(...), Effects.PhaseOut())`).
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
- `GrantProtectionFromColor(color, target, duration)` — grant protection from a **fixed** color to a target (no player choice); a thin recipe over `GrantKeyword("PROTECTION_FROM_<COLOR>")`. "{W}: Target creature gains protection from red until end of turn." (Crimson Acolyte).
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
- `lookAtTopXAndPutOntoBattlefield(countSource, filter, shuffleAfter, entersTapped)` — look at top N (DynamicAmount), put any matching `filter` onto the battlefield (optionally `entersTapped = true`), rest back on library (`shuffleAfter` toggles shuffled vs. preserve-order). Used e.g. by Famished Worldsire's ETB land tutor.

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
- `connive(target?)` — draw 1, discard 1, then put a +1/+1 counter on `target` if the discard was a nonland (CR 702.166). Also exposed as `Effects.Connive(target)`.
- `readTheRunes()` — "draw X cards; for each, discard a card unless you sacrifice a permanent." Composes `RepeatDynamicTimesEffect(XValue, ChooseActionEffect(...))` with feasibility guards. Exposed as `Effects.ReadTheRunes()`.
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
- `EnchantedPermanent` — same `AttachedToComponent` resolution as `EnchantedCreature`, but type-agnostic; use for
  Auras that enchant non-creature permanents (e.g. Wellspring enchants a land: "gain control of enchanted land").

### Cast-time (`Targets.*` / `TargetRequirement`)

- `Targets.Any` — any creature, player, or planeswalker.
- `Targets.AnyOtherThanEnchantedCreature` — any target except the creature the source Aura/Equipment
  is attached to. Desugars to `TargetOther(AnyTarget(), excludeAttachedCreature = true)`; for Aura/Equipment
  abilities worded "enchanted/equipped creature deals damage … to **any other target**" (e.g. Pain for All),
  where the dealer is the attached creature, not the ability's source permanent.
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

### Target count

Every `TargetRequirement` carries count semantics (defaults shown):

- `count = 1` — maximum number of targets.
- `minCount = count` — minimum; set below `count` for "one or two target creatures".
- `optional = false` — when `true`, minimum becomes 0 ("up to N target ...").
- `unlimited = false` — when `true`, **"any number of target ..."** — no upper cap. The practical
  maximum is the number of legal targets, which the engine sends to the client; validation imposes
  no limit and the minimum is 0. Use this instead of a large placeholder `count` (Phyrexian Purge,
  Kaboom, Weaver of Lies). For "**X** target creatures" use `dynamicMaxCount = DynamicAmount.XValue`
  instead — that clamps the count to the chosen X.
- `dynamicMaxCount: DynamicAmount?` — evaluated when the spell/ability hits the stack; the resolved
  value becomes the max ("up to X target creatures", X = board state or chosen X).
- `sameController = false` — on `TargetObject` / `TargetCreature(...)`; when `true` and the requirement
  picks more than one target, every chosen target must share a controller ("**two target creatures
  controlled by the same player**"). Enforced cross-target by `TargetValidator` at cast time using
  projected control; a no-op for single-target requirements. E.g.
  `TargetCreature(count = 2, sameController = true)` (Barrin's Spite).

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
- `GameObjectFilter.Multicolored` — multicolored card (two or more colors; `CardPredicate.IsMulticolored`).

**Chained predicates**

- `.youControl()` / `.controlledByOpponent()` — control predicate.
- `.withSubtype(s)` / `.withKeyword(k)` — type/ability predicate.
- `.ofColor(c)` / `.ofColors(set)` — color predicate.
- `.withColor(c)` / `.withAnyColor(c…)` / `.notColor(c)` — fixed-color predicates (`CardPredicate.HasColor`/`NotColor`).
- `.withChosenColor()` — `CardPredicate.HasChosenColor`: matches the color chosen during the current
  effect's resolution (read from `EffectContext.chosenColor`, set by `Effects.ChooseColorThen`). Use with
  `AggregateBattlefield(Player.Each, …)` for "for each permanent of that color" (Coalition Dragon cycle).
- `.sharingColorWith(entity)` — `CardPredicate.SharesColorWith(entity)`: shares ≥1 (projected) color with
  a referenced entity (e.g. `EntityReference.Triggering`). Mirror of `.sharingCreatureTypeWith(entity)`.
  Colorless entities share no color (never match). Used by Spreading Plague ("destroy all other creatures
  that share a color with it") — pair with `Effects.DestroyAll(filter, excludeTriggering = true)` so the
  triggering creature itself is spared.
- `.named(name)` — `CardPredicate.NameEquals`: matches a fixed card name.
- `.namedFromVariable(variableName)` — `CardPredicate.NameEqualsChosen`: matches the card name stored in
  `chosenValues[variableName]` (case-insensitive). Set the name with `Effects.ChooseCardName` (player names it)
  or `Effects.StoreCardName` (captured from a chosen card). Fails closed in static/projection contexts. Used by
  the "name a card … cards with that name" family (Desperate Research, Lobotomy).
- `.power(n)` / `.minPower(n)` / `.maxPower(n)` — P/T comparator.
- `.manaValue(n)` / `.manaValueAtMost(n)` / `.manaValueAtLeast(n)` — mana-value comparator.
- `.manaValueAtMostX()` — mana value ≤ the X chosen for the source spell/ability.
- `.manaValueAtMostEntity(ref)` — mana value ≤ a referenced entity's mana value (e.g. Kodama of the East Tree).
- `.manaValueAtMostEntityManaSpent(ref)` — mana value ≤ the mana **actually spent** to cast a referenced
  entity. Reads the live `SpellOnStackComponent` buckets while the entity is still a spell, or the
  `CastRecordComponent` snapshot once it has resolved onto the battlefield (0 if it was never cast).
  Used by Edge of Eternities warp payoffs like Astelli Reclaimer ("…mana value X or less…, where X is the
  amount of mana spent to cast this creature") — X is 5 for `{3}{W}{W}`, 3 for warp `{2}{W}`, 0 for free.
- `.manaValueIsOdd()` / `.manaValueIsEven()` — mana-value parity (zero is even). Pair with modal
  spells whose modes ask the caster to choose a parity (e.g. *Mutinous Massacre*).
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

### Stack-object predicates

These `CardPredicate`s evaluate against entities in the `Zone.STACK` (spells and activated/triggered
abilities on the stack). They are handled in the evaluator before the `CardComponent` check, so they
work for abilities-on-stack (which carry no `CardComponent`).

- `CardPredicate.IsActivatedOrTriggeredAbility` — true for activated/triggered abilities on the stack
  (Stifle).
- `CardPredicate.IsTriggeredAbility` — triggered abilities only (excludes activated abilities and
  spells).
- `CardPredicate.TargetsMatching(subfilter)` — true when the stack object's `TargetsComponent`
  includes at least one chosen target matching `subfilter`. Player targets are skipped. The
  subfilter inherits the outer `PredicateContext`, so `Land.youControl()` inside the subfilter
  resolves against the outer chooser. Used by Teferi's Response.

### `StatePredicate` — battlefield state checks

- `IsTapped` — currently tapped.
- `IsUntapped` — currently untapped.
- `IsAttacking` — declared as attacker this combat.
- `IsBlocking` — declared as blocker this combat.
- `InSameBandAsSource` (filter builder `inSameBandAsSource()`) — source-relative (CR 702.22):
  matches the effect's source creature itself and any creature sharing its combat band id.
  Resolves against `PredicateContext.sourceId`, so it only matches while that source is attacking
  (band membership exists only during combat). Used as the recipient filter of Camel's
  "prevent all damage Deserts would deal to this creature and to creatures banded with this
  creature". Note: it's only evaluated where the context carries a source entity — currently the
  recipient filter of a `PreventDamage` replacement (see §15); it's inert in group/projection,
  untap, and trigger-gating contexts.
- `IsFaceDown` — currently face-down.
- `HasCounter(type)` — has at least one counter of `type`.
- `IsWarpExiled` (filter builder `warpExiled()`) — card in exile via warp's
  end-of-turn delayed trigger (CR 702.185b).
- `WasCastForWarp` (filter builder `castForWarp()`) — battlefield permanent that
  was cast for its warp cost (CR 702.185). Pair with
  `Conditions.TargetMatchesFilter(GameObjectFilter.Creature.castForWarp(), …)` to
  branch on whether a target was warp-cast (e.g., Full Bore).

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

Named sugar for the common cases; reach for `entersBattlefield(...)` / `leavesBattlefield(...)`
for any other (filter, binding, to/excludeTo) combination.

**Enters the battlefield**

- `EntersBattlefield` — SELF, no filter. ("When this permanent enters.")
- `OtherCreatureEnters` — OTHER binding, filter = `Creature.youControl()`.
- `LandYouControlEnters` — landfall: OTHER binding, filter = `Land.youControl()`.
- `entersBattlefield(filter, binding)` — factory. Covers face-down filters,
  ANY-binding tribal scopes, permanent-you-control scopes, enchantment-enters scopes (Eerie), etc.

**Leaves / dies**

- `LeavesBattlefield` — SELF, any destination.
- `Dies` — SELF, battlefield → graveyard.
- `AnyCreatureDies` — ANY binding, filter = `Creature`.
- `YourCreatureDies` — ANY binding, filter = `Creature.youControl()`.
- `PutIntoGraveyardFromBattlefield` — SELF, same event shape as `Dies`; rename
  clarifies non-creature intent (artifact / enchantment going to yard).
- `leavesBattlefield(filter, to?, excludeTo?, binding)` — factory. `to = GRAVEYARD`
  gives a "dies" variant scoped beyond the named constants (other tribal deaths,
  any-controller deaths); `excludeTo = GRAVEYARD` gives "leaves without dying"
  (Three Tree Scribe shape); leaving both null gives "leaves to any zone."

### Combat

Named sugar for the common cases; reach for `attacks(...)` / `blocks(...)` /
`becomesBlocked(...)` for any other combination, and use the [AttackPredicate]
sealed set for attack-time facts beyond the basics.

**Attacks (per-attacker `AttackEvent`)**

- `Attacks` — SELF, no filter. ("When this creature attacks.")
- `attacks(filter?, requires?, binding?)` — factory. Covers ANY-binding scopes,
  type-filtered scopes (creature-you-control, nontoken-creature-you-control),
  and attack-time predicates (alone, future Battalion-style count gates).

**Attacks (player-level)**

- `YouAttack` — when you declare attackers (player-level, ANY binding).
- `YouAttackWithFilter(filter)` — when you attack with ≥1 matching attacker.
- `CreaturesAttackYou` — defender side; fires once per `AttackersDeclaredEvent`,
  not per attacker. Excludes creatures attacking a planeswalker you control
  (CR 509.1b). Pair with `DynamicAmounts.creaturesAttackingYou()` for
  attacker-count payoffs (e.g., Orim's Prayer).

**Blocks**

- `Blocks` — SELF, no filter.
- `BecomesBlocked` — SELF, no filter.
- `blocks(filter?, binding?, attackerFilter?)` — factory. `filter` constrains the
  blocker (ANY binding). `attackerFilter` constrains the blocked attacker — requires
  SELF binding for "whenever this creature blocks a [filter]" (Skystinger);
  combining it with ANY is rejected (the ANY detector branch ignores `attackerFilter`).
  `triggeringEntityId` is set to the blocked attacker in that case.
- `becomesBlocked(filter?, binding?)` — factory. Replaces the old
  `CreatureYouControlBecomesBlocked` and `FilteredBecomesBlocked(filter)`.
- `BlocksOrBecomesBlockedBy(filter)` — either direction, partner-filtered;
  sole consumer of `BlocksOrBecomesBlockedByEvent`. Prefer `blocks(attackerFilter=...)`
  when only the blocking direction should fire.

**`AttackPredicate`** — extensible "facts about an attack declaration."
Adding a new attack-time mechanic is one new sealed-case + one matcher branch
— `AttackEvent` does not grow a new field per axis.

- `AttackPredicate.Alone` — the attacker is the only declared attacker this
  combat (`attacker count == 1`). Replaces the old `alone: Boolean` axis.
- `AttackPredicate.AttackerCountAtLeast(n)` — at least N creatures total were
  declared as attackers (counting the trigger's attacker). Battalion shape:
  `attacks(requires = setOf(AttackerCountAtLeast(3)))` on a `SELF` binding.

Examples:

```kotlin
// "Whenever this creature attacks alone"
Triggers.attacks(requires = setOf(AttackPredicate.Alone))

// "Whenever a nontoken creature you control attacks"
Triggers.attacks(
    filter = GameObjectFilter.Creature.youControl().nontoken(),
    binding = TriggerBinding.ANY,
)

// "Whenever a Beast becomes blocked"
Triggers.becomesBlocked(
    filter = GameObjectFilter.Creature.withSubtype("Beast"),
    binding = TriggerBinding.ANY,
)

// "Whenever this creature blocks a creature with flying" (Skystinger)
Triggers.blocks(attackerFilter = GameObjectFilter.Creature.withKeyword(Keyword.FLYING))
```

`Triggers.BecomesBlocked` (SELF, **unfiltered**) fires **once** when the creature becomes
blocked, regardless of how many creatures block it, with `triggeringEntityId` = the source —
so `DynamicAmounts.numberOfBlockers()` reads this creature's blocker count (Rampage). The
**filtered** SELF form `becomesBlocked(filter = …)` instead fires once per matching blocker,
with `triggeringEntityId` = that blocker (Flanking gives each blocker -1/-1).

### Damage

Named sugar for the common cases; reach for the factories for any other combination of axes.

- `DealsDamage` — source deals any damage (SELF binding).
- `DealsCombatDamageToPlayer` — source deals combat damage to a player (SELF binding).
- `DealsCombatDamageToCreature` — source deals combat damage to a creature (SELF binding).
- `TakesDamage` — source is dealt damage by any source (SELF binding).
- `CreatureDealtDamageByThisDies` — Etali / Sengir / Soul Collector shape; only consumer of `CreatureDealtDamageBySourceDiesEvent`.

**Factories** (axes: `damageType` × `recipient` × `sourceFilter` × `binding` for outgoing; `source` × `binding` for incoming):

- `dealsDamage(damageType?, recipient?, sourceFilter?, binding?)` — outgoing-damage trigger. Pick `DamageType.{Any,Combat,NonCombat}`, `RecipientFilter.{Any,AnyPlayer,AnyPlayerOrPlaneswalker,AnyCreature,…}`, an optional source `GameObjectFilter`, and `TriggerBinding.{SELF,ANY,ATTACHED}`. Covers "deals combat damage to a player or planeswalker", "creature you control deals combat damage to a player" (`binding = ANY` + `sourceFilter = Creature.youControl()`), "nontoken creature you control deals…" (`.nontoken()`), and "enchanted creature deals damage" (`binding = ATTACHED`).
- `takesDamage(source?, binding?)` — incoming-damage trigger. Pick `SourceFilter.{Any,Creature,Spell,Combat,NonCombat,HasColor(c),…}` and `TriggerBinding.{SELF,ATTACHED}`. Covers "damaged by a creature/spell" and "enchanted creature is dealt damage" (`binding = ATTACHED`, Aurification / Frozen Solid shape).

### Phase & turn

Named sugar for the common `(step, player)` cases; reach for `phase(step, player?, binding?)`
for anything else (the ATTACHED-binding aura shapes, custom step/player combinations).

- `YourUpkeep` — start of your upkeep.
- `YourDrawStep` — start of your draw step.
- `EachUpkeep` — every upkeep.
- `EachOpponentUpkeep` — at each opponent's upkeep.
- `YourEndStep` — beginning of your end step.
- `EachEndStep` — beginning of each end step.
- `BeginCombat` — start of combat on your turn.
- `EachCombat` — beginning of each combat (any player's turn).
- `FirstMainPhase` — start of pre-combat main.
- `YourPostcombatMain` — start of post-combat main.

**Factory** — `phase(step, player = Player.You, binding = TriggerBinding.ANY)`.

### Aura / equipment

No named constants for the "enchanted/equipped creature does X" shapes — they all collapse to
the existing event factories with `binding = TriggerBinding.ATTACHED`. Examples (all card uses
in the repo today):

- *Enchanted creature dies* (Demonic Vigor):
  `Triggers.leavesBattlefield(to = Zone.GRAVEYARD, binding = TriggerBinding.ATTACHED)`
- *Enchanted/equipped creature leaves the battlefield* (Curator's Ward):
  `Triggers.leavesBattlefield(binding = TriggerBinding.ATTACHED)`
- *Enchanted/equipped creature attacks* (Extra Arms, Heart-Piercer Bow, Ordeal of Nylea,
  Chorale of the Void, Atomic Microsizer, Sorcerer Role token):
  `Triggers.attacks(binding = TriggerBinding.ATTACHED)`
- *Enchanted permanent becomes tapped* (Uncontrolled Infestation, Cryoshatter):
  `Triggers.becomesTapped(binding = TriggerBinding.ATTACHED)`
- *Enchanted creature is turned face up* (Fatal Mutation):
  `Triggers.turnedFaceUp(binding = TriggerBinding.ATTACHED)`
- *At the beginning of enchanted creature's controller's `<step>`* (Custody Battle,
  Lingering Death): `Triggers.phase(step, binding = TriggerBinding.ATTACHED)`
- *Enchanted-creature damage triggers* — damage factories already support binding:
  `Triggers.dealsDamage(binding = TriggerBinding.ATTACHED)` (any damage),
  `Triggers.dealsDamage(damageType = Combat, recipient = AnyPlayer, binding = TriggerBinding.ATTACHED)`,
  `Triggers.takesDamage(binding = TriggerBinding.ATTACHED)` (Aurification / Frozen Solid).

### Cards & draws

- `YouDraw` — when you draw a card.
- `RevealCreatureFromDraw` — Hatching Plans-style top-card reveal.
- `RevealCardFromDraw` — generic reveal-from-draw trigger.
- `CardsPutIntoYourGraveyard(filter?)` — when matching cards enter your yard.
- `PermanentCardsPutIntoYourGraveyard` — only permanent cards.
- `CreaturesPutIntoGraveyardFromLibrary` — mill-trigger shape.

### Discard

Fires once per card discarded — a single resolution that discards N cards fires the
trigger N times (mirrors how `YouDraw` handles multi-card draws). The engine emits
one aggregate `CardsDiscardedEvent` per resolution and fans it out in the detector.
`Player.TriggeringPlayer` resolves to the discarding player inside the effect.

- `AnyOpponentDiscards` — whenever an opponent discards a card. (Entropic Battlecruiser.)
- `YouDiscard` — whenever you discard a card.

**Factory** — `discards(player?, cardFilter?)` — generic shape. `player = Player.Each`
matches any player; `cardFilter` narrows the fan-out to matching cards, so a batch that
discards a creature and two lands fires a `cardFilter = Creature` trigger once, not three
times. The cardFilter is evaluated against the **post-discard zone** (the cards are already
in the graveyard when the trigger matches) — safe for type/subtype/color predicates,
but a filter that depends on hand-specific state would read the wrong zone.

### Spell casting

Named sugar for the common type-primitive cases; reach for `youCastSpell(...)` plus a
`SpellCastPredicate` set for anything from-zone / kicked / mana-source-tagged.

- `YouCastSpell` — any spell you cast.
- `YouCastCreature` — any creature spell you cast.
- `YouCastNoncreature` — non-creature spells you cast.
- `YouCastInstantOrSorcery` — instant/sorcery you cast.
- `YouCastEnchantment` — any enchantment you cast.
- `YouCastHistoric` — artifact / legendary / Saga.
- `YouCastSubtype(subtype)` — tribal helper: spell with matching subtype.
- `AnySpellOrAbilityOnStack` — any object hits the stack.

**Other casters.** The same shape, scoped to a different caster via the runtime
`Player.Each` / `Player.Opponent` matching on `SpellCastEvent`. Bind the payoff to the
caster with `EffectTarget.PlayerRef(Player.TriggeringPlayer)`.

- `AnyPlayerCastsSpell` — any player (including you) casts a spell.
- `OpponentCastsSpell` — an opponent casts a spell.
- `AnyPlayerChoosesTargets` — any player casts a spell, activates an ability, or puts a triggered ability on the stack with ≥1 target (fires once per object via `GameEvent.TargetsChosenEvent`). The triggering entity is that spell/ability, so the payoff can read/change its targets (Psychic Battle).
- `anyPlayerCasts(spellFilter?, requires?)` — factory; e.g. `anyPlayerCasts(GameObjectFilter.Creature)`
  for "whenever a player casts a creature spell" (Pure Reflection).
- `opponentCasts(spellFilter?, requires?)` — factory; e.g. `opponentCasts(GameObjectFilter.Multicolored)`
  for "whenever an opponent casts a multicolored spell" (Rewards of Diversity).

**Factory** — `youCastSpell(spellFilter?, requires: Set<SpellCastPredicate>)`. The
`requires` set is conjunctive — every predicate must hold for the trigger to fire.

**`SpellCastPredicate`** — extensible "facts about a cast." Adding a new cast-time mechanic
(was-copied, was-overloaded, paid-additional-life-cost, …) is one new sealed-case plus one
matcher branch — `SpellCastEvent` does not grow a new field per axis.

- `SpellCastPredicate.CastFromZone(zone)` — spell was cast from this zone. Used for Sunbird's
  Invocation (`Zone.HAND`), Goliath Daydreamer's instant/sorcery-from-hand trigger,
  Wildsear's enchantment-from-hand cascade.
- `SpellCastPredicate.WasKicked` — spell was cast with kicker (CR 702.32). Used for
  Hallar / Bloodstone Goblin.
- `SpellCastPredicate.PaidWithManaFromSubtype(subtype)` — mana from a permanent of this
  subtype was spent on the cast. Resolves Treasure today (Rain of Riches, Alchemist's
  Talent); engine matcher accepts other token subtypes as the shape, but only Treasure
  actually fires until the mana-pool tracker generalizes beyond its current Treasure-only
  boolean.
- `SpellCastPredicate.IsModal` — spell was cast with at least one chosen mode (rules
  700.2). Matches `SpellCastEvent.chosenModesCount > 0`, where the count is the size of
  `SpellOnStackComponent.chosenModes` (so Spree picking the same mode twice counts as
  two). Used by Riku of Many Paths: "Whenever you cast a modal spell, …".

Examples:

```kotlin
// "Whenever you cast a spell from your hand"
Triggers.youCastSpell(requires = setOf(SpellCastPredicate.CastFromZone(Zone.HAND)))

// "Whenever you cast an instant or sorcery from your hand"
Triggers.youCastSpell(
    spellFilter = GameObjectFilter.InstantOrSorcery,
    requires = setOf(SpellCastPredicate.CastFromZone(Zone.HAND)),
)

// "Whenever you cast a kicked spell"
Triggers.youCastSpell(requires = setOf(SpellCastPredicate.WasKicked))

// "Whenever you cast a spell using mana from a Treasure"
Triggers.youCastSpell(
    requires = setOf(SpellCastPredicate.PaidWithManaFromSubtype(Subtype.TREASURE)),
)

// "Whenever you cast a modal spell" (Riku of Many Paths)
Triggers.youCastSpell(requires = setOf(SpellCastPredicate.IsModal))

// "Whenever you cast a noncreature or Otter spell"
Triggers.youCastSpell(
    spellFilter = GameObjectFilter.Noncreature or
                  GameObjectFilter.Any.withSubtype(Subtype("Otter")),
)
```

### State change & misc

- `TurnedFaceUp` — source turns face up. Use `turnedFaceUp(binding)` for the ATTACHED-binding aura variant (Fatal Mutation).
- `CreatureTurnedFaceUp(player?)` — when a creature you control turns face up.
- `GainControlOfSelf` — you gain control of source.
- `BecomesTarget(filter?)` — source becomes target of spell/ability.
- `CreatureYouControlBecomesTargetByOpponent(filter?)` — your creature gets targeted by opponent.
- `Transforms` — source transforms (either direction).
- `TransformsToFront` — to front face.
- `TransformsToBack` — to back face.
- `YouCycleThis` — you cycle source.
- `AnyPlayerCycles` — anyone cycles.
- `AnyPlayerTapsLandForMana` — whenever any player taps a land for mana. Use
  `landTappedForMana(player, landFilter, binding)` for "an opponent"/"you" variants or a land-type
  restriction. Fires on the manual mana-ability path only (auto-pay adds mana via the solver without
  emitting the event). Backs the "whenever a player taps a land for mana" family (Mana Flare, Heartbeat
  of Spring); the inline-static cards (Overabundance, Pulse) use the mana statics in §9 instead.
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

### The Ring

- `RingTemptsYou` — whenever the Ring tempts you (CR 701.52d). Paired with `Effects.TheRingTemptsYou()`.

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
- `SetEnchantedLandType(landType)` — "Enchanted land is an Island" — replaces the enchanted
  land's basic land types with a fixed type (Rule 305.7). (Sea's Claim)
- `SetEnchantedLandTypeFromChosen` — "Enchanted land is the chosen type" — same, but reads the
  type from the source's `ChosenLandTypeComponent` (paired with
  `EntersWithChoice(ChoiceType.BASIC_LAND_TYPE)`). Chosen-value counterpart to
  `SetEnchantedLandType`, mirroring `GrantChosenColor`/`GrantColor`. (Phantasmal Terrain)
- `GrantLandwalkOfChosenType(filter = attachedCreature())` — "Enchanted creature has landwalk of
  the chosen type" — grants the landwalk keyword matching the source's `ChosenLandTypeComponent`
  (Plains→Plainswalk, Island→Islandwalk, …) at projection time. Chosen-value counterpart to
  `GrantKeyword`; pair with `EntersWithChoice(ChoiceType.BASIC_LAND_TYPE)`. (Traveler's Cloak)
- `GrantProtectionFromControlledColors(filter = attachedCreature())` — "[filter] have protection from
  the colors of permanents you control" — grants the affected creature(s) protection from every color
  among the permanents the source's controller controls, recomputed at projection (Layer 6, after
  Layer 5 colors) so it tracks the board in real time. Colorless permanents add no color. (Pledge of
  Loyalty)
- `GrantCardType(cardType, filter)` / `RemoveCardType(cardType, filter)` — Layer 4 type-changing statics that add or
  remove a card type (e.g. `"CREATURE"`). `RemoveCardType` backs Impending's "isn't a creature while it has a time
  counter" (wrapped in a `ConditionalStaticAbility`); reuse it for any "it's no longer a [type]" effect.
- `ConditionalStaticAbility` — static gated by a runtime `Condition`.
- `Effects.CreatePermanentEmblem(...)` — emblem with static abilities (planeswalker ultimates).
- `AttackTax(amountPerAttacker: DynamicAmount)` — Propaganda / Ghostly Prison / Windborn Muse /
  Collective Restraint. Per-attacker generic-mana tax for attacking the source's controller; the
  amount is a `DynamicAmount` so it can scale with state (e.g., `DynamicAmounts.domain()` for
  "{X} where X is your domain"). Evaluated with the source permanent's controller as "you".
  When `totalTax > 0`, the engine pauses `DeclareAttackers` for a `YesNoDecision` *before* tapping
  any mana — declining is a clean no-op that leaves the player in `DECLARE_ATTACKERS` to re-declare.
  The same prompt/cancel pattern applies to block-tax floating effects (e.g. Whipgrass Entangler)
  via `AttackBlockTaxPerCreatureType`.
- `CantBeAttackedWithout(keyword)` — Form of the Dragon-style "Creatures without flying can't
  attack you." defender-side restriction.
- `CantAttackUnlessCoAttacker(coAttackerFilter, filter = source)` — "This creature can't attack
  unless [a creature matching coAttackerFilter] also attacks" (Scarred Puma). Unlike
  `CantAttackUnless` (which is defender-relative), this depends on the whole proposed attacker
  group, so it's validated against the other declared attackers at declaration time (projected
  state; self never counts as its own co-attacker).

**Spell cost statics — `ModifySpellCost`**

Replaces the per-shape cost classes. Use directly as the `ability` of a `staticAbility { }` block.

```kotlin
staticAbility {
    ability = ModifySpellCost(
        target = SpellCostTarget.YouCast(GameObjectFilter.Any),
        modification = CostModification.ReduceGeneric(2),
        gating = CostGating.NthOfTypePerTurn(2),
    )
}
```

- `target: SpellCostTarget` — `SelfCast`, `YouCast(filter)`, `AnyCaster(filter)`,
  `OpponentsCastTargeting(GroupFilter)`, `FaceDownYouCast`, `MorphActivation`.
- `modification: CostModification` — `ReduceGeneric(amount)`, `ReduceGenericBy(source)`,
  `ReduceColored(symbols)`, `ReduceColoredPerUnit(symbols, source)`, `IncreaseGeneric(amount)`,
  `IncreaseColored(symbols)` (colored tax — adds colored pips, e.g. the Invasion Leeches'
  "White spells you cast cost {W} more"), `IncreaseGenericPerOtherSpellThisTurn(amountPerSpell)`,
  `IncreaseLife(amount)`.
  Reduction `source: CostReductionSource` covers fixed amounts, counts of permanents/cards in
  zones, target/condition gates, and a few mechanic-specific shapes — see
  `CostStaticAbilities.kt` for the full list.
- `gating: CostGating` — restricts how often the modifier fires:
  - `None` (default) — applies to every matching cast.
  - `NthOfTypePerTurn(n)` — only when this is the Nth matching spell each turn (1-indexed; counts the
    spell currently being cast). Use `n = 1` for "the first ... each turn" (Eluge); use
    `NthOfTypePerTurn(2)` with `target = YouCast(GameObjectFilter.Any)` for Uthros Psionicist's "the
    second spell you cast each turn costs {2} less".

`NthOfTypePerTurn` requires a filter-bearing target (`YouCast` / `AnyCaster`) — it needs a notion
of "type" to count.

**Global denial statics** (no `filter`/`duration` block — they're singleton-style)

- `PreventCycling` — "Players can't cycle cards." (Stabilizer)
- `PreventActivatedAbilities(filter)` — activated abilities (mana + non-mana) of matching
  permanents can't be activated; loyalty abilities and animation costs that haven't yet
  produced a creature are unaffected. (Cursed Totem → `GameObjectFilter.Creature`)
- `PreventManaPoolEmptying` — mana pools don't empty between steps/phases. (Upwelling)
- `NoMaximumHandSize` — controller has no hand-size limit. (Thought Vessel)
- `DampLandManaProduction` — a land tapped for 2+ mana produces `{C}` instead. (Damping Sphere)
- `RestrictSpellsCastPerTurn(maxPerTurn)` — the controller can't cast more than `maxPerTurn`
  spell(s) each turn. Per-controller; the most restrictive applies when several are in play.
  Already-cast spells count, even those cast before this permanent entered. (Yawgmoth's Agenda)
- `CantCastSpellsSharingColorWithLastCast` — *global* (all players): can't cast a spell that shares a
  color with the spell most recently cast this turn. Backed by `GameState.lastCastSpellColors` (the
  colors of the last spell cast, cleared each turn). Never blocks the first spell of the turn; a
  colorless spell shares no color, so it is always castable and casting one lifts the restriction
  until the next colored spell. (Mana Maze)

**Tapped-for-mana mana statics** (extra mana / replaced mana when a land is tapped for mana — resolve
inline as triggered mana abilities, off the stack per CR 605). These fire on the *manual* mana-ability
path; automatic cost payment adds the extra/replacement *mana* via the solver but skips non-mana
riders, matching how the engine already treats e.g. City of Brass's damage during auto-pay.

- `AdditionalManaOnTap(color, amount, anyColor = false)` — aura: "Whenever enchanted land is tapped
  for mana, its controller adds additional mana." `color = null` reads the aura's `ChosenColorComponent`;
  `anyColor = true` makes it one mana of **any color the controller chooses** each tap (prompts on a
  manual tap; flexible for the solver). (Elvish Guidance = fixed `{G}`; **Fertile Ground** = `anyColor`)
- `AdditionalManaOnSourceTap(sourceFilter, color = null, amount = 1, rider = null)` — global: "Whenever
  a `<sourceFilter>` is tapped for mana, that player adds …". `color = null` mirrors the produced color.
  `rider` is an optional non-mana `Effect` resolved inline, controlled by the tapping player
  (`EffectTarget.Controller` = tapper, `EffectTarget.Self` = the static's source). (Lavaleaper = basic-land
  mirror; Badgermole Cub = `+{G}`; **Overabundance** = `GameObjectFilter.Land` mirror + `DealDamage(1,
  Controller)` rider)
- `ReplaceLandManaColor(filter)` — global: lands matching `filter` produce one mana of a color of their
  controller's choice instead of their normal mana. Implemented by swapping the land's base mana effect
  for "add one mana of any color", so the choice flows through the normal any-color machinery (manual tap
  prompts; solver treats a matched basic as a five-color source). (**Pulse of Llanowar** =
  `GameObjectFilter.BasicLand.youControl()`)
- `OverrideEnchantedLandManaColor(color)` — aura: replaces the enchanted land's *own* produced color with
  a fixed/aura-chosen `color` (vs. `ReplaceLandManaColor`'s filter-based, free-choice form). (Shimmerwilds Growth)

**Alternative play / cast permissions** (let a player play or cast cards from non-hand zones)

- `MayPlayLandsFromGraveyard` — play lands from your graveyard (no per-turn cap). (Icetill Explorer)
- `MayPlayPermanentsFromGraveyard` — Muldrotha: play a land + cast one permanent spell of each
  permanent type from your graveyard each turn (per-type-per-turn cap).
- `MayCastFromGraveyard(filter, lifeCost = 0, duringYourTurnOnly = false)` — cast spells matching
  `filter` from your graveyard following normal timing, optionally paying `lifeCost` life. Free for
  Yawgmoth's Agenda (`MayCastFromGraveyard(Nonland)`); `lifeCost = 1, duringYourTurnOnly = true` for
  Festival of Embers. Pair with `MayPlayLandsFromGraveyard` for "play lands and cast spells from
  your graveyard". Lands are *played*, not cast, so they need the lands permission separately.

**Top-of-library reveal & play** (reveal the top card of a library, optionally with permission to
play it from there). Visibility (public reveal to all players) and play permission are separate
concerns — the `ClientStateTransformer` reveals the top card for `PlayFromTopOfLibrary` *or*
`RevealTopOfLibrary`, while the cast/play-from-top paths key only on the play-granting variants.

- `RevealTopOfLibrary` — *public reveal only*, no play permission: the controller's top card is
  shown to all players, but can only be played once drawn. (**Goblin Spy**)
- `PlayFromTopOfLibrary` — public reveal **and** "play lands and cast spells from the top of your
  library" (all card types). (Future Sight)
- `PlayLandsAndCastFilteredFromTopOfLibrary(spellFilter)` — like `PlayFromTopOfLibrary` but only
  spells matching `spellFilter` are castable (lands always playable). (Glarb, Calamity's Augur =
  `GameObjectFilter.Any.manaValueAtLeast(4)`)
- `CastSpellTypesFromTopOfLibrary(filter)` — cast only matching spell types from the top; no land
  play, no full public reveal. (Precognition Field = instants/sorceries)
- `LookAtTopOfLibrary` — *private*: the controller may look at their own top card any time (revealed
  only to them, not opponents). (Lens of Clarity, Vizier of the Menagerie)

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
Strike, Trample, Deathtouch, Lifelink, Vigilance, Reach, Provoke, Flanking, Defender, Indestructible, Hexproof, Shroud, Haste,
Flash, Prowess, Changeling, Convoke, Delve, Affinity, Storm, Flashback, Evoke, Impending, Conspire, Hideaway, Cascade, Plot,
Offspring, Persist, Ascend, Wither, Toxic, Eerie, Vivid, Fateful Bite, … (display-only — engine effect lives in handlers or
composite abilities).

**Parameterized `KeywordAbility.*`**

- `Ward(amount)` — opponent pays cost to target this.
- `Protection(color)` — protection from a single color.
- `ProtectionFrom(set)` — protection from a set of colors/types.
- `Protection(ProtectionScope.Supertype("Legendary"))` / `KeywordAbility.protectionFromSupertype("Legendary")` — protection from a supertype, e.g. "protection from legendary creatures" (Tsabo Tavoc). Enforced across targeting, blocking, and combat damage via projected `PROTECTION_FROM_SUPERTYPE_<X>` keywords.
- `Affinity(filter)` — cost reduction per matching permanent.
- `Amplify(n)` — ETB reveal-creatures-for-counters.
- `Devour(multiplier, sacrificeFilter, variant)` — "As this enters, you may sacrifice any number of [sacrificeFilter]. It enters with [multiplier] × that many +1/+1 counters." Plain Devour uses `sacrificeFilter = Creature` and `variant = ""`; the Edge of Eternities variant "Devour land N" uses `KeywordAbility.devourLand(n)` (`sacrificeFilter = Land`, `variant = "land"`). The keyword surfaces the rules text; pair with [`EntersWithDevour`](#15-replacement-effects) for the mechanical behavior.
- `Annihilator(n)` — attacker forces sacrifices.
- `Absorb(n)` — prevent N damage each time it would be dealt to this.
- `Bushido(n)` — +N/+N when blocking or blocked.
- `Rampage(n)` — +N/+N for each blocker past the first. Display-only; wire the behavior with the
  `card { rampage(n) }` builder helper, which adds this keyword ability plus a "becomes blocked"
  triggered ability granting `+n/+n × (blockers − 1)` until end of turn (mirrors `prowess()`).
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
- `Plot(cost)` — `KeywordAbility.plot(cost)`. Special action available during your main phase while the stack is empty: pay [cost] and exile the card from your hand. It becomes plotted (stamped with a `PlottedComponent`). On a later turn you may cast it from exile without paying its mana cost, as a sorcery (CR 718). Cast permission is granted via the engine's standard `MayPlayPermission` + `PlayWithoutPayingCostComponent`, gated by `Conditions.SourcePlottedOnPriorTurn`. No card-side wiring needed — declare the keyword ability on the card and the engine handles the rest.
- `Hideaway(n)` — `KeywordAbility.hideaway(n)`; display tag rendered "Hideaway N". Mechanic is composed manually via `MoveCollectionEffect(faceDown = true, linkToSource = true)` + `CardSource.FromLinkedExile()` — the keyword itself carries no engine behavior.
- `OptionalAdditionalCost(manaCost?, additionalCost?, multi, displayPrefix, branchesEffect, grantsFlashTiming)` — generalised "pay an optional extra cost while casting" primitive. Backs printed Kicker / Multikicker / Offspring **and** the pre-kicker "pay {N} more to cast as though it had flash" pattern (Ghitu Fire). When `branchesEffect = true` (default) paying the cost marks the spell so `WasKicked` fires for the card's own effect/triggers; when `false` the payment is invisible to `WasKicked` (used by `flashKicker`). When `grantsFlashTiming = true` paying the cost unlocks instant-speed casting in addition to whatever else it does. Prefer the factories: `KeywordAbility.kicker(cost)`, `KeywordAbility.kicker(additionalCost)`, `KeywordAbility.multikicker(cost)`, `KeywordAbility.offspring(cost)`, `KeywordAbility.flashKicker(cost)`. Serial name is `Kicker` for wire compatibility. **Kicker {X}** (variable kicker, e.g. `KeywordAbility.kicker("{X}")` on Verdeloth the Ancient): the kicked cast surfaces `hasXCost`/`maxAffordableX` so the client prompts for X exactly like a base-cost X spell; the chosen X is paid as part of the kicker and stamped onto `SpellOnStackComponent.xValue`, so the card's ETB trigger reads it via `DynamicAmount.XValue` ("create X tokens").
- `Impending(time, cost)` — `card { impending(n, cost) }` builder helper (CR 702.175, Duskmourn). A self-alternative
  cost: pay [cost] instead of the mana cost and the permanent enters with N **time counters**, isn't a creature until
  the last is removed, and loses one at the beginning of your end step. The helper wires everything from one call — the
  `KeywordAbility.Impending` alt-cost (display + cast enumeration), a `ConditionalStaticAbility(RemoveCardType("CREATURE"),
  Conditions.SourceHasCounter(TIME))` "isn't a creature while it has a time counter" static, and a `YourEndStep`
  triggered ability (gated by the same intervening-if) that removes a time counter. The engine places the N TIME counters
  when a spell cast for its impending cost resolves; casting for the normal mana cost adds no counters, so neither wiring
  fires (mirrors `prowess()` / `rampage()`).
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
- `TargetMatchesFilter(filter, targetIndex = 0)` — the context target matches a `GameObjectFilter`.
- `TargetSharesMostCommonColor(targetIndex = 0)` — the context target shares a color with the
  most common color among all permanents, or a color tied for most common. Tallies each of the
  five colors across every battlefield permanent (multicolored permanents count once per color,
  using projected colors), takes the highest tally, and checks whether the target has any color
  in that (possibly tied) most-common set. A board with no colored permanents is `false`. Used by
  Tsabo's Assassin.
- `ColorIsMostCommon(color)` — the self-gating sibling of the above: true when `color` is the most
  common color among all permanents, or tied for most common (same tally rules). Board-derived
  only — no targets/triggering/kicker — so it evaluates identically in resolution and in
  projection, which lets it gate a `ConditionalStaticAbility`. Used by the Invasion djinn cycle
  ("as long as [color] is the most common color among all permanents…" — Goham/Halam/Ruham/Sulam/Zanam).
- `YouHaveCitysBlessing` — you have City's Blessing (10+ permanents).
- `SourceIsRingBearer` — the source permanent is your Ring-bearer (CR 701.52e).

### Life & damage

- `LifeAtLeast(n, player?)` — player has ≥N life.
- `LifeAtMost(n, player?)` — player has ≤N life.
- `YouLostLife` — you lost life this turn.
- `OpponentLostLife` — an opponent lost life this turn.

### Cast / cost

- `WasCast` — source was cast (not put onto the stack).
- `WasCastFromHand` — cast specifically from hand.
- `WasCastFromZone(zone)` — cast from a specific zone.
- `WasKicked` — cast with kicker / multikicker / offspring (i.e. an `OptionalAdditionalCost` with `branchesEffect = true` whose extra cost was paid). FlashKicker payments are intentionally invisible to this condition.
- `BlightWasPaid(amount)` — the Blight X additional cost was paid.

### Source state

All "source matches X" conditions desugar to `SourceMatches(filter)` — a generic predicate
check against the source entity that works in both resolution and static-ability (projection)
contexts.

- `SourceMatches(filter)` — primitive: source entity matches a `GameObjectFilter`.
- `SourceIsAttacking` — source is attacking.
- `SourceIsBlocking` — source is blocking.
- `SourceIsTapped` — source is tapped.
- `SourceIsUntapped` — source is untapped.
- `SourceEnteredThisTurn` — source entered the battlefield this turn.
- `SourceHasDealtDamage` — source has dealt damage since entering the battlefield.
- `SourceHasDealtCombatDamageToPlayer` — saboteur-style payoff gate.
- `SourceIsModified` — has counters, attached Equipment, or controller-owned Aura
  attached (CR 700.4). Kept as a dedicated condition because the controller-of-Aura
  match isn't expressible via the generic `SourceMatches` machinery.
- `SourceHasSubtype(subtype)` — `SourceMatches(GameObjectFilter.Any.withSubtype(...))`;
  Changeling is honored.
- `SourceHasKeyword(keyword)` — `SourceMatches(GameObjectFilter.Any.withKeyword(...))`.
- `SourceHasCounter(counterType)` — `SourceMatches(GameObjectFilter.Any` with the
  corresponding `StatePredicate.HasCounter` / `HasAnyCounter`).

### Turn / phase

- `IsYourTurn` — it's your turn.
- `IsNotYourTurn` — it's an opponent's turn.
- `IsInPhase(phase)` — currently in `BEGINNING | MAIN | COMBAT | …`.

### Per-turn counts

All three are parameterised by a `Player` reference (default `Player.You`), so they
work in both resolution and static-ability (projection) contexts. The DSL helpers
default to "you" so card authors don't need to pass it explicitly.

- `YouAttackedWithCreaturesThisTurn(filter, atLeast)` — Raid/Battalion shape. Backed by
  `PlayerAttackedWithCreaturesThisTurn(Player.You, filter, atLeast)`.
- `YouCastSpellsThisTurn(atLeast, filter)` — Prowess/Magecraft shape. Backed by
  `PlayerCastSpellsThisTurn(Player.You, filter, atLeast)`.
- `YouHaveCitysBlessing` — Ascend gate. Backed by `PlayerHasCitysBlessing(Player.You)`.
- `IsFirstSpellPaidWithTreasureManaCastThisTurn` — gates a triggered ability to fire only
  on the first spell each turn that mana from a Treasure was spent to cast (Rain of
  Riches). Reads `CastSpellRecord.paidWithTreasureMana` on the per-player spell history.
- `PermanentTypeEnteredBattlefieldThisTurn(cardType, player = Player.You)` — true if a
  permanent of `cardType` entered the battlefield under `player`'s control at any point
  this turn. Pure ETB tracker: the permanent need not still be on the battlefield, still
  be of that type, or still be under the same controller — only the entry event matters
  (so Mechan Shieldmate's "as long as an artifact entered ... this turn" stays satisfied
  even if the artifact is destroyed before combat). Captured types are read from the
  *projected* state at the moment of entry, so a permanent that's an artifact via a
  continuous effect at ETB (Mycosynth Lattice, etc.) also counts. Backed by the per-player
  `PermanentTypesEnteredBattlefieldThisTurnComponent`, cleared by `CleanupPhaseManager` at
  end of turn. Every battlefield entry must go through `BattlefieldEntry.place` for this
  tracker to stay in sync. Shortcut: `Conditions.ArtifactEnteredBattlefieldThisTurn`.

### Composition

- `All(c1, c2, ...)` — AND.
- `Any(c1, c2, ...)` — OR.
- `Not(c)` — negate.
- `Compare(v1, op, v2)` — numeric comparison between `DynamicAmount`s.
- `Exists(player, zone, filter)` — at least one matching object exists.
- `FixedIfCondition(...)` — bake a condition into a static-ability gate.

### Static-ability vs resolution-time evaluation

Every `Condition` works in both contexts: at spell/trigger resolution (full
`EffectContext` — targets, kicker, triggering entity, etc.) and during state projection
inside a `ConditionalStaticAbility` (only the source entity and projected values are
known). The engine dispatches via a `ConditionEvaluationContext.Resolution` /
`Projection` sealed type — there is **no** separate `SourceProjectionCondition` arm.

Conditions that need resolution-only facts (e.g. `TargetMatchesFilter`, `TargetSharesMostCommonColor`, `TriggeringEntity*`,
`WasKicked`, `ManaSpentToCastIncludes`, `CollectionContainsMatch`) silently evaluate to
`false` under projection — a static-ability gate is never "in the middle of casting a spell".

Other gates available in both contexts:

- `ColorIsMostCommon(color)` — board-derived, so it gates a `ConditionalStaticAbility` directly
  (the Invasion djinns rely on this).
- `SourceChosenModeIs("id")` — gate on the chosen mode (Sieges / `EntersWithChoice`).
  Currently resolution-only; can be extended to projection if needed.

---

## 13. Dynamic amounts (`DynamicAmount.*`)

Numbers computed at resolution time.

### Math

- `Fixed(n)` — literal constant.
- `XValue` — the X chosen for the spell/ability.
- `TotalManaSpent` — total mana paid from the pool to cast the current spell (sum of every per-color
  bucket; for X spells the X portion is included). E.g. Memory Deluge "where X is the mana spent."
- `ManaSpentOnX(color)` — the amount of `{color}` mana spent on the `{X}` portion specifically, broken
  down by color. Used by payoffs that scale with how much of a color went into X — Soul Burn ("you gain
  life equal to the amount of black mana spent on X"). Pair with `xManaRestriction` (see below) so the X
  can only be paid with the relevant colors.
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
- `DynamicAmounts.targetManaSpent(index)` — sum of all `manaSpent{Color}` buckets on
  the targeted spell's `SpellOnStackComponent` (i.e. what was actually paid, after
  cost reductions/increases). Pair with `targetManaValue()` for "if the amount of
  mana spent to cast that spell was less than its mana value" gates (Unravel).
  Desugars to `EntityProperty(EntityReference.Target(index), EntityNumericProperty.ManaSpent)`.
  Returns 0 if the target isn't a spell on the stack.
- `CardNumericProperty(card, property)` — generic numeric property accessor.

### Triggering-entity shortcuts (`DynamicAmounts.*` facades)

For triggered abilities whose effect reads a property of the entity that caused the trigger
(rather than the source of the ability):

- `DynamicAmounts.triggeringPower()` — power of the triggering entity (e.g. Warstorm Surge:
  "it deals damage equal to its power").
- `DynamicAmounts.triggeringToughness()` — toughness of the triggering entity.
- `DynamicAmounts.triggeringManaValue()` — mana value of the triggering entity.

All three desugar to `EntityProperty(EntityReference.Triggering, …)`.

### Attached-creature shortcut (`DynamicAmounts.*` facade)

For Aura/Equipment abilities that read a property of the creature the source is attached to (rather
than the source permanent itself — for an Aura, `EntityReference.Source` is the Aura, not the creature):

- `DynamicAmounts.enchantedCreaturePower()` — power of the attached creature (e.g. Pain for All:
  "enchanted creature deals damage equal to its power"). Desugars to
  `EntityProperty(EntityReference.EnchantedCreature, EntityNumericProperty.Power)`. The
  `EnchantedCreature` reference resolves through the source's `AttachedToComponent` (state-aware), so it
  needs an effect context with a `sourceId`; it returns 0 in predicate/filter-only contexts that don't
  thread state. When read in a **triggered ability** and the attached creature has already left the
  battlefield by resolution (e.g. removed in response to the aura's ETB trigger), it falls back to the
  creature's last-known power — captured when the trigger fired — per CR 608.2g, rather than 0.

### Context-plumbed

- `ContextProperty(key)` — value plumbed via `EffectContext`. Keys include:
  - `TRIGGER_DAMAGE_AMOUNT` — damage in the current trigger payload (Tephraderm).
  - `TRIGGER_LIFE_GAINED` / `TRIGGER_LIFE_LOST` — life delta from a `LifeChangedEvent`.
  - `TRIGGER_COUNTERS_PLACED_AMOUNT` — counters placed in the triggering event (Simic Ascendancy).
  - `LAST_KNOWN_PLUS_ONE_COUNTER_COUNT` / `LAST_KNOWN_TOTAL_COUNTER_COUNT` — counters on the
    source as it last existed on the battlefield (Hooded Hydra / Shadow Urchin).
  - `ADDITIONAL_COST_EXILED_COUNT` / `ADDITIONAL_COST_BLIGHT_AMOUNT` — cost-step accumulators.
  - `TARGET_COUNT` — still-legal targets in the current effect context.
  - `LINKED_EXILE_CARD_COUNT` / `LINKED_EXILE_DISTINCT_CARD_TYPE_COUNT` — cards / distinct
    types in the source's linked exile pile (Veteran Survivor / Keen-Eyed Curator).
  - `MODES_CHOSEN_ON_TRIGGERING_SPELL` — number of mode picks recorded on the cast that fired
    the trigger (Riku of Many Paths). Counts selections, not distinct modes, so Spree with
    the same mode twice reads as `2`.
- `AdditionalCostBlightAmount` — X paid via the Blight additional cost.
- `ChosenNumber` — number a player chose via a Choose action.
- `VariableReference(name)` — named variable stored earlier by `StoreResult`/`StoreCount`.
- `ColorsAmongPermanents(player)` — count of distinct colors among player's permanents.

### `ManaColorSet`<a id="manacolorset"></a>

Color analogue of `DynamicAmount` — pure data resolved at the moment a mana effect fires.
Used by `AddManaOfChoice(colorSet, amount)`; the engine's `ManaColorSetResolver` materializes
a `Set<Color>` from the source/controller/projected state, the player picks one (or the
solver picks if there's only one), and that color is added to the pool.

- `ManaColorSet.AnyColor` — all five colors. The "any-color" default.
- `ManaColorSet.Specific(colors)` — hand-authored fixed set (e.g., `{R, G}` for a Gruul producer).
- `ManaColorSet.CommanderIdentity` — union of color identities of every commander the controller has registered. Empty (no mana produced) in non-Commander formats.
- `ManaColorSet.AmongPermanents(filter)` — colors of permanents matching `filter`, read via projected state so type/color-changing effects are honored. Mox Amber shape.
- `ManaColorSet.LandsCouldProduce(scope)` — colors any land in `scope` could produce; tapped state and activation costs are ignored (CR 106.7). `scope` is `LandControllerScope.{YOU, OPPONENTS, ANY}`. Fellwar Stone / Exotic Orchard / Reflecting Pool shape.
- `ManaColorSet.SourceChosenColor` — the single color stored on the source's `ChosenColorComponent` (set via `EntersWithChoice(ChoiceType.COLOR)`). Uncharted Haven / Ashling Rekindled shape.

### `ManaRestriction`

Spending restrictions attached to a unit of mana when it is added to the pool. Used by
`AddMana`, `AddColorlessMana`, and `AddManaOfChoice` (via the `restriction` parameter).
When the engine pays a spell's cost, restricted mana is consumed preferentially when its
restriction matches the spell context.

- `ManaRestriction.AnySpend` — no restriction; satisfies any spend. Used internally when
  `AddManaOfChoice(riders = ...)` is provided without an explicit restriction, so the rider
  set survives in the pool without limiting where the mana can be spent (Path of Ancestry).
- `ManaRestriction.InstantOrSorceryOnly` — only instants and sorceries.
- `ManaRestriction.KickedSpellsOnly` — only kicked spells.
- `ManaRestriction.CreatureSpellsOnly` / `CreatureMV4OrXCost` / `SpellsMV4OrGreater` —
  creature- or mana-value-gated.
- `ManaRestriction.SubtypeSpellsOrAbilitiesOnly(subtype, creatureOnly?)` — Cavern of Souls /
  Unclaimed Territory: only spells of a baked subtype, optionally creature-only.
- `ManaRestriction.CastFromExileOnly` — only spells cast from exile.
- `ManaRestriction.CardTypeSpellsOrAbilitiesOnly(cardType, allowSpells?, allowAbilities?)` —
  Steelswarm Operator shape.

### `ManaSpellRider`

Side-effects attached to mana that fire when the mana is spent on a spell. Orthogonal to
`ManaRestriction`: the restriction controls *where* the mana may be spent; the rider
controls *what happens to the spell* when it is spent. The cast pipeline either mutates the
spell directly (e.g. stamps a component) or queues a triggered ability onto the stack above
the spell when the rider needs the stack (typically because it requires a player decision).

- `ManaSpellRider.MakesSpellUncounterable` — Cavern of Souls: stamps `CantBeCounteredComponent`
  on the spell at cast time.
- `ManaSpellRider.ScryOnSharedTypeWithCommander(amount)` — Path of Ancestry: if the spell is
  a creature spell that shares a creature type with any of the controller's commanders,
  queues a `scry amount` triggered ability above the spell.

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

**Dynamic "choose up to X"** — `ModalEffect.chooseUpToDynamic(dynamicMax, *modes, allowRepeat = false)`
caps the pick count by a `DynamicAmount` evaluated at resolution time. `minChooseCount` is
forced to `0` (the player may always decline); `chooseCount` becomes `min(eval, modes.size)`.
If the evaluated cap is `0` the effect resolves as a no-op. Used by Riku of Many Paths,
where the cap is `ContextProperty(MODES_CHOSEN_ON_TRIGGERING_SPELL)`. Equivalent raw shape:
`ModalEffect(modes, chooseCount = modes.size, minChooseCount = 0, dynamicChooseCount = …)`.

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

**Other `ChoiceType`s** — `ChoiceType.COLOR` writes `ChosenColorComponent` (read by
`GrantChosenColor`), `ChoiceType.CREATURE_TYPE` writes `ChosenCreatureTypeComponent`,
`ChoiceType.CREATURE_ON_BATTLEFIELD` writes `ChosenCreatureComponent`, and
`ChoiceType.BASIC_LAND_TYPE` writes `ChosenLandTypeComponent` (read by
`SetEnchantedLandTypeFromChosen` and `GrantLandwalkOfChosenType`). Example — Phantasmal Terrain
("As this Aura enters, choose a basic land type. Enchanted land is the chosen type."):

```kotlin
auraTarget = Targets.Land
replacementEffect(EntersWithChoice(ChoiceType.BASIC_LAND_TYPE))
staticAbility { ability = SetEnchantedLandTypeFromChosen }
```

Traveler's Cloak grants landwalk of the chosen type to the enchanted creature instead:

```kotlin
auraTarget = Targets.Creature
replacementEffect(EntersWithChoice(ChoiceType.BASIC_LAND_TYPE))
staticAbility { ability = GrantLandwalkOfChosenType() }
```

### Other choice effects

- `ChooseActionEffect(choices)` — pick one effect from a list.
- `ChooseColorThenEffect(whenChosen)` — pick a color, then apply a function of the color.
- `ChooseColorAndGrantProtectionTo{Target,Group}Effect` — color → protection from that color.
- `GrantHexproofFromChosenColorEffect(target)` — same shape, hexproof.
- `ChooseCreatureTypeEffect(...)` — pause for creature-type selection.
- `Effects.ChooseCardName(storeAs, prompt?, excludeBasicLandNames?)` — name a card (`ChooseOptionEffect(OptionType.CARD_NAME)`); the chosen name is stored in `chosenValues[storeAs]`. Options are every registry card name (searchable list, not free text); `excludeBasicLandNames` drops the five basics. Match cards by it with `GameObjectFilter.namedFromVariable(storeAs)`. (Desperate Research)
- `Effects.StoreCardName(from, storeAs)` — capture the name of the first card in collection `from` into `chosenValues[storeAs]`. The "choose a card, then act on cards of that name" counterpart to `ChooseCardName`. (Lobotomy)
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

- `ReplacementEffect.PreventDamage(amount?, restrictions?, appliesTo)` — prevent damage matching the
  `GameEvent.DamageEvent` shape. `amount = null` prevents all; a number prevents up to that much.
  `restrictions: List<Condition>` (default empty) gates the prevention on extra conditions evaluated
  against the source's controller — the same pattern as `ModifyLifeLoss.restrictions`. Use it for
  "as long as …, prevent …" statics (Spirit of Resistance: a five-distinct-colors `Compare` gate).
- `CapDamage(maxAmount, appliesTo)` — clamp matching damage to `maxAmount` (a *replacement* distinct
  from prevent/modify; applied after all amplification). Divine Presence: `CapDamage(3, DamageEvent(recipient = Any))`.
- `RedirectDamage(redirectTo, appliesTo)` — redirect matching damage to another recipient. Now wired
  as a continuous static replacement (each source applies at most once per damage event). `redirectTo`
  supports `EffectTarget.ControllerOfDamageSource` (the controller of the damaging source),
  `Controller`/`Self` (the replacement's owner/controller), and `TargetController`. Harsh Judgment:
  redirect chosen-color instant/sorcery damage dealt to you back to the spell's controller.
- **DamageEvent filters (gap #7):** `GameEvent.DamageEvent(recipient, source, damageType, amount)`.
  `amount: AmountFilter` (`Any` / `AtMost(n)` / `AtLeast(n)` / `Exactly(n)`) gates on the would-be
  amount (Callous Giant: `AtMost(3)`). `source = SourceFilter.Matching(filter)` can carry relational
  predicates: `GameObjectFilter.sharingColorWithRecipient()` (`CardPredicate.SharesColorWithRecipient`,
  Well-Laid Plans — "another creature that shares a color") and `sharingChosenColorWithSource()`
  (`CardPredicate.SharesChosenColorWithSource`, reads the replacement source's `ChosenColorComponent`).
- `ReplacementEffect.EntersBattlefieldTappedUnless(condition)` — ETB tapped unless condition met.
- `ReplacementEffect.IfYouDoBranchEffect(...)` — branch on "if you do" replacement.
- `OnEnterRunEffect(effect)` — generic "as ~ enters the battlefield, run [effect]". The wrapped effect
  executes via the normal effect-executor pipeline at entry time (so `EffectTarget.Self` resolves to
  the entering permanent) and may pause for player input. Compose with atomic pausable effects like
  `Effects.MayRevealCardFromHand` to build SOI shadow lands or other "as ~ enters" choices.
  **Scope today:** only wired into the land-play path (`PlayLandHandler`). When the first non-land
  permanent needs this, also wire it into `StackResolver.enterPermanentOnBattlefield`.
- `EntersWithDevour(multiplier, sacrificeFilter, counterType, variant)` — Devour (CR 702.82) and its
  printed variants. As the permanent resolves from the stack, the controller is prompted to pick any
  number of their own permanents matching `sacrificeFilter`. Those permanents are sacrificed and the
  entering permanent gains `multiplier × count` counters of `counterType` (default `+1/+1`). Pair
  with `KeywordAbility.Devour(multiplier, sacrificeFilter, variant)` so the rules text renders. The
  `variant` parameter is a textual tag only — `""` for plain Devour, `"land"` for the EOE
  "Devour land N" wording. **Scope today:** only the stack-spell entry path is wired; reanimation and
  token entries skip Devour (which is fine for printed cards — Devour creatures all cost real mana to
  cast).
- `EntersAsCopy(optional, copyFilter, copyFromZone, filterByTotalManaSpent, additionalSubtypes, additionalKeywords, nameOverride, powerOverride, toughnessOverride, exileCopiedCard)` —
  "enter as a copy of …". As the permanent resolves, the controller picks an object matching
  `copyFilter` and the permanent enters as a copy (Rule 707 copiable values), with any overrides
  applied. `copyFromZone` selects the candidate pool: `Zone.BATTLEFIELD` (default — Clone, Clever
  Impersonator, Mockingbird) copies a permanent in play; `Zone.GRAVEYARD` copies a creature *card*
  from any graveyard (Superior Spider-Man) via the modal card-list overlay. `additionalSubtypes` /
  `additionalKeywords` are added "in addition to its other types"; `nameOverride` keeps a fixed name;
  `powerOverride` / `toughnessOverride` force base P/T; `exileCopiedCard` exiles the copied card after
  the copy ("When you do, exile that card"). `filterByTotalManaSpent` restricts copy targets to mana
  value ≤ total mana spent (Mockingbird). The copy snapshots a `CopyOfComponent` so it reverts to its
  printed identity when it leaves the battlefield (CR 400.7 / 707.2).
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
  `hour`, `energy`, `scry`, `aura`, `chapter`, `citation`, `rune`, `scar`, `crux`, `omen`, `secret`, `feather` —
  assorted printed counter kinds.

Counter effects live in §4 (`AddCounters`, `RemoveCounters`, `Proliferate`, `MoveAllLastKnownCounters`, etc.).

---

## 17. Zones & movement

**Zones** — `BATTLEFIELD`, `HAND`, `LIBRARY`, `GRAVEYARD`, `EXILE`, `STACK`.

**Primitives**

- `MoveToZoneEffect(target, zone, faceDown?, byDestruction?, linked?)` — single-target move.
- `MoveCollectionEffect(collectionName, zone, faceDown?, linkToSource?, asOwner?, likelyPosition?)` — pipeline move of a
  stored collection.
- `GatherCardsEffect(source, filter, into)` — pipeline gather from a zone into a named collection. `CardSource`
  variants include zones (`FromZone`, `FromMultipleZones`), battlefield queries (`BattlefieldMatching`,
  `ControlledPermanents`), linked exile (`FromLinkedExile`), tapped-as-cost (`TappedAsCost`), and the resolved
  spell/ability targets (`ChosenTargets`). The zone/library sources (`FromZone`, `FromMultipleZones`,
  `TopOfLibrary`) accept a multi-player `player` reference (`Player.Each`, `Player.ActivePlayerFirst`,
  `Player.EachOpponent`) and fan out across every relevant player's copy of the zone in a single gather —
  e.g. "all creature cards in each player's graveyard" (Bringer of the Last Gift). Pair with
  `MoveCollectionEffect(underOwnersControl = true)` to return each card to its owner.
- `CaptureControllersEffect(from, storeAs)` — snapshot each entity's current controller into a parallel
  `List<EntityId>` under `storedCollections[storeAs]`. Required when a later step needs "who controlled
  this card before it left the battlefield" — `ControllerComponent` is stripped on move-out.
- `ForEachCapturedControllerEffect(collection, originalCollection, controllerSnapshot, countVariable?, effects)` —
  cross-references a post-move `collection` against an `originalCollection` + parallel `controllerSnapshot` to
  build per-controller tallies, then runs `effects` once per controller (turn order from the active player). Each
  iteration sets `context.controllerId` to the controller (so `Player.You` / `EffectTarget.Controller` resolve to
  them) and writes the tally into `storedNumbers[countVariable]` (default `"iterationCount"`) for
  `DynamicAmount.VariableReference` to read. Outer `storedCollections` are preserved (unlike
  `ForEachPlayerEffect`). Used by Builder's Bane via the
  `GatherCards(ChosenTargets) → CaptureControllers → MoveCollection(Destroy, storeMovedAs) → ForEachCapturedController`
  shape.
- `ForEachInCollectionEffect(collection, effect)` — run `effect` once per entity in a named pipeline collection
  (snapshotted at resolution), with `pipeline.iterationTarget` set to that entity. Collection-based sibling of
  `ForEachInGroupEffect` (which iterates a battlefield filter): use it to apply a per-entity effect to a *chosen*
  set rather than a re-evaluated filter. Pair with a single-target effect on `EffectTarget.Self` — e.g.
  `ForEachInCollection(nonChosenPile, Effects.CantAttack(EffectTarget.Self))` gives each creature in a chosen pile
  its own snapshot can't-attack floating effect (Fight or Flight / Stand or Fall; creatures entering after the
  split are unaffected).
- `SelectFromCollectionEffect(from, into, selectCount?, allowZero?, alwaysPrompt?, restrictions?)` — let a player pick
  from a collection. `restrictions` (`List<SelectionRestriction>`) cap and trim the picks server-side: `OnePerCardType`,
  `OnePerColor(matchControllerPermanentColors?)`, `OnePerCardName`, `TotalManaValueAtMost(max)`, and
  `OnePerBasicLandType`. `OnePerBasicLandType` keeps at most one land of each basic land type (a kept land claims
  *every* basic type it has) and — unlike `OnePerColor`, where a colourless card is unconstrained — a land with no
  basic land type can't be kept at all (Global Ruin: "chooses a land of each basic land type, then sacrifices the
  rest"). Each restriction also exposes a boolean flag on `SelectCardsDecision` (`onePerBasicLandType`, …) so the UI
  can disable redundant picks.
  - `chooser` (`Chooser`, default `Controller`) — who makes the selection: `Controller`, `Opponent`, `TargetPlayer`
    (`context.targets[0]`), `TriggeringPlayer`, `SourceController` (the source's controller, ignoring per-iteration
    swaps), or `ControllerOfSelection` (the controller of the cards in `from` — resolved from the first card's
    projected controller). Use `ControllerOfSelection` for "their controller chooses…" where the deciding player is
    whoever controls the gathered cards and may be you or an opponent (Barrin's Spite: gather the two targeted
    creatures, their controller sacrifices one, the other is returned to hand). The same `chooser` set is accepted by
    `ChoosePileEffect`.

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
- `TheRingComponent` — you have the Ring emblem; `temptCount` gates its four abilities (CR 701.52).
- `RingBearerComponent` — designates a creature as a player's Ring-bearer (on the creature, not the player).
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
- **Plot (CR 718)** — `KeywordAbility.plot(cost)`. Engine wires a sorcery-speed `PlotEnumerator` + `PlotCardHandler`
  that pays the plot cost, exiles the card face-up from hand, stamps `PlottedComponent(controllerId, turnPlotted)` +
  `PlayWithoutPayingCostComponent`, and adds a permanent `MayPlayPermission` gated by `SourcePlottedOnPriorTurn`.
  The cast-from-exile path is the standard `MayPlayPermission` flow in `CastFromZoneEnumerator` — `permanent = true`
  keeps the grant alive across end-of-turn cleanup. Emits `CardPlottedEvent` / `ClientEvent.CardPlotted`.
- **Adventure (CR 715)** — `layout = ADVENTURE` + `cardFaces[0]` Adventure spell; DSL:
  `card { adventure("Name") { spell { … } } }`.
- **Hideaway N** — `KeywordAbility.hideaway(n)` (display, "Hideaway N") + `MoveCollectionEffect(faceDown = true,
  linkToSource = true)` + `CardSource.FromLinkedExile()`; no special engine plumbing needed.
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
- **The Ring / the Ring tempts you (CR 701.52)** — `Effects.TheRingTemptsYou(target = Controller)`: the player gets
  the Ring emblem (`TheRingComponent`, tempt-count tracked) and chooses a creature they control to become their
  Ring-bearer (`RingBearerComponent` designation). The emblem's four cumulative abilities are resolved by the engine,
  not card data: the bearer is made legendary in `StateProjector` and can't be blocked by greater power via
  `RingBearerCantBeBlockedByGreaterPowerRule`; the ≥2/≥3/≥4 triggered abilities are appended to the bearer by
  `TriggerAbilityResolver` (see `TheRingAbilities`). For card triggers/checks use `Triggers.RingTemptsYou`
  ("Whenever the Ring tempts you") and `Conditions.SourceIsRingBearer` ("if this is your Ring-bearer").
- **Amass [subtype] N (CR 701.47)** — `Effects.Amass(count, subtype = "Orc")` (fixed) or
  `Effects.Amass(amount, subtype)` (a `DynamicAmount`, for "amass Orcs X"). If the controller controls no Army
  creature, a 0/0 black `[subtype]` Army token is created first (composing `CreateTokenEffect`); then they put N
  +1/+1 counters on an Army they control (a `SelectCardsDecision` resolved by `AmassContinuation` picks which one
  when they control several) and that Army becomes the subtype if it isn't already. The counter/subtype back half
  lives in `AmassResolution`; counters route through `AddCountersEffect`, so placement replacements still apply.

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
