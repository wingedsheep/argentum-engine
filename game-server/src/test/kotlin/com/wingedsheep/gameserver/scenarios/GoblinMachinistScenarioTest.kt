package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.OrderedResponse
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Goblin Machinist.
 *
 * Card reference:
 * - Goblin Machinist ({4}{R}): Creature — Goblin 0/5
 *   "{2}{R}: Reveal cards from the top of your library until you reveal a nonland card.
 *   Goblin Machinist gets +X/+0 until end of turn, where X is that card's mana value.
 *   Put the revealed cards on the bottom of your library in any order."
 */
class GoblinMachinistScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    private fun TestGame.activateGoblinMachinist() {
        val machinistId = findPermanent("Goblin Machinist")!!
        val cardDef = cardRegistry.getCard("Goblin Machinist")!!
        val ability = cardDef.script.activatedAbilities.first()
        val result = execute(
            ActivateAbility(
                playerId = player1Id,
                sourceId = machinistId,
                abilityId = ability.id
            )
        )
        withClue("Ability should activate successfully: ${result.error}") {
            result.error shouldBe null
        }
    }

    init {
        context("Goblin Machinist - reveal until nonland, buff power") {

            test("nonland on top gives +X/+0 and card goes to bottom") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Goblin Machinist")
                    .withLandsOnBattlefield(1, "Mountain", 3) // For {2}{R} cost
                    .withCardInLibrary(1, "Shock") // CMC 1, nonland on top
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.activateGoblinMachinist()

                // Resolve the ability on the stack
                game.resolveStack()

                // Only 1 card revealed, no reorder needed
                withClue("No pending decision for single card") {
                    game.hasPendingDecision() shouldBe false
                }

                // Goblin Machinist should be 1/5 (base 0/5 + 1/0 from CMC 1)
                val machinistId = game.findPermanent("Goblin Machinist")!!
                withClue("Projected power should be 1") {
                    projector.getProjectedPower(game.state, machinistId) shouldBe 1
                }
                withClue("Projected toughness should be 5") {
                    projector.getProjectedToughness(game.state, machinistId) shouldBe 5
                }

                // Library should have the card on bottom
                val library = game.state.getZone(ZoneKey(game.player1Id, Zone.LIBRARY))
                val lastCardName = game.state.getEntity(library.last())?.get<CardComponent>()?.name
                withClue("Shock should be on bottom of library") {
                    lastCardName shouldBe "Shock"
                }
            }

            test("multiple revealed cards prompt reorder decision") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Goblin Machinist")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withCardInLibrary(1, "Forest") // land on top (added first = index 0)
                    .withCardInLibrary(1, "Grizzly Bears") // CMC 2, nonland (deeper = index 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Library: [Forest, Grizzly Bears] - Forest on top

                game.activateGoblinMachinist()
                game.resolveStack()

                // 2 cards revealed, should have reorder decision
                withClue("Should have reorder decision") {
                    game.hasPendingDecision() shouldBe true
                }
                game.getPendingDecision().shouldBeInstanceOf<ReorderLibraryDecision>()

                val decision = game.getPendingDecision() as ReorderLibraryDecision
                withClue("Should have 2 cards to reorder") {
                    decision.cards.size shouldBe 2
                }

                // Buff should already be applied
                val machinistId = game.findPermanent("Goblin Machinist")!!
                withClue("Projected power should be 2 (Grizzly Bears CMC)") {
                    projector.getProjectedPower(game.state, machinistId) shouldBe 2
                }

                // Submit the reorder
                game.submitDecision(OrderedResponse(decision.id, decision.cards))

                withClue("Decision should be resolved") {
                    game.hasPendingDecision() shouldBe false
                }

                // Cards should be on bottom
                val library = game.state.getZone(ZoneKey(game.player1Id, Zone.LIBRARY))
                withClue("Library should have the 2 revealed cards on bottom") {
                    library.takeLast(2).toSet() shouldBe decision.cards.toSet()
                }
            }

            test("all lands - no buff, cards go to bottom") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Goblin Machinist")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.activateGoblinMachinist()
                game.resolveStack()

                // 2 lands revealed, reorder decision
                game.hasPendingDecision() shouldBe true
                game.getPendingDecision().shouldBeInstanceOf<ReorderLibraryDecision>()

                // No buff - still 0/5
                val machinistId = game.findPermanent("Goblin Machinist")!!
                withClue("Power should be 0 (no nonland found)") {
                    projector.getProjectedPower(game.state, machinistId) shouldBe 0
                }
                withClue("Toughness should be 5") {
                    projector.getProjectedToughness(game.state, machinistId) shouldBe 5
                }

                // Submit reorder
                val decision = game.getPendingDecision() as ReorderLibraryDecision
                game.submitDecision(OrderedResponse(decision.id, decision.cards))

                game.hasPendingDecision() shouldBe false
            }

            test("empty library - no buff, no crash") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Goblin Machinist")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    // No cards in library
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.activateGoblinMachinist()
                game.resolveStack()

                withClue("No pending decision") {
                    game.hasPendingDecision() shouldBe false
                }

                val machinistId = game.findPermanent("Goblin Machinist")!!
                withClue("Power should be 0") {
                    projector.getProjectedPower(game.state, machinistId) shouldBe 0
                }
                withClue("Toughness should be 5") {
                    projector.getProjectedToughness(game.state, machinistId) shouldBe 5
                }
            }
        }
    }
}
