package com.wingedsheep.sdk.serialization

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

class ForcePlaySerializationTest : FunSpec({
    test("a paid play instruction preserves affected player and completed-play collection") {
        val effect = Effects.ForcePlay("chosen", EffectTarget.ContextTarget(0), "played")
        CardSerialization.json.decodeFromString<Effect>(CardSerialization.json.encodeToString<Effect>(effect)) shouldBe effect
    }
})
