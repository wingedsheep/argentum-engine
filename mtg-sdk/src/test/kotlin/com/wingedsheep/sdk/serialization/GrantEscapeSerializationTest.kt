package com.wingedsheep.sdk.serialization

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class GrantEscapeSerializationTest : StringSpec({
    "escape grants preserve inherited or explicit mana costs and their exile payment" {
        for (cost in listOf(null, ManaCost.ZERO, ManaCost.parse("{2}{R}"))) {
            val original = Effects.GrantEscape(EffectTarget.ContextTarget(0), cost, Costs.additional.ExileOtherCards(2))
            CardSerialization.json.decodeFromString(Effect.serializer(),
                CardSerialization.json.encodeToString(Effect.serializer(), original)) shouldBe original
        }
    }
})
