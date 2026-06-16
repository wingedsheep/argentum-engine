package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.IntimidationCampaign
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Intimidation Campaign (OTJ #208) — {1}{U}{B} Enchantment.
 *
 * "When this enchantment enters, each opponent loses 1 life, you gain 1 life, and you draw a card.
 *  Whenever you commit a crime, you may return this enchantment to its owner's hand."
 *
 * Verifies the ETB drain/gain/draw, and the optional crime trigger that may bounce the enchantment
 * back to its owner's hand (exercised by casting a spell that targets the opponent — a crime).
 */
class IntimidationCampaignScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(IntimidationCampaign)
        driver.initMirrorMatch(deck = Deck.of("Island" to 30, "Mountain" to 30), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("enters: each opponent loses 1, you gain 1, you draw a card") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val handBefore = driver.getHand(me).size

        val campaign = driver.putCardInHand(me, "Intimidation Campaign")
        driver.giveMana(me, Color.BLUE, 1)
        driver.giveMana(me, Color.BLACK, 1)
        driver.giveColorlessMana(me, 1)
        driver.castSpell(me, campaign).isSuccess shouldBe true
        driver.bothPass() // resolve enchantment -> enters -> ETB trigger on stack
        driver.bothPass() // resolve the ETB trigger

        driver.getLifeTotal(opp) shouldBe 19
        driver.getLifeTotal(me) shouldBe 21
        // +1 from the draw effect (mana was injected, no land was played from the draw).
        driver.getHand(me).size shouldBe handBefore + 1
    }

    test("committing a crime offers to return the enchantment to hand; accepting bounces it") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val campaign = driver.putPermanentOnBattlefield(me, "Intimidation Campaign")

        // Commit a crime: cast Lightning Bolt targeting the opponent.
        val bolt = driver.putCardInHand(me, "Lightning Bolt")
        driver.giveMana(me, Color.RED, 1)
        driver.castSpell(me, bolt, targets = listOf(opp)).isSuccess shouldBe true
        // Crime is committed at cast; the bounce trigger goes on the stack above the Bolt and
        // resolves first, presenting the "may" prompt.
        driver.bothPass()

        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(me, true) // choose to return it

        driver.getHand(me).contains(campaign) shouldBe true
    }

    test("committing a crime: declining the may leaves the enchantment on the battlefield") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val campaign = driver.putPermanentOnBattlefield(me, "Intimidation Campaign")

        val bolt = driver.putCardInHand(me, "Lightning Bolt")
        driver.giveMana(me, Color.RED, 1)
        driver.castSpell(me, bolt, targets = listOf(opp)).isSuccess shouldBe true
        driver.bothPass()

        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(me, false) // decline

        driver.getHand(me).contains(campaign) shouldBe false
    }
})
