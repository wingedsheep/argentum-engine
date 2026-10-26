import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { ClientGameState, DecisionResponse, PendingDecision, StateUpdateMessage } from '@/types'
import type { GameWebSocket } from '@/network/websocket'

vi.stubGlobal('localStorage', { getItem: () => null, setItem: () => {}, removeItem: () => {} })
vi.stubGlobal('window', { addEventListener: () => {}, removeEventListener: () => {} })

const { useGameStore } = await import('../gameStore')
const { createGameplayHandlers } = await import('./handlers/gameplayHandlers')
const { setWebSocket } = await import('./shared')
const { AUTO_ANSWER_DELAY_MS, cancelAutoAnswer, describeAutoAnswer } = await import('./handlers/autoAnswer')
const { usePreferences } = await import('@/store/preferencesStore')
const { entityId, Phase, Step } = await import('@/types')

const ME = entityId('me')
const KAVU = entityId('kavu')
const EPOCH = 'epoch'
const send = vi.fn()
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
})

afterEach(() => {
  cancelAutoAnswer()
  vi.useRealTimers()
})

describe('forced questions', () => {
  it('never shows the question, and answers with the server-sent answer after the delay', () => {
    receive(reveal, forced)
    expect(useGameStore.getState().pendingDecision).toBeNull()

    vi.advanceTimersByTime(AUTO_ANSWER_DELAY_MS - 1)
    expect(send).not.toHaveBeenCalled()
    vi.advanceTimersByTime(1)
    expect(send).toHaveBeenCalledTimes(1)
    const sent = send.mock.calls[0]![0] as { action: { type: string; playerId: string; response: DecisionResponse } }
    expect(sent.action).toEqual({ type: 'SubmitDecision', playerId: ME, response: forced })
    expect(useGameStore.getState().autoAnswerNotice?.sourceName).toBe('Dragonlord Spire')
  })

  it('keeps the original clock when the same question is re-sent', () => {
    receive(reveal, forced)
    vi.advanceTimersByTime(AUTO_ANSWER_DELAY_MS / 2)
    receive(reveal, forced)
    vi.advanceTimersByTime(AUTO_ANSWER_DELAY_MS / 2)
    expect(send).toHaveBeenCalledTimes(1)
  })

  it('drops the answer when the question goes away before the delay is up', () => {
    receive(reveal, forced)
    receive()
    vi.advanceTimersByTime(AUTO_ANSWER_DELAY_MS * 2)
    expect(send).not.toHaveBeenCalled()
  })

  it('asks as usual when the player turned it off, or when the answer is for another question', () => {
    setAutoAnswer(false)
    receive(reveal, forced)
    expect(useGameStore.getState().pendingDecision).toEqual(reveal)

    setAutoAnswer(true)
    receive({ ...reveal, id: `${EPOCH}:other` }, forced)
    expect(useGameStore.getState().pendingDecision?.id).toBe(`${EPOCH}:other`)
    vi.advanceTimersByTime(AUTO_ANSWER_DELAY_MS * 2)
    expect(send).not.toHaveBeenCalled()
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
