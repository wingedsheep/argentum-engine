import { useRef } from 'react'
import { Group } from 'three'
import type { Vector3Tuple } from 'three'
import { Card3D, type CardHighlightType } from '../components/card/Card3D'
import type { ClientCard } from '../types'
import { useCardAnimation } from './useCardAnimation'

interface AnimatedCardProps {
  card: ClientCard
  targetPosition: Vector3Tuple
  targetRotation?: Vector3Tuple
  targetScale?: number
  duration?: number
  faceDown?: boolean
  interactive?: boolean
  hoverLift?: boolean
  highlight?: CardHighlightType
}

/**
 * A card with smooth position/rotation/scale animations.
 *
 * Wraps Card3D and automatically animates when target values change.
 */
export function AnimatedCard({
  card,
  targetPosition,
  targetRotation = [0, 0, 0],
  targetScale = 1,
  duration = 300,
  faceDown = false,
  interactive = true,
  hoverLift = false,
  highlight,
}: AnimatedCardProps) {
  const groupRef = useRef<Group>(null)

  const { position, rotation, scale, isAnimating } = useCardAnimation({
    entityId: card.id,
    targetPosition,
    targetRotation,
    targetScale,
    duration,
  })

  // During animation, disable hover lift to prevent jittering
  const effectiveHoverLift = hoverLift && !isAnimating

  return (
    <group ref={groupRef}>
      <Card3D
        card={card}
        position={position}
        rotation={rotation}
        scale={scale}
        faceDown={faceDown}
        interactive={interactive}
        hoverLift={effectiveHoverLift}
        highlight={highlight}
      />
    </group>
  )
}

/**
 * Animate a card along a bezier curve path.
 * Useful for spell cast animations, damage arcs, etc.
 */
interface BezierAnimatedCardProps extends Omit<AnimatedCardProps, 'targetPosition'> {
  startPosition: Vector3Tuple
  controlPoint: Vector3Tuple
  endPosition: Vector3Tuple
  progress: number // 0 to 1
}

/**
 * Calculate a point on a quadratic bezier curve.
 */
function quadraticBezier(
  p0: Vector3Tuple,
  p1: Vector3Tuple,
  p2: Vector3Tuple,
  t: number
): Vector3Tuple {
  const oneMinusT = 1 - t
  return [
    oneMinusT * oneMinusT * p0[0] + 2 * oneMinusT * t * p1[0] + t * t * p2[0],
    oneMinusT * oneMinusT * p0[1] + 2 * oneMinusT * t * p1[1] + t * t * p2[1],
    oneMinusT * oneMinusT * p0[2] + 2 * oneMinusT * t * p1[2] + t * t * p2[2],
  ]
}

export function BezierAnimatedCard({
  card,
  startPosition,
  controlPoint,
  endPosition,
  progress,
  ...rest
}: BezierAnimatedCardProps) {
  const position = quadraticBezier(startPosition, controlPoint, endPosition, progress)

  return (
    <Card3D
      card={card}
      position={position}
      {...rest}
    />
  )
}
