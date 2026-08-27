package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.engineSerializersModule
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.gameserver.protocol.ClientMessage
import com.wingedsheep.gameserver.protocol.ServerMessage
import com.wingedsheep.gameserver.session.GameSession
import com.wingedsheep.gameserver.session.PlayerSession
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.springframework.web.socket.WebSocketSession

/**
 * The server half of the cost preview: `GameSession.previewCost` answers under the state lock,
 * applies the same seat authorization as `executeAction`, never mutates the game, and the two
 * protocol messages round-trip through the wire format.
 */
class CostPreviewSessionTest : ScenarioTestBase() {

    init {
        cardRegistry.register(
            CardDefinition.creature(
                name = "Preview Bear",
                manaCost = ManaCost.parse("{1}{G}"),
                subtypes = setOf(Subtype("Bear")),
                power = 2,
                toughness = 2
            )
        )

        test("prices a draft for the acting seat and leaves the game untouched") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Preview Bear")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val session = newSession(game)
            val p1 = game.player1Id
            val bear = cardInHand(game, p1, "Preview Bear")
            val before = session.getStateForTesting()

            val preview = session.previewCost(p1, CastSpell(playerId = p1, cardId = bear))
            preview.manaCostString shouldBe "{1}{G}"
            preview.genericRemaining shouldBe 1
            preview.affordable shouldBe true
            preview.error shouldBe null
            preview.autoTapPreview.shouldNotBeNull().size shouldBe 2

            session.getStateForTesting() shouldBe before
        }

        test("a seat may not preview for another player it doesn't control") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Preview Bear")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val session = newSession(game)
            val bear = cardInHand(game, game.player1Id, "Preview Bear")

            val preview = session.previewCost(game.player2Id, CastSpell(playerId = game.player1Id, cardId = bear))
            preview.affordable shouldBe false
            preview.error.shouldNotBeNull() shouldContain "Not authorized"
        }

        test("the request and reply round-trip through the wire format") {
            val json = Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
                classDiscriminator = "type"
                serializersModule = engineSerializersModule
            }
            val request: ClientMessage = ClientMessage.PreviewCost(
                action = CastSpell(playerId = EntityId.of("p1"), cardId = EntityId.of("c1"), xValue = 2),
                requestId = "cp-7",
            )
            val requestJson = json.encodeToString(request)
            requestJson shouldContain "\"type\":\"previewCost\""
            json.decodeFromString<ClientMessage>(requestJson) shouldBe request

            val reply: ServerMessage = ServerMessage.CostPreview(
                requestId = "cp-7",
                manaCostString = "{2}{G}",
                genericRemaining = 2,
                xValue = 2,
                affordable = false,
                error = "Not enough mana to cast this spell",
                autoTapPreview = null,
            )
            val replyJson = json.encodeToString(reply)
            replyJson shouldContain "\"type\":\"costPreview\""
            json.decodeFromString<ServerMessage>(replyJson).shouldBeInstanceOf<ServerMessage.CostPreview>() shouldBe reply
        }
    }

    private fun cardInHand(game: TestGame, playerId: EntityId, name: String): EntityId =
        game.state.getHand(playerId).first { game.state.getEntity(it)?.get<CardComponent>()?.name == name }

    private fun newSession(game: TestGame): GameSession {
        val session = GameSession(cardRegistry = cardRegistry)
        val ws1 = mockk<WebSocketSession>(relaxed = true) { every { id } returns "ws1" }
        val ws2 = mockk<WebSocketSession>(relaxed = true) { every { id } returns "ws2" }
        session.injectStateForTesting(
            game.state,
            mapOf(
                game.player1Id to PlayerSession(ws1, game.player1Id, "Player1"),
                game.player2Id to PlayerSession(ws2, game.player2Id, "Player2")
            )
        )
        return session
    }
}
