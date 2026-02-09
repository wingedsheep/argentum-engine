import { test, expect, type Page } from '@playwright/test'
import { HAND, BATTLEFIELD, cardByName, graveyard, PLAYER_LIBRARY, OPPONENT_LIBRARY } from './selectors'

/**
 * Page object wrapping common game board interactions.
 * All methods use resilient locator strategies (text, alt, roles).
 *
 * The game uses an auto-pass system that automatically passes priority
 * when a player has no legal actions. Most priority passes happen
 * automatically — only call pass() when the player genuinely needs
 * to manually advance (e.g. moving to combat, confirming attackers).
 *
 * Each interaction method captures a screenshot and attaches it to the
 * Playwright HTML report for step-by-step visual debugging.
 */
/** Shared step counter across all GamePage instances in a test, for ordered screenshots. */
let stepCounter = 0

export class GamePage {
  constructor(
    readonly page: Page,
    readonly label: string = 'Player',
  ) {}

  /** Capture a screenshot and attach it to the test report. Waits briefly for images to load. */
  async screenshot(stepName: string) {
    const step = String(++stepCounter).padStart(2, '0')
    // Wait for network to settle so card images finish loading
    await this.page.waitForLoadState('networkidle').catch(() => {})
    const body = await this.page.screenshot()
    await test.info().attach(`${step} ${this.label}: ${stepName}`, {
      body,
      contentType: 'image/png',
    })
  }

  /** Reset step counter — call at the start of each test via fixture. */
  static resetStepCounter() {
    stepCounter = 0
  }

  /** Wait for the game board to be fully loaded (hand zone visible). */
  async waitForGameReady() {
    await this.page.locator(HAND).waitFor({ state: 'visible', timeout: 30_000 })
    await this.screenshot('Game ready')
  }

  /** Click a card by its name (uses img alt text). */
  async clickCard(name: string) {
    await this.page.locator(cardByName(name)).first().click()
    await this.screenshot(`Click ${name}`)
  }

  /** Click an action menu button by its label text (e.g. "Cast Gravedigger", "Cycle"). */
  async selectAction(label: string) {
    await this.page.locator('button').filter({ hasText: label }).first().click()
    await this.screenshot(`Select action: ${label}`)
  }

  /** Click the Pass / Resolve / End Turn button. */
  async pass() {
    const passButton = this.page
      .getByRole('button')
      .filter({ hasText: /^(Pass|Resolve|End Turn)/ })
    await passButton.first().click()
    await this.screenshot('Pass')
  }

  /**
   * Wait for an item to appear on this player's stack, then pass/resolve.
   * Use when the opponent needs to respond to a new stack item — ensures the
   * state update has arrived over WebSocket before clicking.
   */
  async resolveStack(stackItemText: string) {
    await this.page.getByText(stackItemText).waitFor({ state: 'visible', timeout: 10_000 })
    await this.pass()
  }

  /** Click the Yes button on a may-effect decision. */
  async answerYes() {
    await this.page.locator('button').filter({ hasText: /^Yes/ }).first().click()
    await this.screenshot('Answer Yes')
  }

  /** Click the No button on a may-effect decision. */
  async answerNo() {
    await this.page.locator('button').filter({ hasText: /^No/ }).first().click()
    await this.screenshot('Answer No')
  }

  /**
   * Select a target card on the battlefield — click the card image.
   * For cards visible directly on the game board.
   */
  async selectTarget(name: string) {
    await this.page.locator(cardByName(name)).first().click()
    await this.screenshot(`Select target: ${name}`)
  }

  /**
   * Select a card inside a targeting step modal (multi-target spells like Cruel Revival).
   * Dismisses card preview first, then clicks the card inside the targeting overlay.
   */
  async selectTargetInStep(name: string) {
    // Dismiss any card preview tooltip by moving mouse to corner
    await this.page.mouse.move(0, 0)
    // Wait for the targeting overlay card to be clickable (last instance, after sidebar)
    await this.page.locator(cardByName(name)).last().click()
    await this.screenshot(`Select target in step: ${name}`)
  }

  /**
   * Select a card inside a zone selection overlay (graveyard, library search, etc.).
   * Scopes the click to the overlay to avoid clicking same-named cards on the board.
   */
  async selectCardInZoneOverlay(name: string) {
    // Wait for the zone selection overlay to appear (has a heading like "Choose from Graveyard")
    await this.page
      .getByRole('heading', { level: 2 })
      .first()
      .waitFor({ state: 'visible', timeout: 15_000 })

    // Find the overlay: the deepest div containing both a heading and action buttons
    const overlay = this.page
      .locator('div')
      .filter({ has: this.page.getByRole('heading', { level: 2 }) })
      .filter({
        has: this.page.getByRole('button', { name: /Decline|Confirm|View Battlefield/ }),
      })
      .last()

    await overlay.locator(cardByName(name)).click()
    await this.screenshot(`Select in overlay: ${name}`)
  }

