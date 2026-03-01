package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Decree of Pain.
 *
 * Decree of Pain: {6}{B}{B} Sorcery
 * Destroy all creatures. They can't be regenerated. Draw a card for each creature destroyed this way.
 * Cycling {3}{B}{B}
 * When you cycle Decree of Pain, all creatures get -2/-2 until end of turn.
 */
class DecreeOfPainScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        context("Decree of Pain main spell") {

            test("destroys all creatures and draws cards for each destroyed") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Decree of Pain")
                    .withLandsOnBattlefield(1, "Swamp", 8)
                    .withCardOnBattlefield(1, "Severed Legion")   // 2/2 Zombie
                    .withCardOnBattlefield(2, "Glory Seeker")     // 2/2 Soldier
                    .withCardOnBattlefield(2, "Gustcloak Runner") // 1/1 Soldier
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val initialHandSize = game.handSize(1)

                // Cast Decree of Pain
                val result = game.castSpell(1, "Decree of Pain")
                withClue("Decree of Pain should cast successfully: ${result.error}") {
                    result.error shouldBe null
                }

                // Resolve
                game.resolveStack()

                // All 3 creatures should be destroyed
                withClue("Player 1's creature should be destroyed") {
                    game.isOnBattlefield("Severed Legion") shouldBe false
                }
                withClue("Player 2's creatures should be destroyed") {
                    game.isOnBattlefield("Glory Seeker") shouldBe false
                    game.isOnBattlefield("Gustcloak Runner") shouldBe false
                }

                // Player 1 should have drawn 3 cards (one for each destroyed creature)
                // Hand: initialHandSize - 1 (cast) + 3 (drawn)
                withClue("Player 1 should have drawn 3 cards") {
                    game.handSize(1) shouldBe initialHandSize - 1 + 3
                }
            }

            test("draws zero cards when no creatures on battlefield") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Decree of Pain")
                    .withLandsOnBattlefield(1, "Swamp", 8)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val initialHandSize = game.handSize(1)

                val result = game.castSpell(1, "Decree of Pain")
                withClue("Should cast successfully: ${result.error}") {
                    result.error shouldBe null
                }

                game.resolveStack()

                // No creatures destroyed, so no cards drawn
                withClue("Player 1 hand size should decrease by 1 (cast, no draws)") {
                    game.handSize(1) shouldBe initialHandSize - 1
                }
            }
        }

        context("Decree of Pain cycling trigger") {

            test("cycling gives all creatures -2/-2 until end of turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Decree of Pain")
                    .withLandsOnBattlefield(1, "Swamp", 5) // {3}{B}{B} for cycling
                    .withCardOnBattlefield(2, "Towering Baloth") // 7/6 Beast
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val balothId = game.findPermanent("Towering Baloth")!!

                // Cycle Decree of Pain
                val cycleResult = game.cycleCard(1, "Decree of Pain")
                withClue("Cycling should succeed: ${cycleResult.error}") {
                    cycleResult.error shouldBe null
                }

                // Resolve the cycling trigger
                game.resolveStack()

                // Towering Baloth (7/6) should now be 5/4
                val projected = stateProjector.project(game.state)
                withClue("Towering Baloth should be 5/4 after -2/-2") {
                    projected.getPower(balothId) shouldBe 5
                    projected.getToughness(balothId) shouldBe 4
                }
            }

            test("cycling trigger kills 2-toughness creatures via SBA") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Decree of Pain")
                    .withLandsOnBattlefield(1, "Swamp", 5)
                    .withCardOnBattlefield(2, "Glory Seeker")     // 2/2
                    .withCardOnBattlefield(2, "Gustcloak Runner") // 1/1
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cycleResult = game.cycleCard(1, "Decree of Pain")
                withClue("Cycling should succeed: ${cycleResult.error}") {
                    cycleResult.error shouldBe null
                }

                game.resolveStack()

                // 2/2 becomes 0/0 — dies to SBA
                withClue("Glory Seeker (2/2) should die from -2/-2") {
                    game.isOnBattlefield("Glory Seeker") shouldBe false
                }

                // 1/1 becomes -1/-1 — dies to SBA
                withClue("Gustcloak Runner (1/1) should die from -2/-2") {
                    game.isOnBattlefield("Gustcloak Runner") shouldBe false
                }
            }
        }
    }
}
