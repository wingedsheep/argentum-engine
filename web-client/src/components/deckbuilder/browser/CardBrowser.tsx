/**
 * Composed card browser: search bar + collapsible filter menu + image grid, over the full card
 * catalogue. This is the deckbuilder's browsing experience packaged for reuse — the deckbuilder
 * page itself composes the same pieces into its own three-column layout, while callers that
 * just want "let me find a card" (the scenario builder) mount this.
 *
 * State owned here: query, sort, filter-drawer open/closed, page size, and the set-filter art
 * override. Card selection is the caller's business: `onAdd` / `onRemove` fire on click and
 * shift/right-click, and every tile is draggable (payload source `catalog`).
 */
import { useEffect, useMemo, useState } from 'react'
import { isAdvancedQuery, parseQuery, type CardSummary } from '../cardFilter'
import { extractSetFilter } from '../query'
import { CardGrid } from './CardGrid'
import { FilterSection } from './FilterSection'
import { SearchBar } from './SearchBar'
import { sortCards, type SortMode } from './cardSort'
import type { SetInfo } from './useCardCatalog'
import { useCardsWithSetArt, useSetPrintingOverride, type PrintingOverride } from './useSetPrintingOverride'
import styles from './CardBrowser.module.css'

const PAGE_SIZE = 120

export function CardBrowser({
  catalog,
  setInfos,
  loading = false,
  error = null,
  counts,
  onAdd,
  onRemove,
  header,
  actionHint,
  initialQuery = '',
  filtersOpenByDefault = false,
  compact = true,
}: {
  catalog: CardSummary[]
  setInfos: SetInfo[]
  loading?: boolean
  error?: string | null
  /** Copies already placed, keyed by card name — drawn as a badge on each tile. */
  counts: Record<string, number>
  /**
   * Fires on click (and on drop, via the caller's own drop zones). `printing` is the reprint
   * matching an active `s:` filter, so a caller that tracks printings can pin it; null when no
   * set filter is on.
   */
  onAdd: (card: CardSummary, printing: PrintingOverride | null) => void
  onRemove: (name: string) => void
  /** Rendered above the search bar — used for "add to seat/zone" style context controls. */
  header?: React.ReactNode
  actionHint?: React.ReactNode
  initialQuery?: string
  filtersOpenByDefault?: boolean
  /** Denser card tiles — on by default, since this component is built for a side pane. */
  compact?: boolean
}) {
  const [query, setQuery] = useState(initialQuery)
  const [sortMode, setSortMode] = useState<SortMode>('name')
  const [filtersOpen, setFiltersOpen] = useState(filtersOpenByDefault)
  const [visibleCount, setVisibleCount] = useState(PAGE_SIZE)

  const parseResult = useMemo(() => parseQuery(query, { withErrors: true }), [query])
  const advanced = useMemo(() => isAdvancedQuery(query), [query])
  const activeSetFilter = useMemo(() => extractSetFilter(parseResult.ast), [parseResult.ast])

  const predicate = parseResult.predicate
  const filtered = useMemo(
    () => sortCards(catalog.filter(predicate), sortMode),
    [catalog, predicate, sortMode],
  )

  useEffect(() => {
    setVisibleCount(PAGE_SIZE)
  }, [query, sortMode])

  // Only the visible page needs reprint art — asking for every match of a broad `s:` filter
  // would be a needlessly large request.
  const visibleNames = useMemo(
    () => filtered.slice(0, visibleCount).map((c) => c.name),
    [filtered, visibleCount],
  )
  const overrides = useSetPrintingOverride(activeSetFilter, visibleNames)
  const withArt = useCardsWithSetArt(filtered, activeSetFilter, overrides)
  const displayed = useMemo(() => withArt.slice(0, visibleCount), [withArt, visibleCount])

  const resultLabel = loading
    ? 'Loading…'
    : `Showing ${displayed.length} of ${filtered.length}`

  return (
    <div className={styles.browser}>
      {header && <div className={styles.header}>{header}</div>}
      <div className={styles.searchRow}>
        <SearchBar
          query={query}
          onQueryChange={setQuery}
          sortMode={sortMode}
          onSortChange={setSortMode}
          errors={parseResult.errors}
          resultLabel={resultLabel}
          {...(actionHint !== undefined ? { actionHint } : {})}
        />
        <button
          type="button"
          className={filtersOpen ? styles.filtersToggleActive : styles.filtersToggle}
          onClick={() => setFiltersOpen((v) => !v)}
          aria-pressed={filtersOpen}
        >
          Filters
        </button>
      </div>
      {error ? (
        <div className={styles.error}>Couldn’t load the card catalogue: {error}</div>
      ) : (
        <div className={styles.body}>
          {filtersOpen && (
            <div className={styles.filters}>
              <FilterSection
                query={query}
                onQueryChange={setQuery}
                catalog={catalog}
                setInfos={setInfos}
                advanced={advanced}
              />
            </div>
          )}
          <div className={styles.results}>
            {loading ? (
              <div className={styles.notice}>Loading cards…</div>
            ) : (
              <CardGrid
                cards={displayed}
                deckCards={counts}
                onAdd={(card) => onAdd(card, overrides[card.name] ?? null)}
                onRemove={onRemove}
                hasMore={displayed.length < filtered.length}
                onShowMore={() => setVisibleCount((c) => c + PAGE_SIZE)}
                compact={compact}
              />
            )}
          </div>
        </div>
      )}
    </div>
  )
}
