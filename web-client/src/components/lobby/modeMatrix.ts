/**
 * What you can actually play, before any lobby exists.
 *
 * The pre-lobby twin of `axisChoices.ts`. Both answer "can I have this combination?"; they differ in
 * where they start. `axisChoices` is asked from inside a lobby that is already backed by one of the
 * two server implementations, so its third answer is `RECREATE` — tear this one down and stand the
 * other one up. Here nothing exists yet, so every reachable combination is simply created correctly
 * the first time, and the only answers are *yes* and *not implemented*.
 *
 * That is the whole reason the landing screen asks its three questions before creating anything: the
 * six preset cards it replaces (see `backlog/menu-lobby-restructure-and-help.md` § 3a) committed to a
 * lobby kind on the first click, so "vs Friend, actually let's do Free-for-All" cost a recreate.
 *
 * ## The three questions
 *
 * 1. **Roster** — who fills the seats. Not one of the three axes and deliberately so: seats are per
 *    seat, and any Cards × Table × Event point could in principle be played by any roster. It leads
 *    because it prunes hardest and because it is the one thing a player has already decided before
 *    opening the app.
 * 2. **Cards** — where the deck comes from (`axes.ts`).
 * 3. **Shape** — Table × Event as one list of named shapes rather than two rows. Only five of the
 *    eight combinations exist, so two rows would mean three dead cells; the lobby keeps the axes
 *    separate because there its job is editing, not choosing.
 *
 * ## Disabled versus absent
 *
 * The distinction is load-bearing, and it is the same one the lobby draws:
 *
 * - **Disabled with a reason** — nothing implements this yet. Every one of these is a gap in § 4c of
 *   the plan, and this is the surface where the player meets it while asking the question rather
 *   than after committing to a lobby.
 * - **Absent** — contradicts an answer already given. A group of five is not shown a disabled "1v1,
 *   one game"; they said there were five of them. Rendering that as a limitation would teach
 *   something false about the system.
 */
import type { GameRules, LobbyGameMode, TournamentFormat } from '@/types'
import {
  cardsKindLabel,
  cardsLabel,
  cardsSeatCap,
  rulesForCards,
  rulesTableBlock,
  tournamentFormatForCards,
  gameModeForTable,
  type CardsAxis,
  type CardsKind,
  type EventAxis,
  type TableAxis,
} from './axes'
import type { DeckPickerTab } from '../ui/DeckPicker'

/* ── Vocabulary ─────────────────────────────────────────────────────────── */

/** Who fills the seats. */
export type Roster =
  /** You and the built-in AI. */
  | 'SOLO'
  /** One human opponent, reached with an invite code. */
  | 'FRIEND'
  /** Three or more players. */
  | 'GROUP'

export const ROSTERS: readonly Roster[] = ['SOLO', 'FRIEND', 'GROUP']

/** A reachable Table × Event pair, named the way a player would describe it. */
export type ShapeId = 'ONE_GAME' | 'BRACKET' | 'FREE_FOR_ALL' | 'TWO_HEADED_GIANT' | 'TEAM_VS_TEAM'

const SHAPE_AXES: Record<ShapeId, { table: TableAxis; event: EventAxis }> = {
  ONE_GAME: { table: 'ONE_V_ONE', event: 'SINGLE_GAME' },
  BRACKET: { table: 'ONE_V_ONE', event: 'ROUND_ROBIN' },
  FREE_FOR_ALL: { table: 'FREE_FOR_ALL', event: 'SINGLE_GAME' },
  TWO_HEADED_GIANT: { table: 'TWO_HEADED_GIANT', event: 'SINGLE_GAME' },
  TEAM_VS_TEAM: { table: 'TEAM_VS_TEAM', event: 'SINGLE_GAME' },
}

/** The closed domain, for the same reason `CARDS_KINDS` / `TABLE_VALUES` / `ROSTERS` exist: so a
 *  test can walk every value rather than restate the list and drift from it. */
export const SHAPE_IDS: readonly ShapeId[] = Object.keys(SHAPE_AXES) as ShapeId[]

