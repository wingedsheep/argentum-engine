package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.Duration
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario test for Threaten.
 *
 * Card reference:
 * - Threaten (2R): Sorcery
 *   "Untap target creature and gain control of it until end of turn.
 *   That creature gains haste until end of turn."
 */
class ThreatenScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        context("Threaten steals control of target creature until end of turn") {
            test("casting Threaten on tapped opponent creature untaps it, gives control and haste") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Threaten")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withCardOnBattlefield(2, "Grizzly Bears", tapped = true)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val opponentCreature = game.findPermanent("Grizzly Bears")!!

                // Verify creature starts tapped
                withClue("Creature should start tapped") {
                    game.state.getEntity(opponentCreature)?.has<TappedComponent>() shouldBe true
                }

                // Cast Threaten targeting opponent's creature
                val castResult = game.castSpell(1, "Threaten", opponentCreature)
                withClue("Cast should succeed") {
                    castResult.error shouldBe null
                }

                // Resolve the spell
                game.resolveStack()

                // Verify untapped
                withClue("Creature should be untapped after Threaten resolves") {
                    game.state.getEntity(opponentCreature)?.has<TappedComponent>() shouldBe false
                }

                // Verify control changed
                val projected = stateProjector.project(game.state)
                withClue("Player 1 should control the stolen creature") {
                    projected.getController(opponentCreature) shouldBe game.player1Id
                }

                // Verify haste
                withClue("Creature should have haste") {
                    projected.hasKeyword(opponentCreature, Keyword.HASTE) shouldBe true
                }
            }

            test("control reverts at end of turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Threaten")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val opponentCreature = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Threaten", opponentCreature)
                game.resolveStack()

                // Confirm we have control
                val projectedBefore = stateProjector.project(game.state)
                withClue("Player 1 should control creature after Threaten") {
                    projectedBefore.getController(opponentCreature) shouldBe game.player1Id
                }

                // Advance to end of turn - control should revert
                game.passUntilPhase(Phase.ENDING, Step.CLEANUP)

                val projectedAfter = stateProjector.project(game.state)
                withClue("Control should revert to Player 2 at end of turn") {
                    projectedAfter.getController(opponentCreature) shouldBe game.player2Id
                }
            }

            test("owner remains original player after Threaten") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Threaten")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val opponentCreature = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Threaten", opponentCreature)
                game.resolveStack()

                // Owner should still be Player 2
                val owner = game.state.getEntity(opponentCreature)?.get<OwnerComponent>()?.playerId
                withClue("Owner should remain Player 2") {
                    owner shouldBe game.player2Id
                }

                // But projected controller should be Player 1
                val projected = stateProjector.project(game.state)
                withClue("Projected controller should be Player 1") {
                    projected.getController(opponentCreature) shouldBe game.player1Id
                }
            }

            test("floating effect has Duration.EndOfTurn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Threaten")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val opponentCreature = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Threaten", opponentCreature)
                game.resolveStack()

                // Verify the floating control effect has EndOfTurn duration
                val controlEffect = game.state.floatingEffects.find { floating ->
                    opponentCreature in floating.effect.affectedEntities
                            && floating.duration == Duration.EndOfTurn
                }
                withClue("Floating control effect with EndOfTurn duration should exist") {
                    controlEffect shouldNotBe null
                }
            }
        }
    }
}
