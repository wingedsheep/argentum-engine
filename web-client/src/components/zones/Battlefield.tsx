import { useMemo } from 'react'
import { Card3D } from '../card/Card3D'
import { useBattlefieldCards } from '../../store/selectors'
import {
  CARD_SCALES,
  ZONE_POSITIONS,
  calculateGridPositions,
} from './ZoneLayout'
import type { ClientCard } from '../../types'

interface BattlefieldRowProps {
  cards: readonly ClientCard[]
  basePosition: [number, number, number]
  flipZ?: boolean
}

/**
 * A single row of battlefield cards (lands or creatures).
 */
function BattlefieldRow({ cards, basePosition, flipZ = false }: BattlefieldRowProps) {
  const positions = useMemo(
    () => calculateGridPositions(cards.length),
    [cards.length]
  )

  return (
    <group position={basePosition}>
      {cards.map((card, index) => {
        const pos = positions[index]
        if (!pos) return null

        return (
          <Card3D
            key={card.id}
            card={card}
            position={[pos.x, 0, flipZ ? -pos.z : pos.z]}
            rotation={[0, flipZ ? Math.PI : 0, 0]}
            scale={CARD_SCALES.battlefield}
          />
        )
      })}
    </group>
  )
}

/**
 * The battlefield zone showing all permanents.
 *
 * Layout:
 * - Player lands: Bottom row
 * - Player creatures: Above player lands
 * - Opponent creatures: Below opponent lands
 * - Opponent lands: Top row
 */
export function Battlefield() {
  const {
    playerLands,
    playerCreatures,
    playerOther,
    opponentLands,
    opponentCreatures,
    opponentOther,
  } = useBattlefieldCards()

  return (
    <group>
      {/* Player's permanents */}
      <BattlefieldRow
        cards={playerLands}
        basePosition={ZONE_POSITIONS.playerLands}
      />
      <BattlefieldRow
        cards={playerCreatures}
        basePosition={ZONE_POSITIONS.playerCreatures}
      />
      {playerOther.length > 0 && (
        <BattlefieldRow
          cards={playerOther}
          basePosition={ZONE_POSITIONS.playerOther}
        />
      )}

      {/* Opponent's permanents */}
      <BattlefieldRow
        cards={opponentLands}
        basePosition={ZONE_POSITIONS.opponentLands}
        flipZ
      />
      <BattlefieldRow
        cards={opponentCreatures}
        basePosition={ZONE_POSITIONS.opponentCreatures}
        flipZ
      />
      {opponentOther.length > 0 && (
        <BattlefieldRow
          cards={opponentOther}
          basePosition={ZONE_POSITIONS.opponentOther}
          flipZ
        />
      )}
    </group>
  )
}
