package com.wingedsheep.gameserver.messages

import com.wingedsheep.gameserver.friends.PresenceService
import com.wingedsheep.gameserver.messages.DirectMessageService.SendResult
import com.wingedsheep.gameserver.messages.DirectMessageService.ThreadState
import com.wingedsheep.gameserver.persistence.DmMessageRow
import com.wingedsheep.gameserver.persistence.DmThreadRow
import com.wingedsheep.gameserver.persistence.FriendshipRepository
import com.wingedsheep.gameserver.persistence.FriendshipRow
import com.wingedsheep.gameserver.persistence.FriendshipStatus
import com.wingedsheep.gameserver.persistence.UserRepository
import com.wingedsheep.gameserver.persistence.UserRow
import com.wingedsheep.gameserver.protocol.ServerMessage
import com.wingedsheep.gameserver.session.UserSockets
import com.wingedsheep.gameserver.social.AccountBlockStore
import com.wingedsheep.gameserver.social.BlockService
import com.wingedsheep.gameserver.social.BlockedAccount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.Optional
import java.util.UUID

/**
 * The direct-message rules — friends talk freely, strangers go through a capped message request,
 * blocks hide everything, deleting is one-sided — against an in-memory store with a controllable clock.
 */
class DirectMessageServiceTest : FunSpec({

    val alice: UUID = UUID.fromString("00000000-0000-0000-0000-0000000a11ce")
    val bob: UUID = UUID.fromString("00000000-0000-0000-0000-00000000ab0b")
    val carol: UUID = UUID.fromString("00000000-0000-0000-0000-0000000ca201")

    lateinit var store: InMemoryDirectMessageStore
    lateinit var blockStore: InMemoryBlockStore
    lateinit var friendships: FriendshipRepository
    lateinit var sockets: UserSockets
    lateinit var service: DirectMessageService
    var now = Instant.parse("2026-10-09T12:00:00Z")

    fun tick(seconds: Long = 1) {
        now = now.plusSeconds(seconds)
        service.clock = Clock.fixed(now, ZoneOffset.UTC)
    }

    fun befriend(a: UUID, b: UUID) {
        val row = FriendshipRow(UUID.randomUUID(), a, b, FriendshipStatus.ACCEPTED.name)
        every { friendships.findPair(a, b) } returns row
        every { friendships.findPair(b, a) } returns row
        every { friendships.findByRequesterIdOrAddresseeId(a, a) } returns listOf(row)
        every { friendships.findByRequesterIdOrAddresseeId(b, b) } returns listOf(row)
    }

    beforeTest {
        store = InMemoryDirectMessageStore()
        blockStore = InMemoryBlockStore()
        val users = mockk<UserRepository>()
        val all = listOf(alice to "Alice", bob to "Bob", carol to "Carol")
            .associate { (id, name) -> id to UserRow(id = id, email = "$name@test", displayName = name) }
        every { users.findById(any()) } answers { Optional.ofNullable(all[firstArg()]) }
        every { users.findAllById(any()) } answers { firstArg<Iterable<UUID>>().mapNotNull { all[it] } }
        friendships = mockk()
        every { friendships.findPair(any(), any()) } returns null
        every { friendships.findByRequesterIdOrAddresseeId(any(), any()) } returns emptyList()
        val presence = mockk<PresenceService>()
        every { presence.isVisiblyOnline(any(), any()) } returns false
        sockets = mockk(relaxed = true)
        service = DirectMessageService(store, users, friendships, BlockService(blockStore), presence, sockets)
        tick(0)
    }

    fun send(from: UUID, to: UUID, body: String = "hi"): SendResult = service.send(from, to, body).also { tick() }

    test("friends message each other freely and both see an active thread") {
        befriend(alice, bob)
        repeat(5) { send(alice, bob).shouldBeInstanceOf<SendResult.Sent>() }

        val bobsThreads = service.listThreads(bob)
        bobsThreads shouldHaveSize 1
        bobsThreads[0].state shouldBe ThreadState.ACTIVE
        bobsThreads[0].unread shouldBe 5
        service.listThreads(alice)[0].unread shouldBe 0
    }

    test("a stranger's message is a request for the recipient and capped for the sender") {
        repeat(DirectMessageService.REQUEST_MESSAGE_LIMIT) { send(alice, bob).shouldBeInstanceOf<SendResult.Sent>() }
        send(alice, bob) shouldBe SendResult.RequestLimitReached

        service.listThreads(bob)[0].state shouldBe ThreadState.INCOMING_REQUEST
        val mine = service.conversation(alice, bob)!!
        mine.state shouldBe ThreadState.OUTGOING_REQUEST
        mine.requestMessagesLeft shouldBe 0
    }

    test("a new conversation with a stranger starts as an outgoing request with the full allowance") {
        val fresh = service.conversation(alice, carol)!!
        fresh.state shouldBe ThreadState.OUTGOING_REQUEST
        fresh.requestMessagesLeft shouldBe DirectMessageService.REQUEST_MESSAGE_LIMIT
        fresh.messages.shouldBeEmpty()
    }

    test("accepting a request makes it active for both and lifts the cap") {
        repeat(3) { send(alice, bob) }
        service.accept(alice, bob) shouldBe false // the sender can't accept their own request
        service.accept(bob, alice) shouldBe true

        service.conversation(alice, bob)!!.state shouldBe ThreadState.ACTIVE
        send(alice, bob).shouldBeInstanceOf<SendResult.Sent>()
        verify { sockets.send(alice, ServerMessage.DirectMessagesChanged(bob.toString())) }
    }

    test("replying to a request accepts it") {
        send(alice, bob)
        send(bob, alice).shouldBeInstanceOf<SendResult.Sent>()
        service.listThreads(bob)[0].state shouldBe ThreadState.ACTIVE
        service.listThreads(alice)[0].state shouldBe ThreadState.ACTIVE
    }

    test("new messages are pushed to the recipient, flagged as a request when they are one") {
        send(alice, bob, "hello")
        verify {
            sockets.send(bob, match { it is ServerMessage.DirectMessage && it.request && it.message.body == "hello" && it.withAccountId == alice.toString() })
            sockets.send(alice, match { it is ServerMessage.DirectMessage && !it.request && it.withAccountId == bob.toString() })
        }
    }

    test("declining hides the thread for the recipient only, until a new message arrives") {
        repeat(3) { send(alice, bob) }
        service.clear(bob, alice) shouldBe true
        tick()
        service.listThreads(bob).shouldBeEmpty()
        service.listThreads(alice) shouldHaveSize 1

        // Declining resets the sender's allowance, and the next message is all the recipient sees.
        send(alice, bob, "one more").shouldBeInstanceOf<SendResult.Sent>()
        val bobsView = service.conversation(bob, alice)!!
        bobsView.messages.map { it.body } shouldBe listOf("one more")
        bobsView.state shouldBe ThreadState.INCOMING_REQUEST
        service.listThreads(bob)[0].unread shouldBe 1
    }

    test("a block either way hides the conversation and stops new messages") {
        befriend(alice, bob)
        send(alice, bob)
        service.block(bob, alice) shouldBe true

        service.listThreads(bob).shouldBeEmpty()
        service.listThreads(alice).shouldBeEmpty()
        service.conversation(alice, bob) shouldBe null
        service.send(alice, bob, "hey") shouldBe SendResult.CannotMessage
        service.send(bob, alice, "hey") shouldBe SendResult.CannotMessage
    }

    test("you can only block someone you share a conversation with") {
        service.block(alice, carol) shouldBe false
        blockStore.hasBlocked(alice, carol) shouldBe false
    }

    test("marking read clears the unread count") {
        befriend(alice, bob)
        send(alice, bob); send(alice, bob)
        service.markRead(bob, alice)
        tick()
        service.listThreads(bob)[0].unread shouldBe 0
        send(alice, bob)
        service.listThreads(bob)[0].unread shouldBe 1
    }

    test("bodies are trimmed and validated") {
        service.send(alice, bob, "   ") shouldBe SendResult.EmptyBody
        service.send(alice, bob, "x".repeat(DirectMessageService.MAX_BODY_LENGTH + 1)) shouldBe SendResult.TooLong
        service.send(alice, alice, "me") shouldBe SendResult.SelfMessage
        service.send(alice, UUID.randomUUID(), "who") shouldBe SendResult.CannotMessage
        val sent = service.send(alice, bob, "  padded  ")
        (sent as SendResult.Sent).message.body shouldBe "padded"
    }

    test("sending is rate limited per account") {
        befriend(alice, bob)
        repeat(DirectMessageService.RATE_LIMIT) { service.send(alice, bob, "spam").shouldBeInstanceOf<SendResult.Sent>() }
        service.send(alice, bob, "spam") shouldBe SendResult.RateLimited
        tick(DirectMessageService.RATE_WINDOW.seconds + 1)
        service.send(alice, bob, "later").shouldBeInstanceOf<SendResult.Sent>()
    }

    test("conversations page backwards from the newest message") {
        befriend(alice, bob)
        repeat(DirectMessageService.PAGE_SIZE + 5) { i ->
            service.send(alice, bob, "m$i")
            tick(Duration.ofSeconds(4).seconds) // stay under the rate limit
        }
        val first = service.conversation(bob, alice)!!
        first.messages shouldHaveSize DirectMessageService.PAGE_SIZE
        first.hasMore shouldBe true
        first.messages.last().body shouldBe "m${DirectMessageService.PAGE_SIZE + 4}"

        val older = service.conversation(bob, alice, before = first.messages.first().createdAt)!!
        older.messages.map { it.body } shouldBe (0 until 5).map { "m$it" }
        older.hasMore shouldBe false
    }

    test("threads list most recent first") {
        befriend(alice, bob)
        send(alice, bob)
        send(carol, alice)
        service.listThreads(alice).map { it.other.displayName } shouldBe listOf("Carol", "Bob")
        service.listThreads(alice)[0].lastMessage shouldNotBe null
    }
})

