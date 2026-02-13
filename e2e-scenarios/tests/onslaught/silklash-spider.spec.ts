import { test, expect } from '../../fixtures/scenarioFixture'

/**
 * E2E browser tests for Silklash Spider.
 *
 * Card: Silklash Spider ({3}{G}{G}) — Creature — Spider 2/7
 * Reach
 * {X}{G}{G}: Silklash Spider deals X damage to each creature with flying.
 *
 * Covers: X-cost selector UI for activated abilities (not just spells)
 */
test.describe('Silklash Spider', () => {
  test('activated ability with X=2 kills flying creatures and spares non-flying', async ({ createGame }) => {
    const { player1, player2 } = await createGame({
      player1Name: 'SpiderPlayer',
      player2Name: 'Opponent',
      player1: {
        battlefield: [
          { name: 'Silklash Spider', summoningSickness: false },
          // 4 Forests: 2 for {G}{G} cost + 2 for X=2
          { name: 'Forest' },
          { name: 'Forest' },
          { name: 'Forest' },
          { name: 'Forest' },
        ],
      },
      player2: {
        battlefield: [
          // 1/2 flyer — should die to X=2
          { name: 'Goblin Sky Raider' },
          // 2/2 non-flyer — should survive
          { name: 'Glory Seeker' },
        ],
        library: ['Mountain'],
      },
      phase: 'PRECOMBAT_MAIN',
      activePlayer: 1,
    })

    const p1 = player1.gamePage
    const p2 = player2.gamePage

    // Click Silklash Spider and select its activated ability
    await p1.clickCard('Silklash Spider')
    await p1.selectAction('Deal X damage')

    // X cost selector overlay should appear — set X = 2 and activate
    await p1.selectXValue(2)

    // Opponent resolves the ability
    await p2.pass()

    // The flying creature should be destroyed (2 damage kills 1/2)
    await p1.expectNotOnBattlefield('Goblin Sky Raider')

    // Non-flying creature should survive
    await p1.expectOnBattlefield('Glory Seeker')

    // Silklash Spider itself (no flying, 2/7) should survive
    await p1.expectOnBattlefield('Silklash Spider')

    await p1.screenshot('End state')
  })
})
