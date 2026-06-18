/**
 * SetPickerModal — the searchable, grouped set browser shared by the tournament lobby and the
 * Quick Game lobby's "Random" deck tab.
 *
 * Every set (complete + partial) lives behind this one modal. It supports two modes:
 *   - `multi`  (default): clicking a row toggles it and the modal stays open. The host builds a
 *               multi-set pool (tournament lobby).
 *   - `single`: clicking a row selects it and closes the modal. With `onSelectRandom` a top
 *               "Random Set" row clears the selection so the server rolls a random set
 *               (Quick Game random sealed pool).
 *
 * Visuals come from `GameUI.module.css` (the shared lobby stylesheet — `QuickGameLobbyOverlay`
 * already reuses it) so both lobbies look identical.
 */
import { useState } from 'react'
import type { AvailableSet } from '@/types/messages'
import { SetIcon } from './SetIcon'
import styles from './GameUI.module.css'

export interface SetPickerModalProps {
  /** All sets the picker can browse (complete + partial). */
  sets: readonly AvailableSet[]
  /** Codes currently selected — drives row highlight and (multi mode) the "N selected" count. */
  selectedCodes: readonly string[]
  /** Toggle (multi) or select (single) a set by code. */
  onToggleSet: (code: string) => void
  /** Dismiss the modal. */
  onClose: () => void
  /**
   * 'multi' (default) keeps the modal open after each toggle; 'single' selects one set and closes.
   */
  mode?: 'single' | 'multi'
  /**
   * Single mode only — when provided, a "Random Set" row is shown at the top that clears the
   * selection (the server then rolls a random set). Selecting it closes the modal.
   */
  onSelectRandom?: () => void
  /** Modal heading. Defaults to "Choose sets". */
  title?: string
}

const MONTH_ABBREVS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']

// Format an ISO `YYYY-MM-DD` release date as e.g. "Oct 2002". Parsed manually to avoid timezone shifts.
function formatReleaseDate(releaseDate?: string): string | null {
  if (!releaseDate) return null
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(releaseDate)
  if (!match) return null
  const [, year, month] = match
  if (!year) return null
  const monthName = MONTH_ABBREVS[Number(month) - 1]
  return monthName ? `${monthName} ${year}` : year
}

// Newest-first comparator on ISO `YYYY-MM-DD` keys; sets/blocks with no known release date sort last.
const UNKNOWN_RELEASE = ''
const releaseSortKey = (releaseDate?: string): string => releaseDate ?? UNKNOWN_RELEASE
function byNewestFirst(a: string, b: string): number {
  if (a === UNKNOWN_RELEASE || b === UNKNOWN_RELEASE) {
    if (a === b) return 0
    return a === UNKNOWN_RELEASE ? 1 : -1 // unknowns always last
  }
  return b.localeCompare(a) // later date first
}

