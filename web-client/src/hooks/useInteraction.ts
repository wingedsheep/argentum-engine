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
  | { type: 'requiresConvokeSelection'; actionInfo: LegalActionInfo }

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
  const startConvokeSelection = useGameStore((state) => state.startConvokeSelection)

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
          case 'CycleCard':
            return a.cardId === cardId
          case 'ActivateAbility':
            return a.sourceId === cardId
          case 'TurnFaceUp':
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
      const actions = getCardActions(cardId)

      // Debug logging
      if (import.meta.env.DEV) {
        console.log('handleCardClick:', cardId, 'result:', result.type, 'actions:', actions)
      }

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
    [processCardClick, getCardActions, submitAction, selectCard, startXSelection]
  )

  /**
   * Execute a specific action from the action menu.
   * This checks for X cost spells, Convoke spells, etc. and enters appropriate selection mode.
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

      // Check if spell has Convoke and there are creatures to tap
      if (action.type === 'CastSpell' && actionInfo.hasConvoke && actionInfo.validConvokeCreatures && actionInfo.validConvokeCreatures.length > 0) {
        startConvokeSelection({
          actionInfo,
          cardName: actionInfo.description.replace('Cast ', ''),
          manaCost: actionInfo.manaCostString ?? '',
          selectedCreatures: [],
          validCreatures: actionInfo.validConvokeCreatures,
        })
        selectCard(null)
        return
      }

      // Check if spell or ability requires sacrifice as a cost
      if ((action.type === 'CastSpell' || action.type === 'ActivateAbility') && actionInfo.additionalCostInfo?.costType === 'SacrificePermanent') {
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

      // Check if ability requires tapping permanents as a cost
      if (action.type === 'ActivateAbility' && actionInfo.additionalCostInfo?.costType === 'TapPermanents') {
        const costInfo = actionInfo.additionalCostInfo
        startTargeting({
          action,
          validTargets: [...(costInfo.validTapTargets ?? [])],
          selectedTargets: [],
          minTargets: costInfo.tapCount ?? 1,
          maxTargets: costInfo.tapCount ?? 1,
          isSacrificeSelection: true,
          pendingActionInfo: actionInfo,
        })
        selectCard(null)
        return
      }

      // Check if action requires targeting (for spells or activated abilities)
      if (actionInfo.requiresTargets && actionInfo.validTargets && actionInfo.validTargets.length > 0) {
        // Check for multi-target spells (e.g., Wicked Pact)
        if (actionInfo.targetRequirements && actionInfo.targetRequirements.length > 1) {
          const firstReq = actionInfo.targetRequirements[0]!
          const targetingState = {
            action,
            validTargets: [...firstReq.validTargets],
            selectedTargets: [] as readonly import('../types').EntityId[],
            minTargets: firstReq.minTargets,
            maxTargets: firstReq.maxTargets,
            currentRequirementIndex: 0,
            allSelectedTargets: [] as readonly (readonly import('../types').EntityId[])[],
            targetRequirements: actionInfo.targetRequirements,
            ...(firstReq.targetZone ? { targetZone: firstReq.targetZone } : {}),
          }
          // Pass actionInfo for damage distribution spells
          if (actionInfo.requiresDamageDistribution) {
            startTargeting({ ...targetingState, pendingActionInfo: actionInfo })
          } else {
            startTargeting(targetingState)
          }
        } else {
          const targetingState = {
            action,
            validTargets: [...actionInfo.validTargets],
            selectedTargets: [] as readonly import('../types').EntityId[],
            minTargets: actionInfo.minTargets ?? actionInfo.targetCount ?? 1,
            maxTargets: actionInfo.targetCount ?? 1,
          }
          // Pass actionInfo for damage distribution spells (e.g., Forked Lightning)
          if (actionInfo.requiresDamageDistribution) {
            startTargeting({ ...targetingState, pendingActionInfo: actionInfo })
          } else {
            startTargeting(targetingState)
          }
        }
        selectCard(null)
        return
      }

      submitAction(action)
      selectCard(null)
    },
    [submitAction, selectCard, startXSelection, startTargeting, startConvokeSelection]
  )

  /**
   * Check if an action can be auto-executed (no special requirements).
   */
  const canAutoExecute = useCallback(
    (actionInfo: LegalActionInfo): boolean => {
      const action = actionInfo.action

      // X cost spells need selection
      if (action.type === 'CastSpell' && actionInfo.hasXCost) {
        return false
      }

      // Convoke spells with creatures need selection
      if (action.type === 'CastSpell' && actionInfo.hasConvoke && actionInfo.validConvokeCreatures && actionInfo.validConvokeCreatures.length > 0) {
        return false
      }

      // Sacrifice costs need selection
      if ((action.type === 'CastSpell' || action.type === 'ActivateAbility') && actionInfo.additionalCostInfo?.costType === 'SacrificePermanent') {
        return false
      }

      // Targeting spells need selection
      if (actionInfo.requiresTargets && actionInfo.validTargets && actionInfo.validTargets.length > 0) {
        return false
      }

      return true
    },
    []
  )

  /**
   * Handle a card being double-clicked.
   * Auto-executes simple actions, shows action menu for complex ones.
   */
  const handleDoubleClick = useCallback(
    (cardId: EntityId) => {
      const actions = getCardActions(cardId)

      // Debug logging
      if (import.meta.env.DEV) {
        console.log('handleDoubleClick:', cardId, 'actions:', actions.length)
      }

      if (actions.length === 0) {
        selectCard(null)
        return
      }

      if (actions.length === 1) {
        const actionInfo = actions[0]!
        if (canAutoExecute(actionInfo)) {
          // Auto-execute simple action
          submitAction(actionInfo.action)
          selectCard(null)
          return
        }
      }

      // Multiple actions or complex action - open the action menu
      selectCard(cardId)
    },
    [getCardActions, canAutoExecute, submitAction, selectCard, executeAction]
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
    handleDoubleClick,
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
