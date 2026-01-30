import { useState, useMemo } from 'react'
import { useGameStore } from '../../store/gameStore'
import type { EntityId } from '../../types'
import { calculateFittingCardWidth, type ResponsiveSizes } from '../../hooks/useResponsive'
import { getCardImageUrl } from '../../utils/cardImages'

/**
 * Card info for zone selection - works with both visible cards and hidden cards.
 */
export interface ZoneCardInfo {
  id: EntityId
  name: string
  typeLine?: string | undefined
  manaCost?: string | undefined
  imageUri?: string | null | undefined
}

interface ZoneSelectionUIProps {
  /** Title shown at the top (e.g., "Search Your Library", "Choose from Graveyard") */
  title: string
  /** Prompt describing what to select */
  prompt: string
  /** Cards available for selection */
  cards: ZoneCardInfo[]
  /** Minimum number of cards to select */
  minSelections: number
  /** Maximum number of cards to select */
  maxSelections: number
  /** Responsive sizing info */
  responsive: ResponsiveSizes
  /** Called when selection is confirmed */
  onConfirm: (selectedCards: EntityId[]) => void
  /** Optional filter description (e.g., "Basic land card") */
  filterDescription?: string
  /** Whether to show "Fail to Find" button (when minSelections is 0) */
  showFailToFind?: boolean
  /** Text for the confirm button */
  confirmText?: string
  /** Text for the fail to find button */
  failToFindText?: string
  /** Whether to sort cards by type */
  sortByType?: boolean
  /** Use global hover preview (for cards that exist in gameState) */
  useGlobalHover?: boolean
}

/**
 * Shared UI for selecting cards from a zone (library, graveyard, etc.).
 *
 * Features:
 * - Full-screen dark overlay
 * - Horizontal scrolling ribbon of cards
 * - Cards optionally sorted by type/name
 * - Selection counter showing progress
 * - "Fail to Find" button (optional)
 * - "Confirm" button enabled when selection is valid
 * - Selected cards pop up with golden border
 */
