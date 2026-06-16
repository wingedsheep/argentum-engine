import type { ReactNode } from 'react'
import { useDraggable } from '@/hooks/useDraggable.ts'
import styles from './DecisionUI.module.css'

/**
 * Side banner that the player can drag out of the way (e.g. when the targeting
 * prompt covers the opponent's board on a small screen). Renders a grab handle at
 * the top; the banner keeps its CSS-default position until the handle is dragged.
 */
export function DraggableBanner({
  className,
  children,
}: {
  className: string | undefined
  children: ReactNode
}) {
  const { ref, handleProps, style, isDragging } = useDraggable()

  return (
    <div ref={ref} className={className} style={style}>
      <div
        className={styles.dragHandle}
        style={isDragging ? { cursor: 'grabbing' } : undefined}
        aria-label="Drag to move"
        {...handleProps}
      >
        <span className={styles.dragHandleGrip} />
      </div>
      {children}
    </div>
  )
}
