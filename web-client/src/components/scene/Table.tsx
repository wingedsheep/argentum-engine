/**
 * The game table surface - MTG Arena style.
 *
 * A flat plane representing the battlefield with zone indicators.
 * Dark theme matching MTG Arena's aesthetic.
 */
export function Table() {
  return (
    <group>
      {/* Main battlefield surface */}
      <mesh
        rotation={[-Math.PI / 2, 0, 0]}
        position={[0, -0.02, 0]}
        receiveShadow
      >
        <planeGeometry args={[16, 12]} />
        <meshStandardMaterial
          color="#1a1a2e"
          roughness={0.9}
          metalness={0.0}
        />
      </mesh>

      {/* Battlefield center - slightly lighter */}
      <mesh
        rotation={[-Math.PI / 2, 0, 0]}
        position={[0, -0.01, 0]}
        receiveShadow
      >
        <planeGeometry args={[14, 8]} />
        <meshStandardMaterial
          color="#16213e"
          roughness={0.8}
          metalness={0.1}
        />
      </mesh>

      {/* Center divider line */}
      <mesh
        rotation={[-Math.PI / 2, 0, 0]}
        position={[0, 0.001, 0]}
      >
        <planeGeometry args={[14, 0.03]} />
        <meshStandardMaterial
          color="#0f3460"
          roughness={0.5}
          metalness={0.3}
        />
      </mesh>

      {/* Player creature zone highlight */}
      <mesh
        rotation={[-Math.PI / 2, 0, 0]}
        position={[0, 0.001, 1.2]}
      >
        <planeGeometry args={[12, 1.0]} />
        <meshStandardMaterial
          color="#1a3a5c"
          roughness={0.7}
          metalness={0.1}
          transparent
          opacity={0.3}
        />
      </mesh>

      {/* Player land zone highlight */}
      <mesh
        rotation={[-Math.PI / 2, 0, 0]}
        position={[0, 0.001, 2.6]}
      >
        <planeGeometry args={[12, 1.0]} />
        <meshStandardMaterial
          color="#1a4a3c"
          roughness={0.7}
          metalness={0.1}
          transparent
          opacity={0.3}
        />
      </mesh>

      {/* Opponent creature zone highlight */}
      <mesh
        rotation={[-Math.PI / 2, 0, 0]}
        position={[0, 0.001, -1.2]}
      >
        <planeGeometry args={[12, 1.0]} />
        <meshStandardMaterial
          color="#3a1a1a"
          roughness={0.7}
          metalness={0.1}
          transparent
          opacity={0.3}
        />
      </mesh>

      {/* Opponent land zone highlight */}
      <mesh
        rotation={[-Math.PI / 2, 0, 0]}
        position={[0, 0.001, -2.6]}
      >
        <planeGeometry args={[12, 1.0]} />
        <meshStandardMaterial
          color="#4a2a1a"
          roughness={0.7}
          metalness={0.1}
          transparent
          opacity={0.3}
        />
      </mesh>
    </group>
  )
}
