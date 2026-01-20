import { useMemo } from 'react'
import { Line } from '@react-three/drei'
import type { Vector3Tuple } from 'three'
import * as THREE from 'three'

interface TargetArrowProps {
  start: Vector3Tuple
  end: Vector3Tuple
  color?: string
  animated?: boolean
}

/**
 * Arrow connecting a source to a target.
 *
 * Uses a quadratic bezier curve with an arrowhead at the end.
 */
export function TargetArrow({
  start,
  end,
  color = '#ff00ff',
}: TargetArrowProps) {
  // Calculate control point for bezier curve (arc upward)
  const controlPoint: Vector3Tuple = useMemo(() => {
    const midX = (start[0] + end[0]) / 2
    const midZ = (start[2] + end[2]) / 2
    const distance = Math.sqrt(
      Math.pow(end[0] - start[0], 2) + Math.pow(end[2] - start[2], 2)
    )
    const arcHeight = Math.min(distance * 0.3, 1.5)

    return [midX, start[1] + arcHeight, midZ]
  }, [start, end])

  // Generate points along the bezier curve
  const curvePoints = useMemo(() => {
    const points: THREE.Vector3[] = []
    const segments = 32

    for (let i = 0; i <= segments; i++) {
      const t = i / segments
      const oneMinusT = 1 - t

      const x =
        oneMinusT * oneMinusT * start[0] +
        2 * oneMinusT * t * controlPoint[0] +
        t * t * end[0]
      const y =
        oneMinusT * oneMinusT * start[1] +
        2 * oneMinusT * t * controlPoint[1] +
        t * t * end[1]
      const z =
        oneMinusT * oneMinusT * start[2] +
        2 * oneMinusT * t * controlPoint[2] +
        t * t * end[2]

      points.push(new THREE.Vector3(x, y, z))
    }

    return points
  }, [start, end, controlPoint])

  // Calculate arrowhead direction
  const arrowDirection = useMemo(() => {
    if (curvePoints.length < 2) return new THREE.Vector3(0, 0, -1)

    const lastPoint = curvePoints[curvePoints.length - 1]!
    const secondLast = curvePoints[curvePoints.length - 2]!
    return new THREE.Vector3()
      .subVectors(lastPoint, secondLast)
      .normalize()
  }, [curvePoints])

  // Arrowhead points
  const arrowheadPoints = useMemo(() => {
    const tip = new THREE.Vector3(...end)
    const size = 0.15
    const angle = Math.PI / 6 // 30 degrees

    // Create rotation matrix from arrow direction
    const quaternion = new THREE.Quaternion().setFromUnitVectors(
      new THREE.Vector3(0, 0, 1),
      arrowDirection
    )

    // Two points for the arrowhead
    const left = new THREE.Vector3(-Math.sin(angle) * size, 0, -Math.cos(angle) * size)
    const right = new THREE.Vector3(Math.sin(angle) * size, 0, -Math.cos(angle) * size)

    left.applyQuaternion(quaternion).add(tip)
    right.applyQuaternion(quaternion).add(tip)

    return [
      [left, tip],
      [right, tip],
    ] as const
  }, [end, arrowDirection])

  return (
    <group>
      {/* Main curve */}
      <Line
        points={curvePoints}
        color={color}
        lineWidth={3}
        transparent
        opacity={0.8}
      />

      {/* Arrowhead lines */}
      <Line
        points={arrowheadPoints[0]}
        color={color}
        lineWidth={3}
        transparent
        opacity={0.8}
      />
      <Line
        points={arrowheadPoints[1]}
        color={color}
        lineWidth={3}
        transparent
        opacity={0.8}
      />

      {/* Glow effect */}
      <Line
        points={curvePoints}
        color={color}
        lineWidth={8}
        transparent
        opacity={0.2}
      />
    </group>
  )
}

/**
 * Dashed arrow for indicating potential targets.
 */
export function DashedTargetArrow({
  start,
  end,
  color = '#888888',
}: TargetArrowProps) {
  const midPoint: Vector3Tuple = [
    (start[0] + end[0]) / 2,
    Math.max(start[1], end[1]) + 0.5,
    (start[2] + end[2]) / 2,
  ]

  return (
    <group>
      <Line
        points={[start, midPoint, end]}
        color={color}
        lineWidth={2}
        dashed
        dashScale={10}
        dashSize={0.1}
        gapSize={0.05}
        transparent
        opacity={0.5}
      />
    </group>
  )
}