export function ZoneSelectionUI({
  title,
  prompt,
  cards,
  minSelections,
  maxSelections,
  responsive,
  onConfirm,
  filterDescription,
  showFailToFind = false,
  confirmText = 'Confirm Selection',
  failToFindText = 'Fail to Find',
  sortByType = true,
  useGlobalHover = false,
}: ZoneSelectionUIProps) {
  const [selectedCards, setSelectedCards] = useState<EntityId[]>([])
  const [hoveredCardId, setHoveredCardId] = useState<EntityId | null>(null)
  const [minimized, setMinimized] = useState(false)
  const hoverCard = useGameStore((s) => s.hoverCard)

  // Handle hover - use global store if enabled
  const handleMouseEnter = (cardId: EntityId) => {
    setHoveredCardId(cardId)
    if (useGlobalHover) {
      hoverCard(cardId)
    }
  }

  const handleMouseLeave = () => {
    setHoveredCardId(null)
    if (useGlobalHover) {
      hoverCard(null)
    }
  }

  // Sort cards by type then name for better browsing
  const sortedCards = useMemo(() => {
    if (!sortByType) return cards

    return [...cards].sort((a, b) => {
      const typeOrder = (typeLine?: string) => {
        if (!typeLine) return 5
        const lower = typeLine.toLowerCase()
        if (lower.includes('land')) return 0
        if (lower.includes('creature')) return 1
        if (lower.includes('instant')) return 2
        if (lower.includes('sorcery')) return 3
        return 4
      }

      const typeCompare = typeOrder(a.typeLine) - typeOrder(b.typeLine)
      if (typeCompare !== 0) return typeCompare

      return a.name.localeCompare(b.name)
    })
  }, [cards, sortByType])

  // Check if selection is valid
  const canConfirm = selectedCards.length >= minSelections && selectedCards.length <= maxSelections

  // Get hovered card info
  const hoveredCard = hoveredCardId ? cards.find((c) => c.id === hoveredCardId) : null

  // Calculate card size that fits available width
  const availableWidth = responsive.viewportWidth - responsive.containerPadding * 2 - 64
  const gap = responsive.isMobile ? 8 : 12
  const maxCardWidth = responsive.isMobile ? 100 : 140
  const cardWidth = calculateFittingCardWidth(
    Math.min(sortedCards.length, 8), // Show up to 8 cards without scrolling
    availableWidth,
    gap,
    maxCardWidth,
    60
  )

  const toggleCard = (cardId: EntityId) => {
    setSelectedCards((prev) => {
      if (prev.includes(cardId)) {
        return prev.filter((id) => id !== cardId)
      }
      // Don't allow selecting more than max
      if (prev.length >= maxSelections) {
        return prev
      }
      return [...prev, cardId]
    })
  }

  const handleConfirm = () => {
    onConfirm(selectedCards)
    setSelectedCards([])
  }

  const handleFailToFind = () => {
    onConfirm([])
    setSelectedCards([])
  }

  // When minimized, show floating button to restore
  if (minimized) {
    return (
      <button
        onClick={() => setMinimized(false)}
        style={{
          position: 'fixed',
          bottom: 70,
          left: '50%',
          transform: 'translateX(-50%)',
          padding: responsive.isMobile ? '10px 16px' : '12px 24px',
          fontSize: responsive.fontSize.normal,
          backgroundColor: '#1e40af',
          color: 'white',
          border: 'none',
          borderRadius: 8,
          cursor: 'pointer',
          fontWeight: 600,
          boxShadow: '0 4px 12px rgba(0,0,0,0.4)',
          zIndex: 100,
          display: 'flex',
          alignItems: 'center',
          gap: 8,
        }}
      >
        ↑ Return to {title}
      </button>
    )
  }

  return (
    <div
      style={{
        position: 'fixed',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        backgroundColor: 'rgba(0, 0, 0, 0.92)',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        gap: responsive.isMobile ? 16 : 24,
        padding: responsive.containerPadding,
        pointerEvents: 'auto',
        zIndex: 1000,
      }}
    >
      {/* Header */}
      <div style={{ textAlign: 'center' }}>
        <h2
          style={{
            color: 'white',
            margin: 0,
            fontSize: responsive.isMobile ? 20 : 28,
            fontWeight: 600,
          }}
        >
          {title}
        </h2>
        <p
          style={{
            color: '#aaa',
            margin: '8px 0 0',
            fontSize: responsive.fontSize.normal,
          }}
        >
          {prompt}
        </p>
      </div>

      {/* Selection counter */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: 16,
          color: '#888',
          fontSize: responsive.fontSize.normal,
        }}
      >
        <span>
          Selected:{' '}
          <span
            style={{
              color: canConfirm ? '#4ade80' : selectedCards.length > 0 ? '#fbbf24' : '#888',
              fontWeight: 600,
            }}
          >
            {selectedCards.length}
          </span>
          {' / '}
          {maxSelections}
        </span>
        {filterDescription && (
          <>
            <span style={{ color: '#666' }}>|</span>
            <span style={{ color: '#666' }}>
              {filterDescription}
            </span>
          </>
        )}
      </div>

      {/* Card ribbon */}
      <div
        style={{
          display: 'flex',
          gap,
          padding: responsive.isMobile ? 12 : 24,
          justifyContent: sortedCards.length <= 6 ? 'center' : 'flex-start',
          overflowX: 'auto',
          maxWidth: '100%',
          scrollBehavior: 'smooth',
        }}
      >
        {sortedCards.map((card) => (
          <ZoneCard
            key={card.id}
            card={card}
            isSelected={selectedCards.includes(card.id)}
            isHovered={hoveredCardId === card.id}
            onClick={() => toggleCard(card.id)}
            cardWidth={cardWidth}
            isMobile={responsive.isMobile}
            onMouseEnter={() => handleMouseEnter(card.id)}
            onMouseLeave={handleMouseLeave}
          />
        ))}
      </div>

      {/* No cards message */}
      {sortedCards.length === 0 && (
        <p style={{ color: '#666', fontSize: responsive.fontSize.normal }}>
          No cards available.
        </p>
      )}

      {/* Action buttons */}
      <div
        style={{
          display: 'flex',
          gap: 16,
          marginTop: 8,
        }}
      >
        {/* View Battlefield button */}
        <button
          onClick={() => setMinimized(true)}
          style={{
            padding: responsive.isMobile ? '10px 20px' : '12px 28px',
            fontSize: responsive.fontSize.normal,
            backgroundColor: '#1e40af',
            color: 'white',
            border: 'none',
            borderRadius: 8,
            cursor: 'pointer',
            transition: 'all 0.15s',
          }}
        >
          View Battlefield
        </button>

        {/* Fail to Find button */}
        {showFailToFind && (
          <button
            onClick={handleFailToFind}
            style={{
              padding: responsive.isMobile ? '10px 20px' : '12px 28px',
              fontSize: responsive.fontSize.normal,
              backgroundColor: '#333',
              color: '#aaa',
              border: '1px solid #555',
              borderRadius: 8,
              cursor: 'pointer',
              transition: 'all 0.15s',
            }}
            onMouseEnter={(e) => {
              e.currentTarget.style.backgroundColor = '#444'
              e.currentTarget.style.color = '#fff'
            }}
            onMouseLeave={(e) => {
              e.currentTarget.style.backgroundColor = '#333'
              e.currentTarget.style.color = '#aaa'
            }}
          >
            {failToFindText}
          </button>
        )}

        {/* Confirm button */}
        <button
          onClick={handleConfirm}
          disabled={!canConfirm || selectedCards.length === 0}
          style={{
            padding: responsive.isMobile ? '10px 24px' : '12px 36px',
            fontSize: responsive.fontSize.large,
            backgroundColor: canConfirm && selectedCards.length > 0 ? '#16a34a' : '#333',
            color: canConfirm && selectedCards.length > 0 ? 'white' : '#666',
            border: 'none',
            borderRadius: 8,
            cursor: canConfirm && selectedCards.length > 0 ? 'pointer' : 'not-allowed',
            fontWeight: 600,
            transition: 'all 0.15s',
          }}
        >
          {confirmText}
        </button>
      </div>

      {/* Card preview on hover (only when not using global hover) */}
      {hoveredCard && !responsive.isMobile && !useGlobalHover && (
        <ZoneCardPreview card={hoveredCard} />
      )}
    </div>
  )
}

