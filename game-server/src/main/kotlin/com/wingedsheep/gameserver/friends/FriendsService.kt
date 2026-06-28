package com.wingedsheep.gameserver.friends

import com.wingedsheep.gameserver.persistence.FriendshipRepository
import com.wingedsheep.gameserver.persistence.FriendshipRow
import com.wingedsheep.gameserver.persistence.FriendshipStatus
import com.wingedsheep.gameserver.persistence.UserRepository
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

/**
 * Friend relationships and the request → accept lifecycle.
 *
 * A friendship is a single directed [FriendshipRow]: PENDING while the addressee hasn't responded,
 * ACCEPTED once they have (symmetric from then on). You invite someone by their account id (their
 * shareable "friend code"), never their email. Presence is layered on at read time from
 * [PresenceService]; visible-presence changes are pushed live via [FriendPresenceBroadcaster].
 *
 * Only mounted when accounts are enabled.
 */
@Service
@ConditionalOnProperty(name = ["accounts.enabled"], havingValue = "true")
class FriendsService(
    private val friendships: FriendshipRepository,
    private val users: UserRepository,
    private val presence: PresenceService,
    private val broadcaster: FriendPresenceBroadcaster,
) {
    /** An accepted friend plus their current visible-online state. */
    data class FriendView(val accountId: UUID, val displayName: String, val online: Boolean)

    /** A pending request (incoming or outgoing), identified by the other party. */
    data class RequestView(
        val requestId: UUID,
        val accountId: UUID,
        val displayName: String,
        val createdAt: Instant,
    )

    data class Requests(val incoming: List<RequestView>, val outgoing: List<RequestView>)

    /** Outcome of [sendRequest], so the controller can map each case to the right HTTP status. */
    sealed interface SendResult {
        data class Created(val request: RequestView) : SendResult
        data object SelfRequest : SendResult
        data object UnknownUser : SendResult
        data object AlreadyFriends : SendResult
        data object AlreadyRequested : SendResult
        /** The target had already sent *you* a request — accept that one instead. */
        data class IncomingPending(val requestId: UUID) : SendResult
    }

    fun listFriends(userId: UUID): List<FriendView> {
        val rows = acceptedRows(userId)
        val byId = users.findAllById(rows.map { other(it, userId) }).associateBy { it.id }
        return rows.mapNotNull { row ->
            val otherId = other(row, userId)
            val user = byId[otherId] ?: return@mapNotNull null
            FriendView(otherId, user.displayName, presence.isVisiblyOnline(otherId, user.hidePresence))
        }.sortedWith(compareByDescending<FriendView> { it.online }.thenBy { it.displayName.lowercase() })
    }

    fun listRequests(userId: UUID): Requests {
        val rows = friendships.findByRequesterIdOrAddresseeId(userId, userId)
            .filter { it.status == FriendshipStatus.PENDING.name }
        val byId = users.findAllById(rows.flatMap { listOf(it.requesterId, it.addresseeId) }.toSet())
            .associateBy { it.id }
        val incoming = rows.filter { it.addresseeId == userId }.mapNotNull { r ->
            byId[r.requesterId]?.let { RequestView(r.id!!, r.requesterId, it.displayName, r.createdAt) }
        }
        val outgoing = rows.filter { it.requesterId == userId }.mapNotNull { r ->
            byId[r.addresseeId]?.let { RequestView(r.id!!, r.addresseeId, it.displayName, r.createdAt) }
        }
        return Requests(incoming, outgoing)
    }

    fun sendRequest(requesterId: UUID, targetId: UUID): SendResult {
        if (targetId == requesterId) return SendResult.SelfRequest
        val target = users.findById(targetId).orElse(null) ?: return SendResult.UnknownUser

        friendships.findPair(requesterId, targetId)?.let { existing ->
            return when {
                existing.status == FriendshipStatus.ACCEPTED.name -> SendResult.AlreadyFriends
                existing.requesterId == requesterId -> SendResult.AlreadyRequested
                else -> SendResult.IncomingPending(existing.id!!)
            }
        }

        val saved = friendships.save(FriendshipRow(requesterId = requesterId, addresseeId = targetId))
        val requesterName = users.findById(requesterId).orElse(null)?.displayName ?: "Someone"
        broadcaster.notifyRequestReceived(targetId, requesterId, requesterName)
        return SendResult.Created(RequestView(saved.id!!, targetId, target.displayName, saved.createdAt))
    }

    /** Accept an incoming pending request. Only the addressee may accept. */
    fun accept(userId: UUID, requestId: UUID): Boolean {
        val row = friendships.findById(requestId).orElse(null) ?: return false
        if (row.addresseeId != userId || row.status != FriendshipStatus.PENDING.name) return false
        friendships.save(row.copy(status = FriendshipStatus.ACCEPTED.name, respondedAt = Instant.now()))
        broadcaster.exchangePresence(row.requesterId, row.addresseeId)
        return true
    }

    /** Decline an incoming request (addressee) or cancel an outgoing one (requester) — both delete it. */
    fun removeRequest(userId: UUID, requestId: UUID): Boolean {
        val row = friendships.findById(requestId).orElse(null) ?: return false
        if (row.status != FriendshipStatus.PENDING.name) return false
        if (row.requesterId != userId && row.addresseeId != userId) return false
        friendships.deleteById(requestId)
        return true
    }

    /** Remove an accepted friendship in either direction. */
    fun unfriend(userId: UUID, otherId: UUID): Boolean {
        val row = friendships.findPair(userId, otherId) ?: return false
        if (row.status != FriendshipStatus.ACCEPTED.name) return false
        friendships.deleteById(row.id!!)
        return true
    }

    /** Toggle whether the account appears offline to its friends; pushes the new state live. */
    fun setHidePresence(userId: UUID, hidden: Boolean) {
        val user = users.findById(userId).orElse(null) ?: return
        if (user.hidePresence != hidden) users.save(user.copy(hidePresence = hidden))
        broadcaster.broadcastOwnPresence(userId)
    }

    private fun acceptedRows(userId: UUID): List<FriendshipRow> =
        friendships.findByRequesterIdOrAddresseeId(userId, userId)
            .filter { it.status == FriendshipStatus.ACCEPTED.name }

    private fun other(row: FriendshipRow, userId: UUID): UUID =
        if (row.requesterId == userId) row.addresseeId else row.requesterId
}
