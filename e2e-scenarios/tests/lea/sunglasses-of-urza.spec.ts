import { test, expect } from '../../fixtures/scenarioFixture'

test.setTimeout(120_000)
test.use({ channel: 'chrome', viewport: { width: 1440, height: 1000 }, deviceScaleFactor: 2 })

test('Sunglasses credits a Plains in the red activated-ability payment readout', async ({ createGame }) => {
  const { player1, player2 } = await createGame({
    player1: {
      battlefield: [{ name: 'Sunglasses of Urza' }, { name: 'Shivan Dragon' }, { name: 'Plains' }],
      library: ['Plains', 'Plains'],
    },
    player2: { library: ['Mountain', 'Mountain'] },
    phase: 'PRECOMBAT_MAIN', activePlayer: 1, player1StopAtSteps: ['PRECOMBAT_MAIN'],
  })
  await player1.gamePage.clickCard('Shivan Dragon')
  await player1.page.getByTitle('Choose which lands to tap', { exact: true }).click()
  await expect(player1.page.getByText('1/1', { exact: true })).toBeVisible()
  await player1.gamePage.screenshot('White source covers the red pip')
  await player1.page.screenshot({ path: '/tmp/loop-lea-g8-mana-readout.png' })
  await player1.page.getByRole('button', { name: 'Confirm', exact: true }).click()
  await player2.gamePage.pass()
  await player1.gamePage.expectStats('Shivan Dragon', '6/5')
  await player1.gamePage.expectTapped('Plains')
})

test('Sunglasses lets white sources cover a red upkeep payment in the decision banner', async ({ createGame }) => {
  const { player1, player2 } = await createGame({
    player1: {
      battlefield: [{ name: 'Sunglasses of Urza' }, { name: 'Palladia-Mors' },
        { name: 'Plains' }, { name: 'Plains' }, { name: 'Forest' }],
      library: ['Plains', 'Plains'],
    },
    player2: { library: ['Mountain', 'Mountain'] },
    phase: 'ENDING', step: 'END', activePlayer: 2, priorityPlayer: 2,
    player1StopAtSteps: ['UPKEEP'], player2StopAtSteps: ['END'],
  })
  await player2.gamePage.pass()
  await player2.gamePage.resolveStack('Palladia-Mors trigger')
  await player1.page.getByRole('button', { name: /^Pay/ }).click()
  const pay = player1.page.getByRole('button', { name: 'Pay (3)', exact: true })
  await expect(pay).toBeEnabled()
  await expect(player1.page.getByText('Cost covered — press Pay.', { exact: true })).toBeVisible()
  await player1.gamePage.screenshot('White sources cover red and white in the upkeep payment')
  await player1.page.screenshot({ path: '/tmp/loop-lea-g8-upkeep-payment.png' })
  await pay.click()
  await player1.gamePage.expectOnBattlefield('Palladia-Mors')
  await expect(pay).not.toBeVisible()
})
