/**
 * Battlefield card sizing — the pure solver behind `useSlotSizedResponsive`
 * (per-slot: one battlefield measuring its own bounded slot) and
 * `usePooledBattlefieldLayout` (two-player: both battlefields solved together
 * over the height the board grid gives them jointly).
 *
 * Plain data in, plain data out — no DOM, no React — so the geometry that used
 * to live inline in the hook is unit-testable and has one home. The design and
 * the worked numbers are in `docs/plans/battlefield-sparse-layout.md`.
 *
 * Each battlefield holds two rows: the front row (creatures + planeswalkers,
 * nearest the center HUD) and the back row (lands + other, at the outer edge).
 * Rows never move; only the card size and the number of wrap lines per row
 * change with what is on the board. The solver picks the largest card width
 * such that every row's fullest wrap line fits the slot's width and all lines,
 * dividers, paddings and the gap toward the HUD fit its height.
 */

export const CARD_ASPECT = 1.4

/**
 * Absolute ceiling on slot-derived card growth, used when a `LayoutEnv` names
 * no `maxCardWidth`. In the app the ceiling is the window-derived base card
 * (`useResponsive`'s `battlefieldCardWidth`, 125 px on desktop — see
 * `layoutEnvFor`): a sparse board reclaims the size the base card would have
 * had, never more. Growing past it made a lone permanent fill the board.
 */
export const SLOT_MAX_CARD_WIDTH = 200

/**
 * Upper bound on the wrap-line search per row. Four lines of tiny cards is the
 * most a phone slot can ever usefully hold; a larger bound only adds work.
 */
export const MAX_LINES_PER_ROW = 4

/**
 * Preferred readability floor. When a board is so crowded that respecting it
 * would make the layout taller than the slot (overlapping the center HUD),
 * cards shrink further — fitting beats size.
 */
export const PREFERRED_MIN_CARD_WIDTH = 60

/**
 * Below this, cards are unrecognizable; clamp here for pathological boards
 * (25+ permanents on a phone). The slot clips its content, so even then
 * nothing bleeds over the center HUD.
 */
export const ABSOLUTE_MIN_CARD_WIDTH = 32

/**
 * Minimum gap kept toward the center HUD once the comfortable breathing margin
 * has been sacrificed for card size. Clears the StepStrip's active-player
 * chevron (~9 px) and its glow.
 */
export const TIGHT_HUD_GAP = 16

/** Height of the radial-gradient strip drawn between the two rows. */
export const DIVIDER_STRIP_HEIGHT = 24

/**
 * Compact spacing, used once the board is crowded enough that size is being
 * traded for fit (the squeezed pass): no divider strip and no row padding, so
 * the rows sit one ordinary line gap apart like wrap lines of one grid. At full
 * size the divider, its margins and the row paddings cost ~45 px — on a 220 px
 * multiplayer cell holding three lines of cards, a fifth of the height.
 */
export const COMPACT_DIVIDER_STRIP_HEIGHT = 0
export const COMPACT_DIVIDER_MARGIN = 4

/**
 * Height an empty row reserves. Zero: the row element stays in the DOM (its
 * order and `data-zone` spotlights are unchanged) but costs no line — the
 * turn-1 board no longer pays a full card height for a creature row with
 * nothing in it.
 */
export const EMPTY_ROW_MIN_HEIGHT = 0

/**
 * Optional second size for the back row (lands + other) relative to the front
 * row: 1 renders both rows at one size; 0.7 renders lands at 70 % so creatures
 * can grow into the reclaimed height. Positions are unaffected either way.
 * Shipped at 1 — a second card size on the board is a taste decision to take
 * from a screenshot walk, not from the solver.
 */
export const BACK_ROW_SCALE = 1

/**
 * Two-player pooled solve: the smaller battlefield's share of the pooled
 * height never drops under this fraction, so the center HUD stays within ~15 %
 * of the middle of the screen however lopsided the boards are.
 */
export const SLOT_SPLIT_MIN = 0.35

