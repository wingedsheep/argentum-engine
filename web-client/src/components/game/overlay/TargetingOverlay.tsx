import React from 'react'
import { useGameStore } from '@/store/gameStore.ts'
import type { ClientCard, EntityId } from '@/types'
import type { ResponsiveSizes } from '@/hooks/useResponsive.ts'
import { calculateFittingCardWidth } from '@/hooks/useResponsive.ts'
import { useDraggable } from '@/hooks/useDraggable.ts'
import { getCardImageUrl } from '@/utils/cardImages.ts'
import { routeTargetsByZone } from '@/utils/targeting.ts'
import { useResponsiveContext, handleImageError } from '../board/shared'
import { styles } from '../board/styles'
import { TARGET_COLOR, TARGET_COLOR_BRIGHT } from '@/styles/targetingColors.ts'
import { CraftMaterialOverlay } from '@/components/decisions/CraftMaterialOverlay'
import { ManaSymbol } from '@/components/ui/ManaSymbols'
import { parseManaCost } from '@/utils/manaCost'

/**
 * Cross-zone card targeting overlay — shows when targeting mode requires selecting card(s) from a
 * card zone (graveyard or exile), possibly a union of both (Sorceress's Schemes). Cards are grouped
 * into tabs by (owner, zone) so e.g. "Your Graveyard" and "Your Exile" are distinct, browsable
 * piles. Similar to GraveyardTargetingUI in DecisionUI but for client-side spell casting targeting.
 */
