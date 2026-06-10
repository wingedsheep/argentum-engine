/**
 * Typed wrappers for the AI-assistance REST endpoints (draft "Suggest Pick" and deckbuild
 * "Auto-build"). The server holds the pluggable engines; the client sends card names and gets back
 * scores / a built decklist. See `game-server` AiAssistController.
 */

export interface AdvisorInfo {
  readonly id: string
  readonly name: string
}

export interface AdvisorsResponse {
  readonly draft: readonly AdvisorInfo[]
  readonly deckbuild: readonly AdvisorInfo[]
}

export interface CardScore {
  readonly cardName: string
  /** 0–100 normalized score; higher is a better pick. */
  readonly score: number
  readonly reason: string
}

export interface DraftPickAdvice {
  readonly advisorId: string
  readonly scores: readonly CardScore[]
  readonly recommended: readonly string[]
}

/** One candidate deck within a {@link DeckBuildResult}. */
export interface DeckBuildOption {
  readonly deckList: Readonly<Record<string, number>>
  readonly score: number | null
  readonly archetype: string | null
  /** Build colors as WUBRG single-letter codes (for color pips); empty if the engine doesn't report them. */
  readonly colors: readonly string[]
}

export interface DeckBuildResult {
  readonly advisorId: string
  /** Candidate decks, ordered best-first. A single-deck engine returns one entry. */
  readonly builds: readonly DeckBuildOption[]
  /** Index into `builds` of the deck to apply by default. */
  readonly recommended: number
}

// The engine list is fixed for the server's lifetime, but both the draft and deckbuild controls
// fetch it on every mount (which happens on each deck edit). Cache the in-flight/resolved promise so
// repeated calls reuse one request instead of re-hitting /api/ai-advisors. A failed load is not
// cached, so a transient error can be retried on the next mount.
let advisorsCache: Promise<AdvisorsResponse> | null = null

/** List the AI engines available for the dropdowns. */
export function fetchAdvisors(): Promise<AdvisorsResponse> {
  if (advisorsCache) return advisorsCache
  const request = (async () => {
    const res = await fetch('/api/ai-advisors')
    if (!res.ok) throw new Error(`Failed to load AI engines (${res.status})`)
    return res.json() as Promise<AdvisorsResponse>
  })()
  advisorsCache = request
  request.catch(() => {
    if (advisorsCache === request) advisorsCache = null
  })
  return request
}

export interface SuggestPickParams {
  readonly lobbyId?: string | null
  readonly advisorId?: string | null
  readonly pack: readonly string[]
  readonly pickedSoFar: readonly string[]
  readonly packNumber: number
  readonly pickNumber: number
  readonly picksRequired: number
  /** Set code(s) for set-specific engines (e.g. Draftsim). */
  readonly setCodes?: readonly string[]
}

/** Score every card in the current pack and recommend the best pick(s). */
export async function suggestPick(params: SuggestPickParams): Promise<DraftPickAdvice> {
  const res = await fetch('/api/draft/suggest-pick', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(params),
  })
  if (res.status === 403) throw new Error('AI assistance is disabled for this tournament')
  if (!res.ok) throw new Error(`Suggest pick failed (${res.status})`)
  return res.json() as Promise<DraftPickAdvice>
}

export interface AutoBuildParams {
  readonly lobbyId?: string | null
  readonly advisorId?: string | null
  /** Pool card names, one entry per physical copy. */
  readonly pool: readonly string[]
  /** Basic land names available to the build. */
  readonly basics: readonly string[]
  /** Cards already in the deck (name → count). Empty = build fresh; non-empty = complete it. */
  readonly lockedDeck: Readonly<Record<string, number>>
  readonly targetSize: number
  /** Set code(s) for set-specific engines (e.g. Draftsim). */
  readonly setCodes?: readonly string[]
}

/** Build (or complete) a deck from the pool. */
export async function autoBuildDeck(params: AutoBuildParams): Promise<DeckBuildResult> {
  const res = await fetch('/api/deckbuild/auto-build', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(params),
  })
  if (res.status === 403) throw new Error('AI assistance is disabled for this tournament')
  if (!res.ok) throw new Error(`Auto-build failed (${res.status})`)
  return res.json() as Promise<DeckBuildResult>
}
