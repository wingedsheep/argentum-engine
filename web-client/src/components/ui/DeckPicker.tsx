/**
 * DeckPicker — tabbed deck selector for the quick-game and tournament lobby flows.
 *
 * Tabs:
 *   - My decks: the unified deck library (cloud + browser), as a full-art deck gallery
 *   - Examples: server-supplied starter lists, same gallery tiles
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
 *   GET  /api/cards            — slim metadata for every card (validation + stats)
 *   GET  /api/decks/examples   — the starter decks shown in the Examples tab
 *   POST /api/decks/validate   — authoritative validation pass when a list is non-empty
 */
import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import type { PrintingRef } from '@/types'
import type { AvailableSet } from '@/types/messages'
import {
  mergeCommanderIntoCards,
  stripCommanderFromCards,
} from '@/store/deckLibrary'
import { type UnifiedDeck, useUnifiedDecks } from '@/store/useUnifiedDecks'
import { useSaveDeck } from '@/store/useSaveDeck'
import {
  labelForFormat,
  useDeckLegalFormats,
} from '@/utils/deckLegality'
import {
  DeckSummary,
  computeDeckStats,
  type DeckValidationResult,
} from './DeckSummary'
import {
  DeckTile,
  DeckTileActionButton,
  deckColors,
  rarestCard,
} from '@/components/deck/DeckTile'
import { parseArenaDeckList } from '../deckbuilder/parseArenaDeck'
import type { CardSummary } from '../deckbuilder/cardFilter'
import { SetSelector, rollRandomSet } from './SetSelector'
import styles from './DeckPicker.module.css'

export type DeckPickerTab = 'saved' | 'examples' | 'paste' | 'random'
type Tab = DeckPickerTab

export interface DeckPickerProps {
  /**
   * Emitted whenever the deck content changes. The deck list is the full deck (including the
   * commander, when applicable, merged into the card counts). The optional `commander` is the
   * designated commander card name when the active selection is a saved deck with one. Quick
   * Game / Premade-Decks tournament flows pass it through to the server so commander-shape
   * formats can wire the engine into Format.Commander.
   */
  onDeckChange: (
    deckList: Record<string, number>,
    commander?: string | null,
    sideboard?: Record<string, number>,
  ) => void
  onValidityChange?: (isValid: boolean) => void
  /** Set selection for quick-game Random decks. Empty means the server chooses a set. */
  onSetCodesChange?: (setCodes: readonly string[]) => void
  /** Initial set code for the Random tab — used to re-hydrate after a reconnect. */
  initialSetCode?: string | null
  initialSetCodes?: readonly string[]
  /** Available sets for the Random tab set picker. Empty list hides the picker. */
  availableSets?: ReadonlyArray<AvailableSet>
  disabled?: boolean
  /**
   * Tabs to expose. Defaults to all four. Pass a subset to restrict the picker — e.g. the
   * Premade Decks tournament lobby uses `['saved', 'examples', 'paste']` (no Random).
   */
  tabs?: ReadonlyArray<Tab>
  /**
   * Optional deck-construction format the picker is constrained to. When set:
   *   - Saved decks are filtered to only those legal in this format.
   *   - Validation requests pass the format so per-card legality errors surface.
   * Null/undefined = no restriction.
   */
  format?: string | null
  /**
   * Optional controlled tab. Pass it together with {@link onTabChange} to hoist the picker's tab
   * out of the picker — the unified lobby does this so that "Random pool" on its **Cards** axis
   * and the picker's Random tab are the same control rather than two things that can disagree.
   * Leave both undefined and the picker owns its tab as before.
   */
  tab?: DeckPickerTab | undefined
  /** Fired for every tab change, whether the user clicked it or the picker moved itself. */
  onTabChange?: (tab: DeckPickerTab) => void
  /**
   * A saved deck to preselect, by name, once the library has hydrated.
   *
   * By *name* rather than id because that is what a saved setup can portably store — the unified
   * library already merges cloud and local decks on `name.toLowerCase()`, so a name survives signing
   * in where `cloud:7` would not. See `lobbyRecipe.ts`.
   *
   * Deliberately inert unless the picker is sitting on Saved with nothing chosen: it must never
   * fight a Random tab the caller asked for, nor overwrite a deck the player has already picked.
   */
  initialSavedDeckName?: string | undefined
  /**
   * Fired with the name of the selected saved deck, or null when the selection isn't one.
   *
   * This is how a lobby records *which* deck was played without holding the card list — the half of
   * `onDeckChange` a recipe needs and that one can't give, since a decklist has no identity.
   */
  onSavedDeckNameChange?: (name: string | null) => void
}

