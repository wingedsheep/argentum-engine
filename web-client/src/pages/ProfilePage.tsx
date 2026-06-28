/**
 * Account profile: shows the signed-in user, lets them rename their display name, a compact win/loss
 * summary (with a link to the full stats dashboard), a launcher into the deckbuilder's saved-deck
 * browser, and a paginated list of recent games — each of which can be opened to review both players'
 * decks or watched/shared as a replay. Prompts sign-in when anonymous. The heavy analytics (charts,
 * head-to-head, colors, mana curve, …) live on the separate /stats page.
 */
import { useEffect, useState } from 'react'
import type React from 'react'
import { useNavigate } from 'react-router-dom'
import {
  type AccountStats,
  type DeckSummary,
  type GameHistoryEntry,
  fetchHistoryPage,
  fetchStats,
  listDecks,
} from '@/api/account'
import { LoginModal } from '@/components/auth/LoginModal'
import { DeckViewModal } from '@/components/profile/DeckViewModal'
import { colorForIdentity, colorLabel } from '@/components/admin/statFormat'
import { useAuthStore } from '@/store/authStore'

const PAGE_SIZE = 10

export function ProfilePage() {
  const navigate = useNavigate()
  const user = useAuthStore((s) => s.user)
  const status = useAuthStore((s) => s.status)
  const accountsEnabled = useAuthStore((s) => s.accountsEnabled)
  const init = useAuthStore((s) => s.init)
  const logout = useAuthStore((s) => s.logout)
  const updateDisplayName = useAuthStore((s) => s.updateDisplayName)

  const [stats, setStats] = useState<AccountStats | null>(null)
  const [decks, setDecks] = useState<DeckSummary[]>([])
  const [history, setHistory] = useState<GameHistoryEntry[]>([])
  const [historyTotal, setHistoryTotal] = useState(0)
  const [page, setPage] = useState(0)
  const [loginOpen, setLoginOpen] = useState(false)

  const [editingName, setEditingName] = useState(false)
  const [nameDraft, setNameDraft] = useState('')
  const [nameError, setNameError] = useState<string | null>(null)
  const [savingName, setSavingName] = useState(false)
  const [copiedReplay, setCopiedReplay] = useState<string | null>(null)
  const [deckModal, setDeckModal] = useState<{ gameId: string; opponent: string } | null>(null)

  const shareReplay = (gameId: string) => {
    const url = `${window.location.origin}/replay/${gameId}`
    void navigator.clipboard.writeText(url).then(
      () => {
        setCopiedReplay(gameId)
        setTimeout(() => setCopiedReplay((c) => (c === gameId ? null : c)), 2000)
      },
      () => window.prompt('Copy this replay link', url),
    )
  }

  useEffect(() => {
    if (status === 'idle') void init()
  }, [status, init])

  useEffect(() => {
    if (status !== 'authenticated') return
    void fetchStats().then(setStats).catch(() => setStats(null))
    void listDecks().then(setDecks).catch(() => setDecks([]))
  }, [status])

  useEffect(() => {
    if (status !== 'authenticated') return
    void fetchHistoryPage(PAGE_SIZE, page * PAGE_SIZE)
      .then((p) => {
        setHistory(p.entries)
        setHistoryTotal(p.total)
      })
      .catch(() => {
        setHistory([])
        setHistoryTotal(0)
      })
  }, [status, page])

  const startEditName = () => {
    setNameDraft(user?.displayName ?? '')
    setNameError(null)
    setEditingName(true)
  }

  const submitName = async () => {
    const trimmed = nameDraft.trim()
    if (!trimmed) {
      setNameError('Name cannot be empty')
      return
    }
    setSavingName(true)
    setNameError(null)
    try {
      await updateDisplayName(trimmed)
      setEditingName(false)
    } catch (e) {
      setNameError(e instanceof Error ? e.message : 'Could not update name')
    } finally {
      setSavingName(false)
    }
  }

  if (status === 'authenticated' && user) {
    const pageCount = Math.max(1, Math.ceil(historyTotal / PAGE_SIZE))
    const firstShown = historyTotal === 0 ? 0 : page * PAGE_SIZE + 1
    const lastShown = Math.min(historyTotal, page * PAGE_SIZE + history.length)
    return (
      <div style={styles.wrap}>
        <div style={styles.container}>
          <div style={styles.header}>
            <button type="button" style={styles.link} onClick={() => navigate('/')}>
              ← Home
            </button>
            <div style={{ display: 'flex', gap: 16 }}>
              <button type="button" style={styles.link} onClick={() => navigate('/friends')}>
                Friends
              </button>
              <button type="button" style={styles.link} onClick={logout}>
                Sign out
              </button>
            </div>
          </div>

          {editingName ? (
            <div style={styles.nameEditRow}>
              <input
                style={styles.nameInput}
                value={nameDraft}
                maxLength={40}
                autoFocus
                onChange={(e) => setNameDraft(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') void submitName()
                  if (e.key === 'Escape') setEditingName(false)
                }}
              />
              <button type="button" style={styles.smallPrimary} disabled={savingName} onClick={() => void submitName()}>
                {savingName ? 'Saving…' : 'Save'}
              </button>
              <button type="button" style={styles.smallGhost} onClick={() => setEditingName(false)}>
                Cancel
              </button>
            </div>
          ) : (
            <div style={styles.nameRow}>
              <h1 style={styles.title}>{user.displayName}</h1>
              <button type="button" style={styles.editLink} onClick={startEditName}>
                Edit name
              </button>
            </div>
          )}
          {nameError ? <p style={styles.error}>{nameError}</p> : <p style={styles.muted}>{user.email}</p>}

          {user.isAdmin && (
            <button type="button" style={styles.adminLink} onClick={() => navigate('/admin')}>
              <span style={styles.adminLinkBadge}>ADMIN</span>
              <span style={styles.adminLinkText}>Open the admin dashboard</span>
              <span style={styles.adminLinkArrow}>→</span>
            </button>
          )}

          <div style={styles.statsRow}>
            <Stat label="Games" value={stats?.games ?? 0} />
            <Stat label="Wins" value={stats?.wins ?? 0} />
            <Stat label="Losses" value={stats?.losses ?? 0} />
            <Stat label="Win rate" value={stats ? `${Math.round(stats.winRate * 100)}%` : '—'} />
          </div>

          <button type="button" style={styles.statsLink} onClick={() => navigate('/stats')}>
            <span style={styles.statsLinkText}>
              <span style={styles.statsLinkTitle}>View full stats</span>
              <span style={styles.muted}>Charts, colors, mana curve, head-to-head & more</span>
            </span>
            <span style={styles.statsLinkArrow}>→</span>
          </button>

          <h2 style={styles.section}>Decks</h2>
          {/* Small launcher into the deckbuilder's saved-deck browser (the polished overlay that lists
              account + browser decks with online badges) — no need to duplicate that UI here. */}
          <button type="button" style={styles.deckManager} onClick={() => navigate('/deckbuilder?decks=open')}>
            <span style={styles.deckManagerText}>
              <span style={styles.deckManagerTitle}>Manage my decks</span>
              <span style={styles.muted}>
                {decks.length === 0
                  ? 'No decks saved to your account yet'
                  : `${decks.length} deck${decks.length === 1 ? '' : 's'} saved to your account`}
              </span>
            </span>
            <span style={styles.deckManagerArrow}>Open deck browser →</span>
          </button>

          {(history.length > 0 || historyTotal > 0) && (
            <div style={styles.sectionBlock}>
              <div style={styles.recentHead}>
                <h2 style={{ ...styles.section, margin: 0 }}>Recent games</h2>
                {historyTotal > 0 && (
                  <span style={styles.muted}>
                    {firstShown}–{lastShown} of {historyTotal}
                  </span>
                )}
              </div>
              <SimpleTable head={['Date', 'Mode', 'Colors', 'Opponent', 'Result', 'Deck', 'Replay']}>
                {history.map((g, i) => (
                  <tr key={`${g.gameId}-${i}`}>
                    <td style={styles.td}>{g.endedAt.slice(0, 10)}</td>
                    <td style={styles.td}>{prettyMode(g.gameMode)}</td>
                    <td style={styles.td}>
                      {g.colors ? (
                        <span style={styles.colorsCell}>
                          <span style={{ ...styles.colorDot, backgroundColor: colorForIdentity(g.colors) }} />
                          {colorLabel(g.colors)}
                        </span>
                      ) : (
                        '—'
                      )}
                    </td>
                    <td style={styles.td}>{g.opponents ?? '—'}</td>
                    <td style={{ ...styles.tdNum, color: g.won ? '#5bd16e' : '#e15b6e' }}>
                      {g.won ? 'Win' : 'Loss'}
                    </td>
                    <td style={styles.tdNum}>
                      <button
                        type="button"
                        style={styles.link}
                        onClick={() => setDeckModal({ gameId: g.gameId, opponent: g.opponents ?? 'opponent' })}
                      >
                        View
                      </button>
                    </td>
                    <td style={styles.tdNum}>
                      {g.hasReplay ? (
                        <span style={{ display: 'inline-flex', gap: 10, justifyContent: 'flex-end' }}>
                          <button type="button" style={styles.link} onClick={() => navigate(`/replay/${g.gameId}`)}>
                            Watch
                          </button>
                          <button type="button" style={styles.link} onClick={() => shareReplay(g.gameId)}>
                            {copiedReplay === g.gameId ? 'Copied!' : 'Share'}
                          </button>
                        </span>
                      ) : (
                        '—'
                      )}
                    </td>
                  </tr>
                ))}
              </SimpleTable>
              {pageCount > 1 && (
                <div style={styles.pager}>
                  <button
                    type="button"
                    style={{ ...styles.pagerBtn, ...(page === 0 ? styles.pagerBtnDisabled : {}) }}
                    disabled={page === 0}
                    onClick={() => setPage((p) => Math.max(0, p - 1))}
                  >
                    ← Newer
                  </button>
                  <span style={styles.muted}>
                    Page {page + 1} of {pageCount}
                  </span>
                  <button
                    type="button"
                    style={{ ...styles.pagerBtn, ...(page + 1 >= pageCount ? styles.pagerBtnDisabled : {}) }}
                    disabled={page + 1 >= pageCount}
                    onClick={() => setPage((p) => p + 1)}
                  >
                    Older →
                  </button>
                </div>
              )}
            </div>
          )}
        </div>

        {deckModal && (
          <DeckViewModal
            gameId={deckModal.gameId}
            opponentLabel={deckModal.opponent}
            onClose={() => setDeckModal(null)}
          />
        )}
      </div>
    )
  }

  const resolving = status === 'idle' || status === 'loading'

  return (
    <div style={styles.wrap}>
      <div style={styles.container}>
        <button type="button" style={styles.link} onClick={() => navigate('/')}>
          ← Home
        </button>
        <h1 style={styles.title}>Your account</h1>
        {accountsEnabled ? (
          <>
            <p style={styles.muted}>
              Sign in to save decks to the cloud and track your win/loss record.
            </p>
            <button type="button" style={styles.primary} onClick={() => setLoginOpen(true)}>
              Sign in
            </button>
            <LoginModal open={loginOpen} onClose={() => setLoginOpen(false)} />
          </>
        ) : resolving ? (
          <p style={styles.muted}>Loading…</p>
        ) : (
          <p style={styles.muted}>Accounts aren't available on this server.</p>
        )}
      </div>
    </div>
  )
}

