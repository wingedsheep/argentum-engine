import { test, expect } from '../../fixtures/scenarioFixture'

test.setTimeout(120_000)
test.use({ channel: 'chrome', viewport: { width: 1440, height: 1000 }, deviceScaleFactor: 2 })

test('Jade Monolith targets a creature then chooses a damage source on the battlefield', async ({ createGame }) => {
  const { player1, player2, response } = await createGame({
    player1: {
      battlefield: [{ name: 'Jade Monolith' }, { name: 'Grizzly Bears' }, { name: 'Forest' }],
      library: ['Forest', 'Forest'],
    },
    player2: { battlefield: [{ name: 'Prodigal Sorcerer' }], library: ['Mountain', 'Mountain'] },
    phase: 'PRECOMBAT_MAIN', activePlayer: 1, priorityPlayer: 1,
    player1StopAtSteps: ['PRECOMBAT_MAIN'], player2OpponentStopAtSteps: ['PRECOMBAT_MAIN'],
  })
  await player1.gamePage.clickCard('Jade Monolith')
  await player1.gamePage.selectAction('The next time a source')
  await player1.gamePage.selectTarget('Grizzly Bears')
  await player1.gamePage.confirmTargets()
  await player2.gamePage.resolveStack('Jade Monolith ability')
  await expect(player1.page.getByText('Choose a source of damage', { exact: true })).toBeVisible()
  await player1.gamePage.selectTarget('Prodigal Sorcerer')
  await player1.page.getByRole('button', { name: /Confirm/ }).click()
  await player1.gamePage.screenshot('Chosen Sorcerer redirects its next damage from Grizzly Bears')
  await player1.gamePage.pass()
  await player2.gamePage.clickCard('Prodigal Sorcerer')
  await player2.gamePage.selectAction('damage')
  await player2.gamePage.selectTarget('Grizzly Bears')
  await player2.gamePage.confirmTargets()
  await player1.gamePage.resolveStack('Prodigal Sorcerer ability')
  await player1.gamePage.expectLifeTotal(response.player1.playerId, 19)
  await player1.gamePage.expectOnBattlefield('Grizzly Bears')
})
