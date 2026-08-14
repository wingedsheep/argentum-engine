/**
 * Handlers for gameplay messages: state updates, mulligan, game lifecycle, and errors.
 */
import type { MessageHandlers } from '@/network/messageHandlers.ts'
import { ZoneType } from '@/types'
import type { EntityId } from '@/types'
import type { ClientGameState, ClientEvent, LegalActionInfo, PendingDecision, OpponentDecisionStatus, PriorityModeValue, Step } from '@/types'
import { trackEvent, setInGame } from '@/utils/analytics.ts'
import { applyStateDelta } from '@/network/deltaApplicator.ts'
import { getWebSocket, clearLobbyId, requestReauth } from '../shared'
import type { SetState, GetState } from './types'
import type {
  LogEntry,
  DrawAnimation,
  DamageAnimation,
  RevealAnimation,
  CoinFlipAnimation,
  TargetReselectedAnimation,
} from '../types'

/**
 * Extract the relevant player ID from an event for log coloring.
 */
function getEventPlayerId(event: { type: string; playerId?: string; casterId?: string; controllerId?: string; attackingPlayerId?: string; viewingPlayerId?: string; revealingPlayerId?: string; activePlayerId?: string; newControllerId?: string }): EntityId | null {
  switch (event.type) {
    case 'lifeChanged': return event.playerId as EntityId
    case 'cardDrawn': return event.playerId as EntityId
    case 'cardDiscarded': return event.playerId as EntityId
    case 'manaAdded': return event.playerId as EntityId
    case 'playerLost': return event.playerId as EntityId
    case 'spellCast': return event.casterId as EntityId
    case 'permanentEntered': return event.controllerId as EntityId
    case 'creatureAttacked': return event.attackingPlayerId as EntityId
    case 'handLookedAt': return event.viewingPlayerId as EntityId
    case 'handRevealed': return event.revealingPlayerId as EntityId
    case 'cardsRevealed': return event.revealingPlayerId as EntityId
    case 'turnedFaceUp': return event.controllerId as EntityId
    case 'transformed': return event.controllerId as EntityId
    case 'coinFlipped': return event.playerId as EntityId
    case 'turnChanged': return event.activePlayerId as EntityId
    case 'permanentsSacrificed': return event.playerId as EntityId
    case 'cardCycled': return event.playerId as EntityId
    case 'libraryShuffled': return event.playerId as EntityId
    case 'controlChanged': return event.newControllerId as EntityId
    case 'decisionMade': return event.playerId as EntityId
    default: return null
  }
}

/**
 * True when every revealed card ID is present in the given SelectCards decision's
 * selectable + non-selectable options. Used to suppress a redundant reveal overlay
 * for the caster when the selection modal already displays the revealed cards.
 */
function isRevealCoveredBySelectDecision(
  revealedIds: readonly EntityId[],
  options: readonly EntityId[],
  nonSelectableOptions: readonly EntityId[]
): boolean {
  if (revealedIds.length === 0) return false
  const covered = new Set<EntityId>([...options, ...nonSelectableOptions])
  return revealedIds.every((id) => covered.has(id))
}

/**
 * Map a game log the server has resent into log entries, reusing the entries already built for
 * the prefix. The log is append-only, so everything but the new tail is unchanged — rebuilding
 * it wholesale meant every click late in a game re-mapped hundreds of entries it had already
 * mapped. Falls back to a full rebuild if the log ever shrinks (a new game, or an undo).
 */
function appendGameLogTail(
  existing: readonly LogEntry[],
  gameLog: readonly ClientEvent[] | undefined
): readonly LogEntry[] {
  const log = gameLog ?? []
  if (log.length === existing.length) return existing
  const reusable = log.length > existing.length ? existing.length : 0
  const entries: LogEntry[] = reusable > 0 ? existing.slice(0, reusable) : []
  const now = Date.now()
  for (let i = reusable; i < log.length; i++) {
    const e = log[i] as { type: string; description: string; playerId?: string; casterId?: string; controllerId?: string; attackingPlayerId?: string; viewingPlayerId?: string; revealingPlayerId?: string; activePlayerId?: string; newControllerId?: string }
    entries.push({
      description: e.description,
      playerId: getEventPlayerId(e),
      timestamp: now,
      type: getEventLogType(e.type),
    })
  }
  return entries
}

