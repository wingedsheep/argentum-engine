/**
 * Direct messages: your conversation list and the conversations you've opened, kept current by the
 * WebSocket push (`directMessage` for a new message, `directMessagesChanged` for anything else) with
 * REST as the source of truth. Standalone like the friends store — messages are orthogonal to a game.
 *
 * Sending is optimistic: the message shows at once as `pending` and is swapped for the server's copy
 * when the request returns (or when the push from the server beats it there). A failed send is
 * removed and the error rethrown, so the composer can put the text back.
 */
import { create } from 'zustand'
import {
  type Conversation,
  type DirectMessage,
  type ThreadSummary,
  CannotMessageError,
  acceptMessageRequest,
  blockFromConversation,
  deleteConversation,
  fetchConversation,
  fetchThreads,
  markConversationRead,
  sendMessage,
} from '@/api/messages'
import type { DirectMessageMessage } from '@/types'

export interface ChatMessage extends DirectMessage {
  /** Shown before the server has confirmed it. */
  readonly pending?: boolean
}

export interface OpenConversation extends Omit<Conversation, 'messages'> {
  readonly messages: ChatMessage[]
  readonly loadingOlder: boolean
}

/** What the conversation pane shows for one account id. */
export type ConversationEntry =
  | { readonly status: 'loading' }
  | { readonly status: 'unavailable'; readonly message: string }
  | { readonly status: 'error'; readonly message: string }
  | ({ readonly status: 'ready' } & OpenConversation)

interface MessagesState {
  threads: ThreadSummary[]
  threadsLoaded: boolean
  conversations: Record<string, ConversationEntry>

  loadThreads: () => Promise<void>
  /** Fetch (or refresh) the newest page of a conversation. */
  openConversation: (accountId: string) => Promise<void>
  loadOlder: (accountId: string) => Promise<void>
  /** Throws with the server's message on failure, after removing the optimistic copy. */
  send: (accountId: string, myId: string, body: string) => Promise<void>
  markRead: (accountId: string) => void
  accept: (accountId: string) => Promise<void>
  /** Delete the conversation for yourself (also declines a request). */
  remove: (accountId: string) => Promise<void>
  block: (accountId: string) => Promise<void>

  /** WS push: a new message in one of your conversations. */
  receive: (msg: DirectMessageMessage) => void
  /** WS push: a conversation changed some other way — refetch it. */
  noteChanged: (accountId: string) => void
  reset: () => void
}

let pendingCounter = 0
let reloadTimer: number | undefined

/** Messages without duplicates, oldest first. */
function merge(existing: ChatMessage[], incoming: ChatMessage[]): ChatMessage[] {
  const ids = new Set(existing.map((m) => m.id))
  const added = incoming.filter((m) => !ids.has(m.id))
  if (added.length === 0) return existing
  return [...existing, ...added].sort((a, b) => a.createdAt.localeCompare(b.createdAt))
}

