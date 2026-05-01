/**
 * Standalone deckbuilder page.
 *
 * Three-column layout:
 *   - Left:   saved-deck library (load/rename/delete) + menu-style filter chips
 *   - Center: search bar + sortable card grid (lazy images, click=add, shift/right-click=remove)
 *   - Right:  deck list, name input, mana curve, color pips, validation, save/save-as/delete
 *
 * Decoupled from gameStore — runs offline. Persistence is via useDeckLibrary
 * (localStorage). Server validation reuses POST /api/decks/validate.
 */
import { useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useDeckLibrary, type SavedDeck } from '@/store/deckLibrary'
import { ManaCost } from '@/components/ui/ManaSymbols'
import { HoverCardPreview } from '@/components/ui/HoverCardPreview'
import { getCardImageUrl } from '@/utils/cardImages'
import {
  parseQuery,
  toggleToken,
  hasToken,
  type CardSummary,
} from './cardFilter'
import styles from './DeckbuilderPage.module.css'

// ---------------------------------------------------------------------------
// Types & constants
// ---------------------------------------------------------------------------

const COLOR_DOT: Record<string, string> = {
  WHITE: '#f5f3da',
  BLUE: '#62a8ff',
  BLACK: '#3a3a3a',
  RED: '#ff6a4a',
  GREEN: '#4ab86a',
}

const COLOR_TOKENS: Array<{ letter: string; label: string; key: string }> = [
  { letter: 'w', label: 'W', key: 'WHITE' },
  { letter: 'u', label: 'U', key: 'BLUE' },
  { letter: 'b', label: 'B', key: 'BLACK' },
  { letter: 'r', label: 'R', key: 'RED' },
  { letter: 'g', label: 'G', key: 'GREEN' },
]

const TYPE_TOKENS = [
  'Creature',
  'Instant',
  'Sorcery',
  'Enchantment',
  'Artifact',
  'Planeswalker',
  'Land',
]

const RARITY_TOKENS = ['common', 'uncommon', 'rare', 'mythic']

type SortMode = 'name' | 'cmc' | 'color' | 'rarity'

const PAGE_SIZE = 120

// Evergreen keywords most commonly used as filters. Surface as chips; the rest
// remain reachable via free-text `kw:` / `is:` tokens.
const KEYWORD_TOKENS = [
  'Flying',
  'Trample',
  'Vigilance',
  'Lifelink',
  'Deathtouch',
  'Haste',
  'First strike',
  'Double strike',
  'Reach',
  'Menace',
  'Hexproof',
  'Defender',
  'Indestructible',
  'Flash',
  'Ward',
]

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

// ---------------------------------------------------------------------------
// Page
// ---------------------------------------------------------------------------

