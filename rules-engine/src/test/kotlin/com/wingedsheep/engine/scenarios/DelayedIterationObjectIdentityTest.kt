package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.engineSerializersModule
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.ObjectReferenceEnvironment
import com.wingedsheep.engine.handlers.PipelineState
import com.wingedsheep.engine.handlers.effects.EffectExecutorRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect
import com.wingedsheep.sdk.scripting.effects.ForEachInCollectionEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

class DelayedIterationObjectIdentityTest : FunSpec({
    val json = Json {
        serializersModule = engineSerializersModule
        allowStructuredMapKeys = true
        ignoreUnknownKeys = true
        encodeDefaults = true
        classDiscriminator = "type"
    }
    fun roundTrip(driver: GameTestDriver) {
        driver.replaceState(json.decodeFromString(GameState.serializer(),
            json.encodeToString(GameState.serializer(), driver.state)))
    }

    for (sacrifice in listOf(false, true)) {
        for (blinkOne in listOf(false, true)) {
            test("serialized delayed collection Self affects each original object: sacrifice=$sacrifice blinkOne=$blinkOne") {
                val d = GameTestDriver().apply {
                    registerCards(TestCards.all)
                    initMirrorMatch(Deck.of("Plains" to 40), startingPlayer = 0)
                    passPriorityUntil(Step.PRECOMBAT_MAIN)
                }
                val source = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
                val first = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
                val second = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
                val origin = d.state.objectRef(source)!!
                val firstObject = d.state.objectRef(first)!!
                val secondObject = d.state.objectRef(second)!!
                val destination = if (sacrifice) Zone.GRAVEYARD else Zone.HAND
                val cleanup = if (sacrifice) Effects.SacrificeTarget(EffectTarget.Self)
                    else Effects.ReturnToHand(EffectTarget.Self)
                val result = EffectExecutorRegistry(cardRegistry = d.cardRegistry).execute(d.state,
                    ForEachInCollectionEffect("cleanup", CreateDelayedTriggerEffect(step = Step.END, effect = cleanup)),
                    EffectContext(sourceId = source, controllerId = d.player1,
                        objectReferences = ObjectReferenceEnvironment(captured = true,
                            origin = origin, source = origin, resolutionKey = "delayed-collection-test"),
                        pipeline = PipelineState(storedCollections = mapOf("cleanup" to listOf(first, second)))))
                result.error shouldBe null
                d.replaceState(result.state)
                d.state.delayedTriggers.size shouldBe 2
                d.state.delayedTriggers.map { it.objectReferences.origin } shouldBe listOf(origin, origin)
                roundTrip(d)
                if (blinkOne) {
                    val battlefield = ZoneKey(d.player1, Zone.BATTLEFIELD)
                    val exile = ZoneKey(d.player1, Zone.EXILE)
                    d.replaceState(d.state.moveToZone(first, battlefield, exile).moveToZone(first, exile, battlefield))
                }
                roundTrip(d)
                d.passPriorityUntil(Step.END)
                while (d.pendingDecision != null) d.autoResolveDecision()
                d.stackSize shouldBe 2
                roundTrip(d)
                repeat(2) { d.bothPass().error shouldBe null }
                d.stackSize shouldBe 0
                d.state.delayedTriggers.size shouldBe 0
                d.state.isCurrentObject(origin) shouldBe true
                (source in d.state.getZone(ZoneKey(d.player1, Zone.BATTLEFIELD))) shouldBe true
                (first in d.state.getZone(ZoneKey(d.player1, destination))) shouldBe !blinkOne
                (first in d.state.getZone(ZoneKey(d.player1, Zone.BATTLEFIELD))) shouldBe blinkOne
                (second in d.state.getZone(ZoneKey(d.player1, destination))) shouldBe true
                d.state.isCurrentObject(firstObject) shouldBe false
                d.state.isCurrentObject(secondObject) shouldBe false
            }
        }
    }
})
