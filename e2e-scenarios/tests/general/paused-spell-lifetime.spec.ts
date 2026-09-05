import { test, expect } from '../../fixtures/scenarioFixture'
import { cardByName } from '../../helpers/selectors'

test.use({ channel: 'chrome' })

test('Careful Study stays on both stacks through dredge and discard choices', async ({ createGame }) => {
  const { player1, player2 } = await createGame({
    player1Name: 'Study caster',
    player2Name: 'Opponent',
    player1: {
      hand: ['Careful Study', 'Grizzly Bears'],
      battlefield: [{ name: 'Island' }],
      graveyard: ['Greater Mossdog'],
      library: ['Forest', 'Island', 'Mountain', 'Swamp', 'Plains', 'Forest', 'Island'],
    },
    player2: {
      hand: ['Counterspell'],
      battlefield: [{ name: 'Island' }, { name: 'Island' }],
      library: ['Forest', 'Island'],
    },
    phase: 'PRECOMBAT_MAIN',
    activePlayer: 1,
  })
  const p1 = player1.gamePage
  const p2 = player2.gamePage
  const stacks = [player1.page, player2.page].map(page =>
    page.locator('[data-learn="stack"]').locator(cardByName('Careful Study')))

  await p1.clickCard('Careful Study')
  await p1.selectAction('Cast Careful Study')
  for (const stack of stacks) await expect(stack).toHaveCount(1)
  await p2.resolveStack('Careful Study')

  await expect(player1.page.getByRole('heading', {
    name: 'Dredge 3 — Mill 3 cards and return Greater Mossdog from your graveyard to your hand instead of drawing?',
    exact: true,
  })).toBeVisible()
  for (const stack of stacks) await expect(stack).toHaveCount(1)
  await p1.expectGraveyardSize(player1.playerId, 1)
  await p2.screenshot('Resolving Study remains visible during dredge choice')
  await p1.answerYes()
  await p2.dismissRevealedCards()

  await p1.selectCardInDecision('Greater Mossdog')
  for (const stack of stacks) await expect(stack).toHaveCount(1)
  await p1.expectGraveyardSize(player1.playerId, 3)
  await p1.screenshot('Study remains on stack during mandatory discard')
  await p1.selectCardInDecision('Grizzly Bears')
  await p1.confirmTargets()

  for (const stack of stacks) await expect(stack).toHaveCount(0)
  await p1.expectGraveyardSize(player1.playerId, 6)
  await p1.expectHandSize(1)
  await p1.expectNotInHand('Greater Mossdog')
  await p1.expectNotInHand('Grizzly Bears')
  await p1.screenshot('Study completed after both resolving choices')
  await p2.screenshot('Opponent sees completed stack')
})
