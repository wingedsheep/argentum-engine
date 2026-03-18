import { useEffect, useState } from 'react'
import { useGameStore } from '@/store/gameStore.ts'
import { useStackCards } from '@/store/selectors.ts'
import type { EntityId, ClientChosenTarget } from '@/types'

interface Point {
  x: number
  y: number
}

interface ArrowProps {
  start: Point
  end: Point
  color: string
  damageLabel?: number | null | undefined
}

/**
 * SVG arrow component with curved path and arrowhead.
 * Reuses the same design as CombatArrows.
 */
function Arrow({ start, end, color, damageLabel }: ArrowProps) {
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

  // Damage badge position at t=0.5 on the bezier curve (midpoint)
  let badgeElement: React.ReactNode = null
  if (damageLabel != null) {
    const t = 0.5
    const mt = 1 - t
    const badgeX = mt * mt * start.x + 2 * mt * t * controlX + t * t * end.x
    const badgeY = mt * mt * start.y + 2 * mt * t * controlY + t * t * end.y
    const label = `${damageLabel} dmg`
    const textWidth = label.length * 7 + 12

    badgeElement = (
      <g>
        <rect
          x={badgeX - textWidth / 2}
          y={badgeY - 11}
          width={textWidth}
          height={22}
          rx={11}
          fill="#000000"
          fillOpacity={0.85}
          stroke="#dc2626"
          strokeWidth={1.5}
        />
        <text
          x={badgeX}
          y={badgeY + 4}
          textAnchor="middle"
          fill="#f87171"
          fontSize={12}
          fontWeight={700}
          fontFamily="system-ui, sans-serif"
          style={{ pointerEvents: 'none' }}
        >
          {label}
        </text>
      </g>
    )
  }

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
      {badgeElement}
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
 * Get the entity ID from a target for damage distribution lookup.
 */
function getTargetEntityId(target: ClientChosenTarget): EntityId | null {
  switch (target.type) {
    case 'Player':
      return target.playerId
    case 'Permanent':
      return target.entityId
    case 'Spell':
      return target.spellEntityId
    case 'Card':
      return target.cardId
    default:
      return null
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
    case 'Card':
      // Card in a zone (e.g., graveyard) - use the same card lookup
      return getCardCenter(target.cardId)
    default:
      return null
  }
}

interface TargetArrow {
  sourceId: EntityId
  targetKey: string
  start: Point
  end: Point
  color: string
  damageLabel?: number | null | undefined
}

/**
 * Targeting arrows overlay - draws arrows from spells on the stack to their targets.
 */
export function TargetingArrows() {
  const stackCards = useStackCards()
  const pendingDecision = useGameStore((state) => state.pendingDecision)
  const lastDamageDistribution = useGameStore((state) => state.lastDamageDistribution)
  const [arrows, setArrows] = useState<TargetArrow[]>([])

  // Hide arrows during full-screen overlay decisions (e.g., ChooseColorDecision)
  // Keep arrows visible for inline trigger YesNo decisions (rendered on the card, not as overlay)
  const hasOverlayDecision = pendingDecision != null &&
    pendingDecision.type !== 'ChooseTargetsDecision' &&
    !(pendingDecision.type === 'SelectCardsDecision' && pendingDecision.useTargetingUI) &&
    !(pendingDecision.type === 'YesNoDecision' && !!pendingDecision.context.triggeringEntityId)

  // Update arrow positions periodically
  useEffect(() => {
    // Only process cards on the stack that have targets or a triggering entity
    const cardsWithTargets = stackCards.filter(card =>
      (card.targets && card.targets.length > 0) || card.triggeringEntityId
    )

    if (cardsWithTargets.length === 0) {
      setArrows([])
      return
    }

    const updateArrows = () => {
      const newArrows: TargetArrow[] = []

      for (const card of cardsWithTargets) {
        const stackPos = getCardCenter(card.id)
        if (!stackPos) continue

        // Target arrows: stack item → target (orange)
        for (let i = 0; i < card.targets.length; i++) {
          const target = card.targets[i]
          if (!target) continue
          const targetPos = getTargetPosition(target)
          if (!targetPos) continue

          // Look up damage distribution for this target:
          // 1. Server-sent distribution on the stack card (visible to all players)
          // 2. Local distribution from the caster's UI (before server confirms)
          const targetEntityId = getTargetEntityId(target)
          const serverDistribution = card.damageDistribution
          const damageLabel = targetEntityId != null
            ? (serverDistribution?.[targetEntityId] ?? lastDamageDistribution?.[targetEntityId] ?? null)
            : null

          newArrows.push({
            sourceId: card.id,
            targetKey: `${card.id}-target-${i}`,
            start: stackPos,
            end: targetPos,
            color: '#ff8800',
            damageLabel,
          })
        }

        // Source arrow: triggering entity → stack item (cyan, reversed)
        if (card.triggeringEntityId) {
          const triggerPos = getCardCenter(card.triggeringEntityId)
          if (triggerPos) {
            newArrows.push({
              sourceId: card.id,
              targetKey: `${card.id}-source`,
              start: triggerPos,
              end: stackPos,
              color: '#44ccdd',
            })
          }
        }
      }

      setArrows(newArrows)
    }

    // Update immediately and on interval for smooth updates
    updateArrows()
    const interval = setInterval(updateArrows, 100)
    return () => clearInterval(interval)
  }, [stackCards, lastDamageDistribution])

  if (arrows.length === 0 || hasOverlayDecision) {
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
      {arrows.map(({ targetKey, start, end, color, damageLabel }) => (
        <Arrow
          key={targetKey}
          start={start}
          end={end}
          color={color}
          damageLabel={damageLabel}
        />
      ))}
    </svg>
  )
}
