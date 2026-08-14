import React from 'react'
import { createPortal } from 'react-dom'
import { useResponsiveContext } from '../board/shared'

/**
 * A player's **speed** (Aetherdrift, CR 702.179) as a compact four-bar tachometer.
 *
 * Renders nothing until the player actually has speed (`speed <= 0`) — most games never involve the
 * mechanic and shouldn't carry a dead badge. From 1 to 4 the bars fill left to right and the whole
 * gauge heats from amber to red, redlining with a pulsing glow at max speed, which is the one
 * threshold the rules care about (CR 702.179e: "max speed" is a speed of exactly 4).
 *
 * Hovering opens a portal tooltip that spells out both halves of the mechanic — how speed rises and
 * what max speed unlocks — because a bare number tells a player nothing about how to move it. The
 * portal is required for the same reason The Ring's is: the badge sits inside the HUD's
 * overflow-clipped life column, which would crop a nested tooltip.
 *
 * Purely presentational. Speed comes from `ClientPlayer.speed`; the client never computes it.
 */

/** CR 702.179e — max speed is a speed of exactly 4, so the gauge has four bars. */
const MAX_SPEED = 4

/** Per-step gauge colour: amber → orange → red-orange → redline. Index 0 = speed 1. */
const STEP_COLORS: readonly string[] = ['#ffc94d', '#ffa22b', '#ff6b2b', '#ff3b30']

const TRACK = 'rgba(255, 255, 255, 0.13)'

export function SpeedGauge({ speed, compact = false }: { speed: number; compact?: boolean }) {
  const responsive = useResponsiveContext()
  const gaugeRef = React.useRef<HTMLDivElement>(null)
  const [anchor, setAnchor] = React.useState<DOMRect | null>(null)

  if (speed <= 0) return null

  const level = Math.min(speed, MAX_SPEED)
  const atMax = level >= MAX_SPEED
  const hue = STEP_COLORS[level - 1] ?? STEP_COLORS[STEP_COLORS.length - 1]!

  const fontSize = compact ? 9 : responsive.fontSize.small
  const barWidth = compact ? 3 : 4
  const barHeight = compact ? 9 : 11

  return (
    <div
      ref={gaugeRef}
      role="img"
      aria-label={`Speed ${level} of ${MAX_SPEED}${atMax ? ' — max speed' : ''}`}
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: compact ? 4 : 5,
        padding: compact ? '1px 5px' : responsive.isMobile ? '2px 6px' : '3px 8px',
        borderRadius: 5,
        cursor: 'help',
        fontSize,
        color: hue,
        whiteSpace: 'nowrap',
        // Dark asphalt backing; the glow tightens and brightens as speed climbs.
        background: 'linear-gradient(150deg, rgba(38, 20, 10, 0.95) 0%, rgba(12, 8, 6, 0.97) 100%)',
        border: `1px solid ${atMax ? hue : 'rgba(255, 160, 60, 0.35)'}`,
        boxShadow: atMax
          ? `0 0 10px 1px ${hue}66, inset 0 0 7px ${hue}44`
          : `0 0 ${3 + level * 2}px ${hue}33, inset 0 1px 1px rgba(0, 0, 0, 0.5)`,
        // Redline pulse — the only animation, reserved for the state that changes card behaviour.
        animation: atMax ? 'argentum-speed-redline 1.6s ease-in-out infinite' : undefined,
      }}
      onMouseEnter={() => setAnchor(gaugeRef.current?.getBoundingClientRect() ?? null)}
      onMouseLeave={() => setAnchor(null)}
    >
      <style>{REDLINE_KEYFRAMES}</style>
      {/* Speedometer glyph, tilted like a needle at speed. */}
      <span aria-hidden style={{ fontSize: fontSize * 1.15, lineHeight: 1, filter: `drop-shadow(0 0 2px ${hue}cc)` }}>
        ⏱
      </span>
      {/* Four rev bars, filling left to right and growing taller like a tachometer. */}
      <span style={{ display: 'flex', alignItems: 'flex-end', gap: 2 }}>
        {Array.from({ length: MAX_SPEED }, (_, i) => {
          const lit = i < level
          return (
            <span
              key={i}
              style={{
                width: barWidth,
                height: barHeight * (0.6 + i * 0.135),
                borderRadius: 1,
                background: lit ? (STEP_COLORS[i] ?? hue) : TRACK,
                boxShadow: lit ? `0 0 4px ${STEP_COLORS[i] ?? hue}bb` : 'none',
              }}
            />
          )
        })}
      </span>
      <span style={{ fontWeight: 700, letterSpacing: '0.4px' }}>
        {atMax ? 'MAX' : level}
      </span>

      {anchor && <SpeedTooltip anchor={anchor} level={level} atMax={atMax} hue={hue} />}
    </div>
  )
}

const REDLINE_KEYFRAMES = `
@keyframes argentum-speed-redline {
  0%, 100% { filter: brightness(1); }
  50% { filter: brightness(1.28); }
}`

const TOOLTIP_WIDTH = 268
const VIEWPORT_PADDING = 8

function SpeedTooltip({
  anchor,
  level,
  atMax,
  hue,
}: {
  anchor: DOMRect
  level: number
  atMax: boolean
  hue: string
}) {
  const vw = window.innerWidth
  const rawLeft = anchor.left + anchor.width / 2 - TOOLTIP_WIDTH / 2
  const left = Math.max(VIEWPORT_PADDING, Math.min(rawLeft, vw - TOOLTIP_WIDTH - VIEWPORT_PADDING))
  // Open away from the nearer viewport edge: the player's own gauge sits low, the opponent's high.
  const openAbove = anchor.top > window.innerHeight / 2

  return createPortal(
    <div
      style={{
        position: 'fixed',
        left,
        ...(openAbove
          ? { bottom: window.innerHeight - anchor.top + 6 }
          : { top: anchor.bottom + 6 }),
        width: TOOLTIP_WIDTH,
        padding: '10px 12px',
        borderRadius: 6,
        zIndex: 2500,
        pointerEvents: 'none',
        textAlign: 'left',
        background: 'linear-gradient(160deg, rgba(30, 17, 9, 0.98) 0%, rgba(9, 7, 6, 0.98) 100%)',
        border: `1px solid ${hue}55`,
        boxShadow: `0 4px 18px rgba(0, 0, 0, 0.55), inset 0 0 14px ${hue}1f`,
        color: '#eadfd2',
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
        <span style={{ color: hue, fontWeight: 700, fontSize: 14, letterSpacing: '0.5px' }}>Speed</span>
        <span style={{ fontSize: 11, color: hue, opacity: 0.9, whiteSpace: 'nowrap' }}>
          {level} / {MAX_SPEED}{atMax ? ' — max speed' : ''}
        </span>
      </div>

      <p style={{ margin: '0 0 6px', fontSize: 11.5, lineHeight: 1.4 }}>
        Your speed increases by 1 the first time one or more opponents lose life during each of your
        turns, up to a maximum of {MAX_SPEED}. It never decreases.
      </p>
      <p
        style={{
          margin: 0,
          fontSize: 11.5,
          lineHeight: 1.4,
          color: atMax ? '#f7e6d4' : '#9b8d7e',
          fontStyle: atMax ? 'normal' : 'italic',
          ...(atMax ? { textShadow: `0 0 6px ${hue}44` } : {}),
        }}
      >
        {atMax
          ? `At max speed, every “Max speed —” ability you control is active.`
          : `“Max speed —” abilities switch on at ${MAX_SPEED}.`}
      </p>
    </div>,
    document.body,
  )
}
