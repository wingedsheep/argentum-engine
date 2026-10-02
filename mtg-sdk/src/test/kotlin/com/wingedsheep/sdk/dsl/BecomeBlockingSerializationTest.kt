package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

class BecomeBlockingSerializationTest : FunSpec({
    test("blocking effect preserves two independent symbolic references") {
        val effect = Effects.BecomeBlocking(EffectTarget.ContextTarget(0), EffectTarget.PipelineTarget("attacker"))
        Json.decodeFromString<Effect>(Json.encodeToString<Effect>(effect)) shouldBe effect
    }
    test("defending-side attacker filter preserves its reference") {
        val filter = GameObjectFilter.Creature.attackingDefenderOf(EffectTarget.ContextTarget(0))
        Json.decodeFromString<GameObjectFilter>(Json.encodeToString(filter)) shouldBe filter
    }
})
