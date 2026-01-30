import { useEffect, useState, useCallback } from 'react'
import { useGameStore, type DamageAnimation } from '../../store/gameStore'

const ANIMATION_DURATION = 800 // ms

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

  // Get target position from DOM - find the player's life display
  const getPosition = () => {
    const targetEl = document.querySelector(`[data-life-display="${animation.targetId}"]`)

    if (targetEl) {
      const rect = targetEl.getBoundingClientRect()
      return {
        x: rect.left + rect.width / 2,
        y: rect.top + rect.height / 2,
      }
    }

    // Fallback to center of screen
    return {
      x: window.innerWidth / 2,
      y: window.innerHeight / 2,
    }
  }

  const { x, y } = getPosition()

  // Animation: float upward and fade out
  const offsetY = -60 * progress // Move up 60px
  const opacity = progress < 0.2 ? progress * 5 : progress > 0.7 ? (1 - progress) * 3.33 : 1
  const scale = 1 + 0.3 * Math.sin(progress * Math.PI) // Pulse effect

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
        zIndex: 10001,
        pointerEvents: 'none',
        fontFamily: 'Impact, Arial Black, sans-serif',
        fontSize: 36,
        fontWeight: 'bold',
        color,
        textShadow: `
          0 0 10px ${glowColor},
          0 0 20px ${glowColor2},
          2px 2px 4px rgba(0, 0, 0, 0.8)
        `,
        WebkitTextStroke: `1px ${strokeColor}`,
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
