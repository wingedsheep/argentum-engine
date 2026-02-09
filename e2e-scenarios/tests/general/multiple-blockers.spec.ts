import { test, expect } from '../../fixtures/scenarioFixture'

/**
 * E2E browser tests for multiple blockers on a single attacker.
 *
 * When multiple creatures block the same attacker, the attacking player
 * must order the blockers for damage assignment (OrderBlockersDecision).
 *
 * Covers: Blocker ordering UI (OrderBlockersUI — drag/arrows to reorder, confirm)
 */
test.describe('Multiple blockers', () => {
  test('order blockers when two creatures block one attacker', async ({ createGame }) => {
    const { player1, player2 } = await createGame({
      player1Name: 'Attacker',
      player2Name: 'Defender',
      player1: {
        // Hill Giant (3/3) — large enough to kill at least one blocker
        battlefield: [{ name: 'Hill Giant', tapped: false, summoningSickness: false }],
      },
      player2: {
        battlefield: [
          { name: 'Glory Seeker' },
          { name: 'Grizzly Bears' },
        ],
        library: ['Mountain'],
      },
      phase: 'PRECOMBAT_MAIN',
      activePlayer: 1,
    })

    const p1 = player1.gamePage
    const p2 = player2.gamePage

    // Advance to combat and attack with Hill Giant
    await p1.pass()
    await p1.attackAll()

    // Defender blocks with both creatures
    await p2.declareBlocker('Glory Seeker', 'Hill Giant')
    await p2.declareBlocker('Grizzly Bears', 'Hill Giant')
    await p2.confirmBlockers()

    // Attacker must order blockers for damage assignment
    await p1.confirmBlockerOrder()

    // Combat damage resolves:
    // Hill Giant (3/3) assigns lethal (2) to first blocker, 1 to second
    // Both blockers deal 2 damage each = 4 total to Hill Giant (dies)
    // First-ordered blocker dies, second survives with 1 damage
    await p1.expectNotOnBattlefield('Hill Giant')

    // No combat damage to defending player (attacker was blocked)
    await p1.expectLifeTotal(player2.playerId, 20)

    await p1.screenshot('End state')
  })
})
