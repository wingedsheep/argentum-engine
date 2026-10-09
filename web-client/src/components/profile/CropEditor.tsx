/**
 * Choose which circle of a card's illustration becomes the avatar. The art is shown whole with the
 * circle cut out of a dimmed veil: drag the circle (or click elsewhere to jump it there), zoom with the
 * slider or the mouse wheel, nudge with the arrow keys and +/−. The crop is kept in units of the art's
 * height, the same units the stored avatar uses.
 */
import { useCallback, useRef, useState } from 'react'
import type React from 'react'
import { type CardCrop, MIN_CROP_SIZE } from './avatars'
import s from './CropEditor.module.css'

/** Where a fresh crop starts: a large circle, centred, a little above the middle (faces sit high). */
export function defaultCrop(aspect: number): CardCrop {
  const size = Math.min(0.8, aspect)
  return clampCrop({ x: (aspect - size) / 2, y: 0.08, size }, aspect)
}

export function clampCrop(crop: CardCrop, aspect: number): CardCrop {
  const size = Math.max(MIN_CROP_SIZE, Math.min(crop.size, 1, aspect))
  return {
    size,
    x: Math.max(0, Math.min(crop.x, aspect - size)),
    y: Math.max(0, Math.min(crop.y, 1 - size)),
  }
}

export function CropEditor({
  src,
  alt,
  crop,
  onChange,
}: {
  src: string
  alt: string
  /** Null until the art has loaded and a crop has been placed. */
  crop: CardCrop | null
  onChange: (crop: CardCrop) => void
}) {
  const frameRef = useRef<HTMLDivElement>(null)
  const [aspect, setAspect] = useState<number | null>(null)
  const drag = useRef<{ dx: number; dy: number } | null>(null)

  const toUnits = (e: { clientX: number; clientY: number }) => {
    const rect = frameRef.current!.getBoundingClientRect()
    return { u: (e.clientX - rect.left) / rect.height, v: (e.clientY - rect.top) / rect.height }
  }

  const set = useCallback(
    (next: CardCrop) => {
      if (aspect) onChange(clampCrop(next, aspect))
    },
    [aspect, onChange],
  )

  const onPointerDown = (e: React.PointerEvent) => {
    if (!crop || !aspect) return
    e.preventDefault()
    frameRef.current?.focus()
    const { u, v } = toUnits(e)
    const r = crop.size / 2
    const inside = Math.hypot(u - (crop.x + r), v - (crop.y + r)) <= r
    // Grab the circle where it was touched; a press outside it first jumps the circle there.
    const start = inside ? crop : clampCrop({ ...crop, x: u - r, y: v - r }, aspect)
    if (!inside) onChange(start)
    drag.current = { dx: u - start.x, dy: v - start.y }
    ;(e.currentTarget as HTMLElement).setPointerCapture(e.pointerId)
  }

  const onPointerMove = (e: React.PointerEvent) => {
    if (!drag.current || !crop) return
    const { u, v } = toUnits(e)
    set({ ...crop, x: u - drag.current.dx, y: v - drag.current.dy })
  }

  const endDrag = () => {
    drag.current = null
  }

  /** Resize about the circle's centre, so zooming never makes the subject jump. */
  const zoomTo = (size: number) => {
    if (!crop) return
    const cx = crop.x + crop.size / 2
    const cy = crop.y + crop.size / 2
    set({ size, x: cx - size / 2, y: cy - size / 2 })
  }

  const onWheel = (e: React.WheelEvent) => {
    if (!crop) return
    zoomTo(crop.size * (e.deltaY > 0 ? 1.06 : 1 / 1.06))
  }

  const onKeyDown = (e: React.KeyboardEvent) => {
    if (!crop) return
    const step = e.shiftKey ? 0.05 : 0.01
    const moves: Record<string, [number, number]> = {
      ArrowLeft: [-step, 0],
      ArrowRight: [step, 0],
      ArrowUp: [0, -step],
      ArrowDown: [0, step],
    }
    const move = moves[e.key]
    if (move) {
      e.preventDefault()
      set({ ...crop, x: crop.x + move[0], y: crop.y + move[1] })
    } else if (e.key === '+' || e.key === '=') {
      e.preventDefault()
      zoomTo(crop.size / 1.08)
    } else if (e.key === '-' || e.key === '_') {
      e.preventDefault()
      zoomTo(crop.size * 1.08)
    }
  }

  const maxSize = aspect ? Math.min(1, aspect) : 1
  // The slider reads as zoom: right is closer in, i.e. a smaller crop.
  const zoom = crop ? maxSize + MIN_CROP_SIZE - crop.size : maxSize

  return (
    <div className={s.root}>
      <div
        ref={frameRef}
        className={s.frame}
        tabIndex={0}
        role="group"
        aria-label="Avatar crop. Drag to move, scroll or use + and − to zoom, arrow keys to nudge."
        onPointerDown={onPointerDown}
        onPointerMove={onPointerMove}
        onPointerUp={endDrag}
        onPointerCancel={endDrag}
        onWheel={onWheel}
        onKeyDown={onKeyDown}
        data-loaded={aspect !== null}
        style={aspect ? ({ '--aspect': aspect } as React.CSSProperties) : undefined}
      >
        <img
          className={s.art}
          src={src}
          alt={alt}
          draggable={false}
          onLoad={(e) => {
            const img = e.currentTarget
            const a = img.naturalWidth / img.naturalHeight
            setAspect(a)
            onChange(crop ? clampCrop(crop, a) : defaultCrop(a))
          }}
        />
        {crop && aspect && (
          <div
            className={s.circle}
            style={{
              left: `${(crop.x / aspect) * 100}%`,
              top: `${crop.y * 100}%`,
              width: `${(crop.size / aspect) * 100}%`,
              height: `${crop.size * 100}%`,
            }}
          />
        )}
        {!aspect && <div className={s.loading}>Loading art…</div>}
      </div>
      <label className={s.zoomRow}>
        <span className={s.zoomIcon} aria-hidden>−</span>
        <input
          type="range"
          className={s.zoom}
          min={MIN_CROP_SIZE}
          max={maxSize}
          step={0.005}
          value={zoom}
          disabled={!crop}
          aria-label="Zoom"
          onChange={(e) => zoomTo(maxSize + MIN_CROP_SIZE - Number(e.target.value))}
        />
        <span className={s.zoomIcon} aria-hidden>+</span>
      </label>
    </div>
  )
}
