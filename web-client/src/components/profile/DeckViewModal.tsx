/**
 * Modal that shows both seats' decklists for one finished game from the user's history. Opened from
 * the recent-games table so a player can review what they (and their opponent) actually played. The
 * decks come straight from the recorded match — the server only returns games the user took part in.
 */
import { useEffect, useState } from 'react'
import type React from 'react'
import { type GameDecks, fetchGameDecks } from '@/api/account'
import { colorForIdentity, colorLabel } from '@/components/admin/statFormat'

export function DeckViewModal({
  gameId,
  opponentLabel,
  onClose,
}: {
  gameId: string
  opponentLabel: string
  onClose: () => void
}) {
  const [decks, setDecks] = useState<GameDecks | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let live = true
    setDecks(null)
    setError(null)
    fetchGameDecks(gameId)
      .then((d) => live && setDecks(d))
      .catch(() => live && setError('Could not load this game’s decks.'))
    return () => {
      live = false
    }
  }, [gameId])

  return (
    <div style={styles.backdrop} onClick={onClose} role="presentation">
      <div style={styles.modal} onClick={(e) => e.stopPropagation()} role="dialog" aria-modal>
        <div style={styles.header}>
          <h2 style={styles.title}>Game decks</h2>
          <button type="button" style={styles.close} onClick={onClose} aria-label="Close">
            ✕
          </button>
        </div>
        <p style={styles.muted}>vs {opponentLabel}</p>

        {error ? (
          <p style={styles.error}>{error}</p>
        ) : !decks ? (
          <p style={styles.muted}>Loading…</p>
        ) : decks.participants.length === 0 ? (
          <p style={styles.muted}>No decklist was recorded for this game.</p>
        ) : (
          <div style={styles.columns}>
            {decks.participants.map((p, i) => (
              <DeckColumn key={`${p.playerName}-${i}`} p={p} />
            ))}
          </div>
        )}
      </div>
    </div>
  )
}

function DeckColumn({ p }: { p: GameDecks['participants'][number] }) {
  const total = p.cards.reduce((sum, c) => sum + c.copies, 0)
  return (
    <div style={styles.col}>
      <div style={styles.colHead}>
        <span style={styles.colName}>
          {p.isSelf ? 'You' : p.playerName}
          {p.isAi ? <span style={styles.aiTag}> AI</span> : null}
        </span>
        <span style={{ ...styles.resultTag, color: p.won ? '#5bd16e' : '#e15b6e' }}>
          {p.won ? 'Win' : 'Loss'}
        </span>
      </div>
      <div style={styles.colMeta}>
        <span style={{ ...styles.colorDot, backgroundColor: colorForIdentity(p.colors) }} />
        <span>{colorLabel(p.colors)}</span>
        <span style={styles.dim}>· {total} cards</span>
      </div>
      {p.cards.length === 0 ? (
        <p style={styles.muted}>Deck not recorded.</p>
      ) : (
        <ul style={styles.cardList}>
          {p.cards.map((c) => (
            <li key={c.cardName} style={styles.cardRow}>
              <span style={styles.copies}>{c.copies}×</span>
              <span style={styles.cardName}>{c.cardName}</span>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}

const styles: Record<string, React.CSSProperties> = {
  backdrop: {
    position: 'fixed',
    inset: 0,
    backgroundColor: 'rgba(0,0,0,0.6)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    padding: 16,
    zIndex: 1000,
  },
  modal: {
    width: 'min(760px, 100%)',
    maxHeight: '85vh',
    overflowY: 'auto',
    backgroundColor: '#12121e',
    border: '1px solid #2a2a3e',
    borderRadius: 14,
    padding: '20px 22px',
  },
  header: { display: 'flex', justifyContent: 'space-between', alignItems: 'center' },
  title: { margin: 0, color: '#fff', fontSize: 20 },
  close: { background: 'none', border: 'none', color: '#999', cursor: 'pointer', fontSize: 18 },
  muted: { margin: '4px 0 0', color: '#888', fontSize: 13 },
  error: { margin: '8px 0 0', color: '#ff6b6b', fontSize: 13 },
  columns: { display: 'flex', gap: 16, marginTop: 14, flexWrap: 'wrap' },
  col: {
    flex: '1 1 300px',
    minWidth: 0,
    backgroundColor: '#171723',
    border: '1px solid #2a2a3e',
    borderRadius: 10,
    padding: '12px 14px',
  },
  colHead: { display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', gap: 8 },
  colName: { color: '#fff', fontSize: 15, fontWeight: 600 },
  resultTag: { fontSize: 12, fontWeight: 700 },
  aiTag: { color: '#888', fontSize: 11 },
  colMeta: { display: 'flex', alignItems: 'center', gap: 6, color: '#bbb', fontSize: 12, margin: '6px 0 10px' },
  colorDot: { width: 10, height: 10, borderRadius: 999, display: 'inline-block' },
  dim: { color: '#777' },
  cardList: { listStyle: 'none', margin: 0, padding: 0, display: 'flex', flexDirection: 'column', gap: 2 },
  cardRow: { display: 'flex', gap: 8, fontSize: 13, color: '#cdd' },
  copies: { color: '#8b9bff', fontVariantNumeric: 'tabular-nums', minWidth: 24 },
  cardName: { color: '#cdd' },
}
