package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.frf.cards.WhiskAway
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Whisk Away (Fate Reforged #57) — {2}{U} Instant.
 *
 * "Put target attacking or blocking creature on top of its owner's library."
 */
class WhiskAwayScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + WhiskAway)
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("puts an attacking creature on top of its owner's library") {
        val driver = createDriver()
        val attacker = driver.activePlayer!!
        val defender = driver.getOpponent(attacker)
        val courser = driver.putCreatureOnBattlefield(attacker, "Centaur Courser")
        driver.removeSummoningSickness(courser)
        val whisk = driver.putCardInHand(defender, "Whisk Away")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attacker, listOf(courser), defender).error shouldBe null
        driver.passPriority(attacker)
        driver.giveMana(defender, Color.BLUE, 3)
        driver.castSpellWithTargets(defender, whisk, listOf(ChosenTarget.Permanent(courser))).error shouldBe null
        driver.bothPass()

        driver.findPermanent(attacker, "Centaur Courser") shouldBe null
        driver.state.getZone(attacker, Zone.LIBRARY).first() shouldBe courser
    }

    test("a creature that is neither attacking nor blocking can't be targeted") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        val courser = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")
        val whisk = driver.putCardInHand(me, "Whisk Away")
        driver.giveMana(me, Color.BLUE, 3)

        driver.castSpellWithTargets(me, whisk, listOf(ChosenTarget.Permanent(courser))).error shouldNotBe null
        driver.findPermanent(opponent, "Centaur Courser") shouldBe courser
    }
})
