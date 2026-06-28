package com.wingedsheep.gameserver.friends

import com.wingedsheep.gameserver.persistence.FriendshipRepository
import com.wingedsheep.gameserver.persistence.FriendshipRow
import com.wingedsheep.gameserver.persistence.FriendshipStatus
import com.wingedsheep.gameserver.persistence.UserRepository
import com.wingedsheep.gameserver.persistence.UserRow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.util.Optional
import java.util.UUID

/**
 * The friend request → accept lifecycle and the online-status derivation, with the repositories and
 * presence layer mocked. These pin the request/accept/decline/unfriend rules and the privacy behaviour
 * of [PresenceService.isVisiblyOnline].
 */
class FriendsServiceTest : FunSpec({

    val alice: UUID = UUID.fromString("00000000-0000-0000-0000-0000000a11ce")
    val bob: UUID = UUID.fromString("00000000-0000-0000-0000-00000000ab0b")

    lateinit var friendships: FriendshipRepository
    lateinit var users: UserRepository
    lateinit var presence: PresenceService
    lateinit var broadcaster: FriendPresenceBroadcaster
    lateinit var service: FriendsService

    fun user(id: UUID, name: String, hidePresence: Boolean = false) =
        UserRow(id = id, email = "$name@test", displayName = name, hidePresence = hidePresence)

    beforeTest {
        friendships = mockk(relaxed = true)
        users = mockk()
        presence = mockk()
        broadcaster = mockk(relaxed = true)
        service = FriendsService(friendships, users, presence, broadcaster)
    }

    // ---- sendRequest ----

    test("cannot friend yourself") {
        service.sendRequest(alice, alice) shouldBe FriendsService.SendResult.SelfRequest
    }

    test("unknown target is rejected") {
        every { users.findById(bob) } returns Optional.empty()
        service.sendRequest(alice, bob) shouldBe FriendsService.SendResult.UnknownUser
    }

    test("a fresh request is created and the target is notified") {
        every { users.findById(bob) } returns Optional.of(user(bob, "Bob"))
        every { users.findById(alice) } returns Optional.of(user(alice, "Alice"))
        every { friendships.findPair(alice, bob) } returns null
        val savedId = UUID.randomUUID()
        every { friendships.save(any()) } answers { (firstArg() as FriendshipRow).copy(id = savedId) }

        val result = service.sendRequest(alice, bob)

        result.shouldBeInstanceOf<FriendsService.SendResult.Created>()
        result.request.accountId shouldBe bob
        verify { broadcaster.notifyRequestReceived(bob, alice, "Alice") }
    }

    test("an existing accepted friendship reports AlreadyFriends") {
        every { users.findById(bob) } returns Optional.of(user(bob, "Bob"))
        every { friendships.findPair(alice, bob) } returns
            FriendshipRow(id = UUID.randomUUID(), requesterId = alice, addresseeId = bob, status = "ACCEPTED")
        service.sendRequest(alice, bob) shouldBe FriendsService.SendResult.AlreadyFriends
    }

    test("a duplicate outgoing request reports AlreadyRequested") {
        every { users.findById(bob) } returns Optional.of(user(bob, "Bob"))
        every { friendships.findPair(alice, bob) } returns
            FriendshipRow(id = UUID.randomUUID(), requesterId = alice, addresseeId = bob, status = "PENDING")
        service.sendRequest(alice, bob) shouldBe FriendsService.SendResult.AlreadyRequested
    }

    test("a reverse pending request points you at accepting theirs") {
        val reqId = UUID.randomUUID()
        every { users.findById(bob) } returns Optional.of(user(bob, "Bob"))
        every { friendships.findPair(alice, bob) } returns
            FriendshipRow(id = reqId, requesterId = bob, addresseeId = alice, status = "PENDING")
        service.sendRequest(alice, bob) shouldBe FriendsService.SendResult.IncomingPending(reqId)
    }

    // ---- accept / remove / unfriend ----

    test("the addressee can accept a pending request; presence is exchanged") {
        val reqId = UUID.randomUUID()
        every { friendships.findById(reqId) } returns
            Optional.of(FriendshipRow(id = reqId, requesterId = bob, addresseeId = alice, status = "PENDING"))
        val saved = slot<FriendshipRow>()
        every { friendships.save(capture(saved)) } answers { saved.captured }

        service.accept(alice, reqId) shouldBe true
        saved.captured.status shouldBe FriendshipStatus.ACCEPTED.name
        verify { broadcaster.exchangePresence(bob, alice) }
    }

    test("a non-addressee cannot accept") {
        val reqId = UUID.randomUUID()
        every { friendships.findById(reqId) } returns
            Optional.of(FriendshipRow(id = reqId, requesterId = bob, addresseeId = alice, status = "PENDING"))
        // Bob is the requester, not the addressee.
        service.accept(bob, reqId) shouldBe false
        verify(exactly = 0) { friendships.save(any()) }
    }

    test("either party can remove a pending request") {
        val reqId = UUID.randomUUID()
        every { friendships.findById(reqId) } returns
            Optional.of(FriendshipRow(id = reqId, requesterId = alice, addresseeId = bob, status = "PENDING"))
        service.removeRequest(alice, reqId) shouldBe true
        verify { friendships.deleteById(reqId) }
    }

    test("an unrelated user cannot remove a request") {
        val reqId = UUID.randomUUID()
        val stranger = UUID.randomUUID()
        every { friendships.findById(reqId) } returns
            Optional.of(FriendshipRow(id = reqId, requesterId = alice, addresseeId = bob, status = "PENDING"))
        service.removeRequest(stranger, reqId) shouldBe false
    }

    test("unfriend deletes an accepted pair in either direction") {
        val rowId = UUID.randomUUID()
        every { friendships.findPair(alice, bob) } returns
            FriendshipRow(id = rowId, requesterId = bob, addresseeId = alice, status = "ACCEPTED")
        service.unfriend(alice, bob) shouldBe true
        verify { friendships.deleteById(rowId) }
    }

    test("unfriend on a still-pending pair is a no-op") {
        every { friendships.findPair(alice, bob) } returns
            FriendshipRow(id = UUID.randomUUID(), requesterId = bob, addresseeId = alice, status = "PENDING")
        service.unfriend(alice, bob) shouldBe false
        verify(exactly = 0) { friendships.deleteById(any<UUID>()) }
    }

    // ---- listFriends presence ----

    test("listFriends reports each friend's visible-online status") {
        every { friendships.findByRequesterIdOrAddresseeId(alice, alice) } returns listOf(
            FriendshipRow(id = UUID.randomUUID(), requesterId = alice, addresseeId = bob, status = "ACCEPTED"),
        )
        every { users.findAllById(listOf(bob)) } returns listOf(user(bob, "Bob"))
        every { presence.isVisiblyOnline(bob, false) } returns true

        val friends = service.listFriends(alice)
        friends.size shouldBe 1
        friends[0].accountId shouldBe bob
        friends[0].online shouldBe true
    }

    // ---- setHidePresence ----

    test("setHidePresence persists the flag and rebroadcasts presence") {
        every { users.findById(alice) } returns Optional.of(user(alice, "Alice", hidePresence = false))
        every { users.save(any()) } answers { firstArg() }

        service.setHidePresence(alice, true)

        verify { users.save(match { it.hidePresence }) }
        verify { broadcaster.broadcastOwnPresence(alice) }
    }
})
