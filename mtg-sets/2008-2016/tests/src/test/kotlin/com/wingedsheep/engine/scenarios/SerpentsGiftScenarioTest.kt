package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.m13.cards.SerpentsGift
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Serpent's Gift {2}{G} — Instant
 *   Target creature gains deathtouch until end of turn.
 */
class SerpentsGiftScenarioTest : FunSpec({

    test("the target gains deathtouch, which wears off at end of turn") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(SerpentsGift))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!
        repeat(3) { driver.putLandOnBattlefield(me, "Forest") }
        val courser = driver.putCreatureOnBattlefield(me, "Centaur Courser")

        val gift = driver.putCardInHand(me, "Serpent's Gift")
        driver.castSpell(me, gift, listOf(courser)).error shouldBe null
        driver.bothPass()
        driver.state.projectedState.hasKeyword(courser, Keyword.DEATHTOUCH) shouldBe true

        driver.passPriorityUntil(Step.UPKEEP, driver.getOpponent(me))
        driver.state.projectedState.hasKeyword(courser, Keyword.DEATHTOUCH) shouldBe false
    }
})
