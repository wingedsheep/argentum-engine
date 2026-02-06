package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

class LightningRiftScenarioTest : ScenarioTestBase() {

    init {
        context("Lightning Rift") {
            test("deals 2 damage to target creature when a card is cycled and player chooses yes") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Lightning Rift")
                    .withCardInHand(1, "Disciple of Grace") // Cycling {2}
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInLibrary(1, "Mountain") // Card to draw
                    .withCardOnBattlefield(2, "Glory Seeker") // 2/2 target
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cycle - Lightning Rift triggers
                game.cycleCard(1, "Disciple of Grace")

                withClue("Lightning Rift should trigger - target selection pending") {
                    game.hasPendingDecision() shouldBe true
                }

                // Select Glory Seeker as target
                val targetId = game.findPermanent("Glory Seeker")!!
                game.selectTargets(listOf(targetId))

                // Resolve the triggered ability on the stack
                game.resolveStack()

                // MayEffect asks yes/no
                withClue("Should have may decision") {
                    game.hasPendingDecision() shouldBe true
                }
                game.answerYesNo(true)

                // Glory Seeker is 2/2 and takes 2 damage - should die
                withClue("Glory Seeker should be destroyed by 2 damage") {
                    game.isOnBattlefield("Glory Seeker") shouldBe false
                }
            }

            test("does not deal damage when player declines may ability") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Lightning Rift")
                    .withCardInHand(1, "Disciple of Grace")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInLibrary(1, "Mountain")
                    .withCardOnBattlefield(2, "Glory Seeker")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.cycleCard(1, "Disciple of Grace")

                val targetId = game.findPermanent("Glory Seeker")!!
                game.selectTargets(listOf(targetId))
                game.resolveStack()

                // Decline
                game.answerYesNo(false)

                // Glory Seeker should survive
                withClue("Glory Seeker should still be on battlefield") {
                    game.isOnBattlefield("Glory Seeker") shouldBe true
                }
            }

            test("can deal 2 damage to a player") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Lightning Rift")
                    .withCardInHand(1, "Disciple of Grace")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInLibrary(1, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val opponentLife = game.getLifeTotal(2)

                game.cycleCard(1, "Disciple of Grace")

                // Target opponent
                game.selectTargets(listOf(game.player2Id))
                game.resolveStack()
                game.answerYesNo(true)

                withClue("Opponent should have lost 2 life") {
                    game.getLifeTotal(2) shouldBe opponentLife - 2
                }
            }

            test("triggers from opponent's cycling too") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Lightning Rift")
                    .withCardInHand(2, "Disciple of Malice") // Opponent's cycling card
                    .withLandsOnBattlefield(2, "Swamp", 2)
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(2) // Opponent's turn
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Opponent cycles - Lightning Rift should still trigger for player 1
                game.cycleCard(2, "Disciple of Malice")

                withClue("Lightning Rift should trigger even from opponent's cycling") {
                    game.hasPendingDecision() shouldBe true
                }
            }
        }
    }
}
