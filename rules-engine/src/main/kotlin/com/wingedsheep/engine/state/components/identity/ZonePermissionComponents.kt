package com.wingedsheep.engine.state.components.identity

import com.wingedsheep.engine.state.Component
import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.Serializable

/**
 * Tracks which players have been revealed this card's identity.
 * Used for "look at opponent's hand" effects where the viewing player
 * should continue to see those cards even though they're in a hidden zone.
 */
@Serializable
data class RevealedToComponent(
    val playerIds: Set<EntityId>
) : Component {
    fun withPlayer(playerId: EntityId): RevealedToComponent =
        copy(playerIds = playerIds + playerId)

    fun isRevealedTo(playerId: EntityId): Boolean = playerId in playerIds

    companion object {
        fun to(playerId: EntityId): RevealedToComponent =
            RevealedToComponent(setOf(playerId))
    }
}

/**
 * Marks a card in exile as castable via its warp ability.
 * Applied when a warped permanent is exiled by the warp end-step trigger.
 * The card can be re-cast for its warp cost from exile, and the warp loop continues.
 *
 * @param controllerId The player who may cast this card from exile via warp.
 */
@Serializable
data class WarpExiledComponent(
    val controllerId: EntityId
) : Component

/**
 * Marks a card in exile as "plotted" (CR 718, Outlaws of Thunder Junction).
 *
 * Applied when a player pays the plot cost and exiles a card from their hand via the
 * Plot keyword's special action. Carries [turnPlotted] = `state.turnNumber` at the
 * moment the card was plotted, so the `SourcePlottedOnPriorTurn` condition gating
 * the cast-from-exile permission can enforce CR 718.2 ("you can't cast a plotted
 * card on the same turn it became plotted").
 *
 * @param controllerId The player who plotted the card.
 * @param turnPlotted The `GameState.turnNumber` on which the card was plotted.
 */
@Serializable
data class PlottedComponent(
    val controllerId: EntityId,
    val turnPlotted: Int,
) : Component

/**
 * Marks a card as playable without paying its mana cost.
 * Applied by effects like Mind's Desire, Cascade, Omniscience.
 * This only waives the mana cost — the card must still be in a zone where it can
 * be played (hand, or exile with a may-play permission).
 *
 * @param controllerId The player who may play this card for free.
 * @param permanent If true, this permission persists indefinitely (not cleaned up at end of turn).
 *   Used for "for as long as it remains exiled" effects like Kheru Spellsnatcher / Spelljack.
 */
@Serializable
data class PlayWithoutPayingCostComponent(
    val controllerId: EntityId,
    val permanent: Boolean = false
) : Component

/**
 * Marks a card in exile as requiring an additional cost when cast.
 * Used with a may-play permission + [PlayWithoutPayingCostComponent] to model
 * "may cast by paying [cost] rather than its mana cost" — the mana is waived,
 * but this non-mana cost must still be paid.
 *
 * Used by The Infamous Cruelclaw ("may cast that card by discarding a card
 * rather than paying its mana cost").
 *
 * @param controllerId The player who has this cost obligation when casting.
 * @param additionalCosts The costs that must be paid (e.g., discard a card).
 */
@Serializable
data class PlayWithAdditionalCostComponent(
    val controllerId: EntityId,
    val additionalCosts: List<com.wingedsheep.sdk.scripting.AdditionalCost>
) : Component

/**
 * Marks a card in exile as costing more to cast for the specified player.
 * Used by effects like Soul Partition that let a card's owner play it from exile
 * but tax opponents of the effect's controller.
 *
 * @param controllerId The player who has this cost increase when casting.
 * @param amount Generic mana added to the spell's cost.
 */
@Serializable
data class PlayWithCostIncreaseComponent(
    val controllerId: EntityId,
    val amount: Int
) : Component

/**
 * Marks a spell so that if it would be put into a graveyard after resolving or being
 * countered, it is exiled instead. Used by effects like Daring Waverider that grant
 * one-shot free casts from exile with "exile it instead" clauses.
 *
 * The [StackResolver] checks for this component when determining the destination zone
 * after a spell resolves or fizzles.
 *
 * @param withCounters Counter types to add to the card after it is exiled (one of each).
 *   Used by Goliath Daydreamer's "exile that card with a dream counter on it" wording.
 * @param linkedSourceId When set, the spell is added to this permanent's
 *   [com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent] once
 *   it actually lands in exile, so the UI can show the exiled cards tethered under
 *   the source permanent (e.g. Goliath Daydreamer's "exile that card with a dream
 *   counter on it" pile). Only applied when the spell actually resolves into exile.
 * @param makePlotted When true, the card becomes *plotted* (CR 718) for its owner once it lands
 *   in exile — the same effect as [com.wingedsheep.sdk.scripting.effects.MakePlottedEffect], but
 *   applied to a self-cast spell as it resolves rather than to a targeted spell. Used by Lilah,
 *   Undefeated Slickshot: "exile that spell instead of putting it into your graveyard as it
 *   resolves. If you do, it becomes plotted." Only applied when the card actually lands in exile.
 */
@Serializable
data class ExileAfterResolveComponent(
    val withCounters: List<com.wingedsheep.sdk.core.CounterType> = emptyList(),
    /**
     * When true, the spell only goes to exile if it actually resolves. If it is
     * countered or fizzles, it goes to its owner's graveyard normally. Used by
     * Goliath Daydreamer per ruling: "If a spell is countered or otherwise fails
     * to resolve, Goliath Daydreamer's first ability won't exile it."
     */
    val onlyIfResolved: Boolean = false,
    val linkedSourceId: EntityId? = null,
    val makePlotted: Boolean = false
) : Component
