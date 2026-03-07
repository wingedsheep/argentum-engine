package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
import com.wingedsheep.sdk.scripting.text.TextReplacer
import com.wingedsheep.sdk.scripting.values.DynamicAmount
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
        val filter: GameObjectFilter = GameObjectFilter.Companion.Any
    ) : CardSource {
        override val description: String = "${filter.description} cards in ${player.possessive} ${zone.displayName}"
    }

    /**
     * Cards from multiple zones matching an optional filter.
     * Used for "search your graveyard, hand, and/or library" effects.
     */
    @SerialName("FromMultipleZones")
    @Serializable
    data class FromMultipleZones(
        val zones: List<Zone>,
        val player: Player = Player.You,
        val filter: GameObjectFilter = GameObjectFilter.Companion.Any
    ) : CardSource {
        override val description: String = "${filter.description} cards in ${player.possessive} ${zones.joinToString(", ") { it.displayName }}"
    }

    /**
     * Read cards from a previously stored collection variable.
     */
    @SerialName("FromVariable")
    @Serializable
    data class FromVariable(val variableName: String) : CardSource {
        override val description: String = "the $variableName cards"
    }

    /**
     * Permanents controlled by a player (uses projected state to respect control-changing effects).
     */
    @SerialName("ControlledPermanents")
    @Serializable
    data class ControlledPermanents(
        val player: Player = Player.You,
        val filter: GameObjectFilter = GameObjectFilter.Companion.Any
    ) : CardSource {
        override val description: String = "permanents ${player.possessive} control"
    }

    /**
     * All permanents on the battlefield matching a filter (any controller by default).
     * Uses projected state for type/color/keyword checks to correctly handle continuous effects.
     *
     * When [player] is [Player.Each], gathers from all players' battlefields.
     * When [player] is a specific player, gathers only permanents that player controls (projected).
     */
    @SerialName("BattlefieldMatching")
    @Serializable
    data class BattlefieldMatching(
        val filter: GameObjectFilter = GameObjectFilter.Companion.Any,
        val player: Player = Player.Each,
        val excludeSelf: Boolean = false,
        val includeAttachments: Boolean = false
    ) : CardSource {
        override val description: String = buildString {
            if (excludeSelf) append("all other ") else append("all ")
            append("${filter.description} permanents on the battlefield")
            if (includeAttachments) append(" and all permanents attached to them")
            if (player != Player.Each) {
                append(" ${player.possessive} control")
            }
        }
    }

    /**
     * Cards from the source permanent's linked exile (LinkedExileComponent).
     * Returns the entity IDs stored in the component, filtered to only those
     * currently in exile.
     *
     * @property count When set, only gather the first [count] cards from the ordered pile.
     *   When null (default), gather all linked exile cards.
     */
    @SerialName("FromLinkedExile")
    @Serializable
    data class FromLinkedExile(
        val count: Int? = null
    ) : CardSource {
        override val description: String = buildString {
            if (count != null) append("the top $count of ") else append("")
            append("cards exiled by this permanent")
        }
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

    /**
     * Randomly select N cards (no player choice — engine picks randomly).
     * Used for "discard X cards at random" effects.
     */
    @SerialName("Random")
    @Serializable
    data class Random(val count: DynamicAmount) : SelectionMode {
        override val description: String = "random ${count.description}"
    }

    /**
     * Player may select any number of cards (0 to the full collection size).
     * Used for "put any number of cards" effects like Tempting Wurm.
     */
    @SerialName("ChooseAnyNumber")
    @Serializable
    data object ChooseAnyNumber : SelectionMode {
        override val description: String = "any number"
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
    TargetPlayer,
    /** The triggering player decides (resolved from trigger context) */
    TriggeringPlayer
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

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
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
    val filter: GameObjectFilter = GameObjectFilter.Companion.Any,
    val storeSelected: String,
    val storeRemainder: String? = null,
    /**
     * When true, additionally filter eligible cards to only those with the
     * creature type stored in the effect context's chosenCreatureType.
     * Used for "creature card of the chosen type" patterns.
     */
    val matchChosenCreatureType: Boolean = false,
    /** Custom prompt for the selection decision. If null, a generic prompt is generated. */
    val prompt: String? = null,
    /** Label describing where selected cards go (e.g., "Put on bottom"). Shown in the UI. */
    val selectedLabel: String? = null,
    /** Label describing where non-selected cards go (e.g., "Put on top"). Shown in the UI. */
    val remainderLabel: String? = null
) : Effect {
    override val description: String = buildString {
        if (chooser == Chooser.Opponent) append("An opponent ")
        append(selection.description.replaceFirstChar { it.uppercase() })
        append(" from the $from cards")
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
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
    Discard,
    /**
     * Sacrifice — emits PermanentsSacrificedEvent in addition to ZoneChangeEvent.
     * Cards are routed to their owner's graveyard (not necessarily the destination player),
     * matching MTG rule 701.16a: sacrificed permanents go to their owner's graveyard.
     */
    Sacrifice,
    /**
     * Destroy — respects indestructible (skips those permanents) and regeneration shields.
     * Cards are routed to their owner's graveyard.
     * Destination should be Graveyard; the executor enforces owner routing.
     */
    Destroy
}

@SerialName("MoveCollection")
@Serializable
data class MoveCollectionEffect(
    val from: String,
    val destination: CardDestination,
    val order: CardOrder = CardOrder.Preserve,
    val revealed: Boolean = false,
    val moveType: MoveType = MoveType.Default,
    val linkToSource: Boolean = false,
    val unlinkFromSource: Boolean = false,
    val faceDown: Boolean = false,
    val noRegenerate: Boolean = false,
    val storeMovedAs: String? = null,
    val underOwnersControl: Boolean = false
) : Effect {
    override val description: String = buildString {
        if (revealed) append("Reveal and put") else append("Put")
        append(" the $from cards ")
        append(destination.description)
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
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

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newFilter = matchFilter.applyTextReplacement(replacer)
        return if (newFilter !== matchFilter) copy(matchFilter = newFilter) else this
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

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

/**
 * The type of option to choose from in a ChooseOptionEffect.
 */
@Serializable
enum class OptionType {
    /** Choose from all creature types */
    CREATURE_TYPE,
    /** Choose from the five Magic colors */
    COLOR
}

/**
 * Generic pipeline step that presents a choice from a set of options and stores
 * the result in a named variable (via EffectContext.chosenValues).
 *
 * This is the generic replacement for type-specific choice effects. Downstream
 * pipeline effects can read the chosen value from chosenValues[storeAs].
 *
 * @property optionType What kind of options to present (creature types, colors, etc.)
 * @property storeAs Key under which the chosen value is stored in EffectContext.chosenValues
 * @property prompt Custom prompt text. If null, a default is generated from the option type.
 * @property excludedOptions Options to exclude from the presented list
 */
@SerialName("ChooseOption")
@Serializable
data class ChooseOptionEffect(
    val optionType: OptionType,
    val storeAs: String = "chosenOption",
    val prompt: String? = null,
    val excludedOptions: List<String> = emptyList()
) : Effect {
    override val description: String = buildString {
        append("Choose ")
        append(when (optionType) {
            OptionType.CREATURE_TYPE -> "a creature type"
            OptionType.COLOR -> "a color"
        })
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

/**
 * Each player (in APNAP order) chooses a creature type.
 * All chosen types are accumulated and stored as a List<String> under [storeAs]
 * in the effect context's storedStringLists.
 *
 * Used for cards like Harsh Mercy and similar "each player chooses a creature type" effects.
 *
 * @property storeAs Key under which the accumulated list of chosen types is stored
 */
@SerialName("EachPlayerChoosesCreatureType")
@Serializable
data class EachPlayerChoosesCreatureTypeEffect(
    val storeAs: String
) : Effect {
    override val description: String = "Each player chooses a creature type"

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

/**
 * Select a target during effect resolution (mid-pipeline targeting).
 *
 * Unlike cast-time targeting (TargetRequirement on spells/abilities), this step
 * selects targets at resolution time as part of a pipeline. The selected target
 * entity IDs are stored in [storedCollections][storeAs] for use by subsequent
 * pipeline effects via [com.wingedsheep.sdk.scripting.targets.EffectTarget.PipelineTarget].
 *
 * **Important:** This is for non-targeting choices ("choose a creature") or for
 * targets that depend on earlier pipeline results. Do NOT use this for cards whose
 * oracle text says "target" — those must use cast-time targeting so the target is
 * declared when the spell/ability goes on the stack, giving opponents a chance to
 * respond with knowledge of what is being targeted. Mid-resolution selection happens
 * after priority has passed and cannot be responded to.
 *
 * @property requirement The target requirement (reuses all existing TargetRequirement types)
 * @property storeAs Name of the collection to store the selected target IDs in
 */
@SerialName("SelectTarget")
@Serializable
data class SelectTargetEffect(
    val requirement: TargetRequirement,
    val storeAs: String = "pipelineTarget"
) : Effect {
    override val description: String = "Choose ${requirement.description}"

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newRequirement = requirement.applyTextReplacement(replacer)
        return if (newRequirement !== requirement) copy(requirement = newRequirement) else this
    }
}

/**
 * Grant "may play from exile" permission to all cards in a named collection.
 * The cards must already be in exile. This adds a MayPlayFromExileComponent,
 * allowing the controller to play them as if they were in hand until end of turn.
 *
 * Does NOT waive the mana cost — pair with [GrantPlayWithoutPayingCostEffect]
 * to also make them free. Used by impulse-draw effects (Chandra, Act on Impulse, etc.).
 *
 * @property from Name of the collection containing the exiled card(s)
 */
@SerialName("GrantMayPlayFromExile")
@Serializable
data class GrantMayPlayFromExileEffect(
    val from: String
) : Effect {
    override val description: String =
        "Until end of turn, you may play the $from cards from exile"

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

/**
 * Conditionally execute an effect based on whether a named collection is non-empty.
 *
 * Used for "if you do" effects where a subsequent action depends on whether
 * a previous pipeline step actually moved/selected any cards.
 *
 * @property collection Name of the collection to check
 * @property ifNotEmpty Effect to execute if the collection has at least one card
 * @property ifEmpty Effect to execute if the collection is empty (optional)
 */
@SerialName("ConditionalOnCollection")
@Serializable
data class ConditionalOnCollectionEffect(
    val collection: String,
    val ifNotEmpty: Effect,
    val ifEmpty: Effect? = null
) : Effect {
    override val description: String = buildString {
        append("If $collection is not empty, ")
        append(ifNotEmpty.description.replaceFirstChar { it.lowercase() })
        if (ifEmpty != null) {
            append(". Otherwise, ")
            append(ifEmpty.description.replaceFirstChar { it.lowercase() })
        }
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newIfNotEmpty = ifNotEmpty.applyTextReplacement(replacer)
        val newIfEmpty = ifEmpty?.applyTextReplacement(replacer)
        return if (newIfNotEmpty !== ifNotEmpty || newIfEmpty !== ifEmpty)
            copy(ifNotEmpty = newIfNotEmpty, ifEmpty = newIfEmpty) else this
    }
}

/**
 * Grant "play without paying mana cost" permission to all cards in a named
 * collection until end of turn. Adds a PlayWithoutPayingCostComponent.
 *
 * The card must still be in a playable zone (hand, or exile with
 * [GrantMayPlayFromExileEffect]). Used by Mind's Desire, Cascade, etc.
 *
 * @property from Name of the collection containing the card(s)
 */
@SerialName("GrantPlayWithoutPayingCost")
@Serializable
data class GrantPlayWithoutPayingCostEffect(
    val from: String
) : Effect {
    override val description: String =
        "Until end of turn, you may play the $from cards without paying their mana costs"

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

// =============================================================================
// Collection Filtering
// =============================================================================

/**
 * How to filter a named collection.
 */
@Serializable
sealed interface CollectionFilter {
    /**
     * Exclude entities that have any subtype matching a stored string list.
     * Reads the list from [storedKey] in the effect context's storedStringLists.
     */
    @SerialName("ExcludeSubtypesFromStored")
    @Serializable
    data class ExcludeSubtypesFromStored(val storedKey: String) : CollectionFilter

    /**
     * Keep only entities that share at least one subtype with the sacrificed creature.
     * Reads subtypes from context.sacrificedPermanentSubtypes (snapshotted at sacrifice time).
     */
    @SerialName("SharesSubtypeWithSacrificed")
    @Serializable
    data object SharesSubtypeWithSacrificed : CollectionFilter

    /**
     * Keep only entities that match a [GameObjectFilter] using projected state.
     * This is the general-purpose filter for any predicate-based collection filtering.
     */
    @SerialName("MatchesFilter")
    @Serializable
    data class MatchesFilter(val filter: GameObjectFilter) : CollectionFilter

    /**
     * Keep only creatures with the greatest power in the collection.
     * Uses projected state to determine power (respects continuous effects).
     * If multiple creatures are tied for the greatest power, all of them are kept.
     *
     * Used for Crackling Doom and similar "sacrifice the creature with the greatest power" effects.
     */
    @SerialName("GreatestPower")
    @Serializable
    data object GreatestPower : CollectionFilter
}

/**
 * Filter a named collection, splitting it into matching and non-matching subsets.
 *
 * This is a purely automatic filter (no player choice). The matching entities
 * are stored in [storeMatching]; non-matching entities are stored in [storeNonMatching]
 * if provided.
 *
 * @property from Name of the collection to filter
 * @property filter How to filter the collection
 * @property storeMatching Name of the collection to store entities that pass the filter
 * @property storeNonMatching Optional name to store entities that fail the filter
 */
@SerialName("FilterCollection")
@Serializable
data class FilterCollectionEffect(
    val from: String,
    val filter: CollectionFilter,
    val storeMatching: String,
    val storeNonMatching: String? = null
) : Effect {
    override val description: String = "Filter $from collection"
    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}
