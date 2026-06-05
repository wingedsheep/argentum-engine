package com.wingedsheep.engine.handlers.effects.composite

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.Gate
import com.wingedsheep.sdk.scripting.effects.GatedEffect
import com.wingedsheep.sdk.scripting.effects.PayManaCostEffect
import com.wingedsheep.sdk.core.ManaCost
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Pins the recognition boundary of [asConditional] — the matcher engine paths use to recognize the
 * lowered `ConditionalEffect` (a [GatedEffect] over a [Gate.WhenCondition]) after the wrapper became
 * a facade. Stack-time branch resolution, repeat-activation analysis, and limited card rating all
 * key off it, so it must match exactly the WhenCondition shape and nothing else.
 */
class ConditionalGateTest : FunSpec({

    val thenEffect = Effects.DrawCards(1)
    val elseEffect = Effects.DrawCards(2)
    val condition = Conditions.IsYourTurn

    test("the ConditionalEffect facade lowers to a Gate.WhenCondition") {
        val lowered = ConditionalEffect(condition, thenEffect, elseEffect)
        lowered.shouldBeInstanceOf<GatedEffect>()
        val gate = lowered.gate
        gate.shouldBeInstanceOf<Gate.WhenCondition>()
        gate.condition shouldBe condition
        lowered.then shouldBe thenEffect
        lowered.otherwise shouldBe elseEffect
    }

    test("asConditional matches the lowered ConditionalEffect, exposing both branches") {
        val match = ConditionalEffect(condition, thenEffect, elseEffect).asConditional()
        match.shouldNotBeNull()
        match.condition shouldBe condition
        match.then shouldBe thenEffect
        match.otherwise shouldBe elseEffect
    }

    test("asConditional matches a one-branch conditional, with a null otherwise") {
        val match = ConditionalEffect(condition, thenEffect).asConditional()
        match.shouldNotBeNull()
        match.then shouldBe thenEffect
        match.otherwise.shouldBeNull()
    }

    test("a MayPay (payment) gate does NOT match") {
        GatedEffect(gate = Gate.MayPay(PayManaCostEffect(ManaCost.parse("{1}"))), then = thenEffect)
            .asConditional().shouldBeNull()
    }

    test("a MayDecide (yes/no) gate does NOT match") {
        GatedEffect(gate = Gate.MayDecide(), then = thenEffect).asConditional().shouldBeNull()
    }

    test("a non-gated effect does NOT match") {
        thenEffect.asConditional().shouldBeNull()
    }
})
