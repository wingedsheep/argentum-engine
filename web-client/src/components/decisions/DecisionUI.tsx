import { useState, useMemo, useEffect } from 'react'
import { useGameStore } from '../../store/gameStore'
import type { DecisionSelectionState } from '../../store/gameStore'
import type { EntityId, SelectCardsDecision, ChooseTargetsDecision, YesNoDecision, ChooseNumberDecision, ChooseOptionDecision, ChooseColorDecision, SelectManaSourcesDecision, SplitPilesDecision, ClientCard, ClientGameState } from '../../types'
import { ColorDisplayNames } from '../../types'
import { ZoneType } from '../../types'
import { useResponsive, calculateFittingCardWidth, type ResponsiveSizes } from '../../hooks/useResponsive'
import { LibrarySearchUI } from './LibrarySearchUI'
import { ReorderCardsUI } from './ReorderCardsUI'
import { OrderBlockersUI } from './OrderBlockersUI'
import { CombatDamageAssignmentModal } from './CombatDamageAssignmentModal'
import { ZoneSelectionUI, type ZoneCardInfo } from './ZoneSelectionUI'
import { getCardImageUrl } from '../../utils/cardImages'
import { AbilityText } from '../ui/ManaSymbols'
import styles from './DecisionUI.module.css'

/**
 * Check if all legal targets in a ChooseTargetsDecision are players.
 */
function isPlayerOnlyTargeting(decision: ChooseTargetsDecision, playerIds: EntityId[]): boolean {
  const legalTargets = decision.legalTargets[0] ?? []
  if (legalTargets.length === 0) return false
  return legalTargets.every((targetId) => playerIds.includes(targetId))
}

/**
 * Check if all legal targets in a ChooseTargetsDecision are in graveyards.
 * Returns the graveyard cards if true, null otherwise.
 */
function getGraveyardTargets(decision: ChooseTargetsDecision, gameState: ClientGameState | null): ClientCard[] | null {
  if (!gameState) return null
  const legalTargets = decision.legalTargets[0] ?? []
  if (legalTargets.length === 0) return null

  const graveyardCards: ClientCard[] = []
  for (const targetId of legalTargets) {
    const card = gameState.cards[targetId]
    if (!card || card.zone?.zoneType !== ZoneType.GRAVEYARD) {
      return null // Not all targets are graveyard cards
    }
    graveyardCards.push(card)
  }
  return graveyardCards
}

/**
 * Decision UI overlay for pending decisions (e.g., discard to hand size, library search).
 */
