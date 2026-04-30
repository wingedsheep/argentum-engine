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
 * Marks a card in exile as playable by the specified player.
 * Applied by impulse-draw effects like Mind's Desire, Chandra's +1, Act on Impulse.
 * This only grants *permission* to play from exile — it does NOT make the card free.
 * Pair with [PlayWithoutPayingCostComponent] to also waive the mana cost.
 *
 * @param controllerId The player who may play this card from exile.
 * @param permanent If true, this permission persists indefinitely (not cleaned up at end of turn).
 *   Used for "for as long as it remains exiled" effects like Kheru Spellsnatcher / Spelljack.
 * @param expiresAfterTurn If set, the permission persists until the end of the specified turn number
 *   (inclusive). Used for "until the end of your next turn" effects like Muerra, Trash Tactician.
 * @param withAnyManaType If true, mana of any type can be spent to cast this card. Used by effects
 *   like Taster of Wares ("...mana of any type can be spent to cast that spell"). Treats every
 *   colored cost symbol as generic for the purposes of payment when casting from exile.
 */
@Serializable
data class MayPlayFromExileComponent(
    val controllerId: EntityId,
    val permanent: Boolean = false,
    val expiresAfterTurn: Int? = null,
    val withAnyManaType: Boolean = false
) : Component

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
 * Marks a card as playable without paying its mana cost.
 * Applied by effects like Mind's Desire, Cascade, Omniscience.
 * This only waives the mana cost — the card must still be in a zone where
 * it can be played (hand, or exile with [MayPlayFromExileComponent]).
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
 * Used with [MayPlayFromExileComponent] + [PlayWithoutPayingCostComponent] to model
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
 */
@Serializable
data object ExileAfterResolveComponent : Component
