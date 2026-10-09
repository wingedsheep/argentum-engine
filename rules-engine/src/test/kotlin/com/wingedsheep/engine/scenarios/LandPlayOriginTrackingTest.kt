package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.LandPlayedEvent
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.components.player.LandsPlayedThisTurnComponent
import com.wingedsheep.engine.state.permissions.MayPlayPermission
import com.wingedsheep.engine.state.permissions.addMayPlayPermission
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.matchers.shouldBe

class LandPlayOriginTrackingTest : ScenarioTestBase() {
    private fun game() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInExile(2, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.playedFromExile() = PredicateEvaluator(cardRegistry = null).conditions.evaluate(
        state, Conditions.YouPlayedLandThisTurn(fromZone = Zone.EXILE),
        EffectContext(sourceId = null, controllerId = player1Id)
    )

    init {
        for (duringResolution in listOf(false, true)) {
            test("exile land play records the player and original zone duringResolution=$duringResolution") {
                val g = game()
                val land = g.state.getExile(g.player2Id).single()
                val (id, withId) = g.state.newEntity()
                g.state = withId.addMayPlayPermission(MayPlayPermission(
                    id, setOf(land), g.player1Id, timestamp = withId.timestamp
                ))
                g.playedFromExile() shouldBe false
                val result = if (duringResolution) services.playLandHandler.executeDuringResolution(
                    g.state, PlayLand(g.player1Id, land)
                ) else g.execute(PlayLand(g.player1Id, land))
                result.error shouldBe null
                g.state = result.state
                result.events.filterIsInstance<LandPlayedEvent>().single().fromZone shouldBe Zone.EXILE
                g.playedFromExile() shouldBe true
                g.state.getEntity(g.player1Id)!!.get<LandsPlayedThisTurnComponent>()!!.fromZones shouldBe listOf(Zone.EXILE)
                g.state.getEntity(g.player2Id)!!.get<LandsPlayedThisTurnComponent>()?.fromZones.orEmpty() shouldBe emptyList()
            }
        }
        test("an effect putting a land onto the battlefield from exile is not a land play") {
            val g = game()
            val land = g.state.getExile(g.player2Id).single()
            val result = services.effectExecutorRegistry.execute(
                g.state, Effects.PutOntoBattlefield(EffectTarget.SpecificEntity(land)),
                EffectContext(sourceId = null, controllerId = g.player1Id)
            )
            result.error shouldBe null
            g.state = result.state
            (land in g.state.getBattlefield(g.player2Id)) shouldBe true
            result.events.filterIsInstance<LandPlayedEvent>() shouldBe emptyList()
            g.playedFromExile() shouldBe false
        }
        test("a rejected exile land play does not update turn history") {
            val g = game()
            val land = g.state.getExile(g.player2Id).single()
            (g.execute(PlayLand(g.player1Id, land)).error != null) shouldBe true
            g.playedFromExile() shouldBe false
        }
    }
}
