package com.wingedsheep.engine.state

import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.core.PendingDecision
import com.wingedsheep.engine.core.SubmitDecision
import com.wingedsheep.engine.core.engineSerializersModule
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class LegacySuspensionMigrationTest : ScenarioTestBase() {
    private val json = Json {
        serializersModule = engineSerializersModule
        allowStructuredMapKeys = true
        encodeDefaults = true
    }

    init {
        for (fixture in listOf("nested-may", "repeat-while", "suspended-mana-window", "free-cast-target")) {
            test("legacy $fixture loads without allocation and resumes the captured response trace") {
                val original = fixtureObject(fixture, "state.json")
                val manifest = fixtureObject(fixture, "manifest.json")
                manifest.getValue("sourceRevision").jsonPrimitive.content shouldBe
                    "fba4b704cb213843a1420809e0cf6aee656045c9"
                manifest.getValue("verified") shouldBe JsonPrimitive(true)
                var state = json.decodeFromString<GameState>(original.toString())
                state.zoneReturns shouldBe emptyList()
                state.nextRoutingId shouldBe original.getValue("nextRoutingId").jsonPrimitive.content.toLong()
                state.pendingDecision shouldBe json.decodeFromString<PendingDecision>(original.getValue("pendingDecision").toString())

                // The reader changes only suspension representation. Entity state, RNG, counters,
                // permissions, and all other saved fields are retained exactly on initial load.
                // Object identity, zone returns and the waiting-trigger queue postdate these
                // captures. Compare the saved fields after checking that the new return
                // bookkeeping starts empty.
                val encoded = encodeState(state)
                withoutCopyTriggerDefaults(JsonObject(encoded - "continuationStack" - POST_CAPTURE_FIELDS)) shouldBe
                    JsonObject(original - "continuationStack" - "pendingDecision")
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

        test("default-omitting parent cycling snapshot loads and completes its deferred draw") {
            val fixture = "compact-cycling"
            val compactJson = Json(json) { encodeDefaults = false }
            val original = fixtureObject(fixture, "state.json")
            val manifest = fixtureObject(fixture, "manifest.json")
            manifest.getValue("sourceRevision").jsonPrimitive.content shouldBe
                "fba4b704cb213843a1420809e0cf6aee656045c9"
            manifest.getValue("encodeDefaults") shouldBe JsonPrimitive(false)
            manifest.getValue("verified") shouldBe JsonPrimitive(true)
            val oldDraw = fixtureStack(fixture).first().jsonObject
            oldDraw.getValue("type") shouldBe JsonPrimitive(CORE + "CycleDrawContinuation")
            oldDraw.containsKey("decisionId") shouldBe false

            val state = json.decodeFromString<GameState>(original.toString())
            state.zoneReturns shouldBe emptyList()
            state.nextRoutingId shouldBe original.getValue("nextRoutingId").jsonPrimitive.content.toLong()
            state.pendingDecision shouldBe json.decodeFromString<PendingDecision>(original.getValue("pendingDecision").toString())
            val compact = compactJson.parseToJsonElement(compactJson.encodeToString(state)).jsonObject
            JsonObject(compact - "continuationStack" - POST_CAPTURE_FIELDS) shouldBe
                JsonObject(original - "continuationStack" - "pendingDecision")
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

        test("legacy active answer still requires its response identity") {
            val old = fixtureObject("nested-may", "state.json")
            val stack = fixtureStack("nested-may").toMutableList()
            stack[stack.lastIndex] = JsonObject(stack.last().jsonObject - "decisionId")
            expectMalformed(JsonObject(old + ("continuationStack" to JsonArray(stack))))
        }

        test("legacy mana reopen still requires its saved association identity") {
            val old = fixtureObject("suspended-mana-window", "state.json")
            val stack = fixtureStack("suspended-mana-window").toMutableList()
            stack[1] = JsonObject(stack[1].jsonObject - "decisionId")
            expectMalformed(JsonObject(old + ("continuationStack" to JsonArray(stack))))
        }

        test("legacy repeat decision becomes an answer carrying an automatic loop") {
            val state = json.decodeFromString<GameState>(fixtureText("repeat-while", "state.json"))
            val suspension = encodedStack(state).last().jsonObject
            suspension.getValue("type") shouldBe JsonPrimitive(CORE + "Suspension")
            val answer = suspension.getValue("answer").jsonObject
            answer.getValue("type") shouldBe JsonPrimitive(CORE + "RepeatWhileDecisionContinuation")
            val loop = answer.getValue("loop").jsonObject
            loop.containsKey("phase") shouldBe false
            loop.containsKey("decisionId") shouldBe false
            loop.getValue("body") shouldBe fixtureStack("repeat-while").last().jsonObject.getValue("body")
        }

        test("legacy hidden payment question is paired inside its reopen frame") {
            val state = json.decodeFromString<GameState>(fixtureText("suspended-mana-window", "state.json"))
            val migrated = encodedStack(state)
            migrated.size shouldBe 2
            val reopen = migrated.first().jsonObject
            reopen.getValue("type") shouldBe JsonPrimitive(CORE + "ReopenManaPaymentDecisionContinuation")
            val saved = reopen.getValue("suspension").jsonObject
            val original = fixtureStack("suspended-mana-window")
            saved.getValue("question") shouldBe JsonObject(
                original[1].jsonObject.getValue("decision").jsonObject + ("type" to JsonPrimitive("SelectManaSourcesDecision"))
            )
            // Every field the legacy frame carried survives untouched. Fields added since the
            // capture (object identity's, and anything later) decode to their defaults and are
            // not the reader's doing, so the comparison is restricted to the captured shape. The
            // capture predates the trigger-fact record, so its flat trigger keys are compared as the
            // reader lifts them (LegacyTriggerContextLift) — all null here, so they simply fold away.
            val capturedAnswer = LegacyTriggerContextLift.lift(JsonObject(original[0].jsonObject - "decisionId")).jsonObject
            restrictTo(saved.getValue("answer"), capturedAnswer) shouldBe capturedAnswer
            // Same rule for the pending question: fields added to the decision since the capture
            // (ChooseColorDecision.maxColors) decode to their defaults, so compare the captured shape.
            val capturedQuestion = fixtureObject("suspended-mana-window", "state.json").getValue("pendingDecision")
            restrictTo(migrated.last().jsonObject.getValue("question"), capturedQuestion) shouldBe capturedQuestion
        }

        test("legacy combat question is retained once and the duplicate answer shape is removed") {
            val original = legacyCombatState()
            val state = json.decodeFromString<GameState>(original.toString())
            state.zoneReturns shouldBe emptyList()
            state.nextRoutingId shouldBe original.getValue("nextRoutingId").jsonPrimitive.content.toLong()
            val suspension = encodedStack(state).last().jsonObject
            suspension.getValue("question") shouldBe json.parseToJsonElement(
                json.encodeToString(json.decodeFromString<PendingDecision>(original.getValue("pendingDecision").toString()))
            )
            suspension.getValue("answer").jsonObject.containsKey("decisionShape") shouldBe false
            assertCurrentRoundTrip(state)
        }

        test("legacy combat answer cannot silently replace a different represented question") {
            val original = legacyCombatState()
            val question = original.getValue("pendingDecision").jsonObject
            expectMalformed(JsonObject(original + (
                "pendingDecision" to JsonObject(question + ("prompt" to JsonPrimitive("A different question")))
            )))
        }

        test("legacy active question with a mismatched top answer fails explicitly") {
            val old = fixtureObject("nested-may", "state.json")
            val mismatched = JsonObject(old.getValue("pendingDecision").jsonObject + ("id" to JsonPrimitive("other-question")))
            expectMalformed(JsonObject(old + ("pendingDecision" to mismatched)))
        }

        test("legacy active question without an answer fails explicitly") {
            val old = fixtureObject("nested-may", "state.json")
            expectMalformed(JsonObject(old + ("continuationStack" to JsonArray(emptyList()))))
        }

        test("legacy unanswered frame without any represented question fails explicitly") {
            val old = fixtureObject("nested-may", "state.json")
            expectMalformed(JsonObject(old + ("pendingDecision" to JsonNull)))
        }

        test("legacy active question cannot skip automatic work to find an answer") {
            val old = fixtureObject("nested-may", "state.json")
            val stack = fixtureStack("nested-may")
            expectMalformed(JsonObject(old + ("continuationStack" to JsonArray(stack + stack.first()))))
        }

        test("legacy mana reopen cannot claim a different lower answer") {
            val old = fixtureObject("suspended-mana-window", "state.json")
            val stack = fixtureStack("suspended-mana-window").toMutableList()
            stack[0] = JsonObject(stack[0].jsonObject + ("decisionId" to JsonPrimitive("other-payment")))
            expectMalformed(JsonObject(old + ("continuationStack" to JsonArray(stack))))
        }

        test("legacy mana reopen requires its saved question") {
            val old = fixtureObject("suspended-mana-window", "state.json")
            val stack = fixtureStack("suspended-mana-window").toMutableList()
            stack[1] = JsonObject(stack[1].jsonObject - "decision")
            expectMalformed(JsonObject(old + ("continuationStack" to JsonArray(stack))))
        }

        test("legacy repeat with an unknown phase fails explicitly") {
            val old = fixtureObject("repeat-while", "state.json")
            val stack = fixtureStack("repeat-while").toMutableList()
            stack[stack.lastIndex] = JsonObject(stack.last().jsonObject + ("phase" to JsonPrimitive("UNKNOWN")))
            expectMalformed(JsonObject(old + ("continuationStack" to JsonArray(stack))))
        }

        test("legacy migration does not silently ignore unrelated unknown payload fields") {
            val old = fixtureObject("nested-may", "state.json")
            val stack = fixtureStack("nested-may").toMutableList()
            stack[stack.lastIndex] = JsonObject(stack.last().jsonObject + ("unknownPayload" to JsonPrimitive(true)))
            shouldThrow<SerializationException> {
                json.decodeFromString<GameState>(JsonObject(old + ("continuationStack" to JsonArray(stack))).toString())
            }
        }
    }

    /** Synthetic representation fixture: migration needs no gameplay or combat execution. */
    private fun legacyCombatState(): JsonObject {
        val state = fixtureObject("nested-may", "state.json")
        val previous = state.getValue("pendingDecision").jsonObject
        val question = JsonObject(mapOf(
            "type" to JsonPrimitive("CombatResolutionDecision"),
            "id" to previous.getValue("id"),
            "playerId" to previous.getValue("playerId"),
            "prompt" to JsonPrimitive("Assign combat damage"),
            "context" to previous.getValue("context"),
            "firstStrike" to JsonPrimitive(false),
            "attackers" to JsonArray(emptyList()),
            "blockers" to JsonArray(emptyList()),
            "defenders" to JsonArray(emptyList()),
            "edges" to JsonArray(emptyList()),
        ))
        val answer = JsonObject(mapOf(
            "type" to JsonPrimitive(CORE + "CombatResolutionContinuation"),
            "decisionId" to previous.getValue("id"),
            "firstStrike" to JsonPrimitive(false),
            "pendingChoosers" to JsonArray(listOf(previous.getValue("playerId"))),
            "decisionShape" to JsonObject(question - "type"),
        ))
        return JsonObject(state + mapOf(
            "pendingDecision" to question,
            "continuationStack" to JsonArray(listOf(answer)),
        ))
    }

    private fun expectMalformed(state: JsonObject) {
        shouldThrow<LegacySuspensionSerializationException> { json.decodeFromString<GameState>(state.toString()) }
    }

    private fun assertCurrentRoundTrip(state: GameState) {
        val encoded = encodeState(state)
        encoded.containsKey("pendingDecision") shouldBe false
        containsObsoleteId(encoded.getValue("continuationStack")) shouldBe false
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
     */
    private fun normalizeRouting(value: JsonElement, root: Boolean = false): JsonElement = when (value) {
        is JsonObject -> JsonObject((if (root) value - "controlAtTurnStart" else value).mapValues { (key, child) ->
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

    /** Copy-added rules text postdates the capture; loading an old identity must default it empty. */
    private fun withoutCopyTriggerDefaults(value: JsonElement): JsonElement = when (value) {
        is JsonObject -> {
            val fields = if (value["type"] == JsonPrimitive(
                    "com.wingedsheep.engine.state.components.identity.CardComponent"
                )) {
                value.getValue("copyTriggeredAbilities") shouldBe JsonArray(emptyList())
                value - "copyTriggeredAbilities"
            } else value
            JsonObject(fields.mapValues { withoutCopyTriggerDefaults(it.value) })
        }
        is JsonArray -> JsonArray(value.map(::withoutCopyTriggerDefaults))
        else -> value
    }

    /** [actual] narrowed to the keys [shape] has, at every depth — additions are ignored. */
    private fun restrictTo(actual: JsonElement, shape: JsonElement): JsonElement = when {
        actual is JsonObject && shape is JsonObject ->
            JsonObject(actual.filterKeys { it in shape }.mapValues { restrictTo(it.value, shape.getValue(it.key)) })
        actual is JsonArray && shape is JsonArray && actual.size == shape.size ->
            JsonArray(actual.mapIndexed { i, child -> restrictTo(child, shape[i]) })
        else -> actual
    }

    private fun encodeState(state: GameState): JsonObject = json.parseToJsonElement(json.encodeToString(state)).jsonObject
    private fun encodedStack(state: GameState): JsonArray = encodeState(state).getValue("continuationStack") as JsonArray
    private fun fixtureStack(fixture: String): JsonArray = fixtureObject(fixture, "state.json").getValue("continuationStack") as JsonArray
    private fun fixtureObject(fixture: String, name: String): JsonObject = json.parseToJsonElement(fixtureText(fixture, name)).jsonObject
    private fun fixtureText(fixture: String, name: String): String =
        checkNotNull(javaClass.getResource("/legacy-suspensions/$fixture/$name")) { "Missing captured fixture $fixture/$name" }.readText()

    companion object {
        private const val CORE = "com.wingedsheep.engine.core."

        /** Fields introduced after these captures; decoding supplies their defaults. */
        private val POST_CAPTURE_FIELDS = setOf(
            "objectIdentities", "nextObjectGeneration", "zoneReturns", "pendingTriggers", "controlAtTurnStart",
            "playersDealtNoncombatDamageThisTurn", "playersDealtNoncombatDamageLastTurn",
            "pendingReplacementRiders", "playersDealtCombatDamageSinceTheirLastTurn",
        )
    }
}
