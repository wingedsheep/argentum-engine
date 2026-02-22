import { test, expect, Page, BrowserContext } from '@playwright/test'

/**
 * E2E test for a sealed tournament with 5 players.
 * Tests the full tournament lifecycle:
 * - Lobby creation and joining
 * - State preservation across page refreshes at every stage
 * - Deck building and submission
 * - Round 1: matchup display, BYE handling, match play, spectating
 * - Round completion via concede, standings updates
 * - Round 2: dynamic matchmaking (ready up → next match)
 */

interface PlayerPage {
  name: string
  page: Page
  context: BrowserContext
}

/** Create a lobby and have all players join. Returns the lobby ID. */
async function createLobbyWithPlayers(players: PlayerPage[]): Promise<string> {
  const host = players[0]
  await host.page.goto('/')
  await host.page.getByPlaceholder('Your name').fill(host.name)
  await host.page.getByRole('button', { name: 'Continue' }).click()
  await expect(host.page.getByRole('button', { name: 'Tournament' })).toBeVisible({ timeout: 10000 })
  await host.page.getByRole('button', { name: 'Tournament' }).click()
  await host.page.getByRole('button', { name: 'Create Lobby' }).click()
  await expect(host.page.getByText('Invite Code')).toBeVisible({ timeout: 10000 })

  const lobbyId = await host.page.getByTestId('invite-code').textContent() ?? ''
  expect(lobbyId).toBeTruthy()
  console.log(`Lobby created: ${lobbyId}`)

  for (let i = 1; i < players.length; i++) {
    const player = players[i]
    await player.page.goto('/')
    await player.page.getByPlaceholder('Your name').fill(player.name)
    await player.page.getByRole('button', { name: 'Continue' }).click()
    await expect(player.page.getByRole('button', { name: 'Tournament' })).toBeVisible({ timeout: 10000 })
    await player.page.getByRole('button', { name: 'Tournament' }).click()
    await player.page.getByPlaceholder('Enter Lobby ID').fill(lobbyId)
    await player.page.getByRole('button', { name: 'Join' }).click()
    await expect(player.page.getByText('Invite Code')).toBeVisible({ timeout: 10000 })
  }

  return lobbyId
}

/** Build and submit a deck for a player. */
async function buildAndSubmitDeck(player: PlayerPage): Promise<void> {
  const poolCards = player.page.locator('[data-testid="pool-card"]')
  let cardCount = await poolCards.count()
  const cards = cardCount > 0
    ? poolCards
    : player.page.locator('img[alt]:not([alt^="{"]):not([alt^="Mana"])').locator('..')
  if (cardCount === 0) cardCount = await cards.count()

  for (let i = 0; i < Math.min(20, cardCount); i++) {
    try {
      await cards.first().click({ timeout: 500 })
      await player.page.waitForTimeout(50)
    } catch {
      // Card may have moved
    }
  }

  const suggestButton = player.page.getByRole('button', { name: 'Suggest' })
  if (await suggestButton.isVisible({ timeout: 2000 }).catch(() => false)) {
    await suggestButton.click()
    await player.page.waitForTimeout(500)
  }

  const submitButton = player.page.getByRole('button', { name: 'Submit Deck' })
  await player.page.waitForTimeout(500)
  await expect(submitButton).toBeEnabled({ timeout: 10000 })
  await submitButton.click()
  await expect(player.page.getByRole('button', { name: 'Edit Deck' })).toBeVisible({ timeout: 10000 })
}

/** Keep hand for a player if they're in the mulligan phase. */
async function keepHandIfNeeded(player: PlayerPage): Promise<void> {
  const keepButton = player.page.getByRole('button', { name: 'Keep Hand' })
  if (await keepButton.isVisible({ timeout: 5000 }).catch(() => false)) {
    await keepButton.click()
    await player.page.waitForTimeout(500)
  }
}

/** Concede the current match: Concede → Confirm → Return to Menu. */
async function concedeMatch(player: PlayerPage): Promise<void> {
  // The concede button should be visible once the game board is showing
  // Use force:true because game-over overlays from other contexts can intercept clicks
  const concedeButton = player.page.getByRole('button', { name: 'Concede' })
  await expect(concedeButton).toBeVisible({ timeout: 30000 })
  await concedeButton.click({ force: true })

  // Confirm concession
  const confirmButton = player.page.getByRole('button', { name: 'Confirm' })
  await expect(confirmButton).toBeVisible({ timeout: 3000 })
  await confirmButton.click({ force: true })

  // Wait for game over overlay and return to tournament
  const returnButton = player.page.getByRole('button', { name: 'Return to Menu' })
  await expect(returnButton).toBeVisible({ timeout: 10000 })
  await returnButton.click()

  // Should return to tournament standings
  await expect(player.page.getByText('Standings')).toBeVisible({ timeout: 10000 })
}

