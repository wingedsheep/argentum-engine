/** `/deckbuilder/abc` → `deckbuilder`; `/` → `home`; `/u/:id` → `player-profile`. Never ids or typed text. */
export function pageForPath(pathname: string): string {
  const first = pathname.split('/').find((segment) => segment.length > 0)
  if (!first) return 'home'
  if (first === 'u') return 'player-profile'
  return first.toLowerCase()
}
