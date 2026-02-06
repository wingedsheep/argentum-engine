import { useEffect, useState } from 'react'
import { useGameStore } from '../../store/gameStore'
import { selectGameState } from '../../store/selectors'
import type { EntityId } from '../../types'
import { Step, ZoneType } from '../../types'

interface Point {
  x: number
  y: number
}

interface ArrowProps {
  start: Point
  end: Point
  color: string
  dashed?: boolean
}

interface ArrowData {
  start: Point
  end: Point
  blockerId: EntityId
}

interface AttackerArrowData {
  start: Point
  end: Point
  attackerId: EntityId
}

/**
 * SVG arrow component with curved path and arrowhead.
 */
function Arrow({ start, end, color, dashed = false }: ArrowProps) {
  // Calculate control point for quadratic bezier (arc upward)
  const midX = (start.x + end.x) / 2
  const midY = (start.y + end.y) / 2
  const dx = end.x - start.x
  const dy = end.y - start.y
  const distance = Math.sqrt(dx * dx + dy * dy)

  // Arc height based on distance, curving upward (negative Y in SVG)
  const arcHeight = Math.min(distance * 0.2, 60)
  const controlX = midX
  const controlY = midY - arcHeight

  // Path for quadratic bezier curve
  const pathD = `M ${start.x} ${start.y} Q ${controlX} ${controlY} ${end.x} ${end.y}`

  // Calculate arrowhead direction from the curve tangent at the end
  // For quadratic bezier, tangent at t=1 is: 2(P2 - P1) where P1 is control, P2 is end
  const tangentX = end.x - controlX
  const tangentY = end.y - controlY
  const tangentLen = Math.sqrt(tangentX * tangentX + tangentY * tangentY)
  const normX = tangentX / tangentLen
  const normY = tangentY / tangentLen

  // Arrowhead size
  const arrowSize = 12
  const arrowAngle = Math.PI / 6 // 30 degrees

  // Calculate arrowhead points
  const cos = Math.cos(arrowAngle)
  const sin = Math.sin(arrowAngle)

  const arrow1X = end.x - arrowSize * (normX * cos + normY * sin)
  const arrow1Y = end.y - arrowSize * (normY * cos - normX * sin)
  const arrow2X = end.x - arrowSize * (normX * cos - normY * sin)
  const arrow2Y = end.y - arrowSize * (normY * cos + normX * sin)

  const arrowheadD = `M ${end.x} ${end.y} L ${arrow1X} ${arrow1Y} M ${end.x} ${end.y} L ${arrow2X} ${arrow2Y}`

  return (
    <g>
      {/* Glow effect */}
      <path
        d={pathD}
        fill="none"
        stroke={color}
        strokeWidth={8}
        strokeOpacity={0.3}
        strokeLinecap="round"
      />
      {/* Main path */}
      <path
        d={pathD}
        fill="none"
        stroke={color}
        strokeWidth={3}
        strokeOpacity={0.9}
        strokeLinecap="round"
        strokeDasharray={dashed ? '8,4' : undefined}
      />
      {/* Arrowhead */}
      <path
        d={arrowheadD}
        fill="none"
        stroke={color}
        strokeWidth={3}
        strokeOpacity={0.9}
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </g>
  )
}

/**
 * Get the center position of a card element by its ID.
 */
function getCardCenter(cardId: EntityId): Point | null {
  const element = document.querySelector(`[data-card-id="${cardId}"]`)
  if (!element) return null

  const rect = element.getBoundingClientRect()
  return {
    x: rect.left + rect.width / 2,
    y: rect.top + rect.height / 2,
  }
}

/**
 * Get the center position of a player's life display.
 */
function getPlayerLifeCenter(playerId: EntityId): Point | null {
  const element = document.querySelector(`[data-life-id="${playerId}"]`)
  if (!element) return null

  const rect = element.getBoundingClientRect()
  return {
    x: rect.left + rect.width / 2,
    y: rect.top + rect.height / 2,
  }
}

/**
 * Combat arrows overlay - draws arrows between blockers and attackers.
 *
 * Shows arrows in three scenarios:
 * 1. During declare blockers phase (for the defending player) - uses local combatState
 * 2. For the attacking player during declare blockers - uses opponentBlockerAssignments (real-time sync)
 * 3. After blockers are declared (for both players) - uses server-sent gameState.combat
 */
// Combat steps where blocker arrows should be visible
const COMBAT_STEPS = new Set([
  Step.BEGIN_COMBAT,
  Step.DECLARE_ATTACKERS,
  Step.DECLARE_BLOCKERS,
  Step.FIRST_STRIKE_COMBAT_DAMAGE,
  Step.COMBAT_DAMAGE,
  Step.END_COMBAT,
])

