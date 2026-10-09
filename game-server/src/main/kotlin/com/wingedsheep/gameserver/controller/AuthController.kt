package com.wingedsheep.gameserver.controller

import com.fasterxml.jackson.annotation.JsonProperty
import com.wingedsheep.gameserver.auth.AuthSupport
import com.wingedsheep.gameserver.auth.EmailService
import com.wingedsheep.gameserver.auth.InvalidLoginTokenException
import com.wingedsheep.gameserver.auth.MagicLinkService
import com.wingedsheep.gameserver.persistence.UserRow
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/**
 * Passwordless magic-link auth. Only mounted when accounts are enabled.
 *
 *  - POST /api/auth/request-login  { email }            → emails a sign-in link (always 200, even if
 *                                                          the email is unknown, to avoid leaking who
 *                                                          has an account)
 *  - POST /api/auth/verify         { token }            → { authToken, user }
 *  - GET  /api/auth/me             (Bearer authToken)   → { user }
 *  - GET/PUT /api/auth/me/preferences (Bearer authToken) → the client's preferences JSON, verbatim
 *
 * **Dev sign-in.** With no mail configured the link is only logged, which makes signing in locally
 * a hunt through the server log for a link that points at whichever port `base-url` names. When
 * `game.dev-endpoints.enabled` is also on, `request-login` returns the link's path as `devLoginPath`
 * and the login modal offers to follow it directly. Both conditions are required: a server that can
 * send mail never returns it, and neither does one without dev endpoints — which production never
 * enables — since handing the link to whoever typed the address would make the email meaningless.
 */
