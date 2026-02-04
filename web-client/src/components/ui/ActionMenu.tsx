import { useEffect, useCallback, useMemo, useState } from 'react'
import { useGameStore } from '../../store/gameStore'
import { useCardActions } from '../../hooks/useLegalActions'
import { useInteraction } from '../../hooks/useInteraction'
import type { LegalActionInfo, ClientCard } from '../../types'
import { ManaCost, AbilityText } from './ManaSymbols'
import { getCardImageUrl } from '../../utils/cardImages'

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
      <div
        style={{
          position: 'fixed',
          bottom: 140,
          left: '50%',
          transform: 'translateX(-50%)',
          zIndex: 1000,
          pointerEvents: 'auto',
        }}
      >
        <div
          style={{
            backgroundColor: 'rgba(20, 20, 28, 0.95)',
            borderRadius: 12,
            padding: 16,
            minWidth: 220,
            maxWidth: 320,
            boxShadow: '0 8px 32px rgba(0, 0, 0, 0.6), 0 0 0 1px rgba(255, 255, 255, 0.1)',
            backdropFilter: 'blur(8px)',
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
          }}
        >
          {/* Large card image */}
          {cardImageUrl && (
            <CardImage imageUrl={cardImageUrl} cardName={cardInfo?.name ?? 'Card'} />
          )}

          {/* Action buttons */}
          <div
            style={{
              display: 'flex',
              flexDirection: 'column',
              gap: 8,
              width: '100%',
            }}
          >
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
          <button
            onClick={cancelAction}
            style={{
              marginTop: 12,
              width: '100%',
              padding: '8px 16px',
              backgroundColor: 'transparent',
              color: '#666',
              border: '1px solid #333',
              borderRadius: 6,
              cursor: 'pointer',
              fontSize: 12,
              transition: 'all 0.15s',
            }}
            onMouseOver={(e) => {
              e.currentTarget.style.backgroundColor = '#333'
              e.currentTarget.style.color = '#aaa'
            }}
            onMouseOut={(e) => {
              e.currentTarget.style.backgroundColor = 'transparent'
              e.currentTarget.style.color = '#666'
            }}
          >
            Cancel (Esc)
          </button>
        </div>
      </div>
    )
  }

  // Fallback (shouldn't reach here normally)
  return (
    <div
      style={{
        position: 'absolute',
        bottom: 120,
        left: '50%',
        transform: 'translateX(-50%)',
        backgroundColor: 'rgba(0, 0, 0, 0.9)',
        borderRadius: 8,
        padding: 16,
        display: 'flex',
        flexDirection: 'column',
        gap: 8,
        minWidth: 200,
        pointerEvents: 'auto',
      }}
    >
      <span style={{ color: '#888', fontSize: 12, marginBottom: 4 }}>
        Choose action:
      </span>

      {cardActions.map((action, index) => (
        <ActionButton
          key={index}
          action={action}
          onClick={() => executeAction(action)}
        />
      ))}

      <button
        onClick={cancelAction}
        style={{
          padding: '8px 16px',
          backgroundColor: '#333',
          color: '#888',
          border: 'none',
          borderRadius: 4,
          cursor: 'pointer',
          marginTop: 4,
        }}
      >
        Cancel
      </button>
    </div>
  )
}

/**
 * Individual action button.
 */