export function DeckbuilderPage() {
  const navigate = useNavigate()
  const { deckId } = useParams<{ deckId?: string }>()

  const decks = useDeckLibrary((s) => s.decks)
  const hydrate = useDeckLibrary((s) => s.hydrate)
  const hydrated = useDeckLibrary((s) => s.hydrated)
  const saveDeck = useDeckLibrary((s) => s.saveDeck)
  const deleteDeck = useDeckLibrary((s) => s.deleteDeck)
  const renameDeck = useDeckLibrary((s) => s.renameDeck)
  const getDeck = useDeckLibrary((s) => s.getDeck)

  // Hydrate localStorage once on mount.
  useEffect(() => {
    hydrate()
  }, [hydrate])

  // Catalog (all implemented cards) — fetched once.
  const [catalog, setCatalog] = useState<CardSummary[]>([])
  useEffect(() => {
    let cancelled = false
    fetch('/api/decks/cards')
      .then((r) => (r.ok ? r.json() : []))
      .then((list: CardSummary[]) => {
        if (!cancelled) setCatalog(list)
      })
      .catch(() => {})
    return () => {
      cancelled = true
    }
  }, [])

  const catalogIndex: Record<string, CardSummary> = useMemo(() => {
    const out: Record<string, CardSummary> = {}
    for (const c of catalog) out[c.name] = c
    return out
  }, [catalog])

  // Working deck state.
  const [deckName, setDeckName] = useState('Untitled deck')
  const [deckCards, setDeckCards] = useState<Record<string, number>>({})
  const [activeDeckId, setActiveDeckId] = useState<string | null>(null)

  // Hydrate from URL deckId once decks are loaded.
  useEffect(() => {
    if (!hydrated || !deckId) return
    const existing = getDeck(deckId)
    if (existing) {
      setDeckName(existing.name)
      setDeckCards(existing.cards)
      setActiveDeckId(existing.id)
    }
  }, [hydrated, deckId, getDeck])

  // Search & filters.
  const [query, setQuery] = useState('')
  const [sortMode, setSortMode] = useState<SortMode>('name')

  const predicate = useMemo(() => parseQuery(query), [query])
  const filtered = useMemo(() => {
    const result = catalog.filter(predicate)
    return sortCards(result, sortMode)
  }, [catalog, predicate, sortMode])

  // Pager: cap rendered tiles so a 1000+ card catalogue doesn't melt the browser.
  // Reset whenever the result set changes (new query / filter / sort).
  const [visibleCount, setVisibleCount] = useState(PAGE_SIZE)
  useEffect(() => {
    setVisibleCount(PAGE_SIZE)
  }, [query, sortMode])

  const displayed = useMemo(() => filtered.slice(0, visibleCount), [filtered, visibleCount])

  // Server-side validation (debounced via abort controllers, like DeckPicker).
  const [validation, setValidation] = useState<ValidationResult | null>(null)
  const validateAbortRef = useRef<AbortController | null>(null)
  useEffect(() => {
    if (Object.keys(deckCards).length === 0) {
      setValidation(null)
      return
    }
    validateAbortRef.current?.abort()
    const ctrl = new AbortController()
    validateAbortRef.current = ctrl
    const handle = window.setTimeout(() => {
      fetch('/api/decks/validate', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ deckList: deckCards }),
        signal: ctrl.signal,
      })
        .then((r) => (r.ok ? r.json() : null))
        .then((result: ValidationResult | null) => {
          if (!ctrl.signal.aborted) setValidation(result)
        })
        .catch(() => {})
    }, 300)
    return () => {
      window.clearTimeout(handle)
      ctrl.abort()
    }
  }, [deckCards])

  // ----- Mutations -----

  const addCard = (card: CardSummary) => {
    setDeckCards((prev) => {
      const current = prev[card.name] ?? 0
      const max = card.basicLand ? Infinity : 4
      if (current >= max) return prev
      return { ...prev, [card.name]: current + 1 }
    })
  }

  const removeCard = (name: string) => {
    setDeckCards((prev) => {
      const current = prev[name] ?? 0
      if (current <= 0) return prev
      const next = { ...prev }
      if (current === 1) delete next[name]
      else next[name] = current - 1
      return next
    })
  }

  const handleNew = () => {
    setDeckName('Untitled deck')
    setDeckCards({})
    setActiveDeckId(null)
    navigate('/deckbuilder')
  }

  const handleSave = () => {
    const saved = saveDeck({
      ...(activeDeckId ? { id: activeDeckId } : {}),
      name: deckName.trim() || 'Untitled deck',
      cards: deckCards,
    })
    setActiveDeckId(saved.id)
    if (saved.id !== deckId) navigate(`/deckbuilder/${saved.id}`, { replace: true })
  }

  const handleSaveAs = () => {
    const name = window.prompt('New deck name', `${deckName} (copy)`)
    if (!name) return
    const saved = saveDeck({ name: name.trim(), cards: deckCards })
    setDeckName(saved.name)
    setActiveDeckId(saved.id)
    navigate(`/deckbuilder/${saved.id}`, { replace: true })
  }

  const handleDelete = () => {
    if (!activeDeckId) return
    if (!window.confirm(`Delete "${deckName}"?`)) return
    deleteDeck(activeDeckId)
    handleNew()
  }

  const handleLoadSaved = (deck: SavedDeck) => {
    setDeckName(deck.name)
    setDeckCards(deck.cards)
    setActiveDeckId(deck.id)
    navigate(`/deckbuilder/${deck.id}`)
  }

  const handleRenameSaved = (deck: SavedDeck) => {
    const next = window.prompt('Rename deck', deck.name)
    if (!next || !next.trim()) return
    renameDeck(deck.id, next.trim())
    if (deck.id === activeDeckId) setDeckName(next.trim())
  }

  const handleDeleteSaved = (deck: SavedDeck) => {
    if (!window.confirm(`Delete "${deck.name}"?`)) return
    deleteDeck(deck.id)
    if (deck.id === activeDeckId) handleNew()
  }

  // ----- Render -----

  const totalCards = Object.values(deckCards).reduce((a, b) => a + b, 0)
  const stats = useMemo(() => computeStats(deckCards, catalogIndex), [deckCards, catalogIndex])

  return (
    <div className={styles.page}>
      <header className={styles.topbar}>
        <button className={styles.iconButton} onClick={() => navigate('/')}>
          ← Back to menu
        </button>
        <h1 className={styles.title}>Deckbuilder</h1>
        <div className={styles.topbarSpacer} />
        <button className={styles.iconButton} onClick={handleNew}>
          New deck
        </button>
      </header>

      {/* Left rail */}
      <aside className={styles.left}>
        <SavedDecksSection
          decks={decks}
          activeDeckId={activeDeckId}
          onLoad={handleLoadSaved}
          onRename={handleRenameSaved}
          onDelete={handleDeleteSaved}
        />
        <FilterSection query={query} onQueryChange={setQuery} catalog={catalog} />
      </aside>

      {/* Center */}
      <main className={styles.center}>
        <SearchBar
          query={query}
          onQueryChange={setQuery}
          sortMode={sortMode}
          onSortChange={setSortMode}
          resultLabel={
            catalog.length === 0
              ? 'Loading…'
              : displayed.length === filtered.length
              ? `${filtered.length} / ${catalog.length}`
              : `Showing ${displayed.length} of ${filtered.length}`
          }
        />
        <CardGrid
          cards={displayed}
          deckCards={deckCards}
          onAdd={addCard}
          onRemove={removeCard}
          hasMore={displayed.length < filtered.length}
          onShowMore={() => setVisibleCount((c) => c + PAGE_SIZE)}
        />
      </main>

      {/* Right rail */}
      <aside className={styles.right}>
        <div className={styles.deckHeader}>
          <input
            className={styles.nameInput}
            value={deckName}
            onChange={(e) => setDeckName(e.target.value)}
            placeholder="Deck name"
          />
        </div>

        <DeckListPanel deckCards={deckCards} catalog={catalogIndex} onRemove={removeCard} />

        <div className={styles.summary}>
          <div className={styles.summaryRow}>
            <span>{totalCards} cards</span>
            <span className={statusClass(validation, totalCards)}>
              {statusLabel(validation, totalCards)}
            </span>
          </div>

          {validation && validation.errors.length > 0 && (
            <ul className={styles.issues}>
              {validation.errors.slice(0, 6).map((e, i) => (
                <li key={i}>• {e.message}</li>
              ))}
              {validation.errors.length > 6 && <li>+{validation.errors.length - 6} more…</li>}
            </ul>
          )}

          {totalCards > 0 && stats.colorCounts.length > 0 && (
            <>
              <div className={styles.summaryRow}>
                <span>Colours</span>
                <span className={styles.colorPips}>
                  {stats.colorCounts.map(([color, n]) => (
                    <span key={color} className={styles.colorPip} title={`${color}: ${n}`}>
                      <span className={styles.colorDot} style={{ background: COLOR_DOT[color] ?? '#888' }} />
                      {n}
                    </span>
                  ))}
                </span>
              </div>
              <ManaCurveBars curve={stats.curve} />
            </>
          )}
        </div>

        <div className={styles.actionRow}>
          <button
            className={styles.primaryButton}
            onClick={handleSave}
            disabled={Object.keys(deckCards).length === 0}
          >
            {activeDeckId ? 'Save' : 'Save deck'}
          </button>
          <button
            className={styles.secondaryButton}
            onClick={handleSaveAs}
            disabled={Object.keys(deckCards).length === 0}
          >
            Save as
          </button>
          <button
            className={styles.dangerButton}
            onClick={handleDelete}
            disabled={!activeDeckId}
          >
            Delete
          </button>
        </div>
      </aside>
    </div>
  )
}

