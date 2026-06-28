/**
 * Standalone account/auth store (separate from the game store — accounts are orthogonal to a live
 * game). Holds the signed-in user and drives the magic-link login flow. The auth token itself lives
 * in localStorage (see api/account); this store mirrors the derived user for the UI.
 */
import { create } from 'zustand'
import {
  type AccountUser,
  type LoginResponse,
  clearAuthToken,
  fetchAppConfig,
  fetchMe,
  setAuthToken,
  updateProfile,
} from '@/api/account'

export type AuthStatus = 'idle' | 'loading' | 'authenticated' | 'anonymous'

interface AuthState {
  user: AccountUser | null
  status: AuthStatus
  /**
   * Whether the server runs the accounts subsystem. Defaults to false (hide all account UI) until
   * {@link init} learns otherwise, so a server with accounts disabled never shows a sign-in form
   * that can only fail.
   */
  accountsEnabled: boolean
  /** Resolve server config + the current session from a stored token (call once on app start). */
  init: () => Promise<void>
  /** Apply a fresh login (stores the token and the user). */
  setSession: (login: LoginResponse) => void
  /** Change the signed-in user's display name (persists to the server, then updates the store). */
  updateDisplayName: (displayName: string) => Promise<void>
  /** Patch fields of the signed-in user in the store (e.g. after toggling presence visibility). */
  patchUser: (patch: Partial<AccountUser>) => void
  /** Sign out: drop the token and the user. */
  logout: () => void
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  status: 'idle',
  accountsEnabled: false,

  init: async () => {
    set({ status: 'loading' })
    const { accountsEnabled } = await fetchAppConfig()
    if (!accountsEnabled) {
      set({ user: null, status: 'anonymous', accountsEnabled: false })
      return
    }
    const user = await fetchMe()
    set(
      user
        ? { user, status: 'authenticated', accountsEnabled: true }
        : { user: null, status: 'anonymous', accountsEnabled: true },
    )
  },

  setSession: (login) => {
    setAuthToken(login.authToken)
    set({ user: login.user, status: 'authenticated' })
  },

  updateDisplayName: async (displayName) => {
    const user = await updateProfile(displayName)
    set({ user })
  },

  patchUser: (patch) => set((s) => (s.user ? { user: { ...s.user, ...patch } } : {})),

  logout: () => {
    clearAuthToken()
    set({ user: null, status: 'anonymous' })
  },
}))
