import { useLayoutEffect, useMemo, useRef, useState } from 'react'
import type React from 'react'
import type { RefObject } from 'react'
import { useGameStore } from '@/store/gameStore'
import type { ClientPlayer } from '@/types'
import { hand } from '@/types'
import { useRevealedLibraryTopCard, useIdentityColor, useIsSharedLifeTeamGame, useZoneCards } from '@/store/selectors'
import { useResponsiveContext } from './shared'
import { Battlefield } from './Battlefield'
import { CardRow, HAND_FAN_EDGE_MARGIN } from './HandZone'
import { CommandZone } from './CommandZone'
import { ZonePile } from './ZonePiles'
import { styles } from './styles'
import { isLoneTargetRequirement } from '@/utils/targeting.ts'

/** Height of a shared-strip cell's name-plate band (the pill plus its top margin). */
export const CELL_PLATE_BAND = 34

/**
 * How far a [HandFan] paints past the edge it hangs from — its own negative margin plus the
 * cards' negative edge offset. Symmetric: an inverted fan spills this far *above* its box, a
 * normal one this far *below*. Either way the band a cell hand sits in has to reserve it, or the
 * fan draws through the name plate above (inverted) or the battlefield below (normal) — and
 * because an ally's cell hand is click-through-able, an unreserved overhang also swallows clicks
 * meant for the permanents underneath it.
 */
const FAN_EDGE_OVERHANG = HAND_FAN_EDGE_MARGIN * 2

/**
 * Height a `cellHand: 'count'` cell owes its hand — zero, because the count rides *inside* the
 * name plate rather than under it. A number and a stack glyph fit on the plate beside the seat's
 * name, and a whole extra band to carry them was a row of screen height spent on two characters.
 */
export const CELL_HAND_COUNT_BAND = 0

/**
 * Sizing for the hand a board renders *inside* a shared-strip cell (table overview, team-split
 * bottom row, combat defender-focus split). The full-width fan is viewport-relative and would
 * run straight over the neighbouring cells, so the cell measures itself and the fan is capped
 * from that — deliberately smaller than a fan that merely fits, because with every board on
 * screen at once the battlefields are what the width is for.
 *
 * [handHeight] is the fan's own box and an upper bound on it (`calculateFittingCardWidth` only
 * ever shrinks the cap). [handBand] is what a cell must actually reserve: that box plus the
 * [FAN_EDGE_OVERHANG] the fan spills past whichever edge it hangs from. Exported so the viewer's
 * own bottom-row cell — which has no cell hand, its fan being the full-width one at the screen
 * bottom — reserves the identical band and stays aligned with its neighbours.
 */
