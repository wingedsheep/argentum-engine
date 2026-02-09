import { test, expect } from '../../fixtures/scenarioFixture'

/**
 * E2E browser tests for Skirk Prospector.
 *
 * Card: Skirk Prospector ({R}) — Creature — Goblin (1/1)
 * "Sacrifice a Goblin: Add {R}."
 *
 * Covers: Mana ability with sacrifice cost — sacrifice selection UI must appear
 * even though the ability is a mana ability (resolves immediately, no stack).
 */
test.describe('Skirk Prospector', () => {
  test('sacrifice a goblin to add mana', async ({ createGame }) => {
    const { player1, player2 } = await createGame({
      player1Name: 'Goblin Player',
      player2Name: 'Opponent',
      player1: {
        battlefield: [
          { name: 'Skirk Prospector', tapped: false, summoningSickness: false },
          { name: 'Festering Goblin', tapped: false, summoningSickness: false },
        ],
      },
      player2: {
        library: ['Mountain'],
      },
      phase: 'PRECOMBAT_MAIN',
      activePlayer: 1,
    })

    const p1 = player1.gamePage

    // Click Skirk Prospector to see its ability
    await p1.clickCard('Skirk Prospector')
    await p1.selectAction('Sacrifice a creature Goblin')

    // Sacrifice selection modal appears — select Festering Goblin and confirm
    await p1.selectTarget('Festering Goblin')
    await p1.confirmTargets()

    // Mana ability resolves immediately (no stack) — Festering Goblin should be gone
    await p1.expectNotOnBattlefield('Festering Goblin')

    // Skirk Prospector should still be on the battlefield
    await p1.expectOnBattlefield('Skirk Prospector')

    await p1.screenshot('End state')
  })

  test('sacrifice itself to add mana', async ({ createGame }) => {
    const { player1, player2 } = await createGame({
      player1Name: 'Goblin Player',
      player2Name: 'Opponent',
      player1: {
        battlefield: [
          { name: 'Skirk Prospector', tapped: false, summoningSickness: false },
        ],
      },
      player2: {
        library: ['Mountain'],
      },
      phase: 'PRECOMBAT_MAIN',
      activePlayer: 1,
    })

    const p1 = player1.gamePage

    // Click Skirk Prospector — it's a Goblin so it can sacrifice itself
    await p1.clickCard('Skirk Prospector')
    await p1.selectAction('Sacrifice a creature Goblin')

    // Select itself as the sacrifice target
    await p1.selectTarget('Skirk Prospector')
    await p1.confirmTargets()

    // Skirk Prospector should be gone from the battlefield
    await p1.expectNotOnBattlefield('Skirk Prospector')

    await p1.screenshot('End state')
  })
})
