package com.wingedsheep.sdk.serialization

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

class LoseUnspentManaSerializationTest : FunSpec({
    test("player target survives polymorphic effect round trip") {
        val effect = Effects.LoseUnspentMana(EffectTarget.ContextTarget(0))
        val encoded = CardSerialization.json.encodeToString<Effect>(effect)
        CardSerialization.json.decodeFromString<Effect>(encoded) shouldBe effect
    }
})
