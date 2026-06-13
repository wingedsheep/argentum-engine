/**
 * Scenario Builder / Tester.
 *
 * Construct an arbitrary board state — paste a scenario JSON, or search cards (by name or
 * Scryfall-style query, reusing the deckbuilder parser) and drop them into either player's
 * hand / battlefield / graveyard / exile / library with common toggles — then play it against
 * yourself (hotseat), the AI, or as two-player. Scenarios share via an encoded `?s=` URL.
 */
import { useCallback, useEffect, useMemo, useRef, useState, type CSSProperties } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { parseQuery, type CardSummary } from '../deckbuilder/cardFilter'
import {
  encodeScenario,
  buildScenarioUrl,
  decodeScenario,
  SCENARIO_SHARE_PARAM,
  SCENARIO_REPLAY_PARAM,
  SCENARIO_FRAME_PARAM,
} from './shareScenario'
import type {
  ScenarioBattlefieldCard,
  ScenarioCreateResponse,
  ScenarioMode,
  ScenarioSeatSpec,
  ScenarioSpec,
  ScenarioZone,
} from './types'
import { SCENARIO_ZONES } from './types'

type PlayerKey = 'player1' | 'player2'

interface BuilderPlayer {
  name: string
  life: number
  hand: string[]
  battlefield: ScenarioBattlefieldCard[]
  graveyard: string[]
  exile: string[]
  library: string[]
}

const PHASES = ['BEGINNING', 'PRECOMBAT_MAIN', 'COMBAT', 'POSTCOMBAT_MAIN', 'ENDING'] as const

function emptyPlayer(name: string): BuilderPlayer {
  return { name, life: 20, hand: [], battlefield: [], graveyard: [], exile: [], library: [] }
}

const ZONE_LABEL: Record<ScenarioZone, string> = {
  hand: 'Hand',
  battlefield: 'Battlefield',
  graveyard: 'Graveyard',
  exile: 'Exile',
  library: 'Library',
}

const MODE_HINT: Record<ScenarioMode, string> = {
  SELF: 'You play both sides yourself in one window.',
  AI: 'You play; the computer controls the opponent.',
  TWO_PLAYER: 'Two people — each gets their own link to join.',
}

// --- spec <-> builder conversions ----------------------------------------------------------

function toSpec(p1: BuilderPlayer, p2: BuilderPlayer, extraSeats: ScenarioSeatSpec[], opts: {
  phase: string
  activePlayer: number
  mode: ScenarioMode
}): ScenarioSpec {
  const playerConfig = (p: BuilderPlayer) => {
    const cfg: ScenarioSpec['player1'] = { lifeTotal: p.life }
    if (p.hand.length) cfg.hand = p.hand
    if (p.battlefield.length) cfg.battlefield = p.battlefield
    if (p.graveyard.length) cfg.graveyard = p.graveyard
    if (p.exile.length) cfg.exile = p.exile
    if (p.library.length) cfg.library = p.library
    return cfg
  }
  const spec: ScenarioSpec = {
    player1Name: p1.name,
    player2Name: p2.name,
    player1: playerConfig(p1),
    player2: playerConfig(p2),
    phase: opts.phase,
    activePlayer: opts.activePlayer,
    mode: opts.mode,
  }
  if (extraSeats.length > 0) {
    // N-player pod: send the full seat list (the server prefers it over the
    // legacy two-seat fields). Pods only support hotseat.
    spec.players = [
      { name: p1.name, config: playerConfig(p1) },
      { name: p2.name, config: playerConfig(p2) },
      ...extraSeats,
    ]
    spec.mode = 'SELF'
  } else if (opts.mode === 'AI') {
    spec.aiPlayer = 2
  }
  return spec
}

