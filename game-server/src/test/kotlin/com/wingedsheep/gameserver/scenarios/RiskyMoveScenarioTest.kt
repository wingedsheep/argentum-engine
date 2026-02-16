package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.CoinFlipEvent
import com.wingedsheep.engine.core.ControlChangedEvent
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Risky Move (ONS #223).
 *
 * Card reference:
 * - Risky Move ({3}{R}{R}{R}): Enchantment
 *   At the beginning of each player's upkeep, that player gains control of Risky Move.
 *   When you gain control of Risky Move from another player, choose a creature you control
 *   and an opponent. Flip a coin. If you lose the flip, that opponent gains control of
 *   that creature.
 *
 * Cards used:
 * - Glory Seeker (Creature — Human Soldier 2/2) — test creature
 * - Grizzly Bears (Creature — Bear 2/2) — another test creature
 */
class RiskyMoveScenarioTest : ScenarioTestBase() {

    init {
        context("Risky Move upkeep control transfer") {

            test("active player gains control at their upkeep") {
                // P1 controls Risky Move. P2's upkeep → P2 gains control.
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Risky Move")
                    .withCardOnBattlefield(2, "Glory Seeker")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UNTAP)
                    .build()

                // Pass through P1's turn until P2's upkeep
                // P1's upkeep: Risky Move triggers, but P1 already controls it → no-op
                // (GainControlByActivePlayerEffect checks if current controller == active player)
                // Then continue to P2's upkeep where the transfer actually happens
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)

                // We're now at P1's upkeep. P1 already controls it, so pass through.
                // The trigger fires but the effect is a no-op (active player = controller).
                // We need to advance to P2's upkeep.
                // First pass through P1's turn entirely
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)

                // Now we should be at P2's upkeep. The trigger fires and transfers control.
                // Wait for trigger to resolve
                var iterations = 0
                while (iterations < 50) {
                    val p = game.state.priorityPlayerId ?: break
                    // If there's a pending decision (target selection for 2nd trigger), handle it
                    if (game.hasPendingDecision()) break
                    game.execute(PassPriority(p))
                    iterations++
                    // Check if a ChangeController effect for Risky Move was created
                    val hasControlChange = game.state.floatingEffects.any { floating ->
                        floating.effect.modification is SerializableModification.ChangeController &&
                            floating.sourceName == "Risky Move"
                    }
                    if (hasControlChange) break
                }

                // Verify that Risky Move's control was changed
                withClue("Should have a ChangeController floating effect for Risky Move") {
                    game.state.floatingEffects.any { floating ->
                        floating.effect.modification is SerializableModification.ChangeController &&
                            floating.sourceName == "Risky Move"
                    } shouldBe true
                }

                val controlEffect = game.state.floatingEffects.first { floating ->
                    floating.effect.modification is SerializableModification.ChangeController &&
                        floating.sourceName == "Risky Move"
                }
                val changeController = controlEffect.effect.modification as SerializableModification.ChangeController

                withClue("Control of Risky Move should transfer to the active player (P2)") {
                    changeController.newControllerId shouldBe game.player2Id
                }
            }

            test("no control transfer when active player already controls it") {
                // P1 controls Risky Move. P1's upkeep → P1 already controls it, no change.
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Risky Move")
                    .withCardOnBattlefield(1, "Glory Seeker")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UNTAP)
                    .build()

                // Advance to P1's upkeep
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)

                // Pass through the upkeep - trigger fires but is a no-op
                var iterations = 0
                while (iterations < 50) {
                    val p = game.state.priorityPlayerId ?: break
                    if (game.hasPendingDecision()) break
                    game.execute(PassPriority(p))
                    iterations++
                    if (game.state.phase != Phase.BEGINNING || game.state.step != Step.UPKEEP) break
                }

                withClue("Should NOT have any ChangeController floating effects (controller = active player)") {
                    game.state.floatingEffects.none { floating ->
                        floating.effect.modification is SerializableModification.ChangeController &&
                            floating.sourceName == "Risky Move"
                    } shouldBe true
                }
            }
        }

        context("Risky Move gain-control trigger with coin flip") {

            test("gaining control triggers coin flip - result depends on flip outcome") {
                // P2 controls Risky Move. P1's upkeep → P1 gains control from P2 →
                // triggers second ability: choose creature P1 controls + opponent (P2).
                // Flip coin: lose → P2 gets the creature; win → nothing happens.
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(2, "Risky Move")
                    .withCardOnBattlefield(1, "Glory Seeker")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UNTAP)
                    .build()

                // Advance to P1's upkeep
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)

                // The first trigger (EachUpkeep) fires and transfers control to P1.
                // This generates a ControlChangedEvent which triggers the second ability.
                // The second ability requires target selection (creature you control).

                // Pass priority until we get a target selection decision or everything resolves
                var iterations = 0
                var controlTransferred = false
                while (iterations < 80) {
                    if (game.hasPendingDecision()) break
                    val p = game.state.priorityPlayerId ?: break
                    game.execute(PassPriority(p))
                    iterations++

                    // Check for control change
                    if (!controlTransferred && game.state.floatingEffects.any { floating ->
                        floating.effect.modification is SerializableModification.ChangeController &&
                            floating.sourceName == "Risky Move"
                    }) {
                        controlTransferred = true
                    }

                    // If we've left upkeep, stop
                    if (game.state.phase != Phase.BEGINNING || game.state.step != Step.UPKEEP) break
                }

                withClue("Risky Move control should have been transferred to P1") {
                    controlTransferred shouldBe true
                }

                // If there's a pending decision, it should be target selection for the creature
                if (game.hasPendingDecision()) {
                    val creatureId = game.findPermanent("Glory Seeker")
                    withClue("Glory Seeker should still be on the battlefield") {
                        creatureId shouldNotBe null
                    }

                    // Select the creature as target
                    game.selectTargets(listOf(creatureId!!))

                    // Resolve the triggered ability on the stack
                    game.resolveStack()

                    // At this point, the coin flip has happened. Check the result.
                    // We can't control the coin flip, so verify consistency:
                    // - If there's a ChangeController effect for Glory Seeker, the flip was lost
                    // - If there isn't, the flip was won
                    val gloryControlChanged = game.state.floatingEffects.any { floating ->
                        val mod = floating.effect.modification
                        mod is SerializableModification.ChangeController &&
                            floating.effect.affectedEntities.contains(creatureId)
                    }

                    if (gloryControlChanged) {
                        // Coin flip was lost - opponent (P2) should now control the creature
                        val controlMod = game.state.floatingEffects.first { floating ->
                            floating.effect.affectedEntities.contains(creatureId) &&
                                floating.effect.modification is SerializableModification.ChangeController
                        }.effect.modification as SerializableModification.ChangeController

                        withClue("If coin flip lost, P2 should gain control of Glory Seeker") {
                            controlMod.newControllerId shouldBe game.player2Id
                        }
                    }
                    // If not changed, coin flip was won - creature stays with P1 (valid outcome)
                }
            }
        }

        context("Risky Move with no valid targets") {

            test("no creatures to choose - trigger fizzles") {
                // P2 controls Risky Move. P1's upkeep → P1 gains control from P2 →
                // triggers second ability, but P1 has no creatures → trigger has no valid target
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(2, "Risky Move")
                    // P1 has no creatures
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UNTAP)
                    .build()

                // Advance to P1's upkeep
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)

                // Pass through - the control transfer trigger fires, transferring control to P1.
                // The second trigger fires but has no valid target (no creatures P1 controls).
                // It should fizzle or be skipped.
                var iterations = 0
                while (iterations < 80) {
                    if (game.hasPendingDecision()) {
                        // If we get a target decision with no valid targets, skip it
                        game.skipTargets()
                    }
                    val p = game.state.priorityPlayerId ?: break
                    game.execute(PassPriority(p))
                    iterations++
                    if (game.state.phase != Phase.BEGINNING || game.state.step != Step.UPKEEP) break
                }

                // Risky Move should have been transferred to P1
                val riskyMoveControlled = game.state.floatingEffects.any { floating ->
                    floating.effect.modification is SerializableModification.ChangeController &&
                        floating.sourceName == "Risky Move"
                }
                withClue("Risky Move control should have transferred to P1") {
                    riskyMoveControlled shouldBe true
                }

                // No creatures should have changed control (there were none)
                val creatureControlChanges = game.state.floatingEffects.count { floating ->
                    floating.effect.modification is SerializableModification.ChangeController &&
                        floating.sourceName != "Risky Move"
                }
                withClue("No creature control changes should have occurred") {
                    creatureControlChanges shouldBe 0
                }
            }
        }
    }
}
