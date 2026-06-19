package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.predicates.StatePredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.text.TextReplaceable
import com.wingedsheep.sdk.scripting.text.TextReplacer
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityReference
import kotlinx.serialization.Serializable

/**
 * Universal filter for matching game objects (cards, permanents, spells).
 * Composes CardPredicate, StatePredicate, and ControllerPredicate for flexible filtering.
 *
 * This replaces the scattered filter types (CardFilter, CountFilter, etc.) with a unified,
 * composable approach.
 *
 * Note: Named GameObjectFilter to distinguish from other filter types in the SDK
 *
 * ## Usage Examples
 *
 * ```kotlin
 * // Simple type filter
 * GameObjectFilter.Creature
 *
 * // Creature with specific properties
 * GameObjectFilter.Creature.withColor(Color.BLACK).tapped()
 *
 * // Controlled creatures
 * GameObjectFilter.Creature.youControl()
 *
 * // Complex filter: tapped creatures with power 2 or less that opponent controls
 * GameObjectFilter.Creature
 *     .tapped()
 *     .powerAtMost(2)
 *     .opponentControls()
 * ```
 */
@Serializable
data class GameObjectFilter(
    val cardPredicates: List<CardPredicate> = emptyList(),
    val statePredicates: List<StatePredicate> = emptyList(),
    val controllerPredicate: ControllerPredicate? = null,
    /**
     * Recursive union: when non-empty, an object matches this filter only if it matches
     * the base predicates above AND at least one of these sub-filters. This is what the
     * [or] infix builds, and it is the only faithful way to express a *heterogeneous* OR —
     * one whose branches carry different state/controller predicates (e.g. "artifact or
     * tapped creature", where the tapped restriction applies only to the creature branch).
     * Each branch is a full [GameObjectFilter], so it composes to any depth.
     */
    val anyOf: List<GameObjectFilter> = emptyList()
) : TextReplaceable<GameObjectFilter> {
    val description: String
        get() = buildDescription()

    private fun buildDescription(): String = buildString {
        controllerPredicate?.let {
            if (it.description.isNotEmpty()) {
                append(it.description)
                append(" ")
            }
        }
        statePredicates.forEach { predicate ->
            append(predicate.description)
            append(" ")
        }
        cardPredicates.forEach { predicate ->
            append(predicate.description)
            append(" ")
        }
        if (anyOf.isNotEmpty()) {
            append(anyOf.joinToString(" or ") { it.description })
        }
    }.trim().ifEmpty { "card" }

    // =============================================================================
    // Pre-built Common Filters
    // =============================================================================

    companion object {
        /** Match any object */
        val Any = GameObjectFilter()

        // Type filters
        val Creature = GameObjectFilter(cardPredicates = listOf(CardPredicate.IsCreature))
        val Land = GameObjectFilter(cardPredicates = listOf(CardPredicate.IsLand))
        val BasicLand = GameObjectFilter(cardPredicates = listOf(CardPredicate.IsBasicLand))
        val NonbasicLand = GameObjectFilter(
            cardPredicates = listOf(CardPredicate.IsLand, CardPredicate.Not(CardPredicate.IsBasicLand))
        )
        val Artifact = GameObjectFilter(cardPredicates = listOf(CardPredicate.IsArtifact))
        val Enchantment = GameObjectFilter(cardPredicates = listOf(CardPredicate.IsEnchantment))
        val Planeswalker = GameObjectFilter(cardPredicates = listOf(CardPredicate.IsPlaneswalker))
        val Instant = GameObjectFilter(cardPredicates = listOf(CardPredicate.IsInstant))
        val Sorcery = GameObjectFilter(cardPredicates = listOf(CardPredicate.IsSorcery))
        val Permanent = GameObjectFilter(cardPredicates = listOf(CardPredicate.IsPermanent))
        val Nonland = GameObjectFilter(cardPredicates = listOf(CardPredicate.IsNonland))
        val NonlandPermanent = GameObjectFilter(
            cardPredicates = listOf(CardPredicate.IsNonland, CardPredicate.IsPermanent)
        )
        val Noncreature = GameObjectFilter(cardPredicates = listOf(CardPredicate.IsNoncreature))
        val Nonenchantment = GameObjectFilter(cardPredicates = listOf(CardPredicate.IsNonenchantment))
        val Nonartifact = GameObjectFilter(cardPredicates = listOf(CardPredicate.IsNonartifact))
        val Token = GameObjectFilter(cardPredicates = listOf(CardPredicate.IsToken))
        val Multicolored = GameObjectFilter(cardPredicates = listOf(CardPredicate.IsMulticolored))

        // Combined type filters
        val InstantOrSorcery = GameObjectFilter(
            cardPredicates = listOf(
                CardPredicate.Or(listOf(CardPredicate.IsInstant, CardPredicate.IsSorcery))
            )
        )
        val CreatureOrPlaneswalker = GameObjectFilter(
            cardPredicates = listOf(
                CardPredicate.Or(listOf(CardPredicate.IsCreature, CardPredicate.IsPlaneswalker))
            )
        )
        val CreatureOrLand = GameObjectFilter(
            cardPredicates = listOf(
                CardPredicate.Or(listOf(CardPredicate.IsCreature, CardPredicate.IsLand))
            )
        )
        val CreatureOrSorcery = GameObjectFilter(
            cardPredicates = listOf(
                CardPredicate.Or(listOf(CardPredicate.IsCreature, CardPredicate.IsSorcery))
            )
        )
        val CreatureOrEnchantment = GameObjectFilter(
            cardPredicates = listOf(
                CardPredicate.Or(listOf(CardPredicate.IsCreature, CardPredicate.IsEnchantment))
            )
        )
        val ArtifactOrEnchantment = GameObjectFilter(
            cardPredicates = listOf(
                CardPredicate.Or(listOf(CardPredicate.IsArtifact, CardPredicate.IsEnchantment))
            )
        )
        val CreatureOrArtifact = GameObjectFilter(
            cardPredicates = listOf(
                CardPredicate.Or(listOf(CardPredicate.IsCreature, CardPredicate.IsArtifact))
            )
        )
        val ArtifactOrLand = GameObjectFilter(
            cardPredicates = listOf(
                CardPredicate.Or(listOf(CardPredicate.IsArtifact, CardPredicate.IsLand))
            )
        )
        val ArtifactCreature = GameObjectFilter(
            cardPredicates = listOf(CardPredicate.IsArtifact, CardPredicate.IsCreature)
        )
        val NoncreaturePermanent = GameObjectFilter(
            cardPredicates = listOf(CardPredicate.IsNoncreature, CardPredicate.IsPermanent)
        )
        val Historic = GameObjectFilter(
            cardPredicates = listOf(
                CardPredicate.Or(listOf(
                    CardPredicate.IsArtifact,
                    CardPredicate.IsLegendary,
                    CardPredicate.HasSubtype(Subtype("Saga"))
                ))
            )
        )
    }

    // =============================================================================
    // Fluent Builder Methods - Card Predicates
    // =============================================================================

    /** Add a color requirement */
    fun withColor(color: Color) = copy(
        cardPredicates = cardPredicates + CardPredicate.HasColor(color)
    )

    /** Match any of the specified colors (OR logic) */
    fun withAnyColor(vararg colors: Color) = copy(
        cardPredicates = cardPredicates + CardPredicate.Or(colors.map { CardPredicate.HasColor(it) })
    )

    /** Exclude a color */
    fun notColor(color: Color) = copy(
        cardPredicates = cardPredicates + CardPredicate.NotColor(color)
    )

    /** Match the color chosen during the current effect's resolution (e.g. via ChooseColorThen). */
    fun withChosenColor() = copy(
        cardPredicates = cardPredicates + CardPredicate.HasChosenColor
    )

    /** Restrict to monocolored objects (exactly one color). Colorless objects do not match. */
    fun monocolored() = copy(
        cardPredicates = cardPredicates + CardPredicate.IsMonocolored
    )

    /** Add a subtype requirement */
    fun withSubtype(subtype: Subtype) = copy(
        cardPredicates = cardPredicates + CardPredicate.HasSubtype(subtype)
    )

    /** Add a subtype requirement by string */
    fun withSubtype(subtype: String) = withSubtype(Subtype(subtype))

    /** Match any of the specified subtypes (OR logic). */
    fun withAnySubtype(vararg subtypes: String) = copy(
        cardPredicates = cardPredicates + CardPredicate.Or(
            subtypes.map { CardPredicate.HasSubtype(Subtype(it)) }
        )
    )

    /** Exclude a subtype */
    fun notSubtype(subtype: Subtype) = copy(
        cardPredicates = cardPredicates + CardPredicate.NotSubtype(subtype)
    )

    /** Restrict to nonartifact objects ("nonartifact creature", the Terror template). */
    fun nonartifact() = copy(
        cardPredicates = cardPredicates + CardPredicate.IsNonartifact
    )

    /** Exclude creatures ("noncreature artifact", e.g. Guardian Beast). */
    fun notCreature() = copy(
        cardPredicates = cardPredicates + CardPredicate.Not(CardPredicate.IsCreature)
    )

    /**
     * Restrict to spells/abilities on the stack that target at least one object matching
     * [subfilter]. Used for "an instant or sorcery spell that targets a creature" (Repartee —
     * Forum Necroscribe, Lecturing Scornmage) and "target spell that targets a land you control"
     * (Teferi's Response). Player targets are skipped (CR — they have no game-object filter).
     */
    fun targetsMatching(subfilter: GameObjectFilter) = copy(
        cardPredicates = cardPredicates + CardPredicate.TargetsMatching(subfilter)
    )

    /** Add a keyword requirement */
    fun withKeyword(keyword: Keyword) = copy(
        cardPredicates = cardPredicates + CardPredicate.HasKeyword(keyword)
    )

    /** Exclude a keyword */
    fun withoutKeyword(keyword: Keyword) = copy(
        cardPredicates = cardPredicates + CardPredicate.NotKeyword(keyword)
    )

    /** Match by exact card name */
    fun named(name: String) = copy(
        cardPredicates = cardPredicates + CardPredicate.NameEquals(name)
    )

    /** Match cards whose name equals the card name stored in chosenValues[variableName] */
    fun namedFromVariable(variableName: String) = copy(
        cardPredicates = cardPredicates + CardPredicate.NameEqualsChosen(variableName)
    )

    /**
     * Match cards whose name equals the name durably chosen by the *source permanent* as it
     * entered (its [com.wingedsheep.engine.state.components.battlefield.CastChoicesComponent] under
     * [slot]). Static-projection / activation-legality safe — use this in static-ability filters
     * (Petrified Hamlet), where [namedFromVariable] would fail closed.
     */
    fun namedFromChosenComponent(slot: com.wingedsheep.sdk.scripting.ChoiceSlot = com.wingedsheep.sdk.scripting.ChoiceSlot.CARD_NAME) = copy(
        cardPredicates = cardPredicates + CardPredicate.NameEqualsChosenComponent(slot)
    )

    /** Mana value equals */
    fun manaValue(value: Int) = copy(
        cardPredicates = cardPredicates + CardPredicate.ManaValueEquals(value)
    )

    /** Mana value at most */
    fun manaValueAtMost(max: Int) = copy(
        cardPredicates = cardPredicates + CardPredicate.ManaValueAtMost(max)
    )

    /** Mana value at most the X chosen for the source spell/ability */
    fun manaValueAtMostX() = copy(
        cardPredicates = cardPredicates + CardPredicate.ManaValueAtMostX
    )

    /** Mana value exactly equal to the number chosen for the source spell/ability (Void) */
    fun manaValueEqualsX() = copy(
        cardPredicates = cardPredicates + CardPredicate.ManaValueEqualsX
    )

    /** Mana value at least */
    fun manaValueAtLeast(min: Int) = copy(
        cardPredicates = cardPredicates + CardPredicate.ManaValueAtLeast(min)
    )

    /** Mana value at most that of a referenced entity (triggering, source, etc.) */
    fun manaValueAtMostEntity(reference: EntityReference) = copy(
        cardPredicates = cardPredicates + CardPredicate.ManaValueAtMostEntity(reference)
    )

    /** Mana value at most the mana actually spent to cast a referenced entity (source, etc.) */
    fun manaValueAtMostEntityManaSpent(reference: EntityReference) = copy(
        cardPredicates = cardPredicates + CardPredicate.ManaValueAtMostEntityManaSpent(reference)
    )

    /** Mana value at most the number of colors of mana spent to cast a referenced entity (Converge). */
    fun manaValueAtMostColorsSpent(reference: EntityReference) = copy(
        cardPredicates = cardPredicates + CardPredicate.ManaValueAtMostColorsSpent(reference)
    )

    /** Mana value at most a resolved [DynamicAmount] (e.g. "X or less, where X is the life you gained this turn"). */
    fun manaValueAtMostDynamic(amount: DynamicAmount) = copy(
        cardPredicates = cardPredicates + CardPredicate.ManaValueAtMostDynamic(amount)
    )

    /** Mana value is even (zero is even). */
    fun manaValueIsEven() = copy(
        cardPredicates = cardPredicates + CardPredicate.ManaValueIsEven
    )

    /** Mana value is odd. */
    fun manaValueIsOdd() = copy(
        cardPredicates = cardPredicates + CardPredicate.ManaValueIsOdd
    )

    /** Printed mana cost contains an {X} symbol (Paradox Surveyor). */
    fun hasXInManaCost() = copy(
        cardPredicates = cardPredicates + CardPredicate.HasXInManaCost
    )

    /** Power equals */
    fun power(value: Int) = copy(
        cardPredicates = cardPredicates + CardPredicate.PowerEquals(value)
    )

    /** Power exactly equal to the X chosen for the source spell/ability (Ent-Draught Basin) */
    fun powerEqualsX() = copy(
        cardPredicates = cardPredicates + CardPredicate.PowerEqualsX
    )

    /** Power at most */
    fun powerAtMost(max: Int) = copy(
        cardPredicates = cardPredicates + CardPredicate.PowerAtMost(max)
    )

    /** Power at least */
    fun powerAtLeast(min: Int) = copy(
        cardPredicates = cardPredicates + CardPredicate.PowerAtLeast(min)
    )

    /** Power strictly greater than the projected power of a referenced entity (source, triggering, etc.) */
    fun powerGreaterThanEntity(reference: EntityReference) = copy(
        cardPredicates = cardPredicates + CardPredicate.PowerGreaterThanEntity(reference)
    )

    /** Power less than or equal to the projected power of a referenced entity (source, triggering, etc.) */
    fun powerLessThanEntity(reference: EntityReference) = copy(
        cardPredicates = cardPredicates + CardPredicate.PowerLessThanEntity(reference)
    )

    fun powerAtMostEntity(reference: EntityReference) = copy(
        cardPredicates = cardPredicates + CardPredicate.PowerAtMostEntity(reference)
    )

    /** Toughness at most */
    fun toughnessAtMost(max: Int) = copy(
        cardPredicates = cardPredicates + CardPredicate.ToughnessAtMost(max)
    )

    /** Toughness at most the X chosen for the source spell/ability. */
    fun toughnessAtMostX() = copy(
        cardPredicates = cardPredicates + CardPredicate.ToughnessAtMostX
    )

    /** Toughness at least */
    fun toughnessAtLeast(min: Int) = copy(
        cardPredicates = cardPredicates + CardPredicate.ToughnessAtLeast(min)
    )

    /** Power or toughness at least */
    fun powerOrToughnessAtLeast(min: Int) = copy(
        cardPredicates = cardPredicates + CardPredicate.PowerOrToughnessAtLeast(min)
    )

    /** Total power and toughness (sum) at most */
    fun totalPowerAndToughnessAtMost(max: Int) = copy(
        cardPredicates = cardPredicates + CardPredicate.TotalPowerAndToughnessAtMost(max)
    )

    /** Toughness strictly greater than power */
    fun toughnessGreaterThanPower() = copy(
        cardPredicates = cardPredicates + CardPredicate.ToughnessGreaterThanPower
    )

    /** Must be legendary */
    fun legendary() = copy(
        cardPredicates = cardPredicates + CardPredicate.IsLegendary
    )

    /** Must not be legendary */
    fun nonlegendary() = copy(
        cardPredicates = cardPredicates + CardPredicate.IsNonlegendary
    )

    /** Must not be a basic land ("nonbasic land", e.g. Rocket Volley, Shivan Harvest). */
    fun nonbasic() = copy(
        cardPredicates = cardPredicates + CardPredicate.Not(CardPredicate.IsBasicLand)
    )

    /** Must not be a token */
    fun nontoken() = copy(
        cardPredicates = cardPredicates + CardPredicate.IsNontoken
    )

    /** Must be a token */
    fun token() = copy(
        cardPredicates = cardPredicates + CardPredicate.IsToken
    )

    /** Must not be of the creature type chosen on the source permanent */
    fun notOfSourceChosenType() = copy(
        cardPredicates = cardPredicates + CardPredicate.NotOfSourceChosenType
    )

    /** Must have a subtype matching the value stored in chosenValues[variableName] */
    fun withSubtypeFromVariable(variableName: String) = copy(
        cardPredicates = cardPredicates + CardPredicate.HasSubtypeFromVariable(variableName)
    )

    /** Must have a subtype matching any value in storedStringLists[listName] */
    fun withSubtypeInStoredList(listName: String) = copy(
        cardPredicates = cardPredicates + CardPredicate.HasSubtypeInStoredList(listName)
    )

    /**
     * Must share at least one subtype with every group in the stored subtype list
     * `pipeline.storedSubtypeGroups[groupName]`. Pair with
     * [com.wingedsheep.sdk.scripting.effects.GatherSubtypesEffect] to populate the
     * stored groups from an entity collection.
     */
    fun withSubtypeInEachStoredGroup(groupName: String) = copy(
        cardPredicates = cardPredicates + CardPredicate.HasSubtypeInEachStoredGroup(groupName)
    )

    /** Must have any one of the given subtypes (OR logic) */
    fun withAnyOfSubtypes(subtypes: List<Subtype>) = copy(
        cardPredicates = cardPredicates + CardPredicate.HasAnyOfSubtypes(subtypes)
    )

    /**
     * Must have none of the given subtypes (e.g. "non-outlaw creature" for a
     * group of outlaw subtypes). Composes [CardPredicate.Not] over
     * [CardPredicate.HasAnyOfSubtypes], so it reuses existing evaluator support.
     */
    fun notAnyOfSubtypes(subtypes: List<Subtype>) = copy(
        cardPredicates = cardPredicates + CardPredicate.Not(CardPredicate.HasAnyOfSubtypes(subtypes))
    )

    /** Must have the subtype chosen on the source permanent */
    fun withChosenSubtype() = copy(
        cardPredicates = cardPredicates + CardPredicate.HasChosenSubtype
    )

    /** Must include the color chosen on the source permanent (CastChoicesComponent) */
    fun sharingChosenColorWithSource() = copy(
        cardPredicates = cardPredicates + CardPredicate.SharesChosenColorWithSource
    )

    /** Must share a creature type with the referenced entity */
    fun sharingCreatureTypeWith(entity: EntityReference) = copy(
        cardPredicates = cardPredicates + CardPredicate.SharesCreatureTypeWith(entity)
    )

    /** Must share a color with the referenced entity */
    fun sharingColorWith(entity: EntityReference) = copy(
        cardPredicates = cardPredicates + CardPredicate.SharesColorWith(entity)
    )

    /**
     * Must share a color with the recipient of the in-flight damage (and not be that
     * recipient). Only meaningful in a damage replacement's source filter. Used by
     * Well-Laid Plans.
     */
    fun sharingColorWithRecipient() = copy(
        cardPredicates = cardPredicates + CardPredicate.SharesColorWithRecipient
    )

    /**
     * Must share a color with at least one permanent the evaluating player controls matching
     * [filter] (Ringsight: "a card that shares a color with a legendary creature you control").
     */
    fun sharingColorWithPermanentYouControl(filter: GameObjectFilter) = copy(
        cardPredicates = cardPredicates + CardPredicate.SharesColorWithPermanentYouControl(filter)
    )

    /**
     * Must share **no** creature type with any permanent the evaluating player controls matching
     * [filter] (Radagast the Brown: "a creature card that doesn't share a creature type with a
     * creature you control").
     */
    fun notSharingCreatureTypeWithPermanentYouControl(filter: GameObjectFilter) = copy(
        cardPredicates = cardPredicates + CardPredicate.DoesNotShareCreatureTypeWithPermanentYouControl(filter)
    )

    // =============================================================================
    // Fluent Builder Methods - State Predicates
    // =============================================================================

    /** Must be tapped */
    fun tapped() = copy(
        statePredicates = statePredicates + StatePredicate.IsTapped
    )

    /** Must be untapped */
    fun untapped() = copy(
        statePredicates = statePredicates + StatePredicate.IsUntapped
    )

    /** Must be attacking */
    fun attacking() = copy(
        statePredicates = statePredicates + StatePredicate.IsAttacking
    )

    /**
     * Must have been declared as an attacker at least once during the current turn.
     * Survives leaving combat; cleared at end-of-turn cleanup.
     */
    fun attackedThisTurn() = copy(
        statePredicates = statePredicates + StatePredicate.AttackedThisTurn
    )

    /**
     * Must have been dealt damage this turn (marked-damage *history*, not current marked damage).
     * Survives damage removal / leaving combat; cleared at end-of-turn cleanup. Used by
     * "...that was dealt damage this turn" (Rooftop Assassin, Unsparing Boltcaster).
     */
    fun dealtDamageThisTurn() = copy(
        statePredicates = statePredicates + StatePredicate.WasDealtDamageThisTurn
    )

    /**
     * Must be in the same combat band as the effect's source (the source itself, or a band-mate
     * sharing its band id — CR 702.22). Source-relative; only matches while the source attacks.
     */
    fun inSameBandAsSource() = copy(
        statePredicates = statePredicates + StatePredicate.InSameBandAsSource
    )

    /**
     * Must have dealt combat damage *this turn* to the player who controls the effect's source.
     * Source-relative; cleared at end-of-turn cleanup. Used by "each opponent sacrifices a
     * creature ... that dealt combat damage to you this turn" (Witch-king of Angmar).
     */
    fun dealtCombatDamageToSourceControllerThisTurn() = copy(
        statePredicates = statePredicates + StatePredicate.DealtCombatDamageToSourceControllerThisTurn
    )

    /** Must be blocking */
    fun blocking() = copy(
        statePredicates = statePredicates + StatePredicate.IsBlocking
    )

    /**
     * Must be blocking the effect's source (CR 509). Source-relative; only matches the source's
     * own blockers. "Whenever this becomes blocked, it deals N damage to each creature blocking it."
     */
    fun blockingSource() = copy(
        statePredicates = statePredicates + StatePredicate.IsBlockingSource
    )

    /** Must be attacking or blocking */
    fun attackingOrBlocking() = copy(
        statePredicates = statePredicates + StatePredicate.Or(
            listOf(StatePredicate.IsAttacking, StatePredicate.IsBlocking)
        )
    )

    /** Must be attacking, blocking, or tapped */
    fun attackingOrBlockingOrTapped() = copy(
        statePredicates = statePredicates + StatePredicate.Or(
            listOf(StatePredicate.IsAttacking, StatePredicate.IsBlocking, StatePredicate.IsTapped)
        )
    )

    /** Must have entered the battlefield this turn */
    fun enteredThisTurn() = copy(
        statePredicates = statePredicates + StatePredicate.EnteredThisTurn
    )

    /**
     * Must currently be in a graveyard *and* have been put there from the battlefield
     * during the current turn. Used by LTR's Samwise the Stouthearted / Lobelia
     * Sackville-Baggins. See [StatePredicate.PutIntoGraveyardFromBattlefieldThisTurn].
     */
    fun putIntoGraveyardFromBattlefieldThisTurn() = copy(
        statePredicates = statePredicates + StatePredicate.PutIntoGraveyardFromBattlefieldThisTurn
    )

    /** Must be saddled (CR 702.171b) */
    fun saddled() = copy(
        statePredicates = statePredicates + StatePredicate.IsSaddled
    )

    /**
     * Must have crewed (CR 702.122) or saddled (CR 702.171) the effect's source permanent this
     * turn. Source-relative — see [StatePredicate.CrewedOrSaddledSourceThisTurn].
     */
    fun crewedOrSaddledSourceThisTurn() = copy(
        statePredicates = statePredicates + StatePredicate.CrewedOrSaddledSourceThisTurn
    )

    /** Must be face-down */
    fun faceDown() = copy(
        statePredicates = statePredicates + StatePredicate.IsFaceDown
    )

    /** Must be face-up (not face-down) */
    fun faceUp() = copy(
        statePredicates = statePredicates + StatePredicate.IsFaceUp
    )

    /** Must have a morph ability */
    fun withMorph() = copy(
        statePredicates = statePredicates + StatePredicate.HasMorphAbility
    )

    /** Must have a counter of the specified type */
    fun withCounter(counterType: String) = copy(
        statePredicates = statePredicates + StatePredicate.HasCounter(counterType)
    )

    /** Must have any counter of any type */
    fun withAnyCounter() = copy(
        statePredicates = statePredicates + StatePredicate.HasAnyCounter
    )

    /** Must have the greatest power among creatures its controller controls */
    fun hasGreatestPower() = copy(
        statePredicates = statePredicates + StatePredicate.HasGreatestPower
    )

    /**
     * Must have the least power among *all* creatures on the battlefield (global, both players).
     * On a tie every minimum-power creature matches — pair with a "choose one" selection to break
     * the tie (Drop of Honey).
     */
    fun hasLeastPowerAmongAllCreatures() = copy(
        statePredicates = statePredicates + StatePredicate.HasLeastPowerAmongAllCreatures
    )

    /** Must have the least power among creatures its controller controls */
    fun hasLeastPower() = copy(
        statePredicates = statePredicates + StatePredicate.HasLeastPower
    )

    /** Must be its controller's Ring-bearer (CR 701.54). */
    fun ringBearer() = copy(
        statePredicates = statePredicates + StatePredicate.IsRingBearer
    )

    /** Must have blocked, or been blocked by, a legendary creature this turn (You Cannot Pass!). */
    fun blockedOrWasBlockedByLegendaryThisTurn() = copy(
        statePredicates = statePredicates + StatePredicate.BlockedOrWasBlockedByLegendaryThisTurn
    )

    /** Must have at least one Equipment attached */
    fun equipped() = copy(
        statePredicates = statePredicates + StatePredicate.IsEquipped
    )

    /**
     * Must be marked as a "warped card in exile" (CR 702.185b) — i.e., the
     * engine wrote a `WarpExiledComponent` when the warped permanent left the
     * battlefield at end of turn. Use this when filtering candidates in the
     * exile zone for costs like Close Encounter.
     */
    fun warpExiled() = copy(
        statePredicates = statePredicates + StatePredicate.IsWarpExiled
    )

    /**
     * Must be a permanent on the battlefield that was cast for its warp cost
     * (CR 702.185) — i.e., the engine wrote a `WarpedComponent` when the
     * warped spell resolved. Use this to gate effects on warp-cast permanents,
     * e.g. Full Bore's conditional trample + haste branch.
     */
    fun castForWarp() = copy(
        statePredicates = statePredicates + StatePredicate.WasCastForWarp
    )

    // =============================================================================
    // Fluent Builder Methods - Controller Predicates
    // =============================================================================

    /** Must be controlled by you */
    fun youControl() = copy(controllerPredicate = ControllerPredicate.ControlledByYou)

    /** Must be controlled by an opponent */
    fun opponentControls() = copy(controllerPredicate = ControllerPredicate.ControlledByOpponent)

    /** Must be controlled by the active player (the player whose turn it is) */
    fun controlledByActivePlayer() = copy(controllerPredicate = ControllerPredicate.ControlledByActivePlayer)

    /** Must be controlled by the target opponent */
    fun targetOpponentControls() = copy(controllerPredicate = ControllerPredicate.ControlledByTargetOpponent)

    /** Must be controlled by the target player */
    fun targetPlayerControls() = copy(controllerPredicate = ControllerPredicate.ControlledByTargetPlayer)

    /**
     * Must be controlled by the player referenced by [target].
     *
     * Preferred over [targetPlayerControls] when a spell declares the player target
     * explicitly (e.g., `val p = target("target player", TargetPlayer())`) and threads
     * that reference into the filter — avoids relying on implicit "first player target"
     * resolution.
     */
    fun targetPlayerControls(target: EffectTarget) =
        copy(controllerPredicate = ControllerPredicate.ControlledByReferencedPlayer(target))

    /** Must be owned by you (for cards in graveyards/exile that don't have controllers) */
    fun ownedByYou() = copy(controllerPredicate = ControllerPredicate.OwnedByYou)

    /** Must be owned by an opponent (for cards in graveyards/exile that don't have controllers) */
    fun ownedByOpponent() = copy(controllerPredicate = ControllerPredicate.OwnedByOpponent)

    /**
     * Must match [predicate] on the controller/owner axis. The entry point for *composed*
     * predicates ([ControllerPredicate.And] / [ControllerPredicate.Or] / [ControllerPredicate.Not])
     * — e.g. "permanents you own but don't control":
     * `withControllerPredicate(ControllerPredicate.And(listOf(OwnedByYou, ControlledByOpponent)))`.
     * For the plain single predicates, prefer the named builders ([youControl],
     * [opponentControls], [ownedByYou], …).
     */
    fun withControllerPredicate(predicate: ControllerPredicate) = copy(controllerPredicate = predicate)

    // =============================================================================
    // Composition
    // =============================================================================

    /**
     * Combine with another filter using AND logic.
     *
     * Both sides may carry the *same* controller predicate (or only one side any), but two
     * *different* controller predicates are rejected: silently keeping one of them was the
     * old fail-open behavior, and there is no single right merge (AND-ing `youControl` with
     * `opponentControls` is unsatisfiable, while "owned by you AND controlled by an
     * opponent" is a real MTG concept). State the intent explicitly with a composed
     * [ControllerPredicate.And] via [withControllerPredicate] instead.
     */
    infix fun and(other: GameObjectFilter): GameObjectFilter {
        require(
            controllerPredicate == null ||
                other.controllerPredicate == null ||
                controllerPredicate == other.controllerPredicate
        ) {
            "Cannot AND two filters with different controller predicates " +
                "('${controllerPredicate?.description}' vs '${other.controllerPredicate?.description}'). " +
                "Compose them explicitly with withControllerPredicate(ControllerPredicate.And(...))."
        }
        require(other.anyOf.isEmpty()) {
            "Cannot AND a filter whose right-hand side carries an anyOf union " +
                "('${other.description}') — the union branches would be silently dropped. " +
                "Restructure so the union is the left-hand side, or distribute the AND over the branches."
        }
        return copy(
            cardPredicates = cardPredicates + other.cardPredicates,
            statePredicates = statePredicates + other.statePredicates,
            controllerPredicate = controllerPredicate ?: other.controllerPredicate
        )
    }

    /**
     * Combine with another filter using OR logic.
     *
     * A *homogeneous* OR — both branches sharing the same state and controller gate and
     * differing only in card-type predicates (e.g. `Creature.youControl() or
     * Artifact.youControl()`) — collapses to a single [CardPredicate.Or] under that shared
     * gate. This is the flat representation the whole engine already understands (including
     * lord / subtype resolution), so the common case stays simple.
     *
     * A *heterogeneous* OR, whose branches carry different state/controller predicates
     * (e.g. `Artifact or Creature.tapped()` — the tapped restriction binds only to the
     * creature branch), cannot be flattened and instead builds the recursive [anyOf] union,
     * where each branch is matched as a complete filter.
     */
    infix fun or(other: GameObjectFilter): GameObjectFilter {
        val homogeneous = controllerPredicate == other.controllerPredicate &&
            statePredicates == other.statePredicates &&
            anyOf.isEmpty() && other.anyOf.isEmpty()
        return if (homogeneous) {
            GameObjectFilter(
                cardPredicates = listOf(
                    CardPredicate.Or(
                        listOf(cardPredicates.toConjunction(), other.cardPredicates.toConjunction())
                    )
                ),
                statePredicates = statePredicates,
                controllerPredicate = controllerPredicate
            )
        } else {
            GameObjectFilter(anyOf = listOf(this, other))
        }
    }

    override fun applyTextReplacement(replacer: TextReplacer): GameObjectFilter {
        var changed = false
        val newPredicates = cardPredicates.map {
            val new = it.applyTextReplacement(replacer)
            if (new !== it) changed = true
            new
        }
        val newAnyOf = anyOf.map {
            val new = it.applyTextReplacement(replacer)
            if (new !== it) changed = true
            new
        }
        return if (changed) copy(cardPredicates = newPredicates, anyOf = newAnyOf) else this
    }
}

/** Wraps a list of predicates into a single conjunction; returns the single element if only one. */
private fun List<CardPredicate>.toConjunction(): CardPredicate =
    if (isEmpty()) CardPredicate.And(emptyList())
    else if (size == 1) first() else CardPredicate.And(this)
