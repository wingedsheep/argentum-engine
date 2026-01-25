import { useEffect, useState } from 'react'
import { useStackCards } from '../../store/selectors'
import type { EntityId, ClientChosenTarget } from '../../types'

interface Point {
  x: number
  y: number
}

interface ArrowProps {
  start: Point
  end: Point
  color: string
}

/**
 * SVG arrow component with curved path and arrowhead.
 * Reuses the same design as CombatArrows.
 */
function Arrow({ start, end, color }: ArrowProps) {
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
 * Get the center position of a player's life display by their ID.
 */
function getPlayerCenter(playerId: EntityId): Point | null {
  const element = document.querySelector(`[data-player-id="${playerId}"]`)
  if (!element) return null

  const rect = element.getBoundingClientRect()
  return {
    x: rect.left + rect.width / 2,
    y: rect.top + rect.height / 2,
  }
}

/**
 * Get the position for a target based on its type.
 */
function getTargetPosition(target: ClientChosenTarget): Point | null {
  switch (target.type) {
    case 'Player':
      return getPlayerCenter(target.playerId)
    case 'Permanent':
      return getCardCenter(target.entityId)
    case 'Spell':
      return getCardCenter(target.spellEntityId)
    default:
      return null
  }
}

interface TargetArrow {
  sourceId: EntityId
  targetKey: string
  start: Point
  end: Point
}

/**
 * Targeting arrows overlay - draws arrows from spells on the stack to their targets.
 */
export function TargetingArrows() {
  const stackCards = useStackCards()
  const [arrows, setArrows] = useState<TargetArrow[]>([])

  // Update arrow positions periodically
  useEffect(() => {
    // Only process cards on the stack that have targets
    const cardsWithTargets = stackCards.filter(card => card.targets && card.targets.length > 0)

    if (cardsWithTargets.length === 0) {
      setArrows([])
      return
    }

    const updateArrows = () => {
      const newArrows: TargetArrow[] = []

      for (const card of cardsWithTargets) {
        const sourcePos = getCardCenter(card.id)
        if (!sourcePos) continue

        for (let i = 0; i < card.targets.length; i++) {
          const target = card.targets[i]
          if (!target) continue
          const targetPos = getTargetPosition(target)
          if (!targetPos) continue

          newArrows.push({
            sourceId: card.id,
            targetKey: `${card.id}-${i}`,
            start: sourcePos,
            end: targetPos,
          })
        }
      }

      setArrows(newArrows)
    }

    // Update immediately and on interval for smooth updates
    updateArrows()
    const interval = setInterval(updateArrows, 100)
    return () => clearInterval(interval)
  }, [stackCards])

  if (arrows.length === 0) {
    return null
  }

  return (
    <svg
      style={{
        position: 'fixed',
        top: 0,
        left: 0,
        width: '100vw',
        height: '100vh',
        pointerEvents: 'none',
        zIndex: 999,
      }}
    >
      {arrows.map(({ targetKey, start, end }) => (
        <Arrow
          key={targetKey}
          start={start}
          end={end}
          color="#ff8800" // Orange to differentiate from combat arrows (blue)
        />
      ))}
    </svg>
  )
}
