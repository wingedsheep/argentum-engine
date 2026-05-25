package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.view.LegalActionEnricher
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.gameserver.priority.AutoPassManager
import com.wingedsheep.gameserver.session.GameSession
import com.wingedsheep.gameserver.session.PlayerSession
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import org.springframework.web.socket.WebSocketSession

/**
 * Regression: when player 1 casts an instant whose chosen mode targets player 2
 * (e.g. Ashling's Command "deals 2 damage to each creature target player controls"),
 * player 2 must get a priority window to respond — they can activate abilities or
 * cast instants in response. The Arena-style auto-pass logic must STOP for player 2
 * because an opponent's non-permanent spell is on the stack.
 *
 * Card setup mirrors the user-reported bug:
 *   - Player 1 (Alice) casts Ashling's Command, picks the damage mode targeting Bob.
 *   - Player 2 (Bob) controls Timid Shieldbearer ({4}{W}: creatures get +1/+1) and
 *     Morcant's Loyalist, with 5 Plains untapped.
 *   - Both Bob's creatures die without the pump (2/2 and 3/2 vs 2 damage); both
 *     survive with the pump (3/3 and 4/3). Bob has a strong reason to activate.
 */
class AshlingsCommandPriorityScenarioTest : ScenarioTestBase() {

    private val autoPassManager = AutoPassManager()
    private val manaSolver = ManaSolver(cardRegistry)
    private val legalActionEnumerator = LegalActionEnumerator.create(cardRegistry, manaSolver)
    private val legalActionEnricher = LegalActionEnricher(manaSolver, cardRegistry)

    private fun TestGame.chooseMode(optionIndex: Int) {
        val decision = getPendingDecision()
        decision.shouldNotBeNull()
        decision.shouldBeInstanceOf<ChooseOptionDecision>()
        submitDecision(OptionChosenResponse(decision.id, optionIndex))
    }

    init {
        test("Bob gets priority window to respond to Ashling's Command damage mode") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardInHand(1, "Ashling's Command")
                .withLandsOnBattlefield(1, "Island", 3)
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withCardOnBattlefield(2, "Timid Shieldbearer")
                .withCardOnBattlefield(2, "Morcant's Loyalist")
                .withLandsOnBattlefield(2, "Plains", 5)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bob = game.player2Id

            game.castSpell(1, "Ashling's Command")

            // Alice controls no Elementals → mode 0 is filtered (rule 700.2a).
            // Offered indices: [1, 2, 3]. Mode 2 (damage) is at offered position 1.
            game.chooseMode(1)
            // Remaining: [1, 3]. Pick mode 1 (draw) at offered position 0.
            game.chooseMode(0)

            // Per-mode targets in pick order: mode 2 (damage) → Bob, mode 1 (draw) → Bob.
            game.selectTargets(listOf(bob))
            game.selectTargets(listOf(bob))

            withClue("Ashling's Command should be on the stack") {
                game.state.stack.isNotEmpty() shouldBe true
            }
            withClue("Caster (Alice) holds priority right after the cast resolves") {
                game.state.priorityPlayerId shouldBe game.player1Id
            }

            // Alice passes priority — own spell on top, AutoPass would also do this in-game.
            game.passPriority()

            withClue("Priority should move to Bob with the spell still on the stack") {
                game.state.priorityPlayerId shouldBe bob
                game.state.stack.isNotEmpty() shouldBe true
            }

            val bobActions = legalActionEnricher.enrich(
                legalActionEnumerator.enumerate(game.state, bob),
                game.state, bob
            )

            withClue("Bob's Timid Shieldbearer activated ability should be a legal action") {
                val hasShieldbearerAbility = bobActions.any { action ->
                    action.actionType == "ActivateAbility" &&
                        !action.isManaAbility &&
                        action.description.contains("Creatures you control get +1/+1")
                }
                hasShieldbearerAbility shouldBe true
            }

            withClue("AutoPassManager must STOP for Bob — opponent's instant is on the stack") {
                autoPassManager.shouldAutoPass(game.state, bob, bobActions) shouldBe false
            }
        }

        test("GameSession auto-pass loop stops on Bob's priority with Ashling's Command on stack") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardInHand(1, "Ashling's Command")
                .withLandsOnBattlefield(1, "Island", 3)
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withCardOnBattlefield(2, "Timid Shieldbearer")
                .withCardOnBattlefield(2, "Morcant's Loyalist")
                .withLandsOnBattlefield(2, "Plains", 5)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bob = game.player2Id

            game.castSpell(1, "Ashling's Command")
            game.chooseMode(1)  // mode 2 (damage) at offered-position 1
            game.chooseMode(0)  // mode 1 (draw) at offered-position 0
            game.selectTargets(listOf(bob))
            game.selectTargets(listOf(bob))

            val session = newSession(game)

            // Drive the auto-pass loop the way GamePlayHandler does. The loop should
            // give priority to Bob and stop there — an opponent's instant is on the
            // stack, so Bob is entitled to a response window.
            var loopCount = 0
            val maxLoops = 50
            while (loopCount < maxLoops) {
                val autoPassPlayer = session.getAutoPassPlayer() ?: break
                session.executeAutoPass(autoPassPlayer)
                loopCount++
            }

            val finalState = session.getStateForTesting()!!

            withClue("Spell must still be on the stack — it must not have resolved without Bob's chance to respond") {
                finalState.stack.isNotEmpty() shouldBe true
            }
            withClue("Auto-pass loop must stop on Bob's priority so Bob can respond") {
                finalState.priorityPlayerId shouldBe bob
            }
        }
    }

    private fun newSession(game: TestGame): GameSession {
        val session = GameSession(cardRegistry = cardRegistry)
        val ws1 = mockk<WebSocketSession>(relaxed = true) { every { id } returns "ws1" }
        val ws2 = mockk<WebSocketSession>(relaxed = true) { every { id } returns "ws2" }
        session.injectStateForTesting(
            game.state,
            mapOf(
                game.player1Id to PlayerSession(ws1, game.player1Id, "Alice"),
                game.player2Id to PlayerSession(ws2, game.player2Id, "Bob")
            )
        )
        return session
    }
}
