package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fin.cards.FateOfTheSunCryst
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Fate of the Sun-Cryst — {4}{W} Instant
 *
 * "This spell costs {2} less to cast if it targets a tapped creature.
 *  Destroy target nonland permanent."
 */
class FateOfTheSunCrystTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(FateOfTheSunCryst)
        return driver
    }

    test("destroys a target nonland permanent") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val victim = driver.putCreatureOnBattlefield(opp, "Glory Seeker")

        val fate = driver.putCardInHand(me, "Fate of the Sun-Cryst")
        driver.giveColorlessMana(me, 4)
        driver.giveMana(me, Color.WHITE, 1)
        driver.castSpell(me, fate, targets = listOf(victim))
        driver.bothPass()

        driver.findPermanent(opp, "Glory Seeker") shouldBe null
        driver.getGraveyard(opp).contains(victim) shouldBe true
    }

    test("costs {2} less when targeting a tapped creature, and casts for the reduced cost") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val victim = driver.putCreatureOnBattlefield(opp, "Glory Seeker")
        driver.tapPermanent(victim)
        driver.isTapped(victim) shouldBe true

        // Provide exactly the reduced cost: {2}{W} (generic 4 - 2 reduction = 2, plus one white).
        val fate = driver.putCardInHand(me, "Fate of the Sun-Cryst")
        driver.giveColorlessMana(me, 2)
        driver.giveMana(me, Color.WHITE, 1)
        driver.castSpell(me, fate, targets = listOf(victim)).isSuccess shouldBe true
        driver.bothPass()

        driver.findPermanent(opp, "Glory Seeker") shouldBe null
        driver.getGraveyard(opp).contains(victim) shouldBe true
    }
})
