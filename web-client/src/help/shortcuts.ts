/**
 * The one index of keyboard shortcuts.
 *
 * Shortcuts were previously implemented in seven unrelated files (`useMultiplayerView.ts`,
 * `ZonePiles.tsx`, `CardPreview.tsx`, `useDfcHoverFlip.tsx`, `ReplayPage.tsx`, `ActionMenu.tsx`,
 * `DeckbuilderPage.tsx`) and documented nowhere — several were completely undiscoverable. This
 * list is what `/help` renders; adding a shortcut means adding a row here in the same change.
 */
export interface Shortcut {
  id: string
  /** Rendered as `<kbd>` chips; split on " / " for alternatives. */
  keys: string
  label: string
  /** Where the shortcut is live — a phrase, not a file path. */
  where: string
}

export const SHORTCUTS: readonly Shortcut[] = [
  {
    id: 'opponent-boards',
    keys: '1 – 9',
    label: 'Focus an opponent’s board',
    where: 'Multiplayer games (3+ players)',
  },
  {
    id: 'overview',
    keys: '0',
    label: 'Toggle the table overview — every board side by side',
    where: 'Multiplayer games, desktop and landscape tablet',
  },
  {
    id: 'escape',
    keys: 'Esc',
    label: 'Cancel: unpin the camera, close a modal or zone browser, leave a replay',
    where: 'Everywhere',
  },
  {
    id: 'deck-browser',
    keys: 'D',
    label: 'Open or close the deck browser',
    where: 'In a game',
  },
  {
    id: 'flip-dfc',
    keys: 'F',
    label: 'Flip a double-faced card while previewing it',
    where: 'Any card preview — hand, battlefield, deckbuilder',
  },
  {
    id: 'replay-frame',
    keys: '← / →',
    label: 'Step one frame back or forward',
    where: 'Replay viewer',
  },
  {
    id: 'replay-play',
    keys: 'Space',
    label: 'Play / pause',
    where: 'Replay viewer',
  },
  {
    id: 'submit',
    keys: 'Enter',
    label: 'Submit the focused field or dialog',
    where: 'Name entry, join code, deck name, search',
  },
  {
    id: 'deckbuilder-remove',
    keys: 'Right-click / Shift-click',
    label: 'Remove one copy of a card',
    where: 'Deckbuilder',
  },
  {
    id: 'stack-yield-menu',
    keys: 'Right-click / long-press',
    label: 'Open the yield menu for an ability on the stack',
    where: 'In a game, on a stack item',
  },
]

export function shortcutById(id: string): Shortcut | undefined {
  return SHORTCUTS.find((s) => s.id === id)
}
