package com.wingedsheep.gameserver.messages

import com.wingedsheep.gameserver.friends.PresenceService
import com.wingedsheep.gameserver.persistence.DmMessageRow
import com.wingedsheep.gameserver.persistence.DmThreadRow
import com.wingedsheep.gameserver.persistence.FriendshipRepository
import com.wingedsheep.gameserver.persistence.FriendshipStatus
import com.wingedsheep.gameserver.persistence.UserRepository
import com.wingedsheep.gameserver.persistence.UserRow
import com.wingedsheep.gameserver.protocol.ServerMessage
import com.wingedsheep.gameserver.session.UserSockets
import com.wingedsheep.gameserver.social.BlockService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Direct messages between accounts.
 *
 * Every conversation is addressed by the other person's account id, so opening one from a profile
 * needs no thread to exist yet. The rules:
 *
 * - **Friends** message each other freely.
 * - **Anyone else** can start a conversation, but it lands as a *message request*: the recipient
 *   sees it apart from their chats and chooses to accept, delete or block. Until they accept, the
 *   sender can send only [REQUEST_MESSAGE_LIMIT] messages. Replying counts as accepting.
 * - **A block either way** makes the conversation vanish and reads as "can't message this player";
 *   it never tells the blocked side.
 * - **Deleting** (or declining a request) hides the conversation for you only — up to now. A new
 *   message brings it back, starting from that message.
 *
 * New messages are pushed live to every open socket of both participants; everything else changes
 * with a [ServerMessage.DirectMessagesChanged] hint and is refetched over REST.
 *
 * Only mounted when accounts are enabled.
 */
