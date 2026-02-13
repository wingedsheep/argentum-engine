import { test, expect } from '../../fixtures/scenarioFixture'

/**
 * E2E browser tests for ghost cards in the hand zone.
 *
 * Graveyard cards with legal activated abilities appear as translucent
 * "ghost" cards appended to the player's hand for discoverability.
 * Triggered abilities (e.g., Gigapede) do NOT produce ghost cards.
 */
test.describe('Ghost cards in hand', () => {
  test('Undead Gladiator shows as ghost card during upkeep', async ({ createGame }) => {
    const { player1 } = await createGame({
      player1Name: 'Player1',
      player2Name: 'Opponent',
      player1: {
        graveyard: ['Undead Gladiator'],
        hand: ['Swamp'],
        battlefield: [{ name: 'Swamp' }, { name: 'Swamp' }],
        library: ['Mountain'],
      },
      player2: {
        library: ['Mountain'],
      },
      phase: 'BEGINNING',
      step: 'UPKEEP',
      activePlayer: 1,
      player1StopAtSteps: ['UPKEEP'],
    })

    const p1 = player1.gamePage

    // Ghost card should appear in hand zone with purple glow
    await p1.expectGhostCardInHand('Undead Gladiator')
    // Real hand should only have the Swamp
    await p1.expectHandSize(1)

    // Clicking the ghost card opens the action menu with the ability
    await p1.selectCardInHand('Undead Gladiator')
    // Verify the ability button is shown (partial match on description)
    const actionButton = p1.page.locator('button').filter({ hasText: "Return this creature" })
    await expect(actionButton.first()).toBeVisible({ timeout: 10_000 })

    await p1.screenshot('Ghost card with action menu')
  })

  test('no ghost card outside upkeep (main phase)', async ({ createGame }) => {
    const { player1 } = await createGame({
      player1Name: 'Player1',
      player2Name: 'Opponent',
      player1: {
        graveyard: ['Undead Gladiator'],
        hand: ['Swamp'],
        battlefield: [{ name: 'Swamp' }, { name: 'Swamp' }],
        library: ['Mountain'],
      },
      player2: {
        library: ['Mountain'],
      },
      phase: 'PRECOMBAT_MAIN',
      activePlayer: 1,
    })

    const p1 = player1.gamePage

    // No ghost card — restriction is upkeep only
    await p1.expectNoGhostCardInHand('Undead Gladiator')
    await p1.expectHandSize(1)

    await p1.screenshot('No ghost card in main phase')
  })

  test('Gigapede (triggered ability) does NOT show ghost card', async ({ createGame }) => {
    const { player1 } = await createGame({
      player1Name: 'Player1',
      player2Name: 'Opponent',
      player1: {
        graveyard: ['Gigapede'],
        hand: ['Forest'],
        library: ['Mountain'],
      },
      player2: {
        library: ['Mountain'],
      },
      phase: 'BEGINNING',
      step: 'UPKEEP',
      activePlayer: 1,
      player1StopAtSteps: ['UPKEEP'],
    })

    const p1 = player1.gamePage

    // Gigapede uses a triggered ability, not an activated ability — no ghost card
    await p1.expectNoGhostCardInHand('Gigapede')
    await p1.expectHandSize(1)

    await p1.screenshot('No ghost card for triggered ability')
  })
})