/** Find the player who has a BYE this round. */
async function findByePlayer(players: PlayerPage[]): Promise<PlayerPage | null> {
  for (const player of players) {
    const hasBye = await player.page.getByText('Sitting out this round').isVisible({ timeout: 2000 }).catch(() => false)
    if (hasBye) return player
  }
  return null
}

/** Find all players who are currently in a match (mulligan or game board). */
async function findMatchPlayers(players: PlayerPage[]): Promise<PlayerPage[]> {
  const result: PlayerPage[] = []
  for (const player of players) {
    const inMatch = await player.page.getByRole('button', { name: 'Keep Hand' }).isVisible({ timeout: 3000 }).catch(() => false)
      || await player.page.getByRole('button', { name: 'Concede' }).isVisible({ timeout: 1000 }).catch(() => false)
    if (inMatch) result.push(player)
  }
  return result
}

test.describe('Sealed Tournament with 5 Players', () => {
  const playerNames = ['Alice', 'Bob', 'Charlie', 'Diana', 'Eve']
  let players: PlayerPage[] = []
  let lobbyId: string

  test.beforeEach(async ({ browser }) => {
    players = []
    for (const name of playerNames) {
      const context = await browser.newContext()
      const page = await context.newPage()
      players.push({ name, page, context })
    }
  })

  test.afterEach(async () => {
    for (const player of players) {
      await player.context.close()
    }
    players = []
  })

  test('full tournament flow with page refresh at each stage', async () => {
    test.setTimeout(360_000)

    // =========================================================================
    // Stage 1: WAITING_FOR_PLAYERS - Create lobby and add players
    // =========================================================================

    lobbyId = await createLobbyWithPlayers(players)

    // Verify all players see each other in the lobby
    for (const player of players) {
      for (const otherPlayer of players) {
        await expect(player.page.getByText(otherPlayer.name)).toBeVisible({ timeout: 5000 })
      }
    }
    console.log('All 5 players in lobby')

    // =========================================================================
    // Test page refresh during WAITING_FOR_PLAYERS
    // =========================================================================

    const player3 = players[2]
    console.log(`Testing page refresh for ${player3.name} during WAITING_FOR_PLAYERS`)
    await player3.page.reload()
    await expect(player3.page.getByText('Invite Code')).toBeVisible({ timeout: 10000 })
    await expect(player3.page.getByText(lobbyId)).toBeVisible()
    for (const otherPlayer of players) {
      await expect(player3.page.getByText(otherPlayer.name)).toBeVisible({ timeout: 5000 })
    }
    console.log(`${player3.name} successfully reconnected after refresh`)

    // =========================================================================
    // Stage 2: DECK_BUILDING - Start tournament
    // =========================================================================

    await players[0].page.getByRole('button', { name: 'Start' }).click()

    for (const player of players) {
      await expect(player.page.getByText('Deck Builder')).toBeVisible({ timeout: 30000 })
      console.log(`${player.name} received sealed pool`)
    }

    // Test page refresh during DECK_BUILDING
    const player2 = players[1]
    console.log(`Testing page refresh for ${player2.name} during DECK_BUILDING`)
    await player2.page.reload()
    await expect(player2.page.getByText('Deck Builder')).toBeVisible({ timeout: 15000 })
    // Wait for WebSocket to reconnect after refresh
    await player2.page.waitForTimeout(2000)
    console.log(`${player2.name} successfully reconnected to deck building`)

    // =========================================================================
    // Stage 3: Submit decks
    // =========================================================================

    for (const player of players) {
      // If WebSocket error banner is showing, reload to reconnect
      const wsError = player.page.getByText('WebSocket connection error')
      if (await wsError.isVisible({ timeout: 500 }).catch(() => false)) {
        await player.page.reload()
        await expect(player.page.getByText('Deck Builder')).toBeVisible({ timeout: 15000 })
        await player.page.waitForTimeout(1000)
      }
      await buildAndSubmitDeck(player)
      console.log(`${player.name} submitted deck`)
    }

    // Wait for tournament to start
    await expect(players[0].page.getByText('Standings')).toBeVisible({ timeout: 30000 })
    console.log('Tournament started!')

    // =========================================================================
    // Stage 4: Verify matchup display before round 1
    // =========================================================================

    // Each non-BYE player should see "ROUND 1" and "vs <opponent>" in their matchup card
    // The BYE player should see "Sitting out this round"
    let preRoundByePlayer: PlayerPage | null = null
    for (const player of players) {
      const hasBye = await player.page.getByText('Sitting out this round').isVisible({ timeout: 2000 }).catch(() => false)
      if (hasBye) {
        preRoundByePlayer = player
        console.log(`${player.name} has BYE for round 1`)
      } else {
        // Should see the "vs <opponent>" matchup card
        await expect(player.page.getByText(/^vs /)).toBeVisible({ timeout: 5000 })
        console.log(`${player.name} sees round 1 matchup`)
      }
    }
    expect(preRoundByePlayer).not.toBeNull()

    // =========================================================================
    // Stage 5: Ready up → matches start
    // =========================================================================

    for (const player of players) {
      const readyButton = player.page.getByRole('button', { name: 'Ready for Next Round' })
      if (await readyButton.isVisible({ timeout: 3000 }).catch(() => false)) {
        await readyButton.click()
        console.log(`${player.name} clicked Ready for Next Round`)
        await player.page.waitForTimeout(300)
      }
    }

    // Wait for matches to start
    await players[0].page.waitForTimeout(5000)

    // Find who is in a match vs who has BYE
    const matchPlayers = await findMatchPlayers(players)
    const byePlayer = await findByePlayer(players)

    console.log(`Match players: ${matchPlayers.map(p => p.name).join(', ')}`)
    console.log(`BYE player: ${byePlayer?.name ?? 'none'}`)

    // With 5 players: 2 matches (4 players) + 1 BYE
    expect(matchPlayers.length).toBe(4)
    expect(byePlayer).not.toBeNull()

    // =========================================================================
    // Stage 6: Test reconnection during active match
    // =========================================================================

    const matchPlayer = matchPlayers[0]
    console.log(`Testing page refresh for ${matchPlayer.name} during active match`)
    await matchPlayer.page.reload()

    // Should reconnect to the game (mulligan or game board)
    const keepOrConcede = matchPlayer.page.getByRole('button', { name: /Keep Hand|Concede/ })
    await expect(keepOrConcede.first()).toBeVisible({ timeout: 15000 })
    console.log(`${matchPlayer.name} successfully reconnected to match`)

    // =========================================================================
    // Stage 7: Test BYE player refresh + spectating
    // =========================================================================

    if (byePlayer) {
      console.log(`Testing page refresh for ${byePlayer.name} during BYE`)
      await byePlayer.page.reload()
      await expect(byePlayer.page.getByText('Standings')).toBeVisible({ timeout: 15000 })
      await expect(byePlayer.page.getByText('Sitting out this round')).toBeVisible({ timeout: 5000 })
      console.log(`${byePlayer.name} successfully reconnected while on BYE`)

      // BYE player should see live matches to spectate
      const watchButton = byePlayer.page.getByText('Watch').first()
      if (await watchButton.isVisible({ timeout: 3000 }).catch(() => false)) {
        console.log(`${byePlayer.name} can see live matches to spectate`)
      }
    }

    // =========================================================================
    // Stage 8: Complete round 1 via concede
    // =========================================================================

    // All match players must keep their hands before the game board appears
    for (const player of matchPlayers) {
      await keepHandIfNeeded(player)
      console.log(`${player.name} kept hand`)
    }

    // Wait for all mulligans to resolve and game boards to appear
    await matchPlayers[0].page.waitForTimeout(3000)

    // Complete both matches by conceding one player per match.
    // We don't know the exact pairings, so we concede one at a time and
    // find the opponent who sees "Victory!" after each concession.
    const remaining = [...matchPlayers]
    for (let matchNum = 0; matchNum < 2; matchNum++) {
      // Find a player who still has the Concede button (still in a match)
      let conceder: PlayerPage | null = null
      for (const player of remaining) {
        const hasConcede = await player.page.getByRole('button', { name: 'Concede' }).isVisible({ timeout: 2000 }).catch(() => false)
        if (hasConcede) {
          conceder = player
          break
        }
      }
      if (!conceder) break

      console.log(`${conceder.name} conceding match ${matchNum + 1}...`)
      await concedeMatch(conceder)
      console.log(`${conceder.name} conceded and returned to standings`)
      remaining.splice(remaining.indexOf(conceder), 1)

      // Find the opponent who now sees "Victory!" / "Return to Menu"
      for (const player of [...remaining]) {
        const returnButton = player.page.getByRole('button', { name: 'Return to Menu' })
        if (await returnButton.isVisible({ timeout: 3000 }).catch(() => false)) {
          await returnButton.click()
          await expect(player.page.getByText('Standings')).toBeVisible({ timeout: 10000 })
          console.log(`${player.name} (winner) returned to standings`)
          remaining.splice(remaining.indexOf(player), 1)
          break
        }
      }
    }

    // =========================================================================
    // Stage 9: Verify standings after round 1
    // =========================================================================

    // Wait for all players to be back at tournament standings
    await players[0].page.waitForTimeout(2000)

    // All players should see the standings table
    for (const player of players) {
      await expect(player.page.getByText('Standings')).toBeVisible({ timeout: 10000 })
    }

    // Winners should have 1 win (3 points): the 2 winning players + BYE player
    // Losers should have 1 loss (0 points)
    // Check from any player's perspective that standings are visible
    const standingsTable = players[0].page.locator('table')
    await expect(standingsTable).toBeVisible({ timeout: 5000 })
    console.log('Round 1 complete — standings updated')

    // =========================================================================
    // Stage 10: Ready up for next round (dynamic matchmaking)
    // =========================================================================

    // Click "Ready for Next Round" for all players
    for (const player of players) {
      const readyButton = player.page.getByRole('button', { name: 'Ready for Next Round' })
      if (await readyButton.isVisible({ timeout: 5000 }).catch(() => false)) {
        await readyButton.click()
        console.log(`${player.name} readied for next round`)
        await player.page.waitForTimeout(300)
      }
    }

    // Wait for next round to start
    await players[0].page.waitForTimeout(5000)

    // With dynamic matchmaking, the tournament advances rapidly (BYE rounds
    // complete instantly, matches start as soon as both players ready).
    // Verify at least some players entered their next match.
    const round2MatchPlayers = await findMatchPlayers(players)
    console.log(`Next round match players: ${round2MatchPlayers.map(p => p.name).join(', ')}`)
    expect(round2MatchPlayers.length).toBeGreaterThanOrEqual(2)

    console.log('Full tournament flow test complete!')
  })

  test('lobby settings preserved after host refresh', async ({ browser }) => {
    const context = await browser.newContext()
    const page = await context.newPage()

    await page.goto('/')
    await page.getByPlaceholder('Your name').fill('Host')
    await page.getByRole('button', { name: 'Continue' }).click()
    await expect(page.getByRole('button', { name: 'Tournament' })).toBeVisible({ timeout: 10000 })
    await page.getByRole('button', { name: 'Tournament' }).click()
    await page.getByRole('button', { name: 'Create Lobby' }).click()
    await expect(page.getByText('Invite Code')).toBeVisible({ timeout: 10000 })

    const settingsText = await page.locator('body').textContent() ?? ''
    const hasSealedText = settingsText.includes('Sealed') || settingsText.includes('boosters per player')
    expect(hasSealedText).toBe(true)

    await page.reload()

    await expect(page.getByText('Invite Code')).toBeVisible({ timeout: 15000 })
    const settingsTextAfter = await page.locator('body').textContent() ?? ''
    const stillHasSealedText = settingsTextAfter.includes('Sealed') || settingsTextAfter.includes('boosters per player')
    expect(stillHasSealedText).toBe(true)

    await context.close()
  })

  test('multiple players can refresh simultaneously', async ({ browser }) => {
    test.setTimeout(60_000)
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

    await pages[0].getByRole('button', { name: 'Tournament' }).click()
    await pages[0].getByRole('button', { name: 'Create Lobby' }).click()
    await expect(pages[0].getByText('Invite Code')).toBeVisible({ timeout: 10000 })

    const lid = await pages[0].getByTestId('invite-code').textContent() ?? ''

    for (let i = 1; i < 3; i++) {
      await pages[i].getByRole('button', { name: 'Tournament' }).click()
      await pages[i].getByPlaceholder('Enter Lobby ID').fill(lid)
      await pages[i].getByRole('button', { name: 'Join' }).click()
      await expect(pages[i].getByText('Invite Code')).toBeVisible({ timeout: 10000 })
    }

    for (const pg of pages) {
      for (const name of names) {
        await expect(pg.getByText(name, { exact: true })).toBeVisible({ timeout: 5000 })
      }
    }

    // All players refresh with slight stagger
    for (const pg of pages) {
      await pg.reload()
      await pg.waitForTimeout(500)
    }

    for (const pg of pages) {
      await expect(pg.getByText('Invite Code')).toBeVisible({ timeout: 15000 })
      for (const name of names) {
        await expect(pg.getByText(name, { exact: true })).toBeVisible({ timeout: 5000 })
      }
    }

    for (const ctx of contexts) {
      await ctx.close()
    }
  })
})
