package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario test for Wildfire Eternal (HOU #109).
 *
 * Focus: the attacks-and-isn't-blocked trigger casts a sorcery from hand for free mid-combat (its
 * timing restriction ignored), a creature card can't be chosen and the cast may be declined, and
 * when blocked the free cast never offers while afflict 4 drains the defender.
 */
class WildfireEternalScenarioTest : ScenarioTestBase() {

    init {
        context("Wildfire Eternal") {

            fun board(blocker: Boolean = false): TestGame {
                val builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Wildfire Eternal", summoningSickness = false)
                    .withCardInHand(1, "Mind Rot")
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInHand(2, "Grizzly Bears")
                    .withCardInHand(2, "Forest")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                if (blocker) builder.withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                return builder.build()
            }

            fun attack(game: TestGame, blocks: Map<String, List<String>> = emptyMap()) {
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Wildfire Eternal" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(blocks).error shouldBe null
                game.resolveStack()
            }

            test("unblocked: casts a sorcery from hand mid-combat without paying its mana cost") {
                val game = board()
                attack(game)

                withClue("the unblocked trigger should offer the hand") {
                    (game.state.pendingDecision is SelectCardsDecision) shouldBe true
                }
                game.selectCards(listOf(game.findCardsInHand(1, "Mind Rot").single())).error shouldBe null
                game.selectTargets(listOf(game.player2Id)).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(1, "Mind Rot") shouldBe true
                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                game.isInGraveyard(2, "Forest") shouldBe true
            }

            test("unblocked: a creature card can't be chosen, and the cast may be declined") {
                val game = board()
                attack(game)

                game.selectCards(listOf(game.findCardsInHand(1, "Grizzly Bears").single())).outcome shouldNotBe Outcome.Done
                game.selectCards(emptyList()).error shouldBe null
                game.resolveStack()

                game.isInHand(1, "Mind Rot") shouldBe true
                game.isInHand(1, "Grizzly Bears") shouldBe true
            }

            test("blocked: afflict 4 drains the defender and no free cast is offered") {
                val game = board(blocker = true)
                attack(game, mapOf("Grizzly Bears" to listOf("Wildfire Eternal")))

                game.getLifeTotal(2) shouldBe 16
                (game.state.pendingDecision is SelectCardsDecision) shouldBe false
                game.isInHand(1, "Mind Rot") shouldBe true
            }
        }
    }
}
