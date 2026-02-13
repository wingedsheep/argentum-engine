import { test, expect } from '../../fixtures/scenarioFixture'

/**
 * E2E browser test: killing a blocker after declare blockers (before combat damage).
 *
 * When a blocker is destroyed after blocker ordering, its reference must be
 * cleaned up from the attacker's BlockedComponent and DamageAssignmentOrderComponent.
 * Otherwise the damage assignment modal shows inconsistent data and freezes the game.
 *
 * Scenario: P1 attacks with Blistering Firecat (7/1 trample haste). P2 blocks with
 * two 2/2 creatures. P1 orders blockers, then casts Shock to kill one blocker.
 * Damage assignment modal should show only the surviving blocker + defending player.
 */
test.describe('Combat — kill blocker after ordering', () => {
  test('shock kills one blocker, damage assignment shows only survivor', async ({ createGame }) => {
    const { player1, player2 } = await createGame({
      player1Name: 'Attacker',
      player2Name: 'Defender',
      player1: {
        battlefield: [
          { name: 'Blistering Firecat', tapped: false, summoningSickness: false },
          { name: 'Mountain', tapped: false },
        ],
        hand: ['Shock'],
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

    // Advance to declare attackers
    await p1.pass()

    // P1 attacks with Blistering Firecat (7/1 trample haste)
    await p1.attackAll()

    // P2 declares both creatures as blockers
    await p2.declareBlocker('Glory Seeker', 'Blistering Firecat')
    await p2.declareBlocker('Grizzly Bears', 'Blistering Firecat')
    await p2.confirmBlockers()

    // P1 confirms blocker order (default order)
    await p1.confirmBlockerOrder()

    // P1 casts Shock targeting Glory Seeker
    await p1.clickCard('Shock')
    await p1.selectAction('Cast Shock')
    await p1.selectTarget('Glory Seeker')
    await p1.confirmTargets()

    // P2 must pass priority to let Shock resolve
    await p2.pass()

    // Glory Seeker should be dead (2 damage from Shock >= 2 toughness)
    await p1.expectNotOnBattlefield('Glory Seeker')

    // Both players pass → combat damage step
    // Damage assignment modal appears with only Grizzly Bears + defending player
    // Default: 2 to Grizzly Bears (lethal), 5 to defending player
    await p1.confirmDamage()

    // Grizzly Bears should be dead (2 lethal damage)
    await p1.expectNotOnBattlefield('Grizzly Bears')

    // Blistering Firecat should be dead (took 2 damage from Grizzly Bears, 1 toughness)
    await p1.expectNotOnBattlefield('Blistering Firecat')

    // Defending player took 5 trample damage (7 - 2 = 5)
    await p1.expectLifeTotal(player2.playerId, 15)

    await p1.screenshot('End state')
  })
})
