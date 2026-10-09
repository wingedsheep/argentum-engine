import { useLayoutEffect, useRef, useState, type CSSProperties } from 'react'
import type { EntityId } from '@/types'
import type { BlockEdge, CombatClique } from './combatClique'
import type { ViewportBounds } from './combatFocusBounds'

/**
 * Hover focus for combat: a spotlight over the hovered creature's clique (see computeCombatClique)
 * and a caption on the hovered card saying who it blocks or is blocked by.
 */

export const ATTACKER_TONE = '#ff6b6b'
export const BLOCKER_TONE = '#64a8ff'
const UNBLOCKED_TONE = '#fbbf24'
const BAND_TONE = '#c084fc'

/** Darkness of the scrim over everything outside the clique. */
const SCRIM_OPACITY = 0.5
const RING_PAD = 5
const RING_RADIUS = 12
/**
 * How far the published bounds reach past the cards: the ring, plus the caption pill — above or
 * below the hovered card, and, centred on a narrow card, overhanging it a little on each side.
 */
const BOUNDS_PAD_X = RING_PAD + 40
const BOUNDS_PAD_Y = 44
const VIEWPORT_MARGIN = 8

export interface FocusCaption {
  /** Null for a band before blocks are known: only the band chip shows. */
  readonly role: 'blocking' | 'blockedBy' | 'unblocked' | null
  /** The creatures on the other side: attackers it blocks, or blockers blocking it. */
  readonly others: readonly EntityId[]
  /** Size of the hovered attacker's band, 0 when it isn't banded. */
  readonly bandSize: number
}

/**
 * What the caption on the hovered creature says. A banded attacker with no block of its own is
 * still blocked when a band mate is, so it reports the band's blockers. Returns null when there's
 * nothing true to say yet — an attacker before blocks are known, outside a band.
 */
export function describeFocus(
  focusId: EntityId,
  clique: CombatClique,
  edges: readonly BlockEdge[],
  bands: readonly (readonly EntityId[])[],
  blocksKnown: boolean,
): FocusCaption | null {
  if (clique.blockers.has(focusId)) {
    return { role: 'blocking', others: edges.filter((e) => e.blockerId === focusId).map((e) => e.attackerId), bandSize: 0 }
  }
  const bandSize = bands.find((band) => band.includes(focusId))?.length ?? 0
  const direct = edges.filter((e) => e.attackerId === focusId).map((e) => e.blockerId)
  const blockers = direct.length > 0 ? direct : bandSize > 1 ? [...clique.blockers] : []
  if (blockers.length > 0) return { role: 'blockedBy', others: blockers, bandSize }
  if (blocksKnown) return { role: 'unblocked', others: [], bandSize }
  return bandSize > 1 ? { role: null, others: [], bandSize } : null
}

interface Rect {
  readonly left: number
  readonly top: number
  readonly right: number
  readonly bottom: number
  readonly width: number
  readonly height: number
}

/** On-screen rects of the clique's cards; a card on a slid-away multiplayer board is skipped. */
export function measureClique(clique: CombatClique): Map<EntityId, Rect> {
  const rects = new Map<EntityId, Rect>()
  for (const id of clique.members) {
    const el = document.querySelector(`[data-card-id="${id}"]`)
    if (!el) continue
    const r = el.getBoundingClientRect()
    const cx = r.left + r.width / 2
    if (cx < -4 || cx > window.innerWidth + 4) continue
    rects.set(id, r)
  }
  return rects
}

/** Bounding box of the measured clique, padded to take in its rings and the caption pill. */
export function cliqueBounds(rects: ReadonlyMap<EntityId, Rect>): ViewportBounds | null {
  let left = Infinity, top = Infinity, right = -Infinity, bottom = -Infinity
  for (const r of rects.values()) {
    left = Math.min(left, r.left)
    top = Math.min(top, r.top)
    right = Math.max(right, r.right)
    bottom = Math.max(bottom, r.bottom)
  }
  if (left === Infinity) return null
  return {
    left: left - BOUNDS_PAD_X,
    top: top - BOUNDS_PAD_Y,
    right: right + BOUNDS_PAD_X,
    bottom: bottom + BOUNDS_PAD_Y,
  }
}

/**
 * The spotlight, drawn under the arrows: a scrim with a rounded hole over every clique card,
 * plus a ring in the card's combat role colour (white on the hovered card). `visible` fades the
 * scrim out before the caller unmounts it, so leaving a creature eases back instead of snapping.
 *
 * Its own full-viewport `<svg>`, promoted to a compositor layer, rather than a group inside the
 * arrow overlay: the masked scrim is the most expensive thing on screen to rasterize, and sharing
 * a layer with the arrows re-rasterized it on every frame of every arrow's dim/undim transition.
 * On its own layer it is painted once per clique and the fades are pure compositor opacity.
 */
