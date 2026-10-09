package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.scripting.ExtraLoyaltyActivation
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.StaticAbility
import io.kotest.core.spec.style.FunSpec
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

class LoyaltyAllowanceSerializationTest : FunSpec({
    test("old Oath JSON retains its original allowance") {
        Json.decodeFromString<StaticAbility>("""{"type":"ExtraLoyaltyActivation"}""") shouldBe ExtraLoyaltyActivation()
    }
    test("self and filtered allowances round trip with arbitrary counts") {
        for (filter in listOf(GameObjectFilter.Any.sourceItself(), GameObjectFilter.Planeswalker.youControl())) {
            val ability: StaticAbility = ExtraLoyaltyActivation(filter, times = 3)
            Json.decodeFromString<StaticAbility>(Json.encodeToString(ability)) shouldBe ability
        }
    }
    test("an extra allowance cannot lower the normal limit") {
        shouldThrow<IllegalArgumentException> { ExtraLoyaltyActivation(times = 1) }
    }
})
