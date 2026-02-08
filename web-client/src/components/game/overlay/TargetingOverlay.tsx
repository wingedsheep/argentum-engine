import React from 'react'
import { useGameStore } from '../../../store/gameStore'
import type { ClientCard, EntityId } from '../../../types'
import type { ResponsiveSizes } from '../../../hooks/useResponsive'
import { calculateFittingCardWidth } from '../../../hooks/useResponsive'
import { getCardImageUrl } from '../../../utils/cardImages'
import { useResponsiveContext, handleImageError } from '../board/shared'
import { styles } from '../board/styles'
import { TARGET_COLOR, TARGET_COLOR_BRIGHT } from '../../../styles/targetingColors'

/**
 * Graveyard targeting overlay - shows when targeting mode requires selecting cards from graveyards.
 * Similar to GraveyardTargetingUI in DecisionUI but for client-side spell casting targeting.
 */
function GraveyardTargetingOverlay({
  graveyardCards,
  targetingState,
  responsive,
  onSelect,
  onDeselect,
  onConfirm,
  onCancel,
}: {
  graveyardCards: ClientCard[]
  targetingState: { selectedTargets: readonly EntityId[]; minTargets: number; maxTargets: number; targetDescription?: string; currentRequirementIndex?: number; totalRequirements?: number }
  responsive: ResponsiveSizes
  onSelect: (cardId: EntityId) => void
  onDeselect: (cardId: EntityId) => void
  onConfirm: () => void
  onCancel: () => void
}) {
  const hoverCard = useGameStore((s) => s.hoverCard)
  const gameState = useGameStore((s) => s.gameState)
  const viewingPlayerId = gameState?.viewingPlayerId

  const selectedCount = targetingState.selectedTargets.length
  const minTargets = targetingState.minTargets
  const maxTargets = targetingState.maxTargets
  const hasEnoughTargets = selectedCount >= minTargets
  const hasMaxTargets = selectedCount >= maxTargets

  // Group cards by graveyard owner
  const cardsByOwner = React.useMemo(() => {
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
  const ownerIds = React.useMemo(() => {
    const ids = Array.from(cardsByOwner.keys())
    return ids.sort((a, b) => {
      if (a === viewingPlayerId) return -1
      if (b === viewingPlayerId) return 1
      return 0
    })
  }, [cardsByOwner, viewingPlayerId])

  const [selectedOwnerId, setSelectedOwnerId] = React.useState<EntityId | null>(() => ownerIds[0] ?? null)
  const currentOwnerId = selectedOwnerId && ownerIds.includes(selectedOwnerId) ? selectedOwnerId : ownerIds[0] ?? null
  const currentCards = currentOwnerId ? (cardsByOwner.get(currentOwnerId) ?? []) : []

  // Sort cards by type then name
  const sortedCards = React.useMemo(() => {
    return [...currentCards].sort((a, b) => {
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
  }, [currentCards])

  const getPlayerLabel = (ownerId: EntityId): string => {
    if (ownerId === viewingPlayerId) return 'Your Graveyard'
    const player = gameState?.players.find((p) => p.playerId === ownerId)
    return player ? `${player.name}'s Graveyard` : "Opponent's Graveyard"
  }

  const toggleCard = (cardId: EntityId) => {
    if (targetingState.selectedTargets.includes(cardId)) {
      onDeselect(cardId)
    } else if (selectedCount < maxTargets) {
      onSelect(cardId)
    }
  }

  const gap = responsive.isMobile ? 8 : 12
  const availableWidth = responsive.viewportWidth - responsive.containerPadding * 2 - 64
  const maxCardWidth = responsive.isMobile ? 100 : 140
  const cardWidth = calculateFittingCardWidth(
    Math.min(sortedCards.length, 8),
    availableWidth,
    gap,
    maxCardWidth,
    60
  )

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
        {targetingState.totalRequirements && targetingState.totalRequirements > 1 && (
          <div
            style={{
              color: '#888',
              fontSize: responsive.fontSize.small,
              marginBottom: 6,
              textTransform: 'uppercase',
              letterSpacing: 1,
            }}
          >
            Step {(targetingState.currentRequirementIndex ?? 0) + 1} of {targetingState.totalRequirements}
          </div>
        )}
        <h2
          style={{
            color: 'white',
            margin: 0,
            fontSize: responsive.isMobile ? 20 : 28,
            fontWeight: 600,
          }}
        >
          {targetingState.targetDescription
            ? `Select ${targetingState.targetDescription}`
            : 'Choose Target from Graveyard'}
        </h2>
        <p
          style={{
            color: '#aaa',
            margin: '8px 0 0',
            fontSize: responsive.fontSize.normal,
          }}
        >
          {minTargets === 0
            ? `Select up to ${maxTargets} target${maxTargets > 1 ? 's' : ''} (optional)`
            : `Select ${minTargets === maxTargets ? minTargets : `${minTargets}-${maxTargets}`} target${maxTargets > 1 ? 's' : ''}`}
        </p>
      </div>

      {/* Graveyard tabs (if multiple graveyards) */}
      {ownerIds.length > 1 && (
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
              targetingState.selectedTargets.includes(c.id)
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
      )}

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
              color: hasEnoughTargets ? '#4ade80' : selectedCount > 0 ? '#fbbf24' : '#888',
              fontWeight: 600,
            }}
          >
            {selectedCount}
          </span>
          {' / '}
          {maxTargets}
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
        {sortedCards.map((card) => {
          const isSelected = targetingState.selectedTargets.includes(card.id)
          const cardImageUrl = getCardImageUrl(card.name, card.imageUri)
          const cardHeight = Math.round(cardWidth * 1.4)

          return (
            <div
              key={card.id}
              onClick={() => toggleCard(card.id)}
              onMouseEnter={() => hoverCard(card.id)}
              onMouseLeave={() => hoverCard(null)}
              style={{
                width: cardWidth,
                height: cardHeight,
                backgroundColor: isSelected ? '#1a3320' : '#1a1a1a',
                border: isSelected ? '3px solid #fbbf24' : '2px solid #333',
                borderRadius: responsive.isMobile ? 6 : 10,
                display: 'flex',
                flexDirection: 'column',
                overflow: 'hidden',
                cursor: hasMaxTargets && !isSelected ? 'not-allowed' : 'pointer',
                transition: 'all 0.2s ease-out',
                transform: isSelected ? 'translateY(-12px) scale(1.05)' : 'none',
                boxShadow: isSelected
                  ? '0 12px 28px rgba(251, 191, 36, 0.4), 0 0 20px rgba(251, 191, 36, 0.2)'
                  : '0 4px 12px rgba(0, 0, 0, 0.6)',
                flexShrink: 0,
                position: 'relative',
                opacity: hasMaxTargets && !isSelected ? 0.5 : 1,
              }}
            >
              <img
                src={cardImageUrl}
                alt={card.name}
                style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                onError={(e) => handleImageError(e, card.name, 'normal')}
              />
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
        })}
      </div>

      {/* No cards message */}
      {sortedCards.length === 0 && (
        <p style={{ color: '#666', fontSize: responsive.fontSize.normal }}>
          No valid targets in this graveyard.
        </p>
      )}

      {/* Buttons */}
      <div style={{ display: 'flex', gap: 12, marginTop: 8 }}>
        <button
          onClick={onConfirm}
          disabled={!hasEnoughTargets}
          style={{
            padding: responsive.isMobile ? '10px 24px' : '12px 36px',
            fontSize: responsive.fontSize.large,
            backgroundColor: hasEnoughTargets ? '#16a34a' : '#333',
            color: hasEnoughTargets ? 'white' : '#666',
            border: 'none',
            borderRadius: 8,
            cursor: hasEnoughTargets ? 'pointer' : 'not-allowed',
            fontWeight: 600,
            transition: 'all 0.15s',
          }}
        >
          {minTargets === 0 && selectedCount === 0 ? 'Skip' : selectedCount > 0 ? `Confirm (${selectedCount})` : 'Confirm Target'}
        </button>
        <button
          onClick={onCancel}
          style={{
            padding: responsive.isMobile ? '10px 24px' : '12px 36px',
            fontSize: responsive.fontSize.large,
            backgroundColor: '#444',
            color: 'white',
            border: 'none',
            borderRadius: 8,
            cursor: 'pointer',
            fontWeight: 600,
            transition: 'all 0.15s',
          }}
        >
          Cancel
        </button>
      </div>
    </div>
  )
}

/**
 * Targeting overlay that appears when selecting targets for spells/abilities.
 * Handles graveyard targeting, sacrifice selection, and normal targeting.
 */
export function TargetingOverlay() {
  const targetingState = useGameStore((state) => state.targetingState)
  const cancelTargeting = useGameStore((state) => state.cancelTargeting)
  const confirmTargeting = useGameStore((state) => state.confirmTargeting)
  const responsive = useResponsiveContext()

  const gameState = useGameStore((state) => state.gameState)
  const addTarget = useGameStore((state) => state.addTarget)
  const removeTarget = useGameStore((state) => state.removeTarget)

  // Only show when in targeting mode
  if (!targetingState) return null

  const selectedCount = targetingState.selectedTargets.length
  const minTargets = targetingState.minTargets
  const maxTargets = targetingState.maxTargets
  const hasEnoughTargets = selectedCount >= minTargets
  const hasMaxTargets = selectedCount >= maxTargets
  const isSacrifice = targetingState.isSacrificeSelection
  const isTapPermanent = targetingState.isTapPermanentSelection

  // Check if targets are graveyard cards — use explicit targetZone when available,
  // fall back to inspecting gameState.cards for backward compatibility
  const graveyardCards: ClientCard[] = []
  const isGraveyardTargeting = targetingState.targetZone === 'Graveyard'
  if (isGraveyardTargeting) {
    // Server told us this is a graveyard targeting requirement — collect cards
    for (const targetId of targetingState.validTargets) {
      const card = gameState?.cards[targetId]
      if (card) {
        graveyardCards.push(card)
      }
    }
  } else {
    // Fallback: check if all valid targets happen to be graveyard cards
    let allTargetsAreGraveyard = targetingState.validTargets.length > 0
    for (const targetId of targetingState.validTargets) {
      const card = gameState?.cards[targetId]
      if (card && card.zone?.zoneType === 'Graveyard') {
        graveyardCards.push(card)
      } else {
        allTargetsAreGraveyard = false
        break
      }
    }
    if (!allTargetsAreGraveyard) {
      graveyardCards.length = 0
    }
  }

  // If targets are graveyard cards, show graveyard selection UI
  if (graveyardCards.length > 0) {
    return (
      <GraveyardTargetingOverlay
        graveyardCards={graveyardCards}
        targetingState={targetingState}
        responsive={responsive}
        onSelect={addTarget}
        onDeselect={removeTarget}
        onConfirm={confirmTargeting}
        onCancel={cancelTargeting}
      />
    )
  }

  // Build the target count display
  const targetDisplay = minTargets === maxTargets
    ? `${selectedCount}/${maxTargets}`
    : `${selectedCount} (${minTargets}-${maxTargets})`

  // Multi-target step info
  const isMultiTarget = targetingState.totalRequirements && targetingState.totalRequirements > 1
  const stepLabel = isMultiTarget
    ? `Step ${(targetingState.currentRequirementIndex ?? 0) + 1}/${targetingState.totalRequirements}`
    : null

  // Build the prompt text based on selection type
  const promptText = isTapPermanent
    ? `Select permanents to tap (${targetDisplay})`
    : isSacrifice
      ? `Select creature to sacrifice (${targetDisplay})`
      : targetingState.targetDescription
        ? `Select ${targetingState.targetDescription} (${targetDisplay})`
        : `Select targets (${targetDisplay})`

  const hintText = hasMaxTargets
    ? isTapPermanent ? 'Permanents selected' : isSacrifice ? 'Creature selected' : 'Maximum targets selected'
    : hasEnoughTargets
      ? 'Click Confirm or select more'
      : isTapPermanent ? 'Click a highlighted permanent' : isSacrifice ? 'Click a creature you control' : 'Click a highlighted target'

  return (
    <div style={{
      ...styles.targetingOverlay,
      padding: responsive.isMobile ? '12px 16px' : '16px 24px',
      borderColor: TARGET_COLOR,
    }}>
      {stepLabel && (
        <div style={{
          color: '#888',
          fontSize: responsive.fontSize.small,
          textTransform: 'uppercase',
          letterSpacing: 1,
          marginBottom: 2,
        }}>
          {stepLabel}
        </div>
      )}
      <div style={{
        ...styles.targetingPrompt,
        fontSize: responsive.fontSize.normal,
        color: TARGET_COLOR_BRIGHT,
      }}>
        {promptText}
      </div>
      <div style={{ color: '#aaa', fontSize: responsive.fontSize.small, marginTop: 4 }}>
        {hintText}
      </div>
      <div style={{ display: 'flex', gap: 8, marginTop: 8 }}>
        {hasEnoughTargets && (
          <button onClick={confirmTargeting} style={{
            ...styles.actionButton,
            padding: responsive.isMobile ? '8px 12px' : '10px 16px',
            fontSize: responsive.fontSize.normal,
          }}>
            Confirm ({selectedCount})
          </button>
        )}
        <button onClick={cancelTargeting} style={{
          ...styles.cancelButton,
          padding: responsive.isMobile ? '8px 12px' : '10px 16px',
          fontSize: responsive.fontSize.normal,
        }}>
          Cancel
        </button>
      </div>
    </div>
  )
}
