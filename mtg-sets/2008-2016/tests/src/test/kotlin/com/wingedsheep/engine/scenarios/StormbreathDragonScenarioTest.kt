package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Stormbreath Dragon (THS #143): "When this creature becomes monstrous, it deals damage to each
 * opponent equal to the number of cards in that player's hand." The amount is the opponent's own
 * hand size (read per opponent through the ForEachPlayer rebind), not the controller's.
 */
class StormbreathDragonScenarioTest : ScenarioTestBase() {

    init {
        test("becoming monstrous deals damage to the opponent equal to that opponent's hand size") {
            val game = scenario().withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Stormbreath Dragon")
                .withLandsOnBattlefield(1, "Mountain", 7)
                .withCardsInHand(1, "Mountain", 1)
                .withCardsInHand(2, "Mountain", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val p1Life = game.getLifeTotal(1)
            val p2Life = game.getLifeTotal(2)

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = game.findPermanent("Stormbreath Dragon")!!,
                    abilityId = cardRegistry.getCard("Stormbreath Dragon")!!.activatedAbilities[0].id,
                    paymentStrategy = PaymentStrategy.AutoPay,
                )
            ).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(2) shouldBe p2Life - 4
            game.getLifeTotal(1) shouldBe p1Life
            val dragon = game.findPermanent("Stormbreath Dragon")!!
            game.state.projectedState.getPower(dragon) shouldBe 7
        }
    }
}