export function shapeAxes(shape: ShapeId): { table: TableAxis; event: EventAxis } {
  return SHAPE_AXES[shape]
}

/**
 * The inverse: which named shape a live lobby's Table and Event add up to.
 *
 * Needed to read a *selection* back out of a lobby that already exists — capturing a recipe, which
 * is the one direction the wizard never had to go. Only the 1v1 table has two shapes to tell apart;
 * every multiplayer table plays exactly one game, so `eventFromGameMode` derives `SINGLE_GAME` for
 * all of them and the Event half carries no information there.
 */
export function shapeFromAxes(table: TableAxis, event: EventAxis): ShapeId {
  switch (table) {
    case 'FREE_FOR_ALL': return 'FREE_FOR_ALL'
    case 'TWO_HEADED_GIANT': return 'TWO_HEADED_GIANT'
    case 'TEAM_VS_TEAM': return 'TEAM_VS_TEAM'
    case 'ONE_V_ONE': return event === 'ROUND_ROBIN' ? 'BRACKET' : 'ONE_GAME'
  }
}

export function rosterLabel(roster: Roster): string {
  switch (roster) {
    case 'SOLO': return 'Just me'
    case 'FRIEND': return 'A friend'
    case 'GROUP': return 'A group'
  }
}

export function rosterCaption(roster: Roster): string {
  switch (roster) {
    case 'SOLO': return 'You and the built-in AI. Nobody else has to show up.'
    case 'FRIEND': return 'One opponent. You get an invite code to share.'
    case 'GROUP': return 'Three to eight players, at one table or in a bracket.'
  }
}

export function rosterTopicId(roster: Roster): string {
  switch (roster) {
    case 'SOLO': return 'roster-solo'
    case 'FRIEND': return 'roster-friend'
    case 'GROUP': return 'roster-group'
  }
}

export function shapeLabel(shape: ShapeId): string {
  switch (shape) {
    case 'ONE_GAME': return 'One game'
    case 'BRACKET': return 'Round-robin bracket'
    case 'FREE_FOR_ALL': return 'Free-for-All'
    case 'TWO_HEADED_GIANT': return 'Two-Headed Giant'
    case 'TEAM_VS_TEAM': return 'Team vs. Team'
  }
}

export function shapeCaption(shape: ShapeId): string {
  switch (shape) {
    case 'ONE_GAME': return 'A single 1v1 game. Play again afterwards if you want another.'
    case 'BRACKET': return 'Everyone plays everyone, with standings. 1v1 matches.'
    case 'FREE_FOR_ALL': return 'One shared game, every player for themselves (CR 802/803).'
    case 'TWO_HEADED_GIANT': return 'Two teams of two sharing 30 life, turns and combat (CR 810).'
    case 'TEAM_VS_TEAM': return 'Teams with their own life totals and turns (CR 808).'
  }
}

export function shapeTopicId(shape: ShapeId): string {
  switch (shape) {
    case 'ONE_GAME': return 'event-single-game'
    case 'BRACKET': return 'event-round-robin'
    case 'FREE_FOR_ALL': return 'table-free-for-all'
    case 'TWO_HEADED_GIANT': return 'table-two-headed-giant'
    case 'TEAM_VS_TEAM': return 'table-team-vs-team'
  }
}

/**
 * A tile's badge: how much of a *commitment* this option is.
 *
 * The thing a newcomer most wants to know before clicking is whether they are about to play a game
 * or start an event — open packs, build a deck, then several rounds and a standings table. That is
 * not derivable from a name like "Sealed", so every Cards and Shape tile says which it is.
 */
export type ChoiceWeight =
  /** Straight into a game. */
  | 'QUICK'
  /** There is a step before you play, or more than one game after. */
  | 'EVENT'

/** One option in a wizard step. `disabledReason` present ⇒ nothing implements it yet. */
export interface Choice<V> {
  value: V
  label: string
  caption: string
  topicId: string
  /** Short badge on the tile. Omitted where the distinction doesn't apply (the roster step). */
  badge?: { text: string; weight: ChoiceWeight }
  disabledReason?: string
}

