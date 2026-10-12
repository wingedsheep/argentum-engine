import { Suspense, lazy, memo, useEffect, useMemo, useRef } from 'react'
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
import overlay from './components/game/overlay/GameOverlays.module.css'
import { trackPageView } from './utils/analytics'
import { randomBackground } from './utils/background'
import { useNavigate } from 'react-router-dom'
import { useGameStore } from './store/gameStore'
import { useConnectName } from './store/useConnectName'
import { useRematch } from '@/components/lobby/useRematch'
import { PostGamePanel } from '@/components/tableTalk/PostGamePanel'
import { useTableTalkStore } from '@/store/tableTalkStore'
import { LearnCoach } from '@/components/learn/LearnCoach'
import { useViewingPlayer, useBattlefieldCards, selectTeamMap } from './store/selectors'
import type { ClientAttacker, EntityId } from './types'
import { GameOverReason } from './types'
import { defendingPlayerOf } from './utils/combatTargets'

// Limited-format and spectator screens are a minority of sessions but about a third of this
// chunk, so they load on demand. Each keeps its own <Suspense> — the one in main.tsx would blank
// the whole app while a chunk streams in. `preloadSecondaryScreens` warms them once the browser
// is idle, so opening a draft still feels instant.
const importDeckBuilderOverlay = () => import('./components/sealed/DeckBuilderOverlay')
const JumpstartOverlay = lazy(() => import('./components/draft/JumpstartOverlay').then((m) => ({ default: m.JumpstartOverlay })))
const importDraftPickOverlay = () => import('./components/draft/DraftPickOverlay')
const importWinstonDraftOverlay = () => import('./components/draft/WinstonDraftOverlay')
const importGridDraftOverlay = () => import('./components/draft/GridDraftOverlay')
const importSpectatorGameBoard = () => import('./components/spectating/SpectatorGameBoard')
const DeckBuilderOverlay = lazy(() => importDeckBuilderOverlay().then((m) => ({ default: m.DeckBuilderOverlay })))
const DraftPickOverlay = lazy(() => importDraftPickOverlay().then((m) => ({ default: m.DraftPickOverlay })))
const WinstonDraftOverlay = lazy(() => importWinstonDraftOverlay().then((m) => ({ default: m.WinstonDraftOverlay })))
const GridDraftOverlay = lazy(() => importGridDraftOverlay().then((m) => ({ default: m.GridDraftOverlay })))
const SpectatorGameBoard = lazy(() => importSpectatorGameBoard().then((m) => ({ default: m.SpectatorGameBoard })))

function preloadSecondaryScreens() {
  const load = () => {
    void importDeckBuilderOverlay()
    void importDraftPickOverlay()
    void importWinstonDraftOverlay()
    void importGridDraftOverlay()
    void importSpectatorGameBoard()
  }
  if (typeof window.requestIdleCallback === 'function') window.requestIdleCallback(load, { timeout: 5000 })
  else setTimeout(load, 2000)
}

/**
 * The in-game interaction overlays. None takes props beyond the epoch key — each reads its own
 * store slice — so memoizing the group keeps App's own re-render on every server message (it
 * watches gameState and legalActions to drive combat mode) from re-rendering all of them.
 */
const InGameOverlays = memo(function InGameOverlays({ interactionEpoch }: { interactionEpoch: string | null }) {
  return (
    <>
      {/* X cost selection overlay (when casting spells with X in cost) */}
      <XCostSelector key={interactionEpoch} />

      {/* Blight X variable additional cost overlay (e.g., Soul Immolation) */}
      <BlightVariableSelector key={interactionEpoch} />
      <PayXLifeSelector key={interactionEpoch} />

      {/* Choose-N modal (Spree / "choose one or more") mode-selection panel */}
      <ModalModeSelector key={interactionEpoch} />

      {/* Convoke selection overlay (when casting spells with Convoke) */}
      <ConvokeSelector key={interactionEpoch} />

      {/* Tap-for-generic selection overlay (improvise CR 702.126 / waterbend costs) */}
      <TapForGenericSelector key={interactionEpoch} />

      {/* Harmonize creature-tap overlay (when casting from graveyard via Harmonize) */}
      <HarmonizeSelector key={interactionEpoch} />

      {/* Tap-for-power selection overlay (crewing Vehicles / saddling Mounts) */}
      <TapForPowerSelector key={interactionEpoch} />

      {/* Delve selection overlay (when casting spells with Delve) */}
      <DelveSelector key={interactionEpoch} />

      {/* Damage distribution overlay (for DividedDamageEffect spells like Forked Lightning) */}
      <DamageDistributionModal key={interactionEpoch} />

      {/* Decision overlay (for pending decisions like discard to hand size) */}
      <DecisionUI />

      {/* Revealed cards overlay (hand reveals and library reveals) */}
      <RevealedCardsUI />
    </>
  )
})

