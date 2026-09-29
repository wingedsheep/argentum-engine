import React, { createContext, useContext, useLayoutEffect, useMemo, useState, type RefObject } from 'react'
import {
  BACK_ROW_SCALE,
  LANDSCAPE_CONTAINER_PAD,
  cardHeightFor,
  rowPaddingFor,
  solveSlotLayout,
  stackOffsetFor,
  type BoardStats,
  type LayoutEnv,
  type PooledLayout,
  type SlotLayout,
} from './battlefieldLayout'
import type { ResponsiveSizes, BadgeSizes } from '../../../hooks/useResponsive'
import { getScryfallFallbackUrl } from '../../../utils/cardImages'
import { keywordAlternativeCostFor } from '../../../utils/actionOptions'
import type { ClientCard, LegalActionInfo } from '../../../types'
import { CounterType, CounterTypeDisplayNames } from '../../../types'
import { Color } from '../../../types/enums'

// Context to pass responsive sizes down the component tree
export const ResponsiveContext = createContext<ResponsiveSizes | null>(null)

export function useResponsiveContext(): ResponsiveSizes {
  const ctx = useContext(ResponsiveContext)
  if (!ctx) throw new Error('ResponsiveContext not provided')
  return ctx
}

/**
 * Two-player pooled layout, provided by `GameBoard` once it has measured the
 * height the grid gives both battlefields jointly (`usePooledBattlefieldLayout`).
 * Each `Battlefield` reads its own side and skips its per-slot solve; `null`
 * (multiplayer strips, spectator bottom seats, or before the first measurement)
 * means "size yourself from your own slot".
 */
export const PooledBattlefieldLayoutContext = createContext<PooledLayout | null>(null)

/** The solver inputs that come from the responsive base (gap and stack peek). */
export function layoutEnvFor(base: ResponsiveSizes): LayoutEnv {
  return {
    cardGap: base.cardGap,
    stackOffset: stackOffsetFor(base.isMobile),
    backRowScale: BACK_ROW_SCALE,
    // A sparse board grows cards back up to the ordinary window-derived size,
    // never past it — one permanent should not fill the board.
    maxCardWidth: base.battlefieldCardWidth,
  }
}

/**
 * The base responsive sizes with the battlefield card (and everything that
 * scales with it — row padding, badges) replaced for `cardWidth`. Returns
 * `base` itself when nothing would change, so downstream useMemos keyed on the
 * sizes identity don't invalidate for no visual change.
 */
/**
 * Per-base cache of derived sizes, so the same width always yields the same object. The sizes are
 * a context value every battlefield card reads: without the cache, anything that re-runs the slot
 * solve — tapping a land changes the row stats — hands every card a fresh-but-equal context
 * value and re-renders the whole board even though no card changed size.
 */
const sizesCache = new WeakMap<ResponsiveSizes, Map<number, ResponsiveSizes>>()
const SIZES_CACHE_LIMIT = 64

export function sizesForCardWidth(base: ResponsiveSizes, cardWidth: number): ResponsiveSizes {
  let byWidth = sizesCache.get(base)
  const cached = byWidth?.get(cardWidth)
  if (cached) return cached
  const sizes = computeSizesForCardWidth(base, cardWidth)
  if (!byWidth) {
    byWidth = new Map()
    sizesCache.set(base, byWidth)
  }
  // Widths come from a continuous solve; a live resize drag can mint many. Keep the cache bounded.
  if (byWidth.size >= SIZES_CACHE_LIMIT) byWidth.clear()
  byWidth.set(cardWidth, sizes)
  return sizes
}