/**
 * Individual card in the zone selection UI.
 */
function ZoneCard({
  card,
  isSelected,
  isHovered,
  onClick,
  cardWidth,
  isMobile,
  onMouseEnter,
  onMouseLeave,
}: {
  card: ZoneCardInfo
  isSelected: boolean
  isHovered: boolean
  onClick: () => void
  cardWidth: number
  isMobile: boolean
  onMouseEnter?: () => void
  onMouseLeave?: () => void
}) {
  const cardImageUrl = getCardImageUrl(card.name, card.imageUri)

  const cardRatio = 1.4
  const cardHeight = Math.round(cardWidth * cardRatio)

  // Determine visual state
  const showHoverEffect = isHovered && !isSelected

  return (
    <div
      onClick={onClick}
      onMouseEnter={onMouseEnter}
      onMouseLeave={onMouseLeave}
      style={{
        width: cardWidth,
        height: cardHeight,
        backgroundColor: isSelected ? '#1a3320' : '#1a1a1a',
        border: isSelected
          ? '3px solid #fbbf24'
          : showHoverEffect
            ? '2px solid #666'
            : '2px solid #333',
        borderRadius: isMobile ? 6 : 10,
        display: 'flex',
        flexDirection: 'column',
        overflow: 'hidden',
        cursor: 'pointer',
        transition: 'all 0.2s ease-out',
        transform: isSelected
          ? 'translateY(-12px) scale(1.05)'
          : showHoverEffect
            ? 'translateY(-4px) scale(1.02)'
            : 'none',
        boxShadow: isSelected
          ? '0 12px 28px rgba(251, 191, 36, 0.4), 0 0 20px rgba(251, 191, 36, 0.2)'
          : showHoverEffect
            ? '0 8px 20px rgba(255, 255, 255, 0.15)'
            : '0 4px 12px rgba(0, 0, 0, 0.6)',
        flexShrink: 0,
        position: 'relative',
      }}
    >
      {/* Card image */}
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
          padding: isMobile ? '6px' : '10px',
          gap: 4,
        }}
      >
        <span
          style={{
            color: 'white',
            fontSize: isMobile ? 10 : 12,
            fontWeight: 600,
            textAlign: 'center',
            lineHeight: 1.2,
          }}
        >
          {card.name}
        </span>
        {card.typeLine && (
          <span
            style={{
              color: '#888',
              fontSize: isMobile ? 8 : 10,
              textAlign: 'center',
            }}
          >
            {card.typeLine}
          </span>
        )}
        {card.manaCost && (
          <span
            style={{
              color: '#666',
              fontSize: isMobile ? 8 : 10,
            }}
          >
            {card.manaCost}
          </span>
        )}
      </div>

      {/* Selection indicator */}
      {isSelected && (
        <div
          style={{
            position: 'absolute',
            top: 6,
            right: 6,
            width: 24,
            height: 24,
            backgroundColor: '#fbbf24',
            borderRadius: '50%',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: '#1a1a1a',
            fontWeight: 'bold',
            fontSize: 14,
            boxShadow: '0 2px 8px rgba(0, 0, 0, 0.4)',
          }}
        >
          &#10003;
        </div>
      )}
    </div>
  )
}

/**
 * Card preview overlay - shows enlarged card when hovering.
 */
function ZoneCardPreview({ card }: { card: ZoneCardInfo }) {
  const cardImageUrl = getCardImageUrl(card.name, card.imageUri, 'large')

  const previewWidth = 280
  const previewHeight = Math.round(previewWidth * 1.4)

  return (
    <div
      style={{
        position: 'fixed',
        top: 20,
        right: 20,
        pointerEvents: 'none',
        zIndex: 1001,
      }}
    >
      <div
        style={{
          width: previewWidth,
          height: previewHeight,
          borderRadius: 12,
          overflow: 'hidden',
          boxShadow: '0 8px 32px rgba(0, 0, 0, 0.6)',
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
        />
      </div>
    </div>
  )
}
