package com.wingedsheep.gameserver.friends

import com.wingedsheep.gameserver.session.SessionRegistry
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * Derives whether an account is online from the live WebSocket sessions in [SessionRegistry] — there
 * is no stored "online" flag. An account is *connected* if any of its non-AI identities currently
 * holds an open socket; it is *visibly online* (what friends see) only if it is connected AND has not
 * opted out of presence via `hide_presence`.
 *
 * Only mounted when accounts are enabled (there are no accounts to have presence otherwise).
 */
@Service
@ConditionalOnProperty(name = ["accounts.enabled"], havingValue = "true")
class PresenceService(private val sessionRegistry: SessionRegistry) {

    /** Whether the account currently holds an open WebSocket session (ignores the visibility opt-out). */
    fun isConnected(userId: UUID): Boolean =
        sessionRegistry.getAllIdentities().any { !it.isAi && it.userId == userId && it.isConnected }

    /** Online as seen by friends: connected and not hiding presence. */
    fun isVisiblyOnline(userId: UUID, hidePresence: Boolean): Boolean =
        !hidePresence && isConnected(userId)
}