function ZoneCardTargetingOverlay({
  zoneCards,
  targetingState,
  responsive,
  onSelect,
  onDeselect,
  onConfirm,
  onCancel,
  onBack,
  onViewBattlefield,
}: {
  zoneCards: ClientCard[]
  targetingState: { selectedTargets: readonly EntityId[]; minTargets: number; maxTargets: number; targetDescription?: string; currentRequirementIndex?: number; totalRequirements?: number; sourceCardName?: string; minTotalManaValue?: number }
  responsive: ResponsiveSizes
  onSelect: (cardId: EntityId) => void
  onDeselect: (cardId: EntityId) => void
  onConfirm: () => void
  onCancel: () => void
  /** Present when an earlier target requirement can be revised (multi-target spells). */
  onBack?: () => void
  /**
   * Present when the caller owns the "look at the board instead" state — a mixed
   * battlefield ∪ pile requirement, where dismissing the picker hands control back to the
   * targeting banner (which keeps Confirm/Cancel and leaves permanents clickable). Absent for a
   * pile-only requirement, where there is nothing to pick on the board and the overlay minimizes
   * itself into a "return to card selection" button instead.
   */
  onViewBattlefield?: () => void
}) {
  const hoverCard = useGameStore((s) => s.hoverCard)
  const gameState = useGameStore((s) => s.gameState)
  const viewingPlayerId = gameState?.viewingPlayerId
  const [minimized, setMinimized] = React.useState(false)

  const selectedCount = targetingState.selectedTargets.length
  const minTargets = targetingState.minTargets
  const maxTargets = targetingState.maxTargets

  // Collect evidence N (CR 701.59a): the cost constrains the *summed mana value* of the picked
  // cards, not how many there are, so Confirm is gated on the running total rather than the count.
  // An empty selection is exempt — that is how an optional collection is declined.
  const manaFloor = targetingState.minTotalManaValue
  const totalManaValueSelected =
    manaFloor == null
      ? 0
      : targetingState.selectedTargets.reduce(
          (sum, id) => sum + (gameState?.cards[id]?.manaValue ?? 0),
          0,
        )
  const meetsManaFloor =
    manaFloor == null || selectedCount === 0 || totalManaValueSelected >= manaFloor

  const hasEnoughTargets = selectedCount >= minTargets && meetsManaFloor
  const hasMaxTargets = manaFloor != null ? false : selectedCount >= maxTargets

  // A group key combines the owning player and the card's zone, so graveyard and exile piles for
  // the same player are separate tabs. Zone defaults to Graveyard when the card carries none.
  const groupKeyOf = (card: ClientCard): string =>
    `${card.zone?.ownerId ?? card.ownerId}|${card.zone?.zoneType ?? 'Graveyard'}`

  // Group cards by (owner, zone)
  const cardsByGroup = React.useMemo(() => {
    const grouped = new Map<string, ClientCard[]>()
    for (const card of zoneCards) {
      const key = groupKeyOf(card)
      if (!grouped.has(key)) {
        grouped.set(key, [])
      }
      grouped.get(key)!.push(card)
    }
    return grouped
  }, [zoneCards])

  // Get group keys sorted (viewer's piles first)
  const groupKeys = React.useMemo(() => {
    const ids = Array.from(cardsByGroup.keys())
    return ids.sort((a, b) => {
      const aMine = a.startsWith(`${viewingPlayerId}|`)
      const bMine = b.startsWith(`${viewingPlayerId}|`)
      if (aMine && !bMine) return -1
      if (bMine && !aMine) return 1
      return a.localeCompare(b)
    })
  }, [cardsByGroup, viewingPlayerId])

  const [selectedGroupKey, setSelectedGroupKey] = React.useState<string | null>(() => groupKeys[0] ?? null)
  const currentGroupKey = selectedGroupKey && groupKeys.includes(selectedGroupKey) ? selectedGroupKey : groupKeys[0] ?? null
  const currentCards = currentGroupKey ? (cardsByGroup.get(currentGroupKey) ?? []) : []

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

  const getGroupLabel = (groupKey: string): string => {
    const [ownerId, zoneType = 'Graveyard'] = groupKey.split('|')
    const zoneLabel = zoneType === 'Exile' ? 'Exile' : 'Graveyard'
    if (ownerId === viewingPlayerId) return `Your ${zoneLabel}`
    const player = gameState?.players.find((p) => p.playerId === ownerId)
    return player ? `${player.name}'s ${zoneLabel}` : `Opponent's ${zoneLabel}`
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

  if (minimized && !onViewBattlefield) {
    return (
      <button
        onClick={() => setMinimized(false)}
        style={{
          position: 'fixed',
          bottom: 70,
          left: '50%',
          transform: 'translateX(-50%)',
          padding: responsive.isMobile ? '10px 16px' : '12px 24px',
          fontSize: responsive.fontSize.normal,
          backgroundColor: '#1e40af',
          color: 'white',
          border: 'none',
          borderRadius: 8,
          cursor: 'pointer',
          fontWeight: 600,
          boxShadow: '0 4px 12px rgba(0,0,0,0.4)',
          zIndex: 100,
        }}
      >
        ↑ Return to Card Selection
      </button>
    )
  }

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
            : 'Choose Target'}
        </h2>
        {targetingState.sourceCardName && (
          <p
            style={{
              color: '#ccc',
              margin: '4px 0 0',
              fontSize: responsive.fontSize.normal,
              fontStyle: 'italic',
            }}
          >
            for {targetingState.sourceCardName}
          </p>
        )}
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

      {/* Zone tabs (if multiple graveyard/exile piles) */}
      {groupKeys.length > 1 && (
        <div
          style={{
            display: 'flex',
            gap: responsive.isMobile ? 8 : 12,
            backgroundColor: 'rgba(0, 0, 0, 0.4)',
            padding: 4,
            borderRadius: 8,
          }}
        >
          {groupKeys.map((groupKey) => {
            const isActive = groupKey === currentGroupKey
            const groupCards = cardsByGroup.get(groupKey) ?? []
            const cardCount = groupCards.length
            // Count how many cards are selected from this pile
            const selectedFromThisGroup = groupCards.filter((c) =>
              targetingState.selectedTargets.includes(c.id)
            ).length
            return (
              <button
                key={groupKey}
                onClick={() => setSelectedGroupKey(groupKey)}
                style={{
                  padding: responsive.isMobile ? '8px 16px' : '10px 24px',
                  fontSize: responsive.fontSize.normal,
                  backgroundColor: isActive ? '#4a5568' : 'transparent',
                  color: isActive ? 'white' : '#888',
                  border: selectedFromThisGroup > 0 && !isActive ? '2px solid #fbbf24' : 'none',
                  borderRadius: 6,
                  cursor: 'pointer',
                  fontWeight: isActive ? 600 : 400,
                  transition: 'all 0.15s',
                  position: 'relative',
                }}
              >
                {getGroupLabel(groupKey)} ({cardCount})
                {selectedFromThisGroup > 0 && (
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
                    {selectedFromThisGroup}
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
              onMouseEnter={(e) => hoverCard(card.id, { x: e.clientX, y: e.clientY })}
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
          No valid targets here.
        </p>
      )}

      {/* Buttons */}
      <div style={{ display: 'flex', gap: 12, marginTop: 8 }}>
        {onBack && (
          <button
            onClick={onBack}
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
            ← Back
          </button>
        )}
        <button
          onClick={() => (onViewBattlefield ? onViewBattlefield() : setMinimized(true))}
          style={{
            padding: responsive.isMobile ? '10px 24px' : '12px 36px',
            fontSize: responsive.fontSize.large,
            backgroundColor: '#1e40af',
            color: 'white',
            border: 'none',
            borderRadius: 8,
            cursor: 'pointer',
            fontWeight: 600,
            transition: 'all 0.15s',
          }}
        >
          View Battlefield
        </button>
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
          {manaFloor != null
            ? `Confirm (${totalManaValueSelected}/${manaFloor} mana value)`
            : minTargets === 0 && selectedCount === 0
              ? 'Skip'
              : selectedCount > 0
                ? `Confirm (${selectedCount})`
                : 'Confirm Target'}
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
  const goBackTargeting = useGameStore((state) => state.goBackTargeting)
  const responsive = useResponsiveContext()
  const draggable = useDraggable()

  const gameState = useGameStore((state) => state.gameState)
  const addTarget = useGameStore((state) => state.addTarget)
  const removeTarget = useGameStore((state) => state.removeTarget)

  // Whether the pile picker is open on a *mixed* battlefield ∪ pile requirement (see below).
  // This overlay is mounted for the whole game, so the flag is reset whenever targeting ends or
  // advances to the next requirement — otherwise a picker left open would reopen over an unrelated
  // spell's board targeting.
  const [pilePickerOpen, setPilePickerOpen] = React.useState(false)
  const requirementKey = targetingState
    ? `${targetingState.currentRequirementIndex ?? 0}`
    : null
  React.useEffect(() => {
    setPilePickerOpen(false)
  }, [requirementKey])

  // Only show when in targeting mode
  if (!targetingState) return null

  // Craft material selection (CR 702.167) spans BF + GY simultaneously — route to the
  // dedicated cross-zone overlay rather than the single-zone targeting flows below.
  if (targetingState.isCraftMaterialSelection) {
    return <CraftMaterialOverlay responsive={responsive} />
  }

  const selectedCount = targetingState.selectedTargets.length
  const minTargets = targetingState.minTargets
  const maxTargets = targetingState.maxTargets
  // Teamwork N (CR 702.194a) and any other "tap any number … with total power N or more" cost:
  // how many permanents are picked is free, so the confirm gate is the summed power the server
  // sent with the candidates — never a client-derived one.
  const requiredTotalPower = targetingState.requiredTotalPower ?? 0
  const selectedTotalPower = targetingState.selectedTargets.reduce(
    (sum, id) => sum + (targetingState.powerByEntityId?.[id] ?? 0),
    0,
  )
  const hasEnoughTargets = requiredTotalPower > 0
    ? selectedTotalPower >= requiredTotalPower
    : selectedCount >= minTargets
  const hasMaxTargets = selectedCount >= maxTargets
  const canGoBack = (targetingState.previousRequirementStates?.length ?? 0) > 0
  const isSacrifice = targetingState.isSacrificeSelection
  const isBounce = targetingState.isBounceSelection
  const isTapPermanent = targetingState.isTapPermanentSelection
  const isDiscard = targetingState.isDiscardSelection
  const isReveal = targetingState.isRevealSelection
  const isBehold = targetingState.isBeholdSelection

  // Split the server's valid targets by how the player can physically reach them: a graveyard or
  // exile pile isn't individually clickable on the board, so those cards need the cross-zone
  // picker, while permanents, players and stack objects are clicked on the board. Zones come from
  // server-sent card state — nothing here decides legality.
  //
  // Three shapes fall out, and all three occur:
  //  - pile only (a graveyard reanimation spell, Sorceress's Schemes' graveyard ∪ exile): the
  //    picker owns the screen, exactly as before.
  //  - board only: the draggable banner, and the player clicks the board.
  //  - **both** (Taskmaster, Mercenary Mimic: "target creature on the battlefield *or* creature
  //    card in a graveyard"): the banner stays up so permanents remain clickable, *and* it offers
  //    a button that opens the picker for the pile half. Selections live in the shared targeting
  //    store, so a pick made on either side counts toward the same requirement and either side's
  //    Confirm submits them. Before this, a mixed union fell through to board-only clicking and
  //    the graveyard half was simply unreachable.
  const { mode, pileCards, pileZoneLabel } = routeTargetsByZone(
    targetingState.validTargets,
    gameState?.cards,
    targetingState.targetZone,
  )
  const isMixedZoneTargeting = mode === 'mixed'

  // Pile-only: the picker is the whole UI (unchanged behaviour).
  if (mode === 'pile') {
    return (
      <ZoneCardTargetingOverlay
        zoneCards={pileCards}
        targetingState={targetingState}
        responsive={responsive}
        onSelect={addTarget}
        onDeselect={removeTarget}
        onConfirm={confirmTargeting}
        onCancel={cancelTargeting}
        {...(canGoBack ? { onBack: goBackTargeting } : {})}
      />
    )
  }

  // Mixed union, picker open: same picker, but "View Battlefield" hands control back to the
  // banner below rather than minimising into a bare re-open button — the banner is where Confirm
  // and Cancel live for this requirement, and the board half still has to be clickable.
  if (isMixedZoneTargeting && pilePickerOpen) {
    return (
      <ZoneCardTargetingOverlay
        zoneCards={pileCards}
        targetingState={targetingState}
        responsive={responsive}
        onSelect={addTarget}
        onDeselect={removeTarget}
        onConfirm={confirmTargeting}
        onCancel={cancelTargeting}
        onViewBattlefield={() => setPilePickerOpen(false)}
        {...(canGoBack ? { onBack: goBackTargeting } : {})}
      />
    )
  }

  // Build the target count display. A total-power cost counts power, not permanents.
  const targetDisplay = requiredTotalPower > 0
    ? `power ${selectedTotalPower}/${requiredTotalPower}`
    : minTargets === maxTargets
      ? `${selectedCount}/${maxTargets}`
      : `${selectedCount} (${minTargets}-${maxTargets})`

  // Multi-target step info
  const isMultiTarget = targetingState.totalRequirements && targetingState.totalRequirements > 1
  const stepLabel = isMultiTarget
    ? `Step ${(targetingState.currentRequirementIndex ?? 0) + 1}/${targetingState.totalRequirements}`
    : null

  // Build the prompt text based on selection type. isBounce is checked before isSacrifice
  // because a bounce cost (Sneak, CR 702.190) sets both flags but is a "return to hand", not
  // a sacrifice.
  const promptText = isBehold
    ? `${targetingState.targetDescription ?? 'Behold a card'} (${targetDisplay})`
    : isDiscard
      ? `Select card to discard (${targetDisplay})`
      : isReveal
        ? `Select card to reveal (${targetDisplay})`
        : isTapPermanent
          ? // The server already sends the cost description sentence-cased ("Tap any number of
            // creatures you control with total power 2 or more"), so it is shown verbatim.
            requiredTotalPower > 0 && targetingState.targetDescription
            ? `${targetingState.targetDescription} (${targetDisplay})`
            : `Select permanents to tap (${targetDisplay})`
          : isBounce
            ? `Select ${targetingState.targetDescription ?? 'a creature to return to its owner’s hand'} (${targetDisplay})`
            : isSacrifice
              ? // The cost description reads "sacrifice an artifact" / "sacrifice two creatures";
                // capitalise it rather than assuming a creature.
                targetingState.targetDescription
                ? `${targetingState.targetDescription.charAt(0).toUpperCase()}${targetingState.targetDescription.slice(1)} (${targetDisplay})`
                : `Select permanent to sacrifice (${targetDisplay})`
              : targetingState.targetDescription
                ? `Select ${targetingState.targetDescription} (${targetDisplay})`
                : `Select targets (${targetDisplay})`

  // Emerge (CR 702.119): the sacrifice is the only cost choice that changes the mana owed, so the
  // banner shows the server's per-candidate arithmetic live — "{5}{U} → {2}{U}" the moment a
  // creature is picked. Without it the player has to know the generic-only reduction rule and do
  // the subtraction in their head against a cost label that never moves.
  const costBeforeSacrifice = targetingState.costBeforeSacrifice
  const costAfterMap = targetingState.costAfterSacrifice
  const chosenSacrifice = targetingState.selectedTargets[0]
  const costAfterSacrifice =
    costAfterMap && chosenSacrifice ? costAfterMap[chosenSacrifice] : undefined
  const showSacrificeCost = !!costBeforeSacrifice && !!costAfterMap

  const baseHintText = hasMaxTargets
    ? isBehold ? 'Card selected' : isDiscard ? 'Card selected' : isReveal ? 'Card selected' : isTapPermanent ? 'Permanents selected' : isBounce ? 'Creature selected' : isSacrifice ? 'Selected' : 'Maximum targets selected'
    : hasEnoughTargets
      ? 'Click Confirm or select more'
      : isBehold ? `Click a highlighted card on the battlefield or in your hand` : isDiscard ? 'Click a card in your hand' : isReveal ? 'Click a card in your hand' : isTapPermanent ? 'Click a highlighted permanent' : isBounce ? 'Click an attacking creature you control' : isSacrifice ? 'Click a highlighted permanent you control' : 'Click a highlighted target'
  // On a mixed union the board is only half the answer — say so, since the other half lives
  // behind the button below and there is nothing on the board to hint at it.
  const hintText = isMixedZoneTargeting && !hasMaxTargets
    ? `${baseHintText}, or open the ${pileZoneLabel.toLowerCase()}`
    : baseHintText

  return (
    <div
      ref={draggable.ref}
      style={{
        ...styles.targetingOverlay,
        ...draggable.style,
        padding: responsive.isMobile ? '12px 16px' : '16px 24px',
        borderColor: TARGET_COLOR,
        pointerEvents: 'none',
      }}
    >
      <div
        aria-label="Drag to move"
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          width: '100%',
          padding: '4px 0',
          margin: '-8px 0 -4px',
          cursor: draggable.isDragging ? 'grabbing' : 'grab',
          touchAction: 'none',
          pointerEvents: 'auto',
        }}
        {...draggable.handleProps}
      >
        <span
          style={{
            width: 36,
            height: 4,
            borderRadius: 9999,
            backgroundColor: TARGET_COLOR,
            opacity: draggable.isDragging ? 0.9 : 0.65,
          }}
        />
      </div>
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
      {showSacrificeCost && (
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            flexWrap: 'wrap',
            gap: 6,
            marginTop: 8,
            padding: '5px 10px',
            borderRadius: 6,
            background: 'rgba(255, 255, 255, 0.06)',
            fontSize: responsive.fontSize.small,
          }}
        >
          <span style={{ color: '#aaa' }}>You pay</span>
          <span style={{ display: 'inline-flex', gap: 2, opacity: costAfterSacrifice ? 0.45 : 1 }}>
            {parseManaCost(costBeforeSacrifice!).map((symbol, i) => (
              <ManaSymbol key={i} symbol={symbol} size={16} />
            ))}
          </span>
          <span style={{ color: '#888' }}>→</span>
          {costAfterSacrifice !== undefined ? (
            parseManaCost(costAfterSacrifice).length > 0 ? (
              <span style={{ display: 'inline-flex', gap: 2 }}>
                {parseManaCost(costAfterSacrifice).map((symbol, i) => (
                  <ManaSymbol key={i} symbol={symbol} size={16} />
                ))}
              </span>
            ) : (
              <span style={{ color: '#86efac', fontWeight: 700 }}>free</span>
            )
          ) : (
            <span style={{ color: '#888', fontStyle: 'italic' }}>
              pick a creature — its mana value comes off
            </span>
          )}
        </div>
      )}
      {targetingState.warning && (
        <div
          role="alert"
          style={{
            marginTop: 8,
            padding: '6px 10px',
            borderRadius: 6,
            background: 'rgba(251, 191, 36, 0.15)',
            border: '1px solid rgba(251, 191, 36, 0.7)',
            color: '#fde68a',
            fontSize: responsive.fontSize.small,
            fontWeight: 600,
            lineHeight: 1.3,
            pointerEvents: 'auto',
          }}
        >
          {targetingState.warning}
        </div>
      )}
      <div style={{ display: 'flex', gap: 8, marginTop: 8, pointerEvents: 'auto' }}>
        {isMixedZoneTargeting && (
          <button
            onClick={() => setPilePickerOpen(true)}
            style={{
              ...styles.actionButton,
              padding: responsive.isMobile ? '8px 12px' : '10px 16px',
              fontSize: responsive.fontSize.normal,
              backgroundColor: '#1e40af',
            }}
          >
            {pileZoneLabel} ({pileCards.length})
          </button>
        )}
        {canGoBack && (
          <button onClick={goBackTargeting} style={{
            ...styles.cancelButton,
            padding: responsive.isMobile ? '8px 12px' : '10px 16px',
            fontSize: responsive.fontSize.normal,
          }}>
            ← Back
          </button>
        )}
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
