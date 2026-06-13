import { useMemo, useRef } from 'react'
import { useGameStore, type GameStore } from './gameStore'
import type {
  EntityId,
  ClientCard,
  ClientZone,
  ClientPlayer,
  LegalActionInfo,
  ZoneId,
} from '@/types'
import { ZoneType, zoneIdEquals, graveyard, library } from '@/types'
import { seatColor, type SeatColor } from '@/styles/seatColors'

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
 * In spectator mode, returns the seat anchoring the bottom of the screen — the
 * seat-switcher override when set, else the stream's player1Id.
 */
export const selectViewingPlayerId = (state: GameStore) =>
  (state.spectatingState
    ? ((state.spectatorBottomSeatId ?? state.spectatingState.player1Id) as EntityId | null)
    : null) ?? state.playerId

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
 * Select the decision selection state.
 */
export const selectDecisionSelectionState = (state: GameStore) => state.decisionSelectionState

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
 *
 * 2-player shape: returns the sole opponent. In a multiplayer game this returns the
 * first opponent in turn order — multiplayer-aware components should use
 * [useOpponents] / [useViewedOpponent] instead.
 */
export function useOpponent(): ClientPlayer | null {
  const gameState = useGameStore(selectGameState)
  const playerId = useGameStore(selectViewingPlayerId)
  return useMemo(() => {
    if (!gameState || !playerId) return null
    return gameState.players.find((p) => p.playerId !== playerId) ?? null
  }, [gameState, playerId])
}

const EMPTY_PLAYERS: readonly ClientPlayer[] = Object.freeze([])

/**
 * True when the game has more than two seats — the switch that turns on the
 * multiplayer chrome (opponent rail, board strip, seat colors). A 2-player game
 * must render exactly as before.
 */
export const selectIsMultiplayerGame = (state: GameStore): boolean => {
  const gameState = state.spectatingState?.gameState ?? state.gameState
  return (gameState?.players.length ?? 0) > 2
}

/**
 * All opponents of the viewing player, rotated to read in play order *after* the
 * viewing player (server orders `players` by turn order). Includes players who
 * have lost — they keep their seat for stable ordering and render as tombstones;
 * filter `hasLost` where only live boards matter.
 */
export function useOpponents(): readonly ClientPlayer[] {
  const gameState = useGameStore(selectGameState)
  const playerId = useGameStore(selectViewingPlayerId)
  return useMemo(() => {
    if (!gameState || !playerId) return EMPTY_PLAYERS
    const players = gameState.players
    const selfIndex = players.findIndex((p) => p.playerId === playerId)
    if (selfIndex === -1) return players.filter((p) => p.playerId !== playerId)
    return [...players.slice(selfIndex + 1), ...players.slice(0, selfIndex)]
  }, [gameState, playerId])
}

/**
 * The opponent whose board occupies the viewed slot. Resolves the boardView
 * selection with fallbacks: an eliminated or unknown selection falls back to the
 * first opponent still in the game (or the first opponent, if all have lost).
 */
export function useViewedOpponent(): ClientPlayer | null {
  const opponents = useOpponents()
  const viewedOpponentId = useGameStore((state) => state.viewedOpponentId)
  return useMemo(() => {
    if (opponents.length === 0) return null
    const selected = opponents.find((p) => p.playerId === viewedOpponentId)
    if (selected && !selected.hasLost) return selected
    return opponents.find((p) => !p.hasLost) ?? opponents[0] ?? null
  }, [opponents, viewedOpponentId])
}

/**
 * Seat index of a player = its index in the turn-ordered roster. Drives stable
 * seat colors and rail ordering. -1 when unknown.
 */
export function useSeatIndex(playerId: EntityId | null): number {
  const gameState = useGameStore(selectGameState)
  return useMemo(() => {
    if (!gameState || !playerId) return -1
    return gameState.players.findIndex((p) => p.playerId === playerId)
  }, [gameState, playerId])
}

/**
 * The stable seat-identity color for a player (rail chip, combat arrows, stack
 * borders, log names). Falls back to seat 0's hue for unknown players.
 */
export function useSeatColor(playerId: EntityId | null): SeatColor {
  const seatIndex = useSeatIndex(playerId)
  return seatColor(Math.max(0, seatIndex))
}

const EMPTY_ACTIONS: readonly LegalActionInfo[] = Object.freeze([])

