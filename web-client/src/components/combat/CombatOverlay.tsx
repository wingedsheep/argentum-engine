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
  const title = isAttacking ? 'Declare Attackers' : 'Declare Blockers'

  // Dynamic instructions
  const getInstructions = () => {
    if (isAttacking) {
      if (!hasValidCreatures) {
        return 'No creatures can attack this turn'
      }
      if (numAttackers === 0) {
        return 'Click your creatures to select attackers'
      }
      return `${numAttackers} creature${numAttackers > 1 ? 's' : ''} attacking`
    } else {
      if (!hasValidCreatures || !hasAttackers) {
        return 'No blocks possible'
      }
      if (draggingBlockerId) {
        return 'Drop on an attacker to assign block'
      }
      if (numBlockers === 0) {
        return 'Drag your creatures onto attackers to block'
      }
      return `${numBlockers} blocker${numBlockers > 1 ? 's' : ''} assigned`
    }
  }

  // Button labels
  const confirmLabel = isAttacking
    ? (numAttackers > 0 ? `Attack with ${numAttackers}` : 'Attack')
    : (numBlockers > 0 ? `Block with ${numBlockers}` : 'Confirm Blocks')

  const skipLabel = isAttacking ? "Don't Attack" : "No Blocks"

  // Button states
  const canConfirm = isAttacking ? numAttackers > 0 : numBlockers > 0
  const showSkipAsMain = !canConfirm

  const spacing = responsive.isMobile ? 8 : 12

  return (
    <div
      style={{
        position: 'fixed',
        top: responsive.isMobile ? 60 : 80,
        left: '50%',
        transform: 'translateX(-50%)',
        backgroundColor: 'rgba(0, 0, 0, 0.95)',
        padding: responsive.isMobile ? '16px 20px' : '20px 28px',
        borderRadius: 12,
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        gap: spacing,
        pointerEvents: 'auto',
        boxShadow: '0 4px 24px rgba(0, 0, 0, 0.6)',
        border: `2px solid ${isAttacking ? '#e74c3c' : '#3498db'}`,
        minWidth: responsive.isMobile ? 220 : 300,
        zIndex: 1001,
      }}
    >
      {/* Title */}
      <h3
        style={{
          margin: 0,
          color: isAttacking ? '#e74c3c' : '#3498db',
          fontSize: responsive.fontSize.large,
          fontWeight: 700,
          textTransform: 'uppercase',
          letterSpacing: 1,
        }}
      >
        {title}
      </h3>

      {/* Instructions */}
      <p
        style={{
          margin: 0,
          color: draggingBlockerId ? '#88ccff' : '#aaa',
          fontSize: responsive.fontSize.normal,
          textAlign: 'center',
          minHeight: 20,
        }}
      >
        {getInstructions()}
      </p>

      {/* Must-be-blocked warning */}
      {!isAttacking && hasMustBeBlocked && (
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: 8,
            backgroundColor: 'rgba(231, 76, 60, 0.2)',
            border: '1px solid #e74c3c',
            borderRadius: 6,
            padding: '8px 12px',
            color: '#ff9999',
            fontSize: responsive.fontSize.small,
          }}
        >
          <span style={{ fontSize: 16 }}>⚠️</span>
          <span>
            {combatState.mustBeBlockedAttackers.length === 1
              ? 'Your creatures must block the highlighted attacker if able'
              : 'Your creatures must block one of the highlighted attackers if able'}
          </span>
        </div>
      )}

      {/* Drag hint for blockers */}
      {!isAttacking && hasValidCreatures && hasAttackers && !draggingBlockerId && numBlockers === 0 && !hasMustBeBlocked && (
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: 8,
            color: '#666',
            fontSize: responsive.fontSize.small,
          }}
        >
          <span style={{ fontSize: 16 }}>👆</span>
          <span>Click and drag from your creature</span>
        </div>
      )}

      {/* Buttons */}
      <div style={{ display: 'flex', gap: spacing, marginTop: 4 }}>
        {/* Skip/No action button */}
        <button
          onClick={cancelCombat}
          style={{
            padding: responsive.isMobile ? '10px 16px' : '12px 20px',
            fontSize: responsive.fontSize.normal,
            fontWeight: showSkipAsMain ? 600 : 400,
            backgroundColor: showSkipAsMain ? (isAttacking ? '#c0392b' : '#2980b9') : '#444',
            color: 'white',
            border: 'none',
            borderRadius: 8,
            cursor: 'pointer',
            transition: 'background-color 0.15s',
          }}
        >
          {skipLabel}
        </button>

        {/* Confirm button - only show if there are selections */}
        {canConfirm && (
          <button
            onClick={confirmCombat}
            style={{
              padding: responsive.isMobile ? '10px 16px' : '12px 20px',
              fontSize: responsive.fontSize.normal,
              fontWeight: 600,
              backgroundColor: isAttacking ? '#c0392b' : '#2980b9',
              color: 'white',
              border: 'none',
              borderRadius: 8,
              cursor: 'pointer',
              transition: 'background-color 0.15s',
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
