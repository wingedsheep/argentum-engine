import { useMemo } from 'react'
import { useGameStore, type GameStore } from './gameStore'
import type {
  EntityId,
  ClientCard,
  ClientZone,
  ClientPlayer,
  LegalActionInfo,
  ZoneId,
} from '../types'
import { ZoneType, zoneIdEquals } from '../types'

/**
 * Select the game state.
 */
export const selectGameState = (state: GameStore) => state.gameState

/**
 * Select the viewing player ID.
 */
export const selectViewingPlayerId = (state: GameStore) => state.playerId

/**
 * Select all legal actions.
 */
export const selectLegalActions = (state: GameStore) => state.legalActions

/**
 * Select the selected card ID.
 */
export const selectSelectedCardId = (state: GameStore) => state.selectedCardId

/**
 * Select the hovered card ID.
 */
export const selectHoveredCardId = (state: GameStore) => state.hoveredCardId

/**
 * Select the targeting state.
 */
export const selectTargetingState = (state: GameStore) => state.targetingState

/**
 * Select the mulligan state.
 */
export const selectMulliganState = (state: GameStore) => state.mulliganState

/**
 * Select connection status.
 */
export const selectConnectionStatus = (state: GameStore) => state.connectionStatus

/**
 * Select if it's the player's turn.
 */
export const selectIsMyTurn = (state: GameStore): boolean => {
  const { gameState, playerId } = state
  if (!gameState || !playerId) return false
  return gameState.activePlayerId === playerId
}

/**
 * Select if the player has priority.
 */
export const selectHasPriority = (state: GameStore): boolean => {
  const { gameState, playerId } = state
  if (!gameState || !playerId) return false
  return gameState.priorityPlayerId === playerId
}

/**
 * Select the current phase.
 */
export const selectCurrentPhase = (state: GameStore) => state.gameState?.currentPhase ?? null

/**
 * Select the current step.
 */
export const selectCurrentStep = (state: GameStore) => state.gameState?.currentStep ?? null

/**
 * Select the turn number.
 */
export const selectTurnNumber = (state: GameStore) => state.gameState?.turnNumber ?? 0

/**
 * Hook to get a specific card by ID.
 */
export function useCard(cardId: EntityId | null): ClientCard | null {
  const gameState = useGameStore(selectGameState)
  return useMemo(() => {
    if (!gameState || !cardId) return null
    return gameState.cards[cardId] ?? null
  }, [gameState, cardId])
}

/**
 * Hook to get cards in a specific zone.
 */
export function useZoneCards(zoneId: ZoneId): readonly ClientCard[] {
  const gameState = useGameStore(selectGameState)
  return useMemo(() => {
    if (!gameState) return []
    const zone = gameState.zones.find((z) => zoneIdEquals(z.zoneId, zoneId))
    if (!zone || !zone.cardIds) return []
    return zone.cardIds
      .map((id) => gameState.cards[id])
      .filter((card): card is ClientCard => card !== null && card !== undefined)
  }, [gameState, zoneId])
}

/**
 * Hook to get zone info.
 */
export function useZone(zoneId: ZoneId): ClientZone | null {
  const gameState = useGameStore(selectGameState)
  return useMemo(() => {
    if (!gameState) return null
    return gameState.zones.find((z) => zoneIdEquals(z.zoneId, zoneId)) ?? null
  }, [gameState, zoneId])
}

/**
 * Hook to get a player by ID.
 */
export function usePlayer(playerId: EntityId | null): ClientPlayer | null {
  const gameState = useGameStore(selectGameState)
  return useMemo(() => {
    if (!gameState || !playerId) return null
    return gameState.players.find((p) => p.playerId === playerId) ?? null
  }, [gameState, playerId])
}

/**
 * Hook to get the viewing player.
 */
export function useViewingPlayer(): ClientPlayer | null {
  const playerId = useGameStore(selectViewingPlayerId)
  return usePlayer(playerId)
}

/**
 * Hook to get the opponent player.
 */
export function useOpponent(): ClientPlayer | null {
  const gameState = useGameStore(selectGameState)
  const playerId = useGameStore(selectViewingPlayerId)
  return useMemo(() => {
    if (!gameState || !playerId) return null
    return gameState.players.find((p) => p.playerId !== playerId) ?? null
  }, [gameState, playerId])
}

