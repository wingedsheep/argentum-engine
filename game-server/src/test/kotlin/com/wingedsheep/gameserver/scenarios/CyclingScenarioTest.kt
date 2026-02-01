package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

class CyclingScenarioTest : ScenarioTestBase() {

    init {
        context("Cycling ability") {
            test("can cycle a card to draw a new card") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Disciple of Grace")  // Has Cycling {2}
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInLibrary(1, "Mountain")  // Card to draw
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Verify initial state
                withClue("Should start with 1 card in hand") {
                    game.handSize(1) shouldBe 1
                }
                withClue("Should have 1 card in library") {
                    game.librarySize(1) shouldBe 1
                }
                withClue("Should start with 0 cards in graveyard") {
                    game.graveyardSize(1) shouldBe 0
                }

                // Cycle the card
                val cycleResult = game.cycleCard(1, "Disciple of Grace")
                withClue("Should cycle successfully") {
                    cycleResult.error shouldBe null
                }

                // Verify result - card was discarded and a new one drawn
                withClue("Should have 1 card in hand (drew 1)") {
                    game.handSize(1) shouldBe 1
                }
                withClue("Should have 0 cards in library (drew 1)") {
                    game.librarySize(1) shouldBe 0
                }
                withClue("Cycled card should be in graveyard") {
                    game.graveyardSize(1) shouldBe 1
                }
                withClue("Disciple of Grace should be in graveyard") {
                    game.isInGraveyard(1, "Disciple of Grace") shouldBe true
                }
            }

            test("cannot cycle without enough mana") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Disciple of Grace")  // Has Cycling {2}
                    .withLandsOnBattlefield(1, "Plains", 1)  // Only 1 mana, need 2
                    .withCardInLibrary(1, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Try to cycle - should fail
                val cycleResult = game.cycleCard(1, "Disciple of Grace")
                withClue("Should not be able to cycle without enough mana") {
                    cycleResult.error shouldBe "Not enough mana to cycle this card"
                }

                // Hand should be unchanged
                withClue("Should still have 1 card in hand") {
                    game.handSize(1) shouldBe 1
                }
            }

            test("cycling works with different card") {
                // Test cycling with a different card that has cycling
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Disciple of Malice")  // Also has Cycling {2}
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardInLibrary(1, "Forest")  // Card to draw
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cycle the card
                val cycleResult = game.cycleCard(1, "Disciple of Malice")
                withClue("Should be able to cycle") {
                    cycleResult.error shouldBe null
                }

                // Verify the cycle happened
                withClue("Cycled card should be in graveyard") {
                    game.isInGraveyard(1, "Disciple of Malice") shouldBe true
                }
                withClue("Should have drawn a card") {
                    game.handSize(1) shouldBe 1
                }
            }
        }
    }
}
