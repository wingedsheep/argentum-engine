/**
 * REST client for the admin dashboard's global stats (`/api/stats/admin/*`). Auth is the dashboard's
 * shared {@link AdminAuth} — the bootstrap password, or the signed-in admin account's token. These
 * endpoints are only mounted when the server has accounts enabled.
 */
import type { GameDecks, StatBucket } from './account'
import { type AdminAuth, adminAuthHeaders } from './adminAuth'

export interface GlobalOverview {
  readonly totalGames: number
  readonly totalPlayers: number
  readonly totalAccounts: number
  readonly totalTournaments: number
  readonly gamesLast24h: number
  readonly gamesLast7d: number
}

export interface DailyCount {
  readonly day: string
  readonly count: number
}

/** One table's row count and on-disk footprint in the accounts database. */
export interface DatabaseTableStat {
  readonly tableName: string
  readonly rows: number
  /** Heap + TOAST, excluding indexes. */
  readonly tableBytes: number
  readonly indexBytes: number
  /** tableBytes + indexBytes. */
  readonly totalBytes: number
}

/** Storage overview of the accounts database. */
export interface DatabaseStats {
  readonly databaseName: string
  /** Whole-database size — includes catalogs, so it exceeds the sum of the tables. */
  readonly databaseSizeBytes: number
  readonly tables: DatabaseTableStat[]
}

export interface GeoBucket {
  readonly country: string | null
  readonly countryCode: string | null
  readonly region: string | null
  readonly city: string | null
  readonly games: number
}

export interface CardStat {
  readonly cardName: string
  readonly copies: number
  readonly decks: number
}

export interface CardWinRate {
  readonly cardName: string
  readonly decks: number
  readonly wins: number
  readonly winRate: number
}

/** Tournament lifecycle status, mirroring the server's `TournamentStatus`. */
export type TournamentStatus = 'IN_PROGRESS' | 'COMPLETED' | 'ABANDONED'

export interface TournamentSummary {
  /** Opens the full tournament detail (standings + games). */
  readonly id: number
  /** Completion time, or the start time while still in progress. */
  readonly endedAt: string
  readonly name: string | null
  readonly format: string | null
  readonly gameMode: string | null
  readonly playerCount: number
  readonly winnerName: string | null
  readonly status: TournamentStatus
}

/** Coarse connection origin for a seat, resolved from its IP server-side (raw IP never sent). */
export interface PlayerLocation {
  readonly country: string | null
  readonly countryCode: string | null
  readonly region: string | null
  readonly city: string | null
}

/** One seat in a recorded game, for the admin global game list. */
export interface AdminGamePlayer {
  readonly name: string
  readonly userId: string | null
  readonly isAi: boolean
  readonly won: boolean
  readonly location: PlayerLocation | null
}

/** A recorded game in the admin global game list, with every seat. */
export interface AdminRecentGame {
  readonly gameId: string
  readonly endedAt: string
  readonly gameMode: string | null
  readonly format: string | null
  readonly players: AdminGamePlayer[]
  readonly winnerName: string | null
  readonly hasReplay: boolean
  readonly tournamentName: string | null
}

/** A page of global games plus the total count (for the pager). */
export interface AdminRecentGamesPage {
  readonly entries: AdminRecentGame[]
  readonly total: number
}

async function getAdminStats<T>(auth: AdminAuth, path: string): Promise<T> {
  const res = await fetch(`/api/stats/admin${path}`, { headers: adminAuthHeaders(auth) })
  if (!res.ok) throw new Error(`Failed to load stats (${res.status})`)
  return (await res.json()) as T
}

export const fetchOverview = (auth: AdminAuth) => getAdminStats<GlobalOverview>(auth, '/overview')
export const fetchGamesPerDay = (auth: AdminAuth, days = 30) =>
  getAdminStats<DailyCount[]>(auth, `/games-per-day?days=${days}`)
export const fetchModeDistribution = (auth: AdminAuth) => getAdminStats<StatBucket[]>(auth, '/modes')
/** Sets played most across recorded tournaments (counted once per tournament that used each set). */
export const fetchTournamentSets = (auth: AdminAuth) =>
  getAdminStats<StatBucket[]>(auth, '/tournament-sets')
