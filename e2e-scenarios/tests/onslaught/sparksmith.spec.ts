import { test, expect } from '../../fixtures/scenarioFixture'

/**
 * E2E browser tests for Sparksmith.
 *
 * Card: Sparksmith ({1}{R}) — Creature — Goblin (1/1)
 * "{T}: Sparksmith deals X damage to target creature and X damage to you,
 *  where X is the number of Goblins on the battlefield."
 *
 * Covers: Activated ability UI (click permanent → action menu → target selection)
 */
test.describe('Sparksmith', () => {
  test('tap to deal damage based on goblin count', async ({ createGame }) => {
    const { player1, player2 } = await createGame({
      player1Name: 'Goblin Player',
      player2Name: 'Opponent',
      player1: {
        battlefield: [
          { name: 'Sparksmith', tapped: false, summoningSickness: false },
          { name: 'Goblin Sky Raider', tapped: false, summoningSickness: false },
        ],
      },
      player2: {
        // Glory Seeker is 2/2 — will die to 2 damage (2 Goblins on battlefield)
        battlefield: [{ name: 'Glory Seeker' }],
        library: ['Mountain'],
      },
      phase: 'PRECOMBAT_MAIN',
      activePlayer: 1,
    })

    const p1 = player1.gamePage
    const p2 = player2.gamePage

    // Activate Sparksmith's ability (button shows full ability description)
    await p1.clickCard('Sparksmith')
    await p1.selectAction('damage to target')

    // Target Glory Seeker and confirm
    await p1.selectTarget('Glory Seeker')
    await p1.confirmTargets()

    // P1 auto-passes, P2 must resolve the ability
    await p2.pass()

    // Glory Seeker takes 2 damage (2 Goblins) and dies
    await p1.expectNotOnBattlefield('Glory Seeker')

    // Sparksmith's controller takes 2 damage too
    await p1.expectLifeTotal(player1.playerId, 18)

    await p1.screenshot('End state')
  })
})
