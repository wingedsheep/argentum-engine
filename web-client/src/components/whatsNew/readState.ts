import type { Announcement } from './announcements'

/**
 * Which announcements this browser has read.
 *
 * Two parts, so the stored list never grows with the feed: everything dated on or before
 * `readThrough` is read, and `read` holds the individually-clicked ids newer than that. "Mark all as
 * read" just moves `readThrough` to the newest entry and empties `read`.
 */
export interface ReadState {
  readThrough: string
  read: string[]
}

/**
 * How far back a first-time visitor's feed starts out unread. Long enough to show someone new what
 * has been landing lately, short enough that the badge isn't a wall of history.
 */
export const FIRST_VISIT_UNREAD_DAYS = 30

export function initialReadState(today: Date): ReadState {
  const cutoff = new Date(today)
  cutoff.setUTCDate(cutoff.getUTCDate() - FIRST_VISIT_UNREAD_DAYS)
  return { readThrough: isoDay(cutoff), read: [] }
}

export function isRead(state: ReadState, entry: Announcement): boolean {
  return entry.date <= state.readThrough || state.read.includes(entry.id)
}

export function markRead(state: ReadState, entry: Announcement): ReadState {
  if (isRead(state, entry)) return state
  return { ...state, read: [...state.read, entry.id] }
}

export function markAllRead(state: ReadState, entries: readonly Announcement[]): ReadState {
  const newest = entries.reduce((max, e) => (e.date > max ? e.date : max), state.readThrough)
  return { readThrough: newest, read: [] }
}

/** Parse what localStorage held; anything malformed is treated as no state at all. */
export function parseReadState(raw: string | null): ReadState | null {
  if (!raw) return null
  try {
    const value = JSON.parse(raw) as Partial<ReadState>
    if (typeof value.readThrough !== 'string' || !Array.isArray(value.read)) return null
    return { readThrough: value.readThrough, read: value.read.filter((id) => typeof id === 'string') }
  } catch {
    return null
  }
}

function isoDay(date: Date): string {
  return date.toISOString().slice(0, 10)
}
