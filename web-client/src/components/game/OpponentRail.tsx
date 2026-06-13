import { useEffect, useMemo, useRef, useState } from 'react'
import type React from 'react'
import { useGameStore } from '@/store/gameStore'
import {
  selectGameState,
  useOpponents,
  useViewedOpponent,
  useSeatIndex,
} from '@/store/selectors'
import { seatColor } from '@/styles/seatColors'
import type { ClientCard, ClientPlayer } from '@/types'
import { useResponsiveContext } from './board/shared'

/** Height of the rail band; GameBoard adds this to its top offset in multiplayer. */
export const OPPONENT_RAIL_HEIGHT = 34

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
  topOffset,
  spectatorMode = false,
}: {
  topOffset: number
  spectatorMode?: boolean
}) {
  const opponents = useOpponents()
  const viewedOpponent = useViewedOpponent()
  const viewPinned = useGameStore((state) => state.viewPinned)
  const followAction = useGameStore((state) => state.followAction)
  const toggleFollowAction = useGameStore((state) => state.toggleFollowAction)

  if (opponents.length <= 1) return null

  return (
    <div
      data-opponent-rail
      style={{
        position: 'fixed',
        top: topOffset,
        left: 0,
        right: 0,
        height: OPPONENT_RAIL_HEIGHT,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        gap: 8,
        zIndex: 120,
        pointerEvents: 'none',
        // Leave the top corners free for the fullscreen / spectator-count /
        // concede buttons that live at left/right edges.
        padding: '0 150px',
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
      `}</style>
      {opponents.map((opponent) => (
        <RailChip
          key={opponent.playerId}
          opponent={opponent}
          isViewed={viewedOpponent?.playerId === opponent.playerId}
          viewPinned={viewPinned}
          spectatorMode={spectatorMode}
        />
      ))}
      {!spectatorMode && (
        <button
          onClick={toggleFollowAction}
          title={
            followAction
              ? 'Follow the action: the view slides to the active board automatically. Click for a manual camera.'
              : 'Manual camera: the view only moves when you switch boards. Click to follow the action.'
          }
          style={{
            pointerEvents: 'auto',
            height: 22,
            padding: '0 8px',
            display: 'inline-flex',
            alignItems: 'center',
            gap: 4,
            borderRadius: 999,
            border: `1px solid ${followAction ? 'rgba(110, 200, 255, 0.5)' : '#444'}`,
            background: followAction ? 'rgba(20, 50, 80, 0.8)' : 'rgba(25, 25, 35, 0.8)',
            color: followAction ? '#9fd8ff' : '#777',
            fontSize: 10,
            fontWeight: 700,
            letterSpacing: '0.06em',
            textTransform: 'uppercase',
            cursor: 'pointer',
            whiteSpace: 'nowrap',
          }}
        >
          <span aria-hidden style={{ fontSize: 11 }}>{followAction ? '◉' : '○'}</span>
          Follow
        </button>
      )}
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
}: {
  opponent: ClientPlayer
  isViewed: boolean
  viewPinned: boolean
  spectatorMode: boolean
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
  const seatIndex = useSeatIndex(playerId)
  const seat = seatColor(Math.max(0, seatIndex))

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

  const ringShadow = [
    isActiveTurn ? `0 0 0 2px ${seat.bright}, 0 0 10px ${seat.soft}` : null,
    isDefenderAssignTarget ? '0 0 12px rgba(255, 68, 68, 0.6)' : null,
  ].filter(Boolean).join(', ')

  const lifeDanger = opponent.life <= 5

  return (
    <div style={{ position: 'relative', pointerEvents: 'auto' }}>
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
          display: 'inline-flex',
          alignItems: 'center',
          gap: compact ? 4 : 7,
          height: compact ? 22 : 26,
          padding: compact ? '0 7px' : '0 11px',
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
          ...(boardHasTargets && !isViewed
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
        {/* Viewed marker / seat dot / skull */}
        <span
          aria-hidden
          style={{
            display: 'inline-flex',
            alignItems: 'center',
            justifyContent: 'center',
            width: 10,
            height: 10,
            borderRadius: '50%',
            background: tomb ? 'transparent' : seat.base,
            boxShadow: tomb ? 'none' : `0 0 5px ${seat.base}`,
            fontSize: 10,
            flexShrink: 0,
          }}
        >
          {tomb ? '💀' : ''}
        </span>

        {/* Name (hidden on phones — the seat dot + position carries identity) */}
        {!compact && (
          <span
            style={{
              maxWidth: 110,
              overflow: 'hidden',
              textOverflow: 'ellipsis',
              fontSize: 12,
              fontWeight: 700,
              letterSpacing: '0.02em',
              color: tomb ? '#666' : seat.bright,
            }}
          >
            {opponent.name}
            {isViewed && viewPinned && !spectatorMode && (
              <span aria-hidden title="Pinned — follow-the-action paused" style={{ marginLeft: 4 }}>📌</span>
            )}
          </span>
        )}

        {/* Life — also the anchor for floating ±life deltas (data-life-display) */}
        <span
          style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: 3,
            fontSize: compact ? 11 : 13,
            fontWeight: 800,
            fontVariantNumeric: 'tabular-nums',
            color: tomb ? '#555' : lifeDanger ? '#ff5555' : '#ffffff',
            textDecoration: tomb ? 'line-through' : 'none',
          }}
        >
          <span aria-hidden style={{ color: tomb ? '#555' : '#ff6b6b', fontSize: compact ? 10 : 12 }}>❤</span>
          {opponent.life}
        </span>

        {/* Hand count */}
        {!tomb && (
          <span
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: 3,
              fontSize: compact ? 10 : 12,
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
