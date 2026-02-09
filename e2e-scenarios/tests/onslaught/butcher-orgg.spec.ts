import { test, expect } from '../../fixtures/scenarioFixture'

/**
 * E2E browser tests for Butcher Orgg's combat damage distribution.
 *
 * Card: Butcher Orgg ({4}{R}{R}{R}) — Creature — Orgg (6/6)
 * "You may assign Butcher Orgg's combat damage divided as you choose
 *  among defending player and/or any number of creatures they control."
 *
 * Covers: Inline DistributeDecision UI (+/- buttons on cards and life display)
 */
test.describe('Butcher Orgg', () => {
  test('distribute combat damage between blocker and defending player', async ({ createGame }) => {
    const { player1, player2 } = await createGame({
      player1Name: 'Attacker',
      player2Name: 'Defender',
      player1: {
        battlefield: [{ name: 'Butcher Orgg', tapped: false, summoningSickness: false }],
      },
      player2: {
        battlefield: [{ name: 'Grizzly Bears' }],
        library: ['Mountain'],
      },
      phase: 'PRECOMBAT_MAIN',
      activePlayer: 1,
    })

    const p1 = player1.gamePage
    const p2 = player2.gamePage

    // Advance to declare attackers
    await p1.pass()

    // Attack with Butcher Orgg (only creature, use attackAll to avoid combat overlay click issue)
    await p1.attackAll()

    // Defender blocks with Grizzly Bears
    await p2.declareBlocker('Grizzly Bears', 'Butcher Orgg')
    await p2.confirmBlockers()

    // Auto-pass → combat damage step → distribute decision for attacker
    // Wait for the distribute mode to appear
    await p1.page.getByRole('button', { name: 'Confirm Damage' }).waitFor({ state: 'visible', timeout: 10_000 })

    // Allocate 2 damage to Grizzly Bears (lethal for 2/2) and 4 to defending player
    await p1.allocateDamage('Grizzly Bears', 2)
    await p1.allocateDamageToPlayer(player2.playerId, 4)
    await p1.confirmDamage()

    // Grizzly Bears should be dead (2 lethal damage)
    await p1.expectNotOnBattlefield('Grizzly Bears')

    // Defending player took 4 damage
    await p1.expectLifeTotal(player2.playerId, 16)

    // Butcher Orgg survives (6 toughness - 2 damage from blocker)
    await p1.expectOnBattlefield('Butcher Orgg')

    await p1.screenshot('End state')
  })

  test('unblocked deals full damage to defending player', async ({ createGame }) => {
    const { player1, player2 } = await createGame({
      player1Name: 'Attacker',
      player2Name: 'Defender',
      player1: {
        battlefield: [{ name: 'Butcher Orgg', tapped: false, summoningSickness: false }],
      },
      player2: {
        library: ['Mountain'],
      },
      phase: 'PRECOMBAT_MAIN',
      activePlayer: 1,
    })

    const p1 = player1.gamePage

    // Advance to declare attackers
    await p1.pass()

    // Attack with Butcher Orgg
    await p1.attackAll()

    // No blockers → auto-pass → full 6 damage to defending player
    await p1.expectLifeTotal(player2.playerId, 14)

    await p1.screenshot('End state')
  })
})
