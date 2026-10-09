package com.wingedsheep.gameserver.social

import com.wingedsheep.gameserver.persistence.FriendshipRepository
import com.wingedsheep.gameserver.persistence.UserBlockRepository
import com.wingedsheep.gameserver.persistence.UserBlockRow
import com.wingedsheep.gameserver.persistence.UserRepository
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

/** An account [accountId] has blocked, for the "Blocked players" list. */
data class BlockedAccount(val accountId: UUID, val displayName: String, val blockedAt: Instant, val avatar: String? = null)

/**
 * Durable account-to-account blocks. Wired like [com.wingedsheep.gameserver.matchmaking.RatingLookup]:
 * with accounts disabled there are no accounts to block and every guest block lives in
 * [BlockService]'s memory instead.
 */
interface AccountBlockStore {
    /** Accounts [userId] has blocked, plus accounts that have blocked [userId]. */
    fun blockedEitherWay(userId: UUID): Set<UUID>
    fun hasBlocked(blocker: UUID, blocked: UUID): Boolean
    fun block(blocker: UUID, blocked: UUID)
    fun unblock(blocker: UUID, blocked: UUID): Boolean
    fun list(blocker: UUID): List<BlockedAccount>
}

@Component
@ConditionalOnProperty(name = ["accounts.enabled"], havingValue = "false", matchIfMissing = true)
class NoAccountBlockStore : AccountBlockStore {
    override fun blockedEitherWay(userId: UUID): Set<UUID> = emptySet()
    override fun hasBlocked(blocker: UUID, blocked: UUID): Boolean = false
    override fun block(blocker: UUID, blocked: UUID) {}
    override fun unblock(blocker: UUID, blocked: UUID): Boolean = false
    override fun list(blocker: UUID): List<BlockedAccount> = emptyList()
}

@Component
@ConditionalOnProperty(name = ["accounts.enabled"], havingValue = "true")
class JdbcAccountBlockStore(
    private val blocks: UserBlockRepository,
    private val friendships: FriendshipRepository,
    private val users: UserRepository,
) : AccountBlockStore {

    override fun blockedEitherWay(userId: UUID): Set<UUID> =
        blocks.findByBlockerId(userId).mapTo(HashSet()) { it.blockedId } +
            blocks.findByBlockedId(userId).map { it.blockerId }

    override fun hasBlocked(blocker: UUID, blocked: UUID): Boolean =
        blocks.findByBlockerIdAndBlockedId(blocker, blocked) != null

    /** Also ends any friendship or pending request between the two — you can't be both. */
    override fun block(blocker: UUID, blocked: UUID) {
        if (blocker == blocked) return
        if (blocks.findByBlockerIdAndBlockedId(blocker, blocked) == null) {
            blocks.save(UserBlockRow(blockerId = blocker, blockedId = blocked))
        }
        friendships.findPair(blocker, blocked)?.id?.let(friendships::deleteById)
    }

    override fun unblock(blocker: UUID, blocked: UUID): Boolean {
        val row = blocks.findByBlockerIdAndBlockedId(blocker, blocked) ?: return false
        blocks.deleteById(row.id!!)
        return true
    }

    override fun list(blocker: UUID): List<BlockedAccount> {
        val rows = blocks.findByBlockerId(blocker)
        val byId = users.findAllById(rows.map { it.blockedId }).associateBy { it.id }
        return rows.mapNotNull { row ->
            byId[row.blockedId]?.let { BlockedAccount(row.blockedId, it.displayName, row.createdAt, it.avatar) }
        }.sortedBy { it.displayName.lowercase() }
    }
}