/**
 * Extra width GameCard's own wrapper reserves for a card lying sideways on the
 * battlefield (see the `needsLandscapeContainer` branch in GameCard.tsx). Kept
 * in sync with that constant so the fit math reserves the footprint GameCard uses.
 */
export const LANDSCAPE_CONTAINER_PAD = 8

/** Horizontal peek of each card stacked behind a group's first (see `CardStack`). */
export const stackOffsetFor = (isMobile: boolean): number => (isMobile ? 12 : 18)

/** Rendered stacks on one row (after `groupCards`), with the footprint modifiers the fit math needs. */
export interface RowStats {
  /** Stacks on the row. */
  count: number
  /** Stacks whose representative card is tapped — rotated 90°, so ~1.4× as wide. */
  tapped: number
  /** Cards peeking out behind a stack's first card (`visibleStackDepth(n) − 1` summed over stacks). */
  stackedExtra: number
}

export interface BoardStats {
  front: RowStats
  back: RowStats
}

export interface LayoutEnv {
  /** Flex gap between stacks and between wrap lines. */
  cardGap: number
  /** Horizontal peek of each card stacked behind a group's first (see `CardStack`). */
  stackOffset: number
  /** Back-row size relative to the front row; defaults to `BACK_ROW_SCALE`. */
  backRowScale?: number
  /**
   * Largest card the solver may render; defaults to `SLOT_MAX_CARD_WIDTH`.
   * The app passes the window-derived base card so a sparse board grows back
   * up to the ordinary card size and no further.
   */
  maxCardWidth?: number
}

/** The ceiling this environment allows (see `LayoutEnv.maxCardWidth`). */
export const maxCardWidthFor = (env: LayoutEnv): number =>
  Math.max(ABSOLUTE_MIN_CARD_WIDTH, env.maxCardWidth ?? SLOT_MAX_CARD_WIDTH)

export interface SlotLayout {
  /** Front-row card width; the size `ResponsiveContext` carries as `battlefieldCardWidth`. */
  cardWidth: number
  cardHeight: number
  /** Back-row card size — equals the front size unless `backRowScale < 1`. */
  backCardWidth: number
  backCardHeight: number
  /** Wrap lines each row is budgeted for; 0 for an empty row. */
  frontLines: number
  backLines: number
  /** Solved in the squeezed pass: compact divider and no row padding (`dividerFor`, `rowPaddingFor`). */
  compact: boolean
}

export interface PooledLayout {
  /** Common card width both battlefields render at. */
  cardWidth: number
  player: SlotLayout
  opponent: SlotLayout
  /** Slot heights the grid should give each battlefield; they sum to the pooled height. */
  playerHeight: number
  opponentHeight: number
}

/**
 * Transition applied to a battlefield card's box when the solver resizes it —
 * the card's own `transform` / `box-shadow` transitions plus the size axes.
 *
 * The `transform` leg also carries tapping: a tapped permanent turns 90deg and drops onto the
 * row's baseline through this one property, so the curve is tuned for that — most of the turn
 * happens in the first third and then settles, which reads as the card being laid down rather
 * than swept around. Keep it short; a tap is punctuation, not an event.
 */
export const CARD_RESIZE_TRANSITION =
  'transform 0.15s cubic-bezier(0.22, 0.75, 0.3, 1), box-shadow 0.15s, width 0.18s ease, height 0.18s ease'

/** Reduced motion: the turn and the lift snap; only the non-motion box-shadow still eases. */
export const CARD_REDUCED_MOTION_TRANSITION = 'box-shadow 0.15s'

/**
 * Chips drawn inside a card counter-rotate so they stay upright when it taps. They have to turn
 * on exactly the same curve as the card, or they read as spinning independently of it.
 */
export const CARD_COUNTER_ROTATE_TRANSITION = 'transform 0.15s cubic-bezier(0.22, 0.75, 0.3, 1)'

/**
 * Viewer asked for reduced motion — in their OS, or in Preferences → Display (which marks <html>
 * with `data-motion="reduce"`): size changes then snap instead of easing.
 */
export const prefersReducedMotion = (): boolean =>
  typeof window !== 'undefined' &&
  (document.documentElement.dataset.motion === 'reduce' ||
    (typeof window.matchMedia === 'function' && window.matchMedia('(prefers-reduced-motion: reduce)').matches))