// ---------------------------------------------------------------------------
// Left rail sections
// ---------------------------------------------------------------------------

function SavedDecksSection({
  decks,
  activeDeckId,
  onLoad,
  onRename,
  onDelete,
}: {
  decks: SavedDeck[]
  activeDeckId: string | null
  onLoad: (d: SavedDeck) => void
  onRename: (d: SavedDeck) => void
  onDelete: (d: SavedDeck) => void
}) {
  const sorted = useMemo(() => [...decks].sort((a, b) => b.updatedAt - a.updatedAt), [decks])
  return (
    <section className={styles.section}>
      <h2 className={styles.sectionLabel}>My decks ({decks.length})</h2>
      {sorted.length === 0 ? (
        <p style={{ fontSize: '0.75rem', color: 'var(--text-muted)', margin: 0 }}>
          No saved decks yet. Build one and click Save.
        </p>
      ) : (
        <ul className={styles.savedList}>
          {sorted.map((d) => {
            const total = Object.values(d.cards).reduce((a, b) => a + b, 0)
            const isActive = d.id === activeDeckId
            return (
              <li
                key={d.id}
                className={`${styles.savedItem} ${isActive ? styles.savedItemSelected : ''}`}
                onClick={() => onLoad(d)}
              >
                <div className={styles.savedItemMeta}>
                  <span className={styles.savedItemName}>{d.name}</span>
                  <span className={styles.savedItemCount}>{total} cards</span>
                </div>
                <button
                  className={styles.linkButton}
                  onClick={(e) => {
                    e.stopPropagation()
                    onRename(d)
                  }}
                  type="button"
                >
                  Rename
                </button>
                <button
                  className={styles.dangerLink}
                  onClick={(e) => {
                    e.stopPropagation()
                    onDelete(d)
                  }}
                  type="button"
                >
                  ✕
                </button>
              </li>
            )
          })}
        </ul>
      )}
    </section>
  )
}

