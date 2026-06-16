import { useState } from 'react'
import { useGameStore } from '@/store/gameStore.ts'
import type { EntityId, SelectCardsDecision, ClientCard } from '@/types'
import { ZoneType } from '@/types'
import { calculateFittingCardWidth, type ResponsiveSizes } from '@/hooks/useResponsive.ts'
import { getCardImageUrl } from '@/utils/cardImages.ts'
import { DecisionCard, DecisionCardPreview } from './DecisionComponents'
import styles from './DecisionUI.module.css'

interface ZoneGroup {
  zone: ZoneType
  label: string
  cards: { id: EntityId; card: ClientCard | undefined }[]
}

const ZONE_ORDER: ZoneType[] = [ZoneType.BATTLEFIELD, ZoneType.HAND, ZoneType.GRAVEYARD]

const ZONE_LABELS: Partial<Record<ZoneType, string>> = {
  [ZoneType.BATTLEFIELD]: 'Battlefield',
  [ZoneType.HAND]: 'Hand',
  [ZoneType.GRAVEYARD]: 'Graveyard',
}

/**
 * Multi-zone selection UI for SelectCardsDecision where options span multiple zones
 * (e.g., Lich's Mastery - exile from battlefield, hand, or graveyard).
 *
 * Shows cards grouped by zone with clear section headers so the player
 * knows exactly where each card is.
 */
