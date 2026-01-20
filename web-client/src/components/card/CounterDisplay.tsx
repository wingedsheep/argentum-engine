import { Text } from '@react-three/drei'
import { CounterType } from '../../types'

interface CounterDisplayProps {
  counters: Partial<Record<CounterType, number>>
  position: [number, number, number]
  scale: number
}

/**
 * Counter colors by type.
 */
const COUNTER_COLORS: Record<CounterType, string> = {
  [CounterType.PLUS_ONE_PLUS_ONE]: '#00ff00',
  [CounterType.MINUS_ONE_MINUS_ONE]: '#ff0000',
  [CounterType.LOYALTY]: '#0088ff',
  [CounterType.CHARGE]: '#ffff00',
  [CounterType.POISON]: '#00ff88',
}

/**
 * Counter icons/symbols.
 */
const COUNTER_SYMBOLS: Record<CounterType, string> = {
  [CounterType.PLUS_ONE_PLUS_ONE]: '+1',
  [CounterType.MINUS_ONE_MINUS_ONE]: '-1',
  [CounterType.LOYALTY]: 'L',
  [CounterType.CHARGE]: 'C',
  [CounterType.POISON]: 'P',
}

/**
 * Counter display for permanents.
 *
 * Shows counter types and quantities on the card.
 */
export function CounterDisplay({ counters, position, scale }: CounterDisplayProps) {
  const counterEntries = Object.entries(counters).filter(
    ([_, count]) => count !== undefined && count > 0
  ) as Array<[CounterType, number]>

  if (counterEntries.length === 0) return null

  return (
    <group position={position} rotation={[-Math.PI / 2, 0, 0]}>
      {counterEntries.map(([type, count], index) => {
        const color = COUNTER_COLORS[type] ?? '#ffffff'
        const symbol = COUNTER_SYMBOLS[type] ?? '?'
        const yOffset = index * 0.1 * scale

        return (
          <group key={type} position={[0, yOffset, 0]}>
            {/* Counter background circle */}
            <mesh position={[0, 0, -0.001]}>
              <circleGeometry args={[0.04 * scale, 16]} />
              <meshBasicMaterial color={color} transparent opacity={0.9} />
            </mesh>

            {/* Counter symbol */}
            <Text
              fontSize={0.03 * scale}
              color="#000000"
              anchorX="center"
              anchorY="middle"
              fontWeight="bold"
            >
              {symbol}
            </Text>

            {/* Counter count */}
            {count > 1 && (
              <group position={[0.05 * scale, -0.02 * scale, 0]}>
                <mesh position={[0, 0, -0.001]}>
                  <circleGeometry args={[0.025 * scale, 12]} />
                  <meshBasicMaterial color="#000000" transparent opacity={0.8} />
                </mesh>
                <Text
                  fontSize={0.02 * scale}
                  color="#ffffff"
                  anchorX="center"
                  anchorY="middle"
                >
                  {count.toString()}
                </Text>
              </group>
            )}
          </group>
        )
      })}
    </group>
  )
}
