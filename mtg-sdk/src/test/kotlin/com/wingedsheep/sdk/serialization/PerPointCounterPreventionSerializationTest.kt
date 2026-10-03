package com.wingedsheep.sdk.serialization

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.events.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

class PerPointCounterPreventionSerializationTest : FunSpec({
    test("counter and damage scope survive polymorphic round trip") {
        val replacement: ReplacementEffect = PreventDamagePerCounter(CounterType.CHARGE,
            EventPattern.DamageEvent(recipient = Recipient.Any, source = GameObjectFilter.Creature.withColor(Color.RED),
                damageType = DamageType.NonCombat))
        CardSerialization.json.decodeFromString<ReplacementEffect>(
            CardSerialization.json.encodeToString<ReplacementEffect>(replacement)) shouldBe replacement
    }
})
