package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.PlayerActionTiming
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

class PlayerActionSerializationTest : FunSpec({
    test("player special actions round trip nested effects, costs, timing, and duration") {
        for (timing in PlayerActionTiming.entries) for (duration in listOf(Duration.EndOfTurn, Duration.Permanent)) {
            val effect: Effect = Effects.GrantPlayerAction(Costs.pay.PayLife(2), Effects.DrawCards(1), timing, "Pay 2 life: draw a card", duration = duration)
            Json.decodeFromString<Effect>(Json.encodeToString(effect)) shouldBe effect
        }
    }
})
