import { useState } from 'react'
import { useGameStore } from '../../store/gameStore'
import type { EntityId, SelectCardsDecision, ChooseTargetsDecision, YesNoDecision } from '../../types'
import { useResponsive, calculateFittingCardWidth, type ResponsiveSizes } from '../../hooks/useResponsive'
import { LibrarySearchUI } from './LibrarySearchUI'
import { ReorderCardsUI } from './ReorderCardsUI'
import { OrderBlockersUI } from './OrderBlockersUI'

/**
 * Check if all legal targets in a ChooseTargetsDecision are players.
 */
function isPlayerOnlyTargeting(decision: ChooseTargetsDecision, playerIds: EntityId[]): boolean {
  const legalTargets = decision.legalTargets[0] ?? []
  if (legalTargets.length === 0) return false
  return legalTargets.every((targetId) => playerIds.includes(targetId))
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

    // If all targets are players, show a simple prompt instead of overlay
    // The user can click on life totals on the game board
    if (isPlayerOnlyTargeting(pendingDecision, playerIds)) {
      return (
        <div
          style={{
            position: 'fixed',
            top: responsive.isMobile ? 60 : 80,
            left: '50%',
            transform: 'translateX(-50%)',
            backgroundColor: 'rgba(0, 0, 0, 0.9)',
            padding: responsive.isMobile ? '12px 20px' : '16px 32px',
            borderRadius: 12,
            border: '2px solid #ff4444',
            boxShadow: '0 4px 20px rgba(255, 68, 68, 0.3)',
            zIndex: 1000,
            textAlign: 'center',
            pointerEvents: 'none',
          }}
        >
          <div style={{ color: 'white', fontSize: responsive.fontSize.large, fontWeight: 600 }}>
            {pendingDecision.prompt}
          </div>
          <div style={{ color: '#aaa', fontSize: responsive.fontSize.normal, marginTop: 4 }}>
            Click a player's life total to target them
          </div>
        </div>
      )
    }

    // For non-player targets, show the full overlay
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
        <TargetSelectionDecision decision={pendingDecision} responsive={responsive} />
      </div>
    )
  }

  // Other decision types not yet implemented
  return null
}

/**
 * Target selection decision - choose targets for a spell or ability.
 */