interface ExampleDeck {
  id: string
  name: string
  description: string
  cards: Record<string, number>
  /** Deck format this example is built for. Null = no format hint. */
  format?: string | null
  /** Designated commander name for commander-shape examples. */
  commander?: string | null
  /** Preferred printing per card name (sparse). Picker loads these as pinned printings. */
  printings?: Record<string, PrintingRef> | null
  /** Preferred printing for the commander, when one is designated. */
  commanderPrinting?: PrintingRef | null
}

type ValidationResult = DeckValidationResult

function parseDeckText(text: string): { cards: Record<string, number>; deckName?: string } {
  // Delegate to the shared Arena/Moxfield/plain-text parser so section headers
  // (`About`, `Deck`, `Sideboard`, …) and Arena's `Name <deck-name>` metadata
  // line don't end up as bogus card entries.
  const parsed = parseArenaDeckList(text)
  const cards: Record<string, number> = {}
  for (const entry of parsed.entries) {
    cards[entry.name] = (cards[entry.name] ?? 0) + entry.count
  }
  // Tolerate the "bare card name = 1 copy" shorthand the old parser supported,
  // since the picker's textarea never required a leading count.
  for (const err of parsed.errors) {
    if (err.reason !== 'unrecognised line format') continue
    const name = err.raw.trim()
    if (!name) continue
    cards[name] = (cards[name] ?? 0) + 1
  }
  return parsed.deckName !== undefined ? { cards, deckName: parsed.deckName } : { cards }
}

function formatDeckText(cards: Record<string, number>): string {
  return Object.entries(cards)
    .filter(([, n]) => n > 0)
    .map(([name, n]) => `${n} ${name}`)
    .join('\n')
}

const ALL_TABS: ReadonlyArray<Tab> = ['saved', 'examples', 'paste', 'random']

/** Formats the server can't autobuild for; a Random seat there still gets a sealed pool. */
const COMMANDER_SHAPES = ['COMMANDER', 'BRAWL', 'STANDARD_BRAWL']

