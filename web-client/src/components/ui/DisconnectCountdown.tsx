import { useEffect, useState } from 'react'
import { useGameStore } from '../../store/gameStore'

/**
 * Displays a countdown when the opponent has disconnected.
 * The server sends the initial seconds remaining; we count down locally.
 */
export function DisconnectCountdown() {
  const serverSeconds = useGameStore((s) => s.opponentDisconnectCountdown)
  const opponentName = useGameStore((s) => s.opponentName)
  const [localSeconds, setLocalSeconds] = useState<number | null>(null)

  // Sync local countdown with server value
  useEffect(() => {
    if (serverSeconds != null) {
      setLocalSeconds(serverSeconds)
    } else {
      setLocalSeconds(null)
    }
  }, [serverSeconds])

  // Tick down every second
  useEffect(() => {
    if (localSeconds == null || localSeconds <= 0) return
    const timer = setTimeout(() => {
      setLocalSeconds((s) => (s != null && s > 0 ? s - 1 : s))
    }, 1000)
    return () => clearTimeout(timer)
  }, [localSeconds])

  if (localSeconds == null) return null

  const minutes = Math.floor(localSeconds / 60)
  const seconds = localSeconds % 60
  const timeStr = `${minutes}:${seconds.toString().padStart(2, '0')}`
  const isUrgent = localSeconds <= 30

  return (
    <div style={styles.container}>
      <div style={{
        ...styles.icon,
        color: isUrgent ? '#ff4444' : '#ffc107',
      }}>
        !
      </div>
      <div>
        <div style={{
          ...styles.text,
          color: isUrgent ? '#ff4444' : '#ffc107',
        }}>
          {opponentName ?? 'Opponent'} disconnected
        </div>
        <div style={{
          ...styles.countdown,
          color: isUrgent ? '#ff6666' : '#ccc',
        }}>
          Auto-concedes in {timeStr}
        </div>
      </div>
    </div>
  )
}

const styles: Record<string, React.CSSProperties> = {
  container: {
    position: 'absolute',
    top: 80,
    left: '50%',
    transform: 'translateX(-50%)',
    backgroundColor: 'rgba(0, 0, 0, 0.9)',
    border: '1px solid #ffc107',
    borderRadius: 8,
    padding: '10px 20px',
    display: 'flex',
    alignItems: 'center',
    gap: 12,
    zIndex: 100,
    pointerEvents: 'none',
  },
  icon: {
    width: 24,
    height: 24,
    borderRadius: '50%',
    border: '2px solid currentColor',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    fontWeight: 'bold',
    fontSize: 14,
  },
  text: {
    fontWeight: 500,
  },
  countdown: {
    fontSize: '0.85em',
  },
}
