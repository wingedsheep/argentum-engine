package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Planar Guide.
 *
 * Card reference:
 * - Planar Guide ({W}): Creature — Human Cleric 1/1
 *   "{3}{W}, Exile Planar Guide: Exile all creatures. At the beginning of the next end step,
 *   return those cards to the battlefield under their owners' control."
 */
class PlanarGuideScenarioTest : ScenarioTestBase() {

    private fun ScenarioTestBase.TestGame.isInExile(playerNumber: Int, cardName: String): Boolean {
        val playerId = if (playerNumber == 1) player1Id else player2Id
        return state.getExile(playerId).any { entityId ->
            state.getEntity(entityId)?.get<CardComponent>()?.name == cardName
        }
    }

    init {
        context("Planar Guide") {

            test("exile all creatures and return at end step under owners' control") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Planar Guide")
                    .withCardOnBattlefield(1, "Glory Seeker")
                    .withCardOnBattlefield(2, "Elvish Warrior")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Activate Planar Guide's ability
                val guideId = game.findPermanent("Planar Guide")!!
                val cardDef = cardRegistry.getCard("Planar Guide")!!
                val ability = cardDef.script.activatedAbilities.first()

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = guideId,
                        abilityId = ability.id
                    )
                )
                withClue("Activation should succeed") {
                    result.error shouldBe null
                }

                // Resolve the ability on the stack
                game.resolveStack()

                // All creatures should be exiled (Planar Guide already exiled as cost)
                withClue("Planar Guide should be in exile (cost)") {
                    game.isOnBattlefield("Planar Guide") shouldBe false
                    game.isInExile(1, "Planar Guide") shouldBe true
                }
                withClue("Glory Seeker should be in exile") {
                    game.isOnBattlefield("Glory Seeker") shouldBe false
                    game.isInExile(1, "Glory Seeker") shouldBe true
                }
                withClue("Elvish Warrior should be in exile") {
                    game.isOnBattlefield("Elvish Warrior") shouldBe false
                    game.isInExile(2, "Elvish Warrior") shouldBe true
                }

                // Advance to end step - delayed trigger returns creatures
                game.passUntilPhase(Phase.ENDING, Step.END)
                if (game.state.stack.isNotEmpty()) {
                    game.resolveStack()
                }

                // Creatures return under owners' control
                withClue("Glory Seeker should be back on battlefield") {
                    game.isOnBattlefield("Glory Seeker") shouldBe true
                }
                withClue("Elvish Warrior should be back on battlefield") {
                    game.isOnBattlefield("Elvish Warrior") shouldBe true
                }
                // Planar Guide should NOT return (it was exiled as a cost, not by the effect)
                withClue("Planar Guide should NOT return (exiled as cost, not linked)") {
                    game.isOnBattlefield("Planar Guide") shouldBe false
                }
            }

            test("exile self as cost moves Planar Guide to exile") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Planar Guide")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val guideId = game.findPermanent("Planar Guide")!!
                val cardDef = cardRegistry.getCard("Planar Guide")!!
                val ability = cardDef.script.activatedAbilities.first()

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = guideId,
                        abilityId = ability.id
                    )
                )

                // After activation (before resolution), Planar Guide should already be in exile
                withClue("Planar Guide should be exiled as cost") {
                    game.isOnBattlefield("Planar Guide") shouldBe false
                    game.isInExile(1, "Planar Guide") shouldBe true
                }
            }
        }
    }
}
