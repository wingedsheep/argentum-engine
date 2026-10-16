import { useEffect, useState } from 'react'
import overlay from '../game/overlay/GameOverlays.module.css'
import { GameOverReason } from '@/types'
import type { EntityId } from '@/types'
import type { SpectatorGameOver } from '@/store/slices/types'

/** Seconds the final board stays up before the spectator is taken back to the overview. */
export const SPECTATOR_AUTO_RETURN_SECONDS = 12

interface Seat {
  readonly playerId: EntityId
  readonly name: string
}

/**
 * Why the game ended, told from outside it: the player game-over copy names "you" and "your
 * opponent", a spectator needs names. A server-written message (a stalled game's explanation, a
 * card's own loss text) wins, as it does for players.
 */
export function spectatorGameOverReason(gameOver: SpectatorGameOver, seats: readonly Seat[]): string {
  if (gameOver.message) return gameOver.message
  if (gameOver.winnerIds.length === 0) return 'Nobody won — the game is a draw.'
  const losers = seats.filter((s) => !gameOver.winnerIds.includes(s.playerId))
  if (losers.length !== 1) {
    // A pod or a team game is won by outlasting several seats; naming how the last one fell
    // would misdescribe it.
    return gameOver.winnerIds.length > 1 ? 'Their team is the last one standing.' : 'The last player standing.'
  }
  const loser = losers[0]!.name
  switch (gameOver.reason) {
    case GameOverReason.LIFE_ZERO: return `${loser}'s life total reached zero.`
    case GameOverReason.DECK_OUT: return `${loser} had no cards left to draw.`
    case GameOverReason.CONCESSION: return `${loser} conceded the game.`
    case GameOverReason.POISON_COUNTERS: return `${loser} received ten poison counters.`
    case GameOverReason.DISCONNECTION: return `${loser} disconnected.`
    default: return `${loser} lost the game.`
  }
}

/**
 * The result card a spectator sees when the watched game ends. It sits over the final board
 * without blurring it, counts down to an automatic return to the overview, and can be set aside
 * to study the board — the header's Back to Overview still leaves from there.
 */
export function SpectatorGameOverBanner({
  gameOver,
  seats,
  onBack,
}: {
  gameOver: SpectatorGameOver
  seats: readonly Seat[]
  onBack: () => void
}) {
  const [secondsLeft, setSecondsLeft] = useState(SPECTATOR_AUTO_RETURN_SECONDS)
  const [dismissed, setDismissed] = useState(false)

  useEffect(() => {
    if (dismissed) return
    if (secondsLeft <= 0) {
      onBack()
      return
    }
    const timer = setTimeout(() => setSecondsLeft((s) => s - 1), 1000)
    return () => clearTimeout(timer)
  }, [dismissed, secondsLeft, onBack])

  if (dismissed) return null

  const winners = seats.filter((s) => gameOver.winnerIds.includes(s.playerId)).map((s) => s.name)
  const isDraw = gameOver.winnerIds.length === 0
  const title = isDraw ? 'Draw' : `${winners.length > 0 ? winners.join(' & ') : 'Game'} ${winners.length > 1 ? 'win' : 'wins'}`

  return (
    <div style={styles.layer}>
      <div
        className={overlay.card}
        data-result={isDraw ? 'draw' : 'win'}
        role="dialog"
        aria-labelledby="spectator-game-over-title"
        style={styles.card}
        data-testid="spectator-game-over"
      >
        <p className={overlay.eyebrow}>Game over</p>
        <h1 id="spectator-game-over-title" className={overlay.title} style={styles.title}>{title}</h1>
        <p className={overlay.reason}>{spectatorGameOverReason(gameOver, seats)}</p>
        <div className={overlay.actions}>
          <button type="button" onClick={onBack} className={overlay.primary}>
            Back to Overview
          </button>
          <div className={overlay.secondaryRow}>
            <button type="button" onClick={() => setDismissed(true)} className={overlay.secondary}>
              Stay on this board
            </button>
          </div>
        </div>
        <p style={styles.countdown} aria-live="polite">
          Returning to the overview in {secondsLeft}s
        </p>
      </div>
    </div>
  )
}

const styles: Record<string, React.CSSProperties> = {
  // Over the board, but no scrim or blur: the final board is the point of the moment.
  layer: {
    position: 'absolute',
    inset: 0,
    zIndex: 2000,
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    padding: 16,
    boxSizing: 'border-box',
    pointerEvents: 'none',
  },
  card: {
    pointerEvents: 'auto',
    background: 'rgba(10, 12, 20, 0.9)',
  },
  title: {
    fontSize: 'clamp(26px, 4.5vw, 38px)',
    textAlign: 'center',
    overflowWrap: 'anywhere',
  },
  countdown: {
    margin: '4px 0 0',
    fontSize: 12.5,
    color: '#8f98ac',
  },
}