function ActionButton({
  action,
  onClick,
}: {
  action: LegalActionInfo
  onClick: () => void
}) {
  const getActionColor = () => {
    // Handle morph-specific action types first (these have actionType different from action.type)
    if (action.actionType === 'CastFaceDown') {
      return '#555599' // Purple-blue for morph face-down
    }
    if (action.actionType === 'TurnFaceUp') {
      return '#996633' // Brown-gold for turning face-up
    }

    switch (action.action.type) {
      case 'PlayLand':
        return '#00aa00'
      case 'CastSpell':
        return '#0066cc'
      case 'CycleCard':
        return '#8855aa' // Purple for cycling
      case 'ActivateAbility':
        return '#886600'
      case 'TurnFaceUp':
        return '#996633' // Brown-gold for turning face-up
      case 'PassPriority':
        return '#888888'
      default:
        return '#666666'
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
      style={{
        padding: '10px 16px',
        backgroundColor: getActionColor(),
        color: 'white',
        border: 'none',
        borderRadius: 4,
        cursor: 'pointer',
        textAlign: 'left',
        transition: 'background-color 0.15s',
      }}
      onMouseOver={(e) => {
        e.currentTarget.style.filter = 'brightness(1.2)'
      }}
      onMouseOut={(e) => {
        e.currentTarget.style.filter = 'brightness(1)'
      }}
    >
      <div style={{ fontWeight: 500, display: 'flex', alignItems: 'center' }}>
        <AbilityText text={getActionLabel()} size={14} />
      </div>
    </button>
  )
}

/**
 * Get gradient colors for action type.
 */
function getActionGradient(
  actionType: ActionOption['actionType'],
  isAvailable: boolean
): string {
  if (!isAvailable) {
    return 'linear-gradient(135deg, #252528 0%, #1a1a1e 100%)'
  }
  switch (actionType) {
    case 'cast':
      return 'linear-gradient(135deg, #2563eb 0%, #1e40af 100%)'
    case 'castFaceDown':
      return 'linear-gradient(135deg, #7c3aed 0%, #5b21b6 100%)'
    case 'cycle':
      return 'linear-gradient(135deg, #9333ea 0%, #6b21a8 100%)'
    case 'playLand':
      return 'linear-gradient(135deg, #16a34a 0%, #15803d 100%)'
    case 'activate':
      return 'linear-gradient(135deg, #d97706 0%, #b45309 100%)' // Orange/amber for abilities
    case 'turnFaceUp':
      return 'linear-gradient(135deg, #be185d 0%, #9d174d 100%)' // Pink for morph reveal
    default:
      return 'linear-gradient(135deg, #444 0%, #333 100%)'
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
  const gradient = getActionGradient(option.actionType, option.isAvailable)
  // Only show separate mana cost if label doesn't already contain mana symbols
  const showSeparateCost = option.manaCost && !option.label.includes('{')

  return (
    <button
      onClick={onClick}
      disabled={!option.isAvailable}
      style={{
        padding: '8px 12px',
        background: gradient,
        color: option.isAvailable ? 'white' : '#555',
        border: option.isAvailable
          ? '1px solid rgba(255, 255, 255, 0.15)'
          : '1px solid #333',
        borderRadius: 6,
        cursor: option.isAvailable ? 'pointer' : 'not-allowed',
        textAlign: 'left',
        transition: 'all 0.15s ease',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        gap: 8,
      }}
      onMouseOver={(e) => {
        if (option.isAvailable) {
          e.currentTarget.style.filter = 'brightness(1.1)'
          e.currentTarget.style.borderColor = 'rgba(255, 255, 255, 0.25)'
        }
      }}
      onMouseOut={(e) => {
        e.currentTarget.style.filter = 'brightness(1)'
        e.currentTarget.style.borderColor = option.isAvailable
          ? 'rgba(255, 255, 255, 0.15)'
          : '#333'
      }}
    >
      <span style={{ fontWeight: 500, fontSize: 13 }}>
        <AbilityText text={option.label} size={14} />
      </span>
      {showSeparateCost && (
        <ManaCost cost={option.manaCost} size={16} gap={2} />
      )}
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
    <div
      style={{
        width: 200,
        height: 279, // Standard MTG card aspect ratio (63mm x 88mm ≈ 1:1.395)
        marginBottom: 12,
        borderRadius: 8,
        overflow: 'hidden',
        backgroundColor: '#1a1a1e',
        position: 'relative',
      }}
    >
      {!imageLoaded && !imageError && (
        <div
          style={{
            position: 'absolute',
            inset: 0,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: '#666',
            fontSize: 12,
          }}
        >
          Loading...
        </div>
      )}
      {imageError ? (
        <div
          style={{
            position: 'absolute',
            inset: 0,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: '#fbbf24',
            fontSize: 14,
            fontWeight: 600,
            textAlign: 'center',
            padding: 16,
          }}
        >
          {cardName}
        </div>
      ) : (
        <img
          src={imageUrl}
          alt={cardName}
          style={{
            width: '100%',
            height: '100%',
            objectFit: 'cover',
            opacity: imageLoaded ? 1 : 0,
            transition: 'opacity 0.2s ease',
          }}
          onLoad={() => setImageLoaded(true)}
          onError={() => setImageError(true)}
        />
      )}
    </div>
  )
}

