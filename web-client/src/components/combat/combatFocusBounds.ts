import { create } from 'zustand'

/** A viewport rectangle, in client coordinates. */
export interface ViewportBounds {
  readonly left: number
  readonly top: number
  readonly right: number
  readonly bottom: number
}

/**
 * The on-screen box around the combat clique of the creature under the cursor (see
 * computeCombatClique), published by CombatArrows so the hover preview can open beside it instead
 * of on top of the blocks it is trying to explain.
 *
 * Its own store rather than a field on the game store: it changes on every hover in combat, and a
 * game-store write runs every card's selectors for a value only the preview reads.
 */
export const useCombatFocusBounds = create<{ bounds: ViewportBounds | null }>(() => ({ bounds: null }))

export function setCombatFocusBounds(next: ViewportBounds | null): void {
  const prev = useCombatFocusBounds.getState().bounds
  if (prev === next) return
  if (prev && next && prev.left === next.left && prev.top === next.top && prev.right === next.right && prev.bottom === next.bottom) return
  useCombatFocusBounds.setState({ bounds: next })
}
