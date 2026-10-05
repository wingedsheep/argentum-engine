package com.wingedsheep.engine.handlers.effects.composite

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.effects.Gate
import com.wingedsheep.sdk.scripting.effects.GatedEffect
import com.wingedsheep.sdk.scripting.effects.PayManaCostEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Pins the bare may shape used to resume an already suspended consent question in a saved game.
 * New targeted instances retain their gate through target announcement until resolution.
 */
class MayDecideGateTest : FunSpec({

    val inner = Effects.DrawCards(1)

    test("the Effects.May facade lowers to a Gate.MayDecide, carrying its skip flags") {
        val lowered = Effects.May(inner, sourceRequiredZone = Zone.GRAVEYARD, inlineOnTrigger = true)
        lowered.shouldBeInstanceOf<GatedEffect>()
        val gate = lowered.gate
        gate.shouldBeInstanceOf<Gate.MayDecide>()
        gate.sourceRequiredZone shouldBe Zone.GRAVEYARD
        gate.inlineOnTrigger.shouldBeTrue()
        lowered.then shouldBe inner
        lowered.otherwise.shouldBeNull()
    }

    test("asMayDecide matches the lowered Effects.May, exposing the inner effect and flags") {
        val match = Effects.May(inner, sourceRequiredZone = Zone.GRAVEYARD, inlineOnTrigger = true).asMayDecide()
        match.shouldNotBeNull()
        match.then shouldBe inner
        match.sourceRequiredZone shouldBe Zone.GRAVEYARD
        match.inlineOnTrigger.shouldBeTrue()
    }

    test("a MayDecide gate that carries an otherwise does NOT match (not the bare Effects.May shape)") {
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
