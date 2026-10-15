import { test, expect } from '../../fixtures/scenarioFixture'

/**
 * E2E coverage for blitz (Streets of New Capenna, CR 702.152).
 *
 * A blitz card in hand offers its blitz cast beside the normal cast; the blitzed permanent shows
 * the "Blitzed" cue on the battlefield.
 */
test.describe('Blitz', () => {
  test('Riveteers Requisitioner offers a blitz cast and enters blitzed', async ({ createGame }) => {
    const { player1, player2 } = await createGame({
      player1Name: 'Riveteer',
      player2Name: 'Opponent',
      player1: {
        hand: ['Riveteers Requisitioner'],
        battlefield: [{ name: 'Mountain' }, { name: 'Mountain' }, { name: 'Mountain' }],
        library: ['Mountain', 'Mountain'],
      },
      player2: {
        library: ['Forest', 'Forest'],
      },
      phase: 'PRECOMBAT_MAIN',
      activePlayer: 1,
    })

    const p1 = player1.gamePage
    const p2 = player2.gamePage

    await p1.clickCard('Riveteers Requisitioner')
    await expect(p1.page.locator('button').filter({ hasText: 'Cast Riveteers Requisitioner (Blitz)' })).toBeVisible()
    await p1.screenshot('Cast options include blitz')

    await p1.selectAction('Cast Riveteers Requisitioner (Blitz)')
    // The opponent has no responses, so the spell resolves on its own.

    await p1.expectOnBattlefield('Riveteers Requisitioner')
    await p1.expectBadge('Riveteers Requisitioner', 'Blitzed')
    await p1.screenshot('Blitzed permanent on the battlefield')
    await p2.expectBadge('Riveteers Requisitioner', 'Blitzed')
    await p2.screenshot('Opponent sees the Blitzed cue')
  })
})
