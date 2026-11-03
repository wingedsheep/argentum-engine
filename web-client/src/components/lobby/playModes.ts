/**
 * The landing screen's game modes: what a player names when they know what they want to play.
 *
 * The wizard this replaces asked three abstract questions — who fills the seats, where the cards come
 * from, which table and event — because those are the axes every game is a point in. That is the
 * right model for the lobby, which *edits* a point, and the wrong one for choosing, because nobody
 * thinks "Group · Bring a deck · Free-for-All". They think "Commander with friends", or "a draft
 * against the AI". So the landing screen offers named modes, and the axes become the options inside
 * one: who you play with, how many opponents, which draft style, which deck.
 *
 * This module is the translation, and only that. Whether a combination is playable is still
 * `modeMatrix.ts`'s answer — every {@link PlayOptions} resolves to a {@link Selection} that its
 * `cardsChoices` / `shapeChoices` accept (`playModes.test.ts` walks them all), and the launch is still
 * a {@link LobbyRecipe} through `useApplyRecipe`. Nothing here talks to the store.
 */
import type { DeckFormat } from '@/types'
import type { CardsAxis } from './axes'
import { lobbyKindFor, type Roster, type Selection, type ShapeId } from './modeMatrix'
import { recipeFromSelection, type LobbyRecipe, type RecipeSettings } from './lobbyRecipe'

/* ── Vocabulary ─────────────────────────────────────────────────────────── */

export type ModeId =
  | 'CONSTRUCTED'
  | 'JUMP_IN'
  | 'RANDOM'
  | 'MOMIR'
  | 'DRAFT'
  | 'SEALED'
  | 'FREE_FOR_ALL'
  | 'TWO_HEADED_GIANT'
  | 'TEAM_VS_TEAM'
  | 'COMMANDER'

/** How much stands between the click and the first game — the catalogue's grouping. */
export type ModeGroup = 'NOW' | 'PACKS' | 'TABLE'

/** Who takes the other seats. Public is Friends with the lobby listed for anyone to join. */
export type PlayWith = 'AI' | 'FRIENDS' | 'PUBLIC'

export type DraftStyle = 'BOOSTER' | 'WINSTON' | 'GRID' | 'COMMANDER'
export type SealedStyle = 'STANDARD' | 'COMMANDER'
/** Where the decks come from at a multiplayer table. */
export type TableCards = 'DECKS' | 'JUMP_IN' | 'SEALED' | 'DRAFT'
/** With people rather than the AI: one opponent, or a bracket anyone can join up to its cap. */
export type HumanTable = 'ONE_V_ONE' | 'BRACKET'

/**
 * The deck the launch panel settled on, when the mode brings one.
 *
 * Names, not lists — the same portable key a recipe stores. `LOBBY` is the explicit "I'll pick
 * there"; a starter deck and a rolled one exist so that someone with no decks of their own still
 * has a deck to name here, and an AI game can go straight to the table.
 */
export type PanelDeck =
  | { readonly kind: 'SAVED'; readonly name: string }
  | { readonly kind: 'EXAMPLE'; readonly name: string }
  | { readonly kind: 'RANDOM' }
  | { readonly kind: 'LOBBY' }

export interface ModeInfo {
  id: ModeId
  group: ModeGroup
  label: string
  /** One line on the tile. */
  caption: string
  /** One sentence at the top of the launch panel. */
  description: string
  /** The player-count line on the tile. */
  players: string
  /** URL slug, `/play/<slug>`. Hand-written: a saved link must survive an enum rename. */
  slug: string
}

export const MODE_GROUPS: ReadonlyArray<{ id: ModeGroup; label: string; caption: string }> = [
  { id: 'NOW', label: 'Play right now', caption: 'No deckbuilding' },
  { id: 'PACKS', label: 'Build from packs', caption: 'Open or draft packs, then build 40 cards' },
  { id: 'TABLE', label: 'Multiplayer tables', caption: 'Three or more players in one game' },
]

