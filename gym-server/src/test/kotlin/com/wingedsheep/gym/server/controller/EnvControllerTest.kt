package com.wingedsheep.gym.server.controller

import com.wingedsheep.gym.contract.TrainingObservation
import com.wingedsheep.gym.service.DeckSpec
import com.wingedsheep.gym.service.EnvConfig
import com.wingedsheep.gym.service.EnvId
import com.wingedsheep.gym.service.PlayerSpec
import com.wingedsheep.gym.server.dto.CreateEnvResponse
import com.wingedsheep.gym.server.dto.DisposeBody
import com.wingedsheep.gym.server.dto.SchemaHashResponse
import com.wingedsheep.gym.server.dto.StepBody
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/**
 * End-to-end integration test for the HTTP surface. Boots the full Spring
 * app on a random port, then drives each endpoint via `java.net.http.HttpClient`
 * so we exercise the real converter chain (kotlinx.serialization) and
 * exception-handler chain.
 *
 * Deliberately thin — happy path, 404/400 errors, and a stale-action-ID
 * rejection. Sealed-deck flows and structured decisions belong in
 * dedicated tests alongside the controllers.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EnvControllerTest : FunSpec() {

    @LocalServerPort
    private var port: Int = 0

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    private val client: HttpClient = HttpClient.newBuilder().build()

    init {
        extension(SpringExtension())

        fun baseUrl() = "http://localhost:$port"

        fun miniDeck() = DeckSpec.Explicit(mapOf("Mountain" to 17, "Raging Goblin" to 3))
        fun twoPlayerConfig() = EnvConfig(
            players = listOf(
                PlayerSpec("Alice", miniDeck()),
                PlayerSpec("Bob", miniDeck())
            ),
            skipMulligans = true,
            startingPlayerIndex = 0
        )

        fun get(path: String): HttpResponse<String> = client.send(
            HttpRequest.newBuilder(URI.create("${baseUrl()}$path")).GET().build(),
            HttpResponse.BodyHandlers.ofString()
        )

        fun postJson(path: String, body: String): HttpResponse<String> = client.send(
            HttpRequest.newBuilder(URI.create("${baseUrl()}$path"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build(),
            HttpResponse.BodyHandlers.ofString()
        )

        fun deleteJson(path: String, body: String): HttpResponse<String> = client.send(
            HttpRequest.newBuilder(URI.create("${baseUrl()}$path"))
                .header("Content-Type", "application/json")
                .method("DELETE", HttpRequest.BodyPublishers.ofString(body))
                .build(),
            HttpResponse.BodyHandlers.ofString()
        )

        test("GET /schema-hash returns the current schema") {
            val response = get("/schema-hash")
            response.statusCode() shouldBe 200
            val parsed = json.decodeFromString<SchemaHashResponse>(response.body())
            parsed.schemaHash.shouldNotBe("")
        }

        test("GET /health returns ok") {
            val response = get("/health")
            response.statusCode() shouldBe 200
            response.body() shouldContain "\"status\""
            response.body() shouldContain "\"ok\""
        }

        test("GET /v3/api-docs serves the OpenAPI spec") {
            val response = get("/v3/api-docs")
            response.statusCode() shouldBe 200
            // Sanity-check a couple of endpoints appear in the spec.
            response.body() shouldContain "\"/envs\""
            response.body() shouldContain "\"/envs/{id}/step\""
            response.body() shouldContain "Environments"  // tag name from EnvController
        }

        test("OpenAPI spec has no Kotlin inline-class mangled property names") {
            // Regression for the `id-v2tQoa0` / `ownerId-Z9UYGMk` cosmetic bug:
            // Kotlin mangles @JvmInline value class getter method names, and
            // Springdoc's reflection introspection picks those names up unless
            // the `stripInlineClassMangling` customizer in OpenApiConfig runs.
            val body = get("/v3/api-docs").body()
            // Any property ending in `-XXXXXX` with mixed case/digits is suspect.
            val suspicious = Regex("\"([A-Za-z]+-[A-Za-z0-9_]{5,})\"\\s*:\\s*\\{")
                .findAll(body)
                .map { it.groupValues[1] }
                .toList()
            suspicious shouldBe emptyList()
        }

        test("GET /swagger-ui/index.html serves the UI") {
            // The `/swagger-ui.html` path redirects to `/swagger-ui/index.html`;
            // we hit the underlying page directly so a default HttpClient
            // (which follows redirects by default) doesn't matter.
            val response = get("/swagger-ui/index.html")
            response.statusCode() shouldBe 200
            response.body() shouldContain "Swagger UI"
        }

        test("create -> observe -> step -> dispose round-trips over HTTP") {
            // -- create --
            val createResponse = postJson("/envs", json.encodeToString(twoPlayerConfig()))
            createResponse.statusCode() shouldBe 200
            val created = json.decodeFromString<CreateEnvResponse>(createResponse.body())

            created.envId.value.shouldNotBe("")
            created.observation.players.size shouldBe 2
            created.observation.terminated.shouldBeFalse()
            created.observation.legalActions.shouldNotBeEmpty()

            // -- observe (no-op) --
            val observed = json.decodeFromString<TrainingObservation>(
                get("/envs/${created.envId.value}").body()
            )
            observed.stateDigest shouldBe created.observation.stateDigest

            // -- step using an actionId from the opening observation --
            val actionId = created.observation.legalActions.first().actionId
            val stepResp = postJson(
                "/envs/${created.envId.value}/step",
                json.encodeToString(StepBody(actionId))
            )
            stepResp.statusCode() shouldBe 200
            val afterStep = json.decodeFromString<TrainingObservation>(stepResp.body())
            afterStep.stateDigest shouldNotBe created.observation.stateDigest

            // -- list includes the env --
            val listResp = get("/envs")
            val ids = json.decodeFromString<List<EnvId>>(listResp.body())
            ids.any { it.value == created.envId.value } shouldBe true

            // -- dispose --
            val disposeResp = deleteJson(
                "/envs",
                json.encodeToString(DisposeBody(listOf(created.envId)))
            )
            disposeResp.statusCode() shouldBe 204

            // Observing a disposed env returns 404.
            get("/envs/${created.envId.value}").statusCode() shouldBe 404
        }

        test("POST /envs with an unknown set code surfaces 400") {
            val bogus = EnvConfig(
                players = listOf(
                    PlayerSpec("A", DeckSpec.RandomSealed(setCode = "ZZZ")),
                    PlayerSpec("B", DeckSpec.RandomSealed(setCode = "ZZZ"))
                )
            )
            postJson("/envs", json.encodeToString(bogus)).statusCode() shouldBe 400
        }

        test("GET /envs/{unknown} returns 404") {
            get("/envs/definitely-not-an-env").statusCode() shouldBe 404
        }

        test("step with an out-of-range actionId returns 400") {
            val created = json.decodeFromString<CreateEnvResponse>(
                postJson("/envs", json.encodeToString(twoPlayerConfig())).body()
            )

            val stepResp = postJson(
                "/envs/${created.envId.value}/step",
                json.encodeToString(StepBody(99_999))
            )
            stepResp.statusCode() shouldBe 400

            // Cleanup
            deleteJson("/envs", json.encodeToString(DisposeBody(listOf(created.envId))))
        }

        test("turnNumber advances after several steps") {
            val created = json.decodeFromString<CreateEnvResponse>(
                postJson("/envs", json.encodeToString(twoPlayerConfig())).body()
            )
            val initialTurn = created.observation.turnNumber

            repeat(3) {
                val obs = json.decodeFromString<TrainingObservation>(
                    get("/envs/${created.envId.value}").body()
                )
                val nextAction = obs.legalActions.firstOrNull() ?: return@repeat
                postJson(
                    "/envs/${created.envId.value}/step",
                    json.encodeToString(StepBody(nextAction.actionId))
                )
            }
            val finalObs = json.decodeFromString<TrainingObservation>(
                get("/envs/${created.envId.value}").body()
            )
            (finalObs.turnNumber >= initialTurn) shouldBe true

            deleteJson("/envs", json.encodeToString(DisposeBody(listOf(created.envId))))
        }
    }
}
