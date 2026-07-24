import { useMemo, useCallback, useRef, useEffect } from 'react'
import { useGameStore } from '@/store/gameStore'
import { useInteraction } from '@/hooks/useInteraction'
import { useViewingPlayer, useOpponent, useOpponents, useViewedOpponent, useStackCards, selectPriorityMode, useGhostCards, useBattlefieldCards, selectTeamMap, useIdentityColor, useViewerTeamIndex, useIsAlly, identitySeatColor, selectViewingPlayerId } from '@/store/selectors'
import { useMultiplayerView, useCombatDefenderFocus } from '@/hooks/useMultiplayerView'
import { OpponentRail, railReservedWidth } from './OpponentRail'
import { hand, getNextStep, StepShortNames } from '@/types'
import type { EntityId } from '@/types'
import { StepStrip } from '../ui/StepStrip'
import { ManaPool } from '../ui/ManaPool'
import { ActionMenu } from '../ui/ActionMenu'
import { CombatArrows } from '../combat/CombatArrows'
import { TargetingArrows } from '../targeting/TargetingArrows'
import { DraggedCardOverlay } from './DraggedCardOverlay'
import { GameLog } from './GameLog'
import { ActiveYieldsPanel } from './ActiveYieldsPanel'
import { DrawAnimations } from '../animations/DrawAnimations'
import { DamageAnimations } from '../animations/DamageAnimations'
import { RevealAnimations } from '../animations/RevealAnimations'
import { CoinFlipAnimations } from '../animations/CoinFlipAnimations'
import { TargetReselectedAnimations } from '../animations/TargetReselectedAnimations'
import { useResponsive } from '@/hooks/useResponsive'
import { ManaSymbol } from '../ui/ManaSymbols'

// Import extracted components
import { Battlefield, CardRow, CommandZone, OpponentBoardArea, CollapsedBoardTab, COLLAPSED_TAB_WIDTH, StackDisplay, ZonePile, ResponsiveContext } from './board'
import { RenderProfiler } from '@/utils/renderProfiler'
import { CardPreview } from './card'
import { TargetingOverlay, ManaColorSelectionOverlay, LifeDisplay, ActiveEffectsBadges, ConcedeButton, FullscreenButton, SpectatorCountBadge } from './overlay'
import { styles } from './board/styles'

/**
 * Props for GameBoard component.
 */
interface GameBoardProps {
  /** When true, disables all interactions (for spectator view) */
  spectatorMode?: boolean
  /** Top offset in pixels for fixed elements (to account for headers) */
  topOffset?: number
}

/**
 * 2D Game board layout - MTG Arena style.
 * This is the main orchestrator component that composes all game UI elements.
 */
