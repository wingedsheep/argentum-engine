package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.core.YesNoResponse
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.spm.cards.RentIsDue
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Rent Is Due (SPM #11) — "At the beginning of your end step, you may tap two untapped creatures
 * and/or Treasures you control. If you do, draw a card. Otherwise, sacrifice this enchantment."
 *
 * Pins the trigger timing: it fires at the **end step** (the pre-fix bug used `Triggers.YourUpkeep`).
 * The enchantment is placed during the main phase — after this turn's upkeep — so if the trigger were
 * still on the upkeep it would never fire this turn and the draw/tap below would not happen.
 */
class RentIsDueScenarioTest : FunSpec({

    test("fires at your end step: tap two creatures, then draw a card") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(RentIsDue)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20, skipMulligans = true)
        val you = driver.player1
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(you, "Rent Is Due")
        val c1 = driver.putCreatureOnBattlefield(you, "Savannah Lions")
        val c2 = driver.putCreatureOnBattlefield(you, "Grizzly Bears")
        val handBefore = driver.getHandSize(you)

        // Advance to the end step — passPriorityUntil stops at the step without resolving the queued
        // trigger, so our decision handling below drives it.
        driver.passPriorityUntil(Step.END)

        var guard = 0
        while (guard++ < 40 && (driver.isPaused || driver.state.stack.isNotEmpty())) {
            if (driver.isPaused) {
                when (val dec = driver.pendingDecision) {
                    is YesNoDecision -> driver.submitDecision(dec.playerId, YesNoResponse(dec.id, true))
                    is SelectCardsDecision ->
                        driver.submitDecision(dec.playerId, CardsSelectedResponse(dec.id, listOf(c1, c2)))
                    else -> error("unexpected decision resolving Rent Is Due: $dec")
                }
            } else {
                driver.bothPass()
            }
        }

        driver.getHandSize(you) shouldBe handBefore + 1
        driver.isTapped(c1) shouldBe true
        driver.isTapped(c2) shouldBe true
    }
})