export function SetPickerModal({
  sets,
  selectedCodes,
  onToggleSet,
  onClose,
  mode = 'multi',
  onSelectRandom,
  title = 'Choose sets',
}: SetPickerModalProps) {
  const [search, setSearch] = useState('')
  // Partially-implemented sets are hidden by default; the user opts into them with this toggle.
  const [showPartialSets, setShowPartialSets] = useState(false)
  // Hide thin sets (lots of partial sets have only a handful of cards). Defaults to 30.
  const [minCards, setMinCards] = useState(30)

  const isSingle = mode === 'single'
  const isSelected = (code: string) => selectedCodes.includes(code)

  // Picker visibility rules (an already-selected set always stays visible so it can be changed):
  //  - partial sets are hidden unless the toggle is flipped, and
  //  - sets with fewer than `minCards` implemented cards are pruned (kills the long tail of
  //    near-empty sets you'd otherwise see once partial sets are shown).
  const partialSetCount = sets.filter((s) => s.partial).length
  const pickerSets = sets.filter((s) => {
    if (isSelected(s.code)) return true
    if (s.partial && !showPartialSets) return false
    if ((s.implementedCount ?? 0) < minCards) return false
    return true
  })

  // Searchable inside the modal — match on set name or code, like the deckbuilder picker.
  const searchNeedle = search.trim().toLowerCase()
  const filteredPickerSets = searchNeedle
    ? pickerSets.filter(
        (s) =>
          s.name.toLowerCase().includes(searchNeedle) ||
          s.code.toLowerCase().includes(searchNeedle),
      )
    : pickerSets

  const handlePick = (code: string) => {
    onToggleSet(code)
    if (isSingle) onClose()
  }

  const handleRandom = () => {
    onSelectRandom?.()
    onClose()
  }

  // One toggle row inside the picker modal. Incomplete sets get a "partial" tag so the user knows
  // those boosters draw from a reduced pool of implemented cards.
  const renderSetPickerRow = (set: AvailableSet) => {
    const selected = isSelected(set.code)
    const released = formatReleaseDate(set.releaseDate)
    return (
      <button
        key={set.code}
        type="button"
        onClick={() => handlePick(set.code)}
        className={`${styles.setPickerRow} ${selected ? styles.setPickerRowActive : ''}`}
      >
        <span className={styles.setPickerCheck} aria-hidden>{selected ? '✓' : ''}</span>
        <SetIcon code={set.code} className={styles.setPickerIcon} />
        <span className={styles.setPickerName}>{set.name}</span>
        {set.partial && <span className={styles.setPartialBadge}>partial</span>}
        {released && <span className={styles.setReleaseDate}>{released}</span>}
        {set.implementedCount != null && (
          <span className={styles.setButtonCardCount}>{set.implementedCount} cards</span>
        )}
      </button>
    )
  }

  // Order sets newest-first while keeping blocks grouped: each block is a single unit positioned by
  // its newest set's release date, and each blockless set is its own unit. Units are sorted by date
  // (newest on top), and sets within a block are sorted newest-first too.
  const renderGroupedSetRows = (rows: readonly AvailableSet[]) => {
    const blockOrder: string[] = []
    const blockSets = new Map<string, AvailableSet[]>()
    const ungrouped: AvailableSet[] = []
    for (const set of rows) {
      if (set.block) {
        if (!blockSets.has(set.block)) {
          blockOrder.push(set.block)
          blockSets.set(set.block, [])
        }
        blockSets.get(set.block)!.push(set)
      } else {
        ungrouped.push(set)
      }
    }

    type Unit =
      | { kind: 'block'; key: string; label: string; sets: AvailableSet[]; sortKey: string }
      | { kind: 'set'; key: string; set: AvailableSet; sortKey: string }

    const units: Unit[] = []
    for (const name of blockOrder) {
      const blockMembers = blockSets
        .get(name)!
        .slice()
        .sort((a, b) => byNewestFirst(releaseSortKey(a.releaseDate), releaseSortKey(b.releaseDate)))
      units.push({
        kind: 'block',
        key: name,
        label: `${name} Block`,
        sets: blockMembers,
        sortKey: releaseSortKey(blockMembers[0]?.releaseDate), // newest in block, after sort above
      })
    }
    for (const set of ungrouped) {
      units.push({ kind: 'set', key: set.code, set, sortKey: releaseSortKey(set.releaseDate) })
    }
    units.sort((a, b) => byNewestFirst(a.sortKey, b.sortKey))

    return units.map((unit) =>
      unit.kind === 'block' ? (
        <div key={`block:${unit.key}`} className={styles.setPickerGroup}>
          <div className={styles.setPickerGroupLabel}>{unit.label}</div>
          {unit.sets.map(renderSetPickerRow)}
        </div>
      ) : (
        renderSetPickerRow(unit.set)
      ),
    )
  }

  const showRandomRow = isSingle && onSelectRandom != null
  const randomActive = selectedCodes.length === 0

  return (
    <div className={styles.deckViewerBackdrop} onClick={onClose}>
      <div className={styles.deckViewerPanel} style={{ maxWidth: 560 }} onClick={(e) => e.stopPropagation()}>
        <div className={styles.deckViewerHeader}>
          <h3 className={styles.deckViewerTitle}>{title}</h3>
          {!isSingle && (
            <span className={styles.setPickerSelectedCount}>
              {selectedCodes.length} selected
            </span>
          )}
          <button className={styles.deckViewerClose} onClick={onClose}>×</button>
        </div>
        <div className={styles.deckViewerBody}>
          <input
            type="text"
            className={styles.setPickerSearch}
            placeholder="Search sets by name or code…"
            value={search}
            spellCheck={false}
            autoComplete="off"
            autoFocus
            onChange={(e) => setSearch(e.target.value)}
          />
          <div className={styles.setPickerFilters}>
            <button
              type="button"
              role="switch"
              aria-checked={showPartialSets}
              className={`${styles.setPickerSwitch} ${showPartialSets ? styles.setPickerSwitchOn : ''}`}
              onClick={() => setShowPartialSets((v) => !v)}
            >
              <span className={styles.setPickerSwitchTrack} aria-hidden>
                <span className={styles.setPickerSwitchThumb} />
              </span>
              <span className={styles.setPickerSwitchLabel}>
                Show partial sets
                {partialSetCount > 0 && (
                  <span className={styles.setPickerSwitchCount}>{partialSetCount}</span>
                )}
              </span>
            </button>
            <label className={styles.setPickerMinCards}>
              <span className={styles.setPickerMinCardsLabel}>Min cards</span>
              <input
                type="number"
                min={0}
                step={10}
                inputMode="numeric"
                className={styles.setPickerMinCardsInput}
                value={minCards}
                onChange={(e) => setMinCards(Math.max(0, Math.floor(Number(e.target.value) || 0)))}
              />
            </label>
          </div>
          <p className={styles.setPickerNote}>
            Sets tagged <span className={styles.setPartialBadge}>partial</span> aren't fully
            implemented — their boosters draw from a reduced pool of the cards that exist.
          </p>
          <div className={styles.setPickerList}>
            {showRandomRow && (
              <button
                type="button"
                onClick={handleRandom}
                className={`${styles.setPickerRow} ${randomActive ? styles.setPickerRowActive : ''}`}
              >
                <span className={styles.setPickerCheck} aria-hidden>{randomActive ? '✓' : ''}</span>
                <span className={styles.setPickerIcon} aria-hidden>🎲</span>
                <span className={styles.setPickerName}>Random Set</span>
              </button>
            )}
            {filteredPickerSets.length > 0 ? (
              renderGroupedSetRows(filteredPickerSets)
            ) : (
              !showRandomRow && (
                <div className={styles.setPickerEmpty}>
                  No sets match.{' '}
                  {!showPartialSets && partialSetCount > 0
                    ? 'Try enabling partial sets'
                    : minCards > 0
                      ? 'Try lowering the minimum card count'
                      : 'Try a different search'}
                  .
                </div>
              )
            )}
          </div>
        </div>
      </div>
    </div>
  )
}
