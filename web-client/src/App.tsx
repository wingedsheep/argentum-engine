import { useEffect, useMemo, useRef } from 'react'
import { GameBoard } from './components/game/GameBoard'
import { GameUI } from './components/ui/GameUI'
import { MulliganUI } from './components/mulligan/MulliganUI'
import { DecisionUI } from './components/decisions/DecisionUI'
import { RevealedCardsUI } from './components/decisions/RevealedCardsUI'
import { XCostSelector } from './components/ui/XCostSelector'
import { ModalModeSelector } from './components/ui/ModalModeSelector'
import { BlightVariableSelector } from './components/ui/BlightVariableSelector'
import { PayXLifeSelector } from './components/ui/PayXLifeSelector'
import { ConvokeSelector } from './components/ui/ConvokeSelector'
import { TapForGenericSelector } from './components/ui/TapForGenericSelector'
import { HarmonizeSelector } from './components/ui/HarmonizeSelector'
import { TapForPowerSelector } from './components/ui/TapForPowerSelector'
import { DelveSelector } from './components/ui/DelveSelector'
import { DamageDistributionModal } from './components/decisions/DamageDistributionModal'
import { OpponentDecisionIndicator } from './components/ui/OpponentDecisionIndicator'
import { DisconnectCountdown } from './components/ui/DisconnectCountdown'
import { SessionReplacedOverlay } from './components/ui/SessionReplacedOverlay'
import { MatchIntroAnimation } from './components/animations/MatchIntroAnimation'
import { StandaloneConcedeButton } from './components/game/overlay'
import { DeckBuilderOverlay } from './components/sealed/DeckBuilderOverlay'
import { DraftPickOverlay } from './components/draft/DraftPickOverlay'
import { WinstonDraftOverlay } from './components/draft/WinstonDraftOverlay'
import { GridDraftOverlay } from './components/draft/GridDraftOverlay'
import { SpectatorGameBoard } from './components/spectating/SpectatorGameBoard'
import { trackPageView } from './utils/analytics'
import { randomBackground } from './utils/background'
import { useNavigate } from 'react-router-dom'
import { useGameStore } from './store/gameStore'
import { useConnectName } from './store/useConnectName'
import { useRematch } from '@/components/lobby/useRematch'
import { useViewingPlayer, useBattlefieldCards } from './store/selectors'
import type { ClientAttacker, EntityId } from './types'
import { GameOverReason } from './types'

