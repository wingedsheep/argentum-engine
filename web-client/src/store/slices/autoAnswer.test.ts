import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { ClientGameState, DecisionResponse, PendingDecision, StateUpdateMessage } from '@/types'
import type { GameWebSocket } from '@/network/websocket'

vi.stubGlobal('localStorage', { getItem: () => null, setItem: () => {}, removeItem: () => {} })
vi.stubGlobal('window', { addEventListener: () => {}, removeEventListener: () => {} })

const { useGameStore } = await import('../gameStore')
const { createGameplayHandlers } = await import('./handlers/gameplayHandlers')
const { setWebSocket } = await import('./shared')
const {
  AUTO_ANSWER_MAX_DELAY_MS,
  AUTO_ANSWER_MIN_DELAY_MS,
  cancelAutoAnswer,
  describeAutoAnswer,
  humanAnswerDelayMs,
  setAutoAnswerDelayForTests,
} = await import('./handlers/autoAnswer')
const { usePreferences } = await import('@/store/preferencesStore')
const { entityId, Phase, Step } = await import('@/types')

const ME = entityId('me')
const KAVU = entityId('kavu')
const EPOCH = 'epoch'
const send = vi.fn()
/** The delays the next questions draw, in order; the scheduler takes one per new question. */
let delays: number[] = []
let restoreDelay: () => void = () => {}
const handlers = createGameplayHandlers(useGameStore.setState, useGameStore.getState)

function setAutoAnswer(autoAnswerForced: boolean): void {
  const prefs = usePreferences.getState().prefs
  usePreferences.getState().replace({ ...prefs, gameplay: { ...prefs.gameplay, autoAnswerForced } })
}

const position: ClientGameState = {
  viewingPlayerId: ME,
  cards: {},
  zones: [],
  players: [],
  currentPhase: Phase.PRECOMBAT_MAIN,
  currentStep: Step.PRECOMBAT_MAIN,
  activePlayerId: ME,
  priorityPlayerId: ME,
  turnNumber: 1,
  isGameOver: false,
  winnerId: null,
  combat: null,
}

const reveal: PendingDecision = {
  type: 'SelectCardsDecision',
  id: `${EPOCH}:reveal`,
  playerId: ME,
  prompt: 'You may reveal a Dragon card from your hand',
  context: { phase: 'RESOLUTION', sourceName: 'Dragonlord Spire' },
  options: [],
  minSelections: 0,
  maxSelections: 1,
  ordered: false,
}
const forced = { type: 'CardsSelectedResponse', decisionId: reveal.id, selectedCards: [] } as DecisionResponse

function receive(pendingDecision?: PendingDecision, autoAnswer?: DecisionResponse): void {
  const message: StateUpdateMessage = {
    type: 'stateUpdate', state: position, events: [], legalActions: [], interactionEpoch: EPOCH,
    ...(pendingDecision ? { pendingDecision } : {}),
    ...(autoAnswer ? { autoAnswer } : {}),
  }
  handlers.onStateUpdate(message)
}

beforeEach(() => {
  vi.useFakeTimers()
  send.mockClear()
  setWebSocket({ send, onStateVersionReceived: vi.fn(), requestResync: vi.fn() } as unknown as GameWebSocket)
  useGameStore.setState({ sessionId: 'auto-answer-test', playerId: ME, interactionEpoch: null, autoAnswerNotice: null })
  setAutoAnswer(true)
  delays = [1400, 2100]
  restoreDelay = setAutoAnswerDelayForTests(() => delays.shift() ?? 1000)
})

afterEach(() => {
  cancelAutoAnswer()
  restoreDelay()
  vi.useRealTimers()
})

