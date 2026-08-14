/**
 * Scenario Builder / Tester.
 *
 * Two panes, resizable: the deckbuilder's card browser on the left (full Scryfall-style query
 * language, filter chips, set picker, image grid, hover previews) and an editable board on the
 * right. Cards are dragged from the browser into any seat's battlefield / hand / graveyard /
 * exile / library / command zone, or clicked to drop into the currently targeted zone; cards
 * already on the board drag between zones. Battlefield permanents open an editor for tapped
 * state, counters, attachments, and pre-set ETB choices.
 *
 * Then play it against yourself (hotseat), the AI, or as two players. Scenarios round-trip
 * through an encoded `?s=` share URL, a JSON drawer, and `.json` files; a `?replay=&frame=`
 * link jumps straight into a stored replay position.
 */
import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { CardBrowser, useCardCatalog, type CardDragPayload } from '@/components/deckbuilder/browser'
import type { CardSummary } from '@/components/deckbuilder/cardFilter'
import { CardEditorModal } from './CardEditorModal'
import { ScenarioBoard, type BoardTarget } from './ScenarioBoard'
import {
  MODE_HINT,
  PHASES,
  addCardToZone,
  clearSeat,
  emptyBuilderState,
  emptySeat,
  fromSpec,
  moveCard,
  placedCounts,
  removeCardAt,
  toSpec,
  updateBattlefieldCard,
  type BuilderSeat,
  type BuilderState,
} from './builderState'
import { useUndoable } from './builderHistory'
import {
  SCENARIO_FRAME_PARAM,
  SCENARIO_REPLAY_PARAM,
  SCENARIO_SHARE_PARAM,
  buildScenarioUrl,
  decodeScenario,
  encodeScenario,
} from './shareScenario'
import {
  ZONE_LABEL,
  type ScenarioCreateResponse,
  type ScenarioMode,
  type ScenarioSpec,
  type ScenarioZone,
} from './types'
import styles from './ScenarioBuilder.module.css'

const SPLIT_STORAGE_KEY = 'scenarioBuilder.browserWidth'
const DEFAULT_SPLIT = 520
const MIN_BROWSER = 320
const MIN_BOARD = 380

/** Copies added per click in the browser — filling a library one card at a time is tedious. */
const COPY_OPTIONS = [1, 2, 3, 4, 10, 20, 40]

