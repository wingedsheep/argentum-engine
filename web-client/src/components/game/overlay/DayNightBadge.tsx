import React from 'react'
import { createPortal } from 'react-dom'
import { DayNight } from '@/types/enums'

/**
 * The game's **day/night designation** (Innistrad, CR 731) as a compact game-level badge.
 *
 * Day/night is a single global fact, not a per-player one — so, unlike the speed gauge, exactly one of
 * these renders for the whole table, sitting in the center HUD next to the step strip. It renders
 * nothing while the game is neither day nor night (`designation == null`), which is the state most
 * games stay in forever; the badge only appears once a daybound/nightbound permanent or an effect first
 * establishes a designation (CR 731.1).
 *
 * Hovering opens a portal tooltip explaining how the designation flips — the untap-step check keys off
 * how many spells the previous turn's active player cast (CR 731.2) — because the sun/moon glyph alone
 * doesn't tell a player why it might change.
 *
 * Purely presentational. The designation comes from `ClientGameState.dayNight`; the client never
 * computes it.
 */

const DAY_HUE = '#ffd66b'
const NIGHT_HUE = '#8ea6ff'

export function DayNightBadge({ designation }: { designation?: DayNight | null | undefined }) {
  const badgeRef = React.useRef<HTMLDivElement>(null)
  const [anchor, setAnchor] = React.useState<DOMRect | null>(null)

  if (designation == null) return null

  const isDay = designation === DayNight.DAY
  const hue = isDay ? DAY_HUE : NIGHT_HUE
  const label = isDay ? 'Day' : 'Night'
  const glyph = isDay ? '☀' : '☾'

  return (
    <div
      ref={badgeRef}
      role="img"
      aria-label={`It is ${label.toLowerCase()}`}
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: 5,
        padding: '2px 9px',
        borderRadius: 999,
        cursor: 'help',
        fontSize: 11,
        fontWeight: 700,
        letterSpacing: '0.06em',
        textTransform: 'uppercase',
        color: hue,
        whiteSpace: 'nowrap',
        userSelect: 'none',
        background: isDay
          ? 'linear-gradient(150deg, rgba(60, 44, 12, 0.92) 0%, rgba(28, 20, 6, 0.95) 100%)'
          : 'linear-gradient(150deg, rgba(18, 22, 46, 0.94) 0%, rgba(8, 10, 24, 0.96) 100%)',
        border: `1px solid ${hue}66`,
        boxShadow: `0 0 9px ${hue}33, inset 0 0 8px ${hue}1c`,
      }}
      onMouseEnter={() => setAnchor(badgeRef.current?.getBoundingClientRect() ?? null)}
      onMouseLeave={() => setAnchor(null)}
    >
      <span aria-hidden style={{ fontSize: 13, lineHeight: 1, filter: `drop-shadow(0 0 3px ${hue}cc)` }}>
        {glyph}
      </span>
      <span>{label}</span>

      {anchor && <DayNightTooltip anchor={anchor} isDay={isDay} hue={hue} />}
    </div>
  )
}

const TOOLTIP_WIDTH = 264
const VIEWPORT_PADDING = 8

function DayNightTooltip({ anchor, isDay, hue }: { anchor: DOMRect; isDay: boolean; hue: string }) {
  const vw = window.innerWidth
  const rawLeft = anchor.left + anchor.width / 2 - TOOLTIP_WIDTH / 2
  const left = Math.max(VIEWPORT_PADDING, Math.min(rawLeft, vw - TOOLTIP_WIDTH - VIEWPORT_PADDING))
  const openAbove = anchor.top > window.innerHeight / 2

  return createPortal(
    <div
      style={{
        position: 'fixed',
        left,
        ...(openAbove ? { bottom: window.innerHeight - anchor.top + 6 } : { top: anchor.bottom + 6 }),
        width: TOOLTIP_WIDTH,
        padding: '10px 12px',
        borderRadius: 6,
        zIndex: 2500,
        pointerEvents: 'none',
        textAlign: 'left',
        background: isDay
          ? 'linear-gradient(160deg, rgba(44, 33, 11, 0.98) 0%, rgba(14, 11, 5, 0.98) 100%)'
          : 'linear-gradient(160deg, rgba(14, 17, 38, 0.98) 0%, rgba(6, 7, 18, 0.98) 100%)',
        border: `1px solid ${hue}55`,
        boxShadow: `0 4px 18px rgba(0, 0, 0, 0.55), inset 0 0 14px ${hue}1f`,
        color: '#e8e4dc',
      }}
    >
      <div
        style={{
          display: 'flex',
          alignItems: 'baseline',
          justifyContent: 'space-between',
          gap: 8,
          marginBottom: 8,
          paddingBottom: 6,
          borderBottom: `1px solid ${hue}44`,
        }}
      >
        <span style={{ color: hue, fontWeight: 700, fontSize: 14, letterSpacing: '0.5px' }}>
          {isDay ? 'Day' : 'Night'}
        </span>
      </div>

      <p style={{ margin: '0 0 6px', fontSize: 11.5, lineHeight: 1.4 }}>
        {isDay
          ? 'It becomes night at the start of a turn if the previous turn’s active player cast no spells.'
          : 'It becomes day at the start of a turn if the previous turn’s active player cast two or more spells.'}
      </p>
      <p style={{ margin: 0, fontSize: 11.5, lineHeight: 1.4, color: '#9b968c', fontStyle: 'italic' }}>
        Daybound permanents are front-face up by day; nightbound ones are back-face up by night. They
        transform whenever the designation flips.
      </p>
    </div>,
    document.body,
  )
}
