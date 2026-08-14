import { test, expect, Page, BrowserContext } from '@playwright/test'
import { createLobby, enterName, joinLobby, GROUP_DRAFT } from '../../helpers/homeScreen'

/**
 * E2E test for a draft tournament with 4 players.
 * Tests state preservation across page refreshes during drafting phase.
 */

interface PlayerPage {
  name: string
  page: Page
  context: BrowserContext
}

test.describe('Draft Tournament with 4 Players', () => {
  const playerNames = ['Alice', 'Bob', 'Charlie', 'Diana']
  let players: PlayerPage[] = []
  let lobbyId: string

  test.beforeEach(async ({ browser }) => {
    // Create separate browser contexts for each player (isolated storage)
    players = []
    for (const name of playerNames) {
      const context = await browser.newContext()
      const page = await context.newPage()
      players.push({ name, page, context })
    }
  })

  test.afterEach(async () => {
    // Close all contexts
    for (const player of players) {
      await player.context.close()
    }
    players = []
  })

  test('draft tournament flow with page refresh during drafting', async () => {
    test.setTimeout(300_000)
    // =========================================================================
    // Stage 1: Create Draft Lobby
    // =========================================================================

    const host = players[0]
    await enterName(host.page, host.name)

    // Booster draft, straight from the wizard. This used to open a sealed lobby and then switch the
    // format inside it, because the home screen's only limited entry point was labelled
    // "Draft & Sealed"; the wizard asks for the draft shape directly.
    lobbyId = await createLobby(host.page, GROUP_DRAFT)
    console.log(`Lobby created: ${lobbyId}`)

    // Other players join
    for (let i = 1; i < 4; i++) {
      const player = players[i]
      await enterName(player.page, player.name)
      await joinLobby(player.page, lobbyId)
    }

    // Verify all players see 4 players in the lobby
    for (const player of players) {
      for (const otherPlayer of players) {
        await expect(player.page.getByText(otherPlayer.name, { exact: true })).toBeVisible({ timeout: 5000 })
      }
    }
    console.log('All 4 players in draft lobby')

    // =========================================================================
    // Stage 2: Start Draft
    // =========================================================================

    // Host starts the draft
    await host.page.getByRole('button', { name: 'Start' }).click()

    // All players should see the draft interface
    for (const player of players) {
      // Wait for draft UI - look for "Draft -" header and pack indicator
      await expect(player.page.getByText(/Draft -/)).toBeVisible({ timeout: 30000 })
      console.log(`${player.name} entered draft phase`)
    }

    // =========================================================================
    // Stage 3: Test page refresh during DRAFTING (Bob - player 2)
    // =========================================================================

    const player2 = players[1]
    console.log(`Testing page refresh for ${player2.name} during DRAFTING`)

    // First, make sure Bob has a pack (wait for cards to appear)
    await expect(player2.page.locator('img[alt]').first()).toBeVisible({ timeout: 10000 })

    await player2.page.reload()

    // Should automatically reconnect to draft with current pack restored
    await expect(player2.page.getByText(/Draft -/)).toBeVisible({ timeout: 15000 })
    // Should still see cards in the pack
    await expect(player2.page.locator('img[alt]').first()).toBeVisible({ timeout: 10000 })
    console.log(`${player2.name} successfully reconnected to draft with pack restored`)

    // =========================================================================
    // Stage 4: Complete the draft (3 packs x 15 picks each = 45 picks total)
    // =========================================================================

    // We'll do a simplified draft - just pick the first available card each time
    // Draft has 3 packs of 15 cards each
    const totalPicks = 3 * 15

    for (let pick = 0; pick < totalPicks; pick++) {
      const packNum = Math.floor(pick / 15) + 1
      const pickNum = (pick % 15) + 1

      if (pick % 15 === 0) {
        console.log(`Starting Pack ${packNum}`)
      }

      // Each player makes a pick
      for (const player of players) {
        // Wait for pack to arrive (cards visible)
        const packCards = player.page.locator('img[alt]')

        try {
          // Wait for at least one card in the pack
          await expect(packCards.first()).toBeVisible({ timeout: 10000 })

          // Click the first card to select it
          await packCards.first().click()

          // Click the confirm pick button (it shows "Pick [cardname]" when card selected)
          const pickButton = player.page.locator('button:has-text("Pick ")').first()
          await expect(pickButton).toBeEnabled({ timeout: 5000 })
          await pickButton.click()

          // Small delay to allow state to update
          await player.page.waitForTimeout(100)
        } catch {
          // Player might be waiting for pack to pass
          await player.page.waitForTimeout(200)
        }
      }

      // Test refresh mid-draft for Charlie on pick 8 of pack 1
      if (pick === 7) {
        const player3 = players[2]
        console.log(`Testing page refresh for ${player3.name} mid-draft (pick ${pick + 1})`)
        await player3.page.reload()
        await expect(player3.page.getByText(/Draft -/)).toBeVisible({ timeout: 15000 })
        console.log(`${player3.name} successfully reconnected mid-draft`)
      }
    }

    console.log('Draft complete!')

    // =========================================================================
    // Stage 5: Verify transition to Deck Building
    // =========================================================================

    // All players should see the deck builder after draft completes
    for (const player of players) {
      await expect(player.page.getByText('Deck Builder')).toBeVisible({ timeout: 30000 })
      console.log(`${player.name} entered deck building after draft`)
    }

    // =========================================================================
    // Stage 6: Test refresh during DECK_BUILDING after draft
    // =========================================================================

    const player4 = players[3]
    console.log(`Testing page refresh for ${player4.name} during DECK_BUILDING (post-draft)`)

    await player4.page.reload()

    await expect(player4.page.getByText('Deck Builder')).toBeVisible({ timeout: 15000 })
    console.log(`${player4.name} successfully reconnected to deck building`)

    console.log('All draft reconnection tests passed!')
  })

  test('draft picks are preserved after refresh', async ({ browser }) => {
    test.setTimeout(120_000)
    // Create 2 players for a quick draft pick preservation test
    const contexts: BrowserContext[] = []
    const pages: Page[] = []
    const names = ['Host', 'Guest']

    for (const name of names) {
      const ctx = await browser.newContext()
      const pg = await ctx.newPage()
      contexts.push(ctx)
      pages.push(pg)

      await enterName(pg, name)
    }

    // Host creates draft lobby
    const lid = await createLobby(pages[0], GROUP_DRAFT)

    // Guest joins
    await joinLobby(pages[1], lid)

    // Verify both in lobby (look for player names in the player list)
    for (const pg of pages) {
      for (const name of names) {
        // Use first() since name might appear multiple times
        await expect(pg.getByText(name, { exact: true }).first()).toBeVisible({ timeout: 5000 })
      }
    }

    // Start draft
    await pages[0].getByRole('button', { name: 'Start' }).click()

    // Both should see draft UI
    for (const pg of pages) {
      await expect(pg.getByText(/Draft -/)).toBeVisible({ timeout: 30000 })
    }

    // Host makes a pick
    const hostPage = pages[0]
    await expect(hostPage.locator('img[alt]').first()).toBeVisible({ timeout: 10000 })

    // Get initial picked count (should be "0 / 45")
    const initialPickedText = await hostPage.locator('text=/\\d+ \\/ 45/').textContent() ?? ''
    expect(initialPickedText).toContain('0 / 45')

    // Click first card to select
    await hostPage.locator('img[alt]').first().click()

    // Click pick button
    const pickButton = hostPage.locator('button:has-text("Pick ")').first()
    await expect(pickButton).toBeEnabled({ timeout: 5000 })
    await pickButton.click()

    // Wait for pick to register
    await hostPage.waitForTimeout(500)

    // Verify picked count increased (should be "1 / 45")
    await expect(hostPage.locator('text=/1 \\/ 45/')).toBeVisible({ timeout: 5000 })

    // Now refresh
    await hostPage.reload()

    // Should reconnect to draft
    await expect(hostPage.getByText(/Draft -/)).toBeVisible({ timeout: 15000 })

    // Picked count should still show 1 / 45 (pick was preserved)
    await expect(hostPage.locator('text=/1 \\/ 45/')).toBeVisible({ timeout: 10000 })
    console.log('Pick count preserved after refresh!')

    // Cleanup
    for (const ctx of contexts) {
      await ctx.close()
    }
  })
})
