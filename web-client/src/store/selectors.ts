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
import { isViewerEliminated } from './slices/ui/boardViewSlice'
import { seatColor, teamColor, type SeatColor } from '@/styles/seatColors'
import {
  type GroupedCard,
  computeCardGroupKey,
  MAX_VISUAL_STACK_DEPTH,
  visibleStackDepth,
  groupCards,
} from './cardGrouping'

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
 * True when the local player's seat is out of a multiplayer game the others are still playing.
 * See [isViewerEliminated] — derived from the roster, not from the elimination message, so it
 * is right however the seat died.
 */
export function useViewerEliminated(): boolean {
  return useGameStore(isViewerEliminated)
}

/**
 * The living seat that holds the bottom half of the table for an eliminated spectator.
 * A dead player has no board of their own to sit there, so the bottom side of the table
 * belongs to a survivor: their explicit pick from the spectating banner, or — by default —
 * a living teammate (team game) else the next living seat in turn order. Self-heals when
 * that seat dies. Null outside the eliminated-spectator layout, or when nobody is left.
 */
export function useEliminatedBottomSeatId(): EntityId | null {
  const eliminated = useGameStore(isViewerEliminated)
  const pickedSeatId = useGameStore((state) => state.eliminatedBottomSeatId)
  const opponents = useOpponents()
  const teamMap = useGameStore(selectTeamMap)
  const viewerTeam = useViewerTeamIndex()
  return useMemo(() => {
    if (!eliminated) return null
    const living = opponents.filter((p) => !p.hasLost)
    const picked = living.find((p) => p.playerId === pickedSeatId)
    if (picked) return picked.playerId
    const ally = viewerTeam != null ? living.find((p) => teamMap[p.playerId] === viewerTeam) : undefined
    return (ally ?? living[0])?.playerId ?? null
  }, [eliminated, pickedSeatId, opponents, teamMap, viewerTeam])
}

/**
 * The opponent whose board occupies the viewed slot. Resolves the boardView
 * selection with fallbacks: an eliminated or unknown selection falls back to the
 * first opponent still in the game (or the first opponent, if all have lost).
 * The eliminated spectator's bottom seat never occupies the viewed slot — its board
 * is already fully visible on the bottom half of the screen.
 */