export function CombatArrows() {
  const combatState = useGameStore((state) => state.combatState)
  const gameState = useGameStore(selectGameState)
  const gameStateCombat = gameState?.combat
  const currentStep = gameState?.currentStep
  const cards = gameState?.cards
  const opponentBlockerAssignments = useGameStore((state) => state.opponentBlockerAssignments)
  const draggingBlockerId = useGameStore((state) => state.draggingBlockerId)
  const isSpectating = useGameStore((state) => state.spectatingState !== null)
  const pendingDecision = useGameStore((state) => state.pendingDecision)
  const [mousePos, setMousePos] = useState<Point | null>(null)
  const [arrows, setArrows] = useState<Array<{ start: Point; end: Point; blockerId: EntityId }>>([])
  const [attackerArrows, setAttackerArrows] = useState<AttackerArrowData[]>([])

  // Check if we're still in combat phase
  const isInCombatPhase = currentStep && COMBAT_STEPS.has(currentStep as Step)

  // Check if we're selecting damage order (hide blocker arrows during this UI)
  const isSelectingDamageOrder = pendingDecision?.type === 'OrderObjectsDecision' &&
    pendingDecision?.context?.phase === 'COMBAT'

  // Hide all arrows during full-screen overlay decisions (e.g., ChooseColorDecision)
  const hasOverlayDecision = pendingDecision != null &&
    pendingDecision.type !== 'ChooseTargetsDecision' &&
    !(pendingDecision.type === 'SelectCardsDecision' && pendingDecision.useTargetingUI)

  // Track mouse position during drag
  useEffect(() => {
    if (!draggingBlockerId) {
      setMousePos(null)
      return
    }

    const handleMouseMove = (e: MouseEvent) => {
      setMousePos({ x: e.clientX, y: e.clientY })
    }

    window.addEventListener('mousemove', handleMouseMove)
    return () => window.removeEventListener('mousemove', handleMouseMove)
  }, [draggingBlockerId])

  // Check if we're in active declare blockers mode (local state)
  const isDeclaringBlockers = combatState?.mode === 'declareBlockers'

  // Update arrow positions when assignments change
  useEffect(() => {
    // Determine which blocker assignments to use:
    // 1. If we're actively declaring blockers, use local combatState
    // 2. If we're the attacker and opponent is assigning blockers, use opponentBlockerAssignments
    // 3. Otherwise, use server-sent combat data (if blockers have been declared)

    const updateArrows = () => {
      const newArrows: ArrowData[] = []

      // Skip blocker arrows during damage order selection (that UI shows blockers separately)
      if (isSelectingDamageOrder) {
        setArrows([])
        // Still compute attacker arrows below
      } else if (isDeclaringBlockers && combatState) {
        // Use local blocker assignments (real-time feedback during declaration)
        for (const [blockerIdStr, attackerId] of Object.entries(combatState.blockerAssignments)) {
          const blockerId = blockerIdStr as EntityId
          const blockerPos = getCardCenter(blockerId)
          const attackerPos = getCardCenter(attackerId)

          if (blockerPos && attackerPos) {
            newArrows.push({
              start: blockerPos,
              end: attackerPos,
              blockerId,
            })
          }
        }
      } else if (opponentBlockerAssignments && Object.keys(opponentBlockerAssignments).length > 0 && isInCombatPhase) {
        // Use opponent's real-time blocker assignments (for attacking player, only during combat)
        for (const [blockerIdStr, attackerId] of Object.entries(opponentBlockerAssignments)) {
          const blockerId = blockerIdStr as EntityId

          // Check that both blocker and attacker are still on the battlefield
          const blockerCard = cards?.[blockerId]
          const attackerCard = cards?.[attackerId]
          const blockerOnBattlefield = blockerCard?.zone?.zoneType === ZoneType.BATTLEFIELD
          const attackerOnBattlefield = attackerCard?.zone?.zoneType === ZoneType.BATTLEFIELD

          if (!blockerOnBattlefield || !attackerOnBattlefield) {
            continue
          }

          const blockerPos = getCardCenter(blockerId)
          const attackerPos = getCardCenter(attackerId)

          if (blockerPos && attackerPos) {
            newArrows.push({
              start: blockerPos,
              end: attackerPos,
              blockerId,
            })
          }
        }
      } else if (gameStateCombat && gameStateCombat.blockers.length > 0 && isInCombatPhase) {
        // Use server-sent combat data (shows to both players after blockers declared, only during combat)
        for (const blocker of gameStateCombat.blockers) {
          // Only draw arrows for creatures still on the battlefield
          // (creatures that died during combat damage should not have arrows)
          const blockerCard = cards?.[blocker.creatureId]
          const attackerCard = cards?.[blocker.blockingAttacker]
          const blockerOnBattlefield = blockerCard?.zone?.zoneType === ZoneType.BATTLEFIELD
          const attackerOnBattlefield = attackerCard?.zone?.zoneType === ZoneType.BATTLEFIELD

          if (!blockerOnBattlefield || !attackerOnBattlefield) {
            continue
          }

          const blockerPos = getCardCenter(blocker.creatureId)
          const attackerPos = getCardCenter(blocker.blockingAttacker)

          if (blockerPos && attackerPos) {
            newArrows.push({
              start: blockerPos,
              end: attackerPos,
              blockerId: blocker.creatureId,
            })
          }
        }
      }

      setArrows(newArrows)

      // Compute attacker arrows (for spectators and during combat)
      const newAttackerArrows: AttackerArrowData[] = []
      if (gameStateCombat && gameStateCombat.attackers.length > 0 && isInCombatPhase) {
        // Build set of blocked attacker IDs for quick lookup
        const blockedAttackerIds = new Set(
          gameStateCombat.blockers.map(b => b.blockingAttacker)
        )

        for (const attacker of gameStateCombat.attackers) {
          // Only show attacker arrows for unblocked attackers
          if (blockedAttackerIds.has(attacker.creatureId)) {
            continue
          }

          // Check if attacker is still on battlefield
          const attackerCard = cards?.[attacker.creatureId]
          if (attackerCard?.zone?.zoneType !== ZoneType.BATTLEFIELD) {
            continue
          }

          const attackerPos = getCardCenter(attacker.creatureId)
          // Get the target - either a player or planeswalker
          let targetPos: Point | null = null
          if (attacker.attackingTarget.type === 'Player') {
            targetPos = getPlayerLifeCenter(attacker.attackingTarget.playerId)
          }
          // TODO: Add support for planeswalker targets

          if (attackerPos && targetPos) {
            newAttackerArrows.push({
              start: attackerPos,
              end: targetPos,
              attackerId: attacker.creatureId,
            })
          }
        }
      }
      setAttackerArrows(newAttackerArrows)
    }

    // Update immediately and on animation frames for smooth updates
    updateArrows()
    const interval = setInterval(updateArrows, 100)
    return () => clearInterval(interval)
  }, [combatState, gameStateCombat, opponentBlockerAssignments, isDeclaringBlockers, isInCombatPhase, cards, isSpectating, isSelectingDamageOrder])

  // Don't render during full-screen overlay decisions
  if (hasOverlayDecision) {
    return null
  }

  // Don't render if no arrows to show (only show during combat phase)
  const hasBlockers = isDeclaringBlockers ||
    (opponentBlockerAssignments && Object.keys(opponentBlockerAssignments).length > 0 && isInCombatPhase) ||
    (gameStateCombat && gameStateCombat.blockers.length > 0 && isInCombatPhase)
  const hasAttackers = gameStateCombat && gameStateCombat.attackers.length > 0 && isInCombatPhase
  if (!hasBlockers && !hasAttackers && !draggingBlockerId) {
    return null
  }

  // Get dragging arrow if applicable
  const draggingArrow = (() => {
    if (!draggingBlockerId || !mousePos) return null
    const blockerPos = getCardCenter(draggingBlockerId)
    if (!blockerPos) return null
    return { start: blockerPos, end: mousePos }
  })()

  return (
    <svg
      style={{
        position: 'fixed',
        top: 0,
        left: 0,
        width: '100vw',
        height: '100vh',
        pointerEvents: 'none',
        zIndex: 2000, // Above spectator container (1500)
      }}
    >
      {/* Attacker arrows (unblocked attackers to defending player) */}
      {attackerArrows.map(({ start, end, attackerId }) => (
        <Arrow
          key={`attacker-${attackerId}`}
          start={start}
          end={end}
          color="#ff4444"
        />
      ))}

      {/* Blocker assignments */}
      {arrows.map(({ start, end, blockerId }) => (
        <Arrow
          key={`blocker-${blockerId}`}
          start={start}
          end={end}
          color="#4488ff"
        />
      ))}

      {/* Dragging arrow */}
      {draggingArrow && (
        <Arrow
          start={draggingArrow.start}
          end={draggingArrow.end}
          color="#88ccff"
          dashed
        />
      )}
    </svg>
  )
}
