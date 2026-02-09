package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.Duration
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
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

        context("Threaten interacts with triggered abilities") {
            test("stolen Exalted Angel's damage trigger gives life to current controller, not owner") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Threaten")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withCardOnBattlefield(2, "Exalted Angel")
                    .withActivePlayer(1)
                    .withLifeTotal(1, 15)
                    .withLifeTotal(2, 20)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val angel = game.findPermanent("Exalted Angel")!!

                // Steal Exalted Angel with Threaten
                game.castSpell(1, "Threaten", angel)
                game.resolveStack()

                // Verify Player 1 now controls the Angel
                val projected = stateProjector.project(game.state)
                withClue("Player 1 should control the stolen Exalted Angel") {
                    projected.getController(angel) shouldBe game.player1Id
                }

                // Move to combat and attack with the stolen Angel
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Exalted Angel" to 2))

                // Opponent declares no blockers
                game.passPriority()
                game.declareNoBlockers()

                // Pass through to combat damage
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)

                // Exalted Angel's trigger should fire - resolve it
                game.resolveStack()

                // Player 1 (controller) should gain 4 life, NOT Player 2 (owner)
                withClue("Player 1 (controller) should gain 4 life from Exalted Angel's trigger") {
                    game.getLifeTotal(1) shouldBe 19 // 15 + 4
                }
                withClue("Player 2 (owner) should NOT gain life - only lose 4 from damage") {
                    game.getLifeTotal(2) shouldBe 16 // 20 - 4
                }
            }
        }

        context("Threaten interacts with sacrifice effects") {
            test("can sacrifice a stolen creature via Accursed Centaur ETB") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Threaten")
                    .withCardInHand(1, "Accursed Centaur")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val opponentCreature = game.findPermanent("Grizzly Bears")!!

                // Steal the opponent's creature with Threaten
                game.castSpell(1, "Threaten", opponentCreature)
                game.resolveStack()

                // Verify we control the stolen creature
                val projected = stateProjector.project(game.state)
                withClue("Player 1 should control the stolen creature") {
                    projected.getController(opponentCreature) shouldBe game.player1Id
                }

                // Cast Accursed Centaur - its ETB will require sacrificing a creature
                val castResult = game.castSpell(1, "Accursed Centaur")
                withClue("Cast Accursed Centaur should succeed") {
                    castResult.error shouldBe null
                }

                // Resolve the creature spell (puts Centaur on battlefield, ETB trigger goes on stack)
                game.resolveStack()

                // ETB trigger should now be on the stack - resolve it
                game.resolveStack()

                // Player should be prompted to choose which creature to sacrifice
                // (Accursed Centaur + stolen Grizzly Bears = 2 creatures)
                val decision = game.state.pendingDecision
                withClue("Should have a pending sacrifice decision") {
                    decision shouldNotBe null
                }

                // Choose to sacrifice the stolen creature
                game.selectCards(listOf(opponentCreature))

                // The stolen creature should be in its OWNER's graveyard (Player 2), not Player 1's
                withClue("Stolen creature should be in owner's (Player 2) graveyard") {
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                }
                withClue("Stolen creature should NOT be in Player 1's graveyard") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe false
                }

                // Accursed Centaur should still be on the battlefield
                withClue("Accursed Centaur should remain on the battlefield") {
                    game.findPermanent("Accursed Centaur") shouldNotBe null
                }

                // The creature should no longer be on the battlefield
                withClue("Grizzly Bears should no longer be on battlefield") {
                    game.findPermanent("Grizzly Bears") shouldBe null
                }
            }

            test("stolen creature is auto-sacrificed when it's the only creature") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Threaten")
                    .withCardInHand(1, "Accursed Centaur")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val opponentCreature = game.findPermanent("Hill Giant")!!
                val ownCreature = game.findPermanent("Grizzly Bears")!!

                // Steal opponent's Hill Giant
                game.castSpell(1, "Threaten", opponentCreature)
                game.resolveStack()

                // Sacrifice our own Grizzly Bears (to set up a scenario where
                // after Centaur ETB, we only have Centaur + stolen Hill Giant)
                // Actually, let's just cast Accursed Centaur and pick the stolen creature
                game.castSpell(1, "Accursed Centaur")
                game.resolveStack() // resolve creature spell
                game.resolveStack() // resolve ETB trigger

                // Should have a sacrifice decision (Accursed Centaur, Grizzly Bears, stolen Hill Giant = 3 creatures)
                val decision = game.state.pendingDecision
                withClue("Should have a pending sacrifice decision") {
                    decision shouldNotBe null
                }

                // Sacrifice the stolen Hill Giant
                game.selectCards(listOf(opponentCreature))

                // Hill Giant should be in Player 2's graveyard (the owner)
                withClue("Hill Giant should be in owner's (Player 2) graveyard") {
                    game.isInGraveyard(2, "Hill Giant") shouldBe true
                }

                // Player 1 should still have Accursed Centaur and Grizzly Bears
                withClue("Accursed Centaur should remain") {
                    game.findPermanent("Accursed Centaur") shouldNotBe null
                }
                withClue("Grizzly Bears should remain") {
                    game.findPermanent("Grizzly Bears") shouldNotBe null
                }
            }
        }
    }
}
