package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Gruesome Realization (BRO #103) — {1}{B}{B} Sorcery.
 *
 * "Choose one —
 * • You draw two cards and you lose 2 life.
 * • Creatures your opponents control get -1/-1 until end of turn."
 */
class GruesomeRealizationScenarioTest : ScenarioTestBase() {

    private fun game(): TestGame = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Gruesome Realization")
        .withLandsOnBattlefield(1, "Swamp", 3)
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withCardInLibrary(1, "Swamp")
        .withCardInLibrary(1, "Swamp")
        .withCardInLibrary(1, "Swamp")
        .withLifeTotal(1, 20)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Gruesome Realization") {

            test("mode one: you draw two cards and lose 2 life") {
                val game = game()
                val handBefore = game.handSize(1)

                game.castSpellWithMode(1, "Gruesome Realization", 0).error shouldBe null
                game.resolveStack()

                withClue("cast the spell (-1) and drew two (+2)") {
                    game.handSize(1) shouldBe handBefore + 1
                }
                game.getLifeTotal(1) shouldBe 18
            }

            test("mode two: only creatures your opponents control get -1/-1") {
                val game = game()

                game.castSpellWithMode(1, "Gruesome Realization", 1).error shouldBe null
                game.resolveStack()

                val (mine, theirs) = game.findPermanents("Grizzly Bears")
                    .partition { game.state.getBattlefield(game.player1Id).contains(it) }
                withClue("the opponent's Grizzly Bears shrinks to 1/1") {
                    game.state.projectedState.getPower(theirs.single()) shouldBe 1
                    game.state.projectedState.getToughness(theirs.single()) shouldBe 1
                }
                withClue("your own Grizzly Bears is untouched") {
                    game.state.projectedState.getPower(mine.single()) shouldBe 2
                    game.state.projectedState.getToughness(mine.single()) shouldBe 2
                }
                game.getLifeTotal(1) shouldBe 20
            }
        }
    }
}
