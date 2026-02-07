package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.Duration
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario test for Blatant Thievery.
 *
 * Card reference:
 * - Blatant Thievery (4UUU): Sorcery
 *   "For each opponent, gain control of target permanent that player controls."
 */
class BlatantThieveryScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        context("Blatant Thievery steals control of target permanent") {
            test("casting Blatant Thievery on opponent's creature gives you control of it") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Blatant Thievery")
                    .withLandsOnBattlefield(1, "Island", 7)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val opponentCreature = game.findPermanent("Grizzly Bears")!!

                // Cast Blatant Thievery targeting opponent's creature
                val castResult = game.castSpell(1, "Blatant Thievery", opponentCreature)
                withClue("Cast should succeed") {
                    castResult.error shouldBe null
                }

                // Resolve the spell
                game.resolveStack()

                // The projected state should show P1 as controller of Grizzly Bears
                val projected = stateProjector.project(game.state)
                val creatureController = projected.getController(opponentCreature)
                withClue("Player 1 should control the stolen creature") {
                    creatureController shouldBe game.player1Id
                }
            }

            test("stolen permanent stays under control permanently (floating effect has Duration.Permanent)") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Blatant Thievery")
                    .withLandsOnBattlefield(1, "Island", 7)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val opponentCreature = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Blatant Thievery", opponentCreature)
                game.resolveStack()

                // Verify the floating effect has Duration.Permanent
                val controlEffect = game.state.floatingEffects.find { floating ->
                    opponentCreature in floating.effect.affectedEntities
                }
                withClue("Floating control effect should exist") {
                    controlEffect shouldNotBe null
                }
                withClue("Control effect should have permanent duration") {
                    controlEffect!!.duration shouldBe Duration.Permanent
                }

                // Advance through a full turn cycle and verify control persists
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passPriority()
                game.passPriority()

                // Player 2's turn
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passPriority()
                game.passPriority()

                // Back to Player 1's turn
                val projected = stateProjector.project(game.state)
                withClue("Player 1 should still control the creature after a full turn cycle") {
                    projected.getController(opponentCreature) shouldBe game.player1Id
                }
            }

            test("owner remains original player after control steal") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Blatant Thievery")
                    .withLandsOnBattlefield(1, "Island", 7)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val opponentCreature = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Blatant Thievery", opponentCreature)
                game.resolveStack()

                // Owner should still be Player 2
                val creatureEntity = game.state.getEntity(opponentCreature)!!
                val owner = creatureEntity.get<OwnerComponent>()?.playerId
                    ?: creatureEntity.get<CardComponent>()?.ownerId
                withClue("Owner should remain Player 2") {
                    owner shouldBe game.player2Id
                }

                // But projected controller should be Player 1
                val projected = stateProjector.project(game.state)
                withClue("Projected controller should be Player 1") {
                    projected.getController(opponentCreature) shouldBe game.player1Id
                }
            }

            test("client state shows correct controller for stolen permanent") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Blatant Thievery")
                    .withLandsOnBattlefield(1, "Island", 7)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val opponentCreature = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Blatant Thievery", opponentCreature)
                game.resolveStack()

                // Get client states for both players
                val player1ClientState = game.getClientState(1)
                val player2ClientState = game.getClientState(2)

                // Player 1's client state should show them as controller
                val creatureInP1View = player1ClientState.cards[opponentCreature]
                withClue("Creature should appear in Player 1's client state") {
                    creatureInP1View shouldNotBe null
                }
                withClue("Player 1 should be shown as controller") {
                    creatureInP1View!!.controllerId shouldBe game.player1Id
                }

                // Player 2's client state should also show Player 1 as controller
                val creatureInP2View = player2ClientState.cards[opponentCreature]
                withClue("Creature should appear in Player 2's client state") {
                    creatureInP2View shouldNotBe null
                }
                withClue("Player 2 should see Player 1 as controller") {
                    creatureInP2View!!.controllerId shouldBe game.player1Id
                }

                // Owner should still be Player 2 in both views
                withClue("Owner should still be Player 2 in Player 1's view") {
                    creatureInP1View!!.ownerId shouldBe game.player2Id
                }
            }
        }
    }
}