export const useMessagesStore = create<MessagesState>((set, get) => {
  const patchConversation = (accountId: string, patch: (c: OpenConversation) => Partial<OpenConversation>) =>
    set((s) => {
      const entry = s.conversations[accountId]
      if (!entry || entry.status !== 'ready') return s
      return { conversations: { ...s.conversations, [accountId]: { ...entry, ...patch(entry) } } }
    })

  /** Coalesce bursts of pushes into one thread-list fetch. */
  const scheduleThreadReload = () => {
    window.clearTimeout(reloadTimer)
    reloadTimer = window.setTimeout(() => void get().loadThreads(), 250)
  }

  return {
    threads: [],
    threadsLoaded: false,
    conversations: {},

    loadThreads: async () => {
      try {
        const threads = await fetchThreads()
        set({ threads, threadsLoaded: true })
      } catch {
        set({ threadsLoaded: true })
      }
    },

    openConversation: async (accountId) => {
      const current = get().conversations[accountId]
      if (!current || current.status !== 'ready') {
        set((s) => ({ conversations: { ...s.conversations, [accountId]: { status: 'loading' } } }))
      }
      try {
        const c = await fetchConversation(accountId)
        set((s) => {
          const prev = s.conversations[accountId]
          // Keep older pages already loaded and any message still in flight.
          const kept = prev?.status === 'ready' ? prev.messages : []
          const pending = kept.filter((m) => m.pending)
          const confirmed = merge(kept.filter((m) => !m.pending), c.messages)
          return {
            conversations: {
              ...s.conversations,
              [accountId]: {
                status: 'ready',
                ...c,
                hasMore: prev?.status === 'ready' && kept.length > c.messages.length ? prev.hasMore : c.hasMore,
                messages: [...confirmed, ...pending],
                loadingOlder: false,
              },
            },
          }
        })
      } catch (e) {
        const message = e instanceof Error ? e.message : 'Couldn’t load the conversation.'
        set((s) => ({
          conversations: {
            ...s.conversations,
            [accountId]: e instanceof CannotMessageError ? { status: 'unavailable', message } : { status: 'error', message },
          },
        }))
      }
    },

    loadOlder: async (accountId) => {
      const entry = get().conversations[accountId]
      if (!entry || entry.status !== 'ready' || !entry.hasMore || entry.loadingOlder) return
      const oldest = entry.messages.find((m) => !m.pending)
      if (!oldest) return
      patchConversation(accountId, () => ({ loadingOlder: true }))
      try {
        const page = await fetchConversation(accountId, oldest.createdAt)
        patchConversation(accountId, (c) => ({
          messages: merge(page.messages, c.messages),
          hasMore: page.hasMore,
          loadingOlder: false,
        }))
      } catch {
        patchConversation(accountId, () => ({ loadingOlder: false }))
      }
    },

    send: async (accountId, myId, body) => {
      const tempId = `pending-${++pendingCounter}`
      const optimistic: ChatMessage = {
        id: tempId,
        senderId: myId,
        body: body.trim(),
        createdAt: new Date().toISOString(),
        pending: true,
      }
      patchConversation(accountId, (c) => ({ messages: [...c.messages, optimistic] }))
      try {
        const sent = await sendMessage(accountId, body)
        patchConversation(accountId, (c) => {
          const without = c.messages.filter((m) => m.id !== tempId)
          return {
            messages: merge(without, [sent]),
            requestMessagesLeft:
              c.state === 'OUTGOING_REQUEST' && c.requestMessagesLeft != null
                ? Math.max(0, c.requestMessagesLeft - 1)
                : c.requestMessagesLeft,
            // Replying to a request accepts it.
            state: c.state === 'INCOMING_REQUEST' ? 'ACTIVE' : c.state,
          }
        })
        scheduleThreadReload()
      } catch (e) {
        patchConversation(accountId, (c) => ({ messages: c.messages.filter((m) => m.id !== tempId) }))
        if (e instanceof CannotMessageError) void get().openConversation(accountId)
        throw e
      }
    },

    markRead: (accountId) => {
      const thread = get().threads.find((t) => t.other.accountId === accountId)
      if (!thread || thread.unread === 0) return
      set((s) => ({
        threads: s.threads.map((t) => (t.other.accountId === accountId ? { ...t, unread: 0 } : t)),
      }))
      void markConversationRead(accountId).catch(() => {})
    },

    accept: async (accountId) => {
      await acceptMessageRequest(accountId)
      patchConversation(accountId, () => ({ state: 'ACTIVE', requestMessagesLeft: null }))
      set((s) => ({
        threads: s.threads.map((t) => (t.other.accountId === accountId ? { ...t, state: 'ACTIVE' } : t)),
      }))
    },

    remove: async (accountId) => {
      await deleteConversation(accountId)
      set((s) => {
        const { [accountId]: _dropped, ...rest } = s.conversations
        return { threads: s.threads.filter((t) => t.other.accountId !== accountId), conversations: rest }
      })
    },

    block: async (accountId) => {
      await blockFromConversation(accountId)
      set((s) => {
        const { [accountId]: _dropped, ...rest } = s.conversations
        return { threads: s.threads.filter((t) => t.other.accountId !== accountId), conversations: rest }
      })
    },

    receive: (msg) => {
      const accountId = msg.withAccountId
      patchConversation(accountId, (c) => {
        // Our own message coming back from the server replaces its optimistic copy.
        const pendingCopy = c.messages.find((m) => m.pending && m.senderId === msg.message.senderId && m.body === msg.message.body)
        const base = pendingCopy ? c.messages.filter((m) => m !== pendingCopy) : c.messages
        const fromThem = msg.message.senderId === accountId
        return {
          messages: merge(base, [msg.message]),
          // Their message on a request we sent means they accepted it.
          ...(fromThem && c.state === 'OUTGOING_REQUEST' && !msg.request ? { state: 'ACTIVE', requestMessagesLeft: null } : {}),
        }
      })
      scheduleThreadReload()
    },

    noteChanged: (accountId) => {
      const entry = get().conversations[accountId]
      if (entry?.status === 'ready') void get().openConversation(accountId)
      scheduleThreadReload()
    },

    reset: () => {
      window.clearTimeout(reloadTimer)
      set({ threads: [], threadsLoaded: false, conversations: {} })
    },
  }
})

/** Unread messages in your chats plus unanswered requests — the number on the Messages badge. */
export function unreadBadgeCount(threads: readonly ThreadSummary[]): number {
  return threads.reduce((n, t) => {
    if (t.state === 'INCOMING_REQUEST') return n + (t.unread > 0 ? 1 : 0)
    return n + t.unread
  }, 0)
}
