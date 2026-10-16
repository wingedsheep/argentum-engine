import { describe, it, expect } from 'vitest'
import { createGameplayHandlers } from './gameplayHandlers'
import { GameOverReason, entityId } from '@/types'
import type { GameOverMessage } from '@/types'
import type { GameStore, SpectatingState } from '../types'
import { spectatorGameOverReason } from '@/components/spectating/SpectatorGameOverBanner'

/**
 * A spectator used to be dropped to the overview the instant a watched game ended, never seeing
 * who won. The server now sends spectators the public `gameOver`; these pin that the client keeps
 * the board up and records the result, and leaves a player's own game-over path alone.
 */

const GAME = 'game-1'

function spectating(): SpectatingState {
  return {
    gameSessionId: GAME,
    gameState: null,
    player1Id: 'p1',
    player2Id: 'p2',
    player1Name: 'Alice',
    player2Name: 'Bob',
    player1: null,
    player2: null,
    currentPhase: null,
    activePlayerId: null,
    priorityPlayerId: null,
    combat: null,
    decisionStatus: { playerId: 'p1', playerName: 'Alice', decisionType: 'x', displayText: 'Choosing' } as never,
  }
}

function harness(initial: Partial<GameStore>) {
  let state = { ...initial } as GameStore
  const set = (partial: unknown) => {
    const next = typeof partial === 'function' ? (partial as (s: GameStore) => Partial<GameStore>)(state) : partial
    state = { ...state, ...(next as Partial<GameStore>) }
  }
  const handlers = createGameplayHandlers(set as never, () => state)
  return { handlers, get: () => state }
}

function gameOver(over: Partial<GameOverMessage> = {}): GameOverMessage {
  return {
    type: 'gameOver',
    winnerId: entityId('p2'),
    winnerIds: [entityId('p2')],
    reason: GameOverReason.LIFE_ZERO,
    gameId: GAME,
    ...over,
  }
}

describe('spectator game over', () => {
  it('keeps the spectated board and records the result', () => {
    const { handlers, get } = harness({ sessionId: null, spectatingState: spectating(), gameOverState: null })
    handlers.onGameOver(gameOver())
    const s = get()
    expect(s.spectatingState?.gameSessionId).toBe(GAME)
    expect(s.spectatingState?.gameOver).toEqual({ winnerIds: ['p2'], reason: GameOverReason.LIFE_ZERO, message: null })
    expect(s.spectatingState?.decisionStatus).toBeNull()
    // Not the player's Victory/Defeat overlay.
    expect(s.gameOverState).toBeNull()
  })

  it('ignores a game-over for a game other than the one being watched', () => {
    const { handlers, get } = harness({ sessionId: null, spectatingState: spectating(), gameOverState: null })
    handlers.onGameOver(gameOver({ gameId: 'other' }))
    expect(get().spectatingState?.gameOver).toBeUndefined()
  })

  it('names the loser in the reason, and defers to a server message', () => {
    const seats = [{ playerId: entityId('p1'), name: 'Alice' }, { playerId: entityId('p2'), name: 'Bob' }]
    const result = { winnerIds: [entityId('p2')], reason: GameOverReason.LIFE_ZERO, message: null }
    expect(spectatorGameOverReason(result, seats)).toBe("Alice's life total reached zero.")
    expect(spectatorGameOverReason({ ...result, reason: GameOverReason.CONCESSION }, seats)).toBe('Alice conceded the game.')
    expect(spectatorGameOverReason({ ...result, message: 'Nobody could break the loop.' }, seats)).toBe('Nobody could break the loop.')
    expect(spectatorGameOverReason({ winnerIds: [], reason: GameOverReason.DRAW, message: null }, seats)).toBe('Nobody won — the game is a draw.')
  })
})
