import { test, expect } from '../../fixtures/scenarioFixture'

test.describe('Sunfire Balm - damage prevention choice', () => {
  test('defender distributes prevention among multiple attackers', async ({ createGame }) => {
    const { player1, player2 } = await createGame({
      player1Name: 'Defender',
      player2Name: 'Attacker',
      player1: {
        hand: ['Sunfire Balm'],
        battlefield: [
          { name: 'Plains' },
          { name: 'Plains' },
          { name: 'Plains' },
        ],
        library: ['Mountain'],
      },
      player2: {
        battlefield: [
          { name: 'Headhunter', tapped: false, summoningSickness: false },
          { name: 'Anurid Murkdiver', tapped: false, summoningSickness: false },
        ],
        library: ['Mountain'],
      },
      phase: 'PRECOMBAT_MAIN',
      activePlayer: 2,
    })

    const p1 = player1.gamePage
    const p2 = player2.gamePage

    // P2 advances to declare attackers
    await p2.pass()

    // P2 attacks with both creatures
    await p2.attackAll()

    // P1 gets priority after attackers declared — cast Sunfire Balm targeting self
    await p1.clickCard('Sunfire Balm')
    await p1.selectAction('Cast Sunfire Balm')
    await p1.selectPlayer(player1.playerId)
    await p1.confirmTargets()

    // P2 resolves the spell
    await p2.resolveStack('Sunfire Balm')

    // No blocks — P1 has no creatures, so auto-advances to combat damage step
    // P1 receives 5 total damage (1 from Headhunter + 4 from Murkdiver)
    // but has a "prevent next 4" shield — needs to choose distribution

    // Wait for the prevention distribute mode to appear
    await p1.page.getByRole('button', { name: 'Confirm Prevention' }).waitFor({ state: 'visible', timeout: 10_000 })

    // Allocate prevention: 1 to Headhunter (prevent all its 1 damage) and 3 to Anurid Murkdiver
    await p1.allocateDamage('Headhunter', 1)
    await p1.allocateDamage('Anurid Murkdiver', 3)
    await p1.page.getByRole('button', { name: 'Confirm Prevention' }).click()

    // P1 should take only 1 damage (4 from Murkdiver - 3 prevented = 1)
    // Headhunter's 1 damage fully prevented, so no "deals combat damage to a player" trigger
    await p1.expectLifeTotal(player1.playerId, 19)

    // Headhunter's triggered ability should NOT fire (0 combat damage dealt)
    // so no discard decision — hand should be empty (Sunfire Balm was cast)
    await p1.expectHandSize(0)

    await p1.screenshot('End state')
  })
})
