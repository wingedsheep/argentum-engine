import { useState, useCallback } from 'react'
import { useGameStore } from '../../store/gameStore'
import type { EntityId, OrderObjectsDecision, SearchCardInfo, ClientCard } from '../../types'
import { calculateFittingCardWidth, type ResponsiveSizes } from '../../hooks/useResponsive'
import { getCardImageUrl } from '../../utils/cardImages'

interface OrderBlockersUIProps {
  decision: OrderObjectsDecision
  responsive: ResponsiveSizes
}

/**
 * UI for ordering blockers for damage assignment.
 *
 * Per MTG CR 509.2, when an attacker is blocked by multiple creatures,
 * the attacking player must declare the order in which blockers receive damage.
 * The first creature in the order receives damage first, and must receive
 * lethal damage before the next can receive any.
 *
 * Features:
 * - Full-screen dark overlay
 * - Horizontal arrangement of blockers with clear FIRST/LAST indicators
 * - Drag and drop to reorder
 * - "Confirm Order" button to submit the arrangement
 */
export function OrderBlockersUI({ decision, responsive }: OrderBlockersUIProps) {
  const [orderedBlockers, setOrderedBlockers] = useState<EntityId[]>([...decision.objects])
  const [draggedIndex, setDraggedIndex] = useState<number | null>(null)
  const [dragOverIndex, setDragOverIndex] = useState<number | null>(null)
  const submitOrderedDecision = useGameStore((s) => s.submitOrderedDecision)
  const gameState = useGameStore((s) => s.gameState)

  // Get the attacker card from game state using sourceId from decision context
  const attackerId = decision.context.sourceId
  const attackerCard = attackerId && gameState ? gameState.cards[attackerId] : null

  // Calculate card size that fits available width
  const availableWidth = responsive.viewportWidth - responsive.containerPadding * 2 - 64
  const gap = responsive.isMobile ? 12 : 16
  const maxCardWidth = responsive.isMobile ? 100 : 140
  const cardWidth = calculateFittingCardWidth(
    Math.min(orderedBlockers.length, 6),
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

    setOrderedBlockers(prev => {
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
    if (newIndex < 0 || newIndex >= orderedBlockers.length) return

    setOrderedBlockers(prev => {
      const newOrder = [...prev]
      const [card] = newOrder.splice(index, 1) as [EntityId]
      newOrder.splice(newIndex, 0, card)
      return newOrder
    })
  }, [orderedBlockers.length])

  const handleConfirm = () => {
    submitOrderedDecision(orderedBlockers)
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
          Order Damage Assignment
        </h2>
      </div>

      {/* Attacker Card Display */}
      {attackerCard && (
        <AttackerCardDisplay
          card={attackerCard}
          isMobile={responsive.isMobile}
        />
      )}

      {/* Instruction */}
      <p
        style={{
          color: '#888',
          margin: 0,
          fontSize: responsive.fontSize.small,
          textAlign: 'center',
          maxWidth: 500,
        }}
      >
        Order the blockers for damage assignment. The leftmost creature receives damage first and must receive lethal damage before the next can be assigned any.
      </p>

      {/* Card arrangement with FIRST/LAST indicators */}
      <div
        style={{
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          gap: 8,
        }}
      >
        {/* FIRST indicator */}
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: 8,
            color: '#f87171',
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
              borderBottom: '12px solid #f87171',
            }}
          />
          FIRST TO RECEIVE DAMAGE
          <div
            style={{
              width: 0,
              height: 0,
              borderLeft: '8px solid transparent',
              borderRight: '8px solid transparent',
              borderBottom: '12px solid #f87171',
            }}
          />
        </div>

        {/* Blockers */}
        <div
          style={{
            display: 'flex',
            gap,
            padding: responsive.isMobile ? 12 : 24,
            justifyContent: 'center',
            alignItems: 'flex-end',
          }}
        >
          {orderedBlockers.map((blockerId, index) => {
            const cardInfo = decision.cardInfo?.[blockerId]
            const isDragging = draggedIndex === index
            const isDragOver = dragOverIndex === index

            return (
              <div
                key={blockerId}
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
                    color: index === 0 ? '#f87171' : '#666',
                    fontSize: responsive.fontSize.small,
                    fontWeight: index === 0 ? 600 : 400,
                  }}
                >
                  {index === 0 ? '1st' : `${index + 1}${getOrdinalSuffix(index + 1)}`}
                </div>

                <BlockerCard
                  blockerId={blockerId}
                  cardInfo={cardInfo}
                  index={index}
                  cardWidth={cardWidth}
                  isMobile={responsive.isMobile}
                  isDragging={isDragging}
                  isDragOver={isDragOver}
                  canMoveLeft={index > 0}
                  canMoveRight={index < orderedBlockers.length - 1}
                  onDragStart={() => handleDragStart(index)}
                  onDragOver={(e) => handleDragOver(e, index)}
                  onDragLeave={handleDragLeave}
                  onDrop={() => handleDrop(index)}
                  onDragEnd={handleDragEnd}
                  onMoveLeft={() => moveCard(index, 'left')}
                  onMoveRight={() => moveCard(index, 'right')}
                />
              </div>
            )
          })}
        </div>

        {/* LAST indicator */}
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: 8,
            color: '#666',
            fontSize: responsive.fontSize.small,
          }}
        >
          (rightmost = last to receive damage)
        </div>
      </div>

      {/* Confirm button */}
      <button
        onClick={handleConfirm}
        style={{
          padding: responsive.isMobile ? '10px 24px' : '12px 36px',
          fontSize: responsive.fontSize.large,
          backgroundColor: '#dc2626',
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
 * Individual blocker card with drag-and-drop support.
 */
function BlockerCard({
  blockerId: _blockerId,
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
}: {
  blockerId: EntityId
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
}) {
  const cardName = cardInfo?.name || 'Unknown Card'
  // Use the imageUri from cardInfo if available, otherwise fall back to Scryfall API
  const cardImageUrl = cardInfo?.imageUri || `https://api.scryfall.com/cards/named?exact=${encodeURIComponent(cardName)}&format=image&version=normal`

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
          title="Move left (receives damage earlier)"
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
          title="Move right (receives damage later)"
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
        style={{
          width: cardWidth,
          height: cardHeight,
          backgroundColor: '#1a1a1a',
          border: isDragOver ? '3px solid #f87171' : '2px solid #333',
          borderRadius: isMobile ? 6 : 10,
          display: 'flex',
          flexDirection: 'column',
          overflow: 'hidden',
          cursor: 'grab',
          transition: 'all 0.2s ease-out',
          transform: isDragging ? 'scale(1.05)' : isDragOver ? 'translateX(8px)' : 'none',
          opacity: isDragging ? 0.7 : 1,
          boxShadow: isDragOver
            ? '0 0 20px rgba(248, 113, 113, 0.5)'
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

/**
 * Displays the attacker card with its current battlefield state.
 * Shows the card image, name, and power/toughness with damage marked.
 */
function AttackerCardDisplay({
  card,
  isMobile,
}: {
  card: ClientCard
  isMobile: boolean
}) {
  const cardWidth = isMobile ? 120 : 160
  const cardHeight = Math.round(cardWidth * 1.4)
  const imageUrl = getCardImageUrl(card.name, card.imageUri)

  // Calculate effective toughness (remaining after damage)
  const hasDamage = card.damage && card.damage > 0
  const effectiveToughness = card.toughness !== null && card.damage
    ? card.toughness - card.damage
    : card.toughness

  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        gap: 8,
      }}
    >
      {/* Label */}
      <div
        style={{
          color: '#fbbf24',
          fontSize: isMobile ? 12 : 14,
          fontWeight: 600,
          textTransform: 'uppercase',
          letterSpacing: 1,
        }}
      >
        Attacker
      </div>

      {/* Card container */}
      <div
        style={{
          position: 'relative',
          width: cardWidth,
          height: cardHeight,
        }}
      >
        {/* Card image */}
        <div
          style={{
            width: cardWidth,
            height: cardHeight,
            backgroundColor: '#1a1a1a',
            border: '3px solid #fbbf24',
            borderRadius: isMobile ? 6 : 10,
            overflow: 'hidden',
            boxShadow: '0 0 20px rgba(251, 191, 36, 0.3), 0 4px 12px rgba(0, 0, 0, 0.6)',
          }}
        >
          <img
            src={imageUrl}
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
          {/* Fallback */}
          <div
            style={{
              position: 'absolute',
              inset: 0,
              backgroundColor: '#1a1a1a',
              display: 'none',
              flexDirection: 'column',
              alignItems: 'center',
              justifyContent: 'center',
              padding: isMobile ? 8 : 12,
              gap: 4,
            }}
          >
            <span
              style={{
                color: 'white',
                fontSize: isMobile ? 12 : 14,
                fontWeight: 600,
                textAlign: 'center',
              }}
            >
              {card.name}
            </span>
            <span
              style={{
                color: '#888',
                fontSize: isMobile ? 10 : 12,
                textAlign: 'center',
              }}
            >
              {card.typeLine}
            </span>
          </div>
        </div>

        {/* Power/Toughness badge */}
        {card.power !== null && card.toughness !== null && (
          <div
            style={{
              position: 'absolute',
              bottom: -8,
              right: -8,
              backgroundColor: hasDamage ? '#dc2626' : '#1a1a1a',
              border: `2px solid ${hasDamage ? '#f87171' : '#fbbf24'}`,
              borderRadius: 6,
              padding: isMobile ? '2px 6px' : '4px 8px',
              display: 'flex',
              alignItems: 'center',
              gap: 2,
              boxShadow: '0 2px 8px rgba(0, 0, 0, 0.5)',
            }}
          >
            <span
              style={{
                color: 'white',
                fontSize: isMobile ? 14 : 18,
                fontWeight: 700,
              }}
            >
              {card.power}
            </span>
            <span
              style={{
                color: '#888',
                fontSize: isMobile ? 12 : 14,
              }}
            >
              /
            </span>
            <span
              style={{
                color: hasDamage ? '#f87171' : 'white',
                fontSize: isMobile ? 14 : 18,
                fontWeight: 700,
              }}
            >
              {effectiveToughness}
            </span>
            {hasDamage && (
              <span
                style={{
                  color: '#888',
                  fontSize: isMobile ? 10 : 12,
                  marginLeft: 2,
                }}
              >
                ({card.toughness})
              </span>
            )}
          </div>
        )}
      </div>

      {/* Card name */}
      <div
        style={{
          color: 'white',
          fontSize: isMobile ? 12 : 14,
          fontWeight: 500,
          textAlign: 'center',
          maxWidth: cardWidth + 40,
        }}
      >
        {card.name}
      </div>
    </div>
  )
}
