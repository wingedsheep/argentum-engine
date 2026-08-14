/**
 * What each axis value costs on *this* lobby.
 *
 * `axes.ts` is the vocabulary and the server mapping. This module is the third thing the unified
 * lobby needs: given a lobby that is already backed by one of the two server implementations,
 * which values of Cards / Table / Event can it actually offer, and what happens if you pick one.
 *
 * Three answers, and the distinction between them is the whole point:
 *
 * - **DIRECT** — one settings update on the lobby you are already in.
 * - **RECREATE** — the other server implementation is the one that can express this, so picking it
 *   tears this lobby down and creates that one. Costs the invite code and any joined players, so
 *   it is always confirmed first (plan § 4b v1). A `convertLobby` message that preserves both is
 *   Phase 6; nothing here blocks on it.
 * - **BLOCKED** — nothing implements this combination yet. Rendered disabled *with the reason
 *   attached* rather than hidden: an option you can see and can't use teaches the shape of the
 *   system, while an option that isn't there just looks like it was never considered. These are
 *   exactly the Phase 5 holes.
 *
 * Two combinations become reachable here for the first time, purely by letting the client cross
 * between the two kinds: **1v1 single game with a friend's own deck** (previously you had to know
 * that the "Quick Game" button, not the lobby, was the way to get one) and **going the other way**
 * — turning a quick game into a draft or a Free-for-All without backing out to the home screen.
 */
import type { DeckFormat, LobbyGameMode, TournamentFormat } from '@/types'
import {
  CARDS_KINDS,
  RULES_VALUES,
  TABLE_VALUES,
  cardsFromTournamentFormat,
  cardsKindLabel,
  eventFromGameMode,
  eventLabel,
  gameModeForTable,
  rulesLabel,
  rulesTableBlock,
  tableFromGameMode,
  tableLabel,
  tournamentFormatForCards,
  type CardsKind,
  type EventAxis,
  type RulesAxis,
  type TableAxis,
} from './axes'
import type { LobbyKind, UnifiedLobbyView } from './lobbyViewModel'
import type { DeckPickerTab } from '../ui/DeckPicker'

/** Everything needed to stand the lobby back up on the other server implementation. */
export type RecreateSpec =
  | {
      to: 'QUICK'
      momirBasic: boolean
      format: DeckFormat | null
      /**
       * Which deck-picker tab the new lobby should open on. Random pool is not a lobby setting —
       * it is this tab — so a switch that promises one has to carry it, or the new lobby would
       * land on "Bring a deck" under a button labelled Random pool.
       */
      deckTab: DeckPickerTab
    }
  | { to: 'TOURNAMENT'; format: TournamentFormat; gameMode: LobbyGameMode }

export type ChoiceAvailability =
  | { kind: 'DIRECT' }
  | { kind: 'RECREATE'; spec: RecreateSpec }
  | { kind: 'BLOCKED'; reason: string }

export interface AxisChoice<V> {
  value: V
  label: string
  selected: boolean
  availability: ChoiceAvailability
}

const DIRECT: ChoiceAvailability = { kind: 'DIRECT' }
const blocked = (reason: string): ChoiceAvailability => ({ kind: 'BLOCKED', reason })
const recreate = (spec: RecreateSpec): ChoiceAvailability => ({ kind: 'RECREATE', spec })

/** Why a value only exists on the other implementation, phrased for a confirm dialog. */
export const RECREATE_NOTE: Record<LobbyKind, string> = {
  QUICK: 'Single 1v1 games run on the quick-game lobby, which is a different lobby to this one.',
  TOURNAMENT: 'Limited pools, multiplayer tables and brackets run on the tournament lobby, which is a different lobby to this one.',
}

/* ── Cards ──────────────────────────────────────────────────────────────── */

/** Asking for Momir when the table is wrong for it. */
const MOMIR_NEEDS_1V1 =
  'Momir Basic is only implemented for 1v1 quick games. Switch the Table to 1v1 first, or bring a deck.'

/** Asking for a different table or event while Momir is the Cards value. */
const MOMIR_BLOCKS_THE_REST =
  'Momir Basic only exists as a 1v1 single game — nothing implements it at another table or in a bracket. Change Cards first.'

