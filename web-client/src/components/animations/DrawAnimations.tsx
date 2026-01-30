import { useEffect, useState, useCallback } from 'react'
import { useGameStore, type DrawAnimation } from '../../store/gameStore'
import { useResponsive } from '../../hooks/useResponsive'
import { getCardImageUrl, getScryfallFallbackUrl } from '../../utils/cardImages'

const ANIMATION_DURATION = 500 // ms

/**
 * Single animated card draw.
 */
function DrawAnimationCard({
  animation,
  onComplete,
  cardWidth,
  cardHeight,
}: {
  animation: DrawAnimation
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

  // Easing function for smooth animation
  const easeOutCubic = (t: number) => 1 - Math.pow(1 - t, 3)
  const easedProgress = easeOutCubic(progress)

  const isOpponent = animation.isOpponent

  // Get positions from DOM elements if available
  const getPositions = () => {
    // Find the library element (deck pile)
    const librarySelector = isOpponent ? '[data-zone="opponent-library"]' : '[data-zone="player-library"]'
    const handSelector = isOpponent ? '[data-zone="opponent-hand"]' : '[data-zone="hand"]'

    const libraryEl = document.querySelector(librarySelector)
    const handEl = document.querySelector(handSelector)

    let startX = isOpponent ? window.innerWidth - 80 : window.innerWidth - 80
    let startY = isOpponent ? 100 : window.innerHeight - 100
    let endX = window.innerWidth / 2
    let endY = isOpponent ? 60 : window.innerHeight - cardHeight / 2 - 20

    if (libraryEl) {
      const rect = libraryEl.getBoundingClientRect()
      startX = rect.left + rect.width / 2
      startY = rect.top + rect.height / 2
    }

    if (handEl) {
      const rect = handEl.getBoundingClientRect()
      endX = rect.left + rect.width / 2
      endY = rect.top + rect.height / 2
    }

    return { startX, startY, endX, endY }
  }

  const { startX, startY, endX, endY } = getPositions()

  const currentX = startX + (endX - startX) * easedProgress
  const currentY = startY + (endY - startY) * easedProgress
  const scale = 0.3 + 0.7 * easedProgress
  const opacity = progress < 0.05 ? progress * 20 : progress > 0.85 ? (1 - progress) * 6.67 : 1

  // Get image URL - for player, show the card; for opponent, show card back
  const showCardImage = !isOpponent && (animation.imageUri || animation.cardName)
  const imageUrl = animation.imageUri
    ? getCardImageUrl(animation.imageUri, 'normal')
    : animation.cardName
      ? getScryfallFallbackUrl(animation.cardName, 'normal')
      : null

  return (
    <div
      style={{
        position: 'fixed',
        left: currentX,
        top: currentY,
        transform: `translate(-50%, -50%) scale(${scale})`,
        opacity,
        zIndex: 10000,
        pointerEvents: 'none',
      }}
    >
      {showCardImage && imageUrl && !imageError ? (
        <img
          src={imageUrl}
          alt={animation.cardName || 'Card'}
          onError={() => setImageError(true)}
          style={{
            width: cardWidth,
            height: cardHeight,
            borderRadius: Math.round(cardWidth * 0.05),
            boxShadow: '0 8px 32px rgba(0,0,0,0.6)',
            objectFit: 'cover',
          }}
        />
      ) : showCardImage && animation.cardName ? (
        // Fallback: show card name in a styled box
        <div
          style={{
            width: cardWidth,
            height: cardHeight,
            backgroundColor: '#2a2a3e',
            borderRadius: Math.round(cardWidth * 0.05),
            border: '2px solid #666',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            padding: 8,
            boxShadow: '0 8px 32px rgba(0,0,0,0.6)',
          }}
        >
          <span style={{ color: '#fff', fontSize: 12, textAlign: 'center' }}>
            {animation.cardName}
          </span>
        </div>
      ) : (
        // Card back for opponent or unknown cards
        <div
          style={{
            width: cardWidth,
            height: cardHeight,
            backgroundColor: '#1a1a2e',
            borderRadius: Math.round(cardWidth * 0.05),
            border: '2px solid #4a4a6a',
            backgroundImage:
              'radial-gradient(ellipse at center, #3a3a5e 0%, #1a1a2e 70%)',
            boxShadow: '0 8px 32px rgba(0,0,0,0.6)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
          }}
        >
          <div
            style={{
              width: '60%',
              height: '80%',
              border: '2px solid #5a5a8a',
              borderRadius: Math.round(cardWidth * 0.03),
              backgroundImage:
                'repeating-linear-gradient(45deg, transparent, transparent 5px, rgba(90,90,138,0.3) 5px, rgba(90,90,138,0.3) 10px)',
            }}
          />
        </div>
      )}
    </div>
  )
}

/**
 * Container for all active draw animations.
 */
export function DrawAnimations() {
  const drawAnimations = useGameStore((state) => state.drawAnimations)
  const removeDrawAnimation = useGameStore((state) => state.removeDrawAnimation)
  const responsive = useResponsive()

  const handleComplete = useCallback(
    (id: string) => {
      removeDrawAnimation(id)
    },
    [removeDrawAnimation]
  )

  if (drawAnimations.length === 0) return null

  return (
    <>
      {drawAnimations.map((animation) => (
        <DrawAnimationCard
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
