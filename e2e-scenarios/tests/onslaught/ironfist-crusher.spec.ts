import { test, expect } from '../../fixtures/scenarioFixture'

/**
 * E2E browser tests for Ironfist Crusher (Onslaught).
 *
 * Ironfist Crusher {4}{W}
 * Creature — Human Soldier 2/4
 * Ironfist Crusher can block any number of creatures.
 * Morph {3}{W}
 *
 * Covers: CanBlockAnyNumber (one blocker assigned to multiple attackers),
 *         combat damage from multiple blocked attackers.
 */
test.describe('Ironfist Crusher', () => {
  test('blocks two attackers at once and survives', async ({ createGame }) => {
    const { player1, player2 } = await createGame({
      player1Name: 'Attacker',
      player2Name: 'Defender',
      player1: {
        battlefield: [
          { name: 'Glory Seeker', tapped: false, summoningSickness: false },
          { name: 'Devoted Hero', tapped: false, summoningSickness: false },
        ],
        library: ['Mountain'],
      },
      player2: {
        battlefield: [{ name: 'Ironfist Crusher' }],
        library: ['Plains'],
      },
      phase: 'PRECOMBAT_MAIN',
      activePlayer: 1,
    })

    const p1 = player1.gamePage
    const p2 = player2.gamePage

    // Advance to combat
    await p1.pass()
    await p1.attackAll()

    // Ironfist Crusher blocks both attackers
    await p2.declareBlocker('Ironfist Crusher', 'Glory Seeker')
    await p2.declareBlocker('Ironfist Crusher', 'Devoted Hero')
    await p2.confirmBlockers()

    // Combat damage: Glory Seeker (2/2) + Devoted Hero (1/2) = 3 damage to Ironfist Crusher (2/4)
    // Ironfist Crusher survives with 1 toughness remaining
    await p2.expectOnBattlefield('Ironfist Crusher')

    // No damage to defending player — all attackers were blocked
    await p1.expectLifeTotal(player2.playerId, 20)

    await p1.screenshot('End state')
  })

  test('blocks three attackers and dies to combined damage', async ({ createGame }) => {
    const { player1, player2 } = await createGame({
      player1Name: 'Attacker',
      player2Name: 'Defender',
      player1: {
        battlefield: [
          { name: 'Glory Seeker', tapped: false, summoningSickness: false },
          { name: 'Grizzly Bears', tapped: false, summoningSickness: false },
          { name: 'Devoted Hero', tapped: false, summoningSickness: false },
        ],
        library: ['Mountain'],
      },
      player2: {
        battlefield: [{ name: 'Ironfist Crusher' }],
        library: ['Plains'],
      },
      phase: 'PRECOMBAT_MAIN',
      activePlayer: 1,
    })

    const p1 = player1.gamePage
    const p2 = player2.gamePage

    // Advance to combat
    await p1.pass()
    await p1.attackAll()

    // Ironfist Crusher blocks all three attackers
    await p2.declareBlocker('Ironfist Crusher', 'Glory Seeker')
    await p2.declareBlocker('Ironfist Crusher', 'Grizzly Bears')
    await p2.declareBlocker('Ironfist Crusher', 'Devoted Hero')
    await p2.confirmBlockers()

    // Combat damage: Glory Seeker (2) + Grizzly Bears (2) + Devoted Hero (1) = 5 damage
    // Ironfist Crusher (2/4) takes 5 damage and dies
    await p2.expectNotOnBattlefield('Ironfist Crusher')

    // No damage to defending player — all attackers were blocked
    await p1.expectLifeTotal(player2.playerId, 20)

    await p1.screenshot('End state')
  })
})
