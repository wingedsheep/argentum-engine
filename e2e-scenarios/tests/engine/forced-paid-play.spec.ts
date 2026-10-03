import { test, expect } from '@playwright/test'
import fs from 'node:fs'
import path from 'node:path'
import { GamePage } from '../../helpers/gamePage'

// Saved from the engine's ForcedPlayTest with an unpaid Shock instruction and one red mana.
const fixture = fs.readFileSync(path.resolve(__dirname, '../../fixtures/states/forced-paid-play.json'), 'utf8')
const backend = process.env.E2E_SERVER_URL ?? 'http://localhost:8080'

test('mandatory paid play uses normal targeting and protects the private choice', async ({ browser, request }) => {
  const response = await request.post(`${backend}/api/scenarios/from-state?mode=TWO_PLAYER`, {
    data: fixture, headers: { 'Content-Type': 'application/json' },
  })
  expect(response.ok()).toBeTruthy()
  const session = await response.json()
  const actor = await browser.newContext()
  const observer = await browser.newContext()
  try {
    for (const [context, player] of [[actor, session.player1], [observer, session.player2]] as const) {
      await context.addInitScript(({ token, name }) => {
        localStorage.setItem('argentum-token', token)
        localStorage.setItem('argentum-player-name', name)
      }, player)
    }
    const page = await actor.newPage()
    const other = await observer.newPage()
    await page.goto('/')
    await other.goto('/')
    await expect(page.getByRole('heading', { name: 'Play Shock', exact: true })).toBeVisible()
    await expect(other.getByRole('heading', { name: 'Play Shock', exact: true })).toHaveCount(0)
    await expect(page.getByRole('button', { name: /Cancel|Decline/ })).toHaveCount(0)
    await page.screenshot({ path: process.env.FORCED_PLAY_SCREENSHOT ?? 'test-results/forced-paid-play.png', fullPage: true })
    await page.getByRole('button', { name: /Cast Shock/ }).click()
    await expect(page.getByRole('heading', { name: 'Play Shock', exact: true })).toHaveCount(0)
    const game = new GamePage(page)
    await game.selectPlayer(session.player2.playerId)
    await game.confirmTargets()
    await expect(page.locator('[data-card-id] img[alt="Shock"]')).toBeVisible()
    await expect(page.getByRole('heading', { name: 'Play Shock', exact: true })).toHaveCount(0)
    await expect(page.getByText('Answer the current casting decision', { exact: false })).toHaveCount(0)
    await expect(page.getByText('Play the instructed card', { exact: false })).toHaveCount(0)
  } finally {
    await actor.close()
    await observer.close()
  }
})
