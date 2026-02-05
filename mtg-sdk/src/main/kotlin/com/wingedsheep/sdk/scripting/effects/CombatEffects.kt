package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.Color
import kotlinx.serialization.Serializable

// =============================================================================
// Combat Effects
// =============================================================================

/**
 * All creatures that can block target creature must do so.
 * "All creatures able to block target creature this turn do so."
 */
@Serializable
data class MustBeBlockedEffect(
    val target: EffectTarget
) : Effect {
    override val description: String = "All creatures able to block ${target.description} this turn do so"
}

/**
 * Force creatures to attack during target player's next turn.
 * Used for Taunt: "During target player's next turn, creatures that player controls attack you if able."
 */
@Serializable
data class TauntEffect(
    val target: EffectTarget = EffectTarget.PlayerRef(Player.TargetPlayer)
) : Effect {
    override val description: String =
        "During ${target.description}'s next turn, creatures they control attack you if able"
}

/**
 * Creates a delayed trigger for the rest of the turn that reflects combat damage.
 * "This turn, whenever an attacking creature deals combat damage to you,
 *  it deals that much damage to its controller."
 * Used for Harsh Justice.
 *
 * The engine implements this by creating a temporary triggered ability that
 * listens for combat damage events and applies reflection.
 */
@Serializable
data class ReflectCombatDamageEffect(
    val target: EffectTarget = EffectTarget.Controller
) : Effect {
    override val description: String =
        "This turn, whenever an attacking creature deals combat damage to you, " +
                "it deals that much damage to its controller"
}

/**
 * Prevents combat damage that would be dealt by specified creatures.
 * "Prevent all combat damage that would be dealt by creatures you don't control."
 * Used for Fog-type effects with creature restrictions.
 */
@Serializable
data class PreventCombatDamageFromEffect(
    val source: GroupFilter,
    val duration: Duration = Duration.EndOfTurn
) : Effect {
    override val description: String =
        "Prevent all combat damage that would be dealt by ${source.description.replaceFirstChar { it.lowercase() }}"
}

/**
 * Prevent all damage that would be dealt to you this turn by attacking creatures.
 * Used for Deep Wood: "Prevent all damage that would be dealt to you this turn by attacking creatures."
 */
@Serializable
data object PreventDamageFromAttackingCreaturesThisTurnEffect : Effect {
    override val description: String = "Prevent all damage that would be dealt to you this turn by attacking creatures"
}

/**
 * Grant evasion to a group of creatures until end of turn.
 * "Black creatures you control can't be blocked this turn except by black creatures."
 * Used for Dread Charge.
 *
 * @property filter Which creatures gain the evasion
 * @property canOnlyBeBlockedByColor The color of creatures that can block them
 * @property duration How long the effect lasts
 */
@Serializable
data class GrantCantBeBlockedExceptByColorEffect(
    val filter: GroupFilter,
    val canOnlyBeBlockedByColor: Color,
    val duration: Duration = Duration.EndOfTurn
) : Effect {
    override val description: String = buildString {
        append("${filter.description} can't be blocked")
        if (duration.description.isNotEmpty()) append(" ${duration.description}")
        append(" except by ${canOnlyBeBlockedByColor.displayName.lowercase()} creatures")
    }
}
