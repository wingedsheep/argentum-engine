import { test, expect } from '../../../fixtures/scenarioFixture'

/**
 * E2E browser tests for Scion of Darkness.
 *
 * Card: Scion of Darkness ({5}{B}{B}{B}) — Creature — Avatar (6/6)
 * "Trample
 *  Whenever Scion of Darkness deals combat damage to a player, you may put target
 *  creature card from that player's graveyard onto the battlefield under your control.
 *  Cycling {3}"
 */
test.describe('Scion of Darkness', () => {
  test('accepting puts creature from opponent graveyard onto your battlefield', async ({
    createGame,
  }) => {
    const { player1, player2 } = await createGame({
      player1Name: 'Attacker',
      player2Name: 'Defender',
      player1: {
        battlefield: [{ name: 'Scion of Darkness', tapped: false, summoningSickness: false }],
        library: ['Swamp'],
      },
      player2: {
        graveyard: ['Hill Giant'],
        library: ['Mountain'],
      },
      phase: 'PRECOMBAT_MAIN',
      activePlayer: 1,
    })

    const p1 = player1.gamePage
    const p2 = player2.gamePage

    // Advance to declare attackers
    await p1.pass()

    // Attack with Scion of Darkness (P2 has no creatures, blockers auto-skipped)
    await p1.attackWith('Scion of Darkness')

    // Trigger fires after combat damage — P2 auto-passes (no responses)
    // P1 sees the may question directly
    await p1.answerYes()

    // Select the creature from opponent's graveyard
    await p1.selectCardInZoneOverlay('Hill Giant')
    await p1.confirmTargets()

    // Trigger is now on the stack — opponent resolves
    await p2.resolveStack('Scion of Darkness trigger')

    // Hill Giant should now be on P1's battlefield (under P1's control)
    await p1.expectOnBattlefield('Hill Giant')

    // Opponent took 6 combat damage (trample, no blockers)
    await p1.expectLifeTotal(player2.playerId, 14)

    await p1.screenshot('End state — Hill Giant stolen from graveyard')
  })

  test('declining leaves opponent graveyard intact', async ({ createGame }) => {
    const { player1, player2 } = await createGame({
      player1Name: 'Attacker',
      player2Name: 'Defender',
      player1: {
        battlefield: [{ name: 'Scion of Darkness', tapped: false, summoningSickness: false }],
        library: ['Swamp'],
      },
      player2: {
        graveyard: ['Hill Giant'],
        library: ['Mountain'],
      },
      phase: 'PRECOMBAT_MAIN',
      activePlayer: 1,
    })

    const p1 = player1.gamePage
    const p2 = player2.gamePage

    // Advance to declare attackers
    await p1.pass()

    // Attack with Scion of Darkness
    await p1.attackWith('Scion of Darkness')

    // Trigger fires after combat damage — P2 auto-passes (no responses)
    // P1 declines
    await p1.answerNo()

    // Hill Giant should NOT be on the battlefield
    await p1.expectNotOnBattlefield('Hill Giant')

    // Opponent took 6 combat damage
    await p1.expectLifeTotal(player2.playerId, 14)

    await p1.screenshot('End state — declined, graveyard intact')
  })
})
