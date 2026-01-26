import { useGameStore } from '../../store/gameStore'
import { useResponsive, calculateFittingCardWidth } from '../../hooks/useResponsive'

/**
 * Overlay that shows opponent's hand cards when a "look at hand" effect resolves.
 * Displays the revealed cards and allows the player to dismiss it.
 */
export function RevealedHandUI() {
  const revealedHandCardIds = useGameStore((state) => state.revealedHandCardIds)
  const gameState = useGameStore((state) => state.gameState)
  const dismissRevealedHand = useGameStore((state) => state.dismissRevealedHand)
  const responsive = useResponsive()

  if (!revealedHandCardIds || !gameState) return null

  // Get card info for each revealed card
  const cards = revealedHandCardIds
    .map((cardId) => gameState.cards[cardId])
    .filter((card) => card != null)

  // Calculate card size that fits all cards
  const availableWidth = responsive.viewportWidth - responsive.containerPadding * 2 - 32
  const gap = responsive.isMobile ? 4 : 8
  const maxCardWidth = responsive.isMobile ? 90 : 130
  const cardWidth = calculateFittingCardWidth(
    cards.length || 1,
    availableWidth,
    gap,
    maxCardWidth,
    45
  )
  const cardHeight = Math.round(cardWidth * 1.4)

  return (
    <div
      style={{
        position: 'fixed',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        backgroundColor: 'rgba(0, 0, 0, 0.85)',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        gap: responsive.isMobile ? 12 : 24,
        padding: responsive.containerPadding,
        pointerEvents: 'auto',
        zIndex: 1000,
      }}
    >
      <h2
        style={{
          color: 'white',
          margin: 0,
          fontSize: responsive.isMobile ? 18 : 24,
          textAlign: 'center',
        }}
      >
        Opponent's Hand
      </h2>

      <p style={{ color: '#888', margin: 0, fontSize: responsive.fontSize.normal }}>
        {cards.length === 0
          ? 'Opponent has no cards in hand'
          : `${cards.length} card${cards.length !== 1 ? 's' : ''}`}
      </p>

      {/* Card display */}
      {cards.length > 0 && (
        <div
          style={{
            display: 'flex',
            gap,
            padding: responsive.isMobile ? 8 : 16,
            justifyContent: 'center',
            flexWrap: 'wrap',
            maxWidth: '100%',
          }}
        >
          {cards.map((card) => {
            const cardImageUrl = `https://api.scryfall.com/cards/named?exact=${encodeURIComponent(card.name)}&format=image&version=normal`
            return (
              <div
                key={card.id}
                style={{
                  width: cardWidth,
                  height: cardHeight,
                  backgroundColor: '#1a1a1a',
                  border: '2px solid #444',
                  borderRadius: responsive.isMobile ? 6 : 10,
                  overflow: 'hidden',
                  flexShrink: 0,
                  position: 'relative',
                }}
              >
                <img
                  src={cardImageUrl}
                  alt={card.name}
                  style={{
                    width: '100%',
                    height: '100%',
                    objectFit: 'cover',
                  }}
                  onError={(e) => {
                    e.currentTarget.style.display = 'none'
                    const fallback = e.currentTarget.nextElementSibling as HTMLElement
                    if (fallback) fallback.style.display = 'flex'
                  }}
                />
                {/* Fallback when image fails */}
                <div
                  style={{
                    position: 'absolute',
                    inset: 0,
                    backgroundColor: '#1a1a1a',
                    display: 'none',
                    flexDirection: 'column',
                    alignItems: 'center',
                    justifyContent: 'center',
                    padding: responsive.isMobile ? '4px' : '8px',
                  }}
                >
                  <span
                    style={{
                      color: 'white',
                      fontSize: responsive.isMobile ? 9 : 11,
                      fontWeight: 500,
                      textAlign: 'center',
                    }}
                  >
                    {card.name}
                  </span>
                </div>
              </div>
            )
          })}
        </div>
      )}

      {/* Dismiss button */}
      <button
        onClick={dismissRevealedHand}
        style={{
          padding: responsive.isMobile ? '10px 20px' : '12px 32px',
          fontSize: responsive.fontSize.large,
          backgroundColor: '#00aa00',
          color: 'white',
          border: 'none',
          borderRadius: 8,
          cursor: 'pointer',
          fontWeight: 600,
        }}
      >
        OK
      </button>
    </div>
  )
}
