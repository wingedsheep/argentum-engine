import { useMemo } from 'react'
import type { Vector3Tuple } from 'three'
import { Text } from '@react-three/drei'
import { Card3D } from '../card/Card3D'
import { useZoneCards, useZone } from '../../store/selectors'
import { CARD_SCALES, CARD_WIDTH, CARD_HEIGHT, calculatePilePositions } from './ZoneLayout'
import type { ZoneId } from '../../types'

interface GraveyardProps {
  zoneId: ZoneId
  position: Vector3Tuple
}

/**
 * Graveyard zone showing a pile of face-up cards.
 *
 * Displays cards stacked with slight random offsets.
 * Only the top few cards are rendered as individual meshes
 * to optimize performance.
 */
export function Graveyard({ zoneId, position }: GraveyardProps) {
  const cards = useZoneCards(zoneId)
  const zone = useZone(zoneId)

  const cardCount = zone?.size ?? 0

  // Only render top N cards individually
  const maxVisibleCards = 5
  const visibleCards = cards.slice(-maxVisibleCards)

  const pilePositions = useMemo(
    () => calculatePilePositions(visibleCards.length),
    [visibleCards.length]
  )

  const scale = CARD_SCALES.graveyard

  return (
    <group position={position}>
      {/* Base pile for cards below visible threshold */}
      {cardCount > maxVisibleCards && (
        <mesh
          position={[0, (cardCount - maxVisibleCards) * 0.002, 0]}
          castShadow
        >
          <boxGeometry
            args={[
              CARD_WIDTH * scale,
              (cardCount - maxVisibleCards) * 0.004,
              CARD_HEIGHT * scale,
            ]}
          />
          <meshStandardMaterial color="#3a3a3a" />
        </mesh>
      )}

      {/* Visible cards */}
      {visibleCards.map((card, index) => {
        const pilePos = pilePositions[index]
        if (!pilePos) return null

        const baseY = cardCount > maxVisibleCards
          ? (cardCount - maxVisibleCards) * 0.004
          : 0

        return (
          <Card3D
            key={card.id}
            card={card}
            position={[pilePos.x, baseY + pilePos.y, pilePos.z]}
            rotation={[0, pilePos.rotation, 0]}
            scale={scale}
            faceDown={false}
            interactive={true}
          />
        )
      })}

      {/* Card count badge */}
      {cardCount > 0 && (
        <group position={[CARD_WIDTH * scale * 0.4, 0.1, -CARD_HEIGHT * scale * 0.4]}>
          <mesh>
            <circleGeometry args={[0.12, 16]} />
            <meshBasicMaterial color="#1a1a1a" />
          </mesh>
          <Text
            position={[0, 0, 0.01]}
            fontSize={0.1}
            color="white"
            anchorX="center"
            anchorY="middle"
          >
            {cardCount.toString()}
          </Text>
        </group>
      )}
    </group>
  )
}
