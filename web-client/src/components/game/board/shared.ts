import React, { createContext, useContext, useLayoutEffect, useMemo, useState, type RefObject } from 'react'
import type { ResponsiveSizes, BadgeSizes } from '../../../hooks/useResponsive'
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

// Hard ceiling on slot-derived card growth. The window-derived `useResponsive`
// caps base battlefieldCardWidth at 125 (desktop) — that estimate was tuned
// for the legacy layout where hand reservations weren't explicit grid tracks.
// With the explicit-track grid, slots are often substantially taller than that
// budget anticipated, so we let cards grow up to ~1.6× before clamping.
const SLOT_MAX_CARD_WIDTH = 200

/**
 * Measures the bounded slot a battlefield occupies (set up by the grid in
 * board/styles.ts) and derives card sizes that fit inside it. Cards both
 * shrink (when slot is too small) and grow (when slot has unused height,
 * up to SLOT_MAX_CARD_WIDTH) so the slot is used as fully as possible
 * without overflow.
 *
 * The `maxRowCount` argument is the largest card count across the two
 * battlefield rows (front + back). It's used to enforce a horizontal-fit
 * constraint so cards never wrap into a second physical row, which would
 * otherwise overflow the slot vertically and clip into / overlap with
 * the neighbouring row.
 *
 * The `maxRowTappedCount` argument is the largest tapped-card count across
 * the two rows. Tapped cards are rotated 90°, so their horizontal footprint
 * is the portrait card *height* (≈1.4× card width) rather than the width.
 * Without accounting for that, a row of many tapped creatures on a narrow
 * viewport overflows the slot horizontally, then flex-wraps to a second
 * physical line — which pushes the row's total height past the allotted
 * 1fr and spills into the adjacent center HUD.
 *
 * Phase 2 of the no-overlap layout: makes overflow into the center HUD
 * geometrically impossible by sizing cards from the actual slot rather
 * than estimating from window dimensions.
 */
