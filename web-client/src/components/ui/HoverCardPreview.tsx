import { useState, useEffect, useRef, type ReactNode } from 'react'
import { createPortal } from 'react-dom'
import { getCardImageUrl } from '@/utils/cardImages.ts'

const PREVIEW_WIDTH = 280
const MARGIN = 40
/** Height of the `hint` row below the image, for vertical positioning. */
const HINT_HEIGHT = 30
const VIEWPORT_PADDING = 10
const RULINGS_MAX_HEIGHT = 300

export interface HoverCardPreviewProps {
  name: string
  imageUri: string | null
  /** Image size variant passed to getCardImageUrl */
  imageSize?: 'small' | 'normal' | 'large'
  pos: { x: number; y: number } | null
  rulings?: readonly { date: string; text: string }[] | undefined
  /** Extra content rendered below the card image (e.g., stats breakdown, keywords) */
  children?: ReactNode
  /** Content rendered as an overlay on top of the card image */
  overlay?: ReactNode
  /** A short hint rendered directly below the card image (the DFC "F to flip" pill) */
  hint?: ReactNode
  /** Estimated extra height from children, used for vertical positioning (default 0) */
  extraHeight?: number
  /**
   * Rotate the printed-portrait image by this angle to display it landscape (used by
   * Rooms — CR 709.5 — where the printed orientation is sideways). Container dims swap
   * accordingly so the rotated image fills the preview. Overlay content stays in
   * container (post-rotation) coordinates and is *not* rotated by this prop.
   */
  imageRotateDeg?: 0 | 90 | 180 | 270
}

/**
 * Shared card hover preview — positions a large card image near the cursor.
 * Used by the game board, deck builder, and all draft overlays.
 *
 * Always portalled to `<body>` on the tooltip layer (`--z-tooltip`). A cursor-following
 * preview is a viewport-level layer, so it must not be trapped in whatever stacking context
 * happens to wrap its caller: the spectator/replay shells wrap the whole board in a
 * `position: fixed; z-index: 1500` container, which clamped an in-tree preview *below* the
 * zone browsers (graveyard/exile/deck) that portal to `<body>` at z-index 2000. Cards can
 * also live inside `overflow: hidden` / transformed ancestors (a tapped permanent rotates),
 * which would clip a preview rendered in place.
 */
