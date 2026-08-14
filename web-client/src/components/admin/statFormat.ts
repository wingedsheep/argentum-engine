/** Shared formatting helpers for stat displays. */

const COLOR_NAMES: Record<string, string> = { W: 'White', U: 'Blue', B: 'Black', R: 'Red', G: 'Green' }

/** Per-pip swatch colours (MTG-ish), used when rendering a single-color identity. */
const COLOR_SWATCH: Record<string, string> = {
  W: '#e9e3c8',
  U: '#3a83d6',
  B: '#7a6b86',
  R: '#d9534f',
  G: '#4fa45a',
}

/**
 * Render a WUBRG color-identity string (e.g. "WU") as a readable label. Empty = colorless. Used by
 * both the profile and admin color breakdowns so they read the same way.
 */
export function colorLabel(colors: string): string {
  if (!colors) return 'Colorless'
  const names = [...colors].map((c) => COLOR_NAMES[c]).filter(Boolean)
  return names.length > 0 ? names.join('/') : colors
}

/**
 * A chart fill for a color-identity string: the pip colour for monocolor, gold for multicolor,
 * grey for colorless. Keeps every colors-you-play chart reading the same way.
 */
export function colorForIdentity(colors: string): string {
  if (!colors) return '#9aa0a6'
  const pips = [...colors].filter((c) => c in COLOR_SWATCH)
  if (pips.length === 0) return '#9aa0a6'
  if (pips.length === 1) return COLOR_SWATCH[pips[0] as string] as string
  return '#cba14a'
}

/** Title-case an UPPER_SNAKE enum token: WINSTON_DRAFT → "Winston Draft". */
function titleCaseToken(token: string): string {
  return token
    .toLowerCase()
    .split('_')
    .map((w) => (w ? w.charAt(0).toUpperCase() + w.slice(1) : w))
    .join(' ')
}

/** A TournamentFormat enum name as a short label (PREMADE_DECKS reads better as just "Premade"). */
function prettyFormat(format: string): string {
  if (format === 'PREMADE_DECKS') return 'Premade'
  return titleCaseToken(format)
}

/**
 * A game's mode as a two-level hierarchy — a top-level category plus an optional variant — so the
 * profile can show "Tournament › Draft" or "Multiplayer › Two-Headed Giant" instead of a flat
 * "Tournament" / "Free For All". `gameMode` is a LobbyGameMode name (or QUICK_GAME / CASUAL for
 * non-lobby games); `format` is the TournamentFormat used to acquire decks, shown as the variant for
 * tournaments. Falls back to a title-cased token for anything unrecognised.
 */
export function gameModeLabel(
  gameMode: string | null,
  format: string | null,
): { primary: string; variant: string | null } {
  switch (gameMode) {
    case 'TOURNAMENT':
      return { primary: 'Tournament', variant: format ? prettyFormat(format) : null }
    case 'FREE_FOR_ALL':
      return { primary: 'Multiplayer', variant: 'Free-for-All' }
    case 'TWO_HEADED_GIANT':
      return { primary: 'Multiplayer', variant: 'Two-Headed Giant' }
    case 'TEAM_VS_TEAM':
      return { primary: 'Multiplayer', variant: 'Team vs Team' }
    case 'QUICK_GAME':
      return { primary: 'Quick Game', variant: null }
    case 'CASUAL':
      return { primary: 'Casual', variant: null }
    case 'HOTSEAT':
      return { primary: 'Hotseat', variant: null }
    case null:
    case '':
    case 'UNKNOWN':
      return { primary: '—', variant: null }
    default:
      return { primary: titleCaseToken(gameMode), variant: null }
  }
}

/**
 * Split a mode-breakdown bucket label — a `"<gameMode>~<format>"` composite emitted by the server's
 * `modeBreakdown` — into the display hierarchy. Tolerates a bare label (no `~`) defensively.
 */
export function splitModeBucket(label: string): { primary: string; variant: string | null } {
  const sep = label.indexOf('~')
  const gameMode = sep < 0 ? label || null : label.slice(0, sep) || null
  const format = sep < 0 ? null : label.slice(sep + 1) || null
  return gameModeLabel(gameMode, format)
}

/**
 * Merge raw mode buckets into display rows, summing any whose hierarchy label collapses to the same
 * thing (e.g. several quick-game engine formats all read as "Quick Game"). Most-played first.
 */
export function mergeModeBuckets(
  buckets: readonly { label: string; count: number }[],
): { label: string; count: number }[] {
  const acc = new Map<string, number>()
  for (const b of buckets) {
    const { primary, variant } = splitModeBucket(b.label)
    const label = variant ? `${primary} › ${variant}` : primary
    acc.set(label, (acc.get(label) ?? 0) + b.count)
  }
  return [...acc.entries()].map(([label, count]) => ({ label, count })).sort((a, b) => b.count - a.count)
}

/** Byte count as a compact binary-unit size: 0 → "0 B", 1536 → "1.5 KiB", 5e6 → "4.8 MiB". */
export function formatBytes(bytes: number): string {
  if (!Number.isFinite(bytes) || bytes <= 0) return '0 B'
  const units = ['B', 'KiB', 'MiB', 'GiB', 'TiB']
  const exp = Math.min(units.length - 1, Math.floor(Math.log(bytes) / Math.log(1024)))
  const value = bytes / 1024 ** exp
  // Whole bytes read oddly as "512.0 B"; above that one decimal is enough precision.
  const digits = exp === 0 ? 0 : value < 10 ? 1 : value < 100 ? 1 : 0
  return `${value.toFixed(digits)} ${units[exp]}`
}

/** Thousands-separated integer, so six-figure row counts stay readable. */
export function formatCount(n: number): string {
  return n.toLocaleString('en-US')
}