export const MODES: readonly ModeInfo[] = [
  {
    id: 'CONSTRUCTED', group: 'NOW', label: 'Constructed', slug: 'constructed', players: '2–8 players',
    caption: 'Your own deck, or a starter deck.',
    description: 'Play a deck of your own or one of ours — one game, or a bracket with more players.',
  },
  {
    id: 'JUMP_IN', group: 'NOW', label: 'Jump In', slug: 'jump-in', players: '2–8 players',
    caption: 'Pick two themed packs and play.',
    description: 'Choose two themed Jumpstart packs. They make a 40-card deck, lands included.',
  },
  {
    id: 'RANDOM', group: 'NOW', label: 'Random deck', slug: 'random', players: '2 players',
    caption: 'We deal you a ready-made deck.',
    description: 'The server rolls a deck for you. Zero preparation.',
  },
  {
    id: 'MOMIR', group: 'NOW', label: 'Momir Basic', slug: 'momir', players: '2 players',
    caption: '60 basics, a random creature each turn.',
    description: 'Sixty basic lands. Discard one to create a random creature of that mana value.',
  },
  {
    id: 'DRAFT', group: 'PACKS', label: 'Draft', slug: 'draft', players: '2–8 players',
    caption: 'Pick cards from packs, then play a bracket.',
    description: 'Take one card at a time as the packs go round, build 40 cards, play everyone.',
  },
  {
    id: 'SEALED', group: 'PACKS', label: 'Sealed', slug: 'sealed', players: '2–8 players',
    caption: 'Open boosters and build from them.',
    description: 'Everyone opens their own boosters and builds a 40-card deck from what they get.',
  },
  {
    id: 'FREE_FOR_ALL', group: 'TABLE', label: 'Free-for-All', slug: 'free-for-all', players: '3–6 players',
    caption: 'Everyone for themselves at one table.',
    description: 'One shared game. Every player for themselves; the last one standing wins.',
  },
  {
    id: 'TWO_HEADED_GIANT', group: 'TABLE', label: 'Two-Headed Giant', slug: 'two-headed-giant', players: '4 players',
    caption: 'Two teams of two, shared life and turns.',
    description: 'Two teams of two. Teammates share 30 life, their turns and combat.',
  },
  {
    id: 'TEAM_VS_TEAM', group: 'TABLE', label: 'Team vs. Team', slug: 'team-vs-team', players: '4–8 players',
    caption: 'Two teams, every player with their own life.',
    description: 'Two even teams in one game; every player keeps their own life total and turn.',
  },
  {
    id: 'COMMANDER', group: 'TABLE', label: 'Commander', slug: 'commander', players: '2–6 players',
    caption: 'Bring a 100-card commander deck.',
    description: 'Commander rules with your own decks: a 1v1 duel, or a Free-for-All pod.',
  },
]

export function modeInfo(id: ModeId): ModeInfo {
  return MODES.find((m) => m.id === id)!
}

export function modeFromSlug(slug: string | undefined): ModeId | null {
  return MODES.find((m) => m.slug === slug)?.id ?? null
}

/* ── Options ────────────────────────────────────────────────────────────── */

export interface PlayOptions {
  mode: ModeId
  playWith: PlayWith
  /** AI opponents. Only read when {@link PlayOptions.playWith} is `AI`. */
  opponents: number
  /** Only read for Constructed and Jump In with people. */
  humanTable: HumanTable
  draftStyle: DraftStyle
  sealedStyle: SealedStyle
  /** Only read at a multiplayer table. */
  tableCards: TableCards
  /**
   * The set to open or draft, when the cards come from packs. Null = the lobby's default.
   * `RANDOM_SET_CODE` = a deferred Random Set the server rolls when the event starts.
   */
  setCode: string | null
  /**
   * The set a rolled deck is built from, when the panel rolls one. Null = any set — the server picks
   * one (and the AI opponent, on its default, plays the same set either way).
   */
  randomSetCode: string | null
  /** The deck to bring. Null = not decided — the panel fills in a default. */
  deck: PanelDeck | null
  /** AI assistance (Suggest Pick, Auto-build, card scores). Only read where {@link offersAiAssist}. */
  aiAssist: boolean
}

