/**
 * Admin-side deck viewers for the player detail screen: a modal that shows both seats' decks for one
 * of the player's games, and a panel that lists the account's saved decks (each openable). Both reuse
 * the shared, polished {@link GameDeckColumns}/{@link DeckCardBody} renderer — the same one the
 * player's own profile uses — so there's a single deck look across the app. Data comes from the
 * admin endpoints (`/api/admin/users/{id}/games/{gameId}/decks` and `.../decks`).
 */
import { useEffect, useRef, useState } from 'react'
import type React from 'react'
import {
  type AdminUserDeck,
  fetchUserGameDecks,
  fetchUserSavedDecks,
} from '@/api/adminUsers'
import type { AdminAuth } from '@/api/adminAuth'
import type { GameDecks } from '@/api/account'
import { DeckCardBody, GameDeckColumns } from '@/components/deck/GameDeckView'
import { Panel, adminTheme, cellStyle } from './adminUi'
import { colorLabel } from './statFormat'

/** Close callbacks of the open admin modals, innermost last — Escape closes only the top one. */
const openModals: Array<{ current: () => void }> = []

/**
 * A dark modal shell shared by the admin deck viewers. `title` may carry markup (a name with a tag);
 * `actions` sit beside the close button; `width` widens it for side-by-side decks. Escape closes the
 * topmost open shell, so a deck opened over a player's games closes back to the list.
 */
export function ModalShell({ title, subtitle, actions, width = 820, onClose, children }: {
  title: React.ReactNode
  subtitle?: React.ReactNode
  actions?: React.ReactNode
  width?: number
  onClose: () => void
  children: React.ReactNode
}) {
  const closeRef = useRef(onClose)
  closeRef.current = onClose
  useEffect(() => {
    const entry = { current: () => closeRef.current() }
    openModals.push(entry)
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && openModals[openModals.length - 1] === entry) {
        e.preventDefault()
        entry.current()
      }
    }
    window.addEventListener('keydown', onKey)
    return () => {
      window.removeEventListener('keydown', onKey)
      openModals.splice(openModals.indexOf(entry), 1)
    }
  }, [])
  return (
    <div style={styles.backdrop} onClick={onClose} role="presentation">
      <div
        style={{ ...styles.modal, width: `min(${width}px, 100%)` }}
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-modal
        aria-label={typeof title === 'string' ? title : undefined}
      >
        <div style={styles.header}>
          <div style={{ minWidth: 0 }}>
            <h2 style={styles.title}>{title}</h2>
            {subtitle && <p style={styles.subtitle}>{subtitle}</p>}
          </div>
          <div style={styles.headerActions}>
            {actions}
            <button type="button" style={styles.close} onClick={onClose} aria-label="Close">
              ✕
            </button>
          </div>
        </div>
        <div style={styles.bodyWrap}>{children}</div>
      </div>
    </div>
  )
}

/** Both seats' decks for one of a player's games. */
export function AdminDeckModal({ auth, userId, gameId, opponentLabel, onClose }: {
  auth: AdminAuth
  userId: string
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
    fetchUserGameDecks(auth, userId, gameId)
      .then((d) => live && setDecks(d))
      .catch(() => live && setError('Could not load this game’s decks.'))
    return () => {
      live = false
    }
  }, [auth, userId, gameId])

  return (
    <ModalShell title="Game decks" subtitle={`vs ${opponentLabel}`} onClose={onClose}>
      {error ? (
        <p style={styles.error}>{error}</p>
      ) : !decks ? (
        <p style={cellStyle.muted}>Loading…</p>
      ) : decks.participants.length === 0 ? (
        <p style={cellStyle.muted}>No decklist was recorded for this game.</p>
      ) : (
        <GameDeckColumns participants={decks.participants} />
      )}
    </ModalShell>
  )
}

