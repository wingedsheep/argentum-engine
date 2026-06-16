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

// Upper bound on the wrap-line search per battlefield row. Four lines of
// tiny cards per row is the most a phone slot can ever usefully hold; a
// larger bound only adds search work.
const MAX_LINES_PER_ROW = 4

// Preferred readability floor for battlefield cards. When a board is so
// crowded that respecting it would make the layout taller than the slot
// (overlapping the center HUD), cards shrink further — fitting beats size.
const PREFERRED_MIN_CARD_WIDTH = 60

// Below this, cards are unrecognizable; clamp here for pathological boards
// (25+ permanents on a phone). The slot clips its content (Battlefield.tsx),
// so even then nothing can bleed over the center HUD.
const ABSOLUTE_MIN_CARD_WIDTH = 32

// Minimum gap kept toward the center HUD when the comfortable `breathing`
// margin has been sacrificed for card size. Clears the StepStrip's
// active-player chevron (~9px) and its glow, which paints over anything
// closer and reads as cards tucked under the HUD.
const TIGHT_HUD_GAP = 16

/**
 * Slot-derived battlefield layout: the card sizes to render with, plus the
 * number of wrap lines each row was budgeted for (used by Battlefield.tsx to
 * reserve matching minHeight per row so a wrapped row can't collapse or
 * overflow its neighbour).
 */
export interface SlotSizedLayout {
  sizes: ResponsiveSizes
  frontRowLines: number
  backRowLines: number
}

/**
 * Measures the bounded slot a battlefield occupies (set up by the grid in
 * board/styles.ts) and derives card sizes that fit inside it. Cards both
 * shrink (when slot is too small) and grow (when slot has unused height,
 * up to SLOT_MAX_CARD_WIDTH) so the slot is used as fully as possible
 * without overflow.
 *
 * Each row (front: creatures + planeswalkers, back: lands + other) may wrap
 * into multiple physical lines when that yields *larger* cards than squeezing
 * everything onto one line — e.g. many creatures on a wide board, or a narrow
 * portrait phone where vertical space is plentiful and horizontal space isn't.
 * The hook searches line-count combinations (1..MAX_LINES_PER_ROW per row)
 * and picks the one that maximizes card width while the combined height of
 * all lines still fits the slot. Single lines win ties, so roomy boards keep
 * today's flat layout.
 *
 * Counts are *rendered stacks* (after groupCards), not raw cards. The tapped
 * counts matter because tapped cards are rotated 90°: their horizontal
 * footprint is the portrait card *height* (≈1.4× card width) rather than the
 * width. The stackedExtra counts (cards hidden behind a stack's first card)
 * each add a fixed peek offset to their stack's footprint. The width
 * constraint assumes the worst-case line (as many tapped cards as can share a
 * line) so an unplanned extra wrap line — which would overflow the slot
 * vertically into the center HUD — stays geometrically impossible.
 *
 * Phase 2 of the no-overlap layout: makes overflow into the center HUD
 * geometrically impossible by sizing cards from the actual slot rather
 * than estimating from window dimensions.
 */
