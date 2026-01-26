package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario test for Cruel Tutor.
 *
 * Card reference:
 * - Cruel Tutor (2B): Sorcery. "Search your library for a card, then shuffle and put that card on top. You lose 2 life."
 *
 * Scenario:
 * - Player 1 has Cruel Tutor in hand with enough mana (3 Swamps)
 * - Player 1 has cards in their library (Grizzly Bears, Forest, Mountain)
 * - Player 1 casts Cruel Tutor
 * - After resolution, player 1 should be prompted to search their library
 * - Player 1 selects Grizzly Bears
 * - Library should be shuffled, then Grizzly Bears should be on top
 * - Player 1 should lose 2 life
 */
class CruelTutorScenarioTest : ScenarioTestBase() {

    init {
        context("Cruel Tutor searches library and puts card on top") {
            test("selected card ends up on top of library after shuffle") {
                // Setup:
                // - Player 1 has Cruel Tutor in hand
                // - Player 1 has 3 Swamps for mana ({2}{B})
                // - Player 1 has cards in library: Grizzly Bears, Forest, Mountain
                // - It's Player 1's main phase with priority
                val game = scenario()
                    .withPlayers("TutorPlayer", "Opponent")
                    .withCardInHand(1, "Cruel Tutor")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Record initial state
                val initialLifeTotal = game.getLifeTotal(1)
                val initialLibrarySize = game.librarySize(1)

                withClue("Initial life should be 20") {
                    initialLifeTotal shouldBe 20
                }

                withClue("Initial library should have 3 cards") {
                    initialLibrarySize shouldBe 3
                }

                // Cast Cruel Tutor (no target needed - it's a sorcery that affects self)
                val castResult = game.castSpell(1, "Cruel Tutor")
                withClue("Cruel Tutor should be cast successfully: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Both players pass priority to resolve
                game.passPriority() // Player 1 passes
                game.passPriority() // Player 2 passes

                // Now there should be a pending decision for the library search
                withClue("Should have a pending decision for library search") {
                    game.hasPendingDecision() shouldBe true
                }

                val decision = game.getPendingDecision()
                withClue("Decision should be present") {
                    decision shouldNotBe null
                }

                // Find Grizzly Bears in the decision options
                val searchDecision = decision as com.wingedsheep.engine.core.SearchLibraryDecision
                val grizzlyBearsId = searchDecision.options.find { cardId ->
                    searchDecision.cards[cardId]?.name == "Grizzly Bears"
                }

                withClue("Grizzly Bears should be in the search options") {
                    grizzlyBearsId shouldNotBe null
                }

                // Select Grizzly Bears
                val selectResult = game.selectCards(listOf(grizzlyBearsId!!))
                withClue("Selection should succeed: ${selectResult.error}") {
                    selectResult.error shouldBe null
                }

                // Verify results:
                // 1. Library size should still be 3 (card moved within library)
                withClue("Library size should still be 3") {
                    game.librarySize(1) shouldBe 3
                }

                // 2. Top of library should be Grizzly Bears
                val topOfLibrary = game.state.getLibrary(game.player1Id).firstOrNull()
                val topCard = topOfLibrary?.let { game.state.getEntity(it) }
                    ?.get<CardComponent>()

                withClue("Top card of library should be Grizzly Bears") {
                    topCard?.name shouldBe "Grizzly Bears"
                }

                // 3. Player 1 should have lost 2 life
                withClue("Player 1 should have lost 2 life (20 -> 18)") {
                    game.getLifeTotal(1) shouldBe 18
                }
            }

            test("player can fail to find and just shuffles library") {
                // Setup with library containing only lands
                val game = scenario()
                    .withPlayers("TutorPlayer", "Opponent")
                    .withCardInHand(1, "Cruel Tutor")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val initialLibrarySize = game.librarySize(1)

                // Cast Cruel Tutor
                game.castSpell(1, "Cruel Tutor")
                game.passPriority() // Player 1 passes
                game.passPriority() // Player 2 passes

                // There should be a search decision
                withClue("Should have a pending decision for library search") {
                    game.hasPendingDecision() shouldBe true
                }

                // Player chooses to "fail to find" by selecting nothing
                val selectResult = game.selectCards(emptyList())
                withClue("Empty selection (fail to find) should succeed: ${selectResult.error}") {
                    selectResult.error shouldBe null
                }

                // Library should still have same size
                withClue("Library size should still be 3 after fail to find") {
                    game.librarySize(1) shouldBe initialLibrarySize
                }

                // Player should still lose 2 life
                withClue("Player 1 should have lost 2 life even on fail to find") {
                    game.getLifeTotal(1) shouldBe 18
                }
            }

            test("cruel tutor goes to graveyard after resolving") {
                val game = scenario()
                    .withPlayers("TutorPlayer", "Opponent")
                    .withCardInHand(1, "Cruel Tutor")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast and resolve
                game.castSpell(1, "Cruel Tutor")
                game.passPriority()
                game.passPriority()

                // Select a card
                val decision = game.getPendingDecision() as com.wingedsheep.engine.core.SearchLibraryDecision
                val cardId = decision.options.first()
                game.selectCards(listOf(cardId))

                // Cruel Tutor should be in graveyard
                withClue("Cruel Tutor should be in graveyard after resolution") {
                    game.isInGraveyard(1, "Cruel Tutor") shouldBe true
                }
            }
        }
    }
}