function computeSizesForCardWidth(base: ResponsiveSizes, cardWidth: number): ResponsiveSizes {
  const cardHeight = cardHeightFor(cardWidth)
  if (cardWidth === base.battlefieldCardWidth && cardHeight === base.battlefieldCardHeight) return base

  // Recompute the same badge scale formula useResponsive uses so badges
  // stay proportionate to the (resized) battlefield card.
  const DESKTOP_BF_WIDTH = 125
  const bfScale = Math.max(0.5, Math.min(1.6, cardWidth / DESKTOP_BF_WIDTH))
  const scaled = (desktop: number, floor: number) => Math.max(floor, Math.round(desktop * bfScale))
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
    battlefieldCardWidth: cardWidth,
    battlefieldCardHeight: cardHeight,
    battlefieldRowPadding: rowPaddingFor(cardHeight),
    badges,
  }
}

/**
 * Slot-derived battlefield layout: the card sizes to render with (front row,
 * and the back row — the same object unless `BACK_ROW_SCALE < 1`), plus the
 * number of wrap lines each row was budgeted for (used by Battlefield.tsx to
 * reserve matching minHeight per row so a wrapped row can't collapse or
 * overflow its neighbour; 0 for an empty row).
 */
export interface SlotSizedLayout {
  sizes: ResponsiveSizes
  backSizes: ResponsiveSizes
  frontRowLines: number
  backRowLines: number
  /** The solve traded the roomy row spacing for card size (see `SlotLayout.compact`). */
  compact: boolean
}

/**
 * Measures the bounded slot a battlefield occupies (set up by the grid in
 * board/styles.ts) and derives card sizes that fit inside it via
 * `solveSlotLayout`. Cards both shrink (when the slot is too small) and grow
 * (when it has unused height, up to SLOT_MAX_CARD_WIDTH) so the slot is used
 * as fully as possible without overflow — an empty row costs no line, and the
 * divider margins and the gap toward the HUD scale with the card rendered
 * rather than with the desktop base card.
 *
 * When `pooled` is given (the two-player board, solved jointly by GameBoard so
 * both players share one width and the crowded side gets the height it needs)
 * the slot measurement is ignored and the pooled result is rendered as-is.
 *
 * Phase 2 of the no-overlap layout: makes overflow into the center HUD
 * geometrically impossible by sizing cards from the actual slot rather
 * than estimating from window dimensions.
 */
export function useSlotSizedResponsive(
  slotRef: RefObject<HTMLElement | null>,
  stats: BoardStats,
  pooled: SlotLayout | null = null,
): SlotSizedLayout {
  const base = useResponsiveContext()
  const [slotSize, setSlotSize] = useState<{ width: number; height: number } | null>(null)

  useLayoutEffect(() => {
    const node = slotRef.current
    if (!node) return
    // Whole pixels, and no state change for a same-size report: fractional
    // ResizeObserver readings would otherwise re-solve (and re-render) on every
    // sub-pixel wobble of the grid rows.
    const update = (width: number, height: number) =>
      setSlotSize((prev) => {
        const next = { width: Math.round(width), height: Math.round(height) }
        return prev !== null && prev.width === next.width && prev.height === next.height ? prev : next
      })
    const rect = node.getBoundingClientRect()
    update(rect.width, rect.height)
    const obs = new ResizeObserver((entries) => {
      const entry = entries[0]
      if (entry) update(entry.contentRect.width, entry.contentRect.height)
    })
    obs.observe(node)
    return () => obs.disconnect()
  }, [slotRef])

  const { front, back } = stats
  return useMemo(() => {
    const own =
      slotSize !== null && slotSize.height > 0 && slotSize.width > 0
        ? solveSlotLayout(slotSize.width, slotSize.height, { front, back }, layoutEnvFor(base))
        : null
    // The pooled width was solved from the rows' measured heights, but this
    // slot's own measurement can lag a frame behind a grid change — and
    // whatever the source, a card that doesn't fit the slot it is in would be
    // clipped against the center HUD. Never render wider than the slot fits;
    // in a consistent frame the two agree and the pooled width wins as-is.
    const layout = pooled !== null && own !== null && own.cardWidth < pooled.cardWidth ? own : (pooled ?? own)
    if (layout === null) {
      return {
        sizes: base,
        backSizes: base,
        frontRowLines: front.count > 0 ? 1 : 0,
        backRowLines: back.count > 0 ? 1 : 0,
        compact: false,
      }
    }
    const sizes = sizesForCardWidth(base, layout.cardWidth)
    const backSizes = layout.backCardWidth === layout.cardWidth ? sizes : sizesForCardWidth(base, layout.backCardWidth)
    return {
      sizes,
      backSizes,
      frontRowLines: layout.frontLines,
      backRowLines: layout.backLines,
      compact: layout.compact,
    }
    // Keyed on the stats' numbers, not the objects, so an unrelated store
    // update that rebuilds equal stats doesn't produce a fresh sizes identity.
  }, [base, slotSize, pooled, front.count, front.tapped, front.stackedExtra, back.count, back.tapped, back.stackedExtra])
}


