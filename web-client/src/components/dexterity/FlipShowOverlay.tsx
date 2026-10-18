import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useGameStore } from '@/store/gameStore.ts'
import type { FlipShow } from '@/store/gameStore.ts'
import { ZoneType, type EntityId } from '@/types'
import { getCardImageUrl } from '@/utils/cardImages.ts'
import { FlipTableStage, type CardMark } from './FlipTableStage'
import { FlipOrb } from './FlipOrb'
import { makeFlipView, previewTouches, type Point } from './flipGeometry'
import styles from './Flip.module.css'

const FLIGHT_MS = 1500
/** Where the flight waits, spinning at the top of its arc, if the landing hasn't arrived yet. */
const APEX = 0.48
/** Give up on a landing that never comes (a rejected throw) and hand the table back. */
const LANDING_TIMEOUT_MS = 6000
const AUTO_CLOSE_MS = 9000
/** Half-turns the flight spins with before it knows the real count. */
const PROVISIONAL_HALF_TURNS = 6

const easeInOut = (t: number) => (t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2)

/**
 * The flight and landing of a dexterity flip (Chaos Orb, Falling Star), on every client. The thrower's own
 * client starts it the moment they let go; the server's landing joins mid-air. Everyone else
 * watches it from across the table, replayed from the event.
 */
export function FlipShowOverlay() {
  const show = useGameStore((s) => s.flipShow)
  if (!show) return null
  return <FlipShowView key={show.id} show={show} />
}