export default function App() {
  const interactionEpoch = useGameStore((state) => state.interactionEpoch)
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

  useEffect(preloadSecondaryScreens, [])

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
        interactionEpoch: attackersAction?.interactionEpoch ?? null,
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
        // Per-attacker narrowing (Goblin Rock Sled, Blazing Archon) — the server's word on which
        // defender each attacker may pick; the combat slice only reads it.
        attackTargetsByAttacker: attackersAction?.attackTargetsByAttacker ?? {},
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
        return defendingPlayerOf(a.attackingTarget.permanentId, gameState?.cards) === defendingSeat
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
        interactionEpoch: blockersAction?.interactionEpoch ?? null,
        mode: 'declareBlockers',
        actingSeat: blockersAction?.action.type === 'DeclareBlockers' ? blockersAction.action.playerId : null,
        stickyDefenderId: null,
        selectedAttackers: [],
        attackerTargets: {},
        validAttackTargets: [],
        attackTargetsByAttacker: {},
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
  const showJumpstart = lobbyState?.settings.jumpstartActive && lobbyState.state === 'DECK_BUILDING' && !tournamentState && !ffaState
  const showDeckBuilder = !lobbyState?.settings.jumpstartActive && (deckBuildingState?.phase === 'building' ||
    (deckBuildingState?.phase === 'submitted' && !tournamentState && !ffaState))
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

      {/* Learn-to-play coach — renders nothing unless the course's first game armed it. Mounted
          through game over too, so it can close with the result. */}
      {(showGame || gameOverState) && <LearnCoach />}

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
      {showDeckBuilder && <Suspense fallback={null}><DeckBuilderOverlay /></Suspense>}

      {/* Draft picking overlay */}
      {showJumpstart && <Suspense fallback={null}><JumpstartOverlay /></Suspense>}
      {showDraftPick && <Suspense fallback={null}><DraftPickOverlay /></Suspense>}

      {/* Winston Draft overlay */}
      {showWinstonDraft && <Suspense fallback={null}><WinstonDraftOverlay /></Suspense>}

      {/* Grid Draft overlay */}
      {showGridDraft && <Suspense fallback={null}><GridDraftOverlay /></Suspense>}

      {/* Match intro animation (plays before mulligan) */}
      {matchIntro && <MatchIntroAnimation />}

      {/* Mulligan overlay */}
      {mulliganState && !matchIntro && <MulliganUI />}

      {/* Waiting for opponent mulligan overlay */}
      {!mulliganState && !matchIntro && waitingForOpponentMulligan && <WaitingForMulliganOverlay />}


      {showGame && <InGameOverlays interactionEpoch={interactionEpoch} />}

      {/* Game over overlay (rendered independently so it persists after game state clears) */}
      <GameOverlay />

      {/* Spectator view (when watching another game — skip when ReplayViewer handles its own UI) */}
      {spectatingState && !spectatingState.isReplay && <Suspense fallback={null}><SpectatorGameBoard /></Suspense>}

      {/* Session takeover overlay (this tab's identity connected from another tab/device) */}
      <SessionReplacedOverlay />
    </div>
  )
}

