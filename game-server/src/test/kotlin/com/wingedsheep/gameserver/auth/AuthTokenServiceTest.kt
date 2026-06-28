package com.wingedsheep.gameserver.auth

import com.wingedsheep.gameserver.config.AccountsProperties
import com.wingedsheep.gameserver.config.AuthProperties
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.time.Instant
import java.util.UUID

class AuthTokenServiceTest : FunSpec({

    val userId: UUID = UUID.fromString("11111111-1111-1111-1111-111111111111")

    fun service(secret: String = "a-fixed-secret-used-only-in-tests", ttlHours: Long = 1) =
        AuthTokenService(
            AccountsProperties(
                enabled = true,
                auth = AuthProperties(secret = secret, tokenTtlHours = ttlHours),
            )
        )

    test("issues a token that verifies back to the same claims") {
        val svc = service()
        val claims = svc.verify(svc.issue(userId, "player@example.com"))
        claims.shouldNotBeNull()
        claims.userId shouldBe userId
        claims.email shouldBe "player@example.com"
    }

    test("rejects a token after it expires") {
        val svc = service(ttlHours = 1)
        val now = Instant.parse("2026-01-01T00:00:00Z")
        val token = svc.issue(userId, "p@example.com", now)
        svc.verify(token, now.plusSeconds(3599)).shouldNotBeNull() // still valid just before 1h
        svc.verify(token, now.plusSeconds(3601)).shouldBeNull() // expired just after 1h
    }

    test("rejects a token with a tampered payload") {
        val svc = service()
        val token = svc.issue(userId, "p@example.com")
        val payload = token.substringBefore('.')
        val tampered = "${payload}x.${token.substringAfter('.')}"
        svc.verify(tampered).shouldBeNull()
    }

    test("rejects a token signed with a different secret") {
        val token = service(secret = "secret-one").issue(userId, "p@example.com")
        service(secret = "secret-two").verify(token).shouldBeNull()
    }

    test("rejects malformed tokens") {
        val svc = service()
        svc.verify("not-a-token").shouldBeNull()
        svc.verify("only.two.parts").shouldBeNull()
        svc.verify("").shouldBeNull()
    }
})
