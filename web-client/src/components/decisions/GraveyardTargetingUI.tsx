import { useState, useMemo } from 'react'
import { useGameStore } from '@/store/gameStore.ts'
import { ZoneType } from '@/types'
import type { EntityId, ChooseTargetsDecision, ClientCard } from '@/types'
import { calculateFittingCardWidth, type ResponsiveSizes } from '@/hooks/useResponsive.ts'
import { ZoneSelectionUI, type ZoneCardInfo } from './ZoneSelectionUI'
import { getCardImageUrl } from '@/utils/cardImages.ts'
import { derivePileAction } from '@/utils/targeting.ts'
import styles from './DecisionUI.module.css'

/**
 * Graveyard / exile pile targeting UI for **one** requirement of a ChooseTargetsDecision — uses the
 * shared ZoneSelectionUI component. Supports selecting from multiple piles with owner tabs. Labels
 * adapt to the cards' actual zone (Graveyard vs Exile) so the same component renders Blade of the
 * Swarm's exile-targeting mode and the existing graveyard-target spells with the right wording.
 *
 * The picked cards go back to the parent [ChooseTargetsUI] rather than straight to the server: the
 * pile slot may be one of several requirements (The Spot, Living Portal exiles a battlefield
 * permanent *and* a graveyard card), and only the parent knows whether more slots follow.
 */
