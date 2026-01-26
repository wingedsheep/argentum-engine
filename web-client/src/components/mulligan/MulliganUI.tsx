import { useState } from 'react'
import { useGameStore, type MulliganState, type MulliganCardInfo } from '../../store/gameStore'
import type { EntityId } from '../../types'
import { useResponsive, calculateFittingCardWidth, type ResponsiveSizes } from '../../hooks/useResponsive'
import { getCardImageUrl } from '../../utils/cardImages'

/**
 * Mulligan UI overlay.
 */
export function MulliganUI() {
  const mulliganState = useGameStore((state) => state.mulliganState)
  const responsive = useResponsive()
  const [hoveredCardId, setHoveredCardId] = useState<EntityId | null>(null)

  if (!mulliganState) return null

  const hoveredCardInfo = hoveredCardId ? mulliganState.cards[hoveredCardId] : null

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
      {mulliganState.phase === 'deciding' ? (
        <MulliganDecision state={mulliganState} responsive={responsive} onHoverCard={setHoveredCardId} />
      ) : (
        <ChooseBottomCards state={mulliganState} responsive={responsive} onHoverCard={setHoveredCardId} />
      )}

      {/* Card preview on hover */}
      {hoveredCardInfo && !responsive.isMobile && (
        <MulliganCardPreview cardInfo={hoveredCardInfo} />
      )}
    </div>
  )
}

/**
 * Mulligan decision phase - keep or mulligan.
 */
function MulliganDecision({ state, responsive, onHoverCard }: { state: MulliganState; responsive: ResponsiveSizes; onHoverCard: (cardId: EntityId | null) => void }) {
  const keepHand = useGameStore((s) => s.keepHand)
  const mulligan = useGameStore((s) => s.mulligan)

  // Calculate card size that fits all 7 cards
  const availableWidth = responsive.viewportWidth - (responsive.containerPadding * 2) - 32 // extra padding
  const gap = responsive.isMobile ? 4 : 8
  const maxCardWidth = responsive.isMobile ? 90 : 130
  const cardWidth = calculateFittingCardWidth(state.hand.length, availableWidth, gap, maxCardWidth, 45)

  return (
    <>
      <h2 style={{ color: 'white', margin: 0, fontSize: responsive.isMobile ? 18 : 24 }}>
        {state.mulliganCount === 0
          ? 'Opening Hand'
          : `Mulligan ${state.mulliganCount}`}
      </h2>

      <p style={{ color: '#888', margin: 0, fontSize: responsive.fontSize.normal, textAlign: 'center' }}>
        {state.mulliganCount > 0 &&
          `If you keep, you'll put ${state.cardsToPutOnBottom} card(s) on the bottom.`}
      </p>

      {/* Hand preview */}
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
        {state.hand.map((cardId) => {
          const cardInfo = state.cards[cardId]
          return (
            <MulliganCard
              key={cardId}
              cardId={cardId}
              cardName={cardInfo?.name || 'Unknown'}
              imageUri={cardInfo?.imageUri}
              selectable={false}
              cardWidth={cardWidth}
              isMobile={responsive.isMobile}
              onMouseEnter={() => onHoverCard(cardId)}
              onMouseLeave={() => onHoverCard(null)}
            />
          )
        })}
      </div>

      {/* Action buttons */}
      <div style={{ display: 'flex', gap: responsive.isMobile ? 8 : 16, flexWrap: 'wrap', justifyContent: 'center' }}>
        <button
          onClick={keepHand}
          style={{
            padding: responsive.isMobile ? '10px 20px' : '12px 32px',
            fontSize: responsive.fontSize.large,
            backgroundColor: '#00aa00',
            color: 'white',
            border: 'none',
            borderRadius: 8,
            cursor: 'pointer',
          }}
        >
          Keep Hand
        </button>

        <button
          onClick={mulligan}
          style={{
            padding: responsive.isMobile ? '10px 20px' : '12px 32px',
            fontSize: responsive.fontSize.large,
            backgroundColor: '#cc6600',
            color: 'white',
            border: 'none',
            borderRadius: 8,
            cursor: 'pointer',
          }}
        >
          Mulligan
        </button>
      </div>
    </>
  )
}

/**
 * Choose cards to put on bottom after keeping.
 */
