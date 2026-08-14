import { test, expect } from '../../fixtures/scenarioFixture'

/**
 * E2E browser tests for the gift mechanic's timing (Bloomburrow, CR 702.174a).
 *
 * The gift is promised **as you cast** — so the choice has to be a cast option in the action menu,
 * never a question after the card has been played. Kitnap (a gift permanent) used to ask at
 * resolution, once the Aura had already entered the battlefield.
 */
test.describe('Gift promised at cast time', () => {
  test('Kitnap offers the gift as a cast option and asks nothing afterwards', async ({ createGame }) => {
    const { player1, player2 } = await createGame({
      player1Name: 'Caster',
      player2Name: 'Opponent',
      player1: {
        hand: ['Kitnap'],
        battlefield: [{ name: 'Island' }, { name: 'Island' }, { name: 'Island' }, { name: 'Island' }],
        library: ['Island'],
      },
      player2: {
        battlefield: [{ name: 'Grizzly Bears' }],
        library: ['Forest', 'Forest'],
      },
      phase: 'PRECOMBAT_MAIN',
      activePlayer: 1,
    })

    const p1 = player1.gamePage
    const p2 = player2.gamePage

    // The action menu must present both casts: with and without the promise.
    await p1.clickCard('Kitnap')
    await expect(p1.page.locator('button').filter({ hasText: 'Cast Kitnap (Gift a card)' })).toBeVisible()
    await p1.screenshot('Cast options include the gift promise')

    await p1.selectAction('Cast Kitnap (Gift a card)')
    await p1.selectTarget('Grizzly Bears')
    await p1.confirmTargets()
    await p2.resolveStack('Kitnap')

    // Once the Aura is on the battlefield the promise is already locked in: the gift arrives as a
    // triggered ability (CR 702.174b), and the player is asked nothing at all.
    await p1.expectOnBattlefield('Kitnap')
    await expect(p1.page.getByText('Kitnap trigger')).toBeVisible()
    await expect(p1.page.locator('button').filter({ hasText: /gift/i })).toHaveCount(0)
    await p1.screenshot('Aura entered, gift already promised, nothing asked')
  })

  test("Long River's Pull picks its gift among the cast options too", async ({ createGame }) => {
    const { player1, player2 } = await createGame({
      player1Name: 'Caster',
      player2Name: 'Opponent',
      player1: {
        hand: ["Long River's Pull"],
        battlefield: [{ name: 'Island' }, { name: 'Island' }],
      },
      player2: {
        hand: ['Grizzly Bears'],
        battlefield: [{ name: 'Forest' }, { name: 'Forest' }],
        library: ['Forest'],
      },
      phase: 'PRECOMBAT_MAIN',
      activePlayer: 2,
    })

    const p1 = player1.gamePage
    const p2 = player2.gamePage

    await p2.clickCard('Grizzly Bears')
    await p2.selectAction('Cast Grizzly Bears')

    // An instant with gift folds the promise into its cast-time mode choice (both modes shown).
    await p1.clickCard("Long River's Pull")
    await expect(
      p1.page.locator('button').filter({ hasText: 'Counter target creature spell' })
    ).toBeVisible()
    await expect(
      p1.page.locator('button').filter({ hasText: 'Gift a card — counter target spell' })
    ).toBeVisible()
    await p1.screenshot('Both gift modes offered while casting')

    await p1.selectAction('Gift a card — counter target spell')
    await p1.selectTarget('Grizzly Bears')
    await p1.confirmTargets()
    await p2.resolveStack("Long River's Pull")

    await p1.expectNotOnBattlefield('Grizzly Bears')
    await p2.expectHandSize(1) // the promised card
  })
})