export function DeckPicker({
  onDeckChange,
  onValidityChange,
  onSetCodesChange,
  initialSetCode = null,
  initialSetCodes,
  availableSets = [],
  disabled = false,
  tabs = ALL_TABS,
  format = null,
  tab: controlledTab,
  onTabChange,
  initialSavedDeckName,
  onSavedDeckNameChange,
}: DeckPickerProps) {
  // Unified library: cloud decks (when signed in) + browser-only decks, each tagged with where it
  // lives. Selecting a cloud deck works the same as a local one because both carry their card list.
  const { decks, reload: reloadDecks, removeDeck } = useUnifiedDecks()
  const { save: saveDeckRouted } = useSaveDeck()

  const showSaved = tabs.includes('saved')
  const showExamples = tabs.includes('examples')
  const showPaste = tabs.includes('paste')
  const showRandom = tabs.includes('random')

  // Under a constructed format the server builds a 60-card format-legal deck rather than opening
  // boosters; commander shapes build a singleton deck and pick a commander. Which builder runs is
  // what this decides — *not* whether the set choice matters, which it does either way (see
  // `randomDescription`).
  const randomIsConstructed = format !== null && !COMMANDER_SHAPES.includes(format.toUpperCase())
  const formatLabel = format?.replace('_', ' ').toLowerCase() ?? ''

  // Default tab: saved if available, else paste, else the first allowed tab.
  const initialTab: Tab = decks.length > 0 && showSaved
    ? 'saved'
    : showRandom
      ? 'random'
      : showPaste
        ? 'paste'
        : (tabs[0] ?? 'paste')
  const [uncontrolledTab, setUncontrolledTab] = useState<Tab>(() => initialTab)
  const tab = controlledTab ?? uncontrolledTab
  /**
   * Whether landing on Random was a *placeholder* rather than a decision.
   *
   * The deck library hydrates asynchronously, so on the first render `decks` is always empty and
   * {@link initialTab} falls through to `random` — the picker's way of showing something rather than
   * an empty "My Decks". That placeholder is replaced by `saved` as soon as the list arrives.
   *
   * Random asked for *from outside* is the opposite: the landing wizard's "Random pool" and the
   * cross-kind recreate both hand the tab in through {@link DeckPickerProps.tab}, having already
   * promised the player a rolled pool. Before this the two were indistinguishable, so
   * "A friend → Random pool → Create lobby" opened a lobby sitting on My Decks — the picker
   * overwrote the answer the wizard had just collected.
   */
  const randomIsPlaceholder = useRef(controlledTab === undefined)
  const setTab = useCallback((next: Tab) => {
    randomIsPlaceholder.current = false
    setUncontrolledTab(next)
    onTabChange?.(next)
  }, [onTabChange])
  const [pasteText, setPasteText] = useState('')
  // Commander designation that rides along with the Paste tab. The paste textarea has no
  // commander UI of its own — loading a commander-shape example is the only way this gets
  // populated. Cleared whenever the user edits the paste text manually so a stale designation
  // can't outlive the original example contents.
  const [pasteCommander, setPasteCommander] = useState<string | null>(null)
  const [selectedSavedId, setSelectedSavedId] = useState<string | null>(null)
  const [pendingName, setPendingName] = useState('')
  const [cards, setCards] = useState<Record<string, CardSummary>>({})
  const [examples, setExamples] = useState<ExampleDeck[]>([])
  const [validation, setValidation] = useState<ValidationResult | null>(null)
  const [randomSetCodes, setRandomSetCodes] = useState<readonly string[]>(
    initialSetCodes ?? (initialSetCode ? [initialSetCode] : []),
  )
  const validateAbortRef = useRef<AbortController | null>(null)

  // The Random tab's set choice is both local (it drives the chips) and lifted (the lobby submits it),
  // so every change goes through one place rather than being duplicated per handler.
  const commitSetCodes = useCallback(
    (next: readonly string[]) => {
      setRandomSetCodes(next)
      onSetCodesChange?.(next)
    },
    [onSetCodesChange],
  )

  /**
   * What the server will actually do, given the lobby's format *and* the sets you picked.
   *
   * The set choice used to be hidden here whenever a constructed format was set, on the reasoning
   * that "the format defines the pool, not the boosters". That was only half true: the server passes
   * a human Random seat's `setCodes` straight into `ConstructedDeckGenerator.generate(setCodes,
   * format)` / `CommanderDeckGenerator.generate(setCodes, …)`, both of which narrow their pool to
   * those sets. So the capability was there and only the control was missing — which is why the AI
   * seat could be told "build a Pauper deck out of Innistrad" and you could not. Now both seats can,
   * and this sentence is what makes the difference visible rather than something you infer.
   */
  const pinnedSets = randomSetCodes.length > 0
  const setsPhrase = randomSetCodes
    .map((code) => availableSets.find((s) => s.code === code)?.name ?? code)
    .join(', ')
  const randomDescription = randomIsConstructed
    ? pinnedSets
      ? `A 60-card ${formatLabel}-legal deck, auto-built the moment the game starts from the
         ${formatLabel}-legal cards in ${setsPhrase}. Nothing to submit — just ready up.`
      : `A 60-card ${formatLabel}-legal deck, auto-built from the whole legal card pool the moment
         the game starts. Pick sets below to build it out of those instead.`
    : format !== null
      ? pinnedSets
        ? `A commander and a singleton deck in its colours, drawn from ${setsPhrase} the moment the
           game starts. Nothing to submit — just ready up.`
        : `A commander and a singleton deck in its colours, drawn from the whole legal card pool.
           Pick sets below to draw from those instead.`
      : pinnedSets
        ? `Eight boosters across ${setsPhrase}, auto-built into a 40-card deck the moment the game
           starts. Nothing else to submit — just ready up.`
        : `Eight boosters from one set chosen for you, auto-built into a 40-card deck the moment the
           game starts. Pick sets below to choose which.`

  // Re-hydrate on initial-set-code change (e.g. server-driven on reconnect).
  useEffect(() => {
    setRandomSetCodes(initialSetCodes ?? (initialSetCode ? [initialSetCode] : []))
  }, [initialSetCode, initialSetCodes])

  // Tell a controlling parent which tab we resolved to, so it starts in sync rather than having
  // to guess the default. Fires once; every later change goes through `setTab`.
  useEffect(() => {
    onTabChange?.(tab)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  // Replace the placeholder Random tab with `saved` once decks are hydrated, so users land on their
  // own list. Keyed on `decks.length`, so this only fires as the deck list arrives — a user who
  // picks Random later stays on it, and so does a Random the caller asked for
  // (see `randomIsPlaceholder`).
  useEffect(() => {
    if (randomIsPlaceholder.current && decks.length > 0 && tab === 'random' && showSaved) {
      setTab('saved')
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [decks.length])

  /**
   * Preselect the deck a saved setup asked for, once the library has arrived.
   *
   * Guarded on three things, each of which is a way this could otherwise do harm: only on the Saved
   * tab (so a promised Random pool is never overwritten), only with nothing already chosen (so a
   * player's own pick wins), and only while the name still matches something. A name that no longer
   * resolves simply leaves the picker where it was — the setup says so in its notes rather than
   * silently substituting a different deck.
   */
  const wantedSavedName = initialSavedDeckName?.trim().toLowerCase()
  useEffect(() => {
    if (!wantedSavedName || selectedSavedId !== null || tab !== 'saved') return
    const match = decks.find((d) => d.name.trim().toLowerCase() === wantedSavedName)
    if (match) setSelectedSavedId(match.id)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [wantedSavedName, decks, tab])

  // Report the *identity* of the chosen deck, which `onDeckChange`'s card list cannot carry.
  useEffect(() => {
    onSavedDeckNameChange?.(
      tab === 'saved' ? (decks.find((d) => d.id === selectedSavedId)?.name ?? null) : null,
    )
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [tab, selectedSavedId, decks])

  // If the active tab gets removed (e.g. the Random tab is hidden), fall back.
  useEffect(() => {
    if (!tabs.includes(tab)) {
      setTab(tabs[0] ?? 'paste')
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [tabs])

  // Fetch card metadata + examples once.
  useEffect(() => {
    let cancelled = false
    fetch('/api/cards')
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

  const parsedPaste = useMemo(() => parseDeckText(pasteText), [pasteText])

  // The deck list emitted to the parent based on the active tab.
  const currentDeck: Record<string, number> = useMemo(() => {
    switch (tab) {
      case 'random':
        return {}
      case 'paste':
        return parsedPaste.cards
      case 'saved': {
        const saved = decks.find((d) => d.id === selectedSavedId)
        if (!saved) return {}
        // Saved decks store the commander separately from `cards` (per the
        // `SavedDeck.commander` contract — and matching CR 903.6a). Merge it
        // back so consumers that just want "the full deck list" see all 100
        // cards. Quick Game / Premade-Decks tournament submission flows feed
        // the result straight into the lobby payload, so without this the
        // server would receive 99 cards and the commander would be missing.
        return mergeCommanderIntoCards(saved.cards, saved.commander ?? null)
      }
      case 'examples':
        // Examples become a deck via the picker's Paste preview as soon as the user clicks one.
        // Selecting an example loads its text into the paste tab; while still on the Examples tab
        // we treat it as "no deck chosen yet".
        return {}
    }
  }, [tab, parsedPaste, decks, selectedSavedId])

  // Auto-populate the Save-deck name from an Arena export's `Name <…>` line.
  // Only fills when the user hasn't typed something themselves, so we don't
  // trample manual edits when they re-paste or tweak the textarea.
  useEffect(() => {
    if (parsedPaste.deckName && pendingName.trim() === '') {
      setPendingName(parsedPaste.deckName)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [parsedPaste.deckName])

  // Commander designation. Saved decks store one explicitly; commander-shape examples carry
  // theirs through Paste via [pasteCommander]. Random has no commander hint.
  const currentCommander: string | null = useMemo(() => {
    if (tab === 'saved') {
      const saved = decks.find((d) => d.id === selectedSavedId)
      return saved?.commander ?? null
    }
    if (tab === 'paste') return pasteCommander
    return null
  }, [tab, decks, selectedSavedId, pasteCommander])

  // The constructed sideboard ("outside the game", CR 100.4a) the wish effects fetch from. Only
  // saved decks carry one (the deckbuilder persists it); paste/random/examples have none. The
  // server ignores it for Limited lobbies (those derive pool − maindeck) and uses it for
  // constructed/premade ones.
  const currentSideboard: Record<string, number> = useMemo(() => {
    if (tab === 'saved') {
      const saved = decks.find((d) => d.id === selectedSavedId)
      return saved?.sideboard ?? {}
    }
    return {}
  }, [tab, decks, selectedSavedId])

  // Strip the commander out of `currentDeck` before crossing the network boundary. `currentDeck`
  // keeps the commander baked in so the totalCards display reads "100 cards" for a Commander
  // deck, but the server's `Deck.cards` is documented as the library only (CR 903.6a) — the
  // validator / lobby registrar adds the commander on top of `cards`. Sending both would count
  // the commander twice. Mirrors the equivalent strip in DeckbuilderPage.
  const deckListForServer = useMemo(
    () => stripCommanderFromCards(currentDeck, currentCommander),
    [currentDeck, currentCommander],
  )

  // Push the current deck up. We deliberately suppress empty emissions from non-Random tabs
  // so that landing on the Saved tab with nothing selected doesn't auto-submit `{}` to the
  // server — that would mark the player as "deck selected" with an empty deck and surface as
  // "Random Pool" in the lobby even though the user hasn't actually picked anything yet.
  // On the Random tab `{}` *is* the chosen deck (server generates a random pool), so emit it.
  useEffect(() => {
    if (tab !== 'random' && Object.keys(currentDeck).length === 0) return
    onDeckChange(deckListForServer, currentCommander, currentSideboard)
  }, [tab, currentDeck, deckListForServer, currentCommander, currentSideboard, onDeckChange])

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
      body: JSON.stringify({
        deckList: deckListForServer,
        ...(format ? { format } : {}),
        ...(currentCommander ? { commander: currentCommander } : {}),
      }),
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
  }, [currentDeck, deckListForServer, currentCommander, onValidityChange, format])

  const stats = useMemo(() => computeDeckStats(currentDeck, cards), [currentDeck, cards])
  const totalCards = Object.values(currentDeck).reduce((a, b) => a + b, 0)

  const handleLoadExample = (ex: ExampleDeck) => {
    setPasteText(formatDeckText(ex.cards))
    setPasteCommander(ex.commander ?? null)
    setPendingName(ex.name)
    setTab('paste')
  }

  const handleSaveCurrent = async () => {
    if (!pendingName.trim() || Object.keys(currentDeck).length === 0) return
    // Routes to the account when signed in (then refresh so the new cloud deck appears), else local.
    const { id } = await saveDeckRouted({ name: pendingName.trim(), cards: currentDeck })
    reloadDecks()
    setSelectedSavedId(id)
    setPendingName('')
    setTab('saved')
  }

  // Server-authoritative legality. Single batched POST covers every saved deck; the result is
  // a deckId → format[] map keyed by format name (uppercase). Commander is merged in so
  // count-based format checks (e.g. exactly 100 for Commander) see the full deck — saved
  // decks keep the commander out of `cards` per `SavedDeck.commander`.
  const legalityInput = useMemo(() => {
    const out: Record<string, Record<string, number>> = {}
    for (const d of decks) {
      out[d.id] = mergeCommanderIntoCards(d.cards, d.commander ?? null)
    }
    return out
  }, [decks])
  const legalityMap = useDeckLegalFormats(legalityInput)

  // Examples are curated for a concrete format. Once the lobby chooses one, do not show untagged
  // or differently-shaped decks that the server will reject on submission.
  const visibleExamples = useMemo(() => {
    if (!format) return examples
    const target = format.toUpperCase()
    return examples.filter((ex) => ex.format?.toUpperCase() === target)
  }, [examples, format])

  // Saved decks filtered by the lobby's format (when set). While the legality response is in
  // flight we leave the unfiltered list visible so the picker doesn't briefly empty out.
  const visibleDecks = useMemo(() => {
    if (!format) return decks
    const target = format.toUpperCase()
    return decks.filter((d) => {
      const legal = legalityMap[d.id]
      if (!legal) return true
      return legal.includes(target)
    })
  }, [decks, format, legalityMap])

  return (
    <div className={styles.picker}>
      <div className={styles.tabs}>
        {showSaved && <TabButton label={`My Decks${decks.length ? ` (${decks.length})` : ''}`} active={tab === 'saved'} onClick={() => setTab('saved')} disabled={disabled} />}
        {showExamples && <TabButton label="Examples" active={tab === 'examples'} onClick={() => setTab('examples')} disabled={disabled} />}
        {showPaste && <TabButton label="Paste" active={tab === 'paste'} onClick={() => setTab('paste')} disabled={disabled} />}
        {showRandom && <TabButton label="Random" active={tab === 'random'} onClick={() => setTab('random')} disabled={disabled} />}
      </div>

      <div className={styles.panel}>
        {tab === 'saved' && (
          <SavedDecksPanel
            decks={visibleDecks}
            catalog={cards}
            legalityMap={legalityMap}
            format={format}
            hiddenCount={decks.length - visibleDecks.length}
            selectedId={selectedSavedId}
            disabled={disabled}
            onSelect={setSelectedSavedId}
            onDelete={(d) => {
              void removeDeck(d)
              if (selectedSavedId === d.id) setSelectedSavedId(null)
            }}
            onEdit={(d) => {
              // Show the full deck in the paste editor — including the commander,
              // which is stored separately on `SavedDeck` but should appear in the
              // text the user can edit.
              setPasteText(formatDeckText(mergeCommanderIntoCards(d.cards, d.commander ?? null)))
              setPendingName(d.name)
              setTab('paste')
            }}
          />
        )}

        {tab === 'examples' && (
          <ExampleDecksPanel
            examples={visibleExamples}
            catalog={cards}
            loading={examples.length === 0}
            disabled={disabled}
            onLoad={handleLoadExample}
          />
        )}

        {tab === 'paste' && (
          <>
            <textarea
              value={pasteText}
              onChange={(e) => {
                setPasteText(e.target.value)
                // A manual edit voids any commander designation an example may have carried —
                // the user might have removed the commander card from the list entirely.
                if (pasteCommander !== null) setPasteCommander(null)
              }}
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
            {/* The one tab whose answer is "nothing to do", which is exactly why it has to say so.
                It used to be a single grey line in a 220px box — indistinguishable from a panel
                that had failed to load, and the least convincing possible landing place for
                someone who has just answered "Random pool" in the wizard. */}
            <div className={styles.randomCard} data-testid="random-pool-panel">
              <span className={styles.randomDie} aria-hidden>🎲</span>
              <h3 className={styles.randomTitle}>The server builds your deck</h3>
              <p className={styles.randomBody}>{randomDescription}</p>
              <p className={styles.randomBody}>
                This covers your seat only. Your opponent can still bring a deck of their own.
              </p>
              {availableSets.length > 0 && (
                <div className={styles.randomSetRow}>
                  <label className={styles.helperText} style={{ flexShrink: 0 }}>Sets</label>
                  <SetSelector
                    sets={availableSets}
                    selectedCodes={randomSetCodes}
                    onToggleSet={(code) => {
                      commitSetCodes(
                        randomSetCodes.includes(code)
                          ? randomSetCodes.filter((selected) => selected !== code)
                          : [...randomSetCodes, code],
                      )
                    }}
                    onSelectRandom={() => {
                      const chosen = rollRandomSet(availableSets, randomSetCodes)
                      if (chosen) commitSetCodes([...randomSetCodes, chosen.code])
                    }}
                    disabled={disabled}
                    align="start"
                    emptyMeansRandom={!randomIsConstructed}
                    emptyLabel="Every set — the whole legal card pool"
                  />
                </div>
              )}
            </div>
          </>
        )}
      </div>

      {tab !== 'random' && (
        <div className={styles.summaryWrapper}>
          <DeckSummary validation={validation} totalCards={totalCards} stats={stats} />
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

/**
 * "My Decks" gallery. Same full-art {@link DeckTile} the deckbuilder's saved-deck browser
 * uses, so a deck looks identical wherever it's picked; clicking a tile selects it as the
 * deck to play, and hover exposes Edit (opens it in the Paste tab) / Delete.
 */
function SavedDecksPanel({
  decks, catalog, legalityMap, format, hiddenCount, selectedId, disabled, onSelect, onDelete, onEdit,
}: {
  decks: UnifiedDeck[]
  catalog: Record<string, CardSummary>
  legalityMap: Record<string, string[]>
  format: string | null
  hiddenCount: number
  selectedId: string | null
  disabled: boolean
  onSelect: (id: string) => void
  onDelete: (d: UnifiedDeck) => void
  onEdit: (d: UnifiedDeck) => void
}) {
  // Tile metadata per deck. The commander is folded back into the card map (saved decks keep
  // it out of `cards` per `SavedDeck.commander`) so the count and pips match what actually
  // gets played, and so a commander can win the hero-art tie-break.
  const tiles = useMemo(
    () =>
      decks.map((d) => {
        const fullCards = mergeCommanderIntoCards(d.cards, d.commander ?? null)
        return {
          deck: d,
          total: Object.values(fullCards).reduce((a, b) => a + b, 0),
          colors: deckColors(fullCards, catalog),
          hero: rarestCard(fullCards, catalog, d.commander ?? null),
        }
      }),
    [decks, catalog],
  )

  if (decks.length === 0) {
    if (format && hiddenCount > 0) {
      return (
        <p className={styles.helperText}>
          None of your saved decks are legal in {labelForFormat(format.toUpperCase())}.
          Use the Paste tab to build one.
        </p>
      )
    }
    return <p className={styles.helperText}>No saved decks yet. Use the Paste tab to enter a list, then Save it.</p>
  }
  return (
    <>
      {format && hiddenCount > 0 && (
        <p className={styles.helperText}>
          Showing {decks.length} deck{decks.length === 1 ? '' : 's'} legal in {labelForFormat(format.toUpperCase())} ·
          hiding {hiddenCount} that {hiddenCount === 1 ? 'is not' : 'are not'} legal.
        </p>
      )}
      <div className={styles.deckGridScroll}>
        <div className={styles.deckGrid}>
          {tiles.map(({ deck, total, colors, hero }) => {
            // One chip only: the format the deck was saved as, else the first format it's legal in.
            const shownFormat = deck.format ?? legalityMap[deck.id]?.[0] ?? null
            return (
              <DeckTile
                key={deck.id}
                name={deck.name}
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
                selected={deck.id === selectedId}
                badge={deck.id === selectedId ? 'Selected' : undefined}
                storage={deck.online ? 'cloud' : 'local'}
                title={`Play with ${deck.name}`}
                disabled={disabled}
                onClick={() => onSelect(deck.id)}
                actions={
                  <>
                    <DeckTileActionButton
                      onClick={() => onEdit(deck)}
                      title="Edit in the Paste tab"
                      ariaLabel={`Edit ${deck.name}`}
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
          })}
        </div>
      </div>
    </>
  )
}

/**
 * Server-supplied starter decks, shown as the same art tiles as "My Decks" so both halves of
 * the picker read as one gallery. Clicking a tile loads the list into the Paste tab (where the
 * user can tweak and save it) — matching the pre-gallery behaviour.
 */
function ExampleDecksPanel({
  examples, catalog, loading, disabled, onLoad,
}: {
  examples: ExampleDeck[]
  catalog: Record<string, CardSummary>
  loading: boolean
  disabled: boolean
  onLoad: (ex: ExampleDeck) => void
}) {
  const tiles = useMemo(
    () =>
      examples.map((ex) => ({
        example: ex,
        total: Object.values(ex.cards).reduce((a, b) => a + b, 0),
        colors: deckColors(ex.cards, catalog),
        hero: rarestCard(ex.cards, catalog, ex.commander ?? null),
      })),
    [examples, catalog],
  )
  if (tiles.length === 0) {
    return (
      <p className={styles.helperText}>
        {loading ? 'Loading examples…' : 'No examples for this format.'}
      </p>
    )
  }
  return (
    <div className={styles.deckGridScroll}>
      <div className={styles.deckGrid}>
        {tiles.map(({ example, total, colors, hero }) => (
          <DeckTile
            key={example.id}
            name={example.name}
            description={example.description}
            total={total}
            colors={colors}
            hero={hero}
            format={example.format ?? null}
            formatTitle={example.format ? `Built for ${labelForFormat(example.format)}` : undefined}
            title={`Load ${example.name} into the Paste tab`}
            disabled={disabled}
            onClick={() => onLoad(example)}
          />
        ))}
      </div>
    </div>
  )
}