export function GraveyardTargetingUI({
  decision,
  graveyardCards,
  responsive,
  requirementIndex,
  totalRequirements,
  initialSelection,
  onComplete,
  onBack,
  onViewBattlefield,
}: {
  decision: ChooseTargetsDecision
  graveyardCards: ClientCard[]
  responsive: ResponsiveSizes
  requirementIndex: number
  totalRequirements: number
  /**
   * Picks to pre-select — non-empty when the player stepped Back into this requirement, or came
   * here from the board on a mixed requirement carrying the permanents they picked there. Carried
   * board picks are counted (they fill this requirement's slots) but aren't shown in the ribbon;
   * they live on the battlefield, which is one click away on "View Battlefield".
   */
  initialSelection: readonly EntityId[]
  onComplete: (targets: readonly EntityId[]) => void
  /** Present when an earlier requirement can be revised. */
  onBack?: () => void
  /**
   * Present on a mixed `battlefield ∪ pile` requirement, where the board half is collected by the
   * banner this picker was opened from. "View Battlefield" then hands control (and the picks made
   * here) back to that banner instead of minimising into a floating re-open button — the banner is
   * where Confirm lives for this requirement.
   */
  onViewBattlefield?: (carried: readonly EntityId[]) => void
}) {
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
      manaValue: card.manaValue,
    }))
  }, [currentCards])

  // The list is homogenous-zone by construction (the caller's filter routes mixed-zone
  // sets elsewhere), so we read the first card's zone to pick wording for everything.
  const pileZoneType = graveyardCards[0]?.zone?.zoneType ?? ZoneType.GRAVEYARD
  const pileNoun = pileZoneType === ZoneType.EXILE ? 'Exile' : 'Graveyard'

  // Get player names for tabs
  const getPlayerLabel = (ownerId: EntityId): string => {
    if (ownerId === viewingPlayerId) return `Your ${pileNoun}`
    const player = gameState?.players.find((p) => p.playerId === ownerId)
    return player ? `${player.name}'s ${pileNoun}` : `Opponent's ${pileNoun}`
  }

  const handleConfirm = (selectedCards: EntityId[]) => {
    // Hand this requirement's picks to the walker, which advances or submits.
    // If minTargets is 0 (optional ability), confirming 0 cards declines this slot.
    onComplete(selectedCards)
  }

  // Get target requirements
  const targetReq = decision.targetRequirements[requirementIndex]
  const minTargets = targetReq?.minTargets ?? 1
  const maxTargets = targetReq?.maxTargets ?? 1
  const isOptionalTarget = minTargets === 0
  const isMultiTarget = maxTargets > 1
  const sourceName = decision.context.sourceName ?? 'this ability'
  const baseTitle = isOptionalTarget ? `Resolve ${sourceName}` : `Choose from ${pileNoun}`
  const title = totalRequirements > 1
    ? `${baseTitle} (${requirementIndex + 1}/${totalRequirements})`
    : baseTitle

  // Derive the action wording from effectHint so the button/text matches the actual effect.
  //
  // TODO(per-requirement action hints): effectHint describes the effect as a whole, while this UI
  // now collects one requirement of it. Both of The Spot's slots exile, so a single verb still
  // fits; a composite whose slots take different verbs ("destroy target creature and return target
  // card from a graveyard to your hand") would mislabel this slot. The durable fix is an action
  // hint on TargetRequirementInfo instead of sniffing prose — a server-side change.
  //
  // Playtesting proved the sharper half of this: prose sniffing doesn't merely go vague, it goes
  // *wrong*. Taskmaster, Mercenary Mimic ("becomes a copy of … creature card in a graveyard")
  // matched no known verb and inherited the old "Return to Hand" fallback, so the picker promised a
  // card would come back to hand while the effect only copied it and left it in the graveyard. The
  // fallback is neutral now and the copy shape has its own branch, but every unlisted effect is
  // still one unlucky substring away from the same class of lie. A server-supplied action verb per
  // requirement removes the guesswork rather than lengthening the keyword list.
  const { confirmText: optionalConfirmText, verb: actionVerb } =
    derivePileAction(decision.context.effectHint)

  const numWord = (n: number): string =>
    ({ 1: 'one', 2: 'two', 3: 'three', 4: 'four', 5: 'five' } as Record<number, string>)[n] ?? String(n)
  const cardNoun = isMultiTarget ? 'cards' : 'a card'
  const countPhrase =
    minTargets === maxTargets
      ? isMultiTarget
        ? `${numWord(maxTargets)} ${cardNoun}`
        : cardNoun
      : minTargets === 0
        ? isMultiTarget
          ? `up to ${numWord(maxTargets)} ${cardNoun}`
          : 'up to one card'
        : `${numWord(minTargets)} to ${numWord(maxTargets)} cards`

  const helperText = isOptionalTarget
    ? `Optional: choose ${countPhrase} to ${actionVerb}, or decline.`
    : `Choose ${countPhrase} to ${actionVerb}.`

  // Lift selection state to persist across tab switches. Seeded from initialSelection so stepping
  // Back into this requirement keeps its confirmed picks — the parent remounts on requirement
  // change (key={currentReqIndex}), so the initializer re-runs per slot.
  const [selectedCards, setSelectedCards] = useState<EntityId[]>(() => [...initialSelection])
  const [minimized, setMinimized] = useState(false)

  // If only one graveyard, use simple UI
  if (ownerIds.length <= 1) {
    return (
      <ZoneSelectionUI
        title={title}
        prompt={decision.prompt}
        helperText={helperText}
        cards={cards}
        minSelections={minTargets}
        maxSelections={maxTargets}
        initialSelection={initialSelection}
        responsive={responsive}
        onConfirm={handleConfirm}
        confirmText={isOptionalTarget ? optionalConfirmText : 'Confirm Target'}
        showFailToFind={isOptionalTarget}
        failToFindText="Decline Trigger"
        confirmRequiresSelection={isOptionalTarget}
        sortByType={true}
        useGlobalHover={true}
        {...(onViewBattlefield ? { onViewBattlefield } : {})}
        // The secondary button is "← Back" mid-walk (revise an earlier requirement) and
        // "Cancel" otherwise; a cancellable cast can still be cancelled from requirement 0.
        {...(onBack
          ? { onCancel: onBack, cancelText: '← Back' }
          : decision.canCancel
            ? { onCancel: () => submitCancelDecision() }
            : {})}
      />
    )
  }

  // When minimized, show floating button to restore
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

  // Multiple graveyards - show tabs
  return (
    <div className={styles.overlayDarker}>
      {/* Header */}
      <div className={styles.header}>
        <h2 className={styles.title}>
          {title}
        </h2>
        <p className={styles.headerSubtitle}>
          {decision.prompt}
        </p>
        <p className={styles.decisionHelperText}>
          {helperText}
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
        totalManaValueAtMost={targetReq?.totalManaValueAtMost ?? null}
        responsive={responsive}
        onConfirm={handleConfirm}
        onMinimize={() => setMinimized(true)}
        {...(onViewBattlefield ? { onViewBattlefield } : {})}
        confirmText={isOptionalTarget ? optionalConfirmText : 'Confirm Target'}
        declineText={isOptionalTarget ? 'Decline Trigger' : undefined}
        confirmRequiresSelection={isOptionalTarget}
        {...(onBack ? { onBack } : {})}
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
  totalManaValueAtMost,
  responsive,
  onConfirm,
  onMinimize,
  onViewBattlefield,
  confirmText,
  declineText,
  confirmRequiresSelection,
  onBack,
}: {
  cards: ZoneCardInfo[]
  selectedCards: EntityId[]
  onSelectedCardsChange: (cards: EntityId[]) => void
  minSelections: number
  maxSelections: number
  totalManaValueAtMost?: number | null
  responsive: ResponsiveSizes
  onConfirm: (selectedCards: EntityId[]) => void
  onMinimize: () => void
  /** Overrides [onMinimize] on a mixed requirement — see GraveyardTargetingUI's prop of this name. */
  onViewBattlefield?: (carried: readonly EntityId[]) => void
  confirmText: string
  declineText?: string | undefined
  confirmRequiresSelection: boolean
  onBack?: () => void
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

  const canConfirm = selectedCards.length >= minSelections &&
    selectedCards.length <= maxSelections &&
    (!confirmRequiresSelection || selectedCards.length > 0)

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

  // Running "total mana value ≤ cap" limit (Fire Lord Sozin). Selecting a card that would push the
  // summed mana value over the cap is blocked, mirroring the server-side aggregate target rule.
  const manaValueOf = (id: EntityId) => cards.find((c) => c.id === id)?.manaValue ?? 0
  const selectedManaValue = selectedCards.reduce((sum, id) => sum + manaValueOf(id), 0)

  const toggleCard = (cardId: EntityId) => {
    if (selectedCards.includes(cardId)) {
      onSelectedCardsChange(selectedCards.filter((id) => id !== cardId))
      return
    }
    if (selectedCards.length >= maxSelections) {
      // Single-select slot at its cap: clicking a different card replaces the pick, the same rule
      // the board path uses (toggleDecisionSelection). It also unblocks a mixed
      // `battlefield ∪ pile` requirement — the pick carried in from the board fills the only slot
      // and isn't in this ribbon, so without replacement no graveyard card could ever be clicked.
      if (maxSelections === 1) {
        if (
          totalManaValueAtMost != null &&
          manaValueOf(cardId) > totalManaValueAtMost
        ) {
          return
        }
        onSelectedCardsChange([cardId])
      }
      return
    }
    if (
      totalManaValueAtMost != null &&
      selectedManaValue + manaValueOf(cardId) > totalManaValueAtMost
    ) {
      return
    }
    onSelectedCardsChange([...selectedCards, cardId])
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
        {totalManaValueAtMost != null && (
          <span>
            {' · '}Total mana value:{' '}
            <span className={styles.selectionCount}>{selectedManaValue}</span>
            {' / '}
            {totalManaValueAtMost}
          </span>
        )}
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

      {/* Action buttons */}
      <div className={styles.optionButtonRow}>
        {onBack && (
          <button
            onClick={onBack}
            className={styles.viewBattlefieldButton}
          >
            ← Back
          </button>
        )}
        <button
          onClick={() => (onViewBattlefield ? onViewBattlefield(selectedCards) : onMinimize())}
          className={styles.viewBattlefieldButton}
        >
          View Battlefield
        </button>
        <button
          onClick={handleConfirmClick}
          disabled={!canConfirm}
          className={styles.confirmButton}
        >
          {selectedCards.length === 0 && minSelections === 0 && !confirmRequiresSelection ? 'Decline' : confirmText}
        </button>
        {declineText && (
          <button
            onClick={() => onConfirm([])}
            className={styles.noButton}
          >
            {declineText}
          </button>
        )}
      </div>
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