export function defaultOptions(mode: ModeId, aiEnabled: boolean): PlayOptions {
  const base: PlayOptions = {
    mode,
    playWith: aiEnabled ? 'AI' : 'FRIENDS',
    opponents: 1,
    humanTable: 'ONE_V_ONE',
    draftStyle: 'BOOSTER',
    sealedStyle: 'STANDARD',
    tableCards: 'DECKS',
    setCode: null,
    randomSetCode: null,
    deck: null,
    aiAssist: false,
  }
  return { ...base, opponents: opponentRange(base)?.fallback ?? 1 }
}

/** The AI-opponent stepper for these options, or null when the count is fixed by the mode. */
export function opponentRange(
  options: PlayOptions,
): { min: number; max: number; step: number; fallback: number } | null {
  switch (options.mode) {
    case 'RANDOM':
    case 'MOMIR':
    case 'TWO_HEADED_GIANT':
      return null
    case 'CONSTRUCTED':
    case 'JUMP_IN':
      return { min: 1, max: 7, step: 1, fallback: 1 }
    case 'SEALED':
      return { min: 1, max: 7, step: 1, fallback: 3 }
    case 'DRAFT':
      switch (options.draftStyle) {
        case 'WINSTON': return null
        case 'GRID': return { min: 1, max: 3, step: 1, fallback: 3 }
        default: return { min: 1, max: 7, step: 1, fallback: 3 }
      }
    case 'FREE_FOR_ALL':
      return { min: 2, max: 5, step: 1, fallback: 3 }
    // Two even teams: the table is 4, 6 or 8 including you.
    case 'TEAM_VS_TEAM':
      return { min: 3, max: 7, step: 2, fallback: 3 }
    case 'COMMANDER':
      return { min: 1, max: 5, step: 1, fallback: 3 }
  }
}

/** The AI count these options actually ask for — clamped, and fixed where the mode fixes it. */
export function effectiveOpponents(options: PlayOptions): number {
  if (options.mode === 'TWO_HEADED_GIANT') return 3
  if (options.mode === 'DRAFT' && options.draftStyle === 'WINSTON') return 1
  const range = opponentRange(options)
  if (!range) return 1
  const clamped = Math.min(range.max, Math.max(range.min, options.opponents))
  // Snap onto the step so Team vs. Team never asks for an odd table.
  return range.min + Math.round((clamped - range.min) / range.step) * range.step
}

/** Whether these options choose between one human opponent and a bracket. */
export function hasHumanTableChoice(mode: ModeId): boolean {
  return mode === 'CONSTRUCTED' || mode === 'JUMP_IN'
}

/** Whether the panel should ask for a saved deck. */
export function needsDeck(options: PlayOptions): boolean {
  return cardsFor(options).kind === 'BRING_A_DECK'
}

/**
 * Whether "Random deck" can be offered as your deck.
 *
 * A rolled deck is the quick lobby's Random tab; the premade-decks tournament a bracket runs on
 * takes a submitted list only, so a bracket or a pod never offers it.
 */
export function canRollDeck(options: PlayOptions): boolean {
  return needsDeck(options) && lobbyKindFor(selectionFor(options)) === 'QUICK'
}

/**
 * Whether the server rolls your deck — the Random deck mode, or "Random deck" picked as your deck —
 * so the panel can offer to pin which set it is rolled from.
 */
export function rollsDeck(options: PlayOptions): boolean {
  if (cardsFor(options).kind === 'RANDOM') return true
  return options.deck?.kind === 'RANDOM' && canRollDeck(options)
}

/** Whether the cards come out of packs from a set the player should choose. */
export function needsSet(options: PlayOptions): boolean {
  const cards = cardsFor(options)
  if (cards.kind === 'SEALED' || cards.kind === 'DRAFT') return true
  return false
}

/**
 * Whether the panel asks about AI assistance: a game against the AI whose cards come from packs, so
 * there is a draft or a deck to build. It starts right away, with no lobby to switch it on in; a
 * lobby with people keeps the choice in its own settings, where the host makes it for everyone.
 */
export function offersAiAssist(options: PlayOptions): boolean {
  return options.playWith === 'AI' && needsSet(options)
}

