import { useEffect, useCallback, useMemo, useState } from 'react'
import { useGameStore } from '@/store/gameStore.ts'
import { useCardActions } from '@/hooks/useLegalActions.ts'
import { useInteraction } from '@/hooks/useInteraction.ts'
import type { LegalActionInfo } from '@/types'
import { ManaCost, AbilityText } from './ManaSymbols'
import { ManaCostProgress } from './ManaCostProgress'
import { useViewingPlayer } from '@/store/selectors.ts'
import { isManaPoolEmpty } from '@/types'
import { getCardImageUrl } from '@/utils/cardImages.ts'
import { buildActionOptions, type ActionOption } from '@/utils/actionOptions.ts'
import styles from './ActionMenu.module.css'

/**
 * Action menu displayed when a card with multiple actions is selected.
 * Shows as a centered modal when multiple actions are available.
 */
export function ActionMenu() {
  const selectedCardId = useGameStore((state) => state.selectedCardId)
  const gameState = useGameStore((state) => state.gameState)
  const cardActions = useCardActions(selectedCardId)
  const { executeAction, cancelAction } = useInteraction()

  // Get card info for modal display
  const cardInfo = selectedCardId ? gameState?.cards[selectedCardId] ?? null : null

  // Build all action options (available + unavailable)
  const actionOptions = useMemo(
    () => buildActionOptions(cardInfo, cardActions),
    [cardInfo, cardActions]
  )

  // Check if we should show the modal
  // Show modal when there are multiple options OR when there's at least one action
  // (so user can see mana cost and confirm their choice)
  const hasMultiplePotentialOptions = actionOptions.length > 1
  const hasSingleAction = actionOptions.length === 1
  const hasAnyLegalAction = cardActions.length > 0
  // Show the nice modal for any actionable card click
  const shouldShowModal = hasMultiplePotentialOptions || hasSingleAction

  // Debug logging - always log when card is selected
  if (import.meta.env.DEV && selectedCardId) {
    console.log('ActionMenu render:', {
      selectedCardId,
      cardActionsCount: cardActions.length,
      cardActionTypes: cardActions.map(a => a.action.type),
      actionOptionsCount: actionOptions.length,
      actionOptionKeys: actionOptions.map(o => o.key),
    })
  }

  // Handle Escape key to cancel
  const handleKeyDown = useCallback(
    (e: KeyboardEvent) => {
      if (e.key === 'Escape' && selectedCardId && shouldShowModal) {
        cancelAction()
      }
    },
    [selectedCardId, shouldShowModal, cancelAction]
  )

  useEffect(() => {
    if (selectedCardId && shouldShowModal) {
      window.addEventListener('keydown', handleKeyDown)
      return () => window.removeEventListener('keydown', handleKeyDown)
    }
  }, [selectedCardId, shouldShowModal, handleKeyDown])

  // Don't show if no card selected or no potential options
  if (!selectedCardId || !hasAnyLegalAction) {
    return null
  }

  // Show floating action panel (subtle, no full overlay)
  if (shouldShowModal) {
    // Get card image URL
    const cardImageUrl = cardInfo ? getCardImageUrl(cardInfo.name, cardInfo.imageUri, 'large') : null

    return (
      <div className={styles.container}>
        <div className={styles.panel}>
          {/* Large card image — rotated landscape for split-layout cards (CR 709):
              Rooms and classic split spells like Pain // Suffering are printed portrait
              with the two halves stacked. */}
          {cardImageUrl && (
            <CardImage
              imageUrl={cardImageUrl}
              cardName={cardInfo?.name ?? 'Card'}
              rotateDeg={cardInfo?.cardFaces?.length === 2 ? 90 : 0}
            />
          )}

          {/* Action buttons */}
          <div className={styles.actionsContainer}>
            {actionOptions.map((option) => (
              <ActionOptionButton
                key={option.key}
                option={option}
                onClick={() => {
                  if (option.isAvailable && option.action) {
                    executeAction(option.action)
                  }
                }}
              />
            ))}
          </div>

          {/* Cancel button */}
          <button onClick={cancelAction} className={styles.cancelButton}>
            Cancel (Esc)
          </button>
        </div>
      </div>
    )
  }

  // Fallback (shouldn't reach here normally)
  return (
    <div className={styles.fallbackContainer}>
      <span className={styles.fallbackLabel}>
        Choose action:
      </span>

      {cardActions.map((action, index) => (
        <ActionButton
          key={index}
          action={action}
          onClick={() => executeAction(action)}
        />
      ))}

      <button onClick={cancelAction} className={styles.fallbackCancelButton}>
        Cancel
      </button>
    </div>
  )
}

