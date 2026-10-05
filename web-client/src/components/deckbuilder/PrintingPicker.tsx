/**
 * Printing picker for the deckbuilder.
 *
 * Opens as a centered modal from a row chip on each deck entry. Fetches
 * `/api/cards/{name}/printings` once per open and shows every known printing as an
 * art thumbnail. Selecting a thumbnail calls back with the picked [PrintingRef]; the
 * picker doesn't own the pinned-state — the deckbuilder does, since pins persist
 * alongside the deck list.
 *
 * Because some cards (basic lands especially) have hundreds of printings, the modal
 * gives the grid room to breathe: printings are grouped under per-set headers and can
 * be narrowed by a set filter and a free-text search (set / collector number / artist).
 * Art tiles load lazily so an "All sets" view of a basic land stays cheap.
 *
 * Selecting "Use default" clears any pin (the engine resolves the card's canonical
 * printing as before). The currently-pinned thumbnail is highlighted so the user can
 * confirm without re-reading the set code.
 */
import { useEffect, useMemo, useRef, useState } from 'react'
import type { PrintingRef } from '@/types'
import styles from './PrintingPicker.module.css'

export interface PrintingDTO {
  readonly setCode: string
  readonly setName: string | null
  readonly collectorNumber: string
  readonly imageUri: string | null
  readonly backFaceImageUri: string | null
  readonly rarity: string
  readonly artist: string | null
  readonly releaseDate: string | null
  readonly scryfallId: string | null
  readonly isPromo: boolean
  readonly isFullArt: boolean
  readonly frameEffects: readonly string[]
  readonly borderColor: string | null
  /** Name printed on this card when it differs from the oracle name (e.g. Through the Omenpaths). */
  readonly printedName?: string | null
}

/** All printings sharing one set, ready to render under a single header. */
interface SetGroup {
  readonly setCode: string
  readonly setName: string | null
  readonly printings: PrintingDTO[]
}

