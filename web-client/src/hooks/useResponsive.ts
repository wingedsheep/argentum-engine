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

    // Scale down card sizes when viewport height is limited.
    // The board needs to fit: opponent info + opponent hand + 2 battlefield rows (opponent) +
    // center area + 2 battlefield rows (player) + player hand + player controls.
    // On short screens, reduce card sizes proportionally.
    const heightScale = height < 900 ? Math.max(0.6, height / 900) : 1

    const cardWidth = Math.round(baseCardWidth * heightScale)
    const cardHeight = Math.round(cardWidth * cardRatio)

    // Small cards (opponent hand)
    const baseSmallCardWidth = isMobile ? 40 : isTablet ? 50 : 60
    const smallCardWidth = Math.round(baseSmallCardWidth * heightScale)
    const smallCardHeight = Math.round(smallCardWidth * cardRatio)

    // Battlefield cards - slightly smaller than hand cards
    const baseBattlefieldCardWidth = isMobile ? 60 : isTablet ? 80 : 100
    const battlefieldCardWidth = Math.round(baseBattlefieldCardWidth * heightScale)
    const battlefieldCardHeight = Math.round(battlefieldCardWidth * cardRatio)

    // Pile sizes (deck/graveyard)
    const basePileWidth = isMobile ? 50 : isTablet ? 60 : 70
    const pileWidth = Math.round(basePileWidth * heightScale)
    const pileHeight = Math.round(pileWidth * cardRatio)

    // Spacing scales with screen size
    const cardGap = isMobile ? 4 : isTablet ? 6 : 8
    const sectionGap = isMobile ? 4 : isTablet ? 6 : 8
    const containerPadding = isMobile ? 8 : isTablet ? 12 : 16

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
