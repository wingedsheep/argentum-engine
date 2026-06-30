/**
 * Set release-state helpers, shared by every surface that lists sets (the deckbuilder set
 * combobox, the Set Completion grid, the lobby/tournament SetPickerModal).
 *
 * A set is "unreleased" when its ISO `YYYY-MM-DD` release date is strictly after today.
 * Sets with no (or malformed) release date are treated as released — those are old / undated
 * sets, never future spoilers. Dates are compared as ISO strings against the local calendar
 * day, so there's no timezone drift: lexicographic order on `YYYY-MM-DD` is chronological.
 */

const ISO_DATE = /^\d{4}-\d{2}-\d{2}$/

function pad2(n: number): string {
  return n < 10 ? `0${n}` : String(n)
}

/** Today's local calendar day as an ISO `YYYY-MM-DD` string. */
export function todayIso(now: Date = new Date()): string {
  return `${now.getFullYear()}-${pad2(now.getMonth() + 1)}-${pad2(now.getDate())}`
}

/**
 * Whether a set's release date is in the future (a not-yet-released spoiler set).
 * `today` is injectable so callers can compute it once for a whole list.
 */
export function isUnreleasedSet(releaseDate?: string | null, today: string = todayIso()): boolean {
  if (!releaseDate || !ISO_DATE.test(releaseDate)) return false
  return releaseDate > today
}