/** The commander deck legality, which also implies Commander rules (`rulesForLegality`). */
const COMMANDER_LEGALITY: DeckFormat = 'COMMANDER'

/* ── Resolution ─────────────────────────────────────────────────────────── */

function cardsFor(options: PlayOptions): CardsAxis {
  switch (options.mode) {
    case 'CONSTRUCTED': return { kind: 'BRING_A_DECK', legality: null }
    case 'COMMANDER': return { kind: 'BRING_A_DECK', legality: COMMANDER_LEGALITY }
    case 'JUMP_IN': return { kind: 'JUMP_IN' }
    case 'RANDOM': return { kind: 'RANDOM' }
    case 'MOMIR': return { kind: 'MOMIR' }
    case 'DRAFT': return { kind: 'DRAFT', shape: options.draftStyle }
    case 'SEALED': return { kind: 'SEALED', shape: options.sealedStyle }
    case 'FREE_FOR_ALL':
    case 'TWO_HEADED_GIANT':
    case 'TEAM_VS_TEAM':
      switch (options.tableCards) {
        case 'DECKS': return { kind: 'BRING_A_DECK', legality: null }
        case 'JUMP_IN': return { kind: 'JUMP_IN' }
        case 'SEALED': return { kind: 'SEALED', shape: 'STANDARD' }
        case 'DRAFT': return { kind: 'DRAFT', shape: 'BOOSTER' }
      }
  }
}

function rosterFor(options: PlayOptions): Roster {
  if (options.playWith === 'AI') return 'SOLO'
  switch (options.mode) {
    case 'RANDOM':
    case 'MOMIR':
      return 'FRIEND'
    case 'CONSTRUCTED':
    case 'JUMP_IN':
      return options.humanTable === 'ONE_V_ONE' ? 'FRIEND' : 'GROUP'
    default:
      return 'GROUP'
  }
}

function shapeFor(options: PlayOptions, roster: Roster): ShapeId {
  switch (options.mode) {
    case 'RANDOM':
    case 'MOMIR':
      return 'ONE_GAME'
    case 'CONSTRUCTED':
      if (roster === 'SOLO') return effectiveOpponents(options) === 1 ? 'ONE_GAME' : 'BRACKET'
      return roster === 'FRIEND' ? 'ONE_GAME' : 'BRACKET'
    // A pool is meant to be played more than once, so even a two-seat Jump In, Sealed or Draft runs
    // as a bracket — with one opponent that is a single matchup anyway.
    case 'JUMP_IN':
    case 'DRAFT':
    case 'SEALED':
      return 'BRACKET'
    case 'FREE_FOR_ALL': return 'FREE_FOR_ALL'
    case 'TWO_HEADED_GIANT': return 'TWO_HEADED_GIANT'
    case 'TEAM_VS_TEAM': return 'TEAM_VS_TEAM'
    // A commander duel is a two-seat bracket — the quick lobby has no Rules axis to carry Commander.
    case 'COMMANDER':
      if (roster === 'SOLO') return effectiveOpponents(options) === 1 ? 'BRACKET' : 'FREE_FOR_ALL'
      return 'FREE_FOR_ALL'
  }
}

export function selectionFor(options: PlayOptions): Selection {
  const roster = rosterFor(options)
  return { roster, cards: cardsFor(options), shape: shapeFor(options, roster) }
}

/**
 * The recipe these options describe.
 *
 * Built on {@link recipeFromSelection} so a mode-made lobby and a wizard-made one would start from
 * the same defaults, then refined with what the panel knows that a bare selection doesn't: the set,
 * the deck, how many AI opponents, whether it is listed, and — for an AI game — that there is nobody
 * to wait for, so the lobby may start itself once every seat is filled.
 */