function SearchBar({
  query,
  onQueryChange,
  sortMode,
  onSortChange,
  resultLabel,
}: {
  query: string
  onQueryChange: (next: string) => void
  sortMode: SortMode
  onSortChange: (m: SortMode) => void
  resultLabel: string
}) {
  const [helpOpen, setHelpOpen] = useState(false)
  return (
    <div className={styles.searchBar}>
      <input
        className={styles.searchInput}
        placeholder='Search — try: c:r cmc<=3 t:creature, o:flying, -is:legendary'
        value={query}
        onChange={(e) => onQueryChange(e.target.value)}
      />
      <button
        className={`${styles.iconButton} ${helpOpen ? styles.chipActive : ''}`}
        onClick={() => setHelpOpen((v) => !v)}
        title="Search syntax"
        type="button"
      >
        ?
      </button>
      <select
        className={styles.sortSelect}
        value={sortMode}
        onChange={(e) => onSortChange(e.target.value as SortMode)}
      >
        <option value="name">Name</option>
        <option value="cmc">Mana value</option>
        <option value="color">Colour</option>
        <option value="rarity">Rarity</option>
      </select>
      <span className={styles.resultCount}>{resultLabel}</span>
      {helpOpen && <SearchHelp onClose={() => setHelpOpen(false)} onInsert={(t) => onQueryChange(t)} />}
    </div>
  )
}

function SearchHelp({ onClose, onInsert }: { onClose: () => void; onInsert: (t: string) => void }) {
  const examples: Array<{ syntax: string; desc: string }> = [
    { syntax: 'lightning', desc: 'name contains "lightning"' },
    { syntax: 't:creature', desc: 'card type / supertype / subtype' },
    { syntax: 't:goblin', desc: 'subtype (tribe) lookup' },
    { syntax: 'o:flying', desc: 'oracle text contains' },
    { syntax: 'c:r', desc: 'colour includes red (also wu, br, wubrg)' },
    { syntax: 'c=wu', desc: 'colours are exactly white + blue' },
    { syntax: 'c<=rw', desc: 'colours are a subset of red / white' },
    { syntax: 'c:colorless', desc: 'no colours' },
    { syntax: 'cmc:3', desc: 'mana value (also <=, >=, <, >, !=)' },
    { syntax: 'pow>=4', desc: 'power (numeric only)' },
    { syntax: 'tou<=2', desc: 'toughness' },
    { syntax: 'r:rare', desc: 'rarity (common / uncommon / rare / mythic)' },
    { syntax: 's:blb', desc: 'set code' },
    { syntax: 'is:legendary', desc: 'shorthand: land/creature/spell/permanent/legendary/basic' },
    { syntax: 'kw:flying', desc: 'keyword' },
    { syntax: '-t:creature', desc: 'negate any term' },
    { syntax: '"lord of"', desc: 'quote multi-word values' },
  ]
  return (
    <>
      <div className={styles.helpBackdrop} onClick={onClose} />
      <div className={styles.helpPopover} role="dialog" aria-label="Search syntax">
        <div className={styles.helpHeader}>
          <strong>Search syntax</strong>
          <button className={styles.linkButton} onClick={onClose} type="button">
            Close
          </button>
        </div>
        <p className={styles.helpHint}>
          Combine tokens with spaces (AND). Click an example to drop it into the search box.
        </p>
        <ul className={styles.helpList}>
          {examples.map((ex) => (
            <li key={ex.syntax}>
              <button
                className={styles.helpExample}
                onClick={() => {
                  onInsert(ex.syntax)
                  onClose()
                }}
                type="button"
              >
                <code>{ex.syntax}</code>
              </button>
              <span className={styles.helpDesc}>{ex.desc}</span>
            </li>
          ))}
        </ul>
      </div>
    </>
  )
}

