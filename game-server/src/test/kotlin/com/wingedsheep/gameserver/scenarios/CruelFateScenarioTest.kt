package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario test for Cruel Fate.
 *
 * Card: Cruel Fate
 * {4}{U}
 * Sorcery
 * "Look at the top five cards of target opponent's library. Put one of them into that
 * player's graveyard and the rest on top of their library in any order."
 *
 * Test scenarios:
 * 1. Basic flow: Cast Cruel Fate, select 1 card for graveyard, reorder remaining 4
 * 2. Fewer cards in library: When opponent has fewer than 5 cards
 * 3. All cards go to graveyard: When opponent has exactly 1 card (edge case)
 */
class CruelFateScenarioTest : ScenarioTestBase() {

    init {
        context("Cruel Fate basic functionality") {
            test("allows caster to view opponent's library, discard one, and reorder the rest") {
                // Setup the scenario:
                // - Player 1 has Cruel Fate in hand with 5 Islands (enough mana to cast {4}{U})
                // - Player 2 has 5 known cards in library
                // - It's Player 1's main phase with priority
                val game = scenario()
                    .withPlayers("FateCaster", "Opponent")
                    .withCardInHand(1, "Cruel Fate")
                    .withLandsOnBattlefield(1, "Island", 5)
                    .withCardInLibrary(2, "Hill Giant")           // top of library
                    .withCardInLibrary(2, "Elvish Ranger")
                    .withCardInLibrary(2, "Rowan Treefolk")
                    .withCardInLibrary(2, "Willow Dryad")
                    .withCardInLibrary(2, "Panther Warriors")     // bottom of the top 5
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Record initial state
                val libraryBefore = game.librarySize(2)
                val graveyardBefore = game.graveyardSize(2)

                withClue("Opponent should start with 5 cards in library") {
                    libraryBefore shouldBe 5
                }
                withClue("Opponent should start with 0 cards in graveyard") {
                    graveyardBefore shouldBe 0
                }

                // Player 1 casts Cruel Fate targeting Player 2
                val castResult = game.execute(
                    CastSpell(
                        game.player1Id,
                        game.findCardsInHand(1, "Cruel Fate").first(),
                        listOf(com.wingedsheep.engine.state.components.stack.ChosenTarget.Player(game.player2Id))
                    )
                )
                withClue("Cruel Fate should be cast successfully: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Both players pass priority to resolve
                game.resolveStack()

                // After resolution, there should be a pending decision for card selection
                withClue("There should be a pending decision to select cards for graveyard") {
                    game.hasPendingDecision() shouldBe true
                }

                val selectDecision = game.getPendingDecision()
                withClue("Decision should be SelectCardsDecision") {
                    selectDecision.shouldBeInstanceOf<SelectCardsDecision>()
                }

                val selectCards = selectDecision as SelectCardsDecision
                withClue("Should be asked to select exactly 1 card") {
                    selectCards.minSelections shouldBe 1
                    selectCards.maxSelections shouldBe 1
                }
                withClue("Should see 5 cards to choose from") {
                    selectCards.options shouldHaveSize 5
                }
                withClue("Card info should be provided for display") {
                    selectCards.cardInfo shouldNotBe null
                    selectCards.cardInfo?.size shouldBe 5
                }

                // Select one card to put in graveyard (first card in the list)
                val cardToDiscard = selectCards.options.first()
                val selectResult = game.selectCards(listOf(cardToDiscard))

                withClue("Card selection should succeed: ${selectResult.error}") {
                    selectResult.error shouldBe null
                }

                // Now there should be a reorder decision for the remaining 4 cards
                withClue("There should be a pending decision to reorder remaining cards") {
                    game.hasPendingDecision() shouldBe true
                }

                val reorderDecision = game.getPendingDecision()
                withClue("Decision should be ReorderLibraryDecision") {
                    reorderDecision.shouldBeInstanceOf<ReorderLibraryDecision>()
                }

                val reorderCards = reorderDecision as ReorderLibraryDecision
                withClue("Should have 4 cards to reorder") {
                    reorderCards.cards shouldHaveSize 4
                }
                withClue("Card info should be provided for display") {
                    reorderCards.cardInfo.size shouldBe 4
                }
                withClue("The discarded card should not be in the reorder list") {
                    reorderCards.cards.contains(cardToDiscard) shouldBe false
                }

                // Submit the reorder - reverse the order just to change it
                val newOrder = reorderCards.cards.reversed()
                val decisionId = reorderDecision.id
                val reorderResult = game.submitDecision(OrderedResponse(decisionId, newOrder))

                withClue("Reorder should succeed: ${reorderResult.error}") {
                    reorderResult.error shouldBe null
                }

                // Verify final state
                withClue("No more pending decisions") {
                    game.hasPendingDecision() shouldBe false
                }

                withClue("Opponent should now have 4 cards in library (5 - 1 discarded)") {
                    game.librarySize(2) shouldBe 4
                }

                withClue("Opponent should now have 1 card in graveyard") {
                    game.graveyardSize(2) shouldBe 1
                }
            }

            test("works correctly when opponent has fewer than 5 cards") {
                // Setup with only 3 cards in opponent's library
                val game = scenario()
                    .withPlayers("FateCaster", "Opponent")
                    .withCardInHand(1, "Cruel Fate")
                    .withLandsOnBattlefield(1, "Island", 5)
                    .withCardInLibrary(2, "Hill Giant")
                    .withCardInLibrary(2, "Elvish Ranger")
                    .withCardInLibrary(2, "Rowan Treefolk")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val libraryBefore = game.librarySize(2)
                withClue("Opponent should start with 3 cards in library") {
                    libraryBefore shouldBe 3
                }

                // Cast Cruel Fate
                val castResult = game.execute(
                    CastSpell(
                        game.player1Id,
                        game.findCardsInHand(1, "Cruel Fate").first(),
                        listOf(com.wingedsheep.engine.state.components.stack.ChosenTarget.Player(game.player2Id))
                    )
                )
                castResult.error shouldBe null

                game.resolveStack()

                // Should be able to select from only 3 cards
                val selectDecision = game.getPendingDecision() as SelectCardsDecision
                withClue("Should see only 3 cards (all that's available)") {
                    selectDecision.options shouldHaveSize 3
                }

                // Select one for graveyard
                val cardToDiscard = selectDecision.options.first()
                game.selectCards(listOf(cardToDiscard))

                // Reorder the remaining 2
                val reorderDecision = game.getPendingDecision() as ReorderLibraryDecision
                withClue("Should have 2 cards to reorder") {
                    reorderDecision.cards shouldHaveSize 2
                }

                game.submitDecision(OrderedResponse(reorderDecision.id, reorderDecision.cards))

                // Verify final state
                withClue("Opponent should now have 2 cards in library") {
                    game.librarySize(2) shouldBe 2
                }
                withClue("Opponent should now have 1 card in graveyard") {
                    game.graveyardSize(2) shouldBe 1
                }
            }

            test("handles single card in library correctly") {
                // Setup with only 1 card in opponent's library
                val game = scenario()
                    .withPlayers("FateCaster", "Opponent")
                    .withCardInHand(1, "Cruel Fate")
                    .withLandsOnBattlefield(1, "Island", 5)
                    .withCardInLibrary(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Cruel Fate
                val castResult = game.execute(
                    CastSpell(
                        game.player1Id,
                        game.findCardsInHand(1, "Cruel Fate").first(),
                        listOf(com.wingedsheep.engine.state.components.stack.ChosenTarget.Player(game.player2Id))
                    )
                )
                castResult.error shouldBe null

                game.resolveStack()

                // When there's only 1 card and we need to put 1 in graveyard,
                // it should go directly to graveyard without decision
                withClue("Opponent should now have 0 cards in library") {
                    game.librarySize(2) shouldBe 0
                }
                withClue("Opponent should now have 1 card in graveyard") {
                    game.graveyardSize(2) shouldBe 1
                }
                withClue("No pending decisions (all cards went to graveyard)") {
                    game.hasPendingDecision() shouldBe false
                }
            }

            test("handles empty library correctly") {
                // Setup with empty opponent library
                val game = scenario()
                    .withPlayers("FateCaster", "Opponent")
                    .withCardInHand(1, "Cruel Fate")
                    .withLandsOnBattlefield(1, "Island", 5)
                    // No cards in opponent's library
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Cruel Fate
                val castResult = game.execute(
                    CastSpell(
                        game.player1Id,
                        game.findCardsInHand(1, "Cruel Fate").first(),
                        listOf(com.wingedsheep.engine.state.components.stack.ChosenTarget.Player(game.player2Id))
                    )
                )
                castResult.error shouldBe null

                game.resolveStack()

                // Nothing to look at, spell should just resolve with no effect
                withClue("Opponent should still have 0 cards in library") {
                    game.librarySize(2) shouldBe 0
                }
                withClue("Opponent should still have 0 cards in graveyard") {
                    game.graveyardSize(2) shouldBe 0
                }
                withClue("No pending decisions") {
                    game.hasPendingDecision() shouldBe false
                }
            }
        }
    }
}
