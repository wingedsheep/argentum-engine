package com.wingedsheep.gameserver.auth

import com.wingedsheep.gameserver.config.AccountsProperties
import com.wingedsheep.gameserver.config.AuthProperties
import com.wingedsheep.gameserver.persistence.LoginTokenRepository
import com.wingedsheep.gameserver.persistence.LoginTokenRow
import com.wingedsheep.gameserver.persistence.UserRepository
import com.wingedsheep.gameserver.persistence.UserRow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.security.MessageDigest
import java.time.Instant
import java.util.Optional
import java.util.UUID

private fun sha256Hex(value: String): String =
    MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

class MagicLinkServiceTest : FunSpec({

    lateinit var users: UserRepository
    lateinit var loginTokens: LoginTokenRepository
    lateinit var email: EmailService
    lateinit var authToken: AuthTokenService
    lateinit var service: MagicLinkService

    val props = AccountsProperties(
        enabled = true,
        auth = AuthProperties(baseUrl = "https://app.test/", loginTokenTtlMinutes = 15),
    )

    val u7: UUID = UUID.fromString("00000000-0000-0000-0000-000000000007")
    val u3: UUID = UUID.fromString("00000000-0000-0000-0000-000000000003")
    val u5: UUID = UUID.fromString("00000000-0000-0000-0000-000000000005")

    beforeTest {
        users = mockk()
        loginTokens = mockk(relaxed = true)
        email = mockk(relaxed = true)
        authToken = mockk()
        // save() is generic (<S> save(S): S); a relaxed return would CHECKCAST-fail. Echo the arg.
        every { loginTokens.save(any()) } answers { firstArg() }
        service = MagicLinkService(users, loginTokens, email, authToken, props)
    }

    test("requestLogin creates a new account and emails a link") {
        every { users.findByEmail("new@example.com") } returns null
        every { users.save(any()) } returns UserRow(id = u7, email = "new@example.com", displayName = "new")

        service.requestLogin("  New@Example.com ") // trimmed + lowercased

        verify { users.save(match { it.email == "new@example.com" && it.displayName == "new" }) }
        verify { loginTokens.save(match { it.userId == u7 && it.tokenHash.isNotBlank() }) }
        val link = slot<String>()
        verify { email.sendMagicLink(eq("new@example.com"), capture(link)) }
        link.captured shouldStartWith "https://app.test/login/verify?token="
    }

    test("requestLogin reuses an existing account") {
        every { users.findByEmail("e@example.com") } returns UserRow(id = u3, email = "e@example.com", displayName = "e")

        service.requestLogin("e@example.com")

        verify(exactly = 0) { users.save(any()) }
        verify { loginTokens.save(match { it.userId == u3 }) }
    }

    test("requestLogin rejects an invalid email") {
        shouldThrow<IllegalArgumentException> { service.requestLogin("not-an-email") }
    }

    test("verify consumes a valid token and issues an auth token") {
        val raw = "raw-login-token"
        val now = Instant.parse("2026-01-01T00:00:00Z")
        val record = LoginTokenRow(
            id = 1, userId = u5, tokenHash = sha256Hex(raw), expiresAt = now.plusSeconds(600),
        )
        every { loginTokens.findByTokenHash(sha256Hex(raw)) } returns record
        every { users.findById(u5) } returns Optional.of(UserRow(id = u5, email = "u@example.com", displayName = "u"))
        every { authToken.issue(u5, "u@example.com", now) } returns "signed-token"

        val result = service.verify(raw, now)

        result.authToken shouldBe "signed-token"
        result.user.id shouldBe u5
        verify { loginTokens.save(match { it.consumedAt != null }) }
    }

    test("verify rejects an expired token") {
        val raw = "raw"
        val now = Instant.parse("2026-01-01T00:00:00Z")
        every { loginTokens.findByTokenHash(sha256Hex(raw)) } returns
            LoginTokenRow(id = 1, userId = u5, tokenHash = sha256Hex(raw), expiresAt = now.minusSeconds(1))

        shouldThrow<InvalidLoginTokenException> { service.verify(raw, now) }
    }

    test("verify rejects an already-used token") {
        val raw = "raw"
        val now = Instant.parse("2026-01-01T00:00:00Z")
        every { loginTokens.findByTokenHash(sha256Hex(raw)) } returns
            LoginTokenRow(
                id = 1, userId = u5, tokenHash = sha256Hex(raw),
                expiresAt = now.plusSeconds(600), consumedAt = now.minusSeconds(10),
            )

        shouldThrow<InvalidLoginTokenException> { service.verify(raw, now) }
    }

    test("verify rejects an unknown token") {
        every { loginTokens.findByTokenHash(any()) } returns null
        shouldThrow<InvalidLoginTokenException> { service.verify("whatever") }
    }
})
