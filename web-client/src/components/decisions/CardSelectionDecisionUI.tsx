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

/** The five basic land types, used for the OnePerBasicLandType restriction. */
const BASIC_LAND_TYPES = new Set(['Plains', 'Island', 'Swamp', 'Mountain', 'Forest'])

/** Extract basic land subtypes from a type-line (e.g. "Land — Plains Island" → ["Plains", "Island"]). */
function extractBasicLandTypes(typeLine: string): string[] {
  const subtypes = typeLine.split(/[—–-]/)[1] ?? ''
  return subtypes.trim().split(/\s+/).filter((w) => BASIC_LAND_TYPES.has(w))
}

/** Map colour name → display symbol + CSS colour for the indicator pip. */
const COLOR_PIPS: Record<string, { symbol: string; bg: string; fg: string }> = {
  WHITE: { symbol: 'W', bg: '#f8f6e8', fg: '#3a3320' },
  BLUE: { symbol: 'U', bg: '#a6dafc', fg: '#0a2a48' },
  BLACK: { symbol: 'B', bg: '#2b2a2a', fg: '#e6e2d8' },
  RED: { symbol: 'R', bg: '#f5a986', fg: '#4a1a0a' },
  GREEN: { symbol: 'G', bg: '#a8d59e', fg: '#0e3214' },
}

/** Parse colour identity from mana cost (fallback when explicit colors aren't shipped). */
function parseColorsFromManaCost(manaCost: string): string[] {
  const set = new Set<string>()
  for (const ch of manaCost) {
    if (ch === 'W') set.add('WHITE')
    else if (ch === 'U') set.add('BLUE')
    else if (ch === 'B') set.add('BLACK')
    else if (ch === 'R') set.add('RED')
    else if (ch === 'G') set.add('GREEN')
  }
  return [...set]
}