function FilterSection({
  query,
  onQueryChange,
  catalog,
}: {
  query: string
  onQueryChange: (next: string) => void
  catalog: CardSummary[]
}) {
  const toggle = (token: string) => onQueryChange(toggleToken(query, token))

  // Numeric range filters parse current values out of the query so the boxes
  // round-trip with whatever the user typed.
  const [cmcMin, cmcMax] = useMemo(() => extractRange(query, 'cmc'), [query])
  const [powMin, powMax] = useMemo(() => extractRange(query, 'pow'), [query])
  const [touMin, touMax] = useMemo(() => extractRange(query, 'tou'), [query])

  // Distinct set codes from the catalogue (sorted), for the Set dropdown.
  const setCodes = useMemo(() => {
    const set = new Set<string>()
    for (const c of catalog) if (c.setCode) set.add(c.setCode)
    return [...set].sort()
  }, [catalog])

  const activeSet = useMemo(() => {
    const m = query.match(/(?:^|\s)s:([^\s]+)/)
    return m ? m[1]! : ''
  }, [query])

  return (
    <>
      <section className={styles.section}>
        <h2 className={styles.sectionLabel}>Colour</h2>
        <div className={styles.filterRow}>
          {COLOR_TOKENS.map(({ letter, label }) => {
            const token = `c:${letter}`
            const active = hasToken(query, token)
            return (
              <button
                key={letter}
                className={`${styles.chip} ${active ? styles.chipActive : ''}`}
                onClick={() => toggle(token)}
                type="button"
              >
                {label}
              </button>
            )
          })}
          <button
            className={`${styles.chip} ${hasToken(query, 'is:colorless') ? styles.chipActive : ''}`}
            onClick={() => toggle('is:colorless')}
            type="button"
          >
            C
          </button>
        </div>
      </section>

      <section className={styles.section}>
        <h2 className={styles.sectionLabel}>Type</h2>
        <div className={styles.filterRow}>
          {TYPE_TOKENS.map((label) => {
            const token = `t:${label}`
            const active = hasToken(query, token)
            return (
              <button
                key={label}
                className={`${styles.chip} ${active ? styles.chipActive : ''}`}
                onClick={() => toggle(token)}
                type="button"
              >
                {label}
              </button>
            )
          })}
        </div>
      </section>

      <section className={styles.section}>
        <h2 className={styles.sectionLabel}>Rarity</h2>
        <div className={styles.filterRow}>
          {RARITY_TOKENS.map((label) => {
            const token = `r:${label}`
            const active = hasToken(query, token)
            return (
              <button
                key={label}
                className={`${styles.chip} ${active ? styles.chipActive : ''}`}
                onClick={() => toggle(token)}
                type="button"
              >
                {label[0]!.toUpperCase() + label.slice(1)}
              </button>
            )
          })}
        </div>
      </section>

      <section className={styles.section}>
        <h2 className={styles.sectionLabel}>Mana value</h2>
        <RangeRow
          token="cmc"
          min={cmcMin}
          max={cmcMax}
          onMin={(v) => onQueryChange(setRange(query, 'cmc', v, cmcMax))}
          onMax={(v) => onQueryChange(setRange(query, 'cmc', cmcMin, v))}
        />
      </section>

      <section className={styles.section}>
        <h2 className={styles.sectionLabel}>Power / Toughness</h2>
        <RangeRow
          token="pow"
          label="Pow"
          min={powMin}
          max={powMax}
          onMin={(v) => onQueryChange(setRange(query, 'pow', v, powMax))}
          onMax={(v) => onQueryChange(setRange(query, 'pow', powMin, v))}
        />
        <RangeRow
          token="tou"
          label="Tou"
          min={touMin}
          max={touMax}
          onMin={(v) => onQueryChange(setRange(query, 'tou', v, touMax))}
          onMax={(v) => onQueryChange(setRange(query, 'tou', touMin, v))}
        />
      </section>

      <section className={styles.section}>
        <h2 className={styles.sectionLabel}>Keywords</h2>
        <div className={styles.filterRow}>
          {KEYWORD_TOKENS.map((label) => {
            const token = `kw:${label.toLowerCase().replace(/\s+/g, '_')}`
            // KEYWORD enum names use underscores; the parser is case-insensitive.
            const active = hasToken(query, token)
            return (
              <button
                key={label}
                className={`${styles.chip} ${active ? styles.chipActive : ''}`}
                onClick={() => toggle(token)}
                type="button"
              >
                {label}
              </button>
            )
          })}
        </div>
      </section>

      {setCodes.length > 0 && (
        <section className={styles.section}>
          <h2 className={styles.sectionLabel}>Set</h2>
          <select
            className={styles.sortSelect}
            value={activeSet}
            onChange={(e) => {
              const next = e.target.value
              const without = query.replace(/(?:^|\s)s:[^\s]+(?=\s|$)/g, '').trim()
              onQueryChange(next ? (without ? `${without} s:${next}` : `s:${next}`) : without)
            }}
          >
            <option value="">All sets</option>
            {setCodes.map((code) => (
              <option key={code} value={code}>
                {code}
              </option>
            ))}
          </select>
        </section>
      )}

      <section className={styles.section}>
        <button
          className={styles.linkButton}
          onClick={() => onQueryChange('')}
          disabled={query.length === 0}
        >
          Clear filters
        </button>
      </section>
    </>
  )
}

