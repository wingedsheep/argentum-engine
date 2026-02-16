package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.Color
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Combat Effects
// =============================================================================

/**
 * All creatures that can block target creature must do so.
 * "All creatures able to block target creature this turn do so."
 */
@SerialName("MustBeBlocked")
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
@SerialName("Taunt")
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
@SerialName("ReflectCombatDamage")
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
@SerialName("PreventCombatDamageFrom")
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
@SerialName("PreventDamageFromAttackingCreatures")
@Serializable
data object PreventDamageFromAttackingCreaturesThisTurnEffect : Effect {
    override val description: String = "Prevent all damage that would be dealt to you this turn by attacking creatures"
}

/**
 * Prevent all combat damage that would be dealt this turn.
 * Used for Leery Fogbeast: "Whenever this creature becomes blocked, prevent all combat damage that would be dealt this turn."
 */
@SerialName("PreventAllCombatDamage")
@Serializable
data object PreventAllCombatDamageThisTurnEffect : Effect {
    override val description: String = "Prevent all combat damage that would be dealt this turn"
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
@SerialName("GrantCantBeBlockedExceptByColor")
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

/**
 * Target creatures can't block this turn.
 * Used for Wave of Indifference: "X target creatures can't block this turn."
 *
 * Works with multi-target spells - applies "can't block" to all creatures
 * in context.targets by creating a floating effect until end of turn.
 */
@SerialName("CantBlockTargetCreatures")
@Serializable
data class CantBlockTargetCreaturesEffect(
    val duration: Duration = Duration.EndOfTurn
) : Effect {
    override val description: String = "Target creatures can't block this turn"
}

/**
 * Prevent the next X damage that would be dealt to target creature this turn.
 * Used for Battlefield Medic and similar damage prevention effects.
 *
 * @property amount The amount of damage to prevent (can be dynamic, e.g., number of Clerics)
 * @property target The creature receiving the prevention shield
 */
@SerialName("PreventNextDamage")
@Serializable
data class PreventNextDamageEffect(
    val amount: DynamicAmount,
    val target: EffectTarget
) : Effect {
    override val description: String =
        "Prevent the next ${amount.description} damage that would be dealt to ${target.description} this turn"
}

/**
 * Remove a creature from combat.
 * Removes all combat-related components (attacking, blocking, blocked, damage assignment).
 * Also cleans up any blockers that were blocking the removed creature.
 * Used for Gustcloak creatures: "you may untap it and remove it from combat."
 */
@SerialName("RemoveFromCombat")
@Serializable
data class RemoveFromCombatEffect(
    val target: EffectTarget
) : Effect {
    override val description: String = "Remove ${target.description} from combat"
}

/**
 * Choose a creature type, then creatures of that type attack this turn if able.
 * Used for Walking Desecration: "{B}, {T}: Creatures of the creature type of your choice attack this turn if able."
 *
 * The controller chooses a creature type at resolution, then all creatures of that type
 * on the battlefield are marked as "must attack this turn if able."
 */
@SerialName("ChooseCreatureTypeMustAttack")
@Serializable
data object ChooseCreatureTypeMustAttackEffect : Effect {
    override val description: String =
        "Creatures of the creature type of your choice attack this turn if able"
}

/**
 * Redirect the next time damage would be dealt to the protected targets this turn.
 * "The next time damage would be dealt to [protected targets] this turn,
 *  that damage is dealt to [redirectTo] instead."
 * Used for Glarecaster and similar redirection effects.
 *
 * @property protectedTargets The entities protected by this redirection shield (e.g., Self + Controller)
 * @property redirectTo The target that will receive the redirected damage (chosen at activation)
 */
@SerialName("RedirectNextDamage")
@Serializable
data class RedirectNextDamageEffect(
    val protectedTargets: List<EffectTarget>,
    val redirectTo: EffectTarget
) : Effect {
    override val description: String =
        "The next time damage would be dealt to ${protectedTargets.joinToString(" and/or ") { it.description }} this turn, " +
                "that damage is dealt to ${redirectTo.description} instead"
}

/**
 * Prevent the next time a creature of the chosen type would deal damage to you this turn.
 * Used for Circle of Solace: "{1}{W}: The next time a creature of the chosen type would
 * deal damage to you this turn, prevent that damage."
 *
 * Reads the chosen creature type from the source permanent's ChosenCreatureTypeComponent
 * and creates a prevention shield on the controller.
 */
@SerialName("PreventNextDamageFromChosenCreatureType")
@Serializable
data object PreventNextDamageFromChosenCreatureTypeEffect : Effect {
    override val description: String =
        "The next time a creature of the chosen type would deal damage to you this turn, prevent that damage"
}

/**
 * Prevent all damage target creature would deal this turn, then its controller may
 * sacrifice a land to copy this spell and may choose a new target for that copy.
 * Used for Chain of Silence.
 *
 * @property target The creature whose damage is prevented
 * @property targetFilter The filter for valid targets (used when creating copies)
 * @property spellName The name of the spell (for the copy's description on the stack)
 */
@SerialName("PreventDamageAndChainCopy")
@Serializable
data class PreventDamageAndChainCopyEffect(
    val target: EffectTarget,
    val targetFilter: TargetFilter,
    val spellName: String
) : Effect {
    override val description: String = buildString {
        append("Prevent all damage ${target.description} would deal this turn. ")
        append("That creature's controller may sacrifice a land. ")
        append("If the player does, they may copy this spell and may choose a new target for that copy")
    }
}