/* ── The reasons ────────────────────────────────────────────────────────────
 * Each of these is a Phase 5 gap (§ 4c), phrased for someone who has not yet created anything —
 * which is why they say "go back and pick X" rather than the lobby's "switch the Table first".
 * ─────────────────────────────────────────────────────────────────────────── */

const MOMIR_IS_A_1V1_SINGLE_GAME =
  'Momir Basic only exists as a 1v1 single game — it has no bracket or multiplayer implementation.'

const RANDOM_IS_A_1V1_SINGLE_GAME =
  'A rolled random pool only exists on the two-seat lobby that plays one game.'

const LIMITED_ALWAYS_RUNS_AS_A_BRACKET =
  'A limited pool always runs as a bracket at a 1v1 table — the pool is meant to be played more than once. With two players and one game per matchup that is a single game anyway.'

/** The AI is off on this server entirely. */
export const AI_DISABLED_ON_SERVER = 'The AI player is disabled on this server.'

/* ── Step 1: roster ─────────────────────────────────────────────────────── */

export function rosterChoices(aiEnabled: boolean): Choice<Roster>[] {
  return ROSTERS.map((roster) => ({
    value: roster,
    label: rosterLabel(roster),
    caption: rosterCaption(roster),
    topicId: rosterTopicId(roster),
    ...(roster === 'SOLO' && !aiEnabled ? { disabledReason: AI_DISABLED_ON_SERVER } : {}),
  }))
}

/* ── Step 2: cards ──────────────────────────────────────────────────────── */

/** The order the five Cards values are offered in — cheapest on-ramp first. */
const CARDS_ORDER: readonly CardsKind[] = ['BRING_A_DECK', 'RANDOM', 'MOMIR', 'SEALED', 'DRAFT']

function cardsCaption(kind: CardsKind): string {
  switch (kind) {
    case 'BRING_A_DECK': return 'Play one of your own constructed decks.'
    case 'RANDOM': return 'The server rolls you a deck. Zero preparation.'
    case 'MOMIR': return '60 basics; flip a random creature each turn. No deckbuilding.'
    case 'SEALED': return 'Open boosters and build a deck from what you get.'
    case 'DRAFT': return 'Pick cards one at a time from packs, then build.'
  }
}

/** Sealed and draft put a pool-building step in front of the game; the other three do not. */
function cardsBadge(kind: CardsKind): { text: string; weight: ChoiceWeight } {
  return kind === 'SEALED' || kind === 'DRAFT'
    ? { text: 'Build a deck first', weight: 'EVENT' }
    : { text: 'Play right away', weight: 'QUICK' }
}

export function cardsChoices(roster: Roster): Choice<CardsKind>[] {
  return CARDS_ORDER.map((kind) => ({
    value: kind,
    label: cardsKindLabel(kind),
    caption: cardsCaption(kind),
    topicId: `cards-${kind.toLowerCase().replace(/_/g, '-')}`,
    badge: cardsBadge(kind),
    ...(roster === 'GROUP' && kind === 'RANDOM' ? { disabledReason: RANDOM_IS_A_1V1_SINGLE_GAME } : {}),
    ...(roster === 'GROUP' && kind === 'MOMIR' ? { disabledReason: MOMIR_IS_A_1V1_SINGLE_GAME } : {}),
  }))
}

/** The default sub-shape when a Cards value is first selected. */
export function defaultCardsAxis(kind: CardsKind): CardsAxis {
  switch (kind) {
    case 'BRING_A_DECK': return { kind: 'BRING_A_DECK', legality: null }
    case 'RANDOM': return { kind: 'RANDOM' }
    case 'MOMIR': return { kind: 'MOMIR' }
    case 'SEALED': return { kind: 'SEALED', shape: 'STANDARD' }
    case 'DRAFT': return { kind: 'DRAFT', shape: 'BOOSTER' }
  }
}

/* ── Step 3: shape ──────────────────────────────────────────────────────── */

/** True for the Cards values that only the two-seat quick-game lobby implements. */
function isQuickOnly(kind: CardsKind): boolean {
  return kind === 'RANDOM' || kind === 'MOMIR'
}