function RangeRow({
  label,
  min,
  max,
  onMin,
  onMax,
}: {
  token: string
  label?: string
  min: number | null
  max: number | null
  onMin: (v: string) => void
  onMax: (v: string) => void
}) {
  return (
    <div className={styles.numericRow}>
      {label && <span style={{ minWidth: 32 }}>{label}</span>}
      <span>min</span>
      <input
        type="number"
        min={0}
        max={20}
        className={styles.numericInput}
        value={min ?? ''}
        onChange={(e) => onMin(e.target.value)}
      />
      <span>max</span>
      <input
        type="number"
        min={0}
        max={20}
        className={styles.numericInput}
        value={max ?? ''}
        onChange={(e) => onMax(e.target.value)}
      />
    </div>
  )
}

// ---------------------------------------------------------------------------
// Card grid
// ---------------------------------------------------------------------------

function CardGrid({
  cards,
  deckCards,
  onAdd,
  onRemove,
  hasMore,
  onShowMore,
}: {
  cards: CardSummary[]
  deckCards: Record<string, number>
  onAdd: (c: CardSummary) => void
  onRemove: (name: string) => void
  hasMore: boolean
  onShowMore: () => void
}) {
  const [hoverCard, setHoverCard] = useState<CardSummary | null>(null)
  const [hoverPos, setHoverPos] = useState<{ x: number; y: number } | null>(null)

  if (cards.length === 0) {
    return (
      <div className={styles.gridScroll}>
        <div className={styles.grid}>
          <div className={styles.emptyState}>No cards match the current filters.</div>
        </div>
      </div>
    )
  }

  return (
    <>
      <div className={styles.gridScroll}>
        <div className={styles.grid}>
          {cards.map((card) => (
            <CardTile
              key={card.name}
              card={card}
              count={deckCards[card.name] ?? 0}
              onAdd={onAdd}
              onRemove={onRemove}
              onHover={(c, e) => {
                setHoverCard(c)
                if (e) setHoverPos({ x: e.clientX, y: e.clientY })
              }}
              onLeave={() => {
                setHoverCard(null)
                setHoverPos(null)
              }}
            />
          ))}
        </div>
        {hasMore && (
          <div className={styles.showMoreRow}>
            <button className={styles.secondaryButton} onClick={onShowMore} type="button">
              Show more
            </button>
          </div>
        )}
      </div>
      {hoverCard && (
        <HoverCardPreview
          name={hoverCard.name}
          imageUri={hoverCard.imageUri ?? null}
          pos={hoverPos}
        />
      )}
    </>
  )
}

