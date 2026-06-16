import { useEffect, useMemo, useRef, useState } from 'react'
import type React from 'react'
import { useGameStore } from '@/store/gameStore'
import {
  selectGameState,
  selectTeamMap,
  useOpponents,
  useViewedOpponent,
  useViewingPlayer,
  useIdentityColor,
  useIsTeamGame,
  useIsSharedLifeTeamGame,
  useViewerTeamIndex,
} from '@/store/selectors'
import { teamColor, type SeatColor } from '@/styles/seatColors'
import type { ClientCard, ClientPlayer } from '@/types'
import { useResponsiveContext } from './board/shared'

/**
 * Chip dimensions by screen size. The rail is a fixed-width column, so every chip shares one
 * size; large desktops get noticeably bigger chips (the small default felt cramped there).
 */
function chipSizing(responsive: { isMobile: boolean; isTablet: boolean; isShortDesktop: boolean }) {
  const compact = responsive.isMobile
  const large = !responsive.isMobile && !responsive.isTablet && !responsive.isShortDesktop
  return {
    width: compact ? 150 : large ? 224 : 188,
    height: compact ? 22 : large ? 32 : 26,
    padX: compact ? 7 : large ? 14 : 11,
    gap: compact ? 4 : large ? 9 : 7,
    dot: large ? 12 : 10,
    name: large ? 14 : 12,
    life: compact ? 11 : large ? 15 : 13,
    heart: compact ? 10 : large ? 14 : 12,
    hand: compact ? 10 : large ? 13 : 12,
    marker: compact ? 9 : large ? 13 : 11,
  }
}

/**
 * The opponent rail — the multiplayer overview guarantee. One chip per opponent,
 * always visible: seat color, name, life (the floating ±delta anchor), hand count,
 * poison, commander-damage warning, active-turn ring, priority dot, deciding
 * spinner, attention pulses. Chips are also the player-level click target:
 * - click → slide that opponent's board into view (pins; re-click unpins)
 * - crosshair badge → target the player (never mixed with the view-switch click)
 * - during attack declaration with attackers selected → assign them this defender
 * - tombstone (grayed, skull) once the player has left the game
 *
 * Renders only in games with more than two players — the 2-player layout is sacred.
 */
