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
    val target: EffectTarget = EffectTarget.AnyPlayer
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
    val source: CreatureGroupFilter,
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
 * @property filter Which creatures gain the evasion (deprecated, use unifiedFilter)
 * @property canOnlyBeBlockedByColor The color of creatures that can block them
 * @property unifiedFilter Unified group filter (preferred)
 */
@Serializable
data class GrantCantBeBlockedExceptByColorEffect(
    @Deprecated("Use unifiedFilter instead")
    val filter: CreatureGroupFilter = CreatureGroupFilter.All,
    val canOnlyBeBlockedByColor: Color,
    val duration: Duration = Duration.EndOfTurn,
    val unifiedFilter: GroupFilter? = null
) : Effect {
    override val description: String = buildString {
        val filterDesc = unifiedFilter?.description ?: filter.description
        append("$filterDesc can't be blocked")
        if (duration.description.isNotEmpty()) append(" ${duration.description}")
        append(" except by ${canOnlyBeBlockedByColor.displayName.lowercase()} creatures")
    }

    companion object {
        /** Create with unified filter */
        operator fun invoke(
            unifiedFilter: GroupFilter,
            canOnlyBeBlockedByColor: Color,
            duration: Duration = Duration.EndOfTurn
        ) = GrantCantBeBlockedExceptByColorEffect(
            filter = CreatureGroupFilter.All,
            canOnlyBeBlockedByColor = canOnlyBeBlockedByColor,
            duration = duration,
            unifiedFilter = unifiedFilter
        )
    }
}
