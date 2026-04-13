import { useMemo, useState } from 'react'
import { useGameStore } from '@/store/gameStore.ts'
import type { EntityId, SelectCardsDecision } from '@/types'
import { calculateFittingCardWidth, type ResponsiveSizes } from '@/hooks/useResponsive.ts'
import { getCardImageUrl } from '@/utils/cardImages.ts'
import { DecisionCard, DecisionCardPreview } from './DecisionComponents'
import styles from './DecisionUI.module.css'

/** Known MTG card types used for OnePerCardType restriction enforcement. */
const CARD_TYPES = new Set([
  'Artifact', 'Battle', 'Creature', 'Enchantment', 'Instant',
  'Kindred', 'Land', 'Planeswalker', 'Sorcery',
])

/** Extract card types from a type-line string (e.g. "Artifact Creature — Golem" → ["Artifact", "Creature"]). */
function extractCardTypes(typeLine: string): string[] {
  const mainTypes = typeLine.split('—')[0] ?? typeLine
  return mainTypes.trim().split(/\s+/).filter((w) => CARD_TYPES.has(w))
}

/**
 * Card selection decision - select cards from a list.
 */
export function CardSelectionDecision({
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
  const nonSelectableOptions = decision.nonSelectableOptions ?? []
  const totalCardCount = decision.options.length + nonSelectableOptions.length
  const cardWidth = calculateFittingCardWidth(
    totalCardCount,
    availableWidth,
    gap,
    maxCardWidth,
    45
  )

  // OnePerCardType: compute which types are already claimed by selected cards
  const claimedTypes = useMemo(() => {
    if (!decision.onePerCardType) return new Set<string>()
    const types = new Set<string>()
    for (const id of selectedCards) {
      const typeLine = decision.cardInfo?.[id]?.typeLine ?? gameState?.cards[id]?.typeLine ?? ''
      for (const t of extractCardTypes(typeLine)) types.add(t)
    }
    return types
  }, [decision.onePerCardType, decision.cardInfo, selectedCards, gameState?.cards])

  /** Check if a card is disabled by the OnePerCardType restriction. */
  const isCardDisabled = (cardId: EntityId): boolean => {
    if (!decision.onePerCardType) return false
    if (selectedCards.includes(cardId)) return false // already selected — can deselect
    const typeLine = decision.cardInfo?.[cardId]?.typeLine ?? gameState?.cards[cardId]?.typeLine ?? ''
    const types = extractCardTypes(typeLine)
    return types.length > 0 && types.some((t) => claimedTypes.has(t))
  }

  const toggleCard = (cardId: EntityId) => {
    setSelectedCards((prev) => {
      if (prev.includes(cardId)) {
        return prev.filter((id) => id !== cardId)
      }
      // Don't allow selecting more than max
      if (prev.length >= decision.maxSelections) {
        return prev
      }
      // OnePerCardType: block if types already claimed
      if (isCardDisabled(cardId)) {
        return prev
      }
      return [...prev, cardId]
    })
  }

  const handleConfirm = () => {
    submitDecision(selectedCards)
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

  // Look up source card info for context display
  const sourceCard = decision.context.sourceId ? gameState?.cards[decision.context.sourceId] : undefined
  const sourceCardName = decision.context.sourceName ?? sourceCard?.name
  const sourceCardImageUrl = sourceCard ? getCardImageUrl(sourceCard.name, sourceCard.imageUri) : undefined

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
        {decision.minSelections === 0
          ? `Select up to ${decision.maxSelections}`
          : `Selected: ${selectedCards.length} / ${decision.minSelections}${decision.minSelections !== decision.maxSelections ? ` - ${decision.maxSelections}` : ''}`
        }
      </p>

      {(decision.selectedLabel || decision.remainderLabel) && (
        <div className={styles.destinationLabels}>
          {decision.selectedLabel && (
            <span className={styles.selectedLabel}>Selected &rarr; {decision.selectedLabel}</span>
          )}
          {decision.remainderLabel && (
            <span className={styles.remainderLabel}>Not selected &rarr; {decision.remainderLabel}</span>
          )}
        </div>
      )}

      {/* Card options */}
      <div className={styles.cardContainer} style={{ gap }}>
        {decision.options.map((cardId) => {
          // For hidden cards (e.g., opponent's library), use cardInfo from decision
          // For visible cards (e.g., own hand for discard), use gameState.cards
          const cardInfoFromDecision = decision.cardInfo?.[cardId]
          const cardFromState = gameState?.cards[cardId]
          const cardName = cardInfoFromDecision?.name || cardFromState?.name || 'Unknown Card'
          const imageUri = cardInfoFromDecision?.imageUri || cardFromState?.imageUri
          const disabled = isCardDisabled(cardId)
          return (
            <DecisionCard
              key={cardId}
              cardId={cardId}
              cardName={cardName}
              imageUri={imageUri}
              isSelected={selectedCards.includes(cardId)}
              onClick={() => toggleCard(cardId)}
              cardWidth={cardWidth}
              onMouseEnter={() => setHoveredCardId(cardId)}
              onMouseLeave={() => setHoveredCardId(null)}
              nonSelectable={disabled}
            />
          )
        })}
        {nonSelectableOptions.map((cardId) => {
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
              isSelected={false}
              onClick={() => {}}
              cardWidth={cardWidth}
              onMouseEnter={() => setHoveredCardId(cardId)}
              onMouseLeave={() => setHoveredCardId(null)}
              nonSelectable
            />
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
        <button
          onClick={handleConfirm}
          disabled={!canConfirm}
          className={styles.confirmButton}
        >
          {decision.minSelections === 0 && selectedCards.length === 0
            ? 'Select None'
            : 'Confirm Selection'}
        </button>
      </div>

      {/* Card preview on hover (source card or option card) */}
      {isHoveringSource && sourceCardName && !responsive.isMobile && (
        <DecisionCardPreview cardName={sourceCardName} imageUri={sourceCard?.imageUri} />
      )}
      {!isHoveringSource && hoveredCardInfo?.name && !responsive.isMobile && (
        <DecisionCardPreview cardName={hoveredCardInfo.name} imageUri={hoveredCardInfo.imageUri} />
      )}
    </div>
  )
}
