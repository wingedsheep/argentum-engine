import { useState, useMemo } from 'react'
import { useGameStore } from '../../store/gameStore'
import type { EntityId, SelectCardsDecision, ChooseTargetsDecision, YesNoDecision, ChooseNumberDecision, ClientCard, ClientGameState } from '../../types'
import { ZoneType } from '../../types'
import { useResponsive, calculateFittingCardWidth, type ResponsiveSizes } from '../../hooks/useResponsive'
import { LibrarySearchUI } from './LibrarySearchUI'
import { ReorderCardsUI } from './ReorderCardsUI'
import { OrderBlockersUI } from './OrderBlockersUI'
import { ZoneSelectionUI, type ZoneCardInfo } from './ZoneSelectionUI'
import { DistributeDecisionUI } from './DistributeDecisionUI'
import { getCardImageUrl } from '../../utils/cardImages'

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
        <YesNoDecisionUI decision={pendingDecision} responsive={responsive} />
      </div>
    )
  }

  // Handle ChooseNumberDecision (e.g., "Choose how many cards to draw")
  if (pendingDecision.type === 'ChooseNumberDecision') {
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
        <ChooseNumberDecisionUI decision={pendingDecision} responsive={responsive} />
      </div>
    )
  }

  // Handle DistributeDecision (e.g., "Divide 4 damage among targets")
  if (pendingDecision.type === 'DistributeDecision') {
    return <DistributeDecisionUI decision={pendingDecision} responsive={responsive} />
  }

  // Handle SelectCardsDecision
  if (pendingDecision.type === 'SelectCardsDecision') {
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
      <div
        style={{
          position: 'fixed',
          top: '50%',
          right: responsive.isMobile ? 8 : 16,
          transform: 'translateY(-50%)',
          backgroundColor: 'rgba(0, 0, 0, 0.95)',
          padding: responsive.isMobile ? '12px 16px' : '14px 20px',
          borderRadius: 10,
          border: '2px solid #ff4444',
          boxShadow: '0 4px 24px rgba(255, 68, 68, 0.4)',
          zIndex: 1001,
          textAlign: 'center',
          pointerEvents: 'none',
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          gap: 8,
        }}
      >
        <div
          style={{
            color: '#ff4444',
            fontSize: responsive.fontSize.normal,
            fontWeight: 700,
            textTransform: 'uppercase',
            letterSpacing: 1,
          }}
        >
          Choose Target
        </div>
        <div style={{ color: 'white', fontSize: responsive.fontSize.small, maxWidth: 180 }}>
          {pendingDecision.prompt}
        </div>
        <div style={{ color: '#888', fontSize: responsive.fontSize.small }}>
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
 * Yes/No decision - make a binary choice.
 */
function YesNoDecisionUI({
  decision,
  responsive,
}: {
  decision: YesNoDecision
  responsive: ResponsiveSizes
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
      <h2
        style={{
          color: 'white',
          margin: 0,
          fontSize: responsive.isMobile ? 18 : 24,
          textAlign: 'center',
        }}
      >
        {decision.prompt}
      </h2>

      {decision.context.sourceName && (
        <p style={{ color: '#aaa', margin: 0, fontSize: responsive.fontSize.normal }}>
          {decision.context.sourceName}
        </p>
      )}

      {/* Yes/No buttons */}
      <div
        style={{
          display: 'flex',
          gap: responsive.isMobile ? 16 : 24,
          marginTop: responsive.isMobile ? 16 : 24,
        }}
      >
        <button
          onClick={handleYes}
          style={{
            padding: responsive.isMobile ? '12px 32px' : '16px 48px',
            fontSize: responsive.fontSize.large,
            backgroundColor: '#00aa00',
            color: 'white',
            border: 'none',
            borderRadius: 8,
            cursor: 'pointer',
            fontWeight: 600,
            minWidth: responsive.isMobile ? 100 : 120,
          }}
        >
          {decision.yesText}
        </button>
        <button
          onClick={handleNo}
          style={{
            padding: responsive.isMobile ? '12px 32px' : '16px 48px',
            fontSize: responsive.fontSize.large,
            backgroundColor: '#666',
            color: 'white',
            border: 'none',
            borderRadius: 8,
            cursor: 'pointer',
            fontWeight: 600,
            minWidth: responsive.isMobile ? 100 : 120,
          }}
        >
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
  responsive,
}: {
  decision: ChooseNumberDecision
  responsive: ResponsiveSizes
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
      <h2
        style={{
          color: 'white',
          margin: 0,
          fontSize: responsive.isMobile ? 18 : 24,
          textAlign: 'center',
        }}
      >
        {decision.prompt}
      </h2>

      {decision.context.sourceName && (
        <p style={{ color: '#aaa', margin: 0, fontSize: responsive.fontSize.normal }}>
          {decision.context.sourceName}
        </p>
      )}

      {/* Number selection buttons */}
      <div
        style={{
          display: 'flex',
          gap: responsive.isMobile ? 8 : 12,
          marginTop: responsive.isMobile ? 16 : 24,
          flexWrap: 'wrap',
          justifyContent: 'center',
        }}
      >
        {numbers.map((num) => (
          <button
            key={num}
            onClick={() => setSelectedNumber(num)}
            style={{
              padding: responsive.isMobile ? '12px 20px' : '16px 28px',
              fontSize: responsive.fontSize.large,
              backgroundColor: selectedNumber === num ? '#0066cc' : '#444',
              color: 'white',
              border: selectedNumber === num ? '3px solid #66aaff' : '2px solid #666',
              borderRadius: 8,
              cursor: 'pointer',
              fontWeight: 600,
              minWidth: responsive.isMobile ? 50 : 60,
              transition: 'all 0.15s',
            }}
          >
            {num}
          </button>
        ))}
      </div>

      {/* Confirm button */}
      <button
        onClick={handleConfirm}
        style={{
          padding: responsive.isMobile ? '12px 32px' : '16px 48px',
          fontSize: responsive.fontSize.large,
          backgroundColor: '#00aa00',
          color: 'white',
          border: 'none',
          borderRadius: 8,
          cursor: 'pointer',
          fontWeight: 600,
          marginTop: responsive.isMobile ? 16 : 24,
        }}
      >
        Confirm
      </button>
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
      <h2
        style={{
          color: 'white',
          margin: 0,
          fontSize: responsive.isMobile ? 18 : 24,
          textAlign: 'center',
        }}
      >
        {decision.prompt}
      </h2>

      <p style={{ color: '#888', margin: 0, fontSize: responsive.fontSize.normal }}>
        Selected: {selectedCards.length} / {decision.minSelections}
        {decision.minSelections !== decision.maxSelections &&
          ` - ${decision.maxSelections}`}
      </p>

      {/* Card options */}
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
              isMobile={responsive.isMobile}
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
    if (selectedCards.length > 0) {
      // Submit with the selected targets for target index 0
      submitTargetsDecision({ 0: selectedCards })
    }
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
    <div
      style={{
        position: 'fixed',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        backgroundColor: 'rgba(0, 0, 0, 0.92)',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        gap: responsive.isMobile ? 12 : 20,
        padding: responsive.containerPadding,
        pointerEvents: 'auto',
        zIndex: 1000,
      }}
    >
      {/* Header */}
      <div style={{ textAlign: 'center' }}>
        <h2
          style={{
            color: 'white',
            margin: 0,
            fontSize: responsive.isMobile ? 20 : 28,
            fontWeight: 600,
          }}
        >
          Choose from Graveyard
        </h2>
        <p
          style={{
            color: '#aaa',
            margin: '8px 0 0',
            fontSize: responsive.fontSize.normal,
          }}
        >
          {decision.prompt}
        </p>
      </div>

      {/* Graveyard tabs */}
      <div
        style={{
          display: 'flex',
          gap: responsive.isMobile ? 8 : 12,
          backgroundColor: 'rgba(0, 0, 0, 0.4)',
          padding: 4,
          borderRadius: 8,
        }}
      >
        {ownerIds.map((ownerId) => {
          const isActive = ownerId === currentOwnerId
          const ownerCards = cardsByOwner.get(ownerId) ?? []
          const cardCount = ownerCards.length
          // Count how many cards are selected from this graveyard
          const selectedFromThisGraveyard = ownerCards.filter((c) =>
            selectedCards.includes(c.id)
          ).length
          return (
            <button
              key={ownerId}
              onClick={() => setSelectedOwnerId(ownerId)}
              style={{
                padding: responsive.isMobile ? '8px 16px' : '10px 24px',
                fontSize: responsive.fontSize.normal,
                backgroundColor: isActive ? '#4a5568' : 'transparent',
                color: isActive ? 'white' : '#888',
                border: selectedFromThisGraveyard > 0 && !isActive ? '2px solid #fbbf24' : 'none',
                borderRadius: 6,
                cursor: 'pointer',
                fontWeight: isActive ? 600 : 400,
                transition: 'all 0.15s',
                position: 'relative',
              }}
            >
              {getPlayerLabel(ownerId)} ({cardCount})
              {selectedFromThisGraveyard > 0 && (
                <span
                  style={{
                    position: 'absolute',
                    top: -6,
                    right: -6,
                    backgroundColor: '#fbbf24',
                    color: '#1a1a1a',
                    borderRadius: '50%',
                    width: 20,
                    height: 20,
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    fontSize: 12,
                    fontWeight: 'bold',
                  }}
                >
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

  return (
    <>
      {/* Selection counter */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: 16,
          color: '#888',
          fontSize: responsive.fontSize.normal,
        }}
      >
        <span>
          Selected:{' '}
          <span
            style={{
              color: canConfirm ? '#4ade80' : selectedCards.length > 0 ? '#fbbf24' : '#888',
              fontWeight: 600,
            }}
          >
            {selectedCards.length}
          </span>
          {' / '}
          {maxSelections}
        </span>
      </div>

      {/* Card ribbon */}
      <div
        style={{
          display: 'flex',
          gap,
          padding: responsive.isMobile ? 12 : 24,
          justifyContent: sortedCards.length <= 6 ? 'center' : 'flex-start',
          overflowX: 'auto',
          maxWidth: '100%',
          scrollBehavior: 'smooth',
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
            isMobile={responsive.isMobile}
            onMouseEnter={() => handleMouseEnter(card.id)}
            onMouseLeave={handleMouseLeave}
          />
        ))}
      </div>

      {/* No cards message */}
      {sortedCards.length === 0 && (
        <p style={{ color: '#666', fontSize: responsive.fontSize.normal }}>
          No valid targets in this graveyard.
        </p>
      )}

      {/* Confirm button */}
      <button
        onClick={handleConfirmClick}
        disabled={!canConfirm}
        style={{
          padding: responsive.isMobile ? '10px 24px' : '12px 36px',
          fontSize: responsive.fontSize.large,
          backgroundColor: canConfirm ? '#16a34a' : '#333',
          color: canConfirm ? 'white' : '#666',
          border: 'none',
          borderRadius: 8,
          cursor: canConfirm ? 'pointer' : 'not-allowed',
          fontWeight: 600,
          transition: 'all 0.15s',
        }}
      >
        {selectedCards.length === 0 && minSelections === 0 ? 'Skip' : 'Confirm Target'}
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
  isMobile,
  onMouseEnter,
  onMouseLeave,
}: {
  card: ZoneCardInfo
  isSelected: boolean
  isHovered: boolean
  onClick: () => void
  cardWidth: number
  isMobile: boolean
  onMouseEnter?: () => void
  onMouseLeave?: () => void
}) {
  const cardImageUrl = getCardImageUrl(card.name, card.imageUri)
  const cardHeight = Math.round(cardWidth * 1.4)
  const showHoverEffect = isHovered && !isSelected

  return (
    <div
      onClick={onClick}
      onMouseEnter={onMouseEnter}
      onMouseLeave={onMouseLeave}
      style={{
        width: cardWidth,
        height: cardHeight,
        backgroundColor: isSelected ? '#1a3320' : '#1a1a1a',
        border: isSelected
          ? '3px solid #fbbf24'
          : showHoverEffect
            ? '2px solid #666'
            : '2px solid #333',
        borderRadius: isMobile ? 6 : 10,
        display: 'flex',
        flexDirection: 'column',
        overflow: 'hidden',
        cursor: 'pointer',
        transition: 'all 0.2s ease-out',
        transform: isSelected
          ? 'translateY(-12px) scale(1.05)'
          : showHoverEffect
            ? 'translateY(-4px) scale(1.02)'
            : 'none',
        boxShadow: isSelected
          ? '0 12px 28px rgba(251, 191, 36, 0.4), 0 0 20px rgba(251, 191, 36, 0.2)'
          : showHoverEffect
            ? '0 8px 20px rgba(255, 255, 255, 0.15)'
            : '0 4px 12px rgba(0, 0, 0, 0.6)',
        flexShrink: 0,
        position: 'relative',
      }}
    >
      <img
        src={cardImageUrl}
        alt={card.name}
        style={{ width: '100%', height: '100%', objectFit: 'cover' }}
        onError={(e) => {
          e.currentTarget.style.display = 'none'
          const fallback = e.currentTarget.nextElementSibling as HTMLElement
          if (fallback) fallback.style.display = 'flex'
        }}
      />
      <div
        style={{
          position: 'absolute',
          inset: 0,
          backgroundColor: '#1a1a1a',
          display: 'none',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          padding: isMobile ? '6px' : '10px',
          gap: 4,
        }}
      >
        <span style={{ color: 'white', fontSize: isMobile ? 10 : 12, fontWeight: 600, textAlign: 'center' }}>
          {card.name}
        </span>
      </div>
      {isSelected && (
        <div
          style={{
            position: 'absolute',
            top: 6,
            right: 6,
            width: 24,
            height: 24,
            backgroundColor: '#fbbf24',
            borderRadius: '50%',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: '#1a1a1a',
            fontWeight: 'bold',
            fontSize: 14,
            boxShadow: '0 2px 8px rgba(0, 0, 0, 0.4)',
          }}
        >
          &#10003;
        </div>
      )}
    </div>
  )
}

/**
 * Card preview for graveyard selection.
 */

/**
 * Card preview overlay - shows enlarged card when hovering.
 */
function DecisionCardPreview({ cardName, imageUri }: { cardName: string; imageUri?: string | null | undefined }) {
  const cardImageUrl = getCardImageUrl(cardName, imageUri, 'large')

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
          alt={cardName}
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
  isMobile = false,
  onMouseEnter,
  onMouseLeave,
}: {
  cardId: EntityId
  cardName: string
  imageUri?: string | null | undefined
  isSelected: boolean
  onClick: () => void
  cardWidth?: number
  isMobile?: boolean
  onMouseEnter?: () => void
  onMouseLeave?: () => void
}) {
  const cardImageUrl = getCardImageUrl(cardName, imageUri)

  const cardRatio = 1.4
  const cardHeight = Math.round(cardWidth * cardRatio)

  return (
    <div
      onClick={onClick}
      onMouseEnter={onMouseEnter}
      onMouseLeave={onMouseLeave}
      style={{
        width: cardWidth,
        height: cardHeight,
        backgroundColor: isSelected ? '#330000' : '#1a1a1a',
        border: isSelected ? '3px solid #ff4444' : '2px solid #444',
        borderRadius: isMobile ? 6 : 10,
        display: 'flex',
        flexDirection: 'column',
        overflow: 'hidden',
        cursor: 'pointer',
        transition: 'all 0.15s',
        transform: isSelected ? 'translateY(-8px) scale(1.05)' : 'none',
        boxShadow: isSelected
          ? '0 8px 20px rgba(255, 68, 68, 0.3)'
          : '0 4px 8px rgba(0, 0, 0, 0.5)',
        flexShrink: 0,
        position: 'relative',
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
          e.currentTarget.style.display = 'none'
          const fallback = e.currentTarget.nextElementSibling as HTMLElement
          if (fallback) fallback.style.display = 'flex'
        }}
      />

      {/* Fallback when image fails */}
      <div
        style={{
          position: 'absolute',
          inset: 0,
          backgroundColor: '#1a1a1a',
          display: 'none',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          padding: isMobile ? '4px' : '8px',
        }}
      >
        <span
          style={{
            color: 'white',
            fontSize: isMobile ? 9 : 11,
            fontWeight: 500,
            textAlign: 'center',
          }}
        >
          {cardName}
        </span>
      </div>

      {/* Selection indicator */}
      {isSelected && (
        <div
          style={{
            position: 'absolute',
            top: 4,
            right: 4,
            width: 20,
            height: 20,
            backgroundColor: '#ff4444',
            borderRadius: '50%',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: 'white',
            fontWeight: 'bold',
            fontSize: 12,
          }}
        >
          ✓
        </div>
      )}
    </div>
  )
}
