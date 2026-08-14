/**
 * AI Sandbox — a dev-only "put two bots at a table and let me watch" screen.
 *
 * The LLM Tournament page next door exists to compare *models*; this one exists to watch the
 * built-in engine AI play so its mistakes are visible. Everything is driven by the existing
 * `/api/dev/ai-tournament` endpoint (the same one `just watch-ai-match` uses): create a private,
 * AI-only sealed lobby, let the bots build decks and start matches unattended, then hand you the
 * live game.
 *
 * The lobby id lives in the URL (`/ai-sandbox/:lobbyId`) so a refresh — and the browser Back
 * button out of a spectated game — comes back to the same sandbox rather than an empty form.
 */
import { useCallback, useEffect, useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import type { AvailableSet } from '@/types/messages'
import { SetPickerModal } from '@/components/ui/SetPickerModal'

const API = '/api/dev/ai-tournament'

interface LiveGame {
  gameSessionId: string
  player1Name: string
  player2Name: string
  player1Life: number
  player2Life: number
  turnNumber: number
}

interface SandboxStatus {
  lobbyId: string
  state: string
  playerNames: string[]
  decksSubmitted: number
  round: number
  totalRounds: number
  complete: boolean
  liveGames: LiveGame[]
}

/** Auto-watch is a preference, not a per-run choice — it survives the jump into the game. */
const AUTO_WATCH_KEY = 'argentum-ai-sandbox-autowatch'

/**
 * Games we have already auto-jumped into, so coming Back from a spectated game doesn't bounce you
 * straight into the same one again. Session-scoped: a fresh tab starts clean.
 */
function markAutoWatched(gameSessionId: string) {
  sessionStorage.setItem(`argentum-ai-sandbox-watched:${gameSessionId}`, '1')
}
function wasAutoWatched(gameSessionId: string): boolean {
  return sessionStorage.getItem(`argentum-ai-sandbox-watched:${gameSessionId}`) === '1'
}

/**
 * Full page load rather than a react-router navigation: `/?spectate=` is read off
 * `window.location.search` when `App` mounts, and the spectator connects as its own ephemeral
 * session (see App.tsx).
 */
function watchGame(gameSessionId: string) {
  markAutoWatched(gameSessionId)
  window.location.assign(`/?spectate=${gameSessionId}`)
}

export function AiSandboxPage() {
  const { lobbyId: routeLobbyId } = useParams<{ lobbyId?: string }>()
  const navigate = useNavigate()
  const [status, setStatus] = useState<SandboxStatus | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [autoWatch, setAutoWatch] = useState(() => localStorage.getItem(AUTO_WATCH_KEY) !== 'false')

  const lobbyIdRef = useRef<string | null>(routeLobbyId ?? null)

  const refresh = useCallback(async () => {
    const id = lobbyIdRef.current
    if (!id) return
    try {
      const res = await fetch(`${API}/${id}`)
      if (res.ok) {
        setStatus(await res.json() as SandboxStatus)
      } else if (res.status === 404) {
        // Lobbies don't survive a server restart — drop back to the form rather than polling a
        // lobby id that will never answer again.
        lobbyIdRef.current = null
        setStatus(null)
        navigate('/ai-sandbox', { replace: true })
      }
    } catch {
      /* transient — keep the last view */
    }
  }, [navigate])

  useEffect(() => {
    lobbyIdRef.current = routeLobbyId ?? null
    if (routeLobbyId) void refresh()
    else setStatus(null)
  }, [routeLobbyId, refresh])

  useEffect(() => {
    const interval = setInterval(refresh, 1500)
    return () => clearInterval(interval)
  }, [refresh])

  useEffect(() => {
    localStorage.setItem(AUTO_WATCH_KEY, String(autoWatch))
  }, [autoWatch])

  // The whole point of the screen: as soon as the bots actually start playing, put the board on
  // screen without a click.
  useEffect(() => {
    if (!autoWatch || !status) return
    const next = status.liveGames.find((g) => !wasAutoWatched(g.gameSessionId))
    if (next) watchGame(next.gameSessionId)
  }, [autoWatch, status])

  return (
    <div style={styles.page}>
      <header style={styles.header}>
        <h1 style={styles.h1}>🤖 AI Sandbox <span style={styles.devTag}>dev</span></h1>
        <a href="/" style={styles.homeLink}>← Home</a>
      </header>

      {error && (
        <div style={styles.errorBar} onClick={() => setError(null)}>
          {error} <span style={{ float: 'right' }}>✕</span>
        </div>
      )}

      {!status && (
        <SetupForm
          onCreated={(lobbyId) => {
            lobbyIdRef.current = lobbyId
            navigate(`/ai-sandbox/${lobbyId}`)
          }}
          onError={setError}
        />
      )}

      {status && (
        <StatusPanel
          status={status}
          autoWatch={autoWatch}
          onToggleAutoWatch={setAutoWatch}
          onNew={() => {
            lobbyIdRef.current = null
            setStatus(null)
            navigate('/ai-sandbox')
          }}
        />
      )}
    </div>
  )
}

// ============================================================================
// Setup form
// ============================================================================

function SetupForm({
  onCreated,
  onError,
}: {
  onCreated: (lobbyId: string) => void
  onError: (e: string) => void
}) {
  const [sets, setSets] = useState<AvailableSet[]>([])
  const [setCodes, setSetCodes] = useState<string[]>([])
  const [pickerOpen, setPickerOpen] = useState(false)
  const [playerCount, setPlayerCount] = useState(2)
  const [gamesPerMatch, setGamesPerMatch] = useState(3)
  const [deckbuild, setDeckbuild] = useState<'heuristic' | 'llm'>('heuristic')
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    fetch(`${API}/sets`)
      .then((r) => (r.ok ? r.json() : Promise.reject(new Error(String(r.status)))))
      .then((s: AvailableSet[]) => {
        setSets(s)
        // Newest fully-implemented set, matching the lobby picker's bias — a partial set makes the
        // bots look worse than they are.
        const def = s
          .filter((x) => !x.partial && !x.extensionSet)
          .sort((a, b) => (b.releaseDate ?? '').localeCompare(a.releaseDate ?? ''))[0]
        if (def) setSetCodes([def.code])
      })
      .catch(() => onError('Failed to load sets — is the server running with dev endpoints enabled?'))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const selectedNames = setCodes
    .map((code) => sets.find((s) => s.code === code)?.name ?? code)
    .join(' + ')

  const create = async () => {
    if (setCodes.length === 0) {
      onError('Pick at least one set')
      return
    }
    setBusy(true)
    try {
      const res = await fetch(API, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          setCodes,
          playerCount,
          gamesPerMatch,
          heuristicDeckbuilding: deckbuild === 'heuristic',
        }),
      })
      const data = await res.json() as { lobbyId: string; message: string }
      if (res.ok && data.lobbyId) onCreated(data.lobbyId)
      else onError(data.message || 'Failed to create the AI lobby')
    } catch (e) {
      onError(String(e))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div style={styles.card}>
      <h2 style={styles.h2}>New AI-only lobby</h2>

      <div style={styles.formRow}>
        <div style={{ flex: 1 }}>
          <label style={styles.label}>Sets (sealed pool)</label>
          <button type="button" style={styles.setButton} onClick={() => setPickerOpen(true)}>
            {selectedNames || 'Choose sets…'}
          </button>
        </div>
        <div>
          <label style={styles.label}>AI players</label>
          <input
            type="number"
            min={2}
            max={8}
            value={playerCount}
            onChange={(e) => setPlayerCount(Math.min(8, Math.max(2, Number(e.target.value))))}
            style={{ ...styles.select, width: 70 }}
          />
        </div>
        <div>
          <label style={styles.label}>Games per pairing</label>
          <input
            type="number"
            min={1}
            max={9}
            value={gamesPerMatch}
            onChange={(e) => setGamesPerMatch(Math.min(9, Math.max(1, Number(e.target.value))))}
            style={{ ...styles.select, width: 70 }}
          />
        </div>
      </div>

      <label style={styles.label}>Deck building</label>
      <div style={styles.radioRow}>
        <label style={styles.radioLabel}>
          <input type="radio" checked={deckbuild === 'heuristic'} onChange={() => setDeckbuild('heuristic')} />
          Heuristic (fast)
        </label>
        <label style={styles.radioLabel}>
          <input type="radio" checked={deckbuild === 'llm'} onChange={() => setDeckbuild('llm')} />
          Server default (LLM if configured)
        </label>
      </div>

      <div style={{ marginTop: 18 }}>
        <button style={styles.primaryBtn} disabled={busy} onClick={create}>
          {busy ? 'Creating…' : 'Create & watch'}
        </button>
      </div>

      <p style={styles.smallNote}>
        Every seat is a bot playing with whatever AI the server is configured for
        (<code>game.ai.mode</code> — <code>engine</code> by default). Each opens its own 6-pack
        sealed pool, builds a deck, and plays a round robin unattended. Requires the server running
        with <code>game.dev-endpoints.enabled=true</code>.
      </p>

      {pickerOpen && (
        <SetPickerModal
          sets={sets}
          selectedCodes={setCodes}
          mode="multi"
          title="Choose sets"
          onToggleSet={(code) =>
            setSetCodes((prev) => (prev.includes(code) ? prev.filter((c) => c !== code) : [...prev, code]))
          }
          onClose={() => setPickerOpen(false)}
        />
      )}
    </div>
  )
}

