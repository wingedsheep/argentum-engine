package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

class ChosenSourceDamageRedirectionSerializationTest : FunSpec({
    test("source-choice redirection roundtrips with any recipients and duration") {
        val effect = Effects.RedirectDamageFromChosenSource(
            EffectTarget.ContextTarget(1), EffectTarget.ContextTarget(0), Duration.EndOfCombat)
        Json.decodeFromString<Effect>(Json.encodeToString<Effect>(effect)) shouldBe effect
    }
})
