/**
 * Combat sub-slice — handles attacker/blocker declarations, dragging, and combat confirmation.
 */
import type {
  SliceCreator,
  EntityId,
  CombatState,
} from '../types'
import {
  entityId,
  createSubmitActionMessage,
  createUpdateAttackerTargetsMessage,
  createUpdateBlockerAssignmentsMessage,
} from '@/types'
import { getWebSocket } from '../shared'

export interface CombatSliceState {
  combatState: CombatState | null
  draggingBlockerId: EntityId | null
  draggingAttackerId: EntityId | null
  draggingCardId: EntityId | null
  opponentAttackerTargets: { selectedAttackers: readonly EntityId[]; attackerTargets: Record<EntityId, EntityId> } | null
  opponentBlockerAssignments: Record<EntityId, EntityId[]> | null
}

export interface CombatSliceActions {
  startCombat: (state: CombatState) => void
  toggleAttacker: (creatureId: EntityId) => void
  setAttackTarget: (attackerId: EntityId, targetId: EntityId) => void
  assignBlocker: (blockerId: EntityId, attackerId: EntityId) => void
  removeBlockerAssignment: (blockerId: EntityId) => void
  clearBlockerAssignments: () => void
  startDraggingBlocker: (blockerId: EntityId) => void
  stopDraggingBlocker: () => void
  startDraggingAttacker: (attackerId: EntityId) => void
  stopDraggingAttacker: () => void
  startDraggingCard: (cardId: EntityId) => void
  stopDraggingCard: () => void
  confirmCombat: () => void
  cancelCombat: () => void
  attackWithAll: () => void
  clearAttackers: () => void
  clearCombat: () => void
}

export type CombatSlice = CombatSliceState & CombatSliceActions

