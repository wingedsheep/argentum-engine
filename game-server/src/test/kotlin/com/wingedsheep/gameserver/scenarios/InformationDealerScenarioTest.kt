package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Information Dealer.
 *
 * Card reference:
 * - Information Dealer ({1}{U}): Creature — Human Wizard 1/1
 *   "{T}: Look at the top X cards of your library, where X is the number of
 *   Wizards you control, then put them back in any order."
 */
class InformationDealerScenarioTest : ScenarioTestBase() {

    private fun TestGame.activateInformationDealer() {
        val dealerId = findPermanent("Information Dealer")!!
        val cardDef = cardRegistry.getCard("Information Dealer")!!
        val ability = cardDef.script.activatedAbilities.first()
        val result = execute(
            ActivateAbility(
                playerId = player1Id,
                sourceId = dealerId,
                abilityId = ability.id
            )
        )
        withClue("Ability should activate successfully: ${result.error}") {
            result.error shouldBe null
        }
    }

    init {
        context("Information Dealer - look at top X cards where X = Wizards you control") {

            test("with only Information Dealer (1 Wizard), looks at top 1 card - no reorder needed") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Information Dealer")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.activateInformationDealer()
                game.resolveStack()

                // With 1 Wizard, X=1, so only 1 card - no reorder decision needed
                withClue("No reorder decision needed for 1 card") {
                    game.hasPendingDecision() shouldBe false
                }

                // Library should still have all 3 cards
                withClue("Library size unchanged") {
                    game.librarySize(1) shouldBe 3
                }
            }

            test("with 2 Wizards, looks at top 2 cards and allows reorder") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Information Dealer")
                    .withCardOnBattlefield(1, "Sage Aven") // Bird Wizard
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.activateInformationDealer()
                game.resolveStack()

                // With 2 Wizards, X=2
                game.hasPendingDecision() shouldBe true
                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<ReorderLibraryDecision>()
                decision as ReorderLibraryDecision
                withClue("Should see 2 cards") {
                    decision.cards.size shouldBe 2
                }

                // Reverse the order
                val libraryZone = ZoneKey(game.player1Id, Zone.LIBRARY)
                val topTwo = game.state.getZone(libraryZone).take(2)
                val reversedOrder = topTwo.reversed()
                game.submitDecision(OrderedResponse(decision.id, reversedOrder))

                // Verify reorder
                val libraryAfter = game.state.getZone(libraryZone)
                libraryAfter.take(2) shouldBe reversedOrder
            }

            test("with 3 Wizards, looks at top 3 cards") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Information Dealer")
                    .withCardOnBattlefield(1, "Sage Aven") // Bird Wizard
                    .withCardOnBattlefield(1, "Crafty Pathmage") // Human Wizard
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(1, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.activateInformationDealer()
                game.resolveStack()

                game.hasPendingDecision() shouldBe true
                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<ReorderLibraryDecision>()
                decision as ReorderLibraryDecision
                withClue("Should see 3 cards") {
                    decision.cards.size shouldBe 3
                }

                // Submit in same order to continue
                game.submitDecision(OrderedResponse(decision.id, decision.cards))

                // Library should still have all 4 cards
                withClue("Library size unchanged") {
                    game.librarySize(1) shouldBe 4
                }
            }

            test("counts only Wizards you control, not opponent's") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Information Dealer")
                    .withCardOnBattlefield(2, "Sage Aven") // Opponent's Wizard
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.activateInformationDealer()
                game.resolveStack()

                // Only 1 Wizard you control, so X=1, no reorder
                withClue("No reorder decision for 1 card") {
                    game.hasPendingDecision() shouldBe false
                }
            }

            test("fewer cards in library than Wizards - looks at all available") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Information Dealer")
                    .withCardOnBattlefield(1, "Sage Aven")
                    .withCardOnBattlefield(1, "Crafty Pathmage")
                    .withCardInLibrary(1, "Mountain") // Only 1 card but 3 Wizards
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.activateInformationDealer()
                game.resolveStack()

                // 3 Wizards but only 1 card - no reorder needed for single card
                withClue("No reorder decision for 1 card") {
                    game.hasPendingDecision() shouldBe false
                }
            }

            test("empty library - nothing happens") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Information Dealer")
                    .withCardOnBattlefield(1, "Sage Aven")
                    // No cards in library
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.activateInformationDealer()
                game.resolveStack()

                withClue("No pending decision with empty library") {
                    game.hasPendingDecision() shouldBe false
                }
            }
        }
    }
}