function FlipShowView({ show }: { show: FlipShow }) {
  const setFlipShow = useGameStore((s) => s.setFlipShow)
  const gameState = useGameStore((s) => s.gameState)
  const viewerId = useGameStore((s) => s.playerId)
  const view = useMemo(() => makeFlipView(show.table, !show.isYours), [show.table, show.isYours])
  const orbImage = getCardImageUrl(show.sourceName, show.sourceImageUri)
  const reducedMotion = useMemo(() => window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false, [])

  const [landed, setLanded] = useState(false)
  const anchorRef = useRef<HTMLDivElement>(null)
  const orbRef = useRef<HTMLDivElement>(null)
  const shadowRef = useRef<HTMLDivElement>(null)
  // The latest result, read by the animation loop without restarting it.
  const resultRef = useRef(show.result)
  resultRef.current = show.result

  const playerName = useCallback(
    (id: EntityId) => gameState?.players.find((p) => p.playerId === id)?.name ?? 'Opponent',
    [gameState],
  )
  const close = useCallback(() => setFlipShow(null), [setFlipShow])

  // The flight, per frame.
  useEffect(() => {
    const table = show.table
    const start: Point = { x: table.handX, y: table.handY }
    const fallbackTarget: Point = show.aim ?? { x: table.width / 2, y: table.height / 3 }
    const duration = reducedMotion ? 450 : FLIGHT_MS
    let t = 0
    let last = performance.now()
    let angle = 0
    let spinTarget: number | null = null
    let spinFrom = { t: 0, angle: 0 }
    let shownTarget = { ...fallbackTarget }
    let frame = 0
    const began = performance.now()

    const tick = (now: number) => {
      const dt = now - last
      last = now
      const result = resultRef.current
      if (!result && now - began > LANDING_TIMEOUT_MS) {
        close()
        return
      }
      // Hold at the apex until the landing is known.
      t = Math.min(1, t + dt / duration)
      if (!result && t > APEX) t = APEX

      const target = result ? result.landing : fallbackTarget
      // Ease onto a target that changed mid-air rather than jumping to it.
      shownTarget = {
        x: shownTarget.x + (target.x - shownTarget.x) * Math.min(1, dt / 90),
        y: shownTarget.y + (target.y - shownTarget.y) * Math.min(1, dt / 90),
      }
      const e = easeInOut(t)
      const pos = { x: start.x + (shownTarget.x - start.x) * e, y: start.y + (shownTarget.y - start.y) * e }

      // Spin: provisional until the real half-turn count lands, then settle exactly on it.
      if (result && spinTarget === null) {
        let total = result.landing.halfTurns * 180
        if (total < angle) total = angle + ((((total - angle) % 360) + 360) % 360)
        spinTarget = total
        spinFrom = { t, angle }
      }
      if (spinTarget !== null) {
        const span = Math.max(1e-6, 1 - spinFrom.t)
        angle = spinFrom.angle + (spinTarget - spinFrom.angle) * Math.min(1, (t - spinFrom.t) / span)
      } else {
        angle += (PROVISIONAL_HALF_TURNS * 180 * dt) / duration
      }

      const lift = Math.sin(Math.PI * t)
      const restAngle = result ? view.rotation(result.landing.rotation) : view.rotation(0)
      const startAngle = view.rotation(-6)
      const zAngle = startAngle + (restAngle - startAngle) * t
      const s = view.toStage(pos)

      const anchor = anchorRef.current
      if (anchor) { anchor.style.left = `${s.x}px`; anchor.style.top = `${s.y}px` }
      if (orbRef.current) {
        orbRef.current.style.transform =
          `rotateZ(${zAngle}deg) scale(${1 + 0.9 * lift}) rotateX(${angle}deg)`
      }
      const shadow = shadowRef.current
      if (shadow) {
        shadow.style.left = `${s.x + 6 + 34 * lift}px`
        shadow.style.top = `${s.y + 10 + 46 * lift}px`
        shadow.style.opacity = `${0.55 - 0.35 * lift}`
        shadow.style.filter = `blur(${3 + 9 * lift}px)`
        shadow.style.transform = `rotateZ(${zAngle}deg) scale(${1 + 0.35 * lift})`
      }

      if (t >= 1 && result) {
        setLanded(true)
        return
      }
      frame = requestAnimationFrame(tick)
    }
    frame = requestAnimationFrame(tick)
    return () => cancelAnimationFrame(frame)
  }, [show.table, show.aim, view, reducedMotion, close])

  // Once it has landed: Enter/Escape continues, and the table closes itself after a while.
  useEffect(() => {
    if (!landed) return
    const timer = window.setTimeout(close, AUTO_CLOSE_MS)
    const key = (e: KeyboardEvent) => {
      if (e.key === 'Enter' || e.key === 'Escape' || e.key === ' ') { e.preventDefault(); close() }
    }
    window.addEventListener('keydown', key)
    return () => { window.clearTimeout(timer); window.removeEventListener('keydown', key) }
  }, [landed, close])

  const result = landed ? show.result : null
  const cardsById = useMemo(() => new Map(show.table.cards.map((c) => [c.entityId, c])), [show.table])

  const { marks, chips } = useMemo(() => {
    const marks = new Map<EntityId, CardMark>()
    const chips = new Map<EntityId, { text: string; tone: 'danger' | 'warn' | 'calm' }>()
    if (!result) return { marks, chips }
    const affected = new Set(result.affected)
    for (const id of result.touched) {
      if (affected.has(id)) {
        // The table only says what the card hit; the board says what that did. A permanent that
        // left the battlefield was destroyed (Chaos Orb); one still there was struck and survived
        // (Falling Star's 3 damage to a big creature).
        const survived = gameState?.cards[id]?.zone?.zoneType === ZoneType.BATTLEFIELD
        marks.set(id, survived ? 'hit' : 'destroyed')
        chips.set(id, { text: survived ? 'Hit' : 'Destroyed', tone: 'danger' })
      } else {
        marks.set(id, 'spared')
        chips.set(id, { text: !result.turnedOver ? 'Spared' : 'Safe', tone: 'calm' })
      }
    }
    return { marks, chips }
  }, [result, gameState])

  const whose = show.isYours ? 'Your' : `${playerName(show.flipperId)}'s`
  const summary = (() => {
    if (!result) return null
    const names = result.affected.map((id) => {
      const c = cardsById.get(id)
      const name = c?.name ?? 'a face-down permanent'
      return c?.controllerId === viewerId ? `${name} (yours)` : name
    })
    const drift = result.landing.drift ?? 0
    const pull = result.landing.pull ?? 0
    const cm = Math.round(Math.abs(drift) / 10)
    const sideCm = Math.round(Math.abs(pull) / 10)
    // Left and right are the thrower's; anyone else just sees it go wide.
    const side = sideCm < 3 ? null : show.isYours ? `pulled ${sideCm} cm ${pull < 0 ? 'left' : 'right'}` : `pulled ${sideCm} cm wide`
    const depth = cm < 3 ? null : drift < 0 ? `Let go early — it fell ${cm} cm short` : `Let go late — it sailed ${cm} cm long`
    const carry = depth && side ? `${depth} and ${side}. ` : depth ? `${depth}. ` : side ? `The hand swayed — it ${side}. ` : ''
    if (!result.turnedOver) {
      return {
        title: `${whose} ${show.sourceName} didn't turn over`,
        body: `${carry}${result.landing.halfTurns === 0 ? 'It never turned in the air' : 'It only made a half turn in the air'}, so it has no effect on what it touched.`,
      }
    }
    if (names.length > 0) {
      // What the thrower meant to hit: the permanents a card resting square on the aim would cover.
      const { aimX, aimY } = result.landing
      const intended = aimX != null && aimY != null
        ? show.table.cards.filter((c) => c.eligible && previewTouches({ x: aimX, y: aimY }, c)).map((c) => c.entityId)
        : []
      const strayed = intended.length > 0 && !intended.some((id) => result.affected.includes(id))
      const friendlyFire = result.affected.some((id) => cardsById.get(id)?.controllerId === show.flipperId)
      const title = strayed
        ? friendlyFire ? 'Friendly fire!' : 'Off target!'
        : names.length > 1 ? `Direct hit — ${names.length} permanents` : 'Direct hit!'
      return {
        title,
        body: `${carry}Hit: ${names.join(', ')}.`,
      }
    }
    return result.touched.length > 0
      ? { title: 'Nothing to hit', body: `${carry}It only touched permanents ${show.sourceName} doesn't affect.` }
      : { title: 'Missed!', body: `${carry}It came down on bare felt.` }
  })()

  const landingStage = show.result ? view.toStage(show.result.landing) : null
  const callout = (() => {
    if (!result) return null
    if (!result.turnedOver) return { text: 'No full turn!', className: styles.calloutBad }
    if (result.affected.length > 0) return { text: `${result.affected.length} hit`, className: styles.calloutHit }
    return { text: result.touched.length > 0 ? 'No effect' : 'Miss', className: styles.calloutMiss }
  })()

  return (
    <div className={styles.overlay} role="dialog" aria-label={`${whose} ${show.sourceName} flip`} onClick={landed ? close : undefined}>
      <div className={styles.header}>
        <div className={styles.headerArt} style={{ backgroundImage: `url("${orbImage}")` }} />
        <div className={styles.headerText}>
          <div className={styles.title}>{show.isYours ? `Flip ${show.sourceName}` : `${playerName(show.flipperId)} flips ${show.sourceName}`}</div>
          <div className={styles.subtitle}>
            {landed ? 'It has landed.' : show.isYours ? 'It’s in the air…' : 'Here it comes — watch where it lands.'}
          </div>
        </div>
      </div>

      <FlipTableStage
        table={show.table}
        view={view}
        viewerId={viewerId}
        playerName={playerName}
        marks={marks}
        chips={chips}
        className={landed && !reducedMotion ? styles.shake : undefined}
      >
        <div ref={shadowRef} className={styles.orbShadow} />
        {landed && landingStage && <div className={styles.impact} style={{ left: landingStage.x, top: landingStage.y }} />}
        <div ref={anchorRef} className={styles.orbAnchor} style={{ perspective: 700 }}>
          <FlipOrb ref={orbRef} imageUrl={orbImage} />
        </div>
        {callout && landingStage && (
          <div className={`${styles.landingCallout} ${callout.className}`} style={{ left: landingStage.x, top: landingStage.y }}>
            {callout.text}
          </div>
        )}
      </FlipTableStage>

      <div className={styles.footer}>
        {summary ? (
          <>
            <div className={styles.summary} aria-live="polite">
              <div className={styles.summaryTitle}>{summary.title}</div>
              <div className={styles.summaryBody}>{summary.body}</div>
            </div>
            <button className={styles.primaryButton} onClick={close} autoFocus>
              Continue
            </button>
          </>
        ) : (
          <span className={styles.subtitle}>{show.isYours ? 'Flipping…' : 'In the air…'}</span>
        )}
      </div>
    </div>
  )
}