export function useSlotSizedResponsive(
  slotRef: RefObject<HTMLElement | null>,
  maxRowCount: number = 0,
  maxRowTappedCount: number = 0,
): ResponsiveSizes {
  const base = useResponsiveContext()
  const [slotSize, setSlotSize] = useState<{ width: number; height: number } | null>(null)

  useLayoutEffect(() => {
    const node = slotRef.current
    if (!node) return
    const rect = node.getBoundingClientRect()
    setSlotSize({ width: rect.width, height: rect.height })
    const obs = new ResizeObserver((entries) => {
      const entry = entries[0]
      if (entry) setSlotSize({ width: entry.contentRect.width, height: entry.contentRect.height })
    })
    obs.observe(node)
    return () => obs.disconnect()
  }, [slotRef])

  return useMemo(() => {
    if (slotSize === null || slotSize.height <= 0 || slotSize.width <= 0) return base

    // Each battlefield holds two card rows separated by a divider strip
    // (24px + 2 × dividerMargin, see Battlefield.tsx). The `breathing` value
    // is leftover slot height that ends up between the cards and the slot's
    // anchored end — which, for both opponent (justify flex-start) and player
    // (justify flex-end), is the center-HUD side. So this is effectively the
    // gap between battlefield cards and the center HUD.
    //
    // Sized to clear the StepStrip's active-player chevron (6px triangle +
    // 3px gap = ~9px protruding from the HUD into our slot's bottom edge)
    // with room to spare. Scaled with slot height so tight viewports stay
    // tight and roomy ones get a comfortable gap.
    const dividerMargin = Math.max(10, Math.round(base.battlefieldCardHeight * 0.1))
    const dividerSpace = 24 + 2 * dividerMargin
    const breathing = Math.max(12, Math.min(48, Math.round(slotSize.height * 0.10)))
    const availablePerRow = (slotSize.height - dividerSpace - breathing) / 2

    // Vertical-fit cap: largest card the slot can hold along the height axis.
    const slotMaxHeight = Math.floor(availablePerRow)
    const widthFromHeight = Math.round(slotMaxHeight / 1.4)

    // Horizontal-fit cap: largest card width that lets `maxRowCount` cards sit
    // side-by-side in the slot's width (accounting for inter-card gaps). When
    // there's only 0–1 card, no horizontal constraint applies.
    //
    // Each tapped card's rotated container is ≈cardHeight (= 1.4 × cardWidth)
    // wide rather than cardWidth. Solving for cardWidth with t tapped out of n:
    //   slotWidth ≥ cw × (n + 0.4·t) + (n − 1) × gap
    //   cw ≤ (slotWidth − (n − 1) × gap) / (n + 0.4·t)
    let widthFromWidth = SLOT_MAX_CARD_WIDTH
    if (maxRowCount > 1) {
      const totalGap = (maxRowCount - 1) * base.cardGap
      const clampedTapped = Math.max(0, Math.min(maxRowTappedCount, maxRowCount))
      const widthDivisor = maxRowCount + 0.4 * clampedTapped
      widthFromWidth = Math.floor((slotSize.width - totalGap) / widthDivisor)
    }

    const slotCardWidth = Math.max(
      60,
      Math.min(SLOT_MAX_CARD_WIDTH, widthFromHeight, widthFromWidth),
    )
    const slotCardHeight = Math.round(slotCardWidth * 1.4)

    // No-op if the resulting size matches what the base context already supplies
    // (within rounding) — avoids creating a fresh ResponsiveSizes identity that
    // would invalidate every downstream useMemo for no visual change.
    if (
      slotCardWidth === base.battlefieldCardWidth &&
      slotCardHeight === base.battlefieldCardHeight
    ) {
      return base
    }

    // Recompute the same badge scale formula useResponsive uses so badges
    // stay proportionate to the (resized) battlefield card.
    const DESKTOP_BF_WIDTH = 125
    const bfScale = Math.max(0.5, Math.min(1.6, slotCardWidth / DESKTOP_BF_WIDTH))
    const scaled = (desktop: number, floor: number) =>
      Math.max(floor, Math.round(desktop * bfScale))
    const badgeInset = scaled(4, 2)
    const badgePadH = scaled(6, 3)
    const badgePadV = scaled(2, 1)
    const tightPadH = scaled(5, 3)
    const tightPadV = scaled(2, 1)
    const badges: BadgeSizes = {
      ptFontSize: scaled(12, 9),
      counterTextFontSize: scaled(11, 8),
      counterIconFontSize: scaled(10, 7),
      keywordIconSize: scaled(18, 12),
      sicknessIconSize: scaled(24, 14),
      smallLabelFontSize: scaled(9, 7),
      manaCostFontSize: scaled(13, 9),
      classLevelMarkerSize: scaled(18, 12),
      classLevelMarkerFontSize: scaled(9, 7),
      countBadgeSize: scaled(22, 16),
      countBadgeFontSize: scaled(12, 9),
      distributeBadgeSize: scaled(26, 18),
      distributeBadgeFontSize: scaled(14, 10),
      indicatorFontSize: scaled(13, 9),
      badgePadding: `${badgePadV}px ${badgePadH}px`,
      badgePaddingTight: `${tightPadV}px ${tightPadH}px`,
      badgeInset,
    }

    return {
      ...base,
      battlefieldCardWidth: slotCardWidth,
      battlefieldCardHeight: slotCardHeight,
      battlefieldRowPadding: Math.round(slotCardHeight * 0.08),
      badges,
    }
  }, [base, slotSize, maxRowCount, maxRowTappedCount])
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
    (a) => a.action.type === 'CastSpell' && a.actionType !== 'CastFaceDown' && a.actionType !== 'CastWithKicker' && a.actionType !== 'CastWithFlashback' && a.actionType !== 'CastWithWarp'
  )
  const hasMorphCast = cardLegalActions.some((a) => a.actionType === 'CastFaceDown')
  const hasKickerCast = cardLegalActions.some((a) => a.actionType === 'CastWithKicker')
  const hasFlashbackCast = cardLegalActions.some((a) => a.actionType === 'CastWithFlashback')
  const hasWarpCast = cardLegalActions.some((a) => a.actionType === 'CastWithWarp')
  const hasCycling = cardLegalActions.some((a) => a.action.type === 'CycleCard')
  const hasPlayLand = cardLegalActions.some((a) => a.action.type === 'PlayLand')

  let options = 0
  if (hasNormalCast) options++
  if (hasMorphCast) options++
  if (hasKickerCast) options++
  if (hasFlashbackCast) options++
  if (hasWarpCast) options++
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
 * Get the number of charge counters on a card.
 */
