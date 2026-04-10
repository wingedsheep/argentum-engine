package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test verifying that the 1/1 Offspring token of Steampath Charger inherits
 * the "when this creature dies, it deals 1 damage to target player" triggered ability.
 *
 * The token is created as a copy of the source card (same cardDefinitionId), so the
 * engine resolves its triggered abilities from the shared card definition.
 */
class SteampathChargerScenarioTest : ScenarioTestBase() {

    init {
        context("Steampath Charger — Offspring token dies trigger") {
            test("1/1 Offspring token deals 1 damage when it dies") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Steampath Charger")
                    .withCardInHand(1, "Scorching Spear")
                    .withLandsOnBattlefield(1, "Mountain", 5) // {1}{R}+{2} kicker + {R} spear
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Steampath Charger with Offspring (modeled as kicker)
                val playerId = game.player1Id
                val hand = game.state.getHand(playerId)
                val cardId = hand.find { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Steampath Charger"
                }!!

                val castResult = game.execute(CastSpell(playerId, cardId, wasKicked = true))
                withClue("Kicked Steampath Charger cast should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack() // SC resolves → ETB trigger fires → creates 1/1 token

                // Find the token (marked with TokenComponent) copy of Steampath Charger
                val tokenId = game.state.getBattlefield().find { entityId ->
                    val container = game.state.getEntity(entityId) ?: return@find false
                    container.has<TokenComponent>() &&
                        container.get<CardComponent>()?.name == "Steampath Charger"
                }
                withClue("Offspring should create a token copy of Steampath Charger") {
                    (tokenId != null) shouldBe true
                }

                // Scorching Spear the token for 1 to kill it (token is 1/1)
                val spearResult = game.castSpell(1, "Scorching Spear", tokenId)
                withClue("Scorching Spear cast should succeed: ${spearResult.error}") {
                    spearResult.error shouldBe null
                }
                game.resolveStack()

                withClue("Token should be dead after taking 1 damage") {
                    game.state.getBattlefield().contains(tokenId!!) shouldBe false
                }

                // The token's dies trigger should now be pending target selection
                withClue("Token's dies trigger should be pending target selection") {
                    game.hasPendingDecision() shouldBe true
                }

                // Target the opponent (player 2)
                game.selectTargets(listOf(game.player2Id))
                game.resolveStack()

                withClue("Opponent should be at 19 life from the token's dies trigger") {
                    game.getLifeTotal(2) shouldBe 19
                }
            }
        }
    }
}