export function MultiZoneSelectionUI({
  decision,
  responsive,
}: {
  decision: SelectCardsDecision
  responsive: ResponsiveSizes
}) {
  const [selectedCards, setSelectedCards] = useState<EntityId[]>([])
  const [hoveredCardId, setHoveredCardId] = useState<EntityId | null>(null)
  const [minimized, setMinimized] = useState(false)
  const [isHoveringSource, setIsHoveringSource] = useState(false)
  const submitDecision = useGameStore((s) => s.submitDecision)
  const gameState = useGameStore((s) => s.gameState)

  // Group options by zone
  const zoneGroups: ZoneGroup[] = ZONE_ORDER
    .map((zone) => {
      const cards = decision.options
        .map((id) => ({ id, card: gameState?.cards[id] }))
        .filter(({ card }) => card?.zone?.zoneType === zone)
      return { zone, label: ZONE_LABELS[zone] ?? zone, cards }
    })
    .filter((group) => group.cards.length > 0)

  // Catch any cards in unexpected zones
  const knownZones = new Set(ZONE_ORDER)
  const otherCards = decision.options
    .map((id) => ({ id, card: gameState?.cards[id] }))
    .filter(({ card }) => !card?.zone?.zoneType || !knownZones.has(card.zone.zoneType))
  if (otherCards.length > 0) {
    zoneGroups.push({ zone: ZoneType.BATTLEFIELD, label: 'Other', cards: otherCards })
  }

  const hoveredCard = hoveredCardId ? gameState?.cards[hoveredCardId] : null

  const conditionalMinimums = decision.conditionalMinimums ?? []
  const satisfiesConditionalMinimum = conditionalMinimums.some((minimum) => {
    const matching = selectedCards.filter((id) => minimum.matchingOptions.includes(id)).length
    return selectedCards.length >= minimum.minimumSelections && matching >= minimum.requiredMatches
  })
  const requiredMinimum = conditionalMinimums.length > 0
    ? Math.max(...conditionalMinimums.map((minimum) => minimum.requiredSelections))
    : decision.minSelections
  const canConfirm =
    selectedCards.length >= decision.minSelections &&
    selectedCards.length <= decision.maxSelections &&
    (selectedCards.length >= requiredMinimum || satisfiesConditionalMinimum)
  const canSkip = decision.minSelections === 0

  // Calculate card size
  const availableWidth = responsive.viewportWidth - responsive.containerPadding * 2 - 32
  const gap = responsive.isMobile ? 4 : 8
  const maxCardWidth = responsive.isMobile ? 90 : 130
  const maxCardsInAnyGroup = Math.max(...zoneGroups.map((g) => g.cards.length), 1)
  const cardWidth = calculateFittingCardWidth(
    maxCardsInAnyGroup,
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

  const handleSkip = () => {
    submitDecision([])
    setSelectedCards([])
  }

  if (minimized) {
    return (
      <button
        className={styles.floatingReturnButton}
        onClick={() => setMinimized(false)}
      >
        Return to Card Selection
      </button>
    )
  }

  // Source card info
  const sourceCard = decision.context.sourceId ? gameState?.cards[decision.context.sourceId] : undefined
  const sourceCardName = decision.context.sourceName ?? sourceCard?.name
  const sourceCardImageUrl = sourceCard ? getCardImageUrl(sourceCard.name, sourceCard.imageUri) : undefined

  // Count selected per zone for badges
  const selectedByZone = new Map<ZoneType, number>()
  for (const id of selectedCards) {
    const card = gameState?.cards[id]
    const zone = card?.zone?.zoneType
    if (zone) {
      selectedByZone.set(zone, (selectedByZone.get(zone) ?? 0) + 1)
    }
  }

  return (
    <div className={styles.overlay}>
      {/* Source card image */}
      {sourceCardImageUrl && (
        <img
          src={sourceCardImageUrl}
          alt={`Source: ${sourceCardName ?? 'card'}`}
          className={styles.bannerCardImage}
          onMouseEnter={() => setIsHoveringSource(true)}
          onMouseLeave={() => setIsHoveringSource(false)}
        />
      )}

      <h2 className={styles.title}>
        {decision.prompt}
      </h2>

      {sourceCardName && (
        <p className={styles.sourceLabel}>
          {sourceCardName}
        </p>
      )}

      <p className={styles.hint}>
        {conditionalMinimums.length > 0
          ? `Selected: ${selectedCards.length} / ${requiredMinimum}; fewer is allowed if the selection matches the requirement`
          : decision.minSelections === decision.maxSelections
          ? `Selected: ${selectedCards.length} / ${decision.minSelections}`
          : decision.minSelections === 0
            ? `Select up to ${decision.maxSelections}`
            : `Selected: ${selectedCards.length} / ${decision.minSelections} - ${decision.maxSelections}`
        }
      </p>

      {conditionalMinimums.map((minimum) => (
        <div key={`${minimum.requiredSelections}-${minimum.minimumSelections}-${minimum.requiredMatches}`} className={styles.hint}>
          {minimum.description ?? `You may select ${minimum.minimumSelections} if it matches the requirement.`}
        </div>
      ))}

      {/* Zone groups */}
      <div className={styles.multiZoneContainer}>
        {zoneGroups.map((group) => {
          const zoneSelectedCount = selectedByZone.get(group.zone) ?? 0
          return (
            <div key={group.zone} className={styles.zoneSection}>
              <div className={styles.zoneSectionHeader}>
                <span className={styles.zoneSectionLabel}>{group.label}</span>
                <span className={styles.zoneSectionCount}>
                  {group.cards.length} card{group.cards.length !== 1 ? 's' : ''}
                  {zoneSelectedCount > 0 && (
                    <span className={styles.zoneSectionSelected}>
                      {' '}&middot; {zoneSelectedCount} selected
                    </span>
                  )}
                </span>
              </div>
              <div className={styles.cardContainer} style={{ gap }}>
                {group.cards.map(({ id, card }) => {
                  const cardName = card?.name ?? 'Unknown Card'
                  const imageUri = card?.imageUri
                  return (
                    <DecisionCard
                      key={id}
                      cardId={id}
                      cardName={cardName}
                      imageUri={imageUri}
                      isSelected={selectedCards.includes(id)}
                      onClick={() => toggleCard(id)}
                      cardWidth={cardWidth}
                      onMouseEnter={() => setHoveredCardId(id)}
                      onMouseLeave={() => setHoveredCardId(null)}
                    />
                  )
                })}
              </div>
            </div>
          )
        })}
      </div>

      {/* Action buttons */}
      <div className={styles.optionButtonRow}>
        <button
          onClick={() => setMinimized(true)}
          className={styles.viewBattlefieldButton}
        >
          View Battlefield
        </button>
        {canSkip && selectedCards.length === 0 ? (
          <button
            onClick={handleSkip}
            className={styles.confirmButton}
          >
            Select None
          </button>
        ) : (
          <button
            onClick={handleConfirm}
            disabled={!canConfirm}
            className={styles.confirmButton}
          >
            Confirm Selection ({selectedCards.length})
          </button>
        )}
      </div>

      {/* Card preview on hover */}
      {isHoveringSource && sourceCardName && !responsive.isMobile && (
        <DecisionCardPreview cardName={sourceCardName} imageUri={sourceCard?.imageUri} />
      )}
      {!isHoveringSource && hoveredCard && !responsive.isMobile && (
        <DecisionCardPreview cardName={hoveredCard.name} imageUri={hoveredCard.imageUri} />
      )}
    </div>
  )
}
