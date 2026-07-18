package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lci.cards.GrowingRitesOfItlimoc
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Growing Rites of Itlimoc // Itlimoc, Cradle of the Sun (LCI #188).
 *
 * Front — Growing Rites: ETB look at the top four cards, reveal a creature to hand, rest to the
 * bottom; and "At the beginning of your end step, if you control four or more creatures,
 * transform Growing Rites of Itlimoc."
 *
 * Pins the end-step intervening-if transform (fires at 4 creatures, not 3) and the ETB reveal.
 */
class GrowingRitesOfItlimocScenarioTest : FunSpec({

    fun setup(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(GrowingRitesOfItlimoc))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true)
        return driver
    }

    fun cardName(driver: GameTestDriver, id: EntityId): String? =
        driver.state.getEntity(id)?.get<CardComponent>()?.name

    fun passToEndStepAndResolve(driver: GameTestDriver) {
        driver.passPriorityUntil(Step.END)
        var guard = 0
        while (guard++ < 30 && (driver.pendingDecision != null || driver.state.stack.isNotEmpty())) {
            if (driver.pendingDecision != null) driver.autoResolveDecision() else driver.bothPass()
        }
    }

    test("transforms at the end step when you control four or more creatures") {
        val driver = setup()
        val p1 = driver.activePlayer!!
        val rites = driver.putPermanentOnBattlefield(p1, "Growing Rites of Itlimoc")
        repeat(4) { driver.putCreatureOnBattlefield(p1, "Savannah Lions") }

        passToEndStepAndResolve(driver)

        cardName(driver, rites) shouldBe "Itlimoc, Cradle of the Sun"
    }

    test("does not transform with only three creatures") {
        val driver = setup()
        val p1 = driver.activePlayer!!
        val rites = driver.putPermanentOnBattlefield(p1, "Growing Rites of Itlimoc")
        repeat(3) { driver.putCreatureOnBattlefield(p1, "Savannah Lions") }

        passToEndStepAndResolve(driver)

        cardName(driver, rites) shouldBe "Growing Rites of Itlimoc"
    }

    test("ETB reveals a creature from the top four and puts it into your hand") {
        val driver = setup()
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val lion = driver.putCardOnTopOfLibrary(p1, "Savannah Lions")
        val rites = driver.putCardInHand(p1, "Growing Rites of Itlimoc")
        driver.giveMana(p1, Color.GREEN, 3) // {2}{G}
        driver.castSpell(p1, rites).isSuccess shouldBe true

        driver.bothPass() // resolve the spell — Growing Rites enters, ETB queued
        driver.bothPass() // resolve the ETB — pauses for the reveal selection

        val decision = driver.pendingDecision as SelectCardsDecision
        driver.submitCardSelection(p1, listOf(lion))
        while (driver.pendingDecision != null) driver.autoResolveDecision()

        driver.getHand(p1).contains(lion) shouldBe true
    }
})