function formatGameOverReason(
  reason: GameOverReason,
  result: 'win' | 'lose' | 'draw',
  table: 'duel' | 'pod' | 'teams' = 'duel',
): string {
  if (result === 'draw') {
    return table === 'duel' ? 'Both players lost simultaneously.' : 'The remaining players lost simultaneously.'
  }
  // A pod is won by outlasting several opponents, so "your opponent" (singular, and naming only
  // how the last one fell) misdescribes it.
  if (result === 'win' && table === 'pod') return 'You are the last player standing.'
  if (result === 'win' && table === 'teams') return 'Your team is the last one standing.'
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
  // The board state is still in the store behind the overlay; with it gone, fall back to duel copy.
  const table = useGameStore((state): 'duel' | 'pod' | 'teams' => {
    if ((state.gameState?.players.length ?? 0) <= 2) return 'duel'
    return Object.keys(selectTeamMap(state)).length > 0 ? 'teams' : 'pod'
  })
  const navigate = useNavigate()
  const rematch = useRematch()
  // A human opponent's result screen: rematch / add friend / block, all from the server.
  const postGame = useTableTalkStore((state) =>
    state.postGame && state.postGame.gameId === gameOverState?.gameId ? state.postGame : null,
  )

  // Auto-dismiss is handled centrally in the store (setError schedules clearError), so it
  // works on every route — not just where this overlay happens to be mounted. The × button
  // below dismisses early.

  if (gameOverState) {
    // Use custom message if provided, otherwise fall back to standard reason
    const reasonText = gameOverState.message || formatGameOverReason(gameOverState.reason, gameOverState.result, table)
    const title =
      gameOverState.result === 'win' ? 'Victory!' : gameOverState.result === 'draw' ? 'Draw' : 'Defeat'
    const showReplay = gameOverState.gameId != null
    const watchReplay = () => {
      returnToMenu()
      navigate(`/replay/${gameOverState.gameId}`)
    }
    // One amber action: play again when there is a rematch to offer, otherwise back to the menu.
    // Everything else sits in the quieter row beneath it.
    return (
      <div className={overlay.scrim} role="dialog" aria-modal="true" aria-labelledby="game-over-title">
        <div className={overlay.card} data-result={gameOverState.result}>
          <p className={overlay.eyebrow}>Game over</p>
          <h1 id="game-over-title" className={overlay.title}>{title}</h1>
          <p className={overlay.reason}>{reasonText}</p>
          {postGame && <PostGamePanel postGame={postGame} />}
          <div className={overlay.actions}>
            {/* The quick lobby is destroyed when the game starts
                (`QuickGameLobbyHandler.startGame` removes it), but the recipe that built it is not —
                so a rematch is that recipe replayed, and for a vs-AI game every input is decidable
                locally: no new protocol, no lobby to keep alive. A human 1v1 rematch needs the
                server to ask both players and re-seat them — that is `PostGamePanel`, driven by the
                server's `PostGameService`, which replays the finished game's own recipe.

                A rematch and a saved setup are the same object with different seats: a rematch is
                a recipe replayed with the seats intact, a setup is one replayed with them open. */}
            {/* Against a person, the panel above carries the one primary action (Rematch). */}
            {postGame ? null : rematch ? (
              <button type="button" onClick={rematch.play} className={overlay.primary} data-testid="game-over-play-again">
                Play Again
              </button>
            ) : (
              <button type="button" onClick={returnToMenu} className={overlay.primary}>
                Return to Menu
              </button>
            )}
            <div className={overlay.secondaryRow}>
              {/* Eliminated from a multiplayer game that plays on: offer to stay and
                  spectate the rest of it (board overview, action UI hidden). */}
              {gameOverState.eliminated && (
                <button type="button" onClick={enterEliminatedSpectate} className={overlay.secondary}>
                  Keep Watching
                </button>
              )}
              {(rematch || postGame) && (
                <button type="button" onClick={returnToMenu} className={overlay.secondary}>
                  Return to Menu
                </button>
              )}
              {showReplay && (
                <button type="button" onClick={watchReplay} className={overlay.secondary}>
                  Watch Replay
                </button>
              )}
            </div>
          </div>
        </div>
      </div>
    )
  }

  if (lastError) {
    return (
      <div className={overlay.toast} role="alert">
        <span>{lastError.message}</span>
        <button type="button" onClick={clearError} className={overlay.toastClose} aria-label="Dismiss">
          ×
        </button>
      </div>
    )
  }

  return null
}

function WaitingForMulliganOverlay() {
  return (
    <div className={`${overlay.scrim} ${overlay.scrimFixed}`}>
      <StandaloneConcedeButton />
      <div className={overlay.card}>
        <div className={overlay.spinner} aria-hidden />
        <h2 className={overlay.waitTitle}>Waiting for your opponent</h2>
        <p className={overlay.waitText}>They're choosing their opening hand.</p>
      </div>
    </div>
  )
}
