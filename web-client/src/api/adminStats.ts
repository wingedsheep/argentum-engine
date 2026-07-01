/**
 * REST client for the admin dashboard's global stats (`/api/stats/admin/*`). Auth is the dashboard's
 * shared {@link AdminAuth} — the bootstrap password, or the signed-in admin account's token. These
 * endpoints are only mounted when the server has accounts enabled.
 */
import type { StatBucket } from './account'
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
export const fetchColorDistribution = (auth: AdminAuth) => getAdminStats<StatBucket[]>(auth, '/colors')
export const fetchGeo = (auth: AdminAuth) => getAdminStats<GeoBucket[]>(auth, '/geo')
export const fetchTopCards = (auth: AdminAuth, limit = 50) =>
  getAdminStats<CardStat[]>(auth, `/cards?limit=${limit}`)
export const fetchCardWinRates = (auth: AdminAuth, minDecks = 10, limit = 50) =>
  getAdminStats<CardWinRate[]>(auth, `/cards/win-rates?minDecks=${minDecks}&limit=${limit}`)
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
