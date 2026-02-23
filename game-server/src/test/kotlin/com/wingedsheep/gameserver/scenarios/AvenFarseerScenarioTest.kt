package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Aven Farseer.
 *
 * Card reference:
 * - Aven Farseer ({1}{W}): Creature — Bird Soldier 1/1
 *   Flying
 *   Whenever a permanent is turned face up, put a +1/+1 counter on this creature.
 */
class AvenFarseerScenarioTest : ScenarioTestBase() {

    init {
        context("Aven Farseer") {

            test("gets a +1/+1 counter when a morph creature is turned face up") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Aven Farseer")
                    .withCardInHand(1, "Battering Craghorn")
                    .withLandsOnBattlefield(1, "Mountain", 6)
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Battering Craghorn face-down for {3}
                val morphCardId = game.state.getHand(game.player1Id).first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Battering Craghorn"
                }
                val castResult = game.execute(CastSpell(game.player1Id, morphCardId, castFaceDown = true))
                withClue("Cast face-down should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                // Find the face-down creature
                val faceDownId = game.state.getBattlefield().find { entityId ->
                    game.state.getEntity(entityId)?.has<FaceDownComponent>() == true
                }
                withClue("Face-down creature should be on battlefield") {
                    faceDownId shouldNotBe null
                }

                // Turn Battering Craghorn face up (morph cost {1}{R}{R})
                val turnUpResult = game.execute(TurnFaceUp(game.player1Id, faceDownId!!))
                withClue("Turn face-up should succeed: ${turnUpResult.error}") {
                    turnUpResult.error shouldBe null
                }

                // Aven Farseer's trigger should fire — resolve it
                game.resolveStack()

                // Verify Aven Farseer got a +1/+1 counter (base 1/1 → projected 2/2)
                val farseerId = game.findPermanent("Aven Farseer")!!
                val counters = game.state.getEntity(farseerId)?.get<CountersComponent>()
                withClue("Aven Farseer should have a +1/+1 counter") {
                    counters shouldNotBe null
                    counters!!.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
                }

                val projected = StateProjector().project(game.state)
                withClue("Aven Farseer should be 2/2 after getting a +1/+1 counter") {
                    projected.getPower(farseerId) shouldBe 2
                    projected.getToughness(farseerId) shouldBe 2
                }
            }
        }
    }
}