/** Compute mana value from a "{2}{R}{R}" string. {X} contributes 0 (CR 202.3e). */
function manaValueFromCost(manaCost: string): number {
  let total = 0
  const re = /\{([^}]+)\}/g
  let match: RegExpExecArray | null
  while ((match = re.exec(manaCost)) !== null) {
    const sym = match[1] ?? ''
    const num = parseInt(sym, 10)
    if (!isNaN(num)) total += num
    else if (sym === 'X' || sym === 'Y' || sym === 'Z') total += 0
    else total += 1 // any single-letter pip (W/U/B/R/G/C/S, hybrid, phyrexian)
  }
  return total
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

  /** Resolve a card's colour identity from cardInfo, gameState, or its mana cost. */
  const colorsForCard = (cardId: EntityId): string[] => {
    const fromInfo = decision.cardInfo?.[cardId]?.colors
    if (fromInfo && fromInfo.length > 0) return [...fromInfo]
    const fromState = gameState?.cards[cardId]?.colors
    if (fromState && fromState.length > 0) return [...fromState]
    const manaCost = decision.cardInfo?.[cardId]?.manaCost ?? gameState?.cards[cardId]?.manaCost ?? ''
    return parseColorsFromManaCost(typeof manaCost === 'string' ? manaCost : '')
  }

  // OnePerColor: compute which colours are already claimed by selected cards
  const claimedColors = useMemo(() => {
    if (!decision.onePerColor) return new Set<string>()
    const colors = new Set<string>()
    for (const id of selectedCards) {
      for (const c of colorsForCard(id)) colors.add(c)
    }
    return colors
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [decision.onePerColor, decision.cardInfo, selectedCards, gameState?.cards])

  // OnePerBasicLandType: compute which basic land types are already claimed by selected cards.
  const claimedLandTypes = useMemo(() => {
    if (!decision.onePerBasicLandType) return new Set<string>()
    const types = new Set<string>()
    for (const id of selectedCards) {
      const typeLine = decision.cardInfo?.[id]?.typeLine ?? gameState?.cards[id]?.typeLine ?? ''
      for (const t of extractBasicLandTypes(typeLine)) types.add(t)
    }
    return types
  }, [decision.onePerBasicLandType, decision.cardInfo, selectedCards, gameState?.cards])

  /** Read a card's mana value: prefer gameState (server-computed), fall back to parsing the cost string. */
  const manaValueForCard = (cardId: EntityId): number => {
    const fromState = gameState?.cards[cardId]?.manaValue
    if (typeof fromState === 'number') return fromState
    const cost = decision.cardInfo?.[cardId]?.manaCost ?? gameState?.cards[cardId]?.manaCost ?? ''
    return manaValueFromCost(typeof cost === 'string' ? cost : '')
  }

  // TotalManaValueAtMost: running sum of selected cards' mana values.
  const totalManaValueSelected = useMemo(() => {
    if (decision.maxTotalManaValue == null) return 0
    return selectedCards.reduce((sum, id) => sum + manaValueForCard(id), 0)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [decision.maxTotalManaValue, selectedCards, decision.cardInfo, gameState?.cards])

  /** Check if a card is disabled by an active selection restriction. */
  const isCardDisabled = (cardId: EntityId): boolean => {
    if (selectedCards.includes(cardId)) return false // already selected — can deselect
    if (decision.onePerCardType) {
      const typeLine = decision.cardInfo?.[cardId]?.typeLine ?? gameState?.cards[cardId]?.typeLine ?? ''
      const types = extractCardTypes(typeLine)
      if (types.length > 0 && types.some((t) => claimedTypes.has(t))) return true
    }
    if (decision.onePerColor) {
      const colors = colorsForCard(cardId)
      // Colourless cards are unconstrained.
      if (colors.length > 0 && colors.some((c) => claimedColors.has(c))) return true
    }
    if (decision.maxTotalManaValue != null) {
      if (totalManaValueSelected + manaValueForCard(cardId) > decision.maxTotalManaValue) return true
    }
    if (decision.onePerBasicLandType) {
      const typeLine = decision.cardInfo?.[cardId]?.typeLine ?? gameState?.cards[cardId]?.typeLine ?? ''
      const landTypes = extractBasicLandTypes(typeLine)
      // A land with no basic land type can't be kept; one sharing a claimed type is blocked.
      if (landTypes.length === 0 || landTypes.some((t) => claimedLandTypes.has(t))) return true
    }
    return false
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
        {conditionalMinimums.length > 0
          ? `Selected: ${selectedCards.length} / ${requiredMinimum}; fewer is allowed if the selection matches the requirement`
          : decision.minSelections === 0
          ? `Select up to ${decision.maxSelections}`
          : `Selected: ${selectedCards.length} / ${decision.minSelections}${decision.minSelections !== decision.maxSelections ? ` - ${decision.maxSelections}` : ''}`
        }
      </p>

      {conditionalMinimums.map((minimum) => (
        <div key={`${minimum.requiredSelections}-${minimum.minimumSelections}-${minimum.requiredMatches}`} className={styles.hint}>
          {minimum.description ?? `You may select ${minimum.minimumSelections} if it matches the requirement.`}
        </div>
      ))}

      {decision.onePerColor && (
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: 6,
            margin: '0 0 8px',
            fontSize: 12,
            opacity: 0.85,
          }}
        >
          <span>At most one card per colour:</span>
          {((decision.availableColors && decision.availableColors.length > 0)
            ? decision.availableColors
            : ['WHITE', 'BLUE', 'BLACK', 'RED', 'GREEN']
          ).map((c) => {
            const pip = COLOR_PIPS[c]
            if (!pip) return null
            const claimed = claimedColors.has(c)
            return (
              <span
                key={c}
                title={claimed ? `${pip.symbol} claimed` : pip.symbol}
                style={{
                  display: 'inline-flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  width: 18,
                  height: 18,
                  borderRadius: '50%',
                  background: pip.bg,
                  color: pip.fg,
                  fontWeight: 700,
                  fontSize: 11,
                  lineHeight: 1,
                  border: claimed ? '2px solid #ffd668' : '1px solid rgba(0,0,0,0.3)',
                  opacity: claimed ? 1 : 0.45,
                  boxShadow: claimed ? '0 0 6px rgba(255,214,104,0.7)' : 'none',
                }}
              >
                {pip.symbol}
              </span>
            )
          })}
        </div>
      )}

      {decision.maxTotalManaValue != null && (
        <div
          style={{
            margin: '0 0 8px',
            fontSize: 12,
            opacity: 0.85,
          }}
        >
          Total mana value: {totalManaValueSelected} / {decision.maxTotalManaValue}
        </div>
      )}

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
