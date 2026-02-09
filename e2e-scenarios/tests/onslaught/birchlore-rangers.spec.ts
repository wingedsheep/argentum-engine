import { test, expect } from '../../fixtures/scenarioFixture'

/**
 * E2E browser tests for Birchlore Rangers (Morph).
 *
 * Card: Birchlore Rangers ({G}) — Creature — Elf Druid Ranger (1/1)
 * Morph {G}
 *
 * Covers: Morph UI (cast face-down as 2/2, turn face-up)
 */
test.describe('Birchlore Rangers', () => {
  test('cast face-down then turn face-up', async ({ createGame }) => {
    const { player1, player2 } = await createGame({
      player1Name: 'Caster',
      player2Name: 'Opponent',
      player1: {
        hand: ['Birchlore Rangers'],
        battlefield: [
          { name: 'Forest' },
          { name: 'Forest' },
          { name: 'Forest' },
          { name: 'Forest' },
        ],
      },
      player2: {
        library: ['Mountain'],
      },
      phase: 'PRECOMBAT_MAIN',
      activePlayer: 1,
    })

    const p1 = player1.gamePage

    // Cast Birchlore Rangers face-down (costs {3})
    await p1.clickCard('Birchlore Rangers')
    await p1.selectAction('Cast Face-Down')

    // Face-down creature should appear on battlefield (alt text is "Card back")
    await p1.expectOnBattlefield('Card back')

    // Turn the face-down creature face-up (morph cost {G})
    await p1.clickCard('Card back')
    await p1.selectAction('Turn Face-Up')

    // Birchlore Rangers should now be visible on the battlefield
    await p1.expectOnBattlefield('Birchlore Rangers')

    await p1.screenshot('End state')
  })
})
