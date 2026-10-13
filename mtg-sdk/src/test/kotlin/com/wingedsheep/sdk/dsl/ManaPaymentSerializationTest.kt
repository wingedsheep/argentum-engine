package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.ManaPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

class ManaPaymentSerializationTest : FunSpec({
    test("existing cast provenance JSON keeps its meaning") {
        val original = """{"type":"ManaSpentFromSubtype","subtype":"Treasure"}"""
        Json.decodeFromString<DynamicAmount>(original) shouldBe
            DynamicAmounts.manaSpentFromSubtype(Subtype.TREASURE)
    }
    test("activation provenance round-trips within a composed dice amount") {
        val amount = DynamicAmounts.fixed(1) +
            DynamicAmounts.manaSpentFromSubtype(Subtype.TREASURE, ManaPayment.ACTIVATION)
        Json.decodeFromString<DynamicAmount>(Json.encodeToString<DynamicAmount>(amount)) shouldBe amount
    }
})
