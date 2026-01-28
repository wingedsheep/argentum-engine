package com.wingedsheep.gameserver.persistence

import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.gameserver.dto.ClientEvent
import com.wingedsheep.gameserver.persistence.dto.PersistentGameSession
import com.wingedsheep.gameserver.persistence.dto.PersistentPlayerInfo
import com.wingedsheep.gameserver.session.GameSession
import com.wingedsheep.gameserver.session.PlayerIdentity
import com.wingedsheep.gameserver.session.SessionRegistry
import com.wingedsheep.sdk.model.EntityId

/**
 * Converts a GameSession to its persistent representation.
 *
 * Note: This requires access to internal session state. The GameSession class
 * must expose getters for persistence (see getStateForPersistence, etc.).
 */
fun GameSession.toPersistent(
    playerIdentities: List<PlayerIdentity>,
    lobbyId: String?
): PersistentGameSession {
    return PersistentGameSession(
        sessionId = sessionId,
        gameState = getStateForPersistence(),
        deckLists = getDeckListsForPersistence().mapKeys { it.key.value },
        lastProcessedMessageId = getLastMessageIdsForPersistence().mapKeys { it.key.value },
        gameLogs = getLogsForPersistence().mapKeys { it.key.value },
        playerInfos = playerIdentities.map { identity ->
            PersistentPlayerInfo(
                playerId = identity.playerId.value,
                playerName = identity.playerName,
                token = identity.token
            )
        },
        lobbyId = lobbyId
    )
}

/**
 * Restores a GameSession from its persistent representation.
 *
 * This creates a new GameSession with the persisted state restored.
 * Player sessions are NOT restored here - they are recreated when players reconnect.
 *
 * @param persistent The persisted session data
 * @param cardRegistry The card registry for rebuilding the session
 * @return A new GameSession with state restored, and a list of player identities to register
 */
fun restoreGameSession(
    persistent: PersistentGameSession,
    cardRegistry: CardRegistry
): Pair<GameSession, List<PlayerIdentity>> {
    val session = GameSession(
        sessionId = persistent.sessionId,
        cardRegistry = cardRegistry
    )

    // Convert persisted data back to EntityId-keyed maps
    val deckLists = persistent.deckLists.mapKeys { EntityId(it.key) }
    val lastMessageIds = persistent.lastProcessedMessageId.mapKeys { EntityId(it.key) }
    val logs = persistent.gameLogs.mapKeys { EntityId(it.key) }
        .mapValues { it.value.toMutableList() }

    // Restore internal state
    session.restoreFromPersistence(
        state = persistent.gameState,
        decks = deckLists,
        logs = logs,
        lastIds = lastMessageIds
    )

    // Create PlayerIdentity objects for each persisted player
    val playerIdentities = persistent.playerInfos.map { info ->
        PlayerIdentity(
            token = info.token,
            playerId = EntityId(info.playerId),
            playerName = info.playerName
        ).also {
            it.currentGameSessionId = persistent.sessionId
        }
    }

    return session to playerIdentities
}
