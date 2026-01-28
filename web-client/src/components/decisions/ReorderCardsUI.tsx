import { useState, useCallback } from 'react'
import { useGameStore } from '../../store/gameStore'
import type { EntityId, ReorderLibraryDecision, SearchCardInfo } from '../../types'
import { calculateFittingCardWidth, type ResponsiveSizes } from '../../hooks/useResponsive'
import { getCardImageUrl } from '../../utils/cardImages'

interface ReorderCardsUIProps {
  decision: ReorderLibraryDecision
  responsive: ResponsiveSizes
}

/**
 * UI for reordering cards on top of the library.
 *
 * Features:
 * - Full-screen dark overlay
 * - Horizontal arrangement of cards with clear "TOP" indicator
 * - Drag and drop to reorder cards
 * - "Confirm Order" button to submit the new arrangement
 */
export function ReorderCardsUI({ decision, responsive }: ReorderCardsUIProps) {
  const [orderedCards, setOrderedCards] = useState<EntityId[]>([...decision.cards])
  const [draggedIndex, setDraggedIndex] = useState<number | null>(null)
  const [dragOverIndex, setDragOverIndex] = useState<number | null>(null)
  const submitOrderedDecision = useGameStore((s) => s.submitOrderedDecision)
  const hoverCard = useGameStore((s) => s.hoverCard)

  // Calculate card size that fits available width
  const availableWidth = responsive.viewportWidth - responsive.containerPadding * 2 - 64
  const gap = responsive.isMobile ? 12 : 16
  const maxCardWidth = responsive.isMobile ? 100 : 140
  const cardWidth = calculateFittingCardWidth(
    Math.min(orderedCards.length, 6),
    availableWidth,
    gap,
    maxCardWidth,
    60
  )

  const handleDragStart = useCallback((index: number) => {
    setDraggedIndex(index)
  }, [])

  const handleDragOver = useCallback((e: React.DragEvent, index: number) => {
    e.preventDefault()
    if (draggedIndex !== null && draggedIndex !== index) {
      setDragOverIndex(index)
    }
  }, [draggedIndex])

  const handleDragLeave = useCallback(() => {
    setDragOverIndex(null)
  }, [])

  const handleDrop = useCallback((targetIndex: number) => {
    if (draggedIndex === null || draggedIndex === targetIndex) {
      setDraggedIndex(null)
      setDragOverIndex(null)
      return
    }

    setOrderedCards(prev => {
      const newOrder = [...prev]
      const [draggedCard] = newOrder.splice(draggedIndex, 1) as [EntityId]
      newOrder.splice(targetIndex, 0, draggedCard)
      return newOrder
    })

    setDraggedIndex(null)
    setDragOverIndex(null)
  }, [draggedIndex])

  const handleDragEnd = useCallback(() => {
    setDraggedIndex(null)
    setDragOverIndex(null)
  }, [])

  // Move card left or right (for non-drag interaction)
  const moveCard = useCallback((index: number, direction: 'left' | 'right') => {
    const newIndex = direction === 'left' ? index - 1 : index + 1
    if (newIndex < 0 || newIndex >= orderedCards.length) return

    setOrderedCards(prev => {
      const newOrder = [...prev]
      const [card] = newOrder.splice(index, 1) as [EntityId]
      newOrder.splice(newIndex, 0, card)
      return newOrder
    })
  }, [orderedCards.length])

  const handleConfirm = () => {
    submitOrderedDecision(orderedCards)
  }

  const handleMouseEnter = useCallback((cardId: EntityId) => {
    hoverCard(cardId)
  }, [hoverCard])

  const handleMouseLeave = useCallback(() => {
    hoverCard(null)
  }, [hoverCard])

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
          Reorder Cards
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

      {/* Instruction */}
      <p
        style={{
          color: '#888',
          margin: 0,
          fontSize: responsive.fontSize.small,
          textAlign: 'center',
        }}
      >
        Drag cards to reorder, or use the arrow buttons. The leftmost card will be on top of your library.
      </p>

      {/* Card arrangement with TOP indicator */}
      <div
        style={{
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          gap: 8,
        }}
      >
        {/* TOP indicator */}
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: 8,
            color: '#4ade80',
            fontSize: responsive.fontSize.normal,
            fontWeight: 600,
          }}
        >
          <div
            style={{
              width: 0,
              height: 0,
              borderLeft: '8px solid transparent',
              borderRight: '8px solid transparent',
              borderBottom: '12px solid #4ade80',
            }}
          />
          TOP OF LIBRARY
          <div
            style={{
              width: 0,
              height: 0,
              borderLeft: '8px solid transparent',
              borderRight: '8px solid transparent',
              borderBottom: '12px solid #4ade80',
            }}
          />
        </div>

        {/* Cards */}
        <div
          style={{
            display: 'flex',
            gap,
            padding: responsive.isMobile ? 12 : 24,
            justifyContent: 'center',
            alignItems: 'flex-end',
          }}
        >
          {orderedCards.map((cardId, index) => {
            const cardInfo = decision.cardInfo[cardId]
            const isDragging = draggedIndex === index
            const isDragOver = dragOverIndex === index

            return (
              <div
                key={cardId}
                style={{
                  display: 'flex',
                  flexDirection: 'column',
                  alignItems: 'center',
                  gap: 8,
                }}
              >
                {/* Position indicator */}
                <div
                  style={{
                    color: index === 0 ? '#4ade80' : '#666',
                    fontSize: responsive.fontSize.small,
                    fontWeight: index === 0 ? 600 : 400,
                  }}
                >
                  {index === 0 ? '1st (Top)' : `${index + 1}${getOrdinalSuffix(index + 1)}`}
                </div>

                <ReorderCard
                  cardId={cardId}
                  cardInfo={cardInfo}
                  index={index}
                  cardWidth={cardWidth}
                  isMobile={responsive.isMobile}
                  isDragging={isDragging}
                  isDragOver={isDragOver}
                  canMoveLeft={index > 0}
                  canMoveRight={index < orderedCards.length - 1}
                  onDragStart={() => handleDragStart(index)}
                  onDragOver={(e) => handleDragOver(e, index)}
                  onDragLeave={handleDragLeave}
                  onDrop={() => handleDrop(index)}
                  onDragEnd={handleDragEnd}
                  onMoveLeft={() => moveCard(index, 'left')}
                  onMoveRight={() => moveCard(index, 'right')}
                  onMouseEnter={() => handleMouseEnter(cardId)}
                  onMouseLeave={handleMouseLeave}
                />
              </div>
            )
          })}
        </div>

        {/* BOTTOM indicator */}
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: 8,
            color: '#666',
            fontSize: responsive.fontSize.small,
          }}
        >
          (rightmost = bottom)
        </div>
      </div>

      {/* Confirm button */}
      <button
        onClick={handleConfirm}
        style={{
          padding: responsive.isMobile ? '10px 24px' : '12px 36px',
          fontSize: responsive.fontSize.large,
          backgroundColor: '#16a34a',
          color: 'white',
          border: 'none',
          borderRadius: 8,
          cursor: 'pointer',
          fontWeight: 600,
          transition: 'all 0.15s',
        }}
      >
        Confirm Order
      </button>
    </div>
  )
}