export function PrintingPicker({
  cardName,
  pinned,
  onPick,
  onClear,
  onClose,
}: {
  cardName: string
  pinned: PrintingRef | undefined
  /**
   * Selection callback. Receives the full [PrintingDTO] so the deckbuilder can populate
   * its art-by-name cache without a second round-trip — the picker already has every
   * printing's image URL on hand from the lookup it performed to render the grid.
   */
  onPick: (printing: PrintingDTO) => void
  onClear: () => void
  onClose: () => void
}) {
  const [printings, setPrintings] = useState<PrintingDTO[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [setFilter, setSetFilter] = useState<string>('') // '' = all sets
  const [search, setSearch] = useState('')
  const dialogRef = useRef<HTMLDivElement | null>(null)

  // Fetch on open. Re-runs only when the card name changes — the picker is
  // unmounted/remounted between rows so there's no stale-card race. Filters reset with it.
  useEffect(() => {
    let cancelled = false
    setPrintings(null)
    setError(null)
    setSetFilter('')
    setSearch('')
    fetch(`/api/cards/${encodeURIComponent(cardName)}/printings`)
      .then((r) => {
        if (r.status === 404) return [] as PrintingDTO[]
        if (!r.ok) throw new Error(`HTTP ${r.status}`)
        return r.json() as Promise<PrintingDTO[]>
      })
      .then((list) => {
        if (!cancelled) setPrintings(list)
      })
      .catch((e: unknown) => {
        if (!cancelled) setError(e instanceof Error ? e.message : 'Failed to load printings')
      })
    return () => {
      cancelled = true
    }
  }, [cardName])

  // Dismiss on Escape. (Outside-click is handled by the backdrop's onMouseDown.)
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [onClose])

  // Distinct sets present on this card. Printings already arrive sorted newest-first from
  // the server, so first-seen order is the right set order too. Drives the set-filter
  // dropdown — we only offer sets this card actually has, not the whole catalogue.
  const setOptions = useMemo(() => {
    const seen = new Map<string, string | null>()
    for (const p of printings ?? []) {
      if (!seen.has(p.setCode)) seen.set(p.setCode, p.setName)
    }
    return Array.from(seen, ([code, name]) => ({ code, name }))
  }, [printings])

  // Apply set filter + free-text search, then group what remains under set headers,
  // preserving the server's newest-first order within and across sets.
  const groups = useMemo<SetGroup[]>(() => {
    if (!printings) return []
    const needle = search.trim().toLowerCase()
    const byCode = new Map<string, SetGroup>()
    for (const p of printings) {
      if (setFilter && p.setCode !== setFilter) continue
      if (
        needle &&
        !p.setCode.toLowerCase().includes(needle) &&
        !(p.setName?.toLowerCase().includes(needle) ?? false) &&
        !p.collectorNumber.toLowerCase().includes(needle) &&
        !(p.artist?.toLowerCase().includes(needle) ?? false) &&
        !(p.printedName?.toLowerCase().includes(needle) ?? false)
      ) {
        continue
      }
      const group = byCode.get(p.setCode)
      if (group) group.printings.push(p)
      else byCode.set(p.setCode, { setCode: p.setCode, setName: p.setName, printings: [p] })
    }
    return Array.from(byCode.values())
  }, [printings, setFilter, search])

  const totalShown = useMemo(() => groups.reduce((n, g) => n + g.printings.length, 0), [groups])

  const isCurrent = (p: PrintingDTO): boolean =>
    pinned !== undefined && pinned.setCode === p.setCode && pinned.collectorNumber === p.collectorNumber

  const hasPrintings = printings !== null && printings.length > 0

  return (
    <div
      className={styles.overlay}
      onMouseDown={(e) => {
        if (!dialogRef.current?.contains(e.target as Node)) onClose()
      }}
    >
      <div
        ref={dialogRef}
        className={styles.dialog}
        role="dialog"
        aria-modal="true"
        aria-label={`Pick a printing for ${cardName}`}
      >
        <header className={styles.header}>
          <span className={styles.title}>{cardName}</span>
          {hasPrintings && (
            <div className={styles.controls}>
              <select
                className={styles.setSelect}
                value={setFilter}
                onChange={(e) => setSetFilter(e.target.value)}
                aria-label="Filter by set"
              >
                <option value="">All sets ({printings.length})</option>
                {setOptions.map((s) => (
                  <option key={s.code} value={s.code}>
                    {s.name ? `${s.name} (${s.code})` : s.code}
                  </option>
                ))}
              </select>
              <input
                type="search"
                className={styles.search}
                placeholder="Search set, number, artist…"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                aria-label="Search printings"
              />
            </div>
          )}
          <button
            type="button"
            className={styles.clearButton}
            onClick={onClear}
            disabled={pinned === undefined}
          >
            Use default
          </button>
          <button type="button" className={styles.closeButton} onClick={onClose} aria-label="Close">
            ✕
          </button>
        </header>
        <div className={styles.body}>
          {printings === null && error === null && <div className={styles.muted}>Loading printings…</div>}
          {error !== null && <div className={styles.error}>Couldn't load printings: {error}</div>}
          {printings !== null && printings.length === 0 && (
            <div className={styles.muted}>No printings registered for this card yet.</div>
          )}
          {hasPrintings && totalShown === 0 && (
            <div className={styles.muted}>No printings match this filter.</div>
          )}
          {hasPrintings &&
            groups.map((g) => (
              <section key={g.setCode} className={styles.group}>
                <h3 className={styles.groupHeader}>
                  <span className={styles.groupName}>{g.setName ?? g.setCode}</span>
                  <span className={styles.groupCode}>{g.setCode}</span>
                  <span className={styles.groupCount}>{g.printings.length}</span>
                </h3>
                <ul className={styles.grid}>
                  {g.printings.map((p) => (
                    <li
                      key={`${p.setCode}-${p.collectorNumber}`}
                      className={`${styles.tile} ${isCurrent(p) ? styles.tileActive : ''}`}
                    >
                      <button
                        type="button"
                        className={styles.tileButton}
                        onClick={() => onPick(p)}
                        title={`${p.printedName ? `${p.printedName} · ` : ''}${p.setName ?? p.setCode} #${p.collectorNumber}${p.artist ? ` — ${p.artist}` : ''}`}
                      >
                        {p.imageUri ? (
                          <img src={p.imageUri} alt="" className={styles.tileImage} loading="lazy" />
                        ) : (
                          <div className={styles.tileImagePlaceholder}>{p.setCode}</div>
                        )}
                        <span className={styles.tileMeta}>
                          <span className={styles.tileSetCode}>{p.setCode}</span>
                          <span className={styles.tileCollector}>#{p.collectorNumber}</span>
                        </span>
                      </button>
                    </li>
                  ))}
                </ul>
              </section>
            ))}
        </div>
      </div>
    </div>
  )
}
