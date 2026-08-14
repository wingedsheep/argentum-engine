/**
 * DeckPickerModal — the shell every "what does this seat bring?" dialog opens in.
 *
 * There were four of these: the quick lobby's own seat and the premade-decks tournament seat shared
 * one, the quick lobby's AI seat had a near-copy, and the pod's AI seat used the generic confirm
 * panel with a hand-tuned `maxWidth`. So the same question arrived in three different sizes
 * depending on which seat you clicked. One shell, one header, one Done button.
 *
 * Named for the seat it belongs to (`title`) with one line of orientation under it (`subtitle`),
 * because these are always opened *from a player row* and the first thing to be sure of is whose
 * deck is about to change.
 */
import type { ReactNode } from 'react'
import styles from './GameUI.module.css'

export function DeckPickerModal({
  title,
  subtitle = 'Choose the deck for this player seat.',
  onClose,
  children,
}: {
  title: string
  subtitle?: string
  onClose: () => void
  children: ReactNode
}) {
  return (
    <div className={styles.confirmBackdrop} role="dialog" aria-modal="true" onClick={onClose}>
      <div className={styles.deckPickerModal} onClick={(event) => event.stopPropagation()}>
        <div className={styles.deckPickerModalHeader}>
          <div>
            <div className={styles.confirmTitle}>{title}</div>
            <p className={styles.confirmBody}>{subtitle}</p>
          </div>
          <button
            type="button"
            onClick={onClose}
            className={styles.deckPickerModalClose}
            aria-label="Close deck picker"
          >
            ×
          </button>
        </div>
        {children}
        <div className={styles.confirmActions}>
          <button type="button" onClick={onClose} className={styles.startButton}>Done</button>
        </div>
      </div>
    </div>
  )
}
