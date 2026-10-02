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


test('a long graveyard order keeps both ends reachable on a narrow screen', async ({ createGame }, testInfo) => {
  test.setTimeout(120_000)
  const { player1, player2 } = await createGame({
    player1: {
      hand: ['Wrath of God'],
      battlefield: [
        ...Array.from({ length: 4 }, () => ({ name: 'Plains' })),
        ...Array.from({ length: 11 }, () => ({ name: 'Grizzly Bears' })),
        { name: 'Llanowar Elves' },
      ],
      graveyard: ['Nether Shadow'],
      library: ['Plains', 'Plains'],
    },
    player2: { library: ['Forest', 'Forest'] },
    phase: 'PRECOMBAT_MAIN', activePlayer: 1,
  })
  await player1.page.setViewportSize({ width: 390, height: 844 })
  await player1.gamePage.clickCard('Wrath of God')
  await player1.gamePage.selectAction('Cast')
  await player2.gamePage.pass()
  await expect(player1.page.getByRole('heading', { name: 'Order Graveyard' })).toBeVisible()

  const cards = player1.page.getByRole('region', { name: 'Cards to order' })
  await expect(cards.getByTitle('Move earlier in the order')).toHaveCount(12)
  await expect.poll(() => cards.evaluate(element => element.scrollWidth > element.clientWidth)).toBe(true)
  const bounds = await cards.boundingBox()
  expect(bounds).not.toBeNull()
  expect(bounds!.x).toBeGreaterThanOrEqual(0)
  expect(bounds!.x + bounds!.width).toBeLessThanOrEqual(390)

  const lastCard = cards.locator(':scope > div').filter({ has: player1.page.getByRole('img', { name: 'Llanowar Elves', exact: true }) })
  const moveEarlier = lastCard.getByTitle('Move earlier in the order')
  await moveEarlier.scrollIntoViewIfNeeded()
  await expect(moveEarlier).toBeInViewport()
  await moveEarlier.click()
  await expect(lastCard).toContainText('11th')

  const firstMoveLater = cards.getByTitle('Move later in the order').first()
  await firstMoveLater.scrollIntoViewIfNeeded()
  await expect(firstMoveLater).toBeInViewport()
  await firstMoveLater.click()
  await testInfo.attach('graveyard-order-narrow-screen', {
    body: await player1.page.screenshot(),
    contentType: 'image/png',
  })
  await player1.page.getByRole('button', { name: 'Confirm Order', exact: true }).click()
  await expect(player1.page.getByRole('heading', { name: 'Order Graveyard' })).toBeHidden()
})