@RestController
@RequestMapping("/api/auth")
@ConditionalOnProperty(name = ["accounts.enabled"], havingValue = "true")
class AuthController(
    private val magicLinkService: MagicLinkService,
    private val authSupport: AuthSupport,
    private val emailService: EmailService,
    @Value("\${game.dev-endpoints.enabled:false}") private val devEndpointsEnabled: Boolean,
) {
    data class RequestLoginBody(val email: String)
    data class VerifyBody(val token: String)
    data class UpdateProfileBody(val displayName: String)

    /**
     * The signed-in account as seen by the client. [id] (a UUID) doubles as the shareable "friend
     * code" — you invite a friend by handing them this id, never your email. [hidePresence] mirrors the
     * presence opt-out toggled from the friends page.
     */
    data class UserDto(
        val id: UUID,
        val email: String,
        val displayName: String,
        // No jackson-module-kotlin is on the classpath, so Jackson serializes via the JavaBean
        // `isAdmin()` getter and would emit the key `admin` — but the client reads `user.isAdmin`
        // (it gates the Admin button). Pin the wire name so a promoted account is seen as admin.
        @JsonProperty("isAdmin") val isAdmin: Boolean,
        val hidePresence: Boolean,
    )
    data class LoginResponse(val authToken: String, val user: UserDto)

    companion object {
        const val MAX_DISPLAY_NAME_LENGTH = 40
        /** A course of a handful of missions is a few hundred bytes; anything near this is not progress. */
        const val MAX_LEARN_PROGRESS_BYTES = 4096
        /** A few dozen toggles and numbers; anything near this is not preferences. */
        const val MAX_PREFERENCES_BYTES = 8192
    }

    @PostMapping("/request-login")
    fun requestLogin(@RequestBody body: RequestLoginBody): ResponseEntity<Any> {
        return try {
            val path = magicLinkService.requestLogin(body.email)
            if (devEndpointsEnabled && !emailService.canSend) {
                ResponseEntity.ok(mapOf("status" to "sent", "devLoginPath" to path))
            } else {
                ResponseEntity.ok(mapOf("status" to "sent"))
            }
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "Invalid email")))
        }
    }

    @PostMapping("/verify")
    fun verify(@RequestBody body: VerifyBody): ResponseEntity<Any> {
        return try {
            val result = magicLinkService.verify(body.token)
            ResponseEntity.ok(LoginResponse(result.authToken, result.user.toDto()))
        } catch (e: InvalidLoginTokenException) {
            ResponseEntity.status(401).body(mapOf("error" to (e.message ?: "Invalid sign-in link")))
        }
    }

    @GetMapping("/me")
    fun me(@RequestHeader(HttpHeaders.AUTHORIZATION, required = false) authorization: String?): ResponseEntity<Any> {
        val claims = authSupport.requireUser(authorization)
        val user = magicLinkService.findUser(claims.userId)
            ?: return ResponseEntity.status(401).body(mapOf("error" to "Account no longer exists"))
        return ResponseEntity.ok(user.toDto())
    }

    /** Update the signed-in account's display name (free-form label; the email stays the identity). */
    @PutMapping("/me")
    fun updateMe(
        @RequestHeader(HttpHeaders.AUTHORIZATION, required = false) authorization: String?,
        @RequestBody body: UpdateProfileBody,
    ): ResponseEntity<Any> {
        val claims = authSupport.requireUser(authorization)
        val name = body.displayName.trim()
        if (name.isEmpty() || name.length > MAX_DISPLAY_NAME_LENGTH) {
            return ResponseEntity.badRequest()
                .body(mapOf("error" to "Display name must be 1–$MAX_DISPLAY_NAME_LENGTH characters"))
        }
        val updated = magicLinkService.updateDisplayName(claims.userId, name)
            ?: return ResponseEntity.status(401).body(mapOf("error" to "Account no longer exists"))
        return ResponseEntity.ok(updated.toDto())
    }

    /**
     * The account's Learn to Play progress — the client's own JSON, returned verbatim, or `{}` when
     * the course was never started on this account. Guests keep the same document in localStorage;
     * the client merges the two on sign-in.
     */
    @GetMapping("/me/learn-progress", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun learnProgress(@RequestHeader(HttpHeaders.AUTHORIZATION, required = false) authorization: String?): ResponseEntity<Any> {
        val claims = authSupport.requireUser(authorization)
        val user = magicLinkService.findUser(claims.userId)
            ?: return ResponseEntity.status(401).body(mapOf("error" to "Account no longer exists"))
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(user.learnProgress ?: "{}")
    }

    /** Replace the account's Learn to Play progress. The body must be a JSON object, and small. */
    @PutMapping("/me/learn-progress", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun updateLearnProgress(
        @RequestHeader(HttpHeaders.AUTHORIZATION, required = false) authorization: String?,
        @RequestBody body: String,
    ): ResponseEntity<Any> = replaceDocument(authorization, body, MAX_LEARN_PROGRESS_BYTES, "Progress") { userId, json ->
        magicLinkService.updateLearnProgress(userId, json)
    }

    /**
     * The account's player preferences — the client's JSON, returned verbatim, or `{}` when none were
     * ever saved. Guests keep the same document in localStorage; the client reconciles the two on
     * sign-in (newest `updatedAt` wins).
     */
    @GetMapping("/me/preferences", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun preferences(@RequestHeader(HttpHeaders.AUTHORIZATION, required = false) authorization: String?): ResponseEntity<Any> {
        val claims = authSupport.requireUser(authorization)
        val user = magicLinkService.findUser(claims.userId)
            ?: return ResponseEntity.status(401).body(mapOf("error" to "Account no longer exists"))
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(user.preferences ?: "{}")
    }

    /** Replace the account's player preferences. The body must be a JSON object, and small. */
    @PutMapping("/me/preferences", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun updatePreferences(
        @RequestHeader(HttpHeaders.AUTHORIZATION, required = false) authorization: String?,
        @RequestBody body: String,
    ): ResponseEntity<Any> = replaceDocument(authorization, body, MAX_PREFERENCES_BYTES, "Preferences") { userId, json ->
        magicLinkService.updatePreferences(userId, json)
    }

    /**
     * Shared body of the opaque per-account JSON documents: authenticate, cap the size, require a JSON
     * object, store it normalized. [store] returns null when the account is gone.
     */
    private fun replaceDocument(
        authorization: String?,
        body: String,
        maxBytes: Int,
        label: String,
        store: (UUID, String) -> UserRow?,
    ): ResponseEntity<Any> {
        val claims = authSupport.requireUser(authorization)
        if (body.length > maxBytes) {
            return ResponseEntity.badRequest().body(mapOf("error" to "$label document too large"))
        }
        val parsed = runCatching { Json.parseToJsonElement(body) }.getOrNull()
        if (parsed !is JsonObject) {
            return ResponseEntity.badRequest().body(mapOf("error" to "$label must be a JSON object"))
        }
        store(claims.userId, parsed.toString())
            ?: return ResponseEntity.status(401).body(mapOf("error" to "Account no longer exists"))
        return ResponseEntity.noContent().build()
    }

    private fun UserRow.toDto() =
        UserDto(id = id!!, email = email, displayName = displayName, isAdmin = isAdmin, hidePresence = hidePresence)
}
