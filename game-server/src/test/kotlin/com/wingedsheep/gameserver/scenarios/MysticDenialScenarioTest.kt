package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.nulls.shouldNotBeNull

/**
 * Scenario tests for Mystic Denial.
 *
 * Card reference:
 * - Mystic Denial ({1}{U}{U}): Instant
 *   "Cast this spell only after an opponent casts a creature or sorcery spell.
 *    Counter target creature or sorcery spell."
 */
class MysticDenialScenarioTest : ScenarioTestBase() {

    init {
        context("Mystic Denial cast restriction") {
            test("cannot be cast when no opponent spell is on the stack") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Mystic Denial")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Try to cast Mystic Denial with no spell on the stack
                val result = game.castSpell(1, "Mystic Denial")
                withClue("Mystic Denial should not be castable without an opponent spell on the stack") {
                    result.error.shouldNotBeNull()
                }
            }

            test("can be cast in response to opponent's creature spell") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInHand(2, "Mystic Denial")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withLandsOnBattlefield(2, "Island", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Player 1 casts Grizzly Bears
                val castResult = game.castSpell(1, "Grizzly Bears")
                withClue("Grizzly Bears should be cast successfully: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Player 1 passes priority
                game.execute(PassPriority(game.player1Id))

                // Find the spell on the stack to target
                val spellOnStack = game.state.stack.first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Grizzly Bears"
                }

                // Player 2 casts Mystic Denial targeting the creature spell
                val hand = game.state.getHand(game.player2Id)
                val mysticDenialId = hand.first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Mystic Denial"
                }
                val counterResult = game.execute(
                    CastSpell(
                        game.player2Id,
                        mysticDenialId,
                        listOf(ChosenTarget.Spell(spellOnStack))
                    )
                )
                withClue("Mystic Denial should be cast successfully: ${counterResult.error}") {
                    counterResult.error shouldBe null
                }

                // Resolve the entire stack
                var iterations = 0
                while (game.state.stack.isNotEmpty() && iterations < 20) {
                    val priorityPlayer = game.state.priorityPlayerId ?: break
                    val r = game.execute(PassPriority(priorityPlayer))
                    if (r.error != null) break
                    iterations++
                }

                // Grizzly Bears should be countered (in graveyard)
                withClue("Grizzly Bears should be in player 1's graveyard") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                }
                withClue("Grizzly Bears should not be on the battlefield") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                }
            }
            test("can be cast in response to opponent's sorcery spell") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Temporary Truce")
                    .withCardInHand(2, "Mystic Denial")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withLandsOnBattlefield(2, "Island", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Player 1 casts Temporary Truce (sorcery)
                val castResult = game.castSpell(1, "Temporary Truce")
                withClue("Temporary Truce should be cast successfully: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Player 1 passes priority
                game.execute(PassPriority(game.player1Id))

                // Find the spell on the stack to target
                val spellOnStack = game.state.stack.first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Temporary Truce"
                }

                // Player 2 casts Mystic Denial targeting the sorcery spell
                val hand = game.state.getHand(game.player2Id)
                val mysticDenialId = hand.first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Mystic Denial"
                }
                val counterResult = game.execute(
                    CastSpell(
                        game.player2Id,
                        mysticDenialId,
                        listOf(ChosenTarget.Spell(spellOnStack))
                    )
                )
                withClue("Mystic Denial should be cast successfully: ${counterResult.error}") {
                    counterResult.error shouldBe null
                }

                // Resolve the entire stack
                var iterations = 0
                while (game.state.stack.isNotEmpty() && iterations < 20) {
                    val priorityPlayer = game.state.priorityPlayerId ?: break
                    val r = game.execute(PassPriority(priorityPlayer))
                    if (r.error != null) break
                    iterations++
                }

                // Temporary Truce should be countered (in graveyard)
                withClue("Temporary Truce should be in player 1's graveyard") {
                    game.isInGraveyard(1, "Temporary Truce") shouldBe true
                }
            }
        }
    }
}