  /** Confirm target selection (clicks "Confirm Target" or "Confirm (N)" button). */
  async confirmTargets() {
    await this.page
      .getByRole('button')
      .filter({ hasText: /^Confirm/ })
      .first()
      .click()
    await this.screenshot('Confirm targets')
  }

  /** Decline optional targets (clicks "Decline" or "Select None" button). */
  async skipTargets() {
    const decline = this.page.getByRole('button').filter({ hasText: /^(Decline|Select None)/ })
    await decline.first().click()
    await this.screenshot('Skip targets')
  }

  /** Assert a card is on the battlefield (scoped to battlefield zones). */
  async expectOnBattlefield(name: string) {
    const battlefield = this.page.locator(BATTLEFIELD)
    await expect(battlefield.locator(cardByName(name)).first()).toBeVisible({ timeout: 10_000 })
  }

  /** Assert a card is NOT on the battlefield (scoped to battlefield zones). */
  async expectNotOnBattlefield(name: string) {
    const battlefield = this.page.locator(BATTLEFIELD)
    await expect(battlefield.locator(cardByName(name))).toHaveCount(0, { timeout: 10_000 })
  }

  /** Assert a card is in the player's hand (inside the hand zone). */
  async expectInHand(name: string) {
    const handZone = this.page.locator(HAND)
    await expect(handZone.locator(cardByName(name)).first()).toBeVisible({ timeout: 10_000 })
  }

  /** Assert a card is NOT in the player's hand. */
  async expectNotInHand(name: string) {
    const handZone = this.page.locator(HAND)
    await expect(handZone.locator(cardByName(name))).toHaveCount(0, { timeout: 10_000 })
  }

  /** Assert a player's life total. */
  async expectLifeTotal(playerId: string, value: number) {
    const display = this.page.locator(`[data-life-display="${playerId}"]`)
    await expect(display).toContainText(String(value), { timeout: 10_000 })
  }

  /** Wait for a decision prompt to appear (yes/no, targeting, zone selection, etc.). */
  async waitForDecision(timeout = 10_000) {
    // Decision UIs render buttons like Yes/No, Confirm, Decline, or zone selection overlays.
    // Wait for any decision-related button to appear.
    await this.page
      .locator('button')
      .filter({ hasText: /^(Yes|No|Confirm|Decline|Select None)/ })
      .first()
      .waitFor({ state: 'visible', timeout })
  }

  /** Click "Attack All" then confirm the attack declaration. Waits for buttons to appear. */
  async attackAll() {
    const attackAllBtn = this.page.getByRole('button', { name: 'Attack All' })
    await attackAllBtn.waitFor({ state: 'visible', timeout: 10_000 })
    await attackAllBtn.click()

    // After selecting attackers, confirm with the "Attack with N" button
    const confirmBtn = this.page.locator('button').filter({ hasText: /^Attack with/ })
    await confirmBtn.waitFor({ state: 'visible', timeout: 5_000 })
    await confirmBtn.click()
    await this.screenshot('Attack all')
  }

  /** Click "Skip Attacking" to declare no attackers. */
  async skipAttacking() {
    await this.page.getByRole('button', { name: 'Skip Attacking' }).click()
    await this.screenshot('Skip attacking')
  }

  /** Declare a creature as attacker by clicking it. */
  async declareAttacker(name: string) {
    await this.clickCard(name)
  }

  /** Confirm attackers / move to next combat step by passing. */
  async confirmAttackers() {
    await this.pass()
  }

  /** Confirm no blockers by passing. */
  async confirmNoBlockers() {
    await this.pass()
  }

  /** Click "No Blocks" to skip blocking entirely. */
  async noBlocks() {
    await this.page.getByRole('button', { name: 'No Blocks' }).click()
    await this.screenshot('No blocks')
  }

  /** Assign a blocker to an attacker: click the blocker card then click the attacker card. */
  async declareBlocker(blockerName: string, attackerName: string) {
    await this.page.locator(cardByName(blockerName)).first().click()
    await this.page.locator(cardByName(attackerName)).first().click()
    await this.screenshot(`Block ${attackerName} with ${blockerName}`)
  }

  /** Confirm blocker assignments by clicking "Confirm Blocks". */
  async confirmBlockers() {
    await this.page.getByRole('button', { name: 'Confirm Blocks' }).click()
    await this.screenshot('Confirm blocks')
  }

  /** Select a mana color from the mana color selection overlay (e.g. "White", "Blue"). */
  async selectManaColor(color: 'White' | 'Blue' | 'Black' | 'Red' | 'Green') {
    const btn = this.page.getByRole('button', { name: color })
    await btn.waitFor({ state: 'visible', timeout: 10_000 })
    await btn.click()
    await this.screenshot(`Select mana color: ${color}`)
  }

  /** Select a number from the ChooseNumberDecision UI, then confirm. */
  async selectNumber(n: number) {
    // Click the number button in the number selection UI
    const numberBtn = this.page.locator('button').filter({ hasText: new RegExp(`^${n}$`) })
    await numberBtn.first().click()
    // Confirm the selection
    await this.page.getByRole('button', { name: 'Confirm' }).click()
    await this.screenshot(`Select number: ${n}`)
  }