/** Placement of one card inside an attachment stack, in container-local pixels. */
export interface AttachmentStackBox {
  left: number
  top: number
  /** Footprint GameCard reserves at this card's current orientation. */
  width: number
}

export interface AttachmentStackLayout {
  containerWidth: number
  containerHeight: number
  /** Peeking attachments in render order; index 0 peeks furthest out from behind the host. */
  attachments: AttachmentStackBox[]
  host: AttachmentStackBox
  /** Left edge of the upright card column — the count pill and click-catcher align to it. */
  columnLeft: number
}

/**
 * Geometry for a permanent rendered with its attachments peeking out from behind it.
 *
 * Every card is placed in its own box and rotates *itself* when tapped, so a card's
 * orientation depends only on its own tap state. That matters for rules clarity: an
 * Equipment and its equipped creature are independent permanents (CR 301.5d), so
 * tapping the creature must not make the still-untapped Equipment look tapped — and
 * a tapped Equipment must read as tapped even while its host is untapped.
 *
 * Boxes are horizontally centered on a shared axis, so a card's *visual* center is
 * the same whichever way it faces. The host's box sits flush with the container
 * bottom; rows are bottom-aligned (`alignItems: flex-end`), which keeps the host on
 * the same baseline as an unattached permanent whether or not it's tapped.
 *
 * The two orientations peek from opposite ends. An *upright* attachment peeks above
 * the host, where its title bar is what shows. A *sideways* one is already wider than
 * the host, so it shows down both flanks on its own — it bottom-aligns instead, tucking
 * under the host rather than riding above it, and claims no peek height of its own.
 */
