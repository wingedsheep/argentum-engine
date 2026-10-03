package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CountersAddedEvent
import com.wingedsheep.engine.core.SourceObjectsRecordedEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PipelineState
import com.wingedsheep.engine.handlers.effects.library.GatherCardsExecutor
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.CantReceiveCounters
import com.wingedsheep.sdk.scripting.DoubleCounterPlacement
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class CounterPlacementSuccessCriterionTest : FunSpec({
    val kind = CounterType.PLUS_ONE_PLUS_ONE
    val warded = card("Test Counter Prevention") {
        typeLine = "Creature"
        power = 1
        toughness = 1
        staticAbility { ability = CantReceiveCounters(GroupFilter.source()) }
    }
    val doubler = card("Test Placement Doubler") {
        typeLine = "Enchantment"
        replacementEffect(DoubleCounterPlacement(appliesTo = EventPattern.CounterPlacementEvent(kind, Recipient.AnyPermanent)))
    }
    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(warded, doubler))
        it.initMirrorMatch(Deck.of("Mountain" to 40))
    }
    for (prevented in listOf(false, true)) for (doubled in listOf(false, true)) {
        test("source history records actual placement: prevented=$prevented doubled=$doubled") {
            val d = driver()
            val source = d.putCreatureOnBattlefield(d.player1, if (prevented) warded.name else "Centaur Courser")
            if (doubled) d.putPermanentOnBattlefield(d.player1, doubler.name)
            val ctx = EffectContext(sourceId = source, controllerId = d.player1,
                pipeline = PipelineState(storedCollections = mapOf("placed" to listOf(source))))
                .withCurrentObjectReferences(d.state)
            val result = d.services.effectExecutorRegistry.execute(d.state, Effects.IfYouDo(
                Effects.AddCounters(kind, 1, EffectTarget.Self), Effects.RecordSourceObjects("placed", "marked"),
                successCriterion = SuccessCriterion.CountersAdded), ctx)
            result.events.filterIsInstance<SourceObjectsRecordedEvent>().size shouldBe if (prevented) 0 else 1
            result.events.filterIsInstance<CountersAddedEvent>().sumOf { it.amount } shouldBe
                if (prevented) 0 else if (doubled) 2 else 1
            val remembered = GatherCardsExecutor(d.services.predicateEvaluator).execute(result.state,
                GatherCardsEffect(CardSource.SourceLinkedBattlefield("marked"), "remembered"), ctx)
            remembered.updatedCollections["remembered"] shouldBe if (prevented) emptyList() else listOf(source)
        }
    }
    test("zero placement cannot create a record and criterion round-trips") {
        val d = driver()
        val source = d.putCreatureOnBattlefield(d.player1, "Centaur Courser")
        val ctx = EffectContext(sourceId = source, controllerId = d.player1,
            pipeline = PipelineState(storedCollections = mapOf("placed" to listOf(source))))
        val result = d.services.effectExecutorRegistry.execute(d.state, Effects.IfYouDo(
            Effects.AddCounters(kind, 0, EffectTarget.Self), Effects.RecordSourceObjects("placed", "marked"),
            successCriterion = SuccessCriterion.CountersAdded), ctx)
        result.state.sourceObjectRecords shouldBe emptyMap()
        result.state.getEntity(source)!!.get<CountersComponent>()?.getCount(kind) shouldBe null
        val criterion: SuccessCriterion = SuccessCriterion.CountersAdded
        Json.decodeFromString<SuccessCriterion>(Json.encodeToString(criterion)) shouldBe criterion
    }
})
