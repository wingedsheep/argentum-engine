package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Vindictive Lich (C17 #21) — {3}{B} Creature — Zombie Wizard 4/1.
 *
 *   When this creature dies, choose one or more. Each mode must target a different player.
 *   • Target opponent sacrifices a creature of their choice.
 *   • Target opponent discards two cards.
 *   • Target opponent loses 5 life.
 *
 * "Choose one or more" with a distinct-player rule means a game with N opponents allows at most N modes
 * (ruling). In a two-player game that is exactly one: the question is asked once and never offers a second
 * mode, because the only opponent is already taken.
 */
class VindictiveLichScenarioTest : ScenarioTestBase() {

    private fun board(opponentCreatures: Int = 0, opponentHand: Int = 0) = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Vindictive Lich")
        .apply { repeat(opponentCreatures) { withCardOnBattlefield(2, "Grizzly Bears") } }
        .apply { repeat(opponentHand) { withCardInHand(2, "Hill Giant") } }
        .withCardInHand(1, "Lightning Bolt")
        .withLandsOnBattlefield(1, "Mountain", 1)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    /** Bolt your own Lich (toughness 1) so its dies trigger fires through the real death path. */
    private fun TestGame.killLich() {
        val lich = findPermanent("Vindictive Lich") ?: error("Vindictive Lich not on the battlefield")
        castSpell(1, "Lightning Bolt", lich).error shouldBe null
        resolveStack()
    }

    private fun TestGame.modeQuestion(): ChooseOptionDecision =
        getPendingDecision() as? ChooseOptionDecision
            ?: error("expected the Lich's mode question; got ${getPendingDecision()}")

    private fun TestGame.pickMode(fragment: String) {
        val decision = modeQuestion()
        val index = decision.options.indexOfFirst { it.contains(fragment) }
        check(index >= 0) { "'$fragment' not offered; options=${decision.options}" }
        submitDecision(OptionChosenResponse(decision.id, index)).error shouldBe null
    }

    /** Aim the chosen mode at the opponent if the engine asks; with one legal target it may not. */
    private fun TestGame.aimAtOpponentIfAsked() {
        if (getPendingDecision() is ChooseTargetsDecision) {
            selectTargets(listOf(player2Id)).error shouldBe null
        }
    }

    init {
        context("Vindictive Lich") {
            test("offers all three modes, and with one opponent never offers a second") {
                val game = board()
                game.killLich()

                game.modeQuestion().options.size shouldBe 3
                game.pickMode("loses 5 life")
                game.aimAtOpponentIfAsked()

                withClue("the only opponent is already targeted, so no further mode can be chosen") {
                    (game.getPendingDecision() is ChooseOptionDecision) shouldBe false
                }
            }

            test("loses 5 life") {
                val game = board()
                game.killLich()

                game.pickMode("loses 5 life")
                game.aimAtOpponentIfAsked()
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 15
                game.getLifeTotal(1) shouldBe 20
            }

            test("discards two cards") {
                val game = board(opponentHand = 3)
                game.killLich()

                game.pickMode("discards two cards")
                game.aimAtOpponentIfAsked()
                game.resolveStack()

                val pick = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                game.selectCards(pick.options.take(2)).error shouldBe null
                game.resolveStack()

                game.handSize(2) shouldBe 1
                game.graveyardSize(2) shouldBe 2
            }

            test("the opponent sacrifices a creature of their choice") {
                val game = board(opponentCreatures = 2)
                game.killLich()

                game.pickMode("sacrifices a creature")
                game.aimAtOpponentIfAsked()
                game.resolveStack()

                val pick = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                withClue("the choice is the opponent's, not ours") { pick.playerId shouldBe game.player2Id }
                game.selectCards(pick.options.take(1)).error shouldBe null
                game.resolveStack()

                game.findAllPermanents("Grizzly Bears").size shouldBe 1
                game.graveyardSize(2) shouldBe 1
            }
        }
    }
}
