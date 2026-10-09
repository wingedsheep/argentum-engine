import { test } from '../../fixtures/scenarioFixture'

test('The Fall of Kroog chooses an opponent before their land', async ({ createGame }) => {
  test.setTimeout(60_000)
  const { player1, player2 } = await createGame({
    player1Name: 'Caster', player2Name: 'Defender',
    player1: { hand: ['The Fall of Kroog'], battlefield: Array.from({ length: 6 }, () => ({ name: 'Mountain' })), library: ['Mountain', 'Mountain', 'Mountain'] },
    player2: { battlefield: [{ name: 'Forest' }, { name: 'Llanowar Elves' }], library: ['Forest', 'Forest', 'Forest'] },
    phase: 'PRECOMBAT_MAIN', activePlayer: 1,
  })
  const p1 = player1.gamePage
  await p1.clickCard('The Fall of Kroog')
  await p1.selectAction('Cast The Fall of Kroog')
  await p1.selectPlayer(player2.playerId)
  await p1.confirmTargets()
  await player1.page.screenshot({ path: test.info().outputPath('dependent-land-target.png') })
  await p1.clickCard('Forest')
  await p1.confirmTargets()
  await player2.gamePage.resolveStack('The Fall of Kroog')
  await p1.expectLifeTotal(player2.playerId, 17)
  await p1.expectNotOnBattlefield('Forest')
  await p1.expectNotOnBattlefield('Llanowar Elves')
})
