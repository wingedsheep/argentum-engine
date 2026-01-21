/**
 * Scene lighting setup - optimized for top-down view.
 *
 * Uses primarily ambient lighting for even illumination
 * with subtle directional light for depth.
 */
export function Lighting() {
  return (
    <>
      {/* Strong ambient light for even illumination of cards */}
      <ambientLight intensity={0.7} />

      {/* Main directional light from directly above */}
      <directionalLight
        position={[0, 15, 0]}
        intensity={0.5}
        castShadow
        shadow-mapSize-width={2048}
        shadow-mapSize-height={2048}
        shadow-camera-far={50}
        shadow-camera-left={-10}
        shadow-camera-right={10}
        shadow-camera-top={10}
        shadow-camera-bottom={-10}
      />

      {/* Subtle fill light */}
      <directionalLight
        position={[0, 10, 5]}
        intensity={0.2}
      />
    </>
  )
}
