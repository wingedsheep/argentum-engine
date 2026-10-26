/**
 * Answering questions that have exactly one legal answer, for the player (#3340).
 *
 * The engine always asks — whether a question exists must not depend on hidden cards — so some
 * questions have only one answer: "you may reveal a Dragon card" with none in hand, a mandatory
 * trigger whose only legal target is the creature itself. The server says which (it classifies the
 * engine's own decision, with the same rules the AI uses) and sends the answer with the decision.
 * This module is the client's half: it never decides what is legal, it only chooses *whether* to
 * use the answer it was given (the player's preference) and *when* to send it.
 */
import type { ClientGameState, DecisionResponse, EntityId, PendingDecision } from '@/types'
import { createSubmitActionMessage } from '@/types'
import { decisionInteractionEpoch } from '@/network/liveAction'
import { getWebSocket } from '../shared'
import type { GetState, SetState } from './types'

/**
 * How long a forced question waits before it is answered.
 *
 * The opponent still sees "deciding…" while it waits, and an instant answer would make forced
 * questions stand out from real ones by their length alone. One second is about as fast as a
 * person clicks the only button on a dialog that has just appeared, so the forced answer sits at
 * the quick end of real answers rather than off on its own. It is fixed rather than tuned per
 * question: answer timing in general is out of scope for #3340.
 */
export const AUTO_ANSWER_DELAY_MS = 1000

/** A forced question this client is going to answer, and the answer the server sent for it. */
export interface AutoAnswer {
  readonly decision: PendingDecision
  readonly response: DecisionResponse
}

/** The short "answered for you" notice shown to the decider. */
export interface AutoAnswerNotice {
  readonly key: number
  readonly sourceName: string | null
  readonly text: string
}

/**
 * The forced question in this update this client should answer itself, or null to show it as usual.
 * Only the server's answer for this very decision is used; a mismatched id means a stale pairing.
 */
export function autoAnswerFor(
  pendingDecision: PendingDecision | null | undefined,
  response: DecisionResponse | null | undefined,
  enabled: boolean
): AutoAnswer | null {
  if (!enabled || !pendingDecision || !response) return null
  if (response.decisionId !== pendingDecision.id) return null
  return { decision: pendingDecision, response }
}

let scheduled: { decisionId: string; handle: ReturnType<typeof setTimeout> } | null = null

/** Drop a scheduled answer — the question went away, or a real one replaced it. */
export function cancelAutoAnswer(): void {
  if (scheduled) clearTimeout(scheduled.handle)
  scheduled = null
}

/**
 * Answer [auto] after [AUTO_ANSWER_DELAY_MS], or cancel the pending answer when this update has
 * none. The same question arriving again (a resync) keeps its original clock.
 */
export function scheduleAutoAnswer(auto: AutoAnswer | null, set: SetState, get: GetState): void {
  if (!auto) {
    cancelAutoAnswer()
    return
  }
  if (scheduled?.decisionId === auto.decision.id) return
  cancelAutoAnswer()
  const decisionId = auto.decision.id
  const handle = setTimeout(() => {
    if (scheduled?.decisionId !== decisionId) return
    scheduled = null
    const epoch = decisionInteractionEpoch(auto.response.decisionId)
    const { sessionId, gameState, interactionEpoch } = get()
    if (!sessionId || !epoch || epoch !== interactionEpoch) return
    getWebSocket()?.send(
      createSubmitActionMessage(
        // The decision's owner, not the connection's seat: equal in normal play, different in
        // hotseat / Mindslaver control, and the engine requires the decision's own player.
        { type: 'SubmitDecision', playerId: auto.decision.playerId, response: auto.response },
        epoch
      )
    )
    set({
      autoAnswerNotice: {
        key: Date.now(),
        sourceName: auto.decision.context.sourceName ?? null,
        text: describeAutoAnswer(auto.decision, auto.response, gameState),
      },
    })
  }, AUTO_ANSWER_DELAY_MS)
  scheduled = { decisionId, handle }
}

/** What was answered, in a few words: "Fail to find", "Targeted Flametongue Kavu", "Chose blue". */
export function describeAutoAnswer(
  decision: PendingDecision,
  response: DecisionResponse,
  gameState: ClientGameState | null
): string {
  const raw = response as unknown as Record<string, unknown>
  const nameOf = (id: EntityId): string => {
    const hidden =
      decision.type === 'SelectCardsDecision'
        ? decision.cardInfo?.[id]?.name
        : decision.type === 'SearchLibraryDecision'
          ? decision.cards[id]?.name
          : undefined
    return (
      hidden ??
      gameState?.cards[id]?.name ??
      gameState?.players.find((p) => p.playerId === id)?.name ??
      'the only choice'
    )
  }
  switch (decision.type) {
    case 'SelectCardsDecision':
    case 'SearchLibraryDecision': {
      const picked = (raw.selectedCards as readonly EntityId[] | undefined) ?? []
      if (picked.length === 0) {
        const nothing =
          decision.type === 'SearchLibraryDecision' || decision.librarySearch
            ? 'Fail to find'
            : decision.type === 'SelectCardsDecision' && decision.declineLabel
              ? decision.declineLabel
              : 'Nothing to choose'
        return `${decision.prompt} — ${nothing}`
      }
      return `Chose ${picked.map(nameOf).join(', ')}`
    }
    case 'ChooseTargetsDecision': {
      const targets = Object.values((raw.selectedTargets as Record<string, readonly EntityId[]>) ?? {}).flat()
      return targets.length > 0 ? `Targeted ${targets.map(nameOf).join(', ')}` : decision.prompt
    }
    case 'YesNoDecision':
      return `${decision.prompt} — ${raw.choice === true ? decision.yesText : decision.noText}`
    case 'ChooseOptionDecision': {
      const index = raw.optionIndex as number | undefined
      return `Chose ${index != null ? decision.options[index] ?? 'the only option' : 'the only option'}`
    }
    case 'ChooseColorDecision':
      return `Chose ${String(raw.color ?? 'the only color').toLowerCase()}`
    case 'ChooseNumberDecision':
      return `Chose ${String(raw.number ?? decision.minValue)}`
    default:
      return decision.prompt
  }
}