export function CliqueSpotlight({
  clique,
  rects,
  focusId,
  visible,
}: {
  clique: CombatClique
  rects: ReadonlyMap<EntityId, Rect>
  focusId: EntityId
  visible: boolean
}) {
  const hole = (r: Rect) => ({
    x: r.left - RING_PAD,
    y: r.top - RING_PAD,
    width: r.width + RING_PAD * 2,
    height: r.height + RING_PAD * 2,
    rx: RING_RADIUS,
  })
  return (
    <svg
      className="combat-focus-in"
      style={{
        position: 'fixed',
        top: 0,
        left: 0,
        width: '100vw',
        height: '100vh',
        pointerEvents: 'none',
        zIndex: 2000,
        opacity: visible ? 1 : 0,
        transition: 'opacity 180ms ease-out',
        willChange: 'opacity',
      }}
    >
      <defs>
        <mask id="combat-focus-mask" maskUnits="userSpaceOnUse">
          <rect x={0} y={0} width="100%" height="100%" fill="white" />
          {[...rects.values()].map((r, i) => <rect key={i} {...hole(r)} fill="black" />)}
        </mask>
      </defs>
      <rect x={0} y={0} width="100%" height="100%" fill="#04060b" fillOpacity={SCRIM_OPACITY}
        mask="url(#combat-focus-mask)" />
      {[...rects.entries()].map(([id, r]) => {
        const tone = clique.blockers.has(id) ? BLOCKER_TONE : ATTACKER_TONE
        const hovered = id === focusId
        return (
          <g key={id}>
            <rect {...hole(r)} fill="none" stroke={tone} strokeOpacity={0.28} strokeWidth={9} />
            <rect {...hole(r)} fill="none" stroke={hovered ? '#ffffff' : tone} strokeWidth={hovered ? 2.5 : 2} />
          </g>
        )
      })}
    </svg>
  )
}

const ROLE_LABEL: Record<NonNullable<FocusCaption['role']>, string> = {
  blocking: 'Blocking',
  blockedBy: 'Blocked by',
  unblocked: 'Unblocked',
}

/**
 * Caption pill on the hovered creature. It sits on the card's outer edge (the centre-facing edge
 * is where arrows and attack chevrons leave); the hover preview keeps clear of it. Measured
 * after layout so it can be clamped on-screen.
 */
export function CliqueCaption({
  caption,
  rect,
  isBlocker,
  nameOf,
}: {
  caption: FocusCaption
  rect: Rect
  isBlocker: boolean
  nameOf: (id: EntityId) => string
}) {
  const ref = useRef<HTMLDivElement>(null)
  const [width, setWidth] = useState<number | null>(null)
  const names = caption.others.slice(0, 2).map(nameOf).join(', ')
  const extra = caption.others.length - 2
  const key = `${caption.role}|${names}|${extra}|${caption.bandSize}`

  useLayoutEffect(() => {
    setWidth(ref.current?.offsetWidth ?? null)
  }, [key])

  const below = rect.top + rect.height / 2 > window.innerHeight / 2
  const w = width ?? 0
  // The hover preview opens beside the clique (see cliqueBounds), so the pill can simply centre.
  const idealLeft = rect.left + rect.width / 2 - w / 2
  const left = Math.min(Math.max(idealLeft, VIEWPORT_MARGIN), window.innerWidth - w - VIEWPORT_MARGIN)
  const top = below ? rect.bottom + RING_PAD + 6 : rect.top - RING_PAD - 6

  const roleTone = caption.role === 'unblocked' ? UNBLOCKED_TONE : isBlocker ? BLOCKER_TONE : ATTACKER_TONE
  const style: CSSProperties = {
    position: 'fixed',
    left,
    top,
    transform: below ? undefined : 'translateY(-100%)',
    visibility: width == null ? 'hidden' : 'visible',
    zIndex: 2001,
    pointerEvents: 'none',
    display: 'flex',
    alignItems: 'center',
    gap: 8,
    maxWidth: 440,
    // Chips end flush; plain text needs breathing room before the pill's edge.
    padding: names && caption.bandSize <= 1 ? '4px 12px 4px 4px' : 4,
    borderRadius: 999,
    background: 'rgba(12, 15, 22, 0.94)',
    border: '1px solid rgba(255, 255, 255, 0.12)',
    boxShadow: '0 8px 24px rgba(0, 0, 0, 0.55)',
    color: '#f1f5f9',
    fontFamily: 'system-ui, -apple-system, sans-serif',
    fontSize: 12.5,
    fontWeight: 600,
    whiteSpace: 'nowrap',
    animation: 'combat-caption-in 160ms ease-out',
  }
  const chip = (tone: string): CSSProperties => ({
    flexShrink: 0,
    padding: '3px 8px',
    borderRadius: 999,
    background: `${tone}26`,
    color: tone,
    fontSize: 10.5,
    fontWeight: 700,
    letterSpacing: '0.06em',
    textTransform: 'uppercase',
  })

  return (
    <div ref={ref} style={style} data-testid="combat-focus-caption">
      {caption.role && <span style={chip(roleTone)}>{ROLE_LABEL[caption.role]}</span>}
      {names && (
        <span style={{ overflow: 'hidden', textOverflow: 'ellipsis' }}>
          {names}
          {extra > 0 && <span style={{ color: '#94a3b8' }}> +{extra}</span>}
        </span>
      )}
      {caption.bandSize > 1 && <span style={chip(BAND_TONE)}>Band of {caption.bandSize}</span>}
    </div>
  )
}
