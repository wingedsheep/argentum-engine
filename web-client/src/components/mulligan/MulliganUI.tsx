import { useState, useCallback } from 'react'
import { useGameStore, type MulliganState } from '@/store/gameStore.ts'
import type { EntityId } from '@/types'
import { useResponsive, calculateFittingCardWidth, type ResponsiveSizes } from '@/hooks/useResponsive.ts'
import { getCardImageUrl } from '@/utils/cardImages.ts'
import { HoverCardPreview } from '../ui/HoverCardPreview'
import { StandaloneConcedeButton } from '../game/overlay'
import styles from './MulliganUI.module.css'

/**
 * Mulligan UI overlay.
 */
export function MulliganUI() {
  const mulliganState = useGameStore((state) => state.mulliganState)
  const responsive = useResponsive()
  const [hoveredCardId, setHoveredCardId] = useState<EntityId | null>(null)
  const [hoverPos, setHoverPos] = useState<{ x: number; y: number } | null>(null)
  const [minimized, setMinimized] = useState(false)

  const handleHover = useCallback((cardId: EntityId | null, e?: React.MouseEvent) => {
    setHoveredCardId(cardId)
    if (cardId && e) {
      setHoverPos({ x: e.clientX, y: e.clientY })
    } else {
      setHoverPos(null)
    }
  }, [])

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
      <StandaloneConcedeButton />
      {mulliganState.phase === 'deciding' ? (
        <MulliganDecision state={mulliganState} responsive={responsive} onHoverCard={handleHover} />
      ) : (
        <ChooseBottomCards state={mulliganState} responsive={responsive} onHoverCard={handleHover} onMinimize={() => setMinimized(true)} />
      )}

      {/* Card preview on hover */}
      {hoveredCardInfo && !responsive.isMobile && (
        <HoverCardPreview name={hoveredCardInfo.name} imageUri={hoveredCardInfo.imageUri} pos={hoverPos} />
      )}
    </div>
  )
}

/**
 * Figure out how big each card should be so all cards fit horizontally
 * without overflowing vertically. Scales up aggressively on big screens
 * so cards stay readable, while preventing overflow on short viewports.
 */
function computeMulliganCardWidth(
  cardCount: number,
  responsive: ResponsiveSizes,
  gap: number,
): number {
  const { viewportWidth, viewportHeight, containerPadding, isMobile, isTablet } = responsive

  // Horizontal budget
  const availableWidth = viewportWidth - containerPadding * 2 - 32

  // Vertical budget: reserve space for title, subtitle, buttons, padding.
  // Cards can take roughly 55% of the viewport height on mobile, 60% otherwise.
  const cardVerticalBudget = viewportHeight * (isMobile ? 0.55 : 0.6)
  const cardRatio = 1.4
  const widthFromHeight = Math.floor(cardVerticalBudget / cardRatio)

  // Target max card width scales by breakpoint
  const breakpointMax = isMobile ? 110 : isTablet ? 170 : 240
  const minCardWidth = isMobile ? 52 : isTablet ? 72 : 96

  const maxCardWidth = Math.min(breakpointMax, widthFromHeight)
  return calculateFittingCardWidth(cardCount, availableWidth, gap, maxCardWidth, minCardWidth)
}

/**
 * Mulligan decision phase - keep or mulligan.
 */
function MulliganDecision({ state, responsive, onHoverCard }: { state: MulliganState; responsive: ResponsiveSizes; onHoverCard: (cardId: EntityId | null, e?: React.MouseEvent) => void }) {
  const keepHand = useGameStore((s) => s.keepHand)
  const mulligan = useGameStore((s) => s.mulligan)
  const tournamentState = useGameStore((s) => s.tournamentState)
  const playerId = useGameStore((s) => s.playerId)

  // Calculate card size that fits all 7 cards
  const gap = responsive.isMobile ? 4 : responsive.isTablet ? 6 : 10
  const cardWidth = computeMulliganCardWidth(state.hand.length, responsive, gap)

  // Tournament info
  const tournamentInfo = (() => {
    if (!tournamentState || !playerId) return null
    const playerStanding = tournamentState.standings.find((s) => s.playerId === playerId)
    const opponentName = tournamentState.currentMatchOpponentName
    const opponentStanding = opponentName
      ? tournamentState.standings.find((s) => s.playerName === opponentName)
      : null
    if (!playerStanding || !opponentName) return null
    const playerRecord = `${playerStanding.wins}-${playerStanding.losses}${playerStanding.draws > 0 ? `-${playerStanding.draws}` : ''}`
    const opponentRecord = opponentStanding
      ? `${opponentStanding.wins}-${opponentStanding.losses}${opponentStanding.draws > 0 ? `-${opponentStanding.draws}` : ''}`
      : null
    return { round: tournamentState.currentRound, playerRecord, opponentName, opponentRecord }
  })()

  return (
    <>
      {tournamentInfo && (
        <div className={styles.tournamentBanner}>
          <p className={styles.tournamentRound}>Round {tournamentInfo.round}</p>
          <p className={styles.tournamentMatchup}>
            You ({tournamentInfo.playerRecord}) vs {tournamentInfo.opponentName}
            {tournamentInfo.opponentRecord ? ` (${tournamentInfo.opponentRecord})` : ''}
          </p>
        </div>
      )}

      <h2 className={styles.title}>
        {state.mulliganCount === 0
          ? 'Opening Hand'
          : `Mulligan ${state.mulliganCount}`}
      </h2>

      <div
        className={`${styles.playDrawBadge} ${state.isOnThePlay ? styles.playDrawBadgePlay : styles.playDrawBadgeDraw}`}
      >
        <span className={styles.playDrawIcon} aria-hidden>
          {state.isOnThePlay ? '⚔' : '✦'}
        </span>
        <span className={styles.playDrawLabel}>
          {state.isOnThePlay ? 'On the Play' : 'On the Draw'}
        </span>
      </div>

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
              onMouseEnter={(e: React.MouseEvent) => onHoverCard(cardId, e)}
              onMouseMove={(e: React.MouseEvent) => onHoverCard(cardId, e)}
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
function ChooseBottomCards({ state, responsive, onHoverCard, onMinimize }: { state: MulliganState; responsive: ResponsiveSizes; onHoverCard: (cardId: EntityId | null, e?: React.MouseEvent) => void; onMinimize: () => void }) {
  const chooseBottomCards = useGameStore((s) => s.chooseBottomCards)
  const toggleMulliganCard = useGameStore((s) => s.toggleMulliganCard)

  const canConfirm = state.selectedCards.length === state.cardsToPutOnBottom

  // Calculate card size that fits all cards
  const gap = responsive.isMobile ? 4 : responsive.isTablet ? 6 : 10
  const cardWidth = computeMulliganCardWidth(state.hand.length, responsive, gap)

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
              onMouseEnter={(e: React.MouseEvent) => onHoverCard(cardId, e)}
              onMouseMove={(e: React.MouseEvent) => onHoverCard(cardId, e)}
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
  onMouseMove,
  onMouseLeave,
}: {
  cardId: EntityId
  cardName: string
  imageUri?: string | null | undefined
  selectable: boolean
  isSelected?: boolean
  onClick?: () => void
  cardWidth?: number
  onMouseEnter?: (e: React.MouseEvent) => void
  onMouseMove?: (e: React.MouseEvent) => void
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
      onMouseMove={onMouseMove}
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