const RANDOM_NEEDS_1V1 =
  'Rolling a random pool is only implemented for 1v1 quick games. Switch the Table to 1v1 first.'

export function cardsChoices(view: UnifiedLobbyView): AxisChoice<CardsKind>[] {
  const selectedKind = view.axes.cards.kind
  return CARDS_KINDS.map((kind) => ({
    value: kind,
    label: cardsKindLabel(kind),
    selected: kind === selectedKind,
    availability: cardsAvailability(view, kind),
  }))
}

function cardsAvailability(view: UnifiedLobbyView, kind: CardsKind): ChoiceAvailability {
  if (kind === view.axes.cards.kind) return DIRECT

  if (view.kind === 'QUICK') {
    switch (kind) {
      case 'BRING_A_DECK':
      case 'RANDOM':
      case 'MOMIR':
        return DIRECT
      case 'SEALED':
        return recreate({ to: 'TOURNAMENT', format: 'SEALED', gameMode: 'TOURNAMENT' })
      case 'DRAFT':
        return recreate({ to: 'TOURNAMENT', format: 'DRAFT', gameMode: 'TOURNAMENT' })
    }
  }

  // Tournament-backed.
  switch (kind) {
    case 'BRING_A_DECK':
    case 'SEALED':
    case 'DRAFT':
      return DIRECT
    case 'MOMIR':
      return view.axes.table === 'ONE_V_ONE'
        ? recreate({ to: 'QUICK', momirBasic: true, format: null, deckTab: 'saved' })
        : blocked(MOMIR_NEEDS_1V1)
    case 'RANDOM':
      return view.axes.table === 'ONE_V_ONE'
        ? recreate({ to: 'QUICK', momirBasic: false, format: null, deckTab: 'random' })
        : blocked(RANDOM_NEEDS_1V1)
  }
}

/* ── Rules ──────────────────────────────────────────────────────────────── */

export function rulesChoices(view: UnifiedLobbyView): AxisChoice<RulesAxis>[] {
  return RULES_VALUES.map((rules) => ({
    value: rules,
    label: rulesLabel(rules),
    selected: rules === view.axes.rules,
    availability: rulesAvailability(view, rules),
  }))
}

/**
 * Note the table conflict is checked *before* the "already selected" shortcut every other axis takes
 * first. A lobby can hold a contradiction — setting commander deck legality at a Two-Headed Giant
 * table defaults the Rules axis to Commander — and when it does, the honest rendering is the selected
 * value shown disabled with the reason on it and the other value there to fix it, not a happy button
 * over a Start that refuses.
 */
function rulesAvailability(view: UnifiedLobbyView, rules: RulesAxis): ChoiceAvailability {
  const conflict = rulesTableBlock(rules, view.axes.table)
  if (conflict !== null) return blocked(conflict)
  // An AI seat is no longer a reason to refuse Commander: it builds its own legal deck, and picks
  // its own commander, from a brought-deck lobby or from the pool it is dealt.
  return DIRECT
}

/* ── Table ──────────────────────────────────────────────────────────────── */

/**
 * Upper seat bounds per shape. Shapes that also want an exact or even count (2HG wants 4; Team vs.
 * Team wants an even 4/6/8) stay selectable and are caught by the start button, so the host can
 * pick the shape first and then fill the seats.
 */
const TABLE_SEAT_CAP: Record<TableAxis, number> = {
  ONE_V_ONE: Infinity,
  FREE_FOR_ALL: 6,
  TWO_HEADED_GIANT: 4,
  TEAM_VS_TEAM: 8,
}

function tooManySeats(table: TableAxis, count: number): string {
  switch (table) {
    case 'FREE_FOR_ALL': return `Free-for-All seats at most 6 — this lobby has ${count}`
    case 'TWO_HEADED_GIANT': return `Two-Headed Giant is exactly 4 players — this lobby has ${count}`
    case 'TEAM_VS_TEAM': return `Team vs. Team seats at most 8 — this lobby has ${count}`
    case 'ONE_V_ONE': return ''
  }
}

