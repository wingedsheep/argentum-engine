/**
 * DeckPicker — tabbed deck selector for the quick-game flow.
 *
 * Tabs:
 *   - My decks: localStorage-backed library (load / delete)
 *   - Examples: server-supplied starter lists
 *   - Paste:    free-form deck list parser ("4 Lightning Bolt" / "Lightning Bolt x4")
 *   - Random:   defer to the server (empty deck list → random sealed pool)
 *
 * Emits the current deck list to the parent via `onDeckChange`. When the picker
 * is in "Random" mode it emits `{}`, which the existing server endpoints already
 * treat as "generate a random deck for me".
 *
 * The picker also surfaces server-side validation (≥ 60 cards, 4-of rule, unknown
 * card resolution) and quick stats (color distribution, mana curve, type counts).
 *
 * Data dependencies:
 *   GET  /api/decks/cards     — slim metadata for every card (validation + stats)
 *   GET  /api/decks/examples  — the starter decks shown in the Examples tab
 *   POST /api/decks/validate  — authoritative validation pass when a list is non-empty
 */
import { useEffect, useMemo, useRef, useState } from 'react'
import { useDeckLibrary, type SavedDeck } from '@/store/deckLibrary'
import styles from './DeckPicker.module.css'

type Tab = 'saved' | 'examples' | 'paste' | 'random'

export interface DeckPickerProps {
  onDeckChange: (deckList: Record<string, number>) => void
  onValidityChange?: (isValid: boolean) => void
  /**
   * Optional set selection callback for the "Random" tab. When the picker is on Random and
   * the user changes the set, this is fired with the chosen set code (or null = "any set").
   * Only meaningful for the Quick Game lobby; standalone uses can ignore it.
   */
  onSetCodeChange?: (setCode: string | null) => void
  /** Initial set code for the Random tab — used to re-hydrate after a reconnect. */
  initialSetCode?: string | null
  /** Available sets for the Random tab dropdown. Empty list hides the dropdown. */
  availableSets?: ReadonlyArray<{ code: string; name: string }>
  disabled?: boolean
}

interface CardSummary {
  name: string
  manaCost: string
  cmc: number
  colors: string[]
  cardTypes: string[]
  supertypes: string[]
  subtypes: string[]
  basicLand: boolean
  rarity: string
  setCode: string | null
  collectorNumber: string | null
}

interface ExampleDeck {
  id: string
  name: string
  description: string
  cards: Record<string, number>
}

interface ValidationIssue {
  code: string
  message: string
  cardName: string | null
}

interface ValidationResult {
  valid: boolean
  totalCards: number
  errors: ValidationIssue[]
  warnings: ValidationIssue[]
}

const COLOR_DOT: Record<string, string> = {
  WHITE: '#f5f3da',
  BLUE: '#62a8ff',
  BLACK: '#3a3a3a',
  RED: '#ff6a4a',
  GREEN: '#4ab86a',
}

function parseDeckText(text: string): Record<string, number> {
  const result: Record<string, number> = {}
  for (const rawLine of text.split('\n')) {
    const line = rawLine.trim()
    if (!line) continue
    const leading = line.match(/^(\d+)\s+(.+)$/)
    const trailing = line.match(/^(.+?)\s*x(\d+)$/i)
    let name: string
    let count: number
    if (leading) {
      count = parseInt(leading[1]!, 10)
      name = leading[2]!.trim()
    } else if (trailing) {
      name = trailing[1]!.trim()
      count = parseInt(trailing[2]!, 10)
    } else {
      name = line
      count = 1
    }
    if (name && Number.isFinite(count) && count > 0) {
      result[name] = (result[name] ?? 0) + count
    }
  }
  return result
}

function formatDeckText(cards: Record<string, number>): string {
  return Object.entries(cards)
    .filter(([, n]) => n > 0)
    .map(([name, n]) => `${n} ${name}`)
    .join('\n')
}

