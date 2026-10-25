import { useEffect, useState, useCallback } from 'react'
import { useGameStore, type TargetReselectedAnimation } from '@/store/gameStore.ts'

const ANIMATION_DURATION = 2000 // ms — longer than damage since it has text to read

/**
 * Single animated target reselection notification.
 * Appears near the stack area showing "Source → Old Target ➜ New Target".
 */
function TargetReselectedNotification({
  animation,
  onComplete,
  index,
}: {
  animation: TargetReselectedAnimation
  onComplete: () => void
  index: number
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

  // Fade in quickly, hold, then fade out
  const opacity =
    progress < 0.1 ? progress * 10 :
    progress > 0.75 ? (1 - progress) * 4 :
    1
  // Slide in from left
  const slideX = progress < 0.1 ? -20 * (1 - progress * 10) : 0
  // Stack offset for multiple simultaneous animations
  const yOffset = index * 56

  return (
    <div
      style={{
        position: 'fixed',
        left: 16 + slideX,
        top: 120 + yOffset,
        zIndex: 'calc(var(--z-animation) + 1)' as unknown as number,
        pointerEvents: 'none',
        opacity,
        display: 'flex',
        flexDirection: 'column',
        gap: 2,
        padding: '8px 14px',
        borderRadius: 8,
        backgroundColor: 'rgba(180, 60, 0, 0.85)',
        border: '1px solid rgba(255, 140, 0, 0.6)',
        boxShadow: '0 0 16px rgba(255, 100, 0, 0.4), 0 2px 8px rgba(0, 0, 0, 0.5)',
        maxWidth: 300,
      }}
    >
      <div
        style={{
          fontSize: 11,
          fontWeight: 600,
          color: 'rgba(255, 200, 100, 0.9)',
          textTransform: 'uppercase',
          letterSpacing: 0.5,
        }}
      >
        {animation.sourceName}
      </div>
      <div style={{ fontSize: 13, color: '#fff', lineHeight: 1.3 }}>
        <span style={{ color: '#ffaa66' }}>{animation.spellOrAbilityName}</span>
        {': '}
        <span style={{ color: '#ff8888', textDecoration: 'line-through', opacity: 0.7 }}>{animation.oldTargetName}</span>
        {' \u2192 '}
        <span style={{ color: '#88ff88', fontWeight: 600 }}>{animation.newTargetName}</span>
      </div>
    </div>
  )
}

/**
 * Container for all active target reselection animations.
 */
export function TargetReselectedAnimations() {
  const targetReselectedAnimations = useGameStore((state) => state.targetReselectedAnimations)
  const removeTargetReselectedAnimation = useGameStore((state) => state.removeTargetReselectedAnimation)

  const handleComplete = useCallback(
    (id: string) => {
      removeTargetReselectedAnimation(id)
    },
    [removeTargetReselectedAnimation]
  )

  if (targetReselectedAnimations.length === 0) return null

  return (
    <>
      {targetReselectedAnimations.map((animation, index) => (
        <TargetReselectedNotification
          key={animation.id}
          animation={animation}
          index={index}
          onComplete={() => handleComplete(animation.id)}
        />
      ))}
    </>
  )
}
