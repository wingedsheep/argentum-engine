const BASE_URL = 'http://localhost:8080/api/dev/scenarios'

export interface BattlefieldCardConfig {
  name: string
  tapped?: boolean
  summoningSickness?: boolean
}

export interface PlayerConfig {
  lifeTotal?: number
  hand?: string[]
  battlefield?: BattlefieldCardConfig[]
  graveyard?: string[]
  library?: string[]
}

export interface ScenarioRequest {
  player1Name?: string
  player2Name?: string
  player1?: PlayerConfig
  player2?: PlayerConfig
  phase?: 'BEGINNING' | 'PRECOMBAT_MAIN' | 'COMBAT' | 'POSTCOMBAT_MAIN' | 'ENDING'
  step?: string
  activePlayer?: number
  priorityPlayer?: number
}

export interface PlayerInfo {
  name: string
  token: string
  playerId: string
}

export interface ScenarioResponse {
  sessionId: string
  player1: PlayerInfo
  player2: PlayerInfo
  message: string
}

export async function createScenario(config: ScenarioRequest): Promise<ScenarioResponse> {
  const response = await fetch(BASE_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(config),
  })

  if (!response.ok) {
    const body = await response.text()
    throw new Error(`Failed to create scenario (${response.status}): ${body}`)
  }

  return response.json()
}
