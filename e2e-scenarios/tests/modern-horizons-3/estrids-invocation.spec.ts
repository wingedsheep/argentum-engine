import { test, expect } from '../../fixtures/scenarioFixture'

test("Estrid's Invocation chooses a copy and then an Aura host on the battlefield", async ({ createGame }) => {
  const { player1 } = await createGame({
    player1Name: 'Copier',
    player2Name: 'Opponent',
    player1: {
      hand: ["Estrid's Invocation"],
      battlefield: [
        { name: 'Island' }, { name: 'Island' }, { name: 'Island' },
        { name: 'Grizzly Bears' },
        { name: 'Holy Strength', attachedTo: 'Grizzly Bears' },
      ],
      library: ['Island', 'Island', 'Island'],
    },
    player2: {
      battlefield: [{ name: 'Hill Giant' }],
      library: ['Forest', 'Forest', 'Forest'],
    },
    phase: 'PRECOMBAT_MAIN',
    activePlayer: 1,
  })

  const p1 = player1.gamePage
  await p1.clickCard("Estrid's Invocation")
  await p1.selectAction("Cast Estrid's Invocation")
  await p1.waitForDecision()
  // Attached Auras expose their top edge above the host card.
  await player1.page.locator('img[alt="Holy Strength"]').first().click({ position: { x: 25, y: 5 } })
  await p1.confirmTargets()
  await expect(player1.page.getByText('what Holy Strength enchants', { exact: true })).toBeVisible()
  await p1.selectTarget('Hill Giant')
  await p1.confirmTargets()
  await p1.expectStats('Hill Giant', '4/5')
  await p1.expectStats('Grizzly Bears', '3/4')
  await p1.expectNotInHand("Estrid's Invocation")
  await p1.screenshot('Copied Aura attached to a different host')
})