/** Extract the card-id this legal action is anchored to, if any. */
function cardIdForAction(info: LegalActionInfo): EntityId | undefined {
  const a = info.action
  switch (a.type) {
    case 'PlayLand':
    case 'CastSpell':
    case 'CycleCard':
    case 'TypecycleCard':
      return a.cardId
    case 'ActivateAbility':
    case 'TurnFaceUp':
      return a.sourceId
    case 'CrewVehicle':
      return a.vehicleId
    case 'SaddleMount':
      return a.mountId
    case 'UnlockRoomDoor':
      return a.roomId
    default:
      return undefined
  }
}

function isHighlightable(a: LegalActionInfo): boolean {
  return (!a.isManaAbility || a.additionalCostInfo != null || a.manaCostString != null) && a.isAffordable !== false
}

/**
 * Module-level cache keyed on the legalActions array reference. Rebuilt once
 * per new legalActions identity and reused across every card on the board.
 * Without this, every card re-filters the full array on each state update —
 * O(cards × actions) per render.
 */
let cachedLegalActionsRef: readonly LegalActionInfo[] | null = null
let cachedByCard: Map<EntityId, LegalActionInfo[]> = new Map()
let cachedHighlightable: Set<EntityId> = new Set()

function getLegalActionsIndex(legalActions: readonly LegalActionInfo[]): {
  byCard: Map<EntityId, LegalActionInfo[]>
  highlightable: Set<EntityId>
} {
  if (cachedLegalActionsRef === legalActions) {
    return { byCard: cachedByCard, highlightable: cachedHighlightable }
  }
  const byCard = new Map<EntityId, LegalActionInfo[]>()
  const highlightable = new Set<EntityId>()
  for (const info of legalActions) {
    const id = cardIdForAction(info)
    if (id === undefined) continue
    const list = byCard.get(id)
    if (list) list.push(info)
    else byCard.set(id, [info])
    if (isHighlightable(info)) highlightable.add(id)
  }
  cachedLegalActionsRef = legalActions
  cachedByCard = byCard
  cachedHighlightable = highlightable
  return { byCard, highlightable }
}

/**
 * Hook to get legal actions for a specific card.
 * Uses a shared index so we don't re-filter the full legalActions array per card.
 */
export function useCardLegalActions(cardId: EntityId | null): readonly LegalActionInfo[] {
  const legalActions = useGameStore(selectLegalActions)
  return useMemo(() => {
    if (!cardId) return EMPTY_ACTIONS
    return getLegalActionsIndex(legalActions).byCard.get(cardId) ?? EMPTY_ACTIONS
  }, [legalActions, cardId])
}

/**
 * Hook to check if a card has any legal actions (excluding simple mana abilities and unaffordable actions).
 * Returns a plain boolean so Zustand's default equality prevents re-renders when the value is unchanged —
 * avoids re-rendering every battlefield card whenever legalActions is replaced.
 */
export function useHasLegalActions(cardId: EntityId | null): boolean {
  return useGameStore((state) => {
    if (!cardId) return false
    return getLegalActionsIndex(state.legalActions).highlightable.has(cardId)
  })
}

export interface BattlefieldAttachments {
  readonly attachments: readonly ClientCard[]
  readonly linkedExile: readonly ClientCard[]
}

export interface BattlefieldCards {
  playerLands: readonly ClientCard[]
  playerCreatures: readonly ClientCard[]
  playerPlaneswalkers: readonly ClientCard[]
  playerOther: readonly ClientCard[]
  opponentLands: readonly ClientCard[]
  opponentCreatures: readonly ClientCard[]
  opponentPlaneswalkers: readonly ClientCard[]
  opponentOther: readonly ClientCard[]
  /** Resolved attachment + linked-exile cards, keyed by parent permanent id. */
  attachmentsByCardId: ReadonlyMap<EntityId, BattlefieldAttachments>
}

const EMPTY_ATTACHMENTS: ReadonlyMap<EntityId, BattlefieldAttachments> = new Map()

/**
 * Structural equality for values the battlefield actually renders. Small, JSON-shaped objects —
 * no dates, functions, or Maps, so a plain recursive walk is sufficient and fast.
 */
function deepEqual(a: unknown, b: unknown): boolean {
  if (a === b) return true
  if (a == null || b == null) return false
  if (typeof a !== 'object' || typeof b !== 'object') return false
  if (Array.isArray(a)) {
    if (!Array.isArray(b) || a.length !== b.length) return false
    for (let i = 0; i < a.length; i++) {
      if (!deepEqual(a[i], b[i])) return false
    }
    return true
  }
  if (Array.isArray(b)) return false
  const ka = Object.keys(a as Record<string, unknown>)
  const kb = Object.keys(b as Record<string, unknown>)
  if (ka.length !== kb.length) return false
  for (const k of ka) {
    if (!deepEqual((a as Record<string, unknown>)[k], (b as Record<string, unknown>)[k])) return false
  }
  return true
}