export function DecisionUI() {
  const pendingDecision = useGameStore((state) => state.pendingDecision)
  const gameState = useGameStore((state) => state.gameState)
  const responsive = useResponsive()

  if (!pendingDecision) return null

  // Handle SelectManaSourcesDecision (mana source selection for Lightning Rift etc.)
  if (pendingDecision.type === 'SelectManaSourcesDecision') {
    return <ManaSourceSelectionUI decision={pendingDecision} />
  }

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
      return <OrderBlockersUI key={pendingDecision.id} decision={pendingDecision} responsive={responsive} />
    }
    // Other ordering decisions could use a generic ordering UI (not yet implemented)
    return null
  }

  // Handle YesNoDecision (e.g., "You may shuffle your library")
  if (pendingDecision.type === 'YesNoDecision') {
    // Combat trigger with a triggering entity that is a battlefield card:
    // rendered inline on the card (GameCard + GameBoard prompt bar)
    if (pendingDecision.context.triggeringEntityId && gameState?.combat) {
      const triggeringCard = gameState.cards[pendingDecision.context.triggeringEntityId]
      if (triggeringCard?.zone?.zoneType === ZoneType.BATTLEFIELD) {
        return null
      }
    }
    return (
      <div className={styles.overlay}>
        <YesNoDecisionUI decision={pendingDecision} />
      </div>
    )
  }

  // Handle ChooseNumberDecision (e.g., "Choose how many cards to draw")
  if (pendingDecision.type === 'ChooseNumberDecision') {
    return (
      <div className={styles.overlay}>
        <ChooseNumberDecisionUI decision={pendingDecision} />
      </div>
    )
  }

  // Handle ChooseOptionDecision (e.g., "Choose a creature type")
  if (pendingDecision.type === 'ChooseOptionDecision') {
    return (
      <ChooseOptionDecisionUI key={pendingDecision.id} decision={pendingDecision} />
    )
  }

  // DistributeDecision is handled inline on the board (GameCard + LifeDisplay + GameBoard confirm bar)
  if (pendingDecision.type === 'DistributeDecision') {
    return null
  }

  // Handle AssignDamageDecision (combat damage assignment to blockers/player)
  if (pendingDecision.type === 'AssignDamageDecision') {
    return <CombatDamageAssignmentModal decision={pendingDecision} />
  }

  // Handle ChooseColorDecision (e.g., "Choose a color for protection")
  if (pendingDecision.type === 'ChooseColorDecision') {
    return (
      <div className={styles.overlay}>
        <ChooseColorDecisionUI decision={pendingDecision} />
      </div>
    )
  }

  // Handle SelectCardsDecision
  if (pendingDecision.type === 'SelectCardsDecision') {
    // If useTargetingUI is true, use targeting-style UI (click on battlefield)
    if (pendingDecision.useTargetingUI) {
      return (
        <BattlefieldSelectionUI
          decision={pendingDecision}
        />
      )
    }

    // Default: full-screen modal
    return (
      <CardSelectionDecision key={pendingDecision.id} decision={pendingDecision} responsive={responsive} />
    )
  }

  // Handle ChooseTargetsDecision
  if (pendingDecision.type === 'ChooseTargetsDecision') {
    const playerIds = gameState?.players.map((p) => p.playerId) ?? []
    const isPlayerOnly = isPlayerOnlyTargeting(pendingDecision, playerIds)
    const graveyardTargets = getGraveyardTargets(pendingDecision, gameState)

    // If all targets are in graveyards, show a selection overlay
    if (graveyardTargets && graveyardTargets.length > 0) {
      return (
        <GraveyardTargetingUI
          decision={pendingDecision}
          graveyardCards={graveyardTargets}
          responsive={responsive}
        />
      )
    }

    // Player-only targeting: simple banner (auto-submit via LifeDisplay click)
    if (isPlayerOnly) {
      return (
        <div className={styles.sideBannerTarget}>
          <div className={styles.bannerTitle}>
            Choose Target
          </div>
          <div className={styles.prompt}>
            {pendingDecision.prompt}
          </div>
          <div className={styles.hint}>
            Click a player's life total
          </div>
        </div>
      )
    }

    // Battlefield targeting: use selection state with Confirm/Decline buttons
    return (
      <BattlefieldTargetingUI decision={pendingDecision} />
    )
  }

  // Handle SplitPilesDecision (e.g., Surveil - put cards on top of library or into graveyard)
  if (pendingDecision.type === 'SplitPilesDecision') {
    return (
      <div className={styles.overlay}>
        <SplitPilesUI decision={pendingDecision} responsive={responsive} />
      </div>
    )
  }

  // Other decision types not yet implemented
  return null
}

/**
 * Battlefield selection UI for SelectCardsDecision with useTargetingUI=true.
 * Shows a side banner and allows clicking cards on the battlefield to select them.
 */
function BattlefieldSelectionUI({
  decision,
}: {
  decision: SelectCardsDecision
}) {
  const startDecisionSelection = useGameStore((s) => s.startDecisionSelection)
  const decisionSelectionState = useGameStore((s) => s.decisionSelectionState)
  const cancelDecisionSelection = useGameStore((s) => s.cancelDecisionSelection)
  const submitDecision = useGameStore((s) => s.submitDecision)

  // Start decision selection state when this component mounts
  useEffect(() => {
    const selectionState: DecisionSelectionState = {
      decisionId: decision.id,
      validOptions: decision.options,
      selectedOptions: [],
      minSelections: decision.minSelections,
      maxSelections: decision.maxSelections,
      prompt: decision.prompt,
    }
    startDecisionSelection(selectionState)

    // Cleanup when unmounting
    return () => {
      cancelDecisionSelection()
    }
  }, [decision.id])

  const selectedCount = decisionSelectionState?.selectedOptions.length ?? 0
  const minSelections = decision.minSelections
  const maxSelections = decision.maxSelections
  const canConfirm = selectedCount >= minSelections && selectedCount <= maxSelections
  const canSkip = minSelections === 0

  const handleConfirm = () => {
    if (canConfirm && decisionSelectionState) {
      submitDecision(decisionSelectionState.selectedOptions)
      cancelDecisionSelection()
    }
  }

  const handleSkip = () => {
    submitDecision([])
    cancelDecisionSelection()
  }

  // Side banner (similar to ChooseTargetsDecision)
  return (
    <div className={styles.sideBannerSelection}>
      <div className={styles.bannerTitleSelection}>
        {decision.prompt}
      </div>
      <div className={styles.hint}>
        {selectedCount > 0
          ? `${selectedCount} / ${maxSelections} selected`
          : 'Click cards to select'}
        {minSelections > 0 && ` (min ${minSelections})`}
      </div>

      {/* Confirm/Skip buttons */}
      <div className={styles.buttonContainerSmall}>
        {canSkip && selectedCount === 0 && (
          <button onClick={handleSkip} className={`${styles.confirmButton} ${styles.confirmButtonSmall}`}>
            Select None
          </button>
        )}
        {selectedCount > 0 && (
          <button
            onClick={handleConfirm}
            disabled={!canConfirm}
            className={`${styles.confirmButton} ${styles.confirmButtonSmall}`}
          >
            Confirm ({selectedCount})
          </button>
        )}
      </div>
    </div>
  )
}

