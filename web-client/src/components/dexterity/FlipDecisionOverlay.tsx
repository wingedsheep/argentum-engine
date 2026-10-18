import { useCallback, useEffect, useMemo, useRef, useState, type PointerEvent as ReactPointerEvent } from 'react'
import { useGameStore } from '@/store/gameStore.ts'
import type { EntityId, FlipOntoBattlefieldDecision } from '@/types'
import { getCardImageUrl } from '@/utils/cardImages.ts'
import { FlipTableStage, type CardMark, type StagePointerKind } from './FlipTableStage'
import { FlipOrb } from './FlipOrb'
import { carryDrift, carryPoint, driftLabel, makeFlipView, swayPull, previewTouches, releaseQuality, scatterSigma, type Point } from './flipGeometry'
import decisionStyles from '../decisions/DecisionUI.module.css'
import styles from './Flip.module.css'

/** The first full swing of the release meter, up and back down; every swing after it is quicker. */
const FIRST_SWING_MS = 1700
/** The quickest the swing gets, and how fast it gets there (ms of period lost per ms held). */
const FASTEST_SWING_MS = 850
const SWING_SPEEDUP = 0.28
/** One sway of the hand, left and back; out of step with the meter so the two rarely line up. */
const SWAY_MS = 1150
/** How far the hand sways at first (fraction of a full sway), and how fast that widens per ms. */
const SWAY_START = 0.4
const SWAY_GROWTH = 0.00035
/** How long the hand stays steady before it starts to shake. */
const STEADY_MS = 900
const GAUGE_HEIGHT = 220

/** The meter's swing phase (radians) [elapsed] ms in: the period shrinks steadily to its floor. */
function phaseAt(elapsed: number): number {
  const rampEnd = (FIRST_SWING_MS - FASTEST_SWING_MS) / SWING_SPEEDUP
  const ramp = Math.min(elapsed, rampEnd)
  const phase = ((2 * Math.PI) / SWING_SPEEDUP) * Math.log(FIRST_SWING_MS / (FIRST_SWING_MS - SWING_SPEEDUP * ramp))
  return phase + (elapsed > rampEnd ? (2 * Math.PI * (elapsed - rampEnd)) / FASTEST_SWING_MS : 0)
}

/** The meter's strength at [elapsed] ms into the swing: a 0 → 1 → 0 ping-pong that keeps speeding up. */
function meterAt(elapsed: number): number {
  return (1 - Math.cos(phaseAt(elapsed))) / 2
}

/** The hand's sideways sway (-1 left .. 1 right) [elapsed] ms into a swing that began at [phase]. */
function swayAt(elapsed: number, phase: number): number {
  const amp = Math.min(1, SWAY_START + elapsed * SWAY_GROWTH)
  return amp * Math.sin((2 * Math.PI * elapsed) / SWAY_MS + phase)
}

/** The hand's drift (mm) at [elapsed] ms: nearly steady at first, then a growing tremble. */
function tremorAt(elapsed: number): Point {
  const t = elapsed / 1000
  const amp = Math.min(30, 0.8 + Math.max(0, elapsed - STEADY_MS) * 0.016)
  return {
    x: amp * (0.7 * Math.sin(5.1 * t + 0.3) + 0.3 * Math.sin(11.3 * t)),
    y: amp * (0.7 * Math.cos(3.7 * t) + 0.3 * Math.sin(9.1 * t + 1.2)),
  }
}

/**
 * The dexterity-flip mini-game (Chaos Orb, Falling Star), thrower's side: aim over the engine's table, press and hold to swing,
 * let go with the meter in the gold. The table is the server's; the throw is the player's; where
 * it lands is rolled by the server when it receives the throw.
 */
