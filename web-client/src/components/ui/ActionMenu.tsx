import { useGameStore } from '../../store/gameStore'
import { useCardActions } from '../../hooks/useLegalActions'
import { useInteraction, useCanPassPriority } from '../../hooks/useInteraction'
import { useStackCards } from '../../store/selectors'
import type { LegalActionInfo } from '../../types'
import { AbilityText } from './ManaSymbols'

/**
 * Action menu displayed when a card with multiple actions is selected.
 */
export function ActionMenu() {
  const selectedCardId = useGameStore((state) => state.selectedCardId)
  const cardActions = useCardActions(selectedCardId)
  const { executeAction, cancelAction, passPriority } = useInteraction()
  const canPassPriority = useCanPassPriority()

  // Debug logging
  if (import.meta.env.DEV && selectedCardId) {
    console.log('ActionMenu - selectedCardId:', selectedCardId, 'cardActions:', cardActions)
  }

  // Don't show if no card selected or no actions
  if (!selectedCardId || cardActions.length === 0) {
    // Show pass priority button when appropriate
    if (canPassPriority && !selectedCardId) {
      return <PassPriorityButton onClick={passPriority} />
    }
    return null
  }

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
    switch (action.action.type) {
      case 'PlayLand':
        return '#00aa00'
      case 'CastSpell':
        return '#0066cc'
      case 'CycleCard':
        return '#8855aa' // Purple for cycling
      case 'ActivateAbility':
        return '#886600'
      case 'PassPriority':
        return '#888888'
      default:
        return '#666666'
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
      <div style={{ fontWeight: 500 }}>{action.actionType}</div>
      <div style={{ fontSize: 12, opacity: 0.8, display: 'flex', alignItems: 'center' }}>
        <AbilityText text={action.description} size={14} />
      </div>
    </button>
  )
}

/**
 * Pass priority button shown when player has priority.
 * Shows "Resolve" with orange color when there's something on the stack,
 * "Pass" with gray color otherwise (like MTG Arena).
 */
function PassPriorityButton({ onClick }: { onClick: () => void }) {
  const stackCards = useStackCards()
  const hasStackItems = stackCards.length > 0
  const combatState = useGameStore((state) => state.combatState)
  const attackWithAll = useGameStore((state) => state.attackWithAll)

  // Check if we can attack with all
  const canAttackWithAll =
    combatState?.mode === 'declareAttackers' && combatState.validCreatures.length > 0

  // Different styling based on whether we're resolving stack or passing phase
  const backgroundColor = hasStackItems ? '#c76e00' : '#444'
  const hoverBackgroundColor = hasStackItems ? '#e08000' : '#555'
  const borderColor = hasStackItems ? '#e08000' : '#666'
  const hoverBorderColor = hasStackItems ? '#ff9500' : '#888'
  const label = hasStackItems ? 'Resolve' : 'Pass'
  const icon = hasStackItems ? '✓' : '⏭️'

  return (
    <div
      style={{
        position: 'absolute',
        bottom: 100,
        right: 20,
        pointerEvents: 'auto',
        display: 'flex',
        gap: 8,
      }}
    >
      {canAttackWithAll && (
        <button
          onClick={attackWithAll}
          style={{
            padding: '12px 24px',
            backgroundColor: '#c0392b',
            color: 'white',
            border: '2px solid #e74c3c',
            borderRadius: 8,
            cursor: 'pointer',
            fontSize: 14,
            fontWeight: 500,
            transition: 'all 0.15s',
          }}
          onMouseOver={(e) => {
            e.currentTarget.style.backgroundColor = '#e74c3c'
            e.currentTarget.style.borderColor = '#ff6b6b'
          }}
          onMouseOut={(e) => {
            e.currentTarget.style.backgroundColor = '#c0392b'
            e.currentTarget.style.borderColor = '#e74c3c'
          }}
        >
          Attack with All
        </button>
      )}
      <button
        onClick={onClick}
        style={{
          padding: '12px 24px',
          backgroundColor,
          color: 'white',
          border: `2px solid ${borderColor}`,
          borderRadius: 8,
          cursor: 'pointer',
          fontSize: 14,
          fontWeight: 500,
          transition: 'all 0.15s',
        }}
        onMouseOver={(e) => {
          e.currentTarget.style.backgroundColor = hoverBackgroundColor
          e.currentTarget.style.borderColor = hoverBorderColor
        }}
        onMouseOut={(e) => {
          e.currentTarget.style.backgroundColor = backgroundColor
          e.currentTarget.style.borderColor = borderColor
        }}
      >
        {label}
        <span style={{ marginLeft: 8, opacity: 0.8 }}>{icon}</span>
      </button>
    </div>
  )
}
