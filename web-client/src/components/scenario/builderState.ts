/**
 * The Scenario Builder's editing model, and its conversions to/from the wire
 * {@link ScenarioSpec} that `POST /api/scenarios` accepts.
 *
 * The builder keeps an ordered list of seats rather than the wire format's `player1`/`player2`
 * pair, so a 3-4 player pod is edited exactly like a duel. `toSpec` narrows back to the legacy
 * two-seat shape when there are only two seats (the server prefers `players` when present, but
 * the shorter form keeps share links small and matches every scenario JSON already checked in).
 */
import type {
  ScenarioBattlefieldCard,
  ScenarioMode,
  ScenarioPlayerConfig,
  ScenarioSpec,
  ScenarioZone,
} from './types'

export interface BuilderSeat {
  /** Stable identity for React keys and drag refs — never sent to the server. */
  id: string
  name: string
  life: number
  battlefield: ScenarioBattlefieldCard[]
  hand: string[]
  graveyard: string[]
  exile: string[]
  library: string[]
  commanders: string[]
}

export interface BuilderState {
  seats: BuilderSeat[]
  phase: string
  /** 1-based seat number. */
  activePlayer: number
  mode: ScenarioMode
}

export const PHASES = ['BEGINNING', 'PRECOMBAT_MAIN', 'COMBAT', 'POSTCOMBAT_MAIN', 'ENDING'] as const

export const MODE_HINT: Record<ScenarioMode, string> = {
  SELF: 'You play every seat yourself in one window.',
  AI: 'You play; the computer controls the opponent.',
  TWO_PLAYER: 'Two people — each gets their own link to join.',
}

let seatCounter = 0
function nextSeatId(): string {
  seatCounter += 1
  return `seat-${seatCounter}`
}

export function emptySeat(name: string): BuilderSeat {
  return {
    id: nextSeatId(),
    name,
    life: 20,
    battlefield: [],
    hand: [],
    graveyard: [],
    exile: [],
    library: [],
    commanders: [],
  }
}

export function emptyBuilderState(): BuilderState {
  return {
    seats: [emptySeat('Player 1'), emptySeat('Player 2')],
    phase: 'PRECOMBAT_MAIN',
    activePlayer: 1,
    mode: 'SELF',
  }
}

/** Every card name in a seat, across all zones — powers the "already placed" tile badges. */
export function seatCardNames(seat: BuilderSeat): string[] {
  return [
    ...seat.battlefield.map((c) => c.name),
    ...seat.hand,
    ...seat.graveyard,
    ...seat.exile,
    ...seat.library,
    ...seat.commanders,
  ]
}

/** Card name → total copies placed anywhere in the scenario. */
export function placedCounts(state: BuilderState): Record<string, number> {
  const out: Record<string, number> = {}
  for (const seat of state.seats) {
    for (const name of seatCardNames(seat)) out[name] = (out[name] ?? 0) + 1
  }
  return out
}

export function zoneSize(seat: BuilderSeat, zone: ScenarioZone): number {
  return zone === 'battlefield' ? seat.battlefield.length : seat[zone].length
}

// --- mutations (all pure — they return a new state) ----------------------------------------

function mapSeat(
  state: BuilderState,
  seatId: string,
  fn: (seat: BuilderSeat) => BuilderSeat,
): BuilderState {
  return { ...state, seats: state.seats.map((s) => (s.id === seatId ? fn(s) : s)) }
}

export function addCardToZone(
  state: BuilderState,
  seatId: string,
  zone: ScenarioZone,
  name: string,
  copies = 1,
): BuilderState {
  return mapSeat(state, seatId, (seat) => {
    if (zone === 'battlefield') {
      const added = Array.from({ length: copies }, () => ({ name }))
      return { ...seat, battlefield: [...seat.battlefield, ...added] }
    }
    const added = Array.from({ length: copies }, () => name)
    return { ...seat, [zone]: [...seat[zone], ...added] }
  })
}

export function removeCardAt(
  state: BuilderState,
  seatId: string,
  zone: ScenarioZone,
  index: number,
): BuilderState {
  return mapSeat(state, seatId, (seat) => {
    if (zone === 'battlefield') {
      const removed = seat.battlefield[index]
      const battlefield = seat.battlefield.filter((_, i) => i !== index)
      // Auras/Equipment attached to the removed permanent would dangle — the server rejects an
      // `attachedTo` naming a card that isn't on the same battlefield, so clear those links.
      return {
        ...seat,
        battlefield: removed
          ? battlefield.map((c) => (c.attachedTo === removed.name ? stripAttachment(c) : c))
          : battlefield,
      }
    }
    return { ...seat, [zone]: seat[zone].filter((_, i) => i !== index) }
  })
}

function stripAttachment(card: ScenarioBattlefieldCard): ScenarioBattlefieldCard {
  const { attachedTo: _attachedTo, ...rest } = card
  return rest
}

export function updateBattlefieldCard(
  state: BuilderState,
  seatId: string,
  index: number,
  patch: Partial<ScenarioBattlefieldCard>,
): BuilderState {
  return mapSeat(state, seatId, (seat) => ({
    ...seat,
    battlefield: seat.battlefield.map((c, i) => (i === index ? applyPatch(c, patch) : c)),
  }))
}

/**
 * Merge a patch, dropping keys whose new value is "unset". `exactOptionalPropertyTypes` is on,
 * so an explicit `undefined` isn't assignable to an optional field — and leaving `tapped: false`
 * or an empty counters map in the JSON is noise the tester didn't ask for.
 */