/**
 * Mana-font loyalty icon for planeswalker abilities.
 *   +N → `ms-loyalty-up ms-loyalty-N`  (green upward chevron with "+N")
 *   -N → `ms-loyalty-down ms-loyalty-N` (red downward chevron with "-N")
 *    0 → `ms-loyalty-zero` (neutral)
 *
 * Magnitude > 20 falls back to the plain chevron (mana-font ships variants up to 20).
 */
function LoyaltyIcon({ change }: { change: number }) {
  const magnitude = Math.abs(change)
  const direction = change > 0 ? 'up' : change < 0 ? 'down' : 'zero'
  // mana-font ships ms-loyalty-{0..20,25} digit overlays; combine with the direction class
  // so 0-cost abilities render the neutral marker with a "0" inside, matching ±N styling.
  const hasNumberVariant = (direction === 'zero' && magnitude === 0) || (magnitude >= 1 && magnitude <= 20)
  const className = hasNumberVariant
    ? `ms ms-loyalty-${direction} ms-loyalty-${magnitude}`
    : `ms ms-loyalty-${direction}`
  return (
    <span
      aria-hidden
      className={className}
      style={{ fontSize: 22, marginRight: 6, flexShrink: 0 }}
    />
  )
}

/**
 * Time-counter glyph for the impending cast option (CR 702.176). Mirrors the battlefield
 * time-counter badge (mana-font `ms ms-counter-time` + count) so the player recognizes that
 * casting for impending makes the permanent enter with `time` time counters.
 */
function ImpendingTimeIcon({ time }: { time: number }) {
  return (
    <span
      aria-label={`Enters with ${time} time counters`}
      title={`Enters with ${time} time counters`}
      style={{ display: 'inline-flex', alignItems: 'center', gap: 2, marginRight: 6, flexShrink: 0 }}
    >
      <i className="ms ms-counter-time" style={{ fontSize: 18 }} aria-hidden />
      <span style={{ fontWeight: 700, fontSize: 13 }}>{time}</span>
    </span>
  )
}

/**
 * Get the style class for an action type.
 */
function getActionStyleClass(actionType: ActionOption['actionType'], isAvailable: boolean): string {
  if (!isAvailable) {
    return styles.actionDisabled ?? ''
  }
  switch (actionType) {
    case 'cast':
      return styles.actionCast ?? ''
    case 'castFaceDown':
      return styles.actionCastFaceDown ?? ''
    case 'castWithKicker':
      return styles.actionCastWithKicker ?? ''
    case 'cycle':
      return styles.actionCycle ?? ''
    case 'plot':
      return styles.actionCycle ?? ''
    case 'suspend':
      return styles.actionCycle ?? ''
    case 'playLand':
      return styles.actionPlayLand ?? ''
    case 'activate':
      return styles.actionActivate ?? ''
    case 'turnFaceUp':
      return styles.actionTurnFaceUp ?? ''
    default:
      return styles.actionDisabled ?? ''
  }
}

/**
 * Action option button for the cast method selection.
 * Compact design with mana costs.
 */
