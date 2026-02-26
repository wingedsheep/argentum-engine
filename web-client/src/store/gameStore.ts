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

import { createConnectionSlice } from './slices/connectionSlice'
import { createGameplaySlice } from './slices/gameplaySlice'
import { createLobbySlice } from './slices/lobbySlice'
import { createDraftSlice } from './slices/draftSlice'
import { createUISlice } from './slices/uiSlice'
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
  DeckBuildingState,
  DraftState,
  LobbyState,
  TournamentState,
  SpectatingState,
  LogEntry,
  DrawAnimation,
  DamageAnimation,
  RevealAnimation,
  CoinFlipAnimation,
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
    ...createUISlice(...args),
  }))
)
