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
import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate, useParams, useSearchParams } from 'react-router-dom'
import {
  useDeckLibrary,
  mergeCommanderIntoCards,
  stripCommanderFromCards,
  type SavedDeck,
} from '@/store/deckLibrary'
import { ManaCost, ManaSymbol } from '@/components/ui/ManaSymbols'
import { HoverCardPreview } from '@/components/ui/HoverCardPreview'
import { useDfcHoverFlip } from '@/components/ui/useDfcHoverFlip'
import { getCardImageUrl } from '@/utils/cardImages'
import {
  parseQuery,
  toggleToken,
  hasToken,
  addToken,
  removeToken,
  isAdvancedQuery,
  type CardSummary,
  type ParseError,
} from './cardFilter'
import {
  parseArenaDeckList,
  resolveAgainstCatalog,
  type ResolveResult,
} from './parseArenaDeck'
import {
  detectProducedColors,
  suggestBasicLands,
  type BasicLand,
  type DeckEntry,
  type LandColor,
} from '@/utils/landSuggestion'
import {
  labelForFormat,
  DECK_FORMATS,
  useDeckLegalFormats,
} from '@/utils/deckLegality'
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

// Deck-construction formats with Scryfall-sourced legality data. Pulled from the shared
// helper so the deckbuilder's picker and the lobby filtering stay in lockstep.
const FORMAT_TOKENS = DECK_FORMATS

type SetInfo = { code: string; name: string }

type SortMode = 'name' | 'cmc' | 'color' | 'rarity'

