import { useEffect, useCallback, useMemo, useState } from 'react'
import { useGameStore } from '../../store/gameStore'
import { useCardActions } from '../../hooks/useLegalActions'
import { useInteraction } from '../../hooks/useInteraction'
import type { LegalActionInfo, ClientCard } from '../../types'
import { ManaCost, AbilityText } from './ManaSymbols'
import { getCardImageUrl } from '../../utils/cardImages'
import styles from './ActionMenu.module.css'

/**
 * Represents an action option in the modal (either available or unavailable).
 */
interface ActionOption {
  /** Unique key for React */
  key: string
  /** Display label */
  label: string
  /** Mana cost to display */
  manaCost: string | null
  /** Whether this action is available (affordable) */
  isAvailable: boolean
  /** The legal action info if available */
  action: LegalActionInfo | null
  /** Action type for coloring */
  actionType: 'cast' | 'castFaceDown' | 'cycle' | 'playLand' | 'activate' | 'turnFaceUp'
}

/**
 * Build all potential action options for a card from server-sent legal actions.
 * The server now sends ALL potential actions with isAffordable flags.
 */
function buildActionOptions(
  cardInfo: ClientCard | null,
  legalActions: LegalActionInfo[]
): ActionOption[] {
  const options: ActionOption[] = []
  if (!cardInfo) return options

  // Debug: log all action types received
  if (import.meta.env.DEV) {
    console.log('buildActionOptions - legalActions:', legalActions.map(a => ({
      actionType: a.actionType,
      'action.type': a.action.type,
      description: a.description,
      isAffordable: a.isAffordable
    })))
  }

  // Find each type of action - server sends all potential actions with isAffordable flag
  const castAction = legalActions.find(
    (a) => a.action.type === 'CastSpell' && a.actionType !== 'CastFaceDown'
  )
  const morphAction = legalActions.find((a) => a.actionType === 'CastFaceDown')
  const cycleAction = legalActions.find((a) => a.action.type === 'CycleCard')
  const playLandAction = legalActions.find((a) => a.action.type === 'PlayLand')

  // Debug: log found actions
  if (import.meta.env.DEV) {
    console.log('buildActionOptions - found:', { castAction: !!castAction, morphAction: !!morphAction, cycleAction: !!cycleAction, playLandAction: !!playLandAction })
    // Extra debug for cycling
    legalActions.forEach((a, i) => {
      console.log(`  action[${i}]: action.type=${a.action.type}, actionType=${a.actionType}, isCycleCard=${a.action.type === 'CycleCard'}`)
    })
  }

  // 1. Normal cast (for non-land cards)
  if (castAction) {
    options.push({
      key: 'cast',
      label: `Cast ${cardInfo.name}`,
      manaCost: castAction.manaCostString || cardInfo.manaCost || null,
      isAvailable: castAction.isAffordable !== false, // default true if not set
      action: castAction,
      actionType: 'cast',
    })
  }

  // 2. Play land (for land cards)
  if (playLandAction) {
    options.push({
      key: 'playLand',
      label: `Play ${cardInfo.name}`,
      manaCost: null,
      isAvailable: playLandAction.isAffordable !== false,
      action: playLandAction,
      actionType: 'playLand',
    })
  } else if (cycleAction && cardInfo.cardTypes.includes('LAND')) {
    // Land with cycling but no PlayLand action (already played a land this turn)
    // Show grayed-out "Play land" so the action menu always has both options
    options.push({
      key: 'playLand',
      label: `Play ${cardInfo.name}`,
      manaCost: null,
      isAvailable: false,
      action: null,
      actionType: 'playLand',
    })
  }

  // 3. Cast face-down (morph)
  if (morphAction) {
    options.push({
      key: 'castFaceDown',
      label: 'Cast Face-Down',
      manaCost: morphAction.manaCostString || '{3}',
      isAvailable: morphAction.isAffordable !== false,
      action: morphAction,
      actionType: 'castFaceDown',
    })
  }

  // 4. Cycling
  if (cycleAction) {
    options.push({
      key: 'cycle',
      label: 'Cycle',
      manaCost: cycleAction.manaCostString || null,
      isAvailable: cycleAction.isAffordable !== false,
      action: cycleAction,
      actionType: 'cycle',
    })
  }

  // 5. Activated abilities (for permanents on battlefield)
  const activateActions = legalActions.filter((a) => a.action.type === 'ActivateAbility')
  activateActions.forEach((activateAction, index) => {
    options.push({
      key: `activate-${index}`,
      label: activateAction.description,
      manaCost: activateAction.manaCostString || null,
      isAvailable: activateAction.isAffordable !== false,
      action: activateAction,
      actionType: 'activate',
    })
  })

  // 6. Turn face-up (morph)
  const turnFaceUpAction = legalActions.find((a) => a.action.type === 'TurnFaceUp')
  if (turnFaceUpAction) {
    options.push({
      key: 'turnFaceUp',
      label: 'Turn Face-Up',
      manaCost: turnFaceUpAction.manaCostString || null,
      isAvailable: turnFaceUpAction.isAffordable !== false,
      action: turnFaceUpAction,
      actionType: 'turnFaceUp',
    })
  }

  return options
}

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
          {/* Large card image */}
          {cardImageUrl && (
            <CardImage imageUrl={cardImageUrl} cardName={cardInfo?.name ?? 'Card'} />
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
    case 'cycle':
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
  const styleClass = getActionStyleClass(option.actionType, option.isAvailable)
  // Only show separate mana cost if label doesn't already contain mana symbols
  const showSeparateCost = option.manaCost && !option.label.includes('{')

  return (
    <button
      onClick={onClick}
      disabled={!option.isAvailable}
      className={`${styles.actionButton} ${styleClass}`}
    >
      <span className={styles.actionButtonLabel}>
        <AbilityText text={option.label} size={14} />
      </span>
      {showSeparateCost && (
        <ManaCost cost={option.manaCost} size={16} gap={2} />
      )}
    </button>
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
    // Handle morph-specific action types first (these have actionType different from action.type)
    if (action.actionType === 'CastFaceDown') {
      return styles.fallbackCastFaceDown
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
}: {
  imageUrl: string
  cardName: string
}) {
  const [imageLoaded, setImageLoaded] = useState(false)
  const [imageError, setImageError] = useState(false)

  return (
    <div className={styles.cardImageContainer}>
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
          onLoad={() => setImageLoaded(true)}
          onError={() => setImageError(true)}
        />
      )}
    </div>
  )
}
