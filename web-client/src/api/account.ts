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
  const body = (await res.json().catch(() => null)) as { error?: string } | null
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
 * sign-in form would only produce a confusing error. Fails closed (accounts hidden) on any error.
 */
export async function fetchAppConfig(): Promise<AppConfig> {
  try {
    const res = await fetch('/api/config')
    if (!res.ok) return { accountsEnabled: false }
    return (await res.json()) as AppConfig
  } catch {
    return { accountsEnabled: false }
  }
}

// ----- Auth -----

/** Request a magic-link sign-in email. Resolves on success regardless of whether the email exists. */
export async function requestLogin(email: string): Promise<void> {
  const res = await fetch('/api/auth/request-login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email }),
  })
  if (!res.ok) throw new Error(await errorMessage(res, `Sign-in request failed (${res.status})`))
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

/** Fetch the current account, or null if not signed in / accounts disabled. */
export async function fetchMe(): Promise<AccountUser | null> {
  if (!getAuthToken()) return null
  const res = await fetch('/api/auth/me', { headers: authHeaders() })
  if (res.status === 401) return null
  if (!res.ok) return null
  return (await res.json()) as AccountUser
}

// ----- Saved decks -----

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

export interface GameHistoryEntry {
  readonly endedAt: string
  readonly gameMode: string | null
  readonly format: string | null
  readonly colors: string | null
  readonly opponents: string | null
  readonly won: boolean
}

export interface CardStat {
  readonly cardName: string
  readonly copies: number
  readonly decks: number
}

export interface UserTournamentEntry {
  readonly endedAt: string
  readonly name: string | null
  readonly format: string | null
  readonly gameMode: string | null
  readonly placement: number
  readonly playerCount: number
}

async function getStats<T>(path: string): Promise<T> {
  const res = await fetch(`/api/stats${path}`, { headers: authHeaders() })
  if (res.status === 401) throw new UnauthorizedError()
  if (!res.ok) throw new Error(`Failed to load stats (${res.status})`)
  return (await res.json()) as T
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
