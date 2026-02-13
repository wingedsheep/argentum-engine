import { test, expect } from '../../fixtures/scenarioFixture'

/**
 * E2E browser tests for Starstorm.
 *
 * Card: Starstorm ({X}{R}{R}) — Instant
 * "Starstorm deals X damage to each creature."
 * Cycling {3}
 *
 * Covers: X-cost selector UI (slider + Cast button)
 */
test.describe('Starstorm', () => {
  test('cast with X=2 deals 2 damage to each creature', async ({ createGame }) => {
    const { player1, player2 } = await createGame({
      player1Name: 'Caster',
      player2Name: 'Opponent',
      player1: {
        hand: ['Starstorm'],
        battlefield: [
          { name: 'Mountain' },
          { name: 'Mountain' },
          { name: 'Mountain' },
          { name: 'Mountain' },
        ],
      },
      player2: {
        battlefield: [{ name: 'Glory Seeker' }, { name: 'Grizzly Bears' }],
        library: ['Mountain'],
      },
      phase: 'PRECOMBAT_MAIN',
      activePlayer: 1,
    })

    const p1 = player1.gamePage
    const p2 = player2.gamePage

    // Cast Starstorm — X cost selector overlay appears
    await p1.clickCard('Starstorm')
    await p1.selectAction('Cast Starstorm')

    // Set X = 2 on the slider and click Cast
    await p1.selectXValue(2)

    // Opponent resolves the spell
    await p2.resolveStack('Starstorm')

    // Both 2/2 creatures should be destroyed (2 damage to each)
    await p1.expectNotOnBattlefield('Glory Seeker')
    await p1.expectNotOnBattlefield('Grizzly Bears')

    await p1.screenshot('End state')
  })
})
