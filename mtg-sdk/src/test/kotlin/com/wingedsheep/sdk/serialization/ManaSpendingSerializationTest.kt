package com.wingedsheep.sdk.serialization

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.scripting.StaticAbility
import com.wingedsheep.sdk.scripting.SpendManaAsColor
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

class ManaSpendingSerializationTest : FunSpec({
    test("directional spending permission round trips as a static ability") {
        val ability: StaticAbility = SpendManaAsColor(Color.WHITE, Color.RED)
        val encoded = Json.encodeToString(StaticAbility.serializer(), ability)
        Json.decodeFromString(StaticAbility.serializer(), encoded) shouldBe ability
        ability.description shouldBe "You may spend white mana as though it were red mana."
    }
})