export const EMPTY_ROW: RowStats = { count: 0, tapped: 0, stackedExtra: 0 }
export const EMPTY_BOARD: BoardStats = { front: EMPTY_ROW, back: EMPTY_ROW }

const clamp = (v: number, lo: number, hi: number) => Math.max(lo, Math.min(hi, v))

export const cardHeightFor = (cardWidth: number): number => Math.round(cardWidth * CARD_ASPECT)

/**
 * Margin above and below the between-rows divider strip. Scales with the card
 * actually rendered — the old `max(10, 0.1 × baseCardHeight)` was sized for the
 * 125 px desktop base card and cost 36 px around a 92 px-tall card.
 */
export const dividerMarginFor = (cardHeight: number): number => Math.max(6, Math.round(cardHeight * 0.1))

/** Strip height and the margin on each side of it, full size or compact. */
export const dividerFor = (cardHeight: number, compact: boolean): { strip: number; margin: number } =>
  compact
    ? { strip: COMPACT_DIVIDER_STRIP_HEIGHT, margin: COMPACT_DIVIDER_MARGIN }
    : { strip: DIVIDER_STRIP_HEIGHT, margin: dividerMarginFor(cardHeight) }

/** Total height the between-rows divider costs, margins included. */
export const dividerSpaceFor = (cardHeight: number, compact: boolean): number => {
  const { strip, margin } = dividerFor(cardHeight, compact)
  return strip + 2 * margin
}

/**
 * Gap left between the battlefield cards and the center HUD. Scales with the
 * card so a small crowded board doesn't spend up to 48 px of slot on air.
 * Big enough at every size to clear the StepStrip chevron.
 */
export const breathingFor = (cardHeight: number): number => clamp(Math.round(cardHeight * 0.15), 12, 48)

/**
 * Min padding each populated row reserves (the `battlefieldRowPadding` responsive value);
 * none under compact spacing.
 */
export const rowPaddingFor = (cardHeight: number, compact = false): number =>
  compact ? 0 : Math.round(cardHeight * 0.08)

/**
 * Height a row's wrap lines reserve (`minHeight` in Battlefield.tsx): one card
 * height per line, the flex gap between lines, plus the row padding. An empty
 * row keeps only `EMPTY_ROW_MIN_HEIGHT` — it costs no line.
 */
export function rowMinHeightFor(lines: number, cardHeight: number, cardGap: number, compact = false): number {
  if (lines <= 0) return EMPTY_ROW_MIN_HEIGHT
  return lines * cardHeight + (lines - 1) * cardGap + rowPaddingFor(cardHeight, compact)
}

/**
 * Largest card width that lets a row's fullest wrap line sit side-by-side in
 * `slotWidth` (accounting for inter-card gaps). With 0–1 cards per line and no
 * stacked peeks, no horizontal constraint applies.
 *
 * Each tapped stack's rotated container is cardHeight + LANDSCAPE_CONTAINER_PAD
 * (= 1.4 × cardWidth + 8) wide rather than cardWidth, and every card stacked
 * behind a group's first peeks out by `stackOffset`. Flex-wrap breaks lines
 * greedily, so we can't control which items share a line — assume the worst
 * case where a line holds as many tapped stacks (and all the stacked-extra
 * cards) as possible. Solving for cardWidth with t tapped and e stacked-extra
 * out of n items on a line:
 *   slotWidth ≥ cw × (n + 0.4·t) + 8·t + stackOffset·e + (n − 1) × gap
 *   cw ≤ (slotWidth − 8·t − stackOffset·e − (n − 1) × gap) / (n + 0.4·t)
 */
