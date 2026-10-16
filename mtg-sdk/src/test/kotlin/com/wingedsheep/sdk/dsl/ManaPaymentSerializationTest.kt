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
    test("existing colors-spent JSON reads the cast payment; activation round-trips") {
        Json.decodeFromString<DynamicAmount>("""{"type":"DistinctColorsManaSpent"}""") shouldBe
            DynamicAmounts.colorsOfManaSpent()
        val amount = DynamicAmounts.colorsOfManaSpent(ManaPayment.ACTIVATION)
        Json.decodeFromString<DynamicAmount>(Json.encodeToString<DynamicAmount>(amount)) shouldBe amount
    }
    test("mixed token batch round-trips without losing member art or counters") {
        val batch = Effects.CreateTokensTogether(
            Effects.CreateToken(power = 3, toughness = 3, creatureTypes = setOf("Robot"),
                artifactToken = true, imageUri = "robot", initialCounters = mapOf(com.wingedsheep.sdk.core.CounterType.PLUS_ONE_PLUS_ONE to 2)),
            Effects.CreateTreasure(tapped = true, imageUri = "treasure"),
        )
        Json.decodeFromString<com.wingedsheep.sdk.scripting.effects.Effect>(
            Json.encodeToString<com.wingedsheep.sdk.scripting.effects.Effect>(batch)
        ) shouldBe batch
    }

})
