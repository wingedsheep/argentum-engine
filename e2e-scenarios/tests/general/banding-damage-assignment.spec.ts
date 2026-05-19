import { test, expect } from '../../fixtures/scenarioFixture'

/**
 * E2E test for the Noble Elephant banding scenario that exposed two engine
 * bugs:
 *
 *   1. `getMinimumAssignments` returned full `lethalAmount` even when the
 *      attacker's power couldn't reach it, producing edges with
 *      `minimum > maximum`. The default response was then rejected as
 *      "Edge … amount X below minimum Y" — combat was unresolvable.
 *      Fixed by clamping each minimum to remaining attacker power and
 *      zeroing subsequent blockers when a preceding one couldn't be killed
 *      (CR 510.1c).
 *
 *   2. CR 702.22j/k says the chooser may *ignore* the damage-assignment
 *      order when banding is in play. The board was setting non-zero
 *      `minimum` on banding-controlled edges, so "dump all damage on one
 *      band member" was rejected even though the rules permit it. Fixed by
 *      forcing `minimum = 0` whenever banding flips the chooser.
 *
 * Setup: two Noble Elephants (2/2 banding) attack as a band; defender blocks
 * with a Hill Giant (3/3). Per CR 702.21g, blocking one band member blocks
 * all of them, so HG ends up blocking both NEs — bipartite. NE.power (2) <
 * HG.toughness (3), so the minimum-clamp fix matters on the ATK→BLK side.
 * NE has banding, so CR 702.22k's bypass kicks in on the BLK→ATK side.
 *
 * If either fix regresses, the test fails at the `confirmDamage` step
 * because the engine validator rejects the response.
 */
test.describe('Banding damage assignment', () => {
  test('band of Noble Elephants vs Hill Giant resolves without "below minimum" rejection', async ({
    createGame,
  }) => {
    const { player1, player2 } = await createGame({
      player1Name: 'Alice',
      player2Name: 'Bob',
      player1: {
        lifeTotal: 20,
        battlefield: [
          { name: 'Noble Elephant', tapped: false, summoningSickness: false },
          { name: 'Noble Elephant', tapped: false, summoningSickness: false },
          { name: 'Plains' },
          { name: 'Plains' },
          { name: 'Plains' },
        ],
        library: ['Plains', 'Plains'],
      },
      player2: {
        lifeTotal: 20,
        battlefield: [
          { name: 'Hill Giant' },
          { name: 'Mountain' },
          { name: 'Mountain' },
          { name: 'Mountain' },
        ],
        library: ['Mountain', 'Mountain'],
      },
      phase: 'PRECOMBAT_MAIN',
      activePlayer: 1,
    })

    const p1 = player1.gamePage
    const p2 = player2.gamePage

    // Advance into the combat phase.
    await p1.pass()

    // Click "Attack All" — selects both NEs as attackers but DOESN'T confirm
    // yet. The band drop only registers while combat is still in
    // declareAttackers mode, before "Attack with N" is clicked.
    const attackAllBtn = p1.page.getByRole('button', { name: 'Attack All' })
    await attackAllBtn.waitFor({ state: 'visible', timeout: 10_000 })
    await attackAllBtn.click()
    await p1.screenshot('Both NEs selected, pre-band')

    // Form a band: drag the second Noble Elephant onto the first via raw
    // mouse events. The app's banding drop handler listens to
    // mousedown/mousemove/mouseup (GameCard.tsx#draggingAttackerId), not
    // HTML5 drag-and-drop — so Playwright's `dragTo` doesn't trigger it.
    // Drag distance has to exceed MIN_DRAG_DISTANCE (30 px) or it registers
    // as a click and toggles the attacker off instead.
    const noBlockers = p1.page.locator('img[alt="Noble Elephant"]')
    await expect(noBlockers).toHaveCount(2, { timeout: 10_000 })
    const ne1Box = await noBlockers.first().boundingBox()
    const ne2Box = await noBlockers.nth(1).boundingBox()
    if (!ne1Box || !ne2Box) throw new Error('Noble Elephant bounding boxes unavailable')
    const ne1Center = { x: ne1Box.x + ne1Box.width / 2, y: ne1Box.y + ne1Box.height / 2 }
    const ne2Center = { x: ne2Box.x + ne2Box.width / 2, y: ne2Box.y + ne2Box.height / 2 }
    await p1.page.mouse.move(ne2Center.x, ne2Center.y)
    await p1.page.mouse.down()
    // Several intermediate moves so React can register the drag start and
    // travel before the drop lands.
    await p1.page.mouse.move(ne2Center.x + 5, ne2Center.y + 5)
    await p1.page.mouse.move(ne1Center.x, ne1Center.y, { steps: 20 })
    await p1.page.mouse.up()
    await p1.screenshot('Form Noble Elephant band')

    // Confirm attackers with "Attack with N".
    const confirmAttackBtn = p1.page.locator('button').filter({ hasText: /^Attack with/ })
    await confirmAttackBtn.waitFor({ state: 'visible', timeout: 5_000 })
    await confirmAttackBtn.click()
    await p1.screenshot('Attackers confirmed')

    // Bob declares Hill Giant as a blocker. Per CR 702.21g the band is
    // blocked as a group, so HG ends up blocking both elephants — a true
    // bipartite block.
    await p2.declareBlocker('Hill Giant', 'Noble Elephant')
    await p2.confirmBlockers()

    // The combat resolution board emits, and the local player's edges have
    // nothing meaningful to override (HG's bipartite division and the band
    // members' single-blocker edges all default to valid values now that
    // `getMinimumAssignments` clamps to attacker power). Auto-confirm fires
    // in ~600 ms using those defaults — the very behaviour the engine fix
    // unblocks. We verify by waiting for the *outcome* rather than racing
    // the briefly-visible Confirm button:
    //
    //   - Default ATK→BLK: NE1 deals 2 to HG, NE2 deals 2 to HG → HG takes
    //     4 ≥ 3 toughness → HG dies.
    //   - Default BLK→ATK (banding bypass, lethal-first defaults): HG deals
    //     2 to NE1 (lethal) and 1 to NE2 → NE1 dies, NE2 survives at 1/2.
    //   - No trample, no spillover → Bob stays at 20 life.
    //
    // Pre-fix, the validator would reject the server-supplied default with
    // "Edge … amount 2 below minimum 3" and combat would freeze.
    await p1.expectNotOnBattlefield('Hill Giant')
    await expect(p1.page.locator('img[alt="Noble Elephant"]')).toHaveCount(1, {
      timeout: 15_000,
    })
    await p1.expectLifeTotal(player2.playerId, 20)
    await p1.screenshot('End state')
  })
})