function CardTile({
  card,
  count,
  onAdd,
  onRemove,
  onHover,
  onLeave,
}: {
  card: CardSummary
  count: number
  onAdd: (c: CardSummary) => void
  onRemove: (name: string) => void
  onHover: (c: CardSummary, e: React.MouseEvent | null) => void
  onLeave: () => void
}) {
  // Lazy-load the image once the tile scrolls into view.
  const ref = useRef<HTMLDivElement | null>(null)
  const [visible, setVisible] = useState(false)
  useEffect(() => {
    if (!ref.current) return
    const obs = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (entry.isIntersecting) {
            setVisible(true)
            obs.disconnect()
            return
          }
        }
      },
      { rootMargin: '200px' }
    )
    obs.observe(ref.current)
    return () => obs.disconnect()
  }, [])

  const handleClick = (e: React.MouseEvent) => {
    if (e.shiftKey) {
      onRemove(card.name)
    } else {
      onAdd(card)
    }
  }

  const handleContextMenu = (e: React.MouseEvent) => {
    e.preventDefault()
    onRemove(card.name)
  }

  return (
    <div
      ref={ref}
      className={styles.cardTile}
      onClick={handleClick}
      onContextMenu={handleContextMenu}
      onMouseEnter={(e) => onHover(card, e)}
      onMouseMove={(e) => onHover(card, e)}
      onMouseLeave={onLeave}
      title={`${card.name}\nClick to add · Shift/right-click to remove`}
    >
      {visible ? (
        <img
          className={styles.cardImage}
          src={resolveImageUrl(card)}
          alt={card.name}
          loading="lazy"
          onError={(e) => {
            // Fall back to the text tile if the image 404s.
            ;(e.currentTarget as HTMLImageElement).style.display = 'none'
          }}
        />
      ) : (
        <CardTextFallback card={card} />
      )}
      {count > 0 && <span className={styles.cardCountBadge}>{count}</span>}
    </div>
  )
}

function CardTextFallback({ card }: { card: CardSummary }) {
  const typeLabel = [card.supertypes, card.cardTypes, card.subtypes]
    .flat()
    .filter(Boolean)
    .map((t) => t[0]! + t.slice(1).toLowerCase())
    .join(' ')
  return (
    <div className={styles.cardTextFallback}>
      <span className={styles.cardFallbackName}>{card.name}</span>
      <ManaCost cost={card.manaCost || null} size={12} />
      <span className={styles.cardFallbackType}>{typeLabel}</span>
    </div>
  )
}

function resolveImageUrl(card: CardSummary): string {
  return getCardImageUrl(card.name, card.imageUri ?? null, 'normal')
}

// ---------------------------------------------------------------------------
// Deck list panel (right rail)
// ---------------------------------------------------------------------------

function DeckListPanel({
  deckCards,
  catalog,
  onRemove,
}: {
  deckCards: Record<string, number>
  catalog: Record<string, CardSummary>
  onRemove: (name: string) => void
}) {
  const grouped = useMemo(() => groupForDeckList(deckCards, catalog), [deckCards, catalog])
  return (
    <div className={styles.deckList}>
      {grouped.map((group) => (
        <div key={group.label} className={styles.deckGroup}>
          <h3 className={styles.deckGroupLabel}>
            {group.label} ({group.entries.reduce((a, e) => a + e.count, 0)})
          </h3>
          {group.entries.map((entry) => (
            <div key={entry.name} className={styles.deckRow}>
              <span className={styles.deckRowCount}>{entry.count}×</span>
              <span className={styles.deckRowName}>{entry.name}</span>
              <span className={styles.deckRowCost}>
                <ManaCost cost={entry.card?.manaCost || null} size={11} />
              </span>
              <button
                className={styles.deckRowRemove}
                onClick={() => onRemove(entry.name)}
                aria-label={`Remove ${entry.name}`}
                type="button"
              >
                ✕
              </button>
            </div>
          ))}
        </div>
      ))}
      {grouped.length === 0 && (
        <p style={{ color: 'var(--text-muted)', fontSize: '0.8rem', textAlign: 'center', margin: 'var(--space-4) 0' }}>
          Click cards in the grid to add them.
        </p>
      )}
    </div>
  )
}

// ---------------------------------------------------------------------------
// Stats / helpers
// ---------------------------------------------------------------------------

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
  const curve = [0, 0, 0, 0, 0, 0, 0, 0]
  for (const [name, count] of Object.entries(deck)) {
    if (count <= 0) continue
    const c = cards[name.split('#')[0] ?? name]
    if (!c) continue
    for (const col of c.colors) colorCount[col] = (colorCount[col] ?? 0) + count
    if (!c.cardTypes.includes('LAND')) {
      const idx = Math.min(c.cmc, curve.length - 1)
      curve[idx] = (curve[idx] ?? 0) + count
    }
  }
  return {
    colorCounts: Object.entries(colorCount).sort((a, b) => b[1] - a[1]),
    curve,
  }
}

interface DeckGroup {
  label: string
  entries: Array<{ name: string; count: number; card: CardSummary | undefined }>
}

