package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.text.TextReplacer
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Counter Manipulation Effects
// =============================================================================

/**
 * Add counters effect.
 * "Put X +1/+1 counters on target creature"
 */
@SerialName("AddCounters")
@Serializable
data class AddCountersEffect(
    val counterType: String,
    val count: Int,
    val target: EffectTarget
) : Effect {
    override val description: String =
        "Put $count $counterType counter${if (count != 1) "s" else ""} on ${target.description}"

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

/**
 * Add counters with a dynamic amount.
 * "Put N +1/+1 counters on target creature, where N is [amount]"
 */
@SerialName("AddDynamicCounters")
@Serializable
data class AddDynamicCountersEffect(
    val counterType: String,
    val amount: DynamicAmount,
    val target: EffectTarget
) : Effect {
    override val description: String =
        "Put ${amount.description} $counterType counters on ${target.description}"

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

/**
 * Remove counters effect.
 * "Remove X -1/-1 counters from target creature"
 */
@SerialName("RemoveCounters")
@Serializable
data class RemoveCountersEffect(
    val counterType: String,
    val count: Int,
    val target: EffectTarget
) : Effect {
    override val description: String =
        "Remove $count $counterType counter${if (count != 1) "s" else ""} from ${target.description}"

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

/**
 * Put -1/-1 counters on a creature.
 * Used for blight effects and wither-style damage.
 *
 * @property count Number of -1/-1 counters to place
 * @property target The creature to receive the counters
 */
@SerialName("AddMinusCounters")
@Serializable
data class AddMinusCountersEffect(
    val count: Int,
    val target: EffectTarget
) : Effect {
    override val description: String =
        "Put $count -1/-1 counter${if (count != 1) "s" else ""} on ${target.description}"

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

/**
 * Add counters to all entities in a named collection.
 * Used for non-targeting "choose" effects that place counters on multiple permanents.
 * "Put an aim counter on each of them"
 */
@SerialName("AddCountersToCollection")
@Serializable
data class AddCountersToCollectionEffect(
    val collectionName: String,
    val counterType: String,
    val count: Int = 1
) : Effect {
    override val description: String =
        "Put $count $counterType counter${if (count != 1) "s" else ""} on each permanent in $collectionName"

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

/**
 * Distribute any number of counters from this creature onto other creatures.
 * "At the beginning of your upkeep, you may move any number of +1/+1 counters
 * from Forgotten Ancient onto other creatures."
 *
 * At resolution time, the executor:
 * 1. Checks how many counters of the given type are on the source creature
 * 2. Finds all other creatures on the battlefield
 * 3. If 0 counters or no other creatures, does nothing
 * 4. Presents a DistributeDecision with total = counter count, targets = other creatures
 * 5. On response, removes distributed counters from self and adds them per the distribution
 *
 * Does not target — the recipient creatures are chosen at resolution time.
 *
 * @property counterType The type of counter to move (e.g., "+1/+1")
 */
@SerialName("DistributeCountersFromSelf")
@Serializable
data class DistributeCountersFromSelfEffect(
    val counterType: String = Counters.PLUS_ONE_PLUS_ONE
) : Effect {
    override val description: String =
        "Move any number of $counterType counters from this creature onto other creatures"

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

/**
 * Distribute a fixed number of counters among the targets from context.
 * "Distribute N counters among one or more target creatures you control."
 *
 * Distribution is deterministic when totalCounters equals number of targets * minPerTarget.
 * With 1 target, all counters go on it. With multiple targets, counters are divided evenly
 * (remainder goes to the first target).
 *
 * @property totalCounters Total number of counters to distribute
 * @property counterType The type of counter (e.g., "+1/+1")
 * @property minPerTarget Minimum counters each target must receive (per MTG rules, typically 1)
 */
@SerialName("DistributeCountersAmongTargets")
@Serializable
data class DistributeCountersAmongTargetsEffect(
    val totalCounters: Int,
    val counterType: String = Counters.PLUS_ONE_PLUS_ONE,
    val minPerTarget: Int = 1
) : Effect {
    override val description: String =
        "Distribute $totalCounters $counterType counter${if (totalCounters != 1) "s" else ""} among targets"

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}
