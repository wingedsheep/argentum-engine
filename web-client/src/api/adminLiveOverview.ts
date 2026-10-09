/**
 * REST client for the admin Live overview (`/api/admin/live-overview`): every game session and every
 * running tournament lobby the server holds in memory right now. Needs no database, so it works on a
 * server without accounts too. Auth is the dashboard's shared {@link AdminAuth}.
 */
import { type AdminAuth, adminAuthHeaders } from './adminAuth'

export interface LiveSeat {
  readonly name: string
  readonly isAi: boolean
  readonly connected: boolean
  /** Null before the game has started. */
  readonly life: number | null
}

export interface LiveGame {
  readonly gameSessionId: string
  readonly seats: LiveSeat[]
  /** LobbyGameMode name, QUICK_GAME, or CASUAL — the same vocabulary as recorded match history. */
  readonly gameMode: string
  readonly format: string
  readonly setCode: string | null
  readonly ranked: boolean
  readonly publicSpectate: boolean
  readonly tournamentLobbyId: string | null
  readonly started: boolean
  readonly gameOver: boolean
  readonly turnNumber: number | null
  readonly activePlayerName: string | null
  readonly step: string | null
  readonly startedAt: string | null
  readonly lastActionAt: string | null
  readonly spectatorCount: number
  /** At least one human seat with an open socket — a restart would interrupt this game. */
  readonly hasConnectedHuman: boolean
}

export interface LiveLobby {
  readonly lobbyId: string
  readonly gameMode: string
  readonly format: string
  /** DRAFTING, DECK_BUILDING or TOURNAMENT_ACTIVE. */
  readonly state: string
  readonly setNames: string[]
  readonly humanPlayers: number
  readonly connectedHumans: number
  readonly currentRound: number | null
  readonly totalRounds: number | null
}

/** What an online player is doing — the server's `PlayerActivityResolver.Kind`, in precedence order. */
export type ActivityKind =
  | 'PLAYING'
  | 'SPECTATING'
  | 'DRAFTING'
  | 'BUILDING_LIMITED'
  | 'TOURNAMENT'
  | 'LOBBY'
  | 'SEARCHING'
  | 'DECKBUILDER'
  | 'BROWSING'
  | 'HOME'

export interface OnlinePlayer {
  readonly name: string
  readonly signedIn: boolean
  readonly activity: ActivityKind
  /** Server-written sentence: "Drafting · Bloomburrow · pack 2, 17 picked". */
  readonly detail: string
  /** The game to watch, when seated in or spectating one. */
  readonly gameSessionId: string | null
  /** Matchmaking queue searched beside the main activity, e.g. while in a practice game. */
  readonly searching: string | null
  readonly searchingSince: string | null
  readonly page: string | null
  readonly pageSince: string | null
  /** Last real input (click, pick, game action) — not a page report or heartbeat. */
  readonly lastInputAt: string | null
}

export interface ActivityFeedEntry {
  readonly at: string
  readonly playerName: string
  readonly signedIn: boolean
  /** connect, lobby, deck, queue, watch, game, page. */
  readonly kind: string
  readonly text: string
}

export interface LiveOverview {
  readonly generatedAt: string
  readonly onlinePlayers: number
  readonly games: LiveGame[]
  readonly lobbies: LiveLobby[]
  readonly players: OnlinePlayer[]
  /** Newest first; in memory only, so empty right after a server restart. */
  readonly feed: ActivityFeedEntry[]
}

export async function fetchLiveOverview(auth: AdminAuth): Promise<LiveOverview> {
  const res = await fetch('/api/admin/live-overview', { headers: adminAuthHeaders(auth) })
  if (!res.ok) throw new Error(`Failed to load the live overview (${res.status})`)
  return (await res.json()) as LiveOverview
}
