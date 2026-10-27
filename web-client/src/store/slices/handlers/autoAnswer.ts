/**
 * Answering questions that have exactly one legal answer, for the player (#3340).
 *
 * The engine always asks — whether a question exists must not depend on hidden cards — so some
 * questions have only one answer: "you may reveal a Dragon card" with none in hand, a mandatory
 * trigger whose only legal target is the creature itself. The server says which (it classifies the
 * engine's own decision, with the same rules the AI uses) and sends the answer with the decision.
 * This module is the client's half: it never decides what is legal, it only chooses *whether* to
 * use the answer it was given (the player's preference) and *when* to send it.
 *
 * When: at once in the "instant" mode, and after [humanAnswerDelayMs] in the "delayed" one — except
 * that a question the server marks public (`autoAnswerPublic`: everyone can see it has one answer,
 * like Flametongue Kavu's trigger with no other creature in play) is answered at once in both, since
 * a pause there hides nothing.
 */
import type { ClientGameState, DecisionResponse, EntityId, PendingDecision } from '@/types'
import type { ForcedChoiceMode } from '@/store/preferencesStore'
import { createSubmitActionMessage } from '@/types'
import { decisionInteractionEpoch } from '@/network/liveAction'
import { getWebSocket } from '../shared'
import type { GetState, SetState } from './types'

/**
 * How long a forced question waits before it is answered: a fresh draw per question, shaped like
 * the time a person takes to click the only button on a dialog that has just appeared.
 *
 * The opponent still sees "deciding…" while it waits. An instant answer would make forced questions
 * stand out from real ones by their length alone, and so would any fixed length — the same second
 * every time is a fingerprint of its own. So the wait is 800 ms of reading and reaching for the
 * button plus a log-normal tail (median 400 ms more, σ = 0.6): most answers land between about
 * 1.0 and 1.6 s, a few take two seconds or more, and none passes [AUTO_ANSWER_MAX_DELAY_MS].
 */
export const AUTO_ANSWER_MIN_DELAY_MS = 800
export const AUTO_ANSWER_MAX_DELAY_MS = 2500
const TAIL_MEDIAN_MS = 400
const TAIL_SIGMA = 0.6

/** One human-like answer delay, in ms, drawn with [random] (uniform on [0, 1), like Math.random). */
export function humanAnswerDelayMs(random: () => number = Math.random): number {
  // Box–Muller: a standard normal from two uniforms; 1 - u keeps the log's argument above zero.
  const z = Math.sqrt(-2 * Math.log(1 - random())) * Math.cos(2 * Math.PI * random())
  const delay = AUTO_ANSWER_MIN_DELAY_MS + TAIL_MEDIAN_MS * Math.exp(TAIL_SIGMA * z)
  return Math.round(Math.min(AUTO_ANSWER_MAX_DELAY_MS, delay))
}

let drawDelay: () => number = () => humanAnswerDelayMs()

/** Tests: replace the delay draw with a deterministic one; returns the function that restores it. */
export function setAutoAnswerDelayForTests(draw: () => number): () => void {
  const previous = drawDelay
  drawDelay = draw
  return () => {
    drawDelay = previous
  }
}

/** A forced question this client is going to answer, and the answer the server sent for it. */
export interface AutoAnswer {
  readonly decision: PendingDecision
  readonly response: DecisionResponse
  /** Answer without the human-like pause: the player chose "instant", or the forcedness is public. */
  readonly immediate: boolean
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
  mode: ForcedChoiceMode,
  isPublic: boolean = false
): AutoAnswer | null {
  if (mode === 'ask' || !pendingDecision || !response) return null
  if (response.decisionId !== pendingDecision.id) return null
  return { decision: pendingDecision, response, immediate: mode === 'instant' || isPublic }
}

let scheduled: { decisionId: string; handle: ReturnType<typeof setTimeout> } | null = null

/** Drop a scheduled answer — the question went away, or a real one replaced it. */
export function cancelAutoAnswer(): void {
  if (scheduled) clearTimeout(scheduled.handle)
  scheduled = null
}

/**
 * Answer [auto] after a delay drawn for this question (none when it is [AutoAnswer.immediate]), or
 * cancel the pending answer when this update has none. The same question arriving again (a resync)
 * keeps its original clock.
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
  }, auto.immediate ? 0 : drawDelay())
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
