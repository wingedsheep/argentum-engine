import { Canvas } from '@react-three/fiber'
import { Camera } from './Camera'
import { Lighting } from './Lighting'
import { Table } from './Table'
import { Battlefield } from '../zones/Battlefield'
import { Hand } from '../zones/Hand'
import { Library } from '../zones/Library'
import { Graveyard } from '../zones/Graveyard'
import { Stack } from '../zones/Stack'
import { useGameStore } from '../../store/gameStore'
import { hand, library, graveyard } from '../../types'
import { ZONE_POSITIONS } from '../zones/ZoneLayout'

/**
 * Main scene containing the game board - MTG Arena style layout.
 */
export function GameScene() {
  const playerId = useGameStore((state) => state.playerId)
  const gameState = useGameStore((state) => state.gameState)

  // Get opponent ID
  const opponentId = gameState?.players.find((p) => p.playerId !== playerId)?.playerId ?? null

  return (
    <Canvas
      style={{ width: '100%', height: '100%', background: '#0a0a15' }}
      gl={{ antialias: true, alpha: false }}
      dpr={[1, 2]}
    >
      <Camera />
      <Lighting />
      <Table />

      {/* Player zones (bottom of screen) */}
      {playerId && (
        <>
          <Hand
            zoneId={hand(playerId)}
            position={ZONE_POSITIONS.playerHand}
            rotation={[0, 0, 0]}
            isOpponent={false}
          />
          <Library
            zoneId={library(playerId)}
            position={ZONE_POSITIONS.playerLibrary}
          />
          <Graveyard
            zoneId={graveyard(playerId)}
            position={ZONE_POSITIONS.playerGraveyard}
          />
        </>
      )}

      {/* Opponent zones (top of screen) */}
      {opponentId && (
        <>
          <Hand
            zoneId={hand(opponentId)}
            position={ZONE_POSITIONS.opponentHand}
            rotation={[0, Math.PI, 0]}
            isOpponent={true}
          />
          <Library
            zoneId={library(opponentId)}
            position={ZONE_POSITIONS.opponentLibrary}
          />
          <Graveyard
            zoneId={graveyard(opponentId)}
            position={ZONE_POSITIONS.opponentGraveyard}
          />
        </>
      )}

      {/* Battlefield (center) */}
      <Battlefield />

      {/* Stack (right side) */}
      <Stack position={ZONE_POSITIONS.stack} />
    </Canvas>
  )
}
