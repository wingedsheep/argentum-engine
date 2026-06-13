/**
 * Board-view sub-slice — which opponent's board occupies the viewed slot in a
 * multiplayer (3-4 player) game, plus the follow-the-action camera settings.
 *
 * Written by rail-chip clicks, keyboard (1/2/3), swipe, and the follow-the-action
 * rules. In a 2-player game none of this state is consulted — the sole opponent is
 * always the viewed board and the rail doesn't render.
 */
import type { SliceCreator, EntityId } from '../types'

const FOLLOW_ACTION_KEY = 'argentum-follow-action'

function loadFollowAction(): boolean {
  try {
    return localStorage.getItem(FOLLOW_ACTION_KEY) !== 'false'
  } catch {
    return true
  }
}

export interface BoardViewSliceState {
  /**
   * The opponent whose board is slid into view. Null = default (first opponent
   * after the viewing player in turn order). Selectors fall back when this player
   * has lost or left the game.
   */
  viewedOpponentId: EntityId | null
  /**
   * True when the player manually selected a board — suspends follow-the-action
   * until unpinned (re-click the viewed chip / Esc).
   */
  viewPinned: boolean
  /** Follow-the-action camera setting (persisted). Default on. */
  followAction: boolean
  /**
   * Spectator/replay: which seat anchors the bottom half of the board. Null =
   * the stream's default (`spectatingState.player1Id`). Set by the spectator
   * seat-switcher in multiplayer pods.
   */
  spectatorBottomSeatId: EntityId | null
}

export interface BoardViewSliceActions {
  /** Manual board select (chip click, keyboard, swipe). Pins by default. */
  viewOpponent: (playerId: EntityId, opts?: { pin?: boolean }) => void
  /** Unpin the camera (re-enables follow-the-action). */
  unpinView: () => void
  /** Toggle the follow-the-action setting (persisted). */
  toggleFollowAction: () => void
  /**
   * Follow-the-action write. Refused at the handler (not at render time) when the
   * view is pinned, following is off, or the player has any pending input — the
   * camera must never move under an in-progress selection.
   */
  followViewTo: (playerId: EntityId) => void
  /** Spectator/replay: anchor the bottom half to a seat (null = stream default). */
  setSpectatorBottomSeat: (playerId: EntityId | null) => void
  /** Reset on game start / leave. */
  resetBoardView: () => void
}

export type BoardViewSlice = BoardViewSliceState & BoardViewSliceActions

export const createBoardViewSlice: SliceCreator<BoardViewSlice> = (set, get) => ({
  viewedOpponentId: null,
  viewPinned: false,
  followAction: loadFollowAction(),
  spectatorBottomSeatId: null,

  viewOpponent: (playerId, opts) => {
    const { gameState, playerId: ownId } = get()
    // Only opponents occupy the viewed slot — your half is sacred.
    if (playerId === ownId && !get().spectatingState) return
    if (gameState && !gameState.players.some((p) => p.playerId === playerId)) return
    set({ viewedOpponentId: playerId, viewPinned: opts?.pin ?? true })
  },

  unpinView: () => set({ viewPinned: false }),

  toggleFollowAction: () => {
    const next = !get().followAction
    try {
      localStorage.setItem(FOLLOW_ACTION_KEY, String(next))
    } catch {
      // Private mode — setting just won't persist.
    }
    set({ followAction: next })
  },

  followViewTo: (playerId) => {
    const state = get()
    if (!state.followAction || state.viewPinned) return
    if (state.viewedOpponentId === playerId) return
    // Never move the camera while the player has a pending input or an
    // in-progress selection (stale-UI-suppression applies to camera movement).
    // declare-blockers is the one exception: sliding the attacker's board in is
    // exactly what a defender needs, and blocking happens on your own (always
    // visible) half — so only an in-progress *attack* declaration blocks it.
    if (
      state.targetingState ||
      state.decisionSelectionState ||
      state.pendingDecision ||
      state.combatState?.mode === 'declareAttackers' ||
      state.distributeState ||
      state.counterDistributionState ||
      state.manaSelectionState ||
      state.delveSelectionState ||
      state.tapForPowerSelectionState ||
      state.pipelineState
    ) {
      return
    }
    set({ viewedOpponentId: playerId })
  },

  setSpectatorBottomSeat: (playerId) =>
    set({ spectatorBottomSeatId: playerId, viewedOpponentId: null, viewPinned: false }),

  resetBoardView: () =>
    set({ viewedOpponentId: null, viewPinned: false, spectatorBottomSeatId: null }),
})