// "cards" = original layout (catalog grid in center, deck list on right).
// "deck"  = Moxfield-style: deck takes the wide center pane, multi-column grouped by type;
//           catalog/search moves to the right rail. Persisted via `?view=deck`.
type ViewMode = 'cards' | 'deck'

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
  const [searchParams, setSearchParams] = useSearchParams()

  // Preserve the current filter querystring across deck-route navigations so
  // changing/loading a deck doesn't wipe the user's filters.
  const searchSuffix = useCallback(() => {
    const s = searchParams.toString()
    return s ? `?${s}` : ''
  }, [searchParams])

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
    fetch('/api/cards')
      .then((r) => (r.ok ? r.json() : []))
      .then((list: CardSummary[]) => {
        if (!cancelled) setCatalog(list)
      })
      .catch(() => {})
    return () => {
      cancelled = true
    }
  }, [])

  // Set code → display name, fetched from the server (active sets only).
  const [setNames, setSetNames] = useState<Record<string, string>>({})
  useEffect(() => {
    let cancelled = false
    fetch('/api/sets')
      .then((r) => (r.ok ? r.json() : []))
      .then((list: SetInfo[]) => {
        if (cancelled) return
        const map: Record<string, string> = {}
        for (const s of list) map[s.code] = s.name
        setSetNames(map)
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
  // Designated commander for Commander/Brawl/Standard Brawl decks. Null when no commander has
  // been picked yet, or when the current format doesn't use a commander. The deckbuilder UI
  // exposes a crown toggle on each row to set/clear this — see DeckListPanel below.
  const [commander, setCommander] = useState<string | null>(null)

  // Hydrate from URL deckId once decks are loaded. Also push the saved deck's stamped
  // format into the URL — without it, `activeFormat` stays whatever was in the search
  // params (often `null`), and the "clear commander when not a commander format" effect
  // would immediately wipe a just-loaded commander designation.
  //
  // Guarded by a ref so the effect runs at most once per actual `deckId` change. Without
  // this, react-router's `setSearchParams` reference changes on every URL update — which
  // includes typing into the search filter. Each keystroke would otherwise re-trigger
  // hydration and overwrite in-progress edits with whatever's persisted in localStorage.
  const lastHydratedDeckIdRef = useRef<string | null>(null)
  useEffect(() => {
    if (!hydrated || !deckId) return
    if (lastHydratedDeckIdRef.current === deckId) return
    lastHydratedDeckIdRef.current = deckId
    const existing = getDeck(deckId)
    if (existing) {
      setDeckName(existing.name)
      setDeckCards(mergeCommanderIntoCards(existing.cards, existing.commander ?? null))
      setCommander(existing.commander ?? null)
      setActiveDeckId(existing.id)
      if (existing.format) {
        setSearchParams(
          (prev) => {
            const params = new URLSearchParams(prev)
            params.set('fmt', existing.format!.toUpperCase())
            return params
          },
          { replace: true },
        )
      }
    }
    // setSearchParams is intentionally excluded — see the comment above.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [hydrated, deckId, getDeck])

  // Import-from-text modal visibility.
  const [importOpen, setImportOpen] = useState(false)
  // Bulk-edit (export + edit text) modal visibility.
  const [bulkEditOpen, setBulkEditOpen] = useState(false)
  // Saved-decks browser overlay visibility.
  const [decksBrowserOpen, setDecksBrowserOpen] = useState(false)

  // Search & filters live in the URL (?q=…&sort=…) so they're shareable and
  // survive refreshes. `query` and `sortMode` are derived from searchParams.
  const query = searchParams.get('q') ?? ''
  const sortParam = searchParams.get('sort')
  const sortMode: SortMode =
    sortParam === 'cmc' || sortParam === 'color' || sortParam === 'rarity' ? sortParam : 'name'

  const setQuery = useCallback(
    (next: string) => {
      setSearchParams(
        (prev) => {
          const params = new URLSearchParams(prev)
          if (next) params.set('q', next)
          else params.delete('q')
          return params
        },
        { replace: true }
      )
    },
    [setSearchParams]
  )

  const setSortMode = useCallback(
    (next: SortMode) => {
      setSearchParams(
        (prev) => {
          const params = new URLSearchParams(prev)
          if (next === 'name') params.delete('sort')
          else params.set('sort', next)
          return params
        },
        { replace: true }
      )
    },
    [setSearchParams]
  )

  // View mode toggle — Moxfield-style "deck centric" layout vs. the original "cards to add"
  // layout. Persisted in the URL so refreshes / shared links keep the user's preference.
  const viewMode: ViewMode = searchParams.get('view') === 'deck' ? 'deck' : 'cards'
  const setViewMode = useCallback(
    (next: ViewMode) => {
      setSearchParams(
        (prev) => {
          const params = new URLSearchParams(prev)
          if (next === 'deck') params.set('view', 'deck')
          else params.delete('view')
          return params
        },
        { replace: true }
      )
    },
    [setSearchParams]
  )

  // The deck's chosen format lives in its own URL param (`fmt`) so the search-bar query is
  // free to contain anything. Without this separation, editing/typing in the search bar
  // would clobber the `format:<name>` token and silently un-set the deck format.
  const activeFormat = useMemo(() => {
    const raw = searchParams.get('fmt')
    return raw ? raw.toUpperCase() : null
  }, [searchParams])

  const setActiveFormat = useCallback(
    (next: string | null) => {
      setSearchParams(
        (prev) => {
          const params = new URLSearchParams(prev)
          if (next) params.set('fmt', next.toUpperCase())
          else params.delete('fmt')
          return params
        },
        { replace: true },
      )
    },
    [setSearchParams],
  )

  // True when the active format uses a designated commander (CR 903.5b for Commander; same
  // shape for Brawl and Standard Brawl). Mirrors `DeckFormat.isCommanderShape` on the server.
  const isCommanderFormat = useMemo(
    () => activeFormat === 'COMMANDER' || activeFormat === 'BRAWL' || activeFormat === 'STANDARD_BRAWL',
    [activeFormat],
  )

  // Clear the commander designation when:
  //  - the user switches to a non-commander format (the field would otherwise be ignored
  //    by validation but stay visually marked, which is confusing), or
  //  - the designated commander gets removed from the deck list entirely.
  // Both conditions are quiet: we don't surface a toast, the crown just goes away.
  useEffect(() => {
    if (!isCommanderFormat && commander !== null) setCommander(null)
  }, [isCommanderFormat, commander])
  useEffect(() => {
    if (commander && !(commander in deckCards)) setCommander(null)
  }, [commander, deckCards])

  const parseResult = useMemo(() => parseQuery(query, { withErrors: true }), [query])
  const predicate = parseResult.predicate
  const queryErrors = parseResult.errors
  const advanced = useMemo(() => isAdvancedQuery(query), [query])
  // When a deck format is selected, scope the catalog to format-legal cards automatically so
  // the user only sees plays they can actually run. The `format:` query token still works as
  // an extra filter (intersected on top), but isn't required for this default behavior.
  const filtered = useMemo(() => {
    let result = catalog.filter(predicate)
    if (activeFormat) {
      result = result.filter((c) => c.legalFormats?.includes(activeFormat) ?? false)
    }
    return sortCards(result, sortMode)
  }, [catalog, predicate, sortMode, activeFormat])

  // Pager: cap rendered tiles so a 1000+ card catalogue doesn't melt the browser.
  // Reset whenever the result set changes (new query / filter / sort).
  const [visibleCount, setVisibleCount] = useState(PAGE_SIZE)
  useEffect(() => {
    setVisibleCount(PAGE_SIZE)
  }, [query, sortMode, activeFormat])

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
      // The server's `Deck.cards` is documented as the library only — it does NOT include
      // the commander (CR 903.6a: the commander begins in the command zone). The validator
      // adds the commander on top of `cards`, so if we include the designated commander in
      // `deckList` it gets counted twice. Strip it out at the boundary; the internal UI
      // model still keeps the commander in `deckCards` so the crown sits on a real row.
      const sendCommander = isCommanderFormat && commander
      const deckListForValidation = sendCommander
        ? Object.fromEntries(
            Object.entries(deckCards).flatMap(([name, count]) => {
              if (name !== commander) return [[name, count]]
              const remaining = count - 1
              return remaining > 0 ? [[name, remaining]] : []
            }),
          )
        : deckCards
      fetch('/api/decks/validate', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          deckList: deckListForValidation,
          ...(activeFormat ? { format: activeFormat } : {}),
          // Threading commander through the validation payload lets the server apply the
          // commander rules (eligibility + color identity) live as the user designates one.
          // Only sent for commander-shape formats — for Standard/Modern/etc. it'd be ignored
          // anyway, but keeping the wire payload minimal avoids stale fields surfacing later.
          ...(sendCommander ? { commander } : {}),
        }),
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
  }, [deckCards, activeFormat, isCommanderFormat, commander])

  // Map card-name → set of violation codes produced by the latest validation pass. Used to
  // light up specific rows in the deck list (color identity, singleton, format-illegal copies)
  // without re-implementing those rules on the client. Keys are case-sensitive card names —
  // identical to the entries in `deckCards`, so `rowViolations.get(entry.name)` is a direct hit.
  const rowViolations = useMemo(() => {
    const map = new Map<string, Set<string>>()
    for (const issue of validation?.errors ?? []) {
      if (!issue.cardName) continue
      const codes = map.get(issue.cardName) ?? new Set<string>()
      codes.add(issue.code)
      map.set(issue.cardName, codes)
    }
    return map
  }, [validation])

  // ----- Mutations -----

  const addCard = (card: CardSummary) => {
    setDeckCards((prev) => {
      const current = prev[card.name] ?? 0
      const max = effectiveCopyCap(card, isCommanderFormat)
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

  const setCardCount = useCallback((name: string, count: number) => {
    setDeckCards((prev) => {
      const next = { ...prev }
      if (count <= 0) delete next[name]
      else next[name] = count
      return next
    })
  }, [])

  const handleNew = () => {
    setDeckName('Untitled deck')
    setDeckCards({})
    setCommander(null)
    setActiveDeckId(null)
    navigate(`/deckbuilder${searchSuffix()}`)
  }

  const handleSave = () => {
    // Per `SavedDeck.commander`: the commander is stored separately from `cards` (the
    // library), matching the server's `Deck.cards` convention. Strip it out on save so
    // reloading + re-validating doesn't double-count.
    const designated = isCommanderFormat ? commander : null
    const saved = saveDeck({
      ...(activeDeckId ? { id: activeDeckId } : {}),
      name: deckName.trim() || 'Untitled deck',
      cards: stripCommanderFromCards(deckCards, designated),
      ...(activeFormat ? { format: activeFormat } : {}),
      ...(designated ? { commander: designated } : {}),
    })
    setActiveDeckId(saved.id)
    if (saved.id !== deckId) navigate(`/deckbuilder/${saved.id}${searchSuffix()}`, { replace: true })
  }

  const handleSaveAs = () => {
    const name = window.prompt('New deck name', `${deckName} (copy)`)
    if (!name) return
    const designated = isCommanderFormat ? commander : null
    const saved = saveDeck({
      name: name.trim(),
      cards: stripCommanderFromCards(deckCards, designated),
      ...(activeFormat ? { format: activeFormat } : {}),
      ...(designated ? { commander: designated } : {}),
    })
    setDeckName(saved.name)
    setActiveDeckId(saved.id)
    navigate(`/deckbuilder/${saved.id}${searchSuffix()}`, { replace: true })
  }

  const handleDelete = () => {
    if (!activeDeckId) return
    if (!window.confirm(`Delete "${deckName}"?`)) return
    deleteDeck(activeDeckId)
    handleNew()
  }

  const handleLoadSaved = (deck: SavedDeck) => {
    setDeckName(deck.name)
    setDeckCards(mergeCommanderIntoCards(deck.cards, deck.commander ?? null))
    setCommander(deck.commander ?? null)
    setActiveDeckId(deck.id)
    // Restore the deck's stamped format into the URL alongside the existing search
    // params. Without this, `activeFormat` stays whatever was selected before, and
    // the "clear commander when not a commander format" effect would immediately
    // wipe a just-loaded commander designation. Done in one navigate call so we
    // don't race against the async `setSearchParams` update.
    const params = new URLSearchParams(searchParams)
    if (deck.format) params.set('fmt', deck.format.toUpperCase())
    else params.delete('fmt')
    const suffix = params.toString()
    navigate(`/deckbuilder/${deck.id}${suffix ? `?${suffix}` : ''}`)
    setDecksBrowserOpen(false)
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

  const handleImport = (
    cards: Record<string, number>,
    suggestedName: string | null,
    importedCommander: string | null,
  ) => {
    setDeckCards(cards)
    // The commander designation rides along when the source list had a Commander
    // section; otherwise reset so a stale value from the previous deck doesn't leak.
    setCommander(importedCommander)
    setActiveDeckId(null)
    if (suggestedName) setDeckName(suggestedName)
    navigate(`/deckbuilder${searchSuffix()}`)
    setImportOpen(false)
  }

  // ----- Render -----

  const totalCards = Object.values(deckCards).reduce((a, b) => a + b, 0)
  const stats = useMemo(() => computeStats(deckCards, catalogIndex), [deckCards, catalogIndex])

  // Deck-mode left-rail card preview. Hover state is lifted out of DeckCentricView so the
  // (Moxfield-style) preview can live in the left rail instead of floating near the cursor.
  const [deckHoverName, setDeckHoverName] = useState<string | null>(null)
  const [deckHoverCard, setDeckHoverCard] = useState<CardSummary | null>(null)
  const deckHoverDfc = useDfcHoverFlip(
    deckHoverCard
      ? {
          name: deckHoverCard.name,
          imageUri: deckHoverCard.imageUri ?? null,
          isDoubleFaced: deckHoverCard.isDoubleFaced ?? false,
          backFaceName: deckHoverCard.backFaceName ?? null,
          backFaceImageUri: deckHoverCard.backFaceImageUri ?? null,
        }
      : null,
  )
  const resetDeckHoverDfc = deckHoverDfc.resetFlip
  const handleDeckHoverEnter = useCallback(
    (entry: { name: string; card: CardSummary | undefined }) => {
      setDeckHoverName((prev) => {
        if (prev !== entry.name) resetDeckHoverDfc()
        return entry.name
      })
      setDeckHoverCard(entry.card ?? null)
    },
    [resetDeckHoverDfc],
  )
  const handleDeckHoverLeave = useCallback(() => {
    setDeckHoverName(null)
    setDeckHoverCard(null)
  }, [])
  useEffect(() => {
    if (deckHoverName && !(deckHoverName in deckCards) && deckHoverName !== commander) {
      setDeckHoverName(null)
      setDeckHoverCard(null)
    }
  }, [deckCards, commander, deckHoverName])

  return (
    <div className={`${styles.page} ${viewMode === 'deck' ? styles.pageDeckMode : ''}`}>
      <header className={styles.topbar}>
        <button className={styles.iconButton} onClick={() => navigate('/')}>
          ← Back to menu
        </button>
        <h1 className={styles.title}>Deckbuilder</h1>
        <div className={styles.viewToggle} role="group" aria-label="View mode">
          <button
            type="button"
            className={
              viewMode === 'cards' ? styles.viewToggleButtonActive : styles.viewToggleButton
            }
            onClick={() => setViewMode('cards')}
            aria-pressed={viewMode === 'cards'}
            title="Browse the catalog and click cards to add them"
          >
            Cards to add
          </button>
          <button
            type="button"
            className={
              viewMode === 'deck' ? styles.viewToggleButtonActive : styles.viewToggleButton
            }
            onClick={() => setViewMode('deck')}
            aria-pressed={viewMode === 'deck'}
            title="See the deck grouped by type — Moxfield style"
          >
            Deck
          </button>
        </div>
        <div className={styles.topbarSpacer} />
        <button className={styles.iconButton} onClick={() => setImportOpen(true)}>
          Import deck
        </button>
        <button
          className={styles.iconButton}
          onClick={() => setBulkEditOpen(true)}
          disabled={Object.keys(deckCards).length === 0}
        >
          Bulk edit / export
        </button>
        <button className={styles.iconButton} onClick={handleNew}>
          New deck
        </button>
      </header>

      {importOpen && (
        <ImportDeckModal
          catalog={catalog}
          hasExisting={Object.keys(deckCards).length > 0}
          onCancel={() => setImportOpen(false)}
          onImport={handleImport}
        />
      )}

      {bulkEditOpen && (
        <BulkEditDeckModal
          deckCards={deckCards}
          commander={commander}
          catalog={catalog}
          catalogIndex={catalogIndex}
          onClose={() => setBulkEditOpen(false)}
          onApply={(cards, nextCommander) => {
            setDeckCards(cards)
            setCommander(nextCommander)
            setBulkEditOpen(false)
          }}
        />
      )}

      {decksBrowserOpen && (
        <SavedDecksBrowser
          decks={decks}
          catalog={catalogIndex}
          activeDeckId={activeDeckId}
          onClose={() => setDecksBrowserOpen(false)}
          onLoad={handleLoadSaved}
          onRename={handleRenameSaved}
          onDelete={handleDeleteSaved}
        />
      )}

      {/* Left rail */}
      <aside className={styles.left}>
        <SavedDecksSummary
          decks={decks}
          activeDeckId={activeDeckId}
          onOpen={() => setDecksBrowserOpen(true)}
        />
        {viewMode === 'deck' ? (
          <DeckHoverPreview
            name={deckHoverName ? (deckHoverDfc.displayName ?? deckHoverName) : null}
            imageUri={
              deckHoverName
                ? (deckHoverDfc.displayImageUri ?? deckHoverCard?.imageUri ?? null)
                : null
            }
            overlay={deckHoverDfc.hint}
          />
        ) : (
          <FilterSection
            query={query}
            onQueryChange={setQuery}
            catalog={catalog}
            setNames={setNames}
            advanced={advanced}
          />
        )}
      </aside>

      {/* Center + right rail are swapped between view modes. In "cards" mode the catalog grid
          owns the center pane and the deck list lives on the right (the original layout). In
          "deck" mode (Moxfield-style) the deck takes the wide center as a multi-column grouped
          list, and the catalog/search collapses to the right rail. */}
      {viewMode === 'cards' ? (
        <>
          <main className={styles.center}>
            <SearchBar
              query={query}
              onQueryChange={setQuery}
              sortMode={sortMode}
              onSortChange={setSortMode}
              errors={queryErrors}
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

          <aside className={styles.right}>
            <div className={styles.deckHeader}>
              <input
                className={styles.nameInput}
                value={deckName}
                onChange={(e) => setDeckName(e.target.value)}
                placeholder="Deck name"
              />
              <DeckFormatPicker
                activeFormat={activeFormat}
                onChange={setActiveFormat}
              />
            </div>

            <DeckListPanel
              deckCards={deckCards}
              catalog={catalogIndex}
              activeFormat={activeFormat}
              onAdd={addCard}
              onRemove={removeCard}
              commander={commander}
              showCommanderControls={isCommanderFormat}
              onToggleCommander={(name) =>
                setCommander((prev) => (prev === name ? null : name))
              }
              rowViolations={rowViolations}
              isCommanderFormat={isCommanderFormat}
            />

            <BasicLandsPanel
              catalog={catalog}
              deckCards={deckCards}
              onAdd={addCard}
              onRemove={removeCard}
              onSuggest={() => suggestLandsForDeck(deckCards, catalogIndex, catalog, setCardCount)}
            />

            <DeckSummary
              validation={validation}
              totalCards={totalCards}
              stats={stats}
            />

            <DeckActionRow
              activeDeckId={activeDeckId}
              isEmpty={Object.keys(deckCards).length === 0}
              onSave={handleSave}
              onSaveAs={handleSaveAs}
              onDelete={handleDelete}
            />
          </aside>
        </>
      ) : (
        <main className={styles.centerDeck}>
          <div className={styles.deckHeaderInline}>
            <input
              className={styles.nameInput}
              value={deckName}
              onChange={(e) => setDeckName(e.target.value)}
              placeholder="Deck name"
            />
            <DeckFormatPicker
              activeFormat={activeFormat}
              onChange={setActiveFormat}
            />
            <div className={styles.deckHeaderInlineSpacer} />
            <span className={styles.deckHeaderInlineCount}>
              <span>{totalCards} cards</span>
              <span className={statusClass(validation, totalCards)}>
                {statusLabel(validation, totalCards)}
              </span>
            </span>
          </div>

          <AddCardSearch
            catalog={catalog}
            deckCards={deckCards}
            isCommanderFormat={isCommanderFormat}
            onAdd={addCard}
            onSuggestBasics={() =>
              suggestLandsForDeck(deckCards, catalogIndex, catalog, setCardCount)
            }
          />

          <DeckCentricView
            deckCards={deckCards}
            catalog={catalogIndex}
            activeFormat={activeFormat}
            onAdd={addCard}
            onRemove={removeCard}
            commander={commander}
            showCommanderControls={isCommanderFormat}
            onToggleCommander={(name) =>
              setCommander((prev) => (prev === name ? null : name))
            }
            rowViolations={rowViolations}
            isCommanderFormat={isCommanderFormat}
            onHoverEnter={handleDeckHoverEnter}
            onHoverLeave={handleDeckHoverLeave}
          />

          <DeckCentricFooter
            validation={validation}
            totalCards={totalCards}
            stats={stats}
            activeDeckId={activeDeckId}
            isEmpty={Object.keys(deckCards).length === 0}
            onSave={handleSave}
            onSaveAs={handleSaveAs}
            onDelete={handleDelete}
          />
        </main>
      )}
    </div>
  )
}

// ---------------------------------------------------------------------------
// Shared deck summary + action row — used by both view modes so the buttons
// and stats panels stay in lockstep regardless of where they're rendered.
// ---------------------------------------------------------------------------

function DeckSummary({
  validation,
  totalCards,
  stats,
}: {
  validation: ValidationResult | null
  totalCards: number
  stats: ReturnType<typeof computeStats>
}) {
  return (
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
          <ManaCurveBars curve={stats.curve} curveByColor={stats.curveByColor} />
        </>
      )}
    </div>
  )
}

function DeckActionRow({
  activeDeckId,
  isEmpty,
  onSave,
  onSaveAs,
  onDelete,
}: {
  activeDeckId: string | null
  isEmpty: boolean
  onSave: () => void
  onSaveAs: () => void
  onDelete: () => void
}) {
  return (
    <div className={styles.actionRow}>
      <button className={styles.primaryButton} onClick={onSave} disabled={isEmpty}>
        {activeDeckId ? 'Save' : 'Save deck'}
      </button>
      <button className={styles.secondaryButton} onClick={onSaveAs} disabled={isEmpty}>
        Save as
      </button>
      <button className={styles.dangerButton} onClick={onDelete} disabled={!activeDeckId}>
        Delete
      </button>
    </div>
  )
}

/**
 * Single horizontal footer strip used in deck-mode (Moxfield-style). Lays out
 * deck size, legality, color pips, the colored mana curve, and the action
 * buttons in one tightly-packed row instead of stacking the summary block above
 * the actions like the right-rail layout does. Cards-mode keeps the stacked
 * `DeckSummary` + `DeckActionRow` because the right rail is too narrow for a
 * horizontal layout.
 */
function DeckCentricFooter({
  validation,
  totalCards,
  stats,
  activeDeckId,
  isEmpty,
  onSave,
  onSaveAs,
  onDelete,
}: {
  validation: ValidationResult | null
  totalCards: number
  stats: ReturnType<typeof computeStats>
  activeDeckId: string | null
  isEmpty: boolean
  onSave: () => void
  onSaveAs: () => void
  onDelete: () => void
}) {
  const showStats = totalCards > 0 && stats.colorCounts.length > 0
  const errors = validation?.errors ?? []
  const errorTooltip = errors.length > 0 ? errors.map((e) => `• ${e.message}`).join('\n') : undefined
  return (
    <div className={styles.deckCentricFooter}>
      <div className={styles.footerMeta}>
        <div className={styles.footerCount}>
          <span className={styles.footerCountNum}>{totalCards}</span>
          <span className={styles.footerCountLabel}>cards</span>
        </div>
        <span
          className={`${styles.footerStatus} ${statusClass(validation, totalCards)}`}
          title={errorTooltip}
        >
          {statusLabel(validation, totalCards)}
        </span>
        {showStats && (
          <div className={styles.footerColorPips}>
            {stats.colorCounts.map(([color, n]) => (
              <span key={color} className={styles.footerColorPip}>
                <span
                  className={styles.colorDot}
                  style={{ background: COLOR_DOT[color] ?? '#888' }}
                />
                {n}
              </span>
            ))}
          </div>
        )}
      </div>

      {showStats && (
        <div className={styles.footerCurve}>
          <ManaCurveBars curve={stats.curve} curveByColor={stats.curveByColor} />
        </div>
      )}

      <div className={styles.footerActions}>
        <button className={styles.primaryButton} onClick={onSave} disabled={isEmpty}>
          {activeDeckId ? 'Save' : 'Save deck'}
        </button>
        <button className={styles.secondaryButton} onClick={onSaveAs} disabled={isEmpty}>
          Save as
        </button>
        <button className={styles.dangerButton} onClick={onDelete} disabled={!activeDeckId}>
          Delete
        </button>
      </div>
    </div>
  )
}

// ---------------------------------------------------------------------------
// Import-deck modal — accepts plain text, MTG Arena, and Moxfield formats.
// ---------------------------------------------------------------------------

const IMPORT_PLACEHOLDER = `Deck
4 Lightning Bolt (LEA) 161
2 Counterspell
20 Mountain

Sideboard
2 Disenchant`

function ImportDeckModal({
  catalog,
  hasExisting,
  onCancel,
  onImport,
}: {
  catalog: CardSummary[]
  hasExisting: boolean
  onCancel: () => void
  onImport: (
    cards: Record<string, number>,
    suggestedName: string | null,
    commander: string | null,
  ) => void
}) {
  const [text, setText] = useState('')

  // Re-parse on every keystroke. The catalog is fixed for the modal lifetime,
  // so only the text input drives the preview. Commander entries are merged
  // into the main entries so they show up in the catalogue resolution and
  // land in the imported deck list (the commander itself is also a card in
  // the deck — see designation logic below).
  const preview = useMemo(() => {
    if (text.trim() === '') return null
    const parsed = parseArenaDeckList(text)
    const resolved = resolveAgainstCatalog(
      [...parsed.commander, ...parsed.entries],
      catalog,
    )
    return { parsed, resolved }
  }, [text, catalog])

  const canImport = !!preview && preview.resolved.totalCards > 0

  const handleConfirm = () => {
    if (!preview || !canImport) return
    if (
      hasExisting &&
      !window.confirm('Replace your current deck contents with the imported list?')
    ) {
      return
    }
    // Merge implemented cards with unimplemented ones so the imported deck
    // reflects the full intended list. Unknown cards render as placeholder
    // rows in the deck list and are flagged by the validator.
    const merged = { ...preview.resolved.deckCards, ...preview.resolved.unmatchedCards }
    // First entry under a `Commander` header becomes the designation. Use the
    // resolved/canonical card name when we matched it (so casing matches the
    // catalogue); fall back to the raw name otherwise.
    const commanderEntry = preview.parsed.commander[0] ?? null
    const commanderName = commanderEntry
      ? catalog.find((c) => c.name.toLowerCase() === commanderEntry.name.toLowerCase())?.name
        ?? commanderEntry.name
      : null
    onImport(merged, null, commanderName)
  }

  return (
    <>
      <div className={styles.importBackdrop} onClick={onCancel} />
      <div className={styles.importDialog} role="dialog" aria-label="Import deck">
        <div className={styles.importHeader}>
          <strong>Import deck</strong>
          <button className={styles.linkButton} onClick={onCancel} type="button">
            Close
          </button>
        </div>
        <p className={styles.importHint}>
          Paste a deck list in <strong>plain text</strong>, <strong>MTG Arena</strong>, or{' '}
          <strong>Moxfield</strong> format — the parser detects all three. Each line is{' '}
          <code>count name</code>, optionally followed by <code>(SET) NUM</code> and Moxfield
          decorations (<code>*F*</code>, <code>*A*</code>, <code>#tag</code>). Section headers like{' '}
          <code>Deck</code>, <code>Sideboard</code>, <code>Commander</code> are recognised; lines
          starting with <code>//</code> or <code>#</code> are comments.
        </p>
        <textarea
          className={styles.importTextarea}
          value={text}
          onChange={(e) => setText(e.target.value)}
          placeholder={IMPORT_PLACEHOLDER}
          spellCheck={false}
          autoFocus
        />
        {preview && <ImportPreview preview={preview} />}
        <div className={styles.importActions}>
          <button className={styles.secondaryButton} onClick={onCancel} type="button">
            Cancel
          </button>
          <button
            className={styles.primaryButton}
            onClick={handleConfirm}
            disabled={!canImport}
            type="button"
          >
            Import
            {preview ? ` (${preview.resolved.totalCards} cards)` : ''}
          </button>
        </div>
      </div>
    </>
  )
}

function ImportPreview({
  preview,
}: {
  preview: {
    parsed: ReturnType<typeof parseArenaDeckList>
    resolved: ResolveResult
  }
}) {
  const { parsed, resolved } = preview
  const issueCount = parsed.errors.length + resolved.unmatched.length + resolved.truncated.length
  return (
    <div className={styles.importPreview}>
      <div className={styles.importSummary}>
        <span>
          <strong>{resolved.matchedCards}</strong> matched
          {resolved.totalCards !== resolved.matchedCards
            ? ` of ${resolved.totalCards} (${resolved.totalCards - resolved.matchedCards} placeholder)`
            : ''}
        </span>
        {parsed.sideboard.length > 0 && (
          <span className={styles.importMutedBadge}>
            sideboard ignored ({parsed.sideboard.reduce((a, e) => a + e.count, 0)})
          </span>
        )}
        {issueCount > 0 && <span className={styles.importBadBadge}>{issueCount} issue{issueCount === 1 ? '' : 's'}</span>}
      </div>

      {resolved.unmatched.length > 0 && (
        <details className={styles.importDetails} open>
          <summary>
            Not implemented yet ({resolved.unmatched.length}) — imported as placeholders
          </summary>
          <ul>
            {resolved.unmatched.map((u) => (
              <li key={`${u.entry.line}-${u.entry.raw}`}>
                Line {u.entry.line}: <code>{u.entry.raw}</code>
              </li>
            ))}
          </ul>
        </details>
      )}

      {parsed.errors.length > 0 && (
        <details className={styles.importDetails}>
          <summary>Unparseable lines ({parsed.errors.length})</summary>
          <ul>
            {parsed.errors.map((e) => (
              <li key={`${e.line}-${e.raw}`}>
                Line {e.line}: <code>{e.raw}</code> — {e.reason}
              </li>
            ))}
          </ul>
        </details>
      )}

      {resolved.truncated.length > 0 && (
        <details className={styles.importDetails}>
          <summary>Capped to 4 copies ({resolved.truncated.length})</summary>
          <ul>
            {resolved.truncated.map((t) => (
              <li key={t.name}>
                {t.name}: requested {t.requested}, kept {t.capped}
              </li>
            ))}
          </ul>
        </details>
      )}
    </div>
  )
}

// ---------------------------------------------------------------------------
// Bulk-edit modal — editable MTG Arena text view of the current deck.
// Doubles as an export modal: copy-to-clipboard works whether the user has
// edited the text or not.
// ---------------------------------------------------------------------------

type ExportFormat = 'plain' | 'arena' | 'moxfield'

const EXPORT_FORMATS: Array<{ value: ExportFormat; label: string; hint: string }> = [
  { value: 'plain', label: 'Plain text', hint: 'Just count and name — most compatible.' },
  { value: 'arena', label: 'MTG Arena', hint: 'Includes (SET) and collector number for Arena import.' },
  { value: 'moxfield', label: 'Moxfield', hint: 'Same as Arena. Foil/alter/tag flags are preserved on import but not emitted (we don’t track them).' },
]

function BulkEditDeckModal({
  deckCards,
  commander,
  catalog,
  catalogIndex,
  onClose,
  onApply,
}: {
  deckCards: Record<string, number>
  commander: string | null
  catalog: CardSummary[]
  catalogIndex: Record<string, CardSummary>
  onClose: () => void
  onApply: (cards: Record<string, number>, commander: string | null) => void
}) {
  const [format, setFormat] = useState<ExportFormat>('arena')

  const rendered = useMemo(
    () => formatDeck(deckCards, catalogIndex, format, commander),
    [deckCards, catalogIndex, format, commander]
  )
  const [text, setText] = useState(rendered.text)
  const [copied, setCopied] = useState(false)

  // Parse-and-resolve mirrors the import flow so both modals stay consistent.
  // The parser is format-agnostic and accepts plain / Arena / Moxfield input.
  // Commander entries are merged into the resolved set so the user sees them
  // in the matched/unmatched preview just like main-deck cards.
  const preview = useMemo(() => {
    if (text.trim() === '') return null
    const parsed = parseArenaDeckList(text)
    const resolved = resolveAgainstCatalog(
      [...parsed.commander, ...parsed.entries],
      catalog,
    )
    return { parsed, resolved }
  }, [text, catalog])

  const dirty = text !== rendered.text
  const canApply = !!preview && preview.resolved.totalCards > 0

  const handleFormatChange = (next: ExportFormat) => {
    if (next === format) return
    if (
      dirty &&
      !window.confirm('Switching format will discard your edits. Continue?')
    ) {
      return
    }
    setFormat(next)
    const re = formatDeck(deckCards, catalogIndex, next, commander)
    setText(re.text)
  }

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(text)
      setCopied(true)
      window.setTimeout(() => setCopied(false), 1500)
    } catch {
      // Clipboard blocked — user can still select and copy by hand.
    }
  }

  const handleApply = () => {
    if (!preview || !canApply) return
    if (!window.confirm('Replace your current deck contents with this list?')) return
    const merged = { ...preview.resolved.deckCards, ...preview.resolved.unmatchedCards }
    const commanderEntry = preview.parsed.commander[0] ?? null
    const nextCommander = commanderEntry
      ? catalog.find((c) => c.name.toLowerCase() === commanderEntry.name.toLowerCase())?.name
        ?? commanderEntry.name
      : null
    onApply(merged, nextCommander)
  }

  const formatHint = EXPORT_FORMATS.find((f) => f.value === format)?.hint ?? ''

  return (
    <>
      <div className={styles.importBackdrop} onClick={onClose} />
      <div className={styles.importDialog} role="dialog" aria-label="Bulk edit / export deck">
        <div className={styles.importHeader}>
          <strong>Bulk edit / export deck</strong>
          <button className={styles.linkButton} onClick={onClose} type="button">
            Close
          </button>
        </div>
        <div className={styles.formatSwitcher}>
          <span className={styles.formatSwitcherLabel}>Format</span>
          {EXPORT_FORMATS.map((f) => (
            <button
              key={f.value}
              type="button"
              className={`${styles.formatSwitcherChip} ${
                f.value === format ? styles.formatSwitcherChipActive : ''
              }`}
              onClick={() => handleFormatChange(f.value)}
              title={f.hint}
            >
              {f.label}
            </button>
          ))}
        </div>
        <p className={styles.importHint}>
          {formatHint} Paste lists from any of the three formats — the parser detects them all.
          Edit the text and click <strong>Save changes</strong> to apply, or <strong>Copy</strong>{' '}
          to export.
        </p>
        <textarea
          className={styles.importTextarea}
          value={text}
          onChange={(e) => setText(e.target.value)}
          spellCheck={false}
          autoFocus
        />
        {preview && <ImportPreview preview={preview} />}
        <div className={styles.importActions}>
          <button className={styles.secondaryButton} onClick={onClose} type="button">
            Cancel
          </button>
          <button
            className={styles.secondaryButton}
            onClick={handleCopy}
            type="button"
          >
            {copied ? 'Copied!' : 'Copy'}
          </button>
          <button
            className={styles.primaryButton}
            onClick={handleApply}
            disabled={!canApply || !dirty}
            type="button"
          >
            Save changes
            {preview && dirty ? ` (${preview.resolved.totalCards} cards)` : ''}
          </button>
        </div>
      </div>
    </>
  )
}

/**
 * Render `deckCards` as MTG Arena deck-list text. Lands are placed in a
 * trailing block. Unknown (not-implemented) cards mix into the spell block
 * since we have no type info; their names are emitted plain so the text
 * round-trips through `parseArenaDeckList` without confusing the parser.
 */
/**
 * Render `deckCards` as deck-list text in one of three formats:
 *   - `plain`:    `<count> <name>` only (most permissive importers).
 *   - `arena`:    Adds `(SET) <collector>` when known. Uses `Deck` header.
 *   - `moxfield`: Same wire shape as Arena for our subset (we don't track
 *                 foils/alters/tags). Lines round-trip through Moxfield's
 *                 bulk-edit input.
 *
 * The designated commander, if any, is emitted in its own `Commander` section
 * and excluded from the main `Deck` block — that way re-importing produces
 * the same `(deckCards, commander)` pair without double-counting. The parser
 * also recognises plain-text exports without the `Commander` header (it falls
 * through to `Deck`), so any tool downstream can still consume the output.
 *
 * Lands are placed in a trailing block so the spell curve is easy to scan.
 * Unknown (not-implemented) cards mix into the spell block since we have no
 * type info; their names are emitted plain so the text round-trips through
 * the parser without confusion.
 */
function formatDeck(
  deck: Record<string, number>,
  catalog: Record<string, CardSummary>,
  format: ExportFormat,
  commander: string | null = null,
): { text: string; totalCards: number; unknownCards: number } {
  type Entry = { name: string; count: number; card: CardSummary | undefined }
  const spells: Entry[] = []
  const lands: Entry[] = []
  let commanderEntry: Entry | null = null
  let totalCards = 0
  let unknownCards = 0
  for (const [name, count] of Object.entries(deck)) {
    if (count <= 0) continue
    totalCards += count
    const card = catalog[name]
    if (!card) unknownCards += count
    const entry: Entry = { name, count, card }
    if (commander && name === commander) {
      // Lift the commander out of the main block so re-import doesn't double
      // it. We still keep its full count (typically 1) in the Commander
      // section — partner pairs are uncommon enough that we render whatever
      // count is in the deck rather than special-casing.
      commanderEntry = entry
      continue
    }
    if (card?.cardTypes.includes('LAND')) lands.push(entry)
    else spells.push(entry)
  }
  spells.sort((a, b) => a.name.localeCompare(b.name))
  lands.sort((a, b) => a.name.localeCompare(b.name))

  const includePrinting = format !== 'plain'
  const renderLine = (e: Entry): string => {
    const base = `${e.count} ${e.name}`
    if (!includePrinting || !e.card?.setCode) return base
    const set = `(${e.card.setCode.toUpperCase()})`
    return e.card.collectorNumber
      ? `${base} ${set} ${e.card.collectorNumber}`
      : `${base} ${set}`
  }

  const lines: string[] = []
  if (commanderEntry) {
    lines.push('Commander')
    lines.push(renderLine(commanderEntry))
    lines.push('')
  }
  // Plain text often omits headers; include `Deck` only for Arena/Moxfield
  // where the section marker is the convention. Either way the parser
  // accepts both shapes.
  if (format !== 'plain') lines.push('Deck')
  for (const e of spells) lines.push(renderLine(e))
  if (lands.length > 0) {
    if (lines.length > 0 && lines[lines.length - 1] !== '') lines.push('')
    for (const e of lands) lines.push(renderLine(e))
  }
  return { text: lines.join('\n') + '\n', totalCards, unknownCards }
}

// ---------------------------------------------------------------------------
// Left rail sections
// ---------------------------------------------------------------------------

function SavedDecksSummary({
  decks,
  activeDeckId,
  onOpen,
}: {
  decks: SavedDeck[]
  activeDeckId: string | null
  onOpen: () => void
}) {
  const active = useMemo(() => decks.find((d) => d.id === activeDeckId) ?? null, [decks, activeDeckId])
  const legalityInput = useMemo(
    () => (active ? { [active.id]: active.cards } : {}),
    [active]
  )
  const legalityMap = useDeckLegalFormats(legalityInput)
  const activeFormats = active ? (legalityMap[active.id] ?? []) : []
  return (
    <section className={styles.section}>
      <h2 className={styles.sectionLabel}>My decks</h2>
      <div className={styles.savedSummary}>
        <div className={styles.savedSummaryActive}>
          {active ? (
            <>
              <span className={styles.savedSummaryLabel}>Editing</span>
              <span className={styles.savedSummaryName}>{active.name}</span>
              {activeFormats.length > 0 && (
                <FormatLegalityBadges formats={activeFormats} />
              )}
            </>
          ) : (
            <>
              <span className={styles.savedSummaryLabel}>Editing</span>
              <span className={styles.savedSummaryNameMuted}>Unsaved deck</span>
            </>
          )}
        </div>
        <button
          className={styles.savedBrowseButton}
          onClick={onOpen}
          type="button"
          disabled={decks.length === 0}
          title={decks.length === 0 ? 'No saved decks yet' : 'Browse saved decks'}
        >
          {decks.length === 0
            ? 'No saved decks yet'
            : `Browse decks (${decks.length}) →`}
        </button>
      </div>
    </section>
  )
}

/**
 * Renders the formats a saved deck is legal in as small pill badges. Empty list = the deck has
 * cards with unknown legality (test/custom cards) or no constructed format admits it; in both
 * cases we render nothing so the saved-deck row stays compact.
 */
function FormatLegalityBadges({ formats }: { formats: string[] }) {
  if (formats.length === 0) return null
  // Order matches FORMAT_TOKENS so the badges always read in the same sequence.
  const order = new Map(FORMAT_TOKENS.map((f, i) => [f.value.toUpperCase(), i]))
  const sorted = [...formats].sort(
    (a, b) => (order.get(a) ?? 99) - (order.get(b) ?? 99)
  )
  return (
    <span className={styles.formatBadges}>
      {sorted.map((f) => (
        <span key={f} className={styles.formatBadge} title={`Legal in ${labelForFormat(f)}`}>
          {labelForFormat(f)}
        </span>
      ))}
    </span>
  )
}


type DecksBrowserSort = 'updated' | 'name' | 'size' | 'colors'

function SavedDecksBrowser({
  decks,
  catalog,
  activeDeckId,
  onClose,
  onLoad,
  onRename,
  onDelete,
}: {
  decks: SavedDeck[]
  catalog: Record<string, CardSummary>
  activeDeckId: string | null
  onClose: () => void
  onLoad: (d: SavedDeck) => void
  onRename: (d: SavedDeck) => void
  onDelete: (d: SavedDeck) => void
}) {
  const [filter, setFilter] = useState('')
  const [sort, setSort] = useState<DecksBrowserSort>('updated')
  const [colorFilter, setColorFilter] = useState<Set<string>>(new Set())

  // Close on Escape.
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [onClose])

  // Server-authoritative legality map (deckId → format names). The hook batches all decks
  // into one POST and re-uses cache for unchanged decks. Commander is folded into the
  // card map so the count-based legality checks (e.g. exactly 100 for Commander) see the
  // full deck — saved-deck storage keeps it separate per `SavedDeck.commander`.
  const legalityInput = useMemo(() => {
    const out: Record<string, Record<string, number>> = {}
    for (const d of decks) {
      out[d.id] = mergeCommanderIntoCards(d.cards, d.commander ?? null)
    }
    return out
  }, [decks])
  const legalityMap = useDeckLegalFormats(legalityInput)

  // Pre-compute per-deck metadata once. Doing this up front keeps sort/filter
  // O(n) for hundreds of decks even when the user types fast. Card totals and
  // colour pips include the commander so the user-visible numbers match what
  // they'd actually play with.
  const enriched = useMemo(
    () =>
      decks.map((d) => {
        const fullCards = mergeCommanderIntoCards(d.cards, d.commander ?? null)
        const total = Object.values(fullCards).reduce((a, b) => a + b, 0)
        const colors = deckColors(fullCards, catalog)
        const legalFormats = legalityMap[d.id] ?? []
        return { deck: d, total, colors, legalFormats }
      }),
    [decks, catalog, legalityMap]
  )

  const filtered = useMemo(() => {
    const f = filter.trim().toLowerCase()
    let out = enriched
    if (f) out = out.filter((e) => e.deck.name.toLowerCase().includes(f))
    if (colorFilter.size > 0) {
      out = out.filter((e) => {
        const has = (k: string) =>
          k === 'COLORLESS' ? e.colors.length === 0 : e.colors.includes(k)
        // OR semantics: keep decks that match any selected colour bucket.
        for (const k of colorFilter) if (has(k)) return true
        return false
      })
    }
    return [...out].sort((a, b) => {
      switch (sort) {
        case 'name':
          return a.deck.name.localeCompare(b.deck.name)
        case 'size':
          return b.total - a.total || a.deck.name.localeCompare(b.deck.name)
        case 'colors':
          return colorBucketKey(a.colors) - colorBucketKey(b.colors)
            || a.deck.name.localeCompare(b.deck.name)
        case 'updated':
        default:
          return b.deck.updatedAt - a.deck.updatedAt
      }
    })
  }, [enriched, filter, colorFilter, sort])

  const toggleColor = (key: string) => {
    setColorFilter((prev) => {
      const next = new Set(prev)
      if (next.has(key)) next.delete(key)
      else next.add(key)
      return next
    })
  }

  return (
    <>
      <div className={styles.browserBackdrop} onClick={onClose} />
      <div className={styles.browserDialog} role="dialog" aria-label="Saved decks">
        <header className={styles.browserHeader}>
          <div>
            <strong className={styles.browserTitle}>Saved decks</strong>
            <span className={styles.browserSubtitle}>
              {filtered.length === decks.length
                ? `${decks.length} deck${decks.length === 1 ? '' : 's'}`
                : `${filtered.length} of ${decks.length}`}
            </span>
          </div>
          <button className={styles.linkButton} onClick={onClose} type="button">
            Close (Esc)
          </button>
        </header>

        <div className={styles.browserToolbar}>
          <input
            className={styles.browserSearch}
            value={filter}
            onChange={(e) => setFilter(e.target.value)}
            placeholder="Search decks by name…"
            autoFocus
          />
          <select
            className={styles.sortSelect}
            value={sort}
            onChange={(e) => setSort(e.target.value as DecksBrowserSort)}
            aria-label="Sort decks"
          >
            <option value="updated">Recently updated</option>
            <option value="name">Name (A→Z)</option>
            <option value="size">Card count</option>
            <option value="colors">Colour</option>
          </select>
          <div className={styles.browserColorChips} role="group" aria-label="Filter by colour">
            {COLOR_TOKENS.map(({ label, key }) => {
              const active = colorFilter.has(key)
              return (
                <button
                  key={key}
                  className={`${styles.chip} ${styles.chipMana} ${active ? styles.chipActive : ''}`}
                  onClick={() => toggleColor(key)}
                  type="button"
                  aria-pressed={active}
                  aria-label={label}
                  title={key.toLowerCase()}
                >
                  <ManaSymbol symbol={label} size={18} />
                </button>
              )
            })}
            <button
              className={`${styles.chip} ${styles.chipMana} ${colorFilter.has('COLORLESS') ? styles.chipActive : ''}`}
              onClick={() => toggleColor('COLORLESS')}
              type="button"
              aria-pressed={colorFilter.has('COLORLESS')}
              aria-label="Colourless"
              title="colourless"
            >
              <ManaSymbol symbol="C" size={18} />
            </button>
            {/* Always rendered (with `visibility: hidden` when inactive) so toggling a
             * colour doesn't widen the chip group and shift the chips leftwards. */}
            <button
              className={`${styles.linkButton} ${styles.colorChipsClear}`}
              onClick={() => setColorFilter(new Set())}
              type="button"
              aria-hidden={colorFilter.size === 0}
              tabIndex={colorFilter.size === 0 ? -1 : 0}
              style={colorFilter.size === 0 ? { visibility: 'hidden' } : undefined}
            >
              clear
            </button>
          </div>
        </div>

        <div className={styles.browserGrid}>
          {filtered.length === 0 ? (
            <div className={styles.savedEmpty}>
              {decks.length === 0
                ? 'No saved decks yet. Build one and click Save.'
                : 'No decks match the current filters.'}
            </div>
          ) : (
            filtered.map(({ deck, total, colors, legalFormats }) => (
              <DeckCard
                key={deck.id}
                deck={deck}
                total={total}
                colors={colors}
                legalFormats={legalFormats}
                isActive={deck.id === activeDeckId}
                onLoad={onLoad}
                onRename={onRename}
                onDelete={onDelete}
              />
            ))
          )}
        </div>
      </div>
    </>
  )
}

function DeckCard({
  deck,
  total,
  colors,
  legalFormats,
  isActive,
  onLoad,
  onRename,
  onDelete,
}: {
  deck: SavedDeck
  total: number
  colors: string[]
  legalFormats: string[]
  isActive: boolean
  onLoad: (d: SavedDeck) => void
  onRename: (d: SavedDeck) => void
  onDelete: (d: SavedDeck) => void
}) {
  const updated = useMemo(() => formatRelativeTime(deck.updatedAt), [deck.updatedAt])
  // Banner gradient derived from the deck's colour identity. Falls back to a
  // neutral gradient for colourless / empty decks.
  const banner = useMemo(() => deckBannerGradient(colors), [colors])

  return (
    <button
      type="button"
      className={`${styles.deckCard} ${isActive ? styles.deckCardActive : ''}`}
      onClick={() => onLoad(deck)}
      title={`Load ${deck.name}`}
    >
      <div className={styles.deckCardBanner} style={{ background: banner }}>
        {colors.length > 0 ? (
          <div className={styles.deckCardColors}>
            {colors.map((c) => (
              <span
                key={c}
                className={styles.deckCardColorDot}
                style={{ background: COLOR_DOT[c] ?? '#888' }}
                title={c}
              />
            ))}
          </div>
        ) : (
          <span className={styles.deckCardColorless}>colourless</span>
        )}
        <span className={styles.deckCardCount}>{total}</span>
      </div>
      <div className={styles.deckCardBody}>
        <div className={styles.deckCardName}>{deck.name}</div>
        <div className={styles.deckCardMeta}>
          <span>{updated}</span>
        </div>
        {(deck.format || legalFormats.length > 0) && (
          <span className={styles.formatBadges}>
            {deck.format && (
              <span
                className={styles.formatBadgeSaved}
                title={`Saved as ${labelForFormat(deck.format)}`}
              >
                {labelForFormat(deck.format)}
              </span>
            )}
            {legalFormats
              .filter((f) => f !== deck.format)
              .map((f) => (
                <span
                  key={f}
                  className={styles.formatBadge}
                  title={`Legal in ${labelForFormat(f)}`}
                >
                  {labelForFormat(f)}
                </span>
              ))}
          </span>
        )}
      </div>
      <div className={styles.deckCardActions} onClick={(e) => e.stopPropagation()}>
        <button
          className={styles.savedIconButton}
          onClick={() => onRename(deck)}
          type="button"
          title="Rename"
          aria-label={`Rename ${deck.name}`}
        >
          ✎
        </button>
        <button
          className={styles.savedIconButtonDanger}
          onClick={() => onDelete(deck)}
          type="button"
          title="Delete"
          aria-label={`Delete ${deck.name}`}
        >
          ✕
        </button>
      </div>
      {isActive && <span className={styles.deckCardActiveBadge}>Editing</span>}
    </button>
  )
}

function deckBannerGradient(colors: string[]): string {
  if (colors.length === 0) {
    return 'linear-gradient(135deg, rgba(120,120,140,0.5), rgba(60,60,80,0.6))'
  }
  if (colors.length === 1) {
    const c = COLOR_DOT[colors[0]!] ?? '#888'
    return `linear-gradient(135deg, ${c}aa, ${c}55)`
  }
  const stops = colors.map((c, i) => {
    const hex = COLOR_DOT[c] ?? '#888'
    return `${hex} ${Math.round((i / (colors.length - 1)) * 100)}%`
  })
  return `linear-gradient(135deg, ${stops.join(', ')})`
}

function colorBucketKey(colors: string[]): number {
  if (colors.length === 0) return 99
  if (colors.length > 1) return 90 + colors.length
  switch (colors[0]) {
    case 'WHITE': return 0
    case 'BLUE': return 1
    case 'BLACK': return 2
    case 'RED': return 3
    case 'GREEN': return 4
    default: return 5
  }
}

function deckColors(
  cards: Record<string, number>,
  catalog: Record<string, CardSummary>
): string[] {
  const counts: Record<string, number> = {}
  for (const [name, n] of Object.entries(cards)) {
    if (n <= 0) continue
    const c = catalog[name.split('#')[0] ?? name]
    if (!c) continue
    for (const col of c.colors) counts[col] = (counts[col] ?? 0) + n
  }
  return Object.entries(counts)
    .sort((a, b) => b[1] - a[1])
    .map(([color]) => color)
}

function formatRelativeTime(ts: number): string {
  const now = Date.now()
  const diff = Math.max(0, now - ts)
  const sec = Math.floor(diff / 1000)
  if (sec < 60) return 'just now'
  const min = Math.floor(sec / 60)
  if (min < 60) return `${min}m ago`
  const hr = Math.floor(min / 60)
  if (hr < 24) return `${hr}h ago`
  const day = Math.floor(hr / 24)
  if (day < 7) return `${day}d ago`
  const wk = Math.floor(day / 7)
  if (wk < 5) return `${wk}w ago`
  const mo = Math.floor(day / 30)
  if (mo < 12) return `${mo}mo ago`
  const yr = Math.floor(day / 365)
  return `${yr}y ago`
}

function SearchBar({
  query,
  onQueryChange,
  sortMode,
  onSortChange,
  resultLabel,
  errors,
}: {
  query: string
  onQueryChange: (next: string) => void
  sortMode: SortMode
  onSortChange: (m: SortMode) => void
  resultLabel: string
  errors: ParseError[]
}) {
  const [helpOpen, setHelpOpen] = useState(false)
  const hasErrors = errors.length > 0
  // Local mirror of the URL-backed `query` so the input never has its DOM value reassigned by
  // the round-trip through `useSearchParams`. Without this, typing certain characters (e.g.
  // parentheses) caused the cursor to jump because React would briefly rerender the input
  // with the previous-tick query while the URL update was in flight, and the browser
  // re-anchored the caret.
  const [localQuery, setLocalQuery] = useState(query)
  useEffect(() => {
    setLocalQuery((prev) => (prev === query ? prev : query))
  }, [query])
  return (
    <div className={styles.searchBar}>
      <div className={styles.searchInputWrap}>
        <input
          className={hasErrors ? styles.searchInputError : styles.searchInput}
          placeholder='Search — try: t:creature c:r cmc<=3, o:flying, (c:u or c:b) -is:legendary'
          value={localQuery}
          onChange={(e) => {
            setLocalQuery(e.target.value)
            onQueryChange(e.target.value)
          }}
          aria-invalid={hasErrors}
          aria-describedby={hasErrors ? 'search-errors' : undefined}
        />
        {hasErrors && (
          <ul id="search-errors" className={styles.searchErrors} role="alert">
            {errors.map((e, i) => (
              <li key={i}>
                <code>{query.slice(e.span.start, e.span.end) || '·'}</code>
                <span>{e.message}{e.suggestion ? ` ${e.suggestion}` : ''}</span>
              </li>
            ))}
          </ul>
        )}
      </div>
      <button
        className={helpOpen ? styles.helpIconActive : styles.helpIcon}
        onClick={() => setHelpOpen((v) => !v)}
        title="Search syntax"
        aria-label="Show search syntax help"
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
      <span className={styles.cardActionHint}>
        <kbd>Click</kbd> add · <kbd>Right-click</kbd> remove
      </span>
      {helpOpen && <SearchHelp onClose={() => setHelpOpen(false)} onInsert={(t) => onQueryChange(t)} />}
    </div>
  )
}

function SearchHelp({ onClose, onInsert }: { onClose: () => void; onInsert: (t: string) => void }) {
  const examples: Array<{ syntax: string; desc: string }> = [
    { syntax: 'lightning', desc: 'name contains "lightning"' },
    { syntax: '!"Lightning Bolt"', desc: 'exact card name' },
    { syntax: 'name:/^bolt/', desc: 'regex on card name (case-insensitive by default)' },
    { syntax: 't:creature', desc: 'type line — AND of words (try t:legendary creature elf)' },
    { syntax: 'o:flying', desc: 'oracle text contains' },
    { syntax: 'c:r', desc: 'colour identity includes red' },
    { syntax: 'c:azorius', desc: 'guild / shard / wedge name (azorius, bant, mardu, …)' },
    { syntax: 'c=wu', desc: 'colours are exactly white + blue' },
    { syntax: 'c<=rw', desc: 'colours are a subset of red / white' },
    { syntax: 'c>=2', desc: 'colour count comparison' },
    { syntax: 'c:colorless', desc: 'no colours' },
    { syntax: 'mana:{2}{u}{u}', desc: 'mana cost symbols (multiset compare)' },
    { syntax: 'mana>={r}{r}', desc: 'cost contains at least these symbols' },
    { syntax: 'cmc:3', desc: 'mana value (also <=, >=, <, >, !=)' },
    { syntax: 'pow>tou', desc: 'cross-field compare (power vs. toughness)' },
    { syntax: 'pow>=4', desc: 'power (numeric only)' },
    { syntax: 'r:rare', desc: 'rarity (common / uncommon / rare / mythic)' },
    { syntax: 's:blb', desc: 'set code' },
    { syntax: 'f:standard', desc: 'format legality' },
    { syntax: 'is:legendary', desc: 'land/creature/spell/permanent/legendary/basic/dfc/vanilla/bear/historic' },
    { syntax: 'layout:transform', desc: 'card layout (transform / mdfc / normal)' },
    { syntax: 'kw:flying', desc: 'keyword' },
    { syntax: '-t:creature', desc: 'negate any term (also: not t:creature)' },
    { syntax: 't:creature or t:planeswalker', desc: 'boolean OR' },
    { syntax: '(c:u or c:b) t:creature', desc: 'grouping with parens' },
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
          Tokens combine with implicit AND. Use <code>or</code> for alternation,
          parentheses for grouping, and <code>-</code> or <code>not</code> for negation.
          Click an example to drop it into the search box.
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
  setNames,
  advanced,
}: {
  query: string
  onQueryChange: (next: string) => void
  catalog: CardSummary[]
  setNames: Record<string, string>
  /**
   * `true` when the query uses or / parens — features the flat chip helpers can't
   * round-trip without rewriting expressions the user authored. We surface a hint
   * banner and freeze chip toggles instead. The active-state detection still runs
   * (chips stay visually inert because hasToken is whole-term and won't match
   * anything inside parens), so the panel reads as "snapshot" rather than "stale".
   */
  advanced: boolean
}) {
  const toggle = (token: string) => {
    if (advanced) return
    onQueryChange(toggleToken(query, token))
  }

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

  // Color mode is derived from the query so a directly typed `c=wu` immediately
  // re-selects the "Exactly" segment without a separate state to drift.
  const colorOp = useMemo(() => detectColorOp(query), [query])
  const colorLetters = useMemo(() => collectColorLetters(query), [query])

  const toggleColorLetter = (letter: string) => {
    const next = new Set(colorLetters)
    if (next.has(letter)) next.delete(letter)
    else next.add(letter)
    onQueryChange(rewriteColorTokens(query, colorOp, next))
  }

  const changeColorMode = (newOp: ColorOp) => {
    onQueryChange(rewriteColorTokens(query, newOp, colorLetters))
  }

  return (
    <fieldset className={styles.filterFieldset} disabled={advanced}>
      {advanced && (
        <div className={styles.advancedBanner} role="status">
          Advanced query — chips disabled. Edit the search bar directly.
        </div>
      )}
      <section className={styles.section}>
        <div className={styles.sectionHeader}>
          <h2 className={styles.sectionLabel}>Colour</h2>
          <ColorModeSegmented op={colorOp} onChange={changeColorMode} />
        </div>
        <div className={styles.filterRow}>
          {COLOR_TOKENS.map(({ letter, label }) => {
            const active = colorLetters.has(letter)
            return (
              <button
                key={letter}
                className={`${styles.chip} ${styles.chipMana} ${active ? styles.chipActive : ''}`}
                onClick={() => toggleColorLetter(letter)}
                type="button"
                aria-label={label}
                title={label}
              >
                <ManaSymbol symbol={label} size={16} />
              </button>
            )
          })}
          <button
            className={`${styles.chip} ${styles.chipMana} ${hasToken(query, 'is:colorless') ? styles.chipActive : ''}`}
            onClick={() => toggle('is:colorless')}
            type="button"
            aria-label="Colourless"
            title="Colourless cards"
          >
            <ManaSymbol symbol="C" size={16} />
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
          {(() => {
            // Legendary lives next to the type chips because it's the most common supertype
            // filter players reach for (commander eligibility, "your commander said legendary
            // matters", etc.). `is:legendary` resolves via supertypes — distinct from `t:` which
            // matches across cardTypes/supertypes/subtypes by substring. The crown glyph (♛)
            // is the same one the deck-row commander toggle uses, so the visual cue carries
            // across the deckbuilder.
            const token = 'is:legendary'
            const active = hasToken(query, token)
            return (
              <button
                key="legendary"
                className={`${styles.chip} ${active ? styles.chipActive : ''}`}
                onClick={() => toggle(token)}
                type="button"
              >
                <span aria-hidden="true" style={{ marginRight: 4, color: '#d4a017' }}>♛</span>
                Legendary
              </button>
            )
          })()}
        </div>
      </section>

      <SubtypeSection query={query} onQueryChange={onQueryChange} catalog={catalog} />

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
                {setNames[code] ? `${setNames[code]} (${code})` : code}
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
    </fieldset>
  )
}

// ---------------------------------------------------------------------------
// Format selector (right rail) — drives validation, deck-list red-flagging,
// and the catalog's automatic format filter.
// ---------------------------------------------------------------------------

/**
 * Compact format picker for the right rail. Drives the deck-level `fmt` URL param —
 * deliberately separate from the search-bar query so editing the catalog filter never
 * clobbers the user's chosen format. `null` clears the format.
 */
function DeckFormatPicker({
  activeFormat,
  onChange,
}: {
  activeFormat: string | null
  onChange: (value: string | null) => void
}) {
  const value = activeFormat ? activeFormat.toLowerCase() : ''
  return (
    <label className={styles.formatPickerRow} title="Pick a format to validate this deck against and highlight illegal cards.">
      <span className={styles.formatPickerLabel}>Format</span>
      <select
        className={styles.formatPickerSelect}
        value={value}
        onChange={(e) => onChange(e.target.value || null)}
      >
        <option value="">No format</option>
        {FORMAT_TOKENS.map(({ value: v, label }) => (
          <option key={v} value={v}>{label}</option>
        ))}
      </select>
    </label>
  )
}

// ---------------------------------------------------------------------------
// Subtype / tribe filter
// ---------------------------------------------------------------------------

function SubtypeSection({
  query,
  onQueryChange,
  catalog,
}: {
  query: string
  onQueryChange: (next: string) => void
  catalog: CardSummary[]
}) {
  const [input, setInput] = useState('')

  const allSubtypes = useMemo(() => {
    const set = new Set<string>()
    for (const c of catalog) for (const s of c.subtypes) set.add(s)
    return [...set].sort()
  }, [catalog])

  // Active subtype tokens: any t:<X> where X (case-insensitive) is a known
  // subtype. This lets the type-row chips (t:Creature, etc.) coexist without
  // showing as subtype chips here.
  const activeSubtypes = useMemo(() => {
    if (allSubtypes.length === 0) return [] as Array<{ token: string; label: string }>
    const subSet = new Set(allSubtypes.map((s) => s.toLowerCase()))
    const tokens = query.match(/(?:^|\s)(-?t:(?:"[^"]+"|[^\s]+))/g) ?? []
    const out: Array<{ token: string; label: string }> = []
    for (const raw of tokens) {
      const trimmed = raw.trim()
      const m = trimmed.match(/^-?t:"?([^"]+?)"?$/)
      if (m && subSet.has(m[1]!.toLowerCase())) {
        out.push({ token: trimmed, label: m[1]! })
      }
    }
    return out
  }, [query, allSubtypes])

  const addSubtype = (raw: string) => {
    const trimmed = raw.trim()
    if (!trimmed) return
    const canonical = allSubtypes.find((s) => s.toLowerCase() === trimmed.toLowerCase()) ?? trimmed
    const tokenValue = canonical.includes(' ') ? `"${canonical}"` : canonical
    onQueryChange(addToken(query, `t:${tokenValue}`))
    setInput('')
  }

  if (allSubtypes.length === 0) return null

  return (
    <section className={styles.section}>
      <h2 className={styles.sectionLabel}>Subtype / tribe</h2>
      {activeSubtypes.length > 0 && (
        <div className={styles.filterRow}>
          {activeSubtypes.map(({ token, label }) => (
            <button
              key={token}
              className={`${styles.chip} ${styles.chipActive}`}
              onClick={() => onQueryChange(removeToken(query, token))}
              type="button"
              title="Click to remove"
            >
              {label} ✕
            </button>
          ))}
        </div>
      )}
      <input
        list="deckbuilder-subtypes"
        className={styles.textInput}
        value={input}
        onChange={(e) => setInput(e.target.value)}
        onKeyDown={(e) => {
          if (e.key === 'Enter') {
            e.preventDefault()
            addSubtype(input)
          }
        }}
        onBlur={() => {
          // Auto-add on blur if the entry exactly matches a known subtype,
          // so picking from the datalist (which doesn't always fire Enter)
          // still works.
          const exact = allSubtypes.find((s) => s.toLowerCase() === input.trim().toLowerCase())
          if (exact) addSubtype(exact)
        }}
        placeholder="Goblin, Wizard, Beast…"
      />
      <datalist id="deckbuilder-subtypes">
        {allSubtypes.map((s) => (
          <option key={s} value={s} />
        ))}
      </datalist>
    </section>
  )
}

// ---------------------------------------------------------------------------
// Color mode segmented control + helpers
// ---------------------------------------------------------------------------

type ColorOp = ':' | '=' | '<='

function ColorModeSegmented({
  op,
  onChange,
}: {
  op: ColorOp
  onChange: (op: ColorOp) => void
}) {
  const options: Array<{ op: ColorOp; label: string; title: string }> = [
    { op: ':', label: 'Includes', title: 'Cards that include the chosen colour(s)' },
    { op: '=', label: 'Exactly', title: 'Cards whose colours are exactly the chosen set' },
    { op: '<=', label: 'At most', title: 'Cards whose colours are a subset of the chosen set' },
  ]
  return (
    <div className={styles.modeSegmented} role="group" aria-label="Colour comparison mode">
      {options.map((opt) => (
        <button
          key={opt.op}
          className={op === opt.op ? styles.modeButtonActive : styles.modeButton}
          onClick={() => onChange(opt.op)}
          title={opt.title}
          type="button"
        >
          {opt.label}
        </button>
      ))}
    </div>
  )
}

const C_TOKEN_RE = /(?:^|\s)c(<=|=|:)([wubrg]+)(?=\s|$)/gi

function detectColorOp(query: string): ColorOp {
  C_TOKEN_RE.lastIndex = 0
  const match = C_TOKEN_RE.exec(query)
  return ((match?.[1] as ColorOp) ?? ':')
}

function collectColorLetters(query: string): Set<string> {
  const out = new Set<string>()
  C_TOKEN_RE.lastIndex = 0
  let m: RegExpExecArray | null
  while ((m = C_TOKEN_RE.exec(query)) !== null) {
    for (const ch of m[2]!.toLowerCase()) out.add(ch)
  }
  return out
}

function rewriteColorTokens(query: string, op: ColorOp, letters: Set<string>): string {
  // Strip every existing colour token regardless of operator so we don't
  // leave stale `c:X` behind when switching to exactly/at-most.
  const cleaned = query.replace(/(?:^|\s)c(?:<=|=|:)[wubrgWUBRG]+(?=\s|$)/g, '').trim()
  if (letters.size === 0) return cleaned
  const sorted = [...letters].sort().join('')
  if (op === ':') {
    // Per-letter tokens — "must contain each chosen colour" reads cleanly
    // and stays compatible with the existing chip-toggle UX.
    const tokens = [...sorted].map((l) => `c:${l}`).join(' ')
    return cleaned ? `${cleaned} ${tokens}` : tokens
  }
  return cleaned ? `${cleaned} c${op}${sorted}` : `c${op}${sorted}`
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
  const dfc = useDfcHoverFlip(
    hoverCard
      ? {
          name: hoverCard.name,
          imageUri: hoverCard.imageUri ?? null,
          isDoubleFaced: hoverCard.isDoubleFaced ?? false,
          backFaceName: hoverCard.backFaceName ?? null,
          backFaceImageUri: hoverCard.backFaceImageUri ?? null,
        }
      : null,
  )
  const resetDfcFlip = dfc.resetFlip

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
              onHover={(c) => {
                setHoverCard((prev) => {
                  if (prev?.name !== c.name) resetDfcFlip()
                  return c
                })
              }}
              onLeave={() => setHoverCard(null)}
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
      <HoverFollowPreview
        name={hoverCard ? (dfc.displayName ?? hoverCard.name) : null}
        imageUri={hoverCard ? (dfc.displayImageUri ?? hoverCard.imageUri ?? null) : null}
        overlay={dfc.hint}
      />
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
  onHover: (c: CardSummary) => void
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
      onMouseEnter={() => onHover(card)}
      onMouseLeave={onLeave}
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

/**
 * Floats a card preview that follows the cursor while a row/tile is hovered.
 * The position state lives here, not on the parent panel — so mouse motion
 * doesn't re-render the (potentially large) sibling list. The parent only
 * re-renders when the *hovered card* changes.
 */
function HoverFollowPreview({
  name,
  imageUri,
  overlay,
}: {
  name: string | null
  imageUri: string | null
  overlay?: React.ReactNode
}) {
  const [pos, setPos] = useState<{ x: number; y: number } | null>(null)
  useEffect(() => {
    if (!name) {
      setPos(null)
      return
    }
    const onMove = (e: MouseEvent) => setPos({ x: e.clientX, y: e.clientY })
    window.addEventListener('mousemove', onMove)
    return () => window.removeEventListener('mousemove', onMove)
  }, [name])
  if (!name || !pos) return null
  return <HoverCardPreview name={name} imageUri={imageUri} pos={pos} overlay={overlay} />
}

/**
 * Card preview pane shown in the left rail in deck-mode (Moxfield-style). Replaces
 * the filter menu, which is meaningless when the right-rail catalog grid is hidden.
 * Driven by the lifted hover state in DeckbuilderPage.
 */
function DeckHoverPreview({
  name,
  imageUri,
  overlay,
}: {
  name: string | null
  imageUri: string | null
  overlay?: React.ReactNode
}) {
  if (!name) {
    return (
      <div className={styles.deckHoverPreviewEmpty}>
        Hover a card to preview it here.
      </div>
    )
  }
  const imageUrl = getCardImageUrl(name, imageUri, 'large')
  return (
    <div className={styles.deckHoverPreview}>
      <div className={styles.deckHoverPreviewImageWrap}>
        <img className={styles.deckHoverPreviewImage} src={imageUrl} alt={name} />
        {overlay}
      </div>
    </div>
  )
}

// ---------------------------------------------------------------------------
// Deck list panel (right rail)
// ---------------------------------------------------------------------------

function DeckListPanel({
  deckCards,
  catalog,
  activeFormat,
  onAdd,
  onRemove,
  commander,
  showCommanderControls,
  onToggleCommander,
  rowViolations,
  isCommanderFormat,
}: {
  deckCards: Record<string, number>
  catalog: Record<string, CardSummary>
  activeFormat: string | null
  onAdd: (card: CardSummary) => void
  onRemove: (name: string) => void
  commander: string | null
  showCommanderControls: boolean
  onToggleCommander: (name: string) => void
  rowViolations: Map<string, Set<string>>
  isCommanderFormat: boolean
}) {
  const grouped = useMemo(
    () => groupForDeckList(deckCards, catalog, commander),
    [deckCards, catalog, commander],
  )
  const [hoverCard, setHoverCard] = useState<CardSummary | null>(null)
  const [hoverName, setHoverName] = useState<string | null>(null)
  const dfc = useDfcHoverFlip(
    hoverCard
      ? {
          name: hoverCard.name,
          imageUri: hoverCard.imageUri ?? null,
          isDoubleFaced: hoverCard.isDoubleFaced ?? false,
          backFaceName: hoverCard.backFaceName ?? null,
          backFaceImageUri: hoverCard.backFaceImageUri ?? null,
        }
      : null,
  )
  const resetDfcFlip = dfc.resetFlip

  const handleEnter = (entry: { name: string; card: CardSummary | undefined }) => {
    if (hoverName !== entry.name) resetDfcFlip()
    setHoverName(entry.name)
    setHoverCard(entry.card ?? null)
  }
  const handleLeave = () => {
    setHoverName(null)
    setHoverCard(null)
  }

  useEffect(() => {
    if (hoverName && !(hoverName in deckCards)) {
      setHoverName(null)
      setHoverCard(null)
    }
  }, [deckCards, hoverName])

  return (
    <div className={styles.deckList}>
      {grouped.map((group) => (
        <div key={group.label} className={styles.deckGroup}>
          <h3 className={styles.deckGroupLabel}>
            {group.label} ({group.entries.reduce((a, e) => a + e.count, 0)})
          </h3>
          {group.entries.map((entry) => (
            <DeckRow
              key={entry.name}
              entry={entry}
              activeFormat={activeFormat}
              commander={commander}
              showCommanderControls={showCommanderControls}
              isCommanderFormat={isCommanderFormat}
              rowViolations={rowViolations}
              onAdd={onAdd}
              onRemove={onRemove}
              onToggleCommander={onToggleCommander}
              onEnter={() => handleEnter(entry)}
              onLeave={handleLeave}
            />
          ))}
        </div>
      ))}
      {grouped.length === 0 && (
        <p style={{ color: 'var(--text-muted)', fontSize: '0.8rem', textAlign: 'center', margin: 'var(--space-4) 0' }}>
          Click cards in the grid to add them.
        </p>
      )}
      <HoverFollowPreview
        name={hoverName ? (dfc.displayName ?? hoverName) : null}
        imageUri={hoverName ? (dfc.displayImageUri ?? hoverCard?.imageUri ?? null) : null}
        overlay={dfc.hint}
      />
    </div>
  )
}

// ---------------------------------------------------------------------------
// Single deck-list row. Extracted so both DeckListPanel (right-rail single
// column) and DeckCentricView (multi-column Moxfield-style layout) render the
// same +/-/count/name/crown/cost shape with the same validation visuals.
// ---------------------------------------------------------------------------

function DeckRow({
  entry,
  activeFormat,
  commander,
  showCommanderControls,
  isCommanderFormat,
  rowViolations,
  onAdd,
  onRemove,
  onToggleCommander,
  onEnter,
  onLeave,
}: {
  entry: { name: string; count: number; card: CardSummary | undefined }
  activeFormat: string | null
  commander: string | null
  showCommanderControls: boolean
  isCommanderFormat: boolean
  rowViolations: Map<string, Set<string>>
  onAdd: (card: CardSummary) => void
  onRemove: (name: string) => void
  onToggleCommander: (name: string) => void
  onEnter: () => void
  onLeave: () => void
}) {
  const illegal =
    activeFormat !== null &&
    !!entry.card?.legalFormats &&
    entry.card.legalFormats.length > 0 &&
    !entry.card.legalFormats.includes(activeFormat.toUpperCase())
  const unknown = !entry.card
  const isCommanderRow = showCommanderControls && commander === entry.name
  // Eligible commanders: legendary creatures or planeswalkers. The server's
  // CommanderEligibility is the authoritative gate (it also accepts the rare
  // "can be your commander" oracle override on non-legendary creatures and oddities
  // like Faceless One); this UI hint covers the 99% case so users don't crown a card
  // the validator will immediately reject. The cursed override-clause cards still
  // round-trip via paste-import or hand-edit if anyone ever needs them.
  const canBeCommander = !!entry.card && (
    (entry.card.supertypes.includes('LEGENDARY') && entry.card.cardTypes.includes('CREATURE')) ||
    entry.card.cardTypes.includes('PLANESWALKER')
  )
  // Pull this row's violations out of the validation response. We surface two of
  // them as inline visuals in the deck list (color identity outside the commander's;
  // exceeding the per-format copy cap); the rest are still listed in the right-rail
  // issues panel. The commander row itself is never marked as a copy violation —
  // it's the deck's commander, not a duplicate (TOO_MANY_COPIES would only fire if
  // the user *also* added it as a main-deck card, in which case the regular row
  // gets the mark and the commander row stays clean).
  const violationCodes = rowViolations.get(entry.name)
  const offIdentity = violationCodes?.has('COLOR_IDENTITY_VIOLATION') ?? false
  const tooManyCopies =
    !isCommanderRow && (violationCodes?.has('TOO_MANY_COPIES') ?? false)
  const violation = offIdentity || tooManyCopies
  // Cap-aware `+` button. effectiveCopyCap mirrors the server's per-format limit so
  // the user literally cannot exceed it; in commander-shape formats this is what
  // blocks adding a second copy of any non-basic non-override card.
  const cap = entry.card
    ? effectiveCopyCap(entry.card, isCommanderFormat)
    : Number.POSITIVE_INFINITY
  const atCap = entry.count >= cap
  const rowClasses = [
    styles.deckRow,
    illegal ? styles.deckRowIllegal : '',
    unknown ? styles.deckRowUnknown : '',
    isCommanderRow ? styles.deckRowCommander : '',
    violation ? styles.deckRowViolation : '',
    // 0-count placeholder rows (sticky basic lands in deck-centric mode) read in a muted tone
    // so they're clearly a "ramp from here" affordance rather than a normal deck entry.
    entry.count <= 0 ? styles.deckRowPlaceholder : '',
  ]
    .filter(Boolean)
    .join(' ')
  const violationReasons: string[] = []
  if (offIdentity && commander) {
    violationReasons.push(`Outside ${commander}'s color identity`)
  }
  if (tooManyCopies) {
    violationReasons.push(
      isCommanderFormat
        ? `${activeFormat} is singleton — only 1 copy allowed`
        : `Too many copies for ${activeFormat ?? 'this format'}`,
    )
  }
  const rowTitle = unknown
    ? 'Not implemented yet — placeholder only'
    : illegal
    ? `Not legal in ${activeFormat}`
    : violationReasons.length > 0
    ? violationReasons.join(' · ')
    : undefined

  return (
    <div
      className={rowClasses}
      title={rowTitle}
      onMouseEnter={onEnter}
      onMouseLeave={onLeave}
    >
      <button
        className={styles.deckRowStep}
        onClick={() => onRemove(entry.name)}
        disabled={entry.count <= 0}
        aria-label={`Decrease ${entry.name}`}
        title={entry.count <= 0 ? 'None to remove' : 'Remove one'}
        type="button"
      >
        −
      </button>
      <button
        className={styles.deckRowStep}
        onClick={() => entry.card && !atCap && onAdd(entry.card)}
        disabled={!entry.card || atCap}
        aria-label={`Increase ${entry.name}`}
        title={
          unknown
            ? 'Card not implemented'
            : atCap
            ? isCommanderFormat
              ? 'Singleton format — only 1 copy allowed'
              : `At copy limit (${cap})`
            : 'Add one'
        }
        type="button"
      >
        +
      </button>
      <span className={styles.deckRowCount}>{entry.count}×</span>
      <span className={styles.deckRowName}>
        {entry.name}
        {unknown && <span className={styles.deckRowUnknownTag}>not implemented</span>}
      </span>
      {showCommanderControls && (
        <button
          type="button"
          className={`${styles.deckRowCrown} ${
            isCommanderRow ? styles.deckRowCrownActive : ''
          }`}
          onClick={(e) => {
            e.stopPropagation()
            if (!unknown && canBeCommander) onToggleCommander(entry.name)
          }}
          disabled={unknown || !canBeCommander}
          aria-pressed={isCommanderRow}
          aria-label={
            isCommanderRow
              ? `Unset ${entry.name} as commander`
              : `Set ${entry.name} as commander`
          }
          title={
            unknown
              ? 'Card not implemented'
              : !canBeCommander
              ? 'Only legendary creatures or planeswalkers can be commanders'
              : isCommanderRow
              ? 'Commander — click to unset'
              : 'Set as commander'
          }
        >
          ♛
        </button>
      )}
      <span className={styles.deckRowCost}>
        <ManaCost cost={entry.card?.manaCost || null} size={11} />
      </span>
    </div>
  )
}

// ---------------------------------------------------------------------------
// Add-card search bar (deck-centric mode).
//
// A single text input with a popover dropdown of matching cards. Replaces the right-rail
// catalog grid in deck mode — the catalog grid is poorly suited to the narrow rail and most
// users in this view know the card name they want. Substring match on name with an exact
// > prefix > substring sort so typing "bolt" surfaces "Lightning Bolt" before "Bolt of Keranos".
// Clicking a result adds one copy (respecting the format's per-card cap); the input stays open
// so multiple adds in a row don't require re-focusing.
// ---------------------------------------------------------------------------

function AddCardSearch({
  catalog,
  deckCards,
  isCommanderFormat,
  onAdd,
  onSuggestBasics,
}: {
  catalog: CardSummary[]
  deckCards: Record<string, number>
  isCommanderFormat: boolean
  onAdd: (card: CardSummary) => void
  onSuggestBasics: () => void
}) {
  const [text, setText] = useState('')
  const [open, setOpen] = useState(false)
  const [hoverCard, setHoverCard] = useState<CardSummary | null>(null)
  const containerRef = useRef<HTMLDivElement | null>(null)
  const inputRef = useRef<HTMLInputElement | null>(null)
  const dfc = useDfcHoverFlip(
    hoverCard
      ? {
          name: hoverCard.name,
          imageUri: hoverCard.imageUri ?? null,
          isDoubleFaced: hoverCard.isDoubleFaced ?? false,
          backFaceName: hoverCard.backFaceName ?? null,
          backFaceImageUri: hoverCard.backFaceImageUri ?? null,
        }
      : null,
  )
  const resetDfcFlip = dfc.resetFlip

  const matches = useMemo(() => {
    const t = text.trim().toLowerCase()
    if (t.length < 1) return []
    const out: CardSummary[] = []
    for (const c of catalog) {
      if (c.name.toLowerCase().includes(t)) out.push(c)
    }
    out.sort((a, b) => {
      const al = a.name.toLowerCase()
      const bl = b.name.toLowerCase()
      const aRank = al === t ? 0 : al.startsWith(t) ? 1 : 2
      const bRank = bl === t ? 0 : bl.startsWith(t) ? 1 : 2
      if (aRank !== bRank) return aRank - bRank
      return al.localeCompare(bl)
    })
    return out.slice(0, 14)
  }, [catalog, text])

  // Close dropdown when the user clicks outside the search container — the dropdown is
  // absolutely positioned over the deck columns, so without this it'd intercept hovers on
  // the deck rows after the user finishes searching.
  useEffect(() => {
    if (!open) return
    const onDoc = (e: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setOpen(false)
      }
    }
    document.addEventListener('mousedown', onDoc)
    return () => document.removeEventListener('mousedown', onDoc)
  }, [open])

  const handleAdd = (card: CardSummary) => {
    onAdd(card)
    // Keep the input focused but clear the dropdown so subsequent typing starts fresh — most
    // users want to add one card at a time, then move on. They can still arrow back into the
    // input or just keep typing.
    inputRef.current?.focus()
  }

  return (
    <div ref={containerRef} className={styles.addCardSearch}>
      <input
        ref={inputRef}
        className={styles.addCardInput}
        type="text"
        value={text}
        onChange={(e) => {
          setText(e.target.value)
          setOpen(true)
        }}
        onFocus={() => setOpen(true)}
        onKeyDown={(e) => {
          if (e.key === 'Escape') {
            setOpen(false)
            inputRef.current?.blur()
          } else if (e.key === 'Enter' && matches.length > 0) {
            e.preventDefault()
            handleAdd(matches[0]!)
          }
        }}
        placeholder="Find and add cards to your deck…"
        aria-label="Find and add cards to your deck"
      />
      <button
        type="button"
        className={styles.addCardBasicsButton}
        onClick={onSuggestBasics}
        disabled={Object.keys(deckCards).length === 0}
        title="Auto-fill basic lands (Plains, Island, Swamp, Mountain, Forest) from your deck's mana curve and color requirements"
      >
        Suggest basic lands
      </button>
      {open && matches.length > 0 && (
        <div className={styles.addCardDropdown} role="listbox">
          {matches.map((card) => {
            const cap = effectiveCopyCap(card, isCommanderFormat)
            const current = deckCards[card.name] ?? 0
            const atCap = current >= cap
            return (
              <button
                key={card.name}
                type="button"
                role="option"
                aria-selected={false}
                className={styles.addCardResult}
                onClick={() => handleAdd(card)}
                onMouseEnter={() => {
                  if (hoverCard?.name !== card.name) resetDfcFlip()
                  setHoverCard(card)
                }}
                onMouseLeave={() => setHoverCard(null)}
                disabled={atCap}
                title={atCap ? 'At copy limit' : `Add ${card.name}`}
              >
                <span className={styles.addCardResultCount}>{current}×</span>
                <span className={styles.addCardResultName}>{card.name}</span>
                <span className={styles.addCardResultCost}>
                  <ManaCost cost={card.manaCost || null} size={11} />
                </span>
              </button>
            )
          })}
        </div>
      )}
      <HoverFollowPreview
        name={hoverCard ? (dfc.displayName ?? hoverCard.name) : null}
        imageUri={hoverCard ? (dfc.displayImageUri ?? hoverCard.imageUri ?? null) : null}
        overlay={dfc.hint}
      />
    </div>
  )
}

// ---------------------------------------------------------------------------
// Deck-centric view (Moxfield-style).
//
// Renders the deck as multiple columns, one bucket per card type. Uses CSS
// columns so the bucket count auto-fits the available width (3 columns on a
// typical desktop, 2 on a narrow window). Each group is a self-contained block
// with `break-inside: avoid` so a Creatures group doesn't get split mid-list.
// ---------------------------------------------------------------------------

function DeckCentricView({
  deckCards,
  catalog,
  activeFormat,
  onAdd,
  onRemove,
  commander,
  showCommanderControls,
  onToggleCommander,
  rowViolations,
  isCommanderFormat,
  onHoverEnter,
  onHoverLeave,
}: {
  deckCards: Record<string, number>
  catalog: Record<string, CardSummary>
  activeFormat: string | null
  onAdd: (card: CardSummary) => void
  onRemove: (name: string) => void
  commander: string | null
  showCommanderControls: boolean
  onToggleCommander: (name: string) => void
  rowViolations: Map<string, Set<string>>
  isCommanderFormat: boolean
  onHoverEnter: (entry: { name: string; card: CardSummary | undefined }) => void
  onHoverLeave: () => void
}) {
  const grouped = useMemo(
    () => groupByCardType(deckCards, catalog, commander),
    [deckCards, catalog, commander],
  )

  // Use the raw deck size (not the grouped-bucket count) because the Lands group now always
  // synthesizes the 5 basic-land rows even at count 0 — so an empty deck would otherwise still
  // produce one non-empty group and we'd never show the empty state.
  const isEmpty = Object.keys(deckCards).length === 0 && commander === null
  if (isEmpty) {
    return (
      <div className={styles.deckCentricEmpty}>
        Your deck is empty. Type a card name in the search bar above, or switch to{' '}
        <strong>Cards to add</strong> to browse the catalog.
      </div>
    )
  }

  return (
    <div className={styles.deckCentric}>
      <div className={styles.deckCentricColumns}>
        {grouped.map((group) => (
          <div key={group.label} className={styles.deckCentricGroup}>
            <h3 className={styles.deckCentricGroupLabel}>
              {group.label} ({group.entries.reduce((a, e) => a + e.count, 0)})
            </h3>
            {group.entries.map((entry) => (
              <DeckRow
                key={entry.name}
                entry={entry}
                activeFormat={activeFormat}
                commander={commander}
                showCommanderControls={showCommanderControls}
                isCommanderFormat={isCommanderFormat}
                rowViolations={rowViolations}
                onAdd={onAdd}
                onRemove={onRemove}
                onToggleCommander={onToggleCommander}
                onEnter={() => onHoverEnter(entry)}
                onLeave={onHoverLeave}
              />
            ))}
          </div>
        ))}
      </div>
    </div>
  )
}

// ---------------------------------------------------------------------------
// Basic-lands quick-add panel (right rail)
// ---------------------------------------------------------------------------

const BASIC_LAND_ORDER = ['Plains', 'Island', 'Swamp', 'Mountain', 'Forest']
const BASIC_LAND_COLOR: Record<string, string> = {
  Plains: 'W',
  Island: 'U',
  Swamp: 'B',
  Mountain: 'R',
  Forest: 'G',
}
function BasicLandsPanel({
  catalog,
  deckCards,
  onAdd,
  onRemove,
  onSuggest,
}: {
  catalog: CardSummary[]
  deckCards: Record<string, number>
  onAdd: (card: CardSummary) => void
  onRemove: (name: string) => void
  onSuggest: () => void
}) {
  // Pick one printing per basic land name. Prefer the entry flagged basicLand
  // but fall back to anything in the catalog with a matching name so the panel
  // still works on a partially loaded catalog.
  const basics = useMemo(() => {
    const byName = new Map<string, CardSummary>()
    for (const c of catalog) {
      if (!BASIC_LAND_ORDER.includes(c.name)) continue
      if (!byName.has(c.name) || c.basicLand) byName.set(c.name, c)
    }
    return BASIC_LAND_ORDER
      .map((name) => byName.get(name))
      .filter((c): c is CardSummary => Boolean(c))
  }, [catalog])

  const [hoverCard, setHoverCard] = useState<CardSummary | null>(null)

  if (basics.length === 0) return null

  const hasDeck = Object.keys(deckCards).length > 0

  return (
    <div className={styles.basicLands}>
      <div className={styles.basicLandsHeader}>
        <span className={styles.deckGroupLabel} style={{ margin: 0 }}>
          Basic Lands
        </span>
        <button
          className={styles.basicLandsSuggest}
          onClick={onSuggest}
          disabled={!hasDeck}
          title="Auto-fill basic lands from your deck's mana curve and color requirements"
          type="button"
        >
          Suggest
        </button>
      </div>
      <div className={styles.basicLandsGrid}>
        {basics.map((card) => {
          const count = deckCards[card.name] ?? 0
          const color = BASIC_LAND_COLOR[card.name]
          return (
            <div
              key={card.name}
              className={styles.basicLandTile}
              onMouseEnter={() => setHoverCard(card)}
              onMouseLeave={() => setHoverCard(null)}
              title={card.name}
            >
              {color ? (
                <ManaSymbol symbol={color} size={20} />
              ) : (
                <span className={styles.colorDot} style={{ background: '#888' }} aria-hidden />
              )}
              <span className={styles.basicLandTileCount}>{count}</span>
              <div className={styles.basicLandTileButtons}>
                <button
                  className={styles.basicLandBtn}
                  onClick={() => onRemove(card.name)}
                  disabled={count <= 0}
                  aria-label={`Remove ${card.name}`}
                  type="button"
                >
                  −
                </button>
                <button
                  className={styles.basicLandBtnAdd}
                  onClick={() => onAdd(card)}
                  aria-label={`Add ${card.name}`}
                  type="button"
                >
                  +
                </button>
              </div>
            </div>
          )
        })}
      </div>
      <HoverFollowPreview
        name={hoverCard?.name ?? null}
        imageUri={hoverCard?.imageUri ?? null}
      />
    </div>
  )
}

/**
 * Per-card deck-size override parsed from oracle text. Mirrors `DeckValidator.parseDeckSizeOverride`
 * on the server so the client `+` button respects "A deck can have any number / up to N cards
 * named X" without round-tripping to the server.
 */
function parseDeckSizeOverride(card: CardSummary): number | null {
  const text = card.oracleText ?? ''
  if (!text) return null
  const anyNumber = /A deck can have any number of cards named ([^.]+)\./i.exec(text)
  if (anyNumber && anyNumber[1]?.trim().toLowerCase() === card.name.toLowerCase()) {
    return Number.POSITIVE_INFINITY
  }
  const upTo = /A deck can have up to (one|two|three|four|five|six|seven|eight|nine|ten|\d+) cards named ([^.]+)\./i.exec(text)
  if (upTo && upTo[2]?.trim().toLowerCase() === card.name.toLowerCase()) {
    const word = upTo[1]!.toLowerCase()
    const map: Record<string, number> = {
      one: 1, two: 2, three: 3, four: 4, five: 5,
      six: 6, seven: 7, eight: 8, nine: 9, ten: 10,
    }
    if (word in map) return map[word]!
    const parsed = Number.parseInt(word, 10)
    return Number.isFinite(parsed) ? parsed : null
  }
  return null
}

/**
 * Maximum legal copies of [card] for a given format. Mirrors `DeckValidator.copyLimitFor`:
 *  - basics are unlimited;
 *  - "any number / up to N" oracle override wins next;
 *  - commander-shape formats (Commander/Brawl/Standard Brawl) cap non-basics at 1;
 *  - everything else caps at 4.
 *
 * The result drives both the `+`-button enabled state and the `addCard` mutation, so the user
 * literally cannot exceed the cap from the deckbuilder UI. Server validation remains the
 * authoritative gate (e.g. for paste-imports that bypass these affordances).
 */
function effectiveCopyCap(card: CardSummary, isCommanderShape: boolean): number {
  if (card.basicLand) return Number.POSITIVE_INFINITY
  const override = parseDeckSizeOverride(card)
  if (override !== null) return override
  return isCommanderShape ? 1 : 4
}

/**
 * Adapter: build `DeckEntry[]` from the standalone deckbuilder's deck +
 * catalog and call the shared `suggestBasicLands`. The standalone builder
 * doesn't know its target format, so no minDeckSize floor is applied — basics
 * scale purely off the spell curve. The validation panel surfaces undersized
 * decks so the user can re-run Suggest after adding more spells.
 */
function suggestLandsForDeck(
  deck: Record<string, number>,
  catalog: Record<string, CardSummary>,
  catalogList: CardSummary[],
  setCount: (name: string, count: number) => void,
) {
  const basicByName = new Map<string, CardSummary>()
  for (const c of catalogList) {
    if (BASIC_LAND_ORDER.includes(c.name) && (!basicByName.has(c.name) || c.basicLand)) {
      basicByName.set(c.name, c)
    }
  }
  if (basicByName.size === 0) return

  const availableBasics: BasicLand[] = BASIC_LAND_ORDER
    .filter((name) => basicByName.has(name))
    .map((name) => ({ name, color: BASIC_LAND_COLOR[name] as LandColor }))

  const entries: DeckEntry[] = []
  for (const [name, count] of Object.entries(deck)) {
    if (count <= 0) continue
    const card = catalog[name]
    if (!card || card.basicLand) continue
    entries.push({
      name: card.name,
      manaCost: card.manaCost,
      cmc: card.cmc,
      isLand: card.cardTypes.includes('LAND'),
      isBasicLand: false,
      producedColors: detectProducedColors({
        subtypes: card.subtypes,
        oracleText: card.oracleText ?? null,
      }),
      count,
    })
  }

  const result = suggestBasicLands({ entries, availableBasics })
  for (const basic of availableBasics) setCount(basic.name, result[basic.name] ?? 0)
}

// ---------------------------------------------------------------------------
// Stats / helpers
// ---------------------------------------------------------------------------

/** WUBRG order keeps the visual stack consistent with how mana costs are written. */
const CURVE_COLOR_ORDER = ['WHITE', 'BLUE', 'BLACK', 'RED', 'GREEN', 'COLORLESS'] as const
const CURVE_COLOR_HEX: Record<string, string> = {
  WHITE: '#f5f3da',
  BLUE: '#62a8ff',
  BLACK: '#5a5a5a',
  RED: '#ff6a4a',
  GREEN: '#4ab86a',
  COLORLESS: '#9aa3b2',
}

function ManaCurveBars({
  curve,
  curveByColor,
}: {
  curve: number[]
  curveByColor: Array<Record<string, number>>
}) {
  const max = Math.max(1, ...curve)
  return (
    <div className={styles.curveContainer}>
      <div className={styles.curveBars}>
        {curve.map((n, i) => {
          const segments = CURVE_COLOR_ORDER
            .map((col) => ({ col, share: curveByColor[i]?.[col] ?? 0 }))
            .filter((s) => s.share > 0)
          const cmcLabel = i === curve.length - 1 ? `${i}+` : `${i}`
          return (
            <div key={i} className={styles.curveBarColumn}>
              <div className={styles.curveBarCount}>{n > 0 ? n : ''}</div>
              <div
                className={styles.curveBarTrack}
                title={`CMC ${cmcLabel}: ${n}`}
              >
                <div
                  className={styles.curveBarStack}
                  style={{ height: `${(n / max) * 100}%` }}
                >
                  {segments.map(({ col, share }) => (
                    <div
                      key={col}
                      className={styles.curveBarSegment}
                      style={{
                        flex: share,
                        background: CURVE_COLOR_HEX[col] ?? '#888',
                      }}
                    />
                  ))}
                </div>
              </div>
              <div className={styles.curveLabel}>{cmcLabel}</div>
            </div>
          )
        })}
      </div>
    </div>
  )
}

function computeStats(deck: Record<string, number>, cards: Record<string, CardSummary>) {
  const colorCount: Record<string, number> = {}
  const curve = [0, 0, 0, 0, 0, 0, 0, 0]
  const curveByColor: Array<Record<string, number>> = Array.from(
    { length: curve.length },
    () => ({}),
  )
  for (const [name, count] of Object.entries(deck)) {
    if (count <= 0) continue
    const c = cards[name.split('#')[0] ?? name]
    if (!c) continue
    for (const col of c.colors) colorCount[col] = (colorCount[col] ?? 0) + count
    if (!c.cardTypes.includes('LAND')) {
      const idx = Math.min(c.cmc, curve.length - 1)
      curve[idx] = (curve[idx] ?? 0) + count
      // Multicolor cards split their count across each contributing color so the stacked
      // segments add up to the total bar height. Colorless cards go in their own bucket.
      const cs = c.colors.length === 0 ? ['COLORLESS'] : c.colors
      const share = count / cs.length
      for (const col of cs) {
        curveByColor[idx]![col] = (curveByColor[idx]![col] ?? 0) + share
      }
    }
  }
  return {
    colorCounts: Object.entries(colorCount).sort((a, b) => b[1] - a[1]),
    curve,
    curveByColor,
  }
}

interface DeckGroup {
  label: string
  entries: Array<{ name: string; count: number; card: CardSummary | undefined }>
}

function groupForDeckList(
  deck: Record<string, number>,
  catalog: Record<string, CardSummary>,
  commander: string | null,
): DeckGroup[] {
  const spells: DeckGroup['entries'] = []
  const lands: DeckGroup['entries'] = []
  // Commander row (if any). Pulled out of the spell/land buckets and rendered as its own
  // group at the top of the list — even if the commander is technically a creature/planeswalker
  // that would otherwise sort under Spells, it should always lead the deck list visually.
  let commanderEntry: DeckGroup['entries'][number] | null = null
  for (const [name, count] of Object.entries(deck)) {
    if (count <= 0) continue
    const card = catalog[name]
    // Basic lands have their own dedicated panel; skip them here to avoid duplication.
    if (card?.basicLand) continue
    const entry = { name, count, card }
    if (commander !== null && name === commander) {
      commanderEntry = entry
      continue
    }
    if (card?.cardTypes.includes('LAND')) lands.push(entry)
    else spells.push(entry)
  }
  spells.sort(byCmcThenName)
  lands.sort(byCmcThenName)
  const groups: DeckGroup[] = []
  if (commanderEntry) groups.push({ label: 'Commander', entries: [commanderEntry] })
  if (spells.length > 0) groups.push({ label: 'Spells', entries: spells })
  if (lands.length > 0) groups.push({ label: 'Lands', entries: lands })
  return groups
}

// Bucket the deck by card type for the multi-column "deck centric" view. Each card lands in
// exactly one bucket, picked by the first matching type in priority order — so an
// artifact-creature shows up under Creatures (where players expect it), a creature-land under
// Creatures, etc. Mirrors the convention Moxfield/Archidekt use. The commander, when
// designated, is hoisted to its own group at the top regardless of its types.
//
// Basic lands are special: every basic gets a sticky row in the Lands group with `count` from
// the deck (0 if absent). This way the user can always +/- a basic from the deck list itself,
// and a basic at zero never disappears — Moxfield-style. Basics sit at the bottom of the Lands
// group in canonical W/U/B/R/G order so they read as the deck's mana base footer.
function groupByCardType(
  deck: Record<string, number>,
  catalog: Record<string, CardSummary>,
  commander: string | null,
): DeckGroup[] {
  type BucketKey =
    | 'Creatures'
    | 'Planeswalkers'
    | 'Battles'
    | 'Instants'
    | 'Sorceries'
    | 'Artifacts'
    | 'Enchantments'
    | 'Lands'
    | 'Other'
  const buckets: Record<BucketKey, DeckGroup['entries']> = {
    Creatures: [],
    Planeswalkers: [],
    Battles: [],
    Instants: [],
    Sorceries: [],
    Artifacts: [],
    Enchantments: [],
    Lands: [],
    Other: [],
  }
  let commanderEntry: DeckGroup['entries'][number] | null = null
  const seenNames = new Set<string>()
  for (const [name, count] of Object.entries(deck)) {
    if (count <= 0) continue
    const card = catalog[name]
    seenNames.add(name)
    const entry = { name, count, card }
    if (commander !== null && name === commander) {
      commanderEntry = entry
      continue
    }
    if (!card) {
      buckets.Other.push(entry)
      continue
    }
    const t = card.cardTypes
    if (t.includes('CREATURE')) buckets.Creatures.push(entry)
    else if (t.includes('PLANESWALKER')) buckets.Planeswalkers.push(entry)
    else if (t.includes('BATTLE')) buckets.Battles.push(entry)
    else if (t.includes('INSTANT')) buckets.Instants.push(entry)
    else if (t.includes('SORCERY')) buckets.Sorceries.push(entry)
    else if (t.includes('ARTIFACT')) buckets.Artifacts.push(entry)
    else if (t.includes('ENCHANTMENT')) buckets.Enchantments.push(entry)
    else if (t.includes('LAND')) buckets.Lands.push(entry)
    else buckets.Other.push(entry)
  }
  const order: BucketKey[] = [
    'Creatures',
    'Planeswalkers',
    'Battles',
    'Instants',
    'Sorceries',
    'Artifacts',
    'Enchantments',
    'Lands',
    'Other',
  ]
  // Sort each bucket. Lands gets two-stage sort: non-basics by cmc/name, then basics in W/U/B/R/G
  // order at the end — basics with count=0 still appear so the user can ramp them up directly
  // from the deck list.
  for (const key of order) {
    if (key === 'Lands') {
      buckets.Lands.sort(byCmcThenName)
      // Filter out basics that may have been collected above (count > 0 ones); we re-add them
      // in canonical order from the catalog so 0-count basics sit alongside the >0 ones.
      const nonBasicLands = buckets.Lands.filter((e) => !e.card?.basicLand)
      const basicEntries: DeckGroup['entries'] = []
      for (const basicName of BASIC_LAND_ORDER) {
        const card = catalog[basicName]
        if (!card) continue
        basicEntries.push({ name: basicName, count: deck[basicName] ?? 0, card })
      }
      buckets.Lands = [...nonBasicLands, ...basicEntries]
    } else {
      buckets[key].sort(byCmcThenName)
    }
  }
  const groups: DeckGroup[] = []
  if (commanderEntry) groups.push({ label: 'Commander', entries: [commanderEntry] })
  for (const key of order) {
    const entries = buckets[key]
    if (entries.length === 0) continue
    groups.push({ label: key, entries })
  }
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
