package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.player.SkipNextTurnComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Lethal Vapors.
 *
 * Card reference:
 * - Lethal Vapors ({2}{B}{B}): Enchantment
 *   "Whenever a creature enters, destroy it."
 *   "{0}: Destroy Lethal Vapors. You skip your next turn. Any player may activate this ability."
 */
class LethalVaporsScenarioTest : ScenarioTestBase() {

    init {
        context("Lethal Vapors triggered ability") {

            test("entering creature is destroyed") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Lethal Vapors")
                    .withCardInHand(1, "Glory Seeker")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Glory Seeker
                game.castSpell(1, "Glory Seeker")
                game.resolveStack()

                // Lethal Vapors triggers - the ability goes on the stack and resolves
                game.resolveStack()

                withClue("Glory Seeker should be destroyed by Lethal Vapors") {
                    game.isOnBattlefield("Glory Seeker") shouldBe false
                }
                withClue("Lethal Vapors should still be on the battlefield") {
                    game.isOnBattlefield("Lethal Vapors") shouldBe true
                }
            }

            test("opponent's creature entering is also destroyed") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Lethal Vapors")
                    .withCardInHand(2, "Elvish Warrior")
                    .withLandsOnBattlefield(2, "Forest", 2)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(2, "Elvish Warrior")
                game.resolveStack()

                // Lethal Vapors triggers
                game.resolveStack()

                withClue("Elvish Warrior should be destroyed") {
                    game.isOnBattlefield("Elvish Warrior") shouldBe false
                }
            }
        }

        context("Lethal Vapors activated ability") {

            test("controller can activate to destroy and skip turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Lethal Vapors")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val vaporsId = game.findPermanent("Lethal Vapors")!!
                val cardDef = cardRegistry.getCard("Lethal Vapors")!!
                val ability = cardDef.script.activatedAbilities.first()

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = vaporsId,
                        abilityId = ability.id
                    )
                )
                withClue("Activation should succeed: ${result.error}") {
                    result.error shouldBe null
                }

                // Resolve the ability
                game.resolveStack()

                withClue("Lethal Vapors should be destroyed") {
                    game.isOnBattlefield("Lethal Vapors") shouldBe false
                }
                withClue("Player 1 should have SkipNextTurnComponent") {
                    game.state.getEntity(game.player1Id)?.has<SkipNextTurnComponent>() shouldBe true
                }
            }

            test("opponent can activate to destroy and skip their turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Lethal Vapors")
                    .withActivePlayer(1)
                    .withPriorityPlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val vaporsId = game.findPermanent("Lethal Vapors")!!
                val cardDef = cardRegistry.getCard("Lethal Vapors")!!
                val ability = cardDef.script.activatedAbilities.first()

                // Player 2 (opponent) activates the ability on Player 1's permanent
                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player2Id,
                        sourceId = vaporsId,
                        abilityId = ability.id
                    )
                )
                withClue("Opponent activation should succeed: ${result.error}") {
                    result.error shouldBe null
                }

                // Resolve the ability
                game.resolveStack()

                withClue("Lethal Vapors should be destroyed") {
                    game.isOnBattlefield("Lethal Vapors") shouldBe false
                }
                withClue("Player 2 (opponent/activating player) should have SkipNextTurnComponent") {
                    game.state.getEntity(game.player2Id)?.has<SkipNextTurnComponent>() shouldBe true
                }
                withClue("Player 1 (controller) should NOT have SkipNextTurnComponent") {
                    game.state.getEntity(game.player1Id)?.has<SkipNextTurnComponent>() shouldBe false
                }
            }
        }
    }
}