export function HoverCardPreview({ name, imageUri, imageSize = 'large', pos, rulings, children, overlay, hint, extraHeight = 0, imageRotateDeg = 0 }: HoverCardPreviewProps) {
  const [showRulings, setShowRulings] = useState(false)
  const [lastCardName, setLastCardName] = useState<string | null>(null)

  // Show rulings after hovering for 1 second
  useEffect(() => {
    if (name !== lastCardName) {
      setLastCardName(name)
      setShowRulings(false)
    }

    const timer = setTimeout(() => {
      setShowRulings(true)
    }, 1000)

    return () => clearTimeout(timer)
  }, [name, lastCardName])

  // The preview is `pointer-events: none` and the cursor stays on the hovered card, so the
  // rulings panel can never receive a wheel event of its own. Route wheel input to it while
  // it is open, and only swallow the event while the panel can still scroll that way — at
  // either end the wheel falls through to whatever is underneath (a deck list, a zone browser).
  const rulingsRef = useRef<HTMLDivElement>(null)
  const hasRulings = !!rulings && rulings.length > 0
  const rulingsOpen = showRulings && hasRulings
  useEffect(() => {
    if (!rulingsOpen) return
    const onWheel = (e: WheelEvent) => {
      const el = rulingsRef.current
      if (!el || el.scrollHeight <= el.clientHeight) return
      const delta = e.deltaMode === WheelEvent.DOM_DELTA_LINE ? e.deltaY * 16 : e.deltaY
      const atTop = el.scrollTop <= 0
      const atBottom = el.scrollTop + el.clientHeight >= el.scrollHeight - 1
      if ((delta < 0 && atTop) || (delta > 0 && atBottom) || delta === 0) return
      e.preventDefault()
      el.scrollTop += delta
    }
    window.addEventListener('wheel', onWheel, { passive: false, capture: true })
    return () => window.removeEventListener('wheel', onWheel, { capture: true })
  }, [rulingsOpen])

  const imageUrl = getCardImageUrl(name, imageUri, imageSize)
  const portraitWidth = PREVIEW_WIDTH
  const portraitHeight = Math.round(portraitWidth * 1.4)
  // For sideways layouts (Rooms), the displayed container is landscape; the image element
  // keeps its portrait pixel dims and rotates inside it.
  const isLandscape = imageRotateDeg === 90 || imageRotateDeg === 270
  const previewWidth = isLandscape ? portraitHeight : portraitWidth
  const previewHeight = isLandscape ? portraitWidth : portraitHeight

  // Estimate total height for positioning
  const GAP = 8
  let panelHeight = extraHeight + (hint ? HINT_HEIGHT + GAP : 0)
  if (rulingsOpen) panelHeight += 120 + GAP
  else if (hasRulings) panelHeight += 20 + GAP
  const estimatedHeight = previewHeight + panelHeight

  // Position near cursor, clamped to viewport
  let top = 80
  let left = 20
  if (pos) {
    const vw = window.innerWidth

    if (pos.x + previewWidth + MARGIN < vw - VIEWPORT_PADDING) {
      left = pos.x + MARGIN
    } else if (pos.x - previewWidth - MARGIN > VIEWPORT_PADDING) {
      left = pos.x - previewWidth - MARGIN
    } else {
      left = Math.max(VIEWPORT_PADDING, (vw - previewWidth) / 2)
    }
    left = Math.max(VIEWPORT_PADDING, Math.min(left, vw - previewWidth - VIEWPORT_PADDING))

    // Place the preview above the cursor, falling back to below if too close to top
    const aboveTop = pos.y - estimatedHeight - MARGIN
    if (aboveTop >= VIEWPORT_PADDING) {
      top = aboveTop
    } else {
      top = VIEWPORT_PADDING
    }
  }

  // Keep the rulings panel inside the viewport; anything beyond it scrolls (see the wheel hook).
  const rulingsMaxHeight = Math.max(
    120,
    Math.min(RULINGS_MAX_HEIGHT, window.innerHeight - top - previewHeight - extraHeight - (hint ? HINT_HEIGHT + GAP : 0) - GAP * 2 - VIEWPORT_PADDING),
  )

  return createPortal(
    <div
      style={{
        position: 'fixed',
        top,
        left,
        pointerEvents: 'none',
        zIndex: 'var(--z-tooltip)' as unknown as number,
        display: 'flex',
        flexDirection: 'column',
        gap: GAP,
      }}
    >
      <div
        style={{
          position: 'relative',
          width: previewWidth,
          height: previewHeight,
          borderRadius: 12,
          overflow: 'hidden',
          boxShadow: '0 8px 32px rgba(0, 0, 0, 0.8), 0 0 0 2px rgba(255, 255, 255, 0.1)',
        }}
      >
        <img
          src={imageUrl}
          alt={name}
          style={imageRotateDeg
            ? {
                position: 'absolute',
                top: '50%',
                left: '50%',
                width: portraitWidth,
                height: portraitHeight,
                transform: `translate(-50%, -50%) rotate(${imageRotateDeg}deg)`,
                objectFit: 'cover',
              }
            : { width: '100%', height: '100%', objectFit: 'cover' }
          }
        />
        {overlay}
      </div>

      {hint}

      {children}

      {/* Rulings panel - appears after 1 second of hovering */}
      {rulingsOpen && (
        <div ref={rulingsRef} style={{ ...rulingsStyles.container, maxHeight: rulingsMaxHeight }}>
          <div style={rulingsStyles.header}>Rulings</div>
          {rulings!.map((ruling, index) => (
            <div key={index} style={rulingsStyles.ruling}>
              <div style={rulingsStyles.date}>{ruling.date}</div>
              <div style={rulingsStyles.text}>{ruling.text}</div>
            </div>
          ))}
        </div>
      )}

      {/* Rulings indicator */}
      {!showRulings && hasRulings && (
        <div style={rulingsStyles.hint}>
          Hold to see rulings...
        </div>
      )}
    </div>,
    document.body,
  )
}

const rulingsStyles = {
  container: {
    display: 'flex',
    flexDirection: 'column' as const,
    gap: 8,
    backgroundColor: 'rgba(0, 0, 0, 0.92)',
    padding: 12,
    borderRadius: 8,
    border: '1px solid rgba(100, 150, 255, 0.3)',
    maxWidth: 320,
    overflowY: 'auto' as const,
  },
  header: {
    color: '#6699ff',
    fontWeight: 700,
    fontSize: 13,
    textTransform: 'uppercase' as const,
    letterSpacing: 1,
    borderBottom: '1px solid rgba(100, 150, 255, 0.2)',
    paddingBottom: 6,
  },
  ruling: {
    display: 'flex',
    flexDirection: 'column' as const,
    gap: 2,
  },
  date: {
    color: '#888888',
    fontSize: 11,
    fontStyle: 'italic' as const,
  },
  text: {
    color: '#dddddd',
    fontSize: 12,
    lineHeight: 1.4,
  },
  hint: {
    color: '#666666',
    fontSize: 11,
    fontStyle: 'italic' as const,
    textAlign: 'center' as const,
    padding: '4px 8px',
  },
}
