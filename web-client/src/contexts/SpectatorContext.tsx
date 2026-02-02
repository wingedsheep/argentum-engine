import { createContext, useContext } from 'react'

/**
 * Context value for spectator mode.
 * Provides information about the spectating state to child components.
 */
export interface SpectatorContextValue {
  /** Whether we're currently spectating a game */
  isSpectating: boolean
  /** Player 1's entity ID (null if not yet known) */
  player1Id: string | null
  /** Player 2's entity ID (null if not yet known) */
  player2Id: string | null
  /** Player 1's display name */
  player1Name: string
  /** Player 2's display name */
  player2Name: string
}

/**
 * Default context value (not spectating).
 */
const defaultValue: SpectatorContextValue = {
  isSpectating: false,
  player1Id: null,
  player2Id: null,
  player1Name: '',
  player2Name: '',
}

/**
 * Context for spectator mode information.
 * Used by components to check if they're in spectator mode and adjust behavior.
 */
export const SpectatorContext = createContext<SpectatorContextValue>(defaultValue)

/**
 * Hook to check if we're currently spectating.
 */
export function useIsSpectating(): boolean {
  return useContext(SpectatorContext).isSpectating
}

/**
 * Hook to get full spectator context value.
 */
export function useSpectatorContext(): SpectatorContextValue {
  return useContext(SpectatorContext)
}