export function useSlotSizedResponsive(
  slotRef: RefObject<HTMLElement | null>,
  frontRowCount: number = 0,
  frontRowTappedCount: number = 0,
  frontRowStackedExtra: number = 0,
  backRowCount: number = 0,
  backRowTappedCount: number = 0,
  backRowStackedExtra: number = 0,
): SlotSizedLayout {
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
    if (slotSize === null || slotSize.height <= 0 || slotSize.width <= 0) {
      return { sizes: base, frontRowLines: 1, backRowLines: 1 }
    }

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

    // Horizontal-fit cap for one row spread across `lines` wrap lines: the
    // largest card width that lets the fullest line sit side-by-side in the
    // slot's width (accounting for inter-card gaps). With 0–1 cards per line,
    // no horizontal constraint applies.
    //
    // Each tapped stack's rotated container is cardHeight + 8 (= 1.4 ×
    // cardWidth + 8, see GameCard's needsLandscapeContainer) wide rather than
    // cardWidth, and every card stacked behind a group's first peeks out by a
    // fixed offset (see CardStack). Flex-wrap breaks lines greedily, so we
    // can't control which items share a line — assume the worst case where a
    // line holds as many tapped stacks (and all the stacked-extra cards) as
    // possible. Solving for cardWidth with t tapped and e stacked-extra out
    // of n items on a line:
    //   slotWidth ≥ cw × (n + 0.4·t) + 8·t + stackOffset·e + (n − 1) × gap
    //   cw ≤ (slotWidth − 8·t − stackOffset·e − (n − 1) × gap) / (n + 0.4·t)
    const stackOffset = base.isMobile ? 12 : 18
    const widthCapForRow = (count: number, tappedCount: number, stackedExtra: number, lines: number): number => {
      const cardsPerLine = Math.ceil(count / lines)
      if (cardsPerLine <= 1 && stackedExtra <= 0) return SLOT_MAX_CARD_WIDTH
      const tappedOnLine = Math.max(0, Math.min(tappedCount, cardsPerLine))
      const widthDivisor = cardsPerLine + 0.4 * tappedOnLine
      const totalGap = (cardsPerLine - 1) * base.cardGap
      return Math.floor(
        (slotSize.width - totalGap - 8 * tappedOnLine - stackOffset * stackedExtra) / widthDivisor,
      )
    }

    // Search every (frontLines, backLines) combination and keep whichever
    // yields the widest card. More lines relax the horizontal constraint but
    // tighten the vertical one (each line costs cardHeight + a wrap gap), so
    // the optimum depends on slot aspect ratio and card counts. Strict `>`
    // with ascending iteration means fewer lines win ties — a board that fits
    // comfortably on single lines keeps the flat layout.
    //
    // The vertical budget also reserves the two rows' minHeight padding
    // (battlefieldRowPadding = 0.08 × cardHeight each, hence the 0.224·cw
    // term folded into the divisor).
    const maxFrontLines = Math.min(MAX_LINES_PER_ROW, Math.max(1, frontRowCount))
    const maxBackLines = Math.min(MAX_LINES_PER_ROW, Math.max(1, backRowCount))
    const search = (hudGap: number, maxWidth: number) => {
      let width = 0
      let frontLines = 1
      let backLines = 1
      for (let front = 1; front <= maxFrontLines; front++) {
        for (let back = 1; back <= maxBackLines; back++) {
          const totalLines = front + back
          // Vertical-fit cap: every line costs one cardHeight, and wrapped
          // lines within a row are separated by the flex `gap` (cardGap).
          const heightBudget =
            slotSize.height - dividerSpace - hudGap - (totalLines - 2) * base.cardGap
          const widthFromHeight = Math.floor(heightBudget / (totalLines * 1.4 + 0.224))
          const candidate = Math.min(
            maxWidth,
            widthFromHeight,
            widthCapForRow(frontRowCount, frontRowTappedCount, frontRowStackedExtra, front),
            widthCapForRow(backRowCount, backRowTappedCount, backRowStackedExtra, back),
          )
          if (candidate > width) {
            width = candidate
            frontLines = front
            backLines = back
          }
        }
      }
      return { width, frontLines, backLines }
    }

    // Pass 1: comfortable layout — full breathing gap toward the center HUD.
    let { width: slotCardWidth, frontLines: frontRowLines, backLines: backRowLines } =
      search(breathing, SLOT_MAX_CARD_WIDTH)

    if (slotCardWidth < PREFERRED_MIN_CARD_WIDTH) {
      // Crowded board: cards would drop below the preferred readability
      // floor. Re-search with the breathing margin sacrificed (keeping just
      // enough to clear the StepStrip chevron) and the floor as the ceiling —
      // trading the comfort gap for card size, but never letting the layout
      // grow taller than the slot, which is what used to push cards over the
      // center HUD on phones.
      const tight = search(TIGHT_HUD_GAP, PREFERRED_MIN_CARD_WIDTH)
      slotCardWidth = tight.width
      frontRowLines = tight.frontLines
      backRowLines = tight.backLines

      if (slotCardWidth < ABSOLUTE_MIN_CARD_WIDTH) {
        // Even unreadably small cards can't fit this board (20+ permanents on
        // a phone). Clamp to the absolute minimum and re-derive each row's
        // line count from what greedy flex-wrap actually produces at that
        // width, so the minHeight reservations in Battlefield.tsx track
        // reality instead of the impossible plan. Some overflow is now
        // unavoidable.
        slotCardWidth = ABSOLUTE_MIN_CARD_WIDTH
        const linesAtFloor = (count: number, tappedCount: number, stackedExtra: number): number => {
          if (count <= 0) return 1
          const contentWidth =
            count * (slotCardWidth + base.cardGap) +
            tappedCount * (0.4 * slotCardWidth + 8) +
            stackedExtra * stackOffset
          const lineCapacity = slotSize.width + base.cardGap
          return Math.min(
            MAX_LINES_PER_ROW,
            Math.max(1, Math.ceil(contentWidth / lineCapacity)),
          )
        }
        frontRowLines = linesAtFloor(frontRowCount, frontRowTappedCount, frontRowStackedExtra)
        backRowLines = linesAtFloor(backRowCount, backRowTappedCount, backRowStackedExtra)
      }
    }
    const slotCardHeight = Math.round(slotCardWidth * 1.4)

    // No-op if the resulting size matches what the base context already supplies
    // (within rounding) — avoids creating a fresh ResponsiveSizes identity that
    // would invalidate every downstream useMemo for no visual change.
    if (
      slotCardWidth === base.battlefieldCardWidth &&
      slotCardHeight === base.battlefieldCardHeight
    ) {
      return { sizes: base, frontRowLines, backRowLines }
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
      sizes: {
        ...base,
        battlefieldCardWidth: slotCardWidth,
        battlefieldCardHeight: slotCardHeight,
        battlefieldRowPadding: Math.round(slotCardHeight * 0.08),
        badges,
      },
      frontRowLines,
      backRowLines,
    }
  }, [
    base,
    slotSize,
    frontRowCount,
    frontRowTappedCount,
    frontRowStackedExtra,
    backRowCount,
    backRowTappedCount,
    backRowStackedExtra,
  ])
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
  const hasPlot = cardLegalActions.some((a) => a.action.type === 'PlotCard')
  const hasPlayLand = cardLegalActions.some((a) => a.action.type === 'PlayLand')

  let options = 0
  if (hasNormalCast) options++
  if (hasMorphCast) options++
  if (hasKickerCast) options++
  if (hasFlashbackCast) options++
  if (hasWarpCast) options++
  if (hasCycling) options++
  if (hasPlot) options++
  if (hasPlayLand) options++

  return options > 1
}