export default function App() {
  const connectionStatus = useGameStore((state) => state.connectionStatus)
  const gameState = useGameStore((state) => state.gameState)
  const gameOverState = useGameStore((state) => state.gameOverState)
  const mulliganState = useGameStore((state) => state.mulliganState)
  const waitingForOpponentMulligan = useGameStore((state) => state.waitingForOpponentMulligan)
  const legalActions = useGameStore((state) => state.legalActions)
  const combatState = useGameStore((state) => state.combatState)
  const deckBuildingState = useGameStore((state) => state.deckBuildingState)
  const lobbyState = useGameStore((state) => state.lobbyState)
  const tournamentState = useGameStore((state) => state.tournamentState)
  const ffaState = useGameStore((state) => state.ffaState)
  const spectatingState = useGameStore((state) => state.spectatingState)
  const matchIntro = useGameStore((state) => state.matchIntro)
  const startCombat = useGameStore((state) => state.startCombat)
  const connect = useGameStore((state) => state.connect)
  const spectateGame = useGameStore((state) => state.spectateGame)
  const sessionReplaced = useGameStore((state) => state.sessionReplaced)
  const { name: connectName } = useConnectName()
  const hasConnectedRef = useRef(false)

  // Dev deep-link: /?spectate=<gameSessionId> connects and auto-spectates that game,
  // used by the LLM-tournament page's "Watch" links to watch a live AI-vs-AI match.
  const spectateParam = useMemo(
    () => new URLSearchParams(window.location.search).get('spectate'),
    []
  )
  const hasSpectatedRef = useRef(false)

  const viewingPlayer = useViewingPlayer()
  const battlefieldCards = useBattlefieldCards()

  // Check for combat actions in legal actions
  const hasDeclareAttackersAction = legalActions.some(
    (a) => a.actionType === 'DeclareAttackers' || a.action.type === 'DeclareAttackers'
  )
  const hasDeclareBlockersAction = legalActions.some(
    (a) => a.actionType === 'DeclareBlockers' || a.action.type === 'DeclareBlockers'
  )

  useEffect(() => {
    if (connectionStatus !== 'disconnected' || hasConnectedRef.current) return
    // Another tab/device owns the session; reconnecting here would steal it back.
    // The SessionReplacedOverlay's "Use here" is the only way back in.
    if (sessionReplaced) return
    if (spectateParam) {
      // Spectate deep-link: connect as an isolated, ephemeral spectator (no shared token/name) so
      // it doesn't collide with the user's identity and each watch gets a fresh session.
      hasConnectedRef.current = true
      connect(`Spectator-${Math.floor(Math.random() * 9000 + 1000)}`, { spectator: true })
      return
    }
    // Otherwise only auto-connect for someone who already has a name — a stored one, or the display
    // name of a signed-in account. Genuinely new users see name entry. `connectName` starts null
    // while the account check is in flight, so this re-runs once it resolves.
    if (connectName) {
      hasConnectedRef.current = true
      connect(connectName)
    }
  }, [connectionStatus, connect, connectName, spectateParam, sessionReplaced])

  // Once connected, honor a /?spectate=<gameId> deep-link (fire once).
  useEffect(() => {
    if (spectateParam && connectionStatus === 'connected' && !hasSpectatedRef.current && !spectatingState) {
      hasSpectatedRef.current = true
      spectateGame(spectateParam)
    }
  }, [spectateParam, connectionStatus, spectatingState, spectateGame])

  /**
   * Keep the URL bar in sync with tournament state so the link is shareable.
   *
   * **Deliberately raw `history.replaceState`, not `navigate()`.** `/tournament/:lobbyId` is a real
   * route that renders `TournamentEntryPage`, so routing there would unmount this component and drop
   * the WebSocket. This writes the address bar without telling the router — which is why React
   * Router's location is stale relative to the bar during lobby play. Giving the in-`/` screens their
   * own routes is the remaining half of Phase 6.
   *
   * The reset arm is scoped to lobby paths. It used to send *any* non-`/` path back to `/`, which
   * erased the wizard's `/play/...` steps (and would erase any future in-`/` route) on every lobby
   * state change.
   */
  useEffect(() => {
    const lobbyId = tournamentState?.lobbyId ?? lobbyState?.lobbyId
    if (lobbyId) {
      const target = `/tournament/${lobbyId}`
      if (window.location.pathname !== target) {
        window.history.replaceState(null, '', target)
      }
    } else if (window.location.pathname.startsWith('/tournament/')) {
      window.history.replaceState(null, '', '/')
    }
  }, [tournamentState?.lobbyId, lobbyState?.lobbyId])

  // Get the clearCombat action
  const clearCombat = useGameStore((state) => state.clearCombat)

  // Auto-enter/exit combat mode based on available actions
  useEffect(() => {
    if (!gameState || !viewingPlayer) return

    // Exit combat mode if the relevant action is no longer available
    if (combatState) {
      if (combatState.mode === 'declareAttackers' && !hasDeclareAttackersAction) {
        // DeclareAttackers was submitted and processed - exit combat mode
        clearCombat()
        return
      }
      if (combatState.mode === 'declareBlockers' && !hasDeclareBlockersAction) {
        // DeclareBlockers was submitted and processed - exit combat mode
        clearCombat()
        return
      }
      // Update valid creatures if server sent updated list (e.g., after ability resolution during declare blockers)
      if (combatState.mode === 'declareBlockers' && hasDeclareBlockersAction) {
        const blockersAction = legalActions.find(
          (a) => a.actionType === 'DeclareBlockers' || a.action.type === 'DeclareBlockers'
        )
        const updatedValidCreatures = blockersAction?.validBlockers ?? []
        if (JSON.stringify(updatedValidCreatures) !== JSON.stringify([...combatState.validCreatures])) {
          // Clean stale blocker assignments for creatures no longer in valid list
          const validSet = new Set(updatedValidCreatures)
          const cleanedAssignments: Record<EntityId, EntityId[]> = {}
          for (const [blockerId, attackerIds] of Object.entries(combatState.blockerAssignments)) {
            if (validSet.has(blockerId as EntityId)) {
              cleanedAssignments[blockerId as EntityId] = attackerIds
            }
          }
          startCombat({
            ...combatState,
            validCreatures: [...updatedValidCreatures],
            blockerAssignments: cleanedAssignments,
          })
        }
      }
      // Already in combat mode, don't re-enter
      return
    }

    // Enter combat mode when action becomes available
    if (hasDeclareAttackersAction) {
      // Find the DeclareAttackers action to get valid attackers from server
      const attackersAction = legalActions.find(
        (a) => a.actionType === 'DeclareAttackers' || a.action.type === 'DeclareAttackers'
      )
      // Use server-provided valid attackers (handles haste, defender, etc.)
      const validCreatures: EntityId[] = attackersAction?.validAttackers
        ? [...attackersAction.validAttackers]
        : []

      // Get planeswalker attack targets from server
      const validAttackTargets: EntityId[] = attackersAction?.validAttackTargets
        ? [...attackersAction.validAttackTargets]
        : []

      // Get mandatory attackers (creatures that must attack, e.g., Valley Dasher)
      const mandatoryAttackers: EntityId[] = attackersAction?.mandatoryAttackers
        ? [...attackersAction.mandatoryAttackers]
        : []

      // Multiplayer with exactly one legal attack target that is a player (attack
      // left/right, last opponent standing — and no attackable planeswalkers):
      // pre-assign it as the sticky defender so every selected attacker gets it
      // and the "Attack which player?" popup never needs to ask.
      const playerIdSet = new Set(gameState?.players.map((p) => p.playerId) ?? [])
      const soleDefenderId =
        (gameState?.players.length ?? 0) > 2 &&
        validAttackTargets.length === 1 &&
        playerIdSet.has(validAttackTargets[0]!)
          ? validAttackTargets[0]!
          : null

      // Enter combat mode — pre-select mandatory attackers
      startCombat({
        mode: 'declareAttackers',
        actingSeat: attackersAction?.action.type === 'DeclareAttackers' ? attackersAction.action.playerId : null,
        stickyDefenderId: soleDefenderId,
        selectedAttackers: [...mandatoryAttackers],
        // Mandatory attackers are pre-selected, so give them the sole defender too —
        // otherwise the defender popup would still ask about them.
        attackerTargets: soleDefenderId
          ? Object.fromEntries(mandatoryAttackers.map((id) => [id, soleDefenderId]))
          : {},
        validAttackTargets,
        blockerAssignments: {},
        validCreatures,
        mandatoryAttackers,
        attackingCreatures: [],
        mustBeBlockedAttackers: [],
        blockerMaxBlockCounts: {},
        bands: [],
      })
      return
    }

    if (hasDeclareBlockersAction) {
      // Find the DeclareBlockers action to get valid blockers from server
      const blockersAction = legalActions.find(
        (a) => a.actionType === 'DeclareBlockers' || a.action.type === 'DeclareBlockers'
      )
      // Use server-provided valid blockers
      const validCreatures: EntityId[] = blockersAction?.validBlockers
        ? [...blockersAction.validBlockers]
        : []

      // Attacking creatures come from the server's authoritative combat state rather than
      // "the viewing player's opponent" — in single-client hotseat the seat we control (the
      // defender) can be the top row, so the attackers are the viewer's own creatures.
      // In multiplayer a defender may only block attackers attacking *them* or their
      // planeswalkers (CR 509.1b) — scope to the acting defender's slice of the combat.
      // In a 2-player game every attacker attacks the sole defender, so this keeps all.
      const defendingSeat = blockersAction?.action.type === 'DeclareBlockers'
        ? blockersAction.action.playerId
        : null
      const attacksSeat = (a: ClientAttacker): boolean => {
        if (!defendingSeat) return true
        if (a.attackingTarget.type === 'Player') return a.attackingTarget.playerId === defendingSeat
        return gameState?.cards[a.attackingTarget.permanentId]?.controllerId === defendingSeat
      }
      const relevantAttackers = (gameState?.combat?.attackers ?? []).filter(attacksSeat)
      const attackingCreatures: EntityId[] = relevantAttackers.map((a) => a.creatureId)

      // Find attackers that must be blocked by all (from combat state in game state)
      const mustBeBlockedAttackers: EntityId[] = relevantAttackers
        .filter((a) => a.mustBeBlockedByAll)
        .map((a) => a.creatureId)

      // Use server-provided mandatory blocker assignments (Provoke + MustBeBlockedByAll)
      const blockerAssignments: Record<EntityId, EntityId[]> = {}
      if (blockersAction?.mandatoryBlockerAssignments) {
        for (const [blockerId, attackerIds] of Object.entries(blockersAction.mandatoryBlockerAssignments)) {
          blockerAssignments[blockerId as EntityId] = [...attackerIds]
        }
      }

      // Extract blockerMaxBlockCounts from the legal action
      const blockerMaxBlockCounts: Record<EntityId, number> = blockersAction?.blockerMaxBlockCounts
        ? { ...blockersAction.blockerMaxBlockCounts }
        : {}

      // Enter combat mode
      startCombat({
        mode: 'declareBlockers',
        actingSeat: blockersAction?.action.type === 'DeclareBlockers' ? blockersAction.action.playerId : null,
        stickyDefenderId: null,
        selectedAttackers: [],
        attackerTargets: {},
        validAttackTargets: [],
        blockerAssignments,
        validCreatures,
        mandatoryAttackers: [],
        attackingCreatures,
        mustBeBlockedAttackers,
        blockerMaxBlockCounts,
        bands: [],
      })
    }
  }, [hasDeclareAttackersAction, hasDeclareBlockersAction, gameState, viewingPlayer, combatState, startCombat, clearCombat, battlefieldCards, legalActions])

  // Show connection/game creation UI when not in a game
  const showLobby = connectionStatus !== 'connected' || !gameState
  const showGame = !showLobby && !mulliganState
  // Show deck builder during building phase, or during submitted phase if no tournament yet
  // When tournament exists and deck is submitted, TournamentOverlay (in GameUI) handles UI
  const showDeckBuilder = deckBuildingState?.phase === 'building' ||
    (deckBuildingState?.phase === 'submitted' && !tournamentState && !ffaState)
  const showDraftPick = lobbyState?.state === 'DRAFTING' && lobbyState?.settings.format === 'DRAFT'
  const showWinstonDraft = lobbyState?.state === 'DRAFTING' && lobbyState?.settings.format === 'WINSTON_DRAFT'
  const showGridDraft = lobbyState?.state === 'DRAFTING' && lobbyState?.settings.format === 'GRID_DRAFT'

  // Track virtual page views for GA4 when the active screen changes
  const currentScreen = useMemo(() => {
    if (spectatingState) return 'spectate'
    if (showDraftPick || showWinstonDraft || showGridDraft) return 'draft'
    if (showDeckBuilder) return 'deck-builder'
    if (mulliganState) return 'mulligan'
    if (showGame) return 'game'
    return 'lobby'
  }, [spectatingState, showDraftPick, showWinstonDraft, showGridDraft, showDeckBuilder, mulliganState, showGame])

  const prevScreenRef = useRef(currentScreen)
  useEffect(() => {
    if (currentScreen !== prevScreenRef.current) {
      prevScreenRef.current = currentScreen
      trackPageView(`/${currentScreen}`, currentScreen)
    }
  }, [currentScreen])

  return (
    <div style={{ width: '100%', height: '100%', position: 'relative' }}>
      {/* Main game board (2D) */}
      {showGame && <GameBoard />}

      {/* Opponent decision indicator (shown during game when opponent is deciding) */}
      {showGame && <OpponentDecisionIndicator />}

      {/* Disconnect countdown (shown when opponent disconnects during game or mulligan) */}
      {(showGame || mulliganState || waitingForOpponentMulligan) && <DisconnectCountdown />}

      {/* Connection/lobby UI overlay (suppressed during mulligan and game-over) */}
      {showLobby && !gameOverState && !mulliganState && !waitingForOpponentMulligan && <GameUI />}

      {/* Background image behind mulligan/intro overlay */}
      {(mulliganState || waitingForOpponentMulligan || matchIntro) && (
        <div style={{
          position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
          backgroundImage: `url(${randomBackground})`,
          backgroundSize: 'cover', backgroundPosition: 'center',
        }} />
      )}

      {/* Deck building overlay (sealed/draft) */}
      {showDeckBuilder && <DeckBuilderOverlay />}

      {/* Draft picking overlay */}
      {showDraftPick && <DraftPickOverlay />}

      {/* Winston Draft overlay */}
      {showWinstonDraft && <WinstonDraftOverlay />}

      {/* Grid Draft overlay */}
      {showGridDraft && <GridDraftOverlay />}

      {/* Match intro animation (plays before mulligan) */}
      {matchIntro && <MatchIntroAnimation />}

      {/* Mulligan overlay */}
      {mulliganState && !matchIntro && <MulliganUI />}

      {/* Waiting for opponent mulligan overlay */}
      {!mulliganState && !matchIntro && waitingForOpponentMulligan && <WaitingForMulliganOverlay />}


      {/* X cost selection overlay (when casting spells with X in cost) */}
      {showGame && <XCostSelector />}

      {/* Blight X variable additional cost overlay (e.g., Soul Immolation) */}
      {showGame && <BlightVariableSelector />}
      {showGame && <PayXLifeSelector />}

      {/* Choose-N modal (Spree / "choose one or more") mode-selection panel */}
      {showGame && <ModalModeSelector />}

      {/* Convoke selection overlay (when casting spells with Convoke) */}
      {showGame && <ConvokeSelector />}

      {/* Tap-for-generic selection overlay (improvise CR 702.126 / waterbend costs) */}
      {showGame && <TapForGenericSelector />}

      {/* Harmonize creature-tap overlay (when casting from graveyard via Harmonize) */}
      {showGame && <HarmonizeSelector />}

      {/* Tap-for-power selection overlay (crewing Vehicles / saddling Mounts) */}
      {showGame && <TapForPowerSelector />}

      {/* Delve selection overlay (when casting spells with Delve) */}
      {showGame && <DelveSelector />}

      {/* Damage distribution overlay (for DividedDamageEffect spells like Forked Lightning) */}
      {showGame && <DamageDistributionModal />}

      {/* Decision overlay (for pending decisions like discard to hand size) */}
      {showGame && <DecisionUI />}

      {/* Revealed cards overlay (hand reveals and library reveals) */}
      {showGame && <RevealedCardsUI />}

      {/* Game over overlay (rendered independently so it persists after game state clears) */}
      <GameOverlay />

      {/* Spectator view (when watching another game — skip when ReplayViewer handles its own UI) */}
      {spectatingState && !spectatingState.isReplay && <SpectatorGameBoard />}

      {/* Session takeover overlay (this tab's identity connected from another tab/device) */}
      <SessionReplacedOverlay />
    </div>
  )
}