export function OpponentRail({
  spectatorMode = false,
}: {
  spectatorMode?: boolean
}) {
  const responsive = useResponsiveContext()
  const opponents = useOpponents()
  const viewedOpponent = useViewedOpponent()
  const self = useViewingPlayer()
  const viewPinned = useGameStore((state) => state.viewPinned)
  const followAction = useGameStore((state) => state.followAction)
  const toggleFollowAction = useGameStore((state) => state.toggleFollowAction)

  // Two-Headed Giant (CR 810): when a team game is in progress, the rail splits into two
  // team sections — your team (you + ally) and the opposing team — colored by team with one
  // shared-life header each. Spectators (no "you") keep the flat per-seat rail.
  const isTeamGame = useIsTeamGame()
  const teamMap = useGameStore(selectTeamMap)
  const viewerTeam = useViewerTeamIndex()
  const teamMode = isTeamGame && viewerTeam != null && !spectatorMode
  // Only 2HG pools life per team; Team vs. Team groups by team but shows per-player life on chips.
  const sharedLife = useIsSharedLifeTeamGame()

  if (opponents.length <= 1) return null

  const teammates = teamMode ? opponents.filter((o) => teamMap[o.playerId] === viewerTeam) : []
  const enemies = teamMode ? opponents.filter((o) => teamMap[o.playerId] !== viewerTeam) : opponents
  const enemyTeam = viewerTeam === 0 ? 1 : 0
  // Shared life is the same on every living member of a team (the engine pools it), so read it
  // from the first living member — or any member if the whole team is gone (game's already over).
  const yourTeamLife = self?.life ?? 0
  const enemyTeamLife = (enemies.find((e) => !e.hasLost) ?? enemies[0])?.life ?? 0

  // Vertical column in the top-left corner, tucked under the fullscreen button (top: 8/12,
  // ~36px tall). A column reads as a clear turn-order list and leaves the board its full height.
  const top = responsive.isMobile ? 46 : 54
  const left = responsive.isMobile ? 8 : 12
  const columnWidth = chipSizing(responsive).width

  return (
    <div
      data-opponent-rail
      style={{
        position: 'fixed',
        top,
        left,
        // Fixed width so every chip is the same size regardless of name length — a ragged
        // column reads as messy. Names truncate with ellipsis inside this width.
        width: columnWidth,
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'stretch',
        gap: 5,
        zIndex: 120,
        pointerEvents: 'none',
        maxHeight: `calc(100vh - ${top + 16}px)`,
      }}
    >
      <style>{`
        @keyframes railChipPulse {
          0% { box-shadow: 0 0 0 0 var(--pulse-color), 0 0 10px var(--pulse-color); }
          100% { box-shadow: 0 0 0 10px rgba(0,0,0,0), 0 0 0 rgba(0,0,0,0); }
        }
        @keyframes railSpin { to { transform: rotate(360deg); } }
        @keyframes railHalo {
          0%, 100% { box-shadow: 0 0 4px 1px var(--halo-color); }
          50% { box-shadow: 0 0 10px 3px var(--halo-color); }
        }
        @keyframes seatEdgeFlash {
          0% { opacity: 0.9; }
          100% { opacity: 0; }
        }
        /* Active-turn ring: a steady, clearly-visible pulse around whoever's turn it is. */
        @keyframes railTurnRing {
          0%, 100% { box-shadow: 0 0 0 2px var(--turn-color), 0 0 8px 1px var(--turn-glow); }
          50% { box-shadow: 0 0 0 3px var(--turn-color), 0 0 14px 4px var(--turn-glow); }
        }
        @keyframes railTurnArrow {
          0%, 100% { transform: translateX(0); opacity: 0.85; }
          50% { transform: translateX(2px); opacity: 1; }
        }
      `}</style>
      {teamMode ? (
        <>
          {/* Your team: shared-life header (2HG only), then your "you" chip + your ally chip(s). */}
          <TeamRailSection label="Your Team" color={teamColor(viewerTeam!)} life={sharedLife ? yourTeamLife : null}>
            {self && <SelfRailChip self={self} />}
            {teammates.map((opponent) => (
              <RailChip
                key={opponent.playerId}
                opponent={opponent}
                isViewed={viewedOpponent?.playerId === opponent.playerId}
                viewPinned={viewPinned}
                spectatorMode={spectatorMode}
                isAlly
              />
            ))}
          </TeamRailSection>
          {/* Opposing team: their shared life (2HG) above their seats, or per-player life on each. */}
          <TeamRailSection label="Opponents" color={teamColor(enemyTeam)} life={sharedLife ? enemyTeamLife : null}>
            {enemies.map((opponent) => (
              <RailChip
                key={opponent.playerId}
                opponent={opponent}
                isViewed={viewedOpponent?.playerId === opponent.playerId}
                viewPinned={viewPinned}
                spectatorMode={spectatorMode}
              />
            ))}
          </TeamRailSection>
        </>
      ) : (
        <>
          {/* "You" chip first, so the full turn order — and whose turn it is — reads left to
              right across the rail. Informational only: your board is always at the bottom, and
              your life/targeting anchor stays on the center-HUD orb. Hidden when spectating. */}
          {!spectatorMode && self && <SelfRailChip self={self} />}
          {opponents.map((opponent) => (
            <RailChip
              key={opponent.playerId}
              opponent={opponent}
              isViewed={viewedOpponent?.playerId === opponent.playerId}
              viewPinned={viewPinned}
              spectatorMode={spectatorMode}
            />
          ))}
        </>
      )}
      {!spectatorMode && (
        <>
          {/* Divider — the Follow control is a camera *setting*, not a player, so set it apart
              from the chip list above. */}
          <div aria-hidden style={{ alignSelf: 'stretch', height: 1, margin: '4px 6px 2px', background: 'rgba(255, 255, 255, 0.1)' }} />
          <button
            onClick={toggleFollowAction}
            title={
              followAction
                ? 'Follow the action: the view slides to the active board automatically. Click for a manual camera.'
                : 'Manual camera: the view only moves when you switch boards. Click to follow the action.'
            }
            style={{
              // Deliberately unlike a chip: rounded-rect (not a full pill), smaller, narrower
              // than the fixed-width chips (flex-start), with an eye icon + explicit ON/OFF.
              alignSelf: 'flex-start',
              pointerEvents: 'auto',
              height: 20,
              padding: '0 9px',
              display: 'inline-flex',
              alignItems: 'center',
              gap: 5,
              borderRadius: 6,
              border: `1px solid ${followAction ? 'rgba(110, 200, 255, 0.55)' : '#3a3a44'}`,
              background: followAction ? 'rgba(20, 50, 80, 0.7)' : 'rgba(18, 18, 26, 0.7)',
              color: followAction ? '#9fd8ff' : '#888',
              fontSize: 9,
              fontWeight: 700,
              letterSpacing: '0.08em',
              textTransform: 'uppercase',
              cursor: 'pointer',
              whiteSpace: 'nowrap',
            }}
          >
            <span aria-hidden style={{ fontSize: 11 }}>{followAction ? '◉' : '○'}</span>
            Follow
            <span style={{ fontWeight: 800, color: followAction ? '#cdebff' : '#666' }}>
              {followAction ? 'On' : 'Off'}
            </span>
          </button>
        </>
      )}
    </div>
  )
}

/**
 * A team grouping in the rail: a team-colored banner with the team label, above that team's member
 * chips. In Two-Headed Giant (CR 810) it also carries the team's single shared [life] total — and
 * the member chips drop their own (identical) life number. In Team vs. Team (CR 808) life is
 * per-player, so [life] is null here and each chip shows its own.
 */
