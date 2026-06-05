package com.wingedsheep.engine.handlers.effects.composite

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.effects.Gate
import com.wingedsheep.sdk.scripting.effects.GatedEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.PayManaCostEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Pins the recognition boundary of [asMayDecide] — the matcher the trigger machinery uses to
 * recognize the lowered `MayEffect` (a [GatedEffect] over a bare [Gate.MayDecide]) after the
 * wrapper became a facade. The may-then-target reorder in `TriggerProcessor` and its
 * `resumeMayTrigger` unwrap key off it, so it must match exactly the no-`otherwise` MayDecide
 * shape and nothing else.
 */
class MayDecideGateTest : FunSpec({

    val inner = Effects.DrawCards(1)

    test("the MayEffect facade lowers to a Gate.MayDecide, carrying its skip flags") {
        val lowered = MayEffect(inner, sourceRequiredZone = Zone.GRAVEYARD, inlineOnTrigger = true)
        lowered.shouldBeInstanceOf<GatedEffect>()
        val gate = lowered.gate
        gate.shouldBeInstanceOf<Gate.MayDecide>()
        gate.sourceRequiredZone shouldBe Zone.GRAVEYARD
        gate.inlineOnTrigger.shouldBeTrue()
        lowered.then shouldBe inner
        lowered.otherwise.shouldBeNull()
    }

    test("asMayDecide matches the lowered MayEffect, exposing the inner effect and flags") {
        val match = MayEffect(inner, sourceRequiredZone = Zone.GRAVEYARD, inlineOnTrigger = true).asMayDecide()
        match.shouldNotBeNull()
        match.then shouldBe inner
        match.sourceRequiredZone shouldBe Zone.GRAVEYARD
        match.inlineOnTrigger.shouldBeTrue()
    }

    test("a MayDecide gate that carries an otherwise does NOT match (not the bare MayEffect shape)") {
        GatedEffect(gate = Gate.MayDecide(), then = inner, otherwise = Effects.DrawCards(2))
            .asMayDecide().shouldBeNull()
    }

    test("a MayPay (payment) gate does NOT match") {
        GatedEffect(gate = Gate.MayPay(PayManaCostEffect(ManaCost.parse("{1}"))), then = inner)
            .asMayDecide().shouldBeNull()
    }

    test("a non-gated effect does NOT match") {
        inner.asMayDecide().shouldBeNull()
    }
})
