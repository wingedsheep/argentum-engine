package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.predicates.StatePredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * The one fluent builder surface for narrowing an object filter — "tapped", "you control",
 * "with flying", "mana value 3 or less".
 *
 * Implemented by [GameObjectFilter] itself and by the wrappers that carry one as their base —
 * [com.wingedsheep.sdk.scripting.filters.unified.TargetFilter] (adds a zone and target
 * exclusions) and [com.wingedsheep.sdk.scripting.filters.unified.GroupFilter] (adds a scope and
 * group exclusions) — so every builder reads the same on all three and returns the receiver's own
 * type: `TargetFilter.Creature.tapped()` is still a `TargetFilter`.
 *
 * Every builder here only *appends a predicate* to the wrapped [GameObjectFilter], through the
 * three primitives [withCardPredicate], [withStatePredicate] and [withControllerPredicate], which
 * in turn go through [mapObjectFilter]. Operations whose meaning depends on the wrapper — `and` /
 * `or` unions, `other()` (the wrapper's own "exclude the source"), zones and scopes — stay on the
 * type they belong to.
 *
 * Not serialized: this is authoring vocabulary only, and adds nothing to any card's compiled tree.
 */
interface ObjectFilterBuilder<out Self> {

    /** Rebuild the receiver with its underlying [GameObjectFilter] replaced by [transform]'s result. */
    fun mapObjectFilter(transform: (GameObjectFilter) -> GameObjectFilter): Self

    // =============================================================================
    // Fluent Builder Methods - Card Predicates
    // =============================================================================

    /** Add a color requirement */
    fun withColor(color: Color) = withCardPredicate(CardPredicate.HasColor(color))

    /** Match any of the specified colors (OR logic) */
    fun withAnyColor(vararg colors: Color) =
        withCardPredicate(CardPredicate.Or(colors.map { CardPredicate.HasColor(it) }))

    /** Exclude a color */
    fun notColor(color: Color) = withCardPredicate(CardPredicate.NotColor(color))

    /** Match the color chosen during the current effect's resolution (e.g. via ChooseColorThen). */
    fun withChosenColor() = withCardPredicate(CardPredicate.HasChosenColor)

    /** Restrict to monocolored objects (exactly one color). Colorless objects do not match. */
    fun monocolored() = withCardPredicate(CardPredicate.IsMonocolored)

    /** Add a subtype requirement */
    fun withSubtype(subtype: Subtype) = withCardPredicate(CardPredicate.HasSubtype(subtype))

    /** Add a subtype requirement by string */
    fun withSubtype(subtype: String) = withSubtype(Subtype(subtype))

    /** Match any of the specified subtypes (OR logic). */
    fun withAnySubtype(vararg subtypes: String) =
        withCardPredicate(CardPredicate.Or(subtypes.map { CardPredicate.HasSubtype(Subtype(it)) }))

    /** Exclude a subtype */
    fun notSubtype(subtype: Subtype) = withCardPredicate(CardPredicate.NotSubtype(subtype))

    /** Restrict to nonartifact objects ("nonartifact creature", the Terror template). */
    fun nonartifact() = withCardPredicate(CardPredicate.IsNonartifact)

    /** Exclude creatures ("noncreature artifact", e.g. Guardian Beast). */
    fun notCreature() = withCardPredicate(CardPredicate.Not(CardPredicate.IsCreature))

    /**
     * Restrict to spells/abilities on the stack that target at least one object matching
     * [subfilter]. Used for "an instant or sorcery spell that targets a creature" (Repartee —
     * Forum Necroscribe, Lecturing Scornmage) and "target spell that targets a land you control"
     * (Teferi's Response). Player targets are skipped (CR — they have no game-object filter).
     */
    fun targetsMatching(subfilter: GameObjectFilter) =
        withCardPredicate(CardPredicate.TargetsMatching(subfilter))

    /**
     * Restrict to activated/triggered abilities on the stack whose *source* (CR 113.7) matches
     * [subfilter] — "from a creature source" (Echo, Perceptive Prodigy), "from an artifact source"
     * (Scientist Supreme of A.I.M.). Read with last known information when the source has already
     * left the battlefield. See [CardPredicate.AbilitySourceMatches].
     */
    fun abilitySourceMatches(subfilter: GameObjectFilter) =
        withCardPredicate(CardPredicate.AbilitySourceMatches(subfilter))

    /**
     * Add an arbitrary [CardPredicate] requirement. General-purpose combinator for predicates that
     * don't have a dedicated helper — e.g. `GameObjectFilter.Nonland.withCardPredicate(
     * CardPredicate.HasActivatedAbility)` for The Enigma Jewel's craft materials.
     */
    fun withCardPredicate(predicate: CardPredicate): Self =
        mapObjectFilter { it.copy(cardPredicates = it.cardPredicates + predicate) }

    /** Add a keyword requirement */
    fun withKeyword(keyword: Keyword) = withCardPredicate(CardPredicate.HasKeyword(keyword))

    /** Exclude a keyword */
    fun withoutKeyword(keyword: Keyword) = withCardPredicate(CardPredicate.NotKeyword(keyword))

    /** Match by exact card name */
    fun named(name: String) = withCardPredicate(CardPredicate.NameEquals(name))

    /** Match cards whose name is **not** [name] — e.g. "creatures … that don't have the same name". */
    fun notNamed(name: String) = withCardPredicate(CardPredicate.Not(CardPredicate.NameEquals(name)))

    /** Match cards whose name equals the card name stored in chosenValues[variableName] */
    fun namedFromVariable(variableName: String) =
        withCardPredicate(CardPredicate.NameEqualsChosen(variableName))

    /**
     * Match Room cards whose name isn't shared with a Room the evaluating player controls
     * (CR 709, by unlocked door names) — "a Room card that doesn't have the same name as a
     * Room you control" (Central Elevator).
     */
    fun nameNotSharedWithControlledRoom() = withCardPredicate(CardPredicate.NameNotSharedWithControlledRoom)

    /** Match permanents whose name isn't shared with a token the evaluating player controls. */
    fun nameNotSharedWithControlledToken() = withCardPredicate(CardPredicate.NameNotSharedWithControlledToken)

    /**
     * Match permanents whose name isn't shared with *another* permanent the evaluating player
     * controls — "that doesn't have the same name as another permanent you control" (Yenna,
     * Redtooth Regent). The candidate itself is excluded from the comparison; see
     * [CardPredicate.NameNotSharedWithAnotherControlledPermanent].
     */
    fun nameNotSharedWithAnotherControlledPermanent() =
        withCardPredicate(CardPredicate.NameNotSharedWithAnotherControlledPermanent)

    /**
     * Match cards whose name equals the name durably chosen by the *source permanent* as it
     * entered (its [com.wingedsheep.engine.state.components.battlefield.CastChoicesComponent] under
     * [slot]). Static-projection / activation-legality safe — use this in static-ability filters
     * (Petrified Hamlet), where [namedFromVariable] would fail closed.
     */
    fun namedFromChosenComponent(slot: ChoiceSlot = ChoiceSlot.CARD_NAME) =
        withCardPredicate(CardPredicate.NameEqualsChosenComponent(slot))

    /**
     * Match cards whose *card type* equals the card type durably chosen by the *source permanent*
     * as it entered (its [com.wingedsheep.engine.state.components.battlefield.CastChoicesComponent]
     * under [slot]). Static-projection / cost-calculation safe — use this in cost-static filters
     * (Arachne, Psionic Weaver's "spells of the chosen type cost {1} more").
     */
    fun ofChosenCardTypeComponent(slot: ChoiceSlot = ChoiceSlot.CARD_TYPE) =
        withCardPredicate(CardPredicate.CardTypeEqualsChosenComponent(slot))

    /** Mana value equals */
    fun manaValue(value: Int) = withCardPredicate(CardPredicate.ManaValueEquals(value))

    /** Mana value at most */
    fun manaValueAtMost(max: Int) = withCardPredicate(CardPredicate.ManaValueAtMost(max))

    /** Mana value at most the X chosen for the source spell/ability */
    fun manaValueAtMostX() = withCardPredicate(CardPredicate.ManaValueAtMostX)

    /** Mana value exactly equal to the number chosen for the source spell/ability (Void) */
    fun manaValueEqualsX() = withCardPredicate(CardPredicate.ManaValueEqualsX)

    /** Mana value at least */
    fun manaValueAtLeast(min: Int) = withCardPredicate(CardPredicate.ManaValueAtLeast(min))

    /** Mana value at most that of a referenced entity (triggering, source, etc.) */
    fun manaValueAtMostEntity(reference: EffectTarget.SingleEntity) =
        withCardPredicate(CardPredicate.ManaValueAtMostEntity(reference))

    /** Mana value at most the mana actually spent to cast a referenced entity (source, etc.) */
    fun manaValueAtMostEntityManaSpent(reference: EffectTarget.SingleEntity) =
        withCardPredicate(CardPredicate.ManaValueAtMostEntityManaSpent(reference))

    /** Mana value at most the number of colors of mana spent to cast a referenced entity (Converge). */
    fun manaValueAtMostColorsSpent(reference: EffectTarget.SingleEntity) =
        withCardPredicate(CardPredicate.ManaValueAtMostColorsSpent(reference))

    /**
     * Mana value at most a resolved [DynamicAmount] (e.g. "X or less, where X is the life you gained
     * this turn"). On a target filter the cap is re-read both when targets are chosen and again on
     * resolution (CR 608.2b) — Spellstutter Sprite.
     */
    fun manaValueAtMostDynamic(amount: DynamicAmount) =
        withCardPredicate(CardPredicate.ManaValueAtMostDynamic(amount))

    /**
     * Mana value **exactly** a resolved [DynamicAmount] — "a creature card with mana value equal to
     * the number of harmony counters on this artifact" (Instrument of the Bards).
     *
     * The equality sibling of [manaValueAtMostDynamic]. Oracle marks the difference with the word in
     * front of the clause rather than after it: "equal to …" is this, "less than or equal to …" is
     * the cap.
     */
    fun manaValueEqualsDynamic(amount: DynamicAmount) =
        withCardPredicate(CardPredicate.ManaValueEqualsDynamic(amount))

    /** Mana value is even (zero is even). */
    fun manaValueIsEven() = withCardPredicate(CardPredicate.ManaValueIsEven)

    /** Mana value is odd. */
    fun manaValueIsOdd() = withCardPredicate(CardPredicate.ManaValueIsOdd)

    /** Printed mana cost contains an {X} symbol (Paradox Surveyor). */
    fun hasXInManaCost() = withCardPredicate(CardPredicate.HasXInManaCost)

    /**
     * Printed mana cost contains at least [min] mana symbols of [colors] — "with one or more blue
     * mana symbols in its mana cost" (Namor the Sub-Mariner), or every colour at `min = 3` for
     * Omnath, Locus of All's "three or more colored mana symbols in its mana cost". Hybrid and
     * Phyrexian pips count for their colour(s); this is the printed cost, not the object's colour
     * (see [CardPredicate.ColoredManaSymbolsAtLeast]).
     *
     * Colours are varargs to match the amount-side facade
     * [com.wingedsheep.sdk.dsl.DynamicAmounts.coloredManaSymbolsOf], so the gate and the count
     * read the same way at a card's two call sites; [min] follows them and must be named.
     */
    fun coloredManaSymbolsAtLeast(vararg colors: Color, min: Int = 1) =
        withCardPredicate(CardPredicate.ColoredManaSymbolsAtLeast(colors.toList(), min))

    /** Power equals */
    fun power(value: Int) = withCardPredicate(CardPredicate.PowerEquals(value))

    /** Power exactly equal to the X chosen for the source spell/ability (Ent-Draught Basin) */
    fun powerEqualsX() = withCardPredicate(CardPredicate.PowerEqualsX)

    /** "with base power [value]" — see [CardPredicate.BasePowerEquals]. */
    fun basePower(value: Int) = withCardPredicate(CardPredicate.BasePowerEquals(value))

    /** "with base toughness [value]" — see [CardPredicate.BaseToughnessEquals]. */
    fun baseToughness(value: Int) = withCardPredicate(CardPredicate.BaseToughnessEquals(value))

    /** "with base power or toughness [value]" (Sword of the Squeak) — either half qualifies. */
    fun basePowerOrToughness(value: Int) = withCardPredicate(
        CardPredicate.Or(listOf(CardPredicate.BasePowerEquals(value), CardPredicate.BaseToughnessEquals(value)))
    )

    /** Power at most */
    fun powerAtMost(max: Int) = withCardPredicate(CardPredicate.PowerAtMost(max))

    /** Power at least */
    fun powerAtLeast(min: Int) = withCardPredicate(CardPredicate.PowerAtLeast(min))

    /** Power at least the X chosen for the source spell/ability (Expel the Interlopers). */
    fun powerAtLeastX() = withCardPredicate(CardPredicate.PowerAtLeastX)

    /** Power strictly greater than the projected power of a referenced entity (source, triggering, etc.) */
    fun powerGreaterThanEntity(reference: EffectTarget.SingleEntity) =
        withCardPredicate(CardPredicate.PowerGreaterThanEntity(reference))

    /** Power less than or equal to the projected power of a referenced entity (source, triggering, etc.) */
    fun powerLessThanEntity(reference: EffectTarget.SingleEntity) =
        withCardPredicate(CardPredicate.PowerLessThanEntity(reference))

    /** An Aura card whose enchant restriction the referenced permanent satisfies (Auratouched Mage). */
    fun couldEnchant(reference: EffectTarget.SingleEntity) =
        withCardPredicate(CardPredicate.CouldEnchant(reference))

    fun powerAtMostEntity(reference: EffectTarget.SingleEntity) =
        withCardPredicate(CardPredicate.PowerAtMostEntity(reference))

    /** Projected power strictly greater than the object's own base (printed) power. */
    fun powerGreaterThanBase() = withCardPredicate(CardPredicate.PowerGreaterThanBase)

    /** Toughness at most */
    fun toughnessAtMost(max: Int) = withCardPredicate(CardPredicate.ToughnessAtMost(max))

    /** Toughness at most the X chosen for the source spell/ability. */
    fun toughnessAtMostX() = withCardPredicate(CardPredicate.ToughnessAtMostX)

    /** Toughness at least */
    fun toughnessAtLeast(min: Int) = withCardPredicate(CardPredicate.ToughnessAtLeast(min))

    /** Power or toughness at least */
    fun powerOrToughnessAtLeast(min: Int) = withCardPredicate(CardPredicate.PowerOrToughnessAtLeast(min))

    /** Power or toughness at most */
    fun powerOrToughnessAtMost(max: Int) = withCardPredicate(CardPredicate.PowerOrToughnessAtMost(max))

    /** Total power and toughness (sum) at most */
    fun totalPowerAndToughnessAtMost(max: Int) =
        withCardPredicate(CardPredicate.TotalPowerAndToughnessAtMost(max))

    /** Toughness strictly greater than power */
    fun toughnessGreaterThanPower() = withCardPredicate(CardPredicate.ToughnessGreaterThanPower)

    /** Must be legendary */
    fun legendary() = withCardPredicate(CardPredicate.IsLegendary)

    /** Must not be legendary */
    fun nonlegendary() = withCardPredicate(CardPredicate.IsNonlegendary)

    /** Must not be a basic land ("nonbasic land", e.g. Rocket Volley, Shivan Harvest). */
    fun nonbasic() = withCardPredicate(CardPredicate.Not(CardPredicate.IsBasicLand))

    /** Must not be a token */
    fun nontoken() = withCardPredicate(CardPredicate.IsNontoken)

    /**
     * Must be *originally printed* in the given set (canonical set code, case-insensitive) —
     * reprints still match their original set. Models "a name originally printed in the [set]
     * expansion" (Golgothian Sylex → "ATQ"; City in a Bottle → "ARN").
     */
    fun originallyPrintedInSet(setCode: String) =
        withCardPredicate(CardPredicate.OriginallyPrintedInSet(setCode))

    /** Must be a token */
    fun token() = withCardPredicate(CardPredicate.IsToken)

    /** Must not be of the creature type chosen on the source permanent */
    fun notOfSourceChosenType() = withCardPredicate(CardPredicate.NotOfSourceChosenType)

    /** Must have a subtype matching the value stored in chosenValues[variableName] */
    fun withSubtypeFromVariable(variableName: String) =
        withCardPredicate(CardPredicate.HasSubtypeFromVariable(variableName))

    /** Must have a subtype matching any value in storedStringLists[listName] */
    fun withSubtypeInStoredList(listName: String) =
        withCardPredicate(CardPredicate.HasSubtypeInStoredList(listName))

    /**
     * Must have **none** of the subtypes in storedStringLists[listName] — "destroy all creatures that
     * aren't of a type chosen this way" (Harsh Mercy). The negation of [withSubtypeInStoredList].
     */
    fun withoutSubtypeInStoredList(listName: String) =
        withCardPredicate(CardPredicate.Not(CardPredicate.HasSubtypeInStoredList(listName)))

    /**
     * Must share at least one subtype with every group in the stored subtype list
     * `pipeline.storedSubtypeGroups[groupName]`. Pair with
     * [com.wingedsheep.sdk.scripting.effects.GatherSubtypesEffect] to populate the
     * stored groups from an entity collection.
     */
    fun withSubtypeInEachStoredGroup(groupName: String) =
        withCardPredicate(CardPredicate.HasSubtypeInEachStoredGroup(groupName))

    /** Must have any one of the given subtypes (OR logic) */
    fun withAnyOfSubtypes(subtypes: List<Subtype>) =
        withCardPredicate(CardPredicate.HasAnyOfSubtypes(subtypes))

    /**
     * Must have none of the given subtypes (e.g. "non-outlaw creature" for a
     * group of outlaw subtypes). Composes [CardPredicate.Not] over
     * [CardPredicate.HasAnyOfSubtypes], so it reuses existing evaluator support.
     */
    fun notAnyOfSubtypes(subtypes: List<Subtype>) =
        withCardPredicate(CardPredicate.Not(CardPredicate.HasAnyOfSubtypes(subtypes)))

    /** Must have the subtype chosen on the source permanent */
    fun withChosenSubtype() = withCardPredicate(CardPredicate.HasChosenSubtype)

    /** Must include the color chosen on the source permanent (CastChoicesComponent) */
    fun sharingChosenColorWithSource() = withCardPredicate(CardPredicate.SharesChosenColorWithSource)

    /** Must share a creature type with the referenced entity */
    fun sharingCreatureTypeWith(entity: EffectTarget.SingleEntity) =
        withCardPredicate(CardPredicate.SharesCreatureTypeWith(entity))

    /**
     * Must share a card type with **any** card exiled with the filtering ability's source — "shares
     * a card type with a card exiled with this creature" (Cemetery Illuminator). The pile-wide form
     * of [sharingCardTypeWith]`(EffectTarget.LinkedExiledCard())`; see
     * [CardPredicate.SharesCardTypeWithLinkedExile].
     */
    fun sharingCardTypeWithLinkedExile() = withCardPredicate(CardPredicate.SharesCardTypeWithLinkedExile)

    /**
     * Must share a **name** with any card exiled with the filtering ability's source — "spells with
     * the same name as a card exiled with Circu" (Circu, Dimir Lobotomist). The name axis of
     * [sharingCardTypeWithLinkedExile]; see [CardPredicate.SharesNameWithLinkedExile].
     */
    fun sharingNameWithLinkedExile() = withCardPredicate(CardPredicate.SharesNameWithLinkedExile)

    /**
     * Must share a **card type** with the referenced entity (Confusion in the Ranks: "target
     * permanent another player controls that shares a card type with it"). The card-type sibling
     * of [sharingCreatureTypeWith] — see [CardPredicate.SharesCardTypeWith] for why the two axes
     * stay apart.
     */
    fun sharingCardTypeWith(entity: EffectTarget.SingleEntity) =
        withCardPredicate(CardPredicate.SharesCardTypeWith(entity))

    /** Must share a color with the referenced entity */
    fun sharingColorWith(entity: EffectTarget.SingleEntity) =
        withCardPredicate(CardPredicate.SharesColorWith(entity))

    /**
     * Must have the same name as the referenced entity (Extraplanar Lens: "a land with the same
     * name as the exiled card"). The entity-referencing counterpart of
     * [sharingNameWithPermanentYouControl].
     */
    fun sharingNameWith(entity: EffectTarget.SingleEntity) =
        withCardPredicate(CardPredicate.SharesNameWith(entity))

    /** Must have the same mana value as the referenced entity */
    fun sharingManaValueWith(entity: EffectTarget.SingleEntity) =
        withCardPredicate(CardPredicate.SharesManaValueWith(entity))

    /**
     * Must share a color with the recipient of the in-flight damage (and not be that
     * recipient). Only meaningful in a damage replacement's source filter. Used by
     * Well-Laid Plans.
     */
    fun sharingColorWithRecipient() = withCardPredicate(CardPredicate.SharesColorWithRecipient)

    /**
     * Must share a color with at least one permanent the evaluating player controls matching
     * [filter] (Ringsight: "a card that shares a color with a legendary creature you control").
     */
    fun sharingColorWithPermanentYouControl(filter: GameObjectFilter) =
        withCardPredicate(CardPredicate.SharesColorWithPermanentYouControl(filter))

    /**
     * Must have the same name as at least one permanent the evaluating player controls matching
     * [filter] (Key to the Side-Door: "a legendary card with the same name as a legendary permanent
     * you control").
     */
    fun sharingNameWithPermanentYouControl(filter: GameObjectFilter) =
        withCardPredicate(CardPredicate.SharesNameWithPermanentYouControl(filter))

    /**
     * Must share **no** creature type with any permanent the evaluating player controls matching
     * [filter] (Radagast the Brown: "a creature card that doesn't share a creature type with a
     * creature you control").
     */
    fun notSharingCreatureTypeWithPermanentYouControl(filter: GameObjectFilter) =
        withCardPredicate(CardPredicate.DoesNotShareCreatureTypeWithPermanentYouControl(filter))

    /**
     * Must share **no** land type with any permanent the evaluating player controls matching
     * [filter] (Hiveheart Shaman: "a basic land card that doesn't share a land type with a land
     * you control").
     */
    fun notSharingLandTypeWithPermanentYouControl(filter: GameObjectFilter) =
        withCardPredicate(CardPredicate.DoesNotShareLandTypeWithPermanentYouControl(filter))

    // =============================================================================
    // Fluent Builder Methods - State Predicates
    // =============================================================================

    /**
     * Add an arbitrary [StatePredicate] requirement — the state-axis twin of [withCardPredicate],
     * and the primitive every state builder below appends through.
     */
    fun withStatePredicate(predicate: StatePredicate): Self =
        mapObjectFilter { it.copy(statePredicates = it.statePredicates + predicate) }

    /** Must be tapped */
    fun tapped() = withStatePredicate(StatePredicate.IsTapped)

    /** Must be untapped */
    fun untapped() = withStatePredicate(StatePredicate.IsUntapped)

    /** Must be prepared (Secrets of Strixhaven prepare). */
    fun prepared() = withStatePredicate(StatePredicate.IsPrepared)

    /** Must be a Room with at least one locked door (CR 709.5c). */
    fun hasLockedDoor() = withStatePredicate(StatePredicate.HasLockedDoor)

    /**
     * Must be on the battlefield right now. Pair with a predicate that carries a last-known-info
     * fallback to force a live reading — `onBattlefield().attacking()` is "is attacking", where a
     * bare `attacking()` is "is or was attacking" for an object that has already left.
     */
    fun onBattlefield() = withStatePredicate(StatePredicate.IsOnBattlefield)

    /**
     * Must currently be in [zone] — [StatePredicate.InZone]. `currentlyIn(Zone.STACK)` is "a spell":
     * the damage-source reading of "a spell you control" (Hostility).
     */
    fun currentlyIn(zone: Zone) = withStatePredicate(StatePredicate.InZone(zone))

    /** Must be attacking */
    fun attacking() = withStatePredicate(StatePredicate.IsAttacking)

    /** Must be attacking, with no other creature attacking (CR 506.5). */
    fun attackingAlone() = withStatePredicate(StatePredicate.IsAttackingAlone)

    /**
     * Must be attacking one of *your* opponents — the player themselves, so an attacker pointed at
     * an opponent's planeswalker or battle doesn't match. Narrower than [attacking]; "you" is the
     * controller of whatever ability applies the filter.
     */
    fun attackingAnOpponent() = withStatePredicate(StatePredicate.IsAttackingAnOpponent)

    /**
     * The defender-side mirror of [attackingAnOpponent]: must be attacking *you* or a planeswalker
     * *you* control (Tomik, Wielder of Law). "You" is the controller of whatever ability applies
     * the filter, so this matches regardless of who controls the attacker. Battles are excluded.
     */
    fun attackingYouOrYourPlaneswalkers() =
        withStatePredicate(StatePredicate.IsAttackingYouOrYourPlaneswalkers)

    /**
     * Must be attacking the player the filtering ability's source is attached to — "creatures
     * attacking enchanted player" (Curse of Hospitality). The attachment-scoped sibling of
     * [attackingAnOpponent]; only meaningful on an Aura that enchants a player.
     */
    fun attackingEnchantedPlayer() = withStatePredicate(StatePredicate.IsAttackingEnchantedPlayer)

    /**
     * Must have been declared as an attacker at least once during the current turn.
     * Survives leaving combat; cleared at end-of-turn cleanup.
     */
    fun attackedThisTurn() = withStatePredicate(StatePredicate.AttackedThisTurn)

    /** Was **not** declared as an attacker at any point this turn. Negation of [attackedThisTurn]. */
    fun didntAttackThisTurn() = withStatePredicate(StatePredicate.Not(StatePredicate.AttackedThisTurn))

    /**
     * Could **not** have been declared as an attacker this turn — its controller wasn't the one
     * declaring attackers, no Declare Attackers Step happened at all, or it has defender, can't
     * attack, or is summoning sick. "Except for creatures that couldn't attack" (Season of the
     * Witch). See [StatePredicate.CouldNotHaveAttackedThisTurn] for exactly what it covers.
     */
    fun couldNotHaveAttackedThisTurn() = withStatePredicate(StatePredicate.CouldNotHaveAttackedThisTurn)

    /** The complement: the creature *could* have been declared as an attacker this turn. */
    fun couldHaveAttackedThisTurn() =
        withStatePredicate(StatePredicate.Not(StatePredicate.CouldNotHaveAttackedThisTurn))

    /**
     * Was declared as an attacker during its controller's **most recent own turn** — the one-turn-back
     * sibling of [attackedThisTurn]. False on the turn it actually attacked, true on the next one.
     */
    fun attackedLastTurn() = withStatePredicate(StatePredicate.AttackedLastTurn)

    /** Was declared as a blocker at least once this turn (CR 509.1). */
    fun blockedThisTurn() = withStatePredicate(StatePredicate.BlockedThisTurn)

    /**
     * Was declared as an attacker at least once during the current combat (CR 508.1). Backed by a
     * per-entity marker stamped at attacker-declaration time; cleared when the combat phase ends.
     * Survives removal from combat. Pair with [blockedThisCombat] for "attacked or blocked this combat".
     */
    fun attackedThisCombat() = withStatePredicate(StatePredicate.AttackedThisCombat)

    /**
     * Was declared as a blocker at least once during the current combat (CR 509.1). Backed by a
     * per-entity marker stamped at blocker-declaration time; cleared when the combat phase ends.
     * Survives the blocked attacker dying (which clears the live `BlockingComponent`).
     */
    fun blockedThisCombat() = withStatePredicate(StatePredicate.BlockedThisCombat)

    /**
     * Must have become tapped **exactly once so far this turn** — the live reading of "if it's the
     * first time that creature has become tapped this turn" (Captain America, Living Legend), backed
     * by [StatePredicate.BecameTappedOnlyOnceThisTurn].
     *
     * Answers from the permanent's current tap count, so it is the half of that printed intervening
     * "if" that CR 603.4 re-checks at resolution; the trigger-time half rides on the tap event as
     * `Triggers.self.becomesTapped(firstTimeEachTurn = true)`. `Conditions
     * .TriggeringPermanentBecameTappedOnlyOnceThisTurn` is this predicate aimed at the triggering
     * permanent.
     */
    fun becameTappedOnlyOnceThisTurn() = withStatePredicate(StatePredicate.BecameTappedOnlyOnceThisTurn)

    /**
     * Must have had one or more counters put on it this turn — the counter-history counterpart of
     * [wasDealtDamageThisTurn]. Recorded at placement time, so it survives the counters being removed
     * again; cleared at end-of-turn cleanup.
     *
     * [counterType] scopes it to one kind ("one or more **+1/+1** counters") and
     * [placedByController] to counters the permanent's own controller put on ("**you've** put").
     * Used by Kid Loki's "each creature you control that you've put one or more +1/+1 counters on
     * this turn"; `Conditions.SourceReceivedCounterThisTurn` is this predicate under `SourceMatches`.
     */
    fun receivedCounterThisTurn(
        counterType: CounterType? = null,
        placedByController: Boolean = false
    ) = withStatePredicate(StatePredicate.ReceivedCounterThisTurn(counterType, placedByController))

    /**
     * Must have been dealt damage this turn — the **passive** voice (damage *received*), marked-damage
     * *history* rather than current marked damage. Survives damage removal / leaving combat; cleared at
     * end-of-turn cleanup. Used by "...that was dealt damage this turn" (Rooftop Assassin, Unsparing
     * Boltcaster). The active-voice counterpart is [hasDealtDamageThisTurn].
     */
    fun wasDealtDamageThisTurn() = withStatePredicate(StatePredicate.WasDealtDamageThisTurn)

    /** Matches names recorded when a spell was cast this turn, regardless of its caster or current zone. */
    fun sharesNameWithSpellCastThisTurn() = withStatePredicate(StatePredicate.SharesNameWithSpellCastThisTurn)

    /**
     * Must have **dealt** damage this turn — the active voice, and the mirror of
     * [wasDealtDamageThisTurn]. Combat and noncombat damage both count, to any recipient. Used by
     * "target creature an opponent controls that dealt damage this turn" (Red Guardian,
     * Super-Soldier).
     *
     * Named `hasDealtDamageThisTurn`, not the shorter `dealtDamageThisTurn`, deliberately: the short
     * name used to mean the *passive* predicate above. Retiring it outright turns any un-rebased
     * caller into a compile error at the exact line instead of a silent flip to the opposite set of
     * permanents. It also pairs with [hasDealtDamage], the same predicate's lifetime window.
     */
    fun hasDealtDamageThisTurn() = withStatePredicate(StatePredicate.HasDealtDamage(thisTurnOnly = true))

    /**
     * Must have dealt damage at least once since entering the battlefield — [hasDealtDamageThisTurn]
     * widened to the permanent's whole lifetime as the current object. `Conditions.SourceHasDealtDamage`
     * is this predicate under `SourceMatches` (Karakyk Guardian).
     */
    fun hasDealtDamage() = withStatePredicate(StatePredicate.HasDealtDamage())

    /**
     * Must have dealt *combat* damage (to any recipient) at least once since entering the
     * battlefield — [hasDealtDamage] narrowed to combat damage. `Conditions.SourceHasDealtCombatDamage`
     * is this predicate under `SourceMatches` (Ruric Thar, Magecrusher).
     */
    fun hasDealtCombatDamage() = withStatePredicate(StatePredicate.HasDealtDamage(combatOnly = true))

    /**
     * Must be in the same combat band as the effect's source (the source itself, or a band-mate
     * sharing its band id — CR 702.22). Source-relative; only matches while the source attacks.
     */
    fun inSameBandAsSource() = withStatePredicate(StatePredicate.InSameBandAsSource)

    /**
     * Must have dealt combat damage *this turn* to the player who controls the effect's source.
     * Source-relative; cleared at end-of-turn cleanup. Used by "each opponent sacrifices a
     * creature ... that dealt combat damage to you this turn" (Witch-king of Angmar).
     */
    fun dealtCombatDamageToSourceControllerThisTurn() =
        withStatePredicate(StatePredicate.DealtCombatDamageToSourceControllerThisTurn)

    /**
     * Must be controlled — right now — by a player the effect's *source* dealt combat damage to
     * this turn. Mirror of [dealtCombatDamageToSourceControllerThisTurn]; source-relative and
     * cleared at end-of-turn cleanup. Used by "destroy each nonland permanent … whose controller
     * was dealt combat damage by this creature this turn" (Steel Hellkite).
     */
    fun controllerDealtCombatDamageBySourceThisTurn() =
        withStatePredicate(StatePredicate.ControllerDealtCombatDamageBySourceThisTurn)

    /**
     * Must have been dealt damage this turn by the effect's source. Source-relative; stops matching
     * once the source leaves the battlefield. Used by "if a creature dealt damage by this creature
     * this turn would die, exile it instead" (Frostwielder, Kumano).
     */
    fun wasDealtDamageBySourceThisTurn() =
        withStatePredicate(StatePredicate.WasDealtDamageBySourceThisTurn)

    /**
     * The candidate's controller controls at least one permanent matching [subfilter] — Seasinger's
     * "target creature whose controller controls an Island". The subfilter's "you" is the
     * *candidate's* controller, not the ability's.
     */
    fun controllerControls(subfilter: GameObjectFilter) =
        withStatePredicate(StatePredicate.ControllerControls(subfilter))

    /** Must be blocking */
    fun blocking() = withStatePredicate(StatePredicate.IsBlocking)

    /**
     * Must be an attacker with blocked status, even after its last blocker leaves combat.
     * Leaving combat or an explicit unblock effect ends that status (CR 509.1h).
     * See [StatePredicate.IsBlocked].
     */
    fun blocked() = withStatePredicate(StatePredicate.IsBlocked)

    /** Must be an attacker with unblocked status after blockers are declared. See [StatePredicate.IsUnblocked]. */
    fun unblocked() = withStatePredicate(StatePredicate.IsUnblocked)

    /**
     * Must be blocking the effect's source (CR 509). Source-relative; only matches the source's
     * own blockers. "Whenever this becomes blocked, it deals N damage to each creature blocking it."
     */
    fun blockingSource() = withStatePredicate(StatePredicate.IsBlockingSource)

    /**
     * Must be blocking the effect's source **or** blocked by it (CR 509), read live from combat
     * state. Source-relative. "Each creature blocking or blocked by this creature" (Spitting Slug).
     */
    fun blockingOrBlockedBySource() = withStatePredicate(StatePredicate.IsCombatPairedWithSource)

    /**
     * Blocking the entity the enclosing `ForEachInGroup` is iterating over — Tidal Flats'
     * "creatures you control blocking that creature".
     */
    fun blockingIterationEntity() = withStatePredicate(StatePredicate.IsBlockingIterationEntity)

    /**
     * Must be a token created by the effect's source permanent (CR 111 provenance), recognized via
     * the source's stamped `CreatedByComponent`. "Tokens created with this creature" (Tetravus).
     */
    fun createdBySource() = withStatePredicate(StatePredicate.CreatedBySource)

    /**
     * Not the target of an ability on the stack from another permanent sharing the effect source's
     * name. Goblin Artisans: "counter target artifact spell you control that isn't the target of an
     * ability from another creature named Goblin Artisans."
     */
    fun notTargetedByAbilityFromSameNamedSource() =
        withStatePredicate(StatePredicate.NotTargetedByAbilityFromSameNamedSource)

    /**
     * Must NOT be the permanent the effect's source is attached to (its enchanted/equipped
     * creature). "Other than enchanted creature" exclusions on Aura/Equipment edicts — e.g.
     * Sporogenic Infection's "sacrifices a creature of their choice other than enchanted creature".
     */
    fun notAttachedToBySource() = withStatePredicate(StatePredicate.Not(StatePredicate.IsAttachedToBySource))

    /**
     * Must BE the permanent the effect's source is attached to (its enchanted/equipped
     * permanent). Source-relative — scopes a static ability on an Aura/Equipment to just its
     * host, e.g. Stuck in Summoner's Sanctum's "enchanted permanent's activated abilities can't
     * be activated" via [com.wingedsheep.sdk.scripting.PreventActivatedAbilities].
     */
    fun attachedToBySource() = withStatePredicate(StatePredicate.IsAttachedToBySource)

    /**
     * Must BE the effect's source permanent itself — the [GameObjectFilter] counterpart of
     * `GroupFilter`'s `Scope.Self`. Scopes a filter-carrying static ability to the very
     * permanent that carries it: granting
     * [PreventActivatedAbilities][com.wingedsheep.sdk.scripting.PreventActivatedAbilities]
     * with this filter locks the *holder's own* activated abilities (Braided Net's
     * "Its activated abilities can't be activated for as long as it remains tapped").
     */
    fun sourceItself() = withStatePredicate(StatePredicate.IsSource)

    /**
     * Must NOT be the effect's source permanent — the negation of [sourceItself], and the
     * [GameObjectFilter] counterpart of `GroupFilter`/`TargetFilter`'s `excludeSelf`. Needed
     * wherever "other [permanents]" has to be expressed as a bare `GameObjectFilter` rather
     * than a group/target filter — e.g. a `Recipient.Object` on a damage replacement
     * ("prevent all noncombat damage that would be dealt to *other* creatures you control",
     * Crystal Barricade).
     */
    fun notSourceItself() = withStatePredicate(StatePredicate.Not(StatePredicate.IsSource))

    /**
     * Must BE an Aura/Equipment attached to the effect's source permanent — the mirror of
     * [attachedToBySource]. Source-relative — scopes a static ability on the *host* to its own
     * attachments, e.g. Cloud, Midgar Mercenary's "an Equipment attached to it".
     */
    fun attachedToSource() = withStatePredicate(StatePredicate.IsAttachedToSource)

    /**
     * Must NOT be an Aura/Equipment attached to the effect's source permanent — the negation of
     * [attachedToSource].
     */
    fun notAttachedToSource() = withStatePredicate(StatePredicate.Not(StatePredicate.IsAttachedToSource))

    /**
     * Must NOT be the *granting permanent* of the resolving ability — excludes exactly the
     * Equipment/Aura/permanent whose static ability granted the ability (read from the evaluation
     * context's `granterId`, CR 201.5a). Backs "an artifact other than [this granting Equipment]"
     * on a granted triggered ability whose source is the equipped creature (Dire Blunderbuss's
     * "sacrifice an artifact other than Dire Blunderbuss"): unlike [notAttachedToSource] it
     * excludes only the specific granter, leaving other attachments and same-named copies legal.
     */
    fun notGrantingPermanent() = withStatePredicate(StatePredicate.Not(StatePredicate.IsGrantingPermanent))

    /**
     * Must be attached to a permanent matching [hostFilter] (general form of attachment matching —
     * the host filter may carry a controller predicate, e.g. "a creature you control"). Used by
     * Stolen Uniform's reflexive "if it's attached to a creature you control" guard.
     */
    fun attachedTo(hostFilter: GameObjectFilter) = withStatePredicate(StatePredicate.AttachedTo(hostFilter))

    /** Must be attacking or blocking */
    fun attackingOrBlocking() =
        withStatePredicate(StatePredicate.Or(listOf(StatePredicate.IsAttacking, StatePredicate.IsBlocking)))

    /** Must be attacking, blocking, or tapped */
    fun attackingOrBlockingOrTapped() =
        withStatePredicate(
            StatePredicate.Or(listOf(StatePredicate.IsAttacking, StatePredicate.IsBlocking, StatePredicate.IsTapped))
        )

    /** Must have entered the battlefield this turn */
    fun enteredThisTurn() = withStatePredicate(StatePredicate.EnteredThisTurn)

    /**
     * Must currently be in a graveyard *and* have been put there from the battlefield
     * during the current turn. Used by LTR's Samwise the Stouthearted / Lobelia
     * Sackville-Baggins. See [StatePredicate.PutIntoGraveyardFromBattlefieldThisTurn].
     */
    fun putIntoGraveyardFromBattlefieldThisTurn() =
        withStatePredicate(StatePredicate.PutIntoGraveyardFromBattlefieldThisTurn)

    /**
     * Must currently be in a graveyard *and* have been put there during the current turn,
     * from any zone. The zone-agnostic sibling of [putIntoGraveyardFromBattlefieldThisTurn],
     * used by FDN's Abyssal Harvester. See [StatePredicate.PutIntoGraveyardThisTurn].
     */
    fun putIntoGraveyardThisTurn() = withStatePredicate(StatePredicate.PutIntoGraveyardThisTurn)

    /** Must be saddled (CR 702.171b) */
    fun saddled() = withStatePredicate(StatePredicate.IsSaddled)

    /** Must be suspected (CR 701.60a) — see [StatePredicate.IsSuspected]. */
    fun suspected() = withStatePredicate(StatePredicate.IsSuspected)

    /** Must have the solved designation (CR 719.3b) — see [StatePredicate.IsSolved]. */
    fun solved() = withStatePredicate(StatePredicate.IsSolved)

    /** Must have the renowned designation (CR 702.112b) — see [StatePredicate.IsRenowned]. */
    fun renowned() = withStatePredicate(StatePredicate.IsRenowned)

    /**
     * Must have crewed (CR 702.122) or saddled (CR 702.171) the effect's source permanent this
     * turn. Source-relative — see [StatePredicate.CrewedOrSaddledSourceThisTurn].
     */
    fun crewedOrSaddledSourceThisTurn() = withStatePredicate(StatePredicate.CrewedOrSaddledSourceThisTurn)

    /**
     * Must be a Vehicle/Mount that the effect's source creature crewed (CR 702.122) or saddled
     * (CR 702.171) this turn. Source-relative mirror of [crewedOrSaddledSourceThisTurn] — see
     * [StatePredicate.CrewedOrSaddledBySourceThisTurn].
     */
    fun crewedOrSaddledBySourceThisTurn() = withStatePredicate(StatePredicate.CrewedOrSaddledBySourceThisTurn)

    /** Must be face-down */
    fun faceDown() = withStatePredicate(StatePredicate.IsFaceDown)

    /** Must be face-up (not face-down) */
    fun faceUp() = withStatePredicate(StatePredicate.IsFaceUp)

    /** Must have a morph ability */
    fun withMorph() = withStatePredicate(StatePredicate.HasMorphAbility)

    /**
     * Must have a printed disguise ability (CR 702.168) — see [StatePredicate.HasDisguiseAbility].
     * Zone-independent, and independent of whether the object is currently face down.
     */
    fun withDisguise() = withStatePredicate(StatePredicate.HasDisguiseAbility)

    /** Must have a counter of the specified type */
    fun withCounter(counterType: CounterType) = withStatePredicate(StatePredicate.HasCounter(counterType))

    /** Must not have a counter of the specified type. Other counter types are allowed. */
    fun withoutCounter(counterType: CounterType) =
        withStatePredicate(StatePredicate.Not(StatePredicate.HasCounter(counterType)))

    /** Must have any counter of any type */
    fun withAnyCounter() = withStatePredicate(StatePredicate.HasAnyCounter)

    /** Must have no counters of any type ("with no counters on it" — Heartless Act). */
    fun withoutCounters() = withStatePredicate(StatePredicate.Not(StatePredicate.HasAnyCounter))

    /** Must have the greatest power among creatures its controller controls */
    fun hasGreatestPower() = withStatePredicate(StatePredicate.HasGreatestPower)

    /**
     * Must have the least power among *all* creatures on the battlefield (global, both players).
     * On a tie every minimum-power creature matches — pair with a "choose one" selection to break
     * the tie (Drop of Honey).
     */
    fun hasLeastPowerAmongAllCreatures() = withStatePredicate(StatePredicate.HasLeastPowerAmongAllCreatures)

    /**
     * Must have the greatest mana value among *all* creatures on the battlefield (global, both
     * players). On a tie every maximum-mana-value creature matches (Favor of the Mighty).
     */
    fun hasGreatestManaValueAmongAllCreatures() =
        withStatePredicate(StatePredicate.HasGreatestManaValueAmongAllCreatures)

    /** Must have the least mana value among battlefield permanents matching [candidates]. */
    fun hasLeastManaValueAmong(candidates: GameObjectFilter) =
        withStatePredicate(StatePredicate.HasLeastManaValueAmong(candidates))

    /** Must have the least power among creatures its controller controls */
    fun hasLeastPower() = withStatePredicate(StatePredicate.HasLeastPower)

    /** Must be its controller's Ring-bearer (CR 701.54). */
    fun ringBearer() = withStatePredicate(StatePredicate.IsRingBearer)

    /** Must be soulbond-paired with another creature (CR 702.95b). */
    fun paired() = withStatePredicate(StatePredicate.IsPaired)

    /** Must **not** be soulbond-paired — the "unpaired creature" of CR 702.95b. */
    fun unpaired() = withStatePredicate(StatePredicate.Not(StatePredicate.IsPaired))

    /** Must have blocked, or been blocked by, a legendary creature this turn (You Cannot Pass!). */
    fun blockedOrWasBlockedByLegendaryThisTurn() =
        withStatePredicate(StatePredicate.BlockedOrWasBlockedByLegendaryThisTurn)

    /**
     * Must have blocked, or been blocked by, the creature [reference] names this turn — "all
     * creatures that blocked or were blocked by it this turn" (Gaze of the Gorgon).
     */
    fun blockedOrWasBlockedByThisTurn(reference: EffectTarget.SingleEntity) =
        withStatePredicate(StatePredicate.BlockedOrWasBlockedByEntityThisTurn(reference))

    /** Must have at least one Equipment attached */
    fun equipped() = withStatePredicate(StatePredicate.IsEquipped)

    /**
     * Must have at least one Aura attached — "enchanted creature" as a group adjective. Compose with
     * [youControl] for "enchanted creatures you control" (A Tale for the Ages); the Aura's own
     * controller is irrelevant. See [StatePredicate.IsEnchanted].
     */
    fun enchanted() = withStatePredicate(StatePredicate.IsEnchanted)

    /**
     * Must have at least one attached Aura controlled by [auraController] — the aura-control-scoped
     * form of [enchanted] ("enchanted by Auras you control", Archon of the Wild Rose). Compose with
     * [youControl] to constrain the enchanted permanent's controller too; the two bind to different
     * objects. See [StatePredicate.IsEnchantedByAura].
     */
    fun enchantedByAura(auraController: ControllerPredicate = ControllerPredicate.ControlledByYou) =
        withStatePredicate(StatePredicate.IsEnchantedByAura(auraController))

    /**
     * Must be marked as a "warped card in exile" (CR 702.185b) — i.e., the
     * engine wrote a `WarpExiledComponent` when the warped permanent left the
     * battlefield at end of turn. Use this when filtering candidates in the
     * exile zone for costs like Close Encounter.
     */
    fun warpExiled() = withStatePredicate(StatePredicate.IsWarpExiled)

    /**
     * Must be a card exiled by the effect's source permanent (recorded in the source's
     * `LinkedExileComponent`). Source-relative — use when filtering candidates in the exile zone
     * for "target card exiled with ~" reanimation abilities (The Darkness Crystal).
     */
    fun exiledWithSource() = withStatePredicate(StatePredicate.ExiledWithSource)

    /**
     * Must be a permanent on the battlefield that was cast for its warp cost
     * (CR 702.185) — i.e., the engine wrote a `WarpedComponent` when the
     * warped spell resolved. Use this to gate effects on warp-cast permanents,
     * e.g. Full Bore's conditional trample + haste branch.
     */
    fun castForWarp() = withStatePredicate(StatePredicate.WasCastForWarp)

    /**
     * Must be a spell on the stack cast from [zone] — reads the engine's stamped
     * `SpellOnStackComponent.castFromZone`. See [StatePredicate.WasCastFromZone].
     */
    fun castFromZone(zone: Zone) = withStatePredicate(StatePredicate.WasCastFromZone(zone))

    /**
     * Must be a spell on the stack that was *not* cast from [zone]. Backs Wash Away's
     * "counter target spell that wasn't cast from its owner's hand" (`Zone.HAND`).
     */
    fun notCastFromZone(zone: Zone) =
        withStatePredicate(StatePredicate.Not(StatePredicate.WasCastFromZone(zone)))

    // =============================================================================
    // Fluent Builder Methods - Controller Predicates
    // =============================================================================

    /** Must be controlled by you */
    fun youControl() = withControllerPredicate(ControllerPredicate.ControlledByYou)

    /** Must be controlled by an opponent */
    fun opponentControls() = withControllerPredicate(ControllerPredicate.ControlledByOpponent)

    /** Controlled by any player (no controller scope) */
    fun anyController() = withControllerPredicate(ControllerPredicate.ControlledByAny)

    /**
     * Controlled by the player the asking ability's *source* Aura is attached to — "creatures
     * enchanted player controls" (Radiant Restraints). The attachment-scoped controller sibling of
     * [StatePredicate.IsAttackingEnchantedPlayer]: every other controller scope here resolves
     * against the ability's own controller, which for a curse is exactly the wrong player.
     *
     * A named recipe over [ControllerPredicate.ControlledByReferencedPlayer], not a new predicate —
     * `PlayerRef(Player.EnchantedPlayer)` already names the player; this only spares every curse
     * from spelling out the reference and gives the shape a searchable name.
     *
     * Fails closed when the source isn't an Aura attached to a player, so a curse that has fallen
     * off (or a filter asked with no source in scope) matches nothing rather than everything.
     */
    fun controlledByEnchantedPlayer() = withControllerPredicate(
        ControllerPredicate.ControlledByReferencedPlayer(EffectTarget.PlayerRef(Player.EnchantedPlayer))
    )

    /** Must be controlled by the active player (the player whose turn it is) */
    fun controlledByActivePlayer() = withControllerPredicate(ControllerPredicate.ControlledByActivePlayer)

    /** Must be controlled by the target opponent */
    fun targetOpponentControls() = withControllerPredicate(ControllerPredicate.ControlledByTargetOpponent)

    /** Must be controlled by the target player */
    fun targetPlayerControls() = withControllerPredicate(ControllerPredicate.ControlledByTargetPlayer)

    /**
     * Must be controlled by the player referenced by [target].
     *
     * Preferred over [targetPlayerControls] when a spell declares the player target
     * explicitly (e.g., `val p = target(Targets.Player)`) and threads
     * that reference into the filter — avoids relying on implicit "first player target"
     * resolution.
     */
    fun targetPlayerControls(target: EffectTarget) =
        withControllerPredicate(ControllerPredicate.ControlledByReferencedPlayer(target))

    /** Must be owned by you (for cards in graveyards/exile that don't have controllers) */
    fun ownedByYou() = withControllerPredicate(ControllerPredicate.OwnedByYou)

    /** Must be owned by an opponent (for cards in graveyards/exile that don't have controllers) */
    fun ownedByOpponent() = withControllerPredicate(ControllerPredicate.OwnedByOpponent)

    /**
     * Must be owned by the target player (Hurkyl's Recall — "all artifacts target player owns").
     * Matches the card's immutable owner, so it captures battlefield permanents the target owns
     * even when another player controls them.
     */
    fun ownedByTargetPlayer() = withControllerPredicate(ControllerPredicate.OwnedByTargetPlayer)

    /**
     * Must be owned by the trigger's associated player — the damaged player for a combat/damage
     * trigger (Fire Lord Sozin — "creature cards … from that player's graveyard"). Matches the
     * card's immutable owner, so it captures cards in a graveyard the triggering player owns.
     */
    fun ownedByTriggeringPlayer() = withControllerPredicate(ControllerPredicate.OwnedByTriggeringPlayer)

    /**
     * Must be controlled by the trigger's associated player — the damaged player for a combat/damage
     * trigger (Dreadmaw's Ire — "destroy target artifact that player controls"). Reads projected
     * control, so it tracks control-changing effects, unlike the owner-based
     * [ownedByTriggeringPlayer].
     */
    fun controlledByTriggeringPlayer() =
        withControllerPredicate(ControllerPredicate.ControlledByTriggeringPlayer)

    /**
     * Must match [predicate] on the controller/owner axis. The entry point for *composed*
     * predicates ([ControllerPredicate.And] / [ControllerPredicate.Or] / [ControllerPredicate.Not])
     * — e.g. "permanents you own but don't control":
     * `withControllerPredicate(ControllerPredicate.And(listOf(OwnedByYou, ControlledByOpponent)))`.
     * For the plain single predicates, prefer the named builders ([youControl],
     * [opponentControls], [ownedByYou], …).
     */
    fun withControllerPredicate(predicate: ControllerPredicate): Self =
        mapObjectFilter { it.copy(controllerPredicate = predicate) }

}
