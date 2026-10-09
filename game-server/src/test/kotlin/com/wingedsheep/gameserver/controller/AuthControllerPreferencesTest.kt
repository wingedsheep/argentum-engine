package com.wingedsheep.gameserver.controller

import com.wingedsheep.gameserver.auth.AuthClaims
import com.wingedsheep.gameserver.auth.AuthSupport
import com.wingedsheep.gameserver.auth.EmailService
import com.wingedsheep.gameserver.auth.MagicLinkService
import com.wingedsheep.gameserver.persistence.UserRow
import com.wingedsheep.gameserver.session.SessionRegistry
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.util.UUID

/**
 * The account's preferences are an opaque client document: stored verbatim when it is a small JSON
 * object, rejected otherwise, and read back as `{}` before anything was saved.
 */
class AuthControllerPreferencesTest : FunSpec({

    val userId = UUID.randomUUID()
    val header = "Bearer token"

    fun setup(stored: String? = null): Pair<AuthController, MagicLinkService> {
        val magicLinks = mockk<MagicLinkService>()
        val user = UserRow(id = userId, email = "a@b.co", displayName = "A", preferences = stored)
        every { magicLinks.findUser(userId) } returns user
        val saved = slot<String>()
        every { magicLinks.updatePreferences(userId, capture(saved)) } answers { user.copy(preferences = saved.captured) }
        val auth = mockk<AuthSupport>()
        every { auth.requireUser(header) } returns AuthClaims(uid = userId.toString(), email = "a@b.co", exp = Long.MAX_VALUE)
        return AuthController(magicLinks, auth, mockk<EmailService>(), SessionRegistry(), mockk(), false) to magicLinks
    }

    test("an account with no saved preferences reads as an empty object") {
        val (c, _) = setup()
        c.preferences(header).body shouldBe "{}"
    }

    test("saved preferences are returned verbatim") {
        val (c, _) = setup(stored = """{"version":1}""")
        c.preferences(header).body shouldBe """{"version":1}"""
    }

    test("a JSON object is stored") {
        val (c, magicLinks) = setup()
        c.updatePreferences(header, """{ "version": 1, "gameplay": { "autoTap": false } }""").statusCode.value() shouldBe 204
        verify { magicLinks.updatePreferences(userId, """{"version":1,"gameplay":{"autoTap":false}}""") }
    }

    test("a body that is not a JSON object is rejected") {
        val (c, magicLinks) = setup()
        c.updatePreferences(header, "[1,2]").statusCode.value() shouldBe 400
        c.updatePreferences(header, "not json").statusCode.value() shouldBe 400
        verify(exactly = 0) { magicLinks.updatePreferences(any(), any()) }
    }

    test("an oversized body is rejected") {
        val (c, magicLinks) = setup()
        val big = """{"x":"${"a".repeat(AuthController.MAX_PREFERENCES_BYTES)}"}"""
        c.updatePreferences(header, big).statusCode.value() shouldBe 400
        verify(exactly = 0) { magicLinks.updatePreferences(any(), any()) }
    }
})
