import { useGameStore, type LogEntry } from '../../store/gameStore'
import React, { useState, useRef, useEffect } from 'react'

/**
 * Collapsible game log panel showing accumulated events.
 */
export function GameLog() {
  const eventLog = useGameStore((state) => state.eventLog)
  const playerId = useGameStore((state) => state.playerId)
  const [expanded, setExpanded] = useState(false)
  const scrollRef = useRef<HTMLDivElement>(null)

  // Auto-scroll to bottom when new entries arrive
  useEffect(() => {
    if (expanded && scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight
    }
  }, [eventLog.length, expanded])

  if (!expanded) {
    return (
      <button
        onClick={() => setExpanded(true)}
        style={styles.toggleButton}
      >
        Log ({eventLog.length})
      </button>
    )
  }

  return (
    <div style={styles.panel}>
      <div style={styles.header}>
        <span style={styles.headerTitle}>Game Log</span>
        <button onClick={() => setExpanded(false)} style={styles.closeButton}>
          &times;
        </button>
      </div>
      <div ref={scrollRef} style={styles.entries}>
        {eventLog.length === 0 && (
          <div style={styles.empty}>No events yet</div>
        )}
        {eventLog.map((entry, i) => (
          <LogEntryRow key={i} entry={entry} isPlayer={entry.playerId === playerId} />
        ))}
      </div>
    </div>
  )
}

function LogEntryRow({ entry, isPlayer }: { entry: LogEntry; isPlayer: boolean }) {
  const color = entry.playerId === null
    ? '#888'
    : isPlayer
      ? '#5bc0de'
      : '#e07050'

  return (
    <div style={{ ...styles.entry, color }}>
      {entry.description}
    </div>
  )
}

const styles: Record<string, React.CSSProperties> = {
  toggleButton: {
    position: 'fixed',
    bottom: 12,
    left: 12,
    zIndex: 500,
    padding: '6px 12px',
    fontSize: 12,
    backgroundColor: 'rgba(20, 20, 40, 0.85)',
    color: '#aaa',
    border: '1px solid #444',
    borderRadius: 6,
    cursor: 'pointer',
  },
  panel: {
    position: 'fixed',
    bottom: 12,
    left: 12,
    zIndex: 500,
    width: 'min(320px, calc(100vw - 24px))',
    maxHeight: 300,
    display: 'flex',
    flexDirection: 'column',
    backgroundColor: 'rgba(10, 10, 25, 0.92)',
    border: '1px solid #333',
    borderRadius: 8,
    overflow: 'hidden',
  },
  header: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: '6px 10px',
    borderBottom: '1px solid #333',
  },
  headerTitle: {
    color: '#aaa',
    fontSize: 12,
    fontWeight: 600,
    textTransform: 'uppercase',
    letterSpacing: 1,
  },
  closeButton: {
    background: 'none',
    border: 'none',
    color: '#888',
    fontSize: 18,
    cursor: 'pointer',
    padding: '0 4px',
    lineHeight: 1,
  },
  entries: {
    flex: 1,
    overflowY: 'auto',
    padding: '4px 8px',
    maxHeight: 260,
  },
  empty: {
    color: '#555',
    fontSize: 12,
    padding: 8,
    textAlign: 'center',
  },
  entry: {
    fontSize: 12,
    padding: '2px 0',
    lineHeight: 1.4,
    borderBottom: '1px solid rgba(255,255,255,0.04)',
  },
}
