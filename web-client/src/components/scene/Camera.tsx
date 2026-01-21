import { OrthographicCamera } from '@react-three/drei'

/**
 * Camera configuration for the game view.
 *
 * Uses an orthographic camera for a flat, top-down view similar to MTG Arena.
 * The player's hand and battlefield are at the bottom, opponent's at the top.
 */
export function Camera() {
  // Using orthographic for consistent card sizing regardless of depth
  // Higher zoom = objects appear larger on screen
  // The playing area spans about -6 to +6 in Z (vertical on screen)
  return (
    <OrthographicCamera
      makeDefault
      position={[0, 10, 0]}
      rotation={[-Math.PI / 2, 0, 0]}
      zoom={120}
      near={0.1}
      far={100}
    />
  )
}