export function useViewedOpponent(): ClientPlayer | null {
  const opponents = useOpponents()
  const viewedOpponentId = useGameStore((state) => state.viewedOpponentId)
  const bottomSeatId = useEliminatedBottomSeatId()
  return useMemo(() => {
    const candidates = bottomSeatId ? opponents.filter((p) => p.playerId !== bottomSeatId) : opponents
    if (candidates.length === 0) return null
    const selected = candidates.find((p) => p.playerId === viewedOpponentId)
    if (selected && !selected.hasLost) return selected
    return candidates.find((p) => !p.hasLost) ?? candidates[0] ?? null
  }, [opponents, viewedOpponentId, bottomSeatId])
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

// ── Two-Headed Giant team helpers (CR 810) ──────────────────────────────────
// The seat → team map is stamped once from the game-start roster (boardViewSlice.teamByPlayerId).
// It's empty in every non-team game, so each helper degrades to the per-seat behavior and the
// 2-player / FFA UIs are untouched.

const EMPTY_TEAM_MAP: Readonly<Record<EntityId, number>> = Object.freeze({})

/** The seat → team-index map (empty in a non-team game). */
export const selectTeamMap = (state: GameStore): Readonly<Record<EntityId, number>> =>
  state.teamByPlayerId ?? EMPTY_TEAM_MAP

/** True when a Two-Headed Giant team game is in progress (the team map has entries). */
export const selectIsTeamGame = (state: GameStore): boolean =>
  Object.keys(state.teamByPlayerId ?? {}).length > 0

/** Hook form of [selectIsTeamGame]. */
export function useIsTeamGame(): boolean {
  return useGameStore(selectIsTeamGame)
}

/**
 * True only for a team game where teammates share one life total (Two-Headed Giant — CR 810).
 * Team vs. Team (CR 808) is a team game but each player keeps their own life, so this is false —
 * the rail then shows per-player life instead of a single shared-life team header.
 */
export function useIsSharedLifeTeamGame(): boolean {
  return useGameStore((state) => selectIsTeamGame(state) && state.teamSharedLife)
}

/** Team index of a player, or null in a non-team game / unknown player. */
export function useTeamIndex(playerId: EntityId | null): number | null {
  return useGameStore((state) => (playerId ? state.teamByPlayerId?.[playerId] ?? null : null))
}

/**
 * The viewing player's team index (null outside a team game). Drives the rail's
 * "your team vs the enemy team" split and the ally treatment.
 */
export function useViewerTeamIndex(): number | null {
  const viewerId = useGameStore(selectViewingPlayerId)
  return useGameStore((state) => (viewerId ? state.teamByPlayerId?.[viewerId] ?? null : null))
}

/**
 * Pure identity-color resolver: a player's team hue in a team game, else their per-seat hue.
 * Used both by [useIdentityColor] and inline at call sites that already hold the team map.
 */
export function identitySeatColor(
  teamMap: Readonly<Record<EntityId, number>>,
  playerId: EntityId | null,
  seatIndex: number,
): SeatColor {
  const t = playerId != null ? teamMap[playerId] : undefined
  return t != null ? teamColor(t) : seatColor(Math.max(0, seatIndex))
}

/**
 * The identity color to paint a player with: their team hue in a Two-Headed Giant game (both
 * teammates share it), otherwise their stable per-seat hue. One call for chips, orbs, arrows.
 */
export function useIdentityColor(playerId: EntityId | null): SeatColor {
  const seatIndex = useSeatIndex(playerId)
  const teamMap = useGameStore(selectTeamMap)
  return useMemo(() => identitySeatColor(teamMap, playerId, seatIndex), [teamMap, playerId, seatIndex])
}

/**
 * True when `playerId` is the viewing player's teammate (same team, not self) in a team game —
 * i.e. an ally whose board/hand you may see and whom you can't attack.
 */
export function useIsAlly(playerId: EntityId | null): boolean {
  const viewerId = useGameStore(selectViewingPlayerId)
  const teamMap = useGameStore(selectTeamMap)
  if (!playerId || !viewerId || playerId === viewerId) return false
  const vt = teamMap[viewerId]
  return vt != null && teamMap[playerId] === vt
}

const EMPTY_ACTIONS: readonly LegalActionInfo[] = Object.freeze([])

/** Extract the card-id this legal action is anchored to, if any. */
export function cardIdForAction(info: LegalActionInfo): EntityId | undefined {
  const a = info.action
  switch (a.type) {
    case 'PlayLand':
    case 'CastSpell':
    case 'CycleCard':
    case 'TypecycleCard':
    case 'PlotCard':
    case 'SuspendCardFromHand':
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

export function isHighlightable(a: LegalActionInfo): boolean {
  // A mana ability is normally not highlightable: tapping a land for mana is driven by the mana
  // payment flow, not by clicking the card. The exceptions are the ones that need something from
  // the player first — an extra cost, a mana cost of their own, or an X to choose. That last one
  // covers the storage lands ("{T}, Remove any number of storage counters: Add {B} for each"):
  // without it the card is unclickable, the X picker never opens, and the ability resolves for
  // X = 0 — counters spent, no mana produced.
  // The X exception needs its own guard: a storage land with no counters still enumerates the
  // ability (X = 0 is legal), so without this the land glows, the picker opens with maxX 0, and
  // confirming taps it for nothing.
  const needsPlayerInput =
    a.additionalCostInfo != null ||
    a.manaCostString != null ||
    (a.hasXCost === true && (a.maxAffordableX ?? 0) > 0)
  return (!a.isManaAbility || needsPlayerInput) && a.isAffordable !== false
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
 *
 * `playerIdOverride` makes the player* groups hold another seat's permanents instead of
 * the viewing player's — the eliminated spectator's chosen bottom board (spectating
 * proper doesn't need it: there `selectViewingPlayerId` already resolves to the bottom
 * seat).
 */
/**
 * Reorder a creature row so soulbond-paired creatures sit **next to each other** (CR 702.95b).
 *
 * Without this the bond `SoulbondBonds` draws can stripe across the whole row over unrelated
 * creatures that happen to sit between the pair, which reads as "these two are connected to
 * something in the middle" rather than "these two are one". Adjacency makes the bond short and its
 * meaning obvious, and it matches how a player physically slides two paired cards together.
 *
 * Stable and idempotent: cards keep their relative order except that a paired creature's partner is
 * pulled up to immediately follow it. Only the *first* of a pair moves its partner, so the pair
 * lands wherever the earlier half already was rather than jumping the row around.
 */
function orderPairsAdjacent(cards: readonly ClientCard[]): ClientCard[] {
  if (cards.length < 3) return [...cards]
  if (!cards.some((c) => c.pairedWithId)) return [...cards]

  const byId = new Map(cards.map((c) => [c.id, c]))
  const emitted = new Set<EntityId>()
  const ordered: ClientCard[] = []

  for (const card of cards) {
    if (emitted.has(card.id)) continue
    ordered.push(card)
    emitted.add(card.id)

    const partnerId = card.pairedWithId
    if (!partnerId || emitted.has(partnerId)) continue
    const partner = byId.get(partnerId)
    // The partner may be off this row entirely — an opponent's creature after a control change, or
    // a permanent that stopped being a creature. Leave the row alone in that case.
    if (!partner) continue
    ordered.push(partner)
    emitted.add(partnerId)
  }

  return ordered
}

export function useBattlefieldCards(
  opponentId?: EntityId | null,
  playerIdOverride?: EntityId | null,
): BattlefieldCards {
  const gameState = useGameStore(selectGameState)
  const viewerId = useGameStore(selectViewingPlayerId)
  const playerId = playerIdOverride ?? viewerId
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

    // A card counts as "attached" only when its host is itself a battlefield card. An
    // "enchant player" Aura (Grievous Wound) has attachedTo pointing at a player entity, which
    // is not a battlefield card — render it standalone in its controller's row rather than
    // dropping it (it is only ever rendered nested under a host card otherwise).
    const cardIdSet = new Set(cards.map((c) => c.id))
    const isNotAttached = (c: ClientCard) => !c.attachedTo || !cardIdSet.has(c.attachedTo)
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
      playerCreatures: orderPairsAdjacent(playerCards.filter((c) => isCreature(c) && isNotAttached(c))),
      playerPlaneswalkers: playerCards.filter((c) => isPlaneswalker(c) && !isCreature(c) && !isLand(c) && isNotAttached(c)),
      playerOther: playerCards.filter((c) => !isCreature(c) && !isPlaneswalker(c) && !isLand(c) && isNotAttached(c)),
      opponentLands: opponentCards.filter((c) => isNonCreatureLand(c) && isNotAttached(c)),
      opponentCreatures: orderPairsAdjacent(opponentCards.filter((c) => isCreature(c) && isNotAttached(c))),
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

const EMPTY_ID_SET: ReadonlySet<EntityId> = Object.freeze(new Set<EntityId>())

/**
 * Permanents that must render on their own card (not collapsed into a stack) so a
 * targeting arrow can anchor to their `data-card-id` DOM node — see
 * `groupCards(cards, splitOutIds)`. These are the chosen targets and triggering
 * sources of objects on the stack, plus any target already picked in an in-progress
 * cast. The set is tiny, so splitting them out doesn't re-explode a horde of tokens.
 *
 * Eligible-but-unchosen targets are deliberately NOT included: identical tokens are
 * interchangeable, so the player can click the collapsed stack's representative to
 * pick one. Only a *committed* target needs its specific arrow to resolve.
 *
 * The pending decision's *subject* is included for the same reason: Killing Wave asks
 * "pay X life or sacrifice it" once per creature, and a subject collapsed into a token
 * stack would highlight the whole pile instead of the one creature being decided.
 */
export function useSplitOutTargetIds(): ReadonlySet<EntityId> {
  const stackCards = useStackCards()
  const targetingState = useGameStore(selectTargetingState)
  const decisionSubjectId = useGameStore((s) => s.pendingDecision?.context.subjectEntityId)
  return useMemo(() => {
    const selected = targetingState?.selectedTargets
    if (stackCards.length === 0 && (!selected || selected.length === 0) && !decisionSubjectId) {
      return EMPTY_ID_SET
    }
    const ids = new Set<EntityId>()
    for (const card of stackCards) {
      for (const t of card.targets) {
        if (t.type === 'Permanent') ids.add(t.entityId)
      }
      if (card.triggeringEntityId) ids.add(card.triggeringEntityId)
    }
    if (selected) for (const id of selected) ids.add(id)
    if (decisionSubjectId) ids.add(decisionSubjectId)
    return ids
  }, [stackCards, targetingState, decisionSubjectId])
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

// Battlefield card grouping ("token quantity aggregation", display layer) lives in
// its own store-free module (imported at the top) so it stays purely a function of
// its inputs and is directly unit-testable. Re-exported here for existing call sites.
export { type GroupedCard, computeCardGroupKey, MAX_VISUAL_STACK_DEPTH, visibleStackDepth, groupCards }

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
