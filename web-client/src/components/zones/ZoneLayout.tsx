import type { Vector3Tuple } from 'three'

/**
 * Standard card dimensions (MTG card ratio 2.5" x 3.5")
 * Scaled up for better visibility in the 3D view
 */
export const CARD_WIDTH = 1.4
export const CARD_HEIGHT = 2.0
export const CARD_DEPTH = 0.02

/**
 * Card scale factors for different zones
 */
export const CARD_SCALES = {
  hand: 1.0,
  battlefield: 0.9,
  stack: 0.8,
  graveyard: 0.75,
  library: 0.75,
} as const

/**
 * Spacing between cards in different zones
 */
export const CARD_SPACING = {
  hand: 1.0,
  battlefieldRow: 1.8,
  battlefieldColumn: 2.4,
  stack: 0.3,
  graveyardOffset: 0.04,
} as const

/**
 * Maximum cards visible in zones before stacking/overflow
 */
export const MAX_VISIBLE_CARDS = {
  hand: 10,
  battlefieldRow: 8,
  stack: 5,
  graveyard: 10,
} as const

/**
 * Zone positions on the table - MTG Arena style layout
 *
 * Layout from top to bottom:
 * - Opponent hand (face down, row)
 * - Opponent lands
 * - Opponent creatures
 * - Center divider
 * - Player creatures
 * - Player lands
 * - Player hand (visible, overlapping row)
 */
export const ZONE_POSITIONS = {
  // Player zones (positive Z - bottom of screen)
  playerHand: [0, 0, 5.5] as Vector3Tuple,
  playerLands: [0, 0, 2.5] as Vector3Tuple,
  playerCreatures: [0, 0, 0] as Vector3Tuple,
  playerOther: [-8.0, 0, 1.2] as Vector3Tuple,
  playerLibrary: [9.0, 0, 4.0] as Vector3Tuple,
  playerGraveyard: [9.0, 0, 1.5] as Vector3Tuple,

  // Opponent zones (negative Z - top of screen)
  opponentHand: [0, 0, -5.5] as Vector3Tuple,
  opponentLands: [0, 0, -2.5] as Vector3Tuple,
  opponentCreatures: [0, 0, 0] as Vector3Tuple,
  opponentOther: [8.0, 0, -1.2] as Vector3Tuple,
  opponentLibrary: [-9.0, 0, -4.0] as Vector3Tuple,
  opponentGraveyard: [-9.0, 0, -1.5] as Vector3Tuple,

  // Shared zones
  stack: [10.0, 0, 0] as Vector3Tuple,
} as const

/**
 * Calculate positions for cards in a horizontal row (e.g., lands, creatures)
 */
export function calculateRowPositions(
  count: number,
  spacing: number,
  maxWidth: number = 8
): number[] {
  if (count === 0) return []

  const totalWidth = (count - 1) * spacing
  const actualSpacing = totalWidth > maxWidth ? maxWidth / (count - 1) : spacing
  const startX = -((count - 1) * actualSpacing) / 2

  return Array.from({ length: count }, (_, i) => startX + i * actualSpacing)
}

/**
 * Calculate positions for cards in a fan layout (e.g., hand)
 */
export function calculateFanPositions(
  count: number,
  maxSpread: number = 4,
  maxAngle: number = Math.PI / 6
): Array<{ x: number; angle: number; z: number }> {
  if (count === 0) return []
  if (count === 1) return [{ x: 0, angle: 0, z: 0 }]

  const positions: Array<{ x: number; angle: number; z: number }> = []
  const spreadPerCard = Math.min(0.5, maxSpread / count)

  for (let i = 0; i < count; i++) {
    const t = count === 1 ? 0 : (i / (count - 1)) * 2 - 1 // -1 to 1
    const x = t * (spreadPerCard * (count - 1)) / 2
    const angle = t * (maxAngle / 2)
    // Slight arc - cards in the middle are slightly forward
    const z = -Math.abs(t) * 0.1

    positions.push({ x, angle, z })
  }

  return positions
}

/**
 * Calculate positions for cards in a stack (e.g., spell stack)
 */
export function calculateStackPositions(
  count: number,
  offsetY: number = 0.05,
  offsetZ: number = 0.15
): Array<{ y: number; z: number }> {
  return Array.from({ length: count }, (_, i) => ({
    y: i * offsetY,
    z: -i * offsetZ,
  }))
}

/**
 * Calculate positions for cards in a pile (e.g., graveyard)
 */
export function calculatePilePositions(
  count: number,
  randomOffset: number = 0.02
): Array<{ x: number; y: number; z: number; rotation: number }> {
  // Use seeded random for consistent positioning
  const seededRandom = (seed: number) => {
    const x = Math.sin(seed * 12.9898) * 43758.5453
    return x - Math.floor(x)
  }

  return Array.from({ length: count }, (_, i) => ({
    x: (seededRandom(i * 1.1) - 0.5) * randomOffset * 2,
    y: i * 0.005,
    z: (seededRandom(i * 2.3) - 0.5) * randomOffset * 2,
    rotation: (seededRandom(i * 3.7) - 0.5) * 0.1,
  }))
}

/**
 * Calculate grid positions for battlefield cards
 */
export function calculateGridPositions(
  count: number,
  columns: number = 5,
  spacingX: number = CARD_SPACING.battlefieldRow,
  spacingZ: number = CARD_SPACING.battlefieldColumn
): Array<{ x: number; z: number; row: number; col: number }> {
  if (count === 0) return []

  const positions: Array<{ x: number; z: number; row: number; col: number }> = []

  for (let i = 0; i < count; i++) {
    const col = i % columns
    const row = Math.floor(i / columns)
    const totalCols = Math.min(count - row * columns, columns)
    const startX = -((totalCols - 1) * spacingX) / 2

    positions.push({
      x: startX + col * spacingX,
      z: row * spacingZ,
      row,
      col,
    })
  }

  return positions
}
