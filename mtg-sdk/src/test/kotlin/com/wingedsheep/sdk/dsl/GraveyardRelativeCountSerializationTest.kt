package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

class GraveyardRelativeCountSerializationTest : FunSpec({
    test("positional counts retain entity, direction and filter") {
        for (value in listOf(DynamicAmounts.cardsAboveInGraveyard(EffectTarget.ContextTarget(0), GameObjectFilter.Creature),
            DynamicAmounts.cardsBelowInGraveyard(EffectTarget.Self, GameObjectFilter.Artifact))) {
            Json.decodeFromString<DynamicAmount>(Json.encodeToString<DynamicAmount>(value)) shouldBe value
        }
    }
})
