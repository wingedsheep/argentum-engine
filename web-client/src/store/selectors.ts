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
 * Select the game state (works for both normal play and spectating).
 * Returns spectatingState.gameState when spectating, otherwise gameState.
 */
export const selectGameState = (state: GameStore) =>
  state.spectatingState?.gameState ?? state.gameState

/**
 * Select the raw player game state (not spectator).
 */
export const selectPlayerGameState = (state: GameStore) => state.gameState

/**
 * Check if we're currently spectating.
 */
export const selectIsSpectating = (state: GameStore) => state.spectatingState !== null

/**
 * Select the viewing player ID.
 * In spectator mode, returns player1Id as the "viewing" player (bottom of screen).
 */
export const selectViewingPlayerId = (state: GameStore) =>
  (state.spectatingState?.player1Id as EntityId | null) ?? state.playerId

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
 * Priority mode for visual indication.
 * - ownTurn: Player has priority on their own turn (proactive)
 * - responding: Player has priority on opponent's turn (reactive)
 * - waiting: Player does not have priority
 */
export type PriorityMode = 'ownTurn' | 'responding' | 'waiting'

/**
 * Select the current priority mode for visual styling.
 */
export const selectPriorityMode = (state: GameStore): PriorityMode => {
  const isMyTurn = selectIsMyTurn(state)
  const hasPriority = selectHasPriority(state)
  if (!hasPriority) return 'waiting'
  if (isMyTurn) return 'ownTurn'
  return 'responding'
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
  const gameState = useGameStore(selectGameState)
  const playerId = useGameStore(selectViewingPlayerId)
  return useMemo(() => {
    if (!gameState || !playerId) return null
    return gameState.players.find((p) => p.playerId === playerId) ?? null
  }, [gameState, playerId])
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
        case 'ActivateAbility':
          return a.sourceId === cardId
        default:
          return false
      }
    })
  }, [legalActions, cardId])
}

/**
 * Hook to check if a card has any legal actions (excluding mana abilities).
 * Mana abilities are always available but shouldn't cause cards to be highlighted.
 */
export function useHasLegalActions(cardId: EntityId | null): boolean {
  const actions = useCardLegalActions(cardId)
  // Filter out mana abilities - they shouldn't cause highlighting
  const nonManaActions = actions.filter((a) => !a.isManaAbility)
  return nonManaActions.length > 0
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

    // Find ALL battlefield zones (there's one per player)
    const battlefields = gameState.zones.filter((z) => z.zoneId.zoneType === ZoneType.BATTLEFIELD)
    if (battlefields.length === 0) {
      return {
        playerLands: [],
        playerCreatures: [],
        playerOther: [],
        opponentLands: [],
        opponentCreatures: [],
        opponentOther: [],
      }
    }

    // Aggregate all card IDs from all battlefield zones
    const allCardIds = battlefields.flatMap((z) => z.cardIds ?? [])
    const cards = allCardIds
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
    const stack = gameState.zones.find((z) => z.zoneId.zoneType === ZoneType.STACK)
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

/**
 * Represents a group of identical cards for display purposes.
 */
export interface GroupedCard {
  /** The representative card to display (first card in group) */
  card: ClientCard
  /** Number of cards in this group */
  count: number
  /** All card IDs in this group (for action handling) */
  cardIds: readonly EntityId[]
  /** All cards in this group (for stacked rendering) */
  cards: readonly ClientCard[]
}

/**
 * Computes a grouping key for a card based on properties that make cards "different".
 * Cards with different keys should NOT be grouped together.
 */
export function computeCardGroupKey(card: ClientCard): string {
  const parts: string[] = [card.name]

  // Cards with counters are different
  const counterEntries = Object.entries(card.counters).filter(([, count]) => count && count > 0)
  if (counterEntries.length > 0) {
    const sortedCounters = counterEntries.sort(([a], [b]) => a.localeCompare(b))
    parts.push(`counters:${JSON.stringify(sortedCounters)}`)
  }

  // Cards with modified P/T are different
  if (card.power !== card.basePower || card.toughness !== card.baseToughness) {
    parts.push(`pt:${card.power}/${card.toughness}`)
  }

  // Cards with attachments are different
  if (card.attachments.length > 0) {
    parts.push(`attached:${card.attachments.length}`)
  }

  // Cards with damage are different (for battlefield creatures)
  if (card.damage != null && card.damage > 0) {
    parts.push(`damage:${card.damage}`)
  }

  // Tapped cards are different from untapped
  if (card.isTapped) {
    parts.push('tapped')
  }

  // Transformed cards are different
  if (card.isTransformed) {
    parts.push('transformed')
  }

  // Face-down cards are different
  if (card.isFaceDown) {
    parts.push('facedown')
  }

  return parts.join('|')
}

/**
 * Maximum cards per visual group. Groups larger than this are split.
 */
const MAX_GROUP_SIZE = 4

/**
 * Groups an array of cards by identical properties.
 * Cards are grouped if they have the same name and no distinguishing modifiers.
 * Groups are limited to MAX_GROUP_SIZE (4) cards - larger groups are split.
 */
export function groupCards(cards: readonly ClientCard[]): readonly GroupedCard[] {
  if (cards.length === 0) return []

  const groups = new Map<string, { cards: ClientCard[]; cardIds: EntityId[] }>()

  for (const card of cards) {
    const key = computeCardGroupKey(card)
    const existing = groups.get(key)
    if (existing) {
      existing.cards.push(card)
      existing.cardIds.push(card.id)
    } else {
      groups.set(key, { cards: [card], cardIds: [card.id] })
    }
  }

  // Split groups larger than MAX_GROUP_SIZE into multiple groups
  const result: GroupedCard[] = []
  for (const { cards: groupCards, cardIds } of groups.values()) {
    for (let i = 0; i < groupCards.length; i += MAX_GROUP_SIZE) {
      const slicedCards = groupCards.slice(i, i + MAX_GROUP_SIZE)
      const slicedIds = cardIds.slice(i, i + MAX_GROUP_SIZE)
      const firstCard = slicedCards[0]
      if (!firstCard) continue // Should never happen, but satisfies TypeScript
      result.push({
        card: firstCard,
        count: slicedCards.length,
        cardIds: slicedIds,
        cards: slicedCards,
      })
    }
  }

  return result
}

/**
 * Hook to get cards in a zone, grouped by identical properties.
 * Cards are grouped if they have the same name and no distinguishing modifiers.
 */
export function useGroupedZoneCards(zoneId: ZoneId): readonly GroupedCard[] {
  const cards = useZoneCards(zoneId)
  return useMemo(() => groupCards(cards), [cards])
}