describe('forced questions', () => {
  it('never shows the question, and answers with the server-sent answer after the delay', () => {
    receive(reveal, forced)
    expect(useGameStore.getState().pendingDecision).toBeNull()

    vi.advanceTimersByTime(1400 - 1)
    expect(send).not.toHaveBeenCalled()
    vi.advanceTimersByTime(1)
    expect(send).toHaveBeenCalledTimes(1)
    const sent = send.mock.calls[0]![0] as { action: { type: string; playerId: string; response: DecisionResponse } }
    expect(sent.action).toEqual({ type: 'SubmitDecision', playerId: ME, response: forced })
    expect(useGameStore.getState().autoAnswerNotice?.sourceName).toBe('Dragonlord Spire')
  })

  it('keeps the original clock when the same question is re-sent', () => {
    receive(reveal, forced)
    vi.advanceTimersByTime(700)
    receive(reveal, forced)
    vi.advanceTimersByTime(700)
    expect(send).toHaveBeenCalledTimes(1)
  })

  it('draws a fresh delay for each new question', () => {
    receive(reveal, forced)
    vi.advanceTimersByTime(1400)
    expect(send).toHaveBeenCalledTimes(1)

    const next = { ...reveal, id: `${EPOCH}:next` }
    receive(next, { ...forced, decisionId: next.id } as DecisionResponse)
    vi.advanceTimersByTime(2100 - 1)
    expect(send).toHaveBeenCalledTimes(1)
    vi.advanceTimersByTime(1)
    expect(send).toHaveBeenCalledTimes(2)
  })

  it('drops the answer when the question goes away before the delay is up', () => {
    receive(reveal, forced)
    receive()
    vi.advanceTimersByTime(AUTO_ANSWER_MAX_DELAY_MS * 2)
    expect(send).not.toHaveBeenCalled()
  })

  it('asks as usual when the player turned it off, or when the answer is for another question', () => {
    setAutoAnswer(false)
    receive(reveal, forced)
    expect(useGameStore.getState().pendingDecision).toEqual(reveal)

    setAutoAnswer(true)
    receive({ ...reveal, id: `${EPOCH}:other` }, forced)
    expect(useGameStore.getState().pendingDecision?.id).toBe(`${EPOCH}:other`)
    vi.advanceTimersByTime(AUTO_ANSWER_MAX_DELAY_MS * 2)
    expect(send).not.toHaveBeenCalled()
  })
})

describe('humanAnswerDelayMs', () => {
  /** mulberry32: a small seeded generator, so the sample is the same on every run. */
  function seeded(seed: number): () => number {
    let a = seed
    return () => {
      a = (a + 0x6d2b79f5) | 0
      let t = Math.imul(a ^ (a >>> 15), 1 | a)
      t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t
      return ((t ^ (t >>> 14)) >>> 0) / 4294967296
    }
  }

  it('stays in range, skews short, and varies', () => {
    const random = seeded(3340)
    const sample = Array.from({ length: 10_000 }, () => humanAnswerDelayMs(random)).sort((a, b) => a - b)
    const at = (q: number) => sample[Math.floor(q * (sample.length - 1))]!
    expect(sample[0]).toBeGreaterThanOrEqual(AUTO_ANSWER_MIN_DELAY_MS)
    expect(sample[sample.length - 1]).toBeLessThanOrEqual(AUTO_ANSWER_MAX_DELAY_MS)
    // Median near 1.2 s; the long tail pulls the mean above it.
    expect(at(0.5)).toBeGreaterThan(1100)
    expect(at(0.5)).toBeLessThan(1300)
    const mean = sample.reduce((sum, x) => sum + x, 0) / sample.length
    expect(mean).toBeGreaterThan(at(0.5))
    // Most answers are quick, a few are slow, and no single value dominates.
    expect(at(0.9)).toBeLessThan(1800)
    expect(at(0.99)).toBeGreaterThan(2000)
    expect(new Set(sample).size).toBeGreaterThan(500)
  })

  it('handles the extremes of the uniform source', () => {
    expect(humanAnswerDelayMs(() => 0)).toBe(AUTO_ANSWER_MIN_DELAY_MS + 400)
    expect(humanAnswerDelayMs(() => 0.999999999)).toBeLessThanOrEqual(AUTO_ANSWER_MAX_DELAY_MS)
  })
})

describe('describeAutoAnswer', () => {
  it('says what was answered', () => {
    expect(describeAutoAnswer(reveal, { decisionId: reveal.id, type: 'CardsSelectedResponse' }, position))
      .toBe('You may reveal a Dragon card from your hand — Nothing to choose')
    expect(
      describeAutoAnswer({ ...reveal, librarySearch: true, prompt: 'Search for a basic land' }, forced, position)
    ).toBe('Search for a basic land — Fail to find')

    const target: PendingDecision = {
      type: 'ChooseTargetsDecision',
      id: `${EPOCH}:target`,
      playerId: ME,
      prompt: 'Choose a target',
      context: { phase: 'TRIGGER' },
      targetRequirements: [],
      legalTargets: { 0: [KAVU] },
    }
    const withKavu = { ...position, cards: { [KAVU]: { name: 'Flametongue Kavu' } } } as unknown as ClientGameState
    const response = { type: 'TargetsResponse', decisionId: target.id, selectedTargets: { 0: [KAVU] } } as DecisionResponse
    expect(describeAutoAnswer(target, response, withKavu)).toBe('Targeted Flametongue Kavu')
  })
})
