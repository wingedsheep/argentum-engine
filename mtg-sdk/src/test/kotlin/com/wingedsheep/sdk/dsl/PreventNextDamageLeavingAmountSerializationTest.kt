package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

class PreventNextDamageLeavingAmountSerializationTest : FunSpec({
    test("prevention preserves amount, recipient, source qualification, combat scope and duration") {
        val effect = Effects.PreventNextDamageLeavingAmount(
            DynamicAmount.XValue, EffectTarget.ContextTarget(0), GameObjectFilter.Creature.unblocked(),
            combatOnly = true, duration = Duration.EndOfCombat
        )
        Json.decodeFromString<Effect>(Json.encodeToString<Effect>(effect)) shouldBe effect
    }
})
