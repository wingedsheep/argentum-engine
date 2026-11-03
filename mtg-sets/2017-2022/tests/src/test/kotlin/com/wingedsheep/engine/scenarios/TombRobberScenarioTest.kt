package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.matchers.shouldBe

/**
 * Tomb Robber (RIX #87) — "Menace. {1}, Discard a card: This creature explores."
 */
class TombRobberScenarioTest : ScenarioTestBase() {

    private val abilityId = cardRegistry.getCard("Tomb Robber")!!.activatedAbilities.first().id

    init {
        test("paying {1} and discarding explores, putting a revealed land into hand") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Tomb Robber")
                .withLandsOnBattlefield(1, "Swamp", 1)
                .withCardInHand(1, "Grizzly Bears")
                .withCardInLibrary(1, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val robber = game.findPermanent("Tomb Robber")!!
            val bears = game.findCardsInHand(1, "Grizzly Bears").single()
            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = robber,
                    abilityId = abilityId,
                    costPayment = AdditionalCostPayment(discardedCards = listOf(bears)),
                )
            ).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            game.isInHand(1, "Forest") shouldBe true
        }
    }
}
