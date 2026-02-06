package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario test for Annex.
 *
 * Card reference:
 * - Annex (2UU): Enchantment — Aura
 *   "Enchant land"
 *   "You control enchanted land."
 */
class AnnexScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()
    private val manaSolver = ManaSolver(cardRegistry)

    init {
        context("Annex steals control of enchanted land") {
            test("casting Annex on opponent's land gives you control of it") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Annex")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withLandsOnBattlefield(2, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val opponentForest = game.findPermanent("Forest")!!

                // Cast Annex targeting opponent's Forest
                val castResult = game.castSpell(1, "Annex", opponentForest)
                withClue("Cast should succeed") {
                    castResult.error shouldBe null
                }

                // Resolve the spell
                game.resolveStack()

                // Annex should be on the battlefield
                withClue("Annex should be on the battlefield") {
                    game.isOnBattlefield("Annex") shouldBe true
                }

                // Annex should be attached to the Forest
                val annexId = game.findPermanent("Annex")!!
                val annexEntity = game.state.getEntity(annexId)!!
                val attachedTo = annexEntity.get<AttachedToComponent>()
                withClue("Annex should be attached to the Forest") {
                    attachedTo shouldNotBe null
                    attachedTo!!.targetId shouldBe opponentForest
                }

                // The projected state should show P1 as controller of the Forest
                val projected = stateProjector.project(game.state)
                val forestController = projected.getController(opponentForest)
                withClue("Player 1 should control the enchanted Forest") {
                    forestController shouldBe game.player1Id
                }
            }

            test("Annex on own land keeps control unchanged") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Annex")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val ownForest = game.findPermanent("Forest")!!

                // Cast Annex targeting own Forest
                val castResult = game.castSpell(1, "Annex", ownForest)
                withClue("Cast should succeed") {
                    castResult.error shouldBe null
                }

                game.resolveStack()

                // The projected state should still show P1 as controller
                val projected = stateProjector.project(game.state)
                val forestController = projected.getController(ownForest)
                withClue("Player 1 should still control own Forest") {
                    forestController shouldBe game.player1Id
                }
            }

            test("owner remains original player when controller changes") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Annex")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withLandsOnBattlefield(2, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val opponentForest = game.findPermanent("Forest")!!

                game.castSpell(1, "Annex", opponentForest)
                game.resolveStack()

                // Owner should still be Player 2 (the original owner)
                val forestEntity = game.state.getEntity(opponentForest)!!
                val owner = forestEntity.get<OwnerComponent>()?.playerId
                    ?: forestEntity.get<CardComponent>()?.ownerId
                withClue("Owner should remain Player 2") {
                    owner shouldBe game.player2Id
                }

                // But projected controller should be Player 1
                val projected = stateProjector.project(game.state)
                withClue("Projected controller should be Player 1") {
                    projected.getController(opponentForest) shouldBe game.player1Id
                }
            }

            test("client state shows correct controller for stolen land") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Annex")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withLandsOnBattlefield(2, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val opponentForest = game.findPermanent("Forest")!!

                game.castSpell(1, "Annex", opponentForest)
                game.resolveStack()

                // Get client states for both players
                val player1ClientState = game.getClientState(1)
                val player2ClientState = game.getClientState(2)

                // Player 1's client state should show them as controller of the Forest
                val forestInP1View = player1ClientState.cards[opponentForest]
                withClue("Forest should appear in Player 1's client state") {
                    forestInP1View shouldNotBe null
                }
                withClue("Player 1 should be shown as controller of the Forest") {
                    forestInP1View!!.controllerId shouldBe game.player1Id
                }

                // Player 2's client state should also show Player 1 as controller
                val forestInP2View = player2ClientState.cards[opponentForest]
                withClue("Forest should appear in Player 2's client state") {
                    forestInP2View shouldNotBe null
                }
                withClue("Player 2 should see Player 1 as controller of the Forest") {
                    forestInP2View!!.controllerId shouldBe game.player1Id
                }

                // Owner should still be Player 2 in both views
                withClue("Owner should still be Player 2 in Player 1's view") {
                    forestInP1View!!.ownerId shouldBe game.player2Id
                }
            }

            test("mana solver finds stolen land as mana source for Annex caster") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Annex")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withLandsOnBattlefield(2, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val opponentForest = game.findPermanent("Forest")!!

                game.castSpell(1, "Annex", opponentForest)
                game.resolveStack()

                // Player 1 should be able to use the Forest for mana
                // The ManaSolver should find the stolen Forest as available mana
                val availableMana = manaSolver.getAvailableManaCount(game.state, game.player1Id)
                withClue("Player 1 should have access to the stolen Forest's mana (untapped islands that remain + stolen forest)") {
                    // 4 islands were tapped to cast Annex, so 0 islands left + 1 stolen forest = 1
                    availableMana shouldBe 1
                }

                // Player 2 should NOT be able to use the Forest for mana
                val opponentMana = manaSolver.getAvailableManaCount(game.state, game.player2Id)
                withClue("Player 2 should not have access to the stolen Forest's mana") {
                    opponentMana shouldBe 0
                }
            }

            test("getBattlefieldControlledBy returns stolen permanent") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Annex")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withLandsOnBattlefield(2, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val opponentForest = game.findPermanent("Forest")!!

                game.castSpell(1, "Annex", opponentForest)
                game.resolveStack()

                val projected = stateProjector.project(game.state)

                // Player 1's controlled permanents should include the stolen Forest
                val p1Controlled = projected.getBattlefieldControlledBy(game.player1Id)
                withClue("Player 1's controlled permanents should include the stolen Forest") {
                    p1Controlled shouldContain opponentForest
                }

                // Player 2's controlled permanents should NOT include the Forest
                val p2Controlled = projected.getBattlefieldControlledBy(game.player2Id)
                withClue("Player 2's controlled permanents should not include the Forest") {
                    p2Controlled.contains(opponentForest) shouldBe false
                }
            }
        }
    }
}