function TeamRailSection({
  label,
  color,
  life,
  children,
}: {
  label: string
  color: SeatColor
  life?: number | null
  children: React.ReactNode
}) {
  const lifeDanger = life != null && life <= 5
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 5, pointerEvents: 'none' }}>
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: 6,
          padding: '2px 10px',
          borderRadius: 6,
          border: `1px solid ${color.base}`,
          borderLeft: `4px solid ${color.base}`,
          background: `linear-gradient(90deg, ${color.soft}, rgba(10, 12, 20, 0.55))`,
          color: color.bright,
          userSelect: 'none',
        }}
      >
        <span
          style={{
            flex: 1,
            minWidth: 0,
            overflow: 'hidden',
            textOverflow: 'ellipsis',
            whiteSpace: 'nowrap',
            fontSize: 10,
            fontWeight: 800,
            letterSpacing: '0.08em',
            textTransform: 'uppercase',
          }}
        >
          {label}
        </span>
        {life != null && (
          <span
            title="Shared team life"
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: 3,
              flexShrink: 0,
              fontSize: 14,
              fontWeight: 800,
              fontVariantNumeric: 'tabular-nums',
              color: lifeDanger ? '#ff5555' : '#ffffff',
            }}
          >
            <span aria-hidden style={{ color: '#ff6b6b', fontSize: 12 }}>❤</span>
            {life}
          </span>
        )}
      </div>
      {children}
    </div>
  )
}

/**
 * The viewing player's own chip in the rail — the multiplayer overview's "you" entry, so the
 * whole table (and whose turn it is) is visible in one strip. Deliberately a slim, informational
 * subset of [RailChip]: seat color, name, life, hand, poison, plus the active-turn ring and
 * priority/deciding indicators. It is not a view-switch / defender / target anchor — those stay
 * on your board and center-HUD orb — so it carries no `data-life-id`.
 */
function SelfRailChip({ self }: { self: ClientPlayer }) {
  const responsive = useResponsiveContext()
  const gameState = useGameStore(selectGameState)
  const opponentDecisionStatus = useGameStore((state) => state.opponentDecisionStatus)

  const playerId = self.playerId
  const seat = useIdentityColor(playerId)
  // Only when life is shared (2HG) does the team header carry it, so the chip drops its own
  // (identical) number; in Team vs. Team each chip keeps its own life. Hand/poison/turn/priority
  // signals are kept either way.
  const teamMode = useIsSharedLifeTeamGame()

  const isActiveTurn = gameState?.activePlayerId === playerId && !self.hasLost
  const hasPriority = gameState?.priorityPlayerId === playerId && !self.hasLost
  const isDeciding = opponentDecisionStatus?.playerId === playerId

  const compact = responsive.isMobile
  const sz = chipSizing(responsive)
  const tomb = self.hasLost
  const lifeDanger = self.life <= 5

  const borderColor = tomb ? '#3a3a44' : seat.base
  const background = tomb
    ? 'rgba(18, 18, 24, 0.85)'
    : `linear-gradient(180deg, ${seat.soft}, rgba(10, 12, 20, 0.92))`

  return (
    <div style={{ position: 'relative', pointerEvents: 'auto', width: '100%' }}>
      <div
        title={`You — Life ${self.life} · Hand ${self.handSize}${self.hasLost ? ' · Eliminated' : ''}`}
        style={{
          position: 'relative',
          display: 'flex',
          alignItems: 'center',
          width: '100%',
          boxSizing: 'border-box',
          gap: sz.gap,
          height: sz.height,
          padding: `0 ${sz.padX}px`,
          borderRadius: 999,
          border: `2px solid ${borderColor}`,
          background,
          color: tomb ? '#666' : '#dde3f0',
          userSelect: 'none',
          whiteSpace: 'nowrap',
          filter: tomb ? 'grayscale(1)' : 'none',
          opacity: tomb ? 0.6 : 1,
          // Active turn → animated ring (same signal as the opponent chips).
          ...(isActiveTurn && !tomb
            ? {
                animation: 'railTurnRing 1.4s ease-in-out infinite',
                ['--turn-color' as string]: seat.bright,
                ['--turn-glow' as string]: seat.soft,
              }
            : {}),
        }}
      >
        {/* Turn marker — triangle in front when it's your turn. */}
        {isActiveTurn && !tomb && (
          <span
            aria-hidden
            title="Your turn"
            style={{
              color: seat.bright,
              fontSize: sz.marker,
              lineHeight: 1,
              flexShrink: 0,
              animation: 'railTurnArrow 1s ease-in-out infinite',
              textShadow: `0 0 5px ${seat.soft}`,
            }}
          >
            ▶
          </span>
        )}
        {/* Seat dot / skull */}
        <span
          aria-hidden
          style={{
            display: 'inline-flex',
            alignItems: 'center',
            justifyContent: 'center',
            width: sz.dot,
            height: sz.dot,
            borderRadius: '50%',
            background: tomb ? 'transparent' : seat.base,
            boxShadow: tomb ? 'none' : `0 0 5px ${seat.base}`,
            fontSize: 10,
            flexShrink: 0,
          }}
        >
          {tomb ? '💀' : ''}
        </span>

        {!compact && (
          <span style={{ flex: 1, minWidth: 0, display: 'flex', alignItems: 'baseline', gap: 5, overflow: 'hidden' }}>
            <span
              style={{
                overflow: 'hidden',
                textOverflow: 'ellipsis',
                whiteSpace: 'nowrap',
                fontSize: sz.name,
                fontWeight: 700,
                letterSpacing: '0.02em',
                color: tomb ? '#666' : seat.bright,
              }}
            >
              {self.name}
            </span>
            <span aria-hidden style={{ fontSize: 9, opacity: 0.75, fontWeight: 600, flexShrink: 0 }}>YOU</span>
          </span>
        )}

        {/* Life — hidden in team mode (the team header carries the shared total). */}
        {!teamMode && (
          <span
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: 3,
              flexShrink: 0,
              marginLeft: compact ? 'auto' : 0,
              fontSize: sz.life,
              fontWeight: 800,
              fontVariantNumeric: 'tabular-nums',
              color: tomb ? '#555' : lifeDanger ? '#ff5555' : '#ffffff',
              textDecoration: tomb ? 'line-through' : 'none',
            }}
          >
            <span aria-hidden style={{ color: tomb ? '#555' : '#ff6b6b', fontSize: sz.heart }}>❤</span>
            {self.life}
          </span>
        )}

        {/* Hand count */}
        {!tomb && (
          <span
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: 3,
              flexShrink: 0,
              // On phones the life number normally provides the right-push; with it hidden in
              // team mode the hand count takes over the auto margin.
              marginLeft: compact && teamMode ? 'auto' : 0,
              fontSize: sz.hand,
              fontWeight: 700,
              fontVariantNumeric: 'tabular-nums',
              color: '#9fb0d0',
            }}
          >
            <HandCountIcon />
            {self.handSize}
          </span>
        )}

        {/* Poison */}
        {!tomb && self.poisonCounters > 0 && (
          <span
            title={`${self.poisonCounters}/10 poison counters`}
            style={{ fontSize: compact ? 10 : 11, fontWeight: 800, color: '#71f5a7' }}
          >
            ☠{self.poisonCounters}
          </span>
        )}

        {/* Deciding spinner / priority dot (the turn ring is the border glow) */}
        {isDeciding ? (
          <span
            aria-hidden
            title="Deciding"
            style={{
              width: 10,
              height: 10,
              borderRadius: '50%',
              border: '2px solid rgba(255, 193, 7, 0.3)',
              borderTopColor: '#ffc107',
              animation: 'railSpin 0.9s linear infinite',
              flexShrink: 0,
            }}
          />
        ) : hasPriority && !isActiveTurn ? (
          <span
            aria-hidden
            title="You hold priority"
            style={{
              width: 6,
              height: 6,
              borderRadius: '50%',
              background: '#ffc107',
              boxShadow: '0 0 5px rgba(255, 193, 7, 0.8)',
              flexShrink: 0,
            }}
          />
        ) : null}
      </div>
    </div>
  )
}

