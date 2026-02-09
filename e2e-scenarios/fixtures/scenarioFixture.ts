import { test as base, type BrowserContext, type Page } from '@playwright/test'
import { createScenario, type ScenarioRequest, type ScenarioResponse } from '../helpers/scenarioApi'
import { GamePage } from '../helpers/gamePage'

export interface ScenarioPlayer {
  page: Page
  gamePage: GamePage
  playerId: string
  token: string
}

export interface ScenarioResult {
  player1: ScenarioPlayer
  player2: ScenarioPlayer
  response: ScenarioResponse
}

interface ScenarioFixtures {
  createGame: (config: ScenarioRequest) => Promise<ScenarioResult>
}

export const test = base.extend<ScenarioFixtures>({
  createGame: async ({ browser }, use) => {
    const contexts: BrowserContext[] = []

    await use(async (config: ScenarioRequest) => {
      GamePage.resetStepCounter()
      const response = await createScenario(config)

      // Create isolated browser contexts for each player with tokens pre-injected
      const [ctx1, ctx2] = await Promise.all([
        browser.newContext(),
        browser.newContext(),
      ])
      contexts.push(ctx1, ctx2)

      // Inject localStorage tokens BEFORE any page scripts run
      await ctx1.addInitScript(
        ({ token, name }: { token: string; name: string }) => {
          localStorage.setItem('argentum-token', token)
          localStorage.setItem('argentum-player-name', name)
        },
        { token: response.player1.token, name: response.player1.name },
      )
      await ctx2.addInitScript(
        ({ token, name }: { token: string; name: string }) => {
          localStorage.setItem('argentum-token', token)
          localStorage.setItem('argentum-player-name', name)
        },
        { token: response.player2.token, name: response.player2.name },
      )

      // Navigate both players to the game
      const [page1, page2] = await Promise.all([ctx1.newPage(), ctx2.newPage()])
      await Promise.all([page1.goto('/'), page2.goto('/')])

      const gp1 = new GamePage(page1, config.player1Name ?? 'P1')
      const gp2 = new GamePage(page2, config.player2Name ?? 'P2')

      // Wait for both game boards to load
      await Promise.all([gp1.waitForGameReady(), gp2.waitForGameReady()])

      return {
        player1: {
          page: page1,
          gamePage: gp1,
          playerId: response.player1.playerId,
          token: response.player1.token,
        },
        player2: {
          page: page2,
          gamePage: gp2,
          playerId: response.player2.playerId,
          token: response.player2.token,
        },
        response,
      }
    })

    // Cleanup all browser contexts
    for (const ctx of contexts) {
      await ctx.close()
    }
  },
})

export { expect } from '@playwright/test'
