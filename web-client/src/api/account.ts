/**
 * REST client for the optional accounts subsystem: magic-link auth, server-side saved decks, and
 * win/loss stats. The account endpoints aren't mounted when the server has accounts disabled, so the
 * client first asks {@link fetchAppConfig} whether accounts exist and hides all account UI when they
 * don't; the individual calls still fail gracefully as a backstop.
 *
 * The auth token (a signed token from magic-link login) lives in localStorage under `argentum-auth`
 * and is sent as `Authorization: Bearer <token>` on authenticated calls. It is also picked up by the
 * WebSocket connect handshake (see connectionSlice) to attribute games to the account.
 */
import type { SharedDeck } from '@/components/deckbuilder/shareDeck'
import type { CubeBoosterStyle, CubeRarity } from '@/types'

export type { CubeBoosterStyle, CubeRarity }

const AUTH_KEY = 'argentum-auth'

export function getAuthToken(): string | null {
  return localStorage.getItem(AUTH_KEY)
}
export function setAuthToken(token: string): void {
  localStorage.setItem(AUTH_KEY, token)
}
export function clearAuthToken(): void {
  localStorage.removeItem(AUTH_KEY)
}

/** Thrown when an authenticated request is rejected — the caller should clear the session. */
export class UnauthorizedError extends Error {}

function authHeaders(): Record<string, string> {
  const token = getAuthToken()
  return token ? { Authorization: `Bearer ${token}` } : {}
}

async function errorMessage(res: Response, fallback: string): Promise<string> {
  const body = (await res.json().catch(() => null)) as { error?: string; timestamp?: unknown; path?: unknown } | null
  // Spring's default error body also carries `error` — the bare HTTP reason ("Not Found") — beside
  // `timestamp` and `path`. That is not a message for a player; only our own `{ error }` bodies are.
  if (body?.timestamp !== undefined && body.path !== undefined) return fallback
  return body?.error ?? fallback
}

export interface AccountUser {
  /** UUID. Doubles as the shareable "friend code" — invite friends by this id, not your email. */
  readonly id: string
  readonly email: string
  readonly displayName: string
  /** True when this account has been granted access to the admin dashboard. */
  readonly isAdmin: boolean
  /** When true the account appears offline to its friends even while connected. */
  readonly hidePresence: boolean
  /** Chosen preset avatar id (see components/profile/avatars), or null for the initial. */
  readonly avatar?: string | null
}

export interface LoginResponse {
  readonly authToken: string
  readonly user: AccountUser
}

// ----- App config -----

/** Client bootstrap config from the always-mounted `/api/config` endpoint. */
export interface AppConfig {
  /** True when the server runs the optional accounts/magic-link subsystem. */
  readonly accountsEnabled: boolean
}

/**
 * Fetch server feature availability. Used on startup to decide whether to show the accounts/sign-in
 * UI at all — on a server with accounts disabled the auth endpoints don't exist, so showing a
 * sign-in form would only produce a confusing error.
 *
 * Resolves `null` when the server couldn't be reached (down, restarting, a proxy's 502): that's
 * "unknown", not "accounts disabled", and the caller retries rather than settling on it.
 */
export async function fetchAppConfig(): Promise<AppConfig | null> {
  try {
    const res = await fetch('/api/config')
    if (!res.ok) return null
    return (await res.json()) as AppConfig
  } catch {
    return null
  }
}

// ----- Auth -----

/**
 * Request a magic-link sign-in email. Resolves on success regardless of whether the email exists.
 *
 * `devLoginPath` is present only on a dev server that can't send mail (dev endpoints on, no mail
 * credentials): the link's own path, so local sign-in doesn't mean fishing it out of the server log.
 */
export async function requestLogin(email: string): Promise<{ devLoginPath?: string }> {
  const res = await fetch('/api/auth/request-login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email }),
  })
  if (!res.ok) throw new Error(await errorMessage(res, `Sign-in request failed (${res.status})`))
  const body = await res.json().catch(() => ({})) as { devLoginPath?: unknown }
  return typeof body.devLoginPath === 'string' && body.devLoginPath.startsWith('/login/verify')
    ? { devLoginPath: body.devLoginPath }
    : {}
}

