import { test, expect, type Page } from '@playwright/test'
import { HAND, BATTLEFIELD, cardByName } from './selectors'

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
}
