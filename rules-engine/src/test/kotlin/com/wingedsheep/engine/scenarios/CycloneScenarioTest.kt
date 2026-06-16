package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Cyclone (ARN #45).
 *
 * "{2}{G}{G} Enchantment. At the beginning of your upkeep, put a wind counter on this enchantment,
 *  then sacrifice this enchantment unless you pay {G} for each wind counter on it. If you pay, this
 *  enchantment deals damage equal to the number of wind counters on it to each creature and each
 *  player."
 *
 * Exercises the colored dynamic mana cost ({G} per wind counter via
 * `Effects.PayDynamicMana(color = GREEN)`) gated by `OptionalCostEffect`: pay → damage to each
 * creature and player; decline / can't pay → sacrifice.
 */
class CycloneScenarioTest : ScenarioTestBase() {

    init {
        context("Cyclone upkeep") {

            test("paying {G} per wind counter deals that much damage to each creature and player") {
                var builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Cyclone")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardOnBattlefield(1, "Mons's Goblin Raiders", summoningSickness = false) // 0/1
                    .withCardOnBattlefield(2, "Mons's Goblin Raiders", summoningSickness = false) // 0/1
                    .withLifeTotal(1, 20)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(5) { builder = builder.withCardInLibrary(1, "Forest") }
                repeat(5) { builder = builder.withCardInLibrary(2, "Forest") }
                val game = builder.build()

                // Advance to Cyclone's controller's (player 1's) upkeep.
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()

                // First wind counter added → cost is {G}; pay it.
                game.answerYesNo(true)
                game.resolveStack()

                withClue("With 1 wind counter, Cyclone deals 1 to each player") {
                    game.getLifeTotal(1) shouldBe 19
                    game.getLifeTotal(2) shouldBe 19
                }
                withClue("1 damage kills both 0/1 creatures") {
                    game.isOnBattlefield("Mons's Goblin Raiders") shouldBe false
                }
                withClue("The cost was paid, so Cyclone is not sacrificed") {
                    game.isOnBattlefield("Cyclone") shouldBe true
                }
            }

            test("declining to pay sacrifices Cyclone and deals no damage") {
                var builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Cyclone")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardOnBattlefield(2, "Mons's Goblin Raiders", summoningSickness = false)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(5) { builder = builder.withCardInLibrary(1, "Forest") }
                repeat(5) { builder = builder.withCardInLibrary(2, "Forest") }
                val game = builder.build()

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()

                // Decline the {G} payment.
                game.answerYesNo(false)
                game.resolveStack()

                withClue("Declining the cost sacrifices Cyclone") {
                    game.isOnBattlefield("Cyclone") shouldBe false
                }
                withClue("No damage is dealt when the cost isn't paid") {
                    game.getLifeTotal(2) shouldBe 20
                    game.isOnBattlefield("Mons's Goblin Raiders") shouldBe true
                }
            }

            test("with no green mana available, Cyclone is sacrificed") {
                var builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Cyclone")
                    // No lands — the {G} cost is unaffordable.
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(5) { builder = builder.withCardInLibrary(1, "Forest") }
                repeat(5) { builder = builder.withCardInLibrary(2, "Forest") }
                val game = builder.build()

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()

                withClue("Unable to pay {G}, Cyclone sacrifices itself") {
                    game.isOnBattlefield("Cyclone") shouldBe false
                }
            }
        }
    }
}
