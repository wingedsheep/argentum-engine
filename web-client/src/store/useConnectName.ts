/**
 * Resolves the name this browser can connect to the game server with — the one gate every entry
 * screen (home, `/join/:id`, `/tournament/:id`) puts in front of a player.
 */
import { useEffect } from 'react'
import { useAuthStore } from '@/store/authStore'

/** localStorage key holding the guest / last-used player name. */
export const PLAYER_NAME_KEY = 'argentum-player-name'

export interface ConnectName {
  /** The name to connect with, or null when this browser has no name yet. */
  readonly name: string | null
  /** True until the account check has come back — see the note on prompting below. */
  readonly resolving: boolean
}

/**
 * Two sources, account first: a signed-in account's display name is what the server puts on your
 * seat (`connectionSlice.connect` sends it over the stored one), so it *is* a name even on a
 * browser that has never stored one. Screens used to look only at localStorage, which asked a
 * logged-in player on a fresh device to type a name that the very next connect overwrote with
 * their profile name.
 *
 * `resolving` covers the window before `/api/config` + `/api/auth/me` answer. Connecting on a
 * stored name during it is harmless — the socket re-reads the account name when it opens — but
 * *prompting* for one is not: the prompt would flash and then vanish under a signed-in user. Any
 * screen that renders a name entry should hold it back until `resolving` clears.
 *
 * Also kicks the one-time auth bootstrap, so the deep-link entry pages resolve an account without
 * depending on the home screen having mounted first.
 */
export function useConnectName(): ConnectName {
  const status = useAuthStore((s) => s.status)
  const init = useAuthStore((s) => s.init)
  const accountName = useAuthStore((s) => s.user?.displayName.trim() || null)

  useEffect(() => {
    if (status === 'idle') void init()
  }, [status, init])

  // Read on every render rather than into state: `connect()` writes this key, and the callers all
  // re-render on the connection status that follows.
  const storedName = localStorage.getItem(PLAYER_NAME_KEY)?.trim() || null

  return {
    name: accountName ?? storedName,
    resolving: status === 'idle' || status === 'loading',
  }
}
