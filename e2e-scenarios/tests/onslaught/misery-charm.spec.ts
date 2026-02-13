import { test, expect } from '../../fixtures/scenarioFixture'

/**
 * E2E browser tests for Misery Charm.
 *
 * Card: Misery Charm ({B}) — Instant
 * "Choose one —
 *  • Destroy target Cleric.
 *  • Return target Cleric card from your graveyard to your hand.
 *  • Target player loses 2 life."
 *
 * Covers: ChooseOptionDecisionUI (modal spell mode selection)
 */
test.describe('Misery Charm', () => {
  test('mode: target player loses 2 life', async ({ createGame }) => {
    const { player1, player2 } = await createGame({
      player1Name: 'Caster',
      player2Name: 'Opponent',
      player1: {
        hand: ['Misery Charm'],
        battlefield: [{ name: 'Swamp' }],
      },
      player2: {},
      phase: 'PRECOMBAT_MAIN',
      activePlayer: 1,
    })

    const p1 = player1.gamePage
    const p2 = player2.gamePage

    // Cast Misery Charm
    await p1.clickCard('Misery Charm')
    await p1.selectAction('Cast Misery Charm')

    // Opponent resolves the spell
    await p2.resolveStack('Misery Charm')

    // Mode selection overlay appears — choose "Target player loses 2 life"
    await p1.selectOption('Target player loses 2 life')

    // Target selection — click opponent's life display
    await p1.selectPlayer(player2.playerId)

    // Opponent loses 2 life
    await p1.expectLifeTotal(player2.playerId, 18)

    await p1.screenshot('End state')
  })

  test('mode: destroy target Cleric', async ({ createGame }) => {
    const { player1, player2 } = await createGame({
      player1Name: 'Caster',
      player2Name: 'Opponent',
      player1: {
        hand: ['Misery Charm'],
        battlefield: [{ name: 'Swamp' }],
      },
      player2: {
        // Nova Cleric on battlefield but no mana to activate ability → auto-pass
        battlefield: [{ name: 'Nova Cleric' }],
      },
      phase: 'PRECOMBAT_MAIN',
      activePlayer: 1,
    })

    const p1 = player1.gamePage
    const p2 = player2.gamePage

    // Cast Misery Charm
    await p1.clickCard('Misery Charm')
    await p1.selectAction('Cast Misery Charm')

    // Opponent resolves (has a permanent on battlefield)
    await p2.resolveStack('Misery Charm')

    // Mode selection overlay appears — choose "Destroy target Cleric"
    await p1.selectOption('Destroy target Cleric')

    // Select the single valid Cleric target
    await p1.selectTarget('Nova Cleric')
    await p1.confirmTargets()

    await p1.expectNotOnBattlefield('Nova Cleric')

    await p1.screenshot('End state')
  })
})
