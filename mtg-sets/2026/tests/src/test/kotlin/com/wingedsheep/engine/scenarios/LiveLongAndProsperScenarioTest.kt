package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Live Long and Prosper: doubles your life total, then draws as many cards as are in your hand
 * (counted on resolution, with the spell itself on the stack), and lifts your maximum hand size
 * until your next turn.
 */
class LiveLongAndProsperScenarioTest : ScenarioTestBase() {

    init {
        context("Live Long and Prosper") {
            test("doubles life, draws one card per card in hand, and keeps the big hand through cleanup") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withLifeTotal(1, 13)
                    .withCardInHand(1, "Live Long and Prosper")
                    .withCardsInHand(1, "Grizzly Bears", 5)
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withLandsOnBattlefield(1, "Island", 3)
                    .apply { repeat(10) { withCardInLibrary(1, "Forest") } }
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Live Long and Prosper").error shouldBe null
                game.resolveStack()

                withClue("life total doubled") { game.getLifeTotal(1) shouldBe 26 }
                withClue("five cards in hand draw five") { game.handSize(1) shouldBe 10 }

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                withClue("no discard at this turn's cleanup") { game.handSize(1) shouldBe 10 }
            }
        }
    }
}