function battlefieldCardsEqual(a: BattlefieldCards, b: BattlefieldCards): boolean {
  const keys = [
    'playerLands', 'playerCreatures', 'playerPlaneswalkers', 'playerOther',
    'opponentLands', 'opponentCreatures', 'opponentPlaneswalkers', 'opponentOther',
  ] as const
  for (const k of keys) {
    const arrA = a[k], arrB = b[k]
    if (arrA === arrB) continue
    if (arrA.length !== arrB.length) return false
    for (let i = 0; i < arrA.length; i++) {
      if (!deepEqual(arrA[i], arrB[i])) return false
    }
  }
  // Attachments map — same set of parent ids, each with equal attachment/linkedExile arrays.
  if (a.attachmentsByCardId.size !== b.attachmentsByCardId.size) return false
  for (const [id, valA] of a.attachmentsByCardId) {
    const valB = b.attachmentsByCardId.get(id)
    if (!valB) return false
    if (!deepEqual(valA.attachments, valB.attachments)) return false
    if (!deepEqual(valA.linkedExile, valB.linkedExile)) return false
  }
  return true
}

/**
 * Hook to get battlefield cards grouped by controller and type, plus resolved attachments.
 *
 * Returned object has *content-based stability*: when consecutive game state updates don't
 * change the visible battlefield (hand draws, phase changes, etc.), we return the previous
 * object ref. That lets `Battlefield` and its downstream `useMemo`s bail out entirely —
 * without this, every server message replaces every card ref and forces a re-render.
 *
 * `opponentId` scopes the opponent* groups to one seat's permanents (a multiplayer
 * opponent board). When omitted, the opponent groups hold everything the viewing
 * player doesn't control — the 2-player shape, unchanged.
 */