export function tableChoices(view: UnifiedLobbyView): AxisChoice<TableAxis>[] {
  return TABLE_VALUES.map((table) => ({
    value: table,
    label: tableLabel(table),
    selected: table === view.axes.table,
    availability: tableAvailability(view, table),
  }))
}

function tableAvailability(view: UnifiedLobbyView, table: TableAxis): ChoiceAvailability {
  if (table === view.axes.table) return DIRECT
  if (view.players.length > TABLE_SEAT_CAP[table]) {
    return blocked(tooManySeats(table, view.players.length))
  }
  // Commander plays at any table except Two-Headed Giant, whose shared team life total (CR 810.4)
  // has nowhere to put Commander's per-player 40. Asked of the Rules axis, so it now also catches a
  // Commander lobby whose decks were brought rather than drafted — the case the old
  // `isCommanderLimited` check structurally could not see.
  const conflict = rulesTableBlock(view.axes.rules, table)
  if (conflict !== null) return blocked(conflict)

  if (view.kind === 'TOURNAMENT') return DIRECT

  // Quick-backed: two seats, one game. Anything else is the tournament lobby's job.
  const format = tournamentFormatForCards(view.axes.cards)
  if (format === null) return blocked(MOMIR_BLOCKS_THE_REST)
  return recreate({ to: 'TOURNAMENT', format, gameMode: gameModeForTable(table) })
}

/* ── Event ──────────────────────────────────────────────────────────────── */

const BRACKET_IS_1V1_ONLY =
  'Bracket play is 1v1 only today. A multiplayer table plays one shared game.'

const LIMITED_IS_ALWAYS_A_BRACKET =
  'A limited lobby always runs as a bracket, because the pool it builds is meant to be played more than once. With two players that is one matchup — set “Games per matchup” to 1 and it is a single game.'

export function eventChoices(view: UnifiedLobbyView): AxisChoice<EventAxis>[] {
  return (['SINGLE_GAME', 'ROUND_ROBIN'] as const).map((event) => ({
    value: event,
    label: eventLabel(event),
    selected: event === view.axes.event,
    availability: eventAvailability(view, event),
  }))
}

function eventAvailability(view: UnifiedLobbyView, event: EventAxis): ChoiceAvailability {
  if (event === view.axes.event) return DIRECT

  if (view.kind === 'QUICK') {
    // A quick lobby is always one game; a bracket is the tournament lobby at a 1v1 table.
    const format = tournamentFormatForCards(view.axes.cards)
    if (format === null) return blocked(MOMIR_BLOCKS_THE_REST)
    return recreate({ to: 'TOURNAMENT', format, gameMode: 'TOURNAMENT' })
  }

  // Tournament-backed. Event is not independent server-side: `gameMode = TOURNAMENT` *is* the
  // round-robin bracket of 1v1 matches, and every multiplayer table plays exactly one game.
  if (event === 'ROUND_ROBIN') return blocked(BRACKET_IS_1V1_ONLY)
  // …but a 1v1 single game does exist — it is exactly what the quick-game lobby is. Reaching it
  // from here is new: before the unification the only route was backing out to the home screen.
  const cards = view.axes.cards
  if (cards.kind === 'BRING_A_DECK') {
    return recreate({ to: 'QUICK', momirBasic: false, format: cards.legality, deckTab: 'saved' })
  }
  return blocked(LIMITED_IS_ALWAYS_A_BRACKET)
}

/* ── Confirm copy ───────────────────────────────────────────────────────── */

/** What a recreate would land you on — the confirm dialog's headline. */
export function recreateTargetLabel(spec: RecreateSpec): string {
  if (spec.to === 'QUICK') {
    const cards = spec.momirBasic ? 'Momir Basic' : spec.deckTab === 'random' ? 'Random pool' : 'Bring a deck'
    return `${cards} · 1v1 · Single game`
  }
  const cards = cardsKindLabel(cardsFromTournamentFormat(spec.format, null).kind)
  const table = tableFromGameMode(spec.gameMode)
  return `${cards} · ${tableLabel(table)} · ${eventLabel(eventFromGameMode(spec.gameMode))}`
}
