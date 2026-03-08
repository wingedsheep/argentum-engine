package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.text.TextReplacer
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Combat Effects
// =============================================================================

/**
 * Provoke effect: untap target creature and force it to block the source creature if able.
 * "You may have target creature defending player controls untap and block it if able."
 *
 * @property target The creature to provoke (untap and force to block)
 */
@SerialName("Provoke")
@Serializable
data class ProvokeEffect(
    val target: EffectTarget = EffectTarget.ContextTarget(0)
) : Effect {
    override val description: String = "Target creature defending player controls untaps and blocks ${target.description} if able"

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

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

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
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

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
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

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
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

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newSource = source.applyTextReplacement(replacer)
        return if (newSource !== source) copy(source = newSource) else this
    }
}

/**
 * Prevent all damage that would be dealt to you this turn by attacking creatures.
 * Used for Deep Wood: "Prevent all damage that would be dealt to you this turn by attacking creatures."
 */
@SerialName("PreventDamageFromAttackingCreatures")
@Serializable
data object PreventDamageFromAttackingCreaturesThisTurnEffect : Effect {
    override val description: String = "Prevent all damage that would be dealt to you this turn by attacking creatures"

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

/**
 * Prevent all combat damage that would be dealt this turn.
 * Used for Leery Fogbeast: "Whenever this creature becomes blocked, prevent all combat damage that would be dealt this turn."
 */
@SerialName("PreventAllCombatDamage")
@Serializable
data object PreventAllCombatDamageThisTurnEffect : Effect {
    override val description: String = "Prevent all combat damage that would be dealt this turn"

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
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

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * Force a target creature to block a specific attacker this combat if able.
 * Unlike Provoke, this does NOT untap the target creature.
 * "Target creature defending player controls blocks this creature this combat if able."
 *
 * @property target The creature forced to block
 */
@SerialName("ForceBlock")
@Serializable
data class ForceBlockEffect(
    val target: EffectTarget = EffectTarget.ContextTarget(0)
) : Effect {
    override val description: String = "Target creature blocks ${target.description} this combat if able"

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

/**
 * All creatures matching a filter can't block this turn.
 * Used for Barrage of Boulders: "creatures can't block this turn."
 *
 * Unlike CantBlockTargetCreaturesEffect which operates on targeted creatures,
 * this applies to all creatures matching a filter on the battlefield at resolution.
 *
 * @property filter Which creatures can't block (e.g., GroupFilter.AllCreatures)
 * @property duration How long the restriction lasts
 */
@SerialName("CantBlockGroup")
@Serializable
data class CantBlockGroupEffect(
    val filter: GroupFilter,
    val duration: Duration = Duration.EndOfTurn
) : Effect {
    override val description: String = "${filter.description} can't block this turn"

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
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

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
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

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newAmount = amount.applyTextReplacement(replacer)
        return if (newAmount !== amount) copy(amount = newAmount) else this
    }
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

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

/**
 * Mark a creature as "must attack this turn if able."
 * Adds MustAttackThisTurnComponent to the target entity.
 *
 * Used within ForEachInGroupEffect pipelines to mark groups of creatures.
 *
 * @property target The creature to mark (typically EffectTarget.Self within ForEachInGroup)
 */
@SerialName("MarkMustAttackThisTurn")
@Serializable
data class MarkMustAttackThisTurnEffect(
    val target: EffectTarget = EffectTarget.Self
) : Effect {
    override val description: String = "${target.description} attacks this turn if able"

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
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
    val redirectTo: EffectTarget,
    /** If set, only redirect up to this many damage (e.g., "the next 1 damage"). Null = redirect all. */
    val amount: Int? = null
) : Effect {
    override val description: String = buildString {
        append("The next ")
        if (amount != null) append("$amount ")
        append("damage that would be dealt to ${protectedTargets.joinToString(" and/or ") { it.description }} this turn")
        append(" is dealt to ${redirectTo.description} instead")
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
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

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

/**
 * Prevent all combat damage that would be dealt to and dealt by a creature this turn.
 * Used for Deftblade Elite and similar effects.
 *
 * @property target The creature whose combat damage (both dealing and receiving) is prevented
 */
@SerialName("PreventCombatDamageToAndBy")
@Serializable
data class PreventCombatDamageToAndByEffect(
    val target: EffectTarget = EffectTarget.Self
) : Effect {
    override val description: String =
        "Prevent all combat damage that would be dealt to and dealt by ${target.description} this turn"

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

/**
 * Prevent all damage target creature or spell would deal this turn.
 * Used for Shieldmage Elder and similar "prevent all damage target would deal" effects.
 *
 * Creates a floating effect (PreventAllDamageDealtBy) on the target entity.
 * Works for both creatures (prevents combat and non-combat damage) and spells on the stack.
 *
 * @property target The creature or spell whose damage is prevented
 */
@SerialName("PreventAllDamageDealtByTarget")
@Serializable
data class PreventAllDamageDealtByTargetEffect(
    val target: EffectTarget
) : Effect {
    override val description: String =
        "Prevent all damage ${target.description} would deal this turn"

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

/**
 * Grant a creature "can't attack or block unless its controller pays {X} for each [creature type]
 * on the battlefield" until end of turn.
 * Used for Whipgrass Entangler and similar effects.
 *
 * @property target The creature gaining the restriction
 * @property creatureType The creature type to count (e.g., "Cleric")
 * @property manaCostPer The mana cost per creature of that type (e.g., "{1}")
 * @property duration How long the restriction lasts
 */
@SerialName("GrantAttackBlockTaxPerCreatureType")
@Serializable
data class GrantAttackBlockTaxPerCreatureTypeEffect(
    val target: EffectTarget = EffectTarget.ContextTarget(0),
    val creatureType: String,
    val manaCostPer: String,
    val duration: Duration = Duration.EndOfTurn
) : Effect {
    override val description: String =
        "Target creature gains \"This creature can't attack or block unless its controller pays $manaCostPer for each $creatureType on the battlefield.\""

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newType = replacer.replaceCreatureType(creatureType)
        return if (newType != creatureType) copy(creatureType = newType) else this
    }
}

/**
 * Redirect a creature's combat damage to its controller.
 * "The next time [creature] would deal combat damage this turn,
 *  it deals that damage to you instead."
 * Used for Goblin Psychopath and similar coin-flip combat redirection.
 *
 * Creates a floating effect that is consumed after the first combat damage event.
 *
 * @property target The creature whose combat damage is redirected (typically Self)
 */
/**
 * Grant a keyword to all attacking creatures that were blocked by the target creature.
 * "Creatures that were blocked by that creature this combat gain [keyword] until end of turn."
 * Used for Ride Down and similar combat tricks that destroy a blocker and grant abilities
 * to the attackers it was blocking.
 *
 * @property target The blocking creature whose blocked attackers receive the keyword
 * @property keyword The keyword to grant (e.g., "TRAMPLE")
 * @property duration How long the keyword lasts
 */
@SerialName("GrantKeywordToAttackersBlockedBy")
@Serializable
data class GrantKeywordToAttackersBlockedByEffect(
    val target: EffectTarget,
    val keyword: String,
    val duration: Duration = Duration.EndOfTurn
) : Effect {
    override val description: String =
        "Creatures that were blocked by ${target.description} gain ${keyword.lowercase().replace('_', ' ')} ${duration.description}"

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

@SerialName("RedirectCombatDamageToController")
@Serializable
data class RedirectCombatDamageToControllerEffect(
    val target: EffectTarget = EffectTarget.Self
) : Effect {
    override val description: String =
        "The next time ${target.description} would deal combat damage this turn, it deals that damage to you instead"

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