export function useBattlefieldCards(opponentId?: EntityId | null): BattlefieldCards {
  const gameState = useGameStore(selectGameState)
  const playerId = useGameStore(selectViewingPlayerId)
  const previousRef = useRef<BattlefieldCards | null>(null)

  return useMemo(() => {
    const empty: BattlefieldCards = {
      playerLands: [],
      playerCreatures: [],
      playerPlaneswalkers: [],
      playerOther: [],
      opponentLands: [],
      opponentCreatures: [],
      opponentPlaneswalkers: [],
      opponentOther: [],
      attachmentsByCardId: EMPTY_ATTACHMENTS,
    }

    if (!gameState || !playerId) {
      if (previousRef.current && battlefieldCardsEqual(previousRef.current, empty)) {
        return previousRef.current
      }
      previousRef.current = empty
      return empty
    }

    // Find ALL battlefield zones (there's one per player)
    const battlefields = gameState.zones.filter((z) => z.zoneId.zoneType === ZoneType.BATTLEFIELD)
    if (battlefields.length === 0) {
      if (previousRef.current && battlefieldCardsEqual(previousRef.current, empty)) {
        return previousRef.current
      }
      previousRef.current = empty
      return empty
    }

    // Aggregate all card IDs from all battlefield zones
    const allCardIds = battlefields.flatMap((z) => z.cardIds ?? [])
    const cards = allCardIds
      .map((id) => gameState.cards[id])
      .filter((card): card is ClientCard => card !== null && card !== undefined)

    const isNotAttached = (c: ClientCard) => !c.attachedTo
    const playerCards = cards.filter((c) => c.controllerId === playerId)
    const opponentCards = opponentId
      ? cards.filter((c) => c.controllerId === opponentId)
      : cards.filter((c) => c.controllerId !== playerId)

    const isLand = (c: ClientCard) => c.cardTypes.includes('LAND')
    const isCreature = (c: ClientCard) => c.cardTypes.includes('CREATURE')
    const isPlaneswalker = (c: ClientCard) => c.cardTypes.includes('PLANESWALKER')

    // Animated lands (both creature + land) should appear in the creatures row
    const isNonCreatureLand = (c: ClientCard) => isLand(c) && !isCreature(c)

    // Build attachment / linked-exile resolution once so Battlefield doesn't have to
    // subscribe to the full game state or re-resolve these per render.
    const attachmentsByCardId = new Map<EntityId, BattlefieldAttachments>()
    for (const c of cards) {
      const hasAttachments = c.attachments.length > 0
      const hasLinkedExile = c.linkedExile && c.linkedExile.length > 0
      if (!hasAttachments && !hasLinkedExile) continue
      const attachments = hasAttachments
        ? c.attachments
            .map((id) => gameState.cards[id])
            .filter((x): x is ClientCard => x != null)
        : []
      const linkedExile = hasLinkedExile
        ? c.linkedExile!
            .map((id) => gameState.cards[id])
            .filter((x): x is ClientCard => x != null)
        : []
      attachmentsByCardId.set(c.id, { attachments, linkedExile })
    }

    const result: BattlefieldCards = {
      playerLands: playerCards.filter((c) => isNonCreatureLand(c) && isNotAttached(c)),
      playerCreatures: playerCards.filter((c) => isCreature(c) && isNotAttached(c)),
      playerPlaneswalkers: playerCards.filter((c) => isPlaneswalker(c) && !isCreature(c) && !isLand(c) && isNotAttached(c)),
      playerOther: playerCards.filter((c) => !isCreature(c) && !isPlaneswalker(c) && !isLand(c) && isNotAttached(c)),
      opponentLands: opponentCards.filter((c) => isNonCreatureLand(c) && isNotAttached(c)),
      opponentCreatures: opponentCards.filter((c) => isCreature(c) && isNotAttached(c)),
      opponentPlaneswalkers: opponentCards.filter((c) => isPlaneswalker(c) && !isCreature(c) && !isLand(c) && isNotAttached(c)),
      opponentOther: opponentCards.filter((c) => !isCreature(c) && !isPlaneswalker(c) && !isLand(c) && isNotAttached(c)),
      attachmentsByCardId,
    }

    if (previousRef.current && battlefieldCardsEqual(previousRef.current, result)) {
      return previousRef.current
    }
    previousRef.current = result
    return result
  }, [gameState, playerId, opponentId])
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
 *
 * The goal is that two cards share a key only when they have *exactly the same projected
 * status* — same printed identity AND same battlefield state (counters, P/T, damage,
 * tapped, combat assignment, summoning sickness, chosen attributes, granted keywords,
 * designations, …). That way a stack of N identical tokens collapses into one visual
 * group, but the moment one of them is buffed, tapped, attacks, etc. it splits back out.
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

  // Cards with attachments (auras/equipment) should never be grouped — use unique ID
  if (card.attachments.length > 0) {
    parts.push(`id:${card.id}`)
  }

  // Cards with linked-exiled cards (e.g. Suspension Field) carry hidden state — never group
  if (card.linkedExile && card.linkedExile.length > 0) {
    parts.push(`id:${card.id}`)
  }

  // Cards with damage are different (for battlefield creatures)
  if (card.damage != null && card.damage > 0) {
    parts.push(`damage:${card.damage}`)
  }

  // Tapped cards are different from untapped
  if (card.isTapped) {
    parts.push('tapped')
  }

  // Combat participants are different from idle permanents, and from each other when they
  // attack/block different things (drives distinct targeting arrows, so they can't share a slot).
  if (card.isAttacking) {
    parts.push(`attacking:${card.attackingTarget ?? ''}`)
  }
  if (card.isBlocking) {
    parts.push(`blocking:${card.blockingTarget ?? ''}`)
  }

  // Summoning-sick creatures behave differently (can't attack/tap) — keep them visually distinct.
  if (card.hasSummoningSickness) {
    parts.push('sick')
  }

  // Phased-out permanents render translucent and are functionally absent — never mix with present ones.
  if (card.isPhasedOut) {
    parts.push('phased')
  }

  // Transformed cards are different
  if (card.isTransformed) {
    parts.push('transformed')
  }

  // Face-down cards are different
  if (card.isFaceDown) {
    parts.push('facedown')
  }

  // "As enters, choose ..." selections (creature type / color / mode) are rendered as badges.
  if (card.chosenCreatureType) parts.push(`ct:${card.chosenCreatureType}`)
  if (card.chosenColor) parts.push(`cc:${card.chosenColor}`)
  if (card.chosenMode) parts.push(`cm:${card.chosenMode}`)

  // Class/Saga progress is a per-permanent badge, not shared identity.
  if (card.classLevel != null) parts.push(`class:${card.classLevel}`)

  // Special designations carry prominent badges.
  if (card.isSuspected) parts.push('suspected')
  if (card.isRingBearer) parts.push('ringbearer')
  if (card.isCommander) parts.push('commander')

  // Copy provenance is badged and shown in details, so a token copy doesn't merge with the original.
  if (card.copyOf) parts.push(`copy:${card.copyOf}`)
  if (card.nonLegendaryCopy) parts.push('nonleg')

  // Granted/projected keywords, ability flags and protections differ when one copy is
  // pumped or granted an ability (without an attachment) but its twin isn't.
  if (card.keywords.length > 0) {
    parts.push(`kw:${[...card.keywords].sort().join(',')}`)
  }
  if (card.abilityFlags && card.abilityFlags.length > 0) {
    parts.push(`af:${[...card.abilityFlags].sort().join(',')}`)
  }
  if (card.protections && card.protections.length > 0) {
    parts.push(`prot:${[...card.protections].sort().join(',')}`)
  }
  if (card.hexproofFromColors && card.hexproofFromColors.length > 0) {
    parts.push(`hpx:${[...card.hexproofFromColors].sort().join(',')}`)
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

/**
 * Hook to get "ghost" cards — graveyard cards that have legal activated abilities
 * or castable spells (e.g., flashback), top-of-library cards playable via
 * Future Sight-like effects, and exile cards playable via Mind's Desire-like effects.
 * These are shown as translucent cards appended to the player's hand for discoverability.
 * Excludes simple mana abilities and unaffordable actions (same filtering as useHasLegalActions).
 */
export function useGhostCards(playerId: EntityId | null): readonly ClientCard[] {
  const gameState = useGameStore(selectGameState)
  const legalActions = useGameStore(selectLegalActions)

  return useMemo(() => {
    if (!gameState || !playerId) return []

    const ghostCardIds = new Set<EntityId>()

    // 1. Graveyard cards with legal activated abilities or castable spells (e.g., flashback)
    const gyZoneId = graveyard(playerId)
    const gyZone = gameState.zones.find((z) => zoneIdEquals(z.zoneId, gyZoneId))
    if (gyZone && gyZone.cardIds && gyZone.cardIds.length > 0) {
      const gyCardIds = new Set(gyZone.cardIds)
      for (const actionInfo of legalActions) {
        const action = actionInfo.action
        if (action.type === 'ActivateAbility') {
          if (!gyCardIds.has(action.sourceId)) continue
          if (actionInfo.isManaAbility && !actionInfo.additionalCostInfo) continue
          if (actionInfo.isAffordable === false) continue
          ghostCardIds.add(action.sourceId)
        } else if (action.type === 'CastSpell' && actionInfo.sourceZone === 'GRAVEYARD') {
          if (!gyCardIds.has(action.cardId)) continue
          if (actionInfo.isAffordable === false) continue
          ghostCardIds.add(action.cardId)
        } else if (action.type === 'PlayLand' && actionInfo.sourceZone === 'GRAVEYARD') {
          if (!gyCardIds.has(action.cardId)) continue
          ghostCardIds.add(action.cardId)
        }
      }
    }

    // 2. Top-of-library card revealed via Future Sight-like effects
    // Always show the revealed top card as a ghost card, even when it's not playable
    const libZoneId = library(playerId)
    const libZone = gameState.zones.find((z) => zoneIdEquals(z.zoneId, libZoneId))
    if (libZone && libZone.cardIds && libZone.cardIds.length > 0) {
      const topCardId = libZone.cardIds[0]!
      if (gameState.cards[topCardId]) {
        ghostCardIds.add(topCardId)
      }
    }

    // 3. Exile cards playable via Mind's Desire-like effects
    // Check ALL exile zones because cards like Kheru Spellsnatcher exile opponent's spells
    // (cards stay in their owner's exile zone but are playable by the Spellsnatcher's controller)
    for (const zone of gameState.zones) {
      if (zone.zoneId.zoneType !== ZoneType.EXILE) continue
      for (const cardId of zone.cardIds) {
        const card = gameState.cards[cardId]
        if (card?.playableFromExile) {
          ghostCardIds.add(cardId)
        }
      }
    }

    // Return deduplicated ClientCard objects
    return Array.from(ghostCardIds)
      .map((id) => gameState.cards[id])
      .filter((card): card is ClientCard => card != null)
  }, [gameState, legalActions, playerId])
}

/**
 * Hook to get revealed top-of-library cards for any player (for opponent display).
 * Returns the top card of a player's library if it's visible in the game state,
 * which happens when they control a Future Sight-like permanent.
 */
export function useRevealedLibraryTopCard(playerId: EntityId | null): ClientCard | null {
  const gameState = useGameStore(selectGameState)

  return useMemo(() => {
    if (!gameState || !playerId) return null

    const libZoneId = library(playerId)
    const libZone = gameState.zones.find((z) => zoneIdEquals(z.zoneId, libZoneId))
    if (!libZone || !libZone.cardIds || libZone.cardIds.length === 0) return null

    // The first visible card in the library zone is the revealed top card
    const topCardId = libZone.cardIds[0]!
    return gameState.cards[topCardId] ?? null
  }, [gameState, playerId])
}
