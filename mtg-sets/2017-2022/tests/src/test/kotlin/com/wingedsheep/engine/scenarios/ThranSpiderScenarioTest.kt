package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Thran Spider (BRO #254) — {3} Artifact Creature — Spider 2/4, reach.
 *
 *   When this creature enters, you and target opponent each create a tapped Powerstone token.
 *
 * The second token is redirected to the *targeted* opponent through `CreatePowerstone`'s
 * `controller`; the test pins that each player ends up controlling exactly one tapped Powerstone.
 */
class ThranSpiderScenarioTest : ScenarioTestBase() {

    init {
        test("entering gives you and the target opponent one tapped Powerstone each") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Thran Spider")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Thran Spider").error shouldBe null
            game.resolveStack()

            // The only opponent is the only legal target, so the engine may bind it without asking.
            if (game.getPendingDecision() != null) {
                game.selectTargets(listOf(game.player2Id))
                game.resolveStack()
            }

            val powerstones = game.findAllPermanents("Powerstone")
            powerstones shouldHaveSize 2
            val controllers = powerstones.map { game.state.projectedState.getController(it) }.toSet()
            controllers shouldBe setOf(game.player1Id, game.player2Id)
            powerstones.forEach { id ->
                val entity = game.state.getEntity(id)!!
                entity.has<TokenComponent>() shouldBe true
                entity.has<TappedComponent>() shouldBe true
            }
        }
    }
}