export function recipeForOptions(options: PlayOptions): LobbyRecipe {
  const selection = selectionFor(options)
  const base = recipeFromSelection(selection)
  const solo = selection.roster === 'SOLO'

  const settings: { -readonly [K in keyof RecipeSettings]: RecipeSettings[K] } = { ...base.settings }
  if (options.setCode && needsSet(options)) settings.setCodes = [options.setCode]
  if (options.playWith === 'PUBLIC') settings.isPublic = true
  if (offersAiAssist(options) && options.aiAssist) settings.aiAssistEnabled = true
  if (options.mode === 'COMMANDER') {
    // `rulesForCards` only infers Commander from a commander *pack* shape; a brought commander deck
    // has to say it, or the lobby would open on Standard rules with Commander deck legality.
    settings.rules = 'COMMANDER'
    settings.deckFormat = COMMANDER_LEGALITY
  }

  const picked = selection.cards.kind === 'BRING_A_DECK' ? recipeDeckFor(options, base.deck) : base.deck
  const deck = picked.kind === 'RANDOM' && options.randomSetCode
    ? { kind: 'RANDOM' as const, setCodes: [options.randomSetCode] }
    : picked

  return {
    ...base,
    settings,
    deck,
    // `recipeFromSelection` seeds the smallest playable table; the panel asked for a number.
    aiSeats: solo && base.aiSeats > 0 ? effectiveOpponents(options) : base.aiSeats,
    autoStart: solo,
  }
}

function recipeDeckFor(options: PlayOptions, fallback: LobbyRecipe['deck']): LobbyRecipe['deck'] {
  const deck = options.deck
  switch (deck?.kind) {
    case 'SAVED': return { kind: 'SAVED', name: deck.name }
    case 'EXAMPLE': return { kind: 'EXAMPLE', name: deck.name }
    case 'RANDOM': return canRollDeck(options) ? { kind: 'RANDOM' } : fallback
    default: return fallback
  }
}

/** Whether these options name a deck, so nothing is left to choose in the lobby. */
export function hasDeck(options: PlayOptions): boolean {
  const kind = options.deck?.kind
  return kind === 'SAVED' || kind === 'EXAMPLE' || (kind === 'RANDOM' && canRollDeck(options))
}

/** The players at the table, you included — for the seat preview and the Play button. */
export function tableSize(options: PlayOptions): number | null {
  if (options.playWith !== 'AI') return null
  return effectiveOpponents(options) + 1
}

/**
 * What happens after Play, as short stages: `Draft 3 packs → Build 40 → Everyone plays everyone`.
 *
 * The one place the panel says how long this is before you commit — "Sealed" alone doesn't say a
 * deckbuilding step and a standings table are coming.
 */
export function stagesFor(options: PlayOptions): string[] {
  const selection = selectionFor(options)
  const stages: string[] = []
  if (options.playWith !== 'AI') stages.push('Players join')
  const cards = selection.cards
  switch (cards.kind) {
    case 'BRING_A_DECK':
      if (!hasDeck(options)) stages.push('Choose a deck')
      else if (options.deck?.kind === 'RANDOM') stages.push('Get a deck')
      break
    case 'RANDOM': stages.push('Get a deck'); break
    case 'MOMIR': break
    case 'JUMP_IN': stages.push('Pick 2 packs'); break
    case 'SEALED': stages.push('Open boosters', 'Build 40'); break
    case 'DRAFT':
      stages.push(cards.shape === 'WINSTON' ? 'Winston draft' : cards.shape === 'GRID' ? 'Grid draft' : 'Draft', 'Build 40')
      break
  }
  const players = tableSize(options)
  switch (selection.shape) {
    case 'ONE_GAME': stages.push('Play'); break
    case 'BRACKET':
      stages.push(players === 2 || selection.roster === 'FRIEND' ? 'Play the match' : 'Everyone plays everyone')
      if (players !== 2 && selection.roster !== 'FRIEND') stages.push('Standings')
      break
    default: stages.push('One shared game')
  }
  return stages
}

/** The Play button's label. */
export function launchLabel(options: PlayOptions): string {
  if (options.playWith === 'FRIENDS') return 'Create lobby'
  if (options.playWith === 'PUBLIC') return 'Open public lobby'
  const cards = selectionFor(options).cards
  if (cards.kind === 'DRAFT') return 'Start draft'
  if (cards.kind === 'SEALED') return 'Open boosters'
  return 'Play'
}
