package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Fasting (DRK #7).
 *
 * {W} Enchantment
 * "At the beginning of your upkeep, put a hunger counter on this enchantment. Then destroy this
 *  enchantment if it has five or more hunger counters on it.
 *  If you would begin your draw step, you may skip that step instead. If you do, you gain 2 life.
 *  When you draw a card, destroy this enchantment."
 *
 * Fasting is a race between its own clock and the first card you draw, so the tests cover both
 * ways it ends as well as the payoff for keeping it.
 */
class FastingScenarioTest : ScenarioTestBase() {

    init {
        /** Round the table back to the Faster's own upkeep. */
        fun TestGame.toMyNextUpkeep() {
            passUntilPhase(Phase.ENDING, Step.END)
            passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            state.activePlayerId shouldBe player2Id
            passUntilPhase(Phase.ENDING, Step.END)
            passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            state.activePlayerId shouldBe player1Id
        }

        context("Fasting") {

            test("skipping the draw step gains 2 life and keeps the enchantment alive") {
                val game = scenario()
                    .withPlayers("Faster", "Opponent")
                    .withCardOnBattlefield(1, "Fasting")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(2, "Grizzly Bears")
                    .withCardInLibrary(2, "Grizzly Bears")
                    .withLifeTotal(1, 20)
                    .withActivePlayer(2)
                    .inPhase(Phase.ENDING, Step.END)
                    .build()

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.state.activePlayerId shouldBe game.player1Id
                game.resolveStack()

                // The optional skip-draw offer.
                val offer = game.state.pendingDecision
                offer.shouldNotBeNull()
                offer.shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(true)
                game.resolveStack()

                withClue("accepting gains 2 life") {
                    game.getLifeTotal(1) shouldBe 22
                }

                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                withClue("the draw was skipped, so nothing drew and Fasting survives") {
                    game.handSize(1) shouldBe 0
                    game.findPermanent("Fasting").shouldNotBeNull()
                }
            }

            test("five hunger counters destroy it even if you never draw") {
                // Accept the fast every turn, so the only thing that can end Fasting is its own
                // clock. The fifth upkeep is the last one.
                val builder = scenario()
                    .withPlayers("Faster", "Opponent")
                    .withCardOnBattlefield(1, "Fasting")
                    .withLifeTotal(1, 20)
                    .withActivePlayer(2)
                    .inPhase(Phase.ENDING, Step.END)
                repeat(12) {
                    builder.withCardInLibrary(1, "Grizzly Bears")
                    builder.withCardInLibrary(2, "Grizzly Bears")
                }
                val game = builder.build()

                repeat(5) { upkeep ->
                    if (upkeep == 0) {
                        game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                        game.state.activePlayerId shouldBe game.player1Id
                    } else {
                        game.toMyNextUpkeep()
                    }
                    game.resolveStack()

                    val offer = game.state.pendingDecision
                    offer.shouldNotBeNull()
                    offer.shouldBeInstanceOf<YesNoDecision>()
                    game.answerYesNo(true)
                    game.resolveStack()

                    if (upkeep < 4) {
                        withClue("upkeep ${upkeep + 1}: only ${upkeep + 1} hunger counters, so it survives") {
                            game.findPermanent("Fasting").shouldNotBeNull()
                        }
                    }
                }

                withClue("the fifth hunger counter destroys it") {
                    game.findPermanent("Fasting").shouldBeNull()
                }
                withClue("five fasts, ten life") {
                    game.getLifeTotal(1) shouldBe 30
                }
            }

            test("declining lets the draw happen, and drawing destroys the enchantment") {
                val game = scenario()
                    .withPlayers("Faster", "Opponent")
                    .withCardOnBattlefield(1, "Fasting")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(2, "Grizzly Bears")
                    .withCardInLibrary(2, "Grizzly Bears")
                    .withLifeTotal(1, 20)
                    .withActivePlayer(2)
                    .inPhase(Phase.ENDING, Step.END)
                    .build()

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.state.activePlayerId shouldBe game.player1Id
                game.resolveStack()
                game.state.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(false)
                game.resolveStack()

                withClue("declining gains nothing") {
                    game.getLifeTotal(1) shouldBe 20
                }

                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                game.resolveStack()
                withClue("the draw happened, and drawing a card destroys Fasting") {
                    game.handSize(1) shouldBe 1
                    game.findPermanent("Fasting").shouldBeNull()
                }
            }
        }
    }
}
