/**
 * Board-view sub-slice — which opponent's board occupies the viewed slot in a
 * multiplayer (3-4 player) game, plus the follow-the-action camera settings.
 *
 * Written by rail-chip clicks, keyboard (1/2/3), swipe, and the follow-the-action
 * rules. In a 2-player game none of this state is consulted — the sole opponent is
 * always the viewed board and the rail doesn't render.
 */
import type { SliceCreator, EntityId, GameStore } from '../types'

const FOLLOW_ACTION_KEY = 'argentum-follow-action'

/**
 * True while the player has any pending input or in-progress selection. The camera —
 * whether follow-the-action, the combat defender-focus split, or any other automatic
 * view change — must never move under an in-progress selection (stale-UI suppression
 * applies to camera movement). Shared by `followViewTo` and the render-time camera hooks.
 */
export function hasPendingInputSelection(state: GameStore): boolean {
  return !!(
    state.targetingState ||
    state.decisionSelectionState ||
    state.pendingDecision ||
    state.distributeState ||
    state.counterDistributionState ||
    state.manaSelectionState ||
    state.delveSelectionState ||
    state.tapForPowerSelectionState ||
    state.pipelineState
  )
}

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
   * Table overview: every living opponent's board is shown side-by-side (cards shrink to
   * fit) instead of the one-board sliding camera. Toggled from the rail or the `0` key;
   * selecting a single board (chip click / 1-9) exits back to the focused camera.
   */
  overviewMode: boolean
  /**
   * MTGO-style per-opponent collapse in the table overview: seats whose cell is folded
   * to a narrow tab so the remaining boards split the freed width. Only consulted while
   * the overview is on; the focused camera and the combat split ignore it. Seats stay
   * collapsed across overview toggles until reset (new game / leave).
   */
  collapsedSeats: readonly EntityId[]
  /**
   * The local player was eliminated from a multiplayer game and chose "Keep watching"
   * on the defeat overlay: their dead bottom half collapses, the freed space goes to the
   * opponent boards, and all action UI hides. Cleared on reset (new game / leave).
   */
  eliminatedSpectating: boolean
  /**
   * Eliminated spectator's chosen bottom seat: a *living* player whose board fills the
   * (otherwise collapsed) bottom half, the way spectating anchors a bottom seat. Null =
   * no bottom board (the freed height flows to the opponent strip). Ignored outside the
   * eliminated-spectating layout; self-heals to null rendering-side if the seat dies.
   */
  eliminatedBottomSeatId: EntityId | null
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
  /**
   * Team-split board layout (team games only): the viewer's team fills the bottom half and the
   * enemy team the top half, each a multi-board strip (e.g. 3v3 = 3 boards top, 3 bottom) with the
   * same per-board collapse as the table overview. Auto-defaulted on when spectating a team game;
   * an opt-in toggle when playing. Ignored in non-team games.
   */
  teamSplit: boolean
}

export interface BoardViewSliceActions {
  /** Manual board select (chip click, keyboard, swipe). Pins by default. */
  viewOpponent: (playerId: EntityId, opts?: { pin?: boolean }) => void
  /** Unpin the camera (re-enables follow-the-action). */
  unpinView: () => void
  /** Toggle the follow-the-action setting (persisted). */
  toggleFollowAction: () => void
  /** Toggle the all-boards table overview. */
  toggleOverviewMode: () => void
  /** Fold/unfold one opponent's overview cell (MTGO-style per-board collapse). */
  toggleSeatCollapsed: (playerId: EntityId) => void
  /**
   * "Keep watching" after being eliminated from a multiplayer game: dismisses the defeat
   * overlay and enters the spectator layout (overview on, dead bottom half collapsed).
   */
  enterEliminatedSpectate: () => void
  /**
   * Eliminated spectator: anchor the bottom half to a living player's board
   * (null = collapse the bottom half again).
   */
  setEliminatedBottomSeat: (playerId: EntityId | null) => void
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
  /** Toggle the team-split board layout (team games only). */
  toggleTeamSplit: () => void
  /** Reset on game start / leave. */
  resetBoardView: () => void
}

export type BoardViewSlice = BoardViewSliceState & BoardViewSliceActions

export const createBoardViewSlice: SliceCreator<BoardViewSlice> = (set, get) => ({
  viewedOpponentId: null,
  viewPinned: false,
  followAction: loadFollowAction(),
  overviewMode: false,
  collapsedSeats: [],
  eliminatedSpectating: false,
  eliminatedBottomSeatId: null,
  spectatorBottomSeatId: null,
  teamByPlayerId: {},
  teamSharedLife: false,
  teamSplit: false,

  viewOpponent: (playerId, opts) => {
    const { gameState, playerId: ownId } = get()
    // Only opponents occupy the viewed slot — your half is sacred.
    if (playerId === ownId && !get().spectatingState) return
    if (gameState && !gameState.players.some((p) => p.playerId === playerId)) return
    // The eliminated spectator's chosen bottom board is already fully visible at the
    // bottom — it never also occupies the viewed strip slot.
    if (get().eliminatedSpectating && playerId === get().eliminatedBottomSeatId) return
    const pin = opts?.pin ?? true
    // Pinning a board and follow-the-action are mutually exclusive: pinning turns follow off
    // (the camera is locked), so the Follow toggle reflects that rather than lying "on".
    // Picking a single board also exits the table overview — it *is* the focus gesture.
    set({
      viewedOpponentId: playerId,
      viewPinned: pin,
      overviewMode: false,
      ...(pin ? { followAction: false } : {}),
    })
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

  toggleOverviewMode: () => {
    // Entering the overview releases any pin (there is no single board to pin).
    const next = !get().overviewMode
    set({ overviewMode: next, ...(next ? { viewPinned: false } : {}) })
  },

  toggleSeatCollapsed: (playerId) => {
    const prev = get().collapsedSeats
    set({
      collapsedSeats: prev.includes(playerId)
        ? prev.filter((id) => id !== playerId)
        : [...prev, playerId],
    })
  },

  enterEliminatedSpectate: () =>
    set({ eliminatedSpectating: true, overviewMode: true, viewPinned: false, gameOverState: null }),

  setEliminatedBottomSeat: (playerId) => set({ eliminatedBottomSeatId: playerId }),

  followViewTo: (playerId) => {
    const state = get()
    if (!state.followAction || state.viewPinned) return
    if (state.viewedOpponentId === playerId) return
    // Never move the camera while the player has a pending input or an
    // in-progress selection (stale-UI-suppression applies to camera movement).
    // declare-blockers is the one exception: sliding the attacker's board in is
    // exactly what a defender needs, and blocking happens on your own (always
    // visible) half — so only an in-progress *attack* declaration blocks it.
    if (hasPendingInputSelection(state) || state.combatState?.mode === 'declareAttackers') {
      return
    }
    set({ viewedOpponentId: playerId })
  },

  setSpectatorBottomSeat: (playerId) =>
    set({ spectatorBottomSeatId: playerId, viewedOpponentId: null, viewPinned: false }),

  setSeatTeams: (teamByPlayerId, sharedLife = false) =>
    set({ teamByPlayerId, teamSharedLife: sharedLife }),

  toggleTeamSplit: () => set({ teamSplit: !get().teamSplit }),

  resetBoardView: () =>
    set({
      viewedOpponentId: null,
      viewPinned: false,
      overviewMode: false,
      collapsedSeats: [],
      eliminatedSpectating: false,
      eliminatedBottomSeatId: null,
      spectatorBottomSeatId: null,
      teamByPlayerId: {},
      teamSharedLife: false,
      teamSplit: false,
    }),
})
