package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.m20.cards.AtemsisAllSeeing
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Atemsis, All-Seeing (M20 #46) — "Whenever Atemsis deals damage to an opponent, you may reveal
 * your hand. If cards with at least six different mana values are revealed this way, that player
 * loses the game."
 *
 * Mana values in hand count once each (two 0s are one value), a land counts as 0, and an {X} card
 * counts with X = 0 (Fireball is 1). Declining to reveal never wins, even with six values in hand.
 */
class AtemsisAllSeeingScenarioTest : ScenarioTestBase() {

    init {
        cardRegistry.register(AtemsisAllSeeing)

        context("Atemsis, All-Seeing") {

            fun build(hand: List<String>) = scenario()
                .withPlayers("Sphinx", "Opponent")
                .withCardOnBattlefield(1, "Atemsis, All-Seeing", summoningSickness = false)
                .apply { hand.forEach { withCardInHand(1, it) } }
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            fun TestGame.hitOpponent() {
                passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                declareAttackers(mapOf("Atemsis, All-Seeing" to 2)).error shouldBe null
                passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                declareNoBlockers()
                passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
                passPriority()
                resolveStack()
            }

            // Mana values 0, 1, 2, 4, 5, 6.
            val sixValues = listOf("Island", "Lightning Bolt", "Grizzly Bears", "Hill Giant", "Air Elemental", "Craw Wurm")

            // Seven cards but only five values: 0 (Island, Ornithopter), 1 (Lightning Bolt, Fireball with X = 0), 2, 4, 5.
            val fiveValues = listOf(
                "Island", "Ornithopter", "Lightning Bolt", "Fireball", "Grizzly Bears", "Hill Giant", "Air Elemental",
            )

            test("revealing six different mana values makes the damaged opponent lose") {
                val game = build(sixValues)
                game.hitOpponent()

                withClue("the reveal is optional, so the trigger asks first") { game.hasPendingDecision() shouldBe true }
                game.answerYesNo(true).error shouldBe null

                game.state.gameOver shouldBe true
                game.state.winnerId shouldBe game.player1Id
            }

            test("seven cards sharing five mana values is not enough") {
                val game = build(fiveValues)
                game.hitOpponent()

                game.answerYesNo(true).error shouldBe null
                game.resolveStack()

                withClue("duplicate values count once, and Fireball's X is 0") {
                    game.state.gameOver shouldBe false
                }
                game.getLifeTotal(2) shouldBe 16
            }

            test("declining to reveal does nothing even with six mana values in hand") {
                val game = build(sixValues)
                game.hitOpponent()

                game.answerYesNo(false).error shouldBe null
                game.resolveStack()

                game.state.gameOver shouldBe false
            }
        }
    }
}
