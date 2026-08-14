import type { ClientGameState, ClientCard } from '@/types'
import type { StateDelta } from '@/types'
import type { EntityId } from '@/types'

/**
 * Apply a StateDelta to an existing ClientGameState, producing the new state.
 *
 * This is the client-side counterpart to StateDiffCalculator on the server.
 * Fields present in the delta override the corresponding field in the current state.
 * Null/undefined delta fields mean "unchanged".
 */
export function applyStateDelta(
  current: ClientGameState,
  delta: StateDelta
): ClientGameState {
  // --- Cards ---
  let cards = current.cards
  const hasCardChanges = delta.addedCards || delta.removedCardIds || delta.updatedCards
  if (hasCardChanges) {
    // Start with a shallow copy
    const newCards: Record<EntityId, ClientCard> = { ...cards }

    // Remove cards
    if (delta.removedCardIds) {
      for (const id of delta.removedCardIds) {
        // eslint-disable-next-line @typescript-eslint/no-dynamic-delete
        delete newCards[id as EntityId]
      }
    }

    // Add new cards
    if (delta.addedCards) {
      for (const id of Object.keys(delta.addedCards)) {
        newCards[id as EntityId] = delta.addedCards[id as EntityId]!
      }
    }

    // Update changed cards
    if (delta.updatedCards) {
      for (const id of Object.keys(delta.updatedCards)) {
        newCards[id as EntityId] = delta.updatedCards[id as EntityId]!
      }
    }

    cards = newCards
  }

  // --- Zones ---
  let zones = current.zones
  if (delta.updatedZones) {
    const updatedZoneMap = new Map(
      delta.updatedZones.map((z) => [JSON.stringify(z.zoneId), z])
    )
    const mapped = current.zones.map((z) => {
      const key = JSON.stringify(z.zoneId)
      return updatedZoneMap.get(key) ?? z
    })
    // Add any entirely new zones not in current
    for (const updatedZone of delta.updatedZones) {
      const key = JSON.stringify(updatedZone.zoneId)
      if (!current.zones.some((z) => JSON.stringify(z.zoneId) === key)) {
        mapped.push(updatedZone)
      }
    }
    zones = mapped
  }

  // --- Game log (append-only) ---
  const gameLog = delta.newLogEntries
    ? [...(current.gameLog ?? []), ...delta.newLogEntries]
    : (current.gameLog ?? [])

  // --- Combat ---
  let combat = current.combat
  if (delta.combatCleared) {
    combat = null
  } else if (delta.combat !== undefined && delta.combat !== null) {
    combat = delta.combat
  }

  return {
    viewingPlayerId: current.viewingPlayerId,
    cards,
    zones,
    players: delta.players,
    currentPhase: delta.currentPhase ?? current.currentPhase,
    currentStep: delta.currentStep ?? current.currentStep,
    activePlayerId: delta.activePlayerId ?? current.activePlayerId,
    priorityPlayerId: delta.priorityPlayerId ?? current.priorityPlayerId,
    turnNumber: delta.turnNumber ?? current.turnNumber,
    isGameOver: delta.isGameOver ?? current.isGameOver,
    winnerId: delta.winnerId !== undefined ? delta.winnerId : current.winnerId,
    // Carried forward: a null delta value means unchanged (the game never reverts to neither, CR 731.1).
    dayNight: delta.dayNight ?? current.dayNight ?? null,
    combat,
    gameLog,
    youAreHijacking: delta.youAreHijacking ?? null,
    youAreHijackedBy: delta.youAreHijackedBy ?? null,
    hotseat: delta.hotseat ?? false,
    // Carried forward: the server omits these from a delta when unchanged, and rebuilding the
    // state object without them would silently blank the yields panel / deck tracker.
    activeYields: current.activeYields ?? [],
    deck: delta.deck ?? current.deck ?? [],
  }
}