function getEventLogType(eventType: string): 'action' | 'turn' | 'combat' | 'system' {
  switch (eventType) {
    case 'turnChanged': return 'turn'
    case 'creatureAttacked':
    case 'creatureBlocked': return 'combat'
    case 'abilityFizzled':
    case 'abilityAutoAnswered':
    case 'gameEnded':
    case 'playerLost': return 'system'
    default: return 'action'
  }
}

type RawCardsRevealedEvent = {
  type: 'cardsRevealed'
  revealingPlayerId: EntityId
  cardIds: readonly EntityId[]
  cardNames: readonly string[]
  imageUris: readonly (string | null)[]
  source: string | null
  cardOwnerIds?: readonly EntityId[]
  fromZone?: string | null
  toZone?: string | null
}

/**
 * Merge every cardsRevealed event in a state update into one overlay payload. A single
 * resolution can reveal cards more than once (e.g. a creature exploring twice), and the
 * overlay can only show one reveal — so combine them rather than dropping all but the first.
 *
 * Fields that disagree across events degrade gracefully: mixed sources → no source label,
 * mixed zone transitions → plain reveal, mixed revealers → per-card owner attribution
 * (the same mechanism multi-player reveals like Psychic Battle already use). A card
 * revealed twice in the same update is shown once.
 */
function mergeCardsRevealedEvents(
  events: readonly RawCardsRevealedEvent[]
): RawCardsRevealedEvent | undefined {
  if (events.length <= 1) return events[0]

  const first = events[0]!
  const sameRevealer = events.every((e) => e.revealingPlayerId === first.revealingPlayerId)
  const needOwners = !sameRevealer || events.some((e) => e.cardOwnerIds && e.cardOwnerIds.length > 0)
  const sources = [...new Set(events.map((e) => e.source).filter((s): s is string => s != null))]
  const sameTransition = events.every(
    (e) => (e.fromZone ?? null) === (first.fromZone ?? null) && (e.toZone ?? null) === (first.toZone ?? null)
  )

  const seen = new Set<EntityId>()
  const cardIds: EntityId[] = []
  const cardNames: string[] = []
  const imageUris: (string | null)[] = []
  const cardOwnerIds: EntityId[] = []
  for (const e of events) {
    e.cardIds.forEach((id, i) => {
      if (seen.has(id)) return
      seen.add(id)
      cardIds.push(id)
      cardNames.push(e.cardNames[i] ?? 'Unknown Card')
      imageUris.push(e.imageUris[i] ?? null)
      cardOwnerIds.push(e.cardOwnerIds?.[i] ?? e.revealingPlayerId)
    })
  }

  return {
    type: 'cardsRevealed',
    revealingPlayerId: first.revealingPlayerId,
    cardIds,
    cardNames,
    imageUris,
    source: sources.length === 1 ? sources[0]! : null,
    ...(needOwners ? { cardOwnerIds } : {}),
    fromZone: sameTransition ? first.fromZone ?? null : null,
    toZone: sameTransition ? first.toZone ?? null : null,
  }
}

/**
 * Common state update fields shared by both full and delta update messages.
 */
interface StateUpdateEnvelope {
  readonly events: readonly ClientEvent[]
  readonly legalActions: readonly LegalActionInfo[]
  readonly pendingDecision?: PendingDecision
  readonly nextStopPoint?: string | null
  readonly opponentDecisionStatus?: OpponentDecisionStatus | null
  readonly stopOverrides?: { readonly myTurnStops: readonly string[]; readonly opponentTurnStops: readonly string[] } | null
  readonly undoAvailable?: boolean
  readonly priorityMode?: PriorityModeValue | null
}

/**
 * Process a state update — shared between full StateUpdate and StateDeltaUpdate.
 * Takes the resolved (full) ClientGameState and the envelope fields.
 */