export function rowWidthCap(slotWidth: number, row: RowStats, lines: number, env: LayoutEnv): number {
  if (lines <= 0 || row.count <= 0) return SLOT_MAX_CARD_WIDTH
  const cardsPerLine = Math.ceil(row.count / lines)
  if (cardsPerLine <= 1 && row.stackedExtra <= 0) return SLOT_MAX_CARD_WIDTH
  const tappedOnLine = Math.max(0, Math.min(row.tapped, cardsPerLine))
  const widthDivisor = cardsPerLine + (CARD_ASPECT - 1) * tappedOnLine
  const totalGap = (cardsPerLine - 1) * env.cardGap
  return Math.floor(
    (slotWidth - totalGap - LANDSCAPE_CONTAINER_PAD * tappedOnLine - env.stackOffset * row.stackedExtra) /
      widthDivisor,
  )
}

/**
 * Slot height a board needs at card width `cardWidth` with the given wrap
 * lines: every line costs one card height, wrapped lines are separated by the
 * flex gap, each populated row adds its padding, a divider is paid only when
 * both rows are populated, and a gap toward the HUD is always reserved —
 * `breathingFor` normally, `TIGHT_HUD_GAP` once size has been traded for it.
 */
export function slotHeightNeeded(
  stats: BoardStats,
  frontLines: number,
  backLines: number,
  cardWidth: number,
  env: LayoutEnv,
  tight: boolean,
): number {
  const scale = env.backRowScale ?? BACK_ROW_SCALE
  const h = cardHeightFor(cardWidth)
  const hb = cardHeightFor(cardWidth * scale)
  const fl = stats.front.count > 0 ? frontLines : 0
  const bl = stats.back.count > 0 ? backLines : 0
  let total = 0
  if (fl > 0) total += fl * h + (fl - 1) * env.cardGap + rowPaddingFor(h, tight)
  else total += EMPTY_ROW_MIN_HEIGHT
  if (bl > 0) total += bl * hb + (bl - 1) * env.cardGap + rowPaddingFor(hb, tight)
  else total += EMPTY_ROW_MIN_HEIGHT
  if (fl > 0 && bl > 0) total += dividerSpaceFor(h, tight)
  total += hudGapFor(h, tight)
  return total
}

/**
 * Gap reserved toward the center HUD: the comfortable breathing margin, or —
 * once size is being traded for it — the tight gap, which is never *more* than
 * the comfortable one (small cards already breathe less than `TIGHT_HUD_GAP`).
 */
export const hudGapFor = (cardHeight: number, tight: boolean): number =>
  tight ? Math.min(TIGHT_HUD_GAP, breathingFor(cardHeight)) : breathingFor(cardHeight)

interface LineCandidate {
  frontLines: number
  backLines: number
}

/**
 * Every (frontLines, backLines) combination worth trying, fewest lines first
 * so that a strict "larger card wins" comparison keeps the flat layout on ties.
 * An empty row only ever has 0 lines.
 */
function lineCandidates(stats: BoardStats): LineCandidate[] {
  const options = (row: RowStats): number[] => {
    if (row.count <= 0) return [0]
    const max = Math.min(MAX_LINES_PER_ROW, row.count)
    return Array.from({ length: max }, (_, i) => i + 1)
  }
  const out: LineCandidate[] = []
  for (const frontLines of options(stats.front)) {
    for (const backLines of options(stats.back)) out.push({ frontLines, backLines })
  }
  out.sort((a, b) => a.frontLines + a.backLines - (b.frontLines + b.backLines))
  return out
}

/** Largest width in [ABSOLUTE_MIN_CARD_WIDTH, maxWidth] whose horizontal caps admit it. */
function widthCapFor(slotWidth: number, stats: BoardStats, c: LineCandidate, env: LayoutEnv, maxWidth: number): number {
  const scale = env.backRowScale ?? BACK_ROW_SCALE
  return Math.min(
    maxWidth,
    rowWidthCap(slotWidth, stats.front, c.frontLines, env),
    Math.floor(rowWidthCap(slotWidth, stats.back, c.backLines, env) / scale),
  )
}

/**
 * Largest integer width in [lo, hi] for which `fits` holds, or null. `fits`
 * must be monotone (true for every width below the first true one), which
 * every height budget here is — a bigger card never needs less room.
 */
