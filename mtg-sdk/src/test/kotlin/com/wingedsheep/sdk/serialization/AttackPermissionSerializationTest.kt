package com.wingedsheep.sdk.serialization

import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

class AttackPermissionSerializationTest : FunSpec({
    test("attack-only permission round trips with an attached-creature filter") {
        val ability: StaticAbility = CanAttackAsThoughHasty(GroupFilter.attachedCreature())
        val json = CardSerialization.json
        json.decodeFromString<StaticAbility>(json.encodeToString<StaticAbility>(ability)) shouldBe ability
    }
})
