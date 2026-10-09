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
  type UserTournamentEntry,
  fetchHistoryPage,
  fetchStats,
  fetchTournamentHistory,
  listDecks,
} from '@/api/account'
import { LoginModal } from '@/components/auth/LoginModal'
import { DeckViewModal } from '@/components/profile/DeckViewModal'
import { TournamentDetailModal } from '@/components/profile/TournamentDetailModal'
import {
  AccountPage,
  Avatar,
  GameHistoryList,
  MessageCard,
  TournamentList,
  accountStyles as a,
} from '@/components/profile/accountUi'
import { pageStyles as p } from '@/components/ui/PageShell'
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
  const [tournaments, setTournaments] = useState<UserTournamentEntry[]>([])
  const [openTournament, setOpenTournament] = useState<number | null>(null)
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
    void fetchTournamentHistory(25).then(setTournaments).catch(() => setTournaments([]))
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
      <AccountPage title="Profile">
        <section className={p.panel}>
          <div className={a.identity}>
            <Avatar name={user.displayName} />
            <div className={a.identityText}>
              {editingName ? (
                <div className={a.nameEdit}>
                  <input
                    className={p.input}
                    value={nameDraft}
                    maxLength={40}
                    autoFocus
                    aria-label="Display name"
                    onChange={(e) => setNameDraft(e.target.value)}
                    onKeyDown={(e) => {
                      if (e.key === 'Enter') void submitName()
                      if (e.key === 'Escape') setEditingName(false)
                    }}
                  />
                  <button type="button" className={p.buttonPrimary} disabled={savingName} onClick={() => void submitName()}>
                    {savingName ? 'Saving…' : 'Save'}
                  </button>
                  <button type="button" className={p.buttonGhost} onClick={() => setEditingName(false)}>
                    Cancel
                  </button>
                </div>
              ) : (
                <div className={a.nameRow}>
                  <h1 className={p.h1}>{user.displayName}</h1>
                  <button type="button" className={a.link} style={{ fontSize: 13 }} onClick={startEditName}>
                    Edit name
                  </button>
                </div>
              )}
              {nameError ? <p className={a.error}>{nameError}</p> : <p className={a.muted}>{user.email}</p>}
            </div>
            <div className={a.identityActions}>
              {user.isAdmin && (
                <button type="button" className={a.adminPill} onClick={() => navigate('/admin')}>
                  <span className={a.adminBadge}>ADMIN</span>
                  Dashboard
                </button>
              )}
              <button type="button" className={p.button} onClick={logout}>
                Log out
              </button>
            </div>
          </div>
          <div className={p.statGrid} style={{ marginTop: 18 }}>
            <Stat label="Games" value={stats?.games ?? 0} />
            <Stat label="Wins" value={stats?.wins ?? 0} />
            <Stat label="Losses" value={stats?.losses ?? 0} />
            <Stat label="Win rate" value={stats ? `${Math.round(stats.winRate * 100)}%` : '—'} />
          </div>
        </section>

        <div className={a.shortcuts}>
          <Shortcut
            icon={<ChartIcon />}
            title="Full stats"
            sub="Ratings, colors, curve & more"
            onClick={() => navigate('/stats')}
          />
          {/* Launcher into the deckbuilder's saved-deck browser — no need to duplicate that UI here. */}
          <Shortcut
            icon={<DecksIcon />}
            title="My decks"
            sub={
              decks.length === 0
                ? 'None saved to your account yet'
                : `${decks.length} deck${decks.length === 1 ? '' : 's'} saved to your account`
            }
            onClick={() => navigate('/deckbuilder?decks=open')}
          />
          <Shortcut icon={<FriendsIcon />} title="Friends" sub="Your friend code and who's online" onClick={() => navigate('/friends')} />
          <Shortcut
            icon={<SlidersIcon />}
            title="Preferences"
            sub="Auto-pass stops, card stacking & more"
            onClick={() => navigate('/preferences')}
          />
        </div>

        <section className={p.panel}>
          <div className={a.sectionHead}>
            <h2 className={a.sectionTitle}>Recent games</h2>
            {historyTotal > 0 && (
              <span className={a.count}>
                {firstShown}–{lastShown} of {historyTotal}
              </span>
            )}
          </div>
          {history.length === 0 ? (
            <p className={a.muted}>No games yet — finished games show up here with their decks and replays.</p>
          ) : (
            <GameHistoryList
              games={history}
              renderActions={(g) => (
                <>
                  <button
                    type="button"
                    className={a.miniButton}
                    onClick={() => setDeckModal({ gameId: g.gameId, opponent: g.opponents ?? 'opponent' })}
                  >
                    Decks
                  </button>
                  {g.hasReplay && (
                    <>
                      <button type="button" className={a.miniButton} onClick={() => navigate(`/replay/${g.gameId}`)}>
                        Watch
                      </button>
                      <button type="button" className={a.miniButton} onClick={() => shareReplay(g.gameId)}>
                        {copiedReplay === g.gameId ? 'Copied!' : 'Share'}
                      </button>
                    </>
                  )}
                </>
              )}
            />
          )}
          {pageCount > 1 && (
            <div className={a.pager}>
              <button
                type="button"
                className={p.buttonGhost}
                disabled={page === 0}
                onClick={() => setPage((n) => Math.max(0, n - 1))}
              >
                ← Newer
              </button>
              <span>
                Page {page + 1} of {pageCount}
              </span>
              <button
                type="button"
                className={p.buttonGhost}
                disabled={page + 1 >= pageCount}
                onClick={() => setPage((n) => n + 1)}
              >
                Older →
              </button>
            </div>
          )}
        </section>

        {tournaments.length > 0 && (
          <section className={p.panel}>
            <div className={a.sectionHead}>
              <h2 className={a.sectionTitle}>Recent tournaments</h2>
              <span className={a.count}>Open one for its standings and replays</span>
            </div>
            <TournamentList tournaments={tournaments} onOpen={setOpenTournament} />
          </section>
        )}

        {deckModal && (
          <DeckViewModal
            gameId={deckModal.gameId}
            opponentLabel={deckModal.opponent}
            onClose={() => setDeckModal(null)}
          />
        )}
        {openTournament != null && (
          <TournamentDetailModal tournamentId={openTournament} onClose={() => setOpenTournament(null)} />
        )}
      </AccountPage>
    )
  }

  const resolving = status === 'idle' || status === 'loading'

  return (
    <AccountPage title="Profile">
      <MessageCard>
        <h1 className={p.h1}>Your account</h1>
        {accountsEnabled ? (
          <>
            <p className={p.lede}>Sign in with your email — no password — to keep your progress across devices.</p>
            <ul className={a.benefits}>
              <li>Save decks to the cloud</li>
              <li>Track your record, ratings and stats</li>
              <li>Add friends and see who's online</li>
            </ul>
            <button type="button" className={`${p.buttonPrimary} ${a.fullButton}`} onClick={() => setLoginOpen(true)}>
              Sign in
            </button>
            <LoginModal open={loginOpen} onClose={() => setLoginOpen(false)} />
          </>
        ) : resolving ? (
          <p className={a.muted}>Loading…</p>
        ) : (
          <p className={a.muted}>Accounts aren't available on this server.</p>
        )}
      </MessageCard>
    </AccountPage>
  )
}

