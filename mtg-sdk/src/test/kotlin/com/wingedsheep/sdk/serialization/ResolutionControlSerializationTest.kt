package com.wingedsheep.sdk.serialization

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

class ResolutionControlSerializationTest : FunSpec({
    test("resolution control composes with a captured pipeline spell and round trips") {
        val effect = Effects.ControlPlayerDuringResolution(EffectTarget.ContextTarget(0), EffectTarget.PipelineTarget("cast"))
        val encoded = CardSerialization.json.encodeToString<Effect>(effect)
        CardSerialization.json.decodeFromString<Effect>(encoded) shouldBe effect
    }
})