function largestFitting(lo: number, hi: number, fits: (w: number) => boolean): number | null {
  if (hi < lo || !fits(lo)) return null
  let good = lo
  let bad = hi + 1
  while (bad - good > 1) {
    const mid = (good + bad) >> 1
    if (fits(mid)) good = mid
    else bad = mid
  }
  return good
}

function makeLayout(
  cardWidth: number,
  c: LineCandidate,
  stats: BoardStats,
  env: LayoutEnv,
  compact: boolean,
): SlotLayout {
  const scale = env.backRowScale ?? BACK_ROW_SCALE
  const backCardWidth = Math.round(cardWidth * scale)
  return {
    cardWidth,
    cardHeight: cardHeightFor(cardWidth),
    backCardWidth,
    backCardHeight: cardHeightFor(backCardWidth),
    frontLines: stats.front.count > 0 ? c.frontLines : 0,
    backLines: stats.back.count > 0 ? c.backLines : 0,
    compact,
  }
}

/**
 * Wrap lines greedy flex-wrap actually produces for a row at the floor width,
 * so the row's `minHeight` reservation tracks reality instead of an impossible
 * plan once even unreadably small cards can't fit the board.
 */
function linesAtFloor(slotWidth: number, row: RowStats, env: LayoutEnv, cardWidth: number): number {
  if (row.count <= 0) return 0
  const contentWidth =
    row.count * (cardWidth + env.cardGap) +
    row.tapped * ((CARD_ASPECT - 1) * cardWidth + LANDSCAPE_CONTAINER_PAD) +
    row.stackedExtra * env.stackOffset
  const lineCapacity = slotWidth + env.cardGap
  return Math.min(MAX_LINES_PER_ROW, Math.max(1, Math.ceil(contentWidth / lineCapacity)))
}

function floorLayout(slotWidth: number, stats: BoardStats, env: LayoutEnv): SlotLayout {
  const w = ABSOLUTE_MIN_CARD_WIDTH
  return makeLayout(
    w,
    {
      frontLines: linesAtFloor(slotWidth, stats.front, env, w),
      backLines: linesAtFloor(slotWidth, stats.back, env, Math.round(w * (env.backRowScale ?? BACK_ROW_SCALE))),
    },
    stats,
    env,
    true,
  )
}

/**
 * Card size for one battlefield measuring its own slot (the multiplayer strip
 * cells, and the two-player board before the pooled solve has measurements).
 *
 * Pass 1 keeps the comfortable breathing gap toward the HUD and lets cards grow
 * to the environment's ceiling (`maxCardWidthFor`). If that lands under `PREFERRED_MIN_CARD_WIDTH`, pass
 * 2 trades the breathing gap and the roomy spacing (`dividerFor`, `rowPaddingFor`) for size,
 * with the floor as the ceiling. If even
 * that can't fit, cards clamp to `ABSOLUTE_MIN_CARD_WIDTH` and the line counts
 * follow what greedy wrapping will actually do.
 */
export function solveSlotLayout(slotWidth: number, slotHeight: number, stats: BoardStats, env: LayoutEnv): SlotLayout {
  const candidates = lineCandidates(stats)
  const pass = (tight: boolean, maxWidth: number): SlotLayout | null => {
    let best: SlotLayout | null = null
    for (const c of candidates) {
      const cap = widthCapFor(slotWidth, stats, c, env, maxWidth)
      const w = largestFitting(ABSOLUTE_MIN_CARD_WIDTH, cap, (cw) =>
        slotHeightNeeded(stats, c.frontLines, c.backLines, cw, env, tight) <= slotHeight,
      )
      if (w !== null && (best === null || w > best.cardWidth)) best = makeLayout(w, c, stats, env, tight)
    }
    return best
  }
  const comfortable = pass(false, maxCardWidthFor(env))
  if (comfortable && comfortable.cardWidth >= PREFERRED_MIN_CARD_WIDTH) return comfortable
  const squeezed = pass(true, PREFERRED_MIN_CARD_WIDTH)
  if (squeezed) return squeezed
  return floorLayout(slotWidth, stats, env)
}

