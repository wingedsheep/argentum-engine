/**
 * REST client for direct messages (`/api/messages/*`). Every conversation is addressed by the other
 * person's account id. Friends message freely; anyone else's first messages arrive as a *message
 * request* the recipient accepts, deletes or blocks. New messages are also pushed live over the
 * WebSocket (`directMessage`); this is the read/write side. Only mounted when accounts are enabled.
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

/** How a conversation stands from your side. */
export type ThreadState = 'ACTIVE' | 'INCOMING_REQUEST' | 'OUTGOING_REQUEST'

export interface Participant {
  readonly accountId: string
  readonly displayName: string
  /** Preset avatar id, or null for the initial. */
  readonly avatar?: string | null
  readonly online: boolean
}

export interface DirectMessage {
  readonly id: string
  readonly senderId: string
  readonly body: string
  readonly createdAt: string
}

export interface ThreadSummary {
  readonly other: Participant
  readonly state: ThreadState
  readonly isFriend: boolean
  readonly lastMessage: DirectMessage
  readonly unread: number
}

export interface Conversation {
  readonly other: Participant
  readonly state: ThreadState
  readonly isFriend: boolean
  /** Oldest first. */
  readonly messages: DirectMessage[]
  readonly hasMore: boolean
  /** While your request is unanswered: how many more messages you can send. */
  readonly requestMessagesLeft: number | null
  readonly maxLength: number
}

/** The conversation can't be opened: no such account, or a block stands between you. */
export class CannotMessageError extends Error {}

export async function fetchThreads(): Promise<ThreadSummary[]> {
  const res = await fetch('/api/messages', { headers: authHeaders() })
  if (res.status === 401) throw new UnauthorizedError()
  if (!res.ok) throw new Error(await errorMessage(res, `Failed to load messages (${res.status})`))
  return (await res.json()) as ThreadSummary[]
}

/** The newest page of a conversation, or the page before [before] (an ISO timestamp). */
export async function fetchConversation(accountId: string, before?: string): Promise<Conversation> {
  const query = before ? `?before=${encodeURIComponent(before)}` : ''
  const res = await fetch(`/api/messages/${accountId}${query}`, { headers: authHeaders() })
  if (res.status === 401) throw new UnauthorizedError()
  if (res.status === 404 || res.status === 400) throw new CannotMessageError(await errorMessage(res, 'You can’t message this player.'))
  if (!res.ok) throw new Error(await errorMessage(res, `Failed to load the conversation (${res.status})`))
  return (await res.json()) as Conversation
}

export async function sendMessage(accountId: string, body: string): Promise<DirectMessage> {
  const res = await fetch(`/api/messages/${accountId}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...authHeaders() },
    body: JSON.stringify({ body }),
  })
  if (res.status === 401) throw new UnauthorizedError()
  if (res.status === 404) throw new CannotMessageError(await errorMessage(res, 'You can’t message this player.'))
  if (!res.ok) throw new Error(await errorMessage(res, `Couldn’t send the message (${res.status})`))
  return (await res.json()) as DirectMessage
}

async function post(path: string, fallback: string, method = 'POST'): Promise<void> {
  const res = await fetch(`/api/messages/${path}`, { method, headers: authHeaders() })
  if (res.status === 401) throw new UnauthorizedError()
  if (!res.ok && res.status !== 404) throw new Error(await errorMessage(res, `${fallback} (${res.status})`))
}

export const acceptMessageRequest = (accountId: string) => post(`${accountId}/accept`, 'Couldn’t accept the request')
export const markConversationRead = (accountId: string) => post(`${accountId}/read`, 'Couldn’t mark as read')
/** Delete the conversation for yourself — also how a request is declined. */
export const deleteConversation = (accountId: string) => post(accountId, 'Couldn’t delete the conversation', 'DELETE')
export const blockFromConversation = (accountId: string) => post(`${accountId}/block`, 'Couldn’t block this player')
