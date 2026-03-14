package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.state.components.player.DamageBonusComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for The Flame of Keld — specifically the Chapter III damage bonus effect.
 *
 * Card reference:
 * - The Flame of Keld ({1}{R}): Enchantment — Saga
 *   I — Discard your hand.
 *   II — Draw two cards.
 *   III — If a red source you control would deal damage to a permanent or player this turn,
 *          it deals that much damage plus 2 instead.
 */
class TheFlameOfKeldScenarioTest : ScenarioTestBase() {

    init {
        context("The Flame of Keld - Chapter III damage bonus") {
            test("red spell deals +2 damage when damage bonus is active") {
                // Set up a state where the player has the damage bonus component
                // (simulating after Chapter III has resolved)
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Shock") // instant: deal 2 damage to any target
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Manually add the DamageBonusComponent to simulate Chapter III resolving
                val player1Id = game.state.turnOrder[0]
                game.state = game.state.updateEntity(player1Id) { container ->
                    container.with(
                        DamageBonusComponent(
                            bonusAmount = 2,
                            sourceFilter = com.wingedsheep.sdk.scripting.events.SourceFilter.HasColor(
                                com.wingedsheep.sdk.core.Color.RED
                            )
                        )
                    )
                }

                val startingLife = game.getLifeTotal(2)

                game.castSpellTargetingPlayer(1, "Shock", 2)
                game.resolveStack()

                withClue("Shock should deal 2 + 2 = 4 damage with red damage bonus") {
                    game.getLifeTotal(2) shouldBe startingLife - 4
                }
            }

            test("non-red source does not get bonus damage") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Glory Seeker") // 2/2 white creature
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Add the DamageBonusComponent for red sources
                val player1Id = game.state.turnOrder[0]
                game.state = game.state.updateEntity(player1Id) { container ->
                    container.with(
                        DamageBonusComponent(
                            bonusAmount = 2,
                            sourceFilter = com.wingedsheep.sdk.scripting.events.SourceFilter.HasColor(
                                com.wingedsheep.sdk.core.Color.RED
                            )
                        )
                    )
                }

                val startingLife = game.getLifeTotal(2)

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Glory Seeker" to 2))
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("White creature combat damage should NOT get +2 bonus") {
                    game.getLifeTotal(2) shouldBe startingLife - 2
                }
            }

            test("red creature gets bonus combat damage") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Ghitu Lavarunner") // 1/2 red creature
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Add the DamageBonusComponent for red sources
                val player1Id = game.state.turnOrder[0]
                game.state = game.state.updateEntity(player1Id) { container ->
                    container.with(
                        DamageBonusComponent(
                            bonusAmount = 2,
                            sourceFilter = com.wingedsheep.sdk.scripting.events.SourceFilter.HasColor(
                                com.wingedsheep.sdk.core.Color.RED
                            )
                        )
                    )
                }

                val startingLife = game.getLifeTotal(2)

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Ghitu Lavarunner" to 2))
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("Red creature combat damage should be 1 + 2 = 3 with bonus") {
                    game.getLifeTotal(2) shouldBe startingLife - 3
                }
            }

            test("damage bonus is cleaned up at end of turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val player1Id = game.state.turnOrder[0]
                game.state = game.state.updateEntity(player1Id) { container ->
                    container.with(
                        DamageBonusComponent(
                            bonusAmount = 2,
                            sourceFilter = com.wingedsheep.sdk.scripting.events.SourceFilter.HasColor(
                                com.wingedsheep.sdk.core.Color.RED
                            )
                        )
                    )
                }

                // Verify the component is present
                withClue("DamageBonusComponent should be present before end of turn") {
                    game.state.getEntity(player1Id)?.get<DamageBonusComponent>() shouldNotBe null
                }

                // Pass to end step (cleanup happens here), then to next turn
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

                withClue("DamageBonusComponent should be cleaned up after end of turn") {
                    game.state.getEntity(player1Id)?.get<DamageBonusComponent>() shouldBe null
                }
            }
        }
    }
}
