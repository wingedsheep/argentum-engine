package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Firewing Phoenix (M13 #131) — {3}{R} 4/2 Creature — Phoenix.
 *   Flying
 *   {1}{R}{R}{R}: Return this card from your graveyard to your hand.
 */
class FirewingPhoenixScenarioTest : ScenarioTestBase() {
    init {
        test("{1}{R}{R}{R} returns it from the graveyard to your hand") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInGraveyard(1, "Firewing Phoenix")
                .withLandsOnBattlefield(1, "Mountain", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val phoenix = game.findCardsInGraveyard(1, "Firewing Phoenix").single()
            val returnId = cardRegistry.getCard("Firewing Phoenix")!!.script.activatedAbilities.single().id

            val result = game.execute(ActivateAbility(game.player1Id, phoenix, returnId))
            withClue("activation from the graveyard should succeed: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            game.isInHand(1, "Firewing Phoenix") shouldBe true
            game.isInGraveyard(1, "Firewing Phoenix") shouldBe false
        }
    }
}
