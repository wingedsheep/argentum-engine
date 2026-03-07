import type { SpectatorStateUpdate } from '../components/admin/ReplayViewer'

/**
 * A delta representation of a SpectatorStateUpdate change.
 * Null fields mean "unchanged from previous snapshot".
 */
export interface SpectatorReplayDelta {
  gameStateDelta?: StateDelta | null
  player1?: unknown | null
  player2?: unknown | null
  currentPhase?: string | null
  activePlayerId?: string | null
  priorityPlayerId?: string | null
  combat?: unknown | null
  combatCleared?: boolean | null
  decisionStatus?: unknown | null
  decisionCleared?: boolean | null
}

/**
 * Delta for the ClientGameState.cards / zones / scalars.
 */
interface StateDelta {
  addedCards?: Record<string, unknown> | null
  removedCardIds?: string[] | null
  updatedCards?: Record<string, unknown> | null
  updatedZones?: unknown[] | null
  players: unknown[]
  currentPhase?: string | null
  currentStep?: string | null
  activePlayerId?: string | null
  priorityPlayerId?: string | null
  turnNumber?: number | null
  isGameOver?: boolean | null
  winnerId?: string | null
  combat?: unknown | null
  combatCleared?: boolean | null
  newLogEntries?: unknown[] | null
}

/**
 * Wire format for replay data from the server.
 */
export interface ReplayData {
  initialSnapshot: SpectatorStateUpdate
  deltas: SpectatorReplayDelta[]
}

/**
 * Wire format for public replay endpoint (includes metadata).
 */
export interface PublicReplayData {
  metadata: {
    gameId: string
    player1Name: string
    player2Name: string
    winnerName: string | null
    startedAt: string
    endedAt: string
    snapshotCount: number
  }
  initialSnapshot: SpectatorStateUpdate
  deltas: SpectatorReplayDelta[]
}

/**
 * Reconstruct full SpectatorStateUpdate snapshots from an initial snapshot and a list of deltas.
 */
export function reconstructSnapshots(
  initialSnapshot: SpectatorStateUpdate,
  deltas: SpectatorReplayDelta[],
): SpectatorStateUpdate[] {
  const snapshots: SpectatorStateUpdate[] = [initialSnapshot]
  let current = initialSnapshot

  for (const delta of deltas) {
    current = applySpectatorDelta(current, delta)
    snapshots.push(current)
  }

  return snapshots
}

function applySpectatorDelta(
  prev: SpectatorStateUpdate,
  delta: SpectatorReplayDelta,
): SpectatorStateUpdate {
  return {
    gameSessionId: prev.gameSessionId,
    gameState: delta.gameStateDelta != null
      ? applyGameStateDelta(prev.gameState as GameStateObj | null, delta.gameStateDelta)
      : prev.gameState,
    player1Id: prev.player1Id,
    player2Id: prev.player2Id,
    player1Name: prev.player1Name,
    player2Name: prev.player2Name,
    player1: delta.player1 !== undefined && delta.player1 !== null ? delta.player1 : prev.player1,
    player2: delta.player2 !== undefined && delta.player2 !== null ? delta.player2 : prev.player2,
    currentPhase: delta.currentPhase ?? prev.currentPhase,
    activePlayerId: delta.activePlayerId !== undefined && delta.activePlayerId !== null
      ? (delta.activePlayerId === '' ? null : delta.activePlayerId)
      : prev.activePlayerId,
    priorityPlayerId: delta.priorityPlayerId !== undefined && delta.priorityPlayerId !== null
      ? (delta.priorityPlayerId === '' ? null : delta.priorityPlayerId)
      : prev.priorityPlayerId,
    combat: delta.combatCleared ? null : (delta.combat ?? prev.combat),
    decisionStatus: delta.decisionCleared ? null : (delta.decisionStatus ?? prev.decisionStatus),
  }
}

interface GameStateObj {
  cards: Record<string, unknown>
  zones: unknown[]
  players: unknown[]
  currentPhase: string
  currentStep: string
  activePlayerId: string
  priorityPlayerId: string
  turnNumber: number
  isGameOver: boolean
  winnerId: string | null
  combat: unknown | null
  gameLog: unknown[]
  [key: string]: unknown
}

function applyGameStateDelta(
  prev: GameStateObj | null,
  delta: StateDelta,
): GameStateObj {
  if (prev == null) {
    // Can't apply delta without a base — shouldn't happen in practice
    return prev as unknown as GameStateObj
  }

  // Apply card changes
  let cards = prev.cards
  if (delta.addedCards ?? delta.removedCardIds ?? delta.updatedCards) {
    cards = { ...cards }
    if (delta.addedCards) {
      for (const [id, card] of Object.entries(delta.addedCards)) {
        cards[id] = card
      }
    }
    if (delta.updatedCards) {
      for (const [id, card] of Object.entries(delta.updatedCards)) {
        cards[id] = card
      }
    }
    if (delta.removedCardIds) {
      for (const id of delta.removedCardIds) {
        // eslint-disable-next-line @typescript-eslint/no-dynamic-delete
        delete cards[id]
      }
    }
  }

  // Apply zone changes
  let zones = prev.zones
  if (delta.updatedZones && delta.updatedZones.length > 0) {
    const zoneMap = new Map<string, unknown>()
    for (const zone of prev.zones) {
      const z = zone as { zoneId: string }
      zoneMap.set(z.zoneId, zone)
    }
    for (const zone of delta.updatedZones) {
      const z = zone as { zoneId: string }
      zoneMap.set(z.zoneId, zone)
    }
    zones = Array.from(zoneMap.values())
  }

  // Apply game log (append-only)
  let gameLog = prev.gameLog
  if (delta.newLogEntries && delta.newLogEntries.length > 0) {
    gameLog = [...gameLog, ...delta.newLogEntries]
  }

  // Apply combat
  let combat = prev.combat
  if (delta.combatCleared) {
    combat = null
  } else if (delta.combat !== undefined && delta.combat !== null) {
    combat = delta.combat
  }

  return {
    ...prev,
    cards,
    zones,
    players: delta.players,
    currentPhase: delta.currentPhase ?? prev.currentPhase,
    currentStep: delta.currentStep ?? prev.currentStep,
    activePlayerId: delta.activePlayerId ?? prev.activePlayerId,
    priorityPlayerId: delta.priorityPlayerId ?? prev.priorityPlayerId,
    turnNumber: delta.turnNumber ?? prev.turnNumber,
    isGameOver: delta.isGameOver ?? prev.isGameOver,
    winnerId: delta.winnerId !== undefined ? delta.winnerId : prev.winnerId,
    combat,
    gameLog,
  }
}
