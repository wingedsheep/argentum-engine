import { useCallback } from 'react'
import { useGameStore } from '../store/gameStore'
import type { EntityId, GameAction, LegalActionInfo } from '../types'

/**
 * Result of processing a card click.
 */
export type CardClickResult =
  | { type: 'noAction' }
  | { type: 'singleAction'; action: GameAction }
  | { type: 'multipleActions'; actions: LegalActionInfo[] }
  | { type: 'requiresTargeting'; action: GameAction; requiredTargets: number }
  | { type: 'requiresXSelection'; actionInfo: LegalActionInfo }

/**
 * Hook for handling card interactions.
 *
 * Provides functions to:
 * - Process card clicks
 * - Execute actions
 * - Handle action menu selection
 */
export function useInteraction() {
  const submitAction = useGameStore((state) => state.submitAction)
  const selectCard = useGameStore((state) => state.selectCard)
  const selectedCardId = useGameStore((state) => state.selectedCardId)
  const legalActions = useGameStore((state) => state.legalActions)
  const startXSelection = useGameStore((state) => state.startXSelection)
  const startTargeting = useGameStore((state) => state.startTargeting)

  /**
   * Get legal actions for a card.
   */
  const getCardActions = useCallback(
    (cardId: EntityId): LegalActionInfo[] => {
      return legalActions.filter((action) => {
        const a = action.action
        switch (a.type) {
          case 'PlayLand':
            return a.cardId === cardId
          case 'CastSpell':
            return a.cardId === cardId
          case 'ActivateAbility':
            return a.sourceId === cardId
          default:
            return false
        }
      })
    },
    [legalActions]
  )

  /**
   * Process a card click and determine what should happen.
   */
  const processCardClick = useCallback(
    (cardId: EntityId): CardClickResult => {
      const actions = getCardActions(cardId)

      if (actions.length === 0) {
        return { type: 'noAction' }
      }

      if (actions.length === 1) {
        const actionInfo = actions[0]!
        const action = actionInfo.action

        // Check if spell has X cost - needs X selection first
        if (action.type === 'CastSpell' && actionInfo.hasXCost) {
          return { type: 'requiresXSelection', actionInfo }
        }

        // Check if action requires targeting
        if (action.type === 'CastSpell' && (action.targets?.length ?? 0) > 0) {
          // For now, targets are pre-selected in legal actions
          // Future: would enter targeting mode
        }

        return { type: 'singleAction', action }
      }

      return { type: 'multipleActions', actions }
    },
    [getCardActions]
  )

  /**
   * Handle a card being clicked.
   */
  const handleCardClick = useCallback(
    (cardId: EntityId) => {
      const result = processCardClick(cardId)

      switch (result.type) {
        case 'noAction':
          // Clicking a card with no actions deselects
          selectCard(null)
          break

        case 'singleAction':
        case 'multipleActions':
        case 'requiresTargeting':
        case 'requiresXSelection':
          // Always open action menu so the player can see available actions
          selectCard(cardId)
          break
      }
    },
    [processCardClick, submitAction, selectCard, startXSelection]
  )

  /**
   * Execute a specific action from the action menu.
   * This checks for X cost spells and enters X selection mode if needed.
   */
  const executeAction = useCallback(
    (actionInfo: LegalActionInfo) => {
      const action = actionInfo.action

      // Check if spell has X cost - needs X selection first
      if (action.type === 'CastSpell' && actionInfo.hasXCost) {
        startXSelection({
          actionInfo,
          cardName: actionInfo.description.replace('Cast ', ''),
          minX: actionInfo.minX ?? 0,
          maxX: actionInfo.maxAffordableX ?? 0,
          selectedX: actionInfo.maxAffordableX ?? 0,
        })
        selectCard(null)
        return
      }

      // Check if spell requires sacrifice as additional cost
      if (action.type === 'CastSpell' && actionInfo.additionalCostInfo?.costType === 'SacrificePermanent') {
        const costInfo = actionInfo.additionalCostInfo
        startTargeting({
          action,
          validTargets: [...(costInfo.validSacrificeTargets ?? [])],
          selectedTargets: [],
          minTargets: costInfo.sacrificeCount ?? 1,
          maxTargets: costInfo.sacrificeCount ?? 1,
          isSacrificeSelection: true,
          pendingActionInfo: actionInfo,
        })
        selectCard(null)
        return
      }

      // Check if action requires targeting (for spells or activated abilities)
      if (actionInfo.requiresTargets && actionInfo.validTargets && actionInfo.validTargets.length > 0) {
        startTargeting({
          action,
          validTargets: [...actionInfo.validTargets],
          selectedTargets: [],
          minTargets: actionInfo.minTargets ?? actionInfo.targetCount ?? 1,
          maxTargets: actionInfo.targetCount ?? 1,
        })
        selectCard(null)
        return
      }

      submitAction(action)
      selectCard(null)
    },
    [submitAction, selectCard, startXSelection, startTargeting]
  )

  /**
   * Cancel the current selection/action.
   */
  const cancelAction = useCallback(() => {
    selectCard(null)
  }, [selectCard])

  /**
   * Pass priority.
   */
  const passPriority = useCallback(() => {
    const playerId = useGameStore.getState().playerId
    if (!playerId) return

    // Find pass priority action
    const passAction = legalActions.find(
      (a) => a.action.type === 'PassPriority'
    )

    if (passAction) {
      submitAction(passAction.action)
    }
  }, [legalActions, submitAction])

  return {
    selectedCardId,
    getCardActions,
    processCardClick,
    handleCardClick,
    executeAction,
    cancelAction,
    passPriority,
  }
}

/**
 * Hook to check if a specific action type is available.
 */
export function useHasAction(actionType: string): boolean {
  const legalActions = useGameStore((state) => state.legalActions)
  return legalActions.some((a) => a.action.type === actionType)
}

/**
 * Hook to check if player can pass priority.
 */
export function useCanPassPriority(): boolean {
  return useHasAction('PassPriority')
}
