import { useState, useMemo, useEffect } from 'react'
import { useGameStore } from '../../store/gameStore'
import type { DecisionSelectionState } from '../../store/gameStore'
import type { EntityId, SelectCardsDecision, ChooseTargetsDecision, YesNoDecision, ChooseNumberDecision, ChooseOptionDecision, ChooseColorDecision, ClientCard, ClientGameState } from '../../types'
import { ColorDisplayNames } from '../../types'
import { ZoneType } from '../../types'
import { useResponsive, calculateFittingCardWidth, type ResponsiveSizes } from '../../hooks/useResponsive'
import { LibrarySearchUI } from './LibrarySearchUI'
import { ReorderCardsUI } from './ReorderCardsUI'
import { OrderBlockersUI } from './OrderBlockersUI'
import { ZoneSelectionUI, type ZoneCardInfo } from './ZoneSelectionUI'
import { getCardImageUrl } from '../../utils/cardImages'
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
      <div className={styles.overlay}>
        <CardSelectionDecision decision={pendingDecision} responsive={responsive} />
      </div>
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

    // Otherwise show the default banner for board targeting
    // Positioned on the right side (like CombatOverlay) so it doesn't cover targets
    return (
      <div className={styles.sideBannerTarget}>
        <div className={styles.bannerTitle}>
          Choose Target
        </div>
        <div className={styles.prompt}>
          {pendingDecision.prompt}
        </div>
        <div className={styles.hint}>
          {isPlayerOnly
            ? "Click a player's life total"
            : 'Click a valid target'}
        </div>
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
        Select Cards
      </div>
      <div className={styles.prompt}>
        {decision.prompt}
      </div>
      <div className={styles.hint}>
        {selectedCount} / {maxSelections} selected
        {minSelections > 0 && ` (min ${minSelections})`}
      </div>
      <div className={styles.hintSmall}>
        Click valid cards to select
      </div>

      {/* Confirm/Skip buttons */}
      <div className={styles.buttonContainerSmall}>
        {canSkip && (
          <button onClick={handleSkip} className={`${styles.skipButton}`}>
            Skip
          </button>
        )}
        <button
          onClick={handleConfirm}
          disabled={!canConfirm}
          className={`${styles.confirmButton} ${styles.confirmButtonSmall}`}
        >
          Confirm
        </button>
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
        {decision.prompt}
      </h2>

      {decision.context.sourceName && (
        <p className={styles.subtitle}>
          {decision.context.sourceName}
        </p>
      )}

      {/* Yes/No buttons */}
      <div className={styles.buttonContainer}>
        <button onClick={handleYes} className={styles.yesButton}>
          {decision.yesText}
        </button>
        <button onClick={handleNo} className={styles.noButton}>
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
  const submitOptionDecision = useGameStore((s) => s.submitOptionDecision)
  const gameState = useGameStore((s) => s.gameState)
  const responsive = useResponsive()

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
      <h2 className={styles.title}>
        {decision.prompt}
      </h2>

      {decision.context.sourceName && (
        <p className={styles.subtitle}>
          {decision.context.sourceName}
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

      {/* Hover-to-zoom preview */}
      {hoveredPreviewCard && !responsive.isMobile && (
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
      <h2 className={styles.title}>
        {decision.prompt}
      </h2>

      <p className={styles.hint}>
        {decision.minSelections === 0
          ? `Select up to ${decision.maxSelections}`
          : `Selected: ${selectedCards.length} / ${decision.minSelections}${decision.minSelections !== decision.maxSelections ? ` - ${decision.maxSelections}` : ''}`
        }
      </p>

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

      {/* Confirm button */}
      <button
        onClick={handleConfirm}
        disabled={!canConfirm}
        className={styles.confirmButton}
      >
        {decision.minSelections === 0 && selectedCards.length === 0
          ? 'Select None'
          : 'Confirm Selection'}
      </button>

      {/* Card preview on hover */}
      {hoveredCardInfo?.name && !responsive.isMobile && (
        <DecisionCardPreview cardName={hoveredCardInfo.name} imageUri={hoveredCardInfo.imageUri} />
      )}
    </>
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
          alt={cardName}
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
