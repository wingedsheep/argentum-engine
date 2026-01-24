import { useEffect, useState } from 'react'
import { useGameStore } from '../../store/gameStore'
import type { EntityId } from '../../types'

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
 * Combat arrows overlay - draws arrows between blockers and attackers.
 */
export function CombatArrows() {
  const combatState = useGameStore((state) => state.combatState)
  const draggingBlockerId = useGameStore((state) => state.draggingBlockerId)
  const [mousePos, setMousePos] = useState<Point | null>(null)
  const [arrows, setArrows] = useState<Array<{ start: Point; end: Point; blockerId: EntityId }>>([])

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

  // Update arrow positions when assignments change
  useEffect(() => {
    if (!combatState || combatState.mode !== 'declareBlockers') {
      setArrows([])
      return
    }

    const updateArrows = () => {
      const newArrows: Array<{ start: Point; end: Point; blockerId: EntityId }> = []

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

      setArrows(newArrows)
    }

    // Update immediately and on animation frames for smooth updates
    updateArrows()
    const interval = setInterval(updateArrows, 100)
    return () => clearInterval(interval)
  }, [combatState])

  // Don't render if not in blocker mode
  if (!combatState || combatState.mode !== 'declareBlockers') {
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
        zIndex: 1000,
      }}
    >
      {/* Existing blocker assignments */}
      {arrows.map(({ start, end, blockerId }) => (
        <Arrow
          key={blockerId}
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
