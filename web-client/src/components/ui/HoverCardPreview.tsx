import { useState, useEffect, type ReactNode } from 'react'
import { getCardImageUrl } from '@/utils/cardImages.ts'

const PREVIEW_WIDTH = 280
const MARGIN = 40
const VIEWPORT_PADDING = 10

export interface HoverCardPreviewProps {
  name: string
  imageUri: string | null
  /** Image size variant passed to getCardImageUrl */
  imageSize?: 'small' | 'normal' | 'large'
  pos: { x: number; y: number } | null
  rulings?: readonly { date: string; text: string }[] | undefined
  /** Extra content rendered below the card image (e.g., stats breakdown, keywords) */
  children?: ReactNode
  /** Estimated extra height from children, used for vertical positioning (default 0) */
  extraHeight?: number
}

/**
 * Shared card hover preview — positions a large card image near the cursor.
 * Used by the game board, deck builder, and all draft overlays.
 */
export function HoverCardPreview({ name, imageUri, imageSize = 'large', pos, rulings, children, extraHeight = 0 }: HoverCardPreviewProps) {
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

  const imageUrl = getCardImageUrl(name, imageUri, imageSize)
  const previewWidth = PREVIEW_WIDTH
  const previewHeight = Math.round(previewWidth * 1.4)
  const hasRulings = rulings && rulings.length > 0

  // Estimate total height for positioning
  let panelHeight = extraHeight
  const GAP = 8
  if (showRulings && hasRulings) panelHeight += 120 + GAP
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

  return (
    <div
      style={{
        position: 'fixed',
        top,
        left,
        pointerEvents: 'none',
        zIndex: 2500,
        transition: 'top 0.05s, left 0.05s',
        display: 'flex',
        flexDirection: 'column',
        gap: GAP,
      }}
    >
      <div
        style={{
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
          style={{ width: '100%', height: '100%', objectFit: 'cover' }}
        />
      </div>

      {children}

      {/* Rulings panel - appears after 1 second of hovering */}
      {showRulings && hasRulings && (
        <div style={rulingsStyles.container}>
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
    </div>
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
    maxHeight: 300,
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
