package com.wingedsheep.gameserver.friends

import com.wingedsheep.gameserver.handler.MessageSender
import com.wingedsheep.gameserver.persistence.FriendshipRepository
import com.wingedsheep.gameserver.persistence.FriendshipStatus
import com.wingedsheep.gameserver.persistence.UserRepository
import com.wingedsheep.gameserver.protocol.ServerMessage
import com.wingedsheep.gameserver.session.SessionRegistry
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * Pushes live friend updates over the existing WebSocket connections — presence changes and incoming
 * friend requests — so the friends UI stays current without polling for the common cases. (The client
 * also polls periodically as a catch-all for accept/unfriend on the passive side.) Messages are only
 * delivered to currently-connected sockets; offline friends pick up the state from `GET /api/friends`
 * on their next load.
 *
 * Only mounted when accounts are enabled.
 */
@Component
@ConditionalOnProperty(name = ["accounts.enabled"], havingValue = "true")
class FriendPresenceBroadcaster(
    private val friendships: FriendshipRepository,
    private val users: UserRepository,
    private val presence: PresenceService,
    private val sessionRegistry: SessionRegistry,
    private val sender: MessageSender,
) {
    /** A user connected, disconnected, or toggled visibility — tell their friends their new state. */
    fun broadcastOwnPresence(userId: UUID) {
        val msg = ServerMessage.FriendPresence(userId.toString(), visiblyOnline(userId))
        acceptedFriendIds(userId).forEach { sendToUser(it, msg) }
    }

    /** On accept: each side immediately sees the other's current presence (rather than waiting a poll). */
    fun exchangePresence(a: UUID, b: UUID) {
        sendToUser(b, ServerMessage.FriendPresence(a.toString(), visiblyOnline(a)))
        sendToUser(a, ServerMessage.FriendPresence(b.toString(), visiblyOnline(b)))
    }

    /** Tell the addressee that [fromUserId] just sent them a friend request. */
    fun notifyRequestReceived(toUserId: UUID, fromUserId: UUID, fromName: String) {
        sendToUser(toUserId, ServerMessage.FriendRequestReceived(fromUserId.toString(), fromName))
    }

    private fun visiblyOnline(userId: UUID): Boolean {
        val user = users.findById(userId).orElse(null) ?: return false
        return presence.isVisiblyOnline(userId, user.hidePresence)
    }

    private fun acceptedFriendIds(userId: UUID): List<UUID> =
        friendships.findByRequesterIdOrAddresseeId(userId, userId)
            .filter { it.status == FriendshipStatus.ACCEPTED.name }
            .map { if (it.requesterId == userId) it.addresseeId else it.requesterId }

    private fun sendToUser(userId: UUID, message: ServerMessage) {
        sessionRegistry.getAllIdentities().forEach { identity ->
            if (!identity.isAi && identity.userId == userId) {
                val ws = identity.webSocketSession
                if (ws != null && ws.isOpen) sender.send(ws, message)
            }
        }
    }
}