/** Tiny card-back glyph for the hand count (cross-platform safe, unlike 🂠). */
function HandCountIcon({ color = '#8899bb' }: { color?: string }) {
  return (
    <span
      aria-hidden
      style={{
        display: 'inline-block',
        width: 8,
        height: 11,
        borderRadius: 2,
        border: `1px solid ${color}`,
        background: 'linear-gradient(135deg, rgba(120,140,200,0.35), rgba(40,50,90,0.6))',
        flexShrink: 0,
      }}
    />
  )
}

function RailChip({
  opponent,
  isViewed,
  viewPinned,
  spectatorMode,
  isAlly = false,
}: {
  opponent: ClientPlayer
  isViewed: boolean
  viewPinned: boolean
  spectatorMode: boolean
  /** Two-Headed Giant: this seat is the viewing player's teammate (ally treatment, no own life). */
  isAlly?: boolean
}) {
  const responsive = useResponsiveContext()
  const gameState = useGameStore(selectGameState)
  const viewOpponent = useGameStore((state) => state.viewOpponent)
  const unpinView = useGameStore((state) => state.unpinView)

  // Targeting plumbing — mirrors LifeDisplay's player-click handling so a rail
  // chip is a full substitute for the absorbed opponent life orb.
  const targetingState = useGameStore((state) => state.targetingState)
  const pendingDecision = useGameStore((state) => state.pendingDecision)
  const addTarget = useGameStore((state) => state.addTarget)
  const removeTarget = useGameStore((state) => state.removeTarget)
  const submitTargetsDecision = useGameStore((state) => state.submitTargetsDecision)
  const decisionSelectionState = useGameStore((state) => state.decisionSelectionState)
  const toggleDecisionSelection = useGameStore((state) => state.toggleDecisionSelection)
  const distributeState = useGameStore((state) => state.distributeState)
  const incrementDistribute = useGameStore((state) => state.incrementDistribute)
  const decrementDistribute = useGameStore((state) => state.decrementDistribute)

  // Combat (defender assignment + planeswalker flyout)
  const combatState = useGameStore((state) => state.combatState)
  const assignDefender = useGameStore((state) => state.assignDefenderToSelectedAttackers)
  const draggingAttackerId = useGameStore((state) => state.draggingAttackerId)

  const opponentDecisionStatus = useGameStore((state) => state.opponentDecisionStatus)

  const playerId = opponent.playerId
  const seat = useIdentityColor(playerId)
  // Shared-life team game (2HG): the life is in the team header, so the chip drops its own number.
  // Team vs. Team keeps per-player life on the chip.
  const teamMode = useIsSharedLifeTeamGame()

  const isActiveTurn = gameState?.activePlayerId === playerId && !opponent.hasLost
  const hasPriority = gameState?.priorityPlayerId === playerId && !opponent.hasLost
  const isDeciding = opponentDecisionStatus?.playerId === playerId

  /* ── Targeting state (player as target) ──────────────────────────────── */
  const isValidTargetingTarget = targetingState?.validTargets.includes(playerId) ?? false
  const isTargetingSelected = targetingState?.selectedTargets.includes(playerId) ?? false
  const isChooseTargetsDecision = pendingDecision?.type === 'ChooseTargetsDecision'
  const isSingleRequirementDecision = isChooseTargetsDecision && pendingDecision.targetRequirements.length === 1
  const decisionLegalTargets = isSingleRequirementDecision ? (pendingDecision.legalTargets[0] ?? []) : []
  const isValidDecisionTarget = decisionLegalTargets.includes(playerId)
  const isValidDecisionSelection = decisionSelectionState?.validOptions.includes(playerId) ?? false
  const isSelectedDecisionOption = decisionSelectionState?.selectedOptions.includes(playerId) ?? false
  const isPlayerTargetable = isValidTargetingTarget || isValidDecisionTarget || isValidDecisionSelection
  const isPlayerTargetSelected = isTargetingSelected || isSelectedDecisionOption

  const isDistributeTarget = distributeState?.targets.includes(playerId) ?? false
  const distributeAllocated = isDistributeTarget ? (distributeState?.distribution[playerId] ?? 0) : 0
  const distributeTotal = distributeState
    ? Object.values(distributeState.distribution).reduce((sum, v) => sum + v, 0)
    : 0
  const distributeRemaining = distributeState ? distributeState.totalAmount - distributeTotal : 0

  /* ── "Contains targets" halo: their board (off-screen or not) holds valid
        targets of the in-progress selection ─────────────────────────────── */
  const boardHasTargets = useMemo(() => {
    if (!gameState || opponent.hasLost) return false
    const ids = targetingState?.validTargets ?? decisionSelectionState?.validOptions
    if (!ids || ids.length === 0) return false
    return ids.some((id) => gameState.cards[id]?.controllerId === playerId)
  }, [gameState, targetingState?.validTargets, decisionSelectionState?.validOptions, playerId, opponent.hasLost])

  /* ── Combat: this chip is a defender drop/click target while declaring ── */
  const declaringAttackers = combatState?.mode === 'declareAttackers'
  const hasSelectedAttackers = declaringAttackers && (combatState?.selectedAttackers.length ?? 0) > 0
  const isDefenderTarget =
    !spectatorMode &&
    !opponent.hasLost &&
    declaringAttackers &&
    (combatState?.validAttackTargets.includes(playerId) ?? false)
  const isDefenderAssignTarget = isDefenderTarget && (hasSelectedAttackers || draggingAttackerId !== null)
  const assignedCount = useMemo(() => {
    if (!declaringAttackers || !combatState || !gameState) return 0
    return Object.entries(combatState.attackerTargets).filter(([, targetId]) => {
      if (targetId === playerId) return true
      return gameState.cards[targetId]?.controllerId === playerId
    }).length
  }, [declaringAttackers, combatState, gameState, playerId])

  // Planeswalker flyout: this opponent's planeswalkers that are legal attack targets.
  const attackablePlaneswalkers = useMemo<readonly ClientCard[]>(() => {
    if (!declaringAttackers || !combatState || !gameState) return []
    return combatState.validAttackTargets
      .map((id) => gameState.cards[id])
      .filter((c): c is ClientCard => !!c && c.controllerId === playerId)
  }, [declaringAttackers, combatState, gameState, playerId])
  const [hovered, setHovered] = useState(false)

  /* ── Attention pulse on life / hand changes ─────────────────────────── */
  const prevLifeRef = useRef(opponent.life)
  const prevHandRef = useRef(opponent.handSize)
  const [pulse, setPulse] = useState<{ color: string; key: number } | null>(null)
  useEffect(() => {
    const lifeDelta = opponent.life - prevLifeRef.current
    const handChanged = opponent.handSize !== prevHandRef.current
    prevLifeRef.current = opponent.life
    prevHandRef.current = opponent.handSize
    if (lifeDelta < 0) {
      setPulse({ color: 'rgba(255, 68, 68, 0.55)', key: Date.now() })
    } else if (lifeDelta > 0 || handChanged) {
      setPulse({ color: seat.soft, key: Date.now() })
    }
  }, [opponent.life, opponent.handSize, seat.soft])

  const worstCommanderDamage = useMemo(() => {
    const entries = opponent.commanderDamage ?? []
    if (entries.length === 0) return null
    return entries.reduce((worst, e) => (e.amount > worst.amount ? e : worst))
  }, [opponent.commanderDamage])

  const handleChipClick = () => {
    if (spectatorMode) {
      if (!opponent.hasLost) viewOpponent(playerId)
      return
    }
    // Defender assignment takes precedence while declaring attackers with a
    // selection — assigning is an input action, not a view change.
    if (isDefenderTarget && hasSelectedAttackers) {
      assignDefender(playerId)
      return
    }
    if (opponent.hasLost) return
    if (isViewed) {
      // Re-click the viewed chip toggles the pin (Esc also unpins).
      if (viewPinned) unpinView()
      else viewOpponent(playerId)
      return
    }
    // A view change never cancels an in-progress selection.
    viewOpponent(playerId)
  }

  // Mirrors LifeDisplay.handleClick — the chip's crosshair badge is the
  // player-level target click in multiplayer.
  const handleTargetClick = (e: React.MouseEvent) => {
    e.stopPropagation()
    if (targetingState) {
      if (isTargetingSelected) {
        removeTarget(playerId)
        return
      }
      if (isValidTargetingTarget) {
        addTarget(playerId)
        return
      }
    }
    if (isChooseTargetsDecision && isValidDecisionTarget) {
      submitTargetsDecision({ 0: [playerId] })
      return
    }
    if (isValidDecisionSelection) {
      toggleDecisionSelection(playerId)
    }
  }

  const compact = responsive.isMobile
  const sz = chipSizing(responsive)
  const tomb = opponent.hasLost

  const borderColor = tomb
    ? '#3a3a44'
    : isDefenderAssignTarget
      ? '#ff4444'
      : isViewed
        ? seat.bright
        : seat.base
  const background = tomb
    ? 'rgba(18, 18, 24, 0.85)'
    : isViewed
      ? `linear-gradient(180deg, ${seat.soft}, rgba(10, 12, 20, 0.92))`
      : 'rgba(10, 12, 20, 0.88)'

  // Active turn is shown as an animated ring (see the chip's `animation` below); only the
  // defender-assign highlight uses a static box-shadow here.
  const ringShadow = isDefenderAssignTarget ? '0 0 12px rgba(255, 68, 68, 0.6)' : ''

  const lifeDanger = opponent.life <= 5

  return (
    <div style={{ position: 'relative', pointerEvents: 'auto', width: '100%' }}>
      <div
        data-rail-chip={playerId}
        // The viewed opponent's full-size life orb (center HUD) carries the
        // player anchors while their board is in view — the chip only anchors
        // arrows / damage floats / target clicks for off-screen opponents.
        // Duplicate anchors would make querySelector pick whichever comes
        // first in the DOM.
        {...(!isViewed
          ? {
              'data-player-id': playerId,
              'data-life-id': playerId,
              'data-life-display': playerId,
            }
          : {})}
        role="button"
        title={chipTitle(opponent)}
        onClick={handleChipClick}
        onMouseEnter={() => setHovered(true)}
        onMouseLeave={() => setHovered(false)}
        style={{
          position: 'relative',
          display: 'flex',
          alignItems: 'center',
          width: '100%',
          boxSizing: 'border-box',
          gap: sz.gap,
          height: sz.height,
          padding: `0 ${sz.padX}px`,
          borderRadius: 999,
          border: `${isViewed ? 2 : 1}px solid ${borderColor}`,
          background,
          color: tomb ? '#666' : '#dde3f0',
          cursor: tomb && !spectatorMode ? 'default' : 'pointer',
          userSelect: 'none',
          whiteSpace: 'nowrap',
          filter: tomb ? 'grayscale(1)' : 'none',
          opacity: tomb ? 0.6 : 1,
          transition: 'border-color 150ms, background 150ms, opacity 200ms',
          ...(ringShadow ? { boxShadow: ringShadow } : {}),
          // Active turn wins the animation slot (clearest signal); otherwise the
          // "board has targets" halo. Both drive box-shadow, so only one can run.
          ...(isActiveTurn && !tomb
            ? {
                animation: 'railTurnRing 1.4s ease-in-out infinite',
                ['--turn-color' as string]: seat.bright,
                ['--turn-glow' as string]: seat.soft,
              }
            : boardHasTargets && !isViewed
              ? {
                  animation: 'railHalo 1.2s ease-in-out infinite',
                  ['--halo-color' as string]: 'rgba(0, 187, 255, 0.7)',
                }
              : {}),
        }}
      >
        {/* Attention pulse overlay — keyed so each event restarts the animation
            without remounting the chip (which would drop hover state). */}
        {pulse && (
          <span
            key={pulse.key}
            aria-hidden
            style={{
              position: 'absolute',
              inset: 0,
              borderRadius: 999,
              pointerEvents: 'none',
              animation: 'railChipPulse 650ms ease-out',
              ['--pulse-color' as string]: pulse.color,
            }}
          />
        )}
        {/* Turn marker — a small triangle in front of whoever's turn it is. */}
        {isActiveTurn && !tomb && (
          <span
            aria-hidden
            title="Active turn"
            style={{
              color: seat.bright,
              fontSize: sz.marker,
              lineHeight: 1,
              flexShrink: 0,
              animation: 'railTurnArrow 1s ease-in-out infinite',
              textShadow: `0 0 5px ${seat.soft}`,
            }}
          >
            ▶
          </span>
        )}
        {/* Viewed marker / seat dot / skull */}
        <span
          aria-hidden
          style={{
            display: 'inline-flex',
            alignItems: 'center',
            justifyContent: 'center',
            width: sz.dot,
            height: sz.dot,
            borderRadius: '50%',
            background: tomb ? 'transparent' : seat.base,
            boxShadow: tomb ? 'none' : `0 0 5px ${seat.base}`,
            fontSize: 10,
            flexShrink: 0,
          }}
        >
          {tomb ? '💀' : ''}
        </span>

        {/* Name (hidden on phones — the seat dot + position carries identity). Flexes to fill
            the fixed chip width and truncates, so every chip is the same size. An ally (your
            2HG teammate) gets an "ALLY" tag so it never reads as an opponent. */}
        {!compact && (
          <span style={{ flex: 1, minWidth: 0, display: 'flex', alignItems: 'baseline', gap: 5, overflow: 'hidden' }}>
            <span
              style={{
                overflow: 'hidden',
                textOverflow: 'ellipsis',
                whiteSpace: 'nowrap',
                fontSize: sz.name,
                fontWeight: 700,
                letterSpacing: '0.02em',
                color: tomb ? '#666' : seat.bright,
              }}
            >
              {opponent.name}
            </span>
            {isAlly && !tomb && (
              <span aria-hidden title="Your teammate" style={{ fontSize: 9, opacity: 0.85, fontWeight: 700, flexShrink: 0, color: seat.bright }}>ALLY</span>
            )}
            {isViewed && viewPinned && !spectatorMode && (
              <span aria-hidden title="Pinned — follow-the-action paused" style={{ flexShrink: 0 }}>📌</span>
            )}
          </span>
        )}

        {/* Life — hidden in team mode (the team header carries the shared total). The floating
            ±life delta anchor is the chip div itself (data-life-display), not this span. */}
        {!teamMode && (
          <span
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: 3,
              flexShrink: 0,
              marginLeft: compact ? 'auto' : 0,
              fontSize: sz.life,
              fontWeight: 800,
              fontVariantNumeric: 'tabular-nums',
              color: tomb ? '#555' : lifeDanger ? '#ff5555' : '#ffffff',
              textDecoration: tomb ? 'line-through' : 'none',
            }}
          >
            <span aria-hidden style={{ color: tomb ? '#555' : '#ff6b6b', fontSize: sz.heart }}>❤</span>
            {opponent.life}
          </span>
        )}

        {/* Hand count */}
        {!tomb && (
          <span
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: 3,
              flexShrink: 0,
              // On phones the life number normally provides the right-push; with it hidden in
              // team mode the hand count takes over the auto margin.
              marginLeft: compact && teamMode ? 'auto' : 0,
              fontSize: sz.hand,
              fontWeight: 700,
              fontVariantNumeric: 'tabular-nums',
              color: '#9fb0d0',
            }}
          >
            <HandCountIcon />
            {opponent.handSize}
          </span>
        )}

        {/* Poison */}
        {!tomb && opponent.poisonCounters > 0 && (
          <span
            title={`${opponent.poisonCounters}/10 poison counters`}
            style={{ fontSize: compact ? 10 : 11, fontWeight: 800, color: '#71f5a7' }}
          >
            ☠{opponent.poisonCounters}
          </span>
        )}

        {/* Commander damage (worst pair; full rows in the tooltip) */}
        {!tomb && worstCommanderDamage && worstCommanderDamage.amount > 0 && (
          <span
            title={(opponent.commanderDamage ?? [])
              .map((e) => `${e.commanderName}: ${e.amount}/${e.threshold}`)
              .join('\n')}
            style={{
              fontSize: compact ? 10 : 11,
              fontWeight: 800,
              fontVariantNumeric: 'tabular-nums',
              color:
                worstCommanderDamage.threshold - worstCommanderDamage.amount <= 3
                  ? '#ff5555'
                  : worstCommanderDamage.threshold - worstCommanderDamage.amount <= 7
                    ? '#ffae7a'
                    : '#b9925f',
            }}
          >
            ⚔{worstCommanderDamage.amount}/{worstCommanderDamage.threshold}
          </span>
        )}

        {/* Attack assignment count while declaring attackers */}
        {!tomb && declaringAttackers && assignedCount > 0 && (
          <span
            title={`${assignedCount} attacker${assignedCount === 1 ? '' : 's'} assigned to ${opponent.name}`}
            style={{ fontSize: compact ? 10 : 11, fontWeight: 800, color: '#ff8888' }}
          >
            ⚔︎→{assignedCount}
          </span>
        )}

        {/* Deciding spinner / priority dot / (turn ring is the border glow) */}
        {isDeciding ? (
          <span
            aria-hidden
            title={opponentDecisionStatus?.displayText ?? 'Deciding'}
            style={{
              width: 10,
              height: 10,
              borderRadius: '50%',
              border: '2px solid rgba(255, 193, 7, 0.3)',
              borderTopColor: '#ffc107',
              animation: 'railSpin 0.9s linear infinite',
              flexShrink: 0,
            }}
          />
        ) : hasPriority && !isActiveTurn ? (
          <span
            aria-hidden
            title="Holding priority"
            style={{
              width: 6,
              height: 6,
              borderRadius: '50%',
              background: '#ffc107',
              boxShadow: '0 0 5px rgba(255, 193, 7, 0.8)',
              flexShrink: 0,
            }}
          />
        ) : null}

        {/* Crosshair badge — targets the player; the rest of the chip only switches the view */}
        {!spectatorMode && (isPlayerTargetable || isPlayerTargetSelected) && (
          <button
            onClick={handleTargetClick}
            title={isPlayerTargetSelected ? `Unselect ${opponent.name}` : `Target ${opponent.name}`}
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              justifyContent: 'center',
              width: 18,
              height: 18,
              borderRadius: '50%',
              border: `2px solid ${isPlayerTargetSelected ? '#ffff00' : '#ff4444'}`,
              background: isPlayerTargetSelected ? 'rgba(120, 120, 0, 0.5)' : 'rgba(80, 10, 10, 0.7)',
              color: isPlayerTargetSelected ? '#ffff66' : '#ff7777',
              fontSize: 11,
              lineHeight: 1,
              cursor: 'pointer',
              padding: 0,
              boxShadow: isPlayerTargetSelected
                ? '0 0 10px rgba(255, 255, 0, 0.7)'
                : '0 0 8px rgba(255, 68, 68, 0.7)',
              flexShrink: 0,
            }}
          >
            ⊕
          </button>
        )}

        {/* Inline distribute controls (damage split across players) */}
        {!spectatorMode && isDistributeTarget && (
          <span style={{ display: 'inline-flex', alignItems: 'center', gap: 2 }} onClick={(e) => e.stopPropagation()}>
            <button
              onClick={() => decrementDistribute(playerId)}
              disabled={distributeAllocated <= (distributeState?.minPerTarget ?? 0)}
              style={distributeButtonStyle(distributeAllocated > (distributeState?.minPerTarget ?? 0), '#dc2626')}
            >
              -
            </button>
            <span style={{ color: '#fff', fontSize: 11, fontWeight: 800, minWidth: 12, textAlign: 'center' }}>
              {distributeAllocated}
            </span>
            <button
              onClick={() => incrementDistribute(playerId)}
              disabled={distributeRemaining <= 0}
              style={distributeButtonStyle(distributeRemaining > 0, '#16a34a')}
            >
              +
            </button>
          </span>
        )}
      </div>

      {/* Planeswalker flyout — assign attacks to this player's planeswalkers
          without sliding their board in. */}
      {hovered && isDefenderTarget && hasSelectedAttackers && attackablePlaneswalkers.length > 0 && (
        <div
          style={{
            position: 'absolute',
            top: '100%',
            left: '50%',
            transform: 'translateX(-50%)',
            marginTop: 4,
            display: 'flex',
            gap: 6,
            padding: 6,
            borderRadius: 8,
            background: 'rgba(8, 10, 18, 0.95)',
            border: `1px solid ${seat.base}`,
            boxShadow: '0 4px 14px rgba(0, 0, 0, 0.5)',
            zIndex: 130,
          }}
        >
          {attackablePlaneswalkers.map((pw) => (
            <button
              key={pw.id}
              onClick={(e) => {
                e.stopPropagation()
                assignDefender(pw.id)
                setHovered(false)
              }}
              title={`Attack ${pw.name}`}
              style={{
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                gap: 2,
                padding: '4px 6px',
                borderRadius: 6,
                border: '1px solid rgba(255, 120, 120, 0.5)',
                background: 'rgba(60, 12, 12, 0.7)',
                color: '#ffb3b3',
                fontSize: 10,
                fontWeight: 700,
                cursor: 'pointer',
                maxWidth: 110,
              }}
            >
              <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', maxWidth: 100 }}>
                {pw.name}
              </span>
              {pw.counters.LOYALTY != null && <span style={{ color: '#e0c068' }}>◆ {pw.counters.LOYALTY}</span>}
            </button>
          ))}
        </div>
      )}
    </div>
  )
}

function distributeButtonStyle(enabled: boolean, activeColor: string): React.CSSProperties {
  return {
    width: 16,
    height: 16,
    borderRadius: 3,
    border: 'none',
    backgroundColor: enabled ? activeColor : '#333',
    color: enabled ? 'white' : '#666',
    fontSize: 11,
    fontWeight: 'bold',
    cursor: enabled ? 'pointer' : 'not-allowed',
    display: 'inline-flex',
    alignItems: 'center',
    justifyContent: 'center',
    padding: 0,
    lineHeight: 1,
  }
}

function chipTitle(opponent: ClientPlayer): string {
  const lines = [
    opponent.name,
    `Life ${opponent.life} · Hand ${opponent.handSize} · Library ${opponent.librarySize} · Graveyard ${opponent.graveyardSize}`,
  ]
  if (opponent.poisonCounters > 0) lines.push(`Poison ${opponent.poisonCounters}/10`)
  for (const e of opponent.commanderDamage ?? []) {
    lines.push(`⚔ ${e.commanderName}: ${e.amount}/${e.threshold}`)
  }
  if (opponent.hasLost) lines.push('Eliminated')
  return lines.join('\n')
}
