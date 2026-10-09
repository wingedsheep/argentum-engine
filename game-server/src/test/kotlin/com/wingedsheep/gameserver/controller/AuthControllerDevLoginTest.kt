package com.wingedsheep.gameserver.controller

import com.wingedsheep.gameserver.auth.AuthSupport
import com.wingedsheep.gameserver.auth.EmailService
import com.wingedsheep.gameserver.auth.MagicLinkService
import com.wingedsheep.gameserver.session.SessionRegistry
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

/**
 * The dev sign-in shortcut hands the magic link to whoever typed the address, so it must only
 * appear when *both* dev endpoints are on and the server has no mail to send.
 */
class AuthControllerDevLoginTest : FunSpec({

    fun controller(devEndpoints: Boolean, canSend: Boolean): AuthController {
        val magicLinks = mockk<MagicLinkService>()
        every { magicLinks.requestLogin(any(), any()) } returns "/login/verify?token=abc"
        val email = mockk<EmailService>()
        every { email.canSend } returns canSend
        return AuthController(magicLinks, mockk<AuthSupport>(), email, SessionRegistry(), mockk(), devEndpoints)
    }

    fun devPath(c: AuthController): Any? =
        (c.requestLogin(AuthController.RequestLoginBody("a@b.co")).body as Map<*, *>)["devLoginPath"]

    test("dev server without mail returns the link path") {
        devPath(controller(devEndpoints = true, canSend = false)) shouldBe "/login/verify?token=abc"
    }

    test("a server that can send mail never returns it") {
        devPath(controller(devEndpoints = true, canSend = true)) shouldBe null
    }

    test("without dev endpoints it is never returned") {
        devPath(controller(devEndpoints = false, canSend = false)) shouldBe null
    }
})
