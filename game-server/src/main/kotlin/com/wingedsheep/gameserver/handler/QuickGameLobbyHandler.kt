package com.wingedsheep.gameserver.handler

import com.wingedsheep.ai.engine.SealedDeckGenerator
import com.wingedsheep.ai.engine.deck.GeneratedDeck
import com.wingedsheep.gameserver.ai.AiGameManager
import com.wingedsheep.gameserver.ai.RandomDeckResolver
import com.wingedsheep.gameserver.config.GameProperties
import com.wingedsheep.gameserver.deck.DeckValidator
import com.wingedsheep.gameserver.deck.EasterEggDeckInjector
import com.wingedsheep.gameserver.lobby.AiDeckSpec
import com.wingedsheep.gameserver.lobby.AiDeckSpecView
import com.wingedsheep.gameserver.lobby.MomirBasicSetup
import com.wingedsheep.gameserver.lobby.QuickGameLobby
import com.wingedsheep.gameserver.lobby.QuickGameLobbyPlayer
import com.wingedsheep.gameserver.lobby.QuickGameLobbyRepository
import com.wingedsheep.gameserver.protocol.ClientMessage
import com.wingedsheep.gameserver.protocol.ErrorCode
import com.wingedsheep.gameserver.protocol.ServerMessage
import com.wingedsheep.gameserver.repository.GameRepository
import com.wingedsheep.gameserver.repository.LobbyRepository
import com.wingedsheep.gameserver.session.GameSession
import com.wingedsheep.gameserver.session.PlayerSession
import com.wingedsheep.gameserver.session.SessionRegistry
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.sdk.core.DeckFormat
import com.wingedsheep.sdk.model.Deck
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketSession

/**
 * Lifecycle handler for the Quick Game lobby — the staging area users see between picking
 * "Quick Game" on the home screen and the actual match start. Replaces the old in-place
 * `waitingGameSession: @Volatile` field on [GamePlayHandler] with proper per-lobby locking
 * via [QuickGameLobbyRepository].
 *
 * Responsibilities:
 *  - Create / join / leave a lobby.
 *  - Accept and validate per-player deck submissions (server-authoritative validation via
 *    [DeckValidator]; an empty deck list is accepted and means "use the random sealed pool").
 *  - Track ready state per player.
 *  - When all players in the lobby are ready, hand off to [GamePlayHandler.startQuickGameFromLobby]
 *    which builds the [GameSession], wires AI if needed, and sends `GameStarted`.
 *
 * An AI can fill or leave the ordinary 1v1 opponent seat and is auto-ready while present. The
 * create-time `vsAi` shortcut uses that same state for the wizard's fast solo path.
 */