export function GameBoard({ spectatorMode = false, topOffset = 0 }: GameBoardProps) {
  const playerGameState = useGameStore((state) => state.gameState)
  const spectatingState = useGameStore((state) => state.spectatingState)
  const playerId = useGameStore((state) => state.playerId)
  const submitAction = useGameStore((state) => state.submitAction)
  const combatState = useGameStore((state) => state.combatState)
  const confirmCombat = useGameStore((state) => state.confirmCombat)
  const clearAttackers = useGameStore((state) => state.clearAttackers)
  const clearBlockerAssignments = useGameStore((state) => state.clearBlockerAssignments)
  const attackWithAll = useGameStore((state) => state.attackWithAll)
  const priorityMode = useGameStore(selectPriorityMode)
  const nextStopPoint = useGameStore((state) => state.nextStopPoint)
  const serverPriorityMode = useGameStore((state) => state.priorityMode)
  const cyclePriorityMode = useGameStore((state) => state.cyclePriorityMode)
  const opponentDecisionStatus = useGameStore((state) => state.opponentDecisionStatus)
  const stopOverrides = useGameStore((state) => state.stopOverrides)
  const toggleStopOverride = useGameStore((state) => state.toggleStopOverride)
  const targetingState = useGameStore((state) => state.targetingState)
  const distributeState = useGameStore((state) => state.distributeState)
  const confirmDistribute = useGameStore((state) => state.confirmDistribute)
  const counterDistributionState = useGameStore((state) => state.counterDistributionState)
  const confirmCounterDistribution = useGameStore((state) => state.confirmCounterDistribution)
  const cancelCounterDistribution = useGameStore((state) => state.cancelCounterDistribution)
  const undoAvailable = useGameStore((state) => state.undoAvailable)
  const requestUndo = useGameStore((state) => state.requestUndo)
  const autoTapEnabled = useGameStore((state) => state.autoTapEnabled)
  const toggleAutoTap = useGameStore((state) => state.toggleAutoTap)
  const delveSelectionState = useGameStore((state) => state.delveSelectionState)
  const tapForPowerSelectionState = useGameStore((state) => state.tapForPowerSelectionState)
  const manaSelectionState = useGameStore((state) => state.manaSelectionState)
  // A multi-phase cast/activation in progress (convoke, waterbend, harmonize, targeting, …). While
  // one is mid-flight the player must finish or cancel it, not pass priority / move to combat.
  const pipelineState = useGameStore((state) => state.pipelineState)
  // Lobby settings — only for phrasing the attack-restriction banner ("attack left/right").
  // The actual legality always comes from the server's validAttackTargets.
  const lobbyState = useGameStore((state) => state.lobbyState)
  const cancelManaSelection = useGameStore((state) => state.cancelManaSelection)
  const { executeAction } = useInteraction()

  // In spectator mode, use spectatingState.gameState
  const gameState = spectatorMode ? spectatingState?.gameState : playerGameState

  // Multiplayer (3-4 player) view state. In a 2-player game none of this renders —
  // no rail, no board strip, no extra top offset — so the layout is unchanged.
  const opponents = useOpponents()
  const viewedOpponent = useViewedOpponent()
  const isMulti = (gameState?.players.length ?? 0) > 2
  // The multiplayer player rail is now a floating top-left column (under the fullscreen button),
  // not a reserved full-width band — so it no longer pushes the board down.
  const effectiveTopOffset = topOffset
  useMultiplayerView(isMulti, opponents)
  // Multiplayer camera extensions: the all-boards table overview (toggle / key 0) with
  // MTGO-style per-board collapse, the eliminated-spectator layout, and the combat
  // defender-focus split (combat between two *other* players shows attacker and defender
  // side by side).
  const overviewModeOn = useGameStore((state) => state.overviewMode)
  const collapsedSeats = useGameStore((state) => state.collapsedSeats)
  const toggleSeatCollapsed = useGameStore((state) => state.toggleSeatCollapsed)
  const eliminatedSpectating = useGameStore((state) => state.eliminatedSpectating)
  const eliminatedBottomSeatId = useGameStore((state) => state.eliminatedBottomSeatId)
  const setEliminatedBottomSeat = useGameStore((state) => state.setEliminatedBottomSeat)
  const returnToMenu = useGameStore((state) => state.returnToMenu)
  const viewingPlayer = useViewingPlayer()
  // Eliminated-spectator layout: the local player is out of a multiplayer game and chose
  // "Keep watching" — their dead bottom half collapses and all action UI hides.
  const isEliminatedSpectator =
    isMulti && !spectatorMode && eliminatedSpectating && (viewingPlayer?.hasLost ?? false)
  // Eliminated spectator's chosen bottom seat: a living player whose board fills the
  // (otherwise collapsed) bottom half, the way spectating anchors a bottom seat. Their
  // board leaves the opponent strip while anchored here. Self-heals if the seat dies.
  const eliminatedBottomSeat = useMemo(() => {
    if (!isEliminatedSpectator || !eliminatedBottomSeatId) return null
    const p = gameState?.players.find((pp) => pp.playerId === eliminatedBottomSeatId)
    return p && !p.hasLost ? p : null
  }, [isEliminatedSpectator, eliminatedBottomSeatId, gameState])
  // In a Two-Headed Giant game the orb/edge tints follow the *team* hue (both teammates share
  // it); otherwise they keep the per-seat hue. The team map is empty in every non-team game.
  const teamMap = useGameStore(selectTeamMap)
  const viewerTeam = useViewerTeamIndex()
  const isTeamGame = viewerTeam != null && Object.keys(teamMap).length > 0
  // The seat anchoring the bottom row: you when playing, the spectator's chosen/first seat otherwise.
  const anchorId = useGameStore(selectViewingPlayerId)
  // "Show the whole table" — the unified overview: two rows of boards. A team game splits by team
  // (your team on the bottom row, the enemy team on top); a free-for-all balances the seats evenly
  // with the anchor on the bottom (e.g. 6 players → 3 top / 3 bottom rather than 5 crammed on top).
  // The classic single-opponent sliding camera is what you get when this is off. Not for the
  // eliminated-spectator layout, which has its own bottom handling.
  const twoRowActive = isMulti && overviewModeOn && !isEliminatedSpectator
  const twoRow = useMemo(() => {
    const empty = { top: [] as EntityId[], bottom: [] as EntityId[] }
    if (!twoRowActive || !gameState) return empty
    const living = gameState.players.filter(
      (p) => !p.hasLost && p.playerId !== eliminatedBottomSeat?.playerId,
    )
    if (isTeamGame && viewerTeam != null) {
      return {
        top: living.filter((p) => teamMap[p.playerId] !== viewerTeam).map((p) => p.playerId),
        bottom: living.filter((p) => teamMap[p.playerId] === viewerTeam).map((p) => p.playerId),
      }
    }
    // Free-for-all: anchor first on the bottom, the rest split evenly — the top row takes the odd
    // one so the bottom (which holds your interactive board when playing) is never more crowded.
    const anchorFirst = [
      ...living.filter((p) => p.playerId === anchorId),
      ...living.filter((p) => p.playerId !== anchorId),
    ]
    const bottomCount = Math.max(1, Math.floor(living.length / 2))
    return {
      bottom: anchorFirst.slice(0, bottomCount).map((p) => p.playerId),
      top: anchorFirst.slice(bottomCount).map((p) => p.playerId),
    }
  }, [twoRowActive, gameState, eliminatedBottomSeat, isTeamGame, viewerTeam, teamMap, anchorId])
  const bottomRowIds = twoRow.bottom
  const topRowIds = twoRow.top
  // Camera pool = opponents not pulled down to the bottom row (so no board renders in both halves).
  const stripOpponents = useMemo(
    () =>
      opponents.filter(
        (o) => !o.hasLost && o.playerId !== eliminatedBottomSeat?.playerId && !bottomRowIds.includes(o.playerId),
      ),
    [opponents, eliminatedBottomSeat, bottomRowIds],
  )
  const viewedStripIndex = Math.max(
    0,
    stripOpponents.findIndex((o) => o.playerId === viewedOpponent?.playerId),
  )
  const defenderFocusIds = useCombatDefenderFocus(isMulti && !spectatorMode)
  const viewedSeatColor = useIdentityColor(viewedOpponent?.playerId ?? null)
  const isViewedOpponentAlly = useIsAlly(viewedOpponent?.playerId ?? null)
  const stripTouchX = useRef<number | null>(null)

  // Card counts per battlefield zone — used by useResponsive to decide when wrapping
  // will occur and shrink cards so the wrapped rows fit vertically. In multiplayer
  // the viewed opponent's board drives the shared sizing (each off-screen
  // Battlefield measures its own slot independently anyway).
  const battlefieldCards = useBattlefieldCards(isMulti ? viewedOpponent?.playerId ?? null : null)
  const zoneRowCounts = useMemo(
    () => [
      battlefieldCards.playerCreatures.length + battlefieldCards.playerPlaneswalkers.length,
      battlefieldCards.playerLands.length + battlefieldCards.playerOther.length,
      battlefieldCards.opponentCreatures.length + battlefieldCards.opponentPlaneswalkers.length,
      battlefieldCards.opponentLands.length + battlefieldCards.opponentOther.length,
    ],
    [battlefieldCards]
  )

  const responsive = useResponsive(effectiveTopOffset, zoneRowCounts)

  // Spectators (replay + live) default to the all-boards table overview: one seat anchored
  // at the bottom, every other board across the top — the natural "watch the whole table"
  // layout, rather than a one-board camera that hides most of the game. One-shot on entry so
  // a spectator who then focuses a single board (rail-chip click / key 0) isn't yanked back.
  // Desktop only — GameBoard degrades overview to the single-board camera on phones anyway.
  const didDefaultSpectatorOverview = useRef(false)
  useEffect(() => {
    if (didDefaultSpectatorOverview.current) return
    if (spectatorMode && isMulti && !responsive.isMobile) {
      didDefaultSpectatorOverview.current = true
      const store = useGameStore.getState()
      if (!store.overviewMode) store.toggleOverviewMode()
    }
  }, [spectatorMode, isMulti, responsive.isMobile])

  // Grid row 1: keeps the battlefield clear of the fixed/absolute opponent hand.
  const oppHandReservation =
    responsive.smallCardHeight + effectiveTopOffset + responsive.opponentHandBattlefieldGap

  // Confirm mana selection: if pipeline is active, advance it; otherwise build
  // modified LegalActionInfo with Explicit payment and route through executeAction
  const handleConfirmManaSelection = useCallback(() => {
    if (!manaSelectionState) return
    const { pipelineState, advancePipeline } = useGameStore.getState()
    if (pipelineState) {
      // Pipeline path: clear mana UI state directly (not via cancelManaSelection
      // which would cancel the entire pipeline) and advance
      useGameStore.setState({ manaSelectionState: null })
      advancePipeline({
        type: 'manaSource',
        selectedSources: [...manaSelectionState.selectedSources],
      })
      return
    }

    // Direct mana-button path: build Explicit payment and enter pipeline for remaining phases
    const paymentStrategy = {
      type: 'Explicit' as const,
      manaAbilitiesToActivate: [...manaSelectionState.selectedSources],
    }
    // Cast to add paymentStrategy - only actions with mana costs reach here
    const modifiedAction = { ...manaSelectionState.action, paymentStrategy } as import('../../types').GameAction
    // Strip mana-source fields so executeAction doesn't loop back here
    const { availableManaSources: _, autoTapPreview: _2, ...restActionInfo } = manaSelectionState.actionInfo
    const modifiedActionInfo: import('../../types').LegalActionInfo = {
      ...restActionInfo,
      action: modifiedAction,
    }
    cancelManaSelection()
    executeAction(modifiedActionInfo)
  }, [manaSelectionState, cancelManaSelection, executeAction])

  const opponent = useOpponent()
  const stackCards = useStackCards()
  const ghostCards = useGhostCards(playerId ?? null)

  // Table overview: every living opponent's board shares the strip. Entering the
  // eliminated-spectator layout turns it on; a chip click focuses one board again.
  const overviewActive = isMulti && overviewModeOn
  // Which opponent boards share the strip this frame. One (the viewed board, sliding
  // camera) normally; all living opponents in the overview; the viewed board plus the
  // defending seats during combat between two other players.
  const visibleStripIds = useMemo<readonly EntityId[]>(() => {
    if (!isMulti) return []
    const single = viewedOpponent ? [viewedOpponent.playerId] : []
    // Phones: the shared-strip views split into ~33% cells that are unusable in
    // portrait — keep the focused one-board camera only.
    if (responsive.isMobile) return single
    // Two-row "show table": the top row is the enemy team / the balanced top half.
    if (twoRowActive) return topRowIds
    if (overviewActive) return stripOpponents.map((o) => o.playerId)
    if (!viewedOpponent) return []
    const stripIds = new Set(stripOpponents.map((o) => o.playerId))
    const extras = defenderFocusIds.filter((id) => id !== viewedOpponent.playerId && stripIds.has(id))
    if (extras.length === 0) return single
    // Keep strip (turn) order so boards don't jump when defenders change.
    const visible = new Set([viewedOpponent.playerId, ...extras])
    return stripOpponents.filter((o) => visible.has(o.playerId)).map((o) => o.playerId)
  }, [isMulti, responsive.isMobile, twoRowActive, topRowIds, overviewActive, stripOpponents, viewedOpponent, defenderFocusIds])
  const multiView = visibleStripIds.length > 1
  // MTGO-style per-board collapse — table overview only; the focused camera and the
  // combat defender-focus split ignore it (their board sets are already deliberate).
  const collapsedStripIds = useMemo<readonly EntityId[]>(
    () => (overviewActive && multiView ? visibleStripIds.filter((id) => collapsedSeats.includes(id)) : []),
    [overviewActive, multiView, visibleStripIds, collapsedSeats],
  )
  // Boards whose cards + plate are actually on screen: the shared-strip cells minus the
  // collapsed ones. Drives which rail chips drop their player anchors.
  const expandedStripIds = useMemo<readonly EntityId[]>(
    () => visibleStripIds.filter((id) => !collapsedStripIds.includes(id)),
    [visibleStripIds, collapsedStripIds],
  )
  // In a shared-strip view the strip renders the visible cells in turn order (expanded
  // boards splitting the width, collapsed boards as narrow tabs), then the hidden *and*
  // collapsed full boards at full width, overflowing off-screen to the right — so card
  // anchors on boards without an expanded cell keep resolving to rail chips.
  const visibleStripCells = useMemo(
    () => stripOpponents.filter((o) => visibleStripIds.includes(o.playerId)),
    [stripOpponents, visibleStripIds],
  )
  const offscreenStripBoards = useMemo(
    () =>
      stripOpponents.filter(
        (o) => !visibleStripIds.includes(o.playerId) || collapsedStripIds.includes(o.playerId),
      ),
    [stripOpponents, visibleStripIds, collapsedStripIds],
  )
  // Expanded cells share the width left over by the collapsed tabs.
  const expandedCellBasis =
    collapsedStripIds.length > 0
      ? `calc((100% - ${collapsedStripIds.length * COLLAPSED_TAB_WIDTH}px) / ${Math.max(1, expandedStripIds.length)})`
      : `${100 / Math.max(1, expandedStripIds.length)}%`
  // The rail chips also cover the eliminated spectator's bottom-anchored board, whose
  // anchors live on the bottom life orb while it is on screen.
  const anchorVisibleBoardIds = useMemo<readonly EntityId[]>(
    () => (eliminatedBottomSeat ? [...expandedStripIds, eliminatedBottomSeat.playerId] : expandedStripIds),
    [expandedStripIds, eliminatedBottomSeat],
  )

  // Mindslaver-style hijack indicators (Phase 2C). Spectators don't get the UX promotions.
  const youAreHijacking = !spectatorMode ? (gameState?.youAreHijacking ?? null) : null
  const youAreHijackedBy = !spectatorMode ? (gameState?.youAreHijackedBy ?? null) : null
  // Single-client hotseat (play against yourself): this connection controls every seat.
  // When the seat currently to act is an opponent row, reuse the hijack rendering to
  // make that row interactive and dim our own — the board emphasis flips between seats
  // as priority alternates, like pass-and-play.
  const hotseat = !spectatorMode && (gameState?.hotseat ?? false)
  // The opponent seat (if any) this client is currently driving: the Mindslaver
  // victim, or — in hotseat — whichever opponent seat holds priority.
  const hijackControlledOpponentId =
    youAreHijacking ??
    (hotseat &&
    gameState?.priorityPlayerId &&
    viewingPlayer &&
    gameState.priorityPlayerId !== viewingPlayer.playerId
      ? gameState.priorityPlayerId
      : null)
  const isHijacking = hijackControlledOpponentId !== null
  const isHijacked = !!youAreHijackedBy
  // Soft purple wash for the battlefield row currently under hijack control. We avoid
  // a hard outline (which clipped at the zone-pile column on the right) — instead a
  // gradient + subtle inset glow give a "tinted surface" feel that respects the
  // existing layout boundaries.
  const hijackedSurfaceStyle: React.CSSProperties = {
    background: 'linear-gradient(180deg, rgba(124, 58, 237, 0.10) 0%, rgba(76, 29, 149, 0.04) 60%, rgba(76, 29, 149, 0.0) 100%)',
    borderRadius: 10,
    boxShadow: 'inset 0 0 0 1px rgba(168, 85, 247, 0.28), inset 0 0 24px rgba(168, 85, 247, 0.12)',
    transition: 'background 0.2s, box-shadow 0.2s',
  }
  // For spectator mode, we need to find players differently since playerId won't match
  const spectatorPlayer1 = spectatorMode && gameState
    ? gameState.players.find(p => p.playerId === spectatingState?.player1Id) ?? gameState.players[0]
    : null
  const spectatorPlayer2 = spectatorMode && gameState
    ? gameState.players.find(p => p.playerId === spectatingState?.player2Id) ?? gameState.players[1]
    : null

  // In spectator mode, use player1 as "bottom" player and player2 as "top" (opponent).
  // In multiplayer the viewed slot holds whichever opponent the camera is on.
  const effectiveViewingPlayer = spectatorMode ? spectatorPlayer1 : viewingPlayer
  const effectiveOpponent = isMulti
    ? viewedOpponent
    : spectatorMode
      ? spectatorPlayer2
      : opponent

  // The viewing player's own seat-identity color. In multiplayer "you" are tinted with the same
  // seat color everyone else sees for you (and that the rail's self chip uses), instead of the
  // fixed 2-player blue.
  const selfSeatColor = useIdentityColor(effectiveViewingPlayer?.playerId ?? null)

  // The seat anchoring the bottom HUD (right life orb + bottom half of the board): the
  // eliminated spectator's chosen living seat when one is set, else the viewing player.
  const bottomHudPlayer = eliminatedBottomSeat ?? effectiveViewingPlayer
  const bottomHudSeatColor = useIdentityColor(bottomHudPlayer?.playerId ?? null)

  // Team-split bottom half: the viewer's whole team, anchor seat first (leftmost), then teammates
  // in turn order. When playing, the anchor is your interactive board; when spectating it's just
  // the bottom-anchored seat. Teammate cells reuse the overview cell + per-board collapse.
  const bottomRowOrdered = useMemo(() => {
    if (!twoRowActive || !gameState) return []
    return gameState.players
      .filter((p) => bottomRowIds.includes(p.playerId))
      .sort((a, b) => (a.playerId === anchorId ? -1 : b.playerId === anchorId ? 1 : 0))
  }, [twoRowActive, gameState, bottomRowIds, anchorId])
  // The bottom half becomes a multi-board strip only when it holds more than the anchor (team
  // games; 4+ player free-for-alls). A lone anchor keeps the classic single bottom board.
  const bottomStripActive = twoRowActive && bottomRowOrdered.length > 1
  // Any bottom board may collapse to a tab — except your own interactive board when *playing*
  // (there's no collapse control on it, and folding the board you act from makes no sense). A
  // spectator's anchor seat is just another board, so it stays collapsible.
  const bottomCollapsedIds = useMemo(
    () =>
      bottomRowOrdered
        .filter(
          (p) =>
            collapsedSeats.includes(p.playerId) &&
            !(!spectatorMode && p.playerId === anchorId),
        )
        .map((p) => p.playerId),
    [bottomRowOrdered, collapsedSeats, anchorId, spectatorMode],
  )
  const bottomExpandedCount = Math.max(1, bottomRowOrdered.length - bottomCollapsedIds.length)
  const bottomCellBasis =
    bottomCollapsedIds.length > 0
      ? `calc((100% - ${bottomCollapsedIds.length * COLLAPSED_TAB_WIDTH}px) / ${bottomExpandedCount})`
      : `${100 / bottomExpandedCount}%`

  if (!gameState || (!spectatorMode && (!playerId || !viewingPlayer))) {
    return null
  }

  // In spectator mode: disable all interaction. In hotseat the single connection controls
  // whichever seat holds priority, so it can always act on a live priority window.
  const hasPriority = spectatorMode
    ? false
    : hotseat
      ? gameState.priorityPlayerId != null
      : (gameState.priorityPlayerId === viewingPlayer?.playerId ||
        // Mindslaver-style hijack: this client drives the controlled opponent, so it holds
        // priority whenever that opponent does (enables Pass, casting, ability activation).
        (youAreHijacking != null && gameState.priorityPlayerId === youAreHijacking))
  const canAct = hasPriority && !opponentDecisionStatus
  const isMyTurn = spectatorMode
    ? false
    : (gameState.activePlayerId === viewingPlayer?.playerId ||
      // During a hijack of the opponent's turn, treat it as "my turn" so the active-player
      // controls (combat declaration, sorcery-speed plays) light up for the driving client.
      (youAreHijacking != null && gameState.activePlayerId === youAreHijacking))
  const isInCombatMode = spectatorMode ? false : (combatState !== null)
  const isInDistributeMode = !spectatorMode && distributeState !== null
  const distributeTotalAllocated = distributeState
    ? Object.values(distributeState.distribution).reduce((sum, v) => sum + v, 0)
    : 0
  const distributeRemaining = distributeState ? distributeState.totalAmount - distributeTotalAllocated : 0
  const isInCounterDistMode = !spectatorMode && counterDistributionState !== null
  const isInManaSelectionMode = !spectatorMode && manaSelectionState !== null

  // Compute mana selection progress using most-constrained-first matching
  const manaProgress = useMemo(() => {
    if (!manaSelectionState) return null

    // Parse mana cost into requirements
    const symbols = manaSelectionState.manaCost.match(/\{([^}]+)\}/g)
    if (!symbols) return { satisfied: 0, total: 0, entries: [] }

    // Build list of unfulfilled requirements: colored pips + generic pips
    const coloredReqs: string[] = []
    let genericCount = 0
    for (const match of symbols) {
      const inner = match.slice(1, -1)
      const num = parseInt(inner, 10)
      if (!isNaN(num)) {
        genericCount += num
      } else if (inner !== 'X') {
        coloredReqs.push(inner)
      }
    }
    if (manaSelectionState.xValue > 0) {
      genericCount += manaSelectionState.xValue
    }
    // A Harmonize creature-tap reduces the generic mana to pay by its power
    // (printed {N} first, then the generic {X}); reflect it so the HUD shows the
    // real owed cost rather than the pre-tap {X} amount.
    if (manaSelectionState.harmonizeReduction > 0) {
      genericCount = Math.max(0, genericCount - manaSelectionState.harmonizeReduction)
    }
    const total = coloredReqs.length + genericCount

    // Build source list: each source has the set of colors it can pay
    // A source that produces {W, B, G} can satisfy W, B, or G colored reqs, or 1 generic
    // Multi-mana sources (e.g., Gilded Lotus producing 3) contribute multiple entries
    const sources: { colors: readonly string[] }[] = []
    for (const id of manaSelectionState.selectedSources) {
      const colors = manaSelectionState.sourceColors[id] ?? []
      const manaAmount = manaSelectionState.sourceManaAmounts?.[id] ?? 1
      for (let i = 0; i < manaAmount; i++) {
        sources.push({ colors: colors.length > 0 ? colors : ['C'] })
      }
    }

    // Most-constrained-first: assign sources with fewest color options first
    // This prevents flexible sources from "wasting" on requirements that
    // less flexible sources could have covered
    const sortedSources = [...sources].sort((a, b) => a.colors.length - b.colors.length)

    // Track remaining colored requirements as a mutable count map
    const remainingColorReqs: Record<string, number> = {}
    for (const c of coloredReqs) {
      remainingColorReqs[c] = (remainingColorReqs[c] ?? 0) + 1
    }
    let remainingGeneric = genericCount
    const colorSatisfied: Record<string, number> = {}
    let satisfied = 0

    // Floating mana already in the pool counts toward the cost — the engine pays
    // from the pool before tapping sources (CastPaymentProcessor.autoPay), so the
    // confirmation panel needs to credit it too. Without this, a player who taps
    // a Plains pre-cast sees "0/1" white owed even though their pool already has it.
    const floatingPool = viewingPlayer?.manaPool
    if (floatingPool) {
      const poolByPip: Record<string, number> = {
        W: floatingPool.white,
        U: floatingPool.blue,
        B: floatingPool.black,
        R: floatingPool.red,
        G: floatingPool.green,
        C: floatingPool.colorless,
      }
      // Spend exact-color pool first against colored pips
      for (const pip of Object.keys(poolByPip)) {
        while ((poolByPip[pip] ?? 0) > 0 && (remainingColorReqs[pip] ?? 0) > 0) {
          remainingColorReqs[pip]!--
          colorSatisfied[pip] = (colorSatisfied[pip] ?? 0) + 1
          poolByPip[pip]!--
          satisfied++
        }
      }
      // Anything left in the pool covers generic
      for (const pip of Object.keys(poolByPip)) {
        while ((poolByPip[pip] ?? 0) > 0 && remainingGeneric > 0) {
          remainingGeneric--
          colorSatisfied['1'] = (colorSatisfied['1'] ?? 0) + 1
          poolByPip[pip]!--
          satisfied++
        }
      }
    }

    for (const source of sortedSources) {
      // Try to assign to a colored requirement this source can pay
      let assigned = false
      for (const color of source.colors) {
        if ((remainingColorReqs[color] ?? 0) > 0) {
          remainingColorReqs[color]!--
          colorSatisfied[color] = (colorSatisfied[color] ?? 0) + 1
          satisfied++
          assigned = true
          break
        }
      }
      // If no colored requirement matched, assign to generic
      if (!assigned && remainingGeneric > 0) {
        remainingGeneric--
        colorSatisfied['1'] = (colorSatisfied['1'] ?? 0) + 1
        satisfied++
      }
    }

    // Build per-color requirement counts for display
    const colorRequired: Record<string, number> = {}
    for (const c of coloredReqs) {
      colorRequired[c] = (colorRequired[c] ?? 0) + 1
    }
    if (genericCount > 0) colorRequired['1'] = genericCount

    // Sort: colored first, generic last
    const entries = Object.entries(colorRequired).sort(([a], [b]) => {
      if (a === '1' && b !== '1') return 1
      if (a !== '1' && b === '1') return -1
      return a.localeCompare(b)
    })

    return { satisfied, total, entries, colorSatisfied }
  }, [manaSelectionState, viewingPlayer?.manaPool])

  // Attack restriction ("can only attack left/right", 2HG, …): during declare-attackers,
  // some living non-ally opponent is not a legal attack target. Drives the explainer
  // banner; the rail chips independently dim the unattackable seats. Teammates are
  // excluded — "you can't attack your ally" needs no banner.
  const attackRestriction = (() => {
    if (!isMulti || spectatorMode || combatState?.mode !== 'declareAttackers' || !gameState) return null
    // Candidates are relative to the *acting* seat — normally the viewing player, but a
    // hotseat/hijack declaration acts for another seat, whose own chip must not read as
    // "restricted" and whose enemies differ.
    const actingSeat = combatState.actingSeat ?? viewingPlayer?.playerId ?? null
    const actingTeam = actingSeat != null ? teamMap[actingSeat] : undefined
    const enemies = gameState.players.filter(
      (p) =>
        !p.hasLost &&
        p.playerId !== actingSeat &&
        !(actingTeam != null && teamMap[p.playerId] === actingTeam),
    )
    const attackable = enemies.filter((o) => combatState.validAttackTargets.includes(o.playerId))
    const restricted = enemies.filter((o) => !combatState.validAttackTargets.includes(o.playerId))
    if (attackable.length === 0 || restricted.length === 0) return null
    const lobbyMode =
      lobbyState?.settings.gameMode === 'FREE_FOR_ALL' ? lobbyState.settings.attackMode : undefined
    const direction = lobbyMode === 'LEFT' ? 'left' : lobbyMode === 'RIGHT' ? 'right' : null
    return { attackable, direction }
  })()
  const defenderPopupVisible =
    !spectatorMode &&
    isMulti &&
    combatState?.mode === 'declareAttackers' &&
    combatState.selectedAttackers.some((id) => !combatState.attackerTargets[id])

  const counterTotalAllocated = counterDistributionState
    ? Object.values(counterDistributionState.distribution).reduce<number>(
        (sum, byType) => sum + Object.values(byType).reduce<number>((s, v) => s + v, 0),
        0,
      )
    : 0
  // No "remaining" concept — X is determined by total allocated

  // Compute pass button label - prefer server-computed nextStopPoint, fall back to naive logic
  const getPassButtonLabel = () => {
    if (nextStopPoint) {
      return nextStopPoint
    }
    // Fallback for full control mode (server sends null)
    if (stackCards.length > 0) {
      return 'Resolve'
    }
    // On opponent's turn, just show "Pass" — we're yielding priority, not driving the turn
    if (!isMyTurn) {
      return 'Pass'
    }
    const nextStep = getNextStep(gameState.currentStep)
    if (nextStep) {
      if (nextStep === 'END') {
        return 'End Turn'
      }
      return `Pass to ${StepShortNames[nextStep]}`
    }
    return 'Pass'
  }

  // Get pass button colors based on priority mode
  const getPassButtonStyle = (): React.CSSProperties => {
    const hasStack = stackCards.length > 0
    if (hasStack) {
      // Resolve - keep orange
      return {
        backgroundColor: '#c76e00',
        borderColor: '#e08000',
      }
    }
    if (priorityMode === 'ownTurn') {
      // Own turn - blue/cyan
      return {
        backgroundColor: '#1976d2',
        borderColor: '#4fc3f7',
      }
    }
    // Responding - amber/gold
    return {
      backgroundColor: '#f57c00',
      borderColor: '#ffc107',
    }
  }

  return (
    <RenderProfiler id="GameBoard">
    <ResponsiveContext.Provider value={responsive}>
    <div style={{
      ...styles.container,
      padding: `0 ${responsive.containerPadding}px`,
      // Hand reservation rows (1 and 5) keep the battlefield rows from sliding
      // under the position:fixed hand overlays. Both 1fr rows then receive
      // identical heights → symmetric card sizes for player and opponent.
      // Eliminated spectator: the dead bottom half (rows 4-5) collapses — the freed
      // space flows to the opponent strip (row 2, the only remaining 1fr) — unless a
      // living player's board is anchored there, which restores the spectator-shaped
      // rows (small face-down hand at the bottom).
      gridTemplateRows: isEliminatedSpectator
        ? eliminatedBottomSeat
          ? `${oppHandReservation}px minmax(0, 1fr) auto minmax(0, 1fr) ${
              responsive.smallCardHeight + responsive.handBattlefieldGap
            }px`
          : `${oppHandReservation}px minmax(0, 1fr) auto 0px 0px`
        : `${oppHandReservation}px minmax(0, 1fr) auto minmax(0, 1fr) ${
            (spectatorMode ? responsive.smallCardHeight : responsive.cardHeight) + responsive.handBattlefieldGap
          }px`,
    }}>
      {/* Fullscreen button (top-left) */}
      <FullscreenButton />

      {/* Spectator-count indicator (top-left, next to fullscreen) - only for players */}
      {!spectatorMode && <SpectatorCountBadge />}

      {/* Concede button (top-right) - hidden in spectator mode. Stays live for the
          affected player even when the rest of the UI is disabled by hijack. An
          eliminated spectator has nothing to concede — they get a Leave button. */}
      {!spectatorMode && !isEliminatedSpectator && <ConcedeButton />}
      {isEliminatedSpectator && (
        <>
          <button
            onClick={returnToMenu}
            title="Leave the game and return to the menu"
            style={{
              position: 'fixed',
              top: responsive.isMobile ? 8 : 12,
              right: responsive.isMobile ? 8 : 12,
              zIndex: 120,
              height: 28,
              padding: '0 12px',
              display: 'inline-flex',
              alignItems: 'center',
              gap: 6,
              borderRadius: 6,
              border: '1px solid #555',
              background: 'rgba(18, 18, 26, 0.85)',
              color: '#ccc',
              fontSize: 12,
              fontWeight: 600,
              cursor: 'pointer',
            }}
          >
            Leave Game
          </button>
          {/* Spectating banner — top center, in the chrome row between the Fullscreen
              and Leave Game buttons (the bottom of the screen belongs to the step strip
              or the anchored bottom board). It also carries the bottom-board picker:
              put a living player's board in the empty bottom half (click the active
              seat again to clear it). */}
          <div
            role="status"
            style={{
              position: 'fixed',
              top: responsive.isMobile ? 8 : 12,
              left: '50%',
              transform: 'translateX(-50%)',
              zIndex: 90,
              display: 'inline-flex',
              alignItems: 'center',
              gap: 8,
              padding: '5px 14px',
              borderRadius: 999,
              border: '1px solid #3a3a44',
              background: 'rgba(10, 12, 20, 0.9)',
              color: '#9fb0d0',
              fontSize: 12,
              fontWeight: 600,
              whiteSpace: 'nowrap',
              userSelect: 'none',
            }}
          >
            <span aria-hidden>💀</span>
            You've been eliminated — spectating
            <span aria-hidden style={{ width: 1, height: 14, background: '#3a3a44' }} />
            <span style={{ color: '#7c8aa8', fontWeight: 600 }}>Board below:</span>
            {opponents.filter((o) => !o.hasLost).map((o) => {
              const idx = gameState.players.findIndex((p) => p.playerId === o.playerId)
              const seat = identitySeatColor(teamMap, o.playerId, idx)
              const active = eliminatedBottomSeat?.playerId === o.playerId
              return (
                <button
                  key={o.playerId}
                  onClick={() => setEliminatedBottomSeat(active ? null : o.playerId)}
                  title={active
                    ? `Stop showing ${o.name}'s board at the bottom`
                    : `Show ${o.name}'s board in the bottom half`}
                  style={{
                    display: 'inline-flex',
                    alignItems: 'center',
                    gap: 5,
                    height: 22,
                    padding: '0 9px',
                    borderRadius: 999,
                    border: `${active ? 2 : 1}px solid ${active ? seat.bright : seat.base}`,
                    background: active ? seat.soft : 'rgba(10, 12, 20, 0.85)',
                    color: seat.bright,
                    fontSize: 11,
                    fontWeight: 700,
                    cursor: 'pointer',
                    whiteSpace: 'nowrap',
                  }}
                >
                  <span aria-hidden style={{
                    width: 7,
                    height: 7,
                    borderRadius: '50%',
                    background: seat.base,
                    boxShadow: `0 0 4px ${seat.base}`,
                  }} />
                  {o.name}
                </button>
              )
            })}
          </div>
        </>
      )}


      {/* Opponent half. 2-player: today's exact markup (fixed hand at the top +
          board area on grid row 2), via OpponentBoardArea's grid layout.
          Multiplayer: an always-visible opponent rail + a horizontally sliding
          strip of full-size boards, one per living opponent, in turn order. */}
      {!isMulti && effectiveOpponent && (
        <OpponentBoardArea
          opponent={effectiveOpponent}
          layout="grid"
          topOffset={effectiveTopOffset}
          spectatorMode={spectatorMode}
          isHijacking={isHijacking}
          hijackedSurfaceStyle={hijackedSurfaceStyle}
        />
      )}
      {isMulti && (
        <>
          <OpponentRail
            spectatorMode={spectatorMode}
            visibleBoardIds={anchorVisibleBoardIds}
            topOffset={effectiveTopOffset}
          />
          <div
            data-opponent-strip
            style={{
              gridRow: '1 / 3',
              position: 'relative',
              overflow: 'hidden',
              minHeight: 0,
              minWidth: 0,
              // Shared-strip views: keep the leftmost board clear of the fixed rail
              // column, and every cell's top (name plates, zone piles) clear of the
              // Fullscreen/Concede button row (single view centers one full-width
              // board below the hand reservation, so no clash there).
              paddingLeft: multiView ? railReservedWidth(responsive) : 0,
              paddingTop: multiView ? 48 : 0,
              boxSizing: 'border-box',
            }}
            onTouchStart={(e) => {
              stripTouchX.current = e.touches[0]?.clientX ?? null
            }}
            onTouchEnd={(e) => {
              const start = stripTouchX.current
              stripTouchX.current = null
              // Swiping between boards only applies to the one-board sliding camera.
              if (multiView) return
              const end = e.changedTouches[0]?.clientX ?? null
              if (start == null || end == null) return
              const store = useGameStore.getState()
              // A swipe must not fight an in-progress card drag.
              if (store.draggingCardId || store.draggingAttackerId || store.draggingBlockerId) return
              const dx = end - start
              if (Math.abs(dx) < 48) return
              const next = stripOpponents[viewedStripIndex + (dx < 0 ? 1 : -1)]
              if (next) store.viewOpponent(next.playerId)
            }}
          >
            <div
              style={{
                display: 'flex',
                width: '100%',
                height: '100%',
                transform: multiView ? 'none' : `translateX(-${viewedStripIndex * 100}%)`,
                transition: 'transform 220ms cubic-bezier(0.4, 0, 0.2, 1)',
              }}
            >
              {(() => {
                const renderStripBoard = (o: (typeof stripOpponents)[number], expandedCell: boolean) => {
                  // Two-Headed Giant: your teammate's board is an ally (face-up hand + marker).
                  const oIsAlly = viewerTeam != null && teamMap[o.playerId] === viewerTeam
                  const isViewedCell = o.playerId === viewedOpponent?.playerId
                  return (
                    <OpponentBoardArea
                      key={o.playerId}
                      opponent={o}
                      layout="strip"
                      topOffset={effectiveTopOffset}
                      handReservation={oppHandReservation}
                      // Shared-strip views: expanded cells split the width left over by
                      // the collapsed tabs; hidden/collapsed full boards keep full width
                      // and overflow off-screen (see offscreenStripBoards).
                      stripBasis={multiView && expandedCell ? expandedCellBasis : '100%'}
                      hideHand={multiView}
                      // The viewed board's anchors stay on the center-HUD orb; every other
                      // expanded board's plate takes over from its rail chip.
                      plateCarriesAnchors={multiView && expandedCell && !isViewedCell}
                      {...(multiView && expandedCell && isViewedCell
                        ? { viewedRingColor: identitySeatColor(teamMap, o.playerId, gameState.players.findIndex((p) => p.playerId === o.playerId)).base }
                        : {})}
                      // Fold-away control — table overview only; the combat split and the
                      // focused camera keep their deliberate board sets.
                      {...(overviewActive && multiView && expandedCell
                        ? { onToggleCollapse: () => toggleSeatCollapsed(o.playerId) }
                        : {})}
                      spectatorMode={spectatorMode}
                      isHijacking={hijackControlledOpponentId === o.playerId}
                      hijackedSurfaceStyle={hijackedSurfaceStyle}
                      isAlly={oIsAlly}
                      {...(oIsAlly && viewerTeam != null ? { allyColor: selfSeatColor.base } : {})}
                    />
                  )
                }
                if (!multiView) return stripOpponents.map((o) => renderStripBoard(o, false))
                return (
                  <>
                    {visibleStripCells.map((o) =>
                      collapsedStripIds.includes(o.playerId) ? (
                        <CollapsedBoardTab
                          key={`${o.playerId}-collapsed-tab`}
                          player={o}
                          onExpand={() => toggleSeatCollapsed(o.playerId)}
                        />
                      ) : (
                        renderStripBoard(o, true)
                      ),
                    )}
                    {offscreenStripBoards.map((o) => renderStripBoard(o, false))}
                  </>
                )
              })()}
            </div>
            {/* Seat-colored edge flash on board arrival */}
            {!multiView && viewedOpponent && (
              <div
                key={viewedOpponent.playerId}
                aria-hidden
                style={{
                  position: 'absolute',
                  inset: 0,
                  pointerEvents: 'none',
                  borderRadius: 10,
                  boxShadow: `inset 0 0 0 2px ${viewedSeatColor.base}, inset 0 0 26px ${viewedSeatColor.soft}`,
                  opacity: 0,
                  animation: 'seatEdgeFlash 700ms ease-out',
                }}
              />
            )}
          </div>
        </>
      )}

      {/* Spectator mode: floating opponent name label (2-player only — the rail
          carries opponent identity in multiplayer) */}
      {spectatorMode && !isMulti && effectiveOpponent && (
        <div
          style={{
            ...styles.spectatorNameLabel,
            position: 'fixed',
            top: effectiveTopOffset + responsive.smallCardHeight + responsive.opponentHandBattlefieldGap + 8,
            left: 16,
          }}
        >
          {effectiveOpponent.name}
        </div>
      )}

      {/* Center - Life totals, phase indicator, and stack.
          Grid row 2 — a reserved partition that the opponent/player areas
          can't cross. The outer flex row mirrors `playerRowWithZones` (main
          area + zone pile spacer) so the step strip aligns with the cards
          above/below rather than the viewport. */}
      <div style={{
        gridRow: 3,
        display: 'flex',
        alignItems: 'center',
        gap: 32,
        width: '100%',
      }}>
      <div style={{
        ...styles.centerArea,
        flex: 1,
        minWidth: 0,
        width: 'auto',
        columnGap: responsive.isMobile ? 6 : 16,
      }}>
        {/* Opponent life (left side). In multiplayer this is the *viewed*
            opponent's orb, seat-tinted to match their rail chip — a full-size
            targeting click target in the familiar spot (the chip carries the
            anchors for the other, off-screen opponents). */}
        <div style={{ ...styles.centerLifeSection, ...styles.centerLifeSectionLeft }}>
          {effectiveOpponent && (
            <>
              <LifeDisplay
                life={effectiveOpponent.life}
                playerId={effectiveOpponent.playerId}
                playerName={effectiveOpponent.name}
                spectatorMode={spectatorMode}
                poisonCounters={effectiveOpponent.poisonCounters}
                commanderDamage={effectiveOpponent.commanderDamage ?? []}
                handSize={effectiveOpponent.handSize}
                maxHandSize={effectiveOpponent.maxHandSize}
                {...(isMulti ? { seatColor: viewedSeatColor.base } : {})}
                {...(isViewedOpponentAlly ? { isAlly: true } : {})}
              />
              {!responsive.isMobile && <ActiveEffectsBadges effects={effectiveOpponent.activeEffects} />}
              {!responsive.isMobile && effectiveOpponent.manaPool && <ManaPool manaPool={effectiveOpponent.manaPool} />}
            </>
          )}
        </div>

        {/* Step strip (center) — wrapped so the Mindslaver-style hijack indicator
            can sit directly above it inside the center HUD. */}
        <div style={{
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          gap: 4,
          minWidth: 0,
        }}>
        {(isHijacking || isHijacked || hotseat) && (() => {
          const text = hotseat
            ? `Hotseat — acting as ${gameState.players.find((p) => p.playerId === gameState.priorityPlayerId)?.name ?? 'active player'}`
            : (() => {
                const otherId = youAreHijacking ?? youAreHijackedBy
                const otherName = gameState.players.find((p) => p.playerId === otherId)?.name ?? 'opponent'
                return isHijacking ? `Controlling ${otherName}'s turn` : `${otherName} controls your turn`
              })()
          return (
            <div
              role="status"
              aria-live="polite"
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: 6,
                padding: '3px 10px',
                borderRadius: 999,
                background: 'rgba(76, 29, 149, 0.55)',
                color: '#f3e8ff',
                fontSize: 11,
                fontWeight: 600,
                letterSpacing: '0.04em',
                textTransform: 'uppercase',
                border: '1px solid rgba(168, 85, 247, 0.6)',
                boxShadow: '0 0 10px rgba(168, 85, 247, 0.25)',
                whiteSpace: 'nowrap',
                userSelect: 'none',
              }}
            >
              <span aria-hidden style={{ fontSize: 11 }}>🔒</span>
              <span>{text}</span>
            </div>
          )
        })()}
        <StepStrip
          phase={gameState.currentPhase}
          step={gameState.currentStep}
          turnNumber={gameState.turnNumber}
          isActivePlayer={isMyTurn}
          hasPriority={hasPriority}
          priorityMode={priorityMode}
          activePlayerName={spectatorMode
            ? gameState.players.find(p => p.playerId === gameState.activePlayerId)?.name
            : undefined
          }
          activeSide={
            spectatorMode
              ? (gameState.activePlayerId === effectiveViewingPlayer?.playerId ? 'bottom' : 'top')
              : (isMyTurn || (eliminatedBottomSeat != null && gameState.activePlayerId === eliminatedBottomSeat.playerId)
                  ? 'bottom'
                  : 'top')
          }
          stopOverrides={stopOverrides}
          onToggleStop={toggleStopOverride}
          isSpectator={spectatorMode}
        />
        </div>

        {/* Player life (right side) — the eliminated spectator's chosen bottom seat
            takes this orb over (their board fills the bottom half). */}
        <div style={{ ...styles.centerLifeSection, ...styles.centerLifeSectionRight }}>
          {bottomHudPlayer && (
            <>
              {/* When another seat is anchored here (eliminated spectator), the orb is
                  spectator-shaped: no "You" role tag, no player-click handling. */}
              <LifeDisplay life={bottomHudPlayer.life} isPlayer playerId={bottomHudPlayer.playerId} playerName={bottomHudPlayer.name} spectatorMode={spectatorMode || eliminatedBottomSeat != null} poisonCounters={bottomHudPlayer.poisonCounters} commanderDamage={bottomHudPlayer.commanderDamage ?? []} handSize={bottomHudPlayer.handSize} maxHandSize={bottomHudPlayer.maxHandSize} {...(isMulti ? { seatColor: bottomHudSeatColor.base } : {})} />
              {!responsive.isMobile && <ActiveEffectsBadges effects={bottomHudPlayer.activeEffects} />}
              {!responsive.isMobile && bottomHudPlayer.manaPool && <ManaPool manaPool={bottomHudPlayer.manaPool} />}
            </>
          )}
        </div>
      </div>
        {/* Spacer matching the battlefield's ZonePile column so the centered
            content aligns with the cards above/below rather than the viewport.
            Matches ZonePile's `minWidth: pileWidth + 10` (see ZonePiles.tsx) so
            the centerArea never slides under the pile column — otherwise the
            step-strip glow / player life orb visibly overlap the deck/graveyard
            /exile that intrude into this row via their vertical margin offsets. */}
        <div style={{ width: responsive.pileWidth + 10, flexShrink: 0 }} aria-hidden />
      </div>

      {/* Stack display - floating on the left side */}
      <StackDisplay />


      {/* Player area (grid row 4) — player-hand reservation lives in row 5,
          so no padding here. Equal-height with row 2 → symmetric cards.
          Collapsed (row is 0px) for an eliminated spectator — their permanents
          left the game with them. */}
      {/* Eliminated spectator with a chosen bottom seat: that living player's board fills
          the bottom half, rendered read-only the way spectating renders a bottom seat. */}
      {isEliminatedSpectator && eliminatedBottomSeat && (
        <div style={styles.playerArea}>
          <div style={styles.playerRowWithZones}>
            <CommandZone player={eliminatedBottomSeat} />
            <div style={styles.playerMainArea}>
              <Battlefield isOpponent={false} playerId={eliminatedBottomSeat.playerId} spectatorMode />
            </div>
            <ZonePile player={eliminatedBottomSeat} />
          </div>
        </div>
      )}
      {isEliminatedSpectator && eliminatedBottomSeat && (
        <div
          data-zone="hand"
          style={{
            position: 'fixed',
            bottom: 0,
            left: '50%',
            transform: 'translateX(-50%)',
            zIndex: 50,
          }}
        >
          <CardRow zoneId={hand(eliminatedBottomSeat.playerId)} faceDown small />
        </div>
      )}

      {/* Show-table bottom row: the viewer's team (team game) or the balanced bottom half (FFA)
          shares the bottom as a multi-board strip (rows 4-5), mirroring the top row. Your own
          board (when playing) keeps its interactive battlefield + fixed hand; the others render as
          overview cells (lands toward the bottom edge) with per-board collapse. Otherwise the
          classic single bottom board. */}
      {!isEliminatedSpectator && (bottomStripActive ? (
        <div
          data-team-strip="bottom"
          style={{
            gridRow: '4 / 6',
            position: 'relative',
            display: 'flex',
            overflow: 'hidden',
            minHeight: 0,
            minWidth: 0,
            paddingLeft: railReservedWidth(responsive),
            boxSizing: 'border-box',
          }}
        >
          {bottomRowOrdered.map((p) => {
            const isAnchorSelf = !spectatorMode && p.playerId === anchorId
            if (bottomCollapsedIds.includes(p.playerId)) {
              return (
                <CollapsedBoardTab key={`${p.playerId}-collapsed`} player={p} onExpand={() => toggleSeatCollapsed(p.playerId)} />
              )
            }
            if (isAnchorSelf) {
              return (
                <div
                  key={p.playerId}
                  style={{
                    flex: `0 0 ${bottomCellBasis}`,
                    minWidth: bottomCellBasis,
                    height: '100%',
                    display: 'flex',
                    flexDirection: 'column',
                    overflow: 'hidden',
                    position: 'relative',
                  }}
                >
                  {/* Reservation band mirrors the other cells' name-plate band so all bottom
                      boards line up. */}
                  <div style={{ height: 34, flexShrink: 0 }} aria-hidden />
                  <div style={{ ...styles.playerRowWithZones, alignItems: 'flex-start', flex: 1 }}>
                    <CommandZone player={p} />
                    <div style={{ ...styles.playerMainArea, ...(isHijacked ? hijackedSurfaceStyle : null) }}>
                      <Battlefield isOpponent={false} spectatorMode={spectatorMode} />
                    </div>
                    <ZonePile player={p} />
                  </div>
                </div>
              )
            }
            return (
              <OpponentBoardArea
                key={p.playerId}
                opponent={p}
                layout="strip"
                topOffset={0}
                handReservation={oppHandReservation}
                stripBasis={bottomCellBasis}
                hideHand
                bottomHalf
                plateCarriesAnchors={p.playerId !== bottomHudPlayer?.playerId}
                onToggleCollapse={() => toggleSeatCollapsed(p.playerId)}
                spectatorMode={spectatorMode}
                isHijacking={hijackControlledOpponentId === p.playerId}
                hijackedSurfaceStyle={hijackedSurfaceStyle}
                isAlly={isTeamGame && !spectatorMode}
                {...(isTeamGame && !spectatorMode ? { allyColor: selfSeatColor.base } : {})}
              />
            )
          })}
        </div>
      ) : (
        <div style={styles.playerArea}>
          <div style={styles.playerRowWithZones}>
            {/* Player command zone (left side) — Commander format only; renders nothing otherwise. */}
            {effectiveViewingPlayer && <CommandZone player={effectiveViewingPlayer} />}

            <div style={{
              ...styles.playerMainArea,
              ...(isHijacked ? hijackedSurfaceStyle : null),
            }}>
              {/* Player battlefield - creatures first (closer to center), then lands */}
              <Battlefield isOpponent={false} spectatorMode={spectatorMode} />

            </div>

            {/* Player deck/graveyard (right side) */}
            {effectiveViewingPlayer && <ZonePile player={effectiveViewingPlayer} />}
          </div>
        </div>
      ))}

      {/* Spectator mode: floating player name label for bottom player */}
      {spectatorMode && effectiveViewingPlayer && !bottomStripActive && (
        <div
          style={{
            ...styles.spectatorNameLabel,
            position: 'fixed',
            bottom: responsive.smallCardHeight + responsive.handBattlefieldGap + 8,
            left: 16,
          }}
        >
          {effectiveViewingPlayer.name}
        </div>
      )}

      {/* Player hand - fixed at bottom of screen (gone for an eliminated spectator, and for a
          spectator team-split where the bottom cells hide hands like the overview). A playing
          team-split keeps it — it's your interactive hand. */}
      {!isEliminatedSpectator && !(bottomStripActive && spectatorMode) && <div
        data-zone="hand"
        data-hijack-controlled={isHijacked || undefined}
        data-hijack-dim={(isHijacking && !isHijacked) || undefined}
        style={{
          position: 'fixed',
          bottom: 0,
          left: '50%',
          transform: 'translateX(-50%)',
          zIndex: 50,
          padding: isHijacked ? 6 : 0,
          borderRadius: isHijacked ? 12 : 0,
          outline: isHijacked ? '2px solid #a855f7' : 'none',
          outlineOffset: isHijacked ? -2 : 0,
          boxShadow: isHijacked ? '0 0 14px rgba(168, 85, 247, 0.35)' : 'none',
          background: isHijacked ? 'rgba(76, 29, 149, 0.18)' : 'transparent',
          // Affected player's hand is inert during hijack — let clicks fall through. The hand is
          // also inert while a cast/activation pipeline is in progress (e.g. paying a waterbend or
          // mana cost): you must finish or cancel the current cast before starting another.
          pointerEvents: (isHijacked || pipelineState != null) ? 'none' : undefined,
          // Controller can still see their own hand but cannot act with it during V's turn.
          opacity: isHijacking ? 0.6 : (pipelineState != null ? 0.6 : 1),
          filter: isHijacking ? 'saturate(0.6)' : 'none',
          transition: 'box-shadow 0.2s, outline-color 0.2s, opacity 0.2s',
        }}
      >
        {isHijacked && (
          <div
            style={{
              position: 'absolute',
              left: 8,
              top: -10,
              background: '#4c1d95',
              color: 'white',
              fontSize: 10,
              fontWeight: 600,
              padding: '2px 8px',
              borderRadius: 999,
              border: '1px solid #a855f7',
              boxShadow: '0 1px 4px rgba(0,0,0,0.4)',
              whiteSpace: 'nowrap',
              pointerEvents: 'none',
              zIndex: 60,
            }}
          >
            🔒 Opponent controls
          </div>
        )}
        {spectatorMode && effectiveViewingPlayer ? (
          <CardRow
            zoneId={hand(effectiveViewingPlayer.playerId)}
            faceDown
            small
          />
        ) : playerId ? (
          <CardRow
            zoneId={hand(playerId)}
            faceDown={false}
            // Inert during hijack and while a cast/activation pipeline is in progress (e.g. paying
            // a waterbend or mana cost) — the CardRow re-enables pointer events on its cards, so the
            // wrapper's pointerEvents:none isn't enough; gate interactivity at the component level.
            interactive={!isHijacked && pipelineState == null}
            ghostCards={isHijacked ? [] : ghostCards}
          />
        ) : null}
      </div>}

      {/* Floating pass/resolve button (bottom-right) - always present, disabled when unavailable */}
      {!spectatorMode && !isEliminatedSpectator && viewingPlayer && !isInManaSelectionMode && !isInCounterDistMode && (() => {
        const passEnabled = canAct && !isHijacked && !isInCombatMode && !isInDistributeMode && !isInCounterDistMode && !isInManaSelectionMode && !delveSelectionState && !tapForPowerSelectionState && !targetingState && !pipelineState
        return (
          <div style={{
            position: 'fixed',
            bottom: 16,
            right: 16,
            zIndex: 100,
          }}>
            <button
              disabled={!passEnabled}
              onClick={() => {
                submitAction({
                  type: 'PassPriority',
                  // In hotseat, pass for whichever seat currently holds priority. During a
                  // Mindslaver-style hijack, pass for the controlled opponent (whose priority
                  // window this is), not our own seat.
                  playerId: hotseat
                    ? (gameState.priorityPlayerId ?? viewingPlayer.playerId)
                    : (youAreHijacking != null && gameState.priorityPlayerId === youAreHijacking
                      ? youAreHijacking
                      : viewingPlayer.playerId),
                })
              }}
              style={{
                ...styles.floatingBarButton,
                ...(passEnabled ? getPassButtonStyle() : {}),
                // On phones the desktop-sized button dwarfs the other
                // controls and covers the hand — let the label size it.
                width: responsive.isMobile ? 'auto' : 170,
                height: responsive.isMobile ? 28 : 42,
                padding: responsive.isMobile ? '0 10px' : '0 24px',
                color: passEnabled ? 'white' : '#555',
                fontWeight: 600,
                fontSize: responsive.isMobile ? 12 : responsive.fontSize.normal,
                border: passEnabled ? `1px solid ${getPassButtonStyle().borderColor}` : '1px solid #333',
                transition: 'background-color 0.2s, border-color 0.2s',
                opacity: passEnabled ? 1 : 0.4,
                cursor: passEnabled ? 'pointer' : 'default',
              }}
            >
              {(() => {
                const label = passEnabled ? getPassButtonLabel() : 'Pass'
                // "Pass to Attackers" is too wide for a phone — "→ Attackers"
                // carries the same meaning in half the space.
                return responsive.isMobile ? label.replace(/^Pass to /, '→ ') : label
              })()}
            </button>
          </div>
        )
      })()}

      {/* Undo, priority mode icons (bottom-right, above pass button) */}
      {!spectatorMode && !isEliminatedSpectator && viewingPlayer && !isInManaSelectionMode && !isInCounterDistMode && (
        <div style={{
          position: 'fixed',
          bottom: responsive.isMobile ? 50 : 66,
          right: 16,
          display: 'flex',
          gap: 4,
          alignItems: 'stretch',
          zIndex: 100,
        }}>
          <button
            onClick={requestUndo}
            disabled={!undoAvailable}
            title="Undo"
            style={{
              ...styles.floatingBarButton,
              color: undoAvailable ? '#d4a017' : '#555',
              border: undoAvailable ? '1px solid #8b7000' : '1px solid #333',
              opacity: undoAvailable ? 1 : 0.4,
              cursor: undoAvailable ? 'pointer' : 'default',
            }}
          >
            <i className="ms ms-untap" style={{ fontSize: 14 }} />
          </button>
          <button
            onClick={toggleAutoTap}
            title={
              autoTapEnabled
                ? 'Auto Tap: Lands are tapped automatically. Click to switch to manual mana selection.'
                : 'Manual Tap: You choose which lands to tap. Click to switch to auto tap.'
            }
            style={{
              ...styles.floatingBarButton,
              backgroundColor: autoTapEnabled ? 'rgba(40, 40, 40, 0.8)' : 'rgba(245, 158, 11, 0.9)',
              color: autoTapEnabled ? '#999' : '#000',
              border: autoTapEnabled ? '1px solid #555' : '1px solid #f59e0b',
              cursor: 'pointer',
              transition: 'all 0.2s',
            }}
          >
            <i className="ms ms-land" style={{ fontSize: 14 }} />
          </button>
          <button
            onClick={cyclePriorityMode}
            title={
              serverPriorityMode === 'fullControl'
                ? 'Full Control: You receive priority at every step. Click to switch to Auto.'
                : serverPriorityMode === 'stops'
                ? 'Stops: Pauses on opponent spells/abilities and combat damage. Click to switch to Full Control.'
                : 'Auto: Smart auto-passing. Click to switch to Stops.'
            }
            style={{
              ...styles.floatingBarButton,
              width: 'auto',
              padding: '0 8px',
              backgroundColor:
                serverPriorityMode === 'fullControl' ? 'rgba(79, 195, 247, 0.9)' :
                serverPriorityMode === 'stops' ? 'rgba(245, 158, 11, 0.9)' :
                'rgba(40, 40, 40, 0.8)',
              color:
                serverPriorityMode === 'fullControl' ? '#000' :
                serverPriorityMode === 'stops' ? '#000' :
                '#999',
              border:
                serverPriorityMode === 'fullControl' ? '1px solid #4fc3f7' :
                serverPriorityMode === 'stops' ? '1px solid #f59e0b' :
                '1px solid #555',
              cursor: 'pointer',
              transition: 'all 0.2s',
            }}
          >
            {serverPriorityMode === 'fullControl' ? 'Full Control' :
             serverPriorityMode === 'stops' ? 'Stops' :
             'Auto'}
          </button>
        </div>
      )}

      {/* Attack-restriction explainer — "attack left/right" (CR 803.1) and similar
          restrictions are invisible on the board itself, so say exactly who can be
          attacked. Sits above the defender popup / combat buttons. */}
      {attackRestriction && (
        <div
          role="status"
          style={{
            position: 'fixed',
            bottom: defenderPopupVisible ? 196 : 110,
            right: 16,
            zIndex: 108,
            display: 'inline-flex',
            alignItems: 'center',
            gap: 6,
            padding: '5px 12px',
            borderRadius: 999,
            border: '1px solid rgba(239, 83, 80, 0.55)',
            background: 'rgba(26, 10, 10, 0.92)',
            color: '#ffb3b3',
            fontSize: responsive.isMobile ? 11 : 12,
            fontWeight: 600,
            whiteSpace: 'nowrap',
            userSelect: 'none',
          }}
        >
          <span aria-hidden>⚔</span>
          <span>
            {attackRestriction.direction ? `Attack ${attackRestriction.direction} — you can only attack ` : 'You can only attack '}
            {attackRestriction.attackable.map((o, i) => {
              const idx = gameState.players.findIndex((p) => p.playerId === o.playerId)
              const seat = identitySeatColor(teamMap, o.playerId, idx)
              return (
                <span key={o.playerId}>
                  {i > 0 ? ', ' : ''}
                  <span style={{ color: seat.bright, fontWeight: 800 }}>{o.name}</span>
                </span>
              )
            })}
          </span>
        </div>
      )}

      {/* Multiplayer defender pick — pops on the first attacker selection until a
          defender is chosen; assignment then turns sticky (rail-chip clicks
          override per attacker). Confirm stays disabled while any selected
          attacker has no defender, so attacks are never silently misdirected. */}
      {!spectatorMode && isMulti && combatState?.mode === 'declareAttackers' &&
        combatState.selectedAttackers.some((id) => !combatState.attackerTargets[id]) && (() => {
          const unassignedCount = combatState.selectedAttackers.filter((id) => !combatState.attackerTargets[id]).length
          // Only offer players the engine says are legal attack targets — in Two-Headed Giant
          // that excludes your own teammate (you can't attack your own team).
          const living = opponents.filter(
            (o) => !o.hasLost && combatState.validAttackTargets.includes(o.playerId),
          )
          return (
            <div style={{
              position: 'fixed',
              bottom: 110,
              right: 16,
              zIndex: 110,
              display: 'flex',
              flexDirection: 'column',
              gap: 6,
              alignItems: 'flex-end',
            }}>
              <div style={{
                background: 'rgba(0, 0, 0, 0.9)',
                border: '1px solid #ef5350',
                borderRadius: 8,
                padding: '6px 12px',
                color: '#ffb3b3',
                fontSize: responsive.isMobile ? 11 : 12,
                fontWeight: 600,
              }}>
                Attack which player?{combatState.selectedAttackers.length > 1 ? ` (${unassignedCount} unassigned)` : ''}
              </div>
              <div style={{ display: 'flex', gap: 6 }}>
                {living.map((o) => {
                  const idx = gameState.players.findIndex((p) => p.playerId === o.playerId)
                  const seat = identitySeatColor(teamMap, o.playerId, idx)
                  return (
                    <button
                      key={o.playerId}
                      onClick={() => useGameStore.getState().assignDefenderToSelectedAttackers(o.playerId)}
                      style={{
                        display: 'inline-flex',
                        alignItems: 'center',
                        gap: 6,
                        height: 30,
                        padding: '0 12px',
                        borderRadius: 999,
                        border: `2px solid ${seat.base}`,
                        background: 'rgba(10, 12, 20, 0.92)',
                        color: seat.bright,
                        fontSize: responsive.isMobile ? 11 : 12,
                        fontWeight: 700,
                        cursor: 'pointer',
                        whiteSpace: 'nowrap',
                      }}
                    >
                      <span aria-hidden style={{
                        width: 9,
                        height: 9,
                        borderRadius: '50%',
                        background: seat.base,
                        boxShadow: `0 0 5px ${seat.base}`,
                      }} />
                      {o.name}
                    </button>
                  )
                })}
              </div>
            </div>
          )
        })()}

      {/* Combat buttons (bottom-right) */}
      {isInCombatMode && combatState?.mode === 'declareAttackers' && (
        <div style={styles.combatButtonContainer}>
          {combatState.selectedAttackers.length === 0 ? (
            <>
              <button
                onClick={attackWithAll}
                disabled={combatState.validCreatures.length === 0}
                style={{
                  ...styles.floatingBarButton,
                  ...styles.combatActionButton,
                  backgroundColor: '#c62828',
                  border: '1px solid #ef5350',
                  opacity: combatState.validCreatures.length === 0 ? 0.5 : 1,
                }}
              >
                Attack All
              </button>
              <button
                onClick={confirmCombat}
                style={{
                  ...styles.floatingBarButton,
                  ...styles.combatPassButton,
                }}
              >
                Skip Attacking
              </button>
            </>
          ) : (
            <>
              {(() => {
                // Multiplayer: every attacker needs an explicit defender first.
                const awaitingDefender = isMulti &&
                  combatState.selectedAttackers.some((id) => !combatState.attackerTargets[id])
                return (
                  <button
                    onClick={confirmCombat}
                    disabled={awaitingDefender}
                    title={awaitingDefender ? 'Choose a defender for every attacker first' : undefined}
                    style={{
                      ...styles.floatingBarButton,
                      ...styles.combatActionButton,
                      backgroundColor: '#c62828',
                      border: '1px solid #ef5350',
                      opacity: awaitingDefender ? 0.5 : 1,
                      cursor: awaitingDefender ? 'not-allowed' : 'pointer',
                    }}
                  >
                    Attack with {combatState.selectedAttackers.length}
                  </button>
                )
              })()}
              <button
                onClick={clearAttackers}
                style={{
                  ...styles.floatingBarButton,
                  ...styles.combatActionButton,
                  backgroundColor: '#424242',
                  border: '1px solid #757575',
                }}
              >
                Clear Attackers
              </button>
            </>
          )}
        </div>
      )}

      {isInCombatMode && combatState?.mode === 'declareBlockers' && (
        <div style={styles.combatButtonContainer}>
          {Object.keys(combatState.blockerAssignments).length === 0 ? (
            <>
              <button
                onClick={confirmCombat}
                style={{
                  ...styles.floatingBarButton,
                  ...styles.combatPassButton,
                }}
              >
                No Blocks
              </button>
            </>
          ) : (
            <>
              <button
                onClick={confirmCombat}
                style={{
                  ...styles.floatingBarButton,
                  ...styles.combatActionButton,
                  backgroundColor: '#c62828',
                  border: '1px solid #ef5350',
                }}
              >
                Confirm Blocks
              </button>
              <button
                onClick={clearBlockerAssignments}
                style={{
                  ...styles.floatingBarButton,
                  ...styles.combatActionButton,
                  backgroundColor: '#424242',
                  border: '1px solid #757575',
                }}
              >
                Clear Blockers
              </button>
            </>
          )}
        </div>
      )}


      {/* Floating distribute bar (bottom-right) */}
      {isInDistributeMode && distributeState && (
        <div style={{
          ...styles.combatButtonContainer,
          flexDirection: 'column',
          gap: 8,
          alignItems: 'flex-end',
        }}>
          {(() => {
            const isPartial = distributeState.allowPartial === true
            const isPrevention = distributeState.prompt.toLowerCase().includes('prevention')
            const isCounters = distributeState.prompt.toLowerCase().includes('counter')
            const noun = isCounters ? 'counters' : isPrevention ? 'prevention' : 'damage'
            const confirmLabel = isCounters ? 'Confirm' : isPrevention ? 'Confirm Prevention' : 'Confirm Damage'
            const canConfirm = isPartial ? true : distributeRemaining === 0
            const isComplete = distributeRemaining === 0
            return (
              <>
                <div style={{
                  backgroundColor: isComplete ? 'rgba(22, 163, 74, 0.9)' : isPartial ? 'rgba(59, 130, 246, 0.9)' : 'rgba(220, 38, 38, 0.9)',
                  padding: responsive.isMobile ? '6px 12px' : '8px 16px',
                  borderRadius: 6,
                  border: isComplete ? '1px solid #4ade80' : isPartial ? '1px solid #60a5fa' : '1px solid #f87171',
                  textAlign: 'center',
                }}>
                  <div style={{
                    color: 'white',
                    fontSize: responsive.fontSize.small,
                    fontWeight: 600,
                  }}>
                    {isComplete
                      ? `All ${noun} allocated`
                      : `${distributeRemaining} ${noun} remaining`}
                  </div>
                  <div style={{
                    color: 'rgba(255, 255, 255, 0.7)',
                    fontSize: responsive.isMobile ? 10 : 11,
                    marginTop: 2,
                  }}>
                    {distributeState.prompt}
                  </div>
                </div>
                <button
                  onClick={confirmDistribute}
                  disabled={!canConfirm}
                  style={{
                    ...styles.combatButton,
                    ...(canConfirm ? styles.combatButtonPrimary : {}),
                    backgroundColor: canConfirm ? '#16a34a' : '#333',
                    color: canConfirm ? 'white' : '#666',
                    cursor: canConfirm ? 'pointer' : 'not-allowed',
                    borderColor: canConfirm ? '#4ade80' : '#555',
                  }}
                >
                  {confirmLabel}
                </button>
              </>
            )
          })()}
        </div>
      )}

      {/* Floating counter distribution panel (bottom-right) */}
      {isInCounterDistMode && counterDistributionState && (() => {
        const requiredTotal = counterDistributionState.requiredTotal
        const canConfirm = requiredTotal != null
          ? counterTotalAllocated === requiredTotal
          : counterTotalAllocated > 0
        const hasFixedTotal = requiredTotal != null
        const progressPct = hasFixedTotal
          ? Math.min(100, (counterTotalAllocated / requiredTotal) * 100)
          : 0
        const subtext = counterDistributionState.description
          ?? 'Remove +1/+1 counters from your creatures'
        const panelWidth = responsive.isMobile ? 220 : 240
        return (
          <div style={{
            position: 'fixed',
            bottom: 16,
            right: 16,
            width: panelWidth,
            zIndex: 110,
            display: 'flex',
            flexDirection: 'column',
            backgroundColor: 'rgba(17, 24, 39, 0.92)',
            border: '1px solid rgba(255, 255, 255, 0.1)',
            borderRadius: 8,
            boxShadow: '0 6px 18px rgba(0, 0, 0, 0.35)',
            overflow: 'hidden',
            fontFamily: 'inherit',
          }}>
            {/* Body */}
            <div style={{ padding: '10px 12px' }}>
              <div style={{
                color: '#cbd5e1',
                fontSize: responsive.isMobile ? 11 : 12,
                lineHeight: 1.35,
                marginBottom: 8,
              }}>
                {subtext}
              </div>

              <div style={{
                display: 'flex',
                alignItems: 'baseline',
                justifyContent: 'space-between',
                marginBottom: hasFixedTotal ? 5 : 0,
              }}>
                <span style={{
                  color: '#94a3b8',
                  fontSize: 10,
                  textTransform: 'uppercase',
                  letterSpacing: '0.05em',
                  fontWeight: 600,
                }}>
                  {hasFixedTotal ? 'Allocated' : 'X'}
                </span>
                <span style={{
                  color: canConfirm ? '#86efac' : '#fbbf24',
                  fontSize: responsive.isMobile ? 13 : 14,
                  fontWeight: 700,
                  fontVariantNumeric: 'tabular-nums',
                }}>
                  {hasFixedTotal
                    ? `${counterTotalAllocated} / ${requiredTotal}`
                    : counterTotalAllocated}
                </span>
              </div>

              {hasFixedTotal && (
                <div style={{
                  height: 3,
                  backgroundColor: 'rgba(255, 255, 255, 0.06)',
                  borderRadius: 2,
                  overflow: 'hidden',
                }}>
                  <div style={{
                    width: `${progressPct}%`,
                    height: '100%',
                    backgroundColor: canConfirm ? '#22c55e' : '#f59e0b',
                    transition: 'width 0.15s ease-out, background-color 0.15s',
                  }} />
                </div>
              )}
            </div>

            {/* Footer actions */}
            <div style={{
              display: 'flex',
              gap: 6,
              padding: '8px 12px',
              borderTop: '1px solid rgba(255, 255, 255, 0.06)',
              backgroundColor: 'rgba(0, 0, 0, 0.2)',
            }}>
              <button
                onClick={cancelCounterDistribution}
                style={{
                  flex: 1,
                  height: 30,
                  padding: '0 10px',
                  backgroundColor: 'transparent',
                  color: '#94a3b8',
                  border: '1px solid rgba(255, 255, 255, 0.12)',
                  borderRadius: 5,
                  fontWeight: 500,
                  fontSize: responsive.isMobile ? 12 : 13,
                  cursor: 'pointer',
                  transition: 'background-color 0.15s, color 0.15s',
                }}
                onMouseEnter={(e) => {
                  e.currentTarget.style.backgroundColor = 'rgba(255, 255, 255, 0.04)'
                  e.currentTarget.style.color = '#cbd5e1'
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.backgroundColor = 'transparent'
                  e.currentTarget.style.color = '#94a3b8'
                }}
              >
                Cancel
              </button>
              <button
                onClick={confirmCounterDistribution}
                disabled={!canConfirm}
                style={{
                  flex: 1,
                  height: 30,
                  padding: '0 10px',
                  backgroundColor: canConfirm ? 'rgba(22, 163, 74, 0.9)' : 'transparent',
                  color: canConfirm ? 'white' : '#64748b',
                  border: `1px solid ${canConfirm ? '#4ade80' : 'rgba(255, 255, 255, 0.08)'}`,
                  borderRadius: 5,
                  fontWeight: 600,
                  fontSize: responsive.isMobile ? 12 : 13,
                  cursor: canConfirm ? 'pointer' : 'not-allowed',
                  transition: 'background-color 0.15s, border-color 0.15s',
                }}
              >
                Confirm
              </button>
            </div>
          </div>
        )
      })()}

      {/* Action menu for selected card - hidden in spectator mode */}
      {!spectatorMode && <ActionMenu />}

      {/* Targeting overlay for spell/ability target selection */}
      {!spectatorMode && <TargetingOverlay />}

      {/* Mana color selection overlay */}
      {!spectatorMode && <ManaColorSelectionOverlay />}

      {/* Combat arrows for blocker assignments - rendered by SpectatorGameBoard in spectator mode to avoid stacking context issues */}
      {!spectatorMode && <CombatArrows />}

      {/* Targeting arrows for spells on the stack */}
      <TargetingArrows />

      {/* Dragged card overlay - hidden in spectator mode */}
      {!spectatorMode && <DraggedCardOverlay />}
      <CardPreview />
      {/* Hidden on phones (portrait): the bottom-left toggle steals the little
          horizontal space the hand has, and the expanded panel is unusable at
          that size anyway. */}
      {!spectatorMode && !responsive.isMobile && <GameLog />}
      {!spectatorMode && <ActiveYieldsPanel />}

      {/* Draw animations */}
      <DrawAnimations />

      {/* Damage animations */}
      <DamageAnimations />

      {/* Morph reveal animations */}
      <RevealAnimations />

      {/* Coin flip animations */}
      <CoinFlipAnimations />

      {/* Target reselection animations (Grip of Chaos, etc.) */}
      <TargetReselectedAnimations />

      {/* Mana selection controls (bottom-right, replaces pass button during mana selection mode) */}
      {isInManaSelectionMode && manaSelectionState && (
        <div style={{
          position: 'fixed',
          bottom: 16,
          right: 16,
          display: 'flex',
          flexDirection: 'column',
          gap: 8,
          alignItems: 'flex-end',
          zIndex: 100,
        }}>
          {/* Mana progress indicator */}
          {manaProgress && (
            <div style={{
              backgroundColor: 'rgba(0, 0, 0, 0.9)',
              border: `1px solid ${manaProgress.satisfied >= manaProgress.total ? 'rgba(74, 222, 128, 0.5)' : 'rgba(255, 255, 255, 0.2)'}`,
              borderRadius: 8,
              padding: responsive.isMobile ? '8px 12px' : '10px 16px',
              display: 'flex',
              alignItems: 'center',
              gap: 10,
            }}>
              {manaProgress.entries.map(([symbol, required]) => {
                const fulfilled = manaProgress.colorSatisfied?.[symbol] ?? 0
                return (
                  <div key={symbol} style={{ display: 'flex', alignItems: 'center', gap: 3 }}>
                    <ManaSymbol symbol={symbol} size={18} />
                    <span style={{
                      color: fulfilled >= required ? '#4ade80' : fulfilled > 0 ? '#fbbf24' : '#888',
                      fontWeight: 600,
                      fontSize: responsive.fontSize.normal,
                    }}>
                      {fulfilled}/{required}
                    </span>
                  </div>
                )
              })}
              <span style={{
                color: manaProgress.satisfied >= manaProgress.total ? '#4ade80' : '#888',
                fontSize: responsive.fontSize.small,
                marginLeft: 4,
              }}>
                ({manaProgress.satisfied}/{manaProgress.total})
              </span>
            </div>
          )}
          {/* Confirm / Cancel buttons */}
          <div style={{ display: 'flex', gap: 8 }}>
            <button
              onClick={cancelManaSelection}
              style={{
                padding: responsive.isMobile ? '10px 20px' : '12px 24px',
                fontSize: responsive.fontSize.normal,
                fontWeight: 600,
                backgroundColor: 'rgba(40, 40, 40, 0.9)',
                color: '#ccc',
                border: '2px solid #555',
                borderRadius: 8,
                cursor: 'pointer',
              }}
            >
              Cancel
            </button>
            <button
              onClick={handleConfirmManaSelection}
              style={{
                padding: responsive.isMobile ? '10px 20px' : '12px 24px',
                fontSize: responsive.fontSize.normal,
                fontWeight: 600,
                backgroundColor: 'rgba(22, 101, 52, 0.9)',
                color: '#4ade80',
                border: '2px solid #4ade80',
                borderRadius: 8,
                cursor: 'pointer',
              }}
            >
              Confirm
            </button>
          </div>
        </div>
      )}
    </div>
    </ResponsiveContext.Provider>
    </RenderProfiler>
  )
}
