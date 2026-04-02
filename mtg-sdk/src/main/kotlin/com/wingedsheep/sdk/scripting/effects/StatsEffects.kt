package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.text.TextReplacer
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Power/Toughness Modification Effects
// =============================================================================

/**
 * Modify power/toughness effect.
 * "Target creature gets +X/+Y until end of turn"
 *
 * Supports both fixed and dynamic amounts via [DynamicAmount].
 */
@SerialName("ModifyStats")
@Serializable
data class ModifyStatsEffect(
    val powerModifier: DynamicAmount,
    val toughnessModifier: DynamicAmount,
    val target: EffectTarget,
    val duration: Duration = Duration.EndOfTurn
) : Effect {
    constructor(powerModifier: Int, toughnessModifier: Int, target: EffectTarget, duration: Duration = Duration.EndOfTurn) :
        this(DynamicAmount.Fixed(powerModifier), DynamicAmount.Fixed(toughnessModifier), target, duration)

    override val description: String = buildString {
        append("${target.description} gets ")
        val pDesc = powerModifier.let {
            if (it is DynamicAmount.Fixed) {
                if (it.amount >= 0) "+${it.amount}" else "${it.amount}"
            } else {
                it.description
            }
        }
        val tDesc = toughnessModifier.let {
            if (it is DynamicAmount.Fixed) {
                if (it.amount >= 0) "+${it.amount}" else "${it.amount}"
            } else {
                it.description
            }
        }
        append("$pDesc/$tDesc")
        if (duration.description.isNotEmpty()) append(" ${duration.description}")
    }

    override fun runtimeDescription(resolver: (DynamicAmount) -> Int): String = buildString {
        append("${target.description} gets ")
        fun fmt(v: Int) = if (v >= 0) "+$v" else "$v"
        append("${fmt(resolver(powerModifier))}/${fmt(resolver(toughnessModifier))}")
        if (duration.description.isNotEmpty()) append(" ${duration.description}")
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newPower = powerModifier.applyTextReplacement(replacer)
        val newToughness = toughnessModifier.applyTextReplacement(replacer)
        return if (newPower !== powerModifier || newToughness !== toughnessModifier)
            copy(powerModifier = newPower, toughnessModifier = newToughness) else this
    }
}

/**
 * Set a creature's base power to a specific value.
 * "{1}{U}: Change this creature's base power to target creature's power."
 *
 * Creates a floating effect at Layer.POWER_TOUGHNESS, Sublayer.SET_VALUES that only
 * overrides the power, leaving toughness unchanged. The effect lasts for the specified duration.
 *
 * @property target The creature whose base power is being set
 * @property power The value to set the base power to (evaluated at resolution time)
 * @property duration How long the effect lasts (typically Permanent for indefinite effects)
 */
@SerialName("SetBasePower")
@Serializable
data class SetBasePowerEffect(
    val target: EffectTarget,
    val power: DynamicAmount,
    val duration: Duration = Duration.Permanent
) : Effect {
    override val description: String = buildString {
        append("Change ${target.description}'s base power to ${power.description}")
        if (duration.description.isNotEmpty()) append(" ${duration.description}")
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newPower = power.applyTextReplacement(replacer)
        return if (newPower !== power) copy(power = newPower) else this
    }
}

/**
 * Sets a creature's base power and toughness to fixed values via a floating continuous effect.
 * Creates a floating effect at Layer.POWER_TOUGHNESS, Sublayer.SET_VALUES that overrides both
 * power and toughness.
 *
 * Used for cards like Azure Beastbinder: "it has base power and toughness 2/2 until your next turn".
 *
 * @property target The creature whose base P/T is being set
 * @property power The base power value
 * @property toughness The base toughness value
 * @property duration How long the effect lasts
 */
@SerialName("SetBasePowerToughness")
@Serializable
data class SetBasePowerToughnessEffect(
    val target: EffectTarget,
    val power: Int,
    val toughness: Int,
    val duration: Duration = Duration.Permanent
) : Effect {
    override val description: String = buildString {
        append("${target.description} has base power and toughness $power/$toughness")
        if (duration.description.isNotEmpty()) append(" ${duration.description}")
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

/**
 * Effect that transforms multiple creatures at once.
 * Used for effects like Curious Colossus: "each creature target opponent controls
 * loses all abilities, becomes a Coward, and has base P/T 1/1."
 *
 * @property target Which creatures are affected
 * @property loseAllAbilities If true, creatures lose all abilities
 * @property addCreatureType Type to add (if any)
 * @property setBasePower New base power (if set)
 * @property setBaseToughness New base toughness (if set)
 */
@SerialName("TransformAllCreatures")
@Serializable
data class TransformAllCreaturesEffect(
    val target: EffectTarget,
    val loseAllAbilities: Boolean = false,
    val addCreatureType: String? = null,
    val setBasePower: Int? = null,
    val setBaseToughness: Int? = null
) : Effect {
    override val description: String = buildString {
        append("Each creature ${target.description}")
        val effects = mutableListOf<String>()
        if (loseAllAbilities) effects.add("loses all abilities")
        if (addCreatureType != null) effects.add("becomes a $addCreatureType in addition to its other types")
        if (setBasePower != null && setBaseToughness != null) {
            effects.add("has base power and toughness $setBasePower/$setBaseToughness")
        }
        append(" ")
        append(effects.joinToString(", "))
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newType = addCreatureType?.let { replacer.replaceCreatureType(it) }
        return if (newType != addCreatureType) copy(addCreatureType = newType) else this
    }
}
