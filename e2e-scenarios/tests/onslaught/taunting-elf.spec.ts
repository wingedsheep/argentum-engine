import { test, expect } from '../../fixtures/scenarioFixture'

/**
 * E2E browser tests for Taunting Elf (must-be-blocked-by-all).
 *
 * Taunting Elf has "All creatures able to block Taunting Elf do so."
 * When it attacks, the client auto-assigns all valid blockers to it,
 * so the defending player can simply confirm without manual dragging.
 *
 * Covers: Auto-assign blockers for must-be-blocked attackers, confirm blocks, combat damage.
 */
test.describe('Taunting Elf — must be blocked by all', () => {
  test('able blockers auto-assigned, cant-block creatures excluded', async ({ createGame }) => {
    const { player1, player2 } = await createGame({
      player1Name: 'Attacker',
      player2Name: 'Defender',
      player1: {
        battlefield: [{ name: 'Taunting Elf', tapped: false, summoningSickness: false }],
      },
      player2: {
        battlefield: [{ name: 'Devoted Hero' }, { name: 'Glory Seeker' }, { name: 'Jungle Lion' }],
        library: ['Mountain'],
      },
      phase: 'PRECOMBAT_MAIN',
      activePlayer: 1,
    })

    const p1 = player1.gamePage
    const p2 = player2.gamePage

    // Advance to declare attackers
    await p1.pass()

    // Attack with Taunting Elf
    await p1.attackAll()

    // Taunting Elf's triggered ability auto-resolves (opponent has no responses)

    // Defender should see "Confirm Blocks" immediately — able blockers auto-assigned.
    // Jungle Lion (can't block) should NOT be auto-assigned.
    await p2.confirmBlockers()

    // Multiple blockers on one attacker → P1 must order blockers for damage assignment
    await p1.confirmBlockerOrder()

    // Taunting Elf (0/1) is blocked by Devoted Hero (1/2) and Glory Seeker (2/2)
    // It receives 3 damage total and dies. Both blockers survive (0 damage from 0-power elf).
    await p1.expectNotOnBattlefield('Taunting Elf')
    await p1.expectOnBattlefield('Devoted Hero')
    await p1.expectOnBattlefield('Glory Seeker')

    // Jungle Lion stayed on battlefield — it can't block so wasn't assigned
    await p1.expectOnBattlefield('Jungle Lion')

    // No damage to defending player (attacker was blocked)
    await p1.expectLifeTotal(player2.playerId, 20)

    await p1.screenshot('End state')
  })

  test('must-block forces all blockers to Taunting Elf, leaving other attacker unblocked', async ({ createGame }) => {
    const { player1, player2 } = await createGame({
      player1Name: 'Attacker',
      player2Name: 'Defender',
      player1: {
        battlefield: [
          { name: 'Taunting Elf', tapped: false, summoningSickness: false },
          { name: 'Grizzly Bears', tapped: false, summoningSickness: false },
        ],
      },
      player2: {
        battlefield: [{ name: 'Devoted Hero' }, { name: 'Glory Seeker' }],
        library: ['Mountain'],
      },
      phase: 'PRECOMBAT_MAIN',
      activePlayer: 1,
    })

    const p1 = player1.gamePage
    const p2 = player2.gamePage

    // Advance to declare attackers
    await p1.pass()

    // Attack with both creatures
    await p1.attackAll()

    // Taunting Elf's triggered ability auto-resolves (opponent has no responses)

    // Both blockers are auto-assigned to Taunting Elf (must block). Just confirm.
    await p2.confirmBlockers()

    // Multiple blockers on Taunting Elf → P1 must order blockers for damage assignment
    await p1.confirmBlockerOrder()

    // Taunting Elf (0/1) blocked by both — elf dies, both blockers survive (0-power elf)
    await p1.expectNotOnBattlefield('Taunting Elf')
    await p1.expectOnBattlefield('Devoted Hero')
    await p1.expectOnBattlefield('Glory Seeker')

    // Grizzly Bears (2/2) unblocked — deals 2 damage to defender
    await p1.expectOnBattlefield('Grizzly Bears')
    await p1.expectLifeTotal(player2.playerId, 18)

    await p1.screenshot('End state')
  })
})