/**
 * Two-player solve: one card width for both battlefields, each given the slot
 * height its own rows need out of the height the grid has for the pair. The
 * sparse side hands height to the crowded side; a board that wraps gets its
 * extra line without shrinking below the other player's size. The split is
 * clamped so neither slot drops under `SLOT_SPLIT_MIN` of the pool — that is
 * what keeps the center HUD roughly where it is today.
 *
 * `slotWidth` is the width both battlefield slots share (identical in the
 * two-player grid: same command-zone column and zone-pile column each side).
 */
export function solvePooledLayout(
  slotWidth: number,
  pooledHeight: number,
  player: BoardStats,
  opponent: BoardStats,
  env: LayoutEnv,
): PooledLayout {
  const playerCandidates = lineCandidates(player)
  const opponentCandidates = lineCandidates(opponent)
  const maxSlot = (1 - SLOT_SPLIT_MIN) * pooledHeight

  // Cheapest (least height) line combination whose horizontal caps admit `w`.
  const cheapest = (
    stats: BoardStats,
    candidates: LineCandidate[],
    w: number,
    tight: boolean,
  ): { c: LineCandidate; need: number } | null => {
    let best: { c: LineCandidate; need: number } | null = null
    for (const c of candidates) {
      if (widthCapFor(slotWidth, stats, c, env, maxCardWidthFor(env)) < w) continue
      const need = slotHeightNeeded(stats, c.frontLines, c.backLines, w, env, tight)
      if (best === null || need < best.need) best = { c, need }
    }
    return best
  }

  const pass = (tight: boolean, maxWidth: number): PooledLayout | null => {
    const w = largestFitting(ABSOLUTE_MIN_CARD_WIDTH, maxWidth, (cw) => {
      const a = cheapest(player, playerCandidates, cw, tight)
      const b = cheapest(opponent, opponentCandidates, cw, tight)
      return a !== null && b !== null && a.need + b.need <= pooledHeight && a.need <= maxSlot && b.need <= maxSlot
    })
    if (w === null) return null
    const a = cheapest(player, playerCandidates, w, tight)!
    const b = cheapest(opponent, opponentCandidates, w, tight)!
    return splitHeights(
      w,
      makeLayout(w, a.c, player, env, tight),
      a.need,
      makeLayout(w, b.c, opponent, env, tight),
      b.need,
      pooledHeight,
    )
  }

  const comfortable = pass(false, maxCardWidthFor(env))
  if (comfortable && comfortable.cardWidth >= PREFERRED_MIN_CARD_WIDTH) return comfortable
  const squeezed = pass(true, PREFERRED_MIN_CARD_WIDTH)
  if (squeezed) return squeezed
  // Nothing fits: clamp to the floor and split the pool by what each side needs.
  const a = floorLayout(slotWidth, player, env)
  const b = floorLayout(slotWidth, opponent, env)
  const needA = slotHeightNeeded(player, a.frontLines, a.backLines, a.cardWidth, env, true)
  const needB = slotHeightNeeded(opponent, b.frontLines, b.backLines, b.cardWidth, env, true)
  const share = needA / Math.max(1, needA + needB)
  return {
    cardWidth: ABSOLUTE_MIN_CARD_WIDTH,
    player: a,
    opponent: b,
    playerHeight: pooledHeight * share,
    opponentHeight: pooledHeight * (1 - share),
  }
}

/**
 * Give each side at least what it needs (and at least the clamp share), then
 * hand any leftover out in proportion to need so the crowded side breathes more.
 */
function splitHeights(
  cardWidth: number,
  player: SlotLayout,
  playerNeed: number,
  opponent: SlotLayout,
  opponentNeed: number,
  pooledHeight: number,
): PooledLayout {
  const min = SLOT_SPLIT_MIN * pooledHeight
  let playerHeight = Math.max(playerNeed, min)
  let opponentHeight = Math.max(opponentNeed, min)
  const leftover = pooledHeight - playerHeight - opponentHeight
  if (leftover > 0) {
    const total = Math.max(1, playerNeed + opponentNeed)
    playerHeight += (leftover * playerNeed) / total
    opponentHeight = pooledHeight - playerHeight
  }
  return { cardWidth, player, opponent, playerHeight, opponentHeight }
}