/**
 * Battlefield targeting UI for ChooseTargetsDecision (non-player, non-graveyard targets).
 * Shows a side banner with Confirm/Decline buttons, uses decisionSelectionState for toggle-to-select.
 */
function BattlefieldTargetingUI({
  decision,
}: {
  decision: ChooseTargetsDecision
}) {
  const startDecisionSelection = useGameStore((s) => s.startDecisionSelection)
  const decisionSelectionState = useGameStore((s) => s.decisionSelectionState)
  const cancelDecisionSelection = useGameStore((s) => s.cancelDecisionSelection)
  const submitTargetsDecision = useGameStore((s) => s.submitTargetsDecision)
  const gameState = useGameStore((s) => s.gameState)
  const [isHoveringSource, setIsHoveringSource] = useState(false)
  const responsive = useResponsive()

  const targetReq = decision.targetRequirements[0]
  const minTargets = targetReq?.minTargets ?? 1
  const maxTargets = targetReq?.maxTargets ?? 1
  const legalTargets = decision.legalTargets[0] ?? []

  // Look up source card image from game state
  const sourceId = decision.context.sourceId
  const sourceCard = sourceId ? gameState?.cards[sourceId] : undefined
  const sourceImageUrl = sourceCard ? getCardImageUrl(sourceCard.name, sourceCard.imageUri) : undefined

  // Start decision selection state when this component mounts
  useEffect(() => {
    const selectionState: DecisionSelectionState = {
      decisionId: decision.id,
      validOptions: [...legalTargets],
      selectedOptions: [],
      minSelections: minTargets,
      maxSelections: maxTargets,
      prompt: decision.prompt,
    }
    startDecisionSelection(selectionState)

    return () => {
      cancelDecisionSelection()
    }
  }, [decision.id])

  const selectedCount = decisionSelectionState?.selectedOptions.length ?? 0
  const canConfirm = selectedCount >= minTargets && selectedCount <= maxTargets
  const canDecline = minTargets === 0

  const handleConfirm = () => {
    if (canConfirm && decisionSelectionState) {
      submitTargetsDecision({ 0: decisionSelectionState.selectedOptions })
      cancelDecisionSelection()
    }
  }

  const handleDecline = () => {
    submitTargetsDecision({ 0: [] })
    cancelDecisionSelection()
  }

  return (
    <div className={styles.sideBannerSelection}>
      {sourceImageUrl && (
        <img
          src={sourceImageUrl}
          alt={`Source: ${decision.context.sourceName ?? 'card'}`}
          className={styles.bannerCardImage}
          onMouseEnter={() => setIsHoveringSource(true)}
          onMouseLeave={() => setIsHoveringSource(false)}
        />
      )}
      {isHoveringSource && sourceCard && !responsive.isMobile && (
        <DecisionCardPreview cardName={sourceCard.name} imageUri={sourceCard.imageUri} />
      )}
      <div className={styles.bannerTitleSelection}>
        Choose Target
      </div>
      <div className={styles.hint}>
        {decision.prompt}
      </div>
      <div className={styles.hint}>
        {selectedCount > 0
          ? `${selectedCount} / ${maxTargets} selected`
          : 'Click a valid target'}
      </div>

      <div className={styles.buttonContainerSmall}>
        {canDecline && selectedCount === 0 && (
          <button onClick={handleDecline} className={`${styles.confirmButton} ${styles.confirmButtonSmall}`}>
            Decline
          </button>
        )}
        {selectedCount > 0 && (
          <button
            onClick={handleConfirm}
            disabled={!canConfirm}
            className={`${styles.confirmButton} ${styles.confirmButtonSmall}`}
          >
            Confirm ({selectedCount})
          </button>
        )}
      </div>
    </div>
  )
}

