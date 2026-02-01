package com.wingedsheep.gameserver.persistence

import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.gameserver.lobby.LobbyPlayerState
import com.wingedsheep.gameserver.lobby.LobbyState
import com.wingedsheep.gameserver.lobby.TournamentFormat
import com.wingedsheep.gameserver.lobby.TournamentLobby
import com.wingedsheep.gameserver.persistence.dto.*
import com.wingedsheep.gameserver.sealed.SealedPlayerState
import com.wingedsheep.gameserver.sealed.SealedSession
import com.wingedsheep.gameserver.sealed.SealedSessionState
import com.wingedsheep.gameserver.session.PlayerIdentity
import com.wingedsheep.gameserver.session.PlayerSession
import com.wingedsheep.gameserver.tournament.PlayerStanding
import com.wingedsheep.gameserver.tournament.TournamentManager
import com.wingedsheep.gameserver.tournament.TournamentMatch
import com.wingedsheep.gameserver.tournament.TournamentRound
import com.wingedsheep.sdk.model.EntityId

// ============================================================================
// TournamentLobby Conversion
// ============================================================================

/**
 * Converts a TournamentLobby to its persistent representation.
 */
fun TournamentLobby.toPersistent(): PersistentTournamentLobby {
    return PersistentTournamentLobby(
        lobbyId = lobbyId,
        setCodes = setCodes,
        setNames = setNames,
        format = format.name,
        boosterCount = boosterCount,
        maxPlayers = maxPlayers,
        pickTimeSeconds = pickTimeSeconds,
        gamesPerMatch = gamesPerMatch,
        state = state.name,
        hostPlayerId = hostPlayerId?.value,
        players = players.mapKeys { it.key.value }.mapValues { (_, playerState) ->
            PersistentLobbyPlayer(
                playerId = playerState.identity.playerId.value,
                playerName = playerState.identity.playerName,
                token = playerState.identity.token,
                cardPoolNames = playerState.cardPool.map { it.name },
                currentPackNames = playerState.currentPack?.map { it.name },
                hasPicked = playerState.hasPicked,
                submittedDeck = playerState.submittedDeck
            )
        },
        currentPackNumber = currentPackNumber,
        currentPickNumber = currentPickNumber,
        playerOrder = getPlayerOrderForPersistence()
    )
}

/**
 * Extension to get player order for persistence (exposed from TournamentLobby).
 */
private fun TournamentLobby.getPlayerOrderForPersistence(): List<String> {
    return getPlayerOrder().map { it.value }
}

/**
 * Restores a TournamentLobby from its persistent representation.
 *
 * @param persistent The persisted lobby data
 * @param cardRegistry The card registry for resolving card names to definitions
 * @return A restored TournamentLobby and a list of PlayerIdentity objects to register
 */
fun restoreTournamentLobby(
    persistent: PersistentTournamentLobby,
    cardRegistry: CardRegistry
): Pair<TournamentLobby, List<PlayerIdentity>> {
    val lobby = TournamentLobby(
        lobbyId = persistent.lobbyId,
        setCodes = persistent.setCodes,
        setNames = persistent.setNames,
        format = TournamentFormat.valueOf(persistent.format),
        boosterCount = persistent.boosterCount,
        maxPlayers = persistent.maxPlayers,
        pickTimeSeconds = persistent.pickTimeSeconds,
        gamesPerMatch = persistent.gamesPerMatch
    )

    val playerIdentities = mutableListOf<PlayerIdentity>()

    // Restore players
    for ((playerIdStr, persistentPlayer) in persistent.players) {
        val playerId = EntityId(playerIdStr)
        val identity = PlayerIdentity(
            token = persistentPlayer.token,
            playerId = playerId,
            playerName = persistentPlayer.playerName
        ).also {
            it.currentLobbyId = persistent.lobbyId
        }
        playerIdentities.add(identity)

        // Resolve card names to CardDefinitions
        val cardPool = persistentPlayer.cardPoolNames.mapNotNull { cardName ->
            cardRegistry.getCard(cardName)
        }

        // Resolve current pack for draft
        val currentPack = persistentPlayer.currentPackNames?.mapNotNull { cardName ->
            cardRegistry.getCard(cardName)
        }

        val playerState = LobbyPlayerState(
            identity = identity,
            cardPool = cardPool,
            currentPack = currentPack,
            hasPicked = persistentPlayer.hasPicked,
            submittedDeck = persistentPlayer.submittedDeck
        )
        lobby.players[playerId] = playerState
    }

    // Restore state via internal method
    lobby.restoreFromPersistence(
        state = LobbyState.valueOf(persistent.state),
        hostPlayerId = persistent.hostPlayerId?.let { EntityId(it) }
    )

    // Restore draft state if applicable
    lobby.restoreDraftState(
        currentPackNumber = persistent.currentPackNumber,
        currentPickNumber = persistent.currentPickNumber,
        playerOrder = persistent.playerOrder.map { EntityId(it) }
    )

    return lobby to playerIdentities
}

