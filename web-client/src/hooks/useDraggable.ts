import { useCallback, useRef, useState } from 'react'
import type { CSSProperties, PointerEvent as ReactPointerEvent } from 'react'

interface DraggableResult {
  /** Attach to the element that should move. */
  ref: (node: HTMLElement | null) => void
  /** Spread onto the drag handle (the grab area). */
  handleProps: { onPointerDown: (e: ReactPointerEvent) => void }
  /** Inline style to apply to the element — empty until the user drags it. */
  style: CSSProperties
  isDragging: boolean
}

/**
 * Pointer-based drag for a floating element (mouse + touch via Pointer Events).
 *
 * The element keeps its CSS-defined position until the user grabs the handle; on
 * first drag we measure its current viewport rect and switch to explicit
 * `left`/`top` (clearing `right`/`transform` so CSS anchoring doesn't fight us),
 * then track the pointer and clamp the element inside the viewport.
 *
 * Built for the targeting/selection side banners, which are `position: fixed` and
 * can otherwise sit on top of the opponent's board on small screens (mobile) with
 * no way to move them out of the way.
 */
export function useDraggable(): DraggableResult {
  const elementRef = useRef<HTMLElement | null>(null)
  const dragOffset = useRef<{ x: number; y: number } | null>(null)
  const [pos, setPos] = useState<{ left: number; top: number } | null>(null)
  const [isDragging, setIsDragging] = useState(false)

  const ref = useCallback((node: HTMLElement | null) => {
    elementRef.current = node
  }, [])

  const onPointerMove = useCallback((e: PointerEvent) => {
    const offset = dragOffset.current
    const el = elementRef.current
    if (!offset || !el) return
    const margin = 4
    const maxLeft = window.innerWidth - el.offsetWidth - margin
    const maxTop = window.innerHeight - el.offsetHeight - margin
    const left = Math.max(margin, Math.min(e.clientX - offset.x, maxLeft))
    const top = Math.max(margin, Math.min(e.clientY - offset.y, maxTop))
    setPos({ left, top })
  }, [])

  const onPointerUp = useCallback(() => {
    dragOffset.current = null
    setIsDragging(false)
    window.removeEventListener('pointermove', onPointerMove)
    window.removeEventListener('pointerup', onPointerUp)
  }, [onPointerMove])

  const onPointerDown = useCallback((e: ReactPointerEvent) => {
    const el = elementRef.current
    if (!el) return
    const rect = el.getBoundingClientRect()
    dragOffset.current = { x: e.clientX - rect.left, y: e.clientY - rect.top }
    setPos({ left: rect.left, top: rect.top })
    setIsDragging(true)
    window.addEventListener('pointermove', onPointerMove)
    window.addEventListener('pointerup', onPointerUp)
    e.preventDefault()
  }, [onPointerMove, onPointerUp])

  const style: CSSProperties = pos
    ? { position: 'fixed', left: pos.left, top: pos.top, right: 'auto', transform: 'none' }
    : {}

  return { ref, handleProps: { onPointerDown }, style, isDragging }
}
