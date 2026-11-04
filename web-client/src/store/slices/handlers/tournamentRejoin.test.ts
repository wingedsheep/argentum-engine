import { describe, it, expect } from 'vitest'
import { createLobbyHandlers } from './lobbyHandlers'
import type { GameStore, TournamentState } from '../types'
import { deriveRoundPhase } from '@/components/tournament/roundPhase'

/**
 * A tournament rejoin (a reconnect, or coming Back from a replay to `/tournament/<id>`) re-sends
 * `TournamentStarted` — which resets the client's tournament state — followed by a `MatchComplete`
 * with no results that says "you are between matches". That snapshot used to leave
 * `lastRoundResults` null, so the standings hid the Ready button and the player could never start
 * their next match.
 */

function harness(initial: Partial<GameStore>) {
  let state = { ...initial } as GameStore
  const set = (partial: unknown) => {
    const next = typeof partial === 'function' ? (partial as (s: GameStore) => Partial<GameStore>)(state) : partial
    state = { ...state, ...(next as Partial<GameStore>) }
  }
  const handlers = createLobbyHandlers(set as never, () => state)
  return { handlers, get: () => state }
}

describe('tournament rejoin snapshot', () => {
  it('leaves the player able to ready up for their next match', () => {
    const { handlers, get } = harness({ gameState: null, gameOverState: null, deckBuildingState: null })

    handlers.onTournamentStarted({ type: 'tournamentStarted', lobbyId: 'L', totalRounds: 3, standings: [] } as never)
    handlers.onMatchComplete({
      type: 'matchComplete',
      lobbyId: 'L',
      round: 2,
      results: [],
      standings: [],
      nextOpponentName: 'Bob',
      nextRoundHasBye: false,
      isTournamentComplete: false,
      roundComplete: false,
    } as never)

    const t = get().tournamentState as TournamentState
    expect(t.lastRoundResults).toEqual([])
    expect(deriveRoundPhase(t).isWaitingForReady).toBe(true)
  })
})
