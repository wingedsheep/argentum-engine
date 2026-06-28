/**
 * REST client for the friends subsystem (`/api/friends/*`): your friends + their online status, the
 * incoming/outgoing request lists, and the request → accept lifecycle. Adding a friend takes their
 * account id (their shareable "friend code"), never an email. Only mounted when accounts are enabled;
 * the friends UI is hidden otherwise.
 *
 * Auth is the same Bearer token as the rest of the account API (see api/account).
 */
import { UnauthorizedError, getAuthToken } from './account'

function authHeaders(): Record<string, string> {
  const token = getAuthToken()
  return token ? { Authorization: `Bearer ${token}` } : {}
}

async function errorMessage(res: Response, fallback: string): Promise<string> {
  const body = (await res.json().catch(() => null)) as { error?: string } | null
  return body?.error ?? fallback
}

export interface Friend {
  readonly accountId: string
  readonly displayName: string
  readonly online: boolean
}

export interface FriendRequest {
  readonly requestId: string
  readonly accountId: string
  readonly displayName: string
  readonly createdAt: string
}

export interface FriendRequests {
  readonly incoming: FriendRequest[]
  readonly outgoing: FriendRequest[]
}

export async function fetchFriends(): Promise<Friend[]> {
  const res = await fetch('/api/friends', { headers: authHeaders() })
  if (res.status === 401) throw new UnauthorizedError()
  if (!res.ok) throw new Error(await errorMessage(res, `Failed to load friends (${res.status})`))
  return (await res.json()) as Friend[]
}

export async function fetchRequests(): Promise<FriendRequests> {
  const res = await fetch('/api/friends/requests', { headers: authHeaders() })
  if (res.status === 401) throw new UnauthorizedError()
  if (!res.ok) throw new Error(await errorMessage(res, `Failed to load requests (${res.status})`))
  return (await res.json()) as FriendRequests
}

/** Send a friend request by the recipient's account id (their friend code). */
export async function sendFriendRequest(accountId: string): Promise<FriendRequest> {
  const res = await fetch('/api/friends/requests', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...authHeaders() },
    body: JSON.stringify({ accountId }),
  })
  if (res.status === 401) throw new UnauthorizedError()
  if (!res.ok) throw new Error(await errorMessage(res, `Couldn't send the request (${res.status})`))
  return (await res.json()) as FriendRequest
}

export async function acceptRequest(requestId: string): Promise<void> {
  const res = await fetch(`/api/friends/requests/${requestId}/accept`, {
    method: 'POST',
    headers: authHeaders(),
  })
  if (res.status === 401) throw new UnauthorizedError()
  if (!res.ok) throw new Error(await errorMessage(res, `Couldn't accept the request (${res.status})`))
}

/** Decline an incoming request or cancel an outgoing one — both delete it. */
export async function removeRequest(requestId: string): Promise<void> {
  const res = await fetch(`/api/friends/requests/${requestId}`, {
    method: 'DELETE',
    headers: authHeaders(),
  })
  if (res.status === 401) throw new UnauthorizedError()
  if (!res.ok && res.status !== 404) throw new Error(`Couldn't update the request (${res.status})`)
}

export async function unfriend(accountId: string): Promise<void> {
  const res = await fetch(`/api/friends/${accountId}`, { method: 'DELETE', headers: authHeaders() })
  if (res.status === 401) throw new UnauthorizedError()
  if (!res.ok && res.status !== 404) throw new Error(`Couldn't unfriend (${res.status})`)
}

/** Toggle whether you appear offline to your friends even while connected. */
export async function setPresenceHidden(hidden: boolean): Promise<void> {
  const res = await fetch('/api/friends/visibility', {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json', ...authHeaders() },
    body: JSON.stringify({ hidden }),
  })
  if (res.status === 401) throw new UnauthorizedError()
  if (!res.ok) throw new Error(`Couldn't update your visibility (${res.status})`)
}
