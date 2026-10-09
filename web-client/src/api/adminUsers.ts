/**
 * REST client for the admin Players view (`/api/admin/users/*`): the roster of registered accounts,
 * one account's full stats, and promoting/demoting admins. Auth is the shared {@link AdminAuth}. Only
 * mounted when the server has accounts enabled.
 */
import type {
  CardStat,
  GameDeckCard,
  GameDecks,
  GameHistoryEntry,
  HeadToHead,
  StatBucket,
  UserTournamentEntry,
} from './account'
import { type AdminAuth, adminAuthHeaders } from './adminAuth'

/** A registered account with its lifetime record, for the roster. */
export interface AdminUserSummary {
  readonly id: string
  readonly email: string
  readonly displayName: string
  readonly isAdmin: boolean
  /** Chosen avatar id (see components/profile/avatars), or null for the initial. */
  readonly avatar?: string | null
  readonly createdAt: string
  readonly games: number
  readonly wins: number
  readonly lastPlayed: string | null
}

export interface AdminUserStats {
  readonly games: number
  readonly wins: number
  readonly losses: number
  readonly winRate: number
}

/** One account's full profile + stats, for the detail view. */
export interface AdminUserDetail {
  readonly id: string
  readonly email: string
  readonly displayName: string
  readonly isAdmin: boolean
  /** Chosen avatar id (see components/profile/avatars), or null for the initial. */
  readonly avatar?: string | null
  readonly createdAt: string
  readonly stats: AdminUserStats
  readonly colors: StatBucket[]
  readonly modes: StatBucket[]
  readonly opponents: HeadToHead[]
  readonly topCards: CardStat[]
  readonly tournaments: UserTournamentEntry[]
  readonly recentGames: GameHistoryEntry[]
}

export async function fetchUsers(auth: AdminAuth): Promise<AdminUserSummary[]> {
  const res = await fetch('/api/admin/users', { headers: adminAuthHeaders(auth) })
  if (!res.ok) throw new Error(`Failed to load players (${res.status})`)
  return (await res.json()) as AdminUserSummary[]
}

export async function fetchUserDetail(auth: AdminAuth, id: string): Promise<AdminUserDetail> {
  const res = await fetch(`/api/admin/users/${id}`, { headers: adminAuthHeaders(auth) })
  if (!res.ok) throw new Error(`Failed to load player (${res.status})`)
  return (await res.json()) as AdminUserDetail
}

/** Grant or revoke admin access for an account. Returns the new admin flag. */
export async function setUserAdmin(auth: AdminAuth, id: string, isAdmin: boolean): Promise<boolean> {
  const res = await fetch(`/api/admin/users/${id}/admin`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...adminAuthHeaders(auth) },
    body: JSON.stringify({ isAdmin }),
  })
  if (!res.ok) throw new Error(`Failed to update admin status (${res.status})`)
  return ((await res.json()) as { isAdmin: boolean }).isAdmin
}

/** One of an account's saved decks, enriched for the admin deck viewer. */
export interface AdminUserDeck {
  readonly id: number
  readonly name: string
  readonly format?: string
  readonly updatedAt: string
  readonly cards: GameDeckCard[]
}

/** A page of a player's games plus the total count (for the pager). */
export interface AdminGamesPage {
  readonly entries: GameHistoryEntry[]
  readonly total: number
}

/** A page of the account's games, newest first; `total` comes from the `X-Total-Count` header. */
export async function fetchUserGames(
  auth: AdminAuth,
  id: string,
  limit: number,
  offset: number,
): Promise<AdminGamesPage> {
  const res = await fetch(`/api/admin/users/${id}/games?limit=${limit}&offset=${offset}`, {
    headers: adminAuthHeaders(auth),
  })
  if (!res.ok) throw new Error(`Failed to load games (${res.status})`)
  const total = Number(res.headers.get('X-Total-Count') ?? '0')
  const entries = (await res.json()) as GameHistoryEntry[]
  return { entries, total: Number.isFinite(total) ? total : entries.length }
}

/** Both seats' decks for one of the account's games (from this account's perspective). */
export async function fetchUserGameDecks(auth: AdminAuth, id: string, gameId: string): Promise<GameDecks> {
  const res = await fetch(`/api/admin/users/${id}/games/${gameId}/decks`, { headers: adminAuthHeaders(auth) })
  if (!res.ok) throw new Error(`Failed to load decks (${res.status})`)
  return (await res.json()) as GameDecks
}

/** The account's saved decks, newest first, enriched for the deck viewer. */
export async function fetchUserSavedDecks(auth: AdminAuth, id: string): Promise<AdminUserDeck[]> {
  const res = await fetch(`/api/admin/users/${id}/decks`, { headers: adminAuthHeaders(auth) })
  if (!res.ok) throw new Error(`Failed to load saved decks (${res.status})`)
  return (await res.json()) as AdminUserDeck[]
}
