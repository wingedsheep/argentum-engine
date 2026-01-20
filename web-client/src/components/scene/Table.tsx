import { useRef } from 'react'
import { Mesh } from 'three'

/**
 * The game table surface.
 *
 * A flat plane representing the play mat where cards are placed.
 * Uses a dark green color reminiscent of classic card table felt.
 */
export function Table() {
  const meshRef = useRef<Mesh>(null)

  return (
    <group>
      {/* Main table surface */}
      <mesh
        ref={meshRef}
        rotation={[-Math.PI / 2, 0, 0]}
        position={[0, -0.01, 0]}
        receiveShadow
      >
        <planeGeometry args={[14, 10]} />
        <meshStandardMaterial
          color="#1a472a"
          roughness={0.8}
          metalness={0.1}
        />
      </mesh>

      {/* Table edge/border */}
      <mesh
        rotation={[-Math.PI / 2, 0, 0]}
        position={[0, -0.02, 0]}
      >
        <planeGeometry args={[14.5, 10.5]} />
        <meshStandardMaterial
          color="#0d2818"
          roughness={0.9}
          metalness={0.0}
        />
      </mesh>

      {/* Zone divider line (center) */}
      <mesh
        rotation={[-Math.PI / 2, 0, 0]}
        position={[0, 0.001, 0]}
      >
        <planeGeometry args={[12, 0.02]} />
        <meshStandardMaterial
          color="#2d5a3d"
          roughness={0.5}
          metalness={0.2}
        />
      </mesh>

      {/* Player area indicator */}
      <mesh
        rotation={[-Math.PI / 2, 0, 0]}
        position={[0, 0.001, 2]}
      >
        <planeGeometry args={[10, 0.01]} />
        <meshStandardMaterial
          color="#2d5a3d"
          roughness={0.5}
          metalness={0.2}
          transparent
          opacity={0.5}
        />
      </mesh>

      {/* Opponent area indicator */}
      <mesh
        rotation={[-Math.PI / 2, 0, 0]}
        position={[0, 0.001, -2]}
      >
        <planeGeometry args={[10, 0.01]} />
        <meshStandardMaterial
          color="#2d5a3d"
          roughness={0.5}
          metalness={0.2}
          transparent
          opacity={0.5}
        />
      </mesh>
    </group>
  )
}
