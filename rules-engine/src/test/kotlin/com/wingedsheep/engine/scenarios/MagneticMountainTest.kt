package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import io.kotest.matchers.types.shouldBeInstanceOf
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for Magnetic Mountain (ARN).
 *
 * Magnetic Mountain: {1}{R}{R}, Enchantment
 *   - "Blue creatures don't untap during their controllers' untap steps." (continuous restriction)
 *   - "At the beginning of each player's upkeep, that player may choose any number of tapped blue
 *     creatures they control and pay {4} for each creature chosen this way. If the player does,
 *     untap those creatures." (resolution-time choose-any-number + per-creature scaled payment,
 *     paid by the *upkeep* player — exercising the [PayDynamicManaCostEffect] payer parameter).
 */
class MagneticMountainTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    /** Cross from the current step back to [targetPlayer]'s precombat main (one full untap step run). */
    fun advanceToPlayerMain(driver: GameTestDriver, targetPlayer: EntityId) {
        driver.passPriorityUntil(Step.END, maxPasses = 200)
        driver.bothPass()
        if (driver.activePlayer == targetPlayer) {
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN, maxPasses = 200)
            return
        }
        driver.passPriorityUntil(Step.END, maxPasses = 200)
        driver.bothPass()
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, maxPasses = 200)
    }

    /** Pass priority while inside the upkeep step until the Magnetic Mountain trigger pauses for a decision. */
    fun resolveUpkeepTriggerDecision(driver: GameTestDriver) {
        var passes = 0
        while (driver.pendingDecision == null && driver.currentStep == Step.UPKEEP && !driver.state.gameOver && passes < 40) {
            val pp = driver.priorityPlayer ?: break
            driver.passPriority(pp)
            passes++
        }
    }

    test("blue creatures stay tapped through their untap step; non-blue creatures untap normally") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)

        val player1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(player1, "Magnetic Mountain")
        val blue = driver.putPermanentOnBattlefield(player1, "Storm Crow")
        val green = driver.putPermanentOnBattlefield(player1, "Grizzly Bears")
        driver.tapPermanent(blue)
        driver.tapPermanent(green)

        // Back to player1's next precombat main — their untap step has run.
        advanceToPlayerMain(driver, player1)

        driver.isTapped(blue) shouldBe true   // blue creature held down by Magnetic Mountain
        driver.isTapped(green) shouldBe false // non-blue creature untaps as usual
    }

    test("the restriction applies to every player's blue creatures, not just the controller's") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)

        val player1 = driver.activePlayer!!
        val player2 = driver.getOpponent(player1)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(player1, "Magnetic Mountain")
        val opponentBlue = driver.putPermanentOnBattlefield(player2, "Storm Crow")
        driver.tapPermanent(opponentBlue)

        advanceToPlayerMain(driver, player2)
        driver.isTapped(opponentBlue) shouldBe true
    }

    test("upkeep player pays {4} to untap their chosen tapped blue creature") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)

        val player1 = driver.activePlayer!!
        val player2 = driver.getOpponent(player1)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Magnetic Mountain belongs to player1, but the *upkeep* player (player2) makes the choice
        // and pays — the cross-player payer correctness this feature is built around.
        driver.putPermanentOnBattlefield(player1, "Magnetic Mountain")
        val blue = driver.putPermanentOnBattlefield(player2, "Storm Crow")
        driver.tapPermanent(blue)
        val lands = (1..5).map { driver.putLandOnBattlefield(player2, "Mountain") }

        // Advance to player2's upkeep; the trigger fires and pauses for the selection.
        driver.passPriorityUntil(Step.UPKEEP, maxPasses = 200)
        resolveUpkeepTriggerDecision(driver)

        driver.isTapped(blue) shouldBe true // still tapped — its untap step was skipped
        driver.pendingDecision shouldNotBe null

        driver.submitCardSelection(player2, listOf(blue)) // choose the creature
        driver.submitYesNo(player2, true)                 // pay {4}

        driver.isTapped(blue) shouldBe false              // untapped after payment
        lands.count { driver.isTapped(it) } shouldBe 4    // exactly {4} was paid
    }

    test("pays {4} per creature: choosing two costs {8} and untaps both") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)

        val player1 = driver.activePlayer!!
        val player2 = driver.getOpponent(player1)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(player1, "Magnetic Mountain")
        val blue1 = driver.putPermanentOnBattlefield(player2, "Storm Crow")
        val blue2 = driver.putPermanentOnBattlefield(player2, "Storm Crow")
        driver.tapPermanent(blue1)
        driver.tapPermanent(blue2)
        val lands = (1..9).map { driver.putLandOnBattlefield(player2, "Mountain") }

        driver.passPriorityUntil(Step.UPKEEP, maxPasses = 200)
        resolveUpkeepTriggerDecision(driver)

        driver.submitCardSelection(player2, listOf(blue1, blue2)) // choose both

        // The confirmation shows the computed total, not the per-creature formula.
        val payDecision = driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        payDecision.yesText shouldBe "Pay {8}"

        driver.submitYesNo(player2, true)                         // pay {8} (chosen_count * 4)

        driver.isTapped(blue1) shouldBe false                     // both untapped
        driver.isTapped(blue2) shouldBe false
        lands.count { driver.isTapped(it) } shouldBe 8            // {4} for each = {8}, not {4}
    }

    test("selection is capped by affordability: with {7} available only one of two creatures can be chosen") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)

        val player1 = driver.activePlayer!!
        val player2 = driver.getOpponent(player1)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(player1, "Magnetic Mountain")
        val blue1 = driver.putPermanentOnBattlefield(player2, "Storm Crow")
        val blue2 = driver.putPermanentOnBattlefield(player2, "Storm Crow")
        driver.tapPermanent(blue1)
        driver.tapPermanent(blue2)
        // Seven lands — enough for {4} on one creature, short of the {8} two would require.
        val lands = (1..7).map { driver.putLandOnBattlefield(player2, "Mountain") }

        driver.passPriorityUntil(Step.UPKEEP, maxPasses = 200)
        resolveUpkeepTriggerDecision(driver)

        // MaxAffordablePayment folds floor({7} / {4}) = 1 into the selection's maximum, so the
        // player can never select an unpayable set and silently forfeit the whole untap.
        val selectDecision = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        selectDecision.maxSelections shouldBe 1

        driver.submitCardSelection(player2, listOf(blue1)) // the one affordable creature
        driver.submitYesNo(player2, true)                  // pay {4}

        driver.isTapped(blue1) shouldBe false              // the chosen one untaps
        driver.isTapped(blue2) shouldBe true               // the other stays tapped
        lands.count { driver.isTapped(it) } shouldBe 4     // exactly {4} was paid
    }

    test("declining the payment leaves the chosen creature tapped") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)

        val player1 = driver.activePlayer!!
        val player2 = driver.getOpponent(player1)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(player1, "Magnetic Mountain")
        val blue = driver.putPermanentOnBattlefield(player2, "Storm Crow")
        driver.tapPermanent(blue)
        val lands = (1..5).map { driver.putLandOnBattlefield(player2, "Mountain") }

        driver.passPriorityUntil(Step.UPKEEP, maxPasses = 200)
        resolveUpkeepTriggerDecision(driver)

        driver.submitCardSelection(player2, listOf(blue)) // choose the creature
        driver.submitYesNo(player2, false)                // decline to pay

        driver.isTapped(blue) shouldBe true               // stays tapped
        lands.count { driver.isTapped(it) } shouldBe 0    // no mana spent
    }

    test("a player who cannot afford even one untap is never prompted at all") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)

        val player1 = driver.activePlayer!!
        val player2 = driver.getOpponent(player1)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(player1, "Magnetic Mountain")
        val blue = driver.putPermanentOnBattlefield(player2, "Storm Crow")
        driver.tapPermanent(blue)
        // Only three lands — short of the {4} required to untap even one creature.
        val lands = (1..3).map { driver.putLandOnBattlefield(player2, "Mountain") }

        driver.passPriorityUntil(Step.UPKEEP, maxPasses = 200)
        resolveUpkeepTriggerDecision(driver)

        // The affordability cap is zero, so the trigger resolves without ever prompting —
        // neither a selection nor a payment question.
        driver.pendingDecision shouldBe null
        driver.isTapped(blue) shouldBe true
        lands.count { driver.isTapped(it) } shouldBe 0
    }
})
