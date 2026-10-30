package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.eld.cards.BelovedPrincess
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Beloved Princess (ELD #7) — "Lifelink. This creature can't be blocked by creatures with power 3
 * or greater."
 */
class BelovedPrincessScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(BelovedPrincess))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        return driver
    }

    test("a power-3 creature can't block her, a power-2 creature can") {
        val driver = createDriver()
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!
        val foe = driver.getOpponent(me)
        val princess = driver.putCreatureOnBattlefield(me, "Beloved Princess")
        driver.removeSummoningSickness(princess)
        val courser = driver.putCreatureOnBattlefield(foe, "Centaur Courser")
        val bears = driver.putCreatureOnBattlefield(foe, "Grizzly Bears")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(princess), foe).error shouldBe null
        driver.bothPass()

        driver.declareBlockers(foe, mapOf(courser to listOf(princess))).error shouldNotBe null
        driver.declareBlockers(foe, mapOf(bears to listOf(princess))).error shouldBe null
    }

    test("lifelink: unblocked combat damage gains her controller 1 life") {
        val driver = createDriver()
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!
        val foe = driver.getOpponent(me)
        val princess = driver.putCreatureOnBattlefield(me, "Beloved Princess")
        driver.removeSummoningSickness(princess)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(princess), foe).error shouldBe null
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        driver.getLifeTotal(me) shouldBe 21
        driver.getLifeTotal(foe) shouldBe 19
    }
})
