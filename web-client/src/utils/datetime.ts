/** Shared date/time formatting for stat and activity displays. */

/**
 * Render an ISO-8601 timestamp (e.g. the server's `endedAt`, an `Instant.toString()`) as a localized
 * date *and* time in the viewer's timezone — "Jun 30, 2026, 2:23 PM". Used wherever a recorded game
 * is listed (profile history, admin activity) so the moment it was played reads the same everywhere.
 * Falls back to the raw string if it can't be parsed.
 */
export function formatDateTime(iso: string): string {
  try {
    const d = new Date(iso)
    if (Number.isNaN(d.getTime())) return iso
    return d.toLocaleString(undefined, {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: 'numeric',
      minute: '2-digit',
    })
  } catch {
    return iso
  }
}
