/**
 * REST client for the admin Players view (`/api/admin/users/*`): the roster of registered accounts,
 * one account's full stats, and promoting/demoting admins. Auth is the shared {@link AdminAuth}. Only
 * mounted when the server has accounts enabled.
 */
import type { CardStat, GameHistoryEntry, HeadToHead, StatBucket, UserTournamentEntry } from './account'
import { type AdminAuth, adminAuthHeaders } from './adminAuth'

/** A registered account with its lifetime record, for the roster. */
export interface AdminUserSummary {
  readonly id: string
  readonly email: string
  readonly displayName: string
  readonly isAdmin: boolean
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
