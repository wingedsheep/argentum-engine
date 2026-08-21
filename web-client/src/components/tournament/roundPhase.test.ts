import { describe, it, expect } from 'vitest'
import { deriveRoundPhase, type RoundPhaseInput } from './roundPhase'

const base: RoundPhaseInput = {
  currentRound: 0,
  currentRoundComplete: false,
  lastRoundResults: null,
  currentMatchGameSessionId: null,
  isComplete: false,
}

const phase = (over: Partial<RoundPhaseInput>) => deriveRoundPhase({ ...base, ...over })

describe('deriveRoundPhase', () => {
  it('offers Ready for round 1 before the tournament starts', () => {
    const p = phase({})
    expect(p.isWaitingForReady).toBe(true)
    expect(p.roundStillRunning).toBe(false)
    expect(p.roundNumber).toBe(1)
  })

  it('names the round being played while in a match', () => {
    const p = phase({ currentRound: 2, currentMatchGameSessionId: 'g7', lastRoundResults: [] })
    expect(p.isWaitingForReady).toBe(false)
    expect(p.roundNumber).toBe(2)
  })

  it('keeps naming the current round when we finished early and it is still running', () => {
    // The one-human/seven-AI case: our matchComplete populated lastRoundResults, but three AI-vs-AI
    // tables are still playing round 1. Labelling this "Round 2" was the reported bug.
    const p = phase({ currentRound: 1, lastRoundResults: [{}], currentRoundComplete: false })
    expect(p.isWaitingForReady).toBe(true)
    expect(p.roundStillRunning).toBe(true)
    expect(p.roundNumber).toBe(1)
    expect(p.readyLabel).toBe('Ready for Next Match')
  })

  it('advertises the next round once the round has actually completed', () => {
    const p = phase({ currentRound: 1, lastRoundResults: [{}], currentRoundComplete: true })
    expect(p.isWaitingForReady).toBe(true)
    expect(p.roundStillRunning).toBe(false)
    expect(p.roundNumber).toBe(2)
    expect(p.readyLabel).toBe('Ready for Next Round')
  })

  it('drops out of the waiting state once a new match starts', () => {
    // roundComplete then matchStarting: currentRoundComplete goes back to false because we are
    // playing in the new round — and that must not resurrect the "still running" wording.
    const p = phase({ currentRound: 2, lastRoundResults: [{}], currentMatchGameSessionId: 'g9' })
    expect(p.isWaitingForReady).toBe(false)
    expect(p.roundStillRunning).toBe(false)
    expect(p.roundNumber).toBe(2)
  })

  it('stops offering Ready when the tournament is over', () => {
    const p = phase({ currentRound: 7, lastRoundResults: [{}], currentRoundComplete: true, isComplete: true })
    expect(p.isWaitingForReady).toBe(false)
    expect(p.roundStillRunning).toBe(false)
  })
})