function formatGameOverReason(reason: GameOverReason, result: 'win' | 'lose' | 'draw'): string {
  if (result === 'draw') {
    return 'Both players lost simultaneously.'
  }
  const winMessages: Partial<Record<GameOverReason, string>> = {
    [GameOverReason.LIFE_ZERO]: "Your opponent's life total reached zero.",
    [GameOverReason.DECK_OUT]: 'Your opponent had no cards left to draw.',
    [GameOverReason.CONCESSION]: 'Your opponent conceded the game.',
    [GameOverReason.POISON_COUNTERS]: 'Your opponent received ten poison counters.',
    [GameOverReason.DISCONNECTION]: 'Your opponent disconnected.',
  }
  const loseMessages: Partial<Record<GameOverReason, string>> = {
    [GameOverReason.LIFE_ZERO]: 'Your life total reached zero.',
    [GameOverReason.DECK_OUT]: 'You had no cards left to draw.',
    [GameOverReason.CONCESSION]: 'You conceded the game.',
    [GameOverReason.POISON_COUNTERS]: 'You received ten poison counters.',
    [GameOverReason.DISCONNECTION]: 'You disconnected from the game.',
  }
  const messages = result === 'win' ? winMessages : loseMessages
  return messages[reason] ?? 'The game has ended.'
}

