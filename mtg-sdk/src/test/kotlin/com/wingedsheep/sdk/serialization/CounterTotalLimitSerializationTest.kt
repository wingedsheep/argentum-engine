package com.wingedsheep.sdk.serialization

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

class CounterTotalLimitSerializationTest : FunSpec({
    test("dynamic choice and bounded placement round-trip polymorphically") {
        val effect = Effects.ChooseNumberThen(
            maxValue = DynamicAmount.XValue,
            then = Effects.AddCountersWithLimit(CounterType.PLUS_ONE_PLUS_ZERO, DynamicAmount.XValue, DynamicAmount.Fixed(7), EffectTarget.Self)
        )
        val json = CardSerialization.json
        json.decodeFromString<Effect>(json.encodeToString<Effect>(effect)) shouldBe effect
    }
    test("fixed choice facade retains its bounds") {
        val effect = Effects.ChooseNumberThen(Effects.DrawCards(1), maxValue = 3)
        val json = CardSerialization.json
        json.decodeFromString<Effect>(json.encodeToString<Effect>(effect)) shouldBe effect
    }
})
