import { useCallback, useMemo } from 'react'
import { useGameStore } from '../store/gameStore'
import type { EntityId, GameAction, ClientGameState } from '../types'

/**
 * Target types that can be selected.
 */
export type TargetType = 'creature' | 'player' | 'permanent' | 'spell' | 'card'

/**
 * Targeting requirements for an action.
 */
export interface TargetingRequirements {
  minTargets: number
  maxTargets: number
  targetTypes: TargetType[]
  filter?: (entityId: EntityId, state: ClientGameState) => boolean
}

/**
 * Hook for managing targeting mode.
 */
export function useTargeting() {
  const targetingState = useGameStore((state) => state.targetingState)
  const startTargeting = useGameStore((state) => state.startTargeting)
  const addTarget = useGameStore((state) => state.addTarget)
  const cancelTargeting = useGameStore((state) => state.cancelTargeting)
  const confirmTargeting = useGameStore((state) => state.confirmTargeting)
  const gameState = useGameStore((state) => state.gameState)

  /**
   * Check if currently in targeting mode.
   */
  const isTargeting = targetingState !== null

  /**
   * Get the number of remaining targets needed.
   */
  const targetsRemaining = useMemo(() => {
    if (!targetingState) return 0
    return targetingState.requiredCount - targetingState.selectedTargets.length
  }, [targetingState])

  /**
   * Check if enough targets have been selected.
   */
  const hasEnoughTargets = targetsRemaining <= 0

  /**
   * Enter targeting mode for an action.
   */
  const enterTargetingMode = useCallback(
    (action: GameAction, validTargets: EntityId[], requiredCount: number) => {
      startTargeting({
        action,
        validTargets,
        selectedTargets: [],
        requiredCount,
      })
    },
    [startTargeting]
  )

  /**
   * Select a target.
   */
  const selectTarget = useCallback(
    (targetId: EntityId) => {
      if (!targetingState) return

      // Check if target is valid
      if (!targetingState.validTargets.includes(targetId)) {
        console.warn('Invalid target selected:', targetId)
        return
      }

      // Check if already selected
      if (targetingState.selectedTargets.includes(targetId)) {
        console.warn('Target already selected:', targetId)
        return
      }

      // Check if we already have enough targets
      if (targetingState.selectedTargets.length >= targetingState.requiredCount) {
        console.warn('Already have enough targets')
        return
      }

      addTarget(targetId)
    },
    [targetingState, addTarget]
  )

  /**
   * Check if an entity is a valid target.
   */
  const isValidTarget = useCallback(
    (entityId: EntityId): boolean => {
      if (!targetingState) return false
      return targetingState.validTargets.includes(entityId)
    },
    [targetingState]
  )

  /**
   * Check if an entity is currently selected as a target.
   */
  const isSelectedTarget = useCallback(
    (entityId: EntityId): boolean => {
      if (!targetingState) return false
      return targetingState.selectedTargets.includes(entityId)
    },
    [targetingState]
  )

  /**
   * Get the position of a target entity (for drawing arrows).
   */
  const getTargetPosition = useCallback(
    (entityId: EntityId): [number, number, number] | null => {
      if (!gameState) return null

      const card = gameState.cards[entityId]
      if (!card) {
        // Might be a player
        const player = gameState.players.find((p) => p.playerId === entityId)
        if (player) {
          // Player positions are fixed
          const isViewer = player.playerId === gameState.viewingPlayerId
          return isViewer ? [0, 0, 4] : [0, 0, -4]
        }
        return null
      }

      // For cards, we'd need to track their positions
      // This is a simplified version - in production, would get from layout
      return [0, 0, 0]
    },
    [gameState]
  )

  return {
    isTargeting,
    targetingState,
    targetsRemaining,
    hasEnoughTargets,
    enterTargetingMode,
    selectTarget,
    cancelTargeting,
    confirmTargeting,
    isValidTarget,
    isSelectedTarget,
    getTargetPosition,
  }
}

/**
 * Hook to get all valid targets for the current targeting state.
 */
export function useValidTargets(): readonly EntityId[] {
  const targetingState = useGameStore((state) => state.targetingState)
  return targetingState?.validTargets ?? []
}

/**
 * Hook to get selected targets.
 */
export function useSelectedTargets(): readonly EntityId[] {
  const targetingState = useGameStore((state) => state.targetingState)
  return targetingState?.selectedTargets ?? []
}
