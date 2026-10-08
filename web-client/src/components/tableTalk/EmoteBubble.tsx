import { useLayoutEffect, useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import { useTableTalkStore } from '@/store/tableTalkStore'
import type { EntityId } from '@/types'
import { emoteInfo } from './emotes'
import styles from './TableTalk.module.css'

/**
 * The speech bubble on a seat while their last emote is showing. It is mounted on whichever element
 * carries that seat's anchors (`data-life-id`) — the center-HUD life orb, a board's name plate on the
 * multiplayer table, or a rail chip while the seat's board is off screen — so exactly one bubble shows
 * per seat, whatever the layout.
 *
 * [placement] says which side of the anchor has room — above yours at the bottom of the screen, below
 * an opponent's at the top, to the right of a chip in the left-hand rail. `'auto'` picks above or below
 * from where the anchor sits on screen, for plates that can land on either half of the table.
 *
 * Rendered where the orb is (an invisible anchor fills the orb, which is `position: relative`) but
 * portalled to the body at the anchor's rect: the HUD row the orb lives in clips its overflow and
 * stacks below the battlefields, so a bubble drawn in place would be cut off.
 */
export function EmoteBubble({
  playerId,
  placement: requested,
}: {
  playerId: EntityId
  placement: 'above' | 'below' | 'right' | 'auto'
}) {
  const bubble = useTableTalkStore((s) => s.bubbles[playerId])
  const anchorRef = useRef<HTMLSpanElement>(null)
  const [rect, setRect] = useState<DOMRect | null>(null)

  useLayoutEffect(() => {
    if (!bubble) return
    setRect(anchorRef.current?.getBoundingClientRect() ?? null)
  }, [bubble])

  const info = bubble ? emoteInfo(bubble.emote) : null
  const placement =
    requested !== 'auto' ? requested
      : rect && rect.top + rect.height / 2 > window.innerHeight / 2 ? 'above' : 'below'
  return (
    <>
      <span ref={anchorRef} className={styles.bubbleAnchor} aria-hidden />
      {bubble && info && rect && createPortal(
        <div
          // Keyed on the bubble id so a repeat of the same emote replays the pop.
          key={bubble.id}
          className={styles.bubble}
          data-placement={placement}
          data-mood={info.mood}
          data-emote={info.emote}
          style={
            placement === 'right'
              ? { left: rect.right + 12, top: rect.top + rect.height / 2 }
              : {
                  left: rect.left + rect.width / 2,
                  ...(placement === 'above'
                    ? { bottom: window.innerHeight - rect.top + 12 }
                    : { top: rect.bottom + 12 }),
                }
          }
          role="status"
          aria-live="polite"
          data-testid={`emote-bubble-${playerId}`}
        >
          <span className={styles.bubbleIcon} aria-hidden>{info.icon}</span>
          <span className={styles.bubbleText}>{info.label}</span>
        </div>,
        document.body,
      )}
    </>
  )
}
