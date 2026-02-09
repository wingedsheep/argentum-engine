import { test, expect } from '../../fixtures/scenarioFixture'

/**
 * E2E browser tests for Nantuko Husk.
 *
 * Card: Nantuko Husk ({2}{B}) — Creature — Zombie Insect (2/2)
 * "Sacrifice a creature: Nantuko Husk gets +2/+2 until end of turn."
 *
 * Covers: Sacrifice-as-cost UI (activated ability with sacrifice cost → creature selection)
 */
test.describe('Nantuko Husk', () => {
  test('sacrifice creature to pump stats', async ({ createGame }) => {
    const { player1, player2 } = await createGame({
      player1Name: 'Caster',
      player2Name: 'Opponent',
      player1: {
        battlefield: [
          { name: 'Nantuko Husk', tapped: false, summoningSickness: false },
          { name: 'Glory Seeker', tapped: false, summoningSickness: false },
        ],
      },
      player2: {
        library: ['Mountain'],
      },
      phase: 'PRECOMBAT_MAIN',
      activePlayer: 1,
    })

    const p1 = player1.gamePage
    const p2 = player2.gamePage

    // Activate Nantuko Husk's ability (button shows full ability description)
    await p1.clickCard('Nantuko Husk')
    await p1.selectAction('Sacrifice a creature')

    // Sacrifice selection modal appears — select Glory Seeker and confirm
    await p1.selectTarget('Glory Seeker')
    await p1.confirmTargets()

    // P1 auto-passes, P2 must resolve the ability
    await p2.pass()

    // Glory Seeker should be sacrificed (gone from battlefield)
    await p1.expectNotOnBattlefield('Glory Seeker')

    // Nantuko Husk should now be 4/4 (base 2/2 + 2/+2)
    await p1.expectStats('Nantuko Husk', '4/4')

    await p1.screenshot('End state')
  })
})