export function FlipDecisionOverlay({ decision }: { decision: FlipOntoBattlefieldDecision }) {
  const submitFlipThrow = useGameStore((s) => s.submitFlipThrow)
  const gameState = useGameStore((s) => s.gameState)
  const viewerId = useGameStore((s) => s.playerId)
  const { table, release } = decision
  const view = useMemo(() => makeFlipView(table, false), [table])
  const sourceName = decision.context.sourceName ?? 'Chaos Orb'
  // The card says what the flip does (destroy, deal damage, …); the table only says what it touches.
  const sourceId = decision.context.sourceId
  const rulesText = (sourceId ? gameState?.cards[sourceId]?.oracleText : null) ||
    'If it turns over completely, it affects what it lands on — yours too.'
  const orbImage = getCardImageUrl(sourceName, decision.flippedImageUri)

  const [aim, setAim] = useState<Point | null>(null)
  const [swinging, setSwinging] = useState(false)
  const [hidden, setHidden] = useState(false)
  /** What the ghost covers mid-swing, where the release would carry it right now. */
  const [swingCovered, setSwingCovered] = useState<readonly EntityId[]>([])
  const swingStart = useRef(0)
  /** Where in its sway the hand starts each swing — different every time. */
  const swayPhase = useRef(0)
  const aimRef = useRef<Point | null>(null)
  const submitted = useRef(false)

  const ghostRef = useRef<HTMLDivElement>(null)
  const ringRef = useRef<HTMLDivElement>(null)
  const crossRef = useRef<HTMLDivElement>(null)
  const needleRef = useRef<HTMLDivElement>(null)
  const readoutRef = useRef<HTMLDivElement>(null)
  const swayDotRef = useRef<HTMLDivElement>(null)
  const orbRef = useRef<HTMLDivElement>(null)

  const playerName = useCallback(
    (id: EntityId) => gameState?.players.find((p) => p.playerId === id)?.name ?? 'Opponent',
    [gameState],
  )

  // Place the crosshair at the aim [p] (table mm), and the ghost and scatter ring where a release
  // of [strength] would carry to — on the aim at the sweet spot, short or long of it otherwise.
  const placeAim = useCallback(
    (p: Point, strength: number, locked: boolean, sway = 0) => {
      const c = view.toStage(p)
      const landsAt = carryPoint(table, release, p, strength, sway)
      const s = view.toStage(landsAt)
      const sigma = scatterSigma(table, release, p, strength)
      const r = 2 * sigma + 31.5
      const quality = releaseQuality(release, strength)
      if (crossRef.current) { crossRef.current.style.left = `${c.x}px`; crossRef.current.style.top = `${c.y}px` }
      if (ghostRef.current) { ghostRef.current.style.left = `${s.x}px`; ghostRef.current.style.top = `${s.y}px` }
      ghostRef.current?.classList.toggle(styles.ghostLocked!, locked)
      const ring = ringRef.current
      if (ring) {
        ring.style.left = `${s.x - r}px`
        ring.style.top = `${s.y - r}px`
        ring.style.width = `${2 * r}px`
        ring.style.height = `${2 * r}px`
        ring.classList.toggle(styles.ringClean!, locked && quality >= 0.999)
        ring.classList.toggle(styles.ringSloppy!, locked && quality < 0.6)
      }
      return landsAt
    },
    [view, table, release],
  )

  // While swinging: run the meter, the trembling hand and the ghost sliding short and long per
  // frame. What the ghost covers only re-renders when it changes.
  useEffect(() => {
    if (!swinging) return
    let frame = 0
    let coveredKey = ''
    const tick = () => {
      const elapsed = performance.now() - swingStart.current
      const v = meterAt(elapsed)
      const sway = swayAt(elapsed, swayPhase.current)
      const base = aimRef.current
      if (base) {
        const d = tremorAt(elapsed)
        const landsAt = placeAim({ x: base.x + d.x, y: base.y + d.y }, v, true, sway)
        const now = table.cards.filter((c) => previewTouches(landsAt, c)).map((c) => c.entityId)
        const key = now.join(',')
        if (key !== coveredKey) {
          coveredKey = key
          setSwingCovered(now)
        }
      }
      if (needleRef.current) needleRef.current.style.top = `${(1 - v) * GAUGE_HEIGHT}px`
      if (readoutRef.current) {
        const drift = carryDrift(release, v)
        const pull = swayPull(release, sway)
        readoutRef.current.textContent = driftLabel(drift, pull)
        readoutRef.current.dataset.tone = Math.hypot(drift, pull) < 20 ? 'good' : 'off'
      }
      const dot = swayDotRef.current
      if (dot) {
        dot.style.left = `${50 + 50 * sway}%`
        dot.dataset.tone = Math.abs(sway) < 0.18 ? 'good' : 'off'
      }
      if (orbRef.current) {
        orbRef.current.style.transform = `translateY(${-v * 26}px) rotateX(${v * 38}deg) rotateZ(-6deg) scale(${1 + v * 0.08})`
      }
      frame = requestAnimationFrame(tick)
    }
    frame = requestAnimationFrame(tick)
    return () => cancelAnimationFrame(frame)
  }, [swinging, placeAim, table, release])

  useEffect(() => {
    if (orbRef.current) orbRef.current.style.transform = 'rotateZ(-6deg)'
  }, [])

  // Idle aim: show where a perfect release would land.
  useEffect(() => {
    if (!swinging && aim) placeAim(aim, release.sweetSpot, false)
  }, [aim, swinging, placeAim, release])

  const startSwing = useCallback((p: Point) => {
    if (submitted.current) return
    aimRef.current = p
    setAim(p)
    swayPhase.current = Math.random() * 2 * Math.PI
    swingStart.current = performance.now()
    setSwinging(true)
  }, [])

  const cancelSwing = useCallback(() => {
    setSwinging(false)
    if (orbRef.current) orbRef.current.style.transform = 'rotateZ(-6deg)'
  }, [])

  const releaseSwing = useCallback(() => {
    if (submitted.current || !aimRef.current) return
    const elapsed = performance.now() - swingStart.current
    const strength = meterAt(elapsed)
    const sway = swayAt(elapsed, swayPhase.current)
    const d = tremorAt(elapsed)
    submitted.current = true
    setSwinging(false)
    submitFlipThrow(decision.id, aimRef.current.x + d.x, aimRef.current.y + d.y, strength, sway)
  }, [decision.id, submitFlipThrow])

  const onPointer = (kind: StagePointerKind, p: Point, e: ReactPointerEvent<HTMLDivElement>) => {
    if (submitted.current) return
    switch (kind) {
      case 'move':
        if (swinging) aimRef.current = p
        else setAim(p)
        break
      case 'down':
        if (e.button === 2) { cancelSwing(); break }
        if (e.button !== 0) break
        e.currentTarget.setPointerCapture(e.pointerId)
        startSwing(p)
        break
      case 'up':
        if (swinging) releaseSwing()
        break
      case 'cancel':
        cancelSwing()
        break
      case 'leave':
        if (!swinging) setAim(null)
        break
    }
  }

  // Keyboard: Space/Enter swings at the current aim (the table's middle by default), Esc stops.
  useEffect(() => {
    const down = (e: KeyboardEvent) => {
      if (e.repeat) return
      if ((e.key === ' ' || e.key === 'Enter') && !swinging && !hidden) {
        e.preventDefault()
        startSwing(aimRef.current ?? aim ?? { x: table.width / 2, y: table.bands[0]!.top + table.bands[0]!.height / 2 })
      } else if (e.key === 'Escape' && swinging) {
        e.preventDefault()
        cancelSwing()
      }
    }
    const up = (e: KeyboardEvent) => {
      if ((e.key === ' ' || e.key === 'Enter') && swinging) {
        e.preventDefault()
        releaseSwing()
      }
    }
    window.addEventListener('keydown', down)
    window.addEventListener('keyup', up)
    return () => {
      window.removeEventListener('keydown', down)
      window.removeEventListener('keyup', up)
    }
  }, [swinging, hidden, aim, table, startSwing, cancelSwing, releaseSwing])

  // What the ghost currently covers, for the highlight and the label under it.
  const covered = useMemo(() => {
    if (swinging) {
      const ids = new Set(swingCovered)
      return table.cards.filter((c) => ids.has(c.entityId))
    }
    return aim ? table.cards.filter((c) => previewTouches(aim, c)) : []
  }, [aim, table, swinging, swingCovered])
  const marks = useMemo(() => {
    const m = new Map<EntityId, CardMark>()
    for (const c of covered) m.set(c.entityId, !c.eligible ? 'immune' : c.controllerId === viewerId ? 'own' : 'target')
    return m
  }, [covered, viewerId])
  const chips = useMemo(() => {
    const m = new Map<EntityId, { text: string; tone: 'danger' | 'warn' | 'calm' }>()
    for (const c of covered) {
      if (!c.eligible) m.set(c.entityId, { text: 'Safe', tone: 'calm' })
      else if (c.controllerId === viewerId) m.set(c.entityId, { text: 'Yours', tone: 'warn' })
    }
    return m
  }, [covered, viewerId])

  const aimLabel = (() => {
    if (!aim || swinging) return null
    const hits = covered.filter((c) => c.eligible)
    if (hits.length === 0) return covered.length > 0 ? `Nothing ${sourceName} affects here` : 'Bare felt'
    return hits.map((c) => (c.name ?? 'Face-down permanent') + (c.controllerId === viewerId ? ' (yours)' : '')).join(' · ')
  })()

  if (hidden) {
    return (
      <button className={decisionStyles.floatingReturnButton} onClick={() => setHidden(false)}>
        Return to the {sourceName} flip
      </button>
    )
  }

  const hand = view.toStage({ x: table.handX, y: table.handY })
  // The gauge stands just off the table's right-hand edge, level with the hand.
  const gaugeX = view.toStage({ x: table.width, y: 0 }).x + 26
  const soft = release.tooSoftBelow
  const stageAim = aim ? view.toStage(aim) : null

  return (
    <div className={styles.overlay} role="dialog" aria-label={`Flip ${sourceName}`}>
      <div className={styles.header}>
        <div className={styles.headerArt} style={{ backgroundImage: `url("${orbImage}")` }} />
        <div className={styles.headerText}>
          <div className={styles.title}>Flip {sourceName}</div>
          <div className={styles.subtitle}>{rulesText}</div>
        </div>
        <div className={styles.headerSpacer} />
        <button className={styles.ghostButton} onClick={() => setHidden(true)} disabled={swinging}>
          View battlefield
        </button>
      </div>

      <FlipTableStage
        table={table}
        view={view}
        viewerId={viewerId}
        playerName={playerName}
        marks={marks}
        chips={chips}
        className={styles.aiming}
        onPointer={onPointer}
      >
        <div ref={ringRef} className={styles.ring} style={{ display: aim ? undefined : 'none' }} />
        <div ref={ghostRef} className={styles.ghost} style={{ display: aim ? undefined : 'none' }} />
        <div ref={crossRef} className={styles.crosshair} style={{ display: aim ? undefined : 'none' }} />
        {stageAim && aimLabel && (
          <div className={styles.aimLabel} style={{ left: stageAim.x, top: stageAim.y }}>
            {aimLabel}
            <span className={styles.aimHint}>Pressing starts the swing</span>
          </div>
        )}

        <div className={styles.orbAnchor} style={{ left: hand.x, top: hand.y, perspective: 700 }}>
          <FlipOrb ref={orbRef} imageUrl={orbImage} className={styles.orbInHand} faceClassName={styles.orbGlow} />
        </div>

        <div className={styles.sway} style={{ left: hand.x, top: hand.y + 54, opacity: swinging ? 1 : 0.35 }} aria-hidden>
          <span className={styles.swayEnd}>left</span>
          <div className={styles.swayTrack}>
            <div className={styles.swayCentre} />
            <div ref={swayDotRef} className={styles.swayDot} style={{ left: '50%' }} />
          </div>
          <span className={styles.swayEnd}>right</span>
        </div>

        <div className={styles.gauge} style={{ left: gaugeX, top: hand.y + 44 - GAUGE_HEIGHT, height: GAUGE_HEIGHT }}>
          <div className={styles.gaugeTrack}>
            <div className={`${styles.gaugeZone} ${styles.zoneSoft}`} style={{ top: (1 - soft) * GAUGE_HEIGHT, bottom: 5 }} />
            <div
              className={`${styles.gaugeZone} ${styles.zoneClean}`}
              style={{ top: (1 - release.cleanTo) * GAUGE_HEIGHT, height: (release.cleanTo - release.cleanFrom) * GAUGE_HEIGHT }}
            />
          </div>
          <div ref={needleRef} className={styles.gaugeNeedle} style={{ top: GAUGE_HEIGHT, opacity: swinging ? 1 : 0.35 }} />
          <div className={styles.gaugeSweet} style={{ top: (1 - release.sweetSpot) * GAUGE_HEIGHT }} />
          <div className={styles.gaugeTick} style={{ top: 6 }}>long</div>
          <div className={styles.gaugeTick} style={{ top: (1 - release.sweetSpot) * GAUGE_HEIGHT, color: '#ffe9a8' }}>
            on target
          </div>
          <div className={styles.gaugeTick} style={{ top: (1 - (release.sweetSpot + soft) / 2) * GAUGE_HEIGHT }}>short</div>
          <div className={styles.gaugeTick} style={{ top: GAUGE_HEIGHT - 4 }}>no turn</div>
          <div ref={readoutRef} className={styles.gaugeReadout}>{swinging ? '' : 'carry'}</div>
        </div>
      </FlipTableStage>

      <div className={styles.footer}>
        <div className={styles.steps} aria-live="polite">
          <span className={`${styles.step} ${!swinging && !aim ? styles.stepActive : ''}`}>
            <span className={styles.stepNum}>1</span>Aim over the table
          </span>
          <span className={`${styles.step} ${!swinging && aim ? styles.stepActive : ''}`}>
            <span className={styles.stepNum}>2</span>Press and hold — the swing starts at once
          </span>
          <span className={`${styles.step} ${swinging ? styles.stepActive : ''}`}>
            <span className={styles.stepNum}>3</span>Let go on the gold line, hand centred
          </span>
        </div>
        <span className={styles.subtitle}>
          {swinging
            ? 'Early falls short, late sails long · your hand sways left and right too · Esc to stop'
            : 'Aim first — your first click starts the swing · Space works too'}
        </span>
      </div>
    </div>
  )
}
