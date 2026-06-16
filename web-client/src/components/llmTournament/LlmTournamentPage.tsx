import { useCallback, useEffect, useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import type { AvailableSet } from '@/types/messages'
import { SetPickerModal } from '@/components/ui/SetPickerModal'

// ============================================================================
// Types mirroring LlmTournamentController DTOs
// ============================================================================

interface CostView {
  costUsd: number
  totalTokens: number
  calls: number
  costKnown: boolean
}

interface ParticipantView {
  id: string
  modelId: string
  displayName: string
  seed: number
  buildStatus: string
  deckSize: number
  buildCost: CostView | null
  deck: Record<string, number>
}

interface FinishedGameView {
  gameSessionId: string
  cost: CostView
}

interface MatchView {
  id: string
  roundNumber: number
  slot: number
  player1Name: string | null
  player2Name: string | null
  player1ModelId: string | null
  player2ModelId: string | null
  player1GameWins: number
  player2GameWins: number
  winnerName: string | null
  bye: boolean
  complete: boolean
  status: 'scheduled' | 'live' | 'done' | 'bye'
  currentGameSessionId: string | null
  finishedGames: FinishedGameView[]
  cost: CostView
}

interface RoundView {
  roundNumber: number
  matches: MatchView[]
}

interface TournamentView {
  id: string
  setCode: string
  deckbuildMode: string
  bestOf: number
  status: string
  thinkingDelayMs: number
  championName: string | null
  participants: ParticipantView[]
  rounds: RoundView[]
  gameCost: CostView
  deckbuildCost: CostView
  llmAvailable: boolean
}

const API = '/api/dev/llm-tournament'

/** Format a dollar cost; costKnown=false → shown as a floor (e.g. "≥$0.0000"). */
function fmtCost(c: CostView | null | undefined): string {
  if (!c || c.calls === 0) return '—'
  const dollars = c.costUsd
  const prefix = c.costKnown ? '$' : '≥$'
  const value = dollars >= 0.01 ? dollars.toFixed(4) : dollars.toFixed(6)
  return `${prefix}${value}`
}

function fmtTokens(n: number): string {
  if (n >= 1000) return `${(n / 1000).toFixed(1)}k`
  return String(n)
}

/** The game session id of whatever match is live right now (one at a time), else null. */
function currentLiveGameId(t: TournamentView): string | null {
  for (const round of t.rounds) {
    for (const m of round.matches) {
      if (m.status === 'live' && m.currentGameSessionId) return m.currentGameSessionId
    }
  }
  return null
}

const PRESET_MODELS = [
  'deepseek/deepseek-v4-flash',
  'tencent/hy3-preview',
  'minimax/minimax-m3',
  'xiaomi/mimo-v2.5',
]

// ============================================================================
// Page
// ============================================================================

export function LlmTournamentPage() {
  const { id: routeId } = useParams<{ id?: string }>()
  const navigate = useNavigate()
  const [tournament, setTournament] = useState<TournamentView | null>(null)
  const [error, setError] = useState<string | null>(null)
  const tournamentIdRef = useRef<string | null>(routeId ?? null)

  // Poll the active tournament. The id lives in the URL (/llm-tournament/:id) so a page refresh
  // re-loads the same tournament; a 404 (e.g. server restarted — tournaments are in-memory) drops
  // back to the setup form.
  const refresh = useCallback(async () => {
    const id = tournamentIdRef.current
    if (!id) return
    try {
      const res = await fetch(`${API}/${id}`)
      if (res.ok) {
        setTournament(await res.json())
      } else if (res.status === 404) {
        tournamentIdRef.current = null
        setTournament(null)
        navigate('/llm-tournament', { replace: true })
      }
    } catch {
      /* transient — keep last view */
    }
  }, [navigate])

  // Sync the active id from the URL (initial load, refresh, back/forward) and fetch immediately.
  useEffect(() => {
    tournamentIdRef.current = routeId ?? null
    if (routeId) {
      void refresh()
    } else {
      setTournament(null)
    }
  }, [routeId, refresh])

  useEffect(() => {
    const interval = setInterval(refresh, 1500)
    return () => clearInterval(interval)
  }, [refresh])

  const selectTournament = (view: TournamentView) => {
    tournamentIdRef.current = view.id
    setTournament(view)
    navigate(`/llm-tournament/${view.id}`)
  }

  const control = async (action: string, body?: unknown) => {
    const id = tournamentIdRef.current
    if (!id) return
    try {
      const init: RequestInit = body
        ? { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) }
        : { method: 'POST' }
      const res = await fetch(`${API}/${id}/${action}`, init)
      if (res.ok) setTournament(await res.json())
    } catch (e) {
      setError(String(e))
    }
  }

  return (
    <div style={styles.page}>
      <header style={styles.header}>
        <h1 style={styles.h1}>🏆 LLM Tournament <span style={styles.devTag}>dev</span></h1>
        <a href="/" style={styles.homeLink}>← Home</a>
      </header>

      {error && (
        <div style={styles.errorBar} onClick={() => setError(null)}>
          {error} <span style={{ float: 'right' }}>✕</span>
        </div>
      )}

      {!tournament && (
        <SetupForm onCreated={selectTournament} onError={setError} />
      )}

      {tournament && !tournament.llmAvailable && (
        <div style={styles.warnBar}>
          ⚠ No LLM API key configured on the server — deck building and in-game play fall back to the
          built-in engine AI regardless of the LLM setting. Set <code>GAME_AI_API_KEY</code> (and an
          OpenRouter <code>GAME_AI_BASE_URL</code>) in <code>.env</code> and start with{' '}
          <code>just server</code> (a bare <code>./gradlew bootRun</code> doesn't load <code>.env</code>).
        </div>
      )}

      {tournament && (
        <>
          <PacingBar
            tournament={tournament}
            onStart={() => control('start')}
            onPause={() => control('pause')}
            onResume={() => control('resume')}
            onStep={() => control('step')}
            onSpeed={(ms) => control('speed', { thinkingDelayMs: ms })}
            onNew={() => {
              tournamentIdRef.current = null
              setTournament(null)
              navigate('/llm-tournament')
            }}
          />
          {tournament.status === 'BUILDING' && <BuildProgress tournament={tournament} />}
          <Bracket tournament={tournament} />
          <ParticipantList tournament={tournament} />
        </>
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
  onCreated: (t: TournamentView) => void
  onError: (e: string) => void
}) {
  const [sets, setSets] = useState<AvailableSet[]>([])
  const [setCode, setSetCode] = useState('')
  const [pickerOpen, setPickerOpen] = useState(false)
  const [modelsText, setModelsText] = useState(PRESET_MODELS.slice(0, 4).join('\n'))
  const [mode, setMode] = useState<'heuristic' | 'llm'>('heuristic')
  const [bestOf, setBestOf] = useState(3)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    fetch(`${API}/sets`)
      .then((r) => (r.ok ? r.json() : []))
      .then((s: AvailableSet[]) => {
        setSets(s)
        // Default to the newest fully-implemented set, matching the lobby picker's bias.
        const def = s.filter((x) => !x.partial).sort((a, b) => (b.releaseDate ?? '').localeCompare(a.releaseDate ?? ''))[0]
        if (def && !setCode) setSetCode(def.code)
      })
      .catch(() => onError('Failed to load sets — is the server running with dev endpoints enabled?'))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const selectedSet = sets.find((s) => s.code === setCode)

  const models = modelsText
    .split(/[\n,]/)
    .map((m) => m.trim())
    .filter(Boolean)

  const addPreset = (m: string) => {
    if (!models.includes(m)) setModelsText((t) => (t.trim() ? `${t.trim()}\n${m}` : m))
  }

  const create = async () => {
    if (models.length < 2) {
      onError('Add at least 2 models')
      return
    }
    if (!setCode) {
      onError('Pick a set')
      return
    }
    setBusy(true)
    try {
      const res = await fetch(API, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ modelIds: models, setCode, deckbuildMode: mode, bestOf }),
      })
      const data = await res.json()
      if (res.ok) onCreated(data)
      else onError(data.error || 'Failed to create tournament')
    } catch (e) {
      onError(String(e))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div style={styles.card}>
      <h2 style={styles.h2}>New Tournament</h2>

      <label style={styles.label}>Models (one per line — they are seeded top to bottom)</label>
      <textarea
        value={modelsText}
        onChange={(e) => setModelsText(e.target.value)}
        rows={6}
        spellCheck={false}
        style={styles.textarea}
        placeholder="anthropic/claude-sonnet-4"
      />
      <div style={styles.presetRow}>
        {PRESET_MODELS.map((m) => (
          <button key={m} style={styles.presetBtn} onClick={() => addPreset(m)}>
            + {m.split('/').pop()}
          </button>
        ))}
      </div>
      <div style={styles.hint}>{models.length} model{models.length === 1 ? '' : 's'}</div>

      <div style={styles.formRow}>
        <div style={{ flex: 1 }}>
          <label style={styles.label}>Set</label>
          <button type="button" style={styles.setButton} onClick={() => setPickerOpen(true)}>
            {selectedSet ? `${selectedSet.name} (${selectedSet.code})` : 'Choose a set…'}
          </button>
        </div>
        <div>
          <label style={styles.label}>Deck building</label>
          <div style={styles.radioRow}>
            <label style={styles.radioLabel}>
              <input type="radio" checked={mode === 'heuristic'} onChange={() => setMode('heuristic')} /> Heuristic
            </label>
            <label style={styles.radioLabel}>
              <input type="radio" checked={mode === 'llm'} onChange={() => setMode('llm')} /> Each LLM builds
            </label>
          </div>
        </div>
        <div>
          <label style={styles.label}>Best of</label>
          <input
            type="number"
            min={1}
            max={9}
            step={2}
            value={bestOf}
            onChange={(e) => setBestOf(Number(e.target.value))}
            style={{ ...styles.select, width: 70 }}
          />
        </div>
      </div>

      <button style={styles.primaryBtn} disabled={busy} onClick={create}>
        {busy ? 'Creating…' : 'Create & build decks'}
      </button>
      <p style={styles.smallNote}>
        Sealed: each model opens its own 6-pack pool. Single-elimination knockout. Requires the server
        running with <code>game.dev-endpoints.enabled=true</code> and an OpenRouter API key.
      </p>

      {pickerOpen && (
        <SetPickerModal
          sets={sets}
          selectedCodes={setCode ? [setCode] : []}
          mode="single"
          title="Choose a set"
          onToggleSet={(code) => setSetCode(code)}
          onClose={() => setPickerOpen(false)}
        />
      )}
    </div>
  )
}

// ============================================================================
// Pacing bar
// ============================================================================

function PacingBar({
  tournament,
  onStart,
  onPause,
  onResume,
  onStep,
  onSpeed,
  onNew,
}: {
  tournament: TournamentView
  onStart: () => void
  onPause: () => void
  onResume: () => void
  onStep: () => void
  onSpeed: (ms: number) => void
  onNew: () => void
}) {
  const status = tournament.status
  const building = status === 'BUILDING'
  const running = status === 'RUNNING'
  const complete = status === 'COMPLETE'
  const liveGameId = currentLiveGameId(tournament)

  return (
    <div style={styles.pacingBar}>
      <div style={styles.statusGroup}>
        <span style={{ ...styles.statusPill, ...statusPillColor(status) }}>{status}</span>
        <span style={styles.metaText}>
          {tournament.setCode} · {tournament.deckbuildMode} · Bo{tournament.bestOf}
        </span>
        {complete && tournament.championName && (
          <span style={styles.champion}>👑 {tournament.championName}</span>
        )}
        <span style={styles.costSummary} title={
          `Games: ${fmtCost(tournament.gameCost)} (${fmtTokens(tournament.gameCost.totalTokens)} tok, ${tournament.gameCost.calls} calls)\n` +
          `Deckbuild: ${fmtCost(tournament.deckbuildCost)} (${fmtTokens(tournament.deckbuildCost.totalTokens)} tok)`
        }>
          💰 games {fmtCost(tournament.gameCost)}
          {tournament.deckbuildCost.calls > 0 && <> · build {fmtCost(tournament.deckbuildCost)}</>}
        </span>
      </div>

      <div style={styles.controlsGroup}>
        {liveGameId && (
          <a
            href={`/?spectate=${liveGameId}`}
            target="_blank"
            rel="noreferrer"
            style={styles.watchLiveBtn}
            title="Open the in-progress game in a new tab — updates to whichever game is live"
          >
            👁 Watch live game
          </a>
        )}
        {!running && !complete && (
          <button style={styles.ctrlBtn} disabled={building} onClick={status === 'PAUSED' ? onResume : onStart}>
            ▶ {status === 'PAUSED' ? 'Resume' : 'Start'}
          </button>
        )}
        {running && (
          <button style={styles.ctrlBtn} onClick={onPause}>
            ⏸ Pause
          </button>
        )}
        <button style={styles.ctrlBtn} disabled={building || running || complete} onClick={onStep}>
          ⏭ Step game
        </button>

        <div style={styles.speedGroup}>
          <span style={styles.metaText}>Speed</span>
          <input
            type="range"
            min={0}
            max={3000}
            step={100}
            value={tournament.thinkingDelayMs}
            onChange={(e) => onSpeed(Number(e.target.value))}
            style={{ width: 120 }}
          />
          <span style={styles.metaText}>{tournament.thinkingDelayMs}ms</span>
        </div>

        <button style={styles.secondaryBtn} onClick={onNew}>
          + New
        </button>
      </div>
    </div>
  )
}

// ============================================================================
// Build progress
// ============================================================================

const BUILD_STAGE_LABEL: Record<string, string> = {
  PENDING: 'queued',
  BUILDING_POOL: 'opening packs…',
  BUILDING_DECK: 'building deck…',
  READY: 'ready',
  FAILED: 'failed',
}

function BuildProgress({ tournament }: { tournament: TournamentView }) {
  const total = tournament.participants.length
  const ready = tournament.participants.filter((p) => p.buildStatus === 'READY').length
  const failed = tournament.participants.filter((p) => p.buildStatus === 'FAILED').length
  const pct = total > 0 ? Math.round(((ready + failed) / total) * 100) : 0
  const inProgress = tournament.deckbuildMode === 'llm'
  return (
    <div style={styles.buildCard}>
      <div style={styles.buildHeader}>
        <span style={styles.buildTitle}>
          {inProgress ? 'Models are building decks…' : 'Building pools & decks…'}
        </span>
        <span style={styles.metaText}>{ready}/{total} ready{failed > 0 ? ` · ${failed} failed` : ''}</span>
      </div>
      <div style={styles.progressTrack}>
        <div style={{ ...styles.progressFill, width: `${pct}%` }} />
      </div>
      <div style={styles.buildList}>
        {tournament.participants.map((p) => {
          const active = p.buildStatus === 'BUILDING_POOL' || p.buildStatus === 'BUILDING_DECK'
          return (
            <div key={p.id} style={styles.buildRow}>
              <span style={styles.buildRowName}>
                {active && <span style={styles.spinner} />}
                {p.displayName}
              </span>
              <span style={{ ...styles.miniPill, ...buildStatusColor(p.buildStatus) }}>
                {BUILD_STAGE_LABEL[p.buildStatus] ?? p.buildStatus}
              </span>
            </div>
          )
        })}
      </div>
      <style>{`@keyframes llmtspin { to { transform: rotate(360deg); } }`}</style>
    </div>
  )
}

// ============================================================================
// Bracket
// ============================================================================

function Bracket({ tournament }: { tournament: TournamentView }) {
  if (tournament.rounds.length === 0) return null
  return (
    <div style={styles.bracketScroll}>
      <div style={styles.bracket}>
        {tournament.rounds.map((round) => (
          <div key={round.roundNumber} style={styles.roundCol}>
            <div style={styles.roundTitle}>{roundLabel(round, tournament.rounds.length)}</div>
            <div style={styles.matchStack}>
              {round.matches.map((m) => (
                <MatchCard key={m.id} match={m} />
              ))}
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}

function MatchCard({ match }: { match: MatchView }) {
  return (
    <div style={{ ...styles.matchCard, ...(match.status === 'live' ? styles.matchLive : {}) }}>
      <SeatRow
        name={match.player1Name}
        model={match.player1ModelId}
        wins={match.player1GameWins}
        winner={!!match.winnerName && match.winnerName === match.player1Name}
      />
      <div style={styles.seatDivider} />
      <SeatRow
        name={match.player2Name}
        model={match.player2ModelId}
        wins={match.player2GameWins}
        winner={!!match.winnerName && match.winnerName === match.player2Name}
      />
      <div style={styles.matchFooter}>
        <span style={{ ...styles.miniPill, ...statusPillColor(matchStatusToStatus(match.status)) }}>
          {match.status}
        </span>
        <span style={styles.matchLinks}>
          {match.status === 'live' && match.currentGameSessionId && (
            <a
              href={`/?spectate=${match.currentGameSessionId}`}
              target="_blank"
              rel="noreferrer"
              style={styles.watchLink}
            >
              👁 Watch
            </a>
          )}
          {match.finishedGames.map((g, i) => (
            <a
              key={g.gameSessionId}
              href={`/replay/${g.gameSessionId}`}
              target="_blank"
              rel="noreferrer"
              style={styles.replayLink}
              title={`Game ${i + 1}: ${fmtCost(g.cost)} · ${fmtTokens(g.cost.totalTokens)} tokens`}
            >
              ▷ G{i + 1}
            </a>
          ))}
        </span>
      </div>
      {match.cost.calls > 0 && (
        <div style={styles.matchCostRow}>
          💰 {fmtCost(match.cost)} · {fmtTokens(match.cost.totalTokens)} tok
        </div>
      )}
    </div>
  )
}

function SeatRow({
  name,
  model,
  wins,
  winner,
}: {
  name: string | null
  model: string | null
  wins: number
  winner: boolean
}) {
  return (
    <div style={{ ...styles.seatRow, ...(winner ? styles.seatWinner : {}) }}>
      <div style={styles.seatInfo}>
        <div style={styles.seatName}>{name ?? <span style={styles.bye}>— bye —</span>}</div>
        {model && <div style={styles.seatModel}>{model}</div>}
      </div>
      <div style={styles.seatWins}>{name ? wins : ''}</div>
    </div>
  )
}

// ============================================================================
// Participants
// ============================================================================

function ParticipantList({ tournament }: { tournament: TournamentView }) {
  const [deckOf, setDeckOf] = useState<ParticipantView | null>(null)
  // Keep the open deck modal in sync as polling refreshes the tournament object.
  const liveDeckOf = deckOf ? tournament.participants.find((p) => p.id === deckOf.id) ?? deckOf : null

  return (
    <div style={styles.card}>
      <h2 style={styles.h2}>Participants</h2>
      <table style={styles.table}>
        <thead>
          <tr>
            <th style={styles.th}>Seed</th>
            <th style={styles.th}>Name</th>
            <th style={styles.th}>Model</th>
            <th style={styles.th}>Deck</th>
            <th style={styles.th}>Build</th>
            <th style={styles.th}>Build cost</th>
          </tr>
        </thead>
        <tbody>
          {tournament.participants.map((p) => (
            <tr key={p.id}>
              <td style={styles.td}>{p.seed}</td>
              <td style={styles.td}>{p.displayName}</td>
              <td style={{ ...styles.td, color: '#7dd3fc' }}>{p.modelId}</td>
              <td style={styles.td}>
                {p.deckSize > 0 ? (
                  <button style={styles.linkButton} onClick={() => setDeckOf(p)}>
                    {p.deckSize} cards ▸
                  </button>
                ) : '—'}
              </td>
              <td style={styles.td}>
                <span style={{ ...styles.miniPill, ...buildStatusColor(p.buildStatus) }}>{p.buildStatus}</span>
              </td>
              <td style={styles.td}>
                {p.buildCost ? `${fmtCost(p.buildCost)} (${fmtTokens(p.buildCost.totalTokens)} tok)` : '—'}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      {liveDeckOf && <DeckModal participant={liveDeckOf} onClose={() => setDeckOf(null)} />}
    </div>
  )
}

const BASIC_LANDS = new Set(['Plains', 'Island', 'Swamp', 'Mountain', 'Forest'])

function DeckModal({ participant, onClose }: { participant: ParticipantView; onClose: () => void }) {
  const entries = Object.entries(participant.deck)
  const sortFn = (a: [string, number], b: [string, number]) => b[1] - a[1] || a[0].localeCompare(b[0])
  const spells = entries.filter(([name]) => !BASIC_LANDS.has(name)).sort(sortFn)
  const lands = entries.filter(([name]) => BASIC_LANDS.has(name)).sort(sortFn)
  const spellCount = spells.reduce((s, [, n]) => s + n, 0)
  const landCount = lands.reduce((s, [, n]) => s + n, 0)

  const renderGroup = (title: string, rows: [string, number][]) =>
    rows.length > 0 && (
      <div style={styles.deckGroup}>
        <div style={styles.deckGroupTitle}>{title}</div>
        {rows.map(([name, count]) => (
          <div key={name} style={styles.deckLine}>
            <span style={styles.deckCount}>{count}</span>
            <span>{name}</span>
          </div>
        ))}
      </div>
    )

  return (
    <div style={styles.modalBackdrop} onClick={onClose}>
      <div style={styles.modalPanel} onClick={(e) => e.stopPropagation()}>
        <div style={styles.modalHeader}>
          <div>
            <div style={styles.modalTitle}>{participant.displayName}</div>
            <div style={styles.seatModel}>{participant.modelId} · {participant.deckSize} cards</div>
          </div>
          <button style={styles.modalClose} onClick={onClose}>×</button>
        </div>
        <div style={styles.deckBody}>
          {renderGroup(`Spells (${spellCount})`, spells)}
          {renderGroup(`Lands (${landCount})`, lands)}
        </div>
      </div>
    </div>
  )
}

// ============================================================================
// Helpers + styles
// ============================================================================

function matchStatusToStatus(s: string): string {
  switch (s) {
    case 'live':
      return 'RUNNING'
    case 'done':
      return 'COMPLETE'
    case 'bye':
      return 'READY'
    default:
      return 'PAUSED'
  }
}

function roundLabel(round: RoundView, totalRounds: number): string {
  const fromEnd = totalRounds - round.roundNumber
  if (fromEnd === 0) return 'Final'
  if (fromEnd === 1) return 'Semifinals'
  if (fromEnd === 2) return 'Quarterfinals'
  return `Round ${round.roundNumber}`
}

function statusPillColor(status: string): React.CSSProperties {
  switch (status) {
    case 'RUNNING':
      return { background: '#166534', color: '#dcfce7' }
    case 'PAUSED':
      return { background: '#854d0e', color: '#fef9c3' }
    case 'COMPLETE':
      return { background: '#1e40af', color: '#dbeafe' }
    case 'BUILDING':
      return { background: '#4b5563', color: '#e5e7eb' }
    case 'READY':
      return { background: '#3730a3', color: '#e0e7ff' }
    default:
      return { background: '#374151', color: '#e5e7eb' }
  }
}

function buildStatusColor(status: string): React.CSSProperties {
  switch (status) {
    case 'READY':
      return { background: '#166534', color: '#dcfce7' }
    case 'FAILED':
      return { background: '#7f1d1d', color: '#fecaca' }
    case 'PENDING':
      return { background: '#374151', color: '#e5e7eb' }
    default:
      return { background: '#854d0e', color: '#fef9c3' }
  }
}

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
  warnBar: { background: '#422006', color: '#fed7aa', border: '1px solid #854d0e', padding: '10px 14px', borderRadius: 8, marginBottom: 14, fontSize: 13, lineHeight: 1.5 },
  card: { background: '#11131d', border: '1px solid #1f2433', borderRadius: 12, padding: 18, marginBottom: 16, maxWidth: 760 },
  label: { display: 'block', fontSize: 12, color: '#94a3b8', marginBottom: 6 },
  textarea: {
    width: '100%',
    background: '#0a0a12',
    color: '#e2e8f0',
    border: '1px solid #2a3142',
    borderRadius: 8,
    padding: 10,
    fontFamily: 'monospace',
    fontSize: 13,
    boxSizing: 'border-box',
    resize: 'vertical',
  },
  presetRow: { display: 'flex', flexWrap: 'wrap', gap: 6, marginTop: 8 },
  presetBtn: {
    background: '#1e293b',
    color: '#cbd5e1',
    border: '1px solid #334155',
    borderRadius: 6,
    padding: '4px 8px',
    fontSize: 11,
    cursor: 'pointer',
  },
  hint: { fontSize: 12, color: '#64748b', marginTop: 6 },
  formRow: { display: 'flex', gap: 18, alignItems: 'flex-start', margin: '16px 0' },
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
  costSummary: { fontSize: 12, color: '#fbbf24', fontWeight: 600, cursor: 'help' },
  matchCostRow: {
    padding: '4px 12px',
    fontSize: 11,
    color: '#fbbf24',
    background: '#0c0e16',
    borderTop: '1px solid #161a26',
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
  },
  statusGroup: { display: 'flex', alignItems: 'center', gap: 12 },
  statusPill: { fontSize: 12, fontWeight: 700, padding: '4px 12px', borderRadius: 999, letterSpacing: 0.5 },
  metaText: { fontSize: 12, color: '#94a3b8' },
  champion: { fontSize: 14, fontWeight: 700, color: '#fde047' },
  controlsGroup: { display: 'flex', alignItems: 'center', gap: 10, flexWrap: 'wrap' },
  ctrlBtn: {
    background: '#1e293b',
    color: '#e2e8f0',
    border: '1px solid #334155',
    borderRadius: 8,
    padding: '8px 14px',
    fontSize: 13,
    cursor: 'pointer',
  },
  watchLiveBtn: {
    background: '#166534',
    color: '#dcfce7',
    border: '1px solid #16a34a',
    borderRadius: 8,
    padding: '8px 14px',
    fontSize: 13,
    fontWeight: 600,
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
  speedGroup: { display: 'flex', alignItems: 'center', gap: 8 },
  buildCard: {
    background: '#11131d',
    border: '1px solid #1f2433',
    borderRadius: 12,
    padding: 16,
    marginBottom: 16,
  },
  buildHeader: { display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 10 },
  buildTitle: { fontSize: 14, fontWeight: 600, color: '#cbd5e1' },
  progressTrack: { height: 6, background: '#1f2433', borderRadius: 999, overflow: 'hidden', marginBottom: 12 },
  progressFill: { height: '100%', background: '#2563eb', transition: 'width 0.4s ease' },
  buildList: { display: 'flex', flexDirection: 'column', gap: 6 },
  buildRow: { display: 'flex', alignItems: 'center', justifyContent: 'space-between', fontSize: 13 },
  buildRowName: { display: 'flex', alignItems: 'center', gap: 8 },
  spinner: {
    width: 12,
    height: 12,
    border: '2px solid #334155',
    borderTopColor: '#7dd3fc',
    borderRadius: '50%',
    display: 'inline-block',
    animation: 'llmtspin 0.8s linear infinite',
  },
  linkButton: {
    background: 'none',
    border: 'none',
    color: '#7dd3fc',
    cursor: 'pointer',
    fontSize: 13,
    padding: 0,
    textDecoration: 'underline',
  },
  modalBackdrop: {
    position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.6)',
    display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000,
  },
  modalPanel: {
    background: '#11131d', border: '1px solid #2a3142', borderRadius: 12,
    width: 'min(520px, 92vw)', maxHeight: '82vh', display: 'flex', flexDirection: 'column',
  },
  modalHeader: {
    display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between',
    padding: '14px 18px', borderBottom: '1px solid #1f2433',
  },
  modalTitle: { fontSize: 16, fontWeight: 700 },
  modalClose: { background: 'none', border: 'none', color: '#94a3b8', fontSize: 24, cursor: 'pointer', lineHeight: 1 },
  deckBody: { padding: 18, overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: 18 },
  deckGroup: {},
  deckGroupTitle: { fontSize: 13, fontWeight: 700, color: '#94a3b8', marginBottom: 8 },
  deckLine: { display: 'flex', gap: 10, fontSize: 13, padding: '2px 0' },
  deckCount: { color: '#64748b', minWidth: 20, textAlign: 'right' },
  bracketScroll: { overflowX: 'auto', paddingBottom: 12, marginBottom: 16 },
  bracket: { display: 'flex', gap: 28, alignItems: 'stretch', minHeight: 100 },
  roundCol: { display: 'flex', flexDirection: 'column', minWidth: 220 },
  roundTitle: { fontSize: 13, fontWeight: 700, color: '#94a3b8', marginBottom: 12, textAlign: 'center' },
  matchStack: { display: 'flex', flexDirection: 'column', justifyContent: 'space-around', flex: 1, gap: 14 },
  matchCard: { background: '#11131d', border: '1px solid #1f2433', borderRadius: 10, overflow: 'hidden' },
  matchLive: { border: '1px solid #16a34a', boxShadow: '0 0 0 1px #16a34a55' },
  seatRow: { display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '8px 12px' },
  seatWinner: { background: '#0f291a' },
  seatInfo: { minWidth: 0 },
  seatName: { fontSize: 13, fontWeight: 600, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis', maxWidth: 150 },
  seatModel: { fontSize: 10, color: '#64748b', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis', maxWidth: 150 },
  seatWins: { fontSize: 16, fontWeight: 700, color: '#e2e8f0', marginLeft: 8 },
  seatDivider: { height: 1, background: '#1f2433' },
  bye: { color: '#475569', fontStyle: 'italic', fontWeight: 400 },
  matchFooter: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    padding: '6px 12px',
    background: '#0c0e16',
    borderTop: '1px solid #1f2433',
  },
  miniPill: { fontSize: 10, fontWeight: 700, padding: '2px 8px', borderRadius: 999, textTransform: 'uppercase' },
  matchLinks: { display: 'flex', gap: 8, alignItems: 'center' },
  watchLink: { color: '#4ade80', fontSize: 12, textDecoration: 'none', fontWeight: 600 },
  replayLink: { color: '#7dd3fc', fontSize: 12, textDecoration: 'none' },
  table: { width: '100%', borderCollapse: 'collapse', fontSize: 13 },
  th: { textAlign: 'left', padding: '6px 10px', color: '#64748b', borderBottom: '1px solid #1f2433', fontWeight: 600 },
  td: { padding: '6px 10px', borderBottom: '1px solid #161a26' },
}