/** Exchange a magic-link token for a durable auth token + the account. */
export async function verifyLogin(token: string): Promise<LoginResponse> {
  const res = await fetch('/api/auth/verify', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ token }),
  })
  if (!res.ok) throw new Error(await errorMessage(res, 'This sign-in link is invalid or expired'))
  return (await res.json()) as LoginResponse
}

/** Update the signed-in account's display name. Returns the updated account. */
export async function updateProfile(displayName: string): Promise<AccountUser> {
  const res = await fetch('/api/auth/me', {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json', ...authHeaders() },
    body: JSON.stringify({ displayName }),
  })
  if (res.status === 401) throw new UnauthorizedError()
  if (!res.ok) throw new Error(await errorMessage(res, `Failed to update profile (${res.status})`))
  return (await res.json()) as AccountUser
}

/** Pick a preset avatar, or `null` to go back to the initial. Returns the updated account. */
export async function updateAvatar(avatar: string | null): Promise<AccountUser> {
  const res = await fetch('/api/auth/me/avatar', {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json', ...authHeaders() },
    body: JSON.stringify({ avatar }),
  })
  if (res.status === 401) throw new UnauthorizedError()
  if (!res.ok) throw new Error(await errorMessage(res, `Failed to update avatar (${res.status})`))
  return (await res.json()) as AccountUser
}

/** Fetch the current account, or null if not signed in. Throws when the server can't answer. */
export async function fetchMe(): Promise<AccountUser | null> {
  if (!getAuthToken()) return null
  const res = await fetch('/api/auth/me', { headers: authHeaders() })
  if (res.status === 401) return null
  // Any other failure leaves the session unknown, not signed out — throw so the caller can retry.
  if (!res.ok) throw new Error(`Failed to load the signed-in account (${res.status})`)
  return (await res.json()) as AccountUser
}

// ----- Saved decks -----

/**
 * Learn to Play progress on the account — the client's own document, stored verbatim by the
 * server. `{}` when the course was never started there. See `learn/progressStore.ts` for the merge.
 */
export async function fetchLearnProgress(): Promise<unknown> {
  const res = await fetch('/api/auth/me/learn-progress', { headers: authHeaders() })
  if (res.status === 401) throw new UnauthorizedError()
  if (!res.ok) throw new Error(`Failed to load course progress (${res.status})`)
  return res.json()
}

export async function saveLearnProgress(progress: unknown): Promise<void> {
  const res = await fetch('/api/auth/me/learn-progress', {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json', ...authHeaders() },
    body: JSON.stringify(progress),
  })
  if (res.status === 401) throw new UnauthorizedError()
  if (!res.ok) throw new Error(`Failed to save course progress (${res.status})`)
}

/**
 * Player preferences on the account — the client's own document, stored verbatim by the server.
 * `{}` when none were saved there yet. See `store/preferencesStore.ts` for the sync.
 */
export async function fetchPreferences(): Promise<unknown> {
  const res = await fetch('/api/auth/me/preferences', { headers: authHeaders() })
  if (res.status === 401) throw new UnauthorizedError()
  if (!res.ok) throw new Error(`Failed to load preferences (${res.status})`)
  return res.json()
}

export async function savePreferences(preferences: unknown): Promise<void> {
  const res = await fetch('/api/auth/me/preferences', {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json', ...authHeaders() },
    body: JSON.stringify(preferences),
  })
  if (res.status === 401) throw new UnauthorizedError()
  if (!res.ok) throw new Error(`Failed to save preferences (${res.status})`)
}

export interface DeckSummary {
  readonly id: number
  readonly name: string
  readonly format?: string
  readonly updatedAt: string
}

export interface DeckDetail extends DeckSummary {
  readonly deck: SharedDeck
}

