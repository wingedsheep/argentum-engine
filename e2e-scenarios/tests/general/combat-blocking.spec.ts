import { test, expect } from '../../fixtures/scenarioFixture'

/**
 * E2E browser tests for combat with blocking.
 *
 * Tests the declare blockers flow: selecting a blocker, assigning it
 * to an attacker, confirming blocks, and verifying combat damage.
 *
 * Covers: Blocking UI (declareBlocker, confirmBlockers, creature trading)
 */
test.describe('Combat blocking', () => {
  test('creatures trade when blocked by equal-sized creature', async ({ createGame }) => {
    const { player1, player2 } = await createGame({
      player1Name: 'Attacker',
      player2Name: 'Defender',
      player1: {
        battlefield: [{ name: 'Grizzly Bears', tapped: false, summoningSickness: false }],
        library: ['Forest'],
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

    // Attack with Grizzly Bears (2/2) — only creature, use attackAll
    await p1.attackAll()

    // Defender blocks with Glory Seeker (2/2) — creatures will trade
    await p2.declareBlocker('Glory Seeker', 'Grizzly Bears')
    await p2.confirmBlockers()

    // Auto-pass → combat damage → both 2/2 creatures deal lethal to each other
    // Both creatures should be destroyed
    await p1.expectNotOnBattlefield('Grizzly Bears')
    await p1.expectNotOnBattlefield('Glory Seeker')

    // No damage to defending player (attacker was blocked)
    await p1.expectLifeTotal(player2.playerId, 20)

    await p1.screenshot('End state')
  })

  test('no blocks — attacker deals damage to player', async ({ createGame }) => {
    const { player1, player2 } = await createGame({
      player1Name: 'Attacker',
      player2Name: 'Defender',
      player1: {
        battlefield: [{ name: 'Grizzly Bears', tapped: false, summoningSickness: false }],
        library: ['Forest'],
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

    // Attack with Grizzly Bears — only creature, use attackAll
    await p1.attackAll()

    // Defender chooses not to block
    await p2.noBlocks()

    // Grizzly Bears deals 2 damage to defending player
    await p1.expectLifeTotal(player2.playerId, 18)

    // Both creatures survive
    await p1.expectOnBattlefield('Grizzly Bears')
    await p2.expectOnBattlefield('Glory Seeker')

    await p1.screenshot('End state')
  })
})
