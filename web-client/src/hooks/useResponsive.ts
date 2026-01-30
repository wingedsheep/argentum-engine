import { useState, useEffect, useMemo } from 'react'

export interface ViewportSize {
  width: number
  height: number
}

export interface ResponsiveSizes {
  // Viewport dimensions
  viewportWidth: number
  viewportHeight: number

  // Card sizes
  cardWidth: number
  cardHeight: number
  smallCardWidth: number
  smallCardHeight: number
  battlefieldCardWidth: number
  battlefieldCardHeight: number

  // Pile sizes (deck/graveyard)
  pileWidth: number
  pileHeight: number

  // Spacing
  cardGap: number
  sectionGap: number
  containerPadding: number

  // Font sizes
  fontSize: {
    small: number
    normal: number
    large: number
    xlarge: number
  }

  // Layout flags
  isCompact: boolean
  isMobile: boolean
  isTablet: boolean
}

/**
 * Calculate the optimal card width to fit N cards in available width.
 * Returns width that ensures all cards are visible.
 */
export function calculateFittingCardWidth(
  cardCount: number,
  availableWidth: number,
  gap: number,
  maxCardWidth: number,
  minCardWidth: number = 50
): number {
  if (cardCount <= 0) return maxCardWidth

  // Calculate max width that fits all cards: availableWidth = (cardWidth * count) + (gap * (count - 1))
  // cardWidth = (availableWidth - gap * (count - 1)) / count
  const totalGaps = gap * Math.max(0, cardCount - 1)
  const calculatedWidth = Math.floor((availableWidth - totalGaps) / cardCount)

  // Clamp between min and max
  return Math.max(minCardWidth, Math.min(maxCardWidth, calculatedWidth))
}

/**
 * Hook to track viewport dimensions.
 */
export function useViewportSize(): ViewportSize {
  const [size, setSize] = useState<ViewportSize>({
    width: typeof window !== 'undefined' ? window.innerWidth : 1200,
    height: typeof window !== 'undefined' ? window.innerHeight : 800,
  })

  useEffect(() => {
    const handleResize = () => {
      setSize({
        width: window.innerWidth,
        height: window.innerHeight,
      })
    }

    window.addEventListener('resize', handleResize)
    return () => window.removeEventListener('resize', handleResize)
  }, [])

  return size
}

/**
 * Hook to get responsive sizes based on viewport.
 */
export function useResponsive(): ResponsiveSizes {
  const { width, height } = useViewportSize()

  return useMemo(() => {
    const isMobile = width < 640
    const isTablet = width >= 640 && width < 1024
    const isCompact = width < 1024 || height < 700

    // Base card width scales with viewport
    // Desktop: 120px, Tablet: 90px, Mobile: 70px
    const baseCardWidth = isMobile ? 70 : isTablet ? 90 : 120
    const cardRatio = 1.4 // MTG card aspect ratio

    // Spacing scales with screen size - more aggressive reduction for compact screens
    const cardGap = isMobile ? 2 : isCompact ? 4 : isTablet ? 6 : 8
    const sectionGap = isMobile ? 2 : isCompact ? 4 : isTablet ? 6 : 8
    const containerPadding = isMobile ? 4 : isCompact ? 8 : isTablet ? 12 : 16

    // Calculate height scale based on actual available space.
    // The layout has these vertical elements (after moving life/controls to center):
    // - Center area with life totals and phase indicator: ~40px
    // - Container padding (top + bottom): containerPadding * 2
    // - Section gaps between main areas: sectionGap * 2
    // - Card row padding/gaps: cardGap * 6
    // - Card rows (weighted by relative size):
    //   * Opponent hand (small cards): ~0.5 battlefield row equivalent
    //   * 4 battlefield rows: 4.0 rows
    //   * Player hand (larger cards): ~1.2 battlefield row equivalent
    //   Total: ~5.7 rows, use 6.0 for safety margin
    //
    // We need: fixedHeight + (6 * battlefieldCardHeight) <= viewportHeight
    const fixedElementsHeight = 40 + (containerPadding * 2) + (sectionGap * 2) + (cardGap * 6)
    const availableForCards = Math.max(150, height - fixedElementsHeight)
    const effectiveCardRows = 6.0
    const maxCardHeight = availableForCards / effectiveCardRows
    const maxCardWidth = maxCardHeight / cardRatio

    // Base battlefield card width (the largest cards that determine fit)
    const baseBattlefieldCardWidth = isMobile ? 60 : isTablet ? 80 : 100

    // Calculate scale: how much we need to shrink to fit
    const heightScale = Math.min(1, maxCardWidth / baseBattlefieldCardWidth)

    const cardWidth = Math.round(baseCardWidth * heightScale)
    const cardHeight = Math.round(cardWidth * cardRatio)

    // Small cards (opponent hand) - scale proportionally, extra small on compact
    const baseSmallCardWidth = isMobile ? 30 : isCompact ? 40 : isTablet ? 50 : 60
    const smallCardWidth = Math.round(baseSmallCardWidth * heightScale)
    const smallCardHeight = Math.round(smallCardWidth * cardRatio)

    // Battlefield cards - slightly smaller than hand cards
    const battlefieldCardWidth = Math.round(baseBattlefieldCardWidth * heightScale)
    const battlefieldCardHeight = Math.round(battlefieldCardWidth * cardRatio)

    // Pile sizes (deck/graveyard) - smaller on compact screens
    const basePileWidth = isMobile ? 40 : isCompact ? 50 : isTablet ? 60 : 70
    const pileWidth = Math.round(basePileWidth * heightScale)
    const pileHeight = Math.round(pileWidth * cardRatio)

    // Font sizes
    const fontSize = {
      small: isMobile ? 9 : isTablet ? 10 : 12,
      normal: isMobile ? 11 : isTablet ? 12 : 14,
      large: isMobile ? 14 : isTablet ? 16 : 18,
      xlarge: isMobile ? 18 : isTablet ? 24 : 36,
    }

    return {
      viewportWidth: width,
      viewportHeight: height,
      cardWidth,
      cardHeight,
      smallCardWidth,
      smallCardHeight,
      battlefieldCardWidth,
      battlefieldCardHeight,
      pileWidth,
      pileHeight,
      cardGap,
      sectionGap,
      containerPadding,
      fontSize,
      isCompact,
      isMobile,
      isTablet,
    }
  }, [width, height])
}
