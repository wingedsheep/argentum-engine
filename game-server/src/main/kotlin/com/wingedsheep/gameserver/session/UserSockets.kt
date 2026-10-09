package com.wingedsheep.gameserver.session

import com.wingedsheep.gameserver.handler.MessageSender
import com.wingedsheep.gameserver.protocol.ServerMessage
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * Delivers a message to every open socket a signed-in account holds — one per tab or device. The
 * account-scoped pushes (friend presence, friend requests, direct messages) all go through here;
 * an account with no open socket simply misses the push and catches up over REST.
 */
@Component
class UserSockets(
    private val sessionRegistry: SessionRegistry,
    private val sender: MessageSender,
) {
    fun send(userId: UUID, message: ServerMessage) {
        sessionRegistry.getAllIdentities().forEach { identity ->
            if (!identity.isAi && identity.userId == userId) {
                val ws = identity.webSocketSession
                if (ws != null && ws.isOpen) sender.send(ws, message)
            }
        }
    }
}
