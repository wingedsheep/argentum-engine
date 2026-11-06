package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Thresher Lizard (AKH #150) — {2}{R} 3/2 Creature — Lizard.
 *
 *   This creature gets +1/+2 as long as you have one or fewer cards in hand.
 *
 * Proves the bonus tracks its controller's hand size continuously (not the opponent's), switching
 * on as soon as the hand drops to one card.
 */
class ThresherLizardScenarioTest : ScenarioTestBase() {

    init {
        context("Thresher Lizard") {

            test("is a 3/2 with two cards in hand and a 4/4 once the hand drops to one") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Thresher Lizard")
                    .withCardsInHand(1, "Mountain", 2)
                    .withActivePlayer(1)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val lizard = game.findPermanent("Thresher Lizard")!!

                withClue("two cards in hand: no bonus") {
                    game.state.projectedState.getPower(lizard) shouldBe 3
                    game.state.projectedState.getToughness(lizard) shouldBe 2
                }

                val land = game.findCardsInHand(1, "Mountain").first()
                game.execute(PlayLand(game.player1Id, land)).error shouldBe null
                game.handSize(1) shouldBe 1

                withClue("one card in hand: +1/+2") {
                    game.state.projectedState.getPower(lizard) shouldBe 4
                    game.state.projectedState.getToughness(lizard) shouldBe 4
                }
            }

            test("an empty hand counts, and the opponent's empty hand does not") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Thresher Lizard")
                    .withCardOnBattlefield(2, "Thresher Lizard")
                    .withCardsInHand(2, "Mountain", 3)
                    .withActivePlayer(1)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val (mine, theirs) = game.findPermanents("Thresher Lizard")
                    .partition { game.state.projectedState.getController(it) == game.player1Id }
                    .let { (p1, p2) -> p1.single() to p2.single() }

                game.state.projectedState.getPower(mine) shouldBe 4
                game.state.projectedState.getToughness(mine) shouldBe 4
                game.state.projectedState.getPower(theirs) shouldBe 3
                game.state.projectedState.getToughness(theirs) shouldBe 2
            }
        }
    }
}
