import { test, expect } from '../../fixtures/scenarioFixture'

/**
 * E2E browser test for Choking Tethers cycling trigger.
 *
 * Card: Choking Tethers (3U) — Instant
 * "Tap up to four target creatures.
 *  Cycling {1}{U}
 *  When you cycle Choking Tethers, you may tap target creature."
 */
test.describe('Choking Tethers — Cycling Trigger', () => {
  test('cycling triggers tap on target creature', async ({ createGame }) => {
    const { player1, player2 } = await createGame({
      player1Name: 'Player1',
      player2Name: 'Opponent',
      player1: {
        battlefield: [{ name: 'Island' }, { name: 'Island' }],
        hand: ['Choking Tethers'],
        library: ['Mountain'],
      },
      player2: {
        battlefield: [{ name: 'Glory Seeker' }],
        library: ['Mountain'],
      },
      phase: 'PRECOMBAT_MAIN',
      activePlayer: 1,
    })

    const p1 = player1.gamePage
    const p2 = player2.gamePage

    // Cycle Choking Tethers (costs {1}{U})
    await p1.clickCard('Choking Tethers')
    await p1.selectAction('Cycle')

    // Cycling trigger fires — target selection modal appears
    // Select opponent's Glory Seeker as target
    await p1.selectTarget('Glory Seeker')
    await p1.confirmTargets()

    // Trigger is on the stack — opponent resolves
    await p2.resolveStack('Choking Tethers trigger')

    // Verify: Glory Seeker is now tapped and a card was drawn from cycling
    await p1.expectTapped('Glory Seeker')
    await p1.expectHandSize(1)
  })

  test('declining the may effect leaves creature untapped but still draws', async ({ createGame }) => {
    const { player1, player2 } = await createGame({
      player1Name: 'Player1',
      player2Name: 'Opponent',
      player1: {
        battlefield: [{ name: 'Island' }, { name: 'Island' }],
        hand: ['Choking Tethers'],
        library: ['Mountain'],
      },
      player2: {
        battlefield: [{ name: 'Glory Seeker' }],
        library: ['Mountain'],
      },
      phase: 'PRECOMBAT_MAIN',
      activePlayer: 1,
    })

    const p1 = player1.gamePage
    const p2 = player2.gamePage

    // Cycle Choking Tethers
    await p1.clickCard('Choking Tethers')
    await p1.selectAction('Cycle')

    // Decline the optional target (clicks "Decline" button)
    await p1.skipTargets()

    // Verify: Glory Seeker remains untapped but a card was still drawn
    await p2.expectOnBattlefield('Glory Seeker')
    await p1.expectHandSize(1)
  })
})
