package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.StaticAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

class ZoneAbilityGrantSerializationTest : FunSpec({
    test("recipient zone round trips independently of the ability's activation zone") {
        for (zone in listOf(Zone.BATTLEFIELD, Zone.GRAVEYARD, Zone.HAND)) {
            val grant: StaticAbility = GrantActivatedAbility(
                unearthAbility(ManaCost.parse("{1}{B}{R}")),
                GroupFilter(GameObjectFilter.Artifact.ownedByYou()), recipientZone = zone
            )
            Json.decodeFromString<StaticAbility>(Json.encodeToString(grant)) shouldBe grant
        }
    }
    test("omitted zone retains the battlefield default and old JSON shape") {
        val grant = GrantActivatedAbility(unearthAbility(ManaCost.parse("{1}")))
        grant.recipientZone shouldBe Zone.BATTLEFIELD
        Json.encodeToString<StaticAbility>(grant).contains("recipientZone") shouldBe false
    }
})