export async function listDecks(): Promise<DeckSummary[]> {
  const res = await fetch('/api/account/decks', { headers: authHeaders() })
  if (res.status === 401) throw new UnauthorizedError()
  if (!res.ok) throw new Error(`Failed to load decks (${res.status})`)
  return (await res.json()) as DeckSummary[]
}

/** Full detail for every saved deck in one request (`?full`) — used by the unified deck browser. */
export async function listDeckDetails(): Promise<DeckDetail[]> {
  const res = await fetch('/api/account/decks?full', { headers: authHeaders() })
  if (res.status === 401) throw new UnauthorizedError()
  if (!res.ok) throw new Error(`Failed to load decks (${res.status})`)
  return (await res.json()) as DeckDetail[]
}

export async function getDeck(id: number): Promise<DeckDetail> {
  const res = await fetch(`/api/account/decks/${id}`, { headers: authHeaders() })
  if (res.status === 401) throw new UnauthorizedError()
  if (!res.ok) throw new Error(`Failed to load deck (${res.status})`)
  return (await res.json()) as DeckDetail
}

export async function saveDeck(deck: SharedDeck): Promise<DeckDetail> {
  const res = await fetch('/api/account/decks', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...authHeaders() },
    body: JSON.stringify(deck),
  })
  if (res.status === 401) throw new UnauthorizedError()
  if (!res.ok) throw new Error(`Failed to save deck (${res.status})`)
  return (await res.json()) as DeckDetail
}

export async function updateDeck(id: number, deck: SharedDeck): Promise<DeckDetail> {
  const res = await fetch(`/api/account/decks/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json', ...authHeaders() },
    body: JSON.stringify(deck),
  })
  if (res.status === 401) throw new UnauthorizedError()
  if (!res.ok) throw new Error(`Failed to update deck (${res.status})`)
  return (await res.json()) as DeckDetail
}

export async function deleteDeck(id: number): Promise<void> {
  const res = await fetch(`/api/account/decks/${id}`, { method: 'DELETE', headers: authHeaders() })
  if (res.status === 401) throw new UnauthorizedError()
  if (!res.ok && res.status !== 404) throw new Error(`Failed to delete deck (${res.status})`)
}

/**
 * Save a deck to the account, overwriting any existing deck with the same name (case-insensitive)
 * rather than creating a duplicate. This is the single cloud-save path shared by the deckbuilder, the
 * tournament/draft save, the deck picker, and the sign-in migration — so "save when signed in" means
 * the same thing everywhere.
 */
export async function upsertDeckByName(deck: SharedDeck): Promise<DeckDetail> {
  const existing = await listDecks()
  const match = existing.find((d) => d.name.toLowerCase() === deck.name.toLowerCase())
  return match ? updateDeck(match.id, deck) : saveDeck(deck)
}

// ----- Saved cubes -----

/** One line of a stored cube. `rarity` is an explicit override; absent means the card's own rarity. */
export interface SharedCubeEntry {
  readonly name: string
  readonly count: number
  readonly rarity?: CubeRarity
}

/**
 * A cube as stored in the account — card names + counts, not resolved cards, so a cube can name a
 * card that isn't implemented yet and stays valid as sets land. Mirrors `SavedCube` minus the local
 * id/timestamp, and the server's `CubeList`. Everything after `packSize` is optional so cubes saved
 * before booster styles existed still read as plain shuffled cubes.
 */
export interface SharedCube {
  readonly name: string
  readonly description?: string
  readonly cards: ReadonlyArray<SharedCubeEntry>
  readonly basicLandSetCode: string
  /** Cards per pack — only meaningful for a `SHUFFLED` cube; booster styles fix their own size. */
  readonly packSize: number
  /** Absent = `SHUFFLED`. */
  readonly boosterStyle?: CubeBoosterStyle
}

export interface CubeSummary {
  readonly id: number
  readonly name: string
  readonly cardCount: number
  readonly updatedAt: string
}

export interface CubeDetail extends CubeSummary {
  readonly cube: SharedCube
  /** The public link code when the owner has shared this cube; null when it is private. */
  readonly shareCode?: string | null
}

/** A cube opened through someone's share link — always the owner's latest saved version. */
export interface SharedCubeView {
  readonly shareCode: string
  readonly name: string
  readonly cardCount: number
  readonly updatedAt: string
  readonly ownerName: string | null
  readonly cube: SharedCube
}

export async function listCubes(): Promise<CubeSummary[]> {
  const res = await fetch('/api/account/cubes', { headers: authHeaders() })
  if (res.status === 401) throw new UnauthorizedError()
  if (!res.ok) throw new Error(`Failed to load cubes (${res.status})`)
  return (await res.json()) as CubeSummary[]
}

/** Full detail for every saved cube in one request (`?full`) — used by the unified cube library. */
export async function listCubeDetails(): Promise<CubeDetail[]> {
  const res = await fetch('/api/account/cubes?full', { headers: authHeaders() })
  if (res.status === 401) throw new UnauthorizedError()
  if (!res.ok) throw new Error(`Failed to load cubes (${res.status})`)
  return (await res.json()) as CubeDetail[]
}

export async function saveCube(cube: SharedCube): Promise<CubeDetail> {
  const res = await fetch('/api/account/cubes', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...authHeaders() },
    body: JSON.stringify(cube),
  })
  if (res.status === 401) throw new UnauthorizedError()
  if (!res.ok) throw new Error(`Failed to save cube (${res.status})`)
  return (await res.json()) as CubeDetail
}

export async function updateCube(id: number, cube: SharedCube): Promise<CubeDetail> {
  const res = await fetch(`/api/account/cubes/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json', ...authHeaders() },
    body: JSON.stringify(cube),
  })
  if (res.status === 401) throw new UnauthorizedError()
  if (!res.ok) throw new Error(`Failed to update cube (${res.status})`)
  return (await res.json()) as CubeDetail
}

