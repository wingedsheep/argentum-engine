package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Pyretic Prankster // Glistening Goremonger (MOM #157).
 * Transforms into a 3/2; the back face makes each opponent sacrifice an artifact or creature on death.
 */
class PyreticPranksterScenarioTest : ScenarioTestBase() {

    init {
        context("Pyretic Prankster") {
            test("transforms into a 3/2 Glistening Goremonger") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Pyretic Prankster")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val id = game.findPermanent("Pyretic Prankster")!!
                val abilityId = cardRegistry.getCard("Pyretic Prankster")!!.activatedAbilities.first().id
                game.execute(ActivateAbility(playerId = game.player1Id, sourceId = id, abilityId = abilityId))
                    .error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                val back = game.findPermanent("Glistening Goremonger")!!
                game.state.projectedState.getPower(back) shouldBe 3
                game.state.projectedState.getToughness(back) shouldBe 2
            }
        }
    }
}
