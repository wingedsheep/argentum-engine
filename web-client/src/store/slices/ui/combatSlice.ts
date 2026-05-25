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
  /**
   * Whether the currently-dragged attacker has the BANDING keyword. Set alongside
   * [draggingAttackerId] so other GameCards can decide whether they're a legal
   * band-drop target without re-querying the store for keyword data. Null when no
   * attacker is being dragged.
   */
  draggingAttackerHasBanding: boolean | null
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
  startDraggingAttacker: (attackerId: EntityId, hasBanding?: boolean) => void
  stopDraggingAttacker: () => void
  startDraggingCard: (cardId: EntityId) => void
  stopDraggingCard: () => void
  confirmCombat: () => void
  cancelCombat: () => void
  attackWithAll: () => void
  clearAttackers: () => void
  clearCombat: () => void
  /** Remove the band at [bandIndex] (does not affect attacker selection). */
  removeBand: (bandIndex: number) => void
  /** Clear all bands. */
  clearBands: () => void
  /**
   * Drag-and-drop band assembly (CR 702.22). Called when an attacker is dragged onto
   * another player-controlled attacker. Auto-selects both as attackers, then:
   * - If both are already in the same band → no-op.
   * - If one is in a band and the other isn't → adds the other to that band.
   * - If both are in different bands → merges into the band of [target].
   * - If neither is in a band → creates a fresh band of [source, target].
   *
   * `sourceHasBanding` and `targetHasBanding` are caller-supplied keyword flags. If
   * neither has banding the link is rejected; the "at most one non-banding member"
   * rule (CR 702.22c) is enforced at drop time by the caller and re-checked server-side.
   */
  linkBand: (sourceId: EntityId, targetId: EntityId, sourceHasBanding: boolean, targetHasBanding: boolean) => void
}

export type CombatSlice = CombatSliceState & CombatSliceActions

export const createCombatSlice: SliceCreator<CombatSlice> = (set, get) => ({
  combatState: null,
  draggingBlockerId: null,
  draggingAttackerId: null,
  draggingAttackerHasBanding: null,
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

      // Prune bands when an attacker is deselected: drop the creature from every band,
      // then discard any band that fell below 2 members.
      const newBands = isSelected
        ? state.combatState.bands
            .map((band) => band.filter((id) => id !== creatureId))
            .filter((band) => band.length >= 2)
        : state.combatState.bands

      getWebSocket()?.send(createUpdateAttackerTargetsMessage(newAttackers, newTargets))

      return {
        combatState: {
          ...state.combatState,
          selectedAttackers: newAttackers,
          attackerTargets: newTargets,
          bands: newBands,
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

  startDraggingAttacker: (attackerId, hasBanding = false) => {
    set({ draggingAttackerId: attackerId, draggingAttackerHasBanding: hasBanding })
  },

  stopDraggingAttacker: () => {
    set({ draggingAttackerId: null, draggingAttackerHasBanding: null })
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

      // Drop bands that no longer satisfy CR 702.22 (after attacker deselection /
      // target changes). Bands must have ≥2 members, all selected, all attacking the
      // same defender. Legality is re-checked server-side; this just avoids sending
      // obviously stale data.
      const selectedSet = new Set(combatState.selectedAttackers)
      const validBands = combatState.bands
        .map((band) => band.filter((id) => selectedSet.has(id)))
        .filter((band) => {
          if (band.length < 2) return false
          const target = attackers[band[0]!]
          return band.every((id) => attackers[id] === target)
        })

      const action = {
        type: 'DeclareAttackers' as const,
        playerId,
        attackers,
        ...(validBands.length > 0 ? { bands: validBands } : {}),
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
          bands: [],
        },
      }
    })
  },

  clearCombat: () => {
    set({ combatState: null, draggingBlockerId: null })
  },

  removeBand: (bandIndex) => {
    set((state) => {
      if (!state.combatState || state.combatState.mode !== 'declareAttackers') {
        return state
      }
      if (bandIndex < 0 || bandIndex >= state.combatState.bands.length) return state
      const newBands = state.combatState.bands.filter((_, i) => i !== bandIndex)
      return {
        combatState: {
          ...state.combatState,
          bands: newBands,
        },
      }
    })
  },

  clearBands: () => {
    set((state) => {
      if (!state.combatState) return state
      return {
        combatState: {
          ...state.combatState,
          bands: [],
        },
      }
    })
  },

  linkBand: (sourceId, targetId, sourceHasBanding, targetHasBanding) => {
    if (sourceId === targetId) return
    set((state) => {
      if (!state.combatState || state.combatState.mode !== 'declareAttackers') return state
      if (!sourceHasBanding && !targetHasBanding) return state

      // Auto-select both attackers if not already selected. Mandatory creatures are
      // already in the list; this just extends it.
      const selected = new Set(state.combatState.selectedAttackers)
      let newSelected = state.combatState.selectedAttackers
      if (!selected.has(sourceId)) {
        newSelected = [...newSelected, sourceId]
      }
      if (!selected.has(targetId)) {
        newSelected = [...newSelected, targetId]
      }

      const existingBands = state.combatState.bands.map((b) => [...b])
      const sourceBandIdx = existingBands.findIndex((b) => b.includes(sourceId))
      const targetBandIdx = existingBands.findIndex((b) => b.includes(targetId))

      let nextBands: EntityId[][]
      if (sourceBandIdx !== -1 && sourceBandIdx === targetBandIdx) {
        // Already banded together — nothing to do beyond the (possible) re-selection.
        return {
          combatState: {
            ...state.combatState,
            selectedAttackers: newSelected,
          },
        }
      } else if (sourceBandIdx !== -1 && targetBandIdx !== -1) {
        // Merge the two bands into the target's band, drop the source's band.
        const merged = Array.from(new Set([...existingBands[targetBandIdx]!, ...existingBands[sourceBandIdx]!]))
        nextBands = existingBands
          .map((b, i) => (i === targetBandIdx ? merged : i === sourceBandIdx ? [] : b))
          .filter((b) => b.length >= 2)
      } else if (sourceBandIdx !== -1) {
        nextBands = existingBands.map((b, i) =>
          i === sourceBandIdx ? Array.from(new Set([...b, targetId])) : b
        )
      } else if (targetBandIdx !== -1) {
        nextBands = existingBands.map((b, i) =>
          i === targetBandIdx ? Array.from(new Set([...b, sourceId])) : b
        )
      } else {
        nextBands = [...existingBands, [sourceId, targetId]]
      }

      return {
        combatState: {
          ...state.combatState,
          selectedAttackers: newSelected,
          bands: nextBands,
        },
      }
    })
  },
})