@Component
class QuickGameLobbyHandler(
    private val sessionRegistry: SessionRegistry,
    private val lobbyRepository: QuickGameLobbyRepository,
    private val tournamentLobbyRepository: LobbyRepository,
    private val gameRepository: GameRepository,
    private val sender: MessageSender,
    private val cardRegistry: CardRegistry,
    private val printingRegistry: com.wingedsheep.engine.registry.PrintingRegistry,
    private val tokenArtRegistry: com.wingedsheep.engine.registry.TokenArtRegistry,
    private val deckValidator: DeckValidator,
    private val deckGenerator: SealedDeckGenerator,
    private val gameProperties: GameProperties,
    private val aiGameManager: AiGameManager,
    private val randomDeckResolver: RandomDeckResolver,
    private val boosterGenerator: com.wingedsheep.engine.limited.BoosterGenerator,
    private val gamePlayHandler: GamePlayHandler,
) {
    private val logger = LoggerFactory.getLogger(QuickGameLobbyHandler::class.java)

    /**
     * Optional delegate for tournament lobby joins. Wired by [GameWebSocketHandler.wireCallbacks]
     * so that a code typed in the home-screen "Join" field can land in either a quick-game lobby
     * or a tournament lobby without the user having to know which is which.
     */
    var joinTournamentLobbyCallback: ((WebSocketSession, ClientMessage.JoinLobby) -> Unit)? = null

    fun handle(session: WebSocketSession, message: ClientMessage) {
        when (message) {
            is ClientMessage.CreateQuickGameLobby -> handleCreate(session, message)
            is ClientMessage.JoinQuickGameLobby -> handleJoin(session, message)
            is ClientMessage.LeaveQuickGameLobby -> handleLeave(session)
            is ClientMessage.SubmitQuickGameLobbyDeck -> handleSubmitDeck(session, message)
            is ClientMessage.SetQuickGameLobbyReady -> handleSetReady(session, message)
            is ClientMessage.SetQuickGameLobbySetCode -> handleSetSetCode(session, message)
            is ClientMessage.SetQuickGameLobbyPublic -> handleSetPublic(session, message)
            is ClientMessage.SetQuickGameLobbyRanked -> handleSetRanked(session, message)
            is ClientMessage.SetQuickGameLobbyFormat -> handleSetFormat(session, message)
            is ClientMessage.SetQuickGameAiDeck -> handleSetAiDeck(session, message)
            is ClientMessage.AddQuickGameAi -> handleAddAi(session)
            is ClientMessage.RemoveQuickGameAi -> handleRemoveAi(session)
            else -> {}
        }
    }

    private fun handleAddAi(session: WebSocketSession) {
        val playerSession = sessionRegistry.getPlayerSession(session.id) ?: run {
            sender.sendError(session, ErrorCode.NOT_CONNECTED, "Not connected"); return
        }
        val lobby = lobbyRepository.findContainingPlayer(playerSession.playerId) ?: run {
            sender.sendError(session, ErrorCode.GAME_NOT_FOUND, "Not in a lobby"); return
        }
        lobbyRepository.withLock(lobby.lobbyId) { current ->
            if (current == null) return@withLock
            val host = current.players.firstOrNull { !it.isAi }
            if (host?.playerId != playerSession.playerId) {
                sender.sendError(session, ErrorCode.INVALID_ACTION, "Only the host can add an AI player")
                return@withLock
            }
            if (!aiGameManager.isEnabled) {
                sender.sendError(session, ErrorCode.INVALID_ACTION, "AI opponent is not enabled on this server")
                return@withLock
            }
            if (current.twoHeadedGiant || current.players.size != 1) {
                sender.sendError(session, ErrorCode.INVALID_ACTION, "The opponent seat is not available")
                return@withLock
            }
            current.players += QuickGameLobbyPlayer(
                playerId = com.wingedsheep.sdk.model.EntityId("ai-pending-${current.lobbyId}"),
                playerName = "AI Opponent",
                isAi = true,
                ready = true,
            )
            current.vsAi = true
            current.isPublic = false
            current.ranked = false
            broadcastState(current)
        }
    }

    private fun handleRemoveAi(session: WebSocketSession) {
        val playerSession = sessionRegistry.getPlayerSession(session.id) ?: run {
            sender.sendError(session, ErrorCode.NOT_CONNECTED, "Not connected"); return
        }
        val lobby = lobbyRepository.findContainingPlayer(playerSession.playerId) ?: run {
            sender.sendError(session, ErrorCode.GAME_NOT_FOUND, "Not in a lobby"); return
        }
        lobbyRepository.withLock(lobby.lobbyId) { current ->
            if (current == null) return@withLock
            val host = current.players.firstOrNull { !it.isAi }
            if (host?.playerId != playerSession.playerId) {
                sender.sendError(session, ErrorCode.INVALID_ACTION, "Only the host can remove the AI player")
                return@withLock
            }
            if (!current.players.removeIf { it.isAi }) return@withLock
            current.vsAi = false
            broadcastState(current)
        }
    }

    /**
     * Host picks what the AI opponent plays (see [AiDeckSpec]).
     *
     * A [AiDeckSpec.Fixed] list is validated here, against the lobby's current format, so an
     * illegal choice is refused at the point the host makes it rather than silently seating an
     * illegal deck. [AiDeckSpec.Sets] only checks that the codes exist — whether the resulting pool
     * can carry a deck is the resolver's problem at game start, and it falls back rather than fails.
     */
    private fun handleSetAiDeck(session: WebSocketSession, message: ClientMessage.SetQuickGameAiDeck) {
        val playerSession = sessionRegistry.getPlayerSession(session.id) ?: run {
            sender.sendError(session, ErrorCode.NOT_CONNECTED, "Not connected"); return
        }
        val lobby = lobbyRepository.findContainingPlayer(playerSession.playerId) ?: run {
            sender.sendError(session, ErrorCode.GAME_NOT_FOUND, "Not in a lobby"); return
        }
        lobbyRepository.withLock(lobby.lobbyId) { current ->
            if (current == null) return@withLock
            if (!current.vsAi) {
                sender.sendError(session, ErrorCode.INVALID_ACTION, "This lobby has no AI opponent")
                return@withLock
            }
            val host = current.players.firstOrNull { !it.isAi }
            if (host?.playerId != playerSession.playerId) {
                sender.sendError(session, ErrorCode.INVALID_ACTION, "Only the host can choose the AI's deck")
                return@withLock
            }
            when (val spec = message.spec) {
                is AiDeckSpec.Fixed -> {
                    if (spec.deckList.isEmpty()) {
                        sender.sendError(session, ErrorCode.INVALID_ACTION, "The AI's deck is empty")
                        return@withLock
                    }
                    val result = validateAiDeck(spec, current.format)
                    if (!result.valid) {
                        val reason = result.errors.firstOrNull()?.message ?: "Deck is not legal"
                        sender.sendError(session, ErrorCode.INVALID_ACTION, "AI deck rejected: $reason")
                        return@withLock
                    }
                }
                is AiDeckSpec.Sets -> {
                    val unknown = spec.setCodes.filterNot { it in boosterGenerator.availableSets }
                    if (unknown.isNotEmpty()) {
                        sender.sendError(
                            session,
                            ErrorCode.INVALID_ACTION,
                            "Unknown set${if (unknown.size > 1) "s" else ""}: ${unknown.joinToString(", ")}",
                        )
                        return@withLock
                    }
                }
                is AiDeckSpec.Auto -> {}
            }
            if (current.aiDeckSpec == message.spec) return@withLock
            current.aiDeckSpec = message.spec
            broadcastState(current)
        }
    }

    private fun handleSetFormat(session: WebSocketSession, message: ClientMessage.SetQuickGameLobbyFormat) {
        val playerSession = sessionRegistry.getPlayerSession(session.id) ?: run {
            sender.sendError(session, ErrorCode.NOT_CONNECTED, "Not connected"); return
        }
        val lobby = lobbyRepository.findContainingPlayer(playerSession.playerId) ?: run {
            sender.sendError(session, ErrorCode.GAME_NOT_FOUND, "Not in a lobby"); return
        }
        lobbyRepository.withLock(lobby.lobbyId) { current ->
            if (current == null) return@withLock
            val host = current.players.firstOrNull { !it.isAi }
            if (host?.playerId != playerSession.playerId) {
                sender.sendError(session, ErrorCode.INVALID_ACTION, "Only the host can change the format")
                return@withLock
            }
            if (current.format == message.format && current.momirBasic == message.momirBasic) return@withLock
            // Momir Basic is a "custom format" entry in the same dropdown: picking it flips the
            // lobby into the deckbuilding-free Vanguard mode (mutually exclusive with a deck-format
            // restriction). Any other choice clears Momir and applies the constructed format.
            current.momirBasic = message.momirBasic
            // applyFormat re-derives the lobby's Rules axis from the new legality, so the two can't
            // drift apart on this lobby kind (which has no separate Rules control).
            current.applyFormat(if (message.momirBasic) null else message.format)
            // Re-validate every submitted deck under the new format. Submissions that no longer
            // pass un-ready the player so they have to update their deck or accept a new format.
            // Momir has no deckbuilding, so there is nothing to re-validate.
            if (!message.momirBasic) {
                for (player in current.players) {
                    if (player.isAi) continue
                    val deck = player.deckList ?: continue
                    if (deck.isEmpty()) continue // Random pool — format restriction doesn't apply.
                    val result = deckValidator.validate(deck, message.format)
                    if (!result.valid) player.ready = false
                }
                // Same rule for the AI's hand-picked deck, except the AI has no ready flag to
                // clear: an illegal list is dropped back to Auto, which always builds something
                // legal for the new format. The host sees the seat's label change.
                val aiSpec = current.aiDeckSpec
                if (aiSpec is AiDeckSpec.Fixed && !validateAiDeck(aiSpec, message.format).valid) {
                    logger.info(
                        "Lobby {}: AI deck '{}' is not legal in {}; reverting the AI to Auto",
                        current.lobbyId,
                        aiSpec.label,
                        message.format?.displayName ?: "no format",
                    )
                    current.aiDeckSpec = AiDeckSpec.Auto
                }
            }
            broadcastState(current)
        }
    }

    private fun handleSetPublic(session: WebSocketSession, message: ClientMessage.SetQuickGameLobbyPublic) {
        val playerSession = sessionRegistry.getPlayerSession(session.id) ?: run {
            sender.sendError(session, ErrorCode.NOT_CONNECTED, "Not connected"); return
        }
        val lobby = lobbyRepository.findContainingPlayer(playerSession.playerId) ?: run {
            sender.sendError(session, ErrorCode.GAME_NOT_FOUND, "Not in a lobby"); return
        }
        lobbyRepository.withLock(lobby.lobbyId) { current ->
            if (current == null) return@withLock
            // Host-only: first non-AI player is the host (matches the leave/close convention).
            val host = current.players.firstOrNull { !it.isAi }
            if (host?.playerId != playerSession.playerId) {
                sender.sendError(session, ErrorCode.INVALID_ACTION, "Only the host can change visibility")
                return@withLock
            }
            // AI lobbies are single-player — there's no second seat to discover.
            val effective = message.isPublic && !current.vsAi
            if (current.isPublic == effective) return@withLock
            current.isPublic = effective
            broadcastState(current)
        }
    }

    private fun handleSetRanked(session: WebSocketSession, message: ClientMessage.SetQuickGameLobbyRanked) {
        val playerSession = sessionRegistry.getPlayerSession(session.id) ?: run {
            sender.sendError(session, ErrorCode.NOT_CONNECTED, "Not connected"); return
        }
        val lobby = lobbyRepository.findContainingPlayer(playerSession.playerId) ?: run {
            sender.sendError(session, ErrorCode.GAME_NOT_FOUND, "Not in a lobby"); return
        }
        lobbyRepository.withLock(lobby.lobbyId) { current ->
            if (current == null) return@withLock
            val host = current.players.firstOrNull { !it.isAi }
            if (host?.playerId != playerSession.playerId) {
                sender.sendError(session, ErrorCode.INVALID_ACTION, "Only the host can change ranked")
                return@withLock
            }
            // Ranked is only offered for a standard 1v1 human-vs-human lobby.
            val effective = message.ranked && current.rankedEligible
            if (current.ranked == effective) return@withLock
            current.ranked = effective
            broadcastState(current)
        }
    }

    private fun handleSetSetCode(session: WebSocketSession, message: ClientMessage.SetQuickGameLobbySetCode) {
        val playerSession = sessionRegistry.getPlayerSession(session.id) ?: run {
            sender.sendError(session, ErrorCode.NOT_CONNECTED, "Not connected"); return
        }
        val lobby = lobbyRepository.findContainingPlayer(playerSession.playerId) ?: run {
            sender.sendError(session, ErrorCode.GAME_NOT_FOUND, "Not in a lobby"); return
        }
        lobbyRepository.withLock(lobby.lobbyId) { current ->
            if (current == null) return@withLock
            val player = current.findPlayer(playerSession.playerId) ?: return@withLock
            val requested = message.setCodes.filter { it.isNotBlank() }.distinct()
            if (player.setCodes == requested) return@withLock
            player.setCodes = requested
            player.setCode = requested.singleOrNull()
            broadcastState(current)
        }
    }

    private fun handleCreate(session: WebSocketSession, message: ClientMessage.CreateQuickGameLobby) {
        val playerSession = sessionRegistry.getPlayerSession(session.id) ?: run {
            sender.sendError(session, ErrorCode.NOT_CONNECTED, "Not connected"); return
        }
        if (lobbyRepository.findContainingPlayer(playerSession.playerId) != null) {
            sender.sendError(session, ErrorCode.INVALID_ACTION, "Already in a lobby")
            return
        }
        if (message.vsAi && !aiGameManager.isEnabled) {
            sender.sendError(session, ErrorCode.INVALID_ACTION, "AI opponent is not enabled on this server")
            return
        }
        // A quick lobby's `vsAi` seats exactly one AI, and 2HG needs three to fill its four seats.
        // The AI itself is no obstacle — a Two-Headed Giant tournament lobby seats AI teammates and
        // opponents — so this is about which lobby can hold them, not about what the AI can play.
        if (message.twoHeadedGiant && message.vsAi) {
            sender.sendError(
                session,
                ErrorCode.INVALID_ACTION,
                "A quick game seats only one AI — create a Two-Headed Giant lobby to play a full table against them",
            )
            return
        }
        if (message.twoHeadedGiant && message.momirBasic) {
            sender.sendError(session, ErrorCode.INVALID_ACTION, "Two-Headed Giant and Momir Basic cannot be combined")
            return
        }

        val lobby = QuickGameLobby(
            vsAi = message.vsAi,
            setCode = message.setCode,
            // AI lobbies are single-player — never publicly listed.
            isPublic = message.isPublic && !message.vsAi,
            // Momir Basic has no deck-construction restriction; the two flags are mutually exclusive.
            format = if (message.momirBasic) null else message.format,
            momirBasic = message.momirBasic,
            twoHeadedGiant = message.twoHeadedGiant,
        )
        // Ranked only applies to a standard 1v1 human-vs-human lobby (not AI / Two-Headed Giant).
        lobby.ranked = message.ranked && lobby.rankedEligible
        lobby.players += QuickGameLobbyPlayer(
            playerId = playerSession.playerId,
            playerName = playerSession.playerName
        )
        if (message.vsAi) {
            lobby.players += QuickGameLobbyPlayer(
                playerId = com.wingedsheep.sdk.model.EntityId("ai-pending-${lobby.lobbyId}"),
                playerName = "AI Opponent",
                isAi = true,
                ready = true,
                // AI deck is generated at game start by AiGameManager, so an empty map is fine.
                deckList = emptyMap()
            )
        }
        lobbyRepository.save(lobby)
        markPlayerInLobby(playerSession.playerId, lobby.lobbyId)

        logger.info("Quick game lobby ${lobby.lobbyId} created by ${playerSession.playerName} (vsAi=${message.vsAi})")
        broadcastState(lobby)
    }

    private fun handleJoin(session: WebSocketSession, message: ClientMessage.JoinQuickGameLobby) {
        val playerSession = sessionRegistry.getPlayerSession(session.id) ?: run {
            sender.sendError(session, ErrorCode.NOT_CONNECTED, "Not connected"); return
        }

        // Unified join: if the code points at a tournament lobby instead of a quick-game lobby,
        // delegate to the tournament join handler. The home-screen "Join" field doesn't need to
        // know which kind of lobby is behind a given code.
        if (lobbyRepository.findById(message.lobbyId) == null
            && tournamentLobbyRepository.findLobbyById(message.lobbyId) != null) {
            joinTournamentLobbyCallback?.invoke(session, ClientMessage.JoinLobby(message.lobbyId))
            return
        }

        lobbyRepository.withLock(message.lobbyId) { lobby ->
            if (lobby == null) {
                sender.sendError(session, ErrorCode.GAME_NOT_FOUND, "Lobby not found: ${message.lobbyId}")
                return@withLock
            }
            // Allow re-join if the player was already in this lobby (idempotent reconnect).
            val existing = lobby.findPlayer(playerSession.playerId)
            if (existing == null) {
                if (lobby.vsAi) {
                    sender.sendError(session, ErrorCode.INVALID_ACTION, "This is a single-player lobby")
                    return@withLock
                }
                if (lobby.isFull) {
                    sender.sendError(session, ErrorCode.GAME_FULL, "Lobby is full")
                    return@withLock
                }
                lobby.players += QuickGameLobbyPlayer(
                    playerId = playerSession.playerId,
                    playerName = playerSession.playerName
                )
            }
            markPlayerInLobby(playerSession.playerId, lobby.lobbyId)
            broadcastState(lobby)
        }
    }

    private fun handleLeave(session: WebSocketSession) {
        val playerSession = sessionRegistry.getPlayerSession(session.id) ?: return
        val lobby = lobbyRepository.findContainingPlayer(playerSession.playerId) ?: return

        lobbyRepository.withLock(lobby.lobbyId) { current ->
            if (current == null) return@withLock
            // Host (first non-AI player) leaving closes the lobby for everyone.
            val firstHuman = current.players.firstOrNull { !it.isAi }
            if (firstHuman?.playerId == playerSession.playerId) {
                logger.info("Host ${playerSession.playerName} left lobby ${current.lobbyId}; closing for all")
                current.players.forEach { clearPlayerLobbyMembership(it.playerId, current.lobbyId) }
                broadcastClosed(current, "Host left the lobby")
                lobbyRepository.remove(current.lobbyId)
            } else {
                current.players.removeIf { it.playerId == playerSession.playerId }
                clearPlayerLobbyMembership(playerSession.playerId, current.lobbyId)
                broadcastState(current)
            }
        }
    }

    private fun markPlayerInLobby(playerId: com.wingedsheep.sdk.model.EntityId, lobbyId: String) {
        sessionRegistry.getAllIdentities()
            .firstOrNull { it.playerId == playerId }
            ?.let { it.currentQuickGameLobbyId = lobbyId }
    }

    private fun clearPlayerLobbyMembership(playerId: com.wingedsheep.sdk.model.EntityId, lobbyId: String) {
        sessionRegistry.getAllIdentities()
            .firstOrNull { it.playerId == playerId && it.currentQuickGameLobbyId == lobbyId }
            ?.let { it.currentQuickGameLobbyId = null }
    }

    private fun handleSubmitDeck(session: WebSocketSession, message: ClientMessage.SubmitQuickGameLobbyDeck) {
        val playerSession = sessionRegistry.getPlayerSession(session.id) ?: run {
            sender.sendError(session, ErrorCode.NOT_CONNECTED, "Not connected"); return
        }
        val lobby = lobbyRepository.findContainingPlayer(playerSession.playerId) ?: run {
            sender.sendError(session, ErrorCode.GAME_NOT_FOUND, "Not in a lobby"); return
        }
        // Empty deck = "random pool" — skip validation. The commander field, if any, is meaningless
        // without an accompanying deck list and gets dropped at the lobby layer below.
        if (message.deckList.isNotEmpty()) {
            // Commander-aware path: the wire format `deckList` represents the *full* deck (matches
            // the saved-deck "merged" view the picker emits), but `Deck.cards` follows the server
            // convention where the commander lives in `Deck.commander` and is NOT counted in
            // `cards`. Strip one copy here so the validator (which re-adds it) doesn't trip the
            // singleton check on the commander itself.
            val result = if (message.commander != null) {
                val cardsWithoutCommander = stripCommanderFromCards(message.deckList, message.commander)
                val deckCards = cardsWithoutCommander.flatMap { (name, count) -> List(count) { name } }
                val richEntries = message.cardEntries
                    ?.filterNot { it.name == message.commander && it.printing == message.commanderPrinting }
                    ?.takeIf { it.isNotEmpty() }
                val deck = if (richEntries != null) {
                    com.wingedsheep.sdk.model.Deck.fromEntries(
                        entries = richEntries.map { com.wingedsheep.sdk.model.CardEntry(it.name, it.printing) },
                        commander = message.commander,
                        commanderPrinting = message.commanderPrinting,
                    )
                } else {
                    com.wingedsheep.sdk.model.Deck(
                        cards = deckCards,
                        commander = message.commander,
                        commanderPrinting = message.commanderPrinting,
                    )
                }
                deckValidator.validate(deck, lobby.format)
            } else {
                deckValidator.validate(
                    deckList = message.deckList,
                    format = lobby.format,
                    cardEntries = message.cardEntries,
                )
            }
            if (!result.valid) {
                sender.sendError(
                    session,
                    ErrorCode.INVALID_DECK,
                    result.errors.firstOrNull()?.message ?: "Invalid deck"
                )
                return
            }
        }
        lobbyRepository.withLock(lobby.lobbyId) { current ->
            if (current == null) return@withLock
            val player = current.findPlayer(playerSession.playerId) ?: return@withLock
            // No-op if the same deck is being resubmitted: avoids ping-pong with the picker
            // (which can re-emit its current value on every render) and keeps the ready flag
            // sticky as long as the player's chosen deck hasn't actually changed.
            if (player.deckList == message.deckList && player.commander == message.commander) return@withLock
            player.deckList = message.deckList
            // For random-pool submissions the commander field is meaningless; drop it so a stale
            // commander from a prior submission doesn't leak into the game start.
            player.commander = if (message.deckList.isNotEmpty()) message.commander else null
            // Submitting a *new* deck un-readies the player so they have to confirm again.
            player.ready = false
            broadcastState(current)
        }
    }

    private fun handleSetReady(session: WebSocketSession, message: ClientMessage.SetQuickGameLobbyReady) {
        val playerSession = sessionRegistry.getPlayerSession(session.id) ?: run {
            sender.sendError(session, ErrorCode.NOT_CONNECTED, "Not connected"); return
        }
        val lobby = lobbyRepository.findContainingPlayer(playerSession.playerId) ?: run {
            sender.sendError(session, ErrorCode.GAME_NOT_FOUND, "Not in a lobby"); return
        }

        lobbyRepository.withLock(lobby.lobbyId) { current ->
            if (current == null) return@withLock
            val player = current.findPlayer(playerSession.playerId) ?: return@withLock
            // Momir Basic has no deckbuilding (fixed 60 basics), so there is nothing to pick before
            // readying. For every other lobby, ready-up requires a submitted deck (null = nothing
            // chosen yet).
            if (message.ready && !current.momirBasic && player.deckList == null) {
                sender.sendError(session, ErrorCode.INVALID_ACTION, "Pick a deck before readying up")
                return@withLock
            }
            // A host-supplied AI deck under Commander rules has to name its commander; the
            // generated paths pick their own, so only a Fixed spec can be missing one.
            val aiDeck = current.aiDeckSpec
            if (message.ready && current.vsAi && current.usesCommanderRules &&
                aiDeck is AiDeckSpec.Fixed && aiDeck.commander.isNullOrBlank()
            ) {
                sender.sendError(
                    session,
                    ErrorCode.INVALID_ACTION,
                    "Pick a Commander deck for the AI before readying up",
                )
                return@withLock
            }
            player.ready = message.ready
            broadcastState(current)
            if (current.allReady() && !current.started) {
                current.started = true
                startGame(current)
            }
        }
    }

    private fun startGame(lobby: QuickGameLobby) {
        // The one Rules × Table conflict, read from the single statement of it.
        lobby.rulesTableConflict?.let { conflict ->
            logger.warn("Lobby ${lobby.lobbyId}: $conflict")
            broadcastClosed(lobby, conflict)
            lobbyRepository.remove(lobby.lobbyId)
            return
        }

        // Ranked play only counts when every human seat is a signed-in account (no guests). A guest may
        // have joined a public ranked lobby since the host toggled it on; rather than block the start we
        // downgrade to a casual game and play on.
        if (lobby.ranked) {
            val humans = lobby.players.filter { !it.isAi }
            if (!lobby.rankedEligible || humans.any { userIdOf(it.playerId) == null }) {
                logger.info("Quick lobby ${lobby.lobbyId}: starting unranked — not all players signed in")
                lobby.ranked = false
            }
        }

        val gameSession = GameSession(
            cardRegistry = cardRegistry,
            useHandSmoother = gameProperties.handSmoother.enabled,
            debugMode = gameProperties.debugMode,
            printingRegistry = printingRegistry,
            tokenArtRegistry = tokenArtRegistry,
            // Four seats for Two-Headed Giant (CR 810), two otherwise.
            maxPlayers = lobby.maxPlayers,
        )
        // A validated ranked lobby: this game adjusts both players' ELO for the derived mode.
        if (lobby.ranked) {
            gameSession.ranked = true
            gameSession.rankedMode = com.wingedsheep.gameserver.ranking.Ranked
                .modeForQuickGame(lobby.rules, lobby.format, lobby.momirBasic)
        }
        // Commander rules run on the engine's Commander format; everything else falls through to
        // Standard. Brawl-specific tweaks (60 cards, alternative life total) are Phase 4 territory —
        // Phase 1 covers Commander.
        if (lobby.usesCommanderRules) {
            gameSession.engineFormat = com.wingedsheep.sdk.core.Format.Commander()
        }
        // Two-Headed Giant: run under the team format and forward the seat→team partition. The
        // engine stamps TeamComponent and sets up shared life / turns / combat (Phases 1–5).
        if (lobby.twoHeadedGiant) {
            gameSession.engineFormat = com.wingedsheep.sdk.core.Format.TwoHeadedGiant()
            gameSession.teams = lobby.teamAssignment()
        }
        // Each player can pick their own set for a Random pool. For a vs-AI lobby the AI mirrors
        // the (single) human's set so both sides play the same set. Resolve that set ONCE here —
        // rolling a single random set when the human left the pool on "Random" — and reuse it for
        // the human's deck and the AI's deck. Previously the human's deck and the AI's deck each
        // rolled their own random set, so a "Random Set" pool handed them two different sets.
        val humanPlayers = lobby.players.filter { !it.isAi }
        val aiSetCode = lobby.setCode
            ?: humanPlayers.firstOrNull()?.setCode
            ?: deckGenerator.randomSetCode()
        gameSession.quickGameSetCode = aiSetCode
        // Momir Basic: both seats play the fixed 60-basic deck and the avatar flips creatures from
        // the whole card base (every set). The engine reads `eligibleCreatureNames` from this format
        // at game init and when the avatar's "{X}, Discard a card" ability resolves.
        if (lobby.momirBasic) {
            val pool = MomirBasicSetup.allCreaturePool()
            gameSession.engineFormat = com.wingedsheep.sdk.core.Format.MomirBasic(eligibleCreatureNames = pool)
            logger.info("Momir Basic lobby ${lobby.lobbyId}: ${pool.size} eligible creatures (all sets)")
        }
        gameSession.publicSpectate = lobby.isPublic && !lobby.vsAi

        // Every seat's deck is resolved *before* anyone is seated. Under Commander rules a deck with
        // no designated commander can't be seated at all — GameInitializer requires one — and
        // discovering that halfway through the seating loop would leave the seats already wired
        // pointing at a game that never starts.
        //
        // For Momir Basic every seat plays the same fixed 60 basics. Otherwise: a human's own
        // submitted deck, or (for a Random pool, and for the AI seat's spec) whatever the resolver
        // builds under the lobby's format — which for a commander shape now includes picking the
        // commander.
        val humanDecks = humanPlayers.associate { lobbyPlayer ->
            // Momir Basic has no deckbuilding: the fixed 60 basics are substituted at seating time.
            if (lobby.momirBasic) return@associate lobbyPlayer.playerId to GeneratedDeck(emptyMap())
            // In a vs-AI lobby, share the resolved set with the single human so a random pool draws
            // from the same set the AI got; multi-human lobbies keep each player's own set, rolling
            // an independent random when they didn't pick one.
            val randomFallbackSet = if (lobby.vsAi) aiSetCode else deckGenerator.randomSetCode()
            val resolved = resolveDeck(lobbyPlayer, randomFallbackSet, lobby.format, lobby.usesCommanderRules)
            lobbyPlayer.playerId to resolved.copy(
                deckList = EasterEggDeckInjector.maybeInjectEasterEggs(
                    lobbyPlayer.playerName,
                    resolved.deckList,
                    gameProperties.easterEggs.enabled,
                ),
                // A submitted deck names its own commander on the lobby seat; a generated one names
                // it on the resolved deck. Either way the seat needs exactly one.
                commander = lobbyPlayer.commander ?: resolved.commander,
            )
        }
        val aiDeck = when {
            !lobby.vsAi -> null
            lobby.momirBasic -> GeneratedDeck(MomirBasicSetup.fixedBasicDeck)
            else -> randomDeckResolver.resolve(lobby.aiDeckSpec, lobby.format, aiSetCode, lobby.usesCommanderRules)
        }

        if (lobby.usesCommanderRules && !lobby.momirBasic) {
            val seatWithoutCommander = humanPlayers.firstOrNull { humanDecks[it.playerId]?.commander == null }
            if (seatWithoutCommander != null) {
                logger.warn(
                    "Lobby ${lobby.lobbyId}: no commander for ${seatWithoutCommander.playerName} at ${lobby.format} game start",
                )
                broadcastClosed(
                    lobby,
                    "${seatWithoutCommander.playerName}'s deck has no commander designated — pick a deck with a commander to play ${lobby.format?.displayName ?: "Commander"}",
                )
                lobbyRepository.remove(lobby.lobbyId)
                return
            }
            if (aiDeck != null && aiDeck.commander == null) {
                logger.error("Lobby ${lobby.lobbyId}: could not build a ${lobby.format?.displayName ?: "Commander"} deck for the AI seat")
                broadcastClosed(lobby, "Could not build a Commander deck for the AI — pick one for it and try again")
                lobbyRepository.remove(lobby.lobbyId)
                return
            }
        }

        for (lobbyPlayer in humanPlayers) {
            val deckList = humanDecks.getValue(lobbyPlayer.playerId).deckList
            val playerSession = sessionRegistry
                .getAllIdentities()
                .firstOrNull { it.playerId == lobbyPlayer.playerId }
                ?.let { identity ->
                    val ws = identity.webSocketSession ?: return@let null
                    sessionRegistry.getPlayerSession(ws.id)
                }
            if (playerSession == null) {
                logger.error("Lobby ${lobby.lobbyId}: lost session for ${lobbyPlayer.playerName} on game start")
                broadcastClosed(lobby, "A player disconnected before the game started")
                lobbyRepository.remove(lobby.lobbyId)
                return
            }
            // Pass the commander only under Commander rules; clear it otherwise so a stale
            // commander on a saved deck doesn't accidentally route into a Standard game. Strip
            // one copy of the commander out of the wire deck list so the engine sees `cards`
            // (= library) excluding the commander, matching `Deck.cards` convention.
            val commander = humanDecks.getValue(lobbyPlayer.playerId).commander
                ?.takeIf { lobby.usesCommanderRules }
            // Momir Basic ignores any submitted deck: every seat plays the same fixed 60 basics.
            val engineDeckList = when {
                lobby.momirBasic -> MomirBasicSetup.fixedBasicDeck
                commander != null -> stripCommanderFromCards(deckList, commander)
                else -> deckList
            }
            gameSession.addPlayer(playerSession, engineDeckList, commanderCardName = commander)
            // Persistence info so a mid-game reconnect can find the player by token.
            val token = sessionRegistry.getTokenByWsId(playerSession.webSocketSession.id)
            if (token != null) {
                gameSession.setPlayerPersistenceInfo(playerSession.playerId, playerSession.playerName, token)
                sessionRegistry.getIdentityByToken(token)?.currentGameSessionId = gameSession.sessionId
            }
        }

        gameRepository.save(gameSession)

        if (aiDeck != null) {
            // AI is added by AiGameManager, from the deck resolved above — the deck-source policy
            // lives next to the lobby state that configures it, not inside AiGameManager.
            aiGameManager.createAiOpponent(
                gameSession = gameSession,
                setCode = aiSetCode,
                onActionReady = { id, action -> gamePlayHandler.handleAiAction(gameSession, id, action) },
                onMulliganKeep = { id -> gamePlayHandler.handleAiMulliganKeep(gameSession, id) },
                onMulliganTake = { id -> gamePlayHandler.handleAiMulliganTake(gameSession, id) },
                onBottomCards = { id, cardIds -> gamePlayHandler.handleAiBottomCards(gameSession, id, cardIds) },
                deckOverride = aiDeck.deckList,
                // Cleared outside Commander rules so a commander on a host-picked deck can't route
                // into a Standard game.
                commanderCardName = aiDeck.commander?.takeIf { lobby.usesCommanderRules },
            )
        }

        // Tell players the game has been created (so the client can switch overlays) before mulligan kicks in.
        for (lobbyPlayer in humanPlayers) {
            val ws = sessionRegistry
                .getAllIdentities()
                .firstOrNull { it.playerId == lobbyPlayer.playerId }
                ?.webSocketSession ?: continue
            sender.send(ws, ServerMessage.GameCreated(gameSession.sessionId))
        }

        gamePlayHandler.startGame(gameSession)
        logger.info("Quick lobby ${lobby.lobbyId} → game ${gameSession.sessionId} started")
        // The lobby has done its job; remove it so the same player can create another later.
        lobby.players.forEach { clearPlayerLobbyMembership(it.playerId, lobby.lobbyId) }
        lobbyRepository.remove(lobby.lobbyId)
    }

    /**
     * Re-send the current lobby state to a reconnecting player. Called by [ConnectionHandler]
     * after a successful reconnect, when the player's identity still has a `currentQuickGameLobbyId`.
     */
    fun handleReconnect(session: WebSocketSession, playerId: com.wingedsheep.sdk.model.EntityId, lobbyId: String) {
        val lobby = lobbyRepository.findById(lobbyId)
        if (lobby == null) {
            // Lobby is gone (host left, server restarted, etc.) — clear stale identity pointer
            // and send a closed-message so the client can fall back to the home screen cleanly.
            sessionRegistry.getAllIdentities()
                .firstOrNull { it.playerId == playerId && it.currentQuickGameLobbyId == lobbyId }
                ?.let { it.currentQuickGameLobbyId = null }
            sender.send(session, ServerMessage.QuickGameLobbyClosed("Lobby no longer exists"))
            return
        }
        // Send just this player the current snapshot.
        val msg = ServerMessage.QuickGameLobbyState(
            lobbyId = lobby.lobbyId,
            vsAi = lobby.vsAi,
            setCode = lobby.setCode,
            players = lobby.players.mapIndexed { i, p -> p.toView(lobby, i) },
            youPlayerId = playerId,
            canStart = lobby.allReady(),
            isPublic = lobby.isPublic,
            format = lobby.format,
            rules = lobby.rules.name,
            momirBasic = lobby.momirBasic,
            twoHeadedGiant = lobby.twoHeadedGiant,
            maxPlayers = lobby.maxPlayers,
            ranked = lobby.ranked,
            rankedEligible = lobby.rankedEligible,
            aiDeck = if (lobby.vsAi) AiDeckSpecView.of(lobby.aiDeckSpec) else null,
        )
        sender.send(session, msg)
    }

    /** The signed-in account id behind a lobby seat, or null for a guest. */
    private fun userIdOf(playerId: com.wingedsheep.sdk.model.EntityId): java.util.UUID? =
        sessionRegistry.getAllIdentities().firstOrNull { it.playerId == playerId }?.userId

    private fun resolveDeck(
        player: QuickGameLobbyPlayer,
        randomFallbackSet: String,
        format: DeckFormat?,
        commanderRules: Boolean,
    ): GeneratedDeck {
        val submitted = player.deckList ?: emptyMap()
        if (submitted.isEmpty()) {
            // Player chose Random. Under a constructed format that means a legal 60-card deck built
            // from the format's pool — the same thing the AI seat's Auto gets — not a sealed pool
            // that ignores the restriction their opponent's list was validated against. Under a
            // commander shape it means a generated deck *and* its commander, so "Random" is a legal
            // Commander seat rather than one the engine refuses at init. Without a format it stays a
            // sealed pool from their own set choice, falling back to the caller's pre-resolved set
            // (shared with the AI in a vs-AI lobby so both play the same set).
            return randomDeckResolver.randomDeck(format, player.setCodes, randomFallbackSet, commanderRules)
        }
        return GeneratedDeck(submitted)
    }

    private fun broadcastState(lobby: QuickGameLobby) {
        for (player in lobby.players) {
            if (player.isAi) continue
            val ws = sessionRegistry
                .getAllIdentities()
                .firstOrNull { it.playerId == player.playerId }
                ?.webSocketSession ?: continue
            val msg = ServerMessage.QuickGameLobbyState(
                lobbyId = lobby.lobbyId,
                vsAi = lobby.vsAi,
                setCode = lobby.setCode,
                players = lobby.players.mapIndexed { i, p -> p.toView(lobby, i) },
                youPlayerId = player.playerId,
                canStart = lobby.allReady(),
                isPublic = lobby.isPublic,
                format = lobby.format,
                rules = lobby.rules.name,
                momirBasic = lobby.momirBasic,
                twoHeadedGiant = lobby.twoHeadedGiant,
                maxPlayers = lobby.maxPlayers,
                ranked = lobby.ranked,
                rankedEligible = lobby.rankedEligible,
                aiDeck = if (lobby.vsAi) AiDeckSpecView.of(lobby.aiDeckSpec) else null,
            )
            sender.send(ws, msg)
        }
    }

    /**
     * Subtract one copy of [commander] from [deckList]. Mirrors the web-client's
     * `stripCommanderFromCards` helper — the wire format ships the merged deck (commander
     * counted in `deckList`), but the server's `Deck.cards` convention excludes it. Idempotent
     * when the commander isn't present.
     */
    private fun stripCommanderFromCards(
        deckList: Map<String, Int>,
        commander: String,
    ): Map<String, Int> {
        val current = deckList[commander] ?: return deckList
        val next = deckList.toMutableMap()
        if (current <= 1) next.remove(commander) else next[commander] = current - 1
        return next
    }

    private fun broadcastClosed(lobby: QuickGameLobby, reason: String) {
        for (player in lobby.players) {
            if (player.isAi) continue
            val ws = sessionRegistry
                .getAllIdentities()
                .firstOrNull { it.playerId == player.playerId }
                ?.webSocketSession ?: continue
            sender.send(ws, ServerMessage.QuickGameLobbyClosed(reason))
        }
    }

    private fun QuickGameLobbyPlayer.toView(lobby: QuickGameLobby, seatIndex: Int): ServerMessage.QuickGameLobbyPlayerView {
        val total = deckList?.values?.sum() ?: 0
        // Momir Basic has no deckbuilding: every seat plays the fixed 60 basics, so it always counts
        // as "deck selected" and shows a fixed label rather than the deck-picker states.
        val label = when {
            lobby.momirBasic -> "Momir Basic (${MomirBasicSetup.COPIES_PER_BASIC * MomirBasicSetup.BASIC_LAND_NAMES.size} lands)"
            // The AI seat holds no deck list of its own — it plays whatever the host's
            // `aiDeckSpec` resolves to at game start — so it labels from the spec instead.
            isAi -> aiDeckLabel(lobby)
            deckList == null -> "Choosing…"
            deckList!!.isEmpty() -> if (setCodes.isNotEmpty()) "Random Pool (${setCodes.joinToString(", ")})" else "Random Pool"
            else -> "Custom ($total)"
        }
        return ServerMessage.QuickGameLobbyPlayerView(
            playerId = playerId,
            playerName = playerName,
            isAi = isAi,
            ready = ready,
            deckSelected = lobby.momirBasic || deckList != null,
            deckCardCount = if (lobby.momirBasic) MomirBasicSetup.COPIES_PER_BASIC * MomirBasicSetup.BASIC_LAND_NAMES.size else total,
            deckLabel = label,
            setCode = setCode,
            setCodes = setCodes,
            teamIndex = lobby.teamIndexOf(seatIndex),
        )
    }

    /** Player-list label for the AI seat, describing what the host chose for it. */
    private fun aiDeckLabel(lobby: QuickGameLobby): String = when (val spec = lobby.aiDeckSpec) {
        is AiDeckSpec.Auto -> autoAiDeckLabel(lobby)
        is AiDeckSpec.Sets ->
            if (spec.setCodes.isEmpty()) autoAiDeckLabel(lobby) else "Built from ${spec.setCodes.joinToString(", ")}"
        is AiDeckSpec.Fixed -> "${spec.label} (${spec.deckList.values.sum() + if (spec.commander != null) 1 else 0})"
    }

    /**
     * What "let the server decide" resolves to, named by the axis that decides it. A lobby can run
     * Commander with no deck-legality restriction at all, and "sealed" would be a lie there.
     */
    private fun autoAiDeckLabel(lobby: QuickGameLobby): String = when {
        lobby.format != null -> "Auto (${lobby.format!!.displayName})"
        lobby.usesCommanderRules -> "Auto (Commander)"
        else -> "Auto (sealed)"
    }

    private fun validateAiDeck(
        spec: AiDeckSpec.Fixed,
        format: com.wingedsheep.sdk.core.DeckFormat?,
    ) = if (format?.isCommanderShape == true) {
        deckValidator.validate(
            com.wingedsheep.sdk.model.Deck(
                cards = spec.deckList.flatMap { (name, count) -> List(count) { name } },
                commander = spec.commander?.takeIf { it.isNotBlank() },
            ),
            format,
        )
    } else {
        deckValidator.validate(spec.deckList, format)
    }
}
