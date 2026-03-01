package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Bellowing Saddlebrute.
 *
 * Card reference:
 * - Bellowing Saddlebrute ({3}{B}): Creature — Orc Warrior, 4/5
 *   Raid — When Bellowing Saddlebrute enters the battlefield, you lose 4 life
 *   unless you attacked this turn.
 */
class BellowingSaddlebruteScenarioTest : ScenarioTestBase() {

    init {
        context("Bellowing Saddlebrute Raid ability") {

            test("lose 4 life when entering without having attacked this turn") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Bellowing Saddlebrute")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val startingLife = game.getLifeTotal(1)

                game.castSpell(1, "Bellowing Saddlebrute")
                game.resolveStack()

                withClue("Should lose 4 life from Raid penalty (no attack this turn)") {
                    game.getLifeTotal(1) shouldBe startingLife - 4
                }

                withClue("Bellowing Saddlebrute should be on battlefield") {
                    game.isOnBattlefield("Bellowing Saddlebrute") shouldBe true
                }
            }

            test("no life loss when entering after having attacked this turn") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Bellowing Saddlebrute")
                    .withCardOnBattlefield(1, "Leaping Master")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val startingLife = game.getLifeTotal(1)

                // Go to combat and attack with Leaping Master
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Leaping Master" to 2))
                // Pass through combat
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                val lifeAfterCombat = game.getLifeTotal(1)

                // Cast Bellowing Saddlebrute in postcombat main
                game.castSpell(1, "Bellowing Saddlebrute")
                game.resolveStack()

                withClue("Should NOT lose 4 life from Raid (attacked this turn)") {
                    game.getLifeTotal(1) shouldBe lifeAfterCombat
                }

                withClue("Bellowing Saddlebrute should be on battlefield") {
                    game.isOnBattlefield("Bellowing Saddlebrute") shouldBe true
                }
            }
        }
    }
}