function Stat({ label, value }: { label: string; value: number | string }) {
  return (
    <div className={p.stat}>
      <span className={p.statValue}>{value}</span>
      <span className={p.statLabel}>{label}</span>
    </div>
  )
}

function Shortcut({ icon, title, sub, onClick }: { icon: React.ReactNode; title: string; sub: string; onClick: () => void }) {
  return (
    <button type="button" className={a.shortcut} onClick={onClick}>
      <span className={a.shortcutIcon}>{icon}</span>
      <span className={a.shortcutText}>
        <span className={a.shortcutTitle}>{title}</span>
        <span className={a.shortcutSub}>{sub}</span>
      </span>
      <span className={a.shortcutArrow} aria-hidden>→</span>
    </button>
  )
}

const iconProps = {
  viewBox: '0 0 24 24',
  fill: 'none',
  stroke: 'currentColor',
  strokeWidth: 1.8,
  strokeLinecap: 'round' as const,
  strokeLinejoin: 'round' as const,
  'aria-hidden': true,
}

function ChartIcon() {
  return (
    <svg {...iconProps}>
      <path d="M4 20V10M10 20V4M16 20v-7M22 20H2" />
    </svg>
  )
}

function DecksIcon() {
  return (
    <svg {...iconProps}>
      <rect x="7" y="3" width="12" height="16" rx="2" />
      <path d="M4 7v12a2 2 0 0 0 2 2h9" />
    </svg>
  )
}

function FriendsIcon() {
  return (
    <svg {...iconProps}>
      <circle cx="9" cy="8" r="3.5" />
      <path d="M2.5 20c.8-3.4 3.4-5.5 6.5-5.5s5.7 2.1 6.5 5.5" />
      <path d="M16 4.6a3.5 3.5 0 0 1 0 6.8M18 14.8c1.8.8 3 2.6 3.5 5.2" />
    </svg>
  )
}

function SlidersIcon() {
  return (
    <svg {...iconProps}>
      <path d="M4 6h10M18 6h2M4 12h4M12 12h8M4 18h12M20 18h0" />
      <circle cx="16" cy="6" r="2" />
      <circle cx="10" cy="12" r="2" />
      <circle cx="18" cy="18" r="2" />
    </svg>
  )
}
