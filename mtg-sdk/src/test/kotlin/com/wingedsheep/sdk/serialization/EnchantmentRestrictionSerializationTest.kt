package com.wingedsheep.sdk.serialization

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

class EnchantmentRestrictionSerializationTest : FunSpec({
    test("filtered source exception round trips as a static ability") {
        val ability: StaticAbility = PreventEnchantment(
            auras = GameObjectFilter.Enchantment.withSubtype("Aura").withColor(Color.RED),
            exceptSource = true, filter = GroupFilter.attachedCreature())
        val json = CardSerialization.json
        json.decodeFromString<StaticAbility>(json.encodeToString<StaticAbility>(ability)) shouldBe ability
    }
})
