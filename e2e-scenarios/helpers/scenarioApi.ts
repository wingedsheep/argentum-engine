// Override when the game server under test isn't on the default port — e.g. a worktree running its
// own server next to an already-running main checkout. The `E2E_BASE_URL` sibling in
// playwright.config.ts does the same for the web client.
const BASE_URL = `${process.env.E2E_SERVER_URL ?? 'http://localhost:8080'}/api/dev/scenarios`

export interface BattlefieldCardConfig {
  name: string
  tapped?: boolean
  summoningSickness?: boolean
  counters?: Record<string, number>
  attachedTo?: string
}

export interface PlayerConfig {
  lifeTotal?: number
  hand?: string[]
  battlefield?: BattlefieldCardConfig[]
  graveyard?: string[]
  library?: string[]
  exile?: string[]
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
  /** Steps where player 1 should stop on their own turn (prevents auto-pass) */
  player1StopAtSteps?: string[]
  /** Steps where player 2 should stop on their own turn (prevents auto-pass) */
  player2StopAtSteps?: string[]
  /** Steps where player 1 should stop on opponent's turn (prevents auto-pass) */
  player1OpponentStopAtSteps?: string[]
  /** Steps where player 2 should stop on opponent's turn (prevents auto-pass) */
  player2OpponentStopAtSteps?: string[]
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
