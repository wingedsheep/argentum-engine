package com.wingedsheep.gameserver.controller

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.player.HotseatControlComponent
import com.wingedsheep.gameserver.persistence.persistenceJson
import com.wingedsheep.gameserver.repository.GameRepository
import com.wingedsheep.gameserver.scenario.PlayerConfig
import com.wingedsheep.gameserver.scenario.ScenarioBuilderService
import com.wingedsheep.gameserver.scenario.ScenarioRequest
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
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
 * Production scenario endpoint `POST /api/scenarios`. Unlike the dev endpoint this is NOT
 * gated behind `game.dev-endpoints.enabled` (note its absence below), so these tests double as
 * the proof that the feature ships to all players. Covers: hotseat (SELF) wiring, the exile
 * zone, and validation of unknown card names.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ScenarioControllerTest(
    @param:Autowired private val gameRepository: GameRepository,
    @param:Autowired private val scenarioBuilderService: ScenarioBuilderService,
    @param:LocalServerPort private val port: Int,
) : FunSpec({

    val http = HttpClient.newHttpClient()
    val json = Json { ignoreUnknownKeys = true }

    fun post(body: String): HttpResponse<String> {
        val req = HttpRequest.newBuilder(URI.create("http://localhost:$port/api/scenarios"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()
        return http.send(req, HttpResponse.BodyHandlers.ofString())
    }

    fun postFromState(body: String): HttpResponse<String> {
        val req = HttpRequest.newBuilder(URI.create("http://localhost:$port/api/scenarios/from-state"))
            .header("Content-Type", "text/plain")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()
        return http.send(req, HttpResponse.BodyHandlers.ofString())
    }

    test("SELF mode creates a hotseat session: one token controls both seats") {
        val response = post(
            """
            {
              "player1Name": "Me",
              "player2Name": "Also me",
              "player1": { "lifeTotal": 20, "battlefield": [{"name": "Grizzly Bears"}] },
              "player2": { "lifeTotal": 20, "battlefield": [{"name": "Hill Giant"}] },
              "mode": "SELF"
            }
            """.trimIndent()
        )
        response.statusCode() shouldBe 200

        val body = json.parseToJsonElement(response.body()).jsonObject
        body["mode"]!!.jsonPrimitive.content shouldBe "SELF"
        val sessionId = body["sessionId"]!!.jsonPrimitive.content
        val p1 = body["player1"]!!.jsonObject
        val p2 = body["player2"]!!.jsonObject
        // Both PlayerInfo carry the same token — the one connection plays both seats.
        p1["token"]!!.jsonPrimitive.content shouldBe p2["token"]!!.jsonPrimitive.content

        // Every seat is hotseat-controlled by player 1's id.
        val session = gameRepository.findById(sessionId).shouldNotBeNull()
        val state = session.getStateForTesting().shouldNotBeNull()
        val p1Id = EntityId.of(p1["playerId"]!!.jsonPrimitive.content)
        for (seat in state.turnOrder) {
            state.getEntity(seat)?.get<HotseatControlComponent>()?.controllerId shouldBe p1Id
        }
    }

    test("exile zone is populated from the request") {
        val response = post(
            """
            {
              "player1": { "lifeTotal": 20, "exile": ["Grizzly Bears"] },
              "player2": { "lifeTotal": 20 },
              "mode": "TWO_PLAYER"
            }
            """.trimIndent()
        )
        response.statusCode() shouldBe 200
        val body = json.parseToJsonElement(response.body()).jsonObject
        val sessionId = body["sessionId"]!!.jsonPrimitive.content
        val p1Id = EntityId.of(body["player1"]!!.jsonObject["playerId"]!!.jsonPrimitive.content)

        val state = gameRepository.findById(sessionId).shouldNotBeNull().getStateForTesting().shouldNotBeNull()
        val exile = state.getZone(ZoneKey(p1Id, Zone.EXILE))
        exile.shouldNotBeNull()
        val names = exile.mapNotNull { state.getEntity(it)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name }
        names shouldContain "Grizzly Bears"
    }

    test("a scenario session resolves set-scoped token art") {
        // The scenario page is where a card gets eyeballed, so it is the worst place to render the
        // wrong token: it shipped passing no TokenArtRegistry, and every token it minted quietly
        // fell through to the engine-wide generic art for its creature type. Nothing failed — the
        // picture was just wrong. Asserted on both session-creating endpoints below.
        val response = post(
            """{ "player1": { "lifeTotal": 20 }, "player2": { "lifeTotal": 20 }, "mode": "SELF" }"""
        )
        response.statusCode() shouldBe 200
        val sessionId = json.parseToJsonElement(response.body()).jsonObject["sessionId"]!!.jsonPrimitive.content

        gameRepository.findById(sessionId).shouldNotBeNull().hasTokenArtForTesting() shouldBe true
    }

    test("a from-state session resolves set-scoped token art too") {
        val build = scenarioBuilderService.buildScenario(
            ScenarioRequest(player1 = PlayerConfig(lifeTotal = 20), player2 = PlayerConfig(lifeTotal = 20))
        )
        val response = postFromState(persistenceJson.encodeToString(GameState.serializer(), build.state))
        response.statusCode() shouldBe 200
        val sessionId = json.parseToJsonElement(response.body()).jsonObject["sessionId"]!!.jsonPrimitive.content

        gameRepository.findById(sessionId).shouldNotBeNull().hasTokenArtForTesting() shouldBe true
    }

    test("unknown card name is rejected with a 400 and a clear message") {
        val response = post(
            """
            { "player1": { "battlefield": [{"name": "Definitely Not A Real Card"}] }, "mode": "SELF" }
            """.trimIndent()
        )
        response.statusCode() shouldBe 400
        val errors = json.parseToJsonElement(response.body()).jsonObject["errors"]!!.jsonArray
            .map { it.jsonPrimitive.content }
        errors.any { it.contains("Unknown card: Definitely Not A Real Card") } shouldBe true
    }

    test("from-state injects a full serialized GameState as a hotseat session") {
        // Build a state, serialize it, and round-trip it through the exact-snapshot endpoint.
        val build = scenarioBuilderService.buildScenario(
            ScenarioRequest(
                player1 = PlayerConfig(lifeTotal = 17, battlefield = listOf()),
                player2 = PlayerConfig(lifeTotal = 20),
            )
        )
        val stateJson = persistenceJson.encodeToString(GameState.serializer(), build.state)

        val response = postFromState(stateJson)
        response.statusCode() shouldBe 200
        val body = json.parseToJsonElement(response.body()).jsonObject
        body["mode"]!!.jsonPrimitive.content shouldBe "SELF"
        val sessionId = body["sessionId"]!!.jsonPrimitive.content

        val state = gameRepository.findById(sessionId).shouldNotBeNull().getStateForTesting().shouldNotBeNull()
        val p1Id = EntityId.of(body["player1"]!!.jsonObject["playerId"]!!.jsonPrimitive.content)
        for (seat in state.turnOrder) {
            state.getEntity(seat)?.get<HotseatControlComponent>()?.controllerId shouldBe p1Id
        }
        // The injected life total survived the round-trip.
        state.getEntity(p1Id)?.get<com.wingedsheep.engine.state.components.identity.LifeTotalComponent>()?.life shouldBe 17
    }

    test("from-state rejects malformed JSON with a 400") {
        postFromState("{ not a game state }").statusCode() shouldBe 400
    }

    test("from-replay-frame returns 404 for an unknown replay") {
        val req = HttpRequest.newBuilder(URI.create("http://localhost:$port/api/scenarios/from-replay-frame"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString("""{"gameId":"does-not-exist","frame":0}"""))
            .build()
        http.send(req, HttpResponse.BodyHandlers.ofString()).statusCode() shouldBe 404
    }

    test("TWO_PLAYER mode returns two distinct tokens") {
        val response = post(
            """{ "player1": { "lifeTotal": 20 }, "player2": { "lifeTotal": 20 }, "mode": "TWO_PLAYER" }"""
        )
        response.statusCode() shouldBe 200
        val body = json.parseToJsonElement(response.body()).jsonObject
        val t1 = body["player1"]!!.jsonObject["token"]!!.jsonPrimitive.content
        val t2 = body["player2"]!!.jsonObject["token"]!!.jsonPrimitive.content
        (t1 != t2) shouldBe true
    }
})
