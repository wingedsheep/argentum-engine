import { useMemo } from 'react'
import type React from 'react'
import { useGameStore } from '@/store/gameStore'
import type { ClientPlayer } from '@/types'
import { hand } from '@/types'
import { useRevealedLibraryTopCard, useIdentityColor } from '@/store/selectors'
import { Battlefield } from './Battlefield'
import { CardRow } from './HandZone'
import { CommandZone } from './CommandZone'
import { ZonePile } from './ZonePiles'
import { styles } from './styles'

/**
 * One opponent's half of the board: hand fan (top), command zone | battlefield |
 * zone piles. This is today's 2-player opponent half, parameterized by player.
 *
 * Two layouts:
 * - `grid` — the classic 2-player placement: the hand is position:fixed at the
 *   viewport top and the board area is a direct grid child on row 2. Renders the
 *   exact markup GameBoard always had, so the 2-player game is pixel-identical.
 * - `strip` — a multiplayer slide item: the component renders a full-height
 *   strip cell; the hand is absolutely positioned inside it (so it slides with
 *   the board) above a reservation band matching grid row 1, and the board area
 *   fills the rest. Card scale machinery (slot sizing) is identical in both.
 */
export function OpponentBoardArea({
  opponent,
  layout,
  topOffset,
  handReservation = 0,
  stripBasis = '100%',
  hideHand = false,
  plateCarriesAnchors = false,
  viewedRingColor,
  onToggleCollapse,
  spectatorMode,
  isHijacking,
  hijackedSurfaceStyle,
  isAlly = false,
  allyColor,
  bottomHalf = false,
}: {
  opponent: ClientPlayer
  layout: 'grid' | 'strip'
  topOffset: number
  /** Strip layout only: height of the hand reservation band (grid row 1 height). */
  handReservation?: number
  /**
   * Strip layout only: this cell's share of the strip width as a CSS width value.
   * '100%' (default) for the one-board sliding camera; an equal fraction (or a
   * `calc(...)` share around collapsed tabs) when several boards share the strip
   * (table overview / combat defender-focus split) — card sizing self-measures per slot.
   */
  stripBasis?: string
  /**
   * Strip layout only: shared-strip view (table overview / combat defender-focus split).
   * Hides the opponent hand fan and its reservation band (the fans would overlap across
   * the narrow cells; rail chips carry the hand counts) and renders a seat-colored name
   * plate at the top of the cell instead — the board's "face".
   */
  hideHand?: boolean
  /**
   * Shared-strip view only: the name plate carries this player's anchors
   * (`data-life-id` etc.) so arrows, damage floats, and player-target clicks land on it.
   * False for the *viewed* board, whose anchors stay on the center-HUD life orb —
   * exactly one element per player may carry the anchors (see the OpponentRail comment).
   */
  plateCarriesAnchors?: boolean
  /**
   * Shared-strip view only: seat color for a persistent inset ring marking this cell as
   * the *viewed* board (the one the center-HUD orb and keys 1-9 track). Undefined = no ring.
   */
  viewedRingColor?: string
  /**
   * Table overview only: fold this cell down to a narrow tab (MTGO-style per-board
   * collapse) so the other boards split the freed width. Rendered as a small "−"
   * button next to the name plate; the collapsed tab itself is [CollapsedBoardTab].
   */
  onToggleCollapse?: () => void
  spectatorMode: boolean
  /** This opponent's seat is currently driven by this client (Mindslaver / hotseat). */
  isHijacking: boolean
  hijackedSurfaceStyle?: React.CSSProperties
  /**
   * Two-Headed Giant (CR 810): this board belongs to your teammate. You may see their hand
   * (CR 810.5b), so it renders face-up, and the cell gets an "ALLY" marker so it never reads as
   * an enemy board. You still can't act with their cards — only its controller plays from it.
   */
  isAlly?: boolean
  /** Team color for the ally marker (the viewing player's team hue). */
  allyColor?: string
  /**
   * This cell sits on the *bottom* half of a two-row "show table" layout, so its battlefield is
   * oriented like a player's own board — lands toward the bottom edge, creatures toward the center —
   * instead of the opponent orientation. The name plate still pins to the top of the cell.
   */
  bottomHalf?: boolean
}) {
  const revealedTopCard = useRevealedLibraryTopCard(opponent.playerId)
  const ghostCards = useMemo(
    () => (revealedTopCard ? [revealedTopCard] : []),
    [revealedTopCard]
  )

  /* Opponent hand — fixed at top of screen in grid layout; absolute inside the
     strip cell in strip layout (a strip cell starts at the viewport top, so the
     same `top` offset lands in the same place — but the hand travels with its
     board during slides). The face-up promotion during a Mindslaver-style hijack
     is itself the strongest signal that the controller is driving this hand. */
  const handBlock = (
    <div
      data-zone="opponent-hand"
      style={{
        position: layout === 'grid' ? 'fixed' : 'absolute',
        top: topOffset,
        left: '50%',
        transform: 'translateX(-50%)',
        zIndex: 50,
      }}
    >
      <CardRow
        zoneId={hand(opponent.playerId)}
        faceDown={!isHijacking && !isAlly}
        small
        inverted
        interactive={isHijacking}
        ghostCards={isHijacking ? [] : ghostCards}
      />
    </div>
  )

  const boardBlock = (
    <div
      style={
        layout === 'grid'
          ? styles.opponentArea
          : {
              // styles.opponentArea minus the grid-row binding — the strip cell
              // provides the vertical slot instead.
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              justifyContent: 'flex-start',
              minHeight: 0,
              overflow: 'hidden',
              flex: 1,
              width: '100%',
            }
      }
    >
      <div style={{ ...styles.playerRowWithZones, alignItems: 'flex-start' }}>
        {/* Opponent command zone (left side) — Commander format only; renders nothing otherwise. */}
        <CommandZone player={opponent} isOpponent />

        <div
          style={{
            ...styles.playerMainArea,
            ...(isHijacking ? hijackedSurfaceStyle : null),
          }}
        >
          {/* Opponent battlefield — lands first (closer to opponent), then creatures. On the
              bottom half of a two-row layout, flip to the player orientation so lands sit toward
              the bottom edge. */}
          <Battlefield isOpponent={!bottomHalf} playerId={opponent.playerId} spectatorMode={spectatorMode} />
        </div>

        {/* Opponent deck/graveyard (right side) */}
        <ZonePile player={opponent} isOpponent />
      </div>
    </div>
  )

  if (layout === 'grid') {
    return (
      <>
        {handBlock}
        {boardBlock}
      </>
    )
  }

  return (
    <div
      data-opponent-board={opponent.playerId}
      data-ally={isAlly || undefined}
      style={{
        flex: `0 0 ${stripBasis}`,
        minWidth: stripBasis,
        height: '100%',
        position: 'relative',
        display: 'flex',
        flexDirection: 'column',
        overflow: 'hidden',
        transition: 'flex-basis 220ms cubic-bezier(0.4, 0, 0.2, 1), min-width 220ms cubic-bezier(0.4, 0, 0.2, 1)',
      }}
    >
      {/* Two-Headed Giant ally marker — a team-colored corner badge so a teammate's board (with
          its face-up hand) is never mistaken for an opponent's. */}
      {isAlly && (
        <div
          aria-hidden
          style={{
            position: 'absolute',
            top: topOffset + 4,
            left: 10,
            zIndex: 55,
            display: 'inline-flex',
            alignItems: 'center',
            gap: 5,
            padding: '2px 9px',
            borderRadius: 999,
            border: `1px solid ${allyColor ?? '#2FD1A4'}`,
            background: 'rgba(8, 12, 18, 0.82)',
            color: allyColor ?? '#2FD1A4',
            fontSize: 10,
            fontWeight: 800,
            letterSpacing: '0.1em',
            textTransform: 'uppercase',
            pointerEvents: 'none',
            userSelect: 'none',
          }}
        >
          <span aria-hidden style={{ width: 7, height: 7, borderRadius: '50%', background: allyColor ?? '#2FD1A4' }} />
          Ally · {opponent.name}
        </div>
      )}
      {/* A hijack-controlled hand must stay visible even in shared-strip views —
          this client is playing from it. */}
      {(!hideHand || isHijacking) && handBlock}
      {/* Shared-strip view: the board's "face" — name + life at the top of the cell.
          Sits below the hand when a hijack forces the fan visible. */}
      {hideHand && (
        <BoardNamePlate
          player={opponent}
          carriesAnchors={plateCarriesAnchors}
          top={(isHijacking ? handReservation : 0) + 6}
        />
      )}
      {/* Fold-away control (table overview): collapse this cell to a tab so the
          other boards grow. Top-right corner, clear of the centered name plate. */}
      {onToggleCollapse && (
        <button
          onClick={onToggleCollapse}
          title={`Collapse ${opponent.name}'s board`}
          style={{
            position: 'absolute',
            top: (isHijacking ? handReservation : 0) + 6,
            right: 8,
            zIndex: 56,
            width: 24,
            height: 24,
            display: 'inline-flex',
            alignItems: 'center',
            justifyContent: 'center',
            borderRadius: 6,
            border: '1px solid #3a3a44',
            background: 'rgba(10, 12, 20, 0.85)',
            color: '#9fb0d0',
            fontSize: 14,
            fontWeight: 800,
            lineHeight: 1,
            cursor: 'pointer',
            padding: 0,
          }}
        >
          −
        </button>
      )}
      {/* Persistent inset ring marking the viewed cell in a shared-strip view. */}
      {viewedRingColor && (
        <div
          aria-hidden
          style={{
            position: 'absolute',
            inset: 2,
            pointerEvents: 'none',
            borderRadius: 10,
            boxShadow: `inset 0 0 0 2px ${viewedRingColor}55, inset 0 0 18px ${viewedRingColor}22`,
          }}
        />
      )}
      {/* Reservation band mirrors grid row 1 so the board area below aligns
          exactly with the 2-player opponent area (grid row 2). Shared-strip views
          replace it with room for the name plate — the board gets the rest of the
          vertical space back. */}
      <div
        style={{
          height: hideHand ? (isHijacking ? handReservation : 0) + 34 : handReservation,
          flexShrink: 0,
        }}
        aria-hidden
      />
      {boardBlock}
    </div>
  )
}

