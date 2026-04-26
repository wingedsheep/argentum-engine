package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
import com.wingedsheep.sdk.scripting.text.TextReplacer
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Stack Effects — Counter
// =============================================================================

/**
 * What kind of stack object to counter.
 */
@Serializable
sealed interface CounterTarget {
    /** Counter a spell (goes to graveyard/exile). */
    @SerialName("CounterTarget.Spell")
    @Serializable
    data object Spell : CounterTarget

    /** Counter an activated or triggered ability (removed from stack, no zone change). */
    @SerialName("CounterTarget.Ability")
    @Serializable
    data object Ability : CounterTarget
}

/**
 * How the target of the counter is identified.
 */
@Serializable
sealed interface CounterTargetSource {
    /** Uses a chosen target from context.targets (normal targeting). */
    @SerialName("CounterTargetSource.Chosen")
    @Serializable
    data object Chosen : CounterTargetSource

    /** Uses context.triggeringEntityId (for triggered abilities like Decree of Silence). */
    @SerialName("CounterTargetSource.TriggeringEntity")
    @Serializable
    data object TriggeringEntity : CounterTargetSource
}

/**
 * Where the countered spell goes.
 */
@Serializable
sealed interface CounterDestination {
    /** Spell goes to owner's graveyard (default). */
    @SerialName("CounterDestination.Graveyard")
    @Serializable
    data object Graveyard : CounterDestination

    /**
     * Spell is exiled instead of going to graveyard.
     * @property grantFreeCast If true, the controller may cast the exiled card
     *   without paying its mana cost for as long as it remains exiled.
     */
    @SerialName("CounterDestination.Exile")
    @Serializable
    data class Exile(val grantFreeCast: Boolean = false) : CounterDestination
}

/**
 * Optional condition that allows the spell's controller to prevent countering.
 */
@Serializable
sealed interface CounterCondition {
    /** No condition — always counter. */
    @SerialName("CounterCondition.Always")
    @Serializable
    data object Always : CounterCondition

    /** Counter unless controller pays a fixed mana cost. */
    @SerialName("CounterCondition.UnlessPaysMana")
    @Serializable
    data class UnlessPaysMana(val cost: ManaCost) : CounterCondition

    /** Counter unless controller pays a dynamic generic mana cost. */
    @SerialName("CounterCondition.UnlessPaysDynamic")
    @Serializable
    data class UnlessPaysDynamic(val amount: DynamicAmount) : CounterCondition
}

/**
 * Unified counter effect for all counter-spell and counter-ability variations.
 *
 * Replaces the former CounterSpellEffect, CounterSpellWithFilterEffect,
 * CounterUnlessPaysEffect, CounterUnlessDynamicPaysEffect, CounterTriggeringSpellEffect,
 * CounterSpellToExileEffect, and CounterAbilityEffect with a single parametrized type.
 *
 * Examples:
 * - "Counter target spell" → CounterEffect()
 * - "Counter target creature spell" → CounterEffect(filter = creatureFilter)
 * - "Counter unless pays {2}" → CounterEffect(condition = UnlessPaysMana(ManaCost.parse("{2}")))
 * - "Counter that spell" → CounterEffect(targetSource = TriggeringEntity)
 * - "Counter and exile" → CounterEffect(counterDestination = Exile())
 * - "Counter target ability" → CounterEffect(target = Ability)
 */
@SerialName("Counter")
@Serializable
data class CounterEffect(
    val target: CounterTarget = CounterTarget.Spell,
    val targetSource: CounterTargetSource = CounterTargetSource.Chosen,
    val counterDestination: CounterDestination = CounterDestination.Graveyard,
    val condition: CounterCondition = CounterCondition.Always,
    val filter: TargetFilter? = null
) : Effect {
    override val description: String = buildString {
        when (target) {
            CounterTarget.Ability -> append("Counter target activated or triggered ability")
            CounterTarget.Spell -> {
                when (condition) {
                    is CounterCondition.Always -> {
                        if (targetSource == CounterTargetSource.TriggeringEntity) {
                            append("Counter that spell")
                        } else if (filter != null) {
                            append("Counter target ${filter.baseFilter.description} spell")
                        } else {
                            append("Counter target spell")
                        }
                    }
                    is CounterCondition.UnlessPaysMana -> {
                        append("Counter target spell unless its controller pays ${condition.cost}")
                    }
                    is CounterCondition.UnlessPaysDynamic -> {
                        append("Counter target spell unless its controller pays ${condition.amount.description}")
                    }
                }
                when (val dest = counterDestination) {
                    CounterDestination.Graveyard -> {}
                    is CounterDestination.Exile -> {
                        if (condition is CounterCondition.UnlessPaysMana || condition is CounterCondition.UnlessPaysDynamic) {
                            append(". If countered, exile it")
                        } else {
                            append(". Exile it instead of putting it into its owner's graveyard")
                        }
                        if (dest.grantFreeCast) {
                            append(". You may cast that card without paying its mana cost for as long as it remains exiled")
                        }
                    }
                }
            }
        }
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newFilter = filter?.applyTextReplacement(replacer)
        val newCondition = when (condition) {
            is CounterCondition.UnlessPaysDynamic -> {
                val newAmount = condition.amount.applyTextReplacement(replacer)
                if (newAmount !== condition.amount) CounterCondition.UnlessPaysDynamic(newAmount) else condition
            }
            else -> condition
        }
        return if (newFilter !== filter || newCondition !== condition)
            copy(filter = newFilter, condition = newCondition) else this
    }
}

