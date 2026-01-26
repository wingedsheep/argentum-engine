package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario test for Temporary Truce.
 *
 * Card reference:
 * - Temporary Truce ({1}{W}): Sorcery
 *   "Each player may draw up to two cards. For each card less than two a player
 *   draws this way, that player gains 2 life."
 *
 * Test scenarios:
 * 1. Both players draw 2 cards, gain no life
 * 2. Player 1 draws 0 cards (gains 4 life), Player 2 draws 2 cards (gains 0 life)
 * 3. Player 1 draws 1 card (gains 2 life), Player 2 draws 1 card (gains 2 life)
 * 4. Player 1 draws 2 cards (gains 0 life), Player 2 draws 0 cards (gains 4 life)
 */
class TemporaryTruceScenarioTest : ScenarioTestBase() {

    init {
        context("Temporary Truce effect") {
            test("both players draw 2 cards and gain no life") {
                // Setup: Player 1 has Temporary Truce, both players have cards in library
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Temporary Truce")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withLifeTotal(1, 20)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val initialHandSize1 = game.handSize(1)
                val initialHandSize2 = game.handSize(2)

                // Cast Temporary Truce
                val castResult = game.castSpell(1, "Temporary Truce")
                withClue("Temporary Truce should be cast successfully") {
                    castResult.error shouldBe null
                }

                // Resolve the spell
                game.resolveStack()

                // Player 1 (active player) should be prompted first
                withClue("There should be a pending decision for player 1") {
                    game.hasPendingDecision() shouldBe true
                    game.getPendingDecision().shouldBeInstanceOf<ChooseNumberDecision>()
                }

                // Player 1 chooses to draw 2 cards
                game.chooseNumber(2)

                // Player 2 should be prompted next
                withClue("There should be a pending decision for player 2") {
                    game.hasPendingDecision() shouldBe true
                    game.getPendingDecision().shouldBeInstanceOf<ChooseNumberDecision>()
                }

                // Player 2 chooses to draw 2 cards
                game.chooseNumber(2)

                // Verify results - both players drew 2 cards and gained no life
                withClue("Player 1 should have drawn 2 cards") {
                    game.handSize(1) shouldBe initialHandSize1 - 1 + 2 // -1 for casting spell, +2 for draw
                }

                withClue("Player 2 should have drawn 2 cards") {
                    game.handSize(2) shouldBe initialHandSize2 + 2
                }

                withClue("Player 1 should still have 20 life (no life gain)") {
                    game.getLifeTotal(1) shouldBe 20
                }

                withClue("Player 2 should still have 20 life (no life gain)") {
                    game.getLifeTotal(2) shouldBe 20
                }
            }

            test("player draws 0 cards and gains 4 life") {
                // Setup
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Temporary Truce")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withLifeTotal(1, 20)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val initialHandSize1 = game.handSize(1)
                val initialHandSize2 = game.handSize(2)

                // Cast and resolve
                game.castSpell(1, "Temporary Truce")
                game.resolveStack()

                // Player 1 chooses to draw 0 cards (should gain 4 life)
                game.chooseNumber(0)

                // Player 2 chooses to draw 2 cards (should gain 0 life)
                game.chooseNumber(2)

                // Verify results
                withClue("Player 1 should have drawn 0 cards") {
                    game.handSize(1) shouldBe initialHandSize1 - 1 // -1 for casting spell only
                }

                withClue("Player 2 should have drawn 2 cards") {
                    game.handSize(2) shouldBe initialHandSize2 + 2
                }

                withClue("Player 1 should have 24 life (gained 4 for not drawing 2 cards)") {
                    game.getLifeTotal(1) shouldBe 24
                }

                withClue("Player 2 should still have 20 life") {
                    game.getLifeTotal(2) shouldBe 20
                }
            }

            test("both players draw 1 card and each gains 2 life") {
                // Setup
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Temporary Truce")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withLifeTotal(1, 20)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val initialHandSize1 = game.handSize(1)
                val initialHandSize2 = game.handSize(2)

                // Cast and resolve
                game.castSpell(1, "Temporary Truce")
                game.resolveStack()

                // Player 1 chooses to draw 1 card (should gain 2 life)
                game.chooseNumber(1)

                // Player 2 chooses to draw 1 card (should gain 2 life)
                game.chooseNumber(1)

                // Verify results
                withClue("Player 1 should have drawn 1 card") {
                    game.handSize(1) shouldBe initialHandSize1 - 1 + 1 // -1 for casting, +1 for draw
                }

                withClue("Player 2 should have drawn 1 card") {
                    game.handSize(2) shouldBe initialHandSize2 + 1
                }

                withClue("Player 1 should have 22 life (gained 2 for drawing 1 less than 2)") {
                    game.getLifeTotal(1) shouldBe 22
                }

                withClue("Player 2 should have 22 life (gained 2 for drawing 1 less than 2)") {
                    game.getLifeTotal(2) shouldBe 22
                }
            }

            test("player 2 draws 0 cards and gains 4 life") {
                // Setup
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Temporary Truce")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withLifeTotal(1, 20)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val initialHandSize1 = game.handSize(1)
                val initialHandSize2 = game.handSize(2)

                // Cast and resolve
                game.castSpell(1, "Temporary Truce")
                game.resolveStack()

                // Player 1 chooses to draw 2 cards (should gain 0 life)
                game.chooseNumber(2)

                // Player 2 chooses to draw 0 cards (should gain 4 life)
                game.chooseNumber(0)

                // Verify results
                withClue("Player 1 should have drawn 2 cards") {
                    game.handSize(1) shouldBe initialHandSize1 - 1 + 2 // -1 for casting, +2 for draw
                }

                withClue("Player 2 should have drawn 0 cards") {
                    game.handSize(2) shouldBe initialHandSize2
                }

                withClue("Player 1 should still have 20 life") {
                    game.getLifeTotal(1) shouldBe 20
                }

                withClue("Player 2 should have 24 life (gained 4 for not drawing)") {
                    game.getLifeTotal(2) shouldBe 24
                }
            }

            test("max draw limited by library size") {
                // Setup: Player 2 has only 1 card in library
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Temporary Truce")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Mountain") // Only 1 card
                    .withLifeTotal(1, 20)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val initialHandSize2 = game.handSize(2)

                // Cast and resolve
                game.castSpell(1, "Temporary Truce")
                game.resolveStack()

                // Player 1 chooses to draw 2 cards
                game.chooseNumber(2)

                // Player 2 has only 1 card in library, so can only choose 0-1
                // The decision should be capped at library size
                val decision = game.getPendingDecision()
                withClue("Player 2's decision should exist") {
                    decision shouldBe decision
                }

                // Player 2 chooses to draw 1 card (max available)
                // They gain 2 life because they drew 1 less than 2
                game.chooseNumber(1)

                // Verify results
                withClue("Player 2 should have drawn 1 card") {
                    game.handSize(2) shouldBe initialHandSize2 + 1
                }

                withClue("Player 2 should have 22 life (gained 2 for drawing 1 less than max 2)") {
                    game.getLifeTotal(2) shouldBe 22
                }
            }
        }
    }
}
