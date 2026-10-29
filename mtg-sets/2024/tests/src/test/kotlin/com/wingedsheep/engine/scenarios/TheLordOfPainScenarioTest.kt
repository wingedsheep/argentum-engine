package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

/**
 * The Lord of Pain (DSC 3).
 *
 * "Your opponents can't gain life.
 *  Whenever a player casts their first spell each turn, choose another target player. The Lord of
 *  Pain deals damage equal to that spell's mana value to the chosen player."
 */
class TheLordOfPainScenarioTest : ScenarioTestBase() {
    init {
        test("an opponent's first spell: the caster can't be chosen, so the Lord's controller takes the damage") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "The Lord of Pain")
                .withCardInHand(2, "Grizzly Bears")
                .withCardInHand(2, "Grizzly Bears")
                .withLandsOnBattlefield(2, "Forest", 4)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(2, "Grizzly Bears")
            (game.getPendingDecision() as? ChooseTargetsDecision)?.let { decision ->
                withClue("only the Lord's controller is legal (ruling: two-player game)") {
                    decision.legalTargets.values.flatten() shouldContainExactly listOf(game.player1Id)
                }
                game.selectTargets(listOf(game.player1Id))
            }
            game.resolveStack()
            withClue("2 damage (Grizzly Bears' mana value) to P1") {
                game.getLifeTotal(1) shouldBe 18
                game.getLifeTotal(2) shouldBe 20
            }

            game.castSpell(2, "Grizzly Bears")
            game.resolveStack()
            withClue("second spell that turn: no trigger") {
                game.getLifeTotal(1) shouldBe 18
            }
        }

        test("your own first spell deals its mana value to the opponent") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "The Lord of Pain")
                .withCardInHand(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Grizzly Bears")
            (game.getPendingDecision() as? ChooseTargetsDecision)?.let { game.selectTargets(listOf(game.player2Id)) }
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 20
            game.getLifeTotal(2) shouldBe 18
        }

        test("opponents can't gain life") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "The Lord of Pain")
                .withCardInHand(2, "Angel's Mercy")
                .withLandsOnBattlefield(2, "Plains", 4)
                .withLifeTotal(2, 10)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(2, "Angel's Mercy")
            game.getPendingDecision()?.let { game.selectTargets(listOf(game.player1Id)) }
            game.resolveStack()
            withClue("Angel's Mercy's 7 life is prevented; the Lord dealt 4 (its mana value) to P1") {
                game.getLifeTotal(2) shouldBe 10
                game.getLifeTotal(1) shouldBe 16
            }
        }
    }
}
