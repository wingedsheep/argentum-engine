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

  // Layout-specific spacing (replaces hard-coded values)
  handBattlefieldGap: number    // Space between hand and battlefield
  battlefieldRowPadding: number // Min padding in battlefield rows
  zonePileOffset: number        // Vertical offset for deck/graveyard piles
  centerAreaHeight: number      // Height of center area (life + phase)

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
 *
 * @param topOffset - Optional offset to subtract from available height (e.g., spectator header)
 */
export function useResponsive(topOffset: number = 0): ResponsiveSizes {
  const { width, height } = useViewportSize()

  return useMemo(() => {
    // =========================================================================
    // Breakpoint Detection
    // =========================================================================
    const isMobile = width < 640
    const isTablet = width >= 640 && width < 1024
    const isCompact = width < 1024 || height < 700

    // =========================================================================
    // Base Sizes (before height scaling)
    // =========================================================================

    // Card widths scale with viewport width
    const baseCardWidth = isMobile ? 70 : isTablet ? 90 : 120
    const baseSmallCardWidth = isMobile ? 30 : isCompact ? 40 : isTablet ? 50 : 60
    const baseBattlefieldCardWidth = isMobile ? 60 : isTablet ? 80 : 100
    const basePileWidth = isMobile ? 40 : isCompact ? 50 : isTablet ? 60 : 70

    const cardRatio = 1.4 // MTG card aspect ratio

    // =========================================================================
    // Spacing (scales with screen size)
    // =========================================================================
    const cardGap = isMobile ? 2 : isCompact ? 4 : isTablet ? 6 : 8
    const sectionGap = isMobile ? 2 : isCompact ? 4 : isTablet ? 6 : 8
    const containerPadding = isMobile ? 4 : isCompact ? 8 : isTablet ? 12 : 16

    // =========================================================================
    // Fixed Element Heights
    // =========================================================================

    // Center area with life totals and phase indicator
    const centerAreaHeight = isMobile ? 50 : isCompact ? 55 : 65

    // =========================================================================
    // Height Scale Calculation
    // =========================================================================
    //
    // Calculate how much we need to scale cards to fit in available height.
    //
    // Vertical layout (top to bottom):
    //   1. Top offset (spectator header, etc.): topOffset
    //   2. Opponent hand: smallCardHeight
    //   3. Gap: handBattlefieldGap
    //   4. Opponent battlefield (2 rows): 2 × battlefieldCardHeight
    //   5. Center area: centerAreaHeight
    //   6. Player battlefield (2 rows): 2 × battlefieldCardHeight
    //   7. Gap: handBattlefieldGap
    //   8. Player hand: cardHeight
    //
    // Card height relationships (based on width ratios):
    //   - smallCardHeight = smallCardWidth × 1.4
    //   - battlefieldCardHeight = battlefieldCardWidth × 1.4
    //   - cardHeight = cardWidth × 1.4
    //
    // Express everything in terms of battlefieldCardWidth:
    //   - smallCardWidth ≈ 0.6 × battlefieldCardWidth
    //   - cardWidth ≈ 1.2 × battlefieldCardWidth
    //
    // So in battlefieldCardHeight equivalents:
    //   - Opponent hand: 0.6
    //   - Opponent battlefield: 2.0
    //   - Player battlefield: 2.0
    //   - Player hand: 1.2
    //   Total card rows: 5.8
    //
    // Add gaps and margins:
    //   - Hand-battlefield gaps (2): ~0.8 equivalent rows (accounts for fan arc + hover)
    //   - Section gaps and padding: ~0.4 equivalent rows
    // Total: 7.0 equivalent battlefield card heights
    //
    const effectiveCardRows = 7.0

    // Fixed elements that don't scale with cards
    const fixedHeight = topOffset + centerAreaHeight + (containerPadding * 2) + (sectionGap * 4)

    // Available height for card rows
    const availableForCards = Math.max(200, height - fixedHeight)

    // Maximum battlefield card height that fits
    const maxBattlefieldHeight = availableForCards / effectiveCardRows
    const maxBattlefieldWidth = maxBattlefieldHeight / cardRatio

    // Scale factor: how much to shrink from base sizes to fit
    const heightScale = Math.min(1, maxBattlefieldWidth / baseBattlefieldCardWidth)

    // =========================================================================
    // Final Card Sizes (after scaling)
    // =========================================================================

    const cardWidth = Math.round(baseCardWidth * heightScale)
    const cardHeight = Math.round(cardWidth * cardRatio)

    const smallCardWidth = Math.round(baseSmallCardWidth * heightScale)
    const smallCardHeight = Math.round(smallCardWidth * cardRatio)

    const battlefieldCardWidth = Math.round(baseBattlefieldCardWidth * heightScale)
    const battlefieldCardHeight = Math.round(battlefieldCardWidth * cardRatio)

    const pileWidth = Math.round(basePileWidth * heightScale)
    const pileHeight = Math.round(pileWidth * cardRatio)

    // =========================================================================
    // Layout-Specific Spacing (scales with card sizes)
    // =========================================================================

    // Gap between fixed hand and battlefield area
    // The hand fan has extra height for:
    //   - Fan arc effect (maxVerticalOffset: up to 15px)
    //   - Hover lift space (40px in HandFan)
    //   - Card rotation at edges
    // We need enough gap to prevent overlap with battlefield lands
    const handBattlefieldGap = Math.round(Math.max(cardGap * 4, cardHeight * 0.3))

    // Minimum padding inside battlefield rows (for visual breathing room)
    const battlefieldRowPadding = Math.round(battlefieldCardHeight * 0.4)

    // Vertical offset for zone piles (deck/graveyard) to avoid buttons
    // Scales with pile size to maintain proportion
    const zonePileOffset = Math.round(pileHeight * 0.5)

    // =========================================================================
    // Font Sizes
    // =========================================================================
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
      handBattlefieldGap,
      battlefieldRowPadding,
      zonePileOffset,
      centerAreaHeight,
      fontSize,
      isCompact,
      isMobile,
      isTablet,
    }
  }, [width, height, topOffset])
}
