package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario test for Academy Loremaster (DMU #40).
 *
 * {U}{U} Creature — Human Wizard 2/3
 * "At the beginning of each player's draw step, that player may draw an additional card. If they
 * do, spells they cast this turn cost {2} more to cast."
 *
 * Covers the player scoping of the tax: the draw-step player (here the opponent) makes the choice,
 * and accepting taxes *their* spells for the turn — the Loremaster's controller is untouched.
 */
class AcademyLoremasterScenarioTest : ScenarioTestBase() {

    /** Player 1 controls Loremaster; the game is driven to player 2's draw-step trigger. */
    private fun toOpponentDrawTrigger(opponentForests: Int): TestGame {
        var builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Academy Loremaster")
            .withCardInHand(2, "Grizzly Bears")
            .withLandsOnBattlefield(2, "Forest", opponentForests)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        repeat(10) { builder = builder.withCardInLibrary(1, "Island") }
        repeat(10) { builder = builder.withCardInLibrary(2, "Forest") }
        val game = builder.build()

        game.passUntilPhase(Phase.BEGINNING, Step.DRAW)
        game.resolveStack()
        return game
    }

    init {
        context("Academy Loremaster") {

            test("the draw-step player decides, and accepting taxes their spells by {2} this turn") {
                val game = toOpponentDrawTrigger(opponentForests = 3)
                val handBefore = game.handSize(2)

                val decision = game.getPendingDecision()
                withClue("the opponent — the player whose draw step it is — gets the choice") {
                    decision shouldNotBe null
                    decision!!.playerId shouldBe game.player2Id
                }
                game.answerYesNo(true)
                game.resolveStack()
                withClue("the opponent drew the additional card") {
                    game.handSize(2) shouldBe handBefore + 1
                }
                withClue("the tax is on the opponent, not on Loremaster's controller") {
                    game.state.spellCostModifiers.single().controllerId shouldBe game.player2Id
                }

                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                withClue("Grizzly Bears now costs {3}{G}; three Forests can't pay it") {
                    game.castSpell(2, "Grizzly Bears").error shouldNotBe null
                }
            }

            test("declining draws nothing extra and leaves spells untaxed") {
                val game = toOpponentDrawTrigger(opponentForests = 2)
                val handBefore = game.handSize(2)

                game.answerYesNo(false)
                game.resolveStack()
                game.handSize(2) shouldBe handBefore
                game.state.spellCostModifiers shouldBe emptyList()

                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                game.castSpell(2, "Grizzly Bears").error shouldBe null
            }

            test("the taxed spell is castable once the extra {2} is available") {
                val game = toOpponentDrawTrigger(opponentForests = 4)
                game.answerYesNo(true)
                game.resolveStack()

                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                game.castSpell(2, "Grizzly Bears").error shouldBe null
            }
        }
    }
}
