import { test, expect } from '../../fixtures/scenarioFixture'

/**
 * E2E browser tests for basic combat flow.
 *
 * Tests the full attack → block → damage sequence through the browser UI.
 */
test.describe('Combat', () => {
  test('attack with creature, no blockers, deal damage', async ({ createGame }) => {
    const { player1, player2 } = await createGame({
      player1Name: 'Attacker',
      player2Name: 'Defender',
      player1: {
        battlefield: [{ name: 'Grizzly Bears', tapped: false, summoningSickness: false }],
      },
      player2: {
        lifeTotal: 20,
      },
      phase: 'PRECOMBAT_MAIN',
      activePlayer: 1,
    })

    const p1 = player1.gamePage

    // Pass priority to advance from main phase to declare attackers step
    await p1.pass()

    // Declare all creatures as attackers
    await p1.attackAll()

    // Auto-pass handles blocking (opponent has no creatures) and combat damage

    // Verify: opponent took 2 damage (Grizzly Bears is 2/2)
    await p1.expectLifeTotal(player2.playerId, 18)

    await p1.screenshot('End state')
  })
})
