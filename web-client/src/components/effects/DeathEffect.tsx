import { useRef } from 'react'
import { Group } from 'three'
import type { EntityId } from '../../types'
import { CARD_WIDTH, CARD_HEIGHT } from '../zones/ZoneLayout'

interface DeathEffectProps {
  creatureId: EntityId
  progress: number // 0 to 1
}

/**
 * Death animation effect for creatures.
 *
 * Shows the creature fading and falling.
 * In a full implementation, would look up the creature's position.
 */
export function DeathEffect({ creatureId: _creatureId, progress }: DeathEffectProps) {
  const groupRef = useRef<Group>(null)

  // Animation phases:
  // 0-0.3: Flash red
  // 0.3-0.7: Fade and sink
  // 0.7-1.0: Disappear

  const flashIntensity = progress < 0.3 ? (0.3 - progress) / 0.3 : 0
  const yOffset = -progress * 0.5
  const rotationX = progress * Math.PI / 6

  // Placeholder position - would get from card tracking
  const position: [number, number, number] = [0, 0.1 + yOffset, 0]

  return (
    <group ref={groupRef} position={position} rotation={[rotationX, 0, 0]}>
      {/* Death flash overlay */}
      {flashIntensity > 0 && (
        <mesh rotation={[-Math.PI / 2, 0, 0]} position={[0, 0.01, 0]}>
          <planeGeometry args={[CARD_WIDTH * 0.8, CARD_HEIGHT * 0.8]} />
          <meshBasicMaterial
            color="#ff0000"
            transparent
            opacity={flashIntensity * 0.5}
          />
        </mesh>
      )}

      {/* Particle effect (simplified as expanding circles) */}
      {progress > 0.2 && progress < 0.8 && (
        <group>
          {[0, 1, 2, 3, 4].map((i) => {
            const particleProgress = Math.max(0, progress - 0.2 - i * 0.1)
            const particleScale = particleProgress * 3
            const particleOpacity = Math.max(0, 0.5 - particleProgress)

            return (
              <mesh
                key={i}
                rotation={[-Math.PI / 2, 0, 0]}
                position={[0, 0.02 + i * 0.01, 0]}
              >
                <ringGeometry args={[particleScale * 0.1, particleScale * 0.15, 8]} />
                <meshBasicMaterial
                  color="#ff6666"
                  transparent
                  opacity={particleOpacity}
                />
              </mesh>
            )
          })}
        </group>
      )}

      {/* Skull/death icon (simplified) */}
      {progress > 0.1 && progress < 0.6 && (
        <group position={[0, 0.3, 0]} rotation={[-Math.PI / 2, 0, 0]}>
          <mesh>
            <circleGeometry args={[0.15, 16]} />
            <meshBasicMaterial
              color="#ffffff"
              transparent
              opacity={(0.6 - progress) * 2}
            />
          </mesh>
        </group>
      )}
    </group>
  )
}

/**
 * Exile effect - similar to death but with purple/white flash.
 */
interface ExileEffectProps {
  permanentId: EntityId
  progress: number
}

export function ExileEffect({ permanentId: _permanentId, progress }: ExileEffectProps) {
  const flashIntensity = progress < 0.3 ? (0.3 - progress) / 0.3 : 0
  const fadeProgress = Math.max(0, Math.min(1, (progress - 0.3) / 0.4))
  const opacity = 1 - fadeProgress
  const scale = 1 + progress * 0.5

  return (
    <group position={[0, 0.1, 0]} scale={scale}>
      {/* Exile flash */}
      {flashIntensity > 0 && (
        <mesh rotation={[-Math.PI / 2, 0, 0]} position={[0, 0.01, 0]}>
          <planeGeometry args={[CARD_WIDTH * 0.8, CARD_HEIGHT * 0.8]} />
          <meshBasicMaterial
            color="#ffffff"
            transparent
            opacity={flashIntensity * 0.7}
          />
        </mesh>
      )}

      {/* Purple glow */}
      <mesh rotation={[-Math.PI / 2, 0, 0]} position={[0, 0.005, 0]}>
        <planeGeometry
          args={[CARD_WIDTH * 0.9 * scale, CARD_HEIGHT * 0.9 * scale]}
        />
        <meshBasicMaterial
          color="#8800ff"
          transparent
          opacity={opacity * 0.3}
        />
      </mesh>
    </group>
  )
}
