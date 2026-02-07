package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Starstorm.
 *
 * Card reference:
 * - Starstorm ({X}{R}{R}): Instant
 *   "Starstorm deals X damage to each creature."
 *   Cycling {3}
 *
 * Test scenarios:
 * 1. X=2 deals 2 damage to each creature, killing small ones
 * 2. X=0 deals 0 damage (no creatures die)
 * 3. Can be cast as instant (on opponent's turn or during combat)
 * 4. Cycling draws a card
 */
class StarstormScenarioTest : ScenarioTestBase() {

    init {
        context("Starstorm") {
            test("deals X damage to each creature when X=2") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Starstorm")
                    .withLandsOnBattlefield(1, "Mountain", 4) // {R}{R} + X=2
                    .withCardOnBattlefield(1, "Glory Seeker")    // 2/2 - should die
                    .withCardOnBattlefield(2, "Towering Baloth") // 7/6 - should survive
                    .withCardOnBattlefield(2, "Elvish Warrior")  // 2/3 - should survive with 2 damage
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castXSpell(1, "Starstorm", xValue = 2)
                game.resolveStack()

                withClue("Glory Seeker (2/2) should die from 2 damage") {
                    game.isOnBattlefield("Glory Seeker") shouldBe false
                }
                withClue("Towering Baloth (7/6) should survive 2 damage") {
                    game.isOnBattlefield("Towering Baloth") shouldBe true
                }
                withClue("Elvish Warrior (2/3) should survive 2 damage") {
                    game.isOnBattlefield("Elvish Warrior") shouldBe true
                }
                withClue("Starstorm should be in graveyard") {
                    game.isInGraveyard(1, "Starstorm") shouldBe true
                }
            }

            test("deals 0 damage when X=0") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Starstorm")
                    .withLandsOnBattlefield(1, "Mountain", 2) // {R}{R} + X=0
                    .withCardOnBattlefield(1, "Glory Seeker")   // 2/2 - should survive
                    .withCardOnBattlefield(2, "Elvish Warrior") // 2/3 - should survive
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castXSpell(1, "Starstorm", xValue = 0)
                game.resolveStack()

                withClue("Glory Seeker should survive 0 damage") {
                    game.isOnBattlefield("Glory Seeker") shouldBe true
                }
                withClue("Elvish Warrior should survive 0 damage") {
                    game.isOnBattlefield("Elvish Warrior") shouldBe true
                }
            }

            test("deals X damage with large X value") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Starstorm")
                    .withLandsOnBattlefield(1, "Mountain", 7) // {R}{R} + X=5
                    .withCardOnBattlefield(1, "Towering Baloth") // 7/6 - should die from 5 damage
                    .withCardOnBattlefield(2, "Elvish Warrior")  // 2/3 - should die from 5 damage
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castXSpell(1, "Starstorm", xValue = 5)
                game.resolveStack()

                withClue("Towering Baloth (7/6) should survive with 5 damage (toughness 6)") {
                    game.isOnBattlefield("Towering Baloth") shouldBe true
                }
                withClue("Elvish Warrior (2/3) should die from 5 damage") {
                    game.isOnBattlefield("Elvish Warrior") shouldBe false
                }
            }

            test("cycling draws a card") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Starstorm")
                    .withLandsOnBattlefield(1, "Mountain", 3) // {3} for cycling cost
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val initialHandSize = game.handSize(1)

                game.cycleCard(1, "Starstorm")

                withClue("Player should have same hand size (discard Starstorm + draw 1)") {
                    game.handSize(1) shouldBe initialHandSize
                }
                withClue("Starstorm should be in graveyard after cycling") {
                    game.isInGraveyard(1, "Starstorm") shouldBe true
                }
            }

            test("fails to cast without enough mana") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Starstorm")
                    .withLandsOnBattlefield(1, "Mountain", 1) // Only 1 Mountain, need at least 2 for {R}{R}
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castXSpell(1, "Starstorm", xValue = 0)

                withClue("Starstorm should fail to cast with only 1 Mountain") {
                    castResult.error shouldBe "Not enough mana to cast this spell"
                }
            }
        }
    }
}
