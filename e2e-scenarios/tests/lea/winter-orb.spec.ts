import { test, expect } from '../../fixtures/scenarioFixture'

test.setTimeout(120_000)
test.use({ channel: 'chrome', viewport: { width: 1440, height: 1000 } })

test('Winter Orb and Damping Field keep a rejected selection editable', async ({ createGame }) => {
  const { player1, player2 } = await createGame({
    player1: { battlefield: [{ name: 'Winter Orb' }, { name: 'Damping Field' }], library: ['Forest', 'Forest'] },
    player2: {
      battlefield: ['Plains', 'Island', 'Forest', 'Sol Ring', 'Icy Manipulator'].map(name => ({ name, tapped: true })),
      library: ['Forest', 'Forest'],
    },
    phase: 'ENDING', step: 'END', activePlayer: 1, priorityPlayer: 1,
    player1StopAtSteps: ['END'], player2StopAtSteps: ['UPKEEP'],
    player1OpponentStopAtSteps: ['UPKEEP'], player2OpponentStopAtSteps: [],
  })
  player1.page.setDefaultTimeout(15_000)
  player2.page.setDefaultTimeout(15_000)
  await player1.page.getByRole('button', { name: "To opponent's turn", exact: true }).click()
  await player2.page.getByRole('button', { name: 'To my turn', exact: true }).click()
  const p = player2.page
  await expect(p.getByText('Select permanents to keep tapped', { exact: true })).toBeVisible()
  await player2.gamePage.clickCard('Plains')
  await player2.gamePage.clickCard('Island')
  await p.getByRole('button', { name: 'Confirm (2)', exact: true }).click()
  await expect(p.getByText(/At most 1 of the restricted permanents may untap/)).toBeVisible()
  await expect(p.getByRole('button', { name: 'Confirm (2)', exact: true })).toBeVisible()
  await player2.gamePage.screenshot('Rejected untap selection remains editable')
  await player2.gamePage.clickCard('Sol Ring')
  await p.getByRole('button', { name: 'Confirm (3)', exact: true }).click()
  await expect(p.getByText('Select permanents to keep tapped', { exact: true })).toHaveCount(0)
  await player2.gamePage.expectTapped('Plains')
  await player2.gamePage.expectTapped('Island')
  await player2.gamePage.expectTapped('Sol Ring')
  await player2.gamePage.expectUntapped('Forest')
  await player2.gamePage.expectUntapped('Icy Manipulator')
})