export async function deleteCube(id: number): Promise<void> {
  const res = await fetch(`/api/account/cubes/${id}`, { method: 'DELETE', headers: authHeaders() })
  if (res.status === 401) throw new UnauthorizedError()
  if (!res.ok && res.status !== 404) throw new Error(`Failed to delete cube (${res.status})`)
}

/** Create (or return the existing) public link for a saved cloud cube. */
export async function shareCube(id: number): Promise<string> {
  const res = await fetch(`/api/account/cubes/${id}/share`, { method: 'POST', headers: authHeaders() })
  if (res.status === 401) throw new UnauthorizedError()
  if (!res.ok) throw new Error(`Failed to create a share link (${res.status})`)
  return ((await res.json()) as { shareCode: string }).shareCode
}

/** Revoke a cube's public link; anyone holding the old link gets "not found" from then on. */
export async function unshareCube(id: number): Promise<void> {
  const res = await fetch(`/api/account/cubes/${id}/share`, { method: 'DELETE', headers: authHeaders() })
  if (res.status === 401) throw new UnauthorizedError()
  if (!res.ok && res.status !== 404) throw new Error(`Failed to revoke the share link (${res.status})`)
}

/** Public, no auth. Null when the code is unknown or revoked, or the server has no accounts. */
export async function getSharedCube(code: string): Promise<SharedCubeView | null> {
  const res = await fetch(`/api/cubes/shared/${encodeURIComponent(code)}`)
  if (res.status === 404) return null
  if (!res.ok) throw new Error(`Failed to load the shared cube (${res.status})`)
  return (await res.json()) as SharedCubeView
}

/** The browser path a share code opens on. */
export function sharedCubePath(code: string): string {
  return `/cubes/shared/${encodeURIComponent(code)}`
}

// ----- Stats -----

export interface AccountStats {
  readonly games: number
  readonly wins: number
  readonly losses: number
  readonly winRate: number
}

/** A `(label, count)` bucket — used for color / set / mode breakdowns. */
export interface StatBucket {
  readonly label: string
  readonly count: number
}

