package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Gutwrencher Oni (CHK #113) — "At the beginning of your upkeep, discard a card if you don't
 * control an Ogre."
 *
 * The Ogre check is on resolution and counts only Ogres *you* control: an opponent's Ogre does not
 * spare you the discard.
 */
class GutwrencherOniScenarioTest : ScenarioTestBase() {

    private fun TestGame.resolveUpkeep() {
        var guard = 0
        while (guard++ < 20) {
            when (val decision = getPendingDecision()) {
                is SelectCardsDecision -> selectCards(decision.options.take(decision.minSelections))
                null -> if (state.stack.isNotEmpty()) resolveStack() else break
                else -> error("unexpected decision $decision")
            }
        }
    }

    private fun board(ogreFor: Int?) = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Gutwrencher Oni")
        .apply { if (ogreFor != null) withCardOnBattlefield(ogreFor, "Deathcurse Ogre") }
        .withCardsInHand(1, "Grizzly Bears", 3)
        .withActivePlayer(1)
        .inPhase(Phase.BEGINNING, Step.UNTAP)
        .build()

    init {
        context("Gutwrencher Oni") {
            test("without an Ogre, you discard a card at upkeep") {
                val game = board(ogreFor = null)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveUpkeep()
                game.handSize(1) shouldBe 2
                game.graveyardSize(1) shouldBe 1
            }

            test("controlling an Ogre spares the discard") {
                val game = board(ogreFor = 1)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveUpkeep()
                game.handSize(1) shouldBe 3
                game.graveyardSize(1) shouldBe 0
            }

            test("an opponent's Ogre does not count") {
                val game = board(ogreFor = 2)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveUpkeep()
                game.handSize(1) shouldBe 2
                game.graveyardSize(1) shouldBe 1
            }
        }
    }
}
