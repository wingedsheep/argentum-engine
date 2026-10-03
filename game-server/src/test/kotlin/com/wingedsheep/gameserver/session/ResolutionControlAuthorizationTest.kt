package com.wingedsheep.gameserver.session

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Concede
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.core.SubmitDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.core.YesNoResponse
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.gameserver.protocol.ServerMessage
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import org.springframework.web.socket.WebSocketSession

class ResolutionControlAuthorizationTest : ScenarioTestBase() {
    private val probe = card("Session Resolution Control Probe") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell {
            val opponent = EffectTarget.PlayerRef(Player.AnOpponent)
            effect = Effects.Pipeline {
                run(Effects.ControlPlayerDuringResolution(opponent))
                run(Effects.May(Effects.DrawCards(1, opponent), decisionMaker = opponent, prompt = "Draw a card?"))
            }
        }
    }

    init {
        cardRegistry.register(probe)

        test("only the controller connection may answer the affected seat's live decision") {
            val (session, game) = pausedSession()
            val controller = game.player1Id
            val affected = game.player2Id
            val update = session.createStateUpdate(controller, emptyList())
                .shouldBeInstanceOf<ServerMessage.StateUpdate>()
            val decision = update.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
            val response = SubmitDecision(affected, YesNoResponse(decision.id, false))
            val before = session.getStateForTesting()!!
            before.actorFor(affected) shouldBe controller
            val actions = session.getRecordedActions()
            val messageIds = session.getLastMessageIdsForPersistence()

            session.executeClientAction(affected, response, "answer")
                .shouldBeInstanceOf<GameSession.ActionResult.Failure>()
            session.getStateForTesting() shouldBe before
            session.getRecordedActions() shouldBe actions
            session.getLastMessageIdsForPersistence() shouldBe messageIds

            session.executeClientAction(controller, response, "answer")
                .shouldBeInstanceOf<GameSession.ActionResult.Success>()
            session.getStateForTesting()!!.pendingDecision shouldBe null
            session.getRecordedActions().last().playerId shouldBe affected
        }

        test("control never permits conceding another seat and the affected seat can still concede") {
            val (session, game) = pausedSession()
            val controller = game.player1Id
            val affected = game.player2Id
            val before = session.getStateForTesting()!!
            before.actorFor(affected) shouldBe controller
            session.executeAction(controller, Concede(affected))
                .shouldBeInstanceOf<GameSession.ActionResult.Failure>()
            session.executeAction(affected, Concede(controller))
                .shouldBeInstanceOf<GameSession.ActionResult.Failure>()
            session.getStateForTesting() shouldBe before

            session.executeAction(affected, Concede(affected))
                .shouldBeInstanceOf<GameSession.ActionResult.Success>()
            session.getStateForTesting()!!.gameOver shouldBe true
            session.getWinnerId() shouldBe controller
        }
    }

    private fun pausedSession(): Pair<GameSession, TestGame> {
        val game = scenario().withPlayers().withCardInHand(1, probe.name)
            .withCardInLibrary(2, "Forest").build()
        val session = GameSession(cardRegistry = cardRegistry)
        val firstSocket = mockk<WebSocketSession>(relaxed = true) { every { id } returns "controller" }
        val secondSocket = mockk<WebSocketSession>(relaxed = true) { every { id } returns "affected" }
        session.injectStateForTesting(game.state, mapOf(
            game.player1Id to PlayerSession(firstSocket, game.player1Id, "Controller"),
            game.player2Id to PlayerSession(secondSocket, game.player2Id, "Affected"),
        ))
        session.executeAction(game.player1Id, CastSpell(game.player1Id, game.findCardsInHand(1, probe.name).single()))
            .shouldBeInstanceOf<GameSession.ActionResult.Success>()
        session.executeAction(game.player1Id, PassPriority(game.player1Id))
            .shouldBeInstanceOf<GameSession.ActionResult.Success>()
        session.executeAction(game.player2Id, PassPriority(game.player2Id))
            .shouldBeInstanceOf<GameSession.ActionResult.PausedForDecision>()
        return session to game
    }
}