// ============================================================================
// Status panel
// ============================================================================

const STATE_LABELS: Record<string, string> = {
  WAITING_FOR_PLAYERS: 'Seating bots',
  DECK_BUILDING: 'Building decks',
  TOURNAMENT_ACTIVE: 'Playing',
  TOURNAMENT_COMPLETE: 'Finished',
}

function StatusPanel({
  status,
  autoWatch,
  onToggleAutoWatch,
  onNew,
}: {
  status: SandboxStatus
  autoWatch: boolean
  onToggleAutoWatch: (v: boolean) => void
  onNew: () => void
}) {
  const label = STATE_LABELS[status.state] ?? status.state
  const building = status.state === 'DECK_BUILDING'

  return (
    <>
      <div style={styles.pacingBar}>
        <div style={styles.statusGroup}>
          <span style={{ ...styles.statusPill, ...statePillColor(status.state) }}>{label}</span>
          <span style={styles.metaText}>
            {status.playerNames.length} bots
            {status.totalRounds > 0 && <> · round {status.round}/{status.totalRounds}</>}
            {building && <> · {status.decksSubmitted}/{status.playerNames.length} decks built</>}
          </span>
        </div>
        <div style={styles.controlsGroup}>
          <label style={styles.radioLabel} title="Jump straight into a game the moment one starts">
            <input type="checkbox" checked={autoWatch} onChange={(e) => onToggleAutoWatch(e.target.checked)} />
            Auto-watch
          </label>
          <a href={`/tournament/${status.lobbyId}`} style={styles.ctrlBtn}>Standings</a>
          <button style={styles.secondaryBtn} onClick={onNew}>New lobby</button>
        </div>
      </div>

      <div style={styles.card}>
        <h2 style={styles.h2}>Live games</h2>
        {status.liveGames.length === 0 ? (
          <p style={styles.hint}>
            {status.complete
              ? 'Every match is done — open Standings for the results, or start a new lobby.'
              : 'No game running yet. The bots are still getting to the table; this refreshes itself.'}
          </p>
        ) : (
          status.liveGames.map((game) => (
            <div key={game.gameSessionId} style={styles.gameRow}>
              <div>
                <div style={styles.gameNames}>{game.player1Name} vs {game.player2Name}</div>
                <div style={styles.hint}>
                  Turn {game.turnNumber} · {game.player1Life} / {game.player2Life} life
                </div>
              </div>
              <button style={styles.watchBtn} onClick={() => watchGame(game.gameSessionId)}>
                Watch
              </button>
            </div>
          ))
        )}
      </div>

      <div style={styles.card}>
        <h2 style={styles.h2}>Table</h2>
        <div style={styles.botList}>
          {status.playerNames.map((name) => (
            <span key={name} style={styles.botChip}>{name}</span>
          ))}
        </div>
      </div>
    </>
  )
}