/**
 * Yes/No decision - make a binary choice.
 */
function YesNoDecisionUI({
  decision,
}: {
  decision: YesNoDecision
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
      <h2 className={styles.title}>
        <AbilityText text={decision.prompt} size={20} />
      </h2>

      {decision.context.sourceName && (
        <p className={styles.subtitle}>
          {decision.context.sourceName}
        </p>
      )}

      {/* Yes/No buttons */}
      <div className={styles.buttonContainer}>
        <button onClick={handleYes} className={styles.yesButton}>
          <AbilityText text={decision.yesText} size={16} />
        </button>
        <button onClick={handleNo} className={styles.noButton}>
          <AbilityText text={decision.noText} size={16} />
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
}: {
  decision: ChooseNumberDecision
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
      <h2 className={styles.title}>
        {decision.prompt}
      </h2>

      {decision.context.sourceName && (
        <p className={styles.subtitle}>
          {decision.context.sourceName}
        </p>
      )}

      {/* Number selection buttons */}
      <div className={styles.numberContainer}>
        {numbers.map((num) => (
          <button
            key={num}
            onClick={() => setSelectedNumber(num)}
            className={`${styles.numberButton} ${selectedNumber === num ? styles.numberButtonSelected : ''}`}
          >
            {num}
          </button>
        ))}
      </div>

      {/* Confirm button */}
      <button onClick={handleConfirm} className={styles.confirmButton}>
        Confirm
      </button>
    </>
  )
}

/**
 * Choose option decision - select from a list of string options.
 * Uses a searchable scrollable list for large option sets (e.g., creature types).
 */
function ChooseOptionDecisionUI({
  decision,
}: {
  decision: ChooseOptionDecision
}) {
  const [filter, setFilter] = useState(decision.defaultSearch ?? '')
  const [minimized, setMinimized] = useState(false)
  const [hoveredPreviewCard, setHoveredPreviewCard] = useState<{ name: string; imageUri: string | null | undefined } | null>(null)

  // Auto-select: defaultSearch match, or the option with the most cards
  const initialIndex = useMemo(() => {
    if (decision.defaultSearch) {
      const idx = decision.options.findIndex((opt) => opt.toLowerCase() === decision.defaultSearch!.toLowerCase())
      if (idx >= 0) return idx
    }
    if (decision.optionCardIds) {
      let bestIndex = -1
      let bestCount = 0
      for (let i = 0; i < decision.options.length; i++) {
        const count = decision.optionCardIds[i]?.length ?? 0
        if (count > bestCount) {
          bestCount = count
          bestIndex = i
        }
      }
      if (bestIndex >= 0) return bestIndex
    }
    return null
  }, [decision.defaultSearch, decision.options, decision.optionCardIds])

  const [selectedIndex, setSelectedIndex] = useState<number | null>(initialIndex)
  const [isHoveringSource, setIsHoveringSource] = useState(false)
  const submitOptionDecision = useGameStore((s) => s.submitOptionDecision)
  const gameState = useGameStore((s) => s.gameState)
  const responsive = useResponsive()

  // Source card image for context
  const sourceCard = decision.context.sourceId ? gameState?.cards[decision.context.sourceId] : undefined
  const sourceCardName = decision.context.sourceName ?? sourceCard?.name
  const sourceCardImageUrl = sourceCard ? getCardImageUrl(sourceCard.name, sourceCard.imageUri) : undefined

  const hasCardIds = !!decision.optionCardIds

  const filteredOptions = useMemo(() => {
    const mapped = decision.options.map((opt, i) => {
      const cardCount = decision.optionCardIds?.[i]?.length
      const label = cardCount != null ? `${opt} (${cardCount})` : opt
      return { label, index: i }
    })
    if (!filter) return mapped
    const lower = filter.toLowerCase()
    return mapped.filter((opt) => opt.label.toLowerCase().includes(lower))
  }, [decision.options, decision.optionCardIds, filter])

  // Get card previews for the selected option
  const previewCards = useMemo(() => {
    if (selectedIndex === null || !decision.optionCardIds || !gameState) return []
    const cardIds = decision.optionCardIds[selectedIndex] ?? []
    const results: { id: EntityId; name: string; imageUri: string | null | undefined }[] = []
    for (const id of cardIds) {
      const card = gameState.cards[id]
      if (card) {
        results.push({ id, name: card.name, imageUri: card.imageUri })
      }
    }
    return results
  }, [selectedIndex, decision.optionCardIds, gameState])

  const handleConfirm = () => {
    if (selectedIndex !== null) {
      submitOptionDecision(selectedIndex)
    }
  }

  if (minimized) {
    return (
      <button
        className={styles.floatingReturnButton}
        onClick={() => setMinimized(false)}
      >
        Return to {decision.prompt}
      </button>
    )
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

      {/* Search filter */}
      <input
        type="text"
        value={filter}
        onChange={(e) => setFilter(e.target.value)}
        placeholder="Search..."
        className={styles.optionSearchInput}
        autoFocus
      />

      {/* Scrollable option list */}
      <div className={styles.optionList}>
        {filteredOptions.map((opt) => (
          <button
            key={opt.index}
            onClick={() => setSelectedIndex(opt.index)}
            className={`${styles.optionItem} ${selectedIndex === opt.index ? styles.optionItemSelected : ''}`}
          >
            {opt.label}
          </button>
        ))}
        {filteredOptions.length === 0 && (
          <p className={styles.noCardsMessage}>No matching options</p>
        )}
      </div>

      {/* Card previews for selected option */}
      {hasCardIds && previewCards.length > 0 && (
        <div className={styles.optionCardPreview}>
          {previewCards.map((card) => {
            const imgUrl = getCardImageUrl(card.name, card.imageUri)
            return (
              <div
                key={card.id}
                className={styles.optionPreviewCard}
                onMouseEnter={() => setHoveredPreviewCard({ name: card.name, imageUri: card.imageUri })}
                onMouseLeave={() => setHoveredPreviewCard(null)}
              >
                <img
                  src={imgUrl}
                  alt={card.name}
                  className={styles.optionPreviewImage}
                  onError={(e) => {
                    e.currentTarget.style.display = 'none'
                    const fallback = e.currentTarget.nextElementSibling as HTMLElement
                    if (fallback) fallback.style.display = 'flex'
                  }}
                />
                <div className={styles.optionPreviewFallback}>
                  <span className={styles.cardFallbackName}>{card.name}</span>
                </div>
              </div>
            )
          })}
        </div>
      )}

      {/* Hover-to-zoom preview (source card or option card) */}
      {isHoveringSource && sourceCardName && !responsive.isMobile && (
        <DecisionCardPreview cardName={sourceCardName} imageUri={sourceCard?.imageUri} />
      )}
      {!isHoveringSource && hoveredPreviewCard && !responsive.isMobile && (
        <DecisionCardPreview cardName={hoveredPreviewCard.name} imageUri={hoveredPreviewCard.imageUri} />
      )}

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
          disabled={selectedIndex === null}
          className={styles.confirmButton}
        >
          Confirm
        </button>
      </div>
    </div>
  )
}