// ============================================================================
// SealedSession Conversion (Legacy 2-player format)
// ============================================================================

/**
 * Converts a SealedSession to its persistent representation.
 */
fun SealedSession.toPersistent(): PersistentSealedSession {
    return PersistentSealedSession(
        sessionId = sessionId,
        setCodes = setCodes,
        setNames = setNames,
        state = state.name,
        players = players.mapKeys { it.key.value }.mapValues { (_, playerState) ->
            PersistentSealedPlayer(
                playerId = playerState.session.playerId.value,
                playerName = playerState.session.playerName,
                cardPoolNames = playerState.cardPool.map { it.name },
                submittedDeck = playerState.submittedDeck
            )
        }
    )
}

// ============================================================================
// TournamentManager Conversion
// ============================================================================

/**
 * Converts a TournamentManager to its persistent representation.
 */
fun TournamentManager.toPersistent(lobbyId: String): PersistentTournament {
    return PersistentTournament(
        lobbyId = lobbyId,
        standings = getStandingsForPersistence().mapKeys { it.key.value }.mapValues { (_, standing) ->
            PersistentStanding(
                playerId = standing.playerId.value,
                playerName = standing.playerName,
                wins = standing.wins,
                losses = standing.losses,
                draws = standing.draws
            )
        },
        rounds = getRoundsForPersistence().map { round ->
            PersistentRound(
                roundNumber = round.roundNumber,
                matches = round.matches.map { match ->
                    PersistentMatch(
                        player1Id = match.player1Id.value,
                        player2Id = match.player2Id?.value,
                        gameSessionId = match.gameSessionId,
                        winnerId = match.winnerId?.value,
                        isDraw = match.isDraw,
                        isComplete = match.isComplete
                    )
                }
            )
        },
        currentRoundIndex = getCurrentRoundIndexForPersistence(),
        totalRounds = totalRounds,
        gamesPerMatch = getGamesPerMatchForPersistence(),
        playerIds = playerIds.map { it.value }
    )
}

/**
 * Restores a TournamentManager from its persistent representation.
 *
 * @param persistent The persisted tournament data
 * @return A restored TournamentManager
 */
fun restoreTournamentManager(persistent: PersistentTournament): TournamentManager {
    // Reconstruct player list from standings (preserving order from playerIds)
    val players = persistent.playerIds.map { playerIdStr ->
        val standing = persistent.standings[playerIdStr]
            ?: throw IllegalStateException("Standing not found for player $playerIdStr")
        EntityId(playerIdStr) to standing.playerName
    }

    val tournament = TournamentManager(
        lobbyId = persistent.lobbyId,
        players = players,
        gamesPerMatch = persistent.gamesPerMatch
    )

    // Convert persistent rounds back to TournamentRounds
    val rounds = persistent.rounds.map { persistentRound ->
        TournamentRound(
            roundNumber = persistentRound.roundNumber,
            matches = persistentRound.matches.map { persistentMatch ->
                TournamentMatch(
                    player1Id = EntityId(persistentMatch.player1Id),
                    player2Id = persistentMatch.player2Id?.let { EntityId(it) },
                    gameSessionId = persistentMatch.gameSessionId,
                    winnerId = persistentMatch.winnerId?.let { EntityId(it) },
                    isDraw = persistentMatch.isDraw,
                    isComplete = persistentMatch.isComplete
                )
            }
        )
    }

    // Convert persistent standings back to PlayerStandings
    val standings = persistent.standings.mapKeys { EntityId(it.key) }.mapValues { (_, persistentStanding) ->
        PlayerStanding(
            playerId = EntityId(persistentStanding.playerId),
            playerName = persistentStanding.playerName,
            wins = persistentStanding.wins,
            losses = persistentStanding.losses,
            draws = persistentStanding.draws
        )
    }

    // Restore internal state
    tournament.restoreFromPersistence(
        rounds = rounds,
        standings = standings,
        currentRoundIndex = persistent.currentRoundIndex
    )

    return tournament
}
