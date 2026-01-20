import { useMemo } from 'react'
import type { Vector3Tuple } from 'three'
import { Text } from '@react-three/drei'
import { Card3D } from '../card/Card3D'
import { useStackCards } from '../../store/selectors'
import { CARD_SCALES, calculateStackPositions } from './ZoneLayout'

interface StackProps {
  position: Vector3Tuple
}

/**
 * The spell stack zone.
 *
 * Shows spells and abilities on the stack in a cascading layout.
 * The top of the stack (most recently added) is at the top visually.
 */
export function Stack({ position }: StackProps) {
  const cards = useStackCards()

  const stackPositions = useMemo(
    () => calculateStackPositions(cards.length),
    [cards.length]
  )

  if (cards.length === 0) {
    return null
  }

  return (
    <group position={position}>
      {/* Stack label */}
      <Text
        position={[0, 0.5, 0.5]}
        fontSize={0.12}
        color="#888888"
        anchorX="center"
        anchorY="middle"
      >
        Stack
      </Text>

      {/* Stack items */}
      {cards.map((card, index) => {
        const stackPos = stackPositions[index]
        if (!stackPos) return null

        // Reverse order so top of stack is visually on top
        const reverseIndex = cards.length - 1 - index

        return (
          <Card3D
            key={card.id}
            card={card}
            position={[0, reverseIndex * 0.02, reverseIndex * -0.3]}
            rotation={[-0.2, 0, 0]}
            scale={CARD_SCALES.stack}
            faceDown={false}
            interactive={true}
            highlight={index === 0 ? 'active' : undefined}
          />
        )
      })}

      {/* Stack indicator background */}
      <mesh position={[0, -0.01, 0]} rotation={[-Math.PI / 2, 0, 0]}>
        <planeGeometry args={[1, 2]} />
        <meshStandardMaterial
          color="#2a2a3e"
          transparent
          opacity={0.3}
        />
      </mesh>
    </group>
  )
}