/** The store's contract over plain lists. */
private class InMemoryDirectMessageStore : DirectMessageStore {
    private val threads = mutableListOf<DmThreadRow>()
    private val messages = mutableListOf<DmMessageRow>()

    override fun thread(a: UUID, b: UUID): DmThreadRow? {
        val (low, high) = DmThreadRow.ordered(a, b)
        return threads.find { it.userLow == low && it.userHigh == high }
    }

    override fun threadsFor(userId: UUID, limit: Int): List<DmThreadRow> =
        threads.filter { it.userLow == userId || it.userHigh == userId }
            .sortedByDescending { it.lastMessageAt }.take(limit)

    override fun saveThread(row: DmThreadRow): DmThreadRow {
        val saved = if (row.id == null) row.copy(id = UUID.randomUUID()) else row
        threads.removeIf { it.id == saved.id }
        threads += saved
        return saved
    }

    override fun saveMessage(row: DmMessageRow): DmMessageRow = row.copy(id = UUID.randomUUID()).also { messages += it }

    override fun page(threadId: UUID, after: Instant, before: Instant, limit: Int): List<DmMessageRow> =
        messages.filter { it.threadId == threadId && it.createdAt > after && it.createdAt < before }
            .sortedByDescending { it.createdAt }.take(limit)

    override fun countFrom(threadId: UUID, senderId: UUID, after: Instant): Int =
        messages.count { it.threadId == threadId && it.senderId == senderId && it.createdAt > after }
}

private class InMemoryBlockStore : AccountBlockStore {
    private val blocks = mutableSetOf<Pair<UUID, UUID>>()
    override fun blockedEitherWay(userId: UUID): Set<UUID> =
        blocks.filter { it.first == userId }.map { it.second }.toSet() +
            blocks.filter { it.second == userId }.map { it.first }
    override fun hasBlocked(blocker: UUID, blocked: UUID) = (blocker to blocked) in blocks
    override fun block(blocker: UUID, blocked: UUID) { blocks += blocker to blocked }
    override fun unblock(blocker: UUID, blocked: UUID) = blocks.remove(blocker to blocked)
    override fun list(blocker: UUID): List<BlockedAccount> = emptyList()
}