export const fetchColorDistribution = (auth: AdminAuth) => getAdminStats<StatBucket[]>(auth, '/colors')
export const fetchGeo = (auth: AdminAuth) => getAdminStats<GeoBucket[]>(auth, '/geo')
export const fetchTopCards = (auth: AdminAuth, limit = 50) =>
  getAdminStats<CardStat[]>(auth, `/cards?limit=${limit}`)
export const fetchCardWinRates = (auth: AdminAuth, minDecks = 10, limit = 50) =>
  getAdminStats<CardWinRate[]>(auth, `/cards/win-rates?minDecks=${minDecks}&limit=${limit}`)
/** Per-table row counts + sizes. Exact counts server-side, so fetch on demand rather than polling. */
export const fetchDatabaseStats = (auth: AdminAuth) => getAdminStats<DatabaseStats>(auth, '/database')
export const fetchTournaments = (auth: AdminAuth, limit = 50) =>
  getAdminStats<TournamentSummary[]>(auth, `/tournaments?limit=${limit}`)

/** A page of global games, newest first; `total` comes from the `X-Total-Count` header. */
export async function fetchRecentGames(
  auth: AdminAuth,
  limit: number,
  offset: number,
): Promise<AdminRecentGamesPage> {
  const res = await fetch(`/api/stats/admin/recent-games?limit=${limit}&offset=${offset}`, {
    headers: adminAuthHeaders(auth),
  })
  if (!res.ok) throw new Error(`Failed to load games (${res.status})`)
  const total = Number(res.headers.get('X-Total-Count') ?? '0')
  const entries = (await res.json()) as AdminRecentGame[]
  return { entries, total: Number.isFinite(total) ? total : entries.length }
}

/** Who won an AI-vs-human game. `DRAW` also covers an AI and a human winning together (a mixed 2HG team). */
export type AiMatchOutcome = 'AI' | 'HUMAN' | 'DRAW'

/** A tally from the AI's side. Rates are over decisive games (draws excluded); null when there were none. */
export interface AiRecord {
  readonly games: number
  readonly aiWins: number
  readonly humanWins: number
  readonly draws: number
  readonly aiWinRate: number | null
  /** 95% Wilson interval around `aiWinRate`. */
  readonly aiWinRateLow: number | null
  readonly aiWinRateHigh: number | null
}

export type AiBucket = 'DAY' | 'WEEK' | 'MONTH'

/** A change to the engine AI merged to main, and the first deployed build that carried it (null = none yet). */
export interface AiChange {
  readonly sha: string
  readonly committedAt: string
  readonly pr: number | null
  readonly subject: string
  readonly shippedIn: string | null
  readonly shippedAt: string | null
  /** The series bucket it was merged in. */
  readonly bucketStart: string
}

export interface AiVsHumanReport {
  /** Range length in days; null for all time. */
  readonly days: number | null
  readonly bucket: AiBucket
  readonly overall: AiRecord
  /** The equally long window before the range; null for all time. */
  readonly previous: AiRecord | null
  /** Oldest first, gap-filled. `start` is the bucket's first UTC day; `aiChanges` counts engine AI changes merged in it. */
  readonly series: { readonly start: string; readonly record: AiRecord; readonly aiChanges: number }[]
  readonly byMode: { readonly gameMode: string | null; readonly format: string | null; readonly record: AiRecord }[]
  /** The most-played modes (at most six), each with its own series on the same buckets as `series`. */
  readonly seriesByMode: {
    readonly gameMode: string | null
    readonly format: string | null
    readonly series: { readonly start: string; readonly record: AiRecord }[]
  }[]
  /** Newest deployed build first. `newAiChanges`: AI changes it added over the build before it (null = unknown). */
  readonly byBuild: {
    readonly version: string
    readonly firstSeen: string
    readonly lastSeen: string
    readonly record: AiRecord
    readonly newAiChanges: number | null
  }[]
  /** Engine AI changes merged within the time axis, newest first. */
  readonly aiChanges: {
    /** False when the server build had no git history, so changes can't be shown. */
    readonly available: boolean
    readonly changes: AiChange[]
  }
  /** Latest games, oldest first. */
  readonly recent: AiRecentGame[]
  /** Humans who played the AI most, from the human's side. */
  readonly topHumans: AiHumanRecord[]
  /** The filter the report was computed under, with the humans it left out. */
  readonly filter: {
    readonly gameMode: string | null
    readonly format: string | null
    readonly setCode: string | null
    readonly excludeTopHumans: number
    readonly excludedHumans: AiHumanRecord[]
    /** Games in the range the human exclusions dropped. */
    readonly excludedGames: number
  }
  /** The values each filter can take, counted over the whole range before any filter applies. */
  readonly options: {
    readonly modes: { readonly gameMode: string | null; readonly format: string | null; readonly games: number }[]
    readonly sets: { readonly code: string; readonly name: string | null; readonly games: number }[]
    readonly gamesWithoutSet: number
  }
}

