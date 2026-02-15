import { useEffect, useState, useCallback } from 'react'
import { useGameStore, type CoinFlipAnimation } from '../../store/gameStore'

const ANIMATION_DURATION = 2000 // ms total
const SCALE_IN_DURATION = 200
const HOLD_DURATION = 1400

/**
 * Single coin flip animation — shows the result centered on screen.
 */
function CoinFlipAnimationCard({
  animation,
  onComplete,
}: {
  animation: CoinFlipAnimation
  onComplete: () => void
}) {
  const [progress, setProgress] = useState(0)

  useEffect(() => {
    const startDelay = Math.max(0, animation.startTime - Date.now())

    const startAnimation = () => {
      const startTime = Date.now()

      const animate = () => {
        const elapsed = Date.now() - startTime
        const newProgress = Math.min(1, elapsed / ANIMATION_DURATION)
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
  }, [animation.startTime, onComplete])

  // Phase-based animation
  const scaleInEnd = SCALE_IN_DURATION / ANIMATION_DURATION
  const holdEnd = (SCALE_IN_DURATION + HOLD_DURATION) / ANIMATION_DURATION

  let scale: number
  let opacity: number

  if (progress < scaleInEnd) {
    const t = progress / scaleInEnd
    const eased = 1 - Math.pow(1 - t, 3)
    scale = eased
    opacity = eased
  } else if (progress < holdEnd) {
    scale = 1
    opacity = 1
  } else {
    const t = (progress - holdEnd) / (1 - holdEnd)
    scale = 1
    opacity = 1 - t
  }

  const won = animation.won
  const glowColor = won ? 'rgba(34, 197, 94, 0.5)' : 'rgba(239, 68, 68, 0.5)'
  const borderColor = won ? '#22c55e' : '#ef4444'
  const bgColor = won ? 'rgba(34, 197, 94, 0.15)' : 'rgba(239, 68, 68, 0.15)'
  const textColor = won ? '#4ade80' : '#f87171'
  const resultText = won ? 'WON' : 'LOST'

  return (
    <div
      style={{
        position: 'fixed',
        left: '50%',
        top: '50%',
        transform: `translate(-50%, -50%) scale(${scale})`,
        opacity,
        zIndex: 10002,
        pointerEvents: 'none',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        gap: 8,
      }}
    >
      {/* Glow background */}
      <div
        style={{
          position: 'absolute',
          inset: -30,
          borderRadius: '50%',
          background: `radial-gradient(ellipse at center, ${glowColor} 0%, transparent 70%)`,
          filter: 'blur(15px)',
        }}
      />

      {/* Coin circle */}
      <div
        style={{
          width: 120,
          height: 120,
          borderRadius: '50%',
          border: `3px solid ${borderColor}`,
          backgroundColor: bgColor,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          boxShadow: `0 0 30px ${glowColor}, 0 8px 32px rgba(0, 0, 0, 0.8)`,
        }}
      >
        <span
          style={{
            color: textColor,
            fontSize: 28,
            fontWeight: 800,
            fontFamily: 'Impact, sans-serif',
            letterSpacing: 2,
            textShadow: `0 0 10px ${glowColor}`,
          }}
        >
          {resultText}
        </span>
      </div>

      {/* Source label */}
      <div
        style={{
          backgroundColor: `${borderColor}dd`,
          color: '#fff',
          padding: '4px 12px',
          borderRadius: 4,
          fontSize: 13,
          fontWeight: 600,
          whiteSpace: 'nowrap',
          boxShadow: '0 2px 8px rgba(0, 0, 0, 0.5)',
        }}
      >
        {animation.sourceName} — Coin flip {won ? 'won' : 'lost'}
      </div>
    </div>
  )
}

/**
 * Container for all active coin flip animations.
 */
export function CoinFlipAnimations() {
  const coinFlipAnimations = useGameStore((state) => state.coinFlipAnimations)
  const removeCoinFlipAnimation = useGameStore((state) => state.removeCoinFlipAnimation)

  const handleComplete = useCallback(
    (id: string) => {
      removeCoinFlipAnimation(id)
    },
    [removeCoinFlipAnimation]
  )

  if (coinFlipAnimations.length === 0) return null

  return (
    <>
      {coinFlipAnimations.map((animation) => (
        <CoinFlipAnimationCard
          key={animation.id}
          animation={animation}
          onComplete={() => handleComplete(animation.id)}
        />
      ))}
    </>
  )
}