/**
 * Hook to get legal actions for a specific card.
 */
export function useCardLegalActions(cardId: EntityId | null): readonly LegalActionInfo[] {
  const legalActions = useGameStore(selectLegalActions)
  return useMemo(() => {
    if (!cardId) return []
    return legalActions.filter((action) => {
      // Check if this action involves the card
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
 * Hook to check if a card has any legal actions.
 */
export function useHasLegalActions(cardId: EntityId | null): boolean {
  const actions = useCardLegalActions(cardId)
  return actions.length > 0
}

/**
 * Hook to get battlefield cards grouped by controller and type.
 */
export function useBattlefieldCards(): {
  playerLands: readonly ClientCard[]
  playerCreatures: readonly ClientCard[]
  playerOther: readonly ClientCard[]
  opponentLands: readonly ClientCard[]
  opponentCreatures: readonly ClientCard[]
  opponentOther: readonly ClientCard[]
} {
  const gameState = useGameStore(selectGameState)
  const playerId = useGameStore(selectViewingPlayerId)

  return useMemo(() => {
    if (!gameState || !playerId) {
      return {
        playerLands: [],
        playerCreatures: [],
        playerOther: [],
        opponentLands: [],
        opponentCreatures: [],
        opponentOther: [],
      }
    }

    const battlefield = gameState.zones.find((z) => z.zoneId.type === ZoneType.BATTLEFIELD)
    if (!battlefield || !battlefield.cardIds) {
      return {
        playerLands: [],
        playerCreatures: [],
        playerOther: [],
        opponentLands: [],
        opponentCreatures: [],
        opponentOther: [],
      }
    }

    const cards = battlefield.cardIds
      .map((id) => gameState.cards[id])
      .filter((card): card is ClientCard => card !== null && card !== undefined)

    const playerCards = cards.filter((c) => c.controllerId === playerId)
    const opponentCards = cards.filter((c) => c.controllerId !== playerId)

    const isLand = (c: ClientCard) => c.cardTypes.includes('LAND')
    const isCreature = (c: ClientCard) => c.cardTypes.includes('CREATURE')

    return {
      playerLands: playerCards.filter(isLand),
      playerCreatures: playerCards.filter((c) => isCreature(c) && !isLand(c)),
      playerOther: playerCards.filter((c) => !isCreature(c) && !isLand(c)),
      opponentLands: opponentCards.filter(isLand),
      opponentCreatures: opponentCards.filter((c) => isCreature(c) && !isLand(c)),
      opponentOther: opponentCards.filter((c) => !isCreature(c) && !isLand(c)),
    }
  }, [gameState, playerId])
}

/**
 * Hook to get stack items in order.
 */
export function useStackCards(): readonly ClientCard[] {
  const gameState = useGameStore(selectGameState)
  return useMemo(() => {
    if (!gameState) return []
    const stack = gameState.zones.find((z) => z.zoneId.type === ZoneType.STACK)
    if (!stack || !stack.cardIds) return []
    return stack.cardIds
      .map((id) => gameState.cards[id])
      .filter((card): card is ClientCard => card !== null && card !== undefined)
  }, [gameState])
}

/**
 * Hook to check if a card is a valid target for current targeting.
 */
export function useIsValidTarget(cardId: EntityId): boolean {
  const targetingState = useGameStore(selectTargetingState)
  return useMemo(() => {
    if (!targetingState) return false
    return targetingState.validTargets.includes(cardId)
  }, [targetingState, cardId])
}

/**
 * Hook to check if a card is currently selected as a target.
 */
export function useIsSelectedTarget(cardId: EntityId): boolean {
  const targetingState = useGameStore(selectTargetingState)
  return useMemo(() => {
    if (!targetingState) return false
    return targetingState.selectedTargets.includes(cardId)
  }, [targetingState, cardId])
}

/**
 * Hook to get combat state.
 * Note: MaskedGameState doesn't currently include combat data from server.
 */
export function useCombatState() {
  // TODO: Server doesn't send combat state in MaskedGameState yet
  return null
}
