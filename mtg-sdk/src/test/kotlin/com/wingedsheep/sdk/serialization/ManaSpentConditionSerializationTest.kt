package com.wingedsheep.sdk.serialization

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.scripting.conditions.Condition
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ManaSpentConditionSerializationTest : FunSpec({
    test("mixed colorless and colored payment conditions round trip through the facade") {
        val condition = Conditions.ManaSpentToCastIncludes(requiredGreen = 1, requiredColorless = 2)
        val json = CardSerialization.json
        json.decodeFromString(Condition.serializer(), json.encodeToString(Condition.serializer(), condition)) shouldBe condition
        condition.description shouldBe "if {G}{C}{C} was spent to cast it"
    }
})
