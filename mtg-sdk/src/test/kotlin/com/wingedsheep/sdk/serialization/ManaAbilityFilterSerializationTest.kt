package com.wingedsheep.sdk.serialization

import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

class ManaAbilityFilterSerializationTest : FunSpec({
    test("mana-ability presence round trips in a composed land filter") {
        val filter = GameObjectFilter.Land.withManaAbility().youControl()
        val json = CardSerialization.json
        json.decodeFromString<GameObjectFilter>(json.encodeToString(filter)) shouldBe filter
    }
})