/** One AI-vs-human game: who sat in it, with which colours, and how it went. */
export interface AiRecentGame {
  /** Opens every seat's deck through {@link fetchMatchDecks}. */
  readonly matchId: number
  readonly endedAt: string
  readonly gameMode: string | null
  readonly format: string | null
  readonly outcome: AiMatchOutcome
  readonly humans: AiRecentHuman[]
  /** Each AI seat's deck colours (WUBRG, "" colourless); null when none was recorded. */
  readonly aiColors: (string | null)[]
  /** The game id to open the replay with; null when no replay was stored. */
  readonly replayGameId: string | null
}

export interface AiRecentHuman {
  /** The {@link AiHumanRecord.key} this seat belongs to. */
  readonly key: string
  readonly name: string
  readonly isGuest: boolean
  readonly won: boolean
  readonly colors: string | null
}

/** One human's games against the AI in a slice, newest first. */
export interface AiHumanGames {
  /** Their record over the slice; null when they have no games in it. */
  readonly human: AiHumanRecord | null
  readonly games: AiRecentGame[]
}

/** A human's record against the AI, from the human's side. */
export interface AiHumanRecord {
  /** Pass back as `excludeHuman` to leave this player out. */
  readonly key: string
  readonly name: string
  readonly isGuest: boolean
  readonly games: number
  readonly wins: number
  readonly losses: number
  readonly draws: number
}

/** Narrows the AI-vs-humans report to a slice of the games. */
export interface AiVsHumanFilter {
  readonly gameMode: string | null
  readonly format: string | null
  readonly setCode: string | null
  /** `AiHumanRecord.key`s whose games are left out. */
  readonly excludeHumans: readonly string[]
  /** Leave out the N humans with the most games in the slice. */
  readonly excludeTopHumans: number
}

/** The AI's record against humans over the last `days` days (0 = all time), narrowed by `filter`. */
export function fetchAiVsHuman(auth: AdminAuth, days: number, bucket: 'day' | 'week' | 'month', filter?: AiVsHumanFilter) {
  const params = new URLSearchParams({ days: String(days), bucket })
  if (filter?.gameMode) params.set('gameMode', filter.gameMode)
  if (filter?.format) params.set('format', filter.format)
  if (filter?.setCode) params.set('set', filter.setCode)
  for (const key of filter?.excludeHumans ?? []) params.append('excludeHuman', key)
  if (filter?.excludeTopHumans) params.set('excludeTopHumans', String(filter.excludeTopHumans))
  return getAdminStats<AiVsHumanReport>(auth, `/ai-vs-human?${params.toString()}`)
}

/**
 * One human's games against the AI over the last `days` days (0 = all time), under the filter's
 * mode/format/set slice — the same games their row in `topHumans` counts.
 */
export function fetchAiHumanGames(auth: AdminAuth, key: string, days: number, filter?: AiVsHumanFilter) {
  const params = new URLSearchParams({ key, days: String(days) })
  if (filter?.gameMode) params.set('gameMode', filter.gameMode)
  if (filter?.format) params.set('format', filter.format)
  if (filter?.setCode) params.set('set', filter.setCode)
  return getAdminStats<AiHumanGames>(auth, `/ai-vs-human/player-games?${params.toString()}`)
}

/** Every seat's recorded deck for one match, AI seats first. */
export function fetchMatchDecks(auth: AdminAuth, matchId: number) {
  return getAdminStats<GameDecks>(auth, `/matches/${matchId}/decks`)
}