/**
 * A collapsed board's stand-in in the table overview (MTGO-style per-board collapse):
 * a narrow full-height tab with the seat color, a "+" affordance, and the player's name
 * running vertically. The whole tab is one click target that re-expands the board. The
 * seat's real board stays mounted off-screen (with the other hidden boards) so its card
 * anchors keep bundling to the rail chip, which also carries the player anchors — the
 * tab itself carries none.
 */
export function CollapsedBoardTab({
  player,
  onExpand,
}: {
  player: ClientPlayer
  onExpand: () => void
}) {
  const seat = useIdentityColor(player.playerId)
  const tomb = player.hasLost
  return (
    <div
      data-collapsed-board={player.playerId}
      role="button"
      title={`Expand ${player.name}'s board`}
      onClick={onExpand}
      style={{
        flex: `0 0 ${COLLAPSED_TAB_WIDTH}px`,
        minWidth: COLLAPSED_TAB_WIDTH,
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        gap: 8,
        padding: '8px 0',
        boxSizing: 'border-box',
        borderRadius: 8,
        border: `1px solid ${seat.base}55`,
        background: `linear-gradient(180deg, ${seat.soft}, rgba(10, 12, 20, 0.85))`,
        cursor: 'pointer',
        userSelect: 'none',
        overflow: 'hidden',
        transition: 'flex-basis 220ms cubic-bezier(0.4, 0, 0.2, 1), min-width 220ms cubic-bezier(0.4, 0, 0.2, 1)',
      }}
    >
      <span
        aria-hidden
        style={{
          width: 20,
          height: 20,
          display: 'inline-flex',
          alignItems: 'center',
          justifyContent: 'center',
          borderRadius: 5,
          border: `1px solid ${seat.base}`,
          color: seat.bright,
          fontSize: 13,
          fontWeight: 800,
          lineHeight: 1,
          flexShrink: 0,
        }}
      >
        +
      </span>
      <span
        aria-hidden
        style={{
          width: 8,
          height: 8,
          borderRadius: '50%',
          background: seat.base,
          boxShadow: `0 0 5px ${seat.base}`,
          flexShrink: 0,
          filter: tomb ? 'grayscale(1)' : 'none',
        }}
      />
      <span
        style={{
          writingMode: 'vertical-rl',
          fontSize: 12,
          fontWeight: 700,
          letterSpacing: '0.06em',
          color: seat.bright,
          maxHeight: '55%',
          overflow: 'hidden',
          textOverflow: 'ellipsis',
          whiteSpace: 'nowrap',
        }}
      >
        {player.name}
      </span>
    </div>
  )
}

