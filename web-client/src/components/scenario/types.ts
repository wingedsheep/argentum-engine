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
}

export interface ScenarioPlayerConfig {
  lifeTotal?: number
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
export type ScenarioZone = 'hand' | 'battlefield' | 'graveyard' | 'exile' | 'library'

export const SCENARIO_ZONES: readonly ScenarioZone[] = [
  'hand',
  'battlefield',
  'graveyard',
  'exile',
  'library',
]
