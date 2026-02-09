import { test, expect } from '../../fixtures/scenarioFixture'

/**
 * E2E browser test: activating abilities during declare blockers.
 *
 * Spitfire Handler (1/1) can't block creatures with power greater than its own.
 * By activating its {R}: +1/+0 ability twice during declare blockers it reaches
 * power 3 and becomes able to block the attacking Hill Giant (3/3).
 */
test.describe('Combat — instant-speed actions during declare blockers', () => {
  test('pump creature during declare blockers to enable blocking', async ({ createGame }) => {
    const { player1, player2 } = await createGame({
      player1Name: 'Attacker',
      player2Name: 'Defender',
      player1: {
        battlefield: [{ name: 'Hill Giant', tapped: false, summoningSickness: false }],
      },
      player2: {
        battlefield: [
          { name: 'Spitfire Handler', tapped: false, summoningSickness: false },
          { name: 'Mountain', tapped: false },
          { name: 'Mountain', tapped: false },
        ],
        library: ['Mountain'],
      },
      phase: 'PRECOMBAT_MAIN',
      activePlayer: 1,
    })

    const p1 = player1.gamePage
    const p2 = player2.gamePage

    // Advance to declare attackers
    await p1.pass()

    // P1 attacks with Hill Giant (3/3)
    await p1.attackAll()

    // P2 is now in declare blockers — Spitfire Handler is 1/1 and can't block 3/3

    // P2 activates Spitfire Handler's {R}: +1/+0 ability (first activation)
    await p2.clickCard('Spitfire Handler')
    await p2.selectAction('+1/+0')

    // P2 auto-passes, P1 resolves the ability on the stack
    await p1.pass()

    // Spitfire Handler is now 2/1
    await p2.expectStats('Spitfire Handler', '2/1')

    // P2 activates the ability again (second activation)
    await p2.clickCard('Spitfire Handler')
    await p2.selectAction('+1/+0')

    // P1 resolves
    await p1.pass()

    // Spitfire Handler is now 3/1 — can block Hill Giant (power 3 >= 3)
    await p2.expectStats('Spitfire Handler', '3/1')

    // P2 declares Spitfire Handler as blocker of Hill Giant
    await p2.declareBlocker('Spitfire Handler', 'Hill Giant')
    await p2.confirmBlockers()

    // Combat damage: Hill Giant (3/3) kills Spitfire Handler (3/1),
    // Spitfire Handler (3/1) kills Hill Giant (3/3)
    await p1.expectNotOnBattlefield('Hill Giant')
    await p1.expectNotOnBattlefield('Spitfire Handler')

    // No damage dealt to defending player (attacker was blocked)
    await p1.expectLifeTotal(player2.playerId, 20)

    await p1.screenshot('End state')
  })
})
