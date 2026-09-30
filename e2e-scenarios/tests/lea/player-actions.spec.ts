import { test, expect } from '../../fixtures/scenarioFixture'

test.setTimeout(90_000)

test.use({ channel: 'chrome', viewport: { width: 1440, height: 1000 }, deviceScaleFactor: 2 })

test('Channel offers repeatable player actions after resolving and only to its controller', async ({ createGame }) => {
  const { player1, player2 } = await createGame({
    player1: { lifeTotal: 20, hand: ['Channel', 'Rod of Ruin'], battlefield: [{ name: 'Forest' }, { name: 'Forest' }], library: ['Forest', 'Forest'] },
    player2: { lifeTotal: 20, hand: [], battlefield: [], library: ['Forest', 'Forest'] },
    phase: 'PRECOMBAT_MAIN', activePlayer: 1,
    player1StopAtSteps: ['PRECOMBAT_MAIN'], player2StopAtSteps: [],
    player1OpponentStopAtSteps: [], player2OpponentStopAtSteps: [],
  })
  await player1.gamePage.clickCard('Channel')
  await player1.gamePage.selectAction('Cast')
  await player2.gamePage.pass()
  const action = player1.page.getByRole('button', { name: 'Channel: Pay 1 life to add {C}', exact: true })
  await expect(action).toBeVisible()
  await expect(player2.page.getByRole('button', { name: 'Channel: Pay 1 life to add {C}', exact: true })).toHaveCount(0)
  await action.click()
  await player1.gamePage.expectLifeTotal(player1.playerId, 19)
  await expect(action).toBeEnabled()
  await action.click()
  await player1.gamePage.expectLifeTotal(player1.playerId, 18)
  await player1.gamePage.screenshot('Repeated Channel actions')
  await player1.page.waitForTimeout(800) // Let the life-change animation settle for the PR screenshot.
  await player1.page.screenshot({ path: '/tmp/lea-player-actions.png' })
  await player1.gamePage.clickCard('Rod of Ruin')
  await player1.gamePage.selectAction('Cast')
  await expect(player1.page.getByText('Produce mana for Rod of Ruin')).toBeVisible()
  await expect(action).toBeEnabled()
  await action.click()
  await player1.gamePage.expectLifeTotal(player1.playerId, 17)
  await action.click()
  await player1.gamePage.expectLifeTotal(player1.playerId, 16)
  await player1.page.getByRole('button', { name: 'Pay', exact: true }).click()
  await expect(player1.page.locator('[data-zone="player-battlefield"] img[alt="Rod of Ruin"]')).toBeVisible()
})
