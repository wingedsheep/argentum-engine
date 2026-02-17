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
     * Select all cards (no choice needed).
     */
    @SerialName("SelectAll")
    @Serializable
    data object All : SelectionMode {
        override val description: String = "all"
    }
}

/**
 * Who makes the selection decision.
 */
@Serializable
enum class Chooser {
    /** The controller of the spell/ability decides */
    Controller,
    /** An opponent decides */
    Opponent,
    /** The target player decides (resolved from context.targets[0]) */
    TargetPlayer
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
 * @property selection How many cards are selected (exactly N, up to N, all)
 * @property chooser Who makes the selection (controller or opponent)
 * @property filter Optional filter for which cards can be selected
 * @property storeSelected Name to store the selected cards under
 * @property storeRemainder Name to store the non-selected cards under (null = discard remainder info)
 */
@SerialName("SelectFromCollection")
@Serializable
data class SelectFromCollectionEffect(
    val from: String,
    val selection: SelectionMode,
    val chooser: Chooser = Chooser.Controller,
    val filter: GameObjectFilter = GameObjectFilter.Any,
    val storeSelected: String,
    val storeRemainder: String? = null,
    /**
     * When true, additionally filter eligible cards to only those with the
     * creature type stored in the effect context's chosenCreatureType.
     * Used for "creature card of the chosen type" patterns.
     */
    val matchChosenCreatureType: Boolean = false
) : Effect {
    override val description: String = buildString {
        if (chooser == Chooser.Opponent) append("An opponent ")
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
/**
 * How the move should be categorized for event emission.
 */
@Serializable
enum class MoveType {
    /** Standard zone change — emits only ZoneChangeEvent */
    Default,
    /** Discard — also emits CardsDiscardedEvent */
    Discard
}

@SerialName("MoveCollection")
@Serializable
data class MoveCollectionEffect(
    val from: String,
    val destination: CardDestination,
    val order: CardOrder = CardOrder.Preserve,
    val revealed: Boolean = false,
    val moveType: MoveType = MoveType.Default
) : Effect {
    override val description: String = buildString {
        if (revealed) append("Reveal and put") else append("Put")
        append(" the $from cards ")
        append(destination.description)
    }
}

/**
 * Reveal cards from the top of a player's library until a card matching
 * the filter is found. The matching card is stored in [storeMatch] and
 * ALL revealed cards (including the match) are stored in [storeRevealed].
 *
 * If no matching card is found (entire library is revealed), [storeMatch] will
 * be empty and [storeRevealed] will contain all revealed cards.
 *
 * @property source Whose library to reveal from
 * @property matchFilter The filter that stops the reveal (e.g., GameObjectFilter.Nonland)
 * @property storeMatch Name to store the matching card under
 * @property storeRevealed Name to store ALL revealed cards under (including the match)
 */
@SerialName("RevealUntil")
@Serializable
data class RevealUntilEffect(
    val source: Player = Player.You,
    val matchFilter: GameObjectFilter,
    val storeMatch: String,
    val storeRevealed: String,
    /**
     * When true, the match condition additionally requires the card to be a creature
     * with the subtype stored in the effect context's chosenCreatureType.
     * Used for "reveal until you reveal a creature card of the chosen type" patterns.
     */
    val matchChosenCreatureType: Boolean = false
) : Effect {
    override val description: String = buildString {
        append("Reveal cards from the top of ")
        append(source.possessive)
        append(" library until you reveal a ${matchFilter.description} card")
        if (matchChosenCreatureType) append(" of the chosen type")
    }
}

/**
 * Choose a creature type. Stores the chosen type in the effect context
 * for use by subsequent pipeline steps (e.g., RevealUntilEffect with
 * matchChosenCreatureType = true).
 *
 * This is a pipeline step that pauses for player input.
 */
@SerialName("ChooseCreatureType")
@Serializable
data object ChooseCreatureTypeEffect : Effect {
    override val description: String = "Choose a creature type"
}
