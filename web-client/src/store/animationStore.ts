import { create } from 'zustand'
import type { EntityId } from '../types'
import type { Vector3Tuple } from 'three'

/**
 * Animation target for a card.
 */
export interface CardAnimationTarget {
  entityId: EntityId
  position: Vector3Tuple
  rotation: Vector3Tuple
  scale: number
  duration: number
  delay: number
}

/**
 * Active animation state.
 */
export interface ActiveAnimation {
  entityId: EntityId
  startPosition: Vector3Tuple
  endPosition: Vector3Tuple
  startRotation: Vector3Tuple
  endRotation: Vector3Tuple
  startScale: number
  endScale: number
  startTime: number
  duration: number
  easing: EasingFunction
}

/**
 * Easing function type.
 */
export type EasingFunction = (t: number) => number

/**
 * Common easing functions.
 */
export const easings = {
  linear: (t: number) => t,
  easeInQuad: (t: number) => t * t,
  easeOutQuad: (t: number) => t * (2 - t),
  easeInOutQuad: (t: number) => (t < 0.5 ? 2 * t * t : -1 + (4 - 2 * t) * t),
  easeOutCubic: (t: number) => --t * t * t + 1,
  easeInOutCubic: (t: number) =>
    t < 0.5 ? 4 * t * t * t : (t - 1) * (2 * t - 2) * (2 * t - 2) + 1,
  easeOutBack: (t: number) => {
    const c1 = 1.70158
    const c3 = c1 + 1
    return 1 + c3 * Math.pow(t - 1, 3) + c1 * Math.pow(t - 1, 2)
  },
} as const

/**
 * Animation store for managing card animations.
 */
interface AnimationStore {
  // Active animations
  animations: Map<EntityId, ActiveAnimation>

  // Actions
  startAnimation: (
    entityId: EntityId,
    from: { position: Vector3Tuple; rotation: Vector3Tuple; scale: number },
    to: { position: Vector3Tuple; rotation: Vector3Tuple; scale: number },
    duration: number,
    easing?: EasingFunction
  ) => void
  cancelAnimation: (entityId: EntityId) => void
  clearAllAnimations: () => void
  getAnimationProgress: (entityId: EntityId, currentTime: number) => number | null
  isAnimating: (entityId: EntityId) => boolean
}

export const useAnimationStore = create<AnimationStore>((set, get) => ({
  animations: new Map(),

  startAnimation: (entityId, from, to, duration, easing = easings.easeOutCubic) => {
    set((state) => {
      const newAnimations = new Map(state.animations)
      newAnimations.set(entityId, {
        entityId,
        startPosition: from.position,
        endPosition: to.position,
        startRotation: from.rotation,
        endRotation: to.rotation,
        startScale: from.scale,
        endScale: to.scale,
        startTime: performance.now(),
        duration,
        easing,
      })
      return { animations: newAnimations }
    })
  },

  cancelAnimation: (entityId) => {
    set((state) => {
      const newAnimations = new Map(state.animations)
      newAnimations.delete(entityId)
      return { animations: newAnimations }
    })
  },

  clearAllAnimations: () => {
    set({ animations: new Map() })
  },

  getAnimationProgress: (entityId, currentTime) => {
    const animation = get().animations.get(entityId)
    if (!animation) return null

    const elapsed = currentTime - animation.startTime
    const rawProgress = Math.min(1, elapsed / animation.duration)
    return animation.easing(rawProgress)
  },

  isAnimating: (entityId) => {
    return get().animations.has(entityId)
  },
}))

/**
 * Interpolate between two values.
 */
export function lerp(a: number, b: number, t: number): number {
  return a + (b - a) * t
}

/**
 * Interpolate between two vectors.
 */
export function lerpVector3(
  a: Vector3Tuple,
  b: Vector3Tuple,
  t: number
): Vector3Tuple {
  return [lerp(a[0], b[0], t), lerp(a[1], b[1], t), lerp(a[2], b[2], t)]
}

/**
 * Get current animation values for an entity.
 */
export function getAnimatedValues(
  entityId: EntityId,
  currentTime: number,
  defaultValues: {
    position: Vector3Tuple
    rotation: Vector3Tuple
    scale: number
  }
): {
  position: Vector3Tuple
  rotation: Vector3Tuple
  scale: number
  isAnimating: boolean
} {
  const store = useAnimationStore.getState()
  const animation = store.animations.get(entityId)

  if (!animation) {
    return { ...defaultValues, isAnimating: false }
  }

  const elapsed = currentTime - animation.startTime
  const rawProgress = Math.min(1, elapsed / animation.duration)
  const progress = animation.easing(rawProgress)

  // Animation complete - remove it and return end values
  if (rawProgress >= 1) {
    store.cancelAnimation(entityId)
    return {
      position: animation.endPosition,
      rotation: animation.endRotation,
      scale: animation.endScale,
      isAnimating: false,
    }
  }

  return {
    position: lerpVector3(animation.startPosition, animation.endPosition, progress),
    rotation: lerpVector3(animation.startRotation, animation.endRotation, progress),
    scale: lerp(animation.startScale, animation.endScale, progress),
    isAnimating: true,
  }
}
