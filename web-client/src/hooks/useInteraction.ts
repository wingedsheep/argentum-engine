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
  const startManaColorSelection = useGameStore((state) => state.startManaColorSelection)

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

        // Check if spell or ability has X cost - needs X selection first
        if ((action.type === 'CastSpell' || action.type === 'ActivateAbility') && actionInfo.hasXCost) {
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

      // Check if spell or ability has X cost - needs X selection first
      if ((action.type === 'CastSpell' || action.type === 'ActivateAbility') && actionInfo.hasXCost) {
        startXSelection({
          actionInfo,
          cardName: action.type === 'CastSpell'
            ? actionInfo.description.replace('Cast ', '')
            : actionInfo.description,
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

      // Check if TurnFaceUp requires returning a permanent (non-mana morph cost)
      if (action.type === 'TurnFaceUp' && actionInfo.additionalCostInfo?.costType === 'Sacrifice') {
        const costInfo = actionInfo.additionalCostInfo
        const returnCount = costInfo.sacrificeCount ?? 1
        const validTargets = costInfo.validSacrificeTargets ?? []

        // Always show selection modal (don't auto-select even with exactly one target)
        startTargeting({
          action,
          validTargets: [...validTargets],
          selectedTargets: [],
          minTargets: returnCount,
          maxTargets: returnCount,
          isSacrificeSelection: true,
          pendingActionInfo: actionInfo,
        })
        selectCard(null)
        return
      }

      // Check if spell or ability requires sacrifice as a cost
      if ((action.type === 'CastSpell' || action.type === 'ActivateAbility') &&
          (actionInfo.additionalCostInfo?.costType === 'SacrificePermanent' || actionInfo.additionalCostInfo?.costType === 'SacrificeSelf')) {
        const costInfo = actionInfo.additionalCostInfo
        const sacrificeCount = costInfo.sacrificeCount ?? 1
        const validSacTargets = costInfo.validSacrificeTargets ?? []

        // Auto-select for SacrificeSelf (obvious — always the source itself)
        if (costInfo.costType === 'SacrificeSelf' && validSacTargets.length === sacrificeCount) {
          if (action.type === 'CastSpell') {
            const actionWithCost = {
              ...action,
              additionalCostPayment: {
                sacrificedPermanents: [...validSacTargets],
              },
            }
            if (actionInfo.requiresTargets && actionInfo.validTargets && actionInfo.validTargets.length > 0) {
              startTargeting({
                action: actionWithCost,
                validTargets: [...actionInfo.validTargets],
                selectedTargets: [],
                minTargets: actionInfo.minTargets ?? actionInfo.targetCount ?? 1,
                maxTargets: actionInfo.targetCount ?? 1,
                ...(actionInfo.requiresDamageDistribution ? { pendingActionInfo: actionInfo } : {}),
              })
            } else {
              submitAction(actionWithCost)
            }
          } else if (action.type === 'ActivateAbility') {
            const actionWithCost = {
              ...action,
              costPayment: { sacrificedPermanents: [...validSacTargets] },
            }
            if (actionInfo.requiresTargets && actionInfo.validTargets && actionInfo.validTargets.length > 0) {
              startTargeting({
                action: actionWithCost,
                validTargets: [...actionInfo.validTargets],
                selectedTargets: [],
                minTargets: actionInfo.minTargets ?? actionInfo.targetCount ?? 1,
                maxTargets: actionInfo.targetCount ?? 1,
                ...(actionInfo.requiresDamageDistribution ? { pendingActionInfo: actionInfo } : {}),
              })
            } else if (actionInfo.requiresManaColorChoice) {
              startManaColorSelection({ action: actionWithCost })
            } else {
              submitAction(actionWithCost)
            }
          }
          selectCard(null)
          return
        }

        // SacrificePermanent always shows the sacrifice selection modal
        startTargeting({
          action,
          validTargets: [...validSacTargets],
          selectedTargets: [],
          minTargets: sacrificeCount,
          maxTargets: sacrificeCount,
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
          isTapPermanentSelection: true,
          pendingActionInfo: actionInfo,
        })
        selectCard(null)
        return
      }

      // Check if ability requires bouncing a permanent as a cost
      if (action.type === 'ActivateAbility' && actionInfo.additionalCostInfo?.costType === 'BouncePermanent') {
        const costInfo = actionInfo.additionalCostInfo
        startTargeting({
          action,
          validTargets: [...(costInfo.validBounceTargets ?? [])],
          selectedTargets: [],
          minTargets: costInfo.bounceCount ?? 1,
          maxTargets: costInfo.bounceCount ?? 1,
          isSacrificeSelection: true,
          isBounceSelection: true,
          pendingActionInfo: actionInfo,
        })
        selectCard(null)
        return
      }

      // Check if spell or ability requires discarding a card as a cost
      if ((action.type === 'CastSpell' || action.type === 'ActivateAbility') &&
          actionInfo.additionalCostInfo?.costType === 'DiscardCard') {
        const costInfo = actionInfo.additionalCostInfo
        startTargeting({
          action,
          validTargets: [...(costInfo.validDiscardTargets ?? [])],
          selectedTargets: [],
          minTargets: costInfo.discardCount ?? 1,
          maxTargets: costInfo.discardCount ?? 1,
          isSacrificeSelection: true,
          isDiscardSelection: true,
          pendingActionInfo: actionInfo,
        })
        selectCard(null)
        return
      }

      // Check if spell or ability requires exiling cards from graveyard as a cost
      if ((action.type === 'CastSpell' || action.type === 'ActivateAbility') &&
          actionInfo.additionalCostInfo?.costType === 'ExileFromGraveyard') {
        const costInfo = actionInfo.additionalCostInfo
        startTargeting({
          action,
          validTargets: [...(costInfo.validExileTargets ?? [])],
          selectedTargets: [],
          minTargets: costInfo.exileMinCount ?? 1,
          maxTargets: costInfo.exileMaxCount ?? costInfo.validExileTargets?.length ?? 1,
          isSacrificeSelection: true,
          pendingActionInfo: actionInfo,
          targetZone: 'Graveyard',
          targetDescription: costInfo.description,
        })
        selectCard(null)
        return
      }

      // Check if ability requires mana color selection (e.g., "add one mana of any color")
      if (action.type === 'ActivateAbility' && actionInfo.requiresManaColorChoice) {
        startManaColorSelection({ action })
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
            targetDescription: firstReq.description,
            totalRequirements: actionInfo.targetRequirements.length,
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
    [submitAction, selectCard, startXSelection, startTargeting, startConvokeSelection, startManaColorSelection]
  )

  /**
   * Check if an action can be auto-executed (no special requirements).
   */
  const canAutoExecute = useCallback(
    (actionInfo: LegalActionInfo): boolean => {
      const action = actionInfo.action

      // X cost spells/abilities need selection
      if ((action.type === 'CastSpell' || action.type === 'ActivateAbility') && actionInfo.hasXCost) {
        return false
      }

      // Convoke spells with creatures need selection
      if (action.type === 'CastSpell' && actionInfo.hasConvoke && actionInfo.validConvokeCreatures && actionInfo.validConvokeCreatures.length > 0) {
        return false
      }

      // TurnFaceUp with non-mana morph cost needs selection (e.g., return a Bird)
      if (action.type === 'TurnFaceUp' && actionInfo.additionalCostInfo) {
        return false
      }

      // SacrificeSelf is auto-executable (obvious target — the source itself)
      // SacrificePermanent always needs selection (non-obvious targets like "Sacrifice a Goblin")
      if ((action.type === 'CastSpell' || action.type === 'ActivateAbility') && actionInfo.additionalCostInfo?.costType === 'SacrificePermanent') {
        return false
      }

      // TapPermanents costs need selection (e.g., Birchlore Rangers)
      if (action.type === 'ActivateAbility' && actionInfo.additionalCostInfo?.costType === 'TapPermanents') {
        return false
      }

      // BouncePermanent costs need selection (e.g., Wirewood Symbiote)
      if (action.type === 'ActivateAbility' && actionInfo.additionalCostInfo?.costType === 'BouncePermanent') {
        return false
      }

      // DiscardCard costs need selection (e.g., Undead Gladiator)
      if ((action.type === 'CastSpell' || action.type === 'ActivateAbility') && actionInfo.additionalCostInfo?.costType === 'DiscardCard') {
        return false
      }

      // ExileFromGraveyard costs need selection (e.g., Chill Haunting)
      if ((action.type === 'CastSpell' || action.type === 'ActivateAbility') && actionInfo.additionalCostInfo?.costType === 'ExileFromGraveyard') {
        return false
      }

      // Targeting spells need selection
      if (actionInfo.requiresTargets && actionInfo.validTargets && actionInfo.validTargets.length > 0) {
        return false
      }

      // Cycling/typecycling should always show the action menu so the player
      // can choose between casting and cycling
      if (action.type === 'CycleCard' || action.type === 'TypecycleCard') {
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