function ActionOptionButton({
  option,
  onClick,
}: {
  option: ActionOption
  onClick: () => void
}) {
  const setAutoTapPreview = useGameStore((state) => state.setAutoTapPreview)
  const startManaSelection = useGameStore((state) => state.startManaSelection)
  const startPipeline = useGameStore((state) => state.startPipeline)
  const viewingPlayer = useViewingPlayer()
  const manaPool = viewingPlayer?.manaPool
  const hasFloatingMana = manaPool != null && !isManaPoolEmpty(manaPool)
  const styleClass = getActionStyleClass(option.actionType, option.isAvailable)
  // Only show separate mana cost if label doesn't already contain mana symbols
  const showSeparateCost = option.manaCost && !option.label.includes('{')
  // Mana pips for a cost, showing progress against floating mana when the player has any.
  const renderCost = (cost: string | null) =>
    hasFloatingMana && manaPool
      ? <ManaCostProgress
          cost={cost}
          manaPool={manaPool}
          eligibleRestrictedMana={option.action?.eligibleRestrictedMana ?? []}
          size={16}
          gap={2}
        />
      : <ManaCost cost={cost} size={16} gap={2} />
  // Show mana selection icon for actions that have mana sources available
  // Delve and Convoke spells handle mana selection after their selector, so don't show the icon
  const hasManaSelection = option.isAvailable && option.action?.availableManaSources != null && option.action.availableManaSources.length > 0 && !option.action.hasDelve && !option.action.hasConvoke

  return (
    <div style={{ display: 'flex', alignItems: 'stretch', gap: 0 }}>
      <button
        onClick={onClick}
        disabled={!option.isAvailable}
        className={`${styles.actionButton} ${styleClass}`}
        style={hasManaSelection ? { borderTopRightRadius: 0, borderBottomRightRadius: 0 } : undefined}
        onMouseEnter={() => {
          if (option.action?.autoTapPreview) {
            setAutoTapPreview(option.action.autoTapPreview)
          }
        }}
        onMouseLeave={() => {
          setAutoTapPreview(null)
        }}
      >
        <span className={styles.actionButtonLeading}>
          {option.loyaltyChange !== undefined && (
            <LoyaltyIcon change={option.loyaltyChange} />
          )}
          {option.impendingTime !== undefined && (
            <ImpendingTimeIcon time={option.impendingTime} />
          )}
          <span className={styles.actionButtonLabelStack}>
            <span className={styles.actionButtonLabel}>
              <AbilityText text={option.label} size={14} />
            </span>
            {option.hint && (
              <span className={styles.actionButtonHint}>{option.hint}</span>
            )}
          </span>
        </span>
        {showSeparateCost && (
          option.manaCostReducedTo
            ? (
              // The printed cost, dimmed, then what a best-case choice leaves. "As low as" keeps it
              // honest: the reduction depends on which creature the player picks next.
              <span className={styles.actionButtonCostReduction}>
                <span className={styles.actionButtonCostBefore}>
                  <ManaCost cost={option.manaCost} size={13} gap={1} />
                </span>
                <span aria-hidden className={styles.actionButtonCostArrow}>→</span>
                <span className={styles.actionButtonCostNote}>as low as</span>
                {renderCost(option.manaCostReducedTo)}
              </span>
            )
            : renderCost(option.manaCost)
        )}
      </button>
      {hasManaSelection && option.action && (
        <button
          onClick={(e) => {
            e.stopPropagation()
            // X-cost spells must pick X before land selection so the mana phase
            // can pre-select enough sources for the total cost. Route through the
            // pipeline (xSelection → manaSource) rather than opening mana selection
            // directly with xValue=0.
            if (option.action!.hasXCost) {
              startPipeline(option.action!, { forceManualTap: true })
            } else {
              startManaSelection(option.action!)
            }
          }}
          className={styles.manaSelectionButton}
          title="Choose which lands to tap"
          onMouseEnter={() => {
            if (option.action?.autoTapPreview) {
              setAutoTapPreview(option.action.autoTapPreview)
            }
          }}
          onMouseLeave={() => {
            setAutoTapPreview(null)
          }}
        >
          <i className="ms ms-land" style={{ fontSize: 12 }} />
        </button>
      )}
    </div>
  )
}

