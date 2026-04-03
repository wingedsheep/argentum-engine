import { useState, useMemo } from 'react'
import { useGameStore } from '@/store/gameStore.ts'
import type { EntityId, ChooseTargetsDecision, ClientCard } from '@/types'
import { calculateFittingCardWidth, type ResponsiveSizes } from '@/hooks/useResponsive.ts'
import { ZoneSelectionUI, type ZoneCardInfo } from './ZoneSelectionUI'
import { getCardImageUrl } from '@/utils/cardImages.ts'
import styles from './DecisionUI.module.css'

/**
 * Graveyard targeting UI - uses the shared ZoneSelectionUI component.
 * Supports selecting from multiple graveyards with tabs.
 */
export function GraveyardTargetingUI({
  decision,
  graveyardCards,
  responsive,
}: {
  decision: ChooseTargetsDecision
  graveyardCards: ClientCard[]
  responsive: ResponsiveSizes
}) {
  const submitTargetsDecision = useGameStore((s) => s.submitTargetsDecision)
  const submitCancelDecision = useGameStore((s) => s.submitCancelDecision)
  const gameState = useGameStore((s) => s.gameState)
  const viewingPlayerId = gameState?.viewingPlayerId

  // Group cards by graveyard owner
  const cardsByOwner = useMemo(() => {
    const grouped = new Map<EntityId, ClientCard[]>()
    for (const card of graveyardCards) {
      const ownerId = card.zone?.ownerId ?? card.ownerId
      if (!grouped.has(ownerId)) {
        grouped.set(ownerId, [])
      }
      grouped.get(ownerId)!.push(card)
    }
    return grouped
  }, [graveyardCards])

  // Get owner IDs sorted (viewer's graveyard first)
  const ownerIds = useMemo(() => {
    const ids = Array.from(cardsByOwner.keys())
    // Sort so viewer's graveyard comes first
    return ids.sort((a, b) => {
      if (a === viewingPlayerId) return -1
      if (b === viewingPlayerId) return 1
      return 0
    })
  }, [cardsByOwner, viewingPlayerId])

  // State for which graveyard is selected (default to first one)
  const [selectedOwnerId, setSelectedOwnerId] = useState<EntityId | null>(() => ownerIds[0] ?? null)

  // Ensure selected owner is valid
  const currentOwnerId = selectedOwnerId && ownerIds.includes(selectedOwnerId) ? selectedOwnerId : ownerIds[0] ?? null

  // Get cards for the currently selected graveyard
  const currentCards = currentOwnerId ? (cardsByOwner.get(currentOwnerId) ?? []) : []

  // Convert to ZoneCardInfo format
  const cards: ZoneCardInfo[] = useMemo(() => {
    return currentCards.map((card) => ({
      id: card.id,
      name: card.name,
      typeLine: card.typeLine,
      manaCost: card.manaCost,
      imageUri: card.imageUri,
    }))
  }, [currentCards])

  // Get player names for tabs
  const getPlayerLabel = (ownerId: EntityId): string => {
    if (ownerId === viewingPlayerId) return 'Your Graveyard'
    const player = gameState?.players.find((p) => p.playerId === ownerId)
    return player ? `${player.name}'s Graveyard` : "Opponent's Graveyard"
  }

  const handleConfirm = (selectedCards: EntityId[]) => {
    // Submit with the selected targets for target index 0
    // If minTargets is 0 (optional ability), submitting with 0 cards declines the ability
    submitTargetsDecision({ 0: selectedCards })
  }

  // Get target requirements
  const targetReq = decision.targetRequirements[0]
  const minTargets = targetReq?.minTargets ?? 1
  const maxTargets = targetReq?.maxTargets ?? 1

  // Lift selection state to persist across tab switches
  const [selectedCards, setSelectedCards] = useState<EntityId[]>([])

  // If only one graveyard, use simple UI
  if (ownerIds.length <= 1) {
    return (
      <ZoneSelectionUI
        title="Choose from Graveyard"
        prompt={decision.prompt}
        cards={cards}
        minSelections={minTargets}
        maxSelections={maxTargets}
        responsive={responsive}
        onConfirm={handleConfirm}
        confirmText="Confirm Target"
        sortByType={true}
        useGlobalHover={true}
        onCancel={decision.canCancel ? () => submitCancelDecision() : undefined}
      />
    )
  }

  // Multiple graveyards - show tabs
  return (
    <div className={styles.overlayDarker}>
      {/* Header */}
      <div className={styles.header}>
        <h2 className={styles.title}>
          Choose from Graveyard
        </h2>
        <p className={styles.headerSubtitle}>
          {decision.prompt}
        </p>
      </div>

      {/* Graveyard tabs */}
      <div className={styles.graveyardTabs}>
        {ownerIds.map((ownerId) => {
          const isActive = ownerId === currentOwnerId
          const ownerCards = cardsByOwner.get(ownerId) ?? []
          const cardCount = ownerCards.length
          // Count how many cards are selected from this graveyard
          const selectedFromThisGraveyard = ownerCards.filter((c) =>
            selectedCards.includes(c.id)
          ).length

          const tabClasses = [
            styles.graveyardTab,
            isActive && styles.graveyardTabActive,
            selectedFromThisGraveyard > 0 && !isActive && styles.graveyardTabWithSelection,
          ].filter(Boolean).join(' ')

          return (
            <button
              key={ownerId}
              onClick={() => setSelectedOwnerId(ownerId)}
              className={tabClasses}
            >
              {getPlayerLabel(ownerId)} ({cardCount})
              {selectedFromThisGraveyard > 0 && (
                <span className={styles.graveyardTabBadge}>
                  {selectedFromThisGraveyard}
                </span>
              )}
            </button>
          )
        })}
      </div>

      {/* Card selection - reuse ZoneSelectionUI but embed it */}
      <GraveyardCardSelection
        cards={cards}
        selectedCards={selectedCards}
        onSelectedCardsChange={setSelectedCards}
        minSelections={minTargets}
        maxSelections={maxTargets}
        responsive={responsive}
        onConfirm={handleConfirm}
      />
    </div>
  )
}

