package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.serialization.CardSerialization
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class RollDiceSerializationTest : FunSpec({
    test("dice with dynamic count and overlapping result tallies round-trip") {
        val effect=Effects.RollDice(DynamicAmount.VariableReference("count"),20,resultCounts=mapOf("high" to setOf(18,19,20),"twenty" to setOf(20)))
        val json=CardSerialization.json
        json.decodeFromString(Effect.serializer(),json.encodeToString(Effect.serializer(),effect)) shouldBe effect
    }
    test("result interval and roller trigger round-trip") {
        val pattern=Triggers.anOpponent.rollsDie(4,6).event
        val json=CardSerialization.json
        json.decodeFromString(EventPattern.serializer(),json.encodeToString(EventPattern.serializer(),pattern)) shouldBe pattern
    }
    test("invalid dice and unsupported trigger selectors fail rather than silently changing meaning") {
        shouldThrow<IllegalArgumentException> { Effects.RollDice(sides=0) }
        shouldThrow<IllegalArgumentException> { Triggers.chosenOpponent.rollsDie() }
        shouldThrow<IllegalArgumentException> { Triggers.you.rollsDie(6,4) }
    }
    test("two rolls in a typed pipeline receive distinct tally slots") {
        var first = ""; var second = ""
        Effects.Pipeline {
            val a = rollDice(resultSets=listOf(setOf(6))).single()
            val b = rollDice(resultSets=listOf(setOf(6))).single()
            first=a.key; second=b.key
        }
        (first != second) shouldBe true
    }
})
