package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Smaug the Magnificent (HOB) — {2}{R}{R} Legendary Creature — Dragon 4/3.
 *
 * "Flying, haste
 *  Whenever Smaug attacks, he deals damage equal to the number of Treasures you control to any target.
 *  At the beginning of your upkeep, create a Treasure token."
 *
 * The attack trigger's amount is read at *resolution*, so it scales with the Treasures on board then
 * — including zero, where the ability still resolves and deals nothing.
 */
class SmaugTheMagnificentScenarioTest : ScenarioTestBase() {

    init {
        context("Smaug the Magnificent") {

            test("it is a 4/3 with flying and haste") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Smaug the Magnificent")
                    .build()

                val smaug = game.findPermanent("Smaug the Magnificent")!!
                game.state.projectedState.getPower(smaug) shouldBe 4
                game.state.projectedState.getToughness(smaug) shouldBe 3
                game.state.projectedState.hasKeyword(smaug, Keyword.FLYING) shouldBe true
                game.state.projectedState.hasKeyword(smaug, Keyword.HASTE) shouldBe true
            }

            test("attacking deals damage equal to the Treasures you control") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Smaug the Magnificent")
                    .withCardOnBattlefield(1, "Treasure", isToken = true)
                    .withCardOnBattlefield(1, "Treasure", isToken = true)
                    .withCardOnBattlefield(1, "Treasure", isToken = true)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Smaug the Magnificent" to 2)).error shouldBe null

                withClue("the attack trigger asks for its target") {
                    (game.getPendingDecision() is ChooseTargetsDecision) shouldBe true
                }
                game.selectTargets(listOf(game.player2Id)).error shouldBe null
                game.resolveStack()

                withClue("three Treasures → 3 damage, before combat damage is even dealt") {
                    game.getLifeTotal(2) shouldBe 17
                }
            }

            test("with no Treasures the trigger still resolves and deals nothing") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Smaug the Magnificent")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Smaug the Magnificent" to 2)).error shouldBe null

                (game.getPendingDecision() is ChooseTargetsDecision) shouldBe true
                game.selectTargets(listOf(game.player2Id)).error shouldBe null
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 20
            }

            test("the upkeep trigger creates a Treasure token") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Smaug the Magnificent")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Forest")
                    // Sit in the opponent's turn so the next upkeep reached is Player 1's.
                    .withActivePlayer(2)
                    .inPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                    .build()

                game.findAllPermanents("Treasure").size shouldBe 0

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.state.activePlayerId shouldBe game.player1Id
                game.resolveStack()

                withClue("Smaug hoards one Treasure per upkeep") {
                    game.findAllPermanents("Treasure").size shouldBe 1
                }
            }
        }
    }
}
