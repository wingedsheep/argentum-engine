import { PerspectiveCamera, OrbitControls } from '@react-three/drei'

/**
 * Camera configuration for the game view.
 *
 * The camera is positioned to give an angled top-down view of the table,
 * similar to MTG Arena's perspective. The player's hand and battlefield
 * are at the bottom, opponent's at the top.
 */
export function Camera() {
  return (
    <>
      <PerspectiveCamera
        makeDefault
        position={[0, 8, 6]}
        fov={50}
        near={0.1}
        far={100}
      />
      <OrbitControls
        enablePan={false}
        enableZoom={true}
        enableRotate={true}
        minDistance={5}
        maxDistance={15}
        minPolarAngle={Math.PI / 6}
        maxPolarAngle={Math.PI / 2.5}
        target={[0, 0, 0]}
      />
    </>
  )
}

/**
 * Alternative fixed camera without orbit controls.
 * Use this for production if orbital camera is not desired.
 */
export function FixedCamera() {
  return (
    <PerspectiveCamera
      makeDefault
      position={[0, 8, 6]}
      fov={50}
      near={0.1}
      far={100}
      rotation={[-0.8, 0, 0]}
    />
  )
}
