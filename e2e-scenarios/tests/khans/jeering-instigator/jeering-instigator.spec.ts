import { test, expect } from '../../../fixtures/scenarioFixture'

/**
 * E2E browser tests for Jeering Instigator.
 *
 * Card: Jeering Instigator ({1}{R}) — Creature — Goblin Rogue (2/1)
 * Morph {2}{R}
 * When this creature is turned face up, if it's your turn, gain control of
 * another target creature until end of turn. Untap that creature.
 * It gains haste until end of turn.
 */
test.describe('Jeering Instigator', () => {
  test('turn face up gains control of opponent creature', async ({ createGame }) => {
    const { player1, player2 } = await createGame({
      player1Name: 'Morpher',
      player2Name: 'Opponent',
      player1: {
        hand: ['Jeering Instigator'],
        battlefield: [
          { name: 'Mountain' },
          { name: 'Mountain' },
          { name: 'Mountain' },
          { name: 'Mountain' },
          { name: 'Mountain' },
          { name: 'Mountain' },
        ],
        library: ['Mountain'],
      },
      player2: {
        battlefield: [
          { name: 'Glory Seeker', tapped: true },
        ],
        library: ['Mountain'],
      },
      phase: 'PRECOMBAT_MAIN',
      activePlayer: 1,
    })

    const p1 = player1.gamePage
    const p2 = player2.gamePage

    // Cast Jeering Instigator face-down (costs {3})
    await p1.clickCard('Jeering Instigator')
    await p1.selectAction('Cast Face-Down')

    // Face-down creature should appear on battlefield
    await p1.expectOnBattlefield('Card back')

    // Turn the face-down creature face-up (morph cost {2}{R})
    await p1.clickCard('Card back')
    await p1.selectAction('Turn Face-Up')

    // Triggered ability fires — select opponent's creature as target
    await p1.selectTarget('Glory Seeker')
    await p1.confirmTargets()

    // P2 gets priority to respond to the triggered ability on the stack
    await p2.resolveStack('Jeering Instigator trigger')

    // After resolution:
    // 1. P1 should control Glory Seeker (it appears on P1's battlefield)
    await p1.expectOnBattlefield('Glory Seeker')
    // 2. Jeering Instigator should be face-up on P1's battlefield
    await p1.expectOnBattlefield('Jeering Instigator')

    await p1.screenshot('End state — P1 controls Glory Seeker')
  })
})
