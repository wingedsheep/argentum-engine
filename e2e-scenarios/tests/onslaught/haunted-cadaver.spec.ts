import { test, expect } from '../../fixtures/scenarioFixture'

/**
 * E2E browser tests for Haunted Cadaver.
 *
 * Card: Haunted Cadaver ({3}{B}) — Creature — Zombie (2/2)
 * "Whenever Haunted Cadaver deals combat damage to a player, you may sacrifice it.
 *  If you do, that player discards three cards."
 * Morph {1}{B}
 *
 * Mirrors: HauntedCadaverScenarioTest.kt
 */
test.describe('Haunted Cadaver', () => {
  test('accepting sacrifice causes opponent to discard 3 cards', async ({ createGame }) => {
    const { player1, player2 } = await createGame({
      player1Name: 'Attacker',
      player2Name: 'Defender',
      player1: {
        battlefield: [{ name: 'Haunted Cadaver', tapped: false, summoningSickness: false }],
        library: ['Swamp'],
      },
      player2: {
        hand: ['Swamp', 'Swamp', 'Swamp'],
        library: ['Mountain'],
      },
      phase: 'PRECOMBAT_MAIN',
      activePlayer: 1,
    })

    const p1 = player1.gamePage
    const p2 = player2.gamePage

    // Advance to declare attackers
    await p1.pass()

    // Attack with Haunted Cadaver (P2 has no creatures, blockers auto-skipped)
    await p1.attackWith('Haunted Cadaver')

    // Trigger fires after combat damage — opponent resolves the stack item
    await p2.resolveStack('Haunted Cadaver trigger')

    // P1 chooses yes — sacrifice Haunted Cadaver
    await p1.answerYes()

    // Haunted Cadaver should be sacrificed (no longer on battlefield)
    await p1.expectNotOnBattlefield('Haunted Cadaver')

    // Opponent should have discarded all 3 cards
    await p2.expectHandSize(0)

    // Opponent took 2 combat damage
    await p1.expectLifeTotal(player2.playerId, 18)

    await p1.screenshot('End state — cadaver sacrificed, opponent discarded')
  })

  test('declining sacrifice leaves creature on battlefield', async ({ createGame }) => {
    const { player1, player2 } = await createGame({
      player1Name: 'Attacker',
      player2Name: 'Defender',
      player1: {
        battlefield: [{ name: 'Haunted Cadaver', tapped: false, summoningSickness: false }],
        library: ['Swamp'],
      },
      player2: {
        hand: ['Swamp', 'Swamp', 'Swamp'],
        library: ['Mountain'],
      },
      phase: 'PRECOMBAT_MAIN',
      activePlayer: 1,
    })

    const p1 = player1.gamePage
    const p2 = player2.gamePage

    // Advance to declare attackers
    await p1.pass()

    // Attack with Haunted Cadaver (P2 has no creatures, blockers auto-skipped)
    await p1.attackWith('Haunted Cadaver')

    // Trigger fires after combat damage — opponent resolves the stack item
    await p2.resolveStack('Haunted Cadaver trigger')

    // P1 declines — don't sacrifice
    await p1.answerNo()

    // Haunted Cadaver should still be on the battlefield
    await p1.expectOnBattlefield('Haunted Cadaver')

    // Opponent should still have all 3 cards
    await p2.expectHandSize(3)

    // Opponent took 2 combat damage
    await p1.expectLifeTotal(player2.playerId, 18)

    await p1.screenshot('End state — cadaver stayed, no discard')
  })
})
