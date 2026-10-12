/**
 * Client-side mirror of the backend scenario DTOs
 * (`game-server/.../scenario/ScenarioDtos.kt`). This is the canonical "scenario JSON" that
 * the Scenario Builder both produces and accepts via paste, and that the share codec encodes.
 */

export type ScenarioMode = 'SELF' | 'AI' | 'TWO_PLAYER'

export interface ScenarioBattlefieldCard {
  name: string
  tapped?: boolean
  summoningSickness?: boolean
  counters?: Record<string, number>
  /** Aura host card name (must also be on the same battlefield). */
  attachedTo?: string
  chosenCreatureType?: string
  chosenColor?: string
  /** Durable chosen card type, e.g. Arachne, Psionic Weaver (CR 205.2a). */
  chosenCardType?: string
}

export interface ScenarioPlayerConfig {
  lifeTotal?: number
  counters?: Record<string, number>
  hand?: string[]
  battlefield?: ScenarioBattlefieldCard[]
  graveyard?: string[]
  library?: string[]
  exile?: string[]
  commanders?: string[]
}

/** One seat of an N-player scenario (matches backend `ScenarioSeat`). */
export interface ScenarioSeatSpec {
  name?: string
  config?: ScenarioPlayerConfig
}

/** Full scenario request (matches backend `ScenarioRequest`). */
export interface ScenarioSpec {
  player1Name?: string
  player2Name?: string
  player1?: ScenarioPlayerConfig
  player2?: ScenarioPlayerConfig
  /**
   * N-player seats (3-4 player pods), in turn order. Overrides the legacy two-seat
   * fields when present. Pods of more than two seats start as SELF (hotseat).
   */
  players?: ScenarioSeatSpec[]
  phase?: string
  step?: string
  activePlayer?: number
  priorityPlayer?: number
  mode?: ScenarioMode
  aiPlayer?: number
  /**
   * Custom cards, each a Scryfall(-style) card object as pasted. The server compiles them with
   * Argentum Assay and registers them for this session only, so their names can be used in any
   * zone exactly like corpus cards. Requires dev endpoints to be enabled on the server.
   */
  customCards?: string[]
}

/** One printed line as Assay read it (mirrors the backend `AssayLineReading`). */
export interface AssayLineReading {
  index: number
  text: string
  /** The touchstone's own verdict: ROUND_TRIP | VARIANT | DECLINED | AMBIGUOUS | MISMATCH. */
  verdict: string
  /** The canonical spelling, when the line was written a different legal way. */
  printed?: string
  /** `assay explain`'s caret, pointing at the token the parse died on. */
  explanation?: string
}

export interface AssayDecline {
  kind: string
  detail: string
  lineIndex?: number
  line?: string
}

/** Response from `POST /api/dev/scenarios/assay` (mirrors the backend `AssayCompileResponse`). */
export interface AssayCompileResponse {
  cardName: string | null
  compiled: boolean
  lines: AssayLineReading[]
  declines: AssayDecline[]
  warnings: string[]
  definition?: unknown
}

export interface ScenarioPlayerInfo {
  name: string
  token: string
  playerId: string
}

/** Response from POST /api/scenarios (matches backend `ScenarioResponse`). */
export interface ScenarioCreateResponse {
  sessionId: string
  player1: ScenarioPlayerInfo
  player2: ScenarioPlayerInfo
  message: string
  mode?: ScenarioMode
  /** Full seat roster in turn order (present for pods of more than two seats). */
  players?: ScenarioPlayerInfo[]
}

/** The zones a card can be added to in the builder. */
export type ScenarioZone =
  | 'battlefield'
  | 'hand'
  | 'graveyard'
  | 'exile'
  | 'library'
  | 'commanders'

/** Display order — the board leads with the battlefield, then the zones around it. */
export const SCENARIO_ZONES: readonly ScenarioZone[] = [
  'battlefield',
  'hand',
  'graveyard',
  'exile',
  'library',
  'commanders',
]

export const ZONE_LABEL: Record<ScenarioZone, string> = {
  battlefield: 'Battlefield',
  hand: 'Hand',
  graveyard: 'Graveyard',
  exile: 'Exile',
  library: 'Library',
  commanders: 'Command',
}

/**
 * Zones rendered as a pile — duplicates collapse into one tile with a ×N badge, because a
 * 40-card library as 40 separate tiles is unreadable. The battlefield, hand and command zone
 * stay one tile per card (each is individually meaningful).
 */
export const PILE_ZONES: readonly ScenarioZone[] = ['graveyard', 'exile', 'library']
