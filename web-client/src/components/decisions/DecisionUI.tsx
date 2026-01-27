import { useState } from 'react'
import { useGameStore } from '../../store/gameStore'
import type { EntityId, SelectCardsDecision, ChooseTargetsDecision, YesNoDecision, ChooseNumberDecision } from '../../types'
import { useResponsive, calculateFittingCardWidth, type ResponsiveSizes } from '../../hooks/useResponsive'
import { LibrarySearchUI } from './LibrarySearchUI'
import { ReorderCardsUI } from './ReorderCardsUI'
import { OrderBlockersUI } from './OrderBlockersUI'
import { getCardImageUrl } from '../../utils/cardImages'

/**
 * Check if all legal targets in a ChooseTargetsDecision are players.
 */
function isPlayerOnlyTargeting(decision: ChooseTargetsDecision, playerIds: EntityId[]): boolean {
  const legalTargets = decision.legalTargets[0] ?? []
  if (legalTargets.length === 0) return false
  return legalTargets.every((targetId) => playerIds.includes(targetId))
}

/**
 * Decision UI overlay for pending decisions (e.g., discard to hand size, library search).
 */
export function DecisionUI() {
  const pendingDecision = useGameStore((state) => state.pendingDecision)
  const gameState = useGameStore((state) => state.gameState)
  const responsive = useResponsive()

  if (!pendingDecision) return null

  // Handle SearchLibraryDecision with dedicated UI
  if (pendingDecision.type === 'SearchLibraryDecision') {
    return <LibrarySearchUI decision={pendingDecision} responsive={responsive} />
  }

  // Handle ReorderLibraryDecision with dedicated UI
  if (pendingDecision.type === 'ReorderLibraryDecision') {
    return <ReorderCardsUI decision={pendingDecision} responsive={responsive} />
  }

  // Handle OrderObjectsDecision (e.g., damage assignment order for blockers)
  if (pendingDecision.type === 'OrderObjectsDecision') {
    // Combat phase ordering uses dedicated blocker ordering UI
    if (pendingDecision.context.phase === 'COMBAT') {
      return <OrderBlockersUI decision={pendingDecision} responsive={responsive} />
    }
    // Other ordering decisions could use a generic ordering UI (not yet implemented)
    return null
  }

  // Handle YesNoDecision (e.g., "You may shuffle your library")
  if (pendingDecision.type === 'YesNoDecision') {
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
        <YesNoDecisionUI decision={pendingDecision} responsive={responsive} />
      </div>
    )
  }

  // Handle ChooseNumberDecision (e.g., "Choose how many cards to draw")
  if (pendingDecision.type === 'ChooseNumberDecision') {
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
        <ChooseNumberDecisionUI decision={pendingDecision} responsive={responsive} />
      </div>
    )
  }

  // Handle SelectCardsDecision
  if (pendingDecision.type === 'SelectCardsDecision') {
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
        <CardSelectionDecision decision={pendingDecision} responsive={responsive} />
      </div>
    )
  }

  // Handle ChooseTargetsDecision - always show a prompt banner and let the user click targets on the board
  if (pendingDecision.type === 'ChooseTargetsDecision') {
    const playerIds = gameState?.players.map((p) => p.playerId) ?? []
    const isPlayerOnly = isPlayerOnlyTargeting(pendingDecision, playerIds)

    return (
      <div
        style={{
          position: 'fixed',
          top: responsive.isMobile ? 60 : 80,
          left: '50%',
          transform: 'translateX(-50%)',
          backgroundColor: 'rgba(0, 0, 0, 0.9)',
          padding: responsive.isMobile ? '12px 20px' : '16px 32px',
          borderRadius: 12,
          border: '2px solid #ff4444',
          boxShadow: '0 4px 20px rgba(255, 68, 68, 0.3)',
          zIndex: 1000,
          textAlign: 'center',
          pointerEvents: 'none',
        }}
      >
        <div style={{ color: 'white', fontSize: responsive.fontSize.large, fontWeight: 600 }}>
          {pendingDecision.prompt}
        </div>
        <div style={{ color: '#aaa', fontSize: responsive.fontSize.normal, marginTop: 4 }}>
          {isPlayerOnly
            ? "Click a player's life total to target them"
            : 'Click a valid target on the board'}
        </div>
      </div>
    )
  }

  // Other decision types not yet implemented
  return null
}

/**
 * Yes/No decision - make a binary choice.
 */
