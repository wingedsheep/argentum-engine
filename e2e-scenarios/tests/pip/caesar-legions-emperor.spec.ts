import { test, expect } from '../../fixtures/scenarioFixture'

/**
 * Caesar, Legion's Emperor — "Whenever you attack, you may sacrifice another creature. When you do,
 * choose two — • Create two 1/1 red and white Soldier creature tokens with haste that are tapped and
 * attacking. • You draw a card and you lose 1 life. • Caesar deals damage equal to the number of
 * creature tokens you control to target opponent."
 *
 * The Soldiers say only "tapped and attacking", so CR 508.4 lets Caesar's controller choose, for each
 * token as it enters, the player or planeswalker it attacks: two questions, one per token, before
 * either token exists.
 */
test("Caesar's Soldiers each attack a defender their controller chooses", async ({ createGame }) => {
  test.setTimeout(120_000)
  const { player1, player2 } = await createGame({
    player1Name: 'Caesar', player2Name: 'Opponent',
    player1: {
      battlefield: [
        { name: "Caesar, Legion's Emperor", summoningSickness: false },
        { name: 'Grizzly Bears' },
      ],
      library: ['Plains', 'Plains', 'Plains', 'Plains', 'Plains'],
    },
    player2: {
      battlefield: [{ name: 'Jace Beleren', counters: { LOYALTY: 3 } }],
      library: ['Island', 'Island', 'Island', 'Island', 'Island'],
    },
    phase: 'PRECOMBAT_MAIN', activePlayer: 1, priorityPlayer: 1,
  })
  const p1 = player1.gamePage
  const p2 = player2.gamePage
  const page = player1.page

  // To combat; Caesar attacks the opponent.
  await p1.pass()
  await p1.attackWith("Caesar, Legion's Emperor")

  // "Whenever you attack" — the opponent lets it resolve; sacrifice the Bears.
  await p2.resolveStack("Caesar, Legion's Emperor trigger")
  await p1.answerYes()

  // "When you do, choose two" — the Soldiers and the damage, aimed at the opponent.
  await p1.selectOption('Create two')
  await p1.selectOption('deals damage')
  await p1.selectPlayer(player2.playerId)
  await p2.resolveStack("Caesar, Legion's Emperor trigger")

  // CR 508.4: the first Soldier's defender — the opponent or their planeswalker.
  await expect(page.getByText(/player, planeswalker or battle for Soldier Token 1 of 2 to attack/))
    .toBeVisible({ timeout: 10_000 })
  await p1.screenshot('Defender choice for the first Soldier: opponent or Jace Beleren')
  await p1.selectTarget('Jace Beleren')
  await p1.confirmTargets()

  // The second Soldier gets its own question.
  await expect(page.getByText(/for Soldier Token 2 of 2 to attack/)).toBeVisible({ timeout: 10_000 })
  await p1.screenshot('Defender choice for the second Soldier')
  await p1.selectPlayer(player2.playerId)

  // Both Soldiers are on the battlefield, tapped. With no further actions, combat runs on: the
  // damage mode (2 tokens) plus Caesar (4) and one Soldier (1) take the opponent from 20 to 13,
  // and the other Soldier hits Jace (3 -> 2).
  await p1.expectOnBattlefield('Soldier Token')
  await p1.expectTapped('Soldier Token')
  await p1.expectLifeTotal(player2.playerId, 13)
  await p1.screenshot('After combat: opponent at 13, Jace Beleren at 2 loyalty')
})
