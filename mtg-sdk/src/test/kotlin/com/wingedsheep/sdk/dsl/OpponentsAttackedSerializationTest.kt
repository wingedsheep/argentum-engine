package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

class OpponentsAttackedSerializationTest : FunSpec({
    test("the tracker composes into predefined Junk creation and round trips") {
        val amount = DynamicAmounts.opponentsAttackedThisTurn()
        Json.decodeFromString(DynamicAmount.serializer(), Json.encodeToString(DynamicAmount.serializer(), amount)) shouldBe amount
        val effect = Effects.CreateJunk(amount)
        Json.decodeFromString(Effect.serializer(), Json.encodeToString(Effect.serializer(), effect)) shouldBe effect
    }
})