function YesNoDecisionUI({
  decision,
  responsive,
}: {
  decision: YesNoDecision
  responsive: ResponsiveSizes
}) {
  const submitYesNoDecision = useGameStore((s) => s.submitYesNoDecision)

  const handleYes = () => {
    submitYesNoDecision(true)
  }

  const handleNo = () => {
    submitYesNoDecision(false)
  }

  return (
    <>
      <h2
        style={{
          color: 'white',
          margin: 0,
          fontSize: responsive.isMobile ? 18 : 24,
          textAlign: 'center',
        }}
      >
        {decision.prompt}
      </h2>

      {decision.context.sourceName && (
        <p style={{ color: '#aaa', margin: 0, fontSize: responsive.fontSize.normal }}>
          {decision.context.sourceName}
        </p>
      )}

      {/* Yes/No buttons */}
      <div
        style={{
          display: 'flex',
          gap: responsive.isMobile ? 16 : 24,
          marginTop: responsive.isMobile ? 16 : 24,
        }}
      >
        <button
          onClick={handleYes}
          style={{
            padding: responsive.isMobile ? '12px 32px' : '16px 48px',
            fontSize: responsive.fontSize.large,
            backgroundColor: '#00aa00',
            color: 'white',
            border: 'none',
            borderRadius: 8,
            cursor: 'pointer',
            fontWeight: 600,
            minWidth: responsive.isMobile ? 100 : 120,
          }}
        >
          {decision.yesText}
        </button>
        <button
          onClick={handleNo}
          style={{
            padding: responsive.isMobile ? '12px 32px' : '16px 48px',
            fontSize: responsive.fontSize.large,
            backgroundColor: '#666',
            color: 'white',
            border: 'none',
            borderRadius: 8,
            cursor: 'pointer',
            fontWeight: 600,
            minWidth: responsive.isMobile ? 100 : 120,
          }}
        >
          {decision.noText}
        </button>
      </div>
    </>
  )
}

/**
 * Choose number decision - select a number from a range.
 */
function ChooseNumberDecisionUI({
  decision,
  responsive,
}: {
  decision: ChooseNumberDecision
  responsive: ResponsiveSizes
}) {
  const [selectedNumber, setSelectedNumber] = useState(decision.minValue)
  const submitNumberDecision = useGameStore((s) => s.submitNumberDecision)

  const handleConfirm = () => {
    submitNumberDecision(selectedNumber)
  }

  // Generate number options
  const numbers = []
  for (let i = decision.minValue; i <= decision.maxValue; i++) {
    numbers.push(i)
  }

  return (
    <>
      <h2
        style={{
          color: 'white',
          margin: 0,
          fontSize: responsive.isMobile ? 18 : 24,
          textAlign: 'center',
        }}
      >
        {decision.prompt}
      </h2>

      {decision.context.sourceName && (
        <p style={{ color: '#aaa', margin: 0, fontSize: responsive.fontSize.normal }}>
          {decision.context.sourceName}
        </p>
      )}

      {/* Number selection buttons */}
      <div
        style={{
          display: 'flex',
          gap: responsive.isMobile ? 8 : 12,
          marginTop: responsive.isMobile ? 16 : 24,
          flexWrap: 'wrap',
          justifyContent: 'center',
        }}
      >
        {numbers.map((num) => (
          <button
            key={num}
            onClick={() => setSelectedNumber(num)}
            style={{
              padding: responsive.isMobile ? '12px 20px' : '16px 28px',
              fontSize: responsive.fontSize.large,
              backgroundColor: selectedNumber === num ? '#0066cc' : '#444',
              color: 'white',
              border: selectedNumber === num ? '3px solid #66aaff' : '2px solid #666',
              borderRadius: 8,
              cursor: 'pointer',
              fontWeight: 600,
              minWidth: responsive.isMobile ? 50 : 60,
              transition: 'all 0.15s',
            }}
          >
            {num}
          </button>
        ))}
      </div>

      {/* Confirm button */}
      <button
        onClick={handleConfirm}
        style={{
          padding: responsive.isMobile ? '12px 32px' : '16px 48px',
          fontSize: responsive.fontSize.large,
          backgroundColor: '#00aa00',
          color: 'white',
          border: 'none',
          borderRadius: 8,
          cursor: 'pointer',
          fontWeight: 600,
          marginTop: responsive.isMobile ? 16 : 24,
        }}
      >
        Confirm
      </button>
    </>
  )
}

/**
 * Card selection decision - select cards from a list.
 */
