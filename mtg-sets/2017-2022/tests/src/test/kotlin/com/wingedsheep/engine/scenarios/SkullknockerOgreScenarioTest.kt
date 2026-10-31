package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Skullknocker Ogre (ELD #142) — {3}{R} Creature — Ogre 4/3.
 *
 *   Whenever this creature deals damage to an opponent, that player discards a card at random.
 *   If the player does, they draw a card.
 *
 * The draw is gated on an actual discard (`IfYouDo` over the random-discard pipeline), and both
 * halves are pointed at the damaged player, not the Ogre's controller.
 */
class SkullknockerOgreScenarioTest : ScenarioTestBase() {

    private fun attackOpponent(opponentHand: List<String>): TestGame {
        val builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Skullknocker Ogre", summoningSickness = false)
        opponentHand.forEach { builder.withCardInHand(2, it) }
        repeat(3) {
            builder.withCardInLibrary(1, "Mountain")
            builder.withCardInLibrary(2, "Island")
        }
        val game = builder
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        game.declareAttackers(mapOf("Skullknocker Ogre" to 2)).error shouldBe null
        game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
        game.declareNoBlockers().error shouldBe null
        game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
        game.resolveStack()
        return game
    }

    init {
        context("Skullknocker Ogre") {

            test("the damaged opponent discards at random and then draws") {
                val game = attackOpponent(listOf("Forest"))

                game.getLifeTotal(2) shouldBe 16
                withClue("the only card in hand is the one discarded") {
                    game.isInGraveyard(2, "Forest") shouldBe true
                }
                withClue("the opponent, not the Ogre's controller, draws the replacement card") {
                    game.handSize(2) shouldBe 1
                    game.librarySize(2) shouldBe 2
                    game.librarySize(1) shouldBe 3
                    game.handSize(1) shouldBe 0
                }
            }

            test("an opponent with an empty hand discards nothing and draws nothing") {
                val game = attackOpponent(emptyList())

                game.getLifeTotal(2) shouldBe 16
                game.graveyardSize(2) shouldBe 0
                game.handSize(2) shouldBe 0
                game.librarySize(2) shouldBe 3
            }
        }
    }
}
