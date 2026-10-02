package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class SacrificedManaValueSerializationTest : FunSpec({
    test("indexed sacrificed mana value retains its reference through serialization") {
        val amount = DynamicAmounts.sacrificedManaValue(2)
        amount shouldBe DynamicAmount.EntityProperty(EffectTarget.SacrificedAsCost(2), EntityNumericProperty.ManaValue)
        Json.decodeFromString<DynamicAmount>(Json.encodeToString(amount)) shouldBe amount
    }
})