/**
 * Decide whether dragging a card to play should open the action menu (so the player
 * deliberately picks a casting mode) rather than immediately firing the single available
 * action.
 *
 * The menu must appear whenever a card has more than one way to be played — *even when some
 * of those ways are currently unaffordable*. The server omits an unaffordable normal cast
 * from `legalActions`, so a card you can only afford to cycle arrives with just its
 * `CycleCard` action. But a non-land card that can be cycled/typecycled/plotted always carries
 * an implicit, grayed-out "Cast" option in the menu (see `ActionMenu.buildActionOptions`), and
 * a land that already played a land this turn carries a grayed-out "Play land". Treat both as
 * multi-option so we never silently cycle a card the player might have meant to hard-cast (or
 * cancel). (Whether the grayed-out button reads "Cast" or "Play land" is decided later, by
 * `ActionMenu.buildActionOptions`, from the card's types — it doesn't affect this decision.)
 *
 * @param cardLegalActions Legal actions for this specific card from the server
 */
export function shouldShowCastModal(cardLegalActions: LegalActionInfo[]): boolean {
  if (cardLegalActions.length === 0) return false
  // More than one legal action, or multiple casting variants (morph + normal cast, etc.).
  if (cardLegalActions.length > 1) return true
  if (hasMultipleCastingOptions(cardLegalActions)) return true
  // A lone alternative play mode still implies a second (possibly-unaffordable) option the
  // menu surfaces as a grayed-out button: "Play land" for lands, "Cast" for everything else.
  return cardLegalActions.some(
    (a) =>
      a.action.type === 'CycleCard' ||
      a.action.type === 'TypecycleCard' ||
      a.action.type === 'PlotCard'
  )
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
 * Get the number of first strike counters on a card.
 */
export function getFirstStrikeCounters(card: ClientCard): number {
  return card.counters[CounterType.FIRST_STRIKE] ?? 0
}

/**
 * Get the number of lifelink counters on a card.
 */
export function getLifelinkCounters(card: ClientCard): number {
  return card.counters[CounterType.LIFELINK] ?? 0
}

/**
 * Get the number of reach counters on a card.
 */
export function getReachCounters(card: ClientCard): number {
  return card.counters[CounterType.REACH] ?? 0
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
 * Get the number of dream counters on a card. Dream counters appear on instant
 * and sorcery cards exiled by Goliath Daydreamer's first ability.
 */
export function getDreamCounters(card: ClientCard): number {
  return card.counters[CounterType.DREAM] ?? 0
}

/**
 * Get the number of quest counters on a card. Quest counters appear on
 * enchantments like Beastmaster Ascension that build up toward a threshold.
 */
export function getQuestCounters(card: ClientCard): number {
  return card.counters[CounterType.QUEST] ?? 0
}

/**
 * Get the number of hourglass counters on a card. Hourglass counters (Temporal
 * Distortion) keep a permanent from untapping during its controller's untap step.
 */
export function getHourglassCounters(card: ClientCard): number {
  return card.counters[CounterType.HOURGLASS] ?? 0
}

/**
 * Get the number of growth counters on a card. Growth counters appear on
 * Simic Ascendancy and accumulate toward a 20-counter win condition.
 */
export function getGrowthCounters(card: ClientCard): number {
  return card.counters[CounterType.GROWTH] ?? 0
}

/**
 * Get the number of time counters on a card. Time counters appear on permanents
 * cast for their Impending cost (and Suspend/Vanishing-style mechanics); the
 * permanent isn't a creature until the last one is removed.
 */
export function getTimeCounters(card: ClientCard): number {
  return card.counters[CounterType.TIME] ?? 0
}

/**
 * Get the number of feather counters on a card. Feather counters appear on
 * Soulcatchers' Aerie and accrue when Birds die, boosting Bird creatures.
 */
export function getFeatherCounters(card: ClientCard): number {
  return card.counters[CounterType.FEATHER] ?? 0
}

/**
 * Get the number of decayed counters on a card. A decayed counter (CR 702.147a, TDM)
 * grants the Decayed ability: the creature can't block and is sacrificed at end of combat
 * if it attacks.
 */
export function getDecayedCounters(card: ClientCard): number {
  return card.counters[CounterType.DECAYED] ?? 0
}

/**
 * Get the number of counters of a given type on a card.
 */
export function getCounterCount(card: ClientCard, type: CounterType): number {
  return card.counters[type] ?? 0
}

/**
 * Passive storage counters (hope/verse/influence/burden/loot) — pure marker counters whose only
 * UI is a colored badge with a count. They have no inherent rule and never co-occur on
 * one permanent. Rendered data-driven in GameCard; per-type palette lives in
 * styles.passiveCounterBadgeStyle and icon in counterManaClass.
 */
export const PASSIVE_COUNTER_TYPES: readonly CounterType[] = [
  CounterType.HOPE,
  CounterType.VERSE,
  CounterType.INFLUENCE,
  CounterType.BURDEN,
  CounterType.LOOT,
  CounterType.WIND,
]

/**
 * Get an emoji or icon for an effect based on its icon identifier.
 */
export function getEffectIcon(icon: string): string {
  switch (icon) {
    case 'shield-off':
      return '🛡️'
    case 'shield':
      return '⚡'
    case 'no-counter':
      return '🚫'
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
    case 'granted-ability':
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
