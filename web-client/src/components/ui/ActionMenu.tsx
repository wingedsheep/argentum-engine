import { useEffect, useCallback, useMemo, useState } from 'react'
import { useGameStore } from '@/store/gameStore.ts'
import { useCardActions } from '@/hooks/useLegalActions.ts'
import { useInteraction } from '@/hooks/useInteraction.ts'
import type { LegalActionInfo, ClientCard } from '@/types'
import { ManaCost, AbilityText } from './ManaSymbols'
import { ManaCostProgress } from './ManaCostProgress'
import { useViewingPlayer } from '@/store/selectors.ts'
import { isManaPoolEmpty } from '@/types'
import { getCardImageUrl } from '@/utils/cardImages.ts'
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
  actionType: 'cast' | 'castFaceDown' | 'castWithKicker' | 'cycle' | 'playLand' | 'activate' | 'turnFaceUp'
  /**
   * Signed loyalty change for planeswalker loyalty abilities (+1, -2, -8, 0).
   * When present, the button renders a mana-font loyalty icon instead of a text prefix.
   */
  loyaltyChange?: number
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
  const castActions = legalActions.filter(
    (a) => a.action.type === 'CastSpell' && a.actionType !== 'CastFaceDown' && a.actionType !== 'CastWithKicker'
  )
  const castAction = castActions[0] ?? null
  const kickerAction = legalActions.find((a) => a.actionType === 'CastWithKicker')
  const morphAction = legalActions.find((a) => a.actionType === 'CastFaceDown')
  const cycleAction = legalActions.find((a) => a.action.type === 'CycleCard')
  const typecycleAction = legalActions.find((a) => a.action.type === 'TypecycleCard')
  const playLandAction = legalActions.find((a) => a.action.type === 'PlayLand')

  // Debug: log found actions
  if (import.meta.env.DEV) {
    console.log('buildActionOptions - found:', { castAction: !!castAction, castActions: castActions.length, morphAction: !!morphAction, cycleAction: !!cycleAction, playLandAction: !!playLandAction })
    // Extra debug for cycling
    legalActions.forEach((a, i) => {
      console.log(`  action[${i}]: action.type=${a.action.type}, actionType=${a.actionType}, isCycleCard=${a.action.type === 'CycleCard'}`)
    })
  }

  // 1. Modal spell modes — show one button per mode instead of a single "Cast" button
  const modeActions = legalActions.filter((a) => a.actionType === 'CastSpellMode')
  if (modeActions.length > 0) {
    modeActions.forEach((modeAction, index) => {
      options.push({
        key: `mode-${index}`,
        label: modeAction.description,
        manaCost: modeAction.manaCostString || cardInfo.manaCost || null,
        isAvailable: modeAction.isAffordable !== false,
        action: modeAction,
        actionType: 'cast',
      })
    })
  } else if (castActions.length > 1) {
    // 1b. Multiple cast options (e.g., BlightOrPay — blight path vs pay path)
    castActions.forEach((ca, index) => {
      options.push({
        key: `cast-${index}`,
        label: ca.description,
        manaCost: ca.manaCostString || cardInfo.manaCost || null,
        isAvailable: ca.isAffordable !== false,
        action: ca,
        actionType: 'cast',
      })
    })
  } else if (castAction) {
    // 1c. Normal cast (for non-land, non-modal cards)
    options.push({
      key: 'cast',
      label: `Cast ${cardInfo.name}`,
      manaCost: castAction.manaCostString || cardInfo.manaCost || null,
      isAvailable: castAction.isAffordable !== false, // default true if not set
      action: castAction,
      actionType: 'cast',
    })
  } else if ((cycleAction || typecycleAction) && !cardInfo.cardTypes.includes('LAND')) {
    // Non-land card with cycling but no CastSpell action — show grayed-out cast option
    // so the action menu always presents both choices
    options.push({
      key: 'cast',
      label: `Cast ${cardInfo.name}`,
      manaCost: cardInfo.manaCost || null,
      isAvailable: false,
      action: null,
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

  // 3b. Cast with kicker
  if (kickerAction) {
    options.push({
      key: 'castWithKicker',
      label: `Cast ${cardInfo.name} (Kicked)`,
      manaCost: kickerAction.manaCostString || null,
      isAvailable: kickerAction.isAffordable !== false,
      action: kickerAction,
      actionType: 'castWithKicker',
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

  // 4b. Typecycling (e.g., Islandcycling, Swampcycling)
  if (typecycleAction) {
    options.push({
      key: 'typecycle',
      label: typecycleAction.description,
      manaCost: typecycleAction.manaCostString || null,
      isAvailable: typecycleAction.isAffordable !== false,
      action: typecycleAction,
      actionType: 'cycle',
    })
  }

  // 5. Activated abilities (for permanents on battlefield)
  const activateActions = legalActions.filter((a) => a.action.type === 'ActivateAbility')

  // 5a. Planeswalker: show the full loyalty ability menu, with unavailable abilities grayed
  // out (not enough loyalty, sorcery-speed restriction, already activated this turn, etc.).
  // This overrides the default "only show legal activate actions" rendering so the player
  // sees all three abilities on the card every time they click it.
  const pwAbilities = cardInfo.planeswalkerAbilities
  const renderedActivateActions = new Set<LegalActionInfo>()
  if (pwAbilities && pwAbilities.length > 0) {
    pwAbilities.forEach((pw, index) => {
      const match = activateActions.find(
        (a) => (a.action as { abilityId?: string }).abilityId === pw.abilityId
      )
      if (match) renderedActivateActions.add(match)
      options.push({
        key: `pw-${pw.abilityId}-${index}`,
        label: pw.description,
        manaCost: null,
        isAvailable: match !== undefined && match.isAffordable !== false,
        action: match ?? null,
        actionType: 'activate',
        loyaltyChange: pw.loyaltyChange,
      })
    })
  }

  // 5b. Remaining (non-planeswalker) activated abilities — activated abilities on non-
  // planeswalker permanents, or anything not already rendered above.
  activateActions.forEach((activateAction, index) => {
    if (renderedActivateActions.has(activateAction)) return
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

  // 7. Crew (for Vehicles)
  const crewActions = legalActions.filter((a) => a.action.type === 'CrewVehicle')
  crewActions.forEach((crewAction, index) => {
    options.push({
      key: `crew-${index}`,
      label: crewAction.description,
      manaCost: null,
      isAvailable: crewAction.isAffordable !== false,
      action: crewAction,
      actionType: 'activate',
    })
  })

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
  const hasNumberVariant = direction !== 'zero' && magnitude >= 1 && magnitude <= 20
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
  // Show mana selection icon for actions that have mana sources available
  // Delve spells handle mana selection after the delve selector, so don't show the icon
  const hasManaSelection = option.isAvailable && option.action?.availableManaSources != null && option.action.availableManaSources.length > 0 && !option.action.hasDelve

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
          <span className={styles.actionButtonLabel}>
            <AbilityText text={option.label} size={14} />
          </span>
        </span>
        {showSeparateCost && (
          hasFloatingMana && manaPool
            ? <ManaCostProgress cost={option.manaCost} manaPool={manaPool} size={16} gap={2} />
            : <ManaCost cost={option.manaCost} size={16} gap={2} />
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
        return `Cast (Kicked) (${action.manaCostString ?? ''})`
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
