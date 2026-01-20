import { useMemo } from 'react'
import type { Vector3Tuple } from 'three'
import { Card3D } from '../card/Card3D'
import { useZoneCards } from '../../store/selectors'
import { CARD_SCALES, calculateFanPositions } from './ZoneLayout'
import type { ZoneId } from '../../types'

interface HandProps {
  zoneId: ZoneId
  position: Vector3Tuple
  rotation?: Vector3Tuple
  isOpponent?: boolean
}

/**
 * Hand zone displaying cards in a fan layout.
 *
 * Player's hand shows card faces with interaction enabled.
 * Opponent's hand shows card backs only.
 */
export function Hand({ zoneId, position, rotation = [0, 0, 0], isOpponent = false }: HandProps) {
  const cards = useZoneCards(zoneId)

  const fanPositions = useMemo(
    () => calculateFanPositions(cards.length),
    [cards.length]
  )

  return (
    <group position={position} rotation={rotation}>
      {cards.map((card, index) => {
        const fanPos = fanPositions[index]
        if (!fanPos) return null

        return (
          <Card3D
            key={card.id}
            card={card}
            position={[fanPos.x, 0, fanPos.z]}
            rotation={[-0.3, fanPos.angle, 0]}
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
