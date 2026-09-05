package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.core.engineSerializersModule
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

class EnterCounterObjectIdentityTest : FunSpec({
    val observer = card("Identity Counter Observer") {
        manaCost = "{0}"; typeLine = "Creature — Human"; power = 1; toughness = 1
        triggeredAbility {
            trigger = Triggers.countersPlacedOn()
            effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.TriggeringEntity)
        }
    }
    val entering = card("Identity Counter Entrant") {
        manaCost = "{0}"; typeLine = "Creature — Construct"; power = 1; toughness = 1
        replacementEffect(EntersWithCounters(CounterTypeFilter.PlusOnePlusOne, 1, selfOnly = true))
    }
    val json = Json { serializersModule = engineSerializersModule; allowStructuredMapKeys = true }
    for (blink in listOf(false, true)) {
        test("entry counter trigger captures battlefield object before later events; blink=$blink") {
            val d = GameTestDriver().apply {
                registerCards(TestCards.all + listOf(observer, entering))
                initMirrorMatch(Deck.of("Plains" to 40), startingPlayer = 0)
                passPriorityUntil(Step.PRECOMBAT_MAIN)
            }
            d.putCreatureOnBattlefield(d.player1, observer.name)
            val entrant = d.putCardInHand(d.player1, entering.name)
            d.castSpell(d.player1, entrant).error shouldBe null
            val spellRef = d.state.objectRef(entrant)!!
            d.bothPass().error shouldBe null
            d.stackSize shouldBe 1
            val entryRef = d.state.objectRef(entrant)!!
            val entry = d.events.filterIsInstance<ZoneChangeEvent>().single { it.entityId == entrant && it.toZone == Zone.BATTLEFIELD }
            entry.oldObject shouldBe spellRef
            entry.newObject shouldBe entryRef
            val ability = d.state.getEntity(d.state.stack.single())!!.get<TriggeredAbilityOnStackComponent>()!!
            ability.objectReferences.triggering shouldBe entryRef
            if (blink) {
                d.replaceState(d.state.moveToZone(entrant, ZoneKey(d.player1, Zone.BATTLEFIELD), ZoneKey(d.player1, Zone.EXILE))
                    .moveToZone(entrant, ZoneKey(d.player1, Zone.EXILE), ZoneKey(d.player1, Zone.BATTLEFIELD)))
            }
            val countersBefore = d.state.getEntity(entrant)!!.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
            d.replaceState(json.decodeFromString(GameState.serializer(), json.encodeToString(GameState.serializer(), d.state)))
            d.bothPass().error shouldBe null
            val countersAfter = d.state.getEntity(entrant)!!.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
            countersAfter shouldBe countersBefore + if (blink) 0 else 1
        }
    }
})