export interface HeadToHead {
  readonly opponent: string
  readonly opponentUserId: string | null
  readonly isAi: boolean
  readonly wins: number
  readonly losses: number
}

/** One opponent seat in a recorded game; `userId` is set only for signed-in opponents. */
export interface GameOpponent {
  readonly name: string
  readonly userId: string | null
  readonly isAi: boolean
}

export interface GameHistoryEntry {
  readonly endedAt: string
  readonly gameMode: string | null
  readonly format: string | null
  readonly colors: string | null
  /** Comma-joined opponent names — a plain label fallback. */
  readonly opponents: string | null
  /** Per-seat opponents, so signed-in opponents can link to their public profile. */
  readonly opponentList: GameOpponent[]
  readonly won: boolean
  /** This player's ELO before a ranked game; null for non-ranked games. */
  readonly selfRating: number | null
  /** The opponent's ELO at the time of a ranked game; null otherwise. */
  readonly opponentRating: number | null
  /**
   * How this game moved the player's ELO (rating after − before), for ranked games; null otherwise.
   * Positive for a gain, negative for a loss.
   */
  readonly ratingDelta: number | null
  readonly gameId: string
  /** True when a compact replay was stored for this game and can be watched/shared. */
  readonly hasReplay: boolean
}

export interface CardStat {
  readonly cardName: string
  readonly copies: number
  readonly decks: number
  /** Default-printing art URL (per-user top cards only); null when unresolved. */
  readonly imageUri: string | null
}

/** Tournament lifecycle status, mirroring the server's `TournamentStatus`. */
export type TournamentStatus = 'IN_PROGRESS' | 'COMPLETED' | 'ABANDONED'

export interface UserTournamentEntry {
  /** Opens the full tournament detail (standings + games). */
  readonly id: number
  /** Completion time, or the start time while still in progress. */
  readonly endedAt: string
  readonly name: string | null
  readonly format: string | null
  readonly gameMode: string | null
  /**
   * Final placement (1 = winner). For an in-progress or abandoned tournament, the standing after the
   * last recorded match (0 if none yet).
   */
  readonly placement: number
  readonly playerCount: number
  readonly status: TournamentStatus
  /** Match record — final once completed, last-known for an in-progress or abandoned tournament. */
  readonly wins: number
  readonly losses: number
  readonly draws: number
}

/** One player's final standing in a tournament. */
export interface TournamentStanding {
  readonly placement: number
  readonly playerName: string
  readonly userId: string | null
  readonly isAi: boolean
  readonly wins: number
  readonly losses: number
  readonly draws: number
}

/** One seat within a single tournament game. */
export interface TournamentGamePlayer {
  readonly name: string
  readonly userId: string | null
  readonly isAi: boolean
  readonly won: boolean
}

/** One game played within a tournament. */
export interface TournamentGame {
  readonly gameId: string
  readonly endedAt: string
  readonly hasReplay: boolean
  readonly players: TournamentGamePlayer[]
}

/** Full public detail for one tournament: standings + every game played (all players'). */
export interface TournamentDetail {
  readonly id: number
  readonly name: string | null
  readonly format: string | null
  readonly gameMode: string | null
  readonly setCodes: string | null
  readonly playerCount: number
  readonly winnerName: string | null
  readonly endedAt: string
  readonly status: TournamentStatus
  readonly standings: TournamentStanding[]
  readonly games: TournamentGame[]
}

/** One card line within a stored deck. */
export interface DeckCardEntry {
  readonly cardName: string
  readonly copies: number
}

/**
 * One card line enriched with the registry metadata the deck viewer needs to render a deck the
 * polished way (group by type, mana curve, colour pips). Structurally satisfies `DeckStatsCard`
 * (see `DeckSummary`) so `computeDeckStats` consumes it directly.
 */
