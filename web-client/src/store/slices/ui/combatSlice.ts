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
  createUpdateAttackerTargetsMessage,
  createUpdateBlockerAssignmentsMessage,
} from '@/types'
import type { ClientGameState } from '@/types'
import { bandIsLegal, mergedBand } from '@/utils/combatBands'
import { attackTargetsFor, canAttackTarget } from '@/utils/attackTargets'
import { getWebSocket } from '../shared'

/**
 * The seat a combat declaration should be tagged with. Normally the connection's own seat;
 * in single-client hotseat the one connection declares for whichever seat the server is
 * asking — the acting seat recorded from the legal action, falling back to the active
 * player for attackers / the (2-player) defending player for blockers.
 */
function combatActingSeat(
  combatState: { mode: string; actingSeat: EntityId | null },
  connectionSeat: EntityId,
  gameState: ClientGameState | null,
): EntityId {
  // A Mindslaver-style hijack also drives another seat's combat: this connection declares
  // for the controlled opponent, not for itself. Same routing as hotseat — use the
  // server-recorded acting seat / active player rather than our own connection seat.
  if (!gameState?.hotseat && !gameState?.youAreHijacking) return connectionSeat
  if (combatState.actingSeat) return combatState.actingSeat
  if (combatState.mode === 'declareAttackers') return gameState.activePlayerId
  return (
    gameState.combat?.defendingPlayerId ??
    gameState.players.find((p) => p.playerId !== gameState.activePlayerId)?.playerId ??
    connectionSeat
  )
}

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
  /**
   * Multiplayer defender pick: assign every currently selected attacker to
   * [defenderId] (a player or planeswalker) and make it the sticky default for
   * attackers selected afterwards. Driven by rail-chip / defender-pick clicks.
   */
  assignDefenderToSelectedAttackers: (defenderId: EntityId) => void
  assignBlocker: (blockerId: EntityId, attackerId: EntityId) => void
  removeBlockerAssignment: (blockerId: EntityId) => void
  clearBlockerAssignments: () => void
  startDraggingBlocker: (blockerId: EntityId) => void
  stopDraggingBlocker: () => void
  startDraggingAttacker: (attackerId: EntityId) => void
  stopDraggingAttacker: () => void
  startDraggingCard: (cardId: EntityId) => void
  stopDraggingCard: () => void
  confirmCombat: (interactionEpoch: string | null) => void
  cancelCombat: (interactionEpoch: string | null) => void
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
   * CR 702.22c (at most one member without banding) is checked here against the store's
   * projected keywords — the set the server's `validateBands` reads — so the merge path and the
   * drop path can't disagree, and no band the server would accept is refused. The server
   * re-validates on declaration regardless.
   */
  linkBand: (sourceId: EntityId, targetId: EntityId) => void
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
    if (!combatState.interactionEpoch || combatState.interactionEpoch !== get().interactionEpoch) return
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

      // Clean up attackerTargets if deselecting; sticky-assign on select when a
      // defender has already been picked this declaration (multiplayer).
      const newTargets = { ...state.combatState.attackerTargets }
      if (isSelected) {
        delete newTargets[creatureId]
      } else if (
        state.combatState.stickyDefenderId &&
        !newTargets[creatureId] &&
        // A creature that can't attack the sticky defender (Goblin Rock Sled vs a seat with no
        // Mountain) stays unassigned, so the defender pick asks about it instead.
        canAttackTarget(state.combatState, creatureId, state.combatState.stickyDefenderId)
      ) {
        newTargets[creatureId] = state.combatState.stickyDefenderId
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

      // The server lists which defenders each attacker may pick; ignore any other.
      if (!canAttackTarget(state.combatState, attackerId, targetId)) return state
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

  assignDefenderToSelectedAttackers: (defenderId) => {
    set((state) => {
      if (!state.combatState || state.combatState.mode !== 'declareAttackers') {
        return state
      }
      // Only attackers the server lets attack this defender move to it. One that can't (Goblin
      // Rock Sled vs a seat with no Mountain) keeps its current defender, or stays unassigned so
      // the defender pick asks about it again.
      const newTargets = { ...state.combatState.attackerTargets }
      for (const attackerId of state.combatState.selectedAttackers) {
        if (!canAttackTarget(state.combatState, attackerId, defenderId)) continue
        newTargets[attackerId] = defenderId
      }
      getWebSocket()?.send(createUpdateAttackerTargetsMessage(
        [...state.combatState.selectedAttackers],
        newTargets,
      ))
      return {
        combatState: {
          ...state.combatState,
          attackerTargets: newTargets,
          stickyDefenderId: defenderId,
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

  confirmCombat: (interactionEpoch) => {
    if (!interactionEpoch || interactionEpoch !== get().interactionEpoch) return
    const { combatState, playerId, gameState } = get()
    // The submission below carries combatState's captured origin, so that is the value that has
    // to be current — checking only the caller's would let a stale declaration through.
    if (!combatState || combatState.interactionEpoch !== interactionEpoch || !playerId) return
    // In hotseat the single connection declares for whichever seat the server is asking:
    // the acting seat from the legal action (active player / defending player fallback).
    const actingSeat = combatActingSeat(combatState, playerId, gameState)

    if (combatState.mode === 'declareAttackers') {
      if (!gameState) return

      // Default defender for attackers without an explicit assignment: the sticky
      // defender (multiplayer pick), else the first living opponent the server lists as
      // attackable — under attack-left/right or a "can't be attacked" effect the first
      // living opponent in seat order may not be — else the first opponent at all.
      const attackable = new Set(combatState.validAttackTargets)
      const defaultDefender =
        combatState.stickyDefenderId ??
        gameState.players.find((p) => p.playerId !== actingSeat && !p.hasLost && attackable.has(p.playerId))?.playerId ??
        gameState.players.find((p) => p.playerId !== actingSeat && !p.hasLost)?.playerId ??
        gameState.players.find((p) => p.playerId !== actingSeat)?.playerId
      if (!defaultDefender) return

      const attackers: Record<EntityId, EntityId> = {}
      for (const attackerId of combatState.selectedAttackers) {
        // Use per-attacker target if set, otherwise default to the defender above — or, when the
        // server says this attacker can't attack that one, the first target it lists for it.
        attackers[attackerId] = combatState.attackerTargets[attackerId] ??
          (canAttackTarget(combatState, attackerId, defaultDefender)
            ? defaultDefender
            : attackTargetsFor(combatState, attackerId)[0] ?? defaultDefender)
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
        playerId: actingSeat,
        attackers,
        ...(validBands.length > 0 ? { bands: validBands } : {}),
      }
      get().submitAction(action, combatState.interactionEpoch)
    } else if (combatState.mode === 'declareBlockers') {
      const blockers: Record<EntityId, readonly EntityId[]> = {}
      for (const [blockerIdStr, attackerIds] of Object.entries(combatState.blockerAssignments)) {
        blockers[entityId(blockerIdStr)] = attackerIds
      }

      const action = {
        type: 'DeclareBlockers' as const,
        playerId: actingSeat,
        blockers,
      }
      get().submitAction(action, combatState.interactionEpoch)
    }

    set({ draggingBlockerId: null })
  },

  attackWithAll: () => {
    const { combatState } = get()
    if (!combatState) return
    if (combatState.mode !== 'declareAttackers') return
    if (combatState.validCreatures.length === 0) return

    // Sticky defender applies to the whole selection (multiplayer) — same rule as
    // toggling attackers one by one, so "Attack All" never reopens the defender pick
    // when a defender is already chosen (or was pre-assigned as the sole legal one).
    const newAttackers = [...combatState.validCreatures]
    const newTargets = { ...combatState.attackerTargets }
    if (combatState.stickyDefenderId) {
      for (const id of newAttackers) {
        if (!newTargets[id] && canAttackTarget(combatState, id, combatState.stickyDefenderId)) {
          newTargets[id] = combatState.stickyDefenderId
        }
      }
    }
    getWebSocket()?.send(createUpdateAttackerTargetsMessage(newAttackers, newTargets))

    set({
      combatState: {
        ...combatState,
        selectedAttackers: newAttackers,
        attackerTargets: newTargets,
      },
    })
  },

  cancelCombat: (interactionEpoch) => {
    if (!interactionEpoch || interactionEpoch !== get().interactionEpoch) return
    const { combatState, playerId, gameState } = get()
    if (!combatState || combatState.interactionEpoch !== interactionEpoch || !playerId) return
    const actingSeat = combatActingSeat(combatState, playerId, gameState)

    if (combatState.mode === 'declareAttackers') {
      const action = {
        type: 'DeclareAttackers' as const,
        playerId: actingSeat,
        attackers: {} as Record<EntityId, EntityId>,
      }
      get().submitAction(action, combatState.interactionEpoch)
    } else if (combatState.mode === 'declareBlockers') {
      const action = {
        type: 'DeclareBlockers' as const,
        playerId: actingSeat,
        blockers: {} as Record<EntityId, readonly EntityId[]>,
      }
      get().submitAction(action, combatState.interactionEpoch)
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

  linkBand: (sourceId, targetId) => {
    if (sourceId === targetId) return
    set((state) => {
      if (!state.combatState || state.combatState.mode !== 'declareAttackers') return state
      const cardOf = (id: EntityId) => state.gameState?.cards[id]
      if (!bandIsLegal(mergedBand(state.combatState.bands, sourceId, targetId), cardOf)) return state

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
