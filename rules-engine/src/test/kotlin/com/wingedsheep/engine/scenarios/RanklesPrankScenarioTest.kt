package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Rankle's Prank — {2}{B}{B} Sorcery (WOE).
 *
 * Choose one or more —
 * • Each player discards two cards.
 * • Each player loses 4 life.
 * • Each player sacrifices two creatures of their choice.
 *
 * Covers each mode in isolation and all three together, plus the symmetry (the caster is hit
 * too). No mode targets, so the spell never needs a target and never fizzles.
 */
class RanklesPrankScenarioTest : ScenarioTestBase() {

    private fun game() = scenario()
        .withPlayers()
        .withCardInHand(1, "Rankle's Prank")
        .withCardsInHand(1, "Grizzly Bears", 3)
        .withCardsInHand(2, "Savannah Lions", 3)
        .withLandsOnBattlefield(1, "Swamp", 4)
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardOnBattlefield(1, "Centaur Courser")
        .withCardOnBattlefield(2, "Savannah Lions")
        .withCardOnBattlefield(2, "Force of Nature")
        .withCardInLibrary(1, "Swamp")
        .withCardInLibrary(2, "Forest")
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .withActivePlayer(1)
        .withPriorityPlayer(1)
        .build()

    /** Cast the Prank with an explicit set of modes; no mode has targets. */
    private fun TestGame.castPrank(vararg modes: Int) =
        execute(
            CastSpell(
                playerId = player1Id,
                cardId = findCardsInHand(1, "Rankle's Prank").first(),
                targets = emptyList(),
                chosenModes = modes.toList()
            )
        )

    /**
     * Drain the resolution, answering each player's "choose N cards" prompt by taking the first
     * legal selection. Discard and sacrifice both surface as [SelectCardsDecision].
     */
    private fun TestGame.resolveWithFirstChoices() {
        var guard = 0
        while ((state.stack.isNotEmpty() || hasPendingDecision()) && guard++ < 40) {
            val decision = getPendingDecision()
            if (decision is SelectCardsDecision) {
                selectCards(decision.options.take(decision.minSelections))
            } else if (decision != null) {
                break
            } else {
                passPriority()
            }
        }
    }

    init {
        test("life-loss mode hits both players for 4") {
            val g = game()
            val myLife = g.getLifeTotal(1)
            val theirLife = g.getLifeTotal(2)

            g.castPrank(1).error shouldBe null
            g.resolveWithFirstChoices()

            g.getLifeTotal(1) shouldBe myLife - 4
            g.getLifeTotal(2) shouldBe theirLife - 4
        }

        test("discard mode makes each player pitch two cards") {
            val g = game()
            // Player 1's hand loses the Prank itself as it's cast, then two more to the mode.
            val myHandAfterCast = g.handSize(1) - 1
            val theirHand = g.handSize(2)

            g.castPrank(0).error shouldBe null
            g.resolveWithFirstChoices()

            g.handSize(1) shouldBe myHandAfterCast - 2
            g.handSize(2) shouldBe theirHand - 2
        }

        test("sacrifice mode makes each player sacrifice two creatures") {
            val g = game()

            g.castPrank(2).error shouldBe null
            g.resolveWithFirstChoices()

            // Each side had exactly two creatures, so both boards are swept.
            g.isOnBattlefield("Grizzly Bears") shouldBe false
            g.isOnBattlefield("Centaur Courser") shouldBe false
            g.isOnBattlefield("Savannah Lions") shouldBe false
            g.isOnBattlefield("Force of Nature") shouldBe false
        }

        test("all three modes resolve together, in printed order") {
            val g = game()
            val myLife = g.getLifeTotal(1)
            val theirLife = g.getLifeTotal(2)
            val myHandAfterCast = g.handSize(1) - 1
            val theirHand = g.handSize(2)

            g.castPrank(0, 1, 2).error shouldBe null
            g.resolveWithFirstChoices()

            g.handSize(1) shouldBe myHandAfterCast - 2
            g.handSize(2) shouldBe theirHand - 2
            g.getLifeTotal(1) shouldBe myLife - 4
            g.getLifeTotal(2) shouldBe theirLife - 4
            g.isOnBattlefield("Centaur Courser") shouldBe false
            g.isOnBattlefield("Force of Nature") shouldBe false
        }
    }
}