function processStateUpdate(
  resolvedState: ClientGameState,
  msg: StateUpdateEnvelope,
  set: SetState,
  get: GetState
): void {
  const { playerId, addBeholdPulse, reconcileBeholdPulses } = get()

  // Animations spawned by this update are collected here and committed with the state itself,
  // in one store write. Each separate write re-runs every subscriber's selector across the
  // whole board — a combat update that draws a card and changes two life totals used to cost
  // four of those passes before anything could paint.
  const newDrawAnimations: DrawAnimation[] = []
  const newDamageAnimations: DamageAnimation[] = []
  const newRevealAnimations: RevealAnimation[] = []
  const newCoinFlipAnimations: CoinFlipAnimation[] = []
  const newTargetReselectedAnimations: TargetReselectedAnimation[] = []

  // Check for hand reveal events
  const handLookedAtEvent = msg.events.find(
    (e) => e.type === 'handLookedAt'
  ) as { type: 'handLookedAt'; cardIds: readonly EntityId[] } | undefined

  const handRevealedEvent = msg.events.find(
    (e) => e.type === 'handRevealed' && (e as { revealingPlayerId: EntityId }).revealingPlayerId !== playerId
  ) as { type: 'handRevealed'; cardIds: readonly EntityId[] } | undefined

  const cardsRevealedEvents = msg.events.filter(
    (e) => e.type === 'cardsRevealed'
  ) as RawCardsRevealedEvent[]

  // A single resolution can reveal more than once (e.g. Defossilize's creature explores
  // twice → two cardsRevealed events in one update). Merge them into one overlay payload
  // instead of taking the first and silently dropping the rest.
  const cardsRevealedEventRaw = mergeCardsRevealedEvents(cardsRevealedEvents)

  // A single effect can reveal cards from more than one player at once (Psychic Battle: each
  // player reveals their top card). The event carries per-card owners in that case; derive a
  // "revealed by the viewing player?" flag per card so the overlay can tag each one.
  const cardsRevealedEvent = cardsRevealedEventRaw
    ? {
        ...cardsRevealedEventRaw,
        ...(cardsRevealedEventRaw.cardOwnerIds && cardsRevealedEventRaw.cardOwnerIds.length > 0
          ? { cardOwnerIsYours: cardsRevealedEventRaw.cardOwnerIds.map((ownerId) => ownerId === playerId) }
          : {}),
      }
    : undefined

  const faceDownCastByOpponent = msg.events.some(
    (e) => e.type === 'spellCast' &&
      (e as { casterId?: EntityId; spellName?: string }).casterId !== playerId &&
      (e as { spellName?: string }).spellName === 'Face-down creature'
  )

  // Partition revealed cards into battlefield vs. other zones. Battlefield cards are already
  // public info, so instead of the reveal overlay (which misrepresents them as hidden→shown)
  // we pulse the permanent in place. Cards still hidden (hand) use the existing overlay.
  //
  // Exception: zone-transition reveals (fromZone/toZone set — e.g., graveyard → battlefield
  // via reanimation) always use the overlay so the opponent sees *what* came back and *why*.
  const battlefieldCardIds = new Set<EntityId>(
    resolvedState.zones
      .filter((z) => z.zoneId.zoneType === 'Battlefield')
      .flatMap((z) => z.cardIds)
  )
  const isZoneTransitionReveal = !!(cardsRevealedEvent?.fromZone && cardsRevealedEvent?.toZone)
  const beheldBattlefieldIds = cardsRevealedEvent && !isZoneTransitionReveal
    ? cardsRevealedEvent.cardIds.filter((id) => battlefieldCardIds.has(id))
    : []
  const revealOverlayIndices = cardsRevealedEvent
    ? isZoneTransitionReveal
      ? cardsRevealedEvent.cardIds.map((_, i) => i)
      : cardsRevealedEvent.cardIds
          .map((id, i) => (battlefieldCardIds.has(id) ? -1 : i))
          .filter((i) => i >= 0)
    : []
  const filteredReveal = cardsRevealedEvent && revealOverlayIndices.length > 0
    ? {
        ...cardsRevealedEvent,
        cardIds: revealOverlayIndices.map((i) => cardsRevealedEvent.cardIds[i]!),
        cardNames: revealOverlayIndices.map((i) => cardsRevealedEvent.cardNames[i]!),
        imageUris: revealOverlayIndices.map((i) => cardsRevealedEvent.imageUris[i]!),
        ...(cardsRevealedEvent.cardOwnerIsYours
          ? { cardOwnerIsYours: revealOverlayIndices.map((i) => cardsRevealedEvent.cardOwnerIsYours![i]!) }
          : {}),
      }
    : null

  const currentVisibleHandCardIds = new Set<EntityId>(
    resolvedState.zones
      .filter((z) => z.zoneId.zoneType === ZoneType.HAND)
      .flatMap((z) => z.cardIds)
      .filter((id) => resolvedState.cards[id] != null)
  )

  const filterCurrentVisibleHandIds = (cardIds: readonly EntityId[]): readonly EntityId[] =>
    cardIds.filter((id) => currentVisibleHandCardIds.has(id))

  if (cardsRevealedEvent && beheldBattlefieldIds.length > 0 && cardsRevealedEvent.source) {
    for (const id of beheldBattlefieldIds) {
      addBeholdPulse(id, cardsRevealedEvent.source)
    }
  }

  // Clear any pulses whose beholding spell/ability has left the stack (resolved,
  // countered, or otherwise gone). Runs on every state update so the pulse tracks
  // the lifetime of the stack item that caused it.
  const stackZone = resolvedState.zones.find((z) => z.zoneId.zoneType === 'Stack')
  const stackItemNames = (stackZone?.cardIds ?? [])
    .map((id) => resolvedState.cards[id]?.name)
    .filter((n): n is string => typeof n === 'string')
  reconcileBeholdPulses(stackItemNames)

  // Process card draw events for animations
  const cardDrawnEvents = msg.events.filter((e) => e.type === 'cardDrawn') as {
    type: 'cardDrawn'
    playerId: EntityId
    cardId: EntityId
    cardName: string | null
  }[]

  cardDrawnEvents.forEach((event, index) => {
    const isOpponent = event.playerId !== playerId
    const card = resolvedState.cards[event.cardId]
    newDrawAnimations.push({
      id: `draw-${event.cardId}-${Date.now()}-${index}`,
      cardId: event.cardId,
      cardName: event.cardName,
      imageUri: card?.imageUri ?? null,
      isOpponent,
      startTime: Date.now() + index * 100,
    })
  })

  // Process life changed events for animations
  const lifeChangedEvents = msg.events.filter(
    (e) => e.type === 'lifeChanged' && (e as { change: number }).change !== 0
  ) as {
    type: 'lifeChanged'
    playerId: EntityId
    change: number
  }[]

  const playerLifeChanges = new Map<EntityId, { damage: number; lifeGain: number }>()
  lifeChangedEvents.forEach((event) => {
    const current = playerLifeChanges.get(event.playerId) ?? { damage: 0, lifeGain: 0 }
    if (event.change < 0) {
      current.damage += Math.abs(event.change)
    } else {
      current.lifeGain += event.change
    }
    playerLifeChanges.set(event.playerId, current)
  })

  let animIndex = 0
  playerLifeChanges.forEach((changes, targetPlayerId) => {
    if (changes.damage > 0) {
      newDamageAnimations.push({
        id: `life-${targetPlayerId}-${Date.now()}-damage`,
        targetId: targetPlayerId,
        targetIsPlayer: true,
        amount: changes.damage,
        isLifeGain: false,
        startTime: Date.now() + animIndex * 50,
      })
      animIndex++
    }
    if (changes.lifeGain > 0) {
      newDamageAnimations.push({
        id: `life-${targetPlayerId}-${Date.now()}-gain`,
        targetId: targetPlayerId,
        targetIsPlayer: true,
        amount: changes.lifeGain,
        isLifeGain: true,
        startTime: Date.now() + animIndex * 50,
      })
      animIndex++
    }
  })

  // Process morph face-up events for reveal animations
  const turnedFaceUpEvents = msg.events.filter((e) => e.type === 'turnedFaceUp') as {
    type: 'turnedFaceUp'
    cardId: EntityId
    cardName: string
    controllerId: EntityId
  }[]

  turnedFaceUpEvents.forEach((event, index) => {
    const isOpponent = event.controllerId !== playerId
    const card = resolvedState.cards[event.cardId]
    newRevealAnimations.push({
      id: `reveal-${event.cardId}-${Date.now()}-${index}`,
      cardName: event.cardName,
      imageUri: card?.imageUri ?? null,
      isOpponent,
      startTime: Date.now() + index * 200,
    })
  })

  // Process coin flip events for animations
  const coinFlipEvents = msg.events.filter((e) => e.type === 'coinFlipped') as {
    type: 'coinFlipped'
    playerId: EntityId
    won: boolean
    sourceId: EntityId
    sourceName: string
  }[]

  coinFlipEvents.forEach((event, index) => {
    const isOpponent = event.playerId !== playerId
    newCoinFlipAnimations.push({
      id: `coin-${event.sourceId}-${Date.now()}-${index}`,
      sourceName: event.sourceName,
      won: isOpponent ? !event.won : event.won,
      isOpponent,
      startTime: Date.now() + index * 200,
    })
  })

  // Process target reselection events for animations
  const targetReselectedEvents = msg.events.filter((e) => e.type === 'targetReselected') as {
    type: 'targetReselected'
    spellOrAbilityName: string
    oldTargetName: string
    newTargetName: string
    sourceName: string
  }[]

  targetReselectedEvents.forEach((event, index) => {
    newTargetReselectedAnimations.push({
      id: `reselect-${Date.now()}-${index}`,
      spellOrAbilityName: event.spellOrAbilityName,
      oldTargetName: event.oldTargetName,
      newTargetName: event.newTargetName,
      sourceName: event.sourceName,
      startTime: Date.now() + index * 200,
    })
  })

  // Sync stop overrides from server echo
  const serverOverrides = msg.stopOverrides
    ? {
        myTurnStops: msg.stopOverrides.myTurnStops as unknown as Step[],
        opponentTurnStops: msg.stopOverrides.opponentTurnStops as unknown as Step[],
      }
    : undefined

  // Sync priority mode from server echo
  const serverPriorityMode = msg.priorityMode ?? undefined

  set((state) => ({
    gameState: resolvedState,
    legalActions: msg.legalActions,
    pendingDecision: msg.pendingDecision ?? null,
    opponentDecisionStatus: msg.opponentDecisionStatus ?? null,
    nextStopPoint: msg.nextStopPoint ?? null,
    undoAvailable: msg.undoAvailable ?? false,
    ...(serverPriorityMode ? { priorityMode: serverPriorityMode, fullControl: serverPriorityMode === 'fullControl' } : {}),
    ...(serverOverrides ? { stopOverrides: serverOverrides } : {}),
    // The server resends the whole game log every update, but it only ever grows at the tail:
    // re-mapping all of it each time did work proportional to the game's length so far, so a
    // long game paid steadily more per click. Map only the new tail and keep the prefix's
    // existing entries, which also keeps their identity stable for the log list.
    eventLog: appendGameLogTail(state.eventLog, resolvedState.gameLog),
    ...(newDrawAnimations.length > 0 ? { drawAnimations: [...state.drawAnimations, ...newDrawAnimations] } : {}),
    ...(newDamageAnimations.length > 0 ? { damageAnimations: [...state.damageAnimations, ...newDamageAnimations] } : {}),
    ...(newRevealAnimations.length > 0 ? { revealAnimations: [...state.revealAnimations, ...newRevealAnimations] } : {}),
    ...(newCoinFlipAnimations.length > 0 ? { coinFlipAnimations: [...state.coinFlipAnimations, ...newCoinFlipAnimations] } : {}),
    ...(newTargetReselectedAnimations.length > 0
      ? { targetReselectedAnimations: [...state.targetReselectedAnimations, ...newTargetReselectedAnimations] }
      : {}),
    waitingForOpponentMulligan: false,
    revealedHandCardIds: (() => {
      if (faceDownCastByOpponent) return null
      const newIds = handLookedAtEvent?.cardIds ?? handRevealedEvent?.cardIds
      if (!newIds) {
        if (!state.revealedHandCardIds) return null
        const currentIds = filterCurrentVisibleHandIds(state.revealedHandCardIds)
        return currentIds.length > 0 ? currentIds : null
      }
      // Combined reveal+select UX: when the new hand reveal is paired with a
      // SelectCards decision assigned to this player that already displays every
      // revealed card, the decision modal IS the reveal for them — skip the
      // overlay so they don't have to dismiss a redundant view. (Used by
      // Auntie's Sentence / Despise / Mardu Charm's reveal-and-discard flow.)
      if (
        msg.pendingDecision?.type === 'SelectCardsDecision' &&
        msg.pendingDecision.playerId === playerId &&
        isRevealCoveredBySelectDecision(
          newIds,
          msg.pendingDecision.options,
          msg.pendingDecision.nonSelectableOptions ?? []
        )
      ) {
        return null
      }
      return newIds
    })(),
    // Combined reveal+select UX (e.g., Aurora Awakener's Vivid ETB): if this update
    // carries both a reveal to the caster AND a SelectCards decision covering every
    // revealed card, the selection modal is the reveal for them — suppress the
    // overlay entirely so it doesn't appear before or after the selection resolves.
    revealedCardsInfo: filteredReveal
      ? (filteredReveal.revealingPlayerId === playerId &&
         msg.pendingDecision?.type === 'SelectCardsDecision' &&
         isRevealCoveredBySelectDecision(
           filteredReveal.cardIds,
           msg.pendingDecision.options,
           msg.pendingDecision.nonSelectableOptions ?? []
         )
          ? null
          : { cardIds: filteredReveal.cardIds, cardNames: filteredReveal.cardNames, imageUris: filteredReveal.imageUris, source: filteredReveal.source, isYourReveal: filteredReveal.revealingPlayerId === playerId, fromZone: filteredReveal.fromZone ?? null, toZone: filteredReveal.toZone ?? null, ...(filteredReveal.cardOwnerIsYours ? { cardOwnerIsYours: filteredReveal.cardOwnerIsYours } : {}) })
      : cardsRevealedEvent ? null : state.revealedCardsInfo,
    opponentAttackerTargets: resolvedState.combat ? null : state.opponentAttackerTargets,
    opponentBlockerAssignments: (resolvedState.combat?.blockers?.length || !resolvedState.combat) ? null : state.opponentBlockerAssignments,
  }))

  // Auto-initialize inline distribute state for DistributeDecision
  const decision = msg.pendingDecision
  if (decision?.type === 'DistributeDecision') {
    const initial: Record<string, number> = {}
    for (const targetId of decision.targets) {
      initial[targetId] = decision.minPerTarget
    }
    get().initDistribute({
      decisionId: decision.id,
      prompt: decision.prompt,
      totalAmount: decision.totalAmount,
      targets: decision.targets,
      minPerTarget: decision.minPerTarget,
      ...(decision.maxPerTarget ? { maxPerTarget: decision.maxPerTarget } : {}),
      ...(decision.allowPartial ? { allowPartial: decision.allowPartial } : {}),
      distribution: initial,
    })
  } else if (!decision && get().distributeState) {
    // Clear distribute state when decision goes away
    get().clearDistribute()
  }
}