function quickOnlyReason(kind: CardsKind): string {
  return kind === 'MOMIR' ? MOMIR_IS_A_1V1_SINGLE_GAME : RANDOM_IS_A_1V1_SINGLE_GAME
}

const MULTIPLAYER_SHAPES: readonly ShapeId[] = ['FREE_FOR_ALL', 'TWO_HEADED_GIANT', 'TEAM_VS_TEAM']

/**
 * Which shapes this roster and Cards value can be played in.
 *
 * Read the three branches as the answer to "what stops the rest?": for a solo player it is the AI
 * seat rules, for a pair it is the arithmetic of two seats, and for a group it is only ever the
 * Cards value.
 */
export function shapeChoices(roster: Roster, cards: CardsAxis): Choice<ShapeId>[] {
  const kind = cards.kind
  const choice = (value: ShapeId, disabledReason?: string): Choice<ShapeId> => ({
    value,
    label: shapeLabel(value),
    caption: shapeCaption(value),
    topicId: shapeTopicId(value),
    // A bracket is the only shape that plays more than one game; the multiplayer tables are each one
    // shared game, which is the distinction the old "Multiplayer vs Tournament" pair blurred.
    badge: value === 'BRACKET'
      ? { text: 'Several rounds · standings', weight: 'EVENT' }
      : { text: 'One game', weight: 'QUICK' },
    ...(disabledReason ? { disabledReason } : {}),
  })

  if (roster === 'GROUP') {
    // ONE_GAME is absent, not disabled: a 1v1 single game contradicts "a group".
    // Every multiplayer table is one shared game, and both limited and premade lobbies can seat one
    // — Free-for-All with your own deck is the combination Part 2 called out as
    // supported-but-unreachable. The quick-only values never get here; step 2 disabled them.
    // Commander is the exception at exactly one table: a Free-for-All or Team vs. Team pod plays it,
    // but Two-Headed Giant's shared team life total has nowhere to put Commander's per-player 40.
    const quickOnly = isQuickOnly(kind) ? quickOnlyReason(kind) : undefined
    // Asked through the shared predicate, of the rules this Cards value implies — so the wizard and
    // the lobby cannot disagree about which table Commander can sit at. The wizard deliberately does
    // not ask about Rules (it is three questions); the host changes it in the lobby.
    const reasonFor = (shape: ShapeId): string | undefined =>
      quickOnly ?? rulesTableBlock(rulesForCards(cards), shapeAxes(shape).table) ?? undefined
    return [
      choice('BRACKET', quickOnly),
      ...MULTIPLAYER_SHAPES.map((s) => choice(s, reasonFor(s))),
    ]
  }

  if (roster === 'SOLO') {
    if (isQuickOnly(kind)) {
      return [choice('ONE_GAME'), choice('BRACKET', quickOnlyReason(kind))]
    }
    // Every shape is open to a solo player: the AI takes a seat at a pod like anyone else, and where
    // there is no pool for it to build from — a premade-deck lobby — it is dealt a generated deck,
    // exactly as the quick game has always done. What is left is the Cards value's own limits, asked
    // through the same shared predicates the group branch uses, so a solo pod and a human pod cannot
    // disagree about which table a Commander pool can sit at.
    const reasonFor = (shape: ShapeId): string | undefined =>
      rulesTableBlock(rulesForCards(cards), shapeAxes(shape).table) ?? undefined
    if (kind === 'BRING_A_DECK') {
      return [
        choice('ONE_GAME'),
        choice('BRACKET'),
        ...MULTIPLAYER_SHAPES.map((s) => choice(s, reasonFor(s))),
      ]
    }
    // A limited pool is meant to be played more than once, so a two-seat single game is the one
    // shape it declines — the pod and the bracket both play it out.
    return [
      choice('ONE_GAME', LIMITED_ALWAYS_RUNS_AS_A_BRACKET),
      choice('BRACKET'),
      ...MULTIPLAYER_SHAPES.map((s) => choice(s, reasonFor(s))),
    ]
  }

  // FRIEND — two seats. The multiplayer shapes are absent rather than disabled: they need a third
  // player, which is a previous answer, not a missing feature.
  if (isQuickOnly(kind)) {
    return [choice('ONE_GAME'), choice('BRACKET', quickOnlyReason(kind))]
  }
  if (kind === 'BRING_A_DECK') {
    return [choice('ONE_GAME'), choice('BRACKET')]
  }
  return [choice('ONE_GAME', LIMITED_ALWAYS_RUNS_AS_A_BRACKET), choice('BRACKET')]
}

