package com.wingedsheep.gameserver.controller

import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.registry.PrintingRegistry
import com.wingedsheep.gameserver.auth.AuthClaims
import com.wingedsheep.gameserver.auth.AuthSupport
import com.wingedsheep.gameserver.auth.MagicLinkService
import com.wingedsheep.gameserver.persistence.UserRow
import com.wingedsheep.gameserver.profile.AvatarValidator
import com.wingedsheep.gameserver.profile.CardArtAvatar
import com.wingedsheep.gameserver.session.PlayerIdentity
import com.wingedsheep.gameserver.session.SessionRegistry
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID

/**
 * Picking an avatar: a preset id or a crop of a catalogued card's art is accepted, anything else is
 * not; null goes back to the initial; and a connected tab's identity picks the new avatar up straight
 * away (it is what the next game seats).
 */
class AuthControllerAvatarTest : FunSpec({

    val userId = UUID.randomUUID()
    val boltPath = "front/e/3/e3285e6b-3e79-4d7c-bf96-d920f973b80c"

    fun setup(): Triple<AuthController, MagicLinkService, SessionRegistry> {
        val magicLinks = mockk<MagicLinkService>()
        every { magicLinks.updateAvatar(userId, any()) } answers {
            UserRow(id = userId, email = "a@b.co", displayName = "Ava", avatar = secondArg())
        }
        val auth = mockk<AuthSupport>()
        every { auth.requireUser(any()) } returns AuthClaims(userId.toString(), "a@b.co", Long.MAX_VALUE)
        val bolt = mockk<CardDefinition>(relaxed = true)
        every { bolt.metadata.imageUri } returns "https://cards.scryfall.io/normal/$boltPath.jpg?1628801678"
        every { bolt.backFace } returns null
        val cards = mockk<CardRegistry>()
        every { cards.allCardNames() } returns setOf("Lightning Bolt")
        every { cards.getCard("Lightning Bolt") } returns bolt
        val printings = mockk<PrintingRegistry>()
        every { printings.printingsOf(any()) } returns emptyList()
        val registry = SessionRegistry()
        val controller = AuthController(magicLinks, auth, mockk(), registry, AvatarValidator(cards, printings), false)
        return Triple(controller, magicLinks, registry)
    }

    fun AuthController.pick(avatar: String?) = updateAvatar("Bearer t", AuthController.UpdateAvatarBody(avatar))

    test("a preset avatar is saved and returned on the account") {
        val (controller, _, _) = setup()
        val res = controller.pick("dragon-tyrant")
        res.statusCode.value() shouldBe 200
        (res.body as AuthController.UserDto).avatar shouldBe "dragon-tyrant"
    }

    test("a crop of a catalogued card's art is accepted") {
        val (controller, _, _) = setup()
        val value = CardArtAvatar.format(0.4, 0.1, 0.6, boltPath)
        val res = controller.pick(value)
        res.statusCode.value() shouldBe 200
        (res.body as AuthController.UserDto).avatar shouldBe "card:0.400,0.100,0.600:$boltPath"
    }

    test("art the catalog doesn't have, a malformed crop, or an unknown id is rejected") {
        val (controller, magicLinks, _) = setup()
        for (bad in listOf(
            "../../etc/passwd",
            "card:0.4,0.1,0.6:front/0/0/00000000-0000-0000-0000-000000000000",
            "card:0.4,0.6,0.6:$boltPath", // runs off the bottom of the art
            "card:0,0,0.05:$boltPath", // smaller than the minimum crop
            "card:0.4,0.1,0.6:https://evil.example/x.jpg",
        )) {
            controller.pick(bad).statusCode.value() shouldBe 400
        }
        verify(exactly = 0) { magicLinks.updateAvatar(any(), any()) }
    }

    test("null clears the avatar") {
        val (controller, _, _) = setup()
        (controller.pick(null).body as AuthController.UserDto).avatar shouldBe null
    }

    test("a connected identity of the account picks the new avatar up") {
        val (controller, _, registry) = setup()
        val mine = PlayerIdentity(playerId = EntityId("p1"), playerName = "Ava").apply { this.userId = userId }
        val someoneElse = PlayerIdentity(playerId = EntityId("p2"), playerName = "Bo").apply { this.userId = UUID.randomUUID() }
        registry.preRegisterIdentity(mine)
        registry.preRegisterIdentity(someoneElse)

        controller.pick("elf-druid")

        mine.avatar shouldBe "elf-druid"
        someoneElse.avatar shouldBe null
    }
})
