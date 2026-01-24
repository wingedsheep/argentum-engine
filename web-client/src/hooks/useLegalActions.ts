import { useMemo } from 'react'
import { useGameStore } from '../store/gameStore'
import type { EntityId, LegalActionInfo, GameAction } from '../types'

/**
 * Categorized legal actions for the current player.
 */
export interface CategorizedActions {
  /** Land plays available */
  landPlays: LegalActionInfo[]
  /** Spells that can be cast */
  spellCasts: LegalActionInfo[]
  /** Activated abilities (including mana abilities) */
  activatedAbilities: LegalActionInfo[]
  /** Pass priority action */
  passPriority: LegalActionInfo | null
  /** Declare attackers action (available during declare attackers step) */
  declareAttackers: LegalActionInfo | null
  /** Declare blockers action (available during declare blockers step) */
  declareBlockers: LegalActionInfo | null
  /** All other actions */
  other: LegalActionInfo[]
}

/**
 * Hook to get categorized legal actions.
 */
export function useCategorizedActions(): CategorizedActions {
  const legalActions = useGameStore((state) => state.legalActions)

  return useMemo(() => {
    const result: CategorizedActions = {
      landPlays: [],
      spellCasts: [],
      activatedAbilities: [],
      passPriority: null,
      declareAttackers: null,
      declareBlockers: null,
      other: [],
    }

    for (const action of legalActions) {
      switch (action.action.type) {
        case 'PlayLand':
          result.landPlays.push(action)
          break
        case 'CastSpell':
          result.spellCasts.push(action)
          break
        case 'ActivateAbility':
          result.activatedAbilities.push(action)
          break
        case 'PassPriority':
          result.passPriority = action
          break
        case 'DeclareAttackers':
          result.declareAttackers = action
          break
        case 'DeclareBlockers':
          result.declareBlockers = action
          break
        default:
          result.other.push(action)
      }
    }

    return result
  }, [legalActions])
}

/**
 * Hook to get legal actions for a specific card.
 */
export function useCardActions(cardId: EntityId | null): LegalActionInfo[] {
  const legalActions = useGameStore((state) => state.legalActions)

  return useMemo(() => {
    if (!cardId) return []

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
  }, [legalActions, cardId])
}

/**
 * Hook to check if the player can play any land.
 */
export function useCanPlayLand(): boolean {
  const actions = useCategorizedActions()
  return actions.landPlays.length > 0
}

/**
 * Hook to check if the player can cast any spell.
 */
export function useCanCastSpell(): boolean {
  const actions = useCategorizedActions()
  return actions.spellCasts.length > 0
}

/**
 * Group actions by the card they affect.
 */
export function useActionsGroupedByCard(): Map<EntityId, LegalActionInfo[]> {
  const legalActions = useGameStore((state) => state.legalActions)

  return useMemo(() => {
    const grouped = new Map<EntityId, LegalActionInfo[]>()

    for (const action of legalActions) {
      const cardId = getActionCardId(action.action)
      if (cardId) {
        const existing = grouped.get(cardId) ?? []
        existing.push(action)
        grouped.set(cardId, existing)
      }
    }

    return grouped
  }, [legalActions])
}

/**
 * Get the primary card ID associated with an action.
 */
function getActionCardId(action: GameAction): EntityId | null {
  switch (action.type) {
    case 'PlayLand':
      return action.cardId
    case 'CastSpell':
      return action.cardId
    case 'ActivateAbility':
      return action.sourceId
    default:
      return null
  }
}