/* ── Seats ──────────────────────────────────────────────────────────────── */

/**
 * How many seats the lobby opens with: always the most the selection allows.
 *
 * Nobody is asked. `maxPlayers` is a cap, not a quorum — `startBlockReason` only ever counts the
 * players actually present — so a lobby that opens as wide as its shape permits is one people join
 * until it is full, and the host starts whenever everyone has arrived. Asking up front only ever
 * meant predicting a number and then having to notice it was wrong.
 *
 * The shapes that force a count still force it here: Two-Headed Giant is exactly four, Team vs. Team
 * needs an even pod, Free-for-All is capped by the board layout, and the two-player sub-shapes cap
 * themselves through {@link cardsSeatCap}. A solo lobby uses this as expansion capacity, not as its
 * initial AI count; {@link defaultSoloAiSeats} chooses the smaller roster it starts with.
 */
export function seatCap(roster: Roster, cards: CardsAxis, shape: ShapeId): number {
  if (roster === 'FRIEND') return 2
  // A solo 1v1 is two seats whatever the cards are, and the *shape* is what says so — not the Cards
  // value. Brought decks used to be tested here too, back when the AI could only face one in a quick
  // game; now it can bring a rolled deck to a pod, and a pod must open at its shape's own count.
  if (roster === 'SOLO' && (isQuickOnly(cards.kind) || shape === 'ONE_GAME')) return 2

  const cap = Math.min(cardsSeatCap(cards), shape === 'FREE_FOR_ALL' ? 6 : 8)
  switch (shape) {
    case 'TWO_HEADED_GIANT': return 4
    // Two even teams, and the server rejects fewer than four.
    case 'TEAM_VS_TEAM': return Math.max(4, cap - (cap % 2))
    default: return Math.max(2, cap)
  }
}

/**
 * The starting roster for a solo tournament lobby, deliberately separate from its capacity.
 *
 * Capacity answers how far the host may grow the lobby. Opening with a full table made an eight-seat
 * lobby feel mandatory and forced the host to delete opponents before playing; opening with a
 * *representative* table still guessed, and a draft that started with five bots had to be trimmed by
 * anyone who wanted a shorter event. So the lobby opens with one opponent and Add AI does the rest,
 * up to {@link seatCap} — the smallest playable table is the one nobody has to undo.
 *
 * The two shapes whose count the server forces are the exception: Two-Headed Giant is exactly four
 * seats and Team vs. Team needs two even teams of at least four, so a single opponent would open a
 * lobby that cannot start.
 */
export function defaultSoloAiSeats(cards: CardsAxis, shape: ShapeId): number {
  const totalPlayers = (() => {
    switch (shape) {
      case 'TWO_HEADED_GIANT': return 4
      case 'TEAM_VS_TEAM': return 4
      case 'ONE_GAME':
      case 'FREE_FOR_ALL':
      case 'BRACKET': return 2
    }
  })()
  return Math.max(0, Math.min(totalPlayers, seatCap('SOLO', cards, shape)) - 1)
}

/* ── What will actually happen ──────────────────────────────────────────── */

/**
 * The selection as a sequence of stages: `Open boosters → Build a deck → Everyone plays everyone →
 * Standings`.
 *
 * The single most useful thing to show before committing, because the question a newcomer cannot
 * answer from any of the names involved is "how long is this and how many steps does it have?".
 * "Sealed" does not say that a deckbuilding phase and a standings table are coming.
 */