/**
 * Individual card in the reorder UI with drag-and-drop support.
 */
function ReorderCard({
  cardId: _cardId,
  cardInfo,
  index: _index,
  cardWidth,
  isMobile,
  isDragging,
  isDragOver,
  canMoveLeft,
  canMoveRight,
  onDragStart,
  onDragOver,
  onDragLeave,
  onDrop,
  onDragEnd,
  onMoveLeft,
  onMoveRight,
  onMouseEnter,
  onMouseLeave,
}: {
  cardId: EntityId
  cardInfo: SearchCardInfo | undefined
  index: number
  cardWidth: number
  isMobile: boolean
  isDragging: boolean
  isDragOver: boolean
  canMoveLeft: boolean
  canMoveRight: boolean
  onDragStart: () => void
  onDragOver: (e: React.DragEvent) => void
  onDragLeave: () => void
  onDrop: () => void
  onDragEnd: () => void
  onMoveLeft: () => void
  onMoveRight: () => void
  onMouseEnter: () => void
  onMouseLeave: () => void
}) {
  const cardName = cardInfo?.name || 'Unknown Card'
  const cardImageUrl = getCardImageUrl(cardName, cardInfo?.imageUri)

  const cardRatio = 1.4
  const cardHeight = Math.round(cardWidth * cardRatio)

  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        gap: 4,
      }}
    >
      {/* Arrow buttons */}
      <div
        style={{
          display: 'flex',
          gap: 4,
        }}
      >
        <button
          onClick={onMoveLeft}
          disabled={!canMoveLeft}
          style={{
            width: 28,
            height: 28,
            backgroundColor: canMoveLeft ? '#333' : '#1a1a1a',
            color: canMoveLeft ? '#fff' : '#444',
            border: 'none',
            borderRadius: 4,
            cursor: canMoveLeft ? 'pointer' : 'not-allowed',
            fontSize: 14,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
          }}
          title="Move left (towards top)"
        >
          &#8592;
        </button>
        <button
          onClick={onMoveRight}
          disabled={!canMoveRight}
          style={{
            width: 28,
            height: 28,
            backgroundColor: canMoveRight ? '#333' : '#1a1a1a',
            color: canMoveRight ? '#fff' : '#444',
            border: 'none',
            borderRadius: 4,
            cursor: canMoveRight ? 'pointer' : 'not-allowed',
            fontSize: 14,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
          }}
          title="Move right (towards bottom)"
        >
          &#8594;
        </button>
      </div>

      {/* Card */}
      <div
        draggable
        onDragStart={onDragStart}
        onDragOver={onDragOver}
        onDragLeave={onDragLeave}
        onDrop={onDrop}
        onDragEnd={onDragEnd}
        onMouseEnter={onMouseEnter}
        onMouseLeave={onMouseLeave}
        style={{
          width: cardWidth,
          height: cardHeight,
          backgroundColor: '#1a1a1a',
          border: isDragOver ? '3px solid #fbbf24' : '2px solid #333',
          borderRadius: isMobile ? 6 : 10,
          display: 'flex',
          flexDirection: 'column',
          overflow: 'hidden',
          cursor: 'grab',
          transition: 'all 0.2s ease-out',
          transform: isDragging ? 'scale(1.05)' : isDragOver ? 'translateX(8px)' : 'none',
          opacity: isDragging ? 0.7 : 1,
          boxShadow: isDragOver
            ? '0 0 20px rgba(251, 191, 36, 0.5)'
            : '0 4px 12px rgba(0, 0, 0, 0.6)',
          flexShrink: 0,
          position: 'relative',
        }}
      >
        {/* Card image */}
        <img
          src={cardImageUrl}
          alt={cardName}
          draggable={false}
          style={{
            width: '100%',
            height: '100%',
            objectFit: 'cover',
            pointerEvents: 'none',
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

        {/* Drag handle indicator */}
        <div
          style={{
            position: 'absolute',
            bottom: 4,
            left: '50%',
            transform: 'translateX(-50%)',
            display: 'flex',
            gap: 2,
          }}
        >
          <div style={{ width: 4, height: 4, backgroundColor: '#666', borderRadius: 2 }} />
          <div style={{ width: 4, height: 4, backgroundColor: '#666', borderRadius: 2 }} />
          <div style={{ width: 4, height: 4, backgroundColor: '#666', borderRadius: 2 }} />
        </div>
      </div>
    </div>
  )
}

/**
 * Get ordinal suffix for a number (1st, 2nd, 3rd, 4th, etc.)
 */
function getOrdinalSuffix(n: number): string {
  const s = ['th', 'st', 'nd', 'rd']
  const v = n % 100
  return s[(v - 20) % 10] ?? s[v] ?? s[0] ?? 'th'
}