/**
 * Game over and error overlays.
 */
function GameOverlay() {
  const gameOverState = useGameStore((state) => state.gameOverState)
  const lastError = useGameStore((state) => state.lastError)
  const clearError = useGameStore((state) => state.clearError)
  const returnToMenu = useGameStore((state) => state.returnToMenu)
  const enterEliminatedSpectate = useGameStore((state) => state.enterEliminatedSpectate)
  const navigate = useNavigate()
  const rematch = useRematch()

  // Auto-dismiss is handled centrally in the store (setError schedules clearError), so it
  // works on every route — not just where this overlay happens to be mounted. The × button
  // below dismisses early.

  if (gameOverState) {
    // Use custom message if provided, otherwise fall back to standard reason
    const reasonText = gameOverState.message || formatGameOverReason(gameOverState.reason, gameOverState.result)
    const titleColor =
      gameOverState.result === 'win' ? '#00ff00' : gameOverState.result === 'draw' ? '#ffcc00' : '#ff0000'
    const title =
      gameOverState.result === 'win' ? 'Victory!' : gameOverState.result === 'draw' ? 'Draw' : 'Defeat'
    return (
      <>
        {/* Transparent layer to block board interaction */}
        <div style={overlayStyles.clickBlocker} />
        <div style={overlayStyles.container}>
          <h1 style={{
            ...overlayStyles.title,
            color: titleColor,
          }}>
            {title}
          </h1>
          <p style={overlayStyles.subtitle}>{reasonText}</p>
          <div style={overlayStyles.buttonRow}>
            {/* Eliminated from a multiplayer game that plays on: offer to stay and
                spectate the rest of it (board overview, action UI hidden). */}
            {gameOverState.eliminated && (
              <button
                onClick={enterEliminatedSpectate}
                style={overlayStyles.replayButton}
              >
                Keep Watching
              </button>
            )}
            {/* The quick lobby is destroyed when the game starts
                (`QuickGameLobbyHandler.startGame` removes it), but the recipe that built it is not —
                so a rematch is that recipe replayed, and for a vs-AI game every input is decidable
                locally: no new protocol, no lobby to keep alive. A human 1v1 rematch needs the
                server to re-seat both players and is deliberately not faked here.

                A rematch and a saved setup are the same object with different seats: a rematch is
                a recipe replayed with the seats intact, a setup is one replayed with them open. */}
            {rematch && (
              <button
                onClick={rematch.play}
                style={overlayStyles.button}
                data-testid="game-over-play-again"
              >
                Play Again
              </button>
            )}
            <button
              onClick={returnToMenu}
              style={overlayStyles.button}
            >
              Return to Menu
            </button>
            {gameOverState.gameId && (
              <button
                onClick={() => {
                  returnToMenu()
                  navigate(`/replay/${gameOverState.gameId}`)
                }}
                style={overlayStyles.replayButton}
              >
                Watch Replay
              </button>
            )}
          </div>
        </div>
      </>
    )
  }

  if (lastError) {
    return (
      <div style={overlayStyles.errorToast}>
        <span>{lastError.message}</span>
        <button onClick={clearError} style={overlayStyles.closeButton}>
          ×
        </button>
      </div>
    )
  }

  return null
}

