package com.wingedsheep.gameserver.messages

import com.wingedsheep.gameserver.persistence.DmMessageRepository
import com.wingedsheep.gameserver.persistence.DmMessageRow
import com.wingedsheep.gameserver.persistence.DmThreadRepository
import com.wingedsheep.gameserver.persistence.DmThreadRow
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

/**
 * Persistence seam for [DirectMessageService]: the handful of reads and writes the messaging rules
 * need, so the rules can be tested against an in-memory store. Pages come back newest first.
 */
interface DirectMessageStore {
    fun thread(a: UUID, b: UUID): DmThreadRow?
    fun threadsFor(userId: UUID, limit: Int): List<DmThreadRow>
    fun saveThread(row: DmThreadRow): DmThreadRow
    fun saveMessage(row: DmMessageRow): DmMessageRow
    /** Messages strictly between [after] and [before], newest first. */
    fun page(threadId: UUID, after: Instant, before: Instant, limit: Int): List<DmMessageRow>
    fun countFrom(threadId: UUID, senderId: UUID, after: Instant): Int
}

@Component
@ConditionalOnProperty(name = ["accounts.enabled"], havingValue = "true")
class JdbcDirectMessageStore(
    private val threads: DmThreadRepository,
    private val messages: DmMessageRepository,
) : DirectMessageStore {
    override fun thread(a: UUID, b: UUID): DmThreadRow? {
        val (low, high) = DmThreadRow.ordered(a, b)
        return threads.findByUserLowAndUserHigh(low, high)
    }

    override fun threadsFor(userId: UUID, limit: Int): List<DmThreadRow> = threads.findRecentFor(userId, limit)
    override fun saveThread(row: DmThreadRow): DmThreadRow = threads.save(row)
    override fun saveMessage(row: DmMessageRow): DmMessageRow = messages.save(row)

    override fun page(threadId: UUID, after: Instant, before: Instant, limit: Int): List<DmMessageRow> =
        messages.findPage(threadId, after, before, limit)

    override fun countFrom(threadId: UUID, senderId: UUID, after: Instant): Int =
        messages.countFromSince(threadId, senderId, after)
}
