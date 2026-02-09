package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

class LightningRiftScenarioTest : ScenarioTestBase() {

    init {
        context("Lightning Rift") {
            test("deals 2 damage to target creature when a card is cycled and player pays {1}") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Lightning Rift")
                    .withCardInHand(1, "Disciple of Grace") // Cycling {2}
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withLandsOnBattlefield(1, "Mountain", 1) // Extra land for Lightning Rift's {1} cost
                    .withCardInLibrary(1, "Mountain") // Card to draw
                    .withCardOnBattlefield(2, "Glory Seeker") // 2/2 target
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cycle - Lightning Rift triggers, asks "Pay {1}?" first
                game.cycleCard(1, "Disciple of Grace")

                // Step 1: Pay mana question
                withClue("Lightning Rift should ask to pay {1}") {
                    game.hasPendingDecision() shouldBe true
                }
                game.answerYesNo(true)

                // Step 2: Mana source selection (auto-pay)
                withClue("Should show mana source selection") {
                    game.hasPendingDecision() shouldBe true
                }
                game.submitManaSourcesAutoPay()

                // Step 3: Target selection
                val targetId = game.findPermanent("Glory Seeker")!!
                game.selectTargets(listOf(targetId))

                // Resolve the triggered ability
                game.resolveStack()

                // Glory Seeker is 2/2 and takes 2 damage - should die
                withClue("Glory Seeker should be destroyed by 2 damage") {
                    game.isOnBattlefield("Glory Seeker") shouldBe false
                }
            }

            test("does not deal damage when player declines to pay") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Lightning Rift")
                    .withCardInHand(1, "Disciple of Grace")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInLibrary(1, "Mountain")
                    .withCardOnBattlefield(2, "Glory Seeker")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.cycleCard(1, "Disciple of Grace")

                // Decline to pay {1} - no target selection needed
                game.answerYesNo(false)

                // Glory Seeker should survive
                withClue("Glory Seeker should still be on battlefield") {
                    game.isOnBattlefield("Glory Seeker") shouldBe true
                }
            }

            test("does not ask to pay when player has no mana available") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Lightning Rift")
                    .withCardInHand(1, "Disciple of Grace")
                    .withLandsOnBattlefield(1, "Plains", 2) // Just enough for cycling, none left for {1}
                    .withCardInLibrary(1, "Mountain")
                    .withCardOnBattlefield(2, "Glory Seeker")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.cycleCard(1, "Disciple of Grace")

                // No pending decision - player can't pay, so trigger is skipped silently
                withClue("No decision should be pending when player can't pay") {
                    game.hasPendingDecision() shouldBe false
                }

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
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInLibrary(1, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val opponentLife = game.getLifeTotal(2)

                game.cycleCard(1, "Disciple of Grace")

                // Pay, auto-pay mana, then target opponent
                game.answerYesNo(true)
                game.submitManaSourcesAutoPay()
                game.selectTargets(listOf(game.player2Id))
                game.resolveStack()

                withClue("Opponent should have lost 2 life") {
                    game.getLifeTotal(2) shouldBe opponentLife - 2
                }
            }

            test("triggers from opponent's cycling too") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Lightning Rift")
                    .withLandsOnBattlefield(1, "Mountain", 1) // Mana for {1}
                    .withCardInHand(2, "Disciple of Malice") // Opponent's cycling card
                    .withLandsOnBattlefield(2, "Swamp", 2)
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(2) // Opponent's turn
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Opponent cycles - Lightning Rift should still trigger for player 1
                game.cycleCard(2, "Disciple of Malice")

                // Now the first decision is the YesNo "Pay {1}?" (not target selection)
                withClue("Lightning Rift should trigger even from opponent's cycling") {
                    game.hasPendingDecision() shouldBe true
                }
            }

            test("manual mana source selection works") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Lightning Rift")
                    .withCardInHand(1, "Disciple of Grace") // Cycling {2}
                    .withLandsOnBattlefield(1, "Plains", 3) // 3 Plains: 2 for cycling, 1 left over
                    .withLandsOnBattlefield(1, "Mountain", 1) // Mountain for Lightning Rift's {1}
                    .withCardInLibrary(1, "Mountain")
                    .withCardOnBattlefield(2, "Glory Seeker")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Record the Mountain ID before cycling
                val mountainId = game.findPermanent("Mountain")!!

                game.cycleCard(1, "Disciple of Grace")

                // Step 1: Pay mana question
                game.answerYesNo(true)

                // Step 2: Manually select the Mountain for {1} payment
                val manaDecision = game.getPendingDecision()
                withClue("Should have SelectManaSourcesDecision, got: ${manaDecision?.let { it::class.simpleName }}") {
                    (manaDecision is com.wingedsheep.engine.core.SelectManaSourcesDecision) shouldBe true
                }

                if (manaDecision is com.wingedsheep.engine.core.SelectManaSourcesDecision) {
                    val sourceIds = manaDecision.availableSources.map { it.entityId }
                    withClue("Mountain ($mountainId) should be in available sources: $sourceIds") {
                        sourceIds.contains(mountainId) shouldBe true
                    }
                }

                game.submitManaSourcesDecision(selectedSources = listOf(mountainId))

                // Step 3: Target Glory Seeker
                val targetId = game.findPermanent("Glory Seeker")!!
                game.selectTargets(listOf(targetId))

                // Resolve
                game.resolveStack()

                withClue("Glory Seeker should be destroyed by 2 damage") {
                    game.isOnBattlefield("Glory Seeker") shouldBe false
                }
            }
        }
    }
}
