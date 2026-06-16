package com.wingedsheep.gameserver.controller

import com.wingedsheep.gameserver.ai.AiGameManager
import com.wingedsheep.gameserver.repository.GameRepository
import com.wingedsheep.gameserver.session.SessionRegistry
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/**
 * Verifies that `POST /api/dev/scenarios` with `aiPlayer = 2` wires the AI seat correctly:
 *
 *  - response advertises only the human's token, the AI's token is hidden behind "(AI)"
 *  - both seats end up in `GameSession.players` (so `broadcastStateUpdate` reaches the AI)
 *  - `AiGameManager.isAiPlayer(...)` reports the AI seat
 *
 * Dev scenarios always run with the in-process engine AI — no LLM, no API key needed.
 *
 * Uses raw JSON + kotlinx `JsonElement` parsing instead of Spring's `TestRestTemplate` /
 * `AutoConfigureMockMvc`, both of which were dropped in Spring Boot 4.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "game.dev-endpoints.enabled=true",
        "game.ai.enabled=true",
        "game.ai.mode=engine",
    ],
)
class DevScenarioAiTest(
    @Autowired private val gameRepository: GameRepository,
    @Autowired private val sessionRegistry: SessionRegistry,
    @Autowired private val aiGameManager: AiGameManager,
    @LocalServerPort private val port: Int,
) : FunSpec({

    val http = HttpClient.newHttpClient()
    val json = Json { ignoreUnknownKeys = true }

    fun post(body: String): HttpResponse<String> {
        val req = HttpRequest.newBuilder(URI.create("http://localhost:$port/api/dev/scenarios"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()
        return http.send(req, HttpResponse.BodyHandlers.ofString())
    }

    fun JsonObject.s(key: String): String = this[key]!!.jsonPrimitive.content

    test("aiPlayer=2 fills seat 2 with engine AI and registers it everywhere downstream") {
        val response = post(
            """
            {
              "player1Name": "Human",
              "player2Name": "Bot",
              "player1": {
                "lifeTotal": 20,
                "battlefield": [{"name": "Forest"}],
                "library": ["Forest", "Forest"]
              },
              "player2": {
                "lifeTotal": 20,
                "battlefield": [{"name": "Mountain"}],
                "library": ["Mountain", "Mountain"]
              },
              "aiPlayer": 2
            }
            """.trimIndent()
        )
        response.statusCode() shouldBe 200

        val body = json.parseToJsonElement(response.body()).jsonObject
        val sessionId = body.s("sessionId")
        val player1 = body["player1"]!!.jsonObject
        val player2 = body["player2"]!!.jsonObject

        player1.s("token") shouldBe "p1"
        player2.s("token") shouldBe "(AI)"
        body.s("message") shouldContain "you are Player 1"
        body.s("message") shouldStartWith "Scenario created vs AI."

        val player1Id = EntityId.of(player1.s("playerId"))
        val player2Id = EntityId.of(player2.s("playerId"))

        // AI is associated into GameSession.players right away (so broadcastStateUpdate
        // reaches it); the human seat only enters players when their WebSocket connects.
        val gameSession = gameRepository.findById(sessionId).shouldNotBeNull()
        gameSession.getPlayers().map { it.playerId } shouldBe listOf(player2Id)

        // Human identity is pre-registered with its returned token so the WS handshake
        // resolves it; the AI has its own synthetic token registered via SessionRegistry.register.
        sessionRegistry.getIdentityByToken("p1")?.playerId shouldBe player1Id
        sessionRegistry.getAllIdentities()
            .singleOrNull { it.isAi && it.playerId == player2Id }
            .shouldNotBeNull()

        // The AI seat is tracked by AiGameManager so isAiPlayer + cleanupGame work.
        aiGameManager.isAiPlayer(player2Id) shouldBe true
        aiGameManager.isAiPlayer(player1Id) shouldBe false
        aiGameManager.hasAiPlayer(sessionId) shouldBe true
    }

    test("aiPlayer=5 is rejected with a 400") {
        val response = post("""{"aiPlayer": 5}""")
        response.statusCode() shouldBe 400
        val body = json.parseToJsonElement(response.body()).jsonObject
        body.s("message") shouldContain "aiPlayer must be 1 or 2"
    }
})