function applyPatch(
  card: ScenarioBattlefieldCard,
  patch: Partial<ScenarioBattlefieldCard>,
): ScenarioBattlefieldCard {
  const next: ScenarioBattlefieldCard = { ...card, ...patch }
  if (!next.tapped) delete next.tapped
  if (!next.summoningSickness) delete next.summoningSickness
  if (!next.attachedTo) delete next.attachedTo
  if (!next.chosenCreatureType) delete next.chosenCreatureType
  if (!next.chosenColor) delete next.chosenColor
  if (!next.chosenCardType) delete next.chosenCardType
  if (next.counters && Object.keys(next.counters).length === 0) delete next.counters
  return next
}

/** Move a card between zones (possibly across seats). Index is the position in the source zone. */
export function moveCard(
  state: BuilderState,
  from: { seatId: string; zone: ScenarioZone; index: number },
  to: { seatId: string; zone: ScenarioZone },
): BuilderState {
  const sourceSeat = state.seats.find((s) => s.id === from.seatId)
  if (!sourceSeat) return state
  const name =
    from.zone === 'battlefield'
      ? sourceSeat.battlefield[from.index]?.name
      : sourceSeat[from.zone][from.index]
  if (!name) return state
  // Carry the battlefield card's setup (tapped, counters, …) when it stays on a battlefield.
  const carried =
    from.zone === 'battlefield' && to.zone === 'battlefield'
      ? sourceSeat.battlefield[from.index]
      : null
  const removed = removeCardAt(state, from.seatId, from.zone, from.index)
  if (carried) {
    return mapSeat(removed, to.seatId, (seat) => ({
      ...seat,
      battlefield: [...seat.battlefield, { ...carried }],
    }))
  }
  return addCardToZone(removed, to.seatId, to.zone, name)
}

export function clearSeat(state: BuilderState, seatId: string): BuilderState {
  return mapSeat(state, seatId, (seat) => ({
    ...emptySeat(seat.name),
    id: seat.id,
    life: seat.life,
  }))
}

/** Swap two seats' boards, keeping their names and seat order. */
export function swapSeatBoards(state: BuilderState, aId: string, bId: string): BuilderState {
  const a = state.seats.find((s) => s.id === aId)
  const b = state.seats.find((s) => s.id === bId)
  if (!a || !b) return state
  const boardOf = (seat: BuilderSeat) => ({
    life: seat.life,
    battlefield: seat.battlefield,
    hand: seat.hand,
    graveyard: seat.graveyard,
    exile: seat.exile,
    library: seat.library,
    commanders: seat.commanders,
  })
  return {
    ...state,
    seats: state.seats.map((s) => {
      if (s.id === aId) return { ...s, ...boardOf(b) }
      if (s.id === bId) return { ...s, ...boardOf(a) }
      return s
    }),
  }
}

// --- spec conversions -----------------------------------------------------------------------

function seatConfig(seat: BuilderSeat): ScenarioPlayerConfig {
  const cfg: ScenarioPlayerConfig = { lifeTotal: seat.life }
  if (seat.hand.length) cfg.hand = seat.hand
  if (seat.battlefield.length) cfg.battlefield = seat.battlefield
  if (seat.graveyard.length) cfg.graveyard = seat.graveyard
  if (seat.exile.length) cfg.exile = seat.exile
  if (seat.library.length) cfg.library = seat.library
  if (seat.commanders.length) cfg.commanders = seat.commanders
  return cfg
}

export function toSpec(state: BuilderState): ScenarioSpec {
  const [first, second, ...rest] = state.seats
  const spec: ScenarioSpec = {
    player1Name: first?.name ?? 'Player 1',
    player2Name: second?.name ?? 'Player 2',
    player1: first ? seatConfig(first) : { lifeTotal: 20 },
    player2: second ? seatConfig(second) : { lifeTotal: 20 },
    phase: state.phase,
    activePlayer: state.activePlayer,
    mode: state.mode,
  }
  if (rest.length > 0) {
    // N-player pod: send the full seat list (the server prefers it over the legacy two-seat
    // fields). Pods only support hotseat.
    spec.players = state.seats.map((s) => ({ name: s.name, config: seatConfig(s) }))
    spec.mode = 'SELF'
  } else if (state.mode === 'AI') {
    spec.aiPlayer = 2
  }
  return spec
}

function seatFromConfig(name: string, cfg: ScenarioPlayerConfig | undefined): BuilderSeat {
  return {
    id: nextSeatId(),
    name,
    life: cfg?.lifeTotal ?? 20,
    battlefield: cfg?.battlefield ? cfg.battlefield.map((c) => ({ ...c })) : [],
    hand: cfg?.hand ? [...cfg.hand] : [],
    graveyard: cfg?.graveyard ? [...cfg.graveyard] : [],
    exile: cfg?.exile ? [...cfg.exile] : [],
    library: cfg?.library ? [...cfg.library] : [],
    commanders: cfg?.commanders ? [...cfg.commanders] : [],
  }
}

export function fromSpec(spec: ScenarioSpec): BuilderState {
  const seats =
    spec.players && spec.players.length >= 2
      ? spec.players.map((s, i) => seatFromConfig(s.name ?? `Player ${i + 1}`, s.config))
      : [
          seatFromConfig(spec.player1Name ?? 'Player 1', spec.player1),
          seatFromConfig(spec.player2Name ?? 'Player 2', spec.player2),
        ]
  return {
    seats,
    phase: spec.phase ?? 'PRECOMBAT_MAIN',
    activePlayer: spec.activePlayer ?? 1,
    mode: seats.length > 2 ? 'SELF' : (spec.mode ?? (spec.aiPlayer != null ? 'AI' : 'SELF')),
  }
}
