package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Chimney Imp — Mirrodin #59, {4}{B} Creature — Imp 1/2
 *
 * Flying
 * When this creature dies, target opponent puts a card from their hand on top of their library.
 *
 * The claim under test is *who decides*. The card is the Gather → Select → Move pipeline over
 * the targeted opponent's hand with `Chooser.TargetPlayer`, so the selection decision must be
 * addressed to **that opponent**, not to the Imp's controller — the same three primitives with
 * `Chooser.Controller` would build a Duress-style effect instead, and nothing else in the
 * engine pairs `Chooser.TargetPlayer` with a *dies* trigger's declared target.
 *
 * Also pinned: the chosen card lands on top of *their* library (so it is the very next card
 * they draw), and an empty hand resolves as a clean no-op rather than a stuck decision.
 */
class ChimneyImpScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    /** Bolt [target] dead and resolve everything it puts on the stack up to the next decision. */
    fun killWithBolt(driver: GameTestDriver, caster: EntityId, target: EntityId) {
        driver.giveMana(caster, Color.RED, 1)
        val bolt = driver.putCardInHand(caster, "Lightning Bolt")
        driver.castSpellWithTargets(caster, bolt, listOf(ChosenTarget.Permanent(target)))
        // Resolve the Bolt, then the dies trigger it causes, stopping at the first decision.
        var guard = 0
        while (guard++ < 30 && driver.state.stack.isNotEmpty() && !driver.isPaused) {
            driver.bothPass()
        }
    }

    test("the targeted opponent chooses, and their card goes on top of their own library") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 20, "Mountain" to 20), startingLife = 20)

        val impController = driver.activePlayer!!
        val opponent = driver.getOpponent(impController)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val imp = driver.putCreatureOnBattlefield(impController, "Chimney Imp")
        // A distinctive card in the opponent's hand so the tuck is identifiable among lands.
        val tuckMe = driver.putCardInHand(opponent, "Centaur Courser")
        val handBefore = driver.getHandSize(opponent)
        val libraryBefore = driver.state.getLibrary(opponent).size

        killWithBolt(driver, impController, imp)

        // The dies trigger targets the sole opponent (forced) and pauses for *their* selection.
        val decision = driver.pendingDecision
        decision shouldNotBe null
        (decision as SelectCardsDecision).playerId shouldBe opponent

        driver.submitCardSelection(opponent, listOf(tuckMe))

        // One card left the hand for the library, and it is on top.
        driver.getHandSize(opponent) shouldBe handBefore - 1
        driver.state.getLibrary(opponent).size shouldBe libraryBefore + 1
        driver.state.getLibrary(opponent).first() shouldBe tuckMe
    }

    test("an empty hand resolves the trigger as a no-op") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 20, "Mountain" to 20), startingLife = 20)

        val impController = driver.activePlayer!!
        val opponent = driver.getOpponent(impController)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val imp = driver.putCreatureOnBattlefield(impController, "Chimney Imp")
        // Empty the opponent's opening hand — an opponent is still a legal target, there is
        // just nothing to put back.
        driver.getHand(opponent).toList().forEach { driver.moveToGraveyard(it) }
        driver.getHandSize(opponent) shouldBe 0
        val libraryBefore = driver.state.getLibrary(opponent).size

        killWithBolt(driver, impController, imp)

        // No selection is presented and nothing moves.
        (driver.pendingDecision as? SelectCardsDecision) shouldBe null
        driver.state.getLibrary(opponent).size shouldBe libraryBefore
    }
})
