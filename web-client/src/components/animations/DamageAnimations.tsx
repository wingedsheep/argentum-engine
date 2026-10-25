import { useEffect, useRef, useState, useCallback } from 'react'
import { useGameStore, type DamageAnimation } from '@/store/gameStore.ts'
import { battlefieldCardElement } from '@/utils/cardAnchor.ts'

/**
 * A player's life swing is the loudest number in the game and gets the longer, bigger treatment.
 * Damage on a permanent is bookkeeping by comparison — several can land at once in one combat —
 * so it runs shorter and smaller, enough to catch the eye without turning a board wipe into a
 * fireworks display.
 */
const PLAYER_DURATION = 800 // ms
const PERMANENT_DURATION = 650 // ms

/**
 * Single animated life change number (damage or life gain).
 */
function LifeChangeAnimationNumber({
  animation,
  onComplete,
}: {
  animation: DamageAnimation
  onComplete: () => void
}) {
  const [progress, setProgress] = useState(0)
  const isPermanent = !animation.targetIsPlayer
  const duration = isPermanent ? PERMANENT_DURATION : PLAYER_DURATION

  useEffect(() => {
    const startDelay = Math.max(0, animation.startTime - Date.now())

    const startAnimation = () => {
      const startTime = Date.now()

      const animate = () => {
        const elapsed = Date.now() - startTime
        const newProgress = Math.min(1, elapsed / duration)
        setProgress(newProgress)

        if (newProgress < 1) {
          requestAnimationFrame(animate)
        } else {
          setTimeout(onComplete, 50)
        }
      }

      requestAnimationFrame(animate)
    }

    const timeoutId = setTimeout(startAnimation, startDelay)
    return () => clearTimeout(timeoutId)
  }, [animation.startTime, duration, onComplete])

  // Re-read on every frame rather than resolved once. Both anchors move: a permanent's card slides
  // and resizes as the board reflows around anything that left it, so a number pinned to where the
  // card *was* drifts onto whichever neighbour took its place.
  const getPosition = () => {
    const targetEl = isPermanent
      ? battlefieldCardElement(animation.targetId)
      : document.querySelector(`[data-life-display="${animation.targetId}"]`)

    if (targetEl) {
      const rect = targetEl.getBoundingClientRect()
      return {
        x: rect.left + rect.width / 2,
        y: rect.top + rect.height / 2,
      }
    }

    // A permanent that vanished mid-flight — bounced, sacrificed, killed by something that
    // resolved after the damage — takes its number with it. Parking the number in the middle of
    // the screen, the way a missing life display falls back to, would be worse than showing none.
    if (isPermanent) return null

    // Fallback to center of screen
    return {
      x: window.innerWidth / 2,
      y: window.innerHeight / 2,
    }
  }

  // Held across frames so the number doesn't jump back a frame's worth of reflow if the card is
  // momentarily unresolvable while React is mid-commit.
  const lastPosition = useRef<{ x: number; y: number } | null>(null)
  const position = getPosition() ?? lastPosition.current
  if (position) lastPosition.current = position
  if (!position) return null
  const { x, y } = position

  // Animation: float upward and fade out. The permanent's number travels a shorter distance —
  // it has to stay over its own card long enough to be read as belonging to it, and a card is
  // a much smaller target than a life display.
  const offsetY = (isPermanent ? -26 : -60) * progress
  const opacity = progress < 0.2 ? progress * 5 : progress > 0.7 ? (1 - progress) * 3.33 : 1
  // The player's number pulses through its whole life; the permanent's just pops once on arrival
  // and then holds still, so a row of them doesn't shimmer.
  const scale = isPermanent
    ? 0.86 + 0.14 * Math.min(1, progress / 0.15)
    : 1 + 0.3 * Math.sin(progress * Math.PI)

  // Different colors for damage vs life gain
  const isLifeGain = animation.isLifeGain
  const color = isLifeGain ? '#33ff33' : '#ff3333'
  const glowColor = isLifeGain ? 'rgba(0, 255, 0, 0.8)' : 'rgba(255, 0, 0, 0.8)'
  const glowColor2 = isLifeGain ? 'rgba(0, 255, 0, 0.6)' : 'rgba(255, 0, 0, 0.6)'
  const strokeColor = isLifeGain ? '#008800' : '#880000'
  const prefix = isLifeGain ? '+' : '-'

  return (
    <div
      style={{
        position: 'fixed',
        left: x,
        top: y + offsetY,
        transform: `translate(-50%, -50%) scale(${scale})`,
        opacity,
        zIndex: 'calc(var(--z-animation) + 1)' as unknown as number,
        pointerEvents: 'none',
        fontFamily: 'Impact, Arial Black, sans-serif',
        // Sized against the card rather than the screen. The glow is dropped entirely here: over
        // card art a hard dark shadow is what makes a small number legible, and the halo the
        // player's number wears would just smear it.
        ...(isPermanent
          ? {
              fontSize: 22,
              fontWeight: 'bold',
              color: '#ff5f5f',
              textShadow: '0 1px 3px rgba(0, 0, 0, 0.95), 0 0 2px rgba(0, 0, 0, 0.9)',
              WebkitTextStroke: '1px rgba(60, 0, 0, 0.9)',
            }
          : {
              fontSize: 36,
              fontWeight: 'bold',
              color,
              textShadow: `
          0 0 10px ${glowColor},
          0 0 20px ${glowColor2},
          2px 2px 4px rgba(0, 0, 0, 0.8)
        `,
              WebkitTextStroke: `1px ${strokeColor}`,
            }),
      }}
    >
      {prefix}{animation.amount}
    </div>
  )
}

/**
 * Container for all active damage/life gain animations.
 */
export function DamageAnimations() {
  const damageAnimations = useGameStore((state) => state.damageAnimations)
  const removeDamageAnimation = useGameStore((state) => state.removeDamageAnimation)

  const handleComplete = useCallback(
    (id: string) => {
      removeDamageAnimation(id)
    },
    [removeDamageAnimation]
  )

  if (damageAnimations.length === 0) return null

  return (
    <>
      {damageAnimations.map((animation) => (
        <LifeChangeAnimationNumber
          key={animation.id}
          animation={animation}
          onComplete={() => handleComplete(animation.id)}
        />
      ))}
    </>
  )
}
