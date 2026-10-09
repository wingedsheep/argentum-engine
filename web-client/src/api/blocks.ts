/**
 * REST client for your blocked players (`/api/blocks`). Blocking happens on the result screen over
 * the socket, where the server already knows who your opponent was; this only lists and unblocks.
 * Only mounted when accounts are enabled.
 */
import { UnauthorizedError, getAuthToken } from './account'

function authHeaders(): Record<string, string> {
  const token = getAuthToken()
  return token ? { Authorization: `Bearer ${token}` } : {}
}

export interface BlockedPlayer {
  readonly accountId: string
  readonly displayName: string
  /** Preset avatar id, or null for the initial. */
  readonly avatar?: string | null
  readonly blockedAt: string
}

export async function fetchBlocked(): Promise<BlockedPlayer[]> {
  const res = await fetch('/api/blocks', { headers: authHeaders() })
  if (res.status === 401) throw new UnauthorizedError()
  if (!res.ok) throw new Error(`Failed to load blocked players (${res.status})`)
  return (await res.json()) as BlockedPlayer[]
}

export async function unblock(accountId: string): Promise<void> {
  const res = await fetch(`/api/blocks/${accountId}`, { method: 'DELETE', headers: authHeaders() })
  if (res.status === 401) throw new UnauthorizedError()
  if (!res.ok && res.status !== 404) throw new Error(`Couldn't unblock (${res.status})`)
}
