/**
 * Floats a card preview that follows the cursor while a row/tile is hovered.
 * The position state lives here, not on the parent panel — so mouse motion
 * doesn't re-render the (potentially large) sibling list. The parent only
 * re-renders when the *hovered card* changes.
 */
import { useEffect, useState } from 'react'
import { HoverCardPreview } from '@/components/ui/HoverCardPreview'

export function HoverFollowPreview({
  name,
  imageUri,
  overlay,
  imageRotateDeg = 0,
}: {
  name: string | null
  imageUri: string | null
  overlay?: React.ReactNode
  imageRotateDeg?: 0 | 90
}) {
  const [pos, setPos] = useState<{ x: number; y: number } | null>(null)
  useEffect(() => {
    if (!name) {
      setPos(null)
      return
    }
    let pending: { x: number; y: number } | null = null
    let rafId: number | null = null
    const flush = () => {
      rafId = null
      if (pending) setPos(pending)
    }
    const onMove = (e: MouseEvent) => {
      pending = { x: e.clientX, y: e.clientY }
      if (rafId === null) rafId = requestAnimationFrame(flush)
    }
    window.addEventListener('mousemove', onMove)
    return () => {
      window.removeEventListener('mousemove', onMove)
      if (rafId !== null) cancelAnimationFrame(rafId)
    }
  }, [name])
  if (!name || !pos) return null
  return (
    <HoverCardPreview
      name={name}
      imageUri={imageUri}
      pos={pos}
      overlay={overlay}
      imageRotateDeg={imageRotateDeg}
    />
  )
}
