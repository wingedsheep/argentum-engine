package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh3.cards.RipplesOfUndeath
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe

/**
 * Ripples of Undeath — {1}{B} Enchantment
 * At the beginning of your first main phase, mill three cards. Then you may pay {1} and 3 life.
 * If you do, put a card from among those cards into your hand.
 */
class RipplesOfUndeathScenarioTest : FunSpec({

    data class Setup(val driver: GameTestDriver, val me: EntityId, val milled: List<EntityId>)

    /** Ripples on my battlefield, three known cards on top, advance into my first main phase. */
    fun setup(): Setup {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(RipplesOfUndeath))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40))
        val me = driver.activePlayer!!
        withClue("the game starts before the first main phase") {
            (driver.state.step == Step.PRECOMBAT_MAIN) shouldBe false
        }
        driver.putPermanentOnBattlefield(me, "Ripples of Undeath")
        val c = driver.putCardOnTopOfLibrary(me, "Hill Giant")
        val b = driver.putCardOnTopOfLibrary(me, "Savannah Lions")
        val a = driver.putCardOnTopOfLibrary(me, "Grizzly Bears")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        withClue("it is my first main phase with the trigger waiting") {
            driver.activePlayer shouldBe me
            driver.stackSize shouldBe 1
        }
        return Setup(driver, me, listOf(a, b, c))
    }

    /** Resolve the trigger, answering the pay question with [pay]; stops at a card selection. */
    fun GameTestDriver.resolveTrigger(me: EntityId, pay: Boolean) {
        bothPass()
        var guard = 0
        while (guard++ < 10) {
            when (val d = pendingDecision) {
                is YesNoDecision -> submitYesNo(me, pay).error shouldBe null
                is SelectManaSourcesDecision -> submitManaAutoPayOrDecline(me, autoPay = true).error shouldBe null
                is SelectCardsDecision -> return
                null -> return
                else -> error("unexpected decision $d")
            }
        }
    }

    test("mills three; paying {1} and 3 life puts a chosen milled card into hand") {
        val (driver, me, milled) = setup()
        driver.giveColorlessMana(me, 1)
        driver.resolveTrigger(me, pay = true)

        val decision = driver.pendingDecision as SelectCardsDecision
        withClue("the choice is among the three milled cards") {
            decision.options.toSet() shouldBe milled.toSet()
        }
        driver.submitCardSelection(me, listOf(milled[1])).error shouldBe null

        driver.getLifeTotal(me) shouldBe 17
        driver.getHand(me) shouldContain milled[1]
        driver.getGraveyard(me) shouldContainAll listOf(milled[0], milled[2])
    }

    test("declining to pay leaves all three milled cards in the graveyard") {
        val (driver, me, milled) = setup()
        driver.giveColorlessMana(me, 1)
        driver.resolveTrigger(me, pay = false)

        driver.pendingDecision shouldBe null
        driver.getLifeTotal(me) shouldBe 20
        driver.getGraveyard(me) shouldContainAll milled
    }

    test("without {1} available the payment is not offered") {
        val (driver, me, milled) = setup()
        driver.resolveTrigger(me, pay = true)

        driver.pendingDecision shouldBe null
        driver.getLifeTotal(me) shouldBe 20
        driver.getGraveyard(me) shouldContainAll milled
    }
})
