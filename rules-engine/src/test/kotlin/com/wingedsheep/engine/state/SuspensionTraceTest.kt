package com.wingedsheep.engine.state

import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.core.SubmitDecision
import com.wingedsheep.engine.core.engineSerializersModule
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class SuspensionTraceTest : ScenarioTestBase() {
    // Defaults are omitted on both sides of every comparison (and in the fixtures), so a new field
    // with a default is invisible here: an additive schema change needs no fixture refresh. A field
    // that *moves* or changes value still shows up, which is what these traces are for.
    private val json = Json {
        serializersModule = engineSerializersModule
        allowStructuredMapKeys = true
        encodeDefaults = false
    }

    init {
        for (fixture in listOf("nested-may", "repeat-while", "suspended-mana-window", "free-cast-target")) {
            test("current $fixture suspension reproduces the captured parent response trace") {
                val original = fixtureObject(fixture, "state.json")
                val manifest = fixtureObject(fixture, "manifest.json")
                manifest.getValue("sourceRevision").jsonPrimitive.content shouldBe
                    "fba4b704cb213843a1420809e0cf6aee656045c9"
                manifest.getValue("verified") shouldBe JsonPrimitive(true)
                manifest.getValue("representationEncodeDefaults") shouldBe JsonPrimitive(false)
                var state = json.decodeFromString<GameState>(original.toString())
                manifest.getValue("representationReaderRevision").jsonPrimitive.content shouldBe
                    "6f222eb05a1d9e61f540217b2b372960ddf051c7"
                encodeState(state) shouldBe original
                assertCurrentRoundTrip(state)

                val actions = json.decodeFromString<List<GameAction>>(fixtureText(fixture, "actions.json"))
                for ((index, recorded) in actions.withIndex()) {
                    val action = if (recorded is SubmitDecision) {
                        val question = checkNotNull(state.pendingDecision)
                        if (index == 0) question.id shouldBe recorded.response.decisionId
                        // New automatic work allocates no routing IDs. Only subsequent response
                        // routing is rebound; the recorded player and complete choice payload stay.
                        recorded.copy(response = recorded.response.withDecisionId(question.id))
                    } else recorded
                    val result = actionProcessor.process(state, action).result
                    result.error shouldBe null
                    state = result.state
                    val expected = json.decodeFromString<GameState>(fixtureText(fixture, "after-${index + 1}.json"))
                    normalizeRouting(encodeState(state), root = true) shouldBe
                        normalizeRouting(encodeState(expected), root = true)
                    assertCurrentRoundTrip(state)
                }
            }
        }

        test("current cycling suspension reproduces the captured parent deferred draw and events") {
            val fixture = "compact-cycling"
            val manifest = fixtureObject(fixture, "manifest.json")
            manifest.getValue("sourceRevision").jsonPrimitive.content shouldBe
                "fba4b704cb213843a1420809e0cf6aee656045c9"
            manifest.getValue("representationReaderRevision").jsonPrimitive.content shouldBe
                "6f222eb05a1d9e61f540217b2b372960ddf051c7"
            manifest.getValue("encodeDefaults") shouldBe JsonPrimitive(false)
            manifest.getValue("verified") shouldBe JsonPrimitive(true)
            val state = json.decodeFromString<GameState>(fixtureText(fixture, "state.json"))
            assertCurrentRoundTrip(state)

            val action = json.decodeFromString<List<GameAction>>(fixtureText(fixture, "actions.json")).single()
            val result = actionProcessor.process(state, action).result
            result.error shouldBe null
            result.state.copy(controlAtTurnStart = null) shouldBe
                json.decodeFromString<GameState>(fixtureText(fixture, "after-1.json"))
            result.events shouldBe json.decodeFromString<List<GameEvent>>(fixtureText(fixture, "events-1.json"))
            result.state.pendingDecision shouldBe null
            result.state.continuationStack shouldBe emptyList()
            result.state.getHand(action.playerId).size shouldBe 1
            result.state.getLibrary(action.playerId).size shouldBe 1
        }

    }

    private fun assertCurrentRoundTrip(state: GameState) {
        val encoded = encodeState(state)
        encoded.containsKey("pendingDecision") shouldBe false
        // An empty stack is the default, so it is omitted once the stack has drained.
        (encoded["continuationStack"]?.let(::containsObsoleteId) ?: false) shouldBe false
        json.decodeFromString<GameState>(encoded.toString()) shouldBe state
    }

    private fun containsObsoleteId(value: JsonElement): Boolean = when (value) {
        is JsonObject -> value.containsKey("decisionId") || value.values.any(::containsObsoleteId)
        is JsonArray -> value.any(::containsObsoleteId)
        else -> false
    }

    /**
     * Preserve the captured state and payload identities; normalize routing and omit control history,
     * which postdates this trace and is verified by ControlHistoryTest and scenario tests.
     * Source-choice target references also postdate the trace; ChosenSourceDamageRedirectionTest
     * independently verifies their capture, retention after departure and serialization.
     */
    private fun normalizeRouting(value: JsonElement, root: Boolean = false): JsonElement = when (value) {
        is JsonObject -> JsonObject(((if (root) value - "controlAtTurnStart" else value) - "targetObjectRefs" - "referencedObjects").mapValues { (key, child) ->
            when {
                root && key == "nextRoutingId" -> JsonPrimitive(0)
                key == "question" && "answer" in value -> {
                    val question = child.jsonObject
                    JsonObject(question + ("id" to JsonPrimitive("<question-routing>")))
                }
                else -> normalizeRouting(child)
            }
        })
        is JsonArray -> JsonArray(value.map { normalizeRouting(it) })
        else -> value
    }

    private fun encodeState(state: GameState): JsonObject = json.parseToJsonElement(json.encodeToString(state)).jsonObject
    private fun fixtureObject(fixture: String, name: String): JsonObject = json.parseToJsonElement(fixtureText(fixture, name)).jsonObject
    private fun fixtureText(fixture: String, name: String): String =
        checkNotNull(javaClass.getResource("/suspension-traces/$fixture/$name")) { "Missing captured fixture $fixture/$name" }.readText()
}
