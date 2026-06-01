package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.player.CardsLeftGraveyardThisTurnComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Essence Anchor (TDM #44) — {2}{U} Artifact.
 *
 * "At the beginning of your upkeep, surveil 1.
 *  {T}: Create a 2/2 black Zombie Druid creature token. Activate only during your turn
 *   and only if a card left your graveyard this turn."
 *
 * Verifies:
 *  - The tap ability is gated: with no card having left the graveyard this turn it is not a
 *    legal action.
 *  - Once a card has left the graveyard this turn (tracked via
 *    [CardsLeftGraveyardThisTurnComponent]) on the controller's turn, activating creates a
 *    2/2 black Zombie Druid token.
 */
class EssenceAnchorScenarioTest : ScenarioTestBase() {

    init {
        context("Essence Anchor token ability") {

            test("cannot activate when no card left the graveyard this turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Essence Anchor")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val anchorId = game.findPermanent("Essence Anchor")!!
                val ability = cardRegistry.getCard("Essence Anchor")!!.script.activatedAbilities.first()

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = anchorId,
                        abilityId = ability.id,
                    )
                )
                withClue("No card left the graveyard this turn → activation must be rejected") {
                    (result.error != null) shouldBe true
                }
                withClue("No token should have been created") {
                    game.isOnBattlefield("Zombie Druid") shouldBe false
                }
            }

            test("creates a 2/2 black Zombie Druid token once a card has left the graveyard this turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Essence Anchor")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Simulate that a card has left Player1's graveyard this turn.
                game.state = game.state.updateEntity(game.player1Id) {
                    it.with(CardsLeftGraveyardThisTurnComponent(1))
                }

                val anchorId = game.findPermanent("Essence Anchor")!!
                val ability = cardRegistry.getCard("Essence Anchor")!!.script.activatedAbilities.first()

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = anchorId,
                        abilityId = ability.id,
                    )
                )
                withClue("A card left the graveyard this turn on the controller's turn → activation succeeds: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                withClue("Essence Anchor creates a Zombie Druid token") {
                    game.isOnBattlefield("Zombie Druid") shouldBe true
                }
            }
        }
    }
}
