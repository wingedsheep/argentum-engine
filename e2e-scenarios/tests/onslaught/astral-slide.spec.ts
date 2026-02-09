import { test, expect } from '../../fixtures/scenarioFixture'

/**
 * E2E browser tests for Astral Slide cycling trigger.
 *
 * Card: Astral Slide (2W) — Enchantment
 * "Whenever a player cycles a card, you may exile target creature.
 *  If you do, return that card to the battlefield under its owner's
 *  control at the beginning of the next end step."
 *
 * Mirrors: AstralSlideScenarioTest.kt
 */
test.describe('Astral Slide', () => {
  test('cycling triggers Astral Slide to exile creature', async ({ createGame }) => {
    const { player1, player2 } = await createGame({
      player1Name: 'Player1',
      player2Name: 'Opponent',
      player1: {
        battlefield: [{ name: 'Astral Slide' }, { name: 'Plains' }, { name: 'Plains' }],
        hand: ['Disciple of Grace'],
        library: ['Mountain'],
      },
      player2: {
        battlefield: [{ name: 'Glory Seeker' }],
      },
      phase: 'PRECOMBAT_MAIN',
      activePlayer: 1,
    })

    const p1 = player1.gamePage
    const p2 = player2.gamePage

    // Cycle Disciple of Grace
    await p1.clickCard('Disciple of Grace')
    await p1.selectAction('Cycle')

    // Astral Slide triggers — may decision: choose yes to exile
    await p1.answerYes()

    // Select opponent's Glory Seeker as target (battlefield targeting auto-confirms)
    await p1.selectTarget('Glory Seeker')

    // Trigger is on the stack — opponent resolves
    await p2.resolveStack('Astral Slide trigger')

    // Verify: Glory Seeker is no longer on the battlefield (exiled)
    await p1.expectNotOnBattlefield('Glory Seeker')

    await p1.screenshot('End state')
  })

  test('may decline exile - creature stays on battlefield', async ({ createGame }) => {
    const { player1, player2 } = await createGame({
      player1Name: 'Player1',
      player2Name: 'Opponent',
      player1: {
        battlefield: [{ name: 'Astral Slide' }, { name: 'Plains' }, { name: 'Plains' }],
        hand: ['Disciple of Grace'],
        library: ['Mountain'],
      },
      player2: {
        battlefield: [{ name: 'Glory Seeker' }],
      },
      phase: 'PRECOMBAT_MAIN',
      activePlayer: 1,
    })

    const p1 = player1.gamePage
    const p2 = player2.gamePage

    // Cycle Disciple of Grace
    await p1.clickCard('Disciple of Grace')
    await p1.selectAction('Cycle')

    // Astral Slide triggers — decline the may effect
    await p1.answerNo()

    // Verify: Glory Seeker still on the battlefield
    await p2.expectOnBattlefield('Glory Seeker')

    await p2.screenshot('End state')
  })
})