/** Width of a collapsed board's tab in the table overview. */
export const COLLAPSED_TAB_WIDTH = 30

/**
 * The board's "face" in a shared-strip view: a compact seat-colored pill (name + life)
 * pinned to the top of the cell. When [carriesAnchors] it holds this player's anchor
 * attributes, so attack/targeting arrows and damage floats land on it instead of the
 * board's lands, and it doubles as the player-level click target — defender assignment
 * while declaring attackers, player targeting during a selection (same handling as the
 * rail chip's crosshair).
 */
function BoardNamePlate({
  player,
  carriesAnchors,
  top,
}: {
  player: ClientPlayer
  carriesAnchors: boolean
  top: number
}) {
  const seat = useIdentityColor(player.playerId)
  const playerId = player.playerId

  const combatState = useGameStore((state) => state.combatState)
  const assignDefender = useGameStore((state) => state.assignDefenderToSelectedAttackers)
  const draggingAttackerId = useGameStore((state) => state.draggingAttackerId)
  const targetingState = useGameStore((state) => state.targetingState)
  const addTarget = useGameStore((state) => state.addTarget)
  const removeTarget = useGameStore((state) => state.removeTarget)
  const pendingDecision = useGameStore((state) => state.pendingDecision)
  const submitTargetsDecision = useGameStore((state) => state.submitTargetsDecision)
  const decisionSelectionState = useGameStore((state) => state.decisionSelectionState)
  const toggleDecisionSelection = useGameStore((state) => state.toggleDecisionSelection)

  // Defender assignment (mirrors RailChip): legal while declaring attackers with a
  // selection or an attacker drag in flight.
  const declaringAttackers = combatState?.mode === 'declareAttackers'
  const isDefenderTarget =
    declaringAttackers && (combatState?.validAttackTargets.includes(playerId) ?? false)
  const isDefenderAssignTarget =
    isDefenderTarget &&
    ((combatState?.selectedAttackers.length ?? 0) > 0 || draggingAttackerId !== null)

  // Player-as-target (mirrors RailChip's crosshair handling).
  const isTargetingSelected = targetingState?.selectedTargets.includes(playerId) ?? false
  const isValidTargetingTarget = targetingState?.validTargets.includes(playerId) ?? false
  const isChooseTargetsDecision = pendingDecision?.type === 'ChooseTargetsDecision'
  const isValidDecisionTarget =
    isChooseTargetsDecision &&
    pendingDecision.targetRequirements.length === 1 &&
    (pendingDecision.legalTargets[0] ?? []).includes(playerId)
  const isValidDecisionSelection = decisionSelectionState?.validOptions.includes(playerId) ?? false
  const isSelectedDecisionOption = decisionSelectionState?.selectedOptions.includes(playerId) ?? false
  const isPlayerTargetable = isValidTargetingTarget || isValidDecisionTarget || isValidDecisionSelection
  const isPlayerTargetSelected = isTargetingSelected || isSelectedDecisionOption

  const handleClick = (e: React.MouseEvent) => {
    e.stopPropagation()
    if (isDefenderTarget && (combatState?.selectedAttackers.length ?? 0) > 0) {
      assignDefender(playerId)
      return
    }
    if (isTargetingSelected) {
      removeTarget(playerId)
      return
    }
    if (isValidTargetingTarget) {
      addTarget(playerId)
      return
    }
    if (isValidDecisionTarget) {
      submitTargetsDecision({ 0: [playerId] })
      return
    }
    if (isValidDecisionSelection) {
      toggleDecisionSelection(playerId)
    }
  }

  const interactive = isDefenderAssignTarget || isPlayerTargetable || isPlayerTargetSelected
  const lifeDanger = player.life <= 5
  const borderColor = isDefenderAssignTarget
    ? '#ff4444'
    : isPlayerTargetSelected
      ? '#ffff00'
      : isPlayerTargetable
        ? '#ff4444'
        : seat.base

  return (
    <div
      data-board-plate={playerId}
      {...(carriesAnchors
        ? {
            'data-player-id': playerId,
            'data-life-id': playerId,
            'data-life-display': playerId,
          }
        : {})}
      role={interactive ? 'button' : undefined}
      title={
        isDefenderAssignTarget
          ? `Attack ${player.name}`
          : isPlayerTargetable || isPlayerTargetSelected
            ? (isPlayerTargetSelected ? `Unselect ${player.name}` : `Target ${player.name}`)
            : player.name
      }
      onClick={interactive ? handleClick : undefined}
      style={{
        position: 'absolute',
        top,
        left: '50%',
        transform: 'translateX(-50%)',
        zIndex: 56,
        display: 'inline-flex',
        alignItems: 'center',
        gap: 6,
        height: 24,
        padding: '0 11px',
        borderRadius: 999,
        border: `${interactive ? 2 : 1}px solid ${borderColor}`,
        background: 'rgba(10, 12, 20, 0.9)',
        color: '#dde3f0',
        fontSize: 12,
        fontWeight: 700,
        whiteSpace: 'nowrap',
        userSelect: 'none',
        cursor: interactive ? 'pointer' : 'default',
        pointerEvents: 'auto',
        boxShadow: isDefenderAssignTarget
          ? '0 0 12px rgba(255, 68, 68, 0.6)'
          : isPlayerTargetSelected
            ? '0 0 10px rgba(255, 255, 0, 0.6)'
            : 'none',
        transition: 'border-color 150ms, box-shadow 150ms',
      }}
    >
      <span
        aria-hidden
        style={{
          width: 9,
          height: 9,
          borderRadius: '50%',
          background: seat.base,
          boxShadow: `0 0 5px ${seat.base}`,
          flexShrink: 0,
        }}
      />
      <span
        style={{
          maxWidth: 140,
          overflow: 'hidden',
          textOverflow: 'ellipsis',
          color: seat.bright,
        }}
      >
        {player.name}
      </span>
      <span
        style={{
          display: 'inline-flex',
          alignItems: 'center',
          gap: 3,
          fontVariantNumeric: 'tabular-nums',
          color: lifeDanger ? '#ff5555' : '#ffffff',
        }}
      >
        <span aria-hidden style={{ color: '#ff6b6b', fontSize: 11 }}>❤</span>
        {player.life}
      </span>
    </div>
  )
}