export function useCellHandMetrics(): {
  cellRef: RefObject<HTMLDivElement | null>
  cellWidth: number
  cardWidth: number
  handHeight: number
  handBand: number
} {
  const responsive = useResponsiveContext()
  const cellRef = useRef<HTMLDivElement | null>(null)
  const [cellWidth, setCellWidth] = useState(0)
  useLayoutEffect(() => {
    const node = cellRef.current
    if (!node) return
    setCellWidth(node.getBoundingClientRect().width)
    const obs = new ResizeObserver((entries) => {
      const entry = entries[0]
      if (entry) setCellWidth(entry.contentRect.width)
    })
    obs.observe(node)
    return () => obs.disconnect()
  }, [])
  const cardWidth = Math.max(
    22,
    Math.min(Math.round(responsive.smallCardWidth * 0.72), Math.round(cellWidth / 9) || 999),
  )
  const handHeight = Math.round(cardWidth * 1.4) + 12
  return { cellRef, cellWidth, cardWidth, handHeight, handBand: handHeight + FAN_EDGE_OVERHANG }
}

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
  activeTurnRingColor,
  onToggleCollapse,
  spectatorMode,
  isHijacking,
  hijackedSurfaceStyle,
  isAlly = false,
  allyColor,
  bottomHalf = false,
  cellHand = 'fan',
  plateAtBottom = false,
  liftHand = false,
  controlsTop,
  areaRef,
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
   * Shared-strip view only: seat color for a persistent inset ring marking this cell as the
   * board of the player whose **turn** it is. With every board on screen at once, "whose turn
   * is it" is the thing worth a highlight — which cell the camera nominally tracks isn't.
   * Undefined = no ring.
   */
  activeTurnRingColor?: string
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
   * (CR 810.5), so it renders face-up, and the cell gets an "ALLY" marker so it never reads as
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
  /**
   * Shared-strip view only: how this cell shows the seat's hand.
   *
   * - `'fan'` — the scaled-down fan under the name plate (the default, and what a Free-for-All
   *   pod uses for every board).
   * - `'count'` — a compact face-down stack with the card count. All you can learn from a
   *   *fully* face-down fan is how many cards it holds, and a nine-card arc costs a whole band of
   *   height to say a number; with four boards on screen that height is better spent on
   *   battlefields. The moment anything in that hand is revealed to you (Peek, Duress, Telepathy)
   *   the cell falls back to `'fan'` — a count would hide exactly the information the reveal was
   *   for.
   * - `'none'` — no hand and no band at all. Two-Headed Giant's ally, whose hand is open to you
   *   (CR 810.5) and therefore renders full-size beside your own at the bottom of the screen
   *   rather than shrunk into their cell.
   */
  cellHand?: 'fan' | 'count' | 'none'
  /**
   * Shared-strip view only: hang the name plate from the *bottom* of the cell instead of the top.
   * Used for the bottom row of a Two-Headed Giant table, where the plates are the two heads of the
   * shared team-life banner and belong at the screen edge, beside the life total they share.
   */
  plateAtBottom?: boolean
  /**
   * Shared-strip view only: vertical offset for this cell's corner controls (the collapse button),
   * overriding the plate's own. The two top screen corners belong to Fullscreen and Concede, so a
   * strip that has moved its plate up near the top edge still has to keep anything in a *corner*
   * below that row.
   */
  controlsTop?: number
  /** Grid layout only: ref to the board-area element (grid row 2) for GameBoard's pooled battlefield sizing. */
  areaRef?: RefObject<HTMLDivElement | null>
  /**
   * This seat's hand is rendered *outside* the cell — the Two-Headed Giant ally fan at the bottom
   * of the screen. Without this, a cell being driven by this client (hotseat / Mindslaver) forces
   * its own full-size fan back into the cell, which for a bottom-row cell means the hand lands at
   * the *top* of that cell, floating in the middle of the screen, and pushes the name plate off
   * the bottom edge it shares with the team-life banner.
   */
  liftHand?: boolean
}) {
  const revealedTopCard = useRevealedLibraryTopCard(opponent.playerId)
  const ghostCards = useMemo(
    () => (revealedTopCard ? [revealedTopCard] : []),
    [revealedTopCard]
  )
  const {
    cellRef,
    cellWidth,
    cardWidth: cellHandCardWidth,
    handHeight: cellHandHeight,
    handBand: cellHandBand,
  } = useCellHandMetrics()
  // A bottom-half cell's board is oriented like a player's own, so its hand hangs the same way
  // as yours (face toward the bottom edge) rather than inverted like an opponent's.
  const cellHandInverted = !bottomHalf && !(isAlly && hideHand)
  // A driven seat normally reclaims its full-size interactive fan inside the cell — unless the
  // hand has been lifted out of the cell entirely ([liftHand]), in which case the lifted copy is
  // the interactive one and the cell must not draw a second.
  const drivesOwnHand = isHijacking && !liftHand
  // A face-down hand can still hold cards *you* can see — Peek, Duress, Telepathy, anything that
  // reveals from an opponent's hand. [CardRow] draws those face-up among the face-down placeholders,
  // which a bare count would throw away, so a hand with anything revealed always gets the fan.
  const revealedInHand = useZoneCards(hand(opponent.playerId)).length > 0
  const effectiveCellHand = cellHand === 'count' && revealedInHand ? 'fan' : cellHand
  // How much vertical room this cell owes to the plate + whatever it shows of the hand. A hijacked
  // hand keeps the full grid-row-1 reservation (it renders the real interactive fan); otherwise it
  // is the plate band plus the fan's own band, the count strip, or nothing at all.
  const cellHandOwn =
    drivesOwnHand ? handReservation
      : effectiveCellHand === 'fan' ? cellHandBand
        : effectiveCellHand === 'count' ? CELL_HAND_COUNT_BAND
          : 0
  // A teammate (Two-Headed Giant, 2v2, ...) on either row keeps its open hand *below* its lands, hanging from the
  // bottom edge like your own, so the plate stays alone at the top and the board sits under it.
  const handBelowBoard = hideHand && isAlly && !drivesOwnHand && effectiveCellHand === 'fan'
  const reservationBand = handBelowBoard
    ? CELL_PLATE_BAND
    : hideHand ? cellHandOwn + CELL_PLATE_BAND : handReservation

  /* Opponent hand — fixed at top of screen in grid layout; absolute inside the
     strip cell in strip layout (a strip cell starts at the viewport top, so the
     same `top` offset lands in the same place — but the hand travels with its
     board during slides). The face-up promotion during a Mindslaver-style hijack
     is itself the strongest signal that the controller is driving this hand. */
  const handBlock = (
    <div
      data-zone="opponent-hand"
      data-zone-owner={opponent.playerId}
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
      ref={layout === 'grid' ? areaRef : undefined}
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

        {/* Opponent deck/graveyard (right side). Multi-board cells (hideHand) sit inside a strip
            whose paddingTop already clears the Fullscreen/Concede button row, so the pile column
            must not reserve that room a second time — the double-count clipped the Exile pile. */}
        <ZonePile player={opponent} isOpponent reserveConcedeRoom={layout === 'grid' || !hideHand} />
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
      ref={cellRef}
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
      {isAlly && !hideHand && (
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
          this client is playing from it, so it keeps the full-size interactive fan. */}
      {(!hideHand || drivesOwnHand) && handBlock}
      {/* Shared-strip view: the board's "face" — name (+ life outside a shared-life team
          game) at the top of the cell. Sits below the hand when a hijack forces the fan
          visible. */}
      {hideHand && (
        <BoardNamePlate
          player={opponent}
          carriesAnchors={plateCarriesAnchors}
          top={(drivesOwnHand ? handReservation : 0) + 6}
          anchor={plateAtBottom ? 'bottom' : 'top'}
          isAlly={isAlly}
          {...(
            /* Keyed on what was *asked for*, not what is rendered: the badge stays on the plate
               even when a reveal forces the fan back, so the number doesn't appear and vanish as
               cards become known — and it is then the only place the hand's *full* size shows,
               the fan being a mix of face-up cards and backs. */
            cellHand === 'count' && !drivesOwnHand ? { handCount: opponent.handSize ?? 0 } : {}
          )}
          {...(allyColor ? { allyColor } : {})}
        />
      )}
      {/* Shared-strip view: this seat's hand, scaled down to the cell and sitting under the
          name plate. Knowing how many cards each player is holding — and, for a Two-Headed
          Giant ally whose hand is open to you (CR 810.5), *which* cards — is board state you
          shouldn't have to slide the camera onto a board to read. */}
      {hideHand && !drivesOwnHand && effectiveCellHand === 'fan' && (
        <div
          data-zone="opponent-hand"
          data-zone-owner={opponent.playerId}
          style={{
            position: 'absolute',
            // Only the inverted fan spills *upward*; pushing it down by the overhang is what
            // keeps its top row from drawing straight through the name plate. A normal fan
            // spills downward instead — that half is reserved in the band below.
            ...(handBelowBoard
              ? { bottom: FAN_EDGE_OVERHANG }
              : { top: CELL_PLATE_BAND + (cellHandInverted ? FAN_EDGE_OVERHANG : 0) }),
            left: 0,
            right: 0,
            height: cellHandHeight,
            display: 'flex',
            // Anchor the fan at the edge it hangs from, so the arc grows into the band rather
            // than out of it: down from the top for an opponent-side hand, up from the bottom
            // for a bottom-row one.
            alignItems: cellHandInverted ? 'flex-start' : 'flex-end',
            justifyContent: 'center',
            zIndex: 50,
            pointerEvents: isAlly ? 'auto' : 'none',
          }}
        >
          <CardRow
            zoneId={hand(opponent.playerId)}
            faceDown={!isAlly}
            small
            inverted={cellHandInverted}
            fan
            fitWidth={Math.max(60, cellWidth - 16)}
            maxCardWidth={cellHandCardWidth}
            ghostCards={[]}
          />
        </div>
      )}
      {/* Fold-away control (table overview): collapse this cell to a tab so the
          other boards grow. Top-right corner, clear of the centered name plate. */}
      {onToggleCollapse && (
        <button
          onClick={onToggleCollapse}
          title={`Collapse ${opponent.name}'s board`}
          style={{
            position: 'absolute',
            top: controlsTop ?? (drivesOwnHand ? handReservation : 0) + 6,
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
      {/* Persistent inset ring marking the active player's cell in a shared-strip view. */}
      {activeTurnRingColor && (
        <div
          aria-hidden
          style={{
            position: 'absolute',
            inset: 2,
            pointerEvents: 'none',
            borderRadius: 10,
            boxShadow: `inset 0 0 0 2px ${activeTurnRingColor}55, inset 0 0 18px ${activeTurnRingColor}22`,
          }}
        />
      )}
      {/* Reservation band mirrors grid row 1 so the board area below aligns
          exactly with the 2-player opponent area (grid row 2). Shared-strip views
          replace it with room for the name plate — the board gets the rest of the
          vertical space back. */}
      {/* Reservation band mirrors grid row 1 so the board area below aligns exactly with the
          2-player opponent area (grid row 2). Shared-strip views replace it with room for the name
          plate and whatever the cell shows of the hand — and put it *after* the board when the
          plate hangs from the bottom edge. */}
      {!plateAtBottom && <div style={{ height: reservationBand, flexShrink: 0 }} aria-hidden />}
      {boardBlock}
      {plateAtBottom && <div style={{ height: reservationBand, flexShrink: 0 }} aria-hidden />}
      {handBelowBoard && <div style={{ height: cellHandOwn, flexShrink: 0 }} aria-hidden />}
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
/**
 * A face-down hand reduced to a stack glyph and its count, sized to sit on a board's name plate
 * (`cellHand: 'count'`).
 *
 * All you can learn from a fully face-down fan is how many cards the seat is holding, and a
 * nine-card arc spends a whole band of cell height saying it. On the plate it costs nothing: the
 * plate is already there, and in a shared-life team game it has room going spare where the life
 * total used to be.
 */
function HandCountBadge({ count }: { count: number }) {
  return (
    <span
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: 5,
        flexShrink: 0,
        color: count === 0 ? '#6b7488' : '#c8d2e6',
        fontVariantNumeric: 'tabular-nums',
      }}
    >
      {/* Three card backs, fanned just enough to read as a hand rather than one card. */}
      <span aria-hidden style={{ position: 'relative', width: 20, height: 14, flexShrink: 0 }}>
        {[-1, 0, 1].map((i) => (
          <span
            key={i}
            style={{
              position: 'absolute',
              left: 7 + i * 4.5,
              top: Math.abs(i),
              width: 8,
              height: 12,
              borderRadius: 2,
              border: '1px solid #4a5570',
              background: 'linear-gradient(160deg, #2a3350 0%, #171d2e 100%)',
              transform: `rotate(${i * 9}deg)`,
            }}
          />
        ))}
      </span>
      {count}
    </span>
  )
}

export function BoardNamePlate({
  player,
  carriesAnchors,
  top,
  anchor = 'top',
  isAlly = false,
  allyColor,
  handCount,
}: {
  player: ClientPlayer
  carriesAnchors: boolean
  /** Distance from the cell edge named by [anchor]. */
  top: number
  /**
   * Which cell edge the plate hangs from. `'top'` for an opponent-oriented board (the plate sits
   * above the cards, on the far side of the table). `'bottom'` for a board on your own side of a
   * two-row table: its plate belongs next to *you*, at the screen edge, where it also joins the
   * shared team-life banner ([TeamLifeBanner]) instead of floating in the middle of the screen.
   */
  anchor?: 'top' | 'bottom'
  /**
   * Two-Headed Giant: mark this board as your teammate's. The floating corner badge stands down
   * wherever a plate renders — two labels naming the same player is one too many — so the plate
   * carries the marker instead.
   */
  isAlly?: boolean
  allyColor?: string
  /**
   * Render this seat's hand size on the plate (`cellHand: 'count'`). Undefined leaves it off — the
   * cell is drawing a real fan and the fan already says how many cards there are.
   */
  handCount?: number
}) {
  const seat = useIdentityColor(player.playerId)
  const playerId = player.playerId
  // Two-Headed Giant (CR 810): the team's single shared life lives on the center-HUD team orb,
  // so repeating it on every teammate's plate would print the same number up to four times.
  // The plate keeps the name (which is the thing a plate is for) and drops the life.
  const sharedLifeTeam = useIsSharedLifeTeamGame()

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
  // Only a lone single-target requirement uses the immediate click-to-submit path; a multi-target
  // player slot (e.g. Parker Luck's "two target players") is picked via the decisionSelectionState
  // toggle path (isValidDecisionSelection) instead, so it must NOT match here.
  const isValidDecisionTarget =
    isChooseTargetsDecision &&
    isLoneTargetRequirement(pendingDecision) &&
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
      submitTargetsDecision(pendingDecision!.id, { 0: [playerId] })
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
            : handCount != null
              ? `${player.name} — ${handCount} ${handCount === 1 ? 'card' : 'cards'} in hand`
              : player.name
      }
      onClick={interactive ? handleClick : undefined}
      style={{
        position: 'absolute',
        ...(anchor === 'bottom' ? { bottom: top } : { top }),
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
      {isAlly && (
        <span
          aria-hidden
          title="Your teammate"
          style={{
            fontSize: 9,
            fontWeight: 800,
            letterSpacing: '0.1em',
            color: allyColor ?? seat.bright,
            border: `1px solid ${allyColor ?? seat.base}`,
            padding: '0 4px',
            borderRadius: 3,
            lineHeight: '13px',
            flexShrink: 0,
          }}
        >
          ALLY
        </span>
      )}
      {handCount != null && <HandCountBadge count={handCount} />}
      {!sharedLifeTeam && (
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
      )}
    </div>
  )
}
