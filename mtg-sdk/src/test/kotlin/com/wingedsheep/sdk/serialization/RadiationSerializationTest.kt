package com.wingedsheep.sdk.serialization

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.effects.*
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

class RadiationSerializationTest : FunSpec({
    test("radiation life-loss provenance round trips") {
        val effect = Effects.LoseLife(DynamicAmount.Fixed(3), EffectTarget.Controller, LifeLossOrigin.RADIATION)
        val encoded = CardSerialization.json.encodeToString<Effect>(effect)
        CardSerialization.json.decodeFromString<Effect>(encoded) shouldBe effect
    }
    test("ordinary life loss keeps its old serialized form") {
        val effect: Effect = LoseLifeEffect(2)
        CardSerialization.json.encodeToString(effect).contains("origin") shouldBe false
        CardSerialization.json.decodeFromString<Effect>(CardSerialization.json.encodeToString(effect)) shouldBe effect
    }
    test("rad is a named counter serialized with its canonical id") {
        val encoded = CardSerialization.json.encodeToString(CounterType.RAD)
        encoded shouldBe "\"RAD\""
        CardSerialization.json.decodeFromString<CounterType>(encoded) shouldBe CounterType.RAD
    }
})