const overlayStyles: Record<string, React.CSSProperties> = {
  clickBlocker: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    zIndex: 1999,
  },
  container: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    backgroundColor: 'rgba(0, 0, 0, 0.85)',
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 24,
    zIndex: 2000,
  },
  title: {
    margin: 0,
    fontSize: 48,
  },
  subtitle: {
    fontSize: 18,
    color: '#888',
  },
  buttonRow: {
    display: 'flex',
    gap: 12,
  },
  button: {
    padding: '12px 24px',
    fontSize: 18,
    backgroundColor: '#333',
    color: 'white',
    border: 'none',
    borderRadius: 8,
    cursor: 'pointer',
  },
  replayButton: {
    padding: '12px 24px',
    fontSize: 18,
    backgroundColor: '#1e40af',
    color: 'white',
    border: 'none',
    borderRadius: 8,
    cursor: 'pointer',
  },
  errorToast: {
    position: 'absolute',
    top: 16,
    left: '50%',
    transform: 'translateX(-50%)',
    backgroundColor: '#cc0000',
    color: 'white',
    padding: '12px 24px',
    borderRadius: 8,
    display: 'flex',
    alignItems: 'center',
    gap: 12,
    zIndex: 2000,
  },
  closeButton: {
    background: 'none',
    border: 'none',
    color: 'white',
    fontSize: 18,
    cursor: 'pointer',
  },
}

function WaitingForMulliganOverlay() {
  return (
    <div
      style={{
        position: 'fixed',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        backgroundColor: 'rgba(0, 0, 0, 0.85)',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        gap: 16,
        zIndex: 1000,
      }}
    >
      <StandaloneConcedeButton />
      <div
        style={{
          width: 40,
          height: 40,
          border: '3px solid #333',
          borderTopColor: '#888',
          borderRadius: '50%',
          animation: 'spin 1s linear infinite',
        }}
      />
      <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
      <h2 style={{ color: 'white', margin: 0, fontSize: 22 }}>
        Waiting for opponent...
      </h2>
      <p style={{ color: '#666', margin: 0, fontSize: 14 }}>
        Your opponent is choosing their opening hand
      </p>
    </div>
  )
}