/** A panel listing the account's saved decks, each openable into the polished deck viewer. */
export function AdminSavedDecks({ auth, userId }: { auth: AdminAuth; userId: string }) {
  const [decks, setDecks] = useState<AdminUserDeck[] | null>(null)
  const [open, setOpen] = useState<AdminUserDeck | null>(null)

  useEffect(() => {
    let live = true
    fetchUserSavedDecks(auth, userId)
      .then((d) => live && setDecks(d))
      .catch(() => live && setDecks([]))
    return () => {
      live = false
    }
  }, [auth, userId])

  if (decks === null) {
    return (
      <Panel title="Saved decks">
        <p style={cellStyle.muted}>Loading…</p>
      </Panel>
    )
  }
  if (decks.length === 0) {
    return (
      <Panel title="Saved decks">
        <p style={cellStyle.muted}>This account hasn’t saved any decks.</p>
      </Panel>
    )
  }

  return (
    <Panel title={`Saved decks · ${decks.length}`}>
      <div style={styles.deckGrid}>
        {decks.map((d) => {
          const total = d.cards.reduce((sum, c) => sum + c.copies, 0)
          const colors = colorsOf(d)
          return (
            <button key={d.id} type="button" style={styles.deckCard} onClick={() => setOpen(d)}>
              <span style={styles.deckName}>{d.name}</span>
              <span style={styles.deckMeta}>
                {d.format ? `${prettyFormat(d.format)} · ` : ''}
                {total} cards
              </span>
              <span style={styles.deckColors}>{colors ? colorLabel(colors) : 'Colourless'}</span>
            </button>
          )
        })}
      </div>
      {open && (
        <ModalShell
          title={open.name}
          subtitle={open.format ? prettyFormat(open.format) : undefined}
          onClose={() => setOpen(null)}
        >
          <DeckCardBody cards={open.cards} />
        </ModalShell>
      )}
    </Panel>
  )
}

/** Canonical WUBRG colour string for a saved deck, from its cards' colours. */
function colorsOf(d: AdminUserDeck): string {
  const order = ['W', 'U', 'B', 'R', 'G']
  const sym: Record<string, string> = { WHITE: 'W', BLUE: 'U', BLACK: 'B', RED: 'R', GREEN: 'G' }
  const present = new Set<string>()
  for (const c of d.cards) for (const col of c.colors) if (sym[col]) present.add(sym[col]!)
  return order.filter((s) => present.has(s)).join('')
}

function prettyFormat(f: string): string {
  return f.charAt(0).toUpperCase() + f.slice(1).toLowerCase()
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
    width: 'min(820px, 100%)',
    maxHeight: '85vh',
    overflowY: 'auto',
    backgroundColor: 'rgba(16, 18, 28, 0.96)',
    border: '1px solid rgba(255, 255, 255, 0.12)',
    borderRadius: 14,
    padding: '20px 22px',
  },
  header: { display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 12 },
  title: { margin: 0, color: '#fff', fontSize: 20 },
  subtitle: { margin: '4px 0 0', color: '#9aa3b8', fontSize: 13 },
  headerActions: { display: 'flex', alignItems: 'center', gap: 12, flex: 'none' },
  close: { background: 'none', border: 'none', color: '#999', cursor: 'pointer', fontSize: 18 },
  bodyWrap: { marginTop: 14 },
  error: { margin: '8px 0 0', color: '#ff6b6b', fontSize: 13 },
  deckGrid: { display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(190px, 1fr))', gap: 10 },
  deckCard: {
    display: 'flex',
    flexDirection: 'column',
    gap: 4,
    alignItems: 'flex-start',
    textAlign: 'left',
    backgroundColor: adminTheme.panelAlt,
    border: `1px solid ${adminTheme.border}`,
    borderRadius: 10,
    padding: '12px 14px',
    cursor: 'pointer',
    color: adminTheme.text,
  },
  deckName: { fontSize: 14, fontWeight: 600, color: adminTheme.text },
  deckMeta: { fontSize: 12, color: adminTheme.textMuted },
  deckColors: { fontSize: 12, color: adminTheme.textSecondary },
}
