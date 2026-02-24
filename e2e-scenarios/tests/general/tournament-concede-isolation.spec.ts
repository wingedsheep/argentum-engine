import { test, expect, Page, BrowserContext } from '@playwright/test'

/**
 * E2E test: conceding a tournament match must not affect other active matches.
 *
 * Regression test for a bug where conceding in one match caused ALL players in
 * the tournament to be kicked out of their games. The fix ensures that
 * tournament messages (ActiveMatches, MatchComplete, RoundComplete) do not
 * clear game state for players who are still in an active game.
 */

interface PlayerPage {
  name: string
  page: Page
  context: BrowserContext
}

async function createLobbyWithPlayers(players: PlayerPage[]): Promise<string> {
  const host = players[0]!
  await host.page.goto('/')
  await host.page.getByPlaceholder('Your name').fill(host.name)
  await host.page.getByRole('button', { name: 'Continue' }).click()
  await expect(host.page.getByRole('button', { name: 'Tournament' })).toBeVisible({ timeout: 10000 })
  await host.page.getByRole('button', { name: 'Tournament' }).click()
  await host.page.getByRole('button', { name: 'Create Lobby' }).click()
  await expect(host.page.getByText('Invite Code')).toBeVisible({ timeout: 10000 })

  const lobbyId = await host.page.getByTestId('invite-code').textContent() ?? ''
  expect(lobbyId).toBeTruthy()

  for (let i = 1; i < players.length; i++) {
    const player = players[i]!
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

async function keepHandIfNeeded(player: PlayerPage): Promise<void> {
  const keepButton = player.page.getByRole('button', { name: 'Keep Hand' })
  if (await keepButton.isVisible({ timeout: 5000 }).catch(() => false)) {
    await keepButton.click()
    await player.page.waitForTimeout(500)
  }
}

/** Concede the current match: Concede → Confirm → Return to Menu. */
async function concedeMatch(player: PlayerPage): Promise<void> {
  const concedeButton = player.page.getByRole('button', { name: 'Concede' })
  await expect(concedeButton).toBeVisible({ timeout: 30000 })
  await concedeButton.click({ force: true })

  const confirmButton = player.page.getByRole('button', { name: 'Confirm' })
  await expect(confirmButton).toBeVisible({ timeout: 3000 })
  await confirmButton.click({ force: true })

  const returnButton = player.page.getByRole('button', { name: 'Return to Menu' })
  await expect(returnButton).toBeVisible({ timeout: 10000 })
  await returnButton.click()

  await expect(player.page.getByText('Standings')).toBeVisible({ timeout: 10000 })
}

/** Check if a player is currently in a game (seeing mulligan or game board). */
async function isInMatch(player: PlayerPage): Promise<boolean> {
  const inMatch = await player.page.getByRole('button', { name: 'Keep Hand' }).isVisible({ timeout: 3000 }).catch(() => false)
    || await player.page.getByRole('button', { name: 'Concede' }).isVisible({ timeout: 1000 }).catch(() => false)
  return inMatch
}

test.describe('Tournament concede isolation', () => {
  const playerNames = ['Alice', 'Bob', 'Charlie', 'Diana', 'Eve']
  let players: PlayerPage[] = []

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

  test('conceding one match does not kick players out of other matches', async () => {
    test.setTimeout(300_000)

    // === Setup: create lobby, build decks, start tournament ===

    await createLobbyWithPlayers(players)

    // Start tournament
    await players[0]!.page.getByRole('button', { name: 'Start' }).click()

    // Wait for deck builder
    for (const player of players) {
      await expect(player.page.getByText('Deck Builder')).toBeVisible({ timeout: 30000 })
    }

    // Build and submit decks
    for (const player of players) {
      const wsError = player.page.getByText('WebSocket connection error')
      if (await wsError.isVisible({ timeout: 500 }).catch(() => false)) {
        await player.page.reload()
        await expect(player.page.getByText('Deck Builder')).toBeVisible({ timeout: 15000 })
        await player.page.waitForTimeout(1000)
      }
      await buildAndSubmitDeck(player)
    }

    // Wait for tournament to start (standings visible)
    await expect(players[0]!.page.getByText('Standings')).toBeVisible({ timeout: 30000 })

    // Ready up all players
    for (const player of players) {
      const readyButton = player.page.getByRole('button', { name: 'Ready for Next Round' })
      if (await readyButton.isVisible({ timeout: 3000 }).catch(() => false)) {
        await readyButton.click()
        await player.page.waitForTimeout(300)
      }
    }

    // Wait for matches to start
    await players[0]!.page.waitForTimeout(5000)

    // === Identify who is in matches and who has a bye ===

    const matchPlayers: PlayerPage[] = []
    let byePlayer: PlayerPage | null = null

    for (const player of players) {
      if (await isInMatch(player)) {
        matchPlayers.push(player)
      } else {
        const hasBye = await player.page.getByText('Sitting out this round').isVisible({ timeout: 2000 }).catch(() => false)
        if (hasBye) byePlayer = player
      }
    }

    console.log(`Match players: ${matchPlayers.map(p => p.name).join(', ')}`)
    console.log(`BYE player: ${byePlayer?.name ?? 'none'}`)

    // 5 players → 2 matches (4 players) + 1 bye
    expect(matchPlayers.length).toBe(4)
    expect(byePlayer).not.toBeNull()

    // Keep hands for all match players
    for (const player of matchPlayers) {
      await keepHandIfNeeded(player)
    }

    // Wait for game boards to fully load
    await matchPlayers[0]!.page.waitForTimeout(3000)

    // === The critical test: concede ONE match, verify the other stays intact ===

    // Pick two players from different matches. With 4 match players, players are
    // paired as (0,1) vs (2,3) or similar. We need to find a player to concede
    // and then verify players in the OTHER match are still in their game.

    // Find a player who can concede (has Concede button visible)
    let conceder: PlayerPage | null = null
    for (const player of matchPlayers) {
      const hasConcede = await player.page.getByRole('button', { name: 'Concede' }).isVisible({ timeout: 2000 }).catch(() => false)
      if (hasConcede) {
        conceder = player
        break
      }
    }
    expect(conceder).not.toBeNull()
    console.log(`${conceder!.name} will concede their match`)

    // Before conceding, verify all 4 match players are in their games
    for (const player of matchPlayers) {
      const inGame = await isInMatch(player)
      expect(inGame).toBe(true)
      console.log(`${player.name}: in game ✓`)
    }

    // === CONCEDE one match ===
    await concedeMatch(conceder!)
    console.log(`${conceder!.name} conceded and returned to standings`)

    // The conceder's opponent should see Victory overlay
    // Give it a moment to propagate
    await conceder!.page.waitForTimeout(2000)

    // Find the opponent (has "Return to Menu" visible = they won)
    let winner: PlayerPage | null = null
    for (const player of matchPlayers) {
      if (player === conceder) continue
      const returnButton = player.page.getByRole('button', { name: 'Return to Menu' })
      if (await returnButton.isVisible({ timeout: 3000 }).catch(() => false)) {
        winner = player
        console.log(`${player.name} won via opponent's concession`)
        break
      }
    }

    // The remaining two players should be the OTHER match, still in their game
    const otherMatchPlayers = matchPlayers.filter(p => p !== conceder && p !== winner)
    console.log(`Other match players: ${otherMatchPlayers.map(p => p.name).join(', ')}`)
    expect(otherMatchPlayers.length).toBe(2)

    // === KEY ASSERTION: the other match players must still be in their game ===
    for (const player of otherMatchPlayers) {
      // They should still see the Concede button (= still in their game board)
      const stillInGame = await player.page.getByRole('button', { name: 'Concede' }).isVisible({ timeout: 5000 }).catch(() => false)
      expect(stillInGame).toBe(true)
      console.log(`${player.name}: still in game after other match conceded ✓`)

      // They should NOT see the standings page (which would mean they were kicked out)
      const seesStandings = await player.page.getByText('Standings').isVisible({ timeout: 1000 }).catch(() => false)
      expect(seesStandings).toBe(false)
      console.log(`${player.name}: not kicked to standings ✓`)
    }

    // === Also verify the winner can return to menu without issues ===
    if (winner) {
      const returnButton = winner.page.getByRole('button', { name: 'Return to Menu' })
      if (await returnButton.isVisible({ timeout: 1000 }).catch(() => false)) {
        await returnButton.click()
        await expect(winner.page.getByText('Standings')).toBeVisible({ timeout: 10000 })
        console.log(`${winner.name} returned to standings ✓`)
      }
    }

    // === Finish the test: concede the second match to complete the round ===
    let secondConceder: PlayerPage | null = null
    for (const player of otherMatchPlayers) {
      const hasConcede = await player.page.getByRole('button', { name: 'Concede' }).isVisible({ timeout: 2000 }).catch(() => false)
      if (hasConcede) {
        secondConceder = player
        break
      }
    }

    if (secondConceder) {
      console.log(`${secondConceder.name} conceding second match to finish round...`)
      await concedeMatch(secondConceder)

      // The second winner returns to menu
      const secondWinner = otherMatchPlayers.find(p => p !== secondConceder)
      if (secondWinner) {
        const returnButton = secondWinner.page.getByRole('button', { name: 'Return to Menu' })
        if (await returnButton.isVisible({ timeout: 5000 }).catch(() => false)) {
          await returnButton.click()
          await expect(secondWinner.page.getByText('Standings')).toBeVisible({ timeout: 10000 })
        }
      }
    }

    // All players should now be at standings
    for (const player of players) {
      await expect(player.page.getByText('Standings')).toBeVisible({ timeout: 10000 })
    }

    console.log('Concede isolation test passed!')
  })
})
