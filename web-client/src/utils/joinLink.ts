/**
 * Builds the shareable deep-link a player scans / clicks to join a lobby.
 *
 * The `/join/:lobbyId` route (see `main.tsx` → `JoinLobbyPage`) is lobby-kind agnostic: the server's
 * quick-game join handler delegates to the tournament join handler when the id is a tournament lobby,
 * so one link works for Quick Game lobbies, sealed/draft lobbies and tournaments alike.
 */
export function buildJoinUrl(lobbyId: string, origin: string = window.location.origin): string {
  // Strip any trailing slash so we never emit a double slash for a root-hosted origin.
  return `${origin.replace(/\/$/, '')}/join/${encodeURIComponent(lobbyId)}`
}