export function DeckPicker({
  onDeckChange,
  onValidityChange,
  onSetCodeChange,
  initialSetCode = null,
  availableSets = [],
  disabled = false,
}: DeckPickerProps) {
  const decks = useDeckLibrary((s) => s.decks)
  const hydrate = useDeckLibrary((s) => s.hydrate)
  const saveDeck = useDeckLibrary((s) => s.saveDeck)
  const deleteDeck = useDeckLibrary((s) => s.deleteDeck)

  const [tab, setTab] = useState<Tab>(() => (decks.length > 0 ? 'saved' : 'random'))
  const [pasteText, setPasteText] = useState('')
  const [selectedSavedId, setSelectedSavedId] = useState<string | null>(null)
  const [pendingName, setPendingName] = useState('')
  const [cards, setCards] = useState<Record<string, CardSummary>>({})
  const [examples, setExamples] = useState<ExampleDeck[]>([])
  const [validation, setValidation] = useState<ValidationResult | null>(null)
  const [randomSetCode, setRandomSetCode] = useState<string | null>(initialSetCode)
  const validateAbortRef = useRef<AbortController | null>(null)

  // Re-hydrate on initial-set-code change (e.g. server-driven on reconnect).
  useEffect(() => {
    setRandomSetCode(initialSetCode)
  }, [initialSetCode])

  // Hydrate localStorage once.
  useEffect(() => {
    hydrate()
  }, [hydrate])

  // Move off `random` to `saved` once decks are hydrated, so users land on their own list.
  useEffect(() => {
    if (decks.length > 0 && tab === 'random') {
      setTab('saved')
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [decks.length])

  // Fetch card metadata + examples once.
  useEffect(() => {
    let cancelled = false
    fetch('/api/decks/cards')
      .then((r) => (r.ok ? r.json() : []))
      .then((list: CardSummary[]) => {
        if (cancelled) return
        const byName: Record<string, CardSummary> = {}
        for (const c of list) byName[c.name] = c
        setCards(byName)
      })
      .catch(() => {})
    fetch('/api/decks/examples')
      .then((r) => (r.ok ? r.json() : []))
      .then((list: ExampleDeck[]) => {
        if (!cancelled) setExamples(list)
      })
      .catch(() => {})
    return () => {
      cancelled = true
    }
  }, [])

  // The deck list emitted to the parent based on the active tab.
  const currentDeck: Record<string, number> = useMemo(() => {
    switch (tab) {
      case 'random':
        return {}
      case 'paste':
        return parseDeckText(pasteText)
      case 'saved': {
        const saved = decks.find((d) => d.id === selectedSavedId)
        return saved?.cards ?? {}
      }
      case 'examples':
        // Examples become a deck via the picker's Paste preview as soon as the user clicks one.
        // Selecting an example loads its text into the paste tab; while still on the Examples tab
        // we treat it as "no deck chosen yet".
        return {}
    }
  }, [tab, pasteText, decks, selectedSavedId])

  // Push the current deck up.
  useEffect(() => {
    onDeckChange(currentDeck)
  }, [currentDeck, onDeckChange])

  // Server-side validation when the deck is non-empty.
  useEffect(() => {
    if (Object.keys(currentDeck).length === 0) {
      setValidation(null)
      onValidityChange?.(true) // Random / unset is "valid" — server will fill in.
      return
    }
    validateAbortRef.current?.abort()
    const ctrl = new AbortController()
    validateAbortRef.current = ctrl
    fetch('/api/decks/validate', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ deckList: currentDeck }),
      signal: ctrl.signal,
    })
      .then((r) => (r.ok ? r.json() : null))
      .then((result: ValidationResult | null) => {
        if (ctrl.signal.aborted) return
        setValidation(result)
        onValidityChange?.(result?.valid ?? false)
      })
      .catch(() => {
        if (!ctrl.signal.aborted) {
          // Network failure — fall back to permissive so we don't block play.
          setValidation(null)
          onValidityChange?.(true)
        }
      })
    return () => {
      ctrl.abort()
    }
  }, [currentDeck, onValidityChange])

  const stats = useMemo(() => computeStats(currentDeck, cards), [currentDeck, cards])
  const totalCards = Object.values(currentDeck).reduce((a, b) => a + b, 0)

  const handleLoadExample = (ex: ExampleDeck) => {
    setPasteText(formatDeckText(ex.cards))
    setPendingName(ex.name)
    setTab('paste')
  }

  const handleSaveCurrent = () => {
    if (!pendingName.trim() || Object.keys(currentDeck).length === 0) return
    const saved = saveDeck({ name: pendingName.trim(), cards: currentDeck })
    setSelectedSavedId(saved.id)
    setPendingName('')
    setTab('saved')
  }

  return (
    <div className={styles.picker}>
      <div className={styles.tabs}>
        <TabButton label={`My Decks${decks.length ? ` (${decks.length})` : ''}`} active={tab === 'saved'} onClick={() => setTab('saved')} disabled={disabled} />
        <TabButton label="Examples" active={tab === 'examples'} onClick={() => setTab('examples')} disabled={disabled} />
        <TabButton label="Paste" active={tab === 'paste'} onClick={() => setTab('paste')} disabled={disabled} />
        <TabButton label="Random" active={tab === 'random'} onClick={() => setTab('random')} disabled={disabled} />
      </div>

      <div className={styles.panel}>
        {tab === 'saved' && (
          <SavedDecksPanel
            decks={decks}
            selectedId={selectedSavedId}
            onSelect={setSelectedSavedId}
            onDelete={(id) => {
              deleteDeck(id)
              if (selectedSavedId === id) setSelectedSavedId(null)
            }}
            onEdit={(d) => {
              setPasteText(formatDeckText(d.cards))
              setPendingName(d.name)
              setTab('paste')
            }}
          />
        )}

        {tab === 'examples' && (
          <div className={styles.exampleGrid}>
            {examples.map((ex) => (
              <button key={ex.id} className={styles.exampleCard} onClick={() => handleLoadExample(ex)} disabled={disabled}>
                <span className={styles.exampleName}>{ex.name}</span>
                <span className={styles.exampleDesc}>{ex.description}</span>
                <span className={styles.savedItemCount}>{Object.values(ex.cards).reduce((a, b) => a + b, 0)} cards</span>
              </button>
            ))}
            {examples.length === 0 && <p className={styles.helperText}>Loading examples…</p>}
          </div>
        )}

        {tab === 'paste' && (
          <>
            <textarea
              value={pasteText}
              onChange={(e) => setPasteText(e.target.value)}
              disabled={disabled}
              className={styles.textarea}
              placeholder={'4 Lightning Bolt\n4 Goblin Guide\n12 Mountain\n…'}
            />
            <p className={styles.helperText}>One card per line. Format: "4 Card Name" or "Card Name x4".</p>
            <div className={styles.actionsRow}>
              <input
                value={pendingName}
                onChange={(e) => setPendingName(e.target.value)}
                placeholder="Deck name"
                className={styles.nameInput}
                disabled={disabled}
              />
              <button
                onClick={handleSaveCurrent}
                disabled={disabled || !pendingName.trim() || Object.keys(currentDeck).length === 0}
                className={styles.saveButton}
              >
                Save deck
              </button>
            </div>
          </>
        )}

        {tab === 'random' && (
          <>
            {availableSets.length > 0 && (
              <div className={styles.actionsRow}>
                <label className={styles.helperText} style={{ flex: 1 }}>
                  Set
                </label>
                <select
                  value={randomSetCode ?? ''}
                  onChange={(e) => {
                    const next = e.target.value === '' ? null : e.target.value
                    setRandomSetCode(next)
                    onSetCodeChange?.(next)
                  }}
                  disabled={disabled}
                  className={styles.nameInput}
                  style={{ flex: 2 }}
                >
                  <option value="">Random Set</option>
                  {[...availableSets]
                    .sort((a, b) => a.name.localeCompare(b.name))
                    .map((set) => (
                      <option key={set.code} value={set.code}>
                        {set.name}
                      </option>
                    ))}
                </select>
              </div>
            )}
            <p className={styles.helperText}>
              The server will generate a random sealed pool deck when the game starts.
            </p>
          </>
        )}
      </div>

      {tab !== 'random' && (
        <div className={styles.summary}>
          <div className={styles.summaryRow}>
            <span>{totalCards} cards</span>
            <span className={summaryStatusClass(validation, totalCards)}>
              {validationLabel(validation, totalCards)}
            </span>
          </div>

          {validation && validation.errors.length > 0 && (
            <ul className={styles.issues}>
              {validation.errors.slice(0, 5).map((e, i) => (
                <li key={i}>• {e.message}</li>
              ))}
              {validation.errors.length > 5 && <li>+{validation.errors.length - 5} more…</li>}
            </ul>
          )}

          {totalCards > 0 && stats.colorCounts.length > 0 && (
            <div className={styles.stats}>
              <div className={styles.statBlock}>
                <span className={styles.statLabel}>Colors</span>
                <span className={styles.colorPips}>
                  {stats.colorCounts.map(([color, n]) => (
                    <span key={color} className={styles.colorPip} title={`${color}: ${n}`}>
                      <span className={styles.colorDot} style={{ background: COLOR_DOT[color] ?? '#888' }} />
                      {n}
                    </span>
                  ))}
                </span>
              </div>
              <div className={styles.statBlock}>
                <span className={styles.statLabel}>Types</span>
                <span>{stats.typesLabel}</span>
              </div>
              <div className={styles.statBlock} style={{ gridColumn: '1 / -1' }}>
                <span className={styles.statLabel}>Mana curve (non-land)</span>
                <ManaCurveBars curve={stats.curve} />
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  )
}

function TabButton({
  label, active, onClick, disabled,
}: { label: string; active: boolean; onClick: () => void; disabled?: boolean }) {
  return (
    <button className={`${styles.tab} ${active ? styles.tabActive : ''}`} onClick={onClick} disabled={disabled} type="button">
      {label}
    </button>
  )
}

function SavedDecksPanel({
  decks, selectedId, onSelect, onDelete, onEdit,
}: {
  decks: SavedDeck[]
  selectedId: string | null
  onSelect: (id: string) => void
  onDelete: (id: string) => void
  onEdit: (d: SavedDeck) => void
}) {
  if (decks.length === 0) {
    return <p className={styles.helperText}>No saved decks yet. Use the Paste tab to enter a list, then Save it.</p>
  }
  return (
    <ul className={styles.savedList}>
      {decks.map((d) => {
        const total = Object.values(d.cards).reduce((a, b) => a + b, 0)
        return (
          <li
            key={d.id}
            className={`${styles.savedItem} ${selectedId === d.id ? styles.savedItemSelected : ''}`}
            onClick={() => onSelect(d.id)}
          >
            <div className={styles.savedItemMeta}>
              <span className={styles.savedItemName}>{d.name}</span>
              <span className={styles.savedItemCount}>{total} cards</span>
            </div>
            <div className={styles.savedItemActions}>
              <button className={styles.linkButton} onClick={(e) => { e.stopPropagation(); onEdit(d) }} type="button">Edit</button>
              <button className={styles.dangerButton} onClick={(e) => { e.stopPropagation(); onDelete(d.id) }} type="button">Delete</button>
            </div>
          </li>
        )
      })}
    </ul>
  )
}

function ManaCurveBars({ curve }: { curve: number[] }) {
  const max = Math.max(1, ...curve)
  return (
    <div>
      <div className={styles.curveBars}>
        {curve.map((n, i) => (
          <div
            key={i}
            className={styles.curveBar}
            style={{ height: `${(n / max) * 100}%` }}
            title={`CMC ${i === curve.length - 1 ? `${i}+` : i}: ${n}`}
          />
        ))}
      </div>
      <div className={styles.curveBars} style={{ height: 'auto' }}>
        {curve.map((_, i) => (
          <div key={i} className={styles.curveLabel}>
            {i === curve.length - 1 ? `${i}+` : i}
          </div>
        ))}
      </div>
    </div>
  )
}

function computeStats(deck: Record<string, number>, cards: Record<string, CardSummary>) {
  const colorCount: Record<string, number> = {}
  const typeCount: Record<string, number> = {}
  const curve = [0, 0, 0, 0, 0, 0, 0, 0] // 0..6, last bucket is 6+
  for (const [entry, count] of Object.entries(deck)) {
    if (count <= 0) continue
    // Strip a "#variant" suffix to look up the base card metadata.
    const baseName = entry.split('#')[0]!
    const c = cards[baseName]
    if (!c) continue
    for (const color of c.colors) colorCount[color] = (colorCount[color] ?? 0) + count
    for (const t of c.cardTypes) typeCount[t] = (typeCount[t] ?? 0) + count
    if (!c.cardTypes.includes('LAND')) {
      const idx = Math.min(c.cmc, curve.length - 1)
      curve[idx] = (curve[idx] ?? 0) + count
    }
  }
  const colorCounts = Object.entries(colorCount).sort((a, b) => b[1] - a[1])
  const typesLabel = Object.entries(typeCount)
    .sort((a, b) => b[1] - a[1])
    .slice(0, 3)
    .map(([t, n]) => `${t.charAt(0)}${t.slice(1).toLowerCase()} ${n}`)
    .join(' · ') || '—'
  return { colorCounts, typesLabel, curve }
}

function summaryStatusClass(v: ValidationResult | null, total: number): string {
  if (total === 0) return styles.statusEmpty!
  if (!v) return styles.statusEmpty!
  return v.valid ? styles.statusOk! : styles.statusBad!
}

function validationLabel(v: ValidationResult | null, total: number): string {
  if (total === 0) return ''
  if (!v) return 'Validating…'
  if (v.valid) return 'Legal ✓'
  return `${v.errors.length} issue${v.errors.length === 1 ? '' : 's'}`
}
