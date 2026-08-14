/**
 * Typed wrappers for the set-coverage endpoints that power the Set Completion view.
 *
 * The server joins a committed canonical-totals resource (how many cards a set
 * canonically has, split into booster + extras) with the live card catalog (how many
 * we've implemented), so a set's `implemented` count is always `<= total`. The headline
 * % is over the booster (draft) cards only; the extras come back split into Scryfall's own
 * set-page sections (see `ExtraGroup`). See `game-server` SetCoverageService /
 * `GET /api/sets/coverage` and `GET /api/sets/{code}/coverage`.
 *
 * Cards we've decided never to implement (ante, subgames, physical dexterity) come back
 * flagged `notPlanned` and are already excluded from every total, so `implemented / total`
 * measures what we intend to build.
 */

/** Why a card will never be implemented. Present only on not-planned cards, and never without a reason. */
export interface NotPlanned {
  /** Short reason key — `ante`, `subgame`, `dexterity`. */
  readonly kind: string
  /** Player-facing sentence explaining the decision. */
  readonly why: string
}

export interface SetCoverage {
  readonly code: string
  readonly name: string
  /** ISO `YYYY-MM-DD`, or null if Scryfall didn't report one. */
  readonly releaseDate: string | null
  /** Scryfall set type (`expansion`, `core`, `commander`, …); null if unknown. */
  readonly setType: string | null
  /** Block name (e.g. "Onslaught") if the set belongs to one. */
  readonly block: string | null
  /** Booster (draft) cards we've authored. Always `<= total`; drives the headline %. */
  readonly implemented: number
  /** Booster (draft) cards we intend to build — canonical count minus `notPlanned`. */
  readonly total: number
  /** Completionist extras we've authored. */
  readonly extraImplemented: number
  /** Completionist extras we intend to build — canonical count minus `extraNotPlanned`. */
  readonly extraTotal: number
  /** Booster cards we've decided never to implement; already excluded from `total`. */
  readonly notPlanned: number
  /** Completionist extras we've decided never to implement; already excluded from `extraTotal`. */
  readonly extraNotPlanned: number
  /** `implemented / total * 100` (booster cards), one decimal; `0` when `total` is `0`. */
  readonly percent: number
  /** Whether the set is currently legal in the Standard format (derived from per-card legality). */
  readonly inStandard: boolean
}

/**
 * Project-wide rollup for the Set Completion banner. `distinct*` dedupes reprints by card name
 * (a staple in 15 sets counts once); `printings*` is the naive sum of the per-set rows (each
 * reprint counted per set). The two differ purely by reprint multiplicity.
 */
export interface CoverageSummary {
  readonly distinctImplemented: number
  readonly distinctTotal: number
  readonly distinctPercent: number
  /** Completionist extras, deduped by name and partitioned away from the booster universe above. */
  readonly extraDistinctImplemented: number
  readonly extraDistinctTotal: number
  readonly extraDistinctPercent: number
  readonly printingsImplemented: number
  readonly printingsTotal: number
  readonly printingsPercent: number
  /** Distinct card names we've decided never to implement — excluded from every total above. */
  readonly distinctNotPlanned: number
  readonly setsComplete: number
  readonly setCount: number
}

export interface CardCoverage {
  readonly name: string
  readonly implemented: boolean
  /** Set-specific Scryfall art (direct CDN URL, normal size); null if Scryfall had none. */
  readonly imageUri: string | null
  /** Non-null when we've decided never to implement this card, carrying the reason. */
  readonly notPlanned: NotPlanned | null
}

/**
 * One section of a set's completionist extras, mirroring how scryfall.com/sets/&lt;code&gt; breaks a
 * set page up — "Starter Decks", "Promos", "Beginner Box", … Only the extras are sectioned:
 * Scryfall's other headings (Borderless, Showcase, Extended Art, Raised Foil) are alternate
 * *printings* of cards already in the draft pool, so against a card-name denominator they hold
 * nothing new.
 */
export interface ExtraGroup {
  /** Section heading, e.g. `Starter Decks`. */
  readonly label: string
  readonly implemented: number
  /** Cards in this section we intend to build — count minus `notPlanned`. */
  readonly total: number
  /** Cards here flagged never-to-implement; excluded from `total` but still listed in `cards`. */
  readonly notPlanned: number
  readonly cards: readonly CardCoverage[]
}

export interface SetDetail {
  readonly code: string
  readonly name: string
  readonly releaseDate: string | null
  readonly block: string | null
  readonly implemented: number
  readonly total: number
  readonly extraImplemented: number
  readonly extraTotal: number
  /** Booster cards flagged never-to-implement; excluded from `total` but still listed in `draft`. */
  readonly notPlanned: number
  /** Extras flagged never-to-implement; excluded from `extraTotal` but still listed in `extraGroups`. */
  readonly extraNotPlanned: number
  readonly percent: number
  /** Booster (draft) cards, A→Z — including the not-planned ones, each carrying its reason. */
  readonly draft: readonly CardCoverage[]
  /** Completionist extras in Scryfall's section order. Empty if the set has none. */
  readonly extraGroups: readonly ExtraGroup[]
}

/** Per-set card-implementation coverage, newest release first. */
export async function fetchSetCoverage(): Promise<readonly SetCoverage[]> {
  const res = await fetch('/api/sets/coverage')
  if (!res.ok) throw new Error(`Failed to load set coverage (${res.status})`)
  return res.json() as Promise<readonly SetCoverage[]>
}

/** Project-wide distinct + printing coverage rollup for the Set Completion banner. */
export async function fetchCoverageSummary(): Promise<CoverageSummary> {
  const res = await fetch('/api/sets/coverage/summary')
  if (!res.ok) throw new Error(`Failed to load coverage summary (${res.status})`)
  return res.json() as Promise<CoverageSummary>
}

/** One set's full canonical card list, each card marked implemented / missing. */
export async function fetchSetDetail(code: string): Promise<SetDetail> {
  const res = await fetch(`/api/sets/${encodeURIComponent(code)}/coverage`)
  if (!res.ok) throw new Error(`Failed to load ${code} coverage (${res.status})`)
  return res.json() as Promise<SetDetail>
}

/** One day on the implementation-progress curve. */
export interface ProgressPoint {
  readonly date: string
  readonly added: number
  readonly total: number
}

/** Distinct-implemented-cards-over-time series, one cumulative point per day since the start. */
export async function fetchProgressHistory(): Promise<readonly ProgressPoint[]> {
  const res = await fetch('/api/sets/progress')
  if (!res.ok) throw new Error(`Failed to load progress history (${res.status})`)
  return res.json() as Promise<readonly ProgressPoint[]>
}
