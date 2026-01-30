import { useGameStore, type CombatState } from '../../store/gameStore'
import { useResponsive } from '../../hooks/useResponsive'
import type { EntityId } from '../../types'

/**
 * Combat overlay UI for declaring attackers and blockers.
 *
 * This overlay REPLACES the normal Pass button during combat.
 * All combat actions are submitted through this overlay.
 */
export function CombatOverlay() {
  const combatState = useGameStore((state) => state.combatState)
  const draggingBlockerId = useGameStore((state) => state.draggingBlockerId)
  const confirmCombat = useGameStore((state) => state.confirmCombat)
  const cancelCombat = useGameStore((state) => state.cancelCombat)
  const responsive = useResponsive()

  if (!combatState) return null

  const isAttacking = combatState.mode === 'declareAttackers'
  const hasValidCreatures = combatState.validCreatures.length > 0
  const hasAttackers = combatState.attackingCreatures.length > 0
  const hasMustBeBlocked = combatState.mustBeBlockedAttackers.length > 0

  // Count selections
  const numAttackers = combatState.selectedAttackers.length
  const numBlockers = Object.keys(combatState.blockerAssignments).length

  // Dynamic title
  const title = isAttacking ? 'Attackers' : 'Blockers'

  // Dynamic instructions (compact)
  const getInstructions = () => {
    if (isAttacking) {
      if (!hasValidCreatures) return 'None available'
      if (numAttackers === 0) return 'Click to select'
      return `${numAttackers} selected`
    } else {
      if (!hasValidCreatures || !hasAttackers) return 'None available'
      if (draggingBlockerId) return 'Drop on attacker'
      if (numBlockers === 0) return 'Drag to block'
      return `${numBlockers} assigned`
    }
  }

  // Button labels (compact)
  const confirmLabel = isAttacking
    ? (numAttackers > 0 ? `Attack (${numAttackers})` : 'Attack')
    : (numBlockers > 0 ? `Block (${numBlockers})` : 'Confirm')

  const skipLabel = isAttacking ? 'Skip' : 'Skip'

  // Button states
  const canConfirm = isAttacking ? numAttackers > 0 : numBlockers > 0
  const showSkipAsMain = !canConfirm

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
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        gap: 8,
        pointerEvents: 'auto',
        boxShadow: '0 4px 24px rgba(0, 0, 0, 0.6)',
        border: `2px solid ${isAttacking ? '#e74c3c' : '#3498db'}`,
        zIndex: 1001,
      }}
    >
      {/* Title */}
      <div
        style={{
          color: isAttacking ? '#e74c3c' : '#3498db',
          fontSize: responsive.fontSize.normal,
          fontWeight: 700,
          textTransform: 'uppercase',
          letterSpacing: 1,
        }}
      >
        {title}
      </div>

      {/* Instructions */}
      <div
        style={{
          color: draggingBlockerId ? '#88ccff' : '#888',
          fontSize: responsive.fontSize.small,
          textAlign: 'center',
        }}
      >
        {getInstructions()}
      </div>

      {/* Must-be-blocked warning */}
      {!isAttacking && hasMustBeBlocked && (
        <div
          style={{
            backgroundColor: 'rgba(231, 76, 60, 0.2)',
            border: '1px solid #e74c3c',
            borderRadius: 4,
            padding: '4px 8px',
            color: '#ff9999',
            fontSize: 11,
          }}
        >
          Must block if able
        </div>
      )}

      {/* Buttons */}
      <div style={{ display: 'flex', gap: 8 }}>
        <button
          onClick={cancelCombat}
          style={{
            padding: '8px 14px',
            fontSize: responsive.fontSize.small,
            fontWeight: showSkipAsMain ? 600 : 400,
            backgroundColor: showSkipAsMain ? (isAttacking ? '#c0392b' : '#2980b9') : '#444',
            color: 'white',
            border: 'none',
            borderRadius: 6,
            cursor: 'pointer',
          }}
        >
          {skipLabel}
        </button>

        {canConfirm && (
          <button
            onClick={confirmCombat}
            style={{
              padding: '8px 14px',
              fontSize: responsive.fontSize.small,
              fontWeight: 600,
              backgroundColor: isAttacking ? '#c0392b' : '#2980b9',
              color: 'white',
              border: 'none',
              borderRadius: 6,
              cursor: 'pointer',
            }}
          >
            {confirmLabel}
          </button>
        )}
      </div>
    </div>
  )
}

/**
 * Hook to check if a creature is selected as an attacker.
 */
export function useIsAttacking(creatureId: EntityId): boolean {
  const combatState = useGameStore((state) => state.combatState)
  if (!combatState || combatState.mode !== 'declareAttackers') return false
  return combatState.selectedAttackers.includes(creatureId)
}

/**
 * Hook to check if a creature is assigned as a blocker.
 */
export function useIsBlocking(creatureId: EntityId): EntityId | null {
  const combatState = useGameStore((state) => state.combatState)
  if (!combatState || combatState.mode !== 'declareBlockers') return null
  for (const [blockerId, attackerId] of Object.entries(combatState.blockerAssignments)) {
    if (blockerId === (creatureId as string)) {
      return attackerId
    }
  }
  return null
}

/**
 * Hook to check if we're in combat mode.
 */
export function useCombatMode(): CombatState | null {
  return useGameStore((state) => state.combatState)
}
