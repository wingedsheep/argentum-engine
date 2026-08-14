import { useEffect, useRef, useState } from 'react'
import { useGameStore } from '@/store/gameStore.ts'
import { selectGameState } from '@/store/selectors.ts'
import type { EntityId } from '@/types'
import { ZoneType } from '@/types'

interface Point {
  x: number
  y: number
}

interface BondData {
  /** `${a}|${b}` with the ids sorted, so a pair yields one bond however it is walked. */
  key: string
  from: Point
  to: Point
  /** Seconds since this pair first appeared, for the forming flourish. */
  age: number
}

/** Center of a battlefield card's DOM node, or null if it isn't rendered right now. */
function getCardCenter(cardId: EntityId): Point | null {
  const element = document.querySelector(`[data-card-id="${cardId}"]`)
  if (!element) return null
  const rect = element.getBoundingClientRect()
  return { x: rect.left + rect.width / 2, y: rect.top + rect.height / 2 }
}

/** Is this viewport point on-screen? Mirrors CombatArrows — a slid-away board must not paint. */
function isOnScreen(p: Point): boolean {
  return p.x >= -4 && p.x <= window.innerWidth + 4
}

const BOND_COLOR = '#a78bfa'
const BOND_GLOW = '#e9d5ff'
/** Duration of the "bond forming" flourish when a pair first appears. */
const FORM_SECONDS = 0.9

/** Ease-out so the strands snap out fast and settle. */
function easeOut(t: number): number {
  return 1 - (1 - t) * (1 - t) * (1 - t)
}

/**
 * One soulbond link (CR 702.95b): two spirit strands braided between the paired creatures, with
 * motes of light drifting along the bond and a soft pulse at each anchor.
 *
 * The two strands are the same quadratic curve bowed to opposite sides, which reads as a twist
 * without needing a real 3D helix and stays legible at any distance between the two slots. The
 * strand offsets and mote timings derive from [phase] so several bonds on one board don't animate in
 * lockstep.
 *
 * When the pair is brand new ([age] under [FORM_SECONDS]) the strands *draw in* from the creatures
 * toward the middle and a bright ring expands from the meeting point — so the moment of pairing
 * reads as an event, not as two cards silently acquiring a decoration. Once formed it settles into
 * the steady drift, which is what makes it safe to leave on screen indefinitely.
 */
function Bond({ from, to, phase, age }: { from: Point; to: Point; phase: number; age: number }) {
  const midX = (from.x + to.x) / 2
  const midY = (from.y + to.y) / 2
  const dx = to.x - from.x
  const dy = to.y - from.y
  const distance = Math.hypot(dx, dy) || 1

  // Bow perpendicular to the bond so the braid stays symmetric at any angle.
  const bow = Math.min(distance * 0.22, 52)
  const nx = -dy / distance
  const ny = dx / distance

  const strand = (side: 1 | -1) =>
    `M ${from.x} ${from.y} Q ${midX + nx * bow * side} ${midY + ny * bow * side} ${to.x} ${to.y}`

  // Forming: 0 → 1 over FORM_SECONDS, then pinned at 1.
  const formed = age >= FORM_SECONDS ? 1 : easeOut(age / FORM_SECONDS)
  const isForming = formed < 1

  // Draw-in via dash offset. The dash pattern is the (over-)estimated curve length, so offsetting by
  // the un-drawn remainder wipes the strand on from the `from` anchor.
  const curveLength = distance + bow
  const dashProps = isForming
    ? { strokeDasharray: curveLength, strokeDashoffset: curveLength * (1 - formed) }
    : {}

  // Three motes per strand, evenly spaced and offset by the bond's own phase. Suppressed while
  // forming so the draw-in reads cleanly.
  const motes = isForming
    ? []
    : [0, 1, 2].flatMap((i) =>
        ([1, -1] as const).map((side) => {
          const t = (phase + i / 3 + (side === 1 ? 0 : 1 / 6)) % 1
          const mt = 1 - t
          const cx = midX + nx * bow * side
          const cy = midY + ny * bow * side
          return {
            key: `${i}-${side}`,
            x: mt * mt * from.x + 2 * mt * t * cx + t * t * to.x,
            y: mt * mt * from.y + 2 * mt * t * cy + t * t * to.y,
            // Fade in and out over the traverse so motes condense and dissipate.
            opacity: Math.sin(t * Math.PI) * 0.9,
          }
        }),
      )

  // Anchor halos breathe together — the "these two are one" beat. During the flourish they flare
  // instead, brightest at the instant of pairing.
  const breathe = isForming ? 1 : 0.55 + 0.45 * Math.sin(phase * Math.PI * 2)
  const flare = isForming ? 1 - formed : 0

  return (
    <g opacity={isForming ? Math.min(1, formed * 2.5) : 1}>
      {/* Outer glow: both strands, thick and faint */}
      <path d={strand(1)} fill="none" stroke={BOND_GLOW} strokeWidth={9} strokeOpacity={0.14} strokeLinecap="round" {...dashProps} />
      <path d={strand(-1)} fill="none" stroke={BOND_GLOW} strokeWidth={9} strokeOpacity={0.14} strokeLinecap="round" {...dashProps} />
      {/* The braid itself */}
      <path d={strand(1)} fill="none" stroke={BOND_COLOR} strokeWidth={2.5} strokeOpacity={0.85} strokeLinecap="round" {...dashProps} />
      <path d={strand(-1)} fill="none" stroke={BOND_COLOR} strokeWidth={2.5} strokeOpacity={0.85} strokeLinecap="round" {...dashProps} />
      {/* Motes drifting along the strands (steady state only) */}
      {motes.map((m) => (
        <circle key={m.key} cx={m.x} cy={m.y} r={2.6} fill={BOND_GLOW} fillOpacity={m.opacity} />
      ))}
      {/* Forming flourish: a bright ring blooming outward from where the strands meet */}
      {isForming && (
        <circle
          cx={midX}
          cy={midY}
          r={6 + formed * (18 + distance * 0.08)}
          fill="none"
          stroke={BOND_GLOW}
          strokeWidth={2 + flare * 2}
          strokeOpacity={flare * 0.8}
        />
      )}
      {/* Anchor halos on each paired creature */}
      {[from, to].map((p, i) => (
        <g key={`anchor-${i}`}>
          <circle cx={p.x} cy={p.y} r={13 + breathe * 5 + flare * 10} fill={BOND_COLOR} fillOpacity={0.1 * breathe + flare * 0.2} />
          <circle
            cx={p.x}
            cy={p.y}
            r={9}
            fill="none"
            stroke={BOND_GLOW}
            strokeWidth={1.6 + flare * 1.4}
            strokeOpacity={0.35 + breathe * 0.35}
          />
        </g>
      ))}
    </g>
  )
}