function Stat({ label, value }: { label: string; value: number | string }) {
  return (
    <div style={styles.stat}>
      <div style={styles.statValue}>{value}</div>
      <div style={styles.statLabel}>{label}</div>
    </div>
  )
}

function SimpleTable({ head, children }: { head: string[]; children: React.ReactNode }) {
  return (
    <div style={styles.tableWrap}>
      <table style={styles.table}>
        <thead>
          <tr>
            {head.map((h, i) => (
              <th key={h} style={i === 0 ? styles.th : styles.thNum}>
                {h}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>{children}</tbody>
      </table>
    </div>
  )
}

/** Turn a raw game-mode token (TOURNAMENT / QUICK_GAME / ...) into a readable label. */
function prettyMode(mode: string | null): string {
  if (!mode) return '—'
  return mode
    .toLowerCase()
    .split('_')
    .map((w) => w.charAt(0).toUpperCase() + w.slice(1))
    .join(' ')
}

const styles: Record<string, React.CSSProperties> = {
  wrap: { height: '100vh', overflowY: 'auto', backgroundColor: '#0a0a15', padding: '32px 16px' },
  adminLink: {
    display: 'flex',
    alignItems: 'center',
    gap: 10,
    marginTop: 8,
    backgroundColor: 'rgba(139,155,255,0.08)',
    border: '1px solid #2f2f55',
    borderRadius: 12,
    padding: '12px 16px',
    color: '#fff',
    cursor: 'pointer',
    textAlign: 'left',
  },
  adminLinkBadge: {
    fontSize: 10,
    fontWeight: 700,
    letterSpacing: 0.6,
    color: '#8b9bff',
    backgroundColor: 'rgba(139,155,255,0.14)',
    border: '1px solid #8b9bff55',
    borderRadius: 999,
    padding: '2px 7px',
  },
  adminLinkText: { flex: 1, fontSize: 14, fontWeight: 600 },
  adminLinkArrow: { color: '#8b9bff', fontSize: 16, fontWeight: 700 },
  container: { maxWidth: 720, margin: '0 auto', display: 'flex', flexDirection: 'column', gap: 12 },
  header: { display: 'flex', justifyContent: 'space-between' },
  link: { background: 'none', border: 'none', color: '#8b9bff', cursor: 'pointer', fontSize: 14, padding: 0 },
  title: { margin: '8px 0 0', color: '#fff', fontSize: 28 },
  nameRow: { display: 'flex', alignItems: 'baseline', gap: 12, flexWrap: 'wrap' },
  editLink: { background: 'none', border: 'none', color: '#8b9bff', cursor: 'pointer', fontSize: 13, padding: 0 },
  nameEditRow: { display: 'flex', alignItems: 'center', gap: 8, marginTop: 8, flexWrap: 'wrap' },
  nameInput: {
    flex: '1 1 200px',
    minWidth: 0,
    backgroundColor: '#14141f',
    border: '1px solid #2a2a3e',
    borderRadius: 8,
    padding: '8px 12px',
    color: '#fff',
    fontSize: 18,
  },
  smallPrimary: {
    padding: '8px 14px',
    borderRadius: 8,
    border: 'none',
    backgroundColor: '#5b6ee1',
    color: '#fff',
    fontWeight: 600,
    fontSize: 13,
    cursor: 'pointer',
  },
  smallGhost: {
    padding: '8px 14px',
    borderRadius: 8,
    border: '1px solid #2a2a3e',
    backgroundColor: 'transparent',
    color: '#aaa',
    fontSize: 13,
    cursor: 'pointer',
  },
  error: { margin: 0, color: '#ff6b6b', fontSize: 13 },
  muted: { margin: 0, color: '#888', fontSize: 14 },
  section: { margin: '0 0 10px', color: '#fff', fontSize: 18 },
  sectionBlock: {
    marginTop: 8,
    backgroundColor: '#14141f',
    border: '1px solid #2a2a3e',
    borderRadius: 12,
    padding: '16px 18px',
  },
  recentHead: { display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', marginBottom: 10 },
  tableWrap: { overflowX: 'auto' },
  table: { width: '100%', borderCollapse: 'collapse', fontSize: 13 },
  th: { textAlign: 'left', color: '#888', fontWeight: 600, padding: '6px 8px', borderBottom: '1px solid #2a2a3e' },
  thNum: { textAlign: 'right', color: '#888', fontWeight: 600, padding: '6px 8px', borderBottom: '1px solid #2a2a3e' },
  td: { textAlign: 'left', color: '#ccc', padding: '6px 8px', borderBottom: '1px solid #1f1f2e' },
  tdNum: { textAlign: 'right', color: '#ccc', padding: '6px 8px', borderBottom: '1px solid #1f1f2e' },
  colorsCell: { display: 'inline-flex', alignItems: 'center', gap: 6 },
  colorDot: { width: 9, height: 9, borderRadius: 999, display: 'inline-block' },
  pager: { display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 14, marginTop: 12 },
  pagerBtn: {
    background: 'none',
    border: '1px solid #2a2a3e',
    borderRadius: 8,
    color: '#cdd',
    cursor: 'pointer',
    fontSize: 13,
    padding: '6px 12px',
  },
  pagerBtnDisabled: { color: '#555', cursor: 'default', borderColor: '#1f1f2e' },
  statsRow: { display: 'flex', gap: 12, marginTop: 12, flexWrap: 'wrap' },
  stat: {
    flex: '1 1 120px',
    backgroundColor: '#14141f',
    border: '1px solid #2a2a3e',
    borderRadius: 12,
    padding: '16px 12px',
    textAlign: 'center',
  },
  statValue: { color: '#fff', fontSize: 26, fontWeight: 700 },
  statLabel: { color: '#888', fontSize: 12, marginTop: 4, textTransform: 'uppercase', letterSpacing: 0.5 },
  statsLink: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: 12,
    textAlign: 'left',
    backgroundColor: 'rgba(91,110,225,0.10)',
    border: '1px solid #2f2f55',
    borderRadius: 12,
    padding: '14px 16px',
    color: '#fff',
    cursor: 'pointer',
  },
  statsLinkText: { display: 'flex', flexDirection: 'column', gap: 3 },
  statsLinkTitle: { fontSize: 15, fontWeight: 600 },
  statsLinkArrow: { color: '#8b9bff', fontSize: 18, fontWeight: 700, flexShrink: 0 },
  deckManager: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: 12,
    textAlign: 'left',
    backgroundColor: '#14141f',
    border: '1px solid #2a2a3e',
    borderRadius: 12,
    padding: '16px 18px',
    color: '#fff',
    cursor: 'pointer',
  },
  deckManagerText: { display: 'flex', flexDirection: 'column', gap: 4 },
  deckManagerTitle: { fontSize: 16, fontWeight: 600 },
  deckManagerArrow: { color: '#8b9bff', fontSize: 14, fontWeight: 600, flexShrink: 0 },
  primary: {
    alignSelf: 'flex-start',
    marginTop: 8,
    padding: '10px 18px',
    borderRadius: 8,
    border: 'none',
    backgroundColor: '#5b6ee1',
    color: '#fff',
    fontWeight: 600,
    cursor: 'pointer',
  },
}
