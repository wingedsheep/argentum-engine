import { test, expect } from '../../fixtures/scenarioFixture'
import { BATTLEFIELD, cardByName } from '../../helpers/selectors'

test('Possessed Goat swaps to demon art after activating', async ({ createGame }) => {
  const { player1, player2 } = await createGame({
    player1Name: 'Goat Herder',
    player2Name: 'Concerned Opponent',
    player1: {
      hand: ['Grizzly Bears'],
      battlefield: [
        { name: 'Possessed Goat' },
        { name: 'Plains' },
        { name: 'Plains' },
        { name: 'Plains' },
      ],
      library: ['Plains', 'Forest'],
    },
    player2: {
      library: ['Mountain', 'Forest'],
    },
    phase: 'PRECOMBAT_MAIN',
    activePlayer: 1,
    priorityPlayer: 1,
  })

  const p1 = player1.gamePage
  const p2 = player2.gamePage

  await p1.clickCard('Possessed Goat')
  await p1.selectAction('Put three +1/+1 counters')
  await p1.selectCardInHand('Grizzly Bears')
  await p1.confirmTargets()
  await p2.resolveStack('Possessed Goat ability')

  const goatImage = p1.page
    .locator(BATTLEFIELD)
    .locator(cardByName('Possessed Goat'))
    .first()
  await expect(goatImage).toHaveAttribute('src', /possessed-goat\.jpeg/)

  await goatImage.click()
  await p1.page.waitForTimeout(500)
  await p1.screenshot('Possessed state')
})
