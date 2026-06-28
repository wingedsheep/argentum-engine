package com.wingedsheep.gameserver.auth

import com.wingedsheep.gameserver.config.AccountsProperties
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.util.Base64
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/** The verified contents of an auth token. */
@Serializable
data class AuthClaims(
    /** Account id as a UUID string (kotlinx-serialized into the token). Use [userId] for the parsed value. */
    val uid: String,
    val email: String,
    val exp: Long,
) {
    val userId: UUID get() = UUID.fromString(uid)
}

/**
 * Mints and verifies stateless, HMAC-SHA256-signed auth tokens (a minimal JWT-shape:
 * `base64url(payload).base64url(signature)`), so REST endpoints can authenticate without a database
 * lookup per request. The same token is presented by the web client and, optionally, on the
 * WebSocket Connect handshake to link an in-game identity to its account.
 *
 * We roll our own rather than pull in a JWT library: the payload is three fields, and self-signing
 * keeps the dependency surface (and attack surface) minimal.
 */
@Component
@ConditionalOnProperty(name = ["accounts.enabled"], havingValue = "true")
class AuthTokenService(props: AccountsProperties) {

    private val logger = LoggerFactory.getLogger(AuthTokenService::class.java)
    private val ttlSeconds = props.auth.tokenTtlHours * 3600
    private val urlEncoder = Base64.getUrlEncoder().withoutPadding()
    private val urlDecoder = Base64.getUrlDecoder()
    private val json = Json { ignoreUnknownKeys = true }

    private val secretKey: ByteArray = props.auth.secret.takeIf { it.isNotBlank() }
        ?.toByteArray(Charsets.UTF_8)
        ?: ByteArray(32).also {
            SecureRandom().nextBytes(it)
            logger.warn(
                "accounts.auth.secret is not set — generated a random signing secret. " +
                    "Auth tokens will be invalidated on restart. Set ACCOUNTS_AUTH_SECRET in production."
            )
        }

    /** Issue a signed token for the given user, valid for the configured TTL. */
    fun issue(userId: UUID, email: String, now: Instant = Instant.now()): String {
        val claims = AuthClaims(uid = userId.toString(), email = email, exp = now.epochSecond + ttlSeconds)
        val payload = urlEncoder.encodeToString(json.encodeToString(AuthClaims.serializer(), claims).toByteArray())
        return "$payload.${sign(payload)}"
    }

    /** Verify a token's signature and expiry. Returns the claims, or null if invalid/expired. */
    fun verify(token: String, now: Instant = Instant.now()): AuthClaims? {
        val parts = token.split('.')
        if (parts.size != 2) return null
        val (payload, signature) = parts
        if (!constantTimeEquals(signature, sign(payload))) return null
        val claims = runCatching {
            json.decodeFromString(AuthClaims.serializer(), String(urlDecoder.decode(payload)))
        }.getOrNull() ?: return null
        if (claims.exp < now.epochSecond) return null
        return claims
    }

    private fun sign(payload: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secretKey, "HmacSHA256"))
        return urlEncoder.encodeToString(mac.doFinal(payload.toByteArray(Charsets.UTF_8)))
    }

    private fun constantTimeEquals(a: String, b: String): Boolean =
        MessageDigest.isEqual(a.toByteArray(Charsets.UTF_8), b.toByteArray(Charsets.UTF_8))
}
