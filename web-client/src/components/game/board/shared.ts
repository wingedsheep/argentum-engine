import React, { createContext, useContext } from 'react'
import type { ResponsiveSizes } from '../../../hooks/useResponsive'
import { getScryfallFallbackUrl } from '../../../utils/cardImages'
import type { ClientCard, LegalActionInfo } from '../../../types'
import { CounterType } from '../../../types'
import { Color } from '../../../types/enums'

// Context to pass responsive sizes down the component tree
export const ResponsiveContext = createContext<ResponsiveSizes | null>(null)

export function useResponsiveContext(): ResponsiveSizes {
  const ctx = useContext(ResponsiveContext)
  if (!ctx) throw new Error('ResponsiveContext not provided')
  return ctx
}

/**
 * Check if a card has multiple potential casting options.
 * Returns true if the card has more than one way to be used.
 * The server now sends all potential actions (including unaffordable ones),
 * so we can simply count distinct action types.
 *
 * @param cardLegalActions Legal actions for this specific card from the server
 */
export function hasMultipleCastingOptions(cardLegalActions: LegalActionInfo[]): boolean {
  // Count distinct casting method types
  const hasNormalCast = cardLegalActions.some(
    (a) => a.action.type === 'CastSpell' && a.actionType !== 'CastFaceDown'
  )
  const hasMorphCast = cardLegalActions.some((a) => a.actionType === 'CastFaceDown')
  const hasCycling = cardLegalActions.some((a) => a.action.type === 'CycleCard')
  const hasPlayLand = cardLegalActions.some((a) => a.action.type === 'PlayLand')

  let options = 0
  if (hasNormalCast) options++
  if (hasMorphCast) options++
  if (hasCycling) options++
  if (hasPlayLand) options++

  return options > 1
}

/**
 * Handle image load error by falling back to Scryfall API.
 */
export function handleImageError(
  e: React.SyntheticEvent<HTMLImageElement>,
  cardName: string,
  version: 'small' | 'normal' | 'large' = 'normal'
): void {
  const img = e.currentTarget
  const fallbackUrl = getScryfallFallbackUrl(cardName, version)
  // Only switch to fallback if not already using it (prevent infinite loop)
  if (!img.src.includes('api.scryfall.com')) {
    img.src = fallbackUrl
  }
}

/**
 * Get color for P/T display based on modifications.
 * Green = buffed, Red = debuffed, White = normal
 */
export function getPTColor(
  power: number | null,
  toughness: number | null,
  basePower: number | null,
  baseToughness: number | null
): string {
  if (power === null || toughness === null || basePower === null || baseToughness === null) {
    return 'white'
  }

  const powerDiff = power - basePower
  const toughnessDiff = toughness - baseToughness

  // If both are increased or both are unchanged, and at least one is increased
  if (powerDiff >= 0 && toughnessDiff >= 0 && (powerDiff > 0 || toughnessDiff > 0)) {
    return '#00ff00' // Green for buffed
  }
  // If both are decreased or both are unchanged, and at least one is decreased
  if (powerDiff <= 0 && toughnessDiff <= 0 && (powerDiff < 0 || toughnessDiff < 0)) {
    return '#ff4444' // Red for debuffed
  }
  // Mixed buff/debuff - show yellow
  if (powerDiff !== 0 || toughnessDiff !== 0) {
    return '#ffff00' // Yellow for mixed
  }

  return 'white'
}

/**
 * Calculate the stat contribution from +1/+1 and -1/-1 counters.
 * Returns the net modifier (positive or negative).
 */
export function getCounterStatModifier(card: ClientCard): number {
  const plusCounters = card.counters[CounterType.PLUS_ONE_PLUS_ONE] ?? 0
  const minusCounters = card.counters[CounterType.MINUS_ONE_MINUS_ONE] ?? 0
  return plusCounters - minusCounters
}

/**
 * Check if a card has any +1/+1 or -1/-1 counters.
 */
export function hasStatCounters(card: ClientCard): boolean {
  const plusCounters = card.counters[CounterType.PLUS_ONE_PLUS_ONE] ?? 0
  const minusCounters = card.counters[CounterType.MINUS_ONE_MINUS_ONE] ?? 0
  return plusCounters > 0 || minusCounters > 0
}

/**
 * Get the number of gold counters on a card.
 */
export function getGoldCounters(card: ClientCard): number {
  return card.counters[CounterType.GOLD] ?? 0
}

/**
 * Get the number of plague counters on a card.
 */
export function getPlagueCounters(card: ClientCard): number {
  return card.counters[CounterType.PLAGUE] ?? 0
}

/**
 * Get an emoji or icon for an effect based on its icon identifier.
 */
export function getEffectIcon(icon: string): string {
  switch (icon) {
    case 'shield-off':
      return '🛡️'
    case 'shield':
      return '⚡'
    case 'skip':
      return '⏭️'
    case 'lock':
      return '🔒'
    case 'skull':
      return '💀'
    case 'taunt':
      return '⚔️'
    case 'prevent-damage':
      return '🛡️'
    case 'regeneration':
      return '♻️'
    default:
      return '⚡'
  }
}

// --- Token frame color helpers ---

const COLOR_FRAME: Record<Color, [string, string]> = {
  [Color.WHITE]: ['#f0e8d0', '#d4c9a8'],
  [Color.BLUE]:  ['#1a4a7a', '#0d2d50'],
  [Color.BLACK]: ['#3a3040', '#1e1828'],
  [Color.RED]:   ['#8a2a1a', '#5a1a10'],
  [Color.GREEN]: ['#1a5a2a', '#0d3a18'],
}

/** Returns a CSS gradient for a token card frame based on colors. */
export function getTokenFrameGradient(colors: readonly Color[]): string {
  if (colors.length === 0) return 'linear-gradient(180deg, #4a4a5e 0%, #2a2a3e 100%)'
  if (colors.length > 1) return 'linear-gradient(180deg, #b8953a 0%, #7a6320 100%)'
  const [light, dark] = COLOR_FRAME[colors[0]!] ?? ['#4a4a5e', '#2a2a3e']
  return `linear-gradient(180deg, ${light} 0%, ${dark} 100%)`
}

/** Returns a background color for the card fallback based on card colors. */
export function getCardFallbackColor(colors: readonly Color[]): string {
  if (colors.length === 0) return '#3a3a4e'
  if (colors.length > 1) return '#5a4a1a'
  switch (colors[0]) {
    case Color.WHITE: return '#6b6350'
    case Color.BLUE:  return '#1e3a5e'
    case Color.BLACK: return '#2a2230'
    case Color.RED:   return '#5e1e1e'
    case Color.GREEN: return '#1e4a2a'
    default:          return '#3a3a4e'
  }
}
