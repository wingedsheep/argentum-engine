package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.serialization.CardSerialization
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class SwitchPowerToughnessTest : FunSpec({
    test("target and duration survive effect serialization") {
        val effect = Effects.SwitchPowerToughness(EffectTarget.Self, Duration.Permanent)
        val json = CardSerialization.compactJson
        val encoded = json.encodeToString(Effect.serializer(), effect)
        json.decodeFromString(Effect.serializer(), encoded) shouldBe effect
    }
})