/**
 * Color map for styling color buttons.
 */
const COLOR_STYLES: Record<string, { bg: string; hover: string; selected: string }> = {
  WHITE: { bg: '#f5f0e0', hover: '#faf6eb', selected: '#fff8dc' },
  BLUE: { bg: '#1a4a7a', hover: '#1e5a94', selected: '#2266aa' },
  BLACK: { bg: '#2a2a2a', hover: '#3a3a3a', selected: '#444444' },
  RED: { bg: '#8b2020', hover: '#a02828', selected: '#bb3030' },
  GREEN: { bg: '#1a5a2a', hover: '#1e6e34', selected: '#228b3c' },
}

/**
 * Choose color decision - select a Magic color.
 */
function ChooseColorDecisionUI({
  decision,
}: {
  decision: ChooseColorDecision
}) {
  const submitColorDecision = useGameStore((s) => s.submitColorDecision)

  const handleColorClick = (color: string) => {
    submitColorDecision(color)
  }

  return (
    <>
      <h2 className={styles.title}>
        {decision.prompt}
      </h2>

      {decision.context.sourceName && (
        <p className={styles.subtitle}>
          {decision.context.sourceName}
        </p>
      )}

      <div className={styles.numberContainer}>
        {decision.availableColors.map((color) => {
          const colorStyle = COLOR_STYLES[color]
          const displayName = ColorDisplayNames[color as keyof typeof ColorDisplayNames] ?? color
          const isLight = color === 'WHITE'
          return (
            <button
              key={color}
              onClick={() => handleColorClick(color)}
              style={{
                backgroundColor: colorStyle?.bg ?? '#555',
                color: isLight ? '#1a1a1a' : '#f0f0f0',
                border: `2px solid ${isLight ? '#c0b080' : 'transparent'}`,
                padding: '12px 24px',
                fontSize: 'var(--font-lg)',
                fontWeight: 'var(--font-weight-semibold)',
                borderRadius: 'var(--radius-lg)',
                cursor: 'pointer',
                minWidth: '100px',
                transition: 'all 0.15s',
              }}
              onMouseEnter={(e) => {
                if (colorStyle) e.currentTarget.style.backgroundColor = colorStyle.hover
                e.currentTarget.style.transform = 'translateY(-2px)'
              }}
              onMouseLeave={(e) => {
                if (colorStyle) e.currentTarget.style.backgroundColor = colorStyle.bg
                e.currentTarget.style.transform = 'translateY(0)'
              }}
            >
              {displayName}
            </button>
          )
        })}
      </div>
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
  const [minimized, setMinimized] = useState(false)
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
  const [isHoveringSource, setIsHoveringSource] = useState(false)

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
            <span className={styles.selectedLabel}>Selected → {decision.selectedLabel}</span>
          )}
          {decision.remainderLabel && (
            <span className={styles.remainderLabel}>Not selected → {decision.remainderLabel}</span>
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

/**
 * Graveyard targeting UI - uses the shared ZoneSelectionUI component.
 * Supports selecting from multiple graveyards with tabs.
 */
function GraveyardTargetingUI({
  decision,
  graveyardCards,
  responsive,
}: {
  decision: ChooseTargetsDecision
  graveyardCards: ClientCard[]
  responsive: ResponsiveSizes
}) {
  const submitTargetsDecision = useGameStore((s) => s.submitTargetsDecision)
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
  const handleMouseEnter = (cardId: EntityId) => {
    setHoveredCardId(cardId)
    hoverCard(cardId)
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
            onMouseEnter={() => handleMouseEnter(card.id)}
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
  onMouseEnter?: () => void
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

/**
 * Card preview overlay - shows enlarged card when hovering.
 */
function DecisionCardPreview({ cardName, imageUri }: { cardName: string; imageUri?: string | null | undefined }) {
  const cardImageUrl = getCardImageUrl(cardName, imageUri, 'large')

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
          alt={`${cardName} preview`}
          className={styles.previewImage}
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
  onMouseEnter,
  onMouseLeave,
}: {
  cardId: EntityId
  cardName: string
  imageUri?: string | null | undefined
  isSelected: boolean
  onClick: () => void
  cardWidth?: number
  onMouseEnter?: () => void
  onMouseLeave?: () => void
}) {
  const cardImageUrl = getCardImageUrl(cardName, imageUri)

  const cardRatio = 1.4
  const cardHeight = Math.round(cardWidth * cardRatio)

  const cardClasses = [
    styles.decisionCard,
    isSelected ? styles.decisionCardSelected : styles.decisionCardDefault,
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
      {/* Card image */}
      <img
        src={cardImageUrl}
        alt={cardName}
        className={styles.cardImage}
        onError={(e) => {
          e.currentTarget.style.display = 'none'
          const fallback = e.currentTarget.nextElementSibling as HTMLElement
          if (fallback) fallback.style.display = 'flex'
        }}
      />

      {/* Fallback when image fails */}
      <div className={styles.cardFallback}>
        <span className={styles.cardFallbackName}>
          {cardName}
        </span>
      </div>

      {/* Selection indicator */}
      {isSelected && (
        <div className={styles.selectionIndicator}>
          ✓
        </div>
      )}
    </div>
  )
}

/**
 * Split piles decision UI - assign cards to labeled piles (e.g., Surveil).
 * For single-card decisions: shows the card with pile buttons for quick assignment.
 * For multi-card decisions: shows cards with pile assignment toggles.
 */
function SplitPilesUI({
  decision,
  responsive,
}: {
  decision: SplitPilesDecision
  responsive: ResponsiveSizes
}) {
  const submitSplitPilesDecision = useGameStore((s) => s.submitSplitPilesDecision)
  const gameState = useGameStore((s) => s.gameState)

  // Track which pile each card is assigned to (default: pile 0 = first pile)
  const [assignments, setAssignments] = useState<Record<EntityId, number>>(() => {
    const initial: Record<EntityId, number> = {}
    for (const cardId of decision.cards) {
      initial[cardId] = 0
    }
    return initial
  })

  const labels = decision.pileLabels.length > 0
    ? decision.pileLabels
    : Array.from({ length: decision.numberOfPiles }, (_, i) => `Pile ${i + 1}`)

  const handleAssignToPile = (cardId: EntityId, pileIndex: number) => {
    setAssignments((prev) => ({ ...prev, [cardId]: pileIndex }))
  }

  const handleSubmit = (overrideAssignments?: Record<EntityId, number>) => {
    const finalAssignments = overrideAssignments ?? assignments
    // Build piles array from assignments
    const piles: EntityId[][] = Array.from({ length: decision.numberOfPiles }, () => [])
    for (const cardId of decision.cards) {
      const pileIndex = finalAssignments[cardId] ?? 0
      piles[pileIndex]?.push(cardId)
    }
    submitSplitPilesDecision(piles)
  }

  // For single-card surveil: clicking a pile button directly assigns and submits
  if (decision.cards.length === 1) {
    const cardId = decision.cards[0]!
    const cardInfoFromDecision = decision.cardInfo?.[cardId]
    const cardFromState = gameState?.cards[cardId]
    const cardName = cardInfoFromDecision?.name || cardFromState?.name || 'Unknown Card'
    const imageUri = cardInfoFromDecision?.imageUri || cardFromState?.imageUri

    const cardWidth = responsive.isMobile ? 120 : 180
    const cardHeight = Math.round(cardWidth * 1.4)
    const cardImageUrl = getCardImageUrl(cardName, imageUri)

    return (
      <>
        <h2 className={styles.title}>
          {decision.context.sourceName ?? 'Surveil'}
        </h2>
        <p className={styles.hint}>
          {decision.prompt}
        </p>

        {/* Single card display */}
        <div
          style={{
            width: cardWidth,
            height: cardHeight,
            borderRadius: 'var(--radius-lg)',
            overflow: 'hidden',
            border: '2px solid var(--border-card)',
          }}
        >
          <img
            src={cardImageUrl}
            alt={cardName}
            className={styles.cardImage}
            onError={(e) => {
              e.currentTarget.style.display = 'none'
              const fallback = e.currentTarget.nextElementSibling as HTMLElement
              if (fallback) fallback.style.display = 'flex'
            }}
          />
          <div className={styles.cardFallback}>
            <span className={styles.cardFallbackName}>{cardName}</span>
          </div>
        </div>

        {/* Pile buttons */}
        <div className={styles.buttonContainer}>
          {labels.map((label, index) => (
            <button
              key={index}
              onClick={() => {
                const singleAssignment: Record<EntityId, number> = { [cardId]: index }
                handleSubmit(singleAssignment)
              }}
              className={index === 0 ? styles.yesButton : styles.noButton}
              data-testid={`pile-button-${index}`}
              data-pile-label={label}
            >
              {label}
            </button>
          ))}
        </div>
      </>
    )
  }

  // Multi-card: show cards with pile toggle buttons
  const availableWidth = responsive.viewportWidth - responsive.containerPadding * 2 - 32
  const gap = responsive.isMobile ? 4 : 8
  const maxCardWidth = responsive.isMobile ? 90 : 130
  const cardWidth = calculateFittingCardWidth(
    decision.cards.length,
    availableWidth,
    gap,
    maxCardWidth,
    45
  )

  return (
    <>
      <h2 className={styles.title}>
        {decision.context.sourceName ?? 'Split Piles'}
      </h2>
      <p className={styles.hint}>
        {decision.prompt}
      </p>

      {/* Cards with pile assignment */}
      <div className={styles.cardContainer} style={{ gap }}>
        {decision.cards.map((cardId) => {
          const cardInfoFromDecision = decision.cardInfo?.[cardId]
          const cardFromState = gameState?.cards[cardId]
          const cardName = cardInfoFromDecision?.name || cardFromState?.name || 'Unknown Card'
          const imageUri = cardInfoFromDecision?.imageUri || cardFromState?.imageUri
          const currentPile = assignments[cardId] ?? 0
          const cardImageUrl = getCardImageUrl(cardName, imageUri)
          const cardHeight = Math.round(cardWidth * 1.4)

          return (
            <div key={cardId} style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 4 }}>
              <div
                style={{
                  width: cardWidth,
                  height: cardHeight,
                  borderRadius: 'var(--radius-md)',
                  overflow: 'hidden',
                  border: '2px solid var(--border-card)',
                  position: 'relative',
                  cursor: 'pointer',
                }}
                onClick={() => {
                  // Cycle to next pile
                  const nextPile = (currentPile + 1) % decision.numberOfPiles
                  handleAssignToPile(cardId, nextPile)
                }}
              >
                <img
                  src={cardImageUrl}
                  alt={cardName}
                  className={styles.cardImage}
                  onError={(e) => {
                    e.currentTarget.style.display = 'none'
                    const fallback = e.currentTarget.nextElementSibling as HTMLElement
                    if (fallback) fallback.style.display = 'flex'
                  }}
                />
                <div className={styles.cardFallback}>
                  <span className={styles.cardFallbackName}>{cardName}</span>
                </div>
              </div>
              {/* Pile assignment buttons */}
              <div style={{ display: 'flex', gap: 2 }}>
                {labels.map((label, index) => (
                  <button
                    key={index}
                    onClick={() => handleAssignToPile(cardId, index)}
                    data-testid={`pile-button-${index}`}
                    data-pile-label={label}
                    style={{
                      fontSize: 'var(--font-xs)',
                      padding: '2px 6px',
                      borderRadius: 'var(--radius-sm)',
                      border: 'none',
                      cursor: 'pointer',
                      backgroundColor: currentPile === index ? 'var(--accent-gold)' : 'var(--bg-tertiary)',
                      color: currentPile === index ? 'var(--text-dark)' : 'var(--text-secondary)',
                      fontWeight: currentPile === index ? 'var(--font-weight-bold)' : 'var(--font-weight-normal)',
                    }}
                  >
                    {label}
                  </button>
                ))}
              </div>
            </div>
          )
        })}
      </div>

      {/* Confirm button */}
      <button onClick={() => handleSubmit()} className={styles.confirmButton}>
        Confirm
      </button>
    </>
  )
}

/**
 * Mana source selection UI for SelectManaSourcesDecision.
 * Shows a side banner and allows clicking lands/sources on the battlefield,
 * with an "Auto Pay" shortcut button.
 */
function ManaSourceSelectionUI({
  decision,
}: {
  decision: SelectManaSourcesDecision
}) {
  const startDecisionSelection = useGameStore((s) => s.startDecisionSelection)
  const decisionSelectionState = useGameStore((s) => s.decisionSelectionState)
  const cancelDecisionSelection = useGameStore((s) => s.cancelDecisionSelection)
  const submitManaSourcesDecision = useGameStore((s) => s.submitManaSourcesDecision)

  // Start decision selection state when this component mounts
  useEffect(() => {
    const selectionState: DecisionSelectionState = {
      decisionId: decision.id,
      validOptions: decision.availableSources.map((s) => s.entityId),
      selectedOptions: [...decision.autoPaySuggestion],
      minSelections: 1,
      maxSelections: decision.availableSources.length,
      prompt: decision.prompt,
    }
    startDecisionSelection(selectionState)

    return () => {
      cancelDecisionSelection()
    }
  }, [decision.id])

  const selectedCount = decisionSelectionState?.selectedOptions.length ?? 0

  const handleAutoPay = () => {
    submitManaSourcesDecision([], true)
    cancelDecisionSelection()
  }

  const handleConfirm = () => {
    if (decisionSelectionState && selectedCount > 0) {
      submitManaSourcesDecision(decisionSelectionState.selectedOptions, false)
      cancelDecisionSelection()
    }
  }

  const handleDecline = () => {
    submitManaSourcesDecision([], false)
    cancelDecisionSelection()
  }

  return (
    <div className={styles.sideBannerSelection}>
      <div className={styles.bannerTitleSelection}>
        {decision.canDecline ? 'Activate Ability?' : 'Select Mana Sources'}
      </div>
      {decision.context.sourceName && (
        <div className={styles.hint}>
          <AbilityText text={decision.prompt} size={13} />
        </div>
      )}
      <div className={styles.hint}>
        {selectedCount > 0
          ? `${selectedCount} source${selectedCount !== 1 ? 's' : ''} selected`
          : 'Click lands to select'}
      </div>

      <div className={styles.buttonContainerSmall}>
        <button onClick={handleAutoPay} className={`${styles.confirmButton} ${styles.confirmButtonSmall}`}>
          Auto Pay
        </button>
        {selectedCount > 0 && (
          <button
            onClick={handleConfirm}
            className={`${styles.confirmButton} ${styles.confirmButtonSmall}`}
          >
            Confirm ({selectedCount})
          </button>
        )}
        {decision.canDecline && (
          <button
            onClick={handleDecline}
            className={`${styles.confirmButton} ${styles.confirmButtonSmall}`}
          >
            Decline
          </button>
        )}
      </div>
    </div>
  )
}

