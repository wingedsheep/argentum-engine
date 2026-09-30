package com.wingedsheep.sdk.serialization

import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

class ContinuousControlSerializationTest : FunSpec({
    test("continuous control composes with player and creature predicates and round trips") {
        val target = TargetFilter.Creature.controlledByActivePlayer().controlledSinceTurnBegan()
        val encoded = CardSerialization.json.encodeToString(target)
        CardSerialization.json.decodeFromString<TargetFilter>(encoded) shouldBe target
    }
})
