package com.wingedsheep.gameserver.controller

import com.wingedsheep.gameserver.auth.AuthSupport
import com.wingedsheep.gameserver.persistence.CubeRepository
import com.wingedsheep.gameserver.persistence.CubeRow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
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
 * CRUD for a signed-in user's saved cubes — the cube twin of [AccountDeckController], deliberately
 * the same shape so the client's cloud/local merge (`useUnifiedCubes`) can mirror `useUnifiedDecks`.
 *
 * The cube body is the client's `SharedCube` JSON, stored verbatim in [CubeRow.data]; `name` and the
 * total card count are denormalized out of it for cheap list views. Resolution against the card
 * registry deliberately does *not* happen here: a cube is a list of names until a lobby resolves it
 * (`CubeResolver`), so a cube can be saved while some of its cards are still unimplemented and stays
 * valid as more sets land. All operations are scoped to the authenticated user.
 */
@RestController
@RequestMapping("/api/account/cubes")
@ConditionalOnProperty(name = ["accounts.enabled"], havingValue = "true")
class AccountCubeController(
    private val cubes: CubeRepository,
    private val authSupport: AuthSupport,
) {
    data class CubeSummary(val id: Long, val name: String, val cardCount: Int, val updatedAt: String)

    private val json = Json { ignoreUnknownKeys = true }

    @GetMapping
    fun list(@RequestHeader(HttpHeaders.AUTHORIZATION, required = false) auth: String?): List<CubeSummary> {
        val userId = authSupport.requireUser(auth).userId
        return cubes.findByUserIdOrderByUpdatedAtDesc(userId).map { it.toSummary() }
    }

    /**
     * Full detail for every cube in one round-trip (`GET /api/account/cubes?full`), so the cube
     * picker can show card counts and colours for cloud cubes without an N+1 of per-cube `GET /{id}`.
     */
    @GetMapping(params = ["full"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun listFull(@RequestHeader(HttpHeaders.AUTHORIZATION, required = false) auth: String?): ResponseEntity<Any> {
        val userId = authSupport.requireUser(auth).userId
        val rows = cubes.findByUserIdOrderByUpdatedAtDesc(userId)
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
        val cube = cubes.findByIdAndUserId(id, userId) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(cube.toDetailJson())
    }

    @PostMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun create(
        @RequestBody body: String,
        @RequestHeader(HttpHeaders.AUTHORIZATION, required = false) auth: String?,
    ): ResponseEntity<Any> {
        val userId = authSupport.requireUser(auth).userId
        val parsed = parse(body) ?: return ResponseEntity.badRequest().body(mapOf("error" to "Invalid cube JSON"))
        val now = Instant.now()
        val saved = cubes.save(
            CubeRow(userId = userId, name = parsed.name, cardCount = parsed.cardCount, data = body, createdAt = now, updatedAt = now)
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
        val existing = cubes.findByIdAndUserId(id, userId) ?: return ResponseEntity.notFound().build()
        val parsed = parse(body) ?: return ResponseEntity.badRequest().body(mapOf("error" to "Invalid cube JSON"))
        val saved = cubes.save(
            existing.copy(name = parsed.name, cardCount = parsed.cardCount, data = body, updatedAt = Instant.now())
        )
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(saved.toDetailJson())
    }

    @DeleteMapping("/{id}")
    fun delete(
        @PathVariable id: Long,
        @RequestHeader(HttpHeaders.AUTHORIZATION, required = false) auth: String?,
    ): ResponseEntity<Any> {
        val userId = authSupport.requireUser(auth).userId
        val removed = cubes.deleteByIdAndUserId(id, userId)
        return if (removed > 0) ResponseEntity.noContent().build() else ResponseEntity.notFound().build()
    }

    private data class ParsedCube(val name: String, val cardCount: Int)

    /**
     * Pull the denormalized name + total card count out of a SharedCube JSON body, or null if it
     * isn't valid JSON. `cards` is an array of `{ name, count }` entries; a missing count means 1.
     */
    private fun parse(body: String): ParsedCube? = runCatching {
        val root = json.parseToJsonElement(body).jsonObject
        val name = root["name"]?.jsonPrimitive?.contentOrNull?.trim()?.ifBlank { null } ?: "Untitled cube"
        val cardCount = root["cards"]?.jsonArray?.sumOf { entry ->
            entry.jsonObject["count"]?.jsonPrimitive?.intOrNull ?: 1
        } ?: 0
        ParsedCube(name, cardCount)
    }.getOrNull()

    private fun CubeRow.toSummary() =
        CubeSummary(id = id!!, name = name, cardCount = cardCount, updatedAt = updatedAt.toString())

    /** Build the cube-detail object, embedding the stored cube JSON inline (no re-encoding). */
    private fun CubeRow.toDetailObject(): JsonObject = buildJsonObject {
        put("id", id)
        put("name", name)
        put("cardCount", cardCount)
        put("updatedAt", updatedAt.toString())
        put("cube", json.parseToJsonElement(data))
    }

    private fun CubeRow.toDetailJson(): String = json.encodeToString(JsonObject.serializer(), toDetailObject())
}