type GameplayHandlerKeys =
  | 'onGameCreated' | 'onGameStarted' | 'onGameCancelled'
  | 'onStateUpdate' | 'onStateDeltaUpdate'
  | 'onMulliganDecision' | 'onChooseBottomCards' | 'onMulliganComplete' | 'onWaitingForOpponentMulligan'
  | 'onGameOver' | 'onPlayerEliminated' | 'onError'

export function createGameplayHandlers(set: SetState, get: GetState): Pick<MessageHandlers, GameplayHandlerKeys> {
  return {
    onGameCreated: (msg) => {
      // Clear any quick-game lobby state — the lobby has done its job (server already removed it).
      set({ sessionId: msg.sessionId, quickGameLobbyState: null })
    },

    onGameStarted: (msg) => {
      // Derive "the opponent" from the seat roster — the first non-you seat. For 2-player this is
      // the sole opponent; the N-player UI derives everything from gameState.players directly.
      const opponentName = msg.players.find((p) => !p.isYou)?.name ?? 'Opponent'
      trackEvent('game_started', { opponent_name: opponentName, seats: msg.players.length })
      setInGame(true)

      // Clear spectating state — active game takes priority. Fresh game, fresh camera.
      set({ spectatingState: null })
      get().resetBoardView()

      // Two-Headed Giant (CR 810): the seat → team map only arrives here, in the game-start
      // roster. Stamp it now (after resetBoardView clears it) so the team-grouped rail, team
      // colors, ally board, and shared-life headers can read it for the whole game.
      const seatTeams: Record<string, number> = {}
      for (const p of msg.players) {
        if (p.teamIndex != null) seatTeams[p.playerId] = p.teamIndex
      }
      // Shared life is a game-level fact (same on every seat); 2HG shares, Team vs. Team doesn't.
      const sharedLife = msg.players.some((p) => p.teamSharedLife)
      get().setSeatTeams(seatTeams, sharedLife)

      // Load persisted stop overrides and send to server
      try {
        const saved = localStorage.getItem('argentum-stop-overrides')
        if (saved) {
          const parsed = JSON.parse(saved) as { myTurnStops: string[]; opponentTurnStops: string[] }
          if (parsed.myTurnStops?.length || parsed.opponentTurnStops?.length) {
            set({
              stopOverrides: parsed as { myTurnStops: Step[]; opponentTurnStops: Step[] },
            })
            getWebSocket()?.send({
              type: 'setStopOverrides' as const,
              myTurnStops: parsed.myTurnStops,
              opponentTurnStops: parsed.opponentTurnStops,
            })
          }
        }
      } catch { /* ignore invalid localStorage data */ }

      // Show match intro animation
      const playerName = localStorage.getItem('argentum-player-name') ?? 'You'
      const { tournamentState, playerId } = get()
      let round: number | undefined
      let playerRecord: string | undefined
      let opponentRecord: string | undefined
      if (tournamentState && playerId) {
        round = tournamentState.currentRound
        const playerStanding = tournamentState.standings.find((s) => s.playerId === playerId)
        const opponentStanding = tournamentState.standings.find((s) => s.playerName === opponentName)
        if (playerStanding) {
          playerRecord = `${playerStanding.wins}-${playerStanding.losses}${playerStanding.draws > 0 ? `-${playerStanding.draws}` : ''}`
        }
        if (opponentStanding) {
          opponentRecord = `${opponentStanding.wins}-${opponentStanding.losses}${opponentStanding.draws > 0 ? `-${opponentStanding.draws}` : ''}`
        }
      }
      set({
        matchIntro: {
          playerName,
          opponentName,
          ...(round != null ? { round } : {}),
          ...(playerRecord != null ? { playerRecord } : {}),
          ...(opponentRecord != null ? { opponentRecord } : {}),
        },
      })

      set((state) => ({
        opponentName,
        mulliganState: null,
        // Preserve the drafted deck in tournament/FFA mode so the player can still
        // view or save it from the standings screen between rounds and after
        // the tournament completes.
        deckBuildingState: state.tournamentState || state.ffaState ? state.deckBuildingState : null,
      }))
    },

    onGameCancelled: () => {
      trackEvent('game_cancelled_by_server')
      setInGame(false)
      set({
        sessionId: null,
        opponentName: null,
        gameState: null,
        legalActions: [],
        mulliganState: null,
        deckBuildingState: null,
      })
    },

    onStateUpdate: (msg) => {
      // Once we've left the game (returnToMenu nulls sessionId — e.g. an eliminated FFA player
      // who returned to the pod standings), ignore its ongoing broadcasts instead of being
      // dragged back into the board. sessionId is always set at game start before any state
      // flows, so this never drops a legitimate in-game update.
      if (get().sessionId == null) return
      getWebSocket()?.onStateVersionReceived(msg.stateVersion)
      processStateUpdate(msg.state, msg, set, get)
    },

    onStateDeltaUpdate: (msg) => {
      // See onStateUpdate: don't re-enter a game we've left (and don't request a resync for it).
      if (get().sessionId == null) return
      const ws = getWebSocket()
      ws?.onStateVersionReceived(msg.stateVersion)

      const currentState = get().gameState
      if (!currentState) {
        // No previous state to apply delta to — request a full resync.
        console.warn('[StateDelta] Received delta update but no current gameState exists. Requesting resync.')
        ws?.requestResync()
        return
      }
      const newState = applyStateDelta(currentState, msg.delta)
      processStateUpdate(newState, msg, set, get)
    },

    onMulliganDecision: (msg) => {
      set({
        mulliganState: {
          phase: 'deciding',
          hand: msg.hand,
          mulliganCount: msg.mulliganCount,
          cardsToPutOnBottom: msg.cardsToPutOnBottom,
          selectedCards: [],
          cards: msg.cards || {},
          isOnThePlay: msg.isOnThePlay,
        },
      })
    },

    onChooseBottomCards: (msg) => {
      set((state) => ({
        mulliganState: {
          phase: 'choosingBottomCards',
          hand: msg.hand,
          mulliganCount: 0,
          cardsToPutOnBottom: msg.cardsToPutOnBottom,
          selectedCards: [],
          cards: state.mulliganState?.cards || {},
          isOnThePlay: state.mulliganState?.isOnThePlay ?? false,
        },
      }))
    },

    onMulliganComplete: () => {
      set({ mulliganState: null })
    },

    onWaitingForOpponentMulligan: () => {
      set({ waitingForOpponentMulligan: true })
    },

    onGameOver: (msg) => {
      const { playerId, sessionId } = get()
      // Ignore a game-over for a game we already left — e.g. an eliminated FFA player who
      // returned to the lobby still holds a seat server-side and receives the final GameOver.
      if (msg.gameId && sessionId !== msg.gameId) return
      const result: 'win' | 'lose' | 'draw' =
        msg.winnerId === null ? 'draw' : msg.winnerId === playerId ? 'win' : 'lose'
      trackEvent('game_over', { result, reason: msg.reason })
      setInGame(false)
      set({
        gameOverState: {
          winnerId: msg.winnerId,
          reason: msg.reason,
          result,
          message: msg.message,
          gameId: msg.gameId,
        },
      })
    },

    onPlayerEliminated: (msg) => {
      // Personal elimination from a multiplayer game that continues without us (e.g. we
      // conceded a 4-player pod). Show the defeat overlay; the table plays on.
      if (msg.gameId !== get().sessionId) return
      trackEvent('game_over', { result: 'lose', reason: msg.reason, eliminated: true })
      setInGame(false)
      set({
        gameOverState: {
          winnerId: null,
          reason: msg.reason,
          result: 'lose',
          message: 'You are out of the game — the table plays on without you.',
          gameId: msg.gameId,
          eliminated: true,
        },
      })
    },

    onError: (msg) => {
      // NOT_CONNECTED means the server no longer recognizes this socket as an
      // authenticated session (e.g. it restarted while the tab was backgrounded).
      // Re-send the connect message with the stored token instead of surfacing the
      // error — the server's reconnect path restores identity, game, and lobby state.
      if (msg.code === 'NOT_CONNECTED' && requestReauth()) return
      if (msg.code === 'GAME_NOT_FOUND' || msg.message?.toLowerCase().includes('lobby')) {
        clearLobbyId()
      }
      // Suppress the error toast for "Not in a game" — this is a benign race condition
      // where an in-flight action (e.g. PassPriority) arrives after the game session has
      // already been torn down. The GameOver message handles the real cleanup; no user
      // action is required and showing a toast here is confusing.
      if (msg.code === 'GAME_NOT_FOUND' && msg.message === 'Not in a game') return
      get().setError({
        code: msg.code,
        message: msg.message,
        timestamp: Date.now(),
      })
    },
  }
}
