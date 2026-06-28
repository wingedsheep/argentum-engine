package com.wingedsheep.gameserver.controller

import com.wingedsheep.gameserver.auth.AuthSupport
import com.wingedsheep.gameserver.persistence.DeckRepository
import com.wingedsheep.gameserver.persistence.DeckRow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

/**
 * CRUD for a signed-in user's saved decks. Namespaced under /api/account/decks so it doesn't collide
 * with the existing stateless [DecksController] (/api/decks: validation, formats, examples).
 *
 * The deck body is the client's `SharedDeck` JSON, stored verbatim in [DeckRow.data]; `name` and
 * `format` are denormalized out of it for cheap list views. JSON is handled with kotlinx.serialization
 * (the project's JSON library) rather than Jackson, so deck payloads round-trip without re-encoding.
 * All operations are scoped to the authenticated user.
 */
@RestController
@RequestMapping("/api/account/decks")
@ConditionalOnProperty(name = ["accounts.enabled"], havingValue = "true")
class AccountDeckController(
    private val decks: DeckRepository,
    private val authSupport: AuthSupport,
) {
    data class DeckSummary(val id: Long, val name: String, val format: String?, val updatedAt: String)

    private val json = Json { ignoreUnknownKeys = true }

    @GetMapping
    fun list(@RequestHeader(HttpHeaders.AUTHORIZATION, required = false) auth: String?): List<DeckSummary> {
        val userId = authSupport.requireUser(auth).userId
        return decks.findByUserIdOrderByUpdatedAtDesc(userId).map { it.toSummary() }
    }

    /**
     * Full detail for every deck in one round-trip (`GET /api/account/decks?full`). Lets the deck
     * browser render rich metadata (card counts, colors, art) for cloud decks without an N+1 of
     * per-deck `GET /{id}` calls. Returns the same shape as repeated `get`, as a JSON array.
     */
    @GetMapping(params = ["full"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun listFull(@RequestHeader(HttpHeaders.AUTHORIZATION, required = false) auth: String?): ResponseEntity<Any> {
        val userId = authSupport.requireUser(auth).userId
        val rows = decks.findByUserIdOrderByUpdatedAtDesc(userId)
        val array = buildJsonArray { rows.forEach { add(it.toDetailObject()) } }
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
            .body(json.encodeToString(JsonArray.serializer(), array))
    }

    @GetMapping("/{id}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun get(
        @PathVariable id: Long,
        @RequestHeader(HttpHeaders.AUTHORIZATION, required = false) auth: String?,
    ): ResponseEntity<Any> {
        val userId = authSupport.requireUser(auth).userId
        val deck = decks.findByIdAndUserId(id, userId) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(deck.toDetailJson())
    }

    @PostMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun create(
        @RequestBody body: String,
        @RequestHeader(HttpHeaders.AUTHORIZATION, required = false) auth: String?,
    ): ResponseEntity<Any> {
        val userId = authSupport.requireUser(auth).userId
        val parsed = parse(body) ?: return ResponseEntity.badRequest().body(mapOf("error" to "Invalid deck JSON"))
        val now = Instant.now()
        val saved = decks.save(
            DeckRow(userId = userId, name = parsed.name, format = parsed.format, data = body, createdAt = now, updatedAt = now)
        )
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(saved.toDetailJson())
    }

    @PutMapping("/{id}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun update(
        @PathVariable id: Long,
        @RequestBody body: String,
        @RequestHeader(HttpHeaders.AUTHORIZATION, required = false) auth: String?,
    ): ResponseEntity<Any> {
        val userId = authSupport.requireUser(auth).userId
        val existing = decks.findByIdAndUserId(id, userId) ?: return ResponseEntity.notFound().build()
        val parsed = parse(body) ?: return ResponseEntity.badRequest().body(mapOf("error" to "Invalid deck JSON"))
        val saved = decks.save(
            existing.copy(name = parsed.name, format = parsed.format, data = body, updatedAt = Instant.now())
        )
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(saved.toDetailJson())
    }

    @DeleteMapping("/{id}")
    fun delete(
        @PathVariable id: Long,
        @RequestHeader(HttpHeaders.AUTHORIZATION, required = false) auth: String?,
    ): ResponseEntity<Any> {
        val userId = authSupport.requireUser(auth).userId
        val removed = decks.deleteByIdAndUserId(id, userId)
        return if (removed > 0) ResponseEntity.noContent().build() else ResponseEntity.notFound().build()
    }

    private data class ParsedDeck(val name: String, val format: String?)

    /** Pull the denormalized name/format out of a SharedDeck JSON body, or null if it isn't valid JSON. */
    private fun parse(body: String): ParsedDeck? = runCatching {
        val root = json.parseToJsonElement(body).jsonObject
        val name = root["name"]?.jsonPrimitive?.contentOrNull?.trim()?.ifBlank { null } ?: "Untitled deck"
        val format = root["format"]?.jsonPrimitive?.contentOrNull?.trim()?.takeIf { it.isNotBlank() }
        ParsedDeck(name, format)
    }.getOrNull()

    private fun DeckRow.toSummary() = DeckSummary(id = id!!, name = name, format = format, updatedAt = updatedAt.toString())

    /** Build the deck-detail object, embedding the stored deck JSON inline (no re-encoding). */
    private fun DeckRow.toDetailObject(): JsonObject = buildJsonObject {
        put("id", id)
        put("name", name)
        format?.let { put("format", it) }
        put("updatedAt", updatedAt.toString())
        put("deck", json.parseToJsonElement(data))
    }

    private fun DeckRow.toDetailJson(): String = json.encodeToString(JsonObject.serializer(), toDetailObject())
}
