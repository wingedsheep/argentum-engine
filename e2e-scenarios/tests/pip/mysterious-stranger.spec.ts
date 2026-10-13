import { test, expect } from '../../fixtures/scenarioFixture'

for (const viewport of [{ width: 1280, height: 800 }, { width: 390, height: 844 }]) {
  test(`Mysterious Stranger requires one card per graveyard at ${viewport.width}px`, async ({ createGame }, testInfo) => {
    test.setTimeout(120_000)
    const { player1, player2 } = await createGame({
      player1Name: 'G10 You', player2Name: 'G10 Opponent',
      player1: {
        hand: ['Mysterious Stranger'], battlefield: Array.from({ length: 4 }, () => ({ name: 'Mountain' })),
        graveyard: ['Lightning Bolt', 'Shock'], library: ['Mountain', 'Mountain', 'Mountain'],
      },
      player2: { graveyard: ['Lightning Bolt', 'Shock'], library: ['Mountain', 'Mountain', 'Mountain'] },
      phase: 'PRECOMBAT_MAIN', activePlayer: 1,
    })
    let phase = 'targeting'
    const keyWarnings: { phase: string; text: string }[] = []
    player1.page.on('console', message => {
      if (message.type() === 'error' && message.text().includes('same key')) keyWarnings.push({ phase, text: message.text() })
    })
    await player1.page.setViewportSize(viewport)
    await player1.gamePage.clickCard('Mysterious Stranger')
    await player1.gamePage.selectAction('Cast')
    const heading = player1.page.getByRole('heading', { name: 'Choose from Graveyard', exact: true })
    if (!(await heading.isVisible())) await player2.gamePage.pass()
    await expect(heading).toBeVisible()
    const confirm = player1.page.getByRole('button', { name: 'Confirm Target', exact: true })
    await expect(confirm).toBeDisabled()
    await player1.gamePage.selectCardInZoneOverlay('Lightning Bolt')
    await expect(player1.page.getByText('Selected: 1 / 2', { exact: true })).toBeVisible()
    await player1.gamePage.selectCardInZoneOverlay('Shock')
    await expect(player1.page.getByText('Selected: 1 / 2', { exact: true })).toBeVisible()
    await expect(confirm).toBeDisabled()
    await player1.page.getByRole('button', { name: /G10 Opponent.*Graveyard/ }).click()
    await player1.gamePage.selectCardInZoneOverlay('Shock')
    await expect(player1.page.getByText('Selected: 2 / 2', { exact: true })).toBeVisible()
    await expect(confirm).toBeEnabled()
    await player1.page.getByRole('button', { name: /Your Graveyard/ }).click()
    await player1.gamePage.selectCardInZoneOverlay('Lightning Bolt')
    await expect(confirm).toBeDisabled()
    await player1.gamePage.selectCardInZoneOverlay('Shock')
    await expect(confirm).toBeEnabled()
    await player1.page.mouse.move(0, 0)
    await testInfo.attach(`one-per-graveyard-${viewport.width}`, {
      body: await player1.page.screenshot(), contentType: 'image/png',
    })
    phase = 'copy-offer'
    await confirm.click()
    await expect(heading).toBeHidden()
    const decline = player1.page.getByRole('button').filter({ hasText: /^No/ }).first()
    if (!(await decline.isVisible())) await player2.gamePage.pass()
    await expect(decline).toBeVisible()
    phase = 'copy-declined'
    await player1.gamePage.answerNo()
    await expect(decline).toBeHidden()
    await player1.gamePage.expectGraveyardSize(player1.playerId, 1)
    await player1.gamePage.expectGraveyardSize(player2.playerId, 1)
    await testInfo.attach('react-key-warnings', { body: Buffer.from(JSON.stringify(keyWarnings)), contentType: 'application/json' })
  })
}
