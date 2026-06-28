package com.wingedsheep.gameserver.auth

import com.wingedsheep.gameserver.config.AccountsProperties
import com.wingedsheep.gameserver.persistence.LoginTokenRepository
import com.wingedsheep.gameserver.persistence.LoginTokenRow
import com.wingedsheep.gameserver.persistence.UserRepository
import com.wingedsheep.gameserver.persistence.UserRow
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.util.Base64
import java.util.UUID

/** Raised when a magic-link token is missing, expired, already used, or unknown. */
class InvalidLoginTokenException(message: String) : RuntimeException(message)

/** Result of a successful verification: the account plus a freshly minted auth token. */
data class LoginResult(val user: UserRow, val authToken: String)

/**
 * Orchestrates passwordless magic-link login:
 *  - [requestLogin] upserts the account and emails a single-use link,
 *  - [verify] validates the link's token and issues a durable auth token.
 *
 * Only the SHA-256 hash of the login token is stored; the raw token lives only in the emailed link.
 */
@Service
@ConditionalOnProperty(name = ["accounts.enabled"], havingValue = "true")
class MagicLinkService(
    private val users: UserRepository,
    private val loginTokens: LoginTokenRepository,
    private val emailService: EmailService,
    private val authTokenService: AuthTokenService,
    private val props: AccountsProperties,
) {
    private val logger = LoggerFactory.getLogger(MagicLinkService::class.java)
    private val random = SecureRandom()
    private val encoder = Base64.getUrlEncoder().withoutPadding()

    private val emailRegex = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

    /** Upsert the account for [rawEmail] and email it a single-use sign-in link. Idempotent-ish. */
    fun requestLogin(rawEmail: String, now: Instant = Instant.now()) {
        val email = rawEmail.trim().lowercase()
        require(emailRegex.matches(email)) { "Invalid email address" }

        val user = users.findByEmail(email)
            ?: users.save(UserRow(email = email, displayName = email.substringBefore('@')))

        val rawToken = ByteArray(32).also(random::nextBytes).let(encoder::encodeToString)
        loginTokens.save(
            LoginTokenRow(
                userId = user.id!!,
                tokenHash = sha256Hex(rawToken),
                expiresAt = now.plusSeconds(props.auth.loginTokenTtlMinutes * 60),
            )
        )

        val link = "${props.auth.baseUrl.trimEnd('/')}/login/verify?token=$rawToken"
        emailService.sendMagicLink(email, link)
        logger.info("Issued magic-link login token for {}", email)
    }

    /** Validate [rawToken] from a magic link, consume it, and mint an auth token. */
    fun verify(rawToken: String, now: Instant = Instant.now()): LoginResult {
        val record = loginTokens.findByTokenHash(sha256Hex(rawToken))
            ?: throw InvalidLoginTokenException("Unknown sign-in link")
        if (record.consumedAt != null) throw InvalidLoginTokenException("This sign-in link has already been used")
        if (record.expiresAt.isBefore(now)) throw InvalidLoginTokenException("This sign-in link has expired")

        loginTokens.save(record.copy(consumedAt = now))
        val user = users.findById(record.userId).orElseThrow {
            InvalidLoginTokenException("Account no longer exists")
        }
        return LoginResult(user, authTokenService.issue(user.id!!, user.email, now))
    }

    fun findUser(userId: UUID): UserRow? = users.findById(userId).orElse(null)

    /**
     * Set a user's chosen display name. The email stays the immutable identity; the display name is a
     * free-form label (duplicates across accounts are allowed). Returns the updated account, or null if
     * it no longer exists. Caller is responsible for trimming/length-validating [displayName].
     */
    fun updateDisplayName(userId: UUID, displayName: String): UserRow? {
        val user = users.findById(userId).orElse(null) ?: return null
        return users.save(user.copy(displayName = displayName))
    }

    private fun sha256Hex(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
