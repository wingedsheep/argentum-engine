import type { CardSummary } from './cardFilter'

/** CR 100.4a — in constructed play, a sideboard may contain no more than fifteen cards. */
export const CONSTRUCTED_SIDEBOARD_MAX = 15

/**
 * Whether a card reaches *outside the game* — Learn, the wish cycle, Karn, Ring of Ma'rûf — and so
 * only does anything when the deck's sideboard has something in it. Read off the Oracle text: every
 * such card says "outside the game" (Learn in its reminder text) or names "your sideboard".
 */
export function reachesOutsideTheGame(card: CardSummary | undefined): boolean {
  const text = card?.oracleText ?? ''
  return /outside the game|your sideboard/i.test(text) || /\blearn\b/i.test(text)
}

function learns(card: CardSummary | undefined): boolean {
  return /\blearn\b/i.test(card?.oracleText ?? '')
}

export interface OutsideTheGameSummary {
  /** Names of main-deck cards that reach outside the game, alphabetical. */
  readonly reachers: readonly string[]
  /** Whether any of them is a Learn card — Learn only ever fetches a Lesson. */
  readonly learns: boolean
  /** Lesson cards in the sideboard. */
  readonly lessons: number
  /** Cards in the sideboard. */
  readonly size: number
}

/** What the sideboard panel needs to tell the player about their wishes and Learn cards. */
export function summarizeOutsideTheGame(
  deckCards: Record<string, number>,
  sideboardCards: Record<string, number>,
  catalog: Record<string, CardSummary>,
): OutsideTheGameSummary {
  const reachers = Object.keys(deckCards)
    .filter((name) => (deckCards[name] ?? 0) > 0 && reachesOutsideTheGame(catalog[name]))
    .sort((a, b) => a.localeCompare(b))
  let lessons = 0
  let size = 0
  for (const [name, count] of Object.entries(sideboardCards)) {
    size += count
    if (catalog[name]?.subtypes.includes('Lesson')) lessons += count
  }
  return {
    reachers,
    learns: reachers.some((name) => learns(catalog[name])),
    lessons,
    size,
  }
}

/** "Burning Wish", "Burning Wish and Eyetwitch", "Burning Wish and 2 others". */
export function namesPhrase(names: readonly string[]): string {
  if (names.length <= 1) return names[0] ?? ''
  if (names.length === 2) return `${names[0]} and ${names[1]}`
  return `${names[0]} and ${names.length - 1} others`
}
