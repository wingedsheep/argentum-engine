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
 * [modifier] is the instruction's own adjustment ("roll a d20 and subtract the number of cards in
 * your hand" — The Deck of Many Things). It is evaluated once at resolution and added to each
 * kept die's natural result (the top face, CR 706.2); the sum is the die's *result*, which may be
 * zero or negative. [resultCounts], die observers and the batch summary all read results, never
 * natural faces. [storeTotalAs], when set, writes the sum of the kept results of this instruction
 * — across every player [player] names, when it names several — under that pipeline key — a results table (CR 706.3a) compares it with `Conditions.CompareAmounts`,
 * which reaches rows such as "0 or less" that no finite result set can.
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
    val modifier: DynamicAmount = DynamicAmount.Fixed(0),
    val storeTotalAs: String? = null,
) : Effect {
    init { require(sides > 0) { "A die must have at least one side" } }
    override fun applyTextReplacement(replacer: com.wingedsheep.sdk.scripting.text.TextReplacer): Effect {
        val newCount = count.applyTextReplacement(replacer)
        val newModifier = modifier.applyTextReplacement(replacer)
        return if (newCount !== count || newModifier !== modifier) copy(count = newCount, modifier = newModifier) else this
    }
    override val description: String = "Roll ${count.description} $sides-sided dice" +
        if (modifier == DynamicAmount.Fixed(0)) "" else " and add ${modifier.description} to each result"
}
