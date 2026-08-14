/**
 * Local testing mode: read what the engine AI considered on each decision, hold it before it moves
 * so you can play a different option than the one it picked, and export a position together with
 * those ratings as AI-training input.
 *
 * Served by the game server's `AiInsightController`, which is mounted only when
 * `game.ai.insight-enabled` is set. On any other server these routes 404 — which is the signal the
 * UI uses to hide itself, so nothing here needs a separate feature-flag fetch.
 */

export type AiDecisionKind = 'PRIORITY' | 'DECLARE_ATTACKERS' | 'DECLARE_BLOCKERS'

/** One option the AI weighed. Scores are raw evaluator units — only relative values mean anything. */
export interface AiActionOption {
  readonly label: string
  readonly actionType: string
  readonly cardName?: string | null
  readonly targets: readonly string[]
  /** Null when the option was dropped before it was ever scored. */
  readonly score: number | null
  /** The board score before per-card timing/advisor adjustment; null when nothing adjusted it. */
  readonly rawScore: number | null
  /** `score` minus the baseline. Positive means the AI preferred this to doing nothing. */
  readonly advantage: number | null
  readonly chosen: boolean
  /** The "pass priority" / "no attacks" row — the waterline every other option had to clear. */
  readonly baseline: boolean
  readonly note?: string | null
  /**
   * Present when this option can actually be submitted. Its shape is engine-internal — the client
   * never reads it, it just means "this row is playable", and plays it back by index.
   */
  readonly action?: unknown
}

export interface AiDecisionInsight {
  readonly kind: AiDecisionKind
  readonly playerId: string
  readonly turnNumber: number
  readonly step: string
  readonly activePlayerId: string | null
  readonly onOwnTurn: boolean
  readonly baselineLabel: string
  readonly baselineScore: number
  readonly chosenLabel: string
  readonly thinkTimeMs: number
  /** Scored options best-first, then the ones dropped before scoring. */
  readonly options: readonly AiActionOption[]
}

/** A move a human substituted for the AI's pick. */
export interface AiHumanOverride {
  readonly optionIndex: number
  readonly label: string
}

export interface AiInsightDecision {
  readonly id: number
  readonly recordedAt: string
  readonly insight: AiDecisionInsight
  readonly humanOverride?: AiHumanOverride | null
}

/** The AI is held at this decision, waiting for the human to approve or replace its move. */
export interface AiPendingApproval {
  readonly decisionId: number
  readonly proposedLabel: string
}

export interface AiInsightListResponse {
  readonly gameSessionId: string | null
  /** Newest first. */
  readonly decisions: readonly AiInsightDecision[]
  readonly stepMode: boolean
  readonly pending?: AiPendingApproval | null
}

/** The route isn't mounted: this server doesn't run the local testing mode, and never will. */
export const AI_INSIGHT_UNAVAILABLE = Symbol('ai-insight-unavailable')

/** A request that failed for some other reason — retryable, and says nothing about the server. */
export const AI_INSIGHT_ERROR = Symbol('ai-insight-error')

/**
 * Fetch the AI's recent decisions for the game `playerId` is seated in.
 *
 * The two failure symbols are deliberately distinct: {@link AI_INSIGHT_UNAVAILABLE} (a 404) means
 * the caller should hide the panel and stop asking, while {@link AI_INSIGHT_ERROR} is a blip worth
 * retrying. Collapsing them would let one dropped request hide the panel for the rest of the game.
 */
export async function fetchAiInsight(
  playerId: string,
  limit = 60,
): Promise<AiInsightListResponse | typeof AI_INSIGHT_UNAVAILABLE | typeof AI_INSIGHT_ERROR> {
  let res: Response
  try {
    res = await fetch(`/api/dev/ai-insight/${encodeURIComponent(playerId)}?limit=${limit}`)
  } catch {
    return AI_INSIGHT_ERROR
  }
  if (res.status === 404) return AI_INSIGHT_UNAVAILABLE
  if (!res.ok) return AI_INSIGHT_ERROR
  return (await res.json()) as AiInsightListResponse
}

/** Hold the AI before each move it actually weighed, or let it run again. */
export async function setAiStepMode(playerId: string, enabled: boolean): Promise<void> {
  const res = await fetch(
    `/api/dev/ai-insight/${encodeURIComponent(playerId)}/step?enabled=${enabled}`,
    { method: 'POST' },
  )
  // Surfaced rather than swallowed: a toggle that silently fails leaves the button showing one
  // thing and the server doing another, which is the worst possible state for a debugging tool.
  if (!res.ok) throw new Error(`Could not change step mode (${res.status})`)
}

/**
 * Release a held AI. Without `optionIndex` it plays its own pick; with one, that option is submitted
 * instead — which is how you ask "what would have happened if it took the second-best line?".
 */
export async function resumeAi(playerId: string, optionIndex?: number): Promise<void> {
  const query = optionIndex === undefined ? '' : `?optionIndex=${optionIndex}`
  const res = await fetch(
    `/api/dev/ai-insight/${encodeURIComponent(playerId)}/resume${query}`,
    { method: 'POST' },
  )
  if (!res.ok) throw new Error(`The AI could not play that option (${res.status})`)
}

/** Download one decision's board state plus every rating the AI gave it. */
export async function downloadAiInsightExport(playerId: string, decisionId?: number): Promise<void> {
  const query = decisionId === undefined ? '' : `?decision=${decisionId}`
  const res = await fetch(`/api/dev/ai-insight/${encodeURIComponent(playerId)}/export${query}`)
  if (!res.ok) throw new Error(`Export failed (${res.status})`)
  const text = await res.text()

  const blob = new Blob([text], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `ai-insight-decision-${decisionId ?? 'latest'}.json`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  // Revoking synchronously cancels the download in some browsers — see DeckBuilderOverlay.
  setTimeout(() => URL.revokeObjectURL(url), 0)
}

/** Drop the recorded history for this game, so a fresh sequence is easier to read. */
export async function clearAiInsight(playerId: string): Promise<void> {
  await fetch(`/api/dev/ai-insight/${encodeURIComponent(playerId)}`, { method: 'DELETE' })
}