export function ScenarioBuilderPage() {
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()
  const { catalog, setInfos, index, loading, error: catalogError } = useCardCatalog()

  const { state, commit, reset, undo, redo, canUndo, canRedo } = useUndoable<BuilderState>(
    emptyBuilderState,
  )

  const [target, setTarget] = useState<BoardTarget>(() => ({
    seatId: '',
    zone: 'battlefield',
  }))
  const [copies, setCopies] = useState(1)
  const [editing, setEditing] = useState<{ seatId: string; index: number } | null>(null)

  const [jsonText, setJsonText] = useState('')
  const [jsonOpen, setJsonOpen] = useState(false)
  const [status, setStatus] = useState<string | null>(null)
  const [errors, setErrors] = useState<string[]>([])
  const [starting, setStarting] = useState(false)
  const [snapshotLoading, setSnapshotLoading] = useState(false)

  // Keep the click-target pointed at a seat that exists (seats are added/removed, and loading a
  // scenario mints fresh seat ids).
  useEffect(() => {
    if (state.seats.some((s) => s.id === target.seatId)) return
    const first = state.seats[0]
    if (first) setTarget({ seatId: first.id, zone: target.zone })
  }, [state.seats, target.seatId, target.zone])

  const targetSeat = state.seats.find((s) => s.id === target.seatId) ?? state.seats[0]

  // --- board mutations -----------------------------------------------------

  const addCard = useCallback(
    (name: string, seatId: string, zone: ScenarioZone, count = 1) => {
      commit((prev) => addCardToZone(prev, seatId, zone, name, count))
    },
    [commit],
  )

  const handleBrowserAdd = useCallback(
    (card: CardSummary) => {
      if (!targetSeat) return
      addCard(card.name, targetSeat.id, target.zone, copies)
      setStatus(
        `${copies > 1 ? `${copies}× ` : ''}${card.name} → ${targetSeat.name}’s ${ZONE_LABEL[target.zone].toLowerCase()}.`,
      )
    },
    [addCard, copies, target.zone, targetSeat],
  )

  /** Shift/right-click in the browser grid removes the most recent copy of that card. */
  const handleBrowserRemove = useCallback(
    (name: string) => {
      commit((prev) => {
        for (const seat of prev.seats) {
          for (const zone of ['battlefield', 'hand', 'graveyard', 'exile', 'library', 'commanders'] as ScenarioZone[]) {
            const names =
              zone === 'battlefield' ? seat.battlefield.map((c) => c.name) : seat[zone]
            const idx = names.lastIndexOf(name)
            if (idx >= 0) return removeCardAt(prev, seat.id, zone, idx)
          }
        }
        return prev
      })
    },
    [commit],
  )

  const handleDropCard = useCallback(
    (payload: CardDragPayload, seatId: string, zone: ScenarioZone) => {
      if (payload.source === 'zone' && payload.zoneRef) {
        const [fromSeatId, fromZone, rawIndex] = payload.zoneRef.split('|')
        const fromIndex = Number(rawIndex)
        if (fromSeatId && fromZone && Number.isInteger(fromIndex)) {
          if (fromSeatId === seatId && fromZone === zone) return
          commit((prev) =>
            moveCard(
              prev,
              { seatId: fromSeatId, zone: fromZone as ScenarioZone, index: fromIndex },
              { seatId, zone },
            ),
          )
          return
        }
      }
      addCard(payload.name, seatId, zone)
    },
    [addCard, commit],
  )

  const handleRemove = useCallback(
    (seatId: string, zone: ScenarioZone, index: number) => {
      commit((prev) => removeCardAt(prev, seatId, zone, index))
    },
    [commit],
  )

  const handleQuickPatch = useCallback(
    (seatId: string, index: number, patch: Parameters<typeof updateBattlefieldCard>[3]) => {
      commit((prev) => updateBattlefieldCard(prev, seatId, index, patch))
    },
    [commit],
  )

  const handleSeatPatch = useCallback(
    (seatId: string, patch: Partial<Pick<BuilderSeat, 'name' | 'life'>>) => {
      commit((prev) => ({
        ...prev,
        seats: prev.seats.map((s) => (s.id === seatId ? { ...s, ...patch } : s)),
      }))
    },
    [commit],
  )

  const handleClearZone = useCallback(
    (seatId: string, zone: ScenarioZone) => {
      commit((prev) => ({
        ...prev,
        seats: prev.seats.map((s) =>
          s.id === seatId ? { ...s, [zone]: [] as never } : s,
        ),
      }))
    },
    [commit],
  )

  const handleClearSeat = useCallback(
    (seatId: string) => commit((prev) => clearSeat(prev, seatId)),
    [commit],
  )

  const handleRemoveSeat = useCallback(
    (seatId: string) => {
      commit((prev) => {
        if (prev.seats.length <= 2) return prev
        const seats = prev.seats.filter((s) => s.id !== seatId)
        return {
          ...prev,
          seats,
          activePlayer: Math.min(prev.activePlayer, seats.length),
          mode: seats.length > 2 ? 'SELF' : prev.mode,
        }
      })
    },
    [commit],
  )

  const handleAddSeat = useCallback(() => {
    commit((prev) => ({
      ...prev,
      seats: [...prev.seats, emptySeat(`Player ${prev.seats.length + 1}`)],
      // Pods of 3-4 seats are hotseat-only (the server restricts AI / two-player to duels).
      mode: 'SELF',
    }))
  }, [commit])

  // --- undo/redo shortcuts -------------------------------------------------

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (!(e.metaKey || e.ctrlKey) || e.key.toLowerCase() !== 'z') return
      const el = e.target as HTMLElement | null
      const tag = el?.tagName
      if (tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT' || el?.isContentEditable) return
      e.preventDefault()
      if (e.shiftKey) redo()
      else undo()
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [undo, redo])

  // --- split pane ----------------------------------------------------------

  const [browserWidth, setBrowserWidth] = useState(() => {
    const stored = typeof window !== 'undefined' ? window.localStorage.getItem(SPLIT_STORAGE_KEY) : null
    const parsed = stored ? Number(stored) : NaN
    return Number.isFinite(parsed) ? parsed : DEFAULT_SPLIT
  })
  const [dragging, setDragging] = useState(false)
  useEffect(() => {
    if (!dragging) return
    const onMove = (e: MouseEvent) => {
      const max = Math.max(MIN_BROWSER, window.innerWidth - MIN_BOARD)
      setBrowserWidth(Math.min(max, Math.max(MIN_BROWSER, e.clientX)))
    }
    const onUp = () => setDragging(false)
    window.addEventListener('mousemove', onMove)
    window.addEventListener('mouseup', onUp)
    // Kill text selection while dragging the divider.
    document.body.style.userSelect = 'none'
    return () => {
      window.removeEventListener('mousemove', onMove)
      window.removeEventListener('mouseup', onUp)
      document.body.style.userSelect = ''
    }
  }, [dragging])
  useEffect(() => {
    window.localStorage.setItem(SPLIT_STORAGE_KEY, String(browserWidth))
  }, [browserWidth])

  // --- exact-snapshot jump-in on load (?replay=<id>&frame=<n>) -------------
  // A snapshot link references a stored replay frame (kept short); inject it server-side and
  // jump straight into the exact position (it isn't editable in the builder).

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
  }, [searchParams, jumpInto])

  // --- decode shared scenario on load (?s=<code>) --------------------------
  const applySpec = useCallback(
    (spec: ScenarioSpec) => {
      const next = fromSpec(spec)
      reset(next)
      const first = next.seats[0]
      if (first) setTarget({ seatId: first.id, zone: 'battlefield' })
    },
    [reset],
  )

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
  }, [searchParams, setSearchParams, applySpec])

  // --- actions -------------------------------------------------------------

  const currentSpec = useCallback(() => toSpec(state), [state])

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
  const handleLoadFile = useCallback(
    (file: File) => {
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
    },
    [applySpec, jumpInto],
  )

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

  // --- derived -------------------------------------------------------------

  const counts = useMemo(() => placedCounts(state), [state])

  const creatureTypes = useMemo(() => {
    const out = new Set<string>()
    for (const c of catalog) {
      if (c.cardTypes.includes('CREATURE')) for (const s of c.subtypes) out.add(s)
    }
    return [...out].sort()
  }, [catalog])

  const editingSeat = editing ? state.seats.find((s) => s.id === editing.seatId) : undefined
  const editingCard = editing && editingSeat ? editingSeat.battlefield[editing.index] : undefined

  // --- render --------------------------------------------------------------

  if (snapshotLoading) {
    return (
      <div className={styles.page}>
        <div className={styles.loading}>Loading snapshot…</div>
      </div>
    )
  }

  return (
    <div className={styles.page}>
      <div className={styles.topbar}>
        <button className={styles.linkBtn} onClick={() => navigate('/')}>← Back</button>
        <h1 className={styles.title}>Scenario Builder</h1>
        <button
          className={styles.iconBtn}
          onClick={undo}
          disabled={!canUndo}
          title="Undo (⌘Z)"
          aria-label="Undo"
        >
          ↶
        </button>
        <button
          className={styles.iconBtn}
          onClick={redo}
          disabled={!canRedo}
          title="Redo (⇧⌘Z)"
          aria-label="Redo"
        >
          ↷
        </button>
        <div className={styles.spacer} />
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
        <ActionButton label="View JSON" hint="copy this scenario as text" onClick={handleShowJson} />
        <ActionButton label="Share" hint="copy an editable link" onClick={() => void handleShare()} />
        <ActionButton
          label={starting ? 'Starting…' : 'Start'}
          hint="play this scenario now"
          primary
          disabled={starting}
          onClick={() => void handleStart()}
        />
      </div>

      {(status || errors.length > 0) && (
        <div className={styles.statusBar}>
          {errors.length > 0 ? (
            <ul className={styles.errorList}>
              {errors.map((e, i) => (
                <li key={i}>{e}</li>
              ))}
            </ul>
          ) : (
            <span className={styles.statusOk}>{status}</span>
          )}
        </div>
      )}

      <div className={styles.body}>
        <div className={styles.browserPane} style={{ width: browserWidth, flex: `0 0 ${browserWidth}px` }}>
          <CardBrowser
            catalog={catalog}
            setInfos={setInfos}
            loading={loading}
            error={catalogError}
            counts={counts}
            onAdd={handleBrowserAdd}
            onRemove={handleBrowserRemove}
            actionHint={
              <>
                <kbd>Click</kbd> add · <kbd>Drag</kbd> to any zone
              </>
            }
            header={
              <div className={styles.targetRow}>
                <span className={styles.smallLabel}>Add to</span>
                <select
                  className={styles.select}
                  value={target.seatId}
                  onChange={(e) => setTarget({ ...target, seatId: e.target.value })}
                  aria-label="Target seat"
                >
                  {state.seats.map((s) => (
                    <option key={s.id} value={s.id}>
                      {s.name}
                    </option>
                  ))}
                </select>
                <select
                  className={styles.select}
                  value={target.zone}
                  onChange={(e) => setTarget({ ...target, zone: e.target.value as ScenarioZone })}
                  aria-label="Target zone"
                >
                  {(Object.keys(ZONE_LABEL) as ScenarioZone[]).map((z) => (
                    <option key={z} value={z}>
                      {ZONE_LABEL[z]}
                    </option>
                  ))}
                </select>
                <select
                  className={styles.select}
                  value={copies}
                  onChange={(e) => setCopies(Number(e.target.value))}
                  aria-label="Copies per click"
                >
                  {COPY_OPTIONS.map((n) => (
                    <option key={n} value={n}>
                      ×{n}
                    </option>
                  ))}
                </select>
              </div>
            }
          />
        </div>

        <div
          className={dragging ? styles.splitterActive : styles.splitter}
          onMouseDown={() => setDragging(true)}
          role="separator"
          aria-orientation="vertical"
          aria-label="Resize the card browser"
        />

        <div className={styles.boardPane}>
          <div className={styles.boardBar}>
            <label className={styles.field}>
              <span className={styles.smallLabel}>Opponent</span>
              <select
                className={styles.select}
                value={state.mode}
                disabled={state.seats.length > 2}
                onChange={(e) => commit((prev) => ({ ...prev, mode: e.target.value as ScenarioMode }))}
              >
                <option value="SELF">Yourself (hotseat)</option>
                <option value="AI">AI</option>
                <option value="TWO_PLAYER">Two players</option>
              </select>
            </label>
            <label className={styles.field}>
              <span className={styles.smallLabel}>Phase</span>
              <select
                className={styles.select}
                value={state.phase}
                onChange={(e) => commit((prev) => ({ ...prev, phase: e.target.value }))}
              >
                {PHASES.map((ph) => (
                  <option key={ph} value={ph}>
                    {ph.replace(/_/g, ' ').toLowerCase()}
                  </option>
                ))}
              </select>
            </label>
            <label className={styles.field}>
              <span className={styles.smallLabel}>Active</span>
              <select
                className={styles.select}
                value={state.activePlayer}
                onChange={(e) => commit((prev) => ({ ...prev, activePlayer: Number(e.target.value) }))}
              >
                {state.seats.map((s, i) => (
                  <option key={s.id} value={i + 1}>
                    {s.name}
                  </option>
                ))}
              </select>
            </label>
            <button
              type="button"
              className={styles.ghostBtn}
              onClick={handleAddSeat}
              disabled={state.seats.length >= 4}
              title="Add a seat (3-4 player pods are hotseat-only)"
            >
              + Seat
            </button>
            <div className={styles.spacer} />
            <button type="button" className={styles.ghostBtn} onClick={() => setJsonOpen((o) => !o)}>
              {jsonOpen ? 'Hide JSON' : 'Edit as JSON'}
            </button>
            <button
              type="button"
              className={styles.ghostBtn}
              onClick={() => {
                reset(emptyBuilderState())
                setStatus('Board cleared.')
              }}
            >
              Reset
            </button>
            <span className={styles.modeHint}>
              {state.seats.length > 2
                ? 'Pods of 3-4 seats always start as hotseat — you control every seat.'
                : MODE_HINT[state.mode]}
            </span>
          </div>

          <ScenarioBoard
            state={state}
            index={index}
            target={target}
            onTargetChange={setTarget}
            onSeatPatch={handleSeatPatch}
            onDropCard={handleDropCard}
            onRemove={handleRemove}
            onEditCard={(seatId, i) => setEditing({ seatId, index: i })}
            onQuickPatch={handleQuickPatch}
            onClearZone={handleClearZone}
            onClearSeat={handleClearSeat}
            onRemoveSeat={handleRemoveSeat}
          />

          {jsonOpen && (
            <div className={styles.jsonDrawer}>
              <span className={styles.hint}>
                Paste a scenario JSON and press “Apply”, or copy the text below to save it.
              </span>
              <textarea
                className={styles.jsonArea}
                value={jsonText}
                placeholder="Paste a scenario JSON here…"
                onChange={(e) => setJsonText(e.target.value)}
              />
              <div className={styles.jsonActions}>
                <button type="button" className={styles.ghostBtn} onClick={handleLoadJson}>
                  Apply to builder
                </button>
                <button type="button" className={styles.ghostBtn} onClick={handleShowJson}>
                  Refresh from board
                </button>
              </div>
            </div>
          )}
        </div>
      </div>

      {editing && editingCard && editingSeat && (
        <CardEditorModal
          card={editingCard}
          summary={index[editingCard.name]}
          hostOptions={editingSeat.battlefield
            .filter((_, i) => i !== editing.index)
            .map((c) => c.name)}
          creatureTypes={creatureTypes}
          onChange={(patch) => handleQuickPatch(editing.seatId, editing.index, patch)}
          onRemove={() => handleRemove(editing.seatId, 'battlefield', editing.index)}
          onClose={() => setEditing(null)}
        />
      )}
    </div>
  )
}

function ActionButton(props: {
  label: string
  hint: string
  onClick: () => void
  primary?: boolean
  disabled?: boolean
}) {
  return (
    <button
      className={props.primary ? styles.actionBtnPrimary : styles.actionBtn}
      disabled={props.disabled}
      onClick={props.onClick}
      type="button"
    >
      <span className={styles.actionLabel}>{props.label}</span>
      <span className={styles.actionHint}>{props.hint}</span>
    </button>
  )
}

export default ScenarioBuilderPage