function TargetSelectionDecision({
  decision,
  responsive,
}: {
  decision: ChooseTargetsDecision
  responsive: ResponsiveSizes
}) {
  const [selectedTargets, setSelectedTargets] = useState<EntityId[]>([])
  const [hoveredCardId, setHoveredCardId] = useState<EntityId | null>(null)
  const submitTargetsDecision = useGameStore((s) => s.submitTargetsDecision)
  const gameState = useGameStore((s) => s.gameState)

  const hoveredCard = hoveredCardId ? gameState?.cards[hoveredCardId] : null

  // For now, we only support single-target requirements (index 0)
  const targetReq = decision.targetRequirements[0]
  const legalTargets = decision.legalTargets[0] ?? []

  // Handle case where no target requirements exist
  if (!targetReq) {
    return <div style={{ color: 'white' }}>No target requirements specified</div>
  }

  const canConfirm =
    selectedTargets.length >= targetReq.minTargets &&
    selectedTargets.length <= targetReq.maxTargets

  // Calculate card size that fits all targets
  const availableWidth = responsive.viewportWidth - responsive.containerPadding * 2 - 32
  const gap = responsive.isMobile ? 4 : 8
  const maxCardWidth = responsive.isMobile ? 90 : 130
  const cardWidth = calculateFittingCardWidth(
    legalTargets.length,
    availableWidth,
    gap,
    maxCardWidth,
    45
  )

  const toggleTarget = (targetId: EntityId) => {
    setSelectedTargets((prev) => {
      if (prev.includes(targetId)) {
        return prev.filter((id) => id !== targetId)
      }
      // Don't allow selecting more than max
      if (prev.length >= targetReq.maxTargets) {
        return prev
      }
      return [...prev, targetId]
    })
  }

  const handleConfirm = () => {
    // Submit targets as a map of index -> selected targets
    submitTargetsDecision({ 0: selectedTargets })
    setSelectedTargets([])
  }

  // Get card name for display
  const getTargetName = (targetId: EntityId): string => {
    // Check if it's a card
    const card = gameState?.cards[targetId]
    if (card) return card.name

    // Check if it's a player
    const player = gameState?.players.find((p) => p.playerId === targetId)
    if (player) return player.name

    return 'Unknown'
  }

  // Check if target is a player
  const isPlayerTarget = (targetId: EntityId): boolean => {
    return gameState?.players.some((p) => p.playerId === targetId) ?? false
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

      <p style={{ color: '#888', margin: 0, fontSize: responsive.fontSize.normal }}>
        {targetReq.description} - Select {targetReq.minTargets}
        {targetReq.minTargets !== targetReq.maxTargets && ` - ${targetReq.maxTargets}`} target
        {targetReq.maxTargets !== 1 && 's'}
      </p>

      {/* Target options */}
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
        {legalTargets.map((targetId) => {
          const isPlayer = isPlayerTarget(targetId)
          return isPlayer ? (
            <PlayerTargetCard
              key={targetId}
              playerId={targetId}
              playerName={getTargetName(targetId)}
              isSelected={selectedTargets.includes(targetId)}
              onClick={() => toggleTarget(targetId)}
              cardWidth={cardWidth}
              isMobile={responsive.isMobile}
            />
          ) : (
            <DecisionCard
              key={targetId}
              cardId={targetId}
              cardName={getTargetName(targetId)}
              isSelected={selectedTargets.includes(targetId)}
              onClick={() => toggleTarget(targetId)}
              cardWidth={cardWidth}
              isMobile={responsive.isMobile}
              onMouseEnter={() => setHoveredCardId(targetId)}
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
        Confirm Target
      </button>

      {/* Card preview on hover */}
      {hoveredCard && !responsive.isMobile && (
        <DecisionCardPreview cardName={hoveredCard.name} />
      )}
    </>
  )
}

/**
 * Player target display for target selection.
 */
function PlayerTargetCard({
  playerId: _playerId,
  playerName,
  isSelected,
  onClick,
  cardWidth = 130,
  isMobile = false,
}: {
  playerId: EntityId
  playerName: string
  isSelected: boolean
  onClick: () => void
  cardWidth?: number
  isMobile?: boolean
}) {
  const cardRatio = 1.4
  const cardHeight = Math.round(cardWidth * cardRatio)

  return (
    <div
      onClick={onClick}
      style={{
        width: cardWidth,
        height: cardHeight,
        backgroundColor: isSelected ? '#330000' : '#1a1a2a',
        border: isSelected ? '3px solid #ff4444' : '2px solid #444',
        borderRadius: isMobile ? 6 : 10,
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
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
      {/* Player icon */}
      <div
        style={{
          fontSize: cardWidth * 0.4,
          marginBottom: 8,
        }}
      >
        👤
      </div>

      {/* Player name */}
      <span
        style={{
          color: 'white',
          fontSize: isMobile ? 11 : 14,
          fontWeight: 500,
          textAlign: 'center',
        }}
      >
        {playerName}
      </span>

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

  const hoveredCard = hoveredCardId ? gameState?.cards[hoveredCardId] : null

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
          const card = gameState?.cards[cardId]
          return (
            <DecisionCard
              key={cardId}
              cardId={cardId}
              cardName={card?.name || 'Unknown Card'}
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
      {hoveredCard && !responsive.isMobile && (
        <DecisionCardPreview cardName={hoveredCard.name} />
      )}
    </>
  )
}

/**
 * Card preview overlay - shows enlarged card when hovering.
 */
function DecisionCardPreview({ cardName }: { cardName: string }) {
  const cardImageUrl = `https://api.scryfall.com/cards/named?exact=${encodeURIComponent(cardName)}&format=image&version=large`

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
  isSelected,
  onClick,
  cardWidth = 130,
  isMobile = false,
  onMouseEnter,
  onMouseLeave,
}: {
  cardId: EntityId
  cardName: string
  isSelected: boolean
  onClick: () => void
  cardWidth?: number
  isMobile?: boolean
  onMouseEnter?: () => void
  onMouseLeave?: () => void
}) {
  const cardImageUrl = `https://api.scryfall.com/cards/named?exact=${encodeURIComponent(cardName)}&format=image&version=normal`

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
