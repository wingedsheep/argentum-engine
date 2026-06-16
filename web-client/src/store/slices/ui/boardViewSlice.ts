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
  /**
   * Two-Headed Giant (CR 810) seat → team-index map, keyed by playerId. Empty in a non-team
   * game. `teamIndex` only arrives once, in the `GameStarted` roster, so the client stamps this
   * map at game start and reads it for the whole game (team-grouped rail, team colors, ally
   * board, shared-life headers).
   */
  teamByPlayerId: Readonly<Record<EntityId, number>>
  /**
   * True when teammates share one life total (Two-Headed Giant — CR 810). False for Team vs. Team
   * (CR 808), where each player has their own life even though they are on a team. Drives whether
   * the rail shows a single shared-life team header or per-player life.
   */
  teamSharedLife: boolean
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
  /**
   * Stamp the seat → team map from the game-start roster (2HG / Team vs. Team). Pass an empty map
   * for a non-team game (the default). [sharedLife] is true only when the team shares one life total
   * (2HG); Team vs. Team passes false. Persists until the next reset.
   */
  setSeatTeams: (teamByPlayerId: Record<EntityId, number>, sharedLife?: boolean) => void
  /** Reset on game start / leave. */
  resetBoardView: () => void
}

export type BoardViewSlice = BoardViewSliceState & BoardViewSliceActions

export const createBoardViewSlice: SliceCreator<BoardViewSlice> = (set, get) => ({
  viewedOpponentId: null,
  viewPinned: false,
  followAction: loadFollowAction(),
  spectatorBottomSeatId: null,
  teamByPlayerId: {},
  teamSharedLife: false,

  viewOpponent: (playerId, opts) => {
    const { gameState, playerId: ownId } = get()
    // Only opponents occupy the viewed slot — your half is sacred.
    if (playerId === ownId && !get().spectatingState) return
    if (gameState && !gameState.players.some((p) => p.playerId === playerId)) return
    const pin = opts?.pin ?? true
    // Pinning a board and follow-the-action are mutually exclusive: pinning turns follow off
    // (the camera is locked), so the Follow toggle reflects that rather than lying "on".
    set({ viewedOpponentId: playerId, viewPinned: pin, ...(pin ? { followAction: false } : {}) })
  },

  unpinView: () => set({ viewPinned: false }),

  toggleFollowAction: () => {
    const next = !get().followAction
    try {
      localStorage.setItem(FOLLOW_ACTION_KEY, String(next))
    } catch {
      // Private mode — setting just won't persist.
    }
    // Turning follow on releases any manual pin (the two are mutually exclusive).
    set({ followAction: next, ...(next ? { viewPinned: false } : {}) })
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

  setSeatTeams: (teamByPlayerId, sharedLife = false) =>
    set({ teamByPlayerId, teamSharedLife: sharedLife }),

  resetBoardView: () =>
    set({ viewedOpponentId: null, viewPinned: false, spectatorBottomSeatId: null, teamByPlayerId: {}, teamSharedLife: false }),
})
