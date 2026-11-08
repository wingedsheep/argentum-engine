package com.wingedsheep.engine.handlers.effects.composite

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.Gate
import com.wingedsheep.sdk.scripting.effects.GatedEffect
import com.wingedsheep.sdk.scripting.effects.PayLifeEffect
import com.wingedsheep.sdk.scripting.effects.PayManaCostEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Pins the recognition boundary of [asOptionalManaPayment] — the one piece of new logic introduced
 * when `Effects.MayPay` migrated from a bespoke wrapper+executor onto the [GatedEffect] frame.
 * The engine keys the optional-mana-payment UX (manual mana-source selection at resolution) off
 * this matcher, so its exactness is load-bearing.
 */
class OptionalManaPaymentTest : FunSpec({

    val inner = Effects.DrawCards(1)
    val cost = ManaCost.parse("{1}")

    test("the Effects.MayPay facade lowers to a flat mana Gate.MayPay") {
        val lowered = Effects.MayPay(cost, inner)
        lowered.shouldBeInstanceOf<GatedEffect>()
        val gate = lowered.gate
        gate.shouldBeInstanceOf<Gate.MayPay>()
        gate.cost shouldBe PayManaCostEffect(cost)
        lowered.otherwise.shouldBeNull()
        lowered.then shouldBe inner
    }

    test("asOptionalManaPayment matches the lowered Effects.MayPay shape") {
        val match = Effects.MayPay(cost, inner).asOptionalManaPayment()
        match.shouldNotBeNull()
        match.cost shouldBe cost
        match.then shouldBe inner
    }

    test("asOptionalManaPayment also matches an Effects.MayPay over a bare mana cost") {
        // Descendant of Storms authors "you may pay {1}{W}. If you do, ..." via Effects.MayPay;
        // post-migration it is structurally identical to a lowered Effects.MayPay and so shares
        // the optional-mana-payment UX (source selection) — the runtime cannot, and need not,
        // distinguish the two authoring facades.
        val match = Effects.MayPay(cost = PayManaCostEffect(cost), then = inner).asOptionalManaPayment()
        match.shouldNotBeNull()
        match.cost shouldBe cost
        match.then shouldBe inner
    }

    test("a MayPay with an otherwise branch does NOT match (keeps the generic gated path)") {
        Effects.MayPay(cost = PayManaCostEffect(cost), then = inner, otherwise = Effects.DrawCards(2))
            .asOptionalManaPayment().shouldBeNull()
    }

    test("a composite (mana + life) cost does NOT match — only a flat mana cost qualifies") {
        Effects.MayPay(
            cost = CompositeEffect(listOf(PayManaCostEffect(cost), PayLifeEffect(2))),
            then = inner
        ).asOptionalManaPayment().shouldBeNull()
    }

    test("a non-default decision-maker does NOT match") {
        GatedEffect(
            gate = Gate.MayPay(PayManaCostEffect(cost)),
            then = inner,
            decisionMaker = EffectTarget.Controller
        ).asOptionalManaPayment().shouldBeNull()
    }

    test("a MayDecide (non-payment) gate does NOT match") {
        GatedEffect(gate = Gate.MayDecide(), then = inner).asOptionalManaPayment().shouldBeNull()
    }
})
