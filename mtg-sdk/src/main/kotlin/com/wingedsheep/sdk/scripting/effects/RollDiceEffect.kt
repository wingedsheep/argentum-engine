package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Roll [count] independent [sides]-sided dice for [player]. Each outcome is equally likely.
 * [resultCounts] maps pipeline number names to the results they count in this batch only.
 * Luck Bobblehead counts even results and sixes; die observers read each individual roll.
 *
 * Closest types: [FlipCoinsEffect] counts binary wins rather than numerical die results;
 * [StoreNumberEffect] reads an existing amount but cannot produce a random event.
 * Neither can take a die-size axis without changing the event and outcome contract.
 */
@Serializable
@SerialName("RollDice")
data class RollDiceEffect(
    val count: DynamicAmount = DynamicAmount.Fixed(1),
    val sides: Int = 6,
    val player: EffectTarget = EffectTarget.Controller,
    val resultCounts: Map<String, Set<Int>> = emptyMap(),
) : Effect {
    init { require(sides > 0) { "A die must have at least one side" } }
    override fun applyTextReplacement(replacer: com.wingedsheep.sdk.scripting.text.TextReplacer): Effect {
        val newCount = count.applyTextReplacement(replacer)
        return if (newCount !== count) copy(count = newCount) else this
    }
    override val description: String = "Roll ${count.description} $sides-sided dice"
}
