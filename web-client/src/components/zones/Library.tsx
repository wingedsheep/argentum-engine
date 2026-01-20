import type { Vector3Tuple } from 'three'
import { Text } from '@react-three/drei'
import { useZone } from '../../store/selectors'
import { CARD_WIDTH, CARD_HEIGHT, CARD_DEPTH, CARD_SCALES } from './ZoneLayout'
import type { ZoneId } from '../../types'

interface LibraryProps {
  zoneId: ZoneId
  position: Vector3Tuple
}

/**
 * Library zone showing a deck pile with card count.
 *
 * Displayed as a stack of card backs with the count overlaid.
 */
export function Library({ zoneId, position }: LibraryProps) {
  const zone = useZone(zoneId)

  const cardCount = zone?.size ?? 0
  const stackHeight = Math.min(cardCount, 60) * CARD_DEPTH * 2
  const scale = CARD_SCALES.library

  return (
    <group position={position}>
      {/* Card stack base */}
      <mesh position={[0, stackHeight / 2, 0]} castShadow receiveShadow>
        <boxGeometry
          args={[CARD_WIDTH * scale, stackHeight, CARD_HEIGHT * scale]}
        />
        <meshStandardMaterial color="#1a1a2e" />
      </mesh>

      {/* Top card (card back) */}
      {cardCount > 0 && (
        <mesh position={[0, stackHeight + 0.005, 0]} rotation={[-Math.PI / 2, 0, 0]}>
          <planeGeometry args={[CARD_WIDTH * scale, CARD_HEIGHT * scale]} />
          <meshStandardMaterial color="#2a2a4e" />
        </mesh>
      )}

      {/* Card back pattern */}
      {cardCount > 0 && (
        <mesh position={[0, stackHeight + 0.006, 0]} rotation={[-Math.PI / 2, 0, 0]}>
          <planeGeometry
            args={[CARD_WIDTH * scale * 0.8, CARD_HEIGHT * scale * 0.8]}
          />
          <meshStandardMaterial
            color="#4a4a8e"
            transparent
            opacity={0.5}
          />
        </mesh>
      )}

      {/* Card count text */}
      <Text
        position={[0, stackHeight + 0.02, 0]}
        rotation={[-Math.PI / 2, 0, 0]}
        fontSize={0.15}
        color="white"
        anchorX="center"
        anchorY="middle"
        outlineWidth={0.02}
        outlineColor="#000000"
      >
        {cardCount.toString()}
      </Text>
    </group>
  )
}
