package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class OwnerTargetDistributionSerializationTest : FunSpec({
    test("both owner distribution modes round-trip and the default remains absent") {
        for (mode in OwnerDistribution.entries) {
            val req: TargetRequirement = TargetObject(
                filter = TargetFilter(GameObjectFilter.InstantOrSorcery, Zone.GRAVEYARD), ownerDistribution = mode)
            Json.decodeFromString<TargetRequirement>(Json.encodeToString(req)) shouldBe req
        }
        TargetObject(filter = TargetFilter.Creature).ownerDistribution shouldBe null
        Json.encodeToString<TargetRequirement>(TargetObject(filter = TargetFilter.Creature)).contains("ownerDistribution") shouldBe false
    }
})
