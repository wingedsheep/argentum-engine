import { test, expect } from '../../fixtures/scenarioFixture'

/**
 * E2E browser tests for Accursed Centaur.
 *
 * Card: Accursed Centaur ({B}) — Creature — Zombie Centaur (2/2)
 * "When Accursed Centaur enters the battlefield, sacrifice a creature."
 *
 * Covers: ETB sacrifice selection UI (targeting overlay with battlefield selection)
 */
test.describe('Accursed Centaur', () => {
  test('ETB triggers sacrifice selection when multiple creatures', async ({ createGame }) => {
    const { player1, player2 } = await createGame({
      player1Name: 'Caster',
      player2Name: 'Opponent',
      player1: {
        hand: ['Accursed Centaur'],
        battlefield: [
          { name: 'Swamp' },
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

    // Cast Accursed Centaur
    await p1.clickCard('Accursed Centaur')
    await p1.selectAction('Cast Accursed Centaur')

    // Spell auto-resolves, ETB trigger fires and auto-resolves (opponent has no responses).

    // Sacrifice selection appears (SelectCardsDecision with targeting UI).
    // Choose Glory Seeker and confirm.
    await p1.selectTarget('Glory Seeker')
    await p1.confirmTargets()

    // Glory Seeker is sacrificed
    await p1.expectNotOnBattlefield('Glory Seeker')

    // Accursed Centaur remains
    await p1.expectOnBattlefield('Accursed Centaur')

    await p1.screenshot('End state')
  })
})
