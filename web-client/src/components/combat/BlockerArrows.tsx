import { useGameStore } from '../../store/gameStore'
import type { EntityId } from '../../types'
import { entityId } from '../../types'

/**
 * SVG overlay for drawing blocker assignment arrows.
 */
export function BlockerArrows() {
  const combatState = useGameStore((state) => state.combatState)

  // Only render in blockers mode
  if (!combatState || combatState.mode !== 'declareBlockers') return null

  return (
    <svg
      style={{
        position: 'absolute',
        top: 0,
        left: 0,
        width: '100%',
        height: '100%',
        pointerEvents: 'none',
        zIndex: 100,
      }}
    >
      <defs>
        <marker
          id="arrowhead"
          markerWidth="10"
          markerHeight="7"
          refX="9"
          refY="3.5"
          orient="auto"
        >
          <polygon points="0 0, 10 3.5, 0 7" fill="#3498db" />
        </marker>
      </defs>

      {/* Draw arrows for existing blocker assignments */}
      {Object.entries(combatState.blockerAssignments).map(([blockerIdStr, attackerId]) => (
        <BlockerArrow
          key={blockerIdStr}
          blockerId={entityId(blockerIdStr)}
          attackerId={attackerId}
        />
      ))}
    </svg>
  )
}

interface BlockerArrowProps {
  blockerId: EntityId
  attackerId: EntityId
}

/**
 * Individual arrow from blocker to attacker.
 * Uses data attributes on card elements to find positions.
 */
function BlockerArrow({ blockerId, attackerId }: BlockerArrowProps) {
  // Find card elements by data attributes
  const blockerEl = document.querySelector(`[data-card-id="${blockerId}"]`)
  const attackerEl = document.querySelector(`[data-card-id="${attackerId}"]`)

  if (!blockerEl || !attackerEl) return null

  const blockerRect = blockerEl.getBoundingClientRect()
  const attackerRect = attackerEl.getBoundingClientRect()

  // Calculate center points
  const startX = blockerRect.left + blockerRect.width / 2
  const startY = blockerRect.top + blockerRect.height / 2
  const endX = attackerRect.left + attackerRect.width / 2
  const endY = attackerRect.top + attackerRect.height / 2

  return (
    <line
      x1={startX}
      y1={startY}
      x2={endX}
      y2={endY}
      stroke="#3498db"
      strokeWidth={3}
      markerEnd="url(#arrowhead)"
      style={{ filter: 'drop-shadow(0 0 4px rgba(52, 152, 219, 0.8))' }}
    />
  )
}

/**
 * Hook to get the blocker drag state.
 */
export function useBlockerDrag() {
  const combatState = useGameStore((state) => state.combatState)
  const assignBlocker = useGameStore((state) => state.assignBlocker)
  const removeBlockerAssignment = useGameStore((state) => state.removeBlockerAssignment)

  const isBlockerMode = combatState?.mode === 'declareBlockers'

  const handleBlockerClick = (creatureId: EntityId) => {
    if (!isBlockerMode) return

    // Check if this is a valid blocker
    if (combatState.validCreatures.includes(creatureId)) {
      // If already assigned, remove assignment
      if (combatState.blockerAssignments[creatureId]) {
        removeBlockerAssignment(creatureId)
      }
      // Otherwise, this will be handled by the attacker click
    }
  }

  const handleAttackerClick = (attackerId: EntityId, selectedBlockerId: EntityId | null) => {
    if (!isBlockerMode || !selectedBlockerId) return

    // Check if attacker is valid
    if (combatState.attackingCreatures.includes(attackerId)) {
      assignBlocker(selectedBlockerId, attackerId)
    }
  }

  return {
    isBlockerMode,
    handleBlockerClick,
    handleAttackerClick,
  }
}
