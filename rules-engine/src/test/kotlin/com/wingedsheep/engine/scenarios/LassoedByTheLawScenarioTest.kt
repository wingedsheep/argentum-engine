package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.nulls.shouldNotBeNull

/** Scenario tests for Lassoed by the Law. */
class LassoedByTheLawScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    init {
        context("Lassoed by the Law") {
            test("exiles a nonland permanent AND creates a 1/1 red Mercenary token") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Lassoed by the Law")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val victim = game.findPermanent("Hill Giant")!!
                val cast = game.castSpell(1, "Lassoed by the Law")
                withClue("Cast should succeed: ${cast.error}") { cast.error shouldBe null }
                // Two ETB triggers go on the stack; the exile trigger asks for a target.
                game.resolveStack()
                game.selectTargets(listOf(victim))
                game.resolveStack()

                withClue("Hill Giant should be exiled") {
                    game.isOnBattlefield("Hill Giant") shouldBe false
                    game.state.getExile(game.player2Id).count {
                        game.state.getEntity(it)?.get<CardComponent>()?.name == "Hill Giant"
                    } shouldBe 1
                }
                withClue("A 1/1 red Mercenary token should have been created for the controller") {
                    val token = game.findPermanent("Mercenary Token")
                    token.shouldNotBeNull()
                    projector.getProjectedPower(game.state, token) shouldBe 1
                    projector.getProjectedToughness(game.state, token) shouldBe 1
                }
            }
        }
    }
}
