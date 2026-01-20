/**
 * Scene lighting setup.
 *
 * Uses a combination of ambient light for base illumination
 * and directional lights for shadows and depth.
 */
export function Lighting() {
  return (
    <>
      {/* Ambient light for base illumination */}
      <ambientLight intensity={0.4} />

      {/* Main directional light (from above and slightly front) */}
      <directionalLight
        position={[5, 10, 5]}
        intensity={0.8}
        castShadow
        shadow-mapSize-width={2048}
        shadow-mapSize-height={2048}
        shadow-camera-far={50}
        shadow-camera-left={-10}
        shadow-camera-right={10}
        shadow-camera-top={10}
        shadow-camera-bottom={-10}
      />

      {/* Fill light from the opposite side */}
      <directionalLight
        position={[-5, 5, -5]}
        intensity={0.3}
      />

      {/* Subtle point light for card highlights */}
      <pointLight
        position={[0, 3, 0]}
        intensity={0.2}
        distance={10}
        decay={2}
      />
    </>
  )
}