/**
 * Card selection portion for graveyard targeting (without the full overlay).
 * Now accepts selectedCards as a prop to persist across tab switches.
 */
function GraveyardCardSelection({
  cards,
  selectedCards,
  onSelectedCardsChange,
  minSelections,
  maxSelections,
  responsive,
  onConfirm,
}: {
  cards: ZoneCardInfo[]
  selectedCards: EntityId[]
  onSelectedCardsChange: (cards: EntityId[]) => void
  minSelections: number
  maxSelections: number
  responsive: ResponsiveSizes
  onConfirm: (selectedCards: EntityId[]) => void
}) {
  const [hoveredCardId, setHoveredCardId] = useState<EntityId | null>(null)
  const hoverCard = useGameStore((s) => s.hoverCard)

  // Sort cards by type then name
  const sortedCards = useMemo(() => {
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
  }, [cards])

  const canConfirm = selectedCards.length >= minSelections && selectedCards.length <= maxSelections

  const availableWidth = responsive.viewportWidth - responsive.containerPadding * 2 - 64
  const gap = responsive.isMobile ? 8 : 12
  const maxCardWidth = responsive.isMobile ? 100 : 140
  const cardWidth = calculateFittingCardWidth(
    Math.min(sortedCards.length, 8),
    availableWidth,
    gap,
    maxCardWidth,
    60
  )

  const toggleCard = (cardId: EntityId) => {
    if (selectedCards.includes(cardId)) {
      onSelectedCardsChange(selectedCards.filter((id) => id !== cardId))
    } else if (selectedCards.length < maxSelections) {
      onSelectedCardsChange([...selectedCards, cardId])
    }
  }

  // Handle hover using global store (for the CardPreview component)
  const handleMouseEnter = (cardId: EntityId, e: React.MouseEvent) => {
    setHoveredCardId(cardId)
    hoverCard(cardId, { x: e.clientX, y: e.clientY })
  }

  const handleMouseLeave = () => {
    setHoveredCardId(null)
    hoverCard(null)
  }

  const handleConfirmClick = () => {
    onConfirm(selectedCards)
    onSelectedCardsChange([])
  }

  const countClass = canConfirm
    ? styles.selectionCountValid
    : selectedCards.length > 0
      ? styles.selectionCountPartial
      : ''

  return (
    <>
      {/* Selection counter */}
      <div className={styles.selectionCounter}>
        <span>
          Selected:{' '}
          <span className={`${styles.selectionCount} ${countClass}`}>
            {selectedCards.length}
          </span>
          {' / '}
          {maxSelections}
        </span>
      </div>

      {/* Card ribbon */}
      <div
        className={styles.cardRibbon}
        style={{
          gap,
          justifyContent: sortedCards.length <= 6 ? 'center' : 'flex-start',
        }}
      >
        {sortedCards.map((card) => (
          <GraveyardCard
            key={card.id}
            card={card}
            isSelected={selectedCards.includes(card.id)}
            isHovered={hoveredCardId === card.id}
            onClick={() => toggleCard(card.id)}
            cardWidth={cardWidth}
            onMouseEnter={(e: React.MouseEvent) => handleMouseEnter(card.id, e)}
            onMouseLeave={handleMouseLeave}
          />
        ))}
      </div>

      {/* No cards message */}
      {sortedCards.length === 0 && (
        <p className={styles.noCardsMessage}>
          No valid targets in this graveyard.
        </p>
      )}

      {/* Confirm button */}
      <button
        onClick={handleConfirmClick}
        disabled={!canConfirm}
        className={styles.confirmButton}
      >
        {selectedCards.length === 0 && minSelections === 0 ? 'Decline' : 'Confirm Target'}
      </button>
      {/* Card preview is handled by the global CardPreview component in GameBoard */}
    </>
  )
}