export interface GameDeckCard {
  readonly cardName: string
  readonly copies: number
  /** Converted mana cost. */
  readonly cmc: number
  /** Card type enum names, e.g. `["CREATURE"]`, `["LAND"]`. */
  readonly cardTypes: string[]
  /** The card's own colours as enum names, e.g. `["WHITE","BLUE"]`; empty = colourless. */
  readonly colors: string[]
  /**
   * Direct CDN art URL for the card's default printing, resolved server-side (like the deckbuilder
   * catalog). `null` when unresolved, in which case the preview falls back to a Scryfall name lookup.
   */
  readonly imageUri: string | null
}

/** One seat's recorded deck within a finished game. */
export interface GameDeckParticipant {
  readonly playerName: string
  readonly isAi: boolean
  readonly isSelf: boolean
  readonly won: boolean
  readonly colors: string
  readonly cards: GameDeckCard[]
}

/** Both seats' decks for a finished game, for the recent-games deck viewer. */
export interface GameDecks {
  readonly gameId: string
  readonly endedAt: string
  readonly gameMode: string | null
  readonly participants: GameDeckParticipant[]
  /** The lobby's deck format; only the admin by-match view sends it. */
  readonly format?: string | null
  /** Whether a replay is stored; only the admin by-match view sends it. */
  readonly hasReplay?: boolean | null
}

/** A page of game history plus the total game count (for the pager). */
export interface HistoryPage {
  readonly entries: GameHistoryEntry[]
  readonly total: number
}

async function getStats<T>(path: string): Promise<T> {
  const res = await fetch(`/api/stats${path}`, { headers: authHeaders() })
  if (res.status === 401) throw new UnauthorizedError()
  if (!res.ok) throw new Error(`Failed to load stats (${res.status})`)
  return (await res.json()) as T
}

/** The three ranked queues, mirroring the server's RankedMode. */
export type RankedModeName = 'LIMITED' | 'CONSTRUCTED' | 'COMMANDER'

/** Current ELO standing in one ranked queue. */
export interface RatingEntry {
  readonly mode: RankedModeName
  readonly rating: number
  /** Display tier derived from the rating (or "Provisional" during placement). */
  readonly tier: string
  readonly provisional: boolean
  readonly gamesPlayed: number
  readonly wins: number
  readonly losses: number
  readonly draws: number
  readonly peakRating: number
}

/** One point on the rating-over-time chart. */
export interface RatingPoint {
  readonly mode: RankedModeName
  readonly endedAt: string
  readonly ratingAfter: number
  readonly delta: number
  readonly result: string
}

export const fetchStats = () => getStats<AccountStats>('/me')
export const fetchColorStats = () => getStats<StatBucket[]>('/me/colors')
export const fetchSetStats = () => getStats<StatBucket[]>('/me/sets')
export const fetchModeStats = () => getStats<StatBucket[]>('/me/modes')
export const fetchOpponents = () => getStats<HeadToHead[]>('/me/opponents')
export const fetchHistory = (limit = 25) => getStats<GameHistoryEntry[]>(`/me/history?limit=${limit}`)
export const fetchTopCards = (limit = 30) => getStats<CardStat[]>(`/me/cards?limit=${limit}`)
export const fetchTournamentHistory = (limit = 25) =>
  getStats<UserTournamentEntry[]>(`/me/tournaments?limit=${limit}`)
export const fetchRatings = () => getStats<RatingEntry[]>('/me/ratings')
export const fetchRatingsHistory = () => getStats<RatingPoint[]>('/me/ratings/history')
export const fetchCreatureTypes = (limit = 15) =>
  getStats<StatBucket[]>(`/me/creature-types?limit=${limit}`)
export const fetchCardTypes = () => getStats<StatBucket[]>('/me/card-types')
export const fetchManaCurve = () => getStats<StatBucket[]>('/me/curve')