// =============================================================================
// Stack Effects — Counter All
// =============================================================================

/**
 * Counter every spell and/or ability on the stack matching the controller filter.
 * Non-targeted — resolves against whatever is on the stack at resolution time.
 *
 * Used by "mass counter" effects like Glen Elendra's Answer:
 * "Counter all spells your opponents control and all abilities your opponents control."
 *
 * If [storeCountAs] is set, the countered entity IDs are stored in the pipeline as
 * a named collection. Subsequent pipeline effects can read the count via
 * [com.wingedsheep.sdk.scripting.values.DynamicAmount.VariableReference] using the
 * key `"$storeCountAs" + "_count"` (e.g., "countered_count").
 *
 * @property spells Whether to counter spells on the stack
 * @property abilities Whether to counter activated/triggered abilities on the stack
 * @property opponentsOnly If true (default), only counter objects controlled by the
 *   effect source's opponents; if false, counter every matching object on the stack
 * @property storeCountAs Optional pipeline variable name to store the countered entity
 *   IDs under (for downstream `VariableReference("<name>_count")` usage)
 */
@SerialName("CounterAllOnStack")
@Serializable
data class CounterAllOnStackEffect(
    val spells: Boolean = true,
    val abilities: Boolean = true,
    val opponentsOnly: Boolean = true,
    val storeCountAs: String? = null
) : Effect {
    override val description: String = buildString {
        append("Counter all ")
        val parts = buildList {
            if (spells) add("spells")
            if (abilities) add("abilities")
        }
        append(parts.joinToString(" and "))
        if (opponentsOnly) append(" your opponents control")
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

// =============================================================================
// Stack Effects — Ward
// =============================================================================

/**
 * The cost a controller must pay to prevent a ward-triggered counter.
 * Mirrors the structure of [com.wingedsheep.sdk.scripting.KeywordAbility] ward
 * variants so static-granted ward and intrinsic ward share a single cost shape.
 */
@Serializable
sealed interface WardCost {
    /** Display string for prompts and oracle text generation. */
    val description: String

    /** Ward with a mana cost — e.g. Ward {1}. */
    @SerialName("WardCost.Mana")
    @Serializable
    data class Mana(val manaCost: String) : WardCost {
        override val description: String = manaCost
    }

    /** Ward with a life cost — e.g. Ward—Pay 2 life. */
    @SerialName("WardCost.Life")
    @Serializable
    data class Life(val amount: Int) : WardCost {
        override val description: String = "pay $amount life"
    }
}

/**
 * Counter the spell or ability that targeted this permanent unless its controller pays
 * the ward cost. Used by ward triggered abilities. The targeting source is identified
 * via context.targetingSourceEntityId (set by BecomesTargetEvent).
 *
 * Ward {1}        → WardCounterEffect(WardCost.Mana("{1}"))
 * Ward—Pay 2 life → WardCounterEffect(WardCost.Life(2))
 */
@SerialName("WardCounter")
@Serializable
data class WardCounterEffect(
    val cost: WardCost
) : Effect {
    override val description: String = when (cost) {
        is WardCost.Mana -> "Counter it unless its controller pays ${cost.manaCost}"
        is WardCost.Life -> "Counter it unless its controller pays ${cost.amount} life"
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

// =============================================================================
// Stack Effects — Other
// =============================================================================

/**
 * Change the target of a spell that has exactly one target, and that target is a creature,
 * to another creature.
 * "If target spell has only one target and that target is a creature, change that spell's target to another creature."
 *
 * When [targetMustBeSource] is true, the spell's target must be the source of this effect
 * (e.g., Quicksilver Dragon: "If target spell has only one target and that target is this creature").
 */
@SerialName("ChangeSpellTarget")
@Serializable
data class ChangeSpellTargetEffect(
    val targetMustBeSource: Boolean = false
) : Effect {
    override val description: String = if (targetMustBeSource) {
        "Change target spell's target if it targets this creature"
    } else {
        "Change target spell's target to another creature"
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

/**
 * Change the target of target spell or ability with a single target.
 * "Change the target of target spell or ability with a single target."
 *
 * Unlike ChangeSpellTargetEffect which only redirects creature targets to other creatures,
 * this effect works on any target type (creature, player, etc.) and can target
 * both spells and abilities on the stack.
 *
 * The spell or ability must have exactly one target. If it has zero or multiple targets,
 * the effect does nothing.
 */
@SerialName("ChangeTarget")
@Serializable
data object ChangeTargetEffect : Effect {
    override val description: String = "Change the target of target spell or ability with a single target"

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

/**
 * Reselect the target of the triggering spell or ability at random.
 * "If it has a single target, reselect its target at random."
 *
 * Uses context.triggeringEntityId to find the spell/ability on the stack.
 * If it has exactly one target, finds all legal targets and randomly picks one.
 * If it has zero or multiple targets, does nothing.
 */
@SerialName("ReselectTargetRandomly")
@Serializable
data object ReselectTargetRandomlyEffect : Effect {
    override val description: String = "Reselect its target at random"

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

/**
 * Create copies of a spell on the stack (Storm mechanic).
 * "Copy it for each spell cast before it this turn. You may choose new targets for the copies."
 *
 * When resolved, creates [copyCount] copies of the spell on the stack. Each copy has the same
 * effect as the original. If the spell has targets, the controller may choose new targets for
 * each copy.
 *
 * @property copyCount Number of copies to create
 * @property spellEffect The effect of the original spell to copy
 * @property spellTargetRequirements Target requirements from the original spell (empty if untargeted)
 * @property spellName Name of the original spell for display
 */
@SerialName("StormCopy")
@Serializable
data class StormCopyEffect(
    val copyCount: Int,
    val spellEffect: Effect,
    val spellTargetRequirements: List<TargetRequirement> = emptyList(),
    val spellName: String
) : Effect {
    override val description: String = "Copy $spellName $copyCount time(s)"

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        var changed = false
        val newReqs = spellTargetRequirements.map {
            val n = it.applyTextReplacement(replacer); if (n !== it) changed = true; n
        }
        val newSpellEffect = spellEffect.applyTextReplacement(replacer)
        if (newSpellEffect !== spellEffect) changed = true
        return if (changed) copy(spellEffect = newSpellEffect, spellTargetRequirements = newReqs) else this
    }
}

/**
 * Copy target instant or sorcery spell on the stack.
 * "Copy target instant or sorcery spell. You may choose new targets for the copy."
 *
 * When resolved, reads the targeted spell's effect and target requirements from the stack,
 * then creates a copy. If the original spell has targets, the controller may choose new
 * targets for the copy.
 *
 * @property target The effect target referencing the spell to copy (typically ContextTarget(0))
 */
@SerialName("CopyTargetSpell")
@Serializable
data class CopyTargetSpellEffect(
    val target: EffectTarget = EffectTarget.ContextTarget(0)
) : Effect {
    override val description: String = "Copy target instant or sorcery spell"

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

/**
 * Copy target triggered ability on the stack.
 * "Copy target triggered ability. You may choose new targets for the copy."
 *
 * When resolved, reads the targeted triggered ability's effect and targets from the stack,
 * clones its TriggeredAbilityOnStackComponent onto a new entity, and pushes it onto the stack.
 * If the original ability has targets, the controller may choose new targets for the copy.
 *
 * @property target The effect target referencing the triggered ability to copy (typically ContextTarget(0))
 */
@SerialName("CopyTargetTriggeredAbility")
@Serializable
data class CopyTargetTriggeredAbilityEffect(
    val target: EffectTarget = EffectTarget.ContextTarget(0)
) : Effect {
    override val description: String = "Copy target triggered ability"

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

/**
 * When you next cast an instant or sorcery spell this turn, copy that spell.
 * You may choose new targets for the copies.
 *
 * Creates a pending spell copy entry on the game state. When the controller next casts
 * an instant or sorcery spell this turn, the engine creates [copies] copies of it.
 * Used by Howl of the Horde and similar effects.
 *
 * @property copies Number of copies to create when the next spell is cast
 */
@SerialName("CopyNextSpellCast")
@Serializable
data class CopyNextSpellCastEffect(
    val copies: Int = 1
) : Effect {
    override val description: String = if (copies == 1) {
        "When you next cast an instant or sorcery spell this turn, copy that spell"
    } else {
        "When you next cast an instant or sorcery spell this turn, copy that spell $copies times"
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

/**
 * Until end of turn, whenever you cast an instant or sorcery spell, copy it.
 * You may choose new targets for the copies.
 *
 * Creates a persistent pending spell copy entry on the game state. Unlike
 * CopyNextSpellCastEffect which is consumed after one use, this entry persists
 * for the rest of the turn, copying every instant or sorcery spell cast.
 * Used by The Mirari Conjecture Chapter III and similar effects.
 *
 * @property copies Number of copies to create for each spell cast
 */
@SerialName("CopyEachSpellCast")
@Serializable
data class CopyEachSpellCastEffect(
    val copies: Int = 1
) : Effect {
    override val description: String = if (copies == 1) {
        "Until end of turn, whenever you cast an instant or sorcery spell, copy it"
    } else {
        "Until end of turn, whenever you cast an instant or sorcery spell, copy it $copies times"
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}
