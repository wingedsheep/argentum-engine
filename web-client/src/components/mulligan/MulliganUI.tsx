import { useState } from 'react'
import { useGameStore, type MulliganState, type MulliganCardInfo } from '../../store/gameStore'
import type { EntityId } from '../../types'
import { useResponsive, calculateFittingCardWidth, type ResponsiveSizes } from '../../hooks/useResponsive'
import { getCardImageUrl } from '../../utils/cardImages'
import styles from './MulliganUI.module.css'

/**
 * Mulligan UI overlay.
 */
export function MulliganUI() {
  const mulliganState = useGameStore((state) => state.mulliganState)
  const responsive = useResponsive()
  const [hoveredCardId, setHoveredCardId] = useState<EntityId | null>(null)
  const [minimized, setMinimized] = useState(false)

  if (!mulliganState) return null

  const hoveredCardInfo = hoveredCardId ? mulliganState.cards[hoveredCardId] : null
  const isChoosingBottom = mulliganState.phase === 'choosingBottomCards'

  // When minimized (only during choosingBottom phase), show floating button to restore
  if (minimized && isChoosingBottom) {
    return (
      <button
        onClick={() => setMinimized(false)}
        className={styles.minimizedButton}
      >
        ↑ Return to Card Selection
      </button>
    )
  }

  return (
    <div className={styles.overlay}>
      {mulliganState.phase === 'deciding' ? (
        <MulliganDecision state={mulliganState} responsive={responsive} onHoverCard={setHoveredCardId} />
      ) : (
        <ChooseBottomCards state={mulliganState} responsive={responsive} onHoverCard={setHoveredCardId} onMinimize={() => setMinimized(true)} />
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
      <h2 className={styles.title}>
        {state.mulliganCount === 0
          ? 'Opening Hand'
          : `Mulligan ${state.mulliganCount}`}
      </h2>

      <p className={styles.subtitle}>
        {state.mulliganCount > 0 &&
          `If you keep, you'll put ${state.cardsToPutOnBottom} card(s) on the bottom.`}
      </p>

      {/* Hand preview */}
      <div className={styles.cardContainer} style={{ gap }}>
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
              onMouseEnter={() => onHoverCard(cardId)}
              onMouseLeave={() => onHoverCard(null)}
            />
          )
        })}
      </div>

      {/* Action buttons */}
      <div className={styles.buttonContainer}>
        <button onClick={keepHand} className={styles.keepButton}>
          Keep Hand
        </button>

        <button onClick={mulligan} className={styles.mulliganButton}>
          Mulligan
        </button>
      </div>
    </>
  )
}

/**
 * Choose cards to put on bottom after keeping.
 */
function ChooseBottomCards({ state, responsive, onHoverCard, onMinimize }: { state: MulliganState; responsive: ResponsiveSizes; onHoverCard: (cardId: EntityId | null) => void; onMinimize: () => void }) {
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
      <h2 className={styles.title}>
        Choose {state.cardsToPutOnBottom} Card
        {state.cardsToPutOnBottom > 1 ? 's' : ''} for Bottom
      </h2>

      <p className={styles.subtitle}>
        Selected: {state.selectedCards.length} / {state.cardsToPutOnBottom}
      </p>

      {/* Hand with selectable cards */}
      <div className={styles.cardContainer} style={{ gap }}>
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
              onMouseEnter={() => onHoverCard(cardId)}
              onMouseLeave={() => onHoverCard(null)}
            />
          )
        })}
      </div>

      {/* Action buttons */}
      <div className={styles.buttonContainer}>
        <button onClick={onMinimize} className={styles.viewBattlefieldButton}>
          View Battlefield
        </button>
        <button
          onClick={() => chooseBottomCards(state.selectedCards)}
          disabled={!canConfirm}
          className={styles.confirmButton}
        >
          Confirm
        </button>
      </div>
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
  onMouseEnter?: () => void
  onMouseLeave?: () => void
}) {
  // Use provided imageUri or fall back to Scryfall API
  const cardImageUrl = getCardImageUrl(cardName, imageUri)

  const cardRatio = 1.4
  const cardHeight = Math.round(cardWidth * cardRatio)

  const cardClasses = [
    styles.card,
    isSelected ? styles.cardSelected : styles.cardDefault,
    selectable && styles.cardSelectable,
  ]
    .filter(Boolean)
    .join(' ')

  return (
    <div
      onClick={selectable ? onClick : undefined}
      onMouseEnter={onMouseEnter}
      onMouseLeave={onMouseLeave}
      className={cardClasses}
      style={{
        width: cardWidth,
        height: cardHeight,
      }}
    >
      {/* Card image */}
      <img
        src={cardImageUrl}
        alt={cardName}
        className={styles.cardImage}
        onError={(e) => {
          // Hide image on error and show placeholder
          e.currentTarget.style.display = 'none'
        }}
      />

      {/* Fallback text (shown if image fails) */}
      <div className={styles.cardFallback}>
        <span className={styles.cardFallbackName}>
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
    <div className={styles.previewContainer}>
      <div
        className={styles.previewCard}
        style={{
          width: previewWidth,
          height: previewHeight,
        }}
      >
        <img
          src={cardImageUrl}
          alt={cardInfo.name}
          className={styles.previewImage}
        />
      </div>
    </div>
  )
}
