/**
 * Main game store - combines all slices into a single Zustand store.
 *
 * The store is organized into domain-specific slices:
 * - connectionSlice: WebSocket connection, authentication, reconnection
 * - gameplaySlice: Game state, actions, events, mulligan, decisions
 * - lobbySlice: Tournament lobbies, spectating, tournament state
 * - draftSlice: Sealed/draft deck building
 * - uiSlice: Local UI state (targeting, combat, selections, animations)
 */
import { create } from 'zustand'
import { subscribeWithSelector } from 'zustand/middleware'
import { usePreferences } from './preferencesStore'
import { getWebSocket } from './slices/shared'
import { createSetStopOverridesMessage } from '@/types'

import { createConnectionSlice } from '@/store/slices'
import { createGameplaySlice } from '@/store/slices'
import { createLobbySlice } from '@/store/slices'
import { createDraftSlice } from '@/store/slices'
import { createQuickGameLobbySlice } from '@/store/slices'
import { createUISlice } from './slices/ui'
import type { GameStore } from './slices/types'

// Re-export types for backward compatibility
export type {
  MulliganCardInfo,
  MulliganState,
  TargetingState,
  CombatState,
  GameOverState,
  DecisionSelectionState,
  ErrorState,
  XSelectionState,
  DamageDistributionState,
  ConvokeCreatureSelection,
  ConvokeSelectionState,
  TapForGenericSelectionState,
  TapForPowerSelectionState,
  DelveSelectionState,
  DeckBuildingState,
  DraftState,
  WinstonDraftState,
  LobbyState,
  TournamentState,
  FfaState,
  SpectatingState,
  LogEntry,
  DrawAnimation,
  DamageAnimation,
  RevealAnimation,
  CoinFlipAnimation,
  TargetReselectedAnimation,
  MatchIntro,
  GameStore,
} from './slices/types'

/**
 * Main Zustand store for game state.
 * Combines all slices using the subscribeWithSelector middleware for granular subscriptions.
 */
export const useGameStore = create<GameStore>()(
  subscribeWithSelector((...args) => ({
    ...createConnectionSlice(...args),
    ...createGameplaySlice(...args),
    ...createLobbySlice(...args),
    ...createDraftSlice(...args),
    ...createQuickGameLobbySlice(...args),
    ...createUISlice(...args),
  }))
)

// The preferences page (or the in-game settings dialog, or another device via the account sync) can
// change a gameplay preference while a game store is alive: keep the live game in step. Stops go to
// the server too, so a stop added from the dialog applies to the game in progress. The starting
// priority mode deliberately does not — it only shapes the next game.
usePreferences.subscribe((state, prev) => {
  const next = state.prefs.gameplay
  const before = prev.prefs.gameplay
  if (next === before) return
  const game = useGameStore.getState()
  const patch: Partial<GameStore> = {}
  if (game.autoTapEnabled !== next.autoTap) patch.autoTapEnabled = next.autoTap
  if (game.followAction !== next.followAction) patch.followAction = next.followAction
  const stopsChanged =
    !sameSteps(game.stopOverrides.myTurnStops, next.myTurnStops) ||
    !sameSteps(game.stopOverrides.opponentTurnStops, next.opponentTurnStops)
  if (stopsChanged && game.gameState && !game.spectatingState) {
    patch.stopOverrides = { myTurnStops: next.myTurnStops, opponentTurnStops: next.opponentTurnStops }
    getWebSocket()?.send(createSetStopOverridesMessage(next.myTurnStops, next.opponentTurnStops))
  }
  if (Object.keys(patch).length) useGameStore.setState(patch)
})

function sameSteps(a: readonly string[], b: readonly string[]): boolean {
  return a.length === b.length && a.every((x) => b.includes(x))
}
