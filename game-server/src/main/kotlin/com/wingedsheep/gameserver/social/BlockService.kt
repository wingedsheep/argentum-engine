package com.wingedsheep.gameserver.social

import com.wingedsheep.sdk.model.EntityId
import org.springframework.stereotype.Component
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Who someone is for blocking purposes: a signed-in account by [userId], a guest by the [playerId]
 * of their session identity (stable across reconnects, gone when the identity is).
 */
data class Party(val playerId: EntityId, val userId: UUID?)

/**
 * Blocks between players. A block is one-directional but its effects are mutual: the two are never
 * matched, can't rematch or send each other friend requests, and the blocker stops receiving the
 * other's emotes.
 *
 * Between two accounts the block is durable ([AccountBlockStore]); when either side is a guest it is
 * remembered for the server's lifetime under the guest's session identity. Matchmaking asks
 * [blockedEitherWay] for every candidate pair once a second, so account blocks are cached per account
 * and invalidated on change.
 */
@Component
class BlockService(private val store: AccountBlockStore) {
    /** blocker playerId -> blocked playerIds, for blocks involving a guest. */
    private val sessionBlocks = ConcurrentHashMap<EntityId, MutableSet<EntityId>>()
    private val accountCache = ConcurrentHashMap<UUID, Set<UUID>>()

    fun hasBlocked(blocker: Party, other: Party): Boolean {
        if (other.playerId in sessionBlocks[blocker.playerId].orEmpty()) return true
        val a = blocker.userId ?: return false
        val b = other.userId ?: return false
        return store.hasBlocked(a, b)
    }

    fun blockedEitherWay(a: Party, b: Party): Boolean {
        if (b.playerId in sessionBlocks[a.playerId].orEmpty()) return true
        if (a.playerId in sessionBlocks[b.playerId].orEmpty()) return true
        val ua = a.userId ?: return false
        val ub = b.userId ?: return false
        return ub in accountBlocks(ua)
    }

    fun block(blocker: Party, other: Party) {
        val a = blocker.userId
        val b = other.userId
        if (a != null && b != null) {
            store.block(a, b)
            invalidate(a, b)
        } else {
            sessionBlocks.computeIfAbsent(blocker.playerId) { ConcurrentHashMap.newKeySet() } += other.playerId
        }
    }

    fun unblock(blocker: Party, other: Party) {
        sessionBlocks[blocker.playerId]?.remove(other.playerId)
        val a = blocker.userId ?: return
        val b = other.userId ?: return
        unblockAccount(a, b)
    }

    /** The "Blocked players" list's unblock, which only knows account ids. */
    fun unblockAccount(blocker: UUID, blocked: UUID): Boolean =
        store.unblock(blocker, blocked).also { invalidate(blocker, blocked) }

    /** Block by account id — for places that know both accounts but no session (a message thread). */
    fun blockAccount(blocker: UUID, blocked: UUID) {
        store.block(blocker, blocked)
        invalidate(blocker, blocked)
    }

    fun listAccountBlocks(blocker: UUID): List<BlockedAccount> = store.list(blocker)

    /** Accounts [userId] has blocked or been blocked by. */
    fun accountBlocks(userId: UUID): Set<UUID> =
        accountCache.computeIfAbsent(userId) { store.blockedEitherWay(it) }

    private fun invalidate(a: UUID, b: UUID) {
        accountCache.remove(a)
        accountCache.remove(b)
    }
}
