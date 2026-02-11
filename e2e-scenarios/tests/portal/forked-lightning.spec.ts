import { test, expect } from '../../fixtures/scenarioFixture'

/**
 * E2E browser tests for Forked Lightning.
 *
 * Card: Forked Lightning ({3}{R}) — Sorcery
 * "Forked Lightning deals 4 damage divided as you choose among one, two,
 *  or three target creatures."
 *
 * Covers: Spell-based damage distribution (DamageDistributionModal with +/- buttons)
 */
test.describe('Forked Lightning', () => {
  test('divide 4 damage among two creatures', async ({ createGame }) => {
    const { player1, player2 } = await createGame({
      player1Name: 'Caster',
      player2Name: 'Opponent',
      player1: {
        hand: ['Forked Lightning'],
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

    // Cast Forked Lightning
    await p1.clickCard('Forked Lightning')
    await p1.selectAction('Cast Forked Lightning')

    // Select two targets — click both creatures
    await p1.selectTarget('Glory Seeker')
    await p1.selectTarget('Grizzly Bears')
    await p1.confirmTargets()

    // DamageDistributionModal appears — each target starts at 1 damage (minPerTarget).
    // Need to add 1 more to each to reach 2 damage each (total 4).
    await p1.increaseDamageAllocation('Glory Seeker', 1)
    await p1.increaseDamageAllocation('Grizzly Bears', 1)
    await p1.castSpellFromDistribution()

    // Spell auto-resolves (opponent has no responses)

    // Both 2/2 creatures should be destroyed
    await p1.expectNotOnBattlefield('Glory Seeker')
    await p1.expectNotOnBattlefield('Grizzly Bears')

    await p1.screenshot('End state')
  })
})
