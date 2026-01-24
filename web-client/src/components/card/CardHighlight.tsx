import { useRef } from 'react'
import { Mesh } from 'three'
import { useFrame } from '@react-three/fiber'
import type { CardHighlightType } from './Card3D'

interface CardHighlightProps {
  type: CardHighlightType
  width: number
  height: number
  isHovered?: boolean
}

/**
 * Highlight colors for different states.
 */
const HIGHLIGHT_COLORS: Record<NonNullable<CardHighlightType>, string> = {
  legal: '#00ff00', // Green for playable cards
  selected: '#ffff00', // Yellow for selected
  target: '#ff00ff', // Magenta for valid targets
  active: '#00ffff', // Cyan for active (e.g., top of stack)
  attacking: '#ff4444', // Red for attacking creatures
  blocking: '#3498db', // Blue for blocking creatures
}

/**
 * Glowing highlight effect around a card.
 *
 * Renders a plane slightly behind/below the card with a glow effect.
 * The glow pulses gently to draw attention.
 */
export function CardHighlight({ type, width, height, isHovered = false }: CardHighlightProps) {
  const meshRef = useRef<Mesh>(null)
  const pulseRef = useRef(0)

  const color = type ? HIGHLIGHT_COLORS[type] : '#ffffff'

  // Animate the glow
  useFrame((_, delta) => {
    if (!meshRef.current) return

    pulseRef.current += delta * 3
    const pulseIntensity = 0.6 + Math.sin(pulseRef.current) * 0.2
    const hoverBoost = isHovered ? 1.3 : 1.0

    const material = meshRef.current.material as THREE.MeshBasicMaterial
    if (material.opacity !== undefined) {
      material.opacity = pulseIntensity * hoverBoost * (type === 'legal' ? 0.4 : 0.6)
    }
  })

  if (!type) return null

  return (
    <group>
      {/* Main glow plane */}
      <mesh
        ref={meshRef}
        position={[0, -0.005, 0]}
        rotation={[-Math.PI / 2, 0, 0]}
      >
        <planeGeometry args={[width * 1.15, height * 1.15]} />
        <meshBasicMaterial
          color={color}
          transparent
          opacity={0.5}
          depthWrite={false}
        />
      </mesh>

      {/* Outer glow (softer, larger) */}
      <mesh
        position={[0, -0.006, 0]}
        rotation={[-Math.PI / 2, 0, 0]}
      >
        <planeGeometry args={[width * 1.3, height * 1.3]} />
        <meshBasicMaterial
          color={color}
          transparent
          opacity={0.2}
          depthWrite={false}
        />
      </mesh>

      {/* Border outline */}
      <lineSegments position={[0, 0.001, 0]} rotation={[-Math.PI / 2, 0, 0]}>
        <edgesGeometry args={[new THREE.PlaneGeometry(width * 1.05, height * 1.05)]} />
        <lineBasicMaterial color={color} linewidth={2} />
      </lineSegments>
    </group>
  )
}

// Import THREE for the lineSegments
import * as THREE from 'three'
