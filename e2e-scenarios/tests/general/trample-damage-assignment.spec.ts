import { test, expect } from '../../fixtures/scenarioFixture'

/**
 * E2E browser tests for trample combat damage assignment.
 *
 * When a creature with trample is blocked, the attacking player must assign
 * at least lethal damage to each blocker before excess goes to the defending
 * player. The CombatDamageAssignmentModal presents +/- controls pre-filled
 * with the optimal (lethal) default.
 *
 * Uses: Blistering Firecat (7/1 trample haste) and Glory Seeker (2/2 vanilla).
 */
test.describe('Trample damage assignment', () => {
  test('accept default damage assignment — lethal to blocker, excess to player', async ({ createGame }) => {
    const { player1, player2 } = await createGame({
      player1Name: 'Attacker',
      player2Name: 'Defender',
      player1: {
        battlefield: [{ name: 'Blistering Firecat', tapped: false, summoningSickness: false }],
      },
      player2: {
        battlefield: [{ name: 'Glory Seeker' }],
        library: ['Mountain'],
      },
      phase: 'PRECOMBAT_MAIN',
      activePlayer: 1,
    })

    const p1 = player1.gamePage
    const p2 = player2.gamePage

    // Advance to declare attackers
    await p1.pass()

    // Attack with Blistering Firecat (7/1 trample)
    await p1.attackAll()

    // Defender blocks with Glory Seeker (2/2)
    await p2.declareBlocker('Glory Seeker', 'Blistering Firecat')
    await p2.confirmBlockers()

    // Damage assignment modal appears — defaults: 2 to Glory Seeker (lethal), 5 to player
    // Just confirm the default
    await p1.confirmDamage()

    // Glory Seeker should be dead (2 lethal damage)
    await p1.expectNotOnBattlefield('Glory Seeker')

    // Defending player took 5 trample damage (7 - 2 = 5)
    await p1.expectLifeTotal(player2.playerId, 15)

    await p1.screenshot('End state')
  })

  test('custom damage assignment — put extra damage on blocker', async ({ createGame }) => {
    const { player1, player2 } = await createGame({
      player1Name: 'Attacker',
      player2Name: 'Defender',
      player1: {
        battlefield: [{ name: 'Blistering Firecat', tapped: false, summoningSickness: false }],
      },
      player2: {
        battlefield: [{ name: 'Glory Seeker' }],
        library: ['Mountain'],
      },
      phase: 'PRECOMBAT_MAIN',
      activePlayer: 1,
    })

    const p1 = player1.gamePage
    const p2 = player2.gamePage

    // Advance to declare attackers
    await p1.pass()

    // Attack with Blistering Firecat
    await p1.attackAll()

    // Defender blocks with Glory Seeker
    await p2.declareBlocker('Glory Seeker', 'Blistering Firecat')
    await p2.confirmBlockers()

    // Damage assignment modal appears with defaults: 2 to blocker, 5 to player
    // First decrease player damage to free up points, then increase blocker damage
    await p1.decreaseCombatDamage('Defender', 2)
    await p1.increaseCombatDamage('Glory Seeker', 2)
    await p1.confirmDamage()

    // Glory Seeker still dies (4 >= 2 toughness)
    await p1.expectNotOnBattlefield('Glory Seeker')

    // Defending player took only 3 trample damage
    await p1.expectLifeTotal(player2.playerId, 17)

    await p1.screenshot('End state')
  })

  test('no damage assignment for non-trample creature blocked by single blocker', async ({ createGame }) => {
    const { player1, player2 } = await createGame({
      player1Name: 'Attacker',
      player2Name: 'Defender',
      player1: {
        // Hill Giant (3/3) — no trample
        battlefield: [{ name: 'Hill Giant', tapped: false, summoningSickness: false }],
      },
      player2: {
        battlefield: [{ name: 'Glory Seeker' }],
        library: ['Mountain'],
      },
      phase: 'PRECOMBAT_MAIN',
      activePlayer: 1,
    })

    const p1 = player1.gamePage
    const p2 = player2.gamePage

    // Advance to declare attackers
    await p1.pass()

    // Attack with Hill Giant
    await p1.attackAll()

    // Defender blocks with Glory Seeker
    await p2.declareBlocker('Glory Seeker', 'Hill Giant')
    await p2.confirmBlockers()

    // No damage assignment modal — auto-assigned since single blocker, no trample
    // Glory Seeker dies (3 damage >= 2 toughness)
    await p1.expectNotOnBattlefield('Glory Seeker')

    // No trample — no damage to player
    await p1.expectLifeTotal(player2.playerId, 20)

    // Hill Giant survives (3 toughness - 2 damage from Glory Seeker = 1 remaining)
    await p1.expectOnBattlefield('Hill Giant')

    await p1.screenshot('End state')
  })
})
