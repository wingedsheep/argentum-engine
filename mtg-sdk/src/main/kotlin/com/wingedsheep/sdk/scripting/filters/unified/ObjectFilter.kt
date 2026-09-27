package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.predicates.StatePredicate
import com.wingedsheep.sdk.scripting.text.TextReplaceable
import com.wingedsheep.sdk.scripting.text.TextReplacer
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
 * The fluent builders (`tapped()`, `youControl()`, `withColor(…)`, …) live on
 * [ObjectFilterBuilder], shared with [com.wingedsheep.sdk.scripting.filters.unified.TargetFilter]
 * and [com.wingedsheep.sdk.scripting.filters.unified.GroupFilter].
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
) : TextReplaceable<GameObjectFilter>, ObjectFilterBuilder<GameObjectFilter> {
    val description: String
        get() = buildDescription()

    /**
     * The English indefinite article ("a" or "an") for this filter's type name.
     * Derived from the card predicates' leading type word (e.g. "artifact", "equipment"),
     * not from the full [description] — which may include "you control" prefixes
     * that would give the wrong first letter.
     *
     * Reads the predicates in [orderedCardPredicates] order, the same order [description] renders
     * them in, so the article always agrees with the word it precedes: "**an** Elf creature", not
     * "a Elf creature". The only consumer pairs the two directly (`CostAtom`'s
     * "from <article> <description> you control").
     */
    val indefiniteArticle: String
        get() {
            val typeWord = orderedCardPredicates().firstOrNull()?.description?.trim()
                ?: if (anyOf.isNotEmpty()) return anyOf.first().indefiniteArticle
                else description.trim()
            val first = typeWord.firstOrNull()?.lowercaseChar() ?: return "a"
            return if (first in "aeiou") "an" else "a"
        }

    /**
     * Card predicates in rendering order: subtypes ahead of the card type, because Magic templates
     * them that way — "Wolf creature", "Equipment artifact", never "creature Wolf". A stable
     * partition, so every other pairing keeps its original insertion order.
     */
    private fun orderedCardPredicates(): List<CardPredicate> {
        val (subtypes, rest) = cardPredicates.partition { it is CardPredicate.HasSubtype }
        return subtypes + rest
    }

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
        orderedCardPredicates().forEach { predicate ->
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
        /**
         * A land with one of the five basic land types (CR 205.3i) — Boseiju, Who Endures' "a
         * land card with a basic land type". Deliberately *not* [BasicLand]: a shockland
         * (`Land — Forest Island`) or Dryad Arbor qualifies, and a basic is only the common case.
         */
        val LandWithBasicLandType = GameObjectFilter(
            cardPredicates = listOf(
                CardPredicate.IsLand,
                CardPredicate.Or(
                    listOf(
                        CardPredicate.HasBasicLandType(Subtype.PLAINS.value),
                        CardPredicate.HasBasicLandType(Subtype.ISLAND.value),
                        CardPredicate.HasBasicLandType(Subtype.SWAMP.value),
                        CardPredicate.HasBasicLandType(Subtype.MOUNTAIN.value),
                        CardPredicate.HasBasicLandType(Subtype.FOREST.value),
                    )
                )
            )
        )
        val Artifact = GameObjectFilter(cardPredicates = listOf(CardPredicate.IsArtifact))
        val Enchantment = GameObjectFilter(cardPredicates = listOf(CardPredicate.IsEnchantment))
        val Planeswalker = GameObjectFilter(cardPredicates = listOf(CardPredicate.IsPlaneswalker))
        val Battle = GameObjectFilter(cardPredicates = listOf(CardPredicate.IsBattle))
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

        /** Instant, sorcery, or a card with an Adventure — Frantic Firebolt's graveyard tally. */
        val InstantSorceryOrAdventure = GameObjectFilter(
            cardPredicates = listOf(
                CardPredicate.Or(
                    listOf(CardPredicate.IsInstant, CardPredicate.IsSorcery, CardPredicate.HasAdventure)
                )
            )
        )
        val CreatureOrPlaneswalker = GameObjectFilter(
            cardPredicates = listOf(
                CardPredicate.Or(listOf(CardPredicate.IsCreature, CardPredicate.IsPlaneswalker))
            )
        )
        /** "Creature, planeswalker, or battle" — the March of the Machine damage-spell wording. */
        val CreaturePlaneswalkerOrBattle = GameObjectFilter(
            cardPredicates = listOf(
                CardPredicate.Or(listOf(CardPredicate.IsCreature, CardPredicate.IsPlaneswalker, CardPredicate.IsBattle))
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

        /**
         * Artifact, enchantment, or token — the permanents bargain lets you sacrifice
         * (CR 702.166a). "Token" is not a card type, so it's an independent alternative here:
         * a Food artifact token, an Aura, and a plain 1/1 creature token all qualify, while a
         * nontoken creature does not.
         */
        val ArtifactEnchantmentOrToken = GameObjectFilter(
            cardPredicates = listOf(
                CardPredicate.Or(
                    listOf(
                        CardPredicate.IsArtifact,
                        CardPredicate.IsEnchantment,
                        CardPredicate.IsToken,
                    )
                )
            )
        )
        val CreatureOrArtifact = GameObjectFilter(
            cardPredicates = listOf(
                CardPredicate.Or(listOf(CardPredicate.IsCreature, CardPredicate.IsArtifact))
            )
        )
        // A Vehicle is always an artifact carrying the Vehicle subtype, so matching the subtype
        // alone identifies it. Used for "creature and/or Vehicle" wording (Rip, Spawn Hunter).
        val CreatureOrVehicle = GameObjectFilter(
            cardPredicates = listOf(
                CardPredicate.Or(listOf(CardPredicate.IsCreature, CardPredicate.HasSubtype(Subtype.VEHICLE)))
            )
        )
        val ArtifactOrLand = GameObjectFilter(
            cardPredicates = listOf(
                CardPredicate.Or(listOf(CardPredicate.IsArtifact, CardPredicate.IsLand))
            )
        )

        /**
         * Artifact, enchantment, or land — the "flexible Naturalize" target family
         * (Creeping Mold, Fade from History). Sits alongside [ArtifactOrEnchantment]
         * and [ArtifactOrLand] as the third member of the same union family.
         */
        val ArtifactEnchantmentOrLand = GameObjectFilter(
            cardPredicates = listOf(
                CardPredicate.Or(
                    listOf(
                        CardPredicate.IsArtifact,
                        CardPredicate.IsEnchantment,
                        CardPredicate.IsLand,
                    )
                )
            )
        )
        val ArtifactCreature = GameObjectFilter(
            cardPredicates = listOf(CardPredicate.IsArtifact, CardPredicate.IsCreature)
        )
        val ArtifactCreatureOrEnchantment = GameObjectFilter(
            cardPredicates = listOf(
                CardPredicate.Or(
                    listOf(
                        CardPredicate.IsArtifact,
                        CardPredicate.IsCreature,
                        CardPredicate.IsEnchantment
                    )
                )
            )
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

    override fun mapObjectFilter(transform: (GameObjectFilter) -> GameObjectFilter) = transform(this)

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