export function attachmentStackLayout(input: {
  cardWidth: number
  cardHeight: number
  /** How much of each attachment shows above the card in front of it. */
  peek: number
  hostTapped: boolean
  /** Tap state per peeking attachment, in render order. */
  attachmentsTapped: readonly boolean[]
  /** Breathing room reserved beside a sideways card so it doesn't sit flush against neighbours. */
  gutter: number
}): AttachmentStackLayout {
  const { cardWidth, cardHeight, peek, hostTapped, attachmentsTapped, gutter } = input
  // A sideways card is as wide as an upright one is tall.
  const boxWidth = (tapped: boolean) => (tapped ? cardHeight + LANDSCAPE_CONTAINER_PAD : cardWidth)

  // Only upright attachments claim peek height above the host; sideways ones tuck under it.
  const visiblePeek = attachmentsTapped.filter((tapped) => !tapped).length * peek
  const anySideways = hostTapped || attachmentsTapped.some(Boolean)
  const containerWidth = (anySideways ? cardHeight + LANDSCAPE_CONTAINER_PAD : cardWidth) +
    (anySideways ? gutter : 0)
  const containerHeight = cardHeight + visiblePeek
  const centeredLeft = (tapped: boolean) => (containerWidth - boxWidth(tapped)) / 2
  // GameCard drops a sideways card's visible band onto the bottom of its own box, so a box
  // flush with the container bottom puts the band level with the host's lower edge — the
  // same placement the host itself gets.
  const bottomAlignedTop = containerHeight - cardHeight

  let uprightRung = 0
  return {
    containerWidth,
    containerHeight,
    columnLeft: (containerWidth - cardWidth) / 2,
    attachments: attachmentsTapped.map((tapped) => ({
      left: centeredLeft(tapped),
      top: tapped ? bottomAlignedTop : uprightRung++ * peek,
      width: boxWidth(tapped),
    })),
    host: {
      left: centeredLeft(hostTapped),
      top: visiblePeek,
      width: boxWidth(hostTapped),
    },
  }
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
    (a) => a.action.type === 'CastSpell' && a.actionType !== 'CastFaceDown' && a.actionType !== 'CastWithKicker' && a.actionType !== 'CastWithFlashback' && a.actionType !== 'CastWithEscape' && a.actionType !== 'CastWithWarp' && a.actionType !== 'CastWithDash' && a.actionType !== 'CastWithDisturb'
  )
  const hasMorphCast = cardLegalActions.some((a) => a.actionType === 'CastFaceDown')
  const hasKickerCast = cardLegalActions.some((a) => a.actionType === 'CastWithKicker')
  const hasFlashbackCast = cardLegalActions.some((a) => a.actionType === 'CastWithFlashback')
  const hasEscapeCast = cardLegalActions.some((a) => a.actionType === 'CastWithEscape')
  const hasWarpCast = cardLegalActions.some((a) => a.actionType === 'CastWithWarp')
  const hasDashCast = cardLegalActions.some((a) => a.actionType === 'CastWithDash')
  // Disturb (CR 702.146) casts the card's back face from the graveyard, so it is a distinct
  // casting option from any normal cast of the same card.
  const hasDisturbCast = cardLegalActions.some((a) => a.actionType === 'CastWithDisturb')
  const hasCycling = cardLegalActions.some((a) => a.action.type === 'CycleCard')
  const hasPlot = cardLegalActions.some((a) => a.action.type === 'PlotCard')
  const hasSuspend = cardLegalActions.some((a) => a.action.type === 'SuspendCardFromHand')
  // Counted rather than flagged: a modal double-faced land offers one PlayLand per land face
  // (CR 712.12), and two faces are two options even though there is no cast among them.
  const playLandCount = cardLegalActions.filter((a) => a.action.type === 'PlayLand').length

  let options = 0
  if (hasNormalCast) options++
  if (hasMorphCast) options++
  if (hasKickerCast) options++
  if (hasFlashbackCast) options++
  if (hasEscapeCast) options++
  if (hasWarpCast) options++
  if (hasDashCast) options++
  if (hasDisturbCast) options++
  if (hasCycling) options++
  if (hasPlot) options++
  if (hasSuspend) options++
  options += playLandCount

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
 * A keyword alternative cost (impending, evoke) is the same situation reached from the other side:
 * the card always has two prices, but the server enumerates only the affordable one, so a single
 * `CastSpell` action here can still be one of two buttons the menu is about to draw. Evoke is the
 * case that makes it bite — dragging out a Mulldrifter you can only afford to evoke used to cast
 * it for its evoke cost on the spot, sacrificing the creature with no choice offered.
 *
 * @param cardLegalActions Legal actions for this specific card from the server
 * @param cardInfo The card itself, when known — carries the keyword alternative costs that never
 *   appear in `cardLegalActions` because the player can't currently pay for them
 */
