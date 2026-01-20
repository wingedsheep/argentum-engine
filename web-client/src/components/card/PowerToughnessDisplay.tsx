import { Text } from '@react-three/drei'

interface PowerToughnessDisplayProps {
  power: number
  toughness: number
  damage: number | null
  position: [number, number, number]
  scale: number
}

/**
 * Power/Toughness display for creature cards.
 *
 * Displays power/toughness in the bottom-right corner of the card.
 * Changes color when damaged.
 */
export function PowerToughnessDisplay({
  power,
  toughness,
  damage,
  position,
  scale,
}: PowerToughnessDisplayProps) {
  const effectiveToughness = toughness - (damage ?? 0)
  const isDamaged = (damage ?? 0) > 0

  // Color based on damage state
  const textColor = isDamaged ? '#ff4444' : '#ffffff'
  const bgColor = isDamaged ? '#440000' : '#000000'

  return (
    <group position={position} rotation={[-Math.PI / 2, 0, 0]}>
      {/* Background box */}
      <mesh position={[0, 0, -0.001]}>
        <planeGeometry args={[0.15 * scale, 0.08 * scale]} />
        <meshBasicMaterial color={bgColor} transparent opacity={0.8} />
      </mesh>

      {/* P/T text */}
      <Text
        fontSize={0.05 * scale}
        color={textColor}
        anchorX="center"
        anchorY="middle"
        fontWeight="bold"
      >
        {`${power}/${effectiveToughness}`}
      </Text>
    </group>
  )
}
