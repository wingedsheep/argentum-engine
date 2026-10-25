package com.wingedsheep.sdk.serialization

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.EntryCounter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

class MoveEntryCounterSerializationTest : FunSpec({
    test("single and collection move counter instructions round trip") {
        val effects = listOf(
            Effects.Move(EffectTarget.Self, Zone.BATTLEFIELD,
                addCounterType = CounterType.FINALITY, addCounterIf = GameObjectFilter.Creature),
            Effects.Move(EffectTarget.Self, Zone.BATTLEFIELD, entryCounters = listOf(
                EntryCounter(CounterType.PLUS_ONE_PLUS_ONE, GameObjectFilter.Creature),
                EntryCounter(CounterType.LOYALTY, GameObjectFilter.Planeswalker))),
            Effects.Pipeline {
                val cards = gather(com.wingedsheep.sdk.scripting.effects.CardSource.Entity(com.wingedsheep.sdk.scripting.targets.EffectTarget.Self))
                move(cards, com.wingedsheep.sdk.scripting.effects.CardDestination.ToZone(Zone.BATTLEFIELD), addCounterType = CounterType.PLUS_ONE_PLUS_ONE)
            }
        )
        for (effect in effects) {
            val json = CardSerialization.json.encodeToString<Effect>(effect)
            CardSerialization.json.decodeFromString<Effect>(json) shouldBe effect
        }
    }
})
