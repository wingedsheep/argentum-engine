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
  /** Mana abilities that can be activated */
  manaAbilities: LegalActionInfo[]
  /** Other activated abilities */
  activatedAbilities: LegalActionInfo[]
  /** Attack declarations */
  attackers: LegalActionInfo[]
  /** Block declarations */
  blockers: LegalActionInfo[]
  /** Pass priority action */
  passPriority: LegalActionInfo | null
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
      manaAbilities: [],
      activatedAbilities: [],
      attackers: [],
      blockers: [],
      passPriority: null,
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
        case 'ActivateManaAbility':
          result.manaAbilities.push(action)
          break
        case 'DeclareAttacker':
          result.attackers.push(action)
          break
        case 'DeclareBlocker':
          result.blockers.push(action)
          break
        case 'PassPriority':
          result.passPriority = action
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
        case 'ActivateManaAbility':
          return a.sourceEntityId === cardId
        case 'Tap':
        case 'Untap':
          return a.entityId === cardId
        case 'DeclareAttacker':
          return a.creatureId === cardId
        case 'DeclareBlocker':
          return a.blockerId === cardId
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
 * Hook to check if the player can attack.
 */
export function useCanAttack(): boolean {
  const actions = useCategorizedActions()
  return actions.attackers.length > 0
}

/**
 * Hook to check if the player can block.
 */
export function useCanBlock(): boolean {
  const actions = useCategorizedActions()
  return actions.blockers.length > 0
}

/**
 * Hook to get cards that can attack.
 */
export function useAttackableCreatures(): EntityId[] {
  const actions = useCategorizedActions()
  return useMemo(
    () =>
      actions.attackers
        .map((a) => (a.action as { creatureId: EntityId }).creatureId)
        .filter((id): id is EntityId => id !== undefined),
    [actions.attackers]
  )
}

/**
 * Hook to get cards that can block.
 */
export function useBlockableCreatures(): EntityId[] {
  const actions = useCategorizedActions()
  return useMemo(
    () =>
      actions.blockers
        .map((a) => (a.action as { blockerId: EntityId }).blockerId)
        .filter((id): id is EntityId => id !== undefined),
    [actions.blockers]
  )
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
    case 'ActivateManaAbility':
      return action.sourceEntityId
    case 'Tap':
    case 'Untap':
      return action.entityId
    case 'DeclareAttacker':
      return action.creatureId
    case 'DeclareBlocker':
      return action.blockerId
    default:
      return null
  }
}
