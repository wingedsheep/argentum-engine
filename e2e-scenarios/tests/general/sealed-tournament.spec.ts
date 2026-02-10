import { test, expect, Page, BrowserContext } from '@playwright/test'

/**
 * E2E test for a sealed tournament with 5 players.
 * Tests state preservation across page refreshes at every stage:
 * - WAITING_FOR_PLAYERS
 * - DECK_BUILDING
 * - TOURNAMENT_ACTIVE
 */

interface PlayerPage {
  name: string
  page: Page
  context: BrowserContext
}

test.describe('Sealed Tournament with 5 Players', () => {
  const playerNames = ['Alice', 'Bob', 'Charlie', 'Diana', 'Eve']
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

  test('full tournament flow with page refresh at each stage', async () => {
    // =========================================================================
    // Stage 1: WAITING_FOR_PLAYERS - Create lobby and add players
    // =========================================================================

    // Host (Alice) creates the lobby
    const host = players[0]
    await host.page.goto('/')

    // Enter name
    await host.page.getByPlaceholder('Your name').fill(host.name)
    await host.page.getByRole('button', { name: 'Continue' }).click()

    // Wait for connection
    await expect(host.page.getByRole('button', { name: 'Tournament' })).toBeVisible({ timeout: 10000 })

    // Select Tournament mode
    await host.page.getByRole('button', { name: 'Tournament' }).click()

    // Create lobby
    await host.page.getByRole('button', { name: 'Create Lobby' }).click()

    // Wait for lobby to be created and get the lobby ID
    await expect(host.page.getByText('Invite Code')).toBeVisible({ timeout: 10000 })
    lobbyId = await host.page.locator('text=Invite Code').locator('..').locator('div[style*="monospace"]').textContent() ?? ''
    expect(lobbyId).toBeTruthy()
    console.log(`Lobby created: ${lobbyId}`)

    // Other players join
    for (let i = 1; i < 5; i++) {
      const player = players[i]
      await player.page.goto('/')

      // Enter name
      await player.page.getByPlaceholder('Your name').fill(player.name)
      await player.page.getByRole('button', { name: 'Continue' }).click()

      // Wait for connection
      await expect(player.page.getByRole('button', { name: 'Tournament' })).toBeVisible({ timeout: 10000 })

      // Select Tournament mode
      await player.page.getByRole('button', { name: 'Tournament' }).click()

      // Enter lobby ID and join
      await player.page.getByPlaceholder('Enter Lobby ID').fill(lobbyId)
      await player.page.getByRole('button', { name: 'Join' }).click()

      // Wait for lobby view
      await expect(player.page.getByText('Invite Code')).toBeVisible({ timeout: 10000 })
    }

    // Verify all players see 5 players in the lobby
    for (const player of players) {
      for (const otherPlayer of players) {
        await expect(player.page.getByText(otherPlayer.name)).toBeVisible({ timeout: 5000 })
      }
    }
    console.log('All 5 players in lobby')

    // =========================================================================
    // Test page refresh during WAITING_FOR_PLAYERS (player 3 - Charlie)
    // =========================================================================

    const player3 = players[2]
    console.log(`Testing page refresh for ${player3.name} during WAITING_FOR_PLAYERS`)

    await player3.page.reload()

    // Should automatically reconnect to the lobby
    await expect(player3.page.getByText('Invite Code')).toBeVisible({ timeout: 10000 })
    await expect(player3.page.getByText(lobbyId)).toBeVisible()

    // Verify still sees all players
    for (const otherPlayer of players) {
      await expect(player3.page.getByText(otherPlayer.name)).toBeVisible({ timeout: 5000 })
    }
    console.log(`${player3.name} successfully reconnected after refresh`)

    // =========================================================================
    // Stage 2: DECK_BUILDING - Start tournament
    // =========================================================================

    // Host starts the tournament
    await host.page.getByRole('button', { name: 'Start' }).click()

    // All players should see the deck builder
    for (const player of players) {
      // Wait for sealed pool to load - look for the deck builder UI
      await expect(player.page.getByText('Deck Builder')).toBeVisible({ timeout: 30000 })
      console.log(`${player.name} received sealed pool`)
    }

    // =========================================================================
    // Test page refresh during DECK_BUILDING (player 2 - Bob)
    // =========================================================================

    const player2 = players[1]
    console.log(`Testing page refresh for ${player2.name} during DECK_BUILDING`)

    await player2.page.reload()

    // Should automatically reconnect and see deck builder
    await expect(player2.page.getByText('Deck Builder')).toBeVisible({ timeout: 15000 })
    console.log(`${player2.name} successfully reconnected to deck building`)

    // =========================================================================
    // Stage 3: Submit decks
    // =========================================================================

    // Each player submits their deck
    for (const player of players) {
      // Cards in the pool are divs with img children - click the parent div
      // Each PoolCard div has cursor:pointer and contains an img with alt text (card name)
      const poolCards = player.page.locator('img[alt]').locator('..')

      // Click on cards to add them to deck (need ~20 spells + lands for 40 card deck)
      const cardCount = await poolCards.count()
      console.log(`${player.name}: Found ${cardCount} cards in pool`)

      for (let i = 0; i < Math.min(20, cardCount); i++) {
        const card = poolCards.nth(i)
        try {
          await card.click({ timeout: 500 })
          await player.page.waitForTimeout(50)
        } catch {
          // Card may have moved or been added already
        }
      }

      // Click "Suggest" button to auto-add appropriate lands
      const suggestButton = player.page.getByRole('button', { name: 'Suggest' })
      if (await suggestButton.isVisible({ timeout: 2000 }).catch(() => false)) {
        await suggestButton.click()
        console.log(`${player.name}: Clicked Suggest for lands`)
        await player.page.waitForTimeout(500)
      }

      // Submit the deck
      const submitButton = player.page.getByRole('button', { name: 'Submit Deck' })
      await player.page.waitForTimeout(500)

      // Check current deck size from UI
      const deckInfo = await player.page.locator('text=/\\d+\\s*\\/\\s*40/').textContent().catch(() => '0/40')
      console.log(`${player.name}: Deck status: ${deckInfo}`)

      // Wait for submit button to be enabled and click it
      await expect(submitButton).toBeEnabled({ timeout: 10000 })
      await submitButton.click()

      // Wait for submission confirmation
      // For most players: Edit Deck button appears
      // For the last player: Tournament starts immediately (Tournament Standings visible)
      await expect(
        player.page.getByRole('button', { name: 'Edit Deck' })
          .or(player.page.getByText('Tournament Standings'))
      ).toBeVisible({ timeout: 10000 })
      console.log(`${player.name} submitted deck successfully`)
    }

    // Wait for tournament to start
    // Look for tournament UI elements like match info, standings, etc.
    await expect(host.page.getByRole('heading', { name: 'Tournament Standings' })).toBeVisible({ timeout: 30000 })
    console.log('Tournament started!')

    // =========================================================================
    // Stage 4: TOURNAMENT_ACTIVE - Test reconnection during tournament
    // =========================================================================

    // All players need to click "Ready for Next Round" to start matches
    for (const player of players) {
      const readyButton = player.page.getByRole('button', { name: 'Ready for Next Round' })
      if (await readyButton.isVisible({ timeout: 2000 }).catch(() => false)) {
        await readyButton.click()
        console.log(`${player.name} clicked Ready for Next Round`)
        await player.page.waitForTimeout(200)
      }
    }

    // Wait for matches to start (mulligan phase)
    await host.page.waitForTimeout(3000)

    // Find a player who is in a match (not on bye)
    let matchPlayer: PlayerPage | null = null
    for (const player of players) {
      // Check if this player sees a game board (they're in a match)
      const isInMatch = await player.page.getByText(/Mulligan|Keep Hand|Your Turn|Opponent/i).isVisible({ timeout: 1000 }).catch(() => false)
      if (isInMatch) {
        matchPlayer = player
        break
      }
    }

    if (matchPlayer) {
      console.log(`Testing page refresh for ${matchPlayer.name} during TOURNAMENT_ACTIVE (in match)`)

      await matchPlayer.page.reload()

      // Should reconnect to the game
      await expect(matchPlayer.page.getByText(/Mulligan|Keep Hand|Your Turn|Opponent|Forest|Mountain|Island|Swamp|Plains/i)).toBeVisible({ timeout: 15000 })
      console.log(`${matchPlayer.name} successfully reconnected to match`)
    }

    // =========================================================================
    // Test refresh for player on bye
    // =========================================================================

    // Find player on bye (5 players means 1 has bye in first round)
    let byePlayer: PlayerPage | null = null
    for (const player of players) {
      const isOnBye = await player.page.getByText(/bye/i).isVisible({ timeout: 1000 }).catch(() => false)
      if (isOnBye) {
        byePlayer = player
        break
      }
    }

    if (byePlayer) {
      console.log(`Testing page refresh for ${byePlayer.name} during TOURNAMENT_ACTIVE (on bye)`)

      await byePlayer.page.reload()

      // Should reconnect to tournament view
      await expect(byePlayer.page.getByRole('heading', { name: 'Tournament Standings' })).toBeVisible({ timeout: 15000 })
      console.log(`${byePlayer.name} successfully reconnected while on bye`)
    }

    console.log('All reconnection tests passed!')
  })

  test('lobby settings preserved after host refresh', async ({ browser }) => {
    // Create just one player for this test
    const context = await browser.newContext()
    const page = await context.newPage()

    await page.goto('/')

    // Enter name
    await page.getByPlaceholder('Your name').fill('Host')
    await page.getByRole('button', { name: 'Continue' }).click()

    // Wait for connection
    await expect(page.getByRole('button', { name: 'Tournament' })).toBeVisible({ timeout: 10000 })

    // Select Tournament mode and create lobby
    await page.getByRole('button', { name: 'Tournament' }).click()
    await page.getByRole('button', { name: 'Create Lobby' }).click()

    // Wait for lobby
    await expect(page.getByText('Invite Code')).toBeVisible({ timeout: 10000 })

    // Get initial settings display
    const settingsText = await page.locator('body').textContent() ?? ''
    const hasSealedText = settingsText.includes('Sealed') || settingsText.includes('boosters per player')

    expect(hasSealedText).toBe(true)

    // Refresh the page
    await page.reload()

    // Should return to lobby with same settings
    await expect(page.getByText('Invite Code')).toBeVisible({ timeout: 15000 })

    // Verify it's still the same format
    const settingsTextAfter = await page.locator('body').textContent() ?? ''
    const stillHasSealedText = settingsTextAfter.includes('Sealed') || settingsTextAfter.includes('boosters per player')
    expect(stillHasSealedText).toBe(true)

    await context.close()
  })

  test('multiple players can refresh simultaneously', async ({ browser }) => {
    // Create 3 players
    const contexts: BrowserContext[] = []
    const pages: Page[] = []
    const names = ['P1', 'P2', 'P3']

    for (const name of names) {
      const ctx = await browser.newContext()
      const pg = await ctx.newPage()
      contexts.push(ctx)
      pages.push(pg)

      await pg.goto('/')
      await pg.getByPlaceholder('Your name').fill(name)
      await pg.getByRole('button', { name: 'Continue' }).click()
      await expect(pg.getByRole('button', { name: 'Tournament' })).toBeVisible({ timeout: 10000 })
    }

    // First player creates lobby
    await pages[0].getByRole('button', { name: 'Tournament' }).click()
    await pages[0].getByRole('button', { name: 'Create Lobby' }).click()
    await expect(pages[0].getByText('Invite Code')).toBeVisible({ timeout: 10000 })

    const lid = await pages[0].locator('text=Invite Code').locator('..').locator('div[style*="monospace"]').textContent() ?? ''

    // Other players join
    for (let i = 1; i < 3; i++) {
      await pages[i].getByRole('button', { name: 'Tournament' }).click()
      await pages[i].getByPlaceholder('Enter Lobby ID').fill(lid)
      await pages[i].getByRole('button', { name: 'Join' }).click()
      await expect(pages[i].getByText('Invite Code')).toBeVisible({ timeout: 10000 })
    }

    // Verify all in lobby
    for (const pg of pages) {
      for (const name of names) {
        await expect(pg.getByText(name, { exact: true })).toBeVisible({ timeout: 5000 })
      }
    }

    // All players refresh simultaneously
    await Promise.all(pages.map(pg => pg.reload()))

    // All should reconnect
    for (const pg of pages) {
      await expect(pg.getByText('Invite Code')).toBeVisible({ timeout: 15000 })
      for (const name of names) {
        await expect(pg.getByText(name, { exact: true })).toBeVisible({ timeout: 5000 })
      }
    }

    // Cleanup
    for (const ctx of contexts) {
      await ctx.close()
    }
  })
})