export function flowStages(selection: Selection): string[] {
  const { roster, cards, shape } = selection
  const seats = seatCap(roster, cards, shape)
  const stages: string[] = []

  switch (cards.kind) {
    case 'BRING_A_DECK': stages.push('Pick one of your decks'); break
    case 'RANDOM': stages.push('The server rolls you a deck'); break
    case 'MOMIR': stages.push('60 basics — no deckbuilding'); break
    case 'SEALED': stages.push('Open boosters', 'Build a deck'); break
    case 'DRAFT': stages.push(`${cardsLabel(cards)}`, 'Build a deck'); break
  }

  switch (shape) {
    case 'ONE_GAME': stages.push('One game'); break
    case 'BRACKET':
      stages.push(seats > 2 ? 'Everyone plays everyone' : 'Play the matchup', 'Standings')
      break
    // Two-Headed Giant is the one table whose count is exact rather than a cap.
    case 'TWO_HEADED_GIANT':
      stages.push('One shared game, 4 seats')
      break
    case 'FREE_FOR_ALL':
    case 'TEAM_VS_TEAM':
      stages.push(`One shared game, up to ${seats} seats`)
      break
  }
  return stages
}

/* ── The launch ─────────────────────────────────────────────────────────── */

/**
 * How a completed selection is realised against the two server lobby implementations.
 *
 * This is the seam `ModePreset.launch` used to be, with the six hand-written cases replaced by a
 * derivation — which is why a new Cards or Table value no longer needs a home-screen change.
 */
export type LaunchSpec =
  | { kind: 'QUICK'; vsAi: boolean; momirBasic: boolean; deckTab: DeckPickerTab }
  | {
      kind: 'TOURNAMENT'
      format: TournamentFormat
      /**
       * Rules axis, *inferred* from the Cards answer rather than asked: the wizard is deliberately
       * three questions. Sent explicitly all the same, so the lobby that comes back is the one the
       * wizard's own 2HG check was reasoning about.
       */
      rules: GameRules
      gameMode: LobbyGameMode
      maxPlayers: number
      /** AI seats to seed after the lobby exists. Only non-zero for a solo tournament lobby. */
      aiSeats: number
    }

export interface Selection {
  roster: Roster
  cards: CardsAxis
  shape: ShapeId
}

/**
 * The quick-game lobby is the only thing that implements a 1v1 single game, and the only thing that
 * implements Momir or a rolled pool at all. Everything else is the tournament lobby.
 */
export function lobbyKindFor(selection: Selection): 'QUICK' | 'TOURNAMENT' {
  const { cards, shape } = selection
  if (isQuickOnly(cards.kind)) return 'QUICK'
  return shape === 'ONE_GAME' && cards.kind === 'BRING_A_DECK' ? 'QUICK' : 'TOURNAMENT'
}

export function resolveLaunch(selection: Selection): LaunchSpec {
  const { roster, cards, shape } = selection
  const seats = seatCap(roster, cards, shape)

  if (lobbyKindFor(selection) === 'QUICK') {
    return {
      kind: 'QUICK',
      vsAi: roster === 'SOLO',
      momirBasic: cards.kind === 'MOMIR',
      deckTab: cards.kind === 'RANDOM' ? 'random' : 'saved',
    }
  }

  // `tournamentFormatForCards` only returns null for Momir, which never reaches here.
  const format = tournamentFormatForCards(cards) ?? 'PREMADE_DECKS'
  return {
    kind: 'TOURNAMENT',
    format,
    rules: rulesForCards(cards),
    gameMode: gameModeForTable(shapeAxes(shape).table),
    maxPlayers: seats,
    aiSeats: roster === 'SOLO' ? defaultSoloAiSeats(cards, shape) : 0,
  }
}

/** "Just me · Booster Draft · Round-robin bracket" — the recap, and the Play again chip. */
export function selectionSummary(selection: Selection): string {
  return [
    rosterLabel(selection.roster),
    // `cardsLabel` already folds the sub-shape in ("Commander Sealed", "Winston Draft").
    cardsLabel(selection.cards),
    shapeLabel(selection.shape),
  ].join(' · ')
}
