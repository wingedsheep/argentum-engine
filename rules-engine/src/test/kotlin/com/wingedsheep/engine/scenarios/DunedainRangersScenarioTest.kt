package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.player.TheRingComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Dúnedain Rangers — "Landfall — Whenever a land you control enters, if you don't control a
 * Ring-bearer, the Ring tempts you." Exercises the new `StatePredicate.IsRingBearer` via the
 * `Conditions.YouControl(ringBearer, negate = true)` intervening-if.
 */
class DunedainRangersScenarioTest : FunSpec({

    test("landfall tempts you when you don't control a Ring-bearer") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(p1, "Dúnedain Rangers")
        val land = driver.putCardInHand(p1, "Forest")
        driver.playLand(p1, land) // landfall → no Ring-bearer → the Ring tempts you
        driver.bothPass()

        // The temptation pauses to choose a Ring-bearer.
        driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        driver.state.getEntity(p1)?.get<TheRingComponent>()?.temptCount shouldBe 1
    }

    test("landfall does not tempt you when you already control a Ring-bearer") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(p1, "Dúnedain Rangers")
        val bear = driver.putCreatureOnBattlefield(p1, "Grizzly Bears")

        // Make p1 control a Ring-bearer first (tempt via Birthday Escape).
        val spell = driver.putCardInHand(p1, "Birthday Escape")
        driver.giveMana(p1, Color.BLUE, 1)
        driver.castSpell(p1, spell)
        driver.bothPass()
        val dec = driver.pendingDecision as SelectCardsDecision
        driver.submitDecision(p1, CardsSelectedResponse(dec.id, listOf(bear)))
        driver.state.getEntity(p1)?.get<TheRingComponent>()?.temptCount shouldBe 1

        // Now a landfall: the intervening-if (no Ring-bearer) is false → no new temptation.
        val land = driver.putCardInHand(p1, "Forest")
        driver.playLand(p1, land)
        driver.bothPass()

        driver.pendingDecision shouldBe null
        driver.state.getEntity(p1)?.get<TheRingComponent>()?.temptCount shouldBe 1
    }
})
