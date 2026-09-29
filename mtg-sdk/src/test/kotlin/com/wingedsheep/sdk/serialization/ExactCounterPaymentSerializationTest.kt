package com.wingedsheep.sdk.serialization

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ExactCounterPaymentSerializationTest : FunSpec({
    test("exact fixed and dynamic payments retain payer and amount through serialization") {
        for (payment in listOf(
            Effects.PayExactCounters(CounterType.ENERGY, 3),
            Effects.PayExactCounters(CounterType.POISON, DynamicAmounts.targetManaValue(), Player.AnOpponent)
        )) {
            val effect = Effects.MayPay(payment, Effects.DrawCards(1), Effects.LoseLife(2))
            val json = CardSerialization.json.encodeToString(Effect.serializer(), effect)
            CardSerialization.json.decodeFromString(Effect.serializer(), json) shouldBe effect
        }
    }
})
