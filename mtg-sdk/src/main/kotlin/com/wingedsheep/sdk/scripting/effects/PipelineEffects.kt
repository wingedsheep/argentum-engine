package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.Zone
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Pipeline Vocabulary Types
// =============================================================================

/**
 * Where to gather cards from.
 */
@Serializable
sealed interface CardSource {
    val description: String

    /**
     * Top N cards of a player's library.
     */
    @SerialName("TopOfLibrary")
    @Serializable
    data class TopOfLibrary(
        val count: DynamicAmount,
        val player: Player = Player.You
    ) : CardSource {
        override val description: String = "the top ${count.description} cards of ${player.possessive} library"
    }

    /**
     * Cards from a specific zone matching an optional filter.
     */
    @SerialName("FromZone")
    @Serializable
    data class FromZone(
        val zone: Zone,
        val player: Player = Player.You,
        val filter: GameObjectFilter = GameObjectFilter.Any
    ) : CardSource {
        override val description: String = "${filter.description} cards in ${player.possessive} ${zone.displayName}"
    }

    /**
     * Read cards from a previously stored collection variable.
     */
    @SerialName("FromVariable")
    @Serializable
    data class FromVariable(val variableName: String) : CardSource {
        override val description: String = "the $variableName cards"
    }
}

/**
 * Where to move cards to.
 */
@Serializable
sealed interface CardDestination {
    val description: String

    /**
     * Move cards to a specific zone.
     */
    @SerialName("ToZone")
    @Serializable
    data class ToZone(
        val zone: Zone,
        val player: Player = Player.You,
        val placement: ZonePlacement = ZonePlacement.Default
    ) : CardDestination {
        override val description: String = buildString {
            append(zone.displayName)
            if (placement != ZonePlacement.Default) {
                append(" (${placement.name.lowercase()})")
            }
        }
    }
}

/**
 * How cards are selected from a collection.
 */
@Serializable
sealed interface SelectionMode {
    val description: String

    /**
     * Player must choose exactly N cards.
     */
    @SerialName("ChooseExactly")
    @Serializable
    data class ChooseExactly(val count: DynamicAmount) : SelectionMode {
        override val description: String = "choose exactly ${count.description}"
    }

    /**
     * Player may choose up to N cards.
     */
    @SerialName("ChooseUpTo")
    @Serializable
    data class ChooseUpTo(val count: DynamicAmount) : SelectionMode {
        override val description: String = "choose up to ${count.description}"
    }

    /**
     * An opponent chooses N cards.
     */
    @SerialName("OpponentChooses")
    @Serializable
    data class OpponentChooses(val count: DynamicAmount) : SelectionMode {
        override val description: String = "an opponent chooses ${count.description}"
    }

    /**
     * Select all cards (no choice needed).
     */
    @SerialName("SelectAll")
    @Serializable
    data object All : SelectionMode {
        override val description: String = "all"
    }
}

/**
 * How to order cards when moving a collection.
 */
@Serializable
enum class CardOrder {
    /** Controller chooses the order (prompts if > 1 card) */
    ControllerChooses,
    /** Random order */
    Random,
    /** Preserve the existing order */
    Preserve
}

// =============================================================================
// Pipeline Effect Types
// =============================================================================

/**
 * Gather cards from a source and store them in a named collection.
 *
 * This is the first step in a pipeline: it reads cards from a source (library top,
 * zone, or variable) and stores them for subsequent Select/Move steps.
 *
 * @property source Where to get the cards from
 * @property storeAs Name of the collection to store the cards in
 * @property revealed Whether the gathered cards are revealed to all players
 */
@SerialName("GatherCards")
@Serializable
data class GatherCardsEffect(
    val source: CardSource,
    val storeAs: String,
    val revealed: Boolean = false
) : Effect {
    override val description: String = buildString {
        if (revealed) append("Reveal ") else append("Look at ")
        append(source.description)
    }
}

/**
 * Select cards from a named collection, splitting into selected and remainder.
 *
 * This is the middle step in a pipeline: it presents a choice to the player
 * (or auto-selects) and splits the collection into two named groups.
 *
 * @property from Name of the collection to select from
 * @property selection How cards are selected
 * @property filter Optional filter for which cards can be selected
 * @property storeSelected Name to store the selected cards under
 * @property storeRemainder Name to store the non-selected cards under (null = discard remainder info)
 */
@SerialName("SelectFromCollection")
@Serializable
data class SelectFromCollectionEffect(
    val from: String,
    val selection: SelectionMode,
    val filter: GameObjectFilter = GameObjectFilter.Any,
    val storeSelected: String,
    val storeRemainder: String? = null
) : Effect {
    override val description: String = buildString {
        append(selection.description.replaceFirstChar { it.uppercase() })
        append(" from the $from cards")
    }
}

/**
 * Move all cards in a named collection to a destination zone.
 *
 * This is the final step in a pipeline: it takes a named collection and
 * moves all cards in it to the specified destination.
 *
 * @property from Name of the collection to move
 * @property destination Where to move the cards
 * @property order How to order the cards at the destination
 */
@SerialName("MoveCollection")
@Serializable
data class MoveCollectionEffect(
    val from: String,
    val destination: CardDestination,
    val order: CardOrder = CardOrder.Preserve
) : Effect {
    override val description: String = buildString {
        append("Put the $from cards ")
        append(destination.description)
    }
}
