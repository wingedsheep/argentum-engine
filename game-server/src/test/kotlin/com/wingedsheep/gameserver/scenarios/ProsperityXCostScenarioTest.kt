package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Prosperity and X cost spell handling.
 *
 * Card reference:
 * - Prosperity ({X}{U}): Sorcery
 *   "Each player draws X cards."
 *
 * Test scenarios:
 * 1. Casting with X=2 makes both players draw 2 cards
 * 2. Casting with X=0 draws no cards
 * 3. Casting with larger X value works correctly
 */
class ProsperityXCostScenarioTest : ScenarioTestBase() {

    init {
        context("Prosperity X cost spell") {
            test("casting Prosperity with X=2 makes both players draw 2 cards") {
                // Setup: Player 1 has Prosperity and 3 Islands (enough for {U} + X=2)
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Prosperity")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val initialHandSize1 = game.handSize(1)
                val initialHandSize2 = game.handSize(2)

                // Cast Prosperity with X=2
                val castResult = game.castXSpell(1, "Prosperity", xValue = 2)
                withClue("Prosperity should be cast successfully") {
                    castResult.error shouldBe null
                }

                // Resolve the spell
                game.resolveStack()

                // Verify both players drew 2 cards
                // Player 1: started with Prosperity (-1 from cast) + drew 2 = net +1
                withClue("Player 1 should have drawn 2 cards (net +1 after casting)") {
                    game.handSize(1) shouldBe initialHandSize1 - 1 + 2
                }

                // Player 2: drew 2 cards
                withClue("Player 2 should have drawn 2 cards") {
                    game.handSize(2) shouldBe initialHandSize2 + 2
                }

                // Prosperity should be in graveyard
                withClue("Prosperity should be in graveyard after resolution") {
                    game.isInGraveyard(1, "Prosperity") shouldBe true
                }
            }

            test("casting Prosperity with X=0 draws no cards") {
                // Setup: Player 1 has Prosperity and 1 Island (only enough for {U})
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Prosperity")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val initialHandSize1 = game.handSize(1)
                val initialHandSize2 = game.handSize(2)

                // Cast Prosperity with X=0
                val castResult = game.castXSpell(1, "Prosperity", xValue = 0)
                withClue("Prosperity with X=0 should be cast successfully") {
                    castResult.error shouldBe null
                }

                // Resolve the spell
                game.resolveStack()

                // Verify no cards were drawn
                withClue("Player 1 should not have drawn any cards (net -1 from casting)") {
                    game.handSize(1) shouldBe initialHandSize1 - 1
                }

                withClue("Player 2 should not have drawn any cards") {
                    game.handSize(2) shouldBe initialHandSize2
                }
            }

            test("casting Prosperity with X=4 makes both players draw 4 cards") {
                // Setup: Player 1 has Prosperity and 5 Islands (enough for {U} + X=4)
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Prosperity")
                    .withLandsOnBattlefield(1, "Island", 5)
                    // Need enough cards in library for both players
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val initialHandSize1 = game.handSize(1)
                val initialHandSize2 = game.handSize(2)

                // Cast Prosperity with X=4
                val castResult = game.castXSpell(1, "Prosperity", xValue = 4)
                withClue("Prosperity with X=4 should be cast successfully") {
                    castResult.error shouldBe null
                }

                // Resolve the spell
                game.resolveStack()

                // Verify both players drew 4 cards
                withClue("Player 1 should have drawn 4 cards (net +3 after casting)") {
                    game.handSize(1) shouldBe initialHandSize1 - 1 + 4
                }

                withClue("Player 2 should have drawn 4 cards") {
                    game.handSize(2) shouldBe initialHandSize2 + 4
                }
            }

            test("Prosperity fails to cast without enough mana for X value") {
                // Setup: Player 1 has Prosperity and 2 Islands (enough for {U} + X=1, but not X=2)
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Prosperity")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Try to cast Prosperity with X=2 (requires 3 mana, but only have 2)
                val castResult = game.castXSpell(1, "Prosperity", xValue = 2)

                withClue("Prosperity with X=2 should fail with only 2 Islands") {
                    castResult.error shouldBe "Not enough mana to cast this spell"
                }
            }

            test("Prosperity fails to cast without blue mana") {
                // Setup: Player 1 has Prosperity but only Forests (no blue mana)
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Prosperity")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Try to cast Prosperity with X=0 (still needs {U})
                val castResult = game.castXSpell(1, "Prosperity", xValue = 0)

                withClue("Prosperity should fail without blue mana") {
                    castResult.error shouldBe "Not enough mana to cast this spell"
                }
            }
        }
    }
}
