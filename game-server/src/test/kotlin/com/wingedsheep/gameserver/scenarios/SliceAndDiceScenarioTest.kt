package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

class SliceAndDiceScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        context("Slice and Dice") {
            test("deals 4 damage to each creature when cast") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Slice and Dice")
                    .withLandsOnBattlefield(1, "Mountain", 6) // {4}{R}{R}
                    .withCardOnBattlefield(1, "Glory Seeker")   // 2/2 - should die
                    .withCardOnBattlefield(2, "Towering Baloth") // 7/6 - should survive
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Slice and Dice")
                game.resolveStack()

                withClue("Glory Seeker (2/2) should die from 4 damage") {
                    game.isOnBattlefield("Glory Seeker") shouldBe false
                }

                withClue("Towering Baloth (7/6) should survive 4 damage") {
                    game.isOnBattlefield("Towering Baloth") shouldBe true
                }
            }

            test("cycling trigger deals 1 damage to each creature when accepted") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Slice and Dice")
                    .withLandsOnBattlefield(1, "Mountain", 3) // Cycling cost {2}{R}
                    .withCardInLibrary(1, "Swamp") // Card to draw
                    .withCardOnBattlefield(1, "Festering Goblin") // 1/1 - should die
                    .withCardOnBattlefield(2, "Glory Seeker")      // 2/2 - should survive
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cycle Slice and Dice
                val cycleResult = game.cycleCard(1, "Slice and Dice")
                withClue("Cycling should succeed") {
                    cycleResult.error shouldBe null
                }

                // Triggered ability goes on stack - resolve it
                game.resolveStack()

                // Cycling trigger: "You may have it deal 1 damage to each creature"
                withClue("Should have may decision for cycling trigger") {
                    game.hasPendingDecision() shouldBe true
                }
                game.answerYesNo(true)

                withClue("Festering Goblin (1/1) should die from 1 damage") {
                    game.isOnBattlefield("Festering Goblin") shouldBe false
                }

                withClue("Glory Seeker (2/2) should survive 1 damage") {
                    game.isOnBattlefield("Glory Seeker") shouldBe true
                }
            }

            test("cycling trigger does nothing when declined") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Slice and Dice")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withCardInLibrary(1, "Swamp")
                    .withCardOnBattlefield(1, "Festering Goblin")
                    .withCardOnBattlefield(2, "Glory Seeker")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.cycleCard(1, "Slice and Dice")

                // Resolve the triggered ability on the stack
                game.resolveStack()

                // Decline the cycling trigger
                game.answerYesNo(false)

                withClue("Festering Goblin should survive when trigger is declined") {
                    game.isOnBattlefield("Festering Goblin") shouldBe true
                }

                withClue("Glory Seeker should survive when trigger is declined") {
                    game.isOnBattlefield("Glory Seeker") shouldBe true
                }
            }
        }
    }
}
