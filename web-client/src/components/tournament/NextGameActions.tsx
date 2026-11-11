import type { BetweenGamesMessage } from '@/types'
import overlay from '../game/overlay/GameOverlays.module.css'
import styles from './NextGameActions.module.css'

/**
 * The game-over card's way into the next game of a best-of-three, for a player who doesn't want to
 * sideboard: ready up with the deck that just played, straight from the card. The loser of the game
 * (or the last chooser, after a draw) makes the play/draw choice here too, so the common case — keep
 * the deck, pick play — is one click. Sideboarding stays one click away ("Sideboard first", rendered
 * by the card beside its other secondary actions), and readying here is as reversible as readying on
 * the sideboarding screen: that screen opens in its waiting state, with Keep editing.
 */
export function NextGameActions({
  snapshot,
  onReady,
}: {
  snapshot: BetweenGamesMessage
  /** Ready with the current deck; [playFirst] only when this seat chooses play/draw. */
  onReady: (playFirst?: boolean) => void
}) {
  const game = snapshot.nextGameNumber
  if (snapshot.youAreReady) {
    // Back after a reconnect with the Ready already in: nothing left to decide here.
    return (
      <button type="button" className={overlay.primary} onClick={() => onReady()} data-testid="game-over-continue">
        Continue to game {game}
      </button>
    )
  }
  if (!snapshot.youChoosePlayDraw) {
    return (
      <button type="button" className={overlay.primary} onClick={() => onReady()} data-testid="game-over-ready">
        Ready for game {game}
      </button>
    )
  }
  return (
    <div className={styles.choice}>
      <p className={styles.prompt}>
        {snapshot.lastGameResult === 'DRAW' ? 'You chose last game, so you choose again' : 'You lost, so you choose'}
        {' '}— who plays first in game {game}?
      </p>
      <div className={styles.row} role="group" aria-label={`Who takes the first turn in game ${game}`}>
        <button type="button" className={overlay.primary} onClick={() => onReady(true)} data-testid="game-over-play-first">
          Play first
        </button>
        <button type="button" className={`${overlay.secondary} ${styles.draw}`} onClick={() => onReady(false)} data-testid="game-over-draw-first">
          Draw first
        </button>
      </div>
    </div>
  )
}
