import { useEffect, useRef } from 'react'
import { GameScene } from './components/scene/GameScene'
import { GameUI } from './components/ui/GameUI'
import { MulliganUI } from './components/mulligan/MulliganUI'
import { useGameStore } from './store/gameStore'

export default function App() {
  const connectionStatus = useGameStore((state) => state.connectionStatus)
  const mulliganState = useGameStore((state) => state.mulliganState)
  const connect = useGameStore((state) => state.connect)
  const hasConnectedRef = useRef(false)

  useEffect(() => {
    // Auto-connect on mount (in real app, would have login flow)
    // Use ref to prevent multiple connection attempts from Strict Mode
    if (connectionStatus === 'disconnected' && !hasConnectedRef.current) {
      hasConnectedRef.current = true
      connect('Player')
    }
  }, [connectionStatus, connect])

  return (
    <div style={{ width: '100%', height: '100%', position: 'relative' }}>
      <GameScene />
      <GameUI />
      {mulliganState && <MulliganUI />}
    </div>
  )
}
