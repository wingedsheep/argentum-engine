/**
 * Where a tournament player stands between games: in a match, waiting out a round that's still being
 * played at other tables, or genuinely between rounds.
 *
 * The distinction the overlay used to miss is the middle one. `lastRoundResults` is populated by our
 * *own* `matchComplete`, so an early finisher looked identical to a player whose round had ended —
 * and the header advertised `currentRound + 1` while three other tables were still playing. The
 * server settles it with `currentRoundComplete`; this derivation is the single place that reads it.
 *
 * Readying early is still allowed and still meaningful: the server keeps the flag and starts the
 * match the moment the round-boundary guards clear. Only the labelling changes.
 */
export interface RoundPhaseInput {
  readonly currentRound: number
  readonly currentRoundComplete: boolean
  readonly lastRoundResults: readonly unknown[] | null
  readonly currentMatchGameSessionId: string | null
  readonly isComplete: boolean
}

export interface RoundPhase {
  /** The player is out of a game and may click Ready — before round 1, or between matches. */
  readonly isWaitingForReady: boolean
  /** Other tables are still playing the round named by `roundNumber`. */
  readonly roundStillRunning: boolean
  /** The round the header should name: the one in progress, or the one being readied for. */
  readonly roundNumber: number
  /** Ready-button text — "next match" while the round runs on, "next round" once it's done. */
  readonly readyLabel: string
}

export function deriveRoundPhase(state: RoundPhaseInput): RoundPhase {
  const betweenMatches =
    // Before round 1 the server hasn't sent a round number yet.
    state.currentRound === 0 ||
    // Our match ended (results arrived) and we haven't been put in a new one.
    (state.lastRoundResults !== null && !state.currentMatchGameSessionId)
  const isWaitingForReady = betweenMatches && !state.isComplete

  const roundStillRunning = isWaitingForReady && state.currentRound > 0 && !state.currentRoundComplete

  return {
    isWaitingForReady,
    roundStillRunning,
    roundNumber: isWaitingForReady && !roundStillRunning ? state.currentRound + 1 : state.currentRound,
    readyLabel: roundStillRunning ? 'Ready for Next Match' : 'Ready for Next Round',
  }
}
