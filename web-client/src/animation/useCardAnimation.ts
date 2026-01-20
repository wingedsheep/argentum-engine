import { useRef, useEffect } from 'react'
import { useFrame } from '@react-three/fiber'
import type { Vector3Tuple } from 'three'
import type { EntityId } from '../types'
import {
  useAnimationStore,
  getAnimatedValues,
  easings,
  type EasingFunction,
} from '../store/animationStore'

interface UseCardAnimationOptions {
  entityId: EntityId
  targetPosition: Vector3Tuple
  targetRotation: Vector3Tuple
  targetScale: number
  duration?: number
  easing?: EasingFunction
  enabled?: boolean
}

interface AnimatedValues {
  position: Vector3Tuple
  rotation: Vector3Tuple
  scale: number
  isAnimating: boolean
}

/**
 * Hook for animating a card's position, rotation, and scale.
 *
 * Automatically starts an animation when target values change.
 * Returns current interpolated values for rendering.
 */
export function useCardAnimation({
  entityId,
  targetPosition,
  targetRotation,
  targetScale,
  duration = 300,
  easing = easings.easeOutCubic,
  enabled = true,
}: UseCardAnimationOptions): AnimatedValues {
  const currentValuesRef = useRef<AnimatedValues>({
    position: targetPosition,
    rotation: targetRotation,
    scale: targetScale,
    isAnimating: false,
  })

  const prevTargetRef = useRef({
    position: targetPosition,
    rotation: targetRotation,
    scale: targetScale,
  })

  const startAnimation = useAnimationStore((state) => state.startAnimation)

  // Check if target has changed
  useEffect(() => {
    if (!enabled) return

    const prev = prevTargetRef.current
    const posChanged =
      prev.position[0] !== targetPosition[0] ||
      prev.position[1] !== targetPosition[1] ||
      prev.position[2] !== targetPosition[2]
    const rotChanged =
      prev.rotation[0] !== targetRotation[0] ||
      prev.rotation[1] !== targetRotation[1] ||
      prev.rotation[2] !== targetRotation[2]
    const scaleChanged = prev.scale !== targetScale

    if (posChanged || rotChanged || scaleChanged) {
      // Start animation from current position to new target
      startAnimation(
        entityId,
        {
          position: currentValuesRef.current.position,
          rotation: currentValuesRef.current.rotation,
          scale: currentValuesRef.current.scale,
        },
        {
          position: targetPosition,
          rotation: targetRotation,
          scale: targetScale,
        },
        duration,
        easing
      )

      prevTargetRef.current = {
        position: targetPosition,
        rotation: targetRotation,
        scale: targetScale,
      }
    }
  }, [
    entityId,
    targetPosition,
    targetRotation,
    targetScale,
    duration,
    easing,
    enabled,
    startAnimation,
  ])

  // Update current values on each frame
  useFrame(() => {
    const animated = getAnimatedValues(entityId, performance.now(), {
      position: targetPosition,
      rotation: targetRotation,
      scale: targetScale,
    })
    currentValuesRef.current = animated
  })

  return currentValuesRef.current
}

/**
 * Hook for tracking if any cards are currently animating.
 */
export function useIsAnyAnimating(): boolean {
  const animations = useAnimationStore((state) => state.animations)
  return animations.size > 0
}

/**
 * Hook for checking if a specific card is animating.
 */
export function useIsAnimating(entityId: EntityId): boolean {
  return useAnimationStore((state) => state.isAnimating(entityId))
}