/**
 * Individual action button (fallback style).
 */
function ActionButton({
  action,
  onClick,
}: {
  action: LegalActionInfo
  onClick: () => void
}) {
  const getActionColorClass = () => {
    // Handle special action types first (these have actionType different from action.type)
    if (action.actionType === 'CastFaceDown') {
      return styles.fallbackCastFaceDown
    }
    if (action.actionType === 'CastWithKicker') {
      return styles.fallbackCastWithKicker
    }
    if (action.actionType === 'TurnFaceUp') {
      return styles.fallbackTurnFaceUp
    }

    switch (action.action.type) {
      case 'PlayLand':
        return styles.fallbackPlayLand
      case 'CastSpell':
        return styles.fallbackCast
      case 'CycleCard':
        return styles.fallbackCycle
      case 'PlotCard':
        return styles.fallbackCycle
      case 'SuspendCardFromHand':
        return styles.fallbackCycle
      case 'ActivateAbility':
        return styles.fallbackActivate
      case 'TurnFaceUp':
        return styles.fallbackTurnFaceUp
      case 'PassPriority':
        return styles.fallbackPass
      default:
        return styles.fallbackDefault
    }
  }

  // Get user-friendly label for action type
  const getActionLabel = () => {
    switch (action.actionType) {
      case 'CastFaceDown':
        return 'Cast Face-Down ({3})'
      case 'CastWithKicker':
        // The server's description already names the mechanic ("… (Bargained)"); only fall back to
        // a bare cast label when it's missing.
        return action.description || `Cast (${action.manaCostString ?? ''})`
      case 'TurnFaceUp':
        return `Turn Face-Up (${action.manaCostString ?? ''})`
      default:
        return action.description
    }
  }

  return (
    <button
      onClick={onClick}
      className={`${styles.fallbackActionButton} ${getActionColorClass()}`}
    >
      <div className={styles.fallbackActionLabel}>
        <AbilityText text={getActionLabel()} size={14} />
      </div>
    </button>
  )
}

/**
 * Card image display for the action menu.
 * Shows a large card image with loading state and error fallback.
 */
function CardImage({
  imageUrl,
  cardName,
  rotateDeg = 0,
}: {
  imageUrl: string
  cardName: string
  rotateDeg?: 0 | 90 | 180 | 270
}) {
  const [imageLoaded, setImageLoaded] = useState(false)
  const [imageError, setImageError] = useState(false)

  // For sideways-printed layouts (Rooms), swap the container to landscape and absolutely
  // position the image at its original portrait dimensions, rotated to fit. Same approach
  // as HoverCardPreview's `imageRotateDeg`. Inline styles override the CSS-module dims.
  const isLandscape = rotateDeg === 90 || rotateDeg === 270
  const containerStyle: React.CSSProperties = isLandscape
    ? { width: 279, height: 200 }
    : {}
  const imageStyle: React.CSSProperties = rotateDeg
    ? {
        position: 'absolute',
        top: '50%',
        left: '50%',
        width: 200,
        height: 279,
        transform: `translate(-50%, -50%) rotate(${rotateDeg}deg)`,
        objectFit: 'cover',
      }
    : {}

  return (
    <div className={styles.cardImageContainer} style={containerStyle}>
      {!imageLoaded && !imageError && (
        <div className={styles.loadingIndicator}>
          Loading...
        </div>
      )}
      {imageError ? (
        <div className={styles.errorFallback}>
          {cardName}
        </div>
      ) : (
        <img
          src={imageUrl}
          alt={cardName}
          className={`${styles.cardImage} ${imageLoaded ? styles.cardImageLoaded : styles.cardImageLoading}`}
          style={imageStyle}
          onLoad={() => setImageLoaded(true)}
          onError={() => setImageError(true)}
        />
      )}
    </div>
  )
}
