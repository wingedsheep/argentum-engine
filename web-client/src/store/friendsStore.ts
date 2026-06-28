/**
 * Standalone friends store (separate from the game store — friends are orthogonal to a live game).
 * Holds the accepted-friends list with live online status plus the incoming/outgoing request lists.
 *
 * Freshness comes from two sources: an explicit {@link load} (on the friends page, on sign-in, and on
 * a slow poll) and live WebSocket pushes ({@link applyPresence} when a friend's online status flips,
 * {@link noteIncomingRequest} when someone sends a request). The push keeps presence and the
 * incoming-request badge current without the page open; the poll is the catch-all for the passive side
 * of an accept/unfriend.
 */
import { create } from 'zustand'
import {
  type Friend,
  type FriendRequest,
  acceptRequest,
  fetchFriends,
  fetchRequests,
  removeRequest,
  sendFriendRequest,
  unfriend as unfriendApi,
} from '@/api/friends'

interface FriendsState {
  friends: Friend[]
  incoming: FriendRequest[]
  outgoing: FriendRequest[]
  loading: boolean
  error: string | null

  /** Reload friends + requests from the server. */
  load: () => Promise<void>
  /** Send a friend request by account id; throws with the server's message so the page can show it. */
  sendRequest: (accountId: string) => Promise<void>
  accept: (requestId: string) => Promise<void>
  /** Decline an incoming request or cancel an outgoing one. */
  removeRequest: (requestId: string) => Promise<void>
  unfriend: (accountId: string) => Promise<void>

  /** WS push: a friend's visible online status changed. */
  applyPresence: (accountId: string, online: boolean) => void
  /** WS push: someone sent you a request — refresh the incoming list. */
  noteIncomingRequest: () => void
  /** Clear on sign-out. */
  reset: () => void
}

export const useFriendsStore = create<FriendsState>((set, get) => ({
  friends: [],
  incoming: [],
  outgoing: [],
  loading: false,
  error: null,

  load: async () => {
    set({ loading: true })
    try {
      const [friends, requests] = await Promise.all([fetchFriends(), fetchRequests()])
      set({ friends, incoming: requests.incoming, outgoing: requests.outgoing, error: null })
    } catch (e) {
      set({ error: e instanceof Error ? e.message : 'Failed to load friends' })
    } finally {
      set({ loading: false })
    }
  },

  sendRequest: async (accountId) => {
    await sendFriendRequest(accountId.trim())
    await get().load()
  },

  accept: async (requestId) => {
    await acceptRequest(requestId)
    await get().load()
  },

  removeRequest: async (requestId) => {
    await removeRequest(requestId)
    await get().load()
  },

  unfriend: async (accountId) => {
    await unfriendApi(accountId)
    await get().load()
  },

  applyPresence: (accountId, online) =>
    set((s) => ({
      friends: s.friends.map((f) => (f.accountId === accountId ? { ...f, online } : f)),
    })),

  noteIncomingRequest: () => {
    void get().load()
  },

  reset: () => set({ friends: [], incoming: [], outgoing: [], loading: false, error: null }),
}))
