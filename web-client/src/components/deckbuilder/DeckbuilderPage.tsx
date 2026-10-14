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
import { memo, startTransition, useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useLocation, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import {
  useDeckLibrary,
  commandersOf,
  mergeCommanderIntoCards,
  stripCommanderFromCards,
  type SavedDeck,
  type SavedDeckEntry,
} from '@/store/deckLibrary'
import type { PrintingRef } from '@/types'
import { PrintingPicker, type PrintingDTO } from './PrintingPicker'
import { ManaCost, ManaSymbol } from '@/components/ui/ManaSymbols'
import { useDfcHoverFlip } from '@/components/ui/useDfcHoverFlip'
import {
  DeckTile,
  DeckTileActionButton,
  deckCover,
  deckTileModel,
} from '@/components/deck/DeckTile'
import { DeckDetailsDialog } from '@/components/deck/DeckDetailsDialog'
import { CoverPicker, DeckIdentity } from './DeckIdentity'
import {
  parseQuery,
  isAdvancedQuery,
  type CardSummary,
} from './cardFilter'
import {
  CONSTRUCTED_SIDEBOARD_MAX,
  namesPhrase,
  summarizeOutsideTheGame,
  type OutsideTheGameSummary,
} from './outsideTheGame'
import { extractSetFilter } from './query'
import {
  CardGrid,
  COLOR_TOKENS,
  FilterSection,
  HoverFollowPreview,
  SearchBar,
  setCardDragData,
  sortCards,
  useCardDropZone,
  useArtOverrides,
  useSetPrintingOverride,
  withOverriddenArt,
  type CardDragSource,
  type SetInfo,
  type SortMode,
} from './browser'
import {
  canonicalNameIn,
  designatedCommanders,
  inferCommanders,
  parseArenaDeckList,
  resolveAgainstCatalog,
  type InferredCommanderSource,
  type ParseResult,
  type ResolveResult,
} from './parseArenaDeck'
import {
  canCrown,
  canPairAsCommanders,
  commanderIdentityOf,
  isWithinIdentity,
  toggleCommanderDesignation,
} from './commanderPairing'
import {
  encodeSharedDeck,
  decodeSharedDeck,
  buildShareUrl,
  SHARE_PARAM,
  type SharedDeck,
} from './shareDeck'
import { PageShell } from '@/components/ui/PageShell'
import { BuilderViewToggle } from './BuilderViewToggle'
import { BuilderModeSwitch, clearWorkingCopy, peekWorkingCopy, stashWorkingCopy } from './BuilderModeSwitch'
import { DeckHoverPreview } from './DeckHoverPreview'
import { type HoverStore, useHovered, useHoverStore } from '@/components/ui/hoverStore'
import { getDeck as getAccountDeck, upsertDeckByName } from '@/api/account'
import { useAuthStore } from '@/store/authStore'
import { type UnifiedDeck, useUnifiedDecks } from '@/store/useUnifiedDecks'
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
import {
  DeckSummary,
  ManaCurveBars,
  computeDeckStats as computeStats,
  statusClass,
  statusLabel,
  COLOR_DOT,
  type DeckValidationResult as ValidationResult,
  type DeckStats,
} from '@/components/ui/DeckSummary'
import styles from './deckbuilder.module.css'

// ---------------------------------------------------------------------------
// Types & constants
// ---------------------------------------------------------------------------

// Deck-construction formats with Scryfall-sourced legality data. Pulled from the shared
// helper so the deckbuilder's picker and the lobby filtering stay in lockstep.
const FORMAT_TOKENS = DECK_FORMATS

// Server-supplied example deck (GET /api/decks/examples). Matches the
// `ExampleDeckDTO` shape exposed by `DecksController`.
interface ExampleDeck {
  id: string
  name: string
  description: string
  /** A one-line tip on how to play it — loaded as the deck's note. */
  note?: string | null
  cards: Record<string, number>
  /** Deck format this example is built for. Null = no format hint. */
  format?: string | null
  /** Designated commander name for commander-shape examples. */
  commander?: string | null
  /** Preferred printing per card name (sparse). */
  printings?: Record<string, PrintingRef> | null
  /** Preferred printing for the commander. */
  commanderPrinting?: PrintingRef | null
  /** The second commander of a partner pair, when the example has one. */
  secondCommander?: string | null
  /** Preferred printing for the second commander. */
  secondCommanderPrinting?: PrintingRef | null
}

// "cards" = original layout (catalog grid in center, deck list on right).
// "deck"  = Moxfield-style: deck takes the wide center pane, multi-column grouped by type;
//           catalog/search moves to the right rail. Persisted via `?view=deck`.
type ViewMode = 'cards' | 'deck'

const PAGE_SIZE = 120

// ---------------------------------------------------------------------------
// Pinned-printing helpers
// ---------------------------------------------------------------------------

/**
 * Build the [SavedDeckEntry] rows that get persisted on save. Returns `undefined`
 * when no rows pin a printing — the legacy name-only path is sufficient and stays
 * cheaper to round-trip. When at least one pin exists, every card in [cards] gets
 * an entry so the persisted shape is internally consistent (a row count survey
 * over `entries` matches `cards`, regardless of which rows pinned a printing).
 */
function entriesFromCards(
  cards: Record<string, number>,
  pinned: Record<string, PrintingRef>,
): readonly SavedDeckEntry[] | undefined {
  const hasAnyPin = Object.keys(pinned).some((name) => name in cards)
  if (!hasAnyPin) return undefined
  const out: SavedDeckEntry[] = []
  for (const [name, count] of Object.entries(cards)) {
    if (count <= 0) continue
    const ref = pinned[name]
    out.push(ref ? { name, count, printing: ref } : { name, count })
  }
  return out
}

/**
 * The printed name of the pinned printing when it differs from the oracle name — a deck that
 * pinned Through the Omenpaths' Spider Manifestation lists it as "Leyline Weaver".
 */
function pinnedPrintedName(card: CardSummary | undefined, pinned: PrintingRef | undefined): string | null {
  if (!card || !pinned) return null
  const match = card.printedNamePrintings?.find(
    (p) => p.setCode.toUpperCase() === pinned.setCode.toUpperCase() && p.collectorNumber === pinned.collectorNumber,
  )
  return match?.name ?? null
}

/**
 * Inverse of [entriesFromCards]: rebuild the per-name pin map from a persisted
 * deck's `entries` field. The deck's commander pins (both halves of a partner
 * pair) are folded in so they survive a save → load round-trip alongside the rest.
 */
function pinnedPrintingsFromEntries(
  entries: readonly SavedDeckEntry[] | undefined,
  commanders?: CommanderPins,
): Record<string, PrintingRef> {
  const out: Record<string, PrintingRef> = {}
  for (const e of entries ?? []) {
    if (e.printing) out[e.name] = e.printing
  }
  return { ...out, ...(commanders ? commanderPinsOf(commanders) : {}) }
}

/** The deck being built, as stashed while the player is in cube mode (see `BuilderModeSwitch`). */
interface DeckWorkingCopy {
  readonly activeDeckId: string | null
  readonly deckName: string
  readonly deckNote: string
  readonly coverCard: string | null
  readonly deckCards: Record<string, number>
  readonly sideboardCards: Record<string, number>
  readonly companion: string | null
  readonly commanders: readonly string[]
  readonly pinnedPrintings: Record<string, PrintingRef>
}

/** The commander fields a saved, shared or example deck carries. */
interface CommanderPins {
  readonly commander?: string | null | undefined
  readonly commanderPrinting?: PrintingRef | null | undefined
  readonly secondCommander?: string | null | undefined
  readonly secondCommanderPrinting?: PrintingRef | null | undefined
}

/** Name → pinned printing for a deck's commander(s). */
function commanderPinsOf(deck: CommanderPins): Record<string, PrintingRef> {
  const out: Record<string, PrintingRef> = {}
  if (deck.commander && deck.commanderPrinting) out[deck.commander] = deck.commanderPrinting
  if (deck.commander && deck.secondCommander && deck.secondCommanderPrinting) {
    out[deck.secondCommander] = deck.secondCommanderPrinting
  }
  return out
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

  // The saved-deck browser + "My decks" card show the unified library (cloud decks when signed in,
  // plus browser-only locals), each tagged online/local. Plain localStorage actions below still back
  // the anonymous save path and the active-deck (local) operations.
  const {
    decks: browserDecks,
    reload: reloadUnifiedDecks,
    removeDeck: removeUnifiedDeck,
    updateDetails: updateUnifiedDeckDetails,
  } = useUnifiedDecks()
  const hydrate = useDeckLibrary((s) => s.hydrate)
  const hydrated = useDeckLibrary((s) => s.hydrated)
  const saveDeck = useDeckLibrary((s) => s.saveDeck)
  const deleteDeck = useDeckLibrary((s) => s.deleteDeck)
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

  // Full set catalog (code, display name, ISO release date) fetched from the server.
  // The release date drives the deckbuilder's "sort by release" mode and the year shown
  // next to each set name.
  const [setInfos, setSetInfos] = useState<SetInfo[]>([])
  useEffect(() => {
    let cancelled = false
    fetch('/api/sets')
      .then((r) => (r.ok ? r.json() : []))
      .then((list: SetInfo[]) => {
        if (!cancelled) setSetInfos(list)
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

  // Reverse index for share-link decoding: `SET:collector` → card name. Share codes identify
  // cards by printing rather than name (much shorter), so resolving a code back into a deck
  // means mapping each printing to its name via the catalog's default printings.
  const nameByPrinting = useMemo(() => {
    const out = new Map<string, string>()
    for (const c of catalog) {
      if (c.setCode && c.collectorNumber) {
        out.set(`${c.setCode.toUpperCase()}:${c.collectorNumber}`, c.name)
      }
    }
    return out
  }, [catalog])

  // Share-link resolvers (paired inverses). `resolvePrinting` gives a card's catalog-default
  // printing for encoding; `resolveName` maps a printing back to a name for decoding.
  const resolvePrinting = useCallback(
    (name: string): PrintingRef | null => {
      const c = catalogIndex[name]
      return c?.setCode && c?.collectorNumber
        ? { setCode: c.setCode, collectorNumber: c.collectorNumber }
        : null
    },
    [catalogIndex],
  )
  const resolveName = useCallback(
    (printing: PrintingRef): string | null =>
      nameByPrinting.get(`${printing.setCode.toUpperCase()}:${printing.collectorNumber}`) ?? null,
    [nameByPrinting],
  )

  // Working deck state. A deck left here by a switch to cube mode — saved or not — starts out exactly
  // as it was left rather than reloading from the library (or blank, for a deck never saved).
  const location = useLocation()
  const [resume] = useState(() => peekWorkingCopy<DeckWorkingCopy>('deck', location.pathname))
  useEffect(() => {
    if (resume) clearWorkingCopy('deck')
  }, [resume])
  const [deckName, setDeckName] = useState(resume?.deckName ?? 'Untitled deck')
  // The owner's note and chosen cover card — what the deck galleries show besides the name.
  // A null cover means automatic (the rarest card); see `deckCover`.
  const [deckNote, setDeckNote] = useState(resume?.deckNote ?? '')
  const [coverCard, setCoverCard] = useState<string | null>(resume?.coverCard ?? null)
  const [coverPickerOpen, setCoverPickerOpen] = useState(false)
  const [deckCards, setDeckCards] = useState<Record<string, number>>(resume?.deckCards ?? {})
  // Constructed sideboard ("outside the game", CR 100.4a) — cards reachable in-game only by wish
  // effects (Burning Wish, …). Persisted on the saved deck and sent as `sideboard` when playing.
  // Limited (sealed/draft) decks don't use this: their sideboard is the pool − maindeck complement,
  // derived server-side (CR 100.4b).
  const [sideboardCards, setSideboardCards] = useState<Record<string, number>>(resume?.sideboardCards ?? {})
  // The deck's companion (CR 702.139): one sideboard card with a companion ability, revealed before
  // the game and fetchable for {3} once. Marked from the sideboard panel; the server validates the
  // condition (and, in Commander, the colour identity) on the same /validate round trip.
  const [companion, setCompanion] = useState<string | null>(resume?.companion ?? null)
  // Only a companion still on the sideboard counts — removing the card un-marks it.
  const companionForSave: string | null = companion && (sideboardCards[companion] ?? 0) > 0 ? companion : null
  const toggleCompanion = useCallback((name: string) => {
    setCompanion((prev) => (prev === name ? null : name))
  }, [])
  const [activeDeckId, setActiveDeckId] = useState<string | null>(resume?.activeDeckId ?? null)
  // When signed in, the unified Save targets the account (cloud); otherwise it writes to the local
  // browser library. `saveFlash` briefly overrides the Save button label to confirm the result.
  const isLoggedIn = useAuthStore((s) => s.status) === 'authenticated'
  const [saveFlash, setSaveFlash] = useState<string | null>(null)
  const flashSave = useCallback((msg: string) => {
    setSaveFlash(msg)
    window.setTimeout(() => setSaveFlash(null), 2000)
  }, [])
  // Pinned printings, keyed by card name. Empty unless the user opens the printing picker
  // and chooses a non-default printing for some row. Persisted via [SavedDeck.entries] (v2)
  // and round-tripped on load. Same-name-different-printing rows are not surfaced as separate
  // UI rows yet — Phase 7's first ship pins one printing per name; the storage shape supports
  // splitting later without a v3 bump. Counts collapse on name regardless, so the singleton /
  // 4-of cap stays correct either way (CR 100.4).
  const [pinnedPrintings, setPinnedPrintings] = useState<Record<string, PrintingRef>>(resume?.pinnedPrintings ?? {})
  // Art for pinned printings, keyed by card name. Populated from two sources: (a) the
  // printing picker, which already has every printing's image URL on hand when the user
  // clicks a thumbnail, so a fresh pin populates here for free; (b) a batched
  // `/api/printings?names=…` fetch when a saved deck loads carrying entries that pin
  // printings the picker hasn't been opened for yet. Hover-preview / saved-deck art read
  // from this cache so they reflect the user's choice rather than the catalog default.
  const [pinnedPrintingArt, setPinnedPrintingArt] = useState<
    Record<string, { imageUri: string | null; backFaceImageUri: string | null }>
  >({})
  // Designated commander(s) for Commander/Brawl/Standard Brawl decks: empty when none has been
  // picked yet or the current format doesn't use one; two for a partner pair (CR 702.124), the
  // first being the deck's `commander` and the second its `secondCommander`. The deckbuilder UI
  // exposes a crown toggle on each row to set/clear these — see DeckListPanel below.
  const [commanders, setCommanders] = useState<readonly string[]>(resume?.commanders ?? [])
  const commander: string | null = commanders[0] ?? null
  const secondCommander: string | null = commanders[1] ?? null

  // Hydrate from URL deckId once decks are loaded. Also push the saved deck's stamped
  // format into the URL — without it, `activeFormat` stays whatever was in the search
  // params (often `null`), and the "clear commander when not a commander format" effect
  // would immediately wipe a just-loaded commander designation.
  //
  // Guarded by a ref so the effect runs at most once per actual `deckId` change. Without
  // this, react-router's `setSearchParams` reference changes on every URL update — which
  // includes typing into the search filter. Each keystroke would otherwise re-trigger
  // hydration and overwrite in-progress edits with whatever's persisted in localStorage.
  // A resumed working copy already is this deck, newer than what's saved: don't hydrate over it.
  const lastHydratedDeckIdRef = useRef<string | null>(resume ? deckId ?? null : null)
  useEffect(() => {
    if (!hydrated || !deckId) return
    if (lastHydratedDeckIdRef.current === deckId) return
    lastHydratedDeckIdRef.current = deckId
    const existing = getDeck(deckId)
    if (existing) {
      setDeckName(existing.name)
      setDeckNote(existing.note ?? '')
      setCoverCard(existing.coverCard ?? null)
      setDeckCards(mergeCommanderIntoCards(existing.cards, ...commandersOf(existing)))
      setCommanders(commandersOf(existing))
      // Without this a refresh (or a deep link to /deckbuilder/<id>) rehydrates the deck with an
      // empty sideboard, and the next Save writes that emptiness back — `saveDeck` replaces the
      // stored record wholesale rather than merging, so the sideboard is destroyed.
      setSideboardCards(existing.sideboard ? { ...existing.sideboard } : {})
      setCompanion(existing.companion ?? null)
      setActiveDeckId(existing.id)
      setPinnedPrintings(pinnedPrintingsFromEntries(existing.entries, existing))
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

  // Load a shared deck carried in the URL (`?d=<code>`). The whole deck travels in the link
  // (no server round-trip), so we decode it into the working deck as an *unsaved* draft —
  // the recipient can tweak it and hit Save to add it to their own library. The `d` param is
  // stripped immediately afterwards so it doesn't linger in the address bar, reload on every
  // filter keystroke, or get re-shared by accident. Guarded by a ref so it fires once even as
  // `searchParams` churns. A malformed code is silently ignored (param still stripped).
  // The ref guard makes this fire exactly once per real `?d=` (and survives StrictMode's
  // mount→unmount→mount double-invoke). Deliberately *no* cancel-on-cleanup: the decode is
  // async, and aborting the in-flight decode on StrictMode's simulated unmount would leave the
  // ref already flipped, so the re-run no-ops and the deck never loads (empty-deck bug). The
  // decode resolves and applies its state regardless of remounts, which is harmless.
  const sharedLoadedRef = useRef(false)
  useEffect(() => {
    const code = searchParams.get(SHARE_PARAM)
    if (!code || sharedLoadedRef.current) return
    // v2 share codes identify cards by printing, so decoding needs the catalog's reverse index.
    // Wait for `/api/cards` to land before resolving — the effect re-runs when `catalog` arrives,
    // and the ref guard still fires it exactly once.
    if (catalog.length === 0) return
    sharedLoadedRef.current = true
    void decodeSharedDeck(code, resolveName).then((shared) => {
      // Apply the URL rewrite and the decoded deck as one low-priority update. react-router's
      // BrowserRouter commits location changes inside `startTransition`, so the `fmt` stamp
      // below lands in a transition lane. If we set `commander` as a normal (urgent) update it
      // commits FIRST — in a render where `activeFormat` is still stale (no `fmt` yet) — and the
      // "clear commander when not a commander format" effect immediately wipes the just-loaded
      // designation, dropping the commander from every shared Commander deck. Scheduling our
      // own updates in the same transition coalesces them with the URL change, so `activeFormat`
      // and `commander` commit together and that guard never sees the inconsistent in-between.
      startTransition(() => {
        setSearchParams(
          (prev) => {
            const params = new URLSearchParams(prev)
            params.delete(SHARE_PARAM)
            // Stamp the deck's format so the commander-format guard and the legality filter
            // both see it — same trick as the saved-deck hydration above, done in one URL write.
            if (shared?.format) params.set('fmt', shared.format.toUpperCase())
            // Open shared decks in the Moxfield-style deck view: the recipient is here to
            // read the list, not browse the catalog to build from scratch.
            if (shared) params.set('view', 'deck')
            return params
          },
          { replace: true },
        )
        if (!shared) return
        setDeckName(shared.name || 'Shared deck')
        setDeckNote(shared.note ?? '')
        setCoverCard(shared.coverCard ?? null)
        setDeckCards(mergeCommanderIntoCards(shared.cards, ...commandersOf(shared)))
        setCommanders(commandersOf(shared))
        // Reset rather than leave in place: a deck loaded without one has no sideboard, and a
        // stale board from the previously open deck would otherwise follow it.
        setSideboardCards(shared.sideboard ? { ...shared.sideboard } : {})
        setCompanion(shared.companion ?? null)
        setActiveDeckId(null)
        setPinnedPrintings({ ...(shared.printings ?? {}), ...commanderPinsOf(shared) })
      })
    })
    // setSearchParams is stable; searchParams churn is filtered by the ref guard. `catalog.length`
    // re-triggers once the catalog loads so a code present on first paint still decodes.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [searchParams, catalog.length, resolveName])

  // Transient "Link copied!" feedback for the Share button.
  const [shareCopied, setShareCopied] = useState(false)

  // Import-from-text modal visibility.
  const [importOpen, setImportOpen] = useState(false)
  // Bulk-edit (export + edit text) modal visibility.
  const [bulkEditOpen, setBulkEditOpen] = useState(false)
  // Saved-decks browser overlay visibility.
  const [decksBrowserOpen, setDecksBrowserOpen] = useState(false)
  // Example-decks picker overlay visibility.
  const [examplesOpen, setExamplesOpen] = useState(false)
  // Deep links that open one of the builder's dialogs straight away — the library's "Import a list"
  // and "Start from an example" (?open=import|examples), and the older ?decks=open for the saved-deck
  // browser — then strip the param so it doesn't re-open on later navigations.
  useEffect(() => {
    const open = searchParams.get('decks') === 'open' ? 'decks' : searchParams.get('open')
    if (open !== 'decks' && open !== 'import' && open !== 'examples') return
    if (open === 'decks') {
      reloadUnifiedDecks()
      setDecksBrowserOpen(true)
    } else if (open === 'import') {
      setImportOpen(true)
    } else {
      setExamplesOpen(true)
    }
    setSearchParams(
      (prev) => {
        const params = new URLSearchParams(prev)
        params.delete('decks')
        params.delete('open')
        return params
      },
      { replace: true },
    )
  }, [searchParams, setSearchParams, reloadUnifiedDecks])

  // Server-supplied starter decks. Mirrors the DeckPicker's Examples tab — fetched once and
  // surfaced in the deckbuilder via the topbar "Examples" button.
  const [examples, setExamples] = useState<ExampleDeck[]>([])
  useEffect(() => {
    let cancelled = false
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
  // A partner pair loses only the commander that left; the remaining one moves up.
  useEffect(() => {
    if (!isCommanderFormat && commanders.length > 0) setCommanders([])
  }, [isCommanderFormat, commanders])
  useEffect(() => {
    if (commanders.some((c) => !(c in deckCards))) setCommanders(commanders.filter((c) => c in deckCards))
  }, [commanders, deckCards])

  const parseResult = useMemo(() => parseQuery(query, { withErrors: true }), [query])
  const predicate = parseResult.predicate
  const queryErrors = parseResult.errors
  const advanced = useMemo(() => isAdvancedQuery(query), [query])
  // Dominant set filter (`s:EOE`, `set:eoe`) extracted from the parsed AST. Drives the
  // catalog grid + hover preview to render the *reprint's* art when a card has a printing
  // in that set, instead of always showing the canonical CardDefinition's image.
  const activeSetFilter = useMemo(() => extractSetFilter(parseResult.ast), [parseResult.ast])
  // When a deck format is selected, scope the catalog to format-legal cards automatically so
  // the user only sees plays they can actually run. The `format:` query token still works as
  // an extra filter (intersected on top), but isn't required for this default behavior.
  // Basic lands are hidden from the catalog grid — users add them from the sticky deck-list
  // rows or via "Suggest basic lands", and clogging the search results with five always-legal
  // names that match every color/land filter is just noise.
  // In a commander format with a commander designated, the catalogue also narrows to cards inside
  // the commander(s)' combined colour identity (CR 903.4) — the same rule, and the same helpers,
  // as the Commander Draft / Sealed builder's "♛ Identity" chip. On by default and back on
  // whenever the designation changes; the chip in the search bar turns it off.
  const designatedCommanderCards = useCommanderCards(commanders, catalogIndex)
  const catalogIdentity = useMemo(
    () => (isCommanderFormat && designatedCommanderCards.length > 0 ? commanderIdentityOf(designatedCommanderCards) : null),
    [isCommanderFormat, designatedCommanderCards],
  )
  const [restrictToIdentity, setRestrictToIdentity] = useState(true)
  const commandersKey = commanders.join('\n')
  useEffect(() => {
    if (commandersKey) setRestrictToIdentity(true)
  }, [commandersKey])
  const identityFilterActive = catalogIdentity != null && restrictToIdentity

  const filtered = useMemo(() => {
    let result = catalog.filter((c) => !c.basicLand && predicate(c))
    if (activeFormat) {
      result = result.filter((c) => c.legalFormats?.includes(activeFormat) ?? false)
    }
    if (identityFilterActive && catalogIdentity) {
      result = result.filter((c) => isWithinIdentity(c, catalogIdentity))
    }
    return sortCards(result, sortMode)
  }, [catalog, predicate, sortMode, activeFormat, identityFilterActive, catalogIdentity])

  // Pager: cap rendered tiles so a 1000+ card catalogue doesn't melt the browser.
  // Reset whenever the result set changes (new query / filter / sort).
  const [visibleCount, setVisibleCount] = useState(PAGE_SIZE)
  useEffect(() => {
    setVisibleCount(PAGE_SIZE)
  }, [query, sortMode, activeFormat, identityFilterActive, catalogIdentity])

  // Reprint override map: card name → the matching set's printing (ref + art). Populated
  // lazily via /api/printings whenever an `s:` filter is active and the visible set of
  // card names changes. One batched request per filter change covers (a) the catalog grid
  // and hover preview's art swap, and (b) the auto-pin behavior in `addCard` so adding a
  // reprint while filtered to that set automatically pins its printing on the deck row.
  // When [activeSetFilter] is null this stays empty and renders fall back to the
  // catalog's default printing.
  // Basic lands ride along unconditionally so the sticky deck-list +/- and "Suggest basic
  // lands" can pin the active set's printing even though basics no longer appear in `filtered`.
  // `useArtOverrides` layers on cards matched by a printed name ("ademi" → the Through the
  // Omenpaths printing of Spectacular Spider-Man), so those show and pin that printing too.
  const overrideNames = useMemo(
    () => Array.from(new Set([...filtered.map((c) => c.name), ...BASIC_LAND_ORDER])),
    [filtered],
  )
  const setPrintingOverride = useArtOverrides(
    parseResult.ast,
    filtered,
    useSetPrintingOverride(activeSetFilter, overrideNames),
  )

  // Apply the set-filter art override to filtered cards. Done once here so the catalog
  // grid, the hover preview, and the in-deck-row hover all see the same imageUri without
  // each component needing to know about the override.
  const filteredWithArt = useMemo<CardSummary[]>(
    () => withOverriddenArt(filtered, setPrintingOverride),
    [filtered, setPrintingOverride],
  )

  const displayed = useMemo(
    () => filteredWithArt.slice(0, visibleCount),
    [filteredWithArt, visibleCount],
  )

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
      // A partner pair strips both commanders and sends the second as `secondCommander`.
      const sendCommander = isCommanderFormat && commander
      const sendCompanion = companionForSave
      const deckListForValidation = sendCommander
        ? stripCommanderFromCards(deckCards, commander, secondCommander)
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
          ...(sendCommander && secondCommander ? { secondCommander } : {}),
          // A companion (CR 702.139) routes validation through the structured path, which checks
          // its condition against the deck — and, in Commander, its colour identity.
          ...(sendCompanion ? { companion: sendCompanion, sideboard: sideboardCards } : {}),
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
  }, [deckCards, activeFormat, isCommanderFormat, commander, secondCommander, companionForSave, sideboardCards])

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

  // The validator's companion findings (condition, identity, copies), shown on the sideboard panel
  // beside the companion they are about rather than on a deck row.
  const companionIssues = useMemo(
    () =>
      (validation?.errors ?? [])
        .filter((e) => e.code.includes('COMPANION') || (companionForSave !== null && e.cardName === companionForSave))
        .map((e) => e.message),
    [validation, companionForSave],
  )

  // ----- Mutations -----

  // When a card is added while filtered to a specific set, pin that set's printing
  // automatically — the user is staring at the EOE Banishing Light art when they click +,
  // so the saved entry should remember "EOE printing", not silently fall back to the
  // canonical default. Skips the override when the row already has a manual pin (the
  // user picked something else explicitly). Also drives the basic-land path: basics no
  // longer appear in the catalog grid, but the sticky deck-list +/- and "Suggest basic
  // lands" still run this so basics align with the selected set's art when available.
  // A card found by its printed name pins that printing the same way.
  const applySetPinIfAvailable = useCallback((name: string) => {
    const override = setPrintingOverride[name]
    if (!override || pinnedPrintings[name]) return
    setPinnedPrintings((prev) => ({
      ...prev,
      [name]: { setCode: override.setCode, collectorNumber: override.collectorNumber },
    }))
    setPinnedPrintingArt((prev) => ({
      ...prev,
      [name]: { imageUri: override.imageUri, backFaceImageUri: override.backFaceImageUri },
    }))
  }, [setPrintingOverride, pinnedPrintings])

  const addCard = (card: CardSummary) => {
    setDeckCards((prev) => {
      const current = prev[card.name] ?? 0
      const max = effectiveCopyCap(card, isCommanderFormat)
      if (current >= max) return prev
      return { ...prev, [card.name]: current + 1 }
    })
    applySetPinIfAvailable(card.name)
  }

  const removeCard = (name: string) => {
    setDeckCards((prev) => {
      const current = prev[name] ?? 0
      if (current <= 0) return prev
      const next = { ...prev }
      if (current === 1) {
        delete next[name]
        // Drop the pin once no copies remain so it doesn't silently resurrect when
        // the user re-adds the card later expecting the default printing.
        setPinnedPrintings((prevPins) => {
          if (!(name in prevPins)) return prevPins
          const nextPins = { ...prevPins }
          delete nextPins[name]
          return nextPins
        })
      } else next[name] = current - 1
      return next
    })
  }

  const setCardCount = useCallback((name: string, count: number) => {
    setDeckCards((prev) => {
      const next = { ...prev }
      if (count <= 0) {
        delete next[name]
        setPinnedPrintings((prevPins) => {
          if (!(name in prevPins)) return prevPins
          const nextPins = { ...prevPins }
          delete nextPins[name]
          return nextPins
        })
      } else next[name] = count
      return next
    })
  }, [])

  // Sideboard add/remove. The 4-of cap (CR 100.2a) applies to deck + sideboard *combined*, so a
  // card already maxed in the main deck can't also be added to the sideboard.
  const addToSideboard = useCallback((card: CardSummary) => {
    setSideboardCards((prev) => {
      const inSide = prev[card.name] ?? 0
      const inDeck = deckCards[card.name] ?? 0
      const cap = effectiveCopyCap(card, isCommanderFormat)
      if (inDeck + inSide >= cap) return prev
      return { ...prev, [card.name]: inSide + 1 }
    })
  }, [deckCards, isCommanderFormat])

  const removeFromSideboard = useCallback((name: string) => {
    setSideboardCards((prev) => {
      const current = prev[name] ?? 0
      if (current <= 0) return prev
      const next = { ...prev }
      if (current === 1) delete next[name]
      else next[name] = current - 1
      return next
    })
  }, [])

  const setPinnedPrinting = useCallback((name: string, printing: PrintingDTO) => {
    setPinnedPrintings((prev) => ({
      ...prev,
      [name]: { setCode: printing.setCode, collectorNumber: printing.collectorNumber },
    }))
    setPinnedPrintingArt((prev) => ({
      ...prev,
      [name]: { imageUri: printing.imageUri, backFaceImageUri: printing.backFaceImageUri },
    }))
  }, [])

  const clearPinnedPrinting = useCallback((name: string) => {
    setPinnedPrintings((prev) => {
      if (!(name in prev)) return prev
      const next = { ...prev }
      delete next[name]
      return next
    })
    setPinnedPrintingArt((prev) => {
      if (!(name in prev)) return prev
      const next = { ...prev }
      delete next[name]
      return next
    })
  }, [])

  const handleNew = () => {
    setDeckName('Untitled deck')
    setDeckNote('')
    setCoverCard(null)
    setDeckCards({})
    setSideboardCards({})
    setCompanion(null)
    setCommanders([])
    setActiveDeckId(null)
    setPinnedPrintings({})
    navigate(`/deckbuilder${searchSuffix()}`)
  }

  // The art a card shows in *this* deck: its pinned printing's, else the catalogue default.
  const deckArtFor = useCallback(
    (name: string): string | null => pinnedPrintingArt[name]?.imageUri ?? catalogIndex[name]?.imageUri ?? null,
    [pinnedPrintingArt, catalogIndex],
  )

  // The cover as it paints right now — chosen (while still in the deck) else automatic.
  const coverHero = useMemo(() => {
    const hero = deckCover(
      {
        cards: deckCards,
        commander,
        secondCommander,
        coverCard,
        coverImageUri: coverCard ? deckArtFor(coverCard) : null,
      },
      catalogIndex,
    )
    return hero ? { ...hero, imageUri: deckArtFor(hero.name) ?? hero.imageUri ?? null } : null
  }, [deckCards, commander, secondCommander, coverCard, catalogIndex, deckArtFor])
  const coverChosen = coverCard !== null && (coverCard in deckCards || commanders.includes(coverCard))

  // Note + cover as saved. The cover's art is snapshotted so a picker can paint it without the
  // catalogue; a cover whose card has left the deck isn't saved at all.
  const deckDetailsForSave = useCallback((): { note?: string; coverCard?: string; coverImageUri?: string } => {
    const note = deckNote.trim()
    const art = coverChosen && coverCard ? deckArtFor(coverCard) : null
    return {
      ...(note ? { note } : {}),
      ...(coverChosen && coverCard ? { coverCard } : {}),
      ...(art ? { coverImageUri: art } : {}),
    }
  }, [deckNote, coverChosen, coverCard, deckArtFor])

  // Unified Save: when signed in, persist to the account (cloud); otherwise to the local browser
  // library. We dedupe the cloud deck by name (the same rule the sign-in migration prompt uses) so
  // re-saving the same deck overwrites it rather than piling up duplicates — no client-side id to
  // thread through every load path. `optionalName` overrides the deck name for "Save as".
  const saveToCloud = async (overrideName?: string) => {
    const built = buildSharedDeck()
    if (!built) {
      flashSave('Deck is empty')
      return
    }
    const shared = overrideName ? { ...built, name: overrideName } : built
    try {
      await upsertDeckByName(shared)
      if (overrideName) setDeckName(overrideName)
      flashSave('Saved to account')
    } catch {
      flashSave('Save failed')
    }
  }

  const saveLocal = (overrideName?: string): string => {
    // Per `SavedDeck.commander`: the commander is stored separately from `cards` (the
    // library), matching the server's `Deck.cards` convention. Strip it out on save so
    // reloading + re-validating doesn't double-count. A partner pair strips both.
    const designated = isCommanderFormat ? commander : null
    const designatedSecond = designated ? secondCommander : null
    const cardsForSave = stripCommanderFromCards(deckCards, designated, designatedSecond)
    const entries = entriesFromCards(cardsForSave, pinnedPrintings)
    const commanderPrintingForSave = designated ? pinnedPrintings[designated] : undefined
    const secondPrintingForSave = designatedSecond ? pinnedPrintings[designatedSecond] : undefined
    const isRename = overrideName !== undefined
    const saved = saveDeck({
      // "Save as" always creates a fresh local deck; plain Save updates the active one.
      ...(!isRename && activeDeckId ? { id: activeDeckId } : {}),
      name: (overrideName ?? deckName).trim() || 'Untitled deck',
      cards: cardsForSave,
      ...(activeFormat ? { format: activeFormat } : {}),
      ...(designated ? { commander: designated } : {}),
      ...(commanderPrintingForSave ? { commanderPrinting: commanderPrintingForSave } : {}),
      ...(designatedSecond ? { secondCommander: designatedSecond } : {}),
      ...(secondPrintingForSave ? { secondCommanderPrinting: secondPrintingForSave } : {}),
      ...(entries ? { entries } : {}),
      ...(Object.keys(sideboardCards).length > 0 ? { sideboard: sideboardCards } : {}),
      ...(companionForSave ? { companion: companionForSave } : {}),
      ...deckDetailsForSave(),
    })
    setDeckName(saved.name)
    setActiveDeckId(saved.id)
    return saved.id
  }

  const handleSave = async () => {
    if (isLoggedIn) {
      await saveToCloud()
      return
    }
    const id = saveLocal()
    if (id !== deckId) navigate(`/deckbuilder/${id}${searchSuffix()}`, { replace: true })
  }

  const handleSaveAs = async () => {
    const name = window.prompt('New deck name', `${deckName} (copy)`)
    if (!name || !name.trim()) return
    if (isLoggedIn) {
      await saveToCloud(name.trim())
      return
    }
    const id = saveLocal(name.trim())
    navigate(`/deckbuilder/${id}${searchSuffix()}`, { replace: true })
  }

  const handleDelete = () => {
    if (!activeDeckId) return
    if (!window.confirm(`Delete "${deckName}"?`)) return
    deleteDeck(activeDeckId)
    handleNew()
  }

  // Build a self-contained share link for the current working deck and copy it to the
  // clipboard. The deck is encoded into the URL itself (no server storage), so the link
  // captures exactly what's on screen — saved or not — including the commander, format and
  // any pinned printings. Mirrors the save path's commander-stripping so a shared deck
  // re-imports without double-counting the commander.
  // Snapshot the working deck as a SharedDeck (the canonical wire shape), or null if empty. Used by
  // both the share-link path and the account (cloud) save path so they capture identical content.
  const buildSharedDeck = useCallback((): SharedDeck | null => {
    const designated = isCommanderFormat ? commander : null
    const designatedSecond = designated ? secondCommander : null
    const cardsForShare = stripCommanderFromCards(deckCards, designated, designatedSecond)
    if (Object.keys(cardsForShare).length === 0) return null
    const printings: Record<string, PrintingRef> = {}
    for (const [name, ref] of Object.entries(pinnedPrintings)) {
      if (name in cardsForShare) printings[name] = ref
    }
    const commanderPrinting = designated ? pinnedPrintings[designated] : undefined
    const secondCommanderPrinting = designatedSecond ? pinnedPrintings[designatedSecond] : undefined
    return {
      name: deckName.trim() || 'Untitled deck',
      cards: cardsForShare,
      ...(Object.keys(printings).length > 0 ? { printings } : {}),
      ...(activeFormat ? { format: activeFormat } : {}),
      ...(designated ? { commander: designated } : {}),
      ...(commanderPrinting ? { commanderPrinting } : {}),
      ...(designatedSecond ? { secondCommander: designatedSecond } : {}),
      ...(secondCommanderPrinting ? { secondCommanderPrinting } : {}),
      // Carried for the account save; the v2 share code has no field for them and drops them.
      ...(Object.keys(sideboardCards).length > 0 ? { sideboard: sideboardCards } : {}),
      // The share code carries the companion (not the rest of the sideboard); the account save both.
      ...(companionForSave ? { companion: companionForSave } : {}),
      ...deckDetailsForSave(),
    }
  }, [
    isCommanderFormat, commander, secondCommander, deckCards, pinnedPrintings, deckName, activeFormat, sideboardCards,
    deckDetailsForSave, companionForSave,
  ])

  // Apply a SharedDeck into the builder (account load / deep link). Mirrors the share-URL decode
  // path: schedules format + commander in one transition so the "clear commander" guard never sees
  // an inconsistent in-between state.
  const applySharedDeck = useCallback(
    (shared: SharedDeck) => {
      startTransition(() => {
        setSearchParams(
          (prev) => {
            const params = new URLSearchParams(prev)
            params.delete('accountDeck')
            if (shared.format) params.set('fmt', shared.format.toUpperCase())
            params.set('view', 'deck')
            return params
          },
          { replace: true },
        )
        setDeckName(shared.name || 'Saved deck')
        setDeckNote(shared.note ?? '')
        setCoverCard(shared.coverCard ?? null)
        setDeckCards(mergeCommanderIntoCards(shared.cards, ...commandersOf(shared)))
        setCommanders(commandersOf(shared))
        // Reset rather than leave in place: a deck loaded without one has no sideboard, and a
        // stale board from the previously open deck would otherwise follow it.
        setSideboardCards(shared.sideboard ? { ...shared.sideboard } : {})
        setCompanion(shared.companion ?? null)
        setActiveDeckId(null)
        setPinnedPrintings({ ...(shared.printings ?? {}), ...commanderPinsOf(shared) })
      })
    },
    [setSearchParams],
  )

  // Deep link from the profile page (/deckbuilder?accountDeck=<id>): fetch the saved deck and load
  // it. Guarded so it runs once per id even as the effect re-fires on searchParams churn.
  const accountDeckLoadedRef = useRef<string | null>(null)
  useEffect(() => {
    const id = searchParams.get('accountDeck')
    if (!id || accountDeckLoadedRef.current === id) return
    if (catalog.length === 0) return
    accountDeckLoadedRef.current = id
    void getAccountDeck(Number(id))
      .then((detail) => applySharedDeck(detail.deck))
      .catch(() => {
        /* not found / not signed in — leave the builder as-is */
      })
  }, [searchParams, catalog.length, applySharedDeck])

  const handleShare = async () => {
    const shared = buildSharedDeck()
    if (!shared) return
    const code = await encodeSharedDeck(shared, resolvePrinting)
    const url = buildShareUrl(window.location.origin, code)
    try {
      await navigator.clipboard.writeText(url)
      setShareCopied(true)
      window.setTimeout(() => setShareCopied(false), 2000)
    } catch {
      // Clipboard blocked (e.g. insecure context) — fall back to a copyable prompt.
      window.prompt('Copy this deck link', url)
    }
  }

  const handleLoadSaved = (deck: UnifiedDeck) => {
    // Cloud decks load through the SharedDeck path (fetched fresh, then applied like a deep link);
    // local decks load straight from their stored shape and deep-link to /deckbuilder/<id>.
    if (deck.cloudId != null) {
      void getAccountDeck(deck.cloudId)
        .then((detail) => applySharedDeck(detail.deck))
        .catch(() => {})
      setDecksBrowserOpen(false)
      return
    }
    setDeckName(deck.name)
    setDeckNote(deck.note ?? '')
    setCoverCard(deck.coverCard ?? null)
    setDeckCards(mergeCommanderIntoCards(deck.cards, ...commandersOf(deck)))
    setSideboardCards(deck.sideboard ? { ...deck.sideboard } : {})
    setCompanion(deck.companion ?? null)
    setCommanders(commandersOf(deck))
    setActiveDeckId(deck.id)
    setPinnedPrintings(pinnedPrintingsFromEntries(deck.entries, deck))
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

  // The saved-deck browser's ✎: name and note in one small dialog (the cover is chosen from the
  // deck's own header, where its cards are).
  const [detailsFor, setDetailsFor] = useState<UnifiedDeck | null>(null)
  const handleRenameSaved = (deck: UnifiedDeck) => setDetailsFor(deck)
  const handleSaveDetails = (details: { name: string; note: string }) => {
    const deck = detailsFor
    setDetailsFor(null)
    if (!deck) return
    void updateUnifiedDeckDetails(deck, { name: details.name, note: details.note })
    if (deck.id === activeDeckId || (deck.online && deck.name === deckName)) {
      setDeckName(details.name)
      setDeckNote(details.note)
    }
  }

  const handleDeleteSaved = (deck: UnifiedDeck) => {
    if (!window.confirm(`Delete "${deck.name}"?`)) return
    void removeUnifiedDeck(deck)
    if (deck.id === activeDeckId) handleNew()
  }

  const handleImport = (
    cards: Record<string, number>,
    suggestedName: string | null,
    importedCommanders: readonly string[],
    importedSideboard: Record<string, number> = {},
    importedCompanion: string | null = null,
  ) => {
    // A list that designates a commander is a Commander deck: switch the format when the current
    // one has no commander, or the "clear the commander outside commander formats" guard would
    // drop the designation the moment it lands. One transition, like the shared-deck load, so
    // the new `fmt` and the commanders commit together and that guard never sees them apart.
    const params = new URLSearchParams(searchParams)
    if (importedCommanders.length > 0 && !isCommanderFormat) params.set('fmt', 'COMMANDER')
    const nextSearch = params.toString()
    startTransition(() => {
      setDeckCards(cards)
      // The Arena/MTGO list's "Sideboard"/"SB:" section becomes the wish sideboard (CR 100.4a).
      setSideboardCards(importedSideboard)
      // A `Companion` section names the deck's companion (CR 702.139); it rides on the sideboard.
      setCompanion(importedCompanion)
      // The commander designation rides along when the source list had a Commander
      // section (both, for a partner pair); otherwise reset so a stale value from the previous
      // deck doesn't leak.
      setCommanders(importedCommanders)
      setActiveDeckId(null)
      setPinnedPrintings({})
      setCoverCard(null)
      if (suggestedName) setDeckName(suggestedName)
      navigate(`/deckbuilder${nextSearch ? `?${nextSearch}` : ''}`)
    })
    setImportOpen(false)
  }

  const handleLoadExample = (ex: ExampleDeck) => {
    if (
      Object.keys(deckCards).length > 0 &&
      !window.confirm(`Replace your current deck contents with "${ex.name}"?`)
    ) {
      return
    }
    setDeckCards(mergeCommanderIntoCards({ ...ex.cards }, ...commandersOf(ex)))
    setCommanders(commandersOf(ex))
    // Examples carry no sideboard; clear rather than leave the previous deck's attached, which
    // would follow the example into a save and into a game.
    setSideboardCards({})
    setCompanion(null)
    setActiveDeckId(null)
    // Pre-fill pinned printings from the example. The commander's pin is keyed by name
    // alongside the rest — same shape `pinnedPrintingsFromEntries` produces on saved-deck
    // load, so the art-cache backfill below picks it up automatically.
    setPinnedPrintings({ ...(ex.printings ?? {}), ...commanderPinsOf(ex) })
    setDeckName(ex.name)
    setDeckNote(ex.note ?? '')
    setCoverCard(null)
    // Stamp the example's format into the URL inside the navigate call (rather than via
    // a separate setActiveFormat) so it lands before render. Without this, the next
    // render still sees the old `activeFormat`, the "clear commander when not a commander
    // format" effect fires, and the just-set commander designation gets wiped. Same race
    // and same fix as handleLoadSaved above.
    const params = new URLSearchParams(searchParams)
    if (ex.format) params.set('fmt', ex.format.toUpperCase())
    const suffix = params.toString()
    navigate(`/deckbuilder${suffix ? `?${suffix}` : ''}`)
    setExamplesOpen(false)
  }

  // ----- Render -----

  const totalCards = Object.values(deckCards).reduce((a, b) => a + b, 0)

  // Toolbar overflow menu (compact widths) and, on a phone, which pane of the cards view is showing.
  const [toolsOpen, setToolsOpen] = useState(false)
  const [mobilePane, setMobilePane] = useState<'cards' | 'filters'>('cards')
  const stats = useMemo(() => computeStats(deckCards, catalogIndex), [deckCards, catalogIndex])

  // Lazily fill the art cache for any pinned printing the picker hasn't seen yet.
  // Triggered after a saved deck loads with `entries` carrying pins the user picked in
  // a prior session; one batched call per deck-load (the cache short-circuits names we
  // already know). Failures are silent — the catalog default art still renders.
  useEffect(() => {
    const missing = Object.keys(pinnedPrintings).filter((name) => !(name in pinnedPrintingArt))
    if (missing.length === 0) return
    let cancelled = false
    const params = new URLSearchParams()
    missing.forEach((n) => params.append('names', n))
    fetch(`/api/printings?${params.toString()}`)
      .then((r) => (r.ok ? r.json() : {}))
      .then((data: Record<string, PrintingDTO[]>) => {
        if (cancelled) return
        const updates: Record<string, { imageUri: string | null; backFaceImageUri: string | null }> = {}
        for (const name of missing) {
          const ref = pinnedPrintings[name]
          if (!ref) continue
          const match = data[name]?.find(
            (p) => p.setCode === ref.setCode && p.collectorNumber === ref.collectorNumber,
          )
          if (match) updates[name] = { imageUri: match.imageUri, backFaceImageUri: match.backFaceImageUri }
        }
        if (Object.keys(updates).length > 0) {
          setPinnedPrintingArt((prev) => ({ ...prev, ...updates }))
        }
      })
      .catch(() => {})
    return () => {
      cancelled = true
    }
  }, [pinnedPrintings, pinnedPrintingArt])

  // Currently-open printing picker. The picker renders as a centered modal, so it only
  // needs the card name. Stored at the page level so only one picker is ever open at a
  // time without each row needing to know about the others.
  const [pickerOpenFor, setPickerOpenFor] = useState<{ name: string } | null>(null)
  const handleOpenPicker = useCallback((name: string) => setPickerOpenFor({ name }), [])
  const handleClosePicker = useCallback(() => setPickerOpenFor(null), [])

  // Deck-mode left-rail card preview. The hovered row lives in a store rather than page state, so a
  // hover re-renders the preview alone, not this whole page and every row in it.
  const deckHover = useHoverStore<DeckHoverEntry>()
  const handleDeckHoverEnter = deckHover.set
  const handleDeckHoverLeave = useCallback(() => deckHover.set(null), [deckHover])
  // Crown click: un-designate a commander, or designate a card — as the partner of the single
  // commander when the pair can work (a hint; the server validates the pair), else replacing it.
  const handleToggleCommander = useCallback(
    (name: string) =>
      setCommanders((prev) =>
        toggleCommanderDesignation(prev, name, (first, candidate) => {
          const a = catalogIndex[first]
          const b = catalogIndex[candidate]
          return !!a && !!b && canPairAsCommanders(a, b)
        }),
      ),
    [catalogIndex],
  )
  const handleSuggestBasics = () =>
    suggestLandsForDeck(deckCards, catalogIndex, catalog, setCardCount, applySetPinIfAvailable)

  const closeTools = () => setToolsOpen(false)
  const isEmptyDeck = Object.keys(deckCards).length === 0
  // Flipping to cube mode is a tab switch, not an exit: the deck — saved or not — waits for the way back.
  const stashDeck = () =>
    stashWorkingCopy<DeckWorkingCopy>('deck', {
      activeDeckId, deckName, deckNote, coverCard, deckCards, sideboardCards, companion, commanders, pinnedPrintings,
    })

  return (
    <>
    <PageShell title={<BuilderModeSwitch mode="deck" onLeave={stashDeck} />} fit width="full" plain>
      <div className={styles.toolbar}>
        <BuilderViewToggle
          className={styles.viewToggle}
          noun="Deck"
          count={totalCards}
          view={viewMode === 'deck' ? 'list' : 'cards'}
          onChange={(v) => setViewMode(v === 'list' ? 'deck' : 'cards')}
        />
        {/* Narrow screens only: one tab bar picks the pane and stands in for the view toggle, whose
            "Deck" view is a superset of the narrow deck pane — two bars with a "Deck" each was noise. */}
        <div className={styles.paneTabs} role="tablist" aria-label="Deckbuilder panes">
          <button
            type="button"
            role="tab"
            className={styles.paneTab}
            aria-selected={viewMode === 'cards' && mobilePane !== 'filters'}
            onClick={() => { setViewMode('cards'); setMobilePane('cards') }}
          >
            Cards
          </button>
          <button
            type="button"
            role="tab"
            className={styles.paneTab}
            aria-selected={viewMode === 'cards' && mobilePane === 'filters'}
            onClick={() => { setViewMode('cards'); setMobilePane('filters') }}
          >
            Filters
          </button>
          <button type="button" role="tab" className={styles.paneTab} aria-selected={viewMode === 'deck'} onClick={() => setViewMode('deck')}>
            Deck <span className={styles.paneTabCount}>{totalCards}</span>
          </button>
        </div>
        <div className={styles.toolbarSpacer} />
        {toolsOpen && <div className={styles.toolsBackdrop} onClick={closeTools} aria-hidden />}
        <div className={styles.tools} data-open={toolsOpen}>
          <button
            type="button"
            className={`${styles.toolButton} ${styles.toolButtonCompactOnly}`}
            onClick={() => {
              closeTools()
              reloadUnifiedDecks()
              setDecksBrowserOpen(true)
            }}
            disabled={browserDecks.length === 0}
          >
            My decks{browserDecks.length > 0 ? ` (${browserDecks.length})` : ''}
          </button>
          <button
            type="button"
            className={styles.toolButton}
            onClick={() => {
              closeTools()
              setExamplesOpen(true)
            }}
            disabled={examples.length === 0}
            title={examples.length === 0 ? 'Loading examples…' : 'Load a starter deck'}
          >
            Examples
          </button>
          <button
            type="button"
            className={styles.toolButton}
            onClick={() => {
              closeTools()
              setImportOpen(true)
            }}
          >
            Import
          </button>
          <button
            type="button"
            className={styles.toolButton}
            onClick={() => {
              closeTools()
              setBulkEditOpen(true)
            }}
            disabled={isEmptyDeck}
            title="Edit the deck as text, or copy it out"
          >
            Edit as text
          </button>
          <button
            type="button"
            className={styles.toolButton}
            onClick={() => {
              closeTools()
              void handleShare()
            }}
            disabled={isEmptyDeck}
            title="Copy a shareable link to this deck"
          >
            {shareCopied ? 'Link copied!' : 'Share'}
          </button>
        </div>
        <button
          type="button"
          className={styles.moreButton}
          onClick={() => setToolsOpen((v) => !v)}
          aria-expanded={toolsOpen}
          aria-haspopup="menu"
        >
          {shareCopied ? 'Link copied!' : 'More'}
          <svg width="12" height="12" viewBox="0 0 12 12" aria-hidden focusable="false">
            <path d="M2.5 4.5L6 8l3.5-3.5" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" />
          </svg>
        </button>
        <button type="button" className={styles.newDeckButton} onClick={handleNew}>
          New deck
        </button>
      </div>

      <div
        className={`${styles.workspace} ${viewMode === 'deck' ? styles.workspaceDeckMode : ''}`}
        data-pane={mobilePane}
      >
      {/* Left rail */}
      <aside className={styles.left}>
        <SavedDecksSummary
          decks={browserDecks}
          activeDeckId={activeDeckId}
          onOpen={() => {
            reloadUnifiedDecks()
            setDecksBrowserOpen(true)
          }}
        />
        {viewMode === 'deck' ? (
          <DeckRailPreview
            store={deckHover}
            deckCards={deckCards}
            commanders={commanders}
            pinnedPrintingArt={pinnedPrintingArt}
          />
        ) : (
          <FilterSection
            query={query}
            onQueryChange={setQuery}
            catalog={catalog}
            setInfos={setInfos}
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
                  : `Showing ${displayed.length} of ${filtered.length}`
              }
              filterChip={
                catalogIdentity != null ? (
                  <button
                    type="button"
                    className={restrictToIdentity ? styles.identityChipOn : styles.identityChip}
                    aria-pressed={restrictToIdentity}
                    onClick={() => setRestrictToIdentity((v) => !v)}
                    title={
                      restrictToIdentity
                        ? `Showing only cards inside ${commanders.join(' and ')}'s colour identity. Click to show every card.`
                        : `Show only cards inside ${commanders.join(' and ')}'s colour identity.`
                    }
                  >
                    ♛ Identity {restrictToIdentity ? 'on' : 'off'}
                  </button>
                ) : undefined
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
            <DeckIdentity
              layout="stacked"
              name={deckName}
              onNameChange={setDeckName}
              note={deckNote}
              onNoteChange={setDeckNote}
              cover={coverHero}
              coverChosen={coverChosen}
              onOpenCoverPicker={() => setCoverPickerOpen(true)}
            >
              <DeckFormatPicker
                activeFormat={activeFormat}
                onChange={setActiveFormat}
              />
            </DeckIdentity>

            <DeckListPanel
              deckCards={deckCards}
              catalog={catalogIndex}
              activeFormat={activeFormat}
              onAdd={addCard}
              onRemove={removeCard}
              commanders={commanders}
              showCommanderControls={isCommanderFormat}
              onToggleCommander={handleToggleCommander}
              rowViolations={rowViolations}
              isCommanderFormat={isCommanderFormat}
              onSuggestBasics={handleSuggestBasics}
              pinnedPrintings={pinnedPrintings}
              pinnedPrintingArt={pinnedPrintingArt}
              onOpenPicker={handleOpenPicker}
              rowDragSource="deck"
              onMoveFromSideboard={removeFromSideboard}
            />

            <SideboardPanel
              deckCards={deckCards}
              sideboardCards={sideboardCards}
              catalogIndex={catalogIndex}
              activeFormat={activeFormat}
              isCommanderFormat={isCommanderFormat}
              onAdd={addToSideboard}
              onRemove={removeFromSideboard}
              companion={companionForSave}
              onToggleCompanion={toggleCompanion}
              companionIssues={companionIssues}
              onMoveFromDeck={removeCard}
            />

            <div className={styles.deckSummaryWrap}>
              <DeckSummary
                validation={validation}
                totalCards={totalCards}
                stats={stats}
              />
            </div>

            <DeckActionRow
              saveLabel={saveFlash ?? (activeDeckId ? 'Save' : 'Save deck')}
              canDelete={!!activeDeckId}
              isEmpty={Object.keys(deckCards).length === 0}
              onSave={handleSave}
              onSaveAs={handleSaveAs}
              onDelete={handleDelete}
            />
          </aside>
        </>
      ) : (
        <main className={styles.centerDeck}>
          <DeckIdentity
            layout="inline"
            name={deckName}
            onNameChange={setDeckName}
            note={deckNote}
            onNoteChange={setDeckNote}
            cover={coverHero}
            coverChosen={coverChosen}
            onOpenCoverPicker={() => setCoverPickerOpen(true)}
          >
            <DeckFormatPicker
              activeFormat={activeFormat}
              onChange={setActiveFormat}
            />
          </DeckIdentity>

          <AddCardSearch
            catalog={catalog}
            deckCards={deckCards}
            isCommanderFormat={isCommanderFormat}
            onAdd={addCard}
            onSuggestBasics={handleSuggestBasics}
          />

          <DeckCentricView
            deckCards={deckCards}
            catalog={catalogIndex}
            activeFormat={activeFormat}
            onAdd={addCard}
            onRemove={removeCard}
            commanders={commanders}
            showCommanderControls={isCommanderFormat}
            onToggleCommander={handleToggleCommander}
            rowViolations={rowViolations}
            isCommanderFormat={isCommanderFormat}
            onHoverEnter={handleDeckHoverEnter}
            onHoverLeave={handleDeckHoverLeave}
            pinnedPrintings={pinnedPrintings}
            onOpenPicker={handleOpenPicker}
            onMoveFromSideboard={removeFromSideboard}
          />

          <SideboardPanel
            deckCards={deckCards}
            sideboardCards={sideboardCards}
            catalogIndex={catalogIndex}
            activeFormat={activeFormat}
            isCommanderFormat={isCommanderFormat}
            onAdd={addToSideboard}
            onRemove={removeFromSideboard}
            companion={companionForSave}
            onToggleCompanion={toggleCompanion}
            companionIssues={companionIssues}
            onMoveFromDeck={removeCard}
          />

          <DeckCentricFooter
            validation={validation}
            totalCards={totalCards}
            stats={stats}
            saveLabel={saveFlash ?? (activeDeckId ? 'Save' : 'Save deck')}
            canDelete={!!activeDeckId}
            isEmpty={Object.keys(deckCards).length === 0}
            onSave={handleSave}
            onSaveAs={handleSaveAs}
            onDelete={handleDelete}
          />
        </main>
      )}
      </div>
    </PageShell>

      {importOpen && (
        <ImportDeckModal
          catalog={catalog}
          isCommanderFormat={isCommanderFormat}
          hasExisting={Object.keys(deckCards).length > 0}
          onCancel={() => setImportOpen(false)}
          onImport={handleImport}
        />
      )}

      {bulkEditOpen && (
        <BulkEditDeckModal
          deckCards={deckCards}
          commanders={commanders}
          isCommanderFormat={isCommanderFormat}
          catalog={catalog}
          catalogIndex={catalogIndex}
          onClose={() => setBulkEditOpen(false)}
          onApply={(cards, nextCommanders) => {
            setDeckCards(cards)
            setCommanders(nextCommanders)
            setBulkEditOpen(false)
          }}
        />
      )}

      {decksBrowserOpen && (
        <SavedDecksBrowser
          decks={browserDecks}
          catalog={catalogIndex}
          activeDeckId={activeDeckId}
          onClose={() => setDecksBrowserOpen(false)}
          onLoad={handleLoadSaved}
          onRename={handleRenameSaved}
          onDelete={handleDeleteSaved}
        />
      )}

      {examplesOpen && (
        <ExampleDecksModal
          examples={examples}
          catalog={catalogIndex}
          onCancel={() => setExamplesOpen(false)}
          onLoad={handleLoadExample}
        />
      )}

      {coverPickerOpen && (
        <CoverPicker
          deckCards={deckCards}
          commanders={commanders}
          catalog={catalogIndex}
          artFor={deckArtFor}
          chosen={coverChosen ? coverCard : null}
          onChoose={(name) => {
            setCoverCard(name)
            setCoverPickerOpen(false)
          }}
          onClose={() => setCoverPickerOpen(false)}
        />
      )}

      {detailsFor && (
        <DeckDetailsDialog
          name={detailsFor.name}
          note={detailsFor.note ?? ''}
          onCancel={() => setDetailsFor(null)}
          onSave={handleSaveDetails}
        />
      )}

      {pickerOpenFor && (
        <PrintingPicker
          cardName={pickerOpenFor.name}
          pinned={pinnedPrintings[pickerOpenFor.name]}
          onPick={(ref) => {
            setPinnedPrinting(pickerOpenFor.name, ref)
            handleClosePicker()
          }}
          onClear={() => {
            clearPinnedPrinting(pickerOpenFor.name)
            handleClosePicker()
          }}
          onClose={handleClosePicker}
        />
      )}
    </>
  )
}

// ---------------------------------------------------------------------------
// Shared deck summary + action row — DeckSummary lives in @/components/ui so
// the DeckPicker (quick-game / tournament lobbies) can reuse the same panel.
// ---------------------------------------------------------------------------

type DeckHoverEntry = { name: string; card: CardSummary | undefined }

/**
 * The Deck view's left-rail preview of the hovered row, with a pinned printing's art and F to flip a
 * double-faced card. It subscribes to the hover store itself, so it is the one thing a hover renders.
 */
function DeckRailPreview({
  store,
  deckCards,
  commanders,
  pinnedPrintingArt,
}: {
  store: HoverStore<DeckHoverEntry>
  deckCards: Record<string, number>
  commanders: readonly string[]
  pinnedPrintingArt: Record<string, { imageUri: string | null; backFaceImageUri: string | null }>
}) {
  const hovered = useHovered(store)
  // A row removed while hovered never fires its mouseleave; drop the preview with it.
  const gone = !!hovered && !(hovered.name in deckCards) && !commanders.includes(hovered.name)
  useEffect(() => {
    if (gone) store.set(null)
  }, [gone, store])
  const entry = gone ? null : hovered
  const art = entry ? pinnedPrintingArt[entry.name] : undefined
  const card = entry?.card
    ? art
      ? {
          ...entry.card,
          imageUri: art.imageUri ?? entry.card.imageUri ?? null,
          backFaceImageUri: art.backFaceImageUri ?? entry.card.backFaceImageUri ?? null,
        }
      : entry.card
    : null
  // Keyed by name: a new card starts on its front face.
  return <DeckRailPreviewCard key={entry?.name ?? ''} name={entry?.name ?? null} card={card} />
}

function DeckRailPreviewCard({ name, card }: { name: string | null; card: CardSummary | null }) {
  const dfc = useDfcHoverFlip(
    card
      ? {
          name: card.name,
          imageUri: card.imageUri ?? null,
          isDoubleFaced: card.isDoubleFaced ?? false,
          backFaceName: card.backFaceName ?? null,
          backFaceImageUri: card.backFaceImageUri ?? null,
        }
      : null,
  )
  return (
    <DeckHoverPreview
      name={name ? (dfc.displayName ?? name) : null}
      imageUri={name ? (dfc.displayImageUri ?? card?.imageUri ?? null) : null}
      hint={dfc.hint}
      imageRotateDeg={dfc.imageRotateDeg(card)}
    />
  )
}

function DeckActionRow({
  saveLabel,
  canDelete,
  isEmpty,
  onSave,
  onSaveAs,
  onDelete,
}: {
  saveLabel: string
  canDelete: boolean
  isEmpty: boolean
  onSave: () => void
  onSaveAs: () => void
  onDelete: () => void
}) {
  return (
    <div className={styles.actionRow}>
      <button className={styles.primaryButton} onClick={onSave} disabled={isEmpty}>
        {saveLabel}
      </button>
      <button className={styles.secondaryButton} onClick={onSaveAs} disabled={isEmpty}>
        Save as
      </button>
      <button className={styles.dangerButton} onClick={onDelete} disabled={!canDelete}>
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
  saveLabel,
  canDelete,
  isEmpty,
  onSave,
  onSaveAs,
  onDelete,
}: {
  validation: ValidationResult | null
  totalCards: number
  stats: DeckStats
  saveLabel: string
  canDelete: boolean
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
          {saveLabel}
        </button>
        <button className={styles.secondaryButton} onClick={onSaveAs} disabled={isEmpty}>
          Save as
        </button>
        <button className={styles.dangerButton} onClick={onDelete} disabled={!canDelete}>
          Delete
        </button>
      </div>
    </div>
  )
}

// ---------------------------------------------------------------------------
// Example-decks modal — server-supplied starter lists, click to load.
// ---------------------------------------------------------------------------

function ExampleDecksModal({
  examples,
  catalog,
  onCancel,
  onLoad,
}: {
  examples: ExampleDeck[]
  catalog: Record<string, CardSummary>
  onCancel: () => void
  onLoad: (ex: ExampleDeck) => void
}) {
  return (
    <>
      <div className={styles.importBackdrop} onClick={onCancel} />
      <div className={styles.importDialog} role="dialog" aria-label="Example decks">
        <div className={styles.importHeader}>
          <strong>Load an example deck</strong>
          <button className={styles.linkButton} onClick={onCancel} type="button">
            Close
          </button>
        </div>
        <p className={styles.importHint}>
          Pick a starter list to load into the builder. This replaces the current deck contents.
        </p>
        {examples.length === 0 ? (
          <p className={styles.importHint}>No examples available.</p>
        ) : (
          <div className={styles.browserScroll}>
            <div className={styles.browserGrid}>
              {examples.map((ex) => (
                <DeckTile
                  key={ex.id}
                  name={ex.name}
                  description={ex.description}
                  note={ex.note ?? undefined}
                  {...deckTileModel(ex, catalog)}
                  format={ex.format ?? null}
                  formatTitle={ex.format ? `Built for ${labelForFormat(ex.format)}` : undefined}
                  title={`Load ${ex.name} into the builder`}
                  onClick={() => onLoad(ex)}
                />
              ))}
            </div>
          </div>
        )}
        <div className={styles.importActions}>
          <button className={styles.secondaryButton} onClick={onCancel} type="button">
            Cancel
          </button>
        </div>
      </div>
    </>
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

interface ImportPreviewModel {
  parsed: ParseResult
  resolved: ResolveResult
  /** Where the commander(s) came from when the list didn't say (MTGO / Moxfield exports). */
  inferredFrom: InferredCommanderSource | null
  /** The commanders the import will designate, in catalogue casing. */
  commanders: string[]
}

/**
 * Parse pasted deck text for the import and bulk-edit dialogs: read every line shape the parser
 * knows, resolve a commander the export left implicit ([inferCommanders]), and match the commander
 * and main-deck entries against the catalogue. Commander entries are merged into the resolved list
 * — the commander is also a card of the deck, carrying the crown.
 */
function buildImportPreview(
  text: string,
  catalog: CardSummary[],
  commanderFormat: boolean,
): ImportPreviewModel | null {
  if (text.trim() === '') return null
  const byName = new Map(catalog.map((c) => [c.name.toLowerCase(), c]))
  const { parsed, inferredFrom } = inferCommanders(parseArenaDeckList(text), {
    commanderFormat,
    lookup: (name) => byName.get(name.toLowerCase()),
  })
  const resolved = resolveAgainstCatalog([...parsed.commander, ...parsed.entries], catalog)
  const commanders = designatedCommanders(parsed.commander, canonicalNameIn(catalog))
  return { parsed, resolved, inferredFrom, commanders }
}

function ImportDeckModal({
  catalog,
  isCommanderFormat,
  hasExisting,
  onCancel,
  onImport,
}: {
  catalog: CardSummary[]
  isCommanderFormat: boolean
  hasExisting: boolean
  onCancel: () => void
  onImport: (
    cards: Record<string, number>,
    suggestedName: string | null,
    commanders: readonly string[],
    sideboard: Record<string, number>,
    companion: string | null,
  ) => void
}) {
  const [text, setText] = useState('')

  // Re-parse on every keystroke. The catalog is fixed for the modal lifetime,
  // so only the text input drives the preview.
  const preview = useMemo(
    () => buildImportPreview(text, catalog, isCommanderFormat),
    [text, catalog, isCommanderFormat],
  )

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
    // Resolve the "Sideboard"/"SB:" section against the catalog the same way as the main deck,
    // so the imported sideboard becomes the wish sideboard (unknown cards survive as placeholders).
    // A companion starts the game outside it (CR 702.139a), so it rides on the sideboard too, and
    // the first `Companion` entry becomes the deck's companion.
    const sideResolved = resolveAgainstCatalog(
      [...preview.parsed.sideboard, ...preview.parsed.companion],
      catalog,
    )
    const sideboard = { ...sideResolved.deckCards, ...sideResolved.unmatchedCards }
    // The first two distinct commander entries become the designation (a partner pair); any
    // further ones stay ordinary deck cards. Canonical (catalogue) casing.
    const companionEntry = preview.parsed.companion[0]
    const companion = companionEntry ? (canonicalNameIn(catalog)(companionEntry.name)) : null
    onImport(merged, preview.parsed.deckName ?? null, preview.commanders, sideboard, companion)
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
          Paste an export from <strong>MTG Arena</strong>, <strong>MTGO</strong>,{' '}
          <strong>Moxfield</strong>, <strong>Archidekt</strong>, <strong>TappedOut</strong>,{' '}
          <strong>deckstats</strong> or plain text — the parser detects them. Each line is{' '}
          <code>count name</code>, optionally followed by <code>(SET) NUM</code> and site
          decorations (<code>*F*</code>, <code>#tag</code>, Archidekt <code>[Category]</code>).
          Headers like <code>Deck</code>, <code>Sideboard</code>, <code>Commander</code>,{' '}
          <code>Companion</code> and <code>Maybeboard</code> are recognised. A commander can be
          marked by its section, an Archidekt <code>Commander</code> category or <code>*CMDR*</code>;
          in a 100-card list, a one- or two-card sideboard of legal commanders is read as the
          commander(s).
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

function ImportPreview({ preview }: { preview: ImportPreviewModel }) {
  const { parsed, resolved, inferredFrom, commanders } = preview
  const issueCount = parsed.errors.length + resolved.unmatched.length + resolved.truncated.length
  const countOf = (list: readonly { count: number }[]) => list.reduce((a, e) => a + e.count, 0)
  return (
    <div className={styles.importPreview}>
      <div className={styles.importSummary}>
        <span>
          <strong>{resolved.matchedCards}</strong> matched
          {resolved.totalCards !== resolved.matchedCards
            ? ` of ${resolved.totalCards} (${resolved.totalCards - resolved.matchedCards} placeholder)`
            : ''}
        </span>
        {commanders.length > 0 && (
          <span className={styles.importMutedBadge}>
            ♛ {commanders.join(' + ')}
          </span>
        )}
        {parsed.sideboard.length > 0 && (
          <span className={styles.importMutedBadge}>
            sideboard ({countOf(parsed.sideboard)})
          </span>
        )}
        {issueCount > 0 && <span className={styles.importBadBadge}>{issueCount} issue{issueCount === 1 ? '' : 's'}</span>}
      </div>

      {inferredFrom && (
        <p className={styles.importNote}>
          No commander was marked, so {commanders.join(' and ')}{' '}
          {inferredFrom === 'sideboard' ? 'on the sideboard' : 'in the last block of the list'}{' '}
          {commanders.length > 1 ? 'are' : 'is'} taken as the commander{commanders.length > 1 ? 's' : ''}
          {' '}— the way MTGO and Moxfield exports list them. Click the crown in the deck list to change it.
        </p>
      )}

      {parsed.companion.length > 0 && (
        <p className={styles.importNote}>
          {parsed.companion[0]?.name} becomes the deck&rsquo;s companion: it waits on the sideboard,
          revealed, and you can pay {'{3}'} once a game to put it into your hand. The deck builder
          checks its condition against your deck.
          {parsed.companion.length > 1 &&
            ` Only one companion is allowed — ${parsed.companion.slice(1).map((e) => e.name).join(', ')} stays an ordinary sideboard card.`}
        </p>
      )}

      {parsed.excluded.length > 0 && (
        <details className={styles.importDetails}>
          <summary>Maybeboard skipped ({countOf(parsed.excluded)})</summary>
          <ul>
            {parsed.excluded.map((e) => (
              <li key={`${e.line}-${e.raw}`}>
                Line {e.line}: <code>{e.raw}</code>
              </li>
            ))}
          </ul>
        </details>
      )}

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
        <details className={styles.importDetails} open>
          <summary>Skipped — lines not read as cards ({parsed.errors.length})</summary>
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
  commanders,
  isCommanderFormat,
  catalog,
  catalogIndex,
  onClose,
  onApply,
}: {
  deckCards: Record<string, number>
  commanders: readonly string[]
  isCommanderFormat: boolean
  catalog: CardSummary[]
  catalogIndex: Record<string, CardSummary>
  onClose: () => void
  onApply: (cards: Record<string, number>, commanders: readonly string[]) => void
}) {
  const [format, setFormat] = useState<ExportFormat>('arena')

  const rendered = useMemo(
    () => formatDeck(deckCards, catalogIndex, format, commanders),
    [deckCards, catalogIndex, format, commanders]
  )
  const [text, setText] = useState(rendered.text)
  const [copied, setCopied] = useState(false)

  // Parse-and-resolve is the import flow's, so both modals read the same shapes.
  const preview = useMemo(
    () => buildImportPreview(text, catalog, isCommanderFormat),
    [text, catalog, isCommanderFormat],
  )

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
    const re = formatDeck(deckCards, catalogIndex, next, commanders)
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
    onApply(merged, preview.commanders)
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
  commanders: readonly string[] = [],
): { text: string; totalCards: number; unknownCards: number } {
  type Entry = { name: string; count: number; card: CardSummary | undefined }
  const spells: Entry[] = []
  const lands: Entry[] = []
  const commanderEntries: Entry[] = []
  let totalCards = 0
  let unknownCards = 0
  for (const [name, count] of Object.entries(deck)) {
    if (count <= 0) continue
    totalCards += count
    const card = catalog[name]
    if (!card) unknownCards += count
    const entry: Entry = { name, count, card }
    if (commanders.includes(name)) {
      // Lift the commander(s) out of the main block so re-import doesn't double
      // them. We keep the full count (typically 1) in the Commander section.
      commanderEntries.push(entry)
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
  if (commanderEntries.length > 0) {
    // Designation order (first commander, then its partner) so a re-import keeps it.
    commanderEntries.sort((a, b) => commanders.indexOf(a.name) - commanders.indexOf(b.name))
    lines.push('Commander')
    for (const e of commanderEntries) lines.push(renderLine(e))
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
        {/* Nothing to browse is a note, not a disabled button. */}
        {decks.length === 0 ? (
          <span className={styles.savedNone}>No saved decks yet</span>
        ) : (
          <button className={styles.savedBrowseButton} onClick={onOpen} type="button" title="Browse saved decks">
            {`Browse decks (${decks.length}) →`}
          </button>
        )}
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
  decks: UnifiedDeck[]
  catalog: Record<string, CardSummary>
  activeDeckId: string | null
  onClose: () => void
  onLoad: (d: UnifiedDeck) => void
  onRename: (d: UnifiedDeck) => void
  onDelete: (d: UnifiedDeck) => void
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
  // they'd actually play with. `heroCard` is the deck's rarest non-land card —
  // its Scryfall art crop becomes the tile background so a deck is recognisable
  // by its splashiest spell at a glance.
  const enriched = useMemo(
    () =>
      decks.map((d) => {
        const { total, colors, hero } = deckTileModel(d, catalog)
        const legalFormats = legalityMap[d.id] ?? []
        return { deck: d, total, colors, legalFormats, hero }
      }),
    [decks, catalog, legalityMap]
  )

  const filtered = useMemo(() => {
    const f = filter.trim().toLowerCase()
    let out = enriched
    if (f) {
      out = out.filter(
        (e) => e.deck.name.toLowerCase().includes(f) || (e.deck.note?.toLowerCase().includes(f) ?? false),
      )
    }
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
            placeholder="Search decks by name or note…"
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

        <div className={styles.browserScroll}>
          <div className={styles.browserGrid}>
            {filtered.length === 0 ? (
              <div className={styles.savedEmpty}>
                {decks.length === 0
                  ? 'No saved decks yet. Build one and click Save.'
                  : 'No decks match the current filters.'}
              </div>
            ) : (
              filtered.map(({ deck, total, colors, legalFormats, hero }) => (
                <DeckCard
                  key={deck.id}
                  deck={deck}
                  total={total}
                  colors={colors}
                  legalFormats={legalFormats}
                  isActive={deck.id === activeDeckId}
                  hero={hero}
                  onLoad={onLoad}
                  onRename={onRename}
                  onDelete={onDelete}
                />
              ))
            )}
          </div>
        </div>
      </div>
    </>
  )
}

/**
 * Deckbuilder adapter over the shared {@link DeckTile}: click loads the deck into the editor,
 * hover exposes rename/delete, and the active deck gets the "Editing" ribbon.
 */
function DeckCard({
  deck,
  total,
  colors,
  legalFormats,
  isActive,
  hero,
  onLoad,
  onRename,
  onDelete,
}: {
  deck: UnifiedDeck
  total: number
  colors: string[]
  legalFormats: string[]
  isActive: boolean
  hero: CardSummary | null
  onLoad: (d: UnifiedDeck) => void
  onRename: (d: UnifiedDeck) => void
  onDelete: (d: UnifiedDeck) => void
}) {
  // One chip only: the format the deck was saved as, else the first format it's legal in.
  const shownFormat = deck.format ?? legalFormats[0] ?? null
  return (
    <DeckTile
      name={deck.name}
      note={deck.note}
      total={total}
      colors={colors}
      hero={hero}
      format={shownFormat}
      formatTitle={
        shownFormat
          ? deck.format
            ? `Saved as ${labelForFormat(shownFormat)}`
            : `Legal in ${labelForFormat(shownFormat)}`
          : undefined
      }
      selected={isActive}
      badge={isActive ? 'Editing' : undefined}
      storage={deck.online ? 'cloud' : 'local'}
      title={`Load ${deck.name}`}
      onClick={() => onLoad(deck)}
      actions={
        <>
          <DeckTileActionButton
            onClick={() => onRename(deck)}
            title="Rename or edit note"
            ariaLabel={`Edit name and note of ${deck.name}`}
          >
            ✎
          </DeckTileActionButton>
          <DeckTileActionButton
            onClick={() => onDelete(deck)}
            title="Delete"
            ariaLabel={`Delete ${deck.name}`}
            danger
          >
            ✕
          </DeckTileActionButton>
        </>
      }
    />
  )
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

/**
 * Card preview pane shown in the left rail in deck-mode (Moxfield-style). Replaces
 * the filter menu, which is meaningless when the right-rail catalog grid is hidden.
 * Driven by the lifted hover state in DeckbuilderPage.
 */
// ---------------------------------------------------------------------------
// Deck list panel (right rail)
// ---------------------------------------------------------------------------

function DeckListPanel({
  deckCards,
  catalog,
  activeFormat,
  onAdd,
  onRemove,
  commanders,
  showCommanderControls,
  onToggleCommander,
  rowViolations,
  isCommanderFormat,
  onSuggestBasics,
  pinnedPrintings,
  pinnedPrintingArt,
  onOpenPicker,
  hideBasicLandHelpers = false,
  rowDragSource,
  onMoveFromSideboard,
}: {
  deckCards: Record<string, number>
  catalog: Record<string, CardSummary>
  activeFormat: string | null
  onAdd: (card: CardSummary) => void
  onRemove: (name: string) => void
  commanders: readonly string[]
  showCommanderControls: boolean
  onToggleCommander: (name: string) => void
  rowViolations: Map<string, Set<string>>
  isCommanderFormat: boolean
  onSuggestBasics: () => void
  pinnedPrintings: Record<string, PrintingRef>
  pinnedPrintingArt: Record<string, { imageUri: string | null; backFaceImageUri: string | null }>
  onOpenPicker: (name: string) => void
  // Sideboard use: suppress the "Suggest basic lands" button and the 0-count basic placeholders.
  hideBasicLandHelpers?: boolean
  // When set, rows are draggable with this source tag (the main deck uses 'deck' so rows can be
  // dragged into the sideboard). Omitted for the sideboard's own list.
  rowDragSource?: CardDragSource
  // When set, the list becomes a drop target: catalog tiles add a copy, and a card dragged out of
  // the sideboard is *moved* into the deck (this callback removes the sideboard copy). Omitted for
  // the sideboard's own list, which is never a deck drop target.
  onMoveFromSideboard?: (name: string) => void
}) {
  const grouped = useMemo(
    () => groupForDeckList(deckCards, catalog, commanders, hideBasicLandHelpers),
    [deckCards, catalog, commanders, hideBasicLandHelpers],
  )
  const commanderCards = useCommanderCards(commanders, catalog)
  const [hoverCard, setHoverCard] = useState<CardSummary | null>(null)
  const [hoverName, setHoverName] = useState<string | null>(null)
  // Prefer the pinned printing's art when one exists for the hovered row; fall back to
  // the catalog default. Same fallback applies to the back face for DFCs — pinned printings
  // always carry both faces if applicable, so a missing field really does mean "no override".
  const hoverArtOverride = hoverName ? pinnedPrintingArt[hoverName] : undefined
  const effectiveHoverCard = hoverCard
    ? hoverArtOverride
      ? {
          ...hoverCard,
          imageUri: hoverArtOverride.imageUri ?? hoverCard.imageUri,
          backFaceImageUri: hoverArtOverride.backFaceImageUri ?? hoverCard.backFaceImageUri,
        }
      : hoverCard
    : null
  const dfc = useDfcHoverFlip(
    effectiveHoverCard
      ? {
          name: effectiveHoverCard.name,
          imageUri: effectiveHoverCard.imageUri ?? null,
          isDoubleFaced: effectiveHoverCard.isDoubleFaced ?? false,
          backFaceName: effectiveHoverCard.backFaceName ?? null,
          backFaceImageUri: effectiveHoverCard.backFaceImageUri ?? null,
        }
      : null,
  )
  const resetDfcFlip = dfc.resetFlip

  // Stable identities so memoized DeckRow children skip re-render when hover
  // state changes. Functional setters keep us correct without putting hoverName
  // in the deps array.
  const handleEnter = useCallback(
    (entry: { name: string; card: CardSummary | undefined }) => {
      setHoverName((prev) => {
        if (prev !== entry.name) resetDfcFlip()
        return entry.name
      })
      setHoverCard(entry.card ?? null)
    },
    [resetDfcFlip],
  )
  const handleLeave = useCallback(() => {
    setHoverName(null)
    setHoverCard(null)
  }, [])

  useEffect(() => {
    if (hoverName && !(hoverName in deckCards)) {
      setHoverName(null)
      setHoverCard(null)
    }
  }, [deckCards, hoverName])

  const hasDeck = Object.keys(deckCards).length > 0

  // Deck-list drop target: catalog tiles add a copy; a card dragged out of the sideboard is moved
  // into the deck. Enabled only when `onMoveFromSideboard` is supplied (i.e. the main deck list,
  // not the sideboard's own row list).
  const acceptsDrops = onMoveFromSideboard !== undefined
  const { dragActive, dropHandlers } = useCardDropZone((payload) => {
    // A deck card dropped back onto the deck is a no-op.
    if (payload.source === 'deck') return
    const card = catalog[payload.name]
    if (!card) return
    if (payload.source === 'sideboard') onMoveFromSideboard?.(payload.name)
    onAdd(card)
  })

  return (
    <div
      className={`${styles.deckList} ${acceptsDrops && dragActive ? styles.deckListDrop : ''}`}
      {...(acceptsDrops ? dropHandlers : {})}
    >
      {grouped.map((group) => (
        <div key={group.label} className={styles.deckGroup}>
          <div className={styles.deckGroupHeader}>
            <h3 className={styles.deckGroupLabel}>
              {group.label} ({group.entries.reduce((a, e) => a + e.count, 0)})
            </h3>
            {group.label === 'Lands' && !hideBasicLandHelpers && (
              <button
                type="button"
                className={styles.basicLandsSuggest}
                onClick={onSuggestBasics}
                disabled={!hasDeck}
                title="Auto-fill basic lands from your deck's mana curve and color requirements"
              >
                Suggest basic lands
              </button>
            )}
          </div>
          {group.entries.map((entry) => (
            <DeckRow
              key={entry.name}
              entry={entry}
              activeFormat={activeFormat}
              commanders={commanders}
              commanderCards={commanderCards}
              showCommanderControls={showCommanderControls}
              isCommanderFormat={isCommanderFormat}
              rowViolations={rowViolations}
              onAdd={onAdd}
              onRemove={onRemove}
              onToggleCommander={onToggleCommander}
              onEnter={handleEnter}
              onLeave={handleLeave}
              pinnedPrinting={pinnedPrintings[entry.name]}
              onOpenPicker={onOpenPicker}
              {...(rowDragSource ? { dragSource: rowDragSource } : {})}
            />
          ))}
        </div>
      ))}
      {grouped.length === 0 && (
        <p style={{ color: 'var(--text-muted)', fontSize: '0.8rem', textAlign: 'center', margin: 'var(--space-4) 0' }}>
          Click cards in the grid to add them — or drag them here.
        </p>
      )}
      <HoverFollowPreview
        name={hoverName ? (dfc.displayName ?? hoverName) : null}
        imageUri={hoverName ? (dfc.displayImageUri ?? effectiveHoverCard?.imageUri ?? null) : null}
        hint={dfc.hint}
        imageRotateDeg={dfc.imageRotateDeg(effectiveHoverCard)}
      />
    </div>
  )
}

// ---------------------------------------------------------------------------
// Sideboard panel — the constructed "outside the game" list (CR 100.4a) that
// wish effects (Burning Wish, …) fetch from. Reuses AddCardSearch for input and
// DeckListPanel for the rows, with commander/printing/validation chrome turned
// off (a sideboard has no commander, and the 4-of cap is enforced combined with
// the main deck at add-time). Limited decks never use this — their sideboard is
// the pool − maindeck complement, derived server-side (CR 100.4b).
// ---------------------------------------------------------------------------

const SB_NOOP = () => {}
const SB_NOOP_NAME = (_name: string) => {}
const SB_NO_VIOLATIONS: Map<string, Set<string>> = new Map()
const SB_NO_COMMANDERS: readonly string[] = []
const SB_NO_PRINTINGS: Record<string, PrintingRef> = {}
const SB_NO_PRINTING_ART: Record<string, { imageUri: string | null; backFaceImageUri: string | null }> = {}


function SideboardPanel({
  deckCards,
  sideboardCards,
  catalogIndex,
  activeFormat,
  isCommanderFormat,
  onAdd,
  onRemove,
  onMoveFromDeck,
  companion,
  onToggleCompanion,
  companionIssues,
}: {
  deckCards: Record<string, number>
  sideboardCards: Record<string, number>
  catalogIndex: Record<string, CardSummary>
  activeFormat: string | null
  isCommanderFormat: boolean
  onAdd: (card: CardSummary) => void
  onRemove: (name: string) => void
  // Move one copy out of the main deck (used when a deck row is dragged into the sideboard).
  onMoveFromDeck: (name: string) => void
  /** The sideboard card marked as the deck's companion (CR 702.139), or null. */
  companion: string | null
  onToggleCompanion: (name: string) => void
  /** The server validator's messages about the companion — its condition, identity, copies. */
  companionIssues: readonly string[]
}) {
  const total = Object.values(sideboardCards).reduce((a, b) => a + b, 0)
  const outside = useMemo(
    () => summarizeOutsideTheGame(deckCards, sideboardCards, catalogIndex),
    [deckCards, sideboardCards, catalogIndex],
  )
  // Sideboard cards with a companion ability, which the deck may reveal as its companion.
  const companionCandidates = Object.keys(sideboardCards)
    .map((name) => catalogIndex[name])
    .filter((card): card is CardSummary => !!card?.companionCondition)

  const { dragActive, dropHandlers } = useCardDropZone((payload) => {
    // A sideboard card dropped back onto the sideboard is a no-op.
    if (payload.source === 'sideboard') return
    const card = catalogIndex[payload.name]
    if (!card) return
    // Dragged out of the main deck → move it (the combined 4-of cap means a plain add could
    // otherwise be a silent no-op when the deck already holds the max).
    if (payload.source === 'deck') onMoveFromDeck(payload.name)
    onAdd(card)
  })

  return (
    <section
      className={`${styles.sideboardPanel} ${dragActive ? styles.sideboardPanelDrop : ''}`}
      aria-label="Sideboard"
      {...dropHandlers}
    >
      <div className={styles.sideboardHeader}>
        <h3 className={styles.sideboardTitle}>
          Sideboard
          {total > 0 && (
            <span className={total > CONSTRUCTED_SIDEBOARD_MAX ? styles.sideboardOverCap : undefined}>
              {` (${total}/${CONSTRUCTED_SIDEBOARD_MAX})`}
            </span>
          )}
        </h3>
        <span className={styles.sideboardHint}>
          Cards you own outside the game. Drag cards here from the grid or your deck list.
        </span>
      </div>
      <OutsideTheGameNote summary={outside} />
      {companionCandidates.length > 0 && (
        <div className={styles.companionStrip} aria-label="Companion">
          {companionCandidates.map((card) => {
            const chosen = companion === card.name
            return (
              <button
                key={card.name}
                type="button"
                className={`${styles.companionToggle} ${chosen ? styles.companionToggleOn : ''}`}
                aria-pressed={chosen}
                title={`Companion — ${card.companionCondition}`}
                onClick={() => onToggleCompanion(card.name)}
              >
                {chosen ? '★ Companion: ' : '☆ Make companion: '}
                {card.name}
              </button>
            )
          })}
          {companion && (
            <span className={styles.sideboardHint}>
              Revealed before the game; pay {'{3}'} once, any time you could cast a sorcery, to put it into your
              hand. Condition: {catalogIndex[companion]?.companionCondition}
            </span>
          )}
          {companionIssues.map((message) => (
            <span key={message} className={styles.companionIssue}>
              {message}
            </span>
          ))}
        </div>
      )}
      {total > 0 ? (
        <DeckListPanel
          deckCards={sideboardCards}
          catalog={catalogIndex}
          activeFormat={activeFormat}
          onAdd={onAdd}
          onRemove={onRemove}
          commanders={SB_NO_COMMANDERS}
          showCommanderControls={false}
          onToggleCommander={SB_NOOP_NAME}
          rowViolations={SB_NO_VIOLATIONS}
          isCommanderFormat={isCommanderFormat}
          onSuggestBasics={SB_NOOP}
          pinnedPrintings={SB_NO_PRINTINGS}
          pinnedPrintingArt={SB_NO_PRINTING_ART}
          onOpenPicker={SB_NOOP_NAME}
          hideBasicLandHelpers
          rowDragSource="sideboard"
        />
      ) : (
        <p className={styles.sideboardEmpty}>
          {outside.reachers.length > 0
            ? 'Drag cards here from the grid or your deck list.'
            : 'Drag cards here to set them aside. Optional — most decks leave this empty.'}
        </p>
      )}
    </section>
  )
}

/**
 * The sideboard's one job in a game is feeding the cards that reach outside the game — Learn, the
 * wishes, Karn. Say so when the deck has any: an empty sideboard turns every one of them into a
 * blank, and a Learn deck with no Lessons only ever rummages. Silent for a deck with none, where
 * the sideboard really is optional.
 */
function OutsideTheGameNote({ summary }: { summary: OutsideTheGameSummary }) {
  if (summary.reachers.length === 0) {
    if (summary.size <= CONSTRUCTED_SIDEBOARD_MAX) return null
    return (
      <p className={`${styles.sideboardReach} ${styles.sideboardReachWarn}`} role="status">
        A constructed sideboard holds at most {CONSTRUCTED_SIDEBOARD_MAX} cards.
      </p>
    )
  }
  const who = namesPhrase(summary.reachers)
  const verb = summary.reachers.length === 1 ? 'reaches' : 'reach'
  let message: string
  let warn = true
  if (summary.size === 0) {
    message = `${who} ${verb} outside the game, but your sideboard is empty — add up to ${CONSTRUCTED_SIDEBOARD_MAX} cards for ${summary.reachers.length === 1 ? 'it' : 'them'} to fetch.`
  } else if (summary.learns && summary.lessons === 0) {
    message = `${who} ${verb} outside the game. Learn only finds Lessons, and none are here yet.`
  } else if (summary.size > CONSTRUCTED_SIDEBOARD_MAX) {
    message = `${who} ${verb} outside the game. A constructed sideboard holds at most ${CONSTRUCTED_SIDEBOARD_MAX} cards — trim it to the ones worth fetching.`
  } else {
    warn = false
    const lessons = summary.learns ? ` ${summary.lessons} Lesson${summary.lessons === 1 ? '' : 's'} for Learn.` : ''
    message = `${who} can fetch from these ${summary.size} card${summary.size === 1 ? '' : 's'}.${lessons}`
  }
  return (
    <p className={`${styles.sideboardReach} ${warn ? styles.sideboardReachWarn : ''}`} role="status">
      <span aria-hidden="true">✦ </span>
      {message}
    </p>
  )
}

// ---------------------------------------------------------------------------
// Single deck-list row. Extracted so both DeckListPanel (right-rail single
// column) and DeckCentricView (multi-column Moxfield-style layout) render the
// same +/-/count/name/crown/cost shape with the same validation visuals.
// ---------------------------------------------------------------------------

// Memoized: hover state lives one level up; without memo every row re-renders
// on every hover transition. `onEnter` takes the row's entry as an argument so
// the parent can supply a stable handler (the per-row closure that binds the
// entry is created here, where it's cheap because memo prevents re-renders
// when hover state changes).
const DeckRow = memo(function DeckRow({
  entry,
  activeFormat,
  commanders,
  commanderCards,
  showCommanderControls,
  isCommanderFormat,
  rowViolations,
  onAdd,
  onRemove,
  onToggleCommander,
  onEnter,
  onLeave,
  pinnedPrinting,
  onOpenPicker,
  dragSource,
}: {
  entry: { name: string; count: number; card: CardSummary | undefined }
  activeFormat: string | null
  /** The designated commander(s) — two for a partner pair. */
  commanders: readonly string[]
  /** Catalogue entries of [commanders] that the catalogue knows, for the partner crown hint. */
  commanderCards: readonly CardSummary[]
  showCommanderControls: boolean
  isCommanderFormat: boolean
  rowViolations: Map<string, Set<string>>
  onAdd: (card: CardSummary) => void
  onRemove: (name: string) => void
  onToggleCommander: (name: string) => void
  onEnter: (entry: { name: string; card: CardSummary | undefined }) => void
  onLeave: () => void
  pinnedPrinting: PrintingRef | undefined
  onOpenPicker: (name: string) => void
  // When set, the row can be dragged (e.g. into the sideboard) carrying this source tag.
  dragSource?: CardDragSource
}) {
  const illegal =
    activeFormat !== null &&
    !!entry.card?.legalFormats &&
    entry.card.legalFormats.length > 0 &&
    !entry.card.legalFormats.includes(activeFormat.toUpperCase())
  const unknown = !entry.card
  const isCommanderRow = showCommanderControls && commanders.includes(entry.name)
  const commander = commanders[0] ?? null
  // Crown eligibility is a hint off the server's own `canBeCommander` flag (legendary creatures,
  // and cards that say they can be your commander — not planeswalkers in general), plus a
  // legendary Background as the partner of a "Choose a Background" commander. The server's
  // validation is the authoritative gate, pairs included (INVALID_COMMANDER_PAIR).
  const canBeCommander = !!entry.card && canCrown(entry.card, isCommanderRow, commanderCards)
  // Whether a crown click would add this card as the partner of the single commander.
  const firstCommanderCard = commanderCards.length === 1 && commanders.length === 1 ? commanderCards[0] : undefined
  const pairsAsPartner =
    !isCommanderRow && !!entry.card && !!firstCommanderCard && canPairAsCommanders(firstCommanderCard, entry.card)
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
    // Commander format always renders the crown affordance on each row, which adds a 7th
    // grid child. Switch to the 7-column template so the mana cost doesn't wrap to a new line.
    showCommanderControls ? styles.deckRowWithCrown : '',
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
    violationReasons.push(
      commanders.length > 1
        ? `Outside ${commanders.join(' and ')}'s combined color identity`
        : `Outside ${commander}'s color identity`,
    )
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
      {...(dragSource
        ? {
            draggable: true,
            onDragStart: (e: React.DragEvent) => setCardDragData(e, entry.name, dragSource),
          }
        : {})}
      onMouseEnter={() => onEnter(entry)}
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
        {pinnedPrintedName(entry.card, pinnedPrinting) ?? entry.name}
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
              : pairsAsPartner
              ? `Add ${entry.name} as second commander`
              : `Set ${entry.name} as commander`
          }
          title={
            unknown
              ? 'Card not implemented'
              : !canBeCommander
              ? 'Only legendary creatures, or cards that say they can be your commander, can be commanders. A Background joins a "Choose a Background" commander.'
              : isCommanderRow
              ? 'Commander — click to unset'
              : pairsAsPartner
              ? `Add as second commander alongside ${commander ?? 'your commander'} (partner pair)`
              : commander
              ? `Set as commander (replaces ${commanders.join(' and ')})`
              : 'Set as commander'
          }
        >
          ♛
        </button>
      )}
      {entry.card && entry.count > 0 && (
        <button
          type="button"
          className={`${styles.deckRowPrintingChip} ${
            pinnedPrinting ? styles.deckRowPrintingChipActive : ''
          }`}
          onClick={(e) => {
            e.stopPropagation()
            onOpenPicker(entry.name)
          }}
          title={
            pinnedPrinting
              ? `Pinned printing: ${pinnedPrinting.setCode} #${pinnedPrinting.collectorNumber} — click to change`
              : 'Pick a specific printing'
          }
          aria-label={`Pick printing for ${entry.name}`}
        >
          {pinnedPrinting ? pinnedPrinting.setCode : '◧'}
        </button>
      )}
      <span className={styles.deckRowCost}>
        <ManaCost cost={entry.card?.manaCost || null} size={11} />
      </span>
    </div>
  )
})

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
      if (c.basicLand) continue
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
    setHoverCard(null)
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
        hint={dfc.hint}
        imageRotateDeg={dfc.imageRotateDeg(hoverCard)}
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
  commanders,
  showCommanderControls,
  onToggleCommander,
  rowViolations,
  isCommanderFormat,
  onHoverEnter,
  onHoverLeave,
  pinnedPrintings,
  onOpenPicker,
  onMoveFromSideboard,
}: {
  deckCards: Record<string, number>
  catalog: Record<string, CardSummary>
  activeFormat: string | null
  onAdd: (card: CardSummary) => void
  onRemove: (name: string) => void
  commanders: readonly string[]
  showCommanderControls: boolean
  onToggleCommander: (name: string) => void
  rowViolations: Map<string, Set<string>>
  isCommanderFormat: boolean
  onHoverEnter: (entry: { name: string; card: CardSummary | undefined }) => void
  onHoverLeave: () => void
  pinnedPrintings: Record<string, PrintingRef>
  onOpenPicker: (name: string) => void
  // Drop target: a card dragged out of the sideboard is moved into the deck (this callback removes
  // the sideboard copy). The deck-centric layout has no catalog grid, so in practice only sideboard
  // drags land here, but catalog drags are handled too for parity with the cards layout.
  onMoveFromSideboard: (name: string) => void
}) {
  const grouped = useMemo(
    () => groupByCardType(deckCards, catalog, commanders),
    [deckCards, catalog, commanders],
  )
  const commanderCards = useCommanderCards(commanders, catalog)

  const { dragActive, dropHandlers } = useCardDropZone((payload) => {
    if (payload.source === 'deck') return
    const card = catalog[payload.name]
    if (!card) return
    if (payload.source === 'sideboard') onMoveFromSideboard(payload.name)
    onAdd(card)
  })

  // Use the raw deck size (not the grouped-bucket count) because the Lands group now always
  // synthesizes the 5 basic-land rows even at count 0 — so an empty deck would otherwise still
  // produce one non-empty group and we'd never show the empty state.
  const isEmpty = Object.keys(deckCards).length === 0 && commanders.length === 0
  if (isEmpty) {
    return (
      <div
        className={`${styles.deckCentricEmpty} ${dragActive ? styles.deckListDrop : ''}`}
        {...dropHandlers}
      >
        <p className={styles.deckCentricEmptyText}>
          Your deck is empty. Type a card name in the search bar above, or switch to{' '}
          <strong>Search cards</strong> to browse the catalog.
        </p>
      </div>
    )
  }

  return (
    <div className={`${styles.deckCentric} ${dragActive ? styles.deckListDrop : ''}`} {...dropHandlers}>
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
                commanders={commanders}
                commanderCards={commanderCards}
                showCommanderControls={showCommanderControls}
                isCommanderFormat={isCommanderFormat}
                rowViolations={rowViolations}
                onAdd={onAdd}
                onRemove={onRemove}
                onToggleCommander={onToggleCommander}
                onEnter={onHoverEnter}
                onLeave={onHoverLeave}
                pinnedPrinting={pinnedPrintings[entry.name]}
                onOpenPicker={onOpenPicker}
                dragSource="deck"
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
 * doesn't know its target format, so no minDeckSize is passed — basics scale
 * purely off the spell curve and the current spell count. The validation panel
 * surfaces undersized decks so the user can re-run Suggest after adding more
 * spells.
 */
function suggestLandsForDeck(
  deck: Record<string, number>,
  catalog: Record<string, CardSummary>,
  catalogList: CardSummary[],
  setCount: (name: string, count: number) => void,
  onBasicAdded?: (name: string) => void,
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
  for (const basic of availableBasics) {
    const count = result[basic.name] ?? 0
    setCount(basic.name, count)
    if (count > 0) onBasicAdded?.(basic.name)
  }
}

// ---------------------------------------------------------------------------
// Stats / helpers — DeckSummary, ManaCurveBars, computeDeckStats, statusClass,
// and statusLabel are imported from `@/components/ui/DeckSummary`.
// ---------------------------------------------------------------------------

interface DeckGroup {
  label: string
  entries: Array<{ name: string; count: number; card: CardSummary | undefined }>
}

/** The deck list's leading Commander group — both of a partner pair, in designation order. */
function commanderGroup(entries: DeckGroup['entries'], commanders: readonly string[]): DeckGroup {
  const ordered = [...entries].sort((a, b) => commanders.indexOf(a.name) - commanders.indexOf(b.name))
  return { label: ordered.length > 1 ? 'Commanders' : 'Commander', entries: ordered }
}

/** The catalogue entries of the designated commanders, memoised so memo'd rows don't re-render. */
function useCommanderCards(
  commanders: readonly string[],
  catalog: Record<string, CardSummary>,
): readonly CardSummary[] {
  return useMemo(
    () => commanders.flatMap((name) => {
      const card = catalog[name]
      return card ? [card] : []
    }),
    [commanders, catalog],
  )
}

function groupForDeckList(
  deck: Record<string, number>,
  catalog: Record<string, CardSummary>,
  commanders: readonly string[],
  // When true (sideboard use), don't inject the 0-count basic-land sticky rows — a sideboard
  // shouldn't show an empty mana base. Basics actually present in the sideboard still appear.
  hideEmptyBasics = false,
): DeckGroup[] {
  const spells: DeckGroup['entries'] = []
  const lands: DeckGroup['entries'] = []
  // Commander row (if any). Pulled out of the spell/land buckets and rendered as its own
  // group at the top of the list — even if the commander is technically a creature/planeswalker
  // that would otherwise sort under Spells, it should always lead the deck list visually.
  const commanderEntries: DeckGroup['entries'] = []
  for (const [name, count] of Object.entries(deck)) {
    if (count <= 0) continue
    const card = catalog[name]
    // Basics get appended below as sticky rows in canonical W/U/B/R/G order so 0-count basics
    // still appear; skip them here to avoid double-listing the >0 ones.
    if (card?.basicLand) continue
    const entry = { name, count, card }
    if (commanders.includes(name)) {
      commanderEntries.push(entry)
      continue
    }
    if (card?.cardTypes.includes('LAND')) lands.push(entry)
    else spells.push(entry)
  }
  spells.sort(byCmcThenName)
  lands.sort(byCmcThenName)
  const basicEntries: DeckGroup['entries'] = []
  for (const basicName of BASIC_LAND_ORDER) {
    const card = catalog[basicName]
    if (!card) continue
    const count = deck[basicName] ?? 0
    if (hideEmptyBasics && count <= 0) continue
    basicEntries.push({ name: basicName, count, card })
  }
  const allLands = [...lands, ...basicEntries]
  const groups: DeckGroup[] = []
  if (commanderEntries.length > 0) groups.push(commanderGroup(commanderEntries, commanders))
  if (spells.length > 0) groups.push({ label: 'Spells', entries: spells })
  if (allLands.length > 0) groups.push({ label: 'Lands', entries: allLands })
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
  commanders: readonly string[],
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
  const commanderEntries: DeckGroup['entries'] = []
  const seenNames = new Set<string>()
  for (const [name, count] of Object.entries(deck)) {
    if (count <= 0) continue
    const card = catalog[name]
    seenNames.add(name)
    const entry = { name, count, card }
    if (commanders.includes(name)) {
      commanderEntries.push(entry)
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
  if (commanderEntries.length > 0) groups.push(commanderGroup(commanderEntries, commanders))
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

