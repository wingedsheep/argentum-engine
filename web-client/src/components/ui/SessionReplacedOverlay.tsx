import { useState } from 'react'
import { useGameStore } from '@/store/gameStore.ts'

/**
 * Full-screen overlay shown when the server reports this tab's session was taken over
 * by another tab or device. Auto-reconnect is stopped at that point (reconnecting would
 * steal the session straight back and the tabs would fight); "Use here" reclaims it
 * explicitly — the other tab then gets this same overlay.
 */
export function SessionReplacedOverlay() {
  const sessionReplaced = useGameStore((s) => s.sessionReplaced)
  const connect = useGameStore((s) => s.connect)
  const [reclaiming, setReclaiming] = useState(false)

  if (!sessionReplaced) return null

  const storedName = localStorage.getItem('argentum-player-name')

  const reclaim = () => {
    if (!storedName) return
    setReclaiming(true)
    connect(storedName)
  }

  return (
    <div style={styles.backdrop}>
      <div style={styles.dialog}>
        <div style={styles.title}>Opened in another tab</div>
        <div style={styles.text}>
          Your session is now active in a different tab or device.
          {storedName ? ' You can take it back and continue playing here.' : ' Refresh the page to continue here.'}
        </div>
        {storedName && (
          <button style={styles.button} onClick={reclaim} disabled={reclaiming}>
            {reclaiming ? 'Reconnecting…' : 'Use here'}
          </button>
        )}
      </div>
    </div>
  )
}

const styles: Record<string, React.CSSProperties> = {
  backdrop: {
    position: 'fixed',
    inset: 0,
    backgroundColor: 'rgba(0, 0, 0, 0.75)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    zIndex: 1000,
  },
  dialog: {
    backgroundColor: 'rgba(20, 20, 28, 0.97)',
    border: '1px solid #ffc107',
    borderRadius: 12,
    padding: '28px 36px',
    maxWidth: 380,
    textAlign: 'center',
    display: 'flex',
    flexDirection: 'column',
    gap: 14,
    alignItems: 'center',
  },
  title: {
    fontSize: '1.2em',
    fontWeight: 600,
    color: '#ffc107',
  },
  text: {
    color: '#ccc',
    lineHeight: 1.5,
  },
  button: {
    padding: '10px 28px',
    borderRadius: 8,
    border: 'none',
    backgroundColor: '#ffc107',
    color: '#1a1a22',
    fontWeight: 600,
    fontSize: '1em',
    cursor: 'pointer',
  },
}