function CardSelectionDecision({
  decision,
  responsive,
}: {
  decision: SelectCardsDecision
  responsive: ResponsiveSizes
}) {
  const [selectedCards, setSelectedCards] = useState<EntityId[]>([])
  const [hoveredCardId, setHoveredCardId] = useState<EntityId | null>(null)
  const submitDecision = useGameStore((s) => s.submitDecision)
  const gameState = useGameStore((s) => s.gameState)

  // Get hovered card info from either decision cardInfo (hidden cards) or gameState (visible cards)
  const hoveredCardInfo = hoveredCardId
    ? {
        name: decision.cardInfo?.[hoveredCardId]?.name || gameState?.cards[hoveredCardId]?.name,
        imageUri: decision.cardInfo?.[hoveredCardId]?.imageUri || gameState?.cards[hoveredCardId]?.imageUri
      }
    : null

  const canConfirm =
    selectedCards.length >= decision.minSelections &&
    selectedCards.length <= decision.maxSelections

  // Calculate card size that fits all cards
  const availableWidth = responsive.viewportWidth - responsive.containerPadding * 2 - 32
  const gap = responsive.isMobile ? 4 : 8
  const maxCardWidth = responsive.isMobile ? 90 : 130
  const cardWidth = calculateFittingCardWidth(
    decision.options.length,
    availableWidth,
    gap,
    maxCardWidth,
    45
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

  return (
    <>
      <h2
        style={{
          color: 'white',
          margin: 0,
          fontSize: responsive.isMobile ? 18 : 24,
          textAlign: 'center',
        }}
      >
        {decision.prompt}
      </h2>

      <p style={{ color: '#888', margin: 0, fontSize: responsive.fontSize.normal }}>
        Selected: {selectedCards.length} / {decision.minSelections}
        {decision.minSelections !== decision.maxSelections &&
          ` - ${decision.maxSelections}`}
      </p>

      {/* Card options */}
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
        {decision.options.map((cardId) => {
          // For hidden cards (e.g., opponent's library), use cardInfo from decision
          // For visible cards (e.g., own hand for discard), use gameState.cards
          const cardInfoFromDecision = decision.cardInfo?.[cardId]
          const cardFromState = gameState?.cards[cardId]
          const cardName = cardInfoFromDecision?.name || cardFromState?.name || 'Unknown Card'
          const imageUri = cardInfoFromDecision?.imageUri || cardFromState?.imageUri
          return (
            <DecisionCard
              key={cardId}
              cardId={cardId}
              cardName={cardName}
              imageUri={imageUri}
              isSelected={selectedCards.includes(cardId)}
              onClick={() => toggleCard(cardId)}
              cardWidth={cardWidth}
              isMobile={responsive.isMobile}
              onMouseEnter={() => setHoveredCardId(cardId)}
              onMouseLeave={() => setHoveredCardId(null)}
            />
          )
        })}
      </div>

      {/* Confirm button */}
      <button
        onClick={handleConfirm}
        disabled={!canConfirm}
        style={{
          padding: responsive.isMobile ? '10px 20px' : '12px 32px',
          fontSize: responsive.fontSize.large,
          backgroundColor: canConfirm ? '#00aa00' : '#444',
          color: canConfirm ? 'white' : '#888',
          border: 'none',
          borderRadius: 8,
          cursor: canConfirm ? 'pointer' : 'not-allowed',
        }}
      >
        Confirm
      </button>

      {/* Card preview on hover */}
      {hoveredCardInfo?.name && !responsive.isMobile && (
        <DecisionCardPreview cardName={hoveredCardInfo.name} imageUri={hoveredCardInfo.imageUri} />
      )}
    </>
  )
}

/**
 * Card preview overlay - shows enlarged card when hovering.
 */
function DecisionCardPreview({ cardName, imageUri }: { cardName: string; imageUri?: string | null | undefined }) {
  const cardImageUrl = getCardImageUrl(cardName, imageUri, 'large')

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
          alt={cardName}
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

/**
 * Card display for decision UI.
 */
function DecisionCard({
  cardId: _cardId,
  cardName,
  imageUri,
  isSelected,
  onClick,
  cardWidth = 130,
  isMobile = false,
  onMouseEnter,
  onMouseLeave,
}: {
  cardId: EntityId
  cardName: string
  imageUri?: string | null | undefined
  isSelected: boolean
  onClick: () => void
  cardWidth?: number
  isMobile?: boolean
  onMouseEnter?: () => void
  onMouseLeave?: () => void
}) {
  const cardImageUrl = getCardImageUrl(cardName, imageUri)

  const cardRatio = 1.4
  const cardHeight = Math.round(cardWidth * cardRatio)

  return (
    <div
      onClick={onClick}
      onMouseEnter={onMouseEnter}
      onMouseLeave={onMouseLeave}
      style={{
        width: cardWidth,
        height: cardHeight,
        backgroundColor: isSelected ? '#330000' : '#1a1a1a',
        border: isSelected ? '3px solid #ff4444' : '2px solid #444',
        borderRadius: isMobile ? 6 : 10,
        display: 'flex',
        flexDirection: 'column',
        overflow: 'hidden',
        cursor: 'pointer',
        transition: 'all 0.15s',
        transform: isSelected ? 'translateY(-8px) scale(1.05)' : 'none',
        boxShadow: isSelected
          ? '0 8px 20px rgba(255, 68, 68, 0.3)'
          : '0 4px 8px rgba(0, 0, 0, 0.5)',
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
          padding: isMobile ? '4px' : '8px',
        }}
      >
        <span
          style={{
            color: 'white',
            fontSize: isMobile ? 9 : 11,
            fontWeight: 500,
            textAlign: 'center',
          }}
        >
          {cardName}
        </span>
      </div>

      {/* Selection indicator */}
      {isSelected && (
        <div
          style={{
            position: 'absolute',
            top: 4,
            right: 4,
            width: 20,
            height: 20,
            backgroundColor: '#ff4444',
            borderRadius: '50%',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: 'white',
            fontWeight: 'bold',
            fontSize: 12,
          }}
        >
          ✓
        </div>
      )}
    </div>
  )
}
