import { test, expect } from '../../fixtures/scenarioFixture'

test('simultaneous graveyard entries use top-first labels and accept owner ordering', async ({ createGame }) => {
  test.setTimeout(120_000)
  const { player1, player2 } = await createGame({
    player1: {
      hand: ['Wrath of God'],
      battlefield: [{ name: 'Plains' }, { name: 'Plains' }, { name: 'Plains' }, { name: 'Plains' },
        { name: 'Grizzly Bears' }, { name: 'Llanowar Elves' }],
      graveyard: ['Nether Shadow'],
      library: ['Plains', 'Plains'],
    },
    player2: { library: ['Forest', 'Forest'] },
    phase: 'PRECOMBAT_MAIN', activePlayer: 1,
  })
  await player1.gamePage.clickCard('Wrath of God')
  await player1.gamePage.selectAction('Cast')
  await player2.gamePage.pass()
  await expect(player1.page.getByRole('heading', { name: 'Order Graveyard' })).toBeVisible()
  await expect(player1.page.getByText('TOP OF GRAVEYARD', { exact: true })).toBeVisible()
  await expect(player1.page.getByText('BOTTOM OF NEW CARDS', { exact: true })).toBeVisible()
  await expect(player1.page.getByText('Order the cards entering your graveyard (top card first)', { exact: true })).toBeVisible()
  await player1.page.getByTitle('Move later in the order').first().click()
  await player1.page.getByRole('button', { name: 'Confirm Order', exact: true }).click()
  await expect(player1.page.getByRole('heading', { name: 'Order Graveyard' })).toBeHidden()
})