function groupForDeckList(deck: Record<string, number>, catalog: Record<string, CardSummary>): DeckGroup[] {
  const spells: DeckGroup['entries'] = []
  const lands: DeckGroup['entries'] = []
  for (const [name, count] of Object.entries(deck)) {
    if (count <= 0) continue
    const card = catalog[name]
    const entry = { name, count, card }
    if (card?.cardTypes.includes('LAND')) lands.push(entry)
    else spells.push(entry)
  }
  spells.sort(byCmcThenName)
  lands.sort(byCmcThenName)
  const groups: DeckGroup[] = []
  if (spells.length > 0) groups.push({ label: 'Spells', entries: spells })
  if (lands.length > 0) groups.push({ label: 'Lands', entries: lands })
  return groups
}

function byCmcThenName(a: { name: string; card: CardSummary | undefined }, b: { name: string; card: CardSummary | undefined }) {
  const ac = a.card?.cmc ?? 99
  const bc = b.card?.cmc ?? 99
  if (ac !== bc) return ac - bc
  return a.name.localeCompare(b.name)
}

function sortCards(cards: CardSummary[], mode: SortMode): CardSummary[] {
  const arr = [...cards]
  switch (mode) {
    case 'name':
      arr.sort((a, b) => a.name.localeCompare(b.name))
      break
    case 'cmc':
      arr.sort((a, b) => a.cmc - b.cmc || a.name.localeCompare(b.name))
      break
    case 'color':
      arr.sort((a, b) => colorBucket(a) - colorBucket(b) || a.name.localeCompare(b.name))
      break
    case 'rarity':
      arr.sort((a, b) => rarityRank(a.rarity) - rarityRank(b.rarity) || a.name.localeCompare(b.name))
      break
  }
  return arr
}

function colorBucket(c: CardSummary): number {
  if (c.colors.length === 0) return 99
  if (c.colors.length > 1) return 6
  switch (c.colors[0]) {
    case 'WHITE': return 0
    case 'BLUE': return 1
    case 'BLACK': return 2
    case 'RED': return 3
    case 'GREEN': return 4
    default: return 5
  }
}

function rarityRank(r: string): number {
  switch (r) {
    case 'MYTHIC': return 0
    case 'RARE': return 1
    case 'UNCOMMON': return 2
    case 'COMMON': return 3
    default: return 4
  }
}

function statusClass(v: ValidationResult | null, total: number): string {
  if (total === 0) return styles.statusEmpty!
  if (!v) return styles.statusEmpty!
  return v.valid ? styles.statusOk! : styles.statusBad!
}

function statusLabel(v: ValidationResult | null, total: number): string {
  if (total === 0) return 'Empty'
  if (!v) return 'Validating…'
  if (v.valid) return 'Legal ✓'
  return `${v.errors.length} issue${v.errors.length === 1 ? '' : 's'}`
}

// ---------------------------------------------------------------------------
// Range helpers (numeric inputs ↔ query string)
//
// Manipulate `<key>>=N` and `<key><=N` tokens directly so the menu and the
// raw query string stay in sync. Generic over key (cmc, pow, tou).
// ---------------------------------------------------------------------------

function extractRange(query: string, key: string): [number | null, number | null] {
  const minRe = new RegExp(`(?:^|\\s)${key}>=(\\d+)(?:\\s|$)`)
  const maxRe = new RegExp(`(?:^|\\s)${key}<=(\\d+)(?:\\s|$)`)
  const minMatch = query.match(minRe)
  const maxMatch = query.match(maxRe)
  return [
    minMatch ? parseInt(minMatch[1]!, 10) : null,
    maxMatch ? parseInt(maxMatch[1]!, 10) : null,
  ]
}

function setRange(
  query: string,
  key: string,
  min: string | number | null,
  max: string | number | null
): string {
  const minRe = new RegExp(`(?:^|\\s)${key}>=\\d+(?=\\s|$)`, 'g')
  const maxRe = new RegExp(`(?:^|\\s)${key}<=\\d+(?=\\s|$)`, 'g')
  let next = query.replace(minRe, '').replace(maxRe, '').trim()
  const minN = typeof min === 'string' ? (min === '' ? null : parseInt(min, 10)) : min
  const maxN = typeof max === 'string' ? (max === '' ? null : parseInt(max, 10)) : max
  if (minN !== null && Number.isFinite(minN)) next = `${next} ${key}>=${minN}`.trim()
  if (maxN !== null && Number.isFinite(maxN)) next = `${next} ${key}<=${maxN}`.trim()
  return next
}