export function getChargeCounters(card: ClientCard): number {
  return card.counters[CounterType.CHARGE] ?? 0
}

/**
 * Get the number of gem counters on a card.
 */
export function getGemCounters(card: ClientCard): number {
  return card.counters[CounterType.GEM] ?? 0
}

/**
 * Get the number of depletion counters on a card.
 */
export function getDepletionCounters(card: ClientCard): number {
  return card.counters[CounterType.DEPLETION] ?? 0
}

/**
 * Get the number of trap counters on a card.
 */
export function getTrapCounters(card: ClientCard): number {
  return card.counters[CounterType.TRAP] ?? 0
}

/**
 * Get the number of loyalty counters on a card.
 */
export function getLoyaltyCounters(card: ClientCard): number {
  return card.counters[CounterType.LOYALTY] ?? 0
}

/**
 * Get the number of lore counters on a card (for Sagas).
 */
export function getLoreCounters(card: ClientCard): number {
  return card.counters[CounterType.LORE] ?? 0
}

/**
 * Get the number of stun counters on a card.
 */
export function getStunCounters(card: ClientCard): number {
  return card.counters[CounterType.STUN] ?? 0
}

/**
 * Get the number of finality counters on a card.
 */
export function getFinalityCounters(card: ClientCard): number {
  return card.counters[CounterType.FINALITY] ?? 0
}

/**
 * Get the number of supply counters on a card.
 */
export function getSupplyCounters(card: ClientCard): number {
  return card.counters[CounterType.SUPPLY] ?? 0
}

/**
 * Get the number of stash counters on a card.
 */
export function getStashCounters(card: ClientCard): number {
  return card.counters[CounterType.STASH] ?? 0
}

/**
 * Get the number of flying counters on a card.
 */
export function getFlyingCounters(card: ClientCard): number {
  return card.counters[CounterType.FLYING] ?? 0
}

/**
 * Get the number of blight counters on a card.
 */
export function getBlightCounters(card: ClientCard): number {
  return card.counters[CounterType.BLIGHT] ?? 0
}

/**
 * Get the number of flood counters on a card.
 */
export function getFloodCounters(card: ClientCard): number {
  return card.counters[CounterType.FLOOD] ?? 0
}

/**
 * Get the number of coin counters on a card.
 */
export function getCoinCounters(card: ClientCard): number {
  return card.counters[CounterType.COIN] ?? 0
}

/**
 * Get the number of chorus counters on a card.
 */
export function getChorusCounters(card: ClientCard): number {
  return card.counters[CounterType.CHORUS] ?? 0
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
    case 'emblem':
      return '👑'
    case 'copy-spell':
      return '📋'
    case 'triggered-ability':
      return '✨'
    default:
      return '⚡'
  }
}

// --- Token frame color helpers ---

// [top, bottom, textColor] — lighter frame colors closer to real MTG token frames
const COLOR_FRAME: Record<Color, [string, string, string]> = {
  [Color.WHITE]: ['#f5eed8', '#d8cfb0', '#3a3020'],
  [Color.BLUE]:  ['#2a6aaa', '#143860', '#c0d8f0'],
  [Color.BLACK]: ['#48384e', '#201828', '#c8b8d0'],
  [Color.RED]:   ['#b83a20', '#6a1e10', '#ffd0c0'],
  [Color.GREEN]: ['#2a7a3a', '#104a1a', '#c0e8c8'],
}

/** Returns a CSS gradient for a token card frame based on colors. */
export function getTokenFrameGradient(colors: readonly Color[]): string {
  if (colors.length === 0) return 'linear-gradient(180deg, #5a5a6e 0%, #32323e 100%)'
  if (colors.length > 1) return 'linear-gradient(180deg, #d4aa40 0%, #8a6a18 100%)'
  const [light, dark] = COLOR_FRAME[colors[0]!] ?? ['#5a5a6e', '#32323e']
  return `linear-gradient(180deg, ${light} 0%, ${dark} 100%)`
}

/** Returns the text color appropriate for a token frame of the given colors. */
export function getTokenFrameTextColor(colors: readonly Color[]): string {
  if (colors.length === 0) return '#d0d0e0'
  if (colors.length > 1) return '#3a2800'
  const [, , text] = COLOR_FRAME[colors[0]!] ?? [, , '#d0d0e0']
  return text
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