export const createCombatSlice: SliceCreator<CombatSlice> = (set, get) => ({
  combatState: null,
  draggingBlockerId: null,
  draggingAttackerId: null,
  draggingCardId: null,
  opponentAttackerTargets: null,
  opponentBlockerAssignments: null,

  startCombat: (combatState) => {
    set({ combatState })
    // Sync pre-populated blocker assignments with opponent
    if (combatState.mode === 'declareBlockers' && Object.keys(combatState.blockerAssignments).length > 0) {
      getWebSocket()?.send(createUpdateBlockerAssignmentsMessage(combatState.blockerAssignments))
    }
  },

  toggleAttacker: (creatureId) => {
    set((state) => {
      if (!state.combatState || state.combatState.mode !== 'declareAttackers') {
        return state
      }

      const isSelected = state.combatState.selectedAttackers.includes(creatureId)

      // Don't allow deselecting mandatory attackers
      if (isSelected && state.combatState.mandatoryAttackers.includes(creatureId)) {
        return state
      }

      const newAttackers = isSelected
        ? state.combatState.selectedAttackers.filter((id) => id !== creatureId)
        : [...state.combatState.selectedAttackers, creatureId]

      // Clean up attackerTargets if deselecting
      const newTargets = { ...state.combatState.attackerTargets }
      if (isSelected) {
        delete newTargets[creatureId]
      }

      getWebSocket()?.send(createUpdateAttackerTargetsMessage(newAttackers, newTargets))

      return {
        combatState: {
          ...state.combatState,
          selectedAttackers: newAttackers,
          attackerTargets: newTargets,
        },
      }
    })
  },

  setAttackTarget: (attackerId, targetId) => {
    set((state) => {
      if (!state.combatState || state.combatState.mode !== 'declareAttackers') {
        return state
      }

      const newTargets = {
        ...state.combatState.attackerTargets,
        [attackerId]: targetId,
      }

      getWebSocket()?.send(createUpdateAttackerTargetsMessage(
        [...state.combatState.selectedAttackers],
        newTargets,
      ))

      return {
        combatState: {
          ...state.combatState,
          attackerTargets: newTargets,
        },
      }
    })
  },

  assignBlocker: (blockerId, attackerId) => {
    set((state) => {
      if (!state.combatState || state.combatState.mode !== 'declareBlockers') {
        return state
      }

      const existing = state.combatState.blockerAssignments[blockerId] ?? []
      // If already blocking this attacker, don't add duplicate
      if (existing.includes(attackerId)) return state
      // Check max block count (default 1 for normal creatures)
      const maxBlocks = state.combatState.blockerMaxBlockCounts[blockerId] ?? 1
      if (existing.length >= maxBlocks) {
        return state
      }
      const newAssignments = {
        ...state.combatState.blockerAssignments,
        [blockerId]: [...existing, attackerId],
      }

      getWebSocket()?.send(createUpdateBlockerAssignmentsMessage(newAssignments))

      return {
        combatState: {
          ...state.combatState,
          blockerAssignments: newAssignments,
        },
      }
    })
  },

  removeBlockerAssignment: (blockerId) => {
    set((state) => {
      if (!state.combatState || state.combatState.mode !== 'declareBlockers') {
        return state
      }

      const { [blockerId]: _, ...remaining } = state.combatState.blockerAssignments
      getWebSocket()?.send(createUpdateBlockerAssignmentsMessage(remaining))

      return {
        combatState: {
          ...state.combatState,
          blockerAssignments: remaining,
        },
      }
    })
  },

  clearBlockerAssignments: () => {
    set((state) => {
      if (!state.combatState || state.combatState.mode !== 'declareBlockers') {
        return state
      }

      getWebSocket()?.send(createUpdateBlockerAssignmentsMessage({}))

      return {
        combatState: {
          ...state.combatState,
          blockerAssignments: {},
        },
      }
    })
  },

  startDraggingBlocker: (blockerId) => {
    set({ draggingBlockerId: blockerId })
  },

  stopDraggingBlocker: () => {
    set({ draggingBlockerId: null })
  },

  startDraggingAttacker: (attackerId) => {
    set({ draggingAttackerId: attackerId })
  },

  stopDraggingAttacker: () => {
    set({ draggingAttackerId: null })
  },

  startDraggingCard: (cardId) => {
    set({ draggingCardId: cardId })
  },

  stopDraggingCard: () => {
    set({ draggingCardId: null })
  },

  confirmCombat: () => {
    const { combatState, playerId } = get()
    if (!combatState || !playerId) return

    if (combatState.mode === 'declareAttackers') {
      const { gameState } = get()
      if (!gameState) return

      const opponent = gameState.players.find((p) => p.playerId !== playerId)
      if (!opponent) return

      const attackers: Record<EntityId, EntityId> = {}
      for (const attackerId of combatState.selectedAttackers) {
        // Use per-attacker target if set, otherwise default to opponent player
        attackers[attackerId] = combatState.attackerTargets[attackerId] ?? opponent.playerId
      }

      const action = {
        type: 'DeclareAttackers' as const,
        playerId,
        attackers,
      }
      getWebSocket()?.send(createSubmitActionMessage(action))
    } else if (combatState.mode === 'declareBlockers') {
      const blockers: Record<EntityId, readonly EntityId[]> = {}
      for (const [blockerIdStr, attackerIds] of Object.entries(combatState.blockerAssignments)) {
        blockers[entityId(blockerIdStr)] = attackerIds
      }

      const action = {
        type: 'DeclareBlockers' as const,
        playerId,
        blockers,
      }
      getWebSocket()?.send(createSubmitActionMessage(action))
    }

    set({ draggingBlockerId: null })
  },

  attackWithAll: () => {
    const { combatState } = get()
    if (!combatState) return
    if (combatState.mode !== 'declareAttackers') return
    if (combatState.validCreatures.length === 0) return

    set({
      combatState: {
        ...combatState,
        selectedAttackers: [...combatState.validCreatures],
      },
    })
  },

  cancelCombat: () => {
    const { combatState, playerId } = get()
    if (!combatState || !playerId) return

    if (combatState.mode === 'declareAttackers') {
      const action = {
        type: 'DeclareAttackers' as const,
        playerId,
        attackers: {} as Record<EntityId, EntityId>,
      }
      getWebSocket()?.send(createSubmitActionMessage(action))
    } else if (combatState.mode === 'declareBlockers') {
      const action = {
        type: 'DeclareBlockers' as const,
        playerId,
        blockers: {} as Record<EntityId, readonly EntityId[]>,
      }
      getWebSocket()?.send(createSubmitActionMessage(action))
    }

    set({ draggingBlockerId: null })
  },

  clearAttackers: () => {
    set((state) => {
      if (!state.combatState || state.combatState.mode !== 'declareAttackers') {
        return state
      }
      return {
        combatState: {
          ...state.combatState,
          selectedAttackers: [...state.combatState.mandatoryAttackers],
          attackerTargets: {},
        },
      }
    })
  },

  clearCombat: () => {
    set({ combatState: null, draggingBlockerId: null })
  },
})
