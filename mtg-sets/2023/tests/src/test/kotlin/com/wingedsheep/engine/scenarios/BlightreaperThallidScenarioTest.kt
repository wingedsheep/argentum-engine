package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Blightreaper Thallid // Blightsower Thallid (MOM #92).
 * Transforming into the back face creates a Saproling; the back face also makes one when it dies.
 */
class BlightreaperThallidScenarioTest : ScenarioTestBase() {

    private fun transform(game: TestGame) {
        val id = game.findPermanent("Blightreaper Thallid")!!
        val abilityId = cardRegistry.getCard("Blightreaper Thallid")!!.activatedAbilities.first().id
        game.execute(ActivateAbility(playerId = game.player1Id, sourceId = id, abilityId = abilityId))
            .error shouldBe null
        if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
        game.resolveStack()
    }

    init {
        context("Blightreaper Thallid") {
            test("transforms into a 3/3 and creates a Phyrexian Saproling") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Blightreaper Thallid")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                transform(game)

                val id = game.findPermanent("Blightsower Thallid")!!
                game.state.projectedState.getToughness(id) shouldBe 3
                game.findPermanents("Phyrexian Saproling Token").size shouldBe 1
            }
        }
    }
}
