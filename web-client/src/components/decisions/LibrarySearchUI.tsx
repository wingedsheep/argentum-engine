import { useState, useMemo } from 'react'
import { useGameStore } from '../../store/gameStore'
import type { EntityId, SearchLibraryDecision, SearchCardInfo } from '../../types'
import { calculateFittingCardWidth, type ResponsiveSizes } from '../../hooks/useResponsive'
import { getCardImageUrl } from '../../utils/cardImages'

interface LibrarySearchUIProps {
  decision: SearchLibraryDecision
  responsive: ResponsiveSizes
}

/**
 * MTG Arena-style library search UI.
 *
 * Features:
 * - Full-screen dark overlay
 * - Horizontal scrolling ribbon of cards
 * - Cards sorted by type/mana value
 * - Valid cards highlighted, all cards visible
 * - Selection counter showing progress
 * - "Fail to Find" button (always available when minSelections is 0)
 * - "Confirm" button enabled when selection is valid
 * - Selected cards pop up with golden border
 */
export function LibrarySearchUI({ decision, responsive }: LibrarySearchUIProps) {
  const [selectedCards, setSelectedCards] = useState<EntityId[]>([])
  const submitDecision = useGameStore((s) => s.submitDecision)

  // Sort cards by type then mana value for better browsing
  const sortedOptions = useMemo(() => {
    return [...decision.options].sort((a, b) => {
      const cardA = decision.cards[a]
      const cardB = decision.cards[b]
      if (!cardA || !cardB) return 0

      // Sort by type line first (lands first, then creatures, then others)
      const typeOrder = (typeLine: string) => {
        const lower = typeLine.toLowerCase()
        if (lower.includes('land')) return 0
        if (lower.includes('creature')) return 1
        if (lower.includes('instant')) return 2
        if (lower.includes('sorcery')) return 3
        return 4
      }

      const typeCompare = typeOrder(cardA.typeLine) - typeOrder(cardB.typeLine)
      if (typeCompare !== 0) return typeCompare

      // Then sort by name alphabetically
      return cardA.name.localeCompare(cardB.name)
    })
  }, [decision.options, decision.cards])

  // Check if selection is valid
  const canConfirm =
    selectedCards.length >= decision.minSelections &&
    selectedCards.length <= decision.maxSelections

  // Can always "fail to find" when minSelections is 0
  const canFailToFind = decision.minSelections === 0

  // Calculate card size that fits available width
  const availableWidth = responsive.viewportWidth - responsive.containerPadding * 2 - 64
  const gap = responsive.isMobile ? 8 : 12
  const maxCardWidth = responsive.isMobile ? 100 : 140
  const cardWidth = calculateFittingCardWidth(
    Math.min(sortedOptions.length, 8), // Show up to 8 cards without scrolling
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
      if (prev.length >= decision.maxSelections) {
        return prev
      }
      return [...prev, cardId]
    })
  }

  const handleConfirm = () => {
    submitDecision(selectedCards)
    setSelectedCards([])
  }

  const handleFailToFind = () => {
    submitDecision([])
    setSelectedCards([])
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
          Search Your Library
        </h2>
        <p
          style={{
            color: '#aaa',
            margin: '8px 0 0',
            fontSize: responsive.fontSize.normal,
          }}
        >
          {decision.prompt}
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
          {decision.maxSelections}
        </span>
        <span style={{ color: '#666' }}>|</span>
        <span style={{ color: '#666' }}>
          Searching for: {decision.filterDescription}
        </span>
      </div>

      {/* Card ribbon */}
      <div
        style={{
          display: 'flex',
          gap,
          padding: responsive.isMobile ? 12 : 24,
          justifyContent: sortedOptions.length <= 6 ? 'center' : 'flex-start',
          overflowX: 'auto',
          maxWidth: '100%',
          scrollBehavior: 'smooth',
        }}
      >
        {sortedOptions.map((cardId) => {
          const cardInfo = decision.cards[cardId]
          return (
            <SearchCard
              key={cardId}
              cardId={cardId}
              cardInfo={cardInfo}
              isSelected={selectedCards.includes(cardId)}
              onClick={() => toggleCard(cardId)}
              cardWidth={cardWidth}
              isMobile={responsive.isMobile}
            />
          )
        })}
      </div>

      {/* No cards message */}
      {sortedOptions.length === 0 && (
        <p style={{ color: '#666', fontSize: responsive.fontSize.normal }}>
          No cards match the search criteria.
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
        {/* Fail to Find button - always available when minSelections is 0 */}
        {canFailToFind && (
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
            Fail to Find
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
          Confirm Selection
        </button>
      </div>
    </div>
  )
}

/**
 * Individual card in the search UI.
 */
function SearchCard({
  cardId: _cardId,
  cardInfo,
  isSelected,
  onClick,
  cardWidth,
  isMobile,
}: {
  cardId: EntityId
  cardInfo: SearchCardInfo | undefined
  isSelected: boolean
  onClick: () => void
  cardWidth: number
  isMobile: boolean
}) {
  const cardName = cardInfo?.name || 'Unknown Card'
  const cardImageUrl = getCardImageUrl(cardName, cardInfo?.imageUri)

  const cardRatio = 1.4
  const cardHeight = Math.round(cardWidth * cardRatio)

  return (
    <div
      onClick={onClick}
      style={{
        width: cardWidth,
        height: cardHeight,
        backgroundColor: isSelected ? '#1a3320' : '#1a1a1a',
        border: isSelected ? '3px solid #fbbf24' : '2px solid #333',
        borderRadius: isMobile ? 6 : 10,
        display: 'flex',
        flexDirection: 'column',
        overflow: 'hidden',
        cursor: 'pointer',
        transition: 'all 0.2s ease-out',
        transform: isSelected ? 'translateY(-12px) scale(1.05)' : 'none',
        boxShadow: isSelected
          ? '0 12px 28px rgba(251, 191, 36, 0.4), 0 0 20px rgba(251, 191, 36, 0.2)'
          : '0 4px 12px rgba(0, 0, 0, 0.6)',
        flexShrink: 0,
        position: 'relative',
      }}
    >
      {/* Card image */}
      <img
        src={cardImageUrl}
        alt={cardName}
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
          {cardName}
        </span>
        {cardInfo?.typeLine && (
          <span
            style={{
              color: '#888',
              fontSize: isMobile ? 8 : 10,
              textAlign: 'center',
            }}
          >
            {cardInfo.typeLine}
          </span>
        )}
        {cardInfo?.manaCost && (
          <span
            style={{
              color: '#666',
              fontSize: isMobile ? 8 : 10,
            }}
          >
            {cardInfo.manaCost}
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

      {/* Hover glow effect */}
      <div
        style={{
          position: 'absolute',
          inset: 0,
          border: '2px solid transparent',
          borderRadius: isMobile ? 6 : 10,
          pointerEvents: 'none',
          transition: 'border-color 0.15s',
        }}
        className="hover-glow"
      />
    </div>
  )
}
