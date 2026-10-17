package com.wingedsheep.engine.state

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer

/** A legacy stack cannot be restored without guessing which question an answer belongs to. */
class LegacySuspensionSerializationException(message: String) : SerializationException(message)

/** Reads the former question/continuation representation; writes only the current representation. */
object LegacyGameStateSerializer : JsonTransformingSerializer<GameState>(GameStateSerializer) {
    override fun transformDeserialize(element: JsonElement): JsonElement =
        migrateLegacySuspensions(migrateCastTimeFlags(element))
}

/**
 * A spell on the stack used to record its "as you cast this spell" captures as the set of names whose
 * condition held (`castTimeFlags`); it now records every capture as a number (`castTimeValues`), a
 * condition capture being 1 when it held. Rewrites the former key wherever it appears.
 */
private fun migrateCastTimeFlags(element: JsonElement): JsonElement = when (element) {
    is JsonArray -> {
        val children = element.map(::migrateCastTimeFlags)
        if (children.indices.all { children[it] === element[it] }) element else JsonArray(children)
    }
    is JsonObject -> {
        val flags = element["castTimeFlags"] as? JsonArray
        val children = element.mapValues { (_, value) -> migrateCastTimeFlags(value) }
        when {
            flags != null -> JsonObject(
                (children - "castTimeFlags") + ("castTimeValues" to JsonObject(
                    flags.associate { (it as JsonPrimitive).content to JsonPrimitive(1) }
                ))
            )
            children.all { (key, value) -> value === element[key] } -> element
            else -> JsonObject(children)
        }
    }
    else -> element
}

private const val CORE = "com.wingedsheep.engine.core."
private const val REOPEN = CORE + "ReopenManaPaymentDecisionContinuation"
private const val REPEAT = CORE + "RepeatWhileContinuation"
private const val SUSPENSION = CORE + "Suspension"

// This is the legacy classification, frozen at the representation boundary. In that format an
// automatic frame also carried decisionId, so the presence of an ID alone cannot identify an answer.
private val legacyAutomaticTypes = setOf(
    "ReplacementResolveContinuation",
    "ModalPreChosenContinuation",
    "SpliceTailContinuation",
    "ModalChosenModeTailContinuation",
    "CreateTokenCopyRemainingContinuation",
    "LeylinePhaseContinuation",
    "DrawReplacementRemainingDrawsContinuation",
    "CycleDrawContinuation",
    "TypecycleSearchContinuation",
    "EffectContinuation",
    "PendingTriggersContinuation",
    "GatedActionContinuation",
    "ForEachContinuation",
    "ReflexiveTriggerTargetContinuation",
    "ChainCopyAfterActionContinuation"
).mapTo(mutableSetOf()) { CORE + it }

private data class LegacyStackEntry(
    val value: JsonObject,
    val unansweredId: String? = null,
    val originalIndex: Int
)

private fun migrateLegacySuspensions(element: JsonElement): JsonElement {
    val state = element as? JsonObject ?: return element
    val stackElement = state["continuationStack"] ?: JsonArray(emptyList())
    val stack = stackElement as? JsonArray
        ?: if ("pendingDecision" in state) fail("Legacy continuationStack is not an array") else return element
    val legacy = "pendingDecision" in state || stack.any { (it as? JsonObject)?.containsKey("decisionId") == true }
    if (!legacy) return element

    val pending = when (val question = state["pendingDecision"]) {
        null, JsonNull -> null
        is JsonObject -> question
        else -> fail("Legacy pendingDecision is not a question object")
    }
    if (pending != null && stack.isEmpty()) fail("Legacy pending decision has no answer continuation")

    val migrated = mutableListOf<LegacyStackEntry>()
    for ((index, value) in stack.withIndex()) {
        val frame = value as? JsonObject ?: fail("Legacy continuation at index $index is not an object")
        val type = stringField(frame, "type", "continuation at index $index")
        if (type == SUSPENSION) fail("Legacy state mixes suspension objects with the old pendingDecision representation")
        // Automatic work has no question association to validate. The old serializer could
        // omit its default decisionId (for example CycleDrawContinuation's "cycle-draw").
        if (isAutomatic(frame, type)) {
            if (pending != null && index == stack.lastIndex) {
                fail("Legacy pending decision is covered by a non-answer continuation at index $index")
            }
            migrated += LegacyStackEntry(stripLegacyFields(frame, type), originalIndex = index)
            continue
        }
        val id = stringField(frame, "decisionId", "continuation at index $index")

        // The active answer must be the actual top frame. Searching down past automatic work or
        // another answer would accept a state the former response dispatcher could not resume.
        if (pending != null && index == stack.lastIndex) {
            if (type == REOPEN) {
                fail("Legacy pending decision is covered by a non-answer continuation at index $index")
            }
            requireMatchingId(pending, id, "active answer at index $index")
            migrated += LegacyStackEntry(suspension(pending, migrateAnswer(frame, type, pending), polymorphic = true), originalIndex = index)
            continue
        }

        when {
            type == REOPEN -> {
                val savedQuestion = frame["decision"] as? JsonObject
                    ?: fail("Legacy mana reopen at index $index has no saved question")
                // The old field was statically typed SelectManaSourcesDecision and therefore
                // omitted its discriminator. The new Suspension.question is polymorphic.
                savedQuestion["type"]?.let {
                    if (it != JsonPrimitive("SelectManaSourcesDecision")) {
                        fail("Legacy mana reopen at index $index has a non-payment question")
                    }
                }
                val question = JsonObject(savedQuestion + ("type" to JsonPrimitive("SelectManaSourcesDecision")))
                requireMatchingId(question, id, "mana reopen at index $index")
                // Pair with the nearest lower unanswered frame. Intervening automatic work stays
                // in order; reaching across another unanswered frame would invent an ownership link.
                val answerIndex = migrated.indexOfLast { it.unansweredId != null }
                if (answerIndex < 0) fail("Legacy mana reopen at index $index has no lower answer for $id")
                val answer = migrated[answerIndex]
                if (answer.unansweredId != id) {
                    fail("Legacy mana reopen $id mismatches lower answer ${answer.unansweredId} at index ${answer.originalIndex}")
                }
                migrated.removeAt(answerIndex)
                val answerType = stringField(answer.value, "type", "saved mana answer")
                val nested = suspension(question, migrateAnswer(answer.value, answerType, question), polymorphic = false)
                migrated += LegacyStackEntry(
                    JsonObject((frame - "decisionId" - "decision") + ("suspension" to nested)),
                    originalIndex = index
                )
            }
            else -> migrated += LegacyStackEntry(frame, id, index)
        }
    }

    migrated.firstOrNull { it.unansweredId != null }?.let {
        fail("Legacy answer ${it.unansweredId} at index ${it.originalIndex} has no represented question")
    }
    return JsonObject((state - "pendingDecision") + ("continuationStack" to JsonArray(migrated.map { it.value })))
}