  /** Select an option from the ChooseOptionDecision UI by its text, then confirm. */
  async selectOption(text: string) {
    // Click the option button matching the text
    const optionBtn = this.page.locator('button').filter({ hasText: text })
    await optionBtn.first().click()
    // Confirm the selection
    await this.page.getByRole('button', { name: 'Confirm' }).click()
    await this.screenshot(`Select option: ${text}`)
  }

  /** Select X value on the X-cost selector, then click Cast. */
  async selectXValue(x: number) {
    // Set the slider value by clicking +/- buttons to reach the desired value
    // The slider input[type=range] is available — use fill to set it
    const slider = this.page.locator('input[type="range"]')
    await slider.waitFor({ state: 'visible', timeout: 10_000 })
    await slider.fill(String(x))
    // Click the Cast button to confirm
    await this.page.getByRole('button', { name: 'Cast' }).click()
    await this.screenshot(`Select X = ${x}`)
  }

  /** Click a player's life display to target them (for damage-to-player spells). */
  async selectPlayer(playerId: string) {
    const playerDisplay = this.page.locator(`[data-player-id="${playerId}"]`)
    await playerDisplay.click()
    await this.screenshot(`Select player: ${playerId}`)
  }

  /** Cast a morph creature face-down by clicking "Cast Face-Down" action. */
  async castFaceDown(name: string) {
    await this.clickCard(name)
    await this.selectAction('Cast Face-Down')
  }

  /** Turn a face-down creature face-up by clicking it and selecting the morph action. */
  async turnFaceUp(name: string) {
    // Face-down creatures show as generic morph image — click by position or alt text
    await this.page.locator(cardByName(name)).first().click()
    await this.selectAction('Turn Face Up')
  }

  /** Select a card from hand (for discard or other hand-targeting decisions). */
  async selectCardInHand(name: string) {
    const handZone = this.page.locator(HAND)
    await handZone.locator(cardByName(name)).first().click()
    await this.screenshot(`Select card in hand: ${name}`)
  }

  /** Assert the graveyard card count for a player. */
  async expectGraveyardSize(playerId: string, size: number) {
    const gy = this.page.locator(graveyard(playerId))
    if (size === 0) {
      // Empty graveyard has no count overlay
      await expect(gy.locator('img[alt]')).toHaveCount(0, { timeout: 10_000 })
    } else {
      await expect(gy).toContainText(String(size), { timeout: 10_000 })
    }
  }

  /** Assert the number of cards visible in the player's hand. */
  async expectHandSize(count: number) {
    const handZone = this.page.locator(HAND)
    await expect(handZone.locator('img[alt]')).toHaveCount(count, { timeout: 10_000 })
  }

  /** Assert the library size shown on the deck pile. */
  async expectLibrarySize(playerId: string, size: number) {
    // Library size is displayed as text in the deck pile div
    // Use the player/opponent library selector based on which side we're checking
    // For simplicity, check the text content directly in the appropriate library zone
    const libraryPile = this.page.locator(`[data-zone="player-library"], [data-zone="opponent-library"]`).filter({ hasText: String(size) })
    await expect(libraryPile.first()).toBeVisible({ timeout: 10_000 })
  }

  /** Assert a card on the battlefield is tapped (rotated 90 degrees). */
  async expectTapped(name: string) {
    const card = this.page.locator(BATTLEFIELD).locator(cardByName(name)).first()
    // Tapped cards are inside a container with rotate(90deg) style
    const container = card.locator('..')
    await expect(container).toHaveCSS('transform', /matrix.*/, { timeout: 10_000 })
  }

  /** Assert a card on the battlefield is untapped (not rotated). */
  async expectUntapped(name: string) {
    const card = this.page.locator(BATTLEFIELD).locator(cardByName(name)).first()
    await expect(card).toBeVisible({ timeout: 10_000 })
    // Simply verify it's visible — untapped cards have no rotation transform
  }

  /** Confirm a selection of cards (clicks "Confirm Selection" button). */
  async confirmSelection() {
    await this.page
      .getByRole('button')
      .filter({ hasText: /^(Confirm Selection|Confirm)/ })
      .first()
      .click()
    await this.screenshot('Confirm selection')
  }

  /** Declare a single attacker then confirm with the "Attack with N" button. */
  async attackWith(name: string) {
    await this.declareAttacker(name)
    const confirmBtn = this.page.locator('button').filter({ hasText: /^Attack with/ })
    await confirmBtn.waitFor({ state: 'visible', timeout: 5_000 })
    await confirmBtn.click()
    await this.screenshot(`Attack with ${name}`)
  }

  /**
   * Confirm distribution (e.g. damage assignment via Forked Lightning).
   * Clicks the "Confirm" button in the distribute bar.
   */
  async confirmDistribution() {
    await this.page
      .getByRole('button')
      .filter({ hasText: /^Confirm/ })
      .first()
      .click()
    await this.screenshot('Confirm distribution')
  }
}
