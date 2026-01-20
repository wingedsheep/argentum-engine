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

/**
 * Main 3D scene containing the game table and all zones.
 */
export function GameScene() {
  const playerId = useGameStore((state) => state.playerId)
  const gameState = useGameStore((state) => state.gameState)

  // Get opponent ID
  const opponentId = gameState?.players.find((p) => p.playerId !== playerId)?.playerId ?? null

  return (
    <Canvas
      style={{ width: '100%', height: '100%' }}
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
            position={[0, 0.1, 3.5]}
            rotation={[0, 0, 0]}
            isOpponent={false}
          />
          <Library
            zoneId={library(playerId)}
            position={[4.5, 0, 2.5]}
          />
          <Graveyard
            zoneId={graveyard(playerId)}
            position={[4.5, 0, 1]}
          />
        </>
      )}

      {/* Opponent zones (top of screen) */}
      {opponentId && (
        <>
          <Hand
            zoneId={hand(opponentId)}
            position={[0, 0.1, -3.5]}
            rotation={[0, Math.PI, 0]}
            isOpponent={true}
          />
          <Library
            zoneId={library(opponentId)}
            position={[-4.5, 0, -2.5]}
          />
          <Graveyard
            zoneId={graveyard(opponentId)}
            position={[-4.5, 0, -1]}
          />
        </>
      )}

      {/* Battlefield (center) */}
      <Battlefield />

      {/* Stack (right side) */}
      <Stack position={[5.5, 0, 0]} />
    </Canvas>
  )
}
