package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.gameserver.session.GameSession
import com.wingedsheep.gameserver.session.PlayerSession
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.collections.shouldContain
import io.mockk.every
import io.mockk.mockk
import org.springframework.web.socket.WebSocketSession

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

        context("Mystic Denial appears in legal actions") {
            test("legal actions include Mystic Denial when opponent's creature spell is on stack") {
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

                // Player 1 passes priority, now Player 2 has priority
                game.execute(PassPriority(game.player1Id))

                // Create a GameSession and inject the current state
                val session = GameSession(cardRegistry = cardRegistry)
                val mockWs1 = mockk<WebSocketSession>(relaxed = true) { every { id } returns "ws1" }
                val mockWs2 = mockk<WebSocketSession>(relaxed = true) { every { id } returns "ws2" }
                val player1Session = PlayerSession(mockWs1, game.player1Id, "Player1")
                val player2Session = PlayerSession(mockWs2, game.player2Id, "Player2")
                session.injectStateForTesting(
                    game.state,
                    mapOf(game.player1Id to player1Session, game.player2Id to player2Session)
                )

                // Verify Player 2 has priority
                withClue("Player 2 should have priority") {
                    game.state.priorityPlayerId shouldBe game.player2Id
                }

                // Get legal actions for Player 2
                val legalActions = session.getLegalActions(game.player2Id)

                // Verify Mystic Denial is in the legal actions
                val mysticDenialAction = legalActions.find { it.description == "Cast Mystic Denial" }
                withClue("Legal actions should include 'Cast Mystic Denial'") {
                    mysticDenialAction.shouldNotBeNull()
                }

                // Verify the legal action has valid spell targets (the creature spell on stack)
                withClue("Mystic Denial action should require targets") {
                    mysticDenialAction!!.requiresTargets shouldBe true
                }
                withClue("Mystic Denial action should have valid targets (the spell on stack)") {
                    mysticDenialAction!!.validTargets.shouldNotBeNull()
                    mysticDenialAction.validTargets!!.size shouldBe 1
                }

                // Verify the target is Grizzly Bears spell on the stack
                val spellOnStack = game.state.stack.first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Grizzly Bears"
                }
                withClue("Valid target should be the Grizzly Bears spell on the stack") {
                    mysticDenialAction!!.validTargets!! shouldContain spellOnStack
                }
            }

            test("legal actions do NOT include Mystic Denial when no opponent spell on stack") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(2, "Mystic Denial")
                    .withLandsOnBattlefield(2, "Island", 3)
                    .withActivePlayer(2)
                    .withPriorityPlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Create a GameSession and inject the state
                val session = GameSession(cardRegistry = cardRegistry)
                val mockWs1 = mockk<WebSocketSession>(relaxed = true) { every { id } returns "ws1" }
                val mockWs2 = mockk<WebSocketSession>(relaxed = true) { every { id } returns "ws2" }
                val player1Session = PlayerSession(mockWs1, game.player1Id, "Player1")
                val player2Session = PlayerSession(mockWs2, game.player2Id, "Player2")
                session.injectStateForTesting(
                    game.state,
                    mapOf(game.player1Id to player1Session, game.player2Id to player2Session)
                )

                // Get legal actions for Player 2
                val legalActions = session.getLegalActions(game.player2Id)

                // Verify Mystic Denial is NOT in the legal actions (no opponent spell on stack)
                val mysticDenialAction = legalActions.find { it.description == "Cast Mystic Denial" }
                withClue("Legal actions should NOT include 'Cast Mystic Denial' when no opponent spell on stack") {
                    mysticDenialAction shouldBe null
                }
            }

            test("legal actions include Mystic Denial when responding on opponent's turn") {
                // This tests the scenario where:
                // - It's Player 2's turn (Player 2 is active)
                // - Player 2 casts a creature spell
                // - Player 1 (non-active player) should be able to cast Mystic Denial in response
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Mystic Denial")  // Player 1 has the counterspell
                    .withCardInHand(2, "Grizzly Bears")   // Player 2 will cast the creature
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withLandsOnBattlefield(2, "Forest", 2)
                    .withActivePlayer(2)  // It's Player 2's turn
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Player 2 (active player) casts Grizzly Bears
                val castResult = game.castSpell(2, "Grizzly Bears")
                withClue("Grizzly Bears should be cast successfully: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Player 2 passes priority, now Player 1 has priority
                game.execute(PassPriority(game.player2Id))

                // Create a GameSession and inject the current state
                val session = GameSession(cardRegistry = cardRegistry)
                val mockWs1 = mockk<WebSocketSession>(relaxed = true) { every { id } returns "ws1" }
                val mockWs2 = mockk<WebSocketSession>(relaxed = true) { every { id } returns "ws2" }
                val player1Session = PlayerSession(mockWs1, game.player1Id, "Player1")
                val player2Session = PlayerSession(mockWs2, game.player2Id, "Player2")
                session.injectStateForTesting(
                    game.state,
                    mapOf(game.player1Id to player1Session, game.player2Id to player2Session)
                )

                // Verify Player 1 has priority
                withClue("Player 1 should have priority") {
                    game.state.priorityPlayerId shouldBe game.player1Id
                }

                // Get legal actions for Player 1
                val legalActions = session.getLegalActions(game.player1Id)

                // Verify Mystic Denial is in the legal actions
                val mysticDenialAction = legalActions.find { it.description == "Cast Mystic Denial" }
                withClue("Legal actions should include 'Cast Mystic Denial' when opponent's spell is on stack") {
                    mysticDenialAction.shouldNotBeNull()
                }

                // Verify the legal action has valid spell targets
                withClue("Mystic Denial action should require targets") {
                    mysticDenialAction!!.requiresTargets shouldBe true
                }
                withClue("Mystic Denial action should have valid targets (the spell on stack)") {
                    mysticDenialAction!!.validTargets.shouldNotBeNull()
                    mysticDenialAction.validTargets!!.size shouldBe 1
                }

                // Verify the target is Grizzly Bears spell on the stack
                val spellOnStack = game.state.stack.first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Grizzly Bears"
                }
                withClue("Valid target should be the Grizzly Bears spell on the stack") {
                    mysticDenialAction!!.validTargets!! shouldContain spellOnStack
                }
            }
        }
    }
}
