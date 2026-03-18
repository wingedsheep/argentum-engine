import { useCallback, useMemo } from 'react'
import { useGameStore } from '../store/gameStore'
import type { EntityId, GameAction, LegalActionInfo } from '../types'
import { resolveAction, needsInteraction } from './interaction/actionResolvers'
import type { ActionContext } from './interaction/actionResolvers'

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
  | { type: 'requiresCrewSelection'; actionInfo: LegalActionInfo }
  | { type: 'requiresDelveSelection'; actionInfo: LegalActionInfo }

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
  const startCrewSelection = useGameStore((state) => state.startCrewSelection)
  const startDelveSelection = useGameStore((state) => state.startDelveSelection)
  const startManaColorSelection = useGameStore((state) => state.startManaColorSelection)
  const startCounterDistribution = useGameStore((state) => state.startCounterDistribution)
  const startPipeline = useGameStore((state) => state.startPipeline)

  const actionContext: ActionContext = useMemo(() => ({
    submitAction,
    selectCard,
    startTargeting,
    startXSelection,
    startConvokeSelection,
    startCrewSelection,
    startDelveSelection,
    startManaColorSelection,
    startCounterDistribution,
    startPipeline,
  }), [submitAction, selectCard, startTargeting, startXSelection, startConvokeSelection, startCrewSelection, startDelveSelection, startManaColorSelection, startCounterDistribution, startPipeline])

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
          case 'TypecycleCard':
            return a.cardId === cardId
          case 'ActivateAbility':
            return a.sourceId === cardId
          case 'TurnFaceUp':
            return a.sourceId === cardId
          case 'CrewVehicle':
            return a.vehicleId === cardId
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

        // Check if spell or ability or morph has X cost - needs X selection first
        if ((action.type === 'CastSpell' || action.type === 'ActivateAbility' || action.type === 'TurnFaceUp') && actionInfo.hasXCost) {
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
   * Uses the resolver pipeline to check for special requirements (X cost, Convoke, etc.)
   * and enters the appropriate selection mode, or submits directly if none match.
   */
  const executeAction = useCallback(
    (actionInfo: LegalActionInfo) => {
      if (!resolveAction(actionInfo, actionContext)) {
        submitAction(actionInfo.action)
        selectCard(null)
      }
    },
    [actionContext, submitAction, selectCard]
  )

  /**
   * Check if an action can be auto-executed (no special requirements).
   */
  const canAutoExecute = useCallback(
    (actionInfo: LegalActionInfo): boolean => {
      return !needsInteraction(actionInfo)
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

        // For cycling lands where play land is unavailable, always show action menu
        // so the player sees the grayed-out "Play land" option alongside "Cycle"
        if (actionInfo.action.type === 'CycleCard' || actionInfo.action.type === 'TypecycleCard') {
          const gameState = useGameStore.getState().gameState
          const card = gameState?.cards[cardId]
          if (card?.cardTypes.includes('LAND')) {
            selectCard(cardId)
            return
          }
        }

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
