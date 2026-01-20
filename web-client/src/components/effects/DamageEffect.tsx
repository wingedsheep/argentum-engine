import { useRef } from 'react'
import { Group } from 'three'
import { Text } from '@react-three/drei'
import type { EntityId } from '../../types'

interface DamageEffectProps {
  targetId: EntityId
  amount: number
  isPlayer: boolean
  progress: number // 0 to 1
}

// Note: targetId is used for looking up position in full implementation

/**
 * Floating damage number effect.
 *
 * Shows a red number floating up from the target.
 */
export function DamageEffect({ targetId: _targetId, amount, isPlayer, progress }: DamageEffectProps) {
  const groupRef = useRef<Group>(null)

  // Calculate position based on target
  // In a real implementation, would look up the target's position
  const basePosition: [number, number, number] = isPlayer
    ? [0, 0.5, 3] // Player position
    : [0, 0.5, 0] // Default card position

  // Animation: float up and fade out
  const yOffset = progress * 1.5
  const opacity = 1 - progress * 0.8
  const scale = 1 + progress * 0.3

  return (
    <group
      ref={groupRef}
      position={[
        basePosition[0],
        basePosition[1] + yOffset,
        basePosition[2],
      ]}
    >
      {/* Damage number */}
      <Text
        fontSize={0.3 * scale}
        color="#ff0000"
        anchorX="center"
        anchorY="middle"
        outlineWidth={0.03}
        outlineColor="#000000"
        fillOpacity={opacity}
        outlineOpacity={opacity}
      >
        {`-${amount}`}
      </Text>

      {/* Impact flash (early in animation) */}
      {progress < 0.2 && (
        <mesh>
          <circleGeometry args={[0.3 * (1 - progress * 5), 16]} />
          <meshBasicMaterial
            color="#ff4444"
            transparent
            opacity={0.5 * (1 - progress * 5)}
          />
        </mesh>
      )}
    </group>
  )
}

/**
 * Life gain effect - green floating number.
 */
interface LifeGainEffectProps {
  playerId: EntityId
  amount: number
  progress: number
}

export function LifeGainEffect({ playerId: _playerId, amount, progress }: LifeGainEffectProps) {
  // Similar to damage but green and for life gain
  const yOffset = progress * 1.5
  const opacity = 1 - progress * 0.8
  const scale = 1 + progress * 0.3

  return (
    <group position={[0, 0.5 + yOffset, 3]}>
      <Text
        fontSize={0.3 * scale}
        color="#00ff00"
        anchorX="center"
        anchorY="middle"
        outlineWidth={0.03}
        outlineColor="#000000"
        fillOpacity={opacity}
        outlineOpacity={opacity}
      >
        {`+${amount}`}
      </Text>
    </group>
  )
}
