package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Georgiou, Terran Emperor — "You don't lose the game for having 0 or less life. At the beginning
 * of your end step, you lose 3 life and draw a card. Then if your life total is 0 or less, create
 * ten 1/1 red Officer creature tokens."
 */
class GeorgiouTerranEmperorScenarioTest : ScenarioTestBase() {

    init {
        fun game(life: Int): TestGame = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Georgiou, Terran Emperor")
            .withLifeTotal(1, life)
            .withCardInLibrary(1, "Swamp")
            .withCardInLibrary(1, "Swamp")
            .withCardInLibrary(2, "Swamp")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        context("Georgiou, Terran Emperor") {
            test("dropping to 0 or less life creates ten Officers and its controller stays in the game") {
                val game = game(life = 2)
                val handBefore = game.handSize(1)

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                game.getLifeTotal(1) shouldBe -1
                game.handSize(1) shouldBe handBefore + 1
                withClue("ten 1/1 red Officer tokens") { game.findPermanents("Officer Token").size shouldBe 10 }
                withClue("0 or less life doesn't lose the game") { game.state.gameOver shouldBe false }
            }

            test("staying above 0 life creates no Officers") {
                val game = game(life = 10)

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                game.getLifeTotal(1) shouldBe 7
                game.findPermanents("Officer Token").size shouldBe 0
            }
        }
    }
}