/**
 * Overlay drawing a bond between every pair of soulbond-paired creatures on the battlefield
 * (CR 702.95b), so "which two are paired" is readable at a glance instead of buried in oracle text.
 *
 * Pairing is server state (`pairedWithId`, symmetric on both halves), so this component only reads
 * and renders — no client-side notion of who is paired with whom. The forming flourish is likewise
 * derived from state rather than from the `creaturesPaired` event: keyed on when a pair *first
 * appears* in the card map, it plays correctly on a fresh pairing and stays quiet on a reconnect or
 * mid-game spectator join, where the pair already exists and no event is replayed.
 *
 * Positions are re-measured on a timer like `CombatArrows`, because cards move for reasons React
 * doesn't re-render this component for (battlefield reflow, board slide, window resize).
 */
export function SoulbondBonds() {
  const gameState = useGameStore(selectGameState)
  const cards = gameState?.cards
  const [bonds, setBonds] = useState<BondData[]>([])
  const [phase, setPhase] = useState(0)
  /** When each pair key was first seen, in performance.now() ms — drives the forming flourish. */
  const firstSeenRef = useRef(new Map<string, number>())

  useEffect(() => {
    const measure = () => {
      if (!cards) {
        setBonds([])
        return
      }
      const now = performance.now()
      const next: BondData[] = []
      const seen = new Set<string>()

      for (const [idStr, card] of Object.entries(cards)) {
        const id = idStr as EntityId
        const partnerId = card.pairedWithId
        if (!partnerId) continue
        if (card.zone?.zoneType !== ZoneType.BATTLEFIELD) continue
        if (cards[partnerId]?.zone?.zoneType !== ZoneType.BATTLEFIELD) continue

        // One bond per pair: the sorted id pair is the identity.
        const key = id < partnerId ? `${id}|${partnerId}` : `${partnerId}|${id}`
        if (seen.has(key)) continue

        const from = getCardCenter(id)
        const to = getCardCenter(partnerId)
        // Both halves must be visible — a bond with one anchor off a slid-away board would
        // stripe across the screen edge.
        if (!from || !to || !isOnScreen(from) || !isOnScreen(to)) continue

        seen.add(key)
        const firstSeen = firstSeenRef.current.get(key) ?? now
        firstSeenRef.current.set(key, firstSeen)
        next.push({ key, from, to, age: (now - firstSeen) / 1000 })
      }

      // Forget pairs that have ended, so a re-pairing of the same two creatures flourishes again.
      for (const key of firstSeenRef.current.keys()) {
        if (!seen.has(key)) firstSeenRef.current.delete(key)
      }

      setBonds(next)
    }

    measure()
    const interval = setInterval(measure, 100)
    return () => clearInterval(interval)
  }, [cards])

  // Drive the drift/breathe animation. Only runs while something is actually paired.
  useEffect(() => {
    if (bonds.length === 0) return
    let raf = 0
    const start = performance.now()
    const tick = (now: number) => {
      // One full traverse every 2.6s.
      setPhase(((now - start) / 2600) % 1)
      raf = requestAnimationFrame(tick)
    }
    raf = requestAnimationFrame(tick)
    return () => cancelAnimationFrame(raf)
  }, [bonds.length])

  if (bonds.length === 0) return null

  return (
    <svg
      style={{
        position: 'fixed',
        top: 0,
        left: 0,
        width: '100vw',
        height: '100vh',
        pointerEvents: 'none',
        // Under the combat/targeting arrows (2000) — a pair bond is ambient board state and must
        // never obscure "what is attacking what".
        zIndex: 1900,
      }}
    >
      {bonds.map((bond, i) => (
        // Stagger each bond's phase so multiple pairs don't pulse as one.
        <Bond key={bond.key} from={bond.from} to={bond.to} phase={(phase + i * 0.37) % 1} age={bond.age} />
      ))}
    </svg>
  )
}
