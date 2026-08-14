/**
 * The inline `?` affordance: a small round button that opens a popover with a topic's summary and
 * a link to the full entry.
 *
 * **Portal-rendered on purpose.** The app is `overflow: hidden` and the multiplayer opponent strip
 * uses a CSS transform, which breaks `position: fixed` for anything nested inside it — the same
 * reason the zone browsers in `ZonePiles.tsx` portal to `document.body`. Anchoring is measured
 * from the button's bounding rect at open time.
 */
import { useEffect, useLayoutEffect, useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import { useNavigate } from 'react-router-dom'
import { topicById, helpHref } from '@/help/topics'
import { useHelpUi } from '@/help/helpStore'
import styles from './help.module.css'

const POPOVER_WIDTH = 280
const GAP = 8

export function HelpTip({
  topicId,
  label,
  size = 'md',
}: {
  topicId: string
  /** Accessible name; defaults to the topic title. */
  label?: string
  size?: 'sm' | 'md'
}) {
  const topic = topicById(topicId)
  const buttonRef = useRef<HTMLButtonElement>(null)
  const [open, setOpen] = useState(false)
  const [pos, setPos] = useState<{ top: number; left: number; above: boolean } | null>(null)
  const navigate = useNavigate()
  const drawerMounted = useHelpUi((s) => s.mounted)
  const openDrawer = useHelpUi((s) => s.openDrawer)

  useLayoutEffect(() => {
    if (!open) return
    const rect = buttonRef.current?.getBoundingClientRect()
    if (!rect) return
    const left = Math.max(
      GAP,
      Math.min(rect.left + rect.width / 2 - POPOVER_WIDTH / 2, window.innerWidth - POPOVER_WIDTH - GAP),
    )
    // Prefer below; flip above when there isn't room.
    const below = rect.bottom + GAP
    const above = below + 180 > window.innerHeight && rect.top > 200
    setPos({ top: above ? rect.top - GAP : below, left, above })
  }, [open])

  useEffect(() => {
    if (!open) return
    const close = () => setOpen(false)
    const onKey = (e: KeyboardEvent) => { if (e.key === 'Escape') { e.stopPropagation(); close() } }
    const onDown = (e: MouseEvent) => {
      if (buttonRef.current?.contains(e.target as Node)) return
      const popover = document.getElementById(`help-tip-${topicId}`)
      if (popover?.contains(e.target as Node)) return
      close()
    }
    // Capture phase so we close before the click reaches whatever is underneath.
    window.addEventListener('keydown', onKey, true)
    window.addEventListener('mousedown', onDown, true)
    window.addEventListener('scroll', close, true)
    return () => {
      window.removeEventListener('keydown', onKey, true)
      window.removeEventListener('mousedown', onDown, true)
      window.removeEventListener('scroll', close, true)
    }
  }, [open, topicId])

  if (!topic) return null

  const readMore = () => {
    setOpen(false)
    // In a game, navigating would tear down the WebSocket — use the drawer instead.
    if (drawerMounted) openDrawer(topic.id)
    else navigate(helpHref(topic))
  }

  return (
    <>
      <button
        ref={buttonRef}
        type="button"
        aria-label={label ?? `Help: ${topic.title}`}
        aria-expanded={open}
        className={`${styles.tipButton} ${size === 'sm' ? styles.tipButtonSm : ''} ${open ? styles.tipButtonOpen : ''}`}
        onClick={(e) => { e.preventDefault(); e.stopPropagation(); setOpen((v) => !v) }}
      >
        <span className={styles.tipGlyph} aria-hidden="true">?</span>
      </button>
      {open && pos && createPortal(
        <div
          id={`help-tip-${topicId}`}
          role="dialog"
          aria-label={topic.title}
          className={styles.tipPopover}
          style={{
            top: pos.top,
            left: pos.left,
            width: POPOVER_WIDTH,
            ...(pos.above ? { transform: 'translateY(-100%)' } : {}),
          }}
          onClick={(e) => e.stopPropagation()}
        >
          <div className={styles.tipTitle}>{topic.title}</div>
          <p className={styles.tipSummary}>{topic.summary}</p>
          <button type="button" className={styles.tipReadMore} onClick={readMore}>
            Read more →
          </button>
        </div>,
        document.body,
      )}
    </>
  )
}