export function shouldShowCastModal(
  cardLegalActions: LegalActionInfo[],
  cardInfo?: ClientCard | null
): boolean {
  if (cardLegalActions.length === 0) return false
  // More than one legal action, or multiple casting variants (morph + normal cast, etc.).
  if (cardLegalActions.length > 1) return true
  if (hasMultipleCastingOptions(cardLegalActions)) return true
  // Impending / evoke: the printed cost and the keyword cost are both real prices for this card,
  // so any cast of it is a choice between two buttons — whichever one the server could afford.
  if (
    cardInfo &&
    keywordAlternativeCostFor(cardInfo) &&
    cardLegalActions.some((a) => a.action.type === 'CastSpell')
  ) {
    return true
  }
  // A lone alternative play mode still implies a second (possibly-unaffordable) option the
  // menu surfaces as a grayed-out button: "Play land" for lands, "Cast" for everything else.
  return cardLegalActions.some(
    (a) =>
      a.action.type === 'CycleCard' ||
      a.action.type === 'TypecycleCard' ||
      a.action.type === 'PlotCard' ||
      a.action.type === 'SuspendCardFromHand'
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
 * Get the number of double strike counters on a card.
 */
export function getDoubleStrikeCounters(card: ClientCard): number {
  return card.counters[CounterType.DOUBLE_STRIKE] ?? 0
}

/**
 * Get the number of vigilance counters on a card.
 */
export function getVigilanceCounters(card: ClientCard): number {
  return card.counters[CounterType.VIGILANCE] ?? 0
}

/**
 * Get the number of deathtouch counters on a card.
 */
export function getDeathtouchCounters(card: ClientCard): number {
  return card.counters[CounterType.DEATHTOUCH] ?? 0
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
 * Get the number of shield counters on a card (CR 122.1c). One or more shield counters prevent the
 * next damage dealt to the permanent, or replace the next destruction by an effect, consuming one
 * counter each time.
 */
export function getShieldCounters(card: ClientCard): number {
  return card.counters[CounterType.SHIELD] ?? 0
}

/**
 * Get the number of haste counters on a card. Keyword counter (CR 122.1b) — the permanent has haste
 * for as long as it has one.
 */
export function getHasteCounters(card: ClientCard): number {
  return card.counters[CounterType.HASTE] ?? 0
}

/**
 * Get the number of menace counters on a card. Keyword counter (CR 122.1b) — the permanent has
 * menace for as long as it has one.
 */
export function getMenaceCounters(card: ClientCard): number {
  return card.counters[CounterType.MENACE] ?? 0
}

/**
 * Get the number of counters of a given type on a card.
 */
export function getCounterCount(card: ClientCard, type: CounterType): number {
  return card.counters[type] ?? 0
}

/** One counter type present on a card, ready to render: engine type, player-facing label, count. */
export interface CardCounterEntry {
  readonly type: CounterType
  readonly label: string
  readonly count: number
}

/**
 * Every counter type currently on [card], with its display label and count.
 *
 * Deliberately driven by the counters the server actually sent rather than by a curated list of
 * types the client knows how to badge: the battlefield badges are an allowlist, so a counter with
 * no badge (storage on City of Shadows, hunger on Fasting) was invisible everywhere. This is the
 * complete inventory, which is what the card preview shows.
 *
 * A type missing from `CounterTypeDisplayNames` still renders — its enum name is title-cased —
 * so a counter added to the engine before the client mirror catches up degrades to a readable
 * label instead of disappearing. `CounterTypeClientMirrorTest.kt` (mtg-sdk) keeps the mirror honest
 * separately — it reads `enums.ts` and fails when it drifts from the engine enum.
 *
 * Sorted by count descending, then label, so the biggest pile reads first and the order is stable.
 */
export function listCardCounters(card: ClientCard): CardCounterEntry[] {
  return Object.entries(card.counters)
    .filter(([, count]) => (count ?? 0) > 0)
    .map(([type, count]) => ({
      type: type as CounterType,
      label: CounterTypeDisplayNames[type as CounterType] ?? titleCaseCounterName(type),
      count: count as number,
    }))
    .sort((a, b) => b.count - a.count || a.label.localeCompare(b.label))
}

/** `SOME_COUNTER` → `Some counter`, for a counter type the client mirror doesn't name yet. */
function titleCaseCounterName(raw: string): string {
  const words = raw.toLowerCase().split('_').join(' ')
  return words.charAt(0).toUpperCase() + words.slice(1)
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
  CounterType.NEST,
  CounterType.PAGE,
  CounterType.HOOFPRINT,
  CounterType.MANNEQUIN,
  CounterType.REV,
  CounterType.BLOODSTAIN,
  CounterType.BLOOD,
  CounterType.SOUL,
  CounterType.DIVINITY,
  CounterType.POSSESSION,
  CounterType.LANDMARK,
  CounterType.DREAD,
  CounterType.INCUBATION,
  CounterType.FELLOWSHIP,
  CounterType.BAIT,
  CounterType.BORE,
  CounterType.POINT,
  CounterType.WISH,
  CounterType.REVIVAL,
  CounterType.INGENUITY,
  CounterType.FILM,
  CounterType.ICE,
  CounterType.OMEN,
  CounterType.HARNESS,
  CounterType.PLAN,
  CounterType.INVASION,
  CounterType.UNLOCK,
  CounterType.HONE,
  // Counter types with live cards that rendered no badge at all until now: storage (City of
  // Shadows), hunger (Fasting), doom, fire, conqueror, net, silver, fate (Oblivion Stone), aim,
  // spore (the Fungus/Thallid mechanic).
  //
  // CounterType.DEFENSE is deliberately absent. It is the battle analogue of loyalty (CR 310.4c) —
  // a number the permanent is defined by, not a marker sitting on it — so GameCard draws it as the
  // battle's own defense badge over the printed shield, not in this marker-badge allowlist.
  CounterType.STORAGE,
  CounterType.HUNGER,
  // Phyrexia: All Will Be One's oil — spent, counted and proliferated by the cards that care.
  CounterType.OIL,
  CounterType.DOOM,
  CounterType.FIRE,
  CounterType.CONQUEROR,
  CounterType.NET,
  CounterType.SILVER,
  CounterType.FATE,
  CounterType.AIM,
  CounterType.SPORE,
  CounterType.PLUS_ONE_PLUS_ZERO,
  CounterType.PLUS_ZERO_PLUS_ONE,
  CounterType.PLUS_TWO_PLUS_ZERO,
  CounterType.PLUS_ZERO_PLUS_TWO,
  CounterType.MINUS_ONE_MINUS_ZERO,
  CounterType.MINUS_ZERO_MINUS_ONE,
  // Fallen Empires. Tide is the one that most needs a badge: Homarid's whole clock is the exact
  // count, switching at one and at three and shedding all at four, so a player who can't read it
  // off the board can't play the card.
  CounterType.TIDE,
  CounterType.JAVELIN,
  CounterType.CREDIT,
  CounterType.CUBE,
  // Innistrad: Crimson Vow. Faithbound Judge // Sinner's Judgment counts to three on both
  // faces, and on the Aura face the third counter *ends the game* — a tally a player has to be
  // able to read off the board.
  CounterType.JUDGMENT,
  // Innistrad: Crimson Vow tallies that transform their permanent at three (Edgar Markov's Coffin,
  // Wedding Announcement), and Illicit Masquerade's impostor marker, which decides what it exiles.
  CounterType.BLOODLINE,
  CounterType.INVITATION,
  CounterType.IMPOSTOR,
  // Champions of Kamigawa. Bloodthirsty Ogre's devotion tally is the size of its -X/-X.
  CounterType.DEVOTION,
  // Night Dealings' theft tally is spent as X to tutor a card.
  CounterType.THEFT,
  // Sensei Golden-Tail's training marker records which creatures it trained.
  CounterType.TRAINING,
  CounterType.PLUS_ONE_PLUS_TWO,
  CounterType.PLUS_TWO_PLUS_TWO,
  CounterType.MINUS_TWO_MINUS_TWO,
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
    case 'double-damage':
      return '🔥'
    case 'regeneration':
      return '♻️'
    case 'emblem':
      return '👑'
    case 'copy-spell':
      return '📋'
    case 'free-cast':
      return '🆓'
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
