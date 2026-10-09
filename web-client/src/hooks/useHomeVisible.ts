import { useLocation } from 'react-router-dom'
import { useGameStore } from '@/store/gameStore'

/** Routes that render `App` (the `*` route), where the lobby and the home panel live. */
export function isAppRoute(pathname: string): boolean {
  return pathname === '/' || pathname === ''
}

/**
 * Whether the player is looking at the home screen: on `App`'s route with nothing else up — no game,
 * mulligan, lobby, result screen or spectated game. For the layers mounted beside the router
 * (matchmaking, the chat dock) that behave differently there.
 */
export function useHomeVisible(): boolean {
  const { pathname } = useLocation()
  const busy = useGameStore((s) =>
    s.gameState != null ||
    s.mulliganState != null ||
    s.waitingForOpponentMulligan ||
    s.gameOverState != null ||
    s.quickGameLobbyState != null ||
    s.lobbyState != null ||
    s.spectatingState != null,
  )
  return isAppRoute(pathname) && !busy
}
