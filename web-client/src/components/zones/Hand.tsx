import { useMemo } from 'react'
import type { Vector3Tuple } from 'three'
import { Card3D } from '../card/Card3D'
import { useZoneCards } from '../../store/selectors'
import { CARD_SCALES } from './ZoneLayout'
import type { ZoneId } from '../../types'

interface HandProps {
  zoneId: ZoneId
  position: Vector3Tuple
  rotation?: Vector3Tuple
  isOpponent?: boolean
}

/**
 * Calculate overlapping card positions for hand - MTG Arena style.
 * Cards overlap to fit more in the hand, with slight height variation.
 */
function calculateHandPositions(count: number, isOpponent: boolean): Array<{ x: number; y: number }> {
  if (count === 0) return []
  if (count === 1) return [{ x: 0, y: 0 }]

  // Calculate overlap - more cards = more overlap
  const baseSpacing = 1.2   // Spacing between cards
  const maxWidth = 12       // Max hand width
  const totalWidth = (count - 1) * baseSpacing
  const actualSpacing = totalWidth > maxWidth ? maxWidth / (count - 1) : baseSpacing

  const startX = -((count - 1) * actualSpacing) / 2

  return Array.from({ length: count }, (_, i) => ({
    x: startX + i * actualSpacing,
    y: isOpponent ? 0 : i * 0.002, // Slight y offset for z-ordering
  }))
}

/**
 * Hand zone displaying cards in overlapping row - MTG Arena style.
 *
 * Player's hand shows card faces with interaction enabled.
 * Opponent's hand shows card backs only.
 */
export function Hand({ zoneId, position, rotation = [0, 0, 0], isOpponent = false }: HandProps) {
  const cards = useZoneCards(zoneId)

  const handPositions = useMemo(
    () => calculateHandPositions(cards.length, isOpponent),
    [cards.length, isOpponent]
  )

  return (
    <group position={position} rotation={rotation}>
      {cards.map((card, index) => {
        const pos = handPositions[index] ?? { x: 0, y: 0 }

        return (
          <Card3D
            key={card.id}
            card={card}
            position={[pos.x, pos.y, 0]}
            rotation={[0, 0, 0]}
            scale={CARD_SCALES.hand}
            faceDown={isOpponent || card.isFaceDown}
            interactive={!isOpponent}
            hoverLift={!isOpponent}
          />
        )
      })}
    </group>
  )
}