/**
 * Individual card in graveyard selection.
 */
function GraveyardCard({
  card,
  isSelected,
  isHovered,
  onClick,
  cardWidth,
  onMouseEnter,
  onMouseLeave,
}: {
  card: ZoneCardInfo
  isSelected: boolean
  isHovered: boolean
  onClick: () => void
  cardWidth: number
  onMouseEnter?: (e: React.MouseEvent) => void
  onMouseLeave?: () => void
}) {
  const cardImageUrl = getCardImageUrl(card.name, card.imageUri)
  const cardHeight = Math.round(cardWidth * 1.4)
  const showHoverEffect = isHovered && !isSelected

  const cardClasses = [
    styles.graveyardCard,
    isSelected
      ? styles.graveyardCardSelected
      : showHoverEffect
        ? styles.graveyardCardHovered
        : styles.graveyardCardDefault,
  ].filter(Boolean).join(' ')

  return (
    <div
      onClick={onClick}
      onMouseEnter={onMouseEnter}
      onMouseLeave={onMouseLeave}
      className={cardClasses}
      style={{
        width: cardWidth,
        height: cardHeight,
      }}
    >
      <img
        src={cardImageUrl}
        alt={card.name}
        className={styles.cardImage}
        onError={(e) => {
          e.currentTarget.style.display = 'none'
          const fallback = e.currentTarget.nextElementSibling as HTMLElement
          if (fallback) fallback.style.display = 'flex'
        }}
      />
      <div className={styles.cardFallback}>
        <span className={styles.cardFallbackName}>
          {card.name}
        </span>
      </div>
      {isSelected && (
        <div className={styles.selectionIndicatorGold}>
          &#10003;
        </div>
      )}
    </div>
  )
}