function playerFromSpec(name: string, cfg: ScenarioSpec['player1']): BuilderPlayer {
  return {
    name,
    life: cfg?.lifeTotal ?? 20,
    hand: cfg?.hand ? [...cfg.hand] : [],
    battlefield: cfg?.battlefield ? cfg.battlefield.map((c) => ({ ...c })) : [],
    graveyard: cfg?.graveyard ? [...cfg.graveyard] : [],
    exile: cfg?.exile ? [...cfg.exile] : [],
    library: cfg?.library ? [...cfg.library] : [],
  }
}

// --- component -----------------------------------------------------------------------------

export function ScenarioBuilderPage() {
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()

  const [catalog, setCatalog] = useState<CardSummary[]>([])
  const [catalogError, setCatalogError] = useState<string | null>(null)
  const [query, setQuery] = useState('')

  const [p1, setP1] = useState<BuilderPlayer>(() => emptyPlayer('Player 1'))
  const [p2, setP2] = useState<BuilderPlayer>(() => emptyPlayer('Player 2'))
  // Seats 3-4 of an N-player pod. Named here; their boards are edited via the
  // JSON editor (the visual panels stay two-seat). A pod always starts hotseat.
  const [extraSeats, setExtraSeats] = useState<ScenarioSeatSpec[]>([])
  const [phase, setPhase] = useState<string>('PRECOMBAT_MAIN')
  const [activePlayer, setActivePlayer] = useState<number>(1)
  const [mode, setMode] = useState<ScenarioMode>('SELF')

  const [targetSeat, setTargetSeat] = useState<PlayerKey>('player1')
  const [targetZone, setTargetZone] = useState<ScenarioZone>('battlefield')

  const [jsonText, setJsonText] = useState('')
  const [jsonOpen, setJsonOpen] = useState(false)
  const [status, setStatus] = useState<string | null>(null)
  const [errors, setErrors] = useState<string[]>([])
  const [starting, setStarting] = useState(false)
  const [snapshotLoading, setSnapshotLoading] = useState(false)

  // --- load catalog --------------------------------------------------------
  useEffect(() => {
    let cancelled = false
    fetch('/api/cards')
      .then((r) => (r.ok ? r.json() : Promise.reject(new Error(`HTTP ${r.status}`))))
      .then((list: CardSummary[]) => {
        if (!cancelled) setCatalog(list)
      })
      .catch((e: unknown) => {
        if (!cancelled) setCatalogError(e instanceof Error ? e.message : 'Failed to load cards')
      })
    return () => {
      cancelled = true
    }
  }, [])

  // --- exact-snapshot jump-in on load (?replay=<id>&frame=<n>) -------------
  // A snapshot link references a stored replay frame (kept short); inject it server-side and
  // jump straight into the exact position (it isn't editable in the builder).
  const snapshotLoadedRef = useRef(false)
  useEffect(() => {
    if (snapshotLoadedRef.current) return
    const replay = searchParams.get(SCENARIO_REPLAY_PARAM)
    const frame = searchParams.get(SCENARIO_FRAME_PARAM)
    if (!replay || frame == null) return
    snapshotLoadedRef.current = true
    setSnapshotLoading(true)
    void jumpInto(
      fetch('/api/scenarios/from-replay-frame', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ gameId: replay, frame: Number(frame) }),
      }),
    )
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [searchParams])

  // Drive a from-state / from-replay-frame response: on success, jump into the new session.
  const jumpInto = useCallback(async (req: Promise<Response>) => {
    try {
      const res = await req
      if (!res.ok) {
        setSnapshotLoading(false)
        setStatus('Failed to load the snapshot.')
        return
      }
      const data = (await res.json()) as ScenarioCreateResponse
      const human =
        data.player1.token && data.player1.token !== '(AI)' ? data.player1 : data.player2
      window.location.href = `/?token=${encodeURIComponent(human.token)}`
    } catch {
      setSnapshotLoading(false)
      setStatus('Failed to load the snapshot.')
    }
  }, [])

  // --- decode shared scenario on load (?s=<code>) --------------------------
  const sharedLoadedRef = useRef(false)
  useEffect(() => {
    if (sharedLoadedRef.current) return
    const code = searchParams.get(SCENARIO_SHARE_PARAM)
    if (!code) return
    sharedLoadedRef.current = true
    void decodeScenario(code).then((spec) => {
      if (spec) {
        applySpec(spec)
        setStatus('Loaded shared scenario.')
      } else {
        setStatus('Could not read the shared scenario link.')
      }
      // Strip the param so a refresh doesn't reload it over edits.
      setSearchParams(
        (prev) => {
          const next = new URLSearchParams(prev)
          next.delete(SCENARIO_SHARE_PARAM)
          return next
        },
        { replace: true },
      )
    })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [searchParams])

  const applySpec = useCallback((spec: ScenarioSpec) => {
    const seats = spec.players
    if (seats && seats.length >= 2) {
      // N-player spec: first two seats land in the editable panels, the rest
      // ride along as extra pod seats.
      setP1(playerFromSpec(seats[0]?.name ?? 'Player 1', seats[0]?.config))
      setP2(playerFromSpec(seats[1]?.name ?? 'Player 2', seats[1]?.config))
      setExtraSeats(seats.slice(2).map((s) => ({ ...s })))
    } else {
      setP1(playerFromSpec(spec.player1Name ?? 'Player 1', spec.player1))
      setP2(playerFromSpec(spec.player2Name ?? 'Player 2', spec.player2))
      setExtraSeats([])
    }
    if (spec.phase) setPhase(spec.phase)
    if (spec.activePlayer) setActivePlayer(spec.activePlayer)
    if (spec.mode) setMode(spec.mode)
  }, [])

  // --- search results ------------------------------------------------------
  const results = useMemo(() => {
    if (!catalog.length) return []
    const trimmed = query.trim()
    if (!trimmed) return catalog.slice(0, 60)
    let predicate: (c: CardSummary) => boolean
    try {
      predicate = parseQuery(trimmed)
    } catch {
      return []
    }
    return catalog.filter(predicate).slice(0, 60)
  }, [catalog, query])

  // --- mutations -----------------------------------------------------------
  const seatSetter = (seat: PlayerKey) => (seat === 'player1' ? setP1 : setP2)

  const addCard = useCallback((seat: PlayerKey, zone: ScenarioZone, name: string) => {
    seatSetter(seat)((prev) => {
      if (zone === 'battlefield') {
        return { ...prev, battlefield: [...prev.battlefield, { name }] }
      }
      return { ...prev, [zone]: [...prev[zone], name] }
    })
    setStatus(`Added ${name} → ${ZONE_LABEL[zone]} (${seat === 'player1' ? p1.name : p2.name}).`)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [p1.name, p2.name])

  const removeFromZone = useCallback((seat: PlayerKey, zone: ScenarioZone, index: number) => {
    seatSetter(seat)((prev) => {
      if (zone === 'battlefield') {
        return { ...prev, battlefield: prev.battlefield.filter((_, i) => i !== index) }
      }
      return { ...prev, [zone]: (prev[zone] as string[]).filter((_, i) => i !== index) }
    })
  }, [])

  const updateBattlefieldCard = useCallback(
    (seat: PlayerKey, index: number, patch: Partial<ScenarioBattlefieldCard>) => {
      seatSetter(seat)((prev) => ({
        ...prev,
        battlefield: prev.battlefield.map((c, i) => (i === index ? { ...c, ...patch } : c)),
      }))
    },
    [],
  )

  // --- actions -------------------------------------------------------------
  const currentSpec = useCallback(
    () => toSpec(p1, p2, extraSeats, { phase, activePlayer, mode }),
    [p1, p2, extraSeats, phase, activePlayer, mode],
  )

  const handleLoadJson = useCallback(() => {
    try {
      const parsed = JSON.parse(jsonText) as ScenarioSpec
      applySpec(parsed)
      setStatus('Loaded scenario from JSON.')
      setErrors([])
    } catch {
      setStatus('Invalid JSON.')
    }
  }, [jsonText, applySpec])

  const handleShowJson = useCallback(() => {
    setJsonText(JSON.stringify(currentSpec(), null, 2))
    setJsonOpen(true)
  }, [currentSpec])

  // Load a file: a downloaded full-state snapshot (has `entities`/`zones`) jumps straight in;
  // a name-based scenario file (like the manual-scenarios JSONs) loads into the editable builder.
  const fileInputRef = useRef<HTMLInputElement>(null)
  const handleLoadFile = useCallback((file: File) => {
    void (async () => {
      const text = await file.text()
      let parsed: unknown
      try {
        parsed = JSON.parse(text)
      } catch {
        setStatus('Invalid JSON file.')
        return
      }
      if (parsed && typeof parsed === 'object' && 'entities' in parsed && 'zones' in parsed) {
        // Full-state snapshot → inject server-side and jump into the exact position.
        setSnapshotLoading(true)
        void jumpInto(
          fetch('/api/scenarios/from-state', {
            method: 'POST',
            headers: { 'Content-Type': 'text/plain' },
            body: text,
          }),
        )
      } else {
        // Name-based scenario → load into the editable builder.
        applySpec(parsed as ScenarioSpec)
        setStatus('Loaded scenario from file.')
        setErrors([])
      }
    })()
  }, [applySpec, jumpInto])

  const handleShare = useCallback(async () => {
    const code = await encodeScenario(currentSpec())
    const url = buildScenarioUrl(window.location.origin, code)
    try {
      await navigator.clipboard.writeText(url)
      setStatus('Share link copied to clipboard.')
    } catch {
      window.prompt('Copy this scenario link', url)
    }
  }, [currentSpec])

  const handleStart = useCallback(async () => {
    setStarting(true)
    setErrors([])
    setStatus('Creating scenario…')
    try {
      const res = await fetch('/api/scenarios', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(currentSpec()),
      })
      if (!res.ok) {
        const body: unknown = await res.json().catch(() => null)
        const msgs =
          body && typeof body === 'object' && Array.isArray((body as { errors?: unknown }).errors)
            ? ((body as { errors: string[] }).errors)
            : [`Request failed (HTTP ${res.status})`]
        setErrors(msgs)
        setStatus(null)
        setStarting(false)
        return
      }
      const data = (await res.json()) as ScenarioCreateResponse
      // Pick the seat this client should connect as: SELF/AI hand off a single human token.
      const human =
        data.player1.token && data.player1.token !== '(AI)' ? data.player1 : data.player2
      // Full navigation so the app makes a clean token-based connect to the new session.
      window.location.href = `/?token=${encodeURIComponent(human.token)}`
    } catch (e: unknown) {
      setErrors([e instanceof Error ? e.message : 'Failed to create scenario'])
      setStatus(null)
      setStarting(false)
    }
  }, [currentSpec])

  // --- render --------------------------------------------------------------
  if (snapshotLoading) {
    return (
      <div style={{ ...S.page, alignItems: 'center', justifyContent: 'center' }}>
        <div style={{ color: '#e2e8f0', fontSize: 16 }}>Loading snapshot…</div>
      </div>
    )
  }
  return (
    <div style={S.page}>
      <div style={S.topbar}>
        <button style={S.linkBtn} onClick={() => navigate('/')}>← Back</button>
        <h1 style={S.title}>Scenario Builder</h1>
        <div style={{ flex: 1 }} />
        <input
          ref={fileInputRef}
          type="file"
          accept="application/json,.json"
          style={{ display: 'none' }}
          onChange={(e) => {
            const file = e.target.files?.[0]
            if (file) handleLoadFile(file)
            e.target.value = ''
          }}
        />
        <ActionButton
          label="Load file"
          hint="open a saved snapshot or scenario"
          onClick={() => fileInputRef.current?.click()}
        />
        <ActionButton
          label="View JSON"
          hint="copy this scenario as text"
          onClick={handleShowJson}
        />
        <ActionButton
          label="Share"
          hint="copy an editable link"
          onClick={() => void handleShare()}
        />
        <ActionButton
          label={starting ? 'Starting…' : 'Start'}
          hint="play this scenario now"
          primary
          disabled={starting}
          onClick={() => void handleStart()}
        />
      </div>

      {(status || errors.length > 0) && (
        <div style={S.statusBar}>
          {errors.length > 0 ? (
            <ul style={S.errorList}>
              {errors.map((e, i) => (
                <li key={i} style={{ color: '#fca5a5' }}>{e}</li>
              ))}
            </ul>
          ) : (
            <span style={{ color: '#a7f3d0' }}>{status}</span>
          )}
        </div>
      )}

      <div style={S.body}>
        {/* Left: card search */}
        <div style={S.searchCol}>
          <div style={S.targetRow}>
            <span style={S.smallLabel}>Add to</span>
            <select value={targetSeat} style={S.select} onChange={(e) => setTargetSeat(e.target.value as PlayerKey)}>
              <option value="player1">{p1.name}</option>
              <option value="player2">{p2.name}</option>
            </select>
            <select value={targetZone} style={S.select} onChange={(e) => setTargetZone(e.target.value as ScenarioZone)}>
              {SCENARIO_ZONES.map((z) => (
                <option key={z} value={z}>{ZONE_LABEL[z]}</option>
              ))}
            </select>
          </div>
          <input
            style={S.search}
            placeholder="Search by name or query (e.g. t:creature c:r)"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
          />
          <span style={S.hint}>
            Click a card below to add it to {targetSeat === 'player1' ? p1.name : p2.name}’s {ZONE_LABEL[targetZone].toLowerCase()}.
          </span>
          {catalogError && <div style={{ color: '#fca5a5', padding: 8 }}>{catalogError}</div>}
          <div style={S.resultList}>
            {results.map((c) => (
              <button
                key={`${c.setCode ?? ''}-${c.collectorNumber ?? c.name}`}
                style={S.resultRow}
                title={`Add to ${ZONE_LABEL[targetZone]}`}
                onClick={() => addCard(targetSeat, targetZone, c.name)}
              >
                <span style={S.resultName}>{c.name}</span>
                <span style={S.resultMeta}>{c.manaCost}</span>
              </button>
            ))}
            {!results.length && catalog.length > 0 && (
              <div style={{ color: '#94a3b8', padding: 8 }}>No matches.</div>
            )}
          </div>
        </div>

        {/* Middle/right: two player panels */}
        <div style={S.playersCol}>
          <PlayerPanel
            seat="player1"
            player={p1}
            onName={(name) => setP1((p) => ({ ...p, name }))}
            onLife={(life) => setP1((p) => ({ ...p, life }))}
            onRemove={(zone, i) => removeFromZone('player1', zone, i)}
            onUpdateBattlefield={(i, patch) => updateBattlefieldCard('player1', i, patch)}
          />
          <PlayerPanel
            seat="player2"
            player={p2}
            onName={(name) => setP2((p) => ({ ...p, name }))}
            onLife={(life) => setP2((p) => ({ ...p, life }))}
            onRemove={(zone, i) => removeFromZone('player2', zone, i)}
            onUpdateBattlefield={(i, patch) => updateBattlefieldCard('player2', i, patch)}
          />
        </div>

        {/* Settings */}
        <div style={S.settingsCol}>
          <div style={S.settingsTitle}>Settings</div>
          <label style={S.field}>
            <span style={S.smallLabel}>Opponent</span>
            <select
              value={extraSeats.length > 0 ? 'SELF' : mode}
              style={S.select}
              disabled={extraSeats.length > 0}
              onChange={(e) => setMode(e.target.value as ScenarioMode)}
            >
              <option value="SELF">Yourself (hotseat)</option>
              <option value="AI">AI</option>
              <option value="TWO_PLAYER">Two players</option>
            </select>
            <span style={S.hint}>
              {extraSeats.length > 0
                ? 'Pods of 3-4 seats always start as hotseat — you control every seat.'
                : MODE_HINT[mode]}
            </span>
          </label>
          <div style={S.field}>
            <span style={S.smallLabel}>Pod seats (3-4 player)</span>
            {extraSeats.map((seat, i) => (
              <div key={i} style={{ display: 'flex', gap: 6, alignItems: 'center' }}>
                <input
                  style={{ ...S.select, flex: 1 }}
                  value={seat.name ?? ''}
                  placeholder={`Player ${i + 3}`}
                  onChange={(e) =>
                    setExtraSeats((prev) =>
                      prev.map((s, j) => (j === i ? { ...s, name: e.target.value } : s)),
                    )
                  }
                />
                <button
                  style={S.ghostBtn}
                  title="Remove this seat"
                  onClick={() => setExtraSeats((prev) => prev.filter((_, j) => j !== i))}
                >
                  ✕
                </button>
              </div>
            ))}
            {extraSeats.length < 2 && (
              <button
                style={S.ghostBtn}
                onClick={() =>
                  setExtraSeats((prev) => [...prev, { name: `Player ${prev.length + 3}` }])
                }
              >
                + Add seat
              </button>
            )}
            <span style={S.hint}>
              Extra seats start with an empty board (20 life) — give them cards via the JSON editor.
            </span>
          </div>
          <label style={S.field}>
            <span style={S.smallLabel}>Phase</span>
            <select value={phase} style={S.select} onChange={(e) => setPhase(e.target.value)}>
              {PHASES.map((ph) => (
                <option key={ph} value={ph}>{ph}</option>
              ))}
            </select>
          </label>
          <label style={S.field}>
            <span style={S.smallLabel}>Active player</span>
            <select value={activePlayer} style={S.select} onChange={(e) => setActivePlayer(Number(e.target.value))}>
              <option value={1}>{p1.name}</option>
              <option value={2}>{p2.name}</option>
            </select>
          </label>

          <button style={S.ghostBtn} onClick={() => setJsonOpen((o) => !o)}>
            {jsonOpen ? 'Hide JSON editor' : 'Edit as JSON'}
          </button>
          {jsonOpen && (
            <div style={{ marginTop: 8 }}>
              <span style={S.hint}>
                Paste a scenario JSON and press “Apply JSON”, or copy the text below to save it.
              </span>
              <textarea
                style={S.json}
                value={jsonText}
                placeholder="Paste a scenario JSON here…"
                onChange={(e) => setJsonText(e.target.value)}
              />
              <button style={S.ghostBtn} onClick={handleLoadJson}>Apply JSON to builder</button>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

// --- action button (label + visible one-line description) ----------------------------------

function ActionButton(props: {
  label: string
  hint: string
  onClick: () => void
  primary?: boolean
  disabled?: boolean
}) {
  return (
    <button
      style={{ ...(props.primary ? S.actionBtnPrimary : S.actionBtn), opacity: props.disabled ? 0.5 : 1 }}
      disabled={props.disabled}
      onClick={props.onClick}
    >
      <span style={S.actionLabel}>{props.label}</span>
      <span style={props.primary ? S.actionHintPrimary : S.actionHint}>{props.hint}</span>
    </button>
  )
}

// --- player panel --------------------------------------------------------------------------

function PlayerPanel(props: {
  seat: PlayerKey
  player: BuilderPlayer
  onName: (name: string) => void
  onLife: (life: number) => void
  onRemove: (zone: ScenarioZone, index: number) => void
  onUpdateBattlefield: (index: number, patch: Partial<ScenarioBattlefieldCard>) => void
}) {
  const { player } = props
  return (
    <div style={S.panel}>
      <div style={S.panelHead}>
        <input style={S.nameInput} value={player.name} onChange={(e) => props.onName(e.target.value)} />
        <label style={S.lifeWrap}>
          <span style={S.smallLabel}>Life</span>
          <input
            type="number"
            style={S.lifeInput}
            value={player.life}
            onChange={(e) => props.onLife(Number(e.target.value))}
          />
        </label>
      </div>

      {SCENARIO_ZONES.map((zone) => {
        const isBf = zone === 'battlefield'
        const items = isBf ? player.battlefield : (player[zone] as string[])
        return (
          <div key={zone} style={S.zoneBlock}>
            <div style={S.zoneHead}>{ZONE_LABEL[zone]} <span style={S.count}>{items.length}</span></div>
            {items.length === 0 && <div style={S.empty}>—</div>}
            {isBf
              ? player.battlefield.map((card, i) => (
                  <div key={i} style={S.cardRow}>
                    <span style={S.cardName}>{card.name}</span>
                    <label style={S.toggle} title="Tapped">
                      <input
                        type="checkbox"
                        checked={!!card.tapped}
                        onChange={(e) => props.onUpdateBattlefield(i, { tapped: e.target.checked })}
                      />T
                    </label>
                    <label style={S.toggle} title="Summoning sickness">
                      <input
                        type="checkbox"
                        checked={!!card.summoningSickness}
                        onChange={(e) => props.onUpdateBattlefield(i, { summoningSickness: e.target.checked })}
                      />SS
                    </label>
                    <input
                      type="number"
                      style={S.counterInput}
                      title="+1/+1 counters"
                      value={card.counters?.PLUS_ONE_PLUS_ONE ?? 0}
                      min={0}
                      onChange={(e) => {
                        const n = Number(e.target.value)
                        const counters = { ...(card.counters ?? {}) }
                        if (n > 0) counters.PLUS_ONE_PLUS_ONE = n
                        else delete counters.PLUS_ONE_PLUS_ONE
                        props.onUpdateBattlefield(i, { counters })
                      }}
                    />
                    <button style={S.removeBtn} onClick={() => props.onRemove(zone, i)}>✕</button>
                  </div>
                ))
              : (items as string[]).map((name, i) => (
                  <div key={i} style={S.cardRow}>
                    <span style={S.cardName}>{name}</span>
                    <button style={S.removeBtn} onClick={() => props.onRemove(zone, i)}>✕</button>
                  </div>
                ))}
          </div>
        )
      })}
    </div>
  )
}

// --- styles --------------------------------------------------------------------------------

const S: Record<string, CSSProperties> = {
  page: { position: 'fixed', inset: 0, background: '#0f172a', color: '#e2e8f0', display: 'flex', flexDirection: 'column', fontFamily: 'system-ui, sans-serif' },
  topbar: { display: 'flex', alignItems: 'center', gap: 10, padding: '10px 16px', borderBottom: '1px solid #1e293b', background: '#111827' },
  title: { fontSize: 16, fontWeight: 700, margin: 0 },
  linkBtn: { background: 'transparent', color: '#93c5fd', border: 'none', cursor: 'pointer', fontSize: 14 },
  ghostBtn: { background: '#1e293b', color: '#e2e8f0', border: '1px solid #334155', borderRadius: 6, padding: '6px 12px', cursor: 'pointer', fontSize: 13 },
  primaryBtn: { background: '#7c3aed', color: 'white', border: 'none', borderRadius: 6, padding: '6px 16px', cursor: 'pointer', fontSize: 13, fontWeight: 600 },
  actionBtn: { display: 'flex', flexDirection: 'column', alignItems: 'flex-start', gap: 1, background: '#1e293b', color: '#e2e8f0', border: '1px solid #334155', borderRadius: 6, padding: '5px 12px', cursor: 'pointer', lineHeight: 1.2 },
  actionBtnPrimary: { display: 'flex', flexDirection: 'column', alignItems: 'flex-start', gap: 1, background: '#7c3aed', color: 'white', border: 'none', borderRadius: 6, padding: '5px 14px', cursor: 'pointer', lineHeight: 1.2 },
  actionLabel: { fontSize: 13, fontWeight: 600 },
  actionHint: { fontSize: 10, color: '#94a3b8', fontWeight: 400 },
  actionHintPrimary: { fontSize: 10, color: '#e9d5ff', fontWeight: 400 },
  hint: { fontSize: 11, color: '#94a3b8', lineHeight: 1.35 },
  statusBar: { padding: '6px 16px', borderBottom: '1px solid #1e293b', fontSize: 13 },
  errorList: { margin: 0, paddingLeft: 18 },
  body: { flex: 1, display: 'flex', minHeight: 0 },
  searchCol: { width: 300, borderRight: '1px solid #1e293b', display: 'flex', flexDirection: 'column', padding: 12, gap: 8 },
  targetRow: { display: 'flex', alignItems: 'center', gap: 6 },
  smallLabel: { fontSize: 11, color: '#94a3b8', textTransform: 'uppercase', letterSpacing: '0.04em' },
  select: { background: '#1e293b', color: '#e2e8f0', border: '1px solid #334155', borderRadius: 6, padding: '4px 6px', fontSize: 13 },
  search: { background: '#1e293b', color: '#e2e8f0', border: '1px solid #334155', borderRadius: 6, padding: '8px 10px', fontSize: 13 },
  resultList: { flex: 1, overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: 2 },
  resultRow: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', background: '#1e293b', border: '1px solid #263449', borderRadius: 5, padding: '6px 8px', cursor: 'pointer', textAlign: 'left' },
  resultName: { color: '#e2e8f0', fontSize: 13 },
  resultMeta: { color: '#64748b', fontSize: 11 },
  playersCol: { flex: 1, display: 'flex', gap: 12, padding: 12, overflowY: 'auto' },
  panel: { flex: 1, background: '#111827', border: '1px solid #1e293b', borderRadius: 8, padding: 10, display: 'flex', flexDirection: 'column', gap: 8, minWidth: 0 },
  panelHead: { display: 'flex', alignItems: 'center', gap: 8 },
  nameInput: { flex: 1, background: '#1e293b', color: '#e2e8f0', border: '1px solid #334155', borderRadius: 6, padding: '6px 8px', fontSize: 14, fontWeight: 600 },
  lifeWrap: { display: 'flex', alignItems: 'center', gap: 4 },
  lifeInput: { width: 56, background: '#1e293b', color: '#e2e8f0', border: '1px solid #334155', borderRadius: 6, padding: '6px 8px', fontSize: 13 },
  zoneBlock: { background: '#0b1220', border: '1px solid #1e293b', borderRadius: 6, padding: 8 },
  zoneHead: { fontSize: 12, fontWeight: 600, color: '#cbd5e1', marginBottom: 4 },
  count: { color: '#64748b', fontWeight: 400 },
  empty: { color: '#475569', fontSize: 12 },
  cardRow: { display: 'flex', alignItems: 'center', gap: 6, padding: '2px 0' },
  cardName: { flex: 1, fontSize: 12, color: '#e2e8f0', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' },
  toggle: { display: 'inline-flex', alignItems: 'center', gap: 2, fontSize: 10, color: '#94a3b8' },
  counterInput: { width: 40, background: '#1e293b', color: '#e2e8f0', border: '1px solid #334155', borderRadius: 4, padding: '2px 4px', fontSize: 11 },
  removeBtn: { background: 'transparent', color: '#f87171', border: 'none', cursor: 'pointer', fontSize: 12 },
  settingsCol: { width: 240, borderLeft: '1px solid #1e293b', padding: 12, display: 'flex', flexDirection: 'column', gap: 10 },
  settingsTitle: { fontSize: 13, fontWeight: 700, color: '#cbd5e1' },
  field: { display: 'flex', flexDirection: 'column', gap: 4 },
  json: { width: '100%', height: 220, background: '#0b1220', color: '#e2e8f0', border: '1px solid #334155', borderRadius: 6, padding: 8, fontFamily: 'monospace', fontSize: 11, marginBottom: 6 },
}

export default ScenarioBuilderPage