function ChooseBottomCards({ state, responsive, onHoverCard }: { state: MulliganState; responsive: ResponsiveSizes; onHoverCard: (cardId: EntityId | null) => void }) {
  const chooseBottomCards = useGameStore((s) => s.chooseBottomCards)
  const toggleMulliganCard = useGameStore((s) => s.toggleMulliganCard)

  const canConfirm = state.selectedCards.length === state.cardsToPutOnBottom

  // Calculate card size that fits all cards
  const availableWidth = responsive.viewportWidth - (responsive.containerPadding * 2) - 32
  const gap = responsive.isMobile ? 4 : 8
  const maxCardWidth = responsive.isMobile ? 90 : 130
  const cardWidth = calculateFittingCardWidth(state.hand.length, availableWidth, gap, maxCardWidth, 45)

  return (
    <>
      <h2 style={{ color: 'white', margin: 0, fontSize: responsive.isMobile ? 18 : 24 }}>
        Choose {state.cardsToPutOnBottom} Card
        {state.cardsToPutOnBottom > 1 ? 's' : ''} for Bottom
      </h2>

      <p style={{ color: '#888', margin: 0, fontSize: responsive.fontSize.normal }}>
        Selected: {state.selectedCards.length} / {state.cardsToPutOnBottom}
      </p>

      {/* Hand with selectable cards */}
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
        {state.hand.map((cardId) => {
          const cardInfo = state.cards[cardId]
          return (
            <MulliganCard
              key={cardId}
              cardId={cardId}
              cardName={cardInfo?.name || 'Unknown'}
              imageUri={cardInfo?.imageUri}
              selectable
              isSelected={state.selectedCards.includes(cardId)}
              onClick={() => toggleMulliganCard(cardId)}
              cardWidth={cardWidth}
              isMobile={responsive.isMobile}
              onMouseEnter={() => onHoverCard(cardId)}
              onMouseLeave={() => onHoverCard(null)}
            />
          )
        })}
      </div>

      {/* Confirm button */}
      <button
        onClick={() => chooseBottomCards(state.selectedCards)}
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
    </>
  )
}

/**
 * Card display for mulligan UI.
 * Uses imageUri from server if available, otherwise falls back to Scryfall API.
 */
function MulliganCard({
  cardId: _cardId,
  cardName,
  imageUri,
  selectable,
  isSelected = false,
  onClick,
  cardWidth = 130,
  isMobile = false,
  onMouseEnter,
  onMouseLeave,
}: {
  cardId: EntityId
  cardName: string
  imageUri?: string | null | undefined
  selectable: boolean
  isSelected?: boolean
  onClick?: () => void
  cardWidth?: number
  isMobile?: boolean
  onMouseEnter?: () => void
  onMouseLeave?: () => void
}) {
  // Use provided imageUri or fall back to Scryfall API
  const cardImageUrl = getCardImageUrl(cardName, imageUri)

  const cardRatio = 1.4
  const cardHeight = Math.round(cardWidth * cardRatio)

  return (
    <div
      onClick={selectable ? onClick : undefined}
      onMouseEnter={onMouseEnter}
      onMouseLeave={onMouseLeave}
      style={{
        width: cardWidth,
        height: cardHeight,
        backgroundColor: isSelected ? '#003300' : '#1a1a1a',
        border: isSelected ? '3px solid #00ff00' : '2px solid #444',
        borderRadius: isMobile ? 6 : 10,
        display: 'flex',
        flexDirection: 'column',
        overflow: 'hidden',
        cursor: selectable ? 'pointer' : 'default',
        transition: 'all 0.15s',
        transform: isSelected ? 'translateY(-8px) scale(1.05)' : 'none',
        boxShadow: isSelected ? '0 8px 20px rgba(0, 255, 0, 0.3)' : '0 4px 8px rgba(0, 0, 0, 0.5)',
        flexShrink: 0,
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
          // Hide image on error and show placeholder
          e.currentTarget.style.display = 'none'
        }}
      />

      {/* Fallback text (shown if image fails) */}
      <div
        style={{
          position: 'absolute',
          bottom: 0,
          left: 0,
          right: 0,
          backgroundColor: 'rgba(0, 0, 0, 0.8)',
          padding: isMobile ? '4px' : '8px',
          display: 'none', // Will be shown via CSS when image fails
        }}
      >
        <span
          style={{
            color: 'white',
            fontSize: isMobile ? 9 : 11,
            fontWeight: 500,
          }}
        >
          {cardName}
        </span>
      </div>
    </div>
  )
}

/**
 * Card preview overlay - shows enlarged card when hovering.
 */
function MulliganCardPreview({ cardInfo }: { cardInfo: MulliganCardInfo }) {
  const cardImageUrl = getCardImageUrl(cardInfo.name, cardInfo.imageUri, 'large')

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
          alt={cardInfo.name}
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
