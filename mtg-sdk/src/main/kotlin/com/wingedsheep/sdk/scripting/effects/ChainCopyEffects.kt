package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The primary action to execute before offering the chain copy.
 */
@Serializable
sealed interface ChainAction {
    @SerialName("ChainAction.Destroy")
    @Serializable
    data object Destroy : ChainAction

    @SerialName("ChainAction.BounceToHand")
    @Serializable
    data object BounceToHand : ChainAction

    @SerialName("ChainAction.DealDamage")
    @Serializable
    data class DealDamage(val amount: Int) : ChainAction

    @SerialName("ChainAction.Discard")
    @Serializable
    data class Discard(val count: Int) : ChainAction

    @SerialName("ChainAction.PreventAllDamageDealt")
    @Serializable
    data object PreventAllDamageDealt : ChainAction
}

/**
 * Who gets offered the copy of the chain spell.
 */
@Serializable
enum class CopyRecipient {
    /** Controller of the targeted permanent (Destroy, Bounce, PreventDamage) */
    TARGET_CONTROLLER,
    /** The target player directly (Discard — target IS a player) */
    TARGET_PLAYER,
    /** The "affected player" — the target if it's a player, or its controller if a permanent (Damage) */
    AFFECTED_PLAYER
}

/**
 * Cost to pay for creating the chain copy.
 */
@Serializable
sealed interface ChainCopyCost {
    @SerialName("ChainCopyCost.NoCost")
    @Serializable
    data object NoCost : ChainCopyCost

    @SerialName("ChainCopyCost.SacrificeALand")
    @Serializable
    data object SacrificeALand : ChainCopyCost

    @SerialName("ChainCopyCost.DiscardACard")
    @Serializable
    data object DiscardACard : ChainCopyCost
}

/**
 * Unified chain copy effect for all "Chain of X" cards from Onslaught.
 *
 * Executes a primary action on the target, then offers a specific player the option
 * to copy the spell (optionally paying a cost) and choose a new target.
 *
 * @property action The primary action to execute (destroy, bounce, damage, discard, prevent damage)
 * @property target The target of the primary action
 * @property targetFilter The filter for valid targets (used for permanent-targeting chains)
 * @property copyRecipient Who gets offered the copy
 * @property copyCost Cost required to create the copy
 * @property copyTargetRequirement Target requirement for the copy's new target
 * @property spellName The name of the spell (for display and copy descriptions)
 */
@SerialName("ChainCopy")
@Serializable
data class ChainCopyEffect(
    val action: ChainAction,
    val target: EffectTarget,
    val targetFilter: TargetFilter? = null,
    val copyRecipient: CopyRecipient,
    val copyCost: ChainCopyCost,
    val copyTargetRequirement: TargetRequirement,
    val spellName: String
) : Effect {
    override val description: String = buildString {
        when (action) {
            is ChainAction.Destroy -> {
                append("Destroy ${target.description}. ")
                append("Then that permanent's controller may copy this spell ")
                append("and may choose a new target for that copy")
            }
            is ChainAction.BounceToHand -> {
                append("Return ${target.description} to its owner's hand. ")
                append("Then that permanent's controller may sacrifice a land. ")
                append("If the player does, they may copy this spell and may choose a new target for that copy")
            }
            is ChainAction.DealDamage -> {
                append("Deal ${action.amount} damage to ${target.description}. ")
                append("Then that player or that permanent's controller may discard a card. ")
                append("If the player does, they may copy this spell and may choose a new target for that copy")
            }
            is ChainAction.Discard -> {
                val cardText = if (action.count == 1) "a card" else "${action.count} cards"
                append("Target player discards $cardText. ")
                append("That player may copy this spell and may choose a new target for that copy")
            }
            is ChainAction.PreventAllDamageDealt -> {
                append("Prevent all damage ${target.description} would deal this turn. ")
                append("That creature's controller may sacrifice a land. ")
                append("If the player does, they may copy this spell and may choose a new target for that copy")
            }
        }
    }
}