function statePillColor(state: string): React.CSSProperties {
  switch (state) {
    case 'TOURNAMENT_ACTIVE':
      return { background: '#166534', color: '#dcfce7' }
    case 'TOURNAMENT_COMPLETE':
      return { background: '#3730a3', color: '#e0e7ff' }
    default:
      return { background: '#78350f', color: '#fef3c7' }
  }
}

// ============================================================================
// Styles — inline, matching the LLM Tournament page's dev-tool look.
// ============================================================================

const styles: Record<string, React.CSSProperties> = {
  page: {
    minHeight: '100vh',
    background: '#0a0a12',
    color: '#e2e8f0',
    padding: '20px 28px',
    fontFamily: 'system-ui, sans-serif',
  },
  header: { display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 16 },
  h1: { fontSize: 26, margin: 0, display: 'flex', alignItems: 'center', gap: 10 },
  devTag: {
    fontSize: 11,
    background: '#7f1d1d',
    color: '#fecaca',
    padding: '2px 8px',
    borderRadius: 999,
    textTransform: 'uppercase',
    letterSpacing: 1,
  },
  homeLink: { color: '#7dd3fc', textDecoration: 'none' },
  h2: { fontSize: 16, margin: '0 0 12px', color: '#cbd5e1' },
  errorBar: { background: '#7f1d1d', color: '#fecaca', padding: '10px 14px', borderRadius: 8, marginBottom: 14, cursor: 'pointer' },
  card: { background: '#11131d', border: '1px solid #1f2433', borderRadius: 12, padding: 18, marginBottom: 16, maxWidth: 760 },
  label: { display: 'block', fontSize: 12, color: '#94a3b8', marginBottom: 6 },
  hint: { fontSize: 12, color: '#64748b', marginTop: 6 },
  formRow: { display: 'flex', gap: 18, alignItems: 'flex-start', margin: '0 0 16px' },
  select: {
    background: '#0a0a12',
    color: '#e2e8f0',
    border: '1px solid #2a3142',
    borderRadius: 8,
    padding: '8px 10px',
    fontSize: 13,
  },
  setButton: {
    background: '#0a0a12',
    color: '#e2e8f0',
    border: '1px solid #2a3142',
    borderRadius: 8,
    padding: '8px 12px',
    fontSize: 13,
    cursor: 'pointer',
    textAlign: 'left',
    minWidth: 220,
  },
  radioRow: { display: 'flex', gap: 14, paddingTop: 6 },
  radioLabel: { fontSize: 13, display: 'flex', alignItems: 'center', gap: 6, cursor: 'pointer' },
  primaryBtn: {
    background: '#2563eb',
    color: 'white',
    border: 'none',
    borderRadius: 8,
    padding: '10px 18px',
    fontSize: 14,
    cursor: 'pointer',
    fontWeight: 600,
  },
  smallNote: { fontSize: 11, color: '#64748b', marginTop: 12, lineHeight: 1.5 },
  pacingBar: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    flexWrap: 'wrap',
    gap: 12,
    background: '#11131d',
    border: '1px solid #1f2433',
    borderRadius: 12,
    padding: '12px 16px',
    marginBottom: 16,
    maxWidth: 760,
  },
  statusGroup: { display: 'flex', alignItems: 'center', gap: 12 },
  statusPill: { fontSize: 12, fontWeight: 700, padding: '4px 12px', borderRadius: 999, letterSpacing: 0.5 },
  metaText: { fontSize: 12, color: '#94a3b8' },
  controlsGroup: { display: 'flex', alignItems: 'center', gap: 10, flexWrap: 'wrap' },
  ctrlBtn: {
    background: '#1e293b',
    color: '#e2e8f0',
    border: '1px solid #334155',
    borderRadius: 8,
    padding: '8px 14px',
    fontSize: 13,
    cursor: 'pointer',
    textDecoration: 'none',
  },
  secondaryBtn: {
    background: 'transparent',
    color: '#7dd3fc',
    border: '1px solid #334155',
    borderRadius: 8,
    padding: '8px 14px',
    fontSize: 13,
    cursor: 'pointer',
  },
  watchBtn: {
    background: '#166534',
    color: '#dcfce7',
    border: '1px solid #16a34a',
    borderRadius: 8,
    padding: '8px 16px',
    fontSize: 13,
    fontWeight: 600,
    cursor: 'pointer',
  },
  gameRow: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: 12,
    padding: '10px 0',
    borderTop: '1px solid #1f2433',
  },
  gameNames: { fontSize: 14, fontWeight: 600 },
  botList: { display: 'flex', flexWrap: 'wrap', gap: 8 },
  botChip: {
    background: '#1e293b',
    border: '1px solid #334155',
    borderRadius: 999,
    padding: '4px 12px',
    fontSize: 12,
    color: '#cbd5e1',
  },
}
