/**
 * The slice of recorded games a stats view shows — who the games were against and how recent — as the
 * server's `vs` / `days` query parameters (see `GameFilter` in the game server). Every view leaves
 * out results that don't count (a seat that never took an action); game lists show them, marked.
 */

/** Who a game was against. A pod with both people and the AI is only under `all`. */
export type OpponentFilter = 'all' | 'humans' | 'ai'

/** How far back to look; `0` = all time. */
export type PeriodFilter = 0 | 7 | 30 | 90 | 365

export interface StatsFilter {
  readonly vs: OpponentFilter
  readonly days: PeriodFilter
}

export const ALL_GAMES: StatsFilter = { vs: 'all', days: 0 }

export const OPPONENT_OPTIONS: readonly { value: OpponentFilter; label: string }[] = [
  { value: 'all', label: 'Everyone' },
  { value: 'humans', label: 'Humans' },
  { value: 'ai', label: 'AI' },
]

export const PERIOD_OPTIONS: readonly { value: PeriodFilter; label: string }[] = [
  { value: 0, label: 'All time' },
  { value: 365, label: 'Year' },
  { value: 90, label: '90 days' },
  { value: 30, label: '30 days' },
  { value: 7, label: '7 days' },
]

export function isAllGames(f: StatsFilter): boolean {
  return f.vs === 'all' && f.days === 0
}

/**
 * The filter as query parameters, `&`-prefixed so it appends to a path that already has `?…`, or ''
 * for no filter. [periodParam] names the period parameter (the admin dashboard's games-per-day
 * endpoint takes the period as `days`, a name it had first, so its siblings take `period`).
 */
export function filterQuery(f: StatsFilter, periodParam = 'days'): string {
  let q = ''
  if (f.vs !== 'all') q += `&vs=${f.vs}`
  if (f.days !== 0) q += `&${periodParam}=${f.days}`
  return q
}

/** Read a filter from URL search params (`vs`, `days`), ignoring values the UI doesn't offer. */
export function parseFilter(params: URLSearchParams): StatsFilter {
  const vs = params.get('vs')
  const days = Number(params.get('days') ?? 0)
  return {
    vs: vs === 'humans' || vs === 'ai' ? vs : 'all',
    days: (PERIOD_OPTIONS.some((o) => o.value === days) ? days : 0) as PeriodFilter,
  }
}

/** A short human phrase for the filter, e.g. "against humans · last 30 days". */
export function describeFilter(f: StatsFilter): string {
  const parts: string[] = []
  if (f.vs === 'humans') parts.push('against humans')
  if (f.vs === 'ai') parts.push('against the AI')
  if (f.days !== 0) parts.push(`last ${PERIOD_OPTIONS.find((o) => o.value === f.days)?.label.toLowerCase() ?? `${f.days} days`}`)
  return parts.join(' · ')
}
