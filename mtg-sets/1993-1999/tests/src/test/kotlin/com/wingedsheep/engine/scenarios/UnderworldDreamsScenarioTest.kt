package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Underworld Dreams (LEG): "Whenever an opponent draws a card, this enchantment deals 1 damage to
 * that player."
 */
class UnderworldDreamsScenarioTest : ScenarioTestBase() {
    private fun game() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Underworld Dreams")
        .withCardInHand(1, "Ancestral Recall")
        .withLandsOnBattlefield(1, "Island", 1)
        .apply { repeat(3) { withCardInLibrary(1, "Island"); withCardInLibrary(2, "Island") } }
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("deals 1 damage to an opponent for each card they draw") {
            val game = game()
            game.castSpellTargetingPlayer(1, "Ancestral Recall", 2).error shouldBe null
            game.resolveStack()

            game.handSize(2) shouldBe 3
            game.getLifeTotal(2) shouldBe 17
        }

        test("your own draws do not trigger it") {
            val game = game()
            game.castSpellTargetingPlayer(1, "Ancestral Recall", 1).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(1) shouldBe 20
        }
    }
}
