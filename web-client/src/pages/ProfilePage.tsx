/**
 * Account profile: shows the signed-in user, lets them rename their display name, shows their
 * win/loss record, and offers a small launcher into the deckbuilder's saved-deck browser (the polished
 * overlay that lists account + browser decks with online badges). Prompts sign-in when anonymous.
 */
import { useEffect, useState } from 'react'
import type React from 'react'
import { useNavigate } from 'react-router-dom'
import { Bar, BarChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import {
  type AccountStats,
  type CardStat,
  type DeckSummary,
  type GameHistoryEntry,
  type HeadToHead,
  type StatBucket,
  type UserTournamentEntry,
  fetchColorStats,
  fetchHistory,
  fetchModeStats,
  fetchOpponents,
  fetchSetStats,
  fetchStats,
  fetchTopCards,
  fetchTournamentHistory,
  listDecks,
} from '@/api/account'
import { LoginModal } from '@/components/auth/LoginModal'
import { colorLabel } from '@/components/admin/statFormat'
import { useAuthStore } from '@/store/authStore'

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
  const [colors, setColors] = useState<StatBucket[]>([])
  const [sets, setSets] = useState<StatBucket[]>([])
  const [modes, setModes] = useState<StatBucket[]>([])
  const [opponents, setOpponents] = useState<HeadToHead[]>([])
  const [history, setHistory] = useState<GameHistoryEntry[]>([])
  const [topCards, setTopCards] = useState<CardStat[]>([])
  const [tournaments, setTournaments] = useState<UserTournamentEntry[]>([])
  const [loginOpen, setLoginOpen] = useState(false)

  const [editingName, setEditingName] = useState(false)
  const [nameDraft, setNameDraft] = useState('')
  const [nameError, setNameError] = useState<string | null>(null)
  const [savingName, setSavingName] = useState(false)

  useEffect(() => {
    if (status === 'idle') void init()
  }, [status, init])

  useEffect(() => {
    if (status !== 'authenticated') return
    void fetchStats().then(setStats).catch(() => setStats(null))
    void listDecks().then(setDecks).catch(() => setDecks([]))
    void fetchColorStats().then(setColors).catch(() => setColors([]))
    void fetchSetStats().then(setSets).catch(() => setSets([]))
    void fetchModeStats().then(setModes).catch(() => setModes([]))
    void fetchOpponents().then(setOpponents).catch(() => setOpponents([]))
    void fetchHistory(25).then(setHistory).catch(() => setHistory([]))
    void fetchTopCards(20).then(setTopCards).catch(() => setTopCards([]))
    void fetchTournamentHistory(15).then(setTournaments).catch(() => setTournaments([]))
  }, [status])

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

          {colors.length > 0 && (
            <Section title="Colors you play">
              <ResponsiveContainer width="100%" height={Math.max(120, colors.length * 30)}>
                <BarChart
                  data={colors.map((c) => ({ label: colorLabel(c.label), count: c.count }))}
                  layout="vertical"
                  margin={{ top: 4, right: 16, bottom: 4, left: 8 }}
                >
                  <XAxis type="number" stroke="#666" fontSize={11} allowDecimals={false} />
                  <YAxis type="category" dataKey="label" stroke="#999" fontSize={11} width={90} />
                  <Tooltip contentStyle={tooltipStyle} cursor={{ fill: '#ffffff10' }} />
                  <Bar dataKey="count" fill="#5b6ee1" radius={[0, 4, 4, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </Section>
          )}

          {sets.length > 0 && (
            <Section title="Sets you play">
              <ChipList items={sets.map((sb) => `${sb.label} · ${sb.count}`)} />
            </Section>
          )}

          {modes.length > 0 && (
            <Section title="Game modes">
              <ChipList items={modes.map((m) => `${prettyMode(m.label)} · ${m.count}`)} />
            </Section>
          )}

          {opponents.length > 0 && (
            <Section title="Head to head">
              <SimpleTable head={['Opponent', 'W', 'L']}>
                {opponents.map((o, i) => (
                  <tr key={`${o.opponent}-${i}`}>
                    <td style={styles.td}>
                      {o.opponent}
                      {o.isAi ? <span style={styles.aiTag}> AI</span> : null}
                    </td>
                    <td style={styles.tdNum}>{o.wins}</td>
                    <td style={styles.tdNum}>{o.losses}</td>
                  </tr>
                ))}
              </SimpleTable>
            </Section>
          )}

          {topCards.length > 0 && (
            <Section title="Most-played cards">
              <ChipList items={topCards.map((c) => `${c.cardName} · ${c.copies}`)} />
            </Section>
          )}

          {tournaments.length > 0 && (
            <Section title="Tournaments">
              <SimpleTable head={['Date', 'Tournament', 'Place']}>
                {tournaments.map((t, i) => (
                  <tr key={`${t.endedAt}-${i}`}>
                    <td style={styles.td}>{t.endedAt.slice(0, 10)}</td>
                    <td style={styles.td}>{t.name ?? '—'}</td>
                    <td style={styles.tdNum}>
                      {t.placement}/{t.playerCount}
                    </td>
                  </tr>
                ))}
              </SimpleTable>
            </Section>
          )}

          {history.length > 0 && (
            <Section title="Recent games">
              <SimpleTable head={['Date', 'Mode', 'Colors', 'Opponent', 'Result']}>
                {history.map((g, i) => (
                  <tr key={`${g.endedAt}-${i}`}>
                    <td style={styles.td}>{g.endedAt.slice(0, 10)}</td>
                    <td style={styles.td}>{prettyMode(g.gameMode)}</td>
                    <td style={styles.td}>{g.colors ? colorLabel(g.colors) : '—'}</td>
                    <td style={styles.td}>{g.opponents ?? '—'}</td>
                    <td style={{ ...styles.tdNum, color: g.won ? '#5bd16e' : '#e15b6e' }}>
                      {g.won ? 'Win' : 'Loss'}
                    </td>
                  </tr>
                ))}
              </SimpleTable>
            </Section>
          )}
        </div>
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

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div style={styles.sectionBlock}>
      <h2 style={styles.section}>{title}</h2>
      {children}
    </div>
  )
}

function ChipList({ items }: { items: string[] }) {
  return (
    <div style={styles.chipRow}>
      {items.map((it) => (
        <span key={it} style={styles.chip}>
          {it}
        </span>
      ))}
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

const tooltipStyle: React.CSSProperties = {
  backgroundColor: '#12121e',
  border: '1px solid #2a2a3e',
  borderRadius: 6,
  color: '#ddd',
  fontSize: 12,
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
  chipRow: { display: 'flex', flexWrap: 'wrap', gap: 8 },
  chip: {
    backgroundColor: '#1d1d2e',
    border: '1px solid #2a2a3e',
    borderRadius: 999,
    padding: '4px 12px',
    color: '#cdd',
    fontSize: 13,
  },
  tableWrap: { overflowX: 'auto' },
  table: { width: '100%', borderCollapse: 'collapse', fontSize: 13 },
  th: { textAlign: 'left', color: '#888', fontWeight: 600, padding: '6px 8px', borderBottom: '1px solid #2a2a3e' },
  thNum: { textAlign: 'right', color: '#888', fontWeight: 600, padding: '6px 8px', borderBottom: '1px solid #2a2a3e' },
  td: { textAlign: 'left', color: '#ccc', padding: '6px 8px', borderBottom: '1px solid #1f1f2e' },
  tdNum: { textAlign: 'right', color: '#ccc', padding: '6px 8px', borderBottom: '1px solid #1f1f2e' },
  aiTag: { color: '#888', fontSize: 11 },
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