private fun isAutomatic(frame: JsonObject, type: String): Boolean = when (type) {
    REPEAT -> when (stringField(frame, "phase", "legacy repeat continuation")) {
        "AFTER_BODY" -> true
        "AFTER_DECISION" -> false
        else -> fail("Legacy repeat continuation has an unknown phase")
    }
    else -> type in legacyAutomaticTypes
}

private fun migrateAnswer(frame: JsonObject, type: String, question: JsonObject): JsonObject {
    if (type == REPEAT) {
        if (stringField(frame, "phase", "legacy repeat answer") != "AFTER_DECISION") {
            fail("Legacy repeat answer is not awaiting a repeat decision")
        }
        // loop is a statically typed RepeatWhileContinuation field, so it has no discriminator.
        return JsonObject(mapOf(
            "type" to JsonPrimitive(CORE + "RepeatWhileDecisionContinuation"),
            "loop" to JsonObject(frame - "type" - "decisionId" - "phase")
        ))
    }
    val stripped = stripLegacyFields(frame, type)
    return when (type) {
        CORE + "CombatResolutionContinuation" -> {
            val shape = stripped["decisionShape"] as? JsonObject
                ?: fail("Legacy combat answer has no decision shape")
            if (shape != JsonObject(question - "type")) {
                fail("Legacy combat answer shape differs from its represented question")
            }
            JsonObject(stripped - "decisionShape")
        }
        CORE + "CostPaymentManaSelectionContinuation", CORE + "PayOrSufferManaSelectionContinuation" -> {
            val inner = stripped["inner"] as? JsonObject
                ?: fail("Legacy $type has no inner payment continuation")
            // These two fields are statically typed continuation payloads, not independent pending
            // answers. Remove their obsolete ID without weakening unknown-field validation globally.
            JsonObject(stripped + ("inner" to JsonObject(inner - "decisionId")))
        }
        else -> stripped
    }
}

private fun stripLegacyFields(frame: JsonObject, type: String): JsonObject =
    JsonObject(if (type == REPEAT) frame - "decisionId" - "phase" else frame - "decisionId")

private fun suspension(question: JsonObject, answer: JsonObject, polymorphic: Boolean): JsonObject =
    JsonObject(buildMap {
        if (polymorphic) put("type", JsonPrimitive(SUSPENSION))
        put("question", question)
        put("answer", answer)
    })

private fun requireMatchingId(question: JsonObject, answerId: String, context: String) {
    val questionId = stringField(question, "id", context)
    if (questionId != answerId) fail("Legacy question $questionId mismatches $context ($answerId)")
}

private fun stringField(value: JsonObject, name: String, context: String): String =
    (value[name] as? JsonPrimitive)?.takeIf { it.isString }?.content
        ?: fail("Legacy $context has no string $name")

private fun fail(message: String): Nothing = throw LegacySuspensionSerializationException(message)
