import { useEffect, useState, type ReactNode } from 'react'
import type { SealedCardInfo } from '@/types'

export interface DfcHoverFlip {
  readonly isHoveredDfc: boolean
  readonly dfcFlipped: boolean
  readonly displayName: string | null
  readonly displayImageUri: string | null
  readonly hint: ReactNode
  readonly resetFlip: () => void
}

/**
 * Press-F flipping for the hover preview of a double-faced card.
 * Listens for the `F` key while a DFC is hovered and toggles which face is shown.
 */
export function useDfcHoverFlip(hoveredCard: SealedCardInfo | null): DfcHoverFlip {
  const [dfcFlipped, setDfcFlipped] = useState(false)
  const isHoveredDfc = hoveredCard?.isDoubleFaced === true && !!hoveredCard.backFaceImageUri

  useEffect(() => {
    if (!isHoveredDfc) return
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'f' || e.key === 'F') setDfcFlipped((prev) => !prev)
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [isHoveredDfc])

  const showBack = isHoveredDfc && dfcFlipped
  const displayName = hoveredCard
    ? showBack && hoveredCard.backFaceName
      ? hoveredCard.backFaceName
      : hoveredCard.name
    : null
  const displayImageUri = hoveredCard
    ? showBack
      ? (hoveredCard.backFaceImageUri ?? null)
      : hoveredCard.imageUri
    : null

  return {
    isHoveredDfc,
    dfcFlipped,
    displayName,
    displayImageUri,
    hint: isHoveredDfc ? <DfcFlipHint flipped={dfcFlipped} /> : undefined,
    resetFlip: () => setDfcFlipped(false),
  }
}

function DfcFlipHint({ flipped }: { flipped: boolean }) {
  return (
    <div style={{
      position: 'absolute',
      bottom: 10,
      left: '50%',
      transform: 'translateX(-50%)',
      backgroundColor: 'rgba(0, 0, 0, 0.88)',
      color: '#d0d4e0',
      fontSize: 13,
      fontWeight: 600,
      padding: '5px 12px',
      borderRadius: 6,
      border: '1px solid rgba(180, 190, 220, 0.5)',
      boxShadow: '0 2px 8px rgba(0, 0, 0, 0.5)',
      whiteSpace: 'nowrap',
      zIndex: 5,
      display: 'flex',
      alignItems: 'center',
      gap: 6,
    }}>
      <i className={`ms ms-dfc-${flipped ? 'night' : 'day'}`} style={{ fontSize: 14 }} />
      <span style={{
        backgroundColor: 'rgba(255, 255, 255, 0.15)',
        padding: '1px 6px',
        borderRadius: 3,
        fontSize: 12,
        fontWeight: 700,
        letterSpacing: 0.5,
      }}>F</span>
      <span>to flip</span>
    </div>
  )
}
