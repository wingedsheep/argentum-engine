import { test, expect } from '../../fixtures/scenarioFixture'

/**
 * E2E browser test for Krosan Groundshaker blocked by a sacrificed creature.
 *
 * Scenario: Krosan Groundshaker (6/6) attacks, Skirk Prospector (1/1) blocks,
 * then the blocker sacrifices itself (via its own mana ability) before combat
 * damage. Since the attacker was blocked but the blocker is gone and the
 * attacker has no trample, it deals no combat damage to the player.
 */
test.describe('Krosan Groundshaker blocked by sacrificed creature', () => {
  test('deals no damage when blocker is sacrificed before combat damage', async ({ createGame }) => {
    const { player1, player2 } = await createGame({
      player1Name: 'Attacker',
      player2Name: 'Defender',
      player1: {
        battlefield: [
          { name: 'Krosan Groundshaker', tapped: false, summoningSickness: false },
        ],
        library: ['Forest'],
      },
      player2: {
        battlefield: [
          { name: 'Skirk Prospector', tapped: false, summoningSickness: false },
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

    // Attack with Krosan Groundshaker
    await p1.attackWith('Krosan Groundshaker')

    // P2 has a meaningful action (Skirk Prospector sacrifice) so must pass to blockers
    await p2.pass()

    // Defender blocks with Skirk Prospector
    await p2.declareBlocker('Skirk Prospector', 'Krosan Groundshaker')
    await p2.confirmBlockers()

    // P2 sacrifices Skirk Prospector using its own mana ability before combat damage
    await p2.clickCard('Skirk Prospector')
    await p2.selectAction('Sacrifice a creature Goblin')
    await p2.selectTarget('Skirk Prospector')
    await p2.confirmTargets()

    // Skirk Prospector is gone
    await p2.expectNotOnBattlefield('Skirk Prospector')

    // Krosan Groundshaker was blocked but blocker is gone — no combat damage to player
    await p1.expectLifeTotal(player2.playerId, 20)

    // Krosan Groundshaker survives (no blocker to deal damage to it)
    await p1.expectOnBattlefield('Krosan Groundshaker')

    await p1.screenshot('End state — no damage dealt')
  })
})
