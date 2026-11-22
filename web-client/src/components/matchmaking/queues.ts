/**
 * Presentation helpers for the matchmaking queues. The server owns the queues and pairs players;
 * this module only names the choices and reads the server's searching counts.
 */
import type { DeckFormat, MatchmakingMode, MatchmakingQueueCount } from '@/types'

/** One queue: a mode, the Constructed format (null for every other mode), and casual or ranked. */
export interface QueueChoice {
  readonly mode: MatchmakingMode
  readonly format: DeckFormat | null
  readonly ranked: boolean
}

export interface QueueModeOption {
  readonly mode: MatchmakingMode
  /** The segment label — short enough for four to share the rail. */
  readonly label: string
  /** The full name, used in "Searching …" and the match-found prompt. */
  readonly name: string
  /** One line on what the game is, under the mode picker. */
  readonly blurb: string
  /** Whether a ranked queue exists. Mirrors the server's `MatchmakingMode.rankable`. */
  readonly rankable: boolean
}

/** Every mode, easiest yes first: the three that need no deck, then bring-your-own. */
export const QUEUE_MODES: readonly QueueModeOption[] = [
  {
    mode: 'RANDOM_DECK',
    label: 'Random',
    name: 'Random deck',
    blurb: 'You each get a sealed deck from the same random set.',
    rankable: true,
  },
  {
    mode: 'JUMP_IN',
    label: 'Jump In',
    name: 'Jump In',
    blurb: 'Pick two themed packs; together they make your deck.',
    rankable: false,
  },
  {
    mode: 'MOMIR_BASIC',
    label: 'Momir',
    name: 'Momir Basic',
    blurb: '60 basics and Momir Vig: a random creature each turn.',
    rankable: false,
  },
  {
    mode: 'CONSTRUCTED',
    label: 'Constructed',
    name: 'Constructed',
    blurb: 'Bring a deck legal in the format you pick.',
    rankable: true,
  },
]

export interface QueueFormatOption {
  readonly format: DeckFormat
  readonly label: string
}

/** The Constructed queue's formats. */
export const QUEUE_FORMATS: readonly QueueFormatOption[] = [
  { format: 'STANDARD', label: 'Standard' },
  { format: 'PIONEER', label: 'Pioneer' },
  { format: 'MODERN', label: 'Modern' },
  { format: 'PAUPER', label: 'Pauper' },
  { format: 'LEGACY', label: 'Legacy' },
  { format: 'VINTAGE', label: 'Vintage' },
  { format: 'PREMODERN', label: 'Premodern' },
  { format: 'HIGHLANDER', label: 'Highlander' },
  { format: 'CANADIAN_HIGHLANDER', label: 'Canadian Highlander' },
  { format: 'PRIMORDIAL', label: 'Primordial' },
  { format: 'COMMANDER', label: 'Commander' },
  { format: 'BRAWL', label: 'Brawl' },
  { format: 'STANDARD_BRAWL', label: 'Standard Brawl' },
]

export const DEFAULT_QUEUE: QueueChoice = { mode: 'RANDOM_DECK', format: null, ranked: false }

export function queueMode(mode: MatchmakingMode | null | undefined): QueueModeOption {
  return QUEUE_MODES.find((o) => o.mode === mode) ?? QUEUE_MODES[0]!
}

/**
 * Normalise a stored or half-built choice to a queue the server accepts: Constructed always names a
 * format, no other mode does, and only a rankable mode is ranked.
 */
export function normaliseQueue(choice: {
  readonly mode?: MatchmakingMode | null | undefined
  readonly format?: DeckFormat | null | undefined
  readonly ranked?: boolean | null | undefined
}): QueueChoice {
  const mode = queueMode(choice.mode).mode
  const format = mode === 'CONSTRUCTED'
    ? QUEUE_FORMATS.find((o) => o.format === choice.format)?.format ?? QUEUE_FORMATS[0]!.format
    : null
  return { mode, format, ranked: choice.ranked === true && queueMode(mode).rankable }
}

/** What the game is called, casual or ranked aside: "Random deck", "Jump In", "Pauper". */
export function queueGameLabel(mode: MatchmakingMode | null | undefined, format: DeckFormat | null | undefined): string {
  if (mode === 'CONSTRUCTED') return QUEUE_FORMATS.find((o) => o.format === format)?.label ?? String(format)
  return queueMode(mode).name
}

/**
 * "Ranked Pauper", "Casual Random deck", "Jump In". A mode without a ranked queue is always casual,
 * so saying so would only add a word.
 */
export function queueLabel(
  mode: MatchmakingMode | null | undefined,
  format: DeckFormat | null | undefined,
  ranked: boolean | undefined,
): string {
  const game = queueGameLabel(mode, format)
  if (!queueMode(mode).rankable) return game
  return `${ranked ? 'Ranked' : 'Casual'} ${game}`
}

function isQueue(count: MatchmakingQueueCount, choice: QueueChoice): boolean {
  return count.mode === choice.mode && (count.format ?? null) === choice.format && count.ranked === choice.ranked
}

/** Players searching in one queue. */
export function searchingIn(counts: readonly MatchmakingQueueCount[] | null, choice: QueueChoice): number {
  return counts?.find((c) => isQueue(c, choice))?.searching ?? 0
}

/** Players searching anywhere in [mode] — every format, casual and ranked. */
export function searchingInMode(counts: readonly MatchmakingQueueCount[] | null, mode: MatchmakingMode): number {
  return (counts ?? []).filter((c) => c.mode === mode).reduce((sum, c) => sum + c.searching, 0)
}

/** Queues with someone in them, busiest first — where a game is most likely right now. */
export function activeQueues(counts: readonly MatchmakingQueueCount[] | null): readonly MatchmakingQueueCount[] {
  return [...(counts ?? [])].filter((c) => c.searching > 0).sort((a, b) => b.searching - a.searching)
}

/** "0:07", "2:41" — time spent searching. */
export function formatWait(ms: number): string {
  const totalSeconds = Math.max(0, Math.floor(ms / 1000))
  const minutes = Math.floor(totalSeconds / 60)
  const seconds = totalSeconds % 60
  return `${minutes}:${seconds.toString().padStart(2, '0')}`
}