@Service
@ConditionalOnProperty(name = ["accounts.enabled"], havingValue = "true")
class DirectMessageService(
    private val store: DirectMessageStore,
    private val users: UserRepository,
    private val friendships: FriendshipRepository,
    private val blocks: BlockService,
    private val presence: PresenceService,
    private val sockets: UserSockets,
) {
    /** Replaceable in tests. */
    internal var clock: Clock = Clock.systemUTC()

    /** How a conversation stands from the viewer's side. */
    enum class ThreadState {
        /** Accepted (or between friends): both sides message freely. */
        ACTIVE,
        /** Someone who isn't your friend wrote to you; accept it to reply. */
        INCOMING_REQUEST,
        /** You wrote to someone who isn't your friend and they haven't accepted yet. */
        OUTGOING_REQUEST,
    }

    data class Participant(val accountId: UUID, val displayName: String, val online: Boolean, val avatar: String? = null)
    data class MessageView(val id: UUID, val senderId: UUID, val body: String, val createdAt: Instant)

    data class ThreadSummary(
        val other: Participant,
        val state: ThreadState,
        val isFriend: Boolean,
        val lastMessage: MessageView,
        val unread: Int,
    )

    data class Conversation(
        val other: Participant,
        val state: ThreadState,
        val isFriend: Boolean,
        /** Oldest first. */
        val messages: List<MessageView>,
        /** Older messages exist before the first one returned. */
        val hasMore: Boolean,
        /** For an [ThreadState.OUTGOING_REQUEST]: how many more you can send before they accept. */
        val requestMessagesLeft: Int?,
    )

    sealed interface SendResult {
        data class Sent(val message: MessageView) : SendResult
        data object SelfMessage : SendResult
        data object CannotMessage : SendResult
        data object EmptyBody : SendResult
        data object TooLong : SendResult
        data object RateLimited : SendResult
        data object RequestLimitReached : SendResult
    }

    /** Your conversations with at least one message you can see, most recent first. */
    fun listThreads(me: UUID): List<ThreadSummary> {
        val blocked = blocks.accountBlocks(me)
        val rows = store.threadsFor(me, THREAD_LIST_LIMIT).filter { it.other(me) !in blocked }
        if (rows.isEmpty()) return emptyList()
        val friendIds = friendIds(me)
        val byId = users.findAllById(rows.map { it.other(me) }).associateBy { it.id }
        return rows.mapNotNull { thread ->
            val otherId = thread.other(me)
            val user = byId[otherId] ?: return@mapNotNull null
            val last = store.page(thread.id!!, visibleFrom(thread, me), FAR_FUTURE, 1).firstOrNull()
                ?: return@mapNotNull null
            val friend = otherId in friendIds
            ThreadSummary(
                other = participant(user),
                state = stateOf(thread, me, friend),
                isFriend = friend,
                lastMessage = last.view(),
                unread = unread(thread, me),
            )
        }
    }

    /**
     * Your conversation with [otherId], newest [PAGE_SIZE] messages (or the page before [before]).
     * Null when there's no such account or a block stands between you — the two read the same.
     */
    fun conversation(me: UUID, otherId: UUID, before: Instant? = null): Conversation? {
        val other = reachable(me, otherId) ?: return null
        val friend = areFriends(me, otherId)
        val thread = store.thread(me, otherId)
            ?: return Conversation(
                other = participant(other),
                state = if (friend) ThreadState.ACTIVE else ThreadState.OUTGOING_REQUEST,
                isFriend = friend,
                messages = emptyList(),
                hasMore = false,
                requestMessagesLeft = if (friend) null else REQUEST_MESSAGE_LIMIT,
            )
        val page = store.page(thread.id!!, visibleFrom(thread, me), before ?: FAR_FUTURE, PAGE_SIZE + 1)
        val state = stateOf(thread, me, friend)
        return Conversation(
            other = participant(other),
            state = state,
            isFriend = friend,
            messages = page.take(PAGE_SIZE).reversed().map { it.view() },
            hasMore = page.size > PAGE_SIZE,
            requestMessagesLeft = if (state == ThreadState.OUTGOING_REQUEST) requestMessagesLeft(thread, me) else null,
        )
    }

    fun send(me: UUID, otherId: UUID, rawBody: String): SendResult {
        val body = rawBody.trim()
        if (body.isEmpty()) return SendResult.EmptyBody
        if (body.length > MAX_BODY_LENGTH) return SendResult.TooLong
        if (otherId == me) return SendResult.SelfMessage
        val other = reachable(me, otherId) ?: return SendResult.CannotMessage
        val sender = users.findById(me).orElse(null) ?: return SendResult.CannotMessage
        val friend = areFriends(me, otherId)
        val now = now()

        val existing = store.thread(me, otherId)
        val accepted = when {
            existing == null -> friend
            existing.accepted || friend -> true
            // Replying to a request accepts it.
            existing.initiatorId != me -> true
            requestMessagesLeft(existing, me) <= 0 -> return SendResult.RequestLimitReached
            else -> false
        }
        if (!takeRateBudget(me, now)) return SendResult.RateLimited

        val thread = saveThread(
            (existing ?: newThread(me, otherId, now)).copy(accepted = accepted, lastMessageAt = now).withReadAt(me, now),
        )
        val message = store.saveMessage(
            DmMessageRow(threadId = thread.id!!, senderId = me, body = body, createdAt = now),
        ).view()

        val info = message.info()
        sockets.send(
            otherId,
            ServerMessage.DirectMessage(me.toString(), sender.displayName, info, request = !accepted),
        )
        sockets.send(
            me,
            ServerMessage.DirectMessage(otherId.toString(), other.displayName, info, request = false),
        )
        return SendResult.Sent(message)
    }

    /** Accept a message request sent to you. */
    fun accept(me: UUID, otherId: UUID): Boolean {
        val thread = store.thread(me, otherId) ?: return false
        if (thread.initiatorId == me && !thread.accepted) return false
        if (!thread.accepted) store.saveThread(thread.copy(accepted = true))
        changed(me, otherId)
        changed(otherId, me)
        return true
    }

    /** You've seen everything in the conversation so far. */
    fun markRead(me: UUID, otherId: UUID) {
        val thread = store.thread(me, otherId) ?: return
        if (unread(thread, me) == 0) return
        store.saveThread(thread.withReadAt(me, now()))
        changed(me, otherId)
    }

    /** Delete the conversation for yourself, or decline a request — the same thing. */
    fun clear(me: UUID, otherId: UUID): Boolean {
        val thread = store.thread(me, otherId) ?: return false
        val now = now()
        store.saveThread(thread.withClearedAt(me, now).withReadAt(me, now))
        changed(me, otherId)
        return true
    }

    /**
     * Block the other side of a conversation you have. Only someone you share a thread with can be
     * blocked from here — everywhere else blocks from a place where the server already knows who.
     */
    fun block(me: UUID, otherId: UUID): Boolean {
        if (otherId == me) return false
        val thread = store.thread(me, otherId) ?: return false
        blocks.blockAccount(me, otherId)
        val now = now()
        store.saveThread(thread.withClearedAt(me, now).withReadAt(me, now))
        changed(me, otherId)
        return true
    }

    /** The other account, unless it doesn't exist, is you, or a block stands between you. */
    private fun reachable(me: UUID, otherId: UUID): UserRow? {
        if (otherId == me) return null
        if (otherId in blocks.accountBlocks(me)) return null
        return users.findById(otherId).orElse(null)
    }

    private fun stateOf(thread: DmThreadRow, me: UUID, friend: Boolean): ThreadState = when {
        thread.accepted || friend -> ThreadState.ACTIVE
        thread.initiatorId == me -> ThreadState.OUTGOING_REQUEST
        else -> ThreadState.INCOMING_REQUEST
    }

    /** Messages the other side sent since you last read or cleared. */
    private fun unread(thread: DmThreadRow, me: UUID): Int {
        val since = listOfNotNull(thread.readAt(me), thread.clearedAt(me)).maxOrNull() ?: Instant.EPOCH
        return store.countFrom(thread.id!!, thread.other(me), since)
    }

    /** Counted from the recipient's clear marker: declining a request resets the allowance. */
    private fun requestMessagesLeft(thread: DmThreadRow, me: UUID): Int {
        val since = thread.clearedAt(thread.other(me)) ?: Instant.EPOCH
        return (REQUEST_MESSAGE_LIMIT - store.countFrom(thread.id!!, me, since)).coerceAtLeast(0)
    }

    private fun visibleFrom(thread: DmThreadRow, me: UUID): Instant = thread.clearedAt(me) ?: Instant.EPOCH

    private fun newThread(me: UUID, otherId: UUID, now: Instant): DmThreadRow {
        val (low, high) = DmThreadRow.ordered(me, otherId)
        return DmThreadRow(userLow = low, userHigh = high, initiatorId = me, createdAt = now, lastMessageAt = now)
    }

    /** Two first messages crossing in flight both try to create the thread; the loser updates the winner's. */
    private fun saveThread(row: DmThreadRow): DmThreadRow =
        try {
            store.saveThread(row)
        } catch (e: DataIntegrityViolationException) {
            if (row.id != null) throw e
            val winner = store.thread(row.userLow, row.userHigh) ?: throw e
            store.saveThread(
                winner.copy(accepted = winner.accepted || row.accepted, lastMessageAt = row.lastMessageAt)
                    .withReadAt(row.initiatorId, row.lastMessageAt),
            )
        }

    private val recentSends = ConcurrentHashMap<UUID, ArrayDeque<Instant>>()

    private fun takeRateBudget(me: UUID, now: Instant): Boolean {
        val sends = recentSends.computeIfAbsent(me) { ArrayDeque() }
        synchronized(sends) {
            val windowStart = now.minus(RATE_WINDOW)
            while (sends.isNotEmpty() && sends.first() <= windowStart) sends.removeFirst()
            if (sends.size >= RATE_LIMIT) return false
            sends.addLast(now)
            return true
        }
    }

    private fun friendIds(me: UUID): Set<UUID> =
        friendships.findByRequesterIdOrAddresseeId(me, me)
            .filter { it.status == FriendshipStatus.ACCEPTED.name }
            .mapTo(HashSet()) { if (it.requesterId == me) it.addresseeId else it.requesterId }

    private fun areFriends(a: UUID, b: UUID): Boolean =
        friendships.findPair(a, b)?.status == FriendshipStatus.ACCEPTED.name

    private fun participant(user: UserRow) =
        Participant(user.id!!, user.displayName, presence.isVisiblyOnline(user.id, user.hidePresence), user.avatar)

    private fun changed(to: UUID, withAccount: UUID) =
        sockets.send(to, ServerMessage.DirectMessagesChanged(withAccount.toString()))

    /** Postgres keeps microseconds; truncating keeps "after my clear marker" comparisons exact. */
    private fun now(): Instant = Instant.now(clock).truncatedTo(ChronoUnit.MICROS)

    private fun DmMessageRow.view() = MessageView(id!!, senderId, body, createdAt)
    private fun MessageView.info() =
        ServerMessage.DirectMessageInfo(id.toString(), senderId.toString(), body, createdAt.toString())

    companion object {
        const val MAX_BODY_LENGTH = 1000
        const val REQUEST_MESSAGE_LIMIT = 3
        const val PAGE_SIZE = 50
        const val THREAD_LIST_LIMIT = 100
        const val RATE_LIMIT = 20
        val RATE_WINDOW: Duration = Duration.ofMinutes(1)

        /** An upper bound Postgres accepts — `Instant.MAX` overflows `timestamptz`. */
        private val FAR_FUTURE: Instant = Instant.parse("9999-01-01T00:00:00Z")
    }
}
