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
// Damage Prevention
// =============================================================================

/**
 * The scope of damage a prevention shield applies to.
 */
@Serializable
enum class PreventionScope {
    AllDamage,
    CombatOnly
}

/**
 * The direction of damage prevention relative to the target entity.
 */
@Serializable
enum class PreventionDirection {
    /** Shield protects the target from receiving damage. */
    ToTarget,
    /** Shield prevents the target from dealing damage. */
    FromTarget,
    /** Shield prevents damage both to and from the target. */
    Both
}

/**
 * Filter on the source of the damage being prevented.
 */
@Serializable
sealed interface PreventionSourceFilter {
    /** Any damage source. */
    @SerialName("AnySource") @Serializable data object AnySource : PreventionSourceFilter
    /** Only damage from attacking creatures. */
    @SerialName("AttackingCreatures") @Serializable data object AttackingCreatures : PreventionSourceFilter
    /** Player chooses a damage source on resolution. */
    @SerialName("ChosenSource") @Serializable data object ChosenSource : PreventionSourceFilter
    /** Uses the chosen creature type from the source permanent's component. */
    @SerialName("ChosenCreatureType") @Serializable data object ChosenCreatureType : PreventionSourceFilter
    /** Only damage from creatures matching a group filter. */
    @SerialName("FromGroup") @Serializable data class FromGroup(val filter: GroupFilter) : PreventionSourceFilter
}

/**
 * Unified damage prevention effect.
 *
 * Replaces all previous prevention effect types with a single parametrized type that can express
 * any combination of: amount-based vs prevent-all, combat-only vs all-damage, directional
 * prevention, source filtering, and damage reflection.
 *
 * Examples:
 * - "Prevent the next 3 damage to target creature" → `PreventDamageEffect(target, amount=3)`
 * - "Prevent all combat damage this turn" → `PreventDamageEffect(scope=CombatOnly)`
 * - "Prevent all damage target would deal" → `PreventDamageEffect(target, direction=FromTarget)`
 * - "Prevent combat damage to and by target" → `PreventDamageEffect(target, scope=CombatOnly, direction=Both)`
 * - "Choose a source, prevent + reflect" → `PreventDamageEffect(sourceFilter=ChosenSource, reflect=true)`
 *
 * @property target The entity the shield is attached to (protected or silenced, depending on direction)
 * @property amount Amount of damage to prevent; null means prevent all
 * @property scope Whether to prevent all damage or only combat damage
 * @property direction Whether to prevent damage TO the target, FROM the target, or BOTH
 * @property sourceFilter Filter on which damage sources are affected
 * @property reflect If true, prevented damage is dealt to the source's controller (Deflecting Palm)
 * @property duration When the shield expires
 */
@SerialName("PreventDamageShield")
@Serializable
data class PreventDamageEffect(
    val target: EffectTarget = EffectTarget.Controller,
    val amount: DynamicAmount? = null,
    val scope: PreventionScope = PreventionScope.AllDamage,
    val direction: PreventionDirection = PreventionDirection.ToTarget,
    val sourceFilter: PreventionSourceFilter = PreventionSourceFilter.AnySource,
    val reflect: Boolean = false,
    val duration: Duration = Duration.EndOfTurn
) : Effect {
    override val description: String = buildString {
        append("Prevent ")
        if (amount != null) {
            append("the next ${amount.description} ")
        } else {
            append("all ")
        }
        when (scope) {
            PreventionScope.CombatOnly -> append("combat damage ")
            PreventionScope.AllDamage -> append("damage ")
        }
        when (direction) {
            PreventionDirection.ToTarget -> append("that would be dealt to ${target.description}")
            PreventionDirection.FromTarget -> append("${target.description} would deal")
            PreventionDirection.Both -> append("that would be dealt to and dealt by ${target.description}")
        }
        when (sourceFilter) {
            PreventionSourceFilter.AnySource -> {}
            PreventionSourceFilter.AttackingCreatures -> append(" by attacking creatures")
            PreventionSourceFilter.ChosenSource -> append(" by a source of your choice")
            PreventionSourceFilter.ChosenCreatureType -> append(" by a creature of the chosen type")
            is PreventionSourceFilter.FromGroup ->
                append(" by ${sourceFilter.filter.description.replaceFirstChar { it.lowercase() }}")
        }
        append(" this turn")
        if (reflect) append(". If damage is prevented this way, deal that much damage to that source's controller")
    }

    override fun runtimeDescription(resolver: (DynamicAmount) -> Int): String {
        if (amount == null) return description
        val resolved = resolver(amount)
        return description.replace(amount.description, resolved.toString())
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newAmount = amount?.applyTextReplacement(replacer)
        val newFilter = when (sourceFilter) {
            is PreventionSourceFilter.FromGroup -> {
                val newGroupFilter = sourceFilter.filter.applyTextReplacement(replacer)
                if (newGroupFilter !== sourceFilter.filter) PreventionSourceFilter.FromGroup(newGroupFilter) else sourceFilter
            }
            else -> sourceFilter
        }
        return if (newAmount !== amount || newFilter !== sourceFilter) copy(amount = newAmount, sourceFilter = newFilter) else this
    }
}

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
 * Target creature must be blocked this turn.
 *
 * @property target The creature that must be blocked
 * @property allCreatures If true, ALL creatures able to block it must do so (Lure/Alluring Scent).
 *   If false, at least one creature must block it if able (Gaea's Protector).
 */
@SerialName("MustBeBlocked")
@Serializable
data class MustBeBlockedEffect(
    val target: EffectTarget,
    val allCreatures: Boolean = true
) : Effect {
    override val description: String = if (allCreatures) {
        "All creatures able to block ${target.description} this turn do so"
    } else {
        "${target.description} must be blocked this turn if able"
    }

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
 * All creatures matching a filter can't attack this turn.
 * Used for Meekstone, Silent Arbiter, Propaganda-style effects.
 *
 * Creates a floating effect that dynamically applies to all creatures matching the filter,
 * including those entering the battlefield after the effect resolves (Rule 611.2c).
 *
 * @property filter Which creatures can't attack (e.g., GroupFilter.AllCreaturesOpponentsControl)
 * @property duration How long the restriction lasts
 */
@SerialName("CantAttackGroup")
@Serializable
data class CantAttackGroupEffect(
    val filter: GroupFilter,
    val duration: Duration = Duration.EndOfTurn
) : Effect {
    override val description: String = "${filter.description} can't attack this turn"

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
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
 * Target creature can't block this turn.
 *
 * For multi-target spells, wrap in ForEachTargetEffect with EffectTarget.ContextTarget(0).
 *
 * @property target The creature that can't block
 * @property duration How long the restriction lasts
 */
@SerialName("CantBlockTargetCreatures")
@Serializable
data class CantBlockEffect(
    val target: EffectTarget,
    val duration: Duration = Duration.EndOfTurn
) : Effect {
    override val description: String = "${target.description} can't block this turn"

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

/**
 * Target creature can't attack this turn.
 *
 * @property target The creature that can't attack
 * @property duration How long the restriction lasts
 */
@SerialName("CantAttack")
@Serializable
data class CantAttackEffect(
    val target: EffectTarget,
    val duration: Duration = Duration.EndOfTurn
) : Effect {
    override val description: String = "${target.description} can't attack this turn"

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
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


