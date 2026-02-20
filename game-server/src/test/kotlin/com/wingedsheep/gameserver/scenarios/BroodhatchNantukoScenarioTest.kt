package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Broodhatch Nantuko.
 *
 * Card reference:
 * - Broodhatch Nantuko (1G): 1/1 Creature — Insect Druid
 *   Whenever Broodhatch Nantuko is dealt damage, create that many 1/1 green Insect creature tokens.
 *   Morph {2}{G}
 */
class BroodhatchNantukoScenarioTest : ScenarioTestBase() {

    /**
     * Count tokens on the battlefield with the given name controlled by a player.
     */
    private fun ScenarioTestBase.TestGame.countTokens(playerNumber: Int, tokenName: String): Int {
        val playerId = if (playerNumber == 1) player1Id else player2Id
        return state.getBattlefield().count { entityId ->
            val container = state.getEntity(entityId) ?: return@count false
            val card = container.get<CardComponent>() ?: return@count false
            val isToken = container.has<TokenComponent>()
            val controller = container.get<com.wingedsheep.engine.state.components.identity.ControllerComponent>()?.playerId
            isToken && card.name == tokenName && controller == playerId
        }
    }

    init {
        context("Broodhatch Nantuko damage trigger") {
            test("creates 2 Insect tokens when dealt 2 damage by Shock") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Broodhatch Nantuko")
                    .withCardInHand(2, "Shock")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val nantuko = game.findPermanent("Broodhatch Nantuko")!!

                // Verify no tokens initially
                withClue("Should have no Insect tokens initially") {
                    game.countTokens(1, "Insect Token") shouldBe 0
                }

                // Opponent casts Shock targeting Broodhatch Nantuko
                val castResult = game.castSpell(2, "Shock", nantuko)
                withClue("Cast should succeed") {
                    castResult.error shouldBe null
                }

                // Resolve Shock (deals 2 damage, triggering the ability)
                game.resolveStack()
                // Resolve the triggered ability
                game.resolveStack()

                // Should create 2 Insect tokens (Shock deals 2 damage)
                withClue("Should have 2 Insect tokens after taking 2 damage") {
                    game.countTokens(1, "Insect Token") shouldBe 2
                }
            }

            test("Broodhatch Nantuko dies from damage but still creates tokens") {
                // Broodhatch Nantuko is 1/1 and Shock deals 2 damage.
                // It dies from the damage but the trigger still fires because the
                // ability triggers on damage being dealt (before state-based actions).
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Broodhatch Nantuko")
                    .withCardInHand(2, "Shock")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Opponent casts Shock targeting Broodhatch Nantuko
                val nantuko = game.findPermanent("Broodhatch Nantuko")!!
                game.castSpell(2, "Shock", nantuko)
                game.resolveStack()
                game.resolveStack()

                // Broodhatch Nantuko should be in the graveyard (1 toughness, 2 damage kills it)
                withClue("Broodhatch Nantuko should be dead") {
                    game.findPermanent("Broodhatch Nantuko") shouldBe null
                }

                // Tokens should still have been created
                withClue("Should still have 2 Insect tokens even though Nantuko died") {
                    game.countTokens(1, "Insect Token") shouldBe 2
                }
            }
        }

        context("Broodhatch Nantuko combat damage trigger") {
            test("creates 2 tokens when blocking a 2/2") {
                // Broodhatch Nantuko (1/1) blocks Glory Seeker (2/2).
                // Per CR 510.1d, all 2 combat damage is assigned to the blocker.
                // Nantuko dies but trigger still fires, creating 2 Insect tokens.
                val game = scenario()
                    .withPlayers("Defender", "Attacker")
                    .withCardOnBattlefield(1, "Broodhatch Nantuko")
                    .withCardOnBattlefield(2, "Glory Seeker")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Verify no tokens initially
                withClue("Should have no Insect tokens initially") {
                    game.countTokens(1, "Insect Token") shouldBe 0
                }

                // Advance to declare attackers
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Glory Seeker" to 1))

                // Advance to declare blockers
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Broodhatch Nantuko" to listOf("Glory Seeker")))

                // Pass priority through combat damage step - trigger should fire
                var iterations = 0
                while (game.state.step != Step.END_COMBAT && iterations < 50) {
                    val p = game.state.priorityPlayerId ?: break
                    game.execute(PassPriority(p))
                    iterations++
                }

                // Broodhatch Nantuko should be dead (1/1 took lethal damage)
                withClue("Broodhatch Nantuko should be dead") {
                    game.findPermanent("Broodhatch Nantuko") shouldBe null
                }

                // Should have created 2 Insect tokens (Glory Seeker deals 2 damage)
                withClue("Should have 2 Insect tokens from combat damage trigger") {
                    game.countTokens(1, "Insect Token") shouldBe 2
                }
            }

            test("creates 3 tokens when blocking a 3/3") {
                // Broodhatch Nantuko (1/1) blocks Hill Giant (3/3).
                // All 3 combat damage is assigned to the blocker per CR 510.1d.
                // Nantuko dies but trigger still fires, creating 3 Insect tokens.
                val game = scenario()
                    .withPlayers("Defender", "Attacker")
                    .withCardOnBattlefield(1, "Broodhatch Nantuko")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                withClue("Should have no Insect tokens initially") {
                    game.countTokens(1, "Insect Token") shouldBe 0
                }

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Hill Giant" to 1))

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Broodhatch Nantuko" to listOf("Hill Giant")))

                var iterations = 0
                while (game.state.step != Step.END_COMBAT && iterations < 50) {
                    val p = game.state.priorityPlayerId ?: break
                    game.execute(PassPriority(p))
                    iterations++
                }

                withClue("Broodhatch Nantuko should be dead") {
                    game.findPermanent("Broodhatch Nantuko") shouldBe null
                }

                withClue("Should have 3 Insect tokens from combat damage trigger") {
                    game.countTokens(1, "Insect Token") shouldBe 3
                }
            }
        }
    }
}
