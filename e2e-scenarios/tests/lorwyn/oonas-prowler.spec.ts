import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { test, expect } from '../../fixtures/scenarioFixture'
import type { ScenarioRequest } from '../../helpers/scenarioApi'
import { OPPONENT_BATTLEFIELD, cardByName } from '../../helpers/selectors'

const scenario: ScenarioRequest = JSON.parse(readFileSync(
  resolve(__dirname, '../../../manual-scenarios/cards/o/oonas-prowler.json'), 'utf8',
))

test("opponent activates Oona's Prowler through its menu and discards from their own hand", async ({ createGame }) => {
  const { player1, player2 } = await createGame(scenario)
  const p1 = player1.gamePage
  const p2 = player2.gamePage
  await p1.expectHandSize(3)
  await p2.expectHandSize(2)
  await p2.expectStats("Oona's Prowler", '3/1')

  await player2.page.locator(OPPONENT_BATTLEFIELD).locator(cardByName("Oona's Prowler")).click()
  await expect(player2.page.getByRole('button').filter({ hasText: 'gets -2/-0 until end of turn' })).toBeVisible()
  await p2.screenshot('Opponent sees server-provided Prowler activation')
  await p2.selectAction('gets -2/-0 until end of turn')
  await p2.selectCardInHand('Forest')
  await p2.screenshot('Opponent chooses their own card to discard')
  await p2.confirmTargets()
  await p2.expectHandSize(1)
  await p1.expectHandSize(3)
  await p2.expectGraveyardSize(player2.playerId, 1)

  await p1.resolveStack("Oona's Prowler ability")
  await p1.expectStats("Oona's Prowler", '1/1')
  await p2.expectStats("Oona's Prowler", '1/1')
  await p1.expectHandSize(3)
  await p2.expectHandSize(1)
  await p1.screenshot('Controller sees resolved 1/1 Prowler')
  await p2.screenshot('Opponent sees resolved 1/1 Prowler and paid discard')
})