/** Everything a read-only public profile shows for another player, bundled into one response. */
export interface PublicProfile {
  readonly userId: string
  readonly displayName: string
  /** Preset avatar id, or null for the initial. */
  readonly avatar?: string | null
  readonly stats: AccountStats
  readonly ratings: RatingEntry[]
  readonly ratingHistory: RatingPoint[]
  readonly colors: StatBucket[]
  readonly cardTypes: StatBucket[]
  readonly curve: StatBucket[]
  readonly creatureTypes: StatBucket[]
  readonly modes: StatBucket[]
  readonly sets: StatBucket[]
  readonly topCards: CardStat[]
  readonly opponents: HeadToHead[]
  readonly tournaments: UserTournamentEntry[]
  readonly recentGames: GameHistoryEntry[]
}

/** Thrown when a requested public profile doesn't exist. */
export class ProfileNotFoundError extends Error {}

/** Fetch another player's public, read-only profile by user id. */
export async function fetchPublicProfile(userId: string): Promise<PublicProfile> {
  const res = await fetch(`/api/stats/users/${encodeURIComponent(userId)}`, { headers: authHeaders() })
  if (res.status === 404) throw new ProfileNotFoundError()
  if (!res.ok) throw new Error(`Failed to load profile (${res.status})`)
  return (await res.json()) as PublicProfile
}

/** One page of recent games, with the total count read from `X-Total-Count` for the pager. */
export async function fetchHistoryPage(limit: number, offset: number): Promise<HistoryPage> {
  const res = await fetch(`/api/stats/me/history?limit=${limit}&offset=${offset}`, {
    headers: authHeaders(),
  })
  if (res.status === 401) throw new UnauthorizedError()
  if (!res.ok) throw new Error(`Failed to load history (${res.status})`)
  const entries = (await res.json()) as GameHistoryEntry[]
  const total = Number(res.headers.get('X-Total-Count') ?? entries.length)
  return { entries, total: Number.isFinite(total) ? total : entries.length }
}

/** Full public detail for one tournament (standings + all games). */
export async function fetchTournamentDetail(id: number): Promise<TournamentDetail> {
  const res = await fetch(`/api/stats/tournaments/${id}`, { headers: authHeaders() })
  if (res.status === 404) throw new ProfileNotFoundError()
  if (!res.ok) throw new Error(`Failed to load tournament (${res.status})`)
  return (await res.json()) as TournamentDetail
}

/** One card that changed hands in a game played for ante (CR 407). */
export interface Spoil {
  readonly cardName: string
  readonly imageUri: string | null
  /** The other side of the transfer: who it was won from, or lost to. */
  readonly opponentName: string
  readonly gameId: string | null
  /** ISO-8601 timestamp of the game's end. */
  readonly at: string
}

export interface Spoils {
  readonly won: readonly Spoil[]
  readonly lost: readonly Spoil[]
}

const NO_SPOILS: Spoils = { won: [], lost: [] }

/**
 * The signed-in user's ante winnings and losses. A server without the endpoint (404) or with
 * accounts disabled reads as "no spoils yet" rather than an error — the panel just shows its
 * empty state.
 */
export async function fetchSpoils(): Promise<Spoils> {
  const res = await fetch('/api/stats/me/spoils', { headers: authHeaders() })
  if (res.status === 401) throw new UnauthorizedError()
  if (res.status === 404 || res.status === 503) return NO_SPOILS
  if (!res.ok) throw new Error(`Failed to load spoils (${res.status})`)
  const body = (await res.json().catch(() => null)) as Partial<Spoils> | null
  return {
    won: Array.isArray(body?.won) ? body.won : [],
    lost: Array.isArray(body?.lost) ? body.lost : [],
  }
}

/** Both seats' decklists for one of the user's finished games (for the recent-games deck viewer). */
export async function fetchGameDecks(gameId: string): Promise<GameDecks> {
  const res = await fetch(`/api/stats/me/history/${encodeURIComponent(gameId)}/decks`, {
    headers: authHeaders(),
  })
  if (res.status === 401) throw new UnauthorizedError()
  if (!res.ok) throw new Error(`Failed to load decks (${res.status})`)
  return (await res.json()) as GameDecks
}
