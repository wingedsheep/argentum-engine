package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Thoughtbound Primoc.
 *
 * Card reference:
 * - Thoughtbound Primoc ({2}{R}): Creature — Bird Beast, 2/3
 *   Flying
 *   "At the beginning of your upkeep, if a player controls more Wizards
 *   than each other player, that player gains control of Thoughtbound Primoc."
 *
 * Cards used:
 * - Crafty Pathmage (Creature — Human Wizard) — a Wizard creature
 * - Embermage Goblin (Creature — Goblin Wizard) — another Wizard creature
 * - Grizzly Bears (Creature — Bear) — non-Wizard creature
 */
class ThoughtboundPrimocScenarioTest : ScenarioTestBase() {

    init {
        context("Thoughtbound Primoc upkeep trigger") {

            test("control transfers to player with most Wizards") {
                // P1 owns Primoc + 1 Wizard, P2 has 2 Wizards → control transfers to P2
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Thoughtbound Primoc")
                    .withCardOnBattlefield(1, "Crafty Pathmage")       // P1: 1 Wizard
                    .withCardOnBattlefield(2, "Crafty Pathmage")       // P2: 1st Wizard
                    .withCardOnBattlefield(2, "Embermage Goblin")      // P2: 2nd Wizard
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UNTAP)
                    .build()

                // Advance to upkeep
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)

                // Pass priority to let the trigger resolve
                var iterations = 0
                while (iterations < 50) {
                    val p = game.state.priorityPlayerId ?: break
                    game.execute(PassPriority(p))
                    iterations++
                    // Stop once we've moved past upkeep or a floating effect appears
                    if (game.state.floatingEffects.any {
                        it.effect.modification is SerializableModification.ChangeController
                    }) break
                }

                withClue("Should have a ChangeController floating effect") {
                    game.state.floatingEffects.any {
                        it.effect.modification is SerializableModification.ChangeController
                    } shouldBe true
                }

                val controlEffect = game.state.floatingEffects.first {
                    it.effect.modification is SerializableModification.ChangeController
                }
                val changeController = controlEffect.effect.modification as SerializableModification.ChangeController

                withClue("Control should transfer to Player 2") {
                    changeController.newControllerId shouldBe game.player2Id
                }
            }

            test("no control change when Wizard counts are tied") {
                // Both players have 1 Wizard → tied, no change
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Thoughtbound Primoc")
                    .withCardOnBattlefield(1, "Crafty Pathmage")       // P1: 1 Wizard
                    .withCardOnBattlefield(2, "Crafty Pathmage")       // P2: 1 Wizard
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UNTAP)
                    .build()

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)

                // Pass through the upkeep
                var iterations = 0
                while (iterations < 50) {
                    val p = game.state.priorityPlayerId ?: break
                    game.execute(PassPriority(p))
                    iterations++
                    if (game.state.phase != Phase.BEGINNING || game.state.step != Step.UPKEEP) break
                }

                withClue("Should NOT have any ChangeController floating effects (tied Wizard count)") {
                    game.state.floatingEffects.none {
                        it.effect.modification is SerializableModification.ChangeController
                    } shouldBe true
                }
            }

            test("no control change when no player controls Wizards") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Thoughtbound Primoc")
                    .withCardOnBattlefield(1, "Grizzly Bears")         // Not a Wizard
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UNTAP)
                    .build()

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)

                var iterations = 0
                while (iterations < 50) {
                    val p = game.state.priorityPlayerId ?: break
                    game.execute(PassPriority(p))
                    iterations++
                    if (game.state.phase != Phase.BEGINNING || game.state.step != Step.UPKEEP) break
                }

                withClue("Should NOT have any ChangeController floating effects (no Wizards)") {
                    game.state.floatingEffects.none {
                        it.effect.modification is SerializableModification.ChangeController
                    } shouldBe true
                }
            }

            test("no control change when controller already has the most Wizards") {
                // P1 has 2 Wizards, P2 has 1 → P1 already controls Primoc, no change needed
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Thoughtbound Primoc")
                    .withCardOnBattlefield(1, "Crafty Pathmage")       // P1: 1st Wizard
                    .withCardOnBattlefield(1, "Embermage Goblin")      // P1: 2nd Wizard
                    .withCardOnBattlefield(2, "Crafty Pathmage")       // P2: 1 Wizard
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UNTAP)
                    .build()

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)

                var iterations = 0
                while (iterations < 50) {
                    val p = game.state.priorityPlayerId ?: break
                    game.execute(PassPriority(p))
                    iterations++
                    if (game.state.phase != Phase.BEGINNING || game.state.step != Step.UPKEEP) break
                }

                withClue("Should NOT have any ChangeController floating effects (controller already has most)") {
                    game.state.floatingEffects.none {
                        it.effect.modification is SerializableModification.ChangeController
                    } shouldBe true
                }
            }
        }
    }
}
