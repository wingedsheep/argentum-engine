import { useEffect, useState, useCallback } from 'react'
import { useGameStore, type RevealAnimation } from '../../store/gameStore'
import { useResponsive } from '../../hooks/useResponsive'
import { getCardImageUrl } from '../../utils/cardImages'

const ANIMATION_DURATION = 2000 // ms total
const SCALE_IN_DURATION = 200
const HOLD_DURATION = 1400

/**
 * Single reveal animation card — shows a brief centered card image
 * when a morph creature is turned face-up.
 */
function RevealAnimationCard({
  animation,
  onComplete,
  cardWidth,
  cardHeight,
}: {
  animation: RevealAnimation
  onComplete: () => void
  cardWidth: number
  cardHeight: number
}) {
  const [progress, setProgress] = useState(0)
  const [imageError, setImageError] = useState(false)

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
    // Scale in phase
    const t = progress / scaleInEnd
    const eased = 1 - Math.pow(1 - t, 3) // easeOutCubic
    scale = eased
    opacity = eased
  } else if (progress < holdEnd) {
    // Hold phase
    scale = 1
    opacity = 1
  } else {
    // Fade out phase
    const t = (progress - holdEnd) / (1 - holdEnd)
    scale = 1
    opacity = 1 - t
  }

  const imageUrl = getCardImageUrl(animation.cardName, animation.imageUri, 'normal')

  // Display slightly larger than normal for emphasis
  const displayWidth = cardWidth * 1.4
  const displayHeight = cardHeight * 1.4

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
          inset: -20,
          borderRadius: 16,
          background: 'radial-gradient(ellipse at center, rgba(147, 51, 234, 0.4) 0%, transparent 70%)',
          filter: 'blur(10px)',
        }}
      />

      {imageUrl && !imageError ? (
        <img
          src={imageUrl}
          alt={animation.cardName}
          onError={() => setImageError(true)}
          style={{
            width: displayWidth,
            height: displayHeight,
            borderRadius: Math.round(displayWidth * 0.05),
            boxShadow: '0 0 30px rgba(147, 51, 234, 0.6), 0 8px 32px rgba(0, 0, 0, 0.8)',
            objectFit: 'cover',
          }}
        />
      ) : (
        <div
          style={{
            width: displayWidth,
            height: displayHeight,
            backgroundColor: '#2a2a3e',
            borderRadius: Math.round(displayWidth * 0.05),
            border: '2px solid #9333ea',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            padding: 12,
            boxShadow: '0 0 30px rgba(147, 51, 234, 0.6), 0 8px 32px rgba(0, 0, 0, 0.8)',
          }}
        >
          <span style={{ color: '#fff', fontSize: 16, textAlign: 'center', fontWeight: 600 }}>
            {animation.cardName}
          </span>
        </div>
      )}

      {/* Card name label */}
      <div
        style={{
          backgroundColor: 'rgba(147, 51, 234, 0.85)',
          color: '#fff',
          padding: '4px 12px',
          borderRadius: 4,
          fontSize: 13,
          fontWeight: 600,
          whiteSpace: 'nowrap',
          boxShadow: '0 2px 8px rgba(0, 0, 0, 0.5)',
        }}
      >
        {animation.cardName} turned face up
      </div>
    </div>
  )
}

/**
 * Container for all active morph reveal animations.
 */
export function RevealAnimations() {
  const revealAnimations = useGameStore((state) => state.revealAnimations)
  const removeRevealAnimation = useGameStore((state) => state.removeRevealAnimation)
  const responsive = useResponsive()

  const handleComplete = useCallback(
    (id: string) => {
      removeRevealAnimation(id)
    },
    [removeRevealAnimation]
  )

  if (revealAnimations.length === 0) return null

  return (
    <>
      {revealAnimations.map((animation) => (
        <RevealAnimationCard
          key={animation.id}
          animation={animation}
          onComplete={() => handleComplete(animation.id)}
          cardWidth={responsive.cardWidth}
          cardHeight={responsive.cardHeight}
        />
      ))}
    </>
  )
}
