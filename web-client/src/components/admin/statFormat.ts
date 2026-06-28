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
